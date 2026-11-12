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
  // A compact category for state/events; logHandledException retains redacted details separately.
  if (error && typeof error === 'object' && 'code' in error && typeof error.code === 'string' && /^[A-Z][A-Z_]{1,63}$/.test(error.code)) return error.code;
  return error instanceof Error && ['AbortError', 'TimeoutError', 'NotAllowedError', 'NotFoundError', 'NotReadableError', 'TypeError', 'SyntaxError'].includes(error.name) ? error.name : 'UNKNOWN';
}
/** Called at recovery boundaries, including failures that never reach the console. */
export function logHandledException(operation: string, error: unknown): void {
  // Logging must not turn an otherwise handled failure into an application failure.
  try {
    const seen = new Set<unknown>();
    const details = (value: unknown, depth = 0): Record<string, unknown> => {
      if (depth > 5 || seen.has(value)) return { message: '[cause cycle or depth limit]' };
      seen.add(value);
      if (value instanceof Error) return {
        name: value.name, message: redactLog(value.message),
        // Preserve source filename/line/column while removing origins, queries and private paths.
        stack: redactLog((value.stack || '').replace(/(?:https?|file|blob):[^\s)]+/gi, url => {
          const location = url.split(/[?#]/, 1)[0];
          return '[source]/' + location.substring(location.lastIndexOf('/') + 1);
        })),
        ...(value.cause === undefined ? {} : { cause: details(value.cause, depth + 1) }),
        ...(value instanceof AggregateError ? { errors: value.errors.slice(0, 10).map(cause => details(cause, depth + 1)) } : {}),
      };
      // Do not serialize arbitrary payload objects (e.g. HTTP bodies, SDP or user content).
      if (value && typeof value === 'object') {
        const item = value as { name?: unknown; message?: unknown; code?: unknown };
        return { name: typeof item.name === 'string' ? redactLog(item.name) : 'NonError',
          message: typeof item.message === 'string' ? redactLog(item.message) : '[non-Error exception]', code: diagnosticError(value) };
      }
      return { name: 'NonError', message: redactLog(String(value)) };
    };
    appLog(JSON.stringify({ event: 'exception.handled', operation, ...details(error) }));
  } catch { /* The logging boundary cannot recursively log its own failure. */ }
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
