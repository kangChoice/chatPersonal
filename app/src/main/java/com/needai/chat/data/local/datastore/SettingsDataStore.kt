package com.needai.chat.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
}
