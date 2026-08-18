package com.heydar.filemanager.ui.screens.details

import com.heydar.filemanager.domain.model.FileDetails

data class FileDetailsUiState(
    val isLoading: Boolean = true,
    val details: FileDetails? = null,
    val error: String? = null,
    val isFolderSizeLoading: Boolean = false
)
