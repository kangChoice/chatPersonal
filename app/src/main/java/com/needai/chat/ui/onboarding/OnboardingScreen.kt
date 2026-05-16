package com.needai.chat.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingStep(
    val emoji: String,
    val title: String,
    val description: String,
    val warning: String? = null
)

private val steps = listOf(
    OnboardingStep(
        emoji = "💬",
        title = "聊天页面",
        description = "点击顶部 ✨ 图标可快速切换不同的角色。当前使用的模型名称显示在角色名称下方，随时了解正在与哪个模型对话。",
        warning = "如果聊天页面右上角显示 ⚠️ 警示符号，说明还没有选择聊天模型，请前往设置页配置。"
    ),
    OnboardingStep(
        emoji = "✏️",
        title = "提示词润色",
        description = "在这里输入角色描述，AI 会根据你的描述自动生成详细的系统提示词。生成后点击「创建角色」按钮，即可一键将新角色保存为角色。"
    ),
    OnboardingStep(
        emoji = "🎯",
        title = "角色管理",
        description = "所有已创建的角色都会显示在这里。点击右上角 + 按钮可以创建新的角色，也可以对已有角色进行编辑、导出或删除。"
    ),
    OnboardingStep(
        emoji = "⚙️",
        title = "设置与模型配置",
        description = "在模型配置区域，点击右上角 + 按钮添加新的模型配置（支持 OpenAI、DeepSeek、Anthropic 等多种供应商）。点击任意配置即可将其设为当前使用的模型。"
    )
)

@Composable
fun OnboardingOverlay(
    step: Int,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    val isLastStep = step == steps.size - 1
    val currentStep = steps[step]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        // Dots indicator at top
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == step) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Step indicator text
        if (!isLastStep) {
            Text(
                text = "${step + 1} / ${steps.size}",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Content card
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentStep.emoji,
                    fontSize = 48.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentStep.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Start,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )

                if (currentStep.warning != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = currentStep.warning,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = if (isLastStep) onFinish else onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (isLastStep) "开始使用" else "下一步",
                        fontSize = 16.sp
                    )
                }

                if (!isLastStep) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onFinish) {
                        Text(
                            "跳过引导",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
