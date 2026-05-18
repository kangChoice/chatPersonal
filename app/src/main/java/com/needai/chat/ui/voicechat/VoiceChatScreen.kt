package com.needai.chat.ui.voicechat

import android.graphics.BitmapFactory
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(
    skillId: String = "",
    onNavigateBack: () -> Unit,
    viewModel: VoiceChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleCall()
        } else {
            viewModel.updateError("需要麦克风权限才能通话")
        }
    }

    // 从单人聊天页面发起时：预选角色并自动发起通话
    LaunchedEffect(skillId) {
        if (skillId.isNotBlank()) {
            viewModel.preselectSkill(skillId)
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.toggleCall()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // 通话结束后自动返回聊天页面（从单人聊天进入时）
    var wasCallActive by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isCallActive) {
        if (uiState.isCallActive) {
            wasCallActive = true
        } else if (wasCallActive && skillId.isNotBlank()) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.conversationHistory.size) {
        if (uiState.conversationHistory.isNotEmpty()) {
            listState.animateScrollToItem(uiState.conversationHistory.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FluidGlowBackground()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("语音通话") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    if (uiState.isCallActive) {
                        IconButton(onClick = { viewModel.toggleCall() }) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "结束通话",
                                tint = Color(0xFFE53935)
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
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isCallActive) {
                CallActiveContent(uiState, listState, viewModel)
            } else if (skillId.isBlank()) {
                SkillSelectionContent(uiState, viewModel, permissionLauncher)
            }
            // skillId 非空（来自聊天页）：通话结束后不显示选择页，LaunchedEffect 处理返回

            // 错误提示（带步骤标识）
            if (uiState.error != null) {
                val stepLabel = when (uiState.errorStep) {
                    "ASR" -> "语音识别"
                    "LLM" -> "AI 对话"
                    "TTS" -> "语音合成"
                    "权限" -> "权限"
                    else -> null
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (stepLabel != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    text = stepLabel,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun SkillSelectionContent(
    uiState: VoiceChatUiState,
    viewModel: VoiceChatViewModel,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "选择通话角色",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "选择一个角色开始语音对话",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.allSkills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无角色，请先在角色管理页面创建",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.allSkills.chunked(3)) { rowSkills ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSkills.forEach { skill ->
                            SkillChip(
                                skill = skill,
                                isSelected = skill.id == uiState.selectedSkillId,
                                onClick = { viewModel.selectSkill(skill.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowSkills.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SelectedSkillInfoCard(uiState)

            Spacer(modifier = Modifier.height(16.dp))

            // 开始通话按钮
            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.toggleCall()
                    }
                },
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "开始通话",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }

            Text(
                text = "点击开始通话",
                modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SkillChip(
    skill: Skill,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarBitmap = remember(skill.avatarPath) {
        val path = skill.avatarPath
        if (path.isNotBlank()) {
            val f = java.io.File(path)
            if (f.exists()) {
                try { android.graphics.BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
            } else null
        } else null
    }

    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 固定 48dp 圆形容器，有图显示图，无图显示 emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GlassWhite),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = skill.avatar, fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = skill.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SelectedSkillInfoCard(uiState: VoiceChatUiState) {
    val context = LocalContext.current
    val selAvatarBitmap = remember(uiState.skillAvatarPath) {
        val path = uiState.skillAvatarPath
        if (path.isNotBlank()) {
            val f = java.io.File(path)
            if (f.exists()) {
                try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
            } else null
        } else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GlassWhite),
                    contentAlignment = Alignment.Center
                ) {
                    if (selAvatarBitmap != null) {
                        Image(
                            bitmap = selAvatarBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = uiState.skillAvatar, fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = uiState.skillName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "音色: ${uiState.currentVoiceDisplayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "模型: ${uiState.currentModelDisplayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CallActiveContent(
    uiState: VoiceChatUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    amplitudeProvider: VoiceChatViewModel
) {
    val amplitude by amplitudeProvider.voiceAmplitude.collectAsState()
    val context = LocalContext.current

    // 角色头像
    val callAvatarBitmap = remember(uiState.skillAvatarPath) {
        val path = uiState.skillAvatarPath
        if (path.isNotBlank()) {
            val f = java.io.File(path)
            if (f.exists()) {
                try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null }
            } else null
        } else null
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 当前角色信息（含音色/模型）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 角色头像（固定大小区域）
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GlassWhite),
                    contentAlignment = Alignment.Center
                ) {
                    if (callAvatarBitmap != null) {
                        Image(
                            bitmap = callAvatarBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = uiState.skillAvatar, fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.skillName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = uiState.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.isCallActive)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "音色: ${uiState.currentVoiceDisplayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // 对话记录
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.conversationHistory) { entry ->
                ChatBubble(entry)
            }
            if (uiState.partialText.isNotBlank()) {
                item {
                    Text(
                        text = uiState.partialText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 语音波形（用户说话时跳动，静音时静态）
        VoiceWaveform(
            amplitude = amplitude,
            isSpeaking = uiState.isSpeaking,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 语音波形动画组件。
 * 说话时 7 条竖状条跟随振幅跳动，静音时保持静态低高度。
 */
@Composable
private fun VoiceWaveform(
    amplitude: Int,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 7

    // 平滑振幅，避免动画过于突兀
    val smoothAmplitude by animateFloatAsState(
        targetValue = if (isSpeaking) amplitude.toFloat() / 255f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "amp"
    )

    // 各条高度因子，产生不均匀的跳动效果
    val heightFactors = remember { floatArrayOf(0.4f, 0.7f, 1.0f, 0.8f, 0.6f, 0.9f, 0.5f) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until barCount) {
            val barHeight by animateFloatAsState(
                targetValue = if (smoothAmplitude > 0.02f) {
                    (smoothAmplitude * heightFactors[index]).coerceAtLeast(0.15f)
                } else 0.12f,
                animationSpec = tween(durationMillis = 100 + index * 20),
                label = "bar$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((barHeight * 48).dp.coerceIn(4.dp, 48.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isSpeaking)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun ChatBubble(entry: ChatEntry) {
    val isUser = entry.role == "user"
    val bgColor = if (isUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isUser) "你" else "AI",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = bgColor
        ) {
            Text(
                text = entry.text,
                modifier = Modifier.padding(12.dp),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}
