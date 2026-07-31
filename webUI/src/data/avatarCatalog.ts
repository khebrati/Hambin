import cometAvatar from '../../cartoon-avatars/139ee106-bfbd-4012-9828-862239ee6996.png';
import mintAvatar from '../../cartoon-avatars/12d58aa0-bb64-43fc-81bb-70276a7ce246.png';
import sunnyAvatar from '../../cartoon-avatars/21114769-fb1a-410e-942f-bfcbd2746436.png';
import berryAvatar from '../../cartoon-avatars/48472730-2ab6-4b95-85e4-94623a9b5852.png';
import cloudAvatar from '../../cartoon-avatars/4ce54c06-fdcc-4c21-939b-9c07cd95c58c.png';
import emberAvatar from '../../cartoon-avatars/a0dd89ec-67b8-4f04-82c7-3fb8ca9a71d9.png';
import novaAvatar from '../../cartoon-avatars/5e7f828c-7955-486a-a920-b2cfe4f18797.png';
import orbitAvatar from '../../cartoon-avatars/884fda0d-6030-423d-87c8-f27cd90c6967.png';
import prismAvatar from '../../cartoon-avatars/c0c6eed0-d923-4156-ae44-faa1554083f0.png';
import echoAvatar from '../../cartoon-avatars/c337b58d-481a-4688-971d-f7e4f644a198.png';
import sparkAvatar from '../../cartoon-avatars/de8a2069-a178-43cb-8ed7-535139eed13d.png';
import bloomAvatar from '../../cartoon-avatars/f3f027df-e786-4f32-ad33-30ccd51fe7ba.png';
import type { AvatarId } from '../types';

export const avatarOptions = [
  { id: 'comet', label: 'Comet', src: cometAvatar },
  { id: 'mint', label: 'Mint', src: mintAvatar },
  { id: 'sunny', label: 'Sunny', src: sunnyAvatar },
  { id: 'berry', label: 'Berry', src: berryAvatar },
  { id: 'cloud', label: 'Cloud', src: cloudAvatar },
  { id: 'ember', label: 'Ember', src: emberAvatar },
  { id: 'nova', label: 'Nova', src: novaAvatar },
  { id: 'orbit', label: 'Orbit', src: orbitAvatar },
  { id: 'prism', label: 'Prism', src: prismAvatar },
  { id: 'echo', label: 'Echo', src: echoAvatar },
  { id: 'spark', label: 'Spark', src: sparkAvatar },
  { id: 'bloom', label: 'Bloom', src: bloomAvatar },
] as const satisfies readonly { id: AvatarId; label: string; src: string }[];

export const avatarImageById = Object.fromEntries(
  avatarOptions.map(({ id, src }) => [id, src]),
) as Record<AvatarId, string>;
