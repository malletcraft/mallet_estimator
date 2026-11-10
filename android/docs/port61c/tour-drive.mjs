// §31 build slice: drive the seven tours under EN and मराठी and export EVERY step's
// placement rects (T55.state().rect: frame/target/hole/bubble) plus side/trim/clip/
// overlap/found — the fixtures TourPlaceTest replays. The first drive (drive.mjs)
// recorded the verdicts but not the rects; this run exports them so they can be
// COMMITTED (the scratchpad is a workbench, not storage).
import { chromium } from '/opt/node-tools/node_modules/playwright/index.mjs';
import fs from 'node:fs';
const prepDoc = JSON.parse(fs.readFileSync('db/prep/guest-bedroom.json', 'utf8'));
delete prepDoc.__name__; delete prepDoc.version_;
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1560, height: 1180 } });
page.on('pageerror', e => console.log('PAGEERR', String(e).slice(0, 200)));
await page.addInitScript(doc => {
  const store = { 'prep/guest-bedroom': doc };
  window.claude = {
    use: async name => {
      if (name !== 'db') throw new Error('not available in this view');
      return {
        doc: id => ({
          get: async () => { const d = store[id]; return { exists: !!d, data: () => d }; },
          set: async v => { store[id] = v; }
        }),
        collection: () => { throw new Error('no collections in this stub'); }
      };
    }
  };
}, prepDoc);
await page.goto('http://127.0.0.1:8791/index61c2.html', { waitUntil: 'load' });
await page.waitForTimeout(1500);
try { await page.click('.launcher button', { timeout: 1500 }); await page.waitForTimeout(600); } catch {}

async function settle() {
  await page.waitForFunction(() => {
    if (!window.T55.running()) return true;
    const s = window.T55.state();
    return !!s && !s.busy;
  }, null, { timeout: 20000 });
  await page.waitForTimeout(350);
}
async function runFlows(lang) {
  const out = [];
  const flows = await page.evaluate(() => window.T55.flows().map(f => ({ id: f.id, n: f.steps.length })));
  for (const f of flows) {
    await page.evaluate(id => window.T55.start(id), f.id);
    let guard = 0, lastI = -1;
    while (guard++ < f.n * 2 + 4) {
      await settle();
      if (!(await page.evaluate(() => window.T55.running()))) break;
      const st = await page.evaluate(() => {
        const s = window.T55.state(); if (!s) return null;
        return { i: s.i, id: s.step.id, anchor: s.step.anchor, found: s.found, side: s.side,
                 overlap: s.overlap, clip: s.clip, trim: s.trim, rect: s.rect };
      });
      if (!st) break;
      if (st.i !== lastI) { out.push({ lang, flow: f.id, ...st }); lastI = st.i; }
      await page.evaluate(() => window.T55.next());
    }
    const ended = await page.waitForFunction(() => !window.T55.running(), null, { timeout: 25000 }).then(() => true).catch(() => false);
    if (!ended) { console.log(`  (${f.id}: did not end; skipping)`); await page.evaluate(() => window.T55.skip()); await page.waitForTimeout(400); }
  }
  return out;
}
const en = await runFlows('en');
await page.evaluate(() => { const S = window.__app.S; S.help = false; S.drawer = true; window.__app.render(); });
await page.waitForTimeout(300);
await page.click('[data-l57="lang"]', { timeout: 5000 });
await page.waitForTimeout(600);
const mr = await runFlows('mr');
const all = en.concat(mr);
fs.writeFileSync('out/rects.json', JSON.stringify(all, null, 1));
let bad = 0;
for (const s of all) if (!s.found || s.overlap || s.clip > 0 || !s.rect || !s.rect.frame || !s.rect.bubble) { bad++; console.log('ODD', s.lang, s.flow, s.id, s.found, s.overlap, s.clip); }
console.log(`steps: ${all.length} (en ${en.length}, mr ${mr.length}), odd: ${bad}, trims: ${all.filter(s => s.trim).length}, holes: ${all.filter(s => s.rect && s.rect.hole).length}`);
console.log('DONE');
