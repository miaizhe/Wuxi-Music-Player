package com.wuxi.player

import android.app.Application
import com.wuxi.player.player.PlayerManager

class WuxiApplication : Application() {
    lateinit var playerManager: PlayerManager
        private set

    override fun onCreate() {
        super.onCreate()
        playerManager = PlayerManager(this)
    }
}
