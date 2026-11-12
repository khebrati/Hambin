package top.roomio.data.observability

internal actual fun monitoringEnvironment(): String = if (kotlin.native.Platform.isDebugBinary) "development" else "production"
