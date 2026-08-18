package com.heydar.filemanager.ui.screens.details

sealed interface FileDetailsEvent {
    data class LoadDetails(val filePath: String) : FileDetailsEvent
    object ShareClicked : FileDetailsEvent
    object RetryClicked : FileDetailsEvent
}
