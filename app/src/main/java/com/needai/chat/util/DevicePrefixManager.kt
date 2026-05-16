package com.needai.chat.util

import android.content.Context
import android.provider.Settings.Secure
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevicePrefixManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        private const val PREFIX_LENGTH = 10
    }

    @Volatile
    private var cachedPrefix: String? = null

    fun getPrefix(): String {
        cachedPrefix?.let { return it }
        val raw = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        val firstBytes = hash.copyOfRange(0, 6)
        val prefix = bytesToBase62(firstBytes)
            .padStart(PREFIX_LENGTH, '0')
            .takeLast(PREFIX_LENGTH)
        cachedPrefix = prefix
        return prefix
    }

    fun getRawDeviceId(): String {
        return Secure.getString(context.contentResolver, Secure.ANDROID_ID) ?: "N/A"
    }

    fun clearCache() {
        cachedPrefix = null
    }

    private fun bytesToBase62(bytes: ByteArray): String {
        val num = BigInteger(1, bytes)
        if (num == BigInteger.ZERO) return "0"
        val sb = StringBuilder()
        val base = BigInteger.valueOf(62)
        var remaining = num
        while (remaining > BigInteger.ZERO) {
            sb.append(BASE62[remaining.mod(base).toInt()])
            remaining = remaining.divide(base)
        }
        return sb.reverse().toString()
    }
}
