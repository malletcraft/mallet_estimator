# DESIGN-quad — a mark with four free corners

S2 APK (`mallet_estimator` → `android/`), with the slices S5 QUOTE and S4 MDT need. Written 16 Nov 2026 (IST), branch
`port63-quad`, on `7c49835` (port 63 M1 + M2 over SP0; `:pano` 95 classes, 1,352 tests).

**Basis.** Three readers' reports (the pure code and a mark's box; the screen after M2; S5 and S4), and the code read
again here wherever this file decides something. Nothing was run for this file: no gradle, no S5 test, nothing seen on
a phone. Every "decided by Claude" is judged by Amit on the phone.

Paths: `pano/…` = `android/pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/…`, `app/…` =
`android/app/src/main/kotlin/com/malletcrafts/interiorestimate/…`.

## 0. What Amit asked for

16 Nov 2026, ~16:20 IST, after a balcony window job surveyed with the phone:

> "while marking window, I realised that it can draw only rectangular windows. there are scenarios when i need to draw
> trapezoidal windows as well like one just exported."

The opening was a trapezoid: two uprights of different heights, a level top, a sloping bottom. A Window mark is a box
(`x0 x1 y0 y1`), so he drew it as four Lines and keyed a reading on each. His two answers the same afternoon:

1. **"Four corners"**: every box mark can become a four-corner shape. It starts as a rectangle; any corner is dragged;
   a side close to upright or level snaps straight; **each of the four sides carries its own figure**. Not "slope
   only", not a polygon of any number of corners. Curved tops are not decided.
2. **"After the measuring screen"**: the next phone build after port 63's M1 + M2, before the journey slices.

The example used all through this file is made up (never the job's own figures): front wall 3600 × 2700 mm of project
`PROJ-T1`; window `FW1` with uprights 1550 (right) and 780 (left), a level top of 2370, so a sloping bottom.

**Three places where this design departs from what the brief or the readers expected.** Each is argued where it is
decided; they are listed here so nobody finds them by accident.

- The corners are stored as **parts of the mark's box**, not as mm on the surface (§1.1).
- A reading the mark stops asking for is **kept**, not carried or cleared (§2.6).
- The saved `-survey.jpg` picture of a four-corner mark shows its outline but **no side figures until port 63's M3**
  (§3.9).

## 1. The record

### 1.1 The one new key: `quad`

A mark may carry `quad`: its four corners, each as the part of the mark's box where it stands.

```
"quad": [[px, py], [px, py], [px, py], [px, py]]      bottom-left, bottom-right, top-right, top-left
px = (x − x0) / (x1 − x0)     0 at the box's left edge, 1 at its right
py = (y − y0) / (y1 − y0)     0 at the box's bottom edge, 1 at its top (y up, as every mark has)
```

The corner in mm is `x0 + px·(x1 − x0)`, `y0 + py·(y1 − y0)`. The order is the one the reference rectangle's `q`
already has (`pano/SurveyRef.kt`), and it is counter-clockwise. The corners are called `q0 q1 q2 q3` below.

The example: box `x0 600, x1 2970, y0 900, y1 2450`; the left upright is 780, so its foot is at 2450 − 780 = 1670.

```
{"id": "…", "t": "W", "code": "FW1", "x0": 600, "x1": 2970, "y0": 900, "y1": 2450,
 "quad": [[0, 0.496774], [1, 0], [1, 1], [0, 1]]}
```

Rules of the key:

- **Stored only when the shape is not its box.** `[[0,0],[1,0],[1,1],[0,1]]` is never written: the key is removed
  instead. A mark without `quad` is the rectangle it always was, in every reader.
- **The box stays, and it is the shape's bounding box by construction.** Some corner has `px` 0, some `px` 1, some
  `py` 0, some `py` 1. So S5 `/projects`, S4's "Room from survey", an older phone build, the plan's wall bands and
  the 360 keep reading a rectangle that contains the shape exactly.
- Parts are written to 6 decimals (under 0.01 mm on any wall); a corner on a box edge is exactly 0 or 1. Corners are
  placed to 0.1 mm before the parts are taken, as a Line's end is.
- It lives in `Mark.extra`, like `sub`, `life` and `locked`. `SurveyPrepFile.MARK_KEYS` does not change, so the file's
  writer, the key order, undo, `Surface.copy` and Start over need no change. The map is replaced, never changed in
  place (`SurveyPrep.withKey`).

**Decided by Claude: parts of the box, not mm on the surface.** The brief expected absolute mm, and so did the S5
reader. Absolute corners are a second statement of where the mark is. Every writer of the box would have to move
them too (`moveMark`, `snapMark`, `placeAt`, `duplicate`, S4 when a reading repositions the box), and an older build
keeps an unknown key verbatim while its move changes `x0…y1` only. Three readers (phone, S5, S4) would then each need
the same rule for a box and corners that disagree. With parts of the box there is one statement of position and
extent (the box) and one of shape (`quad`):

- a body move, a nudge, an edge snap, a clamp to the face and a duplicate need no corner code at all;
- an older build that moves the box carries the shape with it; one that resizes the box stretches the shape, and
  upright sides stay upright and level sides level;
- whether a `quad` can be read does not depend on the box (§1.2), so no stretch by any reader can break it;
- S5 draws it in a `0 0 1 1` viewBox with no arithmetic, and S5's `coords` rule passes it unchanged.

What it costs: the key is not readable as mm by eye, and S4 needs one multiplication. If Amit or a builder overrules
this, the change is confined to `Quad.read`, `Quad.set` and S5's reader; nothing else in this file moves.

### 1.2 When a `quad` can be read — one rule, the same on the phone, S5 and S4

A `quad` is read only when all of these hold. They are tested on the parts, so the box plays no role.

1. It is a list of exactly four items, each a list of exactly two finite numbers from 0 to 1.
2. It spans its box: the least `px` is 0 and the greatest 1; the least `py` is 0 and the greatest 1.
3. Each side keeps its direction: `q1.px > q0.px` and `q2.px > q3.px` (bottom and top run left to right);
   `q3.py > q0.py` and `q2.py > q1.py` (left and right run upward). This is what makes "left side" mean one thing on
   every device.
4. It is convex and uncrossed: at every corner the turn from one side to the next is to the left
   (`cross(q[i+1] − q[i], q[i+2] − q[i+1]) > 0` for all four, the indices taken round the four).
5. The mark may have one: it is not a point (E P F PLB LV) and not a Note (`!SurveyPrep.isSpot(t)`).

Anything else is not a shape:

- **`[[0,0],[1,0],[1,1],[0,1]]` says what the box says.** It is read as no key and dropped quietly at decode.
- **Any other unreadable `quad`: the mark is its box, everywhere, and the phone says so.** `SurveyPrepFile.decode`
  drops the key from that mark and adds a problem, `sp.file.x_mark_x_shape_unreadable`. That takes the existing
  salvage path: the file is copied aside before anything is saved, and saved clean on open. S5 and S4 read the same
  mark as its box by the same five tests. There is no second answer, and no build guesses a shape.

**A box and corners that disagree cannot exist.** That is the answer to "an older build moved the box and left the key
in `extra`": the shape went with the box.

A later shape (a curved top, more corners) takes a NEW key. `quad` never gains a second meaning.

### 1.3 Which marks, and what stays impossible

Every mark that is not a spot may have `quad`, on all six surfaces: Door, Window, Opening, Niche, Column, Beam, Loft,
Switch plate, Step, Other, Grill, AC unit, Ceiling drop, Curtain track, Carpentry unit, Sanitary ware, Defect. A new
mark always starts as a rectangle with no key (`defaults`, `placeAt`, `PhaseTools.place` are untouched).

An edit is refused, and the corner stays where it last was, when the result would have:

- a corner off the face (it is clamped to the face first);
- a side that runs less than `SurveyPrep.MIN_SIZE` (20 mm) in its own direction (rule 3, in mm);
- a corner less than 20 mm outside the line joining its two neighbours. That one test forbids crossed sides, a corner
  turned inward and a shape flattened to a triangle.

Never possible: three or five corners, a curved side, a `quad` on a point or a Note.

## 2. The figures

### 2.1 Four sides, each its own figure, each its own key

A four-corner mark is measured by its four sides. Side `i` runs from corner `i` to corner `i + 1`.

| Key | Side | Corners | Words on a wall | Words on the floor or ceiling |
|---|---|---|---|---|
| `bottom` | 0 | `q0`–`q1` | bottom side | back side |
| `right` | 1 | `q1`–`q2` | right side | right side |
| `top` | 2 | `q2`–`q3` | top side | front side |
| `left` | 3 | `q3`–`q0` | left side | left side |

- Reading keys: `FW1.bottom`, `FW1.right`, `FW1.top`, `FW1.left`. Letters only, 3 to 6 long, so S5's `KEY_RE` passes
  them unchanged, and none is taken (`w h l r s t f k p d cl cr cs ct cf ck len per`, a chain's digits and `t`). None
  is a two-letter name starting with `c`, which `MarkDims` and `SurveyPlan` read as a point's centre key.
- **Whole words, decided by Claude.** `FW1.left 780` reads without a table in D1, in d1-peek and in S5's list. Until
  slice Q3, S5 already lists such a reading under its mark by its bare name, with a box for staff.
- The photo's estimate of a side is the distance between its two corners in face mm.
- **`w` and `h` are not asked** on a four-corner mark. They mean the box's overall width and height on every reader,
  and a trapezoid has neither as a thing to laser. The four sides are always asked, also on a type whose rectangle
  asks only a width (Column, Curtain track) or neither (a wall's Beam).

### 2.2 What four sides fix, and the diagonal

Said plainly: **four side lengths alone never fix a four-sided shape.** It racks like a frame with no brace. What
closes it is a square corner or a diagonal.

A side is *straight* when the phone stored it so: the left or right side is upright when its two corners have the same
`px`; the top or bottom is level when its two have the same `py`. A corner is *square* when both its sides are
straight, so a mark has a square corner exactly when it has an upright side and a level side.

| The mark has | The figures fix | Asked |
|---|---|---|
| a square corner, three straight sides (the trapezoid) | everything, with one figure to spare | four sides; the spare is the check (§2.4) |
| a square corner, two straight sides | everything, exactly | four sides |
| no square corner (two parallel straight sides, one, or none) | nothing until braced | four sides and one diagonal |

The diagonal is `diag`, from `q0` to `q2` (bottom-left to top-right; `FW1.diag`). Four sides and a diagonal are two
triangles, which fix the shape. Its tilt on the wall is then a straight side's if it has one, else the photo's. The
diagonal is asked only when needed, so a window fabricator gets the fewest figures that build the opening.

### 2.3 Where the mark sits, and what else its type asks

The position figures keep their keys and their exact meaning. `l r s t f k` run to the box's edge on every reader
today (S4 moves the box by them), and on a four-corner mark the box's edge is its outermost corner on that side. That
is the same distance, so by the c-key rule no new name is needed.

- The line on the photo ends at the corner that touches that box edge. If two do (the side is straight), it ends at
  their midpoint, which is where a rectangle's line ends today.
- When that side of the shape is not straight, the words gain `measure.to_nearest_corner` (", to nearest corner"), as
  a point's gain ", to centre". A Beam's `d` gains nothing: it runs to its lowest corner, and the line shows which.
- Which offset is asked (`l` or `r`, `s` or `t`, `k` or `f`), and the rule that a mark touching a boundary asks none
  there, are the box's as before.
- `p` (reveal depth, projection, drop) and `d` are asked as the type asks them. The `p` chip hangs at `q1`.

So the checklist of a four-corner mark is: `bottom`, `right`, `top`, `left`; `diag` when it has no square corner;
then everything its type's rectangle asks except `w` and `h`, in the type's order.

### 2.4 The checks

**The photo check is unchanged** and runs per figure: a side's reading against its photo length, more than the larger
of 150 mm and 15 %, on marks that lie in the squared plane only (`MarkDims.inPlane`).

**One new check: the three straight sides against the fourth.** When exactly three sides are straight and all four
are read, the sloping side is worked out from the other three readings:

```
derived = √( joining² + (parallelA − parallelB)² )
```

`joining` is the side opposite the sloping one; the parallel pair are its two neighbours. The check fails when the
sloping side's reading differs from `derived` by more than the larger of 25 mm and 1 % of `derived`
(`Quad.CLOSE_MM`, `Quad.CLOSE_PART`). It compares readings with readings, so it also runs on a wardrobe or a loft,
where the photo check cannot.

It is shown exactly as the photo check is: an amber `!` after the sloping side's figure, and the message line when
that reading lands or its plate becomes active, `sp.measure.sides_say_x`: "The other sides make this ≈2466 mm. Check
the readings, or the corners." When both checks fail on one plate, this sentence is the one said. Nothing is stored.

25 mm is Claude's starting figure. A corner that is 1° out of square moves the example's slope by about 13 mm, so the
check should stay quiet on an ordinary opening and fire on a wrong target, two figures typed in each other's place, or
a side drawn straight that really slopes.

### 2.5 The example, to the last figure

`FW1` on the 3600 × 2700 front wall, corners in mm: `q0` (600, 1670), `q1` (2970, 900), `q2` (2970, 2450),
`q3` (600, 2450).

| Key | What the bar says | Photo's estimate | Why |
|---|---|---|---|
| `FW1.bottom` | bottom side | ≈2492 | √(2370² + 770²) = 2491.9 |
| `FW1.right` | right side | ≈1550 | upright |
| `FW1.top` | top side | ≈2370 | level |
| `FW1.left` | left side | ≈780 | upright |
| `FW1.l` | from left corner | ≈600 | 600 ≤ 630, the nearer corner; the left side is upright, so no suffix |
| `FW1.s` | sill, from floor, to nearest corner | ≈900 | to `q1`, the lowest corner; the bottom slopes |
| `FW1.p` | reveal depth | `?` | site only |

Left, right and top are straight; corners `q2` and `q3` are square; no diagonal is asked. Seven figures.

- Read as 2495, 1550, 2370, 780: `derived` = 2491.9, off by 3. No `!`.
- With the left upright typed 870 by mistake: `derived` = √(2370² + 680²) = 2465.6, off by 29.4, over the 25 mm
  limit. `!` on the bottom side, and "The other sides make this ≈2466 mm. Check the readings, or the corners."

A fabricator builds it from 780, 1550, 2370 and 2495, places it by 600 from the left corner and 900 from the floor at
its low corner, and takes the reveal.

### 2.6 A reading the mark stops asking for is kept

When a rectangle becomes four-cornered its `w` and `h` readings are no longer asked, and when a four-corner mark goes
back to a rectangle its side readings are not. **Decided by Claude: they stay in `actual`, untouched**, by the rule
the record already has. A mark moved to touch the floor stops asking `s` and keeps the reading, and S5 lists such a
reading under its mark "because it is still a measurement somebody took" (`app/src/asis_view.js`, `rowsFor`).

- The phone does not show, walk or count a reading nobody asks (`Prep.keysOf` decides all three).
- `w` and `h` keep their meaning, the box's overall size.
- A side reading comes back if the mark is made four-cornered again. The photo check flags it if the shape differs.
- Rejected: carrying `w` to a level side, or clearing. Both need every finished edit to pass one more step, to save
  two re-shoots in a rare order of work (measure first, reshape after).

## 3. On the phone

### 3.1 How a box becomes four corners, and goes back

**The long-press menu gains one row**, on a mark that may have `quad`, after Duplicate:

`Delete | Duplicate | Four corners | Lock | Lock all … | Cancel`

- On a rectangle the row is `sp.shape.four_corners`, "Four corners" (action `SurveyPrepEdit.SHAPE_FREE = "free"`).
- On a four-corner mark it is `sp.shape.rectangle`, "Rectangle" (action `SurveyPrepEdit.SHAPE_BOX = "box"`).
- The row is hidden where the phase holds the mark read-only (`PhaseTools.editable`), like Lock all. On a locked mark
  it is drawn and the act answers the lock's own words, as Delete does.

This is the cheapest control the screen has: `SpCtxMenu` draws any list, and a long press on a mark or on one of its
handles already opens it. The corner tabs cannot do it by themselves, because `lt rt lb rb` are today's two-edge
resize of every rectangle.

**"Four corners" changes nothing in the record.** The shape is still its box, and the key is stored only when it is
not. The act picks the mark and sets one screen state, `freeId`, the id of the mark whose corners are free. It says
`sp.shape.drag_corners`: "Drag a corner. A side near upright or level snaps straight." No undo step.

- A mark's handles are four free corners when it has `quad`, or while it is the `freeId` mark
  (`Quad.free(e, freeId)`). Otherwise they are the eight tabs.
- `freeId` is cleared when the selection leaves that mark. A rectangle whose corners were freed and never dragged is
  simply a rectangle again.

**"Rectangle"** removes `quad` (`Quad.clear`), in one undo step. The box does not move: the mark becomes its bounding
rectangle. The selection goes back to the whole mark, and `freeId` is cleared.

### 3.2 Dragging a corner

- **Handles.** Four white discs of radius 5.5 dp (`SurveyGuide.END_R_DP`) ringed in the mark's colour: round, as a
  Line's ends are, where a rectangle's tabs are square. They are pressed within the tabs' own reach, 16 dp. On the
  Mi A3 (360 × 780 dp, photo about 340 dp wide) the example window is about 224 dp wide and its short upright 74 dp,
  so the four handles stand well apart. Two corners closer than 32 dp on screen share their reach, the nearer wins,
  and zooming separates them.
- **Parts.** The four corners are the parts `q0 q1 q2 q3`. No name holds `l`, `r`, `t` or `b`, which `moveMark`,
  `snapMark` and the picked mark's row test by letter. The row then reads `FW1` over "corner" (`sp.corner`, existing)
  and shows the ◀ ▶ ▼ ▲ nudges of 1 mm.
- **A tap on a handle moves nothing** (`SurveyGuide.EndGate`), as on a Line's end.
- **The magnifier** (120 dp, 3×) centres on the corner while it is dragged, and draws the sloping outline.
- **The snap.** While dragging, the corner is taken onto its upright neighbour's `x` when the side between them is
  within 5° of upright (`SurveyMeas.SNAP_DEG`) and the corner is within the snapping distance of that line. The same
  holds for its level neighbour's `y`. The snapping distance is a Line end's: 25 mm, or 14 dp on screen when that is
  more (`SurveyGuide.endTol`), about 150 mm on the example wall at fit and 37 mm zoomed four times. On an axis no
  side fixed, the corner snaps onto the face's edge within 25 mm (`SurveyPrep.SNAP`).
- **How the snap is seen.** The guide rays run through the corner across the face, as they do through an area's
  corner. The ray along a side that has snapped straight is drawn bold. Both are bold on a square corner.
- **How it is escaped.** Zoom in: the distance shrinks to its 25 mm floor, so a run-out over 25 mm can be drawn. The
  last millimetres go by the nudges, which never snap. A slope too small to see on the photo still shows in the
  readings, and the check of §2.4 fires if the sides do not agree with a side drawn straight.
- **A corner that would break the shape stops** (§1.3). A locked mark does not move and says so, and a mark of an
  earlier phase says the phase's words: both through the branches the press already has.
- One undo step per drag, as any mark's.

### 3.3 Moving, resizing, duplicating, undo, lock, phases

- **Move:** a press inside the shape drags it whole. The box is kept on the face and snaps to its edges as today; the
  shape rides. The nudges on "move" are unchanged.
- **Resize of the whole shape:** a four-corner mark shows no edge tabs. The model still answers `l r t b` on it by
  stretching the box with the shape (that is what an older build does); nothing on screen calls it.
- **Duplicate:** the copy is the box and its `quad`, and nothing else. No readings, unlocked, the next code, as today.
- **Undo:** unchanged. The snapshot shares `extra`, which is only ever replaced.
- **Lock:** a locked mark's corners do not move.
- **Phases:** a corner drag is a resize and asks `PhaseTools.editable` like one. A surveyed mark is reshaped at Survey
  (and while the stage is unread) and never after. A mark born a Client wish is reshaped in the Wish list, one born
  Vendor added in Vendor review and after. This is the condition under which S5 may give a surveyed mark's `quad` to
  a vendor or a client: nothing of the wish round can be written into it.

### 3.4 What 2 · Mark draws

- The outline is the four-sided path, in the type's colour or the state's line, in all three modes of the painter
  (the photo, the magnifier, the saved picture). A Dropped mark's red stroke runs `q0` to `q2`. The dim hole round
  the picked mark is the path.
- The code, the padlock, the out-of-face frame and the floating trash keep the bounding box.
- The press: the picked mark's four corners first, in the place the eight tabs have in the hit order. A body press
  counts only inside the shape, so the empty corner of the bounding box no longer hides what is under it.
- The picked mark's figures: its sides on their sides, then position and `p`, by the rules of §3.5.

### 3.5 What 3 · Measure draws

`MarkDims` and `DimLayout` already draw whatever the checklist returns, by key. They learn two roles.

- **A side** (`Role.SIDE`) is a line parallel to its side, 6 units outside the shape, with its plate mid-span. The
  plate is upright, or turned to read bottom to top when the side is more upright than level. It is not rotated to
  the slope, and it breaks the true sloping line. The fallbacks are the ones every plate has (a quarter along, off
  the line on a leader, a dot).
- **The diagonal** (`Role.DIAG`) is a line from `q0` to `q2` inside the shape, plate mid-span.
- **Position lines** run to the corner (§2.3) and are not shifted off-centre, as a point's are not.
- Sides and the diagonal count as size: every mark of the face shows them, picked or not (`DimLayout.shows`).
- A picked four-corner mark blocks its four handles, not eight tabs. It has no hand-anchored `w` / `h` tags and no
  compact plate: a very small one falls to leaders and dots.
- **The walk** orders marks as before (by the box's left edge). Inside the mark: bottom, right, top, left, the
  diagonal if asked, then the type's own. The counts and the tabs' "to measure" follow the checklist.
- **The bar** says the side's words over the face's progress: "bottom side" over `3 of 12 measured`.
- A mark that is not in the room (a wished four-corner unit) shows its side estimates and takes no reading, as today.

### 3.6 The 360

`Pano360.markPoly` takes the four real corners in place of the box's (same order, so a rectangle is drawn as it was).
`Pano360.tabsOf` gives a free mark its four corners as `q0…q3`. The press, the drag and the snap go through
`moveMark` and `snapMark` as today; there the snapping distance is the 25 mm floor. No figures are drawn on the 360.

### 3.7 The Plan tab and the six drawings

| Drawing | A four-corner mark |
|---|---|
| Floor plan, a mark on a wall | keeps the box: the gap, niche, column or leaf is placed by the box's two ends |
| Floor plan, its label | the code and the reading of a level side that spans the box (`Quad.spanW`), else `≈` the box's width |
| Floor plan, a mark on the floor or ceiling | the real shape |
| The four elevations | the real shape; a dimension along each side, drawn 0.9 text sizes inside it; a door's head along its top side; a window's transom between the mid-points of its left and right sides |
| Elevation, under the floor | the corner offset only; no width segment |
| Elevation, up the wall | the floor or ceiling offset at the corner it runs to; a window's head as sill + the upright side that spans the box (`Quad.spanH`) when both are read, else `≈`; no `h` |
| Ceiling plan | the real shape; an opening's cross along its two diagonals; offsets to the corner |

### 3.8 What leaves the phone

- **The record:** `prep` is uploaded whole, so `quad` reaches S5 with no change to the wire. Staff are given it; a
  vendor or a client is given the box until S5 names the key (slice Q3).
- **The readings:** the new keys sync like any other.
- **The A4 sheet and the saved pictures for others:** `SheetMarks.KEPT` gains `quad`, so a surveyed opening is drawn
  in its real shape. **Decided by Claude:** a surveyed opening's shape is the room as surveyed, as its box is. S5's
  `SEEN_MARK` gains the same key in Q3, and Q3 is on stage before the phone build that carries Q2, so the phone's
  documents and the web never give two answers.
- **The SVG and DXF export:** writes any polygon already. Untouched.

### 3.9 The saved `-survey.jpg` pictures

Their figures are still drawn by the old painter (`SurveyPrepDraw.dims`), which port 63 holds byte for byte until
Amit rules on its question 1 (slice M3). `dims` draws a width only for `w` and a height only for `h`.

**So until M3 a four-corner mark's saved picture shows its true outline, its code and its position figures, and no
side figures.** This is known, and it is not built round. M3 replaces that painter with the one the screen uses, and
the sides arrive with it. Until then the side figures leave the phone on the A4 sheet and in the readings S5 shows.

## 4. S5 and S4

### 4.1 Slice Q3 — S5 `/projects`

Display only: no route, no write, no statement, no migration. `KEY_RE` does not change.

1. **The as-surveyed rule.** `SEEN_MARK` gains `quad: coords` after `locked` (`functions/_lib/asis.js`). `coords`
   passes a list of number pairs whole or not at all, so a malformed one is left out and the mark reads as its box. A
   mark born a wish is still left out whole. `README.md`, the header of `asis.js` and the RD row's key list follow
   (`id t code x0 x1 y0 y1 locked quad`; still not `sub`), as the docs test holds them.
2. **The reader** (`app/src/asis_view.js`): `quadOf(e)` by the five tests of §1.2, tolerant. Anything else reads as
   absent. The view mark gains one optional key, `quad`, only when it is read.
3. **The outline, for staff, vendor and client alike once given.** A mark with `quad` is `class="pmark pquad"` with an
   SVG inside it: `viewBox="0 0 1 1"`, `preserveAspectRatio="none"`, a polygon at `(px, 1 − py)` with
   `vector-effect="non-scaling-stroke"`. `.pmark.pquad{border:0;box-shadow:none}`. Past Existing, `outlineSvg` takes
   the polygon in place of its `<rect>` and the Dropped cross runs `q0` to `q2`.
4. **The checklist.** `measures` mirrors §2.1 to §2.3 line for line: four side rows with the corner-to-corner
   estimate, `diag` when there is no square corner, then the type's rows without `w` and `h`. `rowsFor`, `rowHtml`
   and `wireReadings` then give the Photo / Site / Diff cells, the reading boxes for staff and the PATCH with no
   change. A `w` or `h` reading the mark still holds is listed under it as before (§2.6). The list head says
   `four corners, within W × H mm on the photo`.
5. **A record without the key renders to the byte as before.** Every addition is behind `quad`. The existing fixtures
   are not edited; a new pair is added (a surveyed four-corner window; the same room after the office adds a wished
   four-corner unit).
6. **The CLAUDE.md row**, one line after T5's, in the pinned shape: Amit's words verbatim; `display only`; `drawn
   exactly as before`; the rule is the server's, `redactRecord` alone; `**The guards**:`; `**Decided by Claude in
   this slice, where the plan was silent**:`; `**What does not exist yet**: NOT DEPLOYED when this was written`; and
   that `vector-effect` is proven in Chromium only and nothing on Cloudflare.

Tests: the reader's tolerance (3 or 5 points, a word, a part outside 0…1, no span, crossed, turned inward, on a
point or a Note); the four rows and their estimates, each key through `KEY_RE`; the polygon string plain and per
state; a vendor and a client given a surveyed mark's `quad` and never a wished one's, on both stores; the docs test;
and in a browser at 1280 and 390 px, the outline, four inputs for staff, one side PATCH round trip, and a vendor's
and a client's room the same HTML before and after the office's work.

### 4.2 Slice Q4 — S4 "Room from survey" (not built here)

Until its own slice, S4 ignores `quad`, silently: a four-corner window, door, opening or niche is cut as its bounding
box (the example: 2370 × 1550, about 0.91 m² more than the opening), and the side readings are never read. The
smallest honest step can ride any S4 commit first: a warning, "FW1 has four corners: built as its bounding box". The
slice itself: read `quad` by §1.2 on cut and recess marks, turn the parts into four model points, cut that face,
use the polygon's area in the size check, and treat an overlap as an outline with a warning. Side readings move a
corner only where they fix the shape (a square corner); otherwise the photo's corners stand and the mark says
`photo`. On a four-corner mark `w` and `h` readings are not read. `VERSION` goes from 3 to 4.

## 5. The new texts

English first: each key's Marathi value is its English for now, with a row in `android/i18n/mr-pending.tsv`. Fourteen
keys, 1,924 → 1,938; the pending list grows from 13 to 27. `Tx` is regenerated, never hand-edited. No Marathi is
composed and no existing Marathi value is touched. The picked mark's row says "corner" with the existing `sp.corner`.

| Key | English | Where |
|---|---|---|
| `measure.side_bottom` | bottom side | the bar, a wall |
| `measure.side_right` | right side | the bar |
| `measure.side_top` | top side | the bar, a wall |
| `measure.side_left` | left side | the bar |
| `measure.side_back` | back side | the bar, floor and ceiling: side `bottom` |
| `measure.side_front` | front side | the bar, floor and ceiling: side `top` |
| `measure.diagonal` | diagonal, bottom-left to top-right | the bar, a wall |
| `measure.diagonal_plan` | diagonal, back-left to front-right | the bar, floor and ceiling |
| `measure.to_nearest_corner` | , to nearest corner | a fragment after a position's words, as `measure.to_centre` |
| `sp.shape.four_corners` | Four corners | the long-press menu, on a rectangle |
| `sp.shape.rectangle` | Rectangle | the long-press menu, on a four-corner mark |
| `sp.shape.drag_corners` | Drag a corner. A side near upright or level snaps straight. | the message line, after "Four corners" |
| `sp.measure.sides_say_x` | The other sides make this ≈{est} mm. Check the readings, or the corners. | the message line, the check of §2.4 |
| `sp.file.x_mark_x_shape_unreadable` | {surface}: mark {code}'s four corners were unreadable, kept as its box | opening a saved prep |

## 6. The slices

| Slice | Where | Size | What Amit sees after it |
|---|---|---|---|
| **Q1** the pure models | `:pano` | L | nothing on screen |
| **Q3** S5 | `mcft-interior-estimate` | M | nothing until a phone sends a `quad`; then on `/projects` the true outline and four side rows |
| **Q2** the screen | `:app` (and `:pano` test pins) | M | the feature on the phone |
| **Q4** S4 | `mcft-design-tools` | S–M | the true opening cut in SketchUp |

Order: Q1 and Q3 now, each a local commit. Q3 goes to stage ahead of the phone, as RD, V5 and T5 did. Q2 is built
after this workflow, once port 63 M2 is green in CI. Q4 is another session's. Port 63's M3 then gives the saved
pictures their side figures, and the journey slices follow.

### Q1 · the pure models

New `pano/Quad.kt`, one object:

- `KEY = "quad"`, `PARTS = q0 q1 q2 q3`, `SIDES = bottom right top left`, `DIAG = "diag"`, `CLOSE_MM`, `CLOSE_PART`.
- `mayHave(e)`; `read(v)` (§1.2, coercing numbers as `numOf` does); `of(e)`; `corners(e)` in mm (the box's four
  when there is no key); `cornerOf(part)`; `free(e, freeId)`.
- `set(e, mm)`: round to 0.1 mm, test §1.3, set the box to the bounding box, write the parts (a list of four lists
  of two numbers, in a new `extra` map) or remove the key. `clear(e)`.
- `drag(e, o, i, dx, dy, a, b, tol)` → `Drag(p, snap)` or null: corner `i` of the mark as it was (`o`) moved by
  (`dx`, `dy`); clamped; snapped straight (5° and within `tol`); snapped to the face's edge within 25 mm on a free
  axis; `set`. `snap` is `h`, `v`, `hv` or `d`. `tol = 0` snaps nothing. `guide(s, r)` for the rays.
- `straight(e)`, `square(e)`, `spanW(e)`, `spanH(e)`.
- `asked(s, e, a, b, base)`: the checklist of §2.3. It answers `base` itself when the mark has no `quad`.
- `closing(actual, e)` → `Closing(code, derived, fails)` or null.

Changes to existing files:

| File | Change |
|---|---|
| `SurveyPrep.kt` | `measures0` = `Quad.asked(…)` over today's body; `moveMark` and `snapMark` send a corner part to `Quad.drag` (`tol` 0 and `SNAP`) |
| `SurveyPrepFile.kt` | `decode` drops an unreadable `quad` with its problem, and the identity quietly |
| `SurveyPrepEdit.kt` | `SHAPE_FREE`, `SHAPE_BOX`; the menu row and its phase filter; `duplicate` carries `quad` |
| `SurveyPrepTouch.kt` | `Drawn` gains `corners` (declared after `spot`); `hit` answers a free mark's nearest corner as `Hit.Mark(id, "q2")` and tests a body press inside the shape |
| `MarkDims.kt` | `Role.SIDE`, `Role.DIAG`; `Dim` gains the side's outward normal; `of` draws the new keys and ends position lines at the corner; `value` adds the closing check |
| `DimLayout.kt` | `line` shifts a side along its normal; `shows` and `turns` know the two roles; `Mark` gains `corners`, whose four boxes are blocked in place of eight tabs |
| `MeasureFlow.kt`, `MeasureText.kt` | `checkText(tx, prep, code, ab)`: the closing sentence first, else the photo's; `sidesSay` |
| `SurveyGuide.kt` | `Rays.bold` answers both axes for `hv` |
| `Pano360.kt` | `markPoly` from `Quad.corners`; `tabsOf(e, free)` |
| `SurveyPlan.kt` | §3.7 |
| `SheetMarks.kt` | `KEPT` gains `quad` |
| `i18n/` | the fourteen keys, `Tx` regenerated, the ratchet, `mr-pending.tsv` |

Tests, each failing without its change: `QuadTest` (the record and its round trip, the five tests one at a time, the
parts unchanged by a move and a stretch, every refused edit, the snap by angle and by distance, the example to the
last figure, both closing cases); `KeyGrammarTest` (the five new measure names on every code); `MarkDimsTest` (no key
of a four-corner mark is undrawn, for every type); `DimLayoutTest` (the example at the phone's own numbers: its seven
figures placed, each side's plate on its own side, the count of dots pinned as found); `MeasureWalkTest`,
`MeasureFlowTest`, `MeasureTextTest`; `SurveyPrepFileTest` (unreadable and identity); `SurveyPrepTouchTest`,
`Pano360Test`, `SurveyPlanTest`, `SvgExportTest`; `MrPendingTest`.

Tests that move, each with its reason: `SheetMarksTest` (`KEPT`), `DuplicateTest` ("the box alone" becomes the box
and its shape), `LockTest` (the menu's rows).

**A mark without `quad` is what it was:** its checklist is the same list, its `Dim`s, plates, polygon, tabs, plan
items and file are unchanged. Q1 pins this on the existing example wall.

### Q2 · the screen

To be re-read against the code at build time; `:app` compiles only in CI.

- `SurveyPrepDraw.kt`: the outline as a `Path` in `drawMarks` and `lifeOutline`; the hole; the Dropped stroke; four
  discs for a free mark; `Marks` gains `free`; `dimLine` gains the branch that breaks a sloping line at its plate.
  `dims` is not touched (§3.9).
- `SurveyPrepScreen.kt`: `freeId`, cleared by an effect when the picked id is no longer it; the menu's first-frame
  height for six rows; `drawnMarks()` passes `corners = …` before `spot =` (NotePinTest's literal); the
  corner's grab built like a Line end's (`gate = true`, `focus` on the corner, `Quad.drag` with `endTol`, the guide
  through `onEndGuideS`, cleared with the end's); one branch each in `ctxPick` for the two actions and one local
  function above it (phase, lock, `snapshot()`, `Quad.clear`, `changed()`); `MeasureFlow.checkText` where the photo's
  sentence is said.
- `SurveyPrep360.kt`: `tabsOf(e, free)`.
- `MarkSheet.kt`, `SurveyMeasure.kt`, `FaceWriter.kt`, `AsIsUpload.kt`: no change.
- Source pins in `:pano` that move, each with its reason: `MeasureScreenTest` (the `Marks(…)` literal),
  `PhaseToolsTest` (the phase guard's count, 2 → 3), `LockTest`. No `:app` file may write `.extra =`: every write
  is a `Quad` function.

The build note for Amit: long-press a window, choose Four corners, drag a corner, and measure its four sides in
3 · Measure; use a project at Survey or only on the phone.

## 7. What is not decided, and the risks

Three questions for Amit. None blocks Q1 or Q3.

1. **How a mark is made four-cornered.** (a) Long press → Four corners, as designed: recommended, nothing new on
   screen. (b) A button beside the padlock in the picked mark's row: one tap and always visible, but the row is
   tight in landscape.
2. **An opening that is square but out of true.** (a) A rectangle stays width and height: recommended for now. (b)
   Let a rectangle be measured by its four sides as well; that needs a stored switch, so the key would then be kept
   on a shape that is its box.
3. **Curved tops.** (a) Later, as its own key, with an arched job in hand: recommended. (b) Now, before the journey
   slices.

Risks:

- **Finding it.** The feature is behind a long press. Question 1 is the remedy if he does not find it on the phone.
- **A slope the photo cannot show.** A run-out of a few tens of millimetres is inside the photo's own error. The
  readings carry it, and the check of §2.4 is the only thing that says a side drawn straight is not.
- **The snap at fit is wide** (about 150 mm on a 3.6 m wall). It is a Line end's own rule, and zoom narrows it; if it
  swallows real slopes the one constant is `SNAP_DP`.
- **A mark that stands off the wall.** A four-corner wardrobe is drawn on a photo that enlarges it. The photo check
  is already off there; the closing check still works.
- **The saved pictures** (§3.9), until M3.
- **S5 and S4 lag by design.** Until Q3 a vendor or a client reads the box; until Q4 SketchUp cuts the box.
- **Found, not changed here:** the phone's `SheetMarks.KEPT` keeps `sub` for others, and `SheetMarksTest` calls the
  list S5's own, but S5 took `sub` out of `SEEN_MARK` in RD's review. The two lists already differ by that key.
- **`:app` is unverified.** Q2's file list comes from a reader, not a compiler.
