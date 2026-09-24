package top.roomio.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import top.roomio.domain.party.GuestIdentity
import top.roomio.domain.party.GuestSession
import top.roomio.domain.party.RealtimeClient
import top.roomio.domain.party.RealtimeSession
import top.roomio.domain.party.Room
import top.roomio.domain.party.RoomPreview
import top.roomio.domain.party.RoomRepository
import top.roomio.domain.party.RoomSnapshot
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.party.SessionKeeper
import top.roomio.domain.party.StreamInfo
import top.roomio.domain.party.StreamState
import top.roomio.domain.party.VoiceClient
import top.roomio.domain.party.VoiceEvent
import top.roomio.domain.party.VoiceSession
import top.roomio.domain.party.VoiceToken

internal object testSessionRepository : SessionRepository {
    private val identity = GuestIdentity("test", "Nika", "COMET", "en")
    private val session = GuestSession(identity, "access", "refresh")
    override suspend fun ensureSession(name: String, avatar: String, language: String) = session
    override suspend fun currentSession() = session
    override suspend fun refresh() = session
    override suspend fun updateProfile(name: String, avatar: String, language: String) = identity
}

/** Records [ensureSession] calls so tests can assert identity synchronization. */
internal class RecordingSessionRepository : SessionRepository {
    val ensureCalls = mutableListOf<Pair<String, String>>()
    private var session = GuestSession(
        identity = GuestIdentity("test", "Nika", "COMET", "en"),
        accessToken = "access",
        refreshToken = "refresh",
    )

    override suspend fun ensureSession(name: String, avatar: String, language: String): GuestSession {
        ensureCalls += name to avatar
        session = session.copy(identity = session.identity.copy(name = name, avatar = avatar, language = language))
        return session
    }

    override suspend fun currentSession() = session

    override suspend fun refresh() = session

    override suspend fun updateProfile(name: String, avatar: String, language: String): GuestIdentity {
        session = session.copy(identity = session.identity.copy(name = name, avatar = avatar, language = language))
        return session.identity
    }
}

internal object testRoomRepository : RoomRepository {
    private val room = Room("room", "MOON-42", "Friday night screening", "test", false)
    override suspend fun createRoom(title: String) = room
    override suspend fun preview(code: String) = RoomPreview(code, "Title", "Nika", true, 1, top.roomio.domain.party.RoomAvailability.OPEN, StreamState.EMPTY)
    override suspend fun join(code: String) = room
    override suspend fun snapshot(roomId: String) = RoomSnapshot(room, emptyList(), StreamInfo("", 0, StreamState.EMPTY, ""))
    override suspend fun leave(roomId: String) = false
    override suspend fun startStream(roomId: String, url: String) = StreamInfo("session", 1, StreamState.ACTIVE, url)
    override suspend fun abortStream(roomId: String) = StreamInfo("session", 2, StreamState.ABORTED, "")
    override suspend fun voiceToken(roomId: String) = VoiceToken("token", "wss://example")
}

internal object testRealtimeClient : RealtimeClient {
    override suspend fun connect(roomId: String): RealtimeSession = object : RealtimeSession {
        override val events: Flow<top.roomio.domain.party.RealtimeEvent> = emptyFlow()
        override suspend fun reportPlayback(streamSessionId: String, positionMs: Long, playing: Boolean) = Unit
        override suspend fun requestSync(streamSessionId: String, positionMs: Long) = Unit
        override suspend fun close() = Unit
    }
}

internal object testVoiceClient : VoiceClient {
    override suspend fun connect(token: VoiceToken): VoiceSession = object : VoiceSession {
        override val events: Flow<VoiceEvent> = emptyFlow()
        override fun setMicrophoneEnabled(enabled: Boolean) = Unit
        override suspend fun disconnect() = Unit
    }
}

internal object testSessionKeeper : SessionKeeper {
    override fun start(voiceActive: Boolean) = Unit
    override fun stop() = Unit
}
