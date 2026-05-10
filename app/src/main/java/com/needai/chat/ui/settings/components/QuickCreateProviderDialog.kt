package com.needai.chat.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.needai.chat.domain.model.KnownProvider
import com.needai.chat.domain.model.ModelConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCreateProviderDialog(
    onDismiss: () -> Unit,
    onSave: (ModelConfig) -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0 = pick provider, 1 = enter details
    var selectedProvider by remember { mutableStateOf<KnownProvider?>(null) }
    var configName by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }

    if (step == 0) {
        // Step 1: Pick provider
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("选择厂商", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(com.needai.chat.domain.model.knownProviders) { provider ->
                        Surface(
                            onClick = {
                                selectedProvider = provider
                                configName = provider.displayName
                                modelName = provider.defaultModelName
                                step = 1
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = provider.defaultBaseUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    } else if (step == 1 && selectedProvider != null) {
        // Step 2: Enter API Key and Model Name
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("配置 ${selectedProvider!!.displayName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "协议: ${selectedProvider!!.protocol.value.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Base URL: ${selectedProvider!!.defaultBaseUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    OutlinedTextField(
                        value = configName,
                        onValueChange = { configName = it },
                        label = { Text("配置名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("模型名称") },
                        placeholder = { Text(selectedProvider!!.defaultModelName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSave(ModelConfig(
                            name = configName.ifBlank { selectedProvider!!.displayName },
                            protocol = selectedProvider!!.protocol,
                            remoteBaseUrl = selectedProvider!!.defaultBaseUrl,
                            remoteApiKey = apiKey,
                            remoteModelName = modelName.ifBlank { selectedProvider!!.defaultModelName }
                        ))
                    },
                    enabled = apiKey.isNotBlank()
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { step = 0 }) {
                    Text("返回")
                }
            }
        )
    }
}
