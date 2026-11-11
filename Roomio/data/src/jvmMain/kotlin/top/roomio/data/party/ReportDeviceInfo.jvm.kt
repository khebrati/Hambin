package top.roomio.data.party

import java.util.Locale
import java.util.UUID

internal actual fun reportDeviceInfo(): Map<String, String> = mapOf(
    "platform" to "Desktop", "os" to System.getProperty("os.name"), "osVersion" to System.getProperty("os.version"),
    "architecture" to System.getProperty("os.arch"), "runtime" to System.getProperty("java.version"),
    "appVersion" to (System.getProperty("roomio.version") ?: "development"), "locale" to Locale.getDefault().toLanguageTag())
internal actual fun newReportId(): String = UUID.randomUUID().toString()
