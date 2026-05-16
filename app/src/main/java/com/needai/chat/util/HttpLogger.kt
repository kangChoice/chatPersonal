package com.needai.chat.util

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.util.concurrent.TimeUnit

/**
 * OkHttp 拦截器，自动记录 HTTP 请求/响应到本地日志文件。
 * 在 OkHttpClient.Builder 上添加 .addInterceptor(HttpLogger) 即可启用。
 */
object HttpLogger : Interceptor {

    private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    private const val MAX_BODY_LOG_LENGTH = 2048

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNs = System.nanoTime()

        // ===== 请求日志 =====
        val reqBody = request.body
        val isStream = isStreamingRequest(request)
        val tag = "HTTP"

        FileLogger.d(tag, buildString {
            appendLine("─── 请求 ───")
            appendLine("  接口: ${describeUrl(request.url.encodedPath)}")
            appendLine("  URL: ${request.method} ${request.url}")
            appendLine("  Streaming: $isStream")
            // headers（脱敏）
            request.headers.forEach { (name, value) ->
                appendLine("  Header: $name: ${maskSensitiveHeader(name, value)}")
            }
            // body
            if (reqBody != null && !isStream) {
                val buffer = Buffer()
                try {
                    reqBody.writeTo(buffer)
                    val bodyStr = buffer.readUtf8()
                    appendLine("  Body: ${truncateBody(bodyStr)}")
                } catch (_: Exception) {
                    appendLine("  Body: <读取失败>")
                }
            } else if (reqBody != null) {
                val buffer = Buffer()
                try {
                    reqBody.writeTo(buffer)
                    val bodyStr = buffer.readUtf8()
                    appendLine("  Body: ${truncateBody(bodyStr, 300)}")
                } catch (_: Exception) {
                    appendLine("  Body: <读取失败>")
                }
            } else {
                appendLine("  Body: (无)")
            }
        })

        // ===== 执行请求 =====
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            FileLogger.e(tag, "请求异常: ${request.method} ${request.url}", e)
            throw e
        }

        val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        // ===== 响应日志 =====
        FileLogger.d(tag, buildString {
            appendLine("─── 响应 ───")
            appendLine("  接口: ${describeUrl(request.url.encodedPath)}")
            appendLine("  状态: ${response.code} ${response.message} (${durationMs}ms)")
            // 非流式响应才记录 body
            if (!isStream && response.body != null) {
                val bodyStr = response.body!!.string()
                appendLine("  Body: ${truncateBody(bodyStr)}")
                // body.string() 会消费掉原始 body，需要重建
                val newBody = bodyStr.toResponseBody(response.body!!.contentType())
                return response.newBuilder().body(newBody).build()
            }
        })

        return response
    }

    /** 判断是否为 SSE 流式请求（对话接口） */
    private fun isStreamingRequest(request: okhttp3.Request): Boolean {
        val path = request.url.encodedPath
        return path.contains("chat/completions", ignoreCase = true) ||
               path.contains("/v1/messages", ignoreCase = true)
    }

    /** 把 URL path 转成中文描述 */
    private fun describeUrl(path: String): String = when {
        path.contains("chat/completions") -> "AI 对话"
        path.contains("/v1/messages") -> "AI 对话(Anthropic)"
        path.contains("voice-enrollment") && path.contains("create_voice") -> "创建音色"
        path.contains("voice-enrollment") && path.contains("list_voice") -> "查询音色列表"
        path.contains("voice-enrollment") && path.contains("query_voice") -> "查询音色详情"
        path.contains("voice-enrollment") && path.contains("delete_voice") -> "删除音色"
        else -> path
    }

    /** 对敏感 Header 脱敏（Authorization、x-api-key） */
    private fun maskSensitiveHeader(name: String, value: String): String {
        val sensitive = setOf("Authorization", "x-api-key")
        return if (name in sensitive) {
            if (value.length > 8) "${value.take(4)}****${value.takeLast(4)}"
            else "****"
        } else value
    }

    /** 截断过长 body */
    private fun truncateBody(body: String, maxLen: Int = MAX_BODY_LOG_LENGTH): String {
        return if (body.length > maxLen) "${body.take(maxLen)}... (共 ${body.length} 字符)"
        else body
    }
}
