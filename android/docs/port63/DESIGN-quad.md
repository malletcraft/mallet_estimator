# DESIGN-quad — a mark with four free corners

S2 APK (`mallet_estimator` → `android/`), with the slices S5 QUOTE and S4 MDT need. Written 16 Nov 2026 (IST), branch
`port63-quad`, on `7c49835` (port 63 M1 + M2 over SP0; `:pano` 95 classes, 1,352 tests).

**Basis.** Three readers' reports (the pure code and a mark's box; the screen after M2; S5 and S4), and the code read
again here wherever this file decides something. Nothing was run for this file: no gradle, no S5 test, nothing seen on
a phone. Every "decided by Claude" is judged by Amit on the phone.

**Revised the same day** after the plan critic's nine findings, each checked against the code first. What was
confirmed, what was refuted and what changed is in §8.

**Slice Q1 is built** (§9), and its review's two findings are answered at the end of §9: the corner's stand-off is
1 mm, no longer the 20 mm of a side's run (§1.3), and a snap tag keeps meaning the box's edge (§3.3).

**Slice Q2a is built** (§11, 23 Nov 2026): the pure rules the screen will call. **§11 is the later word wherever it
differs from §2.4, §2.5, §3.1, §3.2, §3.5, §3.7, §5 and §6 "Q2"** — those were written before Amit's "No foto estimation
at all. It confuses user" (`DESIGN-imagemeter.md` §18) and before the panel that slides up took the long-press menu's
place (`DESIGN-imagemeter.md` §6, §8.2, §23): no `≈` is shown for a side, the closing check's sentence quotes no
figure, the control is the panel's act, and the nudges are Fine move's pad.

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
- a side that runs less than `SurveyPrep.MIN_SIZE` (20 mm) in its own direction (rule 3, in mm). This is the floor a
  tab's resize stops at, so a rectangle and a four-corner mark have one smallest side;
- a corner less than 1 mm (`Quad.OUT_MM`) outside the line joining its two neighbours. That one test forbids crossed
  sides, a corner turned inward and a shape flattened to a triangle, and it asks nothing more.

**Why the corner's figure is not the side's** (the review of Q1, finding 1; this bullet first said 20 mm). A W × H
rectangle's corner stands `W·H / √(W² + H²)` outside the line joining its neighbours, which is always under its
shorter side. At 20 mm the rule therefore refused the very shape a mark has after a tab has squeezed it to the floor:
a 2000 × 20 mark could never have a corner moved, not even outward, and a 30 mm square refused some moves that only
enlarged it. Every rectangle a tab can leave stands at least 20 / √2 = 14.1 mm clear, so at 1 mm each of them has
corners that drag. 1 mm is ten steps of the 0.1 mm a corner is placed to. What it allows that 20 mm did not: on a
large mark a corner may be pulled until it is 1 mm short of the line through its neighbours, which looks like a
triangle; its four sides still run 20 mm and are still four figures. `Quad.set` still writes nothing the three
readers would not read back (§1.2 is checked on the parts before the key is stored).

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

`Duplicate | Four corners | Lock | Lock all … | Cancel | Delete`

*(Corrected at the merge with slice F1, 16 Nov 2026 — §10. This line read `Delete | Duplicate | Four corners | Lock |
Lock all … | Cancel`: Q1 was written on the menu of `7c49835`, which began with Delete. F1 made Delete the LAST row.)*

- On a rectangle the row is `sp.shape.four_corners`, "Four corners" (action `SurveyPrepEdit.SHAPE_FREE = "free"`).
- On a four-corner mark it is `sp.shape.rectangle`, "Rectangle" (action `SurveyPrepEdit.SHAPE_BOX = "box"`).
- The row is hidden where the phase holds the mark read-only (`PhaseTools.editable`), like Lock all. On a locked mark
  it is drawn and the act answers the lock's own words, as Delete does.
- With Duplicate hidden by the phase, the row is the menu's FIRST row, before Lock. It is never after Delete, which
  stays last. *(Corrected at the merge, §10: this read "after whichever of the two is left, before Lock", which on
  F1's menu would have put the row after Delete.)*

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
- **What a Line's, an area's or a chain's end snaps to: the mark's box, as today** (the review of Q1, finding 2;
  decided by Claude). `SurveyGuide.snapCoord` reads `x0 x1 y0 y1` alone and is not changed, in Q1 or in Q2. Every box
  edge has a corner of the shape on it, so an end still catches the shape's outermost corner on each side. **A snap
  tag keeps meaning the box's edge:** the tag a chain stores, `FW1 bottom`, is the level of the shape's LOWEST corner,
  which is where the position figure `FW1.s` runs to (§2.3). It is never the sloping side that the reading
  `FW1.bottom` measures. The two cannot be confused in the record: a tag has a space and is kept in a chain's `snaps`;
  a reading key has a dot. A corner INSIDE the box (the example's `q0`, at y 1670) is no snap candidate, so an end
  put level with it is placed by eye and zoom. Making it one needs a tag of its own (the four words are the box's),
  which changes what a chain stores and what S5's as-surveyed rule reads in a tag, so it is not done here (§7).
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
Note, and no row where the phase holds the mark read-only). *(Since the merge with F1, §10, that case reads
`Duplicate|Four corners|Lock|Lock all Door|Cancel|Delete`.)*

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
- `SurveyPrepScreen.kt`, the nudge (`nudgeMark`, `:988-997`) — the review of Q1, finding 1: it takes `snapshot()`
  and then drops `moveMark`'s answer. For a corner part `moveMark` answers false when the shape would be refused
  (§1.3), and nothing moves. Q2 acts on it: `if (!SurveyPrep.moveMark(…)) { hist.removeAt(hist.size - 1); snack(…,
  undo = false); return }`, as the refused restore beside it does (`:976`). So no empty undo step, and one line said.
  Its text is a FIFTEENTH key, keyed in Q2 by §5's rules (English first, a row in `mr-pending.tsv`, `Tx`
  regenerated): `sp.shape.corner_stops`, proposed "{code}: the corner stops here. No side goes under 20 mm, and the
  shape cannot fold." A DRAG says nothing: the corner simply stops (§3.2), and the drag call sites
  (`SurveyPrepScreen.kt:2748`, `SurveyPrep360.kt:465`) may go on dropping the answer.
- `SurveyGuide.snapCoord` (`:pano`): no change (§3.3).
- `MarkSheet.kt`, `SurveyMeasure.kt`, `FaceWriter.kt`, `AsIsUpload.kt`: no change.
- Source pins in `:pano` that move, each with its reason: `PhaseToolsTest` 794 (the menu call's literal ends
  `asPicked)`; it gains `shape = true`) and 801 (the phase guard's count, 2 → 3); `MeasureScreenTest` 335–338
  (`sayCheck`'s body: its `checkEst` and `photoSays` become the one `checkText`, so the screen names
  `MeasureText.photoSays` no longer). Checked and NOT moving: `MeasureScreenTest` 386 (the
  `Marks(…)` literal is FaceWriter's, which Q2 does not change; `Marks` gains `free` as a named, defaulted
  parameter) and `LockTest` (it reads no screen source). No `:app` file may write `.extra =`: every write is a
  `Quad` function.
  **Corrected 16 Nov 2026 (the review of the merge, finding 2):** this list, and every `:app` line number in this
  section, is `7c49835`'s. F1 pinned the same lines of the screen in four more places and moved the lines; the list
  that holds after the merge is §10's note 6, the line numbers its note 7.

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
- **A Line cannot catch a four-corner mark's inner corner** (§3.3). An end snaps to the box's four edges only. On
  the example a Line from the floor to the short upright's foot is placed by eye. If Amit misses it on the phone, the
  remedy is a new snap tag for a corner, with S5.
- **A corner can be pulled almost flat** (§1.3): 1 mm outside the line through its neighbours. Nothing breaks; whether
  the stop should come sooner on a large mark is his to judge on the phone, and the one constant is `Quad.OUT_MM`.
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
1,352 tests to 97 classes and 1,390 tests, all green (the totals of `pano/build/test-results/test`), and to 1,392
with the review's two cases (below); `:app` is compiled only by CI and nothing has been seen on a
phone. This section says what exists, for whoever builds Q2, and where the build went past the design's wording.

**What exists.**

| File | What it now has |
|---|---|
| `pano/Quad.kt` (new) | `KEY PARTS SIDES DIAG CLOSE_MM CLOSE_PART OUT_MM`; `mayHave`, `read`, `identity`, `of`, `corners`, `cornerOf`, `free`, `set`, `clear`, `drag` → `Drag(p, snap)`, `guide`, `straight`, `square`, `spanW`, `spanH`, `touch` → `Touch`, `sideWhat`, `asked`, `closing` → `Closing`, `sideCodes` |
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
9. **WITHDRAWN by the review's finding 1 (below); it described the first build and understated it.** It read:
   "§1.3, read to the letter, refuses any shape under about 28 mm square. A 20 mm rectangle's corner is 14 mm outside
   the line joining its neighbours. Such a mark keeps its eight tabs' resize; once larger, its corners drag." The real
   boundary was any mark with a side at the 20 mm floor, whatever its length (2000 × 20 refused every corner move),
   and partial refusals up to about 30 mm square. Since the fix no rectangle a tab can leave refuses a corner.
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
`.extra =`. The nudge acts on `moveMark`'s false for a corner part: no undo step, and `sp.shape.corner_stops` said
(§6, Q2). `SurveyGuide.snapCoord` is not touched (§3.3).

### Review fixes (16 Nov 2026)

A reviewer read commit `f451363` and raised two findings, both minor. Each was checked against the code before
anything changed. **Both are confirmed; none is declined.** One local commit, `:pano` only: 97 classes, 1,392 tests,
all green (the totals of `pano/build/test-results/test`). No `:app` file changed, `:app` is compiled only by CI, and
nothing has been seen on a phone. No text was added, so `i18n/` and `Tx` are as Q1 left them.

1. **A mark with a 20 mm side could never have a corner moved, and small marks refused moves that only enlarged
   them; item 9 above understated it. CONFIRMED,** by a test written first and run on the reviewed code: a
   2000 × 500 mark squeezed by its top tab to the floor (2000 × 20) answered false to `moveMark(…, "q2", 0, +5)`.
   The cause is as the reviewer gives it: `sound` held a corner to `MIN_SIZE` outside its neighbours' line, and a
   rectangle's corner stands `W·H / √(W² + H²)` out, under its shorter side (19.999 mm at 2000 × 20, 19.6 at
   100 × 20, 14.1 at 20 × 20).
   **Fixed in the code, the reviewer's second way:** the 20 mm stays the floor of a SIDE's run, and a corner's
   stand-off is its own figure, `Quad.OUT_MM` = 1 mm (§1.3, with what it allows). The reviewer's first way, 20 mm less
   half the grid, was not taken: it passes 2000 × 20 and still refuses 100 × 20 and the 30 mm square. Keeping the
   rule and correcting item 9 was not taken either: a rule that refuses the shape the mark already has, on a mark
   whose tabs are hidden while its corners are free, is a fault and not a boundary to document.
   Tests, `QuadTest`: "every rectangle a tab can leave has corners that move -- the thinnest too" (the squeezed mark's
   q2 up 5 and q0 down 1 to the last corner; a corner slid along the thin side; every corner of six floor-sized
   rectangles pulled outward; the reviewer's 30 × 30 case and a 20 × 20 pulled into a 20/40 trapezoid; a side under
   20 mm still refused; the two figures pinned apart). It fails on the reviewed code at "2000 x 20, q2 up 5". The
   old pin "14 mm outside is refused, 21 mm is a shape" moved with the rule: 0.7 mm refused, 1.4 mm and 14 mm shapes.
   **For Q2, recorded in §6:** the nudge takes `snapshot()` and drops `moveMark`'s Boolean (`SurveyPrepScreen.kt:995`),
   so a refused corner nudge would leave an empty undo step and say nothing. Q2 drops the step and says one line,
   whose key Q2 adds. That is `:app`, and nothing here could change or test it.
2. **Line and chain ends snap to a four-corner mark's bounding box only, and the design never said so. CONFIRMED.**
   `SurveyGuide.snapCoord` builds its candidates from `x0 x1 y0 y1`, Q1 did not touch it, and no section named it
   among the box's readers. **Decided, not changed** (the reviewer asked for the decision, and no code change): a
   snap tag keeps meaning the box's edge on a four-corner mark, for the reasons in §3.3. The alternative, the real
   corners as candidates, adds only the corners inside the box, since each box edge already has a corner on it, and
   those have no tag to carry: `FW1 bottom` for q0's level would make one tag mean two levels, and a new tag is a
   change to the wire that this design does not name. It is listed in §7 for Amit's judgement on the phone.
   `snapCoord`'s own comment now says it, and `QuadTest` pins it: "a Line's or a chain's end snaps to a four-corner
   mark's box -- the snap tag names the box's edge, never a side" (the four edges with their tags; q0's level catches
   nothing; over the whole wall the four-corner mark snaps exactly as the rectangle of its box; no side's reading key
   is a tag, and no tag passes S5's key grammar). This test is a pin of the decision: it passes on the reviewed code
   too, because the finding is about what the design left unsaid, not about what the code does.

## 10. Merged with H1 and F1 (16 Nov 2026)

Slice Q1 was built on `7c49835` while two slices were built beside it on `batch-next` and went to the phone (Play
0.3.341): **H1** `8455c77` (the one being measured is gold) and **F1** `136b4c7` + `380c72d` (the chain tool off, D2
alone at the bar's end, delete apart and last, a Tape / Line / area moved by its body) — DESIGN-port63 §11. This
section is the merge of `030a2f7f5` into `port63-quad`: what git could not decide, what was decided where the two
meet, and what Q2 inherits. `:pano` after the merge: **101 classes, 1,435 tests, 0 failures, 0 errors** (the totals of
`pano/build/test-results/test`; batch-next alone 98 and 1,388, Q1 alone 97 and 1,392, the merge's own `QuadMeetsF1Test` 7).
The review of the merge added one test to `QuadMeetsF1Test` (note 6 below): 101 classes, **1,436** tests since.
**`:app` is batch-next's own, unchanged by the merge, compiled only by CI, and nothing of the merge has been seen on a
phone.** `Tx` was regenerated (no difference: batch-next added no key); `unkeyed.tsv` is empty; `mr-pending.tsv`
keeps its 27 keys; no Marathi value changed.

**The one textual conflict** was `SurveyPrepEdit.menuItemsOf`: F1 gave it `chains` and Q1 gave it `shape`, each as a
sixth parameter with a default. It now has both — **`chains` sixth, `shape` seventh** — because batch-next's callers
and tests pass `chains` by position and the phone runs them; Q1's one test names `shape =`.

**Where the two meet — each decided by the two sides' own stated rules, each held by `QuadMeetsF1Test`** (every
decision undone on purpose turned its test red):

| Meeting point | F1 / H1 says | Q1 says | Decided |
|---|---|---|---|
| The menu's rows | the delete row is the LAST row, apart | the shape row stands after Duplicate, before Lock (§3.1) | `Duplicate · Four corners · Lock · Lock all … · Cancel · Delete`. Where the phase hid Duplicate the shape row is the FIRST row. Never after Delete: §3.1's "after whichever of the two is left" was written on a menu that began with Delete, and is corrected in place. A first row that is the shape row suits F1's reason for the order: "Four corners" changes nothing in the record and "Rectangle" is one ↶ step, as Duplicate is |
| The press, 2 · Mark | the PICKED Tape's / Line's / area's body comes after the picked item's handles, a chain's point and a figure, BEFORE the marks | a free mark's four corners stand where the eight tabs do; its body counts only inside its shape (§3.4) | Both, in F1's order. A tape laid across a four-corner window is moved by its band and the window by the rest of its SHAPE; the empty corner of its bounding box is neither. One thing is picked at a time, so the body is never handed with a picked mark |
| The floating trash | the disc is 16 dp from the picked item's OWN handles and off every figure; no place clear, no disc | a free mark's handles are its four corners (§3.1); the trash keeps the bounding box (§3.4) | The disc is placed beside the BOUNDING box and kept 16 dp from the four corners as they stand, not from eight tabs that are not drawn: `SurveyPrepTouch.keepOfMark(bb, spot, corners = null)`, and on the 360 `Pano360.trashOf(…, free = false)` through `tabsOf(e, free)`. Both default to what the screen passes today |
| The 360 | `Step.BODY` after the chain points, before the marks | `tabsOf(e, free)` gives `q0…q3`; `markPoly` is the real shape (§3.6) | The same order as the photo: ends, tabs (the free corners), chain points, the picked tape's body, the marks by their real outline |
| Gold | the figure being measured: a gold plate with dark bold text whatever its state, a gold line, and its `!` in the plate's dark ink | two new roles, `SIDE` and `DIAG`; the closing check's `!` on the sloping side (§2.4) | Gold asks nothing of the role. A side being measured is the gold one like any figure, has a line for the gold line to follow, and its closing `!` is drawn in the gold plate's ink |

**`RectanglePinTest` after the merge.** Its dump was run on a plain checkout of `030a2f7f5` (a temporary worktree,
removed afterwards) and on the merge: the two files are byte for byte the same. Seventeen sections are still held to
`7c49835` — batch-next prints them exactly as `7c49835` did. **One was re-taken from batch-next, `flow`**: F1 put the
menu's delete row last, and with that row put back first the section has `7c49835`'s digest again, so nothing else in
it moved. **Three sections are new and held to `030a2f7f5`** — `gold`, `trash`, `body` — because the dump taken on
`7c49835` never looked at what H1 and F1 built, and a merge could have lost either without this test noticing.

**For Q2, beyond §9's list.**

1. **THE SIX-ROW MENU DOES NOT FIT F1'S PLACEMENT IN LANDSCAPE.** `DeleteGuard.menuTop` keeps the delete row 16 dp
   from the finger that opened the menu. With five rows (248 dp) that holds on every window of the Mi A3. With the
   shape row the menu is 288 dp, and it holds only on a window 368 dp tall or more: on the landscape window (336 to
   360 dp) a press low on the photo has the delete row under the finger again (0 dp at 336 and 348, 8 dp at 360).
   Nothing draws six rows today. **The commit that turns `shape` on has to settle it** — a shorter menu in landscape,
   or the row somewhere else — and `QuadMeetsF1Test` holds the figures so that commit cannot miss them.
   **SETTLED by port 63 slice I2b (23 Nov 2026), ahead of Q2: the long-press menu is gone.** A long press opens the
   panel that slides up (`DESIGN-imagemeter` §6, §8.2), and the menu's rows are its acts (`MarkSheet.acts`): the shape
   row is line 2's FIRST cell, in a column that stood empty, Delete stays in the last column 24 dp from the act beside
   it, and in landscape the one line of six ends 16 dp short of Done. Nothing is placed by where the finger was, so
   there is nothing left for Q2 to settle here; `QuadMeetsF1Test`'s second test holds the panel's figures instead of
   the menu's. `DeleteGuard.menuTop` and `menuClear` went with the menu.
2. **The menu's one call** passes `shape = true` BY NAME: the sixth parameter is `chains`. **Since slice I2b it is
   the panel's two calls:** `MarkSheet.panel(tx, prep, pn, quoteStage, asPicked)` (what is drawn) and
   `MarkSheet.actStep(tx, prep, pn, act, quoteStage, asPicked)` (what a tap on an act does) each gain `shape = true`
   by name, and `panelAct`'s `when (step.act)` gains the two shape acts where it reads `else -> Unit` today.
3. **The trash:** `drawnMarks()`' corners go to `keepOfMark(bb, spot, corners)` as well, and `Sp360` passes its one
   `free` to `trashOf` beside `tabsOf`. **Since slice I2b there is no floating trash**, on the photo or on the 360
   (`trashSpot`, `keepOfMark`, `keepOfLine` and `Pano360.trashOf` are deleted). What stands beside the picked mark is
   its CODE CHIP: `drawnMarks()`' corners go to the chip's handles instead,
   `SurveyPrepTouch.handlesOf(bb, spot, corners)` in `SurveyPrepDraw.drawMarks` — the chip keeps the bounding box and
   stands 16 dp clear of the four corners as they stand (`QuadMeetsF1Test`'s third test). The 360 draws no chip.
4. **H1's gold frame** round the mark being measured is drawn by `:app` from the mark's box
   (`SurveyPrepDraw.measuring`). For a four-corner mark Q2 draws it round the shape, as §3.4 says of the outline and
   the dim hole.
5. **A four-corner mark's own body** is the mark's, moved by `moveMark` as always. F1's `bodyPress` / `bodyMove`
   are a Tape's, a Line's and an area's and are not asked for a mark.
6. **THE SOURCE PINS THAT MOVE IN Q2 — §6's list is `7c49835`'s, and F1 pinned the very lines Q2 rewrites in four
   more places** (the review of the merge, finding 2). Each line of `:app` that Q2 rewrites, and every `:pano` test
   that holds it as it is today. The test files are held: `QuadMeetsF1Test`'s last test lists these lines and fails
   when a test that pins one is not named in this note. The line numbers are this commit's, and nothing holds them.

   | The line of `:app` today | Q2 | Pinned by |
   |---|---|---|
   | the panel's drawing call, `MarkSheet.panel(tx, prep, pn, quoteStage, asPicked)` (`SurveyPrepScreen.kt`; until port 63 slice I2b the menu's one call, `SurveyPrepEdit.menuItemsOf(tx, prep, c.id, quoteStage, asPicked)`) | gains `shape = true` (note 2) | `PhaseToolsTest`; `FeaturesTest`; `DeleteGuardTest`; `MarkSheetTest`; `QuadMeetsF1Test` |
   | the panel's act call, `MarkSheet.actStep(tx, prep, pn, act, quoteStage, asPicked)` (`SurveyPrepScreen.kt`, new in slice I2b) | gains `shape = true` (note 2) | `PhaseToolsTest`; `FeaturesTest`; `DeleteGuardTest`; `QuadMeetsF1Test` |
   | the phase guard, `if (!PhaseTools.editable(prep, mid, quoteStage)) { phaseRefuse(` | a third beside the padlock's and Duplicate's: the shape's act, 2 → 3 | `PhaseToolsTest` |
   | ~~the menu's height before it is first measured, `(232 + DeleteGuard.CLEAR_DP).dp.toPx()`~~ | **gone in port 63 slice I2b** with the long-press menu (note 1) | — |
   | the chip's handles, `SurveyPrepTouch.handlesOf(bb, spot)` in `SurveyPrepDraw.drawMarks` (until slice I2b the trash's keep, `SurveyPrepTouch.keepOfMark(bb, …)`, in the screen) | gains the corners (note 3) | `MeasureScreenTest`; `NotePinTest`; `QuadMeetsF1Test` |
   | ~~the 360's trash, `Pano360.trashOf(v, fr, prep, tool, sel?.id?.takeIf { … }, dpx.toDouble())`~~ | **gone in port 63 slice I2b**: nothing stands beside the picked item on the 360 | — |
   | ~~`sayCheck`'s two lines, `MeasureFlow.checkEst(prep, c) { ab(it) }` and `say(MeasureText.photoSays(tx, est))`~~ | **gone in port 63 slice I1b** (no photo estimate is said: `DESIGN-imagemeter` §18), so Q2 has nothing to rewrite here | — |
   | the dim's hole, `val hole = if (SurveyPrep.isSpot(sm.t)) spotOf(sm, sq, m, o.dp).let { d ->` (`SurveyPrepDraw.kt:502`) | the hole is the shape's for a four-corner mark | `NotePinTest` 116 — it moves only if this opening line is rewritten; a branch added after the spot's leaves it |
   | the plates, since port 63 slice I1b `PhotoFigures.layout(tx, o.prep, s, o.a, o.b, pk, toPx, du, dp, textW, o.live, o.ready)` in `SurveyPrepDraw.figures` (it replaced `DimLayout.mark(…)` there) | is passed `free` | `QuadMeetsF1Test` 357; `MeasureScreenTest` (I1b's painter pins) |

   **And the whole of `QuadMeetsF1Test`'s test "the screen's calls are batch-next's own …" (341–363) is rewritten or
   goes in Q2**: besides the lines above it holds `corners =` at 0 in the screen (349), `Pano360.tabsOf(e)` twice in
   the 360 (355), and no `Quad.`, `SHAPE_FREE`, `SHAPE_BOX`, `shapeRow`, `free = ` or `freeId` in any `:app` file
   (359–362). The test after it, which holds this note, goes with it.

   **Checked and NOT moving — some of them only if Q2 puts a new named argument AHEAD of a pinned tail:**
   `PhaseToolsTest` 825 (`stage = quoteStage, asPicked = asPicked, onAs = { cycleAs() })` twice; one of the two is the
   last line of the `Sp360(…)` call, so `freeId` goes before `stage =`); `MeasureScreenTest` 391 (`scope = figScope,
   live = live, keep = figKeep)`, the tail of the screen's `Marks(…)`: `free` goes before `scope =`); `NotePinTest`
   114 (`spot = …`: `corners = …` goes before it, as §6 says), 117 and 118 (the guards on the line before each
   `tabsOf` call in the 360); `FeaturesTest` 606 (the first line of the `Sp360(` call); `MeasureScreenTest` 192
   (`hit(drawnMarks(), …)`), 299–300 (`grabbed`'s `MeasureFlow.own` line), 363 (`DimLayout.mark(` once) and 386
   (FaceWriter's `Marks(`); `SheetMarksTest` 401 (`drawMarks`' signature: `Marks` gains `free`, the function nothing);
   `DeleteGuardTest` 307 (the nudge's row) and 347 (`DELETE -> deleteMark(mid)`); `LockTest` (it reads no screen
   source).

   **After port 63 slice I2b (23 Nov 2026).** The table above is as that slice left it; the two paragraphs under it
   are older and name lines I2b deleted — the nudge's row and the menu's `DELETE -> deleteMark(mid)` branch are gone
   (Fine move's pad and the panel's Delete, `DESIGN-imagemeter` §23.3), and `MeasureScreenTest`'s pin on the tail of
   the screen's `Marks(…)` now ends `shown = if (which == "main") shown else null), mm, u)`: `free` goes before
   `chip =`. `QuadMeetsF1Test`'s "the screen's calls …" test pins, since I2b, the panel's two calls and the chip's
   `handlesOf(` in place of the menu's call and the two trash keeps.
   **BUILT, slice Q2b (23 Nov 2026).** Every line in the table above is rewritten as it said, and the test that held
   this note to naming their pins is moved with them: the lines Q2 WROTE, and every test that pins one, are §12.5.
   Nothing reads this note any more; it stands as the record of what moved.
7. **`:app`'s line numbers in §3 to §9 are `7c49835`'s** (the same finding). H1 and F1 moved them. As they stand at
   this commit — and to be re-read at build time all the same, since batch-next moves on: the menu's call
   `SurveyPrepScreen.kt:2228` → `:2258`; the long press that sets `ctx` `:1400` → `:1410`; `Drawn(…)` `:2696` →
   `:2732`, its `spot =` on `:2733`; the `checkEst` call `:530` → `:534`; `nudgeMark` `:988-997` → `:998-1007`, its
   dropped `moveMark` `:995` → `:1005`, and the refused restore beside it `:976` → `:980`; the drag's two call sites
   `SurveyPrepScreen.kt:2748` → `:2819` and `SurveyPrep360.kt:465` → `:467`; `tabsOf(e)` at `SurveyPrep360.kt:299`
   (not moved) and the painter's at `:830` → `:853`; `SurveyPrepDraw.kt:917` → `:947` and `:1009` → `:1045`.
8. **batch-next has moved one commit past what was merged** (the same review, finding 3). The merge's parent is
   `030a2f7f5`; `batch-next` is `b475ae8` since — DESIGN-imagemeter §18 and its pictures redrawn: 18 files, all under
   `android/docs/port63`, no Kotlin, no i18n table, no `:app` file. So `port63-quad` is NOT batch-next's tip plus Q1,
   and its next meeting with batch-next is a true merge, never a fast-forward. Mechanically that merge is docs alone:
   no `:pano` total moves by it, nothing is re-pinned and `Tx` is not regenerated — and its commit says so.
   **What that commit SAYS is not docs-only for this design.** §18 is Amit's ruling of 16 Nov 2026 — "No foto
   estimation at all. It confuses user" — and it overrides every `≈` on the phone and names
   `sp.measure.photo_says_x` as a text that goes. This design was written before it: §2.5's example gives an unread side
   the photo's `≈` figure, §3.7's plan label and elevation fall back to `≈`, and §2.4's `checkText` says the photo's
   sentence as its second case (the closing sentence's own `≈` is worked from READINGS, not from the photo; whether it
   stays is not decided here). **Q2 is re-read against §18 before it is built.** Nothing of Q1 is changed on its
   account in this commit: Q1 draws nothing, and the ruling is batch-next's to build first.

## 11. Q2 re-read against the code and against §18 (23 Nov 2026)

Slice Q2 switches the four-corner mark ON. It is built as I1 and I2 were, in two commits. **Q2a (this section's
commit) adds or corrects every pure rule the screen will call, in `:pano`, with its tests, BESIDE what stands:** no
`:app` file is edited, and nothing `:app` calls today answers differently for any record a phone can hold (no build
writes `quad` yet). **Q2b switches the screen over.** Branch `port63-q2`, over I2's review fix (`e892bb2`). *(Q2b is built: §12.)*

`:pano` after Q2a: **112 test classes, 1,591 tests, 0 failures, 0 errors, 0 skipped** (the totals of
`pano/build/test-results/test`; before: 111 and 1,575). `:app` is compiled only by CI, and nothing has been seen on a
phone.

Line numbers are this commit's. `SPS` = `app/SurveyPrepScreen.kt`, `SPD` = `app/SurveyPrepDraw.kt`, `SP360` =
`app/SurveyPrep360.kt`; `pano/…` as everywhere in this file.

### 11.1 Decided by Claude for this slice — each can be turned round by Amit

1. **THE CONTROL IS THE PANEL'S ACT, AND NOTHING ELSE.** Pick the mark, open the panel (its code chip, or a long
   press), `Four corners`; on a four-corner mark the same cell reads `Rectangle`. No button anywhere else. This
   settles §7's question 1: the long-press menu it weighed is gone, and the panel's act is one tap further than a
   button beside the padlock would have been — if he does not find it, a button is the remedy.
2. **NO FIGURE NOBODY MEASURED, for the four sides and the diagonal exactly as for every other dimension**
   (`DESIGN-imagemeter.md` §18). A side that is not read shows NOTHING while its mark is not picked, a quiet `?` on its
   own side while the mark is picked, the gold `▶?` for the one being measured; a read side shows its number alone,
   along its side. So §2.5's `≈` column, §3.5's "side estimates" and §3.7's `≈` fallbacks are NOT built as written.
   On the Plan tab and the six drawings a four-corner mark does what a RECTANGLE does there today — the same
   `SurveyPlan.figure`, which still prints `≈` for an unread figure of any mark until slice M3 changes both at once.
   Never a new `≈`.
3. **THE CLOSING CHECK STAYS, AND ITS SENTENCE CARRIES NO FIGURE.** It compares readings with readings (§2.4), so it
   is not an estimate — but what the other three sides MAKE the fourth is a figure nobody measured, so it is not
   said. The amber `!` stands on the sloping side's plate as designed; the note line says
   `sp.measure.sides_say_x`, re-worded: **"The four sides do not agree — see the ! on the {side}. Check the
   readings, or the corners."** The per-figure check of a side is §18's GROSS GUARD (more than twice, or less than
   half, the side as drawn; `sp.measure.check_reading`), never the 150 mm / 15 % rule and never a sentence that
   quotes the photo.
4. **A REFUSED CORNER NUDGE in Fine move moves nothing, takes NO undo step, and says one line**
   (`sp.shape.corner_stops`: "{code}: the corner stops here. No side goes under {mm} mm, and the shape cannot
   fold."). **A refused corner DRAG says nothing:** the corner simply stops under the finger.
5. **THE SAVED `-survey.jpg` PICTURES ARE NOT CHANGED** in Q2: their old painter, `dims`, stays pinned byte for byte
   until slice M3. A four-corner mark's saved picture shows its true outline, its code and its position figures, and
   no side figures yet (§3.9). The build note says so in one line.
6. **An opening that is square but out of true stays a rectangle** measured by width and height, and **curved tops
   are not built** (§7's questions 2 and 3, their recommended answers).

### 11.2 Every thing the screen must do: the rule that answers it, and the site that will call it

"✔" = existed at `e892bb2` and holds as it is; "**new**" / "**changed**" = this commit (§11.4).

| # | The screen must | `:pano` answers | `:app` site that will call it |
|---|---|---|---|
| 1 | keep `freeId`, the mark whose corners are free with no key stored | **new** `Quad.freed(freeId, picked, part)` (`pano/Quad.kt:172`) | a new `var freeId` beside `part` (`SPS:344`); one line after each `part = NudgePad.touched(…)` / `part = NudgePad.BODY`: `took` (`SPS:676`), `landed` (`:699`), `undo` (`:800`), `settled` (`:887`) |
| 2 | know, for ANY mark, whether its corners are free — one value for the photo, the press, the layout and the 360 | ✔ `Quad.free` (`:148`); **new** `Quad.freeOf(freeId, picked, part)` (`:185`), `Quad.freeIn(p, id, …)` (`:190`) | `SurveyPrepDraw.Marks` gains `free: (SurveyPrep.Mark) -> Boolean = { false }` BEFORE `chip =` (`SPS:3436`; §10 note 6); `SpPhoto` (`SPS:2465`) and `Sp360` (`SP360:195`, before `stage =`) are handed the same value |
| 3 | draw the right handles | **new** `Quad.handles(e, shows, free)` → `NONE` / `TABS` / `CORNERS` (`:209`) | `drawMarks`, where the eight tabs are drawn (`SPD:671-682`): four discs of `SurveyGuide.END_R_DP` at `Quad.corners(e)` for `CORNERS`, each solid when `NudgePad.solid(o.nudge, e.id, Quad.PARTS[i])` |
| 4 | take a press: the held mark's four corners where the eight tabs stand; a body press only INSIDE the shape | ✔ `SurveyPrepTouch.hit` with `Drawn.corners` (`pano/SurveyPrepTouch.kt:97`, `:176`); **new** `Quad.freeCorners(e, free)` (`:222`) | `drawnMarks()` (`SPS:2816-2817`): `corners = Quad.freeCorners(e, free(e))?.map { toS(sq.x(it[0]), sq.y(it[1])) … }` BEFORE `spot =` (NotePinTest's literal) — for EVERY mark of the face |
| 5 | keep the code chip 16 dp clear of the corners as they stand | ✔ `SurveyPrepTouch.handlesOf(bb, spot, corners)` (`:258`), `codeChip` (`:303`), `chipLift` (`:281`) | `SPD:562-563`: `hand` is `Quad.handles(…) != NONE`; `handlesOf(bb, spot, Quad.freeCorners(sm, o.free(sm))?.map { … })` |
| 6 | drag a corner: clamp, snap straight within 5° and the snapping distance, stop where the shape would break, show the guide | ✔ `Quad.drag(e, o, i, dx, dy, a, b, tol)` (`:332`), `Quad.guide(s, r)` (`:355`), `Quad.cornerOf(part)` (`:137`), `SurveyGuide.endTol` (`pano/SurveyGuide.kt:202`) | `markGrab`, `Pick.Drag.MOVE` (`SPS:2907-2913`): for a corner part a grab built like a Line end's (`SPS:2986-2996`): `gate = true`, `focus` on the corner, `cancel = { onEndGuideS(null) }`, `Quad.drag(…, tol)?.let { r -> onEndGuideS(Quad.guide(tab, r)) }`. A refused drag (null) says nothing |
| 7 | on the 360: the same corners, the same drag | ✔ `Pano360.tabsOf(e, free)` (`pano/Pano360.kt:273`), `markPoly` (`:292`); `SurveyPrep.moveMark` / `snapMark` take a corner part | `SP360:299` and `:815`: `Pano360.tabsOf(e, free(e))`; the painter draws discs for a free mark; the drag at `SP360:445-456` is unchanged |
| 8 | draw the outline, the Dropped stroke, the dim hole and the gold frame ROUND THE SHAPE | ✔ `Quad.of`, `Quad.corners` (`:124`, `:131`); **new** `Quad.grown(pts, by)` (`:237`) | `SPD:656-658` and `lifeOutline` (`SPD:410-418`): a `Path` through the four corners when `Quad.of(e) != null` (the Dropped stroke `q0`→`q2`); the hole (`SPD:588-595`): `Quad.grown(cornersPx, 6 u)` as a branch AFTER the spot's (NotePinTest pins the opening line); `measuring` (`SPD:499-512`): `Quad.grown(cornersPx, ACTIVE_GAP_DP)` |
| 9 | show its figures with no photo estimate, each side's plate on its own side, the diagonal inside, the held mark's corners kept clear | ✔ `PhotoFigures.ofMark` / `layout(…, free = …)` (`pano/PhotoFigures.kt:82`, `:119`), `MarkDims.of` / `read` (`pano/MarkDims.kt:338`), `DimLayout.line` / `outward` / `placeShown` / `cornerBoxes`; **changed** `DimLayout.markShown` asks `Quad.handles` (`pano/DimLayout.kt:325`) | `figures` (`SPD:1053`): `PhotoFigures.layout(…, o.live, o.ready, free = o.free, ask = o.ask)`. The painter needs nothing else: `dimLine` already breaks a sloping line at its plate (`SPD:1105-1120`) |
| 10 | walk inside the mark: bottom, right, top, left, the diagonal if asked, then the type's own | ✔ `Quad.asked` (`:438`) under `SurveyPrep.measures`, so `MeasureWalk`, `Pick.body` / `next`, `Landing.after` and `MeasureFlow.firstOpen` follow it | nothing new: the gold walk and `▸` are the screen's own, as built |
| 11 | flag a reading that cannot be this side | ✔ `MarkDims.guard` reads `Role.SIDE` and `Role.DIAG` (`pano/MarkDims.kt:367-371`), in the squared plane only | nothing new |
| 12 | say the closing check, with no figure | ✔ `Quad.closing` (`:467`), the `!` in `MarkDims.read`; **changed** `MeasureFlow.guardText` (`pano/MeasureFlow.kt:176`), **new** `guardMsg` (`:165`), `MeasureText.sidesSay` (`pano/MeasureText.kt:46`); **changed** `Landing.after` / `keyed` (their note) | nothing new: `took` (`SPS:686`) and `landed` (`SPS:703`) already say what these answer |
| 13 | draw the shape row in the panel, and refuse it on a locked mark | ✔ `MarkSheet.acts` / `panel` / `actStep(…, shape = true)` (`pano/MarkSheet.kt:566`, `:646`, `:694`) | `SPS:2314` and `SPS:1291`: each call gains `shape = true` BY NAME |
| 14 | run `Four corners` / `Rectangle` | **new** `MarkSheet.shapeStep(…)` → `Shaped` (`pano/MarkSheet.kt:743`, `:721`); ✔ `Pick.shaped` (`pano/Pick.kt:284`), `MarkSheet.stepsToDrop` | `panelAct`'s `when (step.act)` (`SPS:1303-1310`), where the comment "(the shape row … is slice Q2's …)" stands: one branch for both acts (§11.5) |
| 15 | Fine move on a corner; say why a corner did not move | ✔ `NudgePad.parts` / `aim` / `solid` / `press` on `q0…q3`; **changed** `NudgePad.start(…, free)` (`pano/NudgePad.kt:163`), `Pressed.said` (`:219`), **new** `cornerStops(code)` (`:228`) | `fineMove` (`SPS:1261`): `…, stepTaken, free = Quad.freeIn(prep, mid, freeId, pick.picked, last))`; `finePress` (`SPS:1273-1275`): `r.said?.let { m -> say(m(tx)) }` |
| 16 | leave the saved pictures as they are | — | `dims` (`SPD:1202`) is not touched (decision 5) |

Kept by rules that need no call of their own: a locked mark's corners do not move and a mark of an earlier phase is
read-only (`Pick.drag`'s `LOCKED` / `READ_ONLY` branches, `SPS:2901-2906`, already ahead of the grab); duplicate
carries the shape (`SurveyPrepEdit.duplicate`); undo shares `extra`; the record on the wire is §1 exactly — the one
key `quad`, the reading keys `.bottom .right .top .left .diag`, and nothing else new leaves the phone.

### 11.3 Where Q1's model still answered a photo estimate, a `≈`, the 150 mm / 15 % check or the measure bar's words

| Where | At `e892bb2` | After Q2a |
|---|---|---|
| `Quad.asked`: a side's and the diagonal's `est` (the distance corner to corner) | carried on the `Measure`, as every dimension's is | unchanged in the model, and said plainly in the code: it is the side AS DRAWN, which the gross guard compares a reading with (`MarkDims.guard`), and nothing a person reads on the phone |
| The photo, 2 · Mark — an unread side or diagonal | already §18's since slice I1b: `PhotoFigures.ofMark` → `MarkDims.read` answers MEASURED or UNKNOWN, never ESTIMATE; nothing unpicked, `?` picked, `▶?` gold | unchanged, and now held for a four-corner mark by name (`QuadScreenTest`: no figure of it, in any state, reads `≈` or shows a number nobody measured) |
| The message line — the closing check | **said `sp.measure.check_reading` ("This reading does not match the mark …") on all four side codes**: I1b deleted Q1's `sidesSay`, whose text quoted `≈2466 mm`, and put the gross guard's sentence in its place — which says the wrong thing: the reading may be right, and it never names the side with the `!` | **the closing check has its own sentence again, with no figure** (decision 3): `MeasureFlow.guardMsg` / `guardText`, and `Landing`'s note |
| The message line — the per-figure check of a side | already the gross guard (`MarkDims.guard` reads `SIDE` and `DIAG`); the 150 mm / 15 % rule (`MarkDims.check`, `photoCheck`) has had no screen caller since I1b | unchanged; held by `QuadScreenTest` on a side and on the diagonal |
| `MeasureFlow.guardText`'s order "which sentence, when a code could say two" (§2.4) | lost with `sidesSay`: one sentence for everything | restored with the gross guard in the photo sentence's place: on the sloping side's own code the closing sentence; on one of the other three the guard's when THAT reading fails its own guard, else the closing one |
| `MarkDims.value` / `shown` / `style`, `DimLayout.mark` / `placeBy` — the level-plate form with the ESTIMATE state | no `:app` caller (the placing rule's own tests are written in it); they still answer `≈` for a side | unchanged: not this slice's to delete. `MeasureWalkTest`'s pin "a wished unit … shows its side estimates" is MOVED: on the photo such a mark shows no figure at all (§18 rule 6), and the old line is kept beside it as the model's |
| The Plan tab and the six drawings (`SurveyPlan`: the floor plan's label, a dimension along each side, a window's head) | `≈` for an unread figure — of a rectangle and of a four-corner mark alike | unchanged (decision 2): M3 changes both with the saved pictures |
| The saved `-survey.jpg` pictures (`:app` `dims`) | draw `w` and `h` only; no side figure, no `≈` for one | unchanged (decision 5) |
| `measure.side_*`, `measure.diagonal*`, `measure.to_nearest_corner` — "the bar's first line" (§5) | the bar went in I1b; the words are still the checklist's (`Measure.what`), read on the phone only as a gold `⊥` chip's name, which a side never is | `measure.side_*` are read again: `MeasureText.sidesSay` names the sloping side with them. The diagonal's and "to its nearest corner" have no on-screen reader (S5's checklist mirrors them in its own words); they stay keyed, for the Marathi pass and a screen reader's label |
| §3.5 "The bar says the side's words" · §3.1 "the long-press menu gains one row" · §3.2 "the row then reads `FW1` over 'corner' and shows the nudges" | gone with the bar (I1b), the menu and the row (I2b) | the gold plate on its own side; the panel's act; Fine move's pad (`DESIGN-imagemeter.md` §8.2) |

### 11.4 What was missing, and is built — each with its test

Every function below is in `:pano`. "Fails without it": 21 one-line faults were planted in these functions, one at a
time, and each turned at least one test red (the list is in the commit message).

| Function, exactly | What it answers | Test |
|---|---|---|
| `Quad.freed(freeId: String?, picked: String?, part: String?): String?` | `freeId` after ANY event: null with nothing picked; `picked` when `part` is `q0…q3` (a corner grabbed frees the mark); `freeId` while it is still the picked one; null once the selection left it | `QuadScreenTest`, "freeId lives by one rule": the table, and the whole life as the screen runs it (a door from the file → `Four corners` → a corner dragged out and back through the box, the handles staying four corners with the key gone → a body press → another mark picked → eight tabs again) |
| `Quad.freeOf(freeId: String?, picked: String?, part: String?): (SurveyPrep.Mark) -> Boolean` | `Quad.free` for every mark of the screen at once: `part` counts for the picked mark alone | "one free for every mark of the screen" |
| `Quad.freeIn(p: SurveyPrep.Prep, id: String, freeId: String?, picked: String?, part: String?): Boolean` | the same by id (false for a Tape, a Line, a chain, nobody) — what Fine move is told as it starts | the same test |
| `enum class Quad.Handles { NONE, TABS, CORNERS }`; `Quad.handles(e: SurveyPrep.Mark, shows: Boolean, free: Boolean): Handles` | none unless the mark `shows` handles (picked, held, not locked: the caller's own condition) and never on a spot; four corners where free; else eight tabs. `DimLayout.markShown` asks it too, so what the layout keeps clear is what the painter draws | "which handles a mark shows" (the rule, and `markShown`'s `tabs` / `corners` agreeing with it over every picked / held / free) |
| `Quad.freeCorners(e: SurveyPrep.Mark, free: Boolean): List<DoubleArray>?` | q0…q3 in mm when free (the box's own four with no key), else null — mapped to the canvas by the caller for `Drawn.corners` and `handlesOf` | "the press and the chip": each corner of the held mark is the part it drags; just inside the top side is the body, not a `t` tab; the empty corner of the bounding box is nobody's; the chip 16 dp clear of all four corners |
| `Quad.grown(pts: List<DoubleArray>, by: Double): List<DoubleArray>`; `Quad.MITER = 4.0` | the outline grown by `by` on every side, canvas px, either winding: each side moved out square to itself. A rectangle's is exactly its box grown — so the frame and the hole of a mark with no `quad` are what they were. A corner so sharp that the moved sides would meet more than `MITER × by` away is cut back | "the outline grown" |
| `MarkSheet.shapeStep(tx: Tx, p: SurveyPrep.Prep, id: String, act: String, stage: Int?, freeId: String?, part: String, snapshot: () -> Unit): MarkSheet.Shaped`; `class Shaped(val freeId: String?, val part: String, val said: String?, val hold: Boolean, val changed: Boolean)` | the two shape acts as ONE step. `Four corners`: nothing in the record, `freeId = id`, `part = NudgePad.BODY`, says `sp.shape.drag_corners`, `hold`, no `snapshot`. `Rectangle`: `snapshot()` then `Quad.clear` (only where there is a key), `freeId = null`, `part = BODY` (a corner as the part would free it again), says nothing, `hold`, `changed`. Phase read-only: the phase's words; locked: the lock's; a point, a Note, a Line, nobody, another act: `freeId` and `part` as they were | "Four corners changes nothing in the record …" (the file byte for byte, the undo steps counted, every refusal); "the panel draws the shape row only where it is asked for …" (Done's step and Rectangle's are one) |
| `NudgePad.start(p, id, stage, part, bb, w, h, dp, stepTaken: Boolean = false, free: Boolean = false): State?` | `free`: the mark shows four corners and no tab, so a TAB as the part last touched (pressed before `Four corners`) is the whole mark; a corner is its own part, free or not | "Fine move on a corner" |
| `class NudgePad.Pressed(val state: State?, val moved: Boolean, val said: Msg? = null)`; `NudgePad.cornerStops(code: String): Msg` | a corner the shape refuses: `moved` false, the state as it was (no undo step taken), and `said` the one line. A corner hard against the face's edge, and an edge at its floor, say nothing | the same test |
| `MeasureFlow.guardMsg(prep: SurveyPrep.Prep, code: String, ab: (String) -> DoubleArray): Msg?`; `guardText(tx, prep, code, ab): String?` (signature unchanged) | the note line's sentence for a reading, with no figure: the gross guard's, or the closing check's naming the sloping side — and which, when a code could say both (§11.3) | `MeasureFlowTest`: "the closing check's sentence names the side and quotes no figure …" (the design's 870 case in walk order: said as the 870 lands on the LEFT side, one tick, the gold walking on; a double buzz and the gold staying when the sloping side is read last) and "which sentence, when a side could say two …" (the floor's "back side" too) |
| `MeasureText.sidesSay(tx: Tx, side: String): String` | `sp.measure.sides_say_x` | the same |
| `Landing.after` / `keyed` (signatures unchanged) | their `note` is `MeasureFlow.guardMsg`'s, so a reading that completes four sides that do not close says the closing sentence, not the guard's | the same |

**Held, not built** (they were right, and a four-corner mark's case was not pinned): its figures on the layout the
screen uses (`QuadScreenTest`, "a four-corner mark's figures …": nothing unpicked and unread; `?` picked; `▶?` gold;
read sides the number alone, the sloping one turned to its slope; each side's plate nearest its own side and outside
the shape; no `≈`, no number nobody measured, in any state; a wished unit no figure at all) and "the diagonal lies
inside the shape, and the held mark's four corners are kept clear by every plate"; the walk ("the gold walks inside a
four-corner mark …", through `Pick.body`, `Landing.after` and `Pick.next`); the gross guard on a side and on the
diagonal, never on a position, and not on a unit that stands off the wall — where the closing check still runs; the
360's one `free`; and, in `DeleteGuardTest`, **the panel's figures with the shape row drawn**: Delete still the last
act, where it stood, at least 16 dp from every other act and from Done, in portrait and on all three landscape
windows. `MarkSheet.acts`' default stays `shape = false` in this commit; Q2b turns it on at its two calls.

### 11.5 The calls, as Q2b writes them

Type-checked here as written (§11.7). `free` below is the one value: `Quad.freeOf(freeId, pick.picked, part)`.

```kotlin
// after every Pick event -- took, landed, settled, undo -- once `part` is set:
freeId = Quad.freed(freeId, pick.picked, part)

// the press, for every mark of the face (corners BEFORE spot):
SurveyPrepTouch.Drawn(e.id, p[0].toDouble(), q[0].toDouble(), p[1].toDouble(), q[1].toDouble(),
    corners = Quad.freeCorners(e, free(e))?.map { c -> /* face mm to canvas px */ },
    spot = if (SurveyPrep.isSpot(e.t)) … else null)

// the painter:
val hs = Quad.handles(e, sel && o.mode == "main" && !locked && o.tabs, o.free(e))    // NONE / TABS / CORNERS
SurveyPrepTouch.handlesOf(bb, spot, Quad.freeCorners(sm, o.free(sm))?.map { c -> /* px */ })
val outline = if (Quad.of(e) != null) Quad.corners(e).map { c -> /* px */ } else null   // null: drawRect, as today
Quad.grown(cornersPx, MarkDims.ACTIVE_GAP_DP * dp)      // the gold frame;  Quad.grown(cornersPx, 6 * u): the hole
PhotoFigures.layout(tx, o.prep, s, o.a, o.b, pk, toPx, du, dp, textW, o.live, o.ready, free = o.free, ask = o.ask)

// a corner's grab (Pick.Drag.MOVE, a corner part):
Quad.cornerOf(part)?.let { i -> Quad.drag(e, start, i, dxMm, dyMm, ab[0], ab[1], tol)?.let { r -> onEndGuideS(Quad.guide(tab, r)) } }

// the panel:  MarkSheet.panel(tx, prep, pn, quoteStage, asPicked, shape = true)
//             MarkSheet.actStep(tx, prep, pn, act, quoteStage, asPicked, shape = true)
SurveyPrepEdit.SHAPE_FREE, SurveyPrepEdit.SHAPE_BOX -> {
    val r = MarkSheet.shapeStep(tx, prep, pn.id, step.act, quoteStage, freeId, part) { snapshot() }
    freeId = r.freeId; part = r.part
    r.said?.let { t -> say(t) }
    if (r.hold) took(Pick.shaped(scene(), pick, pn.id))
    if (r.changed) changed()
}

// Fine move:
pad = NudgePad.start(prep, mid, quoteStage, last, box, shown.w, shown.h, pxPerDp, stepTaken,
    free = Quad.freeIn(prep, mid, freeId, pick.picked, last))
val r = NudgePad.press(prep, st, key, a[0], a[1]) { snapshot() };  r.said?.let { m -> say(m(tx)) }

// the 360:  Pano360.tabsOf(e, free(e))   -- at both of its sites
```

`shapeStep` answers the phase and the lock itself, so `panelAct` needs no `phaseRefuse(` of its own for the shape's
act: §10 note 6's row "a third beside the padlock's and Duplicate's … 2 → 3" need not move `PhaseToolsTest`'s count.
`took(Pick.shaped(…))` after `freeId` and `part` are set keeps them: `NudgePad.touched` leaves the part on an event
that is no press, and `Quad.freed(id, id, BODY)` is `id`.

### 11.6 The source pins Q2b moves

§10's note 6 holds as it stands (its table, and the two paragraphs I2b brought up to date): the panel's two calls,
the chip's `handlesOf(`, the hole's opening line, `PhotoFigures.layout(`, and the whole of `QuadMeetsF1Test`'s last
two tests. Three more lines of `:app` are pinned whole by a `:pano` test and are rewritten by §11.5's calls:

| The line of `:app` today | Q2b | Pinned by |
|---|---|---|
| `pad = NudgePad.start(prep, mid, quoteStage, last, box, shown.w, shown.h, pxPerDp, stepTaken)` (`SPS:1261`) | gains `free = …` | `MeasureScreenTest` (its count of this literal) |
| `Pano360.tabsOf(e)`, twice (`SP360:299`, `:815`) | `Pano360.tabsOf(e, free(e))` | `QuadMeetsF1Test` (already in note 6) |
| `SurveyPrepTouch.Drawn(` … `spot = if (SurveyPrep.isSpot(e.t)) …` (`SPS:2816-2817`) | `corners = …` goes on a line BEFORE `spot =` | `NotePinTest` pins `spot = …` (stays); `QuadMeetsF1Test` pins `corners =` at 0 (note 6) |

Checked and NOT moving, if the new code is ADDED beside them: `val r = NudgePad.press(prep, st, key, a[0], a[1]) {
snapshot() }` (`MeasureScreenTest`, `PhaseToolsTest`: the `said` line goes after it); `part = NudgePad.touched(part,
before, o, body)` and `pad = NudgePad.afterEvent(…)` (`MeasureScreenTest`: the `freeId` line goes after them);
`for (k in SurveyPrepTouch.TABS) {` and `val solid = NudgePad.solid(o.nudge, e.id, k)` (`MeasureScreenTest`: the four
discs are a branch beside the eight tabs, never in their place); and the tail of the screen's `Marks(…)`, which since
the review of I2 ends `nudge = if (which == "main") fine else null), mm, u)` (`MeasureScreenTest`: `free = …` goes on
a line before it — §10's "before `chip =`" is still a place that holds).

### 11.7 Type-checked here, and what that cannot prove

- **`:app` as it stands, against this commit's `:pano` classes** (kotlinc 2.0.21 from the Gradle cache, the stand-ins
  of `DESIGN-imagemeter.md` §23.4 copied to their own folder): `SurveyPrepScreen.kt`, `MarkSheet.kt`,
  `SurveyPrepDraw.kt` and `SurveyMeasure.kt` have NO error, and the 192 left are the same stub gaps in the same four
  files, count for count (`SurveyPrepTools.kt` 134, `SurveyPrepLocks.kt` 33, `SurveyPrep360.kt` 13,
  `SurveyPlanScreen.kt` 12). So every signature `:app` calls today is source-compatible: the two new parameters
  (`NudgePad.start`'s `free`, `Pressed`'s `said`) are last and defaulted, and no sealed class gained a case — **no
  `:app` line had to change**, not even one a `when` forces.
- **§11.5's calls**, written out as one Kotlin file that calls each function the way `:app` will, compile against the
  real `:pano` classes with no error. Six errors planted in a copy (an unknown function; two arguments swapped; a
  `Msg` said as a `String`; a `String` for a `Boolean`; a missing argument; an extra one) are each reported.
- **Not proven:** anything about the Compose compiler plugin, lint, or a phone; and the call sheet proves the calls,
  not the code Q2b will write round them — every name it uses must still be in scope where it is written.

### 11.8 Texts

Two keys, ENGLISH FIRST, each with its reader in `:pano` and a row in `mr-pending.tsv` (29 → 31): 1,934 keys.

| Key | English | Read by |
|---|---|---|
| `sp.measure.sides_say_x` | The four sides do not agree — see the ! on the {side}. Check the readings, or the corners. | `MeasureText.sidesSay`. The key Q1 added under this name quoted `≈{est} mm` and went in slice I1b with every estimate; this is its sentence re-worded, with `{side}` and no `{est}` |
| `sp.shape.corner_stops` | {code}: the corner stops here. No side goes under {mm} mm, and the shape cannot fold. | `NudgePad.cornerStops` — the fifteenth key §6 proposed; `{mm}` is `SurveyPrep.MIN_SIZE` |

`sp.shape.drag_corners` has its reader now (`MarkSheet.shapeStep`), so the accessors nothing calls by name go 657 →
656. `Tx` regenerated (1,863 accessors); the ratchet lowered nothing; `unkeyed.tsv` stays empty; no Marathi was
composed and no existing Marathi value changed.

### 11.9 The build note for Amit (when Q2b ships)

Pick a window, tap its `FW1 ▾` chip, `Four corners`, drag a corner (a side near upright or level snaps straight), and
read its four sides: each figure stands on its own side, `?` until it is read. `Fine move` moves a corner by 1 mm.
`Rectangle` in the same place puts it back. Use a project at Survey, or one only on the phone. **One line more: an
exported `-survey.jpg` picture of a four-corner mark shows its outline and no side figures yet — for a picture to
send before the next build, keep the four Lines.**

## 12. Q2 as built (23 Nov 2026)

Slice Q2b switches the four-corner mark ON, in `:app`, from §11's calls: one commit over Q2a (`198c9a2`) on branch
`port63-q2`, local only. **`:app` is compiled only by CI, and nothing of this has been seen on a phone.** `:pano`'s
main code changes in two comments only (`MarkSheet.acts`' and `SurveyPrepEdit`'s, which said the shape row waits for
Q2) — every rule the screen follows was built and tested in Q2a — and its tests move only where they pin a line of
`:app` this commit rewrote (§12.5). `:pano` after Q2b: **112 test classes, 1,591 tests, 0 failures, 0
errors, 0 skipped** (the totals of `pano/build/test-results/test`; the count is Q2a's, since two tests of
`QuadMeetsF1Test` were rewritten in place and none was added or deleted). No text was added or re-worded: 1,934 keys,
`Tx` regenerated with no difference, the ratchet lowered nothing, `unkeyed.tsv` stays empty, `mr-pending.tsv` stays at
31, no Marathi touched.

Line numbers are this commit's. `SPS` = `app/SurveyPrepScreen.kt`, `SPD` = `app/SurveyPrepDraw.kt`, `SP360` =
`app/SurveyPrep360.kt`.

### 12.1 What Amit sees, step by step (Survey; on the photo, and where said on the 360)

1. **Pick a window** (tap it): eight square tabs, its `FW1 ▾` chip above it, as today.
2. **Open its panel** — tap the chip, or long-press the window — and the second line of acts begins with a new cell,
   **`Four corners`**. Delete is still the last cell, apart, where it stood.
3. **Tap `Four corners`.** The panel closes. Nothing in the record changes and there is no ↶ step. The window is held,
   and where its eight tabs were it shows **four ROUND handles, white discs ringed in the window's colour**, one on
   each corner. The note says: "Drag a corner. A side near upright or level snaps straight."
4. **Drag a corner.** It moves by itself, under the magnifier, which looks at the corner where it has snapped to.
   Two dashed guide rays run through the corner across the wall; the ray along a side that has snapped straight is
   drawn bold (both on a square corner). A side within 5° of upright or level snaps straight when the corner is within
   a Line end's snapping distance of it (25 mm, or 14 dp at the zoom he is at — zoom in to draw a small run-out). **A
   corner that would break the shape simply stops**: no side under 20 mm, no fold, nothing said. A tap on a handle
   moves nothing. A second finger puts the corner back. One ↶ step per drag.
5. **Everything follows the SHAPE**: the outline (in the type's colour, or its state's line; a Dropped mark's red
   stroke from its bottom-left corner to its top-right), the hole in the dimmed photo, the gold frame round the mark
   being measured, the magnifier's thin copy, and the press — a tap in the empty corner of its bounding box no longer
   picks it, and picks what is under it instead. The code chip, the padlock and the red out-of-face frame keep the
   bounding box.
6. **Each side shows its own figure, on its own side**, once it is read — the number alone, along the side, the sloping
   one turned to its slope. While the mark is picked an unread side shows a quiet `?`; the one being measured is the
   gold `▶ ?`. Not picked, an unread side shows nothing. No `≈` anywhere (decision 2). The diagonal, where it is asked
   (no square corner), lies inside the shape.
7. **The gold walk and `▸`** go bottom, right, top, left, the diagonal if asked, then the type's own figures (from the
   left corner, the sill, the reveal …). A reading from the D2 or the keypad lands on the gold side.
8. **The closing check**: when three straight sides and the fourth do not agree, an amber `!` stands on the sloping
   side's plate and the note says "The four sides do not agree — see the ! on the bottom side. Check the readings, or
   the corners." — no figure in it (decision 3). A reading more than twice or less than half its side as drawn gets
   the gross guard's sentence, as any figure does.
9. **Fine move** (the panel's act): the pad nudges the corner last touched by 1 mm (or 5 mm), that corner drawn
   solid; a tap on another corner re-aims it; with no corner touched, all four are solid and the arrows move the whole
   mark. An arrow the shape refuses moves nothing, takes no ↶ step and says "FW1: the corner stops here. No side goes
   under 20 mm, and the shape cannot fold." (decision 4).
10. **`Rectangle`** — the same cell of the panel, on a mark that has a shape — puts it back to its bounding rectangle
    in ONE ↶ step; the eight tabs return. Its side readings are kept (§2.6).
11. **Duplicate** copies the shape; **↶** restores it; a **locked** mark shows no handles and a drag on it says the
    lock's words; a mark of an **earlier phase** is read-only and says the phase's (§3.3). A shape dragged or snapped
    back to its box keeps its four round handles until another item is picked; then it is a rectangle again.
12. **On the 360** the same window shows the same four round handles (same `freeId`, same rule), each drags its own
    corner on the wall, and its outline there is its real shape. No figures are drawn on the 360, and a corner there
    snaps at the 25 mm floor with no guide (§3.6: unchanged).
13. **A saved prep whose `quad` cannot be read** opens with that mark as its box and says "Front wall: mark FW1's four
    corners were unreadable, kept as its box" — `SurveyPrepFile`'s problem line, said by the screen as every file
    problem is (Q1; nothing new in Q2b).

### 12.2 What changed in each `:app` file

| File | What |
|---|---|
| `SurveyPrepScreen.kt` | `var freeId` beside `part` (`:350`) and the ONE `freeNow()` = `Quad.freeOf(freeId, pick.picked, part)` (`:353`). `freeId = Quad.freed(freeId, pick.picked, part)` after every event that can move the pick or the part: `took` (`:686`), `landed` (`:710`), readings pulled under the pick (`:811`), `settled` (`:898`); ↶ goes through `took`. `fineMove` hands `NudgePad.start` `free = Quad.freeIn(…)` (`:1275`); `finePress` says `r.said` (`:1290`). The panel's two calls name `shape = true` (`:1308`, `:2345`), and `panelAct`'s `when` runs both shape acts as one branch through `MarkSheet.shapeStep` (`:1329`). `Sp360` is handed `freeId` (`:1809`, before `stage =`), `SpPhoto` `free = freeNow()` (`:1831`). `SpPhoto` gains the parameter `free` (`:2523`) and reads it through `freeS` in the gesture (`:2640`); `drawnMarks()` hands EVERY mark's `corners = Quad.freeCorners(e, freeS(e))…` on the line before `spot =` (`:2863`); `markGrab`'s `Pick.Drag.MOVE` builds a corner's grab like a Line end's — from the corner, `gate = true`, `focus` on the corner, `cancel = { onEndGuideS(null) }`, `Quad.drag(…, tol)` with `SurveyGuide.endTol`, the guide through `onEndGuideS(Quad.guide(…))` (`:2957-2976`); the gesture clears that guide however it ends (`cornerGrabbed`, `:3164`, `:3227`, `:3440`); the painter's `Marks(…)` is handed `free = free,` before `chip =` (`:3507`) |
| `SurveyPrepDraw.kt` | `Marks` gains `free: (SurveyPrep.Mark) -> Boolean = { false }` (named, defaulted, last). `shapeOf` (`:446`): a mark's four corners on the canvas when `Quad.of(e) != null`, else null — so a mark with no shape is drawn by `drawRect` exactly as before. The outline and `lifeOutline` draw a closed path through them, a Dropped stroke `q0`→`q2` (`:417-437`, `:713-725`); `measuring` frames the shape with `Quad.grown(…, ACTIVE_GAP_DP)`; the dim hole is `Quad.grown(…, 6 u)`, a branch after the spot's; the handles are `Quad.handles`' — four discs of `SurveyGuide.END_R_DP` at `Quad.corners(e)`, the one Fine move nudges solid, as a branch BESIDE the eight tabs (`:738-765`); the chip's `hand` asks `Quad.handles` and its handles are `handlesOf(bb, spot, freeAt)` (`:614-616`); `figures` hands `PhotoFigures.layout` `free = o.free`. **`dims` is not touched** (decision 5) |
| `SurveyPrep360.kt` | `Sp360` gains `freeId: String?` before `stage` (`:219`). ONE local function `free(e)` = `Quad.freeOf(freeIdS, selS?.id, selS?.part)(e)` (`:249`), asked by the press (`sceneOf`: `Pano360.tabsOf(e, free(e))`, `:315`) and handed to the painter as `P3Draw.State.free` (`:706`, `:798`), where the picked mark's handles are `Pano360.tabsOf(e, free(e))` — discs when they are corners (`:839-851`). The drag is unchanged |
| `MarkSheet.kt` | one comment (the acts' ids now include the shape row's). No code |
| `FaceWriter.kt`, `AsIsUpload.kt`, `SurveyMeasure.kt` | not touched. `SurveyGuide.snapCoord` (`:pano`) not touched |

No `:app` file writes `.extra =`. The shape is written in three places, all Quad functions: a corner dragged
(`Quad.drag`, called by the photo's grab, and through `SurveyPrep.moveMark` / `snapMark` on the 360 and by Fine move's
pad) and `Quad.clear` inside `MarkSheet.shapeStep`. `QuadMeetsF1Test` holds the list of Quad functions `:app` names.

### 12.3 Decided where the design was silent — by Claude, in Q2b

1. **`freeId` is asked again in FOUR places, not §11.2's three and an "undo".** The fourth is the readings pulled from
   the web under the pick (`Pick.settle` there can clear the pick); ↶ needs no line of its own because it goes through
   `took`. One rule, `Quad.freed`, at every one.
2. **A corner's grab starts from the CORNER, not from where the finger went down** — as a Line's end does — so the
   finger's miss of the handle (up to 16 dp) never jumps the corner at the first move.
3. **`cornerGrabbed` is its own flag, not the end's `endGrabbed`.** `endGrabbed` also makes a tap "the end's own"
   (it skips `Pick.tap`); a tap on a corner stays `Pick.tap`'s handle event (row 8), which is what re-aims Fine move.
4. **A corner drag refused from its first movement still leaves one ↶ step that changes nothing** — exactly what a
   tab dragged against the mark's minimum size leaves today (the gesture takes its step before it asks the model).
   Decision 4's "no undo step" is the NUDGE's, where the model is asked first. Closing it for drags means the grab
   reporting whether it moved anything — the same change for tabs, ends and bodies — and is left for its own slice.
5. **A mark whose corners are free but which has no shape yet is outlined, holed and framed as its box**
   (`shapeOf` asks `Quad.of`, not `free`): its four corners ARE the box's, so there is one drawing for it, the
   rectangle's, byte for byte.
6. **The round handles** are 5.5 dp (`SurveyGuide.END_R_DP`, a Line end's) with the tabs' 2 u ring, on the photo and
   on the 360. On the 360 the corner Fine move nudges is NOT drawn solid, as a tab there is not.
7. **The code chip lifts above a four-corner mark's top side as it does above a rectangle's hand-anchored width
   plate** (`hand` is `Quad.handles(…) != NONE`, §11.2 row 5): the top side's plate stands outside the shape there.
8. **The saved picture and the A4 sheet draw the true outline** through the same path (§3.4, §3.8) — the outline is
   `drawMarks`', which they share; their figures are `dims`', which is not touched.
9. **`QuadMeetsF1Test`'s last test was moved, not deleted.** It held §10's note 6 to naming every test that pinned a
   line Q2 was going to rewrite; it now holds §12.5 to naming every test that pins a line Q2 WROTE, for the slice that
   rewrites one next.

### 12.4 The build note for Amit

Three things to try first, on a project at Survey (or one only on the phone):

1. **Make one.** Tap a window, tap its `FW1 ▾` chip, `Four corners`, and drag one bottom corner up. Does the corner
   follow the finger, does the upright side stay straight, and are the four round handles easy to tell from tabs?
2. **Measure it.** With the window picked, laser its four sides. Does the gold `▶ ?` walk bottom, right, top, left,
   and does each number land on its own side, turned along the sloping one?
3. **Fine move and back.** Panel → `Fine move`, nudge a corner 1 mm at a time, tap another corner and nudge that; then
   panel → `Rectangle`, and ↶ once — the shape should return.

**One line more: an exported `-survey.jpg` picture of a four-corner mark shows its true outline, its code and its
position figures, and no side figures yet — for a picture to send before the next build, keep the four Lines.**

### 12.5 The lines of `:app` Q2 wrote that a `:pano` test pins

For the slice that rewrites one next (M3, the saved pictures; I3, the tool row). `QuadMeetsF1Test`'s last test lists
these lines as `:app` has them and fails when a test that pins one is not named here.

| The line of `:app` | Pinned by |
|---|---|
| `MarkSheet.panel(tx, prep, pn, quoteStage, asPicked, shape = true)` (`SPS`) | `DeleteGuardTest`; `FeaturesTest`; `MarkSheetTest`; `PhaseToolsTest`; `QuadMeetsF1Test` |
| `MarkSheet.actStep(tx, prep, pn, act, quoteStage, asPicked, shape = true)` (`SPS`) | `DeleteGuardTest`; `FeaturesTest`; `PhaseToolsTest`; `QuadMeetsF1Test` |
| `freeId = Quad.freed(freeId, pick.picked, part)` (`SPS`, four times) | `QuadMeetsF1Test`; `QuadScreenTest` (the screen's `took`, run as a model) |
| `free = Quad.freeIn(prep, mid, freeId, pick.picked, last))` (`SPS`, the tail of `NudgePad.start(…)`) | `MeasureScreenTest`; `QuadMeetsF1Test` |
| `corners = Quad.freeCorners(e, freeS(e))` (`SPS`) | `QuadMeetsF1Test`; `NotePinTest` pins the `spot = …` line after it |
| `MarkSheet.shapeStep(tx, prep, pn.id, step.act, quoteStage, freeId, part) { snapshot() }` (`SPS`) | `QuadMeetsF1Test` |
| `SurveyPrepTouch.handlesOf(bb, spot, freeAt)` (`SPD`) | `MeasureScreenTest`; `NotePinTest`; `QuadMeetsF1Test` |
| `Quad.handles(e, sel && o.mode == "main" && !locked && o.tabs, o.free(e))` (`SPD`) | `QuadMeetsF1Test` |
| `PhotoFigures.layout(…, o.live, o.ready, free = o.free, ask = o.ask)` (`SPD`) | `MeasureScreenTest`; `QuadMeetsF1Test` |
| `Pano360.tabsOf(e, free(e))` (`SP360`, twice) | `QuadMeetsF1Test` |

**The pins this commit moved, each with its reason written beside it in the test:** `DeleteGuardTest` (the two panel
calls), `FeaturesTest` (both), `MarkSheetTest` (the drawing call), `PhaseToolsTest` (both), `MeasureScreenTest` (the
layout call; the chip's `handlesOf(`; the `NudgePad.start(` line, which gained `free =`; the count of `NudgePad.solid(`
in the painter, 3 → 4), `NotePinTest` (the chip's `handlesOf(`), and `QuadMeetsF1Test`'s last two tests, rewritten.
**Not moved, as §11.6 said:** `PhaseToolsTest`'s count of `phaseRefuse(` (the shape's act asks none: `shapeStep`
answers the phase and the lock), the tail of the screen's `Marks(…)`, the tail of the `Sp360(…)` call, `for (k in
SurveyPrepTouch.TABS) {`, `val solid = NudgePad.solid(o.nudge, e.id, k)`, `spot = …`, the hole's opening line, the two
guards before the 360's `tabsOf`, and every pin on `dims`.

### 12.6 If CI's `:app` compile is red — the lines to look at first

Every name each added line uses was checked to be in scope where it is written, and every call against its
declaration; the list is where a stand-in could still have hidden something, most likely first.

1. `SPD:450` `private fun Path.poly(…)` and its two callers (`:455`, and inside `Path().apply { … }` at the hole,
   `:650`): an extension on `android.graphics.Path` called with an implicit receiver. The stand-in `Path` has
   `moveTo` / `lineTo` / `close` as the real one does; `addPath` was avoided because the stand-in had none.
2. `SPS:2957-2976`, the corner's grab: `SpGrab(px, py, SpSel(…), null, focus = …, cancel = …, gate = true) { jx, jy
   -> … }` — four positional arguments, three named, the trailing lambda; an `if … { … } else SpGrab(…)` as the
   branch's value. `k()`, `density`, `onEndGuideS`, `ab`, `sq`, `tab` are `SpPhoto`'s own, declared above `markGrab`.
3. `SPS:2863`, `corners = …` inside `SurveyPrepTouch.Drawn(…)`: named `corners` before named `spot` (both exist; the
   five before them are positional).
4. `SPS:1329-1335`, the shape branch: `step.act` is a `String` (`ActStep.Run.act`), `r.said` a `String?`.
5. `SPS:1290`: `r.said` is a `Msg?` (`(Tx) -> String`), said as `m(tx)`.
6. `SP360:706`: `free = { e -> free(e) }` — a named argument and a local function of the same name; `:839` `val free
   = st.free` then `free(e)` — a `val` of function type shadowing nothing in `P3Draw`.
7. `SPD:745`: `for ((i, q0) in Quad.corners(e).withIndex())`; `r`, `at`, `solid` are new names in that block.
8. A new import in each of the three files: `com.malletcrafts.interiorestimate.pano.Quad`.

### 12.7 Type-checked here, and what that cannot prove

kotlinc 2.0.21 from the Gradle cache, the real `:pano` classes of this commit, and the stand-ins of §11.7 (copied to
their own folder; one function fewer was needed than first written — see 12.6, 1): `SurveyPrepScreen.kt`,
`SurveyPrepDraw.kt`, `SurveyPrep360.kt`, `MarkSheet.kt`, `SurveyMeasure.kt`, `SurveyPrepTools.kt`, `SurveyPrepLocks.kt`
and `SurveyPlanScreen.kt` together. The three files this commit wrote have **no error on any line it wrote**; the 192
errors left are the same stub gaps as before Q2b, message for message (`SurveyPrepTools.kt` 134, `SurveyPrepLocks.kt`
33, `SurveyPrep360.kt` 13 — bitmaps, on lines this commit did not write — `SurveyPlanScreen.kt` 12). **Ten errors
planted on this commit's own lines** in a copy — a name not in scope; two arguments swapped, three times; a `String?`
where a `String`; a `Msg` said as a `String`; a function handed where its answer is wanted; a required argument left
out; a field that does not exist; a parameter that does not exist — are each reported. **Not proven:** anything the
Compose compiler plugin or lint adds; that the hand-written stand-ins match the real `android.graphics` and Compose
signatures everywhere (`Canvas.drawPath`, `Canvas.drawCircle`, `Path` are used as the file already used them); and
everything about a phone — how a corner feels under the finger, whether 5.5 dp discs read on a bright photo, the
guide's rays, the 360.

### 12.8 What Q2 does not do

- The saved `-survey.jpg` pictures' side figures: slice M3, with the pictures' "measured figures only" (§3.9).
- S4 MDT's "Room from survey": slice Q4, another session's; it still cuts the bounding box (§4.2).
- A corner's guide and a Line end's snapping distance on the 360; a solid corner there under Fine move (12.3, 6).
- An empty ↶ step after a corner drag that was refused from the start (12.3, 4).
- Curved tops, more than four corners, a square-but-out-of-true opening as anything but a rectangle (decision 6).
- A button outside the panel (decision 1). The Marathi of the fifteen four-corner texts (English first).
