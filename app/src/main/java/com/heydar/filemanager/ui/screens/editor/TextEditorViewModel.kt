package com.heydar.filemanager.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

@HiltViewModel
class TextEditorViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(TextEditorUiState())
    val state: StateFlow<TextEditorUiState> = _state.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    private val maxFileSize = 5 * 1024 * 1024 // 5 MB

    fun onEvent(event: TextEditorEvent) {
        when (event) {
            is TextEditorEvent.LoadFile -> loadFile(event.path)
            is TextEditorEvent.ContentChanged -> updateContent(event.newContent)
            TextEditorEvent.SaveClicked -> saveFile()
            TextEditorEvent.BackClicked -> handleBackClick()
            TextEditorEvent.DiscardChanges -> discardChanges()
            TextEditorEvent.CancelDialog -> {
                _state.update { it.copy(showUnsavedDialog = false) }
            }
        }
    }

    private fun loadFile(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, filePath = path) }
            try {
                val file = File(path)
                if (!file.exists()) {
                    _state.update { it.copy(isLoading = false, error = "File not found") }
                    return@launch
                }
                
                if (file.length() > maxFileSize) {
                    _state.update { it.copy(isLoading = false, error = "File is too large to open in internal editor (Max 5MB)") }
                    return@launch
                }
                
                val fileName = file.name
                val content = withContext(Dispatchers.IO) {
                    file.readText(Charset.forName("UTF-8"))
                }
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        fileName = fileName,
                        content = content,
                        initialContent = content,
                        hasUnsavedChanges = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error reading file: ${e.message}") }
            }
        }
    }

    private fun updateContent(newContent: String) {
        val hasChanges = newContent != _state.value.initialContent
        _state.update { 
            it.copy(
                content = newContent,
                hasUnsavedChanges = hasChanges
            )
        }
    }

    private fun saveFile() {
        val currentState = _state.value
        if (currentState.filePath.isEmpty() || currentState.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                withContext(Dispatchers.IO) {
                    val file = File(currentState.filePath)
                    file.writeText(currentState.content, Charset.forName("UTF-8"))
                }
                _state.update { 
                    it.copy(
                        isSaving = false,
                        initialContent = currentState.content,
                        hasUnsavedChanges = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Error saving file: ${e.message}") }
            }
        }
    }

    private fun handleBackClick() {
        if (_state.value.hasUnsavedChanges) {
            _state.update { it.copy(showUnsavedDialog = true) }
        } else {
            _navigateBack.tryEmit(Unit)
        }
    }

    private fun discardChanges() {
        _state.update { it.copy(showUnsavedDialog = false, hasUnsavedChanges = false) }
        _navigateBack.tryEmit(Unit)
    }
}
