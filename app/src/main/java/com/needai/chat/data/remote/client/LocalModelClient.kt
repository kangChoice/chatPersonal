package com.needai.chat.data.remote.client

import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.usecase.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地模型客户端 — 预留实现
 * 当前版本不实现，返回提示信息
 */
@Singleton
class LocalModelClient @Inject constructor() : ModelClient {

    override fun streamChat(
        messages: List<ChatMessage>,
        config: ModelConfig,
        skill: Skill
    ): Flow<StreamEvent> = flowOf(StreamEvent.Token("[提示] 本地模型功能开发中，敬请期待。"))

    override suspend fun validateConfig(config: ModelConfig): Result<Boolean> {
        return Result.failure(Exception("本地模型功能尚未实现"))
    }
}
