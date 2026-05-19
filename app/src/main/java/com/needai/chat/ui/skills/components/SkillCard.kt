package com.needai.chat.ui.skills.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.theme.*
import java.io.File

@Composable
fun SkillCard(
    skill: Skill,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectionChanged: ((Boolean) -> Unit)? = null,
    voiceName: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarBitmap = remember(skill.avatarPath, skill.id) {
        val path = skill.avatarPath.takeIf { it.isNotBlank() }?.let {
            val f = File(it)
            if (f.exists()) it else null
        }
        path?.let { try { BitmapFactory.decodeFile(it) } catch (_: Exception) { null } }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassWhite)
            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChanged,
                    modifier = Modifier.padding(end = 8.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = BrandBlue,
                        uncheckedColor = TextTertiary
                    )
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgPage),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "角色头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = skill.avatar,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = skill.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    if (skill.isBuiltin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(BrandMint.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "内置",
                                fontSize = 9.sp,
                                color = BrandMint,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = skill.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!voiceName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(BrandMint.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = voiceName,
                                fontSize = 9.sp,
                                color = BrandMint,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Export button
            if (onExport != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Transparent)
                        .clickable(onClick = onExport),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = "导出",
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Delete button
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Transparent)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = BrandPink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
