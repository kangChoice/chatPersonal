package com.needai.chat.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.ModelType

@Composable
fun ModelConfigForm(
    config: ModelConfig,
    onConfigChanged: (ModelConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Model type selection
        Text("模型类型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = config.modelType == ModelType.REMOTE,
                onClick = { onConfigChanged(config.copy(modelType = ModelType.REMOTE)) },
                label = { Text("远程模型") }
            )
            FilterChip(
                selected = config.modelType == ModelType.LOCAL,
                onClick = { onConfigChanged(config.copy(modelType = ModelType.LOCAL)) },
                label = { Text("本地模型 (即将推出)") },
                enabled = false
            )
        }

        if (config.modelType == ModelType.REMOTE) {
            Text("远程模型配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = config.remoteBaseUrl,
                onValueChange = { onConfigChanged(config.copy(remoteBaseUrl = it)) },
                label = { Text("Base URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = config.remoteApiKey,
                onValueChange = { onConfigChanged(config.copy(remoteApiKey = it)) },
                label = { Text("API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = config.remoteModelName,
                onValueChange = { onConfigChanged(config.copy(remoteModelName = it)) },
                label = { Text("模型名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text("本地模型配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = config.localBaseUrl,
                onValueChange = { onConfigChanged(config.copy(localBaseUrl = it)) },
                label = { Text("Ollama Base URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )

            OutlinedTextField(
                value = config.localModelName,
                onValueChange = { onConfigChanged(config.copy(localModelName = it)) },
                label = { Text("本地模型名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
        }

        Divider()
        Text("生成参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = config.temperature.toString(),
            onValueChange = { value ->
                value.toDoubleOrNull()?.let { onConfigChanged(config.copy(temperature = it)) }
            },
            label = { Text("Temperature (0.0 - 2.0)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.maxTokens.toString(),
            onValueChange = { value ->
                value.toIntOrNull()?.let { onConfigChanged(config.copy(maxTokens = it)) }
            },
            label = { Text("Max Tokens") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = config.topP.toString(),
            onValueChange = { value ->
                value.toDoubleOrNull()?.let { onConfigChanged(config.copy(topP = it)) }
            },
            label = { Text("Top P") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
