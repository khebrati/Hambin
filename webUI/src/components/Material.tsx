import {
  useEffect,
  useId,
  useRef,
  type ButtonHTMLAttributes,
  type ReactNode,
} from 'react';
import { X, type LucideIcon } from 'lucide-react';

type ButtonVariant = 'filled' | 'tonal' | 'outlined' | 'text' | 'danger';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  icon?: ReactNode;
  fullWidth?: boolean;
}

export function Button({
  variant = 'filled',
  icon,
  fullWidth = false,
  className = '',
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      className={`md-button md-button--${variant} ${fullWidth ? 'md-button--full' : ''} ${className}`}
      {...props}
    >
      {icon}
      <span>{children}</span>
    </button>
  );
}

interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  icon: LucideIcon;
  label: string;
  variant?: 'standard' | 'tonal' | 'filled' | 'danger';
  selected?: boolean;
}

export function IconButton({
  icon: Icon,
  label,
  variant = 'standard',
  selected = false,
  className = '',
  ...props
}: IconButtonProps) {
  return (
    <button
      className={`md-icon-button md-icon-button--${variant} ${selected ? 'is-selected' : ''} ${className}`}
      aria-label={label}
      aria-pressed={props['aria-pressed'] ?? (selected ? true : undefined)}
      data-tooltip={label}
      title={label}
      {...props}
    >
      <Icon aria-hidden="true" size={22} strokeWidth={2.1} />
    </button>
  );
}

interface TextFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  supportingText?: string;
  error?: string;
  disabled?: boolean;
  trailing?: ReactNode;
  inputMode?: 'text' | 'url';
  autoComplete?: string;
}

export function TextField({
  label,
  value,
  onChange,
  placeholder,
  supportingText,
  error,
  disabled,
  trailing,
  inputMode = 'text',
  autoComplete = 'off',
}: TextFieldProps) {
  const inputId = useId();
  const supportingId = useId();

  return (
    <div className={`md-field ${error ? 'md-field--error' : ''}`}>
      <label htmlFor={inputId}>{label}</label>
      <div className="md-field__control">
        <input
          id={inputId}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          disabled={disabled}
          inputMode={inputMode}
          autoComplete={autoComplete}
          aria-invalid={Boolean(error)}
          aria-describedby={error || supportingText ? supportingId : undefined}
        />
        {trailing ? <div className="md-field__trailing">{trailing}</div> : null}
      </div>
      {error || supportingText ? (
        <p id={supportingId} className="md-field__supporting">
          {error || supportingText}
        </p>
      ) : null}
    </div>
  );
}

interface DialogProps {
  open: boolean;
  title: string;
  children: ReactNode;
  actions: ReactNode;
  onClose: () => void;
}

export function Dialog({ open, title, children, actions, onClose }: DialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={dialogRef}
      className="md-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClose={onClose}
    >
      <div className="md-dialog__header">
        <h2 id={titleId}>{title}</h2>
        <IconButton icon={X} label="Close" onClick={onClose} />
      </div>
      <div className="md-dialog__content">{children}</div>
      <div className="md-dialog__actions">{actions}</div>
    </dialog>
  );
}

interface SnackbarProps {
  message: string;
  onDismiss: () => void;
}

export function Snackbar({ message, onDismiss }: SnackbarProps) {
  if (!message) return null;
  return (
    <div className="md-snackbar" role="status" aria-live="polite">
      <span>{message}</span>
      <IconButton icon={X} label="Dismiss" onClick={onDismiss} />
    </div>
  );
}

export function LoadingIndicator({ label }: { label: string }) {
  return (
    <div className="md-loading" role="status" aria-live="polite">
      <span className="md-loading__shape" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

