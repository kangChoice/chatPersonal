package com.needai.chat.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.needai.chat.ui.theme.*

/**
 * 精确对应 index.html 的 .chat-input-section + .chat-input-wrap + .quick-actions
 * - 胶囊输入框 rgba(255,255,255,0.7) + blur(24px)
 * - 发送按钮 #5B9DFF 渐变
 * - 快速操作栏：送礼 / 动作 / 语音（默认显示）
 */
@Composable
fun ChatInputBar(
    inputText: String,
    isStreaming: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkBg = MaterialTheme.colorScheme.background == DarkBg
    val borderColor = remember(isDarkBg) {
        if (isDarkBg) Color.White.copy(alpha = 0.35f) else Color(0xFF718096).copy(alpha = 0.45f)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // === .chat-input-wrap ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(GlassInput)
                .border(0.5.dp, borderColor, RoundedCornerShape(999.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Input — <input placeholder="输入消息..." />
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                    ),
                    cursorBrush = SolidColor(BrandBlue),
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputText.isEmpty()) {
                                Text(
                                    "输入消息...",
                                    fontSize = 12.sp,
                                    color = TextTertiary
                                )
                            }
                            innerTextField()
                        }
                    },
                    singleLine = false,
                    maxLines = 5
                )

                // Send / Stop button — .send-btn
                val sendBrush = if (isStreaming) SolidColor(BrandPink)
                    else if (inputText.isNotBlank()) Brush.linearGradient(listOf(BrandMint, BrandBlue))
                    else SolidColor(TextTertiary.copy(alpha = 0.3f))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(sendBrush)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isStreaming || inputText.isNotBlank()
                        ) {
                            if (isStreaming) onStop() else onSend()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isStreaming) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "停止",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text("↑", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionBtn(
    emoji: String,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable { /* quick action */ }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}
