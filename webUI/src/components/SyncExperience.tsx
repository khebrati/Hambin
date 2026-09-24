import {
  ArrowRightLeft,
  Gauge,
  MoveRight,
  Send,
  Users,
} from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { formatDelta, formatTime, TOTAL_SECONDS } from '../lib/time';
import type {
  AvatarId,
  Participant,
  Party,
  SyncRequest,
  SyncSurface,
} from '../types';
import { Avatar } from './Avatar';
import {
  Button,
  Checkbox,
  Dialog,
  LoadingIndicator,
  Sheet,
  Switch,
} from './Material';

interface SyncPerson {
  id: string;
  name: string;
  avatarId: AvatarId;
  isSelf: boolean;
  isOwner: boolean;
  isSpeaking: boolean;
  position: number;
}

interface SyncExperienceProps {
  party: Party;
  selfPosition: number;
  requestsMuted: boolean;
  onRequestsMutedChange: (muted: boolean) => void;
  onJumpTo: (seconds: number, targetName: string) => void;
  onNotify: (message: string) => void;
  initialSurface?: SyncSurface;
  incomingRequest: SyncRequest | null;
  onDismissIncoming: () => void;
  onPreviewIncoming: () => void;
}

function toPeople(party: Party, selfPosition: number): SyncPerson[] {
  return party.participants.map((participant: Participant) => ({
    id: participant.id,
    name: participant.name,
    avatarId: participant.avatarId,
    isSelf: Boolean(participant.isSelf),
    isOwner: Boolean(participant.isOwner),
    isSpeaking: Boolean(participant.isSpeaking),
    position: participant.isSelf
      ? selfPosition
      : participant.positionSeconds ?? 0,
  }));
}

function label(person: SyncPerson) {
  return person.isSelf ? 'You' : person.name;
}

export function SyncExperience({
  party,
  selfPosition,
  requestsMuted,
  onRequestsMutedChange,
  onJumpTo,
  onNotify,
  initialSurface,
  incomingRequest,
  onDismissIncoming,
  onPreviewIncoming,
}: SyncExperienceProps) {
  const [surface, setSurface] = useState<SyncSurface>(
    initialSurface && initialSurface !== 'incoming' ? initialSurface : 'idle',
  );
  const [pending, setPending] = useState(false);
  const [pendingTarget, setPendingTarget] = useState(selfPosition);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [draftMute, setDraftMute] = useState(requestsMuted);
  const pendingTimer = useRef<number | null>(null);

  useEffect(() => setDraftMute(requestsMuted), [requestsMuted]);

  useEffect(
    () => () => {
      if (pendingTimer.current) window.clearTimeout(pendingTimer.current);
    },
    [],
  );

  const people = useMemo(() => toPeople(party, selfPosition), [party, selfPosition]);
  const others = useMemo(
    () => people.filter((person) => !person.isSelf),
    [people],
  );
  const leader = useMemo(
    () => people.reduce((best, person) => (person.position > best.position ? person : best)),
    [people],
  );
  const selected = others.find((person) => person.id === selectedId) ?? null;
  const selfIsLeader = leader.isSelf;
  const takeMode = surface === 'take-me-to-others';

  const openSurface = (next: SyncSurface) => {
    if (next === 'take-me-to-others') setSelectedId(null);
    setSurface(next);
  };

  const closeSheet = () => {
    if (pendingTimer.current) window.clearTimeout(pendingTimer.current);
    setPending(false);
    setSurface('idle');
  };

  const sendBringRequest = () => {
    const target = selfPosition;
    setPendingTarget(target);
    setPending(true);
    pendingTimer.current = window.setTimeout(() => {
      setPending(false);
      setSurface('idle');
      onNotify(
        others.length > 0
          ? `Asked ${others.length} friends to join you at ${formatTime(target)}`
          : 'No one else is in the room to ask',
      );
    }, 1500);
  };

  const confirmTake = (person: SyncPerson) => {
    onJumpTo(person.position, person.name);
    closeSheet();
    onNotify(`Moved to ${person.name} at ${formatTime(person.position)}`);
  };

  const agreeIncoming = () => {
    if (!incomingRequest) return;
    onJumpTo(incomingRequest.targetSeconds, incomingRequest.requesterName);
    onDismissIncoming();
    onNotify(
      `Moved to ${incomingRequest.requesterName} at ${formatTime(incomingRequest.targetSeconds)}`,
    );
  };

  const declineIncoming = () => {
    onDismissIncoming();
    onNotify('Sync request declined');
  };

  const statusLine = selfIsLeader
    ? 'You are ahead of the room'
    : `You are behind ${label(leader)} by ${formatTime(leader.position - selfPosition)}`;

  const timeline = (
    <SyncTimeline
      people={people}
      leaderId={leader.id}
      selectedId={selectedId}
      onSelect={takeMode ? (id) => setSelectedId(id) : undefined}
      targetSeconds={takeMode ? undefined : selfPosition}
    />
  );

  const bringPanel = (
    <div className="sync-compose">
      <div className="sync-time-display">
        <span className="eyebrow">Everyone moves to</span>
        <strong>{formatTime(pending ? pendingTarget : selfPosition)}</strong>
        <span>your current position</span>
      </div>
      <div className="sync-recipients">
        <span className="sync-recipient-avatars" aria-hidden="true">
          {others.slice(0, 5).map((person) => (
            <Avatar
              key={person.id}
              avatarId={person.avatarId}
              label={person.name}
              size="small"
            />
          ))}
        </span>
        <span>
          {others.length > 0
            ? `${others.length} ${others.length === 1 ? 'friend' : 'friends'} will be asked`
            : 'You are the only one here'}
        </span>
      </div>
      <Checkbox
        checked={draftMute}
        onChange={(checked) => {
          setDraftMute(checked);
          onRequestsMutedChange(checked);
        }}
        label="Turn off sync requests from others"
        supportingText="You can change this later in the room."
      />
      {pending ? (
        <div className="sync-pending" role="status" aria-live="polite">
          <LoadingIndicator label={`Waiting for ${others.length} friends`} />
          <p>They will move to {formatTime(pendingTarget)} when they agree.</p>
        </div>
      ) : null}
    </div>
  );

  const followPanel = selected ? (
    <div className="sync-follow">
      <div className="sync-follow__person">
        <Avatar avatarId={selected.avatarId} label={selected.name} size="small" />
        <div>
          <strong>{selected.name}</strong>
          <span>
            {formatTime(selected.position)} ·{' '}
            {formatDelta(selected.position - selfPosition)}
          </span>
        </div>
      </div>
      <Button
        fullWidth
        icon={<MoveRight size={18} aria-hidden="true" />}
        onClick={() => confirmTake(selected)}
      >
        Move me to {selected.name}
      </Button>
    </div>
  ) : (
    <p className="sync-hint">Pick a person on the timeline to follow their position.</p>
  );

  const sheetActions = (
    <>
      <Button variant="text" onClick={closeSheet}>
        Close
      </Button>
      {takeMode && selected ? (
        <Button
          icon={<MoveRight size={18} aria-hidden="true" />}
          onClick={() => confirmTake(selected)}
        >
          Move to {selected.name}
        </Button>
      ) : null}
      {!takeMode && !pending ? (
        <Button
          icon={<Send size={18} aria-hidden="true" />}
          onClick={sendBringRequest}
        >
          Send request
        </Button>
      ) : null}
    </>
  );

  return (
    <div className="sync-experience">
      {incomingRequest ? (
        <IncomingDialog
          request={incomingRequest}
          people={people}
          leaderId={leader.id}
          selfPosition={selfPosition}
          muted={draftMute}
          onMutedChange={(checked) => {
            setDraftMute(checked);
            onRequestsMutedChange(checked);
          }}
          onAgree={agreeIncoming}
          onDecline={declineIncoming}
        />
      ) : null}

      <div className="sync-band">
        <div className="sync-band__lead">
          <span className="eyebrow">Playback sync</span>
          <strong>{statusLine}</strong>
        </div>
        <Button
          variant="tonal"
          icon={<ArrowRightLeft size={19} aria-hidden="true" />}
          onClick={() => openSurface('bring-to-me')}
        >
          Open sync center
        </Button>
      </div>

      <Sheet
        open={surface !== 'idle'}
        title="Sync center"
        actions={sheetActions}
        onClose={closeSheet}
      >
        <div className="sync-center">
          <div
            className="segmented-control sync-center__modes"
            role="tablist"
            aria-label="Sync mode"
          >
            <button
              role="tab"
              aria-selected={!takeMode}
              className={!takeMode ? 'is-selected' : ''}
              onClick={() => openSurface('bring-to-me')}
            >
              <Users size={18} aria-hidden="true" />
              Bring everyone
            </button>
            <button
              role="tab"
              aria-selected={takeMode}
              className={takeMode ? 'is-selected' : ''}
              onClick={() => openSurface('take-me-to-others')}
            >
              <MoveRight size={18} aria-hidden="true" />
              Follow someone
            </button>
          </div>
          {timeline}
          {takeMode ? followPanel : bringPanel}
        </div>
      </Sheet>
    </div>
  );
}

function SyncTimeline({
  people,
  leaderId,
  selectedId,
  onSelect,
  targetSeconds,
}: {
  people: SyncPerson[];
  leaderId: string;
  selectedId: string | null;
  onSelect?: (id: string) => void;
  targetSeconds?: number;
}) {
  const positions = people.map((person) => person.position);
  const rawMin = Math.min(...positions, targetSeconds ?? Number.POSITIVE_INFINITY);
  const rawMax = Math.max(...positions, targetSeconds ?? Number.NEGATIVE_INFINITY);
  const min = Math.max(0, rawMin - 90);
  const max = Math.min(TOTAL_SECONDS, rawMax + 90);
  const percent = (position: number) =>
    max <= min ? 50 : ((position - min) / (max - min)) * 100;

  return (
    <div className="sync-timeline">
      <div className="sync-timeline__track" aria-hidden="true" />
      {targetSeconds != null ? (
        <span
          className="sync-timeline__target"
          style={{ left: `${percent(targetSeconds)}%` }}
          aria-hidden="true"
        >
          <Gauge size={14} />
        </span>
      ) : null}
      {people.map((person) => {
        const markerClass = `sync-timeline__marker ${
          person.isSelf ? 'is-self' : ''
        } ${person.id === selectedId ? 'is-selected' : ''} ${
          person.id === leaderId ? 'is-leader' : ''
        }`;
        const markerStyle = { left: `${percent(person.position)}%` };
        const markerLabel = `${label(person)} at ${formatTime(person.position)}`;
        const markerContent = (
          <>
            <Avatar avatarId={person.avatarId} label={person.name} size="small" />
            <span className="sync-timeline__name">{label(person)}</span>
            <span className="sync-timeline__time">
              {formatTime(person.position)}
            </span>
          </>
        );
        return onSelect ? (
          <button
            key={person.id}
            type="button"
            className={markerClass}
            style={markerStyle}
            onClick={() => onSelect(person.id)}
            aria-label={markerLabel}
          >
            {markerContent}
          </button>
        ) : (
          <div key={person.id} className={markerClass} style={markerStyle} aria-label={markerLabel}>
            {markerContent}
          </div>
        );
      })}
    </div>
  );
}

function IncomingDialog({
  request,
  people,
  leaderId,
  selfPosition,
  muted,
  onMutedChange,
  onAgree,
  onDecline,
}: {
  request: SyncRequest;
  people: SyncPerson[];
  leaderId: string;
  selfPosition: number;
  muted: boolean;
  onMutedChange: (checked: boolean) => void;
  onAgree: () => void;
  onDecline: () => void;
}) {
  return (
    <Dialog
      open
      title="Sync request"
      onClose={onDecline}
      actions={
        <>
          <Button variant="text" onClick={onDecline}>
            Not now
          </Button>
          <Button onClick={onAgree}>Agree and move</Button>
        </>
      }
    >
      <SyncTimeline
        people={people}
        leaderId={leaderId}
        selectedId={null}
        targetSeconds={request.targetSeconds}
      />
      <div className="sync-incoming__head">
        <Avatar
          avatarId={request.requesterAvatarId}
          label={request.requesterName}
          size="small"
        />
        <div className="sync-incoming__copy">
          <strong>{request.requesterName} wants to move everyone</strong>
          <span>
            You are at {formatTime(selfPosition)}. Agree to jump to{' '}
            {formatTime(request.targetSeconds)}.
          </span>
        </div>
      </div>
      <Checkbox
        checked={muted}
        onChange={onMutedChange}
        label="Turn off sync requests from others"
        supportingText="You can also change this in the room settings."
      />
    </Dialog>
  );
}

export function SyncPreviewControl({
  onPreviewIncoming,
}: {
  onPreviewIncoming: () => void;
}) {
  return (
    <section className="sync-prototype" aria-label="Sync preview">
      <span className="eyebrow">Prototype</span>
      <Button variant="text" icon={<ArrowRightLeft size={16} aria-hidden="true" />} onClick={onPreviewIncoming}>
        Preview incoming request
      </Button>
    </section>
  );
}

export function SyncSettingRow({
  muted,
  onChange,
}: {
  muted: boolean;
  onChange: (muted: boolean) => void;
}) {
  return (
    <section className="sync-setting" aria-label="Sync requests setting">
      <Switch
        checked={!muted}
        onChange={(checked) => onChange(!checked)}
        label="Allow sync requests"
        supportingText={
          muted
            ? 'Friends cannot ask you to move right now.'
            : 'Friends can ask you to move to their position.'
        }
      />
    </section>
  );
}
