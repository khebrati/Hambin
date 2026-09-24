package top.roomio.data.party

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import top.roomio.data.party.dto.ErrorDto
import top.roomio.domain.party.PARTY_CODE_NETWORK
import top.roomio.domain.party.PartyException

internal suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
    if (status.isSuccess()) {
        return body()
    }
    throw toPartyException()
}

internal suspend fun HttpResponse.toPartyException(): PartyException {
    val error = runCatching { body<ErrorDto>() }.getOrNull()
    return PartyException(
        code = error?.code ?: "HTTP_${status.value}",
        message = error?.message ?: "Request failed",
    )
}

/** A stale or missing access token is worth one refresh-and-retry. */
internal fun PartyException.isAuthFailure(): Boolean =
    code == "INVALID_TOKEN" || code == "UNAUTHENTICATED" || code == "SESSION_EXPIRED"

/**
 * Normalizes any unexpected transport failure (DNS, connection, timeout) into a
 * stable [PartyException] so presentation can show an offline state instead of
 * crashing. Coroutine cancellation is always rethrown.
 */
internal fun Throwable.asPartyException(): PartyException = when (this) {
    is PartyException -> this
    is CancellationException -> throw this
    else -> PartyException(PARTY_CODE_NETWORK, message ?: "Network request failed")
}
