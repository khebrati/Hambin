package top.roomio.app.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import top.roomio.app.home.HomeScreen
import top.roomio.app.manager.ManagerMode
import top.roomio.app.manager.PartyManagerScreen
import top.roomio.app.profile.ProfileAvatar
import top.roomio.app.profile.SettingsScreen
import top.roomio.app.preview.RoomAvailability
import top.roomio.app.preview.RoomPreviewScreen
import top.roomio.app.preview.roomPreviewForCode
import top.roomio.app.room.RoomScreen
import top.roomio.app.room.joinedRoomModel

@Serializable
internal data object HomeRoute : NavKey

@Serializable
internal data object ProfileRoute : NavKey

@Serializable
internal data class PartyManagerRoute(
    val initialMode: PartyManagerMode,
) : NavKey

@Serializable
internal enum class PartyManagerMode { CREATE, JOIN }

@Serializable
internal data class RoomPreviewRoute(
    val partyCode: String,
) : NavKey

@Serializable
internal data class RoomRoute(
    val joinedPartyCode: String? = null,
) : NavKey

@Composable
internal fun RoomioNavigation(
    modifier: Modifier = Modifier,
) {
    val savedStateConfiguration = remember {
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(HomeRoute.serializer())
                    subclass(ProfileRoute.serializer())
                    subclass(PartyManagerRoute.serializer())
                    subclass(RoomPreviewRoute.serializer())
                    subclass(RoomRoute.serializer())
                }
            }
        }
    }
    val backStack = rememberNavBackStack(
        savedStateConfiguration,
        HomeRoute,
    )
    var identityName by rememberSaveable { mutableStateOf("Nika") }
    var identityAvatarName by rememberSaveable {
        mutableStateOf(ProfileAvatar.COMET.name)
    }
    val identityAvatar = ProfileAvatar.valueOf(identityAvatarName)

    fun navigate(route: NavKey) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    fun navigateHome() {
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = ::navigateBack,
        entryProvider = entryProvider {
            entry<HomeRoute> {
                HomeScreen(
                    identityName = identityName,
                    identityAvatar = identityAvatar,
                    onSettings = { navigate(ProfileRoute) },
                    onCreate = {
                        navigate(
                            PartyManagerRoute(
                                initialMode = PartyManagerMode.CREATE,
                            ),
                        )
                    },
                    onJoin = {
                        navigate(
                            PartyManagerRoute(
                                initialMode = PartyManagerMode.JOIN,
                            ),
                        )
                    },
                    onOpenRoom = { navigate(RoomRoute()) },
                )
            }
            entry<ProfileRoute> {
                SettingsScreen(
                    identityName = identityName,
                    identityAvatar = identityAvatar,
                    onBack = ::navigateBack,
                    onSave = { name, avatar ->
                        identityName = name
                        identityAvatarName = avatar.name
                        navigateBack()
                    },
                )
            }
            entry<PartyManagerRoute> { route ->
                PartyManagerScreen(
                    initialMode = when (route.initialMode) {
                        PartyManagerMode.CREATE -> ManagerMode.CREATE
                        PartyManagerMode.JOIN -> ManagerMode.JOIN
                    },
                    onBack = ::navigateBack,
                    onCreate = { navigate(RoomRoute()) },
                    onPreview = { code ->
                        val normalizedCode = code.trim().uppercase()
                        val previewExists = roomPreviewForCode(normalizedCode) != null
                        if (previewExists) {
                            navigate(RoomPreviewRoute(normalizedCode))
                        }
                        previewExists
                    },
                )
            }
            entry<RoomPreviewRoute> { route ->
                val preview = roomPreviewForCode(route.partyCode)
                checkNotNull(preview) {
                    "Unknown local party code: ${route.partyCode}"
                }
                RoomPreviewScreen(
                    model = preview,
                    onBack = ::navigateBack,
                    onJoin = {
                        if (preview.availability == RoomAvailability.AVAILABLE) {
                            navigate(RoomRoute(joinedPartyCode = preview.code))
                        }
                    },
                )
            }
            entry<RoomRoute> { route ->
                RoomScreen(
                    model = route.joinedPartyCode?.let(::joinedRoomModel)
                        ?: top.roomio.app.room.ownerRoomModel(),
                    onLeaveParty = ::navigateHome,
                )
            }
        },
        transitionSpec = {
            (
                slideInHorizontally(
                    initialOffsetX = { width -> width / 10 },
                    animationSpec = tween(300),
                ) + fadeIn(animationSpec = tween(300))
                ) togetherWith fadeOut(animationSpec = tween(200))
        },
        popTransitionSpec = {
            fadeIn(animationSpec = tween(250)) togetherWith (
                slideOutHorizontally(
                    targetOffsetX = { width -> width / 10 },
                    animationSpec = tween(250),
                ) + fadeOut(animationSpec = tween(200))
                )
        },
        predictivePopTransitionSpec = {
            fadeIn(animationSpec = tween(250)) togetherWith (
                slideOutHorizontally(
                    targetOffsetX = { width -> width / 10 },
                    animationSpec = tween(250),
                ) + fadeOut(animationSpec = tween(200))
                )
        },
    )
}
