package com.heydar.filemanager.ui.screens.network

sealed interface NetworkFileListEvent {
    data class LoadFiles(val serverUrl: String, val path: String = "") : NetworkFileListEvent
    data class NavigateTo(val path: String) : NetworkFileListEvent
    data class DownloadFile(val path: String) : NetworkFileListEvent
    data object GoBack : NetworkFileListEvent
    data object Refresh : NetworkFileListEvent
    data object DismissError : NetworkFileListEvent
}
