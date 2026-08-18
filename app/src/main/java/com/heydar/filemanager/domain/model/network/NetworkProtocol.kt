package com.heydar.filemanager.domain.model.network

enum class NetworkProtocol(val displayName: String) {
    HTTP("HTTP"),
    WEBDAV("WebDAV"),
    SMB("SMB/CIFS");

    companion object {
        fun fromPort(port: Int): NetworkProtocol = when (port) {
            445, 139 -> SMB
            80, 8080, 8000, 3000 -> HTTP
            5005, 8081 -> WEBDAV
            else -> HTTP
        }
    }
}
