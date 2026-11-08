package ir.heydar.nexfolder.core.network

import android.content.Context
import ir.heydar.nexfolder.domain.model.network.NetworkClientInfo
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class LocalHttpServer(
    private val context: Context,
    port: Int,
    private var rootPath: String?,
    private val password: String? = null
) : NanoHTTPD(port) {

    private val clients = ConcurrentHashMap<String, NetworkClientInfo>()
    private var clientListener: ClientListener? = null
    private val htmlContent: String by lazy { loadHtmlAsset() }

    interface ClientListener {
        fun onClientConnected(client: NetworkClientInfo)
        fun onClientDisconnected(ip: String)
    }

    fun setClientListener(listener: ClientListener) {
        this.clientListener = listener
    }

    fun updateRootPath(newPath: String) {
        rootPath = newPath
    }

    fun updatePassword(newPassword: String?) {
        // Password is updated on next start
    }

    fun getConnectedClients(): List<NetworkClientInfo> = clients.values.toList()

    override fun serve(session: IHTTPSession): Response {
        val clientIp = session.remoteIpAddress
        val userAgent = session.headers["user-agent"] ?: "Unknown"
        val uri = try {
            URLDecoder.decode(session.uri, "UTF-8")
        } catch (e: Exception) {
            session.uri
        }

        val client = NetworkClientInfo(
            ip = clientIp,
            userAgent = userAgent,
            requestedPath = uri
        )
        clients[clientIp] = client
        clientListener?.onClientConnected(client)

        if (password != null && password.isNotEmpty()) {
            val auth = session.headers["authorization"]
            if (auth == null || !checkAuth(auth)) {
                val response = newFixedLengthResponse(
                    Response.Status.UNAUTHORIZED,
                    "text/plain",
                    "Unauthorized"
                )
                response.addHeader("WWW-Authenticate", "Basic realm=\"NexFolder\"")
                return response
            }
        }

        return when {
            uri == "/" || uri.isEmpty() -> serveApp()
            uri == "/api/files" -> serveFileList(session)
            uri.startsWith("/api/files/") -> serveFileList(session)
            uri == "/api/upload" && session.method == Method.POST -> handleUpload(session)
            uri == "/api/mkdir" && session.method == Method.POST -> handleMkdir(session)
            uri == "/api/delete" && session.method == Method.POST -> handleDelete(session)
            uri == "/api/rename" && session.method == Method.POST -> handleRename(session)
            uri == "/api/search" -> serveSearch(session)
            uri == "/api/info" -> serveInfo()
            uri.startsWith("/api/download/") -> serveFileDownload(uri, session)
            else -> serveApp()
        }
    }

    private fun checkAuth(authHeader: String): Boolean {
        return try {
            val decoded = String(
                android.util.Base64.decode(
                    authHeader.removePrefix("Basic "),
                    android.util.Base64.DEFAULT
                )
            )
            val parts = decoded.split(":")
            parts.size == 2 && parts[1] == password
        } catch (e: Exception) {
            false
        }
    }

    private fun serveApp(): Response {
        return newFixedLengthResponse(Response.Status.OK, "text/html", htmlContent)
    }

    private fun loadHtmlAsset(): String {
        return try {
            context.assets.open("webserver/index.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "<html><body><h1>Failed to load UI</h1></body></html>"
        }
    }

    private fun serveFileList(session: IHTTPSession): Response {
        val params = session.parms
        val path = params["path"]?.let {
            try { URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
        } ?: ""
        val json = buildFileListJson(path.ifEmpty { null })
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun serveFileDownload(uri: String, session: IHTTPSession): Response {
        val filePath = uri.removePrefix("/api/download/").removePrefix("/")
        val decodedPath = try { URLDecoder.decode(filePath, "UTF-8") } catch (e: Exception) { filePath }
        val file = getFile(decodedPath) ?: return newFixedLengthResponse(
            Response.Status.NOT_FOUND, "text/plain", "File not found"
        )
        if (!file.exists()) return newFixedLengthResponse(
            Response.Status.NOT_FOUND, "text/plain", "File not found"
        )
        if (file.isDirectory) return newFixedLengthResponse(
            Response.Status.BAD_REQUEST, "text/plain", "Cannot download directory"
        )

        val mimeType = getMimeType(file.name)
        val response = newFixedLengthResponse(Response.Status.OK, mimeType, FileInputStream(file), file.length())
        response.addHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
        return response
    }

    private fun serveInfo(): Response {
        val root = rootPath ?: return newFixedLengthResponse(
            Response.Status.INTERNAL_ERROR, "text/plain", "No root path"
        )
        val rootDir = File(root)
        val totalSpace = rootDir.totalSpace
        val freeSpace = rootDir.freeSpace
        val json = """
            {
                "name": "NexFolder Server",
                "version": "2.0",
                "rootPath": ${rootPath?.let { "\"$it\"" } ?: "null"},
                "totalSpace": $totalSpace,
                "freeSpace": $freeSpace
            }
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun serveSearch(session: IHTTPSession): Response {
        val query = session.parms["q"]?.lowercase() ?: return newFixedLengthResponse(
            Response.Status.BAD_REQUEST, "text/plain", "Missing query"
        )
        val path = session.parms["path"]?.let {
            try { URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
        } ?: ""

        val root = rootPath ?: return newFixedLengthResponse(
            Response.Status.INTERNAL_ERROR, "text/plain", "No root path"
        )

        val searchDir = if (path.isEmpty()) File(root) else File(root, path)
        if (!searchDir.exists() || !searchDir.isDirectory) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Directory not found")
        }

        val results = mutableListOf<File>()
        searchDir.walkBottomUp().maxDepth(5).forEach { file ->
            if (file.name.lowercase().contains(query) && results.size < 50) {
                results.add(file)
            }
        }

        val items = results.map { file ->
            val isDir = file.isDirectory
            val relativePath = file.path.removePrefix(root).replace('\\', '/').removePrefix("/")
            val encodedPath = URLEncoder.encode(relativePath, "UTF-8").replace("+", "%20")
            """
                {
                    "name": "${file.name}",
                    "path": "$encodedPath",
                    "isDirectory": $isDir,
                    "size": ${if (isDir) 0 else file.length()},
                    "lastModified": ${file.lastModified()}
                }
            """.trimIndent()
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", "[${items.joinToString(",")}]")
    }

    private fun handleMkdir(session: IHTTPSession): Response {
        return try {
            val body = readBody(session)
            val json = parseJson(body)
            val path = json["path"]?.toString() ?: ""
            val name = json["name"]?.toString() ?: ""

            if (name.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", """{"error":"Name required"}""")
            }

            val root = rootPath ?: return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "application/json", """{"error":"No root path"}"""
            )

            val parentDir = if (path.isEmpty()) File(root) else File(root, path)
            val newDir = File(parentDir, name)

            if (newDir.exists()) {
                return newFixedLengthResponse(Response.Status.CONFLICT, "application/json", """{"error":"Already exists"}""")
            }

            if (newDir.mkdirs()) {
                newFixedLengthResponse(Response.Status.OK, "application/json", """{"success":true}""")
            } else {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", """{"error":"Failed to create"}""")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", """{"error":"${e.message}"}""")
        }
    }

    private fun handleDelete(session: IHTTPSession): Response {
        return try {
            val body = readBody(session)
            val json = parseJson(body)
            val path = json["path"]?.toString() ?: ""

            if (path.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", """{"error":"Path required"}""")
            }

            val file = getFile(path) ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "application/json", """{"error":"File not found"}"""
            )

            if (!file.exists()) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", """{"error":"File not found"}""")
            }

            if (file.deleteRecursively()) {
                newFixedLengthResponse(Response.Status.OK, "application/json", """{"success":true}""")
            } else {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", """{"error":"Failed to delete"}""")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", """{"error":"${e.message}"}""")
        }
    }

    private fun handleRename(session: IHTTPSession): Response {
        return try {
            val body = readBody(session)
            val json = parseJson(body)
            val path = json["path"]?.toString() ?: ""
            val newName = json["newName"]?.toString() ?: ""

            if (path.isEmpty() || newName.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", """{"error":"Path and newName required"}""")
            }

            val file = getFile(path) ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "application/json", """{"error":"File not found"}"""
            )

            if (!file.exists()) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", """{"error":"File not found"}""")
            }

            val newFile = File(file.parent, newName)
            if (newFile.exists()) {
                return newFixedLengthResponse(Response.Status.CONFLICT, "application/json", """{"error":"Name already exists"}""")
            }

            if (file.renameTo(newFile)) {
                newFixedLengthResponse(Response.Status.OK, "application/json", """{"success":true}""")
            } else {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", """{"error":"Failed to rename"}""")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", """{"error":"${e.message}"}""")
        }
    }

    private fun handleUpload(session: IHTTPSession): Response {
        return try {
            val root = rootPath ?: return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "application/json", """{"error":"No root path"}"""
            )

            val contentType = session.headers["content-type"] ?: ""
            if (!contentType.contains("multipart/form-data")) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", """{"error":"Expected multipart"}""")
            }

            val uploadDir = session.parms["path"]?.let {
                try { URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
            } ?: ""

            val targetDir = if (uploadDir.isEmpty()) File(root) else File(root, uploadDir)
            if (!targetDir.exists()) targetDir.mkdirs()

            val tempFiles = mutableMapOf<String, String>()
            session.parseBody(tempFiles)

            if (tempFiles.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", """{"error":"No file uploaded"}""")
            }

            var uploadedCount = 0
            tempFiles.forEach { (fieldName, filePath) ->
                val tmpFile = File(filePath)
                if (tmpFile.exists()) {
                    val destFile = File(targetDir, fieldName)
                    tmpFile.copyTo(destFile, overwrite = true)
                    tmpFile.delete()
                    uploadedCount++
                }
            }

            newFixedLengthResponse(Response.Status.OK, "application/json", """{"success":true,"count":$uploadedCount}""")
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", """{"error":"${e.message}"}""")
        }
    }

    private fun readBody(session: IHTTPSession): String {
        val length = session.headers["content-length"]?.toIntOrNull() ?: 0
        if (length <= 0) return ""
        val buffer = ByteArray(length)
        session.inputStream.read(buffer)
        return String(buffer)
    }

    private fun parseJson(json: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val regex = Regex(""""(\w+)"\s*:\s*"([^"]*)"""")
        regex.findAll(json).forEach { match ->
            result[match.groupValues[1]] = match.groupValues[2]
        }
        return result
    }

    private fun getFile(path: String): File? {
        val root = rootPath ?: return null
        val normalized = path.removePrefix("/")
        if (normalized.contains("..")) return null
        return if (normalized.isEmpty()) File(root) else File(root, normalized)
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return mimeTypes[ext] ?: "application/octet-stream"
    }

    private fun buildFileListJson(dirPath: String?): String {
        val root = rootPath ?: return "[]"
        val targetDir = if (dirPath.isNullOrEmpty()) File(root) else File(root, dirPath)
        if (!targetDir.exists() || !targetDir.isDirectory) return "[]"

        val children = targetDir.listFiles() ?: emptyArray()
        val items = children.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() }).map { child ->
            val isDir = child.isDirectory
            val relativePath = child.path.removePrefix(root).replace('\\', '/').removePrefix("/")
            val encodedPath = URLEncoder.encode(relativePath, "UTF-8").replace("+", "%20")
            val lastModified = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(child.lastModified()))
            """
                {
                    "name": "${child.name.replace("\"", "\\\"")}",
                    "path": "$encodedPath",
                    "isDirectory": $isDir,
                    "size": ${if (isDir) 0 else child.length()},
                    "lastModified": "$lastModified",
                    "extension": "${if (isDir) "" else child.extension.lowercase()}"
                }
            """.trimIndent()
        }
        return "[${items.joinToString(",")}]"
    }

    private val mimeTypes = mapOf(
        "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png",
        "gif" to "image/gif", "bmp" to "image/bmp", "webp" to "image/webp",
        "mp4" to "video/mp4", "mkv" to "video/x-matroska", "avi" to "video/x-msvideo",
        "mov" to "video/quicktime", "webm" to "video/webm", "3gp" to "video/3gpp",
        "mp3" to "audio/mpeg", "wav" to "audio/wav", "flac" to "audio/flac",
        "aac" to "audio/aac", "ogg" to "audio/ogg", "m4a" to "audio/mp4",
        "pdf" to "application/pdf", "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "txt" to "text/plain", "html" to "text/html", "json" to "application/json",
        "xml" to "text/xml", "csv" to "text/csv",
        "zip" to "application/zip", "rar" to "application/vnd.rar",
        "7z" to "application/x-7z-compressed", "tar" to "application/x-tar",
        "gz" to "application/gzip", "apk" to "application/vnd.android.package-archive"
    )

    companion object {
        private const val TAG = "LocalHttpServer"
    }
}
