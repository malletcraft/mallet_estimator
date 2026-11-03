# The prototype's phone line — how it is refreshed since 3 Nov 2026 (night)

The S2 prototype ("Set Room Lines", https://claude.ai/artifact/64peToYoeaRz9UNwB926m5) carries, above its frame, the build
Google Play serves the phone: `phone APK 0.3.<run> · carries v61c.2: … · not yet: …`. It is updated in the SAME step as
each Play publish (Amit, 3 Nov 2026 06:57 IST). The one-constant build system that did this (APK61c2.json + apk61c2.py +
integrate61c.py) was lost with the session scratchpad when the cloud container was re-created at ~22:50 IST on 3 Nov;
Amit chose (23:2x IST) to proceed without the prototype gate and rebuild it only if the page's content changes.

So a refresh is now DIRECT SUBSTITUTION on the page as last published, done by `refresh.py` here:
1. read the page as last published (Artifact read of the url, path `index.html`; strip the publisher's skeleton — the
   first line up to `<body>\n` and the trailing `</body></html>` — and check the md5 against the last build's);
2. `python3 -I refresh.py <run> "<HH:MM>" "<HH:MM IST>" ["one sentence added to the bullet"]` — exactly three substitutions
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
