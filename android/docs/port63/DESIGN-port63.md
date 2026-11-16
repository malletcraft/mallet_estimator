# DESIGN-port63 — measure on the photo, and the survey → quote journey

S2 APK (`mallet_estimator` → `android/`), with S5 QUOTE's part of the journey named. Written 16 Nov 2026 (IST),
branch `port63-measure`.

**Basis.** Code read at `775d33c` (0.3.332 = port 62 build 3) and slice SP0 on `port63-sp0` at `2fec4fe`; four
readers' reports (the screen today, ImageMeter's manual, the journey walked in the code, two candidate designs).
Revised the same day after the plan critic's seventeen findings, each checked against the code first; §8 records
what was confirmed, what changed and the one part declined. Nothing was run for this file, no gradle, nothing seen
on a phone. Every "decided by Claude" below is judged by Amit on the phone, not on a prototype: the prototype is on
hold.

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

**One check stays, as a mark, never a number — and only on a mark that lies in the squared plane.** A measured
plate carries a small amber `!` after its figure when the reading differs from the photo by more than the larger of
150 mm and 15 % of the estimate. It is computed from the reading and the box as it is now, so it also catches a
mark moved after it was lasered; nothing is stored. The threshold is deliberately wide: it should fire on a shot at
the wrong target, not on an ordinary estimate. It is Claude's starting figure, to be tuned from his first rooms.

Which marks it is made on is not a figure to tune. A face is squared to the wall's (the floor's, the ceiling's) own
plane, so a thing that stands off that plane is nearer the camera and its box on the squared photo is larger than
the thing: a 600 mm deep wardrobe on the wall of a 2.7 m room, seen from the middle, reads about 1350 / 750 = 1.8
times its width; a 450 mm high bed on the floor about 1.4 times. A check there would flag the true reading of every
such mark, and a warning that fires every time teaches him to skip it. So:

- the check is made on a mark that asks no `p`, and on one whose `p` is a depth INTO the wall: a wall's Door,
  Window, Opening and Niche (`D`, `W`, `O`, `N`);
- it is never made on a mark whose `p` is a projection, a depth from the wall, a drop or a height — a wall's
  Column, Beam, Loft, Grill, AC unit, Curtain track, Carpentry unit and Sanitary ware; the floor's Step, Carpentry
  unit and Sanitary ware; the ceiling's Beam, AC unit and Ceiling drop (`SurveyPrep.measures0`,
  `pano/SurveyPrep.kt:885-903`; `MarkTypes.measures`, `pano/MarkTypes.kt:377-389`). On these no figure of the mark
  carries `!`, its position figures included.

`MarkDims.inPlane` is that rule (§4, M1), and a type added later with a `p` is a red test until it is classified.

The `!` says nothing by itself. When a reading that fails the check lands, and again whenever a plate carrying `!`
becomes active, the screen's existing message line says why: "The photo says ≈640 mm. Check the mark, or shoot
again." (§1.4). After a reading the phone no longer shows the photo's estimate anywhere, except inside that
sentence. The office still sees both on S5 `/projects`.

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
- **Line, area, chain**: already one figure. Two fixes. A plotted Line's tag shows its reading once read, else `≈`
  before the photo's length (`F-M1 1240 mm` / `F-M1 ≈1234 mm`, as S5 already draws it); a Line still being pulled
  keeps `1234 mm`. An area's tag shows its perimeter after its m² once read: `F-M2 1.86 m² · 5734 mm` (figures
  only, no word) — today a read perimeter is shown nowhere but the list (`pano/SurveyMeas.kt:287-303`). All of
  these figures take a tap (§1.5).

A mark that is **not in the room** (born a Client wish or Vendor added; `SheetMarks.surveyed` is false,
`pano/SheetMarks.kt:53`) keeps its state outline and pill and shows estimate-style figures. It is never asked for a
reading: it is never active and it is not in the walk or the counts. Its plates are still tap targets: a tap picks
the mark (so its label is there to open the Mark sheet) and says `sp.measure.not_in_room`, and that is all it
does. A reading such a mark already holds from an earlier build (0.3.332 gave it a reading box) is shown as a
measured figure, S5's rule for the same mark, and is not removed. A mark that was surveyed keeps its readings
whatever its state.

The wall's own W × H stays in the caption above the photo ("Front wall · W 2678 × H 2677").

### 1.3 The steps, and where measuring happens

The strip keeps four steps. The third changes from the list to the photo:

`1 · Lines` · `2 · Mark` · `3 · Measure` · `4 · 360`

- **2 · Mark** is for placing and moving. The picked mark shows all its dimensions in the new one-figure style.
  Placing or picking a mark makes its first unmeasured dimension active, as today, and a tap on a figure makes that
  one active; the measure bar then appears above the palette. So "place a door, shoot its width" still works
  without leaving the step (ImageMeter's "select, then shoot"). After a reading the next unmeasured dimension of
  the same mark becomes active, as today; when the mark has nothing left nothing is active and the bar goes.
- **3 · Measure** is the same squared photo with the figures on it and a walk through the unmeasured ones. Nothing
  can be moved, resized or placed there, so a thumb on the photo cannot drag a mark while the other hand holds the
  laser. A drag pans; a pinch zooms.
- **4 · 360** keeps the same bar (§1.7).

**3 · Measure has a picked mark**, the screen's existing `sel`, and these are all its taps:

- A tap on a **mark** picks it: its position figures are drawn (§1.6) and its first unmeasured dimension becomes
  active if it has one. A fully measured mark is picked with nothing active, and every plate of it takes a tap;
  that is how a reading is corrected.
- A tap on a **plate** picks its mark and makes that dimension active. A tap on the plate that is already active
  opens the pad (§1.4).
- "Becomes active" is always at Survey (or while the stage is unread) and for a mark that is in the room. After
  Survey (§1.8) and on a mark not in the room (§1.2) the same taps pick, say why, and make nothing active.
- A **Line, an area and a chain** are picked by a tap on one of their figures, which makes that reading active
  (§1.5). A tape has no figure to read and is not picked here.
- A tap on **empty photo** does nothing: a stray thumb must not lose the dimension the laser is about to fill.
- **No tap target is added on a code**, and `SurveyPrepTouch.hit` gains no new `Hit`. The code is drawn inside the
  box's top-left corner (`SPD:509`); on a 25 × 11 dp switch plate it covers the whole box, so "tap the code" and
  "tap the mark" could not both exist. 3 · Measure calls `hit` with `selId = null` and no chain nodes, so it
  answers `Tag` or `Mark "body"` only (`pano/SurveyPrepTouch.kt:104-111`, `:131`).

**The Mark sheet opens from the picked mark's label**, the way it does today: in 0.3.332 it never opened from the
code on the photo but from the 52 dp label of the picked mark's row under the photo (`SPS:1735-1738`), from a
Line's row (`SPS:1769-1770`) and from the list (`SPS:1526`). In 2 · Mark and 4 · 360 that row stays as it is. In
3 · Measure, which has no such row, the label is the left cell of the measure bar (§1.4): the same 52 dp cell, the
state's 3 dp bar on its left edge, `clickable { openSheet(id) }`, the existing `life.aria.open_mark_sheet_x`. It is
drawn at every stage, after Survey too, where the rest of the bar is not (L4's rule: the label stays, controls are
hidden). A picked Line or area gets a label the same way; a tape and a chain none, as today
(`MarkSheet.opens`).

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
| Re-opening a reading already taken (a mark's, a Line's, an area's, a chain's) | a tap on its figure on the photo, or `‹` (§1.5): both in M2, the build that removes the list |
| A read area's perimeter | on the area's tag (§1.2) |
| Every figure of a face at once | the `all` toggle in 3 · Measure (§1.6), in M2 |
| A mark's code opening the Mark sheet | the picked mark's label: 2 · Mark's row as today, the measure bar's left cell in 3 · Measure, at every stage — so at Estimated and Client approved, where there is no 2 · Mark, the sheet still opens |
| Delete / Drop / Restore | already on the picked mark's row in 2 · Mark |
| "… is outside the face" | the red dashed outline on the photo, as today |
| All six surfaces at once (LIST tab) | a count on each face tab, and the walk's "next face" (§1.5) |

The tabs become FRONT RIGHT BACK LEFT / FLOOR CEILING PLAN. Each row's tabs share the row's width equally, four in
the first and three in the second: every tab is `Modifier.weight(1f)` in its own row already (`SPS:1301-1313`), so
dropping `list` from `TAB_ROWS` is the whole change and the strip's touch routing is untouched.

### 1.4 The bar, the pad, the laser

```
┌──────────────────────────────────────┐
│ Survey … (the phase band)            │
│ ←  Living · survey prep   ↶ ⋮ ✓ Save │
│ FRONT ✓  RIGHT ✓ 2  BACK   LEFT      │
│ FLOOR       CEILING       PLAN       │
│ 1·Lines  2·Mark  3·Measure  4·360    │
│ Front wall · W 2678 × H 2677         │
│ ┌──────────────────────────────────┐ │
│ │        ├── ≈1203 ──┤        fit  │ │
│ │       ┌───────────┐         grid │ │
│ │       │FW1        │ 1198    all  │ │
│ │       └───────────┘              │ │
│ │  ├─▶≈640─┤  ⊥ ?                  │ │
│ └──────────────────────────────────┘ │
│ FW1    ‹ from left      [≈640] D2 ›  │
│ Window   corner                      │
│          7 of 18 measured            │
└──────────────────────────────────────┘
```

**The measure bar** — `measureBarDp(land)`: 56 dp in portrait, 48 dp in landscape — is one composable used in
steps 2, 3 and 4:

`label` · `‹` · two lines of text · the figure box · `D2` · `›`

- **The label cell** (52 dp) is drawn by the bar wherever the picked mark's own row is not: in 3 · Measure, and in
  2 · Mark and 4 · 360 while the pad is open. For a mark it reads its code over its type's SHORT name
  (`SurveyPrepPalette.short`: `Window`, `Switch`, `Unit`, `Sanitary` — the full name does not fit 52 dp), or over
  `locked`; for a Line or an area its code. It opens the Mark sheet (§1.3). 2 · Mark's own row keeps its label as
  it is today (the code over Move / the edge being moved / `locked`).
- **Line 1 is the measure's own words alone**: `from left corner`, `sill, from floor`, `reveal depth`; a Line
  `length`, an area `perimeter`, a chain `segment 2` or `total` — the `what` of the active key
  (`Prep.readingOf`), nothing composed. 12 sp, at most two lines, then an ellipsis. The code and the type are
  beside it in the label cell (in 2 · Mark, in the row above), so the line does not repeat them.
- **Line 2 is progress**, `7 of 18 measured`, 10 sp, one line.
- **The check's sentence is not in the bar.** It is said through the screen's message line
  (`say(…)`, the line every refusal already uses) when a reading that fails the check lands, and again when a
  plate carrying `!` becomes active.
- **The figure box** (64 dp) shows the same one figure as the plate, in the same style; from the first key press
  it shows the pad's digits. A tap opens the pad.
- **`D2`** fires the laser, as today's button does (`fire()`, `SPS:505`). The meter's own button works the same.
- **`‹` and `›`** are 36 dp wide and as tall as the bar.

The width, which is why the lines are what they are. Portrait: 360 dp less the screen's 20 and the bar's own 12 is
328 dp; landscape's column is 318, so 306.

| | Portrait | Landscape |
|---|---|---|
| label 52 · `‹` 36 · box 64 · `D2` 40 · `›` 36 · five 4 dp gaps | 248 | 248 |
| left for the two lines, with the label cell | 80 dp, about 12 characters a line | 58 dp, about 9 |
| left for the two lines, without it (2 · Mark, pad closed) | 136 dp | 114 dp |

So `from left corner` and `underside, from floor` hold in two lines; the longest, `from right corner, to centre`,
is cut with an ellipsis in the narrow cases. That is accepted: the gold plate on the photo is what says which line
is being measured, and the words name it. These are budgets, first seen on the Mi A3 (§7).

**Where the bar is, and what `‹` and `›` do.**

- **In 3 · Measure** the bar sits where 2 · Mark has its palette and is always there at Survey. `‹` goes to the
  stop before, read or not, to correct it. `›` skips: the dimension stays unmeasured, the walk goes to the next
  unmeasured one and comes round again. Line 2 counts the open face. Its states:
  - a dimension active: the whole bar;
  - nothing active and something left: label, `‹`, the progress line, `›` (which goes to the first unmeasured);
  - the face finished: label, `‹`, "Front wall · all 9 measured ✓" and the next-face button in `›`'s place (§1.5);
  - nothing asked on the face: `fp.nothing_measure_face_yet`.
- **In 2 · Mark and 4 · 360** the bar shows only while a dimension is active, where the reading strip is today.
  `‹` and `›` move within the picked item's own keys only — a mark's `measures()` order, a chain's keys — read or
  not, wrapping; they never change the selection, and an item with one key (a Line) draws neither. Line 2 counts
  the picked item's face (`sel.s`), since 4 · 360 has no open face. When the picked item has nothing left the bar
  goes, as the strip does today; a tap on one of its plates brings it back for a correction.

Only in 3 · Measure do `‹`, `›` and a reading walk from one mark to another.

**The pad** is the app's own, not the system keyboard, which covers far more of the screen, jumps at every field
and has no Next. Four rows of `padRowDp(land)`: 44 dp, 36 dp in landscape.

```
 1   2   3   ⌫
 4   5   6   Clear
 7   8   9   Next ▸
 ⌄   [  0  ]   (Next ▸, two rows tall)
```

`0` spans the two middle columns.

- **It opens only when asked**: a tap on the bar's figure box, or on the plate that is already active. It never
  opens by itself when a dimension becomes active, so placing and nudging are never covered unasked and a laser
  user never sees it. It is closed on first entry to survey prep; open or hidden is then kept in a `remember` of
  the survey prep dialog, and lost when the dialog closes. `⌄` hides it.
- **What it starts with.** When a dimension becomes active the pad's text is `MmBox.shownText(prep.actual[code])`
  — the STORED reading, or empty. Never the estimate, and never a chain's derived or laser figure, which are not
  readings (the fault `pano/MmBox.kt`'s header records). A `fresh` flag is set.
- **Keys.** While fresh, the first digit REPLACES the text and `⌫` empties it; after that a digit appends and `⌫`
  removes one. Digits only, six at most, whole mm; a `0` that would lead the figure is ignored. `Clear` empties.
  From the first key press the digits show live in the bar's box and on the gold plate (an emptied pad shows the
  box's `mm` hint, and the plate keeps the figure it had until the commit).
- **Every commit is `MmBox.commit(prep.actual[code], text)`**, unchanged (`pano/MmBox.kt:26`). So an untouched pad
  writes nothing: walking with `‹`, `›` or `Next ▸` over a measured dimension never changes or removes it. Typing
  the figure it already holds writes nothing. An emptied pad over a stored reading removes the reading, and the
  estimate returns; an empty pad over no reading writes nothing.
- **`Next ▸`** commits and moves on as `›` does; with nothing typed it IS `›`. Leaving the dimension any other way
  commits what was typed too, as losing focus does today: a tap on another plate or mark, `‹`, `›`, `⌄`, a change
  of step, face or tab, ← and ✓ Save.
- **A laser reading that arrives while digits are uncommitted discards them and is the reading.**
- **Where it sits.** Under the bar; the photo gives up the height and nothing is covered. In 3 · Measure that is
  bar + pad. In 2 · Mark and 4 · 360, while the pad is open it takes the place of the palette AND of the picked
  mark's row (nudges, padlock, delete); the label moves into the bar; `⌄` brings both back. All four together do
  not fit: in landscape the 324 dp column less the bar (32), the tabs (56) and the strip (24) leaves 212 dp, and
  the picked row (about 34) + bar 48 + palette 52 + pad 144 is 278. Bar + pad is 192. In portrait bar + pad
  leaves the photo about 342 dp of height (780 − 24 − 12 − 170 − 56 − 176), its full square.

**The laser** is unchanged in its rules. A reading lands on the active dimension, decided when it arrives
(`SurveyRef.route`, `SPS:522-533`); a reference box's shot never becomes a mark's reading. New in M2:

- In 3 · Measure, when the mark has nothing left, the walk moves to the next unmeasured dimension on the face, with
  no tap on the phone; the picked mark follows it. The photo pans only if that plate is off screen.
- A short tick when a reading lands, because his eyes are on the laser dot:
  `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)` — the value read once in
  the dialog's composition, the call made where the laser's reading is taken. No `Vibrator`, no permission (the
  manifest has none for it).
- The plate turns from grey `≈` to white with the figure.

**Corrections.** Tap the plate (or `‹`), then shoot, or open the pad and type: the new value overwrites. `Clear`
then `Next ▸` removes a reading. ↶ still never undoes a reading.

### 1.5 Progress and the next unmeasured

- **In the bar:** `7 of 18 measured` for the open face (in 2 · Mark and 4 · 360, the picked item's face).
- **On each face tab:** a small amber count of what is still to measure there (`RIGHT ✓ 2`). No count when nothing
  is left or nothing is asked. The existing ✓ keeps its one meaning, "four lines set".
- **On a mark:** `✓` after its code once everything it asks is measured.
- **A finished face:** the bar reads "Front wall · all 9 measured ✓" and offers "Next: Right wall · 4 to measure ▸".
  It keeps `‹`, which goes to the face's LAST stop, so a reading on a finished face can still be reached without
  finding its plate. It never changes face by itself: he has to turn round. When no face has anything left: "Room
  measured · all 31 ✓".
- **Entering 3 · Measure**, or changing face inside it, makes the first unmeasured dimension active, so he can
  shoot at once.

**Every reading can be re-opened from the photo.** A mark's plates take a tap. So do the figures that are not
plates: a Line's and an area's tag and a chain's segment figures and its total are added to `o.tags` under their
reading keys (`F-M1.len`, `F-M2.per`, `FK1.2`, `FK1.t`), so a tap makes that reading active exactly as a plate's
does. A chain total the laser gives is not added, and a tap on it does nothing: it takes no reading, as today
(`SurveyChain.totalReadOnly`). This is in M2, because M2 deletes the list: today the list is the only place a
read CHAIN reading can be retyped or cleared (picking a chain makes only an OPEN key active, `SPS:1001`,
`Prep.nextOpen`, `pano/SurveyPrep.kt:1266-1284`) and the only place a read perimeter is seen, and 3 · Measure
picks a Line or an area by its figure alone. In 2 · Mark a fresh pick of a Line or an area goes on making its
key active, read or not, as today (`SurveyMeas.pickActive`).

**The walk's order** (one rule for all six surfaces):

1. the marks that are asked, by their left edge `x0` rounded to the whole mm, ascending; equal `x0`: `y0`
   ascending (lower first); then by code. A point is ordered by its box like any mark;
2. inside a mark, the order `measures()` gives;
3. then Lines and areas in stored order;
4. then each chain's `SurveyChain.openKeys` in stored order — its segments, then its total, the total left out
   where the laser gives it.

Notes, tapes and marks not in the room are left out. Faces follow the tabs: front, right, back, left, floor,
ceiling.

**What counts as left.** A stop is LEFT when it has no reading — and, for a chain's key, when `SurveyChain.vals`
gives it no figure either. A chain is used to read n − 1 segments and derive the last (`vals`, `der`,
`pano/SurveyChain.kt:153-166`), and with every segment read its total is derived; neither has a reading by design.
So a derived segment and a derived total are not left: they are skipped by the walk's `next`, they are not in the
tab's count, and `7 of 18` counts neither side of them (`read` is the stops with a reading, `total` is `read` plus
what is left), so "all N measured ✓" is true of N readings. Each is still a stop: a tap or `‹` reaches it and it
takes a typed or lasered reading. A total the laser gives is not a stop at all.

Progress never gates ✓ Save.

### 1.6 A busy wall at 360 × 780 dp

The example wall (a door, a window, a switch plate, three electric points) asks for 19 figures. The photo is about
340 dp square. All 19 with their lines would put six lines across the wall and five up it.

So, in 3 · Measure, as it opens:

- **Every mark shows its size**: `w` on its top edge and `h` on its right edge, 10 sp.
- **Only the picked mark shows its position** dimensions (corner, floor or ceiling, drop, the `⊥` chip), with their
  lines, at 12 sp — whether or not one of them is active, so a fully measured mark that is tapped shows them all
  for a correction. The other marks draw at 75 % and the rest of the photo dims 20 %, as today.
- **A small box** (both spans shorter than their plates plus 8 dp) gets one compact plate beside it,
  `≈200 × ≈90`, each figure in its own style and its own tap half.
- **A point** that is not picked shows its spot, its code and its `✓`.

That is 6 to 9 plates on the example wall, not 19. In 2 · Mark only the picked mark shows figures, as today.

**The 10 sp and 12 sp are dp on the canvas, not the painter's units** (the review of M1, 16 Nov 2026). The painter's
unit is `u = canvas / 480` (`SPS:2990`): 0.71 dp on the Mi A3's 340 dp photo, and smaller again on a smaller canvas
(landscape). Sized in units, as M1 first had it, a picked mark's figure was 8.5 dp (today's tag) and every other
mark's 7 dp, and shrank with the canvas. So a plate — its figure, 12 dp or 10 dp, and its padding (10 dp across, 2 dp
over the text, 5 dp under) — is in dp and keeps its size on screen whatever the canvas; it does not follow the
system's font scale, as nothing drawn on the photo does. The dimension LINES stay in units, where build 332 draws
them.

**And every figure at once, when he asks: the `all` toggle**, beside fit and grid in 3 · Measure, off on entry
(in M2 — Amit: "imagemeter shows all dimensions on the annotation there itself"). On, every mark draws every
figure it has (a mark not in the room in its estimate style), nothing dims, and plates still take a tap. It is a
view, so it works at every stage. Zoom spreads the plates apart; the placing rule below decides the rest.

Plates keep a constant size on screen. When two would overlap, the placing rule (pure, in `pano/DimLayout.kt`,
tested here) is, in this order:

1. Place in rank order: the active dimension, the picked mark's others, unmeasured, measured, then the figures
   of a mark not in the room.
2. A plate sits mid-span on its line. A span too short for it: the plate steps off the line by one plate height on
   a short leader, arrowheads outside pointing in.
3. If it would cover a placed plate, a mark's code or a box it is told to keep clear: try ¼ and ¾ along the line,
   then step off it on a leader.
4. Still no room: it shrinks to a 6 dp dot on its line (hollow = not measured, solid = measured) with a 32 dp tap
   box. The active dimension is placed first and never shrinks. A dot takes a spot of its own — the middle of what
   the canvas shows of its line, else along it, else just beside it (a `p`: one of the mark's corners) — clear of
   every plate and dot already placed, and keeps it: no dot is drawn under a plate.
5. Every plate stays inside the photo.

**The canvas is the viewport, not the wall** (the review of M1). Zoomed or panned, a mark can be wholly off the
canvas. A figure whose dimension — the span it measures, a `p`'s corner — is wholly off the canvas is not placed at
all: no plate, no dot, no tap box. Two exceptions, slid into view as the picked mark's tags are today: the active
dimension and the picked mark's figures. The model says which were (`Plate.off`) and whether the active one was
(`Out.activeOff`), which is what §1.4's "the photo pans only if that plate is off screen" asks. A dot is never slid
onto the canvas from outside it.

**In 2 · Mark the picked mark keeps its tabs clear** (v52 item 14, Amit's "center handles"): for a picked box mark
that shows its eight tabs — not a point, not locked, editable — the `w` and `h` plates are anchored by
`SurveyPrepTouch.widthTag` / `heightTag` with `hand = true` (`pano/SurveyPrepTouch.kt:53-59`, as `SPD:813-824`
does today), not mid-span on the line, and the eight tab squares join the boxes to keep clear. The plates' tap
boxes (`o.tags`) are what `trashSpot` is given, as today (`SPS:3057`).

### 1.7 The 360 and the Plan

- **4 · 360:** no dimension lines on the sphere. The bar is the same one, so a picked mark can be shot or typed
  there, and the figure is in the bar; `‹` and `›` stay inside the picked item (§1.4). A Line's tag there follows
  §1.2. M4 turns the view to the active mark and rings it in gold.
- **PLAN tab:** unchanged. It already shows one figure per dimension.

### 1.8 Phases, wishes, locked marks

- **Readings follow the phase from M2** (the readings part of port 62's L10): `Tool.READINGS` joins
  `PhaseTools.PREP_HONOURS`. At Survey, and while the stage is unread, everything above applies. After Survey the
  plates are figures: nothing becomes active, there is no pad, no `D2`, no `‹` `›` and no counts, and a tap on a
  plate (or a laser reading arriving) says the existing `refuse.readonly.readings`, "Readings are taken in the
  Survey phase — move the phase back on the project screen to key one".
- **After Survey 3 · Measure says so without being asked.** The quote stage is the project's, not the room's, and
  a live project can already sit at Client wish list; a photo with figures and an empty space under it would read
  as a broken screen. So where the bar would be, 3 · Measure shows one line always: the picked mark's label (when
  one is picked) and `refuse.readonly.readings` beside it, 11 sp, three lines at most. Picking, the Mark sheet and
  the `all` toggle work as at Survey. In 2 · Mark and 4 · 360 the sentence is said on the tap and is not a
  standing line (§8, finding 7). At Client approved the stage cannot be moved back, so the sentence's second half
  does not apply there; it is the prototype's sentence for every stage after Survey and its words are J2's to
  judge, not reworded here.
- **Capture 360 follows the phase from M2 too** (brought forward from J3): the room screen's Capture 360 button is
  drawn only where `PhaseTools.can(Tool.CAPTURE, stage)` — its row is already `[1,0,0,0,0,1]`. Without it, from M2
  a room captured after Survey could be neither measured (this rule) nor, at Estimated, marked, and until then its
  doors and windows are born wishes (port 62 §3.4, the L4 review's finding 2, which left this gate open to be
  brought forward). At Client approved the row shows the button again, as the prototype does for execution; with
  site progress off a 360 taken there is the Quote journey's and its survey prep has no mark tools and no
  readings. That cell is left as SP0 left it and is named here so it is not read as covered.
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
- Hit order: tabs, then plates, then marks (`SurveyPrepTouch.hit`), which gains no new `Hit`.
- Nothing scrolls. The photo takes every dp the controls leave.
- The saved `-survey.jpg` gallery pictures are **not changed by M1 or M2**: they keep today's painter path for mode
  `sheet`, and `lineTag`'s new parameters default to what it prints today (§4, M1), so they are byte-identical
  until Amit answers question 1; M3 moves them.

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
| `sp.measure.photo_says_x` | `The photo says ≈{est} mm. Check the mark, or shoot again.` | the screen's message line, when a reading that fails the check lands and when a plate carrying `!` becomes active — never in the bar |
| `sp.measure.not_in_room` | `Not in the room — a mark placed after the survey has nothing to measure` | the message line, on a tap on such a mark's figure (a Client wish or a Vendor-added mark alike) |
| `sp.measure.x_to_measure` | `{n} to measure` | a face tab's count, read out |
| `sp.measure.all` | `all` | the toggle beside fit and grid in 3 · Measure |
| `sp.measure.aria.all` | `Every figure on this face` | that toggle, read out |
| `sp.measure.aria.previous` | `The measurement before` | `‹` |
| `sp.measure.aria.skip` | `Skip to the next measurement` | `›` |
| `sp.measure.aria.type` | `Type the figure` | the bar's figure box |
| `sp.reference_shown_measure` | `Reference shown on 2 · Mark, 3 · Measure and 4 · 360` | `pano/SurveyRef.kt:68`, in place of `sp.reference_shown_2_mark` |

**Existing keys reused, Marathi untouched:** `common.measure` (the step's name, "Measure"), `common.next` (the
pad's Next, with the glyph ` ▸`), `fp.clear` (the pad's Clear), `fp.nothing_measure_face_yet` (a face with nothing
asked), `refuse.readonly.readings` (said on a tap, and 3 · Measure's standing line after Survey),
`refuse.sp.tap_measurement_first`, `sp.msg.d2_x_tap_measurement`, `sp.locked`, `life.aria.open_mark_sheet_x`, the
measure names (`measure.*`, `life.length`, `sp.segment`), and the type names and short names.

**Keys that lose their reader and stay in the tables:** `common.show`, `common.photo`, `common.both`,
`sp.reference_shown_2_mark`, and `common.list` if nothing else reads it. If a table test refuses a key with no
reader, the builder says so and follows that test's own rule; the Marathi value is not deleted.

**Glyphs**, each a `// text:value` literal, byte-equal in both languages: `≈ ? ▶ ⊥ ✓ ! × ‹ › ⌫ ⌄ ▸`, and the
box's `mm` hint.

Texts of the journey slices (§5) are listed in each slice's own commit, by the same rule.

## 4. The slices

M1 and M2 ship together, with SP0, as one Play build: the tour must go off in the build that changes the screen,
and M1 alone shows nothing. **`port63-measure` takes `port63-sp0` (`2fec4fe`) FIRST, before M1**: SP0 appended two
lines to the END of `android/i18n/ported.txt` and a row to `literal-counts.tsv`, M1 adds five new `:pano` files
that each need both, and M2 needs `pano/Features.kt`. After the merge the `:pano` suite is run once and its totals
recorded before M1 starts.

How each is built: `:pano` is run here (`nice -n 15 gradle … --offline` from `android/`); `:app` is compiled only
by CI. Commits are local, of the changed paths only, and each message gives the `:pano` totals and says that
`:app` was not compiled here and nothing was seen on a phone.

| id | Slice | Piece | Size | Needs | What Amit sees |
|---|---|---|---|---|---|
| SP0 | Site progress behind `Features.SITE_PROGRESS` | S2 APK | built | — | no Album tile; after approval the line reads "Quote · Client approved" |
| M1 | The measuring models, pure, with tests; the new keys and `mr-pending.tsv` | S2 APK | M | SP0 merged | nothing on screen |
| M2 | The measuring screen; the list gone; every figure at once; readings and Capture 360 by phase; the tour off | S2 APK | L | M1, SP0 | one figure on each dimension, the bar, the pad, the laser walking on by itself, an `all` toggle; no list; after Survey the figures with one line saying why they cannot be keyed, and no Capture 360; no Help row |
| M3 | What leaves the phone: the saved pictures by his answer; the old painter deleted | S2 APK | S | M2; question 1 | the gallery pictures with one figure per dimension |
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
- `inPlane(s, e, a, b)` — the mark lies in the squared plane (§1.1): it asks no `p`, or it is a `D`, `W`, `O` or
  `N` on a wall. Anything else that asks a `p` stands off the plane.
- `value(actual, s, e, code, est, a, b): Value` — `text` from `SurveyPlan.figure` (the one rule), `state`
  (`MEASURED`, `ESTIMATE`, `UNKNOWN`), `check`.
- `check(read, est, inPlane)` — true when the mark is in plane, both figures exist and
  `|read − est| > max(CHECK_MM, CHECK_PART × est)`; `CHECK_MM = 150.0`, `CHECK_PART = 0.15`. For a mark that stands
  off the plane it is always false, for every one of its keys.
- `done(prep, s, e, a, b)` — every key the mark asks has a reading (the `✓` after its code).

**`pano/DimLayout.kt`** — plates in px: placing and collisions (§1.6). In: for each figure its code, role, line in
px, plate size, rank; the canvas size; boxes to keep clear (the marks' codes; a picked mark's eight tabs; any box
the screen passes, which in M2 is the fit / grid / all column, already measured as `fitAt`); `u` and `dp`. Out,
per code: the drawn box, the tap box, turned or not, a leader or none, shrunk to a dot or not; and for a small box
the one compact plate with its two tap halves. `Scope`: `PICKED` (2 · Mark: the picked mark, everything), `FACE`
(3 · Measure: every mark's size, the picked mark's positions), `ALL` (3 · Measure with the toggle on: every figure
of every mark). In `PICKED` scope, for a picked box mark that shows its tabs, the `w` and `h` plates are anchored
by `SurveyPrepTouch.widthTag` / `heightTag` with `hand = true` and the tab squares are kept clear. It takes over
the constants `SPD.dims` holds today and uses `SurveyPrepTouch.Box` and `Tag`. As built, with the review of M1
applied, see §9: plates in dp, the off-canvas rule, a dot's own spot, and the pad's live digits on the active plate.

**`pano/MeasureWalk.kt`** — the order and the counts (§1.5).

- `stops(prep, s, a, b)` — every reading key the face asks, in the walk's exact order (§1.5's four rules).
- `left(prep, s, a, b, code)` — no reading, and for a chain's key no figure from `SurveyChain.vals` either: a
  derived segment (`der`) and a derived total are not left.
- `next(prep, s, a, b, after)` — the next stop that is left, wrapping; `after = null` is the first one left; null
  when none.
- `previous(prep, s, a, b, before)` — the stop before, left or not, wrapping; `before = null` is the LAST stop of
  the face; null when the face has none.
- `count(prep, s, a, b): Count(read, total)` — `read` the stops with a reading, `total` = `read` + what is left;
  `room(prep, ab)`; `nextFace(prep, from, ab)` — the next face in tab order with something left, and how many.
- `stopsOf(prep, id, ab)` and `step(keys, from, forward)` — the picked item's own keys (a mark's `keysOf`, a
  Line's or an area's one key, a chain's `openKeys`) and the key before or after `from`, read or not, wrapping:
  `‹` and `›` in 2 · Mark and 4 · 360.

**`pano/MeasurePad.kt`** — the pad's text. `State(text, fresh)`; `start(stored: Double?)` =
`State(MmBox.shownText(stored), fresh = true)`; `press(state, key)`: while fresh the first digit replaces the text
and `⌫` empties it, then a digit appends (six at most; a leading `0` is ignored and leaves the state as it was),
`⌫` removes one, Clear empties; `live(state)` — null while fresh (the box and the plate draw the one figure), else
the text. The write is `MmBox.commit(stored, state.text)`, unchanged.

**`pano/MeasureText.kt`** — the bar's texts through `Tx`: `label` — the measure's own words, the `what` of
`Prep.readingOf(code)` (`from left corner`; a Line `length`; an area `perimeter`; a chain `segment 2`, `total`);
`cell` — the label cell: a mark's code over its type's short name (`SurveyPrepPalette.short`) or `sp.locked`, a
Line's or an area's code; `progress`, `faceDone`, `nextFace`, `roomDone`, `photoSays`, `notInRoom`.

**`pano/SurveyMeas.kt`** — `lineTag(code, kind, pts, reading: Double? = null, approx: Boolean = false)`. A Line:
with a reading, that figure (`F-M1 1240 mm`); without one, `≈` before the photo's length only when `approx`
(`F-M1 ≈1234 mm`), else as today (`F-M1 1234 mm`). An area: its m² as today, and with a reading (its perimeter)
` · 5734 mm` after it; `approx` does not touch an area. A Line being pulled (code null) and mode `sheet` pass
neither, so today's two callers print what they print today and the saved pictures are byte-identical until M3;
the existing pins (`MeasTest:358`, `SurveyDrawMarathiTest:84`) stay as they are.

**`pano/StepRoute.kt`** — only additions in M1 (the steps change in M2): `measureBarDp(land)` 56 / 48,
`padRowDp(land)` 44 / 36, `PAD_ROWS = 4`, and the landscape column's two budgets the screen's own comment gives
(`SPS:2001-2002`, a ~336 dp window less its 12 dp of padding): `LAND_TOPBAR_DP = 32`, `LAND_COLUMN_DP = 324`.

**Tests.**

- `MarkDimsTest`: for every type and sub-kind on every kind of surface, over a grid of boxes, the codes equal
  `Prep.keysOf` in order; every line's length equals its `est`; a point's lines end at its centre; a mark on the
  floor has no `s`; crossing the midline swaps `l` and `r`; no key falls to the unknown case; the figure equals
  `SurveyPlan.figure` for the same input; the check at 149 and 151 mm on a 900 span and at 15 % on a 2678 span,
  never without a reading or an estimate; a mark born a wish is not asked. **In plane:** every type and sub-kind
  on every kind of surface is classified; the set that asks a `p` equals, by name, the in-plane four (a wall's
  `D W O N`) plus the stand-off set (wall `C B L GRL AC CTR CRP SAN`, floor `S CRP SAN`, ceiling `B AC CDP`), so a
  type added with a `p` and no classification is a red test; a 600 mm wardrobe read at 1.8 times its estimate
  carries no `!`, on any of its keys.
- `DimLayoutTest`: home position; a turned plate; the small-box compact plate; the ¼ and ¾ slide; the leader; the
  dot; the active plate never shrinks; no two placed plates overlap; every plate inside the canvas; tap boxes at
  least 32 dp; a kept-clear box is never covered; the same input gives the same output; the example wall at
  340 × 340 in `FACE` scope with the door picked has no dot; the same wall in `ALL` scope places all 19 with no
  overlap, dots allowed and their count pinned; in `PICKED` scope a picked door's `w` and `h` plates stand where
  `widthTag` / `heightTag` with `hand = true` put them and cover none of its eight tabs, and a point's and a
  locked mark's do not move. Added by the review of M1 (§9): the example wall at the phone's own numbers; the
  zoomed and panned canvas (an off-canvas mark has no plate, dot or tap box; no more dots zoomed than at fit); a
  dot's own clear spot; the pad's live digits on the active plate.
- `MeasureWalkTest`: the order, with two marks whose `x0` differ by under a mm and two that share it; Notes, tapes
  and marks not in the room left out; a chain's total left out where the laser gives it; a derived segment and a
  derived total not left, not counted, skipped by `next`, still reached by `previous`; `next` wraps and skips what
  is read; `previous` includes what is read; `next(after = null)` the first left and `previous(before = null)` the
  last stop; the counts; the faces' order; null after the last reading; `stopsOf` and `step` wrapping inside one
  mark, one chain, and a Line's single key.
- `MeasurePadTest`: the start text is the stored reading or empty; `‹`, `›` and `Next ▸` across a measured
  dimension with no key pressed leave `prep.actual` unchanged; the first digit replaces; `⌫` on a fresh pad
  empties; six digits at most; a leading `0` ignored; Clear then `Next ▸` removes the reading; typing the stored
  figure writes nothing; a derived chain segment starts empty and a blank commit writes nothing.
- `StepRouteTest` (M1's part): the landscape column — `LAND_TOPBAR_DP` 32 + tabs 56 + strip 24 +
  `measureBarDp(true)` 48 + `PAD_ROWS × padRowDp(true)` 144 = 304 — is at most `LAND_COLUMN_DP` 324.
- `MeasureTextTest` (English: the label is the measure's words alone, the cell's short names, a locked mark),
  `MeasTest` (`lineTag`'s cases: a plotted Line with a reading, with `approx`, with neither as today, a Line
  being pulled; an area with and without its perimeter), `i18n/MrPendingTest`, and `TxRenderTest`'s two scans
  reading the pending list.

### M2 · the screen (L)

`:app` compiles only in CI, so every import, accessor and caller is exact; the rules of the screen are asked of
`:pano`, never written again in `:app`.

1. **Painter** (`SPD`): for the screen, `dims` draws from `MarkDims.of` and `DimLayout`; a new `plate` draws one
   figure in its state's style (§1.1), turned on a vertical line, with the `!`, the `⊥` chip and the `✓` after a
   code. `Marks` gains the scope (`DimLayout.Scope`); with `ALL` nothing dims. Every plate is added to `o.tags`,
   so the hit order holds — and so are a Line's and an area's tag and a chain's segment figures and total, under
   their reading keys (`F-M1.len`, `F-M2.per`, `FK1.n`, `FK1.t`), each with the drawn label's box grown to 32 dp
   at least each way; a chain total the laser gives is not added. The screen's two callers of `lineTag`
   (`SPD:669` for mode `main`, `app/SurveyPrep360.kt:1052`) pass the reading and `approx = true` for a plotted
   Line and the perimeter for an area. Mode `sheet` (the saved pictures) keeps today's `dims` / `vtag` path
   untouched and passes `lineTag` neither.
2. **`app/SurveyMeasure.kt`** (new): `SpMeasureBar` (its label cell, `‹`, the two lines, the box, `D2`, `›`, and
   its other states of §1.4) and `SpMeasurePad`. Heights from `StepRoute.measureBarDp` / `padRowDp`.
3. **`pano/StepRoute.kt`**: `STEPS = lines, mark, measure, 360`; the step's name is `tx.common_measure`;
   `TAB_ROWS` loses `list`; `listView` and the list's `budgetDp` cases go. **`pano/PhaseTools.kt`**:
   `toolOf("measure") = Tool.LIST` (the prototype's row keeps its name; its KDoc says the phone's third step is
   3 · Measure since port 63); `modeFor` and `prepMode` fall to `measure`; `PREP_HONOURS` gains `READINGS`.
4. **`SPS`**:
   - **The Measure pane** is the photo pane with no drag, resize or placing. Its tap is
     `SurveyPrepTouch.hit(drawn, null, tags, x, y, tol, dp)` — `selId = null`, no chain nodes. `Hit.Tag(code)`:
     the key's owner (`prep.ownerOf`) is picked through the existing `selectMark` / `selectMeas` / `selectChain`
     and, at Survey and for an owner that is asked, the key becomes active; on the key that is already active the
     pad opens; on a mark not in the room it says `sp.measure.not_in_room`; after Survey it says
     `PhaseTools.readingRefusal`; in the last two cases the owner is picked and nothing becomes active.
     `Hit.Mark(id, "body")`: the mark is picked and, at Survey and if it is asked, `prep.nextOpen(s, e, null, …)`
     becomes active (null for a measured mark). No hit: nothing. No drawn tabs, no trash, no magnifier there.
     2 · Mark's own `Hit.Tag` branch (`hitMarks`, `SPS:2545-2549`) follows the same rule: it finds the owner by
     `prep.ownerOf`, not `markOf` alone, since a Line's, an area's and a chain's figures are tags now, and a tap on
     the key that is already active opens the pad.
   - **The bar and the pad.** The reading strip becomes `SpMeasureBar`. The pad's `MeasurePad.State` and its
     open flag are a `remember` of the dialog; the state restarts (`MeasurePad.start`) whenever the active key
     changes. Whatever leaves a dimension commits first through `MmBox.commit` (§1.4's list). While the pad is
     open in 2 · Mark or 4 · 360 the palette and the picked mark's row are not drawn and the bar draws the label.
   - **The walk.** One local function, `walkOn(after: String?)`, is the only caller of `MeasureWalk.next(`: it
     makes the next stop that is left active and the picked item follows that key's owner. `reading()` asks
     `prep.nextAfter(code)` and then, in 3 · Measure only, `walkOn(code)`; `›` and `Next ▸` there are
     `walkOn(active)`; entering 3 · Measure or changing face in it is `walkOn(null)`. `‹` is
     `MeasureWalk.previous`. In 2 · Mark and 4 · 360 `‹` and `›` are `MeasureWalk.step` over `stopsOf`.
   - **The tick**: `val haptic = LocalHapticFeedback.current` in the dialog's composition
     (`androidx.compose.ui.platform.LocalHapticFeedback`), and
     `haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)`
     (`androidx.compose.ui.hapticfeedback.HapticFeedbackType`) in the laser's `Route.Normal` branch after
     `reading(…)`.
   - **The check's sentence**: `say(MeasureText.photoSays(…))` when a reading that fails `MarkDims.check` lands
     and when a plate carrying `!` becomes active.
   - **The `all` toggle**: a third text button in the fit column (`SPS:3141-3158`), after grid, drawn in
     3 · Measure only, styled as grid is, off whenever the step is entered; it sets the painter's scope to `ALL`.
   - **Deleted**: the Show row, `listPane`, `surfaceBlock`, `SpTable`, `SpMmField` (if nothing else uses it) and
     every `listView` branch. The face tabs draw their count.
   - **After Survey**: nothing becomes active; 3 · Measure draws the label cell and `refuse.readonly.readings` in
     the bar's place (§1.8); a plate's tap and a laser reading say it.
   - A face that is not bounded shows in 3 · Measure what 2 · Mark shows there today.
5. **The tour off**: `Features.HELP_TOURS = false`; `TourFlows.shipped(tx, tours = Features.HELP_TOURS)`; the
   drawer's Help row and `TourSeed.ensure` behind the same constant (`app/MainActivity.kt:1600`, `:3645`, `:588`).
6. **`pano/SurveyRef.kt:68`** reads `sp.reference_shown_measure`.
7. **Capture 360 by phase**: the room screen's button — the `ExtendedFloatingActionButton` at
   `app/MainActivity.kt:3216`, the one opener of the capture sheet — is drawn only where
   `PhaseTools.can(PhaseTools.Tool.CAPTURE, stage)`, `stage` being what a new capture's survey prep is already
   handed (`QuoteStageSync.prepStage`: Survey for a project only on the phone, null while unread, so both show
   it). No text changes.

**Tests** (in `:pano`; `:app` is read as source): `StepRouteTest` (the steps, the labels, the tabs — four and
three); `PhaseToolsTest` (`PREP_HONOURS`, the refusal by stage, `prepMode` landing on `measure`, the strip at
every stage, and the `CAPTURE` row at all six stages, unread and not on S5); `TourFlowsTest` (nothing shipped with
the tours off, the survey flow with them on); `FeaturesTest` (the constant, and every reader of it by file and
count). And the source pins:

- no `SpTable`, no Show row and no `"both"` in `SPS`;
- `MeasureWalk.next(` once, in `walkOn`; `readingRefusal` called;
- a TYPED figure is written only through `MmBox.commit`: in `SPS` and `app/SurveyMeasure.kt` `prep.actual[` is
  assigned in one place, `reading()`, and `prep.actual.remove(` in one place, where `MmBox`'s `Commit(null)` is
  acted on. The undo's `back.actual[`, the web merge in `ReadingsSync` and a delete's sweep in `:pano` are other
  names and other files, and stay;
- `SurveyPrepTouch.hit(` called with a null `selId` in the Measure pane;
- `PhaseTools.Tool.CAPTURE` read once in `app/MainActivity.kt`;
- `LocalHapticFeedback`, and no `Vibrator`;
- the four tour anchors and `TourBindSurvey` still there; the moved counts of §1.9.

If M2 proves too large for one review, its cut is: (a) items 1–4 without the phase rule, (b) the readings' phase
rule, Capture 360 by phase and the tour off. Both still land in the same build.

**What the build note tells Amit:** measuring is on the photo now; the list is gone, and `all` in 3 · Measure
shows every figure at once; after Survey a figure cannot be changed until the phase is moved back, 3 · Measure
says so under the photo, and the room screen has no Capture 360 there — so to try measuring, use a project that is
at Survey (a new one is), or move a test project's stage back on `/projects` first; Help's tours are off for now;
the gallery pictures are unchanged.

### M3 · what leaves the phone (S)

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
| 1 | SP0 + M1 + M2 | Site progress hidden; measure on the photo; readings and Capture 360 by phase; tour off | S2 APK | built + M + L | — |
| 2 | J1 | **What he does reaches the office.** Leaving survey prep by ← queues the upload when something changed since the last one; the web line (On web ✓ / Waiting to upload / Not on web) sits on the bar, not behind ⋮; when an upload lands the phone reads S5's project list, so a new project stops reading "not on S5 yet"; and, by question 2, ✓ Save works with fewer than six surfaces bounded | S2 APK | S | question 2 for its last part only |
| 3 | J2 | **True words.** The strip's lines stop naming tools the phone does not have (vendor packs, hand-off); one set of stage names on the phone, S5's stored ones; the "Open ImageMeter → …" line after a capture goes. Amit is shown the new English on a selector when the slice starts | S2 APK | S | — |
| 4 | J3 | **The rest of L10.** 1 · Lines and the room-size pencil follow the phase (`Tool.LINES` joins `PREP_HONOURS`). Capture 360 by phase is no longer here: M2 carries it (§1.8) | S2 APK | S | M2 |
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
room → Capture 360 with its sizes → lines → marks → measure → the room on S5 → Client wish list: wish marks and
the wish list, on the phone and on the web → the vendor round and the estimate by hand, the stage moved by hand →
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
| L10 · lines, readings, room size, Capture 360 by phase | readings and Capture 360 → **M2**; lines and the room-size pencil → **J3** |
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

- **The check's threshold is a guess, and its reach is partial.** If `!` fires on ordinary estimates it will teach
  him to ignore it; the 150 mm / 15 % is widened or removed from his first rooms' evidence, not argued. What is
  not a guess is that it is not made on a mark that stands off the plane (§1.1) — so a wrong-target shot on a
  wardrobe, a loft or a beam goes unflagged, which is the price of not flagging every one of them. Two cases the
  rule cannot see, both to watch on the phone: an Other, which asks no `p` and may stand off the wall, and a point
  that hangs below its ceiling (a fan), whose position on the squared photo is off by the same geometry.
- **Readings after Survey.** From M2 a figure missed on site cannot be keyed once the stage has moved, until the
  phase is moved back, and a room cannot be captured there either. It is the approved phase rule; 3 · Measure says
  it under the photo and the build note says it too. At Client approved the stage cannot go back at all.
- **A busy face with `all` on** may shrink figures to dots at fit; zoom spreads them. The tab counts and the walk
  say what is left; the Plan tab draws each surface with its figures.
- **The bar's words are cut on the narrowest bar** (landscape, with the label cell: about 58 dp for the two
  lines). If `underside, from floor` does not hold there on the Mi A3, the first thing to give is line 1's size
  (12 → 11 sp), not a cell.
- **The screen cannot be seen here.** `:app` is compiled by CI only; plate sizes, the pad's height and the turned
  text are first seen on the Mi A3. The numbers in §1.4 and §1.6 are budgets, checked on the phone.
- **Not designed here:** the vendor document, the quote log and the estimate link beyond their one-line scope (each
  gets its own note when its slice starts); S5's arranger and line drawings (port 62 §4.1, §5); Marathi for any new
  text; site progress; the tours.
- **Held for after walk 1**, each to be asked when its screen is next touched, none decided here: whether the
  vendor-side mark states (Vendor review, Vendor approved, Vendor added) stay on the phone; whether the twelve-row
  wish list is hand-ticked, derived from the marks or removed (L9 counts them in the meantime); the two meanings
  of "scope" (a mark's state and the sixteen works of the approval); and marking a single flat photo in the quote
  journey — until then a one-window job is surveyed with a 360.

## 8. The critic

The plan critic read this design against the code and raised seventeen findings (16 Nov 2026). Each was checked
against the code at `775d33c` (and SP0 at `2fec4fe`) before anything was changed. All seventeen are confirmed in
substance. Sixteen are applied whole — four of them (1, 5, 10, 15) with a detail of the finding corrected from the
code — and finding 7 is applied in part: one piece of its fix is declined, with the reason.

| # | Finding (severity) | Checked against | What changed |
|---|---|---|---|
| 1 | The Mark sheet does not open from a code on the photo; 3 · Measure had no picked mark (major) | **Confirmed.** `Hit` is `Mark`, `Tag`, `Node` only (`pano/SurveyPrepTouch.kt:104-111`); the sheet opens from the picked row's 52 dp label (`SPS:1737`), a Line's row (`:1770`) and the list (`:1526`); the code is drawn inside the box (`SPD:509`); `hit` with `selId = null` and no nodes answers `Tag` or `Mark "body"` | §1.3: 3 · Measure's picked mark and its taps, the label rule, the table row; §1.4 the label cell; §1.6 "picked" for "active"; M2 item 4. **Corrected from the code:** 2 · Mark's label reads the code over Move / an edge / `locked`, not a type name, and a full type name does not fit 52 dp — so the bar's cell reads the type's SHORT name (`SurveyPrepPalette.short`) and 2 · Mark's row is left as it is. Added: a tap on empty photo does nothing |
| 2 | The pad's starting text was undefined; read one way, the design deletes a stored reading he walks past (major) | **Confirmed.** `MmBox.commit` writes nothing only when the text equals `shownText(value)` and removes on a blank text over a reading (`pano/MmBox.kt:20-32`); `SpMmField` starts from `shownText(value)` today (`SPS:3349`) | §1.4 the pad (start text, `fresh`, every commit through `MmBox.commit`, `Next ▸` = `›` when nothing is typed, the laser discards uncommitted digits, `0` two columns wide); `MeasurePad` and `MeasurePadTest`. Added: a leading `0` is ignored and leaves a fresh pad fresh, so `0` then `Next ▸` cannot remove a reading |
| 3 | The measure bar cannot hold its two lines at 360 dp (major) | **Confirmed.** Today's strip is `▶ FD1 width`, two lines, beside a 72 dp box and `D2` (`SPS:1811-1817`); the landscape column is 318 dp (`SPS:2022`); the longest measure words are 28 characters (`measure.right_corner` + `measure.to_centre`) | §1.4: line 1 is the measure's words alone, line 2 the progress, the check's sentence goes to the message line; the width table (with the label cell of finding 1: 80 / 58 dp for the lines, which the finding foresaw); `MeasureText.label` / `cell`; `StepRoute` constants and the landscape-column test; §7's risk |
| 4 | The pad, the palette and the picked row do not fit together (major) | **Confirmed.** Landscape: 324 − 32 − 56 − 24 = 212 dp (`SPS:2000-2038`, `StepRoute.tabRowDp`, `stripDp`); the four together need 278 | §1.4: the pad opens only when asked; in 2 · Mark and 4 · 360 it takes the palette's and the picked row's place; its open flag is a `remember` of the dialog |
| 5 | Deleting the list removed the only way back to a read Line, area or chain reading (major) | **Confirmed for a chain and for 3 · Measure.** Picking a chain makes only an OPEN key active (`selectChain`, `SPS:1001`; `Prep.nextOpen`, `pano/SurveyPrep.kt:1266-1284`); an area's tag and row show m² only (`pano/SurveyMeas.kt:287-303`); `previous` had no meaning for `before = null` | §1.5 "Every reading can be re-opened from the photo", moved from M3 to M2 (painter item 1, and 2 · Mark's `Hit.Tag` branch by `ownerOf`); the area's tag with its perimeter (§1.2, `lineTag`); `previous(before = null)` / `next(after = null)`; a finished face keeps `‹`. **Corrected from the code:** a fresh pick of a Line or an area in 2 · Mark makes its key active even when read (`SurveyMeas.pickActive`, `pano/SurveyMeas.kt:139-140`), so there the list was not the only way back; in 3 · Measure, which picks neither by its body, it would have been |
| 6 | "All dimensions on the annotation" was deferred to M3 while the list goes in M2 (major) | **Confirmed** against the design and Amit's words; `Scope.ALL` was already M1's | §1.6, M2 item 4 and the slice table: the `all` toggle is in M2; M3 keeps only the saved pictures. Added with it: two keys (`sp.measure.all`, `sp.measure.aria.all`; `common.all` is "All", capitalised, beside `fit` and `grid`), and the fit / grid / all column as a box plates keep clear of. `DimLayoutTest`'s `ALL` case |
| 7 | After Survey the measuring step shows nothing and says why only on a tap (major) | **Confirmed.** `Tool.READINGS` is `[1,0,0,0,0,0]` and `CAPTURE` `[1,0,0,0,0,1]` (`pano/PhaseTools.kt:105`, `:112`); no app source reads `Tool.CAPTURE`; port 62's L4 review, finding 2, records the wish-born room and leaves this gate open to be brought forward | §1.8: 3 · Measure's standing line; Capture 360 by phase in M2 (item 7), J3 reduced; the build note. **Declined in part:** the standing line is NOT drawn under the picked mark in 2 · Mark and 4 · 360. After Survey those steps are for marking wishes; a two-line sentence under every picked mark would cost about 28 dp of the photo on the step whose job is placing (finding 4's own concern) and would stand there every time, wanted or not. There the refusal stays on the tap. Also named: at Client approved the row shows Capture 360 and the sentence's "move the phase back" does not apply |
| 8 | The `!` check would fire on every wardrobe, loft, column, beam and bed (major) | **Confirmed.** The types whose `p` is a projection, a depth from the wall, a drop or a height (`pano/SurveyPrep.kt:885-903`, `pano/MarkTypes.kt:377-389`) stand off the squared plane, and the perspective error is systematic | §1.1 rewritten; `MarkDims.inPlane` and the classification test with both sets by name; §7's risk, with the two cases the rule cannot see (an Other, a hanging point) |
| 9 | What `‹`, `›` and the progress line do in 2 · Mark and 4 · 360 was not said (major) | **Confirmed.** After a mark's last reading nothing is active (`reading()`, `SPS:492-497`, `Prep.nextAfter`); 4 · 360 has no open face | §1.4 "Where the bar is": inside the picked item only, wrapping, never changing the selection; line 2 counts `sel.s`; `MeasureWalk.stopsOf` / `step` |
| 10 | A derived chain segment and total would count as "to measure" for ever (minor) | **Confirmed.** `openKeys` leaves out only a laser total (`pano/SurveyChain.kt:182-185`); `vals` derives a segment and a total (`:153-166`) | §1.5 "What counts as left"; `MeasureWalk.left` and `Count`; three test cases. **Corrected from the code:** a laser total is not a stop at all and takes no reading (`totalReadOnly`, `:188`), so only the derived segment and the derived total are "still reached by a tap or `‹`" |
| 11 | A mark not in the room: "takes no tap" against "said on a tap"; wrong words for a Vendor-added mark; a held reading undefined (minor) | **Confirmed.** `SheetMarks.surveyed` is false for any `born` but absent or `exist` (`pano/SheetMarks.kt:53-57`), so Vendor added too | §1.2's paragraph; the key's English. The tap also picks the mark, by the same rule as every plate (finding 1), so its label can open the Mark sheet |
| 12 | The `lineTag` change could alter the saved pictures in M1 (minor) | **Confirmed.** One `lineTag` call serves the screen and FaceWriter's `sheet` pictures (`SPD:669`), and two tests pin today's text (`MeasTest:358`, `SurveyDrawMarathiTest:84`) | M1's `lineTag` with two defaulted parameters; §1.9; M2 item 1 names the two screen callers, the 360's (`app/SurveyPrep360.kt:1052`) included |
| 13 | SP0 should be merged before M1 (minor) | **Confirmed.** `git diff 775d33c..2fec4fe -- android/i18n`: two lines at the end of `ported.txt`, one row in `literal-counts.tsv` | §4's first paragraph and the slice table |
| 14 | The walk's order was not exact (minor) | **Confirmed** against the design: `x0` is a double and no tie was defined | §1.5's four rules; the two near-tie cases in `MeasureWalkTest` |
| 15 | In 2 · Mark the plates must keep the picked mark's tab clearance (minor) | **Confirmed.** `hand` at `SPD:813`, `widthTag` / `heightTag` at `pano/SurveyPrepTouch.kt:53-59` | §1.6's last paragraph; `DimLayout`'s `PICKED` scope and its test. **Corrected from the code:** `trashSpot` is given the tags' TAP boxes (`tags.map { it.box }`, `SPS:3057`), not their drawn boxes; the design says tap boxes |
| 16 | Three small points a `:app` builder cannot check here (minor) | **Confirmed.** (a) every tab is `weight(1f)` in its row (`SPS:1301-1313`); (b) no haptic call and no vibrate permission exist in the app (`AndroidManifest.xml`); `HapticFeedbackType.TextHandleMove` has been in Compose UI since 1.0 and the app's BOM is `2024.12.01` (`app/build.gradle.kts:126`) — not resolvable in this container, so CI is what proves the import; (c) the laser writes `prep.actual[code] = v` in `reading()` (`SPS:493`) and `ReadingsSync` merges into the same map | (a) §1.3; (b) §1.4 and M2 item 4, with the imports; (c) M2's source pin reworded and scoped to `SPS` and `app/SurveyMeasure.kt` |
| 17 | Three of the journey reader's decisions were dropped without a word (minor) | **Confirmed** against the design, and each topic exists in the code (the vendor states in `pano/MarkLife.kt:43-45`, the twelve services in `pano/WishList.kt`, `scope` as a state and as the sixteen works, the single photo's capture). The reader's own list is not in the repository, so its numbering is the finding's | §7 "Held for after walk 1" |

## 9. M1 as built (16 Nov 2026)

Slice M1 is built: the pure models in `:pano`, nothing on screen. `port63-measure` took `port63-sp0` first, by a merge
(`9d619ae`); the `:pano` suite after the merge and before M1 was 87 classes, 1,249 tests, 0 failures. With M1 it is
93 classes, 1,304 tests, 0 failures (55 more: `MarkDimsTest` 11, `DimLayoutTest` 15, `MeasureWalkTest` 7,
`MeasurePadTest` 9, `MeasureTextTest` 5, `i18n/MrPendingTest` 5, two in `StepRouteTest`, one in `MeasTest`). `:app` was
not compiled here and nothing has been seen on a phone. 1,924 keys (13 added, their Marathi pending); 1,853 accessors.

What slice M2 calls, as it is in the code (the KDoc of each file is the full account):

| File | What M2 uses |
|---|---|
| `pano/MarkDims.kt` | `of(s, e, a, b)` → `Dim(key, code, role, what, est, x1, y1, x2, y2, line, known)`; `asked(e)`; `inPlane(s, e, a, b)`; `value(actual, s, e, code, est, a, b)` → `Value(text, state, check)`; `check(read, est, inPlane)`; `done(prep, s, e, a, b)`; `shown(value, role, active)` (the plate's text with `▶` and `⊥ `); `style(state, active)` → `Style(ink, bold, fill, fillAlpha, outline, dashed)`; the glyphs `ACTIVE_GLYPH` `DEPTH_GLYPH` `CHECK_GLYPH` `DONE_GLYPH`; `LINE_COLOUR`, `LINE_ALPHA`, `CHECK_COLOUR` |
| `pano/DimLayout.kt` | `mark(actual, s, e, a, b, picked, tabs, active, toPx, u, dp, textW, live = null)` builds one mark's input (the painter passes its text measurer, in px, and `MeasurePad.live(state)` as `live`); `place(marks, scope, width, height, keep, u, dp, times)` → `Out(plates)` with `of(code)`, `tags`, `dots`, `active`, `activeOff`; a `Plate` has `fig` (with `text`, `state`, `check`, `active`, `typing`, `fs`, `line`, `span`), `how` (`ON_LINE`, `OFF_LINE`, `HAND`, `CHIP`, `COMPACT`, `DOT`), `box`, `tap`, `turned`, `leader`, `arrowsIn`, `whole`, `off`; `textLeft(box, dp)` / `baseline(box, dp)`; `shows(scope, picked, role)`; `textSize(picked, dp)`, `plateW(textW, dp)`, `plateH(fs, dp)`; `onCanvas(fig, width, height)`, `clip`; `tabBoxes(box, u)`; the constants `SurveyPrepDraw.dims` held (the plate's now `PAD_DP`, `OVER_DP`, `UNDER_DP`, `RADIUS_DP`) |
| `pano/MeasureWalk.kt` | `stops`, `left`, `next`, `previous`, `count` → `Count(read, total)` with `left` and `done`, `room`, `nextFace` → `Face(s, left)`, `stopsOf`, `step`, `marks`, `FACES` |
| `pano/MeasurePad.kt` | `start(stored)`, `press(state, Key.Digit(n) / Key.Back / Key.Clear)`, `live(state)`, `commit(stored, state)` (= `MmBox.commit`) |
| `pano/MeasureText.kt` | `label`, `cell` → `Cell(code, under)` with `text`, `progress`, `faceDone`, `nextFace`, `roomDone`, `photoSays`, `notInRoom` |
| `pano/StepRoute.kt` | `measureBarDp(land)`, `padRowDp(land)`, `PAD_ROWS`, `LAND_TOPBAR_DP`, `LAND_COLUMN_DP` |
| `pano/SurveyMeas.kt` | `lineTag(code, kind, pts, reading = null, approx = false)` |

Decided by Claude while building, beyond what §4 says; each is in the code's own comments and held by a test:

1. **A plate that finds no room beside its line tries once more, one plate further out, before it becomes a dot**
   (rule 3 of §1.6, widened). Without it a picked electric point 180 mm from a corner and 300 from the floor had one of
   its two figures as a dot on the 340 dp photo, with room an inch away. With it, the example wall shows no dot in
   3 · Measure whichever mark is picked and whichever of its dimensions is active.
2. **With `all` on the example wall places all 19 with 2 dots at fit** (the corner point's two figures), pinned as
   `DimLayoutTest.ALL_DOTS`. Zoom spreads them, as §7 says. *(The review of M1, below: 2 is the round test canvas's
   count; at the phone's own numbers it is 1, `PHONE_ALL_DOTS`.)*
3. **A `p`'s chip on a mark that shows its tabs stands off the corner by the tags' own clearance** (8 units, 6 dp at
   least), not 4: at 4 every one of its four places touched the corner's tab, and a picked door's chip was a dot.
4. **In 2 · Mark the hand-anchored `w` and `h` tags give way like any plate when their place is taken.** On the example
   wall the switch plate's code stands exactly where the picked door's `h` tag goes; the tag then sits upright a
   quarter along its line. In 0.3.332 the tag was simply drawn over the code. If Amit prefers that in 2 · Mark, M2
   passes no code boxes in `keep` for `Scope.PICKED`: nothing else changes.
5. **A dimension is "too short for its plate" under plate + 8 dp** (the small box's own figure), on the line a plate
   stays half of that clear of each end, plates stay 2 units inside the photo and 1 off each other, and a compact
   plate stands 10 units off its box. These are first figures, seen on the Mi A3 with M2.
6. **`MrPendingTest` reads the tables**, so `I18nTestHygieneTest` names it among the files that may, for its one
   property; it also holds that every pending key is a phone text and that `TxRenderTest`'s leak scan skips nothing
   but the laser test's log lines and the list.
7. **The new test names are plain ASCII.** A test whose name holds `·` and whose body holds a local function or a
   lambda compiled to its own class could not be written to disk by this container's Kotlin daemon (the class file
   is named after the test); older tests with `·` in their names have no such body.

### The review of M1 (16 Nov 2026)

A reviewer read `f9af214` and reported four findings on `DimLayout`. Each was reproduced here first (a probe against
the reviewed commit), all four were confirmed, none declined. After the fixes the `:pano` suite is 93 classes, 1,311
tests, 0 failures (`DimLayoutTest` 15 → 22). `:app` is still not compiled here and nothing has been seen on a phone.

1. **Off-canvas marks' figures were slid back onto the canvas edge once the photo was zoomed or panned (major) —
   confirmed.** The probe: the example wall zoomed 3 times on the door gave, with `all` on, 19 figures of which 12
   were dots, eight of them stacked on one spot in the canvas's corner under the door's chip. `place` clamped every
   box into the canvas without asking whether the figure was on it. **Fixed:** a figure whose dimension is wholly off
   the canvas is not placed (§1.6). "On the canvas" is asked of the dimension's own SPAN (`Fig.span`, the mark's edge
   or the gap it measures), not of its drawn line: a mark against the ceiling or the right wall has its `w` or `h`
   line 6 units outside the canvas at fit and must keep its figure — asking the line would have dropped those. The
   active figure and the picked mark's are still slid into view and say so (`Plate.off`, `Out.activeOff`,
   `Out.active`). A dot sits on the part of its line the canvas shows and is never slid in from outside.
2. **The layout was built and tested at a unit the phone does not have (major) — confirmed.** `u` is canvas / 480
   = 0.71 dp on the Mi A3, so "12 / 10" in units was 8.5 dp / 7 dp, not the 12 sp / 10 sp of §1.6, and the test
   canvas (1 px a dp AND 1 px a unit) pinned results for plates 1.4 times larger than the model would have drawn.
   **Decided by Claude: the design's sizes, in dp** (the reviewer's option a) — a 7 dp figure on every unpicked mark is
   a third under the design's 10 sp, units shrink again in landscape while §1.6 says plates keep a constant size on
   screen, and what Amit asked for is figures he can read on the annotation. So the plate's text and padding
   are dp (`TEXT_PICKED_DP` 12, `TEXT_OTHER_DP` 10, `PAD_DP`, `OVER_DP`, `UNDER_DP`), the lines stay in units, and the
   corner and floor lines stand off by build 332's own 12 units (`LINE_FS_U`) whether or not the mark is picked, so a
   line no longer moves when its mark is picked. The hand-anchored `w` / `h` tags take `widthTag` / `heightTag`'s
   anchor and the plate's own dp box. **Re-pinned at the phone's real numbers** (680 px, 2 px a dp, 680 / 480 px a
   unit) in `the example wall at the phone's own numbers`: as 3 · Measure opens, 6 to 9 plates and no dot whichever
   mark is picked and whichever dimension is active; with `all` on, all 19 with ONE dot (the corner point's floor
   line), `PHONE_ALL_DOTS`. The round canvas keeps its 2 (`ALL_DOTS`). Departure 1 above (the second reach) was
   checked again at the phone's numbers: without it a picked corner point has a dot there too. **For Amit on the
   phone, with M2:** the figures are 12 dp on the picked mark and 10 dp on the others — larger than today's 8.5 dp
   tags; if they crowd a real wall the two constants are the thing to turn.
3. **A dot could sit under another figure's plate, and the test helper exempted dots (minor) — confirmed.** On the
   round canvas with `all` on, the corner point's floor-line dot lay inside another point's plate. **Fixed:** a dot
   looks for a clear spot — the middle of what the canvas shows of its line, then along it a quarter dot at a time,
   then (a line wholly under plates: two points at one height share their corner line) just beside it within two
   plate heights; a `p`: the mark's corners — and its box joins the placed boxes, so no later plate covers it. Only
   with none of those does it fall back to its first spot. `sound()` now fails on any dot under a plate or on
   another dot, on every layout it checks.
4. **`mark` could not carry the pad's live digits onto the active plate (minor) — confirmed** against §1.4.
   **Fixed:** `mark(…, live = MeasurePad.live(state))`. Digits: the active plate reads `▶` and the digits (a `p`'s
   after its `⊥ `), is sized from that text, is drawn as a reading and carries no `!`; `Fig.typing` says so, and
   `Fig.measured` stays what is stored. Null (untouched) or empty (an emptied pad) keeps the figure the plate had.

Not changed by the review: the reading keys, the record, the texts (no key added or changed), `MarkDims`,
`MeasureWalk`, `MeasurePad`, `MeasureText`.
