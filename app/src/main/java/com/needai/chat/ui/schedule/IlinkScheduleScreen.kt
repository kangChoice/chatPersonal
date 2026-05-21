package com.needai.chat.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.data.ilink.FixedScheduleItem
import com.needai.chat.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IlinkScheduleScreen(
    viewModel: IlinkScheduleViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf(-1) }            // -1 = add mode
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定时任务") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPage)
            )
        },
        containerColor = BgPage
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 固定消息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "固定消息",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    if (config.fixedMessages.isEmpty()) {
                        Text(
                            text = "暂无固定消息，点击下方添加",
                            fontSize = 13.sp,
                            color = TextTertiary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        config.fixedMessages.forEachIndexed { index, item ->
                            FixedMessageRow(
                                time = item.time,
                                message = item.message,
                                onEdit = {
                                    editIndex = index
                                    showEditDialog = true
                                },
                                onDelete = {
                                    deleteIndex = index
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            editIndex = -1
                            showEditDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("添加固定消息")
                    }
                }
            }

            // 随机消息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "随机消息",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = config.randomMessage,
                        onValueChange = { viewModel.setRandomMessage(it) },
                        label = { Text("消息内容") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "随机时间范围",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))

                    var startText by remember(config) { mutableStateOf(formatHour(config.randomStartHour)) }
                    var endText by remember(config) { mutableStateOf(formatHour(config.randomEndHour)) }
                    var startError by remember { mutableStateOf(false) }
                    var endError by remember { mutableStateOf(false) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startText,
                            onValueChange = { startText = it },
                            label = { Text("起始") },
                            placeholder = { Text("08:00") },
                            singleLine = true,
                            isError = startError,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Text("~", color = TextSecondary)
                        OutlinedTextField(
                            value = endText,
                            onValueChange = { endText = it },
                            label = { Text("结束") },
                            placeholder = { Text("18:00") },
                            singleLine = true,
                            isError = endError,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val start = parseHourInput(startText)
                            val end = parseHourInput(endText)
                            startError = start == null || start < 0 || start > 23
                            endError = end == null || end < 0 || end > 23
                            if (!startError && !endError && start != null && end != null) {
                                viewModel.setRandomTimeRange(start, end)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("应用时间范围")
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "每日随机次数: ${config.randomCount}",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Slider(
                        value = config.randomCount.toFloat(),
                        onValueChange = { viewModel.setRandomCount(it.toInt()) },
                        valueRange = 0f..60f,
                        steps = 59,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", fontSize = 11.sp, color = TextTertiary)
                        Text("60", fontSize = 11.sp, color = TextTertiary)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // 添加/编辑对话框
    if (showEditDialog) {
        FixedMessageEditDialog(
            initial = if (editIndex >= 0) config.fixedMessages.getOrNull(editIndex) else null,
            onDismiss = { showEditDialog = false },
            onSave = { item ->
                if (editIndex >= 0) {
                    viewModel.updateFixedMessage(editIndex, item)
                } else {
                    viewModel.addFixedMessage(item)
                }
                showEditDialog = false
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog && deleteIndex in config.fixedMessages.indices) {
        val item = config.fixedMessages[deleteIndex]
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed) },
            title = { Text("删除固定消息", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("确定删除【${item.time}】${item.message} 吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFixedMessage(deleteIndex)
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
private fun FixedMessageRow(
    time: String,
    message: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                Text(
                    text = "【$time】",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandBlue
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 2
                )
            }
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

@Composable
private fun FixedMessageEditDialog(
    initial: FixedScheduleItem?,
    onDismiss: () -> Unit,
    onSave: (FixedScheduleItem) -> Unit
) {
    var time by remember { mutableStateOf(initial?.time ?: "") }
    var message by remember { mutableStateOf(initial?.message ?: "") }
    var timeError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "添加固定消息" else "编辑固定消息",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = time,
                    onValueChange = {
                        time = it
                        timeError = false
                    },
                    label = { Text("时间 (HH:mm)") },
                    placeholder = { Text("08:00") },
                    singleLine = true,
                    isError = timeError,
                    supportingText = if (timeError) {{ Text("格式错误，请输入 HH:mm (00:00-23:59)") }} else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("消息内容") },
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val valid = isValidTimeFormat(time)
                timeError = !valid
                if (valid && message.isNotBlank()) {
                    onSave(FixedScheduleItem(time = time, message = message))
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
}

private fun isValidTimeFormat(input: String): Boolean {
    val parts = input.split(":")
    if (parts.size != 2) return false
    val hour = parts[0].toIntOrNull() ?: return false
    val minute = parts[1].toIntOrNull() ?: return false
    return hour in 0..23 && minute in 0..59 && parts[0].length == 2 && parts[1].length == 2
}

private fun formatHour(hour: Int): String = String.format("%02d:00", hour)

private fun parseHourInput(input: String): Int? {
    val trimmed = input.trim()
    val parts = trimmed.split(":")
    if (parts.isEmpty()) return null
    return parts[0].toIntOrNull()
}
