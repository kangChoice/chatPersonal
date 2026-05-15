package com.needai.chat.data.remote.tts

import com.needai.chat.domain.model.SystemVoice

/**
 * 阿里云 CosyVoice 系统预置音色列表
 *
 * v3.5 系列虽然主打设计音色，但也向下兼容大部分 v3 系统音色。
 */
object SystemVoiceProvider {

    private val voiceMap: Map<String, List<SystemVoice>> = mapOf(
        "cosyvoice-v1" to listOf(
            SystemVoice("longanyang", "龙安洋", "阳光大男孩;中英文", listOf("cosyvoice-v1")),
            SystemVoice("zhimi", "知米", "治愈系女声;中文", listOf("cosyvoice-v1")),
            SystemVoice("aicheng", "艾成", "磁性男声;中文", listOf("cosyvoice-v1")),
            SystemVoice("xiaoyun", "晓云", "温柔女声;中文", listOf("cosyvoice-v1")),
        ),
        "cosyvoice-v2" to listOf(
            SystemVoice("longanyang", "龙安洋", "阳光大男孩;中英文", listOf("cosyvoice-v1", "cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus")),
            SystemVoice("zhimi", "知米", "治愈系女声;中文", listOf("cosyvoice-v1", "cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus")),
            SystemVoice("aicheng", "艾成", "磁性男声;中文", listOf("cosyvoice-v1", "cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus")),
            SystemVoice("shanlian", "山岚", "清亮男声;中文", listOf("cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus")),
            SystemVoice("yuexuan", "悦萱", "甜美女声;中英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus")),
            SystemVoice("xiaoxuan", "小萱", "邻家女孩;中英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus")),
            SystemVoice("yina", "伊娜", "知性女声;中英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus")),
            SystemVoice("xiaogang", "小刚", "阳光男声;中文", listOf("cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus")),
        ),
        "cosyvoice-v3-flash" to listOf(
            SystemVoice("longanyang", "龙安洋", "阳光大男孩;中英文", listOf("cosyvoice-v1", "cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus", "cosyvoice-v3.5-flash", "cosyvoice-v3.5-plus")),
            SystemVoice("zhimi", "知米", "治愈系女声;中文", listOf("cosyvoice-v1", "cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus", "cosyvoice-v3.5-flash", "cosyvoice-v3.5-plus")),
            SystemVoice("aicheng", "艾成", "磁性男声;中文", listOf("cosyvoice-v1", "cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus", "cosyvoice-v3.5-flash", "cosyvoice-v3.5-plus")),
            SystemVoice("shanlian", "山岚", "清亮男声;中文", listOf("cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus", "cosyvoice-v3.5-flash", "cosyvoice-v3.5-plus")),
            SystemVoice("yuexuan", "悦萱", "甜美女声;中英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus", "cosyvoice-v3.5-flash", "cosyvoice-v3.5-plus")),
            SystemVoice("xiaoxuan", "小萱", "邻家女孩;中英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus", "cosyvoice-v3.5-flash", "cosyvoice-v3.5-plus")),
            SystemVoice("yina", "伊娜", "知性女声;中英文", listOf("cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus", "cosyvoice-v3.5-flash", "cosyvoice-v3.5-plus")),
            SystemVoice("xiaogang", "小刚", "阳光男声;中文", listOf("cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus", "cosyvoice-v3.5-flash", "cosyvoice-v3.5-plus")),
        ),
        "cosyvoice-v3-plus" to listOf(
            SystemVoice("longanyang", "龙安洋", "阳光大男孩;中英文", listOf("cosyvoice-v1", "cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus", "cosyvoice-v3.5-flash", "cosyvoice-v3.5-plus")),
            SystemVoice("zhimi", "知米", "治愈系女声;中文", listOf("cosyvoice-v1", "cosyvoice-v2", "cosyvoice-v3-flash", "cosyvoice-v3-plus", "cosyvoice-v3.5-flash", "cosyvoice-v3.5-plus")),
        ),
    )

    fun getVoices(model: String): List<SystemVoice> {
        if (model.startsWith("cosyvoice-v3.5")) {
            return voiceMap["cosyvoice-v3-flash"] ?: emptyList()
        }
        return voiceMap.entries
            .firstOrNull { model.startsWith(it.key) }
            ?.value ?: emptyList()
    }

    fun hasSystemVoices(model: String): Boolean {
        return if (model.startsWith("cosyvoice-v3.5")) {
            true
        } else {
            voiceMap.entries.any { model.startsWith(it.key) }
        }
    }

    fun parseFromRaw(raw: String): ParsedVoiceItem? {
        val parts = raw.split("-", limit = 2)
        if (parts.size != 2) return null
        val voiceId = parts[0]
        val detailParts = parts[1].split(";")
        return ParsedVoiceItem(
            voiceId = voiceId,
            displayName = detailParts.getOrElse(0) { voiceId },
            description = detailParts.getOrElse(1) { "" },
            language = detailParts.getOrElse(2) { "" }
        )
    }
}

data class ParsedVoiceItem(
    val voiceId: String,
    val displayName: String,
    val description: String,
    val language: String
)
