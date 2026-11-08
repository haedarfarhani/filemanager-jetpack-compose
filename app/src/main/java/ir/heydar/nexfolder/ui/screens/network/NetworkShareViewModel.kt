package ir.heydar.nexfolder.ui.screens.network

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.heydar.nexfolder.core.network.NetworkPermissionHelper
import ir.heydar.nexfolder.core.network.ServerForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NetworkShareViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkShareUiState())
    val state: StateFlow<NetworkShareUiState> = _state.asStateFlow()

    fun onEvent(event: NetworkShareEvent) {
        when (event) {
            is NetworkShareEvent.StartServer -> startServer()
            is NetworkShareEvent.StopServer -> stopServer()
            is NetworkShareEvent.SetPort -> _state.update { it.copy(port = event.port) }
            is NetworkShareEvent.SetRootPath -> _state.update { it.copy(rootPath = event.path, showFolderPicker = false) }
            is NetworkShareEvent.SetPassword -> _state.update { it.copy(password = event.password) }
            is NetworkShareEvent.TogglePassword -> _state.update { it.copy(passwordEnabled = !it.passwordEnabled) }
            is NetworkShareEvent.DismissError -> _state.update { it.copy(error = null) }
            is NetworkShareEvent.CopyUrl -> copyUrlToClipboard()
        }
    }

    private fun startServer() {
        if (!NetworkPermissionHelper.hasStoragePermission(app)) {
            _state.update { it.copy(error = "Storage permission required") }
            return
        }

        if (!NetworkPermissionHelper.isWifiConnected(app)) {
            _state.update { it.copy(error = "Wi-Fi connection required") }
            return
        }

        val intent = Intent(app, ServerForegroundService::class.java).apply {
            action = ServerForegroundService.ACTION_START
            putExtra(ServerForegroundService.EXTRA_PORT, _state.value.port)
            putExtra(ServerForegroundService.EXTRA_ROOT_PATH, _state.value.rootPath)
            if (_state.value.passwordEnabled && _state.value.password.isNotEmpty()) {
                putExtra(ServerForegroundService.EXTRA_PASSWORD, _state.value.password)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }

        _state.update {
            it.copy(
                isRunning = true,
                ipAddress = ServerForegroundService.getLocalIpAddress()
            )
        }
    }

    private fun stopServer() {
        val intent = Intent(app, ServerForegroundService::class.java).apply {
            action = ServerForegroundService.ACTION_STOP
        }
        app.startService(intent)
        _state.update { NetworkShareUiState() }
    }

    private fun copyUrlToClipboard() {
        val url = _state.value.serverUrl ?: return
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Server URL", url)
        clipboard.setPrimaryClip(clip)
    }
}
