package com.needai.chat.data.remote.asr

import android.content.Context
import android.media.audiofx.AcousticEchoCanceler
import android.util.Log
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.SampleRate
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode

/**
 * 音频处理器，封装 AEC（回声消除）和 VAD（语音活动检测）。
 *
 * AEC：优先使用 Android 平台内置 AcousticEchoCanceler（需 AudioRecord 创建后传入 sessionId）。
 * VAD：使用 Silero DNN 模型（ONNX Runtime），对每帧音频检测是否为真实人声。
 */
class AudioProcessor(
    private val context: Context,
    /** TTS 参考信号缓冲区（由 PcmAudioPlayer 填充） */
    val ttsReference: TtsReferenceBuffer = TtsReferenceBuffer()
) {
    private var aec: AcousticEchoCanceler? = null
    private var vad: VadSilero? = null
    private var initialized = false

    /** 最后一次 VAD 检测结果 */
    var isSpeaking: Boolean = false
        private set

    /**
     * 初始化 AEC 和 VAD。
     * @param audioSessionId AudioRecord.getAudioSessionId()
     */
    fun init(audioSessionId: Int) {
        release()

        // 平台 AEC
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(audioSessionId)
                aec?.let {
                    it.enabled = true
                    Log.i(TAG, "平台 AEC 已启用 (session=$audioSessionId)")
                }
            } else {
                Log.w(TAG, "平台 AEC 不可用")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "启用平台 AEC 失败", e)
        }

        // Silero VAD
        try {
            vad = VadSilero(
                context = context,
                sampleRate = SampleRate.SAMPLE_RATE_16K,
                frameSize = FrameSize.FRAME_SIZE_512,  // 32ms @ 16kHz
                mode = Mode.NORMAL,
                silenceDurationMs = 300,
                speechDurationMs = 50
            )
            Log.i(TAG, "Silero VAD 已初始化")

            vad?.apply {
                // 预热：FRAME_SIZE_512 = 512 样本 = 1024 字节 @ 16bit
                isSpeech(ByteArray(1024))
            }
        } catch (e: Throwable) {
            Log.e(TAG, "初始化 VAD 失败", e)
        }

        initialized = true
    }

    /**
     * 检测音频帧是否包含真实人声。
     * @param audioFrame PCM 16-bit mono 音频帧（大小应与 FrameSize.FRAME_SIZE_512 一致）
     * @return true = 检测到人声
     */
    fun detectVoice(audioFrame: ByteArray): Boolean {
        if (audioFrame.size < 1024 || vad == null) return isSpeaking
        // Silero VAD 支持 ByteArray 直接输入（FRAME_SIZE_512 = 1024 字节）
        val result = vad!!.isSpeech(audioFrame)
        isSpeaking = result
        return result
    }

    /**
     * 释放 AEC 和 VAD 资源。
     */
    fun release() {
        aec?.let {
            try {
                it.enabled = false
                it.release()
            } catch (_: Throwable) {}
        }
        aec = null

        vad?.let {
            try {
                it.close()
            } catch (_: Throwable) {}
        }
        vad = null

        isSpeaking = false
        initialized = false
    }

    companion object {
        private const val TAG = "AudioProcessor"
    }
}
