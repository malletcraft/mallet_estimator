# DESIGN-port61c.md — RECOVERED EXCERPT (3 Nov 2026, ~23:05 IST)

The full port plan lived only in the session scratchpad and was lost when the cloud container was re-created at ~22:50 IST
on 3 Nov 2026 (the scratchpad is ephemeral; nothing had committed the plan). What follows is every part of it the session
transcript still held verbatim: the slice table, §14 AS1, §15 FW1, §16 FW2, §25 M3a and the progress notes; and §18 K3,
re-derived on 9 Nov 2026 from the prototype, S5 and the shipped code (it names the source of each decision). The other
sections (§1–§13, §17, §19–§24, §26–§31) are NOT here. For those slices the sources are the approved prototype
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

## Progress notes recovered

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
