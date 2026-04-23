package com.acdcmaia.callguard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

object AutoStartHelper {

    private data class Entry(val manufacturer: String, val intent: Intent)

    private val entries = listOf(
        Entry("xiaomi", Intent().setComponent(ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        ))),
        Entry("samsung", Intent().setComponent(ComponentName(
            "com.samsung.android.lool",
            "com.samsung.android.sm.ui.battery.BatteryActivity"
        ))),
        Entry("huawei", Intent().setComponent(ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.optimize.process.ProtectActivity"
        ))),
        Entry("honor", Intent().setComponent(ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.optimize.process.ProtectActivity"
        ))),
        Entry("oppo", Intent().setComponent(ComponentName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        ))),
        Entry("oppo", Intent().setComponent(ComponentName(
            "com.oppo.safe",
            "com.oppo.safe.permission.startup.StartupAppListActivity"
        ))),
        Entry("realme", Intent().setComponent(ComponentName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        ))),
        Entry("vivo", Intent().setComponent(ComponentName(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
        ))),
        Entry("vivo", Intent().setComponent(ComponentName(
            "com.vivo.permissionmanager",
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
        ))),
        Entry("oneplus", Intent().setComponent(ComponentName(
            "com.oneplus.security",
            "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
        ))),
        Entry("asus", Intent().setComponent(ComponentName(
            "com.asus.mobilemanager",
            "com.asus.mobilemanager.entry.FunctionActivity"
        ))),
        Entry("meizu", Intent().setComponent(ComponentName(
            "com.meizu.safe",
            "com.meizu.safe.permission.SmartPermissionActivity"
        ))),
        Entry("nokia", Intent().setComponent(ComponentName(
            "com.evenwell.powersaving.g3",
            "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
        )))
    )

    private fun findIntent(context: Context): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return entries
            .filter { it.manufacturer == manufacturer }
            .map { it.intent }
            .firstOrNull { context.packageManager.resolveActivity(it, 0) != null }
    }

    fun canOpen(context: Context): Boolean = findIntent(context) != null

    fun open(context: Context): Boolean {
        val intent = findIntent(context) ?: return false
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
