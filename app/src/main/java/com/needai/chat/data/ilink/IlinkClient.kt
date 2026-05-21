package com.needai.chat.data.ilink

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.needai.chat.util.FileLogger
import com.needai.chat.util.HttpLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ===== DTOs =====

data class QrCodeResponse(
    @SerializedName("qrcode") val qrcode: String? = null,                     // 用于轮询状态
    @SerializedName("qrcode_img_content") val url: String? = null,            // 微信确认页 URL
    @SerializedName("ret") val ret: Int = 0                                   // 0=成功
)

data class QrCodeStatus(
    @SerializedName("status") val status: String,   // wait / scanned / confirmed
    @SerializedName("bot_token") val botToken: String? = null  // 确认后返回
)

data class GetUpdatesResponse(
    @SerializedName("ret") val ret: Int = 0,
    @SerializedName("msgs") val messages: List<IlinkMessage> = emptyList(),
    @SerializedName("get_updates_buf") val newCursor: String? = null,
    @SerializedName("sync_buf") val syncBuf: String? = null
)

data class IlinkMessage(
    @SerializedName("message_id") val msgId: String,
    @SerializedName("from_user_id") val fromUserId: String,
    @SerializedName("to_user_id") val toUserId: String,
    @SerializedName("message_type") val messageType: Int = 2,
    @SerializedName("context_token") val contextToken: String = "",
    @SerializedName("item_list") val itemList: List<MessageItem> = emptyList()
) {
    val text: String get() = itemList
        .firstOrNull { it.type == 1 }
        ?.textItem?.text ?: ""
}

data class MessageItem(
    @SerializedName("type") val type: Int,
    @SerializedName("text_item") val textItem: TextItem? = null
)

data class TextItem(
    @SerializedName("text") val text: String
)

data class SendMessageRequest(
    @SerializedName("msg") val msg: OutgoingMessage,
    @SerializedName("base_info") val baseInfo: BaseInfo = BaseInfo(),
    @SerializedName("sync_buf") val syncBuf: String? = null
)

data class OutgoingMessage(
    @SerializedName("from_user_id") val fromUserId: String = "",
    @SerializedName("to_user_id") val toUserId: String,
    @SerializedName("client_id") val clientId: String = "",
    @SerializedName("message_type") val messageType: Int = 2,
    @SerializedName("message_state") val messageState: Int = 2,
    @SerializedName("context_token") val contextToken: String,
    @SerializedName("item_list") val itemList: List<OutgoingItem>
)

data class OutgoingItem(
    @SerializedName("type") val type: Int = 1,
    @SerializedName("text_item") val textItem: OutgoingTextItem
)

data class OutgoingTextItem(
    @SerializedName("text") val text: String
)

data class BaseInfo(
    @SerializedName("channel_version") val channelVersion: String = "2.0.0"
)

data class SendMessageResponse(
    @SerializedName("ret") val ret: Int = -1,
    @SerializedName("err_msg") val errMsg: String? = null
)

// ===== Client =====

@Singleton
class IlinkClient @Inject constructor(
    private val gson: Gson
) {
    companion object {
        private const val BASE_URL = "https://ilinkai.weixin.qq.com"
        private const val LONG_POLL_TIMEOUT_MS = 45_000L
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLogger)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(LONG_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    private fun randomClientId(): String =
        "needai-weixin-${java.util.UUID.randomUUID().toString().replace("-", "").take(12)}"

    /** 每次请求重新生成 X-WECHAT-UIN：随机 4 字节 → uint32 → 十进制字符串 → base64 */
    private fun getWechatUin(): String {
        val randomUint32 = (0L..0xFFFFFFFFL).random().toUInt()
        return java.util.Base64.getUrlEncoder().encodeToString(
            randomUint32.toString().toByteArray()
        )
    }

    private fun buildHeaders(botToken: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer $botToken",
        "AuthorizationType" to "ilink_bot_token",
        "X-WECHAT-UIN" to getWechatUin(),
        "Content-Type" to "application/json"
    )

    /**
     * 获取授权 URL（手机端可直接打开 URL 确认，无需扫码）
     */
    suspend fun getBotQrCode(botType: Int = 3): Result<QrCodeResponse> = runCatching {
        val TAG = "IlinkClient"
        FileLogger.i(TAG, "getBotQrCode: botType=$botType")
        val url = "$BASE_URL/ilink/bot/get_bot_qrcode?bot_type=$botType"
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string().orEmpty()
        FileLogger.i(TAG, "getBotQrCode 响应: HTTP ${response.code}, body=${body.take(200)}")
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${body.take(200)}")
        }
        if (body.isEmpty()) throw Exception("Empty response")
        gson.fromJson(body, QrCodeResponse::class.java)
    }

    /**
     * 轮询授权状态
     * status: "wait" → "scanned" → "confirmed"
     * confirmed 时 botToken 字段会携带 token
     */
    suspend fun getQrCodeStatus(qrcodeRaw: String?): Result<QrCodeStatus> = runCatching {
        val TAG = "IlinkClient"
        if (qrcodeRaw == null) throw Exception("qrcodeRaw is null")
        FileLogger.i(TAG, "getQrCodeStatus: qrcodeRaw=${qrcodeRaw.take(20)}")
        val url = "$BASE_URL/ilink/bot/get_qrcode_status?qrcode=$qrcodeRaw"
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string().orEmpty()
        FileLogger.i(TAG, "getQrCodeStatus 响应: HTTP ${response.code}, body=${body.take(200)}")
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${body.take(200)}")
        }
        if (body.isEmpty()) throw Exception("Empty response")

        // qrcode_raw 自身可能就是 token
        val status = gson.fromJson(body, QrCodeStatus::class.java)
        if (status.status == "confirmed" && status.botToken == null) {
            status.copy(botToken = qrcodeRaw)
        } else {
            status
        }
    }

    /**
     * 长轮询拉取消息（hold 最多 35s）
     */
    suspend fun getUpdates(
        cursor: String?,
        botToken: String
    ): Result<GetUpdatesResponse> = runCatching {
        val TAG = "IlinkClient"
        val json = buildString {
            append("{\"get_updates_buf\":\"${cursor.orEmpty()}\",")
            append("\"base_info\":{\"channel_version\":\"2.0.0\"}}")
        }
        val request = Request.Builder()
            .url("$BASE_URL/ilink/bot/getupdates")
            .also { req -> buildHeaders(botToken).forEach { (k, v) -> req.addHeader(k, v) } }
            .post(json.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")

        if (response.code == 401) {
            FileLogger.w(TAG, "getUpdates Token 过期 (401)")
            throw TokenExpiredException()
        }

        val type = object : TypeToken<GetUpdatesResponse>() {}.type
        val parsed = gson.fromJson<GetUpdatesResponse>(body, type)
        if (parsed.messages.isNotEmpty()) {
            FileLogger.i(TAG, "getUpdates: ${parsed.messages.size} 条新消息")
        }
        parsed
    }

    /**
     * 发送回复消息
     */
    suspend fun sendMessage(
        toUserId: String,
        text: String,
        contextToken: String,
        botToken: String,
        syncBuf: String? = null
    ): Result<Boolean> = runCatching {
        val TAG = "IlinkClient"
        FileLogger.i(TAG, "sendMessage: toUser=${toUserId.take(20)}, syncBuf=${syncBuf?.take(20) ?: "null"}, text=${text.take(100)}")
        val requestBody = SendMessageRequest(
            msg = OutgoingMessage(
                toUserId = toUserId,
                clientId = randomClientId(),
                contextToken = contextToken,
                itemList = listOf(
                    OutgoingItem(
                        textItem = OutgoingTextItem(text = text)
                    )
                )
            ),
            syncBuf = syncBuf
        )
        val json = gson.toJson(requestBody)
        val request = Request.Builder()
            .url("$BASE_URL/ilink/bot/sendmessage")
            .also { req -> buildHeaders(botToken).forEach { (k, v) -> req.addHeader(k, v) } }
            .post(json.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            FileLogger.w(TAG, "sendMessage 失败: HTTP ${response.code}, body=${body.take(500)}")
            throw Exception("HTTP ${response.code}: ${body.take(200)}")
        }
        val parsed = if (body.isNotBlank()) {
            gson.fromJson(body, SendMessageResponse::class.java)
        } else null
        if (parsed != null && parsed.ret != 0 && parsed.ret != -1) {
            FileLogger.w(TAG, "sendMessage 业务错误: ret=${parsed.ret}, errMsg=${parsed.errMsg}")
            throw Exception("sendMessage 失败: ret=${parsed.ret}, ${parsed.errMsg ?: ""}")
        }
        FileLogger.i(TAG, "sendMessage 完成: HTTP ${response.code}, body=${body.take(500)}")
        true
    }

    /**
     * 发送"正在输入"状态
     * status: 1 = 开始输入, 2 = 停止输入
     */
    suspend fun sendTyping(
        toUserId: String,
        status: Int,
        contextToken: String,
        botToken: String
    ): Result<Boolean> = runCatching {
        val TAG = "IlinkClient"
        FileLogger.i(TAG, "sendTyping: toUser=${toUserId.take(20)}, status=$status")
        val json = buildString {
            append("{\"to_user_id\":\"$toUserId\",")
            append("\"status\":$status,")
            append("\"context_token\":\"$contextToken\"}")
        }
        val request = Request.Builder()
            .url("$BASE_URL/ilink/bot/sendtyping")
            .also { req -> buildHeaders(botToken).forEach { (k, v) -> req.addHeader(k, v) } }
            .post(json.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().isSuccessful
    }
}

class TokenExpiredException : Exception("iLink Token expired, need re-auth")
