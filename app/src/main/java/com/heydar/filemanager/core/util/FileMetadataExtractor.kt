package com.heydar.filemanager.core.util

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.heydar.filemanager.domain.model.FileDetails
import com.heydar.filemanager.domain.model.FileFormat
import com.heydar.filemanager.domain.model.FileItem
import com.heydar.filemanager.domain.model.FileType
import com.heydar.filemanager.domain.model.CommonFileInfo
import com.heydar.filemanager.domain.model.SpecificFileInfo
import java.io.File

/**
 * Extracts file metadata on IO dispatcher.
 * Never call from Main thread.
 */
object FileMetadataExtractor {

    /**
     * Build a complete [FileDetails] for the given [filePath].
     * For folders, [SpecificFileInfo.FolderInfo.totalSize] is null —
     * call [computeFolderSize] separately in a background coroutine.
     */
    fun extract(context: Context, filePath: String): FileDetails {
        val file = File(filePath)
        val ext = file.extension.lowercase()
        val fileType = if (file.isDirectory) FileType.OTHER else FileType.fromExtension(ext)
        val fileFormat = if (file.isDirectory) FileFormat.UNKNOWN else FileFormat.fromExtension(ext)
        val mimeType = MimeTypeHelper.getMimeType(file.name)
            .takeIf { it != "*/*" } ?: "application/octet-stream"

        val fileItem = FileItem(
            name = file.name,
            path = filePath,
            size = if (file.isDirectory) 0L else file.length(),
            lastModified = file.lastModified(),
            isDirectory = file.isDirectory,
            fileType = fileType,
            fileFormat = fileFormat,
            extension = ext
        )

        val common = CommonFileInfo(
            name = file.name,
            path = filePath,
            extension = ext,
            size = if (file.isDirectory) 0L else file.length(),
            lastModified = file.lastModified(),
            mimeType = mimeType,
            fileType = fileType,
            fileItem = fileItem
        )

        val specific: SpecificFileInfo = when {
            file.isDirectory -> extractFolderInfo(file)
            fileType == FileType.IMAGE -> extractImageInfo(filePath)
            fileType == FileType.VIDEO -> extractVideoInfo(filePath)
            fileType == FileType.AUDIO -> extractAudioInfo(filePath)
            fileType == FileType.APK   -> extractApkInfo(context, filePath)
            else -> SpecificFileInfo.GenericInfo
        }

        return FileDetails(common = common, specific = specific)
    }

    /** Counts direct children — does NOT recursively calculate size (pass null). */
    private fun extractFolderInfo(dir: File): SpecificFileInfo.FolderInfo {
        val children = dir.listFiles() ?: emptyArray()
        val fileCount   = children.count { it.isFile }
        val folderCount = children.count { it.isDirectory }
        return SpecificFileInfo.FolderInfo(
            fileCount   = fileCount,
            folderCount = folderCount,
            totalSize   = null          // caller launches lazy computation
        )
    }

    /** Reads image dimensions without decoding pixels into memory. */
    private fun extractImageInfo(path: String): SpecificFileInfo {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                SpecificFileInfo.ImageInfo(opts.outWidth, opts.outHeight)
            } else {
                SpecificFileInfo.GenericInfo
            }
        } catch (e: Exception) {
            SpecificFileInfo.GenericInfo
        }
    }

    private fun extractVideoInfo(path: String): SpecificFileInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val width = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val height = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            SpecificFileInfo.VideoInfo(durationMs = duration, width = width, height = height)
        } catch (e: Exception) {
            SpecificFileInfo.GenericInfo
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun extractAudioInfo(path: String): SpecificFileInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            SpecificFileInfo.AudioInfo(durationMs = duration)
        } catch (e: Exception) {
            SpecificFileInfo.GenericInfo
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun extractApkInfo(context: Context, path: String): SpecificFileInfo {
        return try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageArchiveInfo(path, 0)
                ?: return SpecificFileInfo.GenericInfo
            val appInfo = info.applicationInfo ?: return SpecificFileInfo.GenericInfo
            appInfo.sourceDir = path
            appInfo.publicSourceDir = path
            SpecificFileInfo.ApkInfo(
                packageName  = info.packageName,
                versionName  = info.versionName ?: "",
                versionCode  = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
                    info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong(),
                minSdk    = appInfo.minSdkVersion,
                targetSdk = appInfo.targetSdkVersion
            )
        } catch (e: Exception) {
            SpecificFileInfo.GenericInfo
        }
    }

    /** Call this on IO dispatcher after initial details are shown. */
    fun computeFolderSize(path: String): Long {
        return try {
            File(path).walkBottomUp()
                .filter { it.isFile }
                .sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    /** Format milliseconds as HH:mm:ss or mm:ss */
    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours   = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
