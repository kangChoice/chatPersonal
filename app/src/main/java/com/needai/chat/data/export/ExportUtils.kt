package com.needai.chat.data.export

import android.content.Context
import android.net.Uri
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.MessageRole
import com.needai.chat.domain.model.Skill
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun generateSessionMarkdown(
        title: String,
        skillName: String,
        skillAvatar: String,
        messages: List<Message>,
        createdAt: Long,
        updatedAt: Long
    ): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        sb.appendLine("# $title")
        sb.appendLine()
        sb.appendLine("$skillAvatar $skillName · ${dateFormat.format(Date(createdAt))} · 共 ${messages.size} 条消息")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        val displayMessages = messages.filter { it.role != MessageRole.SYSTEM }
        for (msg in displayMessages) {
            val roleLabel = when (msg.role) {
                MessageRole.USER -> "User"
                MessageRole.ASSISTANT -> "Assistant"
                MessageRole.SYSTEM -> "System"
            }
            sb.appendLine("**$roleLabel**")
            sb.appendLine()
            sb.appendLine(msg.content)
            sb.appendLine()
        }

        sb.appendLine("---")
        sb.appendLine("导出时间: ${dateFormat.format(Date(System.currentTimeMillis()))}")
        return sb.toString()
    }

    fun generateModelConfigJson(
        config: com.needai.chat.domain.model.ModelConfig
    ): String {
        val json = JSONObject()
        json.put("name", config.name)
        json.put("protocol", config.protocol.value)
        json.put("baseUrl", config.remoteBaseUrl)
        json.put("apiKey", config.remoteApiKey)
        json.put("modelName", config.remoteModelName)
        json.put("temperature", config.temperature)
        json.put("maxTokens", config.maxTokens)
        json.put("topP", config.topP)
        return json.toString(2)
    }

    fun generateSkillJson(skill: Skill): String {
        val json = JSONObject()
        json.put("id", skill.id)
        json.put("name", skill.name)
        json.put("description", skill.description)
        json.put("avatar", skill.avatar)
        json.put("systemPrompt", skill.systemPrompt)
        json.put("greeting", skill.greeting)
        json.put("temperature", skill.temperature)
        json.put("tags", JSONObject.wrap(skill.tags))
        json.put("isBuiltin", skill.isBuiltin)
        return json.toString(2)
    }

    fun generateSkillsJson(skills: List<Skill>): String {
        val arr = org.json.JSONArray()
        for (skill in skills) {
            val json = JSONObject()
            json.put("id", skill.id)
            json.put("name", skill.name)
            json.put("description", skill.description)
            json.put("avatar", skill.avatar)
            json.put("systemPrompt", skill.systemPrompt)
            json.put("greeting", skill.greeting)
            json.put("temperature", skill.temperature)
            json.put("tags", JSONObject.wrap(skill.tags))
            json.put("isBuiltin", skill.isBuiltin)
            arr.put(json)
        }
        val root = JSONObject()
        root.put("version", 1)
        root.put("type", "skills_export")
        root.put("skills", arr)
        return root.toString(2)
    }

    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
