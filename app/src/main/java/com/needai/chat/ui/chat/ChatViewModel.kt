package com.needai.chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.MessageRole
import com.needai.chat.domain.model.ModelType
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.repository.ChatRepository
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.ui.chat.state.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val skillRepository: SkillRepository,
    private val modelConfigRepository: ModelConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load session
            val sessionId = chatRepository.getCurrentSessionId()
            _uiState.update { it.copy(sessionId = sessionId) }

            // Load messages
            chatRepository.getMessages(sessionId)
                .catch { e -> _uiState.update { it.copy(error = "加载消息失败: ${e.localizedMessage}") } }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
        }

        viewModelScope.launch {
            // Load skills
            skillRepository.getAllSkills().collect { skills ->
                _uiState.update { it.copy(availableSkills = skills) }
            }
        }

        viewModelScope.launch {
            // Load selected skill
            val skillId = skillRepository.getSelectedSkillId()
            val skill = skillRepository.getSkillById(skillId)
            if (skill != null) {
                _uiState.update { it.copy(currentSkill = skill) }
            }
        }

        viewModelScope.launch {
            // Load model config for model type
            modelConfigRepository.getModelConfig().collect { config ->
                _uiState.update { it.copy(currentModel = config.modelType) }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isStreaming) return

        _uiState.update { it.copy(inputText = "", error = null) }

        viewModelScope.launch {
            val sessionId = _uiState.value.sessionId
            val currentSkill = _uiState.value.currentSkill
            val timestamp = System.currentTimeMillis()

            // Save user message
            val userMessage = Message(
                sessionId = sessionId,
                role = MessageRole.USER,
                content = text,
                timestamp = timestamp
            )
            chatRepository.insertMessage(userMessage)

            // Start streaming
            _uiState.update { it.copy(isStreaming = true, currentStreamingMessage = "") }

            // Create placeholder assistant message
            val assistantMsg = Message(
                sessionId = sessionId,
                role = MessageRole.ASSISTANT,
                content = "",
                skillId = currentSkill.id,
                timestamp = System.currentTimeMillis() + 1,
                isStreaming = true
            )
            val assistantId = chatRepository.insertMessage(assistantMsg)

            val modelClient = com.needai.chat.data.remote.client.RemoteModelClient(
                com.google.gson.Gson()
            )

            modelConfigRepository.getModelConfig().first().let { config ->
                val skill = _uiState.value.currentSkill

                streamingJob = viewModelScope.launch {
                    try {
                        // Build messages
                        val chatMessages = mutableListOf(
                            com.needai.chat.domain.usecase.ChatMessage(
                                role = "system",
                                content = skill.systemPrompt
                            )
                        )

                        val currentMessages = _uiState.value.messages.filter {
                            it.role != MessageRole.SYSTEM
                        }
                        currentMessages.forEach { msg ->
                            val role = when (msg.role) {
                                MessageRole.USER -> "user"
                                MessageRole.ASSISTANT -> "assistant"
                                MessageRole.SYSTEM -> "system"
                            }
                            chatMessages.add(
                                com.needai.chat.domain.usecase.ChatMessage(
                                    role = role,
                                    content = msg.content
                                )
                            )
                        }

                        chatMessages.add(
                            com.needai.chat.domain.usecase.ChatMessage(
                                role = "user",
                                content = text
                            )
                        )

                        val fullContent = StringBuilder()
                        modelClient.streamChat(chatMessages, config, skill).collect { token ->
                            fullContent.append(token)
                            _uiState.update { it.copy(currentStreamingMessage = fullContent.toString()) }
                        }

                        // Streaming done, save full content
                        chatRepository.updateMessageContent(assistantId, fullContent.toString())
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                currentStreamingMessage = ""
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                error = "发送失败: ${e.localizedMessage ?: "未知错误"}"
                            )
                        }
                        chatRepository.updateMessageContent(assistantId, "[发送失败] ${e.localizedMessage ?: "未知错误"}")
                    }
                }
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _uiState.update { it.copy(isStreaming = false, currentStreamingMessage = "") }
    }

    fun switchSkill(skill: Skill) {
        viewModelScope.launch {
            skillRepository.setSelectedSkillId(skill.id)
            _uiState.update { it.copy(currentSkill = skill) }
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            val sessionId = _uiState.value.sessionId
            chatRepository.clearSession(sessionId)
            _uiState.update {
                it.copy(
                    messages = emptyList(),
                    currentStreamingMessage = "",
                    isStreaming = false
                )
            }
        }
    }

    fun newSession() {
        viewModelScope.launch {
            val newId = chatRepository.createNewSession()
            _uiState.update {
                it.copy(
                    sessionId = newId,
                    messages = emptyList(),
                    currentStreamingMessage = "",
                    isStreaming = false
                )
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
