package top.roomio.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import top.roomio.app.room.RoomAction
import top.roomio.app.room.RoomEffect
import top.roomio.app.room.RoomViewModel
import top.roomio.app.room.VoiceConnectionState
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
import top.roomio.domain.party.SessionAction
import top.roomio.domain.party.SessionKeeper
import top.roomio.domain.party.StreamInfo
import top.roomio.domain.party.StreamState
import top.roomio.domain.party.VoiceClient
import top.roomio.domain.party.VoiceEvent
import top.roomio.domain.party.VoiceSession
import top.roomio.domain.party.VoiceToken

@OptIn(ExperimentalCoroutinesApi::class)
class RoomViewModelPresenceTest {

    private val room = Room("room", "MOON-42", "Friday night screening", "owner-1", false)
    private val owner = Member("m1", "owner-1", "Mira", "MINT", isOwner = true, present = true)
    private val guest = Member("m2", "guest-1", "Ellis", "SUNNY", isOwner = false, present = true)

    @Test
    fun ownerDeviceAlwaysReportsOwnerPresent() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = snapshotRepository(RoomSnapshot(room, emptyList(), emptyStream()))
            val viewModel = RoomViewModel("room", asOwner = true, testSessionRepository, repository, testRealtimeClient, testVoiceClient, testSessionKeeper)

            assertTrue(viewModel.state.value.model.ownerPresent)
            assertTrue(viewModel.state.value.isOwner)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun guestSeesOwnerAwayWhenSnapshotReportsAbsent() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = snapshotRepository(RoomSnapshot(room, listOf(owner.copy(present = false), guest), emptyStream()))
            val viewModel = RoomViewModel("room", asOwner = false, testSessionRepository, repository, testRealtimeClient, testVoiceClient, testSessionKeeper)

            assertFalse(viewModel.state.value.model.ownerPresent)
            assertFalse(viewModel.state.value.isOwner)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun absentOwnerRemainsInRoomMarkedAway() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val events = MutableSharedFlow<RealtimeEvent>()
            val repository = snapshotRepository(RoomSnapshot(room, listOf(owner, guest), emptyStream()))
            val viewModel = RoomViewModel("room", asOwner = false, testSessionRepository, repository, emittingRealtimeClient(events), testVoiceClient, testSessionKeeper)

            assertTrue(viewModel.state.value.model.ownerPresent)

            events.emit(RealtimeEvent.MemberAbsent(owner.copy(present = false)))
            testScheduler.runCurrent()

            assertFalse(viewModel.state.value.model.ownerPresent)
            val host = viewModel.state.value.model.participants.first { it.isHost }
            assertFalse(host.isPresent)

            events.emit(RealtimeEvent.MemberReturned(owner))
            testScheduler.runCurrent()

            assertTrue(viewModel.state.value.model.ownerPresent)
            assertTrue(viewModel.state.value.model.participants.first { it.isHost }.isPresent)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun voiceReconnectsAfterSessionDrop() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val voice = RecordingVoiceClient()
            val repository = snapshotRepository(RoomSnapshot(room, listOf(owner, guest), emptyStream()))
            val viewModel = RoomViewModel("room", asOwner = true, testSessionRepository, repository, testRealtimeClient, voice, testSessionKeeper)

            viewModel.onAction(RoomAction.JoinVoiceClicked)
            testScheduler.runCurrent()

            assertEquals(1, voice.connects)
            // Voice is opt-in and starts muted.
            assertEquals(listOf(false), voice.micStates)

            // The first session drops; the loop must fetch a fresh token and reconnect.
            testScheduler.advanceTimeBy(2_000L)
            testScheduler.runCurrent()

            assertEquals(2, voice.connects)
            assertEquals(listOf(false, false), voice.micStates)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sessionKeeperTracksRoomAndVoice() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val keeper = RecordingSessionKeeper()
            val voice = RecordingVoiceClient()
            val repository = snapshotRepository(RoomSnapshot(room, listOf(owner, guest), emptyStream()))
            val viewModel = RoomViewModel("room", asOwner = true, testSessionRepository, repository, testRealtimeClient, voice, keeper)
            testScheduler.runCurrent()

            // Entering the room starts the background session without voice.
            assertEquals(listOf(false), keeper.started.map { it.voiceActive })
            assertEquals("room", keeper.started.first().roomId)
            assertTrue(keeper.started.first().asOwner)

            viewModel.onAction(RoomAction.JoinVoiceClicked)
            testScheduler.runCurrent()

            // Joining voice upgrades the session so the microphone stays alive.
            assertEquals(listOf(false, true), keeper.started.map { it.voiceActive }.take(2))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun notificationUnmuteEnablesMicrophone() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val keeper = RecordingSessionKeeper()
            val voice = RecordingVoiceClient()
            val repository = snapshotRepository(RoomSnapshot(room, listOf(owner, guest), emptyStream()))
            val viewModel = RoomViewModel("room", asOwner = true, testSessionRepository, repository, testRealtimeClient, voice, keeper)

            viewModel.onAction(RoomAction.JoinVoiceClicked)
            testScheduler.runCurrent()
            // The first session drops instantly; advance to the reconnected one.
            testScheduler.advanceTimeBy(2_000L)
            testScheduler.runCurrent()
            assertTrue(viewModel.state.value.micMuted)

            viewModel.onAction(RoomAction.ToggleMicClicked)
            testScheduler.runCurrent()
            assertFalse(viewModel.state.value.micMuted)
            assertEquals(true, voice.micStates.last())

            keeper.emit(SessionAction.MuteVoice)
            testScheduler.runCurrent()

            assertTrue(viewModel.state.value.micMuted)
            assertEquals(false, voice.micStates.last())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun notificationLeaveEndsVoiceAndRoom() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val keeper = RecordingSessionKeeper()
            val voice = RecordingVoiceClient()
            val repository = snapshotRepository(RoomSnapshot(room, listOf(owner, guest), emptyStream()))
            val viewModel = RoomViewModel("room", asOwner = true, testSessionRepository, repository, testRealtimeClient, voice, keeper)
            val effects = mutableListOf<RoomEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.effects.collect { effects += it }
            }

            viewModel.onAction(RoomAction.JoinVoiceClicked)
            testScheduler.runCurrent()
            testScheduler.advanceTimeBy(2_000L)
            testScheduler.runCurrent()

            keeper.emit(SessionAction.Leave)
            testScheduler.runCurrent()

            assertEquals(VoiceConnectionState.IDLE, viewModel.state.value.voiceState)
            assertTrue(keeper.stops >= 1)
            assertTrue(effects.contains(RoomEffect.Left))
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun emptyStream() = StreamInfo("", 0, StreamState.EMPTY, "")

    private fun snapshotRepository(snapshot: RoomSnapshot): RoomRepository = object : RoomRepository {
        override suspend fun createRoom(title: String) = room
        override suspend fun preview(code: String) = RoomPreview(code, "Title", "Mira", true, 1, RoomAvailability.OPEN, StreamState.EMPTY)
        override suspend fun join(code: String) = room
        override suspend fun snapshot(roomId: String) = snapshot
        override suspend fun leave(roomId: String) = false
        override suspend fun startStream(roomId: String, url: String) = StreamInfo("s", 1, StreamState.ACTIVE, url)
        override suspend fun abortStream(roomId: String) = StreamInfo("s", 2, StreamState.ABORTED, "")
        override suspend fun voiceToken(roomId: String) = VoiceToken("t", "wss://x")
    }

    private fun emittingRealtimeClient(events: MutableSharedFlow<RealtimeEvent>): RealtimeClient = object : RealtimeClient {
        override suspend fun connect(roomId: String): RealtimeSession = object : RealtimeSession {
            override val events: Flow<RealtimeEvent> = events
            override suspend fun reportPlayback(streamSessionId: String, positionMs: Long, playing: Boolean) = Unit
            override suspend fun requestSync(streamSessionId: String, positionMs: Long) = Unit
            override suspend fun close() = Unit
        }
    }

    private class RecordingSessionKeeper : SessionKeeper {
        val started = mutableListOf<RoomSession>()
        var stops = 0
        private val mutableActions = MutableSharedFlow<SessionAction>(extraBufferCapacity = 8)
        override val actions: Flow<SessionAction> = mutableActions

        override fun start(room: RoomSession) {
            started += room
        }

        override fun stop() {
            stops++
        }

        suspend fun emit(action: SessionAction) {
            mutableActions.emit(action)
        }
    }

    private class RecordingVoiceClient : VoiceClient {
        var connects = 0
        val micStates = mutableListOf<Boolean>()

        override suspend fun connect(token: VoiceToken): VoiceSession {
            connects++
            val index = connects
            return object : VoiceSession {
                // The first session ends immediately (a dropped call); later
                // sessions stay open so the reconnect loop does not spin.
                override val events: Flow<VoiceEvent> =
                    if (index == 1) emptyFlow() else flow { awaitCancellation() }

                override fun setMicrophoneEnabled(enabled: Boolean) {
                    micStates += enabled
                }

                override suspend fun disconnect() = Unit
            }
        }
    }
}
