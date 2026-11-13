import { useState } from 'react';
import { BugReportDialog } from '../components/BugReportDialog';
import { Snackbar } from '../components/Material';
import { t } from '../i18n';
import { ArrowRight, Bug, Moon, Plus, Settings, Sun, Ticket } from 'lucide-react';
import type { Identity, Party } from '../types';
import { Avatar } from '../components/Avatar';
import { Button, IconButton } from '../components/Material';
import { AndroidDownloadRow } from '../components/AndroidDownloadRow';

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
  const [reportOpen, setReportOpen] = useState(false);
  const [reportSent, setReportSent] = useState(false);
  return (
    <div className="screen home-screen screen-enter">
      <main className="home-main">
        <div className="home-tools" role="group" aria-label={t("Page settings")}>
          <IconButton icon={Bug} label={t("Report a bug")} onClick={() => setReportOpen(true)} />
          <IconButton
            icon={theme === 'dark' ? Sun : Moon}
            label={theme === 'dark' ? t("Use light theme") : t("Use dark theme")}
            className="theme-toggle"
            onClick={onToggleTheme}
          />
          <IconButton icon={Settings} label={t("Settings")} onClick={onSettings} />
        </div>
        <section className="home-stage" aria-labelledby="home-title">
          <div className="home-stage__copy">
            <h1 id="home-title">{t("Stream and chill")}</h1>
          </div>

          <div className="identity-pill">
            <Avatar avatarId={identity.avatarId} label={identity.name} size="large" />
            <div>
              <span>{t("Watching as")}</span>
              <strong>{identity.name || t("Choose a name")}</strong>
            </div>
            <IconButton icon={Settings} label={t("Edit profile")} onClick={onSettings} />
          </div>

          <div className="home-actions">
            <Button
              variant="filled"
              icon={<Plus size={20} aria-hidden="true" />}
              onClick={onCreate}
              disabled={busy}
              fullWidth
            >
              {t("Create a party")}</Button>
            <Button
              variant="tonal"
              icon={<Ticket size={20} aria-hidden="true" />}
              onClick={onJoin}
              disabled={busy}
              fullWidth
            >
              {t("Join with code")}</Button>
          </div>
          <AndroidDownloadRow />
        </section>

        {(!live || returnableParty) && <section
          className={`home-room-preview ${returnableParty ? 'home-room-preview--returnable' : ''}`}
          aria-label={t("Last room")}
        >
          <div className="home-room-preview__scene" aria-hidden="true">
            <span className="mini-scene__moon" />
            <span className="mini-scene__ridge mini-scene__ridge--back" />
            <span className="mini-scene__ridge mini-scene__ridge--front" />
          </div>
          <div className="home-room-preview__copy">
            <span className="eyebrow">
              {t("Last room")}</span>
            <strong>{returnableParty?.title || "Mira's late show"}</strong>
            <span role={busy && live ? 'status' : undefined}>
              {returnableParty
                ? live ? busy ? t("Checking room…") : returnableParty.code : t('{count} friends', { count: returnableParty.participants.length })
                : t('{count} friends', { count: 4 })}
            </span>
          </div>
          <IconButton
            className="directional-icon" icon={ArrowRight}
            label={returnableParty ? t("Return to last room") : t("Open party manager")}
            variant={returnableParty ? 'tonal' : 'standard'}
            onClick={returnableParty ? onReturnToParty : onJoin}
            disabled={busy}
          />
        </section>}
      </main>
      <BugReportDialog open={reportOpen} enabled={live} onClose={() => setReportOpen(false)} onSent={() => setReportSent(true)} />
      <Snackbar message={reportSent ? t("Bug report sent. Thank you.") : ""} onDismiss={() => setReportSent(false)} />
    </div>
  );
}
