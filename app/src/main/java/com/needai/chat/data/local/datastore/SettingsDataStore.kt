package com.needai.chat.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val SELECTED_SKILL_ID = stringPreferencesKey("selected_skill_id")
        private val CURRENT_SESSION_ID = stringPreferencesKey("current_session_id")
        private val CHAT_FONT_SIZE = floatPreferencesKey("chat_font_size")
    }

    val selectedSkillId: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[SELECTED_SKILL_ID] ?: "default"
    }

    val currentSessionId: Flow<String> = context.settingsStore.data.map { preferences ->
        preferences[CURRENT_SESSION_ID] ?: java.util.UUID.randomUUID().toString()
    }

    val chatFontSize: Flow<Float> = context.settingsStore.data.map { preferences ->
        preferences[CHAT_FONT_SIZE] ?: 16f
    }

    suspend fun setSelectedSkillId(id: String) {
        context.settingsStore.edit { preferences ->
            preferences[SELECTED_SKILL_ID] = id
        }
    }

    suspend fun setCurrentSessionId(id: String) {
        context.settingsStore.edit { preferences ->
            preferences[CURRENT_SESSION_ID] = id
        }
    }

    suspend fun setChatFontSize(size: Float) {
        context.settingsStore.edit { preferences ->
            preferences[CHAT_FONT_SIZE] = size
        }
    }
}
