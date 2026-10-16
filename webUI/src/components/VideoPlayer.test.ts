import { describe, expect, it } from 'vitest';
import { forwardSyncPosition, playbackReport } from './VideoPlayer';

describe('forward-only explicit synchronization', () => {
  it('uses milliseconds from the server and clamps to media duration', () => {
    expect(forwardSyncPosition(10, 25_000, 20)).toBe(20);
    expect(forwardSyncPosition(10, 25_000, Infinity)).toBe(25);
  });
  it('never rewinds, including after duration shrinks', () => {
    expect(forwardSyncPosition(20, 10_000, 50)).toBeNull();
    expect(forwardSyncPosition(20, 20_000, 50)).toBeNull();
    expect(forwardSyncPosition(20, 30_000, 15)).toBeNull();
  });
  it('rejects nonfinite or negative server positions', () => {
    for (const value of [NaN, Infinity, -1]) expect(forwardSyncPosition(10, value, 50)).toBeNull();
  });
});

describe('actual media reports', () => {
  const ready = { readyState: 4, currentTime: 12.3456, paused: false, ended: false, seeking: false, error: null };
  it('reports actual media time in milliseconds', () => {
    expect(playbackReport(ready)).toEqual({ positionMs: 12346, playing: true });
  });
  it('does not report unloaded, seeking or failed media', () => {
    expect(playbackReport({ ...ready, readyState: 0 })).toBeNull();
    expect(playbackReport({ ...ready, seeking: true })).toBeNull();
    expect(playbackReport({ ...ready, currentTime: NaN })).toBeNull();
    expect(playbackReport({ ...ready, error: { code: 3, message: 'decode error' } as MediaError })).toBeNull();
  });
  it('does not claim paused, ended or buffering media is playing', () => {
    expect(playbackReport({ ...ready, paused: true })?.playing).toBe(false);
    expect(playbackReport({ ...ready, ended: true })?.playing).toBe(false);
    expect(playbackReport({ ...ready, readyState: 2 })?.playing).toBe(false);
  });
});
