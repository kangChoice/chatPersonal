package com.needai.chat.ui.chat.components

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
