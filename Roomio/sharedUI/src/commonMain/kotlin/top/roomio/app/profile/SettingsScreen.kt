package top.roomio.app.profile

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import top.roomio.domain.profile.ThemeColor
import top.roomio.app.theme.seed
import top.roomio.app.theme.label
import roomio.sharedui.generated.resources.color_theme
import roomio.sharedui.generated.resources.color_theme_description
import roomio.sharedui.generated.resources.color_save_error
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import roomio.sharedui.generated.resources.Res
import roomio.sharedui.generated.resources.app_language
import roomio.sharedui.generated.resources.avatar
import roomio.sharedui.generated.resources.avatar_description
import roomio.sharedui.generated.resources.back
import roomio.sharedui.generated.resources.choose_an_avatar
import roomio.sharedui.generated.resources.english
import roomio.sharedui.generated.resources.language
import roomio.sharedui.generated.resources.language_description
import roomio.sharedui.generated.resources.profile_preview
import roomio.sharedui.generated.resources.ready_to_watch
import roomio.sharedui.generated.resources.save_profile
import roomio.sharedui.generated.resources.save_profile_error
import roomio.sharedui.generated.resources.shown_inside_every_party
import roomio.sharedui.generated.resources.theme
import roomio.sharedui.generated.resources.your_profile
import top.roomio.app.theme.RoomioLightColorScheme
import top.roomio.app.theme.AppTheme
import top.roomio.app.theme.LocalThemeIsDark
import top.roomio.app.theme.RoomioDesignSystem
import top.roomio.app.theme.RoomioTheme

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { ProfileTopBar(onBack = { onAction(SettingsAction.BackClicked) }) },
        bottomBar = {
            SaveProfileBar(
                enabled = true,
                saveFailed = state.saveFailed,
                onSave = { onAction(SettingsAction.SaveClicked) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                )
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 712.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                ProfileSpotlight(
                    name = state.displayName,
                    avatar = state.draftAvatar,
                )
                ProfileSection(
                    number = "01",
                    title = stringResource(Res.string.avatar),
                    description = stringResource(Res.string.avatar_description),
                ) {
                    AvatarPicker(
                        selectedAvatar = state.draftAvatar,
                        onSelected = { onAction(SettingsAction.AvatarSelected(it)) },
                    )
                }
                ProfileSection(
                    number = "02",
                    title = stringResource(Res.string.color_theme),
                    description = stringResource(Res.string.color_theme_description),
                ) {
                    Column(modifier = Modifier.selectableGroup()) {
                        ThemeColor.entries.forEach { color ->
                            val isSelected = color == state.themeColor
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .selectable(selected = isSelected, role = Role.RadioButton,
                                        onClick = { onAction(SettingsAction.ColorSelected(color)) })
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                    .heightIn(min = 56.dp).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(Modifier.size(24.dp).background(color.seed ?: RoomioLightColorScheme.primary, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                                Text(stringResource(color.label), modifier = Modifier.weight(1f),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                if (isSelected) Icon(Icons.Default.Check, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        if (state.colorSaveFailed) Text(stringResource(Res.string.color_save_error), color = MaterialTheme.colorScheme.error)
                    }
                }
                ProfileSection(
                    number = "03",
                    title = stringResource(Res.string.language),
                    description = stringResource(Res.string.language_description),
                    showDivider = false,
                ) {
                    Column(Modifier.selectableGroup().semantics {
                        contentDescription = "English / فارسی"
                    }) {
                        top.roomio.domain.profile.AppLanguage.entries.forEach { language ->
                            val selected = state.draftLanguage == language
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                    .selectable(selected = selected, role = Role.RadioButton,
                                        onClick = { onAction(SettingsAction.LanguageSelected(language)) })
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                androidx.compose.material3.RadioButton(selected = selected, onClick = null)
                                Text(if (language == top.roomio.domain.profile.AppLanguage.PERSIAN) "فارسی" else "English")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTopBar(onBack: () -> Unit) {
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
                .height(68.dp),
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
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.your_profile),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.shown_inside_every_party),
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
private fun ProfileSpotlight(
    name: String,
    avatar: ProfileAvatar,
) {
    val previewLabel = stringResource(Res.string.profile_preview)
    val avatarDescription = name
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$previewLabel: $avatarDescription"
            },
        shape = RoomioDesignSystem.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatarImage(
                avatar = avatar,
                description = avatarDescription,
                selected = false,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(Res.string.ready_to_watch),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProfileSection(
    number: String,
    title: String,
    description: String,
    showDivider: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.size(width = 36.dp, height = 28.dp),
                shape = RoomioDesignSystem.shapes.full,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        content()
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun AvatarPicker(
    selectedAvatar: ProfileAvatar,
    onSelected: (ProfileAvatar) -> Unit,
) {
    val pickerLabel = stringResource(Res.string.choose_an_avatar)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 460.dp) 2 else 3
        val gap = 12.dp
        val itemWidth = (maxWidth - gap * (columns - 1)) / columns
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = pickerLabel },
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            maxItemsInEachRow = columns,
        ) {
            ProfileAvatar.entries.forEach { avatar ->
                AvatarChoice(
                    avatar = avatar,
                    selected = avatar == selectedAvatar,
                    onClick = { onSelected(avatar) },
                    baseArtworkSize = minOf(itemWidth - 16.dp, 124.dp),
                    selectedArtworkSize = minOf(itemWidth - 4.dp, 144.dp),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AvatarChoice(
    avatar: ProfileAvatar,
    selected: Boolean,
    onClick: () -> Unit,
    baseArtworkSize: androidx.compose.ui.unit.Dp,
    selectedArtworkSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = selectedArtworkSize + 44.dp)
            .clickable(
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                this.selected = selected
            },
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box {
                ProfileAvatarImage(
                    avatar = avatar,
                    description = avatar.localizedLabel,
                    selected = selected,
                    baseArtworkSize = baseArtworkSize,
                    selectedArtworkSize = selectedArtworkSize,
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape,
                            )
                            .padding(3.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = avatar.localizedLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun ProfileAvatarImage(
    avatar: ProfileAvatar,
    description: String,
    selected: Boolean,
    baseArtworkSize: androidx.compose.ui.unit.Dp = 84.dp,
    selectedArtworkSize: androidx.compose.ui.unit.Dp = 96.dp,
) {
    val artworkSize by animateDpAsState(
        targetValue = if (selected) selectedArtworkSize else baseArtworkSize,
    )
    val frameSize by animateDpAsState(
        targetValue = (if (selected) selectedArtworkSize else baseArtworkSize) + 4.dp,
    )

    Box(
        modifier = Modifier.size(selectedArtworkSize + 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(frameSize),
            shape = CircleShape,
            color = if (selected) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
            content = {},
        )
        Image(
            painter = painterResource(avatar.resource),
            contentDescription = description,
            modifier = Modifier
                .size(artworkSize)
                .zIndex(1f)
                .graphicsLayer {
                    translationY = if (selected) -3.dp.toPx() else 0f
                    shadowElevation = if (selected) 5.dp.toPx() else 0f
                    shape = CircleShape
                    clip = true
                }
                .border(
                    width = if (selected) 3.dp else 2.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    },
                    shape = CircleShape,
                ),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun SaveProfileBar(
    enabled: Boolean,
    saveFailed: Boolean,
    onSave: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            ),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 728.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (saveFailed) {
                        Text(
                            text = stringResource(Res.string.save_profile_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        enabled = enabled,
                    ) {
                        Text(stringResource(Res.string.save_profile))
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun SettingsCompactPreview() {
    AppTheme(onThemeChanged = {}) {
        SettingsScreen(
            state = SettingsUiState(ProfileAvatar.COMET),
            onAction = {},
        )
    }
}

@Preview(widthDp = 1205, heightDp = 805)
@Composable
private fun SettingsExpandedPreview() {
    AppTheme(onThemeChanged = {}) {
        SettingsScreen(
            state = SettingsUiState(ProfileAvatar.COMET),
            onAction = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 1.5f)
@Composable
private fun SettingsLargeTextPreview() {
    RoomioTheme(darkTheme = true) {
        SettingsScreen(
            state = SettingsUiState(ProfileAvatar.BERRY),
            onAction = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun SettingsSaveErrorPreview() {
    AppTheme(onThemeChanged = {}) {
        SettingsScreen(
            state = SettingsUiState(
                draftAvatar = ProfileAvatar.COMET,
                saveFailed = true,
            ),
            onAction = {},
        )
    }
}
