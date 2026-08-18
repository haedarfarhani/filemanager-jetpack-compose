package com.heydar.filemanager.ui.screens.network

sealed interface NetworkShareEvent {
    data object StartServer : NetworkShareEvent
    data object StopServer : NetworkShareEvent
    data class SetPort(val port: Int) : NetworkShareEvent
    data class SetRootPath(val path: String) : NetworkShareEvent
    data class SetPassword(val password: String) : NetworkShareEvent
    data object TogglePassword : NetworkShareEvent
    data object DismissError : NetworkShareEvent
    data object CopyUrl : NetworkShareEvent
}
