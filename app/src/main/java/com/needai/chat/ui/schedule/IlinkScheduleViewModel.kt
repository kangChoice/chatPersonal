package com.needai.chat.ui.schedule

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.app.ClawBotScheduleScheduler
import com.needai.chat.data.ilink.FixedScheduleItem
import com.needai.chat.data.ilink.IlinkBridgeService
import com.needai.chat.data.ilink.IlinkScheduleManager
import com.needai.chat.data.ilink.ScheduleConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IlinkScheduleViewModel @Inject constructor(
    private val scheduleManager: IlinkScheduleManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _config = MutableStateFlow(ScheduleConfig())
    val config: StateFlow<ScheduleConfig> = _config.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleManager.initialize()
            _config.value = scheduleManager.getConfig()
        }
    }

    private fun refreshConfig() {
        _config.value = scheduleManager.getConfig()
    }

    fun updateFixedMessage(index: Int, item: FixedScheduleItem) {
        viewModelScope.launch {
            val messages = _config.value.fixedMessages.toMutableList()
            if (index in messages.indices) {
                messages[index] = item
                scheduleManager.setFixedMessages(messages)
                refreshConfig()
                ClawBotScheduleScheduler.reschedule(context)
            }
        }
    }

    fun addFixedMessage(item: FixedScheduleItem) {
        viewModelScope.launch {
            scheduleManager.setFixedMessages(_config.value.fixedMessages + item)
            refreshConfig()
            ClawBotScheduleScheduler.reschedule(context)
        }
    }

    fun deleteFixedMessage(index: Int) {
        viewModelScope.launch {
            val messages = _config.value.fixedMessages.toMutableList()
            if (index in messages.indices) {
                messages.removeAt(index)
                scheduleManager.setFixedMessages(messages)
                refreshConfig()
                ClawBotScheduleScheduler.reschedule(context)
            }
        }
    }

    fun setRandomMessage(text: String) {
        viewModelScope.launch {
            scheduleManager.setRandomMessage(text)
            refreshConfig()
            ClawBotScheduleScheduler.reschedule(context)
        }
    }

    fun setRandomTimeRange(startTime: String, endTime: String) {
        viewModelScope.launch {
            scheduleManager.setRandomTimeRange(startTime, endTime)
            refreshConfig()
            ClawBotScheduleScheduler.reschedule(context)
        }
    }

    fun setRandomCount(count: Int) {
        viewModelScope.launch {
            scheduleManager.setRandomCount(count)
            refreshConfig()
            ClawBotScheduleScheduler.reschedule(context)
        }
    }

    fun setRandomEnabled(enabled: Boolean) {
        viewModelScope.launch {
            scheduleManager.setRandomEnabled(enabled)
            refreshConfig()
            ClawBotScheduleScheduler.reschedule(context)
        }
    }

    fun testSendSchedule() {
        val intent = Intent(context, IlinkBridgeService::class.java).apply {
            action = IlinkBridgeService.ACTION_TEST_SCHEDULE
        }
        ContextCompat.startForegroundService(context, intent)
    }
}
