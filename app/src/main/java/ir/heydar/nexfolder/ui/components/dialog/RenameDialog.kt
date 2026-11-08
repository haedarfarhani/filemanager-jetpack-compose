package ir.heydar.nexfolder.ui.components.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.domain.model.FileItem

@Composable
fun RenameDialog(
    fileItem: FileItem,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialName = if (!fileItem.isDirectory && fileItem.extension.isNotEmpty()) {
        fileItem.name.removeSuffix(".${fileItem.extension}")
    } else {
        fileItem.name
    }
    val suffix = if (!fileItem.isDirectory && fileItem.extension.isNotEmpty()) ".${fileItem.extension}" else ""

    var textValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(0, initialName.length)
            )
        )
    }
    val isError = textValue.text.isBlank()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(id = AppIcons.Action.rename),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Rename",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text("Name") },
                suffix = if (suffix.isNotEmpty()) {
                    { Text(suffix, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else null,
                isError = isError,
                supportingText = if (isError) {
                    { Text("Name cannot be empty") }
                } else null,
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isError) onConfirm(textValue.text.trim() + suffix)
                },
                enabled = !isError
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
