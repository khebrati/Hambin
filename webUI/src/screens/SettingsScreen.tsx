import { t } from '../i18n';
import { Check } from 'lucide-react';
import { themeColors, type useThemePalette } from '../theme';
import type { CSSProperties } from 'react';
import { useState } from 'react';
import { avatarOptions, avatarName } from '../data/avatarCatalog';
import type { Identity } from '../types';
import { AppBar } from '../components/AppBar';
import { Avatar } from '../components/Avatar';
import { Button } from '../components/Material';

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
  const name = avatarName(draft.avatarId, draft.language);

  return (
    <div className="screen settings-screen screen-enter">
      <AppBar
        title={t("Your profile")}
        subtitle={t("Shown inside every party")}
        canGoBack
        onBack={onBack}
        theme={theme}
        onToggleTheme={onToggleTheme}
      />

      <main className="form-page">
        <section className="profile-spotlight" aria-label={t("Profile preview")}>
          <Avatar avatarId={draft.avatarId} label={name} size="large" />
          <div>
            <span>{t("Ready to watch")}</span>
            <strong>{name}</strong>
          </div>
        </section>

        <section className="form-section" aria-labelledby="avatar-heading">
          <div className="section-heading">
            <span>01</span>
            <div>
              <h2 id="avatar-heading">{t("Avatar")}</h2>
              <p>{t("Your avatar sets your name. Pick the character friends will see.")}</p>
            </div>
          </div>
          <div className="avatar-picker" role="radiogroup" aria-label={t("Choose an avatar")}>
            {avatarOptions.map((avatar) => {
              const selected = draft.avatarId === avatar.id;
              return (
                <button
                  key={avatar.id}
                  className={`avatar-choice ${selected ? 'is-selected' : ''}`}
                  role="radio"
                  aria-label={t(avatar.label)}
                  aria-checked={selected}
                  onClick={() =>
                    setDraft((current) => ({ ...current, avatarId: avatar.id }))
                  }
                >
                  <Avatar
                    avatarId={avatar.id}
                    label={t(avatar.label)}
                    selected={selected}
                    size="large"
                  />
                  <span>{t(avatar.label)}</span>
                  {selected ? <Check size={16} aria-hidden="true" /> : null}
                </button>
              );
            })}
          </div>
        </section>

        <section className="form-section" aria-labelledby="color-heading">
          <div className="section-heading">
            <span>02</span>
            <div><h2 id="color-heading">{t("Color theme")}</h2><p>{t("Choose a color to make Watch Party yours. Applies immediately.")}</p></div>
          </div>
          <div className="theme-color-picker" role="group" aria-label={t("Color theme")}>
            {[{ id: 'roomio', name: t("Watch Party default"), seed: '#8F3B49' }, ...themeColors].map(color => (
              <button key={color.id} type="button" className="theme-color-choice"
                aria-pressed={palette.colorId === color.id}
                style={{ '--seed-color': color.seed } as CSSProperties}
                onClick={() => palette.selectColor(color.id)}>
                <span className="theme-color-swatch" aria-hidden="true" />
                <span>{t(color.name)}</span>
                {palette.colorId === color.id && <Check size={18} aria-hidden="true" />}
              </button>
            ))}
          </div>
          {palette.saveFailed && <p role="alert">{t("Color applied for now. Couldn’t remember it on this device. Select it again to retry.")}</p>}
        </section>

        <section className="form-section" aria-labelledby="language-heading">
          <div className="section-heading">
            <span>03</span>
            <div>
              <h2 id="language-heading">{t("Language")}</h2>
              <p>{t('Choose your app language. Applies after saving.')}</p>
            </div>
          </div>
          <label className="md-select">
            <span>{t("App language")}</span>
            <select value={draft.language} onChange={event => setDraft(current => ({ ...current, language: event.target.value as Identity['language'] }))} aria-label={t("App language")}>
              <option value="English" lang="en">English</option>
              <option value="فارسی" lang="fa">فارسی</option>
            </select>
          </label>
        </section>

        <div className="sticky-action">
          <Button
            fullWidth
            disabled={saving}
            onClick={() => onSave({ ...draft, name })}
          >
            {saving ? t("Saving…") : t("Save profile")}
          </Button>
        </div>
      </main>
    </div>
  );
}
