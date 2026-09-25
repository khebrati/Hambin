package top.roomio.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import top.roomio.app.home.HomeViewModel
import top.roomio.app.manager.PartyManagerViewModel
import top.roomio.app.navigation.RoomioNavigation
import top.roomio.app.preview.RoomPreviewViewModel
import top.roomio.app.profile.SettingsViewModel
import top.roomio.app.room.RoomViewModel
import top.roomio.app.theme.AppTheme
import top.roomio.domain.party.GuestIdentity
import top.roomio.domain.party.GuestSession
import top.roomio.domain.party.Member
import top.roomio.domain.party.RealtimeClient
import top.roomio.domain.party.RealtimeEvent
import top.roomio.domain.party.RealtimeSession
import top.roomio.domain.party.Room
import top.roomio.domain.party.RoomAvailability
import top.roomio.domain.party.RoomPreview
import top.roomio.domain.party.RoomRepository
import top.roomio.domain.party.RoomSnapshot
import top.roomio.domain.party.RoomSession
import top.roomio.domain.party.SessionKeeper
import top.roomio.domain.party.SessionRepository
import top.roomio.domain.party.StreamInfo
import top.roomio.domain.party.StreamState
import top.roomio.domain.party.VoiceClient
import top.roomio.domain.party.VoiceEvent
import top.roomio.domain.party.VoiceSession
import top.roomio.domain.party.VoiceToken
import top.roomio.domain.profile.ProfileAvatarId
import top.roomio.domain.profile.ProfileRepository
import top.roomio.domain.profile.UserProfile

@DependencyGraph
internal interface PresentationGraph {
    val homeViewModel: HomeViewModel
    val settingsViewModel: SettingsViewModel
    val partyManagerViewModelFactory: PartyManagerViewModel.Factory
    val roomPreviewViewModelFactory: RoomPreviewViewModel.Factory
    val roomViewModelFactory: RoomViewModel.Factory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides profileRepository: ProfileRepository,
            @Provides sessionRepository: SessionRepository,
            @Provides roomRepository: RoomRepository,
            @Provides realtimeClient: RealtimeClient,
            @Provides voiceClient: VoiceClient,
            @Provides sessionKeeper: SessionKeeper,
        ): PresentationGraph
    }
}

@Preview
@Composable
private fun AppPreview() = RoomioUi(
    profileRepository = PreviewProfileRepository,
    sessionRepository = PreviewSessionRepository,
    roomRepository = PreviewRoomRepository,
    realtimeClient = PreviewRealtimeClient,
    voiceClient = PreviewVoiceClient,
    sessionKeeper = PreviewSessionKeeper,
)

@Composable
fun RoomioUi(
    profileRepository: ProfileRepository,
    sessionRepository: SessionRepository = PreviewSessionRepository,
    roomRepository: RoomRepository = PreviewRoomRepository,
    realtimeClient: RealtimeClient = PreviewRealtimeClient,
    voiceClient: VoiceClient = PreviewVoiceClient,
    sessionKeeper: SessionKeeper = PreviewSessionKeeper,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) {
    val presentationGraph = remember(profileRepository, sessionRepository, roomRepository, realtimeClient, voiceClient, sessionKeeper) {
        createGraphFactory<PresentationGraph.Factory>().create(
            profileRepository,
            sessionRepository,
            roomRepository,
            realtimeClient,
            voiceClient,
            sessionKeeper,
        )
    }

    AppTheme(onThemeChanged) {
        RoomioNavigation(
            presentationGraph = presentationGraph,
            modifier = Modifier,
        )
    }
}

private val previewProfile = UserProfile("Nika", ProfileAvatarId.COMET)

private object PreviewProfileRepository : ProfileRepository {
    override fun observeProfile() = flowOf(previewProfile)
    override fun currentProfile() = previewProfile
    override fun saveProfile(profile: UserProfile) = Result.success(Unit)
}

private object PreviewSessionRepository : SessionRepository {
    private val session = GuestSession(
        identity = GuestIdentity("preview", "Nika", "COMET", "en"),
        accessToken = "preview",
        refreshToken = "preview",
    )

    override suspend fun ensureSession(name: String, avatar: String, language: String) = session
    override suspend fun currentSession() = session
    override suspend fun refresh() = session
    override suspend fun updateProfile(name: String, avatar: String, language: String) = session.identity
}

private object PreviewRoomRepository : RoomRepository {
    private val room = Room("preview", "MOON-42", "Friday night screening", "preview", false)
    override suspend fun createRoom(title: String) = room
    override suspend fun preview(code: String) = RoomPreview(
        code = code,
        title = "Mira's late show",
        ownerName = "Mira",
        ownerPresent = true,
        participantCount = 3,
        availability = RoomAvailability.OPEN,
        streamState = StreamState.EMPTY,
    )
    override suspend fun join(code: String) = room
    override suspend fun snapshot(roomId: String) = RoomSnapshot(
        room = room,
        members = previewMembers,
        stream = StreamInfo("preview", 0, StreamState.EMPTY, ""),
    )
    override suspend fun leave(roomId: String) = false
    override suspend fun startStream(roomId: String, url: String) = StreamInfo("preview", 1, StreamState.ACTIVE, url)
    override suspend fun abortStream(roomId: String) = StreamInfo("preview", 2, StreamState.ABORTED, "")
    override suspend fun voiceToken(roomId: String) = VoiceToken("preview", "wss://preview")
}

private object PreviewRealtimeClient : RealtimeClient {
    override suspend fun connect(roomId: String): RealtimeSession = object : RealtimeSession {
        override val events: Flow<RealtimeEvent> = flowOf(RealtimeEvent.Ignored)
        override suspend fun reportPlayback(streamSessionId: String, positionMs: Long, playing: Boolean) = Unit
        override suspend fun requestSync(streamSessionId: String, positionMs: Long, requestId: String) = Unit
        override suspend fun close() = Unit
    }
}

private object PreviewVoiceClient : VoiceClient {
    override suspend fun connect(token: VoiceToken): VoiceSession = object : VoiceSession {
        override val events: Flow<VoiceEvent> = flowOf(VoiceEvent.Connected)
        override fun setMicrophoneEnabled(enabled: Boolean) = Unit
        override suspend fun disconnect() = Unit
    }
}

private object PreviewSessionKeeper : SessionKeeper {
    override fun start(room: RoomSession) = Unit
    override fun stop() = Unit
}

private val previewMembers = listOf(
    Member("m1", "preview", "Mira", "MINT", isOwner = true, present = true),
)
