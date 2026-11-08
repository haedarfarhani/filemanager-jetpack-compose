package ir.heydar.nexfolder.ui.screens.network

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import ir.heydar.nexfolder.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkFileListScreen(
    serverUrl: String,
    onNavigateBack: () -> Unit,
    viewModel: NetworkFileListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val files by viewModel.files.collectAsState()

    LaunchedEffect(serverUrl) {
        viewModel.onEvent(NetworkFileListEvent.LoadFiles(serverUrl))
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Remote Files",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (state.currentPath.isNotEmpty()) {
                                viewModel.onEvent(NetworkFileListEvent.GoBack)
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = AppIcons.Nav.back),
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(NetworkFileListEvent.Refresh) }) {
                            Icon(
                                painter = painterResource(id = AppIcons.Action.sort),
                                contentDescription = "Refresh"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Address Bar
                AddressBar(
                    serverUrl = serverUrl,
                    currentPath = state.currentPath,
                    onPathClick = { path ->
                        viewModel.onEvent(NetworkFileListEvent.NavigateTo(path))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        IconButton(onClick = { viewModel.onEvent(NetworkFileListEvent.Refresh) }) {
                            Icon(
                                painter = painterResource(id = AppIcons.Action.sort),
                                contentDescription = "Retry"
                            )
                        }
                    }
                }
                files.isEmpty() -> {
                    Text(
                        text = "This folder is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(files, key = { it.path }) { file ->
                            RemoteFileItem(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        viewModel.onEvent(NetworkFileListEvent.NavigateTo(file.path))
                                    } else {
                                        viewModel.onEvent(NetworkFileListEvent.DownloadFile(file.path))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressBar(
    serverUrl: String,
    currentPath: String,
    onPathClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = Spacing.md, vertical = 8.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = AppIcons.Storage.internal),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))

        // Server address
        Text(
            text = serverUrl.removePrefix("http://"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        // Path segments
        if (currentPath.isNotEmpty()) {
            val segments = currentPath.split("/").filter { it.isNotEmpty() }
            segments.forEachIndexed { index, segment ->
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val isLast = index == segments.lastIndex
                val fullPath = segments.take(index + 1).joinToString("/")
                Text(
                    text = segment,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLast) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = if (!isLast) {
                        Modifier.clickable { onPathClick(fullPath) }
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}

@Composable
private fun RemoteFileItem(
    file: RemoteFileItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.listItemPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                id = if (file.isDirectory) AppIcons.Folder.default else AppIcons.File.generic
            ),
            contentDescription = null,
            tint = if (file.isDirectory) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(Spacing.iconSize)
        )

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!file.isDirectory) {
                Text(
                    text = FileSizeFormatter.format(file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
