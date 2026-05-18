package com.needai.chat.data.remote.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.needai.chat.data.remote.asr.TtsReferenceBuffer

class PcmAudioPlayer(
    private val sampleRate: Int = 24000,
    /** TTS 参考信号缓冲区（供 AEC 使用） */
    val ttsReference: TtsReferenceBuffer = TtsReferenceBuffer()
) {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    /** 缓冲区设为最少 2 秒（96000 bytes @ 24kHz 16bit mono），
     *  远大于 getMinBufferSize (≈0.2s)，给网络抖动留足余量。 */
    private val bufferSize: Int
        get() {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val desired = sampleRate * 2 * 2 // 2 seconds
            return maxOf(minBuf, desired).coerceAtLeast(4096)
        }

    /** 确保 AudioTrack 已创建（惰性创建，可在 play() 或 write() 时触发） */
    @Synchronized
    private fun ensureTrack() {
        if (audioTrack != null) return
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    @Synchronized
    fun play() {
        if (isPlaying) return
        ensureTrack()
        try {
            audioTrack?.play()
            isPlaying = true
        } catch (e: Exception) {
            Log.e(TAG, "play failed", e)
            isPlaying = false
        }
    }

    @Synchronized
    fun write(data: ByteArray) {
        ensureTrack()
        if (audioTrack == null) return
        // 捕获 TTS 参考信号（用于 AEC 回声消除）
        ttsReference.write(data)
        try {
            audioTrack!!.write(data, 0, data.size)
        } catch (e: Exception) {
            Log.e(TAG, "write failed", e)
        }
    }

    @Synchronized
    fun stop() {
        isPlaying = false
        try {
            audioTrack?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "stop failed", e)
        }
    }

    /**
     * 等待 AudioTrack 缓冲区的数据播放完毕后再 stop/release。
     * 避免 [stop] 直接截断尾部音频。
     */
    @Synchronized
    fun drainAndStop() {
        val track = audioTrack ?: return
        if (!isPlaying) return
        try {
            var lastPos = track.playbackHeadPosition
            var stagnant = 0
            // 每 150ms 检查一次播放头，连续 4 次不动 ≈ 600ms 无声 = 缓冲区已空
            while (stagnant < 4) {
                Thread.sleep(150)
                val pos = track.playbackHeadPosition
                if (pos == lastPos) {
                    stagnant++
                } else {
                    stagnant = 0
                    lastPos = pos
                }
            }
        } catch (_: Exception) { }
        stop()
        release()
    }

    /**
     * 等待音频缓冲区自然播放完毕，然后将 isPlaying 置回 false。
     * 与 drainAndStop 的区别：不 stop/release AudioTrack，后续可直接继续 write+play。
     */
    @Synchronized
    fun awaitDrain() {
        val track = audioTrack ?: return
        if (!isPlaying) return
        try {
            var lastPos = track.playbackHeadPosition
            var stagnant = 0
            while (stagnant < 4) {
                Thread.sleep(150)
                val pos = track.playbackHeadPosition
                if (pos == lastPos) {
                    stagnant++
                } else {
                    stagnant = 0
                    lastPos = pos
                }
            }
        } catch (_: Exception) {}
        isPlaying = false
    }

    @Synchronized
    fun isPlaying(): Boolean = isPlaying

    @Synchronized
    fun release() {
        isPlaying = false
        audioTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "release failed", e)
            }
        }
        audioTrack = null
    }

    companion object {
        private const val TAG = "PcmAudioPlayer"
    }
}
