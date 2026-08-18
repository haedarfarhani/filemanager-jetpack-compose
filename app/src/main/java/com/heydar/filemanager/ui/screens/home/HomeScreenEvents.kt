package com.heydar.filemanager.ui.screens.home

sealed class HomeEvent {
    data object LoadStorageVolumes : HomeEvent()
    data class OnStorageClicked(val path: String) : HomeEvent()
    data class ShowError(val message: String) : HomeEvent()
    data object DismissError : HomeEvent()
}