package ir.heydar.nexfolder.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.icons.AppIcons
import kotlinx.coroutines.flow.collectLatest

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
                    // CSV/TSV grid toggle — only for spreadsheet-like text files
                    if (state.isTabSeparated || state.fileName.endsWith(".csv", ignoreCase = true)) {
                        IconButton(
                            onClick = { viewModel.onEvent(TextEditorEvent.ToggleCsvMode) }
                        ) {
                            Icon(
                                imageVector = if (state.isCsvMode) {
                                    androidx.compose.material.icons.Icons.Rounded.Notes
                                } else {
                                    androidx.compose.material.icons.Icons.Rounded.TableView
                                },
                                contentDescription = if (state.isCsvMode) "Text view" else "Table view"
                            )
                        }
                    }
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
                    if (state.isCsvMode) {
                        CsvTableView(
                            content = state.content,
                            isTabSeparated = state.isTabSeparated
                        )
                    } else {
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

/** Simple read-only grid for the first ~500 rows of a CSV/TSV file. */
@Composable
private fun CsvTableView(
    content: String,
    isTabSeparated: Boolean
) {
    val delimiter = if (isTabSeparated) '\t' else ','
    val rows = remember(content, isTabSeparated) {
        content.split('\n')
            .take(500)
            .map { row -> row.split(delimiter).map { it.trim() } }
    }
    val columnCount = rows.maxOfOrNull { it.size }?.coerceIn(1, 24) ?: 1

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(rows) { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (index == 0) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        } else if (index % 2 == 0) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp)
                )
                repeat(columnCount) { col ->
                    Text(
                        text = row.getOrElse(col) { "" },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}
