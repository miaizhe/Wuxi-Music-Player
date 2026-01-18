package com.wuxi.player.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wuxi.player.api.MusicApi
import com.wuxi.player.model.*
import com.wuxi.player.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MusicViewModel(
    private val playerManager: PlayerManager,
    private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        val Factory: (PlayerManager, Context) -> ViewModelProvider.Factory = { playerManager, context ->
            viewModelFactory {
                initializer {
                    MusicViewModel(playerManager, context)
                }
            }
        }
    }
    
    private val api = Retrofit.Builder()
        .baseUrl(MusicApi.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MusicApi::class.java)

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults

    private val _recommendedPlaylists = MutableStateFlow<List<PersonalizedPlaylist>>(emptyList())
    val recommendedPlaylists: StateFlow<List<PersonalizedPlaylist>> = _recommendedPlaylists

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics

    private val _favoriteSongs = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongs: StateFlow<Set<Long>> = _favoriteSongs

    // 收藏列表（完整对象）
    private val _favoriteSongList = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongList: StateFlow<List<Song>> = _favoriteSongList

    // 播放历史
    private val _playHistory = MutableStateFlow<List<Song>>(emptyList())
    val playHistory: StateFlow<List<Song>> = _playHistory

    private val _customBackground = MutableStateFlow<String?>(null)
    val customBackground: StateFlow<String?> = _customBackground

    // 背景透明度，默认 0.6f
    private val _backgroundAlpha = MutableStateFlow(0.6f)
    val backgroundAlpha: StateFlow<Float> = _backgroundAlpha

    // 遮罩透明度，默认 0.3f
    private val _overlayAlpha = MutableStateFlow(0.3f)
    val overlayAlpha: StateFlow<Float> = _overlayAlpha

    // 歌词颜色，默认白色
    private val _lyricColor = MutableStateFlow(android.graphics.Color.WHITE)
    val lyricColor: StateFlow<Int> = _lyricColor

    // 歌词高亮颜色，默认绿色
    private val _lyricHighlightColor = MutableStateFlow(android.graphics.Color.GREEN)
    val lyricHighlightColor: StateFlow<Int> = _lyricHighlightColor

    // 音质设置：standard, higher, exhigh, lossless, hires
    private val _soundQuality = MutableStateFlow("standard")
    val soundQuality: StateFlow<String> = _soundQuality

    // 旋转封面开关，默认开启
    private val _rotateCover = MutableStateFlow(true)
    val rotateCover: StateFlow<Boolean> = _rotateCover

    // 歌词方框高度比例，默认 0.6f
    private val _lyricViewHeightRatio = MutableStateFlow(0.6f)
    val lyricViewHeightRatio: StateFlow<Float> = _lyricViewHeightRatio

    // 是否在通知栏显示播放控制，默认开启
    private val _showNotification = MutableStateFlow(true)
    val showNotification: StateFlow<Boolean> = _showNotification

    // 下载保存路径，默认是系统的 Music 目录
    private val _downloadPath = MutableStateFlow("Music")
    val downloadPath: StateFlow<String> = _downloadPath

    // 本地歌曲列表
    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs

    private val _playbackMode = MutableStateFlow(PlaybackMode.LIST)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist

    val isPlaying = playerManager.isPlaying
    val currentPosition = playerManager.currentPosition
    val duration = playerManager.duration

    init {
        loadFavorites()
        loadHistory()
        loadSettings()
        loadLastState()
        fetchRecommendations()
        scanLocalSongs()
        
        viewModelScope.launch {
            playerManager.onSongEnded.collect { ended ->
                if (ended) {
                    playNext()
                }
            }
        }
    }

    fun scanLocalSongs() {
        viewModelScope.launch {
            try {
                val songs = mutableListOf<Song>()
                val contentResolver = context.contentResolver
                val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${android.provider.MediaStore.Audio.Media.TITLE} ASC"
                
                val cursor = contentResolver.query(uri, null, selection, null, sortOrder)
                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val titleCol = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                    val artistCol = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                    val albumCol = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
                    val durationCol = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                    val dataCol = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)

                    while (it.moveToNext()) {
                        val id = it.getLong(idCol)
                        val title = it.getString(titleCol) ?: "未知歌曲"
                        val artist = it.getString(artistCol) ?: "未知歌手"
                        val album = it.getString(albumCol) ?: "未知专辑"
                        val data = it.getString(dataCol)
                        
                        // 构造本地歌曲对象
                        songs.add(Song(
                            id = id,
                            name = title,
                            ar = listOf(Artist(id = 0, name = artist)),
                            al = Album(id = 0, name = album, picUrl = null),
                            dt = it.getLong(durationCol),
                            localPath = data
                        ))
                    }
                }
                _localSongs.value = songs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadHistory() {
        val json = prefs.getString("play_history", null)
        if (json != null) {
            val type = object : TypeToken<List<Song>>() {}.type
            _playHistory.value = gson.fromJson(json, type)
        }
    }

    private fun addToHistory(song: Song) {
        val current = _playHistory.value.toMutableList()
        current.removeAll { it.id == song.id }
        current.add(0, song)
        if (current.size > 50) { // Keep last 50
            current.removeAt(current.size - 1)
        }
        _playHistory.value = current
        prefs.edit().putString("play_history", gson.toJson(current)).apply()
    }

    private fun loadLastState() {
        val lastSongJson = prefs.getString("last_song", null)
        val lastPosition = prefs.getLong("last_position", 0L)
        val lastPlaylistJson = prefs.getString("last_playlist", null)
        
        if (lastPlaylistJson != null) {
            val type = object : TypeToken<List<Song>>() {}.type
            _playlist.value = gson.fromJson(lastPlaylistJson, type)
        }

        if (lastSongJson != null) {
            val song = gson.fromJson(lastSongJson, Song::class.java)
            _currentSong.value = song
            // 不自动播放，只是准备
            // 我们需要获取 URL 才能 seek
            viewModelScope.launch {
                try {
                    val urlResponse = api.getSongUrl(song.id, _soundQuality.value)
                    val url = urlResponse.data.firstOrNull()?.url
                    if (url != null) {
                        playerManager.playSong(
                            song.id, 
                            url,
                            title = song.name,
                            artist = song.ar.joinToString("/") { it.name },
                            album = song.al.name,
                            artworkUri = if (song.al.picUrl != null) android.net.Uri.parse(song.al.picUrl) else null
                        )
                        playerManager.togglePlayPause() // 立即暂停，不要自动播放
                        playerManager.seekTo(lastPosition)
                        fetchLyrics(song.id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun saveState() {
        val song = _currentSong.value
        if (song != null) {
            prefs.edit()
                .putString("last_song", gson.toJson(song))
                .putLong("last_position", playerManager.currentPosition.value)
                .putString("last_playlist", gson.toJson(_playlist.value))
                .apply()
        }
    }

    private fun loadFavorites() {
        // Load IDs
        val savedIds = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        _favoriteSongs.value = savedIds.mapNotNull { it.toLongOrNull() }.toSet()
        
        // Load Objects
        val json = prefs.getString("favorite_songs_list", null)
        if (json != null) {
            val type = object : TypeToken<List<Song>>() {}.type
            _favoriteSongList.value = gson.fromJson(json, type)
        }
    }

    private fun saveFavorites(favorites: Set<Long>, list: List<Song>) {
        prefs.edit()
            .putStringSet("favorites", favorites.map { it.toString() }.toSet())
            .putString("favorite_songs_list", gson.toJson(list))
            .apply()
    }

    private fun loadSettings() {
        _customBackground.value = prefs.getString("custom_background", null)
        _backgroundAlpha.value = prefs.getFloat("background_alpha", 0.6f)
        _overlayAlpha.value = prefs.getFloat("overlay_alpha", 0.3f)
        _lyricColor.value = prefs.getInt("lyric_color", android.graphics.Color.WHITE)
        _lyricHighlightColor.value = prefs.getInt("lyric_highlight_color", android.graphics.Color.GREEN)
        _soundQuality.value = prefs.getString("sound_quality", "standard") ?: "standard"
        _rotateCover.value = prefs.getBoolean("rotate_cover", true)
        _lyricViewHeightRatio.value = prefs.getFloat("lyric_view_height_ratio", 0.6f)
        _downloadPath.value = prefs.getString("download_path", "Music") ?: "Music"
        _showNotification.value = prefs.getBoolean("show_notification", true)
        if (_showNotification.value) {
            playerManager.startService()
        }
    }

    fun setShowNotification(show: Boolean) {
        _showNotification.value = show
        prefs.edit().putBoolean("show_notification", show).apply()
        if (show) {
            playerManager.startService()
        } else {
            playerManager.stopService()
        }
    }

    fun setDownloadPath(path: String) {
        _downloadPath.value = path
        prefs.edit().putString("download_path", path).apply()
    }

    fun setRotateCover(rotate: Boolean) {
        _rotateCover.value = rotate
        prefs.edit().putBoolean("rotate_cover", rotate).apply()
    }

    fun setLyricViewHeightRatio(ratio: Float) {
        _lyricViewHeightRatio.value = ratio
        prefs.edit().putFloat("lyric_view_height_ratio", ratio).apply()
    }

    fun setSoundQuality(quality: String) {
        if (_soundQuality.value == quality) return
        
        _soundQuality.value = quality
        prefs.edit().putString("sound_quality", quality).apply()
        
        // Reload current song if playing to apply new quality
        val current = _currentSong.value
        if (current != null) {
            val currentPos = playerManager.currentPosition.value
            val wasPlaying = playerManager.isPlaying.value
            
            viewModelScope.launch {
                try {
                    val urlResponse = api.getSongUrl(current.id, quality)
                    val url = urlResponse.data.firstOrNull()?.url
                    if (url != null) {
                        playerManager.playSong(
                            current.id, 
                            url,
                            title = current.name,
                            artist = current.ar.joinToString("/") { it.name },
                            album = current.al.name,
                            artworkUri = if (current.al.picUrl != null) android.net.Uri.parse(current.al.picUrl) else null
                        )
                        playerManager.seekTo(currentPos)
                        if (!wasPlaying) {
                             playerManager.togglePlayPause()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setCustomBackground(url: String?) {
        // 如果是 URL，尝试追加时间戳以避免缓存（虽然用户输入的 URL 可能是固定的，但如果是 API 地址，追加参数可能有助于刷新）
        // 不过，如果用户想要每次启动刷新，我们需要在加载时追加。这里只是保存配置。
        // 为了支持随机图 API，我们在 load 时不要做特殊处理，但在 UI 使用 Coil 加载时，应该追加时间戳。
        _customBackground.value = url
        prefs.edit().putString("custom_background", url).apply()
    }

    fun setBackgroundAlpha(alpha: Float) {
        _backgroundAlpha.value = alpha
        prefs.edit().putFloat("background_alpha", alpha).apply()
    }

    fun setOverlayAlpha(alpha: Float) {
        _overlayAlpha.value = alpha
        prefs.edit().putFloat("overlay_alpha", alpha).apply()
    }

    fun setLyricColor(color: Int) {
        _lyricColor.value = color
        prefs.edit().putInt("lyric_color", color).apply()
    }

    fun setLyricHighlightColor(color: Int) {
        _lyricHighlightColor.value = color
        prefs.edit().putInt("lyric_highlight_color", color).apply()
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            try {
                // 1. 下载歌曲
                val quality = if (_soundQuality.value == "standard") "exhigh" else _soundQuality.value
                val urlResponse = api.getSongUrl(song.id, quality)
                val url = urlResponse.data.firstOrNull()?.url
                
                val baseFileName = "${song.name} - ${song.ar.joinToString(", ")}"
                val subPath = _downloadPath.value.trim().removePrefix("/").removeSuffix("/")
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager

                if (url != null) {
                    val extension = when {
                        url.contains(".flac", ignoreCase = true) -> "flac"
                        url.contains(".wav", ignoreCase = true) -> "wav"
                        url.contains(".m4a", ignoreCase = true) -> "m4a"
                        else -> "mp3"
                    }
                    val songFileName = "$baseFileName.$extension"
                    val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
                        .setTitle("正在下载歌曲: ${song.name}")
                        .setDescription("${song.ar.joinToString(", ")}")
                        .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)
                    
                    if (subPath.isEmpty() || subPath.equals("Music", ignoreCase = true)) {
                        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MUSIC, songFileName)
                    } else {
                        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MUSIC, "$subPath/$songFileName")
                    }
                    downloadManager.enqueue(request)
                }

                // 2. 下载封面
                val picUrl = song.al.picUrl
                if (!picUrl.isNullOrEmpty()) {
                    val coverFileName = "$baseFileName.jpg"
                    val coverRequest = android.app.DownloadManager.Request(android.net.Uri.parse(picUrl))
                        .setTitle("正在下载封面: ${song.name}")
                        .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_HIDDEN)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)

                    if (subPath.isEmpty() || subPath.equals("Music", ignoreCase = true)) {
                        coverRequest.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MUSIC, coverFileName)
                    } else {
                        coverRequest.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MUSIC, "$subPath/$coverFileName")
                    }
                    downloadManager.enqueue(coverRequest)
                }

                // 3. 下载歌词 (保存为 .lrc 文件)
                try {
                    val lyricResponse = api.getLyric(song.id)
                    val lyricContent = lyricResponse.lrc?.lyric
                    if (!lyricContent.isNullOrEmpty()) {
                        saveLyricToFile(baseFileName, subPath, lyricContent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveLyricToFile(fileName: String, subPath: String, content: String) {
        try {
            val lyricFileName = "$fileName.lrc"
            val musicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
            val targetDir = if (subPath.isEmpty() || subPath.equals("Music", ignoreCase = true)) {
                musicDir
            } else {
                java.io.File(musicDir, subPath)
            }

            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val file = java.io.File(targetDir, lyricFileName)
            file.writeText(content)
            
            // 通知媒体库更新
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("text/plain"),
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveState()
        playerManager.release()
    }

    fun playNext() {
        val current = _currentSong.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return
        
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) return
        
        val nextIndex = when (_playbackMode.value) {
            PlaybackMode.SHUFFLE -> list.indices.random()
            PlaybackMode.REPEAT_ONE -> currentIndex // Repeat one
            else -> (currentIndex + 1) % list.size
        }
        
        playSong(list[nextIndex])
    }
    
    fun playPrevious() {
        val current = _currentSong.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return
        
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) return
        
        val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
        playSong(list[prevIndex])
    }

    fun fetchRecommendations() {
        viewModelScope.launch {
            try {
                val response = api.getPersonalized()
                _recommendedPlaylists.value = response.result ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getPlaylistDetail(playlistId)
                val tracks = response.playlist?.tracks ?: emptyList()
                if (tracks.isNotEmpty()) {
                    _playlist.value = tracks
                    playSong(tracks[0])
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.search(query)
                _searchResults.value = response.result?.songs ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        // 如果当前搜索结果中有这首歌，更新播放列表
        if (_searchResults.value.contains(song)) {
            _playlist.value = _searchResults.value
        } else if (_favoriteSongList.value.contains(song)) {
             // 如果在收藏列表中播放，更新播放列表为收藏列表
             _playlist.value = _favoriteSongList.value
        } else if (!_playlist.value.contains(song)) {
            _playlist.value = _playlist.value + song
        }
        
        saveState()
        addToHistory(song) // 添加到历史记录

        // 如果有本地路径，直接播放本地文件
        if (song.localPath != null) {
            playerManager.playSong(
                song.id, 
                "file://${song.localPath}",
                title = song.name,
                artist = song.ar.joinToString("/") { it.name },
                album = song.al.name,
                artworkUri = null // 本地歌曲暂无封面 Uri
            )
            fetchLyrics(song.id) // 尝试在线获取歌词
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val urlResponse = api.getSongUrl(song.id, _soundQuality.value)
                val url = urlResponse.data.firstOrNull()?.url
                if (url != null) {
                    playerManager.playSong(
                        song.id, 
                        url,
                        title = song.name,
                        artist = song.ar.joinToString("/") { it.name },
                        album = song.al.name,
                        artworkUri = if (song.al.picUrl != null) android.net.Uri.parse(song.al.picUrl) else null
                    )
                    fetchLyrics(song.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchLyrics(songId: Long) {
        viewModelScope.launch {
            try {
                val response = api.getLyric(songId)
                val lrc = response.lrc?.lyric ?: ""
                val tlrc = response.tlyric?.lyric ?: ""
                _lyrics.value = parseLyrics(lrc, tlrc)
            } catch (e: Exception) {
                e.printStackTrace()
                _lyrics.value = emptyList()
            }
        }
    }

    private fun parseLyrics(lrc: String, tlrc: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        // 改进正则：支持 [00:00.00]、[00:00.000]、[00:00:00] 等多种格式
        val timeRegex = Regex("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\]")
        
        val lrcMap = mutableMapOf<Long, String>()
        lrc.lines().forEach { line ->
            val matches = timeRegex.findAll(line)
            if (matches.any()) {
                val text = line.substring(matches.last().range.last + 1).trim()
                // 即使 text 为空也记录，可能是间奏
                matches.forEach { match ->
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val msStr = match.groupValues[3]
                    val ms = msStr.toLong().let { 
                        when (msStr.length) {
                            1 -> it * 100
                            2 -> it * 10
                            else -> it
                        }
                    }
                    val time = min * 60000 + sec * 1000 + ms
                    lrcMap[time] = text
                }
            }
        }

        val tlrcMap = mutableMapOf<Long, String>()
        tlrc.lines().forEach { line ->
            val matches = timeRegex.findAll(line)
            if (matches.any()) {
                val text = line.substring(matches.last().range.last + 1).trim()
                if (text.isNotEmpty()) {
                    matches.forEach { match ->
                        val min = match.groupValues[1].toLong()
                        val sec = match.groupValues[2].toLong()
                        val msStr = match.groupValues[3]
                        val ms = msStr.toLong().let { 
                            when (msStr.length) {
                                1 -> it * 100
                                2 -> it * 10
                                else -> it
                            }
                        }
                        val time = min * 60000 + sec * 1000 + ms
                        tlrcMap[time] = text
                    }
                }
            }
        }

        val sortedTimes = lrcMap.keys.sorted()
        sortedTimes.forEach { time ->
            val text = lrcMap[time] ?: ""
            // 过滤掉一些无意义的标签
            if (!text.startsWith("offset:") && !text.startsWith("by:")) {
                lines.add(LyricLine(time, text, tlrcMap[time]))
            }
        }
        
        // 如果解析后还是空的，尝试处理纯文本歌词
        if (lines.isEmpty() && lrc.isNotBlank()) {
            lrc.lines().forEachIndexed { index, s ->
                val cleanLine = s.replace(Regex("\\[.*?\\]"), "").trim()
                if (cleanLine.isNotBlank()) {
                    lines.add(LyricLine(index * 3000L, cleanLine, null))
                }
            }
        }
        
        return lines
    }

    fun toggleFavorite(songId: Long) {
        val currentIds = _favoriteSongs.value.toMutableSet()
        val currentObjects = _favoriteSongList.value.toMutableList()
        
        if (currentIds.contains(songId)) {
            currentIds.remove(songId)
            currentObjects.removeAll { it.id == songId }
        } else {
            currentIds.add(songId)
            // 尝试查找 Song 对象
            val song = _currentSong.value?.takeIf { it.id == songId }
                ?: _searchResults.value.find { it.id == songId }
                ?: _playlist.value.find { it.id == songId }
                ?: _playHistory.value.find { it.id == songId }
            
            if (song != null) {
                currentObjects.add(song)
            }
        }
        _favoriteSongs.value = currentIds
        _favoriteSongList.value = currentObjects
        saveFavorites(currentIds, currentObjects)
    }

    fun togglePlaybackMode() {
        _playbackMode.value = when (_playbackMode.value) {
            PlaybackMode.LIST -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.HEART
            PlaybackMode.HEART -> PlaybackMode.LIST
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
        saveState()
    }

    fun seekTo(position: Long) {
        playerManager.seekTo(position)
    }
}
