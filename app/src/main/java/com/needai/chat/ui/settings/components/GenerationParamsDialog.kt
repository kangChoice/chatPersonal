package com.needai.chat.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.needai.chat.domain.model.ModelConfig

private const val TEMP_MIN = 0.0
private const val TEMP_MAX = 2.0
private const val TOKENS_MIN = 1
private const val TOKENS_MAX = 128000
private const val TOP_P_MIN = 0.0
private const val TOP_P_MAX = 1.0

@Composable
fun GenerationParamsDialog(
    currentConfig: ModelConfig,
    onDismiss: () -> Unit,
    onSave: (ModelConfig) -> Unit,
    onPreviewChanged: ((ModelConfig) -> Unit)? = null
) {
    var temperature by remember { mutableStateOf(currentConfig.temperature.toString()) }
    var maxTokens by remember { mutableStateOf(currentConfig.maxTokens.toString()) }
    var topP by remember { mutableStateOf(currentConfig.topP.toString()) }

    fun buildPreviewConfig(): ModelConfig {
        val temp = temperature.toDoubleOrNull()?.coerceIn(TEMP_MIN, TEMP_MAX) ?: currentConfig.temperature
        val tokens = maxTokens.toIntOrNull()?.coerceIn(TOKENS_MIN, TOKENS_MAX) ?: currentConfig.maxTokens
        val p = topP.toDoubleOrNull()?.coerceIn(TOP_P_MIN, TOP_P_MAX) ?: currentConfig.topP
        return currentConfig.copy(temperature = temp, maxTokens = tokens, topP = p)
    }

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
                    onValueChange = {
                        temperature = it
                        it.toDoubleOrNull()?.let { v ->
                            onPreviewChanged?.invoke(buildPreviewConfig())
                        }
                    },
                    label = { Text("Temperature (0.0 - 2.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = temperature.isNotEmpty() && temperature.toDoubleOrNull() == null,
                    supportingText = {
                        val v = temperature.toDoubleOrNull()
                        when {
                            v != null && (v < TEMP_MIN || v > TEMP_MAX) -> Text("超出范围，将限制到 $TEMP_MIN-$TEMP_MAX")
                            v == null && temperature.isNotEmpty() -> Text("请输入有效数字")
                        }
                    }
                )
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = {
                        maxTokens = it
                        it.toIntOrNull()?.let { v ->
                            onPreviewChanged?.invoke(buildPreviewConfig())
                        }
                    },
                    label = { Text("Max Tokens ($TOKENS_MIN - $TOKENS_MAX)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = maxTokens.isNotEmpty() && maxTokens.toIntOrNull() == null,
                    supportingText = {
                        val v = maxTokens.toIntOrNull()
                        when {
                            v != null && (v < TOKENS_MIN || v > TOKENS_MAX) -> Text("超出范围，将限制到 $TOKENS_MIN-$TOKENS_MAX")
                            v == null && maxTokens.isNotEmpty() -> Text("请输入有效整数")
                        }
                    }
                )
                OutlinedTextField(
                    value = topP,
                    onValueChange = {
                        topP = it
                        it.toDoubleOrNull()?.let { v ->
                            onPreviewChanged?.invoke(buildPreviewConfig())
                        }
                    },
                    label = { Text("Top P ($TOP_P_MIN - $TOP_P_MAX)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = topP.isNotEmpty() && topP.toDoubleOrNull() == null,
                    supportingText = {
                        val v = topP.toDoubleOrNull()
                        when {
                            v != null && (v < TOP_P_MIN || v > TOP_P_MAX) -> Text("超出范围，将限制到 $TOP_P_MIN-$TOP_P_MAX")
                            v == null && topP.isNotEmpty() -> Text("请输入有效数字")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(buildPreviewConfig())
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
