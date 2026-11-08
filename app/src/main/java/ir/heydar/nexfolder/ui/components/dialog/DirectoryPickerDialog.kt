package ir.heydar.nexfolder.ui.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.heydar.nexfolder.core.icons.AppIcons
import java.io.File

/**
 * In-app destination folder picker. Shows only subdirectories of the current path with
 * up-navigation and a confirm action. Works without SAF because the app holds
 * MANAGE_EXTERNAL_STORAGE.
 */
@Composable
fun DirectoryPickerDialog(
    title: String,
    initialPath: String,
    onPathSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPath by remember {
        mutableStateOf(
            if (File(initialPath).isDirectory) initialPath else (File(initialPath).parentFile?.absolutePath ?: initialPath)
        )
    }
    val directories = remember(currentPath) {
        val dir = File(currentPath)
        (dir.listFiles()?.filter { it.isDirectory && !it.isHidden } ?: emptyList())
            .sortedBy { it.name.lowercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                // Breadcrumb + up navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            File(currentPath).parentFile?.let { parent ->
                                currentPath = parent.absolutePath
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = AppIcons.Nav.up),
                            contentDescription = "Up",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = currentPath.substringAfterLast('/').ifEmpty { currentPath },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(directories, key = { it.absolutePath }) { dir ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentPath = dir.absolutePath }
                                .padding(horizontal = 4.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(24.dp)) {
                                Icon(
                                    painter = painterResource(id = AppIcons.Folder.default),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = dir.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (directories.isEmpty()) {
                        item {
                            Text(
                                text = "No subfolders here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPathSelected(currentPath) }) {
                Text("Use this folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
