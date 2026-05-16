package com.needai.chat.ui.voice.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.needai.chat.domain.model.VoiceInfo

@Composable
fun VoiceCard(
    voice: VoiceInfo,
    alias: String = "",
    isPlaying: Boolean = false,
    canPlay: Boolean = true,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onAliasEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor = when (voice.status) {
        "OK" -> MaterialTheme.colorScheme.primary
        "DEPLOYING" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    val statusText = when (voice.status) {
        "OK" -> "可用"
        "DEPLOYING" -> "部署中"
        "UNDEPLOYED" -> "未部署"
        else -> voice.status.ifEmpty { "未知" }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Row 1: Alias/Name
                Text(
                    text = alias.ifEmpty { voice.displayName.ifEmpty { voice.voiceId } },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (alias.isNotBlank()) FontWeight.Bold else FontWeight.Medium
                )
                // Row 2: Voice prompt
                if (voice.voicePrompt.isNotBlank()) {
                    Text(
                        text = voice.voicePrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                // Row 3: Model info
                Text(
                    text = "模型: ${voice.targetModel.ifEmpty { "未知" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Row 4: Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "状态: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (onAliasEdit != null) {
                IconButton(onClick = onAliasEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑别名",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onPlay, enabled = canPlay) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "停止" else "试听",
                    tint = if (isPlaying) MaterialTheme.colorScheme.error
                           else if (canPlay) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
