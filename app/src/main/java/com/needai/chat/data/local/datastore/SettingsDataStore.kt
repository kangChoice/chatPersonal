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
import com.needai.chat.util.Constants
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

}
