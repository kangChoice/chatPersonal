package com.needai.chat.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

object AvatarUtils {

    private const val AVATAR_DIR = "skills_avatars"
    private const val USER_AVATAR_DIR = "user_avatar"
    private const val DEFAULT_AVATAR = "default.png"
    private const val ASSETS_DEFAULT = "skillsBase.png"
    private const val DEFAULT_USER_AVATAR = "default_user.png"
    private const val ASSETS_USER_DEFAULT = "normal_user.jpg"

    /** 获取角色头像存储目录（用户可见的外部存储） */
    fun getAvatarDir(context: Context): File {
        return File(context.getExternalFilesDir(null), AVATAR_DIR).also { it.mkdirs() }
    }

    /** 获取用户本人头像存储目录 */
    fun getUserAvatarDir(context: Context): File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), USER_AVATAR_DIR).also { it.mkdirs() }
    }

    /** 获取用户本人头像路径 */
    fun getUserAvatarPath(context: Context): String {
        return File(getUserAvatarDir(context), "user_avatar.png").absolutePath
    }

    /** 获取默认头像文件路径 */
    fun getDefaultAvatarPath(context: Context): String {
        return File(getAvatarDir(context), DEFAULT_AVATAR).absolutePath
    }

    /** 获取指定 skill 的头像文件路径 */
    fun getSkillAvatarPath(context: Context, skillId: String): String {
        return File(getAvatarDir(context), "${skillId}.png").absolutePath
    }

    /** 获取默认用户本人头像文件路径（首次自动从 assets 复制） */
    fun getDefaultUserAvatarPath(context: Context): String {
        val dest = File(getUserAvatarDir(context), DEFAULT_USER_AVATAR)
        if (!dest.exists()) {
            try {
                context.assets.open(ASSETS_USER_DEFAULT).use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                FileLogger.e("AvatarUtils", "复制默认用户头像失败", e)
            }
        }
        return dest.absolutePath
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

    /** 保存角色头像图片到本地，返回文件路径 */
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
            FileLogger.e("AvatarUtils", "保存角色头像失败: skillId=$skillId", e)
            null
        }
    }

    /** 保存用户本人头像，返回文件路径 */
    fun saveUserAvatar(context: Context, uri: Uri): String? {
        return try {
            val path = getUserAvatarPath(context)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(path).use { output ->
                    input.copyTo(output)
                }
            }
            path
        } catch (e: Exception) {
            FileLogger.e("AvatarUtils", "保存用户头像失败", e)
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
