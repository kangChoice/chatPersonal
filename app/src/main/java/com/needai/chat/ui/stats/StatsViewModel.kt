package com.needai.chat.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.data.local.db.dao.TokenTotals
import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.repository.ChatRepository
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class StatsUiState(
    val sessions: List<ChatSession> = emptyList(),
    val configs: List<ModelConfig> = emptyList(),
    val selectedSessionId: String? = null,
    val selectedConfigId: String? = null,
    val startTime: Long = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis,
    val endTime: Long = System.currentTimeMillis(),
    val tokenTotals: TokenTotals = TokenTotals(),
    val isLoading: Boolean = false
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
    private val modelConfigRepository: ModelConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            sessionRepository.getAllSessions().collect { sessions ->
                _uiState.update { it.copy(sessions = sessions) }
            }
        }
        viewModelScope.launch {
            modelConfigRepository.getAllConfigs().collect { configs ->
                _uiState.update { it.copy(configs = configs) }
            }
        }
        // Initial token load
        refreshTokens()
    }

    fun selectSession(sessionId: String?) {
        _uiState.update { it.copy(selectedSessionId = sessionId, selectedConfigId = null) }
        refreshTokens()
    }

    fun selectConfig(configId: String?) {
        _uiState.update { it.copy(selectedConfigId = configId, selectedSessionId = null) }
        refreshTokens()
    }

    fun setTimeRange(start: Long, end: Long) {
        _uiState.update { it.copy(startTime = start, endTime = end) }
        refreshTokens()
    }

    private fun refreshTokens() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value
            val totals = when {
                state.selectedSessionId != null -> {
                    chatRepository.getTokenTotalsBySession(state.selectedSessionId)
                }
                state.selectedConfigId != null -> {
                    chatRepository.getTokenTotalsByModelConfig(state.selectedConfigId)
                }
                else -> {
                    chatRepository.getTokenTotalsByTimeRange(state.startTime, state.endTime)
                }
            }
            _uiState.update { it.copy(tokenTotals = totals, isLoading = false) }
        }
    }
}
