package com.needai.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.needai.chat.ui.settings.components.GenerationParamsDialog
import com.needai.chat.ui.settings.components.ModelConfigEditDialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val modelConfig by viewModel.modelConfig.collectAsStateWithLifecycle()
    val chatFontSize by viewModel.chatFontSize.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showModelConfigDialog by remember { mutableStateOf(false) }
    var showGenParamsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("配置已保存")
            viewModel.dismissSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 聊天字体大小
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "聊天字体大小",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("小", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${chatFontSize.roundToInt()}sp",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("大", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = chatFontSize,
                        onValueChange = { viewModel.updateChatFontSize(it) },
                        valueRange = 12f..24f,
                        steps = 11
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 模型配置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "连接配置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalIconButton(onClick = { showModelConfigDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "编辑连接配置")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ModelConfigSummary(label = "协议", value = modelConfig.protocol.value.uppercase())
                        ModelConfigSummary(label = "Base URL", value = modelConfig.remoteBaseUrl.ifEmpty { "未设置" })
                        ModelConfigSummary(label = "模型名称", value = modelConfig.remoteModelName.ifEmpty { "未设置" })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "生成参数",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalIconButton(onClick = { showGenParamsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "编辑生成参数")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ModelConfigSummary(label = "Temperature", value = modelConfig.temperature.toString())
                        ModelConfigSummary(label = "Max Tokens", value = modelConfig.maxTokens.toString())
                        ModelConfigSummary(label = "Top P", value = modelConfig.topP.toString())
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Need AI Chat v1.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "基于 Kotlin + Jetpack Compose 构建",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    if (showModelConfigDialog) {
        ModelConfigEditDialog(
            currentConfig = modelConfig,
            onDismiss = { showModelConfigDialog = false },
            onSave = { newConfig ->
                viewModel.saveModelConfigDirectly(newConfig)
                showModelConfigDialog = false
            }
        )
    }

    if (showGenParamsDialog) {
        GenerationParamsDialog(
            currentConfig = modelConfig,
            onDismiss = { showGenParamsDialog = false },
            onSave = { newConfig ->
                viewModel.saveModelConfigDirectly(newConfig)
                showGenParamsDialog = false
            }
        )
    }
}

@Composable
private fun ModelConfigSummary(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
