package com.heydar.filemanager.domain.model

enum class FileType(val extensions: List<String>) {
    IMAGE(
        listOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "heic", "heif", "tiff", "tif", "svg", "ico", "avif"
        )
    ),
    VIDEO(
        listOf(
            "mp4", "mkv", "avi", "mov", "wmv", "webm",
            "flv", "3gp", "ts", "m4v", "mpeg", "mpg"
        )
    ),
    AUDIO(
        listOf(
            "mp3", "wav", "flac", "aac", "ogg", "m4a",
            "wma", "opus", "amr", "mid", "midi"
        )
    ),
    DOCUMENT(
        listOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "rtf", "odt", "ods", "odp", "epub", "mobi"
        )
    ),
    TEXT(
        listOf(
            "txt", "log", "md", "csv", "json", "xml", "html", "css", 
            "js", "kt", "java", "py", "go", "c", "cpp", "h", "cs"
        )
    ),
    ARCHIVE(
        listOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz",
            "tar.gz", "tar.bz2", "tgz", "cab"
        )
    ),
    APK(
        listOf("apk", "apks", "xapk")
    ),
    OTHER(emptyList());

    companion object {
        fun fromExtension(extension: String): FileType {
            val ext = extension.lowercase().trimStart('.')
            return entries.find { type ->
                type != OTHER && type.extensions.contains(ext)
            } ?: OTHER
        }
    }
}
