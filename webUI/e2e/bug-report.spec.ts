import { test, expect, type Page } from '@playwright/test';

async function roomFixture(page: Page) {
  const sent: Record<string, unknown>[] = [];
  let fail = true;
  const room = { id:'12345678-1234-1234-1234-123456789012',code:'ABCD2345',title:'Friday night screening',ownerIdentityId:'owner' };
  await page.addInitScript(() => localStorage.setItem('roomio-auth-format','backend-v1'));
  await page.route('**/v1/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith('/refresh')) return route.fulfill({json:{identity:{id:'owner',name:'Mira',avatar:'comet',language:'en'},accessToken:'test',accessExpiresAt:new Date(Date.now()+3600000).toISOString(),refreshExpiresAt:new Date(Date.now()+86400000).toISOString()}});
    if (path.endsWith('/preview')) return route.fulfill({json:{code:room.code,title:room.title,ownerName:'Mira',ownerPresent:true,participantCount:1,availability:'OPEN',streamState:'EMPTY'}});
    if (path.endsWith('/join')) return route.fulfill({json:{room,membership:{id:'member',identityId:'owner',isOwner:true}}});
    if (path.endsWith('/snapshot')) return route.fulfill({json:{room,members:[{membershipId:'member',identityId:'owner',displayName:'Mira',avatar:'comet',isOwner:true,present:true}],stream:{sessionId:'',revision:0,state:'EMPTY',url:''},sequence:1}});
    if (path.endsWith('/bug-reports')) {
      sent.push(route.request().postDataJSON());
      return route.fulfill(fail ? {status:503,json:{code:'UNAVAILABLE'}} : {status:201,json:{reportId:sent.at(-1)!.reportId}});
    }
    return route.fulfill({status:503,json:{code:'UNAVAILABLE'}});
  });
  await page.goto('/?invite='+room.code);
  await page.getByRole('button',{name:'Join party',exact:true}).click();
  await expect(page.getByRole('button',{name:'Report a bug',exact:true})).toBeVisible();
  return {sent, succeed: () => {fail=false;}};
}
for (const variant of [
  {name:'desktop light',width:1280,height:800,dark:false,rtl:false},
  {name:'mobile dark large RTL',width:360,height:800,dark:true,rtl:true},
]) test(`room bug report retains draft, retries original snapshot and sends diagnostics: ${variant.name}`, async ({page,context},info) => {
  await page.setViewportSize(variant);
  const fixture = await roomFixture(page);
  await page.evaluate(({dark,rtl}) => {document.documentElement.dir=rtl?'rtl':'ltr';document.documentElement.style.fontSize=rtl?'22px':'16px';if(dark)document.documentElement.dataset.theme='dark';console.warn('earlier failure accessToken=do-not-send https://secret.example/movie?token=hidden');},variant);
  const bug = page.getByRole('button',{name:'Report a bug',exact:true});
  await bug.click();
  const dialog=page.getByRole('dialog');
  await expect(dialog.getByRole('button',{name:'Send',exact:true})).toBeDisabled();
  await dialog.getByRole('textbox',{name:'What went wrong?'}).fill('Voice becomes quiet when I play the movie.');
  await page.keyboard.press('Escape');
  await bug.click();
  await expect(dialog.getByRole('textbox')).toHaveValue('Voice becomes quiet when I play the movie.');
  await context.setOffline(true);
  await expect(dialog.getByRole('button',{name:'Send',exact:true})).toBeDisabled();
  await context.setOffline(false);
  await dialog.getByRole('button',{name:'Send',exact:true}).click();
  await expect(dialog).toContainText('Report wasn’t sent');
  const payload=fixture.sent[0];
  expect(payload.description).toBe('Voice becomes quiet when I play the movie.');
  expect(payload.clientInfo).toMatchObject({platform:'browser'});
  expect(String(payload.log)).toContain('Application started');
  expect(String(payload.log)).toContain('earlier failure');
  for (const event of ['auth.start', 'auth.response', 'http.start', 'http.complete', 'room.connect', 'report.send']) expect(String(payload.log)).toContain(event);
  expect(String(payload.log)).not.toContain('ABCD2345');
  expect(String(payload.log)).not.toMatch(/do-not-send|secret.example|hidden/);
  expect(Date.parse(String(payload.sessionStartedAt))).toBeGreaterThan(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({path:info.outputPath('bug-dialog.png'),fullPage:true});
  const sendBounds = await dialog.getByRole('button',{name:'Send',exact:true}).boundingBox();
  expect(sendBounds!.y + sendBounds!.height).toBeLessThanOrEqual(800);
  fixture.succeed();
  await dialog.getByRole('button',{name:'Send',exact:true}).click();
  await expect(dialog).toHaveCount(0);
  expect(fixture.sent[1]).toEqual(payload);
  await bug.click();
  await expect(page.getByRole('dialog').getByRole('textbox')).toHaveValue('');
});
