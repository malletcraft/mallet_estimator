// Port 62 slice L1: what the APPROVED PROTOTYPE'S OWN CODE answers -- the cross-check behind the hand-written rows of
// TradesTest, MarkLifeTest and PhaseToolsTest (android/pano/src/test/...). It loads four modules straight out of the built
// page (v61c.2, index61c2.html, which is in no repository: the published prototype is the source) into node, with a stub
// page, and prints their tables. Nothing here is read by the build or by a test; it is committed so the check can be run
// again by whoever ports the next slice (the scratchpad is a workbench, not storage).
//
//   node proto-tables.mjs <path to index61c2.html>            the tables: trades, life cycle, phases
//   node proto-tables.mjs <path to index61c2.html> <file>     ... then each line of <file> as the page shows it under मराठी
//
// The modules are found by their own opening lines, not by line number: L57 (the text layer), T53 (trades, types), L53
// (the life cycle and the Mark sheet) and P54 (the phase flow and its tool gating).
// WHAT THE PHONE DOES DIFFERENTLY, ON PURPOSE (DESIGN-port62.md §3, Q-list 6): at Client approved (stage 5) the prototype
// names no rule and falls through to Vendor review's moves; PhaseTools offers none there -- and so, since the L1 review
// (§3.1), PhaseTools.delOk is 0 at stage 5 for a mark past Existing, where the page prints 1 (its Delete is a Drop or a
// Restore, which the page still offers there). Every other row must agree.
import fs from 'node:fs';

const page = process.argv[2];
if (!page) { console.error('usage: node proto-tables.mjs <index61c2.html> [lines.txt]'); process.exit(2); }
const lines = fs.readFileSync(page, 'utf8').split('\n');
// a module: from the line that opens it to the first line after `from` that is exactly `})();`
function cut(open, from = 0) {
  const a = lines.findIndex((l, i) => i >= from && open(l, i));
  if (a < 0) throw new Error('module not found');
  const b = lines.findIndex((l, i) => i > a && l.trim() === '})();');
  return { text: lines.slice(a, b + 1).join('\n'), a: a + 1, b: b + 1 };
}
const mods = {
  L57: cut(l => l.startsWith('window.L57=(function(){')),
  T53: cut((l, i) => l.trim() === '(() => {' && /^const KINDS=\['wall','floor','ceil'\];/.test(lines[i + 1] || '')),
  L53: cut(l => l.startsWith('window.L53=(function(){')),
  P54: cut(l => l.startsWith('window.P54=(function(){')),
};

// ---- the stub page: enough for the modules to load; nothing is drawn
const store = { 'mcft-lang-v57': JSON.stringify({ lang: 'mr' }) };
globalThis.localStorage = { getItem: k => store[k] ?? null, setItem: (k, v) => { store[k] = String(v); }, removeItem: k => { delete store[k]; } };
class El {} El.prototype.setAttribute = function () {}; El.prototype.insertAdjacentHTML = function () {};
Object.defineProperty(El.prototype, 'innerHTML', { configurable: true, enumerable: true, get() { return ''; }, set() {} });
class Nd {} Object.defineProperty(Nd.prototype, 'textContent', { configurable: true, enumerable: true, get() { return ''; }, set() {} });
class HEl {} Object.defineProperty(HEl.prototype, 'innerText', { configurable: true, enumerable: true, get() { return ''; }, set() {} });
Object.assign(globalThis, { Element: El, Node: Nd, HTMLElement: HEl });
globalThis.document = { getElementById: () => null, addEventListener: () => {}, createElement: () => ({ getContext: () => ({}) }) };
globalThis.window = globalThis;
for (const k of ['L57', 'T53', 'L53', 'P54']) (0, eval)(mods[k].text);
const { L57, T53, L53, P54 } = globalThis;
console.log('modules', Object.entries(mods).map(([k, m]) => `${k} ${m.a}-${m.b}`).join(' · '));

// ---- T53: the trades
console.log('\nTRADES', JSON.stringify(T53.TRADES.map(t => [t.id, t.name, t.short, t.dot])));
console.log('ON', JSON.stringify(T53.ON));
console.log('OWN', JSON.stringify(T53.OWN));
console.log('SEES', JSON.stringify(T53.SEES));
console.log('SERVICE_TRADE', JSON.stringify(T53.SERVICE_TRADE));
console.log('SUB ids', JSON.stringify(Object.fromEntries(Object.entries(T53.SUBS).map(([t, v]) => [t, Object.fromEntries(Object.entries(v).map(([k, l]) => [k, l.map(x => x.id)]))]))));
const ALLT = [...T53.V52TOK, ...T53.TOKENS];
for (const k of T53.KINDS) {
  console.log('tradeOf', k, ALLT.map(t => t + '=' + [...new Set([T53.tradeOf(t, k), ...T53.subsOf(t, k).map(s => T53.tradeOf(t, k, s.id))])].join('/')).join(' '));
  console.log('moreRows', k, JSON.stringify(T53.moreRows(k, 'all').map(r => r.trade + ':' + r.types.join(' '))));
  console.log('moreRows', k, '(carpentry picked)', JSON.stringify(T53.moreRows(k, 'carpentry').map(r => (r.picked ? '*' : '') + r.trade + ':' + r.types.join(' '))));
  for (const t of T53.TRADES) console.log('  own | sees', k, t.id, '|', T53.ownOn(t.id, k).join(' '), '|', T53.seesOn(t.id, k).join(' '));
}

// ---- L53: the life cycle
const STAGES = [0, 1, 2, 3, 4, 5, null], lives = L53.LIFE_IDS, borns = ['exist', 'wish', 'vadd'];
const mk = (life, born, t = 'W') => { const e = { id: 'a', t, code: 'FW1', x0: 0, x1: 1, y0: 0, y1: 1 }; if (born !== 'exist') e.born = born; if (life !== 'exist') e.life = life; return e; };
console.log('\nLIFE', JSON.stringify(L53.LIFE)); console.log('STROKE', JSON.stringify(L53.STROKE)); console.log('ACTS', JSON.stringify(L53.ACTS));
console.log('MOVES'); for (const [f, L] of Object.entries(L53.MOVES)) for (const m of L) console.log('  ', `${f}>${m.to}`, m.as, m.why ? 'why' : '', m.back ? 'back' : '', m.bornOnly ? 'bornOnly=' + m.bornOnly : '', m.restore ? 'restore' : '', m.act ? 'act' : '', '|', m.label, '|', m.hint || '');
console.log('allowedMoves, born × state, at stages 0..5 and unread (? needs a why, ! told to the office):');
for (const b of borns) for (const l of lives) console.log('  ', b.padEnd(5), l.padEnd(5), STAGES.map(st => L53.allowedMoves(mk(l, b), st).map(m => m.to + (m.amber ? '!' : '') + (m.why ? '?' : '')).join(',') || '-').join(' | '));
console.log('resolved:', ['drop', 'cok'].flatMap(l => borns.map(b => `${b}/${l}: ` + L53.allowedMoves(mk(l, b), null).map(m => `${m.label}${m.hint ? ' (' + m.hint + ')' : ''}[${m.as}]>${m.to}`).join(' ; '))).join('\n          '));
L53.setNow(() => new Date('2026-11-11T05:00:00Z'));
{ const e = mk('exist', 'exist'); const step = (to, o) => { const r = L53.move(e, to, o); return `${JSON.stringify(r)} ${JSON.stringify(e)}`; };
  console.log('a journey:'); for (const [to, o] of [['wish', { stage: 1 }], ['drop', { stage: 1, why: '  not needed  ' }], ['wish', { stage: 1 }], ['vrev', { stage: 1 }], ['vrev', { stage: 2 }], ['vok', { stage: 2 }], ['cok', { stage: 2 }], ['scope', { stage: 4 }], ['cok', { stage: 4 }], ['wish', { stage: 2 }], ['vrev', { stage: 0 }]]) console.log('  ', to, JSON.stringify(o), '->', step(to, o)); }
console.log('delWord', [mk('exist', 'exist'), mk('wish', 'exist'), mk('drop', 'exist'), mk('scope', 'exist'), { t: 'NOTE', code: 'FNOTE1' }].map(e => L53.delWord(e)).join(' '));
console.log('clearText', [[3, 0, 0], [1, 2, 0], [3, 1, 2], [0, 0, 1]].map(a => L53.clearText(...a)).join(' | '), '|', L53.scopeKeptText(1), '|', L53.scopeKeptText(3), '|', L53.pastKeptText(1), '|', L53.unlockRefusal(mk('scope', 'exist')));
console.log('istISO', L53.istISO(new Date('2026-10-16T01:07:09.500Z')), '·', L53.sinceText('2026-10-16T06:37:09+05:30'));
console.log('countText', L53.countText(L53.countLife({ front: { els: [mk('exist', 'exist'), mk('wish', 'exist'), mk('wish', 'wish'), mk('scope', 'exist'), mk('drop', 'exist')] } })));

// ---- P54: the phases
const bits = f => STAGES.map(s => (f(s) ? 1 : 0)).join('');
console.log('\nPHASES', JSON.stringify(P54.PHASES.map(p => [p.n, p.name, p.quote, p.band])));
console.log('TOOLS (the table, then can() at stages 0..5 and unread -- a missing sixth column reads hidden):');
for (const [k, v] of Object.entries(P54.TOOLS)) console.log('  ', k.padEnd(12), v.join(''), bits(s => P54.can(k, s)));
console.log('AS_FIXED', JSON.stringify(P54.AS_FIXED), '· asFor at 0..5 and unread, nothing cycled:', STAGES.map(s => L53.asFor('c1', s)).join(','));
console.log('moveOk, each row at stages 0..5 and unread:'); for (const [f, L] of Object.entries(L53.MOVES)) for (const m of L) console.log('  ', `${f}>${m.to}`.padEnd(12), bits(s => P54.moveOk(f, m, s)));
const items = { 'a surveyed mark': mk('exist', 'exist'), 'a wish on a surveyed mark': mk('wish', 'exist'), 'a mark born a wish': mk('wish', 'wish'), 'a mark born Vendor added': mk('vadd', 'vadd'),
  'a Dropped surveyed mark': mk('drop', 'exist'), 'a Note': { id: 'n', t: 'NOTE', code: 'FNOTE1' }, 'a Line': { id: 'm', kind: 'line', code: 'F-M1' }, 'a Tape': { id: 't', kind: 'tape', code: 'F-T1' }, 'a chain': { id: 'k', code: 'FK1', nodes: [[0, 0], [1, 0]] } };
console.log('editable, delOk at stages 0..5 and unread:'); for (const [n, e] of Object.entries(items)) console.log('  ', n + ':', bits(s => P54.editable(e, s)), bits(s => P54.delOk(e, s)));
console.log('noMoveText:'); for (const s of STAGES) for (const L of ['exist', 'wish']) console.log('  ', s, L, '|', P54.noMoveText(L, s, L53.byId(L).name));

// ---- L57: what the page shows under मराठी for each line of the file given (a text it does not know comes back as it went in)
if (process.argv[3]) for (const s of fs.readFileSync(process.argv[3], 'utf8').split('\n').filter(Boolean)) console.log(JSON.stringify(s), '->', JSON.stringify(L57.vis(s)));
