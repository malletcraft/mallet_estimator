# Prototype v62b (Version 89) — the room-centred album (item RP0), FOR AMIT'S APPROVAL

Published 04:10 IST, 12 Nov 2026, at https://claude.ai/artifact/64peToYoeaRz9UNwB926m5 (Version 89, page md5 8d4633cb…;
the mock-up as built is md5 c40931a8…, and the header was then moved to the Play build 0.3.324 with refresh.py).
Nothing here is on the phone or on S5. The design is `mcft-erpnext-context` `analysis/room-centred-album.md`
(Amit, 11 Nov 2026: "Room is the scope of work always"; "Every work belongs to a room"; "Rooms + one overview";
scope "Per room"). `build88.py` beside this file rebuilds the mock-up from the Version 87 base in one run (it needs the
base page and the assets, which are not in the repository). The reports below are the two builders', the reviewer's
six findings and the fixer's, verbatim.

---

# Stage A (builder's report)

STAGE A of v62b is built and verified. Nothing is published; the base is untouched (md5 3f481eaa521730871a1aaad9e3b97c00).

Page: /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/index61c2.html — md5 d51a01765e6ef7da8f33af8a3cced16b, 9,710 lines, eyebrow "mock-up v62b · 12 Nov 2026, 00:33 IST".

REBUILD (by counted substitution from the base, as build87.py did)
python3 /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88.py "12 Nov 2026, 00:33 IST" reproduces the same md5 (checked byte for byte). Without the argument the stamp is now, so the md5 changes.
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88.py (dictionary, CSS, P54, arranger cut, the two modules)
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88_app.py (the shell)
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88_page.py (tours, eyebrow)
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88_notes.py (notes card)
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/src/core62b.js and /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/src/room62b.js (the two rewritten modules — edit these, then rebuild)
The local server this task started (port 54767) is stopped. To drive again: serve /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/site on a free port and write the port to /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/port.txt.

WHAT CHANGED (line numbers are the built page's)
1. The model — window.ALB60, lines 6952–7359 (replaces the v60 core; VER 'v62b', store 'mcft-album-v62').
- Everything is keyed by cell (project, room, work): stateOf / stateLog / openWorks / setState, crOf / crDone / changeRequests / raiseCR / answerCR, scopeOf (now [{room, works}]) / roomScope / inScope / scopeCount / recordScope.
- New: rooms(p) and known(p, room) at 7174 — the one "known to S5" set (seeded capture records, this phone's synced 360s with a survey prep, scope rows, log rows).
- New roll-ups: rollUp 7191, roomRoll 7209, workRoll 7211, projectRoll 7214, lineParts 7222. The note's case table is RCASES / LCASES; ALB60.test.rcases() returns [] when all hold (it does).
- roomModel 7267 replaces chapters(). needsRoom 7279 supplies the "Needs a room" counts.
- addShot and upload refuse a photo with no room or an unknown room; setState refuses a cell outside the room's scope.
- SEED 7023 is re-keyed by room.
2. P54 (the S5 stand-in).
- One-off reseed of the two demo projects' stored stage, 4019.
- projectLine 4067 counts rooms.
- cells62 / unknown62 / roomUnknownMsg 4123; move 4084 and recordScope 4153 take scope as [{room, works}] and refuse an unknown room.
- stripHTML's APPROVED SCOPE row 4224 reads "36 works · 4 rooms · since … · tap to view" and opens the overview.
3. Arranger. The page factory and its CSS are removed; window.FP54 (4670) stays, with a v62b note at 4562.
4. The room module — window.__mcftAlbum60, lines 7361–7657 (replaces the Album page).
- card 7430, workRow 7450, groupRow 7476, foldRow 7484, logRow 7493, byWork 7502, room 7512, rows 7537, screen 7544 (placeholder pages).
- openCam 7560, camHTML 7569 (no ROOM row), camSheet 7582, crSheet 7587, crAnswerSheet 7595, stateSheet 7610, viewHTML 7617, ov 7642.
- Returns {room, rows, screen, ov, open, leave, leaveRoom, back, openCam, worksFirst, workSup, at, page}.
5. The shell.
- Demo projects, site rooms and queue rows 7741–7757.
- album kit 7793; wk62 / newWork 7828; ALB60.bind, known62, roomsOfP, order62, wishes62 7832–7840.
- workRow61c 7958, workSup62 7962, workSheet61c 7965, scopeSheet 7974 (per room, two steps).
- captureSheet 8143 (photo62 at 8154).
- treeScreen 8259 (line62 at 8273, tile at 8290; the Album and Floor plan tiles are gone; rows inserted above the chips).
- roomScreen 8331 (R62 at 8345; the work-filter chips and S.capWork are gone).
- captureScreen's WORK line 8370; roomLevelOverlays 8402; view 8417; view0 8420; goBack 8486 (album back in a room at 8517).
- The project line may take two lines (the exec branch of lineHTML).
6. Tours. Flow 6 "Floor plan", its five anchors, planSel, the 'plan' screen and the arrange key in LSKEYS (9276) are removed; six flows remain. The tour.phases.7.body sentence now says "each room's works".
7. The page around the phone. CSS block 1565–1591; notes card #ch62b at 1711 (before #ch62a); two "Where every tap goes" bullets rewritten; the changes line; the eyebrow.

VERIFIED on this exact md5 (logs in /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/)
- final-drive.log: the walk (scripts/drive.mjs, 49 steps) at 360 × 780 and 780 × 360, in English and मराठी — 0 page errors, 0 sideways scroll, 0 failed assertions, and none of the new one-line texts cut by its box.
  - The only Latin left in मराठी is month names, codes, a file path and "ImageMeter"; none is new text.
  - The real-phone viewport (360 × 780, by touch) passes in both languages.
- final-misc.log: every project and room, as each of the four roles, both orientations, with and without the v53 lens — 0 errors, 0 sideways scroll.
- final-regress.log and final-regress-nolens.log: markup against v62a, with the demo data put back.
  - Under the v53 lens every screen is identical (the lens runs were English only).
  - Outside it everything is identical except the two removed tiles, the room screen, and the drawer's "7 tours → 6".
  - P0's two site rename dialogs and its Prototype test sheet are identical to v62a.
- final-tours.log: all six tours run to the end with every anchor found, in English portrait and मराठी landscape (plus two at the phone viewport). Demo flat interiors is put back to Client approved afterwards.
- final-card.log: the eyebrow's 4th line is byte-identical; "0.3.318" appears exactly twice; exactly one build-time span.
Screenshots: /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/shots/ — 196 walk shots named a01…a49-<state>-<en|mr>-<portrait|landscape>.png, plus b01/b02 real-phone shots. Contact sheets are in /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/contact/.

HOW TO REACH EACH STATE BY HAND
- Project in execution: Clients → Demo Client → Demo Site, Pune → Demo flat interiors.
  - Tiles: GB "no scope yet"; KIT In progress 1/10; LR In progress 2/8; MB Done 8/8; T1 On hold 1/10; +1 → Passage.
  - Line: "Execution · 2 rooms in progress · 1 done · 1 on hold".
  - Rows: "All rooms · works ▸" and "Needs a room · 3 photos · 2 changes not sent ▸" (each opens a placeholder page).
  - Tap the line to open the quote view; APPROVED SCOPE → tap to view → the overview placeholder.
- Room's home: Living Room.
  - Set a state: tap the state pill on a work's row. Leaving Done asks for a reason.
  - Change request in scope: open Painting → Raise change request (remove / change).
  - Folded row: Not in this room's scope → Modular furniture has the live add request and its photo → Record the client's answer → approved moves it into the scope.
  - Capture 360 → WORK row, Photo (progress-shot camera, no ROOM row), 360.
  - Also on this screen: not filed to a work; Quote journey; Work log.
- Vendor photo to review: Toilet 1 → "To review: 1" → Waterproofing → Accept / Not for client. It also carries a live change request and is On hold.
- Roles: "Viewing as" is the first row of any room screen. Vendor is Demo Waterproofing Co. — open Toilet 1 for Upload a photo. The Client sees accepted photos only and has no capture button.
- Room with no scope yet: Guest Bedroom. Its folded row holds all 16; add requests are the "new room after approval" walk.
- Room S5 does not know: +1 → Passage. One line, and no Photo on its capture sheet.
- Not yet approved: Demo bathroom door (at Revisions).
  - Room Bathroom shows the lock line and its photos by work.
  - Approve the estimate ▸ → Rooms in the approved estimate → a room → 16 ticks → Back → Move to Client approved.
- Decided change request: Kitchen → Countertops.

NEW TEXTS (key · EN · MR) — 31 for the phone
Static:
- album.all_rooms_works · All rooms · works · सर्व खोल्या · कामे
- album.needs_room · Needs a room · खोली ठरवणे बाकी
- album.change_not_sent_1 · 1 change not sent · 1 बदल पाठवला गेला नाही
- album.no_room_album · In the album with no room · अल्बममध्ये, खोली न ठरवलेले
- album.not_sent_phone · Not sent from this phone · या फोनवरून पाठवले गेले नाही
- album.old_build_msg · Recorded for the whole project by an older build. States are per room now — set it in the room. · जुन्या बिल्डने हे संपूर्ण प्रोजेक्टसाठी नोंदवले होते. आता स्थिती खोलीनुसार असते — ती त्या खोलीत ठरवा. (in the core, not yet drawn)
- album.no_scope_yet · no scope yet · अजून स्कोप नाही
- room.not_on_s5_save_prep · Not on S5 yet — save this room's survey prep to give it works · अजून S5 वर नाही — या खोलीला कामे मिळण्यासाठी तिची सर्व्हे तयारी सेव्ह करा
- room.no_works_in_scope · No works in this room's approved scope yet · या खोलीच्या मंजूर स्कोपमध्ये अजून कोणतेही काम नाही
- room.not_in_scope · Not in this room's scope · या खोलीच्या स्कोपमध्ये नाही
- room.not_in_scope_lc · not in this room's scope · या खोलीच्या स्कोपमध्ये नाही
- phase.scope_rooms_title · Rooms in the approved estimate · मंजूर अंदाजपत्रकातील खोल्या
- phase.scope_no_works_ticked · no works ticked · कोणतेही काम टिक केलेले नाही
- phase.scope_work_ticked_1 · 1 work ticked · 1 काम टिक केले
- app.work_1 · 1 work · 1 काम
- phase.scope_no_rooms · No room of this project is on S5 yet — save a room's survey prep first · या प्रोजेक्टची एकही खोली अजून S5 वर नाही — आधी एखाद्या खोलीची सर्व्हे तयारी सेव्ह करा
- app.room_in_progress_1 · 1 room in progress · 1 खोली चालू
- app.room_done_1 · 1 room done · 1 खोली पूर्ण
- app.room_on_hold_1 · 1 room on hold · 1 खोली थांबवलेली
- app.room_abandoned_1 · 1 room abandoned · 1 खोली सोडून दिलेली
- app.wish_1 · 1 wish · 1 इच्छा
Templates:
- app.x_rooms_in_progress · {n} rooms in progress · {n} खोल्या चालू
- app.x_rooms_done · {n} rooms done · {n} खोल्या पूर्ण
- app.x_rooms_on_hold · {n} rooms on hold · {n} खोल्या थांबवलेल्या
- app.x_rooms_abandoned · {n} rooms abandoned · {n} खोल्या सोडून दिलेल्या
- app.all_x_rooms_done · all {n} rooms done · सर्व {n} खोल्या पूर्ण
- app.x_wishes · {n} wishes · {n} इच्छा
- album.x_changes_not_sent · {n} changes not sent · {n} बदल पाठवले गेले नाहीत
- app.x_works · {n} works · {n} कामे
- phase.scope_works_ticked_x · {n} works ticked · {n} कामे टिक केली
- refuse.stage.room_unknown_x · S5 holds no survey of {room} yet; not saved · {room} चा सर्व्हे अजून S5 वर नाही; सेव्ह झाले नाही (unreachable from the sheet)
Changed:
- tour.phases.7.body · "…from then on each room's works carry the project." · "…तेव्हापासून प्रत्येक खोलीतील कामे प्रोजेक्ट पुढे नेतात."
Placeholder-only (never on the phone):
- proto.ov.todo · The overview of every room's works is drawn in the next step of this mock-up. · प्रत्येक खोलीच्या कामांचा आढावा या मॉक-अपच्या पुढच्या टप्प्यात काढला जाईल.
- proto.nr.todo · The list that gives each of these a room is drawn in the next step of this mock-up. · यातील प्रत्येकाला खोली देणारी यादी या मॉक-अपच्या पुढच्या टप्प्यात काढली जाईल.
Every other text on the new screens is one the Album or the room screen already had. Counts are separate keys rather than new nouns because a "wish" noun would have changed the existing "Marks: … 9 wish" line.

NOT DONE — LEFT FOR STAGE B
- 3.5 The overview page. screen(p) at 7544 draws a placeholder (the project line, one sentence, a plain list of rooms with their roll-up). Stage B replaces it with grid A, the two-line work rows, the head (To review, Change requests, project log) and Viewing as. workRoll and projectRoll are ready and nothing draws workRoll yet. Questions 2 and 7 are not drawn.
- 3.7 The "Needs a room" list. The placeholder shows the two group headings with counts. Stage B draws the photos, "Which room?", the dropped writes with OLD_BUILD_MSG and Dismiss, and adds the guarded re-file writer (question 8). needsRoom(p) returns {photos, held, dropped}; "Dismiss" has no key yet.
- 3.3 Approve per room is built in its basic form (it had to be, for the scope model to hold). Missing: "Copy the ticks from <room>", the marks hint in step 2, and the old project-wide list shown beside the Record sheet.
- 3.4 Change requests per room and work are built and walkable. The notes card says a new room is N add requests but no screen spells out the count.
- Tours: none re-recorded and none added for the new screens. Only the plan flow was removed and one sentence changed.
- "Where every tap goes" and the #ch62b card describe stage A and call the two pages "the next step"; stage B must rewrite those lines.
- Dead Album CSS is left in (.alb59fab, .alb59end, .tile.alb59t, .alb59sg…, .alb60open, .alb60crq). Stage B's overview head may reuse some; cut the rest at the end.
- The dictionary's plan.* and album-page keys are unused but still present.

WHERE §3 WAS NOT FOLLOWED TO THE LETTER, AND WHY
1. Capture cards for staff. A capture of this phone drawn as a card under its work opens its capture screen for staff, not the photo viewer. Otherwise FW2's WORK-line re-file could not be reached for a capture filed to a work. Vendors and clients get the viewer; album photos open the viewer for everyone.
2. Quote journey and "not filed to a work" in execution are drawn as foldable rows in the Album's chapter-0 style (badge 0 / –), holding the capture tiles, so the list is one visual. Before approval, and in an unknown room, they are the old heading bands.
   - The Quote journey row shows only when the room has a survey capture on this phone. An empty "no photos yet" would be wrong for a room whose 360 lives on another phone.
3. The lock is simply "not approved". v61b's "or a state row exists" exemption is dropped in the mock: under question 5's recommended option a cell can hold a state only after approval. S5's notes still describe the exemption — flag it when RS2 is designed.
4. The room tile in execution shows the roll-up word, "2/8" and a thin bar on a line above the room name, not beside it, because both do not fit 118 dp. In the wish phases it reads "N wishes" (marks past Existing and not Dropped in the room's newest 360 with a prep), including "0 wishes".
5. The capture sheet in execution keeps its title and the button label "Capture 360". Photo sits under the existing "A SINGLE PHOTO" label. The single-photo Gallery button is not offered there, since the note lists only Photo and 360.
6. "Viewing as" is the first row of every room screen. The note places it on the overview, which is stage B's.
7. The demo was re-seeded.
   - Demo flat interiors now starts at Client approved; Demo bathroom door starts at Revisions with two new 360 rows.
   - One unfiled Living Room photo is added, and three rooms are added to the demo site's room list.
   - A stage a browser had stored for those two projects is dropped once.
   - v62a's album additions on a browser (store mcft-album-v60) are not carried over.
   - Demo flat interiors can no longer be walked back to Survey by hand; the tours still take it there and put it back.
8. In landscape the two project rows share one line, to leave height for the tiles.

---

# Stage B (builder's report)

STAGE B of v62b is built and fully verified. Nothing is published. The base is untouched (md5 3f481eaa521730871a1aaad9e3b97c00).

Page: /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/index61c2.html — md5 bc84d85d80a5149cec275cd206a6788d, 9,924 lines, eyebrow "mock-up v62b · 12 Nov 2026, 01:47 IST".

## Rebuild
One run from the base: `python3 /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88.py "12 Nov 2026, 01:47 IST"` reproduces the same md5 (checked). Without the argument the stamp is now.
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88.py (texts, CSS, P54, the two modules, header asserts)
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88_app.py (the shell)
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88_clean.py (new: dead CSS and dictionary keys)
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88_page.py (tours, eyebrow)
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88_notes.py (notes card)
- /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/src/core62b.js and /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/src/room62b.js (edit these, then rebuild)
Stage A's scripts are kept in /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/stageA/. The server this task started (port 42329) is stopped; two other servers (8761, 34695) were not mine and are left running. To drive again, serve /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/site and write the port to /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/port.txt.

## What changed in stage B (line numbers are the built page's)
1. **The overview** (room module, 7655–7711; CSS 1559–1596). Read-only, no photos.
   - Sketch A exactly: 8 + 36 + 16 × 17 + 36 + 8 = 360 dp. Rooms down, positions 1–16 across, drawn state signs, "!" for a live change request, token left, "2/8" right, a legend.
   - Sixteen two-line work rows ("Waterproofing / On hold · 0 of 1 rooms done"); a work no room has reads "not in any room's scope".
   - Head: Viewing as, "To review: 1 · Toilet 1" (PM), "Change requests: 2 · Living Room · Toilet 1", the project line; Work log of the whole project at the bottom.
   - A room's row opens that room. Nothing writes. Opens from "All rooms · works ▸" and from APPROVED SCOPE → tap to view.
   - Sketch B (the list) is not built.
2. **Plan view inside the overview** (planHTML 7691; question 7). A Works / Plan chip pair; the rooms in place from a seeded "as S5 web arranged it" layout, each in its state's pill colour with sign and count. A project with no arranged plan says so.
3. **Needs a room** (needsBody 7713, whichSheet 7735; core writers 7372–7385). Two groups, Which room? (known rooms only, no "Whole project"), the held photo is sent with its room, refused writes show their words and Dismiss; when empty the page says so and the project row disappears.
4. **Approve per room** (scopeSheet 8180, marks62 8175). Added: the marks hint in step 2, "Copy the ticks from <room>" chips, and the old project-wide list shown in both steps of Record with nothing pre-ticked. PM-only and approval-final are unchanged and re-tested.
5. **Change requests.** Unchanged from stage A except the folded row now shows one chip with a count ("add · 3") when several add requests are live; eight separate chips would have overflowed.
6. **Album page removed; its parts placed.** 25 dead CSS rules cut; 39 dead dictionary keys dropped (the Floor plan tour's texts, the arranger page's, the Album page's own).
7. **Tours.** Six remain and all complete. The build now asserts that no remaining step names the Album or the Floor plan.
8. **Notes card #ch62b** rewritten for the whole of v62b (25 points, 48 texts, one changed text); "Where every tap goes" gained a line for the two pages; the changes line updated. Eyebrow: mock-up v62b, one build-time span.
9. **Demo.** Sample wardrobe repair (Sample Customer) is now "approved before the scope was per room", with an old project-wide scope and one old state. Demo flat interiors gained a seeded plan; Demo bathroom door gained seeded marks-in-scope for its two rooms.
10. **One fix beyond the list:** a room S5 knows that is not in this phone's own room list now still gets a tile on the project screen.

## How to reach each state by hand
- **Overview:** Clients → Demo Client → Demo Site, Pune → Demo flat interiors → All rooms · works ▸. Tap a room's row to open it. Scroll for the sixteen work lines; Work log (19) is at the bottom. Viewing as is the first row.
- **Plan:** on the overview tap Plan; tap a room to open it.
- **Scope read-only:** on the project tap the green line → APPROVED SCOPE → tap to view.
- **Needs a room:** on the project tap the amber row. Which room? under a photo → pick a room → "Photo filed in …". Under "Not sent from this phone" the held photo → Which room? → "Photo sent to …"; Dismiss on each refused change.
- **Approve per room:** Demo bathroom door → Approve the estimate ▸ → Bathroom (hint line on top; tick) → Back → Passage → Copy the ticks from Bathroom → Back → Move to Client approved.
- **Record with the old list:** Clients → Sample Customer (opens Sample wardrobe repair) → tap the green line → Record the approved scope → Master Bedroom → tick → Back → Record the approved scope. Then All rooms · works ▸ → Work log (1) shows "recorded for the whole project"; Plan shows the not-arranged line.
- **A new room after approval:** Demo flat interiors → Guest Bedroom → Not in this room's scope → open a work → Raise change request · add, repeated; the chip counts them and the overview names the room.
- **Change request, state, vendor photo, roles, room not on S5:** as in stage A (Living Room, Toilet 1, Passage under +1).
- What is added is kept in the browser; clearing the page's site data resets the demo.

## New texts (key · EN · MR) — 48 for the phone
Stage B added 17:
| key | EN | MR |
|---|---|---|
| album.works | Works | कामे |
| album.not_in_scope_lg | not in scope | स्कोपमध्ये नाही |
| album.change_request_lg | change request | बदल विनंती |
| album.not_in_any_scope | not in any room's scope | कोणत्याही खोलीच्या स्कोपमध्ये नाही |
| album.recorded_whole_project | recorded for the whole project | संपूर्ण प्रोजेक्टसाठी नोंदवलेले |
| album.plan_from_s5 | Arranged on S5 web, read-only here | S5 वेबवर मांडलेला, इथे फक्त पाहण्यासाठी |
| album.plan_none | The flat's plan shows here once its rooms are arranged on S5 web | S5 वेबवर खोल्या मांडल्या की फ्लॅटचा प्लॅन इथे दिसेल |
| album.not_on_plan | Not on the plan yet: | अजून प्लॅनवर नाही: |
| album.nothing_needs_room | Nothing needs a room | खोली ठरवायचे काही बाकी नाही |
| album.not_sent | not sent | पाठवला नाही |
| album.dismiss | Dismiss | यादीतून काढा |
| phase.scope_old_list | The scope an older build recorded for the whole project, for reference: | जुन्या बिल्डने संपूर्ण प्रोजेक्टसाठी नोंदवलेला स्कोप, संदर्भासाठी: |
| phase.scope_marks_hint | This room's marks in scope: | या खोलीचे स्कोपमधील मार्क: |
| album.x_of_x_rooms_done | {k} of {n} rooms done | {n} पैकी {k} खोल्या पूर्ण |
| album.photo_filed_in_x | Photo filed in {room} | फोटो "{room}" मध्ये ठेवला |
| album.photo_sent_to_x | Photo sent to {room} | फोटो "{room}" मध्ये पाठवला |
| phase.scope_copy_from_x | Copy the ticks from {room} | {room} च्या टिक कॉपी करा |

Stage A's 31, unchanged:
| key | EN | MR |
|---|---|---|
| album.all_rooms_works | All rooms · works | सर्व खोल्या · कामे |
| album.needs_room | Needs a room | खोली ठरवणे बाकी |
| album.change_not_sent_1 | 1 change not sent | 1 बदल पाठवला गेला नाही |
| album.no_room_album | In the album with no room | अल्बममध्ये, खोली न ठरवलेले |
| album.not_sent_phone | Not sent from this phone | या फोनवरून पाठवले गेले नाही |
| album.old_build_msg | Recorded for the whole project by an older build. States are per room now — set it in the room. | जुन्या बिल्डने हे संपूर्ण प्रोजेक्टसाठी नोंदवले होते. आता स्थिती खोलीनुसार असते — ती त्या खोलीत ठरवा. |
| album.no_scope_yet | no scope yet | अजून स्कोप नाही |
| room.not_on_s5_save_prep | Not on S5 yet — save this room's survey prep to give it works | अजून S5 वर नाही — या खोलीला कामे मिळण्यासाठी तिची सर्व्हे तयारी सेव्ह करा |
| room.no_works_in_scope | No works in this room's approved scope yet | या खोलीच्या मंजूर स्कोपमध्ये अजून कोणतेही काम नाही |
| room.not_in_scope | Not in this room's scope | या खोलीच्या स्कोपमध्ये नाही |
| room.not_in_scope_lc | not in this room's scope | या खोलीच्या स्कोपमध्ये नाही |
| phase.scope_rooms_title | Rooms in the approved estimate | मंजूर अंदाजपत्रकातील खोल्या |
| phase.scope_no_works_ticked | no works ticked | कोणतेही काम टिक केलेले नाही |
| phase.scope_work_ticked_1 | 1 work ticked | 1 काम टिक केले |
| app.work_1 | 1 work | 1 काम |
| phase.scope_no_rooms | No room of this project is on S5 yet — save a room's survey prep first | या प्रोजेक्टची एकही खोली अजून S5 वर नाही — आधी एखाद्या खोलीची सर्व्हे तयारी सेव्ह करा |
| app.room_in_progress_1 | 1 room in progress | 1 खोली चालू |
| app.room_done_1 | 1 room done | 1 खोली पूर्ण |
| app.room_on_hold_1 | 1 room on hold | 1 खोली थांबवलेली |
| app.room_abandoned_1 | 1 room abandoned | 1 खोली सोडून दिलेली |
| app.wish_1 | 1 wish | 1 इच्छा |
| app.x_rooms_in_progress | {n} rooms in progress | {n} खोल्या चालू |
| app.x_rooms_done | {n} rooms done | {n} खोल्या पूर्ण |
| app.x_rooms_on_hold | {n} rooms on hold | {n} खोल्या थांबवलेल्या |
| app.x_rooms_abandoned | {n} rooms abandoned | {n} खोल्या सोडून दिलेल्या |
| app.all_x_rooms_done | all {n} rooms done | सर्व {n} खोल्या पूर्ण |
| app.x_wishes | {n} wishes | {n} इच्छा |
| album.x_changes_not_sent | {n} changes not sent | {n} बदल पाठवले गेले नाहीत |
| app.x_works | {n} works | {n} कामे |
| phase.scope_works_ticked_x | {n} works ticked | {n} कामे टिक केली |
| refuse.stage.room_unknown_x | S5 holds no survey of {room} yet; not saved | {room} चा सर्व्हे अजून S5 वर नाही; सेव्ह झाले नाही |

- Changed (stage A): tour.phases.7.body now ends "…each room's works carry the project." / "…प्रत्येक खोलीतील कामे प्रोजेक्ट पुढे नेतात."
- Stage A's two placeholder texts (proto.ov.todo, proto.nr.todo) are gone with the placeholders.
- album.old_build_msg is now drawn (Needs a room). refuse.stage.room_unknown_x is still unreachable from the sheet.

## Open questions drawn at their recommended option — all eight, and the card says so
1. One folded row for works outside a room's scope.
2. The grid (sketch A) with two-line work rows.
3. The project line counts rooms.
4. Only a room S5 holds carries works (Passage is the counter-example).
5. No state on a work outside the scope.
6. A vendor's works are per project, meeting each room's scope.
7. The finished plan is a second view inside the overview.
8. A staff-only Needs a room list with one guarded "no room → a room" write.

## Where §3 was not followed to the letter, and why
1. **The Album's "Open:" line is not drawn** (3.6's table sends it to the overview's head; 3.5's own list does not have it). The sixteen work lines already say which works are in progress by the one roll-up rule; a separate list would need a second rule and could disagree with them. The card says so.
2. **Overview head placement.** The bar's sub-line carries "Approved scope · 36 works · 4 rooms"; the project line sits in the head, as the Album had it.
3. **Plan view is mock data.** The layout is seeded as "what S5 web arranged"; nothing on the phone or S5 produces it yet. A room too small for two lines shows sign and token only.
4. **Needs a room.** Tapping a photo opens Which room? rather than the viewer. Both the Project Manager and the Designer may give a room or dismiss; the note leaves "which staff" to RS2.
5. **Marks hint.** It reads this phone's survey prep when the room has one; otherwise seeded demo data standing in for S5's record. Without the seed the hint would not show out of the box.
6. **"Beside the sheet"** for the old list means above the ticks inside the sheet, in both steps.
7. **3.4's count of a new room's requests** is shown by the fold's chip and the overview's Change requests line, not by a dedicated screen. A room-level request is not drawn, as the note says.
8. **Landscape.** The overview is two columns (grid or plan; head, work lines, log). Needs a room is one list, four cards across.
9. **The overview reopens on Works** with the log folded each time.
10. **Demo change:** Sample wardrobe repair moved from Survey to Client approved to make Record walkable; its stored stage is dropped once on a browser that had one.
11. **Not drawn:** "The room keeps its works" on the capture delete's confirm (the note places it on S5's web page, and §3 does not list it). No new guided tour for the new screens.
12. Stage A's eight deviations still stand (capture cards open the capture screen for staff; Quote journey and "not filed" as foldable rows; the lock is simply "not approved"; the tile's roll-up on its own line; the capture sheet's title; Viewing as on every room screen; the demo re-seed; two project rows on one line in landscape).

## Verified on this exact md5
Log: /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/final-all.log
- **Walk** (77 steps × EN/MR × portrait/landscape, all four roles): 0 page errors, 0 sideways scroll, 0 failed assertions, no new one-line text cut. The only Latin left in Marathi is month names, room and SKU codes, "IST", a file path and "ImageMeter".
- **360 dp fit** with a room at 10/12, the token DBWA and the longest work name: no sideways scroll in EN and MR, portrait and landscape; the grid is exactly 360 dp.
- **Every screen sweep:** 132 screens per language (every project and room, four roles, both orientations, plus both project pages) and 22 under the v53 lens: 0 errors, 0 sideways scroll.
- **Against v62a:** 0 unexpected differences outside and under the lens; P0's rename dialogs and its Prototype test sheet are identical.
- **Tours:** all six complete with every anchor found (EN portrait, MR landscape; survey and phases also at the phone viewport; survey in EN landscape). The demo is back at Client approved afterwards.
- **Header:** 4th eyebrow line byte-identical; "0.3.318" exactly twice; exactly one build-time span.
- **Real-phone viewport** (360 × 780, touch), EN and MR: overview, Plan and Needs a room pass.
Screenshots: /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/shots/ — 328 files: b01–b77 (the walk, four variants each), c01–c03 (the fit check), p01–p04 (real-phone view). Stage A's shots are in /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/shots/stageA/.

---

# The review: six findings

- **major** — The demo shows rooms carrying works with no survey prep, against the rule open question 4 asks Amit to judge
- **minor** — The marks hint contradicts the project's own count on the same project
- **minor** — One of the room screen's three cases cannot be seen in the demo: before approval, photos under their work headings
- **minor** — Back from a room opened from the overview lands on the project screen, and the overview reopens on Works
- **minor** — Marathi project line: the trailing parts do not agree with खोली
- **minor** — Needs a room: the project's one line is cut in the bar, and both new pages cut the project name in the title

# The fix (all six confirmed)

MOCK-UP v62b AFTER REVIEW 1 — all six findings confirmed and fixed, none declined. Nothing published.

Page: /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/index61c2.html
md5 c40931a8c681802396e2276048d51ac4 (was bc84d85d80a5149cec275cd206a6788d), stamp "12 Nov 2026, 02:58 IST".
Rebuild in one run from the untouched base (md5 3f481eaa...): python3 /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/build88.py "12 Nov 2026, 02:58 IST" — the same stamp gives the same md5 (checked).
Work log, drivers and the reviewed copies: /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/fix1/ (final-all.log, runall.sh, before/). Screenshots regenerated: /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/shots/ (b01–b78 in EN and MR, portrait and landscape).

== CONFIRMED AND FIXED (each driven on the reviewed page first, then on the rebuilt one) ==

1. MAJOR — known rooms looked unsurveyed. Confirmed: Living Room, Kitchen, Toilet 1 and Master Bedroom carried works under "Plan & elevations · no survey prep yet"; Demo bathroom door said "0 rooms surveyed" while Approve offered two rooms.
Fix: every room S5 knows now has, on this phone, a 360 whose survey prep is saved. Its tile reads e.g. "6 of 6 bounded · 3 marks · 3 read" and the chip opens. The seeded prep opens in survey prep and reads back the same.
- New surveyed 360s: Living Room, Kitchen, Toilet 1, Master Bedroom, and Master Bedroom of Sample wardrobe repair (same fault, not in the finding). The two existing Demo bathroom door 360s got a saved prep.
- Passage of Demo flat interiors stays the one unsurveyed room.
- Living Room keeps its two older 360s (one never surveyed, one no longer on this phone) beside the new surveyed one, because one of them is the only demo of "asks the laser sizes first". The card says so. Say if they should go.
- Mechanism: one small function added to survey prep (seed62), which stores a demo prep in the same memory any document-less capture uses.
- Demo bathroom door's strip now counts "2 rooms surveyed".

2. MINOR — marks hint contradicted the strip. Confirmed ("0 marks in Scope" against hints adding to 3).
Fix: one source. The three marks in Scope are real marks in the two rooms' seeded preps. The strip reads "3 marks in Scope", the hints read the same preps (Bathroom: Doors & win. 1 · Carpentry 1; Passage: Painting 1). The separate seed list and the core's marksOf are deleted.

3. MINOR — the before-approval case could not be seen. Confirmed.
Fix: Demo bathroom door → Bathroom has one photo an older build tagged "Door & window frames (chowkat)". It files at Civil & masonry before approval. The room shows the lock line, then CIVIL & MASONRY · 1 photo over QUOTE JOURNEY · 1 photo, with no state or change-request control. Named in the card's "The demo, to walk".

4. MINOR — Back from an overview-opened room landed on the project. Confirmed.
Fix: ◁ in a room opened from the grid or the Plan returns to the overview, on Works or Plan as it was left (also after switching to a sibling room). A room opened from its tile still goes back to the project; a breadcrumb or tab follows the tree and forgets the overview. In the card and in "Where every tap goes".

5. MINOR — Marathi trailing parts did not agree with खोली. Confirmed.
Fix: "1 थांबवलेली" / "{n} थांबवलेल्या", "1 सोडून दिलेली" / "{n} सोडून दिलेल्या". The demo line reads "अंमलबजावणी · 2 खोल्या चालू · 1 पूर्ण · 1 थांबवलेली".

6. MINOR — bar lines cut. Confirmed.
Fix: titles are "All rooms" and "Needs a room", with the project name on the second line; neither is cut in either language or orientation. The overview's head now carries the project line and, under it, "Approved scope · 36 works · 4 rooms", both free to wrap. Needs a room carries no project line.

== BEYOND THE FINDINGS (caused by fix 1 and 2) ==
- The project screen's "Marks:" line now shows on Demo bathroom door ("Marks: 2 existing · 3 scope"). Version 87's dictionary had no Marathi for its counts, so it stayed English under मराठी. Eight counts added, in the states' existing words; they sit in their own table in the card.
- The IN REVISION strip still cuts its text after "2 rooms…" at 360 dp (sooner in landscape), as in v62a. I did not change the strip; the card says so.
- Every demo 360 shows the same picture, so seeded marks sit by their figures, not on a door in the photo. The card says so.
- Vendor and Client also see the open "Plan & elevations" chip on the surveyed rooms. That was already so for Guest Bedroom; it is now on five more rooms.

== VERIFICATION on c40931a8 ==
- State driver, 78 states × EN/MR × portrait/landscape: 0 failures, 0 page errors, 0 sideways scroll, 0 cut texts. Under मराठी the only Latin left is dates, room tokens, SKU codes and a file path. New checks cover all six findings.
- Overview fit (10/12 room, four-letter token, longest work name): OK in all four.
- Regression against Version 87 with demo data put back: 0 unexpected differences, with and without the v53 lens.
- Every project × every room × four roles × both orientations: 132 screens per language without the lens, 28 with; 0 sideways, 0 errors.
- Roles driver (PM, Designer, Vendor, Client) on chips, bars, head and Back, in all four combinations: 0 failures.
- Six tours: every step shown, no anchor missing, wrote=0.
- Real-phone view (360 × 780, no frame): OK in EN and MR.
- Header: eyebrow 4th line byte-identical; 0.3.318 exactly twice; one build-time span; "mock-up v62b"; the 18 code lines of v62a's P0 rename dialog are byte-identical.
- The notes card has 26 items; its three text tables hold 50, 8 and 3 rows.

== TEXTS (key · EN · MR) ==
Added in this pass:
app.on_hold_1 · 1 on hold · 1 थांबवलेली
app.abandoned_1 · 1 abandoned · 1 सोडून दिलेली
life.x_exist · {n} existing · {n} सध्याचे
life.x_wish · {n} wish · {n} इच्छा
life.x_vrev · {n} V review · {n} व्हें. तपासणी
life.x_vok · {n} V approved · {n} व्हें. मंजूर
life.x_vadd · {n} V added · {n} व्हें. जोडले
life.x_cok · {n} C approved · {n} ग्रा. मंजूर
life.x_scope · {n} scope · {n} स्कोप
life.x_drop · {n} dropped · {n} वगळले
Changed in this pass (Marathi only):
app.x_on_hold · {n} on hold · {n} थांबवलेले → {n} थांबवलेल्या
app.x_abandoned · {n} abandoned · {n} सोडून दिलेले → {n} सोडून दिलेल्या
Reused, no new key: "All rooms" (export.all_rooms · सर्व खोल्या), "Approved scope", "none recorded".

Unchanged from the reviewed build (48 new + 1 changed):
album.all_rooms_works · All rooms · works · सर्व खोल्या · कामे
album.needs_room · Needs a room · खोली ठरवणे बाकी
album.change_not_sent_1 · 1 change not sent · 1 बदल पाठवला गेला नाही
album.no_room_album · In the album with no room · अल्बममध्ये, खोली न ठरवलेले
album.not_sent_phone · Not sent from this phone · या फोनवरून पाठवले गेले नाही
album.old_build_msg · Recorded for the whole project by an older build. States are per room now — set it in the room. · जुन्या बिल्डने हे संपूर्ण प्रोजेक्टसाठी नोंदवले होते. आता स्थिती खोलीनुसार असते — ती त्या खोलीत ठरवा.
album.no_scope_yet · no scope yet · अजून स्कोप नाही
room.not_on_s5_save_prep · Not on S5 yet — save this room's survey prep to give it works · अजून S5 वर नाही — या खोलीला कामे मिळण्यासाठी तिची सर्व्हे तयारी सेव्ह करा
room.no_works_in_scope · No works in this room's approved scope yet · या खोलीच्या मंजूर स्कोपमध्ये अजून कोणतेही काम नाही
room.not_in_scope · Not in this room's scope · या खोलीच्या स्कोपमध्ये नाही
room.not_in_scope_lc · not in this room's scope · या खोलीच्या स्कोपमध्ये नाही
phase.scope_rooms_title · Rooms in the approved estimate · मंजूर अंदाजपत्रकातील खोल्या
phase.scope_no_works_ticked · no works ticked · कोणतेही काम टिक केलेले नाही
phase.scope_work_ticked_1 · 1 work ticked · 1 काम टिक केले
app.work_1 · 1 work · 1 काम
phase.scope_no_rooms · No room of this project is on S5 yet — save a room's survey prep first · या प्रोजेक्टची एकही खोली अजून S5 वर नाही — आधी एखाद्या खोलीची सर्व्हे तयारी सेव्ह करा
app.room_in_progress_1 · 1 room in progress · 1 खोली चालू
app.room_done_1 · 1 room done · 1 खोली पूर्ण
app.room_on_hold_1 · 1 room on hold · 1 खोली थांबवलेली
app.room_abandoned_1 · 1 room abandoned · 1 खोली सोडून दिलेली
app.wish_1 · 1 wish · 1 इच्छा
album.works · Works · कामे
album.not_in_scope_lg · not in scope · स्कोपमध्ये नाही
album.change_request_lg · change request · बदल विनंती
album.not_in_any_scope · not in any room's scope · कोणत्याही खोलीच्या स्कोपमध्ये नाही
album.recorded_whole_project · recorded for the whole project · संपूर्ण प्रोजेक्टसाठी नोंदवलेले
album.plan_from_s5 · Arranged on S5 web, read-only here · S5 वेबवर मांडलेला, इथे फक्त पाहण्यासाठी
album.plan_none · The flat's plan shows here once its rooms are arranged on S5 web · S5 वेबवर खोल्या मांडल्या की फ्लॅटचा प्लॅन इथे दिसेल
album.not_on_plan · Not on the plan yet: · अजून प्लॅनवर नाही:
album.nothing_needs_room · Nothing needs a room · खोली ठरवायचे काही बाकी नाही
album.not_sent · not sent · पाठवला नाही
album.dismiss · Dismiss · यादीतून काढा
phase.scope_old_list · The scope an older build recorded for the whole project, for reference: · जुन्या बिल्डने संपूर्ण प्रोजेक्टसाठी नोंदवलेला स्कोप, संदर्भासाठी:
phase.scope_marks_hint · This room's marks in scope: · या खोलीचे स्कोपमधील मार्क:
app.x_rooms_in_progress · {n} rooms in progress · {n} खोल्या चालू
app.x_rooms_done · {n} rooms done · {n} खोल्या पूर्ण
app.x_rooms_on_hold · {n} rooms on hold · {n} खोल्या थांबवलेल्या
app.x_rooms_abandoned · {n} rooms abandoned · {n} खोल्या सोडून दिलेल्या
app.all_x_rooms_done · all {n} rooms done · सर्व {n} खोल्या पूर्ण
app.x_wishes · {n} wishes · {n} इच्छा
album.x_changes_not_sent · {n} changes not sent · {n} बदल पाठवले गेले नाहीत
app.x_works · {n} works · {n} कामे
phase.scope_works_ticked_x · {n} works ticked · {n} कामे टिक केली
refuse.stage.room_unknown_x · S5 holds no survey of {room} yet; not saved · {room} चा सर्व्हे अजून S5 वर नाही; सेव्ह झाले नाही
album.x_of_x_rooms_done · {k} of {n} rooms done · {n} पैकी {k} खोल्या पूर्ण
album.photo_filed_in_x · Photo filed in {room} · फोटो "{room}" मध्ये ठेवला
album.photo_sent_to_x · Photo sent to {room} · फोटो "{room}" मध्ये पाठवला
phase.scope_copy_from_x · Copy the ticks from {room} · {room} च्या टिक कॉपी करा
tour.phases.7.body · changed: "…the Album's works carry the project" → "…each room's works carry the project" (अल्बममधील कामे → प्रत्येक खोलीतील कामे)
The same list is in /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto88/work/texts.txt.

== HOW AMIT REACHES EACH STATE BY HAND (Browse → Demo Client → Demo Site, Pune) ==
- Project in execution: Demo flat interiors. Tiles: GB "no scope yet", KIT In progress 1/10, LR In progress 2/8, MB Done 8/8, T1 On hold 1/10; +1 shows Passage.
- Overview: All rooms · works ▸. Works and Plan chips; Work log at the bottom. Tap a room's row or a room on the Plan, then ◁ returns to the overview in the same view; ◁ again is the project.
- Approved scope: tap the project line → APPROVED SCOPE → tap to view (opens the overview).
- Needs a room: the amber row → Which room? on a photo; Dismiss on a refused change. When empty the row leaves the project screen.
- Room in execution: Living Room. Work rows with state ▾ (state sheet; leaving Done needs a reason); Raise change request; Quote journey (three 360s); not filed to a work; folded "Not in this room's scope" with the live add request; Work log. Capture 360 → WORK row, Photo (progress shot, no room row), 360.
- Vendor photo and review: Toilet 1 → Waterproofing (one awaiting review: Accept / Not for client); the live change request → Record the client's answer.
- Decided change request: Kitchen → Countertops.
- Room with no scope yet, and a new room after approval: Guest Bedroom → folded row → Raise change request · add, one per work.
- Room S5 does not know: +1 → Passage ("Not on S5 yet…", chip greyed).
- Surveyed room: any known room → Plan & elevations, or its 360 tile under Quote journey.
- Before approval: Demo bathroom door → Bathroom (lock line, CIVIL & MASONRY · 1 photo, QUOTE JOURNEY · 1 photo). Its project screen shows the Marks line, "3 marks in Scope", and tiles "2 wishes" / "1 wish".
- Approve per room: Demo bathroom door → Approve the estimate ▸ → a room → ticks, the marks hint, Copy the ticks from …, Back → Move to Client approved. With nothing ticked it is refused.
- Approved before scopes were per room: Sample Customer → Sample wardrobe repair → project line → Record the approved scope (old list shown beside, nothing pre-ticked). Its overview's Work log shows "recorded for the whole project".
- Roles: "Viewing as" at the top of any room screen and of the overview. For Vendor (Demo Waterproofing Co.) open Toilet 1.
- Language and orientation: the page's own switches.

== OPEN QUESTIONS DRAWN AT THEIR RECOMMENDED OPTION (all still Amit's to rule; listed in the card) ==
1. Works outside a room's scope: one folded row, greyed, each with "Raise change request · add" and any photos filed there.
2. Overview shape: the grid, rooms down and sixteen works across, with a two-line row per work (the list is not built).
3. The project line counts rooms.
4. Option (a): only a room S5 holds carries works. The demo now shows this consistently; Passage is the counter-example.
5. No state on a work outside the room's scope until an add request is approved.
6. A vendor's works are per project, meeting each room's scope.
7. The finished whole-flat plan: a second read-only "Plan" view inside the overview.
8. Album photos with no room: the staff-only "Needs a room" list, with one guarded re-file (no room → a room).
Also proposed, to keep or strike on screen: the finished rule, Copy the ticks, the marks hint, the overview's bar and head, "recorded for the whole project", the old list beside Record, and Back to the overview. The card states that nothing in it is approved and nothing is on the phone or S5.
