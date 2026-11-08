package ir.heydar.nexfolder.ui.screens.player

import android.app.Application
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import ir.heydar.nexfolder.core.history.RecentsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class AudioPlayerEvent {
    data object TogglePlayPause : AudioPlayerEvent()
    data object Next : AudioPlayerEvent()
    data object Previous : AudioPlayerEvent()
    data class SeekTo(val positionMs: Long) : AudioPlayerEvent()
}

data class AudioPlayerUiState(
    val playlist: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val title: String = "",
    val artist: String = "",
    val coverImage: android.graphics.Bitmap? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isLoading: Boolean = true
)

/**
 * Simple in-screen audio player built on ExoPlayer. Playback lives as long as this
 * screen exists (closing the screen stops playback — intentionally simple).
 */
@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val app: Application,
    private val recentsManager: RecentsManager
) : ViewModel() {

    private val initialPath: String = savedStateHandle.get<String>("filePath") ?: ""

    private val _state = MutableStateFlow(AudioPlayerUiState())
    val state: StateFlow<AudioPlayerUiState> = _state.asStateFlow()

    private var player: ExoPlayer? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = player?.currentMediaItemIndex ?: return
            _state.update { it.copy(currentIndex = index) }
            loadMetadata(index)
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            recentsManager.record(initialPath)
        }
        viewModelScope.launch {
            val playlist = buildPlaylist()
            _state.update {
                it.copy(
                    playlist = playlist,
                    currentIndex = playlist.indexOf(initialPath).coerceAtLeast(0),
                    title = File(initialPath).name,
                    isLoading = playlist.isEmpty()
                )
            }
            createPlayer(playlist)
            loadMetadata(_state.value.currentIndex)
        }
        viewModelScope.launch {
            while (isActive) {
                player?.let { exo ->
                    _state.update {
                        it.copy(
                            positionMs = exo.currentPosition.coerceAtLeast(0),
                            durationMs = exo.duration.takeIf { d -> d > 0 } ?: 0
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun buildPlaylist(): List<String> {
        val folder = File(initialPath).parentFile ?: return listOf(initialPath)
        val siblings = folder.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS }
            ?.map { it.absolutePath }
            ?.sortedBy { it.lowercase() }
        return if (siblings.isNullOrEmpty()) listOf(initialPath) else siblings
    }

    private fun createPlayer(playlist: List<String>) {
        val exo = ExoPlayer.Builder(app).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            addListener(listener)
            setMediaItems(playlist.map { path -> MediaItem.fromUri(android.net.Uri.fromFile(File(path))) })
            seekTo(_state.value.currentIndex, 0)
            prepare()
            playWhenReady = true
        }
        player = exo
    }

    private fun loadMetadata(index: Int) {
        val path = _state.value.playlist.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            var title = File(path).name
            var artist = ""
            var cover: android.graphics.Bitmap? = null
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(path)
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: title
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    ?: ""
                val art = retriever.embeddedPicture
                if (art != null) {
                    cover = BitmapFactory.decodeByteArray(art, 0, art.size)
                }
            } catch (e: Exception) {
                // Metadata is optional
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // Older APIs may not support release via AutoCloseable
                }
            }
            _state.update { it.copy(title = title, artist = artist, coverImage = cover) }
        }
    }

    fun onEvent(event: AudioPlayerEvent) {
        val exo = player ?: return
        when (event) {
            is AudioPlayerEvent.TogglePlayPause -> {
                if (exo.isPlaying) exo.pause() else exo.play()
            }
            is AudioPlayerEvent.Next -> exo.seekToNextMediaItem()
            is AudioPlayerEvent.Previous -> {
                if (exo.currentPosition > 3000) {
                    exo.seekTo(0)
                } else {
                    exo.seekToPreviousMediaItem()
                }
            }
            is AudioPlayerEvent.SeekTo -> exo.seekTo(event.positionMs)
        }
    }

    override fun onCleared() {
        player?.removeListener(listener)
        player?.release()
        player = null
        super.onCleared()
    }

    private companion object {
        val AUDIO_EXTENSIONS = setOf(
            "mp3", "wav", "flac", "aac", "ogg", "oga", "m4a", "wma", "opus", "mid", "midi", "amr"
        )
    }
}
