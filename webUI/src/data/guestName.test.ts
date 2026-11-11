import { describe, expect, it } from 'vitest';
import { guestNames } from './guestName';

describe('default character names', () => {
  it('offers at least 100 distinct names within profile limits, without digits', () => {
    expect(new Set(guestNames).size).toBeGreaterThanOrEqual(100);
    expect(new Set(guestNames).size).toBe(guestNames.length);
    for (const name of guestNames) {
      expect(name.trim()).toBe(name);
      expect(name.length).toBeGreaterThan(0);
      expect(name.length).toBeLessThanOrEqual(48);
      expect(name).not.toMatch(/\d/);
    }
  });

});
