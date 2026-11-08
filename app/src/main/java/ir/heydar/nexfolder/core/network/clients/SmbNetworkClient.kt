package ir.heydar.nexfolder.core.network.clients

import ir.heydar.nexfolder.domain.model.network.RemoteConnection
import ir.heydar.nexfolder.domain.model.network.RemoteFileEntry
import jcifs.CIFSContext
import jcifs.CIFSException
import jcifs.config.BaseConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * SMB2/3 client built on jcifs-ng (eu.agno3.jcifs packaging, `jcifs.*` namespace).
 * Remote paths use the app-wide convention "/share/folder/file" which maps to
 * smb://host/share/folder/file.
 */
class SmbNetworkClient(private val connection: RemoteConnection) : NetworkClient {

    private val context: CIFSContext

    init {
        context = try {
            val base = BaseContext(BaseConfiguration(false))
            val authenticator = if (connection.anonymous || connection.username.isBlank()) {
                NtlmPasswordAuthenticator()
            } else {
                NtlmPasswordAuthenticator(connection.domain, connection.username, connection.password)
            }
            base.withCredentials(authenticator)
        } catch (e: CIFSException) {
            throw NetworkClientException("Cannot initialize SMB client", e)
        }
    }

    private fun smbUrl(path: String, trailingSlash: Boolean = false): String {
        val cleaned = path.trim('/')
        var url = "smb://${connection.host}:${connection.port}/${cleaned}"
        if (trailingSlash && !url.endsWith("/")) url += "/"
        if (!trailingSlash && url.endsWith("/")) url = url.trimEnd('/')
        return url
    }

    private fun joinPath(dir: String, name: String): String =
        "${dir.trimEnd('/')}/$name"

    override suspend fun connect() = withContext(Dispatchers.IO) {
        // jcifs-ng connects lazily; verify reachability against the server root
        try {
            SmbFile(smbUrl("/", trailingSlash = true), context).exists()
        } catch (e: Exception) {
            throw NetworkClientException("Cannot connect to ${connection.host}:${connection.port}", e)
        }
        Unit
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            context.close()
        } catch (e: Exception) {
            // Best effort
        }
        Unit
    }

    override suspend fun list(path: String): List<RemoteFileEntry> = withContext(Dispatchers.IO) {
        try {
            val dir = SmbFile(smbUrl(path, trailingSlash = true), context)
            if (!dir.exists()) throw NetworkClientException("Folder not found")
            val children: Array<SmbFile> = dir.listFiles()
            children.map { child ->
                RemoteFileEntry(
                    name = child.name.trimEnd('/'),
                    path = joinPath(path, child.name.trimEnd('/')),
                    isDirectory = child.isDirectory,
                    size = child.length(),
                    lastModified = child.lastModified()
                )
            }
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
            val remote = SmbFile(smbUrl(remotePath), context)
            val total = remote.length()
            remote.getInputStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var processed = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } > 0) {
                        output.write(buffer, 0, read)
                        processed += read
                        onProgress(processed, total)
                    }
                }
            }
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
            val remote = SmbFile(smbUrl(remotePath), context)
            val total = localFile.length()
            remote.getOutputStream().use { output ->
                FileInputStream(localFile).use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var processed = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } > 0) {
                        output.write(buffer, 0, read)
                        processed += read
                        onProgress(processed, total)
                    }
                }
            }
        } catch (e: Exception) {
            throw NetworkClientException("Upload failed", e)
        }
    }

    override suspend fun mkdir(path: String) = withContext(Dispatchers.IO) {
        try {
            SmbFile(smbUrl(path, trailingSlash = true), context).mkdir()
        } catch (e: Exception) {
            throw NetworkClientException("Cannot create folder", e)
        }
    }

    override suspend fun delete(path: String, isDirectory: Boolean) = withContext(Dispatchers.IO) {
        try {
            val file = SmbFile(smbUrl(path, trailingSlash = isDirectory), context)
            if (isDirectory) {
                deleteRecursively(file)
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            throw NetworkClientException("Cannot delete $path", e)
        }
    }

    /** jcifs-ng SmbFile.delete() does not recurse; walk children bottom-up. */
    private suspend fun deleteRecursively(dir: SmbFile): Unit = withContext(Dispatchers.IO) {
        val children: Array<SmbFile> = dir.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                deleteRecursively(child)
            } else {
                child.delete()
            }
        }
        dir.delete()
    }

    override suspend fun rename(path: String, newName: String) = withContext(Dispatchers.IO) {
        try {
            val source = SmbFile(smbUrl(path), context)
            val target = SmbFile(smbUrl(joinPath(path.substringBeforeLast('/'), newName)), context)
            source.renameTo(target)
        } catch (e: Exception) {
            throw NetworkClientException("Cannot rename", e)
        }
    }
}
