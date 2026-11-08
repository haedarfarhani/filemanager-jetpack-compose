package ir.heydar.nexfolder.ui.screens.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ir.heydar.nexfolder.core.files.FileOpener
import ir.heydar.nexfolder.core.util.FileShareManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collect
import java.io.File

/**
 * Full-screen image viewer with pinch-zoom/pan, double-tap reset, and swipe
 * between images of the same folder.
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

    // Keep the swiped page in the ViewModel so the position survives
    // a round-trip to the details screen and back.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onEvent(ViewerEvent.PageChanged(page))
        }
    }

    // Jump the pager to the tapped image once the sibling list finishes loading
    // (the pager is created before the async load, so initialPage alone is not enough).
    LaunchedEffect(state.imagePaths) {
        if (state.imagePaths.isNotEmpty()) {
            pagerState.scrollToPage(state.currentIndex.coerceAtMost(state.imagePaths.size - 1))
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
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(
                    imagePath = state.imagePaths[page],
                    pageKey = state.imagePaths[page]
                )
            }
        }

        TopAppBar(
            title = {
                Text(
                    text = File(state.currentPath).name.ifEmpty { "Image" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                IconButton(onClick = {
                    FileOpener.openExternal(context, state.currentPath)
                }) {
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (state.imagePaths.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1} / ${state.imagePaths.size}",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun ZoomableImage(
    imagePath: String,
    pageKey: String
) {
    var scale by remember(pageKey) { mutableFloatStateOf(1f) }
    var offset by remember(pageKey) { mutableStateOf(Offset.Zero) }
    var rotation by remember(pageKey) { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(pageKey) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offset = if (scale > 1f) {
                        Offset(offset.x + pan.x, offset.y + pan.y)
                    } else {
                        Offset.Zero
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
