import {
  ArrowRight,
  CircleStop,
  Clock3,
  DoorOpen,
  LockKeyhole,
  Play,
  Users,
} from 'lucide-react';
import { AppBar } from '../components/AppBar';
import { Button, LoadingIndicator } from '../components/Material';
import type { JoinStatus, PartyPreview } from '../types';

interface RoomPreviewScreenProps {
  preview: PartyPreview;
  joinStatus: JoinStatus;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onBack: () => void;
  onJoin: () => void;
}

export function RoomPreviewScreen({
  preview,
  joinStatus,
  theme,
  onToggleTheme,
  onBack,
  onJoin,
}: RoomPreviewScreenProps) {
  const canJoin = preview.availability === 'available';
  const unavailableCopy =
    preview.availability === 'full'
      ? 'This room has reached its participant limit.'
      : preview.availability === 'ended'
        ? 'This party has ended and is no longer accepting guests.'
        : '';

  return (
    <div className="screen room-preview-screen screen-enter">
      <AppBar
        title="Room preview"
        subtitle="Check the party before joining"
        canGoBack
        onBack={onBack}
        theme={theme}
        onToggleTheme={onToggleTheme}
      />

      <main className="room-preview-main">
        <section className="room-preview-card" aria-labelledby="room-preview-title">
          <div className="room-preview-scene" aria-hidden="true">
            <span className="room-preview-scene__screen">
              <Play size={34} fill="currentColor" />
            </span>
            <span className="room-preview-scene__seat room-preview-scene__seat--one" />
            <span className="room-preview-scene__seat room-preview-scene__seat--two" />
            <span className="room-preview-scene__seat room-preview-scene__seat--three" />
          </div>

          <div className="room-preview-heading">
            <div>
              <span className="eyebrow">Party {preview.id}</span>
              <h2 id="room-preview-title">{preview.title}</h2>
            </div>
            <span className={`room-status-pill room-status-pill--${preview.availability}`}>
              {preview.availability === 'available'
                ? 'Open'
                : preview.availability === 'full'
                  ? 'Full'
                  : 'Ended'}
            </span>
          </div>

          <dl className="room-facts">
            <div>
              <dt><DoorOpen size={19} aria-hidden="true" /> Owner</dt>
              <dd>{preview.ownerPresent ? preview.ownerName : `${preview.ownerName} is away`}</dd>
            </div>
            <div>
              <dt><Users size={19} aria-hidden="true" /> In the room</dt>
              <dd>{preview.participantCount} {preview.participantCount === 1 ? 'person' : 'people'}</dd>
            </div>
            <div>
              <dt><Clock3 size={19} aria-hidden="true" /> Stream</dt>
              <dd>{preview.streamStatus === 'playing' ? 'Playing now' : 'Waiting to start'}</dd>
            </div>
          </dl>

          {!preview.ownerPresent && canJoin ? (
            <div className="room-notice" role="status">
              <LockKeyhole size={22} aria-hidden="true" />
              <div>
                <strong>No owner in the room</strong>
                <span>You can watch and use local controls, but the video stays locked until {preview.ownerName} returns.</span>
              </div>
            </div>
          ) : null}

          {!canJoin ? (
            <div className="room-notice room-notice--unavailable" role="status">
              <CircleStop size={22} aria-hidden="true" />
              <div>
                <strong>Joining is unavailable</strong>
                <span>{unavailableCopy}</span>
              </div>
            </div>
          ) : null}

          <div className="room-preview-actions">
            <Button variant="text" onClick={onBack} disabled={joinStatus === 'loading'}>
              Not now
            </Button>
            {joinStatus === 'loading' ? (
              <LoadingIndicator label={`Joining ${preview.title}`} />
            ) : (
              <Button
                icon={<ArrowRight size={19} aria-hidden="true" />}
                onClick={onJoin}
                disabled={!canJoin}
              >
                Join party
              </Button>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
