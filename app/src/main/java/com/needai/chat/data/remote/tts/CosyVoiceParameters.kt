package com.needai.chat.data.remote.tts

data class CosyVoiceParameters(
    val model: String = "cosyvoice-v3.5-flash",
    val voice: String = "",
    val format: String = "pcm",
    val sampleRate: Int = 24000,
    val volume: Int = 50,
    val rate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val languageHints: List<String> = listOf("zh"),
    val enableAudioDecoder: Boolean = false
) {
    fun toJson(): String {
        return buildString {
            append("{")
            append("\"model\":\"$model\"")
            append(",\"voice\":\"$voice\"")
            append(",\"format\":\"$format\"")
            append(",\"sample_rate\":$sampleRate")
            append(",\"volume\":$volume")
            append(",\"rate\":$rate")
            append(",\"pitch\":$pitch")
            append(",\"language_hints\":[\"${languageHints.joinToString("\",\"")}\"]")
            append(",\"enable_audio_decoder\":$enableAudioDecoder")
            append("}")
        }
    }
}
