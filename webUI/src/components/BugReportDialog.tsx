import { logHandledException } from '../data/sessionLog';
import { t } from '../i18n';
import { useEffect, useRef, useState } from 'react';
import { Button, Dialog, StatusCard } from './Material';
import { errorMessage, request } from '../data/api';
import { diagnosticEvent, diagnosticError, browserInfo, sessionLog, sessionStartedAt } from '../data/sessionLog';

export function BugReportDialog({ open, roomId, onClose, onSent, enabled }: {open: boolean; roomId: string; onClose: () => void; onSent: () => void; enabled: boolean}) {
  const [description, setDescription] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [online, setOnline] = useState(navigator.onLine);
  const pending = useRef<{description: string; payload: Record<string, unknown>} | null>(null);
  const inFlight = useRef(false);
  useEffect(() => {
    const update = () => setOnline(navigator.onLine);
    window.addEventListener('online', update); window.addEventListener('offline', update);
    return () => { window.removeEventListener('online', update); window.removeEventListener('offline', update); };
  }, []);
  const send = async () => {
    const text = description.trim();
    if (!text || !online || inFlight.current || !enabled) return;
    diagnosticEvent('report.send', { retry: pending.current?.description === text });
    inFlight.current = true; setSending(true); setError('');
    try {
      if (pending.current?.description !== text) pending.current = {description: text, payload: {reportId: crypto.randomUUID(), description: text, clientInfo: browserInfo(), sessionStartedAt, log: sessionLog()}};
      await request(`/v1/rooms/${encodeURIComponent(roomId)}/bug-reports`, pending.current.payload);
      diagnosticEvent('report.accepted');
      pending.current = null; setDescription(''); onClose(); onSent();
    } catch (failure) { logHandledException("BugReportDialog.BugReportDialog.send.1", failure);  diagnosticEvent('report.failure', { code: diagnosticError(failure) }); setError(errorMessage(failure)); }
    finally { inFlight.current = false; setSending(false); }
  };
  return <Dialog className="bug-report-dialog" open={open} title={t("Report a bug")} onClose={() => { if (!inFlight.current) onClose(); }} actions={<>
    <Button variant="text" disabled={sending} onClick={onClose}>{t("Cancel")}</Button>
    <Button disabled={!enabled || !description.trim() || sending || !online} onClick={() => void send()}>{sending ? t("Sending…") : t("Send")}</Button>
  </>}>
    <label className="bug-report-field"><span>{t("What went wrong?")}</span><textarea dir="auto" autoFocus rows={7} maxLength={8000} value={description} disabled={sending} onChange={event => { setDescription(event.target.value); setError(''); }} placeholder={t("Describe the bug and what you were doing…")} /></label>
    {!online && <p role="status">{t("You’re offline. Your draft is kept. Reconnect to send.")}</p>}
    {error && <StatusCard title={t("Report wasn’t sent")} tone="error">{error} {t("Your draft is kept; try Send again.")}</StatusCard>}
  </Dialog>;
}
