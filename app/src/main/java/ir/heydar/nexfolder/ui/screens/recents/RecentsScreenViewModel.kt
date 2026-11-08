package ir.heydar.nexfolder.ui.screens.recents

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.heydar.nexfolder.core.files.FileOpener
import ir.heydar.nexfolder.core.history.RecentsManager
import ir.heydar.nexfolder.core.media.MediaStoreQueries
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.FileType
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@HiltViewModel
class RecentsViewModel @Inject constructor(
    private val app: Application,
    private val recentsManager: RecentsManager
) : ViewModel() {

    private val _state = MutableStateFlow(RecentsUiState())
    val state: StateFlow<RecentsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: RecentsEvent) {
        when (event) {
            is RecentsEvent.TabChanged -> {
                _state.update { it.copy(tab = event.tab) }
                refresh()
            }
            is RecentsEvent.Refresh -> refresh()
            is RecentsEvent.ItemClicked -> openItem(event.fileItem)
            is RecentsEvent.ItemLongClicked -> {
                _state.update { it.copy(openDetailsFor = event.fileItem.path) }
            }
            is RecentsEvent.EditorHandled -> _state.update { it.copy(openTextEditorFor = null) }
            is RecentsEvent.DetailsHandled -> _state.update { it.copy(openDetailsFor = null) }
            is RecentsEvent.ViewerHandled -> _state.update { it.copy(openViewerFor = null) }
            is RecentsEvent.PlayerHandled -> _state.update { it.copy(openPlayerFor = null) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val tab = _state.value.tab
            when (tab) {
                RecentsTab.ADDED -> {
                    val files = withContext(Dispatchers.IO) {
                        MediaStoreQueries.queryRecentlyAdded(app, limit = 100)
                            .filter { File(it.path).exists() }
                    }
                    _state.update { it.copy(isLoading = false, addedFiles = files) }
                }
                RecentsTab.OPENED -> {
                    val snapshot = withContext(Dispatchers.IO) {
                        try {
                            recentsManager.observeRecentOpened(100).firstOrNull() ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                    val files = snapshot.mapNotNull { entity ->
                        val file = File(entity.path)
                        if (!file.isFile) return@mapNotNull null
                        FileItem(
                            name = entity.name,
                            path = entity.path,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            isDirectory = false,
                            fileType = FileType.fromExtension(entity.extension),
                            extension = entity.extension,
                            fileFormat = ir.heydar.nexfolder.domain.model.FileFormat.fromExtension(entity.extension)
                        )
                    }
                    _state.update { it.copy(isLoading = false, openedFiles = files) }
                }
            }
        }
    }

    private fun openItem(fileItem: FileItem) {
        viewModelScope.launch {
            recentsManager.record(fileItem.path)
        }
        when (fileItem.fileType) {
            FileType.TEXT -> {
                _state.update { it.copy(openTextEditorFor = fileItem.path) }
            }
            FileType.IMAGE -> {
                _state.update { it.copy(openViewerFor = fileItem.path) }
            }
            FileType.AUDIO -> {
                _state.update { it.copy(openPlayerFor = fileItem.path) }
            }
            else -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val ok = FileOpener.openExternal(app, fileItem.path)
                    if (!ok) {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }
}
