package com.heydar.filemanager.domain.model.network

data class NetworkServerState(
    val isRunning: Boolean = false,
    val ipAddress: String? = null,
    val port: Int = 8080,
    val rootPath: String? = null,
    val connectedClients: List<NetworkClientInfo> = emptyList(),
    val passwordEnabled: Boolean = false,
    val password: String = "",
    val error: String? = null
) {
    val serverUrl: String?
        get() = if (isRunning && ipAddress != null) "http://$ipAddress:$port" else null

    val clientCount: Int get() = connectedClients.size
}
