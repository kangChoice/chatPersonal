package com.needai.chat.util

import com.needai.chat.data.remote.tts.CosyVoiceClient
import com.needai.chat.data.remote.tts.CosyVoiceParameters
import com.needai.chat.data.remote.tts.PcmAudioPlayer
import kotlinx.coroutines.*

interface ITtsManager {
    fun speak(text: String, voiceId: String = "", onDone: (() -> Unit)? = null)
    fun startStreaming(voiceId: String = "", onDone: (() -> Unit)? = null): IStreamingTtsSession?
    fun stop()
    fun shutdown()
}

interface IStreamingTtsSession {
    fun sendText(text: String)
    fun finish()
}

/**
 * TTS 管理器，仅使用外部 AI 模型（CosyVoice）输出。
 * 模型由音色自动决定，不需要全局配置。
 *
 * @param voiceModelResolver 根据 voiceId 返回对应的模型名，
 *   系统音色返回 "cosyvoice-v3-flash"，自定义音色返回其 targetModel。
 *   返回 null 时回退到 parameters.model。
 */
class TtsManagerImpl(
    private val apiKey: String = "",
    private val parameters: CosyVoiceParameters = CosyVoiceParameters(),
    private val voiceModelResolver: ((voiceId: String) -> String?)? = null
) : ITtsManager {

    private var cosyVoiceClient: CosyVoiceClient? = null
    private var audioPlayer: PcmAudioPlayer? = null
    private var synthesisJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun resolveParams(voiceId: String): CosyVoiceParameters {
        val effectiveVoice = when {
            voiceId.isNotBlank() -> voiceId
            parameters.voice.isNotBlank() -> parameters.voice
            else -> "longanyang"
        }
        val effectiveModel = voiceModelResolver?.invoke(effectiveVoice)
            ?: parameters.model
        return parameters.copy(voice = effectiveVoice, model = effectiveModel)
    }

    override fun speak(text: String, voiceId: String, onDone: (() -> Unit)?) {
        stop()

        if (apiKey.isBlank()) {
            android.util.Log.w("TtsManager", "API Key 未配置，无法使用 CosyVoice TTS")
            onDone?.invoke()
            return
        }

        synthesisJob = scope.launch(Dispatchers.IO) {
            try {
                val params = resolveParams(voiceId)
                val client = CosyVoiceClient(apiKey, params)
                cosyVoiceClient = client
                val player = PcmAudioPlayer(params.sampleRate)
                audioPlayer = player

                play()
                client.synthesize(text).collect { chunk ->
                    if (chunk.error != null) {
                        throw RuntimeException(chunk.error)
                    }
                    if (chunk.isLast) {
                        drainAndStop(player)
                        withContext(Dispatchers.Main) { onDone?.invoke() }
                    } else if (chunk.data.isNotEmpty()) {
                        player.write(chunk.data)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                android.util.Log.e("TtsManager", "CosyVoice 合成失败", e)
                withContext(Dispatchers.Main) { onDone?.invoke() }
            }
        }
    }

    override fun startStreaming(voiceId: String, onDone: (() -> Unit)?): IStreamingTtsSession? {
        stop()

        if (apiKey.isBlank()) {
            android.util.Log.w("TtsManager", "API Key 未配置")
            onDone?.invoke()
            return null
        }

        val params = resolveParams(voiceId)
        val client = CosyVoiceClient(apiKey, params)
        val player = PcmAudioPlayer(params.sampleRate)
        cosyVoiceClient = client
        audioPlayer = player
        player.play()

        synthesisJob = scope.launch(Dispatchers.IO) {
            try {
                client.startStream().collect { chunk ->
                    if (chunk.error != null) {
                        throw RuntimeException(chunk.error)
                    }
                    if (chunk.isLast) {
                        drainAndStop(player)
                        withContext(Dispatchers.Main) { onDone?.invoke() }
                    } else if (chunk.data.isNotEmpty()) {
                        player.write(chunk.data)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                android.util.Log.e("TtsManager", "流式合成失败", e)
                withContext(Dispatchers.Main) { onDone?.invoke() }
            }
        }

        return StreamingTtsSession(client)
    }

    override fun stop() {
        synthesisJob?.cancel()
        synthesisJob = null
        try {
            audioPlayer?.stop()
            audioPlayer?.release()
        } catch (_: Exception) {}
        audioPlayer = null
        try {
            cosyVoiceClient?.cancel()
            cosyVoiceClient?.release()
        } catch (_: Exception) {}
        cosyVoiceClient = null
    }

    override fun shutdown() {
        stop()
        scope.cancel()
    }

    // 这两个 helper 避免 speak / startStreaming 里重复代码
    private fun play() {
        audioPlayer?.play()
    }

    private suspend fun drainAndStop(player: PcmAudioPlayer) {
        withContext(Dispatchers.IO) { player.drainAndStop() }
    }
}

class StreamingTtsSession(
    private val client: CosyVoiceClient
) : IStreamingTtsSession {
    override fun sendText(text: String) {
        client.sendText(text)
    }

    override fun finish() {
        client.asyncStop()
    }
}
