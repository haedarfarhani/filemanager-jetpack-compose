package ir.heydar.nexfolder.core.icons

import androidx.annotation.DrawableRes
import ir.heydar.nexfolder.domain.model.FileFormat
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.FileType

/**
 * Centralized resolver that maps FileItem to drawable resource IDs.
 * Single source of truth for file type icon resolution.
 */
object FileIconResolver {

    @DrawableRes
    fun resolve(fileItem: FileItem): Int {
        // 1. Prioritize specific formats
        if (fileItem.fileFormat != FileFormat.UNKNOWN) {
            val specificIcon = resolveSpecificFormat(fileItem.fileFormat)
            if (specificIcon != null) return specificIcon
        }

        // 2. Format family of the extension (pdf, zip, mp3, psd, dwg, ...)
        FileExtensionIcons.resolve(fileItem.extension)?.let { return it.icon }

        // 3. Fallback to generic FileType
        return resolveGenericType(fileItem.fileType)
    }

    /**
     * Format family driving the icon's color, or `null` when the file only has a
     * generic type icon or a format-specific drawable that defines its own color.
     */
    fun resolveKind(fileItem: FileItem): FileIconKind? {
        val hasSpecificIcon = fileItem.fileFormat != FileFormat.UNKNOWN &&
            resolveSpecificFormat(fileItem.fileFormat) != null
        if (hasSpecificIcon) return null
        return FileExtensionIcons.resolve(fileItem.extension)
    }

    private fun resolveSpecificFormat(format: FileFormat): Int? {
        return when (format) {
            // Video
            FileFormat.MP4 -> AppIcons.File.Video.mp4
            FileFormat.MKV -> AppIcons.File.Video.mkv
            FileFormat.AVI -> AppIcons.File.Video.avi
            FileFormat.MOV -> AppIcons.File.Video.mov
            FileFormat.WEBM -> AppIcons.File.Video.webm
            FileFormat.GP3 -> AppIcons.File.Video.gp3
            FileFormat.FLV -> AppIcons.File.Video.flv
            FileFormat.MPEG -> AppIcons.File.Video.mpeg
            FileFormat.MPG -> AppIcons.File.Video.mpg
            FileFormat.M4V -> AppIcons.File.Video.m4v
            FileFormat.TS -> AppIcons.File.Video.ts
            
            // Image
            FileFormat.JPG -> AppIcons.File.Image.jpg
            FileFormat.JPEG -> AppIcons.File.Image.jpeg
            FileFormat.PNG -> AppIcons.File.Image.png
            FileFormat.WEBP -> AppIcons.File.Image.webp
            FileFormat.GIF -> AppIcons.File.Image.gif
            FileFormat.BMP -> AppIcons.File.Image.bmp
            FileFormat.HEIC -> AppIcons.File.Image.heic
            FileFormat.HEIF -> AppIcons.File.Image.heif
            FileFormat.SVG -> AppIcons.File.Image.svg
            FileFormat.TIFF -> AppIcons.File.Image.tiff
            FileFormat.TIF -> AppIcons.File.Image.tif
            FileFormat.ICO -> AppIcons.File.Image.ico
            FileFormat.AVIF -> AppIcons.File.Image.avif
            
            else -> null
        }
    }

    private fun resolveGenericType(fileType: FileType): Int {
        return when (fileType) {
            FileType.IMAGE -> AppIcons.File.image
            FileType.VIDEO -> AppIcons.File.video
            FileType.AUDIO -> AppIcons.File.audio
            FileType.DOCUMENT -> AppIcons.File.document
            FileType.ARCHIVE -> AppIcons.File.archive
            FileType.APK -> AppIcons.File.apk
            FileType.TEXT -> AppIcons.File.text
            FileType.OTHER -> AppIcons.File.unknown
        }
    }
}
