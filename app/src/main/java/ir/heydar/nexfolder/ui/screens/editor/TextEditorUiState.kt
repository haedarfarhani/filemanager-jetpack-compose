package ir.heydar.nexfolder.ui.screens.editor

data class TextEditorUiState(
    val filePath: String = "",
    val fileName: String = "",
    val content: String = "",
    val initialContent: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val showUnsavedDialog: Boolean = false,
    /** CSV/TSV grid view toggle (auto-on for .csv/.tsv files). */
    val isCsvMode: Boolean = false,
    val isTabSeparated: Boolean = false
)
