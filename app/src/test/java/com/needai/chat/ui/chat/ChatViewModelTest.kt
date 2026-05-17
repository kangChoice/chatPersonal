package com.needai.chat.ui.chat

import com.needai.chat.data.repository.FakeChatRepository
import com.needai.chat.data.repository.FakeModelConfigRepository
import com.needai.chat.data.repository.FakeSessionRepository
import com.needai.chat.data.repository.FakeSkillRepository
import com.needai.chat.data.repository.FakeVoiceRepository
import com.needai.chat.domain.model.Skill
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestDispatcherRule()

    private val fakeChatRepository = FakeChatRepository()
    private val fakeSkillRepository = FakeSkillRepository()
    private val fakeModelConfigRepository = FakeModelConfigRepository()
    private val fakeSessionRepository = FakeSessionRepository()
    private val fakeVoiceRepository = FakeVoiceRepository()

    private lateinit var viewModel: ChatViewModel

    private val defaultSkill = Skill(
        id = "test-default",
        name = "测试助手",
        description = "测试用",
        avatar = "🤖",
        systemPrompt = "你是一个测试助手",
        greeting = "你好！",
        isBuiltin = true
    )

    @Before
    fun setup() {
        fakeSkillRepository.setSkills(listOf(defaultSkill))
        runTest {
            fakeSkillRepository.setSelectedSkillId("test-default")
        }
        viewModel = ChatViewModel(
            chatRepository = fakeChatRepository,
            skillRepository = fakeSkillRepository,
            modelConfigRepository = fakeModelConfigRepository,
            sessionRepository = fakeSessionRepository,
            voiceRepository = fakeVoiceRepository
        )
    }

    @Test
    fun `initial state should have default values`() = runTest {
        val state = viewModel.uiState.value
        assertFalse(state.isStreaming)
        assertEquals("", state.inputText)
        assertEquals("", state.currentStreamingMessage)
        assertNotNull(state.sessionId)
        assertTrue(state.sessionId.isNotBlank())
    }

    @Test
    fun `onInputChanged should update input text`() = runTest {
        viewModel.onInputChanged("Hello")
        assertEquals("Hello", viewModel.uiState.value.inputText)

        viewModel.onInputChanged("")
        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun `sendMessage with empty text should do nothing`() = runTest {
        viewModel.onInputChanged("   ")
        viewModel.sendMessage()
        assertFalse(viewModel.uiState.value.isStreaming)
    }

    @Test
    fun `clearSession should clear messages`() = runTest {
        viewModel.onInputChanged("你好")
        viewModel.sendMessage()
        advanceUntilIdle()

        viewModel.clearSession()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals("", viewModel.uiState.value.currentStreamingMessage)
        assertFalse(viewModel.uiState.value.isStreaming)
    }

    @Test
    fun `switchSkill should update current skill`() = runTest {
        val newSkill = Skill(
            id = "friend",
            name = "知心朋友",
            description = "朋友风格",
            avatar = "💛",
            systemPrompt = "你是一个朋友",
            greeting = "嗨！",
            isBuiltin = true
        )

        viewModel.switchSkill(newSkill)
        advanceUntilIdle()

        assertEquals("friend", viewModel.uiState.value.currentSkill.id)
        assertEquals("知心朋友", viewModel.uiState.value.currentSkill.name)
    }

    @Test
    fun `dismissError should clear error`() = runTest {
        viewModel.dismissError()
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `currentSkill should be loaded from repository at init`() = runTest {
        val state = viewModel.uiState.value
        assertEquals("test-default", state.currentSkill.id)
        assertEquals("测试助手", state.currentSkill.name)
    }

    @Test
    fun `sendMessage should start streaming`() = runTest {
        viewModel.onInputChanged("你好")
        viewModel.sendMessage()

        assertTrue(viewModel.uiState.value.isStreaming)
    }

    @Test
    fun `stopStreaming should cancel streaming job`() = runTest {
        viewModel.onInputChanged("测试")
        viewModel.sendMessage()

        viewModel.stopStreaming()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isStreaming)
    }
}
