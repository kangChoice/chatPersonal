package com.needai.chat.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object AvatarUtils {

    private const val AVATAR_DIR = "skills_avatars"
    private const val DEFAULT_AVATAR = "default.png"
    private const val ASSETS_DEFAULT = "skillsBase.png"

    /** 获取头像存储目录 */
    fun getAvatarDir(context: Context): File {
        return File(context.filesDir, AVATAR_DIR).also { it.mkdirs() }
    }

    /** 获取默认头像文件路径 */
    fun getDefaultAvatarPath(context: Context): String {
        return File(getAvatarDir(context), DEFAULT_AVATAR).absolutePath
    }

    /** 获取指定 skill 的头像文件路径 */
    fun getSkillAvatarPath(context: Context, skillId: String): String {
        return File(getAvatarDir(context), "${skillId}.png").absolutePath
    }

    /** 初始化：从 assets 复制默认头像 */
    fun initDefaultAvatar(context: Context) {
        val dest = File(getAvatarDir(context), DEFAULT_AVATAR)
        if (dest.exists()) return
        try {
            context.assets.open(ASSETS_DEFAULT).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            FileLogger.e("AvatarUtils", "复制默认头像失败", e)
        }
    }

    /** 保存头像图片到本地，返回文件路径 */
    fun saveAvatar(context: Context, skillId: String, uri: Uri): String? {
        return try {
            val path = getSkillAvatarPath(context, skillId)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(path).use { output ->
                    input.copyTo(output)
                }
            }
            path
        } catch (e: Exception) {
            FileLogger.e("AvatarUtils", "保存头像失败: skillId=$skillId", e)
            null
        }
    }

    /** 删除指定 skill 的头像文件（内置角色不删除） */
    fun deleteAvatar(context: Context, skillId: String, isBuiltin: Boolean) {
        if (isBuiltin) return
        val file = File(getAvatarDir(context), "${skillId}.png")
        if (file.exists()) file.delete()
    }

    /** 获取要展示的头像路径：有自定义头像则返回，否则返回默认头像路径 */
    fun getDisplayAvatarPath(context: Context, avatarPath: String?): String {
        if (!avatarPath.isNullOrBlank()) {
            val f = File(avatarPath)
            if (f.exists()) return avatarPath
        }
        return getDefaultAvatarPath(context)
    }

    /** 判断是否为默认头像 */
    fun isDefaultAvatar(path: String?): Boolean {
        return path.isNullOrBlank() || !File(path).exists()
    }
}
