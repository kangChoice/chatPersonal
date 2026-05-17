package com.needai.chat.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.chat.components.ChatInputBar
import com.needai.chat.ui.chat.components.HistorySessionSheet
import com.needai.chat.ui.chat.components.MessageBubble
import com.needai.chat.ui.chat.components.SkillCarousel
import com.needai.chat.ui.chat.components.SkillSelectorSheet
import com.needai.chat.ui.chat.components.StreamingBubble
import com.needai.chat.ui.chat.state.ChatUiState
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.remote.tts.CosyVoiceParameters
import com.needai.chat.data.remote.tts.SystemVoiceProvider
import com.needai.chat.util.ITtsManager
import com.needai.chat.util.TtsManagerImpl
import com.needai.chat.ui.navigation.Screen
import com.needai.chat.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 精确对应 index.html 的 .chat-page 结构
 *
 * 布局:
 *   Layer 1: 全屏模糊背景图片（从当前技能头像/自定义背景取）
 *   Layer 2: 渐变覆盖层 linear-gradient(to bottom, rgba(0,0,0,0.2), transparent 50%, rgba(255,255,255,0.9))
 *   Layer 3: Column { chat-nav, chat-msgs(weight=1), chat-input-section }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSkillSelector by remember { mutableStateOf(false) }
    var showCarousel by remember { mutableStateOf(true) }
    var carouselSelectedIndex by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showHistorySession by remember { mutableStateOf(false) }
    var pendingSkill by remember { mutableStateOf<Skill?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingExportSessionId by remember { mutableStateOf<String?>(null) }
    var showModelTip by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
    var speakingMessageId by remember { mutableStateOf<Long?>(null) }
    var autoSpeaking by remember { mutableStateOf(false) }
    val settingsDataStore = remember { SettingsDataStore(context) }
    val ttsApiKey by settingsDataStore.ttsApiKey.collectAsState(initial = "")
    val ttsVoice by settingsDataStore.ttsVoice.collectAsState(initial = "")
    val ttsVolume by settingsDataStore.ttsVolume.collectAsState(initial = 50)
    val ttsRate by settingsDataStore.ttsRate.collectAsState(initial = 1.0f)
    val ttsPitch by settingsDataStore.ttsPitch.collectAsState(initial = 1.0f)
    val ttsAutoRead by settingsDataStore.ttsAutoRead.collectAsState(initial = false)
    val voiceAliases by settingsDataStore.voiceAliases.collectAsState(initial = emptyMap())
    val voiceModelMap by viewModel.voiceModelMap.collectAsState()
    val voiceModelResolver: (String) -> String? = { voiceId ->
        SystemVoiceProvider.getModelForVoice(voiceId) ?: voiceModelMap[voiceId]
    }
    val backgroundList by settingsDataStore.backgrounds.collectAsState(initial = emptyList())
    val selectedBgId by settingsDataStore.selectedBackgroundId.collectAsState(initial = "")
    val selectedBg = remember(backgroundList, selectedBgId) {
        backgroundList.find { it.id == selectedBgId }
    }
    val backgroundBitmap = remember(selectedBg) {
        selectedBg?.imagePath?.let { path ->
            try { android.graphics.BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
        }
    }

    val skill = uiState.currentSkill

    var ttsManager by remember { mutableStateOf<ITtsManager?>(null) }
    LaunchedEffect(ttsApiKey, ttsVoice, ttsVolume, ttsRate, ttsPitch) {
        ttsManager?.shutdown()
        ttsManager = TtsManagerImpl(
            apiKey = ttsApiKey,
            parameters = CosyVoiceParameters(
                voice = ttsVoice,
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

    // Export launchers
    val exportCurrentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) viewModel.exportCurrentSessionToFile(context, uri)
    }
    val exportHistoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            pendingExportSessionId?.let { sessionId ->
                viewModel.exportSessionToFile(context, sessionId, uri)
                pendingExportSessionId = null
            }
        }
    }
    val importSessionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importSession(context, uri) { _, msg ->
                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }
    LaunchedEffect(uiState.messages.size, uiState.currentStreamingMessage) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    // Auto-read TTS（保持不变）
    LaunchedEffect(uiState.isStreaming) {
        if (!uiState.isStreaming) return@LaunchedEffect
        if (!ttsAutoRead || ttsManager == null) return@LaunchedEffect
        val tts = ttsManager as? com.needai.chat.util.TtsManagerImpl ?: return@LaunchedEffect
        val voiceId = if (uiState.currentSkill.voiceId.isNotBlank()) uiState.currentSkill.voiceId else ttsVoice
        speakingMessageId = -1L
        while (uiState.isStreaming && uiState.currentStreamingMessage.isEmpty()) delay(100)
        if (!uiState.isStreaming) return@LaunchedEffect
        autoSpeaking = true
        coroutineScope.launch { snackbarHostState.showSnackbar("TTS: 自动朗读") }
        var lastProcessedText = ""
        var sentenceBuffer = StringBuilder()
        try {
            val firstText = uiState.currentStreamingMessage
            if (firstText.isNotEmpty()) { tts.speakQueued(firstText, voiceId); lastProcessedText = firstText }
            while (uiState.isStreaming && autoSpeaking) {
                delay(200)
                val currentText = uiState.currentStreamingMessage
                if (currentText.length <= lastProcessedText.length) continue
                val newPart = currentText.substring(lastProcessedText.length)
                sentenceBuffer.append(newPart)
                lastProcessedText = currentText
                val buf = sentenceBuffer.toString()
                val lastSentenceEnd = buf.indexOfLast { it in "。！？" }
                val lastComma = buf.indexOfLast { it in "；，" }
                val flushAt = when {
                    lastSentenceEnd >= 15 -> lastSentenceEnd + 1
                    buf.length >= 30 && lastComma >= buf.length / 2 -> lastComma + 1
                    buf.length >= 40 -> buf.length
                    else -> 0
                }
                if (flushAt > 0) {
                    val toSend = buf.substring(0, flushAt)
                    val rest = buf.substring(flushAt)
                    if (toSend.isNotBlank()) tts.speakQueued(toSend, voiceId)
                    sentenceBuffer = StringBuilder(rest)
                }
            }
        } finally {
            if (sentenceBuffer.isNotEmpty()) tts.speakQueued(sentenceBuffer.toString(), voiceId)
            val finalText = uiState.currentStreamingMessage
            if (finalText.length > lastProcessedText.length) tts.speakQueued(finalText.substring(lastProcessedText.length), voiceId)
            speakingMessageId = null; autoSpeaking = false
        }
    }

    // ============================================================
    // LAYOUT — 精确对应 index.html 的 .chat-page 结构
    // ============================================================
    Box(modifier = Modifier.fillMaxSize()) {

        // Layer 0: Fluid glow background (visible in carousel mode)
        FluidGlowBackground()

        // Layer 1: 背景图片 — 对应 .chat-bg img (hide in carousel)
        if (!showCarousel && backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.85f
            )
        }

        // Layer 2: 渐变覆盖 — 对应 .chat-bg-gradient (hide in carousel)
        if (!showCarousel) {
            Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.85f)
                        )
                    )
                )
        )
        }

        // Layer 3: 内容 — carousel or chat
        if (showCarousel) {
            SkillCarousel(
                skills = uiState.availableSkills,
                selectedIndex = carouselSelectedIndex,
                onSelectedIndexChanged = { carouselSelectedIndex = it },
                onSkillSelected = { skill ->
                    viewModel.switchSkill(skill)
                    viewModel.newSession()
                    showCarousel = false
                },
                voiceNameMap = voiceAliases
            )
        } else {
        Column(modifier = Modifier.fillMaxSize()) {

            // ========== .chat-nav ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back → 历史会话入口 / carousel
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { showCarousel = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("<", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // .chat-nav-title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = skill.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.02.sp
                    )
                    val subtitleColor = if (uiState.isModelConfigured) BrandMint else StatusRed.copy(alpha = 0.8f)
                    val subtitleText = if (uiState.isModelConfigured) {
                        "● ${uiState.currentModelName.ifEmpty { "远程模型" }}"
                    } else {
                        "● 未配置模型"
                    }
                    Text(
                        text = subtitleText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtitleColor,
                        letterSpacing = 1.08.sp
                    )
                }

                // Right → 菜单
                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("···", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("切换角色") },
                            onClick = { showMenu = false; showSkillSelector = true }
                        )
                        DropdownMenuItem(
                            text = { Text("历史会话") },
                            onClick = { showMenu = false; showHistorySession = true }
                        )
                        DropdownMenuItem(
                            text = { Text("导出会话") },
                            onClick = { showMenu = false; showExportDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("导入会话") },
                            onClick = { showMenu = false; importSessionLauncher.launch(arrayOf("text/*", "*/*")) }
                        )
                        DropdownMenuItem(
                            text = { Text("新建对话") },
                            onClick = { viewModel.newSession(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("清空上下文") },
                            onClick = { viewModel.clearSession(); showMenu = false }
                        )
                    }
                }
            }

            // ========== .chat-msgs (flex: 1, justify-content: flex-end) ==========
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                    // 空状态 — 居中显示问候语
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            skill.greeting,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                    ) {
                        // Messages
                        items(
                            items = uiState.messages.filter { it.role != com.needai.chat.domain.model.MessageRole.SYSTEM },
                            key = { it.id }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                onSpeak = {
                                    val mgr = ttsManager
                                    if (mgr != null) {
                                        if (speakingMessageId == message.id) {
                                            mgr.stop(); speakingMessageId = null
                                            coroutineScope.launch { snackbarHostState.showSnackbar("TTS: 停止朗读") }
                                        } else {
                                            val voiceId = if (uiState.currentSkill.voiceId.isNotBlank()) uiState.currentSkill.voiceId else ttsVoice
                                            coroutineScope.launch { snackbarHostState.showSnackbar("TTS: 朗读") }
                                            mgr.speak(message.content, voiceId) { speakingMessageId = null }
                                            speakingMessageId = message.id
                                        }
                                    }
                                },
                                isSpeaking = speakingMessageId == message.id
                            )
                        }

                        // Streaming
                        if (uiState.isStreaming && uiState.currentStreamingMessage.isNotEmpty()) {
                            item {
                                StreamingBubble(
                                    content = uiState.currentStreamingMessage,
                                    isStreaming = true
                                )
                                if (autoSpeaking && ttsAutoRead) {
                                    IconButton(
                                        onClick = { ttsManager?.stop(); autoSpeaking = false },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, "暂停朗读", tint = StatusRed, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ========== .chat-input-section ==========
            ChatInputBar(
                inputText = uiState.inputText,
                isStreaming = uiState.isStreaming,
                onInputChanged = viewModel::onInputChanged,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopStreaming
            )
        }
        }
    }

    // ============================================================
    // Dialogs & Sheets
    // ============================================================
    if (showModelTip) {
        AlertDialog(
            onDismissRequest = { showModelTip = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = BrandPink) },
            title = { Text("未配置模型") },
            text = { Text("当前未配置或选择模型，请在设置中配置有效的 API Key 和模型。") },
            confirmButton = { Button(onClick = { showModelTip = false }) { Text("知道了") } }
        )
    }
    if (showSkillSelector) {
        SkillSelectorSheet(
            skills = uiState.availableSkills,
            currentSkillId = uiState.currentSkill.id,
            voiceAliases = voiceAliases,
            onSkillSelected = { s -> if (s.id != uiState.currentSkill.id) pendingSkill = s },
            onDismiss = { showSkillSelector = false }
        )
    }
    if (showHistorySession) {
        HistorySessionSheet(
            sessions = uiState.historySessions,
            currentSessionId = uiState.sessionId,
            onSessionSelected = { viewModel.switchToHistorySession(it) },
            onDeleteSession = { sessionToDelete = it },
            onDismiss = { showHistorySession = false }
        )
    }
    if (pendingSkill != null) {
        AlertDialog(
            onDismissRequest = { pendingSkill = null },
            title = { Text("切换角色") },
            text = { Text("切换到「${pendingSkill!!.name}」将开启新的对话，确定要切换吗？") },
            confirmButton = { Button(onClick = { viewModel.switchSkill(pendingSkill!!); pendingSkill = null }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { pendingSkill = null }) { Text("取消") } }
        )
    }
    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BrandPink) },
            title = { Text("删除会话") },
            text = { Text("确定要删除会话「${sessionToDelete!!.title}」吗？此操作不可撤销。") },
            confirmButton = {
                Button(onClick = { viewModel.deleteSession(sessionToDelete!!.id); sessionToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { sessionToDelete = null }) { Text("取消") } }
        )
    }
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("导出会话") },
            text = {
                Column {
                    Text("选择要导出的会话：", modifier = Modifier.padding(bottom = 8.dp))
                    Surface(
                        onClick = {
                            showExportDialog = false
                            if (uiState.messages.isNotEmpty()) {
                                exportCurrentLauncher.launch("chat_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.md")
                            } else {
                                viewModel.exportSessionToFile(context, "", android.net.Uri.EMPTY)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("当前会话", fontWeight = FontWeight.Medium)
                            Text("${skill.name} · ${uiState.messages.size}条消息", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                    uiState.historySessions.forEach { session ->
                        Surface(
                            onClick = {
                                showExportDialog = false
                                pendingExportSessionId = session.id
                                exportHistoryLauncher.launch("chat_${session.title.take(20).replace(" ", "_")}.md")
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(session.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                Text("${session.skillName} · ${session.messageCount}条消息", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                    if (uiState.messages.isEmpty() && uiState.historySessions.isEmpty()) {
                        Text("没有可导出的会话", color = TextTertiary, modifier = Modifier.padding(vertical = 16.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showExportDialog = false }) { Text("取消") } }
        )
    }

    // Snackbar
    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
                shape = RoundedCornerShape(999.dp)
            )
        }
    }
}
