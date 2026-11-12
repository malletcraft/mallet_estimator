// Port 62 slice L6, its review (12 Nov 2026; DESIGN-port62.md §3.6, "The L6 review"): WHAT SHUTS THE TOOL CHIP'S LIST OF
// KINDS on the approved prototype, DRIVEN -- every press is a DOM click on the page's own control, so it runs the page's
// own listeners in their own order. Read alone, the page's code misleads: survey prep's click listener shuts the list on
// any click outside the list and its button (index61c2.html 6811), but a palette slot and the chip's cycling "as ..."
// button each keep their click to themselves (6618 `ev.stopPropagation()`; 3091, the Mark-life listener, in capture), so
// that line never sees them. The reviewer read 6811 and reported that More > and the "as ..." words shut the list on the
// page; driven, both leave it OPEN. Nothing here is read by the build or by a test; it is committed so the check can be
// run again (the scratchpad is a workbench, not storage).
//
//   1. serve a COPY of the built page (v61c.2, index61c2.html -- in no repository: the published prototype is the source)
//      on a free local port:   python3 -m http.server <port> --bind 127.0.0.1
//   2. node kind-list-drive.mjs http://127.0.0.1:<port>/index61c2.html [<playwright's index.mjs>]
//
// Survey prep is opened by the page's own guided tour ("existing" opens 2 · Mark on the demo room's Front wall at Survey;
// "wish" moves the demo project to the Wish list and arms a type at its step 7) and driven under it. The demo room is
// the page's own; no artifact database is needed (the stub below answers "no saved prep"). Two things are done BY HAND
// to reach a state the demo does not start in, and each is said on its line of the output: undo is enabled (it is
// disabled with nothing to undo), and P54.asFixed is made to answer "not fixed" so the page draws its own cycling
// "as ..." button, which it draws only while the quote stage is unread.
const url = process.argv[2];
if (!url) { console.error('usage: node kind-list-drive.mjs <url of a served copy of index61c2.html> [<playwright index.mjs>]'); process.exit(2); }
const { chromium } = await import(process.argv[3] || '/opt/node-tools/node_modules/playwright/index.mjs');

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1560, height: 1180 } });
page.on('pageerror', e => console.log('PAGEERR', String(e).slice(0, 200)));
await page.addInitScript(() => { const store = {};
  window.claude = { use: async name => { if (name !== 'db') throw new Error('not available in this view');
    return { doc: id => ({ get: async () => ({ exists: !!store[id], data: () => store[id] }), set: async v => { store[id] = v; } }), collection: () => { throw new Error('no collections in this stub'); } }; } }; });
await page.goto(url, { waitUntil: 'load' });
await page.waitForTimeout(1500);
try { await page.click('.launcher button', { timeout: 1500 }); await page.waitForTimeout(600); } catch {}

const settled = () => page.waitForFunction(() => { if (!window.T55.running()) return true; const s = window.T55.state(); return !!s && !s.busy; }, null, { timeout: 20000 });
const q = () => page.evaluate(() => { const sp = document.querySelector('#t53subs'), tc = document.querySelector('#toolchip'), ms = document.querySelector('#moresheet'), a = tc.querySelector('.l53as');
  const lit = [...document.querySelectorAll('#trow .tb.on')].map(b => b.dataset.tool || b.dataset.t).join('+');
  return (sp && !sp.hidden ? 'list OPEN' : 'list shut') + ' | chip ' + (tc.hidden ? '-' : JSON.stringify(tc.querySelector('span').textContent.replace(/\s+/g, ' ').slice(0, 64))) +
    ' | More ' + (ms.hidden ? 'shut' : 'OPEN') + ' | lit ' + lit + (a ? ' | as: ' + (a.tagName === 'BUTTON' ? 'a button that cycles' : 'plain words') : ''); });
const click = async sel => { const ok = await page.evaluate(s => { const e = [...document.querySelectorAll(s)].find(x => x.offsetParent !== null) || document.querySelector(s); if (!e) return false; e.click(); return true; }, sel); await page.waitForTimeout(150); return ok; };
const arm = async () => { await click('#trow [data-tool="more"]'); await click('#moresheet [data-mt="CRP"]'); };   // More > Carpentry unit
const openList = async () => { if (!(await q()).includes('▾ ·')) await arm(); if (!(await q()).startsWith('list OPEN')) await click('[data-t53sub]');
  const t = await q(); if (!t.startsWith('list OPEN')) throw new Error('could not open the list: ' + t); };
// each row starts with Carpentry unit armed on the open face and its list of kinds OPEN
const row = async (name, steps) => { await openList(); const out = [];
  for (const [what, sel] of steps) { const ok = sel === 'ARM' ? (await arm(), true) : await click(sel); out.push(`${what}${ok ? '' : ' (NOT FOUND)'} -> ${await q()}`); }
  console.log(`${name}\n    ` + out.join('\n    ')); };

await page.evaluate(() => window.T55.start('existing')); await settled(); await page.waitForTimeout(400);
console.log('SURVEY, 2 · Mark, Front wall\nat start: ' + await q());
await arm(); console.log('More > Carpentry unit: ' + await q());
await click('[data-t53sub]'); console.log('the kind button: ' + await q());
await click('[data-t53sub]'); console.log('the kind button again: ' + await q());
await row('A. a kind picked', [['Kitchen base', '#t53subs [data-t53subv="kbase"]']]);
await row('B. X on the chip, then the same type again', [['X', '#toolchip [data-cancel]'], ['More > Carpentry unit', 'ARM']]);
await row('C. More >, and More > again', [['More >', '#trow [data-tool="more"]'], ['More > again', '#trow [data-tool="more"]']]);
await row('D. More >, then the sheet\'s own X', [['More >', '#trow [data-tool="more"]'], ['the sheet\'s X', '#moresheet [data-moreclose]']]);
await row('E. fit', [['fit', '.spv .fit:not(.gridb):not(#planfit)']]);
await row('F. grid, twice', [['grid', '.spv [data-grid]'], ['grid again', '.spv [data-grid]']]);
await row('G. the open face\'s own tab', [['Front', '#tabs [data-tab="front"]']]);
await row('H. another face and back, then the same type again', [['Left', '#tabs [data-tab="left"]'], ['Front', '#tabs [data-tab="front"]'], ['More > Carpentry unit', 'ARM']]);
await row('I. another palette slot, then the same type again', [['Window', '#trow [data-t="W"]'], ['More > Carpentry unit', 'ARM']]);
await row('J. the open step on the strip', [['2 · Mark', '#mode [data-mode="mark"]']]);
await row('K. the top bar\'s menu, twice', [['menu', '#spMore'], ['menu again', '#spMore']]);
await row('L. the chip\'s plain words', [['the words', '#toolchip span']]);
// (enabled after the list is open: every render of the page disables it again while there is nothing to undo)
await openList(); await page.evaluate(() => { document.querySelector('#undo').disabled = false; }); await click('#undo');
console.log('M. undo (ENABLED BY HAND: there is nothing to undo)\n    undo -> ' + await q());

await page.evaluate(() => window.T55.skip()); await page.waitForTimeout(600);
await page.evaluate(() => window.T55.start('wish'));
for (let k = 0; k < 40; k++) { await settled(); await page.waitForTimeout(300);
  const id = await page.evaluate(() => { const s = window.T55.state(); return s && s.step.id; }); if (!id || id === 'wish.7') break;
  await page.evaluate(() => window.T55.next()); await page.waitForTimeout(200); }
console.log('WISH LIST, 2 · Mark (the wish tour, step ' + await page.evaluate(() => { const s = window.T55.state(); return s && s.step.id; }) + ')');
await row('N. the chip\'s "as ..." half where the phase fixes it', [['as Client wish', '#toolchip .l53as']]);
await page.evaluate(() => { window.P54.asFixed = () => null; window.__sp.render(); }); await page.waitForTimeout(150);
await row('O. the cycling "as ..." button (P54.asFixed STUBBED to "not fixed", as while the stage is unread)', [['as ...', '#toolchip .l53as'], ['as ... again', '#toolchip .l53as']]);
await browser.close();
