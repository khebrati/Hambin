import { expect, test } from '@playwright/test';

test('Persian profile choice sends fa, persists after reload, and switches back to English', async ({ page }) => {
  let language = 'en';
  await page.route('**/v1/guest-sessions/**', async route => {
    if (route.request().method() === 'PATCH') {
      const body = route.request().postDataJSON();
      expect(body.name).toBe('Ice Queen');
      language = body.language;
      await route.fulfill({ json: { identity: { id: 'guest', name: 'Ice Queen', avatar: 'comet', language } } });
    } else await route.fulfill({ json: { identity: { id: 'guest', name: 'Ice Queen', avatar: 'comet', language }, accessToken: 'test', accessExpiresAt: new Date(Date.now() + 3600000).toISOString(), refreshExpiresAt: new Date(Date.now() + 86400000).toISOString() } });
  });
  await page.goto('/');
  await page.getByRole('button', { name: 'Settings', exact: true }).click();
  await page.getByLabel('App language', { exact: true }).selectOption('فارسی');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'با هم ببینید' })).toBeVisible();
  expect(language).toBe('fa');
  await expect(page).toHaveTitle('هم‌بین');
  await expect(page.locator('html')).toHaveAttribute('lang', 'fa');
  await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
  await page.reload();
  await expect(page.getByRole('button', { name: 'ساخت دورهمی', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'تنظیمات', exact: true }).click();
  await expect(page.getByLabel('زبان برنامه', { exact: true })).toHaveValue('فارسی');
  await page.getByLabel('زبان برنامه', { exact: true }).selectOption('English');
  await page.getByRole('button', { name: 'ذخیره نمایه', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Stream and chill' })).toBeVisible();
  expect(language).toBe('en');
  await expect(page).toHaveTitle('Watch Party');
  await expect(page.locator('html')).toHaveAttribute('dir', 'ltr');
});

for (const config of [{ width: 360, height: 800, theme: 'dark', large: true }, { width: 1440, height: 1000, theme: 'light', large: false }]) {
  test(`Persian screens fit at ${config.width}px in ${config.theme}`, async ({ page }) => {
    await page.setViewportSize(config);
    for (const preview of ['home', 'settings-populated', 'party-manager', 'room-preview-owner-absent', 'owner-empty']) {
      await page.goto(`/?preview=${preview}&lang=fa&theme=${config.theme}&largeText=${config.large ? 1 : 0}`);
      await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
      // Use the preview list below only; a typo must fail instead of reaching the live backend.
      await expect(page.locator('.screen')).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      if (preview === 'settings-populated') {
        await page.getByRole('combobox').scrollIntoViewIfNeeded();
        await expect(page.getByRole('option', { name: 'فارسی', exact: true })).toBeAttached();
      }
      await page.screenshot({ path: `/tmp/hambin-persian-${config.width}-${preview}.png`, fullPage: true });
    }
  });
}

test('discard and failed saves keep the applied language and retain the draft for retry', async ({ page }) => {
  await page.route('**/v1/guest-sessions/**', route => route.fulfill(route.request().method() === 'PATCH'
    ? { status: 503, json: { code: 'UNAVAILABLE' } }
    : { json: { identity: { id: 'guest', name: 'Ice Queen', avatar: 'comet', language: 'en' }, accessToken: 'test', accessExpiresAt: new Date(Date.now() + 3600000).toISOString(), refreshExpiresAt: new Date(Date.now() + 86400000).toISOString() } }));
  await page.goto('/');
  await page.getByRole('button', { name: 'Settings', exact: true }).click();
  await page.getByLabel('App language', { exact: true }).selectOption('فارسی');
  await page.getByRole('button', { name: 'Back', exact: true }).click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  await page.getByRole('button', { name: 'Settings', exact: true }).click();
  await expect(page.getByLabel('App language', { exact: true })).toHaveValue('English');
  await page.getByLabel('App language', { exact: true }).selectOption('فارسی');
  await page.getByRole('button', { name: 'Save profile', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('The service is unavailable. Try again.');
  await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  await expect(page.getByLabel('App language', { exact: true })).toHaveValue('فارسی');
});
