package top.roomio.data.party

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun initializeAppLogging() = AppSessionLog.install()

/** Complete process-session app log. Never captures system logs or HTTP bodies. */
@OptIn(ExperimentalTime::class)
object AppSessionLog : LogWriter() {
    val startedAt: String = Clock.System.now().toString()
    private val installed = MutableStateFlow(false)
    private class Entry(val line: String, val previous: Entry?)
    private val lines = MutableStateFlow(Entry("$startedAt Application started", null))
    fun install() {
        if (installed.compareAndSet(false, true)) Logger.addLogWriter(this)
    }
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val line = redactAppLog("${Clock.System.now()} $severity $tag: $message${throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()}")
        lines.update { Entry(line, it) }
    }
    fun snapshot(): String {
        val snapshot = mutableListOf<String>()
        var entry: Entry? = lines.value
        while (entry != null) { snapshot.add(entry.line); entry = entry.previous }
        return snapshot.asReversed().joinToString("\n")
    }
}

internal fun redactAppLog(value: String): String = value
    .replace(Regex("https?://[^\\s\\\"'<>]+", RegexOption.IGNORE_CASE), "[url]")
    .replace(Regex("Bearer\\s+[^\\s\\\"',}]+", RegexOption.IGNORE_CASE), "Bearer [redacted]")
    .replace(Regex("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"), "[token]")
    .replace(Regex("([\\\"']?(?:accessToken|refreshToken|authorization|password|credential|ticket|token)[\\\"']?\\s*[:=]\\s*)(\\\"[^\\\"]*\\\"|'[^']*'|[^\\s,;}]+)", RegexOption.IGNORE_CASE), "$1[redacted]")

internal expect fun reportDeviceInfo(): Map<String, String>
internal expect fun newReportId(): String
