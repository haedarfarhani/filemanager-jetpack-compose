package ir.heydar.nexfolder.ui.screens.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ir.heydar.nexfolder.core.files.FileOpener
import ir.heydar.nexfolder.core.media.FilePreviewLoader
import ir.heydar.nexfolder.core.util.FileSizeFormatter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

/**
 * Premium full-screen image viewer:
 * - Pinch-zoom / pan with clamping, double-tap zoom, per-page rotation
 * - Swipe between sibling images; thumbnail strip for direct jumps
 * - Dimensions + size chip; pager swiping pauses while zoomed in
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
    onOpenDetails: (String) -> Unit = {},
    viewModel: ImageViewerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = state.currentIndex,
        pageCount = { state.imagePaths.size }
    )

    val scope = rememberCoroutineScope()
    var zoomed by remember { mutableStateOf(false) }
    var rotation by remember { mutableIntStateOf(0) }
    var imageInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Keep the swiped page in the ViewModel so the position survives
    // a round-trip to the details screen and back.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onEvent(ViewerEvent.PageChanged(page))
        }
    }

    // Jump the pager to the tapped image once the sibling list finishes loading
    LaunchedEffect(state.imagePaths) {
        if (state.imagePaths.isNotEmpty()) {
            pagerState.scrollToPage(state.currentIndex.coerceAtMost(state.imagePaths.size - 1))
        }
    }

    // Reset rotation + load dimensions on page change
    LaunchedEffect(pagerState.currentPage) {
        rotation = 0
        state.imagePaths.getOrNull(pagerState.currentPage)?.let { path ->
            imageInfo = FilePreviewLoader.imageDimensions(path)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.imagePaths.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !zoomed
            ) { page ->
                ZoomableImage(
                    imagePath = state.imagePaths[page],
                    pageKey = state.imagePaths[page],
                    rotation = rotation,
                    onZoomChanged = { isZoomed ->
                        if (page == pagerState.currentPage) zoomed = isZoomed
                    }
                )
            }
        }

        // Top scrim + bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = File(state.currentPath).name.ifEmpty { "Image" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.imagePaths.size > 1) {
                            Text(
                                text = "${pagerState.currentPage + 1} of ${state.imagePaths.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { rotation = (rotation + 90) % 360 }) {
                        Icon(
                            imageVector = Icons.Rounded.RotateRight,
                            contentDescription = "Rotate",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { FileOpener.openExternal(context, state.currentPath) }) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { onOpenDetails(state.currentPath) }) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Details",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        // Bottom scrim + thumbnail strip
        if (state.imagePaths.size > 1) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThumbnailStrip(
                    imagePaths = state.imagePaths,
                    currentPage = pagerState.currentPage,
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
                )
                imageInfo?.let { (w, h) ->
                    Text(
                        text = "$w × $h  ·  ${FileSizeFormatter.format(File(state.currentPath).length())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    imagePath: String,
    pageKey: String,
    rotation: Int,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember(pageKey) { mutableFloatStateOf(1f) }
    var offset by remember(pageKey) { mutableStateOf(Offset.Zero) }

    // Report zoom state so the pager can pause swiping while the user pans
    LaunchedEffect(pageKey, scale) {
        onZoomChanged(scale > 1.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(pageKey) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    if (scale > 1f) {
                        // Clamp pan so the image cannot be dragged off-screen
                        val maxX = (size.width * (scale - 1f)) / 2f
                        val maxY = (size.height * (scale - 1f)) / 2f
                        offset = Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            }
            .pointerInput(pageKey) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    rotationZ = rotation.toFloat()
                }
        )
    }
}

@Composable
private fun ThumbnailStrip(
    imagePaths: List<String>,
    currentPage: Int,
    onSelect: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentPage) {
        if (imagePaths.isNotEmpty()) {
            listState.animateScrollToItem(currentPage)
        }
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(imagePaths, key = { it }) { path ->
            val index = imagePaths.indexOf(path)
            val selected = index == currentPage
            AsyncImage(
                model = path,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White.copy(alpha = 0.25f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(index) }
            )
        }
    }
}
