package com.needai.chat.util

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * OkHttp 拦截器，自动记录 HTTP 请求/响应到本地日志文件。
 * 在 OkHttpClient.Builder 上添加 .addInterceptor(HttpLogger) 即可启用。
 */
object HttpLogger : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNs = System.nanoTime()

        FileLogger.d("HTTP", "${request.method} ${request.url}")

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            FileLogger.e("HTTP", "请求异常: ${request.method} ${request.url}", e)
            throw e
        }

        val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        FileLogger.d("HTTP", "${response.code} ${request.url} (${durationMs}ms)")

        return response
    }
}
