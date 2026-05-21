package com.needai.chat.data.ilink

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.needai.chat.util.FileLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * iLink 桥接前台 Service。
 *
 * 职责：
 * 1. 启动 iLink 长轮询循环
 * 2. 将消息交给 WechatProcessor 处理
 * 3. 将回复发回微信
 * 4. 管理 Token 生命周期
 * 5. 通知栏管理
 */
@AndroidEntryPoint
class IlinkBridgeService : Service() {

    @Inject lateinit var ilinkClient: IlinkClient
    @Inject lateinit var wechatProcessor: WechatProcessor
    @Inject lateinit var authManager: IlinkAuthManager
    @Inject lateinit var notificationHelper: IlinkBridgeNotification
    @Inject lateinit var scheduleManager: IlinkScheduleManager
    @Inject lateinit var skillRepository: com.needai.chat.domain.repository.SkillRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // 缓存每个用户的 context_token，同一个 token 可回复最多 10 条消息
    // 不随新消息覆盖，保留首个 token 重复使用，到 ~8 次后刷新
    private val contextTokenCache = mutableMapOf<String, String>()
    private val contextTokenUsage = mutableMapOf<String, Int>()
    private var lastScheduleCheckMs: Long = 0L
    private var syncBuf: String? = null

    companion object {
        private const val TAG = "IlinkBridgeSvc"
        private const val MAX_USAGE = 8  // context_token 10 条上限留 2 条余量
        private const val SCHEDULE_CHECK_INTERVAL_MS = 30_000L
        const val ACTION_START = "com.needai.chat.action.ILINK_START"
        const val ACTION_STOP = "com.needai.chat.action.ILINK_STOP"
        const val ACTION_RECONNECT = "com.needai.chat.action.ILINK_RECONNECT"
        const val ACTION_POLL_AUTH = "com.needai.chat.action.ILINK_POLL_AUTH"
        const val ACTION_TEST_SCHEDULE = "com.needai.chat.action.ILINK_TEST_SCHEDULE"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: "null"
        FileLogger.i(TAG, "onStartCommand: action=$action, flags=$flags, startId=$startId")
        when (action) {
            ACTION_START -> startBridge()
            ACTION_STOP -> stopBridge()
            ACTION_RECONNECT -> {
                stopBridge()
                scope.launch {
                    delay(3000)
                    startBridge()
                }
            }
            ACTION_POLL_AUTH -> {
                startForeground(NOTIFICATION_ID, notificationHelper.buildAuthPolling())
            }
            ACTION_TEST_SCHEDULE -> {
                scope.launch {
                    val token = authManager.getToken()
                    if (token != null) {
                        scheduleManager.initialize()
                        scheduleManager.sendTestMessages(
                            botToken = token,
                            syncBuf = syncBuf,
                            contextTokenCache = contextTokenCache,
                            ilinkClient = ilinkClient
                        )
                    }
                }
            }
            else -> if (pollingJob?.isActive != true) startBridge()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        FileLogger.i(TAG, "onDestroy")
        stopBridge()
        scope.cancel()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NeedAi:IlinkBridge"
        ).apply {
            acquire(10 * 60 * 1000L)  // 10 分钟超时自动释放，防止异常未释放
        }
        FileLogger.i(TAG, "WakeLock 已获取")
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {}
        wakeLock = null
        FileLogger.i(TAG, "WakeLock 已释放")
    }

    private fun startBridge() {
        if (pollingJob?.isActive == true) {
            FileLogger.i(TAG, "startBridge: 已在运行中")
            return
        }

        FileLogger.i(TAG, "startBridge: 启动长轮询")
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, notificationHelper.buildConnecting())

        pollingJob = scope.launch {
            val token = authManager.getToken()
            if (token == null) {
                FileLogger.w(TAG, "startBridge: Token 为空，停止 Service")
                stopSelf()
                return@launch
            }

            startForeground(NOTIFICATION_ID, notificationHelper.buildConnecting())

            // 恢复已持久化的 context_token 缓存
            val savedTokens = authManager.getContextTokens()
            if (savedTokens.isNotEmpty()) {
                contextTokenCache.putAll(savedTokens)
                FileLogger.i(TAG, "startBridge: 恢复 contextTokenCache, ${savedTokens.size} 个用户")
            }

            // 初始化定时调度（加载当天已发送状态）
            FileLogger.i(TAG, "startBridge: 初始化定时调度")
            scheduleManager.initialize()

            var cursor: String? = null
            while (isActive) {
                try {
                    val result = ilinkClient.getUpdates(cursor, token)
                    result.onSuccess { response ->
                        if (response.messages.isEmpty()) {
                            FileLogger.d(TAG, "轮询 iLink 服务器是否有新消息中.....")
                        } else {
                            FileLogger.i(TAG, "收到 ${response.messages.size} 条新消息")
                        }
                        // 首次成功连接后更新通知
                        if (cursor == null) {
                            val ilinkId = authManager.getIlinkSkillId()
                            val skillId = if (!ilinkId.isNullOrBlank()) ilinkId else skillRepository.getSelectedSkillId()
                            val skillName = skillRepository.getSkillById(skillId)?.name ?: "ClawBot"
                            startForeground(NOTIFICATION_ID, notificationHelper.buildConnected(skillName))
                        }
                        syncBuf = response.syncBuf
                        for (msg in response.messages) {
                            FileLogger.i(TAG, "微信消息: msgId=${msg.msgId}, from=${msg.fromUserId.take(20)}, type=${msg.messageType}, text=${msg.text.take(100)}")
                            handleMessage(msg, token, syncBuf)
                        }
                        // 消息全部处理成功后再推进游标
                        cursor = response.newCursor
                    }.onFailure { error ->
                        if (error is TokenExpiredException) {
                            FileLogger.w(TAG, "Token 过期，停止桥接")
                            authManager.clearToken()
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                            return@launch
                        }
                        FileLogger.w(TAG, "getUpdates 失败: ${error.localizedMessage}")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    FileLogger.w(TAG, "长轮询异常: ${e.localizedMessage}")
                    delay(5000)
                }

                // 检查定时消息（节流 30 秒）
                val nowMs = System.currentTimeMillis()
                if (nowMs - lastScheduleCheckMs >= SCHEDULE_CHECK_INTERVAL_MS) {
                    scheduleManager.checkAndSend(
                        now = LocalTime.now(),
                        today = LocalDate.now().toString(),
                        botToken = token,
                        syncBuf = syncBuf,
                        contextTokenCache = contextTokenCache,
                        ilinkClient = ilinkClient
                    )
                    lastScheduleCheckMs = nowMs
                }
            }
        }
    }

    private suspend fun handleMessage(msg: IlinkMessage, token: String, syncBuf: String?) {
        if (msg.text.isBlank()) {
            FileLogger.i(TAG, "handleMessage: 空消息跳过")
            return
        }

        // 缓存首个 context_token 并复用，不随新消息覆盖（一个 token 可回复最多 10 条）
        val cachedToken = contextTokenCache[msg.fromUserId]
        val useCount = contextTokenUsage[msg.fromUserId] ?: 0
        val effectiveToken: String

        if (cachedToken == null || useCount >= MAX_USAGE) {
            // 首次或旧 token 已用满 → 用新消息的 token 刷新缓存
            if (msg.contextToken.isNotEmpty()) {
                contextTokenCache[msg.fromUserId] = msg.contextToken
                contextTokenUsage[msg.fromUserId] = 0
                persistContextTokens()
                FileLogger.i(TAG, "handleMessage: 初始化/刷新 contextToken userId=${msg.fromUserId.take(20)}, token=${msg.contextToken.take(20)}, reason=${if (cachedToken == null) "首次" else "token已用${useCount}次"}")
            }
            effectiveToken = msg.contextToken
        } else {
            // 复用缓存的 token
            effectiveToken = cachedToken
            FileLogger.i(TAG, "handleMessage: 复用缓存 contextToken=${cachedToken.take(20)}, 已用${useCount}次, 新消息token=${msg.contextToken.take(20)}")
        }

        FileLogger.i(TAG, "handleMessage: 处理消息 text=${msg.text.take(60)}")
        val result = wechatProcessor.process(msg.text)
        val replyText = if (result.error != null) "⚠️ ${result.error}" else result.text

        FileLogger.i(TAG, "handleMessage: 回复 text=${replyText.take(60)}")
        val sendResult = ilinkClient.sendMessage(
            toUserId = msg.fromUserId,
            text = replyText,
            contextToken = effectiveToken,
            botToken = token,
            syncBuf = syncBuf
        )
        if (sendResult.isFailure) {
            FileLogger.w(TAG, "handleMessage: sendMessage 失败, 游标不推进, 等待重试")
            throw sendResult.exceptionOrNull() ?: RuntimeException("sendMessage 失败")
        }
        // 发送成功后递增使用计数
        contextTokenUsage[msg.fromUserId] = (contextTokenUsage[msg.fromUserId] ?: 0) + 1
    }

    private fun persistContextTokens() {
        scope.launch {
            authManager.saveContextTokens(contextTokenCache.toMap())
        }
    }

    private fun stopBridge() {
        FileLogger.i(TAG, "stopBridge")
        pollingJob?.cancel()
        pollingJob = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
