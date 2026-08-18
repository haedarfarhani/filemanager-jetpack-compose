package com.heydar.filemanager.ui.screens.files

import com.heydar.filemanager.domain.model.FileItem

import com.heydar.filemanager.domain.model.FileDisplayMode

sealed class FileListEvent {
    data class LoadFiles(val path: String) : FileListEvent()
    data class OnFolderClicked(val path: String) : FileListEvent()
    data class OnFileClicked(val path: String) : FileListEvent()
    data class OnFileLongClicked(val fileItem: FileItem) : FileListEvent()
    data object OnBackClicked : FileListEvent()
    data object DismissSelection : FileListEvent()
    data class ChangeDisplayMode(val mode: FileDisplayMode) : FileListEvent()
    data object TextEditorHandled : FileListEvent()
    data class OnDetailsClicked(val fileItem: FileItem) : FileListEvent()
    data object DetailsHandled : FileListEvent()

    // File operations
    data class OnDeleteConfirmed(val fileItem: FileItem) : FileListEvent()
    data class OnRenameConfirmed(val fileItem: FileItem, val newName: String) : FileListEvent()
    data class OnShareFile(val fileItem: FileItem) : FileListEvent()
    data class OnCopyFile(val fileItem: FileItem) : FileListEvent()
    data class OnMoveFile(val fileItem: FileItem) : FileListEvent()
}