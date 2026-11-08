package ir.heydar.nexfolder.ui.screens.files

import ir.heydar.nexfolder.domain.model.FileDisplayMode
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.SortMode

sealed class FileListEvent {
    /** Screen became visible: restore the last visited folder (e.g. after returning from a viewer). */
    data class ScreenEntered(val rootPath: String) : FileListEvent()
    data class LoadFiles(val path: String) : FileListEvent()
    data class OnFolderClicked(val path: String) : FileListEvent()
    data class OnFileClicked(val path: String) : FileListEvent()
    /** Jump to an ancestor folder shown in the breadcrumb. */
    data class NavigateToPath(val path: String) : FileListEvent()
    data class OnFileLongClicked(val fileItem: FileItem) : FileListEvent()
    data object OnBackClicked : FileListEvent()
    data object DismissSelection : FileListEvent()
    data class ChangeDisplayMode(val mode: FileDisplayMode) : FileListEvent()
    data object TextEditorHandled : FileListEvent()
    data class OnDetailsClicked(val fileItem: FileItem) : FileListEvent()
    data object DetailsHandled : FileListEvent()
    data object ViewerHandled : FileListEvent()
    data object PlayerHandled : FileListEvent()
    data object ArchiveHandled : FileListEvent()

    // File operations
    data class OnDeleteConfirmed(val fileItem: FileItem) : FileListEvent()
    data class OnRenameConfirmed(val fileItem: FileItem, val newName: String) : FileListEvent()
    data class OnShareFile(val fileItem: FileItem) : FileListEvent()
    data class OnCopyFile(val fileItem: FileItem) : FileListEvent()
    data class OnMoveFile(val fileItem: FileItem) : FileListEvent()
    /** Open the file with an app chosen by the user (system chooser). */
    data class OnOpenWith(val fileItem: FileItem) : FileListEvent()

    // Multi-selection
    data class ToggleSelected(val path: String) : FileListEvent()
    data object SelectAll : FileListEvent()
    data object ExitSelectionMode : FileListEvent()
    data class OnDeleteSelectedConfirmed(val paths: List<String>) : FileListEvent()

    // Clipboard / paste
    data class OnPasteConfirmed(val destinationPath: String) : FileListEvent()
    data object OnCancelClipboard : FileListEvent()
    data object OnCancelOperation : FileListEvent()
    data object ErrorShown : FileListEvent()

    // Sort
    data class ChangeSort(val mode: SortMode) : FileListEvent()

    // Compression
    data class OnCompressClicked(val sources: List<String>) : FileListEvent()
    data object OnCompressCancelled : FileListEvent()
    data class OnCompressConfirmed(
        val format: ir.heydar.nexfolder.core.archive.ArchiveFormat,
        val archiveName: String,
        val password: CharArray?
    ) : FileListEvent()
}
