package ir.heydar.nexfolder.core.network.clients

import ir.heydar.nexfolder.domain.model.network.RemoteConnection
import ir.heydar.nexfolder.domain.model.network.RemoteFileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream

/** FTP client built on Apache commons-net (passive mode, binary transfers). */
class FtpNetworkClient(private val connection: RemoteConnection) : NetworkClient {

    private val client = FTPClient()

    override suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            client.connectTimeout = 15_000
            client.connect(connection.host, connection.port)
            val ok = if (connection.anonymous || connection.username.isBlank()) {
                client.login("anonymous", "")
            } else {
                client.login(connection.username, connection.password)
            }
            if (!ok) throw NetworkClientException("FTP login failed")
            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)
            client.setControlEncoding("UTF-8")
        } catch (e: NetworkClientException) {
            throw e
        } catch (e: Exception) {
            throw NetworkClientException("Cannot connect to ${connection.host}:${connection.port}", e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            client.logout()
            client.disconnect()
        } catch (e: Exception) {
            // Best effort
        }
    }

    override suspend fun list(path: String): List<RemoteFileEntry> = withContext(Dispatchers.IO) {
        try {
            val dir = path.ifEmpty { "/" }
            val files: Array<FTPFile> = client.listFiles(dir)
            files.map { ftpFile ->
                RemoteFileEntry(
                    name = ftpFile.name,
                    path = joinPath(dir, ftpFile.name),
                    isDirectory = ftpFile.isDirectory,
                    size = ftpFile.size,
                    lastModified = ftpFile.timestamp?.timeInMillis ?: 0L
                )
            }.filter { it.name != "." && it.name != ".." }
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
            // FTPClient.getSize() has changed signature across commons-net versions;
            // derive the size from LIST instead, which is stable everywhere.
            val size = try {
                client.listFiles(remotePath).firstOrNull()?.size ?: -1L
            } catch (e: Exception) {
                -1L
            }
            client.retrieveFileStream(remotePath)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var processed = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } > 0) {
                        output.write(buffer, 0, read)
                        processed += read
                        onProgress(processed, size)
                    }
                }
            } ?: throw NetworkClientException("Server refused the download")
            if (!client.completePendingCommand()) {
                targetFile.delete()
                throw NetworkClientException("Download interrupted")
            }
        } catch (e: NetworkClientException) {
            throw e
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
            client.storeFileStream(remotePath)?.use { output ->
                FileInputStream(localFile).use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var processed = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } > 0) {
                        output.write(buffer, 0, read)
                        processed += read
                        onProgress(processed, localFile.length())
                    }
                }
            } ?: throw NetworkClientException("Server refused the upload")
            if (!client.completePendingCommand()) {
                throw NetworkClientException("Upload interrupted")
            }
        } catch (e: NetworkClientException) {
            throw e
        } catch (e: Exception) {
            throw NetworkClientException("Upload failed", e)
        }
    }

    override suspend fun mkdir(path: String) = withContext(Dispatchers.IO) {
        if (!client.makeDirectory(path)) {
            throw NetworkClientException("Cannot create folder")
        }
    }

    override suspend fun delete(path: String, isDirectory: Boolean) = withContext(Dispatchers.IO) {
        val ok = if (isDirectory) client.removeDirectory(path) else client.deleteFile(path)
        if (!ok) throw NetworkClientException("Cannot delete $path")
    }

    override suspend fun rename(path: String, newName: String) = withContext(Dispatchers.IO) {
        if (!client.rename(path, joinPath(path.substringBeforeLast('/'), newName))) {
            throw NetworkClientException("Cannot rename")
        }
    }

    private fun joinPath(dir: String, name: String): String =
        "${dir.trimEnd('/')}/$name"
}
