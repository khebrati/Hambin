package top.roomio.app.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import roomio.sharedui.generated.resources.Res
import roomio.sharedui.generated.resources.choose_name
import roomio.sharedui.generated.resources.create_party
import roomio.sharedui.generated.resources.edit_profile
import roomio.sharedui.generated.resources.friends_no_owner
import roomio.sharedui.generated.resources.friends_count
import roomio.sharedui.generated.resources.home_headline
import roomio.sharedui.generated.resources.home_supporting_copy
import roomio.sharedui.generated.resources.join_with_code
import roomio.sharedui.generated.resources.last_room
import roomio.sharedui.generated.resources.movie_night_tagline
import roomio.sharedui.generated.resources.open_party_manager
import roomio.sharedui.generated.resources.ready_when_everyone_is
import roomio.sharedui.generated.resources.rejoin_as_owner
import roomio.sharedui.generated.resources.roomio
import roomio.sharedui.generated.resources.settings
import roomio.sharedui.generated.resources.theme
import roomio.sharedui.generated.resources.watching_as
import roomio.sharedui.generated.resources.your_room_is_waiting
import top.roomio.app.theme.AppTheme
import top.roomio.app.theme.LocalThemeIsDark
import top.roomio.app.theme.RoomioDesignSystem
import top.roomio.app.theme.RoomioTheme
import top.roomio.app.profile.ProfileAvatar

@Immutable
internal data class HomeRoomPreview(
    val title: String,
    val friendCount: Int,
    val returnable: Boolean,
)

internal val returnableHomeRoom = HomeRoomPreview(
    title = "Friday night screening",
    friendCount = 2,
    returnable = true,
)

private val recentRoom = HomeRoomPreview(
    title = "Mira's late show",
    friendCount = 4,
    returnable = false,
)

@Composable
internal fun HomeScreen(
    state: HomeUiState = HomeUiState(),
    onAction: (HomeAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isDark by LocalThemeIsDark.current

    Scaffold(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            HomeTopBar(
                isDark = isDark,
                onSettings = { onAction(HomeAction.SettingsClicked) },
                onToggleTheme = { isDark = !isDark },
            )
        },
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            val expanded = maxWidth >= 840.dp
            val mediumOrWider = maxWidth >= 600.dp
            val horizontalPadding = if (mediumOrWider) {
                RoomioDesignSystem.spacing.medium
            } else {
                RoomioDesignSystem.spacing.small
            }

            if (expanded) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth()
                        .heightIn(min = maxHeight)
                        .padding(
                            horizontal = horizontalPadding,
                            vertical = RoomioDesignSystem.spacing.extraLarge,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeStage(
                        identityName = state.identityName,
                        identityAvatar = state.identityAvatar,
                        actionsHorizontal = true,
                        onSettings = { onAction(HomeAction.SettingsClicked) },
                        onCreate = { onAction(HomeAction.CreatePartyClicked) },
                        onJoin = { onAction(HomeAction.JoinPartyClicked) },
                        modifier = Modifier.weight(1.45f),
                    )
                    if (state.roomPreview != null) {
                        HomeRoomCard(
                            room = state.roomPreview,
                            expanded = true,
                            onClick = { onAction(HomeAction.OpenRoomClicked) },
                            modifier = Modifier.weight(.75f),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = RoomioDesignSystem.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.large),
                ) {
                    HomeStage(
                        identityName = state.identityName,
                        identityAvatar = state.identityAvatar,
                        actionsHorizontal = mediumOrWider,
                        onSettings = { onAction(HomeAction.SettingsClicked) },
                        onCreate = { onAction(HomeAction.CreatePartyClicked) },
                        onJoin = { onAction(HomeAction.JoinPartyClicked) },
                    )
                    if (state.roomPreview != null) {
                        HomeRoomCard(
                            room = state.roomPreview,
                            expanded = false,
                            onClick = { onAction(HomeAction.OpenRoomClicked) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    isDark: Boolean,
    onSettings: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1120.dp)
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.extraSmall),
                ) {
                    RoomioBrandMark()
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.roomio),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(Res.string.movie_night_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HomeIconButton(
                        icon = Icons.Filled.Settings,
                        label = stringResource(Res.string.settings),
                        onClick = onSettings,
                    )
                    HomeIconButton(
                        icon = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        label = stringResource(Res.string.theme),
                        onClick = onToggleTheme,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun RoomioBrandMark() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val container = MaterialTheme.colorScheme.primaryContainer
    Canvas(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.large)
            .background(container),
    ) {
        drawCircle(
            color = primary,
            radius = size.minDimension * .23f,
            center = Offset(size.width * .42f, size.height * .40f),
        )
        drawCircle(
            color = container,
            radius = size.minDimension * .20f,
            center = Offset(size.width * .66f, size.height * .65f),
        )
        drawCircle(
            color = secondary,
            radius = size.minDimension * .16f,
            center = Offset(size.width * .66f, size.height * .65f),
        )
    }
}

@Composable
private fun HomeIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).semantics { contentDescription = label },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun HomeStage(
    identityName: String,
    identityAvatar: ProfileAvatar,
    actionsHorizontal: Boolean,
    onSettings: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.large),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(Res.string.ready_when_everyone_is).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(Res.string.home_headline),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.home_supporting_copy),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IdentityPill(
            identityName = identityName,
            identityAvatar = identityAvatar,
            onSettings = onSettings,
        )
        if (actionsHorizontal) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CreatePartyButton(onClick = onCreate, modifier = Modifier.weight(1f))
                JoinPartyButton(onClick = onJoin, modifier = Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CreatePartyButton(onClick = onCreate)
                JoinPartyButton(onClick = onJoin)
            }
        }
    }
}

@Composable
private fun IdentityPill(
    identityName: String,
    identityAvatar: ProfileAvatar,
    onSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
        shape = RoomioDesignSystem.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.small),
        ) {
            Image(
                painter = painterResource(identityAvatar.resource),
                contentDescription = identityName,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.watching_as),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = identityName.ifBlank { stringResource(Res.string.choose_name) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HomeIconButton(
                icon = Icons.Filled.Settings,
                label = stringResource(Res.string.edit_profile),
                onClick = onSettings,
            )
        }
    }
}

@Composable
private fun CreatePartyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(RoomioDesignSystem.spacing.extraSmall))
        Text(stringResource(Res.string.create_party))
    }
}

@Composable
private fun JoinPartyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp),
    ) {
        Icon(Icons.Filled.LocalActivity, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(RoomioDesignSystem.spacing.extraSmall))
        Text(stringResource(Res.string.join_with_code))
    }
}

@Composable
private fun HomeRoomCard(
    room: HomeRoomPreview,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (room.returnable) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (room.returnable) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val outlineColor = if (room.returnable) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier.fillMaxWidth().border(1.dp, outlineColor, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
    ) {
        if (expanded) {
            Column(
                modifier = Modifier.padding(RoomioDesignSystem.spacing.small),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniCinemaScene(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
                )
                RoomCardDetails(room = room, onClick = onClick)
            }
        } else {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniCinemaScene(
                    modifier = Modifier.width(96.dp).aspectRatio(4f / 3f),
                )
                RoomCardDetails(room = room, onClick = onClick, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RoomCardDetails(
    room: HomeRoomPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.extraSmall),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (room.returnable) {
                    stringResource(Res.string.your_room_is_waiting).uppercase()
                } else {
                    stringResource(Res.string.last_room).uppercase()
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (room.returnable) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Text(
                text = room.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (room.returnable) {
                    stringResource(Res.string.friends_no_owner, room.friendCount)
                } else {
                    stringResource(Res.string.friends_count, room.friendCount)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (room.returnable) {
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                CrownIcon(
                    contentDescription = stringResource(Res.string.rejoin_as_owner),
                )
            }
        } else {
            IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(Res.string.open_party_manager),
                )
            }
        }
    }
}

@Composable
private fun CrownIcon(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onSecondaryContainer
    Canvas(
        modifier = modifier
            .size(24.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        drawCrown(color)
    }
}

private fun DrawScope.drawCrown(color: Color) {
    val crown = Path().apply {
        moveTo(size.width * .17f, size.height * .33f)
        lineTo(size.width * .36f, size.height * .53f)
        lineTo(size.width * .50f, size.height * .25f)
        lineTo(size.width * .64f, size.height * .53f)
        lineTo(size.width * .83f, size.height * .33f)
        lineTo(size.width * .75f, size.height * .72f)
        lineTo(size.width * .25f, size.height * .72f)
        close()
    }
    drawPath(crown, color)
    drawLine(
        color = color,
        start = Offset(size.width * .25f, size.height * .82f),
        end = Offset(size.width * .75f, size.height * .82f),
        strokeWidth = size.width * .09f,
        cap = StrokeCap.Round,
    )
}

@Composable
private fun MiniCinemaScene(modifier: Modifier = Modifier) {
    val colors = RoomioDesignSystem.colors
    Canvas(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(colors.sceneSky),
    ) {
        drawCircle(
            color = colors.scenePlanet,
            radius = size.minDimension * .075f,
            center = Offset(size.width * .89f, size.height * .16f),
        )

        val back = Path().apply {
            moveTo(-size.width * .05f, size.height)
            lineTo(size.width * .20f, size.height * .57f)
            lineTo(size.width * .42f, size.height * .82f)
            lineTo(size.width * .65f, size.height * .49f)
            lineTo(size.width * 1.05f, size.height * .77f)
            lineTo(size.width * 1.05f, size.height)
            close()
        }
        drawPath(back, colors.sceneRidgeBack)

        val front = Path().apply {
            moveTo(-size.width * .05f, size.height)
            lineTo(size.width * .20f, size.height * .79f)
            lineTo(size.width * .42f, size.height)
            lineTo(size.width * .65f, size.height * .72f)
            lineTo(size.width * 1.05f, size.height)
            close()
        }
        drawPath(front, colors.sceneRidgeFront)
    }
}

@Preview(name = "Home · compact", widthDp = 412, heightDp = 920)
@Composable
private fun HomeCompactPreview() = AppTheme(onThemeChanged = {}) {
    HomeScreen()
}

@Preview(name = "Home · expanded", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeExpandedPreview() = AppTheme(onThemeChanged = {}) {
    HomeScreen()
}

@Preview(name = "Home · dark", widthDp = 412, heightDp = 920)
@Composable
private fun HomeDarkPreview() = RoomioTheme(darkTheme = true) {
    HomeScreen()
}

@Preview(name = "Home · large text", widthDp = 412, heightDp = 920, fontScale = 1.5f)
@Composable
private fun HomeLargeTextPreview() = AppTheme(onThemeChanged = {}) {
    HomeScreen(
        state = HomeUiState(
            identityName = "",
            roomPreview = recentRoom,
        ),
    )
}
