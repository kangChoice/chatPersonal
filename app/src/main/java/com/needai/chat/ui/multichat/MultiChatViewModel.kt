package com.needai.chat.ui.multichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.data.remote.client.ModelClient
import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.MessageRole
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.repository.ChatRepository
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SessionRepository
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.domain.usecase.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MultiChatViewModel @Inject constructor(
    private val skillRepository: SkillRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val sessionRepository: SessionRepository,
    private val chatRepository: ChatRepository,
    private val modelClient: ModelClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(MultiChatUiState())
    val uiState: StateFlow<MultiChatUiState> = _uiState.asStateFlow()

    private var currentGenerationJob: kotlinx.coroutines.Job? = null

    init {
        loadSkills()
        loadHistorySessions()
        newSession()
    }

    private fun loadSkills() {
        viewModelScope.launch {
            skillRepository.getAllSkills().collect { skills ->
                _uiState.update { it.copy(availableSkills = skills) }
            }
        }
    }

    private fun loadHistorySessions() {
        viewModelScope.launch {
            sessionRepository.getSessionsByType("multi").collect { sessions ->
                _uiState.update { it.copy(historySessions = sessions) }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun toggleSkillSelector() {
        _uiState.update { it.copy(showSkillSelector = !it.showSkillSelector) }
    }

    fun toggleSkillSelection(skill: Skill) {
        _uiState.update { state ->
            val current = state.selectedSkills.toMutableList()
            val exists = current.any { it.id == skill.id }
            if (exists) {
                current.removeAll { it.id == skill.id }
            } else {
                current.add(skill)
            }
            state.copy(selectedSkills = current)
        }
    }

    fun togglePromptEditor() {
        _uiState.update { it.copy(showPromptEditor = !it.showPromptEditor) }
    }

    fun onMultiPromptChanged(prompt: String) {
        _uiState.update { it.copy(multiPrompt = prompt) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun newSession() {
        viewModelScope.launch {
            saveCurrentSession()
            _uiState.update {
                it.copy(
                    sessionId = java.util.UUID.randomUUID().toString(),
                    messages = emptyList(),
                    selectedSkills = emptyList(),
                    isGenerating = false,
                    currentRespondingSkill = null,
                    currentStreamingContent = "",
                    inputText = "",
                    showSkillSelector = true
                )
            }
        }
    }

    fun switchToSession(session: ChatSession) {
        viewModelScope.launch {
            saveCurrentSession()
            _uiState.update {
                it.copy(
                    sessionId = session.id,
                    messages = emptyList(),
                    isGenerating = false,
                    currentRespondingSkill = null,
                    currentStreamingContent = "",
                    selectedSkills = emptyList()
                )
            }
            // Load messages
            chatRepository.getMessages(session.id).first().forEach { msg ->
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + MultiChatMessage(
                            id = java.util.UUID.randomUUID().toString(),
                            role = msg.role,
                            content = msg.content,
                            skillId = msg.skillId,
                            skillName = if (msg.skillId != null) {
                                state.availableSkills.find { it.id == msg.skillId }?.name
                            } else null,
                            skillAvatar = if (msg.skillId != null) {
                                state.availableSkills.find { it.id == msg.skillId }?.avatar
                            } else null,
                            isStreaming = false,
                            dbId = msg.id,
                            timestamp = msg.timestamp
                        )
                    )
                }
            }
            // Restore selected skills from session
            if (session.skillIds.isNotEmpty()) {
                _uiState.update { state ->
                    val skills = state.availableSkills.filter { it.id in session.skillIds }
                    state.copy(selectedSkills = skills)
                }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isGenerating) return
        if (_uiState.value.selectedSkills.size < 2) {
            _uiState.update { it.copy(error = "请至少选择 2 个技能") }
            return
        }

        _uiState.update { it.copy(inputText = "", error = null) }

        currentGenerationJob = viewModelScope.launch {
            val config = modelConfigRepository.getModelConfig().first()
            val multiPrompt = _uiState.value.multiPrompt
            val selectedSkills = _uiState.value.selectedSkills.toList().shuffled()

            // Add user message
            val userMsgId = java.util.UUID.randomUUID().toString()
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + MultiChatMessage(
                        id = userMsgId,
                        role = MessageRole.USER,
                        content = text
                    ),
                    isGenerating = true
                )
            }

            // Save user message to DB
            val sessionId = _uiState.value.sessionId
            val userDbId = chatRepository.insertMessage(
                Message(sessionId = sessionId, role = MessageRole.USER, content = text, timestamp = System.currentTimeMillis())
            )
            _uiState.update { state ->
                val msgs = state.messages.toMutableList()
                val idx = msgs.indexOfLast { it.id == userMsgId }
                if (idx >= 0) msgs[idx] = msgs[idx].copy(dbId = userDbId)
                state.copy(messages = msgs)
            }

            val allReplies = mutableListOf<Pair<Skill, String>>()

            for (skill in selectedSkills) {
                if (!isActive) break

                _uiState.update { it.copy(currentRespondingSkill = skill, currentStreamingContent = "") }

                // 按群聊策略文档: Skills-1 直接回答, Skills-X (X>=2) 需评鉴所有前置+回答用户
                val isFirstResponder = allReplies.isEmpty()
                val contextInstruction = buildString {
                    appendLine("你当前扮演的角色是「${skill.name}」，正在参与一场群聊。")
                    if (!isFirstResponder) {
                        appendLine("其他角色的发言已用【角色名】标注。")
                        appendLine()
                        appendLine("【回复要求】")
                        appendLine("1. 根据群聊背景提示词设定的互动氛围，对所有前置角色的发言做出评价")
                        appendLine("2. 评价完毕后，回归「${skill.name}」自身的角色设定，回答用户的问题")
                        appendLine()
                        appendLine("【输出格式】")
                        appendLine("【${skill.name}】回复：对前置角色们的评价 + 作为「${skill.name}」对用户问题的回答")
                    } else {
                        appendLine("作为第一个回复的角色，请直接以「${skill.name}」的身份回答用户的问题。")
                    }
                }
                val chatMessages = mutableListOf(
                    ChatMessage("system", multiPrompt),
                    ChatMessage("system", skill.systemPrompt),
                    ChatMessage("system", contextInstruction),
                    ChatMessage("user", text)
                )
                allReplies.forEach { (prevSkill, reply) ->
                    chatMessages.add(ChatMessage("assistant", "【${prevSkill.name}】$reply"))
                }

                // Add placeholder message for this skill
                val msgId = java.util.UUID.randomUUID().toString()
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + MultiChatMessage(
                            id = msgId,
                            role = MessageRole.ASSISTANT,
                            skillId = skill.id,
                            skillName = skill.name,
                            skillAvatar = skill.avatar,
                            isStreaming = true
                        )
                    )
                }

                try {
                    val fullContent = StringBuilder()
                    modelClient.streamChat(chatMessages, config, skill).collect { event ->
                        when (event) {
                            is StreamEvent.Token -> {
                                fullContent.append(event.text)
                                _uiState.update { state ->
                                    val msgs = state.messages.toMutableList()
                                    val idx = msgs.indexOfLast { it.id == msgId }
                                    if (idx >= 0) {
                                        msgs[idx] = msgs[idx].copy(content = fullContent.toString())
                                    }
                                    state.copy(
                                        messages = msgs,
                                        currentStreamingContent = fullContent.toString()
                                    )
                                }
                            }
                            is StreamEvent.Done -> {
                                _uiState.update { state ->
                                    val msgs = state.messages.toMutableList()
                                    val idx = msgs.indexOfLast { it.id == msgId }
                                    if (idx >= 0) {
                                        msgs[idx] = msgs[idx].copy(
                                            content = fullContent.toString(),
                                            isStreaming = false
                                        )
                                    }
                                    state.copy(messages = msgs)
                                }
                                allReplies.add(skill to fullContent.toString())
                                // Save this assistant message to DB
                                val dbId = chatRepository.insertMessage(
                                    Message(
                                        sessionId = sessionId,
                                        role = MessageRole.ASSISTANT,
                                        content = fullContent.toString(),
                                        skillId = skill.id,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                                _uiState.update { state ->
                                    val msgs = state.messages.toMutableList()
                                    val idx = msgs.indexOfLast { it.id == msgId }
                                    if (idx >= 0) msgs[idx] = msgs[idx].copy(dbId = dbId)
                                    state.copy(messages = msgs)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    val errorMsg = e.localizedMessage ?: "未知错误"
                    _uiState.update { state ->
                        val msgs = state.messages.toMutableList()
                        val idx = msgs.indexOfLast { it.id == msgId }
                        if (idx >= 0) {
                            msgs[idx] = msgs[idx].copy(
                                content = "[失败] $errorMsg",
                                isStreaming = false
                            )
                        }
                        state.copy(messages = msgs)
                    }
                    allReplies.add(skill to "[失败] $errorMsg")
                }
            }

            // All skills done — save session
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    currentRespondingSkill = null,
                    currentStreamingContent = ""
                )
            }
            saveCurrentSession()
        }
    }

    fun stopGeneration() {
        currentGenerationJob?.cancel()
        _uiState.update { state ->
            val msgs = state.messages.map { msg ->
                if (msg.isStreaming) msg.copy(isStreaming = false) else msg
            }
            state.copy(
                messages = msgs,
                isGenerating = false,
                currentRespondingSkill = null,
                currentStreamingContent = ""
            )
        }
    }

    fun clearMessages() {
        viewModelScope.launch {
            val sessionId = _uiState.value.sessionId
            chatRepository.clearSession(sessionId)
            _uiState.update {
                it.copy(messages = emptyList())
            }
        }
    }

    private suspend fun saveCurrentSession() {
        val state = _uiState.value
        if (state.messages.isEmpty()) return
        if (state.selectedSkills.size < 2 && state.messages.size <= 1) return

        val firstUserMsg = state.messages.firstOrNull { it.role == MessageRole.USER }
        val title = firstUserMsg?.content?.take(50)?.replace("\n", " ") ?: "群聊会话"
        val timestamps = state.messages.map { it.timestamp }

        sessionRepository.saveSession(
            ChatSession(
                id = state.sessionId,
                skillId = state.selectedSkills.firstOrNull()?.id ?: "",
                type = "multi",
                skillIds = state.selectedSkills.map { it.id },
                skillName = "群聊",
                skillAvatar = "💬",
                title = title,
                messageCount = state.messages.size,
                createdAt = timestamps.minOrNull() ?: System.currentTimeMillis(),
                updatedAt = timestamps.maxOrNull() ?: System.currentTimeMillis()
            )
        )
    }
}
