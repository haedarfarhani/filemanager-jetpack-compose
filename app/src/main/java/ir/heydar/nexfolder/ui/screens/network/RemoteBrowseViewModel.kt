package ir.heydar.nexfolder.ui.screens.network

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.heydar.nexfolder.core.files.FileOperations
import ir.heydar.nexfolder.core.network.clients.ConnectionStore
import ir.heydar.nexfolder.core.network.clients.NetworkClient
import ir.heydar.nexfolder.core.network.clients.NetworkClientFactory
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import ir.heydar.nexfolder.domain.model.network.RemoteConnection
import ir.heydar.nexfolder.domain.model.network.RemoteFileEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

sealed class RemoteBrowseEvent {
    data class Initialize(val connectionId: String) : RemoteBrowseEvent()
    data class EnterFolder(val entry: RemoteFileEntry) : RemoteBrowseEvent()
    data object Back : RemoteBrowseEvent()
    data object Refresh : RemoteBrowseEvent()
    data class DownloadClicked(val entry: RemoteFileEntry) : RemoteBrowseEvent()
    data class OptionsRequested(val entry: RemoteFileEntry) : RemoteBrowseEvent()
    data object OptionsDismissed : RemoteBrowseEvent()
    data class DeleteClicked(val entry: RemoteFileEntry) : RemoteBrowseEvent()
    data class RenameRequested(val entry: RemoteFileEntry, val newName: String) : RemoteBrowseEvent()
    data class MkdirRequested(val name: String) : RemoteBrowseEvent()
    data class ShowMkdirDialog(val show: Boolean) : RemoteBrowseEvent()
    data class UploadUris(val uris: List<Uri>) : RemoteBrowseEvent()
    data object UploadHandled : RemoteBrowseEvent()
    data object ErrorShown : RemoteBrowseEvent()
}

data class RemoteBrowseUiState(
    val connection: RemoteConnection? = null,
    val path: String = "/",
    val entries: List<RemoteFileEntry> = emptyList(),
    val pathHistory: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val operationInProgress: Boolean = false,
    val operationMessage: String = "",
    val optionsEntry: RemoteFileEntry? = null,
    val showMkdirDialog: Boolean = false,
    val uploadReadyUris: List<Uri> = emptyList()
)

@HiltViewModel
class RemoteBrowseViewModel @Inject constructor(
    private val app: Application,
    private val store: ConnectionStore
) : ViewModel() {

    private val _state = MutableStateFlow(RemoteBrowseUiState())
    val state: StateFlow<RemoteBrowseUiState> = _state.asStateFlow()

    private var connectionId: String? = null
    private var client: NetworkClient? = null

    fun onEvent(event: RemoteBrowseEvent) {
        when (event) {
            is RemoteBrowseEvent.Initialize -> initialize(event.connectionId)
            is RemoteBrowseEvent.EnterFolder -> enterFolder(event.entry)
            is RemoteBrowseEvent.Back -> {
                val current = _state.value
                if (current.pathHistory.isNotEmpty()) {
                    _state.update {
                        it.copy(path = it.pathHistory.last(), pathHistory = it.pathHistory.dropLast(1))
                    }
                    viewModelScope.launch { refresh() }
                }
            }
            is RemoteBrowseEvent.Refresh -> viewModelScope.launch { refresh() }
            is RemoteBrowseEvent.DownloadClicked -> download(event.entry)
            is RemoteBrowseEvent.OptionsRequested -> _state.update { it.copy(optionsEntry = event.entry) }
            is RemoteBrowseEvent.OptionsDismissed -> _state.update { it.copy(optionsEntry = null) }
            is RemoteBrowseEvent.DeleteClicked -> delete(event.entry)
            is RemoteBrowseEvent.RenameRequested -> rename(event.entry, event.newName)
            is RemoteBrowseEvent.MkdirRequested -> mkdir(event.name)
            is RemoteBrowseEvent.ShowMkdirDialog -> _state.update { it.copy(showMkdirDialog = event.show) }
            is RemoteBrowseEvent.UploadUris -> upload(event.uris)
            is RemoteBrowseEvent.UploadHandled -> _state.update { it.copy(uploadReadyUris = emptyList()) }
            is RemoteBrowseEvent.ErrorShown -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun initialize(id: String) {
        if (connectionId == id) return
        connectionId = id
        val connection = store.findById(id)
        if (connection == null) {
            _state.update { it.copy(isLoading = false, errorMessage = "Connection not found") }
            return
        }
        _state.update { it.copy(connection = connection) }
        viewModelScope.launch {
            val ok = ensureConnected()
            if (ok) refresh() else _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun ensureConnected(): Boolean {
        val connection = _state.value.connection ?: return false
        if (client == null) {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val created = withContext(Dispatchers.IO) {
                    NetworkClientFactory.create(connection).also { it.connect() }
                }
                client = created
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Connection failed") }
                return false
            }
        }
        return true
    }

    private suspend fun refresh() {
        if (!ensureConnected()) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val entries = withContext(Dispatchers.IO) { client!!.list(_state.value.path) }
            _state.update {
                it.copy(
                    isLoading = false,
                    entries = entries.sortedWith(
                        compareBy({ !it.isDirectory }, { it.name.lowercase() })
                    )
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Listing failed") }
        }
    }

    private fun enterFolder(entry: RemoteFileEntry) {
        if (!entry.isDirectory) return
        _state.update {
            it.copy(
                path = entry.path.trimEnd('/') + "/",
                pathHistory = it.pathHistory + it.path
            )
        }
        viewModelScope.launch { refresh() }
    }

    private fun download(entry: RemoteFileEntry) {
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            _state.update { it.copy(operationInProgress = true, operationMessage = "Downloading ${entry.name}…") }
            try {
                val downloads = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                downloads.mkdirs()
                val target = FileOperations.uniqueDestination(downloads, entry.name)
                withContext(Dispatchers.IO) {
                    client!!.download(entry.path, target)
                }
                _state.update { it.copy(operationInProgress = false, errorMessage = "Saved to Downloads: ${entry.name}") }
            } catch (e: Exception) {
                _state.update { it.copy(operationInProgress = false, errorMessage = e.message ?: "Download failed") }
            }
        }
    }

    private fun upload(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            _state.update { it.copy(operationInProgress = true, operationMessage = "Uploading…") }
            try {
                val resolver = app.contentResolver
                withContext(Dispatchers.IO) {
                    uris.forEach { uri ->
                        val fileName = queryDisplayName(resolver, uri) ?: "upload_${System.currentTimeMillis()}"
                        val temp = File.createTempFile("up_", fileName, app.cacheDir)
                        try {
                            resolver.openInputStream(uri)?.use { input ->
                                temp.outputStream().use { output -> input.copyTo(output) }
                            }
                            client!!.upload(temp, joinRemotePath(_state.value.path, fileName))
                        } finally {
                            temp.delete()
                        }
                    }
                }
                _state.update { it.copy(operationInProgress = false, errorMessage = "Upload finished") }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(operationInProgress = false, errorMessage = e.message ?: "Upload failed") }
            }
        }
    }

    private fun queryDisplayName(resolver: android.content.ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun delete(entry: RemoteFileEntry) {
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            _state.update { it.copy(operationInProgress = true, operationMessage = "Deleting ${entry.name}…") }
            try {
                withContext(Dispatchers.IO) { client!!.delete(entry.path, entry.isDirectory) }
                _state.update { it.copy(operationInProgress = false, optionsEntry = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(operationInProgress = false, errorMessage = e.message ?: "Delete failed") }
            }
        }
    }

    private fun rename(entry: RemoteFileEntry, newName: String) {
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            _state.update { it.copy(operationInProgress = true, operationMessage = "Renaming…") }
            try {
                withContext(Dispatchers.IO) { client!!.rename(entry.path, newName.trim()) }
                _state.update { it.copy(operationInProgress = false, optionsEntry = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(operationInProgress = false, errorMessage = e.message ?: "Rename failed") }
            }
        }
    }

    private fun mkdir(name: String) {
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            _state.update { it.copy(operationInProgress = true, operationMessage = "Creating folder…") }
            try {
                withContext(Dispatchers.IO) {
                    client!!.mkdir(joinRemotePath(_state.value.path, name.trim()))
                }
                _state.update { it.copy(operationInProgress = false, showMkdirDialog = false) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(operationInProgress = false, errorMessage = e.message ?: "Failed to create folder") }
            }
        }
    }

    private fun joinRemotePath(dir: String, name: String): String =
        "${dir.trimEnd('/')}/$name"

    override fun onCleared() {
        val activeClient = client
        client = null
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                activeClient?.disconnect()
            } catch (e: Exception) {
                // Ignore
            }
        }
        super.onCleared()
    }
}
