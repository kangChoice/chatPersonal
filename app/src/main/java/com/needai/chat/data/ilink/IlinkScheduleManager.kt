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

@Singleton
class IlinkScheduleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "IlinkSchedule"
        private val SCHEDULE_STATE_KEY = stringPreferencesKey("schedule_state")
        private const val RANDOM_MSG = "在干嘛?要好好生活哦"
        private const val RANDOM_START_HOUR = 8
        private const val RANDOM_END_HOUR = 18
        private const val DEFAULT_RANDOM_COUNT = 3
        private val FMT = DateTimeFormatter.ofPattern("HH:mm")

        private val FIXED_SCHEDULE = listOf(
            "08:00" to "起床了吗？要好好吃早饭噢",
            "12:00" to "中午要好好吃饭哦！我会担心的",
            "21:00" to "晚安！好梦"
        )
    }

    // 内存状态，避免每次检查都读 DataStore
    private var todayDate: String = ""
    private var sentFixed: MutableSet<String> = mutableSetOf()
    private var sentRandom: MutableSet<String> = mutableSetOf()
    private var randomTimes: List<String> = emptyList()
    private var randomCount: Int = DEFAULT_RANDOM_COUNT
    private var initialized = false

    /**
     * 加载持久化状态，必须在 checkAndSend 之前调用
     */
    suspend fun initialize() {
        if (initialized) return
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
        for ((time, text) in FIXED_SCHEDULE) {
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

        // 随机时间点
        for (time in randomTimes) {
            if (time !in sentRandom) {
                val scheduled = LocalTime.parse(time, FMT)
                if (!now.isBefore(scheduled)) {
                    val inWindow = !now.isAfter(scheduled.plusMinutes(5))
                    if (inWindow) {
                        FileLogger.i(TAG, "checkAndSend: 随机消息触发 $time → $RANDOM_MSG")
                        sendToAllUsers(RANDOM_MSG, botToken, syncBuf, contextTokenCache, ilinkClient)
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

    /** 获取今日所有定时消息预览（时间, 消息文本），含时间前缀 */
    fun getTodaySchedulePreview(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        for ((time, text) in FIXED_SCHEDULE) {
            result.add(time to text)
        }
        for (time in randomTimes) {
            result.add(time to RANDOM_MSG)
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
        val clamped = count.coerceIn(0, 60)
        if (clamped == randomCount) return
        randomCount = clamped
        randomTimes = generateRandomTimes(RANDOM_START_HOUR, sentRandom)
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
        randomTimes = generateRandomTimes(now.hour, emptySet())
    }

    private fun generateRandomTimes(startHour: Int, exclude: Set<String>): List<String> {
        val count = min(randomCount, (RANDOM_END_HOUR - startHour) * 60)
        if (count <= 0) return emptyList()

        val totalMinutes = (RANDOM_END_HOUR - startHour) * 60
        val segmentSize = totalMinutes / count
        val startMinute = startHour * 60

        val result = mutableListOf<Int>()
        for (i in 0 until count) {
            val segStart = startMinute + i * segmentSize
            val segEnd = segStart + segmentSize - 1
            val minutesInSeg = segEnd - segStart + 1
            if (minutesInSeg <= 0) continue
            result.add(segStart + (0 until minutesInSeg).random())
        }

        return result.sorted()
            .map { totalMin ->
                val h = totalMin / 60
                val m = totalMin % 60
                String.format("%02d:%02d", h, m)
            }
            .filter { it !in exclude }
    }

    // ===== DataStore 持久化 =====

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
            randomCount = state.randomCount.coerceIn(1, 60)
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
