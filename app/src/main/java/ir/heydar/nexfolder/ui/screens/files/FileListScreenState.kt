package ir.heydar.nexfolder.ui.screens.files

import ir.heydar.nexfolder.core.files.OperationProgress
import ir.heydar.nexfolder.domain.model.ClipboardMode
import ir.heydar.nexfolder.domain.model.FileDisplayMode
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.SortMode

data class FileListUiState(
    val currentPath: String = "",
    val rootPath: String = "",
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pathHistory: List<String> = emptyList(),
    val selectedFile: FileItem? = null,
    val operationInProgress: Boolean = false,
    val displayMode: FileDisplayMode = FileDisplayMode.LIST_LARGE,
    val openTextEditorFor: String? = null,
    val openDetailsFor: String? = null,
    val openImageViewerFor: String? = null,
    val openAudioPlayerFor: String? = null,
    val openArchiveFor: String? = null,
    // Multi-selection
    val selectionMode: Boolean = false,
    val selectedPaths: Set<String> = emptySet(),
    // Pending copy/move ("clipboard")
    val clipboardMode: ClipboardMode? = null,
    val clipboardPaths: List<String> = emptyList(),
    // Sort
    val sortMode: SortMode = SortMode.NAME_ASC,
    // Long-running operation with progress (copy/move/extract/compress)
    val activeOperation: OperationProgress? = null,
    // Pending compression request (sources picked, dialog open)
    val compressSources: List<String>? = null
)
