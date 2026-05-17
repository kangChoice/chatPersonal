package com.needai.chat.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class JellyTabItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

/**
 * 精确对应 index.html 的 .jelly-tab-bar
 * - 毛玻璃胶囊背景 rgba(255,255,255,0.8) + blur
 * - 边框 1px rgba(255,255,255,0.5)
 * - 激活 tab: color #5B9DFF, jelly 弹性动画
 * - 激活指示线: 三色渐变 90deg #5B9DFF → #FFAEC9 → #88E2CE
 */
@Composable
fun JellyTabBar(
    tabs: List<JellyTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Tab items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                JellyTabIcon(
                    icon = tab.icon,
                    title = tab.title,
                    isActive = index == selectedIndex,
                    onClick = { if (index != selectedIndex) onTabSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun JellyTabIcon(
    icon: ImageVector,
    title: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            // @keyframes jelly: 1→1.25→0.75→1.15→0.95→1
            scale.animateTo(1.25f, tween(150, easing = LinearEasing))
            scale.animateTo(0.75f, tween(100, easing = LinearEasing))
            scale.animateTo(1.15f, tween(100, easing = LinearEasing))
            scale.animateTo(0.95f, tween(80, easing = LinearEasing))
            scale.animateTo(1.0f, tween(80, easing = LinearEasing))
        }
    }

    Column(
        modifier = Modifier
            .width(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) BrandBlue else TextTertiary,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            color = if (isActive) BrandBlue else TextTertiary.copy(alpha = 0.7f)
        )
    }
}
