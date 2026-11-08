package ir.heydar.nexfolder.domain.model.network

import java.util.UUID

/** A saved remote-server connection for FTP / SFTP / SMB / WebDAV browsing. */
data class RemoteConnection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val protocol: NetworkProtocol,
    val host: String,
    val port: Int,
    val username: String = "",
    val password: String = "",
    val anonymous: Boolean = false,
    /** SMB workgroup/domain (optional). */
    val domain: String = ""
) {
    val rootPath: String
        get() = when (protocol) {
            NetworkProtocol.SMB -> "/"
            else -> "/"
        }
}
