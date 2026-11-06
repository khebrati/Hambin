import { cp, mkdir, readdir, readFile, writeFile, rm } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const root = fileURLToPath(new URL('../', import.meta.url));
const destination = resolve(root, 'public/vendor/avplayer/1.3.1-hambin.1');
await mkdir(destination, { recursive: true });
// Remove obsolete generated versions without removing the active directory:
// Vite may be serving its files while a separate production build runs.
for (const entry of await readdir(resolve(root, 'public/vendor/avplayer'))) {
  if (entry !== '1.3.1-hambin.1') await rm(resolve(root, 'public/vendor/avplayer', entry), { recursive: true, force: true });
}
const source = resolve(root, 'node_modules/@libmedia/avplayer/dist/umd');
for (const entry of await readdir(source)) {
  if (/^(?:avplayer|\d+\.avplayer)\.js$/.test(entry)) await cp(resolve(source, entry), resolve(destination, entry));
}
// Version-pinned equivalent of vendor-patches/avplayer-seek.patch. Fail closed
// when the upstream method changes, rather than silently shipping an unpatched build.
const filename = resolve(destination, 'avplayer.js');
const original = await readFile(filename, 'utf8');
const start = original.indexOf('async doSeek(');
const end = original.indexOf('async seek(', start);
const method = original.slice(start, end);
const head = 'async doSeek(e,t){';
const reset = 'this.subtitleRender&&(this.subtitleRender.reset(),this.subtitleRender.start())';
if (start < 0 || end < start || method.split(head).length !== 2 || method.split(reset).length !== 2) throw new Error('AVPlayer seek patch no longer matches the pinned upstream build');
const patched = method.replace(head, `${head}this.subtitleRender&&this.subtitleRender.reset();`).replace(reset, 'this.subtitleRender&&this.subtitleRender.start()');
await writeFile(filename, original.slice(0, start) + patched + original.slice(end));
await cp(resolve(root, 'node_modules/@libmedia/cheap/dist/cheap-polyfill.js'), resolve(destination, 'cheap-polyfill.js'));
await cp(resolve(root, 'vendor-notices/avplayer.txt'), resolve(destination, 'NOTICE.txt'));
await cp(resolve(root, 'vendor-notices/COPYING.LGPLv3'), resolve(destination, 'COPYING.LGPLv3'));
await cp(resolve(root, 'vendor-notices/COPYING.GPLv3'), resolve(destination, 'COPYING.GPLv3'));
await cp(resolve(root, 'vendor-patches/avplayer-seek.patch'), resolve(destination, 'avplayer-seek.patch'));

await cp(resolve(root, 'node_modules/assjs/LICENSE'), resolve(destination, 'ASSJS-LICENSE'));
await cp(resolve(root, 'node_modules/ass-compiler/LICENSE'), resolve(destination, 'ASS-COMPILER-LICENSE'));
