package com.needai.chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.MessageRole
import com.needai.chat.domain.model.ModelType
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.repository.ChatRepository
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SessionRepository
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.ui.chat.state.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val skillRepository: SkillRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    private val sessionIdFlow = MutableStateFlow("")

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load session
            val sessionId = chatRepository.getCurrentSessionId()
            sessionIdFlow.value = sessionId
            _uiState.update { it.copy(sessionId = sessionId) }
        }

        viewModelScope.launch {
            // Reactively observe messages for current session
            sessionIdFlow.flatMapLatest { sid ->
                chatRepository.getMessages(sid)
                    .catch { e -> _uiState.update { it.copy(error = "加载消息失败: ${e.localizedMessage}") } }
            }.collect { messages ->
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
            // Load history sessions
            sessionRepository.getAllSessions().collect { sessions ->
                _uiState.update { it.copy(historySessions = sessions) }
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
                val isConfigured = config.remoteBaseUrl.isNotBlank()
                        && config.remoteApiKey.isNotBlank()
                        && config.remoteModelName.isNotBlank()
                _uiState.update {
                    it.copy(
                        currentModel = config.modelType,
                        currentModelName = config.remoteModelName,
                        isModelConfigured = isConfigured
                    )
                }
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
            val activeConfig = modelConfigRepository.getModelConfig().first()
            val assistantMsg = Message(
                sessionId = sessionId,
                role = MessageRole.ASSISTANT,
                content = "",
                skillId = currentSkill.id,
                timestamp = System.currentTimeMillis() + 1,
                isStreaming = true,
                modelConfigId = activeConfig.id.ifEmpty { null }
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
                        modelClient.streamChat(chatMessages, config, skill).collect { event ->
                            when (event) {
                                is StreamEvent.Token -> {
                                    fullContent.append(event.text)
                                    _uiState.update { it.copy(currentStreamingMessage = fullContent.toString()) }
                                }
                                is StreamEvent.Done -> {
                                    if (event.totalTokens != null || event.promptTokens != null) {
                                        chatRepository.updateMessageTokenUsage(
                                            assistantId, event.promptTokens, event.completionTokens, event.totalTokens
                                        )
                                    }
                                }
                            }
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
            saveCurrentSession()
            skillRepository.setSelectedSkillId(skill.id)
            val newId = chatRepository.createNewSession()
            sessionIdFlow.value = newId
            _uiState.update {
                it.copy(
                    currentSkill = skill,
                    sessionId = newId,
                    messages = emptyList(),
                    currentStreamingMessage = "",
                    isStreaming = false
                )
            }
        }
    }

    fun switchToHistorySession(session: ChatSession) {
        viewModelScope.launch {
            saveCurrentSession()

            // Load the session's skill
            val skill = skillRepository.getSkillById(session.skillId)
            if (skill != null) {
                skillRepository.setSelectedSkillId(skill.id)
            }

            // Switch session ID so the message flow picks it up
            sessionIdFlow.value = session.id
            _uiState.update {
                it.copy(
                    sessionId = session.id,
                    currentSkill = skill ?: it.currentSkill,
                    currentStreamingMessage = "",
                    isStreaming = false
                )
            }
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
            saveCurrentSession()
            val newId = chatRepository.createNewSession()
            sessionIdFlow.value = newId
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

    private suspend fun saveCurrentSession() {
        val state = _uiState.value
        val messages = state.messages
        if (messages.isEmpty()) return

        val firstUserMessage = messages.firstOrNull { it.role == MessageRole.USER }
        val title = firstUserMessage?.content?.take(50)?.replace("\n", " ") ?: "空对话"
        val timestamps = messages.map { it.timestamp }
        val skillId = state.currentSkill.id

        if (skillId.isBlank()) return

        sessionRepository.saveSession(
            ChatSession(
                id = state.sessionId,
                skillId = skillId,
                skillName = state.currentSkill.name,
                skillAvatar = state.currentSkill.avatar,
                title = title,
                messageCount = messages.size,
                createdAt = timestamps.minOrNull() ?: System.currentTimeMillis(),
                updatedAt = timestamps.maxOrNull() ?: System.currentTimeMillis()
            )
        )
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun exportCurrentSessionToFile(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            val state = _uiState.value
            val md = com.needai.chat.data.export.ExportUtils.generateSessionMarkdown(
                title = "当前会话",
                skillName = state.currentSkill.name,
                skillAvatar = state.currentSkill.avatar,
                messages = state.messages,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val success = com.needai.chat.data.export.ExportUtils.writeToUri(context, uri, md)
            _uiState.update {
                it.copy(error = if (success) "会话已导出" else "导出失败")
            }
        }
    }

    fun exportSessionToFile(context: android.content.Context, sessionId: String, uri: android.net.Uri) {
        viewModelScope.launch {
            val messages = chatRepository.getMessages(sessionId).first()
            if (messages.isEmpty()) {
                _uiState.update { it.copy(error = "该会话没有消息可导出") }
                return@launch
            }
            val session = sessionRepository.getSessionById(sessionId)
            val md = com.needai.chat.data.export.ExportUtils.generateSessionMarkdown(
                title = session?.title ?: "历史会话",
                skillName = session?.skillName ?: "",
                skillAvatar = session?.skillAvatar ?: "",
                messages = messages,
                createdAt = session?.createdAt ?: System.currentTimeMillis(),
                updatedAt = session?.updatedAt ?: System.currentTimeMillis()
            )
            val success = com.needai.chat.data.export.ExportUtils.writeToUri(context, uri, md)
            _uiState.update {
                it.copy(error = if (success) "会话已导出" else "导出失败")
            }
        }
    }
}
