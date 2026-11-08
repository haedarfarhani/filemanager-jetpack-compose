package ir.heydar.nexfolder.ui.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.ui.components.file.FileIcon
import ir.heydar.nexfolder.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionsBottomSheet(
    fileItem: FileItem,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onBrowseArchive: (() -> Unit)? = null,
    onCompress: () -> Unit = {},
    onOpenWith: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // File header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileIcon(fileItem = fileItem, size = Spacing.iconSize)
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    if (!fileItem.isDirectory) {
                        Text(
                            text = fileItem.extension.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Spacing.md),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Actions
            if (onBrowseArchive != null) {
                ActionItem(
                    drawableRes = ir.heydar.nexfolder.core.icons.AppIcons.File.archive,
                    label = "Browse contents",
                    onClick = { onBrowseArchive(); onDismiss() }
                )
            }
            ActionItem(
                drawableRes = AppIcons.Action.copy,
                label = "Copy",
                onClick = { onCopy(); onDismiss() }
            )
            ActionItem(
                drawableRes = AppIcons.Action.move,
                label = "Move",
                onClick = { onMove(); onDismiss() }
            )
            ActionItem(
                drawableRes = AppIcons.Action.rename,
                label = "Rename",
                onClick = { onRename(); onDismiss() }
            )
            ActionItem(
                drawableRes = ir.heydar.nexfolder.core.icons.AppIcons.File.archive,
                label = "Compress…",
                onClick = { onCompress(); onDismiss() }
            )
            if (!fileItem.isDirectory && onOpenWith != null) {
                ActionItem(
                    imageVector = Icons.Rounded.OpenInNew,
                    label = "Open with…",
                    onClick = { onOpenWith(); onDismiss() }
                )
            }
            if (!fileItem.isDirectory) {
                ActionItem(
                    drawableRes = AppIcons.Action.share,
                    label = "Share",
                    onClick = { onShare(); onDismiss() }
                )
            }
            ActionItem(
                drawableRes = AppIcons.Action.more,
                label = "Details",
                onClick = { onDetails(); onDismiss() }
            )
            ActionItem(
                drawableRes = AppIcons.Action.delete,
                label = "Delete",
                tint = MaterialTheme.colorScheme.error,
                onClick = { onDelete(); onDismiss() }
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun ActionItem(
    drawableRes: Int? = null,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        when {
            imageVector != null -> Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            drawableRes != null -> Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
