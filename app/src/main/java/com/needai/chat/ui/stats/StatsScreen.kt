package com.needai.chat.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSessionDropdown by remember { mutableStateOf(false) }
    var showConfigDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Filter section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("筛选条件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Session filter
                    Text("按会话筛选", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = showSessionDropdown,
                        onExpandedChange = { showSessionDropdown = it }
                    ) {
                        val sessionLabel = uiState.selectedSessionId?.let { id ->
                            uiState.sessions.find { it.id == id }?.let {
                                it.title.take(30)
                            } ?: "会话已删除"
                        } ?: "所有会话"

                        OutlinedTextField(
                            value = sessionLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSessionDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = showSessionDropdown,
                            onDismissRequest = { showSessionDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("所有会话") },
                                onClick = {
                                    viewModel.selectSession(null)
                                    showSessionDropdown = false
                                }
                            )
                            uiState.sessions.forEach { session ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(session.title.take(40), style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${session.skillName} · ${SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(session.createdAt))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectSession(session.id)
                                        showSessionDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Model config filter
                    Text("按模型筛选", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = showConfigDropdown,
                        onExpandedChange = { showConfigDropdown = it }
                    ) {
                        val configLabel = uiState.selectedConfigId?.let { id ->
                            uiState.configs.find { it.id == id }?.name
                                ?: uiState.configs.find { it.id == id }?.remoteModelName
                                ?: "配置已删除"
                        } ?: "所有模型"

                        OutlinedTextField(
                            value = configLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showConfigDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = showConfigDropdown,
                            onDismissRequest = { showConfigDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("所有模型") },
                                onClick = {
                                    viewModel.selectConfig(null)
                                    showConfigDropdown = false
                                }
                            )
                            uiState.configs.forEach { config ->
                                DropdownMenuItem(
                                    text = {
                                        Text(config.name.ifEmpty { config.remoteModelName })
                                    },
                                    onClick = {
                                        viewModel.selectConfig(config.id)
                                        showConfigDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time range presets
                    Text("时间范围", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val now = System.currentTimeMillis()
                        val cal = Calendar.getInstance()

                        fun timePreset(days: Int) = run {
                            cal.timeInMillis = now
                            cal.add(Calendar.DAY_OF_YEAR, -days)
                            cal.timeInMillis
                        }

                        FilterChip(
                            selected = uiState.selectedSessionId == null && uiState.selectedConfigId == null
                                    && uiState.startTime == timePreset(7),
                            onClick = { viewModel.setTimeRange(timePreset(7), now) },
                            label = { Text("7天") }
                        )
                        FilterChip(
                            selected = uiState.selectedSessionId == null && uiState.selectedConfigId == null
                                    && uiState.startTime == timePreset(30),
                            onClick = { viewModel.setTimeRange(timePreset(30), now) },
                            label = { Text("30天") }
                        )
                        FilterChip(
                            selected = uiState.selectedSessionId != null || uiState.selectedConfigId != null,
                            onClick = {
                                viewModel.setTimeRange(0L, now)
                                viewModel.selectSession(null)
                                viewModel.selectConfig(null)
                            },
                            label = { Text("全部") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Token stats cards
            Text(
                "Token 消耗",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                TokenStatCard(
                    label = "总消耗",
                    value = uiState.tokenTotals.totalTokens ?: 0,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                TokenStatCard(
                    label = "输入 (Prompt)",
                    value = uiState.tokenTotals.promptTokens ?: 0,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                TokenStatCard(
                    label = "输出 (Completion)",
                    value = uiState.tokenTotals.completionTokens ?: 0,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun TokenStatCard(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTokenCount(value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun formatTokenCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
