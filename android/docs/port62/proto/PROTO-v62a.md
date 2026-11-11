# Prototype v62a (Version 87) — P0 and P2, FOR AMIT'S APPROVAL

Published 18:4x IST, 11 Nov 2026, at https://claude.ai/artifact/64peToYoeaRz9UNwB926m5 (Version 87, page md5 3f481eaa…).
Neither item is on the phone. P0 = the rename dialog says why a rename could not be saved (port 62 plan §2);
P2 = the room screen opens the room's floor plan and elevations in one tap (plan §4, Amit 11 Nov: "it should be
on the room"). `build87.py` beside this file rebuilds the page from the Version 86 base by substitution; ONE
post-step was applied by hand after it: in the #ch62a card, "APK 0.3.318 still sends…" became "the phone's build
still sends…", because the header refresh requires the build to be named exactly twice on the page.
The header's 4th line is unchanged (look 4/0). The reports below are the builder's and the fixer's, verbatim.

---

# Prototype v62a (P0 + P2) — built and verified, not published

P0 and P2 are built into `/tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto87/index61c2.html` (md5 `d11af3ea68a169a90931ecb82d334b2c`, 9,657 lines). The base is untouched (`5aa0a190210faa15822716293aa7c071`). Driven at 360 × 780 and 780 × 360 in English and मराठी: zero page errors, the 4th eyebrow line byte-identical, and the plan / wish / vendor / survey tours all complete.

Three choices are mine and are for you to confirm or redirect; each is explained in section 5:
- **P2 placement:** a chip row at the top of the room screen, not a second button beside Capture 360.
- **P0 demo switch:** the page had no offline affordance, so a site's rename dialog ends with a "Prototype test" row.
- **Version label:** eyebrow line 2 now reads "mock-up v62a" and line 3 the build time (11 Nov 2026, 17:59 IST).

## 1. What changed (line numbers in the new file)

| Lines | What |
|---|---|
| 1596–1602 | CSS: `.otf.dis` only, the name field shown but not editable (P0 c), in the page's own disabled tones |
| 1607 | `#changes` line gains "v62a: … not yet approved, not on the phone" |
| 1614 | Eyebrow lines 2 and 3; line 4 untouched (asserted in the build, then byte-diffed) |
| 1689 | `.simnote` names the Prototype test row |
| 1710, 1712 | "Where every tap goes": the rename bullet and the room bullet |
| 1722–1740 | New notes card `#ch62a`, "Changed from v61c.2 — to approve": says neither item is approved or on the phone, what each does, Kotlin / S5 needs, and both text tables |
| 2118 | `RAW` dictionary: 25 keys added in key order (23 in `s`, 2 in `t`); nothing changed or removed |
| 6950 | Survey prep `open(o)`: `o.tab==='plan'` opens on the Plan tab, Floor drawing, fitted |
| 7720 | State: `rnTest`, `rnSheet` |
| 7951–7978 | P0 block: `RN62`, `RN62WHY`, `RN62S5`, `rn62on`, `rn62n`, `rn62opts`, `rn62mode`, `rn62Top`, `rn62Sheet` |
| 7979–7994 | `renameDialog()`: red reason line under the field, S5 grey line, not-editable form, Prototype test row, sheet |
| 7999–8009 | `rn62Save()`: what a site's Save meets |
| 8010–8015 | `doRename()`: one line added (8014) routing a site through `rn62Save` |
| 8118, 8120 | `openSurvey(id,{pending,tab})` passes `tab` on |
| 8229–8244 | P2 block: `size62`, `planCap62`, `openPlan62`, `planRow62` |
| 8255 | `roomScreen()` calls `planRow62` |
| 8427 | `goBack()`: Android Back closes the Prototype test sheet first |

A client's and a project's rename dialog, the delete half of every dialog, and everything under the v53 lens are unchanged.

## 2. New texts

**For the phone, to approve (9):**

| key | EN | MR |
|---|---|---|
| `app.site_on_s5_so_new` | This site is on S5, so the new name is saved there and reaches the web and every other phone. Photos already filed under it keep their place — only the label changes. | ही साइट S5 वर आहे, त्यामुळे नवीन नाव तिथे सेव्ह होते आणि वेबवर व इतर प्रत्येक फोनवर पोहोचते. आधीच त्याखाली ठेवलेले फोटो जागीच राहतात — फक्त नाव बदलते. |
| `refuse.site.offline` | Offline — the name changes only when S5 answers; not saved | ऑफलाइन — S5 उत्तर देईल तेव्हाच नाव बदलते; सेव्ह झाले नाही |
| `refuse.site.signed_out` | Not signed in to S5, so not saved — sign in (menu → Server) and Save again | S5 मध्ये साइन इन केलेले नाही, म्हणून सेव्ह झाले नाही — साइन इन करा (मेनू → सर्व्हर) आणि पुन्हा सेव्ह करा |
| `refuse.site.renamed_x` | Renamed by {by} meanwhile — now {site}; not saved | मधल्या काळात {by} यांनी नाव बदलले — आता {site}; सेव्ह झाले नाही |
| `refuse.site.part_x` | Renamed on {done} of {of} projects — Save again to finish | {of} पैकी {done} प्रोजेक्टवर नाव बदलले — पूर्ण करण्यासाठी पुन्हा सेव्ह करा |
| `refuse.site.capture_on_way` | A capture of this site has not reached S5 yet — rename the site once it has; not saved | या साइटचा एक कॅप्चर अजून S5 वर पोहोचलेला नाही — तो पोहोचल्यावर साइटचे नाव बदला; सेव्ह झाले नाही |
| `app.site_not_renamed_here` | This site cannot be renamed from this phone: S5 does not hold it and it was not made here. | या फोनवरून या साइटचे नाव बदलता येत नाही: ती S5 वर नाही आणि इथे बनवलेलीही नाही. |
| `room.plan_elevations` | Plan & elevations | प्लॅन व एलिव्हेशन |
| `room.no_survey_prep_yet` | no survey prep yet (shown as "Plan & elevations · no survey prep yet") | अजून सर्व्हे तयारी नाही |

**Prototype test row only, never on the phone (16):**

| key | EN | MR |
|---|---|---|
| `proto.rn.test` | Prototype test | प्रोटोटाइप चाचणी |
| `proto.rn.title` | This rename — prototype test | हा नाव-बदल — प्रोटोटाइप चाचणी |
| `proto.rn.ok` / `.sup` | Saves / the new name is saved | सेव्ह होते / नवीन नाव सेव्ह होते |
| `proto.rn.offline` / `.sup` | No signal / nothing reaches S5 | नेटवर्क नाही / S5 पर्यंत काहीही पोहोचत नाही |
| `proto.rn.signedout` / `.sup` | Signed out of S5 / no S5 sign-in on this phone | S5 मधून साइन आउट / या फोनवर S5 चे साइन इन नाही |
| `proto.rn.refused` / `.sup` | S5 refuses / somebody renamed the site meanwhile | S5 नाकारते / मधल्या काळात कोणीतरी साइटचे नाव बदलले |
| `proto.rn.partial` / `.sup` | Lands on 1 project only / this site has more than one project on S5 | फक्त 1 प्रोजेक्टवर पोहोचते / या साइटचे S5 वर एकापेक्षा जास्त प्रोजेक्ट आहेत |
| `proto.rn.uploading` / `.sup` | A capture still on its way / a 360 of this site has not reached S5 yet | एक कॅप्चर अजून वाटेत / या साइटचा एक 360 अजून S5 वर पोहोचलेला नाही |
| `proto.rn.blocked` / `.sup` | Cannot be renamed here / not on S5 and not made on this phone | इथे नाव बदलता येत नाही / S5 वर नाही आणि या फोनवर बनवलेली नाही |

## 3. How to reach each state by hand

**P0** — Clients → Demo Client → hold **Demo Site, Pune** (two projects on S5):
- **Grey line (b):** shows as soon as the dialog opens.
- **Prototype test row:** at the foot of the dialog; tap it for the sheet of seven picks. In landscape the dialog is short, so scroll it to reach the row.
- **No signal:** pick No signal → type a new name → Save → "Saving…" → red line under the name, dialog stays open, nothing renamed.
- **Line clears:** type in the field, or Cancel and hold the site again.
- **Signed out of S5:** pick it → Save → the line appears at once (nothing is sent).
- **S5 refuses:** pick it → Save → "Renamed by Priya Shinde meanwhile — now Demo Site, Baner; not saved". Save again goes through.
- **1 of 2:** pick Lands on 1 project only → Save → "Renamed on 1 of 2 projects — Save again to finish". Save again renames the site and closes the dialog.
- **Capture on its way (d):** pick A capture still on its way → Save.
- **Cannot be renamed (c):** pick Cannot be renamed here → name greyed and not editable, Save off, reason in the grey line's place.
- **A site not on S5:** hold **Demo Office, Pune** → its old "has not synced yet" line; the row offers three picks only (Saves, A capture still on its way, Cannot be renamed here).
- The pick is kept until the page is reloaded; set it back to Saves afterwards.

**P2** — Clients → Demo Client → Demo Site, Pune → Demo flat interiors:
- **Guest Bedroom:** teal **Plan & elevations** chip under the crumbs → one tap opens survey prep on its Plan tab, Floor drawing, with the Ceiling / Front / Right / Back / Left chips. ← or Android Back returns to the room.
- **Living Room** (360s with no survey prep) and **Kitchen** (+N tile → Kitchen, no captures): the chip is greyed, reads "Plan & elevations · no survey prep yet", and is not a tap.
- **Every phase:** move the project to Wish list on the strip and open Guest Bedroom; Capture 360 is hidden, the chip stays.

## 4. Verification

- **New states:** 24 screenshots for each of EN/MR × portrait/landscape, plus 4 in the no-frame phone view, in `/tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto87/shots/` (100 files, named `p0-…` / `p2-…-<lang>-<orientation>.png`). 25 asserted checks per run (28 in मराठी) all pass.
- **Marathi:** no new text is left in English, and the page's own overflow probe finds nothing cut on the new screens.
- **Unchanged screens:** 24 screens compared against Version 86 in five configurations. Only the room screen's new row and a site's rename dialog differ; under the v53 lens nothing differs.
- **Tours:** plan 6/6, wish 13/13, vendor 12/12 and survey 12/12 complete in six runs (EN and MR × phone view, portrait frame, landscape frame). All seven tours also completed on an earlier build that differed only in notes text and the dialog's scroll-to-top.
- **Housekeeping:** I stopped my own server; nothing was published. The build is repeatable from `/tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto87/work/build87.py`; drivers are in `/tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto87/work/scripts/`.

## 5. What is not exactly as §2 / §4 word it, and why

1. **P2 placement is a chip row, not the Capture 360 slot.** The room screen has two places for controls: the Capture 360 button and the chip row. I used a chip row because:
   - Capture 360 is hidden in four of six phases, so that slot comes and goes.
   - The page has no "not yet" form for that button; the greyed chip already exists ("2 rooms shot").
   - The project screen's own entry to a page is a chip (Wish list).

   The catch: a room with one work group gains a 44 dp row. If you meant a second button beside Capture 360, only `planRow62` and line 8255 change.
2. **"Current capture"** is the newest 360 on the phone that has a saved survey prep. An older 360's drawings stay under its own Plan tab, and the chip does not say which capture it opens.
3. **One greyed label covers three cases:** no 360, a 360 not yet prepped, and a 360 removed from the phone. For the last, "yet" is loose.
4. **Demo switch.** The page had no offline or no-signal affordance. I added the Prototype test row inside a site's rename dialog, modelled on the album's "Viewing as — prototype test". It is recorded in the notes card, the simnote and the tap map.
5. **"S5's refusal, shown as S5 sent it."** There is no S5 here, so the line is A1's 409 wording with invented demo values. In मराठी it is keyed and translated, as the page's other S5 refusals are. A1's 403, 404 and 400 are not drawn.
6. **(c) is drawn as the line, not as "no rename offered".** The dialog opens with the name not editable and the reason shown; the notes card says the other choice is not drawn. This needed the one new CSS rule, `.otf.dis`.
7. **Only a site's dialog changes.** The plan (R2) says a client's and a project's refusals also show in the dialog; P0's four items are about the site, so those are not drawn.
8. **The delete half still says "ERP"** ("Deleting removes it from ERP for everyone", the MEST-SITE id), also on a site S5 holds. It is outside (a)–(d), so I left it; worth a decision before A2.
9. **Left as found:** the old मराठी line for a not-synced site ("हा साइट…", the shared `{kind}` template) and the feedback panel's `VERSION='v60'`.

---

# The review (2 findings) and its fix

Both findings are confirmed and fixed in /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto87/index61c2.html (new md5 10b5c830d5fd974c09cf94e72629e3af, was d11af3ea68a169a90931ecb82d334b2c). The optional part of finding 2 is declined. Nothing was published; the Version 86 base is untouched (5aa0a190210faa15822716293aa7c071).

## Confirmed and fixed

**1. Marathi gender of कॅप्चर — confirmed by reading the dictionary.** Every existing text treats कॅप्चर as neuter ("हे कॅप्चर हटवा", "कॅप्चर हटवले", "त्यांचे कॅप्चर … पोहोचलेले नाहीत"); only the new line was masculine. `refuse.site.capture_on_way` now reads "या साइटचे एक कॅप्चर अजून S5 वर पोहोचलेले नाही — ते पोहोचल्यावर साइटचे नाव बदला; सेव्ह झाले नाही", in the RAW dictionary (line 2118) and in the `#ch62a` table (line 1738). `proto.rn.uploading.sup` stays masculine, as the page uses 360 ("काढलेला 360").

**2. Landscape dialog height — confirmed by driving at 780 x 360, EN and MR.**
- Version 86: the site dialog's body is 251 in 251.
- This build at open: 303 in 252, with the Prototype test row out of view.
- With a reason: 323 in 252, Delete cut by 19 px.
- Without the prototype-only row (52 px), the phone's own body would be 271 against 252.
- Each of the five reasons is one line (20 dp) in landscape, in both languages.
- The phone's `RenameDialog` (`TreeShell.kt`) is a plain `Column` with no scroll, so this is a real Kotlin need.

Fix: the "Kotlin needs" bullet of `#ch62a` (line 1733) gains: "In landscape (780 × 360) the site's dialog has no room to spare: its body fills its space exactly today, so the reason line makes it taller than that space — by 20 dp for one line, which is what each of the five reasons takes there in English and in Marathi, and by more when a long name wraps it. So the dialog's body must scroll, and it comes back to the top, with the name field and the reason in view, each time a reason is set — as this page does."

## Declined

**Moving the Prototype test row under the grey line in landscape** (the reviewer's optional suggestion, called a builder's choice). Reasons:
- It would put a prototype-only control between two phone elements, so the landscape dialog would stop being the phone's layout in the phone's order.
- It would give the row two positions, one per orientation.
- The notes card already says the row is reached by scrolling in landscape, and nothing is clipped or lost.

## What changed in the file

Four lines differ from the reviewed build, and nothing else:
- 1614: the build stamp, "11 Nov 2026, 17:59 IST" → "18:36 IST".
- 1733: the Kotlin-needs sentence.
- 1738 and 2118: the Marathi line.

The eyebrow's 4th line is byte-identical to Version 86. The edits were made in `proto87/work/build87.py` and the page rebuilt; the unedited script reproduced d11af3ea… first. The pre-fix page is kept at /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/proto87/work/index61c2.before-fix.html.

## Re-drive

Served on my own port (47507, stopped afterwards; no server left running). Scripts, logs and shots are in /tmp/claude-0/-home-user/a41cc405-6e3e-5125-9e98-bc0f06d00743/scratchpad/fix87/.
- **P0, EN and MR, portrait and landscape:** 0 page errors. Against the reviewer's reports the only non-numeric difference is the corrected Marathi text; the numeric differences are constant vertical page-scroll offsets. The new line is 3 lines in portrait and 1 in landscape, as before, and the dialog stays in frame.
- **P2, EN and MR, portrait and landscape:** reports identical to the reviewer's.
- **Marathi overflow check, portrait and landscape:** nothing cut in the dialog, any of the five reasons, the blocked form, the sheet, or the room screens.
- **Notes card at 360 and 1280:** no horizontal overflow.
- **Base-versus-build regression, EN and MR, both orientations:** the only DOM differences are in the site rename dialog.
- **Four tours (survey 12/12, wish 13/13, vendor 12/12, plan 6/6):** run in EN and MR at phone 360 x 780, portrait frame and landscape frame. All steps shown, no anchor missing, all ended done with wrote=0, 0 page errors; steps, titles, anchors and holes match the pre-fix run.

## Final table of new texts

For the phone, to approve (9):

| key | EN | MR |
|---|---|---|
| `app.site_on_s5_so_new` | This site is on S5, so the new name is saved there and reaches the web and every other phone. Photos already filed under it keep their place — only the label changes. | ही साइट S5 वर आहे, त्यामुळे नवीन नाव तिथे सेव्ह होते आणि वेबवर व इतर प्रत्येक फोनवर पोहोचते. आधीच त्याखाली ठेवलेले फोटो जागीच राहतात — फक्त नाव बदलते. |
| `refuse.site.offline` | Offline — the name changes only when S5 answers; not saved | ऑफलाइन — S5 उत्तर देईल तेव्हाच नाव बदलते; सेव्ह झाले नाही |
| `refuse.site.signed_out` | Not signed in to S5, so not saved — sign in (menu → Server) and Save again | S5 मध्ये साइन इन केलेले नाही, म्हणून सेव्ह झाले नाही — साइन इन करा (मेनू → सर्व्हर) आणि पुन्हा सेव्ह करा |
| `refuse.site.renamed_x` | Renamed by {by} meanwhile — now {site}; not saved | मधल्या काळात {by} यांनी नाव बदलले — आता {site}; सेव्ह झाले नाही |
| `refuse.site.part_x` | Renamed on {done} of {of} projects — Save again to finish | {of} पैकी {done} प्रोजेक्टवर नाव बदलले — पूर्ण करण्यासाठी पुन्हा सेव्ह करा |
| `refuse.site.capture_on_way` (changed) | A capture of this site has not reached S5 yet — rename the site once it has; not saved | या साइटचे एक कॅप्चर अजून S5 वर पोहोचलेले नाही — ते पोहोचल्यावर साइटचे नाव बदला; सेव्ह झाले नाही |
| `app.site_not_renamed_here` | This site cannot be renamed from this phone: S5 does not hold it and it was not made here. | या फोनवरून या साइटचे नाव बदलता येत नाही: ती S5 वर नाही आणि इथे बनवलेलीही नाही. |
| `room.plan_elevations` | Plan & elevations | प्लॅन व एलिव्हेशन |
| `room.no_survey_prep_yet` | no survey prep yet | अजून सर्व्हे तयारी नाही |

Prototype test row, never on the phone (16, unchanged):

| key | EN | MR |
|---|---|---|
| `proto.rn.test` | Prototype test | प्रोटोटाइप चाचणी |
| `proto.rn.title` | This rename — prototype test | हा नाव-बदल — प्रोटोटाइप चाचणी |
| `proto.rn.ok` | Saves | सेव्ह होते |
| `proto.rn.ok.sup` | the new name is saved | नवीन नाव सेव्ह होते |
| `proto.rn.offline` | No signal | नेटवर्क नाही |
| `proto.rn.offline.sup` | nothing reaches S5 | S5 पर्यंत काहीही पोहोचत नाही |
| `proto.rn.signedout` | Signed out of S5 | S5 मधून साइन आउट |
| `proto.rn.signedout.sup` | no S5 sign-in on this phone | या फोनवर S5 चे साइन इन नाही |
| `proto.rn.refused` | S5 refuses | S5 नाकारते |
| `proto.rn.refused.sup` | somebody renamed the site meanwhile | मधल्या काळात कोणीतरी साइटचे नाव बदलले |
| `proto.rn.partial` | Lands on 1 project only | फक्त 1 प्रोजेक्टवर पोहोचते |
| `proto.rn.partial.sup` | this site has more than one project on S5 | या साइटचे S5 वर एकापेक्षा जास्त प्रोजेक्ट आहेत |
| `proto.rn.uploading` | A capture still on its way | एक कॅप्चर अजून वाटेत |
| `proto.rn.uploading.sup` | a 360 of this site has not reached S5 yet | या साइटचा एक 360 अजून S5 वर पोहोचलेला नाही |
| `proto.rn.blocked` | Cannot be renamed here | इथे नाव बदलता येत नाही |
| `proto.rn.blocked.sup` | not on S5 and not made on this phone | S5 वर नाही आणि या फोनवर बनवलेली नाही |

## How to reach each new state by hand

Language: menu → भाषा · Language. Orientation: the portrait / landscape switch beside the phone frame.

**P0 — the site rename dialog**
1. Browse → Demo Client → press and hold "Demo Site, Pune". "Rename or remove site" opens with the S5 grey line.
2. The Prototype test row is at the foot of the dialog; in landscape, scroll the dialog body down to it. Tap it for the sheet "This rename — prototype test".
3. Pick a case, type a new name (for example "Demo Site, Kothrud"), tap Save:
   - **Saves:** the site is renamed.
   - **No signal:** "Saving…", then the red offline line under the outlined field.
   - **Signed out of S5:** the red line at once, nothing sent.
   - **S5 refuses:** "Renamed by … meanwhile — now …; not saved". It happens once; Save again goes through.
   - **Lands on 1 project only:** "Renamed on 1 of 2 projects — Save again to finish". Once; Save again finishes. Offered on Demo Site, Pune, which has two projects on S5.
   - **A capture still on its way:** the corrected line. It refuses every time until another case is picked.
   - **Cannot be renamed here:** the name is shown but not editable, Save is off, and the reason stands in the grey line's place.
4. Typing in the field, Cancel, or a tap outside clears the red line.
5. In landscape, after any failure the dialog returns to the top with the name field and reason in view; scroll down for Delete and the Prototype test row.
6. A site not on S5: hold "Demo Office, Pune". It keeps its "has not synced yet" line, and its sheet offers only Saves, A capture still on its way, and Cannot be renamed here.
7. The pick lasts until the page is reloaded. A client's and a project's dialogs have no row and are unchanged.

**P2 — the room's plan**
1. Demo Client → Demo Site, Pune → Demo flat interiors → Guest Bedroom. The "Plan & elevations" chip sits under the crumbs, above the work chips.
2. Tap it: survey prep opens on its Plan tab, on the Floor drawing, fitted. ← returns to the room.
3. A room with no survey prep (Living Room or Kitchen in the demo) shows the chip greyed, "Plan & elevations · no survey prep yet", and it is not a tap.
4. The chip is there in every phase, also where Capture 360 is hidden.

**Notes card:** the page's notes panel → "Changed from v61c.2 — to approve" (`#ch62a`). It says neither item is approved or on the phone, and carries the Kotlin / S5 needs and both text tables.

## One thing outside these findings, left unchanged

The base page's shared Marathi template for a not-synced row reads "हा साइट अजून सिंक झालेला नाही" (masculine), while the new S5 line says "ही साइट" (feminine). The two are never on screen together, and the masculine one is Version 86's and the phone's own text, so it was not touched.
