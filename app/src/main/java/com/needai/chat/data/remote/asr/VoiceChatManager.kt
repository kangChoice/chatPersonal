package com.needai.chat.data.remote.asr

import com.needai.chat.util.FileLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 语音对话管理器，编排 ASR → 回调给外部做 LLM+TTS。
 *
 * 持续监听模式：ASR 启动后一直运行，句子结束不停录音。
 * 外部通过 setOnText 接收完整句子，自行处理打断逻辑。
 *
 * 使用方式：
 * ```
 * val manager = VoiceChatManager(apiKey)
 * manager.setOnText { text ->
 *     // text 是用户说的一句完整的话
 *     // 如果 TTS 正在播放，自行决定是否打断
 * }
 * manager.start()
 * // ...
 * manager.stop()
 * ```
 */
class VoiceChatManager(private val apiKey: String) {

    sealed class State {
        object Idle : State()
        object Connecting : State()
        object Listening : State()
        object Stopped : State()
        data class Error(val msg: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** ASR 实时音量振幅 0-255，用于 UI 波形显示 */
    private val _asrAmplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _asrAmplitude.asStateFlow()

    private var asrEngine: AsrEngine? = null
    private var scope: CoroutineScope? = null
    private var onTextCallback: ((text: String) -> Unit)? = null
    private var onPartialCallback: ((text: String) -> Unit)? = null
    private var onErrorCallback: ((msg: String) -> Unit)? = null
    private var running = false
    private var isAsrInitialized = false

    fun setOnText(callback: (text: String) -> Unit) { onTextCallback = callback }
    fun setOnPartial(callback: (text: String) -> Unit) { onPartialCallback = callback }
    fun setOnError(callback: (msg: String) -> Unit) { onErrorCallback = callback }

    /**
     * 开始语音对话。初始化 ASR → 开始持续监听。
     */
    fun start() {
        if (running) return
        running = true
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        initAndStartListening()
    }

    /**
     * 停止语音对话。释放所有资源。
     */
    fun stop() {
        running = false
        if (isAsrInitialized) {
            kotlinx.coroutines.runBlocking {
                try {
                    asrEngine?.stop()
                    asrEngine?.release()
                } catch (e: Throwable) {
                    FileLogger.e(TAG, "停止 ASR 异常", e)
                }
            }
            asrEngine = null
            isAsrInitialized = false
        }
        scope?.cancel()
        scope = null
        _asrAmplitude.value = 0
        _state.value = State.Stopped
    }

    private fun initAndStartListening() {
        if (!running) return

        scope?.launch {
            try {
                _state.value = State.Connecting

                if (!isAsrInitialized) {
                    asrEngine = AsrEngine(apiKey)
                    asrEngine?.setCallback(asrCallback)

                    val result = asrEngine!!.initialize()
                    if (result.isFailure) {
                        FileLogger.e(TAG, "ASR 初始化失败: ${result.exceptionOrNull()?.message}")
                        _state.value = State.Error(result.exceptionOrNull()?.message ?: "ASR 初始化失败")
                        onErrorCallback?.invoke(result.exceptionOrNull()?.message ?: "ASR 初始化失败")
                        running = false
                        return@launch
                    }
                    isAsrInitialized = true
                }

                val startResult = asrEngine!!.start()
                if (startResult.isSuccess) {
                    _state.value = State.Listening
                    FileLogger.i(TAG, "开始持续监听（ASR 不间断）")

                    // 持续收集振幅数据
                    launch {
                        asrEngine?.amplitude?.collect { amp ->
                            _asrAmplitude.value = amp
                        }
                    }
                } else {
                    FileLogger.e(TAG, "ASR start 失败: ${startResult.exceptionOrNull()?.message}")
                    _state.value = State.Error(startResult.exceptionOrNull()?.message ?: "ASR start 失败")
                }
            } catch (e: Throwable) {
                FileLogger.e(TAG, "initAndStartListening 异常", e)
                _state.value = State.Error(e.message ?: "启动 ASR 异常")
                onErrorCallback?.invoke(e.message ?: "启动 ASR 异常")
                running = false
            }
        }
    }

    /**
     * 持续监听模式下无需手动恢复 ASR，此方法保留为无操作以兼容旧调用方。
     */
    @Deprecated("持续监听模式下 ASR 不会停止，无需调用 resumeListening")
    fun resumeListening() {
        // no-op
    }

    private val asrCallback = object : AsrEngine.Callback {
        override fun onPartialResult(text: String) {
            onPartialCallback?.invoke(text)
        }

        override fun onSentenceEnd(text: String) {
            FileLogger.i(TAG, "用户说话: $text")
            onTextCallback?.invoke(text)
        }

        override fun onError(code: Int, message: String) {
            FileLogger.e(TAG, "ASR 错误: code=$code, msg=$message")
            _state.value = State.Error(message)
            onErrorCallback?.invoke(message)
        }

        override fun onStateChanged(state: AsrEngine.AsrState) {
            // VoiceChatManager 维护自己的 state
        }
    }

    companion object {
        private const val TAG = "VoiceChatManager"
    }
}
