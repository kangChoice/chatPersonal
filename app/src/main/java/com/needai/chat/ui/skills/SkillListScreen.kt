package com.needai.chat.ui.skills

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.needai.chat.R
import com.needai.chat.data.export.ExportUtils
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.navigation.Screen
import com.needai.chat.ui.skills.components.SkillCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillListScreen(
    navController: NavController,
    viewModel: SkillViewModel = hiltViewModel()
) {
    val skills by viewModel.skills.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var skillToDelete by remember { mutableStateOf<Skill?>(null) }
    var skillToExport by remember { mutableStateOf<Skill?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedSkillIds by remember { mutableStateOf(setOf<String>()) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val exportSkillLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && skillToExport != null) {
            val json = ExportUtils.generateSkillJson(skillToExport!!)
            ExportUtils.writeToUri(context, uri, json)
            skillToExport = null
        }
    }
    val exportSkillsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && selectedSkillIds.isNotEmpty()) {
            val selectedSkills = skills.filter { it.id in selectedSkillIds }
            val json = ExportUtils.generateSkillsJson(selectedSkills)
            ExportUtils.writeToUri(context, uri, json)
            selectedSkillIds = emptySet()
            isSelectionMode = false
        }
    }
    val importSkillLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = com.needai.chat.data.import.ImportUtils.readFromUri(context, uri)
            if (json != null) {
                val result = com.needai.chat.data.import.ImportUtils.parseSkillsJson(json)
                result.onSuccess { skills ->
                    if (skills.size == 1) {
                        viewModel.importSkill(skills.first()) { success, msg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    } else {
                        viewModel.importSkills(skills) { success, msg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }
                }.onFailure { e ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("导入失败: ${e.localizedMessage}")
                    }
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("读取文件失败")
                }
            }
        }
    }

    val uriHandler = LocalUriHandler.current

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            selectedSkillIds = if (selectedSkillIds.size == skills.size) {
                                emptySet()
                            } else {
                                skills.map { it.id }.toSet()
                            }
                        }) {
                            Text(if (selectedSkillIds.size == skills.size) "取消全选" else "全选")
                        }
                        Button(
                            onClick = {
                                if (selectedSkillIds.isNotEmpty()) {
                                    val fileName = "skills_export_${java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())}.json"
                                    exportSkillsLauncher.launch(fileName)
                                }
                            },
                            enabled = selectedSkillIds.isNotEmpty()
                        ) {
                            Text("导出选中 (${selectedSkillIds.size})")
                        }
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("角色管理", fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { uriHandler.openUri("https://github.com/kangChoice") }
                                .padding(top = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_github),
                                contentDescription = "GitHub",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "power by",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        TextButton(onClick = {
                            selectedSkillIds = emptySet()
                            isSelectionMode = false
                        }) {
                            Text("取消")
                        }
                    }
                    if (!isSelectionMode) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "创建角色")
                        }
                        TextButton(onClick = {
                            importSkillLauncher.launch(arrayOf("application/json"))
                        }) {
                            Text("导入")
                        }
                        TextButton(onClick = { isSelectionMode = true }) {
                            Text("选择")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (skills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无角色，点击右上角 + 创建",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(skills, key = { it.id }) { skill ->
                    SkillCard(
                        skill = skill,
                        onClick = {
                            if (isSelectionMode) {
                                selectedSkillIds = if (skill.id in selectedSkillIds) {
                                    selectedSkillIds - skill.id
                                } else {
                                    selectedSkillIds + skill.id
                                }
                            } else {
                                navController.navigate(Screen.skillEdit(skill.id))
                            }
                        },
                        onExport = if (isSelectionMode) null else {
                            {
                                skillToExport = skill
                                val fileName = "skills_${skill.name}.json"
                                exportSkillLauncher.launch(fileName)
                            }
                        },
                        onDelete = if (isSelectionMode || skill.isBuiltin) null else {
                            { skillToDelete = skill }
                        },
                        isSelected = skill.id in selectedSkillIds,
                        isSelectionMode = isSelectionMode,
                        onSelectionChanged = { checked ->
                            selectedSkillIds = if (checked) {
                                selectedSkillIds + skill.id
                            } else {
                                selectedSkillIds - skill.id
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        SkillEditDialog(
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

    if (skillToDelete != null) {
        AlertDialog(
            onDismissRequest = { skillToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除角色") },
            text = {
                Text("确定要删除「${skillToDelete!!.name}」吗？删除后，该角色对应的所有历史会话记录也将一并删除，此操作不可撤销。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSkill(skillToDelete!!.id)
                        skillToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { skillToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun SkillEditDialog(
    initialSkill: com.needai.chat.domain.model.Skill? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, systemPrompt: String, avatar: String, greeting: String, temperature: Double) -> Unit
) {
    val isEdit = initialSkill != null
    var name by remember { mutableStateOf(initialSkill?.name ?: "") }
    var description by remember { mutableStateOf(initialSkill?.description ?: "") }
    var systemPrompt by remember { mutableStateOf(initialSkill?.systemPrompt ?: "") }
    var avatar by remember { mutableStateOf(initialSkill?.avatar ?: "🤖") }
    var greeting by remember { mutableStateOf(initialSkill?.greeting ?: "你好！") }
    var temperature by remember { mutableStateOf(initialSkill?.temperature?.toString() ?: "0.7") }
    var showSystemPromptDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑角色" else "创建角色") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = avatar,
                    onValueChange = { avatar = it },
                    label = { Text("头像 (Emoji)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = greeting,
                    onValueChange = { greeting = it },
                    label = { Text("问候语") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                // System prompt card - click to open full-screen editor
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSystemPromptDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "系统提示词 (System Prompt)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = systemPrompt.ifEmpty { "点击编辑系统提示词..." },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (systemPrompt.isEmpty())
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("温度 (0.0 - 2.0)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = temperature.isNotEmpty() && temperature.toDoubleOrNull() == null,
                    supportingText = {
                        if (temperature.isNotEmpty() && temperature.toDoubleOrNull() == null) {
                            Text("请输入有效数字")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && systemPrompt.isNotBlank()) {
                        val temp = (temperature.toDoubleOrNull() ?: 0.7).coerceIn(0.0, 2.0)
                        onSave(name, description, systemPrompt, avatar, greeting, temp)
                    }
                },
                enabled = name.isNotBlank() && systemPrompt.isNotBlank()
            ) {
                Text(if (isEdit) "保存" else "创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showSystemPromptDialog) {
        SystemPromptEditDialog(
            initialPrompt = systemPrompt,
            isBuiltin = false,
            onDismiss = { showSystemPromptDialog = false },
            onSave = { newPrompt ->
                systemPrompt = newPrompt
                showSystemPromptDialog = false
            }
        )
    }
}
