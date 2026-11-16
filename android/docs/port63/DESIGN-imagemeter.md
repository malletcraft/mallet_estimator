# DESIGN-imagemeter — one screen to mark and measure, worked the way ImageMeter is

S2 APK, port 63. Written 16 Nov 2026 (IST), after Amit's three messages on build 0.3.337 / 0.3.339. **Design only:
nothing here is built.** It replaces the planned slice "F2 · one step for marking and measuring" of
`DESIGN-port63.md` §11 and changes where the four-corner screen (`DESIGN-quad.md` §3, slice Q2) hangs its controls.

- **Read against** `batch-next` `380c72d` (H1 `8455c77`, the phone's 0.3.339, plus slice F1 and its review). Line
  numbers are that commit's. `SPS` = `android/app/src/main/kotlin/com/malletcrafts/interiorestimate/SurveyPrepScreen.kt`,
  `APP/` = that folder, `PANO/` = `android/pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/`, `TEST/` =
  the same under `src/test`.
- **This file is committed on `port63-im` at `8455c77`.** The build starts from `batch-next` once F1 is on it
  (`380c72d` or what follows it), not from this branch's parent.
- **ImageMeter** was read from the vendor's own manual, FAQ, download page and the pictures they embed, and from the
  text and layout files of the vendor's Android package v26.6.0. YouTube, Google Play and the App Store could not be
  opened. A second pass checked every claim; §1 carries its verdicts. We copy PATTERNS only: never ImageMeter's
  artwork, icons, name or branding.
- **No record changes.** Nothing in this design writes a new key into the room record, changes a reading key or
  touches S5. A room saved by 0.3.339 opens unchanged.

Marks used below: **IM** = ImageMeter does this and a vendor source confirms it. **Ours** = our own choice, said as
such. **NC** = not confirmed for ImageMeter; never copied as its way.

---

## 0. Amit's words, and what they mean

All verbatim.

**15 Nov 2026, after the first use on a client site:** "Having both measurements foto and measured and both is very
confusing while using app. The laundry list of measurements shown below screen is also frustrating. Image meter app
which is reference have very elegant annotation ui and end user experience. But do not drop our industry standard
mark items like door window electrical plate etc. imagemeter shows all dimensions on the annotation there itself
which is easy to use."

**16 Nov 2026, evening, on build 0.3.337** (a step "3 · Measure" beside "2 · Mark", one figure per dimension on the
photo, and under the photo a measure bar and a keypad):

- (a) "do we really need two screen now? which is mark and measure? as we should be measure where marking is done.
  also highlight of mark which we aer measuring is not correct . i cant see which mark is being measured as its no
  highlighted on foto"
- (b) "chain mark is rarely used. get rid of it. also navigation button and D2 aer very close where d2 get
  accidentally fired. same for delete button for any item. tape mark does not move unless its end points are moved.
  difficult for moving it from one point to other."
- (c) asked whether the new gold highlight could be seen: "i do not  want explicit box below foto which show the
  measurements. it should follow imagemeter like operation . very less clutter when it comes to measurements. a slide
  up screen in imagemeter  give more options. imagemeter is gold standard when it comes to image annotation. only
  thing it lacks is the business like client / site room etc. it has got lot of good features of annotations. study
  it and use it."

**His two answers on a selector between (b) and (c):** while marking, the photo shows "Every mark's size, always";
and after the laser fills a mark's last figure the screen "Move[s] to the next unmeasured" figure on that wall by
itself.

**Earlier the same day:** a window that is not a rectangle becomes a four-corner mark (`DESIGN-quad.md`; its pure
model is built on `port63-quad`, its screen is not); the saved gallery pictures show "Measured figures only"; a room
is saved "from one surface".

**What they mean.**

1. ONE step where a mark is placed and measured. The steps become **1 · Lines, 2 · Mark, 3 · 360**; the Plan stays a
   tab.
2. NOTHING under the photo shows a measurement: no measure bar, no figure box, no list, no words about a dimension.
   A figure lives on its annotation and only there.
3. The way of working is ImageMeter's: pick an element, its value is on it, the laser's reading goes to the picked
   one, a keypad only when he wants to type, a panel that slides up for the rest.
4. Everything that is ours stays: every mark type and kind, the trades, Notes, wishes and their states, client /
   site / project / room.
5. Far less on screen at once.

**Where the sources do not carry the reading above, said plainly:**

- "a slide up screen in imagemeter" could be any of three things (§1.3). Vendor pictures show two sheets that rise
  from the bottom: the "+" tools sheet and "Tags and properties". This design builds the second for a picked item and
  the first for adding, and asks him which he meant (§15, Q1).
- "Move to the next unmeasured figure on that wall by itself" is NOT what ImageMeter does. It steps through the
  labels of ONE element after a reading and never to another element. Crossing to the next mark is ours, by his
  answer.
- ImageMeter shows `?` on an empty value. We show the photo's estimate, marked `≈` (decided in `DESIGN-port63.md`
  §1.1). ImageMeter's nearest thing is a value computed from a reference, which it shows as one plain figure.
- His message (c) did not answer whether the gold highlight (H1) can be seen. H1 stays as built; the question is
  still open and a picture will not settle it, only the phone.

---

## 1. What ImageMeter does

### 1.1 Patterns we copy (each confirmed)

| # | ImageMeter | Source | What we do with it |
|---|---|---|---|
| 1 | At rest the editor is the photo and ONE toolbar row. No box, bar or list of values under or beside the photo | manual `/manual/editor/editor-screen-overview/`; 12 editor pictures opened | The Mark step is the photo and one tool row (§2) |
| 2 | Every value is written on its annotation and nowhere else | pictures `bluetooth/connect/img/im-use-1.jpg`, `im-use-2.jpg` | The same. Ours: an unread figure is the photo's `≈` estimate, `?` only where the photo cannot show it |
| 3 | A laser reading goes to the ACTIVE measure; no dialog opens; the value entry must stay shut to receive it | `/manual/bluetooth/connect/`, `/manual/faq/` | A reading lands on the gold figure (§4) |
| 4 | After a reading the selection steps to the element's NEXT LABEL by itself | `/manual/bluetooth/connect/` | The same inside a mark; then on to the wall's next unmeasured figure (ours, Amit's answer) |
| 5 | A reading with nothing active is refused with a reason ("no active element") | app text, v26.6.0; how it is shown is NC | Refused and said, as today (§4.4) |
| 6 | A selected element is shown by its HANDLES, with no colour change: discs on a line's ends; corner circles, a centre move glyph and a rotate glyph on a rectangle | pictures `im-use-1.jpg`, `templates/tutorial-partslist/img/260501-edit.element.properties.png` | Handles mean "this can be moved". The gold of H1 is ours, on top, for "this takes the next shot" |
| 7 | Toolbar buttons that need a selection are greyed while nothing is selected | pictures `tutorial-areasum/…-113241` against `…-113032` | `⋯` in the tool row is grey with nothing picked |
| 8 | "+" opens a sheet over about three-quarters of the screen: headed groups, each tool an icon with its name | picture `editor-screen-overview/img/add-tools-menu.png` | The Add sheet (§7) |
| 9 | The last-used tool has its own place beside "+", to add several of a kind without the sheet | manual text | The fourth slot of the tool row |
| 10 | "Tags and properties" is a sheet over the lower 31-43 % of the screen; the photo above is dimmed and the element stays in view; one OK | pictures `…-113250`, `260501-edit.element.properties.png`; `/manual/templates/properties/` | The panel (§6): our Mark sheet, opened half way, the mark kept in view |
| 11 | Lock: a locked element cannot be moved, its other attributes still can | manual text | Our padlock already means this. A locked mark still takes readings (ours) |
| 12 | Delete acts on the selection only; no confirm is evidenced; Undo is one button | manual text; app strings searched | Delete is inside the panel, with UNDO for 5 s and ↶ (§6) |
| 13 | A rectangle can become a free four-sided shape, each corner moved by itself | release note v3.9.13-2 (in an HTML comment of `/download/`); app strings | The four-corner mark (§8.3) |
| 14 | A rectangle's width sits ON its top edge and its height ON a side edge, turned along it, with no separate dimension line | pictures `…-113241`, `East_building_side.jpg` | The size plates of a mark that is not picked (§2.2). Ours: height on the right edge |
| 15 | The meter's own button fires. The device table gives the Leica D2's remote trigger as "?" | `/manual/faq/`, `/manual/bluetooth/devices/` | Nothing on our screen fires the laser (§4.1) |
| 16 | The app can connect to the meter while in the editor ("Auto-connect in editor") | app setting text; the manual shows connecting on the main screen only | The link starts by itself at Survey (§4.2) |
| 17 | Snapping to corners, ends, outlines; a guide line shows it; no per-drag override, the escape is to lower it | `/manual/editor/snapping/` | Our snap, guide rays and zoom-to-escape stay as built |

### 1.2 ImageMeter patterns we know and do NOT copy

- **Its value entry** (app layout only, no picture): four tabs (Value, Extra, Unit, Style), unit keys, a text mode.
  Ours is millimetres only: one small keypad (§5).
- **Drawing a new element by one gesture** (app text): a rectangle by its diagonal. Ours: a tap places the type's
  own default box (a door is a door's size), and a drag places and carries it, as today. Tape and Line are already
  point-and-drag.
- **Per-element label switches and styles** (Show length, colour, line width). Ours: what a mark shows is fixed by
  its checklist; a type's colour and a state's outline mean something.
- **Labels drawn in the image's own scale**, growing with zoom. Ours: plates stay 10 / 12 sp at every zoom.
- **A second figure per label** (an alternative unit, opt-in). Ours: one figure, always.

### 1.3 What could not be confirmed, and is not built on

| Claim | Standing |
|---|---|
| Which ImageMeter panel Amit means by "a slide up screen" | NC. Two sheets are seen in vendor pictures ("+" tools; Tags and properties); three more are known from layouts only (colour, value entry, a three-row toolbar) |
| A top bar appears for every selected element | NC. Seen once, for a perspective reference; not with a selected rectangle or circle |
| Pulling the toolbar's handle up reveals more rows | NC. The three-row structure is real; the gesture is an inference |
| What opens ImageMeter's value entry (one tap on a label, a second tap, a double tap) and where it sits | NC. Ours is decided in §5 |
| Where a dimension line is grabbed to move it whole | NC (likely its body). Our Tape / Line / area body-move is F1's, from Amit's own request |
| Whether the bin asks before deleting; whether a sheet closes by swipe or a tap outside | NC |
| Where the magnifier appears | NC. Ours stays 120 dp, 3× |
| Multi-select, auto-scroll at the edge, the in-editor snapping dialog, value-source icons on the phone | Desktop notes or newer than the Android package read; NC on his phone |

Five of these take two minutes with ImageMeter on Amit's phone. None blocks slice I1.

---

## 2. The screen, state by state

Sizes are for the Mi A3: 360 × 780 dp, a 756 dp window under the status bar. "Today" is `380c72d`.

### 2.1 The frame (portrait)

| Row | dp | Change |
|---|---|---|
| Column padding | 6 + 6 | none |
| Phase band | 20 | none |
| Top bar: ← · title over saved state · **laser chip** · ↶ · ⋮ · ✓ Save | 40 | the chip is new, 44 dp wide, at Survey only (§4.2) |
| Face tabs, two rows; amber count = figures left | 64 | the count shows in the Mark step (today in 3 · Measure only) |
| Step strip: **1 · Lines  2 · Mark  3 · 360** | 28 | three steps. Its lead text becomes the face's W × H (slice I4) |
| Caption (face · W × H) | ≈22 | goes in slice I4 (folded into the strip) |
| PHOTO | the rest | |
| Tool row | 6 + 50 | the ONE row under the photo. No figure and no words about a dimension, ever |

Fixed chrome above the photo: 186 dp today and through I3, 164 dp after I4.

**The tool row** (348 dp wide), left to right, final form (slice I3). Until I3 today's palette stands there, with
`9 ▸` as its eighth slot from I1 on; `⋯` arrives with I3:

`＋` 48 · three pinned types 44 each · last-used type 44 · free space · `⋯` 48 · `9 ▸` 56

- `＋` opens the Add sheet. The four slots arm a type in one tap (§7).
- `⋯` opens the panel for the picked item; grey with nothing picked (IM).
- `9 ▸` is the open face's count of figures left; a tap moves the gold to the next one. Drawn only at Survey while
  the face has figures left (§9).
- Nothing in the row fires the laser and nothing in it deletes.

**Over the photo, costing no height:** the tool chip while a tool is armed (top-left); `fit · grid · zoom %`
(top-right; `all` goes); the magnifier while a handle is dragged; the note and the snackbar at the foot.

### 2.2 What is drawn on the photo

One figure per dimension, from `SurveyPlan.figure` as today: a reading `1198` white bold on black; no reading
`≈1203` grey on half-black, dashed; `?` amber where the photo cannot show it; `!` after a reading that fails the one
check.

- **A mark that is not picked:** its outline (type colour or state line), its code (with `✓` once all its figures
  are read), and its SIZE only. Width is one plate ON the top edge, height one plate ON the right edge, turned to
  read bottom to top; 10 sp. No dimension line and no arrowheads: the edge is the line (IM, row 14). A box too small
  for two plates takes `DimLayout`'s one compact plate (`≈200 × ≈90`) beside it, and with no room a 6 dp dot. A
  point (E, P, F, PLB, LV) has no size: its spot and code. A Note: its pin. A Tape, a Line, an area: its one figure
  mid-span, as today.
- **The picked mark:** the same at 12 sp, plus where it sits — the line from the nearer corner, from the floor or
  ceiling, a Beam's drop, the `⊥` chip — each with its white line and arrowheads as today. If it was picked BY HAND
  it also has its handles (§3): eight tabs, or four round corners on a four-corner mark, or a Line's two ends.
- **The figure being measured (H1, kept as built):** its mark framed in gold, its own line and arrowheads gold and
  three times as heavy, its plate solid gold with `▶`, the rest of the photo 35 % darker. It is placed first and is
  never reduced to a dot. A `⊥` chip that is gold carries its own name while it is gold (`⊥ reveal depth ?`, the
  checklist's existing words), because the photo cannot draw what it is.
- The plate that has just taken a reading keeps a gold ring for one second while the gold moves on, so the eye sees
  where the reading went.

Until slice I4 the unpicked marks' sizes are drawn as 0.3.339's 3 · Measure draws them (a line 6 units outside the
edge). I4 moves them onto the edge.

### 2.3 The states (portrait; final form)

Each list is top to bottom and names only what differs from the frame.

**A · Nothing picked.** Photo: every mark as "not picked". Tool row: `⋯` grey; `9 ▸` if figures are left.
Photo 536 dp (I1-I3: 514).

**B · A tool armed.** Over the photo, top-left: the tool chip, "Placing Door · tap the photo" with ✕ and the kind
button ("Wardrobe ▾"); after Survey "· as Client wish". Tool row: that slot lit. Photo 536 (514).

**C · A mark picked by hand** (a tap on it, or just placed). Photo: its handles, its size and position figures. At
Survey its first unread figure is gold (state D at once). Tool row: `⋯` live. Nothing appears under the photo.
Photo 536 (I1: ≈460, the picked item's row still stands for that one build; I2 on: 514).

**D · A dimension being measured.** Photo: H1's gold on one figure; its mark shows all its figures. If the gold
came BY ITSELF (after a reading, Next ▸ or `▸`) the mark has NO handles and cannot be dragged. Top bar: the laser
chip gold when the D2 is linked. Under the photo: nothing new. Photo 536 (514).

**E · The keypad open.** A keypad 182 dp high (6 + 4 × 44) rises over the tool row and the foot of the photo. It
has no display line: the digits appear on the gold plate. The photo is lifted, not rescaled, just far enough that
the gold plate stands 12 dp clear of the keypad. Photo 536 laid out, 410 visible (I1-I3: 514 laid out, 388 visible;
today 328).

**F · The panel pulled up.** Opens at half the window (378 dp) when its rows need more, else at its own height;
pulls up to its full height. The photo above is dimmed and lifted so the picked item stays in view (IM, row 10).
Photo 536 laid out; 214 visible at half (I2-I3: 192).

**G · After Survey.** Figures are shown and nothing is keyed: no gold, no keypad, no laser chip, no counts, no `▸`.
Wish list, Vendor review, Revisions: the tool row places AS Client wish / Vendor added. Estimation and Client
approved: no tool row at all. Photo 536, or 592 with no row (today 510).

### 2.4 The photo's height against today's

| State, portrait | Today `380c72d` | After I1 | After I2 | Final (I4) |
|---|---|---|---|---|
| Mark, nothing picked | 514 | 514 | 514 | 536 |
| a mark picked, nothing being measured | ≈460 | ≈460 | 514 | 536 |
| a mark picked and being measured (the usual state at Survey) | ≈404 | ≈460 by hand, 514 by the walk | 514 | 536 |
| keypad open (visible) | 328 | 388 | 388 | 410 |
| panel at half (visible) | none | none | 192 | 214 |
| 3 · Measure, pad shut | 510 | step gone | | |
| Estimation, Client approved | 510 | 570 | 570 | 592 |
| 1 · Lines (Reference on) | ≈403 (≈351) | same | same | ≈425 (≈373) |
| 360 | 514 | 514 | 514 | 536 |
| Plan (drawing box) | ≈576 | same | same | same |

**Landscape** (780 × 336 dp window). The photo is ≈302 dp high and ≈436 wide in every state today, because every
row lives in the 318 dp right column. It stays 302 through I3 and becomes ≈324 in I4, in every state. In the
column: top bar 32, tabs 56, strip 24, then 212 dp for the tool row (two rows of 44: `＋` and the four slots; `⋯`
and `9 ▸`). The keypad (6 + 4 × 36 = 150 dp) takes the tool row's place while open. The panel stays today's
full-width landscape sheet with the new action rows under its head. Nothing is lifted: nothing covers the photo
but the panel.

---

## 3. Every gesture in 2 · Mark

This section is the final form. I1 brings `Pick`, the figure taps, the empty tap and the drag rules; the panel's
three openings are I2's (until then the picked item's row opens the sheet and a long press opens today's menu); the
tool rules are I3's; the double tap is I4's.

Three things can be true of an item, held in one value (`Pick`, §13):

- **picked** — one item at most; it shows all its figures;
- **held** — picked by his own hand; it shows handles and it can move. A locked item, and one the phase holds
  read-only, is never held;
- **gold** — one dimension at most, at Survey only, always a dimension of the picked item.

The rule that replaces 3 · Measure's "nothing can be dragged" (ours): **only a held item moves, and whenever the
gold moves by itself the handles go.** A mark the walk has reached cannot be dragged by the thumb of the hand that
is not holding the laser. One tap on it gives the handles back.

**With no tool armed**

| Touch | On | Does |
|---|---|---|
| Tap | a figure | It turns gold; its mark is picked (held only if it already was). After Survey, or on a mark not in the room: picks the mark and says the existing refusal |
| Tap | the gold figure | The keypad opens (§5) |
| Tap | an item's body (mark, Tape band, Line, area, chain) | Picked and held. At Survey its first unread figure turns gold |
| Tap | the picked item's code | The panel opens (§6) |
| Tap | a handle | Nothing moves (`SurveyGuide.EndGate`, today a Line's ends; now tabs and corners too) |
| Tap | empty photo | Pick, handles and gold all clear; an open keypad stores what is typed and shuts. (Today: nothing, `SPS:3020`) |
| Long press | any item | Picks it and opens the panel. (Today: the menu) |
| Double tap | empty photo | Zooms 2× about the point; `fit` returns (ours, slice I4: one thumb cannot pinch) |
| Drag | a handle of the held item (16 dp reach; a Line's end 14 dp) | Resizes, or moves that corner or end; magnifier, guide rays, snap as today |
| Drag | the held item's body | Moves it whole after 8 dp (today from the first pixel), snapping as today. Tape, Line, area: F1's body-move |
| Drag | anything else, an unpicked mark included | Pans. (Today a press on any mark picks and drags it) |
| Pinch | | Zoom ⅓ of fit to 8×, as today |

**With a tool armed**

| Touch | Does |
|---|---|
| Tap | A type: places its default box centred there (a point: its spot), picked and held, first figure gold at Survey. Then the tool goes off, unless it was kept on (§7) |
| Drag | A type: places and carries it. Tape, Line: pulls one out, 5° snap. All as today |
| Tap, Line on | A corner of an area; a double tap closes it, as today |
| ✕ on the chip, or the lit slot again | The tool goes off |

The hit order under a press is today's (`SPS:3053-3078`, `SurveyPrepTouch.hit`): the held item's handles, a
figure, the picked Tape / Line / area's body, then marks topmost first. What changes is that a press on an item
that is not held does nothing until the finger lifts.

The 360 step keeps its own gestures as built: nothing is lasered there, so it needs no guard.

---

## 4. The laser

### 4.1 Nothing on the screen fires it

The meter's own button takes the reading (IM, row 15). He fired ours by accident twice; ImageMeter's own table does
not claim it can trigger a D2; and with one hand on the meter its button is already under a thumb. So `fire()`
(`SPS:610`), the bar's D2 and, in slice I4, the two D2 buttons beside the reference's W and H in 1 · Lines all go.
The command that asks the meter to shoot (`DistoClient.kt:166`) is deleted with its last caller, not kept behind a
switch: it holds no user data, and a control nothing shows is a second answer waiting to be found.

### 4.2 The link, and the chip that shows it

Today the Bluetooth link starts only when the on-screen D2 is pressed (`SPS:518-527`). With that button gone:

- **It starts by itself** when survey prep opens at Survey and the Bluetooth permission is already held (IM, row
  16). One attempt; a failure says the existing `sp.laser.*` words once.
- **The laser chip** in the top bar shows it: grey = off, pulsing = connecting, gold = ready. A tap on grey asks for
  the permission if needed and connects; a tap on gold says `sp.laser.ready`. It never fires. It is in the top bar,
  out of the thumb's reach while measuring, and beside nothing he presses often.
- A link that drops says `sp.laser.disconnected_x` (existing) and the chip turns grey. The chip is the standing
  sign; nothing nags at every figure.
- After Survey there is no chip and no link.

### 4.3 A reading arrives (the routing stays `SurveyRef`'s, `PANO/SurveyRef.kt:396`)

1. **A figure is gold.** The reading is written to it; one haptic tick; typed digits, if the keypad was open, are
   dropped and the keypad shuts (as today, `SPS:640-642`).
2. **The gold moves by itself** (Amit's answer). First to the same item's next unread figure — width, height, then
   position, the checklist's order — then to the face's next unread figure in `MeasureWalk`'s order, whose mark
   becomes the picked one without handles. The photo pans only when the new gold figure is off screen
   (`MeasureFlow.panTo`).
3. **The wall is finished:** nothing is gold, and the note says `sp.measure.face_done_x` ("Front wall · all 9
   measured ✓"), or `sp.measure.room_done_x` when it was the room's last. The face never changes by itself: the
   tabs' amber counts show where figures are left.
4. **A reading that fails the check does not move on** (ours). Double buzz, the plate reads `1234!`, the note says
   `sp.measure.photo_says_x`. The next reading replaces it; `▸` accepts it and moves on. So a double press of the
   meter cannot quietly put a door's width into its height.
5. **Redoing a reading:** tap the figure, shoot again. ↶ does not undo a reading, as today.

### 4.4 A reading with nothing gold

Refused, said, not stored — ImageMeter's rule (row 5) and today's (`SPS:638`): `sp.msg.d2_x_tap_measurement`, "D2
1203 mm — tap a measurement first, then read again". Nothing is held back for a later tap, and nothing lands on a
figure he was not looking at. A tap on any figure, or `▸`, starts measuring.

Other landings, unchanged: an armed reference box in 1 · Lines takes it as the reference's W or H; after Survey it
is refused in the phase's words; on the 360 it is refused with `sp.laser.on_360_x` (§11).

A locked mark still takes readings: our lock holds its position, not its figures.

---

## 5. Typing a value

- **What opens the keypad:** a tap on the gold figure — so, on a figure that is not yet gold, two taps. Never the
  laser. (What opens ImageMeter's is NC; this is ours.)
- **Where and what:** it rises from the bottom over the tool row and the photo's foot, 182 dp, and it is gone when
  it is not in use. Four rows of 44 dp, today's keys (`MeasurePad`): `1 2 3 ⌫` / `4 5 6 Clear` / `7 8 9 Next ▸` /
  `⌄ 0`. Millimetres only.
- **No display line** (ours). The digits appear on the gold plate as they are typed (`MeasurePad.live`). The photo
  is lifted so that plate stands 12 dp clear of the keypad (`PhotoLift`, §13).
- **Next ▸** stores the figure and moves the gold exactly as a reading does; the keypad stays up.
- **Clear** empties the plate. Leaving an emptied plate that held a reading removes the reading, and the estimate
  returns (today's `commitPad`, `SPS:591`).
- **It goes away** by `⌄`, Back, a tap on the photo, or a laser reading. The first three store what was typed.
- In landscape it takes the tool row's place in the right column and covers no photo.

---

## 6. The panel that slides up

Our Mark sheet (`APP/MarkSheet.kt`, `PANO/MarkSheet.kt`) with the acts that today stand in rows under the photo.

- **What opens it:** a tap on the picked item's code on the photo; a long press on any item; `⋯` in the tool row.
  (Today: the label in the picked item's row.)
- **How it rests:** it opens at half the window when its rows need more than that, else at its own height, and
  pulls up to its full height (`ModalBottomSheet` with its half rest on; today it skips it). The photo above is
  dimmed, and lifted so the item stays in view. There is no collapsed strip: at rest the tool row is all there is.
- **A Tape and a chain get the panel too** (today they have none): head and acts only.

**Rows, top to bottom (portrait)**

| Row | dp | Holds |
|---|---|---|
| Grip | 16 | a small bar to pull by |
| Head | 40 | `FW1 · Window` · the trade's dot and short name · ✕ at the right |
| Acts, line 1 | 44 | **Delete** at the LEFT end (red outline; `Drop` / `Restore` past Existing) · 24 dp gap · `Lock` / `Unlock` · `Duplicate` |
| Acts, line 2 | 44 | `Fine move` · `Four corners` / `Rectangle` (Q2; only a type that may take four corners) · `Lock all` |
| KIND | 32 | the type's kinds as chips, as today |
| STATE | varies | the now-line, the four acts (↻ Replace ⇢ Shift ⚒ Repair ✕ Remove), the moves with their Why boxes, as today |
| TRADE | 40 | on an Other, a Note, a Line, an area, as today |
| NOTE | ≈96 | two lines and who reads it, as today |
| Done | 48 | as today |

- **The type is shown, never changed here:** changing it would rename the code and every reading key.
- **Why Delete is where it is.** It is inside a panel that opens only on purpose; it is a second, separate tap; it
  is at the left end of its line, away from ✕ (right end of the head above) and far above Done; the half-open
  panel's lowest visible row sits where `⋯` was, and it is never Delete. Deleting closes the panel and shows "FW1
  deleted · UNDO" for 5 s (`sp.msg.x_deleted`); ↶ restores it too. **Delete has this ONE place:** the picked item's
  row, the floating trash and the long-press menu all go.
- **The sheet stays a draft** for kind, trade, state and note: Done writes them in one undo step; ✕, Back, a swipe
  down and a tap on the dimmed photo drop them, as today. **An act is Done followed by the act** — what was typed is
  kept — except Delete, which drops the draft and deletes. Each act closes the panel.
- **What the phase forbids is not drawn** (`SurveyPrepEdit.menuItemsOf`'s rules, which become the acts' rules): no
  Delete, Duplicate, Lock or shape on a mark of an earlier phase; on a locked mark Delete is drawn and answers the
  lock's words.
- **Fine move** closes the panel and puts a small nudge pad over the photo, in the bottom corner farthest from the
  item: ▲ ◀ ▶ ▼ round a centre cell that switches the step between 1 and 5 mm, and ✓. 3 × 3 cells of 40 dp. It
  nudges the part last touched (the whole item, an edge, a corner, an end), drawn solid. It never snaps. ✓ or a tap
  on empty photo ends it. It is there only while he is using it; it replaces the standing row of nudges.

---

## 7. The tools

- **Three pinned types and the last one used** sit in the tool row (ours, on IM's row 9):
  - wall: Door · Window · Switch plate, then last-used, seeded Electric point;
  - floor: Column · Step · Electric point, seeded Carpentry unit;
  - ceiling: Beam · Electric point · Bulb point, seeded Fan.
  The pinned three never move. A type chosen in the Add sheet that is not pinned takes the fourth slot.
- **Door, window, switch plate and electric point are ONE tap** and then the photo. Every other type and kind is
  two: `＋`, then its tile.
- **`＋` opens the Add sheet** (IM, row 8), headed groups, each tile an icon with its name:
  1. **Most used here** — the surface's three pinned types and its seed;
  2. **Measure** — Tape, Line (the Chain is off, `Features.CHAINS`);
  3. **By trade** — today's More ▸ rows as built (`SurveyPrepPalette.moreRows`), General first; it scrolls.
- **A tool places ONE mark and switches itself off** (ours; today it stays on). With measuring on the same screen a
  tool left on would drop a second door under the next stray tap.
- **A long press on a slot keeps it on** (six sockets on one wall): the chip says `sp.tool.stays_on_x` until ✕.
- **The kind** is on the tool chip before placing ("Wardrobe ▾") and in the panel after; remembered per type and
  surface, as today.
- **Select is no longer a tool.** With no tool armed, a touch selects.
- After Survey the row places AS Client wish or Vendor added; at Estimation and Client approved it is not drawn.

---

## 8. Moving and reshaping

### 8.1 Marks, Tapes, Lines, areas

- Only a HELD item moves (§3). Its body moves it whole after 8 dp; a handle resizes it or moves that corner or end.
- A Tape, a Line and an area move by the body (F1, `SPS:2760`, `SurveyGuide.kt:340`), and by an end with its 4 dp
  dead zone, guide and magnifier, as built.
- The magnifier (120 dp, 3×), the guide rays and the snap are today's. The escape from a snap is to zoom in; the
  last millimetres go by Fine move, which never snaps.
- A locked item shows its padlock and no handles; a drag on it says the refusal once, in today's words. A mark an
  earlier phase holds read-only: the same, with the phase's words.
- One undo step per drag.

### 8.2 What `DESIGN-quad.md` §3 keeps, and what moves

Kept as written: `freeId` and the one rule `Quad.free(e, freeId, part)`; the parts `q0…q3`; four round handles of
5.5 dp pressed within 16 dp; the corner snap, its guide rays and its escape; "a corner that would break the shape
stops"; duplicate, undo, lock, phases; the outline, the hit inside the shape; `Role.SIDE` and `Role.DIAG`; the 360.

Moved by this design:

| `DESIGN-quad.md` says | Now |
|---|---|
| §3.1 the long-press menu gains the row `Four corners` / `Rectangle` | the panel's Acts line 2 holds it; there is no menu |
| §3.2 "the row then reads `FW1` over 'corner' and shows the ◀ ▶ ▼ ▲ nudges" | Fine move's pad nudges the corner last touched |
| §3.4 "the floating trash keep[s] the bounding box" | there is no floating trash |
| §3.5 "What 3 · Measure draws" | what 2 · Mark draws; sides and the diagonal count as size, so every four-corner mark shows them |
| §3.5 "The bar says the side's words" | dropped. Each side's plate sits on its own side, and the gold plate is on the side being measured |

### 8.3 The four-corner mark on this screen

Pick the window; open the panel; `Four corners`. The panel closes, the mark shows four round corners, and the note
says `sp.shape.drag_corners`. Each corner is dragged by itself under the magnifier. Each side shows its own figure
on that side, the diagonal inside if asked. The walk inside the mark is bottom, right, top, left, the diagonal, then
the type's own, as `DESIGN-quad.md` §3.5 orders it. `Rectangle` in the panel puts it back.

---

## 9. What is left to measure, without a bar

- **The face tabs' amber counts** — the room at a glance. They now show in the Mark step.
- **`9 ▸` at the end of the tool row** — the open face's count, and the way to the next unread figure by hand. A
  wrong tap only moves the highlight. It is not drawn when the face has nothing left, or after Survey.
- **`✓` after a mark's code** once all its figures are read.
- **The note** when a wall, and then the room, is finished (§4.3).
- No `‹`: to go back to a figure, tap it. No "7 of 18 measured" and no next-face button.

---

## 10. Phases, locks, wishes

- **2 · Mark is in every phase.** After Survey it is the view: figures show, nothing turns gold, no keypad, no
  laser chip, no counts, no `▸`. A tap on a figure picks its mark and says the phase's existing refusal
  (`PhaseTools.readingRefusal`). Today those phases are sent to 3 · Measure (`PhaseTools.prepMode`).
- **Placing:** as Client wish in the Wish list, as Vendor added in Vendor review and Revisions (`PhaseTools.place`),
  unchanged. No tool row at Estimation and Client approved.
- **Editing:** `PhaseTools.editable` decides who can be held. A mark that cannot be held shows no handles, and the
  panel does not draw the acts the phase forbids.
- **Delete** on a mark past Existing is Drop / Restore, in the panel, as today's rule (`PhaseTools`, `:388`).
- **A mark that is not in the room** (born a wish or Vendor added) shows estimate-style figures, never turns gold
  and is not counted. A tap on its figure picks it and says `sp.measure.not_in_room`.
- **Locked marks** take readings and do not move.

---

## 11. The 360 and the Plan

- **3 · 360** (today's 4 · 360): the same tool row and the same panel. It draws no figures, so nothing is measured
  there: a reading is refused with `sp.laser.on_360_x`. This is a loss against today, where the bar let a figure be
  lasered from the 360. Its gestures stay as built.
- **The Plan tab:** unchanged.
- `sp.reference_shown_measure` ("Reference shown on 2 · Mark, 3 · Measure and 4 · 360") is replaced by
  `sp.reference_shown_mark`.

---

## 12. The texts

English first: each new key's Marathi value is its English for now, and each is listed in
`android/i18n/mr-pending.tsv`.

| Key | English | Slice |
|---|---|---|
| `sp.laser.off` | Laser not connected · tap to connect | I1 |
| `sp.laser.ready` | D2 connected · press the meter's own button to read | I1 |
| `sp.laser.on_360_x` | D2 {mm} mm — not used: readings are taken on the wall's own tab | I1 |
| `sp.measure.aria.next_x` | Next figure to measure · {n} left | I1 |
| `sp.reference_shown_mark` | Reference shown on 2 · Mark and 3 · 360 | I1 |
| `sp.sheet.aria.open_x` | Options for {code} | I2 |
| `sp.sheet.fine_move` | Fine move | I2 |
| `sp.sheet.unlock` | Unlock | I2 |
| `sp.fine.aria.step_x` | Step {mm} mm · tap to change | I2 |
| `sp.fine.aria.done` | Finish fine move | I2 |
| `sp.shape.four_corners` · `sp.shape.rectangle` · `sp.shape.drag_corners` | as `DESIGN-quad.md` §3.1 | Q2 |
| `sp.add.aria.open` | Add a mark or a measure | I3 |
| `sp.add.most_used` | Most used here | I3 |
| `sp.add.measure` | Measure | I3 |
| `sp.add.by_trade` | By trade | I3 |
| `sp.tool.aria.again_x` | {type} again | I3 |
| `sp.tool.stays_on_x` | {type} stays on — ✕ to stop | I3 |

**Reused as they are:** `sp.msg.d2_x_tap_measurement`, `sp.msg.laser_still_connecting`, every `sp.laser.*` the
client already says, `sp.measure.face_done_x`, `.room_done_x`, `.photo_says_x`, `.not_in_room`, `.x_to_measure`,
`.aria.type` (on the gold plate), `sp.msg.x_deleted`, `common.lock`, `common.duplicate`, `common.delete`,
`common.lock_all`, `common.done`, `sp.corner`, and the checklist's own names for a `⊥` figure.

**Deleted:** `sp.measure.all`, `sp.measure.aria.all`, `sp.measure.aria.previous`, `sp.measure.aria.skip` (replaced
by `.aria.next_x`), `sp.measure.x_of_x`, `sp.measure.next_face_x`, `sp.reference_shown_measure`; and
`common.measure` and `common.more` where nothing else reads them.

---

## 13. What goes, what stays

Prefer deleting to hiding: none of this holds a user's data.

**Deleted**

| What | Where | Slice |
|---|---|---|
| The measure bar: `SpMeasureBar`, `MbArrow` | `APP/SurveyMeasure.kt:72-200` | I1 |
| Its layout model and test | `PANO/MeasureBarLayout.kt`, `TEST/MeasureBarLayoutTest.kt` (6) | I1 |
| The step 3 · Measure: `StepRoute.MEASURE`, `measureBarDp`; `measurePanel` (`SPS:2006-2019`); `barStep` (`SPS:1184`); the Measure pane's tap rule (`SPS:2993-2996, 3026-3032`) | `PANO/StepRoute.kt`, `SPS` | I1 |
| `MeasureFlow.Bar`, `Kind`, `onFace`, `onItem`, `move` (WALK / STAY / DROP) | `PANO/MeasureFlow.kt`; `TEST/OnFaceTest.kt` (4) and those cases of `MeasureFlowTest` | I1 |
| `MeasureText`'s progress and next-face texts | `PANO/MeasureText.kt`; cases of `MeasureTextTest` | I1 |
| `all`, and `DimLayout.Scope` with it (the Mark step always shows what `Scope.FACE` shows) | `SPS:3449-3473`, `PANO/DimLayout.kt:114-121` | I1 |
| The on-screen D2 and `fire()` | `SPS:610-615`; `DeleteGuard.rowToBarDp`, `clearOfD2Dp` | I1 |
| The picked item's rows (label, nudges, padlock, Delete) | `SPS:1885-1966` | I2 |
| The long-press menu `SpCtxMenu` and `ctxPick` | `APP/SurveyPrepLocks.kt:123`, `SPS:1107` | I2 |
| The floating trash: `trashSpot`, `keepOfMark`, `keepOfLine`, `DeleteGuard` | `PANO/SurveyPrepTouch.kt:175-300`, `PANO/DeleteGuard.kt`; `TEST/TrashSpotTest.kt` (12), `TEST/DeleteGuardTest.kt` (9) | I2 |
| The palette row, Select as a slot, the sticky toggle, More ▸ as a list | `SpPalette`, `SpMoreSheet`, `drawSelectIcon` (`APP/SurveyPrepTools.kt`); `SurveyPrepPalette.SEL` as a slot, `slots`, `next`, `MAIN` | I3 |
| The caption row | `SPS:1580-1594` | I4 |
| The two D2 buttons in 1 · Lines, and the client's shoot command | `SPS:1820, 1051`; `APP/DistoClient.kt:166` | I4 |

**Kept behind a switch:** only what already is — `Features.CHAINS` (off). A room that has a chain keeps it: drawn,
picked, its segments gold in turn and counted, deleted from the panel.

**New pure rules in `:pano`** (this container cannot compile `:app`, so every rule the screen follows is a pure
function with its own test):

- `Pick` (`PickTest`): picked / held / gold as one value, and one function per event — a tap on a body, on a
  figure, on empty; placed; a reading landed; Next ▸; `▸`; a face or a step left. §3's rule lives here and nowhere
  in `:app`.
- The landing in `SurveyRef`'s route gains "a reading that fails the check stays" (§4.3, 4).
- `PhotoLift` (`PhotoLiftTest`): how far the photo is lifted for a keypad or a panel of a given height.
- `LaserChip` (beside `LaserNote`): the client's state → off / connecting / ready.
- `MarkSheet.acts` (from `SurveyPrepEdit.menuItemsOf`, `:312-365`), and Tape and chain as sheet items (I2).
- `NudgePad` (`NudgePadTest`): which corner the pad stands in, what a press moves (I2).
- `ToolRow` (`ToolRowTest`): the pinned three, the last-used slot, one-shot and kept-on (I3).

**Tests that are rewritten, not deleted:** `MeasureScreenTest` (15; it reads the two screen sources by name),
`StepRouteTest` (`:143-147`, `:186-211`), `PhaseToolsTest` (`toolOf`, `prepSteps`, `prepMode`), `MarkSheetTest` (the
bar's label cell), `BodyMoveTest` ("this step moves things" becomes "this item is held"), `PaletteTest`,
`MeasureFlowTest`, `MeasureTextTest`; `i18n/literal-counts.tsv` and `mr-pending.tsv`.

**Stays as it is:** `MeasurePad` and `SpMeasurePad`, `MeasureWalk`, `MarkDims` (H1's constants included),
`DimLayout`'s placing, `SurveyPlan.figure`, `GestureClassifier`, `SurveyGuide`, `SurveyRef`, `DistoClient`'s link,
the Mark sheet's own rows and its draft, the phase band, the face tabs, the ⋮ menu, the Plan, the 360's gestures,
the record and every reading key.

---

## 14. The slices

Each is one Play build, reviewed before it ships. Sizes: S, M, L.

**F1 · the chain off, a Tape moved by its body** (built and reviewed: `136b4c7`, `380c72d` on `batch-next`).
It ships first, as it stands. What survives this design: `Features.CHAINS`, the body-move of a Tape, Line and area.
What I1 and I2 then delete: its rearranged bar and its delete clearances. Its build is on the phone for the day or
two before I1.

**I1 · one step, and nothing about measuring under the photo (L).**
The steps are Lines, Mark, 360. The measure bar, the figure box, `all` and the on-screen D2 are deleted. In 2 · Mark
every mark shows its size; a tap on a figure makes it gold; a reading lands there and the gold walks on along the
wall; a failed check stays; a tap on the gold figure brings the keypad up over the foot of the photo with the digits
on the plate; `Pick` holds picked / held / gold; an empty tap clears; only a held item moves. The laser chip is in
the top bar and the link starts by itself. Today's `›` becomes the count chip `9 ▸` at the right end of the palette
row. The tabs' counts show in Mark.
Still as today for this one build: the palette with its Select slot and its tools that stay on, the picked item's
row (shown only while an item is held), the long-press menu, the floating trash. So one jump is left in the photo's
height, 460 ↔ 514 dp as the handles come and go; I2 ends it.
*Amit sees:* one step; no box under the photo; the photo keeps its height while he lasers along a wall (514 dp where
it was ≈404); nothing on screen can fire the D2.

**I2 · the panel slides up; the standing rows go (M).**
The Mark sheet opens half way and pulls up; its two lines of acts; Tape and chain get it; a tap on the picked item's
code and a long press open it (`⋯` joins the row in I3, when the palette's seven slots make room); Fine move's pad;
the photo lifts under the panel. The picked item's row, the menu and the floating trash are deleted.
*Amit sees:* picking a mark changes nothing under the photo (514 dp in every state); Delete in one place, behind a
deliberate tap, one UNDO away.

**Q2 · the four-corner screen (M, `DESIGN-quad.md` §6 with §8.2 above).**
After I2, because its `Four corners` / `Rectangle` control and its corner nudges live in the panel. `port63-quad`'s
pure model merges first. If Amit wants it before I2 it can ship on the long-press menu as `DESIGN-quad.md` wrote it,
and I2 then moves the row; Claude recommends after.
*Amit sees:* a sloping window marked corner by corner, each side with its own figure.

**I3 · the tool row and the Add sheet (M).**
`＋`, three pinned types, the last-used slot, one-shot tools, a long press to keep one on, Select gone as a tool, the
Add sheet in place of More ▸.
*Amit sees:* Door, Window, Switch plate one tap away; every other type under `＋`; no second mark from a stray tap.

**I4 · less on the photo and above it (S-M).**
Unpicked marks' sizes sit on their edges with no lines; the caption folds into the step strip (the photo gains 22
dp); double tap zooms; the reference's W and H boxes in 1 · Lines are armed by a tap, their two D2 buttons and the
client's shoot command are deleted.
*Amit sees:* a calmer busy wall; a taller photo; no control anywhere that fires the laser.

Not in these slices, and not changed by them: the saved gallery pictures ("Measured figures only") and saving a room
from one surface — port 63's own later slices. I4's on-the-edge drawing is the screen's; whether the saved pictures
take it is decided in that slice.

---

## 15. Open questions for Amit

Four, each answerable from one static picture of the screen. I1 does not wait for any of them: it is built with the
first option of Q4 and he judges it on the phone.

**Q1 · S2 APK — the panel that slides up (before I2).** Which is the "slide up screen" Amit means?
1. **(Recommended) The picked mark's options**, opened half way: Delete · Lock · Duplicate · Fine move · Four
   corners, then kind, state, note. It is the ImageMeter sheet a vendor picture shows over a selected element, with
   the mark still in view.
2. A low strip of the quick acts only, about a fifth of the screen, pulled up for kind, state and note.
3. He means the sheet "+" opens — the tools — and the mark's options can stay a full sheet as today.

**Q2 · S2 APK — the one row under the photo (before I3).**
1. **(Recommended) `＋` · Door · Window · Switch plate · last-used · `⋯` · `9 ▸`**: his common types one tap away.
2. ImageMeter's own: `＋` · last-used · `⋯` · `9 ▸`. Emptier; a door is two taps.
3. No row at all: `＋` and `⋯` float on the photo's corners. Tallest photo; they cover a corner of it.

**Q3 · S2 APK — a busy wall, "every mark's size, always" (before I4).** Claude draws his busiest wall (about twenty
figures) both ways.
1. **(Recommended) Sizes on the edges, no lines; a switch plate's size is one small plate beside it; a point shows
   no size.**
2. The same, but a mark narrower than 40 dp on screen shows its size only when picked or zoomed in.

**Q4 · S2 APK — typing a figure (I1 ships option 1).**
1. **(Recommended) The keypad alone; the digits appear on the gold plate on the photo.**
2. The keypad with one line above it naming the figure ("FW1 · width  1198"). Easier to read while typing; it is a
   line about a measurement under the photo, there only while the keys are up.

---

## 16. Risks

1. **Crowding at 360 dp.** Every mark's size always, plus the held mark's handles and position plates, on a 348 dp
   photo. A switch plate is about 25 × 11 dp, under its own 32 dp tap box, so a tab, a plate and a body compete.
   Q3's picture comes before I4; the gold figure is always placed first and always whole.
2. **A reading in a figure he was not looking at.** The gold moves by itself, so he shoots without looking. The
   check passes anything within 150 mm or 15 % of the photo, so a near-square window can take its width as its
   height unseen. Answers: the order never varies, a failed check stays, the plate that took the reading rings gold
   for a second, the gold must read from arm's length. ↶ still does not undo a reading.
3. **A slip while lasering.** The read-only step is gone. `Pick`'s rule (only a held item moves; the walk never
   holds) is the whole guard, and it lives in one tested function for that reason. If it leaks, estimates drift
   under his thumb.
4. **A silent laser.** With no D2 button, a failed link looks like a meter not yet pressed. The chip is the only
   standing sign; if he misses it on site, the fix is words when a figure turns gold with the link down, not a fire
   button.
5. **No remote trigger at all.** Pressing the meter's own button can nudge its aim. If he asks for it back it
   returns as a deliberate control designed then, not as today's button.
6. **The lift.** The photo is moved, not rescaled, under a keypad or a panel. Touches must map through the lift, and
   at fit zoom a figure in the bottom third depends on it entirely.
7. **The half-open panel** rests at half the window by the stock sheet's own rule, not at a height we choose. If
   Q1's answer is the low strip, the sheet becomes our own drag code: more to build and to test on the phone.
8. **Measuring from the 360 is lost** (§11). If he used it, a row in the panel that opens the mark's own wall is the
   repair.
9. **The phase table.** `PhaseTools` copies the approved prototype's table, whose third step is the list. Keeping
   2 · Mark in every phase as the view departs from that row; S5 reads none of it.
10. **`:app` cannot be compiled here.** I1 rewrites the centre of a 3,669-line screen. Every rule goes into `:pano`
    first; CI on `batch-next` is the only compiler, and each slice merges only on a green run of every workflow.
11. **Volume of test change.** More than fifty tests are deleted or rewritten in I1 alone (§13). A rule that
    survives must be re-anchored, not dropped with its old test.
12. **F1 and this design overlap.** F1's bar work lives for one build. That is the price of shipping what is built
    and reviewed instead of holding the chain and the Tape fix behind a larger change.
13. **Landscape** gains nothing in photo height until I4 and its panel stays a full-width sheet. Accepted on the
    assumption that he surveys in portrait; nobody has asked him.
14. **The tours** (off) keep anchors on a step that no longer exists. They are rewritten when Amit lifts the hold.
