import { describe, expect, it } from 'vitest';
import { diagnosticError, diagnosticEvent, diagnosticRoute, nextOperationId, redactLog, sessionLog } from './sessionLog';

describe('session diagnostics', () => {
  it('retains ordered structured events and monotonic operation IDs', () => {
    const a = nextOperationId(), b = nextOperationId();
    expect(b).toBe(a + 1);
    diagnosticEvent('http.start', { operation: a, route: diagnosticRoute('/v1/rooms/PRIVATE/chat/messages?before=99') });
    diagnosticEvent('http.response', { operation: a, status: 201, elapsedMs: 12 });
    const log = sessionLog();
    expect(log).toContain('"event":"http.start"');
    expect(log).toContain('"elapsedMs":12');
    expect(log.indexOf('"event":"http.start"')).toBeLessThan(log.indexOf('"event":"http.response"'));
    expect(log).not.toContain('PRIVATE');
    expect(log).not.toContain('before=');
  });
  it('retains operation routes but discards private and unknown path components', () => {
    expect(diagnosticRoute('/v1/rooms/SECRET/preview')).toBe('/v1/rooms/[room]/preview');
    expect(diagnosticRoute('/v1/rooms/SECRET/stream/start')).toBe('/v1/rooms/[room]/stream/start');
    expect(diagnosticRoute('/v1/guest-sessions/refresh')).toBe('/v1/guest-sessions/refresh');
    expect(diagnosticRoute('/v1/unknown/private-value?token=secret')).toBe('[other]');
  });
  it('redacts credentials and remote, socket and local resource URLs before retention', () => {
    const value = redactLog('ticket=secret refreshToken="opaque" Bearer private https://movie.test/private wss://socket.test/?ticket=hidden content://files/subtitle.srt file:///private/name blob:http://localhost/private');
    for (const secret of ['secret', 'opaque', 'private', 'movie.test', 'socket.test', 'hidden', 'subtitle.srt', 'localhost']) expect(value).not.toContain(secret);
  });
  it('reports safe error categories without retaining arbitrary messages or codes', () => {
    expect(diagnosticError(new DOMException('private device name', 'NotAllowedError'))).toBe('NotAllowedError');
    expect(diagnosticError({ code: 'ROOM_ENDED', message: 'private name' })).toBe('ROOM_ENDED');
    expect(diagnosticError({ code: 'my private message' })).toBe('UNKNOWN');
    expect(diagnosticError(new Error('private message'))).toBe('UNKNOWN');
  });
});
