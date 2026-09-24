package top.roomio.domain.party

/** A durable anonymous identity created by the backend. */
data class GuestIdentity(
    val id: String,
    val name: String,
    val avatar: String,
    val language: String,
)

/** A guest identity together with its rotated credential pair. */
data class GuestSession(
    val identity: GuestIdentity,
    val accessToken: String,
    val refreshToken: String,
)

/** A watch party room. */
data class Room(
    val id: String,
    val code: String,
    val title: String,
    val ownerIdentityId: String,
    val ended: Boolean,
)

/** Availability of a room before joining. */
enum class RoomAvailability { OPEN, FULL, ENDED }

/** Authoritative shared stream state. */
enum class StreamState { EMPTY, ACTIVE, ABORTED }

/** The pre-join room summary. */
data class RoomPreview(
    val code: String,
    val title: String,
    val ownerName: String,
    val ownerPresent: Boolean,
    val participantCount: Int,
    val availability: RoomAvailability,
    val streamState: StreamState,
)

/** A member currently seated in a room. */
data class Member(
    val membershipId: String,
    val identityId: String,
    val displayName: String,
    val avatar: String,
    val isOwner: Boolean,
    val present: Boolean,
)

/** Shared stream metadata. Video bytes are never proxied by the backend. */
data class StreamInfo(
    val sessionId: String,
    val revision: Long,
    val state: StreamState,
    val url: String,
)

/** A room's full state: the room, its members, and the shared stream. */
data class RoomSnapshot(
    val room: Room,
    val members: List<Member>,
    val stream: StreamInfo,
)

/** A room-scoped LiveKit join credential. */
data class VoiceToken(
    val token: String,
    val url: String,
)
