package ir.heydar.nexfolder.ui.components.file

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import ir.heydar.nexfolder.core.icons.FileExtensionIcons
import ir.heydar.nexfolder.core.icons.FileIconResolver
import ir.heydar.nexfolder.core.icons.FolderIconResolver
import ir.heydar.nexfolder.domain.model.DirectoryType
import ir.heydar.nexfolder.domain.model.FileFormat
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.ui.theme.FileColorApk
import ir.heydar.nexfolder.ui.theme.FileColorArchive
import ir.heydar.nexfolder.ui.theme.FileColorAudio
import ir.heydar.nexfolder.ui.theme.FileColorDocument
import ir.heydar.nexfolder.ui.theme.FileColorImage
import ir.heydar.nexfolder.ui.theme.FileColorOther
import ir.heydar.nexfolder.ui.theme.FileColorVideo
import kotlin.math.absoluteValue

/**
 * File/folder icons rendered full-bleed — no wrapper badge, no border. The artwork
 * itself (colored page + glyph, tinted folder) is the single visual frame, already
 * scaled up inside its viewport. Images and videos keep real thumbnails.
 */
@Composable
fun FileIcon(
    fileItem: FileItem,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    when {
        fileItem.isDirectory -> DirectoryIcon(
            directoryType = fileItem.directoryType,
            modifier = modifier,
            size = size
        )
        fileItem.isImage -> ImageThumbnail(
            path = fileItem.path,
            modifier = modifier,
            size = size
        )
        fileItem.isVideo -> VideoThumbnail(
            path = fileItem.path,
            modifier = modifier,
            size = size
        )
        else -> FileTypeIcon(
            fileItem = fileItem,
            modifier = modifier,
            size = size
        )
    }
}

@Composable
fun DirectoryIcon(
    directoryType: DirectoryType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    FileGlyph(
        drawableRes = FolderIconResolver.resolve(directoryType),
        accent = folderAccentColor(directoryType),
        modifier = modifier,
        size = size,
        tintIcon = true
    )
}

@Composable
fun FileTypeIcon(
    fileItem: FileItem,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val kind = FileIconResolver.resolveKind(fileItem)
    if (kind != null) {
        FileGlyph(
            drawableRes = kind.icon,
            accent = Color(kind.color),
            modifier = modifier,
            size = size
        )
        return
    }

    // Unknown extension: label a badge with the extension so the file stays identifiable.
    if (fileItem.fileFormat == FileFormat.UNKNOWN && fileItem.fileType == FileType.OTHER) {
        extensionLabel(fileItem.extension)?.let { label ->
            ExtensionBadge(
                label = label,
                background = badgeColor(label),
                modifier = modifier,
                size = size
            )
            return
        }
    }

    FileGlyph(
        drawableRes = FileIconResolver.resolve(fileItem),
        accent = fileAccentColor(fileItem),
        modifier = modifier,
        size = size
    )
}

// ---------------------------------------------------------------------------
// Accent colours — shared with list rows, pills and section headers
// ---------------------------------------------------------------------------

@Composable
fun folderAccentColor(type: DirectoryType): Color = when (type) {
    DirectoryType.DOWNLOAD -> Color(0xFF2196F3)
    DirectoryType.DCIM, DirectoryType.CAMERA -> Color(0xFF4CAF50)
    DirectoryType.PICTURES, DirectoryType.SCREENSHOTS -> Color(0xFF9C27B0)
    DirectoryType.MOVIES -> Color(0xFFE91E63)
    DirectoryType.MUSIC, DirectoryType.RINGTONES,
    DirectoryType.NOTIFICATIONS, DirectoryType.ALARMS -> Color(0xFFFF9800)
    DirectoryType.DOCUMENTS -> Color(0xFF2196F3)
    DirectoryType.ANDROID -> Color(0xFF4CAF50)
    DirectoryType.WHATSAPP -> Color(0xFF25D366)
    DirectoryType.TELEGRAM -> Color(0xFF2196F3)
    DirectoryType.UNKNOWN -> MaterialTheme.colorScheme.primary
}

/** Accent that identifies [fileItem] across the UI (badge, extension pill, highlights). */
@Composable
fun fileAccentColor(fileItem: FileItem): Color {
    if (fileItem.isDirectory) return folderAccentColor(fileItem.directoryType)
    FileExtensionIcons.resolve(fileItem.extension)?.let { return Color(it.color) }
    return fileTypeAccent(fileItem.fileType)
}

private fun fileTypeAccent(type: FileType): Color = when (type) {
    FileType.IMAGE -> FileColorImage
    FileType.VIDEO -> FileColorVideo
    FileType.AUDIO -> FileColorAudio
    FileType.DOCUMENT -> FileColorDocument
    FileType.TEXT -> FileColorDocument
    FileType.ARCHIVE -> FileColorArchive
    FileType.APK -> FileColorApk
    FileType.OTHER -> FileColorOther
}

// ---------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------

@Composable
private fun FileGlyph(
    drawableRes: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tintIcon: Boolean = false
) {
    // Full-bleed artwork: single frame only (the icon's own artwork). Single-color
    // drawables (folders) take the accent tint; multi-color file art stays as-is.
    Icon(
        painter = painterResource(id = drawableRes),
        contentDescription = null,
        tint = if (tintIcon) accent else Color.Unspecified,
        modifier = modifier.size(size)
    )
}

private fun extensionLabel(extension: String): String? {
    val ext = extension.lowercase()
    if (ext.length !in 1..5) return null
    if (!ext.all { it.isLetterOrDigit() }) return null
    return ext.uppercase()
}

@Composable
private fun ExtensionBadge(
    label: String,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val fontSize = size.value * if (label.length > 3) 0.23f else 0.3f
    val shape = RoundedCornerShape(size * 0.30f)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(background.copy(alpha = 0.92f), background.copy(alpha = 0.68f))
                )
            )
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.22f), shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            maxLines = 1
        )
    }
}

private val badgeColors = listOf(
    Color(0xFF546E7A),
    Color(0xFF6D4C41),
    Color(0xFF00838F),
    Color(0xFF3949AB),
    Color(0xFF00897B),
    Color(0xFF8E24AA),
    Color(0xFF1565C0),
    Color(0xFF2E7D32)
)

private fun badgeColor(label: String): Color =
    badgeColors[label.hashCode().absoluteValue % badgeColors.size]

@Composable
private fun ImageThumbnail(
    path: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(path)
                .crossfade(true)
                .size(size.value.toInt() * 3)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            error = painterResource(android.R.drawable.ic_menu_gallery),
            placeholder = null
        )
    }
}

@Composable
private fun VideoThumbnail(
    path: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(path)
                .crossfade(true)
                // Coil needs the video decoder to extract a frame from a video file
                .decoderFactory(VideoFrameDecoder.Factory())
                .videoFrameMillis(1_000L)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            error = painterResource(android.R.drawable.ic_menu_gallery),
            placeholder = null
        )
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(size * 0.44f)
                .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(50))
                .padding(size * 0.05f)
        )
    }
}
