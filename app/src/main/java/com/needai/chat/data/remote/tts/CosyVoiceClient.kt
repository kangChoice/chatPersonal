package com.needai.chat.data.remote.tts

import android.util.Log
import com.alibaba.idst.nui.Constants
import com.alibaba.idst.nui.INativeStreamInputTtsCallback
import com.alibaba.idst.nui.NativeNui
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class CosyVoiceClient(
    private val apiKey: String,
    private val parameters: CosyVoiceParameters,
    deviceId: String = "needai_${UUID.randomUUID().toString().take(8)}"
) {
    private val nativeNui = NativeNui(Constants.ModeType.MODE_STREAM_INPUT_TTS)

    data class AudioChunk(
        val data: ByteArray,
        val isFirst: Boolean,
        val isLast: Boolean = false,
        val error: String? = null,
        val event: INativeStreamInputTtsCallback.StreamInputTtsEvent? = null
    )

    /**
     * 模式 A：一次性合成（点播朗读）
     * 参考官方 DashCosyVoiceStreamTtsActivity.AsyncPlayTts
     */
    /**
     * 一次性合成（点播朗读）。
     * 注意：正常完成（SYNTHESIS_COMPLETE）时不会自动 cancel，以支持同一
     * CosyVoiceClient 实例上依次调用多次 synthesize（分块朗读）。
     * 显式调用 [cancel] 或超时才会清理 NUI 状态。
     */
    suspend fun synthesize(text: String): Flow<AudioChunk> = callbackFlow {
        val timeoutMs = 120_000L
        var timedOut = false
        var finishedOk = false
        val taskId = UUID.randomUUID().toString()
        val ticket = genTicket()
        val params = parameters.toJson()
        var isFirst = true

        val callback = object : INativeStreamInputTtsCallback {
            override fun onStreamInputTtsEventCallback(
                event: INativeStreamInputTtsCallback.StreamInputTtsEvent?,
                taskId: String?,
                sessionId: String?,
                retCode: Int,
                errorMsg: String?,
                timestamp: String?,
                allResponse: String?
            ) {
                Log.d(TAG, "TTS event: $event, taskId=$taskId, retCode=$retCode, errorMsg=$errorMsg, allResponse=$allResponse")
                when (event) {
                    INativeStreamInputTtsCallback.StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_SYNTHESIS_COMPLETE -> {
                        finishedOk = true
                        trySend(AudioChunk(ByteArray(0), isFirst = false, isLast = true, event = event))
                        channel.close()
                    }
                    INativeStreamInputTtsCallback.StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_TASK_FAILED -> {
                        val err = "TASK_FAILED: retCode=$retCode, errorMsg=$errorMsg"
                        Log.e(TAG, err)
                        trySend(AudioChunk(ByteArray(0), isFirst = false, isLast = true, error = err, event = event))
                        channel.close()
                    }
                    else -> { /* no-op */ }
                }
            }

            override fun onStreamInputTtsDataCallback(data: ByteArray?) {
                if (data != null && data.isNotEmpty()) {
                    trySend(AudioChunk(data, isFirst = isFirst))
                    isFirst = false
                }
            }

            override fun onStreamInputTtsLogTrackCallback(
                level: Constants.LogLevel?, log: String?
            ) { /* logging */ }
        }

        val ret = nativeNui.asyncPlayStreamInputTts(
            callback,
            ticket,
            params,
            text,
            taskId,
            Constants.LogLevel.toInt(Constants.LogLevel.LOG_LEVEL_ERROR),
            true
        )

        if (ret != Constants.NuiResultCode.SUCCESS) {
            val errMsg = "asyncPlayStreamInputTts failed: ret=$ret"
            Log.e(TAG, errMsg)
            trySend(AudioChunk(ByteArray(0), isFirst = false, isLast = true, error = errMsg))
            channel.close()
            return@callbackFlow
        }

        // 超时保护：防止服务端无响应导致 collect 永久挂起
        launch(Dispatchers.IO) {
            delay(timeoutMs)
            if (!channel.isClosedForSend) {
                timedOut = true
                Log.e(TAG, "synthesize timed out after ${timeoutMs}ms")
                trySend(AudioChunk(ByteArray(0), isFirst = false, isLast = true, error = "TTS synthesis timed out after ${timeoutMs}ms"))
                channel.close()
                nativeNui.cancelStreamInputTts()
            }
        }

        awaitClose {
            // 正常完成的 task 不需要 cancel，避免破坏 NUI 状态
            // 只有超时或外部取消时才 cancel
            if (timedOut) nativeNui.cancelStreamInputTts()
        }
    }

    /**
     * 模式 B：流式合成（长文本/自动朗读）
     */
    suspend fun startStream(): Flow<AudioChunk> = callbackFlow {
        val timeoutMs = 120_000L
        var timedOut = false
        val taskId = UUID.randomUUID().toString()
        val ticket = genTicket()
        val params = parameters.toJson()
        var isFirst = true

        val callback = object : INativeStreamInputTtsCallback {
            override fun onStreamInputTtsEventCallback(
                event: INativeStreamInputTtsCallback.StreamInputTtsEvent?,
                taskId: String?,
                sessionId: String?,
                retCode: Int,
                errorMsg: String?,
                timestamp: String?,
                allResponse: String?
            ) {
                Log.d(TAG, "Stream event: $event, retCode=$retCode, errorMsg=$errorMsg")
                when (event) {
                    INativeStreamInputTtsCallback.StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_SYNTHESIS_COMPLETE,
                    INativeStreamInputTtsCallback.StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_TASK_FAILED -> {
                        val err = if (event == INativeStreamInputTtsCallback.StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_TASK_FAILED)
                            "TASK_FAILED: $errorMsg" else null
                        trySend(AudioChunk(ByteArray(0), isFirst = false, isLast = true, error = err, event = event))
                        channel.close()
                    }
                    else -> { /* no-op */ }
                }
            }

            override fun onStreamInputTtsDataCallback(data: ByteArray?) {
                if (data != null && data.isNotEmpty()) {
                    trySend(AudioChunk(data, isFirst = isFirst))
                    isFirst = false
                }
            }

            override fun onStreamInputTtsLogTrackCallback(
                level: Constants.LogLevel?, log: String?
            ) { /* logging */ }
        }

        val ret = nativeNui.startStreamInputTts(
            callback, ticket, params, taskId,
            Constants.LogLevel.toInt(Constants.LogLevel.LOG_LEVEL_ERROR), true
        )

        if (ret != Constants.NuiResultCode.SUCCESS) {
            val errMsg = "startStreamInputTts failed: ret=$ret"
            Log.e(TAG, errMsg)
            trySend(AudioChunk(ByteArray(0), isFirst = false, isLast = true, error = errMsg))
            channel.close()
            return@callbackFlow
        }

        // 超时保护
        launch(Dispatchers.IO) {
            delay(timeoutMs)
            if (!channel.isClosedForSend) {
                timedOut = true
                Log.e(TAG, "startStream timed out after ${timeoutMs}ms")
                trySend(AudioChunk(ByteArray(0), isFirst = false, isLast = true, error = "TTS stream timed out after ${timeoutMs}ms"))
                channel.close()
                nativeNui.cancelStreamInputTts()
            }
        }

        awaitClose {
            if (!timedOut) nativeNui.cancelStreamInputTts()
        }
    }

    fun sendText(text: String) {
        nativeNui.sendStreamInputTts(text)
    }

    fun asyncStop() {
        nativeNui.asyncStopStreamInputTts()
    }

    fun cancel() {
        if (nativeReleased.get()) return
        try {
            nativeNui.cancelStreamInputTts()
        } catch (e: Exception) {
            Log.e(TAG, "cancel error", e)
        }
    }

    fun release() {
        if (!nativeReleased.compareAndSet(false, true)) return
        try {
            nativeNui.cancelStreamInputTts()
            nativeNui.release()
        } catch (e: Exception) {
            Log.e(TAG, "release error", e)
        }
    }

    private fun genTicket(): String {
        return buildString {
            append("{")
            append("\"url\":\"wss://dashscope.aliyuncs.com/api-ws/v1/inference\"")
            append(",\"apikey\":\"$apiKey\"")
            append(",\"device_id\":\"needai_device_${UUID.randomUUID().toString().take(8)}\"")
            append(",\"log_track_level\":\"${Constants.LogLevel.toInt(Constants.LogLevel.LOG_LEVEL_NONE)}\"")
            append("}")
        }
    }

    companion object {
        private const val TAG = "CosyVoiceClient"
        private val nativeReleased = AtomicBoolean(false)
    }
}
