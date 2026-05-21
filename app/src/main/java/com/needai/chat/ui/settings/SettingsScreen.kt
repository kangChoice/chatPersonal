package com.needai.chat.ui.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.needai.chat.R
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.domain.model.ApiProtocol
import com.needai.chat.domain.model.BackgroundConfig
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.ui.settings.components.GenerationParamsDialog
import com.needai.chat.ui.settings.components.ModelConfigEditDialog
import com.needai.chat.ui.settings.components.QuickCreateProviderDialog
import com.needai.chat.ui.settings.components.TtsSettingsSection
import com.needai.chat.util.AvatarUtils
import com.needai.chat.util.FileLogger
import com.needai.chat.ui.navigation.Screen
import com.needai.chat.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onStartOnboarding: () -> Unit = {},
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

    val bgSettingsDataStore = remember { SettingsDataStore(context) }
    val backgroundList by bgSettingsDataStore.backgrounds.collectAsState(initial = emptyList())
    val selectedBgId by bgSettingsDataStore.selectedBackgroundId.collectAsState(initial = "")
    var showBackgroundNameDialog by remember { mutableStateOf(false) }
    var pendingBackgroundUri by remember { mutableStateOf<Uri?>(null) }
    var pendingBackgroundName by remember { mutableStateOf("") }
    var bgToDelete by remember { mutableStateOf<BackgroundConfig?>(null) }

    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingBackgroundUri = uri
            pendingBackgroundName = ""
            showBackgroundNameDialog = true
        }
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text("设置", fontWeight = FontWeight.Bold)
            }
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

            // ============ 个人配置 ============
            val userAvatarDataStore = remember { SettingsDataStore(context) }
            val customAvatarFile = java.io.File(AvatarUtils.getUserAvatarPath(context))
            val hasCustomAvatar = customAvatarFile.exists()
            val initialAvatarPath = if (hasCustomAvatar) customAvatarFile.absolutePath
                                    else AvatarUtils.getDefaultUserAvatarPath(context)
            var avatarBitmap by remember { mutableStateOf(try { BitmapFactory.decodeFile(initialAvatarPath) } catch (_: Exception) { null }) }

            val userAvatarPickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    val savedPath = AvatarUtils.saveUserAvatar(context, uri)
                    if (savedPath != null) {
                        scope.launch { userAvatarDataStore.incrementUserAvatarVersion() }
                        avatarBitmap = try { BitmapFactory.decodeFile(savedPath) } catch (_: Exception) { null }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "个人配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { userAvatarPickerLauncher.launch("image/*") }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GlassWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            val bm = avatarBitmap
                            if (bm != null) {
                                Image(
                                    bitmap = bm.asImageBitmap(),
                                    contentDescription = "用户头像",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "设置头像",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("本人头像", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text(
                                text = if (hasCustomAvatar) "点击更换头像" else "点击设置头像",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TTS 配置
            val ttsApiKey by viewModel.ttsApiKey.collectAsStateWithLifecycle()
            val ttsVolume by viewModel.ttsVolume.collectAsStateWithLifecycle()
            val ttsRate by viewModel.ttsRate.collectAsStateWithLifecycle()
            val ttsPitch by viewModel.ttsPitch.collectAsStateWithLifecycle()
            val ttsAutoRead by viewModel.ttsAutoRead.collectAsStateWithLifecycle()

            TtsSettingsSection(
                ttsApiKey = ttsApiKey,
                onTtsApiKeyChange = viewModel::setTtsApiKey,
                ttsVolume = ttsVolume,
                onTtsVolumeChange = viewModel::setTtsVolume,
                ttsRate = ttsRate,
                onTtsRateChange = viewModel::setTtsRate,
                ttsPitch = ttsPitch,
                onTtsPitchChange = viewModel::setTtsPitch,
                ttsAutoRead = ttsAutoRead,
                onTtsAutoReadChange = viewModel::setTtsAutoRead
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 微信 ClawBot
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(Screen.IlinkSetup.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("微信 ClawBot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "将角色接入微信，在微信中与 AI 角色对话",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "微信 ClawBot",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 语音通话
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(Screen.VoiceChat.createRoute()) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("语音通话", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "语音对话，像对讲机一样自然交流",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "语音通话",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 聊天背景
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "聊天背景",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalIconButton(onClick = {
                            backgroundPickerLauncher.launch("image/*")
                        }) {
                            Icon(Icons.Default.Image, contentDescription = "添加背景")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (backgroundList.isEmpty()) {
                        Text(
                            text = "暂无背景，点击图片图标添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        backgroundList.forEach { bg ->
                            val isSelected = bg.id == selectedBgId
                            val bitmap = remember(bg.imagePath) {
                                try {
                                    BitmapFactory.decodeFile(bg.imagePath)
                                } catch (e: Exception) { null }
                            }
                            Surface(
                                onClick = {
                                    scope.launch {
                                        bgSettingsDataStore.setSelectedBackgroundId(
                                            if (isSelected) "" else bg.id
                                        )
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .heightIn(min = 48.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(MaterialTheme.shapes.small),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            modifier = Modifier.size(48.dp),
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Image,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bg.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (isSelected) "当前使用中" else "点击选择",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    IconButton(onClick = { bgToDelete = bg }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
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
                        onCheckedChange = { viewModel.setDarkMode(it) },
                        enabled = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 新手指引
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onStartOnboarding
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("新手指引", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "查看功能介绍和操作说明",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "新手指引",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { uriHandler.openUri("https://www.needaichat.top") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("官方网站", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PS：欢迎向作者反馈",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
            },
            onPreviewChanged = { preview ->
                viewModel.updateModelConfig(preview)
            }
        )
    }

    if (showBackgroundNameDialog && pendingBackgroundUri != null) {
        AlertDialog(
            onDismissRequest = {
                showBackgroundNameDialog = false
                pendingBackgroundUri = null
            },
            title = { Text("命名背景", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pendingBackgroundName,
                    onValueChange = { pendingBackgroundName = it },
                    label = { Text("背景名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = pendingBackgroundName.trim().ifEmpty { "未命名背景" }
                        scope.launch {
                            try {
                                val id = java.util.UUID.randomUUID().toString()
                                val bgDir = java.io.File(context.getExternalFilesDir(null), "backgrounds")
                                bgDir.mkdirs()
                                val destFile = java.io.File(bgDir, "$id.jpg")
                                val inputStream = context.contentResolver.openInputStream(pendingBackgroundUri!!)
                                if (inputStream == null) {
                                    throw Exception("无法读取图片文件，URI 可能已失效")
                                }
                                withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    inputStream.use { input ->
                                        destFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                }
                                bgSettingsDataStore.addBackground(
                                    BackgroundConfig(
                                        id = id,
                                        name = name,
                                        imagePath = destFile.absolutePath
                                    )
                                )
                                showBackgroundNameDialog = false
                                pendingBackgroundUri = null
                            } catch (e: Exception) {
                                FileLogger.e("SettingsScreen", "保存背景失败", e)
                                snackbarHostState.showSnackbar("保存背景失败: ${e.localizedMessage ?: "未知错误"}")
                            }
                        }
                    },
                    enabled = true
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackgroundNameDialog = false
                    pendingBackgroundUri = null
                }) { Text("取消") }
            }
        )
    }

    if (bgToDelete != null) {
        AlertDialog(
            onDismissRequest = { bgToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除背景") },
            text = { Text("确定要删除「${bgToDelete!!.name}」吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        val bg = bgToDelete ?: return@Button
                        scope.launch {
                            try {
                                java.io.File(bg.imagePath).delete()
                            } catch (_: Exception) { }
                            bgSettingsDataStore.removeBackground(bg.id)
                            bgToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { bgToDelete = null }) { Text("取消") }
            }
        )
    }
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
