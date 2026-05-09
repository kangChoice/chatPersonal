package com.needai.chat.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.needai.chat.ui.chat.ChatScreen
import com.needai.chat.ui.settings.SettingsScreen
import com.needai.chat.ui.skills.SkillEditScreen
import com.needai.chat.ui.skills.SkillListScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Chat : Screen("chat", "聊天", Icons.Default.Chat)
    data object SkillList : Screen("skill_list", "技能", Icons.Default.AutoAwesome)
    data object SkillEdit : Screen("skill_edit/{skillId}", "编辑技能")
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)

    companion object {
        fun skillEdit(skillId: String) = "skill_edit/$skillId"
    }
}

val bottomNavItems = listOf(Screen.Chat, Screen.SkillList, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

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
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Chat.route) {
                ChatScreen(navController = navController)
            }
            composable(Screen.SkillList.route) {
                SkillListScreen(navController = navController)
            }
            composable(Screen.SkillEdit.route) { backStackEntry ->
                val skillId = backStackEntry.arguments?.getString("skillId") ?: ""
                SkillEditScreen(
                    skillId = skillId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}
