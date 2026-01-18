package com.wuxi.player.player

import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.media3.common.Player

class PlayerManager(private val context: Context) {
    private val exoPlayer = ExoPlayer.Builder(context).build()
    
    fun getExoPlayer() = exoPlayer

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSongId = MutableStateFlow<Long?>(null)
    val currentSongId: StateFlow<Long?> = _currentSongId

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _onSongEnded = MutableStateFlow(false)
    val onSongEnded: StateFlow<Boolean> = _onSongEnded

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressUpdate()
                } else {
                    stopProgressUpdate()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration
                } else if (state == Player.STATE_ENDED) {
                    _onSongEnded.value = true
                    // Reset flag immediately so it can trigger again
                    scope.launch {
                         delay(100)
                         _onSongEnded.value = false
                    }
                }
            }
        })
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _currentPosition.value = exoPlayer.currentPosition
                delay(30) // 提高频率到 30ms，大约 33fps，视觉上更平滑
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    fun playSong(id: Long, url: String, title: String? = null, artist: String? = null, album: String? = null, artworkUri: android.net.Uri? = null) {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId(id.toString())
            .setMediaMetadata(metadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        _currentSongId.value = id
    }

    fun startService() {
        val intent = Intent(context, PlaybackService::class.java)
        context.startService(intent)
    }

    fun stopService() {
        val intent = Intent(context, PlaybackService::class.java)
        context.stopService(intent)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    fun release() {
        stopProgressUpdate()
        scope.cancel()
        exoPlayer.release()
    }
}
