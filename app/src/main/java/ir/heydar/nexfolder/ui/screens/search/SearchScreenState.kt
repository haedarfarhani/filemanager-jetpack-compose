package ir.heydar.nexfolder.ui.screens.search

import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.domain.model.SearchResult

/** Result ordering options for the search screen. */
enum class ResultSort(val label: String) {
    BY_NAME("Name"),
    BY_SIZE("Size"),
    BY_DATE("Date")
}

/** Quick size-bucket presets. */
enum class SizePreset(val label: String) {
    ANY("Any size"),
    LE_1MB("\u2264 1 MB"),
    LE_10MB("\u2264 10 MB"),
    LE_100MB("\u2264 100 MB"),
    GT_100MB("> 100 MB")
}

/** Quick "last modified" presets. */
enum class DatePreset(val label: String, val days: Long?) {
    ANY("Any time", null),
    TODAY("Today", 1),
    WEEK("This week", 7),
    MONTH("This month", 30)
}

data class SearchUiState(
    val scopePath: String = "",
    val storageRoot: String = "",
    val wholeStorage: Boolean = true,
    val query: String = "",
    val selectedCategories: Set<FileType> = emptySet(),
    val sizePreset: SizePreset = SizePreset.ANY,
    val datePreset: DatePreset = DatePreset.ANY,
    val searchInContents: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val scannedCount: Int = 0,
    val isSearching: Boolean = false,
    val searchedOnce: Boolean = false,
    val sort: ResultSort = ResultSort.BY_DATE,
    val openTextEditorFor: String? = null,
    val openDetailsFor: String? = null
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || selectedCategories.isNotEmpty() ||
            sizePreset != SizePreset.ANY || datePreset != DatePreset.ANY
}
