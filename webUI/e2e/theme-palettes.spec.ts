import { expect, test } from '@playwright/test';
import { themeColors } from '../src/theme';

for (const dark of [false, true]) {
  test(`all named palettes apply and persist in ${dark ? 'dark' : 'light'} mode`, async ({ page }) => {
    await page.goto(`/?preview=settings-populated&theme=${dark ? 'dark' : 'light'}`);
    const choices = page.getByRole('group', { name: 'Color theme', exact: true });
    await expect(choices.getByRole('button')).toHaveCount(25);
    const colors = new Set<string>();
    for (const color of themeColors) {
      const button = choices.getByRole('button', { name: color.name, exact: true });
      await button.click();
      await expect(button).toHaveAttribute('aria-pressed', 'true');
      await expect(page.locator('html')).toHaveAttribute('data-theme-color', color.id);
      colors.add(await page.locator('html').evaluate(el => el.style.getPropertyValue('--md-sys-color-primary')));
    }
    expect(colors.size).toBeGreaterThan(20);
    await page.reload();
    await expect(choices.getByRole('button', { name: 'Birch', exact: true })).toHaveAttribute('aria-pressed', 'true');
    await choices.getByRole('button', { name: 'Roomio default', exact: true }).click();
    expect(await page.locator('html').evaluate(el => el.style.getPropertyValue('--md-sys-color-primary'))).toBe('');
  });
}

test('picker reflows for large text and RTL and supports keyboard selection', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto('/?preview=settings-populated&theme=dark&largeText=1');
  await page.locator('html').evaluate(el => { el.dir = 'rtl'; });
  const sage = page.getByRole('button', { name: 'Sage', exact: true });
  await sage.focus();
  await page.keyboard.press('Enter');
  await expect(sage).toHaveAttribute('aria-pressed', 'true');
  const swatch = await sage.boundingBox();
  expect(swatch!.height).toBeGreaterThanOrEqual(48);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: '/tmp/roomio-palettes-mobile-rtl.png', fullPage: true });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.screenshot({ path: '/tmp/roomio-palettes-desktop.png', fullPage: true });
});

test('storage failure keeps the palette applied with an actionable message', async ({ page }) => {
  await page.goto('/?preview=settings-populated');
  await page.evaluate(() => { Storage.prototype.setItem = () => { throw new Error('Storage unavailable'); }; });
  await page.getByRole('button', { name: 'Cocoa', exact: true }).click();
  await expect(page.locator('html')).toHaveAttribute('data-theme-color', 'cocoa');
  await expect(page.getByRole('alert')).toContainText('Select it again to retry');
});
