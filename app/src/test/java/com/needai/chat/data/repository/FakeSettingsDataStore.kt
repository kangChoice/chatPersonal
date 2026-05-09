package com.needai.chat.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake SettingsDataStore for unit testing. Mirrors the real interface
 * without Android DataStore dependencies.
 */
class FakeSettingsDataStore {

    private val _selectedSkillId = MutableStateFlow("default")
    private val _currentSessionId = MutableStateFlow(java.util.UUID.randomUUID().toString())
    private val _chatFontSize = MutableStateFlow(16f)

    val selectedSkillId: Flow<String> = _selectedSkillId
    val currentSessionId: Flow<String> = _currentSessionId
    val chatFontSize: Flow<Float> = _chatFontSize

    suspend fun setSelectedSkillId(id: String) {
        _selectedSkillId.value = id
    }

    suspend fun setCurrentSessionId(id: String) {
        _currentSessionId.value = id
    }

    suspend fun setChatFontSize(size: Float) {
        _chatFontSize.value = size
    }
}
