// Static pictures of the proposed one-screen "mark and measure" (DESIGN-imagemeter.md, port63-im 8b672eb),
// and of today's 0.3.339 for comparison. Plain HTML + CSS + inline SVG, no script in the pages.
// 1 CSS px = 1 dp. The phone: 360 x 780, a 756 dp window under a 24 dp status bar.
'use strict';
const fs = require('fs'), path = require('path');
const OUT = process.argv[2];
if (!OUT) { console.error('usage: node gen.js <out dir>'); process.exit(2); }

// ------------------------------------------------------------------ the made-up wall, mm
const A = 3800, B = 2900;                 // Front wall, W x H
const COL = 340;                          // the screen's column, dp (SurveyPrepScreen: 10 dp at each side)
const M = 0.04 * Math.max(A, B);          // the squared picture's margin beyond the face: 4 % of its longer side (SurveyPrep.Square.screen)
const S = COL / (A + 2 * M), IH = (B + 2 * M) * S; // fit: the picture fills the width; 340 x 265.4 dp
const X = mm => (mm + M) * S, Y = mm => (B + M - mm) * S;
const WX0 = X(0), WX1 = X(A), WY0 = Y(B), WY1 = Y(0); // the wall's own edges in the picture
const r2 = v => Math.round(v * 100) / 100;

const TC = { D: '#ff8a3d', W: '#4dabf7', S: '#f783ac', E: '#ffd43b' };
const GOLD = '#e0b040', INK = '#14160f';

// marks, in the picture's dp (x from its left, y from its top)
const FD1 = { code: 'FD1', col: TC.D, x0: X(100), x1: X(1003), y0: Y(2104), y1: Y(0) };
const FS1 = { code: 'FS1', col: TC.S, x0: X(1428), x1: X(1650), y0: Y(1154), y1: Y(1052) };
const FW1 = { code: 'FW1', col: TC.W, x0: X(2151), x1: X(3060), y0: Y(1886), y1: Y(780) };
const QUAD = [[1120, 1700], [1950, 1700], [1950, 2330], [1120, 2720]].map(([x, y]) => [X(x), Y(y)]); // q0 BL, q1 BR, q2 TR, q3 TL
const FW2 = { code: 'FW2', col: TC.W, x0: X(1120), x1: X(1950), y0: Y(2720), y1: Y(1700), quad: QUAD };
const FE1 = { code: 'FE1', col: TC.E, cx: X(1539), cy: Y(300) };
const FE2 = { code: 'FE2', col: TC.E, cx: X(600), cy: Y(2550) };

const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

// ------------------------------------------------------------------ the wall, drawn flat (no photo)
function wallArt() {
  const d = FD1, w = FW1, s = FS1;
  const q = QUAD;
  const sl = (q[2][1] - q[3][1]) / (q[2][0] - q[3][0]), dv = 5 * Math.hypot(1, sl); // the sloping top, moved in by 5 dp
  const ins = [[q[0][0] + 5, q[0][1] - 5], [q[1][0] - 5, q[1][1] - 5], [q[2][0] - 5, q[2][1] + dv - 5 * sl], [q[3][0] + 5, q[3][1] + dv + 5 * sl]];
  const pts = a => a.map(p => r2(p[0]) + ',' + r2(p[1])).join(' ');
  const sock = m => `<rect x="${r2(m.cx - 4.2)}" y="${r2(m.cy - 4.2)}" width="8.4" height="8.4" rx="1" fill="#f1eee7" stroke="#a39e93" stroke-width=".6"/>`;
  return `
  <rect x="0" y="0" width="${COL}" height="${r2(IH)}" fill="#c3bcb0"/>
  <polygon points="0,0 ${COL},0 ${r2(WX1)},${r2(WY0)} ${r2(WX0)},${r2(WY0)}" fill="#e3dfd7"/>
  <polygon points="0,${r2(IH)} ${COL},${r2(IH)} ${r2(WX1)},${r2(WY1)} ${r2(WX0)},${r2(WY1)}" fill="#a79c8c"/>
  <rect x="${r2(WX0)}" y="${r2(WY0)}" width="${r2(WX1 - WX0)}" height="${r2(WY1 - WY0)}" fill="#d5cfc4"/>
  <rect x="${r2(WX0)}" y="${r2(WY1 - 7)}" width="${r2(WX1 - WX0)}" height="7" fill="#b9b1a4"/>
  <!-- door -->
  <rect x="${r2(d.x0)}" y="${r2(d.y0)}" width="${r2(d.x1 - d.x0)}" height="${r2(d.y1 - d.y0)}" fill="#8f7d69"/>
  <rect x="${r2(d.x0 + 5)}" y="${r2(d.y0 + 5)}" width="${r2(d.x1 - d.x0 - 10)}" height="${r2(d.y1 - d.y0 - 5)}" fill="#bfa98e"/>
  <rect x="${r2(d.x0 + 13)}" y="${r2(d.y0 + 14)}" width="${r2(d.x1 - d.x0 - 26)}" height="56" fill="none" stroke="#a8927a" stroke-width="1.2"/>
  <rect x="${r2(d.x0 + 13)}" y="${r2(d.y0 + 86)}" width="${r2(d.x1 - d.x0 - 26)}" height="76" fill="none" stroke="#a8927a" stroke-width="1.2"/>
  <circle cx="${r2(d.x1 - 11)}" cy="${r2(d.y0 + 78)}" r="2" fill="#6c5f51"/>
  <!-- window -->
  <rect x="${r2(w.x0)}" y="${r2(w.y0)}" width="${r2(w.x1 - w.x0)}" height="${r2(w.y1 - w.y0)}" fill="#eeebe4"/>
  <rect x="${r2(w.x0 + 5)}" y="${r2(w.y0 + 5)}" width="${r2(w.x1 - w.x0 - 10)}" height="${r2(w.y1 - w.y0 - 10)}" fill="#b9cdd8"/>
  <rect x="${r2((w.x0 + w.x1) / 2 - 1.6)}" y="${r2(w.y0 + 5)}" width="3.2" height="${r2(w.y1 - w.y0 - 10)}" fill="#eeebe4"/>
  <rect x="${r2(w.x0 - 3)}" y="${r2(w.y1)}" width="${r2(w.x1 - w.x0 + 6)}" height="3" fill="#c3bcaf"/>
  <!-- the window that is not a rectangle -->
  <polygon points="${pts(q)}" fill="#eeebe4"/>
  <polygon points="${pts(ins)}" fill="#b9cdd8"/>
  <!-- switch plate, two sockets -->
  <rect x="${r2(s.x0)}" y="${r2(s.y0)}" width="${r2(s.x1 - s.x0)}" height="${r2(s.y1 - s.y0)}" rx="1" fill="#f1eee7" stroke="#a39e93" stroke-width=".6"/>
  <rect x="${r2(s.x0 + 4)}" y="${r2(s.y0 + 2.6)}" width="3" height="4" fill="#cfcabf"/><rect x="${r2(s.x0 + 9)}" y="${r2(s.y0 + 2.6)}" width="3" height="4" fill="#cfcabf"/><rect x="${r2(s.x0 + 14)}" y="${r2(s.y0 + 2.6)}" width="3" height="4" fill="#cfcabf"/>
  ${sock(FE1)}${sock(FE2)}`;
}

// ------------------------------------------------------------------ drawing primitives (MarkDims / DimLayout / SurveyPrepDraw)
const CW = 0.602; // DejaVu Sans Mono's advance, em (Android's monospace: 0.6)
const MONO = "'DejaVu Sans Mono',monospace";

function plateSize(text, fs, extra = 0) { return { w: ([...text].length + extra) * CW * fs + 10, h: fs + 7 }; }

/** One figure's plate (MarkDims.style): 'read' white bold on black; 'est' grey, dashed; 'unk' amber, dashed; active: solid gold. */
function plate(o) {
  const fsz = o.fs || 10;
  const { w, h } = plateSize(o.text, fsz, (o.tri ? 1 : 0) + (o.check ? 1 : 0));
  const l = o.cx - w / 2, t = o.cy - h / 2;
  let st;
  if (o.active) st = { ink: INK, bold: true, fill: GOLD, fo: 1, out: '#ffffff', ow: 1.5, dash: false };
  else if (o.st === 'read') st = { ink: '#ffffff', bold: true, fill: '#000000', fo: 0.867, out: null };
  else if (o.st === 'unk') st = { ink: '#ffc46b', bold: false, fill: '#000000', fo: 0.533, out: '#ffc46b', ow: 1, dash: true };
  else st = { ink: '#cfd3cb', bold: false, fill: '#000000', fo: 0.533, out: '#cfd3cb', ow: 1, dash: true };
  let g = `<g${o.turned ? ` transform="rotate(-90 ${r2(o.cx)} ${r2(o.cy)})"` : ''}>`;
  g += `<rect x="${r2(l)}" y="${r2(t)}" width="${r2(w)}" height="${r2(h)}" rx="5" fill="${st.fill}" fill-opacity="${st.fo}"/>`;
  if (st.out) g += `<rect x="${r2(l)}" y="${r2(t)}" width="${r2(w)}" height="${r2(h)}" rx="5" fill="none" stroke="${st.out}" stroke-width="${st.ow}"${st.dash ? ' stroke-dasharray="3 2"' : ''}/>`;
  if (o.ring) g += `<rect x="${r2(l - 2.5)}" y="${r2(t - 2.5)}" width="${r2(w + 5)}" height="${r2(h + 5)}" rx="7" fill="none" stroke="${GOLD}" stroke-width="2"/>`;
  let tx = l + 5; const base = t + h - 5;
  if (o.tri) {
    const x0 = tx + 0.6, x1 = tx + CW * fsz - 0.9, yt = base - fsz * 0.74, yb = base - fsz * 0.02;
    g += o.tri === 'hollow'
      ? `<polygon points="${r2(x0 + .5)},${r2(yt + .9)} ${r2(x1 - .4)},${r2((yt + yb) / 2)} ${r2(x0 + .5)},${r2(yb - .9)}" fill="none" stroke="${st.ink}" stroke-width="1.2" stroke-linejoin="round"/>`
      : `<polygon points="${r2(x0)},${r2(yt)} ${r2(x1)},${r2((yt + yb) / 2)} ${r2(x0)},${r2(yb)}" fill="${st.ink}"/>`;
    tx += CW * fsz;
  }
  g += `<text x="${r2(tx)}" y="${r2(base)}" font-family="${MONO}" font-size="${fsz}" font-weight="${st.bold ? 700 : 400}" fill="${st.ink}">${esc(o.text)}${o.check ? `<tspan fill="${o.active ? INK : '#ffc46b'}" font-weight="700">!</tspan>` : ''}</text>`;
  return g + '</g>';
}
/** The box a plate covers, for breaking its line. */
function plateBox(o) {
  const fsz = o.fs || 10; const { w, h } = plateSize(o.text, fsz, (o.tri ? 1 : 0) + (o.check ? 1 : 0));
  return o.turned ? { l: o.cx - h / 2, r: o.cx + h / 2, t: o.cy - w / 2, b: o.cy + w / 2 } : { l: o.cx - w / 2, r: o.cx + w / 2, t: o.cy - h / 2, b: o.cy + h / 2 };
}

/** A small box's two figures on one compact plate, "≈222 × ≈102" (DimLayout How.COMPACT). */
function compact(cx, cy, a, b, fsz = 10) {
  const wa = plateSize(a.text, fsz).w, wb = plateSize(b.text, fsz).w, tw = 3 * CW * fsz, h = fsz + 7;
  const l = cx - (wa + tw + wb) / 2;
  return plate({ ...a, cx: l + wa / 2, cy, fs: fsz })
    + `<rect x="${r2(l + wa)}" y="${r2(cy - h / 2)}" width="${r2(tw)}" height="${h}" fill="#000" fill-opacity=".533"/>`
    + `<text x="${r2(l + wa)}" y="${r2(cy + h / 2 - 5)}" font-family="${MONO}" font-size="${fsz}" fill="#cfd3cb" xml:space="preserve"> × </text>`
    + plate({ ...b, cx: l + wa + tw + wb / 2, cy, fs: fsz });
}

function head(ax, ay, bx, by, len, fill, op) {
  const dx = bx - ax, dy = by - ay, L = Math.hypot(dx, dy) || 1, ux = dx / L, uy = dy / L, w = len * 0.4;
  return `<polygon points="${r2(ax)},${r2(ay)} ${r2(ax + ux * len - uy * w)},${r2(ay + uy * len + ux * w)} ${r2(ax + ux * len + uy * w)},${r2(ay + uy * len - ux * w)}" fill="${fill}" fill-opacity="${op}"/>`;
}
/** A dimension's line with an arrowhead at each end, broken where its plate sits on it; gold and heavy when active. */
function dline(x1, y1, x2, y2, o = {}) {
  const segs = [];
  const b = o.gap;
  if (b && Math.abs(x1 - x2) < 0.01) { const top = Math.min(y1, y2), foot = Math.max(y1, y2); if (b.t > top) segs.push([x1, top, x1, b.t]); if (b.b < foot) segs.push([x1, b.b, x1, foot]); }
  else if (b && Math.abs(y1 - y2) < 0.01) { const le = Math.min(x1, x2), ri = Math.max(x1, x2); if (b.l > le) segs.push([le, y1, b.l, y1]); if (b.r < ri) segs.push([b.r, y1, ri, y1]); }
  else segs.push([x1, y1, x2, y2]);
  const ln = (c, w, op) => segs.map(s => `<line x1="${r2(s[0])}" y1="${r2(s[1])}" x2="${r2(s[2])}" y2="${r2(s[3])}" stroke="${c}" stroke-width="${w}" stroke-opacity="${op}"/>`).join('');
  let g = '';
  if (o.active) g += ln('#000', 5.5, 0.8) + ln(GOLD, 3, 1) + head(x1, y1, x2, y2, 6, GOLD, 1) + head(x2, y2, x1, y1, 6, GOLD, 1);
  else g += ln('#fff', 1, 0.8) + head(x1, y1, x2, y2, 4, '#fff', 0.8) + head(x2, y2, x1, y1, 4, '#fff', 0.8);
  if (o.leader) g += `<line x1="${r2(o.leader[0])}" y1="${r2(o.leader[1])}" x2="${r2(o.leader[2])}" y2="${r2(o.leader[3])}" stroke="#fff" stroke-width="1" stroke-opacity=".8"/>`;
  return g;
}

const codeText = (t, x, y, col) => `<text x="${r2(x)}" y="${r2(y)}" font-family="Inter,'DejaVu Sans',sans-serif" font-size="13" font-weight="700" fill="${col}" stroke="#000" stroke-opacity=".8" stroke-width="3" paint-order="stroke" stroke-linejoin="round">${esc(t)}</text>`;

/** A box mark: filled 0x22, outlined in its type's colour (3.5 when picked), its code inside at the top-left. */
function boxMark(m, o = {}) {
  let g = '';
  if (m.quad && !o.asBox) {
    const pts = m.quad.map(p => r2(p[0]) + ',' + r2(p[1])).join(' ');
    g += `<polygon points="${pts}" fill="${m.col}" fill-opacity=".133"/><polygon points="${pts}" fill="none" stroke="${m.col}" stroke-width="${o.sel ? 3.5 : 2.5}" stroke-linejoin="miter"/>`;
  } else {
    g += `<rect x="${r2(m.x0)}" y="${r2(m.y0)}" width="${r2(m.x1 - m.x0)}" height="${r2(m.y1 - m.y0)}" fill="${m.col}" fill-opacity=".133"/>`
      + `<rect x="${r2(m.x0)}" y="${r2(m.y0)}" width="${r2(m.x1 - m.x0)}" height="${r2(m.y1 - m.y0)}" fill="none" stroke="${m.col}" stroke-width="${o.sel ? 3.5 : 2.5}"/>`;
  }
  if (!o.noCode) g += codeText(m.code + (o.done ? '✓' : ''), m.x0 + 6, m.y0 + 16, m.col);
  return g;
}
/** The eight resize tabs of the held mark: white squares of 10 dp ringed in its colour. */
function tabs(m) {
  const xm = (m.x0 + m.x1) / 2, ym = (m.y0 + m.y1) / 2;
  return [[m.x0, m.y0], [m.x1, m.y0], [m.x1, m.y1], [m.x0, m.y1], [xm, m.y0], [xm, m.y1], [m.x0, ym], [m.x1, ym]]
    .map(([x, y]) => `<rect x="${r2(x - 5)}" y="${r2(y - 5)}" width="10" height="10" fill="#fff" stroke="${m.col}" stroke-width="2"/>`).join('');
}
/** A point: its 12 dp spot, two dots for an Electric point, its code beside it. */
function spot(m) {
  const r = 6, k = r * 0.62;
  return `<circle cx="${r2(m.cx)}" cy="${r2(m.cy)}" r="${r}" fill="${m.col}" stroke="#000" stroke-opacity=".667" stroke-width="1.6"/>`
    + `<circle cx="${r2(m.cx - k * 0.38)}" cy="${r2(m.cy)}" r=".85" fill="${INK}"/><circle cx="${r2(m.cx + k * 0.38)}" cy="${r2(m.cy)}" r=".85" fill="${INK}"/>`
    + codeText(m.code, m.cx + r + 4, m.cy - r - 2, m.col);
}
/** H1's gold frame, 3 dp outside the mark being measured, over a dark casing. */
function goldFrame(m) {
  const a = { x: m.x0 - 3, y: m.y0 - 3, w: m.x1 - m.x0 + 6, h: m.y1 - m.y0 + 6 };
  const rc = (c, w, op) => `<rect x="${r2(a.x)}" y="${r2(a.y)}" width="${r2(a.w)}" height="${r2(a.h)}" fill="none" stroke="${c}" stroke-width="${w}" stroke-opacity="${op}"/>`;
  return rc('#000', 6.5, 0.8) + rc(GOLD, 3.5, 1);
}
/** The picked item's code as a chip: the code and ▾ (slice I2). */
function codeChip(m, x, y) {
  const w = 50, h = 22;
  return `<rect x="${r2(x)}" y="${r2(y)}" width="${w}" height="${h}" rx="11" fill="#000" fill-opacity=".86" stroke="${m.col}" stroke-width="1.5"/>`
    + `<text x="${r2(x + 8)}" y="${r2(y + 15.6)}" font-family="Inter,'DejaVu Sans',sans-serif" font-size="13" font-weight="700" fill="${m.col}">${m.code}</text>`
    + `<polygon points="${r2(x + 37.2)},${r2(y + 8.8)} ${r2(x + 43.6)},${r2(y + 8.8)} ${r2(x + 40.4)},${r2(y + 13.8)}" fill="${m.col}"/>`;
}

// ------------------------------------------------------------------ what each state draws on the wall
const W1 = { xm: (FW1.x0 + FW1.x1) / 2, ym: (FW1.y0 + FW1.y1) / 2 };
const CORNER_Y = W1.ym + 31.2;           // DimLayout.CORNER_UNDER_FS x 12 units under mid-height
const FLOOR_X = W1.xm - 19.2;            // DimLayout.FLOOR_LEFT_FS x 12 units left of centre

/** The marks nobody has picked, proposed (I4): size on the edges, no lines. [door]: the door is measured. */
function othersProposed(o = {}) {
  let g = '';
  // door (measured: white bold on black, and its ✓)
  g += boxMark(FD1, { done: true });
  g += plate({ cx: FD1.x0 + 0.75 * (FD1.x1 - FD1.x0), cy: FD1.y0, text: '898', st: 'read' });
  g += plate({ cx: FD1.x1, cy: (FD1.y0 + FD1.y1) / 2, text: '2095', st: 'read', turned: true });
  // the four-corner window: each side's figure on its own side
  g += boxMark(FW2);
  const q = QUAD;
  // NO PHOTO ESTIMATE (Amit, 16 Nov 2026): two of its sides are read and shown; the two not yet read show nothing
  g += plate({ cx: (q[0][0] + q[1][0]) / 2, cy: q[0][1], text: '830', st: 'read' });
  g += plate({ cx: q[1][0], cy: (q[1][1] + q[2][1]) / 2, text: '630', st: 'read', turned: true });
  // switch plate: too small for two plates -- one compact plate beside it
  g += boxMark(FS1);
  // points: a spot and its code, no size
  g += spot(FE1) + spot(FE2);
  if (!o.noWindow) {
    g += boxMark(FW1);
  }
  return g;
}

/** FW1's position figures, each on its white line with arrowheads, as today. [hand]: the mark shows its tabs. */
function windowPosition(hand) {
  let g = '';
  const rp = { cx: (FW1.x1 + WX1) / 2, cy: CORNER_Y, text: '?', fs: 12 };
  g += dline(FW1.x1, CORNER_Y, WX1, CORNER_Y, { gap: plateBox(rp) }) + plate(rp);
  const sp = { cx: FLOOR_X, cy: (FW1.y1 + WY1) / 2, text: '?', fs: 12 };
  g += dline(FLOOR_X, FW1.y1, FLOOR_X, WY1, { gap: plateBox(sp) }) + plate(sp);
  const gp = hand ? 8 : 4, ps = plateSize('⊥ ?', 12);
  g += plate({ cx: FW1.x1 + gp + ps.w / 2, cy: FW1.y1 + gp + ps.h / 2, text: '⊥ ?', fs: 12 });
  return g;
}

/**
 * The wall with its marks for one state:
 *  'none'  nothing picked;
 *  'held'  FW1 held (tabs, hand-anchored plates), its width gold, the laser not linked (hollow glyph);
 *  'walk'  the gold came by itself: FW1 picked without handles, width read, height gold;
 *  'type'  as 'walk' with the keypad's digits on the gold plate.
 */
function wallMarks(state) {
  if (state === 'none') return { pre: '', body: othersProposed(), hole: null };
  const frame = goldFrame(FW1);
  let near = '';
  if (state === 'held') {
    near += boxMark(FW1, { sel: true, noCode: true });
    // width: gold, on its line 6 dp over the edge, the plate hand-anchored clear of the tabs
    const wp = { cx: W1.xm, cy: FW1.y0 - 22.1, text: '?', fs: 12, active: true, tri: 'hollow' };
    near += dline(FW1.x0, FW1.y0 - 6, FW1.x1, FW1.y0 - 6, { active: true });
    // height: its line 6 dp right of the edge, the plate hand-anchored right of it
    const hs = plateSize('?', 12);
    near += dline(FW1.x1 + 6, FW1.y0, FW1.x1 + 6, FW1.y1);
    near += windowPosition(true);
    near += tabs(FW1);
    near += plate(wp);
    near += plate({ cx: FW1.x1 + 14 + hs.w / 2, cy: W1.ym - 0.5, text: '?', fs: 12 });
    near += codeChip(FW1, FW1.x0, FW1.y0 - 31.6 - 3 - 22);
  } else {
    near += boxMark(FW1, { sel: true, noCode: true });
    const typed = state === 'type';
    const hp = { cx: FW1.x1, cy: W1.ym, text: typed ? '110' : '?', fs: 12, active: true, tri: 'solid', turned: true };
    // the edge is the line: the side being measured is gold and heavy
    near += dline(FW1.x1, FW1.y0, FW1.x1, FW1.y1, { active: true, gap: plateBox(hp) });
    near += windowPosition(false);
    near += plate({ cx: W1.xm, cy: FW1.y0, text: '912', st: 'read', fs: 12 });
    near += plate(hp);
    near += codeChip(FW1, FW1.x0, FW1.y0 - 21 - 22);
  }
  return {
    pre: frame,
    body: `<g opacity=".75">${othersProposed({ noWindow: true })}</g>` + near,
    hole: { l: FW1.x0 - 6, t: FW1.y0 - 6, r: FW1.x1 + 6, b: FW1.y1 + 6 },
  };
}

/** Today, 0.3.339, 3 · Measure: W and H of every mark on lines 6 dp outside its edges; FW1 picked, its height active. */
function wallMarksToday() {
  const frame = goldFrame(FW1);
  let far = '';
  far += boxMark(FD1, { done: true });
  let p = { cx: (FD1.x0 + FD1.x1) / 2, cy: FD1.y0 - 6, text: '898', st: 'read' };
  far += dline(FD1.x0, FD1.y0 - 6, FD1.x1, FD1.y0 - 6, { gap: plateBox(p) }) + plate(p);
  p = { cx: FD1.x1 + 6, cy: (FD1.y0 + FD1.y1) / 2, text: '2095', st: 'read', turned: true };
  far += dline(FD1.x1 + 6, FD1.y0, FD1.x1 + 6, FD1.y1, { gap: plateBox(p) }) + plate(p);
  // today a window that is not a rectangle can only be marked by the rectangle round it
  far += boxMark(FW2, { asBox: true });
  p = { cx: (FW2.x0 + FW2.x1) / 2, cy: FW2.y0 - 6, text: '≈830' };
  far += dline(FW2.x0, FW2.y0 - 6, FW2.x1, FW2.y0 - 6, { gap: plateBox(p) }) + plate(p);
  p = { cx: FW2.x1 + 6, cy: (FW2.y0 + FW2.y1) / 2, text: '≈1020', turned: true };
  far += dline(FW2.x1 + 6, FW2.y0, FW2.x1 + 6, FW2.y1, { gap: plateBox(p) }) + plate(p);
  far += boxMark(FS1) + compact((FS1.x0 + FS1.x1) / 2, FS1.y0 - 10 - 8.5, { text: '≈222' }, { text: '≈102' });
  far += spot(FE1) + spot(FE2);
  let near = boxMark(FW1, { sel: true });
  p = { cx: W1.xm, cy: FW1.y0 - 6, text: '912', st: 'read', fs: 12 };
  near += dline(FW1.x0, FW1.y0 - 6, FW1.x1, FW1.y0 - 6, { gap: plateBox(p) }) + plate(p);
  const hp = { cx: FW1.x1 + 6, cy: W1.ym, text: '≈1106', fs: 12, active: true, tri: 'solid', turned: true };
  near += dline(FW1.x1 + 6, FW1.y0, FW1.x1 + 6, FW1.y1, { active: true, gap: plateBox(hp) });
  // the corner line's plate has no room on its line beside the gold plate: it steps off it, on a leader
  const rx = (FW1.x1 + WX1) / 2;
  near += dline(FW1.x1, CORNER_Y, WX1, CORNER_Y, { leader: [rx, CORNER_Y, rx, CORNER_Y + 9.5] });
  near += plate({ cx: rx, cy: CORNER_Y + 19, text: '≈740', fs: 12 });
  const sp = { cx: FLOOR_X, cy: (FW1.y1 + WY1) / 2, text: '≈780', fs: 12 };
  near += dline(FLOOR_X, FW1.y1, FLOOR_X, WY1, { gap: plateBox(sp) }) + plate(sp);
  const ps = plateSize('⊥ ?', 12);
  near += plate({ cx: FW1.x1 - 4 - ps.w / 2, cy: FW1.y1 + 4 + ps.h / 2, text: '⊥ ?', st: 'unk', fs: 12 });
  near += plate(hp);
  return { pre: frame, body: `<g opacity=".75">${far}</g>` + near, hole: { l: FW1.x0 - 6, t: FW1.y0 - 6, r: FW1.x1 + 6, b: FW1.y1 + 6 } };
}

/** The photo box: black, 12 dp corners, the face fitted whole and centred, lifted by [lift] under a panel. */
function photo(boxH, marks, o = {}) {
  const wy = (boxH - IH) / 2 - (o.lift || 0);
  const id = 'c' + Math.round(boxH) + (o.id || '');
  const h = marks.hole;
  const dim = h ? `<path fill-rule="evenodd" fill="#000" fill-opacity=".349" d="M0 0H${COL}V${boxH}H0Z M${r2(h.l)} ${r2(h.t + wy)}H${r2(h.r)}V${r2(h.b + wy)}H${r2(h.l)}Z"/>` : '';
  return `<svg class="photo" width="${COL}" height="${boxH}" viewBox="0 0 ${COL} ${boxH}">
  <defs><clipPath id="${id}"><rect x="0" y="0" width="${COL}" height="${boxH}" rx="12"/></clipPath></defs>
  <g clip-path="url(#${id})">
    <rect x="0" y="0" width="${COL}" height="${boxH}" fill="#000"/>
    <g transform="translate(0 ${r2(wy)})">${wallArt()}
      <rect x="${r2(WX0)}" y="${r2(WY0)}" width="${r2(WX1 - WX0)}" height="${r2(WY1 - WY0)}" fill="none" stroke="#fff" stroke-opacity=".6" stroke-width="1.5" stroke-dasharray="6 5"/>
    </g>
    ${dim}
    <g transform="translate(0 ${r2(wy)})">${marks.pre}${marks.body}</g>
  </g></svg>`;
}

// ------------------------------------------------------------------ icons (SurveyPrepTools.drawMarkIcon / drawNewIcon)
function icon(name, c, size) {
  const n = (b) => `<svg width="${size}" height="${size}" viewBox="0 0 1 1">${b}</svg>`;
  const g = (b) => `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke-linecap="round" stroke-linejoin="round">${b}</svg>`;
  const sw = 0.09, ink = INK;
  switch (name) {
    case 'Window': return n(`<rect x=".12" y=".12" width=".76" height=".76" fill="none" stroke="${c}" stroke-width="${sw}"/><line x1=".5" y1=".12" x2=".5" y2=".88" stroke="${c}" stroke-width="${sw}"/><line x1=".12" y1=".5" x2=".88" y2=".5" stroke="${c}" stroke-width="${sw}"/>`);
    case 'Door': return n(`<rect x=".28" y=".06" width=".44" height=".88" fill="none" stroke="${c}" stroke-width="${sw}"/><circle cx=".62" cy=".52" r=".06" fill="${c}"/>`);
    case 'Niche': return n(`<path d="M.2 .4A.3 .3 0 0 1 .8 .4" fill="none" stroke="${c}" stroke-width="${sw}"/><line x1=".2" y1=".4" x2=".2" y2=".92" stroke="${c}" stroke-width="${sw}"/><line x1=".8" y1=".4" x2=".8" y2=".92" stroke="${c}" stroke-width="${sw}"/>`);
    case 'Loft': return n(`<rect x=".1" y=".1" width=".8" height=".8" fill="none" stroke="${c}" stroke-width="${sw}"/><rect x=".1" y=".1" width=".8" height=".28" fill="${c}"/>`);
    case 'Column': return n(`<rect x=".36" y=".05" width=".28" height=".9" fill="${c}"/>`);
    case 'Beam': return n(`<rect x=".05" y=".36" width=".9" height=".28" fill="${c}"/>`);
    case 'Opening': return n(`<rect x=".14" y=".1" width=".72" height=".8" fill="none" stroke="${c}" stroke-width="${sw}" stroke-dasharray=".12 .08"/>`);
    case 'Electric point': return n(`<circle cx=".5" cy=".5" r=".4" fill="none" stroke="${c}" stroke-width="${sw}"/><circle cx=".36" cy=".5" r=".07" fill="${c}"/><circle cx=".64" cy=".5" r=".07" fill="${c}"/>`);
    case 'Bulb point': return n(`<path d="M.2997 .5457A.26 .26 0 1 1 .7003 .5457" fill="none" stroke="${c}" stroke-width="${sw}"/><rect x=".36" y=".62" width=".28" height=".1" fill="${c}"/><rect x=".38" y=".76" width=".24" height=".08" fill="${c}"/><rect x=".43" y=".87" width=".14" height=".06" fill="${c}"/>`);
    case 'Switch plate': return n(`<rect x=".08" y=".28" width=".84" height=".44" fill="none" stroke="${c}" stroke-width="${sw}"/><rect x=".26" y=".38" width=".14" height=".24" fill="${c}"/><rect x=".6" y=".38" width=".14" height=".24" fill="${c}"/>`);
    case 'Fan': return n(`<circle cx=".5" cy=".5" r=".08" fill="${c}"/>` + [0, 1, 2].map(i => `<ellipse cx=".74" cy=".5" rx=".2" ry=".08" fill="${c}" transform="rotate(${-90 + 120 * i} .5 .5)"/>`).join(''));
    case 'Other': return n([0, 1, 2].map(i => `<circle cx="${0.22 + 0.28 * i}" cy=".5" r=".08" fill="${c}"/>`).join(''));
    case 'Note': return g(`<circle cx="12" cy="12" r="9" fill="${c}"/><circle cx="12" cy="12" r="9" stroke="${ink}" stroke-width="2"/><g transform="rotate(-45 12 12)"><rect x="7" y="10.4" width="8" height="3.2" fill="${ink}"/><polygon points="15,10.4 18,12 15,13.6" fill="${ink}"/></g>`);
    case 'Grill': return g(`<rect x="3" y="3" width="18" height="18" stroke="${c}" stroke-width="1.8"/>` + [7.5, 12, 16.5].map(x => `<line x1="${x}" y1="3" x2="${x}" y2="21" stroke="${c}" stroke-width="1.8"/>`).join('') + `<line x1="3" y1="12" x2="21" y2="12" stroke="${c}" stroke-width="1.8"/>`);
    case 'AC unit': return g(`<rect x="2" y="5" width="20" height="9" rx="2.5" stroke="${c}" stroke-width="2"/><line x1="5" y1="11" x2="19" y2="11" stroke="${c}" stroke-width="2"/>` + [7, 12, 17].map(x => `<path d="M${x} 16Q${x - 2} 18.5 ${x} 21" stroke="${c}" stroke-width="1.6"/>`).join(''));
    case 'Plumbing point': return g(`<path d="M12 3V9M5 9H19M19 9V13" stroke="${c}" stroke-width="2.2"/><path d="M12 14Q8.5 18.5 12 21Q15.5 18.5 12 14Z" fill="${c}"/>`);
    case 'Curtain track': return g(`<line x1="2" y1="4" x2="22" y2="4" stroke="${c}" stroke-width="2.2"/>` + [0, 1, 2, 3].map(i => { const x = 4 + i * 4.6; return `<path d="M${x} 4C${x + 2.4} 10 ${x - 1.4} 15 ${x + 1} 21" stroke="${c}" stroke-width="1.6"/>`; }).join(''));
    case 'Carpentry unit': return g(`<rect x="4" y="2" width="16" height="20" stroke="${c}" stroke-width="1.9"/><line x1="12" y1="2" x2="12" y2="22" stroke="${c}" stroke-width="1.9"/><rect x="9" y="10" width="1.8" height="4" fill="${c}"/><rect x="13.2" y="10" width="1.8" height="4" fill="${c}"/>`);
    case 'Low-voltage point': {
      const arc = (rr, a0, sweep) => { const p = a => [12 + rr * Math.cos(a * Math.PI / 180), 12 + rr * Math.sin(a * Math.PI / 180)]; const s = p(a0), e = p(a0 + sweep); return `<path d="M${r2(s[0])} ${r2(s[1])}A${rr} ${rr} 0 0 1 ${r2(e[0])} ${r2(e[1])}" stroke="${c}" stroke-width="2"/>`; };
      return g(`<circle cx="12" cy="12" r="3" fill="${c}"/>` + [6, 9.5].map(rr => arc(rr, -144, 108) + arc(rr, 36, 108)).join(''));
    }
    case 'Sanitary ware': return g(`<path d="M3 9H21Q20 19 12 19Q4 19 3 9Z" stroke="${c}" stroke-width="2"/><path d="M12 3V6.5M9 3.5H15" stroke="${c}" stroke-width="2"/>`);
    case 'Defect': return g(`<path d="M9 2L13 8L8 12L14 17L11 22M13 8L18 10" stroke="${c}" stroke-width="2"/>`);
    case 'Tape': return g(`<rect x="2" y="5" width="13" height="14" rx="3" stroke="${c}" stroke-width="2"/><circle cx="8.5" cy="12" r="3" stroke="${c}" stroke-width="2"/><rect x="14" y="15" width="8" height="4" fill="${c}"/><rect x="21" y="13" width="2" height="6" fill="${c}"/><rect x="16" y="15" width="1" height="2" fill="${ink}"/><rect x="18.5" y="15" width="1" height="2" fill="${ink}"/>`);
    case 'Line': return g(`<line x1="5.5" y1="18.5" x2="18.5" y2="5.5" stroke="${c}" stroke-width="2" stroke-linecap="butt"/><polygon points="20,4 13.64,6.12 17.88,10.36" fill="${c}"/><polygon points="4,20 10.36,17.88 6.12,13.64" fill="${c}"/>`);
    case 'plus': return g(`<path d="M12 4V20M4 12H20" stroke="${c}" stroke-width="2.4"/>`);
    case 'dots': return g(`<circle cx="5" cy="12" r="2" fill="${c}"/><circle cx="12" cy="12" r="2" fill="${c}"/><circle cx="19" cy="12" r="2" fill="${c}"/>`);
    case 'next': return g(`<polygon points="8,5 18,12 8,19" fill="${c}"/>`);
    default: return n(`<circle cx=".5" cy=".5" r=".3" fill="none" stroke="${c}" stroke-width="${sw}"/>`);
  }
}

// ------------------------------------------------------------------ the screen's rows
const CSS = `
*{box-sizing:border-box}
html,body{margin:0;padding:0;background:#000}
body{font-family:Inter,'DejaVu Sans',sans-serif;color:#ECEEE8;-webkit-font-smoothing:antialiased}
.mono{font-family:${MONO}}
.phone{position:relative;width:360px;height:780px;background:#111311;overflow:hidden}
.a{position:absolute}
.status{left:0;top:0;width:360px;height:24px;background:#000;color:#cfd3cb;font-size:11.5px;font-weight:500;display:flex;align-items:center;padding:0 14px;justify-content:space-between}
.status .ic{display:flex;gap:6px;align-items:center}
.status .bat{width:19px;height:9px;border:1.2px solid #cfd3cb;border-radius:2px;position:relative}
.status .bat:before{content:'';position:absolute;left:1px;top:1px;bottom:1px;width:11px;background:#cfd3cb}
.status .sig{width:0;height:0;border-left:10px solid transparent;border-bottom:10px solid #cfd3cb}
.band{left:10px;width:340px;height:20px;display:flex;align-items:center;gap:6px;padding:0 6px;font-size:11px;line-height:20px;letter-spacing:.3px;white-space:nowrap;overflow:hidden}
.pips{display:flex;gap:2px}.pip{width:8px;height:3px;border-radius:2px;background:#555A50}.pip.on{background:#E0B040}
.band .hd{color:#E0B040;font-weight:700}.band .rs{color:#9AA096;overflow:hidden;text-overflow:ellipsis;flex:1}
.bar{left:10px;width:340px;height:40px;display:flex;align-items:center}
.bar .back{padding:2px 8px;display:flex}.bar .ttl{flex:1;min-width:0}
.bar .t1{font-weight:700;font-size:15px;line-height:18px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.bar .t2{color:#7DFF8A;font-size:10px;line-height:12px}
.bar .undo{padding:2px 6px;display:flex}.bar .menu{padding:2px 8px;display:flex}
.bar .save{margin-left:2px;height:32px;border-radius:16px;background:#E0B040;color:#1A1A12;font-weight:700;font-size:13px;padding:0 12px;display:flex;align-items:center;white-space:nowrap}
.laser{width:44px;height:28px;border-radius:14px;border:1.5px solid #3A3E35;display:flex;align-items:center;justify-content:center;gap:3px;font-family:${MONO};font-weight:700;font-size:11px;color:#7D8278;margin-right:2px}
.laser i{width:7px;height:7px;border-radius:50%;background:#555A50;display:block}
.laser.ready{border-color:#E0B040;background:rgba(224,176,64,.16);color:#E0B040}.laser.ready i{background:#E0B040}
.tabs{left:10px;width:340px}
.trow{height:32px;display:flex;border-bottom:1px solid #3A3E35}
.tab{flex:1;position:relative;display:flex;align-items:center;justify-content:center;font-size:11px;line-height:14px;letter-spacing:.5px;font-weight:500;color:#AEB3AA;white-space:nowrap}
.tab.on{color:#fff;font-weight:700}
.tab.on:after{content:'';position:absolute;bottom:0;left:12%;width:76%;height:3px;border-radius:3px 3px 0 0;background:#E0B040}
.tab .ok{color:#7DFF8A;font-weight:700}.tab .n{color:#FFC46B;font-weight:700}
.strip{left:10px;width:340px;height:28px;padding:2px 2px 0;display:flex;align-items:center}
.strip .lead{color:#9AA096;font-size:11px;line-height:14px;white-space:nowrap;padding-right:6px}
.strip .join{flex:1;height:1.5px;background:#4A4E45;min-width:6px}
.step{height:26px;padding:0 4px;display:flex;align-items:center;gap:5px}
.step .c{width:20px;height:20px;border-radius:50%;border:1.5px solid #7E847A;display:flex;align-items:center;justify-content:center;font-size:11px;line-height:11px;font-weight:700;color:#AEB3AA}
.step.past .c{border-color:#B9A35A;color:#D9CFA5}
.step.on .c{background:#E0B040;border-color:#E0B040;color:#14160F}
.step .nm{color:#E0B040;font-size:12px;line-height:16px;font-weight:700;white-space:nowrap}
.cap{left:10px;width:340px;height:22px;padding:3px 0 3px 6px;font-size:13px;line-height:16px;font-weight:600;white-space:nowrap}
.photo{position:absolute;left:10px;display:block}
.ov{font-size:12px;font-weight:600;color:#fff;background:rgba(0,0,0,.667);border-radius:6px;padding:5px 8px;line-height:14px}
.ovcol{right:16px;display:flex;flex-direction:column;align-items:flex-end;gap:4px}
.trow2{left:10px;width:340px;height:50px;display:flex}
.slot{height:50px;border-radius:12px;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:4px 0 3px}
.slot .lb{margin-top:2px;font-size:9.5px;line-height:12px;letter-spacing:-.2px;font-weight:500;color:#AEB3AA;white-space:nowrap}
.slot.on .lb{color:#fff}
.slot svg{display:block}
.toolchip{left:16px;min-height:32px;border-radius:16px;background:rgba(0,0,0,.82);padding-left:12px;display:flex;align-items:center;font-size:12px;line-height:15px;color:#fff;white-space:nowrap}
.toolchip .x{width:32px;height:32px;display:flex;align-items:center;justify-content:center}
.mbar{left:10px;width:340px;height:56px;background:#2A2410;border:1.5px solid #E0B040;padding:0 6px;display:flex;align-items:center;gap:4px}
.mbar .cell{width:52px;text-align:center;color:#E0B040;font-size:10px;line-height:12px;font-weight:700;padding:4px 0}
.mbar .arr{width:36px;height:100%;display:flex;align-items:center;justify-content:center}
.mbar .lines{flex:1;min-width:0}
.mbar .what{color:#fff;font-size:12px;line-height:14px;font-weight:600}
.mbar .prog{color:#9AA096;font-size:10px;line-height:12px;letter-spacing:-.35px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.mbar .fbox{width:64px;height:32px;border-radius:6px;background:#E0B040;border:1.5px solid #fff;display:flex;align-items:center;justify-content:flex-end;padding:0 5px;font-family:${MONO};font-size:13px;font-weight:700;color:#14160F}
.mbar .d2{width:40px;height:32px;border-radius:8px;background:#3A1512;border:1px solid #FF5A4A;display:flex;align-items:center;justify-content:center;gap:3px;font-family:${MONO};font-size:12px;font-weight:700;color:#FFB3A8}
.mbar .d2 i{width:7px;height:7px;border-radius:50%;background:#FF3B2F;display:block}
.pad{left:10px;width:340px;height:176px;display:flex}
.pad .l{flex:3;display:flex;flex-direction:column}.pad .r{flex:1;display:flex;flex-direction:column}
.pad .rw{flex:1;display:flex}
.key{flex:1;margin:1.5px;border-radius:7px;background:#2C2F28;display:flex;align-items:center;justify-content:center;color:#fff;font-family:${MONO};font-size:18px;font-weight:700}
.key.q{background:#23261F}.key.w{font-family:Inter,sans-serif;font-size:13px}
.key.g{background:#E0B040;color:#1A1A12;font-family:Inter,sans-serif;font-size:13px;gap:4px}
.key.f2{flex:2}
.scrim{left:0;top:24px;width:360px;background:rgba(0,0,0,.533)}
.sheet{left:0;width:360px;bottom:0;background:#1E211B;border-radius:16px 16px 0 0;padding:0 12px 8px;overflow:hidden}
.grip{height:16px;display:flex;align-items:center;justify-content:center}.grip i{width:32px;height:4px;border-radius:2px;background:#555A50;display:block}
.shead{height:40px;display:flex;align-items:center;gap:8px}
.shead .nm{flex:1;font-size:14px;line-height:20px;white-space:nowrap}
.shead .nm b{color:#E0B040;font-family:${MONO}}
.shead .tr{display:flex;align-items:center;gap:6px;color:#CFD3CB;font-size:12px;line-height:16px;white-space:nowrap}
.dot{width:8px;height:8px;border-radius:50%;display:inline-block;flex:none}
.done{height:32px;border-radius:16px;background:#E0B040;color:#1A1A12;font-weight:700;font-size:13px;padding:0 16px;display:flex;align-items:center}
.acts{height:44px;display:flex;align-items:center;gap:4px}
.act{flex:1;height:36px;border-radius:8px;background:#2A2D25;border:1px solid #3A3E35;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:500;color:#ECEEE8;white-space:nowrap}
.act.del{flex:none;background:#000;border:1.5px solid #C9504C;color:#FF8A8A;font-weight:700}
.blk{margin-top:6px}
.lab{color:#9AA096;font-size:11px;line-height:16px;letter-spacing:.6px;font-weight:700;display:flex}
.now{margin-top:4px;display:flex;align-items:center;gap:6px;font-size:12px;line-height:20px;color:#CFD3CB}.now b{color:#ECEEE8}
.gline{margin-top:4px;color:#9AA096;font-size:12px;line-height:16px}
.nbox{margin-top:4px;height:48px;border-radius:8px;background:#111311;border:1px solid #555A50;padding:6px 10px;color:#7D8278;font-size:13px;line-height:18px}
.aud{margin-top:4px;height:32px;border-radius:8px;border:1px solid #3A3E35;display:flex;overflow:hidden}
.aud div{flex:1;display:flex;align-items:center;justify-content:center;font-size:12px;line-height:16px;font-weight:500;color:#CFD3CB}
.aud div.on{background:#3A3E35;border:1.5px solid #E0B040;border-radius:8px;color:#fff;font-weight:700}
.grp{color:#9AA096;font-size:11px;line-height:16px;letter-spacing:.3px;font-weight:700;margin-top:10px}
.tiles{margin-top:4px;display:flex;gap:6px}
.tile{flex:1;height:54px;border-radius:10px;background:#2A2D25;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;font-size:11px;line-height:13px;color:#ECEEE8;white-space:nowrap}
.tile.ghost{background:none;border:none}
.mrow{height:44px;padding:0 4px;display:flex;align-items:center;gap:4px}
.mrow .tl{width:80px;font-size:11px;line-height:13px;font-weight:600}
.mt{width:48px;height:40px;border-radius:10px;background:#2A2D25;border:1.5px solid;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:2px 1px}
.mt .tk{font-size:9px;line-height:10px;letter-spacing:-.2px;font-weight:700}
.mt .sn{font-size:10px;line-height:11px;letter-spacing:-.3px;color:#ECEEE8;white-space:nowrap}
`;

const glyph = {
  back: `<svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="#ECEEE8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 10H4M9 5L4 10L9 15"/></svg>`,
  undo: `<svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="#ECEEE8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 8H12A4.5 4.5 0 0 1 12 17H9"/><path d="M8 4.5L4.5 8L8 11.5"/></svg>`,
  kebab: `<svg width="8" height="20" viewBox="0 0 8 20"><circle cx="4" cy="4" r="1.8" fill="#ECEEE8"/><circle cx="4" cy="10" r="1.8" fill="#ECEEE8"/><circle cx="4" cy="16" r="1.8" fill="#ECEEE8"/></svg>`,
  prev: `<svg width="14" height="24" viewBox="0 0 14 24" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><path d="M10 5L4 12L10 19"/></svg>`,
  nxt: `<svg width="14" height="24" viewBox="0 0 14 24" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5L10 12L4 19"/></svg>`,
  bksp: `<svg width="24" height="18" viewBox="0 0 24 18" fill="none" stroke="#fff" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round"><path d="M8 2H21A1.5 1.5 0 0 1 22.5 3.5V14.5A1.5 1.5 0 0 1 21 16H8L2 9Z"/><path d="M11.5 6L17.5 12M17.5 6L11.5 12"/></svg>`,
  hide: `<svg width="20" height="12" viewBox="0 0 20 12" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 3L10 9L16 3"/></svg>`,
  tri: c => `<svg width="9" height="11" viewBox="0 0 9 11"><polygon points="1,1 8,5.5 1,10" fill="${c}"/></svg>`,
  x: `<svg width="12" height="12" viewBox="0 0 12 12" stroke="#fff" stroke-width="1.6" stroke-linecap="round"><path d="M2 2L10 10M10 2L2 10"/></svg>`,
};

const status = `<div class="a status"><span>16:42</span><span class="ic"><span class="sig"></span><span class="bat"></span></span></div>`;
const band = top => `<div class="a band" style="top:${top}px"><div class="pips"><i class="pip on"></i><i class="pip"></i><i class="pip"></i><i class="pip"></i><i class="pip"></i></div><span class="hd">1 · Survey</span><span class="rs">· capture · bound · mark what is there · readings</span></div>`;
/** The top bar: ← · title over the saved state · [laser chip] · ↶ · ⋮ · ✓ Save. */
const bar = (top, laser) => `<div class="a bar" style="top:${top}px"><span class="back">${glyph.back}</span><div class="ttl"><div class="t1">Bedroom · survey prep</div><div class="t2">Saved ✓ 16:42</div></div>${laser ? `<div class="laser${laser === 'ready' ? ' ready' : ''}"><i></i>D2</div>` : ''}<span class="undo">${glyph.undo}</span><span class="menu">${glyph.kebab}</span><div class="save">✓ Save</div></div>`;
/** The face tabs, two rows; the amber count is the figures left to measure on the face. */
function tabsRow(top, front) {
  const t = (name, n, on) => `<div class="tab${on ? ' on' : ''}"><span>${name}${name === 'PLAN' ? '' : ' <span class="ok">✓</span>'}${n ? ` <span class="n">${n}</span>` : ''}</span></div>`;
  return `<div class="a tabs" style="top:${top}px"><div class="trow">${t('FRONT', front, true)}${t('RIGHT', 12)}${t('BACK', 9)}${t('LEFT', 7)}</div><div class="trow">${t('FLOOR', 4)}${t('CEILING', 5)}${t('PLAN', 0)}</div></div>`;
}
/** The step strip. Proposed: 1 · Lines, 2 · Mark, 3 · 360, its lead the face's W x H. Today: four steps, 3 · Measure on. */
function strip(top, today) {
  const st = (n, cls, nm) => `<div class="step ${cls}"><div class="c">${n}</div>${nm ? `<span class="nm">${nm}</span>` : ''}</div>`;
  const j = '<div class="join"></div>';
  return today
    ? `<div class="a strip" style="top:${top}px"><span class="lead">Front wall ·</span>${st(1, 'past')}${j}${st(2, 'past')}${j}${st(3, 'on', 'Measure')}${j}${st(4, '')}</div>`
    : `<div class="a strip" style="top:${top}px"><span class="lead">Front wall · W ${A} × H ${B}</span>${st(1, 'past')}${j}${st(2, 'on', 'Mark')}${j}${st(3, '')}</div>`;
}
/** The one row under the photo (final form): ＋ · Door · Window · Switch plate · last-used · free · ⋯ · ▸. */
function toolRow(top, o = {}) {
  const slot = (w, inner, cls = '', style = '') => `<div class="slot ${cls}" style="width:${w}px;${style}">${inner}</div>`;
  const ty = (name, short, c, on) => slot(44, icon(name, c, 22) + `<span class="lb">${short}</span>`, on ? 'on' : '', on ? `background:${c}42;border:2px solid ${c};` : '');
  return `<div class="a trow2" style="top:${top}px">`
    + slot(48, icon('plus', '#ECEEE8', 24))
    + ty('Door', 'Door', TC.D, o.armed === 'D') + ty('Window', 'Window', TC.W) + ty('Switch plate', 'Switch', TC.S) + ty('Electric point', 'Electric', TC.E)
    + `<div style="flex:1"></div>`
    + slot(48, icon('dots', o.picked ? '#ECEEE8' : '#555A50', 24))
    + (o.next === false ? slot(48, '') : slot(48, icon('next', '#ECEEE8', 24)))
    + `</div>`;
}
const fitGrid = (top, all) => `<div class="a ovcol" style="top:${top}px"><span class="ov">fit</span><span class="ov">grid</span>${all ? '<span class="ov">all</span>' : ''}</div>`;

/** The keypad: 1 2 3 ⌫ / 4 5 6 Clear / 7 8 9 Next ▸ / ⌄ 0, four rows of 44 dp, 6 dp under what is above it. */
function keypad(top) {
  const k = (t, cls = '') => `<div class="key ${cls}">${t}</div>`;
  return `<div class="a pad" style="top:${top + 6}px"><div class="l">
    <div class="rw">${k(1)}${k(2)}${k(3)}</div><div class="rw">${k(4)}${k(5)}${k(6)}</div><div class="rw">${k(7)}${k(8)}${k(9)}</div>
    <div class="rw">${k(glyph.hide, 'q')}${k(0, 'f2')}</div></div>
    <div class="r">${k(glyph.bksp, 'q')}${k('Clear', 'q w')}${k('Next ' + glyph.tri('#1A1A12'), 'g f2')}</div></div>`;
}

/** The panel (the Mark sheet with the acts), for the picked window at Survey. */
function panel(h) {
  return `<div class="a sheet" style="height:${h}px">
  <div class="grip"><i></i></div>
  <div class="shead"><div class="nm"><b>FW1</b> · Window</div><div class="tr"><span class="dot" style="background:#4dabf7"></span>Doors &amp; win.</div><div class="done">Done</div></div>
  <div class="acts"><div class="act">Lock</div><div class="act">Duplicate</div><div class="act">Fine move</div></div>
  <div class="acts"><div class="act">Four corners</div><div class="act">Lock all</div><div style="width:16px;flex:none"></div><div class="act del" style="width:${r2(336 - 2 * ((336 - 8) / 3) - 4 - 24)}px">Delete</div></div>
  <div class="blk"><div class="lab">STATE</div><div class="now"><span class="dot" style="background:#6b6f68"></span><b>Existing</b></div>
    <div class="gline">Finish the survey first — the stage moves to Client wish list on S5</div></div>
  <div class="blk"><div class="lab"><span style="flex:1">NOTE</span><span style="font-weight:500;letter-spacing:0">0/280</span></div>
    <div class="nbox">A note on this mark (optional)</div>
    <div class="aud"><div class="on">Client &amp; vendors</div><div>Office only</div></div></div>
  </div>`;
}

/** The Add sheet: Most used here / Measure / By trade (today's More rows as built, General first; it scrolls). */
function addSheet(h) {
  const tile = (name, label, c) => `<div class="tile">${icon(name, c, 22)}<span>${label}</span></div>`;
  const TY = {
    NOTE: ['Note', 'Note', '#ffffff'], X: ['Other', 'Other', '#adb5bd'], C: ['Column', 'Column', '#b197fc'], B: ['Beam', 'Beam', '#e0b040'], O: ['Opening', 'Opening', '#69db7c'], N: ['Niche', 'Niche', '#20c997'],
    W: ['Window', 'Window', '#4dabf7'], D: ['Door', 'Door', '#ff8a3d'], GRL: ['Grill', 'Grill', '#82c91e'], E: ['Electric point', 'Electric', '#ffd43b'], P: ['Bulb point', 'Bulb', '#ffa8a8'], S: ['Switch plate', 'Switch', '#f783ac'], F: ['Fan', 'Fan', '#74c0fc'],
    LV: ['Low-voltage point', 'LV point', '#7048e8'], AC: ['AC unit', 'AC', '#99e9f2'], L: ['Loft', 'Loft', '#ff922b'], CRP: ['Carpentry unit', 'Unit', '#a0522d'], PLB: ['Plumbing point', 'Plumbing', '#15aabf'], SAN: ['Sanitary ware', 'Sanitary', '#d0ebff'],
    DEF: ['Defect', 'Defect', '#f03e3e'], CTR: ['Curtain track', 'Track', '#c2255c'],
  };
  const rows = [['General', '#adb5bd', ['NOTE', 'X']], ['Civil', '#b197fc', ['C', 'B', 'O', 'N']], ['Doors & win.', '#4dabf7', ['W', 'D']], ['Grills', '#82c91e', ['GRL']],
    ['Electrical', '#ffd43b', ['E', 'P', 'S', 'F']], ['CCTV & data', '#7048e8', ['LV']], ['AC', '#99e9f2', ['AC']], ['Carpentry', '#a0522d', ['L', 'CRP']],
    ['Plumbing', '#15aabf', ['PLB', 'SAN']], ['Painting', '#63e6be', ['DEF']], ['Curtains', '#c2255c', ['CTR']]];
  const mrow = ([label, c, ts]) => `<div class="mrow"><div class="tl" style="color:${c}">${esc(label)}</div>${ts.map(t => { const [nm, sh, col] = TY[t]; return `<div class="mt" style="border-color:${col}">${icon(nm, col, 16)}<span class="tk" style="color:${col}">${t}</span><span class="sn">${sh}</span></div>`; }).join('')}</div>`;
  return `<div class="a sheet" style="height:${h}px;padding-top:4px">
  <div class="grp">Most used here</div>
  <div class="tiles">${tile('Door', 'Door', TC.D)}${tile('Window', 'Window', TC.W)}${tile('Switch plate', 'Switch plate', TC.S)}${tile('Electric point', 'Electric point', TC.E)}</div>
  <div class="grp">Measure</div>
  <div class="tiles">${tile('Tape', 'Tape', '#facc15')}${tile('Line', 'Line', '#7dd3fc')}<div class="tile ghost"></div><div class="tile ghost"></div></div>
  <div class="grp">By trade</div>
  <div style="margin-top:2px">${rows.map(mrow).join('')}</div>
  </div>`;
}

// ------------------------------------------------------------------ the screens
// Rows, proposed (final form, after I4): pad 6 · band 20 · bar 40 · tabs 64 · strip 28 · PHOTO 536 · tool row 6 + 50 · pad 6
const T = { band: 30, bar: 50, tabs: 90, strip: 154, photo: 182 };
const PH = 536;

function proposed(state, o = {}) {
  const laser = o.laser || 'off';
  const kp = state === 'type';
  const boxH = kp ? PH + 56 - 182 : PH;
  const marks = wallMarks(state);
  let h = status + band(T.band) + bar(T.bar, laser) + tabsRow(T.tabs, o.left) + strip(T.strip, false);
  h += photo(boxH, marks, { lift: o.lift, id: o.id || state }).replace('class="photo"', `class="photo" style="top:${T.photo}px"`);
  h += fitGrid(T.photo + 6, false);
  if (o.armed) h += `<div class="a toolchip" style="top:${T.photo + 6}px;border:1.5px solid ${TC.D}"><span>Placing Door · tap the photo</span><span class="x">${glyph.x}</span></div>`;
  if (kp) h += keypad(T.photo + boxH);
  else h += toolRow(T.photo + PH + 6, { picked: state !== 'none', armed: o.armed, next: true });
  if (o.sheet === 'panel') {
    const sh = o.sheetH;
    h += `<div class="a scrim" style="height:${756 - sh}px"></div>` + panel(sh);
  }
  if (o.sheet === 'add') {
    const sh = o.sheetH;
    h += `<div class="a scrim" style="height:${756 - sh}px"></div>` + addSheet(sh);
  }
  return `<div class="phone">${h}</div>`;
}

/** Today, 0.3.339: 3 · Measure, the caption row, the photo (510), the measure bar with its figure box and D2. */
function today() {
  const top = { band: 30, bar: 50, tabs: 90, strip: 154, cap: 182, photo: 204 };
  const boxH = 510;
  let h = status + band(top.band) + bar(top.bar, null) + tabsRow(top.tabs, 17) + strip(top.strip, true);
  h += `<div class="a cap" style="top:${top.cap}px">Front wall · W ${A} × H ${B}</div>`;
  h += photo(boxH, wallMarksToday(), { id: 'today' }).replace('class="photo"', `class="photo" style="top:${top.photo}px"`);
  h += fitGrid(top.photo + 6, true);
  h += `<div class="a mbar" style="top:${top.photo + boxH + 4}px"><div class="cell">FW1<br>Window</div><div class="arr">${glyph.prev}</div>
    <div class="lines"><div class="what">height</div><div class="prog">5 of 22 measured</div></div>
    <div class="fbox">≈1106</div><div class="d2"><i></i>D2</div><div class="arr">${glyph.nxt}</div></div>`;
  return `<div class="phone">${h}</div>`;
}

const page = (title, body, w = 360, h = 780, extra = '') => `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=${w}, initial-scale=1"><title>${esc(title)}</title>
<style>${CSS}${extra}</style></head><body>${body}</body></html>
`;

const PANEL_H = 344;   // grip 16 + head 40 + acts 88 + STATE 82 + NOTE 110 + foot 8: under half the window (378), so it stands at its own height
const ADD_H = 567;     // about three-quarters of the 756 dp window
// under the panel the photo is lifted so the picked mark sits in the middle of what is still seen
const visible = 780 - PANEL_H - T.photo;
const lift = (T.photo + (PH - IH) / 2 + W1.ym) - (T.photo + visible / 2);

const pages = {
  'mcft-new-1-nothing-picked': page('Proposed · nothing picked', proposed('none', { left: 18, laser: 'off' })),
  'mcft-new-2-mark-held': page('Proposed · a mark held', proposed('held', { left: 18, laser: 'off' })),
  'mcft-new-3-measuring-gold': page('Proposed · a dimension being measured', proposed('walk', { left: 17, laser: 'ready' })),
  'mcft-new-4-keypad': page('Proposed · the keypad open', proposed('type', { left: 17, laser: 'ready' })),
  'mcft-new-5-panel': page('Proposed · the panel', proposed('held', { left: 18, laser: 'off', sheet: 'panel', sheetH: PANEL_H, lift, id: 'panel' })),
  'mcft-new-6-add-sheet': page('Proposed · the Add sheet', proposed('none', { left: 18, laser: 'off', sheet: 'add', sheetH: ADD_H, id: 'add' })),
  'mcft-new-6b-tool-armed': page('Proposed · a tool armed', proposed('none', { left: 18, laser: 'off', armed: 'D', id: 'armed' })),
  'mcft-new-7-today-vs-proposed': page('Today and proposed', `<div class="wrap"><div class="colm"><div class="hd2">today · build 0.3.339</div>${today()}</div><div class="colm"><div class="hd2 new">proposed</div>${proposed('walk', { left: 17, laser: 'ready', id: 'cmp' })}</div></div>`, 760, 800,
    `.wrap{width:760px;height:800px;background:#000;display:flex;gap:20px;padding:0 10px}.colm{width:360px}.hd2{height:20px;line-height:20px;text-align:center;font-size:13px;font-weight:700;letter-spacing:.4px;color:#cfd3cb}.hd2.new{color:#E0B040}`),
};
for (const [name, html] of Object.entries(pages)) fs.writeFileSync(path.join(OUT, name + '.html'), html);
console.log(Object.keys(pages).join('\n'));
console.log('picture', r2(COL), 'x', r2(IH), 'dp · wall', r2(WX1 - WX0), 'x', r2(WY1 - WY0), '· scale', S.toFixed(5), 'dp/mm · lift', r2(lift));
