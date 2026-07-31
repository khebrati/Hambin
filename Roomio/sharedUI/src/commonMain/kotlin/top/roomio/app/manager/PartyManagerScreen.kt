package top.roomio.app.manager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import roomio.sharedui.generated.resources.Res
import roomio.sharedui.generated.resources.a_friend_has_the_code
import roomio.sharedui.generated.resources.back
import roomio.sharedui.generated.resources.create
import roomio.sharedui.generated.resources.create_party_description
import roomio.sharedui.generated.resources.finding_party
import roomio.sharedui.generated.resources.join
import roomio.sharedui.generated.resources.manager_create_party
import roomio.sharedui.generated.resources.open_room_for_movie_night
import roomio.sharedui.generated.resources.opening_your_room
import roomio.sharedui.generated.resources.party_code
import roomio.sharedui.generated.resources.party_code_error
import roomio.sharedui.generated.resources.party_code_hints
import roomio.sharedui.generated.resources.party_manager
import roomio.sharedui.generated.resources.party_manager_subtitle
import roomio.sharedui.generated.resources.preview_party
import roomio.sharedui.generated.resources.step_into_their_room
import roomio.sharedui.generated.resources.theme
import roomio.sharedui.generated.resources.you_will_be_the_host
import top.roomio.app.theme.AppTheme
import top.roomio.app.theme.LocalThemeIsDark
import top.roomio.app.theme.RoomioDesignSystem
import top.roomio.app.theme.RoomioTheme

internal enum class ManagerMode { CREATE, JOIN }

@Composable
internal fun PartyManagerScreen(
    state: PartyManagerUiState,
    onAction: (PartyManagerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { ManagerTopBar(onBack = { onAction(PartyManagerAction.BackClicked) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                )
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 592.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                ManagerTabs(
                    selectedMode = state.mode,
                    enabled = !state.isLoading,
                    onSelected = { onAction(PartyManagerAction.ModeSelected(it)) },
                )
                when (state.mode) {
                    ManagerMode.CREATE -> CreatePartyPanel(
                        loading = state.isLoading,
                        onCreate = { onAction(PartyManagerAction.CreateClicked) },
                    )
                    ManagerMode.JOIN -> JoinPartyPanel(
                        code = state.code,
                        loading = state.isLoading,
                        canPreview = state.canPreview,
                        codeError = state.hasCodeError,
                        onCodeChange = { onAction(PartyManagerAction.CodeChanged(it)) },
                        onPreview = { onAction(PartyManagerAction.PreviewClicked) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagerTopBar(onBack: () -> Unit) {
    val themeIsDark = LocalThemeIsDark.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 1120.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.party_manager),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.party_manager_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { themeIsDark.value = !themeIsDark.value },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (themeIsDark.value) {
                            Icons.Filled.LightMode
                        } else {
                            Icons.Filled.DarkMode
                        },
                        contentDescription = stringResource(Res.string.theme),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ManagerTabs(
    selectedMode: ManagerMode,
    enabled: Boolean,
    onSelected: (ManagerMode) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoomioDesignSystem.shapes.full,
            ),
        shape = RoomioDesignSystem.shapes.full,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            ManagerTab(
                label = stringResource(Res.string.create),
                selected = selectedMode == ManagerMode.CREATE,
                enabled = enabled,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = { onSelected(ManagerMode.CREATE) },
                modifier = Modifier.weight(1f),
            )
            ManagerTab(
                label = stringResource(Res.string.join),
                selected = selectedMode == ManagerMode.JOIN,
                enabled = enabled,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.ConfirmationNumber,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = { onSelected(ManagerMode.JOIN) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ManagerTab(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 44.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        enabled = enabled,
        shape = RoomioDesignSystem.shapes.full,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.size(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CreatePartyPanel(
    loading: Boolean,
    onCreate: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoomioDesignSystem.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CinemaManagerVisual(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Text(
                text = stringResource(Res.string.you_will_be_the_host).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(Res.string.open_room_for_movie_night),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.create_party_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (loading) {
                LoadingAction(stringResource(Res.string.opening_your_room))
            } else {
                Button(
                    onClick = onCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MeetingRoom,
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.manager_create_party))
                }
            }
        }
    }
}

@Composable
private fun JoinPartyPanel(
    code: String,
    loading: Boolean,
    canPreview: Boolean,
    codeError: Boolean,
    onCodeChange: (String) -> Unit,
    onPreview: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoomioDesignSystem.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.a_friend_has_the_code).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(Res.string.step_into_their_room),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                singleLine = true,
                isError = codeError,
                label = { Text(stringResource(Res.string.party_code)) },
                placeholder = { Text("MOON-42") },
                supportingText = if (codeError) {
                    { Text(stringResource(Res.string.party_code_error)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
            )
            if (loading) {
                LoadingAction(stringResource(Res.string.finding_party))
            } else {
                Button(
                    onClick = onPreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    enabled = canPreview,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ConfirmationNumber,
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.preview_party))
                }
            }
            Text(
                text = stringResource(Res.string.party_code_hints),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingAction(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 3.dp,
        )
        Spacer(Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CinemaManagerVisual(modifier: Modifier = Modifier) {
    val colors = RoomioDesignSystem.colors
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        drawRoundRect(
            color = colors.mediaSurface,
            cornerRadius = CornerRadius(20.dp.toPx()),
        )
        val screenWidth = size.width * .60f
        val screenHeight = size.height * .42f
        val screenLeft = (size.width - screenWidth) / 2f
        val screenTop = size.height * .14f
        drawRoundRect(
            color = colors.mediaOutline,
            topLeft = Offset(screenLeft, screenTop),
            size = androidx.compose.ui.geometry.Size(screenWidth, screenHeight),
            cornerRadius = CornerRadius(7.dp.toPx()),
        )
        drawRoundRect(
            color = colors.sceneSky,
            topLeft = Offset(screenLeft + 5.dp.toPx(), screenTop + 5.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(
                screenWidth - 10.dp.toPx(),
                screenHeight - 10.dp.toPx(),
            ),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
        drawCircle(
            color = colors.scenePlanet,
            radius = 14.dp.toPx(),
            center = Offset(screenLeft + screenWidth * .89f, screenTop + screenHeight * .42f),
        )

        val seatWidth = 58.dp.toPx()
        val seatHeight = 55.dp.toPx()
        val seatTop = size.height - 72.dp.toPx()
        val firstSeatLeft = size.width / 2f - seatWidth * 1.4f
        listOf(primary, secondary, tertiary).forEachIndexed { index, color ->
            drawRoundRect(
                color = color,
                topLeft = Offset(firstSeatLeft + index * seatWidth * .9f, seatTop),
                size = androidx.compose.ui.geometry.Size(seatWidth, seatHeight),
                cornerRadius = CornerRadius(15.dp.toPx(), 10.dp.toPx()),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun PartyManagerCompactPreview() {
    AppTheme(onThemeChanged = {}) {
        PartyManagerScreen(
            state = PartyManagerUiState(mode = ManagerMode.CREATE),
            onAction = {},
        )
    }
}

@Preview(widthDp = 1222, heightDp = 710)
@Composable
private fun PartyManagerExpandedPreview() {
    AppTheme(onThemeChanged = {}) {
        PartyManagerScreen(
            state = PartyManagerUiState(mode = ManagerMode.CREATE),
            onAction = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun PartyManagerJoinErrorPreview() {
    RoomioTheme(darkTheme = true) {
        PartyManagerScreen(
            state = PartyManagerUiState(
                mode = ManagerMode.JOIN,
                code = "NOVA-99",
                hasCodeError = true,
            ),
            onAction = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 1.5f)
@Composable
private fun PartyManagerLoadingLargeTextPreview() {
    AppTheme(onThemeChanged = {}) {
        PartyManagerScreen(
            state = PartyManagerUiState(
                mode = ManagerMode.CREATE,
                isLoading = true,
            ),
            onAction = {},
        )
    }
}
