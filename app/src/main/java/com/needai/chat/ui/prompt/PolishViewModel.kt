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
    val error: String? = null
)

@HiltViewModel
class PolishViewModel @Inject constructor(
    private val modelConfigRepository: ModelConfigRepository,
    private val skillRepository: SkillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PolishUiState())
    val uiState: StateFlow<PolishUiState> = _uiState.asStateFlow()

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun polishPrompt() {
        val input = _uiState.value.inputText.trim()
        if (input.isEmpty()) return

        _uiState.update { it.copy(isPolishing = true, polishedPrompt = "", charCount = 0, error = null) }

        viewModelScope.launch {
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

    fun createSkill(name: String, description: String, systemPrompt: String, avatar: String, greeting: String, temperature: Double) {
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
        }
    }

    fun reset() {
        _uiState.update { PolishUiState() }
    }
}
