# DESIGN-port61c.md — RECOVERED EXCERPT (3 Nov 2026, ~23:05 IST)

The full port plan lived only in the session scratchpad and was lost when the cloud container was re-created at ~22:50 IST
on 3 Nov 2026 (the scratchpad is ephemeral; nothing had committed the plan). What follows is every part of it the session
transcript still held verbatim: the slice table, §14 AS1, §15 FW1, §16 FW2, §25 M3a and the progress notes. The other
sections (§1–§13, §17–§24, §26–§31) are NOT here. For those slices the sources are the approved prototype
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
