package com.needai.chat.ui.schedule

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.domain.model.AiNotificationConfig
import com.needai.chat.domain.model.NotificationTemplate
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.theme.BgCard
import com.needai.chat.ui.theme.BgPage
import com.needai.chat.ui.theme.BrandBlue
import com.needai.chat.ui.theme.BrandMint
import com.needai.chat.ui.theme.BrandPink
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
    val globalEnabled by viewModel.globalEnabled.collectAsStateWithLifecycle()
    val availableSkills by viewModel.availableSkills.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
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
                            text = "AI定时通知需要通知权限才能弹出提醒",
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = globalEnabled,
                            onCheckedChange = { viewModel.setGlobalEnabled(it) }
                        )
                        if (globalEnabled && configs.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.testTrigger() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("通知测试", fontSize = 12.sp)
                            }
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
                            templates = templates,
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

        Spacer(Modifier.height(16.dp))
        SchedulePermissionCards()
    }

    // 添加/编辑对话框
    if (showEditDialog) {
        AiNotificationEditDialog(
            initial = editingConfig,
            skills = availableSkills,
            templates = templates,
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
    templates: List<NotificationTemplate>,
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
                val matchedTemplate = templates.find { it.prompt == config.prompt }
                if (matchedTemplate != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = BrandPink.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = matchedTemplate.label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                fontSize = 12.sp,
                                color = BrandPink,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Text(
                        text = config.prompt.orEmpty(),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
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
    templates: List<NotificationTemplate>,
    onDismiss: () -> Unit,
    onSave: (AiNotificationConfig) -> Unit
) {
    var selectedSkill by remember { mutableStateOf<Skill?>(null) }

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
    var skillError by remember { mutableStateOf(false) }
    var promptError by remember { mutableStateOf(false) }
    var showSkillPicker by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }

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
                    text = "角色 *",
                    fontSize = 14.sp,
                    color = if (skillError) StatusRed else TextSecondary
                )
                Surface(
                    onClick = { showSkillPicker = true; skillError = false },
                    shape = RoundedCornerShape(12.dp),
                    color = BgPage,
                    modifier = Modifier.fillMaxWidth(),
                    border = if (skillError) androidx.compose.foundation.BorderStroke(1.dp, StatusRed) else null
                ) {
                    Text(
                        text = selectedSkill?.let { "${it.avatar} ${it.name}" } ?: "点击选择角色",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = if (selectedSkill != null) TextPrimary else TextTertiary
                    )
                }
                if (skillError) {
                    Text(
                        text = "请选择一个角色",
                        fontSize = 12.sp,
                        color = StatusRed
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

                // 快速模板
                Text(
                    text = "语气模板",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.take(6).forEach { template ->
                        Surface(
                            onClick = { prompt = template.prompt },
                            shape = RoundedCornerShape(16.dp),
                            color = if (prompt == template.prompt) BrandBlue else BrandBlue.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = template.label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = if (prompt == template.prompt) androidx.compose.ui.graphics.Color.White else BrandBlue,
                                maxLines = 1
                            )
                        }
                    }
                    if (templates.size > 6) {
                        Surface(
                            onClick = { showTemplateSheet = true },
                            shape = RoundedCornerShape(16.dp),
                            color = BrandMint.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = "更多模板",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = BrandMint
                            )
                        }
                    }
                }

                // 提示文本
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it; promptError = false },
                    label = { Text("提示文本 *") },
                    placeholder = { Text("或自定义提示...") },
                    singleLine = false,
                    maxLines = 3,
                    isError = promptError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        if (promptError) {
                            Text("提示文本不能为空", color = StatusRed, fontSize = 12.sp)
                        } else {
                            Text("AI 将以角色第一人称语气回应，输出一句话")
                        }
                    }
                )
            }
        },
        confirmButton = {
            val isValid = selectedSkill != null && prompt.isNotBlank() && hourText.isNotBlank() && minuteText.isNotBlank() && !hourError && !minuteError
            Button(
                onClick = {
                    val hour = hourText.toIntOrNull()
                    val min = minuteText.toIntOrNull()
                    hourError = hour == null || hour !in 0..23
                    minuteError = min == null || min !in 0..59
                    skillError = selectedSkill == null
                    promptError = prompt.isBlank()

                    if (!hourError && !minuteError && !skillError && !promptError && hour != null && min != null) {
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
                },
                enabled = isValid
            ) {
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

    // 更多模板 BottomSheet
    if (showTemplateSheet) {
        TemplatePickerSheet(
            templates = templates,
            selectedPrompt = prompt,
            onSelect = { selected ->
                prompt = selected.prompt
                showTemplateSheet = false
            },
            onDismiss = { showTemplateSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatePickerSheet(
    templates: List<NotificationTemplate>,
    selectedPrompt: String,
    onSelect: (NotificationTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    val filtered = remember(templates, searchText) {
        if (searchText.isBlank()) templates
        else templates.filter { it.label.contains(searchText, ignoreCase = true) || it.prompt.contains(searchText, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "选择模板",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(12.dp))

            // 搜索框
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("搜索模板...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Text(
                    text = "暂无匹配模板",
                    fontSize = 14.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(filtered, key = { it.id }) { template ->
                        Surface(
                            onClick = { onSelect(template) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedPrompt == template.prompt) BrandBlue.copy(alpha = 0.08f) else BgPage
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = template.label,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                        if (template.isBuiltin) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = BrandMint.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "内置",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                    fontSize = 9.sp,
                                                    color = BrandMint
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = template.prompt,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (selectedPrompt == template.prompt) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandBlue,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("✓", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
