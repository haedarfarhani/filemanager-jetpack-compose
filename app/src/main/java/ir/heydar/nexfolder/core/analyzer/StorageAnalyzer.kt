package ir.heydar.nexfolder.core.analyzer

import ir.heydar.nexfolder.domain.model.CategoryUsage
import ir.heydar.nexfolder.domain.model.FileFormat
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.domain.model.FolderUsage
import ir.heydar.nexfolder.domain.model.StorageAnalysis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Walks a storage root and aggregates usage per file-type category, per top-level
 * folder, and keeps the largest files.
 *
 * Robustness rules:
 * - Every directory listing is individually guarded — unreadable subtrees are
 *   skipped instead of failing the whole scan (some ROMs restrict Android/data).
 * - Top-level subtrees scan in parallel on 6 workers for speed.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class StorageAnalyzer @Inject constructor() {

    private val scanDispatcher = Dispatchers.IO.limitedParallelism(6)

    suspend fun analyze(
        rootPath: String,
        onProgress: suspend (filesScanned: Long, currentPath: String) -> Unit = { _, _ -> }
    ): StorageAnalysis = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.isDirectory) return@withContext StorageAnalysis(rootPath = rootPath)

        val topLevel = try {
            root.listFiles()
        } catch (e: SecurityException) {
            null
        }
        if (topLevel == null) {
            throw SecurityException("Cannot read storage — check All-files access permission")
        }

        val scanned = AtomicLong(0)

        val partials = coroutineScope {
            topLevel.map { entry ->
                async(scanDispatcher) {
                    try {
                        if (entry.isDirectory) {
                            walkSubtree(entry, entry.absolutePath, scanned, onProgress)
                        } else {
                            walkFiles(setOf(entry), folderKey = null, scanned, onProgress)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // One unreadable subtree must not void the whole analysis
                        Partial()
                    }
                }
            }.awaitAll()
        }

        currentCoroutineContext().ensureActive()

        // ---- merge ----
        val categoryBytes = HashMap<FileType, Long>()
        val categoryFiles = HashMap<FileType, Long>()
        val folderBytes = HashMap<String, Long>()
        val folderFiles = HashMap<String, Long>()
        val largest = mutableListOf<File>()
        var totalFiles = 0L
        var totalBytes = 0L
        var scannedTotal = 0L

        partials.forEach { partial ->
            partial.categoryBytes.forEach { (type, bytes) -> categoryBytes[type] = (categoryBytes[type] ?: 0L) + bytes }
            partial.categoryFiles.forEach { (type, count) -> categoryFiles[type] = (categoryFiles[type] ?: 0L) + count }
            partial.folderBytes.forEach { (folder, bytes) -> folderBytes[folder] = (folderBytes[folder] ?: 0L) + bytes }
            partial.folderFiles.forEach { (folder, count) -> folderFiles[folder] = (folderFiles[folder] ?: 0L) + count }
            largest.addAll(partial.largest)
            totalFiles += partial.totalFiles
            totalBytes += partial.totalBytes
            scannedTotal += partial.scanned
        }

        val topFolders = topLevel.filter { it.isDirectory }.map { it.absolutePath }.toSet()

        val categories = FileType.entries.mapNotNull { type ->
            val bytes = categoryBytes[type] ?: 0L
            if (bytes == 0L) null else CategoryUsage(type, bytes, categoryFiles[type] ?: 0L)
        }.sortedByDescending { it.bytes }

        val folders = topFolders.mapNotNull { path ->
            val bytes = folderBytes[path] ?: 0L
            val files = folderFiles[path] ?: 0L
            if (files == 0L || File(path).name.startsWith(".")) {
                null
            } else {
                FolderUsage(path, File(path).name, bytes, files)
            }
        }.sortedByDescending { it.bytes }

        val largestFiles = largest
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.length() }
            .take(LARGEST_KEEP)
            .map { it.toFileItem() }

        onProgress(scannedTotal, "")

        StorageAnalysis(
            rootPath = rootPath,
            volumeCapacity = root.totalSpace,
            totalFiles = totalFiles,
            totalBytes = totalBytes,
            categories = categories,
            largestFiles = largestFiles,
            folders = folders
        )
    }

    /** Aggregated result of scanning one subtree. */
    private class Partial {
        val categoryBytes = HashMap<FileType, Long>()
        val categoryFiles = HashMap<FileType, Long>()
        val folderBytes = HashMap<String, Long>()
        val folderFiles = HashMap<String, Long>()
        val largest = mutableListOf<File>()
        var totalFiles = 0L
        var totalBytes = 0L
        var scanned = 0L
    }

    /** Manual stack DFS with per-directory exception isolation. */
    private suspend fun walkSubtree(
        dir: File,
        folderKey: String,
        scanned: AtomicLong,
        onProgress: suspend (Long, String) -> Unit
    ): Partial {
        val partial = Partial()
        val stack = ArrayDeque<File>()
        stack.addLast(dir)
        while (stack.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val current = stack.removeLast()
            val children = try {
                current.listFiles()
            } catch (e: SecurityException) {
                null
            } ?: continue // unreadable directory — skip silently
            for (child in children) {
                if (child.isDirectory) {
                    stack.addLast(child)
                } else {
                    accumulateFile(child, partial, folderKey, scanned, onProgress)
                }
            }
        }
        return partial
    }

    private suspend fun walkFiles(
        files: Set<File>,
        folderKey: String?,
        scanned: AtomicLong,
        onProgress: suspend (Long, String) -> Unit
    ): Partial {
        val partial = Partial()
        for (file in files) {
            accumulateFile(file, partial, folderKey, scanned, onProgress)
        }
        return partial
    }

    private suspend fun accumulateFile(
        file: File,
        partial: Partial,
        folderKey: String?,
        scanned: AtomicLong,
        onProgress: suspend (Long, String) -> Unit
    ) {
        currentCoroutineContext().ensureActive()
        val count = scanned.incrementAndGet()
        if (count % PROGRESS_EVERY == 0L) onProgress(count, file.path)

        val length = file.length()
        partial.totalFiles++
        partial.totalBytes += length

        val fileType = FileType.fromExtension(file.extension.lowercase())
        partial.categoryBytes[fileType] = (partial.categoryBytes[fileType] ?: 0L) + length
        partial.categoryFiles[fileType] = (partial.categoryFiles[fileType] ?: 0L) + 1

        if (folderKey != null) {
            partial.folderBytes[folderKey] = (partial.folderBytes[folderKey] ?: 0L) + length
            partial.folderFiles[folderKey] = (partial.folderFiles[folderKey] ?: 0L) + 1
        }

        // Keep a small sorted-by-size list capped at LARGEST_KEEP
        if (partial.largest.size < LARGEST_KEEP || length > partial.largest.last().length()) {
            partial.largest.add(file)
            partial.largest.sortByDescending { it.length() }
            if (partial.largest.size > LARGEST_KEEP) {
                partial.largest.removeAt(partial.largest.size - 1)
            }
        }
        partial.scanned++
    }

    private fun File.toFileItem(): FileItem {
        val ext = extension.lowercase()
        return FileItem(
            name = name,
            path = absolutePath,
            size = length(),
            lastModified = lastModified(),
            isDirectory = isDirectory,
            fileType = if (isDirectory) FileType.OTHER else FileType.fromExtension(ext),
            fileFormat = if (isDirectory) FileFormat.UNKNOWN else FileFormat.fromExtension(ext),
            extension = ext
        )
    }

    private companion object {
        const val PROGRESS_EVERY = 300L
        const val LARGEST_KEEP = 30
    }
}
