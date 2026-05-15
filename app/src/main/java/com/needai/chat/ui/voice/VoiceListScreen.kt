package com.needai.chat.ui.voice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
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
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.ui.voice.components.CreateVoiceDialog
import com.needai.chat.ui.voice.components.EditVoiceDialog
import com.needai.chat.ui.voice.components.SimpleCreateVoiceDialog
import com.needai.chat.ui.voice.components.VoiceCard
import com.needai.chat.ui.voice.components.VoicePreviewPlayer
import com.needai.chat.util.ITtsManager
import com.needai.chat.util.TtsManagerImpl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceListScreen(
    onNavigateBack: () -> Unit,
    viewModel: VoiceListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editVoice by remember { mutableStateOf<VoiceInfo?>(null) }
    var deleteVoice by remember { mutableStateOf<VoiceInfo?>(null) }
    var previewVoice by remember { mutableStateOf<VoiceInfo?>(null) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }

    val settingsDataStore = remember { SettingsDataStore(context) }
    val ttsApiKey by settingsDataStore.ttsApiKey.collectAsState(initial = "")
    val ttsModel by settingsDataStore.ttsModel.collectAsState(initial = "cosyvoice-v3.5-flash")
    val ttsVolume by settingsDataStore.ttsVolume.collectAsState(initial = 50)
    val ttsRate by settingsDataStore.ttsRate.collectAsState(initial = 1.0f)
    val ttsPitch by settingsDataStore.ttsPitch.collectAsState(initial = 1.0f)

    val ttsManager = remember(ttsApiKey) {
        TtsManagerImpl(
            context = context,
            apiKey = ttsApiKey,
            parameters = CosyVoiceParameters(
                model = ttsModel,
                volume = ttsVolume,
                rate = ttsRate,
                pitch = ttsPitch
            )
        )
    } as ITtsManager

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音色管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncFromRemote() },
                        enabled = !uiState.isSyncing
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "同步远程音色",
                            tint = if (uiState.isSyncing) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新增音色")
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "创建自定义音色")
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
            // Creating/syncing status indicator
            if (uiState.isSyncing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(text = "正在从远程同步音色...")
                    }
                }
            }
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

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.voices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "暂无音色",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击 + 新增音色或 ✨ 创建自定义音色",
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
                    items(uiState.voices, key = { it.voiceId }) { voice ->
                        VoiceCard(
                            voice = voice,
                            onPlay = { previewVoice = voice },
                            onEdit = { editVoice = voice },
                            onDelete = { deleteVoice = voice }
                        )
                    }
                }
            }
        }
    }

    // Add voice dialog (manual entry)
    if (showAddDialog) {
        SimpleCreateVoiceDialog(
            onDismiss = { showAddDialog = false },
            onCreate = { voiceId, voicePrompt, targetModel, previewText ->
                viewModel.addVoice(
                    VoiceInfo(
                        voiceId = voiceId,
                        displayName = voiceId,
                        voicePrompt = voicePrompt,
                        targetModel = targetModel,
                        previewText = previewText,
                        status = "OK"
                    )
                )
                showAddDialog = false
            }
        )
    }

    // Create custom voice via Voice Design API
    if (showCreateDialog) {
        CreateVoiceDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { targetModel, prefix, voicePrompt, previewText ->
                viewModel.createCustomVoice(targetModel, prefix, voicePrompt, previewText)
                showCreateDialog = false
            }
        )
    }

    // Edit voice dialog
    if (editVoice != null) {
        EditVoiceDialog(
            voice = editVoice!!,
            onDismiss = { editVoice = null },
            onSave = { updated ->
                viewModel.updateVoice(editVoice!!.voiceId, updated)
                editVoice = null
            }
        )
    }

    // Delete confirmation
    if (deleteVoice != null) {
        AlertDialog(
            onDismissRequest = { deleteVoice = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除音色") },
            text = { Text("确定要删除「${deleteVoice!!.displayName.ifEmpty { deleteVoice!!.voiceId }}」吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVoice(deleteVoice!!.voiceId)
                        deleteVoice = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteVoice = null }) { Text("取消") }
            }
        )
    }

    // Preview player dialog
    if (previewVoice != null) {
        AlertDialog(
            onDismissRequest = { previewVoice = null },
            title = { Text("试听: ${previewVoice!!.displayName.ifEmpty { previewVoice!!.voiceId }}") },
            text = {
                VoicePreviewPlayer(
                    previewAudioBase64 = null,
                    onPlay = { text ->
                        ttsManager.speak(text, previewVoice!!.voiceId)
                        playingVoiceId = previewVoice!!.voiceId
                    },
                    onStop = {
                        ttsManager.stop()
                        playingVoiceId = null
                    },
                    isPlaying = playingVoiceId == previewVoice!!.voiceId
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    previewVoice = null
                    playingVoiceId = null
                }) { Text("关闭") }
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
}
