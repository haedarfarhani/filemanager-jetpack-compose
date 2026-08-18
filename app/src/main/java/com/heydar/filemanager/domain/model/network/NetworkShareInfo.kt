package com.heydar.filemanager.domain.model.network

data class NetworkShareInfo(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val protocol: NetworkProtocol,
    val isOnline: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis()
) {
    val address: String get() = "$ip:$port"
    val url: String get() = "http://$ip:$port"
}
