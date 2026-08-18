package com.heydar.filemanager.core.util

object MimeTypeHelper {
    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return mimeTypes[extension] ?: "*/*"
    }

    private val mimeTypes = mapOf(
        // Image
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "gif" to "image/gif",
        "bmp" to "image/bmp",
        "webp" to "image/webp",
        "heic" to "image/heic",
        "heif" to "image/heif",
        "tiff" to "image/tiff",
        "tif" to "image/tiff",
        "svg" to "image/svg+xml",
        "ico" to "image/x-icon",
        // Video
        "mp4" to "video/mp4",
        "mkv" to "video/x-matroska",
        "avi" to "video/x-msvideo",
        "mov" to "video/quicktime",
        "wmv" to "video/x-ms-wmv",
        "webm" to "video/webm",
        "flv" to "video/x-flv",
        "3gp" to "video/3gpp",
        "ts" to "video/mp2t",
        "m4v" to "video/x-m4v",
        // Audio
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "flac" to "audio/flac",
        "aac" to "audio/aac",
        "ogg" to "audio/ogg",
        "m4a" to "audio/mp4",
        "wma" to "audio/x-ms-wma",
        "opus" to "audio/opus",
        "amr" to "audio/amr",
        "mid" to "audio/midi",
        "midi" to "audio/midi",
        // Document
        "pdf" to "application/pdf",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "txt" to "text/plain",
        "rtf" to "application/rtf",
        "csv" to "text/csv",
        "epub" to "application/epub+zip",
        "html" to "text/html",
        "htm" to "text/html",
        // Archive
        "zip" to "application/zip",
        "rar" to "application/vnd.rar",
        "7z" to "application/x-7z-compressed",
        "tar" to "application/x-tar",
        "gz" to "application/gzip",
        "bz2" to "application/x-bzip2",
        // APK
        "apk" to "application/vnd.android.package-archive",
        "apks" to "application/vnd.android.package-archive"
    )
}
