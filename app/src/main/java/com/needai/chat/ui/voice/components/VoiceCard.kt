package com.needai.chat.ui.voice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.ui.theme.*

@Composable
fun VoiceCard(
    voice: VoiceInfo,
    alias: String = "",
    boundSkills: List<Skill> = emptyList(),
    isPlaying: Boolean = false,
    canPlay: Boolean = true,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onAliasEdit: (() -> Unit)? = null,
    onEditBindings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor = when (voice.status) {
        "OK" -> BrandMint
        "DEPLOYING" -> BrandBlue
        else -> TextTertiary
    }
    val statusText = when (voice.status) {
        "OK" -> "可用"
        "DEPLOYING" -> "部署中"
        "UNDEPLOYED" -> "未部署"
        else -> voice.status.ifEmpty { "未知" }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isPlaying) BrandMint.copy(alpha = 0.1f) else GlassWhite)
            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alias.ifEmpty { voice.displayName.ifEmpty { voice.voiceId } },
                    fontSize = 15.sp,
                    fontWeight = if (alias.isNotBlank()) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary
                )
                if (voice.voicePrompt.isNotBlank()) {
                    Text(
                        text = voice.voicePrompt,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = "模型: ${voice.targetModel.ifEmpty { "未知" }}",
                    fontSize = 10.sp,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "状态: ",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (boundSkills.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(boundSkills.take(5), key = { it.id }) { skill ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(BrandMint.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = skill.name,
                                    fontSize = 10.sp,
                                    color = BrandMint,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (boundSkills.size > 5) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(GlassWhite)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "+${boundSkills.size - 5}",
                                        fontSize = 10.sp,
                                        color = TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Edit menu
            if (onAliasEdit != null || onEditBindings != null) {
                var showEditMenu by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { showEditMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "编辑",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showEditMenu,
                        onDismissRequest = { showEditMenu = false }
                    ) {
                        if (onAliasEdit != null) {
                            DropdownMenuItem(
                                text = { Text("编辑别名") },
                                onClick = {
                                    showEditMenu = false
                                    onAliasEdit()
                                }
                            )
                        }
                        if (onEditBindings != null) {
                            DropdownMenuItem(
                                text = { Text("编辑绑定角色") },
                                onClick = {
                                    showEditMenu = false
                                    onEditBindings()
                                }
                            )
                        }
                    }
                }
            }

            // Play button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isPlaying) BrandPink.copy(alpha = 0.2f) else GlassWhite)
                    .clickable(enabled = canPlay) { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "停止" else "试听",
                    tint = if (isPlaying) BrandPink
                           else if (canPlay) BrandBlue
                           else TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = BrandPink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
