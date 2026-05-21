package com.needai.chat.ui.schedule

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.domain.model.AiNotificationConfig
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.theme.BgCard
import com.needai.chat.ui.theme.BgPage
import com.needai.chat.ui.theme.BrandBlue
import com.needai.chat.ui.theme.BrandMint
import com.needai.chat.ui.theme.StatusRed
import com.needai.chat.ui.theme.TextPrimary
import com.needai.chat.ui.theme.TextSecondary
import com.needai.chat.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiNotificationScreen(
    viewModel: AiNotificationViewModel = hiltViewModel()
) {
    val configs by viewModel.configs.collectAsStateWithLifecycle()
    val availableSkills by viewModel.availableSkills.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<AiNotificationConfig?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingConfig by remember { mutableStateOf<AiNotificationConfig?>(null) }

    var notificationsEnabled by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.areNotificationsEnabled()
            } else true
        )
    }

    var pendingToggleConfig by remember { mutableStateOf<AiNotificationConfig?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(NotificationManager::class.java)
            notificationsEnabled = manager.areNotificationsEnabled()
        }
        if (granted || notificationsEnabled) {
            pendingToggleConfig?.let { viewModel.toggleEnabled(it) }
        } else {
            // 用户拒绝，或之前勾了"不再询问"导致系统未弹窗
            val activity = context as? android.app.Activity
            val canAskAgain = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
            } ?: true
            if (!canAskAgain) {
                // 永久拒绝，直接跳系统通知设置
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
        pendingToggleConfig = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Android 13+ 通知权限引导
        if (!notificationsEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = StatusRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "通知权限未开启",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatusRed
                        )
                        Text(
                            text = "定时通知需要通知权限才能弹出提醒",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("去开启")
                    }
                }
            }
        }

        // AI 通知任务列表
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI 通知",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (configs.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.testTrigger() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("测试", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (configs.isEmpty()) {
                    Text(
                        text = "暂无定时通知，点击下方添加",
                        fontSize = 13.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    configs.forEach { config ->
                        AiNotificationRow(
                            config = config,
                            onEdit = {
                                editingConfig = config
                                showEditDialog = true
                            },
                            onDelete = {
                                deletingConfig = config
                                showDeleteDialog = true
                            },
                            onToggle = {
                                if (config.enabled) {
                                    viewModel.toggleEnabled(config)
                                } else if (!notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingToggleConfig = config
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.toggleEnabled(config)
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        editingConfig = null
                        showEditDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("添加 AI 通知")
                }
            }
        }
    }

    // 添加/编辑对话框
    if (showEditDialog) {
        AiNotificationEditDialog(
            initial = editingConfig,
            skills = availableSkills,
            onDismiss = { showEditDialog = false },
            onSave = { config ->
                if (editingConfig != null) {
                    viewModel.update(config)
                } else {
                    viewModel.add(config)
                }
                showEditDialog = false
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog && deletingConfig != null) {
        val config = deletingConfig!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed) },
            title = { Text("删除 AI 通知", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("确定删除【${String.format("%02d:%02d", config.hour, config.minute)}】${config.skillName} 的通知吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(config.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AiNotificationRow(
    config: AiNotificationConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = BgCard
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.skillAvatar,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = config.skillName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "每天 ${String.format("%02d:%02d", config.hour, config.minute)}",
                    fontSize = 13.sp,
                    color = BrandBlue
                )
                Text(
                    text = "提示：${config.prompt}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
                if (config.lastTriggeredAt != null) {
                    val timeStr = java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(config.lastTriggeredAt))
                    Text(
                        text = "上次触发：$timeStr",
                        fontSize = 11.sp,
                        color = BrandMint
                    )
                }
            }
            Switch(
                checked = config.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 4.dp)
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = StatusRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiNotificationEditDialog(
    initial: AiNotificationConfig?,
    skills: List<Skill>,
    onDismiss: () -> Unit,
    onSave: (AiNotificationConfig) -> Unit
) {
    var selectedSkill by remember { mutableStateOf<Skill?>(null) }

    // 新建时，skills 可能还未加载完成，这里需要等待
    LaunchedEffect(skills) {
        if (initial != null && selectedSkill == null) {
            selectedSkill = skills.find { it.id == initial.skillId }
        }
    }

    var hourText by remember { mutableStateOf(initial?.let { String.format("%02d", it.hour) } ?: "08") }
    var minuteText by remember { mutableStateOf(initial?.let { String.format("%02d", it.minute) } ?: "00") }
    var prompt by remember { mutableStateOf(initial?.prompt ?: "") }
    var hourError by remember { mutableStateOf(false) }
    var minuteError by remember { mutableStateOf(false) }
    var showSkillPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "添加 AI 通知" else "编辑 AI 通知",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 角色选择
                Text(
                    text = "角色",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Surface(
                    onClick = { showSkillPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = BgPage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedSkill?.let { "${it.avatar} ${it.name}" } ?: "点击选择角色",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = if (selectedSkill != null) TextPrimary else TextTertiary
                    )
                }

                // 时间
                Text(
                    text = "时间",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = {
                            hourText = it
                            hourError = false
                        },
                        label = { Text("时") },
                        placeholder = { Text("08") },
                        singleLine = true,
                        isError = hourError,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(":", color = TextTertiary, fontSize = 18.sp)
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = {
                            minuteText = it
                            minuteError = false
                        },
                        label = { Text("分") },
                        placeholder = { Text("00") },
                        singleLine = true,
                        isError = minuteError,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 提示文本
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("提示文本") },
                    placeholder = { Text("如：请给我发一条早安问候") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("AI 将以角色身份回应这段提示") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val hour = hourText.toIntOrNull()
                val min = minuteText.toIntOrNull()
                hourError = hour == null || hour !in 0..23
                minuteError = min == null || min !in 0..59

                if (!hourError && !minuteError && hour != null && min != null && prompt.isNotBlank() && selectedSkill != null) {
                    val config = AiNotificationConfig(
                        id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                        skillId = selectedSkill!!.id,
                        skillName = selectedSkill!!.name,
                        skillAvatar = selectedSkill!!.avatar,
                        prompt = prompt.trim(),
                        hour = hour,
                        minute = min,
                        enabled = initial?.enabled ?: true,
                        createdAt = initial?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(config)
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 角色选择器
    if (showSkillPicker) {
        AlertDialog(
            onDismissRequest = { showSkillPicker = false },
            title = { Text("选择角色", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    skills.forEach { skill ->
                        Surface(
                            onClick = {
                                selectedSkill = skill
                                showSkillPicker = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedSkill?.id == skill.id) BrandBlue.copy(alpha = 0.1f) else BgPage,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = skill.avatar, fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = skill.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = skill.description,
                                        fontSize = 12.sp,
                                        color = TextTertiary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSkillPicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}
