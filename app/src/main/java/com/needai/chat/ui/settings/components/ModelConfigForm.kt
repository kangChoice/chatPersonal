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

private const val TEMP_MIN = 0.0
private const val TEMP_MAX = 2.0
private const val TOKENS_MIN = 1
private const val TOKENS_MAX = 128000
private const val TOP_P_MIN = 0.0
private const val TOP_P_MAX = 1.0

@Composable
fun ModelConfigForm(
    config: ModelConfig,
    onConfigChanged: (ModelConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local text state to avoid snap-back during typing
    var tempText by remember { mutableStateOf(config.temperature.toString()) }
    var tokensText by remember { mutableStateOf(config.maxTokens.toString()) }
    var topPText by remember { mutableStateOf(config.topP.toString()) }

    // Sync from external config changes
    LaunchedEffect(config.temperature) { tempText = config.temperature.toString() }
    LaunchedEffect(config.maxTokens) { tokensText = config.maxTokens.toString() }
    LaunchedEffect(config.topP) { topPText = config.topP.toString() }

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
            value = tempText,
            onValueChange = { value ->
                tempText = value
                value.toDoubleOrNull()?.let { v ->
                    val clamped = v.coerceIn(TEMP_MIN, TEMP_MAX)
                    onConfigChanged(config.copy(temperature = clamped))
                }
            },
            label = { Text("Temperature (0.0 - 2.0)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = tempText.isNotEmpty() && tempText.toDoubleOrNull() == null,
            supportingText = {
                val value = tempText.toDoubleOrNull()
                if (value != null && (value < TEMP_MIN || value > TEMP_MAX)) {
                    Text("超出范围，已自动限制到 ${TEMP_MIN}-${TEMP_MAX}")
                } else if (value == null && tempText.isNotEmpty()) {
                    Text("请输入有效数字")
                }
            }
        )

        OutlinedTextField(
            value = tokensText,
            onValueChange = { value ->
                tokensText = value
                value.toIntOrNull()?.let { v ->
                    val clamped = v.coerceIn(TOKENS_MIN, TOKENS_MAX)
                    onConfigChanged(config.copy(maxTokens = clamped))
                }
            },
            label = { Text("Max Tokens ($TOKENS_MIN - $TOKENS_MAX)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = tokensText.isNotEmpty() && tokensText.toIntOrNull() == null,
            supportingText = {
                val value = tokensText.toIntOrNull()
                if (value != null && (value < TOKENS_MIN || value > TOKENS_MAX)) {
                    Text("超出范围，已自动限制到 $TOKENS_MIN-$TOKENS_MAX")
                } else if (value == null && tokensText.isNotEmpty()) {
                    Text("请输入有效整数")
                }
            }
        )

        OutlinedTextField(
            value = topPText,
            onValueChange = { value ->
                topPText = value
                value.toDoubleOrNull()?.let { v ->
                    val clamped = v.coerceIn(TOP_P_MIN, TOP_P_MAX)
                    onConfigChanged(config.copy(topP = clamped))
                }
            },
            label = { Text("Top P ($TOP_P_MIN - $TOP_P_MAX)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = topPText.isNotEmpty() && topPText.toDoubleOrNull() == null,
            supportingText = {
                val value = topPText.toDoubleOrNull()
                if (value != null && (value < TOP_P_MIN || value > TOP_P_MAX)) {
                    Text("超出范围，已自动限制到 $TOP_P_MIN-$TOP_P_MAX")
                } else if (value == null && topPText.isNotEmpty()) {
                    Text("请输入有效数字")
                }
            }
        )
    }
}
