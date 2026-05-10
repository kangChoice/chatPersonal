package com.needai.chat.ui.chat

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.needai.chat.ui.chat.components.ChatInputBar
import com.needai.chat.ui.chat.components.HistorySessionSheet
import com.needai.chat.ui.chat.components.MessageBubble
import com.needai.chat.ui.chat.components.SkillSelectorSheet
import com.needai.chat.ui.chat.components.StreamingBubble
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.chat.state.ChatUiState
import com.needai.chat.ui.navigation.Screen

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
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error as snackbar
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
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "未配置远程模型",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 4.dp)
                        )
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
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        if (uiState.messages.isEmpty() && !uiState.isStreaming) {
            // Empty state
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
                    MessageBubble(message = message)
                }

                // Streaming bubble
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
}
