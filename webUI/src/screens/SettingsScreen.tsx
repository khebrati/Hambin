import { Check } from 'lucide-react';
import { themeColors, type useThemePalette } from '../theme';
import type { CSSProperties } from 'react';
import { useState } from 'react';
import { avatarOptions } from '../data/avatarCatalog';
import type { Identity } from '../types';
import { AppBar } from '../components/AppBar';
import { Avatar } from '../components/Avatar';
import { Button, TextField } from '../components/Material';

interface SettingsScreenProps {
  palette: ReturnType<typeof useThemePalette>;
  identity: Identity;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onBack: () => void;
  onSave: (identity: Identity) => void;
  saving?: boolean;
}

export function SettingsScreen({
  identity,
  palette,
  theme,
  onToggleTheme,
  onBack,
  onSave,
  saving = false,
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

        <section className="form-section" aria-labelledby="color-heading">
          <div className="section-heading">
            <span>03</span>
            <div><h2 id="color-heading">Color theme</h2><p>Choose a color to make Roomio yours. Applies immediately.</p></div>
          </div>
          <div className="theme-color-picker" role="group" aria-label="Color theme">
            {[{ id: 'roomio', name: 'Roomio default', seed: '#8F3B49' }, ...themeColors].map(color => (
              <button key={color.id} type="button" className="theme-color-choice"
                aria-pressed={palette.colorId === color.id}
                style={{ '--seed-color': color.seed } as CSSProperties}
                onClick={() => palette.selectColor(color.id)}>
                <span className="theme-color-swatch" aria-hidden="true" />
                <span>{color.name}</span>
                {palette.colorId === color.id && <Check size={18} aria-hidden="true" />}
              </button>
            ))}
          </div>
          {palette.saveFailed && <p role="alert">Color applied for now. Couldn’t remember it on this device. Select it again to retry.</p>}
        </section>

        <section className="form-section" aria-labelledby="language-heading">
          <div className="section-heading">
            <span>04</span>
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
            disabled={!trimmedName || [...trimmedName].length > 48 || saving}
            onClick={() => onSave({ ...draft, name: trimmedName })}
          >
            {saving ? 'Saving…' : 'Save profile'}
          </Button>
        </div>
      </main>
    </div>
  );
}
