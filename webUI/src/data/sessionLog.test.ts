import { describe, expect, it } from 'vitest';
import { diagnosticError, diagnosticEvent, diagnosticRoute, logHandledException, nextOperationId, redactLog, sessionLog } from './sessionLog';

describe('session diagnostics', () => {
  it('retains handled exception messages, source locations and causes with secrets redacted', () => {
    const cause = new Error('Device setup failed credential=private-credential');
    const error = new TypeError('Peer initialization failed ticket=private-ticket https://private-media/movie', { cause });
    error.stack = `${error.name}: ${error.message}\ncreatePeer@https://private-origin/assets/voice.js:375:16`;
    const before = sessionLog().length;
    logHandledException('voice.createPeer', error);
    const saved = sessionLog().slice(before);
    for (const expected of ['exception.handled', 'voice.createPeer', 'TypeError', 'Peer initialization failed', 'voice.js:375:16', 'Device setup failed']) expect(saved).toContain(expected);
    for (const secret of ['private-credential', 'private-ticket', 'private-media', 'private-origin']) expect(saved).not.toContain(secret);
  });
  it('handles cyclic causes and non-Error rejections without serializing payloads', () => {
    const error = new Error('Loop'); error.cause = error;
    const before = sessionLog().length;
    logHandledException('cycle', error);
    logHandledException('rejection', { code: 'VOICE_DISABLED', message: 'No voice', payload: 'private-payload' });
    logHandledException('string', 'Failed token=private-token');
    const saved = sessionLog().slice(before);
    expect(saved).toContain('cause cycle');
    expect(saved).toContain('VOICE_DISABLED');
    expect(saved).toContain('No voice');
    expect(saved).not.toContain('private-payload');
    expect(saved).not.toContain('private-token');
  });
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
