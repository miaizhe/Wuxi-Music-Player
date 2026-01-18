package com.wuxi.player.ui

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wuxi.player.model.Song

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.*
import com.wuxi.player.model.LyricLine
import com.wuxi.player.model.PlaybackMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerApp(viewModel: MusicViewModel) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val recommendedPlaylists by viewModel.recommendedPlaylists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackMode by viewModel.playbackMode.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val customBackground by viewModel.customBackground.collectAsState()
    val backgroundAlpha by viewModel.backgroundAlpha.collectAsState()
    val overlayAlpha by viewModel.overlayAlpha.collectAsState()
    val playHistory by viewModel.playHistory.collectAsState()
    val favoriteSongList by viewModel.favoriteSongList.collectAsState()
    val lyricColor by viewModel.lyricColor.collectAsState()
    val lyricHighlightColor by viewModel.lyricHighlightColor.collectAsState()
    val soundQuality by viewModel.soundQuality.collectAsState()
    val rotateCover by viewModel.rotateCover.collectAsState()
    val lyricViewHeightRatio by viewModel.lyricViewHeightRatio.collectAsState()
    val downloadPath by viewModel.downloadPath.collectAsState()
    val showNotification by viewModel.showNotification.collectAsState()
    val localSongs by viewModel.localSongs.collectAsState()
    
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showPlaylist by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showFavorites by rememberSaveable { mutableStateOf(false) }
    var showLocalSongs by rememberSaveable { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(with(LocalDensity.current) { (configuration.screenWidthDp.dp * 0.75f).coerceAtMost(320.dp) }),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "菜单",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val items = listOf(
                    Triple("首页", Icons.Default.Home) {
                        showNowPlaying = false
                        showPlaylist = false
                        showSettings = false
                        showFavorites = false
                        showLocalSongs = false
                    },
                    Triple("播放列表", Icons.AutoMirrored.Filled.PlaylistPlay) {
                        showPlaylist = true 
                    },
                    Triple("本地歌曲", Icons.Default.Folder) {
                        showLocalSongs = true 
                    },
                    Triple("我的收藏", Icons.Default.Favorite) {
                        showFavorites = true 
                    },
                    Triple("设置", Icons.Default.Settings) {
                        showSettings = true 
                    }
                )

                items.forEach { (label, icon, action) ->
                    NavigationDrawerItem(
                        label = { Text(label, fontWeight = FontWeight.Medium) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            action()
                        },
                        icon = { Icon(icon, contentDescription = null) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    ) {
        // 统一处理背景 URL（如果是 URL，追加时间戳以支持随机图刷新）
        val processedBackgroundUrl = remember(customBackground) {
            if (customBackground?.startsWith("http") == true) {
                val separator = if (customBackground!!.contains("?")) "&" else "?"
                "$customBackground${separator}t=${System.currentTimeMillis()}"
            } else {
                customBackground
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            // Background Image - 覆盖全屏，包括状态栏和导航栏区域
            if (processedBackgroundUrl != null) {
                AsyncImage(
                    model = processedBackgroundUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = backgroundAlpha
                )
            }

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = { Text("网易云音乐", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                    )
                },
                bottomBar = {
                    if (currentSong != null) {
                        PlaybackControlBar(
                            song = currentSong!!,
                            isPlaying = isPlaying,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onClick = { showNowPlaying = true }
                        )
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Search Bar
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { viewModel.search(searchQuery) }
                        )

                        if (isLoading) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                if (searchQuery.isEmpty() && searchResults.isEmpty()) {
                                    item {
                                        Text(
                                            "推荐歌单",
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        RecommendedPlaylistsRow(
                                            playlists = recommendedPlaylists,
                                            onPlaylistClick = { viewModel.playPlaylist(it.id) }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    if (playHistory.isNotEmpty()) {
                                        item {
                                            Text(
                                                "最近播放",
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        items(playHistory.take(10)) { song ->
                                            SongItem(
                                                song = song,
                                                isCurrent = song.id == currentSong?.id,
                                                isFavorite = favoriteSongs.contains(song.id),
                                                onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                                                onClick = { viewModel.playSong(song) }
                                            )
                                        }
                                        item {
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }

                                items(searchResults) { song ->
                                    SongItem(
                                        song = song,
                                        isCurrent = song.id == currentSong?.id,
                                        isFavorite = favoriteSongs.contains(song.id),
                                        onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                                        onClick = { viewModel.playSong(song) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 全屏叠加层放在 Scaffold 之外，确保不受 Scaffold Padding 影响
            if (showSettings) {
                SettingsDialog(
                    currentBackground = customBackground,
                    backgroundAlpha = backgroundAlpha,
                    overlayAlpha = overlayAlpha,
                    lyricColor = lyricColor,
                    lyricHighlightColor = lyricHighlightColor,
                    soundQuality = soundQuality,
                    rotateCover = rotateCover,
                    lyricViewHeightRatio = lyricViewHeightRatio,
                    downloadPath = downloadPath,
                    showNotification = showNotification,
                    onBackgroundChange = { viewModel.setCustomBackground(it) },
                    onBackgroundAlphaChange = { viewModel.setBackgroundAlpha(it) },
                    onOverlayAlphaChange = { viewModel.setOverlayAlpha(it) },
                    onLyricColorChange = { viewModel.setLyricColor(it) },
                    onLyricHighlightColorChange = { viewModel.setLyricHighlightColor(it) },
                    onSoundQualityChange = { viewModel.setSoundQuality(it) },
                    onRotateCoverChange = { viewModel.setRotateCover(it) },
                    onLyricViewHeightRatioChange = { viewModel.setLyricViewHeightRatio(it) },
                    onDownloadPathChange = { viewModel.setDownloadPath(it) },
                    onShowNotificationChange = { viewModel.setShowNotification(it) },
                    onDismiss = { showSettings = false }
                )
            }

            if (showLocalSongs) {
                ModalBottomSheet(
                    onDismissRequest = { showLocalSongs = false }
                ) {
                    PlaylistSheet(
                        playlist = localSongs,
                        currentSong = currentSong,
                        title = "本地歌曲 (${localSongs.size})",
                        onSongClick = { 
                            viewModel.playSong(it)
                            showLocalSongs = false
                        }
                    )
                }
            }

            if (showFavorites) {
                ModalBottomSheet(
                    onDismissRequest = { showFavorites = false }
                ) {
                    PlaylistSheet(
                        playlist = favoriteSongList,
                        currentSong = currentSong,
                        title = "我的收藏 (${favoriteSongList.size})",
                        onSongClick = { 
                            viewModel.playSong(it)
                            showFavorites = false
                        }
                    )
                }
            }

            if (showNowPlaying && currentSong != null) {
                NowPlayingScreen(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    lyrics = lyrics,
                    currentPosition = currentPosition,
                    duration = duration,
                    playbackMode = playbackMode,
                    isFavorite = favoriteSongs.contains(currentSong!!.id),
                    customBackground = processedBackgroundUrl,
                    backgroundAlpha = backgroundAlpha,
                    overlayAlpha = overlayAlpha,
                    lyricColor = lyricColor,
                    lyricHighlightColor = lyricHighlightColor,
                    rotateCover = rotateCover,
                    lyricViewHeightRatio = lyricViewHeightRatio,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    onSeek = { viewModel.seekTo(it) },
                    onToggleMode = { viewModel.togglePlaybackMode() },
                    onToggleFavorite = { viewModel.toggleFavorite(currentSong!!.id) },
                    onDownload = { viewModel.downloadSong(currentSong!!) },
                    onShowPlaylist = { showPlaylist = true },
                    onDismiss = { showNowPlaying = false }
                )
            }

            if (showPlaylist) {
                ModalBottomSheet(
                    onDismissRequest = { showPlaylist = false },
                    sheetState = rememberModalBottomSheetState()
                ) {
                    PlaylistSheet(
                        playlist = playlist,
                        currentSong = currentSong,
                        onSongClick = { 
                            viewModel.playSong(it)
                            showPlaylist = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistSheet(
    playlist: List<Song>, 
    currentSong: Song?, 
    title: String = "当前播放列表 (${playlist.size})",
    onSongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight(0.6f)) {
        Text(
            title,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        LazyColumn {
            items(playlist) { song ->
                SongItem(
                    song = song,
                    isCurrent = song.id == currentSong?.id,
                    isFavorite = false, // Simplified
                    onFavoriteClick = {},
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("搜索你喜欢的音乐...", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        singleLine = true
    )
}

@Composable
fun SongItem(song: Song, isCurrent: Boolean, isFavorite: Boolean, onFavoriteClick: () -> Unit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.al.picUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.ar.joinToString(", ") { it.name } + " • " + song.al.name,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun PlaybackControlBar(song: Song, isPlaying: Boolean, onTogglePlay: () -> Unit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxSize()
                .blur(if (isPlaying) 0.dp else 0.dp), // 占位，可以根据需要添加模糊
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.al.picUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.ar.firstOrNull()?.name ?: "未知歌手",
                    fontSize = 11.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    lyrics: List<LyricLine>,
    currentPosition: Long,
    duration: Long,
    playbackMode: PlaybackMode,
    isFavorite: Boolean,
    customBackground: String? = null,
    backgroundAlpha: Float = 0.6f,
    overlayAlpha: Float = 0.3f,
    lyricColor: Int = android.graphics.Color.WHITE,
    lyricHighlightColor: Int = android.graphics.Color.GREEN,
    rotateCover: Boolean = true,
    lyricViewHeightRatio: Float = 0.6f,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onShowPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    
    // 使用 interactionSource 消费点击事件，防止穿透到下层
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = interactionSource, indication = null) {}
    ) {
        // 背景：优先使用自定义背景，否则使用高斯模糊封面
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            if (customBackground != null) {
                 AsyncImage(
                    model = customBackground,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (showLyrics) 25.dp else 0.dp), // 增加模糊度
                    contentScale = ContentScale.Crop,
                    alpha = backgroundAlpha
                )
            } else {
                AsyncImage(
                    model = song.al.picUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (showLyrics) 50.dp else 20.dp), // 增加基础模糊度
                    contentScale = ContentScale.Crop,
                    alpha = 0.5f
                )
            }
        }
        
        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back", modifier = Modifier.size(32.dp), tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = song.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.ar.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = { /* Share or more */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main Content: Cover or Lyrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(lyricViewHeightRatio * 10f)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showLyrics = !showLyrics },
                contentAlignment = Alignment.Center
            ) {
                if (!showLyrics) {
                    RotatingCover(song = song, isPlaying = isPlaying && rotateCover)
                } else {
                    LyricView(
                        lyrics = lyrics, 
                        currentPosition = currentPosition,
                        lyricColor = lyricColor,
                        highlightColor = lyricHighlightColor
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Progress Slider
            val sliderInteractionSource = remember { MutableInteractionSource() }
            val isDragged by sliderInteractionSource.collectIsDraggedAsState()
            val isPressed by sliderInteractionSource.collectIsPressedAsState()
            val isDragging = isDragged || isPressed
            
            var sliderPosition by remember { mutableStateOf(currentPosition.toFloat()) }
            var lastSeekTime by remember { mutableLongStateOf(0L) }
            
            // 当不在拖动且不在刚 Seek 完的缓冲期时，同步外部进度
            val isUserInteracting = isDragging || (System.currentTimeMillis() - lastSeekTime < 500)
            
            if (!isUserInteracting) {
                sliderPosition = currentPosition.toFloat()
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Slider(
                    value = sliderPosition,
                    onValueChange = { 
                        sliderPosition = it 
                    },
                    onValueChangeFinished = {
                        lastSeekTime = System.currentTimeMillis()
                        onSeek(sliderPosition.toLong())
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    interactionSource = sliderInteractionSource,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Text(formatTime(duration), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                }
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleMode, modifier = Modifier.size(48.dp)) {
                    val icon = when (playbackMode) {
                        PlaybackMode.LIST -> Icons.Default.Replay
                        PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne
                        PlaybackMode.SHUFFLE -> Icons.Default.Shuffle
                        PlaybackMode.HEART -> Icons.Default.Favorite
                    }
                    Icon(icon, contentDescription = "Mode", modifier = Modifier.size(26.dp), tint = Color.White.copy(alpha = 0.8f))
                }

                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(44.dp), tint = Color.White)
                }
                
                Surface(
                    onClick = onTogglePlay,
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Black
                        )
                    }
                }

                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(44.dp), tint = Color.White)
                }

                IconButton(onClick = onShowPlaylist, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = "Playlist", modifier = Modifier.size(32.dp), tint = Color.White.copy(alpha = 0.8f))
                }
            }

            // Bottom Actions (Like/Download/etc)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { /* Comment */ }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comment", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onDownload, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun RotatingCover(song: Song, isPlaying: Boolean) {
    var rotation by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startTime = System.nanoTime()
            val startRotation = rotation
            while (isPlaying) {
                val elapsed = (System.nanoTime() - startTime) / 1_000_000 // ms
                rotation = (startRotation + (elapsed / 20000f) * 360f) % 360f
                kotlinx.coroutines.delay(16) // ~60fps
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .aspectRatio(1f)
            .padding(16.dp),
        shape = CircleShape,
        color = Color.Black,
        shadowElevation = 20.dp,
        border = androidx.compose.foundation.BorderStroke(10.dp, Color(0xFF111111))
    ) {
        AsyncImage(
            model = song.al.picUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .clip(CircleShape)
                .rotate(rotation),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun RecommendedPlaylistsRow(
    playlists: List<com.wuxi.player.model.PersonalizedPlaylist>,
    onPlaylistClick: (com.wuxi.player.model.PersonalizedPlaylist) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(playlists) { playlist ->
            Surface(
                modifier = Modifier
                    .width(130.dp)
                    .clickable { onPlaylistClick(playlist) },
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                Column {
                    AsyncImage(
                        model = playlist.picUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ColorSettingRow(label: String, colorInt: Int, onColorChange: (Int) -> Unit) {
    val presetColors = listOf(
        android.graphics.Color.WHITE,
        android.graphics.Color.GREEN,
        android.graphics.Color.CYAN,
        android.graphics.Color.YELLOW,
        android.graphics.Color.MAGENTA,
        android.graphics.Color.RED,
        android.graphics.Color.parseColor("#FFD700"), // Gold
        android.graphics.Color.parseColor("#87CEEB")  // SkyBlue
    )
    
    var showColorPicker by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showColorPicker = !showColorPicker }) {
                Text(if (showColorPicker) "收起调色盘" else "打开调色盘")
            }
        }
        
        if (showColorPicker) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // 简单的色相选择器实现
                    Text("选择色调", style = MaterialTheme.typography.labelSmall)
                    val hues = remember { (0..360 step 10).toList() }
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(hues) { hue ->
                            val color = android.graphics.Color.HSVToColor(floatArrayOf(hue.toFloat(), 1f, 1f))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .clickable { onColorChange(color) }
                                    .border(
                                        width = if (android.graphics.Color.red(color) == android.graphics.Color.red(colorInt)) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("选择亮度", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = remember(colorInt) { 
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(colorInt, hsv)
                            hsv[2]
                        },
                        onValueChange = { v ->
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(colorInt, hsv)
                            hsv[2] = v
                            onColorChange(android.graphics.Color.HSVToColor(hsv))
                        },
                        valueRange = 0f..1f
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presetColors) { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(
                            width = if (colorInt == color) 2.dp else 1.dp,
                            color = if (colorInt == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onColorChange(color) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        var text by remember(colorInt) { mutableStateOf("#%06X".format(0xFFFFFF and colorInt)) }
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                if (it.length == 7 && it.startsWith("#")) {
                    try {
                        onColorChange(android.graphics.Color.parseColor(it))
                    } catch (e: Exception) {}
                }
            },
            label = { Text("自定义颜色代码", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun SettingsDialog(
    currentBackground: String?,
    backgroundAlpha: Float,
    overlayAlpha: Float,
    lyricColor: Int,
    lyricHighlightColor: Int,
    soundQuality: String,
    rotateCover: Boolean,
    lyricViewHeightRatio: Float,
    downloadPath: String,
    showNotification: Boolean,
    onBackgroundChange: (String?) -> Unit,
    onBackgroundAlphaChange: (Float) -> Unit,
    onOverlayAlphaChange: (Float) -> Unit,
    onLyricColorChange: (Int) -> Unit,
    onLyricHighlightColorChange: (Int) -> Unit,
    onSoundQualityChange: (String) -> Unit,
    onRotateCoverChange: (Boolean) -> Unit,
    onLyricViewHeightRatioChange: (Float) -> Unit,
    onDownloadPathChange: (String) -> Unit,
    onShowNotificationChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var bgUrlText by remember { mutableStateOf(currentBackground ?: "") }
    var downloadPathText by remember { mutableStateOf(downloadPath) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // 系统设置
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("系统功能", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("通知栏控制台", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "适配 ColorOS 15 / Android 13+ 媒体控件",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = showNotification,
                                onCheckedChange = onShowNotificationChange
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 下载设置
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("下载设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val commonPaths = listOf("Music", "Music/Downloads", "Music/CloudMusic", "Download/Music")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            commonPaths.forEach { path ->
                                FilterChip(
                                    selected = downloadPathText == path,
                                    onClick = { downloadPathText = path },
                                    label = { Text(path, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = downloadPathText,
                            onValueChange = { downloadPathText = it },
                            label = { Text("自定义下载子目录") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            trailingIcon = {
                                if (downloadPathText.isNotEmpty()) {
                                    IconButton(onClick = { downloadPathText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            }
                        )
                        Text(
                            "提示：留空或填写 'Music' 则直接下载到系统音乐目录。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 背景设置
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("界面背景", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bgUrlText,
                            onValueChange = { bgUrlText = it },
                            placeholder = { Text("输入背景图片URL") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("背景透明度: ${(backgroundAlpha * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = backgroundAlpha,
                            onValueChange = onBackgroundAlphaChange,
                            valueRange = 0f..1f
                        )
                        Text("遮罩浓度: ${(overlayAlpha * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = overlayAlpha,
                            onValueChange = onOverlayAlphaChange,
                            valueRange = 0f..1f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // 歌词颜色设置
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("歌词颜色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        ColorSettingRow("普通歌词颜色", lyricColor, onLyricColorChange)
                        Spacer(modifier = Modifier.height(16.dp))
                        ColorSettingRow("高亮歌词颜色", lyricHighlightColor, onLyricHighlightColorChange)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // 个性化设置
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("个性化设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = rotateCover, onCheckedChange = onRotateCoverChange)
                            Text("开启封面旋转动画", style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("播放页中心区域高度比: ${(lyricViewHeightRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = lyricViewHeightRatio,
                            onValueChange = onLyricViewHeightRatioChange,
                            valueRange = 0.3f..0.8f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // 音质设置
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("播放音质", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val qualities = listOf(
                            "standard" to "标准 (128k)",
                            "higher" to "较高 (192k)",
                            "exhigh" to "极高 (320k)",
                            "lossless" to "无损 (FLAC)",
                            "hires" to "Hi-Res"
                        )
                        
                        qualities.forEach { (key, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSoundQualityChange(key) }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = soundQuality == key,
                                    onClick = { onSoundQualityChange(key) }
                                )
                                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { 
                        bgUrlText = ""
                        onBackgroundChange(null)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("恢复默认背景")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onBackgroundChange(if (bgUrlText.isBlank()) null else bgUrlText)
                    onDownloadPathChange(downloadPathText)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun LyricView(
    lyrics: List<LyricLine>, 
    currentPosition: Long, 
    lyricColor: Int, 
    highlightColor: Int
) {
    // 使用 BoxWithConstraints 获取实际可用的容器高度，确保歌词能够精确居中
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerHeight = maxHeight
        val listState = rememberLazyListState()
        
        // 状态管理：当前显示的高亮行索引
        var currentIndex by remember { mutableIntStateOf(0) }
        // 记录上一次的播放进度，用于检测是否发生了 Seek（拖动进度条）
        var lastPosition by remember { mutableLongStateOf(0L) }

        // 当歌词列表变化时（切歌），重置状态
        LaunchedEffect(lyrics) {
            currentIndex = 0
            lastPosition = 0L
            if (lyrics.isNotEmpty()) {
                listState.scrollToItem(0)
            }
        }

        // 核心逻辑：计算当前应该高亮的歌词行
        // 只负责更新 currentIndex，不处理滚动
        LaunchedEffect(currentPosition, lyrics) {
            if (lyrics.isEmpty()) return@LaunchedEffect

            // 1. 计算理论上的当前行（无预判，追求精准同步）
            val rawIndex = lyrics.indexOfLast { it.time <= currentPosition }.coerceAtLeast(0)
            
            // 2. 检测是否发生了 Seek 操作
            // 判定标准：进度突变超过 1 秒，或者进度发生任何显著倒退（超过 200ms）
            // 这样可以确保手动拖动进度条后歌词能立即同步
            val isSeek = kotlin.math.abs(currentPosition - lastPosition) > 1000 || (lastPosition - currentPosition > 200)

            // 3. 更新显示索引
            if (isSeek) {
                // 如果是 Seek，直接跳转到新位置
                currentIndex = rawIndex
            } else {
                // 如果是正常播放，只允许索引递增，防止微小的进度抖动导致歌词回跳
                if (rawIndex > currentIndex) {
                    currentIndex = rawIndex
                }
                // 如果 rawIndex < currentIndex (时间戳微小回退)，保持 currentIndex 不变
            }
            
            // 只有当进度真的在向前走，或者发生了 Seek 时，才更新 lastPosition
            // 这样可以过滤掉偶发的时间戳回退脏数据
            if (currentPosition > lastPosition || isSeek) {
                lastPosition = currentPosition
            }
        }

        // 滚动逻辑：独立于进度更新，只在 currentIndex 真正改变时触发
        LaunchedEffect(currentIndex) {
            if (currentIndex >= 0 && lyrics.isNotEmpty()) {
                // 不再使用偏移量，而是依靠 contentPadding 让 Item 自然居中
                // 这样可以避免手动计算偏移量带来的误差
                try {
                    listState.animateScrollToItem(
                        index = currentIndex,
                        scrollOffset = 0 // 依靠 PaddingValues 将 Item 顶在中间，0 偏移即可
                    )
                } catch (e: Exception) {
                    // 忽略滚动中断异常
                }
            }
        }

        if (lyrics.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无歌词", color = Color.White.copy(alpha = 0.5f))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // 估算一行歌词的大致高度（包含 padding），用于计算居中 Padding
                // 32.sp (lineHeight) + 32.dp (padding vertical) + extra ~ 60-70dp
                val estimatedItemHeight = 64.dp 
                val centerPadding = (containerHeight - estimatedItemHeight) / 2

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true, // 允许手动滑动预览歌词
                    // 关键：设置巨大的首尾 Padding，迫使第一行和最后一行都能滚动到屏幕正中央
                    contentPadding = PaddingValues(top = centerPadding, bottom = centerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(lyrics) { index, line ->
                        val isSelected = index == currentIndex
                        
                        // 动画效果
                        val alpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.5f,
                            animationSpec = tween(durationMillis = 300),
                            label = "alpha"
                        )
                        
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.1f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "scale"
                        )

                        // 颜色过渡 (Deprecated by karaoke effect for selected line, but kept for unselected)
                        val colorAnim by animateColorAsState(
                            targetValue = if (isSelected) Color(highlightColor) else Color(lyricColor).copy(alpha = 0.6f),
                            animationSpec = tween(300), 
                            label = "color"
                        )

                        // Karaoke Effect Logic
                        val textStyle = if (isSelected) {
                            // 计算下一行的时间，如果没有下一行，假设当前行持续 3 秒
                            val nextLineTime = lyrics.getOrNull(index + 1)?.time ?: (line.time + 3000)
                            // 限制单行歌词高亮的最大持续时间为 15 秒，避免间奏时高亮过慢
                            val effectiveDuration = (nextLineTime - line.time).coerceIn(100, 15000)
                            val progress = ((currentPosition - line.time).toFloat() / effectiveDuration).coerceIn(0f, 1f)
                            
                            // Use Brush for gradient highlight
                            TextStyle(
                                brush = Brush.horizontalGradient(
                                    0.0f to Color(highlightColor),
                                    progress to Color(highlightColor),
                                    progress + 0.05f to Color(lyricColor), // 增加过渡宽度，使视觉更柔和
                                    1.0f to Color(lyricColor)
                                )
                            )
                        } else {
                            TextStyle(color = colorAnim)
                        }

                        Column(
                            modifier = Modifier
                                .padding(vertical = 16.dp, horizontal = 32.dp)
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.titleLarge.merge(textStyle),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 32.sp
                            )
                            if (!line.translation.isNullOrBlank()) {
                                Text(
                                    text = line.translation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(lyricColor).copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
                
                // 顶部和底部渐变遮罩 (进一步调淡，几乎不可见，仅用于辅助边缘虚化)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent)))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f))))
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

