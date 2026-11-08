package ir.heydar.nexfolder.ui.screens.recents

import ir.heydar.nexfolder.domain.model.FileItem

enum class RecentsTab(val label: String) {
    ADDED("Recently added"),
    OPENED("Recently opened")
}

data class RecentsUiState(
    val tab: RecentsTab = RecentsTab.ADDED,
    val addedFiles: List<FileItem> = emptyList(),
    val openedFiles: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val openTextEditorFor: String? = null,
    val openDetailsFor: String? = null,
    /** Files routed to the built-in image viewer (Phase E) / audio player (Phase E). */
    val openViewerFor: String? = null,
    val openPlayerFor: String? = null
)
