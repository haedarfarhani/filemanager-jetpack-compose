package ir.heydar.nexfolder.ui.screens.details

sealed interface FileDetailsEvent {
    data class LoadDetails(val filePath: String) : FileDetailsEvent
    object ShareClicked : FileDetailsEvent
    object OpenWithClicked : FileDetailsEvent
    object RetryClicked : FileDetailsEvent
}
