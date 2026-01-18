package com.wuxi.player.model

data class SearchResponse(
    val result: SearchResult?
)

data class SearchResult(
    val songs: List<Song>?
)

data class Song(
    val id: Long,
    val name: String,
    val ar: List<Artist>,
    val al: Album,
    val dt: Long = 0, // 歌曲时长
    val localPath: String? = null // 本地文件路径，如果不为空则播放本地文件
)

data class Artist(
    val id: Long,
    val name: String
)

data class Album(
    val id: Long,
    val name: String,
    val picUrl: String?
)

data class SongUrlResponse(
    val data: List<SongUrl>
)

data class SongUrl(
    val id: Long,
    val url: String?
)

data class LyricResponse(
    val lrc: LyricData?,
    val tlyric: LyricData?,
    val ylrc: LyricData? // 逐字歌词通常在 ylrc 或 klyric
)

data class LyricData(
    val lyric: String?
)

data class LyricLine(
    val time: Long,
    val text: String,
    val translation: String? = null
)

data class PersonalizedResponse(
    val result: List<PersonalizedPlaylist>?
)

data class PersonalizedPlaylist(
    val id: Long,
    val name: String,
    val picUrl: String,
    val playCount: Long
)

data class PlaylistDetailResponse(
    val playlist: PlaylistDetail?
)

data class PlaylistDetail(
    val id: Long,
    val name: String,
    val tracks: List<Song>?
)

enum class PlaybackMode {
    LIST,       // 列表循环
    REPEAT_ONE, // 单曲循环
    SHUFFLE,    // 随机播放
    HEART       // 心动模式/喜欢播放 (从收藏中播放)
}
