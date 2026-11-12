package top.roomio.data.observability

import io.sentry.kotlin.multiplatform.Sentry
import kotlinx.cinterop.ExperimentalForeignApi
import cocoapods.Sentry.SentryException

@OptIn(ExperimentalForeignApi::class)
internal actual fun initializeSentry(environment: String) {
    Sentry.initWithPlatformOptions { options ->
        if (options == null) return@initWithPlatformOptions
        options.dsn = MonitoringConfig.sentryDsn
        options.environment = environment
        options.releaseName = MonitoringConfig.release.ifEmpty { null }
        options.sendDefaultPii = false
        options.attachScreenshot = false
        options.attachViewHierarchy = false
        options.enableCaptureFailedRequests = false
        options.maxBreadcrumbs = 0u
        options.beforeSend = { event ->
            event?.apply {
                user = null
                request = null
                message = null
                extra = null
                breadcrumbs = emptyList<Any>()
                exceptions?.forEach { (it as? SentryException)?.apply { value = type } }
            }
        }
    }
}
