package top.roomio.data.observability

import io.sentry.kotlin.multiplatform.Sentry

internal actual fun initializeSentry(environment: String) {
    Sentry.initWithPlatformOptions { options ->
        options.dsn = MonitoringConfig.sentryDsn
        options.environment = environment
        options.release = MonitoringConfig.release.ifEmpty { null }
        options.isSendDefaultPii = false
        options.maxBreadcrumbs = 0
        options.setBeforeSend { event, _ ->
            event.user = null
            event.serverName = null
            event.request = null
            event.message = null
            event.extras?.clear()
            event.breadcrumbs?.clear()
            event.exceptions?.forEach { it.value = it.type ?: "Application error" }
            event
        }
    }
}
