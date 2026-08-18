package com.heydar.filemanager.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.heydar.filemanager.R
import com.heydar.filemanager.core.icons.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
    viewModel: TextEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(filePath) {
        viewModel.onEvent(TextEditorEvent.LoadFile(filePath))
    }

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collectLatest {
            onNavigateBack()
        }
    }

    val navigateBackWithCheck = {
        if (state.hasUnsavedChanges) {
            viewModel.onEvent(TextEditorEvent.BackClicked)
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = true) {
        navigateBackWithCheck()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = state.fileName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.hasUnsavedChanges) {
                            Text(
                                text = "Unsaved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateBackWithCheck) {
                        Icon(
                            painter = painterResource(id = AppIcons.Nav.back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.onEvent(TextEditorEvent.SaveClicked) },
                        enabled = state.hasUnsavedChanges && !state.isSaving
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    TextField(
                        value = state.content,
                        onValueChange = { viewModel.onEvent(TextEditorEvent.ContentChanged(it)) },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }

    if (state.showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(TextEditorEvent.CancelDialog) },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(TextEditorEvent.DiscardChanges)
                        onNavigateBack()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(TextEditorEvent.CancelDialog) }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
