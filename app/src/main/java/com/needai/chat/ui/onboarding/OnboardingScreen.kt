package com.needai.chat.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.needai.chat.ui.theme.*

data class OnboardingStep(
    val emoji: String,
    val title: String,
    val description: String,
    val warning: String? = null
)

private val steps = listOf(
    OnboardingStep(
        emoji = "💬",
        title = "聊天",
        description = "与 AI 角色一对一畅快交流，支持多轮对话上下文理解。点击角色卡片进入详情，底部输入框支持文字输入。AI 回复支持 TTS 语音朗读，长消息自动分段合成。",
        warning = "首次使用前请前往设置页配置 API Key 和聊天模型，否则无法开始对话。"
    ),
    OnboardingStep(
        emoji = "👥",
        title = "群聊",
        description = "选择多个角色同时参与群聊对话，AI 角色之间会相互互动。支持自定义群聊氛围提示词，打造独特的多人聊天体验。至少选择 2 个角色即可开始群聊。"
    ),
    OnboardingStep(
        emoji = "🎯",
        title = "技能管理",
        description = "管理所有自定义角色和音色。角色支持创建、编辑、导出/导入；音色支持创建（声音克隆）、绑定角色、试听和别名管理。已绑定音色的角色在对话中会自动使用该音色朗读。"
    ),
    OnboardingStep(
        emoji = "📝",
        title = "提示词优化",
        description = "输入角色描述即可让 AI 自动生成专业的系统提示词。支持角色提示词和音色描述两种模式。生成后可直接创建新角色或一键创建自定义音色，极大提升角色创建效率。"
    ),
    OnboardingStep(
        emoji = "⚙️",
        title = "设置",
        description = "配置 API Key 和模型（支持 OpenAI、DeepSeek、Anthropic 等主流供应商）、调整 TTS 参数（语速/音调/音量）、设置自定义聊天背景。所有模型配置自由切换，满足不同场景需求。"
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
            .background(Color.Black.copy(alpha = 0.45f))
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
                            if (index == step) BrandBlue
                            else Color.White.copy(alpha = 0.35f)
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

        // Glass content card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(GlassWhite)
                .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentStep.description,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Start,
                    lineHeight = 22.sp
                )

                if (currentStep.warning != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandBlue.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = currentStep.warning,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            color = BrandBlue,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = if (isLastStep) onFinish else onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        disabledContainerColor = BrandBlue.copy(alpha = 0.3f)
                    )
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
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}
