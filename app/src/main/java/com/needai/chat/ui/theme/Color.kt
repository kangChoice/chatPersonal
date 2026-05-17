package com.needai.chat.ui.theme

import androidx.compose.ui.graphics.Color

// ============ 浅光流色 品牌三原色（来自 index.html） ============
val BrandMint = Color(0xFF88E2CE)
val BrandPink = Color(0xFFFFAEC9)
val BrandBlue = Color(0xFF5B9DFF)

// ============ 页面基础色 ============
val BgPage = Color(0xFFF9F9FB)       // .fluid-glow background
val BgCard = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1A202C)
val TextSecondary = Color(0xFF4A5568) // 加深以提高透明度底色下的可读性
val TextTertiary = Color(0xFF718096) // 同上
val DividerColor = Color(0x1A718096)

// ============ 毛玻璃色（精确对应 HTML 值） ============
val GlassWhite = Color.White.copy(alpha = 0.7f)        // 通用毛玻璃底色
val GlassInput = Color.White.copy(alpha = 0.7f)     // .chat-input-wrap background: rgba(255,255,255,0.7)
val GlassTabBar = Color.White.copy(alpha = 0.8f)    // .jelly-tab-bar: rgba(255,255,255,0.8)
val GlassCardBg = Color.White.copy(alpha = 0.7f)    // 通用毛玻璃卡片
val GlassNavBtn = Color.Black.copy(alpha = 0.3f)    // .chat-nav-btn: rgba(0,0,0,0.3)
val GlassNavBtnBorder = Color.White.copy(alpha = 0.2f) // .chat-nav-btn border

// ============ 消息气泡（精确对应 HTML） ============
val BubbleAiBg = Color.Black.copy(alpha = 0.6f)     // .msg-bubble.ai-msg: rgba(0,0,0,0.6)
val BubbleAiBorder = Color.White.copy(alpha = 0.1f) // border: rgba(255,255,255,0.1)
val BubbleUserStart = BrandMint                     // linear-gradient(135deg, #88E2CE
val BubbleUserEnd = BrandBlue                       // , #5B9DFF)
val BubbleUserBorder = Color.White.copy(alpha = 0.3f)

// ============ 光晕圆斑（精确对应 HTML alpha） ============
val GlowMint = Color(0x4D88E2CE)  // rgba(136,226,206,0.3)
val GlowPink = Color(0x33FFAEC9)  // rgba(255,174,201,0.2)
val GlowBlue = Color(0x265B9DFF)  // rgba(91,157,255,0.15)

// ============ 功能性语义色 ============
val StatusGreen = BrandMint
val StatusRed = Color(0xFFEF5350)

// ============ Dark theme（保留骨架） ============
val DarkBg = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkText = Color(0xFFE6E1E5)
