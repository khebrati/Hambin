package top.roomio.app.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import top.roomio.app.home.HomeAction
import top.roomio.app.home.HomeScreen
import top.roomio.app.manager.ManagerMode
import top.roomio.app.manager.PartyManagerAction
import top.roomio.app.manager.PartyManagerScreen
import top.roomio.app.profile.SettingsAction
import top.roomio.app.profile.SettingsEffect
import top.roomio.app.profile.SettingsScreen
import top.roomio.app.preview.RoomPreviewAction
import top.roomio.app.preview.RoomPreviewScreen
import top.roomio.app.preview.roomPreviewForCode
import top.roomio.app.room.RoomAction
import top.roomio.app.room.RoomScreen
import top.roomio.app.room.joinedRoomModel
import top.roomio.app.ui.PresentationGraph

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
    presentationGraph: PresentationGraph,
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
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeRoute> {
                val viewModel = viewModel { presentationGraph.homeViewModel }
                val state by viewModel.state.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    onAction = { action ->
                        when (action) {
                            HomeAction.SettingsClicked -> navigate(ProfileRoute)
                            HomeAction.CreatePartyClicked -> navigate(
                                PartyManagerRoute(PartyManagerMode.CREATE),
                            )
                            HomeAction.JoinPartyClicked -> navigate(
                                PartyManagerRoute(PartyManagerMode.JOIN),
                            )
                            HomeAction.OpenRoomClicked -> navigate(RoomRoute())
                        }
                    },
                )
            }
            entry<ProfileRoute> {
                val viewModel = viewModel { presentationGraph.settingsViewModel }
                LaunchedEffect(viewModel) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            SettingsEffect.PROFILE_SAVED -> navigateBack()
                        }
                    }
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    onAction = { action ->
                        viewModel.onAction(action)
                        when (action) {
                            SettingsAction.BackClicked -> navigateBack()
                            SettingsAction.SaveClicked,
                            is SettingsAction.NameChanged,
                            is SettingsAction.AvatarSelected,
                            -> Unit
                        }
                    },
                )
            }
            entry<PartyManagerRoute> { route ->
                val initialMode = when (route.initialMode) {
                    PartyManagerMode.CREATE -> ManagerMode.CREATE
                    PartyManagerMode.JOIN -> ManagerMode.JOIN
                }
                val viewModel = viewModel {
                    presentationGraph.partyManagerViewModelFactory.create(
                        initialMode = initialMode,
                        loading = false,
                        codeError = false,
                    )
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                PartyManagerScreen(
                    state = state,
                    onAction = { action ->
                        viewModel.onAction(action)
                        when (action) {
                            PartyManagerAction.BackClicked -> navigateBack()
                            PartyManagerAction.CreateClicked -> navigate(RoomRoute())
                            PartyManagerAction.PreviewClicked -> {
                                if (roomPreviewForCode(state.normalizedCode) != null) {
                                    navigate(RoomPreviewRoute(state.normalizedCode))
                                } else {
                                    viewModel.onAction(PartyManagerAction.PreviewRejected)
                                }
                            }
                            is PartyManagerAction.ModeSelected,
                            is PartyManagerAction.CodeChanged,
                            PartyManagerAction.PreviewRejected,
                            -> Unit
                        }
                    },
                )
            }
            entry<RoomPreviewRoute> { route ->
                val preview = roomPreviewForCode(route.partyCode)
                checkNotNull(preview) {
                    "Unknown local party code: ${route.partyCode}"
                }
                val viewModel = viewModel {
                    presentationGraph.roomPreviewViewModelFactory.create(preview)
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                RoomPreviewScreen(
                    state = state,
                    onAction = { action ->
                        when (action) {
                            RoomPreviewAction.BackClicked -> navigateBack()
                            RoomPreviewAction.JoinClicked -> {
                                if (state.canJoin) {
                                    navigate(RoomRoute(joinedPartyCode = state.model.code))
                                }
                            }
                        }
                    },
                )
            }
            entry<RoomRoute> { route ->
                val model = route.joinedPartyCode?.let(::joinedRoomModel)
                    ?: top.roomio.app.room.ownerRoomModel()
                val viewModel = viewModel {
                    presentationGraph.roomViewModelFactory.create(model)
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                RoomScreen(
                    state = state,
                    effects = viewModel.effects,
                    onAction = { action ->
                        viewModel.onAction(action)
                        if (action == RoomAction.LeaveConfirmed) {
                            navigateHome()
                        }
                    },
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
