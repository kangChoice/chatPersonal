package com.needai.chat.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.needai.chat.ui.theme.BrandBlue
import com.needai.chat.ui.theme.TextTertiary

@Composable
fun ScheduleTabContainer(
    onNavigateToIlinkSetup: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("微信定时", "AI通知")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 药丸风格 Tab 切换 — 底色透明，透出底层 FluidGlowBackground
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            tabs.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selectedTab == index) BrandBlue.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == index) BrandBlue else TextTertiary
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> IlinkScheduleScreen(onNavigateToIlinkSetup = onNavigateToIlinkSetup)
            1 -> AiNotificationScreen()
        }
    }
}
