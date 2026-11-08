package ir.heydar.nexfolder.core.network.clients

import android.util.Base64
import android.util.Xml
import ir.heydar.nexfolder.domain.model.network.RemoteConnection
import ir.heydar.nexfolder.domain.model.network.RemoteFileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Dependency-free WebDAV client over HttpURLConnection:
 * PROPFIND (list), GET (download), PUT (upload), MKCOL (mkdir),
 * DELETE and MOVE (rename). Works over plain HTTP and HTTPS.
 */
class WebDavNetworkClient(private val connection: RemoteConnection) : NetworkClient {

    private val dateParser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)

    private fun baseUrl(): String = "http://${connection.host}:${connection.port}"

    private fun url(path: String): String {
        val encoded = path.trim('/').split('/')
            .filter { it.isNotEmpty() }
            .joinToString("/") { segment ->
                URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
            }
        return "${baseUrl()}/${encoded}"
    }

    private fun joinPath(dir: String, name: String): String =
        "${dir.trimEnd('/')}/$name"

    private fun authHeader(): String? {
        if (connection.anonymous || connection.username.isBlank()) return null
        val token = Base64.encodeToString(
            "${connection.username}:${connection.password}".toByteArray(),
            Base64.NO_WRAP
        )
        return "Basic $token"
    }

    private fun open(
        method: String,
        path: String,
        doOutput: Boolean = false
    ): HttpURLConnection {
        val conn = URL(url(path)).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        authHeader()?.let { conn.setRequestProperty("Authorization", it) }
        if (doOutput) {
            conn.doOutput = true
        }
        return conn
    }

    private fun fail(method: String, code: Int, path: String): NetworkClientException =
        NetworkClientException("$method $path failed (HTTP $code)")

    override suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            val conn = open("PROPFIND", "/", doOutput = true)
            conn.setRequestProperty("Depth", "0")
            conn.setRequestProperty("Content-Type", "application/xml")
            conn.outputStream.use { it.write(PROPFIND_BODY.toByteArray()) }
            val code = conn.responseCode
            if (code != 207 && code != 200) throw fail("PROPFIND", code, "/")
        } catch (e: NetworkClientException) {
            throw e
        } catch (e: Exception) {
            throw NetworkClientException("Cannot connect to ${connection.host}:${connection.port}", e)
        }
    }

    override suspend fun disconnect() {
        // Stateless HTTP — nothing to release
    }

    override suspend fun list(path: String): List<RemoteFileEntry> = withContext(Dispatchers.IO) {
        val conn = open("PROPFIND", path, doOutput = true)
        conn.setRequestProperty("Depth", "1")
        conn.setRequestProperty("Content-Type", "application/xml")
        conn.outputStream.use { it.write(PROPFIND_BODY.toByteArray()) }
        val code = conn.responseCode
        if (code != 207) throw fail("PROPFIND", code, path)

        val selfHref = url(path).trimEnd('/')
        val entries = mutableListOf<RemoteFileEntry>()
        conn.inputStream.use { input ->
            val parser = Xml.newPullParser()
            parser.setInput(input, null)
            var currentHref: String? = null
            var displayName: String? = null
            var isCollection = false
            var contentLength: Long = 0
            var lastModified: Long = 0
            var inProp = false

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                        "prop" -> inProp = true
                        "response" -> {
                            currentHref = null
                            displayName = null
                            isCollection = false
                            contentLength = 0
                            lastModified = 0
                        }
                        "href" -> currentHref = parser.nextText()
                        "displayname" -> if (inProp) displayName = parser.nextText()
                        "getcontentlength" -> if (inProp) {
                            contentLength = parser.nextText()?.toLongOrNull() ?: 0
                        }
                        "getlastmodified" -> if (inProp) {
                            lastModified = try {
                                dateParser.parse(parser.nextText() ?: "")?.time ?: 0
                            } catch (e: Exception) {
                                0
                            }
                        }
                        "collection" -> isCollection = true
                    }
                    XmlPullParser.END_TAG -> when (parser.name.lowercase()) {
                        "prop" -> inProp = false
                        "response" -> {
                            val href = currentHref
                            if (href != null) {
                                val decoded = try {
                                    java.net.URLDecoder.decode(href, "UTF-8")
                                } catch (e: Exception) {
                                    href
                                }
                                val directory = isCollection || decoded.trimEnd('/') != decoded
                                val name = decoded.trimEnd('/').substringAfterLast('/')
                                // Skip the collection itself (first response with Depth 1)
                                if (name.isNotEmpty() && decoded.trimEnd('/') != selfHref) {
                                    entries += RemoteFileEntry(
                                        name = name,
                                        path = joinPath(path, name),
                                        isDirectory = directory,
                                        size = contentLength,
                                        lastModified = lastModified
                                    )
                                }
                            }
                        }
                    }
                }
                event = parser.next()
            }
        }
        entries
    }

    override suspend fun download(
        remotePath: String,
        targetFile: File,
        onProgress: suspend (Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val conn = open("GET", remotePath)
        val code = conn.responseCode
        if (code !in 200..299) throw fail("GET", code, remotePath)
        val total = conn.contentLengthLong
        try {
            conn.inputStream.use { input ->
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
        val conn = open("PUT", remotePath, doOutput = true)
        conn.setFixedLengthStreamingMode(localFile.length())
        try {
            conn.outputStream.use { output ->
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
            }
            val code = conn.responseCode
            if (code !in 200..299) throw fail("PUT", code, remotePath)
        } catch (e: NetworkClientException) {
            throw e
        } catch (e: Exception) {
            throw NetworkClientException("Upload failed", e)
        }
    }

    override suspend fun mkdir(path: String) = withContext(Dispatchers.IO) {
        val conn = open("MKCOL", path)
        val code = conn.responseCode
        if (code !in 200..299) throw fail("MKCOL", code, path)
    }

    override suspend fun delete(path: String, isDirectory: Boolean) = withContext(Dispatchers.IO) {
        val conn = open("DELETE", path)
        val code = conn.responseCode
        if (code !in 200..299) throw fail("DELETE", code, path)
    }

    override suspend fun rename(path: String, newName: String) = withContext(Dispatchers.IO) {
        val conn = open("MOVE", path)
        conn.setRequestProperty("Overwrite", "F")
        conn.setRequestProperty("Destination", url(joinPath(path.substringBeforeLast('/'), newName)))
        val code = conn.responseCode
        if (code !in 200..299) throw fail("MOVE", code, path)
    }

    private companion object {
        val PROPFIND_BODY = """
            |<?xml version="1.0" encoding="utf-8"?>
            |<D:propfind xmlns:D="DAV:">
            |  <D:prop>
            |    <D:displayname/>
            |    <D:getcontentlength/>
            |    <D:getlastmodified/>
            |    <D:resourcetype/>
            |  </D:prop>
            |</D:propfind>
        """.trimMargin()
    }
}
