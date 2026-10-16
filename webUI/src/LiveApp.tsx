import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, LoadingIndicator, Snackbar } from './components/Material';
import { errorMessage, request, ApiError, MutationRequests } from './data/api';
import { token, watchToken } from './data/auth';
import { identityModel, partyModel, previewModel, profileBody, type Admission, type IdentityDto, type PreviewDto, type Snapshot, type StreamDto } from './data/contracts';
import { RoomConnection } from './data/realtime';
import { MeshVoice, type VoiceState } from './data/voice';
import { defaultIdentity } from './previewStates';
import { HomeScreen } from './screens/HomeScreen';
import { PartyManagerScreen, type ManagerMode } from './screens/PartyManagerScreen';
import { PartyScreen } from './screens/PartyScreen';
import { RoomPreviewScreen } from './screens/RoomPreviewScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import type { Identity, Party, PartyPreview, Screen, StreamStatus } from './types';
const voiceOff: VoiceState = { status: 'off', muted: true, members: {} };
const savedCodeKey = 'roomio-last-owned-code';
const activeCodeKey = 'roomio-active-code';

export default function LiveApp() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const q = new URLSearchParams(location.search).get('theme');
    return q === 'dark' || q === 'light' ? q : localStorage.getItem('roomio-theme') === 'dark' || (!localStorage.getItem('roomio-theme') && matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
  });
  const [identity, setIdentity] = useState<Identity>(defaultIdentity);
  const [identityId, setIdentityId] = useState('');
  const [ready, setReady] = useState(false);
  const [initialError, setInitialError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const [screen, setScreen] = useState<Screen>('home');
  const [managerMode, setManagerMode] = useState<ManagerMode>('create');
  const [joinStatus, setJoinStatus] = useState<'idle' | 'loading' | 'error'>('idle');
  const [joinError, setJoinError] = useState('');
  const [codeDraft, setCodeDraft] = useState('');
  const [roomPreview, setRoomPreview] = useState<PartyPreview | null>(null);
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [roomId, setRoomId] = useState('');
  const [streamStatus, setStreamStatus] = useState<StreamStatus>('empty');
  const [connected, setConnected] = useState(false);
  const [aborting, setAborting] = useState(false);
  const [leaving, setLeaving] = useState(false);
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState('');
  const [returnableParty, setReturnableParty] = useState<Party | null>(null);
  const [voiceState, setVoiceState] = useState<VoiceState>(voiceOff);
  const [syncTarget, setSyncTarget] = useState<{requestId: string;positionMs: number}>();
  const [syncing, setSyncing] = useState(false);
  const mutations = useRef(new MutationRequests());
  const entering = useRef(false);
  const realtime = useRef<RoomConnection | null>(null);
  const voice = useRef<MeshVoice | null>(null);
  const playback = useRef({ positionMs: 0, playing: false });
  const streamSession = useRef('');
  const notify = useCallback((message: string) => setSnackbar(message), []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.dataset.textScale = new URLSearchParams(location.search).get('largeText') === '1' ? 'large' : 'standard';
    document.documentElement.dir = new URLSearchParams(location.search).get('dir') === 'rtl' ? 'rtl' : 'ltr';
    localStorage.setItem('roomio-theme', theme);
  }, [theme]);
  useEffect(() => { if (!snackbar) return; const t = setTimeout(() => setSnackbar(''), 5000); return () => clearTimeout(t); }, [snackbar]);

  const loadPreview = async (code: string) => {
    setCodeDraft(code); setJoinStatus('loading'); setJoinError('');
    try {
      const value = await request<PreviewDto>(`/v1/rooms/${encodeURIComponent(code.trim())}/preview`);
      setRoomPreview(previewModel(value)); setScreen('room-preview'); setJoinStatus('idle');
    } catch (error) { setJoinStatus('error'); setJoinError(errorMessage(error)); notify(errorMessage(error)); }
  };
  useEffect(() => {
    let active = true;
    setInitialError('');
    void request<{identity: IdentityDto}>('/v1/identities/bootstrap', profileBody(defaultIdentity)).then(async value => {
      if (!active) return;
      setIdentity(identityModel(value.identity)); setIdentityId(value.identity.id); setReady(true);
      const savedCode = localStorage.getItem(savedCodeKey);
      if (savedCode) {
        try {
          const preview = await request<PreviewDto>(`/v1/rooms/${encodeURIComponent(savedCode)}/preview`);
          if (active && preview.availability !== 'ENDED') setReturnableParty({ id: '', code: savedCode, title: preview.title, role: 'owner', ownerName: preview.ownerName, ownerPresent: preview.ownerPresent, participants: [], streamUrl: '' });
          else if (preview.availability === 'ENDED') localStorage.removeItem(savedCodeKey);
        } catch { /* A temporary outage must not erase the owner's saved code. */ }
      }
      const invite = new URLSearchParams(location.search).get('invite');
      if (active && !invite && sessionStorage.getItem(activeCodeKey)) { await enterRoom(`/v1/rooms/${encodeURIComponent(sessionStorage.getItem(activeCodeKey)!)}/join`); }
      if (active && invite) { setManagerMode('join'); setScreen('party-manager'); await loadPreview(invite); }
    }).catch(error => { if (active) setInitialError(errorMessage(error)); });
    return () => { active = false; };
  }, [attempt]);

  useEffect(() => {
    if (!roomId) return;
    let active = true;
    const onSnapshot = (value: Snapshot) => {
      if (!active) return;
      value.ownerName ||= snapshot?.ownerName;
      if (value.stream.sessionId !== streamSession.current) {
        streamSession.current = value.stream.sessionId;
        playback.current = { positionMs: 0, playing: false }; setSyncTarget(undefined);
        setStreamStatus(value.stream.state === 'ACTIVE' ? 'loading' : 'empty');
      } else if (value.stream.state !== 'ACTIVE') setStreamStatus('empty');
      setSnapshot(value);
    };
    const session = new RoomConnection({ roomId, onSnapshot, onConnection: setConnected, onError: e => notify(errorMessage(e)), onClosed: () => {
      voice.current?.disconnect(); voice.current = null; setVoiceState(voiceOff); sessionStorage.removeItem(activeCodeKey); setRoomId(''); setSnapshot(null); setScreen('home'); setReturnableParty(null); localStorage.removeItem(savedCodeKey); notify('This room session has ended.');
    } });
    realtime.current = session;
    const unsubscribe = watchToken(idToken => {
      void session.refreshAuth(idToken).catch(e => notify(errorMessage(e)));
      void voice.current?.refreshAuth(idToken).catch(e => notify(errorMessage(e)));
    });
    session.start();
    return () => { active = false; unsubscribe(); session.close(); voice.current?.disconnect(); voice.current = null; realtime.current = null; };
  }, [roomId, notify]);

  const enterRoom = async (path: string) => {
    if (entering.current) return;
    entering.current = true;
    setJoinStatus('loading'); setJoinError('');
    try {
      const admission = await mutations.current.run<Admission>(path, {});
      const value = await request<Snapshot>(`/v1/rooms/${admission.room.id}/snapshot`);
      value.ownerName = value.members.find(m => m.isOwner)?.displayName || roomPreview?.ownerName || returnableParty?.ownerName;
      sessionStorage.setItem(activeCodeKey, admission.room.code);
      setSnapshot(value); setRoomId(admission.room.id); setScreen('party'); setJoinStatus('idle'); setVoiceState(voiceOff);
      setStreamStatus(value.stream.state === 'ACTIVE' ? 'loading' : 'empty');
      if (admission.membership.isOwner) localStorage.setItem(savedCodeKey, admission.room.code);
      setReturnableParty(null);
    } catch (error) { setJoinStatus('error'); setJoinError(errorMessage(error)); notify(errorMessage(error)); if(error instanceof ApiError && ['ROOM_ENDED','ROOM_NOT_FOUND'].includes(error.code)) sessionStorage.removeItem(activeCodeKey); }
    finally { entering.current = false; }
  };
  const saveProfile = async (value: Identity) => {
    setSaving(true);
    try { const result = await request<{identity: IdentityDto}>('/v1/guest-sessions/profile', profileBody(value), 'PATCH'); setIdentity(identityModel(result.identity)); setScreen('home'); notify('Profile updated'); }
    catch (error) { notify(errorMessage(error)); }
    finally { setSaving(false); }
  };
  const startStream = async (url: string) => {
    setStreamStatus('loading');
    try {
      await mutations.current.run<{stream: StreamDto}>(`/v1/rooms/${roomId}/stream/start`, { url });
      await realtime.current?.recover();
    } catch (error) { setStreamStatus('error'); notify(errorMessage(error)); }
  };
  const abortStream = async () => {
    setAborting(true);
    try { await mutations.current.run(`/v1/rooms/${roomId}/stream/abort`, { revision: snapshot?.stream.revision }); await realtime.current?.recover(); notify('Stream ended for everyone'); }
    catch (error) { notify(errorMessage(error)); }
    finally { setAborting(false); }
  };
  const leave = async () => {
    if (leaving) return;
    setLeaving(true);
    // Privacy action is immediate even if HTTP leave needs retry.
    voice.current?.disconnect(); voice.current = null; setVoiceState(voiceOff);
    try {
      const result = await request<{roomClosed: boolean}>(`/v1/rooms/${roomId}/leave`, {});
      const party = snapshot && partyModel(snapshot, identityId);
      if (party?.role === 'owner' && !result.roomClosed) setReturnableParty(party);
      else { setReturnableParty(null); localStorage.removeItem(savedCodeKey); }
      sessionStorage.removeItem(activeCodeKey); realtime.current?.close(); setRoomId(''); setSnapshot(null); setStreamStatus('empty'); setScreen('home'); notify(result.roomClosed ? 'The party has ended' : 'You left the party');
    } catch (error) {
      if (error instanceof ApiError && ['NOT_A_MEMBER', 'ROOM_ENDED'].includes(error.code)) { sessionStorage.removeItem(activeCodeKey); realtime.current?.close(); setRoomId(''); setSnapshot(null); setScreen('home'); }
      notify(errorMessage(error));
    } finally { setLeaving(false); }
  };
  const toggleVoice = async () => {
    try {
      if (!voice.current || voiceState.status === 'error') {
        voice.current?.disconnect();
        const client = new MeshVoice({ roomId, request: (path, body) => request(path, body), token, onState: setVoiceState });
        voice.current = client;
        await client.join();
      } else await voice.current.setMuted(!voiceState.muted);
    } catch (error) { notify(error instanceof Error ? error.message : 'Voice is unavailable. Try again.'); }
  };
  const sync = async () => {
    setSyncing(true);
    try { const result = await realtime.current?.sync(playback.current.positionMs); if (result?.status === 'APPLIED') { setSyncTarget({ requestId: result.requestId, positionMs: result.targetPositionMs }); notify("You're caught up with the room"); } else if (result) notify(result.status === 'ALREADY_LEADING' ? "You're already caught up" : 'No recent room position is available. Try again shortly.'); }
    catch (error) { notify(errorMessage(error)); }
    finally { setSyncing(false); }
  };
  const toggleTheme = () => setTheme(v => v === 'light' ? 'dark' : 'light');
  const party = snapshot ? partyModel(snapshot, identityId) : null;
  if (party) party.participants = party.participants.map(p => ({ ...p, isMuted: voiceState.members[p.id]?.muted ?? true, isSpeaking: voiceState.members[p.id]?.speaking ?? false }));
  return <div className="app-shell">
    {!ready ? <main className="form-page" aria-live="polite">{initialError ? <><p role="alert">{initialError}</p><Button onClick={() => setAttempt(v => v + 1)}>Retry connection</Button></> : <LoadingIndicator label="Restoring your profile" />}</main> : <>
      {screen === 'home' && <HomeScreen live busy={joinStatus === 'loading'} identity={identity} theme={theme} onToggleTheme={toggleTheme} onSettings={() => setScreen('settings')} onCreate={() => {setManagerMode('create');setJoinStatus('idle');setJoinError('');setScreen('party-manager');}} onJoin={() => {setManagerMode('join');setJoinStatus('idle');setJoinError('');setScreen('party-manager');}} returnableParty={returnableParty} onRejoinOwnedParty={() => returnableParty && void enterRoom(`/v1/rooms/${returnableParty.code || returnableParty.id}/join`)} />}
      {screen === 'settings' && <SettingsScreen identity={identity} theme={theme} onToggleTheme={toggleTheme} onBack={() => setScreen('home')} onSave={saveProfile} saving={saving} />}
      {screen === 'party-manager' && <PartyManagerScreen initialMode={managerMode} joinStatus={joinStatus} errorMessage={joinError} codeDraft={codeDraft} onCodeChange={setCodeDraft} theme={theme} onToggleTheme={toggleTheme} onBack={() => setScreen('home')} onCreate={() => void enterRoom('/v1/rooms')} onPreview={loadPreview} onResetError={() => {setJoinStatus('idle');setJoinError('');}} live />}
      {screen === 'room-preview' && roomPreview && <><RoomPreviewScreen preview={roomPreview} joinStatus={joinStatus} theme={theme} onToggleTheme={toggleTheme} onBack={() => {setScreen('party-manager');setManagerMode('join');setJoinStatus('idle');}} onJoin={() => void enterRoom(`/v1/rooms/${roomPreview.id}/join`)} />{joinError && <p role="alert" className="room-notice">{joinError}</p>}</>}
      {screen === 'party' && party && <><div className="connection-status" role="status">{connected ? 'Connected' : 'Reconnecting to room…'}{voiceState.error ? ` · ${voiceState.error}` : ''}</div><PartyScreen key={roomId} party={party} streamStatus={streamStatus} theme={theme} aborting={aborting} onToggleTheme={toggleTheme} onStartStream={startStream} onSetStreamStatus={setStreamStatus} onAbort={abortStream} onLeave={leave} onNotify={notify} live={{ connected, leaving, voice: voiceState, onToggleVoice: toggleVoice, onResumeVoice: () => { void voice.current?.resumeAudio().catch(e => notify(e.message)); }, onDisconnectVoice: () => {voice.current?.disconnect();voice.current=null;setVoiceState(voiceOff);}, onSync: sync, syncing, syncTarget, onReport: (positionMs, playing) => { playback.current = {positionMs,playing}; realtime.current?.report(positionMs, playing); } }} /></>}
    </>}
    <Snackbar message={snackbar} onDismiss={() => setSnackbar('')} />
  </div>;
}
