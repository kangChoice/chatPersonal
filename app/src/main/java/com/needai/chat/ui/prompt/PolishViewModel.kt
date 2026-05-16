package com.needai.chat.ui.prompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PolishUiState(
    val inputText: String = "",
    val polishedPrompt: String = "",
    val isPolishing: Boolean = false,
    val charCount: Int = 0,
    val error: String? = null,
    val currentModelName: String = "",
    // Voice prompt polish
    val voiceInputText: String = "",
    val voicePolishedPrompt: String = "",
    val voiceIsPolishing: Boolean = false,
    val voiceCharCount: Int = 0,
    val voiceError: String? = null
)

@HiltViewModel
class PolishViewModel @Inject constructor(
    private val modelConfigRepository: ModelConfigRepository,
    private val skillRepository: SkillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PolishUiState())
    val uiState: StateFlow<PolishUiState> = _uiState.asStateFlow()

    private var polishingJob: Job? = null
    private var voicePolishingJob: Job? = null

    init {
        viewModelScope.launch {
            modelConfigRepository.getModelConfig().collect { config ->
                _uiState.update { it.copy(currentModelName = config.remoteModelName) }
            }
        }
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun polishPrompt() {
        val input = _uiState.value.inputText.trim()
        if (input.isEmpty()) return

        _uiState.update { it.copy(isPolishing = true, polishedPrompt = "", charCount = 0, error = null) }

        polishingJob = viewModelScope.launch {
            val config = modelConfigRepository.getModelConfig().first()
            if (config.remoteBaseUrl.isBlank() || config.remoteApiKey.isBlank()) {
                _uiState.update {
                    it.copy(isPolishing = false, error = "请先在设置中配置模型")
                }
                return@launch
            }

            val systemPrompt = buildString {
                append("你是一个专业的AI角色设定提示词工程师。你的任务是将用户简短的角色描述扩展为详细、生动的系统提示词（System Prompt），用于AI角色扮演。\n\n")
                append("要求：\n")
                append("1. 使用Markdown格式输出\n")
                append("2. 字数控制在500-1200字之间\n")
                append("3. 详细描述角色的性格特点、说话方式、语气、背景故事、行为模式\n")
                append("4. 使用第二人称\"你\"来描述角色（例如\"你是一个...\"），因为这是给AI看的系统提示词\n")
                append("5. 角色形象要鲜明、有沉浸感\n")
                append("6. 只输出提示词本身，不要添加任何解释性文字\n")
                append("7. 如果用户描述过于简单，可以发挥创意进行合理扩充\n\n")
                append("请优化用户提供的以下描述：")
            }

            try {
                val modelClient = com.needai.chat.data.remote.client.RemoteModelClient(
                    com.google.gson.Gson()
                )
                val messages = listOf(
                    com.needai.chat.domain.usecase.ChatMessage(role = "system", content = systemPrompt),
                    com.needai.chat.domain.usecase.ChatMessage(role = "user", content = input)
                )
                val defaultSkill = Skill(
                    id = "prompt_polish",
                    name = "提示词润色",
                    description = "",
                    avatar = "✍️",
                    systemPrompt = systemPrompt,
                    greeting = "",
                    isBuiltin = true
                )

                val fullContent = StringBuilder()
                modelClient.streamChat(messages, config, defaultSkill).collect { event ->
                    when (event) {
                        is StreamEvent.Token -> {
                            fullContent.append(event.text)
                            _uiState.update {
                                it.copy(
                                    polishedPrompt = fullContent.toString(),
                                    charCount = fullContent.length
                                )
                            }
                        }
                        is StreamEvent.Done -> {
                            val finalContent = fullContent.toString()
                            if (finalContent.length < 500) {
                                _uiState.update {
                                    it.copy(
                                        isPolishing = false,
                                        error = "生成内容不足500字（当前${finalContent.length}字），请尝试更详细的描述后重新生成"
                                    )
                                }
                            } else {
                                _uiState.update {
                                    it.copy(isPolishing = false)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isPolishing = false, error = "生成失败: ${e.localizedMessage ?: "未知错误"}")
                }
            }
        }
    }

    fun stopPolishing() {
        polishingJob?.cancel()
        polishingJob = null
        _uiState.update { it.copy(isPolishing = false) }
    }

    // ===== Voice Prompt Polish =====

    fun setVoiceInputText(text: String) {
        _uiState.update { it.copy(voiceInputText = text, voiceError = null) }
    }

    fun polishVoicePrompt() {
        val input = _uiState.value.voiceInputText.trim()
        if (input.isEmpty()) return

        _uiState.update { it.copy(voiceIsPolishing = true, voicePolishedPrompt = "", voiceCharCount = 0, voiceError = null) }

        voicePolishingJob = viewModelScope.launch {
            val config = modelConfigRepository.getModelConfig().first()
            if (config.remoteBaseUrl.isBlank() || config.remoteApiKey.isBlank()) {
                _uiState.update {
                    it.copy(voiceIsPolishing = false, voiceError = "请先在设置中配置模型")
                }
                return@launch
            }

            val systemPrompt = buildString {
                append("你是一个专业的语音设计提示词工程师。你的任务是将用户简短的声音描述优化为高质量的语音提示词（Voice Prompt），用于AI语音合成（TTS）中的音色创建。\n\n")
                append("要求：\n")
                append("1. 输出内容控制在 50-100 字之间\n")
                append("2. 详细描述声音的性别、年龄段、音色特点（如磁性、温柔、清亮、低沉等）\n")
                append("3. 描述说话风格（如沉稳、活泼、知性、亲切等）\n")
                append("4. 描述适用的场景（如新闻播报、故事朗读、日常对话、客服等）\n")
                append("5. 使用中文描述\n")
                append("6. 只输出优化后的声音描述本身，不要添加任何解释性文字、不要加引号\n\n")
                append("请优化用户提供的以下声音描述：")
            }

            try {
                val modelClient = com.needai.chat.data.remote.client.RemoteModelClient(
                    com.google.gson.Gson()
                )
                val messages = listOf(
                    com.needai.chat.domain.usecase.ChatMessage(role = "system", content = systemPrompt),
                    com.needai.chat.domain.usecase.ChatMessage(role = "user", content = input)
                )
                val defaultSkill = Skill(
                    id = "voice_polish",
                    name = "音色优化",
                    description = "",
                    avatar = "🎙️",
                    systemPrompt = systemPrompt,
                    greeting = "",
                    isBuiltin = true
                )

                val fullContent = StringBuilder()
                modelClient.streamChat(messages, config, defaultSkill).collect { event ->
                    when (event) {
                        is StreamEvent.Token -> {
                            fullContent.append(event.text)
                            _uiState.update {
                                it.copy(
                                    voicePolishedPrompt = fullContent.toString(),
                                    voiceCharCount = fullContent.length
                                )
                            }
                        }
                        is StreamEvent.Done -> {
                            _uiState.update {
                                it.copy(voiceIsPolishing = false)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(voiceIsPolishing = false, voiceError = "生成失败: ${e.localizedMessage ?: "未知错误"}")
                }
            }
        }
    }

    fun stopVoicePolishing() {
        voicePolishingJob?.cancel()
        voicePolishingJob = null
        _uiState.update { it.copy(voiceIsPolishing = false) }
    }

    fun createSkill(name: String, description: String, systemPrompt: String, avatar: String, greeting: String, temperature: Double, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val skill = Skill(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                avatar = avatar,
                systemPrompt = systemPrompt,
                greeting = greeting,
                temperature = temperature,
                tags = listOf("custom"),
                isBuiltin = false
            )
            skillRepository.insertSkill(skill)
            onResult?.invoke(true, "角色「${skill.name}」已创建")
        }
    }

    fun reset() {
        voicePolishingJob?.cancel()
        voicePolishingJob = null
        polishingJob?.cancel()
        polishingJob = null
        _uiState.update { PolishUiState() }
    }
}
