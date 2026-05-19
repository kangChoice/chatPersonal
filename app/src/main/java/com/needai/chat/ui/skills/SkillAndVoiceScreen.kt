package com.needai.chat.ui.skills

import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.needai.chat.ui.theme.*
import com.needai.chat.util.ITtsManager
import com.needai.chat.util.TtsManagerImpl
import kotlinx.coroutines.launch

/** 系统预置音色映射为 VoiceInfo，统一列表展示 */
private val SYSTEM_VOICES: List<VoiceInfo> = SystemVoiceProvider.getSkillEditorVoices().map { sv ->
    VoiceInfo(
        voiceId = sv.voiceId,
        displayName = sv.displayName,
        voicePrompt = sv.description,
        targetModel = sv.supportedModels.firstOrNull() ?: "cosyvoice-v3-flash",
        status = "OK"
    )
}

private fun isSystemVoice(voiceId: String): Boolean =
    SYSTEM_VOICES.any { it.voiceId == voiceId }

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

    val voiceSkillBindings = remember(skills, uiState.voices) {
        val nonBuiltinSkills = skills.filter { !it.isBuiltin }
        (uiState.voices + SYSTEM_VOICES).associate { voice ->
            voice.voiceId to nonBuiltinSkills.filter { it.voiceId == voice.voiceId }
        }
    }

    val customVoiceModelMap = remember(uiState.voices) {
        uiState.voices.filter { it.targetModel.isNotBlank() }
            .associate { it.voiceId to it.targetModel }
    }
    val voiceModelResolver: (String) -> String? = { voiceId ->
        SystemVoiceProvider.getModelForVoice(voiceId) ?: customVoiceModelMap[voiceId]
    }

    val modelFilters = remember(uiState.voices) {
        (SYSTEM_VOICES.map { it.targetModel } + uiState.voices.map { it.targetModel })
            .filter { it.isNotBlank() }.distinct().sorted()
    }
    val allVoices = remember(uiState.voices) {
        SYSTEM_VOICES + uiState.voices.filterNot { isSystemVoice(it.voiceId) }
    }
    val filteredVoices = remember(allVoices, selectedModelFilter) {
        if (selectedModelFilter.isEmpty()) allVoices
        else allVoices.filter { it.targetModel == selectedModelFilter }
    }

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
    Box(modifier = Modifier.fillMaxSize()) {
        // Background (root glow in NavGraph covers this)

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)),
                        containerColor = Color.Black.copy(alpha = 0.75f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        val icon = when {
                            data.visuals.message.startsWith("TTS") || data.visuals.message.contains("预览") -> Icons.AutoMirrored.Filled.VolumeUp
                            data.visuals.message.contains("已创建") || data.visuals.message.contains("已导入") -> Icons.Default.Check
                            data.visuals.message.contains("失败") -> Icons.Default.Warning
                            else -> null
                        }
                        if (icon != null) {
                            Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = data.visuals.message,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            topBar = {
                if (!isSelectionMode) {
                    // Glass nav bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandGradientText(
                            text = if (selectedTab == 0) "角色管理" else "音色管理",
                            fontSize = 22.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (selectedTab == 0) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GlassWhite)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        .clickable { navController.navigate(Screen.skillEdit("new")) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", color = BrandBlue, fontSize = 18.sp, fontWeight = FontWeight.Light)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GlassWhite)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        .clickable { importSkillLauncher.launch(arrayOf("application/json")) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↓", color = BrandBlue, fontSize = 14.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GlassWhite)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        .clickable { isSelectionMode = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("☰", color = TextSecondary, fontSize = 14.sp)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GlassWhite)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        .clickable { voiceViewModel.refreshVoices() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "刷新",
                                        tint = if (uiState.isLoading) TextTertiary else BrandBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GlassWhite)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        .clickable { showVoiceCreateDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "创建自定义音色",
                                        tint = BrandPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(GlassWhite)
                                            .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                            .clickable { showVoiceMenu = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "更多",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showVoiceMenu,
                                        onDismissRequest = { showVoiceMenu = false },
                                        modifier = Modifier
                                            .background(GlassWhite, RoundedCornerShape(16.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("删除全部音色", fontSize = 14.sp, color = BrandPink) },
                                            onClick = {
                                                showVoiceMenu = false
                                                showDeleteAllDialog = true
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = BrandPink)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (isSelectionMode) {
                    // Glass selection mode bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                            .clip(RoundedCornerShape(24.dp))
                            .background(GlassWhite)
                            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
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
                                Text(if (selectedSkillIds.size == skills.size) "取消全选" else "全选",
                                    color = BrandBlue)
                            }
                            Button(
                                onClick = {
                                    if (selectedSkillIds.isNotEmpty()) {
                                        val fileName = "skills_export_${java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())}.json"
                                        exportSkillsLauncher.launch(fileName)
                                    }
                                },
                                enabled = selectedSkillIds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandBlue
                                ),
                                shape = RoundedCornerShape(999.dp)
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
                // Capsule tab row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selectedTab == 0) BrandMint.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                selectedTab = 0
                                isSelectionMode = false
                                selectedSkillIds = emptySet()
                            }
                            .padding(horizontal = 24.dp, vertical = 8.dp)

                    ) {
                        Text(
                            "角色管理",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) BrandMint else TextTertiary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selectedTab == 1) BrandMint.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                if (selectedTab != 1) voiceViewModel.loadVoices()
                                selectedTab = 1
                            }
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "音色管理",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) BrandMint else TextTertiary
                        )
                    }
                }

                // Tab content
                if (selectedTab == 0) {
                    // ==============================
                    // 角色管理
                    // ==============================
                    if (skills.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "暂无角色",
                                    fontSize = 16.sp,
                                    color = TextTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "点击右上角 + 创建",
                                    fontSize = 13.sp,
                                    color = TextTertiary.copy(alpha = 0.6f)
                                )
                            }
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
                                    voiceName = if (skill.voiceId.isNotBlank()) {
                                        voiceAliases[skill.voiceId]?.ifBlank { null }
                                            ?: SystemVoiceProvider.findSystemVoice(skill.voiceId)?.displayName
                                            ?: skill.voiceId
                                    } else null,
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
                        if (uiState.isCreating && uiState.creatingStatus != null) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(GlassWhite)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = BrandMint
                                        )
                                        Text(
                                            text = uiState.creatingStatus ?: "",
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

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
                                        label = { Text("全部") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandMint.copy(alpha = 0.2f),
                                            selectedLabelColor = BrandMint
                                        )
                                    )
                                    modelFilters.forEach { model ->
                                        FilterChip(
                                            selected = selectedModelFilter == model,
                                            onClick = { selectedModelFilter = model },
                                            label = { Text(model) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = BrandMint.copy(alpha = 0.2f),
                                                selectedLabelColor = BrandMint
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = BrandMint)
                                }
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
                                            fontSize = 15.sp,
                                            color = TextTertiary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "点击 ✨ 创建自定义音色",
                                            fontSize = 13.sp,
                                            color = TextTertiary.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredVoices, key = { it.voiceId }) { voice ->
                                val isSystem = isSystemVoice(voice.voiceId)
                                val isPlaying = playingVoiceId == voice.voiceId
                                VoiceCard(
                                    voice = voice,
                                    alias = voiceAliases[voice.voiceId] ?: "",
                                    boundSkills = voiceSkillBindings[voice.voiceId] ?: emptyList(),
                                    isPlaying = isPlaying,
                                    canPlay = voice.status == "OK" || isSystem,
                                    isBuiltin = isSystem,
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
                                    onDelete = if (isSystem) null else ({ deleteVoice = voice }),
                                    onAliasEdit = if (isSystem) null else {
                                        {
                                            editingAliasVoice = voice
                                            editingAliasText = voiceAliases[voice.voiceId] ?: ""
                                        }
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

        // API Key 错误弹窗
        if (uiState.apiKeyErrorType != null) {
            AlertDialog(
                onDismissRequest = { voiceViewModel.dismissApiKeyError() },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = BrandPink) },
                title = { Text("API Key 错误", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = { Text(uiState.apiKeyErrorMessage, color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { voiceViewModel.dismissApiKeyError() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(999.dp)
                    ) { Text("确定") }
                }
            )
        }

        // Dialogs – Skill
        if (skillToDelete != null) {
            AlertDialog(
                onDismissRequest = { skillToDelete = null },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BrandPink) },
                title = { Text("删除角色", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Text("确定要删除「${skillToDelete!!.name}」吗？删除后，该角色对应的所有历史会话记录也将一并删除，此操作不可撤销。")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            skillViewModel.deleteSkill(skillToDelete!!.id)
                            skillToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPink)
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { skillToDelete = null }) { Text("取消") }
                }
            )
        }

        // Dialogs – Voice
        if (showVoiceCreateDialog) {
            CreateVoiceDialog(
                devicePrefix = uiState.devicePrefix,
                rawDeviceId = uiState.rawDeviceId,
                onDismiss = { showVoiceCreateDialog = false },
                onCreate = { targetModel, voicePrompt ->
                    voiceViewModel.createCustomVoice(targetModel, voicePrompt)
                    showVoiceCreateDialog = false
                }
            )
        }

        if (editingAliasVoice != null) {
            val voice = editingAliasVoice!!
            Dialog(
                onDismissRequest = { editingAliasVoice = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        BrandGradientText(text = "编辑别名", fontSize = 22.sp)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "为「${voice.displayName.ifEmpty { voice.voiceId }}」设置别名",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editingAliasText,
                            onValueChange = { editingAliasText = it },
                            label = { Text("别名") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                cursorColor = BrandBlue,
                                focusedLabelColor = BrandBlue
                            )
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { editingAliasVoice = null }) { Text("取消", color = TextSecondary) }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        settingsDataStore.setVoiceAlias(voice.voiceId, editingAliasText)
                                    }
                                    editingAliasVoice = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(999.dp)
                            ) { Text("保存") }
                        }
                    }
                }
            }
        }

        if (editingBindingsVoice != null) {
            val voice = editingBindingsVoice!!
            val nonBuiltinSkills = skills.filter { !it.isBuiltin }
            Dialog(
                onDismissRequest = { editingBindingsVoice = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        BrandGradientText(text = "绑定角色", fontSize = 22.sp)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "为「${(voiceAliases[voice.voiceId] ?: "").ifEmpty { voice.displayName.ifEmpty { voice.voiceId } }}」选择绑定的角色",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        if (nonBuiltinSkills.isEmpty()) {
                            Text(text = "暂无自定义角色", color = TextTertiary)
                        } else {
                            Column(
                                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                nonBuiltinSkills.forEach { skill ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (skill.id in selectedBindingSkillIds) BrandBlue.copy(alpha = 0.06f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedBindingSkillIds = if (skill.id in selectedBindingSkillIds) {
                                                    selectedBindingSkillIds - skill.id
                                                } else {
                                                    selectedBindingSkillIds + skill.id
                                                }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
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
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${skill.avatar} ${skill.name}",
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { editingBindingsVoice = null }) { Text("取消", color = TextSecondary) }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    skillViewModel.updateSkillsVoiceId(voice.voiceId, selectedBindingSkillIds)
                                    editingBindingsVoice = null
                                },
                                enabled = nonBuiltinSkills.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(999.dp)
                            ) { Text("保存") }
                        }
                    }
                }
            }
        }

        if (deleteVoice != null) {
            AlertDialog(
                onDismissRequest = { deleteVoice = null },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BrandPink) },
                title = { Text("删除音色", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    val boundSkills = voiceSkillBindings[deleteVoice!!.voiceId] ?: emptyList()
                    Column {
                        Text("确定要删除「${deleteVoice!!.displayName.ifEmpty { deleteVoice!!.voiceId }}」吗？", color = TextSecondary)
                        if (boundSkills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "该音色绑定了 ${boundSkills.size} 个角色，删除后这些角色的音色配置将置为无音色。",
                                fontSize = 13.sp,
                                color = BrandPink
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val voiceId = deleteVoice!!.voiceId
                            val boundIds = (voiceSkillBindings[voiceId] ?: emptyList()).map { it.id }.toSet()
                            if (boundIds.isNotEmpty()) {
                                skillViewModel.clearVoiceIdForSkillIds(boundIds)
                            }
                            voiceViewModel.deleteVoice(voiceId)
                            deleteVoice = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPink)
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteVoice = null }) { Text("取消", color = TextSecondary) }
                }
            )
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BrandPink) },
                title = { Text("删除全部音色", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column {
                        Text("确定要删除所有远程音色吗？此操作不可撤销，已删除的音色无法恢复。系统内置音色不受影响。", color = TextSecondary)
                        val allBoundIds = uiState.voices.flatMap { v ->
                            (voiceSkillBindings[v.voiceId] ?: emptyList()).map { it.id }
                        }.toSet()
                        if (allBoundIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "同时将解除 ${allBoundIds.size} 个角色的音色绑定。",
                                fontSize = 13.sp,
                                color = BrandPink
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val allBoundIds = uiState.voices.flatMap { v ->
                                (voiceSkillBindings[v.voiceId] ?: emptyList()).map { it.id }
                            }.toSet()
                            if (allBoundIds.isNotEmpty()) {
                                skillViewModel.clearVoiceIdForSkillIds(allBoundIds)
                            }
                            voiceViewModel.deleteAllVoices()
                            showDeleteAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPink)
                    ) { Text("全部删除") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) { Text("取消", color = TextSecondary) }
                }
            )
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            voiceViewModel.dismissError()
        }
    }

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
