package top.roomio.app.room.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun rememberPlatformVideoPlayer(
    onStateChanged: (VideoPlayerState) -> Unit,
): VideoPlayerHandle? = null

@Composable
internal actual fun VideoPlayerSurface(
    handle: VideoPlayerHandle?,
    modifier: Modifier,
) = Unit

@Composable
internal actual fun rememberPlatformSubtitlePicker(
    onSubtitlePicked: (SubtitleFile) -> Unit,
): (() -> Unit)? = null
