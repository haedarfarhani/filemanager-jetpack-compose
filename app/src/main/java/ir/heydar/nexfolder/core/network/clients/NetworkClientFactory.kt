package ir.heydar.nexfolder.core.network.clients

import ir.heydar.nexfolder.domain.model.network.NetworkProtocol
import ir.heydar.nexfolder.domain.model.network.RemoteConnection

/** Builds the right [NetworkClient] for a saved connection. */
object NetworkClientFactory {

    fun create(connection: RemoteConnection): NetworkClient = when (connection.protocol) {
        NetworkProtocol.FTP -> FtpNetworkClient(connection)
        NetworkProtocol.SFTP -> SftpNetworkClient(connection)
        NetworkProtocol.SMB -> SmbNetworkClient(connection)
        NetworkProtocol.WEBDAV -> WebDavNetworkClient(connection)
        NetworkProtocol.HTTP -> throw NetworkClientException(
            "Use the built-in 'Share my files' screens for HTTP transfers"
        )
    }
}
