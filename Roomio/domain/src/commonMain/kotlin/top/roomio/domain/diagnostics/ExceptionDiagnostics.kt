package top.roomio.domain.diagnostics

/** Pure formatting policy shared by native logging adapters. No payloads are inspected. */
fun handledExceptionDetails(operation: String, error: Throwable): String = redactExceptionDetails(
    "exception.handled operation=$operation\n${error.stackTraceToString()}",
)

fun redactExceptionDetails(value: String): String = value
    .replace(Regex("(?:https?|wss?|content|file|blob):[^\\s\\\"'<>]+", RegexOption.IGNORE_CASE), "[url]")
    .replace(Regex("Bearer\\s+[^\\s\\\"',}]+", RegexOption.IGNORE_CASE), "Bearer [redacted]")
    .replace(Regex("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"), "[token]")
    .replace(Regex("([\\\"']?(?:accessToken|refreshToken|authorization|password|credential|ticket|token)[\\\"']?\\s*[:=]\\s*)(\\\"[^\\\"]*\\\"|'[^']*'|[^\\s,;}]+)", RegexOption.IGNORE_CASE), "$1[redacted]")
