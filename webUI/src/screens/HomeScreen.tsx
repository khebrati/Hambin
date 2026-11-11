import { ArrowRight, Plus, Settings, Ticket } from 'lucide-react';
import type { Identity, Party } from '../types';
import { AppBar } from '../components/AppBar';
import { Avatar } from '../components/Avatar';
import { Button, IconButton } from '../components/Material';

interface HomeScreenProps {
  live?: boolean;
  busy?: boolean;
  identity: Identity;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onSettings: () => void;
  onCreate: () => void;
  onJoin: () => void;
  returnableParty: Party | null;
  onReturnToParty: () => void;
}

export function HomeScreen({
  identity,
  live = false,
  busy = false,
  theme,
  onToggleTheme,
  onSettings,
  onCreate,
  onJoin,
  returnableParty,
  onReturnToParty,
}: HomeScreenProps) {
  return (
    <div className="screen home-screen screen-enter">
      <AppBar
        title="Roomio"
        subtitle="Movie night, in your room"
        theme={theme}
        onToggleTheme={onToggleTheme}
        actions={
          <IconButton icon={Settings} label="Settings" onClick={onSettings} />
        }
      />

      <main className="home-main">
        <section className="home-stage" aria-labelledby="home-title">
          <div className="home-stage__copy">
            <p className="eyebrow">Ready when everyone is</p>
            <h2 id="home-title">Bring the film. Keep your own pace.</h2>
            <p className="home-stage__body">
              Your next watch party is one room away.
            </p>
          </div>

          <div className="identity-pill">
            <Avatar avatarId={identity.avatarId} label={identity.name} size="large" />
            <div>
              <span>Watching as</span>
              <strong>{identity.name || 'Choose a name'}</strong>
            </div>
            <IconButton icon={Settings} label="Edit profile" onClick={onSettings} />
          </div>

          <div className="home-actions">
            <Button
              variant="filled"
              icon={<Plus size={20} aria-hidden="true" />}
              onClick={onCreate}
              disabled={busy}
              fullWidth
            >
              Create a party
            </Button>
            <Button
              variant="tonal"
              icon={<Ticket size={20} aria-hidden="true" />}
              onClick={onJoin}
              disabled={busy}
              fullWidth
            >
              Join with code
            </Button>
          </div>
        </section>

        {(!live || returnableParty) && <section
          className={`home-room-preview ${returnableParty ? 'home-room-preview--returnable' : ''}`}
          aria-label="Last room"
        >
          <div className="home-room-preview__scene" aria-hidden="true">
            <span className="mini-scene__moon" />
            <span className="mini-scene__ridge mini-scene__ridge--back" />
            <span className="mini-scene__ridge mini-scene__ridge--front" />
          </div>
          <div className="home-room-preview__copy">
            <span className="eyebrow">
              Last room
            </span>
            <strong>{returnableParty?.title || "Mira's late show"}</strong>
            <span role={busy && live ? 'status' : undefined}>
              {returnableParty
                ? live ? busy ? 'Checking room…' : returnableParty.code : `${returnableParty.participants.length} friends`
                : '4 friends'}
            </span>
          </div>
          <IconButton
            icon={ArrowRight}
            label={returnableParty ? 'Return to last room' : 'Open party manager'}
            variant={returnableParty ? 'tonal' : 'standard'}
            onClick={returnableParty ? onReturnToParty : onJoin}
            disabled={busy}
          />
        </section>}
      </main>
    </div>
  );
}
