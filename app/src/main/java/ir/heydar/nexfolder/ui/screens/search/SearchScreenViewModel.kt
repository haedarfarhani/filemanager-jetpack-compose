package ir.heydar.nexfolder.ui.screens.search

import android.app.Application
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.heydar.nexfolder.core.search.FileSearchEngine
import ir.heydar.nexfolder.core.util.MimeTypeHelper
import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.domain.model.SearchFilter
import ir.heydar.nexfolder.domain.model.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val app: Application,
    private val searchEngine: FileSearchEngine,
    private val settingsManager: ir.heydar.nexfolder.core.settings.SettingsManager
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var initialized = false

    init {
        _state.update { it.copy(storageRoot = Environment.getExternalStorageDirectory().absolutePath) }
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.Initialize -> {
                if (initialized) return
                initialized = true
                _state.update { it.copy(scopePath = event.scopePath) }
            }
            is SearchEvent.QueryChanged -> {
                _state.update { it.copy(query = event.query) }
                scheduleSearch()
            }
            is SearchEvent.ToggleWholeStorage -> {
                _state.update { it.copy(wholeStorage = event.whole) }
                scheduleSearch()
            }
            is SearchEvent.ToggleCategory -> {
                _state.update { current ->
                    val updated = if (event.category in current.selectedCategories) {
                        current.selectedCategories - event.category
                    } else {
                        current.selectedCategories + event.category
                    }
                    current.copy(selectedCategories = updated)
                }
                scheduleSearch()
            }
            is SearchEvent.SizePresetChanged -> {
                _state.update { it.copy(sizePreset = event.preset) }
                scheduleSearch()
            }
            is SearchEvent.DatePresetChanged -> {
                _state.update { it.copy(datePreset = event.preset) }
                scheduleSearch()
            }
            is SearchEvent.ToggleContentSearch -> {
                _state.update { it.copy(searchInContents = event.enabled) }
                scheduleSearch()
            }
            is SearchEvent.ChangeSort -> {
                _state.update {
                    it.copy(sort = event.sort, results = sortResults(it.results, event.sort))
                }
            }
            is SearchEvent.ClearFilters -> {
                _state.update {
                    it.copy(
                        query = "",
                        selectedCategories = emptySet(),
                        sizePreset = SizePreset.ANY,
                        datePreset = DatePreset.ANY,
                        searchInContents = false,
                        results = emptyList(),
                        searchedOnce = false
                    )
                }
            }
            is SearchEvent.ResultClicked -> openResult(event.result)
            is SearchEvent.ResultLongClicked -> {
                _state.update { it.copy(openDetailsFor = event.result.file.path) }
            }
            is SearchEvent.EditorHandled -> _state.update { it.copy(openTextEditorFor = null) }
            is SearchEvent.DetailsHandled -> _state.update { it.copy(openDetailsFor = null) }
        }
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch()
        }
    }

    private suspend fun runSearch() {
        val current = _state.value
        if (!current.filterActive()) {
            _state.update { it.copy(results = emptyList(), isSearching = false, searchedOnce = false, scannedCount = 0) }
            return
        }
        val root = if (current.wholeStorage) current.storageRoot else current.scopePath
        if (root.isEmpty()) return

        _state.update { it.copy(isSearching = true, results = emptyList(), scannedCount = 0) }
        try {
            val results = searchEngine.search(
                rootPath = root,
                filter = buildFilter(current)
            )
            _state.update {
                it.copy(
                    isSearching = false,
                    searchedOnce = true,
                    results = sortResults(results, it.sort)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(isSearching = false, searchedOnce = true) }
        }
    }

    private fun SearchUiState.filterActive(): Boolean =
        query.isNotBlank() || selectedCategories.isNotEmpty() ||
            sizePreset != SizePreset.ANY || datePreset != DatePreset.ANY

    private fun buildFilter(state: SearchUiState): SearchFilter {
        val now = System.currentTimeMillis()
        val dateFrom = state.datePreset.days?.let { now - it * 24 * 60 * 60 * 1000 }
        val (minSize, maxSize) = when (state.sizePreset) {
            SizePreset.ANY -> null to null
            SizePreset.LE_1MB -> null to 1L * 1024 * 1024
            SizePreset.LE_10MB -> null to 10L * 1024 * 1024
            SizePreset.LE_100MB -> null to 100L * 1024 * 1024
            SizePreset.GT_100MB -> 100L * 1024 * 1024 to null
        }
        val contentQuery = if (state.searchInContents && state.query.trim().length >= 2) {
            state.query.trim()
        } else null
        return SearchFilter(
            query = state.query,
            categories = state.selectedCategories,
            minSize = minSize,
            maxSize = maxSize,
            dateFrom = dateFrom,
            dateTo = null,
            contentQuery = contentQuery,
            includeHidden = settingsManager.settings.value.showHiddenFiles
        )
    }

    private fun sortResults(results: List<SearchResult>, sort: ResultSort): List<SearchResult> =
        when (sort) {
            ResultSort.BY_NAME -> results.sortedWith(
                compareBy({ it.file.isDirectory.not() }, { it.file.name.lowercase() })
            )
            ResultSort.BY_SIZE -> results.sortedByDescending { it.file.size }
            ResultSort.BY_DATE -> results.sortedByDescending { it.file.lastModified }
        }

    private fun openResult(result: SearchResult) {
        val fileItem = result.file
        if (fileItem.isDirectory) return
        if (fileItem.fileType == FileType.TEXT) {
            _state.update { it.copy(openTextEditorFor = fileItem.path) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(fileItem.path)
                val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, MimeTypeHelper.getMimeType(file.name))
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                app.startActivity(Intent.createChooser(intent, "Open with").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (e: Exception) {
                // Viewer apps unavailable; ignore
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
