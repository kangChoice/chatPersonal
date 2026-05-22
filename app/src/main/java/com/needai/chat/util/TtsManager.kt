package com.needai.chat.util

import com.needai.chat.data.remote.tts.CosyVoiceClient
import com.needai.chat.data.remote.tts.CosyVoiceParameters
import com.needai.chat.data.remote.tts.PcmAudioPlayer
import com.needai.chat.util.FileLogger
import kotlinx.coroutines.*

interface ITtsManager {
    fun speak(text: String, voiceId: String = "", onDone: (() -> Unit)? = null)
    fun startStreaming(voiceId: String = "", onDone: (() -> Unit)? = null): IStreamingTtsSession?
    fun stop()
    fun shutdown()
    fun isBusy(): Boolean
}

interface IStreamingTtsSession {
    fun sendText(text: String)
    fun finish()
}

/**
 * TTS 管理器，使用 CosyVoice 输出。
 *
 * 针对 CosyVoice 长文本合成约 40 字开始跳段/变声的问题，实现了以下策略：
 * - speak()：将长文本按句/30 字分块，每块独立合成后顺序播放
 * - startStreaming() + cycleStreamingSession()：每 30 字轮换 session
 * - speakQueued()：非打断追加朗读，用于流式自动朗读场景
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

    // ---------- 常量 ----------
    companion object {
        /** CosyVoice 单次合成超过约 40 字开始跳段，留余量取 30 */
        private const val CHUNK_MAX_CHARS = 30
        private const val TAG = "TtsManager"
    }

    // ---------- 协程 ----------
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ---------- 批量合成相关 ----------
    private var synthesisJob: Job? = null
    private var cosyVoiceClient: CosyVoiceClient? = null
    private var cosyVoiceVoiceId: String = ""

    // ---------- 流式合成相关 ----------
    private val activeStreamClients = mutableMapOf<Int, CosyVoiceClient>()
    private val activeStreamCollectors = mutableMapOf<Int, Job>()
    private var nextStreamId = 0

    // ---------- 持久化 CosyVoiceClient（避免跨轮次释放 NativeNui 单例） ----------
    private var persistentClient: CosyVoiceClient? = null
    private var persistentClientVoiceId: String = ""

    // ---------- 队列（非打断追加朗读） ----------
    private val speakQueue = mutableListOf<SpeakRequest>()
    private var queueJob: Job? = null
    private val queueLock = Any()

    // ---------- 共享音频播放器 ----------
    private var audioPlayer: PcmAudioPlayer? = null

    // ---------- 数据结构 ----------
    private data class SpeakRequest(
        val text: String,
        val voiceId: String,
        val onDone: (() -> Unit)? = null
    )

    // ======================================================================
    // 音色参数解析
    // ======================================================================
    private fun resolveParams(voiceId: String): CosyVoiceParameters {
        val effectiveVoice = when {
            voiceId.isNotBlank() -> voiceId
            parameters.voice.isNotBlank() -> parameters.voice
            else -> "longanyang"
        }
        val rawModel = voiceModelResolver?.invoke(effectiveVoice) ?: parameters.model
        android.util.Log.d(TAG, "resolveParams voiceId=$voiceId effectiveVoice=$effectiveVoice rawModel=$rawModel effectiveModel=$rawModel")
        return parameters.copy(voice = effectiveVoice, model = rawModel)
    }

    // ======================================================================
    // 文本分块
    // ======================================================================
    private fun chunkText(text: String, maxChars: Int = CHUNK_MAX_CHARS): List<String> {
        if (text.isEmpty()) return emptyList()
        if (text.length <= maxChars) return listOf(text)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            if (text.length - start <= maxChars) {
                chunks.add(text.substring(start))
                break
            }
            val end = start + maxChars
            val segment = text.substring(start, end)
            val sentenceBreak = segment.indexOfLast { it in "。！？；)）" }
            val secondaryBreak = segment.indexOfLast { it in "，、：" }
            val splitAt = when {
                sentenceBreak > maxChars / 2 -> sentenceBreak + 1
                secondaryBreak > maxChars / 3 -> secondaryBreak + 1
                else -> -1
            }
            if (splitAt > 0) {
                chunks.add(text.substring(start, start + splitAt))
                start += splitAt
            } else {
                chunks.add(text.substring(start, end))
                start = end
            }
        }
        return chunks
    }

    // ======================================================================
    // 方法 A：批量合成（点击朗读）— 打断模式
    // ======================================================================
    override fun speak(text: String, voiceId: String, onDone: (() -> Unit)?) {
        stop()

        if (apiKey.isBlank()) {
            android.util.Log.w(TAG, "API Key 未配置")
            FileLogger.w(TAG, "合成被中断: API Key 未配置，跳过语音播放")
            onDone?.invoke()
            return
        }

        val params = resolveParams(voiceId)
        android.util.Log.d(TAG, "speak触发 voiceId=$voiceId model=${params.model} voice=${params.voice} text=${text.take(50)}")
        val player = PcmAudioPlayer(params.sampleRate)
        audioPlayer = player

        val chunks = chunkText(text)
        if (chunks.isEmpty()) {
            onDone?.invoke()
            return
        }

        // 复用 CosyVoiceClient（voiceId 变化时重建，不 release 旧 client，避免破坏 NativeNui 单例）
        if (cosyVoiceClient == null || cosyVoiceVoiceId != voiceId) {
            cosyVoiceClient = CosyVoiceClient(apiKey, params)
            cosyVoiceVoiceId = voiceId
        }
        val client = cosyVoiceClient!!

        synthesisJob = scope.launch(Dispatchers.IO) {
            try {
                var isFirstChunk = true
                for (chunk in chunks) {
                    if (!isActive) break

                    val preBuffer = mutableListOf<ByteArray>()
                    var startedPlayback = false

                    // 预缓冲阈值：0.5 秒 (24000Hz × 2Bytes × 0.5s)
                    val PREBUFFER_THRESHOLD = sampleRateToBytes(params.sampleRate) / 2

                    client.synthesize(chunk).collect { audio ->
                        if (audio.error != null) throw RuntimeException(audio.error)
                        if (audio.isLast) {
                            // 落盘预缓冲数据
                            preBuffer.forEach { player.write(it) }
                            preBuffer.clear()
                            if (!startedPlayback) {
                                player.play()
                                startedPlayback = true
                            }
                        } else if (audio.data.isNotEmpty()) {
                            if (isFirstChunk && !startedPlayback) {
                                preBuffer.add(audio.data)
                                if (preBuffer.sumOf { it.size } >= PREBUFFER_THRESHOLD) {
                                    preBuffer.forEach { player.write(it) }
                                    preBuffer.clear()
                                    player.play()
                                    startedPlayback = true
                                }
                            } else {
                                player.write(audio.data)
                            }
                        }
                    }

                    isFirstChunk = false
                }

                drainAndStop(player)
                withContext(Dispatchers.Main) { onDone?.invoke() }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                android.util.Log.e(TAG, "合成失败", e)
                FileLogger.e(TAG, "批量合成失败，文本: ${chunks.joinToString("") { it.take(20) }}", e)
                drainAndStop(player)
                withContext(Dispatchers.Main) { onDone?.invoke() }
            } finally {
                // 不 release client，复用留给下次 speak()，避免 NativeNui 单例损坏
            }
        }
    }

    // ======================================================================
    // 方法 B：队列追加朗读（自动朗读增量）— 非打断，按序播放
    // 整个队列处理周期共享一个 CosyVoiceClient，避免 NUI 原生层跨实例失效
    // ======================================================================
    fun speakQueued(text: String, voiceId: String = "", onDone: (() -> Unit)? = null) {
        if (text.isEmpty()) return
        synchronized(queueLock) {
            speakQueue.add(SpeakRequest(text, voiceId, onDone))
        }
        if (queueJob?.isActive != true) {
            queueJob = scope.launch(Dispatchers.IO) {
                try {
                    while (isActive) {
                        val req = synchronized(queueLock) {
                            if (speakQueue.isEmpty()) null else speakQueue.removeFirst()
                        }
                        if (req == null) {
                            // 队列空：等待音频播放结束
                            audioPlayer?.awaitDrain()
                            // 再次检查队列（drain 期间可能有新消息入队）
                            if (synchronized(queueLock) { speakQueue.isEmpty() }) break
                            continue
                        }

                        ensurePersistentClient(req.voiceId)
                        android.util.Log.d(TAG, "speakQueued处理 voiceId=${req.voiceId} text=${req.text.take(50)}")
                        synthesizeUsingClient(req.text, persistentClient!!)
                        withContext(Dispatchers.Main) { req.onDone?.invoke() }
                    }
                } finally {
                    // 不 cancel/release persistentClient。
                    // 合成完成后 NativeNui C++ 单例可能已处于 418 损坏状态，
                    // 调用 cancel() 会触发 pthread_mutex_lock on destroyed mutex → SIGABRT。
                    // release 仅在 shutdown() 中执行。
                }
            }
        }
    }

    /**
     * 确保 persistentClient 存在且与目标 voiceId 匹配。
     * 只在 voiceId 变化时重建（避免破坏 NativeNui 单例）。
     */
    private fun ensurePersistentClient(voiceId: String) {
        if (persistentClient == null || persistentClientVoiceId != voiceId) {
            // 不 cancel 旧 client。旧 client 可能处于 418 损坏状态，
            // cancel() 会触发 NativeNui C++ mutex 操作 → SIGABRT。
            // 让旧对象自然 GC，新对象复用底层 C++ 单例（虽然损坏但至少不崩）。
            val params = resolveParams(voiceId)
            persistentClient = CosyVoiceClient(apiKey, params)
            persistentClientVoiceId = voiceId
        }
    }

    /** 使用已有 client 依次合成各个分块并写入共享播放器 */
    private suspend fun synthesizeUsingClient(text: String, client: CosyVoiceClient) {
        val player = audioPlayer ?: PcmAudioPlayer(24000).also { audioPlayer = it }
        player.play()

        val chunks = chunkText(text)
        for (chunk in chunks) {
            if (!currentCoroutineContext().isActive) break
            var hadError = false
            client.synthesize(chunk).collect { audio ->
                if (audio.error != null) {
                    android.util.Log.e(TAG, "队列合成失败: ${audio.error}")
                    FileLogger.e(TAG, "队列合成失败，分块文本: ${chunk.take(30)}，错误: ${audio.error}")
                    hadError = true
                    return@collect
                }
                if (audio.data.isNotEmpty()) {
                    player.write(audio.data)
                }
            }
            if (hadError) break // 遇到 418 后 NativeNui 内部状态已损坏，继续合成会触发 SIGABRT
        }
    }

    // ======================================================================
    // 方法 C：流式合成（自动朗读）— 支持 session 轮换
    // ======================================================================
    override fun startStreaming(voiceId: String, onDone: (() -> Unit)?): IStreamingTtsSession? {
        stop()
        if (apiKey.isBlank()) {
            onDone?.invoke()
            return null
        }

        val params = resolveParams(voiceId)
        android.util.Log.d(TAG, "startStreaming触发 voiceId=$voiceId model=${params.model} voice=${params.voice}")
        val player = PcmAudioPlayer(params.sampleRate)
        audioPlayer = player
        player.play()

        val streamId = createStreamSession(params)
        if (streamId < 0) {
            onDone?.invoke()
            return null
        }

        return StreamingSessionHandle(streamId, this)
    }

    /**
     * 创建一个新的流式 session，不打断已有 session。
     * 用于在 ChatScreen 中每 30 字轮换 session 以规避 CosyVoice 长文本限制。
     */
    fun cycleStreamingSession(): IStreamingTtsSession? {
        if (apiKey.isBlank()) return null
        val params = resolveParams("")
        if (audioPlayer == null) {
            android.util.Log.w(TAG, "cycleStreamingSession: audioPlayer 未初始化")
            FileLogger.w(TAG, "轮换流式 session 失败: audioPlayer 尚未初始化，请先调用 startStreaming")
            return null
        }
        val streamId = createStreamSession(params)
        if (streamId < 0) return null
        return StreamingSessionHandle(streamId, this)
    }

    /** 创建一个流式 session，启动独立协程收集音频到共享 player */
    private fun createStreamSession(params: CosyVoiceParameters): Int {
        val client = CosyVoiceClient(apiKey, params)
        val streamId = nextStreamId++

        synchronized(activeStreamClients) {
            activeStreamClients[streamId] = client
        }

        val collector = scope.launch(Dispatchers.IO) {
            try {
                client.startStream().collect { chunk ->
                    if (chunk.error != null) throw RuntimeException(chunk.error)
                    if (chunk.data.isNotEmpty()) {
                        audioPlayer?.write(chunk.data)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                android.util.Log.e(TAG, "流式 session#$streamId 失败", e)
                FileLogger.e(TAG, "流式 session #$streamId 异常终止", e)
            } finally {
                synchronized(activeStreamClients) {
                    activeStreamClients.remove(streamId)
                }
                client.release()
            }
        }

        synchronized(activeStreamCollectors) {
            activeStreamCollectors[streamId] = collector
        }

        return streamId
    }

    /** 对流式 session 发送文本 */
    internal fun sendStreamText(streamId: Int, text: String) {
        val client = synchronized(activeStreamClients) { activeStreamClients[streamId] }
        client?.sendText(text)
    }

    /** 结束流式 session */
    internal fun finishStream(streamId: Int) {
        val client = synchronized(activeStreamClients) { activeStreamClients.remove(streamId) }
        client?.asyncStop()
        synchronized(activeStreamCollectors) { activeStreamCollectors.remove(streamId) }
    }

    // ======================================================================
    // 控制
    // ======================================================================
    override fun stop() {
        // 取消批量合成（不 release CosyVoiceClient，避免破坏 NativeNui C++ 单例）
        synthesisJob?.cancel()
        synthesisJob = null
        cosyVoiceClient?.let {
            try { it.cancel() } catch (_: Exception) {}
        }

        // 取消队列
        queueJob?.cancel()
        queueJob = null
        synchronized(queueLock) { speakQueue.clear() }
        // persistentClient 只取消不 release，保持 NativeNui 单例状态
        persistentClient?.let {
            try { it.cancel() } catch (_: Exception) {}
        }

        // 取消所有流式 session
        synchronized(activeStreamCollectors) {
            activeStreamCollectors.values.forEach { it.cancel() }
            activeStreamCollectors.clear()
        }
        synchronized(activeStreamClients) {
            activeStreamClients.values.forEach {
                try { it.cancel(); it.release() } catch (_: Exception) {}
            }
            activeStreamClients.clear()
        }

        // 释放播放器
        audioPlayer?.let {
            try { it.stop(); it.release() } catch (_: Exception) {}
        }
        audioPlayer = null
    }

    override fun shutdown() {
        stop()
        cosyVoiceClient?.let {
            try { it.cancel(); it.release() } catch (_: Exception) {}
        }
        cosyVoiceClient = null
        persistentClient?.let {
            try { it.cancel(); it.release() } catch (_: Exception) {}
        }
        persistentClient = null
        persistentClientVoiceId = ""
        scope.cancel()
    }

    override fun isBusy(): Boolean {
        if (synthesisJob?.isActive == true) return true
        if (queueJob?.isActive == true) return true
        if (activeStreamCollectors.any { it.value.isActive }) return true
        if (audioPlayer?.isPlaying() == true) return true
        return false
    }

    private fun drainAndStop(player: PcmAudioPlayer) {
        try { player.drainAndStop() } catch (_: Exception) {}
    }

    private fun sampleRateToBytes(sampleRate: Int): Int = sampleRate * 2 // 16-bit mono
}

// ======================================================================
// 流式 session 句柄（返回给调用方）
// ======================================================================
private class StreamingSessionHandle(
    private val streamId: Int,
    private val manager: TtsManagerImpl
) : IStreamingTtsSession {
    private var finished = false

    @Synchronized
    override fun sendText(text: String) {
        if (!finished) manager.sendStreamText(streamId, text)
    }

    @Synchronized
    override fun finish() {
        if (finished) return
        finished = true
        manager.finishStream(streamId)
    }
}
