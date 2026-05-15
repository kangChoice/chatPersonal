package com.needai.chat.ui.skills

import com.needai.chat.data.repository.FakeSkillRepository
import com.needai.chat.domain.model.Skill
import com.needai.chat.ui.chat.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SkillViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestDispatcherRule()

    private val fakeRepository = FakeSkillRepository()
    private lateinit var viewModel: SkillViewModel

    private val builtinSkill = Skill(
        id = "default",
        name = "默认助手",
        description = "通用助手",
        avatar = "🤖",
        systemPrompt = "你是一个友好的助手",
        greeting = "你好！",
        isBuiltin = true
    )

    @Before
    fun setup() {
        fakeRepository.setSkills(listOf(builtinSkill))
        viewModel = SkillViewModel(fakeRepository)
    }

    @Test
    fun `initial state should load skills from repository`() = runTest {
        val skills = viewModel.skills.value
        assertEquals(1, skills.size)
        assertEquals("default", skills[0].id)
    }

    @Test
    fun `createSkill should add new skill`() = runTest {
        viewModel.createSkill(
            name = "自定义技能",
            description = "我的自定义技能",
            systemPrompt = "你是一个自定义助手",
            avatar = "🌟",
            greeting = "你好！我是自定义的",
            temperature = 0.8
        )
        advanceUntilIdle()

        val skills = viewModel.skills.value
        assertEquals(2, skills.size)
        val customSkill = skills.find { it.id != "default" }
        assertNotNull(customSkill)
        assertEquals("自定义技能", customSkill?.name)
        assertFalse(customSkill?.isBuiltin ?: true)
    }

    @Test
    fun `updateSkill should modify existing skill`() = runTest {
        val updatedSkill = builtinSkill.copy(
            name = "修改后的助手",
            description = "修改后的描述"
        )

        viewModel.updateSkill(updatedSkill)
        advanceUntilIdle()

        val skills = viewModel.skills.value
        val found = skills.find { it.id == "default" }
        assertNotNull(found)
        assertEquals("修改后的助手", found?.name)
    }

    @Test
    fun `deleteSkill should remove custom skill`() = runTest {
        viewModel.createSkill(
            name = "待删除",
            description = "即将被删除",
            systemPrompt = "test",
            avatar = "🗑️",
            greeting = "bye",
            temperature = 0.5
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.skills.value.size)
        val customSkill = viewModel.skills.value.find { !it.isBuiltin }
        assertNotNull(customSkill)

        viewModel.deleteSkill(customSkill!!.id)
        advanceUntilIdle()

        assertEquals(1, viewModel.skills.value.size)
    }

    @Test
    fun `selectSkill should update selected skill id`() = runTest {
        val newSkill = Skill(
            id = "friend",
            name = "朋友",
            description = "朋友风格",
            avatar = "💛",
            systemPrompt = "你是一个朋友",
            greeting = "嗨！",
            isBuiltin = true
        )
        fakeRepository.setSkills(listOf(builtinSkill, newSkill))

        viewModel.selectSkill(newSkill)
        advanceUntilIdle()

        assertEquals("friend", viewModel.selectedSkillId.value)
        assertEquals("friend", fakeRepository.getSelectedSkillId())
    }

    @Test
    fun `getSkillById should return correct skill`() = runTest {
        val result = viewModel.getSkillById("default")
        assertNotNull(result)
        assertEquals("默认助手", result?.name)
    }
}
