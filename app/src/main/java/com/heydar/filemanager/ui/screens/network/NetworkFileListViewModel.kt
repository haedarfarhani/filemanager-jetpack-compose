package com.heydar.filemanager.ui.screens.network

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@HiltViewModel
class NetworkFileListViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkFileListUiState())
    val state: StateFlow<NetworkFileListUiState> = _state.asStateFlow()

    private val _files = MutableStateFlow<List<RemoteFileItem>>(emptyList())
    val files: StateFlow<List<RemoteFileItem>> = _files.asStateFlow()

    private val pathHistory = mutableListOf<String>()

    fun onEvent(event: NetworkFileListEvent) {
        when (event) {
            is NetworkFileListEvent.LoadFiles -> {
                _state.update { it.copy(serverUrl = event.serverUrl) }
                loadFiles(event.path)
            }
            is NetworkFileListEvent.NavigateTo -> navigateTo(event.path)
            is NetworkFileListEvent.DownloadFile -> downloadFile(event.path)
            is NetworkFileListEvent.GoBack -> goBack()
            is NetworkFileListEvent.Refresh -> refresh()
            is NetworkFileListEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadFiles(path: String) {
        _state.update { it.copy(isLoading = true, currentPath = path) }

        viewModelScope.launch {
            try {
                val url = URL("${_state.value.serverUrl}/api/files/$path")
                val connection = withContext(Dispatchers.IO) {
                    url.openConnection() as HttpURLConnection
                }

                val response = withContext(Dispatchers.IO) {
                    connection.inputStream.bufferedReader().readText()
                }

                val items = parseJsonResponse(response)
                _files.value = items
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load files: ${e.message}"
                    )
                }
            }
        }
    }

    private fun navigateTo(path: String) {
        pathHistory.add(_state.value.currentPath)
        loadFiles(path)
    }

    private fun goBack() {
        if (pathHistory.isNotEmpty()) {
            val previousPath = pathHistory.removeLast()
            loadFiles(previousPath)
        }
    }

    private fun refresh() {
        loadFiles(_state.value.currentPath)
    }

    private fun downloadFile(path: String) {
        viewModelScope.launch {
            try {
                val url = URL("${_state.value.serverUrl}/api/download/$path")
                val connection = withContext(Dispatchers.IO) {
                    url.openConnection() as HttpURLConnection
                }

                val fileName = path.substringAfterLast("/")
                val file = File(app.cacheDir, fileName)

                withContext(Dispatchers.IO) {
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val uri = FileProvider.getUriForFile(
                    app,
                    "${app.packageName}.provider",
                    file
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, getMimeType(fileName))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(intent)
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Download failed: ${e.message}")
                }
            }
        }
    }

    private fun parseJsonResponse(json: String): List<RemoteFileItem> {
        val items = mutableListOf<RemoteFileItem>()
        val regex = Regex("""\{[^}]+\}""")

        regex.findAll(json).forEach { match ->
            val obj = match.value
            val name = extractJsonValue(obj, "name")
            val path = extractJsonValue(obj, "path")
            val isDir = obj.contains("\"isDirectory\": true")
            val size = extractJsonLong(obj, "size")

            if (name.isNotEmpty()) {
                items.add(RemoteFileItem(name, path, isDir, size))
            }
        }

        return items.sortedWith(compareBy<RemoteFileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

    private fun extractJsonValue(obj: String, key: String): String {
        val regex = Regex(""""$key":\s*"([^"]*)"""")
        return regex.find(obj)?.groupValues?.get(1) ?: ""
    }

    private fun extractJsonLong(obj: String, key: String): Long {
        val regex = Regex(""""$key":\s*(\d+)""")
        return regex.find(obj)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return mimeTypes[ext] ?: "application/octet-stream"
    }

    private val mimeTypes = mapOf(
        "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png",
        "gif" to "image/gif", "mp4" to "video/mp4", "mkv" to "video/x-matroska",
        "mp3" to "audio/mpeg", "pdf" to "application/pdf", "txt" to "text/plain"
    )
}

data class RemoteFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)
