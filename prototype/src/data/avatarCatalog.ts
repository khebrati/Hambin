import cometAvatar from '../../cartoon-avatars/139ee106-bfbd-4012-9828-862239ee6996.png';
import mintAvatar from '../../cartoon-avatars/58d4c7df-9858-4d05-af23-a01f00264326.png';
import sunnyAvatar from '../../cartoon-avatars/785b4098-2f73-4b2a-9055-2861de265a51.png';
import berryAvatar from '../../cartoon-avatars/7acbc811-7de5-4f1b-a347-ea260935eb7d.png';
import cloudAvatar from '../../cartoon-avatars/8a28be1c-af39-47d1-b350-1d2e71850bcc.png';
import emberAvatar from '../../cartoon-avatars/a0dd89ec-67b8-4f04-82c7-3fb8ca9a71d9.png';
import novaAvatar from '../../cartoon-avatars/c9f90cd6-7ab1-40fb-9f12-ac34f0e7eddb.png';
import orbitAvatar from '../../cartoon-avatars/cf3756b4-bac7-4b77-a22b-458924168c80.png';
import prismAvatar from '../../cartoon-avatars/d7028d07-2c1e-4ce4-b4f4-dd16d5dd6811.png';
import echoAvatar from '../../cartoon-avatars/dcc9f045-1430-4339-ae68-a8a14755dce3.png';
import sparkAvatar from '../../cartoon-avatars/de8a2069-a178-43cb-8ed7-535139eed13d.png';
import bloomAvatar from '../../cartoon-avatars/e5aaf4d2-6ae4-40d4-9e0e-64cf2a2686ac.png';
import pixelAvatar from '../../cartoon-avatars/ea7eeedb-153a-4549-9c7c-f5819423429e.png';
import lunaAvatar from '../../cartoon-avatars/ee31c5cf-cf4a-4d96-a536-06b0166f8820.png';
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
  { id: 'pixel', label: 'Pixel', src: pixelAvatar },
  { id: 'luna', label: 'Luna', src: lunaAvatar },
] as const satisfies readonly { id: AvatarId; label: string; src: string }[];

export const avatarImageById = Object.fromEntries(
  avatarOptions.map(({ id, src }) => [id, src]),
) as Record<AvatarId, string>;
