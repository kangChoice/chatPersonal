package com.needai.chat.ui.voice

import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.remote.tts.CosyVoiceParameters
import com.needai.chat.data.remote.tts.SystemVoiceProvider
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.ui.voice.components.CreateVoiceDialog
import com.needai.chat.ui.voice.components.VoiceCard
import com.needai.chat.util.ITtsManager
import com.needai.chat.util.TtsManagerImpl
import com.needai.chat.ui.theme.*
import androidx.compose.ui.graphics.Color

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
fun VoiceListScreen(
    onNavigateBack: () -> Unit,
    viewModel: VoiceListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var deleteVoice by remember { mutableStateOf<VoiceInfo?>(null) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    var selectedModelFilter by remember { mutableStateOf("") }

    val settingsDataStore = remember { SettingsDataStore(context) }
    val ttsApiKey by settingsDataStore.ttsApiKey.collectAsState(initial = "")
    val ttsVolume by settingsDataStore.ttsVolume.collectAsState(initial = 50)
    val ttsRate by settingsDataStore.ttsRate.collectAsState(initial = 1.0f)
    val ttsPitch by settingsDataStore.ttsPitch.collectAsState(initial = 1.0f)

    // Build voice→model mapping for TTS model resolution
    val customVoiceModelMap = remember(uiState.voices) {
        uiState.voices.filter { it.targetModel.isNotBlank() }
            .associate { it.voiceId to it.targetModel }
    }
    val voiceModelResolver: (String) -> String? = { voiceId ->
        SystemVoiceProvider.getModelForVoice(voiceId) ?: customVoiceModelMap[voiceId]
    }

    // Distinct models from voice list for filter chips
    val modelFilters = remember(uiState.voices) {
        (SYSTEM_VOICES.map { it.targetModel } + uiState.voices.map { it.targetModel })
            .filter { it.isNotBlank() }.distinct().sorted()
    }

    // 合并系统内置音色 + 自定义音色
    val allVoices = remember(uiState.voices) {
        SYSTEM_VOICES + uiState.voices.filterNot { isSystemVoice(it.voiceId) }
    }

    // 按模型筛选
    val filteredVoices = remember(allVoices, selectedModelFilter) {
        if (selectedModelFilter.isEmpty()) allVoices
        else allVoices.filter { it.targetModel == selectedModelFilter }
    }

    var ttsManager by remember { mutableStateOf<ITtsManager?>(null) }
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

    Box(modifier = Modifier.fillMaxSize()) {
        FluidGlowBackground()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("音色管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshVoices() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = if (uiState.isLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "创建自定义音色")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("删除全部音色") },
                                onClick = {
                                    showMenu = false
                                    showDeleteAllDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Creating status indicator
            if (uiState.isCreating && uiState.creatingStatus != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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

            // Model filter chips
            if (modelFilters.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredVoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredVoices, key = { it.voiceId }) { voice ->
                        val isSystem = isSystemVoice(voice.voiceId)
                        val isPlaying = playingVoiceId == voice.voiceId
                        VoiceCard(
                            voice = voice,
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
                            onDelete = if (isSystem) null else ({ deleteVoice = voice })
                        )
                    }
                }
            }
        }
    }
    }

    // Create custom voice via Voice Design API
    if (showCreateDialog) {
        CreateVoiceDialog(
            devicePrefix = uiState.devicePrefix,
            rawDeviceId = uiState.rawDeviceId,
            onDismiss = { showCreateDialog = false },
            onCreate = { targetModel, voicePrompt ->
                viewModel.createCustomVoice(targetModel, voicePrompt)
                showCreateDialog = false
            }
        )
    }

    // Delete confirmation
    if (deleteVoice != null) {
        AlertDialog(
            onDismissRequest = { deleteVoice = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BrandPink) },
            title = { Text("删除音色", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("确定要删除「${deleteVoice!!.displayName.ifEmpty { deleteVoice!!.voiceId }}」吗？", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVoice(deleteVoice!!.voiceId)
                        deleteVoice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPink)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteVoice = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // Delete all voices confirmation
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BrandPink) },
            title = { Text("删除全部音色", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("确定要删除所有远程音色吗？此操作不可撤销，已删除的音色无法恢复。系统内置音色不受影响。", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllVoices()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPink)
                ) { Text("全部删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }
    Box { SnackbarHost(snackbarHostState) }

    // Preview audio: 自动播放创建接口返回的预览 WAV
    if (uiState.previewAudioData != null) {
        val context = LocalContext.current
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
                Log.e("VoiceListScreen", "播放预览音频失败", e)
            }
            viewModel.dismissPreviewAudio()
        }
    }
}
