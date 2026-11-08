package ir.heydar.nexfolder.core.archive

import ir.heydar.nexfolder.core.files.OperationProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Supported archive families. */
enum class ArchiveFormat(val displayName: String, val extension: String) {
    ZIP("ZIP", "zip"),
    SEVEN_ZIP("7Z", "7z"),
    TAR("TAR", "tar"),
    TAR_GZ("TAR.GZ", "tar.gz")
}

/** Thrown when an archive is encrypted and no (or a wrong) password was supplied. */
class ArchivePasswordException(message: String = "Password required or incorrect") :
    Exception(message)

data class ArchiveEntry(
    val path: String,
    val name: String,
    val size: Long,
    val compressedSize: Long?,
    val isDirectory: Boolean,
    val lastModified: Long
)

/**
 * Unified archive facade: list / extract / create for ZIP (with AES encryption via
 * zip4j) and 7Z / TAR / TAR.GZ (via commons-compress, 7Z with AES-256 password).
 */
@Singleton
class ArchiveManager @Inject constructor() {

    private fun formatOf(path: String): ArchiveFormat? {
        val name = path.lowercase()
        return when {
            name.endsWith(".tar.gz") || name.endsWith(".tgz") -> ArchiveFormat.TAR_GZ
            name.endsWith(".tar") -> ArchiveFormat.TAR
            name.endsWith(".7z") -> ArchiveFormat.SEVEN_ZIP
            name.endsWith(".zip") -> ArchiveFormat.ZIP
            else -> null
        }
    }

    suspend fun listEntries(
        archivePath: String,
        password: CharArray? = null
    ): List<ArchiveEntry> = withContext(Dispatchers.IO) {
        when (formatOf(archivePath)) {
            ArchiveFormat.ZIP -> listZip(archivePath, password)
            ArchiveFormat.SEVEN_ZIP -> listSevenZip(archivePath, password)
            ArchiveFormat.TAR -> listTar(archivePath, gzip = false)
            ArchiveFormat.TAR_GZ -> listTar(archivePath, gzip = true)
            null -> throw IOException("Unsupported archive format")
        }
    }

    suspend fun extract(
        archivePath: String,
        destinationDir: File,
        entryNames: Collection<String>? = null,
        password: CharArray? = null,
        onProgress: suspend (OperationProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): Boolean = withContext(Dispatchers.IO) {
        when (formatOf(archivePath)) {
            ArchiveFormat.ZIP -> extractZip(archivePath, destinationDir, entryNames, password, onProgress, isCancelled)
            ArchiveFormat.SEVEN_ZIP -> extractSevenZip(archivePath, destinationDir, entryNames, password, onProgress, isCancelled)
            ArchiveFormat.TAR -> extractTar(archivePath, destinationDir, entryNames, gzip = false, onProgress = onProgress, isCancelled = isCancelled)
            ArchiveFormat.TAR_GZ -> extractTar(archivePath, destinationDir, entryNames, gzip = true, onProgress = onProgress, isCancelled = isCancelled)
            null -> false
        }
    }

    suspend fun create(
        format: ArchiveFormat,
        sources: List<File>,
        output: File,
        password: CharArray? = null,
        onProgress: suspend (OperationProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): Boolean = withContext(Dispatchers.IO) {
        when (format) {
            ArchiveFormat.ZIP -> createZip(sources, output, password, onProgress, isCancelled)
            ArchiveFormat.SEVEN_ZIP -> createSevenZip(sources, output, password, onProgress, isCancelled)
            ArchiveFormat.TAR -> createTar(sources, output, gzip = false, onProgress = onProgress, isCancelled = isCancelled)
            ArchiveFormat.TAR_GZ -> createTar(sources, output, gzip = true, onProgress = onProgress, isCancelled = isCancelled)
        }
    }

    // ---------- ZIP (zip4j) ----------

    private fun listZip(path: String, password: CharArray?): List<ArchiveEntry> {
        val zipFile = ZipFile(path)
        if (zipFile.isEncrypted) {
            if (password == null) throw ArchivePasswordException("Archive is encrypted")
            zipFile.setPassword(password)
        }
        return try {
            zipFile.fileHeaders.map { header ->
                ArchiveEntry(
                    path = header.fileName,
                    name = header.fileName.trimEnd('/').substringAfterLast('/'),
                    size = header.uncompressedSize,
                    compressedSize = header.compressedSize,
                    isDirectory = header.isDirectory,
                    lastModified = header.lastModifiedTime
                )
            }
        } catch (e: ZipException) {
            if (e.type == ZipException.Type.WRONG_PASSWORD) {
                throw ArchivePasswordException()
            }
            throw e
        }
    }

    private suspend fun extractZip(
        path: String,
        destinationDir: File,
        entryNames: Collection<String>?,
        password: CharArray?,
        onProgress: suspend (OperationProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        val zipFile = ZipFile(path)
        if (zipFile.isEncrypted) {
            if (password == null) throw ArchivePasswordException("Archive is encrypted")
            zipFile.setPassword(password)
        }
        val headers = try {
            zipFile.fileHeaders
        } catch (e: ZipException) {
            if (e.type == ZipException.Type.WRONG_PASSWORD) throw ArchivePasswordException()
            throw e
        }
        val selected = if (entryNames == null) {
            headers
        } else {
            headers.filter { it.fileName in entryNames }
        }
        val total = selected.sumOf { it.uncompressedSize }
        var processed = 0L
        var done = 0
        try {
            for (header in selected) {
                if (isCancelled()) return false
                if (header.isDirectory) {
                    File(destinationDir, header.fileName).mkdirs()
                } else {
                    File(destinationDir, header.fileName).parentFile?.mkdirs()
                    zipFile.extractFile(header, destinationDir.absolutePath)
                }
                processed += header.uncompressedSize
                done++
                onProgress(
                    OperationProgress("Extracting", header.fileName, processed, total, done, selected.size)
                )
            }
        } catch (e: ZipException) {
            if (e.type == ZipException.Type.WRONG_PASSWORD) throw ArchivePasswordException()
            return false
        }
        return true
    }

    private suspend fun createZip(
        sources: List<File>,
        output: File,
        password: CharArray?,
        onProgress: suspend (OperationProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        val files = sources.flatMap { flatten(it) }
        if (files.isEmpty()) return false
        val zipFile = ZipFile(output)
        val parameters = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            if (password != null) {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
            }
        }
        if (password != null) zipFile.setPassword(password)
        return try {
            val total = files.sumOf { it.length() }
            var processed = 0L
            var cancelled = false
            // zip4j batches the whole archive; report progress per added file
            val batch = 8
            for (chunk in files.chunked(batch)) {
                if (isCancelled()) {
                    cancelled = true
                    break
                }
                zipFile.addFiles(chunk, parameters)
                processed += chunk.sumOf { it.length() }
                onProgress(
                    OperationProgress(
                        "Compressing",
                        chunk.last().name,
                        processed,
                        total,
                        processed.toInt(),
                        total.toInt()
                    )
                )
            }
            if (cancelled) {
                output.delete()
                return false
            }
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            output.delete()
            false
        }
    }

    // ---------- 7Z (commons-compress) ----------

    private fun listSevenZip(path: String, password: CharArray?): List<ArchiveEntry> {
        val sevenZ = openSevenZip(path, password)
        sevenZ.use { archive ->
            return archive.entries.map { entry ->
                ArchiveEntry(
                    path = entry.name,
                    name = entry.name.trimEnd('/').substringAfterLast('/'),
                    size = entry.size,
                    compressedSize = null,
                    isDirectory = entry.isDirectory,
                    lastModified = entry.lastModifiedDate?.time ?: 0L
                )
            }
        }
    }

    private fun openSevenZip(path: String, password: CharArray?): SevenZFile {
        val file = File(path)
        return try {
            if (password != null) SevenZFile(file, password) else SevenZFile(file)
        } catch (e: IOException) {
            if (password == null && (e.message?.contains("password", ignoreCase = true) == true ||
                    e.message?.contains("encrypted", ignoreCase = true) == true)
            ) {
                throw ArchivePasswordException("Archive is encrypted")
            }
            if (password != null && e.message?.contains("password", ignoreCase = true) == true) {
                throw ArchivePasswordException()
            }
            throw e
        }
    }

    private suspend fun extractSevenZip(
        path: String,
        destinationDir: File,
        entryNames: Collection<String>?,
        password: CharArray?,
        onProgress: suspend (OperationProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        val sevenZ = openSevenZip(path, password)
        sevenZ.use { archive ->
            val total = archive.entries
                .filter { entryNames == null || it.name in entryNames }
                .sumOf { it.size }
            var processed = 0L
            var done = 0
            val buffer = ByteArray(64 * 1024)
            var entry: SevenZArchiveEntry? = archive.nextEntry
            while (entry != null) {
                if (isCancelled()) return false
                val wanted = entryNames == null || entry.name in entryNames
                val outFile = File(destinationDir, entry.name)
                if (entry.isDirectory) {
                    if (wanted) outFile.mkdirs()
                } else if (wanted) {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { output ->
                        var read: Int
                        while (archive.read(buffer, 0, buffer.size).also { read = it } > 0) {
                            if (isCancelled()) {
                                output.flush()
                                return false
                            }
                            output.write(buffer, 0, read)
                            processed += read
                        }
                    }
                    done++
                    onProgress(
                        OperationProgress("Extracting", entry.name, processed, total, done, total.toInt())
                    )
                } else {
                    // Skip unwanted entry content
                    val skip = ByteArray(64 * 1024)
                    while (archive.read(skip, 0, skip.size) > 0) { /* drain */ }
                }
                entry = archive.nextEntry
            }
            return true
        }
    }

    private suspend fun createSevenZip(
        sources: List<File>,
        output: File,
        password: CharArray?,
        onProgress: suspend (OperationProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        return try {
            val out = if (password != null) SevenZOutputFile(output, password) else SevenZOutputFile(output)
            out.use { archive ->
                val plan = mutableListOf<Pair<File, String>>()
                sources.forEach { source ->
                    val base = source.name
                    if (source.isFile) {
                        plan += source to base
                    } else {
                        source.walkTopDown().filter { it.isFile }.forEach { file ->
                            plan += file to (base + "/" + file.relativeTo(source).path)
                        }
                    }
                }
                val total = plan.sumOf { (file, _) -> file.length() }
                var processed = 0L
                var cancelled = false
                val buffer = ByteArray(64 * 1024)
                for ((file, entryName) in plan) {
                    if (isCancelled()) {
                        cancelled = true
                        break
                    }
                    val entry = archive.createArchiveEntry(file, entryName)
                    archive.putArchiveEntry(entry)
                    FileInputStream(file).use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } > 0) {
                            archive.write(buffer, 0, read)
                            processed += read
                        }
                    }
                    archive.closeArchiveEntry()
                    onProgress(OperationProgress("Compressing", file.name, processed, total, plan.size, plan.size))
                }
                if (cancelled) {
                    output.delete()
                    return false
                }
            }
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            output.delete()
            false
        }
    }

    // ---------- TAR / TAR.GZ (commons-compress) ----------

    private fun listTar(path: String, gzip: Boolean): List<ArchiveEntry> {
        FileInputStream(File(path)).use { input ->
            val stream = if (gzip) {
                TarArchiveInputStream(GzipCompressorInputStream(input))
            } else {
                TarArchiveInputStream(input)
            }
            return stream.use { tar ->
                val entries = mutableListOf<ArchiveEntry>()
                var entry = tar.nextTarEntry
                while (entry != null) {
                    entries += ArchiveEntry(
                        path = entry.name,
                        name = entry.name.trimEnd('/').substringAfterLast('/'),
                        size = entry.size,
                        compressedSize = null,
                        isDirectory = entry.isDirectory,
                        lastModified = entry.modTime.time
                    )
                    entry = tar.nextTarEntry
                }
                entries
            }
        }
    }

    private suspend fun extractTar(
        path: String,
        destinationDir: File,
        entryNames: Collection<String>?,
        gzip: Boolean,
        onProgress: suspend (OperationProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        FileInputStream(File(path)).use { rawInput ->
            val input = if (gzip) org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream(rawInput) else rawInput
            TarArchiveInputStream(input).use { tar ->
                var entry = tar.nextTarEntry
                val buffer = ByteArray(64 * 1024)
                var done = 0
                while (entry != null) {
                    if (isCancelled()) return false
                    val wanted = entryNames == null || entry.name in entryNames
                    if (wanted) {
                        val outFile = File(destinationDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { output ->
                                var read: Int
                                while (tar.read(buffer).also { read = it } > 0) {
                                    if (isCancelled()) return false
                                    output.write(buffer, 0, read)
                                }
                            }
                            done++
                            onProgress(OperationProgress("Extracting", entry.name, done.toLong(), done.toLong(), done, done))
                        }
                    }
                    entry = tar.nextTarEntry
                }
                return true
            }
        }
    }

    private suspend fun createTar(
        sources: List<File>,
        output: File,
        gzip: Boolean,
        onProgress: suspend (OperationProgress) -> Unit,
        isCancelled: () -> Boolean
    ): Boolean {
        return try {
            FileOutputStream(output).use { rawOutput ->
                val wrapped = if (gzip) GzipCompressorOutputStream(rawOutput) else rawOutput
                TarArchiveOutputStream(wrapped).use { tar ->
                    val plan = mutableListOf<Pair<File, String>>()
                    sources.forEach { source ->
                        if (source.isFile) {
                            plan += source to source.name
                        } else {
                            source.walkTopDown().forEach { file ->
                                val rel = file.relativeTo(source).path
                                plan += file to (source.name + "/" + rel)
                            }
                        }
                    }
                    val total = plan.count { (file, _) -> file.isFile }
                    var done = 0
                    var cancelled = false
                    val buffer = ByteArray(64 * 1024)
                    for ((file, entryName) in plan) {
                        if (isCancelled()) {
                            cancelled = true
                            break
                        }
                        val entry = TarArchiveEntry(file, entryName)
                        tar.putArchiveEntry(entry)
                        if (file.isFile) {
                            FileInputStream(file).use { input ->
                                var read: Int
                                while (input.read(buffer).also { read = it } > 0) {
                                    tar.write(buffer, 0, read)
                                }
                            }
                            done++
                            onProgress(OperationProgress("Compressing", file.name, done.toLong(), total.toLong(), done, total))
                        }
                        tar.closeArchiveEntry()
                    }
                    if (cancelled) {
                        output.delete()
                        return false
                    }
                }
            }
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            output.delete()
            false
        }
    }

    private fun flatten(source: File): List<File> =
        if (source.isFile) {
            listOf(source)
        } else {
            source.walkTopDown().filter { it.isFile }.toList()
        }
}
