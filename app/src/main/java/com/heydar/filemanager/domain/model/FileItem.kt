package com.heydar.filemanager.domain.model

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val fileType: FileType,
    val fileFormat: FileFormat = FileFormat.UNKNOWN,
    val directoryType: DirectoryType = DirectoryType.UNKNOWN,
    val extension: String = ""
) {
    val isImage: Boolean get() = fileType == FileType.IMAGE
    val isVideo: Boolean get() = fileType == FileType.VIDEO
    val isAudio: Boolean get() = fileType == FileType.AUDIO
    val isApk: Boolean get() = fileType == FileType.APK
    val isText: Boolean get() = fileType == FileType.TEXT
}
