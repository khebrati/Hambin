import type { Identity, Party, Participant } from '../types';

export const sampleVideoUrl =
  'https://media.example.org/films/aurora-station.mp4';

export const avatarOptions = [
  { id: 'comet', label: 'Comet' },
  { id: 'mint', label: 'Mint' },
  { id: 'sunny', label: 'Sunny' },
  { id: 'berry', label: 'Berry' },
  { id: 'cloud', label: 'Cloud' },
  { id: 'ember', label: 'Ember' },
] as const;

const delay = (duration: number) =>
  new Promise<void>((resolve) => window.setTimeout(resolve, duration));

const makeSelf = (identity: Identity, isOwner: boolean): Participant => ({
  id: 'self',
  name: identity.name || 'Nika',
  avatarId: identity.avatarId,
  isOwner,
  isSelf: true,
  isMuted: false,
});

const regulars: Participant[] = [
  {
    id: 'mira',
    name: 'Mira',
    avatarId: 'mint',
    isOwner: true,
    isSpeaking: true,
  },
  { id: 'ellis', name: 'Ellis', avatarId: 'sunny', isMuted: true },
  { id: 'jo', name: 'Jo', avatarId: 'berry' },
];

export const fakePartyRepository = {
  async create(identity: Identity): Promise<Party> {
    await delay(720);
    return {
      id: 'NOVA-27',
      title: 'Friday night screening',
      role: 'owner',
      participants: [
        makeSelf(identity, true),
        { id: 'ellis', name: 'Ellis', avatarId: 'sunny', isSpeaking: true },
        { id: 'jo', name: 'Jo', avatarId: 'berry', isMuted: true },
      ],
      streamUrl: '',
    };
  },

  async join(code: string, identity: Identity): Promise<Party> {
    await delay(820);
    if (code.trim().toUpperCase() !== 'MOON-42') {
      throw new Error('PARTY_NOT_FOUND');
    }

    return {
      id: 'MOON-42',
      title: 'Mira\'s late show',
      role: 'participant',
      participants: [...regulars, makeSelf(identity, false)],
      streamUrl: sampleVideoUrl,
    };
  },

  async startStream(url: string): Promise<string> {
    await delay(880);
    let parsed: URL;
    try {
      parsed = new URL(url);
    } catch {
      throw new Error('INVALID_URL');
    }
    if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') {
      throw new Error('INVALID_URL');
    }
    return parsed.toString();
  },

  async abortStream(): Promise<void> {
    await delay(520);
  },
};

