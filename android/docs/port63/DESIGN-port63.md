# DESIGN-port63 — measure on the photo, and the survey → quote journey

S2 APK (`mallet_estimator` → `android/`), with S5 QUOTE's part of the journey named. Written 16 Nov 2026 (IST),
branch `port63-measure`.

**Basis.** Code read at `775d33c` (0.3.332 = port 62 build 3) and slice SP0 on `port63-sp0` at `2fec4fe`; four
readers' reports (the screen today, ImageMeter's manual, the journey walked in the code, two candidate designs).
Nothing was run for this file, no gradle, nothing seen on a phone. Every "decided by Claude" below is judged by Amit
on the phone, not on a prototype: the prototype is on hold.

Paths: `pano/…` = `android/pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/…`, `app/…` =
`android/app/src/main/kotlin/com/malletcrafts/interiorestimate/…`, `SPS` = `app/SurveyPrepScreen.kt`,
`SPD` = `app/SurveyPrepDraw.kt`.

## 0. What Amit asked for

15 Nov 2026, after his first use of the app on a client site:

> "Having both measurements foto and measured and both is very confusing while using app. The laundry list of
> measurements shown below screen is also frustrating. Image meter app which is reference have very elegant
> annotation ui and end user experience. But do not drop our industry standard mark items like door window
> electrical plate etc. imagemeter shows all dimensions on the annotation there itself which is easy to use.
> Lets focus on survey to quote approval workflow first. Site progress will do post I confirm that survey to quote
> looks good. Do not delete any site progress code but toggle it as feature flag."

Then: develop straight on the phone, no prototype; hold the help tours, and switch the survey tour off (code kept)
in the same build that ships the new measuring screen. 16 Nov: "Survey to Quote journey is priority. Let amit
verify it end to end in English then only start Marathi transformation."

So this design has five rules:

1. **One figure per dimension, on the annotation.** Never the photo's figure beside the reading.
2. **No list under the photo.**
3. **Every mark type stays**, with its code, kinds and colour.
4. **Survey → quote approval first.** Site progress is behind `Features.SITE_PROGRESS` (slice SP0, built).
5. **English first.** New texts are keyed in English; their Marathi waits in `android/i18n/mr-pending.tsv`.

## 1. Measure on the photo — what the phone will show

### 1.1 One figure per dimension

A dimension shows one figure, on its own dimension line. The text comes from `SurveyPlan.figure`
(`pano/SurveyPlan.kt:173`), the rule the Plan tab already draws by, so the photo, the plan and (later) the saved
pictures cannot disagree.

| The dimension is | Figure | Plate |
|---|---|---|
| Measured (laser or typed) | `1198` | white bold on solid black (`0x000000`, alpha `0xdd`) |
| Not measured, the photo can estimate it | `≈1203` | grey (`0xcfd3cb`) on half-black (alpha `0x88`), dashed grey outline |
| Not measured, the photo cannot show it (`p`: reveal depth, projection, drop…) | `?` | amber (`0xffc46b`), dashed amber outline |
| The one being measured now | `▶` before the figure | gold plate `0x4a3d0c` with the gold ring `0xe0b040`, as today |

**Decided by Claude: an unmeasured dimension shows the photo's estimate, not a blank.** The face is already squared
to the laser's W × H, so the estimate is what ImageMeter calls a calculated value. It tells him which line is
which, a wildly wrong estimate shows a misplaced box before he shoots, and a dimension he misses still leaves the
office a figure that is visibly an estimate. A blank cannot tell "not measured yet" from "nothing to measure here".

What goes: the Show row (≈ Photo / Measured / Both), the ` | ` divider, the `±d` difference and its green and
amber. Whether a reading was lasered or typed is not stored and not shown.

**One check stays, as a mark, never a number.** A measured plate carries a small amber `!` after its figure when the
reading differs from the photo by more than the larger of 150 mm and 15 % of the estimate. The bar (§1.4) then says
why. It is computed from the reading and the box as it is now, so it also catches a mark moved after it was
lasered; nothing is stored. The threshold is deliberately wide: it should fire on a shot at the wrong target, not
on an ordinary estimate. It is Claude's starting figure, to be tuned from his first rooms.

After a reading the phone no longer shows the photo's estimate anywhere, except inside that check's sentence. The
office still sees both on S5 `/projects`.

### 1.2 What is drawn for each mark

There is no table of drawings per type. A mark draws whatever `SurveyPrep.measures(s, e, a, b)` returns for it
(`pano/SurveyPrep.kt:913`, the ten new types through `MarkTypes.measures`), by its key. So no type, kind or later
addition can be left out, and no key is invented or renamed.

| Key | Drawn as |
|---|---|
| `w` | a line 6 u above the top edge, corner to corner; plate mid-span |
| `h` | a line 6 u right of the right edge; plate mid-span, turned to read bottom to top |
| `l` / `r`, a point's `cl` / `cr` | a line from that corner of the wall (plan: that wall) to the mark's near edge, a point's to its centre |
| `s` / `t`, `k` / `f`, a point's `cs` `ct` `ck` `cf` | a vertical line from the floor or ceiling (plan: back or front wall) to the mark's near edge, a point's to its centre |
| `d` (Beam) | a vertical line from its underside up to the ceiling |
| `p` (site only) | no line: a chip `⊥ ?` at the mark's lower-right corner, `⊥ 230` once read |

Lines are white (`0xffffff`, alpha `0xcc`), 1 u, with a 4 u arrowhead at each end. They run where 0.3.332 draws
them (`SPD:798-832`): the corner line 2.6 × the text size under mid-height, the floor or ceiling line 1.6 × left of
centre, both through the centre for a point. A plate breaks its line.

Examples, from the same rule:

- **Door** (`w h l|r p`): width above, height on the right, one line from the nearer corner, `⊥ ?` for reveal
  depth. No floor line: it touches the floor and `measures()` already drops that.
- **Window** (`w h l|r s p`): the same, plus the sill line up from the floor.
- **Switch plate** (`w h l|r s`): its box is about 25 × 11 dp on a 2.7 m wall, so width and height share one
  compact plate beside it, `≈200 × ≈90` (§1.6); corner line; floor line.
- **Electric point**, and every 50 mm point (P, F, PLB, LV): the spot, and two lines to its centre. No width or height.
- **Carpentry unit, Grill, Sanitary ware**: width, height, corner, underside from the floor, `⊥` depth.
  **Curtain track**: its length as `w`, corner, from the ceiling, `⊥`. **Beam**: `d`, corner, `⊥`.
- **Note**: its pin; nothing to measure. **Tape**: unchanged; it has no reading.
- **Line, area, chain**: already one figure. One fix: a Line's tag shows its reading once read, else `≈`
  (`F-M1 1240 mm` / `F-M1 ≈1234 mm`, as S5 already draws it).

A mark that is **not in the room** (born a Client wish or Vendor added; `SheetMarks.surveyed` is false) keeps its
state outline and pill, shows estimate-style figures, and is never asked for a reading: its plates take no tap and
it is not in the walk or the counts. This is S5's rule for the same mark. A mark that was surveyed keeps its
readings whatever its state.

The wall's own W × H stays in the caption above the photo ("Front wall · W 2678 × H 2677").

### 1.3 The steps, and where measuring happens

The strip keeps four steps. The third changes from the list to the photo:

`1 · Lines` · `2 · Mark` · `3 · Measure` · `4 · 360`

- **2 · Mark** is for placing and moving. The picked mark shows all its dimensions in the new one-figure style.
  Placing or picking a mark makes its first unmeasured dimension active, as today, and a tap on a figure makes that
  one active; the measure bar then appears above the palette. So "place a door, shoot its width" still works
  without leaving the step (ImageMeter's "select, then shoot"). After a reading the next unmeasured dimension of
  the same mark becomes active, as today.
- **3 · Measure** is the same squared photo with the figures on it and a walk through the unmeasured ones. Nothing
  can be moved, resized or placed there, so a thumb on the photo cannot drag a mark while the other hand holds the
  laser. A drag pans; a pinch zooms.
- **4 · 360** keeps the same bar (§1.7).

Why a third step and not everything in 2 · Mark: at Estimated and at Client approved there is no 2 · Mark (port 62
slice L4, shipped), and survey prep needs a step to land on that places nothing. The third step is that, as the
list was.

**The list leaves the screen, and its code is deleted** (decided by Claude): `SpTable`, the `3 · List` pane, the
LIST tab, the list's squared pictures and the Show row. Amit asked for site progress and the tour to be kept
behind a switch; he did not ask it of the list, which is the thing he called frustrating. A switch here would make
every step, tab and phase rule two-sided and keep the both-figures painter alive. Git keeps the code.

What the list did, and where each part goes:

| The list had | Now |
|---|---|
| Every reading box | the plate on the photo, with the bar, the pad and the laser |
| A mark's code opening the Mark sheet | a tap on the code in 3 · Measure opens it, as in 2 · Mark. This matters at Estimated and Client approved, where there is no 2 · Mark |
| Delete / Drop / Restore | already on the picked mark's row in 2 · Mark |
| "… is outside the face" | the red dashed outline on the photo, as today |
| All six surfaces at once (LIST tab) | a count on each face tab, and the walk's "next face" (§1.5) |

The tabs become FRONT RIGHT BACK LEFT / FLOOR CEILING PLAN.

### 1.4 The bar, the pad, the laser

```
┌──────────────────────────────────────┐
│ Survey … (the phase band)            │
│ ←  Living · survey prep   ↶ ⋮ ✓ Save │
│ FRONT ✓  RIGHT ✓ 2  BACK   LEFT      │
│ FLOOR    CEILING    PLAN             │
│ 1·Lines  2·Mark  3·Measure  4·360    │
│ Front wall · W 2678 × H 2677         │
│ ┌──────────────────────────────────┐ │
│ │        ├── ≈1203 ──┤        fit  │ │
│ │       ┌───────────┐         grid │ │
│ │       │FW1        │ 1198         │ │
│ │       └───────────┘              │ │
│ │  ├─▶≈640─┤  ⊥ ?                  │ │
│ └──────────────────────────────────┘ │
│ ‹  FW1 Window · from left corner     │
│    7 of 18 measured     [≈640] D2  › │
└──────────────────────────────────────┘
```

**The measure bar** (56 dp in portrait, 48 dp in landscape), one composable used in steps 2, 3 and 4:

`‹` · two lines of text · the figure box · `D2` · `›`

- Line 1 names the dimension: `FW1 Window · from left corner` (code, type name, the measure's own words).
- Line 2 is progress, `7 of 18 measured`; or, on a checked figure, "The photo says ≈640 mm. Check the mark, or
  shoot again."
- The figure box shows the same one figure as the plate, in the same style. A tap opens the pad.
- `D2` fires the laser, as today's button does (`fire()`, `SPS:505`). The meter's own button works the same.
- `‹` goes to the dimension before, read or not, to correct it. `›` skips: the dimension stays unmeasured and comes
  round again.

In 2 · Mark the bar shows only while a dimension is active, where the reading strip is today. In 3 · Measure it
takes the palette's place and is always there.

**The pad** is the app's own, not the system keyboard, which covers far more of the screen, jumps at every field
and has no Next. Four rows of 44 dp (36 dp in landscape):

```
 1   2   3   ⌫
 4   5   6   Clear
 7   8   9   Next ▸
 ⌄   0       (Next ▸, two rows tall)
```

- Digits only, six at most, whole mm. The digits show live in the bar's box and on the gold plate.
- `Next ▸` commits and moves to the next unmeasured dimension. Leaving the dimension any other way (a tap on
  another plate, `‹`, `›`) commits what was typed too, as losing focus does today.
- `Clear` empties the figure; committing an empty figure removes the reading and the estimate returns.
- `⌄` hides the pad. Open or hidden is remembered for the session, so a laser user never sees it.
- The pad sits under the bar and the photo gives up the height; it never covers the photo. In landscape bar and
  pad sit in the 318 dp right column.
- Every write goes through `MmBox.commit`, unchanged: only a typed or lasered value becomes a reading.

**The laser** is unchanged in its rules. A reading lands on the active dimension, decided when it arrives
(`SurveyRef.route`, `SPS:522-533`); a reference box's shot never becomes a mark's reading. New in M2:

- In 3 · Measure, when the mark has nothing left, the walk moves to the next unmeasured dimension on the face, with
  no tap on the phone. The photo pans only if that plate is off screen.
- A short haptic tick when a reading lands, because his eyes are on the laser dot.
- The plate turns from grey `≈` to white with the figure.

**Corrections.** Tap the plate (or `‹`), then shoot or type again: the new value overwrites. `Clear` then
`Next ▸` removes a reading. ↶ still never undoes a reading.

### 1.5 Progress and the next unmeasured

- **In the bar:** `7 of 18 measured` for the open face.
- **On each face tab:** a small amber count of what is still to measure there (`RIGHT ✓ 2`). No count when nothing
  is left or nothing is asked. The existing ✓ keeps its one meaning, "four lines set".
- **On a mark:** `✓` after its code once everything it asks is measured.
- **A finished face:** the bar reads "Front wall · all 9 measured ✓" and offers "Next: Right wall · 4 to measure ▸".
  It never changes face by itself: he has to turn round. When no face has anything left: "Room measured · all 31 ✓".
- **Entering 3 · Measure**, or changing face inside it, makes the first unmeasured dimension active, so he can
  shoot at once.

**The walk's order** (one rule for all six surfaces): marks left to right across the photo, ties lower first, then
by code; inside a mark the order `measures()` gives; then Lines and areas in stored order; then each chain's
`SurveyChain.openKeys` (its total left out where the laser gives it). Notes, tapes and marks not in the room are
left out. Faces follow the tabs: front, right, back, left, floor, ceiling.

Progress never gates ✓ Save.

### 1.6 A busy wall at 360 × 780 dp

The example wall (a door, a window, a switch plate, three electric points) asks for 19 figures. The photo is about
340 dp square. All 19 with their lines would put six lines across the wall and five up it.

So, in 3 · Measure:

- **Every mark shows its size**: `w` on its top edge and `h` on its right edge, 10 sp.
- **Only the active mark shows its position** dimensions (corner, floor or ceiling, drop, the `⊥` chip), with their
  lines, at 12 sp. The other marks draw at 75 % and the rest of the photo dims 20 %, as today.
- **A small box** (both spans shorter than their plates plus 8 dp) gets one compact plate beside it,
  `≈200 × ≈90`, each figure in its own style and its own tap half.
- **A point** not being measured shows its spot, its code and its `✓`.

That is 6 to 9 plates on the example wall, not 19. In 2 · Mark only the picked mark shows figures, as today.

Plates keep a constant size on screen, so zooming in spreads them apart. When two would overlap, the placing rule
(pure, in `pano/DimLayout.kt`, tested here) is, in this order:

1. Place in rank order: the active dimension, the active mark's others, unmeasured, measured.
2. A plate sits mid-span on its line. A span too short for it: the plate steps off the line by one plate height on
   a short leader, arrowheads outside pointing in.
3. If it would cover a placed plate or a mark's code: try ¼ and ¾ along the line, then step off it on a leader.
4. Still no room: it shrinks to a 6 dp dot on its line (hollow = not measured, solid = measured) with a 32 dp tap
   box. The active dimension is placed first and never shrinks.
5. Every plate stays inside the photo.

An `all` button beside fit and grid, drawing every figure of every mark for a review, comes in M3 with the same
rule.

### 1.7 The 360 and the Plan

- **4 · 360:** no dimension lines on the sphere. The bar is the same one, so a picked mark can be shot or typed
  there, and the figure is in the bar. M4 turns the view to the active mark and rings it in gold.
- **PLAN tab:** unchanged. It already shows one figure per dimension.

### 1.8 Phases, wishes, locked marks

- **Readings follow the phase from M2** (the readings part of port 62's L10): `Tool.READINGS` joins
  `PhaseTools.PREP_HONOURS`. At Survey, and while the stage is unread, everything above applies. After Survey
  3 · Measure, and the picked mark in 2 · Mark, show the same plates as figures: no bar, no pad, no counts, and a
  tap on a plate (or a laser reading arriving) says the existing `refuse.readonly.readings`, "Readings are taken in
  the Survey phase — move the phase back on the project screen to key one".
- **A locked mark is still measured.** A lock stops moving, not reading.
- **Read-only marks** keep L4's rule: their controls are hidden, never greyed.

### 1.9 What does not change

- Reading keys are `<code>.<measure>` exactly as `measures()`, `SurveyMeas.measures` and `SurveyChain.measures`
  give them; S5's `KEY_RE`; a point's c-keys. `prep.actual`, `ReadingsSync`, the record shape, `SurveyPrepFile` and
  `KeyGrammarTest` are untouched. Nothing on the wire changes.
- Which measure exists is decided by geometry: nearer side, the window's sill, zero offsets dropped.
- One model, two views: 2 · Mark and 4 · 360 edit through the same dialog functions.
- The single call sites pinned by source-reading tests stay single (`PhaseTools.place`, `MarkSheet.apply` / `open`,
  `SurveyExport.sheet`, `SurveyPrepEdit.duplicate(`, `drawMarks` with a named audience). A pin whose count changes
  because the list is gone moves to the new count with the reason in the test; none is loosened.
- Hit order: tabs, then plates, then marks (`SurveyPrepTouch.hit`).
- Nothing scrolls. The photo takes every dp the controls leave.
- The saved `-survey.jpg` gallery pictures are **not changed by M2** (they keep today's painter path for mode
  `sheet`) until Amit answers question 1; M3 moves them.

## 2. Where survey → quote ends, site progress, the tours

- **The journey ends at Client approved, with its scope**: the Project Manager's "Approve the estimate ▸", the
  sixteen-work scope sheet, and the approved quote view with its APPROVED SCOPE row. Approval is final.
- **Everything after it is site progress**, hidden by `Features.SITE_PROGRESS = false` in `pano/Features.kt`
  (slice SP0, built on `port63-sp0`, `3f65ad1` and its review `2fec4fe`): the album, the execution line, filing by
  work and the workers that serve them. Nothing is deleted. It is not redesigned here.
- **The survey tour is switched off in slice M2**: `Features.HELP_TOURS = false`, code kept. With it off
  `TourFlows.shipped` gives nothing and the drawer's Help row is not drawn (it would read "0 tours"). Decided by
  Claude: a fresh install is not seeded with the Demo Client either (`TourSeed.ensure`, `app/MainActivity.kt:588`),
  since it exists only for the tours; a Demo Client already on a phone is left alone. The four tour anchors in
  survey prep, `TourBindSurvey` and `TourOverlay` stay. The tour's step 11 teaches the list and its `mode:list`
  anchor has no step now; both are for whoever rewrites the tour after Amit approves the journey. No tour work
  (port 62's P1 and TR) happens before that.

## 3. English first — the new texts

Every shown text is keyed in `android/i18n` and `Tx` is regenerated (`gradle :pano:i18nGen --offline`, then the
ratchet); `unkeyed.tsv` stays empty. For a new key the Marathi value is its English for now, and the key is listed
in **`android/i18n/mr-pending.tsv`** (new; `key<TAB>English`, under a `#` header saying these wait for Amit's
end-to-end verification of the journey in English). No existing Marathi value is removed or changed: the diff of
`strings-mr.json` is additions only, and the commit says so.

The table tests keep their strength:

- `TxRenderTest`'s two Latin scans skip the keys in `mr-pending.tsv`, and only those.
- A new `MrPendingTest` holds the list honest: every listed key is in both tables, its Marathi is byte-equal to its
  English and to the file's second column, and no key is listed twice. A key leaves the list in the commit that
  writes its Marathi, and from then on the scans read it.

**New keys, all added in M1** (flag `phone-text`):

| Key | English | Where |
|---|---|---|
| `sp.measure.x_of_x` | `{read} of {total} measured` | the bar's second line |
| `sp.measure.face_done_x` | `{face} · all {total} measured ✓` | the bar on a finished face |
| `sp.measure.next_face_x` | `Next: {face} · {n} to measure ▸` | the bar's button on a finished face |
| `sp.measure.room_done_x` | `Room measured · all {total} ✓` | the bar when no face has anything left |
| `sp.measure.photo_says_x` | `The photo says ≈{est} mm. Check the mark, or shoot again.` | the bar's second line on a checked figure |
| `sp.measure.not_in_room` | `Not in the room — a mark placed as a wish has nothing to measure` | said on a tap on such a mark's figure |
| `sp.measure.x_to_measure` | `{n} to measure` | a face tab's count, read out |
| `sp.measure.aria.previous` | `The measurement before` | `‹` |
| `sp.measure.aria.skip` | `Skip to the next measurement` | `›` |
| `sp.measure.aria.type` | `Type the figure` | the bar's figure box |
| `sp.reference_shown_measure` | `Reference shown on 2 · Mark, 3 · Measure and 4 · 360` | `pano/SurveyRef.kt:68`, in place of `sp.reference_shown_2_mark` |

**Existing keys reused, Marathi untouched:** `common.measure` (the step's name, "Measure"), `common.next` (the
pad's Next, with the glyph ` ▸`), `fp.clear` (the pad's Clear), `fp.nothing_measure_face_yet` (a face with nothing
asked), `refuse.readonly.readings`, `refuse.sp.tap_measurement_first`, `sp.msg.d2_x_tap_measurement`, the measure
names (`measure.*`) and the type names.

**Keys that lose their reader and stay in the tables:** `common.show`, `common.photo`, `common.both`,
`sp.reference_shown_2_mark`, and `common.list` if nothing else reads it. If a table test refuses a key with no
reader, the builder says so and follows that test's own rule; the Marathi value is not deleted.

**Glyphs**, each a `// text:value` literal, byte-equal in both languages: `≈ ? ▶ ⊥ ✓ ! × ‹ › ⌫ ⌄ ▸`.

Texts of the journey slices (§5) are listed in each slice's own commit, by the same rule.

## 4. The slices

M1 and M2 ship together, with SP0, as one Play build: the tour must go off in the build that changes the screen,
and M1 alone shows nothing. `port63-measure` takes `port63-sp0` (`2fec4fe`) before M2 starts, because M2 needs
`pano/Features.kt`; M1 does not.

How each is built: `:pano` is run here (`nice -n 15 gradle … --offline` from `android/`); `:app` is compiled only
by CI. Commits are local, of the changed paths only, and each message gives the `:pano` totals and says that
`:app` was not compiled here and nothing was seen on a phone.

| id | Slice | Piece | Size | Needs | What Amit sees |
|---|---|---|---|---|---|
| SP0 | Site progress behind `Features.SITE_PROGRESS` | S2 APK | built | — | no Album tile; after approval the line reads "Quote · Client approved" |
| M1 | The measuring models, pure, with tests; the new keys and `mr-pending.tsv` | S2 APK | M | — | nothing on screen |
| M2 | The measuring screen; the list gone; readings by phase; the tour off | S2 APK | M–L | M1, SP0 | one figure on each dimension, the bar, the pad, the laser walking on by itself; no list; no Help row |
| M3 | Every figure at once (`all`); chain and Line figures take a tap; the saved pictures by his answer | S2 APK | S–M | M2; question 1 | an `all` button; the gallery pictures with one figure per dimension |
| M4 | The 360 turns to the dimension being measured | S2 APK | S | M2 | on 4 · 360 the view turns to the mark and rings it in gold |

### M1 · the pure models (`:pano` only, M)

Run and tested in this container; totals are read from the XML under `pano/build/test-results/test`.

**`pano/MarkDims.kt`** — which dimensions a mark has, where each line runs, and each figure's state.

- `of(s, e, a, b): List<Dim>` — one `Dim` per `SurveyPrep.measures(s, e, a, b)`, same order. A `Dim` has `key`
  (`"w"`), `code` (`"FD1.w"`), `role` (`WIDTH`, `HEIGHT`, `OFF_H`, `OFF_V`, `DROP`, `DEPTH`), `what`, `est`, and its
  line on the face in mm (x from the left edge, y up from the bottom edge): `w` (x0, y1)→(x1, y1); `h`
  (x1, y0)→(x1, y1); `l` (0, ym)→(x0, ym) and `r` (x1, ym)→(a, ym), a point's to its centre; `s` / `k`
  (xm, 0)→(xm, y0) and `t` / `f` (xm, y1)→(xm, b), a point's to its centre; `d` (xm, y0)→(xm, b); `p` no line, its
  anchor at (x1, y0). A key it does not know is drawn as a `DEPTH` chip, never dropped; a test proves no key of
  today's types reaches that case, so a new key shows up as a red test first.
- `asked(e) = SheetMarks.surveyed(e)` — only a mark in the room takes a reading.
- `value(actual, code, est): Value` — `text` from `SurveyPlan.figure` (the one rule), `state` (`MEASURED`,
  `ESTIMATE`, `UNKNOWN`), `check`.
- `check(read, est)` — true when both exist and `|read − est| > max(CHECK_MM, CHECK_PART × est)`;
  `CHECK_MM = 150.0`, `CHECK_PART = 0.15`.
- `done(prep, s, e, a, b)` — every key the mark asks has a reading (the `✓` after its code).

**`pano/DimLayout.kt`** — plates in px: placing and collisions (§1.6). In: for each figure its code, role, line in
px, plate size, rank; the canvas size; boxes to keep clear (the marks' codes); `u` and `dp`. Out, per code: the
drawn box, the tap box, turned or not, a leader or none, shrunk to a dot or not; and for a small box the one
compact plate with its two tap halves. `Scope`: `PICKED` (2 · Mark: the picked mark, everything), `FACE`
(3 · Measure: every asked mark's size, the active mark's positions), `ALL` (M3). It takes over the constants
`SPD.dims` holds today and uses `SurveyPrepTouch.Box` and `Tag`.

**`pano/MeasureWalk.kt`** — the order and the counts (§1.5).

- `stops(prep, s, a, b)` — every reading key the face asks, in walk order.
- `next(prep, s, a, b, after)` — the next one with no reading, wrapping; null when none.
- `previous(prep, s, a, b, before)` — the stop before, read or not.
- `count(prep, s, a, b): Count(read, total)`; `room(prep, ab)`; `nextFace(prep, from, ab)` — the next face in tab
  order with something left, and how many.

**`pano/MeasurePad.kt`** — `press(text, key)`: a digit appends (six at most, no leading zero), `⌫` removes one,
Clear empties. The write is `MmBox.commit`, unchanged.

**`pano/MeasureText.kt`** — the bar's lines through `Tx`: `label` ("FW1 Window · from left corner"; a Line
"F-M1 · length"; a chain "FK1 · segment 2"), `progress`, `faceDone`, `nextFace`, `roomDone`, `photoSays`.

**`pano/SurveyMeas.kt`** — `lineTag` takes the Line's reading: `F-M1 1240 mm` once read, else `F-M1 ≈1234 mm`.
The saved pictures keep passing no reading until M3.

**Tests.**

- `MarkDimsTest`: for every type and sub-kind on every kind of surface, over a grid of boxes, the codes equal
  `Prep.keysOf` in order; every line's length equals its `est`; a point's lines end at its centre; a mark on the
  floor has no `s`; crossing the midline swaps `l` and `r`; no key falls to the unknown case; the figure equals
  `SurveyPlan.figure` for the same input; the check at 149 and 151 mm on a 900 span and at 15 % on a 2678 span,
  never without a reading or an estimate; a mark born a wish is not asked.
- `DimLayoutTest`: home position; a turned plate; the small-box compact plate; the ¼ and ¾ slide; the leader; the
  dot; the active plate never shrinks; no two placed plates overlap; every plate inside the canvas; tap boxes at
  least 32 dp; the same input gives the same output; the example wall at 340 × 340 in `FACE` scope with the door
  active has no dot.
- `MeasureWalkTest`: the order; Notes, tapes and marks not in the room left out; a chain's total left out where
  the laser gives it; `next` wraps and skips what is read; `previous` includes what is read; the counts; the faces'
  order; null after the last reading.
- `MeasurePadTest`, `MeasureTextTest` (English), `MeasTest` (the Line's tag), `i18n/MrPendingTest`, and
  `TxRenderTest`'s two scans reading the pending list.

### M2 · the screen (M–L)

`:app` compiles only in CI, so every import, accessor and caller is exact; the rules of the screen are asked of
`:pano`, never written again in `:app`.

1. **Painter** (`SPD`): for the screen, `dims` draws from `MarkDims.of` and `DimLayout`; a new `plate` draws one
   figure in its state's style (§1.1), turned on a vertical line, with the `!`, the `⊥` chip and the `✓` after a
   code. `Marks` gains the scope (`DimLayout.Scope`). Every plate is added to `o.tags`, so the hit order holds.
   Mode `sheet` (the saved pictures) keeps today's `dims` / `vtag` path untouched.
2. **`app/SurveyMeasure.kt`** (new): `SpMeasureBar` and `SpMeasurePad`.
3. **`pano/StepRoute.kt`**: `STEPS = lines, mark, measure, 360`; the step's name is `tx.common_measure`;
   `TAB_ROWS` loses `list`; `listView` and the list's `budgetDp` cases go. **`pano/PhaseTools.kt`**:
   `toolOf("measure") = Tool.LIST` (the prototype's row keeps its name; its KDoc says the phone's third step is
   3 · Measure since port 63); `modeFor` and `prepMode` fall to `measure`; `PREP_HONOURS` gains `READINGS`.
4. **`SPS`**: the Measure pane is the photo pane with no drag, resize or placing. A tap on a plate makes it active;
   on the active plate, or the bar's box, opens the pad; on a mark's code opens the Mark sheet through the one
   existing call; elsewhere on a mark jumps the walk to its first unmeasured dimension. `reading()` asks
   `prep.nextAfter(code)` and then, in 3 · Measure, `MeasureWalk.next`. The reading strip becomes the bar. The Show
   row, `listPane`, `surfaceBlock`, `SpTable`, `SpMmField` (if nothing else uses it) and every `listView` branch
   are deleted. The face tabs draw their count. After Survey: `PhaseTools.readingRefusal` is said on a plate's tap
   and on a laser reading, and nothing becomes active. A face that is not bounded shows in 3 · Measure what
   2 · Mark shows there today.
5. **The tour off**: `Features.HELP_TOURS = false`; `TourFlows.shipped(tx, tours = Features.HELP_TOURS)`; the
   drawer's Help row and `TourSeed.ensure` behind the same constant (`app/MainActivity.kt:1600`, `:3645`, `:588`).
6. **`pano/SurveyRef.kt:68`** reads `sp.reference_shown_measure`.

**Tests** (in `:pano`; `:app` is read as source): `StepRouteTest` (the steps, the labels, the tabs);
`PhaseToolsTest` (`PREP_HONOURS`, the refusal by stage, `prepMode` landing on `measure`, the strip at every stage);
`TourFlowsTest` (nothing shipped with the tours off, the survey flow with them on); `FeaturesTest` (the constant,
and every reader of it by file and count); and source pins: no `SpTable`, no Show row and no `"both"` in `SPS`;
`MeasureWalk.next(` once; `readingRefusal` called; `MmBox.commit` the only way a figure is written; the four tour
anchors and `TourBindSurvey` still there; the moved counts of §1.9.

If M2 proves too large for one review, its cut is: (a) items 1–4 without the phase rule, (b) the readings' phase
rule and the tour off. Both still land in the same build.

**What the build note tells Amit:** measuring is on the photo now; the list is gone; after Survey a figure cannot
be changed until the phase is moved back; Help's tours are off for now; the gallery pictures are unchanged.

### M3 · every figure, and what leaves the phone (S–M)

- An `all` button beside fit and grid: `DimLayout.Scope.ALL`.
- A chain's segment figures and total, and a Line's and an area's tag, take a tap (a total the laser gives says so
  and takes none).
- The saved `-survey.jpg` pictures follow question 1's answer. They stay English and the room as surveyed.
- The old `vtag`, `Marks.show` and the both-figures code are deleted with their last caller.

### M4 · the 360 (S)

In 4 · 360 the view turns to the active dimension's mark and rings it in gold. No lines on the sphere.

## 5. The journey — slices to one English walk

Today the front (sign in → survey → upload → S5 shows the room) and the last step (Approve with its scope) are real
on both sides. The middle is labels: nothing is handed to a vendor, no quote comes back, and the estimate is a
separate S5 app. Two phone faults break the front half: a change left by ← never reaches S5, and ✓ Save needs all
six surfaces bounded.

In build order. Each is one slice; phone and web are separate pieces.

| # | id | Slice | Piece | Size | Needs |
|---|---|---|---|---|---|
| 1 | SP0 + M1 + M2 | Site progress hidden; measure on the photo; tour off | S2 APK | built + M + M–L | — |
| 2 | J1 | **What he does reaches the office.** Leaving survey prep by ← queues the upload when something changed since the last one; the web line (On web ✓ / Waiting to upload / Not on web) sits on the bar, not behind ⋮; when an upload lands the phone reads S5's project list, so a new project stops reading "not on S5 yet"; and, by question 2, ✓ Save works with fewer than six surfaces bounded | S2 APK | S | question 2 for its last part only |
| 3 | J2 | **True words.** The strip's lines stop naming tools the phone does not have (vendor packs, hand-off); one set of stage names on the phone, S5's stored ones; the "Open ImageMeter → …" line after a capture goes. Amit is shown the new English on a selector when the slice starts | S2 APK | S | — |
| 4 | J3 | **The rest of L10.** 1 · Lines, the room-size pencil and Capture 360 follow the phase (`Tool.LINES` joins `PREP_HONOURS`; `Tool.CAPTURE` on the room screen, its table row as it is) | S2 APK | S–M | M2 |
| 5 | PL | The room's plan opens from the room screen (decided 11 Nov). It no longer waits for prototype item P2: it is built on the phone | S2 APK | S | — |
| 6 | J4 | **The wish list on `/projects`**: read for everyone admitted, edit for staff, on the routes that exist | S5 QUOTE | M | — |
| — | | **Walk 1 can be made here** | | | |
| 7 | L9 | The wish list counts its marks (port 62 §3.9, unchanged) | S2 APK | S–M | — |
| 8 | J5 | **"Print for vendor ▸ trade"**: that trade's wishes on the room's faces with a measured list, printed to PDF and shared by hand | S5 QUOTE | M | its own decision: what a vendor's document carries |
| 9 | J6 | **A quote log** per trade and vendor (sent, received, the vendor's file); "Vendor quoted" is then derived | S5 QUOTE | S–M | its own decision: where a quote is kept (port 62 §6) |
| 10 | J7 | **The estimate linked to the project**, the approval scope pre-ticked from it | S5 QUOTE | S–M | its own decision: where the estimate is made |
| — | | **Walk 2: every step hands something real to the next** | | | |
| 11 | L7′ | A wish's outline and pill on the 360 | S2 APK | S–M | — |
| 12 | L8 | A wish on the six drawings and the export (port 62 §3.8, unchanged) | S2 APK | M | — |
| 13 | J8 | `/projects`' room page: one figure per dimension on the photo, if he wants the web to match the phone | S5 QUOTE | M | after he has seen M2 |

M3 and M4 fit anywhere after M2; M3 waits for question 1.

**Walk 1**, in English, on a test project of its own (approval cannot be undone): sign in → client, site, project,
room → Capture 360 with its sizes → lines → marks → measure → the room on S5 → Client wish list: wish marks and the
wish list, on the phone and on the web → the vendor round and the estimate by hand, the stage moved by hand →
Revisions → Approve with its scope. Walk 2 replaces "by hand" with J5–J7.

J1's caution for its builder: the upload job sends the record, then six photos, then the readings
(`app/AsIsUpload.kt:357-576`). Before ← queues it on every change, check what the job re-sends; if it sends the
photos again each time, J1 first stops that or reports the cost. S5's record rules for a room with fewer than six
bounded surfaces are read in `functions/_lib/asis.js` before the last part is built.

**What port 62's remaining slices become**

| Port 62 | Becomes |
|---|---|
| L7 · a wish on the 360 and in 3 · List | **L7′**: the 360 half only. The List half is void: there is no list |
| L8 · a wish on the six drawings | unchanged, after walk 2's S5 slices |
| L9 · the wish list counts its marks | unchanged, row 7 |
| L10 · lines, readings, room size, Capture 360 by phase | readings → **M2**; the rest → **J3** |
| P1, TR · the tour texts and the two more tours | **held** until Amit approves the journey; then written for the phone, not a prototype |
| PL · the room's plan from the room screen | row 5; prototype item P2 is dropped |
| RP, AR, RQ (S5 web) | unchanged in port 62's plan; J5 uses photos until RP's line drawings exist, and J5–J6 are the first small steps of RQ |
| The Q-list for the prototype (§3.10) | item 1 (List rows or chips) is void; the others are judged on the phone when their screen is next touched |

## 6. Open questions for Amit

None blocks M1 or M2. Each is asked as a selector, recommended option first.

1. **S2 APK — the saved "survey marks" pictures in the gallery** (what he shares for an estimate). Today they
   carry `≈photo | reading ±diff`. (a) **One figure per dimension, the `≈` kept on what was not measured**
   (recommended: whoever quotes from the picture can tell an estimate from a reading); (b) measured figures only,
   unmeasured dimensions left off; (c) leave them as they are.
2. **S2 APK — may a room be saved and sent with fewer than six surfaces bounded?** (a) **Yes: ✓ Save works from
   one bounded surface, and pictures are written for the bounded ones** (recommended: a one-window job needs one
   wall); (b) no, keep all six.
3. **S2 APK + S5 QUOTE — what "verified end to end" must include before the Marathi starts.** (a) **Walk 1: the
   phone's whole journey and the wish list on the web, the vendor round and the estimate by hand** (recommended:
   only the phone has Marathi, and J5–J7 add no phone text); (b) walk 2: the vendor document, the quote log and the
   linked estimate exist first.

## 7. Risks, and what is not in this design

- **The check's threshold is a guess.** If `!` fires on ordinary estimates it will teach him to ignore it; it is
  widened or removed from his first rooms' evidence, not argued.
- **Readings after Survey.** From M2 a figure missed on site cannot be keyed once the stage has moved, until the
  phase is moved back. It is the approved phase rule and the refusal says what to do; the build note says it too.
- **No list means no one-screen review of every figure** until M3's `all`. The tab counts and the walk say what is
  left; the Plan tab draws each surface with its figures.
- **The screen cannot be seen here.** `:app` is compiled by CI only; plate sizes, the pad's height and the turned
  text are first seen on the Mi A3. The numbers in §1.4 and §1.6 are budgets, checked on the phone.
- **Not designed here:** the vendor document, the quote log and the estimate link beyond their one-line scope (each
  gets its own note when its slice starts); S5's arranger and line drawings (port 62 §4.1, §5); Marathi for any new
  text; site progress; the tours.
