# The prototype's phone line — how it is refreshed since 3 Nov 2026 (night)

The S2 prototype ("Set Room Lines", https://claude.ai/artifact/64peToYoeaRz9UNwB926m5) carries, above its frame, the build
Google Play serves the phone: `phone APK 0.3.<run> · carries v61c.2: … · not yet: …`. It is updated in the SAME step as
each Play publish (Amit, 3 Nov 2026 06:57 IST). The one-constant build system that did this (APK61c2.json + apk61c2.py +
integrate61c.py) was lost with the session scratchpad when the cloud container was re-created at ~22:50 IST on 3 Nov;
Amit chose (23:2x IST) to proceed without the prototype gate and rebuild it only if the page's content changes.

So a refresh is now DIRECT SUBSTITUTION on the page as last published, done by `refresh.py` here:
1. read the page as last published (Artifact read of the url, path `index.html`; strip the publisher's skeleton — the
   first line up to `<body>\n` and the trailing `</body></html>` — and check the md5 against the last build's);
2. `HDR_DATE="<D Mon YYYY>" python3 -I refresh.py <run> "<HH:MM>" "<HH:MM IST>" ["one sentence added to the bullet"]` (since
   9 Nov 2026 the build date comes from HDR_DATE; left unset, the page's own date stays; the added sentence must not name
   the build, or the "named exactly twice" check refuses; HDR_SUBS, a JSON list of [old, new] pairs each matched once, carries a
   changed clause of the 4th line or a changed phrase of the bullet, and look.js then takes the clauses as env CL) — exactly
   three substitutions plus the HDR_SUBS pairs
   (the build-time span, `phone APK <b>0.3.<run></b>`, `APK 0.3.<run>, from Google Play internal testing`), each matched
   once; refuses otherwise; the new build must then be named exactly twice and the old one nowhere;
3. the Playwright look, `look.js` (env URL, VER, OUT), at 360 × 780 and 780 × 360 in EN and मराठी: the 4th line's text,
   whole nowrap clauses, inside its box, no ellipsis, breaks only at " · ", the build the only bold, no sideways scroll,
   no page error — 4 passed or nothing is published;
4. publish to the same url; record the version and the time in RUNNING.md (mcft-workspace) and in the log below.

The carries clause is measured, not guessed: at 360 px wide the clause budget is ~312 px at 9.72 px (the 0.3.276 refresh
overflowed with a 62-character clause; `carries v61c.2: phases, Approve, wish list, Marathi` measures 298 px). Changing a
clause means re-measuring with a clone of the clause span in the page's own font.

## Log

| Version | Build | Play (IST) | Published (IST) | Page md5 | Notes |
|---|---|---|---|---|---|
| 73 | 0.3.276 | 17:24, 3 Nov | 17:32, 3 Nov | 0ef7f1bf… | first clause overflowed at 360 px, shortened |
| 74 | 0.3.278 | 19:32, 3 Nov | 19:34, 3 Nov | 722e5681… | clause measured first |
| 75 | 0.3.280 | 21:34, 3 Nov | 21:36, 3 Nov | e9354156… | last build by the one-constant system |
| 76 | 0.3.283 | 23:39, 3 Nov | 23:42, 3 Nov | cda1a2b4… | first refresh by substitution; the bullet gains one sentence (AS1, FW1); look 4/0 |
| 77 | 0.3.286 | 10:18, 9 Nov | 10:47, 9 Nov | a9dc11e9… | refresh.py takes the build date (HDR_DATE); the bullet gains one sentence (FW2); look 4/0 |
| 78 | 0.3.288 | 12:56, 9 Nov | 12:58, 9 Nov | 7b731a52… | the Album ships read-only (K3): the clauses change by HDR_SUBS to `carries phases, Approve, wish list, Album, Marathi` (292.6 of 328 px at 360; "v61c.2" is on the eyebrow's 2nd line) and `not yet: Album edits, tour`; the bullet's not-yet phrase and one sentence; look 4/0 |
| 79 | 0.3.292 | 15:36, 9 Nov | 15:38, 9 Nov | cd8bdebe… | the PM's writes ship (K4): the 2nd clause becomes `not yet: progress shot, tour` (163.8 of 328 px at 360); the bullet's Album phrase and one sentence; look 4/0 |
| 80 | 0.3.296 | 18:48, 9 Nov | 18:50, 9 Nov | 012b56d8… | the progress shot ships (K5): the 2nd clause becomes `not yet: tour` (76.1 of 328 px at 360); the bullet's phrase and one sentence; look 4/0 |
| 81 | 0.3.299 | 21:52, 9 Nov | 21:54, 9 Nov | db718921… | Marathi measures/readings ship (M3b): no clause change; the bullet gains one sentence; look 4/0 |
| 82 | 0.3.303 | 23:30, 9 Nov | 23:33, 9 Nov | df9fd1a1… | Marathi plan + Export sheet ship (M3c): no clause change; the bullet gains one sentence; look 4/0 |
| 83 | 0.3.306 | 01:22, 10 Nov | 01:24, 10 Nov | a11c287b… | Marathi tree/queue/upload status ship (M2): no clause change; the bullet gains one sentence; look 4/0 |
| 84 | 0.3.310 | 03:27, 10 Nov | 03:29, 10 Nov | 1cd6076f… | Marathi shell + S5 sign-in + laser words ship (M6): no clause change; the bullet gains one sentence; look 4/0 |
| 85 | 0.3.314 | 05:00, 10 Nov | 05:02, 10 Nov | 492cb68a… | Marathi for Face Prep/Annotate/Laser test ships (M8, unkeyed.tsv empties): no clause change; the bullet gains one sentence; look 4/0 |
| 86 | 0.3.318 | 11:44, 10 Nov | 11:49, 10 Nov | 5aa0a190… | the tour ships (the port's last slice): the clauses become ONE — `carries the whole v61c port, survey tour included` (286.7 of 328 px at 360), `not yet:` clears; look.js joins N clauses instead of assuming two; the bullet gains one sentence; look 4/0 |
