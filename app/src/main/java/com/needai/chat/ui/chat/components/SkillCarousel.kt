package com.needai.chat.ui.chat.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.theme.*

@Composable
fun SkillCarousel(
    skills: List<Skill>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    onSkillSelected: (Skill) -> Unit,
    voiceNameMap: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    if (skills.isEmpty()) return

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // ============ Header (identical to index.html .home-header) ============
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NeedAI",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        brush = Brush.linearGradient(
                            listOf(BrandBlue, BrandPink, BrandMint)
                        )
                    )
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ============ Accordion Stack ============
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy((-20).dp)
        ) {
            skills.forEachIndexed { index, skill ->
                val isExpanded = index == selectedIndex
                val cardHeight by animateDpAsState(
                    targetValue = if (isExpanded) 288.dp else 80.dp,
                    animationSpec = tween(500),
                    label = "cardHeight"
                )

                val overlayColors = listOf(
                    listOf(BrandMint.copy(alpha = 0.6f), BrandMint.copy(alpha = 0.1f)),
                    listOf(BrandBlue.copy(alpha = 0.6f), BrandBlue.copy(alpha = 0.1f)),
                    listOf(BrandPink.copy(alpha = 0.6f), BrandPink.copy(alpha = 0.1f))
                )
                val overlayBrush = Brush.verticalGradient(
                    overlayColors[index % overlayColors.size]
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .animateContentSize(tween(500))
                        .clickable {
                            if (isExpanded) {
                                onSkillSelected(skill)
                            } else {
                                onSelectedIndexChanged(index)
                            }
                        }
                ) {
                    // Card background gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                ),
                                RoundedCornerShape(32.dp)
                            )
                            .border(
                                0.5.dp,
                                Color.White.copy(alpha = 0.5f),
                                RoundedCornerShape(32.dp)
                            )
                    )

                    // Colored overlay (matching .card-overlay)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayBrush, RoundedCornerShape(32.dp))
                    )

                    // Card content at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Left: skill name + voice tag
                            Column {
                                Text(
                                    text = skill.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(6.dp))
                                // Voice name tag
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color.White.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = voiceNameMap[skill.voiceId]
                                            ?: skill.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        letterSpacing = 0.4.sp
                                    )
                                }
                                if (isExpanded && skill.description.isNotBlank()) {
                                    Text(
                                        text = skill.description,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
