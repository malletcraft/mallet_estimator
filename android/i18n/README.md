# android/i18n: the phone's two string tables

S2 APK shows Marathi from a table generated from the two string files in this folder, which the phone
and the prototype share. Since port 61c slice L2 (28 Oct 2026) this folder is their AUTHORITATIVE copy
(decision 3): nothing imports a prototype's tables over them any more, and a new or changed key is a
reviewed commit here. It is not the only copy yet, because the prototype has not switched to reading
them from here. Prototype v61c.1 (Version 69) was built before L2. It embeds the 1,607-key pair that L1
landed, which lacks L2's 194 keys and still has the old Marathi of `app.x_over_x`. Until the next
prototype build reads this folder at a pinned commit, the old prototype builders refuse to run (decision
3 below says how). The design note (`DESIGN-marathi.md`, 27 Oct 2026) lives in the session's notes, not
in this repository, so this README carries every rule it sets for this folder. The work ships in
slices. Slice 1a (this folder, the generator and its checks) changes nothing on the phone. Slice 1b
(the provider, the pref and the settings drawer keyed) changes nothing visible either: the row is
hidden, so every screen is `Tx.EN`, whose English is byte-for-byte what the drawer said before. Slice
1c (the project list, the result banner and the counts) is the same: keyed English, identical on
screen. Port 61c slice L2 adds keys and no reader.

## Amit's decisions, 27 Oct 2026 (~06:05 IST)

1. **Mechanism: a generated Kotlin table, `Tx`, in `:pano`.** Not Android resources. `:pano` holds
   691 of the shown texts and cannot see `R.string`. Play's `.aab` would also split `values-mr` away
   from a phone whose system language is English.
2. **The भाषा · Language row stayed HIDDEN until every screen was translated** — REVERSED 28 Oct 2026
   (~13:10 IST): Amit asked "Why marathi is still not available in 0.3.264?" and chose "Show it now,
   translate alongside". The row is shown from the build that carries port slices L1, L2 and W.
   Under मराठी a screen not keyed yet stays English until its slice; `unkeyed.tsv` is the list of
   those texts and only shrinks; English stays the default on first run. The row is still behind one
   constant, `LangRow.VISIBLE` in `pano/.../i18n/Msg.kt`, now `true`, so turning Marathi off again is
   that one line; `I18nLiteralTest` pins it shown.
3. **This folder is the ONE home of the two tables.** The prototype builder is to read them from here,
   at a pinned commit, from its next build on. Up to v61c.1 each prototype version was imported over
   them (`i18nImport`, checked with `i18nCheckProto`); L2 deleted both tasks, so no prototype table can
   be imported over the card again, and a new prototype key is a reviewed commit here like any other.
   A lost session loses no table.

   **Where the prototype's switch stands (L2 review, 28 Oct 2026).** The prototype builders live in the
   session's notes, not in this repository. Their shared reader there, `i18nhome.py`, does two things:
   - `tables(pin)` gives a build this folder's pair at a pinned commit. It refuses a pin older than the
     newest commit that changed the pair, and an uncommitted edit to it.
   - `refuse_copy` is the drift guard. Before anything is written, it refuses any table pair that is not
     this folder's, byte for byte, as committed.

   The v61c builders (`_keys61c.py`, `fix61c.py` and `integrate61c.py`) call `refuse_copy`, so they
   refuse today. A page rebuilt their way would draw the Marathi that L2 fixed. On the prototype's side,
   this guard replaces the drift check that `i18nCheckProto` was. No prototype has been built from this
   folder yet: that is the next prototype build, and it reads its pair with `tables(pin)`.
4. **The Marathi for new phone texts is approved ONCE, as the whole prototype v61c card, before
   slice 2.** Slices 1a, 1b and 1c went ahead first: they key only texts that already have a
   prototype key, and they show nothing new. The card was approved with v61c on 28 Oct 2026 and
   landed in port 61c slice L2 (below).

## Amit's decisions, 28 Oct 2026 (~07:15 IST)

5. **The prototype's English wins.** Where the phone's English and the prototype's differ (case, a joined
   word, one word), the phone takes the prototype's key, English and Marathi, screen by screen, in the
   slice that keys that screen; each such commit lists the English that changed.
6. **"Claude will do all Marathi translation at full authority. Amit will test it directly on fone apk".**
   The screens the prototype never had (Face Prep, Annotate and voice notes, the Laser test, the S5
   sign-in sheet) get Marathi written by Claude straight into these tables, in the slice that keys the
   screen; Claude also finalises the card's wording. Amit tests it on the phone once the row is on.

## The v61c card (port 61c slice L2, 28 Oct 2026)

L2 added 194 keys (1,607 -> 1,801) and no reader: 171 are the phone's own texts on screens the prototype
has (group 1), 23 are the 14 English-alternative templates split into whole keys (group 2; the card
reuses 6 keys the table already had for the rest). The 8 pairs the card decided are among the 171. The 14
old templates stay in both tables, refused, until the prototype's own sites use the split keys
(`PINNED_REFUSED` in I18nTableTest only shrinks); the phone calls the split keys.

- **A phone text's entry** (group 1) carries the flag `phone-text` and a `where` list naming the phone
  source (`S2 APK phone text · MainActivity.kt:1045 at 6575b85 · …`): it has no prototype site, so the
  prototype builder draws it nowhere. Its `placeholders` use the generator's grammar in the phone's
  names: a `{pl}` is `n===1?'':'s'` and folds into `{n}` (an `Int`); every other value is a description,
  so the accessor takes it as a `String`, shown as given.
- **A split key** (group 2) keeps its old template's `where` and flags byte for byte, so the site checks
  (I18nTableTest 9, TxRenderTest's doubled separator) see it where the prototype draws it; its Marathi
  `note` names the template and the case it covers.
- **Marathi Claude finalised** (decision 6), each with an `L2 finalised` note on the key:
  `app.x_over_x` (left off the card: "2 दिवस मध्ये 3 फोटो" became "3 फोटो · 2 दिवस", as `app.over_x`'s
  review fix), and nine card values: a doubled मीटर in `sp.laser.not_metric_x`, ब्लूटूथ for the one
  Latin Bluetooth, the gendered त्याची / त्याचे / त्याच्या that was wrong for a wall or the floor in five
  `sp.*` keys, and a plural गोष्टी that read "1 गोष्टी" in the two `sp.file.part_unreadable_*` keys.

## Port 61c slice K1b (3 Nov 2026): the album is keyed

`pano/Album.kt` joined `ported.txt` and its 165 `unkeyed.tsv` rows went. Every text it shows is a `tx.<key>` read
through a function that takes a `Tx` (`Refusal.text`, `workName`, `payOf`, `dayOf`, `decidedText`, `roleLabel`…);
its work names, five states and 39 worksite stages are S5's identity values, shown through `Masters` and marked
`// text:value` on their lines.

- **Deleted, 14 keys** (SPEC61c §6.4 ORPHAN: rendered nowhere since v60, deleted with their last Kotlin reader, the
  v59 block of `Album.kt`): `album.site_is_at`, `album.stage_caps`, `album.moved_by`, `album.stage_log_x`,
  `album.no_moves_yet`, `album.via_sheet`, `album.via_nudge`, `album.nudge_x`, `album.aria.nudge`,
  `album.move_project`, `album.moved_x`, `album.work_stage_moves`, `album.closed_handover`, `album.closes_handover`.
  `album.filed_x` stays (§6.4: still rendered).
- **Added, 4 phone texts** (flag `phone-text`; Marathi by Claude, decision 6), the album gates' refusals the card
  did not have: `refuse.album.cr_not_approved` (a change request is raised and answered only at Client approved,
  §14.8), `refuse.album.cr_off_scope_remove` and `refuse.album.cr_off_scope_change` (S5's own 409 for a remove or a
  change on a work outside the scope, one whole key per kind so no English word fills a placeholder) and
  `refuse.album.review_no_image` (S5's 409 before the JPEG lands, without "; not saved").

## Port 61c slice M4 (3 Nov 2026): the wish list is keyed

`WishListScreen.kt`, `WishSync.kt` and `pano/WishList.kt` joined `ported.txt` and their 34 `unkeyed.tsv` rows went.
Every text the Wish list draws but one is a `tx.<key>` (the exception is the top bar's sub-line, below), through functions that take a `Tx` (`chip`, `total`, `subLine`,
`rowLine`, `savedText`, `banners`, `noteLine`, `statusLine`); the twelve services and their two descriptions are S5's
identity values, marked `// text:value` and shown through `Masters.service` / `Masters.service_note` (`WishList.name`,
`WishList.detail`), and a room through `Masters.room` (`WishList.roomName`), a typed one as typed. Every key the screen
draws but one (below) was already in the tables; their Marathi is the prototype v61c.1 table's, word for word, and the
four phone texts from L2's card that it draws (`wish.not_sent_s5`, `wish.s5_newer_list_replaced`, `wish.on_s5_since_x`,
`wish.water_drain_points_taps`) are unchanged.

- **Stored in English, drawn through a key.** The two notes the phone writes into its copy of a list, `REPLACED` and
  `NOT_SENT` before S5's reason, stay byte for byte what every earlier build stored, so a list kept before M4 is drawn
  in the language too and nothing is migrated: `noteLine` draws them as `wish.s5_newer_list_replaced` and
  `wish.not_sent_s5`, S5's own reason as sent.
- **Added, 1 phone text** (flag `phone-text`; Marathi by Claude, decision 6): `wish.s5_answered_x`, "S5 answered
  {code}", WishSync's stand-in when S5 refused a save with no `error` of its own (`WishList.s5Answered`, stored in
  English like the rest of the note), worded as `app.note.web_answered_x`.
- **masters.tsv** gains the row `service_note` → `wish.water_drain_points_taps`, the 12th service's description, and MastersTest's
  pass-through list loses it.
- **Still English under मराठी: the top bar's sub-line** (corrected by the M4 review; the M4 commit said every text was
  Marathi). It is QuoteStage's quote line, `QUOTE STAGE · Client wish list` (or `· not read yet`, `· not on S5 yet`),
  built by `QuoteStage.lineFor` / `phoneOnly`, which take no `Tx`; its texts are still `unkeyed.tsv` rows of
  `QuoteStage.kt`. Slice PL replaces it with the one derived project line, keyed, and deletes `lineFor`, so it is not
  keyed here. Until then it is the one English text on the Wish list, and a difference from the prototype, whose bar
  draws `projectLine(p).text` through its text layer (`index61c1.html:8562`). With the stage unread the English
  `QUOTE STAGE · not read yet` sits over the Marathi banner `wish.quote_stage_not_read`.
- **Added by the M4 review, 1 phone text** (flag `phone-text`; Marathi by Claude, decision 6): `refuse.wish.staff_only`,
  S5's 403 for a caller who is not staff, `Only MCFT staff can change the wish list` byte for byte (asis.js at 9a51733)
  -> `इच्छा-यादी फक्त स्टाफ बदलू शकतो`, worded as `refuse.stage.staff_only`, the stage route's 403. It is the one
  refusal for good a correctly built phone meets (a 400 needs a phone bug, a 413 a body past 2 MiB), and before it the
  note read `S5 ला पाठवले नाही — Only MCFT staff can change the wish list`: `WishList.reasonLine` matches the stored
  reason against the key's English, as `statusLine` matches `refuse.stage.approved_final`, so `noteLine` and
  `statusLine` draw it in the language. The stored note stays S5's English. A 400's words name the input and are still
  drawn as S5 sent them.

## Port 61c slice QM0 (3 Nov 2026): the project line and the quote move are born keyed

`pano/QuoteMove.kt` (new) joined `ported.txt`: the guarded `PUT …/stage` and `PUT …/album/scope` and S5's answers to
them, every status line a `tx.<key>` through `QuoteMove.text` / `localText` (the `refuse.stage.*` keys, each S5's
asis.js words verbatim, and `wish.not_s5_yet_project`; since the slice's review, S5's 401 through the phone's existing
sign-in words, `app.sign_again_menu_server_x`, with S5's own words as its `why`); S5's 400 words and its not-yet 409 are
data, shown as sent.
`QuoteStage.projectLine` and `coverageParts` (the one derived line, SURVEY COVERAGE) are keyed through existing keys
(`common.quote_x`, `app.execution`, `app.x_in_progress`, `app.x_done`, `app.x_on_hold`, `app.x_abandoned`,
`app.no_work_started`, `wish.*`, `phase.*`); `QuoteStage.kt` itself stays out of `ported.txt` until slice PL deletes
`lineFor`, `phoneOnly`, `TITLE` and the `NOT_*` texts, whose `unkeyed.tsv` rows go with them.

- **Added, 1 phone text** (flag `phone-text`; Marathi by Claude, decision 6): `refuse.stage.scope_final`, S5's 409 to
  Record the approved scope at Client approved with a scope stored, `Approved — the scope changes only through change
  requests; not saved` byte for byte (asis.js `FINAL_SCOPE` at 9a51733) -> `मंजूर झाले — स्कोप फक्त "बदल विनंती" मधूनच
  बदलतो; सेव्ह झाले नाही`, worded as `refuse.stage.approved_final`. The prototype's recordScope has no such answer.

## Port 61c slice PL (3 Nov 2026): the one project line, keyed; PROJECT IS AT leaves the phone

`pano/QuoteStage.kt` and `BrowseScreens.kt` joined `ported.txt`, and `JsonRead.kt` (new; org.json read through
`AlbumWire.Read`, no shown text) with them. Their 36 `unkeyed.tsv` rows went, and the project sheet's 8 stage rows of
`TreeShell.kt` with the code. The project screen, the project list row and the Wish list's top bar draw ONE line,
`QuoteStage.projectLine` (QM0's keys, above): the last English text on the Wish list, M4's exception, is keyed now. The
six stages, the months, the zone id, the ✓ and the two badges (`PRJ`, `SITE`, the prototype's own unkeyed literals) are
marked `// text:value`. Deleted with their readers: the old QUOTE STAGE line (`lineFor`, `Line`, `phoneOnly`, `TITLE`,
the `NOT_*` texts and `since`), StageBar and the project sheet's At stage, Phase, Phases and Stages rows.

- **Deleted, 2 keys** (SPEC61c §6.4 ORPHAN: no phone reader and no prototype renderer): `phase.aria.quote_journey_done`
  and `phase.aria.quote_stage_estimated_four`. 1,792 keys each.
- **Kept, no phone reader any more** (LENS-ONLY, SPEC61c §6.4: the prototype's v53 lens still draws them):
  `app.project_is_at`, `app.no_stage_set_tap`, `app.x_not_shot_stage`, `app.every_photographed_room_current`,
  `app.since_x`, and the project sheet's `app.at_stage`, `app.phase`, `app.phases`, `app.stages`,
  `app.x_master_job_type` (the sheet's texts were never keyed on the phone).
- **Fixed by the PL review: identity values drawn raw.** PL listed `BrowseScreens.kt` as fully keyed while it drew the
  room tile's name (and the photo's description), the project row's job type and the site row's type as stored, and
  the dates and the pill from `Catalogue`'s English: under मराठी the row read `… · Supply & install · from 5 Oct  ACTIVE`
  where the prototype draws `… · पुरवठा व बसवणे · 5 Oct पासून  चालू`. A value with no literal is invisible to
  `I18nLiteralTest`, so no check caught it. They are drawn through `Masters.room`, `Masters.job` and `Masters.site`
  now (the badge and the room token stay from the English), and `Catalogue.Project.dateRange` and `statusLabel` take
  the `Tx` (`app.from_x`, `app.due_x`, `app.offline`, `app.done`, `app.cancelled`, `app.active`; a whole range stays
  `5 Oct → 30 Nov`, as the prototype draws it). MainActivity's top bar draws its job type through `Masters.job`.
  Their 7 `unkeyed.tsv` rows went. No new key.
- **The pips are labelled** (PL review): `QuoteStage.ProjectLine.pips`, the prototype's aria-label, read by TalkBack:
  `export.x_x` (`2 of 6`, under मराठी `2 / 6`, the key the prototype's text layer renders it with), `wish.not_read_yet`
  and `wish.not_on_s5_yet`. A stage named without its stamp reads `<stage> · not read yet` on the second line
  (`wish.not_read_yet`), never `not set on S5 yet`. No new key.

## Port 61c slice QV (3 Nov 2026): the album mirror and the approved quote view, born keyed

`pano/QuoteView.kt` (new), `ScopeSheet.kt` (new) and `AlbumSync.kt` (new) joined `ported.txt`. At Client approved the
project line is the tap that opens the quote view (the five ticked steps, the Approved chip, the APPROVED SCOPE row)
and the scope sheet in its view and record modes; every text they draw is a `tx.<key>` through `QuoteView` (`segments`,
`approvedChip`, `scopeRow`, `title`, `note`, `countLine`, `closeLabel`, `submitLabel`, `emptyRefusal`), a work's name
through `Masters.work` (`Album.workName`), the stamps through `Album.whenOf`. **No new key**: the view and the sheet are
the prototype's own keys (`phase.scope_*`, `phase.tap_view`, `phase.approved_chip`, `phase.aria.approved_fold`,
`phase.aria.phase_x_of_5_x`, `refuse.stage.pm_scope`, `common.close`, `common.cancel`), the five steps are the strip's
(`quote.survey`, `common.wish_list`, `life.vrev`, `common.estimation`, `common.revisions`), and the row's `since` part is
`app.since_x` -- LENS-ONLY since PL, with a phone reader again. The refusal a Record meets is `QuoteMove.text` /
`localText` (QM0's keys); the one new local refusal, a phone with nothing to send with, reads the existing
`app.sign_first_menu_server`. Tx is not regenerated: the tables are unchanged. `AlbumSync.kt` shows nothing; its log
lines are marked `// text:value`.

## Port 61c slice QM1 (3 Nov 2026): the phase strip and the guarded stage move, born keyed

`pano/Phases.kt` (new) and `PhaseStrip.kt` (new) joined `ported.txt`. Through the quote journey the project screen
draws the phase strip under the line -- the five segments, the does-line, the refusal line, the backward confirm,
SURVEY COVERAGE at Survey and IN REVISION at Revisions (the listed subset of the prototype's strip, Amit's answer 1 of
28 Oct: no VENDOR PACKS, no GOES TO ESTIMATION, no hand-off counts; Approve the estimate ▸ is QM2's) -- and a tap on a
segment is the phone's guarded move of the quote stage (`QuoteStageSync.move`). Every text is a `tx.<key>` through
`Phases` (`all`, `segments`, `doesLine`, `ask`, `coverageRow`, `revisionRow`, `groupLabel`): the five names
(`quote.survey`, `common.wish_list`, `life.vrev`, `common.estimation`, `common.revisions`, now the one home of the five
that QuoteView's ticked steps read too), the does-lines (`phase.capture_bound_walls_mark`,
`phase.walk_client_through_mark`, `phase.trades_vendor_packs_wishes`, `phase.hand_off_estimate_nothing`,
`phase.rev_does`), the unread line (`phase.phase_not_read_yet`), the never-sent words (`wish.not_s5_yet_project`,
v61c.2), the segment labels (`phase.aria.phase_x_of_5_x`, `phase.aria.phase_x_of_5_x_now`), the confirm
(`phase.back_x_quote_stage`, `phase.every_mark_stays_tools`, `phase.keep_x`, `phase.move_back`,
`phase.aria.move_phase_back`), the rows (`phase.survey_coverage` with QuoteStage.coverageParts' `phase.x_x_shot`,
`phase.x_not_shot_yet`, `phase.every_room_scope_shot`; `phase.in_revision`, `phase.open_client_vendors`) and the
group's label (`phase.aria.phase_project`); a quote stage in the confirm goes through `Masters.quote`. The refusal a
move meets is `QuoteMove.text` / `localText` (QM0's keys). **No new key**; Tx is not regenerated. The row separator in
`Phases` and the circle's ✓ in `PhaseStrip` are marked `// text:value`.

## Port 61c slice QM2 (3 Nov 2026): Approve the estimate, born keyed

No file is added: `Phases.kt`, `PhaseStrip.kt`, `QuoteView.kt`, `ScopeSheet.kt`, `QuoteStageSync.kt`, `MainActivity.kt`
and `SyncWorker.kt` change, every one already in `ported.txt`. At Revisions the panel's right end carries
`Approve the estimate ▸` for the Project Manager (`phase.approve_estimate`, through `Phases.approve`), and for anyone
else S5's own 403 words on a line of their own (`refuse.stage.pm_approves`, the same key QM0 gave the 403's status
line, so the panel and the sheet never say two things); before the mirror has said the role, neither. The button
opens the scope sheet's approve mode (QV's keys: `phase.scope_title`, `phase.scope_note`, `phase.scope_ticked_x`,
`phase.scope_move_approved`, `common.cancel`), pre-ticked from the mirror's scope (`QuoteView.openTicks`), and its
Move to Client approved is the guarded move carrying the ticks (`QuoteStageSync.approve`), whose every refusal is
`QuoteMove.text` / `localText` (QM0's keys: `refuse.stage.pm_approves`, `refuse.stage.scope_changed`,
`refuse.stage.scope_missing`, `refuse.stage.changed_x`, `refuse.stage.approved_final`, the sign-in, no-answer and
offline lines). **No new key**, no text changed; Tx is not regenerated; `unkeyed.tsv`, `ported.txt` and
`literal-counts.tsv` are unchanged.

## Port 61c slice M3a (3 Nov 2026): survey prep's chrome is keyed

`SurveyPrepScreen.kt`, `SurveyPrepTools.kt`, `SurveyPrepLocks.kt`, `pano/SurveyPrep.kt`, `pano/SurveyPrepPalette.kt`,
`pano/SurveyPrepEdit.kt`, `pano/StepRoute.kt` and `pano/SurveyGuide.kt` joined `ported.txt` and their 245 `unkeyed.tsv` rows went
(1,171 -> 926). Every word of the tabs, the step strip, the Show row, the captions, the status and bounded lines, 1 · Lines' panel,
2 · Mark's selection rows and D2 strip, the palette, More ▸, the tool chip, the long-press menu, the clears, Start over, Discard,
the room size sheet, the List's table and its chain rows, the snackbars and the notes is a `tx.<key>` or a `Masters` name. **No
new key**: every text had one in the tables (the card's 37 survey-prep keys and its group-2 splits among them), so Tx is not
regenerated and the tables hold 1,792 keys as before.

- **Identity values, drawn through Masters.** `SurveyPrep.NAME` and `TAB` (a surface's name and its tab's short name:
  `Masters.surface`, `Masters.surface_tab`), `SurveyPrep.TYPES` (a mark type's name: the `when` key of its icon, what
  Pano360.placeRefusal compares, the English every menu composes from: `Masters.type`, through `SurveyPrep.typeName`) and
  `SurveyPrepPalette.SHORT` (its short form: `Masters.type_short`, through `SurveyPrepPalette.short`) stay English constants,
  marked `// text:value`. So do the laser's axis letters L, W and H (`sizeTag`, the size sheet, ⋮'s size line), the glyphs
  (←, ↶, ⋮, ✎, ✓, ✕, Δ, the nudge arrows) and the unit `mm` in an empty reading box. A lock group's `name` ("tapes",
  "Line marks", a type) is an identity drawn through `SurveyPrepEdit.groupName`.
- **A mark's checklist labels are a `Msg` each.** `SurveyPrep.Measure.what` is `(Tx) -> String` over the `measure.*` keys
  (", to centre" joined as sp.js joins `measure.to_centre`), so `measures0` / `measures` take no `Tx` and SurveyPlan's four
  keys-only readers are untouched. The String constructor is for the two checklists not keyed yet, a Line's (`SurveyMeas.measures`)
  and a chain's (`SurveyChain.measures`), port M3b's; it goes with M3b.
- **The four boundary lines' names** are `SurveyPrep.lineName(tx, s, slot)` (`edge.*`). `lineNames(s)` now RENDERS those keys in
  English for the two readers that draw no language yet -- the on-photo painter (`SurveyPrepDraw.drawLines`) and the salvage
  notes (`SurveyPrepFile`), both M3b's -- so the words live once, in the keys.
- **Functions that took no language now do** (`tx: Tx` first): `SurveyPrepEdit.refusal`, `linesRefusal` (was the constant
  `LINES_REFUSAL`), `deleted`, `linesSaid`, `linesState`, `linesAction`, `lockedSnack`, `startOverLabel` / `startOverArmed` (were
  the constants `START_OVER` / `START_OVER_ARMED`), `startedOver`, `clearedSnack`, `clearLabel`, `labelOf`, `deleteLabel`, `menuHeader`,
  `menuHeaderOf`, `menuItems`, `menuItemsOf`; `SurveyPrepPalette.toolName`, `short`, `moreLabel`, `chipText`, `sheetTitle`;
  `StepRoute.tabName`, `label`, `stepName` (new; `STEPS` is the four keys alone), `stripFace`; `SurveyPrep.sizeHint` (was
  `SIZE_HINT` / `SIZE_HINT_BAD`), `outNote`, `Prep.outsideNote`, `placeMiss`; `SurveyGuide.lockedEnd`; and, because they call those,
  `SurveyChain.lockedNode` and `SurveyRef.lockedSaid` (the latter now `refuse.sp.x_reference_locked_unlock`, the prototype's own
  whole key). The tests keep their English literals with `Tx.EN` added: SurveyPrepTest, PaletteTest, LockTest, ClearTest,
  StartOverTest, StepRouteTest, MeasTest, ChainTest, Pano360Test, EndMoveTest, RefFrameTest, RoomSizeTest and OnFaceTest
  (GuideTest pins no text; `lockedEnd` is pinned in EndMoveTest and ChainTest).
- **The prototype's English wins (decision 5)**, so these change on the phone: the eye's state is read out as `on` / `off`
  (`sp.on`, `sp.off`); the List's size tag ends `· Ref` (`common.ref`, the card's row for the phone's `ref`) and `· Laser`
  (`app.laser`: Claude's extension of that row to the tag's other branch, decision 6 -- the card's only `· laser` row is the
  chain total's, which is keyed whole as `sp.total_laser`) where the phone wrote `ref` / `laser`; the room
  List's two lines name the surface, `Front wall · set its four lines` (`sp.x_set_four_lines_2`) and `Front wall · set its four
  lines in 1 · Lines to see it squared` (`sp.x_set_four_lines`), where the phone said `Set its four lines first.` and `Set its four
  lines in 1 · Lines to see it squared.`; and a face's 3 · List status always writes `measurements` (`sp.x_x_mark_x`), where the
  phone wrote `1 measurement`. Nothing else Amit sees changes in English: the snackbar's `UNDO` and ⋮'s `THIS 360` stay as
  they were, drawn through `sp.aria.undo` / `sp.this_360` upper-cased, as the prototype's CSS (`.spv .spmenu .mh`
  `text-transform: uppercase`) and its raw `UNDO` button literal draw them; `uppercase()` is a no-op on Devanagari, so
  हा 360 and मागे घ्या are untouched (the M3a review: the slice's first version drew `Undo` / `This 360`, which the card's two
  'case only' rows had compared with the key text rather than with the prototype's screen).
- **The four Marathi-only line heights** (design-marathi §4, each `if (tx.mr) … else <today's value>`, `Tx.mr` being the one
  layout flag): the face tabs 17 sp (SpTab), the reading rows 17 sp (the table's reading and chain rows) and the More ▸ tile
  names 14 sp (SpMoreSheet). The fourth, More ▸'s ROW LABELS, has no element on the phone: the prototype's v53 More sheet groups
  the types by trade with a label per row, and the phone's More ▸ is one grid with no rows, so there is nothing to size.
- **Long-lived messages are a `Msg`**: `savedMsg` (the bar's `Saved ✓ 12:34`) and `readingsMsg` (⋮'s web line) are drawn in the
  language on when they are drawn. `say()` and the snackbar keep a String (2.6 s and 5 s): a note raised before a language flip
  is not redrawn. **Still English under मराठी on survey prep**, each a later slice's: the opened file's own words
  (`SurveyPrepFile.savedText`, `openedNote`, `pointsNote`), the readings' conflict texts and not-yet line (`ReadingsSync`,
  `Readings`), the 360 caption and refusals (`Pano360`), the Size from row, the reference's own texts (`SurveyRef`), a Tape's,
  Line's and chain's labels, chips and rows (`SurveyMeas`, `SurveyChain`), the words drawn on the photo (`SurveyPrepDraw`), all
  M3b; the laser's states (`DistoClient`), M6; the upload status line (`AsIsUpload`), M2.
- **Review fixes, 3 Nov 2026** (seven confirmed findings on M3a, fixed by Claude). (1) `SurveyPrepScreen.kt`'s SECOND reader
  of the deleted `SurveyPrepEdit.LINES_REFUSAL` (the SpPhoto callback) now calls `linesRefusal(tx)`: `:app` would not have
  compiled, and nothing here said so, since (f) scans `tx.<name>` alone. `I18nLiteralTest` gains check (i), the other half
  of the compiler stand-in: every `Name.member` of a `:pano` object read from `:app` or `:camera` must resolve by reflection.
  (2) `THIS 360` and `UNDO` kept, above. (3) `Laser` recorded as Claude's extension, above. (4) `StepRoute.tabRowDp(land,
  list, mr)`: a landscape list's tab row is 27 dp under मराठी, since SpTab's 17 sp name over a Ref line's 9 sp is 26 and the
  24 dp row gave the Ref line the 7 dp left and clipped it (`budgetDp` takes `mr` too: 74 for a face's 3 · List, 54 for the
  room's List); pinned in StepRouteTest; the phone is the only proof of the drawing. (5) Three Marathi values L2-finalised,
  `{s}: रेषा लॉक`, `{s}: रेषा अनलॉक` and `{s}: संदर्भ लॉक आहे — आधी अनलॉक करा`: the nominative name straight before रेषा /
  संदर्भ read as two nouns (समोरील भिंत रेषा लॉक) where Marathi wants the genitive, which changes the name itself (भिंतीच्या,
  जमिनीच्या, छताच्या) and no filled placeholder can give, so the name is set off by `:` as `sp.aria.lock_x_four_lines` and the
  `sp.file.x_*` keys do; Tx regenerated (`TxMr.kt`); English untouched. (6) The three `{ t: Tx -> … }` Msg lambdas in
  `SurveyPrepScreen.kt` name their parameter `tx` (shadowing the composable's: a warning), so (f) resolves their five reads,
  and a check refuses a Tx parameter named otherwise. (7) `SurveyPrepMarathiTest`: a hand-typed Marathi pin per Masters-backed
  M3a lookup (`typeName`, `short`, `groupName`, `tabName`, `stripFace`, `label`, `lockedSaid`, `linesSaid`, the checklist
  labels), because every Masters map passes an unknown value through unchanged, so a lookup wired to the wrong map or fed the
  wrong identity prints the same English in every `Tx.EN` pin and leaks Latin only under मराठी (the PL review's fault).

## Port 61c slice FW1 (3 Nov 2026): the room screen reads photos by work

No file is added to `ported.txt`: `CaptureScreens.kt` keeps its STG row and the rest of its texts until FW2, and
`MainActivity.kt` and `Catalogue.kt` are far from keyed. The room screen groups a room's captures by WORK (the
prototype v61c.2 room screen, SPEC61c §4.5: work ids high to low, then the Quote journey, then not filed to a
work; the order is `Album.filingRank` / `filingOrder`, pure and tested), with work chips in place of the phase
chips, and every text it draws is a `tx.<key>` or a Masters name -- **no new key**: the empty line
(`app.no_captures_room_yet`), the `All n` chip (`app.all_x`), the header's `n photos (over n days)`
(`app.x_over_x`, `noun.photo`, as before), the Quote journey and not filed to a work (`album.quote_journey`,
`album.unfiled`, through `Album.workName`) and the sixteen work names (`Masters.work`). The capture title is
`date · <where filed>` and the viewer's sub-line drops the stage, both through `Album.workName`. Tx is not
regenerated; the tables hold 1,792 keys as before. Their 7 `unkeyed.tsv` rows went (958 -> 951): CaptureScreens'
`All ${captures.size}`, `No captures in this room yet. Use Capture 360 below.`, `No stage` and the tile's
`${c.date} ${c.stage}` (its TalkBack label, now `date · <where filed>` like the title); MainActivity's
`${cap.date} · ${cap.stage.ifBlank { "no stage" }}`, `no stage` and ` · ${cap.stage}`. **Still English under
मराठी on the room screen**, FW2's: the tile's `Details`, `PHOTO`, `synced` / `on phone`, `Survey prep` and its
hint, `No work tagged`, and the capture screen's rows. `literal-counts.tsv` gains the row `AsIsPath.kt 0` that
slice AS1's new file never had: `i18nRatchet` rewrites the whole list, and the count is what the check expects.

## Port 61c slice FW2 (3 Nov 2026): new photos and re-files are written by work; the last stage pickers go

`CaptureScreens.kt` joined `ported.txt` and its 52 `unkeyed.tsv` rows went; the capture sheet (`TreeShell.kt`'s
`CaptureSheet`, and the new `WorkSheet`, Which work?) is keyed too, and its 37 rows and the stage picker's went with
it, with `MainActivity.kt`'s two picker headings (951 -> 860, the file's lines as the slices above count them; 919 -> 828 rows). **No new key**: every text the capture sheet,
the capture screen (the WORK line, the SKU row, the folder block, Remove local copy, Delete) and the face viewer draw
had one -- the prototype's own `album.*` row and line words (`album.work_caps`, `album.quote_journey`,
`album.pick_work`, `album.unfiled`, `album.tap_to_change`, `album.which_work`, `app.stg_locked_quote`, the five states
through `Masters.state`, a quote stage through `Masters.quote`, `wish.not_read_yet`, `wish.not_on_s5_yet`) and the
card's capture texts -- so Tx is not regenerated and the tables hold 1,792 keys as before. The WORK row's and line's
texts are built in `:pano` (`Album.filingHead`, `Album.quoteLead`, `Album.workSub`, each taking the `Tx`; pinned in
English and in Marathi by CaptureFilingTest), as the project line is.

- **The prototype's English wins (decision 5)**, so these change on the phone: the tile's corner chip is the noun
  `noun.photo` upper-cased (`PHOTO`, the card: case only; a no-op on Devanagari); Remove local copy always carries its
  size, `Remove local copy (12.3 MB)` (`app.remove_local_copy_x`, the unit in the key; the phone's blank-size branch
  went -- the row exists only for a kept file, so the size is always known); the height hint's range is the key's
  own `1829–6096` (`app.height_mm_1829_6096`), which is `mm(72 in)`–`mm(240 in)` as the phone computed it.
- **Marked `// text:value`**: the lead tokens `SKU` and `SVY` (the prototype's own raw leads; `WORK` is keyed,
  `album.work_caps`), the kept 360's file name `<id>.jpg`, the pen badge's `n/6` (no Latin), and in the new
  `WorkRefile.kt` the request's content type and its log lines.
- **Deleted with their readers**: the thirty-nine-stage picker (`StageSheet`, "Move the project to", "Stage for new
  photos", "Stage of this photo", the job-type narrowing and its "n more … does not normally reach" rows) and the
  capture screen's STG row ("No stage set", "no phase", " · tap to correct (recorded)"). Nothing on the phone shows
  or picks a stage any more; `common.no_stage_set`, `app.no_phase`, `app.tap_correct_recorded` and
  `app.tap_change_can_corrected` keep their prototype readers (the v53 lens) and lose their phone sites.
- **Still English under मराठी on the capture screen**, each a later slice's: the survey-prep row's note (`surveyNote`
  in `MainActivity.kt`: "Opening the 360…", "n of 6 surfaces bounded · …", "Not done yet · …", "The 360 is no longer on
  this phone · tap for why"), the FAB's `Capture 360`, the face viewer's sub-line (an id and a date) and the bench
  refusals ("Could not re-file on the bench: …"), all `MainActivity.kt`'s (M2); the camera's own note on the X3 row
  (`cameraNote`, the camera module's, M6).

**The FW2 review (3 Nov 2026)** adds ONE phone text, born keyed: `app.not_refiled_web_x` — `Not re-filed on the
web: {why}` / `वेबवर पुन्हा फाइल झाले नाही: {why}` (worded as `app.not_on_web_x` and `app.msg.could_not_refile_x`;
`{why}` is S5's own message, data), drawn under survey prep's web line (`SurveyPrepScreen.kt`, which stays in
`ported.txt`) when S5 refused a re-file by work, so the disagreement between the phone's filing and S5's is on the
screen and not only in logcat; the tables hold 1,793 keys and Tx was regenerated. No other text changes: the photo's
WORK line for a project only on this phone now reads `Survey` (`Masters.quote`, the prototype's own photo line,
index61c2.html:7249) where FW2 drew `wish.not_on_s5_yet`, and `wish.not_read_yet` is read on the same line wherever
the phone cannot place the capture's stage (`Album.quoteTakenAt`) — both keys the tables had.

## Port 61c slice K3 (9 Nov 2026): the album, read-only, born keyed

`pano/AlbumPage.kt` (new: the page's model), `app/AlbumScreen.kt` (new: draws it) and `app/AlbumImages.kt` (new: the JPEG
cache, no shown text) joined `ported.txt`. The Album tile closes the project's rooms grid, and the album opens in place of
the rooms: the header (`album.viewing_as_x` with the role through `Album.roleLabel`, `album.open_x`, `album.no_open_work`,
`album.to_review_x`, `album.change_requests_x`, the project line's own text, `album.work_log_x` and `album.no_work_log`),
chapter 0 (`album.quote_journey`, `album.quote_journey_sub`, `noun.photo`, `album.no_photos_yet`, `album.not_your_work`,
the six stages through `Masters.quote`, `album.now`, `app.since_x`, `album.not_reached`, `album.no_photos`,
`album.unfiled`), the banner (`album.exec_lock_banner`), the sixteen works (`Masters.work`, `album.last_photo_x`, the
state through `Masters.state` or `wish.not_read_yet`, `crkind.*`, `album.payment_x` over `album.pay_before_x` /
`album.pay_when_x`, `album.cr_raised_x`, the decided requests through `Album.decidedText`), the end line
(`album.closes_cleaning`, `album.closed_cleaning`), the cards (`album.staff`, `common.vendor`, `album.awaiting_review`,
`album.not_for_client`, a room through `Masters.room` else `common.whole_project`) and the viewer (`common.close`,
`album.photo_x_of_x`, `album.change_request`, `album.no_caption`, `album.accepted_by`, `album.aria.previous_photo` /
`next_photo`), with the TalkBack labels `album.aria.tile_x`, `album.aria.chapter_x`, `album.aria.state_x`,
`album.aria.payment_x` and `album.aria.cr_kind_x` -- every one a key the tables had, Marathi the prototype's.

**Three phone texts, born keyed** (flag `phone-text`; Marathi by Claude, decision 6), 1,793 -> 1,796 keys, Tx regenerated:
- `album.locked_until_approved` — `locked until the estimate is approved` / `अंदाजपत्रक मंजूर होईपर्यंत लॉक`: the prototype's
  own English under a locked work (index61c2.html 7453), drawn there with no key and so English under मराठी; worded as
  `app.stg_locked_quote`.
- `album.work_log_unread` — `Work log · not read yet` / `कामांची नोंद · अजून वाचलेले नाही`: the log of a listed project whose
  album the phone has not read; `Work log (0)` would say there is none. A WHOLE template, never `album.work_log_x`'s words
  glued to `wish.not_read_yet`.
- `album.newest_x` — `Showing the newest {pl}` / `सर्वात नवीन {pl} दाखवत आहे`, `{pl}` = `plural(n,'photo')` as in
  `album.sub_x` (the accessor takes `plN: Int`): S5 lists the newest 1000 photos a caller may see and says `more`.

Marked `// text:value`: `AlbumPage.SEP` (` · `), the Work log's ` → `, S5's surface name `front`, the ▾ / ▴ caret and the
‹ › glyphs, `AlbumImages`' log lines and cache-file name parts. `unkeyed.tsv` is unchanged (860 lines, 828 rows):
every new literal is keyed or a value. `literal-counts.tsv` gains the three new files at 0 (`i18nRatchet`).

## What is here

| File | What it is | Who writes it |
|---|---|---|
| `strings-en.json`, `strings-mr.json` | The two tables, 1,796 keys each: the prototype's 1,607 (v61c.1) and the v61c card's 194 (L2), less the 14 ORPHAN v59 album keys (K1b) and the 2 ORPHAN phase.aria keys (PL), plus 11 phone texts (4 in K1b, 2 in M4 and its review, 1 in QM0, 1 in the FW2 review, 3 in K3, above). In the prototype's byte format (one-space JSON, no trailing newline) and key order (family, then key); never reformatted. | a reviewed commit here |
| `masters.tsv` | Which keys show an identity value (a room, a stage, a mark type) whose key is not in its own family. Read into `Masters.kt`. | by hand |
| `index.tsv` | One row per key: key, accessor on `Tx`, kind, parameters (or why it has none). Grep it in either direction. | GENERATED |
| `unkeyed.tsv` | The worklist: each shown text that is not keyed yet (file, text exactly as in the source). It only shrinks. | each slice removes its rows |
| `ported.txt` | Files whose every shown text is keyed. | each slice adds its files |
| `literal-counts.tsv` | Per-file count of prose literals outside UI positions, minus those listed in `unkeyed.tsv` or marked `// text:value`: the ratchet. | `i18nRatchet` (only lowers counts) |
| `latin-ok.txt` | The Latin words a Marathi text may contain: units, product names, codes, demo names, months. | by hand, with a reason |
| `deny-patterns.txt` | The shapes that must never be in the tables, keys included: email, mobile and landline numbers, 10-digit number, currency figure, lakh/crore, PIN code. | by hand |

GenTx also writes `Tx.kt`, `TxEn.kt`, `TxMr.kt` and `Masters.kt` into
`android/pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/i18n/`. Each starts
`// GENERATED by GenTx`. Never edit them by hand. `Msg.kt` in the same folder is written by hand.

## How to

```sh
# Regenerate after editing a table or masters.tsv (both tables, same keys in the same place):
gradle -p /home/user/mallet_estimator/android :pano:i18nGen --offline

# After keying literals, lower the ratchet (it never raises a count):
gradle -p /home/user/mallet_estimator/android :pano:i18nRatchet --offline

# Everything below runs inside the projection tests, the first step of android-app.yml:
gradle -p /home/user/mallet_estimator/android :pano:test --offline
```

Accessor names follow the key: every `.` and `-` becomes `_`, so `app.display` is `tx.app_display` and
`noun.room` is `tx.noun_room(n)` with `tx.pl_room(n)` giving "3 rooms". A `{name}` placeholder becomes a named
parameter. A plural suffix (`n===1?'':'s'`) folds into the count it agrees with. Each `{name}` is filled
in one pass, so a typed note that contains `{x}` is shown as typed.

Parameters follow the ENGLISH placeholder order, so a prototype that rewords its English can reorder
them. A call to an accessor with two or more parameters therefore NAMES every argument
(`tx.app_x_x_rooms(size = done, length = total)`); check (f) refuses a positional one, which would still
compile with the two values swapped.

An optional tail (`{v}`, `String?`) is one of two kinds, and the KDoc says which. "the tail; ' · ' is
put before it": pass the bare value. "the WHOLE tail from its own key": the tail holds more than a
separator and a value (`· 2 skipped`), so pass the rendering of its fragment key, separator included
(`tx.export_x_x_client_approved(moved = 3, v = tx.export_x_skipped_2(v = "2"), r = …)`).

On the phone (slice 1b): `AppScreen` in MainActivity.kt provides the table ONCE, above every screen, as
`LocalTx` (`app/.../Lang.kt`, a static local with no default). A composable reads
`val tx = LocalTx.current`; a plain function takes a parameter that is always named `tx: Tx`. The choice
is the `lang` string in the `capture` prefs file (`AppPrefs.lang`, flipped by `AppPrefs.flip(p, "lang")`),
read only under `LangRow.VISIBLE` (on since 28 Oct 2026), so it is English on first run whatever the
phone's system language, and मराठी once picked in the drawer.
`app/build.gradle.kts` keeps only `en` library resources (`resourceConfigurations`), so Compose's and
material3's own strings are English by decision rather than the system language's.

The transient messages (slice 1c) hold a `Msg` (`pano/.../i18n/Msg.kt`, `(Tx) -> String`), not a String,
so a message raised before the language changes is drawn in the new one: `lastResult = { tx ->
tx.app_msg_renamed_x(newName) }`, or `lastResult = { _ -> "Could not rename: ${it.message}" }` for one
still English, drawn with `Text(it(tx))`. Name the lambda's parameter, always (a check below refuses an
implicit one): with an implicit `it`, an `it.message` inside would mean the Tx. A literal in such a lambda keeps the assignment's UI position, so
(a) judges it. Any value computed from state that may change (a text field's `typed`) is read into a `val`
BEFORE the lambda, or the banner shows what the field says when it is drawn. Counts use the generated
`tx.pl_<noun>(n)` ("3 rooms"); the old `plural()` helper is gone. Where the table has a WHOLE template for a
site, call it rather than a count glued to a fragment: `tx.app_x_shot(n)`, never `tx.pl_room(n) + tx.app_shot`,
which the English cannot tell apart but which keeps the English word order in Marathi ("3 खोल्या फोटो" where
the prototype draws "फोटो झाले: 3 खोल्या"). A fragment may stand alone as a label (design §9).

## What the checks refuse (`gradle :pano:test` fails)

Each check puts all of its offenders in one assertion message. On CI each check also writes one
`::error` annotation that names up to 25 of them.

- **The table** (`I18nTableTest`):
  - the two files differ in their keys or in key order;
  - a key the phone can call has no Marathi (`sp.the` is the one key allowed to be empty), contains `⟦`,
    uses a Devanagari digit, or contains markup;
  - a Marathi value has a placeholder the English lacks, or drops one that is not a plural suffix;
  - the refused templates differ from the pinned 14 (that list may only shrink);
  - two keys give the same accessor;
  - the committed generated files differ from what GenTx writes. The fix is `gradle :pano:i18nGen`;
  - (9) a fragment key that a whole template at the same prototype site contains (`app.shot` inside
    `app.x_shot`, both v53 app.js:460) glued into a sentence in any scanned module -- `+` or `+=` on
    either side, or inside a string. Call the whole template; the fragment may still stand alone;
  - (10) a `phone-text` key whose `where` is not a list naming the phone source in every entry, or a
    `where` naming the phone on a key without the flag (the v61c card, L2).
- **The public repository**:
  - a currency figure (`₹`, `Rs`, `INR` before a digit) anywhere in either file, notes included;
  - anything matching `deny-patterns.txt`;
  - when one is present, a name from the client-name list. On CI that list is the `I18N_DENY_NAMES`
    secret; locally it is `-Pi18n.deny=<file>` (a relative path is resolved against `android/pano`). It
    is never committed. Without it the step stays green on the structural patterns and prints a
    `::warning` that names were NOT checked: a fork or a run without secrets is protected against
    figures, emails and phone numbers, NOT against names. **The secret is not set yet (27 Oct 2026), so
    CI checks no client names today.** A list that was asked for and cannot be used -- a
    `-Pi18n.deny` path that is not a readable file, or a list with no usable name -- fails the check;
    a usable one prints a `::notice` with its count.

  Keys are checked as well as values: a key is built from the English and never renamed. These failures
  name the key and the pattern, never the matched text, because CI logs are public; when a KEY matched,
  only the entry's position is named.
- **Rendering** (`TxRenderTest`):
  - a Latin word in a Marathi rendering that is not in `latin-ok.txt`, or an entry in that list that
    nothing uses any more;
  - a `{placeholder}` left in any rendering;
  - a separator printed twice, with each optional tail filled from the fragment key at its prototype
    location;
  - the plural and placeholder-fill rules break, or `Tx.of()` stops treating everything except exactly
    `"mr"` as English;
  - a split template's case (the v61c card's group 2) stops rendering as its whole sentence, in English
    or in Marathi: each one is pinned with a literal.
- **Identity values** (`MastersTest`):
  - a stored identity value (a stage, work, state, room, site type, mark type, surface, service) that
    does not show its own English bytes under English, or has no Marathi name. The pass-through list
    (CCTV, Nos, Top/Bottom, the plumbing description) is the exception;
  - a wire, storage or export builder that takes a `Tx`, a `Msg` or any i18n type, or (for the whole
    export objects) whose compiled code names the i18n package at all.
- **The sources** (`I18nLiteralTest`, over `app/`, `camera/` and `pano/` main):
  - (a) a shown Latin literal that is not keyed, not listed in `unkeyed.tsv`, and not marked
    `// text:value <why>`;
  - (b) a per-file count of prose literals that differs from `literal-counts.tsv`. A literal listed in
    `unkeyed.tsv` or marked `// text:value <why>` is not counted, so either remedy lowers the count;
  - (c) an `unkeyed.tsv` row whose text is gone from its file;
  - (d) an `unkeyed.tsv` row for a file listed in `ported.txt`;
  - (e) a `.format(`, `String.format(`, `ofPattern(` or `SimpleDateFormat(` that names no FIXED Locale
    (`Locale.ROOT`, `US` or `ENGLISH`; `Locale.getDefault()` is the system language again). There are
    no exceptions: the 7 `:pano` sites took `Locale.ROOT` in slice 1a, the 8 `:app` ones in 1c;
  - (f) a `tx.<name>` or `Masters.<name>` in `:app` or `:camera` that does not exist, has the wrong
    number of arguments, or passes two or more arguments without naming each. This stands in for the
    compiler of those two modules, which this container lacks;
  - (g) any read of the system language, in any module;
  - (h) `Tx` in `PlanExport.kt`, `FaceWriter.kt` or the upload worker;
  - (i) a `Name.member` of a `:pano` object (`SurveyPrepEdit.linesRefusal`, `StepRoute.TAB_ROWS`, `SurveyPrep.Measure`)
    read from `:app` or `:camera` that the object does not have -- a function, a property's getter, a constant or a nested
    class, by reflection: the other half of the compiler stand-in, added by the M3a review after a reader of a deleted
    constant passed (f);
  - a lambda whose parameter is typed `Tx` and named other than `tx` or `_` (`{ t: Tx -> … }`): (f) scans `tx.<name>`
    alone, so its reads would go unresolved (M3a review);
  - a lambda assigned to `lastResult`, `busy` or `note` that does not name its parameter (`{ tx -> … }`,
    `{ _ -> … }`): its `it` would be the Tx, a break only `:app`'s compiler would find;
  - the language row turned on early;
  - the भाषा · Language row drawn other than as exactly
    `if (LangRow.VISIBLE) DrawerLine("भाषा · Language", …) else null`, a `Tx.of(` in `:app` or `:camera`
    other than exactly `Tx.of(if (LangRow.VISIBLE) <the stored choice> else null)`, or a `Tx.MR` named
    there: the row and the ability to pick मराठी switch on together, by that one constant. A mention of
    the constant is not a gate, so an inverted (`if (LangRow.VISIBLE) null else …`), negated or
    commented-out gate is refused too.
- **The tests themselves** (`I18nTestHygieneTest`): an `assertEquals(Tx.…)`; a test that reads the
  tables or the model built from them (`I18n.model`, `byKey`) other than `I18nTableTest` (properties
  only) and `TxRenderTest` (accessor names and types only); or `TxRenderTest` reading an entry's text.
  Comparing the table with itself proves nothing.

## Rules that do not change

- **Storage never sees Marathi.** Identity values (rooms, stages, services, works, mark types, surfaces)
  stay English constants on the wire, in the preps and in stored notes. They are shown through
  `Masters`, and a typed name is shown as typed. PDFs and exported images stay English (decision 4 of
  21 Oct). Digits stay Latin, and months stay Jan–Dec (decision 3 of 21 Oct).
- **No text is invented on the phone.** A text with no key stays English and listed in `unkeyed.tsv`
  until a reviewed commit here keys it: the v61c card's 194 (L2), or, for a screen the prototype never
  had, the Marathi Claude writes in the slice that keys it (decision 6).
- **This repository is public.** The tables hold no client names, phone numbers, emails, rates or vendor
  names, and the checks above refuse any.
