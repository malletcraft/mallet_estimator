# GAP-0.3.318 — what the prototype draws that 0.3.318 does not, the site-name bug, and what Amit's wish-on-photo + RFQ direction adds

**Dated 11 Nov 2026 (IST). S2 APK, with S5 QUOTE and S1 MEST where the trace reaches them.**
Research only: no code changed by this audit. Nothing here is a design; every UI change below is designed on the
prototype first (Amit, 2026-10-11), and nothing in section 4 is built until Amit answers section 5.

## 0. How to read this

- **What was compared.** The approved prototype v61c.2 (Version 86), one HTML file of 9,563 lines, cited as
  `P L<line>`. The phone at `batch-next` `da28861` = `main` `6267902` + one docs-only commit, so it is the code of
  Play build 0.3.318. S5 at `9a51733` (working copy branch `claude/awesome-gates-n4ossm`). The bench side (S1) in
  this clone.
- **Phone paths** are shortened: `app/…` = `android/app/src/main/kotlin/com/malletcrafts/interiorestimate/`,
  `pano/…` = `android/pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/`. S5 paths are in
  `mcft-interior-estimate`.
- **Where the facts come from.** Three readers: one read and DROVE the prototype at 360 x 780 on the invented Demo
  project (all three tours ran with every anchor found: plan 6/6, wish 13/13, vendor 12/12); one read the phone and
  S5; one traced the rename path. The writer of this file re-read about forty of the cited lines (the ones the
  conclusions rest on) and they held. A fact with no evidence is marked **unverified**.
- **Nothing was run on a phone.** Every phone statement is from reading code. The site-name defects are
  "confirmed by reading", not reproduced.
- **Sizes** are rough and are the writer's estimate, not a plan: **S** = one slice, about a day; **M** = two to four
  slices; **L** = five or more slices, or needs S5 to change first.
- **Screenshots and driver reports are NOT in any repository.** They sit in the session scratchpad
  (`gapaudit/proto/`, 162 pictures; `gapaudit/work/`, drivers and JSON) and will not survive a container restart
  (the rule of 3 Nov 2026). This file is the record; the pictures can be regenerated from the prototype.

## 1. Amit's report, verbatim (11 Nov 2026, after testing APK 0.3.318 on the Mi A3)

> the floor plan is not in 3.318. site name change is not happening correctly. i want to mark client wish list on
> foto itself the way i mark window door etc like new wardrobe, pop, new dining table etc. remove this window and
> install UPVC window. close this door with brickwork . these are real world client wish. i want to mark it on foto
> itself rather than listing as text. so idea of quote journey is first survey will be done. then on same survey
> fotos, client wish list will come, then vendor will review these wish list for quotation. then vendor will submit
> their quote for this wish list. mallet crafts should be able to send this wish list to multiple vendors using RFQ
> process so that we can compare all quotes. then there will be revisions for the wishlist based on quoted amount ,
> where we really need it or not etc. then quote will be finalised. and post that work will start.
>
> RFQ will be sent as plain 2d diagrams as a document so that easy to get quote. RFQ will be per line of service
> like painting, electric work, civil work, furniture work, doors and windows, POP etc.

## 2. The gap table

### The one-paragraph answer

The phone carries the prototype's **v52** survey prep plus **v61c**'s project line, phase strip, album and tour. It
does **not** carry **v53** (trades, ten new mark types, the mark life cycle, the Mark sheet, the Note pin, the Export
sheet and its packs) or **v54** (per-phase tools, vendor packs, the bathroom bundle, the project Floor plan). The
phone's own code says so: `pano/Phases.kt:20-22` ("rest on the v53/v54 marks, trades and packs the phone does not
have, and come with that port"), and three of the tour's flows are marked `ships = false` with the missing anchors
named (`pano/Tour.kt:254-255` wish, `:299-300` vendor, `:368-369` plan).

So most of what Amit asked for on 11 Nov is **already drawn in the approved prototype and not yet ported**; what is
in neither is section 4.

### 2.1 Floor plan — which one is missing

Three different things are called "plan". **The one Amit most likely means is the project-level "Floor plan" tile
and its page, "Floor plan · <project>"** — the arranger he asked for on 19 Oct 2026 ("i need a feature which i can
use to draw floor plan using all the rooms inside the project. like arranging the rooms.", quoted at `P L4510-4511`).
It is the only plan the phone lacks entirely, it sits on the project screen where he would look for it, and the
tour's sixth flow teaches it. This is **likely, not confirmed** — question 1 in section 5.

| # | Prototype feature | Prototype evidence | Phone 0.3.318 | Phone evidence | Size |
|---|---|---|---|---|---|
| F1 | **Floor plan tile** on the project's rooms grid, after the shot rooms, in every phase, reading surveyed/all ("1/3 Floor plan") | `P L4659-4663` (tile), `P L8135` (its place in the grid) | **ABSENT** | `app/BrowseScreens.kt:400-451` draws shot rooms, +N, the rest, + Room, the Album tile — no plan tile | with F2 |
| F2 | **Floor-plan arranger page, stage 1 by hand**: each surveyed room a piece at its laser W x L with only its doors, windows and openings; one finger drags; snap edge to edge within 150 mm; tap selects and shows "↻ 90°"; pinch zoom, pan; Fit; Reset layout with inline confirm; overlaps named in amber; "Not surveyed yet (n)" list | `P L4509-4791` (pure geometry `FP54` `L4526-4612`; page `L4615-4789`); changelog `P L1882` | **ABSENT** | `pano/Tour.kt:369`: "the project-level Floor plan arranger … does not exist on the phone; SurveyPlanScreen is the per-room PLAN tab, a different thing"; `android/docs/port61c/DESIGN-port61c.md:3469-3470` | **M** — the geometry is pure and testable in `:pano`; one Compose screen with gestures; layout kept on the phone only (the prototype stores `{room: x, y, rot}` per project in `mcft-arrange-v54`, `P L4527`, `L4649-4651`, and its changelog says S5 needs nothing for stage 1). The English and Marathi texts already exist, unused (`android/i18n/index.tsv:1082-1097`, `1149-1150`; `pano/i18n/Tx.kt:3419-3438`) |
| F3 | **Per-room PLAN tab**: six drawings of one room (Floor, reflected Ceiling, four elevations), pinch, pan, fit | `P L4302` (tabs), `L6181`, `L6284-6429`, `L6523-6542` | **PRESENT** | `pano/StepRoute.kt:65` (TAB_ROWS … list, plan), `:71`; `app/SurveyPlanScreen.kt:67`, `:144`; `pano/SurveyPlan.kt:71-75` | — |
| F4 | **Single-drawing A4 sheet export** at 1:50: SVG, Image, PDF, More… | `P L6543-6566`; Image and More… added at `P L3771-3781` | **PARTIAL** — SVG, PDF, Share only; no Image, no More… | `app/SurveyPlanScreen.kt:212-216`; `app/PlanExport.kt:39-189`; `pano/SurveyExport.kt:11-26` | Image: **S**. More… needs V4 |
| F5 | **States, trades and notes on the drawings**: a wish dashed with its pill, act glyph, note badge; legend line and legend; "Office view"; the Trade chip filtering the drawing | `P L6075-6135`, `L6430-6521`, `L6512` | **ABSENT** | the phone's tab is v52's; recorded as not ported and "wait for Amit to ask" at `DESIGN-port61c.md:3465-3468` | **M**, after W5 and V2 |
| F6 | **Plan tour** (6 steps) | `P L9064-9084`, anchors `L9187-9191` | **BLOCKED** (`ships = false`) | `pano/Tour.kt:367-369` | **S**, after F2 |
| F7 | One plan of the **whole flat** as an exported document | — | **IN NEITHER.** The arranger has no export, print or share button (buttons found by driving: Back, Search, Fit, Reset layout) and `FP54` / `mcft-arrange-v54` are referenced by no export code (only `P L4509-4791`, `L1882`, `L9122`). "Whole project" in the Export sheet gives one set of drawings per room | — | section 4 |

Also true, and outside the prototype: S5's `/projects` room page shows the six squared photos as a cube net with the
marks over them and draws **no** floor plan or elevation (`app/src/asis_view.js:27`, no plan renderer);
`pano/SurveyPlan.kt:35-38` records the mapping S5 must copy if it ever draws one.

### 2.2 Wish

| # | Prototype feature | Prototype evidence | Phone 0.3.318 | Phone evidence | Size |
|---|---|---|---|---|---|
| W1 | **Wish list screen**: twelve service rows; tick, APPLIES TO rooms, one-line note, Vendor field (only at the vendor stages) | `P L8395-8619`, services `L8418-8431` | **PRESENT** | `pano/WishList.kt:69-84`, `:139-144`; `app/WishListScreen.kt:86-190`, `:209-281`; chip `app/BrowseScreens.kt:376-387` | — |
| W2 | Wish list **storage, sync, stage rules, approval lock** | row shape = S5's `PUT …/wishlist` | **PRESENT** | `app/WishSync.kt:12-71`; `pano/WishList.kt:353-403`, `:558-666`; S5 `functions/_lib/asis.js:1322-1352` | — |
| W3 | **Five-phase strip** on the project screen (1 Survey, 2 Wish list, 3 Vendor review, 4 Estimation, 5 Revisions), mover, Approve | `P L3908-4282` | **PARTIAL** — the strip, the mover and Approve ship; the VENDOR PACKS row, the GOES TO ESTIMATION row and the hand-off counts do not | `pano/Phases.kt:18-23`, `:51-57`; `app/PhaseStrip.kt:56-61` | rows come with V3, V9 |
| W4 | **Phase band inside survey prep** ("2 · Wish list · every mark you place is a client wish") and **per-phase tools** (what each phase shows; earlier-phase marks cannot be moved; readings refused outside Survey) | `P L3947-3964`, `L3985-3988`, `L4204-4264` | **ABSENT** | `pano/Tour.kt:129` (no phase band) | **M** |
| W5 | **Mark life cycle**: every mark has ONE state — Existing, Client wish, Vendor review, Vendor approved, Vendor added, Client approved, Scope, Dropped — drawn by outline and a pill; moves are one data table; past Existing a mark is dropped and restored, never deleted; Scope locks it | `P L2756-2764` (LIFE), `L2794-2811` (MOVES), `L2818-2820`, `L2867-2911`, `L2938-2955` | **ABSENT** — a phone mark is `{id, t, code, x0, x1, y0, y1}` and nothing else is read | `pano/SurveyPrepFile.kt:48` (MARK_KEYS); no `MarkLife` or `life` key under `app/src/main` or `pano/src/main`; `pano/Tour.kt:255` ("lifecycle the phone has never had") | **L** — the base everything else in 2.2 and 2.3 stands on |
| W6 | **A NEW item placed on the photo as a client wish**: in the Wish list phase the tool chip reads "… as Client wish" and every tap places a mark born a wish, on a wall, floor, ceiling photo or the 360 | `P L2847-2860`, `L3964` (AS_FIXED) | **ABSENT** | `pano/SurveyPrepPalette.kt:108-112` (chip text has no "as …") | part of W5 |
| W7 | **Mark sheet**: KIND, TRADE, STATE with the allowed moves, the four **acts** on an existing thing (↻ Replace, ⇢ Shift, ⚒ Repair, ✕ Remove), a Why where needed, NOTE (280) with "Client & vendors \| Office only" | `P L3003-3088`, acts `L2770`, act rule `L3040-3042` | **ABSENT** — the phone has the size sheet, More, a long-press menu, locks | `app/SurveyPrepScreen.kt:1860`, `:2837`; `app/SurveyPrepTools.kt:132`; `app/SurveyPrepLocks.kt:83-215` | **M** |
| W8 | **Note pin** (a mark that is only words, with a trade and an audience) | `P L2912-2926` | **ABSENT** | not in `pano/SurveyPrep.kt:660-672` | **S**, after W7 |
| W9 | A wish **shown everywhere**: the 360, 3 · List (state dot, note, "State: All ▾", "Move n ▸"), the six drawings, the project roll-up ("Marks: 8 existing · 4 wish") | `P L2879-2885`, `L2963-3001`, `L3117-3135`, `L8125-8126` | **ABSENT** | `app/SurveyPrepScreen.kt:2837` (SpTable has no state); `app/BrowseScreens.kt:356-399` (no counts) | **M** |
| W10 | Wish list **knows its marks**: each ticked row counts the marks of its trades and rooms by state; an unticked row holding wish marks says "tick it?"; the two bathroom rows open into five Parts | `P L8530-8548`, `L8583-8586`; service-to-trade table `L2509-2521`; bundle `L4362-4501` | **ABSENT** | `app/WishListScreen.kt:209-281`; `pano/WishList.kt:327-330` | **S–M**, after W5 |
| W11 | **Client documents**: "Client pack ⇪" (the Wish list pack) and the For approval pack ending "Approved by ____ Date ____", with "Record the client's yes…" | `P L8564`, `L3194-3199`, `L3562-3594`, `L3855-3858` | **ABSENT** | no Export sheet (see V4) | with V4 |
| W12 | **Wish tour** (13 steps) | `P L8954-8997` | **BLOCKED** (`ships = false`) | `pano/Tour.kt:253-255` | **S**, after W4–W7 |

Two facts that matter for what Amit said:

- **The phone today cannot attach a wish to a mark, a photo or a capture.** The wish row is `{on, rooms, note,
  vendor}` (`pano/WishList.kt:139-144`), its PUT body the same (`app/WishSync.kt:68-71`), and S5's table has no such
  column (`migrations/0007_wishlist.sql:16-24`). "Remove this window and install UPVC" can be typed only as a
  one-line note on the Doors & windows row.
- **The phone's strip names tools it does not have.** At Wish list it reads "Walk the client through: mark the
  wishes" and at Vendor review "Trades and vendor packs, from the wishes" (`pano/i18n/TxEn.kt:1041`, `:1039`), with
  nothing behind either line. That is a likely reason the gap felt like a missing feature rather than a later slice.

And the prototype was driven with Amit's own five examples (reader's reports `rep_a2.json`, `rep_a2b.json`):

| Amit's wish | What the prototype does today | What it lacks |
|---|---|---|
| "new wardrobe" | Carpentry unit · Wardrobe placed in the Wish list phase: a dashed box with a teal "Client wish" pill, sized by dragging, with a note | the pill does not say "New" |
| "pop" | Ceiling drop (Bulkhead, Cove, Full false ceiling) on the ceiling photo; on a wall only as a Note ("POP cornice along this wall") | no POP type on a wall |
| "new dining table" | Carpentry unit · Other unit on the floor + a note | no table kind |
| "remove this window and install UPVC window" | Window → Client wish · Replace + note | the replacement is free text; no structured "replace with" |
| "close this door with brickwork" | Door → Client wish · Remove + note | there is no "close up" act, and the mark stays in the **Doors & windows** trade (`P L2482-2491`: a Door's trade is fixed) — so it would go to the doors vendor's pack, and the civil vendor would see only a grey outline with a code (`P L3278-3289`) |

### 2.3 Vendor

| # | Prototype feature | Prototype evidence | Phone 0.3.318 | Phone evidence | Size |
|---|---|---|---|---|---|
| V1 | Free-text **Vendor** field on a wish row (one name per service) | `P L8591` | **PRESENT** | `pano/WishList.kt:143`, `:376-380`; `app/WishListScreen.kt:294-302` | — |
| V2 | **13 trades** and the **Trade chip** ("Show marks for"): own marks in colour, what the trade needs to see as grey context, the rest hidden — one filter for photo, 360, List, drawings and export | `P L2379-2392` (TRADES), `L2482-2502`, `L2607-2629`, `L2695-2731` | **ABSENT** — no trade concept in survey prep | no `Trades.kt`; `pano/Tour.kt:300` | **M** |
| V3 | **VENDOR PACKS** row on the project strip: one button per trade that still has wishes with the vendor ("Doors & win. · 2 ⇪") | `P L4123-4138`, `L4160-4162` | **ABSENT** | `pano/Phases.kt:20-22`; `app/PhaseStrip.kt:59-60` | **S**, after V4 |
| V4 | **Export sheet and five packs** (Survey, Wish list, Vendor review, For approval, Scope for estimate): PACK, TRADES, scope wall / room / project, Faces · Drawings · List, PDF · PNG · JPEG, page thumbnails, Save, Share, LAST EXPORTS | `P L3154-3906`: PACKS `L3194-3199`, the one rule `L3278-3289`, plan `L3331-3361`, sheet `L3744-3892`, log `L3718-3728` | **ABSENT** | only `app/PlanExport.kt` (one drawing, SVG / PDF) | **L** |
| V5 | **The Vendor review pack** — the document a vendor receives: cover (client INITIALS only), photo faces with that trade's marks, plain 1:50 drawings (Floor plan always as key plan), a measured list. Never on it: office-only notes, the why of a drop, other trades' marks, the Vendor field, the client's full name, any cost figure | `P L3345-3358`, `L3395-3429`, `L3491-3517`, `L3541-3578` | **ABSENT** | — | part of V4 |
| V6 | **After a Share** of a vendor pack: "Move n Client wish marks to Vendor review" | `P L3851-3854` | **ABSENT** | — | part of V4 |
| V7 | **Vendor's answers on the mark** (→ Vendor approved; ← Client wish with a why; → Dropped with a why) and **vendor-added marks** (dotted violet, V+), all keyed by STAFF | `P L2799-2806`, `L2847-2850` | **ABSENT** | — | part of W5 |
| V8 | **State filter** and **Move N** in 3 · List | `P L3117-3133` | **ABSENT** | `pano/Tour.kt:300` | **S**, after W5 |
| V9 | **Estimation hand-off** ("GOES TO ESTIMATION — n marks in Scope …") and the **Scope pack** with quantities per trade, type and room | `P L4132-4136`, `L4163-4165`, `L3430-3457` | **ABSENT** | `pano/Phases.kt:20-22` | **M**, after V4 |
| V10 | **Revisions** row and "Approve the estimate ▸" → the sixteen-work checklist → Client approved (final) | `P L4166-4169`, `L7887-7904`, `L4099` | **PARTIAL** — Approve ships; the hand-off counts do not | `pano/Phases.kt:20-23` | — |
| V11 | **A vendor's own view** — only in the Album, after approval, by WORK | `P L7070-7338` | **PARTIAL** — the vendor role is a view model and the PM's Accept / Not for client exists; a vendor cannot sign in (the session is ended unless `kind == "staff"`) and the vendor upload (K6) is absent | `app/S5Session.kt:128-131`; `pano/AlbumPage.kt:28`; `pano/Album.kt:99-115` | — |
| V12 | **Vendor tour** (12 steps) | `P L8999-9037` | **BLOCKED** (`ships = false`) | `pano/Tour.kt:298-300` | **S**, after V2–V8 |

### 2.4 Marks

| # | Prototype feature | Prototype evidence | Phone 0.3.318 | Phone evidence | Size |
|---|---|---|---|---|---|
| M1 | v52's **twelve letters** (W D C B O N L E P S F X; floor S = Step) | `P L4920-4922` | **PRESENT** | `pano/SurveyPrep.kt:660-672` | — |
| M2 | **Points** (a 50 mm spot measured to its centre): E, P, F | `P L4934` | **PRESENT** | `pano/SurveyPrep.kt:700-712` | — |
| M3 | **Ten new types**: NOTE Note, GRL Grill, AC AC unit, CDP Ceiling drop, PLB Plumbing point, CTR Curtain track, CRP Carpentry unit, LV Low-voltage point, SAN Sanitary ware, DEF Defect — 22 types in all (wall 21, floor 11, ceiling 13); PLB and LV are points | `P L2395-2407`, `L2415-2416` | **ABSENT** | none in `pano/SurveyPrep.kt:660-672` | **M** |
| M4 | **Sub-kinds** with their own default boxes (Wardrobe, Kitchen base, TV unit, Bed, Full false ceiling, DB / MCB board, Wash basin, Crack …) and the kind picker on the tool chip | `P L2423-2466`, `L2551-2565`, `L6635-6636` | **ABSENT** | `pano/SurveyPrepPalette.kt:108-112` | with M3 |
| M5 | **Palette**: Select, Tape, Line, Chain, three type slots, More ▸ | `P L2593-2599` | **PRESENT** (v52 shape) | `pano/SurveyPrepPalette.kt:62`, `:82-86` | — |
| M6 | **More ▸ by trade** (one row per trade) and the palette putting the picked trade's types first | `P L2600-2605`, `L2685-2694` | **ABSENT** — the phone's More is one grid of every type | `app/SurveyPrepTools.kt:132`; `pano/Tour.kt:208-210` | **S**, after V2 |
| M7 | **Tape, Line, area, Chain** | `P L5263-5549` | **PRESENT** | `pano/SurveyPrepFile.kt:49-51`; palette slots above | — |
| M8 | A Line or area carrying a **trade and a note** (summed in the Scope pack in m or m²) | `P L2782-2783`, `L3441-3445` | **ABSENT** | — | with W7 |
| M9 | Marking on the **360** | `P L4302` (a 360 tab), `L5906-6054` | **PRESENT in a different place** — the phone's step 4 · 360 (`pano/StepRoute.kt:84-88`; `app/SurveyPrep360.kt`). **Parity not audited here** | — | unverified |

No mark TYPE in the prototype expresses a change; change is carried by the mark's **state**, its **birth** (born a
wish or vendor-added = not in the room yet) and its **act** (`P L2742-2751`, `L2786`).

### 2.5 What makes the port cheaper than it looks

- **No file migration.** The phone reads a mark's seven keys and writes every other key back where it was, at file,
  surface and mark level; a mark with a type it does not know is kept and drawn grey (`pano/SurveyPrepFile.kt:17-51`,
  `:137-149`, `:207-211`; `pano/SurveyPrep.kt:650-679`). The prototype stores `life`, `act`, `note` … only when not
  default, so a v52 file saves byte-identical.
- **S5 stores the room record as the phone sent it**; the record PUT does not validate mark types or keys
  (`functions/_lib/asis.js:1387-1399`), and reading keys such as `FPLB1.cl` already pass `KEY_RE` (`:547`).
- **The texts exist.** The phone's tables already hold English and Marathi for `life.*` (79 keys, 3 used),
  `trade.*` (22, 6 used), `export.*` (111, 7 used), `type.*` (86, 17 used) and the `plan.*` arranger texts (none
  used) — counted by key prefix in `android/i18n/index.tsv`.

### 2.6 One thing that must change on S5 BEFORE the phone uploads a note or a why

S5's room GET returns the stored record whole to every caller the access list admits, vendors and clients included
(`functions/_lib/asis.js:1472-1474`, `json({ ...d, … })`). Harmless today, because a mark carries no words. The
moment a mark carries an office-only note, the why of a drop or a vendor's answer, S5 has to redact per caller
first. The prototype's own changelog names this as a precondition (`P L1921`). S5 stage first, then the phone — the
release order the suite already uses.

## 3. The site-name bug

**Status: not reproduced. Nine defects on the rename path and three beside it are confirmed by reading the code;
which one Amit hit depends on which row he held and how the phone is signed in.** The repro asks are in 3.4.

### 3.1 The path, traced

1. **Entry (the only one).** Browse → a client's Sites list → long-press a row: `app/BrowseScreens.kt:249-261` →
   `app/MainActivity.kt:2097-2107` builds `RenameTarget("site", name, client, name, "", serverId, serverKnown)`.
   A site's type and address are set only at creation (`MainActivity.kt:3269-3379`); `FrappeClient.setSiteDetails`
   (`app/FrappeClient.kt:390`) has no caller, so no screen edits them later.
2. **Which id.** `Site.serverId` comes from the masters' `sites` row, else a project's `site` docname
   (`app/Catalogue.kt:673-697`). A project with a blank `site_name` is filed under "(no site)" (`:479`). The tour's
   demo project is appended and never written into the local rows (`:507-513`).
3. **Dialog.** `app/TreeShell.kt:601-757`. The grey line is chosen by `serverId.isNotBlank()` (`:669-673`): "This
   site is on the server…" or "This site has not synced yet…".
4. **Local branch** (`serverId` blank), `MainActivity.kt:1193-1211`: `cat.renameLocal(…)` — which rewrites only the
   `local_sites` preference and returns `false` when no row matches (`Catalogue.kt:220-242`) — its Boolean ignored;
   then `reload()`, `navSite = newName`, "Renamed to …".
5. **Server branch** (`serverId` set), `MainActivity.kt:1213-1242`:
   `FrappeClient.load(context)?.renameNode(…) ?: error("no server configured")`, then `bootstrap()` + `saveMasters`
   in the SAME `runCatching`; on failure only `lastResult` is set. `FrappeClient.load` reads the legacy ERP key pair
   alone (`FrappeClient.kt:413-420`).
6. **Bench (S1).** `mallet_estimator/sitephoto.py`: `rename_node` `768-828` is a field update on
   `Mallet Site.site_name` with a clash scan over ALL sites (`810-815`); `worksite.py` `find_site` `223-232` is per
   customer.
7. **S5.** No site anywhere: `asis_projects` holds project, title, customer (`functions/_lib/asis.js:1924`). On the
   phone, `app/S5Session.kt:298-348` keeps the old bootstrap entries and `sites` array, and stamps each S5 project's
   `site_name` from the phone's local row at fetch time (`:338-339`). `AsIsUpload.kt:456-462` sends project,
   project_title, customer — no site.
8. **Draw.** The row title is `Masters.site_name(tx, s.name)` (`BrowseScreens.kt:252`), which maps only "Main site"
   and "(no site)" (`pano/i18n/Masters.kt:213-217`); a typed name passes through. The result banner is drawn in ONE
   place, the room screen (`MainActivity.kt:3074`); the four-level tree returns before it (`:2135`).
9. **Prototype.** `renameDialog` `P L7924-7933`, `doRename` `P L7934-7938` — it re-keys sites, projects and the queue
   and **never fails**, so the prototype has no failure state to copy.

Slices M2 and M6 and the tour: M2 only keyed the dialog's texts; M6 changed nothing here; the tour appended the demo
client, site and project to the tree with the long-press still live.

### 3.2 The defects

| # | Defect | Evidence | What Amit would see | Confidence |
|---|---|---|---|---|
| R1 | **Renaming a site the phone believes is "on the server" needs the legacy ERP key; a phone signed in to S5 has no working route.** S5 has no rename route and stores no site name | `MainActivity.kt:1193`, `1215-1218`; `FrappeClient.kt:413-420`, `442-451` (a long-dead pair in prefs, 23 Oct); `S5Session.kt:12-29`, `306`, `310-317`; `mcft-erpnext-context/analysis/s5-s6-integration.md:235` (`rename_node`: "none (masters are ERP's) … vanilla PUT /api/resource/* or stays staff-desk work") | Save shows "Saving…", then "Save" again, dialog still open, name unchanged, no reason given | confirmed by reading; **most probable cause, not proven** |
| R2 | **A failed rename shows nothing.** The failure text goes to a banner drawn only on the room screen; the dialog's red text is fed by the delete blocker only | `MainActivity.kt:1238-1240`, `3074`, `2135`, `1245`; `TreeShell.kt:687-718` | every refusal (no key, dead key, no signal, the bench's clash) looks the same: nothing; the reason turns up later as a stale banner in a room | confirmed by reading |
| R3 | **The local branch reports success without checking anything was renamed.** Rows with a blank id and no local row: "(no site)", the tour's "Demo Site, Pune", an S5-listed project's site whose local row is gone. The delete path was already fixed for this exact shape (`Deletes.route`); rename was not | `MainActivity.kt:1194`, `1202-1211`; `Catalogue.kt:220-242`, `479`, `507-513`; `MainActivity.kt:1256-1261`; `pano/DeleteRoute.kt:56` | the dialog closes onto an empty site with the new name; back, and the old name is still listed with its projects | confirmed by reading |
| R4 | **For a project S5 lists, the site name on screen is a cached copy the rename does not update** | `S5Session.kt:338-339`; `Catalogue.kt:496-502`; `MainActivity.kt:1194`, `1202`, `476-481` | the old name stays until the next sync, then changes by itself | confirmed by reading |
| R5 | **The bench refuses a site name any OTHER client's site already uses** (several clients have a "Main site") | `sitephoto.py:810-815` vs `worksite.py:223-232`; `Catalogue.kt:803-810` | with a live key: refused, and per R2 silently | confirmed by reading |
| R6 | A failed refresh after a successful server rename is reported as a failed rename | `MainActivity.kt:1215-1226`, `1230-1240` | on a weak signal the bench has the new name and the phone says nothing and keeps the old one | confirmed by reading |
| R7 | The site's chosen rooms are keyed by the site's NAME and do not follow a rename | `Catalogue.kt:324-335`, `359`, `376-377`; `MainActivity.kt:2850` | after a rename that lands, the rooms grid falls back to the 2 BHK preset and the chooser asks again | confirmed by reading |
| R8 | A server-side site rename leaves this phone's unsynced projects under the old name, and their sync re-creates the old site on the bench | `MainActivity.kt:1213-1237`; `SyncWorker.kt:253-262`; `worksite.py:263-278` | a second folder with the old name | confirmed by reading |
| R9 | The phone-minted S5 project key is a hash of client\|site\|title, so a rename before the next sync can fork the project on S5 | `S5Session.kt:252-263`, `272-276`; `SyncWorker.kt:143` | two S5 projects with one title; stage, wish list and album split | **possible** (narrow window) |
| R10 | Beside it — a PROJECT or CLIENT rename leaves the phone's captures under the old name (the prototype re-keys the queue, `P L7936`; the phone does not) | `MainActivity.kt:2019-2020`, `2606-2608`; `CaptureStore.kt:160-212`; `SyncWorker.kt:254-255` | a renamed project's rooms show no photos; the dialog's "Photos already filed under it keep their place" is untrue | confirmed by reading |
| R11 | Beside it — the approved v37 fix was never ported: Choose rooms, + Room and the first-visit chooser sit below the tree's early return | `MainActivity.kt:2135`, `2671`, `2847`, `2860`; `P L8262-8265`, `L8294-8296` | on the project screen those chips open nothing until a room is opened | confirmed by reading |
| R12 | Beside it — opening a client with one site resolves that site by NAME across all clients | `MainActivity.kt:1534-1540`, `3392-3401`; `Catalogue.kt:514-515`, `803-810` | could open another client's project under this client's crumbs; **not seen on a phone** | confirmed by reading |

### 3.3 The proposed minimal fix, and its test

The root cause is not confirmed, so the fix below is the part that is right **whichever** defect Amit hit — it makes
a rename either happen or say why — and it leaves the one real decision (R1: where a site's name lives for an
S5-signed phone) to question 3 in section 5.

1. **One tested route, as delete already has.** A pure `Renames.route(…)` in `:pano` beside `pano/DeleteRoute.kt`,
   deciding from the kind, the server id, whether a local row exists and whether a live ERP pair is held:
   *local* (rename the row), *server* (send it), or *refused with a reason*. It closes R3 (no local row → refused,
   navigation untouched; "(no site)" and the demo rows get no rename) and makes R1 honest (no live pair → refused
   with the words, instead of a call that cannot succeed).
   **Test:** `RenameRouteTest` in `:pano` — builds and runs in this container (`gradle :pano:test`): one case per
   row kind above, including the demo site, "(no site)", an S5-listed site with no local row, a server id with no
   pair, and a server id with a refused pair.
2. **Say the failure in the dialog** (R2): an error line under the field, set on failure, cleared on edit and
   dismiss. **This is a UI change, so it is drawn on the prototype first** — the prototype's `doRename` has no
   failure path at all.
3. **One source for the site name** (R4): stop copying `site_name` into S5 entries at fetch; resolve a blank one from
   the local row where the tree is read. A copy that can go stale is a second answer.
4. **Judge success on the rename alone** (R6), and move the room-scope keys with the name (R7), re-key the matching
   local rows (R8) and captures (R10) in the same step.
5. **S1, Python only** (R5): compare within the same customer; a test with two clients sharing a site name. No
   schema change, so no patch entry.

Everything under `android/app/` is compiled only by CI: `batch-next`, wait for `android-app.yml`, then merge.
R9, R11 and R12 are separate changes and are listed so they are not lost, not because they explain the report.

### 3.4 What to ask Amit, one step at a time

1. Which row was held — what did the dialog's title say ("Rename or remove site", "Rename client", "Rename or remove
   project"), and what kind of name was on the row ("Main site", "(no site)", "Demo Site, Pune", or a typed one)?
2. What did the grey line under the field say: "This site is on the server…" or "This site has not synced yet…"?
3. After Save: did the dialog stay open (Saving… then Save again), or close onto a list showing only "New project"?
4. Open any room now: does the grey banner at the top hold "Could not rename: …" or "Renamed to …"?
5. Menu → Server: signed in with email only, or is an "ERP key (legacy)" also stored or shown as refused?
6. After Menu → Sync now with signal, did the name change by itself?

Answer 2 + 3 alone separate R1/R2 (on the server, dialog stays) from R3 (not synced, closes onto an empty site) from
R4 (closes, old name still shown, fixes itself after a sync).

## 4. The delta — what Amit's direction needs that NEITHER the prototype nor the phone has

### 4.1 His journey, laid on what exists

The six quote stages (S5, one per project: `Survey → Client wish list → Vendor quoted → Estimated → Revisions →
Client approved`) and the prototype's five phases already have the SHAPE of what he described. What is missing is
the commercial content of the middle three.

| Amit's step | Stage that exists | What exists for it | What is missing everywhere |
|---|---|---|---|
| "first survey will be done" | Survey | phone, shipped | — |
| "on same survey fotos, client wish list will come" | Client wish list | **prototype only**: marks born a wish, acts, notes, on the same squared faces (W5–W9) | D1 below |
| "vendor will review these wish list for quotation" | Vendor quoted (the prototype's phase "3 Vendor review") | **prototype only**: per-trade Vendor review pack, hand-shared; the vendor's per-mark yes / back / dropped keyed by staff (V5, V7) | a record of WHICH vendor got WHICH pack |
| "vendor will submit their quote" | Vendor quoted | **nothing** — "Vendor quoted" is only a label on the project | D3 |
| "send this wish list to multiple vendors using RFQ … compare all quotes" | Vendor quoted → Estimated | **nothing** — one free-text vendor per service row; one state per mark | D3, D4 |
| "revisions for the wishlist based on quoted amount, where we really need it or not" | Revisions | a mark can be Dropped with a why or go back to Client wish; no round, no history, no amount | D5 |
| "then quote will be finalised" | Client approved | shipped on phone and S5: PM approves, the sixteen-work scope, final; the wish list locks | which vendor's quote won, per service line |
| "post that work will start" | (after) Client approved | shipped: the Album by work, states, change requests | — |

Checked by search: `RFQ`, `rfq`, `quotation`, `Supplier` and the rupee sign occur **0** times in the prototype;
no `rfq` in the phone's source or in S5's `functions/`, `app/src`, `migrations/` (the one hit under `android/` is a
git-ignored build jar, a byte coincidence). S5's migrations 0001–0010 hold no table for a vendor's quote.

### 4.2 The delta items

**D1 — Wishes as marks: what to ADD to the prototype's model, not replace.** The prototype already puts a wish on
the photo (section 2.2). Against Amit's words, four things are missing in it:

- **Kinds of wish named as he names them.** Today: a mark *born a wish* (new) and four acts on an existing mark —
  Replace, Shift, Repair, Remove. Missing: the word **New** on a mark born a wish (the pill says only "Client
  wish"); a **close-up** act ("close this door with brickwork" is not Remove — something is built in its place); a
  structured **"replace with"** (today free text in the note, with no link between the thing removed and the thing
  installed).
- **Each wish tied to a service line.** A mark's trade is fixed by its TYPE, except for a Note, Other, a Line or an
  area (`P L2482-2491`). So the door-to-brickwork wish goes to the Doors & windows vendor and the civil vendor sees
  a grey outline with a code and no words. A wish needs a service line of its own — possibly more than one (remove
  the window: civil making-good + the new UPVC window: doors & windows).
- **Wishes that are areas or lengths.** A Line or an area takes a trade and a note but never a state
  (`P L2782-2783`), so a painted wall, a tiled floor or a run of POP cornice cannot be a wish. Tiling has no type of
  its own, Painting only Defect, Civil only existing structure (`P L2501-2502`).
- **What becomes of the twelve-row text list.** Amit: "i want to mark it on foto itself rather than listing as
  text". Today the list is ticked by hand and merely COUNTS marks. By his rule of 27 Oct (one state, in one place;
  derive, never a second hand-set answer) the list should be derived from the marks or go — a decision, question 4.
  S5's `PUT …/wishlist` and migration 0007 exist and are in use by 0.3.270+.

**D2 — Per-service-line RFQ documents as plain 2D diagrams.** The nearest thing is the Vendor review pack (V5):
per trade, with a "Faces · Drawings · List" choice, so a drawings-and-list pack is already "plain 2D". What an RFQ
document adds:

- **One list of service lines.** Three lists exist and do not match: Amit's six ("painting, electric work, civil
  work, furniture work, doors and windows, POP etc."), the prototype's 13 trades, and S5's 12 `WISH_SERVICES`
  (`functions/_lib/asis.js:266-279`), joined today by the prototype's `SERVICE_TRADE` table (`P L2509-2521`). His
  six all map onto trades (POP → False ceiling; electric → Electrical, with AC and CCTV & data beside it). Question 5.
- **An RFQ identity on the page**: a number, an issue date, a reply-by date, the vendor it is addressed to, what to
  quote (a line per wish with its quantity and unit). The pack has a pack number and issue time only.
- **Quantities for the vendor.** The per-trade quantity sums exist only in the office's Scope pack
  (`P L3430-3457`), after the vendor round — an RFQ needs them before it.
- **The whole flat on one sheet** (F7): no export anywhere draws the arranged flat; every drawing is one room.
- **Other rooms' faces**: photo faces are drawn only for the room that is open; other rooms give drawings and list
  only. For "plain 2D diagrams" that may be acceptable — to be confirmed on the prototype.

**D3 — RFQ to several vendors, and their quotes.** Nothing holds this today, anywhere:

- **Vendors as records.** The wish row's `vendor` is free text, one per service, never read by an export and blanked
  by S5 for non-staff (`asis.js:526-533`). S5 does know ERP Suppliers on a project's access list, with the works
  each may see in the Album — after approval, by work, not by trade.
- **Who was sent what.** The export log records WHAT was exported, never TO WHOM (`P L3718-3728`).
- **Several vendors per line.** A mark has ONE state, so two vendors' answers to the same wish cannot both be held.
- **A quote**: an amount per line (or per wish), terms, validity, the day it came, and against which revision of
  the wishes. No field for any of it exists on a mark, a service row or a pack.
- **How a vendor answers.** Today staff key a vendor's per-mark answer; a vendor cannot sign in on the phone
  (`app/S5Session.kt:128-131`) and S5 has no vendor page for the quote journey. Question 6.
- **The candidate backend, named and not designed:** ERPNext's standard Buying doctypes **Request for Quotation**
  (one RFQ to several suppliers) and **Supplier Quotation** (each supplier's reply), with ERPNext's standard
  supplier-quotation comparison report. They fit the rule approved on 10 Nov 2026 — ERP stays vanilla and S5 talks
  to it only through ERPNext's built-in API (`/api/resource/*`); the design home is
  `mcft-erpnext-context/analysis/s5-s6-integration.md`. **Unverified in this audit:** these doctypes were not read
  on the bench, and how a wish mark becomes an RFQ line (ERPNext's RFQ lines are Items) is undecided. An existing
  decision it would meet: subcontracted work is a full Estimate SKU with the vendor rate standing in for shop cost
  (Amit, 2026-08-25).

**D4 — Comparing quotes.** No screen, no data. Wherever it is drawn, three standing rules bind it: a vendor's
prices are cost data and never enter a repository (so every test and prototype uses invented figures); no vendor
names in client-facing output; and the word "margin" is never used.

**D5 — Revisions with a history.** A mark keeps only its current state, its time and a why; Revisions has no
round number and no before / after (`P L4166-4169`). "Revisions … based on quoted amount, where we really need it
or not" needs: the amount visible beside the wish being weighed, a record of each round (what was dropped, changed
or kept, and why), and a re-quote to a vendor for a changed wish that does not lose the first quote.

**D6 — Finalise.** Client approved already exists and is final, with the sixteen-work scope and change requests
after it. What the new direction adds is the join: which vendor's quote was accepted for each service line, and
that being what the estimate and the approved scope are made from. S5's own notes record that no stage moves
automatically because an estimate has no link to an as-is project (`erp_quotation` has no writer).

**Two constraints that cut across all six:**

- **One state, in one place.** An RFQ's status (sent, quoted, awarded) per service line and vendor is a new status
  by nature. By the rule of 27 Oct it has to be DERIVED from the RFQ and quote records themselves, not kept as a
  third hand-set stage beside the project's quote stage and the mark's state.
- **Redaction first.** Section 2.6: S5's room GET hands the whole prep to any admitted vendor or client. With
  several vendors on one project, one vendor must not read another's answers, notes or amounts.

### 4.3 The order it implies (an observation, not a plan)

The port of what is already approved comes before any of the new design: **M3/M4 → W5–W7 (the life cycle and the
Mark sheet) → V2 (trades) → V4 (Export sheet and packs)** is the base that D1 and D2 extend, and F2 (the floor-plan
arranger) is independent of all of it and can ship on its own. D3–D6 need a backend decision before any screen.

## 5. Open questions for Amit (each one a selector; recommended option first)

1. **S2 APK — which floor plan?**
   (a) The project's "Floor plan" tile and the arrange-the-rooms page (stage 1, by hand) — port it now, as approved.
   (b) That, and it must also export the whole flat as one drawing before it ships.
   (c) Something else — the per-room PLAN tab is what is wrong.
2. **S2 APK — the site rename: what happened on screen?**
   (a) The dialog stayed open after Save and the name did not change.
   (b) The dialog closed onto an empty site with the new name; the old name is still in the list.
   (c) The old name stayed, then changed by itself later.
   (d) Something else (the six asks of section 3.4 follow, one at a time).
3. **S2 APK + S5 QUOTE + S6 ERP — where does a site's name live for a phone signed in to S5?**
   (a) Claude fixes the phone to say plainly when it cannot rename, now; the home of the name is decided with the
   S5–S6 integration.
   (b) S5 keeps the site name (a column and a guarded write).
   (c) S5 passes the rename to ERP through the standard API.
   (d) Renaming a synced site is desk work in ERP; the phone renames only what has not synced.
4. **S2 APK + S5 QUOTE — the twelve-row text wish list, once wishes are marks on the photos?**
   (a) Keep it as a summary that is DERIVED from the marks — nothing ticked by hand.
   (b) Keep it as it is, ticked by hand and counting the marks (the prototype today).
   (c) Remove it; the marks are the wish list.
5. **S2 APK + S5 QUOTE — which list is "the lines of service" an RFQ goes out by?**
   (a) The prototype's 13 trades (Amit's six all fit: POP = False ceiling).
   (b) Amit's own short list — painting, electric, civil, furniture, doors and windows, POP — with the rest folded in.
   (c) S5's 12 wish-list services.
6. **S5 QUOTE + S6 ERP — how does a vendor's quote come in, and where is it kept?**
   (a) The RFQ document goes out as a PDF; staff key each vendor's quote; ERPNext's standard Request for Quotation
   and Supplier Quotation hold it.
   (b) The same records, and the vendor enters the quote himself on a page of his own.
   (c) Kept on S5 only for now; ERP later.

Not asked, and decided by nobody yet: whether a vendor may ever see amounts or other vendors on the phone (today
the phone is staff-only), and whether the Marathi of the new screens follows the standing rule (Claude translates,
Amit tests on the phone) — it does unless he says otherwise.
