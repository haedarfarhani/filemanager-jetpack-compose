package com.heydar.filemanager.ui.components.file

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.heydar.filemanager.core.icons.FileIconResolver
import com.heydar.filemanager.core.icons.FolderIconResolver
import com.heydar.filemanager.domain.model.DirectoryType
import com.heydar.filemanager.domain.model.FileItem
import com.heydar.filemanager.domain.model.FileType
import com.heydar.filemanager.ui.components.icon.AppIcon
import com.heydar.filemanager.ui.theme.FileColorApk
import com.heydar.filemanager.ui.theme.FileColorArchive
import com.heydar.filemanager.ui.theme.FileColorAudio
import com.heydar.filemanager.ui.theme.FileColorDocument
import com.heydar.filemanager.ui.theme.FileColorImage
import com.heydar.filemanager.ui.theme.FileColorOther
import com.heydar.filemanager.ui.theme.FileColorVideo

@Composable
fun FileIcon(
    fileItem: FileItem,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    if (fileItem.isDirectory) {
        DirectoryIcon(
            directoryType = fileItem.directoryType,
            modifier = modifier,
            size = size
        )
    } else {
        when {
            fileItem.isImage -> {
                ImageThumbnail(
                    path = fileItem.path,
                    modifier = modifier,
                    size = size
                )
            }
            fileItem.isVideo -> {
                VideoThumbnail(
                    path = fileItem.path,
                    modifier = modifier,
                    size = size
                )
            }
            else -> {
                FileTypeIcon(
                    fileItem = fileItem,
                    modifier = modifier,
                    size = size
                )
            }
        }
    }
}

@Composable
fun DirectoryIcon(
    directoryType: DirectoryType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val drawableRes = FolderIconResolver.resolve(directoryType)
    val tint = getDirectoryTintColor(directoryType)

    IconBox(
        drawableRes = drawableRes,
        tint = tint,
        backgroundColor = tint.copy(alpha = 0.12f),
        modifier = modifier,
        size = size
    )
}

@Composable
fun FileTypeIcon(
    fileItem: FileItem,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val drawableRes = FileIconResolver.resolve(fileItem)
    val color = getFileTypeColor(fileItem.fileType)

    IconBox(
        drawableRes = drawableRes,
        tint = color,
        backgroundColor = color.copy(alpha = 0.12f),
        modifier = modifier,
        size = size
    )
}

@Composable
private fun ImageThumbnail(
    path: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val cornerShape = RoundedCornerShape(8.dp)
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(path)
            .crossfade(true)
            .size(size.value.toInt() * 3)
            .build(),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(cornerShape),
        contentScale = ContentScale.Crop,
        error = painterResource(
            android.R.drawable.ic_menu_gallery
        ),
        placeholder = null
    )
}

@Composable
private fun VideoThumbnail(
    path: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val cornerShape = RoundedCornerShape(8.dp)
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(path)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(cornerShape),
        contentScale = ContentScale.Crop,
        error = painterResource(android.R.drawable.ic_menu_gallery),
        placeholder = null
    )
}

@Composable
private fun IconBox(
    drawableRes: Int,
    tint: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(
            icon = drawableRes,
            contentDescription = null,
            tint = tint,
            size = size * 0.55f
        )
    }
}

@Composable
private fun getDirectoryTintColor(type: DirectoryType): Color {
    return when (type) {
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
        DirectoryType.UNKNOWN -> MaterialTheme.colorScheme.secondary
    }
}

private fun getFileTypeColor(type: FileType): Color {
    return when (type) {
        FileType.IMAGE -> FileColorImage
        FileType.VIDEO -> FileColorVideo
        FileType.AUDIO -> FileColorAudio
        FileType.DOCUMENT -> FileColorDocument
        FileType.TEXT -> FileColorDocument
        FileType.ARCHIVE -> FileColorArchive
        FileType.APK -> FileColorApk
        FileType.OTHER -> FileColorOther
    }
}
