package ir.heydar.nexfolder.ui.screens.viewer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.heydar.nexfolder.core.history.RecentsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel
class ImageViewerViewModel @Inject constructor(
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val app: Application,
    private val recentsManager: RecentsManager
) : ViewModel() {

    private val initialPath: String = savedStateHandle.get<String>("filePath") ?: ""

    private val _state = MutableStateFlow(ImageViewerUiState())
    val state: StateFlow<ImageViewerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            recentsManager.record(initialPath)
            val folder = File(initialPath).parentFile
            val siblings = folder?.listFiles()
                ?.filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
                ?.map { it.absolutePath }
                ?.sortedBy { it.lowercase() }
                ?: listOf(initialPath)
            val startIndex = siblings.indexOf(initialPath).takeIf { it >= 0 }
                ?: siblings.indexOfFirst { it.equals(initialPath, ignoreCase = true) }
                    .takeIf { it >= 0 }
                ?: 0
            _state.update {
                it.copy(imagePaths = siblings, currentIndex = startIndex, isLoading = false)
            }
        }
    }

    fun onEvent(event: ViewerEvent) {
        when (event) {
            is ViewerEvent.Next -> _state.update {
                it.copy(currentIndex = (it.currentIndex + 1).coerceAtMost(it.imagePaths.lastIndex))
            }
            is ViewerEvent.Previous -> _state.update {
                it.copy(currentIndex = (it.currentIndex - 1).coerceAtLeast(0))
            }
            is ViewerEvent.PageChanged -> _state.update { current ->
                if (current.imagePaths.isNotEmpty()) {
                    current.copy(currentIndex = event.index.coerceIn(0, current.imagePaths.lastIndex))
                } else {
                    // Pager emits its initial page before the sibling list loads
                    current
                }
            }
        }
    }

    private companion object {
        val IMAGE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "avif", "tif", "tiff", "svg"
        )
    }
}

data class ImageViewerUiState(
    val imagePaths: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true
) {
    val currentPath: String
        get() = imagePaths.getOrNull(currentIndex) ?: ""
}
