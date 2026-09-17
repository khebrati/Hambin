package top.roomio.app.room.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal enum class VideoPlayerStatus {
    IDLE,
    BUFFERING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR,
}

internal data class VideoPlayerState(
    val status: VideoPlayerStatus = VideoPlayerStatus.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val isLive: Boolean = false,
    val error: String? = null,
) {
    val hasDuration: Boolean
        get() = durationMs > 0L
}

internal interface VideoPlayerHandle {
    fun load(url: String)

    fun play()

    fun pause()

    fun togglePlayPause()

    fun seekTo(positionMs: Long)

    fun seekBy(deltaMs: Long)

    fun setVolume(volume: Float)

    fun stop()
}

@Composable
internal expect fun rememberPlatformVideoPlayer(
    onStateChanged: (VideoPlayerState) -> Unit,
): VideoPlayerHandle?

@Composable
internal expect fun VideoPlayerSurface(
    handle: VideoPlayerHandle?,
    modifier: Modifier,
)
