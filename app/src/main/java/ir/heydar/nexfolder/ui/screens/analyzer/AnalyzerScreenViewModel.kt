package ir.heydar.nexfolder.ui.screens.analyzer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.heydar.nexfolder.core.analyzer.StorageAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StorageAnalyzerViewModel @Inject constructor(
    private val analyzer: StorageAnalyzer
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyzerUiState(isScanning = true))
    val state: StateFlow<AnalyzerUiState> = _state.asStateFlow()

    private var rootPath: String = ""
    private var scanJob: Job? = null

    fun startScan(path: String = rootPath) {
        val target = resolveRoot(path)
        rootPath = target
        if (target.isEmpty()) {
            _state.update {
                it.copy(isScanning = false, analysis = null, errorMessage = "Invalid storage path")
            }
            return
        }
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    rootPath = target,
                    isScanning = true,
                    scannedFiles = 0,
                    errorMessage = null,
                    analysis = null
                )
            }
            try {
                val analysis = analyzer.analyze(target) { scanned, _ ->
                    _state.update { it.copy(scannedFiles = scanned) }
                }
                _state.update { it.copy(isScanning = false, analysis = analysis) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isScanning = false, errorMessage = e.message ?: "Scan failed")
                }
            }
        }
    }

    fun rescan() = startScan(rootPath)

    /**
     * Navigation arguments can arrive URL-encoded. Scanning an encoded path yields an
     * empty analysis (every counter stays at 0), so normalize before scanning.
     */
    private fun resolveRoot(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) return trimmed
        if (File(trimmed).isDirectory) return trimmed
        val decoded = Uri.decode(trimmed)
        return if (decoded != trimmed && File(decoded).isDirectory) decoded else trimmed
    }
}
