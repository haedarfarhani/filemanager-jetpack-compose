package com.heydar.filemanager.ui.screens.files

import com.heydar.filemanager.domain.model.FileItem

import com.heydar.filemanager.domain.model.FileDisplayMode

data class FileListUiState(
    val currentPath: String = "",
    val rootPath: String = "",
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pathHistory: List<String> = emptyList(),
    val selectedFile: FileItem? = null,
    val operationInProgress: Boolean = false,
    val displayMode: FileDisplayMode = FileDisplayMode.LIST_LARGE,
    val openTextEditorFor: String? = null,
    val openDetailsFor: String? = null
)