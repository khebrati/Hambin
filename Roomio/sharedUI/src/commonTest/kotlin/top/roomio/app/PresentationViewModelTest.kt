package top.roomio.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import top.roomio.app.manager.ManagerMode
import top.roomio.app.manager.PartyManagerAction
import top.roomio.app.manager.PartyManagerViewModel
import top.roomio.app.profile.ProfileAvatar
import top.roomio.app.profile.SettingsAction
import top.roomio.app.profile.SettingsViewModel
import top.roomio.app.room.InviteCopyState
import top.roomio.app.room.InviteCopyTarget
import top.roomio.app.room.RoomAction
import top.roomio.app.room.RoomPlaybackState
import top.roomio.app.room.RoomViewModel
import top.roomio.app.room.ownerRoomModel
import top.roomio.app.room.player.VideoPlayerState
import top.roomio.app.room.player.VideoPlayerStatus
import top.roomio.domain.profile.ProfileAvatarId
import top.roomio.domain.profile.ProfileRepository
import top.roomio.domain.profile.UserProfile

class PresentationViewModelTest {
    @Test
    fun settingsStateDerivesValidationFromActions() {
        val viewModel = SettingsViewModel(
            TestProfileRepository(),
        )

        viewModel.onAction(SettingsAction.NameChanged("   "))

        assertEquals("", viewModel.state.value.trimmedName)
        assertTrue(viewModel.state.value.isNameError)
        assertFalse(viewModel.state.value.canSave)

        viewModel.onAction(SettingsAction.AvatarSelected(ProfileAvatar.BERRY))
        viewModel.onAction(SettingsAction.NameChanged("  Mira  "))

        assertEquals("Mira", viewModel.state.value.trimmedName)
        assertEquals(ProfileAvatar.BERRY, viewModel.state.value.draftAvatar)
        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun partyManagerStateNormalizesCodeAndRepresentsRejectedPreview() {
        val viewModel = PartyManagerViewModel(ManagerMode.JOIN)

        viewModel.onAction(PartyManagerAction.CodeChanged(" moon-42 "))

        assertEquals(" MOON-42 ", viewModel.state.value.code)
        assertEquals("MOON-42", viewModel.state.value.normalizedCode)
        assertTrue(viewModel.state.value.canPreview)

        viewModel.onAction(PartyManagerAction.PreviewRejected)

        assertTrue(viewModel.state.value.hasCodeError)
    }

    @Test
    fun roomActionsReducePlayerAndDialogState() {
        val viewModel = RoomViewModel(ownerRoomModel())

        viewModel.onAction(RoomAction.StartPlaybackClicked)
        assertEquals(RoomPlaybackState.ERROR, viewModel.state.value.playback)

        viewModel.onAction(RoomAction.VideoUrlPasted("https://example.com/movie.mp4"))
        viewModel.onAction(RoomAction.InviteClicked)
        viewModel.onAction(RoomAction.InviteCopySucceeded(InviteCopyTarget.CODE))
        viewModel.onAction(RoomAction.ToggleMicClicked)

        with(viewModel.state.value) {
            assertEquals("https://example.com/movie.mp4", videoUrl)
            assertTrue(inviteOpen)
            assertEquals(InviteCopyState.CODE, inviteCopyState)
            assertTrue(micMuted)
            assertTrue(isOwner)
        }
    }

    @Test
    fun roomPlayerStateChangesReducePlaybackAndTimeline() {
        val viewModel = RoomViewModel(ownerRoomModel())

        viewModel.onAction(
            RoomAction.PlayerStateChanged(
                VideoPlayerState(
                    status = VideoPlayerStatus.PLAYING,
                    positionMs = 60_000L,
                    durationMs = 120_000L,
                    bufferedMs = 90_000L,
                    isLive = false,
                ),
            ),
        )

        with(viewModel.state.value) {
            assertEquals(RoomPlaybackState.PLAYING, playback)
            assertEquals(60_000L, positionMs)
            assertEquals(120_000L, durationMs)
            assertEquals(90_000L, bufferedMs)
            assertFalse(isLive)
        }

        viewModel.onAction(RoomAction.PlaybackPositionChanged(150_000L))
        viewModel.onAction(RoomAction.PlaybackSkipped(direction = 1f))

        with(viewModel.state.value) {
            assertEquals(120_000L, positionMs)
        }
    }

    @Test
    fun roomLiveStreamMapsToLiveWithoutDuration() {
        val viewModel = RoomViewModel(ownerRoomModel())

        viewModel.onAction(
            RoomAction.PlayerStateChanged(
                VideoPlayerState(
                    status = VideoPlayerStatus.PLAYING,
                    positionMs = 30_000L,
                    durationMs = 0L,
                    bufferedMs = 30_000L,
                    isLive = true,
                ),
            ),
        )

        viewModel.onAction(RoomAction.PlaybackPositionChanged(9_999L))

        with(viewModel.state.value) {
            assertEquals(RoomPlaybackState.PLAYING, playback)
            assertTrue(isLive)
            assertEquals(9_999L, positionMs)
        }
    }

    @Test
    fun roomPlayerErrorMapsToErrorState() {
        val viewModel = RoomViewModel(ownerRoomModel())

        viewModel.onAction(
            RoomAction.PlayerStateChanged(
                VideoPlayerState(
                    status = VideoPlayerStatus.ERROR,
                    positionMs = 0L,
                    durationMs = 0L,
                    bufferedMs = 0L,
                    error = "ERROR_CODE_IO",
                ),
            ),
        )

        viewModel.onAction(RoomAction.RetryPlaybackClicked)

        with(viewModel.state.value) {
            assertEquals(RoomPlaybackState.OWNER_EMPTY, playback)
        }
    }

    @Test
    fun settingsStateRepresentsPreferenceWriteFailure() {
        val profile = UserProfile("Nika", ProfileAvatarId.COMET)
        val failingRepository = object : ProfileRepository {
            override fun observeProfile(): Flow<UserProfile> = flowOf(profile)

            override fun currentProfile(): UserProfile = profile

            override fun saveProfile(profile: UserProfile): Result<Unit> =
                Result.failure(IllegalStateException("Test write failure"))
        }
        val viewModel = SettingsViewModel(failingRepository)

        viewModel.onAction(SettingsAction.SaveClicked)

        assertTrue(viewModel.state.value.saveFailed)
    }
}
