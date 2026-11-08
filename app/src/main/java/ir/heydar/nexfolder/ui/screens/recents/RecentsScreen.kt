package ir.heydar.nexfolder.ui.screens.recents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.core.util.DateFormatter
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.ui.components.file.FileIcon
import ir.heydar.nexfolder.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    onNavigateBack: () -> Unit,
    onOpenTextEditor: (String) -> Unit = {},
    onOpenDetails: (String) -> Unit = {},
    onOpenViewer: (String) -> Unit = {},
    onOpenPlayer: (String) -> Unit = {},
    viewModel: RecentsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.openTextEditorFor) {
        state.openTextEditorFor?.let { path ->
            onOpenTextEditor(path)
            viewModel.onEvent(RecentsEvent.EditorHandled)
        }
    }

    LaunchedEffect(state.openDetailsFor) {
        state.openDetailsFor?.let { path ->
            onOpenDetails(path)
            viewModel.onEvent(RecentsEvent.DetailsHandled)
        }
    }

    LaunchedEffect(state.openViewerFor) {
        state.openViewerFor?.let { path ->
            onOpenViewer(path)
            viewModel.onEvent(RecentsEvent.ViewerHandled)
        }
    }

    LaunchedEffect(state.openPlayerFor) {
        state.openPlayerFor?.let { path ->
            onOpenPlayer(path)
            viewModel.onEvent(RecentsEvent.PlayerHandled)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent files") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(painterResource(id = AppIcons.Nav.back), contentDescription = "Back")
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
            TabRow(selectedTabIndex = state.tab.ordinal) {
                RecentsTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.onEvent(RecentsEvent.TabChanged(tab)) },
                        text = { Text(tab.label) }
                    )
                }
            }

            val files = when (state.tab) {
                RecentsTab.ADDED -> state.addedFiles
                RecentsTab.OPENED -> state.openedFiles
            }

            when {
                state.isLoading -> {
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
                files.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = AppIcons.Status.empty),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Nothing here yet",
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
                        items(files, key = { it.path }) { file ->
                            RecentFileRow(
                                fileItem = file,
                                onClick = { viewModel.onEvent(RecentsEvent.ItemClicked(file)) },
                                onLongClick = { viewModel.onEvent(RecentsEvent.ItemLongClicked(file)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentFileRow(
    fileItem: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileIcon(fileItem = fileItem, size = 40.dp)
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = fileItem.path.substringBeforeLast('/'),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!fileItem.isDirectory) {
            Text(
                text = FileSizeFormatter.format(fileItem.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = DateFormatter.format(fileItem.lastModified),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
