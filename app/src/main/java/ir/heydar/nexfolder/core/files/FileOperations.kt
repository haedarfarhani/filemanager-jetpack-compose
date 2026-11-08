package ir.heydar.nexfolder.core.files

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Progress snapshot shared by copy/move/extract/compress operations. */
data class OperationProgress(
    val title: String,
    val currentFile: String,
    val processedBytes: Long,
    val totalBytes: Long,
    val filesDone: Int = 0,
    val filesTotal: Int = 0
)

/**
 * Low-level copy/move helpers with byte-accurate progress and cooperative cancellation.
 * All functions are safe to call from coroutines and check for cancellation while streaming.
 */
object FileOperations {

    private const val BUFFER_SIZE = 64 * 1024

    suspend fun copy(
        source: File,
        target: File,
        onProgress: suspend (OperationProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): Boolean = withContext(Dispatchers.IO) {
        if (source.isFile) {
            runPlan(listOf(source to target), onProgress, isCancelled)
        } else {
            copyDirectory(source, target, onProgress, isCancelled)
        }
    }

    suspend fun move(
        source: File,
        target: File,
        onProgress: suspend (OperationProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): Boolean = withContext(Dispatchers.IO) {
        // Fast path: same-volume rename (cheap, no data copy)
        if (source.renameTo(target)) return@withContext true
        val copied = if (source.isFile) {
            runPlan(listOf(source to target), onProgress, isCancelled)
        } else {
            copyDirectory(source, target, onProgress, isCancelled)
        }
        if (copied && !isCancelled()) {
            source.deleteRecursively()
        } else if (!copied) {
            target.deleteRecursively()
        }
        copied
    }

    fun totalSize(file: File): Long = when {
        file.isFile -> file.length()
        else -> file.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }

    /** Returns a non-existing destination for [name] inside [dir] ("file (1).ext" style). */
    fun uniqueDestination(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($index)$ext")
            index++
        }
        return candidate
    }

    private suspend fun copyDirectory(
        source: File,
        target: File,
        onProgress: suspend (OperationProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        if (!target.exists() && !target.mkdirs()) return false
        val plan = mutableListOf<Pair<File, File>>()
        source.walkTopDown().forEach { f ->
            if (f.isDirectory) {
                File(target, f.relativeTo(source).path).mkdirs()
            } else {
                plan += f to File(target, f.relativeTo(source).path)
            }
        }
        return runPlan(plan, onProgress, isCancelled)
    }

    /** Copies a flat plan of (source, destination) pairs with aggregate progress. */
    suspend fun runPlan(
        plan: List<Pair<File, File>>,
        onProgress: suspend (OperationProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): Boolean = withContext(Dispatchers.IO) {
        plan.forEach { it.second.parentFile?.mkdirs() }
        val total = plan.sumOf { (src, _) -> src.length() }
        var processed = 0L
        var done = 0
        for ((src, dst) in plan) {
            if (isCancelled() || !currentCoroutineContext().isActive) return@withContext false
            try {
                FileInputStream(src).use { input ->
                    FileOutputStream(dst).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var lastReport = 0L
                        while (true) {
                            if (isCancelled() || !currentCoroutineContext().isActive) {
                                return@withContext false
                            }
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            processed += read
                            if (processed - lastReport >= BUFFER_SIZE * 4) {
                                lastReport = processed
                                onProgress(
                                    OperationProgress("", src.name, processed, total, done, plan.size)
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                dst.delete()
                return@withContext false
            }
            done++
            onProgress(OperationProgress("", src.name, processed, total, done, plan.size))
        }
        onProgress(OperationProgress("", "", processed, total, plan.size, plan.size))
        true
    }
}
