// hdr6 look (3 Nov 2026, night; written after the container re-creation lost the hdr scripts): the eyebrow's 4th line at
// 360 x 780 and 780 x 360, EN and मराठी. Checks: the text equals L4 (typed here from the brief, not read from the page);
// the two clauses are whole nowrap spans; every drawn line is inside the box; no ellipsis; the line breaks only at " · ";
// the build is the only bold; no sideways scroll; no page error. Env: URL (the page), VER (e.g. 0.3.283), OUT (dir for shots), CL (the two clauses, JSON; default the 3 Nov ones).
const { chromium } = require('playwright');
const URL = process.env.URL, OUT = process.env.OUT || '.', VER = process.env.VER;
const CL = process.env.CL ? JSON.parse(process.env.CL) : ['carries v61c.2: phases, Approve, wish list, Marathi', 'not yet: Album, tour'];   // env CL: the two clauses as a JSON list, typed from the brief
const L4 = ['phone APK ' + VER, ...CL].join(' · ');   // the 4th line from VER and however many clauses CL holds (Version 86 has one)
(async () => {
  const b = await chromium.launch(); let pass = 0, fail = 0;
  for (const [W, H] of [[360, 780], [780, 360]]) for (const lang of ['en', 'mr']) {
    const c = await b.newContext({ viewport: { width: W, height: H }, deviceScaleFactor: 2 });
    await c.addInitScript(l => { try { if (!sessionStorage.getItem('lk')) { sessionStorage.setItem('lk', '1'); localStorage.removeItem('mcft-lang-v57'); if (l === 'mr') localStorage.setItem('mcft-lang-v57', JSON.stringify({ lang: 'mr' })); } } catch (_) {} }, lang);
    const p = await c.newPage(); const errs = []; p.on('pageerror', e => errs.push(String(e))); await p.goto(URL); await p.waitForTimeout(2500);
    const r = await p.evaluate(() => {
      const ey = document.getElementById('eyebrow'), l = [...ey.querySelectorAll('.eyeln')][3], bx = l.getBoundingClientRect();
      const lines = []; const walk = n => { if (n.nodeType === 3) { for (let i = 0; i < n.length; i++) { const rg = document.createRange(); rg.setStart(n, i); rg.setEnd(n, i + 1); const q = rg.getClientRects()[0]; if (!q) continue; const m = (q.top + q.bottom) / 2; let L = lines.find(x => Math.abs(x.m - m) < 6); if (!L) { L = { m, t: '', l: 1e9, r: -1e9 }; lines.push(L); } L.t += n.textContent[i]; if (q.width > 0) { L.l = Math.min(L.l, q.left); L.r = Math.max(L.r, q.right); } } } else n.childNodes.forEach(walk); };
      walk(l); lines.sort((a, b) => a.m - b.m);
      const sp = [...l.querySelectorAll(':scope > span')];
      return { lang: document.getElementById('device').getAttribute('lang') || 'en', txt: l.textContent, box: [Math.round(bx.left), Math.round(bx.right)], sw: l.scrollWidth, cw: l.clientWidth,
        lines: lines.map(x => ({ t: x.t, l: Math.round(x.l * 10) / 10, r: Math.round(x.r * 10) / 10 })), clauses: sp.map(x => ({ t: x.textContent, rects: x.getClientRects().length, ws: getComputedStyle(x).whiteSpace })),
        ell: [l, ...sp].map(x => getComputedStyle(x).textOverflow), bold: [...l.querySelectorAll('b')].map(x => [x.textContent, getComputedStyle(x).fontWeight]),
        hscroll: document.documentElement.scrollWidth > document.documentElement.clientWidth };
    });
    const e = await p.$('#eyebrow'); await e.screenshot({ path: `${OUT}/look-${W}x${H}-${lang}.png` });
    const tr = s => s.replace(/^\s+|\s+$/g, '');
    const okTxt = r.txt === L4, okCl = JSON.stringify(r.clauses.map(x => [x.t, x.rects, x.ws])) === JSON.stringify(CL.map(t => [t, 1, 'nowrap'])),
      okFit = r.sw <= r.cw && r.lines.every(x => x.l >= r.box[0] - .5 && x.r <= r.box[1] + .5), okEll = r.ell.every(x => x !== 'ellipsis'),
      parts = [`phone APK ${VER}`, ...CL], okBreak = r.lines.every(x => { const s = tr(x.t).replace(/ ·$/, ''); return s.split(' · ').every(q => parts.includes(q)); }) && r.lines.map(x => tr(x.t)).join(' ').replace(/\s+/g, ' ') === L4,
      okBold = JSON.stringify(r.bold) === JSON.stringify([[VER, '800']]) || JSON.stringify(r.bold) === JSON.stringify([[VER, '700']]), okErr = errs.length === 0, okH = !r.hscroll;
    const ok = okTxt && okCl && okFit && okEll && okBreak && okBold && okErr && okH; ok ? pass++ : fail++;
    console.log(`${ok ? 'PASS' : 'FAIL'} ${W} x ${H} ${lang}: box ${r.box[0]}-${r.box[1]} px, ${r.lines.length} lines:`);
    for (const x of r.lines) console.log(`     | ${tr(x.t).padEnd(58)} | ${x.l}-${x.r}`);
    console.log(`     text ${okTxt} · clauses whole ${okCl} · inside its box ${okFit} · no ellipsis ${okEll} · breaks only at " · " ${okBreak} · ${VER} bold ${okBold} (${JSON.stringify(r.bold)}) · no h-scroll ${okH} · no page error ${okErr}${errs.length ? ' ' + errs.join(' | ') : ''}`);
    await c.close();
  }
  console.log(`\nLOOK: ${pass} passed, ${fail} failed`); await b.close(); process.exit(fail ? 1 : 0);
})();
