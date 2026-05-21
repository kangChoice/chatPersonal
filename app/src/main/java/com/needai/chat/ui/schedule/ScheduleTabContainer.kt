package com.needai.chat.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.needai.chat.ui.theme.BgPage
import com.needai.chat.ui.theme.BrandBlue
import com.needai.chat.ui.theme.TextPrimary
import com.needai.chat.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTabContainer() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("微信定时", "AI通知")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定时任务") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPage)
            )
        },
        containerColor = BgPage
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab 切换
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BgPage,
                contentColor = BrandBlue,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> IlinkScheduleScreen()
                1 -> AiNotificationScreen()
            }
        }
    }
}
