package com.needai.chat.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.needai.chat.data.local.config.ModelConfigFileManager
import com.needai.chat.domain.model.ApiProtocol
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.ModelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.modelConfigStore: DataStore<Preferences> by preferencesDataStore(name = "model_config")

class ModelConfigDataStore(
    private val context: Context,
    private val configFileManager: ModelConfigFileManager
) {

    companion object {
        private val MODEL_TYPE = stringPreferencesKey("model_type")
        private val PROTOCOL = stringPreferencesKey("protocol")
        private val REMOTE_BASE_URL = stringPreferencesKey("remote_base_url")
        private val REMOTE_API_KEY = stringPreferencesKey("remote_api_key")
        private val REMOTE_MODEL_NAME = stringPreferencesKey("remote_model_name")
        private val LOCAL_BASE_URL = stringPreferencesKey("local_base_url")
        private val LOCAL_MODEL_NAME = stringPreferencesKey("local_model_name")
        private val TEMPERATURE = doublePreferencesKey("temperature")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val TOP_P = doublePreferencesKey("top_p")
        private val CONTEXT_WINDOW = intPreferencesKey("context_window")
    }

    val modelConfig: Flow<ModelConfig> = context.modelConfigStore.data.map { preferences ->
        val fileConfig = configFileManager.loadConfig()
        ModelConfig(
            modelType = try {
                ModelType.valueOf(preferences[MODEL_TYPE] ?: ModelType.REMOTE.name)
            } catch (e: IllegalArgumentException) {
                ModelType.REMOTE
            },
            protocol = ApiProtocol.fromValue(preferences[PROTOCOL] ?: fileConfig.protocol),
            remoteBaseUrl = preferences[REMOTE_BASE_URL] ?: fileConfig.remoteBaseUrl,
            remoteApiKey = preferences[REMOTE_API_KEY] ?: fileConfig.remoteApiKey,
            remoteModelName = preferences[REMOTE_MODEL_NAME] ?: fileConfig.remoteModelName,
            localBaseUrl = preferences[LOCAL_BASE_URL] ?: fileConfig.localBaseUrl,
            localModelName = preferences[LOCAL_MODEL_NAME] ?: fileConfig.localModelName,
            temperature = preferences[TEMPERATURE] ?: fileConfig.temperature,
            maxTokens = preferences[MAX_TOKENS] ?: fileConfig.maxTokens,
            topP = preferences[TOP_P] ?: fileConfig.topP,
            contextWindow = preferences[CONTEXT_WINDOW] ?: fileConfig.contextWindow
        )
    }

    suspend fun saveModelConfig(config: ModelConfig) {
        context.modelConfigStore.edit { preferences ->
            preferences[MODEL_TYPE] = config.modelType.name
            preferences[PROTOCOL] = config.protocol.value
            preferences[REMOTE_BASE_URL] = config.remoteBaseUrl
            preferences[REMOTE_API_KEY] = config.remoteApiKey
            preferences[REMOTE_MODEL_NAME] = config.remoteModelName
            preferences[LOCAL_BASE_URL] = config.localBaseUrl
            preferences[LOCAL_MODEL_NAME] = config.localModelName
            preferences[TEMPERATURE] = config.temperature
            preferences[MAX_TOKENS] = config.maxTokens
            preferences[TOP_P] = config.topP
            preferences[CONTEXT_WINDOW] = config.contextWindow
        }
        // Also sync to config file
        configFileManager.saveConfig(configFileManager.fromModelConfig(config))
    }
}
