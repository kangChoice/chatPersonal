package com.needai.chat.data.ilink

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.needai.chat.util.FileLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

private val Context.scheduleStore: DataStore<Preferences> by preferencesDataStore(name = "ilink_schedule")

data class ScheduleState(
    @SerializedName("date") val date: String = "",
    @SerializedName("sent_fixed") val sentFixed: List<String> = emptyList(),
    @SerializedName("sent_random") val sentRandom: List<String> = emptyList(),
    @SerializedName("random_times") val randomTimes: List<String> = emptyList(),
    @SerializedName("random_count") val randomCount: Int = 3
)

data class FixedScheduleItem(
    val time: String = "08:00",
    val message: String = ""
)

data class ScheduleConfig(
    val fixedMessages: List<FixedScheduleItem> = listOf(
        FixedScheduleItem("08:00", "起床了吗？要好好吃早饭噢"),
        FixedScheduleItem("12:00", "中午要好好吃饭哦！我会担心的"),
        FixedScheduleItem("21:00", "晚安！好梦")
    ),
    val randomMessage: String = "在干嘛?要好好生活哦",
    val randomStartTime: String = "08:00",
    val randomEndTime: String = "18:00",
    val randomCount: Int = 3,
    val randomEnabled: Boolean = true
)

@Singleton
class IlinkScheduleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "IlinkSchedule"
        private val SCHEDULE_STATE_KEY = stringPreferencesKey("schedule_state")
        private val SCHEDULE_CONFIG_KEY = stringPreferencesKey("schedule_config")
        private val FMT = DateTimeFormatter.ofPattern("HH:mm")
    }

    // 内存状态，避免每次检查都读 DataStore
    private var todayDate: String = ""
    private var sentFixed: MutableSet<String> = mutableSetOf()
    private var sentRandom: MutableSet<String> = mutableSetOf()
    private var randomTimes: List<String> = emptyList()
    private var randomCount: Int = 3
    private var randomEnabled: Boolean = true
    private var scheduleConfig: ScheduleConfig = ScheduleConfig()
    private var initialized = false

    /**
     * 加载持久化状态，必须在 checkAndSend 之前调用
     */
    suspend fun initialize() {
        if (initialized) return
        loadConfig()
        loadState()
        initialized = true
        FileLogger.i(TAG, "initialize: date=$todayDate, fixed=${sentFixed.size}, random=${sentRandom.size}, times=$randomTimes")
    }

    /**
     * 每次长轮询循环后调用，检查是否需要发送定时消息
     */
    suspend fun checkAndSend(
        now: LocalTime,
        today: String,
        botToken: String,
        syncBuf: String?,
        contextTokenCache: Map<String, String>,
        ilinkClient: IlinkClient
    ) {
        if (!initialized) {
            FileLogger.w(TAG, "checkAndSend: 尚未初始化")
            return
        }

        // 跨天 → 重置
        if (today != todayDate) {
            resetDay(today, now)
            FileLogger.i(TAG, "checkAndSend: 新的一天, randomTimes=$randomTimes")
        }

        var changed = false

        // 固定时间点
        for (item in scheduleConfig.fixedMessages) {
            val time = item.time
            val text = item.message
            if (time !in sentFixed) {
                val scheduled = LocalTime.parse(time, FMT)
                if (!now.isBefore(scheduled)) {
                    // 只在实际时间窗口内（±5分钟）真正发送，防止重启后堆积发送
                    val inWindow = !now.isAfter(scheduled.plusMinutes(5))
                    if (inWindow) {
                        FileLogger.i(TAG, "checkAndSend: 固定消息触发 $time → $text")
                        sendToAllUsers(text, botToken, syncBuf, contextTokenCache, ilinkClient)
                    } else {
                        FileLogger.i(TAG, "checkAndSend: 固定消息 $time 已过期，跳过发送")
                    }
                    sentFixed.add(time)
                    changed = true
                }
            }
        }

        // 随机时间点（仅在启用时处理）
        if (randomEnabled) for (time in randomTimes) {
            if (time !in sentRandom) {
                val scheduled = LocalTime.parse(time, FMT)
                if (!now.isBefore(scheduled)) {
                    val inWindow = !now.isAfter(scheduled.plusMinutes(5))
                    if (inWindow) {
                        FileLogger.i(TAG, "checkAndSend: 随机消息触发 $time → ${scheduleConfig.randomMessage}")
                        sendToAllUsers(scheduleConfig.randomMessage, botToken, syncBuf, contextTokenCache, ilinkClient)
                    } else {
                        FileLogger.i(TAG, "checkAndSend: 随机消息 $time 已过期，跳过发送")
                    }
                    sentRandom.add(time)
                    changed = true
                }
            }
        }

        if (changed) persistState()
    }

    /** 获取随机消息数量 */
    fun getRandomCount(): Int = randomCount

    /** 随机消息是否启用 */
    fun isRandomEnabled(): Boolean = randomEnabled

    /** 启用/禁用随机消息 */
    suspend fun setRandomEnabled(enabled: Boolean) {
        randomEnabled = enabled
        scheduleConfig = scheduleConfig.copy(randomEnabled = enabled)
        if (enabled) {
            randomTimes = generateRandomTimes(scheduleConfig.randomStartTime, scheduleConfig.randomEndTime, sentRandom)
        } else {
            randomTimes = emptyList()
        }
        persistConfig()
        persistState()
        FileLogger.i(TAG, "setRandomEnabled: $enabled")
    }

    /** 获取今日所有定时消息预览（时间, 消息文本），含时间前缀 */
    fun getTodaySchedulePreview(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        for (item in scheduleConfig.fixedMessages) {
            result.add(item.time to item.message)
        }
        for (time in randomTimes) {
            result.add(time to scheduleConfig.randomMessage)
        }
        result.sortBy { it.first }
        return result
    }

    /**
     * 测试用：发送今日所有定时消息（带时间前缀），不标记已发送
     */
    suspend fun sendTestMessages(
        botToken: String,
        syncBuf: String?,
        contextTokenCache: Map<String, String>,
        ilinkClient: IlinkClient
    ) {
        FileLogger.i(TAG, "sendTestMessages: 测试发送开始")
        for ((time, text) in getTodaySchedulePreview()) {
            val msg = "【$time】$text"
            sendToAllUsers(msg, botToken, syncBuf, contextTokenCache, ilinkClient)
            delay(800)  // 避免 iLink 速率限制 (ret=-2)
        }
        FileLogger.i(TAG, "sendTestMessages: 测试发送完成")
    }

    /** 更新随机消息数量并重新生成时间点（保留已发送的） */
    suspend fun setRandomCount(count: Int) {
        val clamped = count.coerceIn(1, 60)
        if (clamped == randomCount) return
        randomCount = clamped
        scheduleConfig = scheduleConfig.copy(randomCount = clamped)
        randomTimes = generateRandomTimes(scheduleConfig.randomStartTime, scheduleConfig.randomEndTime, sentRandom)
        persistConfig()
        persistState()
        FileLogger.i(TAG, "setRandomCount: $clamped, newTimes=$randomTimes")
    }

    // ===== 内部 =====

    private suspend fun sendToAllUsers(
        text: String,
        botToken: String,
        syncBuf: String?,
        contextTokenCache: Map<String, String>,
        ilinkClient: IlinkClient
    ) {
        if (contextTokenCache.isEmpty()) {
            FileLogger.w(TAG, "sendToAllUsers: contextTokenCache 为空，无法发送")
            return
        }
        for ((userId, ctxToken) in contextTokenCache) {
            val result = ilinkClient.sendMessage(
                toUserId = userId,
                text = text,
                contextToken = ctxToken,
                botToken = botToken,
                syncBuf = syncBuf
            )
            result.onSuccess {
                FileLogger.i(TAG, "sendToAllUsers: userId=${userId.take(20)} 发送成功")
            }.onFailure { e ->
                FileLogger.w(TAG, "sendToAllUsers: userId=${userId.take(20)} 发送失败: ${e.localizedMessage}")
            }
            delay(300)  // 避免多用户连续发送触发速率限制
        }
    }

    private fun resetDay(today: String, now: LocalTime) {
        todayDate = today
        sentFixed.clear()
        sentRandom.clear()
        randomTimes = generateRandomTimes(
            String.format("%02d:%02d", now.hour, now.minute),
            scheduleConfig.randomEndTime,
            emptySet()
        )
    }

    private fun generateRandomTimes(startTime: String, endTime: String, exclude: Set<String>): List<String> {
        val start = parseMinuteOfDay(startTime) ?: return emptyList()
        val end = parseMinuteOfDay(endTime) ?: return emptyList()
        val totalMinutes = end - start
        if (totalMinutes <= 0) return emptyList()

        val count = min(randomCount, totalMinutes)
        if (count <= 0) return emptyList()
        val segmentSize = totalMinutes / count

        val result = mutableListOf<Int>()
        for (i in 0 until count) {
            val segStart = start + i * segmentSize
            val segEnd = if (i == count - 1) end else segStart + segmentSize - 1
            val minutesInSeg = segEnd - segStart + 1
            if (minutesInSeg <= 0) continue
            result.add(segStart + (0 until minutesInSeg).random())
        }

        return result.sorted()
            .map { totalMin ->
                String.format("%02d:%02d", totalMin / 60, totalMin % 60)
            }
            .filter { it !in exclude }
    }

    private fun parseMinuteOfDay(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    /** 获取完整配置 */
    fun getConfig(): ScheduleConfig = scheduleConfig

    /** 原子替换所有固定消息 */
    suspend fun setFixedMessages(messages: List<FixedScheduleItem>) {
        scheduleConfig = scheduleConfig.copy(fixedMessages = messages)
        persistConfig()
    }

    /** 更新随机消息文本 */
    suspend fun setRandomMessage(text: String) {
        scheduleConfig = scheduleConfig.copy(randomMessage = text)
        persistConfig()
    }

    /** 更新随机消息时间范围 */
    suspend fun setRandomTimeRange(startTime: String, endTime: String) {
        val start = parseMinuteOfDay(startTime) ?: return
        val end = parseMinuteOfDay(endTime) ?: return
        if (end - start < 30) return  // 最小 30 分钟范围
        val clampedStart = start.coerceIn(0, 23 * 60 + 59)
        val clampedEnd = end.coerceIn(clampedStart + 30, 23 * 60 + 59)
        val startStr = String.format("%02d:%02d", clampedStart / 60, clampedStart % 60)
        val endStr = String.format("%02d:%02d", clampedEnd / 60, clampedEnd % 60)
        scheduleConfig = scheduleConfig.copy(
            randomStartTime = startStr,
            randomEndTime = endStr
        )
        randomTimes = generateRandomTimes(startStr, endStr, sentRandom)
        persistConfig()
        persistState()
        FileLogger.i(TAG, "setRandomTimeRange: $startStr-$endStr, newTimes=$randomTimes")
    }

    // ===== DataStore 持久化 =====

    private suspend fun loadConfig() {
        val json = context.scheduleStore.data.map { prefs ->
            prefs[SCHEDULE_CONFIG_KEY]
        }.first().orEmpty()
        if (json.isNotEmpty()) {
            try {
                scheduleConfig = gson.fromJson(json, ScheduleConfig::class.java)
                // 迁移：旧配置没有 randomEnabled 字段，Gson 默认 false
                if (!json.contains("randomEnabled") && scheduleConfig.randomCount > 0) {
                    scheduleConfig = scheduleConfig.copy(randomEnabled = true)
                }
            } catch (e: Exception) {
                FileLogger.w(TAG, "loadConfig: 解析失败 ${e.localizedMessage}")
            }
        }
    }

    private suspend fun persistConfig() {
        val json = gson.toJson(scheduleConfig)
        context.scheduleStore.edit { prefs ->
            prefs[SCHEDULE_CONFIG_KEY] = json
        }
    }

    private suspend fun loadState() {
        val json = context.scheduleStore.data.map { prefs ->
            prefs[SCHEDULE_STATE_KEY]
        }.first().orEmpty()
        if (json.isEmpty()) {
            FileLogger.i(TAG, "loadState: 首次，无已保存状态")
            return
        }
        try {
            val state = gson.fromJson(json, ScheduleState::class.java)
            todayDate = state.date
            sentFixed = state.sentFixed.toMutableSet()
            sentRandom = state.sentRandom.toMutableSet()
            randomTimes = state.randomTimes.toMutableList()
            // 迁移：优先使用 config 中的 randomCount
            if (scheduleConfig.randomCount > 0) {
                randomCount = scheduleConfig.randomCount
            } else {
                randomCount = state.randomCount.coerceIn(1, 60)
            }
        } catch (e: Exception) {
            FileLogger.w(TAG, "loadState: 解析失败 ${e.localizedMessage}")
        }
    }

    private suspend fun persistState() {
        val state = ScheduleState(
            date = todayDate,
            sentFixed = sentFixed.toList(),
            sentRandom = sentRandom.toList(),
            randomTimes = randomTimes,
            randomCount = randomCount
        )
        val json = gson.toJson(state)
        context.scheduleStore.edit { prefs ->
            prefs[SCHEDULE_STATE_KEY] = json
        }
    }
}
