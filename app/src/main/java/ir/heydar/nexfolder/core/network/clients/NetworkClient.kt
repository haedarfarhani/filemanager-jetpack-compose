package ir.heydar.nexfolder.core.network.clients

import ir.heydar.nexfolder.domain.model.network.RemoteFileEntry
import java.io.File

/** Error raised for any remote-operation failure with a user-presentable message. */
class NetworkClientException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Uniform file operations over remote protocols. Implementations open a fresh
 * connection per session and are expected to be closed after use.
 */
interface NetworkClient {

    suspend fun connect()

    suspend fun disconnect()

    suspend fun list(path: String): List<RemoteFileEntry>

    suspend fun download(
        remotePath: String,
        targetFile: File,
        onProgress: suspend (processedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    )

    suspend fun upload(
        localFile: File,
        remotePath: String,
        onProgress: suspend (processedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    )

    suspend fun mkdir(path: String)

    suspend fun delete(path: String, isDirectory: Boolean)

    suspend fun rename(path: String, newName: String)
}
