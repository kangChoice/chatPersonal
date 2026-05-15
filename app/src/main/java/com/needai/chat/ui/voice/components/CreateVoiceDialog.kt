package com.needai.chat.ui.voice.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CreateVoiceDialog(
    defaultTargetModel: String = "cosyvoice-v3.5-flash",
    defaultPrefix: String = "needai",
    onDismiss: () -> Unit,
    onCreate: (targetModel: String, prefix: String, voicePrompt: String, previewText: String) -> Unit
) {
    var targetModel by remember { mutableStateOf(defaultTargetModel) }
    var prefix by remember { mutableStateOf(defaultPrefix) }
    var voicePrompt by remember { mutableStateOf("") }
    var previewText by remember { mutableStateOf("") }

    val isValid = voicePrompt.isNotBlank() && previewText.length >= 15

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建自定义音色", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = targetModel,
                    onValueChange = { targetModel = it },
                    label = { Text("目标模型") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                OutlinedTextField(
                    value = previewText,
                    onValueChange = { previewText = it },
                    label = { Text("预览文本") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("创建成功后用于试听的文本（至少 15 个字符）") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(targetModel, prefix, voicePrompt, previewText) },
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

@Composable
fun SimpleCreateVoiceDialog(
    onDismiss: () -> Unit,
    onCreate: (voiceId: String, voicePrompt: String, targetModel: String, previewText: String) -> Unit
) {
    var voiceId by remember { mutableStateOf("") }
    var voicePrompt by remember { mutableStateOf("") }
    var targetModel by remember { mutableStateOf("cosyvoice-v3.5-flash") }
    var previewText by remember { mutableStateOf("") }

    val isValid = voiceId.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增音色", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = voiceId,
                    onValueChange = { voiceId = it },
                    label = { Text("音色 ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = voicePrompt,
                    onValueChange = { voicePrompt = it },
                    label = { Text("声音描述") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetModel,
                    onValueChange = { targetModel = it },
                    label = { Text("目标模型") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = previewText,
                    onValueChange = { previewText = it },
                    label = { Text("预览文本") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(voiceId, voicePrompt, targetModel, previewText) },
                enabled = isValid
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
