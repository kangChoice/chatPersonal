package com.needai.chat.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.data.ilink.FixedScheduleItem
import com.needai.chat.ui.theme.*
import com.needai.chat.ui.util.LocalToast
import com.needai.chat.ui.util.ToastType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IlinkScheduleScreen(
    viewModel: IlinkScheduleViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val toastState = LocalToast.current

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

                    var startHour by remember(config) { mutableStateOf(config.randomStartTime.split(":").getOrElse(0) { "" }) }
                    var startMinute by remember(config) { mutableStateOf(config.randomStartTime.split(":").getOrElse(1) { "" }) }
                    var endHour by remember(config) { mutableStateOf(config.randomEndTime.split(":").getOrElse(0) { "" }) }
                    var endMinute by remember(config) { mutableStateOf(config.randomEndTime.split(":").getOrElse(1) { "" }) }
                    var startHourError by remember { mutableStateOf(false) }
                    var startMinuteError by remember { mutableStateOf(false) }
                    var endHourError by remember { mutableStateOf(false) }
                    var endMinuteError by remember { mutableStateOf(false) }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeFieldGroup(
                            label = "起始",
                            hour = startHour,
                            onHourChange = { startHour = it },
                            minute = startMinute,
                            onMinuteChange = { startMinute = it },
                            hourError = startHourError,
                            minuteError = startMinuteError,
                            modifier = Modifier.weight(1f)
                        )
                        Text("~", color = TextSecondary, modifier = Modifier.padding(top = 28.dp))
                        TimeFieldGroup(
                            label = "结束",
                            hour = endHour,
                            onHourChange = { endHour = it },
                            minute = endMinute,
                            onMinuteChange = { endMinute = it },
                            hourError = endHourError,
                            minuteError = endMinuteError,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val sh = startHour.toIntOrNull()
                            val sm = startMinute.toIntOrNull()
                            val eh = endHour.toIntOrNull()
                            val em = endMinute.toIntOrNull()
                            startHourError = sh == null || sh !in 0..23
                            startMinuteError = sm == null || sm !in 0..59
                            endHourError = eh == null || eh !in 0..23
                            endMinuteError = em == null || em !in 0..59
                            if (!startHourError && !startMinuteError && !endHourError && !endMinuteError) {
                                val startTime = String.format("%02d:%02d", sh!!, sm!!)
                                val endTime = String.format("%02d:%02d", eh!!, em!!)
                                viewModel.setRandomTimeRange(startTime, endTime)
                                toastState.show("时间范围已更新", ToastType.Success)
                            } else {
                                toastState.show("请输入正确的时间", ToastType.Error)
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
                        valueRange = 0f..10f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", fontSize = 11.sp, color = TextTertiary)
                        Text("10", fontSize = 11.sp, color = TextTertiary)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "定时任务依赖 context_token，该凭证有效期内可回复最多 10 条消息。若连续 10 次定时消息期间未收到用户回复，后续定时任务将因 token 耗尽而失效。",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "为确保定时任务正常工作，请在系统设置中将本应用加入省电白名单，避免后台被系统限制。",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
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
    var hour by remember { mutableStateOf(initial?.time?.split(":")?.getOrElse(0) { "" } ?: "") }
    var minute by remember { mutableStateOf(initial?.time?.split(":")?.getOrElse(1) { "" } ?: "") }
    var message by remember { mutableStateOf(initial?.message ?: "") }
    var hourError by remember { mutableStateOf(false) }
    var minuteError by remember { mutableStateOf(false) }

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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { if (it.length <= 2) { hour = it; hourError = false } },
                        label = { Text("时") },
                        placeholder = { Text("08") },
                        singleLine = true,
                        isError = hourError,
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(":", fontSize = 20.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { if (it.length <= 2) { minute = it; minuteError = false } },
                        label = { Text("分") },
                        placeholder = { Text("00") },
                        singleLine = true,
                        isError = minuteError,
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                if (hourError || minuteError) {
                    Text("请输入有效时间（00:00-23:59）", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
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
                val h = hour.toIntOrNull()
                val m = minute.toIntOrNull()
                hourError = h == null || h !in 0..23
                minuteError = m == null || m !in 0..59
                if (!hourError && !minuteError && message.isNotBlank()) {
                    onSave(FixedScheduleItem(time = String.format("%02d:%02d", h!!, m!!), message = message))
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

@Composable
private fun TimeFieldGroup(
    label: String,
    hour: String,
    onHourChange: (String) -> Unit,
    minute: String,
    onMinuteChange: (String) -> Unit,
    hourError: Boolean,
    minuteError: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hour,
                onValueChange = { if (it.length <= 2) onHourChange(it) },
                placeholder = { Text("08") },
                singleLine = true,
                isError = hourError,
                modifier = Modifier.width(56.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(":", fontSize = 20.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 2.dp))
            OutlinedTextField(
                value = minute,
                onValueChange = { if (it.length <= 2) onMinuteChange(it) },
                placeholder = { Text("00") },
                singleLine = true,
                isError = minuteError,
                modifier = Modifier.width(56.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

