package ir.heydar.nexfolder.ui.screens.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.archive.ArchiveEntry
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import ir.heydar.nexfolder.ui.components.dialog.DirectoryPickerDialog
import ir.heydar.nexfolder.ui.components.dialog.OperationProgressDialog
import ir.heydar.nexfolder.ui.theme.Spacing
import java.io.File

/**
 * Browse archive contents before extraction, with password support for
 * encrypted ZIP/7Z and selective or full extraction with progress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveBrowserScreen(
    archivePath: String,
    onNavigateBack: () -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDirectoryPicker by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    LaunchedEffect(state.errorMessage) {
        // errors surface via snackbar-less inline text; nothing to auto-handle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = File(archivePath).name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${state.entries.size} item(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(painterResource(id = AppIcons.Nav.back), contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.requestExtractAll()
                        showDirectoryPicker = true
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Archive,
                            contentDescription = "Extract all"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Spacing.md)
                    ) {
                        items(state.entries, key = { it.path }) { entry ->
                            ArchiveEntryRow(
                                entry = entry,
                                onClick = {
                                    viewModel.requestExtractEntry(entry)
                                    showDirectoryPicker = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Password dialog
    if (state.needsPassword) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ArchiveEvent.PasswordDismissed) },
            title = { Text("Encrypted archive") },
            text = {
                Column {
                    Text(
                        text = if (state.passwordRejected) {
                            "The password was rejected. Try again:"
                        } else {
                            "This archive is password protected:"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.padding(top = Spacing.sm))
                    TextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = state.passwordRejected
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(ArchiveEvent.PasswordEntered(passwordInput))
                        passwordInput = ""
                    },
                    enabled = passwordInput.isNotEmpty()
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ArchiveEvent.PasswordDismissed) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDirectoryPicker && !state.needsPassword) {
        DirectoryPickerDialog(
            title = if (state.pendingExtractEntry == null) "Extract all to" else "Extract 1 item to",
            initialPath = File(archivePath).parent ?: "",
            onPathSelected = { destination ->
                showDirectoryPicker = false
                viewModel.onEvent(ArchiveEvent.ExtractConfirmed(destination))
            },
            onDismiss = { showDirectoryPicker = false }
        )
    }

    state.activeOperation?.let { progress ->
        OperationProgressDialog(
            progress = progress,
            onCancel = { viewModel.onEvent(ArchiveEvent.CancelOperation) }
        )
    }
}

@Composable
private fun ArchiveEntryRow(
    entry: ArchiveEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                id = if (entry.isDirectory) AppIcons.Folder.default else AppIcons.File.document
            ),
            contentDescription = null,
            tint = if (entry.isDirectory) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.path.contains('/')) {
                Text(
                    text = entry.path.substringBeforeLast('/'),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!entry.isDirectory) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FileSizeFormatter.format(entry.size),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                entry.compressedSize?.let { compressed ->
                    Text(
                        text = FileSizeFormatter.format(compressed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
