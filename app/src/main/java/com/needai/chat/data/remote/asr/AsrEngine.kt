package com.needai.chat.data.remote.asr

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.needai.chat.util.FileLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okio.ByteString
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebSocket 实时语音识别引擎，通过 Alibaba Cloud DashScope WebSocket API
 * 实现 ASR。完全绕过 NativeNui，避免与 TTS（CosyVoiceClient）的 singleton 冲突。
 *
 * 使用方式：
 * ```
 * val engine = AsrEngine(apiKey)
 * engine.setCallback(object : AsrEngine.Callback { ... })
 * engine.initialize()  // IO 线程，连接 WebSocket
 * engine.start()       // 发送 run-task，开始录音
 * // ... 自动录音和识别 ...
 * engine.stop()
 * engine.release()
 * ```
 */
class AsrEngine(
    private val context: Context,
    private val apiKey: String,
    private val deviceId: String = "needai_asr_${UUID.randomUUID().toString().take(8)}"
) {

    interface Callback {
        /** 中间识别结果（用户正在说话时） */
        fun onPartialResult(text: String)
        /** 一句话识别完整结果，可以送入 LLM */
        fun onSentenceEnd(text: String)
        /** 识别出错 */
        fun onError(code: Int, message: String)
        /** SDK 状态变更 */
        fun onStateChanged(state: AsrState)
    }

    sealed class AsrState {
        object Idle : AsrState()
        object Initializing : AsrState()
        object Ready : AsrState()
        object Listening : AsrState()
        object Stopping : AsrState()
        object Released : AsrState()
    }

    private var callback: Callback? = null
    private var state: AsrState = AsrState.Idle
    private var scope: CoroutineScope? = null
    private var audioJob: Job? = null
    private var isInitialized = AtomicBoolean(false)

    // 音量振幅（用于 UI 波形显示）
    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    private val audioProcessor = AudioProcessor(context)
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // OkHttp WebSocket
    private var okHttpClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    // AudioRecord
    private val sampleRate = 16000
    private val audioBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)
    private var audioRecord: AudioRecord? = null

    // 当前 task ID（每次 run-task 更新）
    private var currentTaskId: String = UUID.randomUUID().toString()

    private val wsBaseUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference"

    fun setCallback(cb: Callback) { callback = cb }

    /**
     * 初始化：创建 WebSocket 连接。必须在非 UI 线程调用。
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitialized.get()) {
            return@withContext Result.failure(Exception("ASR 已初始化，请先 release"))
        }

        state = AsrState.Initializing
        callback?.onStateChanged(state)

        try {
            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)  // 连接超时
                .readTimeout(0, TimeUnit.SECONDS)      // WebSocket 长连接无需读超时
                .pingInterval(30, TimeUnit.SECONDS)
                .build()

            val latch = CountDownLatch(1)
            var connectResult: Result<Unit>? = null

            val request = Request.Builder()
                .url(wsBaseUrl)
                .header("Authorization", "Bearer $apiKey")
                .build()

            webSocket = okHttpClient!!.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    FileLogger.i(TAG, "WebSocket 已连接, HTTP status=${response.code}")
                    connectResult = Result.success(Unit)
                    latch.countDown()
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    FileLogger.d(TAG, "收到服务端消息: ${text.take(200)}")
                    handleServerMessage(text)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    val msg = "WebSocket 连接失败: ${t.message}" +
                        (if (response != null) ", httpCode=${response.code}" else "")
                    FileLogger.e(TAG, msg)
                    response?.let { FileLogger.e(TAG, "响应 body: ${it.body?.string()}") }
                    connectResult = Result.failure(Exception(msg))
                    latch.countDown()
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    FileLogger.d(TAG, "WebSocket 已关闭: code=$code reason=$reason")
                }
            })

            // 等待连接建立（最多 10 秒）
            val connected = latch.await(10, TimeUnit.SECONDS)
            if (!connected) {
                val msg = "WebSocket 连接超时（10s）"
                FileLogger.e(TAG, msg)
                return@withContext Result.failure(Exception(msg))
            }
            connectResult?.getOrElse {
                return@withContext Result.failure(it)
            }

            isInitialized.set(true)
            state = AsrState.Ready
            callback?.onStateChanged(state)
            FileLogger.i(TAG, "ASR WebSocket 初始化成功")
            return@withContext Result.success(Unit)

        } catch (e: Throwable) {
            FileLogger.e(TAG, "ASR 初始化异常", e)
            state = AsrState.Idle
            callback?.onStateChanged(state)
            return@withContext Result.failure(Exception(e.message ?: "ASR 初始化失败", e))
        }
    }

    /**
     * 开始识别：发送 run-task，启动录音，流式上传音频。
     * 持续监听模式下 ASR 启动后一直运行直到 stop()/release()。
     */
    suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isInitialized.get() || webSocket == null) {
            return@withContext Result.failure(Exception("ASR 未初始化"))
        }
        if (state == AsrState.Listening) {
            return@withContext Result.success(Unit)
        }

        try {
            // 1. 发送 run-task
            currentTaskId = UUID.randomUUID().toString()
            val runTaskCmd = buildRunTaskCommand()
            val sent = webSocket!!.send(runTaskCmd)
            if (!sent) {
                return@withContext Result.failure(Exception("发送 run-task 失败"))
            }
            FileLogger.d(TAG, "已发送 run-task: taskId=$currentTaskId")

            // 2. 启动 AudioRecord
            startAudioRecord()

            // 3. 启动音频流式上传
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            audioJob = scope?.launch {
                streamAudioToWebSocket()
            }

            state = AsrState.Listening
            callback?.onStateChanged(state)
            FileLogger.i(TAG, "ASR 开始识别")
            return@withContext Result.success(Unit)

        } catch (e: Throwable) {
            FileLogger.e(TAG, "ASR start 异常", e)
            return@withContext Result.failure(Exception(e.message ?: "ASR start 失败", e))
        }
    }

    /**
     * 停止当前识别：停止录音，发送 finish-task。
     */
    suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) return@withContext Result.success(Unit)

        val wasListening = state == AsrState.Listening
        state = AsrState.Stopping
        callback?.onStateChanged(state)

        try {
            audioJob?.cancel()
            audioJob = null
            stopAudioRecord()

            if (webSocket != null && wasListening) {
                val finishCmd = buildFinishTaskCommand()
                webSocket!!.send(finishCmd)
                FileLogger.d(TAG, "已发送 finish-task")
            }

            FileLogger.i(TAG, "ASR 停止识别")
            return@withContext Result.success(Unit)
        } catch (e: Throwable) {
            FileLogger.e(TAG, "ASR stop 异常", e)
            return@withContext Result.failure(Exception(e.message ?: "ASR stop 失败", e))
        }
    }

    /**
     * 释放所有资源。release 后需要重新 initialize。
     */
    suspend fun release(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            audioProcessor.release()
            audioJob?.cancel()
            audioJob = null
            scope?.cancel()
            scope = null
            stopAudioRecord()

            // 关闭 WebSocket（stop() 已负责发送 finish-task，此处不再重复发送）
            if (webSocket != null) {
                try {
                    webSocket!!.close(1000, "client release")
                } catch (_: Throwable) { }
            }

            okHttpClient?.dispatcher?.executorService?.shutdown()
            okHttpClient?.connectionPool?.evictAll()
            okHttpClient = null
            webSocket = null
            isInitialized.set(false)
            state = AsrState.Released
            callback?.onStateChanged(state)
            FileLogger.i(TAG, "ASR 已释放")
            return@withContext Result.success(Unit)
        } catch (e: Throwable) {
            FileLogger.e(TAG, "ASR release 异常", e)
            return@withContext Result.failure(Exception(e.message ?: "ASR release 失败", e))
        }
    }

    // ===== AudioRecord 管理 =====

    private fun startAudioRecord() {
        if (audioRecord != null) return
        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                audioBufferSize
            )
            audioRecord = record
            audioProcessor.init(record.audioSessionId)
            record.startRecording()
            FileLogger.d(TAG, "AudioRecord 已启动")
        } catch (e: Throwable) {
            FileLogger.e(TAG, "启动 AudioRecord 失败", e)
        }
    }

    private fun stopAudioRecord() {
        try {
            audioRecord?.let {
                if (it.recordingState == android.media.AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Throwable) { }
        audioRecord = null
    }

    /**
     * 持续读取麦克风音频并通过 WebSocket 发送（作为 binary frame）。
     * 每帧约 100ms（3200 bytes @ 16kHz 16bit mono）。
     */
    private suspend fun streamAudioToWebSocket() {
        val record = audioRecord ?: return
        val buffer = ByteArray(3200) // 100ms
        var frameCount = 0

        while (currentCoroutineContext().isActive) {
            try {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val data = if (read < buffer.size) buffer.copyOf(read) else buffer
                    webSocket?.send(ByteString.of(*data))

                    // VAD: 按 1024 字节（512 样本 @ 16kHz 16bit）逐帧检测人声
                    var offset = 0
                    while (offset + 1024 <= data.size) {
                        audioProcessor.detectVoice(data.copyOfRange(offset, offset + 1024))
                        offset += 1024
                    }
                    _isSpeaking.value = audioProcessor.isSpeaking

                    // 振幅：仅在有 VAD 人声时更新，否则保持 0 让波形静态
                    val amp = if (audioProcessor.isSpeaking) {
                        data.filterIndexed { i, _ -> i % 2 == 1 }
                            .maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
                    } else 0
                    _amplitude.value = (amp * 4).coerceIn(0, 255)
                    frameCount++
                    if (frameCount % 50 == 0) { // 每 5 秒
                        FileLogger.d(TAG, "音频流式上传中: 已发送 ${frameCount * 3200 / 32000}s 音频")
                    }
                } else if (read == AudioRecord.ERROR_INVALID_OPERATION) {
                    FileLogger.e(TAG, "AudioRecord 无效操作")
                    delay(100)
                }
            } catch (e: Throwable) {
                FileLogger.e(TAG, "音频流式上传异常", e)
                delay(100)
            }
        }
    }

    // ===== WebSocket 消息处理 =====

    private fun handleServerMessage(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val header = json.getAsJsonObject("header")
            val event = header?.get("event")?.asString ?: return

            FileLogger.d(TAG, "服务端事件: $event")

            when (event) {
                "task-started" -> {
                    FileLogger.d(TAG, "任务已开始: taskId=${header.get("task_id")?.asString}")
                }
                "task-failed" -> {
                    val errCode = header.get("error_code")?.asString ?: "unknown"
                    val errMsg = header.get("error_message")?.asString ?: "未知错误"
                    FileLogger.e(TAG, "任务失败: code=$errCode, msg=$errMsg")
                    callback?.onError(-1, "[$errCode] $errMsg")
                }
                "result-generated" -> {
                    handleResultGenerated(json)
                }
                "recognition-result" -> {
                    handleRecognitionResult(json)
                }
                "recognition-completed" -> {
                    FileLogger.d(TAG, "识别完成: taskId=${header.get("task_id")?.asString}")
                }
                "task-completed" -> {
                    FileLogger.d(TAG, "任务完成: taskId=${header.get("task_id")?.asString}")
                }
                "task-succeeded" -> {
                    FileLogger.d(TAG, "任务成功: taskId=${header.get("task_id")?.asString}")
                }
                else -> {
                    FileLogger.d(TAG, "未处理事件: $event, 完整消息: ${text.take(200)}")
                }
            }
        } catch (e: Throwable) {
            FileLogger.e(TAG, "处理服务端消息异常", e)
        }
    }

    private fun handleResultGenerated(json: JsonObject) {
        try {
            val payload = json.getAsJsonObject("payload") ?: return
            val output = payload.getAsJsonObject("output") ?: return
            val sentence = output.getAsJsonObject("sentence") ?: return
            val text = sentence.get("text")?.asString ?: ""
            if (text.isBlank()) return

            val isSentenceEnd = sentence.get("end_time")?.isJsonNull == false

            if (isSentenceEnd) {
                FileLogger.i(TAG, "句子结束: $text")
                // 持续监听模式：不停止录音/音频流，让 ASR 服务器保持识别
                callback?.onSentenceEnd(text)
            } else {
                FileLogger.d(TAG, "中间结果: $text")
                callback?.onPartialResult(text)
            }
        } catch (e: Throwable) {
            FileLogger.e(TAG, "解析 result-generated 异常", e)
        }
    }

    private fun handleRecognitionResult(json: JsonObject) {
        try {
            val payload = json.getAsJsonObject("payload")
            if (payload == null) return

            val result = payload.getAsJsonObject("result") ?: return
            val transcripts = result.getAsJsonArray("transcripts")
            if (transcripts == null || transcripts.size() == 0) return

            val text = transcripts[0].asJsonObject.get("text")?.asString ?: ""
            if (text.isBlank()) return

            val isSentenceEnd = result.get("sentence_end")?.asBoolean ?: false

            if (isSentenceEnd) {
                FileLogger.i(TAG, "句子结束: $text")
                // 持续监听模式：不停止录音/音频流
                callback?.onSentenceEnd(text)
            } else {
                FileLogger.d(TAG, "中间结果: $text")
                callback?.onPartialResult(text)
            }
        } catch (e: Throwable) {
            FileLogger.e(TAG, "解析识别结果异常", e)
        }
    }

    // ===== 命令构造 =====

    private fun buildRunTaskCommand(): String {
        val cmd = JsonObject()
        val header = JsonObject().apply {
            addProperty("action", "run-task")
            addProperty("task_id", currentTaskId)
            addProperty("streaming", "duplex")
        }
        val payload = JsonObject().apply {
            addProperty("model", "fun-asr-realtime")
            addProperty("task_group", "audio")
            addProperty("task", "asr")
            addProperty("function", "recognition")
            val input = JsonObject().apply {
                addProperty("sr_format", "pcm")
                addProperty("sample_rate", 16000)
            }
            add("input", input)
            val params = JsonObject().apply {
                addProperty("enable_partial_result", true)
                addProperty("enable_punctuation", true)
                addProperty("enable_semantic_sentence_detection", true)
                addProperty("max_sentence_silence", 800)
                addProperty("speech_noise_threshold", 0.0)
            }
            add("parameters", params)
        }
        cmd.add("header", header)
        cmd.add("payload", payload)
        return cmd.toString()
    }

    private fun buildFinishTaskCommand(): String {
        val cmd = JsonObject()
        val header = JsonObject().apply {
            addProperty("action", "finish-task")
            addProperty("task_id", currentTaskId)
        }
        val payload = JsonObject()
        payload.add("input", JsonObject())
        cmd.add("header", header)
        cmd.add("payload", payload)
        return cmd.toString()
    }

    companion object {
        private const val TAG = "AsrEngine"
    }
}
