package com.needai.chat.ui.ilink

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.domain.model.Skill
import androidx.compose.ui.graphics.Color
import com.needai.chat.ui.theme.*
import com.needai.chat.ui.util.LocalToast
import com.needai.chat.ui.util.ToastType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IlinkStatusScreen(
    viewModel: IlinkViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val skills by viewModel.skills.collectAsStateWithLifecycle()
    val todaySchedule by viewModel.todaySchedule.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ClawBot 管理") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPage)
            )
        },
        containerColor = BgPage
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 连接状态卡片
            item {
                StatusCard(uiState = uiState, viewModel = viewModel)
            }

            // 定时消息测试
            item {
                ScheduleTestCard(
                    schedule = todaySchedule,
                    onTestSend = { viewModel.testSendSchedule() }
                )
            }

            // 角色列表（切换角色用）
            if (skills.isNotEmpty()) {
                item {
                    Text(
                        text = "切换角色",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(skills) { skill ->
                    SkillItem(
                        skill = skill,
                        isSelected = isSkillSelected(uiState, skill),
                        onClick = { viewModel.switchSkill(skill.id) }
                    )
                }
            }

            // 底部操作区
            item {
                Spacer(Modifier.height(16.dp))
                ActionButtons(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun StatusCard(uiState: IlinkUiState, viewModel: IlinkViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "连接状态", fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(8.dp))

            val statusText: String
            val statusColor: Color
            when (uiState) {
                is IlinkUiState.Connected -> { statusText = "已连接"; statusColor = StatusGreen }
                is IlinkUiState.Stopped -> { statusText = "已停止"; statusColor = TextTertiary }
                is IlinkUiState.Setup -> { statusText = "未接入"; statusColor = BrandPink }
                is IlinkUiState.Error -> { statusText = "异常"; statusColor = StatusRed }
                is IlinkUiState.Loading -> { statusText = "加载中..."; statusColor = TextTertiary }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor,
                    modifier = Modifier.size(10.dp)
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(text = statusText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            val skillName = when (uiState) {
                is IlinkUiState.Connected -> uiState.skillName
                is IlinkUiState.Stopped -> uiState.skillName
                is IlinkUiState.Setup -> uiState.currentSkill?.name ?: "未选择"
                else -> "—"
            }
            Spacer(Modifier.height(12.dp))
            Text(text = "当前角色：$skillName", fontSize = 14.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun SkillItem(skill: Skill, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BrandBlue.copy(alpha = 0.1f) else BgCard
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = skill.avatar, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = skill.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(text = skill.description, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
            }
            if (isSelected) {
                Text(text = "✓", color = BrandBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActionButtons(uiState: IlinkUiState, viewModel: IlinkViewModel) {
    val toastState = LocalToast.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (uiState) {
            is IlinkUiState.Connected -> {
                Button(
                    onClick = {
                        viewModel.stopBridge()
                        toastState.show("桥接已停止", ToastType.Info)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("停止桥接", fontSize = 16.sp)
                }
            }
            is IlinkUiState.Stopped -> {
                Button(
                    onClick = {
                        viewModel.reconnect()
                        toastState.show("桥接已启动", ToastType.Success)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("启动桥接", fontSize = 16.sp)
                }
            }
            is IlinkUiState.Error -> {
                Button(
                    onClick = {
                        viewModel.reconnect()
                        toastState.show("正在重新连接...", ToastType.Info)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("重新连接", fontSize = 16.sp)
                }
            }
            else -> {}
        }

        // 重新授权（重置 Token）
        OutlinedButton(
            onClick = {
                viewModel.resetAuth()
                toastState.show("已清除授权信息", ToastType.Info)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("重新授权", fontSize = 16.sp)
        }
    }
}

@Composable
private fun ScheduleTestCard(
    schedule: List<Pair<String, String>>,
    onTestSend: () -> Unit
) {
    val toastState = LocalToast.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "今日定时消息", fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(8.dp))

            if (schedule.isEmpty()) {
                Text(text = "暂无（桥接未启动或随机时间未生成）", fontSize = 13.sp, color = TextTertiary)
            } else {
                schedule.forEach { (time, text) ->
                    Text(
                        text = "【$time】$text",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onTestSend()
                    toastState.show("测试消息已发送", ToastType.Success)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPink),
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("测试发送（带时间前缀）", fontSize = 14.sp)
            }
        }
    }
}

private fun isSkillSelected(uiState: IlinkUiState, skill: Skill): Boolean {
    val selectedName = when (uiState) {
        is IlinkUiState.Connected -> uiState.skillName
        is IlinkUiState.Stopped -> uiState.skillName
        is IlinkUiState.Setup -> uiState.currentSkill?.name
        else -> null
    }
    return skill.name == selectedName
}
