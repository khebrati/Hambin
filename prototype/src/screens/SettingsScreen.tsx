import { Check } from 'lucide-react';
import { useState } from 'react';
import { avatarOptions } from '../data/fakeRepository';
import type { Identity } from '../types';
import { AppBar } from '../components/AppBar';
import { Avatar } from '../components/Avatar';
import { Button, TextField } from '../components/Material';

interface SettingsScreenProps {
  identity: Identity;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onBack: () => void;
  onSave: (identity: Identity) => void;
}

export function SettingsScreen({
  identity,
  theme,
  onToggleTheme,
  onBack,
  onSave,
}: SettingsScreenProps) {
  const [draft, setDraft] = useState(identity);
  const trimmedName = draft.name.trim();

  return (
    <div className="screen settings-screen screen-enter">
      <AppBar
        title="Your profile"
        subtitle="Shown inside every party"
        canGoBack
        onBack={onBack}
        theme={theme}
        onToggleTheme={onToggleTheme}
      />

      <main className="form-page">
        <section className="profile-spotlight" aria-label="Profile preview">
          <Avatar avatarId={draft.avatarId} label={trimmedName || 'Your avatar'} size="large" />
          <div>
            <span>Ready to watch</span>
            <strong>{trimmedName || 'Add your name'}</strong>
          </div>
        </section>

        <section className="form-section" aria-labelledby="identity-heading">
          <div className="section-heading">
            <span>01</span>
            <div>
              <h2 id="identity-heading">Name</h2>
              <p>Keep it recognizable to friends.</p>
            </div>
          </div>
          <TextField
            label="Display name"
            value={draft.name}
            onChange={(name) => setDraft((current) => ({ ...current, name }))}
            placeholder="Nika"
            supportingText={!trimmedName ? 'A name is needed before saving.' : undefined}
          />
        </section>

        <section className="form-section" aria-labelledby="avatar-heading">
          <div className="section-heading">
            <span>02</span>
            <div>
              <h2 id="avatar-heading">Avatar</h2>
              <p>Pick the face friends will see in the room.</p>
            </div>
          </div>
          <div className="avatar-picker" role="radiogroup" aria-label="Choose an avatar">
            {avatarOptions.map((avatar) => {
              const selected = draft.avatarId === avatar.id;
              return (
                <button
                  key={avatar.id}
                  className={`avatar-choice ${selected ? 'is-selected' : ''}`}
                  role="radio"
                  aria-checked={selected}
                  onClick={() =>
                    setDraft((current) => ({ ...current, avatarId: avatar.id }))
                  }
                >
                  <Avatar
                    avatarId={avatar.id}
                    label={avatar.label}
                    selected={selected}
                    size="large"
                  />
                  <span>{avatar.label}</span>
                  {selected ? <Check size={16} aria-hidden="true" /> : null}
                </button>
              );
            })}
          </div>
        </section>

        <section className="form-section" aria-labelledby="language-heading">
          <div className="section-heading">
            <span>03</span>
            <div>
              <h2 id="language-heading">Language</h2>
              <p>English is selected for this design pass.</p>
            </div>
          </div>
          <label className="md-select">
            <span>App language</span>
            <select value={draft.language} disabled aria-label="App language">
              <option>English</option>
            </select>
          </label>
        </section>

        <div className="sticky-action">
          <Button
            fullWidth
            disabled={!trimmedName}
            onClick={() => onSave({ ...draft, name: trimmedName })}
          >
            Save profile
          </Button>
        </div>
      </main>
    </div>
  );
}

