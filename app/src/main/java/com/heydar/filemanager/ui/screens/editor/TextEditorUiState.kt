package com.heydar.filemanager.ui.screens.editor

data class TextEditorUiState(
    val filePath: String = "",
    val fileName: String = "",
    val content: String = "",
    val initialContent: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val showUnsavedDialog: Boolean = false
)
