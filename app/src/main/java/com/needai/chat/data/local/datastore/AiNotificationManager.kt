package com.needai.chat.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.needai.chat.domain.model.AiNotificationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiNotificationStore: DataStore<Preferences> by preferencesDataStore(name = "ai_notification")

@Singleton
class AiNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private val CONFIGS_KEY = stringPreferencesKey("ai_notification_configs")
    }

    val configs: Flow<List<AiNotificationConfig>> = context.aiNotificationStore.data.map { prefs ->
        val json = prefs[CONFIGS_KEY] ?: "[]"
        parseConfigs(json)
    }

    suspend fun add(config: AiNotificationConfig) {
        context.aiNotificationStore.edit { prefs ->
            val current = parseConfigs(prefs[CONFIGS_KEY] ?: "[]").toMutableList()
            current.add(config)
            prefs[CONFIGS_KEY] = gson.toJson(current)
        }
    }

    suspend fun update(config: AiNotificationConfig) {
        context.aiNotificationStore.edit { prefs ->
            val current = parseConfigs(prefs[CONFIGS_KEY] ?: "[]").toMutableList()
            val index = current.indexOfFirst { it.id == config.id }
            if (index >= 0) {
                current[index] = config
            }
            prefs[CONFIGS_KEY] = gson.toJson(current)
        }
    }

    suspend fun delete(id: String) {
        context.aiNotificationStore.edit { prefs ->
            val current = parseConfigs(prefs[CONFIGS_KEY] ?: "[]").toMutableList()
            current.removeAll { it.id == id }
            prefs[CONFIGS_KEY] = gson.toJson(current)
        }
    }

    suspend fun getAll(): List<AiNotificationConfig> {
        val json = context.aiNotificationStore.data.map { it[CONFIGS_KEY] ?: "[]" }.first()
        return parseConfigs(json)
    }

    private fun parseConfigs(json: String): List<AiNotificationConfig> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val type = object : TypeToken<List<AiNotificationConfig>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
