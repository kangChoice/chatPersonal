package com.needai.chat

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.needai.chat.app.AppNotificationHelper
import com.needai.chat.data.ilink.IlinkAuthManager
import com.needai.chat.data.ilink.IlinkBridgeService
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.util.FileLogger
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.ui.navigation.MainScreen
import com.needai.chat.ui.navigation.NavigationCommands
import com.needai.chat.ui.navigation.Screen
import com.needai.chat.ui.theme.NeedAiTheme
import com.needai.chat.ui.util.AppToastHost
import com.needai.chat.ui.util.LocalToast
import com.needai.chat.ui.util.ToastState
import com.needai.chat.ui.util.ToastType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var authManager: IlinkAuthManager
    @Inject lateinit var skillRepository: SkillRepository

    private val toastState = ToastState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        setContent {
            val isDarkMode by settingsDataStore.isDarkMode.collectAsState(initial = false)
            NeedAiTheme(darkTheme = isDarkMode) {
                val bgColor = MaterialTheme.colorScheme.background
                SideEffect {
                    window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor.toArgb()))
                }
                CompositionLocalProvider(LocalToast provides toastState) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = bgColor
                        ) {
                            MainScreen(isDark = isDarkMode)
                        }
                        AppToastHost()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val skillId = intent?.getStringExtra(AppNotificationHelper.EXTRA_SKILL_ID) ?: return
        FileLogger.i("MainActivity", "handleNotificationIntent: skillId=$skillId")
        lifecycleScope.launch {
            skillRepository.setSelectedSkillId(skillId)
            NavigationCommands.navigate(Screen.Chat.route)
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
        toastState.show("微信ClawBot已连接", ToastType.Success)
    }

    private fun isBridgeServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == IlinkBridgeService::class.java.name }
    }
}
