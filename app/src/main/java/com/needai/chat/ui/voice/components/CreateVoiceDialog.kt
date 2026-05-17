package com.needai.chat.ui.voice.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.needai.chat.ui.theme.*

private const val DEFAULT_PREVIEW_TEXT = "你好，欢迎试听我的声音，希望你能喜欢。"

/**
 * 支持音色创建（声音克隆）的 CosyVoice 模型列表。
 * 参考阿里云文档：v3.5-flash / v3.5-plus 支持设计音色。
 */
val SUPPORTED_CREATION_MODELS = listOf(
    "cosyvoice-v3.5-flash",
    "cosyvoice-v3.5-plus"
)

@Composable
fun CreateVoiceDialog(
    devicePrefix: String = "",
    rawDeviceId: String = "",
    onDismiss: () -> Unit,
    onCreate: (targetModel: String, voicePrompt: String) -> Unit
) {
    var selectedModel by remember { mutableStateOf(SUPPORTED_CREATION_MODELS.first()) }
    var voicePrompt by remember { mutableStateOf("") }
    var showDebugInfo by remember { mutableStateOf(false) }

    val isValid = voicePrompt.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Brand gradient title
                BrandGradientText(
                    text = "创建自定义音色",
                    fontSize = 22.sp
                )
                Spacer(Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Model section
                    Text(
                        "选择目标模型",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    SUPPORTED_CREATION_MODELS.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedModel == model) BrandBlue.copy(alpha = 0.06f)
                                    else Color.Transparent
                                )
                                .clickable { selectedModel = model }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedModel == model,
                                onClick = { selectedModel = model },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BrandBlue
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(model, fontSize = 14.sp, color = TextPrimary)
                                Text(
                                    text = if (model.contains("flash")) "快速合成，成本较低" else "高音质合成，成本较高",
                                    fontSize = 12.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DividerColor)

                    // Voice prompt field
                    OutlinedTextField(
                        value = voicePrompt,
                        onValueChange = { voicePrompt = it },
                        label = { Text("声音描述 (Voice Prompt)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("例如：温柔的女声，22岁，略带微笑，适合朗读故事") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            cursorColor = BrandBlue,
                            focusedLabelColor = BrandBlue
                        )
                    )

                    // Debug info
                    if (devicePrefix.isNotBlank()) {
                        Surface(
                            onClick = { showDebugInfo = !showDebugInfo },
                            shape = RoundedCornerShape(12.dp),
                            color = GlassWhite,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (showDebugInfo) "▼ 调试信息" else "▶ 调试信息",
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                                if (showDebugInfo) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "原始机器码: $rawDeviceId",
                                        fontSize = 11.sp,
                                        color = TextTertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "加密前缀: $devicePrefix",
                                        fontSize = 11.sp,
                                        color = TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = TextSecondary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onCreate(selectedModel, voicePrompt) },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBlue,
                            disabledContainerColor = BrandBlue.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }
}
