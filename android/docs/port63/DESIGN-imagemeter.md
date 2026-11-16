# DESIGN-imagemeter — one screen to mark and measure, worked the way ImageMeter is

S2 APK, port 63. Written 16 Nov 2026 (IST), after Amit's three messages on build 0.3.337 / 0.3.339, and revised the
same day by the plan critic's nineteen findings (§17). **Design only: nothing here is built.** It replaces the
planned slice "F2 · one step for marking and measuring" of `DESIGN-port63.md` §11 and changes where the four-corner
screen (`DESIGN-quad.md` §3, slice Q2) hangs its controls.

- **Read against** `batch-next` `380c72d` (H1 `8455c77`, the phone's 0.3.339, plus slice F1 and its review). Line
  numbers are that commit's. `SPS` = `android/app/src/main/kotlin/com/malletcrafts/interiorestimate/SurveyPrepScreen.kt`,
  `APP/` = that folder, `PANO/` = `android/pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/`, `TEST/` =
  the same under `src/test`.
- **This file is committed on `port63-im`, whose parent is `8455c77`.** The build starts from `batch-next` once F1 is
  on it (`380c72d` or what follows it), not from this branch's parent.
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
2. NOTHING under the photo shows a measurement: no measure bar, no figure box, no list, no number and no words about
   a dimension — in every slice, the first included. A figure lives on its annotation and only there.
3. The way of working is ImageMeter's: pick an element, its value is on it, the laser's reading goes to the picked
   one, a keypad only when he wants to type, a panel that slides up for the rest.
4. Everything that is ours stays: every mark type and kind, the trades, Notes, wishes and their states, client /
   site / project / room.
5. Far less on screen at once.

**Where the sources do not carry the reading above, said plainly:**

- "a slide up screen in imagemeter" could be any of three things (§1.3). Vendor pictures show two sheets that rise
  from the bottom: the "+" tools sheet and "Tags and properties". This design builds the second for a picked item and
  the first for adding, and asks him which he meant (§15, A1).
- "Move to the next unmeasured figure on that wall by itself" is NOT what ImageMeter does. It steps through the
  labels of ONE element after a reading and never to another element. Crossing to the next mark is ours, by his
  answer.
- ImageMeter shows `?` on an empty value. We show the photo's estimate, marked `≈` (decided in `DESIGN-port63.md`
  §1.1). ImageMeter's nearest thing is a value computed from a reference, which it shows as one plain figure.
- **His (b) asks that the D2 not be fired BY ACCIDENT** ("very close"), said once; it does not ask for the button to
  go, and F1 has already moved it apart. Taking the fire control off the Mark step altogether is OURS (§4.1), and
  whether the phone should ever fire the meter is asked (§15, A5).
- **His message (c) did not answer whether the gold highlight (H1) can be seen**, and his (a) says he could not see
  which mark was being measured. The whole walk without handles (state D) rests on the gold being unmissable —
  ImageMeter's own answer is handles on exactly one element (row 6). So it is asked, on the phone he holds, before
  I1 is built on it (§15, A6).
- **1 · Lines keeps its panel, the reference's W and H boxes included.** They are typed sizes of the reference, not
  a mark's measurements. Said here so that it is a decision and not an oversight: read literally, two figure boxes
  under the photo are "explicit box below foto". If he wants them on the photo too, it is a slice after I4.

---

## 1. What ImageMeter does

### 1.1 Patterns we copy

Each is confirmed by a vendor source, but for row 16, which is kept so that its number stands: there only a
setting's NAME is known.

| # | ImageMeter | Source | What we do with it |
|---|---|---|---|
| 1 | At rest the editor is the photo and ONE toolbar row. No box, bar or list of values under or beside the photo | manual `/manual/editor/editor-screen-overview/`; 12 editor pictures opened | One tool row under the photo and nothing else there; the band, top bar, tabs and strip stay above it (§2) |
| 2 | Every value is written on its annotation and nowhere else | pictures `bluetooth/connect/img/im-use-1.jpg`, `im-use-2.jpg` | The same. Ours: an unread figure is the photo's `≈` estimate, `?` only where the photo cannot show it |
| 3 | A laser reading goes to the ACTIVE measure; no dialog opens; the value entry must stay shut to receive it | `/manual/bluetooth/connect/`, `/manual/faq/` | A reading lands on the gold figure (§4) |
| 4 | After a reading the selection steps to the element's NEXT LABEL by itself | `/manual/bluetooth/connect/` | The same inside a mark; then on to the wall's next unmeasured figure (ours, Amit's answer) |
| 5 | A reading with nothing active is refused with a reason ("no active element") | app text, v26.6.0; how it is shown is NC | Refused and said, as today, with its own buzz (§4.4) |
| 6 | A selected element is shown by its HANDLES, with no colour change: discs on a line's ends; corner circles, a centre move glyph and a rotate glyph on a rectangle | pictures `im-use-1.jpg`, `templates/tutorial-partslist/img/260501-edit.element.properties.png` | Handles mean "this can be moved". The gold of H1 is ours, on top, for "this takes the next shot" |
| 7 | Toolbar buttons that need a selection are greyed while nothing is selected | pictures `tutorial-areasum/…-113241` against `…-113032` | `⋯` in the tool row is grey with nothing picked |
| 8 | "+" opens a sheet over about three-quarters of the screen: headed groups, each tool an icon with its name | picture `editor-screen-overview/img/add-tools-menu.png` | The Add sheet (§7) |
| 9 | The last-used tool has its own place beside "+", to add several of a kind without the sheet | manual text | The fourth slot of the tool row |
| 10 | "Tags and properties" is a sheet over the lower 31-43 % of the screen; the photo above is dimmed and the element stays in view; one OK | pictures `…-113250`, `260501-edit.element.properties.png`; `/manual/templates/properties/` | The FORM of the panel (§6): our Mark sheet, opened half way in portrait, the mark kept in view. What it holds is ours |
| 11 | Lock: a locked element cannot be moved, its other attributes still can | manual text | Our padlock already means this. A locked mark still takes readings (ours) |
| 12 | Delete acts on the selection only; no confirm is evidenced; Undo is one button. Delete, Lock and Duplicate are in its ONE toolbar row | manual text; app strings searched | Ours: Delete is inside the panel, its LAST act, with UNDO for 5 s and ↶ (§6) — never under the thumb |
| 13 | A rectangle can become a free four-sided shape, each corner moved by itself | release note v3.9.13-2 (in an HTML comment of `/download/`); app strings | The four-corner mark (§8.3) |
| 14 | A rectangle's width sits ON its top edge and its height ON a side edge, turned along it, with no separate dimension line | pictures `…-113241`, `East_building_side.jpg` | The size plates of a mark that shows no handles (§2.2). Ours: height on the right edge |
| 15 | The meter's own button is the default. ImageMeter ALSO offers an on-screen "Trigger laser remotely" (with a delay) where the meter supports it; its device table gives the Leica D2 as "?", not established | `/manual/faq/`, `/manual/bluetooth/devices/`, app text | Ours: no fire control in 2 · Mark (§4.1); asked (A5) |
| 16 | A setting NAMED "Auto-connect in editor" exists; what it does is NC. The manual shows connecting on the main screen only | app setting text | Nothing copied. Starting the link by itself is OURS (§4.2) |
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
- **Its floating +/− zoom control** at the photo's right edge (seen in a vendor picture). Not copied: a double tap
  costs no space, and `fit` returns (§3, slice I4).
- **Delete, Lock and Duplicate in the toolbar row.** Ours are in the panel (row 12).
- **Its on-screen remote trigger** (row 15). Not on our Mark step; A5 asks whether it comes back, out of reach.

### 1.3 What could not be confirmed, and is not built on

| Claim | Standing |
|---|---|
| Which ImageMeter panel Amit means by "a slide up screen" | NC. Two sheets are seen in vendor pictures ("+" tools; Tags and properties); three more are known from layouts only (colour, value entry, a three-row toolbar) |
| A top bar appears for every selected element | NC. Seen once, for a perspective reference; not with a selected rectangle or circle |
| Pulling the toolbar's handle up reveals more rows | NC. The three-row structure is real; the gesture is an inference |
| What opens ImageMeter's value entry (one tap on a label, a second tap, a double tap) and where it sits | NC. Ours is decided in §5 |
| What "Auto-connect in editor" does | NC. Only the setting's name is known (row 16) |
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
| Face tabs, two rows; amber count = figures left | 64 | the count shows in the Mark step (today in 3 · Measure only). It is the ONE visible count |
| Step strip: **1 · Lines  2 · Mark  3 · 360** | 28 | three steps. Its lead text becomes the face's W × H (slice I4) |
| Caption (face · W × H) | ≈22 | goes in slice I4 (folded into the strip) |
| PHOTO | the rest | |
| Tool row | 6 + 50 | the ONE row under the photo. No figure, no number and no words about a dimension, ever |

Fixed chrome above the photo: 186 dp today and through I3, 164 dp after I4.

**The tool row** (348 dp wide), left to right, final form (slice I3):

`＋` 48 · three pinned types 44 each · last-used type 44 · free space · `⋯` 48 · `▸` 48  (= 320 of 348 dp)

**From I1 until I3** today's palette stands there without its Select slot: `Tape · Line · three types · More ▸ · ▸`,
seven equal slots of 49.7 dp (with Select kept and `▸` added it would be eight of 43.5). `▸` is the row's last
slot, where I3 keeps it; `⋯` arrives with I3.

- `＋` opens the Add sheet. The four slots arm a type in one tap (§7).
- `⋯` opens the panel for the picked item; grey with nothing picked (IM).
- `▸` moves the gold to the open face's next unread figure. It carries NO number: the open face's tab already shows
  the count, in amber, and the same number in the row would be a figure about measurements under the photo. The
  count is in its content description only (`sp.measure.aria.next_x`). Drawn at Survey while the open face has a
  figure left OR a figure is gold (§9). When it is not drawn its place stays empty: no other slot moves.
- Nothing in the row fires the laser and nothing in it deletes. Nothing above it deletes either: from I1 on no
  delete control stands in a row under the photo (§14).

**Over the photo, costing no height:** the tool chip while a tool is armed (top-left); `fit · grid · zoom %`
(top-right; `all` goes); the picked item's code chip (I2, §2.2); the Fine move pad while he is using it (I2, §6);
the magnifier while a handle is dragged; the note and the snackbar at the foot.

### 2.2 What is drawn on the photo

One figure per dimension, from `SurveyPlan.figure` as today: a reading `1198` white bold on black; no reading
`≈1203` grey on half-black, dashed; `?` amber where the photo cannot show it; `!` after a reading that fails the one
check.

- **A mark that is not picked:** its outline (type colour or state line), its code inside the box as today (with
  `✓` once all its figures are read), and its SIZE only. Width is one plate ON the top edge, height one plate ON the
  right edge, turned to read bottom to top; 10 sp. No dimension line and no arrowheads: the edge is the line (IM,
  row 14). A box too small for two plates takes `DimLayout`'s one compact plate (`≈200 × ≈90`) beside it, and with no
  room a 6 dp dot. A point (E, P, F, PLB, LV) has no size: its spot and code. A Note: its pin. A Tape, a Line, an
  area: its one figure on it, as today (`F-T1 1234 mm`, `F-M1 ≈1234 mm`, `F-M2 1.86 m²`).
- **The picked mark:** the same at 12 sp, plus where it sits — the line from the nearer corner, from the floor or
  ceiling, a Beam's drop, the `⊥` chip — each with its white line and arrowheads as today.
- **Its code is a CHIP** (I2): the code and `▾`, drawn outside the box above its top-left corner (below its
  bottom-left when the mark touches the photo's top), not inside it. Its tap box is at least 44 × 28 dp and at least
  16 dp clear of every tab, corner handle and end; `DimLayout` blocks it as it blocks a tab, so no plate lands on
  it. A tap on it opens the panel (§6). A picked Tape, Line, area or chain has the same chip off its first point.
  An unpicked item's code stays where it is drawn today; so does the state's tag (act glyph, `✎`, `V+`, pill)
  inside the box. Why a chip: today the code is drawn INSIDE the box at its top-left (`APP/SurveyPrepDraw.kt:571`),
  within the `lt` tab's 16 dp reach, and on a switch plate (about 25 × 11 dp) it is larger than the mark — its
  left half would be a handle, its right half the body, and nothing would be "the code".
- **A HELD mark** (picked by his own hand, §3) also has its handles: eight tabs, or four round corners on a
  four-corner mark, or a Line's two ends. **While it shows tabs it keeps today's hand-anchored width and height
  plates, clear of every tab, in every slice** (`SurveyPrepTouch.widthTag` / `heightTag` with `hand = true`; today
  keyed on `Scope.PICKED`, `PANO/DimLayout.kt:476`, re-keyed to the held mark, §13). On-the-edge plates (I4) are for
  marks that show no tabs: not picked, or picked by the walk.
- **The figure being measured (H1, kept as built):** its mark framed in gold, its own line and arrowheads gold and
  three times as heavy, its plate solid gold with `▶`, the rest of the photo 35 % darker. It is placed first and is
  never reduced to a dot. The `▶` is drawn HOLLOW while the laser chip is not ready (§4.2), so at the figure he is
  looking at he can see that a press of the meter will not arrive. A `⊥` chip that is gold carries its own name
  while it is gold (`⊥ reveal depth ?`, the checklist's existing words), because the photo cannot draw what it is.
- The plate that has just taken a reading keeps a gold ring for one second while the gold moves on, so the eye sees
  where the reading went.

Until slice I4 the sizes of marks without handles are drawn as 0.3.339's 3 · Measure draws them (a line 6 units
outside the edge). I4 moves them onto the edge.

### 2.3 The states (portrait; final form)

Each list is top to bottom and names only what differs from the frame.

**A · Nothing picked.** Photo: every mark as "not picked". Tool row: `⋯` grey; `▸` if figures are left.
Photo 536 dp (I1-I3: 514).

**B · A tool armed.** Over the photo, top-left: the tool chip, "Placing Door · tap the photo" with ✕ and the kind
button ("Wardrobe ▾"); after Survey "· as Client wish". Tool row: that slot lit. Photo 536 (514). One item is
placed and the tool goes off (§7).

**C · A mark held** (a tap on its body, or just placed). Photo: its handles, its code chip, its hand-anchored size
plates and its position figures. At Survey its first unread figure is gold (state D at once). Tool row: `⋯` live.
Nothing appears under the photo. Photo 536 (I2 on: 514). For the one build I1 the held item's row still stands,
reduced to what moves the item — no value, no Delete (§14) — and the photo is ≈466 while an item is held.

**D · A dimension being measured.** Photo: H1's gold on one figure; its mark shows all its figures. If the gold
came BY ITSELF (after a reading, Next ▸ or `▸`) the mark has NO handles and cannot be dragged. Top bar: the laser
chip gold when the D2 is linked; while it is not, the gold plate's `▶` is hollow. Under the photo: nothing new.
Photo 536 (514).

**E · The keypad open.** A keypad 182 dp high (6 + 4 × 44) stands IN THE COLUMN in place of the tool row, as
today's pad does: the photo's box becomes 182 dp shorter and the whole face is rescaled into it, so nothing is
covered and the gold plate is always in view. It has no display line: the digits appear on the gold plate.
Photo 410 (I1-I3: 388; today 328).

**F · The panel pulled up** (I2). In portrait it opens at half the window (378 dp) when its rows need more, else at
its own height, and pulls up to its full height; **Done is in its head**, so it is on screen at every rest. The
photo above is dimmed and lifted so the picked item stays in view (IM, row 10; `PhotoLift`, §13). Photo 536 laid
out; 214 visible at half (I2-I3: 192). In landscape it opens whole (§6).

**G · After Survey.** Figures are shown and nothing is keyed: no gold, no keypad, no laser chip, no counts, no `▸`.
Wish list, Vendor review, Revisions: the tool row places AS Client wish / Vendor added. Estimation and Client
approved: no tool row at all. Photo 536, or 592 with no row (today 510).

### 2.4 The photo's height against today's

| State, portrait | Today `380c72d` | After I1 | After I2 | Final (I4) |
|---|---|---|---|---|
| Mark, nothing picked | 514 | 514 | 514 | 536 |
| a mark picked, nothing being measured | ≈460 | ≈466 held, 514 not held | 514 | 536 |
| a mark picked and being measured (the usual state at Survey) | ≈404 | ≈466 held, 514 by the walk | 514 | 536 |
| keypad open (in the column, the face rescaled) | 328 | 388 | 388 | 410 |
| panel at half (visible) | none | none | 192 | 214 |
| 3 · Measure, pad shut | 510 | step gone | | |
| Estimation, Client approved | 510 | 570 | 570 | 592 |
| 1 · Lines (Reference on) | ≈403 (≈351) | same | same | ≈425 (≈373) |
| 360 | 514 | 514 | 514 | 536 |
| Plan (drawing box) | ≈576 | same | same | same |

≈466 is 570 less the palette's 56 and the held item's row of ≈48; the 6 dp of dead space F1 put under the row's
Delete goes with that Delete.

**Landscape** (780 × 336 dp window). The photo is ≈302 dp high and ≈436 wide in every state today, because every
row lives in the 318 dp right column. It stays 302 through I3 and becomes ≈324 in I4, in every state. In the
column: top bar 32, tabs 56, strip 24, then 212 dp for the tool row (two rows of 44: `＋` and the four slots; `⋯`
and `▸`). The keypad (6 + 4 × 36 = 150 dp) takes the tool row's place while open. The panel opens WHOLE across the
780 dp sheet, with no grip and its acts on ONE line of 40 dp (§6). Nothing is lifted: nothing covers the photo but
the panel.

---

## 3. Every gesture in 2 · Mark

This section is the final form. I1 brings `Pick` (§3.1), the figure taps, the empty tap, the drag rules, the tool
that places one item and goes off, and Select gone as a slot; the panel's openings and the code chip are I2's (until
then the held item's row opens the sheet and a long press opens today's menu); `＋`, `⋯` and keep-on are I3's; the
double tap is I4's.

Three things can be true of an item, held in one value (`Pick`, §3.1):

- **picked** — one item at most; it shows all its figures;
- **held** — picked by his own hand; it shows handles and it can move. A locked item, and one the phase holds
  read-only, is never held;
- **gold** — one dimension at most, at Survey only, on a face only, always a dimension of the picked item.

The rule that replaces 3 · Measure's "nothing can be dragged" (ours): **only a held item moves, and whenever a
reading lands or the gold moves by itself the handles go.** A mark the walk has reached cannot be dragged by the
thumb of the hand that is not holding the laser. One tap on it gives the handles back.

**With no tool armed**

| Touch | On | Does |
|---|---|---|
| Tap | a figure | It turns gold; its mark is picked (held only if it already was). After Survey, or on a mark not in the room: picks the mark and says the existing refusal |
| Tap | the gold figure | The keypad opens (§5) |
| Tap | an item's body (mark, Tape band, Line, area, chain) | Picked and held. At Survey the gold stays where it is if it is already one of this item's figures, else the item's first unread figure turns gold |
| Tap | the picked item's code chip (I2) | The panel opens (§6) |
| Tap | a handle of the held item | Nothing moves (`SurveyGuide.EndGate`, today a Line's ends; now tabs and corners too). That part becomes the one Fine move nudges |
| Tap | empty photo, an item HELD | Handles, pick and gold all clear; an open keypad stores what is typed and shuts. (Today: nothing, `SPS:3020`) |
| Tap | empty photo, a figure gold and NOTHING held | NOTHING, as 3 · Measure today: "a stray thumb must not lose the dimension the laser is about to fill" (`SPS:3023-3025`). An open keypad stores what is typed and shuts; the gold stays. To let it go: tap the mark (held), then empty photo |
| Tap | empty photo, an item picked, not held, nothing gold | The pick clears |
| Long press | any item | Picked and held; the panel opens when the finger LIFTS (I2). Until I2: today's menu |
| Double tap | empty photo | Zooms 2× about the point and changes nothing else; `fit` returns (ours, slice I4: one thumb cannot pinch) |
| Drag | a handle of the held item (16 dp reach; a Line's end 14 dp) | Resizes, or moves that corner or end; magnifier, guide rays, snap as today |
| Drag | the held item's body | Moves it whole after 8 dp (today from the first pixel), snapping as today. Tape, Line, area: F1's body-move |
| Drag | anything else, an unpicked mark included | Pans. (Today a press on any mark picks and drags it) |
| Pinch | | Zoom ⅓ of fit to 8×, as today |

**With a tool armed**

| Touch | Does |
|---|---|
| Tap | A type: places its default box centred there (a point: its spot), picked and held, first figure gold at Survey. Then the tool goes off (I1), unless it was kept on (I3, §7) |
| Drag | A type: places and carries it. Tape, Line: pulls one out, 5° snap; once it is plotted the tool goes off. All else as today |
| Tap, Line on | A corner of an area; a double tap closes it, as today, and then the tool goes off |
| ✕ on the chip, or the lit slot again | The tool goes off |

**The hit order under a press** is today's (`SPS:3053-3078`, `SurveyPrepTouch.hit`) with one new entry: the held
item's handles, THE PICKED ITEM'S CODE CHIP (I2, `Hit.Code`), a figure, the picked Tape / Line / area's body, then
marks topmost first. A handle beats the chip; the chip beats a figure and a body. What else changes is that a press
on an item that is not held does nothing until the finger lifts.

**The double tap and the tap before it.** `GestureClassifier` reports `Tap` on the first lift and `DoubleTap` on
the second, so the first tap of a double tap has already cleared a held mark at the moment he zooms in to place its
handle. So `Pick` keeps the state the first tap cleared for the double-tap window (450 ms) and puts it back on
`DoubleTap`. A keypad the first tap shut stays shut; what it stored stays stored.

**The 360 step** keeps its own gestures as built. Nothing is ever gold there and `▸` is not drawn (§11), so it needs
no guard.

### 3.1 `Pick`, event by event

The whole rule, in `:pano`, with one test case per row (`PickTest`). "–" = unchanged. "X may be held" = the phase
lets it be edited (`PhaseTools.editable`) and it is not locked; where it may not, every "yes" below is "no".

| # | Event | picked | held | gold |
|---|---|---|---|---|
| 1 | Tap on the body of X, not the picked item | X | yes | at Survey X's first unread figure (`MeasureFlow.firstOpen`); none when it has none, is not in the room, or after Survey |
| 2 | Tap on the body of X, already picked | X | yes | UNCHANGED while it is one of X's figures (`MeasureFlow.body`'s rule, kept: a second tap must not throw away the dimension he chose); else X's first unread |
| 3 | Long press on X | X | yes | as rows 1 and 2. The panel opens when the finger lifts |
| 4 | Tap on a figure F of X that takes a reading | X | kept if X was already held, else no | F |
| 5 | Tap on the gold figure | – | – | –. The keypad opens |
| 6 | Tap on a figure after Survey, or of a mark not in the room | X | kept if X was already held, else no | none. The refusal is said |
| 7 | Tap on the picked item's code chip, or `⋯` | – | – | –. The panel opens |
| 8 | Tap on a handle of the held item | – | – | –. That part is the nudge target; nothing moves |
| 9 | Drag of the held item's body or handle | – | – | –. Its live `≈` figures follow |
| 10 | Tap on empty photo, an item held | none | no | none. An open keypad stores and shuts |
| 11 | Tap on empty photo, a figure gold, nothing held | – | – | –. An open keypad stores and shuts |
| 12 | Tap on empty photo, picked but not held, nothing gold | none | no | none |
| 13 | Double tap on empty photo (I4) | as before its first tap | as before | as before. Zoom 2× |
| 14 | A type placed; a Tape, Line or area plotted | the new item | yes | at Survey its first unread figure (a Tape has none). The tool goes off |
| 15 | `Duplicate` | the copy | yes | the copy's first unread |
| 16 | `Four corners`, `Rectangle`, `Fine move` | X | yes | –. An act that edits shape or position holds the item, so a mark the walk reached shows the handles the act needs |
| 17 | `Lock` | X | no | –. (`Unlock`: picked, not held until it is tapped) |
| 18 | A reading lands on the gold figure and passes the check; or Next ▸ stores a typed figure | the new gold's owner; unchanged when nothing is left | NO | the same item's next unread, else the face's next unread (`MeasureWalk.next`), else none and the wall-finished note |
| 19 | A reading lands and fails the check | – | NO | stays where it is (`1234!`) |
| 20 | A reading lands while the panel or the Add sheet is open, or a finger is down on a grab | – | – | stays; row 18 or 19 applies when the sheet closes or the finger lifts (§4.3, 6) |
| 21 | `▸`, a figure gold | the new gold's owner; unchanged when nothing is left | NO | the face's next unread after it, accepting a `!`; nothing left: cleared, and the wall-finished note |
| 22 | `▸`, nothing gold | the new gold's owner | NO | the picked item's first unread, else the face's first unread (`MeasureWalk.next` from the top) |
| 23 | ↶ | the gold's owner if the gold is kept, else none | NO | cleared if its owner is gone, else kept |
| 24 | The picked item deleted | none | no | cleared if it was that item's |
| 25 | Another face tab, another step, the Plan | none | no | none. NOTHING is gold on entry: he may be about to mark; `▸` or a tap on a figure starts |
| 26 | The open face's own tab, the open step, or a thumb under the strip routed to it | – | – | –. No change at all (today's `Move.STAY`, kept: restarting there dropped the dimension he had chosen) |
| 27 | Anything on 3 · 360, or after Survey | as above | as above | NEVER set. `▸` is not drawn |
| 28 | 2 · Mark asked for on a face whose four lines do not close | – | – | –. Refused in every phase with the existing `refuse.sp.these_four_lines_do_2` (`SPS:1292`); `PhaseTools.prepMode` falls to 1 · Lines, not to a step that no longer exists |

---

## 4. The laser

### 4.1 No control in 2 · Mark fires it

The meter's own button takes the reading — ImageMeter's default too (row 15). Amit asked that the D2 not be fired
by accident ("navigation button and D2 aer very close where d2 get accidentally fired"); F1 moved it apart, and with
the bar gone it has no place under the photo. **Taking it off the Mark step is ours, not ImageMeter's:** ImageMeter
carries an on-screen "Trigger laser remotely" where the meter supports it, its table gives the D2 as "?" (not
established), and our own `g` command fires his D2 today. With one hand on the meter its button is already under a
thumb.

- **I1** deletes survey prep's `fire()` (`SPS:610-615`) and the bar's D2.
- **Until I4** the two D2 buttons beside the reference's W and H in 1 · Lines (`fireRef`, `SPS:1051`, `:1820`) still
  fire; I4 deletes them, and a reference box is then armed by a tap.
- **`DistoClient.measure()` STAYS** (`APP/DistoClient.kt:166`): the Laser test screen (`LaserTestScreen.kt:111`),
  Face prep (`FacePrepScreen.kt:174`) and Annotate (`AnnotateScreen.kt:296`) call it. Deleting it would be a red
  build in a container that cannot compile `:app`.
- Whether the phone should ever fire the meter from survey prep is asked (§15, A5). I1 ships "never".

### 4.2 The link, and the chip that shows it

Today the Bluetooth link starts only when the on-screen D2 is pressed (`SPS:518-527`, `:603-609`) — that is, at the
moment of measuring, with the meter on. With that button gone:

- **The link is started, with the Bluetooth permission already held, whenever a figure TURNS GOLD while the client
  is OFF** (a tap on a figure or a body, a placement, `▸`) **and when survey prep opens at Survey**; never more often
  than once in 30 s (Android throttles scans, and each attempt scans for 20 s, `DistoClient.kt:121-147`). Starting it
  once at open, as this design first had it, would usually fail: at open he is still marking and the meter is off, so
  every room would end in "no DISTO found" after 20 s; and a D2 that switches itself off is a clean disconnect with
  no retry (`DistoClient.kt:234-248`), after which the meter's button would do nothing on the phone, silently. A
  figure turning gold is the moment of measuring now.
- **An attempt nobody asked for says NOTHING when it fails** — no meter found, Bluetooth off, a scan refused. The
  chip alone shows it. Only a tap on the chip speaks (`sp.laser.no_disto_found` and the client's other words), and
  only a tap on the chip asks for the permission.
- **The laser chip** in the top bar: grey = off, pulsing = connecting, gold = ready. A tap on grey connects (asking
  for the permission if needed) and speaks; a tap on gold says `sp.laser.ready`. It never fires. It is in the top
  bar, out of the thumb's reach while measuring, and beside nothing he presses often.
- **At the figure itself:** the gold plate's `▶` is hollow while the chip is not ready (§2.2). That is the sign
  where his eyes are; the chip is the sign that stands.
- A link that WAS ready and drops says its existing line once (`sp.laser.disconnected_x`, or the meter's own
  switch-off line) and the chip turns grey; the next figure that turns gold starts it again.
- After Survey there is no chip and no link.

`LaserChip` (pure, beside `LaserNote`) holds all of it, with tests: OFF and a figure turns gold → start; a second
turn inside 30 s → no start; an attempt nobody asked for that fails → no words; the chip tapped → words.

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
4. **A reading that fails the check does not move on** (ours; today it is stored and the walk moves on). Double
   buzz, the plate reads `1234!`, the note says `sp.measure.photo_says_x`. The next reading replaces it. `▸` accepts
   it and moves on, and a tap on another figure accepts it too. When it was the wall's last unread figure nothing is
   "left" — a reading that fails is stored (`MeasureWalk.open`) — so `▸` is drawn while a figure is gold (§9): it
   then clears the gold and the note says the wall is finished. So a double press of the meter cannot quietly put a
   door's width into its height.
5. **Redoing a reading:** tap the figure, shoot again. ↶ does not undo a reading, as today.
6. **While the panel or the Add sheet is open, or a finger is down on a grab,** a reading is written to the gold
   figure and the gold STAYS, as after a failed check; it moves on when the sheet closes or the finger lifts. So the
   picked item never changes under a sheet that holds a draft for it, and a handle is never taken from under a
   finger. **Fine move does not hold it back:** a reading that lands while its pad is up ends Fine move as ✓ does
   (every nudge is already written) and then moves on as any reading.

### 4.4 A reading with nothing gold

Refused, said, not stored — ImageMeter's rule (row 5) and today's (`SPS:638`): `sp.msg.d2_x_tap_measurement`, "D2
1203 mm — tap a measurement first, then read again". Nothing is held back for a later tap, and nothing lands on a
figure he was not looking at. A tap on any figure, or `▸`, starts measuring.

**A refused reading gives its own haptic** — one long buzz, unlike the landing tick (`SPS:642`, today the only
haptic) and the failed check's double buzz — **and its note stays until the next touch** (every other note goes
after 2.6 s). His eyes are on the wall when he shoots; a reading thrown away must be felt.

Other landings, unchanged: an armed reference box in 1 · Lines takes it as the reference's W or H; after Survey it
is refused in the phase's words; on the 360 it is refused with `sp.laser.on_360_x` (§11).

A locked mark still takes readings: our lock holds its position, not its figures.

---

## 5. Typing a value

- **What opens the keypad:** a tap on the gold figure — so, on a figure that is not yet gold, two taps. Never the
  laser. (What opens ImageMeter's is NC; this is ours.)
- **Where and what:** it stands in the column in place of the tool row, 182 dp, as today's pad stands in place of
  the row and the palette (`SPS:1867-1868`, `:1986`, `:1996`), and it is gone when it is not in use. The photo's box
  is 182 dp shorter while it is up and the whole face is rescaled into it, so the gold plate is never covered. Four
  rows of 44 dp, today's keys (`MeasurePad`): `1 2 3 ⌫` / `4 5 6 Clear` / `7 8 9 Next ▸` / `⌄ 0`. Millimetres only.
- **No display line** (ours). The digits appear on the gold plate as they are typed (`MeasurePad.live`).
- **Next ▸** stores the figure and moves the gold as a reading that passes the check does; the keypad stays up. A
  typed figure is deliberate: one that fails the check keeps its `!`, the note says so, and the gold still moves.
- **Clear** empties the plate. Leaving an emptied plate that held a reading removes the reading, and the estimate
  returns (today's `commitPad`, `SPS:591`).
- **It goes away** by `⌄`, Back, a tap on the photo, or a laser reading. The first three store what was typed.
- In landscape it takes the tool row's place in the right column and covers no photo.
- **Not an overlay in I1.** This design first had the keypad rise OVER the photo with the photo lifted. That shows
  the same 388 dp of photo as the column does (570 − 182) and adds a translated photo whose touches must be mapped
  through the lift, in the slice that already rewrites the gesture loop. The lift is built in I2, where the panel
  needs it; the keypad takes it there only if the rescale proves distracting on the phone.

---

## 6. The panel that slides up

Our Mark sheet (`APP/MarkSheet.kt`, `PANO/MarkSheet.kt`) with the acts that today stand in rows under the photo.
Its FORM is ImageMeter's sheet over a selected element (row 10); what it holds is ours.

- **What opens it:** a tap on the picked item's code chip (§2.2); a long press on any item; `⋯` in the tool row
  (I3). (Today: the label in the picked item's row.) A long press opens it when the finger LIFTS, and **the panel
  takes no tap for its first 300 ms**: the chip and the long press are both pressed over the photo, where the
  half-open panel then stands.
- **How it rests, portrait:** it opens at half the window when its rows need more than that, else at its own
  height, and pulls up to its full height (`ModalBottomSheet` with its half rest on). The photo above is dimmed, and
  lifted so the item stays in view. There is no collapsed strip: at rest the tool row is all there is.
- **How it rests, landscape:** it opens WHOLE (`skipPartiallyExpanded = true` there), with no grip.
- **Why Done is in the head.** The half rest was switched OFF after the K4 review, because it hid Done under the
  fold (`APP/MarkSheet.kt:86-89`; `APP/AlbumSheets.kt:47-50`: "hid the state sheet's foot, the answer sheet's button
  and, at 780 x 360, every sheet below its first rows"). The sheet is a draft, so a chip tapped with Done out of
  sight is lost to the first swipe down. This design switches the half rest back on in portrait only, and moves
  Done to where the fold cannot hide it: the RIGHT END OF THE HEAD, in place of ✕. Back, the dimmed photo and a
  swipe down cancel, as today. The foot's Done row goes.
- **A Tape and a chain get the panel too** (today they have none): head and acts only.

**Rows, top to bottom (portrait)**

| Row | dp | Holds |
|---|---|---|
| Grip | 16 | a small bar to pull by |
| Head | 40 | `FW1 · Window` · the trade's dot and short name · **Done** at the right end |
| Acts, line 1 | 44 | `Lock` / `Unlock` · `Duplicate` · `Fine move` |
| Acts, line 2 | 44 | `Four corners` / `Rectangle` (Q2; only a type that may take four corners) · `Lock all` · 24 dp gap · **Delete** LAST, at the right end (red outline) |
| KIND | 32 | the type's kinds as chips, as today |
| STATE | varies | the now-line, the four acts (↻ Replace ⇢ Shift ⚒ Repair ✕ Remove), the moves with their Why boxes, as today |
| TRADE | 40 | on an Other, a Note, a Line, an area, as today |
| NOTE | ≈96 | two lines and who reads it, as today |

At the half rest (378 dp) the grip, the head with Done and both lines of acts are above the fold (16 + 40 + 88 =
144 dp), with KIND and the top of STATE under them.

- **The type is shown, never changed here:** changing it would rename the code and every reading key.
- **Why Delete is where it is.** It is the LAST act, apart, by F1's own rule: "the menu opens with its head under
  the finger, so its first row is the one a hurried second tap lands on" (`PANO/SurveyPrepEdit.kt:361-366`,
  `DeleteGuard.last`, kept and re-used by `MarkSheet.acts`). A long press and a tap on the code are made ON THE
  PHOTO, and the half-open panel covers the photo's lower 322 of 514 dp: its first act row stands 248-292 dp down
  the photo, about switch-plate height on a wall at fit — under the finger that opened it. That row holds nothing
  that loses anything. Delete is a whole act row (44 dp) below Done; what stands between them, `Fine move`, loses
  nothing. Deleting closes the panel and shows "FW1 deleted · UNDO" for 5 s (`sp.msg.x_deleted`); ↶ restores it
  too. **Delete has this ONE place:** the picked item's row loses it in I1, and the floating trash and the
  long-press menu go in I2.
- **A mark past Existing has NO delete cell.** Drop and Restore are the STATE section's moves, in the one place they
  already are: a Drop needs its why, typed there, and its Done takes the one step (`SPS:962-984`); Restore is the
  Dropped mark's own move there, and at a phase gate the section draws the gate's line, as today. A panel opened
  for such a mark by a long press opens with STATE in view.
- **The sheet stays a draft** for kind, trade, state and note: Done writes them in one undo step; Back, a swipe
  down and a tap on the dimmed photo drop them, as today. **An act is Done followed by the act** — what was typed is
  kept, and Done's step and the act's step are ONE ↶ step — except Delete, which drops the draft and deletes. Each
  act closes the panel.
- **What the phase forbids is not drawn** (`SurveyPrepEdit.menuItemsOf`'s rules, which become the acts' rules): no
  Delete, Duplicate, Lock or shape on a mark of an earlier phase; on a locked mark Delete is drawn and answers the
  lock's words.
- **Fine move** closes the panel, holds the item, and puts a small nudge pad over the photo, in the bottom corner
  farthest from the item: ▲ ◀ ▶ ▼ round a centre cell that switches the step between 1 and 5 mm, and ✓. 3 × 3 cells
  of 40 dp. It nudges the part last touched (the whole item, an edge, a corner, an end), drawn solid. **While the
  pad is up, a tap on another handle, corner or end of the same item makes THAT the part nudged**; only ✓, a tap on
  empty photo, or a reading that lands (§4.3, 6) ends it. So four corners are four taps, not four trips through the
  panel. It never snaps. It is offered only for an item the phase and the lock let move. It is there only while he
  is using it; it replaces the standing row of nudges.
- **In landscape** the acts are ONE line of 40 dp across the sheet, Delete last. That line ends 16 dp short of
  Done's left edge, so nothing — Delete least of all — stands under Done. The foot's Done (48 dp, at the foot of
  the left column) goes and the act line (40 dp) comes, so the left column is 8 dp shorter than today's and the
  STATE column 40 dp taller; where the blocks then need more than the window leaves they scroll under the head and
  the acts, as they do today with the keyboard up.

---

## 7. The tools

- **A tool places ONE item and switches itself off** (ours; today it stays on) — **from I1**, not I3, because from
  I1 he measures on the screen where he places. With a type left on, a tap on empty photo — I1's "the pick clears"
  — would place another Door (`tapAt` → `placeGrab`, `SPS:3020-3022`); with Tape or Line left on, nothing under the
  finger is picked (`SPS:2986-2988`), so a tap on a figure would add an area corner and no figure could be measured
  until he found the lit slot. Today that cost nothing, because measuring lived in 3 · Measure, where no tool acts.
  After a mark is placed, or a Tape, a Line or an area is plotted, the tool is off (`SurveyPrepPalette.afterPlaced`,
  one pure line, tested).
- **Select is no longer a tool** (I1): with no tool armed, a touch selects. Its slot goes; `▸` is the row's last.
- **Three pinned types and the last one used** sit in the tool row (I3; ours, on IM's row 9):
  - wall: Door · Window · Switch plate, then last-used, seeded Electric point;
  - floor: Column · Step · Electric point, seeded Carpentry unit;
  - ceiling: Beam · Electric point · Bulb point, seeded Fan.
  The pinned three never move. A type chosen in the Add sheet that is not pinned takes the fourth slot.
- **Door, Window and Switch plate are always ONE tap** and then the photo; the electric point while it holds the
  last-used slot. Every other type and kind is two: `＋`, then its tile.
- **`＋` opens the Add sheet** (I3; IM, row 8), headed groups, each tile an icon with its name:
  1. **Most used here** — the surface's three pinned types and its seed;
  2. **Measure** — Tape, Line (the Chain is off, `Features.CHAINS`);
  3. **By trade** — today's More ▸ rows as built (`SurveyPrepPalette.moreRows`), General first; it scrolls.
- **A long press on a slot keeps it on** (I3; six sockets on one wall): the chip says `sp.tool.stays_on_x` until ✕.
  Until I3 six sockets are six taps on the slot.
- **The kind** is on the tool chip before placing ("Wardrobe ▾") and in the panel after; remembered per type and
  surface, as today.
- After Survey the row places AS Client wish or Vendor added; at Estimation and Client approved it is not drawn.

---

## 8. Moving and reshaping

### 8.1 Marks, Tapes, Lines, areas

- Only a HELD item moves (§3). Its body moves it whole after 8 dp; a handle resizes it or moves that corner or end.
- A Tape, a Line and an area move by the body (F1, `SPS:2760`, `SurveyGuide.kt:340`), and by an end with its 4 dp
  dead zone, guide and magnifier, as built.
- The magnifier (120 dp, 3×), the guide rays and the snap are today's. The escape from a snap is to zoom in — by a
  double tap, which keeps the item held (§3) — and the last millimetres go by Fine move, which never snaps.
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
| §3.1 the long-press menu gains the row `Four corners` / `Rectangle` | the panel's Acts line 2 holds it. I2 carries the row and its off-switch (`menuItemsOf(…, shape = false)`, already on `port63-quad`) into `MarkSheet.acts`; Q2 turns the switch on |
| §3.2 "the row then reads `FW1` over 'corner' and shows the ◀ ▶ ▼ ▲ nudges" — the documented way past the corner snap ("The last millimetres go by the nudges, which never snap") | Fine move's pad nudges the corner last touched, and while it is up a tap on another corner makes that the one nudged (§6): one trip through the panel for all four |
| §3.4 "the floating trash keep[s] the bounding box" | there is no floating trash. The code chip keeps the bounding box's top-left, 16 dp clear of every corner handle |
| §3.5 "What 3 · Measure draws" | what 2 · Mark draws; sides and the diagonal count as size, so every four-corner mark shows them |
| §3.5 "The bar says the side's words" | dropped. Each side's plate sits on its own side, and the gold plate is on the side being measured |

### 8.3 The four-corner mark on this screen

Pick the window; open the panel; `Four corners`. The panel closes, the mark is held and shows four round corners,
and the note says `sp.shape.drag_corners`. Each corner is dragged by itself under the magnifier. Each side shows its
own figure on that side, the diagonal inside if asked. The walk inside the mark is bottom, right, top, left, the
diagonal, then the type's own, as `DESIGN-quad.md` §3.5 orders it. `Rectangle` in the panel puts it back.

---

## 9. What is left to measure, without a bar

- **The face tabs' amber counts** — the room at a glance, and the ONE visible count. They now show in the Mark
  step, from the first mark placed.
- **`▸` at the end of the tool row** — the way to the next unread figure by hand. No number on it. A wrong tap only
  moves the highlight. Drawn at Survey while the open face has a figure left OR a figure is gold; so a failed
  reading on a wall's last figure can still be accepted (§4.3, 4). Not drawn after Survey, nor on the 360.
- **`✓` after a mark's code** once all its figures are read.
- **The note** when a wall, and then the room, is finished (§4.3).
- No `‹`: to go back to a figure, tap it. No "7 of 18 measured" and no next-face button.

---

## 10. Phases, locks, wishes

- **2 · Mark is in every phase.** After Survey it is the view: figures show, nothing turns gold, no keypad, no
  laser chip, no counts, no `▸`. A tap on a figure picks its mark and says the phase's existing refusal
  (`PhaseTools.readingRefusal`). Today those phases are sent to 3 · Measure (`PhaseTools.prepMode`).
- **A face whose four lines do not close** cannot open 2 · Mark in any phase: the existing
  `refuse.sp.these_four_lines_do_2`, and `prepMode` falls to 1 · Lines (§3.1, row 28).
- **Placing:** as Client wish in the Wish list, as Vendor added in Vendor review and Revisions (`PhaseTools.place`),
  unchanged. No tool row at Estimation and Client approved.
- **Editing:** `PhaseTools.editable` decides who can be held. A mark that cannot be held shows no handles, and the
  panel does not draw the acts the phase forbids.
- **A mark past Existing is never deleted:** Drop and Restore are the panel's STATE moves and nowhere else (§6).
- **A mark that is not in the room** (born a wish or Vendor added) shows estimate-style figures, never turns gold
  and is not counted. A tap on its figure picks it and says `sp.measure.not_in_room`.
- **Locked marks** take readings and do not move.

---

## 11. The 360 and the Plan

- **3 · 360** (today's 4 · 360): the same tool row and the same panel (opened by a long press, and `⋯` from I3). It
  draws no figures, so nothing is measured there: nothing is ever gold, `▸` is not drawn, and a reading is refused
  with `sp.laser.on_360_x`. This is a loss against today, where the bar let a figure be lasered from the 360. Its
  gestures stay as built.
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
| `sp.measure.aria.keep` | Keep this reading · nothing else left on this wall | I1 |
| `sp.reference_shown_mark` | Reference shown on 2 · Mark and 3 · 360 | I1 |
| `sp.sheet.aria.open_x` | Options for {code} (the code chip, and `⋯`) | I2 |
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
client already says (`sp.laser.no_disto_found` among them, now only after a tap on the chip),
`refuse.sp.these_four_lines_do_2`, `sp.measure.face_done_x`, `.room_done_x`, `.photo_says_x`, `.not_in_room`,
`.x_to_measure`, `.aria.type` (on the gold plate), `sp.msg.x_deleted`, `common.lock`, `common.duplicate`,
`common.delete`, `common.lock_all`, `common.done`, `sp.corner`, and the checklist's own names for a `⊥` figure.

**Deleted:** `sp.measure.all`, `sp.measure.aria.all`, `sp.measure.aria.previous`, `sp.measure.aria.skip` (replaced
by `.aria.next_x`), `sp.measure.x_of_x`, `sp.measure.next_face_x`, `sp.reference_shown_measure`,
`refuse.sp.tap_measurement_first` (its only reader is `fire()`, `SPS:613`; I1); and `common.measure`,
`common.more`, `common.select`, the row's `sp.tape_x_mm` and the sheet's ✕ description where nothing else reads
them.

---

## 13. What goes, what stays

Prefer deleting to hiding: none of this holds a user's data.

**Deleted**

| What | Where | Slice |
|---|---|---|
| The measure bar: `SpMeasureBar`, `MbArrow` | `APP/SurveyMeasure.kt:72-200` | I1 |
| Its layout model and test | `PANO/MeasureBarLayout.kt`, `TEST/MeasureBarLayoutTest.kt` (6) | I1 |
| The step 3 · Measure: `StepRoute.MEASURE`, `measureBarDp`; `measurePanel` (`SPS:2006-2019`); `barStep` (`SPS:1184`); the Measure pane's tap rule (`SPS:2993-2996, 3026-3032`) — its "empty photo does nothing" is KEPT, in `Pick` (§3.1, row 11) | `PANO/StepRoute.kt`, `SPS` | I1 |
| `MeasureFlow.Bar`, `Kind`, `onFace`, `onItem`, `move` (WALK / STAY / DROP) — `STAY` is KEPT, in `Pick` (row 26); `body`'s rule is KEPT (row 2) | `PANO/MeasureFlow.kt`; `TEST/OnFaceTest.kt` (4) and those cases of `MeasureFlowTest` | I1 |
| `MeasureText`'s progress and next-face texts | `PANO/MeasureText.kt`; cases of `MeasureTextTest` | I1 |
| `all`, and `DimLayout.Scope` with it (the Mark step always shows what `Scope.FACE` shows). The hand-anchored `w` / `h` plates and the eight blocked tab boxes are KEPT, re-keyed from `Scope.PICKED` to `mk.tabs`: the HELD box mark that shows its tabs | `SPS:3449-3473`, `PANO/DimLayout.kt:114-121`, `:476` | I1 |
| Survey prep's on-screen D2 and `fire()`. `DistoClient.measure()` stays (§4.1) | `SPS:610-615` | I1 |
| From the held item's row: its Delete control with `SpDeleteGap`, and every value it prints — `SurveyMeas.rowValue`, `SurveyChain.rowValue` (the row is their only reader) | `SPS:1906-1913, 1931, 1936-1941, 1955, 1960-1965`; `PANO/SurveyMeas.kt:287`, `PANO/SurveyChain.kt:216`; their cases in `MeasTest`, `ChainTest`, `FeaturesTest` | I1 |
| The row's and the bar's delete clearances: `DeleteGuard.rowGapDp`, `rowClearDp`, `rowToBarDp`, `clearOfD2Dp`, `rowToPaletteDp`, `clearOfPaletteDp` | `PANO/DeleteGuard.kt`; those cases of `TEST/DeleteGuardTest.kt` | I1 |
| Select as a slot, and the tool that stays on | `SurveyPrepPalette.SlotKind.SELECT`, `slots`, `next`'s "sticky"; `drawSelectIcon` (`APP/SurveyPrepTools.kt`) | I1 |
| The rest of the held item's rows (label, nudges, padlock) | `SPS:1869-1966` | I2 |
| The long-press menu `SpCtxMenu` and `ctxPick`; `DeleteGuard.menuTop`, `menuClear` | `APP/SurveyPrepLocks.kt:123`, `SPS:1107` | I2 |
| The floating trash: `trashSpot`, `keepOfMark`, `keepOfLine` | `PANO/SurveyPrepTouch.kt:175-300`; `TEST/TrashSpotTest.kt` (12) | I2 |
| The palette row, More ▸ as a list | `SpPalette`, `SpMoreSheet` (`APP/SurveyPrepTools.kt`); `SurveyPrepPalette.slots`, `MAIN` | I3 |
| The caption row | `SPS:1580-1594` | I4 |
| The two D2 buttons in 1 · Lines (`fireRef`'s firing; the box is armed by a tap) | `SPS:1820, 1051` | I4 |

`DeleteGuard.last`, `CLEAR_DP`, `OUTLINE` and `INK` STAY: the panel's Delete is the last act, apart, in the delete
control's own look.

**Kept behind a switch:** only what already is — `Features.CHAINS` (off). A room that has a chain keeps it: drawn,
picked, its segments gold in turn and counted, deleted from the panel.

**New pure rules in `:pano`** (this container cannot compile `:app`, so every rule the screen follows is a pure
function with its own test):

- `Pick` (`PickTest`, I1): picked / held / gold as one value and one function per event; §3.1's table is its
  specification, one test case per row. It lives here and nowhere in `:app`.
- `SurveyPrepPalette.afterPlaced` (I1): a tool that has placed its item is off, unless kept (never, until I3).
- The landing in `SurveyRef`'s route gains "a reading that fails the check stays", "a reading with a sheet open or
  a finger on a grab is written and the gold waits" and the refused reading's buzz (§4.3, §4.4; I1).
- `LaserChip` (beside `LaserNote`, I1): the client's state → off / connecting / ready; when a start is made (OFF and
  a figure turns gold, or the room opens; once in 30 s); when a failure speaks (only after a tap on the chip).
- `DimLayoutTest` gains (I1): no DRAWN plate of the held mark overlaps one of its tab squares. `SurveyPrepTouchTest`
  gains: a tap at the centre of a held mark's width plate, and of its height plate, is `Hit.Tag` — so the gold
  width plate of a mark just placed always opens the keypad.
- `SurveyPrepTouch.codeChip` and `Hit.Code` (I2): where the picked item's chip stands and what it beats — the chip
  beats a figure and a body; a handle beats the chip; a switch plate's chip is 16 dp clear of its tabs.
- `MarkSheet.acts` (from `SurveyPrepEdit.menuItemsOf`, `:312-365`, through `DeleteGuard.last`), Tape and chain as
  sheet items, Done in the head, and the fold: at the half rest the head and both lines of acts are above it (I2).
- `PhotoLift` (`PhotoLiftTest`, I2): how far the photo is lifted for a panel of a given height.
- `NudgePad` (`NudgePadTest`, I2): which corner the pad stands in, what a press moves, a tap on another handle
  changing the part.
- `ToolRow` (`ToolRowTest`, I3): the pinned three, the last-used slot, kept-on.

**Tests that are rewritten, not deleted:** `MeasureScreenTest` (15; it reads the two screen sources by name, and
gains: no figure text under the photo in any state, no delete control in a row under the photo, the trash drawn and
pressed only for a held item), `StepRouteTest` (`:143-147`, `:186-211`), `PhaseToolsTest` (`toolOf`, `prepSteps`,
`prepMode`), `MarkSheetTest` (the bar's label cell; then the head's Done), `BodyMoveTest` ("this step moves things"
becomes "this item is held"), `PaletteTest`, `DeleteGuardTest` (what stays), `MeasureFlowTest`, `MeasureTextTest`;
`i18n/literal-counts.tsv` and `mr-pending.tsv`.

**Stays as it is:** `MeasurePad` and `SpMeasurePad` (in the column), `MeasureWalk`, `MarkDims` (H1's constants
included), `DimLayout`'s placing, `SurveyPlan.figure`, `GestureClassifier`, `SurveyGuide`, `SurveyRef`'s routing,
`DistoClient` whole (its link and its `measure()`), the Mark sheet's own blocks and its draft, the phase band, the
face tabs, the ⋮ menu, the Plan, the 360's gestures, 1 · Lines' panel, the record and every reading key.

---

## 14. The slices

Each is one Play build, reviewed before it ships. Sizes: S, M, L.

**F1 · the chain off, a Tape moved by its body** (built and reviewed: `136b4c7`, `380c72d` on `batch-next`).
It ships first, as it stands. What survives this design: `Features.CHAINS`, the body-move of a Tape, Line and area,
and `DeleteGuard.last`. What I1 and I2 then delete: its rearranged bar and its row and menu clearances. Its build is
on the phone for the day or two before I1.

**I1 · one step, and nothing about measuring under the photo (L).**
The steps are Lines, Mark, 360. The measure bar, the figure box, `all` and survey prep's on-screen D2 are deleted.
In 2 · Mark every mark shows its size; a tap on a figure makes it gold; a reading lands there and the gold walks on
along the wall; a failed check stays; a refused reading buzzes. A tap on the gold figure brings the keypad up IN THE
COLUMN, in place of the tool row as today's pad does (photo 388 dp, the whole face rescaled, nothing covered); it
has no display line — the digits are on the gold plate. `Pick` (§3.1) holds picked / held / gold: only a held item
moves, and an empty tap never takes the gold from under the laser. A tool places ONE item and goes off; the Select
slot goes; today's `›` becomes `▸`, the palette row's seventh and last slot, with no number on it. The laser chip
is in the top bar, and the link starts when a figure turns gold. The tabs' counts show in Mark.
Still for this one build: the long-press menu; the floating trash, drawn and pressed ONLY while the item is held;
and the held item's row REDUCED to what moves it — a mark's label (which still opens its sheet), its nudges and its
padlock; a Tape's, Line's, area's and chain's code and padlock. The row prints no value and has no Delete (the trash
and the menu delete for this build, both with UNDO), so nothing under the photo shows a measurement and nothing
destructive stands over `▸`. One jump is left in the photo's height, ≈466 ↔ 514 dp as the handles come and go; I2
ends it.
*Amit sees:* one step; no box and no figure under the photo; the photo keeps its height while he lasers along a wall
(514 dp where it was ≈404); nothing in 2 · Mark can fire the D2 (the reference's two D2 buttons in 1 · Lines stay
until I4); no second door under a stray tap.

**I2 · the panel slides up; the standing rows go (M).**
`port63-quad` merges to `batch-next` BEFORE I2 starts, as its own step with its own green run: it forks at
`7c49835`, before H1 and F1, and both sides changed `SurveyPrepEdit.kt`, `SurveyPrepTouch.kt`, `SurveyGuide.kt`,
`Pano360.kt`, `MarkDims.kt`, `i18n/literal-counts.tsv` and three tests (`LockTest`, `MarkDimsTest`, `Pano360Test`).
I2 then carries its shape row and its off-switch from `menuItemsOf` into `MarkSheet.acts`.
The Mark sheet opens half way in portrait and pulls up, whole in landscape; Done in its head; its two lines of
acts, Delete last; no delete cell past Existing; Tape and chain get it. The picked item's code becomes a chip, and
a tap on it and a long press (on the lift) open the panel (`⋯` joins the row in I3); the panel takes no tap for its
first 300 ms. Fine move's pad, re-aimed by a tap on another handle. The photo lifts under the panel (`PhotoLift`;
the keypad stays in the column unless the rescale proves distracting on the phone). The held item's row, the menu
and the floating trash are deleted.
*Amit sees:* picking a mark changes nothing under the photo (514 dp in every state); Delete in one place, behind a
deliberate tap, last in its row, one UNDO away.

**Q2 · the four-corner screen (M, `DESIGN-quad.md` §6 with §8.2 above).**
After I2, because its `Four corners` / `Rectangle` control and its corner nudges live in the panel: Q2 turns on the
switch I2 carried over. If Amit wants it before I2 it can ship on the long-press menu as `DESIGN-quad.md` wrote it,
and I2 then moves the row; Claude recommends after.
*Amit sees:* a sloping window marked corner by corner, each side with its own figure.

**I3 · the tool row and the Add sheet (M).**
`＋`, three pinned types, the last-used slot, `⋯`, a long press to keep a tool on, the Add sheet in place of
More ▸.
*Amit sees:* Door, Window, Switch plate one tap away; every other type under `＋`; six sockets without six arms.

**I4 · less on the photo and above it (S-M).**
The sizes of marks that show no handles sit on their edges with no lines; the caption folds into the step strip
(the photo gains 22 dp); a double tap zooms and keeps what is held; the reference's W and H boxes in 1 · Lines are
armed by a tap and their two D2 buttons are deleted.
*Amit sees:* a calmer busy wall; a taller photo; no control in survey prep that fires the laser.

Not in these slices, and not changed by them: the saved gallery pictures ("Measured figures only") and saving a room
from one surface — port 63's own later slices. I4's on-the-edge drawing is the screen's; whether the saved pictures
take it is decided in that slice.

---

## 15. Open questions for Amit

Six, named A1-A6 so that "Q2" means only the four-corner slice. Each goes to him as a selector when its slice comes
up, never more than four at once: A6 now; A4 and A5 with I1, which is built with their first options; A1 before I2;
A2 before I3; A3 before I4. A1-A4 are each answerable from one static picture of the screen; A6 only on the phone.

**A1 · S2 APK — the panel that slides up (before I2).** Which is the "slide up screen" Amit means?
1. **(Recommended) The picked mark's options**, opened half way: Lock · Duplicate · Fine move · Four corners · Lock
   all · Delete, then kind, state, note. The FORM is ImageMeter's sheet over a selected element, the mark still in
   view; putting Delete, Lock and Duplicate in it is OURS (ImageMeter keeps them in its toolbar) — chosen so that
   Delete is never under the thumb.
2. A low strip of the quick acts only, about a fifth of the screen, pulled up for kind, state and note.
3. He means the sheet "+" opens — the tools — and the mark's options can stay a full sheet as today.

**A2 · S2 APK — the one row under the photo (before I3).**
1. **(Recommended) `＋` · Door · Window · Switch plate · last-used · `⋯` · `▸`**: his common types one tap away.
2. ImageMeter's own: `＋` · last-used · `⋯` · `▸`. Emptier; a door is two taps.
3. No row at all: `＋` and `⋯` float on the photo's corners. Tallest photo; they cover a corner of it.

**A3 · S2 APK — a busy wall, "every mark's size, always" (before I4).** Claude draws his busiest wall (about twenty
figures) both ways.
1. **(Recommended) Sizes on the edges, no lines; a switch plate's size is one small plate beside it; a point shows
   no size.**
2. The same, but a mark narrower than 40 dp on screen shows its size only when picked or zoomed in.

**A4 · S2 APK — typing a figure (I1 ships option 1).**
1. **(Recommended) The keypad alone; the digits appear on the gold plate on the photo.**
2. The keypad with one line above it naming the figure ("FW1 · width  1198"). Easier to read while typing; it is a
   line about a measurement under the photo, there only while the keys are up.

**A5 · S2 APK — firing the D2 from the phone (I1 ships option 1).**
1. **(Recommended) Never: the meter's own button only.**
2. A long press on the laser chip in the top bar fires it — out of the thumb's reach, never a slip.

**A6 · S2 APK — on 0.3.339, measuring a mark (now, before I1 is built on it).** At arm's length, can Amit see which
figure takes the next shot?
1. Yes.
2. No — Claude makes the gold frame heavier and pulses the plate before I1.

---

## 16. Risks

1. **Crowding at 360 dp.** Every mark's size always, plus the held mark's handles, its code chip and its position
   plates, on a 348 dp photo. A switch plate is about 25 × 11 dp, under its own 32 dp tap box and under its own 44 ×
   28 dp chip, so a tab, a plate, a chip and a body compete. A3's picture comes before I4; the gold figure is always
   placed first and always whole.
2. **A reading in a figure he was not looking at.** The gold moves by itself, so he shoots without looking. The
   check passes anything within 150 mm or 15 % of the photo, so a near-square window can take its width as its
   height unseen. Answers: the order never varies, a failed check stays, the plate that took the reading rings gold
   for a second, the gold must read from arm's length (A6). ↶ still does not undo a reading.
3. **A slip while lasering.** The read-only step is gone, and with it the guard the M2 review wrote on purpose: "a
   thumb resting on the photo while the other hand holds the laser must change nothing" (`SPS:2993-2995`) and "empty
   photo does nothing" (`SPS:3023-3025`). `Pick` keeps both halves — nothing but a held item drags, a landed reading
   drops the hold (rows 18, 19), and an empty tap leaves a gold figure alone unless he is holding the item (rows 10,
   11) — and it lives in one tested function for that reason. If it leaks, estimates drift under his thumb or
   readings are refused with his eyes on the wall.
4. **A silent laser, and a scanning phone.** With no D2 button, a failed link looks like a meter not yet pressed.
   The design's answers are in §4.2: the link is tried when a figure turns gold, the gold plate's `▶` is hollow
   while it is down, a refused reading buzzes. What is new to watch on the phone: while he marks with the meter off,
   each placement may start a 20 s scan (once in 30 s at most), which costs battery and says nothing.
5. **No remote trigger on the Mark step.** Pressing the meter's own button can nudge its aim. It is ours, not
   ImageMeter's, and he asked only that it not be hit by accident — so it is asked (A5), and option 2 brings it back
   out of reach.
6. **The lift** (I2, the panel only). The photo is moved, not rescaled, under the panel. Touches must map through
   the lift. The keypad does not use it unless the phone says it should.
7. **The half-open panel.** It rests at half the window by the stock sheet's own rule, not at a height we choose.
   That rest was switched off after the K4 review for hiding Done; this design switches it on in portrait only, with
   Done moved to the head where the fold cannot hide it. If A1's answer is the low strip, the sheet becomes our own
   drag code: more to build and to test on the phone.
8. **Measuring from the 360 is lost** (§11). If he used it, a row in the panel that opens the mark's own wall is the
   repair.
9. **The phase table.** `PhaseTools` copies the approved prototype's table, whose third step is the list. Keeping
   2 · Mark in every phase as the view departs from that row; S5 reads none of it.
10. **`:app` cannot be compiled here.** I1 rewrites the centre of a 3,669-line screen. Every rule goes into `:pano`
    first; CI on `batch-next` is the only compiler, and each slice merges only on a green run of every workflow.
    I1 carries no overlay and no lift for that reason.
11. **Volume of test change.** More than fifty tests are deleted or rewritten in I1 alone (§13). A rule that
    survives must be re-anchored, not dropped with its old test — the M2 guard, `Move.STAY` and `MeasureFlow.body`
    are named in §13 so that they are.
12. **F1 and this design overlap.** F1's bar work lives for one build. That is the price of shipping what is built
    and reviewed instead of holding the chain and the Tape fix behind a larger change.
13. **Landscape** gains nothing in photo height until I4. Accepted on the assumption that he surveys in portrait;
    nobody has asked him.
14. **The tours** (off) keep anchors on a step that no longer exists. They are rewritten when Amit lifts the hold.
15. **One tool, one item, with no keep-on until I3.** Six sockets on a wall are six taps on the slot for two
    builds. If it grates on the phone, the long press that keeps a tool on is pulled forward from I3.
16. **No ✕ on the panel.** Cancel is Back, a swipe down or a tap on the dimmed photo; the grip is the only sign of
    it. If he looks for a way out and finds only Done, the head gains a quiet ✕ at its LEFT end.

---

## 17. The critic

The plan critic read the first version of this file (`744925b`) and returned nineteen findings. Each was checked
against `380c72d` and the ImageMeter study before it was applied. **All nineteen are confirmed and applied.** Nothing
was declined whole; five details were changed in applying, and are listed under the table.

| # | Finding | Confirmed by | Applied in |
|---|---|---|---|
| C1 | I1 still showed a measurement in a row under the photo, and put `▸` under that row's Delete (blocker) | The row prints `SurveyMeas.rowValue` and `SurveyChain.rowValue` (`SPS:1931`, `:1955`); its Delete is its last control (`SPS:1906-1913`); the trash is taken for the picked item, held or not (`SPS:2946`) | §0, §2.1, §2.3 C, §2.4, §13, §14 I1 |
| C2 | I1 left tools sticky and Select a slot on the screen where he measures | `tapAt` → `placeGrab` (`SPS:3020-3022`); with Tape or Line on nothing under the finger is picked (`SPS:2986-2988`) | §2.1, §3, §7, §13, §14 I1 and I3 |
| C3 | The on-screen fire removed though he asked only that it not be hit by accident; ImageMeter cited one-sidedly; the shoot command has three other callers | His (b), once; the verifier's "Trigger laser remotely"; `LaserTestScreen.kt:111`, `FacePrepScreen.kt:174`, `AnnotateScreen.kt:296`; `fireRef` (`SPS:1051`, `:1820`) | §0, §1.1 row 15, §1.2, §4.1, §13, §14 I1 and I4, A5 |
| C4 | A link started once at open will usually fail, and nothing reconnects it | 20 s scan then `noDistoFound` (`DistoClient.kt:144-147`); a clean disconnect is OFF with no retry (`:234-248`); today the D2 press starts it (`SPS:603-609`) | §1.1 row 16, §1.3, §2.2, §4.2, §13, risk 4 |
| C5 | With `DimLayout.Scope` deleted the held mark's size plates land on its own tabs | `val hand = scope == Scope.PICKED && mk.picked && mk.tabs` (`DimLayout.kt:476`); `tabOrTag` gives a tie to the tab (`SurveyPrepTouch.kt:90`) | §2.2, §13 |
| C6 | Delete was the first cell of the panel's first act row, under the finger that opens it | F1's own rule, `SurveyPrepEdit.kt:361-366` and `DeleteGuard.last` | §1.1 row 12, §6, §13, A1 |
| C7 | The half-open panel hides Done while the sheet is a draft | `MarkSheet.kt:86-89`, `AlbumSheets.kt:47-50` (the K4 review) | §2.3 F, §2.4, §6, risk 7 |
| C8 | "Tap on the picked item's code" had no place in the hit order | The code is drawn inside the box at its top-left (`SurveyPrepDraw.kt:571-572`), in the `lt` tab's reach | §2.2, §3, §13 |
| C9 | An empty tap cleared the gold: the M2 review's guard dropped, and a refused reading silent | `SPS:2993-2995`, `:3023-3025`; the only haptic is the landing tick (`SPS:642`) | §3, §3.1 rows 10-12, §4.4, risk 3 |
| C10 | `Pick` described but not specified: nine events without an outcome | `MeasureFlow.body`, `move`; `showTab` and `setMode` (`SPS:1272-1310`); `prepMode` → MEASURE (`PhaseTools.kt:203-208`) | §3.1 (28 rows), §10, §11 |
| C11 | A failed check on a wall's last figure left no way to accept it | A failed reading is stored, so nothing is "left" (`MeasureWalk.kt:48-49`) | §4.3 item 4, §9, `sp.measure.aria.keep` |
| C12 | `9 ▸` was a number about measurements under the photo, repeating the tab's count | §2.1's own sentence beside it | §2.1, §9, A2 |
| C13 | Double tap to zoom first clears the pick it was meant to help | `GestureClassifier` reports `Tap`, then `DoubleTap` | §1.2, §3, §3.1 row 13, §8.1 |
| C14 | `Drop` / `Restore` as a quick act contradicts how Drop works | `DROP_SHEET` opens the sheet (`SPS:984`); Drop and Restore are both STATE moves (`MarkLife.kt:278-298`) | §6, §10 |
| C15 | No rule for a reading that arrives with a sheet open or a finger dragging | §4.3 covered the keypad only | §3.1 row 20, §4.3 item 6 |
| C16 | Four-corner nudging cost three steps a corner; the merge order of `port63-quad` not fixed | `DESIGN-quad.md` §3.2; `SHAPE_FREE` / `SHAPE_BOX` already on `port63-quad` (`SurveyPrepEdit.kt:293-380` there) | §6, §8.2, §14 I2 and Q2 |
| C17 | Two things settled silently: whether H1's gold can be seen, and 1 · Lines' W and H boxes | His (a) and (c); `SPS:1812-1824` | §0, A6 |
| C18 | "Q2" meant two things; three overclaims | §15 against §14 and §16; the verifier on IM's toolbar; `refuse.sp.tap_measurement_first` read only by `fire()` (`SPS:613`) | §1.1 rows 1 and 12, §7, §12, §15 (A1-A6), §16 |
| C19 | I1 carried a lift it does not need: the keypad in the column shows the same 388 dp | 570 − 182 = 388; today's pad already stands in the column (`SPS:1867-1868`, `:1986`, `:1996`) | §2.3 E, §2.4, §5, §13, §14 I1 and I2, risks 6 and 10 |

**Changed in applying, each with its reason**

1. **C5's first test, as worded, is refuted by the code it is meant to keep.** "No plate's tap box of the held mark
   within 16 dp of any of its tabs" fails on today's hand-anchored plates: the width plate's drawn box ends 10.6 dp
   above the top edge and its tap box 4.1 dp above the `t` tab (`SurveyPrepTouch.clearance` is 6 dp; `tapOf` grows a
   19 dp plate to 32), and the height plate starts 10.4 dp right of the `r` tab. What keeps them apart today is
   `tabOrTag`, the nearer of the tab and the plate's centre. So what is pinned instead (§13) is what holds: no DRAWN
   plate overlaps a tab square, and a tap at the centre of the held mark's width plate and of its height plate is
   `Hit.Tag` (the finding's own second test, with the height added). One precision on the finding itself: in I1 a
   tap on the upper part of a mid-span plate would still have reached the figure (the plate's centre stands 6 units
   off the tab), the lower part going to the tab; from I4 the two share one point, and the tab wins the tie.
2. **C2: `▸` is the row's LAST slot, not the slot Select leaves.** The row is seven slots either way. Last is where
   I3 keeps it, so it does not move between builds, and with C1 applied nothing destructive stands over it.
3. **C6 and C7 together put Delete under Done in landscape** (Done at the head's right end, Delete last on the one
   act line directly below). So in landscape the act line ends 16 dp short of Done's left edge (§6), and there is
   no grip: the sheet opens whole, so there is nothing to pull.
4. **C15: "Fine move does not hold it back" needed an end.** A landed reading drops the hold (§3.1, row 18), and
   Fine move holds its item (C16), so a reading that lands while the pad is up ends Fine move as ✓ does and then
   moves on (§4.3, 6).
5. **C12 is decided, not asked.** `▸` carries no number. A2's options show it so; if he asks for the count in the
   chip it is one word in his answer.

**Found while confirming, and written into the design**

- `SurveyMeas.rowValue` and `SurveyChain.rowValue` have no reader but the row, so C1 deletes them with their tests;
  the Tape's, Line's and area's own figures are already on the photo, so nothing is lost.
- With the row's Delete gone, the 6 dp of dead space under it goes: the held state in I1 is ≈466 dp of photo, not
  ≈460.
- The code is drawn at `SurveyPrepDraw.kt:571-572` (C8 cited 568-569).
- No double buzz exists today; the failed check's is this design's own, and the refused reading's long buzz is told
  apart from it.
- `port63-quad` forks at `7c49835`, before H1 and F1; five source files, the literal counts and three tests changed
  on both sides, so its merge before I2 is a step with its own green run (§14).
