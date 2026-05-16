package com.needai.chat.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.needai.chat.data.remote.tts.SystemVoiceProvider

@Composable
fun TtsSettingsSection(
    ttsProvider: String,
    onTtsProviderChange: (String) -> Unit,
    ttsApiKey: String,
    onTtsApiKeyChange: (String) -> Unit,
    ttsVoice: String,
    onTtsVoiceChange: (String) -> Unit,
    ttsVolume: Int,
    onTtsVolumeChange: (Int) -> Unit,
    ttsRate: Float,
    onTtsRateChange: (Float) -> Unit,
    ttsPitch: Float,
    onTtsPitchChange: (Float) -> Unit,
    ttsAutoRead: Boolean,
    onTtsAutoReadChange: (Boolean) -> Unit,
    onManageVoices: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showApiKey by remember { mutableStateOf(false) }
    val isCosyVoice = ttsProvider == "cosyvoice"

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "语音合成",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 朗读引擎
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("朗读引擎", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !isCosyVoice,
                        onClick = { if (isCosyVoice) onTtsProviderChange("system") },
                        label = { Text("系统默认") },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    FilterChip(
                        selected = isCosyVoice,
                        onClick = { if (!isCosyVoice) onTtsProviderChange("cosyvoice") },
                        label = { Text("CosyVoice") }
                    )
                }
            }

            if (isCosyVoice) {
                Spacer(modifier = Modifier.height(8.dp))

                // API Key
                OutlinedTextField(
                    value = ttsApiKey,
                    onValueChange = onTtsApiKeyChange,
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showApiKey = !showApiKey }) {
                            Text(if (showApiKey) "隐藏" else "显示")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 音色管理
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("朗读音色", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        Text(
                            text = ttsVoice.ifEmpty { "未选择" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        TextButton(onClick = onManageVoices) {
                            Text("管理")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 语速
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("语速", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = ttsRate,
                    onValueChange = onTtsRateChange,
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "%.1f".format(ttsRate),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(32.dp)
                )
            }

            // 音量
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("音量", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = ttsVolume.toFloat(),
                    onValueChange = { onTtsVolumeChange(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$ttsVolume",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(32.dp)
                )
            }

            // 音高
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("音高", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = ttsPitch,
                    onValueChange = onTtsPitchChange,
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "%.1f".format(ttsPitch),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(32.dp)
                )
            }

            // 自动朗读
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("回复后自动朗读", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "AI 回复完成后自动播放语音",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Switch(
                    checked = ttsAutoRead,
                    onCheckedChange = onTtsAutoReadChange
                )
            }
        }
    }

}
