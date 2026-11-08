package ir.heydar.nexfolder.ui.screens.files

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.core.util.DateFormatter
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import ir.heydar.nexfolder.domain.model.ClipboardMode
import ir.heydar.nexfolder.domain.model.FileDisplayMode
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.domain.model.SortMode
import ir.heydar.nexfolder.ui.components.dialog.ArchiveCreateDialog
import ir.heydar.nexfolder.ui.components.dialog.DeleteConfirmationDialog
import ir.heydar.nexfolder.ui.components.dialog.DirectoryPickerDialog
import ir.heydar.nexfolder.ui.components.dialog.FileActionsBottomSheet
import ir.heydar.nexfolder.ui.components.dialog.OperationProgressDialog
import ir.heydar.nexfolder.ui.components.dialog.RenameDialog
import ir.heydar.nexfolder.ui.components.file.FileIcon
import ir.heydar.nexfolder.ui.components.file.fileAccentColor
import ir.heydar.nexfolder.ui.theme.Spacing
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    rootPath: String,
    onNavigateUp: () -> Unit,
    onOpenTextEditor: (String) -> Unit = {},
    onOpenDetails: (String) -> Unit = {},
    onOpenSearch: (String) -> Unit = {},
    onOpenViewer: (String) -> Unit = {},
    onOpenPlayer: (String) -> Unit = {},
    onOpenArchive: (String) -> Unit = {},
    viewModel: FileListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf<FileItem?>(null) }
    var showMultiDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showDirectoryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(rootPath) {
        // Restores the last visited folder when coming back from a viewer/editor
        // instead of reloading the volume root (which used to lose the location).
        viewModel.onEvent(FileListEvent.ScreenEntered(rootPath))
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

    LaunchedEffect(state.openImageViewerFor) {
        state.openImageViewerFor?.let { path ->
            onOpenViewer(path)
            viewModel.onEvent(FileListEvent.ViewerHandled)
        }
    }

    LaunchedEffect(state.openAudioPlayerFor) {
        state.openAudioPlayerFor?.let { path ->
            onOpenPlayer(path)
            viewModel.onEvent(FileListEvent.PlayerHandled)
        }
    }

    LaunchedEffect(state.openArchiveFor) {
        state.openArchiveFor?.let { path ->
            onOpenArchive(path)
            viewModel.onEvent(FileListEvent.ArchiveHandled)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(FileListEvent.ErrorShown)
        }
    }

    BackHandler(enabled = true) {
        when {
            state.selectionMode -> viewModel.onEvent(FileListEvent.ExitSelectionMode)
            state.selectedFile != null -> viewModel.onEvent(FileListEvent.DismissSelection)
            viewModel.canGoBack() -> viewModel.onEvent(FileListEvent.OnBackClicked)
            else -> onNavigateUp()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                if (state.selectionMode) {
                    SelectionTopBar(
                        selectedCount = state.selectedPaths.size,
                        onClose = { viewModel.onEvent(FileListEvent.ExitSelectionMode) },
                        onCopy = { showDirectoryPicker = true },
                        onMove = { showDirectoryPicker = true },
                        onDelete = {
                            if (settings.confirmDelete) {
                                showMultiDeleteConfirm = true
                            } else {
                                viewModel.onEvent(
                                    FileListEvent.OnDeleteSelectedConfirmed(state.selectedPaths.toList())
                                )
                            }
                        },
                        onSelectAll = { viewModel.onEvent(FileListEvent.SelectAll) },
                        onShare = {
                            selectedItem(state)?.let {
                                viewModel.onEvent(FileListEvent.OnShareFile(it))
                            }
                        },
                        onRename = {
                            selectedItem(state)?.let { showRenameDialog = it }
                        },
                        onDetails = {
                            selectedItem(state)?.let {
                                viewModel.onEvent(FileListEvent.OnDetailsClicked(it))
                            }
                        },
                        onOpenWith = {
                            selectedItem(state)?.let {
                                viewModel.onEvent(FileListEvent.OnOpenWith(it))
                            }
                        },
                        canOpenWith = selectedItem(state)?.isDirectory == false
                    )
                } else {
                    FileTopBar(
                        rootPath = rootPath,
                        currentPath = state.currentPath,
                        sortMode = state.sortMode,
                        displayMode = state.displayMode,
                        onBack = {
                            if (viewModel.canGoBack()) {
                                viewModel.onEvent(FileListEvent.OnBackClicked)
                            } else {
                                onNavigateUp()
                            }
                        },
                        onNavigateTo = { path ->
                            viewModel.onEvent(FileListEvent.NavigateToPath(path))
                        },
                        onSearch = { onOpenSearch(state.currentPath.ifEmpty { rootPath }) },
                        onSortSelected = { mode ->
                            viewModel.onEvent(FileListEvent.ChangeSort(mode))
                        },
                        onDisplayModeSelected = { mode ->
                            viewModel.onEvent(FileListEvent.ChangeDisplayMode(mode))
                        }
                    )
                }

                // Paste banner while a copy/move is pending
                if (state.clipboardMode != null && !state.selectionMode) {
                    PasteBanner(
                        mode = state.clipboardMode!!,
                        itemCount = state.clipboardPaths.size,
                        onPaste = { showDirectoryPicker = true },
                        onCancel = { viewModel.onEvent(FileListEvent.OnCancelClipboard) }
                    )
                }
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
                    LoadingState(modifier = Modifier.align(Alignment.Center))
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
                        selectionMode = state.selectionMode,
                        selectedPaths = state.selectedPaths,
                        onFileClick = { file ->
                            if (state.selectionMode) {
                                viewModel.onEvent(FileListEvent.ToggleSelected(file.path))
                            } else if (file.isDirectory) {
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

    // Bottom Sheet (single-file actions)
    state.selectedFile?.let { file ->
        FileActionsBottomSheet(
            fileItem = file,
            sheetState = bottomSheetState,
            onDismiss = { viewModel.onEvent(FileListEvent.DismissSelection) },
            onCopy = {
                viewModel.onEvent(FileListEvent.OnCopyFile(file))
                showDirectoryPicker = true
            },
            onMove = {
                viewModel.onEvent(FileListEvent.OnMoveFile(file))
                showDirectoryPicker = true
            },
            onRename = { showRenameDialog = file },
            onDelete = {
                if (settings.confirmDelete) {
                    showDeleteDialog = file
                } else {
                    viewModel.onEvent(FileListEvent.OnDeleteConfirmed(file))
                }
            },
            onShare = { viewModel.onEvent(FileListEvent.OnShareFile(file)) },
            onDetails = { viewModel.onEvent(FileListEvent.OnDetailsClicked(file)) },
            onBrowseArchive = if (file.fileType == FileType.ARCHIVE) {
                { onOpenArchive(file.path) }
            } else null,
            onCompress = {
                viewModel.onEvent(FileListEvent.OnCompressClicked(listOf(file.path)))
            },
            onOpenWith = { viewModel.onEvent(FileListEvent.OnOpenWith(file)) }
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

    if (showMultiDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showMultiDeleteConfirm = false },
            title = { Text("Delete ${state.selectedPaths.size} item(s)?") },
            text = { Text("Selected items will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(FileListEvent.OnDeleteSelectedConfirmed(state.selectedPaths.toList()))
                    showMultiDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMultiDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDirectoryPicker && state.clipboardMode != null) {
        DirectoryPickerDialog(
            title = "${state.clipboardMode!!.actionLabel} ${state.clipboardPaths.size} item(s) to",
            initialPath = state.currentPath.ifEmpty { rootPath },
            onPathSelected = { destination ->
                showDirectoryPicker = false
                viewModel.onEvent(FileListEvent.OnPasteConfirmed(destination))
            },
            onDismiss = { showDirectoryPicker = false }
        )
    }

    state.activeOperation?.let { progress ->
        OperationProgressDialog(
            progress = progress,
            onCancel = { viewModel.onEvent(FileListEvent.OnCancelOperation) }
        )
    }

    state.compressSources?.let { sources ->
        ArchiveCreateDialog(
            itemCount = sources.size,
            defaultName = File(sources.first()).name.removeSuffix(".${File(sources.first()).extension}"),
            onConfirm = { format, name, password ->
                viewModel.onEvent(FileListEvent.OnCompressConfirmed(format, name, password))
            },
            onDismiss = { viewModel.onEvent(FileListEvent.OnCompressCancelled) }
        )
    }
}

private fun selectedItem(state: FileListUiState): FileItem? {
    if (state.selectedPaths.size != 1) return null
    val path = state.selectedPaths.first()
    return state.files.firstOrNull { it.path == path }
}

private fun displayModeLabel(mode: FileDisplayMode): String = when (mode) {
    FileDisplayMode.LIST_SMALL -> "Compact list"
    FileDisplayMode.LIST_LARGE -> "List"
    FileDisplayMode.GRID_SMALL -> "Small grid"
    FileDisplayMode.GRID_MEDIUM -> "Grid"
    FileDisplayMode.GRID_LARGE -> "Large grid"
}

private fun displayModeIcon(mode: FileDisplayMode): ImageVector = when (mode) {
    FileDisplayMode.LIST_SMALL, FileDisplayMode.LIST_LARGE -> Icons.Rounded.ViewList
    else -> Icons.Rounded.GridView
}

/**
 * Friendly volume label for the breadcrumb root. The internal storage path ends in
 * "/emulated/0", which used to render a stray "0" as the screen title.
 */
private fun rootDisplayName(rootPath: String): String {
    val storageRoot = Environment.getExternalStorageDirectory().absolutePath
    return when {
        rootPath == storageRoot -> "Internal Storage"
        rootPath.contains(Regex("/storage/[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")) -> "External storage"
        else -> File(rootPath).name.ifEmpty { "Storage" }
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileTopBar(
    rootPath: String,
    currentPath: String,
    sortMode: SortMode,
    displayMode: FileDisplayMode,
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onSearch: () -> Unit,
    onSortSelected: (SortMode) -> Unit,
    onDisplayModeSelected: (FileDisplayMode) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showViewMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            BreadcrumbBar(
                rootPath = rootPath,
                currentPath = currentPath,
                onNavigateTo = onNavigateTo
            )
        },
        navigationIcon = {
            Box(modifier = Modifier.padding(start = Spacing.sm)) {
                CircleIconButton(
                    icon = AppIcons.Nav.back,
                    contentDescription = "Back",
                    onClick = onBack
                )
            }
        },
        actions = {
            CircleIconButton(
                icon = AppIcons.Action.search,
                contentDescription = "Search",
                onClick = onSearch
            )
            Spacer(modifier = Modifier.width(Spacing.xs + 2.dp))
            Box {
                CircleIconButton(
                    icon = AppIcons.Action.sort,
                    contentDescription = "Sort",
                    onClick = { showSortMenu = true }
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                onSortSelected(mode)
                                showSortMenu = false
                            },
                            trailingIcon = if (sortMode == mode) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(Spacing.xs + 2.dp))
            Box(modifier = Modifier.padding(end = Spacing.sm)) {
                CircleIconButton(
                    icon = AppIcons.Action.more,
                    contentDescription = "View options",
                    onClick = { showViewMenu = true }
                )
                DropdownMenu(
                    expanded = showViewMenu,
                    onDismissRequest = { showViewMenu = false }
                ) {
                    FileDisplayMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(displayModeLabel(mode)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = displayModeIcon(mode),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                onDisplayModeSelected(mode)
                                showViewMenu = false
                            },
                            trailingIcon = if (displayMode == mode) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun CircleIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun BreadcrumbBar(
    rootPath: String,
    currentPath: String,
    onNavigateTo: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val rootLabel = rootDisplayName(rootPath)
    val relative = currentPath.removePrefix(rootPath.trimEnd('/')).trimStart('/')
    val segments = relative.split("/").filter { it.isNotEmpty() }

    // Keep the current location visible
    LaunchedEffect(currentPath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = AppIcons.Storage.internal),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.xs + 2.dp))
        Text(
            text = rootLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (segments.isEmpty()) FontWeight.Bold else FontWeight.Medium,
            color = if (segments.isEmpty()) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onNavigateTo(rootPath) }
                .padding(horizontal = 2.dp, vertical = 2.dp)
        )
        segments.forEachIndexed { index, segment ->
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .size(14.dp)
            )
            val isLast = index == segments.lastIndex
            Text(
                text = segment,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                color = if (isLast) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigateTo(joinSegment(rootPath, segments, index)) }
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
    }
}

private fun joinSegment(rootPath: String, segments: List<String>, upTo: Int): String =
    rootPath.trimEnd('/') + "/" + segments.take(upTo + 1).joinToString("/")

// ---------------------------------------------------------------------------
// Selection top bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit,
    onOpenWith: () -> Unit = {},
    canOpenWith: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$selectedCount",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = if (selectedCount == 1) "item selected" else "items selected",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            Box(modifier = Modifier.padding(start = Spacing.sm)) {
                CircleIconButton(
                    icon = AppIcons.Nav.close,
                    contentDescription = "Clear selection",
                    onClick = onClose
                )
            }
        },
        actions = {
            CircleIconButton(AppIcons.Action.copy, "Copy", onCopy)
            Spacer(modifier = Modifier.width(Spacing.xs + 2.dp))
            CircleIconButton(AppIcons.Action.move, "Move", onMove)
            Spacer(modifier = Modifier.width(Spacing.xs + 2.dp))
            Surface(
                onClick = onDelete,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                contentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = AppIcons.Action.delete),
                        contentDescription = "Delete",
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.xs + 2.dp))
            Box(modifier = Modifier.padding(end = Spacing.sm)) {
                CircleIconButton(AppIcons.Action.more, "More", { showMenu = true })
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Select all") },
                        onClick = { onSelectAll(); showMenu = false }
                    )
                    if (selectedCount == 1) {
                        DropdownMenuItem(
                            text = { Text("Open with…") },
                            onClick = { onOpenWith(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = { onShare(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { onRename(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Details") },
                            onClick = { onDetails(); showMenu = false }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ---------------------------------------------------------------------------
// Paste banner
// ---------------------------------------------------------------------------

@Composable
private fun PasteBanner(
    mode: ClipboardMode,
    itemCount: Int,
    onPaste: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    id = if (mode == ClipboardMode.MOVE) AppIcons.Action.move else AppIcons.Action.copy
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.sm + Spacing.xs))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${mode.actionLabel} $itemCount item(s)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pick a destination to finish",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onPaste) { Text("Paste") }
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(id = AppIcons.Nav.close),
                    contentDescription = "Cancel paste",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListContent(
    files: List<FileItem>,
    selectedFile: FileItem?,
    displayMode: FileDisplayMode,
    selectionMode: Boolean,
    selectedPaths: Set<String>,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit
) {
    val isList = displayMode == FileDisplayMode.LIST_SMALL || displayMode == FileDisplayMode.LIST_LARGE
    val folders = files.filter { it.isDirectory }
    val documents = files.filterNot { it.isDirectory }

    if (isList) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = Spacing.xs, bottom = Spacing.xl)
        ) {
            item(key = "summary") {
                FolderSummaryBar(folders = folders.size, documents = documents, files = files)
            }
            if (folders.isNotEmpty()) {
                item(key = "folders-header") {
                    SectionHeader(
                        title = "Folders",
                        count = folders.size,
                        accent = MaterialTheme.colorScheme.primary
                    )
                }
                items(folders, key = { it.path }) { file ->
                    FileListItem(
                        fileItem = file,
                        isSelected = selectedFile?.path == file.path,
                        isChecked = selectionMode && selectedPaths.contains(file.path),
                        showCheckbox = selectionMode,
                        isSmall = displayMode == FileDisplayMode.LIST_SMALL,
                        modifier = Modifier.combinedClickable(
                            onClick = { onFileClick(file) },
                            onLongClick = { onFileLongClick(file) }
                        )
                    )
                }
            }
            if (documents.isNotEmpty()) {
                item(key = "files-header") {
                    SectionHeader(
                        title = "Files",
                        count = documents.size,
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                }
                items(documents, key = { it.path }) { file ->
                    FileListItem(
                        fileItem = file,
                        isSelected = selectedFile?.path == file.path,
                        isChecked = selectionMode && selectedPaths.contains(file.path),
                        showCheckbox = selectionMode,
                        isSmall = displayMode == FileDisplayMode.LIST_SMALL,
                        modifier = Modifier.combinedClickable(
                            onClick = { onFileClick(file) },
                            onLongClick = { onFileLongClick(file) }
                        )
                    )
                }
            }
        }
    } else {
        // Grid density really changes between modes: cell size, icon size and
        // subtitle visibility all scale, so the modes look and feel different.
        val gridSpec = when (displayMode) {
            FileDisplayMode.GRID_SMALL -> GridSpec(minCell = 76.dp, icon = 42.dp, showSubtitle = false)
            FileDisplayMode.GRID_MEDIUM -> GridSpec(minCell = 100.dp, icon = 54.dp, showSubtitle = true)
            else -> GridSpec(minCell = 128.dp, icon = 72.dp, showSubtitle = true)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(gridSpec.minCell),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.sm,
                end = Spacing.sm,
                top = Spacing.xs,
                bottom = Spacing.lg
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            item(
                key = "summary",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                FolderSummaryBar(folders = folders.size, documents = documents, files = files)
            }
            items(files, key = { it.path }) { file ->
                FileGridItem(
                    fileItem = file,
                    isSelected = selectedFile?.path == file.path ||
                        (selectionMode && selectedPaths.contains(file.path)),
                    iconSize = gridSpec.icon,
                    showSubtitle = gridSpec.showSubtitle,
                    modifier = Modifier.combinedClickable(
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) }
                    )
                )
            }
        }
    }
}

private data class GridSpec(val minCell: androidx.compose.ui.unit.Dp, val icon: androidx.compose.ui.unit.Dp, val showSubtitle: Boolean)

@Composable
private fun FolderSummaryBar(
    folders: Int,
    documents: List<FileItem>,
    files: List<FileItem>
) {
    val totalBytes = documents.sumOf { it.size }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (folders > 0) {
            StatPill(
                icon = AppIcons.Folder.default,
                label = "$folders folders",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (documents.isNotEmpty()) {
            StatPill(
                icon = AppIcons.File.generic,
                label = "${documents.size} files",
                tint = MaterialTheme.colorScheme.tertiary
            )
            if (totalBytes > 0) {
                StatPill(
                    icon = null,
                    label = FileSizeFormatter.format(totalBytes),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (files.isEmpty()) {
            StatPill(icon = null, label = "Empty folder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatPill(@DrawableRes icon: Int?, label: String, tint: Color) {
    Surface(
        shape = CircleShape,
        color = tint.copy(alpha = 0.10f),
        contentColor = tint
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm + 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs + 1.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 1.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(
    fileItem: FileItem,
    isSelected: Boolean,
    isChecked: Boolean,
    showCheckbox: Boolean,
    isSmall: Boolean,
    modifier: Modifier = Modifier
) {
    val highlighted = isChecked || isSelected
    val accent = fileAccentColor(fileItem)
    val shape = RoundedCornerShape(if (isSmall) 12.dp else 14.dp)
    // Flat rows: only a soft highlight while selected/checked, no card surface.
    val background by animateColorAsState(
        targetValue = if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            Color.Transparent
        },
        label = "item_background"
    )

    Row(
        modifier = modifier
            .padding(horizontal = Spacing.sm)
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .padding(
                horizontal = Spacing.sm + 2.dp,
                vertical = if (isSmall) 4.dp else 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showCheckbox) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
        }

        FileIcon(fileItem = fileItem, size = if (isSmall) 34.dp else 46.dp)
        Spacer(modifier = Modifier.width(Spacing.sm + 2.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileItem.name,
                style = if (isSmall) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.titleSmall
                },
                fontWeight = if (fileItem.isDirectory) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!isSmall) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)
                ) {
                    if (!fileItem.isDirectory && fileItem.extension.isNotEmpty()) {
                        ExtensionPill(label = fileItem.extension.uppercase(), color = accent)
                    }
                    Text(
                        text = if (fileItem.isDirectory) {
                            "Folder"
                        } else {
                            FileSizeFormatter.format(fileItem.size)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    Text(
                        text = DateFormatter.format(fileItem.lastModified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }
        }

        if (isSmall) {
            Text(
                text = if (fileItem.isDirectory) "Folder" else FileSizeFormatter.format(fileItem.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (fileItem.isDirectory && !showCheckbox) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ExtensionPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = label.take(5),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridItem(
    fileItem: FileItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 54.dp,
    showSubtitle: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    val background by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        },
        label = "grid_background"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = Spacing.xs, horizontal = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FileIcon(fileItem = fileItem, size = iconSize)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (showSubtitle) {
                Text(
                    text = if (fileItem.isDirectory) {
                        "Folder"
                    } else {
                        FileSizeFormatter.format(fileItem.size)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.sm)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            strokeWidth = 4.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Loading…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = AppIcons.Folder.default),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "This folder is empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Files and folders you add here will show up",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
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
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = AppIcons.Status.error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "Couldn't load this folder",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
