package com.wuxi.player

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.wuxi.player.player.PlayerManager
import com.wuxi.player.ui.MusicPlayerApp
import com.wuxi.player.ui.MusicViewModel

import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 申请通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // 启用沉浸式 Edge-to-Edge 模式
        enableEdgeToEdge()
        
        val playerManager = (application as WuxiApplication).playerManager
        val viewModel = ViewModelProvider(this, MusicViewModel.Factory(playerManager, this))[MusicViewModel::class.java]
        
        // 确保播放服务已启动，以激活 MediaSession 和流体云
        playerManager.startService()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Transparent
                ) {
                    MusicPlayerApp(viewModel)
                }
            }
        }
    }
}
