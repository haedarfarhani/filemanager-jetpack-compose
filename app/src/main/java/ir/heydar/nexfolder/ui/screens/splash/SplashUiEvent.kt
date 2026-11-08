package ir.heydar.nexfolder.ui.screens.splash

sealed class SplashUiEvent {
    data object NavigateToHome : SplashUiEvent()
}
