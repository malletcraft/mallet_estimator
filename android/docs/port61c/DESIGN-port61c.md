# DESIGN-port61c.md — RECOVERED EXCERPT (3 Nov 2026, ~23:05 IST)

The full port plan lived only in the session scratchpad and was lost when the cloud container was re-created at ~22:50 IST
on 3 Nov 2026 (the scratchpad is ephemeral; nothing had committed the plan). What follows is every part of it the session
transcript still held verbatim: the slice table, §14 AS1, §15 FW1, §16 FW2, §25 M3a and the progress notes; and §18 K3,
re-derived on 9 Nov 2026 from the prototype, S5 and the shipped code (it names the source of each decision), and §19 K4,
derived the same day in the same way and built after its critic (its "The K4 critic" paragraph), and §20 K5, derived
the same day in the same way and built after its critic (its "The K5 critic" paragraph); and §26 M3b, derived on
9 Nov 2026 from a SERVED copy of the approved prototype v61c.2 (its raw Marathi table extracted and diffed against
android/i18n, its own text layer probed string by string, its survey prep screenshotted under मराठी in both
orientations), from the shipped M3a (android/i18n/README.md and §25) and from the code on batch-next; and §27 M3c,
derived on 9 Nov 2026 the same way (the served copy's PLAN tab and Export overlay driven under मराठी in both
orientations — its vis() answers, the plan SVG's own text and the export preview captured — with §25, §26 and the
code on batch-next); and §24 M2, derived on 9 Nov 2026 the same way (the served copy's tree, drawer and queue driven
under मराठी in both orientations, its embedded table diffed against android/i18n, 116 vis() probes over the tree's,
the queue's and the upload status' compositions — with §25–§27 and the code on batch-next); and §29 M6, derived
on 9 Nov 2026 the same way (the served copy's drawer and its Laser test replica driven under मराठी in both
orientations, 95 vis() probes over the laser, X3, ImageMeter, dialog and sign-in compositions, the RAW table
diffed against android/i18n again — with §24–§27 and the code on batch-next); and §30 M8, derived on 9 Nov 2026
from the shipped tables and the code on batch-next ALONE (decision 6: these screens have no prototype rows, so
there is no page to drive — the card's own laser-test keys, proven byte for byte in M6's derivation, and the
shipped survey-prep register are the vocabulary, with §24–§29); and §31 the tour, derived on 9 Nov 2026
from a SERVED copy of the approved prototype v61c.2 — its seven guided tours driven whole under English and
मराठी, 73 steps each run, every step screenshotted at the Mi A3 frame, the demo room's prep fetched from the
published artifact's own db — and from the code on batch-next, audited anchor by anchor. §28 M4 shipped on 3 Nov 2026
(0.3.272, batch C part 1) BEFORE this plan was re-derived, so it has no section; its record is
android/i18n/README.md "Port 61c slice M4" and the progress note below. The other sections
(§1–§13, §17, §21–§23) are NOT here. For those slices the sources are the approved prototype
(https://claude.ai/artifact/64peToYoeaRz9UNwB926m5, v61c.2 Version 75: its page is the UI spec and its #ch61c notes the
change log), the code as shipped (android/i18n/README.md has a section per shipped slice), S5's CLAUDE.md in
mcft-interior-estimate for every route, and this table. A line that ends abruptly was cut at a column by the tool that
printed it; the sense survives, the tail does not.

RULE RECORDED THE SAME NIGHT: a plan, a spec or a build record that later work depends on is committed to a repository
the moment it is written. The scratchpad is a workbench, not storage.

## The order at a glance

| # | id | slice | size | depends on |
|---|---|---|---|---|
| 1 | S5-W | S5 QUOTE: confirm the wish-list lock on stage with one probe | S | — |
| 2 | P0 | Prove the D1 queries and count what the port will touch | S | — |
| 3 | L1 | Land the v61c.1 tables in android/i18n (1,607 keys) | S | — |
| 4 | L2 | Land the approved card; android/i18n becomes the prototype's only source | M | L1 |
| 5 | D0 | Make the capture store revert-safe before its schema moves | S | — |
| 6 | D1 | Capture store schema 6: a nullable work column, alone | S | D0 |
| 7 | W | Wish list at v61c.1: read-only at Client approved, S5's own words, the Vendor field by phase | M | S5-W, L1 |
| 8 | K1b | pano Album.kt follows S5 as shipped, and Album is keyed | L | L1, L2 |
| 9 | AQ | pano AlbumQueue and AlbumWire: each queued op becomes exactly its S5 call | M | K1b |
| 10 | QM0 | pano: the one project line, the store's merge rule, and the guarded quote move | M | L1 |
| 11 | S5-L | S5 QUOTE: the project list carries each project's work states | S | P0 |
| 12 | PL | One derived project line; PROJECT IS AT and the ERP site stage leave the phone | L | L2, QM0, S5-L, D1, K1b |
| 13 | S5-R | S5 QUOTE: re-file one capture by work, without re-uploading it | S | P0 |
| 14 | AS1 | Capture-keyed as-is upload (task 5, port items 1-4 and 6) | L | D1, K1b |
| 15 | FW1 | Photos are read by work | M | PL, D1, K1b, L2 |
| 16 | FW2 | New photos and re-files are written by work; the last stage pickers go; S5 learns the filing | L | FW1, AS1, S5-R, PL, QV (ships after #17; critic G2) |
| 17 | QV | The album mirror and the approved quote view, with APPROVED SCOPE | L | PL, QM0, AQ, L2 |
| 18 | K3 | The album, read-only (K3) | L | QV, FW2 |
| 19 | K4 | The Project Manager's writes, through the outbox (K4, with K2's drain) | L | K3, AQ |
| 20 | K5 | The progress shot (K5) | M | K4 |
| 21 | QM1 | The phase strip and the guarded stage move | M | QV, QM0 |
| 22 | QM2 | Approve: the PM's 16-work checklist; approval is final | M | QM1 |
| 23 | AS2 | S5 QUOTE retires the old as-is paths (C6) | S | AS1 |
| 24 | M2 | Marathi: the tree, its dialogs, the queue and the upload status (design slice 2) | L | L2, FW2 |
| 25 | M3a | Marathi: survey prep chrome (design slice 3a) | M | L2 |
| 26 | M3b | Marathi: measures, readings and the words drawn on the photo (design slice 3b) | M | M3a |
| 27 | M3c | Marathi: the plan and the Export sheet (design slice 3c) | S | M3b |
| 28 | M4 | Marathi: the rest of the wish list (design slice 4) | S | W, L2 |
| 29 | M6 | Marathi: the rest of the shell and the S5 sign-in sheet (design slice 6) | M | M2 |
| 30 | M8 | Marathi: the screens the prototype never had (decision 6) | L | L2 |
| 31 | VIS | Switch the भाषा · Language row on | S | M2, M3c, M4, M6, M8, W, K1b, PL, FW2 |

### 14. AS1 · Capture-keyed as-is upload (task 5, port items 1-4 and 6)

**Size:** L · **Depends on:** D1, K1b

**Goal**

Every 360 becomes its own S5 capture:
- named by its device id;
- uploaded in the order record → photos → readings, on the explicit path;
- its record carrying the work the phone files it under.

**Files and changes**

In app/…/AsIsUpload.kt (:254-336), following the 'Kotlin port' items 1-4 and 6 in mcft-interior-estimate's CLAUDE.md:
1. **Naming.**
   - The base (:266-267) becomes …/<project>/<room>/c/<deviceId>.
   - The record's `capture` (:315) is c.deviceId, and `erp_capture` is c.serverName, for information only.
   - `captured_at` stays byte-identical (:316-317), so C3 renames an old MEST-PH capture on its first re-save.
   - The record gains `work: Album.workOfCapture(c.work, c.stage)`.
2. **Order.** The record PUT goes first. Then …/c/<id>/img/<surface>, moved from :285 to after the record; S5 answers 404 for an image without its record. Then the readings.
3. **Readings by capture.**
   - app/…/ReadingsSync.kt: url, fetch and push (:106-118) take the capture.
   - SurveyPrepScreen.kt:463 passes the row's device id.
   - A 404 stays NOT_YET (:104).
4. **A new capture.** On `created: true` in the record answer (asis.js:1315, :1319), the capture's ReadingsSync seen state is cleared before pushing, so every reading goes as a first write.
6. **Fresh readings (finding 9).** The readings are read at push time, in a commit of its own.
7. **(Critic S3) The pull asks each name the capture has had.** Until a capture's first record PUT after AS1 lands, S5 still names it as today's record named it, `serverName ?: deviceId` (AsIsUpload.kt:315), often MEST-PH-…. So `…/c/<deviceId>/readings` answers 404, and survey prep would silently stop taking web edits to that capture's readings: the 404 reads as NOT_YET (ReadingsSync.kt:104). 

**Not here:**
- item 5 (delete with a confirm) is a UI change and goes through the prototype first (task 5);
- item 7 is AS2.

**S5 must already serve**

S5's explicit capture routes (migration 0006, on stage since 14 Oct), with C3 and C4.

**Tests and checks**

- A :pano AsIsPath test:
  - each segment encoded once;
  - the record, image and readings URLs;
  - the record body builder takes no Tx (wireHasNoTx).
- The CI gate.
- Claude's stage check with d1-peek:
  - the Mi A3's next upload lands under its MCAP id, with work set;
  - an older MEST-PH row is renamed by C3, with captured_at equal;
  - asis_readings is unchanged per key.

**What Amit sees**

Nothing on the phone; survey prep still says On web ✓. On /projects each 360 is its own capture.

**Risks**

- **C3** depends on captured_at being byte-identical. It is built with ZoneId.systemDefault() (:316).
- **Older builds:** S5's old path keeps serving them until AS2.
- **A new meaning:** an image 404 now means its record did not land first.

=== progress section (tail)
## Progress, 3 Nov 2026 ~15:25 IST

0.3.272 (main 0fa7a1a = batch C part 1: K1b, AQ, M4) is on the Mi A3 from Play; Amit: "Updated — wish list reads
right" under मराठी. Batch C part 2 (QM0 ee60897+c80500a, PL 368f2ff+0154ad9) is on batch-next in CI; QV is building.
M4 is therefore done; the remaining Marathi slices run alongside: M3a, M3b, M3c, M8, M2, M6.

### 15. FW1 · Photos are read by work

**Size:** M · **Depends on:** PL, D1, K1b, L2

**Goal**

Every screen reads a capture's filing through Album.workOfCapture. The readers ship before the writers, so a revert of FW2 leaves rows that FW1 already reads correctly.

**Files and changes**

**app/…/CaptureScreens.kt**
- CaptureCard (:38-62) gains `work: Int?`. `workStage` stays for the STG line until FW2.
- CapturesScreen (:111-192) groups by work:
  - order: work ids high to low, then Quote journey, then not filed to a work;
  - each header: the work name and `n photos (over n days)` (app.x_over_x);
  - chips: `All n`, then one per work present, in place of the phase chips (:131-141).
- StageHeading (:196) becomes the work header. CapturesScreen's phases and stageOrder parameters go.

**app/…/MainActivity.kt**
- roomCaptures (:1848-1868) computes work through Album.workOfCapture(c.work, c.stage).
- The CapturesScreen call (:2314-2317) drops phases and stageOrder.
- The capture title (:1614) becomes `date · <where filed>`: Quote journey, the work, or not filed to a work.
- The FaceViewer sub-line (:1587-1589) drops the stage.

**app/…/Catalogue.kt**
- phases (:728-729) and stageOrder (:744-745) lose their last callers and go.
- canRestage (:738-739) has no caller today and goes too.

**S5 must already serve**

None.

**Tests and checks**

- AlbumTest's workOfCapture cases.
- A :pano test of the group order.
- I18nLiteralTest.
- The CI gate.

**What Amit sees**

- The room screen lists photos under work headings, with work chips. For example, an old 'Plaster & waterproofing' photo appears under Civil & masonry, and survey 360s under Quote journey.
- A photo's title reads `date · work`.

**Risks**

For one build the STG line still shows a 39-stage name above a work grouping. FW2 ships straight after.

### 16. FW2 · New photos and re-files are written by work; the last stage pickers go; S5 learns the filing

**Size:** L · **Depends on:** FW1, AS1, S5-R, PL, QV *(critic G2: QV, section 17, ships first)*

**Goal**

v61c's filing (§3.2, §4.3-§4.5):
- the capture sheet's WORK row and Which work?;
- the photo's WORK line, with re-filing, in the same build that removes STG;
- new captures carrying their work;
- S5's copy of every uploaded capture filed the way the phone files it.

**Files and changes**

**app/…/MainActivity.kt, ingest (:567-640)**
- A new capture carries stage '' and:
  - work = 0 before Client approved;
  - after approval, this project's pick (held in memory per project, like the prototype's S.capWorkFor), defaulting to the store's first In-progress work.
- Both inserts (:628, :2041) and PendingSplit (:2976-2990) carry the work.
- FaceWriter.begin and single get stage '', so Handover's optional caption stage is blank (Handover.kt:83, :100).

**app/…/TreeShell.kt, CaptureSheet (:431-463)**
- **The WORK row replaces FILED AT:**
  - before approval: `Quote journey` / `<stage> · locked until the client approves the estimate`, not a button;
  - after approval: the work and `<state> · tap to change`, or `Pick the work` / `not filed to a work · tap to change` (index61c1.html:7848-7850).
- **A new WorkSheet, Which work?** It lists the 16 works with their states and the current one selected. It never offers the Quote journey (index61c1.html:7851-7854).
  - *(Critic, §4.3, P25, P28)* A pick sets this project's work and reopens the capture sheet. A dismiss, or ◁, reopens it unchanged.
- **The shutter.** Shoot, Pick, Camera and Gallery (:563-590) are disabled while the project is approved and no work is picked.

**app/…/CaptureScreens.kt.** The STG row (:407-417) becomes the WORK line.
- **(Critic G2) Its sub-line follows §4.4 and P23.** It shows the work's state for a work, and `not filed to a work` for an unfiled capture. For a Quote-journey capture it shows the quote stage the capture was taken in: Album.quoteAt over the album mirror's stage rows (with K1b's G2b point). With no mirror read yet it reads `not read yet`, as the capture sheet's row does for an unread stage (index61c1.html:7849). The mirror arrives with QV, which is why FW2 ships after it.
- **Before approval** it is read-only.
- **At Client approved** a tap opens Which work? and re-files:
  1. CaptureStore.setWork;
  2. for a capture S5 holds, a unique job `mcft-work-<id>` calls S5-R with each name the capture has had: deviceId, then serverName on a 404. It is one capture, as S5's delete rule names it.

  A capture not yet on S5 carries the work in its next record.

**The sweep**, once per capture, in SyncWorker.doWork after BOTH legs, beside QuoteStageSync.refresh and the wish lists (SyncWorker.kt:62-75). *(Critic: the first version put it in the S5 leg after step (d), :152-163. But s5Leg runs only when no live ERP pair is stored (SyncWorker.kt:53-54), while as-is uploads go up from survey prep in either leg. On a phone holding a pair, the sweep would never have run.)*
- Every capture whose upload reads on web and whose workOfCapture is not 0 gets the same re-file job. S5 filed every one of them at 0 (AsIsUpload.kt:309-321 with asis.js:617-623).
- **(PL review)** It does NOT reach a capture taken on an approved project between PL and FW2 with no FILED AT pick: its workOfCapture is 0 on the phone and on S5 alike, so it stays in the Quote journey until re-filed by hand on the WORK line (PL's risks say how PL ships around it).
- The done-marks live in asisupload/<id>.json.

**Deleted:**
- StageSheet (TreeShell.kt:182-258) and its two remaining callers (MainActivity.kt:1776-1786, :1897-1907);
- `stage` (:211), showStagePicker (:176), retagStage (:208) and onPickStage (:1665);
- retag's stage half (:1750-1775); the SKU half stays;
- CaptureSheet's stage and onStage arguments (:2186, :2194);
- CaptureStore.setTags' stage parameter (:154-161);
- FrappeClient.setCaptureTags' workStage parameter (:326-332); its SKU use stays.

**Keying.** The capture sheet and CaptureScreens.kt are fully keyed, and CaptureScreens.kt joins ported.txt. These rows leave unkeyed.tsv: the capture sheet's FILED AT (TreeShell.kt:461, the row WORK replaces) *(critic C2: CaptureScreens.kt's two `FILED AT` literals, :441 and :479, are the FOLDER block. SPEC61c §4.4 keeps that block unchanged, so those two are keyed to app.filed_at, not deleted. SPEC61c §9.4's 'FILED AT in CaptureScreens.kt' meant the same folder block)*, Move the project to, Stage for new photos, Stage of this photo, STG, No stage set, no phase, and tap to correct (recorded

**S5 must already serve**

- S5-R on stage.
- The record's optional `work` (asis.js:1280).

**Tests and checks**

- A :pano filing test:
  - the new-capture rule;
  - the default pick;
  - no default when nothing is In progress.
- HandoverConventionTest and AlbumTest.
- I18nLiteralTest (c) and (d).
- The CI gate.
- Claude's stage check with d1-peek: the swept and re-filed records carry the phone's work, and nothing else moved (updated_at and the current capture are unchanged).

**What Amit sees**

- **Capture 360's first row is WORK:**
  - before approval it reads `Quote journey · <stage> · locked until the client approves the estimate`;
  - on the approved project it shows the work picked, or the first one In progress, or `Pick the work` with the shutter disabled.
- **A photo's WORK line** re-files it with a tap after approval.
- **No stage list** appears anywhere.

Amit's walk, one step at a time: a room on a quote-journey project, then a room on the approved project.

**Risks**

- **No work to file to:** until K4 nobody can set a work In progress from the phone, so on the approved project every capture needs a pick first. That is the album camera's rule.
- **The ERP leg** (SyncWorker.kt:227-236) now sends a blank work_stage for new rows, and S1 files them under the frozen mallet_stage. Accepted in §13.3.

**The FW2 review (3 Nov 2026), five confirmed findings, each fixed or recorded in the review commit**

1. **The sweep sends ONE S5-R PUT for every capture 0.3.283 (AS1) uploaded** — recorded, no code change. The done-mark `told` is written by the record PUT only since FW2, so an AS1 upload has none although its record already carried the work; `Album.needsRefile` reads no mark as never told and the sweep sends one idempotent PUT per such capture. S5 answers 200 and sets the SAME `work` again with `work_by` = the login and `work_at` = the sweep's time; `updated_at`, `updated_by` and the room's current capture stay. **So the stage check above reads `work_by` / `work_at` on AS1 rows as this one PUT, not as a changed filing** — and d1-peek as written prints neither column, so it would not even show them. Back-filling the mark at `done` for an AS1-or-later upload would save the PUT and is more machinery than it saves (WorkRefile.kt's header says the same).
2. **The photo's WORK line for a project only on this phone reads `Survey`, the prototype's photo line** — fixed. The prototype's two rows differ on a never-sent project: the capture SHEET's row reads `not on S5 yet` (workRow61c, index61c2.html:7878) and the photo's line reads quoteAt's `Survey` for a project S5 does not list (7249: "its captures are filed at Survey, S5's default for the project its first upload creates"; captureScreen 8205). FW2 read `not on S5 yet` on both; the phone now follows each row's own words — `Album.photoQuoteLead` for the line, `Album.quoteLead` for the row — and the contradiction is the prototype's to rule on at the phone walk.
3. **`ingest` refuses a capture on an approved project with no pick, where the prototype files it at null** — recorded as a deliberate departure, no code change. The prototype's ingest (8016-8019) has no refusal and files `{work: null}`, "not filed to a work", drawn `Pick the work · not filed to a work · tap to change`. On the phone NULL in the store is a pre-FW2 row filed by its legacy stage (schema 6), so a NEW unfiled row cannot be told from one and would file at 0 through `Album.workOfCapture` and reach S5 as the Quote journey. So the phone refuses with the sheet's own words. It can only happen on the LAUNCHER routes (picker, photoPicker, camera: the result goes to the latest composition's ingest, whose `newWork` is re-read when a sync moves the stage to Client approved while the gallery or the camera app is open); the X3 shoot calls the ingest of the composition that drew the enabled button, so it cannot refuse. The JPEG of a refused camera shot stays in `shots/` and nothing is inserted.
4. **A refused re-file wrote the done-mark, recording the disagreement as agreement for good** — fixed. `WorkRefileWorker`'s refusal branch (400, 403, a 401 on the ERP key, S5's 409) now leaves `told` alone, so the sweep asks again at every sync (one small PUT) until S5 takes it — a sign-in or a re-keyed ERP key being the usual cure — and keeps S5's words beside the mark (`refile` in asisupload/<id>.json, `AsIsUpload.markRefileRefused` / `refileRefused`), drawn on survey prep's web line as `Not re-filed on the web: <S5's words>` (`app.not_refiled_web_x`, born keyed) and cleared by the next re-file S5 takes or any state change. The capture this matters for is the one S5-R exists for: its local copy removed, so no record PUT can ever carry the work. Survey prep opens only for a capture whose 360 is on the phone, so for THAT capture the words are not reachable on screen yet; the sweep's re-ask is what cures it.
5. **The WORK line printed `Survey` where the phone could not know** — fixed. `Album.quoteTakenAt` answers null — `not read yet` — for a stage NAMED without its stamp (`QuoteStage.named`, `atUnknown`: the project line reads `<stage> · not read yet` for the same copy) and, with no mirror held, for a capture older than the current stamp or a current stage outside the six; a project at Survey with no stamp never moved, so its captures are Survey's; with a mirror the answer is `Album.quoteAt`'s, the album's own rule over the whole log. The critic G2 line above ("With no mirror read yet it reads `not read yet`") is what ships now for a capture older than the stamp; a capture after the stamp reads the current stage, as before. The album's chapters keep the prototype's default.


### 18. K3 · The album, read-only

**Size:** L · **Depends on:** QV, FW2 *(both on main at 304cf8b)*

*Re-derived on 9 Nov 2026, not recovered: the §18 the lost plan held is gone. Every decision below names its source:
**[P n]** the approved prototype v61c.2 (index61c2.html, line n: the core window.ALB60 7070–7338, the page
window.__mcftAlbum60 7342–7586, its mounting 7723–7726); **[S5]** S5 QUOTE as shipped on stage (mcft-interior-estimate
9a51733: functions/_lib/asis.js, and its CLAUDE.md row "THE ALBUM STORAGE AND ROUTES ARE REAL"); **[Ph]** the phone as
shipped on batch-next 304cf8b (file:line); **[C]** decided by Claude here, with the reason given.*

**Goal**

The Project Album on the phone, exactly as the prototype draws it, with NO write of any kind: the tile that opens it, the
header, chapter 0 (the quote journey, six quote groups and the unfiled photos) and chapters 1–16 (the works in S5's
order, each with its state, live change request, decided requests, payment milestones and photo grid), the lock while
the estimate is not approved, and the photo viewer with ‹ ›. It reads S5's album through the mirror QV keeps
(AlbumSync.kt) and the phone's own captures, through the one visibility rule (Album.canSee) and the one filing rule
(Album.workOfCapture). Every control the prototype draws that WRITES is absent (or, for the state chip, drawn as the
prototype draws it for a person it offers no sheet to), in the place K4 and K5 will add it, so they add and never move.

**Where it is opened from, and how it is left**

- **The Album tile is the LAST child of the project's rooms grid** [P 8137]: after the shot rooms, the `+N` / `Hide`
  tile and, with the full list open, the other rooms and `+ Room` [Ph BrowseScreens.kt:385-425]. Its look is the
  prototype's `.tile.alb59t` [P 1448-1451, 7385-7387]: the tile's square in the tertiary container colour (`--tc` =
  `tertiaryContainer`), the album glyph (30 × 24 dp, two offset frames and a hill, drawn in Compose from the
  prototype's SVG, GLYPH [P 7363]) at 8 dp from the top left, the count of photos this person may see bottom right
  (`labelSmall`, 7 dp in), `Album` bottom left (`album.album`, `labelSmall`, 72 % wide, one line). Its TalkBack label is
  `album.aria.tile_x` (`Album of <title>: N photos, survey to handover`). Shown on EVERY project, a project only on
  this phone included [P 7385: only the v53 lens hides it; v61c.2 draws a never-sent project's album].
- **Opening it shows the album IN PLACE of the project's rooms**, a sub-screen with no bottom bar and no crumbs [P 7487:
  `K.scaffold` with no `onTab` draws neither, 7791-7793], as the wish list is drawn [Ph MainActivity.kt:1424-1437]. The
  top bar is `TreeTopBar` [Ph TreeShell.kt:111]: `Album · <title>` (`album.title_x`), the sub-line `N photos · survey to
  handover` (`album.sub_x`), Back, and Search (the search overlay, which is not a leave, as for the wish list). The
  branch sits BEFORE the wish list's, as the prototype's main() puts the album first [P 8271]; opening the album needs
  no leave of the wish list, which replaces the grid the tile is on.
- **It is let go** when another project, a room, another tab, survey prep or Face Prep opens [P 8243 view()], by an
  effect shaped like the wish list's [Ph MainActivity.kt:334-339]. **Back** closes the viewer first, then leaves the
  album to the project [P 7392-7398 back(), 8341; the sheets and the camera it also unwinds are K4's and K5's]. The
  open chapters and the open photo are this visit's only and go with it [P 7351-7352 memOf, 7389 leave()].

**What feeds each element — S5, the mirror and the phone**

- **Who is looking.** The phone signs in STAFF only: S5Session.login ends any other session [Ph S5Session.kt:103-110].
  S5's albumRole then answers `pm` when the project names no PM or names this login, else `designer` [S5 asis.js:597
  albumRole, D3] — and canSee gives both EVERYTHING [S5 asis.js:620; Ph Album.kt:779]. So on this phone every photo,
  every capture and every log row S5 holds is visible, and the role decides only one K3 element, the PM's `To review:
  N` [P 7409], plus the `Viewing as` line. The role is S5's word, never the phone's [Ph Album.kt:562 viewerOf]: with no
  mirror held, AlbumPage.viewer answers DESIGNER for VISIBILITY ONLY — both staff roles see everything, and DESIGNER
  draws no PM-only line [C: the role is S5's answer and is not read yet; a PM's line drawn from a guess would be the
  confident answer the project forbids] — except for a project S5 does not list, which never has a mirror (AlbumSync
  forgets on 404) and for which S5's own rule answers: albumRole gives staff `pm` when there is no project row (`!p ||
  …`, asis.js:598, D3), and the phone signs in staff only, so AlbumPage.viewer is the PM there and `Viewing as Project
  Manager` is drawn, as the prototype draws it [the K3 critic, finding 11]. Were S5 ever to answer `vendor` or `client`, Album.chapters already applies
  canSee, a vendor's locks and its mine-only change requests (AlbumTest pins them); nothing in K3 branches on them
  beyond that, and vendor and client sign-in stay K6 [S5 CLAUDE.md: "vendor and client sign-in on the phone (K6)"].
- **The mirror** (AlbumSync.mirror, filesDir/album/<project>.json, S5's own shape [Ph AlbumSync.kt:89, AlbumWire.kt:246])
  ALREADY holds everything K3 draws from S5: `role`, `mine`, `scope`, `pm`, `works[16]`, the whole `log` (paged from
  `?since=0`, then `?since=<log_next>` while `log_more`, Album.paged / nextPage [Ph AlbumSync.kt:109-151]), `stages[]`,
  `photos[]` (S5's NEWEST 1000 this caller may see, each with `bytes` > 0 — the photo query's own term `bytes > 0` [S5
  asis.js:1823], so a record whose JPEG has not landed is never listed), `captures[]` (`{room, capture, captured_at,
  stage, work, by, at}`, filed by S5's workOfRecord [S5 asis.js:645]) and `more` [S5 asis.js:758-804 albumView]. Nothing is added
  to the mirror's file.
- **When it is read — K3 widens the project-open read, nothing else** [C]. QV reads the mirror on project open only
  where `Phases.needsMirror` (Revisions, Client approved) [Ph MainActivity.kt:562, Phases.kt:204]. The album draws from
  it at EVERY stage (chapter 0's captures and stage moves are S5's from the first upload), and its tile on the project
  screen counts S5's photos and captures. So the project-open read becomes "every project the store holds as LISTED"
  (`QuoteStageSync.cached(context, id)?.listed == true`, after the list read on the same open): one album GET per
  project open, a 404 forgetting the mirror as before. The hourly SyncWorker read stays at `needsMirror`
  [Ph SyncWorker.kt:79-89] — an hourly GET per project at every stage would buy the album nothing the next project open
  does not. The album screen asks nothing of its own on open: it is opened from the project screen whose open just
  read, and it redraws on every mirror write (AlbumSync.prefs listener, `albumTick` [Ph MainActivity.kt:568-576]).
- **The project's stage and the works' states come from the ONE store**, never from the mirror file
  [Ph AlbumWire.kt:240-245 stored(): "K3 passes [Album.chapters] its `states`, `quoteStage` and `quoteStageAt` from
  there, never from this file"]:
  `QuoteStageSync.cached(context, stageKey)` [Ph QuoteStageSync.kt:93] — `stage`, `at`, `atUnknown`, `states` (S5-L's
  `work_states` from the list GET, the album GET's `works[]` laid in by QuoteStage.fromAlbum). No queue is laid over
  them: the outbox is K4's (Album.withQueued stays uncalled). For a project S5 does not list the states are the empty
  map (nothing can be recorded on a project S5 has no record of); for a listed project whose copy holds no states
  (`states == null`, "not read in this shape" [Ph QuoteStage.kt:73-78]) they stay NULL and the album says `not read yet`
  where a state would stand [C, the PL review's rule: "never a count it has not read" — QuoteStage.projectLine says
  `Execution · not read yet` for the same copy]. **[Superseded by the K3 review, finding 2: a state the prototype does not have; the album draws the
  prototype's Not started / `No work open yet` for such a copy until Amit approves it on the prototype — below.]**
- **The header's project line** is the project screen's own, `QuoteStageSync.lineFor(context, stageKey, tx).text`
  [Ph QuoteStageSync.kt:130; P 7412: the header draws `P54.projectLine(p).text`, the first line only].
- **The phone's own captures** come from the capture queue the room screen reads (`queue`, rows whose `projectTitle`
  equals the project's title ignoring case [Ph MainActivity.kt:2132-2135], the same rule, so the album and the room
  screen hold the same captures), 360s and single photos alike [P 7227-7229 items(): every queue row of the project].
- **The JPEGs** are fetched ON DEMAND, only for the cards and the viewer actually composed, and kept on disk:
  - an album photo: `GET /api/asis/<project>/album/photos/<id>/img` [S5 asis.js:982-989: 404 unless `bytes` > 0 and
    canSee; `cache-control: private, max-age=300`]. A photo's bytes are written ONCE [S5 asis.js:960-965], so a fetched
    file never goes stale and is never asked for again;
  - an as-is capture S5 holds: its FRONT face, `GET /api/asis/<project>/<room>/c/<capture>/img/front` [S5
    asis.js:1607-1615, the explicit capture route of 0006] — S5 holds six squared faces, not the 360 the prototype
    draws [P 7368 thumbBg(pano)], and front is the face the phone already names a capture by (the folder line,
    Handover.filename(id, "front") [Ph MainActivity.kt:1932]) [C]. Its cached file is named by S5's `at` of the capture
    as well (capturesOf's `at: r.updated_at`, which every record PUT moves and S5-R's re-file does not; kept as
    Album.Photo.uploadedAt and written into the stored mirror's `captures[]`), because a re-saved survey prep re-squares
    the six faces under the same capture name (SurveyPrepScreen.kt:1024-1035 → AsIsUpload, asis.js:1596-1598 overwrites
    the R2 key) [the K3 critic, finding 2];
  - a capture the phone holds: the phone's own file (`panoPath`, the 360 or the single photo [Ph CaptureStore.kt:99]),
    no network, exactly as the room grid draws it [Ph BrowseScreens.kt:795-808]. Its bytes are wherever the capture
    is: when the kept file has gone (Remove local copy [Ph MainActivity.kt:1915-1921]) and S5 holds the capture, its
    front face from S5; with neither, the plain block. That is one rule about where a capture's bytes are, not a
    second answer to anything: it is the same capture [C].
  The cache is `cacheDir/album-img/` — NOT filesDir/album, where the mirrors live and where K5's queued shots will
  (AlbumQueue.SHOTS = "album/shots") [C], so Android may evict it and a fetch simply runs again. Each file is named by a
  SHA-256 of what it is (the project and the photo's id, or the project, room, capture and S5's `at`), so a long
  Devanagari room is still one short name (S5's own backup hit ENAMETOOLONG on exactly that); the folder is held to
  256 MB, the least recently drawn files (by mtime, which a hit moves) going first down to 192 MB; each fetch is
  CANCELLED with the card that asked (AsIsUpload.fetchBytes enqueues the call and cancels it on the coroutine's
  cancellation), so the two slots never serve cards scrolled past; a 404 is remembered for the VISIT
  (AlbumImages.newVisit when the tile opens it), not re-asked every time a card scrolls back [the K3 critic, finding
  9]. A 401 renews the S5 sign-in once, as AlbumSync does [Ph AlbumSync.kt:125-131]. Sign-out forgets it with the
  mirrors (AlbumSync.forgetAll [Ph AlbumSync.kt:185]).
- **Offline** the album opens and draws from what the phone holds: the stored mirror, the store's stage and states,
  the phone's captures and the cached JPEGs; a JPEG not cached yet stays the plain block (no spinner, Thumb's own rule
  [Ph Thumbs.kt:105-109]). Nothing is queued, because nothing writes. The album shows NO read time: its header draws
  the project line's FIRST line only, as the prototype's does [P 7412], and QuoteStage.projectLine puts the read time
  only into the second line — and at Client approved into neither (showRead false) [Ph QuoteStage.kt:403-432]. A mark
  of its own would be a UI the prototype does not have, so it goes through the prototype first (Amit, 11 Oct 2026);
  recorded, not built [the K3 critic, finding 5].

**THE ONE FILING: the album is not a second filing of the same captures**

Each capture is ONE item, filed by ONE rule [C, from Amit's rule of 27 Oct: a project has one state, in one place; FW1
review]:
- **A capture the phone holds** is filed as the room screen files it, `Album.workOfCapture(c.work, c.stage)` [Ph
  MainActivity.kt:2142], whatever S5's copy says: the phone is its copy's authority — its record carries the work (AS1)
  and a re-file goes to S5 through S5-R and the FW2 sweep, so S5 converges on the phone's filing. It is matched to S5's
  capture by each name the capture has had: its `deviceId` (every upload since AS1), else its `serverName`
  (MEST-PH-…, which S5 keeps until C3 renames it at the first re-save) [Ph AsIsPath.kt:20-26; S5 CLAUDE.md C3], else
  C3's OWN key — the same room and S5's `captured_at` equal to AsIsPath.capturedAt(createdAt, the phone's zone), byte for
  byte what the record sends — for a `legacy-…` row of 0006 or one not renamed yet, which carries neither name
  (asis.js:1425-1426: C3 renames the NEWEST twin of the room, so that one is the face) [the K3 critic, finding 8].
  Matched, S5's copy is NOT listed beside it; the item remembers S5's name for the capture and S5's `at` so its front
  face can be fetched.
- **A capture only S5 holds** (taken on another phone, or one whose row this phone deleted) is S5's, filed as S5 files
  it (`captures[].work`).
- **A capture only the phone holds** (not sent yet, a single photo, a project S5 does not list) is the phone's alone.
- **S5's album photos** (`photos[]`) are themselves; K3 has none of its own (K5's progress shot is the first).
So chapter 0 shows the as-is captures S5 files there, the phone's own laid over them by device id; a work chapter shows
the captures filed to it and the album photos of it — and the room screen, grouping by the same rule, agrees.

**THE CONTROLS THAT WRITE, and what K3 draws instead**

| The prototype's control (who sees it) | K3 | Who adds it |
|---|---|---|
| `Viewing as <role> ▾` and its role sheet [P 7406, 7540] — a prototype TEST switch ("the Kotlin port reads it from whoami" [P 7086-7087]) | the row stays, `Viewing as <role>` (`album.viewing_as_x`, the role through Album.roleLabel), read-only: no ▾, no tap, S5's role from the mirror; for a project S5 does not list `Viewing as Project Manager` (S5's albumRole with no project row, finding 11); absent only for a listed project whose mirror has not been read | nobody: the phone never switches role |
| the state chip as `<state> ▾`, opening the state sheet and its Done reason (the PM, chapter unlocked) [P 7459, 7543-7548] | the chip as the prototype draws it for everyone else: the state in its colours, no ▾, no tap [P 7460] | K4 |
| `Accept` / `Not for client` under a pending vendor card and in the viewer (the PM) [P 7434-7437, 7563-7564] | absent; the card keeps its `awaiting review` badge and amber ring, the viewer its review line | K4 |
| `Raise change request` and its sheet; `Record the client's answer` and its sheet (the PM, at Client approved) [P 7465-7469, 7520-7539] | absent; the live request's chip and note, and the decided list, are drawn | K4 |
| the result line (`Progress shot filed at …`, `Uploaded to …`, `Accepted · …`, `Kept from the client`) [P 7413-7416] | absent (no write, no result) | K4, K5 |
| `Progress shot` FAB, its camera, Which work? and Which room? (staff) [P 7483, 7491-7518] | absent; the body keeps its 84 dp end padding [P 1470 .alb59body], so the FAB lands with nothing reflowing | K5 |
| `Upload a photo` and its note (a vendor, in its own works) [P 7463-7464] | absent (the phone has no vendor sign-in) | K6 |
| recording the approved scope | not an album control: QV's quote view and ScopeSheet [Ph ScopeSheet.kt] | — |

None is drawn disabled: the prototype has no disabled form of any of them — it draws each live for the person it is
for and not at all for anyone else — and a greyed control with no words would read as a broken one [C]. Each sits in a
slot of its composable (the chapter row's chip, the card's review row, the viewer's review block, the header's result
line, the screen's FAB) that K3 leaves empty.

**Files and changes**

**pano/…/AlbumPage.kt (NEW) — the album page's model, pure**, as QuoteView.kt is the quote view's and Phases.kt the
strip's. Every text it returns is a `tx.<key>`, a work, state, quote stage or room through Masters, a date through
Album.dayOf / whenOf; names, captions, notes and remarks are data.
- `Local(deviceId, serverName, work, stage, createdAt, capturedAt, room, kind)` — one CaptureStore row as the album
  reads it, so :pano needs no Android type (`capturedAt` is AsIsPath.capturedAt on the phone's zone, C3's key); `Me(user,
  name)` — whoami's `user` and `full_name` (S5Session.who).
- `compose(mirror: Album.View?, local: List<Local>, me: Me): List<Album.Photo>` — THE ONE FILING above: the phone's
  rows (id = deviceId, work = Album.workOfCapture(work, stage), `at` = the shutter instant `Instant.ofEpochMilli
  (createdAt)` — the instant FW2's WORK line reads [Ph MainActivity.kt:1887] — kind `360` or `photo`, `source` PHONE,
  `capture` = S5's name for it when matched, `uploadedAt` S5's `at`; WHO: CaptureStore keeps no "taken by"
  [Ph CaptureStore.kt:91-126], so a capture S5 holds is S5's uploader (`by` = the record's updated_by, named `me.name`
  when it is this login by Album.partyKey) and only a capture not on S5 yet is the person signed in now (`me.user`,
  `me.name`) [P 7229: `BY[id] || ME`; the K3 critic, finding 7]), S5's captures not matched (source CAPTURE; byName =
  `me.name` when S5's `by` is this login, else blank, so the chip shows S5's login), S5's photos (source ALBUM); in time
  order by instant, then by key [P 7234]. An author nobody knows (no S5 session: a legacy ERP key, or signed out) is
  blank, which the card shows as `Staff` (`album.staff`), never an empty pill. **[Superseded by the K3 review, finding 2: the pill is empty, as the
  prototype's `esc(it.who||'')`; `album.staff` stays its TalkBack label. And finding 3: a matched capture by another
  login keeps S5's `by_name` when S5 sends one.]**
- `key(p)` = source, room and id — so two `legacy-…` captures of two rooms uploaded in one second (S5's 0006 ids
  collide across rooms [S5 CLAUDE.md, S5-R]) are two items, two cache files and two places in the walk.
- `viewer(mirror, notOnS5)` — Album.viewerOf(mirror) when held; the PM for a project S5 does not list (D3 with no
  project row, finding 11); else DESIGNER for visibility (above). `roleKnown(mirror, notOnS5)` says whether `Viewing as`
  is drawn.
- `statesFor(line, cache)` — the empty map for a project S5 does not list, else `cache?.states` (null = not read).
- `page(tx, title, items, mirror, cache, line, toggled, logOpen): Page` — everything the screen draws, built once: the
  header lines, the chapters from Album.chapters, the walk order (by key), each item's card and quote group, the title
  and sub-line; the viewer is derived inside (`viewer(mirror, notOnS5(line))`), and this visit's taps come in as
  `toggled` (n -> open) and `logOpen`, held by MainActivity so the search overlay does not lose them. Its parts:
  - **header** [P 7405-7423]: `viewingAs` (above); `open` — `Open: <works In progress, joined by · >`
    (`album.open_x`), `No work open yet` (`album.no_open_work`), or `Open: not read yet` (`album.open_x(works =
    tx.wish_not_read_yet)`, the one text: the test name `Open · not read yet` was wrong, finding 10) when the states are not
    read **[Superseded by the K3 review, finding 2: `No work open yet`]**; `toReview` — `To review: N` (`album.to_review_x`) for the PM
    when N > 0, N = Album.pending over the items, the screen deriving its own count as Album.View's doc says rather
    than S5's `to_review` [Ph Album.kt:361-363]; `changeRequests` — `Change requests: N` (`album.change_requests_x`)
    when N > 0, Chapters.changeRequests [P 7410-7411]; `line` — the project line's text; `log` — the toggle `Work log
    (N)` (`album.work_log_x`), N the `state` rows of the mirror's log [P 7404: stateLog — states only], newest first by
    seq, each row `<work> → <state>` / `<when, IST> · <by_name, else by>` / the remark when there is one [P 7419-7421],
    empty `No work state recorded yet` (`album.no_work_log`); with no mirror for a project S5 lists, `Work log · not
    read yet` (`album.work_log_unread`, NEW), not a toggle — the log is unknown, and `Work log (0)` would say there is
    none [C] **[Superseded by the K3 review, finding 2: always the toggle `Work log (N)`, as the prototype; the key is out of the tables]**; for a
    project S5 does not list, `Work log (0)` and `No work state recorded yet`, which is true;
  - **more** — `Showing the newest N photos` (`album.newest_x`, NEW) above the chapters when the mirror says `more`
    [S5 asis.js:236 ALBUM_PHOTO_CAP = 1000, 779; C: the counts below are of what the phone holds, and a silent cap would make
    every count confidently short] **[Superseded by the K3 review, finding 2: no line until Amit approves one on the prototype; the key is out of the
    tables]**;
  - **empty** — `No photos yet — the survey's 360s come first` (`album.empty`) when nothing is visible [P 7476];
  - **chapter 0** [P 7443-7451, 7438-7442]: the header `0`, `Quote journey`, and `n photos` (`noun.photo`'s
    pl_photo), `no photos yet` (`album.no_photos_yet`) or — locked, a vendor only — `not your work`
    (`album.not_your_work`); open: `the as-is survey, before any work` (`album.quote_journey_sub`), then the six quote
    groups, each `<stage>` (Masters.quote), a `now` pill on the current one (`album.now`), `since <d MMM>` (`app.since_x`
    with Album.dayOf of the store's `at`) beside the current one's name — ONLY when that `at` reads a day and the copy is
    not `atUnknown`, as the prototype draws it only with `quoteSince` [P 7440]: never a bare `since ` for a stage S5
    never timed (Survey since creation) or one an answer NAMED without its stamp [the K3 critic, finding 6] — and `n
    photos`; an empty group `<stage>` and
    `not reached yet` (`album.not_reached`) when it is after the current stage, else `no photos` (`album.no_photos`);
    then, for staff, `not filed to a work` (`album.unfiled`) and its count and grid [P 7447-7449];
  - **the lock banner** `Work starts once the client approves the estimate` (`album.exec_lock_banner`) before chapter 1
    while the works are locked [P 7477];
  - **chapters 1–16** [P 7452-7474]: `n`, the work (Masters.work), the line under it — locked: `locked until the
    estimate is approved` (`album.locked_until_approved`, NEW: the prototype draws it with no key [P 7453], so it is
    English under मराठी there) or, a vendor's other works, `not your work`; else `n photos · last photo <d MMM>`
    (pl_photo and `album.last_photo_x`) or `no photos yet`; the live change request's chip, its kind
    (`crkind.add|remove|change`); the state chip (Masters.state; `not read yet` with no states **[Superseded by the K3 review, finding 2: Not started]**); open: `Payment: <milestones
    joined by · >` (`album.payment_x`, Album.payOf), the live request's note `Change request · raised <d MMM> by` +
    `<by_name, else by>` + ` · <note>` (`album.cr_raised_x`), the decided list (Album.decidedText, newest first), then
    the grid or `no photos yet`;
  - **the end** [P 7478]: `The album closes when Cleaning & handover is Done` (`album.closes_cleaning`), or `Album closed
    · Cleaning & handover is Done` (`album.closed_cleaning`) with ` · <d MMM>` of its state row when 140 is Done;
  - **the defaults open** [P 7375-7380 model()]: a chapter opens by itself when it is not locked and holds a photo, or
    is now (chapter 0: a group is now; a work: In progress), or is chapter 0 of a project S5 does not list; a tap on an
    unlocked chapter's header toggles it for the visit; a locked one takes no tap;
  - **the walk** — Chapters.order: the quote groups in order, the unfiled, then the works [P 7265]; the viewer's ‹ ›
    walks it and `Photo i of N` counts it.
- `card(tx, viewer, p): CardLine` [P 7428-7433] (named so nothing clashes with material3's Card, which MainActivity
  imports, finding 12): who (the vendor's `vendor` for a vendor photo, else by_name, else by; a
  staff or a vendor glyph, TalkBack `Staff` / `Vendor` — `album.staff`, `common.vendor`); the badge, never to a client
  — `awaiting review` (`album.awaiting_review`) or `Not for client` (`album.not_for_client`); the amber ring on a pending
  one; when — Album.whenOf, the day alone for a capture (PHONE or CAPTURE) as the prototype's `dateOnly` [P 7229], day
  and time for an album photo; the work (Album.workName: `Quote journey`, `not filed to a work`, a work); the room
  through Masters.room, else `Whole project` (`common.whole_project`); the `360` corner tag for a 360. No Before /
  After tag: S5's photo row has no `pairOf` [S5 asis.js:752 photoView], so the prototype's pair is its seed's alone.
- `shown(tx, page, key): Shown?` — the viewer [P 7550-7572] (null when the walk no longer holds the key: it closes, as
  viewHTML does; the page holds each item's quote group, so no mirror or cache is passed): the title (the work's name), `Photo i of N`
  (`album.photo_x_of_x`), who, when, the room, the chapter line — `Quote journey · <stage>` (Masters.quote of the same
  Album.quoteAt that grouped it, so the viewer names the group the photo sits in) or the work's name; a live change
  request on its work `Change request · <kind>` (`album.change_request`); `Payment: …` for a work; the caption, else
  `No caption` (`album.no_caption`); not to a client, the review line — `awaiting review`, `Accepted by <name> · <when>`
  (`album.accepted_by`), or `Not for client · <name> · <when>`; the keys before and after it in the walk.
- `pic(p, localFile: Boolean): Pic` — `Pic.OnPhone(deviceId)` when the phone's file is there; else `Pic.Face(room,
  capture, version)` for a capture S5 holds (version = S5's `at`, finding 2); `Pic.AlbumJpeg(id)` for an album photo;
  `Pic.None` (named so nothing clashes with Compose's Image or with Album.Photo in a `when`, finding 12). And the addresses: `photoImage(project, id)` =
  `[project, "album", "photos", id, "img"]` (AlbumQueue.requestOf's own segments [Ph AlbumQueue.kt:87]); the face is
  AsIsPath.image(project, room, capture, `FACE` = "front") [Ph AsIsPath.kt:54]; each segment encoded once by the caller.
- `tile(tx, title, mirror, line, items)` — the count over canSee for `viewer(mirror, notOnS5(line))`, `Album`, and the
  TalkBack label.

**pano/…/Album.kt (CHANGED)**
- `Photo` gains `source: Source = Source.ALBUM` (`enum class Source { ALBUM, CAPTURE, PHONE }`) and `capture: String? =
  null` (S5's name for a PHONE item it also holds). Neither is S5's field and neither is written to the mirror
  (AlbumWire.stored writes `captures[]` and `photos[]`, which read back as CAPTURE and ALBUM).
- `chapters(…)` takes `states: Map<Int, WorkState>?` (null = not read: every Chapter.state null, `open` empty, `closed`
  false, the lock decided by the stage alone) and `notOnS5: Boolean = false`. A project S5 does not list LOCKS its works
  as at Survey — `xlock` true, so the banner before chapter 1, every work `locked until the estimate is approved` and no
  tap: the prototype creates such a project at `quote: 'Survey'` (index61c2.html 7958; the demo row 7674), so its
  execMode (4013) is false and chapters()' `xlock = !(exec(p) || stateLog(p).length > 0)` (7254) is true [the K3 critic,
  finding 1]; its quote-journey items all sit at Survey (the prototype's quoteAt, 7249). `QuoteGroup.reached` becomes `Boolean?`:
  null — and no group `now` — for a project S5 does not list [P 7255-7256, v61c.2: "no quote group is now or not
  reached yet"] AND for a stage the phone has not read (blank or outside the six) [C: the prototype's `cur < 0 ? i === 0`
  marks every later group `not reached yet`, which the phone cannot know for a stage it has not read — PL's rule again].
  `Chapter.state` becomes `String?`. **[Superseded by the K3 review, finding 2: an unread stage is the prototype's `cur < 0 ? i === 0` again (`reached`
  null only for a project S5 does not list), and `states` and `Chapter.state` are non-null again.]**

**pano/…/AlbumWire.kt (CHANGED)** — `capture()` sets `source = Album.Source.CAPTURE` and keeps S5's `at` as
`uploadedAt`; `captureRow` writes it back as `at`, so the stored mirror reads back the same (finding 2).

**app/…/AlbumScreen.kt (NEW)** — draws AlbumPage's Page; no rule of its own.
- `AlbumScreen(project, page, localPath, view, onView, onToggle, onLog, onBack, onSearch)` (the visit's taps are
  MainActivity's) and its private parts: the header (fixed above the list in
  portrait, as `.alb59h` is `flex: none` [P 1454]), the chapter list (one LazyColumn: more / empty, chapter 0, the
  banner, chapters 1–16, the end, 84 dp end padding), the photo grid (2 columns, 3 in landscape [P 1486-1487], drawn as
  rows of cards inside that LazyColumn, since a lazy grid cannot nest in it), the card, the viewer, and
  `AlbumTile(args)` for the rooms grid.
- **The prototype's sizes at 360 × 780 dp and 780 × 360 dp, in Compose** (M3 baseline roles for the prototype's
  tokens: `--pri` primary, `--pc` primaryContainer, `--tc` tertiaryContainer, `--sv` surfaceVariant, `--sc0`
  surfaceContainer, `--scl` surfaceContainerLow, `--sch` surfaceContainerHigh, `--ol` outline, `--olv` outlineVariant;
  `.ls` labelSmall, `.ll` labelLarge, `.ts` titleSmall, `.bs` bodySmall, `.bm` bodyMedium):
  - header [P 1454-1468, 1544-1548, 1414]: Viewing as, min 40 dp, 16 dp sides, surfaceContainer; Open, primaryContainer,
    10/16 dp, titleSmall; To review, #5C3A00 on #FFE3B0, 6/16 dp; Change requests, #5C3A00 on
    #FDF0D5, 6/16 dp; the project line, 6/16 dp, onSurfaceVariant on surfaceContainer; Work log, min 40 dp, primary
    labelLarge, a 16 dp ▾ / ▴; its list 16 dp sides, at most 168 dp tall and scrolling (no cap in landscape), each row
    6 dp above and below under a 1 dp outlineVariant rule;
  - chapter [P 1472-1485, 1549-1561]: a 1 dp outlineVariant rule above; the header row min 48 dp (chapter 0) and 56 dp
    (a work), 12 dp sides, 10 dp gaps; the number a 24 dp circle, 12 sp medium, surfaceVariant (primary when now); the
    ▾ / ▴ caret (`.alb59car`, 16 dp wide, onSurfaceVariant; ▴ when open, ▾ otherwise, a locked chapter included) as the
    tap target's last child — after the count in chapter 0, after the name and its line in a work, before the CR and state
    chips, which sit outside the tap target [P 7446, 7457, 1465; the K3 critic, finding 4];
    the CR chip and the state chip 28 dp tall, 14 dp round, 10 dp sides, 12 sp medium — the CR #5C3A00 on #FDF0D5 with
    6 dp after it; the state: Not started a 1 dp outline ring, In progress tertiaryContainer, Done primary, On hold
    #5C3A00 on #FFE3B0, Abandoned surfaceVariant struck through at .75; the name #B07A00 for On hold, at .6 for
    Abandoned, onSurfaceVariant while locked; the sub-line 16/6 dp in from 46 dp; a quote group's head 6/12/4/46 dp; an
    empty group min 28 dp from 46 dp; the now pill 8 dp round, 6 dp sides, 11 sp medium, primary; Payment 28 dp tall,
    8 dp round, surfaceContainer, 46 dp in, 12 dp right, 6 dp below; the CR note 4/16 dp, #5C3A00; the decided rows
    2/16 dp, onSurfaceVariant; `no photos yet` from 46 dp;
  - banner [P 1559]: 8/12 dp out, 10/12 dp in, 12 dp round, #5C3A00 on #FFE3B0, labelLarge; empty [P 1471]: 16 dp,
    bodyMedium, muted; the end [P 1500-1501]: min 48 dp, 8/12 dp out, a 2 dp outline rule above, centred,
    onSurfaceVariant (closed: primary text and rule);
  - grid and card [P 1486-1499, 1567-1571]: 6 dp gaps, 8 dp sides; the card 8 dp round, surfaceContainerLow, a 1 dp
    outlineVariant ring (2 dp #C98A1C pending); the photo 4:3 on surfaceContainerHigh; the 360 tag 6 dp in, 4 dp round,
    5 dp sides, 10 sp bold on black at .6; the body 6/7/8 dp, 2 dp gaps; the who chip 22 dp, 11 dp round, 6/8 dp sides,
    11 sp medium with a 12 dp glyph — staff surfaceVariant, vendor #5C3A00 on #FFE3B0 ringed 1 dp #C98A1C; the badge 8 dp
    round, 6 dp sides, 11 sp medium (Not for client on surfaceVariant);
  - landscape [P 1453-1455, 1467]: the header a 248 dp column on the left that scrolls, a 1 dp outlineVariant rule to
    its right, the chapters beside it;
  - viewer [P 1504-1522, 1573]: over the album, #0B0D0D, text #EDEFEA; the top row 6/8/6/4 dp: `Close` (`common.close`)
    then the title (titleSmall) over `Photo i of N` (#9AA29E); the photo filling the rest, at least 120 dp, FIT; the
    facts 8/14 dp, 3 dp gaps, #9AA29E but the caption #EDEFEA bodyMedium; ‹ and › at 22 sp, at least 56 dp, 4/8/10 dp,
    #5E6663 when there is none; in landscape the top row across, the photo on the left with ‹ › UNDER it, and the facts a
    300 dp column on the right spanning both rows — the prototype's later rules (1580-1583: `grid-template-columns:
    minmax(0,1fr) 300px`, the photo `grid-row:2`, the facts `grid-row:2/4`, ‹ › `grid-column:1; grid-row:3`) override
    1518-1522's 260 px with the facts over ‹ › (the K3 critic's finding 3 read 1518 alone, so it is declined; the first
    version of this line put ‹ › under the facts, which was wrong too).
- TalkBack: a chapter header `Chapter n: <name>` (`album.aria.chapter_x`) with its expanded state as BrowseScreens'
  ProjectLineBox announces it; the state chip `State of <work>: <state>` (`album.aria.state_x`); Payment
  `album.aria.payment_x`; the CR chip `album.aria.cr_kind_x`; ‹ › `album.aria.previous_photo` / `next_photo`.
- The viewer's photo is drawn FIT and does not pinch: the prototype's viewer does not [P 1509-1510], and ZoomableImage
  carries an unkeyed English hint [Ph ZoomableImage.kt:146-147] that would come with it [C].

**app/…/AlbumImages.kt (NEW)** — the JPEG cache: `fileOf(project, pic)` in cacheDir/album-img/, named by a SHA-256 of the
key (finding 9); one fetch per file at a time (a per-file Mutex, at most two in flight, a Semaphore), each cancelled with
its card; written to a temp file and renamed only when S5 answered 200 with JPEG bytes (FF D8 FF) of at most 8 MB (S5's
MAX_IMG); the folder trimmed to 256 MB by mtime; a 404 remembered for the visit (`newVisit`); `forgetAll`. And `AlbumImage(…)`: the file
through Thumbs.file at 512 px (the card, CROP) or VIEWER_TARGET (the viewer, FIT) [Ph Thumbs.kt:78, ZoomableImage.kt:44],
the plain block until it is there.

**app/…/AsIsUpload.kt (CHANGED)** — `internal suspend fun fetchBytes(url, auth, max)` beside `exchange` on the same client
[Ph AsIsUpload.kt:234, 258]: exchange reads the body as a String, which a JPEG cannot be; the call is ENQUEUED and
cancelled with the coroutine (`invokeOnCancellation { call.cancel() }`, finding 9), and the two share one header rule
(`authorized`) so no auth literal is written twice.

**app/…/AlbumSync.kt (CHANGED)** — the WHEN of its KDoc says the project open reads every listed project (K3);
`forgetAll` also calls AlbumImages.forgetAll.

**app/…/BrowseScreens.kt (CHANGED)** — RoomsScreen takes `album: AlbumTileArgs? = null` and draws `AlbumTile` as the
grid's last item.

**app/…/MainActivity.kt (CHANGED)**
- `var albumFor by remember { mutableStateOf<String?>(null) }` beside `wishFor` (:173), and its leave effect beside the
  wish list's (:334-339).
- The project-open read (:549-565): `needsMirror` becomes `listed == true` (above); the comment says why.
- Beside the mirror (:1340-1347): `me` from S5Session.who (re-read on `configured`, `prefsTick`, the sign-in sheet and the
  masters), the project's Local rows from `queue`, `albumItems = remember(mirror, local, me) { AlbumPage.compose(…) }`,
  the kept files by device id (read off the main thread), the tile's args, and
  — when the album is open — `AlbumPage.page(tx, …)` from the same `mirror`, `wishCache` and `projectLine` the project
  screen already holds.
- The album branch before the wish list's (:1424): `BackHandler { leaveAlbum() }`, then AlbumScreen, whose own
  `BackHandler(enabled = view != null)`, composed later, closes the viewer first — enabled only while a photo is open, or
  it would swallow Back once the viewer is closed and the album's never fire (finding 12) — then `return`. The visit's
  taps (`albumOpen`, `albumLog`, `albumView`) are held beside `albumFor` and go with `leaveAlbum()`.
- RoomsScreen's call (:1569-1630) passes the tile's args; `localFile` maps a device id to its `panoPath` when the file
  is there.
- The FW2 comment at :1873-1876 ("the mirror exists only from Revisions on") is corrected: it exists for every listed
  project once opened.

**pano/…/Phases.kt (CHANGED, KDoc only)** — `needsMirror` is the SYNC's and a move's predicate; since K3 the project
open reads every listed project's album.

**android/i18n** — three NEW keys **(two taken out again by the K3 review, finding 2: `album.work_log_unread` and
`album.newest_x`; 1,796 → 1,794)**, born keyed, each with the flag `phone-text` and a `where` naming its phone source
(I18nTableTest 10); Marathi by Claude (decision 6 of 28 Oct):

| key | EN | MR | why |
|---|---|---|---|
| `album.locked_until_approved` | locked until the estimate is approved | अंदाजपत्रक मंजूर होईपर्यंत लॉक | the prototype's own English [P 7453], which has no key there; worded as `app.stg_locked_quote` |
| `album.work_log_unread` | Work log · not read yet | कामांची नोंद · अजून वाचलेले नाही | the log of a listed project with no mirror; a WHOLE template, never `Work log` glued to `wish.not_read_yet` (the `app.x_shot` rule) |
| `album.newest_x` | Showing the newest {pl} | सर्वात नवीन {pl} दाखवत आहे | S5's `more`; `{pl}` is `plural(n,'photo')` as in `album.sub_x`, so the accessor takes `plN: Int` |

Every other text is a key the tables already hold (the `album.*` set that L1 landed, `crkind.*`, `noun.photo`,
`app.since_x`, `common.close`, `common.vendor`, `common.whole_project`, `wish.not_read_yet`, the Masters tables), listed
in the AlbumPage parts above. Tx, TxEn and TxMr are regenerated with `nice -n 15 gradle :pano:i18nGen --offline`, never
edited (1,793 → 1,796 keys). The ` · ` separator, the log row's `→` and the `360` tag are marked `// text:value`.
`ported.txt` gains `pano/…/AlbumPage.kt`, `app/…/AlbumScreen.kt` and `app/…/AlbumImages.kt`; `unkeyed.tsv` gains
nothing (each new literal is keyed or a value); `literal-counts.tsv` is rewritten by `:pano:i18nRatchet`. README gains
"Port 61c slice K3".

**S5 must already serve**

All on stage since 26 Oct – 2 Nov, nothing new: `GET …/album[?since=]` (albumView, with `captures[]`, `more`, the cursor),
`GET …/album/photos/:id/img`, `GET …/:room/c/:capture/img/:surface` (0006), S5-L's `work_states` on the list GET.

**Tests and checks**

- **AlbumPageTest (NEW, :pano)**, from S5Album's verbatim bodies (GET_PM, GET_LOCKED, the vendor's and the client's)
  and invented captures (PROJ-T1, MCAP-T1…, logins `pm@x`):
  - `compose files a capture the phone holds by the phone, once, matched by device id, by serverName and by C3's captured_at`
    (finding 8: the same room and the byte-identical stamp; another room, or a stamp a byte off, is another capture;
    the newest of two twins is the face);
  - `compose keeps a capture only S5 holds as S5 files it, and an unsent one as the phone's`;
  - `compose orders by instant then key, and two legacy ids in two rooms stay two items`;
  - `who is the vendor for a vendor photo, by_name for a photo, S5's uploader for a capture it holds, the person signed in for one not on S5 yet`
    (finding 7; and `Staff` with nobody signed in, never an empty pill);
  - `the header: Viewing as from the mirror, none without it, the PM for a project S5 does not list; Open, No work open yet, Open: not read yet`;
  - `To review is the PM's alone and only above zero; Change requests above zero`;
  - `Work log counts state rows only, newest first by seq; Work log · not read yet with no mirror; (0) for a project S5 does not list`;
  - `a project S5 does not list: no group now, none not reached, chapter 0 opens, every capture at Survey, and its works LOCK under the banner` (finding 1);
  - `an unread stage claims no group not reached`;
  - `the lock: the banner before chapter 1 and locked until the estimate is approved, the chapters taking no tap`;
  - `a chapter's line: n photos · last photo, no photos yet, not your work for a vendor`;
  - `states not read: every chip reads not read yet, nothing is open, the album is not closed`;
  - `the default open: a photo, now or a work In progress opens; a locked chapter never does`;
  - `the end line, closed with its day`;
  - `the viewer walks the order, names the group a quote-journey photo sits in, and draws no review line to a client`;
  - `the image rule: the phone's file, else S5's front face, else none; an album photo's own address; each segment encoded once`;
  - `the more line appears only when S5 says more, the empty line only when nothing is visible`;
  - (finding 10) `since is drawn beside the current group only when the store holds its stamp, never for a stage named without it`;
    `statesFor - the empty map for a project S5 does not list, the store's copy otherwise, null when it holds none`;
    `a card - the 360 tag, the store's kinds, the day alone for a capture and the day and time for an album photo`;
    `the tile counts what this person may see, with its TalkBack label`;
    `a chapter's line, state, change request, payment and decided requests at Client approved`;
  - `every line in Marathi (Tx.MR) for the three new keys and for each header line`.
- **AlbumTest** — the chapters cases follow the new parameters (`reached` nullable: `== true` where it was a Boolean;
  null states; notOnS5; an unread stage).
- **AlbumWireTest** — a capture reads as Source.CAPTURE with S5's `at` as `uploadedAt`, and the stored mirror (now with
  the capture's `at`) reads back the same.
- **I18nTableTest, TxRenderTest, I18nLiteralTest** — the new keys and the three new ported files.
- **The CI gate** (android-app.yml: the projection tests, then :app compiled — :app is compiled ONLY there).
- **Claude's stage check with d1-peek after Amit's walk**: `asis_album_log` and `asis_album_photos` hold exactly the rows
  they held before it — K3 writes nothing.

**What Amit sees**

- A new **Album** tile, last in every project's room grid, with the count of its photos.
- Tapping it opens **Album · <project>**: Viewing as Project Manager, Open: …, the project line, Work log (n); chapter
  0, the quote journey, open, with the survey's 360s in their quote stage; until the estimate is approved — and on a
  project never sent to S5, which has no stage of its own and reads as at Survey — the orange banner and sixteen works
  each `locked until the estimate is approved`; at Client approved each work with its state chip, its payment
  milestones, its live change request and its photos.
- A photo opens the dark viewer with ‹ ›. Nothing on the album writes anything.

Amit's walk, one step at a time: the album of a quote-journey project; then the approved project's album, a work
chapter opened, one photo walked with ‹ ›; then the same under मराठी.

**Claude [C] deviations NOT on the phone, waiting for the prototype** (the K3 review, finding 2): five album states the
approved prototype does not draw. The phone draws the prototype's rendering for each until Amit approves the state on the
prototype, so his walk shows none of them, and each needs his yes on the prototype before it comes back:
1. `Showing the newest N photos` above the chapters when S5 says `more` (a project past S5's 1000 newest photos).
2. `Work log · not read yet` with no tap, for a listed project whose album the phone has not read yet.
3. `not read yet` in the state chip and in `Open:`, for a store copy that holds no states.
4. `Staff` / `Vendor` inside an empty who pill, when nobody is known.
5. An unread stage's quote groups all `no photos`, where the prototype marks the five after Survey `not reached yet`.

**Deliberately NOT in K3**

- Every write: a work's state, Accept / Not for client, a change request raised or answered, the scope, a progress shot,
  a vendor's upload — and so no outbox, no drain, no provisional rows (K2's drain and K4's writes; K5; K6).
- The role switcher (a prototype test switch: the phone never has one).
- Vendor and client sign-in (K6).
- Before / After pairs (S5 has no `pairOf`).
- Pinch zoom in the viewer.
- Opening a capture's faces or survey prep from the album: the room screen is that road.
- Re-filing from the album: FW2's WORK line is the one place.
- A thumbnail route on S5 (an S5 change).
- An hourly read of every project's album, a pull-to-refresh, any prefetch.

**Risks**

- **Data.** A card fetches a full JPEG (up to 8 MB) the first time it is composed. On demand only, and once per photo; a
  thumbnail route would be an S5 slice.
- **A capture image can change under the cache** — and does: every survey-prep ✓ Save re-uploads the six faces squared
  from the edited bounds under the same capture name (finding 2). So the face's file is named by S5's `at` of the
  capture as well, and a re-squared face is a new file, fetched once more. What stays open: a read of the album that
  lands between that record PUT (which moves `at`) and the six faces that follow it in the same job (seconds) caches the
  old face under the new name, until the capture's next record PUT; S5 has no stamp of the faces themselves.
- **The widened read** is one more album GET per project open below Revisions. A Survey project's album is small: its
  captures and a few log rows, no photos.
- **The tile's count** counts only the phone's own captures until that project's first read lands.
- **FW2's WORK line meets a mirror at every stage now.** Its own rule already says "with a mirror the answer is
  quoteAt's" (Album.quoteTakenAt), so a quote-journey capture older than the stamp reads its stage where it read `not
  read yet` before a project reached Revisions — the answer FW2's review wanted, from the same history the album groups
  by.

**Critic notes, answered**

1. *"The album files the captures again, beside the room screen."* — No: one item per capture, by one rule — the phone's
   `Album.workOfCapture` for a capture it holds (the room screen's), S5's for one only S5 holds. They differ only until
   FW2's sweep or the next record carries the phone's work to S5, and in that window the phone shows its own.
2. *"Matching by device id misses old uploads."* — Each name the capture has had is asked: `deviceId`, then
   `serverName` (MEST-PH-…), as S5's delete rule names one capture and as FW2's re-file job does.
3. *"With no mirror the role is a guess."* — The guess is used only for visibility, where both staff roles see
   everything. No PM-only line is drawn from it, and `Viewing as` is absent until S5 has said — but for a project S5 does
   not list, where S5's own albumRole answers `pm` (finding 11).
4. *"`Not started` with no states read is a confident answer."* — So `not read yet` stands in the chip and in `Open:`.
   **[Superseded by the K3 review, finding 2: the prototype's Not started, until Amit approves `not read yet` there.]**
   The empty map is used only for a project S5 does not list, where nothing can have been recorded.
5. *"`To review` should be S5's `to_review`, which counts past the cap."* — K1b's View doc has the screen derive its own
   count, so K4's merges never leave it behind. Past the cap the `more` line says the album shows only the newest
   photos.
6. *"Grouping a capture under Survey with no mirror contradicts FW2's `not read yet` line."* — It can, but only for a
   listed project never read on this phone. Since the project open now reads every listed project, the mirror is there
   from the first open, and with a mirror both read `Album.quoteAt`. The header's `Work log · not read yet` **[taken out
   by the K3 review, finding 2: `Work log (0)`, the prototype's]** marks the
   case. A group must hold the photo somewhere, and the prototype's rule is the newest move at or before it, else
   Survey.
7. *"A stale mirror's `stages` misses a move."* — `Album.quoteAt` adds the store's current stage at its stamp as one
   more point (K1b's G2b). A capture taken after the newest move is placed rightly; only captures between two unread
   moves wait for the next read.
8. *"A never-sent project's works draw unlocked."* — CORRECTED by the K3 critic (finding 1): the first answer here read
   the prototype wrongly. Its execMode treats an UNREAD stage as execution, but a never-sent project is not unread: the
   prototype creates it at `quote: 'Survey'` (7958), so its album draws the banner and sixteen locked works. The phone
   follows: `Album.chapters(…, notOnS5 = true)` locks the works as at Survey; an unread stage of a listed project stays
   unlocked, as execMode leaves it.
9. *"Images of the last person stay after sign-out."* — `AlbumSync.forgetAll` forgets them with the mirrors.
10. *"The image cache in filesDir/album would be deleted, or would delete K5's shots."* — The cache is in cacheDir.
    filesDir/album keeps the mirrors and, from K5, its queued shots. forgetAll deletes only that folder's own files.
11. *"S5's capture face route ignores canSee."* — It checks only maySee (asis.js:1608). That does not matter for a
    staff phone. It is recorded for K6, where a vendor could read a quote-journey face; the fix is S5's.
12. *"Rotation closes the album."* — `albumFor` is a `remember`, as `wishFor` is. The loss is app-wide, recorded in QM2's
    review.
13. *"No vendor name leaks."* — The phone is staff. The chip shows `vendor` only when S5 sent it, and S5 blanks it for
    anyone else (photoView `hide`).
14. *"A thousand photos composed on the main thread."* — The mirror is parsed off it, as QV does. `compose` and
    `chapters` are linear passes over at most a thousand rows and six groups, remembered on their inputs.
    **[Corrected by the K3 review, finding 4: they were not — chapters asked quoteAt per item per group, and the
    page was remembered on the visit's taps, so every tap rebuilt it. Both are fixed below.]**
15. *"K4 will have to move things."* — Each writing control has an empty slot where the prototype draws it. The state
    chip is already where its ▾ goes, and the body's end padding is already the FAB's.

**The K3 critic (9 Nov 2026)** — twelve findings, each checked against the source before building; eleven taken, one declined:

1. Never-sent / not-listed works drawn unlocked (major) — **taken**: the prototype makes such a project at `quote: 'Survey'` (7958, 7674), so execMode (4013) is false and xlock (7254) true; `chapters(…, notOnS5 = true)` locks them.
2. The face cache is never refreshed — **taken**: a survey-prep save re-squares the faces (SurveyPrepScreen.kt:1024-1035, asis.js:1596-1598), so S5's capture `at` is kept and names the file; the risk note says what stays open.
3. The viewer's landscape column is 260 dp — **declined**: 1580 overrides 1518 with 300 px (same specificity, later); but 1581-1583 put ‹ › under the photo, which the plan had wrong, so that is corrected.
4. The chapter caret is missing — **taken**: ▾ / ▴, 16 dp, onSurfaceVariant, last in the tap target, locked chapters included (7446, 7457, 1465).
5. The offline staleness claim is false — **taken** (first option): the header draws the line's first line only (7412) and the read time is never there, so the album says no read time; a mark of its own would need the prototype first.
6. A bare `since ` — **taken**: drawn only when the store's `at` reads a day and the copy is not atUnknown (7440).
7. The who chip — **taken**: S5's uploader for a capture S5 holds, the person signed in only for one not on S5 yet, `Staff` when nobody is known.
8. Name-only matching duplicates a legacy row — **taken**: C3's own key (the room and the byte-identical `captured_at`) matches third, the newest twin being the face.
9. The image cache — **taken**, all four: hashed names, a 256 MB cap by mtime, the call cancelled with its card, a 404 remembered for the visit.
10. Missing :pano tests, and the Open text — **taken**: every case is in AlbumPageTest (23 tests with the :app wiring read as source); the text is `Open: not read yet`.
11. Viewing as on a never-sent project — **taken**: S5's albumRole answers `pm` to staff with no project row (asis.js:598), so `Viewing as Project Manager`.
12. Name clashes and the viewer's BackHandler — **taken**: `AlbumPage.Pic` and `AlbumPage.CardLine`, referenced qualified; `BackHandler(enabled = view != null)`.

Found wrong in the plan while building, and corrected above: the viewer's landscape layout (‹ › under the photo, not the
facts); `Local` needs `capturedAt` for C3; `viewer` needs `notOnS5`; `shown` takes the page alone (the page keeps each
item's quote group); `page` takes this visit's taps, which MainActivity holds so Search and Back return to the album as
it was; the S5 face's file needs S5's `at`, which the mirror had been throwing away; `fetchBytes` is a suspend call, not a
blocking one; and the duplicated auth header literals in AsIsUpload would have raised its literal ratchet (5 → 8), so
`exchange` and `fetchBytes` share one `authorized` rule.

**The K3 review (9 Nov 2026)** — six findings on 9b9f12a, each independently confirmed and all minor; all six fixed:

1. *Every open work had 8 dp under its grid that the prototype does not have* — **fixed**: the `wp` spacer after a
   work's grid is gone (`.alb59chb` has no rule and `.alb59grid` pads only its sides, 1486, so the last card meets the
   next chapter's rule); `gp` (a quote group, `.alb59sg` 1479), `up` (the unfiled) and `wn` (`.alb60none`, 1569) keep
   their 8 dp. Pinned by AlbumPageTest reading AlbumScreen.kt.
2. *Album states not on the approved prototype reached batch-next* — **fixed by dropping them back to the prototype's
   rendering** (the finding's second option) [C: an S2 UI change reaches batch-next only after Amit approves it on the
   prototype (11 Oct 2026), and a gate written only in this file before the Play build would be a hazard written down,
   not handled]: (a) no line for S5's `more` (`Page.more` gone); (b) the Work log is always the toggle `Work log (N)`
   (`Log.toggleable` gone), so a listed project not read yet shows `Work log (0)` and, open, `No work state recorded
   yet`; (c) a store copy holding no states reads Not started in every chip and `No work open yet` (`statesFor` is never
   null, and `Album.chapters` takes non-null states and `Chapter.state` is a String again, as before K3); (d) an unknown
   author is an empty who pill, the prototype's `esc(it.who||'')`, its TalkBack still Staff / Vendor; (e) an unread
   stage's groups are the prototype's `cur < 0 ? i === 0` again (Survey reached, the five after it `not reached yet`,
   none now; `reached` null only for a project S5 does not list). `album.work_log_unread` and `album.newest_x` leave
   the tables (1,796 → 1,794, Tx regenerated with `:pano:i18nGen`); each comes back born keyed with Amit's yes. The five
   are listed under Amit's walk above as Claude [C] deviations waiting for the prototype. What the drop-back costs,
   recorded rather than hidden: for a copy with no states the header's project line (QuoteStage.projectLine, the project
   screen's own) still says `Execution · not read yet` while the chips say Not started — the window between an app
   update and the next list read, or offline; past S5's 1000 newest photos the counts are short without saying so; a
   listed project opened offline before its first album read shows `Work log (0)`. Pinned: AlbumPageTest (the header,
   the log, the chips, statesFor, the unread stage, `more` changing nothing on the page, the empty pill, the Marathi) and
   AlbumTest (the unread stage's `reached`).
3. *The who pill on an S5 capture by another login shows the raw ERP login* — **the phone's half fixed, S5's half
   recorded**: AlbumWire.capture reads `by_name` when S5 sends it, captureRow stores it in the mirror, and compose keeps
   S5's name for a matched capture by another login, which K3 blanked. S5's capturesOf sends `{room, capture,
   captured_at, stage, work, by, at}` (asis.js 645-647) and asis_rooms keeps only `updated_by`, so until an S5 slice
   stores the uploader's `full_name` with each room record and sends it as `by_name` on `captures[]`, the pill on a
   capture by another login (or by this phone under an earlier login) shows that login where the prototype shows a
   name. Staff only, so nothing leaks. The S5 slice is in mcft-interior-estimate and is not built here. Pinned:
   AlbumWireTest (read, stored and read back; blank from today's S5 body) and AlbumPageTest (kept for a matched capture
   and for an S5-only one).
4. *Every chapter or Work log tap rebuilt the whole page on the main thread, and the quote grouping scaled with captures
   × groups × stage moves* — **fixed**: `AlbumPage.model(…)` (the page with no tap, and the Work log's rows) is
   remembered in MainActivity on the data alone, and `AlbumPage.page(model, toggled, logOpen)` only sets each chapter's
   `open` and the log's rows on it, sharing the model's cards, items, walk and groups; `Album.QuoteHistory` reads,
   parses and sorts the stage rows ONCE, and `Album.chapters` places each quote-journey item once, as the prototype's
   chapters() does (7262) — `quoteAt` delegates to it with the same answer. Critic note 14's "linear passes" is
   corrected above. Pinned: AlbumPageTest (the tapped page holds the model's own objects and equals the whole build for
   the same taps; a locked chapter still takes no tap) and AlbumTest (a counting stage list: 10 reads for 10 rows and 60
   items, where K3's grouping read 3,600; every item where quoteAt alone puts it). Not done: the model is still built in
   composition when a mirror read lands — once per read, not per tap; building it off the main thread would need a frame
   with no page, which the prototype has no drawing for.
5. *Search then Back did not return to the album as it was* — **fixed**: the mirror (`heldMirror`), the kept files of
   the phone's captures (`heldFiles`) and the chapters' `LazyListState` (`albumList`) are held in AppScreenBody ABOVE the
   search overlay's early return; the mirror and the file map are each tagged with the key they were read under, so
   another project's never draws (the same reset `remember(stageKey)` and `remember(proj?.key)` gave), and the list state
   is new for each visit (the tile's tap and leaveAlbum). AlbumScreen takes `listState`. Not held: the landscape header
   column's and the portrait Work log list's own scroll (`rememberScrollState` inside AlbumScreen), which still reset
   after a Search. Pinned by AlbumPageTest reading MainActivity.kt and AlbumScreen.kt.
6. *The image cache never trimmed orphaned `.part` files, and a sign-out deleted the whole cache on the main thread* —
   **fixed**: `AlbumPage.trimmed` is the one trim rule, pure in :pano — a `.part` older than a minute goes (an orphan: a
   temp file lives only between its write and its rename, inside its per-file lock), a younger one is neither deleted
   nor counted, then over 256 MB the least recently drawn files until under 192 MB; a write that throws deletes its temp
   file (PlanExport.keep's pattern); `forgetAll` clears the 404s at once and deletes the files on a Thread of its own, as
   signOut sends its logout. No generation counter: a download landing after the forget keeps bytes S5 serves to any
   staff member alike (the phone signs in staff only). Pinned: AlbumPageTest (the trim's cases; AlbumImages.kt read for
   the rule, the catch and the Thread).

:pano tests after the review, from the XML under pano/build/test-results/test: 824 tests, 0 failures, 0 errors, 0 skipped
(820 before: AlbumPageTest 23 → 26, AlbumTest 66 → 67). :app is compiled only by CI; nothing has been seen on a phone.

### 19. K4 · The Project Manager's writes, through the outbox

**Size:** L · **Depends on:** K3, AQ *(both on main at 0bcfc54)*

*Derived on 9 Nov 2026, as §18 was: the §19 the lost plan held is gone. Every decision below names its source:
**[P n]** the approved prototype v61c.2 (index61c2.html, line n: the core window.ALB60 7070–7338, its writers
7220–7311, the page window.__mcftAlbum60 7342–7586, its sheets 7520–7548, its viewer 7550–7572, its CSS 1404–1418 and
1446–1583, the sheet kit 7815–7829); **[S5]** S5 QUOTE as shipped on stage (mcft-interior-estimate 9a51733:
functions/_lib/asis.js, and its CLAUDE.md row "THE ALBUM STORAGE AND ROUTES ARE REAL", with "APPROVAL IS FINAL, AND A
CHANGE AFTER IT IS A CHANGE REQUEST"); **[Ph]** the phone as shipped on batch-next 0bcfc54 (file:line); **[C]** decided
by Claude here, with the reason given. §18's table "THE CONTROLS THAT WRITE" names the empty slot each K4 control goes
into; K4 fills those slots and moves nothing (its critic note 15).*

**Goal**

The Project Manager writes the album from the phone, exactly where and how the prototype draws it: a work's state
through the state sheet, with its Done reason; Accept / Not for client on a vendor's photo awaiting review, under its
card and in the viewer; a change request raised with its kind and the note the client reads; the client's answer
recorded with the day and how (Amit, 27 Oct 2026, ~10:20 IST: "PM raises it, PM records the client's yes/no, a yes
amends the scope"); and the result line a review leaves in the header. Each tap is QUEUED as exactly its S5
call (AQ's AlbumQueue / AlbumWire, unchanged on the wire), shown at once by laying the queued op over what the phone
holds, and sent by the drain (K2's, built here) whenever there is signal: S5's answer replaces the overlay, and S5 wins
every race. Nobody else writes: a Designer, a vendor, a client, a person not signed in to S5 and a project S5 does not
list see the K3 album.

**What the prototype draws, and what it does not**

THE PROTOTYPE HAS NO OUTBOX [P 7081-7082 "Nothing here talks to a server", 7211 persist()]: every ALB60 writer writes
the phone's own store at once (localStorage, through the one persist()) and the page redraws from it, so it draws NO
queued, sending, replayed, refused or offline state for an album write — a tap is written and shown, or refused at
once by returning null with nothing drawn (setState 7220, review 7279, raiseCR 7289), the one exception being the
answer sheet's `client_at` words (answerCR 7297-7299, drawn in the sheet 7535). The quote mover is the contrast: its S5
stand-in draws offline, forbidden and no-answer refusals on the strip (move, 4038; its words 4071-4076), and the
phone's QM0-QM2 draw them; the album has none of that. So, by Amit's rule (11 Oct 2026: a UI state the prototype does
not draw does not reach the phone):
- **Queued** is drawn as the prototype draws WRITTEN: the overlay (below) shows the tap at once, with no mark of its own
  [P 7220-7221: the state row is pushed, persisted and drawn in the same render].
- **Sent and answered** (written, or the `written: false` replay) is drawn from S5's row, which by then says the same.
- **Refused or won by S5** (a 409, or 400/403/413/415) is drawn as what S5 holds now — the overlay simply goes — with no
  words: the prototype has no line for it. S5's words are kept on the phone (the outbox's `dropped`, and one Log.w),
  and the line is a question for the prototype (Q2 below).
- **Offline, or S5 not answering**, is drawn as queued: the prototype's tap shows at once with no signal.

**THE CONTROLS, each in the slot §18 left** (P = the Project Manager as S5 says it, below)

| Control (who) | Where, exactly | Enabled / drawn when | Texts (key) | Tap |
|---|---|---|---|---|
| the state chip as `<state> ▾` (P) [P 7459-7460] | the work header's state chip, in place [Ph AlbumScreen.kt:496, :506 StateChip] | P and the chapter not locked (`!c.locked`: Album.exec, as K3's lock) | the state through Masters.state, then ` ▾` (aria-hidden, `// text:value`); TalkBack `album.aria.state_x` (as K3), role Button | opens the state sheet |
| the state sheet (P) [P 7543-7548] | a bottom sheet (the kit's sheet, 7829), over the album | from the chip | title `album.state_of_x`; the five states (Masters.state), the current one selected; leaving Done only: the field `album.done_reason_label` (`.alb60reason`, 4/12 dp), and after a pick with it blank `album.done_reason_need` (`.alb60need`, 2/16 dp, #C0392B); the foot `album.state_pm_only` (bodySmall, muted, 8/16 dp, 7547) | the current state closes the sheet and writes nothing (7544); leaving Done with a blank reason draws the need line and stays open; any other pick QUEUES the state and closes the sheet |
| `Accept` / `Not for client` under a card (P) [P 7434-7437] | under the card, in its grid cell (`.alb60cw` wraps card and buttons; `.alb60rb`: a column, 4 dp gaps, 4 dp above, each full width and 40 dp tall — `.btn` / `.tbtn` are 40, which `.alb60rb`'s min-height 36 does not shorten; the K4 critic, finding 7) | P and the item a vendor's photo whose review is `pending` (overlay included) | `album.accept` (filled, primary), `album.not_for_client` (text button, primary); the group's TalkBack label `life.vrev` ("Vendor review", the prototype's `aria-label`, 7436) | queues the review; sets the result line |
| `Accept` / `Not for client` in the viewer (P) [P 7563-7564] | after the review line, in the facts (`.alb60rb2`: a row, 8 dp gaps, wrapping; text buttons in #EDEFEA, 0/12 dp, 32 dp tall) [Ph AlbumScreen.kt:735] | P and a vendor's photo: `Accept` unless it is accepted, `Not for client` unless it is kept — so an accepted photo can be kept and a kept one accepted | the same two keys | queues the review; the viewer stays open on the photo and redraws its review line; sets the result line |
| `Raise change request` (P) [P 7469] | in the open work's body, after the change-request note's place and before the decided list (`.alb60crraise`: text button, 6/16 dp) [Ph AlbumScreen.kt:552-553] | P, the stage Client approved (Album.approved, the prototype's approvedQ 7286), a baseline held (the scope not null, scopeOf 7469), and no live request on the work (overlay included) | `album.cr_raise` | opens the raise sheet |
| the raise sheet (P) [P 7520-7528] | a bottom sheet | from Raise | title `album.change_request` + ` · ` + the work (Masters.work); the kinds the work allows — in the approved scope `remove` and `change`, outside it `add` (7521) — each `crkind.*` over its line `album.cr_kind_*_sub` (Album.CrKind.word / sub), the first selected; the field `album.cr_note_client`; the button `album.cr_raise` (filled, 4/16 dp) | the button QUEUES the raise with the selected kind and the note (blank allowed, as 7526) and closes the sheet |
| `Record the client's answer` (P) [P 7468] | after the live request's note (`.alb60crans`: filled, 6/16 dp) | P, Client approved, a live request on the work (overlay included) | `album.cr_answer` | opens the answer sheet |
| the answer sheet (P) [P 7529-7539] | a bottom sheet | from Record | title `album.cr_answer`; the head line (`.alb60crhd`, 0/24/8 dp) the work · `album.change_request` · the kind (`crkind.*`) · the note; the three outcomes (`album.cr_out_approved`, `…_declined`, `…_withdrawn`, Album.CrOutcome.text), approved selected; the field `album.cr_client_at`, today in IST as `YYYY-MM-DD` (C.today, 7332); its status line (role status, #B0740E) `refuse.album.client_at_day` when refused; the field `album.cr_how` over `album.cr_how_eg`; the note `album.cr_answer_note`; the button `album.cr_answer` | the button: approved or declined with a date that is not a day draws the refusal, marks the date field, stays open, and a keystroke in the date clears it (7534); else QUEUES the answer and closes the sheet |
| the result line (P) [P 7413-7416] | the header, after the project line and before the Work log toggle (`.alb59res`: 6/16 dp, bodySmall, `--scn` = secondaryContainer, `--onscn` = onSecondaryContainer) [Ph AlbumScreen.kt:218] | after this visit's last review tap, while that review is queued or taken (gone when S5 refused it: below) | `album.accepted_res` / `album.kept_res` | — |

**A sheet closes itself when its subject goes** (the K4 critic, finding 6; the prototype's ov() 7578 `!pm()`, crSheet
7520 `!approvedQ`, crAnswerSheet 7529 `!cr || !approvedQ`): `AlbumPage.sheet(tx, m, sheetFor, now)` is null — and
MainActivity then closes the open sheet — for every sheet once the person cannot write, for the raise and the answer
sheets once the stage is not Client approved, and for the answer sheet once the overlaid log holds no live request on
the work (a pass's 409 dropping a queued raise, the end-of-pass GET bringing role designer, another phone answering).
A state sheet stays while its chapter is open, as the prototype's does.

The sheets are drawn as FW2's WorkSheet draws Which work? [Ph TreeShell.kt:536-560]: ModalBottomSheet, SheetTitle
[Ph Crumbs.kt:152], one ListItem per row with the selected one on secondaryContainer, the rows scrolling (the kit's
sheet is one scrolling column, `PG` 7788: its `per` is unused), 12 dp under the last element, each opening WHOLE
(`rememberModalBottomSheetState(skipPartiallyExpanded = true)`: `.sheet3` is drawn at its content's height and never
half-open, 278 and 381; the K4 review, finding 1); the fields are
OutlinedTextField with the label floating (`field()` 7824: an outlined field, `err` its error state). There is no Cancel:
a scrim tap, a swipe or ◁ dismisses (`K.sheet`'s onDismiss, 7829) and loses what was typed, as `m.sheet=null` does.
Back closes a sheet before anything else [P 7392-7398 back(): the sheet first], which ModalBottomSheet's own back
handling already does [Ph ScopeSheet.kt's note]; no sheet opens from the viewer, so a sheet and the viewer never stack
(ov() draws at most one, 7575-7586). The `▾` on the chip and every other glyph are `// text:value`. A sheet's write
needs no busy state, unlike ScopeSheet's [Ph ScopeSheet.kt:121-129]: a queued write is on the phone the moment it is
tapped, so the sheet closes at once.

**Who writes: the Project Manager, as S5 says it** [S5 asis.js:597 albumRole; Ph Album.kt:576 viewerOf]

- The write controls are drawn when ALL of: the mirror is held and its `role` is `pm` — S5's own word, which is D3
  (every staff member is PM while the project names none) and "only the named PM" once one is named (Amit, 27 Oct
  2026, ~10:15 IST) — the project is one S5 lists, and the phone holds an S5 sign-in whose whoami `user` is known
  (`AlbumPage.Me.user`, S5Session.who [Ph MainActivity.kt:1393-1396]). `AlbumPage.canWrite(mirror, notOnS5, me)` is the
  one test, and every control above reads it.
- A Designer (`role` designer: the project names another PM), and the phone before its first mirror read (K3 draws the
  Designer for visibility then, §18), see the K3 album: the chip as drawn for everyone else, no button, no result line
  [P 7460: the plain `.alb60st` span for anyone but the PM]. A vendor and a client are K6.
- A project S5 does not list has no controls [S5: every album route answers 404 before its handler, asis.js:834
  visible()]; its works are locked anyway (§18, the K3 critic finding 1).
- A phone with only a legacy ERP key and no S5 sign-in has no controls [C]: every album write is attributed to whoami's
  user — S5 stores `by` and `by_name` from the session it is sent with, and the overlay names the person from `Me` —
  and the outbox is the LOGIN's (below), so a write with no login has nobody to belong to. That is the prototype's
  drawing for a person it offers no control to, so no new UI state. Amit's phone has signed in to S5 since 22 Oct 2026.
  **And a phone S5 signed out by a refused renewal has none either** (the K4 critic, finding 8): S5Session.relogin drops
  the password and the session but keeps `who`, so `Me.user` alone is no sign-in; `AlbumPage.Me.signedIn`
  (S5Session.signedIn) is part of `canWrite`.

**THE OVERLAY: a tap shows at once** [P 7211 persist(), 7220-7221 setState: the write and the render are one step;
Ph Album.kt:796 withQueued, :1202 provisional, the K1b design]

The page is built from what the phone holds with this login's queue for the project laid over it, every reader from
the same overlay [C, from Amit's rule of 27 Oct: one state, in one place — the queued state is that state's pending
half, never a second copy]:
- **The works' states**: `Album.withQueued(states, queue)` over the store's copy, in queue order. It feeds the state
  chips, `Open:`, the end line (a queued Done on Cleaning & handover closes the album at once, with its day), the lock
  (a state row exists) — and, through `QuoteStageSync.states` and `lineFor`, the project line on the project screen,
  the project list row, the album header and the Wish list bar [Ph QuoteStageSync.kt:122-133: "This phone's queued
  state writes lay over the states here once the album outbox exists (K4)"], FW2's Which work? state lines, and
  `Album.newCaptureWork`'s default pick (the first work In progress) [Ph MainActivity.kt:692, 2005, 2182, 2303, 2593].
  A copy that holds no states stays null for the line (`Execution · not read yet`, PL's rule) [Ph QuoteStage.kt:429].
- **The log**: `Album.mergedLog(provisional, mirror.log)` — each queued log op as `Album.provisional(op, i, role,
  me.user, me.name, op.at)` at PENDING_SEQ + its place, and S5's own row winning over it for the same id (mergedLog's
  later list wins). It feeds the live change request (a queued raise draws the chip, the note and `Record the client's
  answer` at once; a queued answer moves it to the decided list), `Change requests: N`, and the Work log (a queued state
  is its newest row, by this person, at the tap's time).
- **The scope**: `Album.scopeWithQueued(scope, scopeAt, queue)` — each queued APPROVED add or remove answer amended in,
  in queue order, through `Album.amendedScope` with the kind the answer carries in `prev` (crAnswerOp) [P 7301-7303:
  answerCR records the amended scope at once]. It feeds the chapters' in-scope test, the raise sheet's kinds, and the
  quote view's APPROVED SCOPE row and its scope sheet's view [P 4170: the quote view reads ALB60.scopeOf and scopeAt;
  Ph MainActivity.kt:1437-1439], its `since` the tap's time as the prototype's `rec.scope.at = at`.
- **The reviews**: each item with a queued review takes the newest one's state, by `me`, at the tap's time — the card's
  badge and amber ring, the viewer's `Accepted by <name> · <when>`, the PM's `To review: N` (Album.pending over the
  overlaid items, as §18 has the screen derive it), and which review buttons are drawn.
- `Op.at` (NEW, the instant of the tap, UTC ISO; never sent: S5 stamps its own) is what every provisional row shows as
  its time, so a row's time does not move between two draws; `withQueued` carries it into the WorkState (`at` was blank,
  so a queued Done on 140 closed the album without its day).

**THE OUTBOX: where it lives, whose it is** [Ph AlbumQueue.kt:8-14 "The drain that sends … is K4's, in :app, with the
queue's first writer"; Ph WishSync.kt:56-61 the phone's offline list in preferences]

- **SharedPreferences `album_outbox`**, ONE entry (`outbox`) holding every login's outboxes — `AlbumQueue.Outboxes`,
  keyed by `Album.partyKey(whoami user)` and then by S5 id — as `AlbumWire.storedOutbox`: `{v: 1, logins: [{login,
  projects: [{project, queue: [op…], dropped: [op…]}]}]}`, each op `{id, kind, work, base, value, prev, note, photo,
  room, caption, photo_kind, client_at, error, recorded, at}` with AlbumQueue's types (`base` text, `work` a number or
  null, null as JSON null) [C; the K4 critic, finding 8: the per-login map lives in this :pano shape, so AlbumQueueTest
  and AlbumWireTest pin that one login's ops are never read, listed or written for another. Lists of entries, not
  objects keyed by login: `AlbumWire.Read` has no way to list an object's members, and a reader that could not would
  read every queued op as none]. Written with `commit()` under the store lock, parsed once per write. Preferences, not a file beside the
  mirrors: the project list reads the line in composition for every row (MainActivity.kt:1497-1502), and preferences are
  an in-memory read, as the quote store is [Ph QuoteStageSync.kt:93]; their listener is what redraws the screens as the
  queue moves, as the mirror's and the wish lists' are [Ph MainActivity.kt:605-612, 628-636]. The project key is the
  S5 id the stage and the mirror are read under (WishSync.stageKey) [Ph AlbumSync.kt KEYS].
- **The login's, and it survives sign-out** [C]. A queued op is somebody's unsent work — SyncWorker's own rule: "Kept,
  not dropped: the queue is the record of what the site said" [Ph SyncWorker.kt:399-401] — so S5Session.signOut does not
  touch it (AlbumSync.forgetAll clears `album_mirror` and the `album/` files only [Ph AlbumSync.kt:191-199]); and it is
  only ever read, drawn and SENT under the login it was queued under, so the next person signed in on the phone neither
  sees it laid over the album nor sends it under their own name. The same login's next sign-in resumes it.
- **Sent only with the S5 session of that login, never the legacy ERP key** [C]: S5Auth.current falls back to the key
  [Ph S5Session.kt:208-209], and S5 would store the key's owner as `by`. With no live session the drain renews it once
  (S5Session.relogin [Ph S5Session.kt:134-151]); refused, the phone is signed out of S5 and the ops wait. **Each pass is
  pinned to the login and the sign-out epoch it started under** (the K4 critic, finding 8): `S5Session.cookieFor(login,
  epoch)` gives the live cookie only while the sign-in is still that login's since that epoch, `S5Session.relogin(asLogin,
  atEpoch)` renews only then (relogin reads the stored email and password, so a 401 inside A's pass after B signed in
  would otherwise renew as B and resend A's op as B), and the pass stops, the queue as it was, the moment either changes
  — checked before every send and every renewal.
- **`dropped` keeps the newest 20** [C], each with S5's `error` and its `at`: nothing draws them (Q2), they are what a
  future line would read, and they cap the entry.
- **The ids are minted once** (Album.mintId, at the tap) and stored with the op, so a retry — the drain's next pass,
  a resend after a lost answer, a process restart — sends the same `ALB-…`, and S5 answers `written: false` with the row
  it already holds [S5 asis.js:75-77, 864; Ph Album.kt:1069 mintId; AQ's "each op is exactly the body S5 accepted"] —
  **unless S5 refuses before it looks the id up** (the K4 critic, finding 4): the log POST answers 404 (visible) and 403
  (not PM) before albumAppend / albumEntry (asis.js 834, 836 before 858-864), the review PUT 403 before its albumEntry
  (1001 before 1017). A write S5 holds, resent after a PM was named meanwhile, is answered 403 and dropped; so after the
  end-of-pass GET `AlbumQueue.settle` takes out of `dropped` every op whose id the album's log holds as that op (the
  same kind and work, a review the same photo), and the result line is judged by what the photo shows, never by
  `dropped` (below).

**THE DRAIN (K2's): when, where, in what order** [Ph AlbumQueue.kt next / requestOf / apply; S5 asis.js:831-880]

- **One unique WorkManager job per project**, `mcft-album-<S5 id>`, CONNECTED, EXPONENTIAL backoff from 30 s, the login's
  queue read when it RUNS — WorkRefile's shape [Ph WorkRefile.kt:71-82]. A tap or a project open with a queue ENQUEUES it
  with `ExistingWorkPolicy.REPLACE`, so the newest tap's job runs at once and never waits behind a backoff (REPLACE is
  what AsIsUpload and WorkRefile use [Ph AsIsUpload.kt:88, WorkRefile.kt:81]); SyncWorker, after BOTH legs and beside
  FW2's sweep [Ph SyncWorker.kt:63-69], enqueues every project of the signed-in login's outbox with REPLACE **unless its
  unique work is RUNNING** (`getWorkInfosForUniqueWorkFlow(…).first()`) [C; the K4 critic, finding 3: KEEP does nothing
  to a job ENQUEUED in backoff, so a drain backed off to WorkManager's 5 h cap after an S5 outage would not have been
  brought forward; a reboot needs no catch-up, WorkManager persists enqueued work]. So the catch-up brings a backed-off
  job forward, starts one for a queue whose job ended, and never cancels a pass in flight. **How a pass ends**: the
  queue empty, or a head this build does not send (K5's photo op, `Pass.held`), or a 404 STOP (S5 does not list the
  project, or lacks the route) — SUCCESS, the next project open and the hourly catch-up ask again, which makes the
  404 case exactly once an hour; any other STOP — RETRY, WorkManager's backoff; no usable session — FAILURE when the
  phone is not signed in to S5 (the next sign-in's project open, or the hourly sync, enqueues it again, as
  WorkRefileWorker returns failure()), RETRY when it is signed in and the renewal could not reach the web; the sign-in
  changed during the pass — SUCCESS, the queue waiting for its own login. Never counted in `failures`. Not Wi-Fi-only: a
  log POST or a review is a few hundred bytes; the Wi-Fi-only toggle is for 20 MB panos [Ph SyncWorker.kt:435-440].
  **K4's drain sends the LOG, REVIEW and REFILE steps alone** (`AlbumQueue.K4_STEPS`; the K4 critic, finding 9): a
  photo's RECORD and its JPEG are K5's, together with how a JPEG step meets the Wi-Fi-only toggle without holding the
  log ops queued behind it (skipped for the pass on a metered network, say, rather than STOPped) — nothing in K4 queues a
  photo op, so a JPEG send built here would ship unexercised. A photo op at the head (a K5 build's queue after a
  downgrade) ends the pass untouched.
- **ONE PASS PER PROJECT AT A TIME** [C]: a pass holds the project's pass lock in AlbumOutbox (in process: a REPLACE-cancelled
  pass still finishing its blocking call and the new one never interleave), and checks `isStopped` between requests.
- **The pass** (`AlbumQueue.drain`, pure, with :app's three callbacks): read the login's outbox as stored NOW; take its
  head (`AlbumQueue.next`); its request (`requestOf`; null — a kind this build cannot send — is applied as a 400, AQ's
  rule); send it; read S5's answer (`AlbumWire.reply`); COMMIT; then the next head, until the queue is empty or a verdict
  stops the pass (`Applied.goOn`). The head is re-read every step, so an op tapped while a pass runs goes in the same pass.
  - **The commit, in this order**: (1) the answer into the mirror (`AlbumSync.take`, at its own ticket after every read
    in flight [Ph AlbumSync.kt:212-229]) and its `work` row into the one store (`QuoteStageSync.write` of
    `QuoteStage.workCopy(stored, work)`: the store's copy with that work's state at S5's seq, merged by S5's order —
    mergeStates, the higher seq winning [Ph QuoteStage.kt:246-276]) — for every verdict but STOP and RELOGIN, decided
    first by `AlbumQueue.verdictOf`; (2) then, under the outbox's store lock alone,
    `AlbumQueue.apply` on the outbox AS STORED NOW — ops are found by id, so ops appended meanwhile ride behind it and are
    rebased as AQ does; its `mirror` (AQ's noRow test, a review's photo) is the pass's copy of AlbumSync's, read once per
    pass and kept by the same `Album.merged` — and the queue and `dropped` (capped) written back. Answer before removal —
    and THE SCREEN READS THEM AS ONE SNAPSHOT, the queue first and then the mirror file, in one IO read keyed on both
    ticks and assigned together (the K4 critic, finding 1): a queue read without the op is then always paired with a
    mirror read after S5's row landed, so no frame draws the overlay gone and S5's word not yet come. The mirror file is
    written whole (a temp file renamed over it), so that read never parses a half-written file into no mirror [C]. And a
    tap built in the gap — the screen's queue still holding a predecessor the stored queue no longer does — has its base
    fixed where it is stored: `AlbumOutbox.add` calls `AlbumWrite.atAdd` under the store lock, which gives it the seq of
    the newest row S5 took for THOSE predecessors on its work (a review: the `at`, S5's `review_at`, of the newest review
    S5 took of its photo), read from the mirror FILE, which (1) updated before (2) removed them — from those rows alone,
    never the file's newest, so a row another writer put there is still S5's 409 to decide [C].
    The pass lock is the outermost and only a pass takes it, and the store lock is never held across (1), so no two locks are ever taken in opposite orders. A crash
    between (1) and (2) re-sends the op: S5's replay, harmless.
  - **401** (RELOGIN): the session renewed once, the same op sent again; a second 401, or a refused renewal, ends the pass
    with the queue as it was [Ph AlbumSync.kt:129-136, the same rule].
  - **STOP** (404 on a log op, 405, 5xx, no network, a 200 that is not S5's handler [Ph AlbumQueue.kt:183-187], and S5's
    scope race on a recorded answer, below): the pass ends, the queue as it was, and the job ends as "How a pass ends"
    says. A timeout once the request may have gone out is STOP too: unlike QuoteMove's no-answer re-read
    [Ph QuoteStageSync.kt:251-255], resending the same id is safe here, because S5 replays it [S5 asis.js:75-77].
  - **After a pass that took any answer**, `AlbumSync.refresh(project)`: one album GET, which writes the mirror and the
    store in S5's own order — "K2 GETs after a drain" [Ph Album.kt:1334-1344, afterPost's note] — and brings S5's current role
    (a PM named meanwhile turns this phone's controls off).
- **The order writes keep** [S5 asis.js:1793-1809 albumAppend: `base` = the work's MAX(seq) of ANY kind, `prev` = its
  state now]: FIFO per project, as tapped; ops on one work chain — each one's `base` is the mirror's (baseOf, PENDING
  rows never counted) at the tap, its `prev` the OVERLAID state, and the answer to the one before rebases it (AQ's
  rebased: the entry's seq; a review's `review_at`); a 409 on a work drops that op and the work's later LOG and photo
  ops — S5 wins — while every other work's still go, **and never a review** (the K4 critic, finding 2: S5's review is
  guarded on its photo's `review_at` alone, albumReview's `party = 'vendor' AND bytes > 0 AND COALESCE(review_at,'') =
  base AND review <> state`, and never reads the work's seq); a review's 409 drops that review and the LATER REVIEWS OF
  THE SAME PHOTO alone — never another photo's, and never the work's log ops, each of which S5 answers on its own base
  (after `Reviewed meanwhile` they meet their own 409; after `No image yet` or a reused id nothing moved and they go); a
  refusal for good drops that op alone and the next on its work takes back S5's base and state
  [Ph Album.kt:1390-1443]. A review's log row moves its photo's work's base, so a state tapped after a review on the same
  work is rebased on the review's entry (AQ pins it).

**S5's answers, and what the phone then shows** [S5 asis.js, at 9a51733; AQ's verdict table]

| Write | S5's answer | Verdict | The mirror, the store and the queue | What the album shows |
|---|---|---|---|---|
| state, raise, answer: `POST …/album/log {v:1, id, kind, work, value, prev?, note, base, client_at?, via:"phone"}` | 200 `{ok, written: true, entry, work, scope, scope_at}` (864) | POSTED | entry into the log, the work row into the mirror and the store, the scope (an approved add or remove has just moved it: asis.js:846-855, one batch with the cr_clear row, guarded on the old `scope_at`); the op leaves, the next on its work rebased | S5's row: the same as the overlay |
| | 200 `written: false`, the row as stored (864) | REPLAY | as POSTED | the same |
| | 409 `{error: EXEC_LOCK_BANNER, locked: true, work, entry, scope, scope_at}` (867) | LOCKED | the rows taken; the op and its work's later ops dropped with S5's words | S5's state; the chapters lock once the GET brings the stage (the banner) |
| | 409 `Changed meanwhile — <state> by <login>; not saved` (868), `The work is <state>, not <prev>; not saved` (869), a raise's four whys (871-875), `No change request to clear on this work` (877) — each with `work`, `entry`, `scope`, `scope_at` | CONFLICT | as LOCKED | S5's state, S5's live request, S5's scope — no words (Q2) |
| | 409 on a recorded answer whose `work.seq` is still the op's base and whose work still has its live request: `The scope was changed meanwhile; not saved — it now shows what is stored` (878) or `Changed meanwhile; not saved` (879) — S5's own read-then-write race with another work's amendment (it computes the amended scope from the `scope_at` it reads in the same handler, 842-856, and guards the batch on it) | STOP (the K4 critic, finding 5) | as it was: the same id is sent again, computed afresh | queued |
| | 403 `{error: "Only the Project Manager sets a work's state"}` (836) — for a raise or an answer too, and with NO `album_role` or `pm` (unlike the scope and stage PUTs' 403s, asis.js:896, 1224) | REFUSED | that op alone dropped; the next on its work rebased; the end-of-pass GET brings S5's role | the controls go when the role is no longer PM; no words (Q2) |
| | 400 (validLogEntry's words, 657-685), 413 `Request too large` (809) | REFUSED | as 403 | — (a correctly built phone meets neither: AQ caps every text) |
| | 404 `Not found` (834: not visible) | STOP | as it was | queued |
| review: `PUT …/album/photos/<id>/review {id, state, base: review_at or "", via:"phone"}` | 200 `{ok, written: true, id, photo, review, review_by, review_by_name, review_at, entry}` (1018) | POSTED | the review patched onto the photo, its entry into the log (it moves the work's base) | S5's review |
| | 200 `written: false` (1020: the same state again, or this id's own row) | REPLAY | as POSTED | the same |
| | 409 `Reviewed meanwhile — <review> by <login>; not saved` (1021), `No image yet…` (1010), `That id already names another album entry…` (1019), each with the review as stored | CONFLICT | the review taken; the op and its work's later ops dropped | S5's review; the result line goes |
| | 403 `Only the Project Manager accepts a vendor photo` (1001, no role); 400 `Only a vendor's photo is reviewed…` (1007) | REFUSED | that op alone | S5's review; the result line goes |
| | 404 `Not found` for a photo the mirror does not hold | REFUSED (AQ's noRow) | that op alone | — |
| any | 401 | RELOGIN | renewed once, resent | queued |
| any | no network, a timeout, 5xx | STOP | as it was | queued |

**THE PM'S TAPS AS OPS** (`pano/AlbumWrite.kt`, NEW, pure) — each tap checked by K1b's gate on the OVERLAID state and
built by K1b's op builder on the MIRROR's bases [Ph Album.kt:938-1015 gates, :1077-1110 builders], so a chain of taps
offline is exactly the chain S5 will take:
- `state(m, work, to, remark, now)`: `from` = the overlaid state (sent as `prev`; S5's SQL compares it with its own,
  asis.js:1802); `Album.stateWrite(viewer, exec, work, from, to, remark)`: SAME_STATE is `Out.None` (the prototype's
  pick of the current state, 7544), DONE_REASON_NEED is the sheet's need line, anything else refused is `Out.None` (the
  chip is not drawn tappable then); else `Album.stateOp(mirror, …)` with `at` = now.
- `kinds(m, work)`: `remove`, `change` in the overlaid scope, else `add` (7521).
- `raise(m, work, kind, note, now)`: `Album.crRaise` on the overlaid log and scope; refused is `Out.None` and the sheet
  closes, as the prototype's raiseCR returning null closes it (7526: `m.sheet=null` whatever it returned) — reachable
  only through a race (a read landing between open and submit); else `Album.crRaiseOp`.
- `answer(m, work, outcome, clientAt, how, now)`: `Album.crAnswer`; CLIENT_AT_DAY is `Out.Refused` (the sheet's line,
  S5's 400 verbatim, keyed); **every other refusal — CR_NONE, CR_NOT_APPROVED, CR_PM_ONLY — is `Out.None`** and the
  sheet closes (the K4 critic, finding 6); else `Album.crAnswerOp(mirror, overlaidLog, …)` — `prev`, the kind it
  answers, read from the OVERLAID log, so an answer to a raise still queued carries its kind.
- `review(m, key, state, now)`: `Album.review(viewer, overlaidPhoto, state)` (so a second Accept is REVIEW_SAME, nothing
  queued), the op built from the MIRROR's photo — `base` is S5's `review_at`, never the overlay's tap time, which S5
  would answer as stale (asis.js:1012 guards on the old `review_at`); a second review of the same photo queued behind
  the first takes the first's answer's `review_at` through AQ's rebase.
- `today(now)`: the IST day, `now.atZone(QuoteStage.IST).toLocalDate().toString()` — never `LocalDate.ofInstant`, which
  is Android API 34 and :pano runs on the phone (minSdk 29).
- `Out`: `Queue(op)`, `Refused(why: Album.Refusal)`, `None` — not `Nothing`, which is Kotlin's.
- `atAdd(op, seen, stored, mirror)`: the op as the outbox stores it (the commit's paragraph above; finding 1).

**Files and changes**

**pano/…/AlbumWrite.kt (NEW)** — above.

**pano/…/AlbumPage.kt (CHANGED)**
- `model(tx, title, items, mirror, cache, line, queue: List<Album.Op> = emptyList(), me: Me = Me("", ""))`: the overlay
  above, built once per change of its inputs as K3's model is; the K3 parameters keep their order.
- `canWrite(mirror, notOnS5, me)` (above); `Model` keeps, internal, what AlbumWrite needs — the viewer, the mirror, the
  overlaid states, log, scope and items, the stage — null when the person cannot write.
- `Header.result: String?`, set by `page(m, toggled, logOpen, result: String? = null)`; `resultText(tx, m, res)`:
  `album.accepted_res` / `album.kept_res` for `Res(state, photo)` while the overlaid photo shows that review (queued, or
  S5's row), null once it shows another — never judged by `dropped`, which a refusal of an op S5 already held would put it
  in (finding 4). `Me` gains `signedIn` (finding 8).
- `WorkCh` gains `stateTap: Boolean`, `raise: String?`, `answer: String?`; `CardLine` gains `review: Boolean`
  (`card(tx, viewer, p, canWrite = false)`); `Shown` gains `accept: String?`, `keep: String?`.
- The three sheets' models (`Sheet`), every text a `tx.<key>`: `StateSheet(work, title, rows, reasonLabel, need,
  footer)`, `RaiseSheet(work, title, kinds, noteLabel, button)`, `AnswerSheet(work, title, head, outcomes, dateLabel,
  today, howLabel, help, note, button)`, from `stateSheet(tx, m, work)`, `raiseSheet(tx, m, work)`,
  `answerSheet(tx, m, work, now)` and `sheet(tx, m, sheetFor, now)`, each null when its subject has gone (finding 6);
  the need line and the date's refusal are `Album.Refusal.text(tx)` (`album.done_reason_need`,
  `refuse.album.client_at_day`).
- `SheetFor` (`State(work)`, `Raise(work)`, `Answer(work)`) and `Res` — the visit's, held by MainActivity.

**pano/…/Album.kt (CHANGED)** — `Op.at: String = ""` (last, defaulted: every caller and AQ's request unchanged; never
sent); `withQueued` carries `op.at` into the WorkState; `scopeWithQueued(scope, scopeAt, queue): Pair<List<Int>?, String>`;
`dropWork` leaves the reviews on a work and drops a review's photo's later reviews alone (finding 2).

**pano/…/AlbumQueue.kt (CHANGED)** — `Sent(code, body)` (code ≤ 0: no network) and `drain(project, read, load, send,
commit, relogin, stopped, sends = K4_STEPS): Pass(answered, stopped, code, held)`, the pass above with no Android in it;
`verdictOf(mirror, op, code, reply)` (apply's verdict, given alone for the commit's step (1), with the answer race of
finding 5); `Stored` (with `capped()`, `KEEP_DROPPED = 20`), `Outboxes` (per login, finding 8), `settle` (finding 4).

**pano/…/AlbumWire.kt (CHANGED)** — `storedOutbox(outboxes)` and `outboxOf(r, o)`: the shape above, read tolerantly as
the mirror is — a field missing takes the Op default, an op without an `id` is left out (it could never be replayed),
an op of an unknown kind is KEPT (the drain refuses it with AQ's 400, that op alone), `base` stays text, a blank login
or project is left out.

**pano/…/QuoteStage.kt (CHANGED)** — `workCopy(stored, work): Cache?` (null when not listed, when the copy holds no
states in the read shape — one work's row would read as the whole project's — or the row has no state: `at` blank, as
statesOfWorks drops it); `statesWith(cache, queue)` (null stays null, else withQueued).

**app/…/AlbumOutbox.kt (NEW)** — the store (`prefs`, `all`, `queued(context, project)`, `dropped(context, project)`,
`add(context, project, login, op, seen)` under the store lock — refused when the sign-in is no longer `login`'s, the op
through `AlbumWrite.atAdd` — the current login from `S5Session.loginKey`), `enqueue(context, project, replace)`,
`enqueueAll(context)` (suspend: REPLACE unless RUNNING), `run` (the pass: `AlbumQueue.drain` with the store, the
pinned send — `AsIsUpload.exchange` with `S5Session.cookieFor`, the JSON body as `JSONObject(request.fields)`, which keeps
a null as JSON null [Ph AlbumQueue.kt:62-69] — the commit, the pinned renewal; then the GET and `settle`), and
`AlbumDrainWorker` (CoroutineWorker, its input the project, `withContext(Dispatchers.IO)`; a session near its end
renewed first, as WorkRefileWorker does). No JPEG is sent: RECORD and IMG are K5's (finding 9). Log lines
`// text:value`, never a header, S5's words at most.

**app/…/S5Session.kt (CHANGED)** — `epoch(context)`, `loginKey(context)` (whoami's user as `Album.partyKey`), `cookieFor(
context, login, epoch)` and `relogin(context, asLogin, atEpoch)`, the defaults keeping every caller as it was (finding 8).

**app/…/AlbumSync.kt (CHANGED)** — the mirror file written whole, a temp file renamed over it (`writeFile`, finding 1);
its KDoc says the outbox exists and its answers come in through `take`.

**app/…/AlbumSheets.kt (NEW)** — `AlbumStateSheet`, `AlbumRaiseSheet`, `AlbumAnswerSheet`: draw the models; the typed
reason, note, date and how are `remember(work)` inside each, lost on dismiss as the prototype's are.

**app/…/AlbumScreen.kt (CHANGED)** — each control in its slot (the table above): StateChip clickable with ` ▾` when
`w.stateTap`; WorkBody's buttons between the note and the decided list; AlbumCard's cell a Column of the card and,
when `line.review`, the two buttons (`.alb60cw`), each the cell's width and **40 dp tall** (`.btn` / `.tbtn` height 40;
`.alb60rb`'s min-height 36 does not shorten them) — a pending card's cell is then taller, and its grid row with it, as
the prototype's grid row is, and **the row stretches every cell to its height** (`height(IntrinsicSize.Min)` on the row,
each cell `fillMaxHeight()`, a plain card filling its cell; measured on the prototype at 360 × 780 with its own CSS: the
pending wrapper 311.75 px, its card 223.75 px, the plain neighbour stretched to 311.75 px — the K4 critic, finding 7);
ViewerFacts' two buttons after the review line; AlbumHeader's result line; the sheet drawn when `sheet != null`. New parameters, after K3's: `sheet: AlbumPage.SheetFor?`, `onSheet`, `onState(work, to, remark):
AlbumWrite.Out`, `onRaise(work, kind, note)`, `onAnswer(work, outcome, clientAt, how): AlbumWrite.Out`,
`onReview(key, state)`.

**app/…/QuoteStageSync.kt (CHANGED)** — `lineFor` lays the login's queue (its KDoc's K4 sentence becomes true);
`states(context, key)`; the class note names the outbox's answers as one more caller of `write`.

**app/…/SyncWorker.kt (CHANGED)** — `AlbumOutbox.enqueueAll(applicationContext)` after `WorkRefile.sweep` (:69).

**app/…/MainActivity.kt (CHANGED)**
- The outbox's preferences listener bumps `quoteTick` and `albumTick` (every reader of the line, the states and the
  mirror is keyed on those already) [Ph MainActivity.kt:605-624].
- `albumQueue` from `heldQueue`, read WITH the mirror in one effect keyed on `stageKey, albumTick, dataTick, outboxTick,
  albumMe.user` — the queue first, then the mirror, both assigned together (finding 1; the first version read the queue
  in composition, which paired the drain's removal with a mirror read frames later); the model's remember gains it and
  `albumMe` (now with `signedIn`) [Ph :1515]; `heldQueue`, `albumSheet` and `albumRes` beside `heldMirror` and
  `albumView`, held above the search overlay and cleared by `leaveAlbum` and the tile's open [Ph :190-213, :1421-1430].
- The callbacks: `AlbumWrite.*` on the model; a `Queue` goes to `AlbumOutbox.add(context, key, albumMe.user, op,
  albumQueue)` off the main thread in `scope` [Ph :149], then `enqueue(replace = true)`; a review sets `albumRes`, and the
  page's result line is `AlbumPage.resultText(tx, model, albumRes)` — judged by what the photo shows (finding 4); the
  open sheet's model is `AlbumPage.sheet(…)`, and a LaunchedEffect closes a sheet whose model is null (finding 6).
- The project-open effect enqueues the drain when the login's queue for the project is not empty [Ph :585-603].
- The five states readers read `QuoteStageSync.states` [Ph :692, 2005, 2182, 2303, 2593]; the quote view's row and the
  scope sheet's view take `Album.scopeWithQueued` [Ph :1437-1439].

**android/i18n** — **NO NEW KEY**: every text above is one the tables hold (L1 landed the prototype's album and change
request keys; K1b's refusals), the group label is `life.vrev`, the prototype's own English for that `aria-label`, whose
RAW Marathi is व्हेंडर तपासणी. Tx is not regenerated. `ported.txt` gains `pano/…/AlbumWrite.kt`, `app/…/AlbumOutbox.kt`
and `app/…/AlbumSheets.kt`; `unkeyed.tsv` gains nothing; `literal-counts.tsv` is rewritten by `:pano:i18nRatchet`;
README gains "Port 61c slice K4". The texts, EN and MR as the tables hold them:

| key | EN | MR |
|---|---|---|
| `album.state_of_x` | State of {work} | {work} ची स्थिती |
| `album.done_reason_label` | Why change a Done work? (required) | पूर्ण झालेले काम का बदलायचे? (आवश्यक) |
| `album.done_reason_need` | A reason is needed to change a Done work | पूर्ण झालेले काम बदलण्यासाठी कारण आवश्यक आहे |
| `album.state_pm_only` | Only the Project Manager sets a work's state | कामाची स्थिती फक्त प्रोजेक्ट मॅनेजर ठरवतो |
| `album.accept` | Accept | स्वीकारा |
| `album.not_for_client` | Not for client | ग्राहकासाठी नाही |
| `life.vrev` | Vendor review | व्हेंडर तपासणी |
| `album.accepted_res` | Accepted · the client sees it now | स्वीकारला · आता ग्राहकाला दिसतो |
| `album.kept_res` | Kept from the client | ग्राहकापासून राखून ठेवला |
| `album.cr_raise` | Raise change request | बदल विनंती नोंदवा |
| `album.change_request` | Change request | बदल विनंती |
| `crkind.add` / `.remove` / `.change` | add / remove / change | समावेश / वगळणे / बदल |
| `album.cr_kind_add_sub` | a work outside the approved scope comes in | मंजूर स्कोपबाहेरचे काम स्कोपमध्ये येते |
| `album.cr_kind_remove_sub` | a work in the approved scope comes out | मंजूर स्कोपमधील काम स्कोपमधून वगळले जाते |
| `album.cr_kind_change_sub` | a work in the approved scope changes | मंजूर स्कोपमधील कामात बदल होतो |
| `album.cr_note_client` | A note the client reads | ग्राहक वाचेल अशी नोंद |
| `album.cr_answer` | Record the client's answer | ग्राहकाचे उत्तर नोंदवा |
| `album.cr_out_approved` / `_declined` / `_withdrawn` | Change approved by client / Change declined by client / Change request withdrawn | ग्राहकाने बदल मंजूर केला / ग्राहकाने बदल नाकारला / बदल विनंती मागे घेतली |
| `album.cr_client_at` | Date the client answered | ग्राहकाने उत्तर दिल्याची तारीख |
| `refuse.album.client_at_day` | client_at must be the date the client answered, as YYYY-MM-DD | ग्राहकाने उत्तर दिल्याची तारीख वर्ष-महिना-दिवस या क्रमाने हवी |
| `album.cr_how` | How the client answered | ग्राहकाने उत्तर कसे दिले |
| `album.cr_how_eg` | a call, WhatsApp, in person | फोन, व्हॉट्सॲप, प्रत्यक्ष भेट |
| `album.cr_answer_note` | If approved, an add puts the work into the approved scope, a remove takes it out and a change leaves the scope as it is; declined or withdrawn changes nothing | मंजूर झाल्यास: समावेशात काम मंजूर स्कोपमध्ये येते, वगळण्यात ते स्कोपमधून जाते आणि बदलात स्कोप तसाच राहतो; नाकारल्यास किंवा मागे घेतल्यास काहीच बदलत नाही |

The five states are Masters.state (`album.st_*`), the works Masters.work, `album.aria.state_x` K3's.

**S5 must already serve**

All on stage since 26–27 Oct, nothing new: `POST …/album/log` with the CR kinds and answers and the scope amend
(27 Oct), `PUT …/album/photos/:id/review`, and `GET …/album` for the end-of-pass read.

**Tests and checks**

- **AlbumWriteTest (NEW, :pano)**, from S5Album's verbatim bodies and invented ids (PROJ-T1, `pm@x`):
  - `a state tap is the op S5 takes - prev the overlaid state, base the mirror's, the tap's at, an ALB id`;
  - `the current state writes nothing, leaving Done needs a reason, a locked chapter offers no write`;
  - `a chain of three taps offline is the chain S5 takes - each prev the one before, every base the mirror's`;
  - `the raise kinds follow the overlaid scope - add off it, remove and change in it - and a queued approved add moves the work into it`;
  - `an answer carries the kind it answers, from a raise still queued, and a date that is not a day is refused with S5's words`;
  - `withdrawn needs no day; today is the IST day`;
  - `a review is built on the mirror's review_at, never the overlay's, and a second Accept queues nothing`;
  - `nobody but the PM writes - a Designer, no mirror, a project S5 does not list, no sign-in: every tap None`.
- **AlbumPageTest (CHANGED)**:
  - `a queued state shows at once - the chip, Open, the Work log row by this person at the tap's time, the end line closed with its day`;
  - `a queued raise draws the chip, the note and Record at once; a queued answer moves it to the decided list`;
  - `a queued review clears the badge, the ring, To review and the card's buttons, and the viewer says Accepted by this person`;
  - `S5's row wins over the provisional one for the same id`;
  - `the controls per role - the PM's chip is a tap with its caret, its card and viewer buttons, Raise and Record where the prototype draws them; the Designer, no mirror, not on S5 and no sign-in get K3's page, with no queue the same objects`;
  - `Raise only at Client approved with a baseline and no live request; Record only on a live one; neither while locked`;
  - `the viewer offers the other review state, never the one the photo has`;
  - `the result line after a review, gone when S5 dropped its op`;
  - `the three sheets - titles, rows, the selected one, the Done reason only when leaving Done, the need line, the kinds and their lines, the answer's head, outcomes, today, the refusal, the help and the note`;
  - `every sheet text in Marathi (Tx.MR)`;
  - read as source (as K3's tests read AlbumScreen.kt): each control sits in its K3 slot, nothing K3 drew moved.
- **AlbumQueueTest (CHANGED)** — the pass, with an in-memory store and a fake S5 answering S5Album's bodies:
  - `the drain sends FIFO and each op rides on the answer before it`;
  - `two works interleaved - a 409 on one drops its later ops, the other's still go, in order`;
  - `an op tapped during a pass goes in the same pass`;
  - `a 401 renews once and resends the same op; a second stops the pass with the queue as it was`;
  - `no network, a timeout and a 5xx stop the pass; the next pass replays the same ids and S5's written false lets them go`;
  - `the answer is taken before the op leaves the queue`;
  - `the GET is asked after a pass that took an answer, never after one that only stopped`;
  - `dropped keeps the newest twenty, each with S5's words`.
- **AlbumWireTest (CHANGED)** — `the outbox round-trips exactly through the stored shape`; `an op without an id is left
  out, one of an unknown kind kept for the drain's 400, a base stays text, a null stays a null`.
- **AlbumTest (CHANGED)** — `withQueued carries the tap's at`; `scopeWithQueued amends approved adds and removes in
  queue order and nothing else`.
- **QuoteStageTest (CHANGED)** — `workCopy takes one work row at S5's seq and never a not-listed copy`; `statesWith
  keeps null as null`.
- **A source-reading test** (in AlbumPageTest, as K3's): AlbumOutbox.kt sends with the S5 session of the pass's login
  (`cookieFor`) and never names S5Auth; the renewal and every send pinned; the taps REPLACE and the catch-up spares a
  RUNNING pass; the commit takes the answer before `apply`; the screen reads the queue before the mirror; the mirror
  file renamed into place; no state reader takes the store's copy bare; no `LocalDate.ofInstant` in :pano (AlbumWriteTest);
  AlbumWrite's `Out.None`.
- **The K4 critic's cases** (each pinned where it lives): AlbumWriteTest, an op built after its predecessor's answer was
  committed carries that answer's seq and `review_at` (finding 1), every answer refusal but the day is None (6);
  AlbumQueueTest, a work's 409 leaves its reviews and a review's 409 drops its photo's later reviews alone (2), the
  scope race is STOP (5), `settle` (4), a photo op held (9), every login's outbox kept apart (8); AlbumPageTest, the
  result line judged by the photo (4), a sheet whose subject has gone has no model (6); AlbumWireTest, the per-login
  stored shape (8).
- **I18nTableTest, TxRenderTest, I18nLiteralTest** — the three new ported files; check (f) resolves every `tx.` the
  :app files call.
- **The CI gate** (android-app.yml: the projection tests, then :app compiled — :app is compiled ONLY there).
- **Claude's stage check with d1-peek after Amit's walk**: the walk's rows in `asis_album_log`, one per `ALB-` id —
  none twice although a retry or an offline tap re-sent it — with `via` phone, `role` pm and the login as `by`; the
  scope's `scope_at` moved by an approved add; nothing written for a project or a work the walk did not touch.

**What Amit sees**

- On the approved project's album, as the Project Manager: every unlocked work's state chip ends in ▾; a tap opens
  `State of <work>` with the five states; a pick shows at once in the chip, in `Open:`, in the Work log, and on the
  project screen's line — with signal or without.
- An open work's body: `Raise change request` under Payment; its sheet offers the kinds the work allows and a note; the
  chip and the note appear at once, then `Record the client's answer`, whose sheet takes the outcome, the date
  (today) and how; an approved add puts the work into the APPROVED SCOPE at once.
- A vendor's photo awaiting review carries Accept and Not for client under its card and in the viewer; a tap moves it
  at once and says `Accepted · the client sees it now` or `Kept from the client` under the project line.
- Under मराठी, all of it in Marathi.
- A Designer's phone, or this phone before its first read of a project's album, shows K3's album.

Amit's walk, one step at a time, on a test project on stage approved for the walk (invented, never a client's: the
log is append-only and approval is final, so every row the walk writes stays): a state set online; a state set in
airplane mode, the phone then online; a change request raised, then answered approved; the same under मराठी. The review
half needs a vendor's photo on stage, which only K6 (or a vendor's own API call) can put there, so it is proven in
:pano and walked with K6.

**Questions for the prototype** — UI states K4 needs and the approved prototype does not draw. The phone draws the
prototype's rendering for each, as K3's review ruled for its five (§18), until Amit approves one on the prototype:
1. **A write still on the phone.** Drawn as written, with no mark. A mark (`queued`, `app.queued`, on the Work log row
   and the chip?) would tell a PM in a basement that S5 has not taken it yet.
2. **A write S5 refused or won over.** Drawn as S5's state, with no words: the chip goes back. S5's words are on the
   phone (the outbox's `dropped`); the result line's slot in amber is the natural place.
3. **A queued write by another login.** Not drawn, not sent until that login signs in again. Should the album say so?

**Deliberately NOT in K4**

- K5: the progress shot, its camera, Which work? and Which room?, its photo record and JPEG — K4's drain holds a photo
  op at the head untouched (`AlbumQueue.K4_STEPS` has no RECORD or IMG), so K5 adds the two steps to the set, the JPEG
  send (a file read from `filesDir`, `image/jpeg`) and its rule against the Wi-Fi-only toggle, and does not reshape the
  pass (the K4 critic, finding 9) — its result line `Progress shot filed at …`, and the photo re-file (S5-P) of a
  Capture 360 photo that went up by K5's route.
- K6: vendor and client sign-in on the phone, a vendor's `Upload a photo` and its note, `Uploaded to …`.
- A queue screen for album writes, a mark on a queued write, the words of a refusal (the questions above).
- Recording the approved scope and the quote move: QV's and QM1/QM2's, online and unqueued as they are.
- A price on a change request (note now, price when the estimate is linked to the project: Amit, 27 Oct 2026).
- Undoing a state: a later state is the undo, as on S5 (the log is append-only).

**Risks**

- **A refusal is silent** until Q2 is answered: a PM whose write lost a race sees the chip return to S5's state. A race
  needs a second writer (/projects, or another staff phone while no PM is named), and S5 wins it by design.
- **A queue for a project S5 stops listing** waits (its 404 is STOP, and the pass then ends SUCCESS, so no backoff
  keeps asking) and is sent if S5 lists it again; the hourly catch-up and every project open ask once meanwhile. Any
  other STOP backs off (30 s, doubling, to WorkManager's 5 h cap), and the catch-up REPLACEs a job in backoff that is not
  running, so an S5 outage costs at most the hour to the next sync once S5 answers again (the K4 critic, finding 3).
- **Ops queued under a login that never signs in again** are never sent and never drawn.
- **The date** defaults to the phone's IST day: a phone with a wrong clock offers a wrong day, which the field shows.
- **The 28 dp state chip** is the prototype's whole tap target, under the 48 dp guidance; the 56 dp header row gives
  it room, and nothing else is tappable beside it.
- **Data on stage**: every row the walk writes is permanent (append-only), hence a test project.

**Critic notes, answered**

1. *"The outbox is a second answer, which Amit's rule forbids."* — It is the pending half of S5's one answer: nothing on
   it is set by hand that S5 does not then take or refuse, its overlay goes the moment S5 answers, and it is never
   written into the store or the mirror.
2. *"Silence and success look identical — this project's most-recorded fault."* — Yes, for a refusal, by the prototype's
   own drawing, which has none; recorded as Q2 with where its line would go, the words kept on the phone, rather than a
   UI state invented on the phone.
3. *"Why queue, when the scope record and the quote move are online and unqueued?"* — S5 built these routes for replay
   (writer-minted ids, `written: false`, asis.js:75-77); the prototype's tap shows at once with no signal; and states and
   reviews are many small acts on site, where the quote move is one guarded commercial act whose lost answer the phone
   re-reads instead [Ph QuoteStageSync.kt:43-52].
4. *"A sign-out sends A's work as B's."* — The outbox is the login's, read and sent only under it.
5. *"The legacy ERP key could send it."* — The drain sends only with the session of the op's login.
6. *"Two drains race."* — One unique job per project and one pass per project at a time; SyncWorker only enqueues, KEEP.
7. *"The screen appends while the drain applies."* — Both under the outbox's lock; the drain applies to the outbox as
   stored now, ops found by id.
8. *"A crash after S5's 200 sends it twice."* — The same id: S5 replays it.
9. *"The overlay flickers back between the answer and the GET."* — The answer goes into the mirror and the store before
   the op leaves the queue, and the screen reads the queue and the mirror as ONE snapshot, the queue first (the K4
   critic, finding 1, corrected this answer: its first version read the queue in composition and the mirror off the main
   thread frames later, which paired a queue without the op with a mirror without S5's row).
10. *"The log POST's 403 names the role, as the scope's does."* — It does not (asis.js:836, 1001): the end-of-pass GET
    brings the role.
11. *"A second review's base would be the overlay's time."* — It is the mirror's `review_at`, rebased by AQ.
12. *"A raise on a work a queued answer is about to amend."* — The kinds come from the overlaid scope, as the prototype's
    from its amended scope; S5 decides on send.
13. *"K4 will have to move K3's controls."* — Each control is in the slot §18 named; a pending card's cell grows, as the
    prototype's grid row does.
14. *"The drain will need rework for K5's photos."* — It sends the steps of a set K5 widens (`K4_STEPS`), and holds a
    photo op untouched until then; K5 adds its writer, the JPEG send and its Wi-Fi rule (the K4 critic, finding 9,
    corrected this answer: the first version built the JPEG send into K4, unexercised, with no room for the Wi-Fi rule).
15. *"`Nothing` and `LocalDate.ofInstant` compile here and fail on the phone or in a `when`."* — `Out.None`, and the
    IST day through `atZone`.
16. *"A PM named on /projects while this phone holds queued writes."* — They still show until the next pass sends them:
    S5's 403 drops each (that op alone, AQ), and the pass's GET then brings `role` designer, which turns the controls
    off. Nothing is sent under a role S5 no longer gives.
17. *"S5's log POST answers ANY entry with the id as a replay, whatever its kind."* — True (asis.js:859-864: the entry
    is found by id alone; only the review route checks the kind, 1019). The ids are 48 random bits minted per op, so a
    collision within one project's log is not a practical case; recorded, not guarded.
18. *"A queue for a project S5 no longer lists asks S5 every hour for ever."* — Its log ops' 404 is STOP (AQ: a route an
    older S5 lacks, a project not visible yet), and the pass then ends SUCCESS rather than backing off, so it is asked
    once an hour by the catch-up and once per project open — one small POST each — and the queue goes the day S5 lists
    the project again. A review's 404 for a photo the mirror no longer holds is refused at once (noRow). (Corrected by
    the K4 critic, finding 3: with KEEP and a retry, the first version's answer was not what WorkManager does.)

**The K4 critic (9 Nov 2026)** — nine findings, each checked against the source before building; all nine taken, three
of them with a part declined or refined:

1. The overlay leaves before S5's row reaches the screen, and a tap in the gap is built on a stale base (major) —
   **taken**: confirmed (the queue was a composition read, the mirror an IO read frames later; AlbumSync.take writes no
   preference for a review's answer). The screen reads the queue and then the mirror as ONE snapshot, assigned together;
   the mirror file is written whole (temp + rename); AlbumOutbox.add fixes a new op's base under the store lock through
   AlbumWrite.atAdd, pinned in AlbumWriteTest. **One part declined**: the base is taken from the rows S5 took for the
   predecessors the screen saw queued and the store no longer holds, not from the mirror file's newest base — which would
   also take another writer's row and let the op overwrite a change the PM never saw, the race S5's guard exists to
   refuse.
2. A work's 409 drops a queued Accept S5 would take (minor) — **taken, one part declined**: confirmed (dropWork's
   cascade; albumReview's guard reads no seq). A log or photo op's 409 never drops a review; a review's 409 drops the
   later reviews of the same photo alone. Declined: dropping the work's later log ops on a review's 409 — `No image yet`
   and a reused id move no seq, so S5 would take them, and after `Reviewed meanwhile` they meet their own 409. Pinned in
   AlbumQueueTest.
3. KEEP cannot rescue a backed-off drain; the "once an hour" claims were wrong (minor) — **taken** (the first option,
   with the sentences corrected too): the catch-up REPLACEs every job that is not RUNNING; a 404 STOP ends SUCCESS, so a
   project S5 stops listing is asked once an hour, not on a 5 h backoff; a pass with no usable session ends FAILURE (not
   signed in) or RETRY (signed in, the renewal unreachable). Pinned by AlbumPageTest's source reading.
4. A resend is not always `written: false`, because S5 checks role and visibility first (minor) — **taken**: confirmed
   (asis.js 834 and 836 before 858-864; 1001 before 1017). `AlbumQueue.settle` takes an op S5 holds out of `dropped`
   after the end-of-pass GET; the result line is judged by the photo's review, never by `dropped`; the "every retry"
   sentence corrected. Pinned in AlbumQueueTest and AlbumPageTest.
5. The scope race on a recorded answer drops a write a resend would land (minor) — **taken, refined**: confirmed (the
   phone sends no scope base; 878 is reached only past `w.seq !== v.base`). A cr_clear's 409 whose `work.seq` is its
   base AND whose work still has its live request is STOP — 878 and 879 alike. Refined: `No change request to clear`
   (877) also matches on the base but has no live request, and resending it could only loop, so it stays CONFLICT.
   Pinned in AlbumQueueTest, on a body shaped by hand from asis.js (S5Album's harness did not race two amendments).
6. Sheets the prototype closes on its own stay open; the answer's other refusals unspecified (minor) — **taken**:
   AlbumPage.sheet is null in the prototype's three cases (7578, 7520, 7529) and MainActivity closes such a sheet; every
   answer refusal but CLIENT_AT_DAY is Out.None. Pinned in AlbumPageTest and AlbumWriteTest.
7. Beside a pending card the neighbour does not stretch (minor) — **taken**: the row `height(IntrinsicSize.Min)`, each
   cell `fillMaxHeight()`, a plain card filling it, a pending card at content height above its two 40 dp buttons. The
   critic's Playwright figures were not re-measured here; they agree with the CSS (`.alb59grid` is a grid, which
   stretches; `.alb60cw` a column; `.btn`/`.tbtn` height 40, 257 and 259; 223.75 + 4 + 40 + 4 + 40 = 311.75). Pinned by
   source reading.
8. The per-login guarantees rest on unpinned :app reads, and `who` survives a refused renewal (minor) — **taken**:
   confirmed (relogin removes `pwd`, `cookie` and `cookie_at` and keeps `who`). `Me.signedIn` is part of canWrite; the
   per-login map lives in :pano (`AlbumQueue.Outboxes`, `AlbumWire.storedOutbox`), pinned there; each pass is pinned to
   its login and sign-out epoch (`S5Session.cookieFor`, `relogin(asLogin, atEpoch)`, `stopped`). **One departure from
   the finding's shape**: lists of entries, `{v, logins: [{login, projects: [{project, …}]}]}`, not objects keyed by
   login, because AlbumWire.Read cannot list an object's members.
9. K5's JPEG was built into K4's drain, leaving no room for a Wi-Fi-only rule (minor) — **taken** (the first option):
   K4's drain sends LOG, REVIEW and REFILE (`AlbumQueue.K4_STEPS`) and holds a photo op at the head untouched; K5 widens
   the set and decides the Wi-Fi rule. Pinned in AlbumQueueTest.

**Found wrong in the plan while building, and corrected above:** the outbox is one preferences entry holding every
login, not one entry per login (finding 8); `add` needs the login the tap was drawn for and the queue the screen drew
(finding 1); AlbumSync wrote the mirror in place, so a read beside it could parse a half-written file into no mirror;
S5Session had no way to pin a pass (its epoch private, relogin renewing whoever is stored), so it gains `epoch`,
`loginKey`, `cookieFor` and a pinned `relogin` — a file the plan did not list; `drain` needs the project and the reader,
and the commit needs the verdict BEFORE apply (`verdictOf`) so that only an answer that carries rows is put on the
phone first; `workCopy` must be null for a copy holding no states, or one work's row would read as the project's whole
states; the scope's overlay needs a mirror, or a queued answer with no baseline under it would invent a one-work scope;
`resultText` reads the model, not `dropped` (finding 4); `SheetFor` needs its `work`, and the sheets a sealed `Sheet`
and one `sheet(…)` that answers null; MainActivity's `albumMe` had to move above the effect it now keys; and K3's and
FW2's source-reading tests pinned the very literals K4 changes on purpose (the model's remember, `fileCache?.states`),
so they now pin K4's. Recorded, not built: a second tap on a sheet row in the frame before the snapshot re-reads can
queue the same write twice (S5 answers the second with its replay or a 409, nothing written twice); the answer race's
STOP has no bound (no persistent cause is known in asis.js).

:pano tests after K4, from the XML under pano/build/test-results/test: 867 tests, 0 failures, 0 errors, 0 skipped (824
before: AlbumPageTest 26 → 39, AlbumWriteTest 11 new, AlbumQueueTest 24 → 37, AlbumWireTest 12 → 14, AlbumTest 67 → 69,
QuoteStageTest 40 → 42). 1,794 keys, unchanged; `unkeyed.tsv` 860 lines before and after. :app is compiled only by CI;
nothing has been seen on a phone.

**The K4 review (9 Nov 2026)** — two findings on 2d33aa8, both confirmed independently (the second by a scratch model of
asis.js's review route against pano.jar), both minor, both fixed:

1. **The three new album sheets opened half-way** — `ModalBottomSheet(onDismissRequest = onDismiss)` with no sheetState
   takes material3 1.3.x's `rememberModalBottomSheetState()`, whose `skipPartiallyExpanded = false` opens a sheet taller
   than half the window at half height: at 360 × 780 the answer sheet's `Record the client's answer` and the state
   sheet's foot `Only the Project Manager sets a work's state` started below the fold (the verification found the
   state sheet's Abandoned row most likely still shown, the reviewer's one overstated detail), and at 780 × 360 every
   sheet opened on its handle, its title and a row or two. The prototype never draws a sheet half-open (`.sheet3` at
   its content's height up to the frame less 24 px, 278 and 381; its handle has no drag code). **Fixed**: each of
   `AlbumStateSheet`, `AlbumRaiseSheet` and `AlbumAnswerSheet` passes `sheetState =
   rememberModalBottomSheetState(skipPartiallyExpanded = true)`; their column already scrolls, so a sheet taller than
   the screen scrolls inside. Pinned in AlbumPageTest's source reading of AlbumSheets.kt (three ModalBottomSheet calls,
   all three opening whole). **App-wide, recorded and not changed here** (K4 adds and never moves): every other sheet
   on the phone keeps the half-open default — ScopeSheet (its `rememberModalBottomSheetState(confirmValueChange =
   confirm)` sets no skip), TreeShell's CaptureSkuSheet, ProjectSheet, CaptureSheet and WorkSheet, and Crumbs' two
   (a crumb's switcher and the jump-up list); whichever of them grows past half the window opens half-way, which the
   prototype draws for none. A slice of its own, through the prototype like any S2 UI change.
2. **A replayed review rebased the next review of its photo onto S5's CURRENT `review_at`** — `Album.afterPost` took a
   review's photo base from the answer's flat review fields, and `afterReplay` is `afterPost`; on a replay S5 fills
   those from the photo as it stands NOW (asis.js, the review PUT: `reviewOf(now_)`), while the op's own row comes
   separately as `entry`. So after a lost answer and two or more reviews by another login S5 gives role pm (D3), the
   next queued review of the photo was based on the newest of them and S5 took it over a review the PM never saw,
   where its guard on the old `review_at` would have answered 409 `Reviewed meanwhile` (with ONE such review the
   next one is the same state again and S5 answers `written: false`: no harm, the verification's correction). The
   K4 drain is the first live caller of this AQ/K1b code, and `AlbumWrite.atAdd` already did it rightly. **Fixed**:
   for a review, `afterPost` takes the base from the answer's `entry` when it is this op's row (its id, kind review,
   its photo, an `at`) — on a fresh write the same instant as the flat `review_at`, since S5 binds one `at` to both —
   and falls back to the flat `review_at` only when no such row came back (a replay S5 answers with no entry, when the
   photo already had the op's state). Pinned in AlbumQueueTest by a replay shaped by hand from asis.js (S5Album's
   harness has no second PM): the next review takes the replayed row's `at`, the mirror still takes the photo as S5
   holds it now, a replay without a row of its own (or with another op's) keeps the flat `review_at`, and a fresh
   write is as before. The test fails without the fix (the base came out as the other login's `review_at`).

:pano tests after the K4 review, from the XML under pano/build/test-results/test: 868 tests, 0 failures, 0 errors, 0
skipped (867 before: AlbumQueueTest 37 → 38; AlbumPageTest 39, one test gaining the sheet pins). No new key;
`unkeyed.tsv` unchanged. :app is compiled only by CI; nothing has been seen on a phone.

### 20. K5 · The progress shot

**Size:** M · **Depends on:** K4 *(on batch-next at 45ba927 above main 0bcfc54; its CI was running when this was written)*

*Derived on 9 Nov 2026, as §18 and §19 were: the §20 the lost plan held is gone. Every decision below names its source:
**[P n]** the approved prototype v61c.2 (index61c2.html, line n: the album core window.ALB60 — its items 7226–7234,
quoteAt 7249–7251, chapters 7254–7267, addShot 7272–7275; the page window.__mcftAlbum60 — back() 7392–7398, the result
line 7413–7416, the button 7483–7485, the camera 7490–7515, ov() 7575–7586; its CSS 348, 1462, 1470, 1502–1542; the
Capture 360 sheet 7997–8015, its ingest 8016–8023, the stand-in for the phone's camera 8307–8308, the room tile 8186–8193
and the photo's WORK line 8205–8208 with its re-file 8276–8277); **[S5]** S5 QUOTE as shipped on stage
(mcft-interior-estimate 9a51733: functions/_lib/asis.js — the photo record 919–949, its JPEG 952–988, S5-P's re-file
1024–1080, albumPutPhoto 1839–1846, albumSetBytes 1848–1851, the photo list's `bytes > 0` 1823, MAX_IMG 291, validPhoto
688–698; migration 0008's `client_at TEXT -- device clock, display only`; its CLAUDE.md rows "THE ALBUM STORAGE AND
ROUTES ARE REAL" and the S5-P paragraph); **[Ph]** the phone as shipped on batch-next 45ba927 (file:line); **[A3]** Amit's
answer 3 of 28 Oct 2026, as S5's CLAUDE.md (the S5-P paragraph) and Album.kt:1044-1049 record it: a single photo from the
phone's Capture 360 sheet after approval goes to S5's album "as a staff photo of its work through the progress shot's
route (record first, then the JPEG; K5)", and its later re-file is S5-P's, by its uploader; **[C]** decided by Claude
here, with the reason given. §18's table "THE CONTROLS THAT WRITE" names the slots K5 fills (the button in the body's
84 dp end padding, the result line's `Progress shot filed at …`), and §19's "Deliberately NOT in K4" what K4 left for
it: the two photo steps, the JPEG send, its rule against the Wi-Fi-only toggle (the K4 critic, finding 9), and the S5-P
re-file of a Capture 360 photo that went up by K5's route.*

**Goal**

Staff take a progress shot from the album, exactly as the prototype draws it: `Progress shot` in the album's corner
opens the camera with WORK (picked before the shot; the Quote journey while the estimate is not approved), ROOM and an
optional caption; the shutter files the shot at once — its card in its chapter, that chapter opened, `Progress shot
filed at <work>` in the header — and the phone sends it to S5's album as a staff photo, the record first and then the
JPEG, through K4's outbox whenever there is signal. A single photo taken from the Capture 360 sheet after Client approved
goes the same route [A3], stays ONE item on the phone (its capture row), and its later re-file on the WORK line reaches
S5 through S5-P. Nobody else shoots: a vendor's own upload (`Upload a photo`, `Uploaded to …`) is K6.

**Where a progress shot is taken from**

- **The album's `Progress shot` button** [P 7483], for staff: the one place in the album a shot starts. The prototype's
  work chapters have no camera of their own for staff; the only per-chapter camera is the vendor's `Upload a photo` in
  its own works [P 7462–7463, 7493], which is K6.
- **The Capture 360 sheet's A SINGLE PHOTO (Camera, Gallery), after Client approved** [A3; P 8014]. In the prototype
  that photo is a capture row like any other (ingest 8020: `S.queue.unshift({… kind:'Photo', work})`), drawn on the room
  screen and in the album through items() (7227–7229). The phone keeps it so (the row, its gallery copy, the room screen,
  the WORK line) and ALSO sends it to S5's album by the progress shot's route [A3]. Before Client approved it stays the
  phone's alone, as now, because answer 3 says "after approval" [A3] -- not because of S5, whose record takes a staff
  photo at work 0 at every stage (albumPutPhoto's `?3 IS NULL OR ?3 = 0 OR …`, asis.js 1842), the very rule K5's progress
  shot relies on before approval (the K5 critic, finding 14, corrected this sentence).
- Nothing else: no gallery pick in the album camera (the prototype's has none, 7499–7510), and no re-file of a progress
  shot (the prototype's album has none, 7463–7542; S5's CLAUDE.md: "K5's progress shot picks its work before the shot
  and is never re-filed").

**What the prototype draws, and what it does not**

The prototype's addShot (7272–7275) writes its store and the page redraws in the same tap, so — as for K4's writes — it
draws no queued, sending, refused or offline state for a shot. By Amit's rule (11 Oct 2026) the phone draws a shot not
yet on S5 as written (the overlay, below), one S5 took as S5's row, and one S5 refused as gone, with no words (§19's Q2).
Three things the phone must do that the prototype does not draw are each answered with the prototype's rendering and
recorded as a question below: the camera's viewfinder, where the prototype paints a simulated scene under its
`phone camera · simulated` tag (7508–7509; Q1); the phone's camera app between the shutter and the result (Q1); and a
project S5 does not list, where the prototype draws the button (Q3).

**THE CONTROLS** (S = staff, as "Who shoots" says)

| Control (who) | Where, exactly | Drawn / enabled when | Texts (key) | Tap |
|---|---|---|---|---|
| `Progress shot` (S) [P 7483–7485; CSS 1502–1503] | over the chapters' column, bottom end, 16 dp in (12 dp in landscape), in the 84 dp end padding K3 left (`.alb59body`, 1470) [Ph AlbumScreen.kt:86, :97]; an ExtendedFloatingActionButton, as the room screen's `Capture 360` is drawn [Ph MainActivity.kt:2799-2803]: 56 dp tall, 16 dp round, primaryContainer on onPrimaryContainer, the camera glyph `ic_mcft_cam` then the label (labelLarge), 16/20 dp, 12 dp gap | `canShoot` — never disabled | `album.progress_shot`; TalkBack `album.aria.take_progress` (the prototype's `aria-label`, which replaces the visible text) | opens the camera |
| the camera (S) [P 7508–7510; CSS 1504–1505, 1509, 1523, 1540–1542] | over the whole album, as the viewer is (a Box over the Scaffold) [Ph AlbumScreen.kt:166]: #000, white; the viewfinder (the height the controls leave, at least 120 dp) over the controls column (6/12/10 dp, 6 dp gaps); in landscape the viewfinder left and the controls a 300 dp column on the right, its content at the bottom | from the button; closed the moment `canShoot` goes (ov(), 7576) | TalkBack pane title `album.progress_shot` (the dialog's `aria-label`, 7508) | — |
| WORK, while locked [P 7502; CSS 1524–1531] | the controls' first row: at least 44 dp, 12 dp sides, 10 dp round, #1B1F1E, #EDEFEA; `WORK` labelSmall in 52 dp, #9AA29E; 8 dp gap; the value bodyMedium; read-only (`aria-disabled`: the WHOLE row at .85 -- its #1B1F1E with its texts, over the camera's black, the alpha before the background: the K5 review, finding 3 -- 6 dp above and below), its second text on a line of its own under the value | the album's lock banner is up (below) | `album.work_caps` · `album.quote_journey` · `app.stg_locked_quote` (on its own line its leading space collapses, as HTML draws it: `· locked until …`) | none |
| WORK, unlocked [P 7503–7504] | the same row, a button (role Button) | the banner is not up | `album.work_caps` · the work (Masters.work) or `album.pick_work` · `album.tap_to_change` (bodySmall, #9AA29E, after the value, no ` · `) | opens Which work? |
| ROOM [P 7505] | the second row, a button, the same look | always | `album.room_caps` · the room (Masters.room) or `common.whole_project` | opens Which room? |
| the caption [P 7506; CSS 1532–1536] | the third row: an OutlinedTextField, one line, on #1B1F1E, its outline 1 dp #5E6663 (2 dp #B9F227 focused), its text #EDEFEA, its label #9AA29E (#B9F227 focused) | always | `album.caption_optional` | — |
| `Cancel` [P 7510; CSS 1537–1538] | the bottom bar (84 dp, its three spaced between): first, a BARE white button at least 56 dp wide -- the prototype's `:where(.m3) button`: no padding, the camera's 16/24 (bodyLarge), its text at the start; never an M3 TextButton (the K5 review, finding 2) | always | `common.cancel` | closes the camera; nothing is written |
| the shutter [P 7507; CSS 1539] | the bottom bar's middle: a 68 dp white circle inside a 4 dp black ring and a 2 dp white one (80 dp across); a 56 dp spacer after it | a tap only when WORK is the Quote journey or a work; with `Pick the work` the same circle takes no tap (the prototype's `disabled` has no style of its own for `.alb59shut`) | TalkBack `album.aria.take_photo` | opens the phone's camera app; its ✓ files the shot |
| Which work? [P 7512] | a bottom sheet: FW2's WorkSheet [Ph TreeShell.kt:537-560] — `album.which_work`, the sixteen works over their OVERLAID states (K4's), the picked one on secondaryContainer, 12 dp after the last — opening whole (below); drawn over the ALBUM, the camera NOT drawn while it is open (the prototype's ov() draws the camera's sheet INSTEAD of the camera, 7582-7583; the K5 critic, finding 7) | from WORK, unlocked | `album.which_work`; the works (Masters.work), the states (Album.stateText) | sets the camera's work and closes, the camera coming back; a dismiss or ◁ brings the camera back as it was |
| Which room? [P 7513–7515] | a bottom sheet drawn as Which work? is (ModalBottomSheet opening whole, SheetTitle, one ListItem per row, the picked one on secondaryContainer, the rows scrolling, 12 dp after the last), over the album, the camera not drawn (finding 7) | from ROOM | `album.which_room`; the project's rooms (Masters.room) in the rooms grid's order, then `common.whole_project` | sets the room (Whole project: none) and closes, the camera coming back; a dismiss or ◁ the same, the room as it was |
| the result line [P 7413–7414] | K4's slot in the header, after the project line [Ph AlbumScreen.kt:253; §19's table] | after this visit's last shot, while the album holds it (queued, or S5's row) | `album.filed_x(stage = <the work's name; Quote journey for 0>)` | — |

**Who shoots: staff, signed in to S5, on a project S5 lists** [P 7483 staff(), 7272; S5 asis.js:922-924; Ph S5Session.kt:119-127]

`AlbumPage.canShoot(mirror, notOnS5, me)` is the one test; the button, the camera and the op read it.
- **Staff, as S5 says it**: the mirror is held and its `role` is `pm` or `designer` (the prototype's `staff()`; S5's
  record PUT takes a PM's and a Designer's photo alike and refuses only a client, 924; the phone signs in staff only,
  S5Session.login ending any other session). The mirror is needed although S5 compares no base for a photo [C; K4's
  writes need it too, for the role]: the record's answer and the JPEG's land in it (AlbumSync.take), and Album.merged keeps a
  null mirror null, so a shot that landed before the album's first read would vanish from the album between its op
  leaving the queue and the end-of-pass GET. Before that first read the album draws no button, as it draws K4's
  controls; the project open reads every listed project's album (§18), so that is a first open offline -- or the first
  open after a sign-in (forgetAll deletes the mirrors) before its read lands, or one whose read failed. The prototype
  draws the button for every staff member (7484), so this is a question for the prototype, Q6, not a decision taken on
  the phone (the K5 critic, finding 8).
- **Signed in to S5 as a login whoami named** (`Me.user`, `Me.signedIn`): the record's `by` is that login, S5 takes the
  JPEG from the record's uploader alone (959), and the outbox is the login's (§19). A phone with only a legacy ERP key,
  or signed out by a refused renewal, draws no button [C, K4's rule].
- **A project S5 lists**: the outbox is keyed by the S5 id (WishSync.stageKey, null for a project only on this phone
  [Ph WishSync.kt:128-138]), and S5 answers 404 before its handler for a project it does not list (922). The prototype
  draws the button there (its addShot has no S5 in it): Q3.

A vendor and a client are K6.

**THE CAMERA: the phone's own camera app** [Ph MainActivity.kt:965-968, 2756-2765; P 8307-8308; C]

The phone has one camera route and K5 uses it: ActivityResultContracts.TakePicture into a FileProvider file under
`files/shots/` (res/xml/file_paths.xml's `shots/`), the route the Capture 360 sheet's Camera takes — and the prototype
draws that route as a simulated "phone camera" too (cameraHtml, 8307, under the same `phone camera · simulated` tag the
album camera carries, 7508). So the dialog is the prototype's — WORK, ROOM, the caption, Cancel and the shutter, in its
places and sizes — and the shutter opens the phone's camera app; its ✓ files the shot as the prototype's shutter does
(shoot, 7497–7498), and its back leaves the dialog as it was. The viewfinder area is the dialog's black with nothing in
it [C: the phone has no preview there; the shot is taken in the camera app]: Q1. An in-app camera with a live preview
(CameraX) is NOT in K5: it is a new library and the CAMERA permission, and an app that declares CAMERA without holding it
has Android refuse the TakePicture intent the Capture 360 sheet already uses — its own slice, through the prototype.
- **The visit's camera** (`AlbumPage.Cam(work, room, caption)`, the prototype's `m.cam`, 7492) opens with the work the
  prototype gives it: while the album's lock banner is up the Quote journey (0); else the first work In progress by the
  OVERLAID states, else none (`Pick the work`, the shutter takes no tap) [P 7492: `K.x56(p)?(ow.length?ow[0]:null):0`];
  the room none (Whole project); the caption blank. The lock is the album's own (Chapters.execLocked: Client approved,
  or a state row, with an unread stage as execution, Album.exec [Ph Album.kt:706]) — S5's albumPutPhoto lock (1842) —
  rather than the prototype's x56, from which it differs only for a project with recorded work that is not approved,
  which approval being final leaves unreachable (§19) [C: one test for the banner, the chapters and the camera].
  Opening the camera closes the viewer [P 7491].
- **The shutter**: the gate runs on the Model (AlbumWrite.shot: canShoot, the lock, a filing), an id is minted
  (Album.mintId) and the camera app is asked to write `files/shots/album-<id>.jpg`. The pending shot —
  `AlbumWrite.Pending(project, login, Shot(id, work, room, caption))`: the S5 id, the LOGIN the tap was drawn for, the id,
  the work, the room, the caption — is held in `rememberSaveable` as its six strings (`Pending.saved()`, read back by
  `Pending.of`) [C], so a camera app that makes Android recreate the activity (it can, on a 4 GB Mi A3; MainActivity
  declares no configChanges, so a camera app held in landscape does it on return) still files the shot when its result
  reaches the new composition; the album itself is a plain `remember` and reopens closed, the app-wide rotation loss
  QM2's review recorded.
- **The camera app's ✓** (TakePicture's `true`): the camera dialog closes, and the shot is filed from the PENDING
  ENTRY ALONE (the K5 critic, finding 2): there may be no Model, mirror, queue or project held in the new composition,
  so `AlbumWrite.shotOp(shot, now)` builds the op with none of them, `seen` = empty, the lock left to S5's 409. The
  encode and the queueing run on a scope of the PROCESS (`AlbumShots.file`: a CoroutineScope(SupervisorJob() +
  Dispatchers.IO) of AlbumShots'), not the composition's -- a rotation during the second the encode takes cancelled that
  scope after the file was written and before the op was queued: first the shot's file is written (below), then the op
  is queued (AlbumOutbox.add under the pending entry's login) and the drain asked to run (REPLACE). ONLY ONCE THE ADD HAS
  RETURNED TRUE does the visit's result become `Res.Filed(work, id)` and the shot's chapter open
  (`Album.chapterOf(work)?.let { albumOpen[it] = true }` -- chapterOf is an `Int?`, so `albumOpen[Album.chapterOf(work)]
  = true` would not compile: the K5 critic, finding 10 -- the prototype's `m.open[nOfWork(p,x.work)]=true`, 7498), so
  no frame draws an opened chapter with no card. The card and the line appear about a second after the camera app
  closes (the encode) [C: the file first and the op after, so no op is ever queued without its JPEG].
- **Its back** (`false`): the dialog stays as it was; the camera app's empty file is deleted. A raw file that survives
  anyway (a failed decode, a process killed during the encode, the app closed with the camera app open) goes when it is
  a day old (`AlbumShot.rawStale`, by AlbumShots.trim at the end of every pass and in the hourly catch-up; the K5
  critic, finding 12) -- `album-*.jpg` only: the Capture 360 sheet's `shot-*.jpg` stay as recorded below.
- **A camera app that cannot be opened, or a file that does not decode**: nothing is queued and nothing is drawn (the
  prototype has no such state), one Log.w (Q1).
- **Back** [P 7392–7398]: a sheet of the camera first (ModalBottomSheet's own), then the camera
  (`BackHandler(enabled = camera open)`, composed after the viewer's), then the viewer, then the album.

**THE SHOT'S FILE** (`app/AlbumShots.kt`, NEW; its rules pure in `pano/AlbumShot.kt`, NEW) [S5 asis.js:952-980; Ph
AlbumQueue.kt:50-53; C]

S5 takes a JPEG (FF D8 FF, else 415) of at most 8 MB (413), written ONCE. The phone's camera app writes what it writes —
on most phones EXIF orientation rather than turned pixels, 12 MP or more on the Mi A3 — and the gallery can hand over a
HEIC or a PNG. So the queued file is never the camera's: AlbumShots writes it once, at the shutter —
- decoded with `inSampleSize` the largest power of two that keeps the long side at least `AlbumShot.LONG_SIDE` = 3072
  px (VIEWER_TARGET, the largest the phone ever draws a photo [Ph ZoomableImage.kt:44]), then scaled to it, never up;
- turned and mirrored by its EXIF orientation (android.media.ExifInterface, the platform's: no new library), the turn
  baked into the pixels — `AlbumShot.turnOf(orientation)`, the EXIF 1–8 table, pure. Every reader then draws it the
  right way up, and the EXIF goes with the re-encode: no location or camera serial reaches S5 or the client [C];
- encoded JPEG at quality 90, else 75, else 60 until it is at most S5's 8 MB (`AlbumShot.QUALITIES`, `MAX_BYTES`): at
  3072 px the first always fits (about 1.5–3 MB); the ladder is the guarantee;
- written to a temp file and renamed onto `filesDir/album/shots/<id>.jpg` (AlbumQueue.shotPath) — and only then is the
  op queued. The camera app's file is deleted.

It is KEPT until it lands, and then it MOVES, never copied: when S5 has the bytes (the JPEG PUT's 200, or a record
replay whose row already has them) the commit renames it into AlbumImages' cache under the album photo's own name
(`AlbumImages.adopt`) -- AFTER the outbox has been applied (the K5 critic, finding 3: moved before it, a crash between
left the op queued at its JPEG step `recorded` with no file there, a NO_FILE that dropped the photo's queued re-files;
moved after it, a crash between leaves a shot no op holds, which the trim deletes and the card then fetches from S5) --
so the card never asks S5 for the bytes it was just sent; a cache that already holds the file keeps its own and the
shot is deleted (the same bytes). AlbumImages.fetch looks at the shot FIRST for an album photo, then the cache, then S5
— one rule about where an album photo's bytes are, as §18 has one for a capture's — so one of the two files is there at
every instant of the move. A shot whose op S5 dropped is kept while the op is in `dropped` (the newest 20, §19) and
goes with it: `AlbumShot.orphans(files, held, now)` names every shot whose id is in no login's queue or `dropped` and
that is older than ten minutes (a shot is written before its op is queued), deleted at the end of every pass and by
the hourly catch-up -- and NOTHING when the outbox could not be read: `held` is null then (`AlbumOutbox.allOrNull`,
where `all` answers an empty outbox for a string that does not parse), and an unreadable outbox is not an empty one
(the K5 critic, finding 5: read as empty, it deleted every queued JPEG, each op then meeting NO_FILE for good). **A sign-out leaves them**: the outbox survives sign-out (§19), so do its shots.
AlbumSync.forgetAll deletes the mirror files in `filesDir/album` and nothing under `album/shots/` — today a non-empty
folder survives its `delete()` by accident and an empty one does not [Ph AlbumSync.kt:208-216; C].

**THE SINGLE PHOTO FROM THE CAPTURE 360 SHEET** [A3; S5-P; Ph MainActivity.kt:725-830, 2171-2190, 2278-2283]

- **It is queued in ingest**, after its row is inserted (MainActivity.kt:802-812), when it is a `Photo`, the project is
  at Client approved (`fileApproved`), its work is one of the sixteen (always so after approval: ingest refuses a photo
  with no work, the FW2 review's finding 3), the project has an S5 id (`fileKey`), and the phone is signed in to S5 as a
  login whoami named. AlbumShots writes the shot from the row's kept file (`panos/<id>.jpg`, FaceWriter.single's copy of
  the original [Ph FaceWriter.kt:532-552] — the clean photo, never the captioned gallery copy) and AlbumWrite.single
  builds the op: the album id `Album.photoIdOf(deviceId)`, the row's work, the row's room as the as-is record sends it,
  no caption (the prototype's queue item has none, 7229), `client_at` the row's createdAt. The room screen's own line,
  `Photo filed in …`, is unchanged [P 8021]. The op is QUEUED from the row as stored when it is added, after the encode
  (`AlbumOutbox.addSingle` under the outbox's lock, `AlbumWrite.singleAtAdd`): a re-file in that second takes the record
  with it, a capture deleted in it queues nothing; and this photo's sync runs after the add, not from ingest (the K5
  review, findings 1 and 4).
- **ONE ID FOR ONE CAPTURE** [Ph Handover.kt:62-65; C]: `Album.photoIdOf("MCAP-<12 hex>") = "ALB-<the same 12 hex>"`,
  S5's ALBUM_ID_RE, and `deviceIdOf` back. No store column and no mapping to keep: the capture names its album photo, a
  retry or a restart sends the same id (S5's replay), and the album composes the two as one.
- **ONE ITEM** (AlbumPage.compose, §18's one filing): the phone's row claims S5's album photo of its album id, as it
  claims S5's capture by its device id. The item is the phone's — filed by the phone, its kept file its picture — and
  remembers `album` = that id when S5 holds it or the outbox does, so its picture falls back to that album photo when the
  kept file has gone (the ERP leg deletes a single photo's file after its bench upload, SyncWorker.kt:294).
- **Its row reads `synced` once S5 has the JPEG** [P 8190]: the commit sets it SYNCED (CaptureStore.setState, no
  serverName) when its album photo lands; and the S5 leg's (c2) (SyncWorker.kt:162-176) passes over a Photo whose album
  op is still queued under any login (`AlbumOutbox.holdsPhoto`), which it would otherwise mark `on this phone only - the
  web takes a 360's survey prep, not a photo` on the queue screen [Ph MainActivity.kt:3403-3409] -- and takes that stamp
  off a held photo a pass before its op had marked (the K5 review, finding 4). SYNCED with no
  serverName is (d)'s own meaning, "on S5, not on the bench", which the ERP leg picks up (SyncWorker.kt:178-189).
- **Its WORK line re-files it on S5 by S5-P** — what FW2 left for K5. FW2's refile() sets the phone's copy and asks S5-R
  only for a capture whose as-is upload reads On web (WorkRefile.onWeb, MainActivity.kt:2278-2283), never true for a
  Photo; AQ built the op (Album.refileOp, Album.kt:1156) and K4's drain sends it, and nothing has queued one. K5's
  refile(), for a Photo, after setWork: `AlbumOutbox.refileSingle` builds the op with `AlbumWrite.refileSingle` on the
  queue AS STORED and the mirror FILE, under the outbox's lock, on ONE thread so two quick re-files are built in tap
  order, and queues `Album.refileOp(photo, to)` whose base is the photo's OVERLAID filing (the K5 critic, finding 1) —
  the target of this login's LAST queued re-file of it, else its queued photo op's work, else the mirror's row, else,
  for a row that reads SYNCED (its album photo landed, or the bench has it) with no mirror held (a sign-out forgot every
  mirror; a re-file before the album's first read), the row's own filing before this re-file, the work the phone last
  sent — whenever `to` differs from that filing. So A→X then X→A before the first is sent queues X→A behind A→X (base X),
  where the first version compared X→A with the mirror's A, queued nothing, and left S5 at X and the phone at A for good;
  and a re-file made with no mirror still reaches S5, whose 409 or 403 then brings S5's filing back and whose 404 (a
  photo it never had) is refused alone, the phone's filing standing. The drain sends `PUT …/album/photos/<id>/work`
  behind the record in FIFO order, so a re-file made before the shot has gone simply follows it (S5-P has no bytes term,
  1067 and 1874–1880), AQ rebasing a chain of re-files. A photo op queued under ANOTHER login -- signed out, or another
  signed in -- takes the re-file behind it in THAT login's queue, sent only under that login, the uploader S5-P takes
  it from (`AlbumWrite.refileSingleAt`, `Outboxes.holderOf`; the K5 review, finding 5: the first version moved the
  phone's filing alone there, "as a capture's does", which it does not -- a capture's next record carries the phone's
  work, a queued album record never changes). Nothing holds it -- a photo taken before approval, or nobody signed in
  with no queue holding it -- and the phone's filing moves alone [C].
- **S5 wins a re-file**: its 403 `Only the login that uploaded the photo re-files it` (1066) and its 409s (the lock,
  `Re-filed meanwhile — …`, 1078–1080) come with the photo as stored, "so the phone can put back the work it set locally"
  (S5's CLAUDE.md, S5-P): the commit puts S5's work back on the capture row (CaptureStore.setWork) when it differs, and the
  WORK line shows S5's filing again, with no words (§19's Q2). A row the drain moved (this, or SYNCED above) reaches the
  screens at once (the K5 critic, finding 6): the commit writes `AlbumOutbox.KEY_ROWS` in the outbox's preferences, the
  screen's listener calls reload() for that key (the room tile's synced, albumLocal, the album's single-photo item), and
  the capture screen re-reads its `navCapture` copy from the store's rows whenever they are read again.
- **A capture deleted before its photo reached S5** takes the queued op (and its photo's later re-files) out of whichever
  login's outbox holds it, queue and `dropped`, with its shot, under the store lock (`AlbumOutbox.unqueuePhoto`) [C: the
  prototype's delete takes the item out of the album]. One already on S5 stays there — S5 has no delete for an album
  photo — and the album then draws S5's copy (Q4). A RACE, recorded beside Q4 (the K5 critic, finding 13): a pass already
  inside the JPEG's blocking PUT holds the bytes and finishes it whatever the unqueue does, so a capture deleted at that
  moment lands on S5's album with nothing on the phone left to stand for it, under a confirm that said it never reached
  the server. Waiting for the pass lock would not avoid it (the PUT is already out), only move it.

**THE OVERLAY: a shot shows at once** [P 7272-7275, 7226-7234; Ph Album.kt:1235 provisionalPhoto; §19]

compose takes this login's queue (`compose(mirror, local, me, queue)`), so the tile, the model and every count read one
set of items:
- each queued photo op that no item already stands for is `Album.provisionalPhoto(op, role, me.user, me.name, at)` with
  `queued = true` — by this person, at its shutter instant, in its work and room, with its caption — and its picture is
  its shot (`Pic.AlbumJpeg(id)`, which AlbumImages finds on disk first);
- S5's row stands for it once its JPEG has landed (`bytes > 0`). The row the record's answer merges into the mirror at
  bytes 0 (AQ's recorded(), AlbumQueue.kt:250-255) never doubles it, and is never listed at all: the provisional stands
  until the bytes do, and S5's GET lists no row with none (1823). A read BETWEEN the record's answer and the JPEG's (the
  project open, the hourly read, the end-of-pass GET after a JPEG that STOPped) replaces the mirror with S5's page, which
  lacks that bytes-0 row, so the JPEG's `{ok, written, bytes}` -- a patch, not a row -- would patch nothing and the landed
  shot vanish until the next read (the K5 critic, finding 4). So the landed JPEG's answer carries a WHOLE row seeded
  from the op (`AlbumQueue.seeded`: its work, room, caption and kind, `client_at` and `at` the shutter, by the pass's
  whoami user and name in the mirror's role, `via` phone, the bytes S5 answered), which `Album.merged` adds only where
  the mirror holds no row of that id (`Album.Answer.seed`), never over S5's own; the next GET brings S5's;
- a single photo's capture row stands for its own (above);
- **a shot's time is its shutter's** [C]: the record carries `client_at` = the shutter instant (Instant.toString, under
  S5's 40 characters; migration 0008: "device clock, display only"), and compose shows an album photo at its `client_at`
  when that reads as an instant no later than S5's `at`, else at S5's `at` (`Album.takenAt`). A shot taken offline at
  10:00 and received at 18:00 keeps its day, its place in the walk and its quote group — quoteAt of the shutter, as the
  prototype's addShot stamps the shot at the tap (7274) — and no card's time moves between the overlay and S5's row
  (K4's Op.at rule). S5 keeps its own `at` beside it.
- The result line is `AlbumPage.resultText` for `Res.Filed(work, photo)`: drawn while the album's items hold the photo,
  gone once S5 dropped it (K4's rule: judged by what the album shows, never by `dropped`). `Res` becomes
  `sealed interface Res { Review(state, photo); Filed(work, photo) }`, K4's review result being `Res.Review`
  [Ph AlbumPage.kt:752-772].

**THE OUTBOX AND THE DRAIN: two more steps, the same pass** [Ph AlbumQueue.kt:59-65, 107-116, 250-255, 334-394;
AlbumOutbox.kt; §19]

- **The photo op joins the login's FIFO** at its tap, after every op stored, as a log op does (AlbumOutbox.add;
  AlbumWrite.atAdd gives a photo op back as it came). Its order against the work's log ops is the tap order; nothing on
  S5 orders them (albumPutPhoto reads no seq, 1839–1846).
- **The drain sends every step.** `AlbumQueue.K4_STEPS`, `drain`'s `sends` and `Pass.held` go: K4's hold existed only
  for this slice (the K4 critic, finding 9), and a parameter with no case left is the fallback the project's rule forbids
  [C]. A photo op is its RECORD then its IMG in the same pass (AQ: the record's 200 keeps the op at the head `recorded`,
  its JPEG next; a replay whose row already has bytes lands it).
- **The JPEG send** (AlbumOutbox.send, IMG): the file `filesDir/<rq.file>` read whole (at most 8 MB; the app is
  largeHeap) and sent as `image/jpeg` exactly — never with the `; charset=utf-8` K4's JSON sends carry [Ph
  AlbumOutbox.kt:240-256] — on the client and timeouts the as-is images use (AsIsUpload.exchange, its 300 s write
  timeout [Ph AsIsUpload.kt:238-241, 261]). A file that is not there (cleared storage) no retry can send: send answers
  `AlbumQueue.Sent(NO_FILE)` and `verdictOf` makes it REFUSED — that op and its photo's dependants alone
  (Album.afterRefused), one Log.w. The record S5 holds stays at bytes 0, which S5 never lists.
- **A work's 409 no longer drops a shot** [S5 1839-1846: albumPutPhoto's WHERE is a vendor's photo, the quote journey,
  unfiled, Client approved or a state row — no seq; C]. Album.dropWork (Album.kt:1431) drops "the later ops on the same
  work" for a 409 on a log or a photo op, so a state write lost to another writer took a queued shot into the same work
  with it — and its JPEG with the op. As K4 took reviews out of that cascade (its finding 2), K5 takes photo ops out: a
  log op's 409 drops the later LOG ops on its work; a photo op's 409 (only ever `locked: true`, 945) drops that op and
  its photo's later re-files and reviews (withDependants). Every other op meets its own answer — the lock is the
  project's, so a shot behind a locked one meets its own 409. A dropped shot's JPEG stays while it is in `dropped`.
- **The commit gains three steps**, each read off the pure verdict (`AlbumQueue.landed(op, verdict, reply)`,
  `AlbumQueue.refiledBack(op, verdict, reply)`): after (1) S5's rows into the mirror (a landed JPEG's seeded row with
  them, above) and before (2) the outbox apply, a single photo's capture row SYNCED and a refused or won re-file's work
  put back on its capture row -- a crash between them and (2) sends the op again, and S5's replay (`written: false`)
  does them again, harmlessly; AFTER (2), the shot moved into the cache (the K5 critic, finding 3, above). AlbumImages
  looks at the shot first, so the screen's one snapshot (the queue, then the mirror) never pairs a gone op with a
  missing picture either way.
- **THE WI-FI-ONLY TOGGLE IS NOT THE SHOT'S** — the decision the K4 critic's finding 9 left to K5 [Ph
  SyncWorker.kt:440-457, AsIsUpload.kt:83, MainActivity.kt:3215-3220; C]. The drain stays CONNECTED and sends the JPEG
  on any network. The toggle is the hourly sync's WorkManager constraint ("A 20 MB pano on a site's mobile data is
  somebody's bill"), and since AS1 the as-is upload sends each 360's six squared faces — six JPEGs, more bytes than one
  3072 px shot — on CONNECTED too. Holding the shot alone to Wi-Fi would make the toggle mean less, not more, and doing
  it without holding the log ops behind it needs a skip in the pass (the shape K4 kept room for): the pass reshaped for
  one step. Whether "Upload on Wi-Fi only" should hold every S5 upload — the shots and the 360s alike — is Amit's (Q5);
  until then the toggle does what it does today (the K5 critic, finding 9, asked to ask Q5 first or keep the toggle's
  promise; declined as a build change, because the promise is the hourly sync's constraint and AS1's six faces already
  go on CONNECTED, so a shot held alone would make the toggle mean less; its head-of-line cost is recorded in the Risks).
- **Not new**: one unique job per project (REPLACE on a tap, the hourly catch-up), one pass at a time pinned to its login
  and epoch, S5's answer before the op leaves the queue, the GET after a pass that took an answer (§19).

**S5's answers, and what the phone then shows** [S5 asis.js at 9a51733; AQ's verdicts]

| Step | S5's answer | Verdict | The mirror, the queue, the files | What the album shows |
|---|---|---|---|---|
| RECORD `PUT …/album/photos/<id> {v:1, work, room, caption, kind: "photo", client_at, via: "phone"}` | 200 `{ok, written: true, photo}` (949), the row at bytes 0 | POSTED | the row into the mirror; the op stays at the head `recorded`, its JPEG next in the same pass | the provisional (the row has no bytes) |
| | 200 `written: false`, the row as stored (949: its uploader's replay) | REPLAY | as POSTED; a row that already has bytes (its JPEG landed on a pass whose answer was lost) lands the op — adopted, SYNCED | the provisional, or S5's row |
| | 409 `{error: "Work starts once the client approves the estimate", locked: true}` (945) | LOCKED | that op and its photo's re-files dropped with S5's words; the shot kept while in `dropped` | the shot goes; the chapters lock once the GET brings the stage — no words (Q2) |
| | 403 `This photo id belongs to another uploader` (948), `A client cannot add photos to the album` (924); 400 (validPhoto's words, 688-698); 413 `Request too large` | REFUSED | that op and its photo's dependants alone | the shot goes — no words |
| | 404 `Not found` (922: not listed), 405, 5xx, no network | STOP | as it was | queued |
| IMG `PUT …/album/photos/<id>/img`, `image/jpeg`, the shot | 200 `{ok, written: true, bytes}` (980) | POSTED | the bytes onto the mirror's row; the op leaves; the shot adopted; a single photo's row SYNCED | S5's row: the same card and picture |
| | 200 `{ok, written: false, bytes}` (964-965, 976: written ONCE — a second PUT stores nothing) | REPLAY | as POSTED | the same |
| | 404 `Upload the photo record first` (958) | STOP, `recorded` cleared (AQ) | the next pass sends the record again: its uploader's replay | queued |
| | 403 `Only the uploader of this photo sends its image` (959); 415 `Send the photo as image/jpeg` (967), `That is not a JPEG` (973); 413 `Image larger than 8 MB` (969, 971) | REFUSED | that op and its photo's dependants; the shot kept while in `dropped` | the shot goes — no words (none reachable as built: the file is a JPEG under 8 MB, sent by the login that recorded it) |
| | 503 `Photo storage not bound for this environment` (953), 5xx, no network | STOP | as it was | queued |
| | (no file on the phone) | REFUSED (`NO_FILE`, the phone's) | that op and its photo's dependants | the shot goes |
| REFILE `PUT …/album/photos/<id>/work {work, base}` (a single photo) | 200 `written: true` (1069) or `false` (1075), the photo | POSTED / REPLAY | the row into the mirror; a later re-file of it rebased (AQ) | the phone's filing, which S5 now holds |
| | 403 `Only the login that uploaded the photo re-files it` (1066); 409 locked (1078), `Re-filed meanwhile — …` (1079), `Changed meanwhile` (1080) — each with the photo | REFUSED / LOCKED / CONFLICT | the row into the mirror; S5's work back on the capture row | S5's filing — no words |
| | 404 for a photo the mirror does not hold (AQ's noRow) | REFUSED | that op alone | the phone's filing (S5 has no such photo) |
| any | 401 | RELOGIN | renewed once, resent | queued |

**THE SHOT AS AN OP** (`pano/AlbumWrite.kt`, CHANGED) — each on K1b's gate and op builder, as K4's taps are:
- `shot(m, cam): Shot?` — the camera's SHUTTER: `m.shoot` (the Model's, kept when canShoot: the viewer, the album's
  lock, the OVERLAID states) and `Album.photoRecord(viewer, exec, work)` [Ph Album.kt:1013]: the Quote journey always, a
  work only unlocked; a null work is null (the shutter takes no tap then); else `Shot(Album.mintId(), work, room,
  caption)`, the pending shot.
- `shotOp(shot, now): Album.Op` — the camera app's ✓, from the pending shot ALONE (finding 2): `Album.photoOp(work,
  room, caption, PHOTO, id = shot.id).copy(at = now, clientAt = now)`; and `Pending(project, login, shot)` with
  `saved()` / `of(strings)`, what rememberSaveable holds across the camera app.
- `single(deviceId, work, room, createdAt): Album.Op?` — the Capture 360 sheet's photo: `Album.photoIdOf(deviceId)`;
  null when the id is not a device id or the work is not one of the sixteen.
- `refileSingle(mirror, queue, deviceId, from, to, landed, now): Album.Op?` — S5-P for a single photo, on the OVERLAID
  filing (above, finding 1); null when nothing holds it, or when `to` is that filing already.
- `openCam(m): AlbumPage.Cam?` — the camera's opening values (above); null when the person cannot shoot.

**Files and changes**

**pano/…/AlbumShot.kt (NEW, pure)** — `LONG_SIDE = 3072`, `MAX_BYTES = 8 * 1024 * 1024` (S5's MAX_IMG), `QUALITIES =
listOf(90, 75, 60)`, `sampleFor(w, h)`, `sizeFor(w, h)` (never up, the aspect kept), `Turn(degrees, mirror)` and
`turnOf(orientation)` (EXIF 1–8; anything else, no turn), `isJpeg(head)`, `SHOT_GRACE_MS = 600_000`, `ShotFile(id,
mtime)` and `orphans(files, held: Set<String>?, now)` (nothing for a null `held`: finding 5); `RAW_PREFIX`,
`RAW_GRACE_MS` and `rawStale(files, now)` (finding 12).

**pano/…/Album.kt (CHANGED)** — `photoIdOf(deviceId)` / `deviceIdOf(photoId)`; `photoOp(…, random, id: String =
mintId(random))` -- LAST, after `random`, since a default reads only the parameters before it (the first version of
this line put it before `random`, which does not compile); every caller unchanged; `Photo.queued: Boolean = false` and
`Photo.album: String? = null` (neither is S5's field and neither is written to the mirror, as `source` and `capture` are
not); `takenAt(p)`; `Answer.seed` and `merged` adding it only where no row of its id is held (finding 4); `dropWork` as
above, and its KDoc and afterLocked's say so.

**pano/…/AlbumQueue.kt (CHANGED)** — `K4_STEPS`, `drain`'s `sends` and `Pass.held` removed; `NO_FILE` (-404: no HTTP
status, read first) and its REFUSED in `verdictOf`, with `NO_FILE_WORDS` for the dropped op's note; `landed(op, verdict,
reply)`; `refiledBack(op, verdict, reply): Int?` (S5's filing of a photo whose re-file it refused or won, from the photo
its answer carries); `seeded(op, verdict, reply, role, by, byName)` (finding 4); `heldPhotos(outboxes): Set<String>`
(every photo op's id in any login's queue or `dropped`); `Outboxes.holds(photo)` and `Outboxes.withoutPhoto(photo)`;
the class note's drain paragraph says every step is sent.

**pano/…/AlbumPage.kt (CHANGED)** — `compose(mirror, local, me, queue = emptyList())` (the queued shots, the single
photo's claim, the taken time); `pic`: an ALBUM item with bytes, or `queued`, is `AlbumJpeg`, and a PHONE item with no
kept file and an `album` is `AlbumJpeg(album)`; `canShoot`; `Model.shoot` (internal) and `Page.shot: Fab?` (`Fab(label,
aria)`); `Cam`, and `Camera` (`work: CamRow(label, value, sub, tap, dim)`, `room: CamRow`, the caption's label, `shutter:
Boolean`, the shutter's and the pane's TalkBack, `cancel`) from `camera(tx, m, cam)`, null when the person cannot shoot;
`RoomSheet(title, rows: List<RoomRow(room: String?, text, selected)>)` from `roomSheet(tx, cam, rooms)`; `Res` sealed
(Review, Filed) and `resultText`'s Filed branch.

**pano/…/AlbumWrite.kt (CHANGED)** — the four above.

**app/…/AlbumShots.kt (NEW)** — `write(context, source: File, id): Boolean` (decode, turn, scale, the quality ladder,
temp + rename; never throws: false and one Log.w on any failure), `rawFile(context, id)` (the camera app's), `fileOf(context,
id)`, `file(context, pending, now, done)` and `single(context, project, login, op, source)` (the write then the add, on
its process scope: finding 2), `delete(context, id)`, `trim(context, held)` (AlbumShot.orphans against AlbumOutbox's held
ids, nothing for null; AlbumShot.rawStale).

**app/…/AlbumCamera.kt (NEW)** — `AlbumCamera(model, caption, onCaption, onWork, onRoom, onCancel, onShutter, landscape)`:
the dialog above, drawing the model and nothing of its own; the shutter's rings in drawBehind.

**app/…/AlbumSheets.kt (CHANGED)** — `AlbumRoomSheet(sheet, onPick, onDismiss)`, opening whole as K4's three do
[Ph AlbumSheets.kt:76, 108].

**app/…/TreeShell.kt (CHANGED)** — `WorkSheet(…, skipPartiallyExpanded: Boolean = false)`: the camera passes true, so its
Which work? opens whole as the prototype draws every sheet (the K4 review, finding 1); the Capture 360 sheet's and the
WORK line's calls keep the default — the app-wide half-open item §19 recorded is not K5's.

**app/…/AlbumScreen.kt (CHANGED)** — the button in a Box over AlbumBody, portrait and landscape; the camera over the album
with its BackHandler, NOT drawn while one of its sheets is open; the two sheets over the album (finding 7). New
parameters after K4's: `shot: AlbumPage.Fab? = page.shot`, `camera: AlbumPage.Camera?` (its caption and its open sheet
inside it), `roomSheet: AlbumPage.RoomSheet?`, `camStates`, `onShot`, `onCamWork`, `onCamRoom`, `onCaption`,
`onCamCancel`, `onShutter`, `onPickWork`, `onPickRoom`, `onCamSheetGone`, each defaulted so K4's call compiles.

**app/…/AlbumImages.kt (CHANGED)** — `fetch` for an `AlbumJpeg` looks at `AlbumShots.fileOf(id)` first; `adopt(context,
project, id)`.

**app/…/AlbumOutbox.kt (CHANGED)** — the IMG send and NO_FILE; the commit's three steps around the apply, with the seeded
row; `holdsPhoto(context, photoId)` (any login's queue); `unqueuePhoto(context, photoId)`; `refileSingle(…)` (one thread,
under the lock: finding 1); `allOrNull` (finding 5) and `AlbumShots.trim` after every pass and in `enqueueAll`;
`KEY_ROWS` (finding 6); the class note's "Not Wi-Fi-only" sentence names the JPEG and why (above).

**app/…/AlbumSync.kt (CHANGED)** — forgetAll deletes the mirror files only, never under `album/shots/`; its KDoc says why.

**app/…/SyncWorker.kt (CHANGED)** — (c2) passes over a Photo whose album op is held (`AlbumOutbox.holdsPhoto(context,
Album.photoIdOf(c.deviceId))`), and its comment says a single photo after approval goes to the album (K5) -- and records
a KNOWN UNTRUTH (finding 14): an approved project's photo that was not queued (its S5 sign-in had lapsed at the shutter)
still reads `on this phone only - the web takes a 360's survey prep, not a photo`, which S5's album now does. Its words
are a phone text no prototype draws, so a new wording waits for the prototype (Q7).

**app/…/MainActivity.kt (CHANGED)**
- `albumCamera = rememberLauncherForActivityResult(TakePicture())` beside `camera` (:965-968), filing the pending shot
  held in rememberSaveable (`albumPending`, the six strings of AlbumWrite.Pending) from that entry alone, on AlbumShots'
  process scope, the chapter opened and `Res.Filed` set only once the add returned true (finding 2); the camera's state
  (`albumCam`: AlbumPage.Cam, its open sheet inside it) beside `albumSheet` and `albumRes` (:212-222), cleared by
  leaveAlbum and by the tile's open (:1471-1482), and closed when its model goes;
- the outbox's preferences listener calls `reload()` when the key is `AlbumOutbox.KEY_ROWS`, and the capture screen
  re-reads `navCapture`'s work and state from the store's rows (finding 6);
- `albumItems = remember(mirror, albumLocal, albumMe, albumQueue) { AlbumPage.compose(mirror, albumLocal, albumMe,
  albumQueue) }` (:1457), so the tile counts a queued shot;
- the album branch (:1567-1633): the button, the camera and its two sheets; `onReview`'s result becomes
  `AlbumPage.Res.Review`;
- ingest (:725-830): the single photo's album op (above), on AlbumShots' process scope (`AlbumShots.single`); refile
  (:2278-2283): S5-P for a single photo through `AlbumOutbox.refileSingle` (above); onDelete (:2171-2190):
  `AlbumOutbox.unqueuePhoto` for a Photo.

**android/i18n** — **NO NEW KEY**: every text K5 draws is one the tables already hold (L1 landed the prototype's album
camera keys, v59 and v60), so Tx is not regenerated. `ported.txt` gains `pano/…/AlbumShot.kt`, `app/…/AlbumShots.kt` and
`app/…/AlbumCamera.kt`; `unkeyed.tsv` gains nothing (its lines only leave); `literal-counts.tsv` is rewritten by
`:pano:i18nRatchet`; README gains "Port 61c slice K5". The shutter's rings are drawn, not text; log lines and the
`album-<id>` file names are `// text:value`. The texts, EN and MR as the tables (and the prototype's RAW table) hold them:

| key | EN | MR |
|---|---|---|
| `album.progress_shot` | Progress shot | कामाचा फोटो |
| `album.aria.take_progress` | Take a progress shot | कामाचा फोटो काढा |
| `album.work_caps` | WORK | काम |
| `album.quote_journey` | Quote journey | कोटेशनचा प्रवास |
| `app.stg_locked_quote` | ` · locked until the client approves the estimate` | ` · ग्राहकाने अंदाजपत्रक मंजूर करेपर्यंत लॉक` |
| `album.pick_work` | Pick the work | काम निवडा |
| `album.tap_to_change` | tap to change | बदलण्यासाठी टॅप करा |
| `album.room_caps` | ROOM | खोली |
| `common.whole_project` | Whole project | संपूर्ण प्रोजेक्ट |
| `album.caption_optional` | Caption (optional) | कॅप्शन (हवे असल्यास) |
| `common.cancel` | Cancel | रद्द करा |
| `album.aria.take_photo` | Take the photo | फोटो काढा |
| `album.which_work` | Which work? | कोणते काम? |
| `album.which_room` | Which room? | कोणती खोली? |
| `album.filed_x` | Progress shot filed at {stage} | कामाचा फोटो "{stage}" मध्ये ठेवला |

`album.filed_x`'s placeholder is named `stage` (v59's; `tx.album_filed_x(stage = …)`) and carries the work's name
(Album.workName: `Quote journey` for 0). The works, states and rooms are Masters'.

**S5 must already serve**

All on stage since 26 Oct – 2 Nov, nothing new: `PUT …/album/photos/:id` and `…/img` (26 Oct), `PUT
…/album/photos/:id/work` (S5-P, 2 Nov), and `GET …/album` for the end-of-pass read.

**Tests and checks**

- **AlbumShotTest (NEW, :pano)**: `the sample keeps the long side at least 3072 and the size never grows` (4000×3000,
  3000×4000, 3072×2304, 1600×1200, 12000×9000); `every EXIF orientation turns and mirrors as the EXIF table says,
  anything else not at all`; `a JPEG is FF D8 FF`; `the qualities step down and 8 MB is S5's cap`; `orphans - a shot no
  queue or dropped holds goes after ten minutes, a younger one and a held one stay`; `an outbox that could not be read
  deletes no shot` (finding 5); `the camera app's own files go a day old, its others never` (finding 12).
- **AlbumWriteTest (CHANGED)**: `a shot is the record S5 takes - work, room, caption, kind photo, client_at the shutter,
  via phone - under the id minted at the shutter` (requestOf's body against a body shaped by hand from validPhoto,
  asis.js 688-698: S5Album's harness was lost with the scratchpad, so no new S5 answer is pasted as verbatim); `while
  locked the shot is the Quote journey's, unlocked a work's, and with no work nothing`; `a Designer shoots; a vendor, a
  client, no sign-in, no mirror read yet and a project S5 does not list do not`; `the camera opens on the first work In
  progress of the OVERLAID states, else on Pick the work, and on the Quote journey while locked`; `a single photo's op
  is its capture's album id, its row's room, no caption, client_at its createdAt - and none before approval or off the
  sixteen`; `the single photo's re-file is S5-P from the mirror's row or the queued op, and none when neither or already
  there`; `a re-file back while another is queued goes behind it, and a synced row with no mirror still reaches S5`
  (finding 1); `the caption and the room are held to S5's caps`; `the op is built from the pending shot alone - no Model -
  and the pending shot round-trips as strings` (finding 2).
- **AlbumPageTest (CHANGED)**: `a queued shot shows at once - its card by this person at its shutter, in its work, room
  and caption, its chapter counting it, the tile counting it`; `S5's row with bytes stands for it, and the record
  answer's row at bytes 0 never doubles it`; `a single photo is one item - claimed by its capture whether S5 or the
  outbox holds it - and its picture is the album photo once the kept file has gone`; `an album photo is shown at its
  client_at, never after S5's at, and a quote-journey shot sits in its shutter's group`; `the button per person - staff
  signed in on a project S5 lists, a Designer included; none with no sign-in, after a refused renewal, before the
  album's first read, on a project S5 does not list, for a vendor or a client`; `the camera - WORK read-only with the
  lock while locked, else the work or Pick the work and tap to change; ROOM the room or Whole project; the shutter a tap
  only with a filing`; `Which room? - the rooms in order, Whole project last, the picked one selected`; `the result line
  Progress shot filed at the work, the Quote journey for 0, gone when S5 dropped the shot; a review's line still K4's`;
  `every camera text in Marathi (Tx.MR)`; and read as source: the button in the body's end padding, the camera over the
  album with its BackHandler after the viewer's and not drawn while a sheet is (finding 7), the two sheets opening
  whole, the camera's file through the FileProvider's `shots/`, the pending shot in rememberSaveable and filed from it
  alone, the chapter opened only once the op is queued and with `?.let` (findings 2 and 10), the outbox listener's
  reload and navCapture's re-read (finding 6), the shot written before it is queued on the process's scope.
- **AlbumQueueTest (CHANGED)**: `a shot drains as S5 takes it - its record, then its JPEG, in one pass; a replay with
  bytes lands it`; `a state's 409 on the work leaves a shot behind it to go; a shot's locked 409 drops it and its photo's
  re-files, and nothing else`; `a JPEG the phone does not have is refused for good, that op and its photo's dependants`; `landed - the JPEG's
  200 and its replay, and a record replay with bytes; never a record without`; `refiledBack - S5's work from a refused or
  won re-file's photo, none from one it took`; `heldPhotos covers every login's queue and dropped`; K4's held test
  becomes `every step is sent`; and (finding 11) the K4 tests that pinned the old cascade are REWRITTEN with the photo op
  meeting its own answer -- `a 409 mid-queue …`, `a 409 locked …` and `a photo op dropped by its work's 409 takes its
  re-file with it` (now `a work's 409 leaves a shot on it to go, its re-file behind it …`), and AlbumTest's
  `afterConflict`, `afterLocked` and `a null mirror stays null` -- with the critic's cases pinned: `a read between the
  record and the JPEG … seeds a whole row back` (finding 4), `a pass that died after the JPEG landed sends the JPEG
  again …` (finding 3).
- **AlbumTest (CHANGED)**: `photoIdOf and deviceIdOf map the twelve hex digits both ways and nothing else`; `dropWork - a
  log op's 409 keeps the photo ops behind it, a photo op's takes its own dependants alone`; `takenAt - client_at when it
  is an instant no later than at, else at`; `merged takes a seed only where no row of its id is held`.
- **AlbumWireTest (CHANGED)**: `a photo op round-trips through the stored outbox with its client_at, recorded and
  caption`.
- **A source-reading test** (in AlbumPageTest, as K3's and K4's are): AlbumOutbox.kt's IMG send is `image/jpeg` with no
  charset, and NO_FILE for a missing file; the commit seeds, marks SYNCED and puts back before `apply`, and adopts AFTER
  it (finding 3); the trim reads `allOrNull` (finding 5); the re-files on one thread (finding 1);
  AlbumImages.fetch looks at the shot first; AlbumSync.forgetAll leaves `album/shots`; SyncWorker's (c2) asks
  holdsPhoto; ingest queues the album op only for a Photo at Client approved with a work; no `K4_STEPS` anywhere.
- **I18nTableTest, TxRenderTest, I18nLiteralTest** — the three new ported files; check (f) resolves every `tx.` the
  :app files call.
- **The CI gate** (android-app.yml — :app is compiled ONLY there).
- **Claude's stage check with d1-peek after Amit's walk**: `asis_album_photos` holds one row per shot of the walk —
  staff, `by` the login, `via` phone, `client_at` the shutter, `bytes` > 0 — the single photo's at `ALB-` + its capture's
  twelve hex digits, the re-filed one with `work_by` and `work_at` set; nothing for a project the walk did not touch; and
  `asis_album_log` unchanged by the shots (a photo writes no log row).

**What Amit sees**

- On a project's album, as staff: `Progress shot`, bottom right. A tap opens the black camera: WORK (before approval
  `Quote journey · locked until the client approves the estimate`; after it the first work In progress, or `Pick the
  work` with a shutter that does not take the tap), ROOM, Caption (optional), Cancel and the shutter. The shutter opens
  the phone's camera; its ✓ brings him back to the album with the shot in its chapter, that chapter open, and `Progress
  shot filed at <work>` under the project line — with signal or without.
- On an approved project a single photo from the Capture 360 sheet also reaches S5's album (the d1-peek above), its
  WORK line moves it there too, and its room tile reads `synced` once it has landed.
- Under मराठी, all of it in Marathi.

Amit's walk, one step at a time, on the test project the K4 walk approved and on one still in the quote journey
(invented, never a client's: every album photo it writes is permanent, S5 having no delete for one): a shot before
approval; a shot after approval, online; one in airplane mode, then online; a single photo from the Capture 360 sheet,
then re-filed on its WORK line; the same under मराठी.

**Questions for the prototype** — the phone draws the prototype's rendering for each until Amit approves otherwise on the
prototype:
1. **The viewfinder.** The phone's shutter hands the shot to the phone's camera app, so the dialog's viewfinder area is
   black with nothing in it, and the camera app appears between the shutter and the album. Approve that, or ask for an
   in-app camera with a live preview (CameraX and the camera permission, its own slice). A camera app that fails to
   open, or a file that does not decode, draws nothing.
2. **A shot still on the phone, and one S5 refused**: §19's Q1 and Q2 (drawn as written; gone, with no words).
3. **A project S5 does not list.** The prototype draws `Progress shot` there; the phone has no S5 id to queue the shot
   under and draws no button.
4. **Deleting a single photo that reached S5's album.** The capture goes from the phone; S5's album keeps the photo (it
   has no delete), and the delete's confirm still says "It never reached the server, so there is nothing there to
   remove" — already untrue for a 360 on S5 since AS1, whose item 5 (the delete and its confirm) waits for the prototype.
   The same wording question, for photos too -- and the race it cannot cover (the K5 critic, finding 13): a capture
   deleted while its JPEG's PUT is already out still lands on S5's album, under that same confirm.
5. **"Upload on Wi-Fi only"** (Amit's, not a drawing): should it hold every S5 upload — the album's shots and the 360s'
   six faces — or stay the hourly sync's, as today?
6. **`Progress shot` before the album's first read** (the K5 critic, finding 8). The prototype draws the button for every
   staff member (7484); the phone draws none until a mirror is held -- a first open offline, the first open after a
   sign-in before its read lands, or a read that failed -- because a shot that landed with no mirror would leave the
   album between its op leaving the queue and the next read. Approve that, or ask for the button always (the provisional
   item would then stay until the first GET).
7. **An approved project's single photo that was not sent** (finding 14): its queue line still reads `on this phone only -
   the web takes a 360's survey prep, not a photo`, untrue since S5's album takes approved photos. A new phone text,
   so its words go through the prototype.

**Deliberately NOT in K5**

- K6: vendor and client sign-in on the phone, a vendor's `Upload a photo` into its own works with its note `Your photos
  show to the client once the Project Manager accepts them`, and `Uploaded to … · awaiting review`.
- An in-app camera (Q1), and a gallery pick in the album camera (the prototype has none).
- A re-file of a progress shot (the prototype's album has none), and deleting an album photo on S5 (no route).
- A single photo before approval to S5 (answer 3 is after approval); a single photo taken before K5, or before approval
  and re-filed after it, is not sent afterwards.
- The other sheets' half-open default (§19's app-wide item); the Capture 360 sheet's camera Uri under a plain `remember`
  (MainActivity.kt:965: the activity-recreation exposure K5 closes for its own shot); and its `files/shots/shot-*.jpg`,
  never deleted after ingest — recorded here, not changed.
- Whether FaceWriter.single's captioned gallery copy honours the EXIF orientation (it decodes with BitmapFactory, which
  ignores it, FaceWriter.kt:554-563): K5's shot does; the gallery copy is not K5's.
- Before / After pairs (S5 has no `pairOf`).

**Risks**

- **The encode** holds a 12 MP bitmap (about 48 MB) for a moment, off the main thread, on AlbumShots' process scope (a
  rotation no longer cancels it, finding 2): a process killed during it loses the shot (the camera app's file stays
  under `files/shots/`, deleted a day later).
- **A shot S5 drops is lost** once it leaves `dropped` (the newest 20): the prototype has no gallery copy of a progress
  shot and K5 adds none. As built every drop above is unreachable but the lock race and a cleared disk.
- **Two copies of a single photo** on the phone until it lands: the kept original and the 3072 px shot, which then moves
  into the cache Android may evict.
- **Data**: 1.5–3 MB per shot on mobile data whatever the toggle (Q5).
- **Head of line** (the K5 critic, finding 9): the drain is FIFO (§19), so a JPEG at the head that STOPs on a weak link
  -- a 300 s write timeout, a 120 s read timeout, a 5xx -- holds every later state, raise and answer of the project until
  it goes, the pass ending RETRY with WorkManager's backoff, although S5's log POST never reads a photo. Letting a log op
  pass a stopped JPEG step (only that photo's re-files and reviews depend on it, as withDependants encodes) would
  reshape the pass for one step; not built, recorded.
- **Storage held by a departed login** (finding 12): a login's queued and dropped shots are kept until that login signs
  in again and its drain runs, megabytes each, with no cap; and a JPEG step's 404 (S5 has no record) ends the pass as
  K4's 404 rule says, SUCCESS, so its record goes again only at the next project open or the hourly catch-up.
- **A downgrade to a K4 build** holds the first queued photo op at the head (K4's `held`), and the rest of that project's
  queue behind it, until K5 is back.
- **The clock**: `client_at` is the phone's; a clock ahead is capped at S5's `at`, one behind shows the shot earlier than
  it was taken.
- **Data on stage**: every row the walk writes is permanent.

**Critic notes, answered**

1. *"A shot S5 has not taken yet is a second answer."* — It is the pending half of S5's one photo (§19's note 1): its
   card goes the moment S5's row with bytes stands for it, and nothing about it is set by hand that S5 does not then take
   or refuse.
2. *"The single photo is filed twice — a capture on the phone and an album photo on S5."* — One item, by one rule: the
   capture names its album photo (`photoIdOf`), compose lets the phone's row stand for both, and the phone's filing
   reaches S5 by S5-P as a capture's reaches it by S5-R.
3. *"Why not rewrite a queued shot's work in place, rather than queue an S5-P re-file behind it?"* — A pass may be sending
   its record at that instant: S5 would take the old work, the stored op keep the new, and nothing would ever reconcile
   them. A re-file queued behind it is right whatever the timing, for one small PUT.
4. *"A state write that loses a race takes the PM's photo with it."* — It did (dropWork); K5 changes it, because S5's
   photo record reads no seq.
5. *"The JPEG ignores Wi-Fi-only."* — As the as-is upload's six faces already do; decided above, with Q5 for Amit.
6. *"The camera app kills the activity and the shot is lost."* — The pending shot is in rememberSaveable WITH its login
   and project, and the result files it from that entry alone, on a scope of the process (the K5 critic, finding 2,
   corrected this answer: its first version needed the album's Model, which a recreated activity does not hold). The
   album reopens closed, the app-wide loss.
7. *"S5 is told the shutter's time by the phone's clock."* — In `client_at`, which S5's own column calls "device clock,
   display only", never shown later than S5's `at`; S5 keeps its own `at`.
8. *"Re-encoding loses the EXIF."* — On purpose: the turn is baked in, the time travels as `client_at`, and no location
   or device serial reaches S5 or the client.
9. *"A sign-out deletes the queued shots."* — It does not, and now explicitly: forgetAll deletes the mirrors only.
10. *"The card downloads the 3 MB the phone just sent."* — The shot moves into the cache when it lands, and AlbumImages
    looks at the shot first, so the card never asks S5 for it -- once the adopted file is marked drawn as it moves (the
    K5 review, finding 6: a rename kept the encode-time mtime, and a full cache trimmed it first).
11. *"`Pick the work` beside a shutter that looks as usual reads as a broken button."* — The prototype draws its disabled
    shutter with no style of its own (7507; no `:disabled` rule for `.alb59shut`), and the WORK row says what to do;
    TalkBack says the shutter is disabled.
12. *"The camera's lock is not the prototype's x56."* — It is the album's own banner test, equal to x56 in every case
    approval-is-final leaves reachable: one test for the banner, the chapters and the camera.
13. *"K4 gave a Designer nothing to write."* — The prototype's camera is staff's (`staff()`, 7483; addShot `pm` or
    `designer`, 7272), and S5's record PUT takes a Designer's (922-924).
14. *"The album id is the device id's hex — a collision?"* — Both are 48 random bits minted once; `ALB-<hex>` names one
    row of one project (asis_album_photos is keyed by project and id), and the log's ids live in another table.
15. *"A queued shot of another login shows, or is sent under this one."* — Neither: the outbox is the login's (§19), and
    so is its overlay.
16. *"A project's queue waits behind a big JPEG."* — FIFO by design (§19): the head goes first, and a log op tapped behind
    a shot waits seconds on a site's 4G.
17. *"Which work? in the camera opens half-way, as FW2's does."* — WorkSheet gains `skipPartiallyExpanded`, which the
    camera passes; FW2's two calls are the app-wide item.
18. *"The capture's delete confirm becomes untrue for a single photo on S5."* — It already was for a 360 on S5 since AS1;
    Q4, with AS1's item 5.
19. *"The button waits for the album's first read, which the prototype's does not."* — A shot landing with no mirror
    would leave the album until the end-of-pass GET (Album.merged keeps a null mirror null), a frame the prototype never
    draws; and the project open reads every listed project's album (§18), so only a first open offline is without it —
    when K4's controls are absent too. (The K5 critic, finding 8: still a departure the prototype does not draw, so it
    is Q6 rather than answered here.)

**The K5 critic (9 Nov 2026)** — fourteen findings on §20, each checked against the source before building; twelve
taken, one taken as a record only, one declined as a build change and taken as a record:

1. A re-file back before the first is sent, and a re-file with no mirror, leave S5 and the phone apart for good (major)
   — **taken**: confirmed (refileSingle compared with the mirror's row or the photo op, never a queued re-file; forgetAll
   deletes every mirror). The base is the OVERLAID filing (the last queued re-file's target, the photo op's work, the
   mirror's row), and a row reading SYNCED with no mirror is based on its own last filing so S5's 409 or 403 brings S5's
   back; built under the outbox's lock on one thread, so two quick re-files are built in tap order (a race the finding
   did not name: two IO coroutines could build the second before the first was stored). Pinned in AlbumWriteTest.
2. After a recreation the shot cannot be filed, and a rotation during the encode orphans it (major) — **taken**:
   confirmed (navProject, albumFor and the queue are plain `remember`; ingest's split runs in the composition's scope).
   The pending entry holds the project and the LOGIN and is saved as six strings; the op is built from it alone
   (`AlbumWrite.shotOp`, no Model); the encode and the add run on AlbumShots' process scope; the chapter opens and
   `Res.Filed` is set only once the add returned true. Pinned in AlbumWriteTest and by source reading.
3. The shot moved into the cache before the outbox is applied makes a crash a NO_FILE that drops the photo's re-files
   (minor) — **taken**: confirmed (AlbumQueue.shotPath is read at the JPEG step, and afterRefused for a photo takes its
   dependants). The move is after (2); a crash then leaves an orphan the trim deletes. Pinned by source reading and in
   AlbumQueueTest (the resent JPEG lands, its re-file goes).
4. A read between the record and the JPEG takes the bytes-0 row out, so the landed shot vanishes until the next read
   (minor) — **taken** (the first option): confirmed (Album.merged applies a bytes patch only to a row it holds; S5 lists
   no bytes-0 row). The landed JPEG's answer seeds a whole row from the op (`AlbumQueue.seeded`, `Album.Answer.seed`),
   added only where no row of that id is held. Pinned in AlbumQueueTest and AlbumTest.
5. The orphan trim reads an unreadable outbox as empty and deletes every queued shot (minor) — **taken**: confirmed
   (AlbumOutbox.all caches an empty Outboxes on a parse failure). `allOrNull` says when it failed, `orphans` deletes
   nothing for a null `held`. Pinned in AlbumShotTest.
6. The drain's capture-row changes reach no screen until a resume (minor) — **taken**: confirmed (reload() runs on resume
   and when `mcft-sync` finishes; navCapture is a copy). The commit writes `KEY_ROWS`, the listener reloads on it, and
   the capture screen re-reads navCapture from the rows. Pinned by source reading.
7. The camera's sheets are drawn over the camera, where the prototype draws them instead of it (minor) — **taken**:
   confirmed in the prototype's ov() (7582-7583); the measurement was not repeated here. The camera is not drawn while
   its sheet is open, the sheet is over the album, and a pick, a dismiss or ◁ brings the camera back. Pinned by source
   reading.
8. Hiding Progress shot before the album's first read is a departure with no question raised (minor) — **taken** as Q6:
   the cause is not removed (with finding 4 fixed a landed shot still has no mirror to land in before the first read),
   so the departure is raised for the prototype rather than decided on the phone.
9. Q5 is decided against the visible Wi-Fi-only toggle, and a stuck JPEG holds the project's queue (minor) — **declined
   as a build change, taken as a record**: the toggle is the hourly sync's WorkManager constraint (SyncWorker.schedule)
   and AS1's as-is upload already sends six faces on CONNECTED (AsIsUpload.enqueue), so holding the shot alone keeps no
   promise the app keeps today, and letting a log op pass a stopped JPEG reshapes K4's FIFO pass for one step. Q5 stays
   Amit's; the head-of-line cost is in the Risks.
10. `albumOpen[Album.chapterOf(work)] = true` does not compile (minor) — **taken**: chapterOf is an `Int?`; written
    `Album.chapterOf(work)?.let { albumOpen[it] = true }`, and a source test refuses the other form.
11. K4's tests assert the opposite of K5's dropWork, and five behaviours have no test (minor) — **taken**, and wider than
    found: besides AlbumQueueTest's two, its `a photo op dropped by its work's 409 takes its re-file with it` and
    AlbumTest's `afterConflict`, `afterLocked` and `a null mirror stays null` asserted the old cascade, and K4's source
    test pinned three sheets in AlbumSheets.kt (now four) and K3's page for a Designer (who now has the button). All
    rewritten; the five behaviours pinned (findings 1-5).
12. Raw camera files and departed logins' shots are never deleted (minor) — **taken**: `album-*.jpg` a day old go
    (`AlbumShot.rawStale`); a departed login's shots, and a capture deleted while its op sits in `dropped` (now taken
    out of `dropped` too), are recorded in the Risks.
13. A capture deleted while its JPEG uploads still lands on S5 (minor) — **taken as a record**, beside Q4: the PUT is out,
    and waiting for the pass lock would only move the race.
14. "S5 takes no single photo before approval" is false, and (c2)'s words become untrue after approval (minor) —
    **taken**: the reason is answer 3 alone (S5's record takes a staff photo at 0 at every stage, asis.js 1842); (c2)'s
    message for an approved photo that was not queued is recorded as a known untruth in SyncWorker's comment, and its
    wording is Q7 (a phone text no prototype draws).

**Found wrong in the plan while building, and corrected above:** `photoOp(…, id: String = mintId(random))` before
`random` does not compile (a default reads only the parameters before it), so `id` is last; AlbumWrite.shot as
`shot(m, cam, id, now): Out` could not serve a recreated activity (finding 2), so it is the shutter's `shot(m, cam):
Shot?` and the ✓'s `shotOp(shot, now)`, with `Pending`; refileSingle needs the row's own filing and whether it reads
synced (finding 1), and a serial executor in AlbumOutbox; the JPEG's answer needed a seeded row (finding 4), a field the
plan's Answer did not have; NO_FILE is a code no HTTP status can be (-404), read before every other rule, with a note for
the dropped op since NO_FILE has no S5 words; compose must never list a bytes-0 album row at all, not only while its op
is queued (S5 lists none, and one whose JPEG was refused would otherwise draw a card with no picture); SyncWorker's
(c2) skip is for a single photo only (a 360's survey prep message stands); Album's no-display-string test refuses a
`MCAP-` String field, so the prefix is inlined; AlbumScreen needs `onCamSheetGone` for a dismissed sheet of either kind.
Recorded, not built: a JPEG step's 404 ends the pass SUCCESS by K4's 404 rule, so its record goes again only at the next
project open or the hourly catch-up; the rememberSaveable entry survives a recreation and Android's own saved state,
not a pending shot whose process died during the encode (the Risks).

:pano tests after K5, from the XML under pano/build/test-results/test: 907 tests, 0 failures, 0 errors, 0 skipped (868
before: AlbumShotTest 7 new, AlbumPageTest 39 → 50, AlbumWriteTest 11 → 20, AlbumQueueTest 38 → 45, AlbumWireTest 14 →
15, AlbumTest 69 → 73). 1,794 keys, unchanged; `unkeyed.tsv` 860 lines before and after. :app is compiled only by CI;
nothing has been seen on a phone.

**The K5 review (9 Nov 2026)** — six findings on 51f2a3f, each independently confirmed and all minor; all six fixed
(the verification's corrections noted where it made them):

1. **A single photo's op was built at ingest and queued only after the 1–2 s encode, so a re-file or a delete in that
   window was lost** — ingest built `AlbumWrite.single(id, workNow, …)` and AlbumShots.single added it after `write`;
   a re-file on the WORK line in between found no queued op, no mirror row and a LOCAL row, so S5-P queued nothing and
   the record then went with the old work, which S5 keeps as immutable (the phone at the new work, S5 and the client at
   the old, for good); a delete in between found no op and no `<id>.jpg` (still a `.part`), and the add then queued the
   photo of a deleted capture (the verification: only once the full decode had read the kept file, which the delete also
   removes). **Fixed**: the op is queued from the capture row AS STORED at the add — `AlbumOutbox.addSingle` re-reads the
   row under the outbox's lock, the one refileSingle and unqueuePhoto take, and `AlbumWrite.singleAtAdd(op, rowWork)` files the op
   at the row's filing now (only the work moves), or queues nothing when the row is gone (AlbumShots then deletes the
   shot) or off the sixteen. A re-file whose lock came first moved the phone alone and the op now carries it; one whose
   lock comes after is based on the op as queued. And onDelete deletes the row BEFORE it launches the unqueue, so an op
   still being encoded is either never queued or queued before the unqueue takes it out (the unqueue used to be launched
   first, leaving a moment in which the add could read a row about to go). Pinned: AlbumWriteTest (the re-file in the
   window queues nothing, the add carries 95 where `built` carried 90, a re-file after it is based on 95, a gone row or a
   filing off the sixteen queues nothing) and AlbumPageTest's source reading (AlbumShots.single never calls
   `AlbumOutbox.add(`; addSingle reads the row under the lock before it writes; the row's delete before the unqueue).
2. **The camera's Cancel was an M3 TextButton** (labelLarge 14/500, 12 dp padding, a 58 dp minimum, centred), where the
   prototype's `.alb59x` is a bare button (`:where(.m3) button`: no padding, the camera's 16/24 weight 400, text-align
   inherited; measured 56 × 24, its text at the bar's edge, the shutter centred), so the text sat about 12 dp in and the
   shutter 4–7 dp right of centre at 360 × 780 (the verification's figure; the finding said about 7). **Fixed**: a Box at
   least 56 dp wide, `clickable(role = Role.Button)`, its content at the start, holding `Text(model.cancel, bodyLarge,
   white)`; the TextButton and ButtonDefaults imports go. Pinned by source reading (no `TextButton(` in AlbumCamera.kt,
   the Box and its Text). The table above says so.
3. **The locked WORK row's .85 dimmed only its texts** — `alpha` came after `background(CAM_ROW)` in the chain, so the
   #1B1F1E stayed whole where CSS opacity composites the whole row over the camera's black (about #171A1A). **Fixed**:
   the alpha before the clip and the background, so the row, its background and its texts are dimmed together (the
   dimmed rows are never tappable, so the clickable's place does not matter). Pinned by source reading (the alpha before
   `.clip(shape)` and `.background(CAM_ROW)` in CameraRow). Not built to a scratch test: :app's Compose jars are not in
   the offline cache; read against Compose's modifier-order rule.
4. **(c2) could stamp a single photo ERROR `on this phone only - the web takes a 360's survey prep, not a photo` while
   its op was still being encoded, and never took the stamp off** — ingest called SyncWorker.syncNow right after the
   insert, and any pass (that one, or the hourly) reaching (c2) before the add found no held op; once the op was queued
   (c2) only `continue`d, so the line stayed until the JPEG landed. **Fixed, both parts**: (c2) takes its own stamp off a
   held photo (`notPhoto` → LOCAL) before it passes over it; and ingest no longer syncs a photo it handed to the album
   (`toAlbum`) — AlbumShots.single runs SyncWorker.syncNow after the add, whatever the add answered, so this photo's own
   sync never reads it before its op exists, and the ERP leg it starts cannot delete the kept file under the encode. The
   finding's second part, that ERP-leg delete between the encode's three opens, the verification found overstated (the
   two decodes are microseconds apart, a failed EXIF read falls back to normal, and the bench upload takes far longer
   than the decode); it is closed for this photo's own sync by the same move, and an hourly pass landing exactly then
   is recorded, not built (one stream for bounds, decode and EXIF would close it). Pinned by source reading (the held
   branch's LOCAL before its `continue`; ingest's `if (!toAlbum) SyncWorker.syncNow(context)` and no unconditional sync;
   AlbumShots.single's sync after the add).
5. **A single photo re-filed while its queued op belonged to a login not signed in now left S5 and the phone apart for
   good** — refile() queued S5-P only while signed in, and AlbumOutbox.refileSingle read the current login's queue
   alone, so a photo op queued under A, re-filed while signed out or signed in as B (the WORK line reads the per-project
   stage cache, which a sign-out keeps), queued nothing; A's record later went with the work frozen at ingest. §20's
   "as a capture's does" did not hold: a capture's next record carries the phone's work, a queued album record never
   changes (the verification proved the pure half with a scratch :pano test). **Fixed**: `Outboxes.holderOf(photo,
   project)` names the login whose QUEUE of the project holds the photo op, and `AlbumWrite.refileSingleAt` queues the
   re-file behind it in that login's queue — sent only under that login, the uploader S5-P takes a re-file from, so no
   sign-in now is needed — else in the signed-in login's on the mirror or the landed row, as before; refile() asks for it
   with no sign-in test of its own (the delete's unqueuePhoto already worked across logins). Pinned: AlbumWriteTest
   (holderOf by project and queue, never `dropped`; signed in as another login, signed out, or blank, the re-file goes
   behind A's photo op on base 90; a second one is based on the first; with no holder the signed-in path is unchanged
   and nobody signed in queues nothing) and source reading (refileSingle places it through refileSingleAt and writes the
   login it names; the WORK line's test has no sign-in). The text above is corrected.
6. **A landed shot moved into the image cache kept its encode-time mtime, so the trim inside adopt could evict it
   first** — a rename keeps the inode's mtime and AlbumPage.trimmed deletes least recently drawn first, so on a full cache
   a shot taken offline and landed a day later went in the very trim adopt runs, and the card downloaded from S5 the
   bytes the phone had just sent (critic note 10 said it never would). The verification ran it (a scratch run against
   pano.jar: the renamed file kept its day-old mtime and was trimmed; marked now, it stayed) and narrowed it: only when
   about 192 MB of the cache had been drawn since the shutter, so mostly a shot taken offline, and one wasted download,
   never lost data. **Fixed**: adopt marks the cache file drawn now (`setLastModified`, as hit() does) after the move or
   the copy and before `trim(d)`. Pinned: AlbumPageTest (a full cache's trim takes the adopted file at its encode-time
   mtime and keeps it marked now) and source reading (the mtime set before adopt's trim). Critic note 10 is corrected.

Every pin was checked against 51f2a3f's :app sources: the new source test fails with each of the six changed :app files
(AlbumShots, AlbumOutbox, MainActivity, SyncWorker, AlbumCamera, AlbumImages) reverted alone, and with the alpha order
alone reverted; the pure tests of findings 1 and 5 call functions 51f2a3f does not have. No new key; Tx not regenerated; `unkeyed.tsv` 860 lines before and after; no new ported file.

:pano tests after the K5 review, from the XML under pano/build/test-results/test: 911 tests, 0 failures, 0 errors, 0
skipped (907 before: AlbumWriteTest 20 → 22, AlbumPageTest 50 → 52). :app is compiled only by CI; nothing has been seen
on a phone.


### 24. M2 · Marathi: the tree, its dialogs, the queue and the upload status

**Size:** L · **Depends on:** L2, FW2

**Goal**

Key everything between the shell and the screens: the TREE's remaining words (TreeShell.kt — the drawer header, the
top bar's three actions, the Tag-to-SKU and project sheets, the bottom bar, the rename-and-delete dialog), the
tree's own dialogs in MainActivity (Add site with its new client / site / project fields, Add room, the laser-sizes
dialog, the three project-list banners, the rename and delete results), the QUEUE (the Sync queue tab's title,
subtitle, banner and rows; the Work tab's headline), and the UPLOAD STATUS everywhere it shows — survey prep's web
line, the capture rows' notes, the queue rows' error tails: AsIsUpload's 23 rows, SyncWorker's 3, and the STORED
notes behind them — plus the :pano word sources the tree reads: FlatPresets, DeleteRoute, RoomToken, ServerMessage,
and Catalogue's data (masters.tsv's own L2 note assigns `app.no_site`'s row to "Marathi slice 2 (port M2, which
keys Catalogue.kt)"). After M2 the English left under मराठी is M6's (the laser's states, the Server sheet and the
legacy key dialog, the drawer's own rows, the ImageMeter import and annotation results, the shoot-and-keep flow)
and M8's (Face Prep's chrome, Annotate, the Laser test screen) — and the exports and the wire, decision 4's for
good.

**Derived from, in this order**

1. **The approved prototype v61c.2**, served as a COPY from the session scratchpad (m2verify: index61c2.html at the
   current header refresh plus the real capture's assets, the M3b/M3c recipe) and driven in Playwright at the Mi A3
   frame (360 × 780 and 780 × 360), switched to मराठी by its own language row. Four products decide this slice:
   - **The page's own table diffed against android/i18n**: 1,750 rows (1,335 static, 392 templates, 23 noun and
     plural rows); ZERO English diffs; 3 Marathi diffs — exactly M3a's review fix 5, the colon genitives, recorded
     finalisations, so the FOLDER wins (decision 3); 16 page-only keys, all known orphans (K1b's v59/v61b album
     keys, PL's two phase.aria keys, three album `*_x` templates of the same vintage). The card IS the page's words.
   - **The queue under मराठी, screenshotted in both orientations**: title "सिंक रांग", subtitle "1 बाकी", the banner
     "1 आयटम बाकी", section heads बाकी / पाठवले, and every row a LATIN token (LR), the ROOM TRANSLATED beside the
     DATE LATIN ("हॉल · 2026-10-10"), the client and project names LATIN (data), the pill रांगेत / पाठवले. The tree
     root: "ग्राहक · 2 ग्राहक", rows "2 साइट · 3 प्रोजेक्ट", "नवीन ग्राहक / नेटवर्कशिवाय चालते"; a site row's sub
     "फ्लॅट · Pune" — the SITE TYPE through its word, the city as typed; the bottom bar काम · ब्राउझ · रांग.
   - **116 `window.L57.vis()` probes** over the tree, queue and upload compositions, each agreeing with the folder's
     MR byte for byte wherever a whole key exists (vis() mis-composes where it must guess — M3b critic 2 — so the
     page's table stays the authority for words): "हॉल · 2026-10-11", "MB · मास्टर बेडरूम", "वेबवर ✓ 14:05 · फोटो
     नाहीत, 360 या फोनवर नाही", every `app.note.*` sentence, "साइट चे नाव बदला किंवा काढा" (the {kind} composition,
     the page's own look), "हटवले जाणार", "PROJ-0001 आणि त्यातील सर्व काही हटवा", "{ss} मधील खोल्या" with the typed
     site name AS TYPED, "Main site" → "मुख्य साइट" (the one master site name) — and the identities: "2 BHK" →
     "2 BHK" (Latin, the page's own ruling), "HTTP 503" unchanged, "4 Jan → 12 Feb" / "Jan" / "Nos" unchanged
     (dates, months and the basis stay Latin).
   - **The page's overflow probe over the tree and the queue**: zero entries — no clipped Devanagari in this scope
     on the page; the phone stays the only proof of Compose's own drawing (M3a fix 4's sentence).
2. **The shipped slices**: M3a–M3c (§25–§27 — the LocalTx pattern, say() snapshots, the Masters-pin lesson, check
   (h) and its regions, decision 4's register for logs and stored notes); M4's REPLACED / NOT_SENT rule (stored in
   English, drawn through a key, anything else drawn as stored — WishList.noteLine is the model); PL and its review
   (identity values drawn through Masters, which no literal check sees until someone looks); FW2 (ported.txt's own
   note: "TreeShell.kt itself stays out until its other sheets are" — M2 is where they are); AS1 (wireHasNoTx);
   the M3b review (a TYPE change consumed through Any is invisible to (f)/(i) — the status() change below walks
   its readers by hand).
3. **The code on batch-next** (b715006): every remaining unkeyed.tsv row of the twelve files below read in its
   file; the callers of every changing signature listed — `AsIsUpload.status` (SurveyPrepScreen:342,349,359,485,
   496,1044; its `.second` reads at SurveyPrepScreen:357 and :361 in the LaunchedEffect, which become
   `?.ok == true`; and its render, the destructuring at SurveyPrepScreen:1611 `webState?.let { (msg, good) -> … }`,
   which statusLine replaces — its `msg.startsWith("Waiting")` colour read dies with it; SyncWorker:202 and
   WorkRefile:88, both `?.second == true` today), `RoomToken.label`
   (MainActivity:472,528,2217,2904,3659,3673), `Deletes.route` / `localOnlyMessage` (MainActivity:1137,1148),
   `queueRow` / `skuQueueRow` (MainActivity:1382–1384), the Banner sites (MainActivity:1870–1874), the RenameDialog
   caller (MainActivity:1059, `what` = the wire kinds "client" / "site" / "project") — and every Tx accessor
   checked to exist in the generated Tx with its exact signature (`app_x_waiting_queue(n: Int)` folds `{pl}`,
   `app_x_6_bounded_x(done, marks: Int, read)` folds `mark{pl}`, `app_in_x(v)`, `app_x_phone(v)`,
   `app_rename_remove_x(kind)`, `app_delete_x_everything(v)`, `app_site_type_x_pick(v)`, `app_x_laser_sizes(title)`,
   `app_msg_removed_phone_x_never(name)`, `app_note_sign_again_x(why)`, `app_note_web_answered_x(code)`,
   `app_note_readings_not_sent_x(why)`, the statics `app_sync_queue`, `app_not_done_yet_tap`, `common_waiting_upload`,
   `app_on_web_x(hm)`, `app_not_on_web_x(why)`, `app_no_photos_360_not_phone`, and the card's 31 MainActivity
   phone-text keys, 18 AsIsUpload ones, 3 each for TreeShell, SyncWorker and DeleteRoute, 2 for ZoomableImage,
   1 for Catalogue).

**The keys: the card already holds all but ONE (1,797 → 1,798).** Every text this slice shows has its key — the
prototype's own tree/queue/dialog keys (`app.tag_photo_sku`, `app.elsewhere_project`, `app.in_x`, `app.none`,
`app.room_photo_not_tied`, `app.project_has_no_skus`, `app.job_type`, `app.status`, `app.erp_id`, `app.starts`,
`app.ends`, `app.photos`, `app.x_phone`, `app.not_synced_still_only`, `common.work/browse/queue/site/client/project`,
`app.rename_x`, `app.rename_remove_x`, `app.name`, `app.x_server_so_new`, `app.x_has_not_synced`,
`app.client_office_s_customer`, `app.about_delete`, `app.deleting_removes_erp_everyone`, `app.delete_x`,
`app.delete_x_everything`, `app.delete_this_project/site`, `common.working/saving/save/cancel`, `app.msg.renamed_x`,
`app.msg.x_was_only_phone`, `app.sync_queue`, `app.x_waiting_queue`, `app.nothing_waiting`, `app.waiting_2`,
`app.sent_2`, `app.x_waiting`, `app.wi_fi_only`, `noun.item` (whose `pl_item` composes the queue banner's
"1 आयटम"), `common.new_site`, `app.new_room/site_name/room_name/site_type/
address/adding/use_these_rooms/use_site/rooms_x/x_x_rooms/x_has_photos/fewer_details/more_details_type_address/
flat_building_street_area/works_offline_when_phone/site_type_x_pick/sku_codes_will_read/sku_code_prefix_appears/ok`,
`common.add`, `common.show_all`, `room.home_theatre` as the Add-room placeholder, `app.aria.search`,
`app.aria.settings`) and the card's phone-text keys for everything the prototype never drew (the `app.note.*`
stored-note family, `app.on_web_x`, `app.not_on_web_x`, `app.no_photos_360_not_phone`, the capture-row and dialog
texts named under files below, `app.pinch_zoom_double_tap`, `app.x_double_tap_fit`, `app.no_site`). The ONE new key,
decision 6 (flag `phone-text`, where-list TreeShell.kt at this commit):

- `app.aria.back` · EN `Back` · MR `मागे` — the top bar's back arrow (TreeShell.kt:140). `surface.back.short` has
  the same EN, but it is the back WALL's tab name: reusing it would tie a navigation control to a surface word,
  two meanings on one key (the two-answers fault from the other side).

Tx IS regenerated (`nice -n 15 gradle :pano:i18nGen --offline`, then `nice -n 15 gradle :pano:i18nRatchet
--offline`), never hand-edited — and the regen is NOT a no-op on Masters.kt (the M3c critic's finding 3):
**masters.tsv gains ONE row, `site_name<TAB>app.no_site`**, so `Masters.site_name` carries "(no site)" → "(साइट
नाही)" beside "Main site" → "मुख्य साइट", exactly as the L2 note promised for M2 (`app.no_client` stays M6's, with
S5Session).

**The stored notes stay English, mechanised in :pano (the M4 rule).** AsIsUpload keeps each capture's state in
asisupload/<id>.json with an English `error` note, SyncWorker stamps CaptureStore rows the same way and COMPARES
the stamp back by equality (`c.error == notPhoto`), and old files on the phone already hold these bytes — so the
constants cannot translate and cannot move a byte. They move HOME instead: **pano/UploadNote.kt (NEW, no shown
literal)** holds every stored-note constant (the sign-in, session, memory, prep-missing, capture-gone, waiting and
`web answered ` / `readings not sent: ` prefixes from AsIsUpload; `on this phone only - …` ×2 and `pano file
missing on device` from SyncWorker), each marked `// text:value a stored note, shown through tx.app_note_*` — the
WishList.kt:111 idiom byte for byte — plus the TWO mappers:

- `line(tx, note)`: the M4 noteLine idiom — an exact match renders its key; `sign in again (menu → Server) - {why}`
  and `readings not sent: {why}` match by prefix and render their key with the TAIL run back through `line` (so a
  nested `the web refused the saved password` or `web answered 401: …` translates too); `web answered {code}` keeps
  S5's own `: {clean}` tail as given (data); ANY OTHER NOTE IS DRAWN AS STORED — a bench exception's words,
  ServerMessage's output, the `"refused"` fallback — never guessed at (M4: "Any other stored note is drawn as
  stored").
- `statusLine(tx, s)`: the web line itself — `app.on_web_x(hm)` + the `app.no_photos_360_not_phone` fragment + a
  note tail through `line`; `app.not_on_web_x(line(tx, note))` — and a BLANK or absent note on an error or refused
  state renders `app.note.upload_failed`, the render default "upload failed" of AsIsUpload:180 (a state whose file
  carries no note, not a stored note; the M2 critic's second round); `common.waiting_upload` + the tail. For this,
  `AsIsUpload.status(context, id)` stops returning `Pair<String, Boolean>` and returns a small
  `Status(state, hm, note, image, ok)` — the LINE is rendered at DRAW time with the live tx (M3b's readingsMsg
  rule: a long-lived message is drawn in the language on when it is drawn), and the colour reads `ok`, never a
  string. Every `.second` / `.first` reader is walked by hand (listed above) because (f)/(i) cannot see a type
  change — but unlike the M3b salvage-log case the compiler refuses `.second` on the new type, so a missed one is
  a red CI compile, not a silent lambda print.
- **The push path's conflict note closes the M3b loose end** (`ReadingsSync.push`'s "composes Tx.EN until M2"): the
  STORED note stays `Readings.conflictText(Tx.EN, conflicts)` — storage never sees Marathi — and
  **`Readings.conflictNoteLine(tx, note)` (beside conflictText, same file)** re-renders it at draw time: split on
  "; ", each segment matched against conflictText's own two EN shapes (`{code} changed on the web`, `… by {by}`)
  and rendered through the same keys, `{by}` as given; a segment that matches neither is drawn as stored. Owner and
  parser are ONE file, pinned by a round trip: `conflictNoteLine(tx, conflictText(Tx.EN, c)) ==
  conflictText(tx, c)` for every shape, under both Tx. The ReadingsSync comment is reworded to "stored English for
  good, re-rendered by Readings.conflictNoteLine" — a note describing a state has to move when the state does.

AsIsUpload's doWork stays an (h) region with ZERO Tx tokens — the constants it writes are UploadNote's, not Tx's —
and `status`/`statusLine` sit outside the region, which is `fun doWork` alone.

**Files and changes** (unkeyed.tsv rows before → after; 614 → 347, 267 rows off)

**pano/:**
- **UploadNote.kt (NEW, no shown literal, joins ported.txt).** Above.
- **FlatPresets.kt (31 → 0, joins ported.txt).** Every row marked `// text:value`: TYPES are the stored site-type
  values (`localSiteType`, ERP's Site rows) and the room lists are the bench's room master — the WIRE, Album.WORKS'
  idiom (K1b). The SHOWN words go through `Masters.site` (the type chips, a site row's sub) and `Masters.room`
  (the Add-site room ticks) at the dialogs — one stored value, one shown word, no second answer.
- **DeleteRoute.kt (5 → 0, joins ported.txt).** `Deletes.route(tx, kind, serverId, serverKnown)` and
  `localOnlyMessage(tx, what)` take the live tx and render WHOLE KEYS PER KIND, as the card where-notes them (a
  gendered हा/ही cannot share one template): the client refusal `app.client_office_s_customer`, the blank-id
  refusals `app.cant_identify_project` / `app.cant_identify_site` (a client never reaches that branch — the first
  `if` returns), and `app_msg_removed_phone_x_never(Masters.site_name(tx, what))` — the fill through the site-name
  master, so "Main site" reads मुख्य साइट and a typed name passes through, exactly what the served page showed.
  DeleteRouteTest takes `Tx.EN` at the new first parameter; a Marathi pin per branch joins it.
- **RoomToken.kt (1 → 0, joins ported.txt).** `label(tx, room)` = the token + " · " + `Masters.room(tx, room)`
  (the glyph a value; the no-token fallback is `Masters.room` alone) — "LR · हॉल", the page's own row. `of`,
  `initials` and `folder` are UNTOUCHED: they name folders and SKU prefixes, the wire. RoomTokenTest takes Tx.EN;
  a हॉल pin joins it.
- **ServerMessage.kt (2 → 0, joins ported.txt, ZERO Tx tokens).** Both rows marked `// text:value`: `humanise`'s
  output is STORED (SyncWorker's `e.message` lands in CaptureStore rows) and is Frappe's own sentence — a
  diagnostic other parties read, decision 4's register, the SurveyPrepStore salvage-log precedent — and the served
  page draws "HTTP 503" UNCHANGED under मराठी, so the fallback is Latin on the approved screen too. The parser of
  the wire stays Tx-free, like AsIsPath.

**app/:**
- **TreeShell.kt (47 → 0, joins ported.txt — FW2's ported.txt note closed).** The drawer header ("Not signed in" →
  `app.not_signed_in`, slice 1b's wait ended); the top bar's arias (`app.aria.back` — the new key — `app.aria.settings`,
  `app.aria.search`); the SKU sheet (`app.tag_photo_sku`, `app.project_has_no_skus`,
  `app_in_x(RoomToken.of(room))` — the token stays Latin inside the fill, and SheetGroup's `.uppercase()` is a
  Devanagari no-op — `app.elsewhere_project`, `app.none`, `app.room_photo_not_tied`; a SKU line's article and room
  through `Masters.article` / `Masters.room`); the project sheet (the eight labels through their keys, the values:
  `app.not_synced_still_only`, `app_x_phone("$photos")`, dates and ids as data, the site through
  `Masters.site_name`, the job type through `Masters.job`, the Status value rendered AS THE PAGE RENDERS IT
  (index61c2.html:7915): "Open" through `app.status_open` — the card's own key for this one value, चालू — and any
  OTHER stored status drawn as stored, ERP's own word (critic note 6, corrected in the second round) —
  and "—" a value); the bottom bar (`common.work/browse/queue`; the badge count a figure); the rename-and-delete
  dialog — the {kind} word is `noun.client` / `noun.site` / `noun.project` (EN byte-identical lowercase), the title
  `app_rename_remove_x` / `app_rename_x`, the field label the CAPITALISED kind (`common.*`) + `app.name` (EN
  "Client" + " name" byte for byte; MR "ग्राहक नाव", the page's own vis answer), the bodies `app.x_server_so_new` /
  `app.x_has_not_synced`, the client note `app.client_office_s_customer`, `app.about_delete`, the blocker default
  `app.deleting_removes_erp_everyone`, the buttons `common.working`, `app_delete_x_everything(id)`,
  `app_delete_x(id)` for a bare server id, `app.delete_this_project` / `app.delete_this_site` for none (whole keys
  per kind, the card's own where-note), `common.saving` / `common.save` / `common.cancel`. The glyphs ("—",
  "— none —"'s dashes ride the key, "/" in the FOV line, " · ") marked `// text:value`.
- **AsIsUpload.kt (23 → 0, joins ported.txt).** The stored notes move to UploadNote (values unchanged, BYTE FOR
  BYTE — old state files must keep reading); `status` returns `Status` and the line renders in :pano (above); the
  `?: "refused"` fallback marked `// text:value a stored note` (HttpError always carries a message, so it is
  unreachable in practice; if ever stored it draws as stored).
- **SyncWorker.kt (3 → 0, joins ported.txt).** `noPrep` / `notPhoto` / the pano-missing stamp become UploadNote's
  constants — the equality compares keep working because the BYTES are the constants' — and nothing else changes:
  the erpLeg's `e.message` stamps stay the system's own words, drawn as stored by `line`.
- **Catalogue.kt (55 → 0, joins ported.txt).** `dateRange(tx)` and `statusLabel(tx)` are already keyed (the PL
  review); the 55 rows are DATA — except the MONTHS, which the card KEYS (`date.month.1`–`12`, MR deliberately
  Latin): `day(tx, iso)` renders them through those accessors (the second round — marking them as values would
  leave two homes for one text, and an MR edit to `date.month.*` would move the prototype and not the phone), so
  "4 Jan → 12 Feb" still draws UNCHANGED under मराठी because the KEYS say so, not because a second list here does.
  The rest: the DEFAULT room list, site types and job types
  marked (stored values; shown through `Masters.room` / `Masters.site` / `Masters.job` where they surface),
  `DEFAULT_SITE` "Main site" and `NO_SITE` "(no site)" marked and SHOWN through `Masters.site_name` (the new
  masters.tsv row), "Nos" marked (the SKU basis, the wire). The site-NAME sites the tree draws raw today —
  BrowseScreens' site rows and crumbs, the project sheet's Site row, HomeScreens' client · site line, the search
  hits, the dialog fills — thread `Masters.site_name` (the PL review's lesson: an identity drawn raw is invisible
  to every literal check).
- **HomeScreens.kt (3 → 0, joins ported.txt).** The resume card's "${token} · ${room}" goes through
  `RoomToken.label(tx, room)`; "${r.client} · ${r.site}" takes the site through `Masters.site_name`; the
  client / site / project line marked `// text:value` where only names and glyphs remain.
- **ZoomableImage.kt (2 → 0, joins ported.txt).** The face viewer's hints: `app.pinch_zoom_double_tap`,
  `app_x_double_tap_fit(zoom)` — the card where-notes both.
- **MainActivity.kt (146 → 51, stays OUT of ported.txt — the FacePrep idiom).** Keyed, by cluster: THE QUEUE — the
  tab title `common.work` / `app.sync_queue`, the subtitle `app_x_waiting_queue(unsent)` (removes "$unsent waiting"
  and the bare "s" fold), `queueRow(tx, c)` with the room through `Masters.room` and the error tail through
  `UploadNote.line` (the " — " and " / " joins values), `skuQueueRow`'s code · qty · basis marked (codes, figures,
  the wire's "Nos"); THE BANNERS — `app.sign_erp_email_password`, `app_sign_again_menu_server_x(why)`,
  `app.waiting_first_master_list`; RENAME AND DELETE — `app_msg_could_not_rename_x`, `app_msg_renamed_x`,
  the local-only toast through `Deletes.localOnlyMessage(tx, …)` (`app_msg_removed_phone_x_never`, the fill through
  `Masters.site_name` — NOT `app_msg_x_was_only_phone`, which has no phone site at all; the second round's
  correction of this list), `app.no_server_configured`,
  `app.server_refused_no_reason`; ADD SITE / ADD ROOM / NEW CLIENT — `common.new_site`, `app.site_name`,
  `app.site_type`, `app_site_type_x_pick(Masters.site(tx, v))`, the type chips through `Masters.site`, "$n BHK"
  marked (`// text:value` — digits and a Latin abbreviation, the page's own "2 BHK"), `app.address`,
  `app.flat_building_street_area`, `app.more_details_type_address`, `app.fewer_details`,
  `app.works_offline_when_phone`, the job chips through `Masters.job`, `app.use_site` ("Use this site"),
  `common.client` /
  `common.project` field labels, `app.sku_codes_will_read` / `app.sku_code_prefix_appears`, `app.new_room`,
  `app.room_name`, `room.home_theatre` as the placeholder, `app_rooms_x(ss)` (the typed name as typed),
  `app_x_x_rooms`, `app_x_has_photos`, `common.show_all`, `app.use_these_rooms`, `common.add` / `app.adding`,
  `app.sign_first_menu_server`, `app.could_not_add_room`; THE CAPTURE ROWS AND THEIR DIALOGS —
  `app.not_done_yet_tap`, `app_x_6_bounded_x` (ONE key replaces the two composed fragments),
  `app_x_6_surfaces_bounded`, `app.not_started_tap_start`, `app.360_not_phone_removed`, `app.360_no_longer_phone`,
  `app.360_photo_capture_was` (replaces its three fragments), `app.survey_prep_can_t`, `app.opening_360`,
  `common.survey_prep` and `common.cancel` at the explainer dialog, `app.ok`, `app_x_laser_sizes(title)`,
  `app.height_floor_ceiling`, `app.length_front_back`, `app.width_left_right`, `app.survey_prep_squares_every`;
  THE CAPTURE SCREEN — `app.msg.capture_deleted_sku`, `app_msg_deleted_here_bench_not_x` (replaces "Deleted here.
  The bench copy " + "did not go: "), `app_msg_could_not_open_360_x`, `app_msg_could_not_refile_x`,
  `app_msg_survey_prep_saved_not_written_x`, the FAB's `common.capture_360`, the face viewer's id · date sub-line
  marked (data); THE SEARCH OVERLAY — the hit type words (`common.client`, `noun.site` after the glyph, the job
  type through `Masters.job`, rooms and the SKU article through `Masters.room` / `Masters.article`, the site through
  `Masters.site_name`), the SITE / PRJ / SKU badges and the MB / KIT / Kothrud recents marked `// text:value` (the
  badges are the prototype's own raw literals, the PL ruling; the recents are demo data — the card's `demo.kothrud`
  row is `skipped` and generates NO accessor, so "Kothrud" cannot render through a key); AND THE VALUE MARKS — "work" (the tab
  id, wire), "ERROR" (a stored CaptureStore state, compared, never shown — the pill renders `app.retrying`),
  "MCFT Interior Estimate" (the product name, both sites), "KB" / "MB" / "%.1f MB" (units beside `app.empty`),
  " ", " / ", " · ", ".", "?", "Flat" (the stored default). THE 51 ROWS THAT STAY are M6's screens: the Server
  sheet and the legacy key dialog (~20 rows: ERP email, Password, Sign in, Site URL, API key/secret and their
  helpers, Signed in as, Signing in…, Close), the drawer's own rows (Server, not set, Sign out, Update failed,
  ERP key refused — tap to sign in to S5), the ImageMeter import and annotation results (~19 rows: the gallery
  scan, faces sent, permissions), and the shoot-and-keep flow (Could not save / split, No camera app answered,
  X3 capture failed, Discard 360) — plus the shared atoms whose only unkeyed sites remain on those screens
  ("Save", "Cancel", "Close", "Survey prep", "face" / "faces"): a row leaves only when EVERY site in the file is
  keyed, and keying one word inside an otherwise-English sheet would put a mixed screen on the phone that the
  prototype never drew (the M3b Face Prep rule).

**What translates and what stays Latin** (each proven by the served page's screenshots and `vis()`):

| shown | under मराठी | mechanism |
|---|---|---|
| the queue row "Living Room · 2026-10-10" | हॉल · 2026-10-10 | `Masters.room` + the date a value |
| the room tokens LR / MB / GB, ERP ids, times "14:05" | UNCHANGED | codes, ids, figures — identities |
| client, site, project NAMES; typed room names; the city | UNCHANGED (as typed) | data; `Masters.*` passes unknowns through |
| "Main site" / "(no site)" | मुख्य साइट / (साइट नाही) | `Masters.site_name` (+ the new masters.tsv row) |
| site types, job types on rows and chips | फ्लॅट, ऑफिस… / नवीन काम, दुरुस्ती… | `Masters.site` / `Masters.job` |
| "On web ✓ 14:05 · no photos, …" | वेबवर ✓ 14:05 · फोटो नाहीत, … | `statusLine` over `app.on_web_x` + fragment |
| the stored notes ("waiting for the project to reach ERP", …) | their `app.note.*` Marathi | stored English, `UploadNote.line` |
| S5's and the bench's own words after a keyed head | UNCHANGED (as given) | data — the M4 reasonLine rule |
| "2 BHK", "Nos", "HTTP 503" | UNCHANGED | the page's own ruling — values |
| "4 Jan → 12 Feb" | UNCHANGED | `date.month.*`, whose MR is deliberately Latin — the keys, not a second list |
| "Rename or remove site", "Client name" | साइट चे नाव बदला किंवा काढा, ग्राहक नाव | `{kind}` = `noun.*`, `app.name` |

**S5 must already serve**

Nothing.

**Tests and checks**

- **UploadNoteTest (new, :pano)**: `statusLine(Tx.EN, …)` BYTE-EQUAL to today's `status()` strings for every state
  (done; done with the no-photos fragment; done with a readings note; error; refused; ERROR WITH NO STORED NOTE —
  the `app.note.upload_failed` default, the second round's addition; waiting with and without a note); under
  `Tx.MR` the card's values; `line` on every constant, on the two nested prefixes, on `web answered
  401: Not staff` keeping S5's tail, and on an unknown note UNCHANGED; the constants' bytes pinned against the
  values 0.3.299 stored (a moved constant that changed a byte orphans every state file on the phone).
- **ReadingsTest grows the round trip**: `conflictNoteLine(tx, conflictText(Tx.EN, c)) == conflictText(tx, c)` for
  the with-by, without-by and mixed shapes, under both Tx; an alien note unchanged.
- **TreeQueueMarathiTest (new, :pano — the M3a fix-7 reason: Masters passes unknowns through, so a wrong map leaks
  Latin only under मराठी)**: hand-typed Marathi pins against the strings the served page answered —
  `RoomToken.label` ("LR · हॉल"), `Deletes` per branch (ओळखता येत नाही…, ग्राहक हा ऑफिसचा…, या फोनवरून काढले —
  मुख्य साइट…), the site-name master both ways (मुख्य साइट; a typed name through unchanged), a site type and a job
  type, the {kind} compositions (साइट चे नाव बदला किंवा काढा; प्रोजेक्ट नाव), and the project sheet's "Open" → चालू
  (`app.status_open`, the second round's finding 1).
- Existing :pano tests take `Tx.EN` at new first parameters where signatures reach them: DeleteRouteTest,
  RoomTokenTest, FlatPresetsTest (no signature moves — data only), ServerMessageTest (unchanged, Tx-free).
- **The i18n suite**: I18nTableTest (1,798 keys; `app.aria.back` flagged `phone-text` with its where-list),
  TxRenderTest (the new static renders), MastersTest (`site_name` keeps its place in the MAPS walk; the new row's
  EN is the stored value byte for byte), I18nLiteralTest (a)–(d) over the 267 removed rows and the 12 files joining
  ported.txt, (f)/(i) over the renamed and moved members — `UploadNote`'s constants are read from :app, so (i)
  resolves them by reflection — **and (h) UNCHANGED**: AsIsUpload's `fun doWork` region still carries ZERO Tx
  mentions (the notes it writes are UploadNote constants), and no region is added, because ServerMessage and
  AsIsPath stay Tx-free by construction and their tests pin it.
- The deny-pattern check over the tables: the one new key carries no name, figure or number.
- The CI gate; `:app` compiles only in CI (push batch-next, wait for android-app.yml) — nothing here has been seen
  on a phone. The status() type change's readers are walked against the files AS THEY ARE on batch-next before the
  commit (the list in "Derived from" 3).

**What Amit sees**

Under English, nothing: every key's EN renders the current literal byte for byte — the uppercased sheet-group and
drawer-group heads, the lowercase {kind} fills and the folded plurals included — and the stored state files on the
phone keep reading, because no stored byte moves. Under मराठी the TREE, its dialogs, the QUEUE and every upload
status line turn Marathi in one build — the drawer header, the sheets, rename-and-delete, Add site and Add room,
the queue tab whole, survey prep's web line and the capture rows' notes — exactly the served page's screens, both
orientations, with names, codes, dates and S5's own words staying as they are.

**Risks**

The clipped-Devanagari class (the phone is the only proof; the page's overflow probe found nothing in this scope,
and the tree's and queue's rows are Material ListItems with room, so no Marathi-only line height is pre-emptively
added — if Amit's phone shows a cut, the M3a `if (tx.mr)` remedy is one line per element). A stored note written
by an older build whose bytes do not match any constant draws as stored — English — which is the rule working, not
a defect. A language flip mid-upload re-renders the web line at next draw (`statusLine` runs at draw time); a say()
raised before the flip keeps its language for 2.6 s (M3a's accepted rule).

**Deliberately NOT in M2**

- **M6** — DistoClient's 31 rows + Disto's 2 (the laser's states); S5Session's 10 and the Server sheet, the legacy
  key dialog and the drawer's own rows in MainActivity; FrappeClient's 1 (`app.download_http_x`, the update flow);
  AnnotationPush's 2 and the ImageMeter import and annotation results; the shoot-and-keep flow (Discard 360 has no
  key yet — M6 adds it); X3Bridge's 11 (the camera's own notes).
- **M8** — FacePrepScreen's 122, FacePrep's 17, AnnotateScreen's 46, Annotation's 17, the Laser test screen's 24.
- **The exported files' English is not a gap** (decision 4): FaceWriter's 13 rows stay, as does everything (h)
  pins.
- **The wire**: AsIsPath's record body, the capture states ("waiting" / "done" / "error" / "refused", SYNCED /
  SYNCING / ERROR / LOCAL), room names SENT to S5, folder names, SKU codes — never translated, marked where
  LiteralScan sees them.

**The M2 critic** — raised by a careful reader, answered:

1. *Why not store the notes in the live language and skip the mapper?* Storage never sees Marathi (M4's rule, both
   its reasons here): a note stored in the flip's language shows the WRONG language after the next flip — a second
   answer — and SyncWorker compares `c.error` by equality, so a translated store would break the compare and
   re-stamp every row on every pass; and old asisupload/<id>.json files hold the English bytes already.
2. *Why a new :pano UploadNote instead of mapping inside AsIsUpload?* The conflictText and PlanSave lesson (M3b
   finding 4, M3c critic 2): a words-home in :app cannot be pinned where the suite runs — and TWO readers share it
   (survey prep's web line and the queue's rows), so the mapper exists once or the two drift.
3. *The S5 tails stay English inside a Marathi sentence ("वेबचे उत्तर 503: Not staff").* Yes — S5's words are data,
   the M4 reasonLine rule; translating them would be inventing S5's voice. The keyed head says what happened; the
   tail says what S5 said.
4. *`room.*` keys exist — why do FlatPresets' and Catalogue's lists get marks instead?* The lists are STORED values
   (the room master, `localSiteType`, ERP's job types) — the wire — and the SHOWN word already goes through
   Masters at the dialog. Keying the list itself would put the translation into storage, which is answer 1 again.
   Album.WORKS set the idiom (K1b): identity values marked, drawn through Masters.
5. *`app.x_server_so_new`'s MR opens "हा {kind}…", which misgenders साइट ("हा साइट…").* The card's MR is what the
   approved page itself composes with the same key and the same `noun.site` fill, so the phone MATCHES the approved
   screen — decision 3 and the prototype's authority outrank the grammar. The same substitution class ships in
   `app.rename_x` / `app.rename_remove_x` ("ग्राहक चे नाव बदला" wants the oblique ग्राहकाचे joined, "साइट चे" reads
   split the same way) and in the `common.* + app.name` field label — all listed for Amit's phone test together
   (the second round). If he reads any as wrong, the fix is whole keys per kind (the DeleteRoute idiom), one card
   edit each — not an unapproved rewording here.
6. *The project sheet's Status value ("Open") stays English?* WRONG as first written, caught by the second-round
   critic: the page itself renders the one value "Open" through `app.status_open` (index61c2.html:7915, inside
   projectSheet) — the card made that key for exactly this site (EN "Open", MR "चालू") and it had ZERO readers in
   the code. So the phone renders it the same way: `project.status == "Open" -> tx.app_status_open`, blank "—",
   local `app.not_synced_still_only` — and only a status OTHER than Open is drawn as stored (that part of the first
   answer stands: inventing a map for Completed/Cancelled here would translate values the page does not).
   TreeQueueMarathiTest pins "Open" → चालू.
7. *status() changes type — the M3b salvage-log fault?* Walked, not assumed: every `.first` / `.second` reader is
   listed in "Derived from" 3 and changed in this slice; and the failure mode differs — the compiler REFUSES
   `.second` on `Status`, where the Log.w case compiled through `Any` — so a missed reader is a red CI build.
8. *One keyed word on the Server sheet would empty the "Cancel" row — why leave it?* Because the sheet would then
   show one Marathi word among English fields: a mixed screen the prototype never drew (the M3b Face Prep rule).
   The row stays listed — the worklist only shrinks, and M6 removes it with the sheet.
9. *The derivation lives in the scratchpad — the lost-plan fault again?* No (M3b critic 10's answer holds): the
   diffs, the vis() rulings, the screenshot findings and the per-file mapping are IN THIS SECTION, committed with
   it; the run is re-creatable in minutes from the published artifact and this recipe. No client name, phone
   number or rate enters the repo: the fills quoted here are the demo rows' and placeholders, and the deny-pattern
   check runs over the tables as always.
10. *"ERROR" and "work" marked as values — but they are shown?* Neither is: "work" is the tab id the `when`
    compares, and "ERROR" is CaptureStore's stored state, compared in `queueRow` — the pill the person sees renders
    `app.retrying`. A mark says WHY a literal is exempt; these are the wire's.

**The M2 critic, second round (9 Nov 2026)** — seven findings on §24 itself, each checked against the source before
building; ALL SEVEN CONFIRMED and applied, and the section above is edited where each one named it:

1. *(major)* Critic 6 contradicted the approved prototype: `projectSheet` (index61c2.html:7915) renders the Status
   value "Open" through `app.status_open` — the key exists on the card (EN "Open", MR "चालू", made for exactly this
   site) with a generated accessor and ZERO readers, so both of critic 6's factual claims (no vocabulary key; a map
   would have to be invented) were false. Confirmed by reading the page's live JS and the card. Applied: the
   TreeShell bullet renders Open through the key (blank "—", local `app.not_synced_still_only`, any other status as
   stored — that part of critic 6 stands), critic 6 is rewritten, and TreeQueueMarathiTest pins "Open" → चालू.
2. `demo.kothrud` was listed among usable keys while index.tsv marks it `skipped` (no accessor): following the list
   as written was an unresolved-reference compile break in CI. Confirmed (index.tsv:614; no `demo_` member on Tx).
   Applied: "Kothrud" is marked `// text:value` with MB / KIT, and demo.kothrud left the keys list.
3. The status() reader walk missed SurveyPrepScreen:357 and :361 (`.second` in the LaunchedEffect) and blurred the
   render reader (:1611's destructuring). Confirmed by reading the file. Applied: both named in "Derived from" 3 —
   and :1611's `msg.startsWith("Waiting")` colour read, which would have broken silently under मराठी, dies with the
   destructuring (the colour reads `st.ok` and the stored `state` now).
4. The plan marked Catalogue's MONTHS as values while the card KEYS them (`date.month.1`–`12`, MR deliberately
   Latin) — two homes for one text. Confirmed. Applied the first remedy: `day(tx, iso)` renders through the
   accessors, the private MONTHS list is deleted. NOTE: `QuoteStage`'s own MONTHS stay the PL ruling (marked
   value), and rightly — its list is the prototype's en-GB `toLocaleDateString` vocabulary, which says "Sept"
   where `date.month.9` says "Sep", so it CANNOT read these keys without changing approved screens.
5. `pl_item` written as a key when it is `noun.item`'s plural accessor. Confirmed (index.tsv:899). Applied in the
   keys list.
6. Critic 5's whole-keys-per-kind remedy was scoped to `app.x_server_so_new` alone while `app.rename_x` /
   `app.rename_remove_x` ship the same {kind}-substitution class ("ग्राहक चे…"). Confirmed (both MR values open
   "{kind} चे"). Applied: critic 5 now lists all of them for Amit's phone test.
7. The UploadNote constants list omitted the "upload failed" render default (AsIsUpload:180, unkeyed row 96) —
   a no-note error/refused state would have slipped through unpinned. Confirmed. Applied: statusLine renders a
   blank/absent note on error/refused as `app.note.upload_failed`, and UploadNoteTest pins that case byte-equal.

And one error the build itself found, recorded the same way: the RENAME AND DELETE cluster named
`app_msg_x_was_only_phone(Masters.site_name(tx, name))`, but that accessor has NO caller and its key no phone
site — the local-only toast is `Deletes.localOnlyMessage`, which renders `app_msg_removed_phone_x_never`; the
cluster line is corrected above. Also recorded, a hair beyond the letter of the files list: HomeScreens' recent
row draws its ROOM name through `Masters.room` (the bullet named only the resume card and the site), because a
room name is an identity value everywhere else on the tree and the queue row beside it already translates its room.

**unkeyed.tsv** 614 → 347 (267 rows off) · **ported.txt** + UploadNote.kt, FlatPresets.kt, DeleteRoute.kt,
RoomToken.kt, ServerMessage.kt (pano), TreeShell.kt, AsIsUpload.kt, SyncWorker.kt, Catalogue.kt, HomeScreens.kt,
ZoomableImage.kt (app) — MainActivity.kt stays out (146 → 51, M6's rows) · **keys** 1,797 → 1,798 (`app.aria.back`;
Tx regenerated, Masters.kt regenerated — `site_name` gains `app.no_site`, one masters.tsv row) · **literal-counts**
re-ratcheted over every touched file.

### 25. M3a · Marathi: survey prep chrome (design slice 3a) — SHIPPED 3 Nov 2026 (0.3.280)

**Size:** M · **Depends on:** L2

**Goal**

Key survey prep's tabs, steps, palette, mark-type names, More ▸ and the Mark sheet.

**Files and changes**

**app/:**
- SurveyPrepScreen.kt (116 rows);
- SurveyPrepTools.kt (6). Mark-type names stay the English `when` keys; only the labels are looked up;
- SurveyPrepLocks.kt (4).

**pano/:**
- SurveyPrep.kt (60), with TYPES and NAME through Masters;
- SurveyPrepPalette.kt (20);
- SurveyPrepEdit.kt (31), with refusal(tx) and deleteLabel(tx);
- StepRoute.kt (7);
- SurveyGuide's lockedEnd.

**Card keys:** the card's 37 survey-prep keys and its survey-prep near-match pairs.

**Line heights.** Four Marathi-only line heights (design-marathi §4):
- face tabs and reading rows, 17 sp;
- More ▸ row labels and tile names, 14 sp.

**S5 must already serve**

None.

**Tests and checks**

- SurveyPrepTest, PaletteTest, LockTest, ClearTest, StartOverTest, StepRouteTest and GuideTest keep their English literals, with Tx.EN added.
- The CI gate.

**What Amit sees**

Only the decision-5 English changes.

**Risks**

A clipped Devanagari line, which shows only after VIS.

*(Built as 8af7b32 + the review fix 8fde7e2: 245 rows off unkeyed.tsv, 1,171 → 926; the builder found GuideTest pins no text, three M3b files touched by signature changes, the fourth line height has no phone element, Measure.what became a Msg; the review fixed THIS 360 / UNDO case, the Laser attribution note, a 27 dp tab row under मराठी in landscape, three genitives, a deleted LINES_REFUSAL's second reader. See android/i18n/README.md "Port 61c slice M3a".)*

### 26. M3b · Marathi: measures, readings and the words drawn on the photo (design slice 3b)

**Size:** M · **Depends on:** M3a

**Goal**

Key the MEASURES AND READINGS half of survey prep — everything M3a's chrome boundary left English: a Tape's, a
Line's and a chain's chips, labels, list rows and refusals (SurveyMeas, SurveyChain), the reference's words and the
Size from row (SurveyRef), the opened file's own words and the salvage notes (SurveyPrepFile), the readings'
conflict texts and not-yet line (Readings, ReadingsSync), the 360 caption and its placement refusals (Pano360), the
360 view's fit / grid buttons (SurveyPrep360) — AND the words DRAWN ON THE PHOTO by both canvas painters
(SurveyPrepDraw, SurveyPrep360's own copy, FacePrepDraw): the four boundary-line names, the reference's "Ref 905 ×
2053" tag, the chain total's " laser" word, Face Prep's step / detail names and guide labels. After M3b the only
English left on survey prep under मराठी is the laser's connection states (DistoClient, M6) and the upload status
line (AsIsUpload, M2).

**Derived from, in this order**

1. **The approved prototype v61c.2**, served as a COPY from the session scratchpad and driven in Playwright at the
   Mi A3 frame (360 × 780 and 780 × 360), switched to मराठी by its own language row (drawer → भाषा · Language,
   `data-l57="lang"`). Three products of that run decide this slice:
   - **The raw Marathi table in the page** (`const RAW=` inside its L57 layer, 1,750 key rows) extracted and diffed
     against android/i18n: ZERO English diffs; 3 Marathi diffs, exactly M3a's review fix 5 (the `{s}:` colon
     genitives of `refuse.sp.x_reference_locked_unlock`, `sp.x_lines_locked`, `sp.x_lines_unlocked`); 16 keys the
     page still has that the folder deleted (K1b's 14 ORPHAN v59 album keys, PL's 2 phase.aria keys). So the page
     embeds the folder's pair at its build commit, and where they differ the FOLDER wins (decision 3: android/i18n
     is the one home; the diffs are its recorded finalisations). The page's visible card tables differ from the
     folder on 9 rows more — the L2 plural fix (गोष्टी → ठिकाणी), the L2 मीटर and ब्लूटूथ fixes, the M3a genitive
     drops in five `sp.file.x_*` keys and `sp.x_its_list` — all recorded finalisations; same ruling.
   - **The page's own text layer probed string by string** (`window.L57.vis(…)` over 113 composed strings — the
     layer every DOM text AND every canvas word goes through): what translates, what stays Latin, and where the
     layer's generic matcher mis-composes (below, "The drawn words" and critic note 2).
   - **Screenshots under मराठी**: the tape pulled on the real capture's front wall (chip "टेप · बाजूवरून ओढा ·
     लांबी फोटोवरून मिळते", caution "फक्त या पृष्ठभागावरच्या गोष्टीच बरोबर मोजल्या जातात.", selection row "F-T1
     टेप · 1430 mm", the drawn tag "F-T1 1430 mm" unchanged); a chain drawn (chip "साखळी · पृष्ठभागावर पॉइंट टॅप
     करा, संपवण्यासाठी डबल टॅप किंवा ✓", row "FK1 ≈821 · ≈715 · Σ ≈1536", reading row "▶ FK1 भाग 1"); 3 · List in
     both orientations (chips FD1 / FE1 / F-T1 1430 / FK1, rows रुंदी / उंची, स्थिती: सर्व ▾, एक्सपोर्ट).
2. **The shipped M3a** (android/i18n/README.md "Port 61c slice M3a", §25): the chrome boundary, Measure.what as a
   Msg with its String constructor left "for the two checklists not keyed yet … it goes with M3b", lineNames
   rendering the edge.* keys in English "for the two readers that draw no language yet — the on-photo painter and
   the salvage notes — both M3b's", the say()-snapshot rule, Tx.EN in tests, the Masters-pin lesson (fix 7).
3. **The code on batch-next** (885c25a): every remaining unkeyed.tsv row of the files below was read in its file,
   every word function's call sites were listed (SurveyPrepScreen, SurveyPrepTools, SurveyPrepDraw, SurveyPrep360,
   SurveyPrepEdit, SurveyPrep, FaceWriter), and every Tx accessor this slice calls was checked to exist in the
   generated Tx with its exact signature (e.g. `sp_chain_x_x_part(dir, n, v: String?)` carries the room tail as
   `{v}`, `sp_chain_x_tap_points(pl: String)` takes the points tail, `sp_x_points_tail(n: Int)` builds it — the
   card's own where-notes say so).

**The keys: the card already holds all but three.** Every measures-and-readings text below has its key in
android/i18n (the card's survey-prep group and its near-match pairs, `sp.*`, `refuse.sp.*`, `refuse.v360.*`,
`refuse.lines_first`, `sp.file.*`, `edge.*`, `measure.*`, `common.*`, `life.length`, `view.*_plan.lc`, `type.*`,
`trade.electrical`), so the card is consumed, not extended — the three exceptions are Face Prep's two plate-guide
labels and its plan-guide label, screens the prototype never had (decision 6: Marathi by Claude, flag `phone-text`):

- `fp.x_wall_corrected_x` · EN `{name} · {wall} · corrected for {d} mm depth` · MR
  `{name} · {wall} · {d} mm डेप्थ धरून दुरुस्त`
- `fp.x_wall_depth_unknown` · EN `{name} · {wall} · depth unknown — draw it here to correct` · MR
  `{name} · {wall} · डेप्थ माहीत नाही — दुरुस्तीसाठी इथे काढा`
- `fp.x_plan_x_deep` · EN `{name} · {plate} · {d} mm deep` · MR `{name} · {plate} · {d} mm डेप्थ`

(depth = डेप्थ, what staff say, as every shipped `measure.*depth*` row has it; the first version of this section
wrote खोली/खोल, which reads as ROOM in this app's own register — the M3b critic's finding 1, applied before the
keys were built.) `{name}` is a stepName rendered through tx (below), `{wall}` the wall's shown name lowercased
(`Masters.surface(tx, LABEL).lowercase()` — "front wall" in English byte for byte, a no-op on Devanagari: the
cannotGo pattern M3a reviewed), `{plate}` is `view.floor_plan.lc` / `view.ceiling_plan.lc` ("floor plan" /
"फ्लोअर प्लॅन"), `{d}` a String of whole mm digits — not the Int this section first said: the generator types Int
only for a `.length`/`.size` count or a folded plural, and bare digits are a Word, passed as given (found in the
build). 1,794 → 1,797 keys; Tx IS regenerated, with the repo's generator task under nice
(`nice -n 15 gradle :pano:i18nGen --offline`), never hand-edited; `nice -n 15 gradle :pano:i18nRatchet --offline`
afterwards (it only lowers counts).

**Files and changes** (unkeyed.tsv rows before → after; 829 → 684, 145 rows off)

**pano/:**
- **SurveyMeas.kt (31 → 0, joins ported.txt).** The constants become functions taking `tx: Tx` first: OFF_REFUSAL
  and CAUTION (one English, one key: `refuse.sp.only_things_surface_measure`), OFF_CHIP
  (`refuse.sp.off_surface_nothing_will`), TAPE_TAP (`sp.msg.tape_drag_along_face`), TAPE_NOTE
  (`sp.no_reading_box_tape`), TAPE_CHIP (`sp.tape_drag_along_face`), LINE_CHIP (`sp.line_drag_marked_length`);
  `areaChip` → `sp_line_area_x_corner(n)`; `chipText(tx, tool, drag, corners)`; `dirText(tx, …)` over
  `sp.along_face_x_mm`, `sp.up_face_x_mm`, `sp.diagonal_face_x_mm`, `sp.left_right_x_x`, `sp.front_back_x_x` with
  `{w}` from `sp.on_the_floor` / `sp.on_the_ceiling`; `typeName(tx, m)` over `common.tape`,
  `common.marked_length`, `common.marked_area`; `label(tx, m)` over the suffix keys `sp.tape_suffix`,
  `sp.marked_length`, `sp.marked_area`; `listName(tx, s, m, room)` over `sp.tape_x_mm_photo` (the key carries
  " mm", the card's near-match ruling), `common.marked_length`, `sp.marked_area_x`, the room tail joined in code
  as `" · " + Masters.surface(tx, …)`; `rowValue(tx, …)` over `sp.tape_x_mm` (a Line's bare reading "1234 mm" and
  "≈1234 mm" stay composed figures — the prototype draws them unchanged); `measures(m)` labels become Msg
  (`life.length` for "length", `measure.perimeter` for "perimeter" — `vis()` proves both are what the prototype
  shows). `mmTxt`, `m2txt`, `tapeTag`, `lineTag`, `tapeTicks`, `tapeTagAt` are untouched: all-ASCII figures
  (critic note 4).
- **SurveyChain.kt (21 → 0, joins ported.txt).** `chipText(tx, points)` = `sp_chain_x_tap_points(pl)` with `pl` =
  `sp_x_points_tail(points)` from the first point, else "" (the card's where-note on `sp.x_points_tail` names
  exactly this composition); `busy(s)` returns data and the refusal is rendered at say() time as
  `refuse_sp_finish_chain_x_first(Masters.surface(tx, …).lowercase())`; NEED_TWO / NEED_DIFFERENT →
  `refuse.sp.chain_needs_two_points` / `refuse.sp.chain_needs_two_different` (as Msg on Finish.Refused, rendered by
  the caller's tx); DONE_LABEL → `sp.aria.end_chain` (reader: SurveyPrepTools); GROUP stays the identity "chains"
  (`// text:value`), drawn through SurveyPrepEdit.groupName → `sp.group.chains` as M3a wired it; `direction(tx, c)`
  → `sp.horizontal` / `sp.vertical`; `label(tx, c)` → `sp_x_chain(code)`; `listName(tx, s, c, room)` →
  `sp_chain_x_x_part(dir, n, if (room) Masters.surface(tx, …) else null)` — the key carries the room tail as
  `{v}`; `measures(c)` labels become Msg (`sp.segment` + the number joined as the prototype joins its fragment key;
  `measure.total`), and SurveyPrep.Measure's String constructor is DELETED (M3a: "it goes with M3b") — check (i)
  walks its readers; `totalText(tx, v)` keeps " laser" through `sp.laser_suffix` and keeps **" d" LATIN** (critic
  note 4); `totalTag(tx, code, v)`, `rowValue(tx, v)`. `segFigure` is untouched (reading / "{n} d" / "≈{n}" — no
  word). A chain's stored `snaps` ("left corner", "FD1 right") are FILE DATA, read by nothing on screen: untouched,
  no migration.
- **SurveyRef.kt (20 → 0, joins ported.txt).** NO_DELETE → `sp.msg.reference_stays_hide_eye`; EYE_LABEL →
  `sp.aria.show_faces`; EDGES → `sp.edges_laser`; REFERENCE → `sp.reference`; SIZE_FROM → `sp.size_from`;
  PLACE_HINT → `sp.drag_four_cyan_corners`; `shownSaid(tx, s, shown)` → `sp.reference_shown_2_mark` /
  `sp_reference_hidden_faces_still(Masters.surface(tx, …).lowercase())`; `name(tx, s)` → `sp_x_reference`;
  `menuHeader(tx, …)` (🔒 a glyph value); `menuItems(tx, locked)` → `common.lock` / `common.unlock` /
  `common.cancel`; `lockLabel(tx, s, locked)` → `sp_aria_lock_x` / `sp_aria_unlock_x` over
  `sp_the_x_reference(Masters.surface(tx, …)).lowercase()` — the card's where-note on `sp.the_x_reference` names
  this exact pairing, and the EN render stays "Lock the front wall reference" byte for byte; `label(tx, r)` →
  `sp_ref_x_x(fig(w), fig(h))` ("संदर्भ 905 × 2053" — DRAWN, see below; "?" stays a glyph); `tabText(tx, sf)`
  through it; `refText(tx, …)` → `sp_measured_x_x_x` (+ `sp_differs_x_x_mm` or the " ✓" value; the axis letters
  stay the L / W / H identities, M3a); `leftSaid` → `sp.msg.d2_x_ref_left`.
- **SurveyPrepFile.kt (21 → 0, joins ported.txt).** `Decoded.problems` becomes `List<Msg>` and every salvage note
  is its `sp.file.*` key, `{surface}` through `Masters.surface`, `{line}` through `SurveyPrep.lineName(tx, s,
  slot)` — the folder's finalised Marathi has no genitive before the name (the page diff above), so the nominative
  fill reads exactly as `vis()` renders it: "समोरील भिंत: जमिनीची रेषा वाचता आले नाही, पुन्हा ठेवले".
  `savedText(o)`, `openedNote(o)`, `pointsNote(o)` become Msg over `common.nothing_saved_yet`, `common.restored`,
  `sp.file.restored_in_part`, `sp.file.started_fresh`, `sp_file_part_unreadable_kept_x(n, first, more, file)` and
  its three siblings, `sp_file_x_points_recentred(n)`; `{first}` renders problems[0] with the same tx, `{more}`
  the " …" glyph value. The aside file NAMES (`<id>.bad-….json`) and everything stored stay byte-identical.
- **Pano360.kt (4 → 0, joins ported.txt).** `caption(tx, s)` → `sp_360_drag_look_x(Masters.surface(tx, …))`;
  `cannotGo(tx, t, to)` → `refuse_v360_x_cannot_go_x(SurveyPrep.typeName(tx, …), Masters.surface(tx,
  …).lowercase())` (the Marathi restructures to "{surfName}: इथे {n} लावता येत नाही" — the key's own shape, proven
  by `vis()`); NO_LINES → `refuse.lines_first`; `placeRefusal` threads the tx; `zoomText` ("{n}°") untouched.
- **Readings.kt (2 → 0, joins ported.txt).** `orphanText(tx, keys)` → `sp_x_changed_web_mark_deleted(code)` joined
  with "; " (a value) + `sp.deleted_web_too`.
- **FacePrep.kt (37 → 17, stays OUT of ported.txt).** Only the words FacePrepDraw DRAWS are keyed; the Measure
  sheet's Dim labels, the capitalised side names and the piece labels stay M8's rows. `stepName(tx, f, i)`: the
  short form through `type.c` ("Column" → "कॉलम"), `type.b` ("Beam" → "बीम"), `type.x` ("Other" → "इतर" — the
  split(" ")[0] of "Other step", which already has its key); `detName(tx, f, i)`: `type.d` / `type.w` / `type.o` /
  `type.l` and `trade.electrical` ("Electrical" → "इलेक्ट्रिकल", the split of "Electrical point");
  STEP_KINDS / DETAIL_TYPES / EDGE_NAMES stay English identity maps, marked `// text:value` (their stored kinds
  and the Dim keys never show); the locator's from-word resolves through the six `measure.*` keys
  (`measure.left_wall` "from left wall" → "डाव्या भिंतीपासून", `measure.from_floor`, `measure.from_ceiling`, …)
  and, for a named mark, `app_from_x(detName/stepName)` — "दरवाजा 1 पासून" — the CARD'S OWN PAIRING for
  FacePrepDraw.kt:238 ("decision 6 … the matcher's pairing: app.from_x"); the two plate-guide labels and the
  plan-guide label become the three fp.* keys above.
- **SurveyPrep.kt.** `lineNames(s)` (the Tx.EN render M3a left for the two M3b readers) is DELETED — both readers
  now take a tx — and `Measure`'s String constructor with it; no fallback stays (the NO FALLBACKS rule: a second
  way to render a line name is a second answer). `placeRefusal` threads the tx to Pano360's; `canPlace` keeps its
  signature (the rule, not its words).
- **SurveyPrepEdit.kt** (moved here from the app/ list — the M3b critic's finding 5): `menuHeaderOf` / `menuItemsOf`
  hand the reference's branch a tx (`SurveyRef.menuHeader(tx, …)`, `menuItems(tx, …)`), and `labelOf` /
  `menuHeader` thread it to `SurveyMeas.label(tx, m)` and `SurveyChain.label(tx, c)`.

**app/:**
- **SurveyPrepDraw.kt (9 → 0, joins ported.txt).** The 9 rows are glyphs and separators (" ", " | ", "+", "?",
  "±", "—", "−", "≈", "▶"): each marked `// text:value` with its why. `drawLines` takes the tx and draws
  `SurveyPrep.lineName(tx, s, k)` — the four boundary-line names are the first Devanagari this painter draws;
  `drawRef`/`drawRefSq` draw `SurveyRef.label(tx, r)`; `chain()` draws `SurveyChain.totalTag(tx, …)`. `Marks`
  gains no field: the tx is a parameter of `drawMarks` / `drawLines` / `drawRef`, so every caller states its
  language (critic note 5).
- **SurveyPrep360.kt (4 → 0, joins ported.txt).** "fit" → `sp.fit`, "grid" → `sp.grid`, "Plane grid" →
  `sp.aria.plane_grid`; "≈" a value. Its own label() draws the same pano words (lineTag at 984, tape figures at
  1017/1030, segFigure at 1083, totalTag at 1089, SurveyRef.label at 1103), so the 360 view changes through the
  one :pano source, plus its chip (`SurveyMeas.chipText`, CAUTION, `SurveyChain.chipText`) through the tx.
- **ReadingsSync.kt (4 → 0, joins ported.txt).** NOT_YET → `sp.web_readings_not_yet` behind a KIND the Msg
  carries (critic note 3); **`conflictText` MOVES INTO :pano, beside `Readings.orphanText`** (`Readings.conflictText
  (tx, conflicts)` → `sp_x_changed_web(code)` / `sp_x_changed_web_by_x(code, by)` joined with "; ", a value) — the
  M3b critic's finding 4: ReadingsSync is :app (it imports android.content.Context), so the ":pano half pins
  conflictText" this section first promised could not run there; moved, the pin is real. The PULL path (survey
  prep) renders with the screen's tx; the PUSH path's note (AsIsUpload's status line, M2's screen) composes with
  `Tx.EN` until M2 — a stored-note twin of M4's REPLACED / NOT_SENT rule, named in the code.
- **SurveyPrepScreen.kt (already in ported.txt).** Call-site changes only: the readingsMsg branches drop the
  English constants (and the `msg == ReadingsSync.NOT_YET` COLOUR comparison at 1612 becomes the kind flag —
  critic note 3). **The KIND is `Readings.noteKind(ok, notYet, clean)`, and OK means the fetch SUCCEEDED AND
  `said.isEmpty()`** — a successful pull that applied a conflict or re-deleted an orphan stays amber, which is
  today's behaviour exactly (green required a clean "✓" line, because the conflict head is prefixed to it); a kind
  mapped from the fetch outcome alone would have turned a conflict-carrying pull green in both languages — the M3b
  critic's finding 4, and the case is pinned. savedMsg takes SurveyPrepFile's Msg; every refusal from a pano
  Finish/Press/End is rendered at say() time with the live tx (say() keeps a String: M3a's rule, a note raised
  before a language flip is not redrawn).
- **SurveyPrepTools.kt** (DONE_LABEL's reader): signatures follow. (**SurveyPrepEdit.kt**, the reference's
  menuItems/menuHeader and labelOf, is :pano, not app/ — the M3b critic's finding 5, moved to the pano/ list above,
  because the grouping says which changes :pano tests run against. No row counts move: it has no unkeyed rows and
  has been in ported.txt since M3a.)
- **FacePrepDraw.kt (9 → 0, joins ported.txt).** The composed figure strings ("$a | ${len - a} mm", "$tot mm",
  "E$n ${pc.len}", "L${i + 1}", …) are marked `// text:value` (codes, digits, mm); the words go through the
  FacePrep tx functions above — **and both of its callers pass `Tx.EN`: FacePrepScreen's screen Canvas (until M8)
  and FacePrepScreen.exportAll, the ImageMeter photo writer (for good)**. The second caller is exportAll
  (FacePrepScreen.kt, `private fun exportAll`'s draw of the "-faceprep.jpg" photos), NEVER FaceWriter.rewriteSurvey,
  whose only painter call is SurveyPrepDraw.drawMarks — the M3b critic's finding 3; this section first named
  FaceWriter, the same fault S5-P's review recorded as "the callers named rightly". So Face Prep stays wholly
  English under मराठी today (critic note 1), the ImageMeter photo stays English for good (critic note 6), and
  exportAll is pinned the way check (h) pins FaceWriter: I18nLiteralTest's (h) companion refuses any Tx mention in
  exportAll but the literal `Tx.EN` and requires its draw to pass it — so M8's flip of the SCREEN call cannot
  silently turn the exported photos Marathi.
- **FaceWriter.kt.** The survey sheet pictures (`drawMarks(mode = "sheet")`) pass `Tx.EN` explicitly: the
  prototype's decision 4 verbatim — "EXPORTS STAY ENGLISH … the PDF and image writers build their text from the
  English strings, never from the screen" (critic note 6). **And check (h) CHANGES WITH IT, as a decision** (the
  M3b critic's finding 2: the slice's own suite would have gone red as first planned — (h) scanned the whole of
  FaceWriter.kt and refused every Tx token, and drawMarks' tx has no default, so FaceWriter must write `Tx.EN`):
  (h) now exempts, in FaceWriter.kt alone, exactly the literal `Tx.EN` — never `tx`, never LocalTx, never Tx.MR,
  never a read like `Tx.EN.x`, plus the import that names it — and a companion test pins that the sheet render's
  argument is `Tx.EN` specifically. (h) stays decision 4's enforcement: the export sees no LIVE language, only the
  pinned English table.

**The words drawn on the photo — what translates and what stays Latin** (each proven by the page's own layer,
`vis()`, and the मराठी screenshots):

| drawn | under मराठी | mechanism |
|---|---|---|
| the four line names ("floor line") | जमिनीची रेषा, छताची रेषा, डावा/उजवा कोपरा; plan: पुढची/मागची/डावी/उजवी कडा | `lineName(tx, s, k)` over `edge.*` |
| "Ref 905 × 2053" | संदर्भ 905 × 2053 | `sp.ref_x_x` |
| chain total "FK1 Σ 3310 laser" | FK1 Σ 3310 लेझर | `sp.laser_suffix` via totalText(tx) |
| derived "3310 d", segment "≈905", reading "3310" | UNCHANGED — the page draws " d" Latin | identity, `// text:value` (critic note 4) |
| tape/Line tags "F-T1 1430 mm", "F-M2 1.86 m²", tick figures | UNCHANGED | codes, digits, mm, m² — identities |
| Face Prep "Column 1", "Door 1", "230 from floor", guide labels | कॉलम 1, दरवाजा 1, 230 जमिनीपासून, the fp.* labels — **but held at Tx.EN until M8** | FacePrep word functions |

Fonts and metrics: the prototype draws Marathi canvas words at the SAME px sizes as English (its label() keeps
`bold ${sz}px` and only inserts 'Noto Sans Devanagari' after Roboto — "what Compose draws on the Mi A3"), so the
phone keeps 12 dp for the line names, Ref and tape/Line tags, 11 dp for the chain total, 9 dp for tick figures.
SurveyPrepDraw.label and SurveyPrep360's label already use the DEFAULT typeface (not MONOSPACE), which carries
Devanagari on Android; the two paints that are MONOSPACE (vtag's MONO_BOLD, FacePrepDraw's tag) draw only ASCII in
this slice (vtag always; FacePrepDraw under Tx.EN until M8 — M8 owns its pill metrics). Both tag placers measure
the REAL string (`measureText` feeds `tapeTagAt` and the total's clamp), so a longer Marathi word moves its tag
inside the canvas instead of clipping — pinned on the pure function (below).

**S5 must already serve**

Nothing.

**Tests and checks**

- Existing :pano tests keep their English literals with `Tx.EN` added at the new first parameter: MeasTest,
  ChainTest, EndMoveTest, RefFrameTest, RoomSizeTest, OnFaceTest, SurveyPrepFileTest (problems rendered with
  Tx.EN), ReadingsTest, Pano360Test, SurveyPrepTest, DuplicateTest, FacePrepTest (stepName / detName / guide
  labels), SurveyPrepTouchTest where signatures reach it.
- **SurveyPrepMarathiTest grows an M3b block** (M3a review fix 7's reason: every Masters map passes an unknown
  value through, so a wrong map or a wrong identity leaks Latin only under मराठी): a hand-typed Marathi pin per
  Masters-backed lookup this slice adds — the sp.file salvage fills, cannotGo, the chain busy refusal, listName's
  room tail, shownSaid, the caption — each against the exact strings `vis()` answered.
- **SurveyDrawMarathiTest (new, :pano) — the canvas-metrics check**: (a) a byte-exact pin per DRAWN word under
  `Tx.MR` (the eight line names, `SurveyRef.label`'s संदर्भ prefix, `totalText` / `totalTag` at src laser /
  derived / measured / none — the " d" pinned LATIN, the " लेझर" pinned Devanagari); (b) a fits-where-English-did
  pin: every drawn tag that holds only a code and figures (`tapeTag`, `lineTag`, `segFigure`, `zoomText`) must be
  BYTE-EQUAL under Tx.EN and Tx.MR — the strongest possible "the Devanagari figure fits" guarantee, because there
  is none; (c) for the words that do change, a width proxy (MR code points ≤ EN code points + 6, characters only
  Devanagari ∪ ASCII ∪ the drawn glyph set — a mojibake guard) and the placement rule: `SurveyMeas.tapeTagAt`
  returns a point inside the canvas for a text 1.6 × the English width (it is pure, so this is a real test, not a
  budget). The RENDERING itself has one proof, the phone (M3a review fix 4's sentence), and the prototype
  screenshots in this derivation are its preview.
- The readingsMsg KIND: a test that the not-yet line colours by the kind, never by string equality — and that a
  SUCCESSFUL pull that had to say something is never OK (the conflict-carrying case, the M3b critic's finding 4).
  The :app half (the colour `when` itself) is CI's; the pano half pins `Readings.noteKind`'s whole truth table and
  `conflictText` / `orphanText` under both Tx — real pins now that conflictText lives in :pano (finding 4).
- The i18n suite: I18nTableTest (1,797 keys, the three fp.* rows flagged `phone-text` with their where lists),
  TxRenderTest (the three new templates render), I18nLiteralTest (a)–(d) over the 145 removed rows and 10 added
  ported.txt files, (f)/(i) over every renamed member — the M3a lesson: a deleted constant's SECOND reader is
  what (i) exists to catch — the Tx-lambda parameter rule, AND (h) with its M3b change (the M3b critic's finding 2:
  (h) exempts exactly the literal `Tx.EN` in FaceWriter.kt, and its companion pins the sheet render's and
  exportAll's arguments as `Tx.EN`), listed here because the first version of this section planned "FaceWriter
  passes Tx.EN" without naming (h) and would have shipped a red suite.
- The CI gate; `:app` compiles only in CI (push batch-next, wait for android-app.yml) — nothing here has been seen
  on a phone.

**What Amit sees**

Under English, nothing: every key's EN renders the current literal byte for byte (the lowercase() compositions
included), and no decision-5 pair is left in this scope — M3a consumed them. Under मराठी, the measures and
readings half turns Marathi in one build — chips, rows, refusals, file notes, readings lines, AND the photo's line
names, संदर्भ tag and chain-total लेझर — exactly the screens the derivation screenshots show. Face Prep shows no
change in either language (critic note 1).

**Risks**

A clipped Devanagari line in the rows M3a did not size (the tape's no-reading note wraps in the 3 · List column;
the prototype's ink check, `L57.overflow` / ct57-3, found this class at 1–2 px) — the phone is the only proof, and
the M3a Marathi-only line heights (reading rows 17 sp) already cover the rows these texts land in. A say() raised
in one language survives a flip for its 2.6 s (the M3a rule, accepted). The canvas words have no clipping box, so
their failure mode is overlap, not loss.

**Deliberately NOT in M3b**

- **M3c** — the plan and the Export sheet (SurveyPlan's 38 rows, SurveyPlanScreen's 8, SurveyExport's 11,
  PlanExport's 5, Handover's 7): the export's language rule there follows decision 4, as FaceWriter's does here.
- **M2** — the tree, its dialogs, the queue and the upload status line (AsIsUpload's 23 rows): until it lands the
  PUSH path's conflict note composes Tx.EN (above).
- **M6** — the laser's connection states: DistoClient.kt's 31 rows (13 shown states — their keys sit ready,
  `sp.laser.turn_bluetooth_on` … `sp.laser.disconnected_x`, beside `app.meter_disconnected` — and ~18 laser-log
  fragments that only the Laser test screen's log shows) and Disto.kt's 2 (`sp.laser.not_metric_x`,
  `sp.laser.not_a_length_x`): the shipped M3a boundary says "the laser's states (DistoClient), M6", and Disto.kt's
  two refusals reach the screen only through DistoClient's onRefused, so they ride with it.
- **M8** — Face Prep's CHROME (FacePrepScreen's 122 rows, FacePrep.kt's remaining 17: the Measure sheet's Dim
  labels, the capitalised side names, the piece labels), Annotate, the Laser test screen, the S5 sign-in sheet —
  and the one-line flip of FacePrepScreen's SCREEN-CANVAS painter argument from Tx.EN to the live tx (that one
  call alone: exportAll's stays Tx.EN for good and (h)'s companion refuses any other Tx there, so the flip cannot
  catch the export — the M3b critic's finding 3).

**The M3b critic** — raised by a careful reader, answered:

1. *FacePrepDraw rides M3b while FacePrepScreen is M8 — a half-Marathi Face Prep?* No: the pano word functions are
   keyed HERE (one draw mechanism, threaded once, its rows off the worklist), but both callers pass `Tx.EN` until
   M8, so Face Prep stays wholly English under मराठी — no mixed screen the prototype never drew ("A UI state the
   approved prototype does not draw does NOT reach the phone"), and M8's flip is one argument, not a re-key.
2. *The served page is the authority — why not copy what `vis()` shows?* Because the layer is a generic matcher
   over composed strings and mis-composes where a WHOLE key exists: it rendered "Tape · 1234 mm · from the photo"
   as "टेप · 1234 mm · पासून फोटो" (the parts of the ' · ' list translated separately) where the key says
   "… · फोटोवरून"; "Lock the front wall reference" as "the front wall लॉक करा संदर्भ" where the keys compose
   "समोरील भिंत संदर्भ लॉक करा"; and the chain chip half-English. The RAW TABLE in the page is the authority for
   the words; the phone keys whole strings, so it draws what the card wrote, not what the page's matcher
   improvised. (The task's "use its rows as the authority" means the table's rows — and they match the folder.)
3. *`msg == ReadingsSync.NOT_YET` would silently stop colouring the not-yet line grey once the text is keyed* —
   fixed in the slice, not after: the Msg carries a kind (OK / NOT_YET / FAILED), the colour reads the kind, and a
   test pins it. A string compare against a now-bilingual text is the two-answers fault in miniature.
4. *Why does " d" stay English when " laser" translates?* The prototype's screen is the spec: its fragment
   dictionary only admits fragments with a 3+-letter word, so " laser" translates and " d" never did —
   `vis("3310 d") == "3310 d"` — and the approved screen shows "3310 d". A Marathi single-letter marker would be a
   state the prototype does not draw. The ROW's word stays `sp.derived` ("हिशोबाने"), keyed since M3a.
5. *Why a tx parameter on the painters instead of a field on Marks?* Because the two callers deliberately differ
   (the screen live, FaceWriter Tx.EN): a field would make the export's language whatever the last screen set — a
   second answer. A parameter forces every call site to state it, and check (i) sees every site.
6. *Should the saved pictures and the ImageMeter photo follow the phone's language?* No — the prototype decided
   it: decision 4 ("PDFs and exported packs stay English", restated in L57's header "EXPORTS STAY ENGLISH … never
   from the screen"). A vendor pack and an ImageMeter annotation photo are documents other parties consume; the
   screen is Amit's staff's. FaceWriter passes Tx.EN, named in a comment that cites decision 4.
7. *The locator's "230 जमिनीपासून" puts the figure first — is that the prototype's order?* The prototype has no
   Face Prep, so there is no order to match (decision 6: Claude's authority); the figure-first form keeps the EN
   byte-identical ("230 from floor") under one composition and reads as a dimension tag. If Amit's phone test
   prefers "जमिनीपासून 230", the swap is one template key (`{from} {v}`), listed for M8's review alongside the
   screen flip.
8. *Three new keys in a new `fp.` family — why not `sp.`?* They are Face Prep's words, not survey prep's; M8 will
   add that screen's remaining keys to the same family, and the generator orders by family, so the home is chosen
   once. The card is not extended: phone-text flags, where-lists naming FacePrep.kt at this commit, Marathi by
   Claude (decision 6).
9. *Does deleting `lineNames(s)` and the Measure String-constructor break a reader this file cannot see?* That is
   M3a's recorded fault (the deleted LINES_REFUSAL's second reader), and its remedy is already in the suite:
   check (i) resolves every `Name.member` read from :app and :camera by reflection, and the build reads every
   caller against the files as they are on batch-next before the commit.
10. *The prototype copy, probes and screenshots live in the scratchpad — the lost-plan fault again?* No: what the
    later work depends on is IN THIS SECTION (the diffs, the vis() rulings, the screenshot findings, the key
    list), committed with it; the scratchpad run is re-creatable in minutes from the published artifact
    (claude.ai/artifact/64peToYoeaRz9UNwB926m5) and this section's recipe. Nothing from the real capture's client
    data enters the repo (deny-patterns hold: the probe strings use room-geometry figures only).

**The M3b critic, second pass** — five findings on this section as committed (87e8768), each checked in the source
before the build; all five CONFIRMED and applied, and the section above corrected in place:

1. *खोली for depth in the three fp.* MRs.* Confirmed: strings-mr.json renders every depth key with डेप्थ and its
   notes say outright "खोली would read as 'room' on these screens", while ~50 shipped keys use खोली for room. The
   three MRs were re-worded with डेप्थ before the keys were built, and SurveyDrawMarathiTest pins the corrected
   strings. (Found alongside it in the build: `{d}` is a String, not the Int this section claimed — the generator
   types Int only for a count expression or a folded plural.)
2. *Check (h) refuses any Tx token in FaceWriter.kt, and the section never named (h).* Confirmed: (h) scans the
   whole file and `drawMarks`' tx has no default, so the slice as planned shipped a red suite. (h) now exempts
   exactly the literal `Tx.EN` there (and the import), a companion test pins the sheet render's argument, and the
   change is recorded above as a decision — (h) IS decision 4's enforcement.
3. *FacePrepDraw's second caller named wrongly (FaceWriter.rewriteSurvey).* Confirmed by grep: the two callers are
   both in FacePrepScreen.kt — the screen Canvas and `private fun exportAll`, the ImageMeter photo writer; FaceWriter
   never touches FacePrepDraw. The section now names exportAll, pins it English for good through (h)'s companion,
   and narrows M8's flip to the screen-Canvas call alone — without which M8's "one-line flip of FacePrepScreen's
   painter argument" would have turned the exported photos Marathi with no pin to catch it (the same fault S5-P's
   review recorded as "the callers named rightly").
4. *The readingsMsg kind under-specified — a successful pull WITH conflicts would turn green — and the ":pano half
   pins conflictText" could not run (ReadingsSync is :app).* Confirmed: today's colour test is
   `contains("✓") && !contains("changed on the web")`, so green has always required a CLEAN pull. OK is now
   specified and built as "fetch succeeded AND said.isEmpty()" (`Readings.noteKind`), the conflict-carrying-success
   case is pinned in ReadingsTest, and `conflictText` moved into :pano beside `orphanText` so the pin is real.
5. *SurveyPrepEdit.kt grouped under app/.* Confirmed (it is pano/SurveyPrepEdit.kt); regrouped above. No row counts
   move, but the grouping is what tells the builder which changes :pano tests can run against.

**unkeyed.tsv** 829 → 684 · **ported.txt** + SurveyMeas.kt, SurveyChain.kt, SurveyRef.kt, SurveyPrepFile.kt,
Pano360.kt, Readings.kt (pano), ReadingsSync.kt, SurveyPrepDraw.kt, SurveyPrep360.kt, FacePrepDraw.kt ·
**keys** 1,794 → 1,797 (the three fp.* phone texts; Tx regenerated) · **literal-counts** re-ratcheted
(SurveyPrepScreen.kt 3 → 2) · **Disto.kt and FacePrep.kt stay out** (M6; M8).

*(Built 9 Nov 2026 on batch-next: 145 rows off unkeyed.tsv (829 → 684), ten files into ported.txt, the three fp.*
keys added with डेप्थ, Tx regenerated with i18nGen; :pano:test 924 tests, 0 failures, 0 errors, 0 skipped — the XML
under pano/build/test-results — with SurveyDrawMarathiTest new, SurveyPrepMarathiTest's M3b block, ReadingsTest's
kind and words pins, and I18nLiteralTest's (h) exemption + companion. :app is compiled only by CI; nothing has been
seen on a phone.)*

**The M3b review (9 Nov 2026)** — two findings on 29d93db, both independently confirmed (the major one twice, and
proven by a scratch Gradle project on the repo's own Kotlin 2.0.21: the bare template compiles and prints the
lambda's identity, `…$$Lambda/0x…@…`, never the note); both fixed:

1. *SurveyPrepStore.open's salvage log prints the lambda's toString, not the note.* `Decoded.problems` became
   `List<Msg>` and SurveyPrepStore.kt's `Log.w(TAG, "$id: $p")` was the one reader the build's caller walk missed —
   a TYPE change consumed through Any, not a rename, so it compiles in CI and no check could see it: (i) resolves
   member NAMES by reflection and `problems` kept its name, (f) scans `tx.<name>`, (h) never scans
   SurveyPrepStore.kt. That Log.w is the ONLY full record of what a salvage dropped (openedNote shows
   problems.first() plus a count), so the record died silently — the disabled-path version-stamp fault by a new
   route. Fixed: the loop renders each problem with the pinned English table, `${p(Tx.EN)}`, with the import and a
   comment naming why (a logcat line is a diagnostic other parties read — decision 4's register, ReadingsSync's
   stored-note precedent); no (h) exemption is needed and none was added, since (h)'s regions stay PlanExport,
   FaceWriter and AsIsUpload's doWork. The sibling pointLog loops stay `List<String>` and are untouched. And the
   CLASS is checked now, not just the instance: I18nLiteralTest gains `a Msg from problems is rendered, never
   interpolated bare` — a scan over every module for a `.problems` loop whose variable a string template takes bare
   (`$p` / `${p}`; template code survives in `masked`, so the scan sees inside the string) — plus a fixture test
   whose bad case is M3b's line byte for byte. Proven both ways on this tree: with the fix reverted the suite goes
   red on exactly that line; with it, green.

2. *Stale KDoc to members M3b deleted or renamed — the stale-note fault in miniature.* FOUR mentions, not the
   finding's three (its own verdict found the fourth): SurveyRef.kt:23 `[NO_DELETE]` → `[noDelete]`;
   SurveyMeas.kt:213 `([CAUTION])` → `([caution])`; ReadingsSync.kt:33 and the fetch KDoc both still said NOT_YET
   though `ReadingsSync.NOT_YET` was deleted — reworded to "the quiet not-yet line (sp.web_readings_not_yet)",
   naming `Readings.NoteKind` so the one LIVE referent a grep hits is the one meant. Doc-only; kotlinc ignores
   KDoc links, nothing compiles differently.

*(Review fixed 9 Nov 2026 on batch-next: :pano:test 926 tests, 0 failures, 0 errors, 0 skipped — the XML under
pano/build/test-results — the two new tests being the bare-Msg scan and its fixture pin. :app is compiled only by
CI; nothing has been seen on a phone.)*

### 27. M3c · Marathi: the plan and the Export sheet (design slice 3c)

**Size:** S · **Depends on:** M3b

**Goal**

Key the PLAN TAB — the six-drawing pane M3a's tab strip opens (chips, the fit button, the line under the drawing,
and the words DRAWN on the plan canvas) — and the Export sheet's ON-SCREEN texts (the bar, its ✕, the pills, the
screen-reader labels, the save and share refusals). The EXPORTED FILES stay English for good: the A4 sheet — title
block, footnotes and the drawing ON it — the SVG, the PDF and the share payload (decision 4), and PlanExport.kt
keeps ZERO Tx tokens (check (h) scans it whole). After M3c, survey prep INCLUDING its Plan tab and Export sheet is
Marathi under मराठी but for the laser's connection states (DistoClient, M6) and the upload status line (AsIsUpload,
M2) — §26's "only English left" sentence said the same without naming the plan, which was still English; this
slice closes it.

**Derived from, in this order**

1. **The approved prototype v61c.2**, served as a COPY from the session scratchpad (m3cverify: index61c2.html +
   its real-capture assets, byte-identical to M3b's copy) and driven in Playwright at the Mi A3 frame (360 × 780
   and 780 × 360), switched to मराठी by its own language row. Three products decide this slice:
   - **The plan SVG's own text under मराठी**, read out of `#planv svg text` per chip: the floor plan draws
     "डाव्या कोपऱ्यात", "समोर" (FRONT), "W 3310 / L 5458" and "1 m"; the front elevation draws "◂ डावी भिंत",
     "उजवी भिंत ▸", "समोरील भिंत" (the wall label), "FD1", "≈905", "W 3310 / H 2705"; the ceiling plan draws
     "रिफ्लेक्टेड सीलिंग प्लॅन". So the DRAWN words translate ON SCREEN while codes, figures, axis letters and
     "1 m" stay Latin — the exact split the table below pins.
   - **The export overlay under मराठी**: the bar reads "एक्सपोर्ट · AP_LR_htu_ceiling-plan" (the FILE NAME stays
     ASCII), the ✕'s aria is "बंद करा" — and the A4 PREVIEW STAYS WHOLLY ENGLISH ("Drawing / Reflected ceiling
     plan / Client / Room / Captured / Scale / 1:50 on A4 · drawn 09 Nov 2026", both footnotes, and FRONT and
     REFLECTED CEILING PLAN on the sheet's own drawing). Decision 4 proven on the approved page itself: the
     preview IS the file, so the preview does not follow the screen's language.
   - **84 `window.L57.vis()` probes** over every composed string the phone's plan tab and Export sheet show; every
     answer in this scope agrees with the folder's own MR byte for byte (no M3b-style finalisation diffs here),
     e.g. "Front · 2678 × 2677 · 1 mark" → "समोर · 2678 × 2677 · 1 मार्क", "fit · 150%" → "बसवा · 150%",
     "905 from left corner" → "डाव्या कोपऱ्यापासून 905", "FB1 beam over" → "FB1 वर बीम", "Share" → "शेअर करा",
     "SVG" / "PDF" / "1 m" / "W 2678" / "FD1 ≈905" / "Top" / "Bottom" → unchanged.
2. **The shipped M3a and M3b** (§25, §26, android/i18n/README.md): the tab strip and `common.plan` are already
   keyed (the प्लॅन tab in the derivation screenshots is M3a's); the drawn-words rules (figures, codes and units
   stay Latin; the painters' language is a PARAMETER, M3b critic 5); decision 4 and check (h) with its FaceWriter
   Tx.EN exemption; Tx.EN at tests' call sites; the Masters-pin lesson (M3a fix 7); the `tx.mr` 17 sp line
   heights; the say() snapshot rule.
3. **The code on batch-next** (1ca493f): every remaining unkeyed.tsv row of the five files below read in its file;
   every caller of every changing signature listed — `SurveyPlan.drawing` (SurveyPlanScreen:85, SurveyExport:154,
   SurveyPlanTest ×41), `infoOf` (SurveyPrepScreen:1400, drawing() itself ×3, SurveyPlanTest), `fitLabel`
   (SurveyPlanScreen:122, SurveyPlanTest) — and every Tx accessor checked to exist in the generated Tx with its
   exact signature: `sp_x_x_x_x(v, A, B, n: Int)` (the `{pl}` fold is `suf(n, …)`, empty under mr),
   `plan_export_x(name)`, `plan_aria_a4_sheet_x_x(view, scale: String)`, `sp_fit_x(v)`,
   `sp_x_from_left_corner(t)` / `sp_x_from_right_corner(t)`, the statics `sp_at_left_corner`, `sp_at_right_corner`,
   `sp_front`, `sp_fit`, `sp_beam`, `sp_beam_over`, `sp_loft_over`, `plan_loft_suffix`,
   `view_reflected_ceiling_plan_caps`, `common_export`, `common_close`, `common_share`, `common_room`, and the four
   `export_msg_*_x(file)` templates with `export_share_x(file)`.

**The keys: ZERO new ones — the card already holds every string this slice shows (1,797 stays 1,797).** card61c
group 1 (approved with v61c, 28 Oct 2026) even where-notes the phone sites by line: `plan.export_x` and
`plan.aria.a4_sheet_x_x` name SurveyPlanScreen.kt:176,178,187, and `export.msg.could_not_keep_x` /
`export.msg.could_not_make_folder_x` / `export.msg.could_not_write_x` / `export.msg.downloads_refused_x` /
`export.share_x` name PlanExport.kt:131/127/154/151/180 — the card was written for this slice. M3c consumes it and
adds nothing; every EN renders today's literal byte for byte (`view_floor_plan` = "Floor plan" = `DTITLE["floor"]`,
`sp.x_x_x_x` EN = the infoOf composition, mark/marks included, `export.msg.*` EN = PlanExport's four `error()`
strings). Tx is still regenerated (`nice -n 15 gradle :pano:i18nGen --offline`, a no-op on the table here) and
`nice -n 15 gradle :pano:i18nRatchet --offline` re-ratchets after.

**Files and changes** (unkeyed.tsv rows before → after; 684 → 615, 69 rows off)

**pano/:**
- **SurveyPlan.kt (38 → 0, joins ported.txt).** `drawing`, `infoOf` and `fitLabel` take `tx: Tx` FIRST, no default
  (M3b critic 5: the two callers deliberately differ — the screen live, the export Tx.EN — so every call site
  states its language). VIEWS, DTITLE and NEIGH stay English identity maps marked `// text:value`, shown through
  Masters: the chips' words through `Masters.surface_tab` (its keys ARE the VIEWS labels: "Floor" → जमीन,
  "Ceiling" → छत, "Front" → समोर …), a drawing's full name through the NEW `Masters.view_title` (below), a
  neighbour through `Masters.surface`. The drawn words: "FRONT" → `tx.sp_front` (समोर);
  "REFLECTED CEILING PLAN" → `tx.view_reflected_ceiling_plan_caps`; the elevation's wall label →
  `Masters.surface(tx, SurveyPrep.NAME.getValue(s)).uppercase()` — EN "Front wall".uppercase() = "FRONT WALL" byte
  for byte, a no-op on Devanagari (समोरील भिंत, exactly what the page draws; M3a's cannotGo pattern, upper-cased);
  the neighbours `"◂ " + Masters.surface(tx, ng.first)` / `+ " ▸"` (the glyphs values); the opening offsets →
  `tx.sp_at_left_corner` / `sp_at_right_corner` and `sp_x_from_left_corner(fo.t)` / `sp_x_from_right_corner(fo.t)`
  (left/right picked by `hx.key`, as the card's own where-notes split them); the floor-plan mark tails
  `e.code + tx.sp_beam_over` / `+ tx.sp_loft_over`, the ceiling plan's `+ tx.sp_beam` / `+ tx.plan_loft_suffix`.
  `infoOf`: the floor line `(Masters.room(tx, room) ?: tx.common_room) + " · " + figures` — the room through the
  room master, as the prototype's own "हॉल · 3310 × 5458" shows it (the M3c critic's finding 2; threading the same
  tx keeps the export English, since `Masters.room(Tx.EN, x) = x` and a typed name passes through); the elevation
  and ceiling line
  `tx.sp_x_x_x_x(Masters.surface_tab(tx, NAME minus " wall"), A, B, n)` — the plural folds in Tx (EN "1 mark" /
  "2 marks", MR "1 मार्क" / "2 मार्क", the page's own "छत · 3310 × 5458 · 0 मार्क"). `fitLabel(tx, vb, box)` →
  `tx.sp_fit` / `tx.sp_fit_x(pct)`. Marked `// text:value` and untouched: "1 m", "W/L/H/$ta + digits" (the axis
  letters are M3a's identities), `${e.code} ${fig}` labels, "≈", "?", "◂", " ▸". The exception texts
  ("not a wall: $s") are thrown on programmer error, never shown — not rows, untouched.
- **Masters: ONE new map, `view_title`** (6 values, keyed by the DTITLE value, pass-through unknown):
  "Floor plan" → `tx.view_floor_plan`, "Reflected ceiling plan" → `tx.view_reflected_ceiling_plan`,
  "<side> wall elevation" → `tx.view_*_wall_elevation`; joins `MAPS`. (view.ceiling_plan — "Ceiling plan" — is
  deliberately NOT in it: no DTITLE says that.) **Declared as six `view_title<TAB>view.*` rows in
  android/i18n/masters.tsv, never edited into Masters.kt** — that file is GENERATED by GenTx ("do not edit"), and
  i18nGen regenerates it with the new map (the M3c critic's finding 3: the regen is a no-op on the 1,797-key
  TABLE, not on Masters.kt).
- **PlanSave.kt (NEW, no shown literal).** The plan save's refusals as DATA, so PlanExport can stay Tx-free and
  the words still translate: `enum Kind { FOLDER, KEEP, INSERT, WRITE }`, `text(tx, kind, file)` rendering
  `export_msg_could_not_make_folder_x` / `_could_not_keep_x` / `_downloads_refused_x` / `_could_not_write_x`, and
  `class Refused(kind, file) : Exception(text(Tx.EN, kind, file))` — the message is the PINNED ENGLISH render, so
  a logcat line reads exactly as today (the M3b salvage-log precedent: a log is a diagnostic other parties read).
  In :pano so the renders are pinned under both Tx — the conflictText lesson (M3b finding 4): a words-home in :app
  cannot be pinned where the suite runs.
- **SurveyExport.kt (11 → 0, joins ported.txt).** NO tx parameter anywhere — the Sheet IS the file. `sheet()`
  builds its drawing as `SurveyPlan.drawing(Tx.EN, …)`, the literal, the FaceWriter idiom; the title block rows
  ("Drawing", "Client", "Project", "Room", "Captured", "Scale"), `footSource`, `FOOT_ESTIMATE`,
  "1:$sc on A4 · drawn …", "—" and "…" stay English literals marked `// text:value` (decision 4; the derivation's
  export preview shows them English under मराठी). **And check (h) grows a region, as a decision** (the M3b shape:
  (h) IS decision 4's enforcement): (h) now scans SurveyExport.kt whole, exempting — exactly as in FaceWriter.kt —
  only the literal `Tx.EN` and the import that names it, and the (h) companion pins that `sheet()`'s drawing call
  passes `Tx.EN` specifically. So no later slice can thread a live tx into the file by accident, and the SVG
  (SvgWriter, same file) is covered by the same scan.
- **Handover.kt (7 → 0, joins ported.txt).** All seven rows marked `// text:value`: `SEP` and the six
  `FACE_LABELS` values are the WIRE — the filename vocabulary `mallet_estimator/handover.py` matches
  (`MCAP-…_top.jpg`) and the caption burned into the ImageMeter photo, a document other parties consume (decision
  4; M3b critic 6 pinned the ImageMeter photo English for good). "Top" and "Bottom" have no key and need none
  (`vis()` answers them unchanged). AnnotateScreen (M8's screen) shows these labels raw today and keeps doing so —
  an unkeyed screen staying English until its slice; when M8 keys Annotate it translates the SHOWN label through a
  map, never the wire values.

**app/:**
- **SurveyPlanScreen.kt (8 → 0, joins ported.txt).** `val tx = LocalTx.current` in SpPlanPane, SpPlanFoot and
  SpPlanExport (the M3a pattern). The chips: `Text(Masters.surface_tab(tx, lab))` with
  `contentDescription = Masters.view_title(tx, SurveyPlan.DTITLE.getValue(v))`; **the drawing's `remember` gains
  `tx` as a key** — without it a language flip keeps the old canvas words until the next edit bumps `rev`; `vb`
  keeps its own keys, so the zoom survives the flip. `fitLabel(tx, shown, d.box)`. The foot: the Export pill is
  `tx.common_export`; the bar `tx.plan_export_x(name)` (text and contentDescription); ✕ a value with
  `contentDescription = tx.common_close`; the preview's label
  `tx.plan_aria_a4_sheet_x_x(Masters.view_title(tx, DTITLE.getValue(sheet.view)), sheet.scale.toString())`; the
  pills "SVG" and "PDF" stay Latin values (the page draws them Latin) and Share is `tx.common_share`. Marathi-only
  line heights, M3a's remedy for the same element classes: the chip text, the fit label (the M3c critic's finding
  4: बसवा is the drawing pane's one Devanagari control, the same 12 sp clipped class as the chips) and the foot's
  info line take `lineHeight = if (tx.mr) 17.sp else 14.sp` (the chip sits centred in its 28 / 24 dp box, which
  the prototype draws at full box height).
- **PlanExport.kt (5 → 0, joins ported.txt; stays an (h) region with ZERO Tx tokens).** The three `error()` calls
  and the `requireNotNull(out)` message (the M3c critic's finding 5: four strings, not four `error()` calls)
  become `throw PlanSave.Refused(PlanSave.Kind.…, display)` — no words left in the file; `share()` takes the
  chooser's title as a plain String parameter (no Tx token). `EXTRA_SUBJECT` and the payload are unchanged data.
- **SurveyPrepScreen.kt (already in ported.txt; call sites only).** `exportFailed` renders a `PlanSave.Refused`
  through `PlanSave.text(tx, e.kind, e.file)`; any other Throwable keeps today's `e.message ?: simpleName` — the
  system's own words stay the system's (the "Not saved: " / "Not shared: {why}" wrappers are already keyed). The
  share call passes `tx.export_share_x(sv.file.name)` as the chooser title; `planFoot` passes `tx` into `infoOf`.

**The words drawn on the plan — what translates and what stays Latin** (each proven by the served page's own plan
SVG and `vis()`):

| drawn | under मराठी | mechanism |
|---|---|---|
| FRONT (both plans) | समोर | `sp.front` |
| REFLECTED CEILING PLAN | रिफ्लेक्टेड सीलिंग प्लॅन | `view.reflected_ceiling_plan.caps` |
| the elevation's wall label (FRONT WALL) | समोरील भिंत | `Masters.surface(…).uppercase()`, a Devanagari no-op |
| ◂ Left wall · Right wall ▸ | ◂ डावी भिंत · उजवी भिंत ▸ | `Masters.surface` + glyph values |
| at the left corner / 905 from left corner | डाव्या कोपऱ्यात / डाव्या कोपऱ्यापासून 905 | `sp.at_*_corner` / `sp.x_from_*_corner` |
| FB1 beam over · FL1 loft over (floor) | FB1 वर बीम · FL1 वर लॉफ्ट | code + `sp.beam_over` / `sp.loft_over` |
| FB1 beam · FL1 loft (ceiling plan) | FB1 बीम · FL1 लॉफ्ट | code + `sp.beam` / `plan.loft_suffix` |
| 1 m, W 3310, L 5458, H 2705, $ta tags, FD1 905, ≈905, ? | UNCHANGED | codes, digits, axis letters, units — identities |
| the EXPORT SHEET's whole drawing, title block and footnotes | ENGLISH, both languages | `drawing(Tx.EN, …)` + literals; decision 4 |

Fonts and metrics: paintItems' text paint is `Typeface.DEFAULT` / `DEFAULT_BOLD`, which carries Devanagari (M3b);
sizes are the drawing's own mm (`fs`), and the prototype draws the Marathi words at the same sizes. SurveyPlan
measures no text — every label is anchored — so EN and MR display lists are item-for-item equal but for the worded
labels, which the metrics pin below turns into a test; the canvas failure mode stays overlap, never loss.

**S5 must already serve**

Nothing.

**Tests and checks**

- SurveyPlanTest keeps its English literals with `Tx.EN` at the new first parameter; its pins survive verbatim
  ("◂ Front wall", "Room · 2678 × 3319", "Back · 2678 × 2677 · 1 mark", "fit · 150%", "W 2678", and
  `DTITLE[v] == d.title` — `view_title`'s EN is DTITLE byte for byte).
- SvgExportTest unchanged: `sheet()` takes no tx, so the file cannot follow a language.
- **SurveyDrawMarathiTest grows a plan block**: (a) a byte-exact pin per drawn word under `Tx.MR` — the seven
  translated rows of the table above (eleven-plus drawn strings with their left/right and beam/loft variants; the
  M3c critic's finding 5: rows 8 and 9 are identities and the export's English, not translations), each against
  the string the served page drew or `vis()` answered; (b) the
  figure-items pin: `drawing(Tx.EN, …)` and `drawing(Tx.MR, …)` on the fixture prep differ ONLY in those worded
  Text items — every other item, positions included, byte-equal (nothing measures text, so this is exact), the
  strongest "the Devanagari drawing is the English drawing" guarantee available.
- **SurveyPrepMarathiTest grows an M3c block** (M3a fix 7's reason — a Masters map passes unknowns through, so a
  wrong map leaks Latin only under मराठी): hand-typed Marathi pins for the six `view_title` renders, the six chip
  words through `surface_tab`, the two infoOf shapes (समोर · 2678 × 2677 · 2 मार्क; खोली · … for the Room fallback;
  AND a master room name, "Living Room" → हॉल · figures, beside the खोली pin — the M3c critic's finding 2, since
  the planned pins alone would not have caught a raw room name), fit (`sp_fit` / `sp_fit_x`, the critic's finding
  4), `plan_export_x`, the A4 aria, and the four `PlanSave.text` renders.
- **PlanSave's own :pano pins**: `text(Tx.EN, …)` byte-equal to PlanExport's four previous literals;
  `text(Tx.MR, …)` to the card's MR; `Refused.message` IS the Tx.EN render.
- I18nLiteralTest: (a)–(d) over the 69 removed rows and the 5 files joining ported.txt; (f)/(i) over the changed
  signatures (names keep, so (i) stays quiet and CI's compile is the resolver — the build walks every caller
  listed above before committing, the M3a lesson about second readers); **(h) with its M3c change**: SurveyExport.kt
  joins the scanned regions with the FaceWriter-style exemption (exactly the literal `Tx.EN` plus its import), the
  companion pins `sheet()`'s drawing argument as `Tx.EN`, and PlanExport.kt stays scanned whole with zero mentions.
  **The change is THREE test edits plus a self-test, not one line** (the M3c critic's finding 1): the scan was
  gated `if (region != null && s.rel.startsWith("app/"))`, so a pano file named in the `when` would have built a
  region the gate then silently never scanned — green while scanning nothing, this file's recorded worst failure
  shape. So the regions are keyed on the FULL `s.rel` (the app/ gate goes), the pinned-English exemption widens
  from `s.name == "FaceWriter.kt"` to FaceWriter + SurveyExport, the not-found guard adds SurveyExport.kt, and a
  self-test (the `i sees a deleted constant` idiom) plants a `tx` mention at SurveyExport's pano path and asserts
  the region fires — and that the pinned `Tx.EN` with its import passes while a read THROUGH it (`Tx.EN.x`) is
  refused.
- MastersTest: `view_title` joins the MAPS walk.
- I18nTableTest (1,797 keys, unchanged) and TxRenderTest (no new templates) stay as they are.
- The CI gate; `:app` compiles only in CI (push batch-next, wait for android-app.yml) — nothing here has been seen
  on a phone.

**What Amit sees**

Under English, nothing: every key's EN renders the current literal byte for byte, uppercase compositions included.
Under मराठी the plan tab turns Marathi in one build — chips, बसवा, the info line, and the drawn corner fills, wall
names and plan titles — and the Export sheet's bar, Close and Share translate, while the A4 preview, the saved SVG
and PDF and the shared file stay English: exactly the served prototype's screens, both orientations. A failed save
reads "सेव्ह नाही: " + the refusal in Marathi for the four PlanSave kinds; a system exception's words stay English
after the keyed fragment, as everywhere else.

**Risks**

The clipped-Devanagari class (the phone is the only proof; the chips and foot take M3a's 17 sp, and the
derivation's overflow probe found no plan-tab cut — its two entries are the title bar's, cut under English too). A
longer Marathi word on the canvas can touch a neighbouring dimension on a small drawing — but the approved page
draws the same words at the same anchors, so the overlap budget is the prototype's own. A language flip with the
Export overlay open re-renders the bar at once and rebuilds the drawing through the `remember` key; the preview is
English either way.

**Deliberately NOT in M3c**

- **The exported files' English is not a gap** — decision 4, now enforced over SurveyExport.kt by (h).
- **The prototype v61c's richer export and plan** — its overlay's SVG · चित्र · PDF · आणखी… buttons, the office
  view, the legend, the trades/states filters (plan.js v53+; the card's `sp.legend_x`, `sp.office_view_*`,
  `sp.showing` rows) — M3c is a TRANSLATION slice over the phone's shipped v52 plan tab and adds no UI; those
  features are no row of this table and wait for Amit to ask.
- **The project-level Floor plan screen** (the prototype's `plan.*` arrange-rooms family, the Floor plan tile):
  no phone screen exists to translate.
- **FlatPresets.kt (31 rows: site types and room names, the bench's room master — M2's Add-site dialog data),
  DeleteRoute.kt (5), RoomToken.kt (1), ServerMessage.kt (2)** — the tree's and dialogs' slices, not the plan's.
- **M6** (DistoClient's 31 + Disto's 2), **M2** (AsIsUpload's 23), **M8** (FacePrep's 17, FacePrepScreen's 122,
  AnnotateScreen's 46 — including, then, the shown face labels drawn through a map over Handover's wire values).

**The M3c critic** — raised by a careful reader, answered:

1. *The card holds Marathi for the title block (common.drawing, sp.1_x_a4_drawn, export.photo_not_yet_measured…) —
   why does the phone's A4 preview stay English?* Because the preview IS the file: `paintSheet` is the ONE painter
   for the overlay's Canvas and the PDF page ("what the phone shows before a save is the file it saves"), and the
   served prototype's own export preview stays wholly English under मराठी — captured in this derivation. Those MR
   rows belong to the prototype's OTHER readers (export53.js's sheet list and notes — screens the phone does not
   have). Translating the preview would fork the painter (two answers) or translate the file (decision 4 broken).
2. *Why a new :pano file for four error strings instead of keying them where they are?* Check (h) scans the whole
   of PlanExport.kt and refuses every Tx token — rightly, it writes the files — while the card names those four
   strings as phone texts to translate. The words therefore move to where a tx can reach them and a :pano test can
   pin them, exactly as ReadingsSync's conflictText moved in M3b (finding 4); the kinds travel as data and the
   screen renders with the live tx. The alternative — exempting four literals inside an export builder — would
   blunt (h) at its sharpest edge.
3. *Does share()'s title parameter smuggle a live language into a file?* No: the chooser title is Android's own
   transient UI, not file content — the payload (EXTRA_STREAM bytes, SUBJECT = the file's name) is untouched; and
   the card where-notes `export.share_x` to PlanExport.kt:180 as a phone text, so it is keyed rather than argued
   away. A String parameter carries no Tx token, so (h) still holds over the file.
4. *The chips say जमीन / छत — surface words for drawings that mean the floor PLAN and ceiling PLAN. Right?* The
   approved page's own chips: text जमीन / छत / समोर …, aria the full plan titles — captured in plan-chrome. The
   phone's Text + contentDescription split the same way, through `surface_tab` and `view_title`.
5. *sp_x_x_x_x folds the plural — does मराठी lose the count?* No: `suf()` empties only the SUFFIX under mr; `{n}`
   stays — "समोर · 3310 × 2705 · 2 मार्क", the page's own answer, and "1 मार्क" for one.
6. *Why does `view_title` exist when `Drawing.title` could just translate inside drawing()?* It does both:
   drawing() builds `title` through the map, and the SCREEN also needs the lookup where no Drawing exists yet (a
   chip's aria for the five drawings not open, the export aria from `sheet.view`). One map, two call shapes —
   against a second hand-written when, which is the two-answers fault.
7. *The derivation lives in the scratchpad — the lost-plan fault again?* No (M3b critic 10's answer holds): what
   later work depends on is IN THIS SECTION — the SVG text captures, the vis() rulings, the export-preview
   finding, the per-file mapping — and the run is re-creatable in minutes from the published artifact and this
   recipe. No client name, phone number or rate enters the repo: the probes use room geometry and the demo's
   initials-style file names only (the page's own "Amit Phadke" title-block demo row stays on the page).

**The M3c critic, second pass** — five findings on this section as committed (191641c), each checked in the source
by the builder; all five CONFIRMED and applied, and the section above amended where each sat:

1. *Check (h)'s region scan is gated on app/ files, so "SurveyExport.kt joins the scanned regions" as written scans
   nothing.* CONFIRMED — I18nLiteralTest.kt line 273 on 1ca493f reads `if (region != null &&
   s.rel.startsWith("app/"))`, the exemption was hard-coded `s.name == "FaceWriter.kt"` and the not-found guard
   listed three files. Applied as the three test edits plus the self-test, written into the (h) bullet above.
2. *The floor info line leaves a master room name Latin under मराठी.* CONFIRMED — infoOf passed `room` raw while
   the derivation's own captures (vis-mr.json "Living Room · 3310 × 5458" → "हॉल · 3310 × 5458", plan-chrome.json
   "info": "हॉल · 3310 × 5458") and the house pattern (BrowseScreens:801, AlbumPage, WishList.roomName) show a room
   through Masters.room. Applied: `Masters.room(tx, room)` threads the same tx (Tx.EN is the identity), and the
   M3c block pins हॉल beside खोली. SurveyPlanTest's EN pins survive unchanged.
3. *Masters.kt is generated; view_title is six masters.tsv rows, and the regen is not the no-op §27 called it.*
   CONFIRMED — Masters.kt line 1 says "GENERATED by GenTx — do not edit"; GenTx.masters() reads `map<TAB>key` rows.
   Applied in the Masters bullet above; i18nGen regenerated Masters.kt (the keys table stayed 1,797).
4. *The fit label shows बसवा but was left out of the 17 sp remedy and the pin list.* CONFIRMED — the fit button is
   the same 12 sp / 14 sp clipped class as the chips. Applied: `if (tx.mr) 17.sp else 14.sp` on the fit Text, and
   sp_fit / sp_fit_x pins in the M3c block.
5. *Two counts drift: "the nine translated rows" and "the four error() calls".* CONFIRMED — the drawn-words table
   has seven translated rows (rows 8 and 9 are identities and the export's pinned English), and PlanExport had
   three error() calls plus one requireNotNull message. Both bullets reworded above.

**And three more the build itself found**, recorded the M3b way (what the plan missed is worth more than what it
got right):

6. **MastersTest's wire-and-export bytecode check had SurveyExport in its `whole` list** — "its compiled bytes may
   not name the i18n package at all" — which §27 never named, and `SurveyPlan.drawing(Tx.EN, …)` names the i18n
   package in SurveyExport's constant pool by design. The suite went red exactly as that check intends. Applied:
   SurveyExport moves from `whole` to `named` (every public builder listed, no signature may take an i18n type);
   what `whole` enforced is now (h)'s whole-file scan plus the companion's Tx.EN pin, which is STRONGER — the
   bytecode check could not tell Tx.EN from Tx.MR, the text scan refuses everything but the pinned literal.
7. **`Sheet.tx` / `Sheet.ty` — the sheet's translate offsets — read as a Tx mention to (h)'s whole-file scan**
   (`val tx: Double`, SurveyExport.kt:134). Renamed `dx` / `dy` (three use sites: SvgWriter's translate,
   PlanExport.paintSheet, SvgExportTest), rather than teaching the scan an exemption: an offset named like the
   language table is exactly what a textual check must flag, and a fresh exemption would blunt (h) where M3c just
   sharpened it.
8. **The totals row was one high on both sides: unkeyed.tsv went 683 → 614, not 684 → 615** (the 69 rows off is
   right; readTsv's count of the committed file at 1ca493f is 683). And literal-counts re-ratcheted ONE file,
   SurveyPlan.kt 5 → 4 — SurveyExport.kt's 50 stood, because its delisted rows were all marked `// text:value` in
   the same commit, which the ratchet counts the same as listed.

**unkeyed.tsv** 683 → 614 · **ported.txt** + SurveyPlan.kt, SurveyExport.kt, Handover.kt (pano),
SurveyPlanScreen.kt, PlanExport.kt · **keys** 1,797 → 1,797 (the card is consumed, not extended; Tx regenerated —
a no-op on the table, Masters.kt regenerated with `view_title`) · **new** pano/PlanSave.kt, `Masters.view_title`
(six masters.tsv rows) · **literal-counts** re-ratcheted (SurveyPlan.kt 5 → 4; every other touched file's count
stood, its leaving rows marked). Built as one commit on batch-next; :pano 935 tests, 0 failures, 0 skipped; :app
compiles only in CI.

### 29. M6 · Marathi: the rest of the shell and the S5 sign-in sheet (design slice 6)

**Size:** M · **Depends on:** M2 · (§28, M4 — Marathi: the rest of the wish list — shipped 3 Nov 2026
inside the port's wish-list work and has no section of its own; the table's row 28 is its record.)

**Goal**

Key everything §24's out-list left to M6: the S5 SIGN-IN SHEET (S5SignInDialog and S5Session's refusals — the
17 rows the page's own left-out table files as "decision6 · the S5 sign-in sheet … no prototype screen yet"),
the legacy ERP key dialog (SettingsDialog), the drawer's leftover sites ("Server" / "not set" / "Sign out" at
their DIALOG sites — the drawer rows themselves have been keyed since slice 1b — plus "ERP key refused — tap to
sign in to S5" and "Update failed: {why}" with FrappeClient's download refusal inside it), the ImageMeter import
and annotation results, the shoot-and-keep flow, the X3's connect and capture notes (X3Bridge), and the LASER's
shown states and refusals (DistoClient, Disto) — the last English on survey prep. After M6, survey prep is
CLOSED (§26/§27's "only English left" sentence ends: M2 took the upload status line, M6 takes the laser
states), MainActivity reaches 0 and joins ported.txt, and the English left under मराठी is M8's screens whole
(FacePrepScreen 122, AnnotateScreen 46, LaserTestScreen 24, FacePrep's 17, Annotation's 17, and the Tx.EN
flips) plus decision 4's exports and the wire, for good.

**Derived from, in this order**

1. **The approved prototype v61c.2**, served as a COPY from the session scratchpad (m6verify: index61c2.html at
   the current header refresh — Version 82, "phone APK 0.3.303" — plus the real capture's assets, the
   M2/M3b/M3c recipe) and driven in Playwright at the Mi A3 frame (360 × 780 and 780 × 360), switched to मराठी
   by its own language row. Six products decide this slice:
   - **The page's table diffed against android/i18n** (raw-table.json, 1,750 rows): ZERO English diffs; the
     same 3 Marathi diffs M2 found (M3a's colon-genitive finalisations — the FOLDER wins, decision 3); the same
     16 page-only orphans. The card is still the page's words.
   - **The drawer and the page's Laser test replica under मराठी, screenshotted in both orientations**: the
     drawer's लेझर group ("लेझर चाचणी", value "DISTO D2" LATIN), सर्व्हर with its URL as data, साइन आउट, "भाषा
     · Language" in both scripts; the replica reading मागे / लेझर चाचणी · DISTO D2 / शेअर करा / — mm / कनेक्ट
     नाही / कनेक्ट करा / फायर / लेझर चालू with the checks note in Marathi. The replica is M8's SCREEN (its
     chrome rows stay LaserTestScreen's 24); M6 takes only the words SURVEY PREP shows today.
   - **95 `window.L57.vis()` probes** over the laser, X3, ImageMeter, dialog and sign-in compositions, each
     agreeing with the folder's MR byte for byte wherever a whole key exists: "Turn Bluetooth on" → "ब्लूटूथ
     सुरू करा"; "Scan refused: application registration failed" → "स्कॅन नाकारले: application registration
     failed" (the system's {why} as given); "X3 capture failed: stitch export cancelled" → "X3 कॅप्चर झाले
     नाही: स्टिच एक्सपोर्ट रद्द झाले" (a note translating INSIDE the keyed head); "Update failed: download:
     HTTP 404" → "अपडेट झाले नाही: डाउनलोड: HTTP 404"; "Sent 2 annotated faces from the gallery." → "गॅलरीतून
     खुणा केलेल्या 2 बाजू पाठवल्या."; "(no client)" → "(ग्राहक नाही)" — and the identities UNCHANGED:
     "DISTO D2", "no ConnectivityManager", "MCFT Interior Estimate", "mm · ft-in".
   - **THE LASER LOG STAYS ENGLISH ON THE APPROVED PAGE**: its one demo log line has its own key
     (app.found_disto_d2_4512) whose MR EQUALS its EN, and the replica draws it unchanged under मराठी — the
     page's own ruling that the laser LOG does not translate. It is also what "Share laser log" sends OFF the
     phone, so decision 4's register applies twice over (a diagnostic other parties read, and an export).
   - **THE SIGN-IN SHEET HAS NO PAGE ROWS**: "Sign in", "ERP email", "Password", "Signing in…", "ERP key
     (legacy)" and every S5Session refusal come back from vis() UNCHANGED — or improvised ("Signed in as Amit"
     → "as Amit मध्ये Signed", the generic matcher mis-composing, M3b critic 2) — and the page's left-out table
     names the 17 rows decision 6. So the sheet's Marathi is Claude's, composed from the page's OWN sign-in
     vocabulary (साइन इन करा and ERP ईमेल / पासवर्ड from app.sign_erp_email_password's MR, सर्व्हर, साइन आउट),
     so the sheet reads as the page's banners already read. One sheet error needed NO new key: "web answered
     {code}" is already app.note.web_answered_x ("वेबचे उत्तर {code}", proven by vis()).
   - **The overflow probe**: zero entries in this scope.
2. **The shipped slices**: M2 (§24 — UploadNote as the stored notes' ONE home and mapper, statusLine, the
   TreeQueueMarathiTest reason); M4's rule (stored in English, drawn through a key, anything else drawn as
   stored); M3b (the words-home lesson — a mapper in :app cannot be pinned where the suite runs; the
   salvage-log fault — a TYPE change consumed through Any compiles and prints a lambda; Tx.EN on the callers of
   a screen not yet keyed, FacePrepDraw's idiom); M3c (the PlanSave.Refused idiom — a typed refusal whose
   exception message is the PINNED English render; (h) as decision 4's enforcement, UNCHANGED here); the PL
   review (an identity drawn raw is invisible to every literal check).
3. **The code on batch-next (80d3526)**: all 108 unkeyed rows of the seven files read in their files; every
   caller of every changing signature listed — `DistoClient.onState` / `onRefused` (SurveyPrepScreen:456–457,
   the say() path; and the three M8 screens that render `Tx.EN` until M8: LaserTestScreen:52,55,
   AnnotateScreen:253–254, FacePrepScreen:173–174); `Disto.Outcome.refused` (DistoClient.handle:332, DistoTest);
   `CameraPort.connect`'s note (X3Bridge.connect, MainActivity:2850–2852 → `camNote`:2465 → CaptureSheet's
   `cameraNote`, TreeShell:382 and its render at :495); `shootAndExport`'s failures (MainActivity:2864–2869);
   `S5Session.login` (MainActivity:3079) and `refusal()` (MainActivity:1865 — the banner's {why} — and :3021;
   AsIsUpload:374 and :548, the SIGN_AGAIN tails UploadNote.line already re-renders); `FrappeClient.
   downloadPrivate` (MainActivity:1229, the update flow); `AnnotationPush.push` (MainActivity:944, :2085) — and
   every Tx accessor checked to exist in the generated Tx with its exact signature (`sp_laser_scan_failed_x
   (code)`, `sp_laser_scan_refused_x(why)`, `sp_laser_bluetooth_refused_x(why)`, `sp_laser_disconnected_x
   (status)`, `sp_laser_reconnecting_x(status)`, `sp_laser_not_metric_x(unit)`, `sp_laser_not_a_length_x(x)`,
   the seven laser statics with `app_meter_disconnected`, `app_x3_x_join_wifi(problem)`,
   `app_x3_cannot_watch_wifi_x(why)`, `app_x3_connect_error_x(code)`, `app_x3_capture_error_x(code)`,
   `app_x3_stitch_failed_x(code, why)`, the three X3 statics, `app_msg_update_failed_x(why)`,
   `app_download_http_x(code)`, `app_msg_x3_capture_failed_x(why)`, `app_msg_could_not_save_x` /
   `_split_x` / `app_msg_no_camera_app_x` / `app_msg_could_not_send_annotation_x(why)`,
   `app_msg_sent_x_annotated_gallery(n: Int)` and `app_msg_sent_x_annotated_to_x(n: Int, server)` (both fold
   face{pl}), `app_msg_x_annotated_already_sent(n)` / `_not_reached(n)`, the five ImageMeter statics,
   `app_generate_both_your_user` (ONE key whose EN is the dialog's two concatenated fragments byte for byte),
   `app_server`, `app_site_url`, `app_api_key`, `app_api_secret`, `common_save/cancel/close/not_set`,
   `app_sign_out`, `app_erp_key_refused_sign_s5`, `common_survey_prep`, `app_discard_360`,
   `app_note_web_answered_x`, `app_sign_again_menu_server_x`).

**The keys: SIXTEEN new (1,798 → 1,814); everything else consumed, not coined.** card61c group 1 (approved with
v61c, 28 Oct 2026) where-notes the laser and X3 keys to these exact files — the 11 `sp.laser.*` states beside
`app.meter_disconnected`, Disto's `sp.laser.not_metric_x` / `not_a_length_x`, the 8 `app.x3.*` — and beside
them the card's earlier phone-text keys (where-noted to these exact sites at 6575b85) hold the drawer leftovers
(`app.erp_key_refused_sign_s5`, `app.msg.update_failed_x`, `app.download_http_x`), most ImageMeter results and
the capture-flow refusals, while the v53 PAGE keys hold the legacy dialog, the dialogs' atoms
(`common.save/cancel/close/not_set`, `app.sign_out`), the shoot-and-keep labels (`common.survey_prep`,
`app.discard_360`) and the two ImageMeter demo sentences (importing, looked-14) — all listed in "Derived from"
3. The sixteen are decision 6 (flag `phone-text`, where-list naming the site at
this commit; MR in the shipped register, the sheet's words from the page's own sign-in vocabulary):

- `app.sign_in` · EN `Sign in` · MR `साइन इन करा` — the sheet's title and its button (MainActivity:3024, :3088)
- `app.signing_in` · EN `Signing in…` · MR `साइन इन होत आहे…` (:3088)
- `app.signed_in_as_x` · EN `Signed in as {name}` · MR `{name} म्हणून साइन इन केले` (:3032; the ` ({kind})`
  tail stays a value join — S5's own kind word, data, and after the staff gate it is always "staff")
- `app.captures_go_x` · EN `Captures go to MCFT Interior Estimate ({url}).` · MR
  `कॅप्चर MCFT Interior Estimate ({url}) कडे जातात.` (:3035; the product name Latin, the URL data)
- `app.erp_email` · EN `ERP email` · MR `ERP ईमेल` (:3040)
- `app.password` · EN `Password` · MR `पासवर्ड` (:3045)
- `app.same_email_password_erpnext` · EN `The same email and password you use for ERPNext.` · MR
  `ERPNext साठी वापरता तोच ईमेल आणि पासवर्ड.` (:3051)
- `app.erp_key_legacy` · EN `ERP key (legacy)` · MR `ERP की (जुनी पद्धत)` (:3061)
- `app.note.email_password_needed` · EN `Email and password are needed` · MR `ईमेल आणि पासवर्ड दोन्ही हवेत`
- `app.note.web_unreachable` · EN `web unreachable` · MR `वेबशी संपर्क होत नाही`
- `app.note.erp_not_accept_email_password` · EN `ERP did not accept that email and password` · MR
  `ERP ने तो ईमेल आणि पासवर्ड स्वीकारला नाही`
- `app.note.web_no_session` · EN `the web did not start a session` · MR `वेबने सेशन सुरू केले नाही`
- `app.note.signed_out_meanwhile` · EN `signed out meanwhile` · MR `दरम्यान साइन आउट झाले`
- `app.note.only_staff_not_staff` · EN
  `Only MCFT staff can send captures from this app; this ERP account is not staff` · MR
  `या अ‍ॅपमधून कॅप्चर फक्त MCFT स्टाफच पाठवू शकतो; हे ERP खाते स्टाफचे नाही` (refuse.stage.staff_only's register)
- `app.note.only_staff_x_account` · EN
  `Only MCFT staff can send captures from this app; this ERP account is a {kind} account` · MR
  `या अ‍ॅपमधून कॅप्चर फक्त MCFT स्टाफच पाठवू शकतो; हे ERP खाते {kind} प्रकारचे आहे` ({kind} is S5's own word,
  Latin — the M4 reasonLine rule)
- `app.msg.looked_x_gallery_photos` · EN `Looked at {n} gallery photos, none carrying this app's stamp. Only
  faces captured with 0.3.93 or later are stamped — re-shoot and annotate that one.` · MR = the card's
  app.msg.looked_14_gallery_photos MR with {n} for its baked-in demo count — that v53 static is the PAGE's own
  demo row and stays; a static cannot carry `scan.looked`.

Tx IS regenerated (`nice -n 15 gradle :pano:i18nGen --offline`, then `nice -n 15 gradle :pano:i18nRatchet
--offline`) — and the regen is NOT a no-op on Masters.kt (the M3c critic's finding 3): **masters.tsv gains ONE
row, `client_name<TAB>app.no_client`, the row its own comment has promised to M6 since L2** ("app.no_client …
in Marathi slice 6 (port M6, which keys S5Session.kt)"), so the NEW map `Masters.client_name` carries
"(no client)" → "(ग्राहक नाही)" and passes every typed client name through.

**The stored sign-in refusals move HOME to UploadNote (the M2/M4 rule, both its reasons again).** S5Session's
error strings are STORED — `relogin` writes the refusal into prefs (`refused`), `AsIsUpload` folds it into a
capture's note as `SIGN_AGAIN_PREFIX + it` (:374, :548) — and old phones already hold these bytes, so the
constants cannot translate and cannot move a byte. They become UploadNote constants (values byte-identical to
what 0.3.303 stores): `EMAIL_PASSWORD_NEEDED`, `WEB_UNREACHABLE`, `ERP_NOT_ACCEPT`, `WEB_NO_SESSION`,
`SIGNED_OUT_MEANWHILE`, and the pair `ONLY_STAFF_IS` ("Only MCFT staff can send captures from this app; this
ERP account is ") with `onlyStaff(kind)` — the ONE composer S5Session now calls, beside the ONE parser —
`line()` grows the five exact matches and the only-staff parse (the webAnswered idiom: the tail "not staff" →
its key, `a {kind} account` → its template with the kind as given, anything else AS STORED). S5Session's
"web answered ${a.code}" becomes `UploadNote.WEB_ANSWERED_PREFIX + a.code` — byte-identical, one home, and
line() already renders it. CONSEQUENCE FOR FREE: the queue rows' and web line's sign-in tails ("sign in again
(menu → Server) - ERP did not accept that email and password") now translate WHOLE through the existing nested
`line` path, with no new wiring — M2 built the pipe, M6 fills the last constants. S5's own error sentence (the
`text` its /api/auth/login body carries) still draws as stored: data, never guessed at.

**Two new :pano word-homes, no shown literal, both join ported.txt** (the conflictText/PlanSave lesson: a
words-home in :app or :camera cannot be pinned where the suite runs):

- **LaserNote.kt (NEW).** The laser's words, rendered live: the 12 state Msgs DistoClient hands its readers
  (`turnBluetoothOn`, `noBleScanner`, `scanRefused(why)`, `noDistoFound`, `scanFailed(code)`,
  `bluetoothRefused(why)`, `connectPermissionRefused`, `reconnecting(status)`, `meterDisconnected`,
  `disconnected(status)`, `notADisto`, `noDistanceCharacteristic` — whole keys, the {why}/{status}/{code}
  fills data) and `refusal(r: Disto.Refusal): Msg` over `sp.laser.not_metric_x` / `not_a_length_x`.
- **CameraNote.kt (NEW).** The X3's words: the connect-note Msg builders (`joinWifi(problem: Msg)` over
  `app.x3.x_join_wifi`; `notOnCameraWifi`, `cannotWatchWifi(why)`; `NO_CM` = "no ConnectivityManager" marked
  `// text:value` — the page draws it Latin, a system identifier; `connectError(code)`), and the shoot path's
  typed refusal, the PlanSave idiom byte for byte: `enum Kind { NO_FILE, CAPTURE_ERROR, STITCH_FAILED,
  STITCH_CANCELLED }`, `text(tx, kind, code, why)` over the four `app.x3.*` keys, `class Refused(kind, code,
  why) : Exception(text(Tx.EN, …))` — a logcat line reads exactly as today — and `line(tx, e: Throwable)`
  rendering a Refused through `text(tx, …)` and any other Throwable as `e.message ?: simpleName` (M3c's
  exportFailed idiom).

**Files and changes** (unkeyed.tsv rows before → after; 347 → 239, 108 rows off)

**pano/:**
- **LaserNote.kt / CameraNote.kt (NEW).** Above.
- **Disto.kt (2 → 0, joins ported.txt, STAYS Tx-FREE — the ServerMessage/AsIsPath idiom: the wire's parser
  carries no language).** `Outcome.refused` stops being a String and becomes `Refusal(kind, arg)` — kind
  NOT_METRIC or NOT_A_LENGTH, arg the pre-formatted figure ("4", "0.0000"; the `%.4f` ROOT formatting stays
  here, pure) — and the two sentence literals LEAVE the file; LaserNote renders them. DistoTest's
  `refused.contains("unit code 4")` pins become renders through `LaserNote.refusal` under both Tx.
- **CameraPort.kt (no rows).** `connect`'s callback becomes `(connected: Boolean, error: Msg?) -> Unit` — the
  KDoc's "error text" becomes "error note, rendered at draw time". Same module as Msg; still SDK-free.
- **UploadNote.kt (already in ported.txt).** Grows the five constants, `ONLY_STAFF_IS` + `onlyStaff(kind)`,
  and line()'s matches, all above the conflictNoteLine fallthrough.

**app/:**
- **DistoClient.kt (31 → 0, joins ported.txt).** `onState` becomes `(State, Msg?)`, `onRefused` becomes
  `(Msg)`; every `moveTo` note is a LaserNote builder — except CONNECTING's, which is the DEVICE NAME
  (`{ _ -> name ?: "DISTO" }`, data; "DISTO" marked `// text:value`, the page's own value list names it). THE
  TWO SILENT TEMPLATE SITES ARE REWRITTEN BY HAND, because a Msg in a string template compiles and prints the
  lambda's identity — the M3b salvage-log fault's exact shape: `moveTo`'s event line renders
  `note?.let { " — " + it(Tx.EN) }` and the refused line renders `"refused: " + why(Tx.EN)` — the LOG keeps
  today's bytes through the pinned table, as SurveyPrepStore's salvage log does. Every OTHER reader of the
  changed types is a compile error CI refuses (say(n) wants a String; the three M8 screens' `note = n` state
  is `String?`), walked in "Derived from" 3. The ~19 event/log rows are marked `// text:value` (the GATT
  transcript: "accepted", "refused by Android", "advertises the DISTO service", "matched by name only",
  "characteristics: ", "found '…' rssi … — ", "sent '$cmd' (…)", "subscribed … (status $s)", "distance […] =
  %.4f m", "unit […] = code $u", "no command characteristic — cannot send '$cmd'", "state …", "refused: ",
  the glyph joins — a diagnostic other parties read AND the Share-laser-log export, decision 4's register,
  the page's own English demo log line the ruling).
- **S5Session.kt (10 → 0, joins ported.txt).** The error literals become the UploadNote constants above
  (bytes unchanged — stored prefs on phones must keep matching); "web answered" through
  `WEB_ANSWERED_PREFIX`; `NO_CLIENT` "(no client)" marked `// text:value` (a stored bucket value, S5Masters
  files projects under it) and SHOWN through the new `Masters.client_name` — threaded at the client-name draw
  sites, the PL review's lesson: BrowseScreens.ClientsScreen:221 (`title = c`; the badge stays `initials(c)`
  on the stored value, a code), the client crumbs (MainActivity:1748 with its :1749 sibling labels, :2502),
  searchTree's client hit (:3553) and the client fills of its site/project hits (:3560, :3564), the project
  sheet's Client value (TreeShell:279), HomeScreens' two lines (:132 — where the hero card also drew the
  SITE raw while :166 drew it through `Masters.site_name`, the M6 critic's finding 2, so :132 takes BOTH
  masters — and :166), queueRow's subtitle fill. AND A
  CATCH-UP THE DERIVATION FOUND: the room-crumbs' SITE label (MainActivity:2506) draws `st` raw where the
  tree crumb (:1753) goes through `Masters.site_name` — an M2 leftover of the same class, threaded here with
  the client crumbs and pinned beside them.
- **FrappeClient.kt (1 → 0, joins ported.txt, stays Tx-free — it is the wire's client).**
  `ApiException` becomes `open` and `class Download(code: Int) : ApiException(code, "download: HTTP $code")`
  (no `val` — the inherited `code` serves the update flow's check) is thrown by downloadPrivate; the first
  version of this bullet subclassed a FINAL class and re-declared `code`, two compile errors in the module
  only CI compiles (the M6 critic's finding 1). SyncWorker:224's `catch (e: FrappeClient.ApiException)` still
  catches a Download unchanged. The message literal marked `// text:value a diagnostic — the shown line
  renders app.download_http_x from [code]`. startUpdate's onFailure becomes
  `lastResult = { tx -> tx.app_msg_update_failed_x(why = if (e is FrappeClient.Download)
  tx.app_download_http_x(e.code.toString()) else e.message.orEmpty()) }` — "अपडेट झाले
  नाही: डाउनलोड: HTTP 404", the served page's own composition.
- **AnnotationPush.kt (2 → 0, joins ported.txt, ZERO Tx tokens).** Both rows marked `// text:value`:
  "Annotated in ImageMeter; matched by the app's stamp" is the comment uploadAnnotation SENDS — stored on the
  bench, read in ERP, a document other parties consume (decision 4) — and "could not read the annotated
  image" is an error() swallowed by push's runCatching (onSuccess-only): a diagnostic no screen shows.
- **MainActivity.kt (51 → 0, JOINS ported.txt — the last of the shell).** By cluster: THE SIGN-IN SHEET —
  `app.sign_in` (title and button), `app.signing_in`, `app_signed_in_as_x(name)` + the ` ({kind})` value
  tail, `app_captures_go_x(BuildConfig.WEB_URL)`, `app.erp_email`, `app.password`,
  `app.same_email_password_erpnext`, `app.erp_key_legacy`, `app.sign_out`, `common.close` / `common.cancel`,
  and `problem` rendered `UploadNote.line(tx, it)` — S5's own sentence as stored, the constants through their
  new keys; the refusal banner's fill becomes `app_sign_again_menu_server_x(UploadNote.line(tx, s5Refused))`
  (:1870). THE LEGACY KEY DIALOG — `app.server` (title), `app.site_url`, `app.api_key`, `app.api_secret`,
  `app.generate_both_your_user` (replaces the two fragments), `common.save`, `common.cancel`. THE DRAWER
  LEFTOVERS — `app.erp_key_refused_sign_s5` (:1276); the Add-site type fallback (:2752) renders
  `tx.common_not_set` when no type is picked instead of passing "not set" through Masters.site (an unknown
  value passed through — English under both languages today). THE IMAGEMETER IMPORT — `localLine` becomes a
  Msg rendered at draw (lastResult already takes the live tx): `app_msg_android_hiding_photos`,
  `app_msg_sent_x_annotated_gallery(sent)`, `app_msg_x_annotated_not_reached("${scan.stamped}")`,
  `app_msg_x_annotated_already_sent`, `app_msg_no_photos_gallery`, `app_msg_looked_x_gallery_photos` (the new
  template), `app_msg_could_not_read_gallery`; the permission answer (:891) `app_msg_gallery_access_granted`
  / `app_msg_without_gallery_access` (one key, three fragments). THE ANNOTATION RESULTS —
  `app_msg_sent_x_annotated_to_x(n, server)` (:2088, the face{pl} fold removes "face"/"faces"),
  `app_msg_could_not_send_annotation_x` (:2091). THE SHOOT-AND-KEEP FLOW — `app_msg_could_not_split_x`
  (:859), `app_msg_could_not_save_x` (:2699), `app_msg_no_camera_app_x` (:2882),
  `app_msg_x3_capture_failed_x(CameraNote.line(tx, it))` (:2869), and the dims dialog's labels
  `tx.common_survey_prep` / `tx.app_discard_360` (:3667 — whose stale comment "Discard 360 has no key yet"
  comes OUT: the card has held app.discard_360, the page's own shoot-and-keep label, since L1; §24's out-list
  repeated the stale claim and this section corrects it — a note describing a state has to move when the
  state does). THE X3 WIRING — `camNote` becomes `Msg?` (:2465, :2852).
- **Call sites only** (the first four already in ported.txt; the three M8 screens stay on unkeyed.tsv —
  I18nLiteralTest (d) would fail them as ported files with 24/46/122 unkeyed rows — and take only the Tx.EN
  renders): TreeShell.kt — CaptureSheet's `cameraNote: Msg?`, rendered
  `cameraNote?.invoke(tx)` at :495 (a LONG-LIVED note drawn in the language on when it is drawn — the M3b
  readingsMsg rule; its stale KDoc "shown as it says it" reworded), and the project sheet's Client value
  through `Masters.client_name` (:279); BrowseScreens.kt (ClientsScreen title); HomeScreens.kt (the client
  fills); SurveyPrepScreen.kt — `say(n(tx))` / `say(why(tx))` at :456–457, the laser's words now rendered
  live on the ONE keyed screen that shows them; LaserTestScreen.kt / AnnotateScreen.kt / FacePrepScreen.kt —
  their `note` state keeps String and takes `n?.invoke(Tx.EN)` / `why(Tx.EN)` until M8 (the FacePrepDraw
  caller idiom: no mixed screen the prototype never drew; M8's flip is one argument per screen).

**camera/:**
- **X3Bridge.kt (11 → 0, joins ported.txt, ZERO words left).** The connect notes become CameraNote Msgs
  (`joinWifi(notOnCameraWifi)`, `joinWifi(cannotWatchWifi(why))`, `joinWifi({ _ -> CameraNote.NO_CM })`,
  `connectError(code)` — the three-fragment connect-error sentence is ONE key, `app.x3.connect_error_x`,
  whose EN is the concatenation byte for byte); the shoot path throws `CameraNote.Refused(NO_FILE)` /
  `(CAPTURE_ERROR, code)` / `(STITCH_FAILED, code, message)` / `(STITCH_CANCELLED)` in place of its bare
  exceptions — `Result<String>` keeps its shape, the messages keep today's bytes (the pinned Tx.EN render).

**What translates and what stays Latin** (each proven by the served page's screenshots and `vis()`):

| shown | under मराठी | mechanism |
|---|---|---|
| the laser states said on survey prep | ब्लूटूथ सुरू करा, मीटर डिस्कनेक्ट झाला, … | LaserNote Msg, rendered at say() with the live tx |
| a laser note's system tail ("Scan refused: …") | स्कॅन नाकारले: application registration failed | the {why} as given — data |
| the laser LOG, and its lines on the Laser test screen | UNCHANGED (English) | values + Tx.EN renders — the page's own demo log line ruling; Share laser log is an export |
| "DISTO", "DISTO D2", "no ConnectivityManager" | UNCHANGED | device and system identifiers — values |
| the X3 notes on the capture sheet and banner | स्टिच एक्सपोर्ट रद्द झाले, कॅमेरा कनेक्ट त्रुटी 1 — … | CameraNote; the SDK's {why} as given |
| the sign-in sheet | साइन इन करा, ERP ईमेल, पासवर्ड, … | the 8 new keys (decision 6) |
| the stored sign-in refusals, wherever they surface | their app.note.* Marathi | stored English, UploadNote.line — sheet, banner, queue tails alike |
| S5's own error sentence from /api/auth/login | UNCHANGED (as given) | data — the M4 reasonLine rule |
| "(no client)" | (ग्राहक नाही) | `Masters.client_name` + the masters.tsv row |
| client / site / project names, URLs, versions, " (staff)" | UNCHANGED | data; the map passes unknowns through |
| the ImageMeter results | the card's app.msg.* Marathi | lastResult as Msg, rendered at draw |
| the legacy key dialog | सर्व्हर, साइट URL, API की, API सिक्रेट, … | the page's own v53 keys |
| "Update failed: download: HTTP 404" | अपडेट झाले नाही: डाउनलोड: HTTP 404 | app.msg.update_failed_x over app.download_http_x |

**S5 must already serve**

Nothing.

**Tests and checks**

- **LaserNoteTest (new, :pano)**: every builder under `Tx.EN` BYTE-EQUAL to the string DistoClient or Disto
  composes at 80d3526 (all 14); under `Tx.MR` hand-typed pins against the card's values (the vis() answers in
  this derivation); the fills as given ("स्कॅन नाकारले: application registration failed" — tail Latin).
- **CameraNoteTest (new, :pano)**: `text(Tx.EN, …)` byte-equal to X3Bridge's previous literals for every Kind,
  the composed connect-error sentence included; `Refused.message` IS the Tx.EN render (the PlanSave pin);
  `joinWifi` over all three problems; `line` on an alien Throwable keeps its message; `Tx.MR` the card's.
- **DistoTest**: the refusal branches pinned through `LaserNote.refusal` under both Tx (its
  `contains("unit code 4")` becomes a render); Pairing's held-distance logic unchanged.
- **UploadNoteTest grows an M6 block**: `line` on each new constant under both Tx; the only-staff ROUND TRIP
  (`line(tx, onlyStaff(k)) == `the key render, for blank and named kinds, under both Tx — owner and parser one
  file, the conflictNoteLine idiom); the nested SIGN_AGAIN tail now translating whole ("sign in again (menu →
  Server) - ERP did not accept that email and password" → both halves Marathi); an unknown refusal (S5's own
  sentence) unchanged; the constants' bytes pinned against the values 0.3.303 stores in prefs.
- **MastersTest**: `client_name` joins the MAPS walk; the row's EN is S5Session.NO_CLIENT byte for byte.
- **TreeQueueMarathiTest grows an M6 block** (the M3a fix-7 reason — a Masters map passes unknowns through):
  `client_name` both ways ("(ग्राहक नाही)"; a typed name through unchanged), and the threaded crumb/hit/sheet
  fills where :pano functions compose them.
- **The i18n suite**: I18nTableTest (1,814 keys; the sixteen flagged `phone-text` with their where-lists),
  TxRenderTest (the four new templates render), the deny-pattern check (no name, phone number or rate in the
  new keys — "0.3.93" is a version already on the card), I18nLiteralTest (a)–(d) over the 108 removed rows and
  the 9 files joining ported.txt, (f)/(i) over every moved constant and renamed member — and the TYPE changes
  walked BY HAND because (f)/(i) cannot see them (the M3b salvage-log lesson): the two DistoClient template
  sites are rewritten in this slice, and every other reader of `Msg` where a String stood is a compile error
  CI refuses (`say(n)`, the M8 screens' `String?` state, `Text(cameraNote ?: …)`). **(h) UNCHANGED**: no
  region is added or widened — PlanExport, FaceWriter, SurveyExport and AsIsUpload's `fun doWork` stay as M3c
  left them; the laser log and the X3 exception messages render the pinned table (`Tx.EN`) exactly as the
  salvage log does, outside every region.
- The CI gate; `:app` AND `:camera` compile only in CI (push batch-next, wait for android-app.yml) — nothing
  here has been seen on a phone. The changed signatures' readers are walked against the files AS THEY ARE on
  batch-next before the commit (the list in "Derived from" 3).

**What Amit sees**

Under English, nothing: every key's EN renders the current literal byte for byte — the sixteen new keys' EN
ARE today's literals — the stored prefs and state files keep reading (no stored byte moves), the exported
ImageMeter caption and the laser log keep their bytes, and the X3/laser logcat lines read exactly as today.
One recorded edge (the M6 review): a Throwable with a NULL message renders an EMPTY tail at MainActivity's
five `orEmpty()` sites where the old template printed `null` — the house idiom at every earlier-keyed site,
on shown notes only; the laser's two refusals keep the `null` bytes (LaserNote renders `why.toString()`,
pinned), so the laser log holds byte for byte.
Under मराठी the shell FINISHES turning in one build: the sign-in sheet, the legacy key dialog, the ImageMeter
import's every verdict, the shoot-and-keep refusals, the X3's connect and capture notes, the update failure,
and the laser's states on survey prep — which closes survey prep entirely. The Laser test, Face Prep and
Annotate screens stay wholly English in both languages (their notes render Tx.EN until M8) — no mixed screen.

**Risks**

The clipped-Devanagari class (the phone is the only proof; this scope is AlertDialogs, ListItems and banners
with room, the page's overflow probe found nothing, and no Marathi-only line height is pre-emptively added —
the M3a `if (tx.mr)` remedy is one line per element if Amit's phone shows a cut). A refusal stored by an older
build whose bytes match no constant draws as stored — English — the rule working. A say() raised before a
language flip keeps its language for 2.6 s (M3a's accepted rule); the capture sheet's camera note and
lastResult re-render at next draw (Msg, the readingsMsg rule).

**Deliberately NOT in M6**

- **M8** — FacePrepScreen's 122, AnnotateScreen's 46, LaserTestScreen's 24 (the replica's look is captured in
  this derivation's screenshots for it), FacePrep's remaining 17, Annotation's 17; and the three one-argument
  flips to the live tx (the M8 screens' laser-note renders here, FacePrepScreen's canvas call from M3b).
- **The exports and the wire** (decision 4, for good): FaceWriter's 13 rows; the ImageMeter caption and the
  swallowed error (AnnotationPush's two values); the laser LOG and "Share laser log"'s payload; AsIsUpload's
  doWork region; the capture states, folder names, S5 fields, ERP endpoints, BLE GATT names and UUIDs —
  `// text:value` where LiteralScan sees them.
- **No new UI**: the sign-in sheet keeps its exact fields and buttons; M6 translates, it does not redesign
  (a UI state the approved prototype does not draw does not reach the phone — and the sheet not being ON the
  prototype does not license inventing one there either).

**The M6 critic** — raised by a careful reader, answered:

1. *Why do the ~19 laser log fragments stay English when the 12 states translate?* The approved page itself
   rules it: its one demo log line has a key whose MR EQUALS its EN, drawn unchanged under मराठी — and the log
   is what "Share laser log" sends off the phone, a diagnostic built to settle the D2 protocol (the file's own
   KDoc), so decision 4's register applies as it did to ServerMessage and the salvage log. Translating the
   transcript would also bury the GATT vocabulary ("indicate", "CCCD") the log exists to compare against the
   published clients.
2. *Why Msg callbacks for the laser instead of UploadNote-style stored English?* Because nothing STORES a
   laser note: it is a transient callback shown at say() or held in screen state — the storage rule (M4)
   protects stored bytes, and there are none. Msg renders in the language on when it is drawn (the readingsMsg
   rule), and the words still get a :pano home (LaserNote) so both languages are pinned where the suite runs.
3. *A Msg in a string template compiles and prints a lambda — the M3b salvage-log fault. Where are those
   sites?* Walked, not assumed: DistoClient's moveTo event line and its "refused: $why" line are the only two
   templates the changed values reach, and both are rewritten in this slice to render Tx.EN explicitly. Every
   other reader is a TYPE error CI refuses (say wants a String; the M8 screens' note state is String?;
   TreeShell's Text). The bare-Msg scan from the M3b review stays scoped to `.problems` loops — widening it to
   every Msg variable would need type resolution a text scan does not have; the walk is the guard here, and
   the two sites are named so the builder cannot miss them.
4. *The sixteen new keys widen the folder-vs-page diff.* As the three fp.* keys did in M3b: phone-text keys
   with where-lists are the recorded, expected divergence — the page's left-out table itself names these rows
   decision 6. The RAW diff stays the check that no SHARED row drifts.
5. *The banner "Sign in again (menu → Server): {why}" will now translate its tail where the approved page
   shows the tail English.* The page's tail is English only because the sheet's rows do not exist there (its
   vis() answer in this derivation shows exactly that), not because a ruling keeps them English — decision 6
   gives the sheet's words to Claude, and a translated head with an English tail that HAS a Marathi row would
   be the mixed line the M4 rule exists to avoid. S5's own sentences still stay as given.
6. *Why a typed Disto.Refusal instead of a Msg from Disto.kt?* The wire parser stays Tx-free (the
   ServerMessage/AsIsPath idiom): Disto.kt keeps bytes, codes and formats; LaserNote keeps words. A Msg built
   in Disto.kt would put the language table into the one file whose job is the BLE protocol.
7. *`app.discard_360` — §24 and the :3667 comment said it has no key.* WRONG, both: the card has carried it
   (v53build/app.js:363, the page's own shoot-and-keep label) since the tables landed; index.tsv generates
   `app_discard_360`. The stale claim is corrected here and the comment leaves with the build — a note
   describing a state has to move when the state does (this file's most-repeated lesson).
8. *Why does `app.msg.looked_x_gallery_photos` get coined when `app.msg.looked_14_gallery_photos` exists?*
   The static's "14" is the page demo's baked count; a static cannot carry `scan.looked`. The new template's
   EN and MR are the card's own sentence with {n} — nothing is reworded — and the static stays the page's.
9. *The " ({kind})" tail and "Signed in as {name}" — why is kind not keyed?* After the staff gate the only
   kind a signed-in phone can show is "staff" — S5's own stored word, data (the M4 rule); a keyed render would
   translate a value S5 compares. The page has no ruling here (no sheet), so the value mark is the
   conservative reading.
10. *S5Session's constants move to UploadNote — does the sheet's immediate `problem` really go through the
    stored-note mapper?* Yes, deliberately: `problem` is EITHER a fresh login error OR the STORED refusal
    (`S5Session.refusal`, prefs) — one string, two sources, so one mapper (line) or the two drift; and the
    same refusal surfaces in the banner and in the queue's SIGN_AGAIN tails, which already go through line.
    Rendering keys directly in the dialog would be a second answer.
11. *The derivation lives in the scratchpad — the lost-plan fault again?* No (M3b critic 10's answer holds):
    the rulings, the vis() answers, the screenshots' findings and the per-file mapping are IN THIS SECTION,
    committed with it; the run is re-creatable in minutes from the published artifact and this recipe. No
    client name, phone number or rate enters the repo: the probe fills are demo rows, status codes and
    system-exception phrases, and the deny-pattern check runs over the tables as always.

**The M6 critic** (the workflow's second reviewer, read against the source with the build) — three findings,
all three CONFIRMED and applied; none refuted:

1. *FrappeClient.Download as specified cannot compile* — CONFIRMED (major). FrappeClient.kt:35 at 80d3526 is
   `class ApiException(val code: Int, …)`: FINAL, so the specified subclass was one compile error, and its
   `val code` re-declaring the supertype's property a second — both in :app, which only CI compiles, the
   exact path the section claimed to have walked. The bullet above is amended as the critic prescribed:
   `open class ApiException`, `class Download(code: Int)` with no `val` (the inherited `code` serves the
   update flow), SyncWorker:224's typed catch unchanged — and the build ships that shape. The lesson for the
   next walk: "every caller of every changing signature" has to include the SUPERTYPE a new class extends.
2. *HomeScreens:132 leaves the hero card's SITE raw* — CONFIRMED (minor). `resume.site` is
   `p?.site ?: Catalogue.DEFAULT_SITE` (MainActivity:3495) and DEFAULT_SITE is "Main site", a mapped master,
   so under मराठी the Work tab showed "Main site" on the hero card over "मुख्य साइट" in the rooms list below
   it — the same class as the :2506 catch-up this section already records. :132 now threads
   `Masters.site_name` beside `Masters.client_name`, and TreeQueueMarathiTest's M6 block pins the maps.
3. *"Call sites only (all already in ported.txt)" is false for the three M8 screens* — CONFIRMED (minor).
   ported.txt holds TreeShell, BrowseScreens, HomeScreens and SurveyPrepScreen and CANNOT hold the M8 three
   (check (d) fails a ported file with unkeyed rows; they hold 24/46/122). The parenthetical above is
   reworded as prescribed: the M8 screens stay on unkeyed.tsv and take only the `Tx.EN` renders.

**The M6 review (9 Nov 2026)** — two findings on 1848b64, both independently confirmed and both minor; both
fixed:

1. *The M6 pass-through pin named a real client.* TreeQueueMarathiTest's new line pinned
   `Masters.client_name(Tx.MR, "Yogesh S")` — the YS customer of the house's own sku grammar, the exact
   abbreviation MainActivity's shortName KDoc renders — against the slice's rule that no client name enters
   any code, table, test or fixture. Any unknown value proves the map's else-branch equally, so the pin now
   reads `"Asha P"`, a name that is nobody's. No name ever reached the i18n tables (the deny-pattern check
   covers those; masters.tsv's client_name row maps only app.no_client). The full name's PRE-EXISTING
   occurrences — HandoverConventionTest.kt, RoomToken.kt's KDoc, MainActivity.kt's shortName KDoc, all older
   than M6 — are a cleanup of their own, outside this slice, not smuggled into a review fix.
2. *A null exception message rendered empty where 0.3.306 printed `null` — unrecorded, and it touched the
   laser log.* The old `"Scan refused: ${it.message}"` template printed a message-less Throwable's null as
   `null` (Kotlin's template semantics); M6's `LaserNote.scanRefused(it.message.orEmpty())` rendered
   `Scan refused: ` — and DistoClient's moveTo event line feeds that render into the laser LOG, whose bytes
   decision 4 and this section's "What Amit sees" promise unchanged, while the SAME commit pinned the opposite
   choice for the X3 (CameraNote's `why.toString()`, "the old template printed \"null\", and so does this").
   Fixed where the words live, not at the call sites: `scanRefused` and `bluetoothRefused` take `String?` and
   render `why.toString()` (CameraNote's pinned choice), DistoClient passes `it.message` bare, and
   LaserNoteTest pins the null fills — `Scan refused: null`, `Bluetooth refused: null` — which do not even
   COMPILE against the old `String` signature, so the suite is red without the fix. MainActivity's five
   `orEmpty()` sites (could_not_split, update_failed's else-branch, could_not_send_annotation, could_not_save,
   no_camera_app) STAY the house idiom — shown notes, never the log, and a message-less Throwable is rare on
   those paths — and "What Amit sees" above now records that edge instead of denying it: a byte claim with a
   silent exception is the stale-note fault wearing a test.

**unkeyed.tsv** 347 → 239 (108 rows off; what remains is M8's five files plus FaceWriter's 13, decision 4's) ·
**ported.txt** + LaserNote.kt, CameraNote.kt, Disto.kt (pano), DistoClient.kt, S5Session.kt, FrappeClient.kt,
AnnotationPush.kt, MainActivity.kt (app), X3Bridge.kt (camera) · **keys** 1,798 → 1,814 (the sixteen above; Tx
regenerated, Masters.kt regenerated — the NEW `client_name` map, one masters.tsv row) · **literal-counts**
re-ratcheted: every touched file's count stood (each leaving row keyed or marked in the same commit) and the
two new :pano files enter at 0. Built as one commit on batch-next; :pano 966 tests, 0 failures, 0 skipped
(the XML under pano/build/test-results); :app and :camera compile only in CI — nothing here has been seen on
a phone.

### 30. M8 · Marathi: the screens the prototype never had (decision 6)

**Size:** L · **Depends on:** L2

**Goal**

Key the last screens: FACE PREP whole (FacePrepScreen's 122 rows, FacePrep.kt's remaining 17 — the Measure
sheet's Dim labels, the side names, the piece labels §26 left), the LASER TEST screen (LaserTestScreen's 24),
the face VIEWER (AnnotateScreen's 46, Annotation.kt's 17), and FaceWriter's 13 — read row by row, not filed by
file: eight of the thirteen are SHOWN refusals, not export bytes (below). Flip the three loose ends M3b/M6
left: FacePrepScreen's screen Canvas from `Tx.EN` to the live tx (one argument, the M3b plan), the three
screens' note state from `String` + `n?.invoke(Tx.EN)` to `Msg` rendered at the live tx (the M6 call-sites
note), and the dims calls beside them. After M8 unkeyed.tsv is EMPTY for the first time, and the English left
under मराठी is exactly decision 4's — the exports, the wire, the laser log — plus the dormant annotate editor
(below, gated).

**Derived from, in this order**

1. **Decision 6 (Amit, 28 Oct): these screens have NO prototype rows** — so unlike M2–M6 there is no served
   page to drive and no vis() to probe. The authorities are: the CARD ITSELF, which already holds the Laser
   test replica's whole vocabulary (M6's derivation proved the RAW table equals the folder byte for byte, and
   §29's screenshots captured the replica's look for this slice): `app.laser_test_disto_d2`, `app.connect` /
   `disconnect` / `fire` / `laser_on` / `laser_off` / `not_connected` / `looking_meter` / `connecting` /
   `connected_press_measure_button` / `checks_1_press_measure` (ONE key whose EN is the screen's two
   concatenated fragments byte for byte) / `share_laser_log`, and the shared atoms (`common.share/help/close/
   cancel/done/delete/duplicate/line/measure/reset/more_2`, `app.ok`, `app.details`, `app.aria.back`,
   `sp.aria.undo`, `sp.loading`, `sp.msg.laser_still_connecting`); and the SHIPPED REGISTER for every word
   Claude coins — a face is बाजू (`app.cleared_work_face`, `app.msg.could_not_split_x`), a surface पृष्ठभाग, a
   figure आकडा, readings रीडिंग, measures मापे (`app.x_laser_sizes`), the measured frame चौकट
   (`sp.msg.d2_x_ref_left`'s "संदर्भ चौकट"), fit बसवा, derived हिशोबाने, clear-a-figure काढा
   (`life.cleared_x_item_x`), डेप्थ never खोली for depth (the M3b critic's finding 1; खोली stays ROOM:
   `common.room`, `app.room_size_mm`'s "खोलीचा आकार").
2. **The shipped slices**: M3b (FacePrepDraw's tx parameter, both callers `Tx.EN`, the fp.* family and its
   डेप्थ register, "M8 owns its pill metrics", the fromWord/app.from_x pairing, critic 7's locator order);
   M3c (the PlanSave typed-refusal idiom; check (h)'s regions and self-test); M6 (LaserNote as the laser's
   words-home, the three screens' String-note bridge this slice removes, the `orEmpty()` choice at
   MainActivity's five shown-note sites, the M4 reasonLine rule); M2/M4 (Masters threading, identity values
   drawn through maps); the PL review (an identity drawn raw is invisible to every literal check).
3. **The code on batch-next (cdde242)**: all 239 unkeyed rows of the six files read in their files; every
   caller of every changing type listed — `FacePrep.dims` (FacePrepScreen:153,157,442 — the three `Tx.EN`
   calls that flip; `faceStatus` keeps `Tx.EN` internally, it reads only `site`; `csvRows` keeps `Tx.EN` for
   good, the CSV is an export), `FacePrep.LABEL` (FacePrepScreen:501,510,514,560 — shown, threaded through
   `Masters.surface` / `surface_tab`; :887 exportAll's caption and :916 shareCsv — exports, raw),
   `FacePrep.SIDE_NAME` (dims only), the note states (LaserTestScreen:39,54,57,71,101; AnnotateScreen:191,
   255–256,263,265 and its status line :405; FacePrepScreen:116,125,126,149,154,160,163,170,174–175,436–438,
   532,535,536,566 — the busy state's own sites :125/:532/:535 included (the M8 critic's finding 4); every
   assignment becomes a Msg, every render an `invoke`), `FaceWriter`'s throw sites (begin:298,304,
   306; single:555,561,568; decodePano:455; saveToGallery:744,747) and their FOUR catchers (MainActivity:480
   `app_msg_could_not_open_360_x`, :522 `app_msg_survey_prep_saved_not_written_x`, :860
   `app_msg_could_not_split_x`, :2699 `app_msg_could_not_save_x` — each `{why}` flips from
   `e.message.orEmpty()` to `FaceNote.line(tx, e)`; AsIsUpload:486's `fullPano` path stores a thrown message
   into a capture note, and a `Refused.message` IS the pinned Tx.EN render, so the stored bytes cannot move) —
   and every Tx accessor checked to exist in the generated Tx with its exact signature once i18nGen runs (the
   81 below; `Masters.surface`, `surface_tab`, `room` exist with their maps — no masters.tsv row this slice).

**The keys: EIGHTY-ONE new (1,814 → 1,895); the card's laser-test group and the shared atoms consumed, not
re-coined.** All 81 are decision 6 (flag `phone-text`, where-list naming the site at this commit; EN byte for
byte today's literal; MR Claude's, in the shipped register — Amit tests it on the phone, the review this slice
gets). By family:

`fp.*` — FacePrepScreen's chrome and menu (11):
- `fp.no_x_photo_capture_phone` · EN `No {face} photo for this capture on this phone` · MR
  `{face}: या कॅप्चरचा फोटो या फोनवर नाही` ({face} = `Masters.surface(tx, LABEL…)` — the colon in place of an
  uninflectable genitive, the folder's own M3a finalisation style)
- `fp.redo` · `Redo` · `पुन्हा करा` — `sp.aria.undo` (मागे घ्या) is its pair
- `fp.snap` · `Snap` · `स्नॅप`
- `fp.reference_grid` · `Reference grid` · `संदर्भ ग्रिड`
- `fp.grid_settings` · `Grid settings…` · `ग्रिड सेटिंग्ज…`
- `fp.save_photos_imagemeter` · `Save photos for ImageMeter` · `ImageMeter साठी फोटो सेव्ह करा`
- `fp.share_measures_csv` · `Share measures (CSV)` · `मापे शेअर करा (CSV)`
- `fp.laser_connected_tap` · `connected (tap to disconnect)` · `कनेक्ट आहे (डिस्कनेक्टसाठी टॅप करा)` — the
  ⋮ menu's laser row composes `tx.app_laser + " · "` (the join a value) + the state word, so `app.laser`
  (लेझर) is CONSUMED, not re-coined (the M8 critic's finding 2)
- `fp.room_size` · `Room size…` · `खोलीचा आकार…`
- `fp.reset_box_corners` · `Reset box to the corners` · `चौकट पुन्हा कोपऱ्यांवर ठेवा`
- `fp.fit_photo` · `Fit photo` · `फोटो बसवा`

notes and busy lines (9):
- `fp.saving_photos` · `Saving photos…` · `फोटो सेव्ह होत आहेत…`
- `fp.saved_x_imagemeter_photos` · `Saved {n} ImageMeter photos next to the faces (…-faceprep.jpg)` · MR
  `ImageMeter साठी {n} फोटो बाजूंच्या शेजारी सेव्ह केले (…-faceprep.jpg)` (the file token Latin, the wire)
- `fp.no_face_photos_save` · `No face photos found to save` · `सेव्ह करायला बाजूंचे फोटो मिळाले नाहीत`
- `fp.every_figure_measured` · `Every figure on this face is measured` · `या बाजूवरचा प्रत्येक आकडा मोजून झाला`
- `fp.nothing_measure_face_yet` · `Nothing to measure on this face yet` · `या बाजूवर मोजायला अजून काही नाही`
- `fp.x_site_x_mm` · `{label}: site {mm} mm` · `{label}: साइट {mm} mm`
- `fp.laser_tap_figure_fire` · `Laser connected — tap a figure, then fire` ·
  `लेझर कनेक्ट झाला — आकड्यावर टॅप करा, मग फायर करा`
- `fp.tap_figure_yellow_laser` · `Tap a figure to give it its site figure. The yellow button connects the
  laser.` · `आकड्यावर टॅप करा म्हणजे त्याला साइटचा आकडा देता येईल. पिवळे बटण लेझर कनेक्ट करते.`
- `fp.laser_connecting` · `The laser is connecting…` · `लेझर कनेक्ट होत आहे…` (the "still" line is
  `sp.msg.laser_still_connecting`, consumed)

the tool strip and its chips (5):
- `fp.box` · `Box` · `चौकट` — the tool aligns the measured frame, `sp.msg.d2_x_ref_left`'s own word
- `fp.columns` · `Columns` · `कॉलम`
- `fp.dividers` · `Dividers` · `डिव्हायडर`
- `fp.other_step` · `Other step` · `इतर` — the chip's word; stepName already renders "इतर {n}" for the kind
- `fp.electrical_point` · `Electrical point` · `इलेक्ट्रिक पॉइंट` (a near-match pair of `type.e` "Electric
  point", the card's own idiom; Details → `app.details`, Line → `common.line`, Measure → `common.measure`,
  Door/Window/Opening/Column/Beam/Loft → `type.d/w/o/c/b/l`, all consumed)

the laser button (4):
- `fp.disto_ready` · `DISTO ready` · `DISTO तयार` (the device name Latin)
- `fp.connect_laser` · `Connect laser` · `लेझर कनेक्ट करा`
- `fp.finding` · `Finding…` · `शोधत आहे…` (the More-menu's lowercase "finding…" is this key's `.lowercase()`
  — a Devanagari no-op, the cannotGo pattern; so is `app.connect`'s for "connect")
- `fp.linking` · `Linking…` · `जोडत आहे…` (the menu's lowercase "linking…" is this key's `.lowercase()`
  likewise — finding 2 named it beside `fp.finding`'s)

the keypad sheet (4):
- `fp.calculated_x_x_measured` · `calculated {calc} · {done}/{all} measured` ·
  `हिशोबाने {calc} · {done}/{all} मोजले`
- `fp.mm_site` · `mm · site` · `mm · साइट`
- `fp.clear` · `Clear` · `काढा` — removes ONE site figure (the register's cleared-items word), not
  `sp.clear_surface`'s रिकामा करा, which empties a surface
- `fp.save_and_next` · `Save & next` · `सेव्ह आणि पुढचा` — drawn `"✓ " + …`, the glyph a value; `"◉ Fire"` is
  `"◉ " + tx.app_fire`, no key

the Room box dialog (8):
- `fp.room_box_mm` · `Room box, mm` · `खोलीची चौकट, mm`
- `fp.height` · `Height` · `उंची`
- `fp.width_left_right` · `Width ↔ (left to right)` · `रुंदी ↔ (डावीकडून उजवीकडे)`
- `fp.length_front_back` · `Length ↕ (front to back)` · `लांबी ↕ (समोरून मागे)`
- `fp.lens_height` · `Lens height` · `लेन्सची उंची`
- `fp.camera_left_right_centre` · `Camera left ↔ right of centre` · `कॅमेरा मध्यापासून डावीकडे ↔ उजवीकडे`
- `fp.camera_back_front_centre` · `Camera back ↕ front of centre` · `कॅमेरा मध्यापासून मागे ↕ पुढे`
- `fp.room_sizes_reshape` · `Room sizes reshape every face. A wall's own figures (Measure) never move anything
  — they are kept beside the calculated ones.` · `खोलीच्या मापांनी प्रत्येक बाजू नव्याने आखली जाते. भिंतीचे
  स्वतःचे आकडे (मोजा) काहीही हलवत नाहीत — ते हिशोबाच्या आकड्यांशेजारीच राहतात.`

the Reference grid dialog (9):
- `fp.across_mm` · `Across mm` · `आडवे mm` · `fp.up_mm` · `Up mm` · `उभे mm`
- `fp.bold_every_mm` · `Bold every mm (0 = none)` · `दर इतक्या mm ला ठळक (0 = नाही)`
- `fp.count_across_from` · `Count across from` · `आडवे कुठून मोजायचे`
- `fp.count_up_from` · `Count up from` · `उभे कुठून मोजायचे` (the chips Left / Right / Floor / Ceiling are
  `surface.left.short` / `right.short` / `surface.floor` / `surface.ceil`, consumed — EN byte for byte)
- `fp.shift_across` · `Shift across` · `आडवे सरकवा` · `fp.shift_up` · `Shift up` · `उभे सरकवा`
- `fp.strength` · `Strength` · `गडदपणा`
- `fp.mm_labels` · `mm labels` · `mm आकडे`

the selection dialog (4):
- `fp.what_it_is` · `What it is` · `हे काय आहे`
- `fp.divide_longest_half` · `Divide the longest piece in half` · `सगळ्यात लांब भागाचे दोन समान भाग करा`
- `fp.or_dividers_tap_line` · `Or use Dividers and tap the line where it should be divided.` ·
  `किंवा डिव्हायडर वापरा आणि जिथे भाग पाडायचा तिथे रेषेवर टॅप करा.`
- `fp.nothing_selected` · `Nothing selected.` · `काहीही निवडलेले नाही.`

Help, the title and the share (9):
- `fp.face_prep` · `Face Prep` · `बाजूंची तयारी` — the `common.survey_prep` register (सर्व्हे तयारी), a
  translation, not a transliteration: the screen prepares the capture's faces
- `fp.hint.box` · (the Box hint, FacePrepScreen HINT) · `कोपऱ्याचा ग्रिप किंवा चौकटीची रेषा ओढून खोलीच्या
  कोपऱ्याच्या रेषांवर ठेवा. स्प्लिटने बनलेल्या एलिव्हेशनवर चौकट आधीच कोपऱ्यांवर असते.`
- `fp.hint.steps` · (the Columns hint) · `कॉलम, बीम किंवा इतर निवडा, मग बाजूचा जो भाग तो व्यापतो त्यावर आयत
  ओढा. जुळवण्यासाठी कड, कोपरा किंवा मधला भाग ओढा.`
- `fp.hint.edges` · (the Dividers hint) · `रेषा, दरवाजा/खिडकीचा बाण किंवा चौकटीची कड विभागण्यासाठी त्यावर टॅप
  करा; डिव्हायडर सरकवण्यासाठी ओढा — गाईड दोन्ही बाजूंचे mm दाखवते.`
- `fp.hint.dets` · (the Details hint) · `प्रकार निवडा आणि त्यावर आयत ओढा — जिथून मापे मोजली जातात त्या
  कोपऱ्यापासून सुरुवात करा. निवडण्यासाठी टॅप करा: हटवा, प्रत बनवा, प्रकार बदला.`
- `fp.hint.lines` · (the Line hint) · `कोणत्याही दिशेने सरळ रेषा ओढा (3° च्या आत आडवी किंवा उभी स्नॅप होते).
  ताणण्यासाठी टोक ओढा, हलवण्यासाठी रेषा ओढा.`
- `fp.hint.measure` · (the Measure hint) · `कोणत्याही आकड्यावर टॅप करा: त्याचा बाण पांढरा होतो आणि कीपॅड
  उघडतो. D2 फायर करा (पिवळे बटण, किंवा D2 चे स्वतःचे) किंवा साइटचा आकडा टाइप करा, मग सेव्ह आणि पुढचा.
  हिशोबाचा आकडा राहतोच; साइटचा आकडा शेजारी हिरव्या रंगात दिसतो आणि ImageMeter फोटोत व CSV मध्ये जातो.`
- `fp.two_fingers_zoom` · `Two fingers zoom and move the photo. Undo and redo are in the top bar; snap, grid,
  saving and the laser are in ⋮.` · `दोन बोटांनी फोटो झूम आणि सरकवता येतो. मागे घ्या / पुन्हा करा टॉप बारमध्ये;
  स्नॅप, ग्रिड, सेव्ह आणि लेझर ⋮ मध्ये.`
- `fp.share_measures` · `Share measures` · `मापे शेअर करा` — the chooser's title, Android's transient UI,
  keyed as `export.share_x` was (M3c critic 3); the CSV itself stays decision 4's

`fp.*` — FacePrep.kt's Dim labels (6):
- `fp.top_total` / `fp.right_total` / `fp.bottom_total` / `fp.left_total` · EN `Top total` / `Right total` /
  `Bottom total` / `Left total` · MR `वरची एकूण` / `उजवी एकूण` / `खालची एकूण` / `डावी एकूण` — four WHOLE keys,
  the DeleteRoute gendered idiom, never one template
- `fp.step_face` · `step face` · `स्टेपची कडा` — कडा DELIBERATELY, where the shipped face word is बाजू
  (the M8 critic's finding 6): the Dim is an edge piece and its siblings are वरची कडा / उजवी कडा …, so the
  family word wins; if Amit's phone prefers बाजू it is a one-row card edit. The edge-piece label's fallback;
  the sides compose
  `"E${n + 1} · "` (a value) + `edge.top_edge` / `right_edge` / `bottom_edge` / `left_edge` (consumed — their
  EN IS `SIDE_NAME.lowercase() + " edge"` byte for byte)
- `fp.piece_x` · EN ` piece {k}` · MR ` भाग {k}` — the leading-space fragment (`sp.laser_suffix`'s shape;
  भाग is `sp.segment`'s own word); width/height compose `"$dn " + tx.measure_width / measure_height`
  ("Door 1 width piece 2" → "दरवाजा 1 रुंदी भाग 2"), the locator composes `"$dn " + FacePrep.fromWord(tx, to)`
  (M3b's pairing, now live), a Line composes `tx.common_line + " ${i + 1}"` ("रेषा 1")

`ann.*` — the face viewer's live texts (5):
- `ann.pinch_zoom_drag` · `Pinch to zoom · drag to move around` · `दोन बोटांनी झूम करा ·
  सरकवण्यासाठी ओढा` (the `app.pinch_zoom_double_tap` register for the first half; ओढा for drag-to-pan
  since the M8 review — the first build coined ड्रॅग करा here, the table's only use of the loanword)
- `ann.face_image_gone_phone` · `This face's image is no longer on the phone.` · `या बाजूचा फोटो आता या
  फोनवर नाही.`
- `ann.tap_face_look` · `Tap a face to look at it.` · `बाजूवर टॅप करून ती पाहा.`
- `ann.image_not_phone` · `image not on this phone` · `फोटो या फोनवर नाही`
- `ann.tap_to_view` · `tap to view` · `पाहण्यासाठी टॅप करा`

`refuse.fw.*` — FaceWriter's shown refusals (7), rendered by the NEW words-home below:
- `refuse.fw.could_not_open_photo` · `could not open the selected photo` · `निवडलेला फोटो उघडता आला नाही`
- `refuse.fw.not_decodable` · `not a decodable image` · `ही इमेज वाचता येत नाही`
- `refuse.fw.not_360_x_x` · `not a 360 photo ({w}×{h} is not 2:1) — export the 360 from the Insta360 app
  first` · `360 फोटो नाही ({w}×{h} म्हणजे 2:1 नाही) — आधी Insta360 अ‍ॅपमधून 360 एक्सपोर्ट करा` — ONE key for
  the two concatenated fragments (the `app.x3.connect_error_x` idiom); Insta360 Latin, a product name
- `refuse.fw.could_not_decode_selected` · `could not decode the selected photo` · `निवडलेला फोटो वाचता आला
  नाही`
- `refuse.fw.could_not_decode_photo` · `could not decode the photo` · `फोटो वाचता आला नाही`
- `refuse.fw.gallery_refused` · `gallery refused the file` · `गॅलरीने फाइल नाकारली`
- `refuse.fw.could_not_write_gallery` · `could not write to the gallery` · `गॅलरीत लिहिता आले नाही`

Tx IS regenerated (`nice -n 15 gradle :pano:i18nGen --offline`, then `nice -n 15 gradle :pano:i18nRatchet
--offline`), never hand-edited; the regen is a no-op on Masters.kt this time (no masters.tsv row — `surface`,
`surface_tab` and `room` already carry every identity this slice shows). **latin-ok.txt grows the Latin runs
the new MRs carry** — CSV, faceprep and jpg are NEW entries with where-lists, DISTO and Insta360 each gain a
where-list entry (fp.disto_ready, refuse.fw.not_360_x_x), and ImageMeter, mm and D2 are already allowed with
no where-list, so they gain nothing (the M8 critic's finding 4) — TxRenderTest's leak scan refuses any other,
and an entry nothing needs fails, so the list stays exactly what the tables use.

**One new :pano word-home, no shown literal, joins ported.txt — FaceNote.kt**, the PlanSave idiom byte for
byte (M3c critic 2's reason, a third time: check (h) scans the whole of FaceWriter.kt and refuses every Tx
token but the pinned literal `Tx.EN`, so the words move to where a tx can reach them and a :pano test can pin
them): `enum Kind { OPEN, NOT_IMAGE, NOT_360, DECODE_SELECTED, DECODE, GALLERY, WRITE }`,
`text(tx, kind, w, h)` over the seven `refuse.fw.*` keys, `class Refused(kind, w, h) : Exception(text(Tx.EN,
…))` — a logcat line and AsIsUpload's stored note keep today's bytes exactly — and `line(tx, e: Throwable)`
rendering a Refused through `text(tx, …)` and ANY OTHER Throwable as `e.message.orEmpty()` — NOT M3c's
`?: simpleName`: the four catchers are MainActivity's shown-note sites, whose `orEmpty()` idiom the M6 review
pinned, and line() must not change their null-message bytes.

**Files and changes** (unkeyed.tsv rows before → after; 239 → 0, the worklist EMPTIES)

**pano/:**
- **FaceNote.kt (NEW, joins ported.txt).** Above.
- **FacePrep.kt (17 → 0, joins ported.txt).** `dims(tx, …)` composes every `Dim.label` through the tx it
  already takes: the edge pieces (`"E${n + 1} · "` + the `edge.*` word, else `fp.step_face`), the four totals
  (the whole keys), a detail's `"$dn " + measure_width / measure_height [+ fp_piece_x(k)]`, the locator
  `"$dn " + fromWord(tx, loc.to)`, a Line `common_line + " ${i + 1}" [+ fp_piece_x(k)]` — EN byte for byte
  today's compositions. LABEL and SIDE_NAME become `// text:value` identity maps (shown through
  `Masters.surface` / `surface_tab` and the edge words; raw in the CSV and the burned caption, the exports);
  a NEW `chipWord(tx, label)` beside `typeWord` gives the full-label words ("Column" → `type.c` … "Other
  step" → `fp.other_step`, "Electrical point" → `fp.electrical_point`, unknown as itself) for the chips and
  the selection dialog — in :pano, so the English-byte map is pinned where the suite runs. `csvRows` and
  `faceStatus` keep their internal `Tx.EN`, each with its why in a comment.
- **Annotation.kt (17 → 0, joins ported.txt, stays Tx-FREE).** Every row marked `// text:value`: the five
  `Kind.label` values are identities a DORMANT screen draws (through `type.*` when the editor returns), the
  five `Unit.label` values are units (the page draws units Latin), and the seven metric / imperial fragments
  and joins are figures. No signature moves.

**app/:**
- **FacePrepScreen.kt (122 → 0, joins ported.txt — the FacePrep idiom ends).** `val tx = LocalTx.current`
  (the M3a pattern; a language flip recomposes the screen, so the Canvas lambda is rebuilt and redraws — no
  `remember` key needed, unlike SurveyPlanScreen's cached drawing). THE FLIPS: the screen Canvas passes
  `FacePrepDraw.draw(nc, tx, …)` (:494 — the one argument M3b promised; exportAll's `draw(c, Tx.EN, …)` at
  :882 STAYS, pinned by (h)'s companion), the three `dims(Tx.EN, …)` become `dims(tx, …)`, and `note` / `busy`
  become `Msg?` — every assignment a `{ tx -> … }` lambda over the keys above, rendered at draw
  (`(busy ?: note)?.let { m -> Text(m(tx)…) }`), the M3b readingsMsg rule; `disto.onState` / `onRefused`
  hand their Msg straight to the state (the `n?.invoke(Tx.EN)` bridge and its comment leave — a note
  describing a state moves when the state does), and the BLE-refusal literal becomes the byte-for-byte key
  `sp.msg.bluetooth_refused` — the Msg SurveyPrepScreen:427 already shows on the SAME launcher-deny event —
  never `LaserNote.connectPermissionRefused`, whose EN is the GATT event's different sentence (the M8
  critic's finding 1). take()'s site note snapshots its `label` from `dims(tx, …)` at say
  time (the M3a say() rule: a 2.6 s note is not redrawn across a flip) and renders `fp_x_site_x_mm` live.
  The face words thread `Masters.surface` (top bar + " ▾", the dropdown, the no-photo fill) and
  `Masters.surface_tab` (the tabs, on `LABEL.getValue(fName).removeSuffix(" wall")` — the suffix operand
  marked `// text:value`). FP_TOOLS keeps (mode, glyph) and its labels move to a `when(mode)` of tx reads;
  HINT becomes `when(mode)` over the six `fp.hint.*` (no English-byte matching, so :app is the right home —
  the words are pinned in the TABLE, not in a mapper). shareCsv takes the chooser title as a plain String
  parameter (`tx.fp_share_measures` at the call — the M3c share() idiom) and its CSV header, rows and
  EXTRA_SUBJECT stay `// text:value` (the export); exportAll's caption line likewise. The keypad digits,
  glyphs ("←", "↶", "↷", "⋮", "✕", "⧉", "⚙", "🗑", "◉", "—", "⌫", "┆", "╱", "▢", "▥", "⊡", "↔"), "$roomName ·
  $deviceId" and the " — " / " · " joins are values (⌫ joined the glyph list — the M8 critic's finding 4).
- **FacePrepDraw.kt (no rows; the M3b "M8 owns its pill metrics" debt).** `tag` / `dimTag` pick their paint
  PER STRING: a tag with a Devanagari code point (`'ऀ'..'ॿ'`) draws in `Typeface.DEFAULT_BOLD` with
  the pill sized by the paint's own FontMetrics (ascenders and matras clear the box); every other tag keeps
  MONOSPACE and today's 1.3-em pill — so under English, and in every export (Tx.EN can render no Devanagari),
  not one pixel moves, and under मराठी the worded tags (stepName, detName, the fromWord locator, the fp.*
  guide labels) stop clipping instead of drawing matras into the pill's edge. Both placers already
  `measureText` the real string (M3b), so a longer Marathi word moves its tag rather than losing it.
- **LaserTestScreen.kt (24 → 0, joins ported.txt).** `note` becomes `Msg?` (rendered
  `note?.let { "\n" + it(tx) }`, the join a value); the chrome renders the card's replica keys (title
  `app.laser_test_disto_d2`, Back `app.aria.back`, Share `common.share`, the chooser `app.share_laser_log`,
  the state line's four `app.*` statics, the buttons `app.connect / disconnect / fire / laser_on / laser_off`,
  the checks note `app.checks_1_press_measure` replacing its two fragments); the BLE-refusal literal becomes
  `sp.msg.bluetooth_refused` (finding 1, as on FacePrepScreen). THE LOG STAYS ENGLISH WHOLE (decision 4; the page's demo-log ruling,
  M6 critic 1): the readout "$it mm" / "— mm", the clock join, "READING … (unit code …)", "connect pressed",
  "permission refused" and the share payload's header line are `// text:value` — the card's own
  `app.connect_pressed` / `found_disto_d2_4512` (flag diagnostic-log, MR == EN) are the PAGE's demo rows and
  are not rendered here: threading a tx into the log path to produce identical bytes would be a second answer.
- **AnnotateScreen.kt (46 → 0, joins ported.txt).** The LIVE texts key: the title threads
  `Masters.surface_tab(tx, Handover.FACE_LABELS[face] ?: face)` (the four walls translate; "Top" / "Bottom"
  pass through Latin — M3c's own vis() ruling, and masters.tsv's surface_tab comment already names
  Handover.FACE_LABELS as this map's reader), Back → `app.aria.back`, the viewer hint → `ann.pinch_zoom_drag`,
  the gone-image line → `ann.face_image_gone_phone`; FacesScreen's title threads `Masters.room` on
  `capture.room` (the PL-review class: a room name drawn raw today), its hint → `ann.tap_face_look`, the rows
  thread `surface_tab` with `ann.image_not_phone` / `ann.tap_to_view`. `distoNote` becomes `Msg?` with the
  onState/onRefused wiring as above and the launcher-deny on `sp.msg.bluetooth_refused` (finding 1). THE DORMANT EDITOR'S ~39 ROWS ARE MARKED, NOT KEYED: everything behind
  `ANNOTATION_ENABLED = false` (the toolbar, the three dialogs, the drawn labels, the laser button, the
  selection hints, the counts row — "+ Measure", "Record", "What is this?", "voice note" and their kin) takes
  `// text:value dormant: behind ANNOTATION_ENABLED = false — the held annotator (ImageMeter is the tool);
  key it through the then-current card before the switch flips`, because a text NO PERSON CAN SEE is what
  every existing mark has in common (M2 critic 10's "ERROR" / "work"), and Claude's Marathi there could never
  meet its only review, Amit's phone. The licence is ENFORCED, not written down (critic 2 below).
- **FaceWriter.kt (13 → 0, joins ported.txt; STAYS an (h) whole-file region with the `Tx.EN` exemption
  alone).** The eight refusal literals become `throw FaceNote.Refused(…)` (`FaceNote` is no (h) token; the
  `require` / `requireNotNull` / `error` wrappers become plain throws — every catcher is a `runCatching`, no
  caller types the old IllegalArgument/IllegalState, checked in the walk) — the two-fragment not-360 sentence
  leaves as ONE Refused whose message is today's concatenation byte for byte. The five drawn rows stay
  `// text:value` (decision 4): " (from photo)", "$label elevation", "$label survey marks" and the figures are
  the survey sheets' and captions' own bytes. The `drawMarks(Canvas(flat), Tx.EN, …)` render is untouched.
- **MainActivity.kt (already in ported.txt; call sites only).** The four FaceWriter catchers' `{why}` fills
  become `FaceNote.line(tx, e)` inside their existing `{ tx -> … }` lambdas (:479, :522, :860, :2699) — a
  Refused translates with the live language, any other exception keeps M6's `orEmpty()` bytes.

**What translates and what stays as it is** (register sources named above; no page to screenshot — decision 6):

| shown | under मराठी | mechanism |
|---|---|---|
| Face Prep's chrome, menu, dialogs, keypad, tools | the fp.* MRs above | the 63 fp.* screen keys + the card's atoms |
| the face names and tabs, everywhere live | समोरील भिंत / समोर / जमीन / छत … | `Masters.surface` / `surface_tab` on LABEL / FACE_LABELS |
| the Measure sheet's Dim labels and the site note | दरवाजा 1 रुंदी भाग 2 · वरची एकूण · {label}: साइट … | `dims(tx)` compositions |
| the words DRAWN on the photo (names, locators, guides) | कॉलम 1 · 230 जमिनीपासून · the fp.* labels | the M3b word functions — the Canvas flip turns them on |
| the Laser test chrome | लेझर चाचणी · DISTO D2, कनेक्ट करा, फायर … | the card's replica keys, consumed |
| the laser states on all three screens | LaserNote's MRs, live | the note states become Msg |
| the laser LOG, its readout, Share's payload | UNCHANGED (English) | values — decision 4, the demo-log ruling |
| the face viewer and FacesScreen | the ann.* MRs; rooms through the master | ann.* + `Masters.room` |
| a failed split / save / open / rewrite's {why} | the refuse.fw.* MRs after M6's keyed heads | `FaceNote.line` — no more mixed line |
| the ImageMeter photos, the CSV, captions, filenames | UNCHANGED for good | Tx.EN pins, (h), values |
| the dormant annotate editor | UNCHANGED (English), gated | marked; the gate test below |
| "DISTO", "D2", "ImageMeter", "Insta360", codes, digits | UNCHANGED | identifiers and figures — values |

**S5 must already serve**

Nothing.

**Tests and checks**

- **FaceNoteTest (new, :pano)**: `text(Tx.EN, …)` byte-equal to FaceWriter's eight previous literals (the
  composed not-360 sentence included); `Refused.message` IS the Tx.EN render; `line` on a Refused under both
  Tx, on an alien Throwable `e.message.orEmpty()` — the orEmpty choice pinned with a null-message Throwable
  (the M6 review's lesson: a byte claim with a silent exception is the stale-note fault wearing a test);
  `Tx.MR` the table's values.
- **FacePrepMarathiTest (new, :pano — the M3a fix-7 reason)**: hand-typed MR pins for the dims compositions
  ("दरवाजा 1 रुंदी", "दरवाजा 1 रुंदी भाग 2", "वरची एकूण", "E1 · वरची कडा", "स्टेपची कडा" fallback, "रेषा 1",
  the locator "दरवाजा 1 जमिनीपासून"), `chipWord` both ways (every chip label, an unknown kind through
  unchanged), and the EN side byte-equal to cdde242's labels — plus `dims(Tx.EN)` and `dims(Tx.MR)` on one
  face differing ONLY in label text, never in keys, calc or site (the SurveyDrawMarathiTest figure-items
  shape).
- **FacePrepTest, DistoTest, LaserNoteTest**: unchanged but for `Tx.EN` where dims' labels reach them —
  their English literals are the byte pins.
- **I18nLiteralTest gains `the annotate editor's marks are licensed by its gate`**: reads AnnotateScreen.kt's
  scan and FAILS unless `ANNOTATION_ENABLED = false` is in the source — so the day the editor returns, the
  suite goes red naming the recipe (key the editor's rows through the then-current card, remove the dormant
  marks) instead of ~39 marked English strings silently reaching the screen. A hazard written down in a mark
  is not a hazard handled; this is the enforcement.
- **The i18n suite**: I18nTableTest (1,895 keys; the 81 flagged `phone-text` with their where-lists),
  TxRenderTest (the six new templates render; the latin-ok.txt where-lists hold), the deny-pattern check (no
  name, phone number or rate in the new keys — the MRs above carry none), I18nLiteralTest (a)–(d) over the
  239 removed rows and the SEVEN files joining ported.txt, (f) over every new accessor, (i) over every moved
  member — and the TYPE changes walked BY HAND because (f)/(i) cannot see them (the M3b salvage-log lesson):
  the three note states (every reader is in the same three files, listed in "Derived from" 3 — a missed
  ASSIGNMENT is `String?` taking a Msg, a compile error CI refuses; the one TEMPLATE reader of a note state,
  LaserTestScreen:101's `"\n$it"`, WOULD compile on a Msg and print the lambda, so it is rewritten in this
  slice as `"\n" + it(tx)`, the join a value — the M8 critic's finding 3 corrected the claim that no
  template takes any of them), dims' callers, and FaceWriter's thrown types (all four catchers are
  `runCatching`).
  **(h) UNCHANGED**: the regions stay PlanExport, FaceWriter, SurveyExport and AsIsUpload's `fun doWork`;
  FaceWriter's region yields ZERO UNEXEMPT mentions (exportBuilderMentions is empty): the pinned `drawMarks`
  `Tx.EN` and its import stay, exactly as before — M8 adds no Tx mention to the file (the M8 critic's
  finding 5 corrected the first wording, "ZERO Tx mentions at all", which contradicted the companion). The
  companion still pins exportAll's `FacePrepDraw.draw(c, Tx.EN,` — the screen call it must NOT match is
  `draw(nc, tx,` — and `FaceNote` is none of (h)'s tokens, exactly as `PlanSave` is not.
- The CI gate; `:app` compiles only in CI (push batch-next, wait for android-app.yml) — FacePrepScreen is the
  largest file this port has keyed WHOLE in one slice (MainActivity, at 3,700+ lines the larger file, went over
  M2 and M6 — the M8 critic's finding 4) and nothing here has been seen on a phone; the readers' walk against
  the files AS THEY ARE on cdde242 is the stand-in, as every M-slice before it.

**What Amit sees**

Under English, nothing: every key's EN renders the current literal byte for byte — the compositions included
("Laser · connect" via `.lowercase()`, "✓ Save & next" and "◉ Fire" via glyph joins, "E1 · top edge",
"Door 1 width piece 2", "Top total") — the exports, the CSV, the burned captions, the laser log and every
stored byte (AsIsUpload's fullPano note included) hold exactly, and not one pixel of an exported photo moves
(the Devanagari-gated pill paint cannot fire under Tx.EN). Under मराठी the LAST screens turn in one build:
Face Prep whole — chrome, menu, dialogs, keypad, AND the photo's drawn names, locators and guide labels (the
M3b words, lit by the one-argument flip) — the Laser test's chrome over its deliberately-English log, the
face viewer, and the shoot-and-keep refusals' tails after M6's keyed heads. The dormant annotate editor and
decision 4's exports stay English, said so and gated. unkeyed.tsv is EMPTY; what remains for #31 VIS is only
ported.txt naming every remaining source, not a text.

**Risks**

The clipped-Devanagari class, now on CANVAS pills too — the Devanagari-gated FontMetrics pill is sized by the
paint, but the phone is the only proof of Compose's and Skia's drawing (the M3a sentence); if Amit's phone
shows a cut, the remedy is per-element, one line. Claude's Marathi here gets NO page preview at all (decision
6's bargain): the phone test IS the review, so a reworded MR is a one-row card edit, and the locator's
figure-first order ("230 जमिनीपासून", M3b critic 7) is explicitly on Amit's phone list. A say-time snapshot
keeps its language for 2.6 s (M3a's accepted rule). FacePrepScreen compiles only in CI; the walk is the guard.

**Deliberately NOT in M8**

- **The tour** — its own slice, after M8 (the quiet run's order).
- **The exports and the wire** (decision 4, for good): the CSV (header, rows, subject), the ImageMeter photos
  and their burned captions, the survey sheets, the laser LOG and "Share laser log"'s payload, Handover's
  FACE_LABELS and filenames, STEP_KINDS / DETAIL_TYPES / EDGE_NAMES / LABEL / SIDE_NAME as stored kinds and
  identity maps, the Dim keys — `// text:value` where LiteralScan sees them.
- **The dormant annotate editor's Marathi** — marked and gate-tested, above; keying it is the slice that
  revives the editor, against the card as it stands THEN.
- **No new UI, no rewording**: M8 translates the screens as they are; a UI state nothing draws today does not
  appear, and the sign-off on every MR is Amit's phone, not a prototype.

**The M8 critic** — raised by a careful reader, answered:

1. *§29 filed "FaceWriter's 13 rows" under decision 4 "for good" — now eight of them translate. Which is
   wrong?* §29's out-list, and it is corrected here: it filed the FILE, not its rows. The read shows five
   rows are export bytes (drawn on sheets and captions) and eight are SHOWN refusals — the {why} of the four
   M6-keyed heads ("Could not split: …"), which Amit reads on a banner, not in a file. A Marathi head with an
   English tail that HAS a Marathi row is the mixed line the M4 rule exists to avoid (M6 critic 5's own
   test). A note describing a state has to move when the state does — this one described a file list.
2. *~39 marked rows on a screen is a new use of `// text:value` — the stale-mark hazard.* The criterion every
   existing mark shares is "no person sees this text" (the wire, a log, a compared state), and a literal
   behind `ANNOTATION_ENABLED = false` — false since 2026-08-20, superseded by ImageMeter — is exactly that.
   What is NEW is that the licence can lapse (someone flips the gate), so the licence is a TEST, not a
   comment: the suite goes red the moment the gate is not `false`, naming the recipe. The alternatives were
   worse: keying them ships ~35 Marathi strings no phone can ever show Amit (decision 6's review is the
   phone), and leaving them listed keeps a worklist of texts no person sees, which the worklist's own header
   says it is not for.
3. *Why does `fp.face_prep` translate ("बाजूंची तयारी") when "DISTO D2" stays Latin?* The register's own
   split: product and device identifiers stay Latin (the page's ruling, M6), while SCREEN names translate —
   `common.survey_prep` is "सर्व्हे तयारी" on the shipped card, and Face Prep is that family's sibling. If
   Amit's phone prefers the spoken "फेस प्रेप", it is a one-row card edit.
4. *Why four whole keys for the side totals instead of `fp.x_total`?* Gender: डावी / उजवी / वरची / खालची
   agree with the feminine कडा, and a {side} fill cannot inflect — the DeleteRoute whole-keys-per-kind idiom
   (M2), third use.
5. *The pill paint switches typeface per string — a mixed-font canvas?* Per STRING, not per word: a worded
   Marathi tag draws whole in DEFAULT_BOLD, a code-and-figures tag ("E1 905", "T:… $tot mm") stays MONOSPACE
   in both languages, and no English or exported render can carry Devanagari, so the only screens that can
   show both fonts side by side are the ones the prototype never drew — Claude's to decide (decision 6), and
   the figures' tabular MONOSPACE is why the split exists.
6. *`dims` recomputes labels per render — and the site note snapshots its label. Two behaviours?* Deliberate,
   each the shipped rule for its surface: the SHEET is long-lived, so it renders live (readingsMsg, M3b); the
   NOTE lives 2.6 s, so it keeps the language it was said in (M3a's say() rule) — the laser states on the
   same screens do the same through their Msg state, rendered at the draw that follows the flip.
7. *The card holds `app.connect_pressed` and nine more diagnostic-log keys — why mark the phone's log lines
   instead of rendering those keys?* Those keys exist for the PAGE's demo replica (their MR equals their EN;
   latin-ok.txt's header exempts them from the leak scan). The phone's log is decision 4's: rendering a key
   whose MR == EN changes no byte TODAY but threads a tx into the one path whose English is load-bearing, and
   a later MR edit to a "demo" key would then silently translate a diagnostic other parties read — the
   two-answers fault with a delay on it.
8. *AnnotateScreen's title now translates the four walls but not Top / Bottom — a mixed dropdown?* It is the
   shipped behaviour of every surface_tab reader (the chips, the net, the plan), the page's own vis() ruling
   (M3c captured "Top" / "Bottom" unchanged), and masters.tsv's surface_tab comment has named
   Handover.FACE_LABELS a reader of this map since M3c — M8 is the slice that makes the comment true.
9. *The derivation has no prototype run — is that a thinner authority than M2–M6's?* It is the SAME authority
   decision 6 grants: for these screens the prototype never had rows, so a Playwright run would prove only
   absence (M6's derivation already proved the sign-in sheet comes back unchanged or mis-composed). What CAN
   be checked mechanically is checked: the card keys consumed here were proven equal to the folder byte for
   byte in M6's RAW diff; the register words are quoted from shipped keys by name; and every EN is pinned
   byte for byte by the suite. No client name, phone number or rate enters the repo; the deny-pattern check
   runs over the tables as always.
10. *81 new keys is the biggest coinage of the port — is the card still "the one home"?* Yes — the keys are
    ADDED to android/i18n (the one home) with `phone-text` flags and where-lists, exactly as the three fp.*
    keys were in M3b and the sixteen in M6; the RAW diff stays the check that no SHARED row drifts, and the
    phone-text flag is the recorded, expected divergence from the page (M6 critic 4). The size is the scope's:
    these are the screens the prototype never had, so nothing on the card could already hold their words —
    which is also why the card's 16 laser-test rows that DO exist are consumed rather than re-coined.

**unkeyed.tsv** 239 → 0 (the worklist EMPTIES; every row keyed or marked in the same commit) · **ported.txt**
+ FaceNote.kt (new), FacePrep.kt, Annotation.kt (pano), FacePrepScreen.kt, AnnotateScreen.kt,
LaserTestScreen.kt, FaceWriter.kt (app) · **keys** 1,814 → 1,895 (the 81 above; Tx regenerated, Masters.kt
untouched — no masters.tsv row) · **latin-ok.txt** + the where-listed tokens above · **literal-counts**
re-ratcheted (every touched file's leaving rows keyed or marked in the same commit; FaceNote.kt enters at 0).
To be built as one commit on batch-next; :pano totals from the XML under pano/build/test-results; :app
compiles only in CI — nothing here will have been seen on a phone until Amit's one update from Play.

**The M8 critic (build review, 9 Nov 2026)** — six findings on §30 as written, each checked against the
source before the build; ALL SIX CONFIRMED and applied, in the text above and in the commit:
1. *(major)* The BLE-refusal literal is "Bluetooth permission refused — the laser can't connect" on all
   three screens — byte for byte `sp.msg.bluetooth_refused`'s EN, the key SurveyPrepScreen:427 already shows
   on the SAME launcher-deny event — while `LaserNote.connectPermissionRefused` renders "Bluetooth connect
   permission refused", a different sentence for a different event (connectGatt threw). As first planned the
   consume would have reworded three screens and broken "every key's EN renders the current literal byte for
   byte", with no :pano test to catch it. Fixed: the three launcher-deny sites assign
   `{ tx -> tx.sp_msg_bluetooth_refused }`; `connectPermissionRefused` stays DistoClient's own (:219).
2. FacePrepScreen:541's "Laser · " prefix had no stated disposal. Fixed: the row composes
   `tx.app_laser + " · "` + the state word (`app.connect` / `fp.finding` / `fp.linking` lowercased,
   `fp.laser_connected_tap`), `app.laser` consumed, and `fp.linking`'s `.lowercase()` is said beside
   `fp.finding`'s.
3. "No string template takes any of them" was false: LaserTestScreen:101 interpolated the note state into
   `"\n$it"`, which compiles on a Msg and prints the lambda — the exact silent-print class the claim ruled
   out. The design already rewrote that render; the CLAIM is corrected to name it as the one template reader.
4. Enumeration fixes: the busy state's sites :125/:532/:535 join the list; MainActivity's first catcher is
   :480, not :479; ⌫ joins the keypad glyphs; ImageMeter is already allowed ANYWHERE in latin-ok.txt (only
   DISTO and Insta360 gain where-list entries; CSV, faceprep and jpg are new); and MainActivity (3,700+
   lines, ported over M2 and M6) is larger than FacePrepScreen, so the size claim now reads "largest file
   keyed WHOLE in one slice".
5. "FaceWriter's region now carries ZERO Tx mentions at all" contradicted (h)'s companion, which FAILS if
   the `drawMarks` `Tx.EN` leaves. Reworded: ZERO UNEXEMPT mentions — the pinned `Tx.EN` and its import
   stay, M8 adds no Tx mention to the file.
6. `fp.step_face` renders EN "step face" with the register's EDGE word (स्टेपची कडा) where the shipped face
   word is बाजू — a deliberate divergence that was unstated. Now stated on the key's line and in the table's
   note: the Dim is an edge piece among वरची कडा siblings, and Amit's phone decides.
None refuted; finding 1 changed what was built, 2 and 6 changed the table's documentation and one
composition, 3–5 corrected the plan's own claims.

**The M8 review (9 Nov 2026)** — one finding on 727fb54, independently confirmed, minor; fixed:

1. *`ann.pinch_zoom_drag` coined ड्रॅग where the shipped register's word for drag-to-pan is ओढा.* The
   slice's MR read `दोन बोटांनी झूम करा · सरकवण्यासाठी ड्रॅग करा` — the only ड्रॅग in the whole Marathi
   table, while drag renders ओढा everywhere else, the identical frame `सरकवण्यासाठी ___` included
   (`plan.aria.floor_plan_x_drag` "कागद सरकवण्यासाठी ओढा", `plan.x_drag_move_pinch`, `tour.360.2.title`,
   `sp.360_drag_look_x`, and this slice's own `fp.hint.*` rows). The first half already matched the
   register (`app.pinch_zoom_double_tap`). Fixed as the one-row card edit the finding asked for:
   the MR is now `दोन बोटांनी झूम करा · सरकवण्यासाठी ओढा`, English unchanged, Tx regenerated
   (`i18nGen`, then `i18nRatchet`: 0 counts lowered), the key table row above corrected, and the
   register is now a PROPERTY the suite holds: I18nTableTest's new check 11 scans every Marathi value
   and fails on any ड्रॅग — red on the old row (proven before the fix was restored), so the loanword
   cannot be coined again anywhere in the table. No key, accessor or caller moved; AnnotateScreen.kt
   is untouched (the words live in the card, which is the point of the card).

### 31. The tour (the last slice of the port)

**Size:** L · **Depends on:** L1/L2 (the keys are shipped), QM1 (the strip), AS1 (the capture-keyed upload the
demo must never enter), M2/M3a–c/M6/M8 (every screen the tour lights already reads in Marathi)

**The number.** The table's row 31, VIS ("Switch the भाषा · Language row on"), is MOOT: the language row has
shipped visible since Amit's 28 Oct "Show it now, translate alongside" (`LangRow.VISIBLE`, drawn in
`drawerGroups`), and M8 left `unkeyed.tsv` empty, so there is no switch left to flip. The tour takes the §31
number. It is the last clause of the prototype eyebrow's phone line — "not yet: tour" — and the last slice of
the port.

**THE PROTOTYPE HAS A TOUR, and it was driven whole before this section was written** (9 Nov 2026). A copy of
the approved v61c.2 page (`index61c2.html`) was served with its assets (`real/htu.js`,
`real/MCAP-61753b40f4f7.jpg`, `pano.jpg`) from the session scratchpad's `tourverify/` on a local port, in
Playwright, at the Mi A3 frame (360 × 780). The page's demo survey prep is NOT in the page: it lives in the
published artifact's own database (the page's `db.doc('prep/guest-bedroom')` — collection `prep`, doc
`guest-bedroom`, `{state, savedAt: 2026-10-11, version: "v35"}`, ~4.4 KB), fetched with ArtifactData and served
to the copy through a `claude.use('db')` stub; without it the demo room opens unbounded and empty, exactly as
`restore()`'s "Not saving: no storage in this view" path says. Then all SEVEN tours were driven end to end
under English AND मराठी — 73 steps each run, every step screenshotted, 150 screenshots in all — with this
result: every step's anchor resolved (`found` true on all 73 × 2), no bubble overlapped its target and no body
clipped (`overlap` false, `clip` 0 throughout — the ct55 bar), every tour ended `done`, and every tour's
`wrote` was 0 (a Next-only run writes nothing — the restore rule, proven live). All 146 shown titles and bodies
under मराठी matched `android/i18n/strings-mr.json` byte for byte, and the English run matched
`strings-en.json` the same way: the phone's tables ARE the tour's texts already. The drawer row read
"Help · guided tours · 7 tours" / "मदत · मार्गदर्शक टूर · 7 टूर".

**What it is** (the page's own words, v55 Builder E, from Amit, 21 Oct 2026: "i want a guided tour on apk and
prototype both . not some thing i will just read but execute there it self like a highlighted screen showing
what to do next in a help section"): a dark scrim clipped to the phone frame with a rounded SPOTLIGHT cut over
one LIVE element, and a bubble beside it — counter `3/9`, the flow's name, a mode chip, the step's title and
body, and `Skip tour · Back · Next` (`Next` reads `Done` on the last step; `Back` is disabled on step 1 and
while a move runs). On a `do` step taps inside the spotlight reach the app and Amit really does it; on a
`look` step the hole is covered too (✓ Save, Capture 360, D2, Reset layout are shown, never pressed).
Everything outside the hole is blocked.

**Where it starts, and the first-run rule.** The ≡ drawer gains a FIRST group, HELP, with one row:
`help.row` ("Help · guided tours") valued `<n> + app.tours` (" tours") — first because the drawer already
scrolls and a row at its foot starts below the fold (ch55). The row opens a full HELP SCREEN, built like the
Laser test screen: its own top bar with ← , title `common.help`, subtitle `help.subtitle` ("Guided tours · on
the demo flat"), the intro paragraph `help.each_tour_opens_demo`, then one row per flow — its `tour.<id>.name`,
its one-line `tour.<id>.line`, and a `common.start` button. ◁ and ← close it. **There is no first-run
overlay, no auto-start and no seen-once prefs flag anywhere in the prototype**: the Help row is the tour's
only entry, and every tour re-runs as often as wanted. The phone copies that exactly — the task-list guess
("first run? a prefs flag?") is answered by the prototype with NO.

**How it advances and dismisses.** `Next` ALWAYS works and never writes data. A step may also auto-advance on
its `done` rule: a tap inside the spotlight after the step showed, advancing 320 ms AFTER THE TAP — a settle
delay, so the app's response to the tap is seen before the bubble moves; the engine's own line is
`run.tapAt>run.shownAt && Date.now()-run.tapAt>=320` (this sentence first demanded the tap itself come ≥ 320 ms
after show, which would IGNORE a prompt tap — critic finding 4, confirmed and corrected) — an anchor appearing or
disappearing (`{exists:…}` / `{gone:…}`), the app reaching a screen (`{screen:…}`), the demo's phase
(`{phase:n}`), survey prep's tool / tab / step (`{tool:…}` `{tab:…}` `{mode:…}`), or one more mark than when
the step showed (`{marks:'+1'}`). The engine polls at 150 ms; every step's `setup` is a POSITION ("be here"),
run on Next, Back and jumps alike, so Back always lands right — and a step with a `done` has a setup that
makes its predicate false when it shows, so Back never lands on a step that advances again at once. Dismissal:
`Skip tour`, or ◁ or Home during a tour (caught before the app sees them), or `Done` on the last step — and
EVERY end restores everything the tour touched (the stores it snapshotted at start; survey prep put back IN
PLACE — marks, readings, size, the ↶ history — before it closes, and written only if the practice changed
it), then returns to the Help screen. Nothing of the tour exists when no tour runs.

**The bubble's placement** (the part worth porting as pure geometry): the bubble takes a side of the hole with
room for ALL of it — below / above first in portrait at width min(328 dp, frame − 16), right / left first in
landscape at 300 dp — then above / below at full width, then beside it down to 176 dp; when no side has room
it TRIMS the lit part to the target's leading part (≥ 48 dp of it left in portrait, ≥ 96 in landscape) so a
side fits whole; the squeeze (a scrolling body, side room ≥ 72 dp) and sitting over the target are last
resorts the prototype never needs (ct55's bar, re-proven in this run: `trim` only where designed, `clip` 0,
`overlap` false on all 146). Constants: PAD 6 round the target, EDGE 8 to the frame, GAP 10 to the bubble.
When a step's anchor is null or missing the bubble sits centred with no spotlight. The target is first
scrolled into view in ITS OWN list (never the page), and anchors are RE-RESOLVED EVERY FRAME — a kept element
is a stale answer.

**The texts are already on the phone — this slice adds no literal and no translation.** The RAW table carries
160 `tour.*` keys (7 × name + line, 73 × title + body: phases 22, survey 26, existing 28, wish 28, vendor 26,
360 16, plan 14) and 7 `help.*` keys, all landed with L1/L2 and all with approved Marathi;
`gradle :pano:i18nGen` has already generated their Tx accessors (`tour_360_1_body`, `help_row`, …; they are in
`android/i18n/index.tsv` today). The chrome reuses rows the table already has: `common.start` (सुरू करा),
`common.next` (पुढे), `common.done` (पूर्ण), `help.tour.skip` ("Skip tour" / टूर वगळा), `help.tour.mode.do`
("Try it" / करून पाहा), `help.tour.mode.look` ("Look only" / फक्त पाहा), `help.tour.aria`, `common.help`,
`app.tours`, and — the one borrowed row — the bubble's Back label is the navigation row `app.aria.back` (मागे),
by M2's decision-6 rule recorded in that row's own where note: a navigation control must not share a surface
word's key. The tables carry a SECOND EN "Back" — `surface.back.short`, the back WALL's tab name, the same
मराठी — and the RAW table files the tour's own Back lines (`tour55.js:261 · o`, `:402 · bar`) under THAT row,
so the first version of this sentence ("the table's one EN Back row") was false as written while the driven
byte-match still held either way (critic finding 5, confirmed). The counter is digits. Decision 6
(Claude translates at full authority) has NOTHING to write here; `unkeyed.tsv` stays empty by construction.

**The contract, which the Kotlin port reuses verbatim** (the page keeps it as a header over the flows, marked
"the contract the Kotlin port reuses verbatim"): a FLOW is `{id, name, line, steps}`; a STEP is
`{id, screen, anchor, focus?, title, body, mode: do|look, setup, next?, done?}`; a MOVE is a position object
(`close`, `go:<screen>`, `drawer`, `phase:<0-3>`, `ask`, `cancelAsk`, `survey:<room>`, `tab`, `mode`, `show`,
`look:<surface>`, `tool` (+`nm`), `more`, `sel`, `pick`, `sheet`, `sheetClose`, `planSel`, `wait`, and v58's
`wishes`, `states`, `statePop`, `export`, `details`). **An anchor is a NAME from one vocabulary, never a
selector**, so the phone tags the same elements with `Modifier.tourAnchor("<name>")` and the flows port word
for word: `help · menu · flow:<id> · start:<id> · row:<title> · room:<room> · cap:<kind> · capture360 ·
p54seg:<0-3> · phase:strip · phase:ask · phase:band · chip:wish · quote · wish:head · phase:packs ·
pack:<trade> · details · stg · service:<id> · service:row:<id> · plan:tile · plan:room:<room> · plan:rotate ·
plan:fit · plan:reset · mode[:<step>] · tab:<tab> · save · lines:row · palette · tool:<t> · toolchip[:as] ·
more[:<trade>] · selrow[:code] · chips · reading:mm · reading:d2 · show · lock · photo · pano · sheet ·
sheet:move:<life> · sheet:act · sheet:acts · sheet:done · trade · list:state · list:move · list:export ·
list:pop · x53 · x53:pack:<id> · x53:trades · x53:fmt` — plus two VIRTUAL anchors the phone computes with
survey prep's own drawing maths, not a tag: `area:<surface>:x0,y0,x1,y1` (fractions of the surface, lit
wherever that surface is drawn now — its photo or the 360) and `mark:<code>` (a mark's own box).

**The seven flows as the prototype ships them** — order, targets, auto-advance and titles. Every title and
body is `tour.<flow>.<n>.title` / `.body` in `android/i18n/strings-en.json` / `strings-mr.json`, committed,
approved and byte-verified against the served page in both languages, so the bodies are not duplicated here:
the tables are the one home of the texts (the M-slices' own rule).

**Tour `phases` · The phases / फेज** — 10 steps. Line: `tour.phases.line`.

| n | screen | anchor | mode | done (auto-advance) | title (EN · MR) |
|---|---|---|---|---|---|
| 1 | project | `phase:strip` | look | — | Four phases, one strip · चार फेज, एक पट्टी |
| 2 | project | `p54seg:0` | look | — | Survey comes first · आधी सर्व्हे |
| 3 | project | `p54seg:1` | do | {phase:1} | Move to Wish list · इच्छा-यादीकडे जा |
| 4 | project | `chip:wish` | look | — | The Wish list appears · इच्छा-यादी दिसते |
| 5 | project | `quote` | look | — | The quote stage follows · कोटेशनचा टप्पा मागोमाग |
| 6 | project | `p54seg:2` | look | — | Then vendor review · मग व्हेंडर तपासणी |
| 7 | project | `p54seg:3` | look | — | Estimation hands off · अंदाजपत्रकाकडे सुपूर्द |
| 8 | project | `p54seg:0` | do | {exists:phase:ask} | Going back asks first · मागे जाताना आधी विचारते |
| 9 | project | `phase:ask` | do | {gone:phase:ask} | Keep or move back · ठेवा किंवा मागे न्या |
| 10 | survey | `phase:band` | look | — | The phase in survey prep · सर्व्हे तयारीतील फेज |

Moves its setups use: `close`, `go`, `phase`, `cancelAsk`, `ask`, `survey`, `tab`, `mode`.

**Tour `survey` · Survey a room / खोलीचा सर्व्हे** — 12 steps. Line: `tour.survey.line`.

| n | screen | anchor | mode | done (auto-advance) | title (EN · MR) |
|---|---|---|---|---|---|
| 1 | clients | `row:Demo Client` | do | {screen:client} | Start from Clients · ग्राहकांपासून सुरुवात |
| 2 | client | `row:Demo Site, Pune` | do | {screen:site} | Then the site · मग साइट |
| 3 | site | `row:Demo flat interiors` | do | {screen:project} | Then the project · मग प्रोजेक्ट |
| 4 | project | `room:Guest Bedroom` | do | {screen:room} | Pick the room · खोली निवडा |
| 5 | room | `capture360` | look | — | Capture 360 starts one · "360 कॅप्चर" नवीन सुरू करते |
| 6 | room | `cap:360` | do | {screen:survey} | Open the 360 · 360 उघडा |
| 7 | survey | `mode:lines` | look | — | 1 · Lines bounds it · 1 · रेषा हद्द आखते |
| 8 | survey | `lines:row` | look | — | Nudge the last millimetres · शेवटचे मिलिमीटर जुळवा |
| 9 | survey | `tab:front` | look | — | Every surface gets ✓ · प्रत्येक पृष्ठभागाला ✓ |
| 10 | survey | `mode:mark` | do | {mode:mark} | 2 · Mark what is there · 2 · जे आहे ते मार्क करा |
| 11 | survey | `mode:list` | do | {mode:list} | 3 · List is the sheet · 3 · यादी हाच तक्ता |
| 12 | survey | `save` | look | — | ✓ Save when all six · सहाही झाल्यावर ✓ सेव्ह |

Moves its setups use: `close`, `go`, `phase`, `survey`, `tab`, `mode`.

**Tour `existing` · Mark existing items / सध्याच्या वस्तू मार्क करा** — 13 steps. Line: `tour.existing.line`.

| n | screen | anchor | mode | done (auto-advance) | title (EN · MR) |
|---|---|---|---|---|---|
| 1 | survey | `palette` | look | — | The mark palette · मार्क पॅलेट |
| 2 | survey | `tool:E` | do | {tool:E} | Pick a type · प्रकार निवडा |
| 3 | survey | `toolchip` | look | — | Placed as Existing · "सध्याचे" म्हणून ठेवले |
| 4 | survey | `area:front:0.25,0.18,0.75,0.32` | do | {marks:+1} | Tap to place it · ठेवण्यासाठी टॅप करा |
| 5 | survey | `tool:sel` | do | {tool:sel} | Select stops placing · "निवडा" ठेवणे थांबवते |
| 6 | survey | `tool:more` | do | {exists:more} | More ▸ has every type · "आणखी ▸" मध्ये सर्व प्रकार |
| 7 | survey | `more:electrical` | look | — | One row per trade · प्रत्येक ट्रेडची एक ओळ |
| 8 | survey | `selrow` | look | — | A selected mark · निवडलेला मार्क |
| 9 | survey | `chips` | look | — | 3 · List takes readings · 3 · यादी मध्ये रीडिंग |
| 10 | survey | `reading:mm` | do | — | Type a reading · रीडिंग टाका |
| 11 | survey | `reading:d2` | look | — | Or read the D2 · किंवा D2 ने वाचा |
| 12 | survey | `show` | do | 'tap' | Photo, measured or both · फोटो, मोजलेले किंवा दोन्ही |
| 13 | survey | `lock` | do | 'tap' | Lock a finished mark · पूर्ण झालेला मार्क लॉक करा |

Moves its setups use: `close`, `phase`, `survey`, `tab`, `mode`, `tool`, `more`, `sel`, `pick`.

**Tour `wish` · Build the wish list / इच्छा-यादी बनवा** — 13 steps. Line: `tour.wish.line`.

| n | screen | anchor | mode | done (auto-advance) | title (EN · MR) |
|---|---|---|---|---|---|
| 1 | project | `p54seg:1` | do | {phase:1} | Move to Wish list · इच्छा-यादीकडे जा |
| 2 | project | `chip:wish` | do | {screen:wish} | Open the Wish list · इच्छा-यादी उघडा |
| 3 | wish | `wish:head` | look | — | What the client wants · ग्राहकाला काय हवे |
| 4 | wish | `service:electrical` | do | 'tap' | Tick a service · सेवा टिक करा |
| 5 | wish | `service:row:master-bath` | look | — | The bathroom bundle · बाथरूम संच |
| 6 | survey | `phase:band` | look | — | Wishes in survey prep · सर्व्हे तयारीत इच्छा |
| 7 | survey | `toolchip:as` | look | — | Placed as Client wish · "ग्राहकाची इच्छा" म्हणून ठेवले |
| 8 | survey | `area:front:0.25,0.18,0.75,0.32` | do | {marks:+1} | Place a wish · इच्छा ठेवा |
| 9 | survey | `selrow:code` | do | {exists:sheet} | Open an existing mark · सध्याचा मार्क उघडा |
| 10 | survey | `sheet:move:wish` | do | {exists:sheet:act} | Make it a wish · त्याला इच्छा बनवा |
| 11 | survey | `sheet:acts` | do | — | Say what is wanted · काय हवे ते सांगा |
| 12 | survey | `sheet:done` | do | {gone:sheet} | Done applies it · "पूर्ण" लागू करते |
| 13 | survey | `mark:FW1` | look | — | FW1 is now a wish · FW1 आता इच्छा आहे |

Moves its setups use: `close`, `go`, `phase`, `survey`, `tab`, `mode`, `tool`, `sel`, `sheet`.

**Tour `vendor` · Vendor review / व्हेंडर तपासणी** — 12 steps. Line: `tour.vendor.line`.

| n | screen | anchor | mode | done (auto-advance) | title (EN · MR) |
|---|---|---|---|---|---|
| 1 | project | `p54seg:2` | do | {phase:2} | Move to Vendor review · व्हेंडर तपासणीकडे जा |
| 2 | project | `phase:packs` | look | — | Wishes become trades · इच्छांचे ट्रेड बनतात |
| 3 | project | `pack:doors-windows` | look | — | One pack per vendor · प्रत्येक व्हेंडरसाठी एक पॅक |
| 4 | survey | `trade` | look | — | Show marks for a trade · एका ट्रेडचे मार्क दाखवा |
| 5 | survey | `toolchip:as` | look | — | Placed as Vendor added · "व्हेंडरने जोडले" म्हणून ठेवले |
| 6 | survey | `area:front:0.25,0.18,0.75,0.32` | do | {marks:+1} | Place a vendor's mark · व्हेंडरचा मार्क ठेवा |
| 7 | survey | `list:state` | do | {exists:list:pop} | State ▾ earns its place · "स्थिती ▾" आता उपयोगी |
| 8 | survey | `list:pop` | do | 'tap' | Pick one state · एक स्थिती निवडा |
| 9 | survey | `list:move` | look | — | Move them on together · सगळे मिळून पुढे हलवा |
| 10 | survey | `list:export` | do | {exists:x53} | Export for one vendor · एका व्हेंडरसाठी एक्सपोर्ट |
| 11 | survey | `x53:trades` | look | — | Their trades, as PDF or image · त्यांचे ट्रेड, PDF किंवा चित्र म्हणून |
| 12 | capture | `stg` | look | — | Photos are filed by work · फोटो कामानुसार ठेवले जातात |

Moves its setups use: `close`, `go`, `phase`, `wishes`, `survey`, `tab`, `mode`, `tool`, `states`, `statePop`,
`export`, `details`. (In v61c.2 the `stg` anchor resolves to the capture screen's WORK line, and
`tour.vendor.12.body` already says "its WORK line stays on the Quote journey until the client approves the
estimate" — the one flow text that was UPDATED for v61c.)

**Tour `360` · The 360 / 360** — 7 steps. Line: `tour.360.line`.

| n | screen | anchor | mode | done (auto-advance) | title (EN · MR) |
|---|---|---|---|---|---|
| 1 | survey | `tab:360` | do | {tab:360} | Open the 360 tab · 360 टॅब उघडा |
| 2 | survey | `pano` | do | — | Drag to look round · फिरून पाहण्यासाठी ओढा |
| 3 | survey | `mode` | look | — | No steps in the 360 · 360 मध्ये पायऱ्या नाहीत |
| 4 | survey | `palette` | look | — | One palette for all · सर्वांसाठी एकच पॅलेट |
| 5 | survey | `tool:E` | do | {tool:E} | Pick a point type · पॉइंटचा प्रकार निवडा |
| 6 | survey | `area:front:0.25,0.18,0.75,0.32` | do | {marks:+1} | Tap where it is · जिथे आहे तिथे टॅप करा |
| 7 | survey | `tab:front` | look | — | Back to a surface · पृष्ठभागावर परत |

Moves its setups use: `close`, `phase`, `survey`, `tab`, `mode`, `tool`, `look`.

**Tour `plan` · Floor plan / फ्लोअर प्लॅन** — 6 steps. Line: `tour.plan.line`.

| n | screen | anchor | mode | done (auto-advance) | title (EN · MR) |
|---|---|---|---|---|---|
| 1 | project | `plan:tile` | do | {screen:plan} | The Floor plan tile · फ्लोअर प्लॅन टाइल |
| 2 | plan | `plan:room:Guest Bedroom` | do | — | Drag a room · खोली ओढा |
| 3 | plan | `plan:rotate` | do | 'tap' | Turn it 90° · 90° फिरवा |
| 4 | plan | `plan:room:Guest Bedroom` | look | — | Edges snap together · कडा आपोआप जुळतात |
| 5 | plan | `plan:fit` | do | 'tap' | Fit shows them all · "बसवा" सगळ्या दाखवते |
| 6 | plan | `plan:reset` | look | — | Reset starts over · रीसेट पुन्हा सुरुवात करते |

Moves its setups use: `close`, `go`, `planSel`.

**The demo flat — the one decision the prototype itself names for the port** (ch55, "Kotlin needs", approved
with v55): "a built-in demo project, kept on the phone only and never synced — the APK carries only real
projects, and a tour's phase move there would be a real stage move on S5 (PUT /api/asis/:project/stage). This
is the one decision the port cannot avoid: the demo is the Guest Bedroom's sample 360 and its prep, flagged
never-sync." So the phone gains, exactly as the prototype seeds it: **Demo Client → Demo Site, Pune → Demo
flat interiors → Guest Bedroom**, one 360 capture (the prototype's `pano.jpg`, ~161 KB, bundled as an app
asset) with room size L 3319 · W 2678 · H 2677 and the demo prep as the artifact's db holds it (the
`prep/guest-bedroom` state fetched above: all six surfaces bounded, marks FW1 FN1 FN2 FE1 · RE1 · BD1 BE1 BL1
· LN1 · GS1 · CE1 and their readings — bundled as a JSON resource so `TourFlowsTest` can read it). Rules, each
ONE place:

- `TourDemo` (:pano) is the single is-this-the-demo rule: the fixed client / site / project names and the
  fixed capture id (its own `DEMO-` grammar, outside `MCAP-`'s, so AS1's device-id rule can never mint a
  colliding real one). Every sync entry point refuses demo rows through it and nothing else: `AsIsUpload`
  (enqueue), `SyncWorker`, `QuoteStageSync` (the guarded S5 stage move), `WishSync`, `AlbumOutbox` /
  `AlbumQueue`, `ReadingsSync`. A demo row never reaches a queue, a PUT or the bench.
- The demo project's stage is PHONE-LOCAL — the one exception to v61c.2's "a project S5 does not list has no
  stage of its own", made by ch55's approved decision, because without it the strip the `phases` tour teaches
  is dead on the demo. IN THIS BUILD that local stage exists only as the tour world's `TourState.demoPhase`
  (the host's `phase` move sets it; the machine's `{phase:n}` reads it): with the `phases` and `wish` flows
  blocked, no shipped step lights the strip, so the strip, the chip gate (`WishList.chipShown`) and the quote
  line are NOT yet rewired to it — the demo project's screen shows the shipped "not on S5 yet" state, which
  is true of it. That rewiring lands with the flow that needs it; `QuoteStageSync.move` refuses demo keys
  either way, so a strip tap there can never become a PUT. Its wish list likewise lives on the phone only.
- The demo is seeded idempotently at app start (`TourDemo.ensure(store)`) and is always in Browse, as Demo
  Client always is on the prototype — practising outside a tour is allowed and is the person's own; a tour
  snapshots at `start()` and restores THAT, not the factory seed, exactly as the prototype does.
- The demo capture sits in `CaptureStore` with a sent-state (the prototype's row is `SYNCED`), so it adds
  nothing to "N waiting" and the Queue tab draws it as the prototype draws its demo rows.

**What ships on the phone in this slice, and what cannot yet.** The port rule (workspace CLAUDE.md): a UI
state the approved prototype does not draw does not reach the phone — and its mirror: a tour step whose
target the phone does not have cannot run true, and a flow with a step cut out is a UI state the prototype
does not draw, so a cut needs Amit's approval ON THE PROTOTYPE first. Each flow was audited anchor by anchor
against batch-next:

| flow | verdict | why |
|---|---|---|
| `survey` (12) | **SHIPS WHOLE** | every anchor exists: the Browse rows and room tile (`BrowseScreens.kt`), Capture 360 (the room screen's FAB, `MainActivity.kt`) and the capture tile (`CaptureScreens.kt` `CaptureTile` — critic finding 3: the first version named `HomeScreens.kt`, which holds Work and Queue, neither of these), the step strip, a 1 · Lines nudge row (`SurveyPrepScreen.kt` `nudgeLine` rows), the face tabs, ✓ Save |
| `360` (7) | blocked | **critic finding 1, confirmed — the first version said SHIPS WHOLE and was wrong.** The prototype's survey prep draws a real 360 TAB (its tab rows are FRONT RIGHT BACK LEFT / FLOOR CEILING 360 LIST PLAN) and its 360 view has NO step strip ("360 · every surface · every mark type"); the page's own flows header calls "a fourth '4 · 360' step" a STALE part not carried over. The phone still has that stale model: `StepRoute.TAB_ROWS` has no 360 tab and its 360 is strip step 4 (`StepRoute.STEPS`, `SurveyPrep360.kt` "4 · 360"). So step 1's anchor `tab:360` has no tab to tag, step 3's words ("The 360 has no step strip") are FALSE on a strip visibly showing 1–4 with 4 lit, and step 7's "A surface tab leaves the 360" describes the wrong mechanism. The engine's `doneNow` already maps `{tab:'360'}` to the phone's `mode === '360'` (kept in `TourMachine`), so the predicate ports; the drawn UI and the committed texts do not |
| `phases` (10) | blocked by ONE step | steps 1–9 run true on the shipped strip (`PhaseStrip.kt`, the ask, the wish chip gate, the quote line); step 10's `phase:band` is v54's phase band at the top of survey prep, which the phone does not have |
| `existing` (13) | blocked by TWO steps | 7 `more:electrical` (the phone's More ▸ is a type grid, `SpMoreSheet`, with no per-trade rows), 9 `chips` (the phone's 3 · List is a checklist of rows, not the prototype's mark chips). Step 12's target EXISTS — `SurveyPrepScreen.kt`'s `showRow` is exactly the Show · ≈ Photo / Measured / Both pills, drawn in list view and mark mode — so the first version's third blocker ("no Show control") was wrong (critic finding 2, confirmed) |
| `wish` (13) | blocked | steps 6–13 need the v53/v54 lifecycle the phone has never had: the phase band, "as Client wish" on the tool chip, the Mark sheet and its moves (`sheet:*` anchors); steps 1–5 alone are not the approved flow |
| `vendor` (12) | blocked | VENDOR PACKS, the Trade chip, State ▾ and its popover, Move N ▸, the Export sheet's pack/trades chips and the `wishes` seeding — all v54/v58 survey-prep features the phone does not have |
| `plan` (6) | blocked | the project-level Floor plan arranger (drag, ↻ 90°, snap, Fit, Reset) does not exist on the phone at all (`SurveyPlanScreen.kt` is the per-room PLAN tab, a different thing) |

So the slice ships the ENGINE, the drawer row, the Help screen, the demo flat, and the ONE flow that runs
true — `survey`, 12 steps (the first version said "survey and 360, 19 steps"; the 360's blocking is critic
finding 1). The Help list and the drawer count are computed from the shipped flows (the prototype's own
`flows().length + ' tours'`), so the row honestly reads "1 tour" / "1 टूर" until more flows' targets exist.
Each blocked flow's data ships in `Tour.kt` marked blocked with its missing anchors named, so adding a flow
later is flipping its entry after its targets land, not re-deriving it.

**Held for Amit** (to join the held prototype questions, raised after the final build per the quiet-run plan —
none of them blocks this slice):

1. S2 APK: cut `phases` to 9 steps on the prototype (drop step 10, the phase band) so the phone can carry the
   tour that teaches the strip Amit just shipped (QM1/QM2)? Claude recommends yes; the step returns when the
   band ports. The same question for `existing` at ELEVEN steps (drop 7 and 9 — critic finding 2: step 12's
   Show control exists on the phone, so the hypothetical cut is two steps, not three) is still NOT
   recommended, though more narrowly than first written: step 9's words ("3 · List takes readings" over mark
   CHIPS) describe a list the phone's checklist does not draw, and cutting the More ▸ trade rows (7) with it
   thins the flow's middle.
2. S2 APK (prototype content, not port work): the `phases` texts predate v61b — `tour.phases.name/line/1.*`
   say FOUR phases while the approved strip has five segments (Revisions) over six quote stages, and no tour
   anywhere covers Revisions, Approve, the scope sheet or the Album (v61c's own features). A v61c tour is
   prototype work first (Amit approves it there), then its keys and a flow entry here; nothing of it is
   invented by this port.
3. S2 APK: the `360` flow (critic finding 1). Two honest ways to unblock it, both Amit's: port the
   prototype's APPROVED 360-tab layout first — the tab among the face tabs and a strip-less 360 view; the
   prototype IS the approved UI and the phone is behind it (its 4 · 360 is the layout the page's own header
   calls stale) — after which the flow's texts run true as committed; or approve ADAPTED step texts on the
   prototype for the phone's strip-step 360. Claude recommends the first: it removes the divergence instead
   of writing around it.

**The phone half — files.**

:pano (pure JVM, tested here):
- `pano/Tour.kt` — the contract's types (Flow, Step, Move, Done, the anchor-name grammar) and THE SEVEN FLOWS
  as data, titles and bodies through the existing Tx accessors, each flow carrying `ships` (true: survey
  alone, after critic finding 1) and, when false, its missing anchors as text. A test, not a comment, keeps
  the shipped set honest.
- `pano/TourPlace.kt` — the placement geometry, pure rect-in/rect-out: the side orders (portrait and
  landscape), full-width, the 176 narrow fit, trim (48 / 96), squeeze (72), centre-when-no-hole; PAD 6,
  EDGE 8, GAP 10, widths 328 / 300.
- `pano/TourMachine.kt` — the step machine: start / next / back / skip / end, busy, the counter, `doneNow`
  over a host-fed world (screen, tool, tab, mode, markCount, anchor-exists, phase), the 320 ms tap rule, the
  log (flow, how it ended, steps seen, wrote).
- `pano/TourDemo.kt` — the demo's identity (the one rule), its seed (the prep JSON as a resource, the fixed
  ids, the size), `ensure` semantics.

:pano tests (all run here with `nice -n 15 gradle :pano:test --offline`):
- `TourFlowsTest` — the counts (7 flows, 73 steps; survey's 12 shipped alone); every anchor parses against the
  vocabulary; every step's title and body accessor non-blank in BOTH languages; every flow's name and line
  keys present; every `done` step's setup makes its predicate false (checkable for tool / tab / mode / phase);
  the first step of every flow opens with the everything-goes-back sentence (the key's own text, pinned).
- `TourPlaceTest` — the prototype's placement bar as properties: the bubble never covers the hole, never
  leaves the frame, trim only when no side fits, squeeze and overlap never on any driven step's measured
  rects. The 146 step rects are COMMITTED as a :pano test resource (`test/resources/tour/tour-place-rects.json`,
  re-exported from the driven page with `android/docs/port61c/tour-drive.mjs`, itself committed) — critic
  finding 6, confirmed: the first plan replayed them from the scratchpad's `report.json`, against the repo's
  own workbench-not-storage rule, and a container restart would have lost the gate's inputs.
- `TourMachineTest` — Next always advances; Back lands on a non-auto-advancing position; Skip / Done both
  end; a Next-only run records wrote 0; the tap rule's 320 ms.
- `TourDemoTest` — the seed parses, six surfaces `set`, the named marks present; `isDemo` covers every seeded
  row and nothing else; and the never-sync rule: the :pano-side queue logic (`AlbumQueue`) refuses demo keys
  (the :app guards are read in review; CI compiles them).

:app (compiled ONLY by CI, as every slice):
- `TourOverlay.kt` (new) — the scrim's four blocks + hole + look-cover, the bubble (counter, flow name, mode
  chip, title, body, Skip tour / Back / Next-Done), placement from `TourPlace`, anchors from a
  `Modifier.tourAnchor(name)` registry (`onGloballyPositioned` into one snapshot map, re-read every frame);
  taps outside the hole consumed; ◁ via BackHandler = skip; the activity's ON_STOP (Home, the task switcher)
  = skip, because Android gives no Home intercept and a tour must never outlive the screen it lit.
- `HelpScreen.kt` (new) — the full screen (bar ← · Help · subtitle, intro, a row per shipped flow with
  Start), opened like the Laser test screen from one flag in `MainActivity`.
- `MainActivity.kt` — the HELP drawer group FIRST in `drawerGroups` (row `help.row`, value computed count +
  `app.tours`); the help flag; the TourHost: every MOVE the shipped flow uses (`close`, `go`, `survey`,
  `phase` — demo-local, `tab`, `mode` — `tool` and `look` left unhosted with the flows that need them,
  TourFlowsTest pinning the hosted set) executed through the app's own state and stores, never a parallel
  path; the demo snapshot / restore (the demo's prep via `SurveyPrepStore`, the row's size, the local phase,
  the navigation) on every end — the restore WAITING for the survey dialog to leave first, because the
  dialog's own dispose-save would otherwise write the practice back over the restored snapshot.
- `CaptureStore.kt` / `TourSeed` wiring — the demo seeded idempotently at start (the pano asset
  `tour_demo_pano.jpg`, the SYNCED capture row, the prep file).
- `BrowseScreens.kt` (`row:`, `room:` — the demo rows only, so a real row never writes an anchor),
  `CaptureScreens.kt` (`cap:` on the demo capture's tile), `MainActivity.kt` (`capture360`, the room screen's
  FAB — critic finding 3: the first version put the FAB and the tile in `HomeScreens.kt`, which holds Work
  and Queue), `SurveyPrepScreen.kt` (`mode:<step>`, `tab:<tab>`, `save`, `lines:row`, in the dialog's own
  window) — `Modifier.tourAnchor` tags on exactly the anchors the SHIPPED flow names; the blocked flows' tag
  sites (`palette`, `tool:<t>`, `pano`, the strip and band, the `area:` maths) land when their flows flip.
- `AsIsUpload.kt` (enqueue — SyncWorker's prep sweep comes through it), `SyncWorker.kt` (the ERP leg, whose
  only-on-S5 filter is exactly the demo row's shape: SYNCED, no serverName), `QuoteStageSync.kt` (move),
  `WishSync.kt` (push — the demo's key is also local by construction), `AlbumOutbox.kt` (add),
  `ReadingsSync.kt` (fetch and push), and :pano's `AlbumQueue.requestOf` — the one-line `TourDemo` refusal at
  each entry.

**What Amit sees.** A new first group in the ≡ drawer: "Help · guided tours · 1 tour" (मदत · मार्गदर्शक टूर ·
1 टूर). Inside, the Help screen with "Survey a room" and Start. A started tour darkens
the screen, lights one real control at a time on the demo flat — which now sits in Browse under Demo Client —
and lets him do the step; Next, Back, Skip tour, the counter, Try it / Look only, everything in whichever
language the drawer is set to; Done (or Skip, or ◁, or Home) puts the demo back exactly as it was and returns
to Help. Nothing he does in a tour reaches S5, ERP or the queue — provable from the Queue tab staying still.
When this build ships, the prototype eyebrow's phone line drops "not yet: tour" in the same step (the
workspace rule: the ship is not done until the eyebrow names it), and S2's Running row moves with it.

**Risks.** (1) The anchor registry is the new moving part: a tag on a composable that recomposes away (the
drawer, sheets) must deregister, or a stale rect lights empty screen — the registry is keyed by name with
last-write-wins and a disposal hook, and `TourOverlay` re-reads it every frame as the engine re-resolves every
frame. (2) The `area:` box over the pano reuses the 360's own direction-to-screen maths; if the phone's pano
projection differs from the prototype's the lit box sits elsewhere on the picture — the box is fractions of
the SURFACE, so it stays on bare wall wherever the projection puts it, and the step's words ("tap the bare
wall in the lit part") stay true. (Moot in THIS build: with the 360 flow blocked, no shipped step names a
virtual anchor, and the phone computes none yet.) (3) The demo is new permanent data in Browse; if Amit does not want it
visible outside the Help screen that is a one-question change (seed on first Help open instead) — but the
prototype shows it always, so the port does too. (4) Nothing here has been seen on a phone; the driven
prototype run and the :pano suites are the stand-in, as every slice before it.

**The tour critic (build review, 10 Nov 2026)** — six findings on §31 as written (`1d68c8e`), each checked
against the code on batch-next and the served v61c.2 page before the build applied it. ALL SIX CONFIRMED;
none refuted. The corrections are folded into the section above, marked by finding; this list is the record.

1. **CONFIRMED (major) — the `360` flow does not run true on the phone.** The prototype draws a 360 TAB
   (FRONT RIGHT BACK LEFT / FLOOR CEILING 360 LIST PLAN) and a strip-less 360 view ("360 · every surface ·
   every mark type"); its flows header calls "a fourth '4 · 360' step" a stale part not carried over. The
   phone IS that stale part: `StepRoute.TAB_ROWS` has no 360 and `StepRoute.STEPS` ends `"360"`. Steps 1, 3
   and 7's committed texts are false on the phone's screen — §31's own standard, the one it blocked
   `existing` over. Applied: the verdict row, `ships` (survey alone, 1 tour, 12 steps), held question 3.
   The mitigating fact stands and is kept in the row: `doneNow`'s `{tab:'360'}` → `mode === '360'` mapping
   ports, so unblocking is UI-and-texts work, not engine work.
2. **CONFIRMED (major) — `existing` is blocked by TWO steps, not three.** `SurveyPrepScreen.kt`'s `showRow`
   (Show · ≈ Photo / Measured / Both, keys `common.show/photo/measured/both` in both tables) is exactly step
   12's target, drawn in list view and mark mode. Applied: the verdict row and held question 1 (a
   hypothetical cut is 7 and 9, eleven steps remain).
3. **CONFIRMED (minor) — Capture 360 and the capture tile are not in `HomeScreens.kt`** (Work and Queue live
   there). The FAB is `MainActivity.kt` (`tx.common_capture_360`), the tile `CaptureScreens.kt`
   (`CapturesScreen` / `CaptureTile`). Applied: the survey verdict row and the tag-site bullet; the build
   tagged the right files.
4. **CONFIRMED (minor) — the 320 ms runs from the TAP, not from show.** The engine:
   `if(d==='tap') return run.tapAt>run.shownAt&&Date.now()-run.tapAt>=320` — any tap after show qualifies and
   the advance settles 320 ms after it. The first wording would have made `TourMachine` IGNORE a prompt tap.
   Applied: the advance paragraph, `TourMachine`'s rule and doc, and `TourMachineTest`'s two tap cases (a
   prompt tap advances 320 ms later; `shown` resets the tap so Back never inherits one).
5. **CONFIRMED (minor) — the tables hold TWO EN "Back" rows.** `surface.back.short` (the back WALL, same
   मराठी) is where the RAW table files `tour55.js`'s own Back lines; `app.aria.back` remains the right key by
   M2's recorded rule (a navigation control must not share a surface word's key), but "the table's one EN
   Back row" was false as written. Applied: the chrome-keys paragraph.
6. **CONFIRMED (minor) — the placement fixtures lived only in the scratchpad**, against this repo's own
   "workbench, not storage" rule, written after that exact loss. Applied: the 146 step rects are a committed
   :pano test resource (`tour/tour-place-rects.json`) and the Playwright driver that exports them is
   committed beside the plan (`android/docs/port61c/tour-drive.mjs`).

**Build notes** (the narrowings the build made, recorded so nobody rediscovers them as bugs):
- The REVEAL (scrolling a target into view in its own list) is not built: a target off screen leaves the
  bubble centred with no spotlight — the engine's own missing-anchor shape. The demo sits near the top of
  every list it is in, so no shipped step needs the scroll.
- No host feeds `TourMachine.tap` yet: no SHIPPED step carries a `done: tap` rule (they are all in blocked
  flows). The machine carries the rule, tested, for the flow that first needs it.
- Only the shipped flow's anchors are tagged, and the Browse/capture tags are the DEMO rows' only (a real
  row never writes an anchor rect, so a registry entry can never name a client).
- The end-of-tour restore waits for the survey dialog to leave before writing the snapshot back, because the
  dialog saves unsaved work in its own dispose (`SurveyPrepScreen`'s `onDispose` save) — restoring first
  would let that save write the practice back over the restored prep.
- `tour.phases.5.body` is flagged `prototype-only` in the table ("the prototype sends nothing" is false of
  the phone), so i18nGen generates no accessor and the step's body is empty until the `phases` flow's phone
  wording is approved on the prototype; `TourFlowsTest` pins the one exception. The flow is blocked anyway.

## Progress, 3 Nov 2026 ~15:25 IST

0.3.272 (main 0fa7a1a = batch C part 1: K1b, AQ, M4) is on the Mi A3 from Play; Amit: "Updated — wish list reads
right" under मराठी. Batch C part 2 (QM0 ee60897+c80500a, PL 368f2ff+0154ad9) is on batch-next in CI; QV is building.
M4 is therefore done; the remaining Marathi slices run alongside: M3a, M3b, M3c, M8, M2, M6.

## Progress, 3 Nov 2026 ~15:25 IST
== M3a
13:39 journal.jsonl
14:04 agent-a6a07e76d3242580d.jsonl
e7bb296 APK: fix the review of port 61c slice QM2 (an Approve sends the scope_at its ticks were opened from, never the mirror's at submit time; the strip is busy while an Approve is out, and the sheet cannot be dismissed with its submit out; the rotation loss recorded as app-wide)
14:07
