import { useEffect, useMemo, useState } from 'react';
import { Snackbar } from './components/Material';
import { fakePartyRepository } from './data/fakeRepository';
import { defaultIdentity, makePreviewParty, previewStates } from './previewStates';
import { HomeScreen } from './screens/HomeScreen';
import { PartyManagerScreen, type ManagerMode } from './screens/PartyManagerScreen';
import { PartyScreen } from './screens/PartyScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import type {
  Identity,
  JoinStatus,
  Party,
  Screen,
  StreamStatus,
} from './types';

type Theme = 'light' | 'dark';

function getInitialTheme(): Theme {
  const queryTheme = new URLSearchParams(window.location.search).get('theme');
  if (queryTheme === 'dark' || queryTheme === 'light') return queryTheme;
  const saved = window.localStorage.getItem('hambin-theme');
  if (saved === 'dark' || saved === 'light') return saved;
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function App() {
  const params = useMemo(() => new URLSearchParams(window.location.search), []);
  const preview = previewStates[params.get('preview') || ''];
  const initialIdentity: Identity = {
    ...defaultIdentity,
    ...preview?.identity,
  };

  const [theme, setTheme] = useState<Theme>(getInitialTheme);
  const [screen, setScreen] = useState<Screen>(preview?.screen || 'home');
  const [identity, setIdentity] = useState<Identity>(initialIdentity);
  const [managerMode, setManagerMode] = useState<ManagerMode>(
    preview?.joinStatus ? 'join' : 'create',
  );
  const [joinStatus, setJoinStatus] = useState<JoinStatus>(preview?.joinStatus || 'idle');
  const [streamStatus, setStreamStatus] = useState<StreamStatus>(
    preview?.streamStatus || 'empty',
  );
  const [party, setParty] = useState<Party | null>(() =>
    preview?.screen === 'party'
      ? makePreviewParty(preview.role || 'owner', initialIdentity)
      : null,
  );
  const [aborting, setAborting] = useState(false);
  const [snackbar, setSnackbar] = useState('');

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.dataset.textScale =
      params.get('largeText') === '1' ? 'large' : 'standard';
    window.localStorage.setItem('hambin-theme', theme);
  }, [params, theme]);

  useEffect(() => {
    if (!snackbar) return;
    const timer = window.setTimeout(() => setSnackbar(''), 3200);
    return () => window.clearTimeout(timer);
  }, [snackbar]);

  const toggleTheme = () => setTheme((current) => (current === 'light' ? 'dark' : 'light'));

  const openManager = (mode: ManagerMode) => {
    setManagerMode(mode);
    setJoinStatus('idle');
    setScreen('party-manager');
  };

  const createParty = async () => {
    setJoinStatus('loading');
    const created = await fakePartyRepository.create(identity);
    setParty(created);
    setStreamStatus('empty');
    setJoinStatus('idle');
    setScreen('party');
  };

  const joinParty = async (code: string) => {
    setJoinStatus('loading');
    try {
      const joined = await fakePartyRepository.join(code, identity);
      setParty(joined);
      setStreamStatus('playing');
      setJoinStatus('idle');
      setScreen('party');
    } catch {
      setJoinStatus('error');
    }
  };

  const startStream = async (url: string) => {
    if (!party) return;
    setStreamStatus('loading');
    try {
      const normalizedUrl = await fakePartyRepository.startStream(url);
      setParty({ ...party, streamUrl: normalizedUrl });
      setStreamStatus('playing');
      setSnackbar('Stream started for the room');
    } catch {
      setStreamStatus('error');
    }
  };

  const abortStream = async () => {
    setAborting(true);
    await fakePartyRepository.abortStream();
    setParty((current) => (current ? { ...current, streamUrl: '' } : current));
    setStreamStatus('empty');
    setAborting(false);
    setSnackbar('Stream ended for everyone');
  };

  const leaveParty = () => {
    setParty(null);
    setStreamStatus('empty');
    setJoinStatus('idle');
    setScreen('home');
    setSnackbar('You left the party');
  };

  return (
    <div className="app-shell">
      {screen === 'home' ? (
        <HomeScreen
          identity={identity}
          theme={theme}
          onToggleTheme={toggleTheme}
          onSettings={() => setScreen('settings')}
          onCreate={() => openManager('create')}
          onJoin={() => openManager('join')}
        />
      ) : null}

      {screen === 'settings' ? (
        <SettingsScreen
          identity={identity}
          theme={theme}
          onToggleTheme={toggleTheme}
          onBack={() => setScreen('home')}
          onSave={(nextIdentity) => {
            setIdentity(nextIdentity);
            setScreen('home');
            setSnackbar('Profile updated');
          }}
        />
      ) : null}

      {screen === 'party-manager' ? (
        <PartyManagerScreen
          initialMode={managerMode}
          joinStatus={joinStatus}
          theme={theme}
          onToggleTheme={toggleTheme}
          onBack={() => setScreen('home')}
          onCreate={createParty}
          onJoin={joinParty}
          onResetError={() => setJoinStatus('idle')}
        />
      ) : null}

      {screen === 'party' && party ? (
        <PartyScreen
          party={party}
          streamStatus={streamStatus}
          theme={theme}
          aborting={aborting}
          onToggleTheme={toggleTheme}
          onStartStream={startStream}
          onSetStreamStatus={setStreamStatus}
          onAbort={abortStream}
          onLeave={leaveParty}
          onNotify={setSnackbar}
        />
      ) : null}

      <Snackbar message={snackbar} onDismiss={() => setSnackbar('')} />
    </div>
  );
}

