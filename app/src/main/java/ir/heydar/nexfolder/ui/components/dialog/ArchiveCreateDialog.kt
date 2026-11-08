package ir.heydar.nexfolder.ui.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ir.heydar.nexfolder.core.archive.ArchiveFormat
import ir.heydar.nexfolder.ui.theme.Spacing

/** Format + name + optional password for creating a new archive. */
@Composable
fun ArchiveCreateDialog(
    itemCount: Int,
    defaultName: String,
    onConfirm: (format: ArchiveFormat, archiveName: String, password: CharArray?) -> Unit,
    onDismiss: () -> Unit
) {
    var format by remember { mutableStateOf(ArchiveFormat.ZIP) }
    var name by remember { mutableStateOf(defaultName) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compress $itemCount item(s)") },
        text = {
            Column {
                Text(
                    text = "Format",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    FilterChip(
                        selected = format == ArchiveFormat.ZIP,
                        onClick = { format = ArchiveFormat.ZIP },
                        label = { Text("ZIP") },
                        modifier = Modifier.padding(end = Spacing.sm)
                    )
                    FilterChip(
                        selected = format == ArchiveFormat.SEVEN_ZIP,
                        onClick = { format = ArchiveFormat.SEVEN_ZIP },
                        label = { Text("7Z") },
                        modifier = Modifier.padding(end = Spacing.sm)
                    )
                    FilterChip(
                        selected = format == ArchiveFormat.TAR_GZ,
                        onClick = { format = ArchiveFormat.TAR_GZ },
                        label = { Text("TAR.GZ") }
                    )
                }

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Archive name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm)
                )

                if (format == ArchiveFormat.ZIP || format == ArchiveFormat.SEVEN_ZIP) {
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (optional)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.sm)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        format,
                        name.trim().ifEmpty { "archive" },
                        password.takeIf { it.isNotBlank() }?.toCharArray()
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
