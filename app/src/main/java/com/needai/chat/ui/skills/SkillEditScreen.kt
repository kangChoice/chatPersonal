package com.needai.chat.ui.skills

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillEditScreen(
    skillId: String,
    onNavigateBack: () -> Unit,
    viewModel: SkillViewModel = hiltViewModel()
) {
    val isNew = skillId == "new"
    val skills by viewModel.skills.collectAsState()
    val selectedSkillId by viewModel.selectedSkillId.collectAsState()
    val existingSkill = remember(skills, skillId) {
        skills.find { it.id == skillId }
    }

    var name by remember { mutableStateOf(existingSkill?.name ?: "") }
    var description by remember { mutableStateOf(existingSkill?.description ?: "") }
    var systemPrompt by remember { mutableStateOf(existingSkill?.systemPrompt ?: "") }
    var avatar by remember { mutableStateOf(existingSkill?.avatar ?: "🤖") }
    var greeting by remember { mutableStateOf(existingSkill?.greeting ?: "你好！") }
    var temperature by remember { mutableStateOf(existingSkill?.temperature?.toString() ?: "0.7") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isBuiltin = existingSkill?.isBuiltin == true
    val isCurrentSkill = existingSkill?.id == selectedSkillId

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
                .padding(16.dp),
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

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("系统提示词 (System Prompt)") },
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBuiltin
            )

            OutlinedTextField(
                value = temperature,
                onValueChange = { temperature = it },
                label = { Text("温度 (0.0 - 2.0)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBuiltin
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!isNew && !isCurrentSkill) {
                OutlinedButton(
                    onClick = {
                        viewModel.selectSkill(existingSkill!!)
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("使用此技能")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && systemPrompt.isNotBlank()) {
                        val temp = temperature.toDoubleOrNull() ?: 0.7
                        if (isNew) {
                            viewModel.createSkill(name, description, systemPrompt, avatar, greeting, temp)
                        } else if (existingSkill != null && !isBuiltin) {
                            viewModel.updateSkill(
                                existingSkill.copy(
                                    name = name,
                                    description = description,
                                    systemPrompt = systemPrompt,
                                    avatar = avatar,
                                    greeting = greeting,
                                    temperature = temp
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
