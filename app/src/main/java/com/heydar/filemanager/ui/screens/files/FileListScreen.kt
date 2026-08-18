package com.heydar.filemanager.ui.screens.files

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.painterResource
import com.heydar.filemanager.core.icons.AppIcons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.heydar.filemanager.domain.model.FileDisplayMode
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.heydar.filemanager.core.util.DateFormatter
import com.heydar.filemanager.core.util.FileSizeFormatter
import com.heydar.filemanager.domain.model.FileItem
import com.heydar.filemanager.ui.components.dialog.DeleteConfirmationDialog
import com.heydar.filemanager.ui.components.dialog.FileActionsBottomSheet
import com.heydar.filemanager.ui.components.dialog.RenameDialog
import com.heydar.filemanager.ui.components.file.FileIcon
import com.heydar.filemanager.ui.theme.Spacing
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    rootPath: String,
    onNavigateUp: () -> Unit,
    onOpenTextEditor: (String) -> Unit = {},
    onOpenDetails: (String) -> Unit = {},
    viewModel: FileListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDeleteDialog by remember { mutableStateOf<FileItem?>(null) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showViewMenu by remember { mutableStateOf(false) }

    LaunchedEffect(rootPath) {
        viewModel.onEvent(FileListEvent.LoadFiles(rootPath))
    }
    
    LaunchedEffect(state.openTextEditorFor) {
        state.openTextEditorFor?.let { path ->
            onOpenTextEditor(path)
            viewModel.onEvent(FileListEvent.TextEditorHandled)
        }
    }

    LaunchedEffect(state.openDetailsFor) {
        state.openDetailsFor?.let { path ->
            onOpenDetails(path)
            viewModel.onEvent(FileListEvent.DetailsHandled)
        }
    }

    BackHandler(enabled = true) {
        when {
            state.selectedFile != null -> {
                viewModel.onEvent(FileListEvent.DismissSelection)
            }
            viewModel.canGoBack() -> {
                viewModel.onEvent(FileListEvent.OnBackClicked)
            }
            else -> {
                onNavigateUp()
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (state.currentPath.isEmpty()) "Files" else File(state.currentPath).name.ifEmpty { "Files" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.canGoBack()) {
                            viewModel.onEvent(FileListEvent.OnBackClicked)
                        } else {
                            onNavigateUp()
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = AppIcons.Nav.back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showViewMenu = true }) {
                        Icon(
                            painter = painterResource(id = AppIcons.Action.more),
                            contentDescription = "View options"
                        )
                    }
                    DropdownMenu(
                        expanded = showViewMenu,
                        onDismissRequest = { showViewMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("List Small") },
                            onClick = { 
                                viewModel.onEvent(FileListEvent.ChangeDisplayMode(FileDisplayMode.LIST_SMALL))
                                showViewMenu = false 
                            },
                            trailingIcon = if (state.displayMode == FileDisplayMode.LIST_SMALL) {
                                { Text("✓") }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("List Large") },
                            onClick = { 
                                viewModel.onEvent(FileListEvent.ChangeDisplayMode(FileDisplayMode.LIST_LARGE))
                                showViewMenu = false 
                            },
                            trailingIcon = if (state.displayMode == FileDisplayMode.LIST_LARGE) {
                                { Text("✓") }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Grid Small") },
                            onClick = { 
                                viewModel.onEvent(FileListEvent.ChangeDisplayMode(FileDisplayMode.GRID_SMALL))
                                showViewMenu = false 
                            },
                            trailingIcon = if (state.displayMode == FileDisplayMode.GRID_SMALL) {
                                { Text("✓") }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Grid Medium") },
                            onClick = { 
                                viewModel.onEvent(FileListEvent.ChangeDisplayMode(FileDisplayMode.GRID_MEDIUM))
                                showViewMenu = false 
                            },
                            trailingIcon = if (state.displayMode == FileDisplayMode.GRID_MEDIUM) {
                                { Text("✓") }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Grid Large") },
                            onClick = { 
                                viewModel.onEvent(FileListEvent.ChangeDisplayMode(FileDisplayMode.GRID_LARGE))
                                showViewMenu = false 
                            },
                            trailingIcon = if (state.displayMode == FileDisplayMode.GRID_LARGE) {
                                { Text("✓") }
                            } else null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

                // Address Bar
                AddressBar(currentPath = state.currentPath)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.files.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.errorMessage != null && state.files.isEmpty() -> {
                    ErrorState(message = state.errorMessage!!, modifier = Modifier.align(Alignment.Center))
                }
                state.files.isEmpty() -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    FileListContent(
                        files = state.files,
                        selectedFile = state.selectedFile,
                        displayMode = state.displayMode,
                        onFileClick = { file ->
                            if (file.isDirectory) {
                                viewModel.onEvent(FileListEvent.OnFolderClicked(file.path))
                            } else {
                                viewModel.onEvent(FileListEvent.OnFileClicked(file.path))
                            }
                        },
                        onFileLongClick = { file ->
                            viewModel.onEvent(FileListEvent.OnFileLongClicked(file))
                        }
                    )
                }
            }

            if (state.operationInProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Bottom Sheet
    state.selectedFile?.let { file ->
        FileActionsBottomSheet(
            fileItem = file,
            sheetState = bottomSheetState,
            onDismiss = { viewModel.onEvent(FileListEvent.DismissSelection) },
            onCopy = { viewModel.onEvent(FileListEvent.OnCopyFile(file)) },
            onMove = { viewModel.onEvent(FileListEvent.OnMoveFile(file)) },
            onRename = { showRenameDialog = file },
            onDelete = { showDeleteDialog = file },
            onShare = { viewModel.onEvent(FileListEvent.OnShareFile(file)) },
            onDetails = { viewModel.onEvent(FileListEvent.OnDetailsClicked(file)) }
        )
    }

    // Dialogs
    showDeleteDialog?.let { file ->
        DeleteConfirmationDialog(
            fileItem = file,
            onConfirm = {
                viewModel.onEvent(FileListEvent.OnDeleteConfirmed(file))
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    showRenameDialog?.let { file ->
        RenameDialog(
            fileItem = file,
            onConfirm = { newName ->
                viewModel.onEvent(FileListEvent.OnRenameConfirmed(file, newName))
                showRenameDialog = null
            },
            onDismiss = { showRenameDialog = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListContent(
    files: List<FileItem>,
    selectedFile: FileItem?,
    displayMode: FileDisplayMode,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit
) {
    val isList = displayMode == FileDisplayMode.LIST_SMALL || displayMode == FileDisplayMode.LIST_LARGE
    if (isList) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Spacing.md)
        ) {
            items(files, key = { it.path }) { file ->
                val isSelected = selectedFile?.path == file.path
                FileListItem(
                    fileItem = file,
                    isSelected = isSelected,
                    isSmall = displayMode == FileDisplayMode.LIST_SMALL,
                    modifier = Modifier.combinedClickable(
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) }
                    )
                )
            }
        }
    } else {
        val minSize = when (displayMode) {
            FileDisplayMode.GRID_SMALL -> 80.dp
            FileDisplayMode.GRID_LARGE -> 140.dp
            else -> 100.dp
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(files, key = { it.path }) { file ->
                val isSelected = selectedFile?.path == file.path
                FileGridItem(
                    fileItem = file,
                    isSelected = isSelected,
                    modifier = Modifier.combinedClickable(
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) }
                    )
                )
            }
        }
    }
}

@Composable
private fun FileListItem(
    fileItem: FileItem,
    isSelected: Boolean,
    isSmall: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val iconSize = if (isSmall) 32.dp else Spacing.iconSize
    val paddingVertical = if (isSmall) 4.dp else Spacing.listItemPadding

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = Spacing.md, vertical = paddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileIcon(
            fileItem = fileItem,
            size = iconSize
        )

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileItem.name,
                style = if (isSmall) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!isSmall) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (!fileItem.isDirectory) {
                        Text(
                            text = FileSizeFormatter.format(fileItem.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = DateFormatter.format(fileItem.lastModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FileGridItem(
    fileItem: FileItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .padding(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FileIcon(
            fileItem = fileItem,
            size = 48.dp
        )
        
        Spacer(modifier = Modifier.height(Spacing.xs))
        
        Text(
            text = fileItem.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(
            painter = painterResource(id = AppIcons.Status.empty),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "This folder is empty",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun AddressBar(currentPath: String) {
    val scrollState = rememberScrollState()
    val storagePath = Environment.getExternalStorageDirectory().absolutePath

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = Spacing.md, vertical = 8.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = AppIcons.Storage.internal),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))

        // Storage root
        Text(
            text = "Internal Storage",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        // Path segments
        val relativePath = currentPath.removePrefix(storagePath).trimStart('/')
        if (relativePath.isNotEmpty()) {
            val segments = relativePath.split("/").filter { it.isNotEmpty() }
            segments.forEachIndexed { index, segment ->
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val isLast = index == segments.lastIndex
                Text(
                    text = segment,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLast) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}