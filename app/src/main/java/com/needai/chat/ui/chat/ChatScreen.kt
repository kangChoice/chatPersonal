package com.needai.chat.ui.chat

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.needai.chat.ui.chat.components.MessageBubble
import com.needai.chat.ui.chat.components.SkillCarousel
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
    viewModel: ChatViewModel = hiltViewModel(),
    onChatDetailChange: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCarousel by remember { mutableStateOf(true) }
    LaunchedEffect(showCarousel) {
        onChatDetailChange(!showCarousel)
        if (showCarousel) viewModel.onInputChanged("")
    }
    var carouselSelectedIndex by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var pendingSkill by remember { mutableStateOf<Skill?>(null) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
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

    // Compression indicator
    LaunchedEffect(uiState.isCompressing) {
        if (uiState.isCompressing) {
            snackbarHostState.showSnackbar("正在整理记忆，请稍候…")
        }
    }

    // Auto-read TTS
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
        var lastSentFilteredLen = 0
        var sentenceBuffer = StringBuilder()
        try {
            while (uiState.isStreaming && autoSpeaking) {
                delay(200)
                val currentText = uiState.currentStreamingMessage
                val filtered = stripParenthetical(currentText)
                if (filtered.length <= lastSentFilteredLen) continue
                val newPart = filtered.substring(lastSentFilteredLen)
                sentenceBuffer.append(newPart)
                lastSentFilteredLen = filtered.length
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
            // 仅在未被手动停止时 flush 残留文本，避免用户点停止后又重新播放
            if (autoSpeaking && sentenceBuffer.isNotEmpty()) {
                tts.speakQueued(sentenceBuffer.toString(), voiceId)
            }
            // currentStreamingMessage 在 isStreaming=false 时被同步清空，
            // 回退到 messages 中已持久化的完整内容，避免轮询窗口遗漏最后一帧文本
            if (autoSpeaking) {
                val streamText = uiState.currentStreamingMessage
                val finalRawText = if (streamText.isNotEmpty()) {
                    streamText
                } else {
                    uiState.messages.lastOrNull {
                        it.role == com.needai.chat.domain.model.MessageRole.ASSISTANT
                    }?.content ?: ""
                }
                val finalFiltered = stripParenthetical(finalRawText)
                if (finalFiltered.length > lastSentFilteredLen) {
                    tts.speakQueued(finalFiltered.substring(lastSentFilteredLen), voiceId)
                }
            }
            speakingMessageId = null
        }
    }

    // Keep autoSpeaking true while TTS is still playing.
    // Use wasActive flag to avoid turning off autoSpeaking before TTS has even started.
    LaunchedEffect(autoSpeaking) {
        if (!autoSpeaking) return@LaunchedEffect
        var ttsWasActive = false
        while (!ttsWasActive || ttsManager?.isBusy() == true) {
            if (ttsManager?.isBusy() == true) ttsWasActive = true
            delay(200)
        }
        autoSpeaking = false
    }

    // ===== 系统返回手势 =====
    val hasDialog = pendingSkill != null || sessionToDelete != null || showMenu
    BackHandler(enabled = hasDialog || !showCarousel) {
        when {
            pendingSkill != null -> pendingSkill = null
            sessionToDelete != null -> sessionToDelete = null
            showMenu -> showMenu = false
            !showCarousel -> showCarousel = true
        }
    }

    // ============================================================
    // LAYOUT — 精确对应 index.html 的 .chat-page 结构
    // ============================================================
    Box(modifier = Modifier.fillMaxSize().imePadding()) {

        // Layer 1: 背景图片 — 对应 .chat-bg img (hide in carousel)
        if (!showCarousel && backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(4.dp),
                contentScale = ContentScale.Crop,
                alpha = 1.0f
            )
            // Layer 2: 渐变覆盖层 — 确保内容可读
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
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
                    showCarousel = false
                },
                voiceNameMap = voiceAliases
            )
        } else {
        Column(modifier = Modifier.fillMaxSize()) {

            // ========== .chat-nav ==========
            val navTitleColor = MaterialTheme.colorScheme.onBackground
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showCarousel = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("<", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // .chat-nav-title
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = skill.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = navTitleColor,
                        letterSpacing = 0.02.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitleColor = if (uiState.isModelConfigured) navTitleColor.copy(alpha = 0.6f) else StatusRed.copy(alpha = 0.8f)
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
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("···", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("导出会话") },
                            onClick = {
                                showMenu = false
                                if (uiState.messages.isNotEmpty()) {
                                    exportCurrentLauncher.launch("chat_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.md")
                                } else {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("当前没有消息可导出") }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导入会话") },
                            onClick = { showMenu = false; importSessionLauncher.launch(arrayOf("text/*", "*/*")) }
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
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
                                    if (autoSpeaking) {
                                        ttsManager?.stop(); autoSpeaking = false
                                    } else {
                                        val mgr = ttsManager
                                        if (mgr != null) {
                                            if (speakingMessageId == message.id) {
                                                mgr.stop(); speakingMessageId = null
                                            } else {
                                                val voiceId = if (uiState.currentSkill.voiceId.isNotBlank()) uiState.currentSkill.voiceId else ttsVoice
                                                mgr.speak(stripParenthetical(message.content), voiceId) { speakingMessageId = null }
                                                speakingMessageId = message.id
                                            }
                                        }
                                    }
                                },
                                onCopy = {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("已复制", duration = SnackbarDuration.Short) }
                                },
                                isSpeaking = speakingMessageId == message.id,
                                isAutoSpeaking = autoSpeaking
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
    // Snackbar
    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
        ) { data ->
            Snackbar(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)),
                containerColor = Color(0xFFF5F5F5),
                contentColor = Color.Black,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = data.visuals.message,
                    color = Color.Black,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 去除 （）() 内的动作/场景描述，只保留正文 */
private fun stripParenthetical(text: String): String {
    val result = StringBuilder()
    var depth = 0
    for (c in text) {
        when (c) {
            '(', '（' -> depth++
            ')', '）' -> if (depth > 0) depth--
            else -> if (depth == 0) result.append(c)
        }
    }
    return result.toString().trim()
}

