import { DoorOpen, Plus, Ticket } from 'lucide-react';
import { useEffect, useState } from 'react';
import { AppBar } from '../components/AppBar';
import { Button, LoadingIndicator, TextField } from '../components/Material';
import type { JoinStatus } from '../types';

export type ManagerMode = 'create' | 'join';

interface PartyManagerScreenProps {
  initialMode: ManagerMode;
  joinStatus: JoinStatus;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onBack: () => void;
  onCreate: () => void;
  onPreview: (code: string) => void;
  onResetError: () => void;
}

export function PartyManagerScreen({
  initialMode,
  joinStatus,
  theme,
  onToggleTheme,
  onBack,
  onCreate,
  onPreview,
  onResetError,
}: PartyManagerScreenProps) {
  const [mode, setMode] = useState(initialMode);
  const [code, setCode] = useState(joinStatus === 'error' ? 'NOVA-99' : '');

  useEffect(() => setMode(initialMode), [initialMode]);

  const changeMode = (nextMode: ManagerMode) => {
    setMode(nextMode);
    onResetError();
  };

  return (
    <div className="screen manager-screen screen-enter">
      <AppBar
        title="Party manager"
        subtitle="Create a room or join friends"
        canGoBack
        onBack={onBack}
        theme={theme}
        onToggleTheme={onToggleTheme}
      />

      <main className="manager-main">
        <div className="segmented-control" role="tablist" aria-label="Party action">
          <button
            role="tab"
            aria-selected={mode === 'create'}
            className={mode === 'create' ? 'is-selected' : ''}
            onClick={() => changeMode('create')}
          >
            <Plus size={18} aria-hidden="true" />
            Create
          </button>
          <button
            role="tab"
            aria-selected={mode === 'join'}
            className={mode === 'join' ? 'is-selected' : ''}
            onClick={() => changeMode('join')}
          >
            <Ticket size={18} aria-hidden="true" />
            Join
          </button>
        </div>

        {mode === 'create' ? (
          <section className="manager-panel" aria-labelledby="create-title">
            <div className="manager-visual manager-visual--create" aria-hidden="true">
              <span className="manager-visual__screen" />
              <span className="manager-visual__seat manager-visual__seat--one" />
              <span className="manager-visual__seat manager-visual__seat--two" />
              <span className="manager-visual__seat manager-visual__seat--three" />
            </div>
            <p className="eyebrow">You will be the host</p>
            <h2 id="create-title">Open a room for movie night</h2>
            <p>
              Roomio will create a party code and take you to the empty cinema.
            </p>
            {joinStatus === 'loading' ? (
              <LoadingIndicator label="Opening your room" />
            ) : (
              <Button
                fullWidth
                icon={<DoorOpen size={20} aria-hidden="true" />}
                onClick={onCreate}
              >
                Create party
              </Button>
            )}
          </section>
        ) : (
          <section className="manager-panel" aria-labelledby="join-title">
            <p className="eyebrow">A friend has the code</p>
            <h2 id="join-title">Step into their room</h2>
            <TextField
              label="Party code"
              value={code}
              onChange={(value) => {
                setCode(value.toUpperCase());
                if (joinStatus === 'error') onResetError();
              }}
              placeholder="MOON-42"
              error={
                joinStatus === 'error'
                  ? "We couldn't find that party. Check the code and try again."
                  : undefined
              }
              disabled={joinStatus === 'loading'}
            />
            {joinStatus === 'loading' ? (
              <LoadingIndicator label="Finding that party" />
            ) : (
              <Button
                fullWidth
                icon={<Ticket size={20} aria-hidden="true" />}
                disabled={!code.trim()}
                onClick={() => onPreview(code)}
              >
                Preview party
              </Button>
            )}
            <p className="manager-code-hints">
              Try <strong>MOON-42</strong>, <strong>ORBIT-08</strong>, <strong>FULL-10</strong>, or <strong>ENDED-3</strong>.
            </p>
          </section>
        )}
      </main>
    </div>
  );
}
