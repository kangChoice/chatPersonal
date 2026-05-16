package com.needai.chat.ui.skills

import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.remote.tts.CosyVoiceParameters
import com.needai.chat.data.remote.tts.SystemVoiceProvider
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.ui.voice.VoiceListViewModel
import com.needai.chat.ui.voice.components.CreateVoiceDialog
import com.needai.chat.ui.voice.components.VoiceCard
import com.needai.chat.data.export.ExportUtils
import com.needai.chat.ui.navigation.Screen
import com.needai.chat.ui.skills.components.SkillCard
import com.needai.chat.util.ITtsManager
import com.needai.chat.util.TtsManagerImpl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillAndVoiceScreen(
    navController: NavController,
    skillViewModel: SkillViewModel = hiltViewModel(),
    voiceViewModel: VoiceListViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // ======================================================================
    // Skill 状态
    // ======================================================================
    val skills by skillViewModel.skills.collectAsStateWithLifecycle()
    var showSkillCreateDialog by remember { mutableStateOf(false) }
    var skillToDelete by remember { mutableStateOf<Skill?>(null) }
    var skillToExport by remember { mutableStateOf<Skill?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedSkillIds by remember { mutableStateOf(setOf<String>()) }

    val exportSkillLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && skillToExport != null) {
            val json = ExportUtils.generateSkillJson(skillToExport!!)
            ExportUtils.writeToUri(context, uri, json)
            skillToExport = null
        }
    }
    val exportSkillsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && selectedSkillIds.isNotEmpty()) {
            val selectedSkills = skills.filter { it.id in selectedSkillIds }
            val json = ExportUtils.generateSkillsJson(selectedSkills)
            ExportUtils.writeToUri(context, uri, json)
            selectedSkillIds = emptySet()
            isSelectionMode = false
        }
    }
    val importSkillLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = com.needai.chat.data.import.ImportUtils.readFromUri(context, uri)
            if (json != null) {
                val result = com.needai.chat.data.import.ImportUtils.parseSkillsJson(json)
                result.onSuccess { parsedSkills ->
                    if (parsedSkills.size == 1) {
                        skillViewModel.importSkill(parsedSkills.first()) { success, msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    } else {
                        skillViewModel.importSkills(parsedSkills) { success, msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    }
                }.onFailure { e ->
                    coroutineScope.launch { snackbarHostState.showSnackbar("导入失败: ${e.localizedMessage}") }
                }
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar("读取文件失败") }
            }
        }
    }

    // ======================================================================
    // Voice 状态
    // ======================================================================
    val uiState by voiceViewModel.uiState.collectAsStateWithLifecycle()

    // TTS 参数（用于试听预览）
    val settingsDataStore = remember { SettingsDataStore(context) }
    val ttsApiKey by settingsDataStore.ttsApiKey.collectAsState(initial = "")
    val ttsVolume by settingsDataStore.ttsVolume.collectAsState(initial = 50)
    val ttsRate by settingsDataStore.ttsRate.collectAsState(initial = 1.0f)
    val ttsPitch by settingsDataStore.ttsPitch.collectAsState(initial = 1.0f)
    val voiceAliases by settingsDataStore.voiceAliases.collectAsState(initial = emptyMap())

    var showVoiceCreateDialog by remember { mutableStateOf(false) }
    var showVoiceMenu by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var deleteVoice by remember { mutableStateOf<VoiceInfo?>(null) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    var selectedModelFilter by remember { mutableStateOf("") }
    var ttsManager by remember { mutableStateOf<ITtsManager?>(null) }
    var editingAliasVoice by remember { mutableStateOf<VoiceInfo?>(null) }
    var editingAliasText by remember { mutableStateOf("") }
    var editingBindingsVoice by remember { mutableStateOf<VoiceInfo?>(null) }
    var selectedBindingSkillIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Voice → bound skills mapping
    val voiceSkillBindings = remember(skills, uiState.voices) {
        val nonBuiltinSkills = skills.filter { !it.isBuiltin }
        uiState.voices.associate { voice ->
            voice.voiceId to nonBuiltinSkills.filter { it.voiceId == voice.voiceId }
        }
    }

    // Voice → model mapping
    val customVoiceModelMap = remember(uiState.voices) {
        uiState.voices.filter { it.targetModel.isNotBlank() }
            .associate { it.voiceId to it.targetModel }
    }
    val voiceModelResolver: (String) -> String? = { voiceId ->
        SystemVoiceProvider.getModelForVoice(voiceId) ?: customVoiceModelMap[voiceId]
    }

    val modelFilters = remember(uiState.voices) {
        uiState.voices.map { it.targetModel }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val filteredVoices = remember(uiState.voices, selectedModelFilter) {
        if (selectedModelFilter.isEmpty()) uiState.voices
        else uiState.voices.filter { it.targetModel == selectedModelFilter }
    }

    // TTS Manager for voice preview
    LaunchedEffect(ttsApiKey, ttsVolume, ttsRate, ttsPitch) {
        ttsManager?.shutdown()
        ttsManager = TtsManagerImpl(
            apiKey = ttsApiKey,
            parameters = CosyVoiceParameters(
                volume = ttsVolume,
                rate = ttsRate,
                pitch = ttsPitch
            ),
            voiceModelResolver = voiceModelResolver
        )
    }
    DisposableEffect(Unit) {
        onDispose { ttsManager?.shutdown() }
    }

    // ======================================================================
    // UI
    // ======================================================================
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        topBar = {
            if (!isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            if (selectedTab == 0) "角色管理" else "音色管理",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        if (selectedTab == 0) {
                            // --- Skill tab actions ---
                            IconButton(onClick = { showSkillCreateDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "创建角色")
                            }
                            TextButton(onClick = {
                                importSkillLauncher.launch(arrayOf("application/json"))
                            }) {
                                Text("导入")
                            }
                            TextButton(onClick = { isSelectionMode = true }) {
                                Text("选择")
                            }
                        } else {
                            // --- Voice tab actions ---
                            IconButton(
                                onClick = { voiceViewModel.refreshVoices() },
                                enabled = !uiState.isLoading
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "刷新",
                                    tint = if (uiState.isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { showVoiceCreateDialog = true }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "创建自定义音色")
                            }
                            Box {
                                IconButton(onClick = { showVoiceMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                                }
                                DropdownMenu(
                                    expanded = showVoiceMenu,
                                    onDismissRequest = { showVoiceMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("删除全部音色") },
                                        onClick = {
                                            showVoiceMenu = false
                                            showDeleteAllDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            selectedSkillIds = if (selectedSkillIds.size == skills.size) {
                                emptySet()
                            } else {
                                skills.map { it.id }.toSet()
                            }
                        }) {
                            Text(if (selectedSkillIds.size == skills.size) "取消全选" else "全选")
                        }
                        Button(
                            onClick = {
                                if (selectedSkillIds.isNotEmpty()) {
                                    val fileName = "skills_export_${java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())}.json"
                                    exportSkillsLauncher.launch(fileName)
                                }
                            },
                            enabled = selectedSkillIds.isNotEmpty()
                        ) {
                            Text("导出选中 (${selectedSkillIds.size})")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab row
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        isSelectionMode = false
                        selectedSkillIds = emptySet()
                    },
                    text = { Text("角色管理") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("音色管理") }
                )
            }

            // ======================================================================
            // Tab content
            // ======================================================================
            if (selectedTab == 0) {
                // ==============================
                // 角色管理
                // ==============================
                if (skills.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无角色，点击右上角 + 创建",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(skills, key = { it.id }) { skill ->
                            SkillCard(
                                skill = skill,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedSkillIds = if (skill.id in selectedSkillIds) {
                                            selectedSkillIds - skill.id
                                        } else {
                                            selectedSkillIds + skill.id
                                        }
                                    } else {
                                        navController.navigate(Screen.skillEdit(skill.id))
                                    }
                                },
                                onExport = if (isSelectionMode) null else {
                                    {
                                        skillToExport = skill
                                        val fileName = "skills_${skill.name}.json"
                                        exportSkillLauncher.launch(fileName)
                                    }
                                },
                                onDelete = if (isSelectionMode || skill.isBuiltin) null else {
                                    { skillToDelete = skill }
                                },
                                isSelected = skill.id in selectedSkillIds,
                                isSelectionMode = isSelectionMode,
                                onSelectionChanged = { checked ->
                                    selectedSkillIds = if (checked) {
                                        selectedSkillIds + skill.id
                                    } else {
                                        selectedSkillIds - skill.id
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // ==============================
                // 音色管理
                // ==============================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 创建中状态指示
                    if (uiState.isCreating && uiState.creatingStatus != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text(text = uiState.creatingStatus ?: "")
                                }
                            }
                        }
                    }

                    // 模型筛选
                    if (modelFilters.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedModelFilter.isEmpty(),
                                    onClick = { selectedModelFilter = "" },
                                    label = { Text("全部") }
                                )
                                modelFilters.forEach { model ->
                                    FilterChip(
                                        selected = selectedModelFilter == model,
                                        onClick = { selectedModelFilter = model },
                                        label = { Text(model) }
                                    )
                                }
                            }
                        }
                    }

                    // 音色列表
                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    } else if (filteredVoices.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (selectedModelFilter.isEmpty()) "暂无音色" else "没有匹配的音色",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "点击 ✨ 创建自定义音色",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredVoices, key = { it.voiceId }) { voice ->
                            val isPlaying = playingVoiceId == voice.voiceId
                            VoiceCard(
                                voice = voice,
                                alias = voiceAliases[voice.voiceId] ?: "",
                                boundSkills = voiceSkillBindings[voice.voiceId] ?: emptyList(),
                                isPlaying = isPlaying,
                                canPlay = voice.status == "OK",
                                onPlay = {
                                    if (isPlaying) {
                                        ttsManager?.stop()
                                        playingVoiceId = null
                                    } else {
                                        ttsManager?.stop()
                                        ttsManager?.speak("你好，欢迎试听我的声音。", voice.voiceId) { playingVoiceId = null }
                                        playingVoiceId = voice.voiceId
                                    }
                                },
                                onDelete = { deleteVoice = voice },
                                onAliasEdit = {
                                    editingAliasVoice = voice
                                    editingAliasText = voiceAliases[voice.voiceId] ?: ""
                                },
                                onEditBindings = {
                                    editingBindingsVoice = voice
                                    selectedBindingSkillIds = (voiceSkillBindings[voice.voiceId] ?: emptyList()).map { it.id }.toSet()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ======================================================================
    // Dialogs – Skill
    // ======================================================================
    if (showSkillCreateDialog) {
        SkillEditDialog(
            onDismiss = { showSkillCreateDialog = false },
            onSave = { name, desc, prompt, avatar, greeting, temp ->
                skillViewModel.createSkill(name, desc, prompt, avatar, greeting, temp) { success, msg ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                }
                showSkillCreateDialog = false
            }
        )
    }

    if (skillToDelete != null) {
        AlertDialog(
            onDismissRequest = { skillToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除角色") },
            text = {
                Text("确定要删除「${skillToDelete!!.name}」吗？删除后，该角色对应的所有历史会话记录也将一并删除，此操作不可撤销。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        skillViewModel.deleteSkill(skillToDelete!!.id)
                        skillToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { skillToDelete = null }) { Text("取消") }
            }
        )
    }

    // ======================================================================
    // Dialogs – Voice
    // ======================================================================
    if (showVoiceCreateDialog) {
        CreateVoiceDialog(
            onDismiss = { showVoiceCreateDialog = false },
            onCreate = { targetModel, prefix, voicePrompt, previewText ->
                voiceViewModel.createCustomVoice(targetModel, prefix, voicePrompt, previewText)
                showVoiceCreateDialog = false
            }
        )
    }

    if (editingAliasVoice != null) {
        val voice = editingAliasVoice!!
        AlertDialog(
            onDismissRequest = { editingAliasVoice = null },
            title = { Text("编辑别名") },
            text = {
                Column {
                    Text(
                        text = "为「${voice.displayName.ifEmpty { voice.voiceId }}」设置别名",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editingAliasText,
                        onValueChange = { editingAliasText = it },
                        label = { Text("别名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        settingsDataStore.setVoiceAlias(voice.voiceId, editingAliasText)
                    }
                    editingAliasVoice = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingAliasVoice = null }) { Text("取消") }
            }
        )
    }

    // 音色-角色绑定编辑对话框
    if (editingBindingsVoice != null) {
        val voice = editingBindingsVoice!!
        val nonBuiltinSkills = skills.filter { !it.isBuiltin }
        AlertDialog(
            onDismissRequest = { editingBindingsVoice = null },
            title = { Text("绑定角色") },
            text = {
                Column {
                    Text(
                        text = "为「${(voiceAliases[voice.voiceId] ?: "").ifEmpty { voice.displayName.ifEmpty { voice.voiceId } }}」选择绑定的角色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (nonBuiltinSkills.isEmpty()) {
                        Text(
                            text = "暂无自定义角色",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        nonBuiltinSkills.forEach { skill ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBindingSkillIds = if (skill.id in selectedBindingSkillIds) {
                                            selectedBindingSkillIds - skill.id
                                        } else {
                                            selectedBindingSkillIds + skill.id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = skill.id in selectedBindingSkillIds,
                                    onCheckedChange = { checked ->
                                        selectedBindingSkillIds = if (checked) {
                                            selectedBindingSkillIds + skill.id
                                        } else {
                                            selectedBindingSkillIds - skill.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${skill.avatar} ${skill.name}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        skillViewModel.updateSkillsVoiceId(voice.voiceId, selectedBindingSkillIds)
                        editingBindingsVoice = null
                    },
                    enabled = nonBuiltinSkills.isNotEmpty()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingBindingsVoice = null }) { Text("取消") }
            }
        )
    }

    if (deleteVoice != null) {
        AlertDialog(
            onDismissRequest = { deleteVoice = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除音色") },
            text = {
                val boundSkills = voiceSkillBindings[deleteVoice!!.voiceId] ?: emptyList()
                Column {
                    Text("确定要删除「${deleteVoice!!.displayName.ifEmpty { deleteVoice!!.voiceId }}」吗？")
                    if (boundSkills.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "该音色绑定了 ${boundSkills.size} 个角色，删除后这些角色的音色配置将置为无音色。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val voiceId = deleteVoice!!.voiceId
                        // Unbind all skills first
                        val boundIds = (voiceSkillBindings[voiceId] ?: emptyList()).map { it.id }.toSet()
                        if (boundIds.isNotEmpty()) {
                            skillViewModel.clearVoiceIdForSkillIds(boundIds)
                        }
                        voiceViewModel.deleteVoice(voiceId)
                        deleteVoice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteVoice = null }) { Text("取消") }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除全部音色") },
            text = {
                Column {
                    Text("确定要删除所有远程音色吗？此操作不可撤销，已删除的音色无法恢复。系统内置音色不受影响。")
                    val allBoundIds = uiState.voices.flatMap { v ->
                        (voiceSkillBindings[v.voiceId] ?: emptyList()).map { it.id }
                    }.toSet()
                    if (allBoundIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "同时将解除 ${allBoundIds.size} 个角色的音色绑定。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Unbind all skills
                        val allBoundIds = uiState.voices.flatMap { v ->
                            (voiceSkillBindings[v.voiceId] ?: emptyList()).map { it.id }
                        }.toSet()
                        if (allBoundIds.isNotEmpty()) {
                            skillViewModel.clearVoiceIdForSkillIds(allBoundIds)
                        }
                        voiceViewModel.deleteAllVoices()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("全部删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("取消") }
            }
        )
    }

    // ======================================================================
    // Voice error snackbar
    // ======================================================================
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            voiceViewModel.dismissError()
        }
    }

    // ======================================================================
    // Preview audio auto-play
    // ======================================================================
    if (uiState.previewAudioData != null) {
        LaunchedEffect(uiState.previewAudioData) {
            val preview = uiState.previewAudioData ?: return@LaunchedEffect
            snackbarHostState.showSnackbar("即将播放预览音频...")
            try {
                val bytes = Base64.decode(preview.data, Base64.DEFAULT)
                if (bytes.isEmpty()) return@LaunchedEffect
                val tempFile = java.io.File(context.cacheDir, "voice_preview_${System.nanoTime()}.wav")
                tempFile.writeBytes(bytes)
                val mp = MediaPlayer()
                mp.setDataSource(tempFile.absolutePath)
                mp.setOnCompletionListener { mp.release(); tempFile.delete() }
                mp.setOnErrorListener { _: MediaPlayer, _: Int, _: Int -> mp.release(); tempFile.delete(); true }
                mp.prepare()
                mp.start()
            } catch (e: Exception) {
                Log.e("SkillAndVoiceScreen", "播放预览音频失败", e)
            }
            voiceViewModel.dismissPreviewAudio()
        }
    }
}
