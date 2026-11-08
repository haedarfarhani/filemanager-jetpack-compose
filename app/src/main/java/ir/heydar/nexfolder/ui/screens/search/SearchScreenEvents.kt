package ir.heydar.nexfolder.ui.screens.search

import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.domain.model.SearchResult

sealed class SearchEvent {
    data class Initialize(val scopePath: String) : SearchEvent()
    data class QueryChanged(val query: String) : SearchEvent()
    data class ToggleWholeStorage(val whole: Boolean) : SearchEvent()
    data class ToggleCategory(val category: FileType) : SearchEvent()
    data class SizePresetChanged(val preset: SizePreset) : SearchEvent()
    data class DatePresetChanged(val preset: DatePreset) : SearchEvent()
    data class ToggleContentSearch(val enabled: Boolean) : SearchEvent()
    data class ChangeSort(val sort: ResultSort) : SearchEvent()
    data object ClearFilters : SearchEvent()
    data class ResultClicked(val result: SearchResult) : SearchEvent()
    data class ResultLongClicked(val result: SearchResult) : SearchEvent()
    data object EditorHandled : SearchEvent()
    data object DetailsHandled : SearchEvent()
}
