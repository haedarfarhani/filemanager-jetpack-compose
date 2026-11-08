package ir.heydar.nexfolder.ui.screens.network

import ir.heydar.nexfolder.domain.model.network.NetworkClientInfo


data class NetworkShareUiState(
    val isRunning: Boolean = false,
    val ipAddress: String? = null,
    val port: Int = 8080,
    val rootPath: String? = null,
    val connectedClients: List<NetworkClientInfo> = emptyList(),
    val passwordEnabled: Boolean = false,
    val password: String = "",
    val error: String? = null,
    val showFolderPicker: Boolean = false
) {
    val serverUrl: String?
        get() = if (isRunning && ipAddress != null) "http://$ipAddress:$port" else null
}
