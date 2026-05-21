package com.needai.chat

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.needai.chat.data.ilink.IlinkAuthManager
import com.needai.chat.data.ilink.IlinkBridgeService
import com.needai.chat.util.FileLogger
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.ui.navigation.MainScreen
import com.needai.chat.ui.theme.NeedAiTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var authManager: IlinkAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by settingsDataStore.isDarkMode.collectAsState(initial = false)
            NeedAiTheme(darkTheme = isDarkMode) {
                val bgColor = MaterialTheme.colorScheme.background
                SideEffect {
                    window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor.toArgb()))
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = bgColor
                ) {
                    MainScreen(isDark = isDarkMode)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            tryConnectBridge()
        }
    }

    private suspend fun tryConnectBridge() {
        if (!authManager.isAuthenticated()) {
            FileLogger.i("MainActivity", "tryConnectBridge: 未授权，跳过")
            return
        }
        if (isBridgeServiceRunning()) {
            FileLogger.i("MainActivity", "tryConnectBridge: Service 已在运行")
            return
        }

        FileLogger.i("MainActivity", "tryConnectBridge: 启动桥接")
        val intent = Intent(this, IlinkBridgeService::class.java).apply {
            action = IlinkBridgeService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "ClawBot已连接", Toast.LENGTH_SHORT).show()
    }

    private fun isBridgeServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == IlinkBridgeService::class.java.name }
    }
}
