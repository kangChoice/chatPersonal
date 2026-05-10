package com.needai.chat.data.local.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.needai.chat.domain.model.ApiProtocol
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.ModelType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConfigFile(
    val protocol: String = "openai",
    @SerializedName("remote_base_url") val remoteBaseUrl: String = "",
    @SerializedName("remote_api_key") val remoteApiKey: String = "",
    @SerializedName("remote_model_name") val remoteModelName: String = "",
    @SerializedName("local_base_url") val localBaseUrl: String = "",
    @SerializedName("local_model_name") val localModelName: String = "",
    val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    @SerializedName("top_p") val topP: Double = 1.0
)

@Singleton
class ModelConfigFileManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val configDir: File get() = File(context.filesDir, "config")
    private val configFile: File get() = File(configDir, "model_config.json")

    private val _configFlow = MutableStateFlow(loadConfigInternal())
    val configFlow: StateFlow<ConfigFile> = _configFlow.asStateFlow()

    fun ensureConfigExists() {
        if (!configFile.exists()) {
            configDir.mkdirs()
            try {
                context.assets.open("model_config.json").use { input ->
                    configFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // Fallback: write default config if assets not available
                configFile.writeText(gson.toJson(ConfigFile()))
            }
        }
    }

    fun loadConfig(): ConfigFile {
        val config = loadConfigInternal()
        _configFlow.value = config
        return config
    }

    private fun loadConfigInternal(): ConfigFile {
        return try {
            if (configFile.exists()) {
                gson.fromJson(configFile.readText(), ConfigFile::class.java)
            } else {
                ConfigFile()
            }
        } catch (e: Exception) {
            ConfigFile()
        }
    }

    fun saveConfig(config: ConfigFile) {
        ensureConfigExists()
        configFile.writeText(gson.toJson(config))
        _configFlow.value = config
    }

    fun toModelConfig(configFile: ConfigFile): ModelConfig {
        return ModelConfig(
            modelType = ModelType.REMOTE,
            protocol = ApiProtocol.fromValue(configFile.protocol),
            remoteBaseUrl = configFile.remoteBaseUrl,
            remoteApiKey = configFile.remoteApiKey,
            remoteModelName = configFile.remoteModelName,
            localBaseUrl = configFile.localBaseUrl,
            localModelName = configFile.localModelName,
            temperature = configFile.temperature,
            maxTokens = configFile.maxTokens,
            topP = configFile.topP
        )
    }

    fun fromModelConfig(config: ModelConfig): ConfigFile {
        return ConfigFile(
            protocol = config.protocol.value,
            remoteBaseUrl = config.remoteBaseUrl,
            remoteApiKey = config.remoteApiKey,
            remoteModelName = config.remoteModelName,
            localBaseUrl = config.localBaseUrl,
            localModelName = config.localModelName,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            topP = config.topP
        )
    }
}
