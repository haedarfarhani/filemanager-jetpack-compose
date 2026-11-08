package ir.heydar.nexfolder.domain.model.network

enum class NetworkProtocol(val displayName: String, val defaultPort: Int) {
    HTTP("HTTP", 8080),
    WEBDAV("WebDAV", 5005),
    SMB("SMB/CIFS", 445),
    FTP("FTP", 21),
    SFTP("SFTP", 22);

    companion object {
        fun fromPort(port: Int): NetworkProtocol = when (port) {
            445, 139 -> SMB
            21 -> FTP
            22 -> SFTP
            80, 8080, 8000, 3000 -> HTTP
            5005, 8081 -> WEBDAV
            else -> HTTP
        }
    }
}
