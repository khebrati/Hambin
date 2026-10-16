import { useEffect, useRef, useState, type CSSProperties } from 'react';
import Hls from 'hls.js';
import { FastForward, Fullscreen, Minimize2, Pause, Play, Rewind, Volume2, VolumeX } from 'lucide-react';
import { Button, IconButton } from './Material';
import type { StreamStatus } from '../types';

export interface VideoPlayerProps {
  url: string;
  sessionId: string;
  onReport: (positionMs: number, playing: boolean) => void;
  onNotify: (message: string) => void;
  syncTarget?: { requestId: string; positionMs: number };
  onState?: (state: StreamStatus) => void;
}

/** Synchronization is explicit and forward-only, including when duration changes. */
export function forwardSyncPosition(current: number, targetMs: number, end: number): number | null {
  if (!Number.isFinite(current) || !Number.isFinite(targetMs) || targetMs < 0) return null;
  const target = Math.min(targetMs / 1000, Number.isFinite(end) ? end : Infinity);
  return target > current ? target : null;
}

export function playbackReport(media: Pick<HTMLVideoElement, 'readyState' | 'currentTime' | 'paused' | 'ended' | 'seeking' | 'error'>) {
  if (media.readyState < 2 || media.seeking || media.error || !Number.isFinite(media.currentTime)) return null;
  return { positionMs: Math.max(0, Math.round(media.currentTime * 1000)), playing: !media.paused && !media.ended && media.readyState >= 3 };
}

function time(seconds: number) {
  if (!Number.isFinite(seconds)) return '—';
  const n = Math.max(0, Math.floor(seconds));
  return n >= 3600 ? `${Math.floor(n / 3600)}:${String(Math.floor(n / 60) % 60).padStart(2, '0')}:${String(n % 60).padStart(2, '0')}` : `${Math.floor(n / 60)}:${String(n % 60).padStart(2, '0')}`;
}

export function VideoPlayer(props: VideoPlayerProps) {
  // Remount the entire media lifecycle on stream changes, including the same URL restarted.
  const previous = useRef({ session: props.sessionId, request: props.syncTarget?.requestId, blocked: undefined as string | undefined });
  if (previous.current.session !== props.sessionId) {
    previous.current = { session: props.sessionId, request: props.syncTarget?.requestId, blocked: previous.current.request };
  } else previous.current.request = props.syncTarget?.requestId;
  return <MediaPlayer key={`${props.sessionId}:${props.url}`} {...props} syncTarget={props.syncTarget?.requestId === previous.current.blocked ? undefined : props.syncTarget} />;
}

function MediaPlayer({ url, onReport, onNotify, onState, syncTarget }: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const callbacks = useRef({ onReport, onNotify, onState });
  callbacks.current = { onReport, onNotify, onState };
  const [retry, setRetry] = useState(0);
  const [status, setStatus] = useState<StreamStatus>('loading');
  const [error, setError] = useState('');
  const [blocked, setBlocked] = useState(false);
  const [position, setPosition] = useState(0);
  const [duration, setDuration] = useState(NaN);
  const [buffered, setBuffered] = useState(0);
  const [live, setLive] = useState(false);
  const [ready, setReady] = useState(false);
  const [paused, setPaused] = useState(true);
  const [volume, setVolume] = useState(1);
  const [muted, setMuted] = useState(false);
  const [fullscreen, setFullscreen] = useState(false);
  const [audio, setAudio] = useState<{ id: number; name: string }[]>([]);
  const [subtitles, setSubtitles] = useState<{ id: number; name: string }[]>([]);
  const [audioId, setAudioId] = useState(-1);
  const [subtitleId, setSubtitleId] = useState('off');
  const [localSubtitle, setLocalSubtitle] = useState<{ url: string; name: string } | null>(null);
  const appliedRequest = useRef<string | undefined>(undefined);

  useEffect(() => { callbacks.current.onState?.(status); }, [status]);
  useEffect(() => () => { if (localSubtitle) URL.revokeObjectURL(localSubtitle.url); }, [localSubtitle]);
  useEffect(() => {
    const change = () => setFullscreen(document.fullscreenElement === containerRef.current);
    document.addEventListener('fullscreenchange', change);
    return () => document.removeEventListener('fullscreenchange', change);
  }, []);

  useEffect(() => {
    const video = videoRef.current!;
    let active = true;
    let failed = false;
    setStatus('loading'); setReady(false); setError(''); setBlocked(false); setPosition(0); setDuration(NaN); setBuffered(0); setLive(false); setAudio([]); setSubtitles([]);
    const fail = (message: string) => { if (active) { failed = true; setError(message); setStatus('error'); setReady(false); } };
    const update = () => {
      if (!active) return;
      setPosition(video.currentTime); setDuration(video.duration); setPaused(video.paused);
      if (video.duration === Infinity) setLive(true);
      setBuffered(video.buffered.length ? video.buffered.end(video.buffered.length - 1) : 0);
    };
    const autoplay = () => {
      void video.play().catch((reason: unknown) => {
        if (!active) return;
        if (reason instanceof DOMException && reason.name === 'AbortError') return;
        if (reason instanceof DOMException && reason.name === 'NotAllowedError') { setBlocked(true); setStatus('paused'); }
        else fail('This video could not start. Check its format and connection, then retry.');
      });
    };
    const canplay = () => { setReady(true); setStatus(video.paused ? 'paused' : 'playing'); update(); };
    const playing = () => { setBlocked(false); setPaused(false); setStatus('playing'); };
    const pause = () => { setPaused(true); if (!video.error) setStatus('paused'); };
    const waiting = () => { if (!video.paused) setStatus('buffering'); };
    const mediaError = () => fail(navigator.onLine ? 'Unable to load video. The link may be unavailable, blocked by cross-origin rules, or use an unsupported format.' : 'You are offline. Reconnect, then retry the video.');
    const volumeChange = () => { setVolume(video.volume); setMuted(video.muted); };
    const listeners: [string, EventListener][] = [['timeupdate', update], ['durationchange', update], ['progress', update], ['canplay', canplay], ['playing', playing], ['pause', pause], ['ended', pause], ['waiting', waiting], ['error', mediaError], ['volumechange', volumeChange], ['loadedmetadata', autoplay]];
    listeners.forEach(([event, listener]) => video.addEventListener(event, listener));
    let hls: Hls | null = null;
    const isHls = /\.m3u8(?:$|[?#])/i.test(url);
    if (isHls && !video.canPlayType('application/vnd.apple.mpegurl') && Hls.isSupported()) {
      hls = new Hls(); hlsRef.current = hls;
      hls.on(Hls.Events.ERROR, (_event, data) => { if (data.fatal) fail('Unable to play this HLS stream. Check the connection, cross-origin access, and supported codecs, then retry.'); });
      hls.on(Hls.Events.LEVEL_LOADED, (_event, data) => setLive(data.details.live));
      const tracks = () => { if (!hls) return; setAudio(hls.audioTracks.map((track, id) => ({ id, name: track.name || track.lang || `Audio ${id + 1}` }))); setSubtitles(hls.subtitleTracks.map((track, id) => ({ id, name: track.name || track.lang || `Subtitles ${id + 1}` }))); setAudioId(hls.audioTrack); };
      hls.on(Hls.Events.AUDIO_TRACKS_UPDATED, tracks); hls.on(Hls.Events.SUBTITLE_TRACKS_UPDATED, tracks);
      hls.on(Hls.Events.AUDIO_TRACK_SWITCHED, (_event, data) => setAudioId(data.id));
      hls.loadSource(url); hls.attachMedia(video);
    } else { video.src = url; video.load(); }
    const timer = window.setInterval(() => { const report = playbackReport(video); if (active && !failed && report) callbacks.current.onReport(report.positionMs, report.playing); }, 1000);
    return () => {
      active = false; window.clearInterval(timer); listeners.forEach(([event, listener]) => video.removeEventListener(event, listener));
      hls?.destroy(); hlsRef.current = null; video.pause(); video.removeAttribute('src'); video.load();
    };
  }, [url, retry]);

  useEffect(() => {
    const video = videoRef.current;
    if (!syncTarget || !video || !ready || appliedRequest.current === syncTarget.requestId) return;
    appliedRequest.current = syncTarget.requestId;
    const end = live && video.seekable.length ? video.seekable.end(video.seekable.length - 1) : video.duration;
    const next = forwardSyncPosition(video.currentTime, syncTarget.positionMs, end);
    if (next !== null) {
      if (live && (!video.seekable.length || next < video.seekable.start(0))) { callbacks.current.onNotify('That position is outside the live window.'); return; }
      try { video.currentTime = next; } catch { callbacks.current.onNotify('This video cannot seek to that position.'); }
    }
  }, [syncTarget, ready, live]);

  const play = async () => {
    try { await videoRef.current?.play(); setBlocked(false); }
    catch { setBlocked(true); callbacks.current.onNotify('Playback could not start. Try Play again or retry the video.'); }
  };
  const seek = (value: number) => {
    const video = videoRef.current;
    if (!video || !ready || !Number.isFinite(video.duration) || live) return;
    try { video.currentTime = Math.max(0, Math.min(value, video.duration)); setPosition(video.currentTime); }
    catch { callbacks.current.onNotify('Seeking is unavailable for this video.'); }
  };
  const selectSubtitle = (value: string) => {
    setSubtitleId(value);
    if (hlsRef.current) hlsRef.current.subtitleTrack = value.startsWith('hls:') ? Number(value.slice(4)) : -1;
    const video = videoRef.current;
    if (video) Array.from(video.textTracks).forEach((track) => { track.mode = value === 'local' && track.label === localSubtitle?.name ? 'showing' : 'disabled'; });
  };
  const seekable = ready && Number.isFinite(duration) && duration > 0 && !live;
  return <div ref={containerRef} className="cinema-player" aria-label="Video player">
    <div className="cinema-player__visual">
      <video ref={videoRef} playsInline preload="metadata" aria-label="Room video" style={{ width: '100%', height: '100%', objectFit: 'contain' }}>
        {localSubtitle && <track key={localSubtitle.url} kind="subtitles" src={localSubtitle.url} label={localSubtitle.name} default onLoad={(event) => { event.currentTarget.track.mode = subtitleId === 'local' ? 'showing' : 'disabled'; }} onError={() => onNotify('Unable to read subtitles. Choose a valid WebVTT (.vtt) file.')} />}
      </video>
      {error ? <div className="player-buffering" role="alert"><span>{error}</span><Button onClick={() => setRetry(value => value + 1)}>Retry video</Button></div>
        : status === 'loading' || status === 'buffering' ? <div className="player-buffering" role="status"><span className="md-loading__shape" aria-hidden="true" /><span>{status === 'loading' ? 'Loading video' : 'Buffering'}</span></div>
        : paused ? <button className="player-big-play" aria-label="Play video" onClick={() => void play()}><Play size={34} aria-hidden="true" /></button> : null}
    </div>
    <div className="player-controls">
      {blocked && <p role="status">Select Play to start the video on this device.</p>}
      <label className="player-progress"><span className="sr-only">Playback position</span><input type="range" min={0} max={seekable ? duration : 1} step="0.1" value={seekable ? Math.min(position, duration) : 0} disabled={!seekable} onChange={event => seek(Number(event.target.value))} aria-valuetext={time(position)} style={{ '--range-progress': `${seekable ? position / duration * 100 : 0}%` } as CSSProperties} /></label>
      <div className="player-controls__row">
        <span className="player-time">{time(position)} <span>/ {live ? 'Live' : time(duration)}</span><span className="sr-only"> Buffered to {time(buffered)}</span></span>
        <div className="transport" role="group" aria-label="Playback controls">
          <IconButton icon={Rewind} label="Rewind 15 seconds" disabled={!seekable} onClick={() => seek(position - 15)} />
          <IconButton icon={paused ? Play : Pause} label={paused ? 'Play' : 'Pause'} variant="filled" disabled={!!error} onClick={() => paused ? void play() : videoRef.current?.pause()} />
          <IconButton icon={FastForward} label="Fast-forward 15 seconds" disabled={!seekable} onClick={() => seek(position + 15)} />
        </div>
        <div className="volume-control">
          <IconButton icon={muted || volume === 0 ? VolumeX : Volume2} label={muted || volume === 0 ? 'Unmute video' : 'Mute video'} onClick={() => { const video = videoRef.current!; if (video.muted || !video.volume) { video.muted = false; if (!video.volume) video.volume = 1; } else video.muted = true; }} />
          <label><span className="sr-only">Volume</span><input type="range" min={0} max={100} value={muted ? 0 : volume * 100} onChange={event => { const video = videoRef.current!; video.volume = Number(event.target.value) / 100; video.muted = false; }} style={{ '--range-progress': `${muted ? 0 : volume * 100}%` } as CSSProperties} /></label>
          <IconButton icon={fullscreen ? Minimize2 : Fullscreen} label={fullscreen ? 'Exit fullscreen' : 'Enter fullscreen'} aria-pressed={fullscreen} onClick={() => { void (async () => { try { if (document.fullscreenElement === containerRef.current) await document.exitFullscreen(); else if (containerRef.current?.requestFullscreen) await containerRef.current.requestFullscreen(); else onNotify('Fullscreen is unavailable in this browser.'); } catch { onNotify('Fullscreen is unavailable in this browser.'); } })(); }} />
        </div>
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center' }}>
        <label>Subtitles <select value={subtitleId} onChange={event => selectSubtitle(event.target.value)}><option value="off">Off</option>{subtitles.map(track => <option key={track.id} value={`hls:${track.id}`}>{track.name}</option>)}{localSubtitle && <option value="local">{localSubtitle.name}</option>}</select></label>
        <label>Local subtitles (.vtt) <input type="file" accept=".vtt,text/vtt" style={{ maxWidth: '100%' }} onChange={async event => {
          const file = event.target.files?.[0]; event.target.value = ''; if (!file) return;
          if (!/\.vtt$/i.test(file.name) || file.size > 5_000_000) { onNotify('Choose a WebVTT (.vtt) file smaller than 5 MB.'); return; }
          try { const content = await file.text(); if (!/^\uFEFF?WEBVTT(?:[ \t\r\n]|$)/.test(content)) { onNotify('This file is not valid WebVTT.'); return; } if (!videoRef.current) return; setLocalSubtitle({ url: URL.createObjectURL(new Blob([content], { type: 'text/vtt' })), name: file.name }); if (hlsRef.current) hlsRef.current.subtitleTrack = -1; setSubtitleId('local'); } catch { onNotify('Unable to read that subtitle file.'); }
        }} /></label>
        {audio.length > 1 && <label>Audio <select value={audioId} onChange={event => { const id = Number(event.target.value); if (hlsRef.current) hlsRef.current.audioTrack = id; setAudioId(id); }}>{audio.map(track => <option key={track.id} value={track.id}>{track.name}</option>)}</select></label>}
      </div>
    </div>
  </div>;
}
