package ir.heydar.nexfolder.ui.screens.files

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.heydar.nexfolder.core.archive.ArchivePasswordException
import ir.heydar.nexfolder.core.files.FileOperations
import ir.heydar.nexfolder.core.files.OperationProgress
import ir.heydar.nexfolder.core.util.FileShareManager
import ir.heydar.nexfolder.core.util.MimeTypeHelper
import ir.heydar.nexfolder.domain.model.ClipboardMode
import ir.heydar.nexfolder.domain.model.DirectoryType
import ir.heydar.nexfolder.domain.model.FileDisplayMode
import ir.heydar.nexfolder.domain.model.FileFormat
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.domain.model.SortMode
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@HiltViewModel
class FileListViewModel @Inject constructor(
    private val app: Application,
    private val recentsManager: ir.heydar.nexfolder.core.history.RecentsManager,
    private val archiveManager: ir.heydar.nexfolder.core.archive.ArchiveManager,
    private val settingsManager: ir.heydar.nexfolder.core.settings.SettingsManager
) : ViewModel() {

    private val prefs = app.getSharedPreferences("file_manager_prefs", Context.MODE_PRIVATE)
    private val DISPLAY_MODE_KEY = "display_mode"
    private val SORT_MODE_KEY = "sort_mode"

    private val _state = MutableStateFlow(FileListUiState())
    val state: StateFlow<FileListUiState> = _state.asStateFlow()

    /** Reactive app settings (theme, hidden files, confirm-delete) for the UI. */
    val settings: StateFlow<ir.heydar.nexfolder.core.settings.AppSettings> =
        settingsManager.settings

    private var operationJob: Job? = null

    init {
        val savedModeString = prefs.getString(DISPLAY_MODE_KEY, FileDisplayMode.LIST_LARGE.name) ?: FileDisplayMode.LIST_LARGE.name
        val savedMode = try { FileDisplayMode.valueOf(savedModeString) } catch (e: Exception) { FileDisplayMode.LIST_LARGE }
        val savedSort = SortMode.fromNameOrDefault(prefs.getString(SORT_MODE_KEY, null))
        _state.update { it.copy(displayMode = savedMode, sortMode = savedSort) }
    }

    fun onEvent(event: FileListEvent) {
        when (event) {
            is FileListEvent.ScreenEntered -> {
                val current = _state.value
                if (current.currentPath.isNotEmpty()) {
                    // Returning from a viewer/editor/details route — restore where the
                    // user actually was instead of jumping back to the volume root.
                    viewModelScope.launch { loadFilesInternal(current.currentPath) }
                } else {
                    loadFiles(event.rootPath, addToHistory = false)
                }
            }
            is FileListEvent.LoadFiles -> loadFiles(event.path, addToHistory = false)
            is FileListEvent.OnFolderClicked -> loadFiles(event.path, addToHistory = true)
            is FileListEvent.NavigateToPath -> navigateToPath(event.path)
            is FileListEvent.OnFileClicked -> {
                val state = _state.value
                if (state.selectionMode) {
                    onEvent(FileListEvent.ToggleSelected(event.path))
                } else {
                    openFile(event.path)
                }
            }
            is FileListEvent.OnFileLongClicked -> {
                if (!_state.value.selectionMode) {
                    _state.update {
                        it.copy(selectionMode = true, selectedPaths = setOf(event.fileItem.path))
                    }
                } else {
                    onEvent(FileListEvent.ToggleSelected(event.fileItem.path))
                }
            }
            is FileListEvent.OnBackClicked -> handleBackNavigation()
            is FileListEvent.DismissSelection -> {
                _state.update { it.copy(selectedFile = null) }
            }
            is FileListEvent.ChangeDisplayMode -> {
                prefs.edit().putString(DISPLAY_MODE_KEY, event.mode.name).apply()
                _state.update { it.copy(displayMode = event.mode) }
            }
            is FileListEvent.TextEditorHandled -> {
                _state.update { it.copy(openTextEditorFor = null) }
            }
            is FileListEvent.OnDetailsClicked -> {
                _state.update { it.copy(openDetailsFor = event.fileItem.path, selectedFile = null) }
            }
            is FileListEvent.DetailsHandled -> {
                _state.update { it.copy(openDetailsFor = null) }
            }
            is FileListEvent.ViewerHandled -> {
                _state.update { it.copy(openImageViewerFor = null) }
            }
            is FileListEvent.PlayerHandled -> {
                _state.update { it.copy(openAudioPlayerFor = null) }
            }
            is FileListEvent.ArchiveHandled -> {
                _state.update { it.copy(openArchiveFor = null) }
            }
            is FileListEvent.OnDeleteConfirmed -> deleteFile(event.fileItem)
            is FileListEvent.OnRenameConfirmed -> renameFile(event.fileItem, event.newName)
            is FileListEvent.OnShareFile -> shareFile(event.fileItem)
            is FileListEvent.OnOpenWith -> openWith(event.fileItem)
            is FileListEvent.OnCopyFile -> {
                _state.update {
                    it.copy(clipboardMode = ClipboardMode.COPY, clipboardPaths = listOf(event.fileItem.path))
                }
            }
            is FileListEvent.OnMoveFile -> {
                _state.update {
                    it.copy(clipboardMode = ClipboardMode.MOVE, clipboardPaths = listOf(event.fileItem.path))
                }
            }

            // Multi-selection
            is FileListEvent.ToggleSelected -> {
                _state.update { current ->
                    val updated = if (event.path in current.selectedPaths) {
                        current.selectedPaths - event.path
                    } else {
                        current.selectedPaths + event.path
                    }
                    current.copy(selectedPaths = updated, selectionMode = updated.isNotEmpty())
                }
            }
            is FileListEvent.SelectAll -> {
                _state.update {
                    it.copy(selectedPaths = it.files.map { file -> file.path }.toSet())
                }
            }
            is FileListEvent.ExitSelectionMode -> {
                _state.update { it.copy(selectionMode = false, selectedPaths = emptySet()) }
            }
            is FileListEvent.OnDeleteSelectedConfirmed -> deletePaths(event.paths)

            // Clipboard / paste
            is FileListEvent.OnPasteConfirmed -> pasteTo(event.destinationPath)
            is FileListEvent.OnCancelClipboard -> {
                _state.update { it.copy(clipboardMode = null, clipboardPaths = emptyList()) }
            }
            is FileListEvent.OnCancelOperation -> {
                operationJob?.cancel()
                _state.update { it.copy(activeOperation = null) }
                reloadCurrent()
            }

            // Sort
            is FileListEvent.ChangeSort -> {
                prefs.edit().putString(SORT_MODE_KEY, event.mode.name).apply()
                _state.update { it.copy(sortMode = event.mode) }
                reloadCurrent()
            }
            is FileListEvent.ErrorShown -> {
                _state.update { it.copy(errorMessage = null) }
            }

            // Compression
            is FileListEvent.OnCompressClicked -> {
                _state.update { it.copy(compressSources = event.sources) }
            }
            is FileListEvent.OnCompressCancelled -> {
                _state.update { it.copy(compressSources = null) }
            }
            is FileListEvent.OnCompressConfirmed -> {
                executeCompress(event.format, event.archiveName, event.password)
            }
        }
    }

    fun canGoBack(): Boolean = _state.value.pathHistory.isNotEmpty()

    private fun handleBackNavigation() {
        val currentState = _state.value
        if (currentState.selectionMode) {
            onEvent(FileListEvent.ExitSelectionMode)
            return
        }
        if (currentState.pathHistory.isNotEmpty()) {
            val previousPath = currentState.pathHistory.last()
            val updatedHistory = currentState.pathHistory.dropLast(1)
            viewModelScope.launch {
                loadFilesInternal(previousPath)
                _state.update { it.copy(pathHistory = updatedHistory) }
            }
        }
    }

    /**
     * Breadcrumb jump: when the target is already in the back history, truncate the
     * history up to it; otherwise treat it like a fresh navigation.
     */
    private fun navigateToPath(path: String) {
        val current = _state.value
        if (path == current.currentPath) return
        viewModelScope.launch {
            val history = current.pathHistory
            val index = history.indexOf(path)
            val newHistory = if (index >= 0) history.take(index) else history + current.currentPath
            loadFilesInternal(path)
            _state.update {
                it.copy(
                    pathHistory = newHistory,
                    selectionMode = false,
                    selectedPaths = emptySet(),
                    errorMessage = null
                )
            }
        }
    }

    private fun loadFiles(path: String, addToHistory: Boolean) {        viewModelScope.launch {
            val currentPath = _state.value.currentPath
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val files = withContext(Dispatchers.IO) {
                readDirectory(path)
            }

            _state.update { current ->
                val newHistory = if (addToHistory && currentPath.isNotEmpty() && currentPath != path) {
                    current.pathHistory + currentPath
                } else {
                    current.pathHistory
                }
                current.copy(
                    isLoading = false,
                    files = files.first,
                    currentPath = path,
                    rootPath = if (current.rootPath.isEmpty()) path else current.rootPath,
                    errorMessage = files.second,
                    pathHistory = newHistory,
                    selectedFile = null,
                    selectionMode = false,
                    selectedPaths = emptySet()
                )
            }
        }
    }

    private suspend fun loadFilesInternal(path: String) {
        _state.update { it.copy(isLoading = true) }
        val result = withContext(Dispatchers.IO) { readDirectory(path) }
        _state.update {
            it.copy(
                isLoading = false,
                files = result.first,
                currentPath = path,
                errorMessage = result.second
            )
        }
    }

    private fun reloadCurrent() {
        val path = _state.value.currentPath
        if (path.isNotEmpty()) {
            viewModelScope.launch { loadFilesInternal(path) }
        }
    }

    private fun readDirectory(path: String): Pair<List<FileItem>, String?> {
        return try {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) {
                return emptyList<FileItem>() to "Invalid path: $path"
            }
            val sortMode = SortMode.fromNameOrDefault(prefs.getString(SORT_MODE_KEY, null))
            val showHidden = settingsManager.settings.value.showHiddenFiles
            val files = (dir.listFiles() ?: emptyArray())
                .filter { showHidden || !it.name.startsWith(".") }
                .sortedWith(sortComparator(sortMode))
                .map { it.toFileItem() }
            files to null
        } catch (e: SecurityException) {
            emptyList<FileItem>() to "Permission denied"
        } catch (e: Exception) {
            emptyList<FileItem>() to "Failed to load files"
        }
    }

    private fun sortComparator(mode: SortMode): Comparator<File> {
        val foldersFirst = compareBy<File> { !it.isDirectory }
        return when (mode) {
            SortMode.NAME_ASC -> foldersFirst.thenBy { it.name.lowercase() }
            SortMode.NAME_DESC -> foldersFirst.thenByDescending { it.name.lowercase() }
            SortMode.SIZE_ASC -> foldersFirst.thenBy { it.length() }
            SortMode.SIZE_DESC -> foldersFirst.thenByDescending { it.length() }
            SortMode.DATE_ASC -> foldersFirst.thenBy { it.lastModified() }
            SortMode.DATE_DESC -> foldersFirst.thenByDescending { it.lastModified() }
            SortMode.TYPE_ASC -> foldersFirst.thenBy { it.extension.lowercase() }.thenBy { it.name.lowercase() }
        }
    }

    private fun File.toFileItem(): FileItem {
        val ext = extension.lowercase()
        val fileType = if (isDirectory) FileType.OTHER else FileType.fromExtension(ext)
        val fileFormat = if (isDirectory) FileFormat.UNKNOWN else FileFormat.fromExtension(ext)
        val dirType = if (isDirectory) DirectoryType.fromName(name) else DirectoryType.UNKNOWN
        return FileItem(
            name = name,
            path = absolutePath,
            size = if (isDirectory) 0L else length(),
            lastModified = lastModified(),
            isDirectory = isDirectory,
            fileType = fileType,
            fileFormat = fileFormat,
            directoryType = dirType,
            extension = ext
        )
    }

    private fun openFile(filePath: String) {
        val ext = File(filePath).extension.lowercase()
        val type = FileType.fromExtension(ext)
        // Track opens for the "Recently opened" list
        viewModelScope.launch {
            recentsManager.record(filePath)
        }
        when (type) {
            FileType.TEXT -> {
                _state.update { it.copy(openTextEditorFor = filePath) }
            }
            FileType.IMAGE -> {
                _state.update { it.copy(openImageViewerFor = filePath) }
            }
            FileType.AUDIO -> {
                _state.update { it.copy(openAudioPlayerFor = filePath) }
            }
            FileType.ARCHIVE -> {
                _state.update { it.copy(openArchiveFor = filePath) }
            }
            else -> {
                viewModelScope.launch {
                    try {
                        val file = File(filePath)
                        val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
                        val mimeType = MimeTypeHelper.getMimeType(file.name)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        val chooser = Intent.createChooser(intent, "Open with").apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        app.startActivity(chooser)
                    } catch (e: Exception) {
                        _state.update { it.copy(errorMessage = "Cannot open this file") }
                    }
                }
            }
        }
    }

    private fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            _state.update { it.copy(operationInProgress = true, selectedFile = null) }
            val success = withContext(Dispatchers.IO) {
                try {
                    val file = File(fileItem.path)
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                } catch (e: Exception) {
                    false
                }
            }
            if (success) {
                val currentPath = _state.value.currentPath
                loadFilesInternal(currentPath)
            } else {
                _state.update {
                    it.copy(
                        operationInProgress = false,
                        errorMessage = "Failed to delete \"${fileItem.name}\""
                    )
                }
            }
            _state.update { it.copy(operationInProgress = false) }
        }
    }

    private fun deletePaths(paths: List<String>) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    operationInProgress = true,
                    selectionMode = false,
                    selectedPaths = emptySet()
                )
            }
            val deleted = withContext(Dispatchers.IO) {
                paths.count { path ->
                    try {
                        val file = File(path)
                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            _state.update { it.copy(operationInProgress = false) }
            reloadCurrent()
            if (deleted < paths.size) {
                _state.update { it.copy(errorMessage = "Some files could not be deleted") }
            }
        }
    }

    private fun renameFile(fileItem: FileItem, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(operationInProgress = true, selectedFile = null) }
            val success = withContext(Dispatchers.IO) {
                try {
                    val oldFile = File(fileItem.path)
                    val newFile = File(oldFile.parent, newName)
                    if (newFile.exists()) return@withContext false
                    oldFile.renameTo(newFile)
                } catch (e: Exception) {
                    false
                }
            }
            if (success) {
                val currentPath = _state.value.currentPath
                loadFilesInternal(currentPath)
            } else {
                _state.update {
                    it.copy(
                        operationInProgress = false,
                        errorMessage = "Failed to rename file"
                    )
                }
            }
            _state.update { it.copy(operationInProgress = false) }
        }
    }

    private fun shareFile(fileItem: FileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = FileShareManager.shareFile(app, fileItem.path)
            result.errorMessage()?.let { error ->
                _state.update { it.copy(errorMessage = error, selectedFile = null) }
            } ?: run {
                _state.update { it.copy(selectedFile = null) }
            }
        }
    }

    /** "Open with…": let the user pick any installed app via the system chooser. */
    private fun openWith(fileItem: FileItem) {
        if (fileItem.isDirectory) return
        viewModelScope.launch {
            recentsManager.record(fileItem.path)
            val ok = withContext(Dispatchers.IO) {
                ir.heydar.nexfolder.core.files.FileOpener.openExternal(app, fileItem.path)
            }
            if (!ok) {
                _state.update { it.copy(errorMessage = "No app can open this file") }
            }
        }
    }

    private fun executeCompress(
        format: ir.heydar.nexfolder.core.archive.ArchiveFormat,
        archiveName: String,
        password: CharArray?
    ) {
        val sources = _state.value.compressSources ?: return
        val destinationDir = File(_state.value.currentPath)
        if (sources.isEmpty()) return

        _state.update { it.copy(compressSources = null) }
        operationJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    activeOperation = OperationProgress("Compressing", "", 0, 0),
                    selectionMode = false,
                    selectedPaths = emptySet()
                )
            }
            val output = FileOperations.uniqueDestination(
                destinationDir,
                "${archiveName.removeSuffix(".${format.extension}")}.${format.extension}"
            )
            val ok = try {
                archiveManager.create(
                    format = format,
                    sources = sources.map { File(it) }.filter { it.exists() },
                    output = output,
                    password = password,
                    onProgress = { progress ->
                        _state.update { it.copy(activeOperation = progress) }
                    },
                    isCancelled = { !isActive }
                )
            } catch (e: ArchivePasswordException) {
                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _state.update {
                it.copy(
                    activeOperation = null,
                    errorMessage = if (ok) null else "Compression failed"
                )
            }
            loadFilesInternal(_state.value.currentPath)
        }
    }

    private fun pasteTo(destinationPath: String) {        val currentState = _state.value
        val mode = currentState.clipboardMode ?: return
        val paths = currentState.clipboardPaths
        if (paths.isEmpty()) return

        operationJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    activeOperation = OperationProgress(
                        title = "${mode.actionLabel}ing ${paths.size} item(s)",
                        currentFile = "",
                        processedBytes = 0,
                        totalBytes = 0
                    ),
                    selectionMode = false,
                    selectedPaths = emptySet()
                )
            }
            var anyFailed = false
            var totalSize = 0L
            paths.forEach { path ->
                val file = File(path)
                if (file.exists()) totalSize += FileOperations.totalSize(file)
            }
            for (path in paths) {
                if (!isActive) break
                val source = File(path)
                if (!source.exists()) continue
                val target = FileOperations.uniqueDestination(File(destinationPath), source.name)
                val ok = try {
                    if (mode == ClipboardMode.COPY) {
                        FileOperations.copy(
                            source = source,
                            target = target,
                            onProgress = { progress ->
                                _state.update {
                                    it.copy(activeOperation = progress.copy(title = "Copying"))
                                }
                            },
                            isCancelled = { !isActive }
                        )
                    } else {
                        FileOperations.move(
                            source = source,
                            target = target,
                            onProgress = { progress ->
                                _state.update {
                                    it.copy(activeOperation = progress.copy(title = "Moving"))
                                }
                            },
                            isCancelled = { !isActive }
                        )
                    }
                } catch (e: Exception) {
                    false
                }
                if (!ok) anyFailed = true
            }
            _state.update {
                it.copy(
                    activeOperation = null,
                    // Move consumes the clipboard; copy stays available for pasting again
                    clipboardMode = if (mode == ClipboardMode.MOVE && !anyFailed) null else it.clipboardMode,
                    clipboardPaths = if (mode == ClipboardMode.MOVE && !anyFailed) emptyList() else it.clipboardPaths,
                    errorMessage = if (anyFailed) "Some items could not be ${mode.actionLabel.lowercase()}d" else null
                )
            }
            loadFilesInternal(_state.value.currentPath)
        }
    }
}
