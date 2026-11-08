package ir.heydar.nexfolder.ui.screens.analyzer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import ir.heydar.nexfolder.domain.model.CategoryUsage
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.domain.model.FolderUsage
import ir.heydar.nexfolder.domain.model.StorageAnalysis
import ir.heydar.nexfolder.ui.components.charts.DonutChart
import ir.heydar.nexfolder.ui.components.charts.DonutSegment
import ir.heydar.nexfolder.ui.components.file.FileIcon
import ir.heydar.nexfolder.ui.theme.FileColorApk
import ir.heydar.nexfolder.ui.theme.FileColorArchive
import ir.heydar.nexfolder.ui.theme.FileColorAudio
import ir.heydar.nexfolder.ui.theme.FileColorDocument
import ir.heydar.nexfolder.ui.theme.FileColorImage
import ir.heydar.nexfolder.ui.theme.FileColorOther
import ir.heydar.nexfolder.ui.theme.FileColorVideo
import ir.heydar.nexfolder.ui.theme.Spacing

private val CategoryColors = mapOf(
    FileType.IMAGE to FileColorImage,
    FileType.VIDEO to FileColorVideo,
    FileType.AUDIO to FileColorAudio,
    FileType.DOCUMENT to FileColorDocument,
    FileType.TEXT to Color(0xFF26A69A),
    FileType.ARCHIVE to FileColorArchive,
    FileType.APK to Color(0xFF00BCD4),
    FileType.OTHER to FileColorOther
)

private fun FileType.displayLabel(): String = when (this) {
    FileType.IMAGE -> "Images"
    FileType.VIDEO -> "Videos"
    FileType.AUDIO -> "Audio"
    FileType.DOCUMENT -> "Documents"
    FileType.TEXT -> "Text files"
    FileType.ARCHIVE -> "Archives"
    FileType.APK -> "Apps"
    FileType.OTHER -> "Other"
}

private enum class AnalyzerTab(val label: String) {
    CATEGORIES("Categories"),
    FOLDERS("Folders"),
    LARGEST("Largest files")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(
    rootPath: String,
    onNavigateBack: () -> Unit,
    onOpenDetails: (String) -> Unit = {},
    onOpenFolder: (String) -> Unit = {},
    viewModel: StorageAnalyzerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(rootPath) {
        viewModel.startScan(rootPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage analysis") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(painterResource(id = AppIcons.Nav.back), contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isScanning && state.analysis != null) {
                        IconButton(onClick = { viewModel.rescan() }) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Rescan")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isScanning -> ScanningState(state.scannedFiles)
                state.errorMessage != null -> ErrorState(
                    message = state.errorMessage!!,
                    onRetry = { viewModel.rescan() }
                )
                state.analysis != null -> AnalysisContent(
                    analysis = state.analysis!!,
                    onOpenDetails = onOpenDetails,
                    onOpenFolder = onOpenFolder
                )
                else -> ErrorState(
                    message = "No analysis available",
                    onRetry = { viewModel.rescan() }
                )
            }
        }
    }
}

@Composable
private fun ScanningState(scannedFiles: Long) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            strokeWidth = 5.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = "Analyzing storage…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "$scannedFiles files scanned",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Large storages may take a moment",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        TextButton(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
private fun AnalysisContent(
    analysis: StorageAnalysis,
    onOpenDetails: (String) -> Unit,
    onOpenFolder: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(AnalyzerTab.CATEGORIES) }
    val usagePercent = if (analysis.volumeCapacity > 0) {
        (analysis.totalBytes * 100 / analysis.volumeCapacity).toInt().coerceIn(0, 100)
    } else 0

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- Summary card ----
        SummaryCard(
            analysis = analysis,
            usagePercent = usagePercent,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
        )

        // ---- Interactive donut ----
        DonutSection(
            analysis = analysis,
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = if (selectedCategory == it) null else it },
            modifier = Modifier.padding(horizontal = Spacing.md)
        )

        // ---- Tabs ----
        TabRow(
            selectedTabIndex = tab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AnalyzerTab.entries.forEach { entry ->
                Tab(
                    selected = tab == entry,
                    onClick = { tab = entry },
                    text = { Text(entry.label) }
                )
            }
        }

        // ---- Tab content ----
        when (tab) {
            AnalyzerTab.CATEGORIES -> CategoriesTab(
                analysis = analysis,
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = if (selectedCategory == it) null else it }
            )
            AnalyzerTab.FOLDERS -> FoldersTab(analysis = analysis, onOpenFolder = onOpenFolder)
            AnalyzerTab.LARGEST -> LargestTab(analysis = analysis, onOpenDetails = onOpenDetails)
        }
    }
}

@Composable
private fun SummaryCard(
    analysis: StorageAnalysis,
    usagePercent: Int,
    modifier: Modifier = Modifier
) {
    val progressColor = when {
        usagePercent > 90 -> MaterialTheme.colorScheme.error
        usagePercent > 70 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (analysis.volumeCapacity > 0) {
            (analysis.totalBytes.toFloat() / analysis.volumeCapacity).coerceIn(0f, 1f)
        } else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "usage_progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = FileSizeFormatter.format(analysis.totalBytes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "used of ${FileSizeFormatter.format(analysis.volumeCapacity)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${analysis.totalFiles} files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$usagePercent% used · ${FileSizeFormatter.format(analysis.volumeCapacity - analysis.totalBytes)} free",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DonutSection(
    analysis: StorageAnalysis,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = analysis.categories.map { usage ->
        DonutSegment(
            label = usage.type.name,
            fraction = if (analysis.totalBytes > 0) usage.bytes.toFloat() / analysis.totalBytes else 0f,
            color = CategoryColors[usage.type] ?: FileColorOther
        )
    }

    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        DonutChart(
            segments = segments,
            modifier = Modifier.size(190.dp),
            strokeWidth = 24.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            selectedLabel = selectedCategory,
            onSegmentClick = onSelectCategory
        ) {
            val selected = selectedCategory?.let { name ->
                analysis.categories.firstOrNull { it.type.name == name }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(Spacing.lg)
            ) {
                if (selected != null) {
                    Text(
                        text = selected.type.displayLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = FileSizeFormatter.format(selected.bytes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CategoryColors[selected.type] ?: FileColorOther
                    )
                    val percent = if (analysis.totalBytes > 0) {
                        (selected.bytes * 100 / analysis.totalBytes).toInt()
                    } else 0
                    Text(
                        text = "$percent% · ${selected.files} file(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = FileSizeFormatter.format(analysis.totalBytes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${analysis.totalFiles} files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap a slice",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoriesTab(
    analysis: StorageAnalysis,
    selectedCategory: String?,
    onSelectCategory: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = Spacing.md, vertical = Spacing.sm
        )
    ) {
        items(analysis.categories.size) { index ->
            val usage = analysis.categories[index]
            CategoryRow(
                usage = usage,
                totalBytes = analysis.totalBytes,
                selected = usage.type.name == selectedCategory,
                onClick = { onSelectCategory(usage.type.name) }
            )
        }
    }
}

@Composable
private fun CategoryRow(
    usage: CategoryUsage,
    totalBytes: Long,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fraction = if (totalBytes > 0) (usage.bytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val percent = if (totalBytes > 0) (usage.bytes * 100 / totalBytes).toInt() else 0
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700),
        label = "category_bar"
    )
    val color = CategoryColors[usage.type] ?: FileColorOther

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = usage.type.displayLabel(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = FileSizeFormatter.format(usage.bytes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFraction)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "$percent% · ${usage.files}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FoldersTab(analysis: StorageAnalysis, onOpenFolder: (String) -> Unit) {
    if (analysis.folders.isEmpty()) {
        EmptyTabMessage("No folders found")
        return
    }
    val maxBytes = analysis.folders.firstOrNull()?.bytes?.takeIf { it > 0 } ?: 1L
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = Spacing.md, vertical = Spacing.sm
        )
    ) {
        items(analysis.folders.size) { index ->
            FolderRow(analysis.folders[index], maxBytes, onOpenFolder)
        }
    }
}

@Composable
private fun FolderRow(folder: FolderUsage, maxBytes: Long, onOpenFolder: (String) -> Unit) {
    val fraction = (folder.bytes.toFloat() / maxBytes).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700),
        label = "folder_bar"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFolder(folder.path) }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = AppIcons.Folder.default),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = FileSizeFormatter.format(folder.bytes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFraction)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "${folder.files} file(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LargestTab(analysis: StorageAnalysis, onOpenDetails: (String) -> Unit) {
    if (analysis.largestFiles.isEmpty()) {
        EmptyTabMessage("No files found")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Spacing.md, top = Spacing.sm, end = Spacing.md, bottom = Spacing.lg
        )
    ) {
        items(analysis.largestFiles.size) { index ->
            LargestFileRow(index + 1, analysis.largestFiles[index], onOpenDetails)
        }
    }
}

@Composable
private fun LargestFileRow(rank: Int, file: FileItem, onOpenDetails: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails(file.path) }
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp)
        )
        FileIcon(fileItem = file, size = 40.dp)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
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
        }
        Text(
            text = FileSizeFormatter.format(file.size),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyTabMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(Spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
