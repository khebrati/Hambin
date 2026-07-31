import { ArrowRight, Crown, Plus, Settings, Ticket } from 'lucide-react';
import type { Identity, Party } from '../types';
import { AppBar } from '../components/AppBar';
import { Avatar } from '../components/Avatar';
import { Button, IconButton } from '../components/Material';

interface HomeScreenProps {
  identity: Identity;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onSettings: () => void;
  onCreate: () => void;
  onJoin: () => void;
  returnableParty: Party | null;
  onRejoinOwnedParty: () => void;
}

export function HomeScreen({
  identity,
  theme,
  onToggleTheme,
  onSettings,
  onCreate,
  onJoin,
  returnableParty,
  onRejoinOwnedParty,
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
              fullWidth
            >
              Create a party
            </Button>
            <Button
              variant="tonal"
              icon={<Ticket size={20} aria-hidden="true" />}
              onClick={onJoin}
              fullWidth
            >
              Join with code
            </Button>
          </div>
        </section>

        <section
          className={`home-room-preview ${returnableParty ? 'home-room-preview--returnable' : ''}`}
          aria-label={returnableParty ? 'Your ownerless room' : 'Recent room'}
        >
          <div className="home-room-preview__scene" aria-hidden="true">
            <span className="mini-scene__moon" />
            <span className="mini-scene__ridge mini-scene__ridge--back" />
            <span className="mini-scene__ridge mini-scene__ridge--front" />
          </div>
          <div className="home-room-preview__copy">
            <span className="eyebrow">
              {returnableParty ? 'Your room is waiting' : 'Last room'}
            </span>
            <strong>{returnableParty?.title || "Mira's late show"}</strong>
            <span>
              {returnableParty
                ? `${returnableParty.participants.length} friends · No owner`
                : '4 friends'}
            </span>
          </div>
          <IconButton
            icon={returnableParty ? Crown : ArrowRight}
            label={returnableParty ? 'Rejoin as owner' : 'Open party manager'}
            variant={returnableParty ? 'tonal' : 'standard'}
            onClick={returnableParty ? onRejoinOwnedParty : onJoin}
          />
        </section>
      </main>
    </div>
  );
}
