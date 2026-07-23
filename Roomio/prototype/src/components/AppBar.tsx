import { ArrowLeft, Moon, Sun } from 'lucide-react';
import { IconButton } from './Material';

interface AppBarProps {
  title: string;
  subtitle?: string;
  canGoBack?: boolean;
  onBack?: () => void;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  actions?: React.ReactNode;
}

export function AppBar({
  title,
  subtitle,
  canGoBack = false,
  onBack,
  theme,
  onToggleTheme,
  actions,
}: AppBarProps) {
  return (
    <header className="app-bar">
      {canGoBack ? (
        <IconButton icon={ArrowLeft} label="Back" onClick={onBack} />
      ) : (
        <span className="brand-mark" aria-hidden="true">
          <span />
          <span />
        </span>
      )}
      <div className="app-bar__title">
        <h1>{title}</h1>
        {subtitle ? <p>{subtitle}</p> : null}
      </div>
      <div className="app-bar__actions">
        {actions}
        <IconButton
          icon={theme === 'dark' ? Sun : Moon}
          label={theme === 'dark' ? 'Use light theme' : 'Use dark theme'}
          className="theme-toggle"
          onClick={onToggleTheme}
        />
      </div>
    </header>
  );
}
