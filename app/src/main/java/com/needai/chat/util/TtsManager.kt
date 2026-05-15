package com.needai.chat.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.needai.chat.data.remote.tts.CosyVoiceClient
import com.needai.chat.data.remote.tts.CosyVoiceParameters
import com.needai.chat.data.remote.tts.PcmAudioPlayer
import kotlinx.coroutines.*
import java.util.Locale

interface ITtsManager {
    fun speak(text: String, voiceId: String = "", onDone: (() -> Unit)? = null)
    fun stop()
    fun shutdown()
}

class AndroidTtsEngine(context: Context) : ITtsManager {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingSpeak: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.apply {
                    language = Locale.CHINESE
                    setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            mainHandler.post { pendingOnDone?.invoke(); pendingOnDone = null }
                        }
                        override fun onError(utteranceId: String?) {
                            mainHandler.post { pendingOnDone?.invoke(); pendingOnDone = null }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainHandler.post { pendingOnDone?.invoke(); pendingOnDone = null }
                        }
                    })
                }
                isInitialized = true
                // 执行初始化期间积压的 speak 请求
                pendingSpeak?.invoke()
                pendingSpeak = null
            }
        }
    }

    private var pendingOnDone: (() -> Unit)? = null

    override fun speak(text: String, voiceId: String, onDone: (() -> Unit)?) {
        tts?.stop()
        pendingOnDone = onDone
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts")
        } else {
            // 队列等待初始化完成
            pendingSpeak = { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts") }
        }
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

class TtsManagerImpl(
    private val context: Context,
    private val apiKey: String = "",
    private val parameters: CosyVoiceParameters = CosyVoiceParameters()
) : ITtsManager {

    private val androidTts = AndroidTtsEngine(context)
    private var cosyVoiceClient: CosyVoiceClient? = null
    private var audioPlayer: PcmAudioPlayer? = null
    private var synthesisJob: Job? = null
    private var pendingOnDone: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun speak(text: String, voiceId: String, onDone: (() -> Unit)?) {
        stop()
        pendingOnDone = onDone

        if (apiKey.isNotBlank()) {
            speakWithCosyVoice(text, voiceId, onDone)
        } else {
            androidTts.speak(text, voiceId, onDone)
        }
    }

    /**
     * 供外部更新参数（如 voice/model 变化时无需重建 TtsManagerImpl）
     */
    fun updateParameters(newParams: CosyVoiceParameters) {
        // TtsManagerImpl 目前只在构造函数接收 parameters，暂不提供动态更新
        // 后续如需动态更新可在此实现
    }

    private fun speakWithCosyVoice(text: String, voiceId: String, onDone: (() -> Unit)?) {
        synthesisJob = scope.launch(Dispatchers.IO) {
            try {
                // 始终保证有效音色，防止 CosyVoice API 因 voice 为空静默失败
                val effectiveVoice = when {
                    voiceId.isNotBlank() -> voiceId
                    parameters.voice.isNotBlank() -> parameters.voice
                    else -> "longanyang" // 默认系统音色，v3.5 兼容
                }
                val params = parameters.copy(voice = effectiveVoice)
                val client = CosyVoiceClient(apiKey, params)
                cosyVoiceClient = client
                val player = PcmAudioPlayer(params.sampleRate)
                audioPlayer = player

                player.play()

                client.synthesize(text).collect { chunk ->
                    // 错误时抛异常，触发 catch 块降级到 Android TTS
                    if (chunk.error != null) {
                        throw RuntimeException(chunk.error)
                    }
                    if (chunk.isLast) {
                        delay(500)
                        player.stop()
                        player.release()
                        withContext(Dispatchers.Main) {
                            onDone?.invoke()
                        }
                    } else if (chunk.data.isNotEmpty()) {
                        player.write(chunk.data)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TtsManager", "CosyVoice failed, fallback to Android TTS", e)
                withContext(Dispatchers.Main) {
                    androidTts.speak(text, "", onDone)
                }
            }
        }
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
        androidTts.stop()
        pendingOnDone = null
    }

    override fun shutdown() {
        stop()
        scope.cancel()
        androidTts.shutdown()
    }
}
