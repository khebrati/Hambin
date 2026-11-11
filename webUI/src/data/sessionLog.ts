// One complete application session, retained until this page is closed/reloaded.
export const sessionStartedAt = new Date().toISOString();
const lines: string[] = [];
export function redactLog(value: string): string {
  return value.replace(/https?:\/\/[^\s"'<>]+/gi, '[url]')
    .replace(/Bearer\s+[^\s"',}]+/gi, 'Bearer [redacted]')
    .replace(/\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/g, '[token]')
    .replace(/(["']?(?:accessToken|refreshToken|authorization|password|credential|ticket|token)["']?\s*[:=]\s*)("[^"]*"|'[^']*'|[^\s,;}]+)/gi, '$1[redacted]');
}
export function appLog(event: string) { lines.push(`${new Date().toISOString()} ${redactLog(event)}`); }
export function sessionLog() { return lines.join('\n'); }
export function browserInfo(): Record<string, string> {
  return { platform: 'browser', build: import.meta.env.VITE_APP_BUILD || "development", userAgent: navigator.userAgent.slice(0, 2048), language: navigator.language, languages: navigator.languages.join(', '), screen: `${screen.width}x${screen.height}`, viewport: `${innerWidth}x${innerHeight}`, pixelRatio: String(devicePixelRatio), timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, online: String(navigator.onLine) };
}
appLog('Application started');
if (typeof window !== 'undefined') {
  for (const level of ['debug', 'info', 'log', 'warn', 'error'] as const) {
    const original = console[level].bind(console);
    console[level] = (...args: unknown[]) => {
      try { appLog(`${level}: ${args.map(value => value instanceof Error ? `${value.name}: ${value.message}\n${value.stack || ''}` : typeof value === 'string' ? value : JSON.stringify(value)).join(' ')}`); } catch { appLog(`${level}: [unserializable message]`); }
      original(...args);
    };
  }
  window.addEventListener('error', event => appLog(`Unhandled error: ${event.message} ${event.error?.stack || ''}`));
  window.addEventListener('unhandledrejection', event => appLog(`Unhandled rejection: ${String(event.reason)}`));
}
