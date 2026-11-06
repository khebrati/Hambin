import { expect, test, type Page } from '@playwright/test';
import { readFile } from 'node:fs/promises';

const url = 'https://media.example.test/integrated.mkv';
const source = readFile(new URL('./fixtures/player-mkv/movie.mkv', import.meta.url));
async function mediaSource(page: Page) {
  const bytes = await source;
  const requests: string[] = [];
  await page.route(url, async route => {
    const request = route.request();
    const range = request.headers().range;
    requests.push(`${request.method()} ${range ?? ''}`);
    const headers = { 'access-control-allow-origin': '*', 'accept-ranges': 'bytes' };
    if (request.method() === 'HEAD') return route.fulfill({ status: 200, headers: { ...headers, 'content-length': String(bytes.length) } });
    const match = /bytes=(\d+)-(\d*)/.exec(range ?? '');
    const start = Number(match?.[1] ?? 0);
    const end = Math.min(Number(match?.[2] || bytes.length - 1), bytes.length - 1);
    if (start >= bytes.length) return route.fulfill({ status: 416, headers: { ...headers, 'content-range': `bytes */${bytes.length}` } });
    const body = bytes.subarray(start, end + 1);
    await route.fulfill({ status: match ? 206 : 200, headers: { ...headers, 'content-length': String(body.length), ...(match ? { 'content-range': `bytes ${start}-${end}/${bytes.length}` } : {}) }, contentType: 'video/x-matroska', body });
  });
  return requests;
}
async function openPlayer(page: Page, settings = '') {
  await page.goto(`/e2e/player.html?url=${encodeURIComponent(url)}${settings}`);
  await expect.poll(() => page.locator('.integrated-player video').evaluateAll(videos => videos[0] ? (videos[0] as HTMLVideoElement).getVideoPlaybackQuality().totalVideoFrames : 0)).toBeGreaterThan(5);
}
async function seek(page: Page, value: number) {
  await page.getByRole('slider', { name: 'Playback position', exact: true }).evaluate((element, value) => {
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(element, String(value));
    element.dispatchEvent(new Event('input', { bubbles: true }));
  }, value);
}

for (const settings of [
  { name: 'desktop light', query: '&theme=light', viewport: { width: 1280, height: 900 } },
  { name: 'mobile dark large RTL', query: '&theme=dark&largeText=1&dir=rtl', viewport: { width: 390, height: 844 } },
]) test(`${settings.name}: real MKV decodes with embedded tracks, local controls and subtitle imports`, async ({ page }) => {
  await page.setViewportSize(settings.viewport);
  const requests = await mediaSource(page);
  await openPlayer(page, settings.query);
  const video = page.locator('.integrated-player video');
  await expect(video).toHaveAttribute('src', /^blob:/);
  await page.getByRole('button', { name: 'Subtitles', exact: true }).click();
  const subtitles = page.getByRole('dialog', { name: 'Subtitles', exact: true });
  await expect(subtitles.getByRole('radio', { name: 'Off', exact: true })).toBeChecked();
  await expect(page.locator('.ASS-box')).not.toBeVisible();
  await subtitles.getByRole('radio', { name: 'Persian · fas', exact: true }).check();
  await expect(page.locator('.ASS-box')).toContainText('سلام دنیا');
  await expect(page.locator('.ASS-box')).not.toContainText('<font');
  await page.getByRole('button', { name: 'Pause', exact: true }).click();
  await expect.poll(() => video.evaluate(v => v.paused)).toBe(true);
  await seek(page, 21);
  await expect.poll(() => video.evaluate(v => v.currentTime)).toBeGreaterThan(20);
  await expect.poll(() => video.evaluate(v => v.paused)).toBe(true);
  await page.getByRole('button', { name: 'Play', exact: true }).click();
  await expect(page.locator('.ASS-box')).toContainText('زیرنویس دوم');
  await page.getByRole('button', { name: 'Pause', exact: true }).click();
  await page.getByRole('button', { name: 'Audio tracks', exact: true }).click();
  const audio = page.getByRole('dialog', { name: 'Audio tracks', exact: true });
  await audio.getByRole('radio', { name: 'English · eng', exact: true }).check();
  await page.getByRole('button', { name: 'Audio tracks', exact: true }).click();
  await audio.getByRole('radio', { name: 'Automatic', exact: true }).check();
  await expect.poll(() => video.evaluate(v => v.paused)).toBe(true);
  await page.getByRole('button', { name: 'Mute video', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Unmute video', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Unmute video', exact: true }).click();
  await page.getByRole('button', { name: 'Enter fullscreen', exact: true }).click();
  await page.getByRole('button', { name: 'Subtitles', exact: true }).click();
  await expect(subtitles).toBeInViewport();
  await expect(subtitles.getByRole('radio', { name: 'Persian · fas', exact: true })).toBeChecked();
  await page.locator('input[type=file]').setInputFiles({ name: 'my.srt', mimeType: 'application/x-subrip', buffer: Buffer.from('1\n00:00:20,000 --> 00:00:31,000\nLocal caption\n') });
  await expect(subtitles).not.toBeVisible();
  await expect.poll(() => video.evaluate(v => Array.from(v.textTracks).flatMap(t => Array.from(t.activeCues ?? []).map(c => (c as VTTCue).text)))).toContain('Local caption');
  await page.getByRole('button', { name: 'Subtitles', exact: true }).click();
  await subtitles.getByRole('radio', { name: 'Off', exact: true }).check();
  await expect(page.locator('.ASS-box')).not.toBeVisible();
  expect(await page.evaluate(() => (window as any).playerActions)).toEqual([]);
  expect(requests.every(request => request.startsWith('HEAD') || request.includes('bytes='))).toBe(true);
  await page.screenshot({ path: `test-results/avplayer-${settings.name.replaceAll(' ', '-')}.png`, fullPage: true });
});

test('failed player asset download offers video retry and recovers', async ({ page }) => {
  await mediaSource(page);
  await page.route('**/vendor/avplayer/**/avplayer.js', route => route.abort());
  await page.goto(`/e2e/player.html?url=${encodeURIComponent(url)}`);
  await expect(page.getByRole('alert')).toBeVisible();
  await page.unroute('**/vendor/avplayer/**/avplayer.js');
  await page.getByRole('button', { name: 'Retry video', exact: true }).click();
  await expect.poll(() => page.locator('.integrated-player video').evaluateAll(videos => videos[0] ? (videos[0] as HTMLVideoElement).getVideoPlaybackQuality().totalVideoFrames : 0)).toBeGreaterThan(5);
  await expect(page.getByRole('alert')).toHaveCount(0);
});

test('browser fallback preserves playback when the enhanced runtime cannot load', async ({ page }) => {
  await mediaSource(page);
  await page.route('**/vendor/avplayer/**/avplayer.js', route => route.abort());
  await page.goto(`/e2e/player.html?url=${encodeURIComponent(url)}`);
  await page.getByRole('button', { name: 'Use browser player', exact: true }).click();
  const video = page.locator('video:not([hidden])');
  await expect(video).toHaveAttribute('src', url);
  await expect.poll(() => video.evaluate(v => v.getVideoPlaybackQuality().totalVideoFrames)).toBeGreaterThan(5);
  await expect(page.getByRole('alert')).toHaveCount(0);
});

test('source replacement disposes the old engine and local subtitle state', async ({ page }) => {
  await mediaSource(page);
  await openPlayer(page);
  await page.getByRole('button', { name: 'Subtitles', exact: true }).click();
  await page.getByRole('dialog', { name: 'Subtitles', exact: true }).getByRole('radio', { name: 'Persian · fas', exact: true }).check();
  await page.route('https://media.example.test/replaced.mp4', () => {});
  await page.evaluate(() => (window as any).replacePlayerSource('https://media.example.test/replaced.mp4'));
  await expect(page.locator('.integrated-player video')).toHaveCount(0);
  await expect.poll(() => page.evaluate(() => (window as any).AVPlayer.Instances.length)).toBe(0);
  await expect(page.locator('video')).toHaveAttribute('src', 'https://media.example.test/replaced.mp4');
  await expect(page.getByRole('button', { name: 'Subtitles', exact: true })).toHaveAttribute('aria-pressed', 'false');
});

test('explicit synchronization only advances the integrated player and reports actual playback', async ({ page }) => {
  await mediaSource(page);
  await openPlayer(page);
  await page.getByRole('button', { name: 'Pause', exact: true }).click();
  const video = page.locator('.integrated-player video');
  await page.evaluate(() => (window as any).syncPlayerTo(18000));
  await expect.poll(() => video.evaluate(v => v.currentTime)).toBeGreaterThan(17.9);
  await expect.poll(() => page.evaluate(() => (window as any).playerReports.some((report: any) => report.positionMs >= 17900 && !report.playing))).toBe(true);
  // Nineteen seconds lies between this fixture's two-second keyframes.
  await page.evaluate(() => (window as any).syncPlayerTo(19000));
  await expect.poll(() => page.evaluate(() => (window as any).playerReports.some((report: any) => report.positionMs >= 18900 && !report.playing))).toBe(true);
  await page.evaluate(() => (window as any).syncPlayerTo(2000));
  await page.waitForTimeout(300);
  expect(await video.evaluate(v => v.currentTime)).toBeGreaterThan(18.9);
  expect(await page.evaluate(() => (window as any).playerActions)).toEqual([]);
});

test('autoplay denial stays paused in reports and Play recovers', async ({ page }) => {
  await mediaSource(page);
  const errors: string[] = [];
  page.on('pageerror', error => errors.push(String(error)));
  await page.addInitScript(() => {
    // Deny both entry points: AVPlayer requests play and sets autoplay=true.
    const autoplay = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'autoplay')!;
    Object.defineProperty(HTMLMediaElement.prototype, 'autoplay', { ...autoplay, set() { autoplay.set!.call(this, false); } });
    const play = HTMLMediaElement.prototype.play;
    HTMLMediaElement.prototype.play = function () {
      if (!(window as any).allowPlayback) return Promise.reject(new DOMException('User gesture required', 'NotAllowedError'));
      return play.call(this);
    };
  });
  await page.goto(`/e2e/player.html?url=${encodeURIComponent(url)}`);
  const blocked = page.getByText('Select Play to start the video on this device.');
  await expect(blocked).toBeVisible();
  expect(await page.evaluate(() => (window as any).playerReports.every((report: any) => !report.playing))).toBe(true);
  await page.evaluate(() => { (window as any).allowPlayback = true; });
  await page.getByRole('button', { name: 'Play', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Pause', exact: true })).toBeVisible();
  await expect(blocked).toHaveCount(0);
  await expect.poll(() => page.locator('.integrated-player video').evaluate(v => v.muted)).toBe(false);
  expect(errors).toEqual([]);
});
