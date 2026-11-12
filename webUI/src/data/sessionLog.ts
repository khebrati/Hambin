// One complete application session, retained until this page is closed/reloaded.
export const sessionStartedAt = new Date().toISOString();
const lines: string[] = [];
let operationSequence = 0;
export function nextOperationId() { return ++operationSequence; }
export function redactLog(value: string): string {
  return value.replace(/(?:https?|wss?|content|file|blob):[^\s"'<>]+/gi, '[url]')
    .replace(/Bearer\s+[^\s"',}]+/gi, 'Bearer [redacted]')
    .replace(/\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/g, '[token]')
    .replace(/(["']?(?:accessToken|refreshToken|authorization|password|credential|ticket|token)["']?\s*[:=]\s*)("[^"]*"|'[^']*'|[^\s,;}]+)/gi, '$1[redacted]');
}
export function appLog(event: string) { lines.push(`${new Date().toISOString()} ${redactLog(event)}`); }
export function diagnosticEvent(event: string, fields: Record<string, string | number | boolean | undefined> = {}) {
  appLog(JSON.stringify({ event, ...fields }));
}
/** Fixed route labels only: identifiers, invitation codes, query values and unknown paths are never retained. */
export function diagnosticRoute(path: string): string {
  const route = path.split(/[?#]/, 1)[0];
  if (/^\/v1\/rooms\/[^/]+(?:\/(?:preview|join|leave|end|snapshot|stream\/(?:start|abort)|realtime-ticket|voice\/config|bug-reports|chat\/messages))?$/.test(route))
    return route.replace(/(\/rooms\/)[^/]+/, '$1[room]');
  if (/^\/v1\/guest-sessions(?:\/(?:refresh|reset|revoke|profile))?$/.test(route) || route === '/v1/rooms') return route;
  return '[other]';
}
export function diagnosticError(error: unknown): string {
  // Never serialize exception messages: they may contain chat text, media URLs, SDP or filenames.
  if (error && typeof error === 'object' && 'code' in error && typeof error.code === 'string' && /^[A-Z][A-Z_]{1,63}$/.test(error.code)) return error.code;
  return error instanceof Error && ['AbortError', 'TimeoutError', 'NotAllowedError', 'NotFoundError', 'NotReadableError', 'TypeError', 'SyntaxError'].includes(error.name) ? error.name : 'UNKNOWN';
}
export function sessionLog() { return lines.join('\n'); }
export function browserInfo(): Record<string, string> {
  return { platform: 'browser', build: import.meta.env.VITE_APP_BUILD || "development", userAgent: navigator.userAgent.slice(0, 2048), language: navigator.language, languages: navigator.languages.join(', '), screen: `${screen.width}x${screen.height}`, viewport: `${innerWidth}x${innerHeight}`, pixelRatio: String(devicePixelRatio), timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, online: String(navigator.onLine) };
}
appLog('Application started');
diagnosticEvent('app.started', { build: import.meta.env.VITE_APP_BUILD || 'development' });
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

if (typeof window !== 'undefined') {
  window.addEventListener('online', () => diagnosticEvent('app.connectivity', { online: true }));
  window.addEventListener('offline', () => diagnosticEvent('app.connectivity', { online: false }));
  window.addEventListener('pageshow', event => diagnosticEvent('app.pageshow', { restored: event.persisted }));
  document.addEventListener('visibilitychange', () => diagnosticEvent('app.visibility', { state: document.visibilityState }));
}
