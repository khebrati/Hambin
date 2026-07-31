package top.roomio.app.room

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import roomio.sharedui.generated.resources.*
import top.roomio.app.theme.AppTheme
import top.roomio.app.theme.LocalThemeIsDark
import top.roomio.app.theme.RoomioDesignSystem

/**
 * Screen fixture and state contract mirrored from the browser prototype's
 * PartyScreen. It stays local and deterministic while the product has no
 * room service or media implementation.
 */
internal data class RoomScreenModel(
    val title: String,
    val partyCode: String,
    val role: RoomRole,
    val ownerName: String,
    val ownerPresent: Boolean,
    val playback: RoomPlaybackState,
    val participants: List<RoomParticipant>,
)

internal enum class RoomRole { OWNER, GUEST }

internal enum class RoomPlaybackState {
    OWNER_EMPTY,
    WAITING_FOR_OWNER,
    LOADING,
    PLAYING,
    PAUSED,
    BUFFERING,
    ERROR,
    ABORTED,
}

internal enum class RoomAvatar { COMET, MINT, SUNNY, BERRY }

internal data class RoomParticipant(
    val name: String,
    val avatar: RoomAvatar,
    val isSelf: Boolean = false,
    val isHost: Boolean = false,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
)

private val ownerEmptyRoom = RoomScreenModel(
    title = "Friday night screening",
    partyCode = "MOON-42",
    role = RoomRole.OWNER,
    ownerName = "You",
    ownerPresent = true,
    playback = RoomPlaybackState.OWNER_EMPTY,
    participants = listOf(
        RoomParticipant("You", RoomAvatar.COMET, isSelf = true, isHost = true),
        RoomParticipant("Ellis", RoomAvatar.SUNNY, isSpeaking = true),
        RoomParticipant("Jo", RoomAvatar.BERRY, isMuted = true),
    ),
)

private val guestPlayingRoom = RoomScreenModel(
    title = "Mira's late show",
    partyCode = "MOON-42",
    role = RoomRole.GUEST,
    ownerName = "Mira",
    ownerPresent = true,
    playback = RoomPlaybackState.PLAYING,
    participants = listOf(
        RoomParticipant("Mira", RoomAvatar.MINT, isHost = true, isSpeaking = true),
        RoomParticipant("Ellis", RoomAvatar.SUNNY, isMuted = true),
        RoomParticipant("Jo", RoomAvatar.BERRY),
        RoomParticipant("You", RoomAvatar.COMET, isSelf = true),
    ),
)

@Composable
internal fun RoomScreen(
    model: RoomScreenModel = ownerEmptyRoom,
    modifier: Modifier = Modifier,
) {
    var isDark by LocalThemeIsDark.current
    var playback by remember(model) { mutableStateOf(model.playback) }
    var videoUrl by remember(model) { mutableStateOf("") }
    var playbackPosition by remember(model) { mutableFloatStateOf(0.13f) }
    var volume by remember { mutableFloatStateOf(0.72f) }
    var micMuted by remember { mutableStateOf(false) }
    var theaterMode by remember { mutableStateOf(false) }
    var inviteOpen by remember { mutableStateOf(false) }
    var linkOpen by remember { mutableStateOf(false) }
    var leaveOpen by remember { mutableStateOf(false) }
    var abortOpen by remember { mutableStateOf(false) }
    var hasLeft by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val syncComplete = stringResource(Res.string.sync_complete)
    val pastedUrl = stringResource(Res.string.sample_video_url)
    val partyCodeCopied = stringResource(Res.string.party_code_copied)
    val inviteLinkCopied = stringResource(Res.string.invite_link_copied)
    val copyUnavailable = stringResource(Res.string.copy_unavailable)

    LaunchedEffect(playback) {
        if (playback == RoomPlaybackState.LOADING) {
            delay(650)
            playback = RoomPlaybackState.PLAYING
        }
    }

    if (hasLeft) {
        LeftRoomState(modifier, onRejoin = { hasLeft = false })
        return
    }

    val isOwner = model.role == RoomRole.OWNER && model.ownerPresent
    val hasPlayer = playback in setOf(
        RoomPlaybackState.PLAYING,
        RoomPlaybackState.PAUSED,
        RoomPlaybackState.BUFFERING,
    )

    Scaffold(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            RoomTopBar(
                title = model.title,
                subtitle = if (model.role == RoomRole.GUEST) {
                    "${model.partyCode} · ${stringResource(Res.string.guest)}"
                } else {
                    null
                },
                showLink = hasPlayer,
                showTheme = true,
                onInvite = { inviteOpen = true },
                onShowLink = { linkOpen = true },
                onLeave = { leaveOpen = true },
                onToggleTheme = { isDark = !isDark },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            val compact = maxWidth < 840.dp || theaterMode
            val cinema: @Composable ColumnScope.() -> Unit = {
                if (!model.ownerPresent) {
                    OwnerAwayBanner(model.ownerName)
                    Spacer(Modifier.height(RoomioDesignSystem.spacing.small))
                }
                CinemaCard(
                    playback = playback,
                    isOwner = isOwner,
                    ownerName = model.ownerName,
                    videoUrl = videoUrl,
                    playbackPosition = playbackPosition,
                    volume = volume,
                    onVideoUrlChange = {
                        videoUrl = it
                        if (playback == RoomPlaybackState.ERROR) playback = RoomPlaybackState.OWNER_EMPTY
                    },
                    onPaste = { videoUrl = pastedUrl },
                    onStart = {
                        playback = if (videoUrl.startsWith("http")) RoomPlaybackState.LOADING else RoomPlaybackState.ERROR
                    },
                    onRetry = { playback = if (isOwner) RoomPlaybackState.OWNER_EMPTY else RoomPlaybackState.WAITING_FOR_OWNER },
                    onTogglePlayback = {
                        playback = if (playback == RoomPlaybackState.PLAYING) RoomPlaybackState.PAUSED else RoomPlaybackState.PLAYING
                    },
                    onPositionChange = { playbackPosition = it },
                    onSkip = { direction -> playbackPosition = (playbackPosition + direction * 0.035f).coerceIn(0f, 1f) },
                    onVolumeChange = { volume = it },
                    onToggleVolume = { volume = if (volume == 0f) .72f else 0f },
                    onToggleTheater = { theaterMode = !theaterMode },
                )
                if (hasPlayer) {
                    Spacer(Modifier.height(RoomioDesignSystem.spacing.small))
                    SyncBand(
                        paused = playback == RoomPlaybackState.PAUSED,
                        onSync = {
                            playbackPosition = .21f
                            scope.launch { snackbarHost.showSnackbar(syncComplete) }
                        },
                    )
                }
            }

            val support: @Composable () -> Unit = {
                RoomSupportPanel(
                    participants = model.participants,
                    micMuted = micMuted,
                    streamActive = hasPlayer,
                    isOwner = isOwner,
                    ownerPresent = model.ownerPresent,
                    onToggleMic = { micMuted = !micMuted },
                    onInvite = { inviteOpen = true },
                    onShowLink = { linkOpen = true },
                    onEndStream = { abortOpen = true },
                )
            }

            if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = RoomioDesignSystem.spacing.small, vertical = RoomioDesignSystem.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.small),
                ) {
                    cinema()
                    if (!theaterMode) support()
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1120.dp)
                        .padding(horizontal = RoomioDesignSystem.spacing.small, vertical = RoomioDesignSystem.spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.medium),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1.7f), verticalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.small)) {
                        cinema()
                    }
                    Box(Modifier.weight(.75f)) { support() }
                }
            }
        }
    }

    if (inviteOpen) {
        InviteFriendsDialog(
            partyCode = model.partyCode,
            inviteLink = "https://roomio.app/?invite=${model.partyCode}",
            onDismiss = { inviteOpen = false },
            onCopied = { target ->
                scope.launch {
                    snackbarHost.showSnackbar(
                        if (target == InviteCopyTarget.CODE) partyCodeCopied else inviteLinkCopied,
                    )
                }
            },
            onCopyFailed = {
                scope.launch { snackbarHost.showSnackbar(copyUnavailable) }
            },
        )
    }
    if (linkOpen) {
        RoomDialog(
            title = stringResource(Res.string.current_video_link),
            body = "https://roomio.app/watch/${model.partyCode.lowercase()}",
            confirm = stringResource(Res.string.done),
            onDismiss = { linkOpen = false },
        )
    }
    if (leaveOpen) {
        AlertDialog(
            onDismissRequest = { leaveOpen = false },
            title = { Text(stringResource(Res.string.leave_party)) },
            text = { Text(stringResource(if (isOwner) Res.string.leave_owner_copy else Res.string.leave_guest_copy)) },
            confirmButton = {
                TextButton(onClick = { hasLeft = true; leaveOpen = false }) { Text(stringResource(Res.string.leave)) }
            },
            dismissButton = { TextButton(onClick = { leaveOpen = false }) { Text(stringResource(Res.string.stay)) } },
        )
    }
    if (abortOpen) {
        AlertDialog(
            onDismissRequest = { abortOpen = false },
            title = { Text(stringResource(Res.string.end_stream_question)) },
            text = { Text(stringResource(Res.string.end_stream_copy)) },
            confirmButton = {
                TextButton(onClick = { playback = RoomPlaybackState.ABORTED; abortOpen = false }) {
                    Text(stringResource(Res.string.end_for_everyone))
                }
            },
            dismissButton = { TextButton(onClick = { abortOpen = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun RoomTopBar(
    title: String,
    subtitle: String?,
    showLink: Boolean,
    showTheme: Boolean,
    onInvite: () -> Unit,
    onShowLink: () -> Unit,
    onLeave: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
        Column {
            BoxWithConstraints {
                val compactBar = maxWidth < 600.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (compactBar) 64.dp else 72.dp)
                        .padding(
                            horizontal = if (compactBar) 4.dp else 16.dp,
                            vertical = if (compactBar) 4.dp else 8.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compactBar) 6.dp else 10.dp),
                ) {
                    RoomioMark(size = if (compactBar) 40.dp else 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            style = if (compactBar) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            maxLines = if (compactBar) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (subtitle != null && !compactBar) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    MaterialIconButton(Icons.Filled.PersonAdd, stringResource(Res.string.invite_friends), tonal = true, onClick = onInvite)
                    if (showLink) MaterialIconButton(Icons.Filled.Link, stringResource(Res.string.current_video_link), onClick = onShowLink)
                    MaterialIconButton(Icons.AutoMirrored.Filled.Logout, stringResource(Res.string.leave), onClick = onLeave)
                    if (showTheme && !compactBar) {
                        MaterialIconButton(Icons.Filled.DarkMode, stringResource(Res.string.theme), onClick = onToggleTheme)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun RoomioMark(size: Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Box(
            Modifier
                .size(21.dp)
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            Modifier
                .size(16.dp)
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
    }
}

@Composable
private fun MaterialIconButton(
    icon: ImageVector,
    label: String,
    tonal: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
) {
    if (tonal) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(size).semantics { contentDescription = label },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) { Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize)) }
    } else {
        IconButton(onClick = onClick, modifier = Modifier.size(size).semantics { contentDescription = label }) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
private fun CinemaCard(
    playback: RoomPlaybackState,
    isOwner: Boolean,
    ownerName: String,
    videoUrl: String,
    playbackPosition: Float,
    volume: Float,
    onVideoUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onStart: () -> Unit,
    onRetry: () -> Unit,
    onTogglePlayback: () -> Unit,
    onPositionChange: (Float) -> Unit,
    onSkip: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleVolume: () -> Unit,
    onToggleTheater: () -> Unit,
) {
    val hasPlayer = playback in setOf(RoomPlaybackState.PLAYING, RoomPlaybackState.PAUSED, RoomPlaybackState.BUFFERING)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = RoomioDesignSystem.colors.mediaSurface,
            contentColor = RoomioDesignSystem.colors.onMedia,
        ),
        shape = RoomioDesignSystem.shapes.extraLargeIncreased,
    ) {
        Column {
            Box(Modifier.fillMaxWidth()) {
                CinemaScene(showTitle = hasPlayer, muted = !hasPlayer)
                if (playback == RoomPlaybackState.PAUSED) {
                    FilledIconButton(
                        onClick = onTogglePlayback,
                        modifier = Modifier.align(Alignment.Center).size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(Res.string.play), modifier = Modifier.size(38.dp))
                    }
                }
            }
            when {
                hasPlayer -> PlayerControls(
                    paused = playback == RoomPlaybackState.PAUSED,
                    buffering = playback == RoomPlaybackState.BUFFERING,
                    position = playbackPosition,
                    volume = volume,
                    onTogglePlayback = onTogglePlayback,
                    onPositionChange = onPositionChange,
                    onSkip = onSkip,
                    onVolumeChange = onVolumeChange,
                    onToggleVolume = onToggleVolume,
                    onToggleTheater = onToggleTheater,
                )
                playback == RoomPlaybackState.LOADING -> LoadingCinema()
                isOwner -> OwnerCinemaEntry(
                    playback = playback,
                    videoUrl = videoUrl,
                    onVideoUrlChange = onVideoUrlChange,
                    onPaste = onPaste,
                    onStart = onStart,
                    onRetry = onRetry,
                )
                else -> GuestWaitingCinema(playback, ownerName, onRetry)
            }
        }
    }
}

@Composable
private fun CinemaScene(showTitle: Boolean, muted: Boolean) {
    val colors = RoomioDesignSystem.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(colors.sceneSky),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val planetCenter = Offset(size.width * .79f, size.height * .30f)
            drawCircle(colors.scenePlanet.copy(alpha = .15f), radius = size.minDimension * .22f, center = planetCenter)
            drawCircle(colors.scenePlanet.copy(alpha = if (muted) .68f else 1f), radius = size.minDimension * .17f, center = planetCenter)
            drawCircle(colors.onMedia.copy(alpha = .72f), radius = 2.5.dp.toPx(), center = Offset(size.width * .18f, size.height * .20f))
            drawCircle(colors.onMedia.copy(alpha = .72f), radius = 2.5.dp.toPx(), center = Offset(size.width * .43f, size.height * .12f))
            drawCircle(colors.onMedia.copy(alpha = .72f), radius = 2.5.dp.toPx(), center = Offset(size.width * .70f, size.height * .33f))

            val backRidge = Path().apply {
                moveTo(0f, size.height * .72f)
                lineTo(size.width * .19f, size.height * .37f)
                lineTo(size.width * .31f, size.height * .65f)
                lineTo(size.width * .48f, size.height * .22f)
                lineTo(size.width * .64f, size.height * .70f)
                lineTo(size.width * .80f, size.height * .35f)
                lineTo(size.width, size.height * .72f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(backRidge, colors.sceneRidgeBack)
            val frontRidge = Path().apply {
                moveTo(0f, size.height * .80f)
                lineTo(size.width * .20f, size.height * .55f)
                lineTo(size.width * .35f, size.height * .76f)
                lineTo(size.width * .48f, size.height * .48f)
                lineTo(size.width * .65f, size.height * .80f)
                lineTo(size.width * .80f, size.height * .50f)
                lineTo(size.width, size.height * .77f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(frontRidge, colors.sceneRidgeFront)

            val stationWidth = size.width * .23f
            val stationHeight = size.height * .26f
            val stationLeft = size.width * .14f
            val stationTop = size.height * .58f
            drawRect(colors.sceneStation, topLeft = Offset(stationLeft + stationWidth * .41f, stationTop - stationHeight * .45f), size = androidx.compose.ui.geometry.Size(stationWidth * .18f, stationHeight * .45f))
            drawRoundRect(
                color = colors.sceneStation,
                topLeft = Offset(stationLeft, stationTop),
                size = androidx.compose.ui.geometry.Size(stationWidth, stationHeight),
                cornerRadius = CornerRadius(size.minDimension * .055f),
            )
            drawRoundRect(
                color = colors.mediaOutline,
                topLeft = Offset(stationLeft, stationTop),
                size = androidx.compose.ui.geometry.Size(stationWidth, stationHeight),
                cornerRadius = CornerRadius(size.minDimension * .055f),
                style = Stroke(width = 3.dp.toPx()),
            )
            repeat(3) { index ->
                drawRoundRect(
                    color = colors.sceneWindow,
                    topLeft = Offset(stationLeft + stationWidth * (.18f + index * .25f), stationTop + stationHeight * .42f),
                    size = androidx.compose.ui.geometry.Size(stationWidth * .15f, stationHeight * .22f),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }
        if (showTitle) {
            Text(
                text = stringResource(Res.string.aurora_station),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onMedia,
            )
        }
    }
}

@Composable
private fun OwnerCinemaEntry(
    playback: RoomPlaybackState,
    videoUrl: String,
    onVideoUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onStart: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = RoomioDesignSystem.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.mediaContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(if (playback == RoomPlaybackState.ABORTED) Res.string.stream_aborted else Res.string.your_cinema_empty), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.inversePrimary)
        Text(stringResource(Res.string.bring_direct_video), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = videoUrl,
            onValueChange = onVideoUrlChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = playback == RoomPlaybackState.ERROR,
            label = { Text(stringResource(Res.string.direct_video_url)) },
            placeholder = { Text(stringResource(Res.string.url_hint)) },
            trailingIcon = {
                MaterialIconButton(
                    Icons.Filled.ContentPaste,
                    stringResource(Res.string.paste_video_link),
                    size = 40.dp,
                    iconSize = 20.dp,
                    onClick = onPaste,
                )
            },
            supportingText = if (playback == RoomPlaybackState.ERROR) {
                { Text(stringResource(Res.string.stream_error_copy)) }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.onMedia,
                unfocusedTextColor = colors.onMedia,
                focusedBorderColor = colors.onMedia.copy(alpha = .75f),
                unfocusedBorderColor = colors.mediaOutline,
                focusedLabelColor = colors.onMedia,
                unfocusedLabelColor = colors.onMedia.copy(alpha = .72f),
                cursorColor = colors.onMedia,
            ),
        )
        Button(
            onClick = if (playback == RoomPlaybackState.ERROR) onRetry else onStart,
            modifier = Modifier.fillMaxWidth(),
            enabled = playback == RoomPlaybackState.ERROR || videoUrl.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = .50f),
            ),
        ) {
            Text(stringResource(if (playback == RoomPlaybackState.ERROR) Res.string.try_again else Res.string.start_stream))
        }
    }
}

@Composable
private fun GuestWaitingCinema(playback: RoomPlaybackState, ownerName: String, onRetry: () -> Unit) {
    val isAborted = playback == RoomPlaybackState.ABORTED
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RoomioDesignSystem.colors.mediaContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(if (isAborted) Res.string.stream_aborted else Res.string.waiting_room), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.inversePrimary)
        Text(
            if (isAborted) stringResource(Res.string.waiting_for_next_stream) else "$ownerName ${stringResource(Res.string.is_choosing_video)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (playback == RoomPlaybackState.ERROR) TextButton(onClick = onRetry) { Text(stringResource(Res.string.try_again)) }
    }
}

@Composable
private fun LoadingCinema() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .background(RoomioDesignSystem.colors.mediaContainer)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiaryContainer)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(Res.string.preparing_cinema), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PlayerControls(
    paused: Boolean,
    buffering: Boolean,
    position: Float,
    volume: Float,
    onTogglePlayback: () -> Unit,
    onPositionChange: (Float) -> Unit,
    onSkip: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleVolume: () -> Unit,
    onToggleTheater: () -> Unit,
) {
    val colors = RoomioDesignSystem.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.mediaContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Slider(
            value = position,
            onValueChange = onPositionChange,
            modifier = Modifier.fillMaxWidth().height(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = colors.mediaOutline,
            ),
        )
        if (buffering) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.onMedia, strokeWidth = 2.dp)
                Text(stringResource(Res.string.buffering), style = MaterialTheme.typography.labelMedium)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 520.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlaybackTime(position, Modifier.weight(1f))
                    TransportControls(paused, onTogglePlayback, onSkip)
                    VolumeControls(volume, onVolumeChange, onToggleVolume, onToggleTheater, Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlaybackTime(position, Modifier.weight(1f))
                        TransportControls(paused, onTogglePlayback, onSkip)
                    }
                    VolumeControls(volume, onVolumeChange, onToggleVolume, onToggleTheater, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PlaybackTime(position: Float, modifier: Modifier = Modifier) {
    val minute = 18 + (position * 10).toInt()
    Text(
        text = "$minute:45 / 1:42:18",
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = RoomioDesignSystem.colors.onMedia,
    )
}

@Composable
private fun TransportControls(paused: Boolean, onTogglePlayback: () -> Unit, onSkip: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        MaterialIconButton(Icons.Filled.FastRewind, stringResource(Res.string.rewind_15), size = 48.dp, onClick = { onSkip(-1f) })
        FilledIconButton(
            onClick = onTogglePlayback,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(
                if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = stringResource(if (paused) Res.string.play else Res.string.pause),
                modifier = Modifier.size(28.dp),
            )
        }
        MaterialIconButton(Icons.Filled.FastForward, stringResource(Res.string.forward_15), size = 48.dp, onClick = { onSkip(1f) })
    }
}

@Composable
private fun VolumeControls(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onToggleVolume: () -> Unit,
    onToggleTheater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        MaterialIconButton(
            if (volume == 0f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
            stringResource(if (volume == 0f) Res.string.unmute_video else Res.string.mute_video),
            size = 40.dp,
            iconSize = 22.dp,
            onClick = onToggleVolume,
        )
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            modifier = Modifier.widthIn(min = 80.dp, max = 108.dp).weight(1f, fill = false),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = RoomioDesignSystem.colors.mediaOutline,
            ),
        )
        MaterialIconButton(Icons.Filled.Fullscreen, stringResource(Res.string.toggle_theater), size = 40.dp, iconSize = 22.dp, onClick = onToggleTheater)
    }
}

@Composable
private fun SyncBand(paused: Boolean, onSync: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.local_playback), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(if (paused) Res.string.paused_for_you else Res.string.playing_for_you), style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onSync) {
                Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.sync_to_room))
            }
        }
    }
}

@Composable
private fun OwnerAwayBanner(ownerName: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoomioDesignSystem.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.owner_away), style = MaterialTheme.typography.labelLarge)
            Text("$ownerName ${stringResource(Res.string.left_the_room)}", style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.owner_away_copy), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RoomSupportPanel(
    participants: List<RoomParticipant>,
    micMuted: Boolean,
    streamActive: Boolean,
    isOwner: Boolean,
    ownerPresent: Boolean,
    onToggleMic: () -> Unit,
    onInvite: () -> Unit,
    onShowLink: () -> Unit,
    onEndStream: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoomioDesignSystem.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ParticipantSection(participants, micMuted)
            HorizontalDivider(color = DividerDefaults.color)
            VoicePanel(micMuted, onToggleMic)
            OutlinedButton(onClick = onInvite, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.invite_friends))
            }
            if (streamActive) {
                HorizontalDivider(color = DividerDefaults.color)
                SourceRow(onShowLink)
            }
            if (isOwner && ownerPresent && streamActive) {
                Button(
                    onClick = onEndStream,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.end_stream_for_everyone))
                }
            }
        }
    }
}

@Composable
private fun ParticipantSection(participants: List<RoomParticipant>, micMuted: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.voice_party), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(Res.string.room_members), style = MaterialTheme.typography.titleLarge)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    text = participants.size.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        participants.forEach { ParticipantRow(it, micMuted) }
    }
}

@Composable
private fun ParticipantRow(participant: RoomParticipant, micMuted: Boolean) {
    val speaking = participant.isSpeaking && !(participant.isSelf && micMuted)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = if (speaking) RoomioDesignSystem.shapes.extraLargeIncreased else MaterialTheme.shapes.large,
        color = if (speaking) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (speaking) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ParticipantAvatar(participant, speaking)
            Column(Modifier.weight(1f)) {
                Text(participant.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        participant.isHost -> stringResource(Res.string.host)
                        speaking -> stringResource(Res.string.speaking)
                        participant.isMuted || (participant.isSelf && micMuted) -> stringResource(Res.string.mic_muted)
                        else -> stringResource(Res.string.listening)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ParticipantAvatar(participant: RoomParticipant, speaking: Boolean) {
    val painter = when (participant.avatar) {
        RoomAvatar.COMET -> painterResource(Res.drawable.avatar_comet)
        RoomAvatar.MINT -> painterResource(Res.drawable.avatar_mint)
        RoomAvatar.SUNNY -> painterResource(Res.drawable.avatar_sunny)
        RoomAvatar.BERRY -> painterResource(Res.drawable.avatar_berry)
    }
    val avatarShape = if (speaking) RoomioDesignSystem.shapes.largeIncreased else CircleShape
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(avatarShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = if (speaking) 4.dp else 2.dp,
                color = if (speaking) RoomioDesignSystem.colors.speaker else MaterialTheme.colorScheme.outlineVariant,
                shape = avatarShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(painter = painter, contentDescription = participant.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Icon(
                imageVector = if (participant.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(3.dp),
            )
        }
    }
}

@Composable
private fun VoicePanel(micMuted: Boolean, onToggleMic: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.voice_room), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(if (micMuted) Res.string.your_mic_is_muted else Res.string.your_mic_is_on), style = MaterialTheme.typography.titleMedium)
        }
        FilledIconButton(
            onClick = onToggleMic,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
        ) {
            Icon(
                if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = stringResource(if (micMuted) Res.string.unmute else Res.string.mute),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SourceRow(onShowLink: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.video_source), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(Res.string.aurora_file_name), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MaterialIconButton(Icons.Filled.ContentCopy, stringResource(Res.string.current_video_link), size = 40.dp, iconSize = 22.dp, onClick = onShowLink)
    }
}

private enum class InviteCopyTarget { CODE, LINK }

private enum class InviteCopyState { IDLE, CODE, LINK, ERROR }

@Composable
private fun InviteFriendsDialog(
    partyCode: String,
    inviteLink: String,
    onDismiss: () -> Unit,
    onCopied: (InviteCopyTarget) -> Unit,
    onCopyFailed: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copyState by remember { mutableStateOf(InviteCopyState.IDLE) }

    fun copy(value: String, target: InviteCopyTarget) {
        runCatching { clipboard.setText(AnnotatedString(value)) }
            .onSuccess {
                copyState = if (target == InviteCopyTarget.CODE) InviteCopyState.CODE else InviteCopyState.LINK
                onCopied(target)
            }
            .onFailure {
                copyState = InviteCopyState.ERROR
                onCopyFailed()
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(RoomioDesignSystem.spacing.small),
            contentAlignment = Alignment.Center,
        ) {
            InviteFriendsDialogCard(
                partyCode = partyCode,
                inviteLink = inviteLink,
                copyState = copyState,
                onCopyCode = { copy(partyCode, InviteCopyTarget.CODE) },
                onCopyLink = { copy(inviteLink, InviteCopyTarget.LINK) },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun InviteFriendsDialogCard(
    partyCode: String,
    inviteLink: String,
    copyState: InviteCopyState,
    onCopyCode: () -> Unit,
    onCopyLink: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .heightIn(max = 720.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.invite_friends),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                val closeLabel = stringResource(Res.string.close)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics { contentDescription = closeLabel },
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.small),
            ) {
                Text(
                    text = stringResource(Res.string.invite_friends_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                InviteShareOption(
                    label = stringResource(Res.string.party_code),
                    value = partyCode,
                    buttonLabel = stringResource(
                        if (copyState == InviteCopyState.CODE) Res.string.code_copied else Res.string.copy_code,
                    ),
                    copied = copyState == InviteCopyState.CODE,
                    tonal = true,
                    onCopy = onCopyCode,
                )
                InviteShareOption(
                    label = stringResource(Res.string.prototype_invite_link),
                    value = inviteLink,
                    buttonLabel = stringResource(
                        if (copyState == InviteCopyState.LINK) Res.string.link_copied else Res.string.copy_link,
                    ),
                    copied = copyState == InviteCopyState.LINK,
                    tonal = false,
                    onCopy = onCopyLink,
                )
                if (copyState == InviteCopyState.ERROR) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Text(
                            text = stringResource(Res.string.copy_unavailable_details),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.done))
                }
            }
        }
    }
}

@Composable
private fun InviteShareOption(
    label: String,
    value: String,
    buttonLabel: String,
    copied: Boolean,
    tonal: Boolean,
    onCopy: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoomioDesignSystem.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        BoxWithConstraints(Modifier.padding(RoomioDesignSystem.spacing.small)) {
            val horizontal = maxWidth >= 400.dp
            val field: @Composable (Modifier) -> Unit = { fieldModifier ->
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    modifier = fieldModifier,
                    readOnly = true,
                    singleLine = true,
                    label = { Text(label) },
                )
            }
            val action: @Composable () -> Unit = {
                val icon = if (copied) {
                    Icons.Filled.Check
                } else if (tonal) {
                    Icons.Filled.ContentCopy
                } else {
                    Icons.Filled.Link
                }
                if (tonal) {
                    FilledTonalButton(
                        onClick = onCopy,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(buttonLabel)
                    }
                } else {
                    OutlinedButton(
                        onClick = onCopy,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(buttonLabel)
                    }
                }
            }

            if (horizontal) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.extraSmall),
                ) {
                    field(Modifier.weight(1f))
                    action()
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(RoomioDesignSystem.spacing.extraSmall),
                ) {
                    field(Modifier.fillMaxWidth())
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        action()
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomDialog(title: String, body: String, confirm: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(confirm) } },
    )
}

@Composable
private fun LeftRoomState(modifier: Modifier, onRejoin: () -> Unit) {
    Box(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
        Surface(shape = RoomioDesignSystem.shapes.extraLargeIncreased, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(Res.string.you_left_party), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.rejoin_room_copy), style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = onRejoin) { Text(stringResource(Res.string.rejoin)) }
            }
        }
    }
}

@Preview(name = "Owner empty · compact", widthDp = 412, heightDp = 920)
@Composable
private fun OwnerEmptyRoomPreview() = AppTheme(onThemeChanged = {}) { RoomScreen(ownerEmptyRoom) }

@Preview(name = "Owner playing · compact", widthDp = 412, heightDp = 920)
@Composable
private fun OwnerPlayingRoomPreview() = AppTheme(onThemeChanged = {}) {
    RoomScreen(ownerEmptyRoom.copy(playback = RoomPlaybackState.PLAYING))
}

@Preview(name = "Owner playing · small", widthDp = 360, heightDp = 800)
@Composable
private fun OwnerPlayingSmallPreview() = AppTheme(onThemeChanged = {}) {
    RoomScreen(ownerEmptyRoom.copy(playback = RoomPlaybackState.PLAYING))
}

@Preview(name = "Invite friends · compact", widthDp = 360, heightDp = 720)
@Composable
private fun InviteFriendsDialogCompactPreview() = AppTheme(onThemeChanged = {}) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        InviteFriendsDialogCard(
            partyCode = "NOVA-27",
            inviteLink = "https://roomio.app/?invite=NOVA-27",
            copyState = InviteCopyState.IDLE,
            onCopyCode = {},
            onCopyLink = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Invite friends · copied", widthDp = 540, heightDp = 620)
@Composable
private fun InviteFriendsDialogCopiedPreview() = AppTheme(onThemeChanged = {}) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        InviteFriendsDialogCard(
            partyCode = "NOVA-27",
            inviteLink = "https://roomio.app/?invite=NOVA-27",
            copyState = InviteCopyState.CODE,
            onCopyCode = {},
            onCopyLink = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Guest playing · desktop", widthDp = 1200, heightDp = 800)
@Composable
private fun GuestPlayingDesktopPreview() = AppTheme(onThemeChanged = {}) { RoomScreen(guestPlayingRoom) }

@Preview(name = "Guest waiting · compact", widthDp = 412, heightDp = 920)
@Composable
private fun GuestWaitingPreview() = AppTheme(onThemeChanged = {}) {
    RoomScreen(guestPlayingRoom.copy(playback = RoomPlaybackState.WAITING_FOR_OWNER))
}

@Preview(name = "Owner away · desktop", widthDp = 1200, heightDp = 800)
@Composable
private fun OwnerAwayDesktopPreview() = AppTheme(onThemeChanged = {}) {
    RoomScreen(guestPlayingRoom.copy(ownerPresent = false, playback = RoomPlaybackState.PLAYING))
}
