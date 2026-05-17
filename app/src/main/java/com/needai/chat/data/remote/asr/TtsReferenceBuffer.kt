package com.needai.chat.data.remote.asr

/**
 * TTS 参考信号环形缓冲区。
 * 存储 TTS 播放前的 PCM 数据，供 AEC 作为参考信号使用。
 *
 * @param capacityBytes 缓冲区容量（字节）。16000×2×2=64000 ≈ 2秒 @ 16kHz 16bit
 */
class TtsReferenceBuffer(
    private val capacityBytes: Int = 64000
) {
    private val buffer = ByteArray(capacityBytes)
    private var writePos = 0
    private var filled = false

    /** 写入 TTS PCM 数据 */
    @Synchronized
    fun write(data: ByteArray) {
        for (i in data.indices) {
            buffer[writePos] = data[i]
            writePos = (writePos + 1) % capacityBytes
        }
        if (writePos == 0 || data.size > 0) {
            filled = true
        }
    }

    /** 读取最近 N 字节 */
    @Synchronized
    fun readLatest(size: Int): ByteArray {
        val actualSize = minOf(size, capacityBytes)
        val result = ByteArray(actualSize)
        if (!filled && writePos < actualSize) {
            return result // 缓冲区尚未填满，返回静音
        }
        for (i in 0 until actualSize) {
            val srcPos = (writePos - actualSize + i).let { if (it < 0) it + capacityBytes else it }
            result[i] = buffer[srcPos]
        }
        return result
    }

    @Synchronized
    fun reset() {
        writePos = 0
        filled = false
        buffer.fill(0)
    }
}
