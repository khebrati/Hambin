import { test, expect } from '@playwright/test';
for (const variant of [{ theme: 'light', dir: 'ltr', width: 1280, height: 900, large: '' }, { theme: 'dark', dir: 'rtl', width: 390, height: 844, large: '1' }]) {
  test(`degraded voice recovery ${variant.theme} ${variant.dir} ${variant.width}`, async ({ page }, testInfo) => {
    await page.setViewportSize(variant);
    await page.goto(`/e2e/voice.html?${new URLSearchParams({ theme: variant.theme, dir: variant.dir, large: variant.large })}`);
    await expect(page.getByRole('heading', { name: 'Some voice links need attention' })).toBeVisible();
    await expect(page.getByRole('list', { name: 'Voice connections' })).toContainText('Reconnecting…');
    await expect(page.getByRole('list', { name: 'Voice connections' })).toContainText('Connection unavailable');
    const retry = page.getByRole('button', { name: 'Retry voice connections' });
    await retry.focus(); await page.keyboard.press('Enter');
    await expect(page.locator('html')).toHaveAttribute('data-retried', 'true');
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    const screenshot = testInfo.outputPath('voice-state.png');
    await page.screenshot({ fullPage: true, path: screenshot });
    await testInfo.attach('voice-state.png', { path: screenshot, contentType: 'image/png' });
  });
}
