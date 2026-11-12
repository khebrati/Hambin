package top.roomio.data.observability

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.sentry.kotlin.multiplatform.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import top.roomio.data.party.newReportId
import top.roomio.data.party.reportDeviceInfo

fun interface ProductAnalytics { fun track(event: String) }

/** Owns a bounded analytics queue. SDKs and transport never enter the domain/UI layers. */
@SingleIn(AppScope::class)
@Inject
class AppObservability(private val http: HttpClient, settings: Settings) : ProductAnalytics {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val events = Channel<String>(32)
    private val installId = settings.getStringOrNull("analytics.install.id") ?: newReportId().also {
        settings.putString("analytics.install.id", it)
    }
    private val environment = monitoringEnvironment()
    private val platform = reportDeviceInfo()["platform"] ?: "native"

    private val logWriter = object : LogWriter() {
        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            if (throwable != null && severity >= Severity.Error && MonitoringConfig.sentryDsn.isNotEmpty()) {
                Sentry.captureException(throwable)
            }
        }
    }
    init {
        if (MonitoringConfig.sentryDsn.isNotEmpty()) {
            initializeSentry(environment)
        }
        Logger.addLogWriter(logWriter)
        scope.launch {
            for (event in events) {
                // Usage analytics are best-effort; they never delay product requests.
                try {
                    withTimeout(5000) {
                        http.post("${MonitoringConfig.posthogHost}/i/v0/e/") {
                            contentType(ContentType.Application.Json)
                            setBody(buildJsonObject {
                                put("api_key", MonitoringConfig.posthogToken)
                                put("distinct_id", installId)
                                put("event", event)
                                put("properties", buildJsonObject {
                                    put("platform", platform)
                                    put("environment", environment)
                                    put("release", MonitoringConfig.release)
                                    put("\$process_person_profile", false)
                                })
                            })
                        }
                    }
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    if (!scope.coroutineContext[kotlinx.coroutines.Job]!!.isActive) throw cancel
                } catch (_: Exception) { /* Analytics failures must not become application failures. */ }
            }
        }
        track("app_opened")
    }
    override fun track(event: String) {
        if (MonitoringConfig.posthogToken.isNotEmpty()) events.trySend(event)
    }
    fun close() { Logger.setLogWriters(Logger.config.logWriterList.filterNot { it === logWriter }); events.close(); scope.cancel() }
}

internal expect fun monitoringEnvironment(): String

internal expect fun initializeSentry(environment: String)
