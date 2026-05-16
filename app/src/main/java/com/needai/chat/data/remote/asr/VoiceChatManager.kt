package com.needai.chat.data.remote.asr

import com.needai.chat.util.FileLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 语音对话管理器，编排 ASR → 回调给外部 → 外部处理 LLM+TTS → 继续循环。
 *
 * 使用方式：
 * ```
 * val manager = VoiceChatManager(apiKey)
 * manager.setOnText { text ->
 *     // text 是用户说的一句完整的话，送给 LLM
 * }
 * manager.start()  // 开始监听
 * // ...
 * manager.stop()   // 结束
 * ```
 */
class VoiceChatManager(private val apiKey: String) {

    sealed class State {
        object Idle : State()
        object Connecting : State()          // ASR 初始化中
        object Listening : State()           // 等待/正在听用户说话
        object Processing : State()          // 用户说完了，外部处理中（LLM+TTS）
        object Speaking : State()            // TTS 播放中（可选，由外部设置）
        object Stopped : State()
        data class Error(val msg: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

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
     * 开始语音对话。初始化 ASR → 开始监听。
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
        // 使用 runBlocking 确保 stop/release 在 scope 取消前完成
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
        _state.value = State.Stopped
    }

    /**
     * 标记外部正在播放 TTS。用来更新状态显示。
     */
    fun setSpeaking(speaking: Boolean) {
        _state.value = if (speaking) State.Speaking else State.Listening
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
                    FileLogger.i(TAG, "开始监听")
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
     * 用户说完了，外部已经处理完 LLM+TTS，继续下一轮监听。
     */
    fun resumeListening() {
        if (!running) return
        _state.value = State.Listening
        scope?.launch {
            try {
                val startResult = asrEngine!!.start()
                if (!startResult.isSuccess) {
                    FileLogger.w(TAG, "继续监听失败: ${startResult.exceptionOrNull()?.message}")
                }
            } catch (e: Throwable) {
                FileLogger.e(TAG, "继续监听异常", e)
            }
        }
    }

    private val asrCallback = object : AsrEngine.Callback {
        override fun onPartialResult(text: String) {
            onPartialCallback?.invoke(text)
        }

        override fun onSentenceEnd(text: String) {
            FileLogger.i(TAG, "用户说完: $text")
            _state.value = State.Processing
            onTextCallback?.invoke(text)
        }

        override fun onError(code: Int, message: String) {
            FileLogger.e(TAG, "ASR 错误: code=$code, msg=$message")
            _state.value = State.Error(message)
            onErrorCallback?.invoke(message)
        }

        override fun onStateChanged(state: AsrEngine.AsrState) {
            // 不需要额外处理，VoiceChatManager 维护自己的 state
        }
    }

    companion object {
        private const val TAG = "VoiceChatManager"
    }
}
