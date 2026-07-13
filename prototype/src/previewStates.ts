import { sampleVideoUrl } from './data/fakeRepository';
import type { Identity, Party, PreviewState } from './types';

export const defaultIdentity: Identity = {
  name: 'Nika',
  language: 'English',
  avatarId: 'comet',
};

export const previewStates: Record<string, PreviewState> = {
  home: { screen: 'home' },
  'settings-empty': { screen: 'settings', identity: { name: '' } },
  'settings-populated': { screen: 'settings' },
  'party-manager': { screen: 'party-manager' },
  'party-manager-loading': {
    screen: 'party-manager',
    joinStatus: 'loading',
  },
  'party-manager-error': { screen: 'party-manager', joinStatus: 'error' },
  'owner-empty': { screen: 'party', role: 'owner', streamStatus: 'empty' },
  'player-loading': { screen: 'party', role: 'owner', streamStatus: 'loading' },
  'player-playing': { screen: 'party', role: 'owner', streamStatus: 'playing' },
  'player-paused': { screen: 'party', role: 'owner', streamStatus: 'paused' },
  'player-buffering': {
    screen: 'party',
    role: 'owner',
    streamStatus: 'buffering',
  },
  'player-error': { screen: 'party', role: 'owner', streamStatus: 'error' },
  'participant-playing': {
    screen: 'party',
    role: 'participant',
    streamStatus: 'playing',
  },
};

export function makePreviewParty(
  role: 'owner' | 'participant',
  identity: Identity,
): Party {
  const self = {
    id: 'self',
    name: identity.name || 'Nika',
    avatarId: identity.avatarId,
    isSelf: true,
    isOwner: role === 'owner',
  } as const;

  if (role === 'participant') {
    return {
      id: 'MOON-42',
      title: "Mira's late show",
      role,
      streamUrl: sampleVideoUrl,
      participants: [
        { id: 'mira', name: 'Mira', avatarId: 'mint', isOwner: true, isSpeaking: true },
        { id: 'ellis', name: 'Ellis', avatarId: 'sunny', isMuted: true },
        { id: 'jo', name: 'Jo', avatarId: 'berry' },
        self,
      ],
    };
  }

  return {
    id: 'NOVA-27',
    title: 'Friday night screening',
    role,
    streamUrl: sampleVideoUrl,
    participants: [
      self,
      { id: 'ellis', name: 'Ellis', avatarId: 'sunny', isSpeaking: true },
      { id: 'jo', name: 'Jo', avatarId: 'berry', isMuted: true },
    ],
  };
}

