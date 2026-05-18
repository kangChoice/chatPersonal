package com.needai.chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.MessageRole
import com.needai.chat.domain.model.ModelType
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.domain.repository.ChatRepository
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SessionRepository
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.domain.repository.VoiceRepository
import com.needai.chat.ui.chat.state.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val skillRepository: SkillRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val sessionRepository: SessionRepository,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _voiceModelMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val voiceModelMap: StateFlow<Map<String, String>> = _voiceModelMap.asStateFlow()

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
            // Reactively observe selected skill and skill list changes,
            // so editing the current skill (e.g. voiceId) takes effect immediately
            combine(
                skillRepository.selectedSkillIdFlow(),
                skillRepository.getAllSkills()
            ) { selectedId, skills ->
                skills.find { it.id == selectedId }
            }.collect { skill ->
                if (skill != null) {
                    _uiState.update { it.copy(currentSkill = skill) }
                }
            }
        }

        viewModelScope.launch {
            // Load voice model map for TTS model resolution
            val voices = voiceRepository.getVoices()
            _voiceModelMap.value = voices.filter { it.targetModel.isNotBlank() }
                .associate { it.voiceId to it.targetModel }
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

            // 立即持久化会话记录，确保角色-会话关联不被应用退出丢失
            sessionRepository.saveSession(
                ChatSession(
                    id = sessionId,
                    skillId = currentSkill.id,
                    skillName = currentSkill.name,
                    skillAvatar = currentSkill.avatar,
                    title = text.take(50).replace("\n", " "),
                    messageCount = 1,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            )

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

                // ★ 记忆压缩检查（仅当技能开启记忆且消息量足够时）
                var summaryText: String? = null
                var summaryEndMessageId: Long? = null
                if (skill.enableMemory) {
                    val session = sessionRepository.getSessionById(sessionId)
                    val existingSummary = session?.summaryText
                    val existingEndId = session?.summaryEndMessageId

                    val allMessages = _uiState.value.messages
                    val uncompressedMessages = if (existingEndId != null) {
                        allMessages.filter { it.id > existingEndId }
                    } else {
                        allMessages
                    }

                    val estimatedTokens = com.needai.chat.util.ContextCompressor.estimateInputTokens(
                        systemPrompt = skill.systemPrompt,
                        summary = existingSummary,
                        messages = uncompressedMessages,
                        currentInput = text
                    )

                    if (com.needai.chat.util.ContextCompressor.shouldCompress(
                            estimatedTokens, config.contextWindow, uncompressedMessages.size
                        )) {
                        _uiState.update { it.copy(isCompressing = true) }
                        try {
                            val newSummary = com.needai.chat.util.ContextCompressor.compress(
                                modelClient = modelClient,
                                config = config,
                                messages = allMessages,
                                existingSummary = existingSummary
                            )
                            if (newSummary != null) {
                                val splitIndex = (allMessages.size * 0.5).toInt().coerceAtLeast(1)
                                val keepMessages = allMessages.drop(splitIndex)
                                val newEndId = keepMessages.firstOrNull()?.id?.minus(1)
                                    ?: allMessages.lastOrNull()?.id ?: -1L
                                sessionRepository.updateSummary(sessionId, newSummary, newEndId)
                                summaryText = newSummary
                                summaryEndMessageId = newEndId
                            } else {
                                summaryText = existingSummary
                                summaryEndMessageId = existingEndId
                            }
                        } catch (_: Exception) {
                            summaryText = existingSummary
                            summaryEndMessageId = existingEndId
                        }
                        _uiState.update { it.copy(isCompressing = false) }
                    } else {
                        summaryText = existingSummary
                        summaryEndMessageId = existingEndId
                    }
                }

                streamingJob = viewModelScope.launch {
                    try {
                        // Build messages
                        val chatMessages = mutableListOf(
                            com.needai.chat.domain.usecase.ChatMessage(
                                role = "system",
                                content = skill.systemPrompt
                            )
                        )

                        // Insert summary if exists (作为 system 消息，模型视作上下文)
                        if (summaryText != null) {
                            chatMessages.add(
                                com.needai.chat.domain.usecase.ChatMessage(
                                    role = "system",
                                    content = "[对话历史摘要] $summaryText"
                                )
                            )
                        }

                        val currentMessages = _uiState.value.messages.filter {
                            it.role != MessageRole.SYSTEM
                        }.let { msgs ->
                            // 只取摘要之后的消息
                            if (summaryEndMessageId != null) {
                                msgs.filter { it.id > summaryEndMessageId }
                            } else msgs
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
                        var hasTokenUsage = false
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
                                        hasTokenUsage = true
                                    }
                                }
                            }
                        }

                        // Streaming done, save full content
                        val finalContent = fullContent.toString()
                        chatRepository.updateMessageContent(assistantId, finalContent)

                        // Fallback: estimate token count if API didn't provide it
                        if (!hasTokenUsage && finalContent.isNotEmpty()) {
                            val inputLength = chatMessages.sumOf { it.content.length }
                            val estimatedPrompt = (inputLength * 0.4).toInt().coerceAtLeast(1)
                            val estimatedCompletion = (finalContent.length * 0.4).toInt().coerceAtLeast(1)
                            val estimatedTotal = estimatedPrompt + estimatedCompletion
                            chatRepository.updateMessageTokenUsage(
                                assistantId, estimatedPrompt, estimatedCompletion, estimatedTotal
                            )
                        }
                        // 同步更新 uiState.messages 中的内容，避免 Room Flow 异步延迟导致
                        // LaunchedEffect 自动朗读时读到的 content 还是空字符串
                        _uiState.update { state ->
                            val updatedMessages = state.messages.map { msg ->
                                if (msg.id == assistantId) msg.copy(content = finalContent)
                                else msg
                            }
                            state.copy(
                                messages = updatedMessages,
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

            // 优先加载该角色的最近一次会话，保留历史记录
            val existingSessions = sessionRepository.getSessionsBySkillId(skill.id)
            val latestSession = existingSessions.maxByOrNull { it.updatedAt }
            val targetSessionId = if (latestSession != null) {
                latestSession.id
            } else {
                chatRepository.createNewSession()
            }

            sessionIdFlow.value = targetSessionId
            _uiState.update {
                it.copy(
                    currentSkill = skill,
                    sessionId = targetSessionId,
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
            sessionRepository.updateSummary(sessionId, null, null)
            _uiState.update {
                it.copy(
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

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
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

    fun importSession(context: android.content.Context, uri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val json = com.needai.chat.data.import.ImportUtils.readFromUri(context, uri)
            if (json == null) {
                onResult(false, "读取文件失败")
                return@launch
            }
            val result = com.needai.chat.data.import.ImportUtils.parseSessionMarkdown(json)
            if (result.isFailure) {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "解析失败")
                return@launch
            }
            val data = result.getOrThrow()
            if (data.messages.isEmpty()) {
                onResult(false, "没有找到消息")
                return@launch
            }

            saveCurrentSession()

            // Use the current/default skill instead of creating one from import data
            val skills = skillRepository.getAllSkills().first()
            val skill = skills.firstOrNull() ?: _uiState.value.currentSkill

            // Create new session
            val sessionId = java.util.UUID.randomUUID().toString()
            val baseTime = System.currentTimeMillis()
            val session = ChatSession(
                id = sessionId,
                skillId = skill.id,
                skillName = skill.name,
                skillAvatar = skill.avatar,
                title = data.title.ifEmpty { "导入的会话" },
                messageCount = data.messages.size,
                createdAt = baseTime,
                updatedAt = baseTime
            )
            sessionRepository.saveSession(session)

            // Save messages
            data.messages.forEachIndexed { index, msg ->
                val role = when (msg.role) {
                    "User" -> MessageRole.USER
                    "Assistant" -> MessageRole.ASSISTANT
                    "System" -> MessageRole.SYSTEM
                    else -> MessageRole.USER
                }
                chatRepository.insertMessage(
                    Message(
                        sessionId = sessionId,
                        role = role,
                        content = msg.content,
                        skillId = skill.id,
                        timestamp = baseTime + index
                    )
                )
            }

            // Switch to imported session (keep current skill)
            sessionIdFlow.value = sessionId
            _uiState.update {
                it.copy(
                    sessionId = sessionId,
                    currentStreamingMessage = "",
                    isStreaming = false
                )
            }
            onResult(true, "会话已导入")
        }
    }
}
