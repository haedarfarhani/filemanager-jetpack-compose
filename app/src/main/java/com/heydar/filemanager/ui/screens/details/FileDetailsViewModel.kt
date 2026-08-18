package com.heydar.filemanager.ui.screens.details

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heydar.filemanager.core.util.FileMetadataExtractor
import com.heydar.filemanager.core.util.FileShareManager
import com.heydar.filemanager.domain.model.SpecificFileInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class FileDetailsViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    private val _state = MutableStateFlow(FileDetailsUiState())
    val state: StateFlow<FileDetailsUiState> = _state.asStateFlow()

    /** Emits a user-visible error message for share failures. */
    private val _shareError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val shareError: SharedFlow<String> = _shareError.asSharedFlow()

    private var currentFilePath: String = ""

    fun onEvent(event: FileDetailsEvent) {
        when (event) {
            is FileDetailsEvent.LoadDetails -> loadDetails(event.filePath)
            FileDetailsEvent.ShareClicked   -> shareCurrentFile()
            FileDetailsEvent.RetryClicked   -> if (currentFilePath.isNotEmpty()) loadDetails(currentFilePath)
        }
    }

    private fun loadDetails(filePath: String) {
        currentFilePath = filePath
        _state.update { FileDetailsUiState(isLoading = true) }

        viewModelScope.launch {
            try {
                val details = withContext(Dispatchers.IO) {
                    FileMetadataExtractor.extract(app, filePath)
                }

                _state.update { it.copy(isLoading = false, details = details) }

                // If it's a folder, kick off lazy size calculation
                if (details.specific is SpecificFileInfo.FolderInfo) {
                    loadFolderSizeLazily(filePath)
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Could not load file details")
                }
            }
        }
    }

    private fun loadFolderSizeLazily(folderPath: String) {
        _state.update { it.copy(isFolderSizeLoading = true) }
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) {
                FileMetadataExtractor.computeFolderSize(folderPath)
            }
            _state.update { current ->
                val updatedSpecific = (current.details?.specific as? SpecificFileInfo.FolderInfo)
                    ?.copy(totalSize = size)
                    ?: current.details?.specific
                current.copy(
                    isFolderSizeLoading = false,
                    details = current.details?.copy(specific = updatedSpecific ?: SpecificFileInfo.GenericInfo)
                )
            }
        }
    }

    private fun shareCurrentFile() {
        val path = _state.value.details?.common?.path ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                FileShareManager.shareFile(app, path)
            }
            result.errorMessage()?.let { msg -> _shareError.tryEmit(msg) }
        }
    }
}
