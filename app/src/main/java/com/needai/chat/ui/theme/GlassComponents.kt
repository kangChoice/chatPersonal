package com.needai.chat.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.math.PI

// ========================================================================
// Fluid Glow Background
// 精确对应 index.html 的 .fluid-glow + .orb + @keyframes floatGlow
//  CSS: 0%→33%: translate(30px,-50px) scale(1.1)   66%: translate(-20px,20px) scale(0.95)   100%: back
// ========================================================================

@Composable
fun FluidGlowBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    mintColor: Color = if (isDark) DarkGlowMint else GlowMint,
    pinkColor: Color = if (isDark) DarkGlowPink else GlowPink,
    blueColor: Color = if (isDark) DarkGlowBlue else GlowBlue
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Mint orb — no delay, 8s cycle
    val mintProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    // Pink orb — 2s delay (25% offset)
    val pinkProgress by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    // Blue orb — 4s delay (50% offset)
    val blueProgress by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    fun floatTransform(progress: Float): Triple<Float, Float, Float> {
        val p = progress - kotlin.math.floor(progress)
        return when {
            p < 0.33f -> {
                val t = p / 0.33f
                Triple(30f * t, -50f * t, 1f + 0.1f * t)
            }
            p < 0.66f -> {
                val t = (p - 0.33f) / 0.33f
                Triple(30f - 50f * t, -50f + 70f * t, 1.1f - 0.15f * t)
            }
            else -> {
                val t = (p - 0.66f) / 0.34f
                Triple(-20f + 20f * t, 20f - 20f * t, 0.95f + 0.05f * t)
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val d = this.density

        // Mint — 288x288, top left (-40,-40)
        val (mx, my, ms) = floatTransform(mintProgress)
        drawCircle(
            color = mintColor,
            radius = 144f * d * ms,
            center = Offset(mx * d, my * d)
        )

        // Pink — 320×320, center right
        val (px, py, ps) = floatTransform(pinkProgress)
        drawCircle(
            color = pinkColor,
            radius = 160f * d * ps,
            center = Offset(size.width + px * d, size.height * 0.5f + py * d)
        )

        // Blue — 384×384, bottom left
        val (bx, by, bs) = floatTransform(blueProgress)
        drawCircle(
            color = blueColor,
            radius = 192f * d * bs,
            center = Offset(40f + bx * d, size.height - 40f + by * d)
        )
    }
}

// ========================================================================
// Glass Capsule — 毛玻璃胶囊（对应 .chat-status-badge 等）
// ========================================================================

@Composable
fun GlassCapsule(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextSecondary,
    background: Color = Color.White.copy(alpha = 0.2f),
    fontSize: TextUnit = 10.sp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontSize = fontSize, letterSpacing = 0.5.sp)
    }
}

// ========================================================================
// Glass Circle Button — 对应 .chat-nav-btn / .moon-btn / .heart-btn
// ========================================================================

@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
    tint: Color = BrandBlue,
    bgColor: Color = Color.Black.copy(alpha = 0.3f),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(0.5.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ========================================================================
// Gradient Text — 对应 .home-header h1 渐变标题
// ========================================================================

@Composable
fun BrandGradientText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            brush = Brush.linearGradient(listOf(BrandBlue, BrandPink, BrandMint)),
            fontSize = fontSize
        )
    )
}
