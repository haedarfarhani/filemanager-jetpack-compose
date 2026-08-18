package com.heydar.filemanager.domain.model

/**
 * Common metadata available for every file or folder.
 */
data class CommonFileInfo(
    val name: String,
    val path: String,
    val extension: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String,
    val fileType: FileType,
    val fileItem: FileItem          // kept for icon resolution
)

/**
 * Type-specific metadata, loaded on IO dispatcher.
 */
sealed interface SpecificFileInfo {

    /** Image files — dimensions extracted without loading the full bitmap */
    data class ImageInfo(val width: Int, val height: Int) : SpecificFileInfo

    /** Video files */
    data class VideoInfo(
        val durationMs: Long,
        val width: Int,
        val height: Int
    ) : SpecificFileInfo

    /** Audio files */
    data class AudioInfo(val durationMs: Long) : SpecificFileInfo

    /** APK files */
    data class ApkInfo(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val minSdk: Int,
        val targetSdk: Int
    ) : SpecificFileInfo

    /**
     * Folder — itemCount and folderCount are loaded immediately,
     * totalSize is null until the background calculation finishes.
     */
    data class FolderInfo(
        val fileCount: Int,
        val folderCount: Int,
        val totalSize: Long?        // null = still calculating
    ) : SpecificFileInfo

    /** All other file types — no extra metadata */
    object GenericInfo : SpecificFileInfo
}

/**
 * Complete details model passed to the UI.
 */
data class FileDetails(
    val common: CommonFileInfo,
    val specific: SpecificFileInfo
)
