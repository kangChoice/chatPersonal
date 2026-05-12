package com.needai.chat.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.chat.components.ChatInputBar
import com.needai.chat.ui.chat.components.HistorySessionSheet
import com.needai.chat.ui.chat.components.MessageBubble
import com.needai.chat.ui.chat.components.SkillSelectorSheet
import com.needai.chat.ui.chat.components.StreamingBubble
import com.needai.chat.ui.chat.state.ChatUiState
import com.needai.chat.util.TtsManager
import com.needai.chat.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSkillSelector by remember { mutableStateOf(false) }
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
    val ttsManager = remember { TtsManager(context) }

    LaunchedEffect(ttsManager) {
        ttsManager.onDone = {
            speakingMessageId = null
        }
    }

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    // Launcher for exporting current session
    val exportCurrentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            viewModel.exportCurrentSessionToFile(context, uri)
        }
    }

    // Launcher for exporting a history session
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
            viewModel.importSession(context, uri) { success, msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    // Show error/success as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size, uiState.currentStreamingMessage) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.currentSkill.avatar + " " + uiState.currentSkill.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.currentModelName.ifEmpty {
                                if (uiState.currentModel == com.needai.chat.domain.model.ModelType.REMOTE) "远程模型" else "本地模型"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    if (!uiState.isModelConfigured) {
                        Box {
                            IconButton(onClick = { showModelTip = true }) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "未配置模型",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            DropdownMenu(
                                expanded = showModelTip,
                                onDismissRequest = { showModelTip = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("当前未配置或选择模型！") },
                                    onClick = { showModelTip = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showSkillSelector = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "切换技能")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Add, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("历史会话") },
                                onClick = {
                                    showMenu = false
                                    showHistorySession = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导出会话") },
                                onClick = {
                                    showMenu = false
                                    showExportDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导入会话") },
                                onClick = {
                                    showMenu = false
                                    importSessionLauncher.launch(arrayOf("text/*", "*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("新建对话") },
                                onClick = {
                                    viewModel.newSession()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("清空上下文") },
                                onClick = {
                                    viewModel.clearSession()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = uiState.inputText,
                isStreaming = uiState.isStreaming,
                onInputChanged = viewModel::onInputChanged,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopStreaming
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    ) { innerPadding ->
        if (uiState.messages.isEmpty() && !uiState.isStreaming) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.currentSkill.avatar,
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.currentSkill.greeting,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = uiState.messages.filter { it.role != com.needai.chat.domain.model.MessageRole.SYSTEM },
                    key = { it.id }
                ) { message ->
                    MessageBubble(
                        message = message,
                        onSpeak = {
                            if (speakingMessageId == message.id) {
                                ttsManager.stop()
                                speakingMessageId = null
                            } else {
                                ttsManager.speak(message.content)
                                speakingMessageId = message.id
                            }
                        },
                        isSpeaking = speakingMessageId == message.id
                    )
                }

                if (uiState.isStreaming && uiState.currentStreamingMessage.isNotEmpty()) {
                    item {
                        StreamingBubble(
                            content = uiState.currentStreamingMessage,
                            isStreaming = true
                        )
                    }
                }
            }
        }
    }

    // Skill selector bottom sheet
    if (showSkillSelector) {
        SkillSelectorSheet(
            skills = uiState.availableSkills,
            currentSkillId = uiState.currentSkill.id,
            onSkillSelected = { skill ->
                if (skill.id != uiState.currentSkill.id) {
                    pendingSkill = skill
                }
            },
            onDismiss = { showSkillSelector = false }
        )
    }

    // History session sheet
    if (showHistorySession) {
        HistorySessionSheet(
            sessions = uiState.historySessions,
            currentSessionId = uiState.sessionId,
            onSessionSelected = { session ->
                viewModel.switchToHistorySession(session)
            },
            onDeleteSession = { session ->
                sessionToDelete = session
            },
            onDismiss = { showHistorySession = false }
        )
    }

    // Confirm skill switch
    if (pendingSkill != null) {
        AlertDialog(
            onDismissRequest = { pendingSkill = null },
            title = { Text("切换技能") },
            text = {
                Text("切换到「${pendingSkill!!.name}」将开启新的对话，确定要切换吗？")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.switchSkill(pendingSkill!!)
                    pendingSkill = null
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSkill = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete session confirmation dialog
    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除会话") },
            text = {
                Text("确定要删除会话「${sessionToDelete!!.title}」吗？此操作不可撤销。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(sessionToDelete!!.id)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Export session dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("导出会话") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "选择要导出的会话：",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Current session option
                    Surface(
                        onClick = {
                            showExportDialog = false
                            if (uiState.messages.isNotEmpty()) {
                                val fileName = "chat_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.md"
                                exportCurrentLauncher.launch(fileName)
                            } else {
                                viewModel.exportSessionToFile(context, "", android.net.Uri.EMPTY)
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "当前会话",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${uiState.currentSkill.name} · ${uiState.messages.size}条消息",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // History sessions
                    uiState.historySessions.forEach { session ->
                        Surface(
                            onClick = {
                                showExportDialog = false
                                pendingExportSessionId = session.id
                                val fileName = "chat_${session.title.take(20).replace(" ", "_")}_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.md"
                                exportHistoryLauncher.launch(fileName)
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${session.skillName} · ${session.messageCount}条消息",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    if (uiState.messages.isEmpty() && uiState.historySessions.isEmpty()) {
                        Text(
                            text = "没有可导出的会话",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
