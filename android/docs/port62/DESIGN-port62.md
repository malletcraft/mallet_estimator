# DESIGN-port62 — Port 62: site name on S5, wish marks on the phone, per-room plans; S5-web arranger and RFQ (outlines)

**Dated 11 Nov 2026 (IST). S2 APK and S5 QUOTE; S6 ERP only as vanilla ERPNext seeded by config-as-code.**
A plan, not code. It follows `GAP-0.3.318.md` (same folder: the readers' line references, the twelve rename
defects R1–R12, the delta D1–D6) and takes its slice shape from `../port61c/DESIGN-port61c.md`.

- `P L<n>` = the approved prototype v61c.2 (Version 86), a built page of 9,563 lines, re-read for this plan.
  Phone paths `app/…` and `pano/…` as in the audit. S5 at `9a51733` (= `stage`). Sizes: S about a day, M two to
  four days, L a week or needs its own design first.
- **Rules that bind every slice.** The prototype is the spec for every APK screen: replicate, invent nothing; a
  state it does not draw goes on a **prototype question** list, never onto the phone. Texts come from
  `android/i18n` (a missing one is added from the prototype's own words, never coined); exports and wire values
  stay English (decision 4). `:pano` is pure JVM and tested here; `:app` compiles only in CI. S5 ships to `stage`
  first, then the phone. Each slice leaves the Play build usable. LEAN: one build agent, one review lens.

## 0. Amit's four answers (11 Nov 2026) and the scope split

1. First to the phone: **"Wish marks on photos"**.
2. Site name: **"Yes — S5 keeps the site name"** — the option as put to him: S5 stores each project's site name and
   gets a rename route; the phone renames through S5; ERP is not involved; a failed rename says why in the
   dialog, and that text goes through the prototype first.
3. RFQ: **"this RFQ stuff will be done on S5 an on computer webbrowser. lets not complicate apk for now."**
4. Floor plan: **"need floor plan per room the way current survey happens per room. arranging a floor plan will be
   done in s5 rather than apk. floor and wall elevations are required so that it will be a cleaner look to verify
   quickly. the whole project floor plan should be visible once its done on s5. each room floor plan should be
   visible inside that room only."**

His journey, the same day (whole in GAP §1): survey first; then on the same survey photos the client wish list
is MARKED (new wardrobe, POP, new dining table, "remove this window and install UPVC window", "close this door with
brickwork"); vendors review and quote; the wish list goes to MULTIPLE vendors by RFQ and the quotes are compared;
revisions against quoted amounts; the quote is finalised; work starts. "RFQ will be sent as plain 2d diagrams as a
document ... RFQ will be per line of service like painting, electric work, civil work, furniture work, doors and
windows, POP etc."

| Step of the journey | S2 APK (phone) | S5 QUOTE (web, on a computer) |
|---|---|---|
| Site name | renames through S5, says why on failure (§2) | stores it, one guarded rename route (§2) |
| Survey | as shipped | as shipped |
| Wish marks on the photos | **all of it:** state, Mark sheet, new types, drawn on face / 360 / List / drawings, the wish list counting them (§3) | shows them; redacts per caller (§3.11–3.13) |
| Vendor review, RFQ, quotes, comparison, revisions | **nothing new** (answer 3); the wish row's Vendor field stays as shipped | all of it (§6), recorded in ERPNext's standard Request for Quotation and Supplier Quotation |
| A room's plan and elevations | PENDING AMIT (§4) | drawn per room; needed by the RFQ document |
| The whole flat's plan | read-only, later | the arranger and the whole plan on the project page (§5) |
| Finalise, then work | as shipped (PM approves, scope, album) | as shipped; the award join comes with §6 |

**The 19 questions parked in `DESIGN-port61c.md`** (it does not number them; the count is this file's reading:
K3's five + K4's three + K5's seven + the tour's three + FW2's one):

| Parked at | n | After this direction |
|---|---|---|
| §18 K3, five album states (`:724-731`) | 5 | none superseded — still parked |
| §19 K4, queued / refused / other-login writes (`:1381-1387`) | 3 | none superseded |
| §20 K5, the seven from the viewfinder to the unsent single photo (`:2106-2130`) | 7 | none superseded |
| §31 the tour, "Held for Amit" (`:4820-4838`) | 3 | **Q1 superseded for `phases`** (slice L10 ports the band, so no step is cut) and narrowed for `existing` (L6 brings More ▸ by trade; only step 9's List chips stay missing). **Q2 grows**: under answer 3 the phone's Vendor review has no packs, so the tour texts need rewriting on the prototype anyway. Q3 (the 360 as a tab) stands, and matters more: wishes are marked on the 360 too |
| §16 FW2 review, finding 2 (`:273`: the photo line's `Survey` against the sheet row's `not on S5 yet`) | 1 | not superseded |

Of the audit's own six (GAP §5): Q3 is answered, so Q2 no longer decides the fix; Q1 is answered in part (the
arranger goes to S5; what he saw of the per-room plan is being asked, §4); Q4 ships as the prototype has it (rows
ticked by hand, counting the marks); Q5 and Q6 are §6's questions 1 and 2.

## 1. The slice table

"Play" = the Play build a slice rides (B1, B2 …); "(B2)" = merged with that build, nothing to see. B2 goes out
as soon as L3 is green, so **the first Play build after the site-name fix already lets him mark a wish**.

| # | id | slice | piece | size | needs | Play | What Amit sees when it ships |
|---|---|---|---|---|---|---|---|
| 1 | P0 | Prototype: the rename dialog says why it failed; its grey line for a site S5 holds | S2 APK (prototype) | S | — | — | On the prototype a rename that fails shows a red line under the name; he approves the words |
| 2 | A1 | S5 keeps the site name: migration 0011, `PUT …/site`, the list carries it | S5 QUOTE | S | — | stage | On /projects each project shows its site name once it has one |
| 3 | A2 | The phone renames a site through S5 and says why when it cannot | S2 APK | M | P0 approved; A1 on stage | **B1** | He holds a site, types a name, taps Save: it changes at once on the phone and the web; with no signal the dialog says so and stays open |
| 4 | RD | S5 gives vendors and clients a room without office-only notes or whys | S5 QUOTE | S | — | stage | Nothing for staff; a vendor or client login never receives an office-only note |
| 5 | L1 | Pure models in `:pano`: MarkLife, PhaseTools, Trades | S2 APK | M | — | (B2) | Nothing on screen |
| 6 | L2 | A mark's state drawn on the face; Drop instead of delete; clears keep wishes | S2 APK | M | L1 | (B2) | Nothing he can reach yet (no control makes a wish before L3) |
| 7 | L3 | The Mark sheet: → Client wish, Replace / Shift / Repair / Remove, the note | S2 APK | M | L2; RD on stage | **B2** | In the Wish list stage he taps a window's code, picks → Client wish and ↻ Replace, types "install UPVC window": the window turns dashed with a "Client wish" pill |
| 8 | L4 | A new mark placed as a Client wish; survey marks read-only in that stage | S2 APK | S–M | L3 | B2 | The tool chip reads "… · as Client wish" and every mark he places is born a wish; a survey mark cannot be moved or deleted by mistake |
| 9 | T5 | S5 learns the ten new types, their sub-kinds and the 13 trades | S5 QUOTE | M | — | stage | /projects names a Carpentry unit or a Ceiling drop instead of a grey code |
| 10 | L5 | The ten new types and sub-kinds, pure: boxes, measures, codes | S2 APK | M | L1 | (B3) | Nothing on screen |
| 11 | L6 | More ▸ by trade, the kind picker, the Note pin | S2 APK | M | L5, L3; T5 on stage | **B3** | He places "Carpentry unit · Wardrobe" for a new wardrobe, "Ceiling drop · Full false ceiling" for POP, and a Note that is only words |
| 12 | L7 | A wish on the 360 and in 3 · List | S2 APK | M | L4 | B4 | The same dashed outline and pill on the 360; a List row shows its state and the first line of its note |
| 13 | L8 | A wish on the six drawings, with the legend | S2 APK | M | L6, L7 | B4 | PLAN's drawings show wishes dashed, with the pill, the act and the note mark; a legend line says what is on the sheet |
| 14 | L9 | The Wish list counts its marks; "Marks:" on the project | S2 APK | S–M | L6 | B5 | A ticked row reads "7 marks in 2 rooms · 3 wished"; an unticked row that holds wishes asks "tick it?" |
| 15 | L10 | The phase band and the per-phase tools in survey prep | S2 APK | M | L4 | B5 | Survey prep opens under "2 · Wish list · every mark you place is a client wish"; Lines and readings show only in Survey |
| 16 | V5 | /projects shows a mark's state, act and note | S5 QUOTE | M | RD, T5 | stage | On the web a wish is dashed with its pill and note; staff see office notes tagged "Office only" |
| 17 | TR | Tours: "The phases" and "Build the wish list" switch on | S2 APK | S | L10, L3, L4 | B6 | Help lists three tours instead of one |
| 18 | PL | The room's plan on the phone | S2 APK | S–M | **PENDING AMIT** (§4) | — | decided by his answer |
| 19 | AR | S5 web: arrange the rooms into the flat's plan (outline, §5) | S5 QUOTE | L | T5; its own design | — | On a computer he drags the rooms into place; the whole plan shows on the project page |
| 20 | RQ | S5 web: an RFQ per trade to several vendors, quotes, comparison, rounds (outline, §6) | S5 QUOTE + S6 ERP | L, several slices | AR's drawings; §6's answers | — | On a computer he sends one trade's 2D document to several vendors and compares what they quote |
| 21 | A3 | The rename's neighbours: R9–R12 | S2 APK | M | A2 | later | A renamed project keeps its photos; Choose rooms works from the project screen |

## 2. Slice A — the site name

**What a "site" is on each side.** On the phone it is a level of the tree (client → site → project → room). S5
has projects only (`asis_projects`: project, title, customer — `functions/_lib/asis.js:1924`). So S5 stores the
site name **on each project**, and a site is the client's projects that carry the same name. No site table.

### A1 · S5 half (S)
- **Migration `0011_site_name.sql`** — checked: `migrations/` holds 0001–0010 on the working branch and on
  `origin/stage` (both `9a51733`), so 0011 is free. `asis_projects` gains `site`, `site_by`, `site_at` (NULL reads
  as `''`; no backfill). S5's CLAUDE.md promises "0011 or after" to the `asis_image_moves` drop: it becomes 0012.
- **`PUT /api/asis/:project/site`**, body `{site, base}`, bounded by `MAX_REC`. `site`: a string, newlines folded,
  trimmed, 1–140 characters (the cap rooms and party ids use), else 400. `base` REQUIRED: the name the writer last
  saw, `''` when none. 404 `This project has never been uploaded`.
- **The guard, in the house idiom** (S5-P's: the value is its own stamp) — ONE statement:
  `UPDATE asis_projects SET site = ?2, site_by = ?3, site_at = ?4 WHERE project = ?1 AND COALESCE(site,'') = ?base
  AND COALESCE(site,'') <> ?2`. The same name again answers `written: false` (the retry after a lost answer); a
  stale base answers **409** `Renamed by <login> meanwhile — now <site>; not saved` with `site`, `site_by`,
  `site_at` as stored. No read-then-write window.
- **Who:** MCFT staff, any of them, at every quote stage (a label, not scope). A vendor or client gets 403
  `Only MCFT staff can rename a site` before the project is read; the route joins the permissions walk.
- **First value.** The room record PUT takes an optional `site` and sets it ONLY WHILE NONE IS STORED. An upload
  never renames: a phone holding an old name must not write it back (the project's `customer` follows this rule).
- **What the reads return.** `GET /api/asis`: each project gains `site` and `site_at`; the project and room GETs
  carry `site`, `site_by`, `site_at`. Additive: 0.3.318 ignores them. /projects shows the name, read-only.
  `site` becomes a reserved room name once d1-peek shows no room on stage is called that; d1-peek prints
  `site_at` and whether a site is set, never the name (it may be an address).
- **Tests** (`tests/api.test.mjs`, memory and sqliteD1): the 400 loop; the 403 walk; 404; 413; a first write with
  `base: ''`; the same-name replay; the stale 409 with a racing writer; an upload setting it only while blank.

### A2 · phone half (M) — after P0 is approved and A1 is on stage
- **`pano/RenameRoute.kt`, beside `DeleteRoute.kt`** — `Renames.route(tx, kind, name, serverId, hasLocalRow,
  s5Projects, signedIn, legacyPair)` → `S5(projects, base)` · `Local` · `Legacy(docname)` · `Blocked(reason)`.
  For a **site**: any project under it that S5 lists → `S5` (signed out → `Blocked`); none, but a local row →
  `Local`; neither ("(no site)", the demo site, a site only an old ERP bootstrap named) → `Blocked`; never the
  bench. Client and project keep today's two routes, with `Blocked` in place of a call that cannot succeed. The
  same file holds the wire: `siteBody`, and `siteVerdict` → Renamed · Same · Stale · Refused(S5's words) · Offline.
- **`RenameRouteTest`** (`:pano`): one case per row kind — the demo site, "(no site)", an S5-listed site with and
  without a local row, local only, the local row gone, signed out, a bootstrap-only site, a client, a project —
  and the verdict on 200, `written: false`, 409, 403, 404 and no answer.
- **`app/MainActivity.kt:1188-1245` (onSave)** routes through it. `S5`: one PUT per listed project (a site usually
  has one); success is S5's answers ALONE. Then, in one step: those projects' `site_name` in the stored masters is
  set from S5's answer, the client's local rows under the old name are re-keyed (`Catalogue.renameLocal`, its
  Boolean now read), the room-scope key moves (`Catalogue.kt:376`), the tree follows, the dialog closes. On any
  failure the dialog STAYS OPEN with the reason under the field, and nothing moves.
- **One source for the name.** `S5Masters.fetch` (`app/S5Session.kt:298-348`) takes `site` from S5's list, on
  whichever entry holds the project, and stops stamping the local row's name into S5 entries (`:338-339`); a
  project S5 holds with no site resolves from the local row where the tree is read (`Catalogue.kt:479`).
- **Uploads and the one-off seed.** The record gains `site` (`pano/AsIsPath.kt` `body`, with its test; called at
  `app/AsIsUpload.kt:456-462`). `SyncWorker`, after the S5 list read (`:143`), sends `PUT …/site {site, base: ''}`
  for each listed project S5 holds with no site and the phone knows one for (first write wins); never the demo.
- **`app/TreeShell.kt:601-757`:** the error line under the field, set on failure, cleared on edit and dismiss —
  exactly as P0 is approved. New texts are born keyed; the Marathi is Claude's (standing rule of 28 Oct).

**The twelve audited defects.** CLOSED: **R1** (a working route for an S5-signed phone), **R2** (every refusal
is said in the dialog — client and project renames too), **R3** (no local row and nothing on S5 → refused,
navigation untouched), **R4** (S5's name is the one shown), **R6** (judged on the rename alone), **R7** (the
room-scope key moves with the name). NO LONGER REACHABLE for a site: **R5** (the bench's cross-client clash scan;
the site rename no longer calls the bench, and S1 is not changed). HALF CLOSED: **R8** — the phone's unsynced rows
are re-keyed in the same step; the bench is deliberately not told (answer 2), so a phone still holding a live ERP
pair would file later captures under a second bench site until the ERP leg retires (`s5-s6-integration.md` §4).
DELIBERATELY LEFT, slice A3, none on the site path: **R9** (the phone-minted S5 key is a hash of
client|site|title, re-derived and not stored, so a rename between a first upload and the next list read can still
fork), **R10** (a PROJECT or CLIENT rename leaves the phone's captures under the old name), **R11** (the v37 Choose
rooms / + Room fix below the tree's early return), **R12** (a one-site client resolved by name across clients).

**P0 · prototype items (Amit approves them there before A2 is built).** The prototype's `doRename`
(`P L7934-7938`) never fails, so there is nothing to copy. (a) The failure line: where it sits, and its words for
no signal, signed out of S5, S5's own refusal (shown as S5 sent it) and "renamed 1 of 2 — Save again to finish".
(b) The grey line for a site S5 holds: "…the new name reaches ERP and every other device" (`P L7926`, key
`app.x_server_so_new`) is untrue now. (c) A row that cannot be renamed: the line, or no rename offered.

## 3. The wish-marks port (prototype v53, and the parts of v54 the wish journey needs)

**The prep file and S5 keep unknown mark keys whole — verified, both sides.** Phone: a mark is read by seven keys
(`pano/SurveyPrepFile.kt:48`); every other key goes into `Mark.extra` (`:148`) and is written back after the seven
(`:230`); the same holds for a Tape, Line or area (`:168`), a chain, a surface and the file, pinned by
`SurveyPrepFileTest` ("round-trips every field, each at its own level"). A type the build does not know is kept
and drawn grey (`pano/SurveyPrep.kt:679`). S5: the record PUT stores the body as sent (`asis.js:1438`) and checks
no mark key or type (`:1387-1399`). So **no file migration and no S5 migration**: v53 adds to a mark, ONLY WHEN
NOT DEFAULT, `life` `lifeAt` `lifeAs` `why` `born` `act` `note` `vis` `sub` `cat` (`P L2749-2751`), and a v52 file
saves byte-identical. What must change first is what S5 hands back (RD, §3.11). The port is **a listed subset,
as QM1's strip was**: answer 3 keeps the vendor round off the phone (the list is in §7), but the life-cycle table
is ported WHOLE, and the Mark sheet offers exactly what the prototype's phase rule allows in each phase.

### 3.1 L1 · pure models (M, `:pano` only)
- **Prototype:** LIFE `P L2756-2764`, STROKE `L2768`, ACTS `L2770`, `spared` `L2789`, MOVES `L2794-2811`,
  `allowedMoves` `L2818`, `move` `L2834-2846`, `asPlace` `L2848`, the delete, unlock and clear rules `L2936-2955`,
  the counts `L2964-2977`; P54's PHASES `L3931`, TOOLS `L3947-3961`, AS_FIXED `L3964`, `moveOk` `L3985`, `editable`
  / `delOk` `L4232-4237`, the refusals `L4241-4250`; TRADES `L2379-2392`, `tradeOf` `L2487`, SEES / OWN / ON
  `L2493-2504`, SERVICE_TRADE `L2509-2521`, `moreRows` `L2602`.
- **Files:** new `pano/MarkLife.kt`, `pano/PhaseTools.kt`, `pano/Trades.kt`; typed readers and writers over
  `Mark.extra` and `Meas.extra` (the map is REPLACED, never changed in place; a key is written only when not
  default; back at Existing `life`, `lifeAt`, `lifeAs`, `act`, `why` are deleted; an unknown `life` reads Existing).
- **Tests:** `MarkLifeTest` (every MOVES row; born; restore to what it was born; a why of 3–140; Scope locks, Out
  of scope unlocks; Survey refuses; there-and-back byte-identical), `PhaseToolsTest` (TOOLS cell by cell, AS_FIXED,
  `moveOk`, `editable`, `delOk`, the words), `TradesTest` (every type and sub-kind on every surface kind),
  `SurveyPrepFileTest` (a v53 prep round-trips; a v52 prep saves unchanged). Nothing is drawn.

### 3.2 L2 · the state on the face, and the rules that protect a wish (M)
- **Prototype:** `outline` `P L2872`, `ring` `L2887` (a spot), `paintState` `L2898-2911` (after the code: the act
  glyph, the ✎ note badge, V+, then the pill, cut to its first word when crowded), the selected label's 3 dp state
  bar `L2930`, `delWord` `L2936`, `interceptDelete` `L2940-2946`, `unlockRefusal` `L2947`, `clearText` `L2952`.
- **Files:** `app/SurveyPrepDraw.kt` (`drawMarks` `:250`, `spot` `:219`) draws from `MarkLife.stroke` and
  `MarkLife.tag` — Client wish dashed 6/4 with a teal pill, Vendor review 2/3, Vendor added dotted, Client approved
  and Scope 2.5 dp, Dropped grey at 40 % with a red diagonal. `pano/SurveyPrep.kt` `deleteMark` `:1144`,
  `dropUnlocked` `:1185`; `pano/SurveyPrepEdit.kt` `setLocked` `:84`, `lockAll` `:128`, `clearItems` `:318`,
  `startOver` `:348`: a mark past Existing is never deleted (Delete reads Drop, or Restore on a Dropped one), the
  clears and Start over keep it and say so, a Scope mark stays locked.
- **Tests:** the stroke and tag tables in `MarkLifeTest`; `ClearTest`, `StartOverTest`, `LockTest` gain a wish.
  **i18n on:** the eight state names, `life.msg.x_restored_x`, `life.cleared_x_item_x`, `life.x_mark_x_past`,
  `life.x_scope_mark_x`, `refuse.life.x_scope_take_out`.
- **Amit:** nothing yet. Drop needs its why, typed in the sheet, so L2 reaches Play only with L3 behind it.

### 3.3 L3 · the Mark sheet (M) — the first thing he can do
- **Prototype:** `openSheet` `P L3012`, the sheet `L3024-3063`: the head (code · name, the trade's dot); STATE (the
  now-line "● Client wish · since … IST · decided by the client", then the phase's moves with their hints, a Why
  box under a move that needs one); the four **acts** on a mark born Existing that has left or is leaving Existing
  (`L3040-3042`: ↻ Replace · ⇢ Shift · ⚒ Repair · ✕ Remove); NOTE (two lines, 0/280, "Client & vendors | Office
  only"); ONE Done; ✕, the scrim and ◁ cancel; `apply` `L3071-3088`, one undo step per sheet. Gates: Survey's
  "Finish the survey first — …", an unread stage's footer, a locked mark ("only its note can change"), the amber
  line after the estimate. A Line or area gets TRADE and NOTE only (`L2783`).
- **Files:** new `pano/MarkSheet.kt` (the sheet's state and `apply`, pure) and `app/MarkSheet.kt` (a bottom sheet,
  keyboard-aware, no scroll in either orientation); `app/SurveyPrepScreen.kt`: the selected mark's code opens it,
  Delete on a mark past Existing opens it on Dropped; `SurveyPrepDialog` (`:181`) is given the project's stage
  index from `QuoteStageSync.cached` at its two call sites (`MainActivity.kt:519`, `:3838`) — null when not read
  or the project is not on S5 (then every move is offered, the prototype's unread rule). **Prep:** it writes
  `life`, `lifeAt`, `lifeAs`, `act`, `why`, `note`, `vis` (and `cat` on a Line or area).
- **Tests:** `MarkSheetTest` — nothing changed closes with no undo step; a move that needs a why keeps Done off
  until 3 characters; `act` written only on Existing → Client wish; the note capped at 280 code points; "Office
  only" kept only with a note; a locked mark changes its note alone.
- **i18n on:** `life.aria.mark_sheet`, the move labels (`life.client_wish`, `life.dropped`, `life.back_existing`,
  `life.restore` and the vendor-side ones), the acts (`life.replace`, `life.shift`, `job.repair`, `common.remove`),
  `life.why_least_3_characters`, `life.note`, `life.decided_x`, `life.since_x_x_ist`, `common.client_vendors`,
  `common.office_only`, `refuse.life.finish_survey_first_stage`.
- **Amit can:** at Client wish list, tap a window's code → → Client wish → ↻ Replace → "install UPVC window" →
  Done; a door → ✕ Remove → "close with brickwork". The mark is dashed, with the pill, the act and ✎.

### 3.4 L4 · a NEW thing placed as a Client wish (S–M)
- **Prototype:** `asPlace` `P L2848`; the chip's second half `L2860` — nothing at Survey; "· as Client wish" as
  plain words where the phase fixes it (AS_FIXED `L3964`); with the stage unread, the "as Existing ▾" button that
  cycles Existing → Client wish → Vendor added per capture (`L2857-2858`); `placed` `L2959`. Earlier-phase marks
  are read-only: `editable` / `delOk` `L4232-4237`, the controls HIDDEN, never greyed (`spPost` `L4255-4263`), the
  words `refuseText` `L4241`.
- **Files:** `pano/SurveyPrepPalette.kt` `chipText` (`:108-112`); `app/SurveyPrepScreen.kt` (place, duplicate,
  nudge, drag, lock, the trash and the ⋮ acts ask `PhaseTools`); `pano/SurveyPrepEdit.kt` `duplicate` `:264`.
  **Prep:** a new mark gets `born`, `life`, `lifeAt`, `lifeAs` once, at placement; never a Note.
- **Tests:** `PaletteTest` (the chip per stage), `DuplicateTest`, `PlaceAtTest`. **i18n on:** `sp.toolchip.as_wish`,
  `sp.toolchip.as_existing`, `life.aria.marks_are_placed_x`, `refuse.phase.x_was_marked_x`.
- **Amit can:** at Client wish list, pick a type and tap the photo: the box is born a wish — with today's twelve
  types until B3 (for one build a new wardrobe is "Other" plus a note).

### 3.5 L5 · the ten new types and their sub-kinds, pure (M)
- **Prototype:** the ten `P L2397-2407` (NOTE, GRL, AC, CDP, PLB, CTR, CRP, LV, SAN, DEF — 22 types: wall 21,
  floor 11, ceiling 13; PLB and LV are 50 mm points), SUBS `L2423-2466`, `displayName` `L2478`, the code grammar
  (the whole letter run is the type) `L2534-2543`, `defaults53` `L2551-2565`, `measures53` `L2569-2588`,
  `placeRefusal` `L2590`.
- **Files:** `pano/SurveyPrep.kt` (TYPES `:660-672`, `isPoint` `:700`, defaults, `measures0`, `highest` /
  `claimCode` `:1094-1121` over a letter run), `pano/Pano360.kt` (`placeRefusal` `:235`), `pano/i18n/Masters.kt`.
  **Prep:** `t` is the token (`FCRP1`, `CCDP1`, `FPLB1.cl`); `sub` only when not the first sub-kind.
- **Tests:** `SurveyPrepTest` (each type's default box and measures per surface kind), `KeyGrammarTest` (every new
  reading key passes S5's `KEY_RE` — it already walks TYPES), a code test (`FP1` and `FPLB1` never collide).
- **i18n on:** `type.*` (86 keys, 17 in use today) with `type.sub.*`, `t53.plumbing_point_x`, the 360's refusals.

### 3.6 L6 · More ▸ by trade, the kind picker, the Note pin (M) — after T5 is on stage
- **Prototype:** More ▸ as one row per trade, General first (`moreRows` `P L2602`, `renderMore` `L2688-2694`); the
  tool's remembered sub-kind (`L2471-2473`) and KIND chips in the Mark sheet (`L3030`); the ten icons `L2649-2666`
  and the PLB / LV spot symbols `L2669`; the Note: a 20 dp pin, tag "FNOTE1 ✎ first 24 characters…", a lock glyph
  when Office only, the sheet opening on its note field, an empty new Note removed (`L2912-2926`, `L3066-3070`).
- **Files:** `app/SurveyPrepTools.kt` (`SpMoreSheet` `:132`, icons), `app/SurveyPrepDraw.kt`, `app/MarkSheet.kt`
  (KIND; TRADE for a Note or Other), `pano/SurveyPrepPalette.kt`. With no Trade chip the three palette slots stay
  v52's (`paletteMain('all')`, `L2597`).
- **Tests:** `PaletteTest` (the rows per surface kind), `MarkSheetTest` (KIND; a Note needs its words).
  **i18n on:** `trade.*.short`, `life.what_should_office_vendor`, `life.msg.empty_note_removed`, `life.trade`.
- **Amit can:** his five examples as the prototype does them — Carpentry unit · Wardrobe; Ceiling drop · Full
  false ceiling (POP); Carpentry unit · Other unit plus a note (dining table); the window and the door from L3.

### 3.7 L7 · a wish on the 360 and in 3 · List (M)
- **Prototype:** `outlinePoly` `P L2880` and the same pill on the 360; in List the state dot before the code
  `L2933`, a Note's panel in place of measure rows `L3138`, the first line of a mark's note under its row `L3141`.
- **Files:** `app/SurveyPrep360.kt`; `app/SurveyPrepScreen.kt` `SpTable` `:2837`. **Tests:** the List's row model
  in `:pano`. **i18n on:** the state names again, `common.office_only`.

### 3.8 L8 · a wish on the six drawings (M)
- **Prototype:** `ls` `P L6121` (the outline by state), `sfx` `L6126` (act and ✎), `drawStateMark` `L6135` (the
  pill, V+), numbered Note pins, the legend line `L6460-6465` and the legend with "Office view: on / off"
  `L6502-6515`. With nothing past Existing, no note and no new type, a drawing is v52's to the byte (`L6083-6084`).
- **Files:** `pano/SurveyPlan.kt`, `pano/SurveyExport.kt`, `app/SurveyPlanScreen.kt`, `app/PlanExport.kt` (the
  exported sheet carries the same marks, in English). **i18n on:** `sp.legend_x`, `sp.office_view_*`.
- **Tests:** `SurveyPlanTest`, `SvgExportTest`: a v52 prep gives today's SVG; a wish carries its dash and state;
  an office-only note is drawn only with Office view on.

### 3.9 L9 · the Wish list counts its marks (S–M)
- **Prototype:** one prep per room, the newest 360 that has one (`P L8502-8513`); the service a mark answers
  (`serviceOf` `L2526`, SERVICE_TRADE `L2509`); `rowCount` `L8530`, the count line `L8535`, `uncovered` `L8539`
  and the two amber lines `L8548`, `L8586`; the project's roll-up `L2964-2977` ("Marks: 8 existing · 4 wish").
- **Files:** `pano/WishList.kt` (a pure `WishMarks`), `app/WishListScreen.kt:209-281`, `app/SurveyPrepStore.kt`
  (a project's preps), `app/BrowseScreens.kt:356-399`. The rows stay ticked by hand (GAP Q4). The two bathroom rows
  count by v53's room-name rule (`L2524`); the pack buttons are not drawn.
- **Tests:** `WishMarksTest` (Dropped and Notes left out; camera → CCTV, intercom → Intercom; a bathroom by its
  room's name; a wish no ticked row covers). **i18n on:** `wish.no_marks_yet_mark`, `wish.x_scope`,
  `wish.x_but_not_list`, `wish.x_not_any_*`, `life.marks`, `life.on_this_phone`.

### 3.10 L10 · the phase band and per-phase tools (M), then TR
- **Prototype:** the band's words `P L3931-3941`, TOOLS `L3947-3961`, `spPre` `L4204-4227` (a hidden step is moved
  on; the band at the top in portrait, over the photo's foot in landscape, `L4199`), readings refused outside
  Survey `L4250-4253`, Capture 360 by phase (changelog `L1885`).
- **Files:** `app/SurveyPrepScreen.kt`, `pano/StepRoute.kt`, the room screen in `app/MainActivity.kt`. The TOOLS
  rows port 62 honours: `lines`, `mark`, `palette`, `readings`, `capture`, `wishlist`, `wishEdit`, `vendorUI`.
  **i18n on:** `phase.band.*`, `refuse.readonly.readings`, `refuse.phase.*`. **Tests:** `StepRouteTest` by phase.
- **Amit sees a change he may read as a loss:** after Survey there is no 1 · Lines, no reading box and no Capture
  360 until he moves the phase back. The builder drives the served prototype at all six stages first.
- **TR (S):** `pano/Tour.kt` flips `phases` (`:127-129`) and `wish` (`:253-255`) once their anchors exist.

**Q-list for the prototype (raised with Amit; none blocks B2).** (1) The phone's 3 · List is rows, the
prototype's chips (`Tour.kt:210`): where the state dot and the note line sit. (2) The phone's 360 is step 4, the
prototype's a tab (parked Q3). (3) At Vendor review and Revisions the prototype shows the Trade chip, packs and
State ▾, which answer 3 keeps off the phone: approve a listed subset, or redraw those phases. (4) His own words
the prototype lacks (GAP D1): "New" on a born-wish pill, a "close up" act, a structured "replace with", a wish
that is an area or a length. (5) The `vendor` and `plan` tours on the phone.

### 3.11 RD · S5 redacts the room record per caller (S) — on stage BEFORE B2
The room GET answers the stored record whole to every caller the access list admits (`asis.js:1472-1474`,
`json({ ...d, … })`); it is the only outlet of the whole record (`captureSummary` only counts marks), and the
prototype's own note names the fix as a precondition (`P L1921`). Design: one exported `redactRecord(d, staff)`
at that GET — both addresses, the room and `/c/:capture`. Staff get the record byte for byte. Anyone else gets a
copy in which, on every surface: a NOTE mark with `vis: 'office'` is removed; an office-only `note` is removed
from a mark, Line or area; `why` is always removed (the prototype prints a why in office packs only, `P L3289`);
and — **an allow-list, not a deny-list** — only the keys S5 knows stay (`id t code x0 x1 y0 y1 locked sub cat life
lifeAt born act`, and `note` when not office-only), so a key a later phone build adds is office-only until S5
names it. **Tests:** staff deep-equal to what was stored; a vendor and a client lose exactly those things at both
addresses; a record with no v53 key answers as today; a malformed `els` never throws. Not solved here: a vendor
still sees other trades' marks and states (S5 learns a vendor's trade only with §6).

### 3.12 T5 · S5 learns the types and trades (M) — on stage BEFORE B3
`app/src/asis_view.js`: the ten rows in TYPES (`:47`) per surface kind, the sub-kind names, the point set grown
to PLB and LV (`:69`), the `measures0` branches (a Note has none), and TRADES / `tradeOf` / SEES / SERVICE_TRADE
exported — held equal to the phone's tables by `tests/asis-view.test.mjs`, as TYPES and the quote stages are.
`KEY_RE` is unchanged. Until it ships, a new type shows on /projects as a grey code with the wrong checklist.

### 3.13 V5 · /projects shows state, act and note (M)
`surfaceView` (`asis_view.js:393`) carries `life`, `born`, `act`, `sub`, `cat`, `note`, `vis` from the (already
redacted) record; `projects.js` `surfaceHtml` (`:236`) draws each box in its state's outline with the pill, the
act and ✎, a Note as a pin with its words, the list head as "FW1 · Window · Client wish · ↻ Replace" with the
note under it ("Office only" for staff, who alone receive it), and the project page a "Marks: …" line from a
per-state count added to `captureSummary` (`asis.js:469`). Display only; nothing on the web edits a mark.

## 4. The room's plan on the phone — PENDING AMIT

**What 0.3.318 already has.** Inside one capture's survey prep, the eighth tab, PLAN (`pano/StepRoute.kt:65`):
six drawings of that room — Floor, reflected Ceiling, Front, Right, Back, Left elevations — one at a time by
chips, with pinch, pan and fit (`app/SurveyPlanScreen.kt`, `pano/SurveyPlan.kt:71-75`), and a 1:50 A4 export of
the drawing on screen as SVG, PDF or Share. It is reached room → photo → survey prep → PLAN. So "inside that
room only" is already true; what his words add is "a cleaner look to verify quickly" — or he did not find the tab.

| | Candidate | Size | Note |
|---|---|---|---|
| a | **A way in, nothing new drawn:** the room screen opens its current capture's PLAN directly | S | if the six drawings are right and only hidden |
| b | **One verification sheet per room:** the floor plan with its four elevations folded out round it (the cube net S5's room page uses), as line drawings on one zoomable page | M | `SurveyPlan` has the six renderers and `FacePrep.EDGE_NAMES` says which edge meets which; "floor and wall elevations … verify quickly" read literally |
| c | The room's floor plan as the picture on its tile | S–M | recognisable at a glance; verifies little |
| d | The six drawings as they are, with the wishes on them (slice L8) | in §3 | if "verify" means checking the wishes on clean drawings |

**The choice is PENDING AMIT** — he is being asked what he saw in 0.3.318. Nothing is built before his answer,
and the choice is drawn on the prototype first. Fixed either way: no arranger on the phone (answer 4; the audit's
F1 and F2 move to S5), and the phone's whole-flat view is §5's, read-only, later.

## 5. S5 web — the arranger (outline only; its own design before any build)

- **Geometry.** The prototype's `FP54` is pure and already JavaScript (`P L4526-4612`: `rectOf`, `toWorld`,
  `rotate`, `snap` at 150 mm, `overlapping`, `fit`, `zoomAt`, `hit`, `startRow`, `appendRow`, `openings`,
  `inView`). It moves to a new `app/src/asis_plan.js` with the prototype's own checks as `tests/asis-plan.test.mjs`.
- **Pieces.** One per surveyed room: its CURRENT capture's W × L with its doors, windows and openings, drawn by
  the phone's mapping, which S5 must copy exactly (`pano/SurveyPlan.kt:29-38`: front at the top, walls clockwise)
  or the web shows the flat mirrored.
- **The page.** A "Floor plan" part of the project page, staff editing with a mouse: drag, edge snap, click then
  ↻ 90°, wheel zoom, Fit, Reset with an inline confirm, overlaps in amber, the rooms not surveyed listed under it
  — the prototype's page (`P L4615-4789`) with a pointer for a finger.
- **Where the layout is stored.** One document per project on S5 (the prototype's changelog says sharing a layout
  needs exactly this, `P L1882`): `layout` (JSON `{room: {x, y, rot}}`), `layout_at`, `layout_by` on
  `asis_projects`; `GET | PUT /api/asis/:project/layout`, the PUT staff-only, the whole document with a REQUIRED
  `base` (the wish list's idiom), 409 when stale. Keyed by ROOM, not capture, so a re-shoot keeps its place.
- **"The whole project floor plan should be visible once its done."** The project page draws the saved layout
  read-only for everyone it admits, and offers it as one SVG / PDF sheet (the audit's F7, which nothing draws
  today). By the one-state rule "done" is DERIVED — every surveyed room placed, none overlapping — not a button.
- **What the phone shows, later:** the project screen's Floor plan tile opening a read-only view of that layout.
  The prototype draws an arranger there, so a read-only page is a prototype item first. **Size:** L. **Open:**
  whether clients and vendors see the whole plan; whether a wished door or window is drawn on a piece.

## 6. S5 web — RFQ (outline only; several slices, after its own design)

- **The document: one per trade, "plain 2d diagrams".** The model is the prototype's Vendor review pack with
  Drawings and List and no photo faces (`P L3194-3199` the packs, `L3278-3289` the ONE rule of what a pack draws,
  `L3331-3361` the page plan): a cover (client INITIALS only, the trade, an RFQ number, issue and reply-by dates,
  the vendor it is addressed to); per room the Floor plan always, as key plan, plus each drawing that holds
  something of the trade; that trade's marks in colour with figures, what it needs to see as grey outlines with
  codes, every other trade hidden; then the measured list with quantities per type. Never on it: office-only
  notes, a why, the Vendor field, the client's full name, any cost figure. The code that draws it is already
  JavaScript in the prototype — `classify` and `plan` (`L3280`, `L3331`), the six drawings (`L6075-6521`), the
  totals (`L6444`) — and moves to S5 beside §5's `asis_plan.js`, since answer 3 makes S5 the only place an RFQ is
  made. Pages are SVG, printed to PDF by the browser; the whole-flat key plan joins the cover once §5 exists.
- **Lines of service.** The 13 trades already carry Amit's six: painting → Painting & finishes; electric →
  Electrical (AC and CCTV & data beside it); civil → Civil & structure (Tiling beside it); furniture → Carpentry &
  furniture; doors and windows → Doors & windows; POP → False ceiling. Which list rules is question 1.
- **One trade to several vendors.** An RFQ tab on the project (staff): pick the trade, the Suppliers (read from
  ERP), a reply-by date. S5 freezes a SNAPSHOT of that trade's wish marks and quantities under a revision number
  in D1 (never an amount) and creates ONE ERPNext Request for Quotation naming those Suppliers.
- **Each quote, and comparing.** A vendor's reply becomes an ERPNext Supplier Quotation against that RFQ.
  **Amounts live in ERP only**: S5 keeps the document names and reads the figures live, for staff; a comparison
  page lays the vendors side by side per line. A vendor's prices are cost data — never in a repository, a test or
  the prototype (invented figures only), never in client-facing output; the word "margin" is not used.
- **Revision rounds.** A changed wish set is a new snapshot and a new RFQ to the same vendors; earlier quotes stay
  under the earlier revision. Sent / quoted / awarded is DERIVED from these records (one state, one place), not a
  third hand-set stage beside the quote stage and the mark's state.
- **ERP stays vanilla.** `mcft_erpnext` seeds, as config-as-code, one non-stock service Item per trade (its own
  Item Group, its units) and a narrowly-scoped service user for S5 (create RFQ and Supplier Quotation; read
  Supplier and Item) — reachable from BOTH `after_install` and `after_migrate`, checked by `verify_setup`, proven
  on `mcft-erp-dev` first. S5 holds that pair as it holds the Lead pair (`s5-s6-integration.md` §3.4).
- **What could be verified from disk.** (1) `Supplier Quotation` is named by shipped S1 code that runs on mcft-stg:
  a permission probe (`mallet_estimator/integration.py:626`) and a count by its `supplier` field
  (`patches/stock_vendor_model.py:72`, guarded by `frappe.db.exists`) — weak evidence that the doctype and that
  field exist. (2) `Request for Quotation` appears only in prose (`mcft-erpnext-context` `estimator/DESIGN.md:20`:
  "RFQ → Supplier Quotation auto-creates Item Prices"), never exercised. (3) No frappe or erpnext source is on
  disk. (4) `mcft_erpnext/generic.py` seeds supplier groups, suppliers, item groups and UOMs; no item seeder yet.
- **CONFIRM ON THE LIVE SITE** (dev first, each absence read behind a control query): both doctypes and the Buying
  module on production's app set; the RFQ's child tables and mandatory fields; that a non-stock service Item is
  accepted on its rows; create and submit over plain REST by a narrow user, and whether submit emails suppliers;
  how a Supplier Quotation is tied to its RFQ over REST; the comparison report's name on v16; whether a Supplier
  Quotation writes Item Prices (it must never touch "Estimation (Assumed)"); India Compliance's mandatory fields.

**Open questions for Amit (six; each becomes a selector, recommended option first).**
1. **S5 QUOTE** — the lines of service an RFQ goes out by: the 13 trades (recommended; his six all fit), his own
   short list with the rest folded in, or S5's twelve wish-list services.
2. **S5 QUOTE + S6 ERP** — who enters a vendor's quote: staff, from the vendor's reply (recommended first), or
   the vendor on a page of his own.
3. **S5 QUOTE** — what a vendor quotes: a rate per line against S5's quantity (recommended; it can be compared),
   one lumpsum per trade, or either.
4. **S5 QUOTE** — how the document reaches a vendor: staff share the PDF by hand (recommended), or ERPNext emails it.
5. **S5 QUOTE + S2 APK** — the per-mark vendor answers (Vendor review / approved / added) hold ONE vendor's word:
   keep them as the summary staff set after comparing (recommended), or make them S5's, derived from the quotes.
6. **S5 QUOTE + S6 ERP** — the award: record the accepted Supplier Quotation per trade and stop there for now
   (recommended), or carry it on to a Purchase Order and into the estimate.

## 7. Risks, and what is deliberately not in port 62

**Risks**
- **An older build opens a prep that holds wishes.** 0.3.318 keeps every key (verified) but its Delete removes a
  wish outright, its clears and Start over do not spare one, and it draws new types grey. Today only Amit's phone
  holds captures and Play updates it; a second phone must be on B2 or later before it opens such a room.
- **The record PUT replaces the whole record**, so two phones editing one capture's marks is last-write-wins, as
  today — but what can be lost is now a client's wish, not only a box.
- **L10 hides tools he uses today** (Lines, readings, Capture 360 outside Survey). It is the approved prototype,
  and the build's note says so plainly, so it does not read as a fault.
- **The vendor-side states are a one-vendor model**, ported because the table is one piece (§6, question 5).
- **The bench's site name is no longer changed** (R8's left half); `rename_node` for a site has no caller after
  A2, and its removal belongs to the ERP-leg retirement, not here.
- **Prototype first.** A2 waits for P0. B2 does not wait for the Q-list, but a screen difference found while
  building stops at the prototype, not on the phone.
- **The line references are to a built page that is in no repository** (the rule of 3 Nov 2026): the published
  prototype is the source, and each slice re-reads its lines before it changes anything.

**Deliberately not in port 62**
- On the phone: the Trade chip and its filter, State ▾ and Move N ▸, the Export sheet and the five packs, VENDOR
  PACKS, GOES TO ESTIMATION, the bathroom bundle, the 360 as a tab, the arranger, vendor and client sign-in (K6),
  an in-app camera, any amount or vendor quote.
- The four things Amit's words ask for that the prototype does not draw (Q-list 4): prototype work first. The
  twelve-row wish list's future (GAP Q4): it ships as the prototype has it.
- R9–R12 (slice A3), AS2, the 19 parked questions, a web rename control, any web editing of a mark, per-trade
  visibility for a vendor on S5 (it needs §6's records).
- Any custom ERP code, any S1 change, any change to the legacy ERP leg.
