package top.roomio.data.party.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GuestSessionRequest(
    val name: String,
    val avatar: String,
    val language: String,
)

@Serializable
data class ProfileResponseDto(
    val identity: IdentityDto,
)

@Serializable
data class CreateRoomRequest(
    val title: String,
)

@Serializable
data class StartStreamRequest(
    val url: String,
)

@Serializable
data class LeaveResponseDto(
    val roomClosed: Boolean,
)

@Serializable
data class PlaybackReportMessage(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val type: String = "playback.report",
    val streamSessionId: String,
    val positionMs: Long,
    val playing: Boolean,
)

@Serializable
data class SyncRequestMessage(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val type: String = "sync.request",
    val streamSessionId: String,
    val positionMs: Long,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class IdentityDto(
    val id: String,
    val name: String,
    val avatar: String,
    val language: String,
)

@Serializable
data class SessionDto(
    val identity: IdentityDto,
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class RoomDto(
    val id: String,
    val code: String,
    val title: String,
    val ownerIdentityId: String,
    val endedAt: String? = null,
)

@Serializable
data class MembershipDto(
    val id: String,
    val roomId: String,
    val identityId: String,
    val displayName: String,
    val avatar: String,
    val isOwner: Boolean,
    val leftAt: String? = null,
)

@Serializable
data class RoomAndMembershipDto(
    val room: RoomDto,
    val membership: MembershipDto,
)

@Serializable
data class RoomPreviewDto(
    val code: String,
    val title: String,
    val ownerName: String,
    val ownerPresent: Boolean,
    val participantCount: Int,
    val availability: String,
    val streamState: String,
)

@Serializable
data class MemberSummaryDto(
    val membershipId: String,
    val identityId: String,
    val displayName: String,
    val avatar: String,
    val isOwner: Boolean,
    val present: Boolean,
)

@Serializable
data class StreamDto(
    val sessionId: String,
    val revision: Long,
    val state: String,
    val url: String,
)

@Serializable
data class StreamResponseDto(
    val stream: StreamDto,
)

@Serializable
data class SnapshotDto(
    val room: RoomDto,
    val members: List<MemberSummaryDto>,
    val stream: StreamDto,
)

@Serializable
data class VoiceTokenDto(
    val token: String,
    val url: String,
)

@Serializable
data class RealtimeTicketDto(
    val ticket: String,
)

@Serializable
data class ErrorDto(
    val code: String,
    val message: String,
)

@Serializable
data class SnapshotMessageDto(
    val type: String,
    val room: RoomDto,
    val members: List<MemberSummaryDto>,
    val stream: StreamDto,
)

@Serializable
data class EventMessageDto(
    val type: String,
    val topic: String,
    val payload: JsonObject? = null,
)

@Serializable
data class SyncResultMessageDto(
    val type: String,
    val status: String,
    val targetPositionMs: Long,
)

@Serializable
data class ErrorMessageDto(
    val type: String,
    val code: String,
    val message: String,
)

@Serializable
data class TypeOnlyDto(
    val type: String,
)

// Durable event payloads.

@Serializable
data class MemberEventPayload(
    val member: MemberSummaryDto? = null,
)

@Serializable
data class OwnerPresencePayload(
    val present: Boolean = false,
)

@Serializable
data class StreamEventPayload(
    val streamSessionId: String = "",
    val revision: Long = 0,
    val url: String = "",
)
