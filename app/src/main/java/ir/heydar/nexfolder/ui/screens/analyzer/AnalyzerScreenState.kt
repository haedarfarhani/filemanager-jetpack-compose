package ir.heydar.nexfolder.ui.screens.analyzer

import ir.heydar.nexfolder.domain.model.StorageAnalysis

data class AnalyzerUiState(
    val rootPath: String = "",
    val isScanning: Boolean = false,
    val scannedFiles: Long = 0,
    val analysis: StorageAnalysis? = null,
    val errorMessage: String? = null
)
