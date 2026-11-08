package ir.heydar.nexfolder.ui.screens.archive

import ir.heydar.nexfolder.core.archive.ArchiveEntry
import ir.heydar.nexfolder.core.files.OperationProgress

data class ArchiveUiState(
    val archivePath: String = "",
    val entries: List<ArchiveEntry> = emptyList(),
    val isLoading: Boolean = true,
    val needsPassword: Boolean = false,
    val passwordRejected: Boolean = false,
    val errorMessage: String? = null,
    val activeOperation: OperationProgress? = null,
    /** Single entry pending extraction (null = extract all). */
    val pendingExtractEntry: String? = null
)
