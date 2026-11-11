package top.roomio.data.party

import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSUUID
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.UIKit.UIDevice

internal actual fun reportDeviceInfo(): Map<String, String> = mapOf(
    "platform" to "iOS", "model" to UIDevice.currentDevice.model,
    "osVersion" to UIDevice.currentDevice.systemVersion,
    "appVersion" to (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")?.toString() ?: "unknown"),
    "appVersionCode" to (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion")?.toString() ?: "unknown"),
    "locale" to NSLocale.currentLocale.localeIdentifier)
internal actual fun newReportId(): String = NSUUID().UUIDString.lowercase()
