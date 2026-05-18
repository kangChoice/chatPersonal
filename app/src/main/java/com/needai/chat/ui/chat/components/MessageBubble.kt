package com.needai.chat.ui.chat.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.MessageRole
import com.needai.chat.ui.theme.*

/**
 * 精确对应 index.html 的 .msg-bubble:
 *   .ai-msg — 暗色毛玻璃 rgba(0,0,0,0.6) + blur(12px), 左上 4px 圆角
 *   .user-msg — 渐变 135deg #88E2CE→#5B9DFF, 右上 4px 圆角, 呼吸发光
 */
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
    fontSize: Float = 16f,
    onSpeak: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    isSpeaking: Boolean = false,
    isAutoSpeaking: Boolean = false,
    skillAvatarBitmap: ImageBitmap? = null,
    userAvatarBitmap: ImageBitmap? = null
) {
    val isUser = message.role == MessageRole.USER

    // 用户气泡呼吸发光 — 对应 @keyframes bubbleBreath (3s infinite)
    val glowInfinite = rememberInfiniteTransition()
    val glowAlpha by glowInfinite.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // 消息行 — 对应 .msg-row
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // AI 头像
            if (!isUser) {
                if (skillAvatarBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GlassWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = skillAvatarBitmap,
                            contentDescription = "角色头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .then(
                        if (isUser) {
                            // .msg-bubble.user-msg
                            Modifier
                                .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                                .background(Brush.linearGradient(listOf(BubbleUserStart, BubbleUserEnd)))
                                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                        } else {
                            // .msg-bubble.ai-msg
                            Modifier
                                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                                .background(BubbleAiBg)
                                .border(0.5.dp, BubbleAiBorder, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (isUser) Color.White else Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 19.2.sp    // line-height 1.6
                )
            }

            // 用户头像（始终显示占位圆形，有图显示图，无图显示图标）
            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GlassWhite),
                    contentAlignment = Alignment.Center
                ) {
                    if (userAvatarBitmap != null) {
                        Image(
                            bitmap = userAvatarBitmap,
                            contentDescription = "用户头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "用户头像",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // TTS 朗读 + 复制按钮（AI 消息专属）
        if (!isUser) {
            val clipboard = LocalClipboardManager.current
            val isDark = MaterialTheme.colorScheme.background == DarkBg
            val btnBg = if (isDark) Color.White.copy(alpha = 0.15f)
                        else Color.Black.copy(alpha = 0.08f)
            val btnTint = if (isDark) Color.White.copy(alpha = 0.6f)
                          else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)

            Row(
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 复制按钮
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(btnBg),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(message.content))
                        onCopy?.invoke()
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "复制",
                            tint = btnTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 朗读按钮
                if (onSpeak != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(btnBg),
                        contentAlignment = Alignment.Center
                    ) {
                        val showStop = isSpeaking || isAutoSpeaking
                        IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (showStop) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (showStop) "停止" else "朗读",
                                tint = btnTint,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * StreamingBubble — 流式输出气泡，样式匹配 .msg-bubble.ai-msg
 */
@Composable
fun StreamingBubble(
    content: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    fontSize: Float = 16f,
    skillAvatarBitmap: ImageBitmap? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            // AI 头像
            if (skillAvatarBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GlassWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = skillAvatarBitmap,
                        contentDescription = "角色头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(BubbleAiBg)
                    .border(0.5.dp, BubbleAiBorder, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                StreamingText(
                    text = content,
                    isStreaming = isStreaming,
                    fontSize = fontSize
                )
            }
        }
    }
}
