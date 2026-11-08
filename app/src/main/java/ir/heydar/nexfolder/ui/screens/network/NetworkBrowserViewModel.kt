package ir.heydar.nexfolder.ui.screens.network

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.heydar.nexfolder.core.network.NetworkDiscoveryManager
import ir.heydar.nexfolder.domain.model.network.NetworkProtocol
import ir.heydar.nexfolder.domain.model.network.NetworkShareInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NetworkBrowserViewModel @Inject constructor(
    private val app: Application,
    private val discoveryManager: NetworkDiscoveryManager
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkBrowserUiState())
    val state: StateFlow<NetworkBrowserUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            discoveryManager.discoveredDevices.collect { devices ->
                _state.update { it.copy(discoveredDevices = devices) }
            }
        }
        viewModelScope.launch {
            discoveryManager.isScanning.collect { scanning ->
                _state.update { it.copy(isScanning = scanning) }
            }
        }
    }

    fun onEvent(event: NetworkBrowserEvent) {
        when (event) {
            is NetworkBrowserEvent.StartDiscovery -> startDiscovery()
            is NetworkBrowserEvent.StopDiscovery -> stopDiscovery()
            is NetworkBrowserEvent.Refresh -> {
                stopDiscovery()
                startDiscovery()
            }
            is NetworkBrowserEvent.ConnectToDevice -> {
                // Navigation handled in UI
            }
            is NetworkBrowserEvent.ManualConnect -> {
                val share = NetworkShareInfo(
                    id = "${event.ip}:${event.port}",
                    name = "Manual: ${event.ip}",
                    ip = event.ip,
                    port = event.port,
                    protocol = NetworkProtocol.HTTP
                )
                _state.update { it.copy(showManualConnect = false) }
                // Navigation handled in UI
            }
            is NetworkBrowserEvent.UpdateManualIp -> _state.update { it.copy(manualIp = event.ip) }
            is NetworkBrowserEvent.UpdateManualPort -> _state.update { it.copy(manualPort = event.port) }
            is NetworkBrowserEvent.ShowManualConnect -> _state.update { it.copy(showManualConnect = true) }
            is NetworkBrowserEvent.DismissManualConnect -> _state.update { it.copy(showManualConnect = false) }
            is NetworkBrowserEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun startDiscovery() {
        _state.update { it.copy(isScanning = true) }
        discoveryManager.startDiscovery()
    }

    private fun stopDiscovery() {
        discoveryManager.stopDiscovery()
        _state.update { it.copy(isScanning = false) }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.stopDiscovery()
    }
}
