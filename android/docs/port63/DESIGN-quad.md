# DESIGN-quad — a mark with four free corners

S2 APK (`mallet_estimator` → `android/`), with the slices S5 QUOTE and S4 MDT need. Written 16 Nov 2026 (IST), branch
`port63-quad`, on `7c49835` (port 63 M1 + M2 over SP0; `:pano` 95 classes, 1,352 tests).

**Basis.** Three readers' reports (the pure code and a mark's box; the screen after M2; S5 and S4), and the code read
again here wherever this file decides something. Nothing was run for this file: no gradle, no S5 test, nothing seen on
a phone. Every "decided by Claude" is judged by Amit on the phone.

**Revised the same day** after the plan critic's nine findings, each checked against the code first. What was
confirmed, what was refuted and what changed is in §8.

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
- The saved `-survey.jpg` picture of a four-corner mark gets its side figures from **port 63's M3, not from Q2**. Amit
  has ruled on those pictures ("Measured figures only"), so M3 is built directly after Q2 and goes out in the same
  Play build (§3.9).

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

1. It is a list of exactly four items, each a list of exactly two finite numbers from 0 to 1. A part is a JSON
   NUMBER on all three readers: text that looks like one (`"0.5"`) is not a number. S5's `coords` and its page
   reader test `typeof v === 'number'`, S4's box test is `is_a?(Numeric)`, and the phone's `Quad.read` tests
   `is Number`. It does not go through `SurveyPrep.numOf`, which turns text into a number.
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
| two parallel straight sides, no square corner | everything when the two differ in length (the legs and their difference make a triangle), nothing when they are equal (a parallelogram), and badly in between | four sides and the diagonal. The diagonal is always asked here, because the photo cannot tell 20 mm of difference; where the parallels differ it is the figure to spare |
| one straight side, or none | nothing until braced | four sides and one diagonal |

The diagonal is `diag`, from `q0` to `q2` (bottom-left to top-right; `FW1.diag`). Four sides and a diagonal are two
triangles, which fix the shape. Its tilt on the wall is then a straight side's if it has one, else the photo's. The
diagonal is asked exactly when the mark has no square corner, so no figure is asked on a mark that has a square
corner beyond its four sides.

Not built: a check of the diagonal against the four sides where it is the figure to spare (level top and level
bottom of different lengths). The closing check of §2.4 is for three straight sides only.

### 2.3 Where the mark sits, and what else its type asks

The position figures keep their keys and their exact meaning. `l r s t f k` run to the box's edge on every reader
today (S4 moves the box by them), and on a four-corner mark the box's edge is its outermost corner on that side. That
is the same distance, so by the c-key rule no new name is needed.

- The line on the photo ends at the corner that touches that box edge. If two do (the side is straight), it ends at
  their midpoint, which is where a rectangle's line ends today.
- When that side of the shape is not straight, the words gain `measure.to_nearest_corner` (", to its nearest corner":
  the MARK's corner nearest that wall, floor or ceiling), as a point's gain ", to centre". "Its" matters: the words
  before it already name a corner of the WALL ("from left corner"), and on the bar these words are all that tells
  the engineer where to put the laser. For `s` the nearest corner to the floor is the mark's lowest. A Beam's `d`
  gains nothing: it runs to its lowest corner, and the line shows which.
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

It is shown as the photo check is, with one difference in WHEN its sentence is said:

- **The `!`** is amber, after the sloping side's figure, on that plate alone.
- **The sentence** goes to the message line whenever ANY of the mark's four side readings lands or becomes active
  while the check fails. The screen says a check only for the code that just landed or became active (`sayCheck`),
  and the reading that completes the four is usually not the sloping side's: in the example the walk reads the
  sloping bottom FIRST and the left upright last.
- **The sentence names the side**, because it is often said while another side is active:
  `sp.measure.sides_say_x`, "The other sides make the bottom side ≈2466 mm. Check the readings, or the corners."
  The side's words are its own (`measure.side_*`).
- **Which sentence, when a code could say two** (decided by Claude while applying the critic's finding 3). On the
  sloping side's own code the closing sentence is said, also when that plate fails the photo check too. On one of
  the other three sides the photo's sentence is said when THAT reading fails its own photo check, because it names
  the figure just read; otherwise the closing sentence.

Nothing is stored.

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
| `FW1.s` | sill, from floor, to its nearest corner | ≈900 | to `q1`, the lowest corner; the bottom slopes |
| `FW1.p` | reveal depth | `?` | site only |

Left, right and top are straight; corners `q2` and `q3` are square; no diagonal is asked. Seven figures.

The walk reads them in the order above: bottom, right, top, left, then `l`, `s`, `p`.

- Read as 2495, 1550, 2370, 780: `derived` = 2491.9, off by 3. No `!`.
- With the left upright typed 870 by mistake: `derived` = √(2370² + 680²) = 2465.6, off by 29.4, over the 25 mm
  limit. The 870 is the reading that completes the four, so the check first fails as it lands: `!` on the bottom
  side's plate, and "The other sides make the bottom side ≈2466 mm. Check the readings, or the corners." is said as
  the 870 lands on the left side, while the walk moves on to `FW1.l`. The 870 itself is 90 from the photo's 780,
  inside the photo check, so no other sentence competes.

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
- **Cost, stated.** S4 today sizes the box it cuts from `w` and `h` readings
  (`mcft-design-tools` `survey_geometry.rb` 275–276) and does not know `quad`. So until S4's first step (§4.2) a
  kept `w` or `h` reading still sizes the bounding box S4 cuts, and the phone cannot show or clear it while the mark
  has four corners: a rectangle lasered `h` = 780 on its short upright and then made a trapezoid is cut 2370 × 780.
  Two things answer it. (a) S5 lists a kept `w` / `h` under the four-corner mark with its reading box, where staff
  clear it (today by its bare name, and in Q3's checklist: §4.1 item 4). (b) S4's first step, which may ride any S4
  commit, IGNORES `w` and `h` readings on a mark that carries a readable `quad`, and says so (§4.2).

## 3. On the phone

### 3.1 How a box becomes four corners, and goes back

**The long-press menu gains one row**, on a mark that may have `quad`, after Duplicate:

`Delete | Duplicate | Four corners | Lock | Lock all … | Cancel`

- On a rectangle the row is `sp.shape.four_corners`, "Four corners" (action `SurveyPrepEdit.SHAPE_FREE = "free"`).
- On a four-corner mark it is `sp.shape.rectangle`, "Rectangle" (action `SurveyPrepEdit.SHAPE_BOX = "box"`).
- The row is hidden where the phase holds the mark read-only (`PhaseTools.editable`), like Lock all. On a locked mark
  it is drawn and the act answers the lock's own words, as Delete does.
- With Delete or Duplicate hidden by the phase, the row stands after whichever of the two is left, before Lock.

This is the cheapest control the screen has: `SpCtxMenu` draws any list, and a long press on a mark or on one of its
handles already opens it. The corner tabs cannot do it by themselves, because `lt rt lb rb` are today's two-edge
resize of every rectangle.

**The row reaches the screen in Q2, not in Q1.** `:app` draws whatever list `:pano` returns and its `ctxPick` ends
`else -> ctx = null`, so a row added to the list in Q1 would be on the phone as a row that only closes the menu. Q1
builds the row and a switch that is off; Q2 turns the switch on in the same commit as the two acts (§6).

**"Four corners" changes nothing in the record.** The shape is still its box, and the key is stored only when it is
not. The act picks the mark and sets one screen state, `freeId`, the id of the mark whose corners are free. It says
`sp.shape.drag_corners`: "Drag a corner. A side near upright or level snaps straight." No undo step.

**Which handles a mark shows: one rule, `Quad.free(e, freeId, part)`.** A mark's handles are four free corners when
any of these holds; otherwise they are the eight tabs.

1. It has `quad`.
2. Its id is `freeId`.
3. `part` is one of `q0…q3`. `part` is the picked part of THIS mark (`SpSel.part`); for a mark that is not the picked
   one it is null.

A point or a Note is never free, whatever the three say (§1.2, rule 5).

Why three, and not the first two: a shape is its box the moment a corner is dragged or snapped back, and §1.1 removes
the key at that moment, in the middle of the drag. With rules 1 and 2 alone a mark opened from the file (so `freeId`
is null) would turn into a rectangle with eight tabs under a finger that still holds `q0`, and the nudges would then
move a corner nobody can see.

- `freeId` is set by the menu act, and by the screen's `grabbed(g)` whenever the part grabbed is a corner. `grabbed`
  is the one function both the photo's press and the 360's call. So a shape dragged or snapped back to its box keeps
  its four corners, also after a later press on its body, until the selection leaves the mark.
- `freeId` is cleared when the selection leaves that mark. Only then is a shape that is its box a rectangle with
  eight tabs again; a rectangle whose corners were freed and never dragged is simply a rectangle again.
- `moveMark` with a corner part works on a mark with no key: its corners are the box's four.
- Every place that asks which handles to show asks this one function: the painter, `drawnMarks()` for the press,
  `DimLayout`'s blocked boxes, and the 360's two places (§3.6).
- On a mark with no key the row reads "Four corners" even while its corners are already free. Picking it again
  changes nothing and says the same words.

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
- **A drag that passes through the box** (the corner snapped level and upright at once, then pulled out again) removes
  the key and writes it again. The handles stay four corners all the way (§3.1, rule 3).
- One undo step per drag, as any mark's.

### 3.3 Moving, resizing, duplicating, undo, lock, phases

- **Move:** a press inside the shape drags it whole. The box is kept on the face and snaps to its edges as today; the
  shape rides. The nudges on "move" are unchanged.
- **Resize of the whole shape:** a mark whose corners are free (§3.1) shows no edge tabs. The model still answers
  `l r t b` on a four-corner mark by stretching the box with the shape (that is what an older build does); nothing
  on screen calls it.
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
- A picked mark whose corners are free (§3.1) blocks its four handles, not eight tabs. A four-corner mark has no
  hand-anchored `w` / `h` tags and no compact plate: a very small one falls to leaders and dots.
- **The walk** orders marks as before (by the box's left edge). Inside the mark: bottom, right, top, left, the
  diagonal if asked, then the type's own. The counts and the tabs' "to measure" follow the checklist.
- **The bar** says the side's words over the face's progress: "bottom side" over `3 of 12 measured`.
- A mark that is not in the room (a wished four-corner unit) shows its side estimates and takes no reading, as today.

### 3.6 The 360

`Pano360.markPoly` takes the four real corners in place of the box's (same order, so a rectangle is drawn as it was).
`Pano360.tabsOf(e, free)` gives a free mark its four corners as `q0…q3`. The press, the drag and the snap go through
`moveMark` and `snapMark` as today; there the snapping distance is the 25 mm floor. No figures are drawn on the 360.

**The 360 uses the same rule and the same `freeId`.** A long press on the 360 opens the screen's ONE menu
(`P3Actions.longPress` sets the same `ctx`, `SurveyPrepScreen.kt:1400`), so the row is there too and its two acts
are the screen's. For that to be more than a dead row, `Sp360` is passed `freeId` beside `sel`, which it already
receives. `free` is worked out by ONE local function there, `Quad.free(e, freeId, sel.part)` for the picked mark,
and both places use it: the tabs a press can take (`SurveyPrep360.kt:299`) and the handles the painter draws (`:830`,
through one new field on `P3Draw.State`). Two places that each work it out would be two answers.

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

Amit ruled on port 63's question 1 on 16 Nov 2026, ~18:50 IST (mcft-workspace `CLAUDE.md`, "THE SAVED PICTURES CARRY
MEASURED FIGURES ONLY"): the saved pictures carry **"Measured figures only"**. A dimension with a reading is printed,
one without is left off. That is slice M3, built after the four-corner marks. `dims` (the old painter, still pinned
byte for byte by `MeasureScreenTest` until M3) draws a width only for `w` and a height only for `h`.

**So a four-corner mark's saved picture from Q2 alone shows its true outline, its code and its position figures, and
no side figures.** That is fewer figures than the workaround gives: the same window drawn as four Lines exports with
four, because a saved picture does draw a Line with its tag (`drawMeas` runs in every mode but the magnifier's). His
request came from an export ("like one just exported"), so this is not left for him to find.

- **M3 is built directly after Q2 and goes out IN THE SAME Play build.** With it a four-corner mark's saved picture
  prints each side that has a reading and leaves an unread side off, by his rule.
- **If Q2 must ship alone**, its build note says in one line: an exported picture of a four-corner mark shows its
  outline and no side figures yet; for a picture to send before the next build, keep the four Lines.

M3 replaces `dims` with the painter the screen uses, and the sides arrive with it. Whatever the build, the side
figures also leave the phone on the A4 sheet and in the readings S5 shows.

`DESIGN-port63.md` still says M3 "waits for question 1" (its §4 and §5). It was written before the ruling; this
section is the later word.

## 4. S5 and S4

### 4.1 Slice Q3 — S5 `/projects`

Display only: no route, no write, no statement, no migration. `KEY_RE` does not change.

1. **The as-surveyed rule.** `SEEN_MARK` gains `quad: coords` after `locked` (`functions/_lib/asis.js`). `coords`
   passes a list of number pairs whole or not at all, so one that is not number pairs is left out; one that is number
   pairs but fails §1.2 (three pairs, a part of 7) is given as stored and read by the page as its box. Either way
   the mark is its box. A mark born a wish is still left out whole. `README.md`, the header of `asis.js` and the RD
   row's key list follow (`id t code x0 x1 y0 y1 locked quad`; still not `sub`), as the docs test holds them.

   **Pins that move, by name** (`mcft-interior-estimate`, line numbers at `f00e8cb`):
   - `tests/api.test.mjs` 5340–5345, the level check: `keysOf(redactRecord(surveyedRecord(), false)…els)` must equal
     `SURVEYED_KEYS.mark`, and no fixture mark carries `quad`. The check builds its record IN THE TEST:
     `surveyedRecord()` with `quad` set on one surveyed mark, as that mark's last key so the order holds. The
     fixture file is not edited (item 5).
   - `tests/api.test.mjs` 5347 and `tests/asis-view.test.mjs` 1075: the literal list gains `quad`.
   - `tests/api.test.mjs` 5359 (`README.md:75`, the level table's mark row), 5372 (the RD row of `CLAUDE.md`) and
     5381 (the header of `asis.js`, lines 197 AND 218): each text gains `quad`.
   - Not pinned, and stale unless corrected: the same list in the T5 row of `CLAUDE.md`. It is corrected with a
     "since slice Q3" note.
2. **The reader** (`app/src/asis_view.js`): `quadOf(e)` by the five tests of §1.2, tolerant. Anything else reads as
   absent. The view mark gains one optional key, `quad`, only when it is read.
3. **The outline, for staff, vendor and client alike once given.** A mark with `quad` is `class="pmark pquad"` with an
   SVG inside it: `viewBox="0 0 1 1"`, `preserveAspectRatio="none"`, a polygon at `(px, 1 − py)` with
   `vector-effect="non-scaling-stroke"`. `.pmark.pquad{border:0;box-shadow:none}`. Past Existing, `outlineSvg` takes
   the polygon in place of its `<rect>` and the Dropped cross runs `q0` to `q2`.

   **An Existing four-corner mark's outline**, which stands in for the border and the shadow `.pquad` removes: two
   polygons of the same points, both `fill="none"`. First, underneath, `stroke="rgba(0,0,0,.45)"` at
   `stroke-width="4"`; over it the mark's colour at `stroke-width="2"`. So 1 px of the dark shows each side, which is
   the box-shadow's stand-in. The colour is written into the attribute as `outlineSvg` writes it (`m.colour`), not as
   `var(--mc)`. The SVG takes the class `pout` (absolute, the box's size, overflow visible), so a stroke centred on
   the box's edge is not cut.

   **The code label.** `.pmark span` sits at `left:-2px;top:-2px` for the 2 px border that `.pquad` removes, so:
   `.pmark.pquad span{left:0;top:0}`, `.pmark.pquad .ptag{left:0;top:0}` and
   `.pmark.pquad.pr .ptag{left:auto;right:0}`, as `.plife` already has them.
4. **The checklist.** `measures` mirrors §2.1 to §2.3 line for line: four side rows with the corner-to-corner
   estimate, `diag` when there is no square corner, then the type's rows without `w` and `h`. `rowsFor`, `rowHtml`
   and `wireReadings` then give the Photo / Site / Diff cells, the reading boxes for staff and the PATCH with no
   change. A `w` or `h` reading the mark still holds is listed under it as before, with its reading box, and that is
   where staff clear it (§2.6: until S4's first step it still sizes the box S4 cuts). The list head says
   `four corners, within W × H mm on the photo`.
5. **A record without the key renders to the byte as before.** Every addition is behind `quad`. The existing fixtures
   are not edited; a new pair is added (a surveyed four-corner window; the same room after the office adds a wished
   four-corner unit). The one existing test that needs a mark with `quad` builds it in the test (item 1).
6. **The CLAUDE.md row**, one line after T5's, in the pinned shape: Amit's words verbatim; `display only`; `drawn
   exactly as before`; the rule is the server's, `redactRecord` alone; `**The guards**:`; `**Decided by Claude in
   this slice, where the plan was silent**:`; `**What does not exist yet**: NOT DEPLOYED when this was written`; and
   that `vector-effect` is proven in Chromium only and nothing on Cloudflare.

Tests: the reader's tolerance (3 or 5 points, a word, a part that is text like `"0.5"`, a part outside 0…1, no span,
crossed, turned inward, on a point or a Note); the four rows and their estimates, each key through `KEY_RE`; the
polygon string plain (both strokes) and per state; a vendor and a client given a surveyed mark's `quad` and never a
wished one's, on both stores; a `quad` of three number pairs given as stored and drawn as the box; the docs test;
and in a browser at 1280 and 390 px, the outline, four inputs for staff, one side PATCH round trip, and a vendor's
and a client's room the same HTML before and after the office's work.

### 4.2 Slice Q4 — S4 "Room from survey" (not built here)

Until S4 changes, it ignores `quad`, silently: a four-corner window, door, opening or niche is cut as its bounding
box (the example: 2370 × 1550, about 0.91 m² more than the opening), and the side readings are never read. Worse, a
`w` or `h` reading the mark kept from its time as a rectangle still sizes that box (`survey_geometry.rb` 275–276;
§2.6).

**The smallest first step, which can ride any S4 commit.** It reads `quad` by the five tests of §1.2, and on a mark
that carries a readable one:

- `w` and `h` readings are NOT read. The box S4 builds is the bounding box from the photo, placed by `l r s t f k`
  as today (those run to the box's edge on every reader).
- It warns: "FW1 has four corners: built as its bounding box from the photo". When the mark holds a `w` or `h`
  reading the warning ends "; its w/h readings not used".

This step changes what is built for such a mark, so `VERSION` goes from 3 to 4 with it.

**The slice itself:** on cut and recess marks, turn the parts into four model points, cut that face, use the
polygon's area in the size check, and treat an overlap as an outline with a warning. Side readings move a corner
only where they fix the shape (a square corner); otherwise the photo's corners stand and the mark says `photo`.
`VERSION` goes up again (4 to 5).

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
| `measure.to_nearest_corner` | , to its nearest corner | a fragment after a position's words, as `measure.to_centre`: "from left corner, to its nearest corner" |
| `sp.shape.four_corners` | Four corners | the long-press menu, on a rectangle |
| `sp.shape.rectangle` | Rectangle | the long-press menu, on a four-corner mark |
| `sp.shape.drag_corners` | Drag a corner. A side near upright or level snaps straight. | the message line, after "Four corners" |
| `sp.measure.sides_say_x` | The other sides make the {side} ≈{est} mm. Check the readings, or the corners. | the message line, the check of §2.4. `{side}` is the sloping side's own words (`measure.side_*`: "bottom side", on a plan "back side"); `{est}` whole mm |
| `sp.file.x_mark_x_shape_unreadable` | {surface}: mark {code}'s four corners were unreadable, kept as its box | opening a saved prep |

## 6. The slices

| Slice | Where | Size | What Amit sees after it |
|---|---|---|---|
| **Q1** the pure models | `:pano` | L | nothing on screen, and nothing `:app` can see or call (below) |
| **Q3** S5 | `mcft-interior-estimate` | M | nothing until a phone sends a `quad`; then on `/projects` the true outline and four side rows |
| **Q2** the screen | `:app` (and `:pano` test pins) | M | the feature on the phone |
| **M3** port 63's (the saved pictures) | `:app` | S | a four-corner mark's exported picture with each side that has a reading |
| **Q4** S4 | `mcft-design-tools` | S–M | the true opening cut in SketchUp |

Order: Q1 and Q3 now, each a local commit. Q3 goes to stage ahead of the phone, as RD, V5 and T5 did. Q2 is built
after this workflow, once port 63 M2 is green in CI. **Q2, then M3 in the same phone build** (§3.9): both land on
`batch-next` before `main` moves, since every green `main` goes to Play at once. Q4 is another session's; its first
step (§4.2) may ride any S4 commit before it. The journey slices follow.

### Q1 · the pure models

**Q1 changes nothing `:app` can see or call.** `:app` compiles only in CI, so a Q1-only push must neither break its
compile nor put anything on the phone.

- **The menu row is NOT in the list the screen draws.** Q1 adds `SHAPE_FREE`, `SHAPE_BOX`, `SurveyPrepEdit.shapeRow(tx, e)`
  and a parameter `shape: Boolean = false` on `menuItemsOf(tx, p, id, stage, picked, shape)`. The screen's one call
  (`SurveyPrepScreen.kt:2228`) does not pass it, so the menu on the phone is the five rows it was. Q2 passes
  `shape = true` in that call in the same commit as the two `ctxPick` branches.
- **Every signature `:app` calls today stays source-compatible:** `Pano360.tabsOf(e, free: Boolean = false)`
  (`SurveyPrep360.kt:299` and `:830` call `tabsOf(e)`); `SurveyPrepTouch.Drawn(…, spot = null, corners = null)`
  (`SurveyPrepScreen.kt:2696` names `spot =`); `DimLayout.mark(…)` unchanged in its thirteen positional parameters
  (`SurveyPrepDraw.kt:917` passes them by position); `MeasureFlow.checkEst` kept beside the new `checkText`
  (`SurveyPrepScreen.kt:530` calls it). `MarkDims.Role` gains two values, and `:app` has no `when` over it (its one
  use is `p.role == MarkDims.Role.WIDTH`, `SurveyPrepDraw.kt:1009`).
- **Q1 ends with a grep of `android/app` for each changed `:pano` symbol**, and the commit message records it:
  every call found compiles against the new signature as written.

New `pano/Quad.kt`, one object:

- `KEY = "quad"`, `PARTS = q0 q1 q2 q3`, `SIDES = bottom right top left`, `DIAG = "diag"`, `CLOSE_MM`, `CLOSE_PART`.
- `mayHave(e)`; `of(e)`; `corners(e)` in mm (the box's four when there is no key); `cornerOf(part)`.
- `read(v)` (§1.2): each part must be a JSON NUMBER (`is Number`, finite, 0…1). A numeric string is unreadable. It
  is NOT `numOf`, which coerces text (`SurveyPrep.kt` 1311–1315), because S5 and S4 do not coerce.
- `free(e, freeId, part)` (§3.1): the mark may have a `quad`, and it has one, or its id is `freeId`, or `part` is
  one of `PARTS`.
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
| `SurveyPrepEdit.kt` | `SHAPE_FREE`, `SHAPE_BOX`; `shapeRow(tx, e)` (the row, or null where the mark may not have a `quad`); `menuItemsOf(tx, p, id, stage, picked, shape: Boolean = false)`, which adds the row only when `shape` is true and the phase lets the mark be edited; `menuItems` and the three-argument `menuItemsOf` are NOT changed; `duplicate` carries `quad` |
| `SurveyPrepTouch.kt` | `Drawn(…, spot = null, corners = null)`: `corners` is declared after `spot`, with a default; `hit` answers a free mark's nearest corner as `Hit.Mark(id, "q2")` and tests a body press inside the shape |
| `MarkDims.kt` | `Role.SIDE`, `Role.DIAG`; `Dim` gains the side's outward normal; `of` draws the new keys and ends position lines at the corner; `value` adds the closing check |
| `DimLayout.kt` | `line` shifts a side along its normal; `shows` and `turns` know the two roles; `mark(…)` keeps its thirteen positional parameters and gains `free: Boolean = false` as its LAST; the output `Mark` gains `corners`, whose four boxes are blocked in place of eight tabs |
| `MeasureFlow.kt`, `MeasureText.kt` | `checkText(tx, prep, code, ab)` (§2.4): on the sloping side's code of a mark whose `Quad.closing` fails, the closing sentence; else the photo's sentence when this code's own reading fails `MarkDims.check`; else, on another of that mark's four side codes, the closing sentence; else null. `sidesSay(tx, side, est)`. `checkEst` STAYS: `checkText` calls it for the photo's half, and the screen calls it until Q2 |
| `SurveyGuide.kt` | `Rays.bold` answers both axes for `hv` |
| `Pano360.kt` | `markPoly` from `Quad.corners`; `tabsOf(e, free: Boolean = false)` |
| `SurveyPlan.kt` | §3.7 |
| `SheetMarks.kt` | `KEPT` gains `quad` |
| `i18n/` | the fourteen keys, `Tx` regenerated, the ratchet, `mr-pending.tsv` |

Tests, each failing without its change: `QuadTest` (the record and its round trip, the five tests one at a time, the
parts unchanged by a move and a stretch, every refused edit, the snap by angle and by distance, the example to the
last figure, both closing cases; `[["0","0"],["1","0"],["1","1"],["0","1"]]` and `[["0","0.5"],…]` unreadable, each
with its problem; `free` by each of its three rules, and a drag of `q0` that passes through the box and out again
keeps `free` true with part `q0` while the key goes and comes back); `KeyGrammarTest` (the five new measure names on
every code); `MarkDimsTest` (no key of a four-corner mark is undrawn, for every type); `DimLayoutTest` (the example
at the phone's own numbers: its seven figures placed, each side's plate on its own side, the count of dots pinned as
found); `MeasureWalkTest`; `MeasureFlowTest` (the example in walk order: bottom 2495, right 1550, top 2370, then
left 870, and the sentence comes on `FW1.left`, naming the bottom side; on `FW1.bottom` it is the same sentence;
a left typed far from the photo says the photo's sentence instead; a rectangle says what `checkEst` said);
`MeasureTextTest`; `SurveyPrepFileTest` (unreadable and identity); `SurveyPrepTouchTest`; `Pano360Test` (`tabsOf(e)`
is the eight tabs it was; `tabsOf(e, Quad.free(e, null, "q0"))` on a mark with no key answers the box's four corners
as `q0…q3`); `SurveyPlanTest`, `SvgExportTest`; `MrPendingTest`.

Tests that gain a case, their existing pins untouched: `LockTest` (its five-row pins stay, since `menuItems` and the
three-argument `menuItemsOf` are not changed; one new case, at Survey with `shape = true`:
`Delete|Duplicate|Four corners|Lock|Lock all Door|Cancel`, "Rectangle" on a mark with `quad`, no row on a point or a
Note, and no row where the phase holds the mark read-only).

Tests that move, each with its reason: `SheetMarksTest` (`KEPT`), `DuplicateTest` ("the box alone" becomes the box
and its shape).

**A mark without `quad` is what it was:** its checklist is the same list, its `Dim`s, plates, polygon, tabs, plan
items and file are unchanged. Q1 pins this on the existing example wall.

### Q2 · the screen

To be re-read against the code at build time; `:app` compiles only in CI.

- `SurveyPrepDraw.kt`: the outline as a `Path` in `drawMarks` and `lifeOutline`; the hole; the Dropped stroke; four
  discs for a free mark; `Marks` gains `free`; `dimLine` gains the branch that breaks a sloping line at its plate.
  `dims` is not touched (§3.9).
- `SurveyPrepScreen.kt`: `freeId`, set by the menu act and by `grabbed(g)` when `g.part` is one of `Quad.PARTS`,
  cleared by an effect when the picked id is no longer it; the menu's one call passes `shape = true`; the menu's
  first-frame height for six rows; `drawnMarks()` passes `corners = …` before `spot =` (NotePinTest's literal) for
  every mark where `Quad.free(e, freeId, part)` holds, `part` being `sel.part` for the picked mark and null for any
  other; the corner's grab built like a Line end's (`gate = true`, `focus` on the corner, `Quad.drag` with `endTol`,
  the guide through `onEndGuideS`, cleared with the end's); one branch each in `ctxPick` for the two actions and one
  local function above it (phase, lock, `snapshot()`, `Quad.clear`, the selection back to the whole mark, `freeId`
  cleared, `changed()`); `sayCheck` says `MeasureFlow.checkText(…)` where it said `checkEst` and `photoSays`;
  `Sp360` is passed `freeId`.
- `SurveyPrep360.kt`: `Sp360` gains `freeId: String?`. One local function answers `free` for the picked mark,
  `Quad.free(e, freeId, sel.part)`, and both places use it: `tabsOf(e, free)` at `:299`, and the painter at `:830`
  through one new field on `P3Draw.State`, where a free mark's handles are four discs.
- `MarkSheet.kt`, `SurveyMeasure.kt`, `FaceWriter.kt`, `AsIsUpload.kt`: no change.
- Source pins in `:pano` that move, each with its reason: `PhaseToolsTest` 794 (the menu call's literal ends
  `asPicked)`; it gains `shape = true`) and 801 (the phase guard's count, 2 → 3); `MeasureScreenTest` 335–338
  (`sayCheck`'s body: its `checkEst` and `photoSays` become the one `checkText`, so the screen names
  `MeasureText.photoSays` no longer). Checked and NOT moving: `MeasureScreenTest` 386 (the
  `Marks(…)` literal is FaceWriter's, which Q2 does not change; `Marks` gains `free` as a named, defaulted
  parameter) and `LockTest` (it reads no screen source). No `:app` file may write `.extra =`: every write is a
  `Quad` function.

The build note for Amit: long-press a window, choose Four corners, drag a corner, and measure its four sides in
3 · Measure; use a project at Survey or only on the phone. If Q2 ships without M3, the note adds one line: an
exported picture of a four-corner mark shows its outline and no side figures yet; for a picture to send before the
next build, keep the four Lines.

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
- **The saved pictures** (§3.9). A Q2 build without M3 exports a four-corner mark with fewer figures than four
  Lines give. M3 rides the same Play build; if it cannot, the build note says so in one line.
- **S5 and S4 lag by design.** Until Q3 a vendor or a client reads the box; until Q4 SketchUp cuts the box.
- **A kept `w` or `h` reading and S4** (§2.6). Until S4's first step it sizes the box S4 cuts, unseen on the phone.
  Staff clear it on `/projects`.
- **The closing sentence is said on a side that is not the one flagged** (§2.4). The sentence names the side, but
  the `!` he sees is on another plate than the one he just read. Whether that reads clearly is his to judge on the
  phone.
- **Found, not changed here:** the phone's `SheetMarks.KEPT` keeps `sub` for others, and `SheetMarksTest` calls the
  list S5's own, but S5 took `sub` out of `SEEN_MARK` in RD's review. The two lists already differ by that key.
- **`:app` is unverified.** Q2's file list comes from a reader, not a compiler.

## 8. The critic

The plan critic read this design against the code and raised nine findings (16 Nov 2026). Each was checked before
anything was changed: `mallet_estimator` at `7c49835`, `mcft-interior-estimate` at `f00e8cb`, `mcft-design-tools` at
`dc010ac`, mcft-workspace `CLAUDE.md` at `229f3a2`. All nine are confirmed in substance and applied. **One statement
inside finding 2 is refuted, and the part of its fix that rested on it is declined.** Nothing else is declined.

Short names: `SPS` = `app/SurveyPrepScreen.kt`, `SPD` = `app/SurveyPrepDraw.kt`, `SP360` = `app/SurveyPrep360.kt`.

| # | Finding (severity) | Checked against | What changed |
|---|---|---|---|
| 1 | Q1 is not screen-neutral: a dead "Four corners" row, and signatures that can break `:app` before Q2 (major) | **Confirmed.** The screen draws the list `:pano` returns (`SPS:2228`) and `ctxPick` ends `else -> ctx = null` (`SPS:1097-1108`); `:app` calls `Pano360.tabsOf(e)` (`SP360:299`, `:830`), `MeasureFlow.checkEst` (`SPS:530`), `DimLayout.mark` by position (`SPD:917`) and `Drawn(…, spot = …)` (`SPS:2696`). Found while checking: `PhaseToolsTest` 794 pins the menu call's literal and `MeasureScreenTest` 335–338 pin `sayCheck`'s body, so both move in Q2; `MeasureScreenTest` 386 and `LockTest` do not, though the Q2 list named them | §3.1 (the row reaches the screen in Q2); §6 Q1 (the opening paragraph, the rows for `SurveyPrepEdit`, `SurveyPrepTouch`, `DimLayout`, `MeasureFlow`, `Pano360`; `LockTest` from "tests that move" to "tests that gain a case"; the closing grep); Q2's list of pins corrected |
| 2 | Which handles a mark shows is undefined once its `quad` is removed mid-drag, and the 360 never receives `freeId` (major) | **Confirmed, with one statement refuted.** Confirmed: the key goes the moment the shape is its box (§1.1) while `SpSel.part` stays `q0` (`SPS:2331`) and the nudges move `sl.part` (`SPS:995`); `Sp360` is passed `sel` and no `freeId` (`SPS:1612`); entering a step sets `sel = null` (`SPS:1290`); `tabsOf` is called in two places. **Refuted: "the 360 has no long-press menu for marks."** It has: with Select on, a long press calls `longPress` (`SP360:129`, `:598`), which sets the screen's `ctx` (`SPS:1400`), and the one `SpCtxMenu` opens over the 360 | §3.1: the one rule `Quad.free(e, freeId, part)` with its three conditions; `freeId` also set by `grabbed(g)` (`SPS:765`), the one function the photo and the 360 both call. §3.2, §3.3, §3.5. `QuadTest` and `Pano360Test` cases as asked. **Declined: "a rectangle is made four-cornered on 2 · Mark only."** The menu is on the 360, so a row whose `freeId` the 360 never saw would be the dead row of finding 1. Instead `Sp360` is passed `freeId`, and one local function answers `free` for both places (§3.6, Q2) |
| 3 | The closing check's sentence is never said in the design's own example: the walk reads the sloping side first (major) | **Confirmed.** `sayCheck(code)` speaks only for the code that landed or became active (`SPS:529-531`; called at `537`, `567`, `1150`); a Window's checklist after its sides is `l`, `s`, `p` (`pano/SurveyPrep.kt:895`), and §3.5 walks bottom first, so the four are complete only when `left` lands. In `reading` the landed code's `sayCheck` runs after the walk's own, so its sentence is the one left on the line | §2.4 (said on any of the four side codes; names the side), §2.5 (the 870 case in walk order), §5 (`{side}` in the text), Q1 `checkText`, `MeasureFlowTest`. **One rule added by Claude:** on a side that is not the sloping one, a reading that fails its OWN photo check says the photo's sentence. The wider rule would otherwise replace the sentence about the figure just typed |
| 4 | §3.9 is stale: Amit has ruled on port 63's question 1, and the first four-corner build exports fewer figures than his four Lines (major) | **Confirmed.** mcft-workspace `CLAUDE.md` 208–217 records the ruling of ~18:50 IST, 16 Nov: "Measured figures only", M3 "built after the four-corner marks". A saved picture draws a Line with its tag (`SPD:580`), and `dims` draws `w` and `h` only (`SPD:1047-1056`) | §0 (third departure), §3.9 rewritten, §6 (M3 in the table; "Q2, then M3 in the same phone build"), §7, the build note. `DESIGN-port63.md` still says M3 waits for question 1; it is not edited by this commit, and §3.9 says which is the later word |
| 5 | A kept `w` / `h` reading the phone no longer shows still resizes the box in S4 (minor) | **Confirmed.** `survey_geometry.rb` 275–276 take `w` and `h` readings for every mark; S4 has no `quad` anywhere. S5 lists a reading its mark does not ask (`asis_view.js` `rowsFor`, 799–807) | §2.6 (the cost, stated), §4.1 item 4, §4.2 (the first step reads `quad`, ignores `w` / `h` on such a mark and says so), §7. Two details added: the warning's tail appears only when the mark holds such a reading, and `VERSION` moves with the first step (3 → 4) and again with the slice, by S4's own rule for it |
| 6 | §2.2 is wrong for two parallel straight sides of different length (minor) | **Confirmed**, by geometry: slide one leg along the parallels and the two legs with the parallels' difference make a triangle, so four sides fix it unless the parallels are equal. No code is involved | §2.2: the table's third row split in two, the false reason replaced, and one line saying the spare diagonal has no check. `Quad.asked` is unchanged |
| 7 | The phone's reader coerces numeric strings; S5's does not (minor) | **Confirmed.** `numOf` takes `is String -> v.toDoubleOrNull()` (`pano/SurveyPrep.kt:1311-1315`); S5's `coordsOk` tests `typeof v === 'number'` and passes any list of number pairs, three pairs included (`asis.js` 605–606) | §1.2 rule 1; Q1 `read(v)` and its `QuadTest` case; §4.1 item 1 reworded, with an S5 test for text parts and for three pairs |
| 8 | Q3 does not name the S5 pins that move, one collides with "the fixtures are not edited"; the plain outline's stroke is unspecified (minor) | **Confirmed**, every line number as given: `api.test.mjs` 5340–5345, 5347, 5359, 5372, 5381; `asis-view.test.mjs` 1075; `asis.js` 197 and 218; `README.md` 75; `.pmark` and `.pmark span` (`projects.css` 43–44) | §4.1 items 1, 3 and 5. Three details differ from the fix's wording: which polygon is on top is said outright (the dark one underneath); the colour is written into the attribute as `outlineSvg` writes it, not as `var(--mc)`; and `.ptag` gets the same offsets as the `span` |
| 9 | ", to nearest corner" after "from left corner" names two different corners (minor) | **Confirmed.** `measure.left_corner` is "from left corner" and `measure.right_corner` "from right corner": the wall's (`i18n/strings-en.json`) | §2.3, §2.5 (`FW1.s`), §5: ", to its nearest corner" |

## 9. Slice Q1 as built (16 Nov 2026)

The pure models, in `:pano` alone; no `:app` file changed and nothing is on screen. `:pano` went from 95 classes and
1,352 tests to 97 classes and 1,390 tests, all green (the totals of `pano/build/test-results/test`); `:app` is compiled only by CI and nothing has been seen on a
phone. This section says what exists, for whoever builds Q2, and where the build went past the design's wording.

**What exists.**

| File | What it now has |
|---|---|
| `pano/Quad.kt` (new) | `KEY PARTS SIDES DIAG CLOSE_MM CLOSE_PART`; `mayHave`, `read`, `identity`, `of`, `corners`, `cornerOf`, `free`, `set`, `clear`, `drag` → `Drag(p, snap)`, `guide`, `straight`, `square`, `spanW`, `spanH`, `touch` → `Touch`, `sideWhat`, `asked`, `closing` → `Closing`, `sideCodes` |
| `pano/SurveyPrep.kt` | `measures0` is `Quad.asked(…)` over its old body (`rect0`); `moveMark` and `snapMark` take a corner part |
| `pano/SurveyPrepFile.kt` | `decode` drops an unreadable `quad` with `sp.file.x_mark_x_shape_unreadable`, and the box's own corners quietly |
| `pano/SurveyPrepEdit.kt` | `SHAPE_FREE`, `SHAPE_BOX`, `shapeRow`, `menuItemsOf(…, shape = false)`; `duplicate` carries `quad` |
| `pano/SurveyPrepTouch.kt` | `Drawn(…, spot = null, corners = null)`, `nearestCorner`; `hit` answers `q0…q3` and a body press inside the shape |
| `pano/MarkDims.kt` | `Role.SIDE`, `Role.DIAG`; `Dim.nx`, `Dim.ny`; `of` for a four-corner mark; `value` with the closing `!`; `photoCheck` |
| `pano/DimLayout.kt` | `isSize`, `outward`, `cornerBoxes`; `Fig.out`; `Mark.corners`; `mark(…, live, free = false)`; a side's line, turn and step-off |
| `pano/MeasureFlow.kt`, `pano/MeasureText.kt` | `checkText`; `checkEst` is the photo's half alone; `sidesSay` |
| `pano/SurveyGuide.kt` | `Rays.bold` answers both axes for `hv` |
| `pano/Pano360.kt` | `markPoly` from `Quad.corners`; `tabsOf(e, free = false)` |
| `pano/SurveyPlan.kt` | §3.7, for a mark with `quad` only |
| `pano/SheetMarks.kt` | `KEPT` holds `quad` |
| `i18n/` | the fourteen keys, English first; 1,938 keys; 27 pending |

**Where the build went past the wording.** Each is small; none changes a decision above.

1. **`Quad.read` answers the parts or null, and `Quad.identity` says "the box's own corners".** `decode` needs to tell
   the quiet drop from the said one, and `read` alone cannot.
2. **`Quad.touch(e)`**: where the shape touches each edge of its box (one corner, or the midpoint of two). `MarkDims`
   and `SurveyPlan` both end a position line there, so it is worked out once.
3. **`Drag.snap` says which of the corner's two sides ARE straight after the move**, not which snapped in that event.
   A nudge that lands a corner exactly level draws its ray bold too. For a drag the two are the same.
4. **`snapMark` on a corner part** is `Quad.drag` from the mark as it stands, moved by nothing, with `SNAP` as its
   distance. `moveMark` on a corner part is `Quad.drag` with distance 0 and answers false when the shape is refused.
5. **`MarkDims.photoCheck`** is new, and `MeasureFlow.checkEst` asks it. `checkEst` used `value(…).check`, which now
   also carries the closing `!` on the sloping side; left as it was it would have said "The photo says …" for a
   reading the photo agrees with.
6. **The diagonal's plate turns like a side's** when its line is more upright than level. §3.5 says it of a side.
7. **`DimLayout.Mark.corners` is set only where the mark shows handles at all** (`tabs` and `free` both). The caller
   passes `tabs` exactly as today (picked, 2 · Mark, unlocked, editable) and `free = Quad.free(e, freeId, part)`.
   The four squares kept clear are 5.5 dp either side of each corner (the disc), never under a tab's own 5 units.
8. **A test moved that §6 did not name:** `DimLayoutTest`, "which figures a scope shows". It walks every `Role`, so
   the set shown on an unpicked mark in 3 · Measure had to gain `SIDE` and `DIAG`. `SheetMarksTest` (`KEPT`) moved
   as named. `DuplicateTest` gained a case; its old assertions stand, since a rectangle's copy is still the box alone.
9. **§1.3, read to the letter, refuses any shape under about 28 mm square.** A 20 mm rectangle's corner is 14 mm
   outside the line joining its neighbours. Such a mark keeps its eight tabs' resize; once larger, its corners drag.
10. **The six drawings draw no diagonal.** §3.7 names a dimension along each side only. `diag` is asked, walked and
    drawn on the photo.
11. **`RectanglePinTest`** is the pin of "a mark without `quad` is what it was". It dumps the checklist, the
    dimensions, the plates in three scopes, the press over a grid, every tab's drag, the 360's tabs and outlines,
    the six drawings, the sheet's marks, the file, the walk and the menu, for marks with no `quad`; and holds each
    section to what the same dump printed on `7c49835`, where it was taken from a plain copy of that commit. The
    two dumps, 3,972 lines, were byte for byte the same.

**For Q2.** The menu's one call passes `shape = true`. `drawnMarks()` passes `corners =`. The painter and `Sp360`
ask `Quad.free`. `sayCheck` says `MeasureFlow.checkText(…)`. `DimLayout.mark` is passed `free`. A corner's grab
calls `Quad.drag` with `endTol` and shows `Quad.guide`. "Rectangle" is `Quad.clear`. Nothing in `:app` writes
`.extra =`.
