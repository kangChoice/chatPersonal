package com.needai.chat.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.needai.chat.domain.model.ModelConfig

@Composable
fun GenerationParamsDialog(
    currentConfig: ModelConfig,
    onDismiss: () -> Unit,
    onSave: (ModelConfig) -> Unit
) {
    var temperature by remember { mutableStateOf(currentConfig.temperature.toString()) }
    var maxTokens by remember { mutableStateOf(currentConfig.maxTokens.toString()) }
    var topP by remember { mutableStateOf(currentConfig.topP.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("生成参数", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("Temperature (0.0 - 2.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { maxTokens = it },
                    label = { Text("Max Tokens") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = topP,
                    onValueChange = { topP = it },
                    label = { Text("Top P") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(currentConfig.copy(
                    temperature = temperature.toDoubleOrNull() ?: currentConfig.temperature,
                    maxTokens = maxTokens.toIntOrNull() ?: currentConfig.maxTokens,
                    topP = topP.toDoubleOrNull() ?: currentConfig.topP
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
