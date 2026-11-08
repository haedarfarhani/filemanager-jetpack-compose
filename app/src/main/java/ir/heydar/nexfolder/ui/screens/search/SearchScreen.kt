package ir.heydar.nexfolder.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.core.util.DateFormatter
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.domain.model.SearchResult
import ir.heydar.nexfolder.ui.components.file.FileIcon
import ir.heydar.nexfolder.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    scopePath: String,
    onNavigateBack: () -> Unit,
    onOpenTextEditor: (String) -> Unit = {},
    onOpenDetails: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(scopePath) {
        viewModel.onEvent(SearchEvent.Initialize(scopePath))
    }

    LaunchedEffect(state.openTextEditorFor) {
        state.openTextEditorFor?.let { path ->
            onOpenTextEditor(path)
            viewModel.onEvent(SearchEvent.EditorHandled)
        }
    }

    LaunchedEffect(state.openDetailsFor) {
        state.openDetailsFor?.let { path ->
            onOpenDetails(path)
            viewModel.onEvent(SearchEvent.DetailsHandled)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.material3.TextField(
                        value = state.query,
                        onValueChange = { viewModel.onEvent(SearchEvent.QueryChanged(it)) },
                        placeholder = { Text("Search files") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(painterResource(id = AppIcons.Nav.back), contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.hasActiveFilters) {
                        IconButton(onClick = { viewModel.onEvent(SearchEvent.ClearFilters) }) {
                            Icon(painterResource(id = AppIcons.Nav.close), contentDescription = "Clear filters")
                        }
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(painterResource(id = AppIcons.Action.sort), contentDescription = "Sort results")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        ResultSort.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.label) },
                                onClick = {
                                    viewModel.onEvent(SearchEvent.ChangeSort(sort))
                                    showSortMenu = false
                                },
                                trailingIcon = if (state.sort == sort) { { Text("✓") } } else null
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(
                        selected = !state.wholeStorage,
                        onClick = { viewModel.onEvent(SearchEvent.ToggleWholeStorage(false)) },
                        label = { Text("This folder") }
                    )
                    FilterChip(
                        selected = state.wholeStorage,
                        onClick = { viewModel.onEvent(SearchEvent.ToggleWholeStorage(true)) },
                        label = { Text("Whole storage") }
                    )
                    FilterChip(
                        selected = state.searchInContents,
                        onClick = {
                            viewModel.onEvent(SearchEvent.ToggleContentSearch(!state.searchInContents))
                        },
                        label = { Text("In contents") }
                    )
                }

                Spacer(modifier = Modifier.padding(2.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    CategoryChip("Images", FileType.IMAGE, state, viewModel)
                    CategoryChip("Videos", FileType.VIDEO, state, viewModel)
                    CategoryChip("Audio", FileType.AUDIO, state, viewModel)
                    CategoryChip("Documents", FileType.DOCUMENT, state, viewModel)
                    CategoryChip("APK", FileType.APK, state, viewModel)
                    CategoryChip("Archives", FileType.ARCHIVE, state, viewModel)
                    CategoryChip("Text", FileType.TEXT, state, viewModel)
                }

                Spacer(modifier = Modifier.padding(2.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SizePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = state.sizePreset == preset,
                            onClick = { viewModel.onEvent(SearchEvent.SizePresetChanged(preset)) },
                            label = { Text(preset.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(2.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    DatePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = state.datePreset == preset,
                            onClick = { viewModel.onEvent(SearchEvent.DatePresetChanged(preset)) },
                            label = { Text(preset.label) }
                        )
                    }
                }
            }

            // Status line
            val statusText = when {
                state.isSearching -> "Searching… ${state.scannedCount} files scanned"
                state.searchedOnce -> "${state.results.size} result(s)"
                else -> "Type a name or pick a filter"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            )

            // Results
            when {
                state.isSearching && state.results.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.searchedOnce && state.results.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = AppIcons.Status.noResults),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = Spacing.sm)
                        )
                        Text(
                            text = "No files match",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Spacing.md)
                    ) {
                        items(state.results, key = { it.file.path }) { result ->
                            SearchResultItem(
                                result = result,
                                onClick = { viewModel.onEvent(SearchEvent.ResultClicked(result)) },
                                onLongClick = { viewModel.onEvent(SearchEvent.ResultLongClicked(result)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChip(
    label: String,
    category: FileType,
    state: SearchUiState,
    viewModel: SearchViewModel
) {
    FilterChip(
        selected = category in state.selectedCategories,
        onClick = { viewModel.onEvent(SearchEvent.ToggleCategory(category)) },
        label = { Text(label) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val file = result.file
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileIcon(fileItem = file, size = 40.dp)
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.path.substringBeforeLast('/'),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (!file.isDirectory) {
                    Text(
                        text = FileSizeFormatter.format(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = DateFormatter.format(file.lastModified),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            result.contentMatch?.let { snippet ->
                Text(
                    text = "L${result.matchedLine}: $snippet",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
