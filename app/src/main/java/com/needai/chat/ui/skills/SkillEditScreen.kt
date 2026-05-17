package com.needai.chat.ui.skills

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.remote.tts.SystemVoiceProvider
import com.needai.chat.ui.settings.components.VoiceSelectorSheet
import com.needai.chat.ui.theme.*
import com.needai.chat.util.AvatarUtils
import java.io.File

/**
 * 博客资料页风格的编辑角色页面
 * 布局: 头像(英雄区) → 基本信息卡片 → 描述卡片 → 系统提示词卡片 → 问候语卡片 → 音色 → 保存按钮
 */
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
    var avatarPath by remember(existingSkill) { mutableStateOf(existingSkill?.avatarPath ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSystemPromptDialog by remember { mutableStateOf(false) }
    var showVoiceSelector by remember { mutableStateOf(false) }

    val isBuiltin = existingSkill?.isBuiltin == true
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    val voiceAliases by settingsDataStore.voiceAliases.collectAsState(initial = emptyMap())

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = AvatarUtils.saveAvatar(context, existingSkill?.id ?: skillId, uri)
            if (savedPath != null) {
                avatarPath = savedPath
            }
        }
    }

    val avatarBitmap = remember(avatarPath) {
        if (avatarPath.isNotBlank()) {
            val f = File(avatarPath)
            if (f.exists()) BitmapFactory.decodeFile(avatarPath) else null
        } else null
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "创建角色" else "编辑角色", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==================== 头像英雄区 ====================
            EditGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(GlassWhite)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = "角色头像",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = avatar, fontSize = 40.sp)
                        }
                    }

                    if (!isBuiltin) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(BrandBlue.copy(alpha = 0.12f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { avatarPickerLauncher.launch("image/*") }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("更换头像", fontSize = 13.sp, color = BrandBlue)
                            }
                        }
                    }
                }
            }

            // ==================== 基本信息卡片 ====================
            EditGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionLabel("基本信息")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("角色名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBuiltin,
                        colors = editFieldColors()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = avatar,
                            onValueChange = { avatar = it },
                            label = { Text("表情") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            enabled = !isBuiltin,
                            supportingText = { Text("未设图片时展示", fontSize = 11.sp) },
                            colors = editFieldColors()
                        )
                        OutlinedTextField(
                            value = temperature,
                            onValueChange = { temperature = it },
                            label = { Text("温度") },
                            singleLine = true,
                            modifier = Modifier.width(110.dp),
                            enabled = !isBuiltin,
                            isError = temperature.isNotEmpty() && temperature.toDoubleOrNull() == null,
                            supportingText = {
                                if (temperature.isNotEmpty() && temperature.toDoubleOrNull() == null) {
                                    Text("无效", fontSize = 11.sp)
                                }
                            },
                            colors = editFieldColors()
                        )
                    }
                }
            }

            // ==================== 角色描述 ====================
            EditGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SectionLabel("角色描述")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("描述这个角色的风格、说话方式、性格…") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBuiltin,
                        colors = editFieldColors()
                    )
                }
            }

            // ==================== 系统提示词 ====================
            EditGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !isBuiltin,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSystemPromptDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("系统提示词")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = systemPrompt.ifEmpty { "点击编辑…" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (systemPrompt.isEmpty())
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!isBuiltin) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = BrandBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .offset(y = 2.dp)
                        )
                    }
                }
            }

            // ==================== 问候语 ====================
            EditGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SectionLabel("问候语")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = greeting,
                        onValueChange = { greeting = it },
                        placeholder = { Text("角色首次对话时的开场白…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBuiltin,
                        colors = editFieldColors()
                    )
                }
            }

            // ==================== 音色 ====================
            EditGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !isBuiltin,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showVoiceSelector = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("音色")
                        Text(
                            text = if (voiceId.isNotBlank())
                                (voiceAliases[voiceId]?.ifBlank { null } ?: "--")
                            else "未关联音色",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (voiceId.isNotBlank())
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    if (!isBuiltin) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "选择",
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ==================== 保存按钮 ====================
            Button(
                onClick = {
                    if (name.isNotBlank() && systemPrompt.isNotBlank()) {
                        val temp = (temperature.toDoubleOrNull() ?: 0.7).coerceIn(0.0, 2.0)
                        if (isNew) {
                            viewModel.createSkill(
                                name, description, systemPrompt, avatar, greeting, temp,
                                voiceId = voiceId, avatarPath = avatarPath
                            )
                        } else if (existingSkill != null && !isBuiltin) {
                            viewModel.updateSkill(
                                existingSkill.copy(
                                    name = name, description = description,
                                    systemPrompt = systemPrompt, avatar = avatar,
                                    greeting = greeting, temperature = temp,
                                    voiceId = voiceId, avatarPath = avatarPath
                                )
                            )
                        }
                        onNavigateBack()
                    }
                },
                enabled = name.isNotBlank() && systemPrompt.isNotBlank() && (isNew || !isBuiltin),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (isNew) "创建角色"
                    else if (isBuiltin) "返回"
                    else "保存修改",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
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
        val customVoices by viewModel.customVoices.collectAsStateWithLifecycle()
        val systemVoices = remember { SystemVoiceProvider.getSkillEditorVoices() }
        VoiceSelectorSheet(
            systemVoices = systemVoices,
            customVoices = customVoices,
            currentVoiceId = voiceId,
            selectedModel = "cosyvoice-v3-flash",
            voiceAliases = voiceAliases,
            onVoiceSelected = { selected ->
                voiceId = selected
                showVoiceSelector = false
            },
            onDismiss = { showVoiceSelector = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除角色") },
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

// ==================== 辅助组件 ====================

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
}

@Composable
private fun EditGlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandBlue,
    cursorColor = BrandBlue,
    focusedLabelColor = BrandBlue
)
