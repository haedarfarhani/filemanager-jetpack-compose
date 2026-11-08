package ir.heydar.nexfolder.ui.screens.details

import ir.heydar.nexfolder.domain.model.FileDetails

data class FileDetailsUiState(
    val isLoading: Boolean = true,
    val details: FileDetails? = null,
    val error: String? = null,
    val isFolderSizeLoading: Boolean = false
)
