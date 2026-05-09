package com.needai.chat.data.remote.api

import com.needai.chat.data.remote.dto.ChatRequest
import com.needai.chat.data.remote.dto.ChatStreamChunk
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface DeepSeekApi {
    @POST
    suspend fun streamChat(
        @Url url: String,
        @Body request: ChatRequest
    ): Response<okhttp3.ResponseBody>
}
