package com.needai.chat.ui.multichat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.chat.components.ChatInputBar
import com.needai.chat.ui.chat.components.HistorySessionSheet
import com.needai.chat.ui.multichat.components.MultiChatMessageBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiChatScreen(
    viewModel: MultiChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showHistorySession by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("多人聊天", fontWeight = FontWeight.Bold)
                        if (uiState.selectedSkills.isNotEmpty()) {
                            Text(
                                text = "已选 ${uiState.selectedSkills.size} 个技能",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePromptEditor() }) {
                        Icon(Icons.Default.Edit, contentDescription = "提示词配置")
                    }
                    BadgedBox(
                        badge = {
                            if (uiState.selectedSkills.isNotEmpty()) {
                                Badge { Text("${uiState.selectedSkills.size}") }
                            }
                        }
                    ) {
                        IconButton(onClick = { viewModel.toggleSkillSelector() }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "选择技能")
                        }
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
                                text = { Text("新建群聊") },
                                onClick = {
                                    viewModel.newSession()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("历史会话") },
                                onClick = {
                                    showMenu = false
                                    showHistorySession = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("清空对话") },
                                onClick = {
                                    viewModel.clearMessages()
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
                isStreaming = uiState.isGenerating,
                onInputChanged = viewModel::onInputChanged,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopGeneration
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Selected skills chips
            if (uiState.selectedSkills.isNotEmpty()) {
                SelectedSkillsRow(
                    selectedSkills = uiState.selectedSkills,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Empty state
            if (uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "多人聊天",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "选择至少 2 个技能，发送消息开始聊天",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        if (uiState.historySessions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { showHistorySession = true }) {
                                Text("查看历史会话 (${uiState.historySessions.size})")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MultiChatMessageBubble(message = message)
                    }
                }
            }

            // Current responding skill indicator
            if (uiState.isGenerating && uiState.currentRespondingSkill != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = uiState.currentRespondingSkill!!.avatar,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${uiState.currentRespondingSkill!!.name} 正在输入...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.width(80.dp).height(2.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Clear button (when messages exist and not generating)
            if (uiState.messages.isNotEmpty() && !uiState.isGenerating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = viewModel::clearMessages) {
                        Text("清空对话", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (uiState.showSkillSelector) {
        SkillSelectorDialog(
            availableSkills = uiState.availableSkills,
            selectedSkills = uiState.selectedSkills,
            onToggle = viewModel::toggleSkillSelection,
            onDismiss = viewModel::toggleSkillSelector
        )
    }

    if (uiState.showPromptEditor) {
        MultiPromptEditorDialog(
            currentPrompt = uiState.multiPrompt,
            onPromptChanged = viewModel::onMultiPromptChanged,
            onDismiss = viewModel::togglePromptEditor
        )
    }

    if (showHistorySession) {
        HistorySessionSheet(
            sessions = uiState.historySessions,
            currentSessionId = uiState.sessionId,
            onSessionSelected = { session ->
                viewModel.switchToSession(session)
            },
            onDeleteSession = { session ->
                sessionToDelete = session
            },
            onDismiss = { showHistorySession = false }
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
}

@Composable
private fun SelectedSkillsRow(
    selectedSkills: List<Skill>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        selectedSkills.forEach { skill ->
            AssistChip(
                onClick = {},
                label = { Text("${skill.avatar} ${skill.name}", maxLines = 1) },
                shape = MaterialTheme.shapes.small
            )
        }
    }
}

@Composable
private fun SkillSelectorDialog(
    availableSkills: List<Skill>,
    selectedSkills: List<Skill>,
    onToggle: (Skill) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择技能", fontWeight = FontWeight.Bold) },
        text = {
            if (availableSkills.isEmpty()) {
                Text(
                    text = "没有可用的技能",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(availableSkills, key = { it.id }) { skill ->
                        val isSelected = selectedSkills.any { it.id == skill.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(skill) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = { onToggle(skill) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = skill.avatar, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = skill.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = skill.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val count = selectedSkills.size
            TextButton(
                onClick = onDismiss,
                enabled = count >= 2
            ) {
                Text(if (count < 2) "至少选 2 个 ($count/2)" else "确定 ($count)")
            }
        }
    )
}

@Composable
private fun MultiPromptEditorDialog(
    currentPrompt: String,
    onPromptChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentPrompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("多人聊天提示词", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "该提示词将注入到每个技能中，让它们按此基调互动：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    label = { Text("提示词") },
                    placeholder = { Text("例如：你们都喜欢我，互相反驳对方的话...") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onPromptChanged(text)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
