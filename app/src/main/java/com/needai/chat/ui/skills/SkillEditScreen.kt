package com.needai.chat.ui.skills

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VolumeUp
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
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.remote.tts.SystemVoiceProvider
import com.needai.chat.ui.settings.components.VoiceSelectorSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillEditScreen(
    skillId: String,
    onNavigateBack: () -> Unit,
    viewModel: SkillViewModel = hiltViewModel()
) {
    val isNew = skillId == "new"
    val skills by viewModel.skills.collectAsState()
    val existingSkill = remember(skills, skillId) {
        skills.find { it.id == skillId }
    }

    var name by remember(existingSkill) { mutableStateOf(existingSkill?.name ?: "") }
    var description by remember(existingSkill) { mutableStateOf(existingSkill?.description ?: "") }
    var systemPrompt by remember(existingSkill) { mutableStateOf(existingSkill?.systemPrompt ?: "") }
    var avatar by remember(existingSkill) { mutableStateOf(existingSkill?.avatar ?: "🤖") }
    var greeting by remember(existingSkill) { mutableStateOf(existingSkill?.greeting ?: "你好！") }
    var temperature by remember(existingSkill) { mutableStateOf(existingSkill?.temperature?.toString() ?: "0.7") }
    var voiceId by remember(existingSkill) { mutableStateOf(existingSkill?.voiceId ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSystemPromptDialog by remember { mutableStateOf(false) }
    var showVoiceSelector by remember { mutableStateOf(false) }

    val isBuiltin = existingSkill?.isBuiltin == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "创建技能" else "编辑技能", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!isNew && !isBuiltin) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("技能名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBuiltin
            )

            OutlinedTextField(
                value = avatar,
                onValueChange = { avatar = it },
                label = { Text("头像 (Emoji)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBuiltin
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBuiltin
            )

            OutlinedTextField(
                value = greeting,
                onValueChange = { greeting = it },
                label = { Text("问候语") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBuiltin
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isBuiltin) { showSystemPromptDialog = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "系统提示词 (System Prompt)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = systemPrompt.ifEmpty { "点击编辑系统提示词..." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (systemPrompt.isEmpty())
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!isBuiltin) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = temperature,
                onValueChange = { temperature = it },
                label = { Text("温度 (0.0 - 2.0)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBuiltin,
                isError = temperature.isNotEmpty() && temperature.toDoubleOrNull() == null,
                supportingText = {
                    if (temperature.isNotEmpty() && temperature.toDoubleOrNull() == null) {
                        Text("请输入有效数字")
                    }
                }
            )

            // 音色选择
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isBuiltin) { showVoiceSelector = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "音色",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (voiceId.isNotBlank()) voiceId else "未关联音色",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (voiceId.isNotBlank())
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                    if (!isBuiltin) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "选择",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isNotBlank() && systemPrompt.isNotBlank()) {
                        val temp = (temperature.toDoubleOrNull() ?: 0.7).coerceIn(0.0, 2.0)
                        if (isNew) {
                            viewModel.createSkill(name, description, systemPrompt, avatar, greeting, temp, voiceId = voiceId)
                        } else if (existingSkill != null && !isBuiltin) {
                            viewModel.updateSkill(
                                existingSkill.copy(
                                    name = name,
                                    description = description,
                                    systemPrompt = systemPrompt,
                                    avatar = avatar,
                                    greeting = greeting,
                                    temperature = temp,
                                    voiceId = voiceId
                                )
                            )
                        }
                        onNavigateBack()
                    }
                },
                enabled = name.isNotBlank() && systemPrompt.isNotBlank() && (isNew || !isBuiltin),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isNew) "创建技能"
                    else if (isBuiltin) "返回"
                    else "保存修改"
                )
            }
        }
    }

    if (showSystemPromptDialog) {
        SystemPromptEditDialog(
            initialPrompt = systemPrompt,
            isBuiltin = isBuiltin,
            onDismiss = { showSystemPromptDialog = false },
            onSave = { newPrompt ->
                systemPrompt = newPrompt
                showSystemPromptDialog = false
            }
        )
    }

    if (showVoiceSelector) {
        val context = LocalContext.current
        val settingsDataStore = remember { SettingsDataStore(context) }
        val ttsModel by settingsDataStore.ttsModel.collectAsState(initial = "cosyvoice-v3.5-flash")
        val customVoices by viewModel.customVoices.collectAsStateWithLifecycle()
        val systemVoices = remember(ttsModel) { SystemVoiceProvider.getVoices(ttsModel) }
        VoiceSelectorSheet(
            systemVoices = systemVoices,
            customVoices = customVoices,
            currentVoiceId = voiceId,
            selectedModel = ttsModel,
            onVoiceSelected = { selected ->
                voiceId = selected
                showVoiceSelector = false
            },
            onManageVoices = {
                showVoiceSelector = false
                // Navigate to voice management (handled by caller)
            },
            onDismiss = { showVoiceSelector = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除技能") },
            text = { Text("确定要删除「${existingSkill?.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSkill(skillId)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
