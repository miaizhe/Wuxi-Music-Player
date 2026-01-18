package com.wuxi.player.api

import com.wuxi.player.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApi {
    @GET("cloudsearch")
    suspend fun search(@Query("keywords") keywords: String): SearchResponse

    @GET("song/url/v1")
    suspend fun getSongUrl(
        @Query("id") id: Long,
        @Query("level") level: String = "standard"
    ): SongUrlResponse

    @GET("lyric")
    suspend fun getLyric(@Query("id") id: Long): LyricResponse

    @GET("personalized")
    suspend fun getPersonalized(@Query("limit") limit: Int = 10): PersonalizedResponse

    @GET("playlist/detail")
    suspend fun getPlaylistDetail(@Query("id") id: Long): PlaylistDetailResponse

    companion object {
        const val BASE_URL = "https://163api.qijieya.cn/"
    }
}
