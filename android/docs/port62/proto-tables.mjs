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
// SLICE L3 (11 Nov 2026) adds one section, "L3 …": the Mark sheet, painted and driven by the page's own code bound to a stub
// host -- what it draws for each kind of item at each stage, what each tap and keystroke leaves in its state, and what Done
// writes, with its undo steps. It is the cross-check behind MarkSheetTest, and its first table IS that test's golden:
//   node proto-tables.mjs <page> | awk '/^L3 the sheet/{f=1;next} /^L3 a Tape/{f=0} f' > ../../pano/src/test/resources/marksheet/proto-sheet-rows.txt
// THE L2 REVIEW (11 Nov 2026; DESIGN-port62.md §3.2): given the line `Drop`, the page prints `"Drop" -> "ड्रॉप"` -- its text
// layer finds a text by its English word, and that word is also the Ceiling drop type's short name. The phone's delete
// control says वगळा (life.drop.verb), the verb of वगळले. And the page this was last run on is mock-up v62a (v61c.2 plus the
// prototype items P0 and P2, 9,657 lines): the four modules are cut by their opening lines, so they are found wherever they
// sit, and they print what the 9,563-line v61c.2 printed -- only the first line, their line numbers, differs (+26).
// SLICE L4 (11 Nov 2026) adds one section, "L4 …": the phase in survey prep, by the page's own P54.spPre run on a stub
// survey-prep root -- the step and the tool it moves on, the band it writes -- and L53's "as …": the chip's second half per
// stage, the cycle, and what a placed mark is stamped with; then what the phase leaves of each item's controls and menu rows
// (spEdit / spDelOk / spBulk, as spPost hides them) and what a refused act says. It is the cross-check behind the L4 tests of
// PhaseToolsTest, PaletteTest, PlaceAtTest and DuplicateTest. WHERE THE PHONE DIFFERS, ON PURPOSE (DESIGN-port62.md §3.4):
// it honours the `mark` and `palette` rows only until slice L10, so 1 · Lines is not moved on at stages 1-4 (the page prints
// lines -> mark / list there); and at Client approved it draws 3 · List without steps 1 and 2 and hides a Drop / Restore and
// a Scope mark's padlock (Q-list 6), where the page prints them.
// THE L4 REVIEW (11 Nov 2026; DESIGN-port62.md §3.4, finding 1): the page's menu prints Duplicate on a MARK wherever spEdit is
// true -- so on a mark born in the phase at stages 1, 2, 4 and 5 -- and its copy is the box alone ("a copy of a wish (the box
// alone)" below). The phone's menu has that row on a mark only where the box alone is what a tap would place
// (PhaseTools.copyOk: Survey, and unread with the chip on "as Existing"), because the unstamped copy of a wish would leave the
// office as a surveyed mark (Q-list 8). A Line's and a chain's rows agree with the page's.
// SLICE L5 (12 Nov 2026) adds one section, "L5 …": the ten new mark types and their sub-kinds by the page's own T53 -- NEW, the
// types of each kind of surface, SUBS with each label, what a mark stores and shows for every sub-kind (and for none, and for
// one nobody knows), defaults53 for every type and sub-kind on seventeen faces, measures53 for a mark in each corner and the
// middle of the room's faces, placeRefusal, parseCode and nextCode. The rows between "L5 GOLDEN" and "L5 END" ARE MarkTypesTest's
// golden (pano/src/test/resources/marktypes/proto-type-rows.txt, 992 rows; the command is at the section). WHERE THE PHONE
// DIFFERS, ON PURPOSE: under मराठी it renders a refusal from the T53 keys themselves, with no article (the keys' own notes:
// "{a} is the English article — supply ''"), where the page's text layer finds the sentence by its English and so shows v54's
// refuse.v360 wording for "A …" and leaves "An" / "A" standing in the others; and its next code never reuses a deleted mark's
// number (v52 slice 8, the surface's `seq`), where T53.nextCode -- which nothing on the page calls -- counts the marks still there:
// the NEXT row is printed with nothing deleted, where the two agree.
// THE L5 REVIEW (12 Nov 2026; DESIGN-port62.md §3.5, "The L5 review") adds "L5 REVIEW normPoints": the page's own normPoints
// (not in a module: cut from `function normPoints(w,act){` to its `return n; }`, with the page's own `ckey`) run over ISPOINT =
// E P F PLB LV on an older box of each point with edge readings. Its rows but the last ARE MarkTypesTest's NORM_PAGE table. A plan
// Plumbing point's and Low-voltage point's l r f k take half the box, as a wall's do (`toC=plan&&e.t==='P'`): the phone does the
// same since that review (L5 as built moved them unchanged). WHERE THE PHONE DIFFERS, ON PURPOSE, the last row: a plan FAN moves
// unchanged on the phone (v52 slice 4), where the page adds half its box. And "L5 REVIEW the memory": T53's remembered sub-kind
// is per type and kind of surface, and a new mark takes the memory of the kind it lands on -- what PhaseTools.place's caller (slice
// L6) must hand in (finding 2; MarkTypesTest pins what the phone stores when it is handed another kind's).
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

// ---- SLICE L3: THE MARK SHEET, run on the page's own code -- openSheet (3012), paint (3023-3060), onClick / onInput (3089-3115),
// apply (3071-3088) and interceptDelete (2940) -- bound to a stub host: the cross-check behind MarkSheetTest (pano/MarkSheet.kt:
// view, the pick / type functions, apply). The sheet's HTML is captured as the page writes it and printed as its texts; a tap is
// the page's own click handler given the control's data attribute, and never a control the HTML marks `disabled`.
// WHAT THE PHONE DOES DIFFERENTLY, ON PURPOSE: at stage 5 it draws the approval lock's line and no move (Q-list 6); it has no
// KIND block until L5 / L6 (the stub host has no subsOf either); and under मराठी a Line's and an area's note placeholder is
// keyed whole (life.note_length_optional, life.note_area_optional) where the page glues three pieces and shows English.
{
  const kind = s => (s === 'floor' || s === 'ceil' ? s : 'wall');
  const els = {}; const fake = id => (els[id] = els[id] || { id, hidden: true, innerHTML: '', classList: { toggle() {}, add() {}, remove() {} }, _q: {},
    querySelector(q) { return (this._q[q] = this._q[q] || { disabled: false, value: '', textContent: '', classList: { add() {}, remove() {} }, focus() { this.focused = true; }, setSelectionRange() {} }); },
    querySelectorAll() { return []; } });
  document.getElementById = id => (/^l53(sheet|scrim|pop)$/.test(id) ? fake(id) : null);
  const ls = {}; const rootEl = { querySelector: () => null, appendChild() {}, addEventListener(t, f) { ls[t] = f; } };
  const W = { front: { els: [], meas: [] } }; let stage = 1; const said = [];
  const H = { st: { hist: [], w: W, sel: null, active: null }, snapshot() { H.st.hist.push(JSON.stringify(W)); }, changed() {}, render() {}, snack(t) { said.push('snack: ' + t); }, say(t) { said.push('say: ' + t); },
    markById: id => { const e = W.front.els.find(x => x.id === id); return e ? ['front', e] : [null, null]; },
    lineById: id => { const m = W.front.meas.find(x => x.id === id && (x.kind === 'line' || x.kind === 'area')); return m ? ['front', m] : [null, null]; },
    tinfo: (s, t) => [t, T53.displayName(t, kind(s))], subName: (s, e, sub) => T53.displayName(e.t, kind(s), sub != null ? sub : e.sub),
    trades: () => T53.TRADES, tradeOf: (s, e, cat, sub) => T53.tradeOf(e.kind === 'line' || e.kind === 'area' ? e.kind : e.t, kind(s), sub != null ? sub : e.sub, cat != null ? cat : e.cat),
    catOf: (s, e) => e.cat || 'general', takesCat: t => t === 'NOTE' || t === 'X', isLand: () => false, root: () => rootEl, captureId: () => 'c1', stageIdx: () => stage, NAME: {}, CODE: {} };
  try { L57.set('en'); } catch (_) {}
  L53.bind(H); L53.setNow(() => new Date('2026-11-11T05:00:00Z'));
  const html = () => (els.l53sheet && !els.l53sheet.hidden ? els.l53sheet.innerHTML : null);
  const dec = t => t.replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&lt;/g, '<').replace(/&gt;/g, '>');
  // the painted sheet as ONE ROW, the form MarkSheetTest prints a MarkSheet.View in: every text, every dot's colour, and each
  // control's state -- (on) picked, (off) disabled
  const col = t => ((/background:(#\w+)/.exec(t) || [, ''])[1] || '').toLowerCase();
  const txt = t => dec(t.replace(/<[^>]+>/g, ''));
  const shown = () => { const h = html(); if (h == null) return '(closed)'; const out = [];
    const one = (re, f) => { const m = re.exec(h); if (m) out.push(f(m)); };
    one(/<span class="l53ttl">(.*?)<\/span>/, m => txt(m[1]));
    one(/<span class="l53trd">(<i[^>]*><\/i>)(.*?)<\/span>/, m => `trade ${col(m[1])} ${txt(m[2])}`);
    one(/<div class="l53lab">([^<]*)<\/div><button class="l53c l53tp" data-l53trp="1"( disabled)?>(<i[^>]*><\/i>)(.*?)<\/button>/, m => `${txt(m[1])} ${col(m[3])} ${txt(m[4])}${m[2] ? ' (off)' : ''}`);
    one(/<div class="l53blk l53state"><div class="l53lab">([^<]*)<\/div><div class="l53now"[^>]*>(<i[^>]*><\/i>)<b>(.*?)<\/b>(.*?)<\/div>/, m => `${txt(m[1])} | now ${col(m[2])} ${txt(m[3])}${txt(m[4].replace(/<span class="l53vp">(.*?)<\/span>/, '[$1]'))}`);
    one(/<div class="l53chips l53acts">(.*?)<\/div>/, m => 'acts ' + [...m[1].matchAll(/<button class="l53c (on)?" data-l53act="(\w+)"( disabled)?>(.*?)<\/button>/g)].map(a => `${txt(a[4])}${a[1] ? '(on)' : ''}${a[3] ? '(off)' : ''}`).join(', '));
    for (const m of h.matchAll(/<button class="l53mv (on)?" data-l53mv="(\w+)"( disabled)?>(<i[^>]*><\/i>)<span>(.*?)<\/span>(?:<small>(.*?)<\/small>)?<\/button>(?:<input class="l53why" data-l53why="\w+" maxlength="(\d+)" placeholder="([^"]*)" value="([^"]*)"( disabled)?>)?/g))
      out.push(`move ${m[2]} ${col(m[4])} ${txt(m[5])}${m[6] ? ' ~ ' + txt(m[6]) : ''}${m[1] ? ' (on)' : ''}${m[3] ? ' (off)' : ''}${m[7] ? ` + why[${dec(m[9])}]${m[10] ? '(off)' : ''} max ${m[7]} ph[${dec(m[8])}]` : ''}`);
    for (const m of h.matchAll(/<div class="l53(gate|amb|foot)">(.*?)<\/div>/g)) out.push(`${m[1]}: ${txt(m[2])}`);
    one(/<div class="l53blk l53note"><div class="l53lab">([^<]*)<span class="l53cnt"[^>]*>([^<]*)<\/span><\/div><textarea[^>]*maxlength="(\d+)"[^>]*placeholder="([^"]*)">([^<]*)<\/textarea>(.*?)<\/div><\/div>/,
      m => `${txt(m[1]).trim()} ${m[2]} [${dec(m[5])}] ph[${dec(m[4])}] ` + [...m[6].matchAll(/<button class="(on)?" data-l53vis="(\w+)"[^>]*>(.*?)<\/button>/g)].map(v => `${txt(v[3])}${v[1] ? '(on)' : ''}`).join(' / '));
    one(/<button class="l53b on" data-l53apply="1">(.*?)<\/button>/, m => `${txt(m[1])} ${els.l53sheet._q['[data-l53apply]'] && els.l53sheet._q['[data-l53apply]'].disabled ? 'off' : 'on'}`);
    if (els.l53pop && !els.l53pop.hidden) { const q = els.l53pop.innerHTML; out.push('picker ' + txt((/<div class="l53lab">(.*?)<\/div>/.exec(q) || [, ''])[1]) + ': ' +
      [...q.matchAll(/<button class="l53c (on)?" data-l53cat="([\w-]+)">(<i[^>]*><\/i>)(.*?)<\/button>/g)].map(c => `${txt(c[4])} ${col(c[3])}${c[1] ? '(on)' : ''}`).join(', ')); }
    return out.join(' | '); };
  const disabled = attr => new RegExp(`data-l53${attr}[^>]* disabled`).test(html() || '');
  const tap = (k, v) => { if (k !== 'apply' && k !== 'close' && disabled(`${k}="${v == null ? '1' : v}"`)) return 'off'; ls.click({ target: { closest: () => ({ dataset: { ['l53' + k]: v == null ? '1' : v }, id: '' }) }, stopPropagation() {}, preventDefault() {} }); return 'ok'; };
  const type = (k, v, text) => { if (k === 'why' && disabled(`why="${v}"`)) return 'off'; ls.input({ target: { dataset: { ['l53' + k]: v == null ? '1' : v }, value: text } }); L53.sheetState() && tap('vis', L53.sheetState().vis); return 'ok'; };
  const M = (id, t, code, x = {}) => Object.assign({ id, t, code, x0: 0, x1: 1, y0: 0, y1: 1 }, x);
  const put = (...items) => { W.front.els = items.filter(x => !x.kind); W.front.meas = items.filter(x => x.kind); H.st.hist.length = 0; said.length = 0; };
  const at = '2026-11-10T06:37:09+05:30';
  const ITEMS = () => [
    ['a surveyed window', M('a', 'W', 'FW1')], ['... with a note for the office', M('a', 'W', 'FW1', { note: 'check the sill', vis: 'office' })],
    ['a wish to replace', M('a', 'W', 'FW1', { life: 'wish', lifeAt: at, lifeAs: 'client', act: 'replace' })], ['a mark born a wish', M('a', 'W', 'FW1', { born: 'wish', life: 'wish', lifeAt: at, lifeAs: 'client' })],
    ['in Vendor review', M('a', 'W', 'FW1', { life: 'vrev', lifeAt: at, lifeAs: 'staff', act: 'shift' })], ['Vendor approved', M('a', 'W', 'FW1', { life: 'vok', lifeAt: at, lifeAs: 'vendor', act: 'replace' })],
    ['Vendor added', M('a', 'W', 'FW1', { born: 'vadd', life: 'vadd', lifeAt: at, lifeAs: 'vendor' })], ['Client approved', M('a', 'W', 'FW1', { life: 'cok', lifeAt: at, lifeAs: 'client', act: 'repair' })],
    ['Client approved, born Vendor added', M('a', 'W', 'FW1', { born: 'vadd', life: 'cok', lifeAt: at, lifeAs: 'client' })], ['in Scope', M('a', 'W', 'FW1', { life: 'scope', lifeAt: at, lifeAs: 'staff', act: 'replace', locked: true })],
    ['Dropped', M('a', 'W', 'FW1', { life: 'drop', lifeAt: at, lifeAs: 'client', act: 'remove', why: 'no budget' })], ['Dropped, born Vendor added', M('a', 'W', 'FW1', { born: 'vadd', life: 'drop', lifeAt: at, lifeAs: 'client', why: 'no budget' })],
    ['a locked door', M('a', 'D', 'FD1', { locked: true })], ['a locked wish', M('a', 'D', 'FD1', { life: 'wish', lifeAt: at, lifeAs: 'client', act: 'remove', locked: true })],
    ['an Other', M('a', 'X', 'FX1')], ['an Other for the carpenter, locked', M('a', 'X', 'FX1', { cat: 'carpentry', locked: true })], ['an Other whose cat is no trade', M('a', 'X', 'FX1', { cat: 'zzz' })],
    ['a switch plate', M('a', 'S', 'FS1')], ['a stamp nobody can read, decided by somebody else', M('a', 'W', 'FW1', { life: 'wish', lifeAt: 'soon', lifeAs: 'architect' })],
    ['a Line', { id: 'a', kind: 'line', code: 'F-M1', pts: [[0, 0], [9, 0]] }], ['an area for the tiler, locked, with a note', { id: 'a', kind: 'area', code: 'F-M2', pts: [[0, 0], [9, 0], [9, 9]], cat: 'tiling', locked: true, note: 'skirting too' }],
  ];
  console.log('\nL3 the sheet as the page paints it, item by item, at stages 0..5 and unread (each dot as its colour before its text; ~ before the hint of a move; (on) picked, (off) disabled):');
  for (const [n, e] of ITEMS()) { console.log('  ', n, '=', JSON.stringify(e)); for (const s of STAGES) { stage = s; put(ITEMS().find(x => x[0] === n)[1]); L53.openSheet('a'); console.log('      ', String(s).padEnd(4), shown()); L53.cancelSheet(); } }
  console.log('L3 a Tape, a chain and an unknown id open no sheet:', JSON.stringify([(put({ id: 'a', kind: 'tape', code: 'F-T1', pts: [] }), L53.openSheet('a')), (put(), L53.openSheet('zz'))]));
  // ---- journeys: what each tap and keystroke leaves in the sheet's state, and what Done writes
  const S = () => { const x = L53.sheetState(); return x ? JSON.stringify({ to: x.to, why: x.why, act: x.act, act0: x.act0, note: x.note, vis: x.vis, cat: x.cat, picker: x.picker, focusMove: x.focusMove }) : '(closed)'; };
  const done = () => { tap('apply'); return `${L53.isOpen() ? 'OPEN' : 'closed'} · undo steps ${H.st.hist.length} · ${JSON.stringify(W.front.els[0] || W.front.meas[0])}${said.length ? ' · ' + said.join(' ; ') : ''}`; };
  const J = (title, f) => { console.log('  ', title); f(l => console.log('      ', l)); };
  console.log('L3 journeys (stage 1 unless said):');
  stage = 1;
  J('nothing changed: Done closes with no undo step', p => { put(M('a', 'W', 'FW1')); L53.openSheet('a'); p(done()); });
  J('Existing -> Client wish, Remove, a note for the office', p => { put(M('a', 'W', 'FW1')); L53.openSheet('a'); p('tap wish: ' + tap('mv', 'wish') + ' ' + S()); p('tap wish again: ' + tap('mv', 'wish') + ' ' + S()); tap('mv', 'wish');
    p('act remove: ' + tap('act', 'remove') + ' ' + S()); type('note', null, 'close with brickwork'); tap('vis', 'office'); p(shown()); p(done());
    L53.openSheet('a'); p('reopened: ' + S()); p('act shift alone: ' + tap('act', 'shift')); p(done());
    p('Delete: handled ' + L53.interceptDelete('a') + ' ' + S()); p('why "no": ' + type('why', 'drop', 'no') + ' ' + S() + ' · tap drop: ' + tap('mv', 'drop')); p('why "no budget": ' + type('why', 'drop', 'no budget') + ' ' + S());
    p('why "n" again: ' + type('why', 'drop', 'n') + ' ' + S()); type('why', 'drop', '  not needed  '); p(shown()); p(done());
    p('Delete on the Dropped mark: handled ' + L53.interceptDelete('a') + ' · ' + JSON.stringify(W.front.els[0]) + ' · undo steps ' + H.st.hist.length + ' · ' + said.join(' ; ')); });
  J('a why typed with no Delete before it does not pick the move; a tap does', p => { put(M('a', 'W', 'FW1', { life: 'wish', lifeAs: 'client', act: 'replace' })); L53.openSheet('a'); p('why: ' + type('why', 'drop', 'too dear') + ' ' + S()); p('tap: ' + tap('mv', 'drop') + ' ' + S()); p('its why cut short again: ' + (type('why', 'drop', 'to'), S())); });
  J('← Back to Existing takes the act and the stamp away, and keeps the note', p => { put(M('a', 'W', 'FW1', { note: 'n', life: 'wish', lifeAt: at, lifeAs: 'client', act: 'shift' })); L53.openSheet('a'); p('tap exist: ' + tap('mv', 'exist') + ' · acts drawn: ' + /l53acts/.test(html())); p(done()); });
  J('the note: 281 characters are cut to 280, an emoji never halved; Office only is kept only beside a note', p => { put(M('a', 'W', 'FW1')); L53.openSheet('a'); type('note', null, 'x'.repeat(279) + '😀😀'); tap('vis', 'office'); const r = done(); const e = W.front.els[0];
    p(`note ${[...e.note].length} characters, ${e.note.length} units, ends ${JSON.stringify(e.note.slice(-3))} · vis ${e.vis} · undo steps ${H.st.hist.length}`);
    L53.openSheet('a'); type('note', null, '   '); p('blanked: ' + done()); L53.openSheet('a'); tap('vis', 'office'); p('Office only with no note: ' + done()); });
  J('an Other takes a trade; General deletes it', p => { put(M('a', 'X', 'FX1')); L53.openSheet('a'); p('open the picker: ' + tap('trp')); p(shown()); p('carpentry: ' + tap('cat', 'carpentry') + ' ' + S()); p(shown()); p(done());
    L53.openSheet('a'); tap('trp'); tap('cat', 'general'); p(done()); L53.openSheet('a'); tap('trp'); tap('trp'); p('opened and shut: ' + done()); });
  J('a locked mark changes its note alone', p => { put(M('a', 'D', 'FD1', { life: 'wish', lifeAt: at, lifeAs: 'client', act: 'remove', locked: true })); L53.openSheet('a'); p('drop: ' + tap('mv', 'drop') + ' · its why: ' + type('why', 'drop', 'because') + ' · act: ' + tap('act', 'shift')); type('note', null, 'ask the client'); p(done()); });
  J('a locked Other keeps its trade', p => { put(M('a', 'X', 'FX1', { cat: 'carpentry', locked: true })); L53.openSheet('a'); p('the picker: ' + tap('trp') + ' ' + S()); type('note', null, 'n'); p(done()); });
  J('a Line takes a trade and a note; a locked one its note alone', p => { put({ id: 'a', kind: 'line', code: 'F-M1', pts: [[0, 0], [9, 0]] }); L53.openSheet('a'); tap('trp'); tap('cat', 'painting'); type('note', null, 'cornice'); tap('vis', 'office'); p(done());
    W.front.meas[0].locked = true; L53.openSheet('a'); p('the picker: ' + tap('trp')); type('note', null, 'cornice, both sides'); tap('vis', 'cv'); p(done()); });
  J('Scope, after the estimate (stage 4): out of scope unlocks, and the office is told', p => { stage = 4; put(M('a', 'W', 'FW1', { life: 'scope', lifeAt: at, lifeAs: 'staff', act: 'replace', locked: true })); L53.openSheet('a'); p('tap cok: ' + tap('mv', 'cok')); p(shown()); p(done());
    L53.openSheet('a'); tap('mv', 'scope'); p(shown()); p(done()); stage = 1; });
  J('a Note needs its words', p => { put(M('a', 'NOTE', 'FNOTE1', { note: 'old' })); L53.openSheet('a'); p(shown()); type('note', null, '  '); p(done()); type('note', null, 'new words'); tap('trp'); tap('cat', 'civil'); p(done()); });
  J('the stage moves under an open sheet (1 -> 3): the move picked is no longer offered', p => { put(M('a', 'W', 'FW1')); L53.openSheet('a'); tap('mv', 'wish'); stage = 3; p(done()); p(S()); stage = 1; });
  J('a mark past Existing with no act stored: Done writes Replace', p => { put(M('a', 'W', 'FW1', { life: 'wish' })); L53.openSheet('a'); p(S()); p(done()); });
  // the L3 review (11 Nov 2026): the page's apply has no stage term in actCh, so it writes Replace at Survey too, where it draws
  // no chip; at Client approved it draws the chips (Vendor review's sheet) and writes. THE PHONE WRITES NONE AT CLIENT APPROVED:
  // its own gate draws no chip there (Q-list 6), and a control that is not drawn must not write. Survey stays as the page has it.
  J('... the same at Survey (stage 0) and at Client approved (stage 5), nothing touched, then with a note typed -- and an act this build does not know', p => { for (const s of [0, 5]) { stage = s;
      put(M('a', 'W', 'FW1', { life: 'wish', lifeAs: 'client' })); L53.openSheet('a'); p(`stage ${s} · acts drawn: ${/l53acts/.test(html())} · ` + done());
      put(M('a', 'W', 'FW1', { life: 'wish', lifeAs: 'client' })); L53.openSheet('a'); type('note', null, 'ask the client'); p(`stage ${s} · a note typed: ` + done());
      put(M('a', 'W', 'FW1', { life: 'wish', act: 'closeup' })); L53.openSheet('a'); type('note', null, 'n'); p(`stage ${s} · act "closeup", a note typed: ` + done()); } stage = 1; });
  J('an act chip changed beside a move past the wish is not written: the act is written only on Existing -> Client wish (stage 2)', p => { stage = 2; put(M('a', 'W', 'FW1', { life: 'wish', lifeAt: at, lifeAs: 'client', act: 'replace' })); L53.openSheet('a'); tap('act', 'shift'); tap('mv', 'vrev'); p(S()); p(done()); stage = 1; });
  J('at Estimation (stage 3) no move is offered, and an act chip changed alone is still written', p => { stage = 3; put(M('a', 'W', 'FW1', { life: 'wish', lifeAt: at, lifeAs: 'client', act: 'replace' })); L53.openSheet('a'); p('act shift: ' + tap('act', 'shift')); p(done()); stage = 1; });
  J('a Dropped mark restored from its sheet', p => { put(M('a', 'W', 'FW1', { note: 'n', life: 'drop', lifeAt: at, lifeAs: 'client', act: 'remove', why: 'no budget' })); L53.openSheet('a'); p(shown()); tap('mv', 'wish'); p(done()); });
  J('an unknown audience is put right by any Done', p => { put(M('a', 'W', 'FW1', { note: 'n', vis: 'cv' })); L53.openSheet('a'); p(done()); });
  console.log('L3 sinceText:', JSON.stringify(['2026-10-16T06:37:09+05:30', '2026-09-03T23:59:59+05:30', '', 'soon', undefined].map(x => L53.sinceText(x))));
  try { L57.set('mr'); } catch (_) {}
  console.log('L3 under मराठी, the texts the page finds:', JSON.stringify(['STATE', 'TRADE', 'NOTE', 'Done', 'Close', 'Mark sheet', '↻ Replace', '⇢ Shift', '⚒ Repair', '✕ Remove', ' · decided by staff', ' · decided by the surveyor', 'since 16 Oct, 06:37 IST',
    'Client & vendors', 'Office only', 'A note on this mark (optional)', 'A note on this length (optional)', 'What should the office or the vendor know here?', 'Trade · FX1', 'Open the Mark sheet for FW1',
    'FD1 is locked — only its note can change', 'A Note needs its words — type them, or delete the Note'].map(x => L57.vis(x))));
}

// ---- SLICE L4: the phase in survey prep -- P54.spPre (4204-4224) on a stub root, L53's asChipHTML / cycleAs / asPlace
// (2848-2860), and P54's editable / delOk / spBulk / refuseText (4232-4264) as spPost applies them to a row and to the menu
{
  try { L57.set('en'); } catch (_) {}
  const QUOTE = P54.QUOTE, strip = h => String(h).replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim();
  // a survey-prep root with a band element spPre can write, in portrait (`land` false) or landscape
  const rootOf = land => { const bandEl = { dataset: {}, attrs: {}, innerHTML: '', parentElement: null, setAttribute(k, v) { this.attrs[k] = v; }, remove() {} };
    const host = id => ({ id, firstElementChild: null, lastElementChild: null, firstChild: null, insertBefore(b) { b.parentElement = this; this.firstElementChild = b; b.where = id + ':first'; }, appendChild(b) { b.parentElement = this; this.lastElementChild = b; b.where = id + ':last'; } });
    const hosts = { '#spctl': host('spctl'), '#spmain': host('spmain') };
    return { bandEl, dataset: {}, classList: { contains: c => c === 'land' && land }, querySelector: q => (q === '#p54band' ? bandEl : hosts[q] || null) }; };
  const pre = (stage, mode, tool, markable, land = false) => { const root = rootOf(land), st = { tab: 'front', mode, tool, more: true };
    P54.spPre({ root, st, meta: { quote: stage == null ? null : QUOTE[stage], project: 'none' }, SURF: ['front'], frame: () => markable }); return { root, st }; };
  console.log('\nL4 spPre, the step and the tool moved on, at stages 0..5 and unread:');
  for (const [mode, markable] of [['lines', true], ['lines', false], ['mark', true], ['list', true], ['360', true]])
    console.log('  ', (mode + (mode === 'lines' ? (markable ? ' (lines closed)' : ' (lines open)') : '')).padEnd(22), STAGES.map(s => pre(s, mode, 'sel', markable).st.mode).join(' '));
  for (const tool of ['E', 'T', 'M', 'K', 'sel']) console.log('   tool', tool.padEnd(17), STAGES.map(s => pre(s, 'mark', tool, true).st.tool).join(' '));
  console.log('L4 the band spPre writes (pips on / 5 | its text | its aria-label | where), portrait then landscape:');
  for (const s of STAGES) for (const land of [false, true]) { const { root } = pre(s, 'mark', 'sel', true, land), b = root.bandEl;
    console.log('  ', String(s).padEnd(4), land ? 'land' : 'port', '|', (b.innerHTML.match(/<i class="on">/g) || []).length, '|', strip(b.innerHTML.replace(/<\/b>/, '</b> ')), '|', b.attrs['aria-label'], '|', b.where, '| data-ph', root.dataset.ph54); }
  console.log('L4 the chip\'s second half, asChipHTML at stages 0..5 and unread (text | aria-label | what it is):');
  try { localStorage.removeItem('mcft-placeas-v53'); } catch (_) {}
  for (const s of STAGES) { const h = L53.asChipHTML('capA', s); console.log('  ', String(s).padEnd(4), '|', JSON.stringify(strip(h)), '|', (/aria-label="([^"]*)"/.exec(h) || [, ''])[1], '|', !h ? 'none' : /<button/.test(h) ? 'button' : 'plain words'); }
  console.log('   drawn where the palette is:', bits(s => P54.can('palette', s)), '(0: no chip at all)');
  { const seq = []; for (let i = 0; i < 4; i++) { seq.push(strip(L53.asChipHTML('capA', null)) + ' [places ' + L53.asFor('capA', null) + ']'); L53.cycleAs('capA', null); }
    console.log('   unread, a tap on the button between each:', seq.join('  ->  '), '· another capture:', strip(L53.asChipHTML('capB', null)));
    console.log('   cycled to wish on capA, then read at 0..5:', (L53.cycleAs('capA', null), STAGES.slice(0, 6).map(s => L53.asFor('capA', s)).join(',')), '· cycleAs at 1:', L53.cycleAs('capA', 1)); }
  L53.setNow(() => new Date('2026-11-11T05:00:09Z')); try { localStorage.removeItem('mcft-placeas-v53'); } catch (_) {}
  console.log('L4 a mark placed (asPlace with asFor), the keys it gains, at stages 0..5 and unread -- the page asks only where its palette is:');
  for (const s of STAGES) { const e = { id: 'n', t: 'E', code: 'FE2', locked: false }; const r = L53.asPlace(e, L53.asFor('capZ', s), s); delete e.id; delete e.t; delete e.code; delete e.locked;
    console.log('  ', String(s).padEnd(4), P54.can('palette', s) ? 'placed  ' : 'NO TOOL ', JSON.stringify(e), '-> ' + r, '· said:', s != null && s >= 3 ? 'After the estimate — tell the office' : '-'); }
  console.log('   a Note placed at stage 1:', JSON.stringify(L53.asPlace({ t: 'NOTE', code: 'FNOTE1' }, 'wish', 1)));
  // the picked row's controls and the menu's rows, as spPost hides them: N nudges (spEdit), L padlock (spEdit), D delete (spDelOk)
  const its = { 'a surveyed mark': mk('exist', 'exist'), 'born a wish': mk('wish', 'wish'), 'born Vendor added': mk('vadd', 'vadd'), 'a surveyed mark wished': mk('wish', 'exist'),
    'a copy of a wish (the box alone)': mk('exist', 'exist'), 'born Vendor added, in Scope, locked': Object.assign(mk('scope', 'vadd'), { locked: true }),
    'a Line': { id: 'm', kind: 'line', code: 'F-M1' }, 'a chain': { id: 'k', code: 'FK1', nodes: [[0, 0], [9, 0]] } };
  console.log('L4 the picked item\'s controls the phase leaves (N nudges and tabs, L padlock, D delete) and its menu rows, at stages 0..5 and unread:');
  for (const [n, e] of Object.entries(its)) console.log('  ', (n + ':').padEnd(38), STAGES.map(s => { pre(s, 'mark', 'sel', true); return (P54.spEdit(e) ? 'NL' : '--') + (P54.spDelOk(e) ? 'D' : '-'); }).join(' '),
    ' menu:', STAGES.map(s => { pre(s, 'mark', 'sel', true); return [P54.spDelOk(e) ? (e.kind || e.nodes ? 'Delete' : L53.delWord(e)) : null, ...(P54.spEdit(e) ? ['Duplicate', e.locked ? 'Unlock' : 'Lock', 'Lock all'] : []), 'Cancel'].filter(Boolean).join('|'); }).join('  '));
  console.log('L4 ⋮ Clear, Lock all and Start over shown (spBulk):', STAGES.map(s => { pre(s, 'mark', 'sel', true); return P54.spBulk() ? 1 : 0; }).join(''));
  console.log('L4 what a refused act says (refuseText), stage by stage:');
  for (const s of [1, 2, 3, 4, 5]) { pre(s, 'mark', 'sel', true); console.log('  ', s, '|', [its['a surveyed mark'], its['born a wish'], its['born Vendor added'], its['a Line'], null, 'restart'].map(x => P54.refuseText(x)).join(' | ')); }
  try { L57.set('mr'); } catch (_) {}
  console.log('L4 under मराठी, the texts the page finds:', JSON.stringify(['as Existing', 'as Client wish', 'as Vendor added', 'Marks are placed as Client wish in this phase', 'Place marks as Existing — tap to change',
    'Placing Electric point · tap the photo', 'every mark you place is a client wish', 'Phase not read yet — S5 decides when online', 'FW1 was marked in Survey — move the phase back to change it',
    'After the estimate — tell the office'].map(x => L57.vis(x))));
}

// ---- SLICE L5: THE TEN NEW TYPES AND THEIR SUB-KINDS, by the page's own T53 -- NEW (2397-2407), SUBS (2423-2466),
// displayName (2478), the code grammar (2534-2543), defaults53 (2551-2565), measures53 (2569-2588), placeRefusal (2590).
// Every row from "L5 GOLDEN" to "L5 END" is one line of MarkTypesTest's golden: that test prints the same rows from
// pano/MarkTypes.kt and SurveyPrep and compares them line for line.
//   node proto-tables.mjs <page> | awk '/^L5 GOLDEN/{f=1;next} /^L5 END/{f=0} f' > ../../pano/src/test/resources/marktypes/proto-type-rows.txt
// A figure is printed to 0.001 mm; a box as x0,x1,y0,y1; `v52` where the page's function answers null (sp.js's own v52
// rule decides); `site` for a measure with no photo estimate. The faces are the tests' room (2678 x 2677 x 3319), a wide
// wall, faces smaller than a default box, a narrow one and a large one, so every clamp is reached.
{
  try { L57.set('en'); } catch (_) {}
  const n = v => (v == null ? 'site' : String(Math.round(v * 1000) / 1000));
  const boxOf = d => (d ? [d.x0, d.x1, d.y0, d.y1].map(n).join(',') : 'v52');
  const SURF = { wall: 'front', floor: 'floor', ceil: 'ceil' };
  const FACES = { wall: [[2678, 2677], [3319, 2677], [6000, 3000], [1000, 1000], [400, 2400], [180, 80], [40, 30]], floor: [[2678, 3319], [6000, 4000], [500, 5000], [180, 80], [40, 30]], ceil: [[2678, 3319], [6000, 4000], [500, 5000], [180, 80], [40, 30]] };
  const subsPlus = (t, k) => [undefined, ...T53.subsOf(t, k).map(x => x.id), 'zzz'];
  const nm = sub => (sub === undefined ? '(none)' : sub);
  console.log('\nL5 GOLDEN');
  for (const x of T53.NEW) console.log('NEW', x.t, '|', x.name, '|', x.col, '|', x.kinds.join(' '), '|', x.short, '|', x.trade || '-', '|', T53.isPin(x.t) ? 'pin' : T53.isPoint53(x.t) ? 'point' : 'rect', '|', x.ink ? 1 : 0);
  for (const k of T53.KINDS) console.log('TYPES53', k, '|', T53.TYPES53[k].map(r => r.join('=')).join(' ; '));
  for (const k of T53.KINDS) console.log('ON', k, '|', T53.ON[k].join(' '));
  console.log('POINT |', ALLT.filter(t => T53.isPoint53(t)).join(' '), '| PIN |', ALLT.filter(t => T53.isPin(t)).join(' '), '| NEW |', ALLT.filter(t => T53.isNew(t)).join(' '));
  for (const t of ALLT) console.log('CANGO', t, '|', T53.KINDS.map(k => k + '=' + (T53.canGo(t, k) == null ? '-' : T53.canGo(t, k) ? 1 : 0)).join(' '), '| typeName', T53.typeName(t));
  console.log('typeName of a token nobody knows |', T53.typeName('QQ'));
  for (const t of ALLT) for (const k of T53.KINDS) {
    const L = T53.subsOf(t, k);
    console.log('SUBS', t, k, '|', L.map(x => x.id + '=' + x.label + (x.service ? ' service:' + x.service : '') + (x.trade ? ' trade:' + x.trade : '')).join(' ; ') || '-', '| first', T53.firstSub(t, k) || '-',
      '| stored', subsPlus(t, k).map(sub => nm(sub) + '>' + (T53.subToStore(t, k, sub) || '-')).join(' '));
    console.log('NAME', t, k, '|', subsPlus(t, k).map(sub => nm(sub) + '=' + T53.displayName(t, k, sub)).join(' ; '));
  }
  for (const k of T53.KINDS) for (const [A, B] of FACES[k]) for (const t of ALLT)
    console.log('BOX', SURF[k], A + 'x' + B, t, '|', subsPlus(t, k).map(sub => nm(sub) + '=' + boxOf(T53.defaults53(SURF[k], t, sub, A, B))).join(' ; '));
  // a mark of each new type in each corner and in the middle of the room's faces: every nearer-side key, with its words
  const ROOM = { wall: [2678, 2677], floor: [2678, 3319], ceil: [2678, 3319] };
  for (const k of T53.KINDS) { const [A, B] = ROOM[k];
    for (const t of ALLT) { const pt = T53.isPoint53(t) || T53.isPin(t), w = pt ? 50 : 300, h = pt ? 50 : 200;
      for (const [x, y] of [[10, 10], [A - w - 10, B - h - 10], [(A - w) / 2, (B - h) / 2], [10, B - h - 10], [A - w - 10, 10], [0, 0]]) {
        const e = { t, x0: x, x1: x + w, y0: y, y1: y + h }, m = T53.measures53(SURF[k], e, A, B);
        console.log('MEAS', SURF[k], t, '@' + boxOf(e), '|', m == null ? 'v52' : m.map(r => r[0] + ':' + r[1] + ':' + n(r[2])).join(' ; ') || '(none)'); } } }
  for (const t of ALLT) console.log('REFUSE', t, '|', T53.KINDS.map(k => k + '=' + (T53.placeRefusal(t, k) || 'ok')).join(' | '));
  for (const c of ['FW1', 'FP1', 'FPLB1', 'FPLB12.cl', 'CCDP1', 'GNOTE2', 'LLV3', 'LL1', 'RS4.w', 'BCRP10.p', 'F-M1', 'F-M2.per', 'G-T3', 'FK1', 'FK1.t', 'FK12.3', 'FWx7', 'FW01', 'fw1', 'XW1', 'F1', 'FW', 'F-X1', 'FPLB1.c l', 'FPLB1.', '', 'F W1'])
    { const p = T53.parseCode(c); console.log('CODE', JSON.stringify(c), '|', p ? [p.surface, p.surf, p.token == null ? '-' : p.token, p.kind, p.n, p.measure == null ? '-' : p.measure].join(' ') : 'null', '| token', T53.codeToken(c) || '-'); }
  // the next code of a token on a surface: counted per TOKEN, so P and PLB (and L and LV) never share a number
  const ELS = [['P', 'FP1'], ['PLB', 'FPLB1'], ['PLB', 'FPLB7'], ['P', 'FP2'], ['L', 'FL3'], ['LV', 'FLV9'], ['S', 'FS2'], ['SAN', 'FSAN5'], ['C', 'FC4'], ['CRP', 'FCRP6'], ['CTR', 'FCTR1'], ['NOTE', 'FNOTE2'], ['D', 'FD1'], ['DEF', 'FDEF8']].map(([t, code]) => ({ t, code }));
  console.log('NEXT front |', ALLT.map(t => t + '>' + T53.nextCode('front', t, ELS)).join(' '));
  console.log('L5 END');
  // ---- the L5 review: an older box of a point, by the page's own normPoints (MarkTypesTest NORM_PAGE; the last row is the Fan's)
  { const a = lines.findIndex(l => l.startsWith('function normPoints(w,act){')), b = lines.findIndex((l, i) => i > a && l.trim() === 'return n; }');
    const ck = lines.find(l => l.startsWith('const ckey='));
    if (a < 0 || b < 0 || !ck) throw new Error('normPoints not found');
    const PTS = ['E', 'P', 'F', ...T53.TOKENS.filter(t => T53.isPoint53(t))];
    const np = new Function('ISPOINT', 'isPlan', 'SURF', 'st', 'ptLog', 'SPOT', ck + '\n' + lines.slice(a, b + 1).join('\n') + '\nreturn normPoints;')(
      t => PTS.includes(t), s => s === 'floor' || s === 'ceil', Object.values(SURF).concat(['right', 'back', 'left']), { w: null, actual: null }, [], 50);
    console.log('\nL5 REVIEW normPoints', `${a + 1}-${b + 1}`, 'over', PTS.join(' '), '|', lines[a + 6].trim());
    for (const [s, t, code, box, rd] of [
      ['floor', 'PLB', 'GPLB1', [100, 250, 200, 350], { l: 40, k: 60 }], ['floor', 'PLB', 'GPLB2', [100, 250, 200, 350], { r: 40, f: 60, w: 120 }],
      ['ceil', 'LV', 'CLV2', [1000, 1200, 2000, 2100], { f: 7 }], ['ceil', 'LV', 'CLV3', [1000, 1200, 2000, 2100], { l: 10, k: 20, h: 80 }],
      ['front', 'PLB', 'FPLB1', [300, 450, 800, 950], { l: 300, s: 800, w: 150 }], ['front', 'LV', 'FLV1', [300, 450, 800, 950], { r: 300, t: 800 }],
      ['ceil', 'P', 'CP9', [500, 650, 800, 950], { r: 900, f: 700 }], ['floor', 'P', 'GP9', [925, 1075, 1925, 2075], { l: 1000, k: 2000 }],
      ['floor', 'E', 'GE1', [0, 150, 0, 150], { l: 75, k: 75 }], ['ceil', 'F', 'CF1', [1000, 1200, 1000, 1200], { l: 1100 }]]) {
      const act = Object.fromEntries(Object.entries(rd).map(([k, v]) => [code + '.' + k, v])), e = { id: 'a', t, code, x0: box[0], x1: box[1], y0: box[2], y1: box[3] };
      np({ [s]: { els: [e] } }, act);
      console.log([s, t, code, box.join(','), ...Object.entries(rd).map(([k, v]) => k + '=' + v)].join(' '), '->', Object.keys(act).sort().map(k => k + '=' + act[k]).join(' '), '@', [e.x0, e.x1, e.y0, e.y1].join(',')); } }
  // ---- the L5 review, finding 2: the tool's remembered sub-kind is kept per type AND kind of surface, and a new mark is given the
  // memory of the kind it LANDS on (addElAt: T53.newFields(t, tkind(s))) -- so a kind picked on a wall says nothing about the floor
  { const nf = (t, k) => t + '@' + k + ' ' + JSON.stringify(T53.newFields(t, k));
    console.log('L5 REVIEW the memory, untouched:', ['PLB', 'CRP', 'LV'].flatMap(t => T53.KINDS.filter(k => T53.subsOf(t, k).length).map(k => nf(t, k))).join(' | '));
    T53.setSub('PLB', 'wall', 'hot'); T53.setSub('CRP', 'wall', 'other'); T53.setSub('LV', 'wall', 'wifi'); T53.setSub('PLB', 'floor', 'mixer');
    console.log('L5 REVIEW after the wall\'s picker chose PLB hot, CRP other, LV wifi (and the floor was offered PLB mixer, not a floor kind):',
      ['PLB', 'CRP', 'LV'].flatMap(t => T53.KINDS.filter(k => T53.subsOf(t, k).length).map(k => nf(t, k))).join(' | ')); }
  try { L57.set('mr'); } catch (_) {}
  console.log('L5 under मराठी, the texts the page finds (a name it has no row for comes back as it went in):');
  console.log('   type names:', JSON.stringify(T53.NEW.map(x => L57.vis(x.name))));
  console.log('   short names:', JSON.stringify(T53.NEW.map(x => L57.vis(x.short))));
  for (const t of ['S', ...T53.TOKENS]) for (const k of T53.KINDS) if (T53.subsOf(t, k).length) console.log('   sub-kinds', t, k + ':', JSON.stringify(T53.subsOf(t, k).map(x => L57.vis(T53.displayName(t, k, x.id)))));
  console.log('   measure words:', JSON.stringify(['track length', 'projection from wall', 'underside, from floor', 'depth from wall', 'drop below ceiling', 'drop below slab', 'height', 'width', 'width, left–right', 'depth, front–back',
    'from left corner, to centre', 'from right wall, to centre', 'from floor, to centre', 'from ceiling, to centre', 'from front wall, to centre', 'from back wall, to centre', 'from floor', 'from ceiling'].map(x => L57.vis(x))));
  console.log('   refusals (the text layer finds a sentence by its English, so "A … cannot go on the ceiling" is matched by v54\'s refuse.v360 row and the article of the others is left as it was -- the phone renders the T53 keys themselves, with no article under मराठी):');
  for (const s of ['A Grill cannot go on the ceiling', 'An AC unit cannot go on the floor', 'A Ceiling drop goes on the ceiling only', 'A Sanitary ware cannot go on the ceiling']) console.log('     ', JSON.stringify(s), '->', JSON.stringify(L57.vis(s)));
}

// ---- L57: what the page shows under मराठी for each line of the file given (a text it does not know comes back as it went in)
if (process.argv[3]) for (const s of fs.readFileSync(process.argv[3], 'utf8').split('\n').filter(Boolean)) console.log(JSON.stringify(s), '->', JSON.stringify(L57.vis(s)));
