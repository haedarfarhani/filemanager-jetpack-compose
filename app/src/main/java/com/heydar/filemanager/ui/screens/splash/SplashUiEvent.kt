package com.heydar.filemanager.ui.screens.splash

sealed class SplashUiEvent {
    data object NavigateToHome : SplashUiEvent()
}
