package com.heydar.filemanager.ui.screens.editor

sealed interface TextEditorEvent {
    data class LoadFile(val path: String) : TextEditorEvent
    data class ContentChanged(val newContent: String) : TextEditorEvent
    object SaveClicked : TextEditorEvent
    object BackClicked : TextEditorEvent
    object DiscardChanges : TextEditorEvent
    object CancelDialog : TextEditorEvent
}
