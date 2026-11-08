package ir.heydar.nexfolder.core.network.clients

import ir.heydar.nexfolder.domain.model.network.RemoteConnection
import ir.heydar.nexfolder.domain.model.network.RemoteFileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.FileSystemFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * SFTP client built on sshj. Host-key verification is promiscuous (accept all)
 * because there is no interactive host-key dialog in this version.
 */
class SftpNetworkClient(private val connection: RemoteConnection) : NetworkClient {

    private val ssh = SSHClient()

    private var sftp: SFTPClient? = null

    override suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connectTimeout = 15_000
            ssh.connect(connection.host, connection.port)
            if (connection.anonymous || connection.username.isBlank()) {
                throw NetworkClientException("SFTP requires a username")
            }
            ssh.authPassword(connection.username, connection.password)
            sftp = ssh.newSFTPClient()
        } catch (e: NetworkClientException) {
            throw e
        } catch (e: Exception) {
            throw NetworkClientException("Cannot connect via SFTP to ${connection.host}:${connection.port}", e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            sftp?.close()
            ssh.disconnect()
        } catch (e: Exception) {
            // Best effort
        }
    }

    override suspend fun list(path: String): List<RemoteFileEntry> = withContext(Dispatchers.IO) {
        try {
            val dir = path.ifEmpty { "." }
            val entries: List<RemoteResourceInfo> = sftp!!.ls(dir)
            entries.map { info ->
                RemoteFileEntry(
                    name = info.name,
                    path = info.path,
                    isDirectory = info.isDirectory,
                    size = info.attributes.size,
                    lastModified = info.attributes.mtime * 1000L
                )
            }.filter { it.name != "." && it.name != ".." }
        } catch (e: NetworkClientException) {
            throw e
        } catch (e: Exception) {
            throw NetworkClientException("Cannot list $path", e)
        }
    }

    override suspend fun download(
        remotePath: String,
        targetFile: File,
        onProgress: suspend (Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            sftp!!.get(remotePath, FileSystemFile(targetFile))
        } catch (e: Exception) {
            targetFile.delete()
            throw NetworkClientException("Download failed", e)
        }
    }

    override suspend fun upload(
        localFile: File,
        remotePath: String,
        onProgress: suspend (Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            sftp!!.put(FileSystemFile(localFile), remotePath)
        } catch (e: Exception) {
            throw NetworkClientException("Upload failed", e)
        }
    }

    override suspend fun mkdir(path: String) = withContext(Dispatchers.IO) {
        try {
            sftp!!.mkdir(path)
        } catch (e: Exception) {
            throw NetworkClientException("Cannot create folder", e)
        }
    }

    override suspend fun delete(path: String, isDirectory: Boolean) = withContext(Dispatchers.IO) {
        try {
            sftp!!.rm(path)
        } catch (e: Exception) {
            throw NetworkClientException("Cannot delete $path", e)
        }
    }

    override suspend fun rename(path: String, newName: String) = withContext(Dispatchers.IO) {
        try {
            sftp!!.rename(path, joinPath(path.substringBeforeLast('/'), newName))
        } catch (e: Exception) {
            throw NetworkClientException("Cannot rename", e)
        }
    }

    private fun joinPath(dir: String, name: String): String =
        "${dir.trimEnd('/')}/$name"
}
