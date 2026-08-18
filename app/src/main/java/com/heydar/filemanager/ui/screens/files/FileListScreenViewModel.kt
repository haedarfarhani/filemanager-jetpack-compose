package com.heydar.filemanager.ui.screens.files

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heydar.filemanager.core.util.MimeTypeHelper
import com.heydar.filemanager.domain.model.DirectoryType
import com.heydar.filemanager.domain.model.FileFormat
import com.heydar.filemanager.core.util.FileShareManager
import com.heydar.filemanager.domain.model.FileItem
import com.heydar.filemanager.domain.model.FileType
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import android.content.Context
import com.heydar.filemanager.domain.model.FileDisplayMode

@HiltViewModel
class FileListViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    private val prefs = app.getSharedPreferences("file_manager_prefs", Context.MODE_PRIVATE)
    private val DISPLAY_MODE_KEY = "display_mode"

    private val _state = MutableStateFlow(FileListUiState())
    val state: StateFlow<FileListUiState> = _state.asStateFlow()

    init {
        val savedModeString = prefs.getString(DISPLAY_MODE_KEY, FileDisplayMode.LIST_LARGE.name) ?: FileDisplayMode.LIST_LARGE.name
        val savedMode = try { FileDisplayMode.valueOf(savedModeString) } catch (e: Exception) { FileDisplayMode.LIST_LARGE }
        _state.update { it.copy(displayMode = savedMode) }
    }

    fun onEvent(event: FileListEvent) {
        when (event) {
            is FileListEvent.LoadFiles -> loadFiles(event.path, addToHistory = false)
            is FileListEvent.OnFolderClicked -> loadFiles(event.path, addToHistory = true)
            is FileListEvent.OnFileClicked -> openFile(event.path)
            is FileListEvent.OnFileLongClicked -> {
                _state.update { it.copy(selectedFile = event.fileItem) }
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
            is FileListEvent.OnDeleteConfirmed -> deleteFile(event.fileItem)
            is FileListEvent.OnRenameConfirmed -> renameFile(event.fileItem, event.newName)
            is FileListEvent.OnShareFile -> shareFile(event.fileItem)
            is FileListEvent.OnCopyFile -> { /* Planned: folder picker required */ }
            is FileListEvent.OnMoveFile -> { /* Planned: folder picker required */ }
        }
    }

    fun canGoBack(): Boolean = _state.value.pathHistory.isNotEmpty()

    private fun handleBackNavigation() {
        val currentState = _state.value
        if (currentState.pathHistory.isNotEmpty()) {
            val previousPath = currentState.pathHistory.last()
            val updatedHistory = currentState.pathHistory.dropLast(1)
            viewModelScope.launch {
                loadFilesInternal(previousPath)
                _state.update { it.copy(pathHistory = updatedHistory) }
            }
        }
    }

    private fun loadFiles(path: String, addToHistory: Boolean) {
        viewModelScope.launch {
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
                    selectedFile = null
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

    private fun readDirectory(path: String): Pair<List<FileItem>, String?> {
        return try {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) {
                return emptyList<FileItem>() to "Invalid path: $path"
            }
            val files = dir.listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?.map { it.toFileItem() }
                ?: emptyList()
            files to null
        } catch (e: SecurityException) {
            emptyList<FileItem>() to "Permission denied"
        } catch (e: Exception) {
            emptyList<FileItem>() to "Failed to load files"
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
        if (type == FileType.TEXT) {
            _state.update { it.copy(openTextEditorFor = filePath) }
            return
        }

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
}