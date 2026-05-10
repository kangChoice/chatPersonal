package com.needai.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.repository.ModelConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val modelConfigRepository: ModelConfigRepository
) : ViewModel() {

    private val _modelConfig = MutableStateFlow(ModelConfig())
    val modelConfig: StateFlow<ModelConfig> = _modelConfig.asStateFlow()

    private val _configs = MutableStateFlow<List<ModelConfig>>(emptyList())
    val configs: StateFlow<List<ModelConfig>> = _configs.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        viewModelScope.launch {
            modelConfigRepository.getModelConfig().collect { config ->
                _modelConfig.value = config
            }
        }
        viewModelScope.launch {
            modelConfigRepository.getAllConfigs().collect { configs ->
                _configs.value = configs
            }
        }
    }

    fun addConfig(config: ModelConfig) {
        viewModelScope.launch {
            modelConfigRepository.saveModelConfig(config)
            _saveSuccess.value = true
        }
    }

    fun importModelConfig(json: String, onResult: (Boolean, String) -> Unit) {
        val result = com.needai.chat.data.import.ImportUtils.parseModelConfigJson(json)
        if (result.isSuccess) {
            viewModelScope.launch {
                modelConfigRepository.saveModelConfig(result.getOrThrow())
                onResult(true, "配置已导入")
            }
        } else {
            onResult(false, result.exceptionOrNull()?.localizedMessage ?: "导入失败")
        }
    }

    fun updateConfig(config: ModelConfig) {
        viewModelScope.launch {
            modelConfigRepository.saveModelConfig(config)
            _saveSuccess.value = true
        }
    }

    fun deleteConfig(id: String) {
        viewModelScope.launch {
            modelConfigRepository.deleteConfig(id)
            _saveSuccess.value = true
        }
    }

    fun selectConfig(id: String) {
        viewModelScope.launch {
            modelConfigRepository.setSelectedConfigId(id)
        }
    }

    fun saveModelConfigDirectly(config: ModelConfig) {
        _modelConfig.value = config
        viewModelScope.launch {
            modelConfigRepository.saveModelConfig(config)
            _saveSuccess.value = true
        }
    }

    fun updateModelConfig(config: ModelConfig) {
        _modelConfig.value = config
    }

    fun dismissSaveSuccess() {
        _saveSuccess.value = false
    }
}
