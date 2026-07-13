import { Mic, MicOff, Star } from 'lucide-react';
import type { AvatarId, Participant } from '../types';

interface AvatarProps {
  avatarId: AvatarId;
  label: string;
  size?: 'small' | 'medium' | 'large';
  selected?: boolean;
  speaking?: boolean;
}

export function Avatar({
  avatarId,
  label,
  size = 'medium',
  selected = false,
  speaking = false,
}: AvatarProps) {
  return (
    <span
      className={`avatar avatar--${avatarId} avatar--${size} ${selected ? 'is-selected' : ''} ${speaking ? 'is-speaking' : ''}`}
      role="img"
      aria-label={`${label}${speaking ? ', speaking' : ''}`}
    >
      <span className="avatar__backdrop" />
      <span className="avatar__ear avatar__ear--left" />
      <span className="avatar__ear avatar__ear--right" />
      <span className="avatar__face">
        <span className="avatar__hair" />
        <span className="avatar__eye avatar__eye--left" />
        <span className="avatar__eye avatar__eye--right" />
        <span className="avatar__nose" />
        <span className="avatar__smile" />
      </span>
      <span className="avatar__accessory" />
    </span>
  );
}

export function ParticipantItem({ participant }: { participant: Participant }) {
  return (
    <div className={`participant ${participant.isSpeaking ? 'is-speaking' : ''}`}>
      <div className="participant__avatar-wrap">
        <Avatar
          avatarId={participant.avatarId}
          label={participant.name}
          speaking={participant.isSpeaking}
        />
        <span className="participant__voice-state" aria-hidden="true">
          {participant.isMuted ? <MicOff size={12} /> : <Mic size={12} />}
        </span>
      </div>
      <div className="participant__copy">
        <strong>{participant.isSelf ? 'You' : participant.name}</strong>
        <span>
          {participant.isOwner ? (
            <>
              <Star size={12} aria-hidden="true" /> Host
            </>
          ) : participant.isSpeaking ? (
            'Speaking'
          ) : participant.isMuted ? (
            'Muted'
          ) : (
            'Listening'
          )}
        </span>
      </div>
    </div>
  );
}

