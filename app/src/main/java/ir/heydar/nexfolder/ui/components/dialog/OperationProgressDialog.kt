package ir.heydar.nexfolder.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.heydar.nexfolder.core.files.OperationProgress
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import ir.heydar.nexfolder.ui.theme.Spacing

/** Shows byte-accurate progress for a long-running file operation with cancel. */
@Composable
fun OperationProgressDialog(
    progress: OperationProgress,
    onCancel: () -> Unit
) {
    val fraction = if (progress.totalBytes > 0) {
        (progress.processedBytes.toFloat() / progress.totalBytes).coerceIn(0f, 1f)
    } else 0f

    AlertDialog(
        onDismissRequest = { /* must use cancel button */ },
        title = { Text(progress.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (progress.currentFile.isNotEmpty()) {
                    Text(
                        text = progress.currentFile,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
                Text(
                    text = FileSizeFormatter.formatDetailed(
                        progress.processedBytes,
                        progress.totalBytes
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
