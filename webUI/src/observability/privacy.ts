import { logHandledException } from '../data/sessionLog';
/** Keep stack locations, while removing fields that can contain product content or credentials. */
export function sanitizeErrorEvent<T extends {
  request?: unknown; user?: unknown; extra?: unknown; contexts?: unknown; breadcrumbs?: unknown;
  message?: string; exception?: { values?: { type?: string; value?: string; stacktrace?: { frames?: { vars?: unknown; filename?: string; abs_path?: string }[] } }[] };
}>(event: T): T {
  delete event.request; delete event.user; delete event.extra;
  event.contexts = {}; event.breadcrumbs = [];
  if (event.message) event.message = 'Application error';
  for (const exception of event.exception?.values ?? []) {
    exception.value = exception.type || 'Application error';
    for (const frame of exception.stacktrace?.frames ?? []) {
      delete frame.vars;
      for (const key of ['filename', 'abs_path'] as const) {
        const value = frame[key];
        if (value) {
          try { const url = new URL(value); frame[key] = url.origin + url.pathname; }
          catch (handledError) { logHandledException("privacy.sanitizeErrorEvent.1", handledError);  frame[key] = value.split(/[?#]/)[0]; }
        }
      }
    }
  }
  return event;
}
