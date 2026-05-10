package com.needai.chat.data.import

import android.content.Context
import android.net.Uri
import com.needai.chat.domain.model.ApiProtocol
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.Skill
import org.json.JSONObject

data class ImportMessage(
    val role: String,
    val content: String
)

data class ImportSessionData(
    val title: String,
    val skillName: String,
    val skillAvatar: String,
    val messages: List<ImportMessage>
)

object ImportUtils {

    fun readFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
        } catch (e: Exception) {
            null
        }
    }

    fun parseModelConfigJson(json: String): Result<ModelConfig> {
        return try {
            val obj = JSONObject(json)
            val protocolStr = obj.optString("protocol", "openai")
            val protocol = ApiProtocol.entries.find { it.value == protocolStr } ?: ApiProtocol.OPENAI
            val config = ModelConfig(
                name = obj.optString("name", ""),
                protocol = protocol,
                remoteBaseUrl = obj.optString("baseUrl", ""),
                remoteApiKey = obj.optString("apiKey", ""),
                remoteModelName = obj.optString("modelName", ""),
                temperature = obj.optDouble("temperature", 0.7),
                maxTokens = obj.optInt("maxTokens", 4096),
                topP = obj.optDouble("topP", 1.0)
            )
            if (config.remoteBaseUrl.isBlank()) {
                return Result.failure(Exception("无效的配置：缺少 Base URL"))
            }
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(Exception("解析配置失败: ${e.localizedMessage}"))
        }
    }

    fun parseSkillJson(json: String): Result<Skill> {
        return try {
            val obj = JSONObject(json)
            val tags = mutableListOf<String>()
            val tagsArr = obj.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    tagsArr.optString(i)?.let { tags.add(it) }
                }
            }
            val skill = Skill(
                id = java.util.UUID.randomUUID().toString(),
                name = obj.optString("name", ""),
                description = obj.optString("description", ""),
                avatar = obj.optString("avatar", "🤖"),
                systemPrompt = obj.optString("systemPrompt", ""),
                greeting = obj.optString("greeting", ""),
                temperature = obj.optDouble("temperature", 0.7),
                tags = tags,
                isBuiltin = false
            )
            if (skill.name.isBlank()) {
                return Result.failure(Exception("无效的技能：缺少名称"))
            }
            if (skill.systemPrompt.isBlank()) {
                return Result.failure(Exception("无效的技能：缺少系统提示词"))
            }
            Result.success(skill)
        } catch (e: Exception) {
            Result.failure(Exception("解析技能失败: ${e.localizedMessage}"))
        }
    }

    fun parseSessionMarkdown(md: String): Result<ImportSessionData> {
        return try {
            val lines = md.lines()
            if (lines.isEmpty()) return Result.failure(Exception("空文件"))

            // Title: first line "# title"
            val title = lines.first().removePrefix("# ").trim()

            // Meta line: first non-empty non-title line with " · "
            val metaLine = lines.drop(1).firstOrNull { it.contains(" · ") }
                ?: return Result.failure(Exception("未找到元数据行"))
            val metaParts = metaLine.split(" · ")
            val avatarAndName = metaParts.getOrElse(0) { "" }.trim()
            val avatar = avatarAndName.firstOrNull()?.toString() ?: ""
            val skillName = avatarAndName.drop(1).trim()

            // Find --- separators
            val separatorIndices = lines.mapIndexedNotNull { idx, line ->
                if (line.trim() == "---") idx else null
            }
            if (separatorIndices.size < 2) {
                return Result.failure(Exception("无效的会话格式：缺少消息分隔符"))
            }

            val firstSep = separatorIndices.first()
            val lastSep = separatorIndices.last()

            // Parse messages between first and last ---
            val messages = mutableListOf<ImportMessage>()
            var currentRole: String? = null
            val content = StringBuilder()

            for (i in firstSep + 1 until lastSep) {
                val line = lines[i]
                val roleMatch = Regex("^\\*\\*(User|Assistant|System)\\*\\*$").find(line.trim())
                if (roleMatch != null) {
                    if (currentRole != null) {
                        messages.add(ImportMessage(currentRole, content.toString().trim()))
                        content.clear()
                    }
                    currentRole = roleMatch.groupValues[1]
                } else if (currentRole != null) {
                    if (content.isNotEmpty() || line.isNotBlank()) {
                        if (content.isNotEmpty()) content.append("\n")
                        content.append(line.trimEnd())
                    }
                }
            }
            if (currentRole != null) {
                messages.add(ImportMessage(currentRole, content.toString().trim()))
            }

            if (messages.isEmpty()) {
                return Result.failure(Exception("没有找到消息"))
            }

            Result.success(ImportSessionData(title, skillName, avatar, messages))
        } catch (e: Exception) {
            Result.failure(Exception("解析会话失败: ${e.localizedMessage}"))
        }
    }
}
