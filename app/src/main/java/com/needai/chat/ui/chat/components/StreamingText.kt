package com.needai.chat.ui.chat.components

import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun StreamingText(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    fontSize: Float = 16f
) {
    val infiniteTransition = rememberInfiniteTransition()
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    val displayText = if (isStreaming) {
        text + if (cursorAlpha > 0.5f) "|" else " "
    } else {
        text
    }

    Text(
        text = displayText,
        fontSize = fontSize.sp,
        color = Color.White.copy(alpha = 0.9f),
        lineHeight = (fontSize * 1.6f).sp,
        modifier = modifier
    )
}
