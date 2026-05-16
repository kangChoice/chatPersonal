package com.needai.chat.data.remote.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

class PcmAudioPlayer(
    private val sampleRate: Int = 24000
) {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    @Synchronized
    fun play() {
        if (audioTrack != null) return
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
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
        val track = audioTrack ?: return
        if (!isPlaying) return
        try {
            track.write(data, 0, data.size)
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

    @Synchronized
    fun release() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "release failed", e)
        }
        audioTrack = null
    }

    companion object {
        private const val TAG = "PcmAudioPlayer"
    }
}
