package com.needai.chat.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.needai.chat.domain.model.BackgroundConfig
import com.needai.chat.util.Constants
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val SELECTED_SKILL_ID = stringPreferencesKey("selected_skill_id")
        private val CURRENT_SESSION_ID = stringPreferencesKey("current_session_id")
        private val SELECTED_MODEL_CONFIG_ID = stringPreferencesKey("selected_model_config_id")
        private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")

        // TTS 配置
        private val TTS_PROVIDER = stringPreferencesKey("tts_provider")
        private val TTS_API_KEY = stringPreferencesKey("tts_api_key")
        private val TTS_MODEL = stringPreferencesKey("tts_model")
        private val TTS_VOICE = stringPreferencesKey("tts_voice")
        private val TTS_VOLUME = intPreferencesKey("tts_volume")
        private val TTS_RATE = floatPreferencesKey("tts_rate")
        private val TTS_PITCH = floatPreferencesKey("tts_pitch")
        private val TTS_PREFIX = stringPreferencesKey("tts_prefix")
        private val TTS_AUTO_READ = booleanPreferencesKey("tts_auto_read")
        private val VOICE_ALIASES = stringPreferencesKey("voice_aliases")
        private val BACKGROUNDS = stringPreferencesKey("backgrounds")
        private val SELECTED_BACKGROUND_ID = stringPreferencesKey("selected_background_id")
        private val DEVICE_PREFIX = stringPreferencesKey("device_prefix")
        private val USER_AVATAR_PATH = stringPreferencesKey("user_avatar_path")
    }

    val selectedSkillId: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[SELECTED_SKILL_ID] ?: Constants.DEFAULT_SKILL_ID
    }

    val currentSessionId: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[CURRENT_SESSION_ID] ?: java.util.UUID.randomUUID().toString()
    }

    val selectedModelConfigId: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[SELECTED_MODEL_CONFIG_ID] ?: ""
    }

    suspend fun setSelectedModelConfigId(id: String) {
        context.settingsStore.edit { preferences ->
            preferences[SELECTED_MODEL_CONFIG_ID] = id
        }
    }

    suspend fun setSelectedSkillId(id: String) {
        context.settingsStore.edit { preferences ->
            preferences[SELECTED_SKILL_ID] = id
        }
    }

    val isDarkMode: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[IS_DARK_MODE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsStore.edit { preferences ->
            preferences[IS_DARK_MODE] = enabled
        }
    }

    suspend fun setCurrentSessionId(id: String) {
        context.settingsStore.edit { preferences ->
            preferences[CURRENT_SESSION_ID] = id
        }
    }

    // ===== TTS 配置 =====

    val ttsProvider: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[TTS_PROVIDER] ?: "system"
    }

    val ttsApiKey: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[TTS_API_KEY] ?: ""
    }

    val ttsModel: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[TTS_MODEL] ?: "cosyvoice-v3.5-flash"
    }

    val ttsVoice: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[TTS_VOICE] ?: ""
    }

    val ttsVolume: Flow<Int> = context.settingsStore.data.map { preferences ->
        preferences[TTS_VOLUME] ?: 50
    }

    val ttsRate: Flow<Float> = context.settingsStore.data.map { preferences ->
        preferences[TTS_RATE] ?: 1.0f
    }

    val ttsPitch: Flow<Float> = context.settingsStore.data.map { preferences ->
        preferences[TTS_PITCH] ?: 1.0f
    }

    val ttsPrefix: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[TTS_PREFIX] ?: "needai"
    }

    val ttsAutoRead: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[TTS_AUTO_READ] ?: false
    }

    suspend fun setTtsProvider(provider: String) {
        context.settingsStore.edit { preferences ->
            preferences[TTS_PROVIDER] = provider
        }
    }

    suspend fun setTtsApiKey(key: String) {
        context.settingsStore.edit { preferences ->
            preferences[TTS_API_KEY] = key
        }
    }

    suspend fun setTtsModel(model: String) {
        context.settingsStore.edit { preferences ->
            preferences[TTS_MODEL] = model
        }
    }

    suspend fun setTtsVoice(voice: String) {
        context.settingsStore.edit { preferences ->
            preferences[TTS_VOICE] = voice
        }
    }

    suspend fun setTtsVolume(volume: Int) {
        context.settingsStore.edit { preferences ->
            preferences[TTS_VOLUME] = volume
        }
    }

    suspend fun setTtsRate(rate: Float) {
        context.settingsStore.edit { preferences ->
            preferences[TTS_RATE] = rate
        }
    }

    suspend fun setTtsPitch(pitch: Float) {
        context.settingsStore.edit { preferences ->
            preferences[TTS_PITCH] = pitch
        }
    }

    suspend fun setTtsPrefix(prefix: String) {
        context.settingsStore.edit { preferences ->
            preferences[TTS_PREFIX] = prefix
        }
    }

    suspend fun setTtsAutoRead(enabled: Boolean) {
        context.settingsStore.edit { preferences ->
            preferences[TTS_AUTO_READ] = enabled
        }
    }

    // ===== 音色别名映射 =====

    val voiceAliases: Flow<Map<String, String>> = context.settingsStore.data.map { preferences ->
        val json = preferences[VOICE_ALIASES] ?: "{}"
        parseAliasesMap(json)
    }

    suspend fun setVoiceAlias(voiceId: String, alias: String) {
        context.settingsStore.edit { preferences ->
            val current = parseAliasesMap(preferences[VOICE_ALIASES] ?: "{}")
            val updated = current.toMutableMap()
            if (alias.isBlank()) updated.remove(voiceId)
            else updated[voiceId] = alias
            preferences[VOICE_ALIASES] = encodeAliasesMap(updated)
        }
    }

    suspend fun setVoiceAliases(aliases: Map<String, String>) {
        context.settingsStore.edit { preferences ->
            preferences[VOICE_ALIASES] = encodeAliasesMap(aliases)
        }
    }

    private fun parseAliasesMap(json: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(json)
            for (key in obj.keys()) {
                map[key] = obj.optString(key, "")
            }
        } catch (_: Exception) { }
        return map
    }

    private fun encodeAliasesMap(map: Map<String, String>): String {
        val obj = JSONObject()
        for ((key, value) in map) {
            obj.put(key, value)
        }
        return obj.toString()
    }

    // ===== 聊天背景 =====

    val backgrounds: Flow<List<BackgroundConfig>> = context.settingsStore.data.map { preferences ->
        val json = preferences[BACKGROUNDS] ?: "[]"
        parseBackgrounds(json)
    }

    val selectedBackgroundId: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[SELECTED_BACKGROUND_ID] ?: ""
    }

    suspend fun setSelectedBackgroundId(id: String) {
        context.settingsStore.edit { preferences ->
            preferences[SELECTED_BACKGROUND_ID] = id
        }
    }

    val devicePrefix: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[DEVICE_PREFIX] ?: ""
    }

    suspend fun setDevicePrefix(prefix: String) {
        context.settingsStore.edit { preferences ->
            preferences[DEVICE_PREFIX] = prefix
        }
    }

    // ===== 用户头像 =====

    val userAvatarPath: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[USER_AVATAR_PATH] ?: ""
    }

    suspend fun setUserAvatarPath(path: String) {
        context.settingsStore.edit { preferences ->
            preferences[USER_AVATAR_PATH] = path
        }
    }

    suspend fun addBackground(background: BackgroundConfig) {
        context.settingsStore.edit { preferences ->
            val current = parseBackgrounds(preferences[BACKGROUNDS] ?: "[]").toMutableList()
            current.removeAll { it.id == background.id }
            current.add(background)
            preferences[BACKGROUNDS] = encodeBackgrounds(current)
        }
    }

    suspend fun removeBackground(id: String) {
        context.settingsStore.edit { preferences ->
            val current = parseBackgrounds(preferences[BACKGROUNDS] ?: "[]").toMutableList()
            current.removeAll { it.id == id }
            preferences[BACKGROUNDS] = encodeBackgrounds(current)
            // Also clear selection if it was the deleted one
            if (preferences[SELECTED_BACKGROUND_ID] == id) {
                preferences[SELECTED_BACKGROUND_ID] = ""
            }
        }
    }

    private fun parseBackgrounds(json: String): List<BackgroundConfig> {
        val list = mutableListOf<BackgroundConfig>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(BackgroundConfig(
                    id = obj.optString("id", ""),
                    name = obj.optString("name", ""),
                    imagePath = obj.optString("imagePath", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                ))
            }
        } catch (_: Exception) { }
        return list
    }

    private fun encodeBackgrounds(list: List<BackgroundConfig>): String {
        val arr = JSONArray()
        for (bg in list) {
            val obj = JSONObject()
            obj.put("id", bg.id)
            obj.put("name", bg.name)
            obj.put("imagePath", bg.imagePath)
            obj.put("createdAt", bg.createdAt)
            arr.put(obj)
        }
        return arr.toString()
    }

}
