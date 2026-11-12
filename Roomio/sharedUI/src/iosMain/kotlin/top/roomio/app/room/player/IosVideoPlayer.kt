@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package top.roomio.app.room.player

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.coroutines.delay
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.Foundation.NSValue
import platform.UIKit.UIView

/** Native AVPlayer surface and real buffered ranges, shared by all iOS entry points. */
internal class IosPlayer(private val changed:(VideoPlayerState)->Unit):VideoPlayerHandle {
    val player=AVPlayer()
    val layer=AVPlayerLayer.playerLayerWithPlayer(player)
    private var seeking=false
    private var closed=false
    override fun load(url:String){
        val source=NSURL.URLWithString(url)?:return
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem.playerItemWithURL(source))
    }
    override fun play(){player.play()}
    override fun pause(){player.pause()}
    override fun togglePlayPause(){if(player.rate>0)pause()else play()}
    override fun seekTo(positionMs:Long){
        seeking=true
        player.seekToTime(CMTimeMakeWithSeconds(positionMs.coerceAtLeast(0)/1000.0,600),toleranceBefore=kCMTimeZero,toleranceAfter=kCMTimeZero,completionHandler={seeking=false})
    }
    override fun seekBy(deltaMs:Long)=seekTo((CMTimeGetSeconds(player.currentTime())*1000).toLong()+deltaMs)
    override fun setVolume(volume:Float){player.volume=volume.coerceIn(0f,1f)}
    override fun stop(){player.pause();player.replaceCurrentItemWithPlayerItem(null)}
    override fun selectSubtitle(trackId:String?){
        val item=player.currentItem?:return
        val group=item.asset.mediaSelectionGroupForMediaCharacteristic(AVMediaCharacteristicLegible)?:return
        val option=trackId?.toIntOrNull()?.let{group.options.getOrNull(it) as? AVMediaSelectionOption}
        item.selectMediaOption(option,inMediaSelectionGroup=group)
    }
    override fun selectAudioTrack(trackId:String?){
        val item=player.currentItem?:return
        val group=item.asset.mediaSelectionGroupForMediaCharacteristic(AVMediaCharacteristicAudible)?:return
        if(trackId==null)item.selectMediaOptionAutomaticallyInMediaSelectionGroup(group)
        else item.selectMediaOption(group.options.getOrNull(trackId.toIntOrNull()?:return) as? AVMediaSelectionOption,inMediaSelectionGroup=group)
    }
    // External subtitle import is not exposed on iOS until a document-picker adapter exists.
    override fun addSubtitle(file:SubtitleFile){selectSubtitle(null)}
    fun publish(){
        if(closed)return
        val item=player.currentItem?:return
        val seconds=CMTimeGetSeconds(player.currentTime());val duration=CMTimeGetSeconds(item.duration)
        var buffered=seconds
        for(entry in item.loadedTimeRanges){val range=(entry as? NSValue)?.CMTimeRangeValue?:continue;val start=range.useContents { CMTimeGetSeconds(start) };val end=CMTimeGetSeconds(CMTimeRangeGetEnd(range));if(start<=seconds+.1&&end>=seconds)buffered=maxOf(buffered,end)}
        val ready=item.status==AVPlayerItemStatusReadyToPlay
        val status=when{item.status==AVPlayerItemStatusFailed->VideoPlayerStatus.ERROR;!ready||seeking->VideoPlayerStatus.BUFFERING;player.rate>0->VideoPlayerStatus.PLAYING;else->VideoPlayerStatus.PAUSED}
        val audio=item.asset.mediaSelectionGroupForMediaCharacteristic(AVMediaCharacteristicAudible)
        val subtitles=item.asset.mediaSelectionGroupForMediaCharacteristic(AVMediaCharacteristicLegible)
        changed(VideoPlayerState(status,if(seconds.isFinite())(seconds*1000).toLong()else 0,if(duration.isFinite())(duration*1000).toLong()else 0,if(buffered.isFinite())(buffered*1000).toLong()else 0,isLive=ready&&!duration.isFinite(),error=item.error?.localizedDescription,
            audioTracks=audio?.options?.mapIndexedNotNull{i,v->(v as? AVMediaSelectionOption)?.let{AudioTrack(i.toString(),it.displayName)}}?:emptyList(),
            subtitles=subtitles?.options?.mapIndexedNotNull{i,v->(v as? AVMediaSelectionOption)?.let{SubtitleTrack(i.toString(),it.displayName,item.currentMediaSelection.selectedMediaOptionInMediaSelectionGroup(subtitles)==it)}}?:emptyList()))
    }
    fun close(){closed=true;stop()}
}
@Composable
internal actual fun rememberPlatformVideoPlayer(onStateChanged:(VideoPlayerState)->Unit):VideoPlayerHandle? {
    val latest by rememberUpdatedState(onStateChanged)
    val handle=remember{IosPlayer{latest(it)}}
    LaunchedEffect(handle){while(true){handle.publish();delay(100)}}
    DisposableEffect(handle){onDispose{handle.close()}}
    return handle
}
@Composable
internal actual fun VideoPlayerSurface(handle:VideoPlayerHandle?,modifier:Modifier){
    val native=handle as? IosPlayer?:return
    UIKitView(factory={object:UIView(frame=CGRectZero.readValue()){
        init{layer.addSublayer(native.layer)}
        override fun layoutSubviews(){super.layoutSubviews();native.layer.frame=bounds}
    }},modifier=modifier)
}
@Composable
internal actual fun rememberPlatformSubtitlePicker(onSubtitlePicked:(SubtitleFile)->Unit):(() -> Unit)?=null
