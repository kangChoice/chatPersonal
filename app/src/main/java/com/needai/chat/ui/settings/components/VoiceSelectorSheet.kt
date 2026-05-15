package com.needai.chat.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.needai.chat.domain.model.SystemVoice
import com.needai.chat.domain.model.VoiceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSelectorSheet(
    systemVoices: List<SystemVoice>,
    customVoices: List<VoiceInfo>,
    currentVoiceId: String,
    selectedModel: String,
    onVoiceSelected: (String) -> Unit,
    onManageVoices: () -> Unit,
    onDismiss: () -> Unit
) {
    val isV35 = selectedModel.startsWith("cosyvoice-v3.5")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "选择音色",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // "无音色" option
            Surface(
                onClick = { onVoiceSelected("") },
                shape = MaterialTheme.shapes.medium,
                color = if (currentVoiceId == "") MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentVoiceId == "",
                        onClick = { onVoiceSelected("") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "无音色（不使用 TTS）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (systemVoices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "系统音色",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                if (isV35) {
                    Text(
                        text = "v3.5 模型主要使用设计音色，以下为兼容的系统音色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                systemVoices.forEach { voice ->
                    Surface(
                        onClick = { onVoiceSelected(voice.voiceId) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (currentVoiceId == voice.voiceId) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentVoiceId == voice.voiceId,
                                onClick = { onVoiceSelected(voice.voiceId) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${voice.displayName} (${voice.voiceId})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = voice.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            if (customVoices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isV35) "设计音色" else "自定义音色",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                if (isV35 && systemVoices.isEmpty()) {
                    Text(
                        text = "尚未创建设计音色，请先在「管理音色」中同步或创建",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                customVoices.forEach { voice ->
                    Surface(
                        onClick = { onVoiceSelected(voice.voiceId) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (currentVoiceId == voice.voiceId) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentVoiceId == voice.voiceId,
                                onClick = { onVoiceSelected(voice.voiceId) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = voice.displayName.ifEmpty { voice.voiceId },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (voice.voicePrompt.isNotBlank()) {
                                    Text(
                                        text = voice.voicePrompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 2
                                    )
                                }
                                if (voice.targetModel.isNotBlank()) {
                                    Text(
                                        text = voice.targetModel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (systemVoices.isEmpty() && customVoices.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "暂无可用音色，请先「管理音色」中添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onManageVoices,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("管理音色")
            }
        }
    }
}
