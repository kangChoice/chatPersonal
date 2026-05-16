package com.needai.chat.ui.voice.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val DEFAULT_PREVIEW_TEXT = "你好，欢迎试听我的声音，希望你能喜欢。"

/**
 * 支持音色创建（声音克隆）的 CosyVoice 模型列表。
 * 参考阿里云文档：v3.5-flash / v3.5-plus 支持设计音色。
 */
val SUPPORTED_CREATION_MODELS = listOf(
    "cosyvoice-v3.5-flash",
    "cosyvoice-v3.5-plus"
)

@Composable
fun CreateVoiceDialog(
    defaultPrefix: String = "needai",
    onDismiss: () -> Unit,
    onCreate: (targetModel: String, prefix: String, voicePrompt: String, previewText: String) -> Unit
) {
    var selectedModel by remember { mutableStateOf(SUPPORTED_CREATION_MODELS.first()) }
    var prefix by remember { mutableStateOf(defaultPrefix) }
    var voicePrompt by remember { mutableStateOf("") }

    val isValid = voicePrompt.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建自定义音色", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("选择目标模型", style = MaterialTheme.typography.labelLarge)
                SUPPORTED_CREATION_MODELS.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedModel = model }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedModel == model,
                            onClick = { selectedModel = model }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(model, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (model.contains("flash")) "快速合成，成本较低" else "高音质合成，成本较高",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("名称前缀") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("用于 list_voice 按前缀筛选") }
                )
                OutlinedTextField(
                    value = voicePrompt,
                    onValueChange = { voicePrompt = it },
                    label = { Text("声音描述 (Voice Prompt)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("例如：温柔的女声，略带微笑，适合朗读故事") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(selectedModel, prefix, voicePrompt, DEFAULT_PREVIEW_TEXT) },
                enabled = isValid
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
