package ir.heydar.nexfolder.ui.screens.archive

sealed class ArchiveEvent {
    data class PasswordEntered(val password: String) : ArchiveEvent()
    data object PasswordDismissed : ArchiveEvent()
    /** Destination chosen in the directory picker; null pendingExtractEntry = extract all. */
    data class ExtractConfirmed(val destination: String) : ArchiveEvent()
    data object CancelOperation : ArchiveEvent()
    data object ErrorShown : ArchiveEvent()
}
