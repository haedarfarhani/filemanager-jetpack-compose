package ir.heydar.nexfolder.ui.screens.network

import ir.heydar.nexfolder.domain.model.network.NetworkShareInfo

sealed interface NetworkBrowserEvent {
    data object StartDiscovery : NetworkBrowserEvent
    data object StopDiscovery : NetworkBrowserEvent
    data object Refresh : NetworkBrowserEvent
    data class ConnectToDevice(val share: NetworkShareInfo) : NetworkBrowserEvent
    data class ManualConnect(val ip: String, val port: Int) : NetworkBrowserEvent
    data class UpdateManualIp(val ip: String) : NetworkBrowserEvent
    data class UpdateManualPort(val port: String) : NetworkBrowserEvent
    data object ShowManualConnect : NetworkBrowserEvent
    data object DismissManualConnect : NetworkBrowserEvent
    data object DismissError : NetworkBrowserEvent
}
