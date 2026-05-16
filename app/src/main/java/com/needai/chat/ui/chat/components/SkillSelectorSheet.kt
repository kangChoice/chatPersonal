package com.needai.chat.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.needai.chat.domain.model.Skill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillSelectorSheet(
    skills: List<Skill>,
    currentSkillId: String,
    voiceAliases: Map<String, String> = emptyMap(),
    onSkillSelected: (Skill) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "选择角色",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                items(skills) { skill ->
                    SkillSelectorItem(
                        skill = skill,
                        isSelected = skill.id == currentSkillId,
                        voiceAlias = voiceAliases[skill.voiceId] ?: "",
                        onClick = {
                            onSkillSelected(skill)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillSelectorItem(
    skill: Skill,
    isSelected: Boolean,
    voiceAlias: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = skill.avatar,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (skill.description.isNotBlank()) {
                    Text(
                        text = skill.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Voice info row with design
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (skill.voiceId.isNotBlank())
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (skill.voiceId.isNotBlank())
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (skill.voiceId.isNotBlank()) {
                                if (voiceAlias.isNotBlank()) voiceAlias
                                else skill.voiceId
                            } else "未配置",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (skill.voiceId.isNotBlank())
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "当前",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
