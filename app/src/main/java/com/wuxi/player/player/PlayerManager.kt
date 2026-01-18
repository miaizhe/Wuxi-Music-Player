package com.wuxi.player.player

import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.os.PowerManager
import androidx.media3.common.Player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C

class PlayerManager(private val context: Context) {
    private val exoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true // 自动处理音频焦点
        )
        .setHandleAudioBecomingNoisy(true) // 拔出耳机自动暂停
        .setWakeMode(C.WAKE_MODE_NETWORK) // 保持网络唤醒
        .build()
    private val wakeLock: PowerManager.WakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WuxiPlayer:WakeLock")

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

    // 播放列表回调，用于与 ViewModel 同步
    var playlistCallback: PlaylistCallback? = null

    interface PlaylistCallback {
        fun onSkipToNext()
        fun onSkipToPrevious()
        fun onToggleFavorite()
        fun isFavorite(): Boolean
    }

    init {
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressUpdate()
                    if (!wakeLock.isHeld) wakeLock.acquire()
                } else {
                    stopProgressUpdate()
                    if (wakeLock.isHeld) wakeLock.release()
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
            .setDisplayTitle(title)
            .setSubtitle(artist)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
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
