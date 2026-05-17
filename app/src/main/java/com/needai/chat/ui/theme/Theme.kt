package com.needai.chat.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============ CompositionLocal 提供主题感知的自定义颜色 ============
data class AppColors(
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val dividerColor: Color,
    val glassWhite: Color,
    val glassInput: Color,
    val glassCardBg: Color,
    val glassTabBar: Color,
    val glassNavBtn: Color,
    val glassNavBtnBorder: Color,
    val bubbleAiBg: Color,
    val bubbleAiBorder: Color,
    val bgCard: Color
)

private val LightAppColors = AppColors(
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary,
    dividerColor = DividerColor,
    glassWhite = GlassWhite,
    glassInput = GlassInput,
    glassCardBg = GlassCardBg,
    glassTabBar = GlassTabBar,
    glassNavBtn = GlassNavBtn,
    glassNavBtnBorder = GlassNavBtnBorder,
    bubbleAiBg = BubbleAiBg,
    bubbleAiBorder = BubbleAiBorder,
    bgCard = BgCard
)

private val DarkAppColors = AppColors(
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    dividerColor = DarkDivider,
    glassWhite = DarkGlassWhite,
    glassInput = DarkGlassInput,
    glassCardBg = DarkGlassWhite,
    glassTabBar = DarkGlassWhite,
    glassNavBtn = Color.White.copy(alpha = 0.15f),
    glassNavBtnBorder = Color.White.copy(alpha = 0.1f),
    bubbleAiBg = DarkBubbleAiBg,
    bubbleAiBorder = DarkBubbleAiBorder,
    bgCard = DarkSurface
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlue.copy(alpha = 0.12f),
    onPrimaryContainer = TextPrimary,
    secondary = BrandPink,
    onSecondary = Color.White,
    secondaryContainer = BrandPink.copy(alpha = 0.12f),
    onSecondaryContainer = TextPrimary,
    tertiary = BrandMint,
    background = BgPage,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = GlassWhite,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = DividerColor,
    error = StatusRed,
    onError = Color.White
)

val DarkColorScheme = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlue.copy(alpha = 0.2f),
    onPrimaryContainer = DarkTextPrimary,
    secondary = BrandPink,
    onSecondary = Color.White,
    secondaryContainer = BrandPink.copy(alpha = 0.2f),
    onSecondaryContainer = DarkTextPrimary,
    tertiary = BrandMint,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkGlassWhite,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider,
    outlineVariant = DarkDivider,
    error = StatusRed,
    onError = Color.White
)

@Composable
fun NeedAiTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
