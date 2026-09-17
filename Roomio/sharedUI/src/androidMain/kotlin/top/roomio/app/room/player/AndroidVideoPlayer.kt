package top.roomio.app.room.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import kotlinx.coroutines.delay

@Composable
internal actual fun rememberPlatformVideoPlayer(
    onStateChanged: (VideoPlayerState) -> Unit,
): VideoPlayerHandle? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStateChanged by rememberUpdatedState(onStateChanged)

    val player = remember(context) {
        ExoPlayer.Builder(context.applicationContext).build()
    }

    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                currentOnStateChanged(player.toVideoPlayerState())
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                currentOnStateChanged(player.toVideoPlayerState())
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                currentOnStateChanged(player.toVideoPlayerState())
            }

            override fun onPlayerError(error: PlaybackException) {
                currentOnStateChanged(
                    player.toVideoPlayerState().copy(
                        status = VideoPlayerStatus.ERROR,
                        error = error.errorCodeName,
                    ),
                )
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                player.pause()
            }
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (player.mediaItemCount > 0) {
                currentOnStateChanged(player.toVideoPlayerState())
            }
            delay(POSITION_POLL_MS)
        }
    }

    return remember(player) { ExoPlayerVideoPlayerHandle(player) }
}

@OptIn(UnstableApi::class)
@Composable
internal actual fun VideoPlayerSurface(
    handle: VideoPlayerHandle?,
    modifier: Modifier,
) {
    val player = (handle as? ExoPlayerVideoPlayerHandle)?.player ?: return
    PlayerSurface(
        player = player,
        modifier = modifier,
    )
}

private const val POSITION_POLL_MS = 500L

internal class ExoPlayerVideoPlayerHandle(
    internal val player: ExoPlayer,
) : VideoPlayerHandle {
    override fun load(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun seekBy(deltaMs: Long) {
        player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0L))
    }

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    override fun stop() {
        player.stop()
        player.clearMediaItems()
    }
}

private fun ExoPlayer.toVideoPlayerState(): VideoPlayerState {
    val rawDuration = duration
    val isLive = isCurrentMediaItemLive || rawDuration == C.TIME_UNSET
    val durationMs = if (rawDuration == C.TIME_UNSET || rawDuration < 0L) 0L else rawDuration
    val status = when {
        playerError != null -> VideoPlayerStatus.ERROR
        playbackState == Player.STATE_BUFFERING -> VideoPlayerStatus.BUFFERING
        playbackState == Player.STATE_READY && playWhenReady -> VideoPlayerStatus.PLAYING
        playbackState == Player.STATE_READY -> VideoPlayerStatus.PAUSED
        playbackState == Player.STATE_ENDED -> VideoPlayerStatus.ENDED
        else -> VideoPlayerStatus.IDLE
    }
    val aspectRatio = videoSize.let { size ->
        if (size.height > 0) {
            (size.width * size.pixelWidthHeightRatio) / size.height
        } else {
            0f
        }
    }
    return VideoPlayerState(
        status = status,
        positionMs = currentPosition.coerceAtLeast(0L),
        durationMs = durationMs,
        bufferedMs = bufferedPosition.coerceAtLeast(0L),
        isLive = isLive,
        videoAspectRatio = aspectRatio,
        error = playerError?.errorCodeName,
    )
}
