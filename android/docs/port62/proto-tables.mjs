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
// SLICE L2 (11 Nov 2026) adds one section, "L2 …": what the page DRAWS of a mark's state -- its outline, a spot's ring and
// the tag after its code -- on a recording canvas, in English and under मराठी; the bar on the picked label; what a clear
// keeps and what stays locked. It is the cross-check behind MarkLifeTest's width and tag tables.
// WHAT THE PHONE DOES DIFFERENTLY, ON PURPOSE (DESIGN-port62.md §3, Q-list 6): at Client approved (stage 5) the prototype
// names no rule and falls through to Vendor review's moves; PhaseTools offers none there -- and so, since the L1 review
// (§3.1), PhaseTools.delOk is 0 at stage 5 for a mark past Existing, where the page prints 1 (its Delete is a Drop or a
// Restore, which the page still offers there). Every other row must agree.
// THE L2 REVIEW (11 Nov 2026; DESIGN-port62.md §3.2): given the line `Drop`, the page prints `"Drop" -> "ड्रॉप"` -- its text
// layer finds a text by its English word, and that word is also the Ceiling drop type's short name. The phone's delete
// control says वगळा (life.drop.verb), the verb of वगळले. And the page this was last run on is mock-up v62a (v61c.2 plus the
// prototype items P0 and P2, 9,657 lines): the four modules are cut by their opening lines, so they are found wherever they
// sit, and they print what the 9,563-line v61c.2 printed -- only the first line, their line numbers, differs (+26).
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

// ---- SLICE L2: what the page DRAWS of a mark's state -- outline (2872), ring (2887), paintState (2898-2911), nlAttrs (2930) --
// on a recording canvas: the cross-check behind MarkLifeTest's width and tag tables (MarkLife.outlineWidth, ringWidth, tag,
// barColour) and behind the app's SurveyPrepDraw.lifeOutline / lifeRing / lifeTag, which no test here can run. A text is
// measured as (its characters) x (its px) x 0.5, so the x positions are comparable, not real.
{
  const rec = [];
  const mkG = () => { const st = []; const g = { globalAlpha: 1, lineWidth: 1, strokeStyle: '', fillStyle: '', font: '', lineCap: 'butt', textAlign: 'left', _dash: [], _path: [],
    save() { st.push([g.globalAlpha, g.lineWidth, g.strokeStyle, g.fillStyle, g.font, g.lineCap, g._dash.slice()]); },
    restore() { [g.globalAlpha, g.lineWidth, g.strokeStyle, g.fillStyle, g.font, g.lineCap, g._dash] = st.pop(); },
    setLineDash(d) { g._dash = d.slice(); }, beginPath() { g._path = []; }, moveTo(x, y) { g._path.push(`M${x},${y}`); }, lineTo(x, y) { g._path.push(`L${x},${y}`); },
    arc(x, y, r) { g._path.push(`arc(${x},${y} r${r})`); }, roundRect(x, y, w, h, r) { g._path = [`rr(${x},${+y.toFixed(2)} ${w}x${h} r${r})`]; }, rect() {}, closePath() {},
    fill() { rec.push(`fill ${g.fillStyle} a${+g.globalAlpha.toFixed(3)} ${g._path.join(' ')}`); },
    stroke() { rec.push(`stroke ${g.strokeStyle} w${g.lineWidth} ${g._dash.join('/') || 'solid'} ${g.lineCap} a${+g.globalAlpha.toFixed(3)} ${g._path.join(' ')}`); },
    fillRect() { rec.push(`fillRect ${g.fillStyle} a${+g.globalAlpha.toFixed(3)}`); },
    strokeRect() { rec.push(`strokeRect ${g.strokeStyle} w${g.lineWidth} ${g._dash.join('/') || 'solid'} ${g.lineCap} a${+g.globalAlpha.toFixed(3)}`); },
    measureText(t) { const m = /(\d+(?:\.\d+)?)px/.exec(g.font); return { width: [...t].length * (m ? +m[1] : 10) * 0.5 }; },
    strokeText(t, x, y) { rec.push(`strokeText "${t}" ${g.strokeStyle} w${g.lineWidth} at ${x},${y} a${+g.globalAlpha.toFixed(3)}`); },
    fillText(t, x, y) { rec.push(`fillText "${t}" ${g.fillStyle} at ${x},${+y.toFixed(2)} ${(/bold [\d.]+px/.exec(g.font) || [''])[0]} ${g.textAlign} a${+g.globalAlpha.toFixed(3)}`); } };
    return g; };
  const mk2 = (life, born, extra = {}) => Object.assign(mk(life, born), extra);
  console.log('\nL2 outline, u = 1, a box (10,20)-(110,220) in #4dabf7: plain || picked || in the loupe || picked in the loupe');
  for (const l of L53.LIFE_IDS) console.log('  ', l.padEnd(6), [[false, false], [true, false], [false, true], [true, true]].map(([sel, thin]) => {
    rec.length = 0; return L53.outline(mkG(), mk2(l, 'exist'), [10, 20], [110, 220], '#4dabf7', 1, sel, thin) ? rec.join(' ; ') : 'false (v52 draws it)'; }).join('  ||  '));
  console.log('   a Note with a life:', L53.outline(mkG(), mk2('wish', 'exist', { t: 'NOTE' }), [0, 0], [1, 1], '#fff', 1, false, false));
  console.log('L2 ring, u = 2, a dot of radius 12 at (100,100) in #ffd43b:');
  for (const l of L53.LIFE_IDS) { rec.length = 0; console.log('  ', l.padEnd(6), L53.ring(mkG(), mk2(l, 'exist', { t: 'E' }), [100, 100], 12, '#ffd43b', 2) ? rec.join(' ; ') : 'false (no ring)'); }
  const cases = [
    ['existing', mk2('exist', 'exist')], ['existing + note', mk2('exist', 'exist', { note: 'check' })], ['existing + blank note', mk2('exist', 'exist', { note: '   ' })],
    ['existing + stale act', mk2('exist', 'exist', { act: 'shift' })], ['existing + stale act + note', mk2('exist', 'exist', { act: 'shift', note: 'x' })],
    ['existing born vadd', mk2('exist', 'vadd')], ['existing born vadd + note', mk2('exist', 'vadd', { note: 'x' })], ['existing born vadd + stale act', mk2('exist', 'vadd', { act: 'shift' })],
    ['wish + replace', mk2('wish', 'exist', { act: 'replace' })], ['wish + remove + note', mk2('wish', 'exist', { act: 'remove', note: 'close with brickwork' })],
    ['wish + an act that is none', mk2('wish', 'exist', { act: 'paint' })], ['born wish', mk2('wish', 'wish')], ['born wish + act', mk2('wish', 'wish', { act: 'repair' })],
    ['born wish + office note', mk2('wish', 'wish', { note: 'n', vis: 'office' })], ['vrev + repair', mk2('vrev', 'exist', { act: 'repair' })], ['vok', mk2('vok', 'exist')],
    ['vadd born vadd', mk2('vadd', 'vadd')], ['cok born vadd + note', mk2('cok', 'vadd', { note: 'n' })], ['cok born vadd + act + note', mk2('cok', 'vadd', { act: 'repair', note: 'n' })],
    ['cok + shift', mk2('cok', 'exist', { act: 'shift' })], ['scope', mk2('scope', 'exist', { locked: true })], ['drop + act + why', mk2('drop', 'exist', { act: 'replace', why: 'no budget' })],
    ['drop born vadd', mk2('drop', 'vadd')], ['unknown life', mk2('exist', 'exist', { life: 'zzz', act: 'replace' })],
  ];
  for (const lang of ['en', 'mr']) {
    try { L57.set(lang); } catch (_) { store['mcft-lang-v57'] = JSON.stringify({ lang }); }
    console.log(`L2 paintState under ${L57.lang()} (the canvas alpha is 0.75 going in, a faded mark's): the log, plain | crowded`);
    for (const [n, e] of cases) console.log('  ', n.padEnd(32), [false, true].map(crowded => { L53.clearDrawLog(); const g = mkG(); g.globalAlpha = 0.75;
      L53.paintState(g, e, { x: 100, y: 200, fs: 26 }, 2, { crowded }); const d = L53.drawLog().slice(-1)[0];
      return d ? [d.act, d.noteBadge ? '✎' : null, d.vplus ? 'V+' : null, d.pill ? `[${d.pill} ${d.pillCol}]` : null].filter(Boolean).join(' ') || '(nothing drawn)' : '-'; }).join('  |  '));
  }
  try { L57.set('mr'); } catch (_) {}
  for (const [n, e] of [['wish + remove + note', cases[9][1]], ['cok born vadd + note', cases[17][1]], ['drop + act + why', cases[21][1]]]) {
    rec.length = 0; const g = mkG(); g.globalAlpha = 0.75; L53.paintState(g, e, { x: 100, y: 200, fs: 26 }, 2, {});
    console.log(`L2 paintState, every call for "${n}" (u = 2, the code at x 100, baseline 200, bold 26 px; alpha after: ${g.globalAlpha}):`); for (const r of rec) console.log('     ', r); }
  console.log('L2 nlAttrs (the picked label\'s bar):', L53.LIFE_IDS.map(l => l + '=' + ((/inset 3px 0 0 (#\w+)/.exec(L53.nlAttrs(mk2(l, 'exist'))) || [, 'none'])[1])).join(' '),
    '· a Line:', (/inset/.test(L53.nlAttrs({ id: 'm', kind: 'line', code: 'F-M1' })) ? 'bar' : 'none'));
  console.log('L2 spared (a clear and Start over keep it): existing, locked, wish, dropped, a Note with a life, a Line with a life, a locked Line ->',
    JSON.stringify([mk2('exist', 'exist'), mk2('exist', 'exist', { locked: true }), mk2('wish', 'exist'), mk2('drop', 'exist'), mk2('wish', 'exist', { t: 'NOTE' }),
      { id: 'm', kind: 'line', code: 'F-M1', life: 'wish' }, { id: 'm', kind: 'line', code: 'F-M1', locked: true }].map(x => L53.spared(x))));
  console.log('L2 unlockable: a Scope mark, a Note in "scope", a Client approved mark, a Line in "scope" ->',
    JSON.stringify([mk2('scope', 'exist', { locked: true }), mk2('scope', 'exist', { t: 'NOTE', locked: true }), mk2('cok', 'exist', { locked: true }), { id: 'm', kind: 'line', code: 'F-M1', life: 'scope', locked: true }].map(x => L53.unlockable(x))));
}

// ---- L57: what the page shows under मराठी for each line of the file given (a text it does not know comes back as it went in)
if (process.argv[3]) for (const s of fs.readFileSync(process.argv[3], 'utf8').split('\n').filter(Boolean)) console.log(JSON.stringify(s), '->', JSON.stringify(L57.vis(s)));
