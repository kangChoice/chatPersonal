package com.needai.chat.ui.multichat

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.needai.chat.ui.theme.*
import androidx.activity.compose.BackHandler

@Composable
fun MultiChatScreen(
    viewModel: MultiChatViewModel = hiltViewModel(),
    onChatDetailChange: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showHistorySession by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
    var showSetup by remember { mutableStateOf(true) }

    // Notify parent about detail state for bottom nav
    LaunchedEffect(showSetup) { onChatDetailChange(!showSetup) }

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

    // ===== 清除输入 =====
    LaunchedEffect(showSetup) {
        if (showSetup) viewModel.onInputChanged("")
    }

    // ===== 系统返回手势 =====
    BackHandler(enabled = !showSetup) {
        showSetup = true
    }

    if (showSetup) {
        // ========== 前置角色选择页面 ==========
        MultiChatSetupPage(
            availableSkills = uiState.availableSkills,
            selectedSkills = uiState.selectedSkills,
            multiPrompt = uiState.multiPrompt,
            historySessionCount = uiState.historySessions.size,
            onToggle = viewModel::toggleSkillSelection,
            onPromptChanged = viewModel::onMultiPromptChanged,
            onConfirm = {
                if (uiState.selectedSkills.size >= 2) {
                    showSetup = false
                }
            },
            onHistorySession = { showHistorySession = true }
        )
    } else {
        // ========== 群聊详细页面 ==========
        Box(modifier = Modifier.fillMaxSize().imePadding()) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ===== 顶部导航栏 =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { showSetup = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("<", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("多人聊天", fontWeight = FontWeight.Bold)
                        if (uiState.selectedSkills.isNotEmpty()) {
                            Text(
                                text = "已选 ${uiState.selectedSkills.size} 个角色",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GlassWhite)
                                .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                .clickable { viewModel.togglePromptEditor() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "提示词配置", tint = BrandBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GlassWhite)
                                .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                .clickable { viewModel.toggleSkillSelector() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "选择角色", tint = BrandPink, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GlassWhite)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .clickable { showMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "更多", tint = BrandBlue, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier
                                    .background(GlassWhite, RoundedCornerShape(16.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("新建群聊", fontSize = 14.sp, color = TextPrimary) },
                                    onClick = {
                                        viewModel.newSession()
                                        showMenu = false
                                        showSetup = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("历史会话", fontSize = 14.sp, color = TextPrimary) },
                                    onClick = {
                                        showMenu = false
                                        showHistorySession = true
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 0.5.dp)
                                DropdownMenuItem(
                                    text = { Text("清空对话", fontSize = 14.sp, color = BrandPink) },
                                    onClick = {
                                        viewModel.clearMessages()
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ===== 已选角色标签 =====
                if (uiState.selectedSkills.isNotEmpty()) {
                    SelectedSkillsRow(
                        selectedSkills = uiState.selectedSkills,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // ===== 消息列表 =====
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.messages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "点击下方输入框开始群聊",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(uiState.messages, key = { it.id }) { message ->
                                MultiChatMessageBubble(
                                    message = message
                                )
                            }
                        }
                    }
                }

                // ===== 当前回复角色指示 =====
                if (uiState.isGenerating && uiState.currentRespondingSkill != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = uiState.currentRespondingSkill!!.avatar, fontSize = 14.sp)
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

                // ===== 清空按钮 =====
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

                // ===== 输入栏 =====
                ChatInputBar(
                    inputText = uiState.inputText,
                    isStreaming = uiState.isGenerating,
                    onInputChanged = viewModel::onInputChanged,
                    onSend = viewModel::sendMessage,
                    onStop = viewModel::stopGeneration
                )
            }
        }

        // Dialogs
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

        // Snackbar overlay
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

    // Shared dialogs
    if (showHistorySession) {
        HistorySessionSheet(
            sessions = uiState.historySessions,
            currentSessionId = uiState.sessionId,
            onSessionSelected = { session ->
                viewModel.switchToSession(session)
                showSetup = false
            },
            onDeleteSession = { session ->
                sessionToDelete = session
            },
            onDismiss = { showHistorySession = false }
        )
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除会话") },
            text = { Text("确定要删除会话「${sessionToDelete!!.title}」吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(sessionToDelete!!.id)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { sessionToDelete = null }) { Text("取消") } }
        )
    }
}

// ========== 前置角色选择页面 ==========
@Composable
private fun MultiChatSetupPage(
    availableSkills: List<Skill>,
    selectedSkills: List<Skill>,
    multiPrompt: String,
    historySessionCount: Int = 0,
    onToggle: (Skill) -> Unit,
    onPromptChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onHistorySession: () -> Unit
) {
    // ... (unchanged, same as before)
    var promptText by remember(multiPrompt) { mutableStateOf(multiPrompt) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("多人聊天", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text("选择至少 2 个角色，共同参与群聊", fontSize = 13.sp, color = TextTertiary)
            Spacer(Modifier.height(8.dp))
            if (historySessionCount > 0) {
                TextButton(onClick = onHistorySession) {
                    Text("查看历史会话 ($historySessionCount)", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("群聊氛围设定", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it; onPromptChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                placeholder = { Text("设定群聊互动基调...", fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("选择角色", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "已选 ${selectedSkills.size} 个",
                    fontSize = 12.sp,
                    color = if (selectedSkills.size >= 2) BrandMint else TextTertiary
                )
            }
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(availableSkills, key = { it.id }) { skill ->
                    val isSelected = selectedSkills.any { it.id == skill.id }
                    SkillSelectItem(skill = skill, isSelected = isSelected, onClick = { onToggle(skill) })
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onConfirm,
                enabled = selectedSkills.size >= 2,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSkills.size >= 2) BrandBlue else TextTertiary.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    if (selectedSkills.size < 2) "至少选择 2 个角色 (${selectedSkills.size}/2)"
                    else "开始群聊 (${selectedSkills.size} 个角色)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SkillSelectItem(
    skill: Skill,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) BrandBlue.copy(alpha = 0.1f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.5.dp,
                color = if (isSelected) BrandBlue.copy(alpha = 0.4f) else DividerColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = skill.avatar, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = skill.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            if (skill.description.isNotBlank()) {
                Text(text = skill.description, fontSize = 11.sp, color = TextTertiary, maxLines = 1)
            }
        }
        Checkbox(checked = isSelected, onCheckedChange = { onClick() }, colors = CheckboxDefaults.colors(checkedColor = BrandBlue))
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
        title = { Text("选择角色", fontWeight = FontWeight.Bold) },
        text = {
            if (availableSkills.isEmpty()) {
                Text("没有可用的角色", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(availableSkills, key = { it.id }) { skill ->
                        val isSelected = selectedSkills.any { it.id == skill.id }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onToggle(skill) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = { onToggle(skill) })
                            Spacer(Modifier.width(8.dp))
                            Text(text = skill.avatar, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(text = skill.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(text = skill.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val count = selectedSkills.size
            TextButton(onClick = onDismiss, enabled = count >= 2) {
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
                Text("该提示词将注入到每个角色中，让它们按此基调互动：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3, maxLines = 6,
                    label = { Text("提示词") },
                    placeholder = { Text("例如：你们都喜欢我，互相反驳对方的话...") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPromptChanged(text); onDismiss() }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
