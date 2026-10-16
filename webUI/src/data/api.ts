import { token } from './auth';
export class ApiError extends Error {
  constructor(public code: string, public status = 0) { super(code); }
}
export const backendUrl = (import.meta.env.VITE_BACKEND_URL || '').replace(/\/$/, '');
export async function request<T>(path: string, body?: unknown, method = body === undefined ? 'GET' : 'POST', idempotencyKey?: string): Promise<T> {
  if (!path.startsWith('/v1/')) throw new Error('Invalid API path');
  const send = async (force: boolean) => {
    const idToken = await token(force);
    try {
      return await fetch(`${backendUrl}${path}`, { method, headers: { Authorization: `Bearer ${idToken}`, ...(body === undefined ? {} : { 'Content-Type': 'application/json' }), ...(idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}) }, body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(15000) });
    } catch { throw new ApiError('NETWORK'); }
  };
  let response = await send(false);
  if (response.status === 401) {
    const failure = await response.clone().json().catch(() => ({}));
    if (['SESSION_EXPIRED', 'INVALID_TOKEN', 'UNAUTHENTICATED'].includes(failure.code)) response = await send(true);
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new ApiError(error.code || 'UNAVAILABLE', response.status);
  }
  return response.status === 204 ? undefined as T : response.json();
}
const messages: Record<string, string> = {
  INVALID_STREAM_URL: 'Enter a valid HTTPS video URL.', FORBIDDEN: 'Only the room owner can change the stream.', OWNER_ABSENT: 'The owner must be present to change the stream.', ALREADY_IN_ROOM: 'You already have an active party. Return to it and leave before opening another.', STREAM_ACTIVE: 'End the current stream before starting another.', STALE_STREAM_REVISION: 'The stream changed. Refresh the room and try again.',
  NETWORK: "You're offline. Check your connection and try again.", ROOM_NOT_FOUND: 'That party could not be found. Check the code and try again.', ROOM_FULL: 'This room is full.', ROOM_ENDED: 'This party has ended.', NOT_A_MEMBER: 'Your room session has ended. Join the party again.', OWNER_REQUIRED: 'Only the present owner can change the stream.', INVALID_URL: 'Enter a valid HTTPS video URL.', INVALID_PROFILE: 'Use a name of 1–48 characters.', RATE_LIMITED: 'Too many requests. Wait a moment and try again.', AUTH_UNAVAILABLE: 'Identity service is unavailable. Your saved identity is kept; try again.', SESSION_REVOKED: 'This session was revoked. Reset the site identity explicitly to start again.', VOICE_DISABLED: 'Voice is unavailable on this server.', UNAVAILABLE: 'The service is unavailable. Try again.'
};
export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? messages[error.code] || 'That action failed. Please try again.' : error instanceof Error && error.message.startsWith('Firebase is not configured') ? error.message : 'Unable to restore your session. Check your connection and try again.';
}

export class MutationRequests {
  private keys = new Map<string, string>();
  async run<T>(path: string, body: unknown): Promise<T> {
    const operation = JSON.stringify([path, body]);
    const key = this.keys.get(operation) || crypto.randomUUID();
    this.keys.set(operation, key);
    try {
      const result = await request<T>(path, body, 'POST', key);
      this.keys.delete(operation);
      return result;
    } catch (error) {
      if (error instanceof ApiError && error.status > 0 && error.status < 500) this.keys.delete(operation);
      throw error;
    }
  }
}
