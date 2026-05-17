package com.needai.chat.ui.prompt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.skills.SkillEditDialog
import com.needai.chat.ui.voice.components.SUPPORTED_CREATION_MODELS
import com.needai.chat.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolishScreen(
    viewModel: PolishViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("角色优化", fontWeight = FontWeight.Bold)
                    if (uiState.currentModelName.isNotBlank()) {
                        Text(
                            text = uiState.currentModelName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Row {
                    val hasPolishedContent = if (selectedTab == 0) uiState.polishedPrompt.isNotBlank()
                    else uiState.voicePolishedPrompt.isNotBlank()
                    if (hasPolishedContent) {
                        IconButton(onClick = {
                            if (selectedTab == 0) viewModel.clearPolishedPrompt()
                            else viewModel.clearVoicePolishedPrompt()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "清空")
                        }
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(999.dp)
                )
            }
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(
                    onClick = { coroutineScope.launch { scrollState.animateScrollTo(0) } }
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "回到顶部")
                }
                SmallFloatingActionButton(
                    onClick = { coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) } }
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "到底部")
                }
            }
        }
    ) { innerPadding ->
        val tabTitles = listOf("角色优化", "音色优化")

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    // === Role Prompt Polish ===
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "描述角色设定",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.inputText,
                                onValueChange = viewModel::setInputText,
                                placeholder = {
                                    Text("想要一个像该软件开发作者一般优秀、风趣的男朋友")
                                },
                                minLines = 5,
                                maxLines = 8,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isPolishing
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (uiState.isPolishing) {
                                Button(
                                    onClick = { viewModel.stopPolishing() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "停止", modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("停止生成")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            } else {
                                Button(
                                    onClick = { viewModel.polishPrompt() },
                                    enabled = uiState.inputText.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("生成角色提示词")
                                }
                            }
                        }
                    }

                    if (uiState.error != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.error!!,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (uiState.polishedPrompt.isNotBlank()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "生成的提示词",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearPolishedPrompt() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "清空",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "已生成 ${uiState.charCount} 字",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = uiState.polishedPrompt,
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showCreateDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("创建角色")
                                }
                            }
                        }
                    }
                } else {
                    // === Voice Polish ===
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "描述音色",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.voiceInputText,
                                onValueChange = viewModel::setVoiceInputText,
                                placeholder = {
                                    Text("温柔的女声，30岁左右，适合朗读情感故事")
                                },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.voiceIsPolishing
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (uiState.voiceIsPolishing) {
                                Button(
                                    onClick = { viewModel.stopVoicePolishing() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "停止", modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("停止生成")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            } else {
                                Button(
                                    onClick = { viewModel.polishVoicePrompt() },
                                    enabled = uiState.voiceInputText.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("生成音色描述")
                                }
                            }
                        }
                    }

                    if (uiState.voiceError != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.voiceError!!,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (uiState.voicePolishedPrompt.isNotBlank()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "优化后的音色描述",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearVoicePolishedPrompt() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "清空",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "已生成 ${uiState.voiceCharCount} 字",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = uiState.voicePolishedPrompt,
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        // Voice creation form
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "一键创建音色",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = uiState.voiceAlias,
                                    onValueChange = viewModel::setVoiceAlias,
                                    label = { Text("别名 *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = uiState.voiceCreateError != null && uiState.voiceAlias.isBlank()
                                )
                                Text("选择模型", style = MaterialTheme.typography.labelLarge)
                                SUPPORTED_CREATION_MODELS.forEach { model ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.setVoiceTargetModel(model) }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = uiState.voiceTargetModel == model,
                                            onClick = { viewModel.setVoiceTargetModel(model) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(model, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                text = if (model.contains("flash")) "快速合成，成本较低" else "高音质合成，成本较高",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                                if (uiState.voiceCreateError != null) {
                                    Text(
                                        text = uiState.voiceCreateError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.createVoice { success, msg ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    },
                                    enabled = uiState.voiceAlias.isNotBlank() && !uiState.isCreatingVoice,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isCreatingVoice) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("创建音色")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog && uiState.polishedPrompt.isNotBlank()) {
        SkillEditDialog(
            initialSkill = Skill(
                id = "",
                name = "",
                description = "",
                avatar = "🤖",
                systemPrompt = uiState.polishedPrompt,
                greeting = "你好！",
                isBuiltin = false
            ),
            onDismiss = { showCreateDialog = false },
            onSave = { name, desc, prompt, avatar, greeting, temp ->
                viewModel.createSkill(name, desc, prompt, avatar, greeting, temp) { success, msg ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                }
                showCreateDialog = false
            }
        )
    }
}
}
