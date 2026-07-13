import {
  Clipboard,
  ClipboardCheck,
  FastForward,
  Fullscreen,
  Link2,
  LogOut,
  Mic,
  MicOff,
  Minimize2,
  Pause,
  Play,
  RefreshCw,
  Rewind,
  Square,
  Volume1,
  Volume2,
  VolumeX,
} from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { AppBar } from '../components/AppBar';
import { ParticipantItem } from '../components/Avatar';
import {
  Button,
  Dialog,
  IconButton,
  LoadingIndicator,
  TextField,
} from '../components/Material';
import { sampleVideoUrl } from '../data/fakeRepository';
import type { Party, StreamStatus } from '../types';

const TOTAL_SECONDS = 6138;
const LEADING_POSITION = 1542;

function formatTime(value: number) {
  const safeValue = Math.max(0, Math.min(value, TOTAL_SECONDS));
  const hours = Math.floor(safeValue / 3600);
  const minutes = Math.floor((safeValue % 3600) / 60);
  const seconds = Math.floor(safeValue % 60);
  return hours > 0
    ? `${hours}:${minutes.toString().padStart(2, '0')}:${seconds
        .toString()
        .padStart(2, '0')}`
    : `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

interface PartyScreenProps {
  party: Party;
  streamStatus: StreamStatus;
  theme: 'light' | 'dark';
  aborting: boolean;
  onToggleTheme: () => void;
  onStartStream: (url: string) => void;
  onSetStreamStatus: (status: StreamStatus) => void;
  onAbort: () => Promise<void>;
  onLeave: () => void;
  onNotify: (message: string) => void;
}

export function PartyScreen({
  party,
  streamStatus,
  theme,
  aborting,
  onToggleTheme,
  onStartStream,
  onSetStreamStatus,
  onAbort,
  onLeave,
  onNotify,
}: PartyScreenProps) {
  const [urlDraft, setUrlDraft] = useState(
    streamStatus === 'error' ? 'not-a-complete-link' : '',
  );
  const [position, setPosition] = useState(1122);
  const [volume, setVolume] = useState(72);
  const [previousVolume, setPreviousVolume] = useState(72);
  const [micMuted, setMicMuted] = useState(false);
  const [leaveOpen, setLeaveOpen] = useState(false);
  const [abortOpen, setAbortOpen] = useState(false);
  const [urlOpen, setUrlOpen] = useState(false);
  const [copied, setCopied] = useState(false);

  const activeUrl = party.streamUrl || sampleVideoUrl;
  const hasPlayer = ['playing', 'paused', 'buffering'].includes(streamStatus);
  const isOwner = party.role === 'owner';

  useEffect(() => {
    if (streamStatus !== 'playing') return;
    const timer = window.setInterval(
      () => setPosition((current) => Math.min(current + 1, TOTAL_SECONDS)),
      1000,
    );
    return () => window.clearInterval(timer);
  }, [streamStatus]);

  const participants = useMemo(
    () =>
      party.participants.map((participant) =>
        participant.isSelf
          ? { ...participant, isMuted: micMuted, isSpeaking: !micMuted && participant.isSpeaking }
          : participant,
      ),
    [micMuted, party.participants],
  );

  const copyUrl = async () => {
    try {
      await navigator.clipboard.writeText(activeUrl);
    } catch {
      // Clipboard access can be blocked in local previews; feedback remains deterministic.
    }
    setCopied(true);
    onNotify('Video link copied');
    window.setTimeout(() => setCopied(false), 1600);
  };

  const pasteUrl = async () => {
    let pasted = '';
    try {
      pasted = await navigator.clipboard.readText();
    } catch {
      pasted = sampleVideoUrl;
    }
    setUrlDraft(pasted || sampleVideoUrl);
    onNotify('Video link ready');
  };

  const toggleMute = () => {
    if (volume === 0) {
      setVolume(previousVolume || 72);
    } else {
      setPreviousVolume(volume);
      setVolume(0);
    }
  };

  const syncPlayback = () => {
    setPosition(LEADING_POSITION);
    onNotify("You're caught up with the room");
  };

  return (
    <div className="screen party-screen screen-enter">
      <AppBar
        title={party.title}
        subtitle={`${party.id} · ${isOwner ? 'Host' : 'Guest'}`}
        theme={theme}
        onToggleTheme={onToggleTheme}
        actions={
          <>
            {hasPlayer ? (
              <IconButton icon={Link2} label="View video link" onClick={() => setUrlOpen(true)} />
            ) : null}
            <IconButton icon={LogOut} label="Leave party" onClick={() => setLeaveOpen(true)} />
          </>
        }
      />

      <main className="party-layout">
        <section className="cinema-column" aria-label="Cinema">
          {!hasPlayer ? (
            <StreamEntry
              status={streamStatus}
              url={urlDraft}
              onUrlChange={(value) => {
                setUrlDraft(value);
                if (streamStatus === 'error') onSetStreamStatus('empty');
              }}
              onPaste={pasteUrl}
              onStart={() => onStartStream(urlDraft)}
              isOwner={isOwner}
            />
          ) : (
            <CinemaPlayer
              status={streamStatus}
              position={position}
              volume={volume}
              onPositionChange={setPosition}
              onVolumeChange={setVolume}
              onToggleMute={toggleMute}
              onTogglePlay={() =>
                onSetStreamStatus(streamStatus === 'playing' ? 'paused' : 'playing')
              }
              onSkipBack={() => setPosition((current) => Math.max(0, current - 15))}
              onSkipForward={() =>
                setPosition((current) => Math.min(TOTAL_SECONDS, current + 15))
              }
            />
          )}

          {hasPlayer ? (
            <div className="sync-band">
              <div>
                <span className="eyebrow">Local playback</span>
                <strong>{streamStatus === 'paused' ? 'Paused for you' : 'Playing for you'}</strong>
              </div>
              <Button
                variant="tonal"
                icon={<RefreshCw size={19} aria-hidden="true" />}
                onClick={syncPlayback}
              >
                Sync to room
              </Button>
            </div>
          ) : null}

          <div className="participants-compact">
            <ParticipantStrip participants={participants} />
          </div>
        </section>

        <aside className="party-support" aria-label="Party details">
          <div className="participants-wide">
            <ParticipantStrip participants={participants} />
          </div>

          <section className="voice-panel" aria-labelledby="voice-heading">
            <div>
              <span className="eyebrow">Voice room</span>
              <h2 id="voice-heading">{micMuted ? 'Your mic is muted' : 'Your mic is on'}</h2>
            </div>
            <IconButton
              icon={micMuted ? MicOff : Mic}
              label={micMuted ? 'Unmute microphone' : 'Mute microphone'}
              variant={micMuted ? 'standard' : 'tonal'}
              selected={!micMuted}
              onClick={() => setMicMuted((current) => !current)}
            />
          </section>

          {hasPlayer ? (
            <section className="source-row" aria-label="Current video source">
              <div>
                <span>Video source</span>
                <strong>{new URL(activeUrl).pathname.split('/').pop()}</strong>
              </div>
              <IconButton
                icon={copied ? ClipboardCheck : Clipboard}
                label={copied ? 'Link copied' : 'Copy video link'}
                onClick={copyUrl}
              />
            </section>
          ) : null}

          {isOwner && hasPlayer ? (
            <Button
              variant="danger"
              icon={<Square size={18} aria-hidden="true" />}
              onClick={() => setAbortOpen(true)}
              fullWidth
            >
              End stream for everyone
            </Button>
          ) : null}
        </aside>
      </main>

      <Dialog
        open={urlOpen}
        title="Current video link"
        onClose={() => setUrlOpen(false)}
        actions={
          <Button
            variant="tonal"
            icon={copied ? <ClipboardCheck size={18} /> : <Clipboard size={18} />}
            onClick={copyUrl}
          >
            {copied ? 'Copied' : 'Copy link'}
          </Button>
        }
      >
        <label className="read-only-field">
          <span>Direct video URL</span>
          <input value={activeUrl} readOnly />
        </label>
        {!isOwner ? <p>Only the host can replace this link.</p> : null}
      </Dialog>

      <Dialog
        open={leaveOpen}
        title="Leave this party?"
        onClose={() => setLeaveOpen(false)}
        actions={
          <>
            <Button variant="text" onClick={() => setLeaveOpen(false)}>
              Stay
            </Button>
            <Button variant="danger" onClick={onLeave}>
              Leave party
            </Button>
          </>
        }
      >
        <p>Your playback will stop and you will return home.</p>
      </Dialog>

      <Dialog
        open={abortOpen}
        title="End the stream for everyone?"
        onClose={() => !aborting && setAbortOpen(false)}
        actions={
          aborting ? (
            <LoadingIndicator label="Ending stream" />
          ) : (
            <>
              <Button variant="text" onClick={() => setAbortOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="danger"
                onClick={async () => {
                  await onAbort();
                  setAbortOpen(false);
                  setUrlDraft('');
                }}
              >
                End for everyone
              </Button>
            </>
          )
        }
      >
        <p>
          This is different from pausing. The video will stop for every person in
          the room.
        </p>
      </Dialog>
    </div>
  );
}

function StreamEntry({
  status,
  url,
  onUrlChange,
  onPaste,
  onStart,
  isOwner,
}: {
  status: StreamStatus;
  url: string;
  onUrlChange: (value: string) => void;
  onPaste: () => void;
  onStart: () => void;
  isOwner: boolean;
}) {
  if (status === 'loading') {
    return (
      <div className="empty-cinema empty-cinema--loading">
        <CinemaScene quiet />
        <div className="empty-cinema__content">
          <LoadingIndicator label="Preparing the cinema" />
          <p>Opening the video on each device.</p>
        </div>
      </div>
    );
  }

  if (!isOwner) {
    return (
      <div className="empty-cinema">
        <CinemaScene quiet />
        <div className="empty-cinema__content">
          <span className="eyebrow">Waiting room</span>
          <h2>The host is choosing the next video</h2>
        </div>
      </div>
    );
  }

  return (
    <div className="empty-cinema">
      <CinemaScene quiet />
      <div className="empty-cinema__content">
        <span className="eyebrow">Your cinema is empty</span>
        <h2>Bring in a direct video link</h2>
        <TextField
          label="Direct video URL"
          value={url}
          onChange={onUrlChange}
          placeholder="https://media.example.org/film.mp4"
          inputMode="url"
          error={status === 'error' ? 'Enter a complete web address for the video.' : undefined}
          trailing={
            <IconButton icon={Clipboard} label="Paste video link" onClick={onPaste} />
          }
        />
        <Button
          icon={<Play size={19} fill="currentColor" aria-hidden="true" />}
          disabled={!url.trim()}
          onClick={onStart}
          fullWidth
        >
          Start stream
        </Button>
      </div>
    </div>
  );
}

function CinemaPlayer({
  status,
  position,
  volume,
  onPositionChange,
  onVolumeChange,
  onToggleMute,
  onTogglePlay,
  onSkipBack,
  onSkipForward,
}: {
  status: StreamStatus;
  position: number;
  volume: number;
  onPositionChange: (value: number) => void;
  onVolumeChange: (value: number) => void;
  onToggleMute: () => void;
  onTogglePlay: () => void;
  onSkipBack: () => void;
  onSkipForward: () => void;
}) {
  const playerRef = useRef<HTMLDivElement>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [isFallbackFullscreen, setIsFallbackFullscreen] = useState(false);
  const paused = status === 'paused';
  const buffering = status === 'buffering';
  const VolumeIcon = volume === 0 ? VolumeX : volume < 55 ? Volume1 : Volume2;

  useEffect(() => {
    const handleFullscreenChange = () => {
      const playerIsFullscreen = document.fullscreenElement === playerRef.current;
      setIsFullscreen(playerIsFullscreen);
      if (playerIsFullscreen) setIsFallbackFullscreen(false);
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
  }, []);

  useEffect(() => {
    if (!isFallbackFullscreen) return;

    const previousOverflow = document.body.style.overflow;
    const exitFallbackFullscreen = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      setIsFullscreen(false);
      setIsFallbackFullscreen(false);
    };

    document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', exitFallbackFullscreen);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', exitFallbackFullscreen);
    };
  }, [isFallbackFullscreen]);

  const toggleFullscreen = async () => {
    if (document.fullscreenElement === playerRef.current) {
      await document.exitFullscreen();
      return;
    }

    if (isFallbackFullscreen) {
      setIsFullscreen(false);
      setIsFallbackFullscreen(false);
      return;
    }

    if (playerRef.current?.requestFullscreen) {
      try {
        await playerRef.current.requestFullscreen();
        return;
      } catch {
        // Embedded webviews may expose but reject the native API.
      }
    }

    setIsFullscreen(true);
    setIsFallbackFullscreen(true);
  };

  return (
    <div
      ref={playerRef}
      className={`cinema-player ${isFallbackFullscreen ? 'is-fullscreen' : ''}`}
      aria-label="Aurora Station video player"
    >
      <div className="cinema-player__visual">
        <CinemaScene />
        {paused ? (
          <button className="player-big-play" onClick={onTogglePlay} aria-label="Play video">
            <Play size={34} fill="currentColor" aria-hidden="true" />
          </button>
        ) : null}
        {buffering ? (
          <div className="player-buffering" role="status">
            <span className="md-loading__shape" aria-hidden="true" />
            <span>Buffering</span>
          </div>
        ) : null}
      </div>

      <div className="player-controls">
        <label className="player-progress">
          <span className="sr-only">Playback position</span>
          <input
            type="range"
            min="0"
            max={TOTAL_SECONDS}
            value={position}
            onChange={(event) => onPositionChange(Number(event.target.value))}
            style={{ '--range-progress': `${(position / TOTAL_SECONDS) * 100}%` } as React.CSSProperties}
          />
        </label>

        <div className="player-controls__row">
          <span className="player-time">
            {formatTime(position)} <span>/ {formatTime(TOTAL_SECONDS)}</span>
          </span>

          <div className="transport" role="group" aria-label="Playback controls">
            <IconButton icon={Rewind} label="Rewind 15 seconds" onClick={onSkipBack} />
            <IconButton
              icon={paused ? Play : Pause}
              label={paused ? 'Play' : 'Pause'}
              variant="filled"
              onClick={onTogglePlay}
            />
            <IconButton
              icon={FastForward}
              label="Fast-forward 15 seconds"
              onClick={onSkipForward}
            />
          </div>

          <div className="volume-control">
            <IconButton icon={VolumeIcon} label={volume === 0 ? 'Unmute video' : 'Mute video'} onClick={onToggleMute} />
            <label>
              <span className="sr-only">Volume</span>
              <input
                type="range"
                min="0"
                max="100"
                value={volume}
                onChange={(event) => onVolumeChange(Number(event.target.value))}
                style={{ '--range-progress': `${volume}%` } as React.CSSProperties}
              />
            </label>
            <IconButton
              icon={isFullscreen ? Minimize2 : Fullscreen}
              label={isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}
              aria-pressed={isFullscreen}
              onClick={() => void toggleFullscreen()}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

function CinemaScene({ quiet = false }: { quiet?: boolean }) {
  return (
    <div className={`cinema-scene ${quiet ? 'cinema-scene--quiet' : ''}`} aria-hidden="true">
      <span className="cinema-scene__planet" />
      <span className="cinema-scene__star cinema-scene__star--one" />
      <span className="cinema-scene__star cinema-scene__star--two" />
      <span className="cinema-scene__star cinema-scene__star--three" />
      <span className="cinema-scene__ridge cinema-scene__ridge--back" />
      <span className="cinema-scene__ridge cinema-scene__ridge--front" />
      <span className="cinema-scene__station">
        <i />
        <i />
        <i />
      </span>
      {!quiet ? <strong>AURORA STATION</strong> : null}
    </div>
  );
}

function ParticipantStrip({ participants }: { participants: Party['participants'] }) {
  return (
    <section className="participant-section" aria-labelledby="participants-title">
      <div className="participant-section__heading">
        <div>
          <span className="eyebrow">Voice party</span>
          <h2 id="participants-title">In the room</h2>
        </div>
        <span className="count-badge">{participants.length}</span>
      </div>
      <div className="participant-list">
        {participants.map((participant) => (
          <ParticipantItem key={participant.id} participant={participant} />
        ))}
      </div>
    </section>
  );
}
