package ir.heydar.nexfolder.ui.screens.network

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.domain.model.network.RemoteFileEntry
import ir.heydar.nexfolder.ui.theme.Spacing
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RemoteBrowseScreen(
    connectionId: String,
    onNavigateBack: () -> Unit,
    viewModel: RemoteBrowseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.onEvent(RemoteBrowseEvent.UploadUris(uris))
    }

    LaunchedEffect(connectionId) {
        viewModel.onEvent(RemoteBrowseEvent.Initialize(connectionId))
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(RemoteBrowseEvent.ErrorShown)
        }
    }

    BackHandler(enabled = state.pathHistory.isNotEmpty() || state.optionsEntry != null) {
        when {
            state.optionsEntry != null -> viewModel.onEvent(RemoteBrowseEvent.OptionsDismissed)
            state.pathHistory.isNotEmpty() -> viewModel.onEvent(RemoteBrowseEvent.Back)
        }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = state.connection?.name ?: "Remote",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.path,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (state.pathHistory.isNotEmpty()) {
                                viewModel.onEvent(RemoteBrowseEvent.Back)
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            uploadLauncher.launch(arrayOf("*/*"))
                        }) {
                            Icon(imageVector = Icons.Rounded.UploadFile, contentDescription = "Upload")
                        }
                        IconButton(onClick = {
                            viewModel.onEvent(RemoteBrowseEvent.ShowMkdirDialog(true))
                        }) {
                            Icon(imageVector = Icons.Rounded.CreateNewFolder, contentDescription = "New folder")
                        }
                        IconButton(onClick = { viewModel.onEvent(RemoteBrowseEvent.Refresh) }) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.entries.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.entries.isEmpty() && !state.isLoading -> {
                    Text(
                        text = "This folder is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Spacing.md)
                    ) {
                        items(state.entries, key = { it.path }) { entry ->
                            RemoteEntryRow(
                                entry = entry,
                                onClick = {
                                    if (entry.isDirectory) {
                                        viewModel.onEvent(RemoteBrowseEvent.EnterFolder(entry))
                                    } else {
                                        viewModel.onEvent(RemoteBrowseEvent.DownloadClicked(entry))
                                    }
                                },
                                onLongClick = {
                                    viewModel.onEvent(RemoteBrowseEvent.OptionsRequested(entry))
                                }
                            )
                        }
                    }
                }
            }

            if (state.operationInProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.padding(top = Spacing.sm))
                        Text(
                            text = state.operationMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Long-press options dialog
    state.optionsEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(RemoteBrowseEvent.OptionsDismissed) },
            title = { Text(entry.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    TextButton(
                        onClick = {
                            viewModel.onEvent(RemoteBrowseEvent.OptionsDismissed)
                            viewModel.onEvent(RemoteBrowseEvent.DownloadClicked(entry))
                        },
                        enabled = !entry.isDirectory
                    ) { Text("Download to device") }
                    RenameRow(entry = entry, onRename = { newName ->
                        viewModel.onEvent(RemoteBrowseEvent.RenameRequested(entry, newName))
                    })
                    TextButton(
                        onClick = {
                            viewModel.onEvent(RemoteBrowseEvent.OptionsDismissed)
                            viewModel.onEvent(RemoteBrowseEvent.DeleteClicked(entry))
                        }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(RemoteBrowseEvent.OptionsDismissed) }) {
                    Text("Close")
                }
            }
        )
    }

    if (state.showMkdirDialog) {
        TextPromptDialog(
            title = "New folder",
            initialText = "New folder",
            onConfirm = { name ->
                viewModel.onEvent(RemoteBrowseEvent.ShowMkdirDialog(false))
                viewModel.onEvent(RemoteBrowseEvent.MkdirRequested(name))
            },
            onDismiss = { viewModel.onEvent(RemoteBrowseEvent.ShowMkdirDialog(false)) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RemoteEntryRow(
    entry: RemoteFileEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ext = entry.name.substringAfterLast('.', "").lowercase()
        val painterId = if (entry.isDirectory) {
            AppIcons.Folder.default
        } else {
            val fileType = FileType.fromExtension(ext)
            when (fileType) {
                FileType.IMAGE -> AppIcons.File.image
                FileType.VIDEO -> AppIcons.File.video
                FileType.AUDIO -> AppIcons.File.audio
                FileType.DOCUMENT -> AppIcons.File.document
                FileType.TEXT -> AppIcons.File.text
                FileType.ARCHIVE -> AppIcons.File.archive
                FileType.APK -> AppIcons.File.apk
                FileType.OTHER -> AppIcons.File.generic
            }
        }
        Icon(
            painter = painterResource(id = painterId),
            contentDescription = null,
            tint = if (entry.isDirectory) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Unspecified
            },
            modifier = Modifier.size(36.dp)
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
            if (!entry.isDirectory) {
                Text(
                    text = FileSizeFormatter.format(entry.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (entry.isDirectory) {
            Text(
                text = ">",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RenameRow(entry: RemoteFileEntry, onRename: (String) -> Unit) {
    var text by remember(entry.path) { mutableStateOf(entry.name) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = { Text("Rename") },
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { onRename(text) }, enabled = text.isNotBlank() && text != entry.name) {
            Text("OK")
        }
    }
}

@Composable
private fun TextPromptDialog(
    title: String,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
