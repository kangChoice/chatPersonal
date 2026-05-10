package com.needai.chat.data.remote.client

import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.usecase.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ModelClient {
    fun streamChat(
        messages: List<ChatMessage>,
        config: ModelConfig,
        skill: Skill
    ): Flow<StreamEvent>

    suspend fun validateConfig(config: ModelConfig): Result<Boolean>
}
