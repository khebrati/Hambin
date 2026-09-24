package top.roomio.data.party

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.roomio.data.party.dto.ErrorMessageDto
import top.roomio.data.party.dto.EventMessageDto
import top.roomio.data.party.dto.MemberEventPayload
import top.roomio.data.party.dto.MemberSummaryDto
import top.roomio.data.party.dto.OwnerPresencePayload
import top.roomio.data.party.dto.SnapshotMessageDto
import top.roomio.data.party.dto.StreamEventPayload
import top.roomio.data.party.dto.SyncResultMessageDto
import top.roomio.data.party.dto.toDomain
import top.roomio.domain.party.Member
import top.roomio.domain.party.RealtimeEvent
import top.roomio.domain.party.SyncStatus

private val log = Logger.withTag("RealtimeParser")

internal fun parseRealtimeFrame(json: Json, text: String): RealtimeEvent {
    val element = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
    if (element == null) {
        log.w("unparseable frame: ${text.take(MAX_FRAME_LOG_CHARS)}")
        return RealtimeEvent.Ignored
    }
    val type = element["type"]?.jsonPrimitive?.contentOrNull
    if (type == null) {
        log.w("frame without type: ${text.take(MAX_FRAME_LOG_CHARS)}")
        return RealtimeEvent.Ignored
    }
    return when (type) {
        "snapshot" -> {
            log.i("snapshot frame (${text.length} chars)")
            RealtimeEvent.Snapshot(
                json.decodeFromJsonElement(SnapshotMessageDto.serializer(), element).toDomain(),
            )
        }
        "event" -> parseEvent(json, element, text)
        "sync.result" -> {
            val dto = json.decodeFromJsonElement(SyncResultMessageDto.serializer(), element)
            log.i("sync.result status=${dto.status} target=${dto.targetPositionMs}ms raw=${text.take(MAX_FRAME_LOG_CHARS)}")
            RealtimeEvent.SyncResult(dto.status.toSyncStatus(), dto.targetPositionMs)
        }
        "error" -> {
            val dto = json.decodeFromJsonElement(ErrorMessageDto.serializer(), element)
            log.e("server error code=${dto.code} message=${dto.message}")
            RealtimeEvent.Failure(dto.code, dto.message)
        }
        else -> {
            log.d("ignored frame type=$type")
            RealtimeEvent.Ignored
        }
    }
}

private fun parseEvent(json: Json, element: JsonObject, text: String): RealtimeEvent {
    val dto = runCatching {
        json.decodeFromJsonElement(EventMessageDto.serializer(), element)
    }.getOrNull()
    if (dto == null) {
        log.w("unparseable event frame: ${text.take(MAX_FRAME_LOG_CHARS)}")
        return RealtimeEvent.Ignored
    }
    val payload = dto.payload
    return when (dto.topic) {
        "member.joined" -> memberEvent(json, payload) { RealtimeEvent.MemberJoined(it) }
        "member.returned" -> memberEvent(json, payload) { RealtimeEvent.MemberReturned(it) }
        "member.left" -> RealtimeEvent.MemberLeft(memberEventPayload(json, payload)?.membershipId.orEmpty())
        "member.absent" -> memberEvent(json, payload) { RealtimeEvent.MemberAbsent(it) }
        "owner.presence" -> RealtimeEvent.OwnerPresence(ownerPresence(json, payload))
        "stream.started" -> streamEvent(json, payload) {
            RealtimeEvent.StreamStarted(it.streamSessionId, it.revision, it.url)
        }
        "stream.aborted" -> streamEvent(json, payload) {
            RealtimeEvent.StreamAborted(it.streamSessionId, it.revision)
        }
        "room.closed" -> RealtimeEvent.RoomClosed
        else -> RealtimeEvent.Ignored
    }
}

private fun memberEvent(
    json: Json,
    payload: JsonObject?,
    factory: (Member) -> RealtimeEvent,
): RealtimeEvent {
    val member = memberEventPayload(json, payload)?.toDomain() ?: return RealtimeEvent.Ignored
    return factory(member)
}

private fun memberEventPayload(json: Json, payload: JsonObject?): MemberSummaryDto? {
    if (payload == null) return null
    return runCatching {
        json.decodeFromJsonElement(MemberEventPayload.serializer(), payload).member
    }.getOrNull()
}

private fun ownerPresence(json: Json, payload: JsonObject?): Boolean {
    if (payload == null) return false
    return runCatching {
        json.decodeFromJsonElement(OwnerPresencePayload.serializer(), payload).present
    }.getOrDefault(false)
}

private fun streamEvent(
    json: Json,
    payload: JsonObject?,
    factory: (StreamEventPayload) -> RealtimeEvent,
): RealtimeEvent {
    if (payload == null) return RealtimeEvent.Ignored
    return runCatching {
        factory(json.decodeFromJsonElement(StreamEventPayload.serializer(), payload))
    }.getOrElse { RealtimeEvent.Ignored }
}

private fun String.toSyncStatus(): SyncStatus = when (uppercase()) {
    "APPLIED" -> SyncStatus.APPLIED
    "ALREADY_LEADING" -> SyncStatus.ALREADY_LEADING
    else -> SyncStatus.UNAVAILABLE
}

private const val MAX_FRAME_LOG_CHARS = 200
