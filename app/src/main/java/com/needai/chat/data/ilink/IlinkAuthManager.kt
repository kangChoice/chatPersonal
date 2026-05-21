package com.needai.chat.data.ilink

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.needai.chat.util.EncryptUtil
import com.needai.chat.util.FileLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ilinkStore: DataStore<Preferences> by preferencesDataStore(name = "ilink")

/**
 * iLink Token 授权管理。
 *
 * Token 使用 EncryptUtil（AES/GCM + Android Keystore）加密存储。
 * 修复了 EncryptUtil 的 IV 持久化 bug：将 iv + ciphertext 拼接为 "base64(iv):base64(ciphertext)" 格式。
 */
@Singleton
class IlinkAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "IlinkAuth"
        private val ILINK_TOKEN = stringPreferencesKey("ilink_bot_token")
        private val WECHAT_USER_ID = stringPreferencesKey("wechat_user_id")
        private val ILINK_SKILL_ID = stringPreferencesKey("ilink_skill_id")
        private val CONTEXT_TOKEN_CACHE = stringPreferencesKey("context_token_cache")
        private const val IV_CIPHER_SEPARATOR = ":"
    }

    /** 获取已存储的 Token（解密后），不存在或解密失败返回 null */
    suspend fun getToken(): String? {
        val encrypted = context.ilinkStore.data.map { prefs ->
            prefs[ILINK_TOKEN]
        }.first() ?: return null
        val token = decryptToken(encrypted)
        FileLogger.i(TAG, "getToken: ${if (token != null) "存在 (${token.take(8)}...)" else "null"}")
        return token
    }

    /** 加密并存储 Token */
    suspend fun saveToken(token: String) {
        FileLogger.i(TAG, "saveToken: ${token.take(8)}...")
        val encrypted = encryptToken(token)
        context.ilinkStore.edit { prefs ->
            prefs[ILINK_TOKEN] = encrypted
        }
        FileLogger.i(TAG, "saveToken: 完成")
    }

    /** 清除 Token */
    suspend fun clearToken() {
        FileLogger.i(TAG, "clearToken")
        context.ilinkStore.edit { prefs ->
            prefs.remove(ILINK_TOKEN)
            prefs.remove(WECHAT_USER_ID)
        }
    }

    /** 是否有有效的已存储 Token */
    suspend fun isAuthenticated(): Boolean {
        val authed = getToken() != null
        FileLogger.i(TAG, "isAuthenticated: $authed")
        return authed
    }

    /** 存储绑定微信用户 ID */
    suspend fun saveWechatUserId(userId: String) {
        FileLogger.i(TAG, "saveWechatUserId: $userId")
        context.ilinkStore.edit { prefs ->
            prefs[WECHAT_USER_ID] = userId
        }
    }

    /** 获取 iLink 独立的角色 ID，未设置返回 null */
    suspend fun getIlinkSkillId(): String? {
        val id = context.ilinkStore.data.map { prefs ->
            prefs[ILINK_SKILL_ID]
        }.first()
        FileLogger.i(TAG, "getIlinkSkillId: $id")
        return id
    }

    /** 设置 iLink 独立的角色 ID */
    suspend fun setIlinkSkillId(id: String) {
        FileLogger.i(TAG, "setIlinkSkillId: $id")
        context.ilinkStore.edit { prefs ->
            prefs[ILINK_SKILL_ID] = id
        }
    }

    /** 获取已持久化的 context_token 缓存 */
    suspend fun getContextTokens(): Map<String, String> {
        val json = context.ilinkStore.data.map { prefs ->
            prefs[CONTEXT_TOKEN_CACHE]
        }.first().orEmpty()
        if (json.isEmpty()) return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** 持久化 context_token 缓存 */
    suspend fun saveContextTokens(tokens: Map<String, String>) {
        val json = gson.toJson(tokens)
        context.ilinkStore.edit { prefs ->
            prefs[CONTEXT_TOKEN_CACHE] = json
        }
    }

    /** 获取绑定的微信用户 ID */
    suspend fun getWechatUserId(): String? {
        val id = context.ilinkStore.data.map { prefs ->
            prefs[WECHAT_USER_ID]
        }.first()
        FileLogger.i(TAG, "getWechatUserId: $id")
        return id
    }

    /** 对 Token 加密，返回 "base64(iv):base64(ciphertext)" 格式 */
    private fun encryptToken(token: String): String {
        val ciphertext = EncryptUtil.encrypt(token) ?: return token // fallback 明文
        // 从 ciphertext 中分离 IV
        // EncryptUtil.encrypt 返回的是 AES/GCM 的完整输出（ciphertext 前 12 字节为 IV）
        val iv = ciphertext.copyOfRange(0, 12)
        val ct = ciphertext.copyOfRange(12, ciphertext.size)
        return Base64.getEncoder().encodeToString(iv) +
                IV_CIPHER_SEPARATOR +
                Base64.getEncoder().encodeToString(ct)
    }

    /** 解密 "base64(iv):base64(ciphertext)" 格式 */
    private fun decryptToken(encrypted: String): String? {
        if (!encrypted.contains(IV_CIPHER_SEPARATOR)) {
            // 明文 fallback（未加密的旧数据）
            return encrypted
        }
        return try {
            val parts = encrypted.split(IV_CIPHER_SEPARATOR, limit = 2)
            val iv = Base64.getDecoder().decode(parts[0])
            val ct = Base64.getDecoder().decode(parts[1])
            EncryptUtil.decrypt(ct, iv)
        } catch (_: Exception) {
            null
        }
    }
}
