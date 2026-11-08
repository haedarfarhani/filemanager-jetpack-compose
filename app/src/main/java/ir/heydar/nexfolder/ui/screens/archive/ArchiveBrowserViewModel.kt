package ir.heydar.nexfolder.ui.screens.archive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.heydar.nexfolder.core.archive.ArchiveEntry
import ir.heydar.nexfolder.core.archive.ArchiveManager
import ir.heydar.nexfolder.core.archive.ArchivePasswordException
import ir.heydar.nexfolder.core.files.FileOperations
import ir.heydar.nexfolder.core.files.OperationProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val archiveManager: ArchiveManager
) : ViewModel() {

    val archivePath: String = savedStateHandle.get<String>("archivePath") ?: ""

    private val _state = MutableStateFlow(ArchiveUiState(archivePath = archivePath))
    val state: StateFlow<ArchiveUiState> = _state.asStateFlow()

    private var password: CharArray? = null
    private var operationJob: Job? = null

    init {
        loadEntries()
    }

    fun onEvent(event: ArchiveEvent) {
        when (event) {
            is ArchiveEvent.PasswordEntered -> {
                password = event.password.toCharArray()
                loadEntries()
            }
            is ArchiveEvent.PasswordDismissed -> {
                _state.update { it.copy(needsPassword = false, passwordRejected = false) }
            }
            is ArchiveEvent.ExtractConfirmed -> {
                extract(event.destination)
            }
            is ArchiveEvent.CancelOperation -> {
                operationJob?.cancel()
                _state.update { it.copy(activeOperation = null) }
            }
            is ArchiveEvent.ErrorShown -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun loadEntries() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, needsPassword = false, passwordRejected = false, errorMessage = null) }
            try {
                val entries = archiveManager.listEntries(archivePath, password)
                _state.update {
                    it.copy(isLoading = false, entries = sortEntries(entries))
                }
            } catch (e: ArchivePasswordException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        needsPassword = true,
                        passwordRejected = password != null,
                        entries = emptyList()
                    )
                }
                password = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to open archive"
                    )
                }
            }
        }
    }

    private fun sortEntries(entries: List<ArchiveEntry>): List<ArchiveEntry> =
        entries.sortedWith(
            compareBy({ !it.isDirectory }, { it.path.substringBefore('/').lowercase() }, { it.name.lowercase() })
        )

    private fun extract(destination: String) {
        val pendingEntry = _state.value.pendingExtractEntry
        val entryNames = if (pendingEntry == null) null else listOf(pendingEntry)
        operationJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    activeOperation = OperationProgress("Extracting", "", 0, 0),
                    pendingExtractEntry = null
                )
            }
            val target = FileOperations.uniqueDestination(File(destination), archiveName())
            val ok = try {
                archiveManager.extract(
                    archivePath = archivePath,
                    destinationDir = target,
                    entryNames = entryNames,
                    password = password,
                    onProgress = { progress ->
                        _state.update { it.copy(activeOperation = progress) }
                    },
                    isCancelled = { !isActive }
                )
            } catch (e: ArchivePasswordException) {
                _state.update {
                    it.copy(activeOperation = null, needsPassword = true, passwordRejected = true)
                }
                return@launch
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _state.update {
                it.copy(
                    activeOperation = null,
                    errorMessage = if (ok) null else "Extraction failed"
                )
            }
        }
    }

    private fun archiveName(): String =
        File(archivePath).name.substringBeforeLast('.').ifEmpty { "archive" }

    /** Called by the screen before a paste destination is picked. */
    fun requestExtractAll() {
        _state.update { it.copy(pendingExtractEntry = null) }
    }

    fun requestExtractEntry(entry: ArchiveEntry) {
        if (entry.isDirectory) return
        _state.update { it.copy(pendingExtractEntry = entry.path) }
    }
}
