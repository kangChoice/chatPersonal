package com.needai.chat.ui.voice.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.needai.chat.domain.model.VoiceInfo

@Composable
fun EditVoiceDialog(
    voice: VoiceInfo,
    onDismiss: () -> Unit,
    onSave: (VoiceInfo) -> Unit
) {
    var displayName by remember { mutableStateOf(voice.displayName) }
    var voicePrompt by remember { mutableStateOf(voice.voicePrompt) }
    var targetModel by remember { mutableStateOf(voice.targetModel) }
    var previewText by remember { mutableStateOf(voice.previewText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑音色", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("展示名称") },
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
                    value = previewText,
                    onValueChange = { previewText = it },
                    label = { Text("预览文本") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetModel,
                    onValueChange = { targetModel = it },
                    label = { Text("目标模型") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(voice.copy(
                    displayName = displayName,
                    voicePrompt = voicePrompt,
                    targetModel = targetModel,
                    previewText = previewText
                ))
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
