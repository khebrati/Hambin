import * as Sentry from '@sentry/react';
import posthog from 'posthog-js';
import { sanitizeErrorEvent } from './privacy';

const enabled = import.meta.env.VITE_OBSERVABILITY_ENABLED === 'true' ||
  (import.meta.env.PROD && import.meta.env.VITE_OBSERVABILITY_ENABLED !== 'false');
let initialized = false;
export function initializeObservability() {
  if (!enabled || initialized) return;
  initialized = true;
  const environment = import.meta.env.VITE_OBSERVABILITY_ENVIRONMENT || import.meta.env.MODE;
  if (import.meta.env.VITE_SENTRY_DSN) Sentry.init({
    dsn: import.meta.env.VITE_SENTRY_DSN, environment,
    release: import.meta.env.VITE_APP_BUILD,
    dataCollection: { userInfo: false, cookies: false, httpHeaders: false, httpBodies: [], urlQueryParams: false, databaseQueryData: false, stackFrameVariables: false }, maxBreadcrumbs: 0,
    beforeSend: sanitizeErrorEvent,
  });
  if (import.meta.env.VITE_POSTHOG_TOKEN) {
    posthog.init(import.meta.env.VITE_POSTHOG_TOKEN, {
      api_host: import.meta.env.VITE_POSTHOG_HOST || 'https://eu.i.posthog.com',
      ui_host: 'https://eu.posthog.com',
      autocapture: false, capture_pageview: false, capture_pageleave: false,
      disable_session_recording: true, capture_exceptions: false,
      person_profiles: 'never', disable_surveys: true,
      advanced_disable_flags: true,
      persistence: 'localStorage', respect_dnt: true,
      opt_out_useragent_filter: import.meta.env.DEV,
      // Only explicit allowlisted properties; SDK URL/referrer defaults can contain invitation codes.
      before_send: event => {
        if (!event) return null;
        const allowed = ['token','distinct_id','$device_id','$session_id','$window_id','$lib','$lib_version',
          '$process_person_profile','platform','environment','release','action','route'];
        event.properties = Object.fromEntries(Object.entries(event.properties || {}).filter(([key]) => allowed.includes(key)));
        return event;
      },
    });
    posthog.register({ platform: 'web', environment, release: import.meta.env.VITE_APP_BUILD });
    track('app_opened');
    track('$pageview', { route: '/' });
  }
}
export function track(event: string, properties: { action?: string; route?: string } = {}) {
  if (!enabled || !initialized || !import.meta.env.VITE_POSTHOG_TOKEN) return;
  posthog.capture(event, properties);
}
/** Called only after a successful mutation. Paths are already masked by diagnosticRoute. */
export function trackApiSuccess(route: string, method: string) {
  if (method !== 'POST') return;
  const event = route === '/v1/rooms' ? 'room_created'
    : route.endsWith('/join') ? 'room_joined'
    : route.endsWith('/leave') ? 'room_left'
    : route.endsWith('/stream/start') ? 'playback_started'
    : route.endsWith('/bug-reports') ? 'bug_report_submitted' : undefined;
  if (event) track(event);
}
initializeObservability();
