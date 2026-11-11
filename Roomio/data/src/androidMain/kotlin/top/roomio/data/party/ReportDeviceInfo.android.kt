package top.roomio.data.party

import android.os.Build
import java.util.Locale
import java.util.UUID

internal actual fun reportDeviceInfo(): Map<String, String> {
    val context = AppContextHolder.appContext
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val display = context.resources.displayMetrics
    return mapOf("platform" to "Android", "manufacturer" to Build.MANUFACTURER, "model" to Build.MODEL,
        "osVersion" to Build.VERSION.RELEASE, "sdk" to Build.VERSION.SDK_INT.toString(),
        "appVersion" to (info.versionName ?: "unknown"), "appVersionCode" to (if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()).toString(),
        "locale" to Locale.getDefault().toLanguageTag(), "screen" to "${display.widthPixels}x${display.heightPixels}", "density" to display.density.toString())
}
internal actual fun newReportId(): String = UUID.randomUUID().toString()
