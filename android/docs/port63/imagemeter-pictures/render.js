// Render each page to a PNG beside it: 360 x 780 at device scale factor 2 (the side-by-side: 760 x 800).
'use strict';
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');
const OUT = process.argv[2];
const only = process.argv[3];
(async () => {
  const browser = await chromium.launch();
  const names = fs.readdirSync(OUT).filter(f => /^mcft-new-.*\.html$/.test(f)).map(f => f.replace(/\.html$/, '')).filter(n => !only || n.includes(only)).sort();
  for (const name of names) {
    const wide = name.includes('today-vs-proposed');
    const ctx = await browser.newContext({ viewport: wide ? { width: 760, height: 800 } : { width: 360, height: 780 }, deviceScaleFactor: 2 });
    const page = await ctx.newPage();
    await page.goto('file://' + path.join(OUT, name + '.html'));
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(OUT, name + '.png') });
    await ctx.close();
    console.log(name + '.png');
  }
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
