import {
  Clipboard,
  ClipboardCheck,
  FastForward,
  Fullscreen,
  Link2,
  LockKeyhole,
  LogOut,
  Mic,
  MicOff,
  Minimize2,
  Pause,
  Play,
  Rewind,
  Square,
  UserPlus,
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
import {
  SyncExperience,
  SyncPreviewControl,
  SyncSettingRow,
} from '../components/SyncExperience';
import { sampleVideoUrl } from '../data/fakeRepository';
import { formatTime, TOTAL_SECONDS } from '../lib/time';
import type {
  Party,
  StreamStatus,
  SyncRequest,
  SyncSurface,
} from '../types';

async function writeClipboardText(value: string) {
  if (!navigator.clipboard?.writeText) throw new Error('CLIPBOARD_UNAVAILABLE');
  await Promise.race([
    navigator.clipboard.writeText(value),
    new Promise<never>((_, reject) =>
      window.setTimeout(() => reject(new Error('CLIPBOARD_TIMEOUT')), 600),
    ),
  ]);
}

function buildIncomingRequest(
  party: Party,
  position: number,
): SyncRequest | null {
  const others = party.participants.filter((participant) => !participant.isSelf);
  if (others.length === 0) return null;
  const requester = others.reduce((best, participant) =>
    (participant.positionSeconds ?? 0) > (best.positionSeconds ?? 0)
      ? participant
      : best,
  );
  return {
    id: `request-${requester.id}`,
    requesterName: requester.name,
    requesterAvatarId: requester.avatarId,
    targetSeconds: requester.positionSeconds ?? position,
    direction: 'bring-to-me',
  };
}

interface PartyScreenProps {
  party: Party;
  streamStatus: StreamStatus;
  theme: 'light' | 'dark';
  aborting: boolean;
  initialSyncSurface?: SyncSurface;
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
  initialSyncSurface,
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
  const [inviteOpen, setInviteOpen] = useState(false);
  const [requestsMuted, setRequestsMuted] = useState(false);
  const [incomingRequest, setIncomingRequest] = useState<SyncRequest | null>(() =>
    initialSyncSurface === 'incoming'
      ? buildIncomingRequest(party, 1122)
      : null,
  );
  const [inviteCopyState, setInviteCopyState] = useState<
    'idle' | 'code' | 'link' | 'error'
  >('idle');

  const activeUrl = party.streamUrl || sampleVideoUrl;
  const hasPlayer = ['playing', 'paused', 'buffering'].includes(streamStatus);
  const isOwner = party.role === 'owner' && party.ownerPresent;
  const inviteLink = useMemo(() => {
    const url = new URL(window.location.href);
    url.search = '';
    url.searchParams.set('invite', party.id);
    return url.toString();
  }, [party.id]);

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
      await writeClipboardText(activeUrl);
    } catch {
      onNotify('Copy is unavailable. Select the link manually.');
      return;
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

  const copyInvite = async (value: string, target: 'code' | 'link') => {
    try {
      await writeClipboardText(value);
      setInviteCopyState(target);
      onNotify(target === 'code' ? 'Party code copied' : 'Invite link copied');
    } catch {
      setInviteCopyState('error');
      onNotify('Copy is unavailable. You can select the text manually.');
    }
  };

  const toggleMute = () => {
    if (volume === 0) {
      setVolume(previousVolume || 72);
    } else {
      setPreviousVolume(volume);
      setVolume(0);
    }
  };

  const jumpTo = (seconds: number) => {
    setPosition(Math.max(0, Math.min(seconds, TOTAL_SECONDS)));
  };

  const previewIncomingRequest = () => {
    if (requestsMuted) {
      onNotify('Sync requests are turned off');
      return;
    }
    setIncomingRequest(buildIncomingRequest(party, position));
  };

  return (
    <div className="screen party-screen screen-enter">
      <AppBar
        title={party.title}
        subtitle={`${party.id} · ${isOwner ? 'Owner' : party.ownerPresent ? 'Guest' : 'Owner away'}`}
        theme={theme}
        onToggleTheme={onToggleTheme}
        actions={
          <>
            <IconButton
              icon={UserPlus}
              label="Invite friends"
              variant="tonal"
              onClick={() => {
                setInviteCopyState('idle');
                setInviteOpen(true);
              }}
            />
            {hasPlayer ? (
              <IconButton icon={Link2} label="View video link" onClick={() => setUrlOpen(true)} />
            ) : null}
            <IconButton icon={LogOut} label="Leave party" onClick={() => setLeaveOpen(true)} />
          </>
        }
      />

      <main className="party-layout">
        <section className="cinema-column" aria-label="Cinema">
          {!party.ownerPresent ? (
            <div className="owner-away-banner" role="status">
              <LockKeyhole size={24} aria-hidden="true" />
              <div>
                <span className="eyebrow">Owner away</span>
                <strong>{party.ownerName} left the room</strong>
                <p>
                  The current video stays available, but nobody can replace or end it until{' '}
                  {party.ownerName} returns.
                </p>
              </div>
            </div>
          ) : null}
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
              ownerPresent={party.ownerPresent}
              ownerName={party.ownerName}
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
            <SyncExperience
              party={party}
              selfPosition={position}
              requestsMuted={requestsMuted}
              onRequestsMutedChange={setRequestsMuted}
              onJumpTo={(seconds) => jumpTo(seconds)}
              onNotify={onNotify}
              initialSurface={initialSyncSurface}
              incomingRequest={incomingRequest}
              onDismissIncoming={() => setIncomingRequest(null)}
              onPreviewIncoming={previewIncomingRequest}
            />
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
            <SyncSettingRow muted={requestsMuted} onChange={setRequestsMuted} />
          ) : null}

          <Button
            variant="outlined"
            icon={<UserPlus size={18} aria-hidden="true" />}
            onClick={() => {
              setInviteCopyState('idle');
              setInviteOpen(true);
            }}
            fullWidth
          >
            Invite friends
          </Button>

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

          {hasPlayer ? (
            <SyncPreviewControl onPreviewIncoming={previewIncomingRequest} />
          ) : null}
        </aside>
      </main>

      <Dialog
        open={inviteOpen}
        title="Invite friends"
        onClose={() => setInviteOpen(false)}
        actions={
          <Button variant="text" onClick={() => setInviteOpen(false)}>
            Done
          </Button>
        }
      >
        <p>Share either option. Friends will preview the room before they join.</p>
        <section className="share-option" aria-labelledby="party-code-label">
          <label className="read-only-field">
            <span id="party-code-label">Party code</span>
            <input value={party.id} readOnly />
          </label>
          <Button
            variant="tonal"
            icon={
              inviteCopyState === 'code' ? (
                <ClipboardCheck size={18} aria-hidden="true" />
              ) : (
                <Clipboard size={18} aria-hidden="true" />
              )
            }
            onClick={() => void copyInvite(party.id, 'code')}
          >
            {inviteCopyState === 'code' ? 'Code copied' : 'Copy code'}
          </Button>
        </section>
        <section className="share-option" aria-labelledby="invite-link-label">
          <label className="read-only-field">
            <span id="invite-link-label">Prototype invite link</span>
            <input value={inviteLink} readOnly />
          </label>
          <Button
            variant="outlined"
            icon={
              inviteCopyState === 'link' ? (
                <ClipboardCheck size={18} aria-hidden="true" />
              ) : (
                <Link2 size={18} aria-hidden="true" />
              )
            }
            onClick={() => void copyInvite(inviteLink, 'link')}
          >
            {inviteCopyState === 'link' ? 'Link copied' : 'Copy link'}
          </Button>
        </section>
        {inviteCopyState === 'error' ? (
          <p className="copy-error" role="status">
            Copy is unavailable here. Select the code or link above to share it manually.
          </p>
        ) : null}
      </Dialog>

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
        {!isOwner ? (
          <p>
            {party.ownerPresent
              ? `Only ${party.ownerName}, the room owner, can replace this link.`
              : `This link stays locked until ${party.ownerName} returns.`}
          </p>
        ) : null}
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
        <p>
          {isOwner
            ? 'Your playback will stop. The room stays open without an owner until you rejoin.'
            : 'Your playback will stop and you will return home.'}
        </p>
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
  ownerPresent,
  ownerName,
}: {
  status: StreamStatus;
  url: string;
  onUrlChange: (value: string) => void;
  onPaste: () => void;
  onStart: () => void;
  isOwner: boolean;
  ownerPresent: boolean;
  ownerName: string;
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
          <h2>
            {ownerPresent
              ? `${ownerName} is choosing the next video`
              : `Waiting for ${ownerName} to return`}
          </h2>
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
