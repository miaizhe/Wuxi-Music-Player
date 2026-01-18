package com.wuxi.player.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.wuxi.player.MainActivity
import com.wuxi.player.R
import com.wuxi.player.WuxiApplication

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "default_channel_id" // 使用 Media3 默认的渠道 ID
        const val NOTIFICATION_ID = 1001
        const val CUSTOM_COMMAND_FAVORITE = "ACTION_FAVORITE"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val playerManager = (application as WuxiApplication).playerManager
        val player = playerManager.getExoPlayer()
        
        // 创建点击通知时跳转到 Activity 的 PendingIntent
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .setId("WuxiPlayerSession")
            .build()
            
        // 确保 Session 处于活跃状态
        // 在 Media3 中通常不需要手动设置，但某些系统适配需要显式声明
        // mediaSession?.isActive = true // Media3 没有直接的 isActive setter，是通过 Player 状态管理的
        
        // 监听播放状态，确保在暂停时也尽量保持服务活跃
        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // 当播放状态改变时，显式更新通知
            }
        })

        // 自定义通知提供者
        val notificationProvider = object : DefaultMediaNotificationProvider(this) {
            override fun getMediaButtons(
                session: MediaSession,
                playerCommands: Player.Commands,
                customLayout: ImmutableList<CommandButton>,
                showCustomButtonsOnKeyguard: Boolean
            ): ImmutableList<CommandButton> {
                // ... (保持原有按钮逻辑)
                val builder = ImmutableList.builder<CommandButton>()
                // 上一首
                builder.add(CommandButton.Builder().setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS).setIconResId(android.R.drawable.ic_media_previous).setDisplayName("上一首").build())
                // 播放/暂停
                builder.add(CommandButton.Builder().setPlayerCommand(Player.COMMAND_PLAY_PAUSE).setIconResId(if (session.player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play).setDisplayName("播放/暂停").build())
                // 下一首
                builder.add(CommandButton.Builder().setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT).setIconResId(android.R.drawable.ic_media_next).setDisplayName("下一首").build())
                // 收藏
                val isFavorite = playerManager.playlistCallback?.isFavorite() ?: false
                builder.add(CommandButton.Builder().setSessionCommand(SessionCommand(CUSTOM_COMMAND_FAVORITE, Bundle.EMPTY)).setIconResId(if (isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off).setDisplayName("收藏").setEnabled(true).build())
                return builder.build()
            }
        }
        
        notificationProvider.setSmallIcon(R.drawable.ic_music_note)
        setMediaNotificationProvider(notificationProvider)
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_FAVORITE, Bundle.EMPTY))
                .build()
            
            val playerCommands = Player.Commands.Builder()
                .addAllCommands()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .build()
                
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CUSTOM_COMMAND_FAVORITE) {
                val playerManager = (application as WuxiApplication).playerManager
                playerManager.playlistCallback?.onToggleFavorite()
                // 刷新通知以更新收藏图标
                mediaSession?.setCustomLayout(ImmutableList.of()) 
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, Bundle.EMPTY))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            val playerManager = (application as WuxiApplication).playerManager
            when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT -> {
                    playerManager.playlistCallback?.onSkipToNext()
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_SEEK_TO_PREVIOUS -> {
                    playerManager.playlistCallback?.onSkipToPrevious()
                    return SessionResult.RESULT_SUCCESS
                }
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "音乐播放"
            val descriptionText = "用于显示正在播放的音乐"
            val importance = NotificationManager.IMPORTANCE_HIGH // 提升优先级以触发流体云
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                // ColorOS 特有：设置声音和振动为无，避免干扰
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && (player.playWhenReady || player.isPlaying)) {
            // 如果正在播放，绝对不能停止服务
            return
        }
        // 当应用从最近任务中移除时，如果不处于播放状态，我们也尽量保留服务一段时间
        // 这样可以防止控制中心控件立即消失
        // super.onTaskRemoved(rootIntent) // 不要调用 super，避免默认的停止逻辑
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
