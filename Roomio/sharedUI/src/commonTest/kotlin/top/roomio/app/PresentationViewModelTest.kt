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
