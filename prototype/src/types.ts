export type Screen = 'home' | 'settings' | 'party-manager' | 'party';
export type PartyRole = 'owner' | 'participant';
export type JoinStatus = 'idle' | 'loading' | 'error';
export type StreamStatus =
  | 'empty'
  | 'loading'
  | 'playing'
  | 'paused'
  | 'buffering'
  | 'error';

export type AvatarId = 'comet' | 'mint' | 'sunny' | 'berry' | 'cloud' | 'ember';

export interface Identity {
  name: string;
  language: 'English';
  avatarId: AvatarId;
}

export interface Participant {
  id: string;
  name: string;
  avatarId: AvatarId;
  isOwner?: boolean;
  isSelf?: boolean;
  isSpeaking?: boolean;
  isMuted?: boolean;
}

export interface Party {
  id: string;
  title: string;
  role: PartyRole;
  participants: Participant[];
  streamUrl: string;
}

export interface PreviewState {
  screen: Screen;
  role?: PartyRole;
  joinStatus?: JoinStatus;
  streamStatus?: StreamStatus;
  identity?: Partial<Identity>;
}

