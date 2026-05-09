package com.needai.chat.domain.usecase

import com.needai.chat.domain.model.Message
import com.needai.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatHistoryUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(sessionId: String): Flow<List<Message>> =
        repository.getMessages(sessionId)
}
