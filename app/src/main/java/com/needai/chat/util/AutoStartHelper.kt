package com.needai.chat.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

object AutoStartHelper {

    fun needsAutoStartGuide(): Boolean = isChineseRom()

    fun openAutoStartSettings(context: Context): Boolean {
        val intents = listOfNotNull(
            // 小米 MIUI
            Intent().setComponent(ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )),
            // 华为 HarmonyOS / EMUI
            Intent().setComponent(ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )),
            // OPPO ColorOS
            Intent().setComponent(ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )),
            // vivo OriginOS
            Intent().setComponent(ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )),
            // 通用备用
            Intent().apply {
                action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                data = android.net.Uri.parse("package:${context.packageName}")
            },
        )
        for (intent in intents) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    private fun isChineseRom(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("huawei") ||
                manufacturer.contains("honor") ||
                manufacturer.contains("oppo") ||
                manufacturer.contains("vivo") ||
                manufacturer.contains("oneplus") ||
                manufacturer.contains("realme") ||
                manufacturer.contains("meizu") ||
                brand.contains("xiaomi") ||
                brand.contains("huawei") ||
                brand.contains("honor") ||
                brand.contains("oppo") ||
                brand.contains("vivo") ||
                brand.contains("oneplus") ||
                brand.contains("realme") ||
                brand.contains("meizu")
    }
}
