package com.needai.chat.ui.chat.components

import android.graphics.BitmapFactory
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.needai.chat.domain.model.Skill
import com.needai.chat.data.remote.tts.SystemVoiceProvider
import com.needai.chat.ui.theme.*
import com.needai.chat.util.AvatarUtils
import java.io.File

@Composable
fun SkillCarousel(
    skills: List<Skill>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    onSkillSelected: (Skill) -> Unit,
    voiceNameMap: Map<String, String> = emptyMap(),
    unreadCounts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    if (skills.isEmpty()) return
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // ============ Header ============
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "NeedAI",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        brush = Brush.linearGradient(
                            listOf(BrandBlue, BrandPink, BrandMint)
                        )
                    )
                )
                Text(
                    text = "需要爱",
                    fontSize = 12.sp,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            listOf(BrandBlue, BrandPink, BrandMint)
                        )
                    ),
                    modifier = Modifier.padding(start = 6.dp, bottom = 3.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ============ Accordion Stack ============
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy((-20).dp)
        ) {
            skills.forEachIndexed { index, skill ->
                val isExpanded = index == selectedIndex
                val cardHeight by animateDpAsState(
                    targetValue = if (isExpanded) 288.dp else 80.dp,
                    animationSpec = tween(500),
                    label = "cardHeight"
                )

                val overlayColors = listOf(
                    listOf(BrandMint.copy(alpha = 0.5f), BrandMint.copy(alpha = 0.05f)),
                    listOf(BrandBlue.copy(alpha = 0.5f), BrandBlue.copy(alpha = 0.05f)),
                    listOf(BrandPink.copy(alpha = 0.5f), BrandPink.copy(alpha = 0.05f))
                )
                val overlayBrush = Brush.verticalGradient(
                    overlayColors[index % overlayColors.size]
                )

                // 加载头像
                val avatarBitmap = remember(skill.avatarPath, skill.id) {
                    val path = if (skill.avatarPath.isNotBlank()) {
                        val f = File(skill.avatarPath)
                        if (f.exists()) skill.avatarPath
                        else AvatarUtils.getDefaultAvatarPath(context)
                    } else {
                        AvatarUtils.getDefaultAvatarPath(context)
                    }
                    try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .animateContentSize(tween(500))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isExpanded) {
                                onSkillSelected(skill)
                            } else {
                                onSelectedIndexChanged(index)
                            }
                        }
                ) {
                    // 头像背景图
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(32.dp)),
                            contentScale = ContentScale.Crop,
                            alpha = 1.0f
                        )
                    }

                    // Card background gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.05f),
                                        Color.White.copy(alpha = 0.0f)
                                    )
                                ),
                                RoundedCornerShape(32.dp)
                            )
                            .border(
                                0.5.dp,
                                Color.White.copy(alpha = 0.5f),
                                RoundedCornerShape(32.dp)
                            )
                    )

                    // Colored overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayBrush, RoundedCornerShape(32.dp))
                    )

                    // Card content at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = skill.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(6.dp))
                                if (skill.voiceId.isNotBlank()) {
                                    val voiceDisplayName = voiceNameMap[skill.voiceId]
                                        ?: SystemVoiceProvider.getAllSystemVoices()
                                            .find { it.voiceId == skill.voiceId }?.displayName
                                        ?: skill.name
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color.White.copy(alpha = 0.7f),
                                                CircleShape
                                            )
                                            .padding(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "voice: $voiceDisplayName",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF2BAF8A),
                                            letterSpacing = 0.4.sp
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                Color(0xFFFFE0B2).copy(alpha = 0.85f),
                                                CircleShape
                                            )
                                            .padding(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "voice: 未配置！",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100),
                                            letterSpacing = 0.4.sp
                                        )
                                    }
                                }
                                if (isExpanded && skill.description.isNotBlank()) {
                                    Text(
                                        text = skill.description,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Unread badge
                    val unreadCount = unreadCounts[skill.id] ?: 0
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .then(
                                    if (unreadCount > 9) Modifier.width(26.dp).height(20.dp)
                                    else Modifier.size(20.dp)
                                )
                                .clip(CircleShape)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = Color.White,
                                fontSize = if (unreadCount > 9) 10.sp else 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
