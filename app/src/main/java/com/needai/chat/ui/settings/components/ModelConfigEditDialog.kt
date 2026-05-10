package com.needai.chat.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.needai.chat.domain.model.ApiProtocol
import com.needai.chat.domain.model.ModelConfig

@Composable
fun ModelConfigEditDialog(
    currentConfig: ModelConfig,
    onDismiss: () -> Unit,
    onSave: (ModelConfig) -> Unit
) {
    var protocol by remember { mutableStateOf(currentConfig.protocol) }
    var baseUrl by remember { mutableStateOf(currentConfig.remoteBaseUrl) }
    var apiKey by remember { mutableStateOf(currentConfig.remoteApiKey) }
    var modelName by remember { mutableStateOf(currentConfig.remoteModelName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("编辑模型配置", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Protocol selector
                Text("协议", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = protocol == ApiProtocol.OPENAI,
                        onClick = { protocol = ApiProtocol.OPENAI },
                        label = { Text("OpenAI") }
                    )
                    FilterChip(
                        selected = protocol == ApiProtocol.ANTHROPIC,
                        onClick = { protocol = ApiProtocol.ANTHROPIC },
                        label = { Text("Anthropic") }
                    )
                }

                Text(
                    text = if (protocol == ApiProtocol.OPENAI)
                        "请求路径: POST {baseUrl}/v1/chat/completions"
                    else
                        "请求路径: POST {baseUrl}/v1/messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("https://api.example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )

                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称") },
                    placeholder = { if (protocol == ApiProtocol.OPENAI) Text("deepseek-v4-flash") else Text("claude-sonnet-4-20250514") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(currentConfig.copy(
                    protocol = protocol,
                    remoteBaseUrl = baseUrl,
                    remoteApiKey = apiKey,
                    remoteModelName = modelName
                ))
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
