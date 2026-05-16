package com.needai.chat.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.needai.chat.ui.chat.ChatScreen
import com.needai.chat.ui.multichat.MultiChatScreen
import com.needai.chat.ui.onboarding.OnboardingOverlay
import com.needai.chat.ui.settings.SettingsScreen
import com.needai.chat.ui.skills.SkillEditScreen
import com.needai.chat.ui.skills.SkillAndVoiceScreen
import com.needai.chat.ui.prompt.PolishScreen
import com.needai.chat.ui.stats.StatsScreen
import com.needai.chat.ui.voice.VoiceListScreen
import com.needai.chat.ui.voicechat.VoiceChatScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Chat : Screen("chat", "聊天", Icons.Default.Chat)
    data object MultiChat : Screen("multi_chat", "群聊", Icons.Default.Forum)
    data object SkillList : Screen("skill_list", "技能管理", Icons.Default.AutoAwesome)
    data object PromptPolish : Screen("prompt_polish", "提示词", Icons.Default.Edit)
    data object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    data object SkillEdit : Screen("skill_edit/{skillId}", "编辑角色")
    data object VoiceManagement : Screen("voice_management", "音色管理")
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
    data object VoiceChat : Screen("voice_chat", "语音通话")

    companion object {
        fun skillEdit(skillId: String) = "skill_edit/$skillId"
    }
}

val bottomNavItems = listOf(Screen.Chat, Screen.MultiChat, Screen.SkillList, Screen.PromptPolish, Screen.Settings)

private val onboardingRoutes = listOf(
    Screen.Chat.route,          // step 0
    Screen.PromptPolish.route,  // step 1
    Screen.SkillList.route,     // step 2
    Screen.Settings.route       // step 3
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    // Onboarding state
    var showOnboarding by remember { mutableStateOf(false) }
    var onboardingStep by remember { mutableIntStateOf(-1) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
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
                        ChatScreen(navController = navController)
                    }
                    composable(Screen.MultiChat.route) {
                        MultiChatScreen()
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
                    composable(Screen.VoiceChat.route) {
                        VoiceChatScreen(
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
