package ir.heydar.nexfolder.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import ir.heydar.nexfolder.domain.model.StorageVolumeInfo
import ir.heydar.nexfolder.ui.theme.Amaranth
import ir.heydar.nexfolder.ui.theme.CoralGlow
import ir.heydar.nexfolder.ui.theme.DarkAmaranth
import ir.heydar.nexfolder.ui.theme.FileColorAudio
import ir.heydar.nexfolder.ui.theme.FileColorDocument
import ir.heydar.nexfolder.ui.theme.FileColorImage
import ir.heydar.nexfolder.ui.theme.NightBordeaux
import ir.heydar.nexfolder.ui.theme.SandyBrown
import ir.heydar.nexfolder.ui.theme.Spacing
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStorageClick: (String) -> Unit,
    onNetworkClick: () -> Unit = {},
    onAnalyzeClick: (String) -> Unit = {},
    onSearchClick: (String) -> Unit = {},
    onRecentsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                viewModel.loadVolumes()
            } else {
                viewModel.onEvent(HomeEvent.ShowError("Storage access was denied"))
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.loadVolumes()
        else viewModel.onEvent(HomeEvent.ShowError("Storage permission denied"))
    }

    LaunchedEffect(Unit) {
        checkAndRequestPermission(
            context = context,
            onPermissionGranted = { viewModel.loadVolumes() },
            onRequestManageStorage = {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                manageStorageLauncher.launch(intent)
            },
            onRequestReadPermission = {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { HomeTopBar(onSettingsClick = onSettingsClick) }
    ) { paddingValues ->
        when {
            state.isLoading -> HomeLoadingState(paddingValues)
            state.errorMessage != null -> HomeErrorState(
                message = state.errorMessage!!,
                paddingValues = paddingValues,
                onRetry = { viewModel.loadVolumes() }
            )
            else -> HomeContent(
                state = state,
                paddingValues = paddingValues,
                onStorageClick = onStorageClick,
                onNetworkClick = onNetworkClick,
                onAnalyzeClick = onAnalyzeClick,
                onSearchClick = onSearchClick,
                onRecentsClick = onRecentsClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "NexFolder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your space, beautifully organized",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            Surface(
                onClick = onSettingsClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = Spacing.md)
                    .size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    paddingValues: PaddingValues,
    onStorageClick: (String) -> Unit,
    onNetworkClick: () -> Unit,
    onAnalyzeClick: (String) -> Unit,
    onSearchClick: (String) -> Unit,
    onRecentsClick: () -> Unit
) {
    val volumes = state.storageVolumes
    val fallbackRoot = remember { Environment.getExternalStorageDirectory().absolutePath }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = volumes.firstOrNull { it.id == selectedId } ?: volumes.firstOrNull()
    val rootPath = selected?.path ?: fallbackRoot
    val quickFolders = remember(rootPath) { findQuickFolders(rootPath) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = Spacing.screenPadding,
            end = Spacing.screenPadding,
            top = Spacing.sm,
            bottom = Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            if (selected != null) {
                HeroStorageCard(
                    volume = selected,
                    onAnalyze = { onAnalyzeClick(selected.path) },
                    onBrowse = { onStorageClick(Uri.encode(selected.path)) }
                )
            } else {
                StorageUnavailableCard()
            }
        }

        if (volumes.size > 1) {
            item {
                VolumeSwitcher(
                    volumes = volumes,
                    selectedId = selected?.id,
                    onSelect = { selectedId = it }
                )
            }
        }

        item {
            Column {
                SectionHeader(
                    title = "Tools",
                    subtitle = "Everything one tap away"
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                ToolsGrid(
                    onSearch = { onSearchClick(rootPath) },
                    onRecents = onRecentsClick,
                    onAnalyze = { onAnalyzeClick(rootPath) },
                    onNetwork = onNetworkClick
                )
            }
        }

        if (quickFolders.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(
                        title = "Quick access",
                        subtitle = "${quickFolders.size} shortcuts"
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    QuickAccessGrid(
                        folders = quickFolders,
                        onOpen = { folder -> onStorageClick(Uri.encode(folder.path)) }
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Storage",
                subtitle = if (volumes.isEmpty()) null else "${volumes.size} volume(s)"
            )
        }

        items(volumes, key = { it.id }) { volume ->
            VolumeCard(
                volume = volume,
                highlighted = volume.id == selected?.id,
                onOpen = { onStorageClick(Uri.encode(volume.path)) },
                onAnalyze = { onAnalyzeClick(volume.path) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------

@Composable
private fun HeroStorageCard(
    volume: StorageVolumeInfo,
    onAnalyze: () -> Unit,
    onBrowse: () -> Unit
) {
    val progress = volume.usageProgress
    val percent = (progress * 100).roundToInt()
    val freePercent = (100 - percent).coerceIn(0, 100)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(NightBordeaux, DarkAmaranth, Amaranth)
                )
            )
    ) {
        // Decorative glows
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CoralGlow.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.02f),
                    radius = size.minDimension * 0.85f
                ),
                radius = size.minDimension * 0.85f,
                center = Offset(size.width * 0.95f, size.height * 0.02f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SandyBrown.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.02f, size.height * 0.98f),
                    radius = size.minDimension * 0.6f
                ),
                radius = size.minDimension * 0.6f,
                center = Offset(size.width * 0.02f, size.height * 0.98f)
            )
        }

        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = volume.iconRes()),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm + Spacing.xs))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = volume.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = volume.typeLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.xs)
                ) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "USED SPACE",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = FileSizeFormatter.format(volume.usedBytes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "of ${FileSizeFormatter.format(volume.totalBytes)} · ${FileSizeFormatter.format(volume.freeBytes)} free",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }

                StorageRing(
                    progress = progress,
                    modifier = Modifier.size(104.dp),
                    strokeWidth = 12.dp,
                    gradient = listOf(CoralGlow, SandyBrown, Amaranth),
                    trackColor = Color.White.copy(alpha = 0.18f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "used",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            SegmentedUsageBar(
                progress = progress,
                segments = 28,
                height = 8.dp,
                activeColors = listOf(Color.White, SandyBrown, CoralGlow),
                trackColor = Color.White.copy(alpha = 0.16f)
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$percent% used",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Text(
                    text = "$freePercent% free",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Surface(
                    onClick = onAnalyze,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    contentColor = NightBordeaux
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Analyze",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Surface(
                    onClick = onBrowse,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm + 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Browse",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageUnavailableCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = AppIcons.Storage.internal),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "No storage detected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Grant storage access to see your volumes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Volume switcher
// ---------------------------------------------------------------------------

@Composable
private fun VolumeSwitcher(
    volumes: List<StorageVolumeInfo>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(volumes, key = { it.id }) { volume ->
            val selected = volume.id == selectedId
            Surface(
                onClick = { onSelect(volume.id) },
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = if (selected) {
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                } else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = volume.iconRes()),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs + 2.dp))
                    Text(
                        text = volume.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tools
// ---------------------------------------------------------------------------

@Composable
private fun ToolsGrid(
    onSearch: () -> Unit,
    onRecents: () -> Unit,
    onAnalyze: () -> Unit,
    onNetwork: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ToolTile(
            icon = Icons.Rounded.Search,
            label = "Search",
            tint = FileColorDocument,
            onClick = onSearch,
            modifier = Modifier.weight(1f)
        )
        ToolTile(
            icon = Icons.Rounded.History,
            label = "Recent",
            tint = FileColorAudio,
            onClick = onRecents,
            modifier = Modifier.weight(1f)
        )
        ToolTile(
            icon = Icons.Rounded.PieChart,
            label = "Analyze",
            tint = MaterialTheme.colorScheme.primary,
            onClick = onAnalyze,
            modifier = Modifier.weight(1f)
        )
        ToolTile(
            icon = Icons.Rounded.Wifi,
            label = "Network",
            tint = FileColorImage,
            onClick = onNetwork,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToolTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md, horizontal = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.linearGradient(
                        listOf(tint.copy(alpha = 0.26f), tint.copy(alpha = 0.10f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Quick access folders
// ---------------------------------------------------------------------------

private data class QuickFolder(
    val label: String,
    val path: String,
    @DrawableRes val icon: Int,
    val tint: Color
)

private fun findQuickFolders(rootPath: String): List<QuickFolder> {
    val base = rootPath.trimEnd('/')
    val candidates = listOf(
        QuickFolder("Downloads", "$base/Download", AppIcons.Folder.download, Color(0xFF8E5BD9)),
        QuickFolder("Pictures", "$base/Pictures", AppIcons.Folder.pictures, FileColorImage),
        QuickFolder("Music", "$base/Music", AppIcons.Folder.music, FileColorAudio),
        QuickFolder("Movies", "$base/Movies", AppIcons.Folder.movies, Color(0xFF2196F3)),
        QuickFolder("Documents", "$base/Documents", AppIcons.Folder.documents, SandyBrown),
        QuickFolder("Camera", "$base/DCIM/Camera", AppIcons.Folder.dcim, CoralGlow)
    )
    return candidates.filter { folder ->
        runCatching { File(folder.path).isDirectory }.getOrDefault(false)
    }
}

@Composable
private fun QuickAccessGrid(
    folders: List<QuickFolder>,
    onOpen: (QuickFolder) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        folders.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                rowItems.forEach { folder ->
                    QuickFolderTile(
                        folder = folder,
                        onClick = { onOpen(folder) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickFolderTile(
    folder: QuickFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md, horizontal = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(folder.tint.copy(alpha = 0.26f), folder.tint.copy(alpha = 0.08f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = folder.icon),
                contentDescription = null,
                tint = folder.tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs + 2.dp))
        Text(
            text = folder.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------
// Volume cards
// ---------------------------------------------------------------------------

@Composable
private fun VolumeCard(
    volume: StorageVolumeInfo,
    highlighted: Boolean,
    onOpen: () -> Unit,
    onAnalyze: () -> Unit
) {
    val percent = (volume.usageProgress * 100).roundToInt()
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .border(
                width = 1.dp,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                },
                shape = shape
            )
            .clickable(onClick = onOpen)
            .padding(Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = volume.iconRes()),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.sm + Spacing.xs))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volume.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${FileSizeFormatter.format(volume.freeBytes)} free of ${FileSizeFormatter.format(volume.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onAnalyze, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.PieChart,
                    contentDescription = "Analyze ${volume.name}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))

        SegmentedUsageBar(
            progress = volume.usageProgress,
            segments = 22,
            height = 7.dp,
            activeColors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary
            ),
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${FileSizeFormatter.format(volume.usedBytes)} used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap to browse",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun StorageVolumeInfo.iconRes(): Int = when {
    isUsb -> AppIcons.Storage.usb
    isRemovable -> AppIcons.Storage.sdCard
    else -> AppIcons.Storage.internal
}

private fun StorageVolumeInfo.typeLabel(): String = when {
    isUsb -> "USB drive"
    isRemovable -> "SD card"
    else -> "Internal storage"
}

// ---------------------------------------------------------------------------
// Loading & error
// ---------------------------------------------------------------------------

@Composable
private fun HomeLoadingState(paddingValues: PaddingValues) {
    val transition = rememberInfiniteTransition(label = "home_shimmer")
    val shimmer by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home_shimmer_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = Spacing.screenPadding, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(248.dp),
            radius = 28.dp,
            alpha = shimmer
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            repeat(4) {
                ShimmerBlock(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp),
                    radius = 18.dp,
                    alpha = shimmer
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            repeat(3) {
                ShimmerBlock(
                    modifier = Modifier
                        .weight(1f)
                        .height(78.dp),
                    radius = 18.dp,
                    alpha = shimmer
                )
            }
        }
        repeat(2) {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                radius = 22.dp,
                alpha = shimmer
            )
        }
    }
}

@Composable
private fun ShimmerBlock(modifier: Modifier, radius: androidx.compose.ui.unit.Dp, alpha: Float) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

@Composable
private fun HomeErrorState(
    message: String,
    paddingValues: PaddingValues,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(Spacing.xl)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = AppIcons.Status.error),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Can't read storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            TextButton(onClick = onRetry) {
                Text("Try again")
            }
        }
    }
}

private fun checkAndRequestPermission(
    context: android.content.Context,
    onPermissionGranted: () -> Unit,
    onRequestManageStorage: () -> Unit,
    onRequestReadPermission: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (Environment.isExternalStorageManager()) {
            onPermissionGranted()
        } else {
            onRequestManageStorage()
        }
    } else {
        val readPermission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(context, readPermission) == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            onRequestReadPermission()
        }
    }
}
