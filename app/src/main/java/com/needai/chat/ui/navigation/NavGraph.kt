package com.needai.chat.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.ui.chat.ChatScreen
import com.needai.chat.ui.ilink.IlinkSetupScreen
import com.needai.chat.ui.ilink.IlinkStatusScreen
import com.needai.chat.ui.schedule.ScheduleTabContainer
import com.needai.chat.ui.multichat.MultiChatScreen
import com.needai.chat.ui.onboarding.OnboardingOverlay
import com.needai.chat.ui.settings.SettingsScreen
import com.needai.chat.ui.skills.SkillEditScreen
import com.needai.chat.ui.skills.SkillAndVoiceScreen
import com.needai.chat.ui.prompt.PolishScreen
import com.needai.chat.ui.stats.StatsScreen
import com.needai.chat.ui.voice.VoiceListScreen
import com.needai.chat.ui.voicechat.VoiceChatScreen
import com.needai.chat.ui.theme.BgPage
import com.needai.chat.ui.theme.JellyTabBar
import com.needai.chat.ui.theme.JellyTabItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Chat : Screen("chat", "聊天", Icons.Default.Chat)
    data object MultiChat : Screen("multi_chat", "群聊", Icons.Default.Forum)
    data object SkillList : Screen("skill_list", "技能管理", Icons.Default.AutoAwesome)
    data object PromptPolish : Screen("prompt_polish", "提示词优化", Icons.Default.Edit)
    data object IlinkSchedule : Screen("ilink_schedule", "定时任务", Icons.Default.Schedule)
    data object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    data object SkillEdit : Screen("skill_edit/{skillId}", "编辑角色")
    data object VoiceManagement : Screen("voice_management", "音色管理")
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
    data object VoiceChat : Screen("voice_chat/{skillId}", "语音通话") {
        fun createRoute(skillId: String = "") = "voice_chat/$skillId"
    }
    data object IlinkSetup : Screen("ilink_setup", "接入微信")
    data object IlinkStatus : Screen("ilink_status", "ClawBot管理")

    companion object {
        fun skillEdit(skillId: String) = "skill_edit/$skillId"
    }
}

val bottomNavItems = listOf(Screen.Chat, Screen.MultiChat, Screen.SkillList, Screen.PromptPolish, Screen.IlinkSchedule, Screen.Settings)

private val onboardingRoutes = listOf(
    Screen.Chat.route,          // step 0: 聊天
    Screen.MultiChat.route,     // step 1: 群聊
    Screen.SkillList.route,     // step 2: 技能管理
    Screen.PromptPolish.route,  // step 3: 提示词优化
    Screen.Settings.route       // step 4: 设置
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(isDark: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var isChatDetail by remember { mutableStateOf(false) }
    var isMultiChatDetail by remember { mutableStateOf(false) }
    val showBottomBar = when (currentDestination?.route) {
        Screen.Chat.route -> !isChatDetail
        Screen.MultiChat.route -> !isMultiChatDetail
        else -> bottomNavItems.any { screen ->
            currentDestination?.hierarchy?.any { it.route == screen.route } == true
        }
    }

    // Current selected tab index
    val selectedTabIndex = remember(currentDestination) {
        bottomNavItems.indexOfFirst { screen ->
            currentDestination?.hierarchy?.any { it.route == screen.route } == true
        }.coerceAtLeast(0)
    }

    // Onboarding state
    var showOnboarding by remember { mutableStateOf(false) }
    var onboardingStep by remember { mutableIntStateOf(-1) }

    // Background image state (全屏背景，仅聊天详情页生效)
    val bgContext = LocalContext.current
    val bgSettingsDataStore = remember { SettingsDataStore(bgContext) }
    val bgList by bgSettingsDataStore.backgrounds.collectAsState(initial = emptyList())
    val bgSelectedId by bgSettingsDataStore.selectedBackgroundId.collectAsState(initial = "")
    val selectedBg = remember(bgList, bgSelectedId) {
        bgList.find { it.id == bgSelectedId }
    }
    var bgBitmap by remember(selectedBg) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(selectedBg) {
        bgBitmap = withContext(Dispatchers.IO) {
            selectedBg?.imagePath?.let { path ->
                try { android.graphics.BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
            }
        }
    }

    // 系统返回手势：非聊天 tab → 回到聊天页
    BackHandler(enabled = currentDestination?.route != Screen.Chat.route) {
        navController.navigate(Screen.Chat.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Auto-navigate when onboarding step changes
    LaunchedEffect(onboardingStep) {
        if (showOnboarding && onboardingStep >= 0 && onboardingStep in onboardingRoutes.indices) {
            val targetRoute = onboardingRoutes[onboardingStep]
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgPage)) {
        // 根级动态波动底色，确保页面切换时始终显示
        com.needai.chat.ui.theme.FluidGlowBackground(isDark = isDark)

        // 全屏聊天背景图（仅聊天详情页，渲染在 Scaffold 之后以覆盖全屏）
        if (isChatDetail && bgBitmap != null) {
            Image(
                bitmap = bgBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(4.dp),
                contentScale = ContentScale.Crop,
                alpha = 1.0f
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.85f)
                        )
                    )
                )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    JellyTabBar(
                        tabs = bottomNavItems.map { screen ->
                            JellyTabItem(
                                title = screen.title,
                                icon = screen.icon!!,
                                route = screen.route
                            )
                        },
                        selectedIndex = selectedTabIndex,
                        onTabSelected = { index ->
                            val screen = bottomNavItems[index]
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Chat.route
                ) {
                    composable(Screen.Chat.route) {
                        ChatScreen(
                            navController = navController,
                            onChatDetailChange = { isChatDetail = it }
                        )
                    }
                    composable(Screen.MultiChat.route) {
                        MultiChatScreen(
                            onChatDetailChange = { isMultiChatDetail = it }
                        )
                    }
                    composable(Screen.SkillList.route) {
                        SkillAndVoiceScreen(navController = navController)
                    }
                    composable(Screen.PromptPolish.route) {
                        PolishScreen()
                    }
                    composable(Screen.Stats.route) {
                        StatsScreen()
                    }
                    composable(Screen.SkillEdit.route) { backStackEntry ->
                        val skillId = backStackEntry.arguments?.getString("skillId") ?: ""
                        SkillEditScreen(
                            skillId = skillId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.VoiceManagement.route) {
                        VoiceListScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            navController = navController,
                            onStartOnboarding = {
                                onboardingStep = 0
                                showOnboarding = true
                            }
                        )
                    }
                    composable(Screen.VoiceChat.route) { backStackEntry ->
                        val skillId = backStackEntry.arguments?.getString("skillId") ?: ""
                        VoiceChatScreen(
                            skillId = skillId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.IlinkSchedule.route) {
                        ScheduleTabContainer()
                    }
                    composable(Screen.IlinkSetup.route) {
                        IlinkSetupScreen(
                            onComplete = {
                                navController.navigate(Screen.IlinkStatus.route) {
                                    popUpTo(Screen.IlinkSetup.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.IlinkStatus.route) {
                        IlinkStatusScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        // Onboarding overlay
        if (showOnboarding) {
            OnboardingOverlay(
                step = onboardingStep,
                onNext = {
                    onboardingStep++
                },
                onFinish = {
                    showOnboarding = false
                    onboardingStep = -1
                }
            )
        }
    }

}
