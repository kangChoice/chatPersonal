package com.needai.chat.ui.schedule

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.app.AiNotificationScheduler
import com.needai.chat.app.ScheduleNotificationService
import com.needai.chat.data.local.datastore.AiNotificationManager
import com.needai.chat.domain.model.AiNotificationConfig
import com.needai.chat.domain.model.NotificationTemplate
import com.needai.chat.domain.repository.NotificationTemplateRepository
import com.needai.chat.domain.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiNotificationViewModel @Inject constructor(
    private val aiNotificationManager: AiNotificationManager,
    private val skillRepository: SkillRepository,
    private val notificationTemplateRepository: NotificationTemplateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _configs = MutableStateFlow<List<AiNotificationConfig>>(emptyList())
    val configs: StateFlow<List<AiNotificationConfig>> = _configs.asStateFlow()

    private val _globalEnabled = MutableStateFlow(true)
    val globalEnabled: StateFlow<Boolean> = _globalEnabled.asStateFlow()

    private val _availableSkills = MutableStateFlow(emptyList<com.needai.chat.domain.model.Skill>())
    val availableSkills: StateFlow<List<com.needai.chat.domain.model.Skill>> = _availableSkills.asStateFlow()

    private val _templates = MutableStateFlow<List<NotificationTemplate>>(emptyList())
    val templates: StateFlow<List<NotificationTemplate>> = _templates.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            aiNotificationManager.configs.collect { list ->
                _configs.value = list.sortedBy { "${it.hour}:${it.minute}" }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            aiNotificationManager.globalEnabled.collect { enabled ->
                _globalEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            skillRepository.getAllSkills().collect { skills ->
                _availableSkills.value = skills
            }
        }
        viewModelScope.launch {
            notificationTemplateRepository.getAllTemplates().collect { templates ->
                _templates.value = templates
            }
        }
    }

    fun add(config: AiNotificationConfig) {
        viewModelScope.launch {
            aiNotificationManager.add(config)
            AiNotificationScheduler.reschedule(context)
        }
    }

    fun update(config: AiNotificationConfig) {
        viewModelScope.launch {
            aiNotificationManager.update(config)
            AiNotificationScheduler.reschedule(context)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            aiNotificationManager.delete(id)
            AiNotificationScheduler.reschedule(context)
        }
    }

    fun toggleEnabled(config: AiNotificationConfig) {
        viewModelScope.launch {
            aiNotificationManager.update(config.copy(enabled = !config.enabled))
            AiNotificationScheduler.reschedule(context)
        }
    }

    fun setGlobalEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiNotificationManager.setGlobalEnabled(enabled)
            AiNotificationScheduler.reschedule(context)
        }
    }

    fun testTrigger() {
        val intent = Intent(context, ScheduleNotificationService::class.java).apply {
            putExtra(ScheduleNotificationService.EXTRA_FORCE_ALL, true)
        }
        context.startService(intent)
    }
}
