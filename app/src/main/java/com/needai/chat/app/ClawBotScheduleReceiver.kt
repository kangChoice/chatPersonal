package com.needai.chat.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.needai.chat.data.ilink.IlinkAuthManager
import com.needai.chat.data.ilink.IlinkBridgeService
import com.needai.chat.data.ilink.IlinkClient
import com.needai.chat.data.ilink.IlinkScheduleManager
import com.needai.chat.util.FileLogger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime

/**
 * ClawBot 定时发送闹钟接收器。
 *
 * 从 DataStore 读取持久化凭证后调用 sendMessage() POST。
 * 与 IlinkBridgeService 长轮询完全解耦，App 被杀后仍可触发。
 */
class ClawBotScheduleReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ClawBotReceiverEntryPoint {
        val authManager: IlinkAuthManager
        val scheduleManager: IlinkScheduleManager
        val ilinkClient: IlinkClient
    }

    companion object {
        private const val TAG = "WxSchedule:Rcvr"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "ClawBot 闹钟触发")
        val pendingResult = goAsync()

        Thread {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context, ClawBotReceiverEntryPoint::class.java
                )
                val authManager = entryPoint.authManager
                val scheduleManager = entryPoint.scheduleManager
                val ilinkClient = entryPoint.ilinkClient

                runBlocking {
                    val token = authManager.getToken()
                    if (token == null) {
                        Log.w(TAG, "Token 为空，跳过")
                        FileLogger.w(TAG, "Token 为空，跳过")
                        return@runBlocking
                    }

                    val syncBuf = authManager.getSyncBuf()
                    val contextTokenCache = authManager.getContextTokens()

                    scheduleManager.initialize()
                    val now = LocalTime.now()
                    val today = LocalDate.now().toString()

                    Log.d(TAG, "检查定时发送: now=$now, today=$today, users=${contextTokenCache.size}")
                    FileLogger.i(TAG, "闹钟触发: now=$now, today=$today, users=${contextTokenCache.size}")

                    scheduleManager.checkAndSend(
                        now = now,
                        today = today,
                        botToken = token,
                        syncBuf = syncBuf,
                        contextTokenCache = contextTokenCache,
                        ilinkClient = ilinkClient
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "处理异常: ${e.localizedMessage}", e)
                FileLogger.w(TAG, "处理异常: ${e.localizedMessage}")
            } finally {
                ClawBotScheduleScheduler.reschedule(context)
                tryStartBridgeService(context)
                pendingResult.finish()
            }
        }.start()
    }

    private fun tryStartBridgeService(context: Context) {
        try {
            val intent = Intent(context, IlinkBridgeService::class.java).apply {
                action = IlinkBridgeService.ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            Log.w(TAG, "启动BridgeService失败: ${e.localizedMessage}")
        }
    }
}
