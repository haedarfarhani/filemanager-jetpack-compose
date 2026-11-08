package ir.heydar.nexfolder.domain.model

enum class FileFormat(val extension: String, val type: FileType) {
    // Video
    MP4("mp4", FileType.VIDEO),
    MKV("mkv", FileType.VIDEO),
    AVI("avi", FileType.VIDEO),
    MOV("mov", FileType.VIDEO),
    WEBM("webm", FileType.VIDEO),
    GP3("3gp", FileType.VIDEO),
    FLV("flv", FileType.VIDEO),
    MPEG("mpeg", FileType.VIDEO),
    MPG("mpg", FileType.VIDEO),
    M4V("m4v", FileType.VIDEO),
    TS("ts", FileType.VIDEO),

    // Image
    JPG("jpg", FileType.IMAGE),
    JPEG("jpeg", FileType.IMAGE),
    PNG("png", FileType.IMAGE),
    WEBP("webp", FileType.IMAGE),
    GIF("gif", FileType.IMAGE),
    BMP("bmp", FileType.IMAGE),
    HEIC("heic", FileType.IMAGE),
    HEIF("heif", FileType.IMAGE),
    SVG("svg", FileType.IMAGE),
    TIFF("tiff", FileType.IMAGE),
    TIF("tif", FileType.IMAGE),
    ICO("ico", FileType.IMAGE),
    AVIF("avif", FileType.IMAGE),

    UNKNOWN("", FileType.OTHER);

    companion object {
        fun fromExtension(extension: String): FileFormat {
            val ext = extension.lowercase().trimStart('.')
            return entries.find { it.extension == ext } ?: UNKNOWN
        }
    }
}
