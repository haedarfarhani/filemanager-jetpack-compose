package com.heydar.filemanager.ui.screens.details

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.heydar.filemanager.core.icons.AppIcons
import com.heydar.filemanager.core.util.DateFormatter
import com.heydar.filemanager.core.util.FileSizeFormatter
import com.heydar.filemanager.core.util.FileMetadataExtractor
import com.heydar.filemanager.domain.model.CommonFileInfo
import com.heydar.filemanager.domain.model.FileDetails
import com.heydar.filemanager.domain.model.FileType
import com.heydar.filemanager.domain.model.SpecificFileInfo
import com.heydar.filemanager.ui.components.file.FileIcon
import com.heydar.filemanager.ui.theme.Spacing
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
    viewModel: FileDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(filePath) {
        viewModel.onEvent(FileDetailsEvent.LoadDetails(filePath))
    }

    LaunchedEffect(Unit) {
        viewModel.shareError.collectLatest { msg ->
            snackbarHost.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("File Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(AppIcons.Nav.back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
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
                    ErrorContent(
                        message = state.error!!,
                        onRetry = { viewModel.onEvent(FileDetailsEvent.RetryClicked) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.details != null -> {
                    DetailsContent(
                        details = state.details!!,
                        isFolderSizeLoading = state.isFolderSizeLoading,
                        onShare = { viewModel.onEvent(FileDetailsEvent.ShareClicked) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailsContent(
    details: FileDetails,
    isFolderSizeLoading: Boolean,
    onShare: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = Spacing.xl)
    ) {
        // ── Header ──
        FileHeader(details.common)

        SectionDivider()

        // ── General info ──
        SectionTitle("General")
        InfoRow("Size",          sizeLabel(details))
        InfoRow("Location",      locationLabel(details.common.path))
        InfoRow("Path",          details.common.path, singleLine = false)
        InfoRow("Last Modified", DateFormatter.format(details.common.lastModified))
        InfoRow("MIME Type",     details.common.mimeType)
        if (details.common.extension.isNotEmpty()) {
            InfoRow("Extension", ".${details.common.extension.uppercase()}")
        }

        // ── Type-specific info ──
        SpecificSection(details.specific, isFolderSizeLoading)

        SectionDivider()

        // ── Action buttons ──
        if (!details.common.fileItem.isDirectory) {
            ActionButtons(onShare = onShare)
        }
    }
}

@Composable
private fun FileHeader(common: CommonFileInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.lg, horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FileIcon(fileItem = common.fileItem, size = 72.dp)
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = common.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = buildTypeBadge(common),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildTypeBadge(common: CommonFileInfo): String {
    val type = common.fileType.name.lowercase().replaceFirstChar { it.uppercase() }
    return if (common.extension.isNotEmpty()) "$type • ${common.extension.uppercase()}" else type
}

@Composable
private fun SpecificSection(specific: SpecificFileInfo, isFolderSizeLoading: Boolean) {
    when (specific) {
        is SpecificFileInfo.ImageInfo -> {
            SectionDivider()
            SectionTitle("Image Information")
            InfoRow("Dimensions", "${specific.width} × ${specific.height}")
        }
        is SpecificFileInfo.VideoInfo -> {
            SectionDivider()
            SectionTitle("Video Information")
            if (specific.durationMs > 0)
                InfoRow("Duration", FileMetadataExtractor.formatDuration(specific.durationMs))
            if (specific.width > 0 && specific.height > 0)
                InfoRow("Resolution", "${specific.width} × ${specific.height}")
        }
        is SpecificFileInfo.AudioInfo -> {
            SectionDivider()
            SectionTitle("Audio Information")
            if (specific.durationMs > 0)
                InfoRow("Duration", FileMetadataExtractor.formatDuration(specific.durationMs))
        }
        is SpecificFileInfo.ApkInfo -> {
            SectionDivider()
            SectionTitle("APK Information")
            if (specific.packageName.isNotEmpty())
                InfoRow("Package",      specific.packageName)
            if (specific.versionName.isNotEmpty())
                InfoRow("Version",      "${specific.versionName} (${specific.versionCode})")
            if (specific.minSdk > 0)
                InfoRow("Min SDK",      specific.minSdk.toString())
            if (specific.targetSdk > 0)
                InfoRow("Target SDK",   specific.targetSdk.toString())
        }
        is SpecificFileInfo.FolderInfo -> {
            SectionDivider()
            SectionTitle("Folder Information")
            InfoRow("Files",   specific.fileCount.toString())
            InfoRow("Folders", specific.folderCount.toString())
            if (isFolderSizeLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Size",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(120.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            } else if (specific.totalSize != null) {
                InfoRow("Total Size", FileSizeFormatter.format(specific.totalSize))
            }
        }
        is SpecificFileInfo.GenericInfo -> { /* nothing extra */ }
    }
}

@Composable
private fun ActionButtons(onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(AppIcons.Action.share),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text("Share")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small reusable composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = Spacing.md, end = Spacing.md,
            top = Spacing.md, bottom = 4.dp
        )
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun InfoRow(label: String, value: String, singleLine: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = 6.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            painter = painterResource(AppIcons.Status.error),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors()
        ) {
            Text("Retry")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun sizeLabel(details: FileDetails): String {
    return when (val sp = details.specific) {
        is SpecificFileInfo.FolderInfo -> if (sp.totalSize != null)
            FileSizeFormatter.format(sp.totalSize) else "—"
        else -> FileSizeFormatter.format(details.common.size)
    }
}

private fun locationLabel(path: String): String {
    return path.substringBeforeLast("/").substringAfterLast("/").ifEmpty { "/" }
}
