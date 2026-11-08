package ir.heydar.nexfolder.ui.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.domain.model.FileItem

@Composable
fun DeleteConfirmationDialog(
    fileItem: FileItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(id = AppIcons.Action.delete),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Delete ${if (fileItem.isDirectory) "folder" else "file"}?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "\"${fileItem.name}\" will be permanently deleted and cannot be recovered.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
