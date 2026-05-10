package com.needai.chat.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.needai.chat.R
import com.needai.chat.domain.model.ApiProtocol
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.ui.settings.components.GenerationParamsDialog
import com.needai.chat.ui.settings.components.ModelConfigEditDialog
import com.needai.chat.ui.settings.components.QuickCreateProviderDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val modelConfig by viewModel.modelConfig.collectAsStateWithLifecycle()
    val configs by viewModel.configs.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddChoice by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showQuickCreate by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val exportConfigLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = com.needai.chat.data.export.ExportUtils.generateModelConfigJson(modelConfig)
            com.needai.chat.data.export.ExportUtils.writeToUri(context, uri, json)
        }
    }
    val importConfigLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = com.needai.chat.data.import.ImportUtils.readFromUri(context, uri)
            if (json != null) {
                viewModel.importModelConfig(json) { success, msg ->
                    scope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("读取文件失败")
                }
            }
        }
    }
    var editConfig by remember { mutableStateOf<ModelConfig?>(null) }
    var configToDelete by remember { mutableStateOf<ModelConfig?>(null) }
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
                .verticalScroll(rememberScrollState())
        ) {
            // 模型配置列表
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "模型配置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (modelConfig.id.isNotEmpty() && !modelConfig.isBuiltin) {
                                FilledTonalIconButton(onClick = {
                                    val fileName = "model_config_${modelConfig.name.ifEmpty { "default" }.replace(" ", "_")}.json"
                                    exportConfigLauncher.launch(fileName)
                                }) {
                                    Icon(Icons.Default.FileDownload, contentDescription = "导出配置")
                                }
                            }
                            FilledTonalIconButton(onClick = {
                                importConfigLauncher.launch(arrayOf("application/json"))
                            }) {
                                Icon(Icons.Default.FileUpload, contentDescription = "导入配置")
                            }
                            FilledTonalIconButton(onClick = { showAddChoice = true }) {
                                Icon(Icons.Default.Add, contentDescription = "添加配置")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (configs.isEmpty()) {
                        Text(
                            text = "暂无配置，点击 + 添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        configs.forEach { config ->
                            ModelConfigItem(
                                config = config,
                                isSelected = config.id == modelConfig.id,
                                onSelect = { viewModel.selectConfig(config.id) },
                                onEdit = { editConfig = config },
                                onDelete = { configToDelete = config }
                            )
                        }
                    }

                    if (modelConfig.id.isNotEmpty()) {
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
                            if (!modelConfig.isBuiltin) {
                                FilledTonalIconButton(onClick = { showGenParamsDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑生成参数")
                                }
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 显示模式
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("显示模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isDarkMode) "暗黑模式" else "明亮模式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { uriHandler.openUri("https://github.com/kangChoice/chatPersonal") }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "哥只是个传说~，叫哥哥",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "欢迎Star power by",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = "GitHub",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    if (showAddChoice) {
        AlertDialog(
            onDismissRequest = { showAddChoice = false },
            title = { Text("新建模型配置", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "选择创建方式：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            showAddChoice = false
                            showQuickCreate = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("快速创建（选择供应商）")
                    }
                    OutlinedButton(
                        onClick = {
                            showAddChoice = false
                            showAddDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("手动创建（自定义配置）")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddChoice = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showQuickCreate) {
        QuickCreateProviderDialog(
            onDismiss = { showQuickCreate = false },
            onSave = { config ->
                viewModel.addConfig(config)
                showQuickCreate = false
            }
        )
    }

    if (configToDelete != null) {
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除配置") },
            text = {
                Text("确定要删除「${configToDelete!!.name.ifEmpty { configToDelete!!.remoteModelName }}」吗？此操作不可撤销。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConfig(configToDelete!!.id)
                        configToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { configToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showAddDialog) {
        ModelConfigEditDialog(
            currentConfig = ModelConfig(),
            onDismiss = { showAddDialog = false },
            onSave = { newConfig ->
                viewModel.addConfig(newConfig)
                showAddDialog = false
            }
        )
    }

    if (editConfig != null) {
        ModelConfigEditDialog(
            currentConfig = editConfig!!,
            onDismiss = { editConfig = null },
            onSave = { newConfig ->
                viewModel.updateConfig(newConfig)
                editConfig = null
            }
        )
    }

    if (showGenParamsDialog) {
        GenerationParamsDialog(
            currentConfig = modelConfig,
            onDismiss = { showGenParamsDialog = false },
            onSave = { newConfig ->
                viewModel.updateConfig(newConfig)
                showGenParamsDialog = false
            }
        )
    }
}

@Composable
private fun ModelConfigItem(
    config: ModelConfig,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val showActions = !config.isBuiltin
    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name.ifEmpty { config.remoteModelName.ifEmpty { "未命名配置" } },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${config.protocol.value.uppercase()} · ${config.remoteModelName.ifEmpty { "未设置模型" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (showActions) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, contentDescription = "删除",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
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
