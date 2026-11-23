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

## 18. No photo estimate, anywhere (Amit, 16 Nov 2026, ~22:55 IST) — this section OVERRIDES every `≈` above

Amit was sent the seven pictures of this design (`imagemeter-pictures/`, their first set) and answered in his own
words: **"No foto estimation at all. It confuses user"**. Every figure drawn `≈` in those pictures — the photo's
estimate of a side nobody had measured — goes. It overrides §0 ("an unread figure is the photo's `≈` estimate"), §2.2,
§4.3 item 4, §5, §9, §15 A3, `DESIGN-port63.md` §1.1 (which chose the estimate over a blank), and his own earlier
choice "Every mark's size, always", which now reads: every mark's MEASURED size, always. The pictures are redrawn; the
folder holds the new set and the first set is gone from it.

**The rules.** Builders follow these where the sections above differ.

1. **The phone never shows a figure nobody measured.** No `≈` on the photo, on the 360, in the note line, on the Plan
   or the six drawings, in the saved pictures (already "Measured figures only", his answer the same evening) or in
   an export a person reads. A dimension has two looks only: READ — its figure, white bold on black — and NOT READ.
2. **Not read is shown only where it can be acted on.**
   - **A mark that is not picked shows only what has been read**: each read size figure on its own edge; nothing at
     all for an unread side. A wall nobody has measured is outlines and codes. What is left is said by the tabs'
     amber counts, by the code's missing `✓`, and found by `▸`.
   - **The picked mark shows every dimension it asks** — size and position — read ones as figures, unread ones as a
     quiet `?` plate (grey, dashed: the old estimate's plate with `?` in it), each a tap target; the one being
     measured is H1's gold plate reading `▶ ?` until the reading lands or digits are typed. This is ImageMeter's own
     sign for a measure with no value (§0), kept to the one element that is selected.
   - A small box's compact plate carries its read halves; an unread half is `?` only while the mark is picked. A
     `⊥` depth is `⊥ ?` in the same quiet style as any other unread figure (it was amber: there is now one "not
     read", not two).
3. **A Line** shows its reading once read; until then its code alone, and `?` after it while it is picked. **An
   area's m² is computed from the photo, so it is not shown**; its perimeter is, once read. **A chain** (kept only
   where a room already has one) shows read segments and a read total; no derived and no photo figure.
4. **The Tape is the one exception, and it is asked** (A7): it is a ruler the surveyor lays on the photo with his own
   hand, its band and ticks say so, and its figure IS what the photo says — that is the tool. It stays as built
   until he rules; he moved one that evening and asked only that it move by its body.
5. **The check keeps no number and becomes a gross guard.** `sp.measure.photo_says_x` ("The photo says ≈640 mm…")
   goes: it quoted an estimate. What stays is the guard against a shot that landed on the wrong thing while the gold
   moved by itself (§16 risk 2): a reading more than TWICE, or less than HALF, the side as the mark is drawn keeps
   its `!`, does not move on, double-buzzes, and the note says, with no figure, that the reading does not match the
   mark (`sp.measure.check_reading`, new, English first). The 150 mm / 15 % rule goes: with loosely drawn marks it
   would cry wolf, and nobody now sees the estimate it was compared with. Whether even this `!` stays is asked (A8).
   The estimate itself lives on in the model for this guard and for nothing a person reads.
6. **A mark that is not in the room** (a Client wish, a Vendor-added mark) asks no reading and so shows no figure at
   all on the phone. How a wished thing's intended size is given — typed, in the panel — belongs to the wish round's
   own design; nothing is built for it here.
7. Unchanged: the room's W, L and H and the reference rectangle's W and H are readings or typed figures, never
   estimates. S5 `/projects` still shows the office the photo's figure beside each reading (its Diff column); that
   page is not the surveyor's screen and is asked separately (A9), not changed here.

**In the models.** `MarkDims.value` answers MEASURED or UNKNOWN; the ESTIMATE state and its style go from the
screen's path (the saved pictures' old painter, which still prints both, goes with slice M3). `DimLayout.place` is
handed only the figures rule 2 shows, so an unpicked, unmeasured mark costs it nothing. `MeasureWalk`'s order and
counts are unchanged: they count what is asked and not read.

**In the slices.** I1 carries rules 1, 2, 3 and 5 for the photo in 2 · Mark and for the 360's tags — it is the slice
that rewrites what the photo draws. The Plan, the six drawings and the exports follow in M3 with the saved pictures
("Measured figures only" is the same rule). Nothing else in §14 moves.

**Asked of Amit when their slice comes up, not before** (he closed a four-question selector that evening and
answered in a line; a question is put to him only when a build waits on it): **A7** the Tape's photo figure (stays /
goes / shown only while picked); **A8** the gross-mismatch `!` (stays / goes); **A9** the office's web page still
showing the photo figure beside a reading. **A3 is answered by this section** and is no longer asked.

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

---

## 19. I1a as built — the pure rules, beside what stands (16 Nov 2026)

Slice I1 is built in two commits. **I1a (this one) adds every pure rule I1 needs to `:pano`, with its tests, and changes
or deletes NOTHING that `:app` calls**: the only edited line of existing logic is `DimLayout.place`, whose body moved
into a private `lay` that both it and the new `placeShown` call (its 1,436 earlier tests are unchanged and green).
**I1b switches the screen over by calling what is listed here and deleting the old** (§13's "Deleted" rows for I1).
Nothing of I1a is on screen; `:app` is compiled only by CI and has not been touched.

Built on `port63-i1` at `3c377c3` (the phone's 0.3.341 plus the four-corner mark's pure models). **Line numbers below
are that commit's — and this commit's, since no `:app` file changed.** `SPS` = `SurveyPrepScreen.kt`, `SPD` =
`SurveyPrepDraw.kt`, `SP360` = `SurveyPrep360.kt`, `SPT` = `SurveyPrepTools.kt`, `SM` = `SurveyMeasure.kt`, all under
`APP/`. `:pano` after I1a: 106 test classes, 1,522 tests, 0 failures (before: 101 and 1,436).

### 19.1 New functions, and the `:app` call site each is meant to replace

**`Pick` (new file `PANO/Pick.kt`; `PickTest`, 40 tests — one per row of §3.1, named by its row)**

| New | Signature | Replaces in `:app` |
|---|---|---|
| `Pick.State` | `data class State(picked: String? = null, held: Boolean = false, gold: String? = null)`; `Pick.NONE` | the two states `sel` (`SPS:299`) and `active` (`SPS:300`); `SpSel.part` stays as the nudge's part |
| `Pick.Scene` | `class Scene(prep, stage: Int?, step: String, ab: (String) -> DoubleArray)` | built where `prep`, `quoteStage`, `mode` and `ab` (`SPS:389`) are in scope |
| `Pick.Out` | `state, pad: Pad, refused: MeasureFlow.Refused?, options, nudge: String?, finished: Finished?, stay, byItself, waits, zoom` | what each handler below then does; `Pad` = `KEEP / OPEN / SHUT_STORE / SHUT_DROP` drives `padOpen` (`SPS:308`) and `commitPad` (`SPS:591`) |
| `Pick.mayHold` | `(sc: Scene, id: String): Boolean` | the `tabs = …editable…` of `SPS:3331`, and the locked / phase checks at the head of every drag |
| `Pick.goldOk` | `(sc: Scene): Boolean` | `mode == StepRoute.MEASURE && …` and `MeasureFlow.canRead(quoteStage)` wherever `active` is set (`SPS:492`, `553`, `566`) |
| `Pick.settle` | `(sc: Scene, st: State): State` | the stage-moved effect `SPS:902-908` (its line 907) |
| `Pick.turnedGold` | `(before: State, after: State): Boolean` | where `sayCheck` (`SPS:533`) and, new, `LaserChip.gold` are asked |
| `Pick.body` (rows 1, 2) | `(sc, st, id: String): Out` | `selectMark` `SPS:760-766`, `grabbed` `SPS:769-782`, `markTap` `SPS:1169-1176`, `selectChain` `SPS:1121-1128`, `selectMeas` `SPS:1131-1138` |
| `Pick.longPress` (row 3) | `(sc, st, id: String): Out` | the `LongPress` branch `SPS:3183` (opens today's `ctx` menu until I2) |
| `Pick.figure` (rows 4-6) | `(sc, st, code: String): Out` | `figureTap` `SPS:1147-1163` |
| `Pick.options` (row 7) | `(st): Out` | I2 (the code chip, `⋯`); until then the held row's label, `SPS:1891` |
| `Pick.handle` (row 8) | `(st, part: String): Out` | the grab of a tab with no drag — `sel.part`, read by the row at `SPS:1870` |
| `Pick.handleId` | `(st): String?` | the `selId` handed to `SurveyPrepTouch.hit` at `SPS:2794` (and the `null` at `SPS:2746`) |
| `Pick.tap` | `(sc, st, hit: SurveyPrepTouch.Hit?, line: String? = null): Out` | `hitMarks` `SPS:2780-2825` with `hitChain` / `hitMeas` (`SPS:3072-3076`), and `measureTap` `SPS:3026-3032` |
| `Pick.drag` (row 9) | `(sc, st, on: String?): Drag` — `MOVE / PAN / LOCKED / READ_ONLY`; `Pick.BODY_SLOP_DP = 8.0` | the `Down` branch that grabs on press, `SPS:3053-3078`, and `val measuring` `SPS:2996`; `Pick.dragged(st)` is row 9's no-change |
| `Pick.empty` (rows 10-12) | `(st): Out` | `tapAt` `SPS:3020-3022` when no tool is armed; the `else -> Unit` of `SPS:3030` |
| `Pick.emptied`, `Pick.doubleTap` (row 13) | `(before: State, now: Long): Emptied`; `(sc, st, first: Emptied?, now: Long): Out` | slice I4, the `DoubleTap` branch `SPS:3170` |
| `Pick.placed` (row 14) | `(sc, id: String): Out` | `SPS:815` (`placeMark`), `837` (`place360`), `856-857` (`finishChain`), `1208` (`endDrag`), `1231` (`areaTap`) |
| `Pick.duplicated` (row 15) | `(sc, copy: String): Out` | `SPS:1092`, `1097`, `1102` |
| `Pick.shaped` (row 16) | `(sc, st, id: String): Out` | I2 / Q2 (`Fine move`, `Four corners`, `Rectangle`) |
| `Pick.locked` (row 17) | `(sc, st, id: String): Out` | `toggleLock` `SPS:1009` |
| `Pick.landed` (rows 18-20), `Pick.released` | `(sc, st, code: String, busy: Boolean, typed: Boolean = false): Out`; `(sc, st, code): Out` | the `moveOn` half of `reading` `SPS:565-570` — asked through `Landing`, below |
| `Pick.next` (rows 21, 22) | `(sc, st, face: String): Out` | `barStep` `SPS:1184-1194`, `walkOn` `SPS:551-556`, `measureAt` `SPS:538-543` |
| `Pick.finishedText` | `(tx, sc, face: String, f: Finished): String` | the bar's `Kind.DONE` line (`MeasureFlow.onFace`, `SPS:2009`), now said by `say` |
| `Pick.undone` (row 23) | `(sc, st): Out` | `SPS:486-493` in `undo` |
| `Pick.deleted` (row 24) | `(st, id: String): Out` | `deleteMark` `SPS:951` |
| `Pick.tab` (rows 25, 26) | `(st, from: String, to: String): Out` | `showTab` `SPS:1272-1287` (`MeasureFlow.move`, `SPS:1275`) |
| `Pick.step` (rows 25, 26, 28) | `(st, from, to, face, ask: PhaseTools.Ask): Out` | `setMode` `SPS:1288-1311` (`MeasureFlow.move`, `SPS:1295`) |
| `Pick.handles` | `(st, id: String): Boolean` | `ends = …` and `tabs = …` handed to `SurveyPrepDraw.Marks`, `SPS:3328`, `3331` |
| `Pick.trash` | `(sc, st, toolArmed: Boolean): Boolean` | the disc's press `SPS:2946` and its drawing `SPS:3342` |
| `Pick.row` | `(sc, st): Row?` — `Row(sheet, nudges, padlock)` | the three rows of `markPanel`, `SPS:1868-1967`: the value texts (`SPS:1931`, `1955`) and the Deletes with `SpDeleteGap` (`SPS:1909-1913`, `1937-1941`, `1961-1965`) go |

**`Landing` (new file `PANO/Landing.kt`; `LandingTest`, 9 tests)**

| New | Signature | Replaces in `:app` |
|---|---|---|
| `Landing.route` | `(shot: SurveyRef.Shot?, sc: Pick.Scene, st: Pick.State, mm: Int, now: Long): Route` — `Ref(s, k)` / `Gold(code)` / `Refused(text: Msg, haptic = LONG, stays = true)` | `disto.onReading`'s `when`, `SPS:627-646` |
| `Landing.after` | `(sc, st, code: String, busy: Boolean): After` — `state, pad, haptic: Haptic?, note: Msg?, ring: String?, byItself, waits` | `SPS:640-642` (`dropTyped`, `reading`, the one haptic) and `sayCheck` `SPS:571` |
| `Landing.released` | `(sc, st, code: String): After` | new: asked when the Mark sheet (`markSheet`, `SPS:787`), the More sheet or the menu closes, and when the last finger lifts (`photoBusy`, `SPS:353`) |
| `Landing.keyed` | `(sc, st, code: String): After` | the pad's `onNext`, `SPS:1986`, when `MeasurePad.commit` wrote a value (else `Pick.next`) |
| `Landing.Haptic`, `Landing.RING_MS` | `TICK / DOUBLE / LONG`; `1000L` | `HapticFeedbackType.TextHandleMove` at `SPS:642`; the ring is new |

A `Refused` whose `stays` is true keeps its note until the next touch: `say` (`SPS:367-368`) needs a sibling that does
not start the 2.6 s timer, cleared by the gesture loop's first `Down`.

**`LaserChip` (new file `PANO/LaserChip.kt`; `LaserChipTest`, 7 tests)**

| New | Signature | Replaces in `:app` |
|---|---|---|
| `LaserChip.Link`, `look`, `shown`, `ready`, `label`, `TEXT`, `WIDTH_DP` | `enum Link { OFF, SCANNING, CONNECTING, READY }` (= `DistoClient.State` by name); `look(link): Look`; `shown(stage): Boolean`; `ready(link): Boolean`; `label(tx, link): String` | the chip is new, in the top bar between the title and ↶; `ready` feeds `PhotoFigures`' `ready` |
| `LaserChip.Mem`, `Step` | `data class Mem(startedAt: Long?, asked: Boolean, wasReady: Boolean)`; `class Step(mem, start, askPermission, say: Msg?)` | one `remember`ed `Mem` beside `distoState`, `SPS:519` |
| `LaserChip.gold`, `opened` | `(mem, link, permitted: Boolean, stage: Int?, now: Long): Step` | `fire()` `SPS:610-615` and `distoGo()` `SPS:603-609` are DELETED from survey prep (`fireRef`, `SPS:1051`, keeps its own until I4); `opened` in a `LaunchedEffect(Unit)` |
| `LaserChip.tap` | `(mem, link, permitted, now): Step` | the chip's click; `askBle.launch` at `SPS:605` moves here |
| `LaserChip.permission` | `(mem, granted: Boolean, now): Step` | the launcher's result, `SPS:525-527` |
| `LaserChip.state` | `(mem, link, note: Msg?): Step` | `disto.onState`, `SPS:625` (`if (n != null) say(n(tx))` becomes `step.say?.let { say(it(tx)) }`) |

**The three steps (`StepRoute`, `PhaseTools`; `OneStepTest`, 11 tests)**

| New | Signature | Replaces in `:app` |
|---|---|---|
| `StepRoute.LINES / MARK / PANO`, `ROUTE` | `const`s; `val ROUTE = listOf("lines", "mark", "360")` | `StepRoute.STEPS` and every `"mark"`, `"lines"`, `"360"`, `StepRoute.MEASURE` literal in `SPS` |
| `StepRoute.routeName`, `routeIndex`, `routeLabel`, `routeOf` | `(tx, key)`, `(mode): Int`, `(tx, i: Int)`, `(mode): String` | `stepName` `SPS:1570`, `stepIndex` `SPS:1536`, `1547`, `label` `SPS:1551` |
| `PhaseTools.routeTool`, `routeSteps` | `(step): Tool?`; `(stage): List<String>` | `prepSteps` at `SPS:1291`, `1374`, `1546` |
| `PhaseTools.routeMode` | `(mode, stage, markable: () -> Boolean): String` | `prepMode` at `SPS:398` |
| `PhaseTools.routeAsk`, `Ask`, `routeRefusal` | `(step, stage, markable): Ask` — `OPEN / HIDDEN / LINES_OPEN`; `(tx): String` | the two guards at the head of `setMode`, `SPS:1291-1292` |
| `PhaseTools.holds` | `(stage: Int?): Boolean` | new (see 19.3, decision 3) |
| `StepRoute.toolRowDp`, `keypadDp`, `heldRowDp`, `Under`, `underDp`, `underMark`, `markPhotoDp`, `landUnderDp`, `landFits` | `(land): Int` x 3; `enum Under { NOTHING, TOOLS, HELD, KEYPAD }`; `underMark(toolRow, held, keypad): Under` | which of the rows `markPanel` draws, `SPS:1866-1868`, `1976-1997`; `measurePanel` `SPS:2006-2019` and its dispatch at `SPS:2188`, `2217` are deleted; the pad's height at `SM:212` gains the 6 dp the bar's padding gave |

**The row and its tools (`SurveyPrepPalette`; in `OneStepTest`)**

| New | Signature | Replaces in `:app` |
|---|---|---|
| `SurveyPrepPalette.afterPlaced`, `armed` | `(tool, keep: Boolean = false): String`; `(tool): Boolean` | after each placement (`SPS:815`, `837`, `1208`, `1231`): `toolSet = SurveyPrepPalette.afterPlaced(tool)`; `armed` for every `tool == SurveyPrepPalette.SEL` test (`SPS:2946`, `3072`, `3328`, `3342`) |
| `SurveyPrepPalette.row`, `RowKind`, `RowSlot`, `rowOn`, `NEXT_GLYPH` | `(kind, tools = offered()): List<RowSlot>`; `enum RowKind { TOOL, TYPE, MORE, NEXT }`; `(slot, kind, tool): Boolean` | `slots`, `SlotKind`, `isOn` in `SpPalette`, `SPT:104-147`; `drawSelectIcon`, `SPT:393`, goes |
| `SurveyPrepPalette.nextSlot`, `NextSlot` | `(tx, prep, face, gold: String?, stage, step, ab): NextSlot` — `shown`, `label` | the bar's `›`, `SM:177` / `SPS:1983`, `2014` |

**No photo estimate (`PhotoFigures` new, `MarkDims`, `DimLayout`, `SurveyMeas`, `SurveyChain`, `MeasureFlow`, `MeasureText`, `SurveyRef`; `PhotoFiguresTest` 16 tests, two more in `DimLayoutTest`, one in `SurveyPrepTouchTest`)**

| New | Signature | Replaces in `:app` |
|---|---|---|
| `PhotoFigures.ofMark`, `Figure`, `Look` | `(tx, actual, s, e, a, b, picked: Boolean, gold: String?, live: String? = null, ready: Boolean = true): List<Figure>`; `Look { READ, ASK, GOLD }` | the rule itself; asked by the two below |
| `PhotoFigures.onFace` | `(tx, prep, s, a, b, pick: Pick.State, live, ready): List<Pair<Mark, List<Figure>>>` | for a test or a reader that wants the figures without a canvas |
| `PhotoFigures.layout` | `(tx, prep, s, a, b, pick, toPx, u, dp, textW, live = null, ready = true, free = { false }): List<DimLayout.Mark>` | the `laid` list, `SPD:942-948` |
| `DimLayout.markShown` | `(tx, actual, s, e, a, b, picked, held, gold, toPx, u, dp, textW, live = null, free = false, ready = true): Mark` | `DimLayout.mark`, `SPD:947` |
| `DimLayout.placeShown` | `(marks, width, height, keep, u, dp, times = 0.0): Out` | `DimLayout.place(…, o.scope, …)`, `SPD:950`; `Marks.scope` (`SPD:236`), `figScope` (`SPS:3303`), `allFigs` (`SPS:303`) and the `all` button (`SPS:3463-3470`) go |
| `MarkDims.read`, `plate`, `figureText`, `ASK_TEXT`, `WAIT_GLYPH`, `ASK_INK` | `read(actual, s, e, d: Dim, a, b): Value`; `plate(state, gold): Style`; `figureText(v, role, gold, ready = true, name: String? = null): String` | `MarkDims.style(f.state, f.active)` at `SPD:1023` becomes `MarkDims.plate(…)`; `value`, `shown`, `style` stay for the saved pictures until M3 |
| `MarkDims.gross`, `guarded`, `guard`, `GROSS_OVER`, `GROSS_UNDER` | `gross(read, drawn, inPlane): Boolean`; `guarded(role): Boolean`; `guard(actual, s, e, d, a, b): Boolean` | inside `read`; `check` / `photoCheck` stay for the old path |
| `MeasureFlow.flagged`, `guardText` | `(prep, code, ab): Boolean`; `(tx, prep, code, ab): String?` | `sayCheck`, `SPS:533-536` (`checkEst` + `MeasureText.photoSays`) |
| `MeasureText.checkReading`, `on360` | `(tx): String`; `(tx, mm: Int): String` | through the two above and `Landing.route` |
| `PhotoFigures.line`, `Tag`, `DRAFT_TAG` | `(m: Meas, actual, pick, ready = true): Tag` — `text, key: String?, gold, colour: Int?` | `SurveyMeas.lineTag` + the glyph + `figureTag`, `SPD:747-757`; a draft (`SPD:695-697`) draws `DRAFT_TAG` |
| `PhotoFigures.chainSeg`, `chainTotal` | `(c, v: Vals, i: Int, pick, ready = true): Tag?`; `(tx, c, v, pick, ready = true): Tag` | `SPD:888-894` and `SPD:896-906`; a chain being drawn (`SPD:837-838`, the `≈` at `SPD:889`) draws no figure |
| `PhotoFigures.line360`, `chainSeg360`, `chainTotal360` | `(m, actual): String`; `(v, i): Pair<String, Int>?`; `(tx, code, v): String` | `SP360:1080-1083`, `SP360:1180-1182`, `SP360:1186` |
| `SurveyMeas.shownTag`, `tagTaps` | `(code: String?, kind, reading: Double?, ask: Boolean): String`; `(reading, ask): Boolean` | under `PhotoFigures.line` |
| `SurveyChain.segShown`, `totalShown` | `(v, i, ask): Pair<String, Int>?`; `(tx, code, v, ask): String` | under `PhotoFigures.chainSeg` / `chainTotal` |
| `SurveyRef.shownOnRoute` | `(tx, s, shown: Boolean): String` | `SurveyRef.shownSaid`, `SPS:1048` |
| `MeasureFlow.tabLeft` (unchanged) | `(prep, s, stage, a, b): Int` | already the tabs' count in EVERY step, `SPS:1522` — see 19.3, 9 |

**Texts (English first; `mr-pending.tsv` 27 → 34):** `sp.laser.off`, `sp.laser.ready`, `sp.laser.on_360_x`,
`sp.measure.aria.next_x`, `sp.measure.aria.keep`, `sp.reference_shown_mark` as §12 words them, and
`sp.measure.check_reading` = "This reading does not match the mark. Shoot again, or ▸ to keep it." No key is deleted
in I1a: §12's "Deleted" keys and `sp.measure.photo_says_x` still have their readers on the screen and go with them in I1b.

### 19.2 The order the screen calls them in (I1b)

- **A tap on the photo, no tool armed:** `val o = Pick.tap(sc, pick, hit, line)`; then the pad as `o.pad` says
  (`SHUT_STORE`: `commitPad()` then shut — NOT before asking `Pick`, or a tap on the gold plate would store half a
  figure); `pick = o.state`; say `o.refused`; if `Pick.turnedGold(before, o.state)`: `LaserChip.gold(…)` and
  `MeasureFlow.guardText`. A press does nothing until it lifts unless `Pick.drag(sc, pick, id) == MOVE`.
- **A reading:** `Landing.route` → for `Gold(code)`: write it (`reading(code, v)`, the one place), then
  `Landing.after(sc, pick, code, busy)` where `busy` = a sheet or the menu is open, or `photoBusy`; apply
  `state`, `pad` (drop the typed digits, shut), `haptic`, `note`, `ring`; if `byItself`, `MeasureFlow.panTo`; if
  `waits`, keep `code` and ask `Landing.released` on close / lift.
- **Next ▸ on the keypad:** `MeasurePad.commit` → a value: write it, `Landing.keyed`; nothing written: `Pick.next`.
- **`sc.step`** is `PhaseTools.routeMode(modeSet, quoteStage) { … }`; `Pick.settle` runs where `SPS:902` runs.

### 19.3 Where the design was not exact enough, and what was decided

1. **Rule 5's "the side as the mark is drawn"** is read as the mark's SIZE: `w`, `h`, a four-corner mark's sides and
   diagonal (`MarkDims.guarded`). Position figures are not guarded: a door drawn 300 mm from a corner that is really
   90 mm away would keep its `!`, stop the walk and double-buzz at every door. One line to widen if Amit wants it (A8).
2. **The new sentence** (§18 gives no words): "This reading does not match the mark. Shoot again, or ▸ to keep it." It
   names `▸` because a failed reading no longer moves on, and `▸` is the way out.
3. **§10's "`PhaseTools.editable` decides who can be held" would let a mark born Vendor added be dragged at Client
   approved** — `editable` answers true there, as the prototype's code does, and until now it could not be reached
   because those phases had no 2 · Mark. So `PhaseTools.holds(stage)` (the prototype's own `mark` row) is asked first:
   at Estimation and Client approved 2 · Mark is the view and NOTHING is held, exactly as 3 · Measure left it.
4. **Every reading thrown away is felt and its note stays** — nothing gold (§4.4), and also on the 360, after Survey
   and a reference box that was left. §4.4 names the first only; one rule for "a reading thrown away must be felt".
5. **A tap on the photo shuts the keypad and stores** (§5), in every row but row 5 — a tap on another figure
   included: the second figure is then gold and the keypad is one more tap. Next ▸ is the way to type along a wall.
6. **Row 13 (the double tap) is built now** though the gesture is I4's, because the table is the specification.
7. **A chain's total the laser gives is shown** ("FK1 Σ 2678 laser"): it is the room's own W, L or H, a reading by
   rule 7, not an estimate. A total that is only the sum of its segments, and a derived segment, are not shown; while
   the chain is picked each is a `?`, since it still takes a reading.
8. **A Line's or an area's tag takes a tap only when it shows a figure or `?`**; the code alone is not a tap target
   (a tap on the Line's body picks it, which is what puts the `?` there). **A Line being pulled and an area being
   tapped show no figure at all** — their length and m² are the photo's; rule 3 names only plotted ones. The Tape's
   draft keeps its figure, with the Tape (rule 4).
9. **"The tabs' counts show in Mark" needs nothing new:** `MeasureFlow.tabLeft` takes no step and `SPS:1522` already
   draws it in every step (§2.1 and §9 say "today in 3 · Measure only"; the code at `3c377c3` says otherwise).
   `OneStepTest` pins that the rule names no step.
10. **§18 rule 6 against `MarkDims.asked`'s own note:** a mark not in the room shows NO figure, a reading an earlier
    build left on it included. Row 6's "figure of a mark not in the room" can then no longer be tapped; the row is
    kept and tested, for a key that arrives any other way.
11. **A gold `⊥` chip carries its name** (§2.2) with the reading or `?` after it: "▶⊥ reveal depth ?". `markShown`
    and `PhotoFigures` therefore take a `Tx`.
12. **The four-corner mark's closing check** (`sp.measure.sides_say_x`, "The other sides make the bottom side ≈2466
    mm") quotes a figure nobody measured. `guardText` says the no-number sentence for it too. No such mark can exist
    yet (its menu row is off); slice Q2 owns its words.
13. **`▸` with a figure gold** goes to the FACE's next unread figure (row 21, `MeasureWalk.next`), not first to the
    same item's as a landed reading does (row 18): a figure skipped inside a mark comes round at the end of the wall.
14. **The hollow `▶`** is the glyph `▷` (`MarkDims.WAIT_GLYPH`), from `LaserChip.ready`.
15. **Landscape's held row** is taken as 34 dp (`StepRouteTest`'s own "about 34"); portrait's as §2.4's 48.
16. **No `:pano` function or test is deleted in I1a** — `MeasureBarLayout`, `MeasureFlow.Bar / onFace / onItem / move`,
    `SurveyMeas.rowValue`, `SurveyChain.rowValue`, `DeleteGuard`'s row clearances, `SurveyPrepPalette.slots`,
    `StepRoute.MEASURE` and their tests all have `:app` callers at this commit. They go in I1b with those callers;
    after deleting each, grep `:app` AND `:camera`.

---

## 20. I1b as built — the screen switched over (23 Nov 2026)

I1b is the third commit of slice I1 on `port63-i1`, over I1c (`4311e1e`) over I1a (`2b0f7bf`) over `main` (`3c377c3`,
Play 0.3.345). It makes survey prep CALL the pure rules of §19 and §21 and DELETES what they replace (§13's "Deleted"
rows for I1). `:app` holds no rule of its own for any of it: who is picked, held or gold, where a reading goes, what
the chip, the bar, the tool row and the photo show are `:pano`'s answers, and the screen applies them.

**What was checked, and what was not.** `:pano` after I1b: 107 test classes, 1,518 tests, 0 failures (after I1c: 109
and 1,549 — the 31 fewer are the deleted rules' own tests: `MeasureBarLayoutTest` 6, `OnFaceTest` 4, `MeasureFlowTest`
14, `MeasureTextTest` 5, one each of `DeleteGuardTest`, `MeasureScreenTest` and `StepRouteTest`, less one new
`SaveStayTest`). `:app` is compiled only by CI. In this container it was TYPE-CHECKED against stand-ins: kotlinc 2.0.21
(from `~/.gradle`), the real `:pano` classes, and hand-written stubs of the Compose and Android APIs, compiling
`SurveyPrepScreen.kt`, `SurveyPrepDraw.kt`, `SurveyPrep360.kt`, `SurveyPrepTools.kt`, `SurveyMeasure.kt`, `MarkSheet.kt`,
`SurveyPlanScreen.kt` and `SurveyPrepLocks.kt` together, and the same eight files of `4311e1e` against `4311e1e`'s
`:pano` with the same stubs. `SurveyPrepScreen.kt`, `SurveyPrepDraw.kt` and `SurveyMeasure.kt` have NO error at all;
every error left (235 now, 247 before) is in the other five files, in lines this commit did not write, and is a stub
gap (a Compose overload the stubs lack — `Modifier.size(w, h)`, `DrawScope.drawRect(style =)`, `Color(r, g, b, a)`,
`Path`); no kind of error grew. `MainActivity.kt` was NOT type-checked (it reaches most of the app); its two changed
call sites were read against the dialog's new parameters. Nothing has been seen on a phone.

### 20.1 What the phone shows, state by state (Survey, unless said)

- **Opening a face (P1).** A face whose four lines are all placed by hand and close (`PhaseTools.linesSet`) opens on
  **2 · Mark**; any other face on **1 · Lines**. A face tab does the same; the open face's own tab changes nothing
  (Pick row 26); the Plan keeps its step; 3 · 360 stays on 3 · 360. 2 · Mark on a face whose lines do not close is
  refused in the words it always was, and nothing changes.
- **The strip is three steps**, `1 · Lines`, `2 · Mark`, `3 · 360` (`StepRoute.ROUTE`, `routeSteps` per phase). There
  is no 3 · Measure, no measure bar, no figure box, no ‹ ›, no on-screen D2 and no `all` button.
- **2 · Mark, nothing picked.** Every mark's CODE on its box (it names the mark), and every figure of every mark that
  has been READ — its size and its position, the `⊥` depth included — as the number alone, laid along its own
  dimension: a width plate level on its width line, a height plate turned up its height line, a Line's or a Tape's
  figure turned to the line and breaking it, never upside down (P3, P4, P5; `PhotoFigures.layout`,
  `DimLayout.placeShown`). An unread figure shows NOTHING. No photo estimate anywhere, on the photo or the 360 (§18).
  A Line, an area, a Tape and a chain carry no code on the photo.
- **A tap on a mark (or a Line, a Tape's band, an area, a chain): picked and HELD** (Pick rows 1 and 2): its unread
  figures show a quiet `?`, and at Survey its first unread figure is **gold** — `▶ ?` on a gold plate over a dark
  casing — unless one of its figures already was. Its handles show. Under the photo the TOOL ROW gives way to the HELD
  ROW (`StepRoute.underMark`): the mark's label (a tap opens its Mark sheet), its nudges and its padlock; for a Line,
  an area, a Tape or a chain its code (the one place a code is shown, P4) and its padlock. No value and no Delete in
  any row. A second tap keeps the gold he chose.
- **Only the held item moves** (`Pick.drag`): a tab resizes from the first pixel; the body moves once the finger is
  `Pick.BODY_SLOP_DP` (8 dp) away. A drag on anything else pans. A locked item or one the phase holds read-only is
  picked, never held, and a drag on it says why.
- **The trash disc shows only while an item is held and no tool is armed** (`Pick.trash`), and it deletes the held item
  only (Pick row 24). The long-press menu keeps its Delete. A delete is one undo step, as before.
- **A tap on a figure** turns it gold (its owner picked, held only if it already was); a tap on the GOLD figure opens
  the KEYPAD in the tool row's place (`Under.KEYPAD`). Its Next ▸ stores what was typed (`Landing.keyed`) or moves on
  (`Pick.next`). Any other tap on the photo stores what was typed and shuts the keypad.
- **The laser.** The chip in the top bar (`LaserChip`, shown in the phases that take readings): grey "Laser not
  connected · tap to connect" (a tap starts the link and asks for Bluetooth once), connecting, gold "D2 connected ·
  press the meter's own button to read". A figure turning gold starts the link by itself (`LaserChip.gold`). A reading
  from the meter's own button goes to the gold figure (`Landing.route`): one light tick when it lands and passes, two
  when it fails the check (`1234!`, the gold stays, "This reading does not match the mark. Shoot again, or ▸ to keep
  it."), one strong buzz when it is thrown away (nothing gold, the 360, after Survey), whose note stays until the next
  touch. The plate it landed on carries a gold ring for a second. After a pass the gold moves on by itself to the next
  unread figure on the wall, the handles go, and the photo pans to it if it is off screen. A reading that lands while a
  sheet is open or a finger is on a grab waits, and moves on when the sheet closes or the finger lifts.
- **The tool row's last slot is `▸`** (`SurveyPrepPalette.nextSlot`): the next figure to measure on the wall, or, while
  a figure is gold and nothing is left, "keep this reading". There is no Select slot: with a type armed, a tap places
  it, and the tool lets go after one placement (`afterPlaced`); the new item is picked, held and its first figure gold.
- **✓ Save saves and STAYS (P2).** On a kept capture a press does, once each: the read-back save, the as-is upload
  queued, the room size kept, the survey pictures rewritten (one press at a time — a `Mutex` in `MainActivity`). The
  screen stays on the same face, step and zoom, and the bar says "Survey prep saved — writing its pictures…", then
  "… · 6 pictures in the room folder" or why not, until the next change. On a NEW capture a press is the read-back save
  alone and the bar says the autosave's line; ← files the capture, as before, and then queues the upload the press
  held back, once (`SaveStay.Flow.NEW`). Only ← leaves. **Since port 63 P2N (§23.1) a NEW capture's first press files
  it, once, and stays, and it is a kept capture's screen from that answer on.**
- **Estimation and Client approved.** 2 · Mark is the view: nothing is held, nothing is gold, figures are the READ ones
  only; the phase's refusals are as before.

### 20.2 What was deleted

From `:app`: the measure bar `SpMeasureBar` and its arrows, `measurePanel` and its dispatch, the 3 · Measure step and
every branch for it (`measureHit`, the measure-mode taps), `fire()` and `distoGo()` (the on-screen D2 of survey prep;
1 · Lines' reference box keeps its own D2, `fireRef`, until I4), the row Delete buttons and `SpDeleteGap` with their
colours and heights, the row values, the Select slot and its icon (`drawSelectIcon`), the sticky tool, the `all`
button and `figScope` / `allFigs`, `figureTag`, the old plate layout call, the photo estimate on every screen path, and
`onDone` (its body is the kept capture's `onSaved`, its NEW-capture use is `onCancel`).

From `:pano` (each with its tests, where it had any): `MeasureBarLayout` (file); `StepRoute.MEASURE`, `STEPS`,
`stepName`, `label`, `stepIndex`, `measureBarDp`; `PhaseTools.toolOf`, `stepShown`, `steps`, `modeFor`, `prepSteps`,
`prepMode`; `MeasureFlow.Bar`, `Kind`, `onFace`, `onItem`, `Move`, `move`, `body`, `own`, `value`, `checkEst`,
`checkText`; `MeasureText.label`, `Cell`, `cell`, `progress`, `nextFace`, `photoSays`, `sidesSay`; `DeleteGuard.rowGapDp`,
`rowClearDp`, `rowPadDp`, `rowToBarDp`, `clearOfD2Dp`, `rowToPaletteDp`, `clearOfPaletteDp`; `SurveyMeas.rowValue`;
`SurveyChain.rowValue`; `SurveyPrepPalette.SlotKind`, `Slot`, `slots`, `isOn`; `DimLayout.Scope`, `shows`, `place`;
`SurveyRef.shownSaid`.

Keys (from both tables, from `mr-pending.tsv` where listed, and `index.tsv` regenerated): `sp.measure.all`,
`sp.measure.aria.all`, `sp.measure.aria.previous`, `sp.measure.aria.skip`, `sp.measure.aria.type`, `sp.measure.x_of_x`,
`sp.measure.next_face_x`, `sp.measure.photo_says_x`, `sp.measure.sides_say_x`, `sp.reference_shown_measure`,
`refuse.sp.tap_measurement_first`, `common.select`, `sp.tape_x_mm` and `sp.x_x_mark_x` — the fourteen whose last
reader this commit deleted (found by diffing the keys with no reader at `4311e1e` against now; the 658 accessors that
had no reader before are untouched). 1,931 keys; `mr-pending.tsv` 34 → 24 keys (ten of the fourteen were pending); `unkeyed.tsv` stays empty.

### 20.3 Departures, and why

1. **`DimLayout.mark` and the level-plate rule (`DimLayout.placeBy`) stay, with no production caller.** The cases
   of `DimLayoutTest`, `RectanglePinTest` and `TurnedLabelsTest` pin the placing rule through them, case by case and
   in the old scopes; the scopes went to a test helper (`OldPlace`) and `placeBy` is `internal`. Deleting them would
   delete the evidence that `placeShown` keeps every level plate where it was.
2. **`MeasureText.sidesSay` and its key went too** — the four-corner closing check's sentence (Q1), which carried the
   photo's `≈` figure and so cannot be said under §18. Q2 needs a sentence of its own for that check; none is written.
3. **A locked item cannot be held, so its padlock is not in a row.** Unlocking is the long-press menu's Unlock (as the
   held row needs a held item, Pick row 17). One tap more than 0.3.345 for a locked mark. **Reversed by the review
   (§22, finding 1): the picked item's row is drawn held or not, and a locked item's padlock is in it again.**
4. **A double tap is still two taps** until I4 brings the zoom (Pick row 13 is built and not called).
5. **A NEW capture's Save says the autosave's line**, not "writing its pictures…": it writes none (`SaveStay.saved(…,
   pictures = false)`), and a line waiting for an answer that never comes would stay for ever. **Amended by the review
   (§22, finding 2): it now says "Survey prep saved — the capture is filed when you go back".**
6. **The haptics are played as pulses** (`Landing.pulses`: one light tick, two light ticks 140 ms apart, one strong
   buzz), because Compose's haptic API has only two kinds; the double is a second light tick, not a distinct effect.
7. **The tests that pinned `:app` lines were moved, not loosened**: every source pin that named a deleted line now
   names the line that replaced it (`PhaseToolsTest`, `BodyMoveTest`, `PaletteTest`, `MarkSheetTest`, `QuadMeetsF1Test`,
   `FeaturesTest`, `DeleteGuardTest`, `MastersTest`), and `DESIGN-quad` §10 note 6 says that `sayCheck`'s two lines are
   gone and that the plates are now `PhotoFigures.layout`.
8. Carried from I1c (§21.3) unchanged: "lines set" means placed by hand AND closing (an older capture marked without
   ever touching its guessed lines opens on 1 · Lines once); the NEW-capture flow above; `Σ` kept on a chain total;
   the held mark's height plate turned; plates kept apart by their turned rectangles.

### 20.4 Three things for Amit to look at first, on the phone

1. **Open a wall whose lines are set, and one whose are not** — the first opens on 2 · Mark, the second on 1 · Lines —
   then press ✓ Save on a kept capture: it stays, and the bar says the pictures were written.
2. **Unpicked marks show every READ figure as a number along its line, and nothing else** — no code on a Line, no `≈`,
   no `mm`; slanting Lines' figures lie along them. Check that plates on a busy wall do not overlap.
3. **Laser walk**: tap a window, press the meter's button — one tick, the gold `▶ ?` moves to the next figure on its
   own, and the thumb resting on the photo moves nothing; then a bad reading — two ticks, `!`, the gold stays.

## 21. Amit's five points from 0.3.345 (23 Nov 2026)

Build 0.3.345 is 0.3.341's screen (1 · Lines, 2 · Mark, 3 · Measure, the measure bar, photo estimates) on Play. Amit
used it and sent five points. **They override §18 rule 2 and anything else they touch.** Slice I1c (this section) is
their PURE half, in `:pano` with tests, added on top of I1a (§19) and still changing nothing `:app` calls; I1b
switches the screen over to I1a's and I1c's rules together.

### 21.1 His words, verbatim

- **(P1)** "every time i open a foto, it always start from 1 - lines. its annoying. once lines are set, I dont want this
  as default screen but i should be able to change line later on as well if required."
- **(P2)** "Click on save always take me out to room foto. its annoying."
- **(P3)** "When a mark type window is selected, then only its different  measurements are visible. its also annoying. i
  want to see all measurements in its units always visible."
- **(P4)** "Measurements on any mark type alwyas show FM1 , 2 etc. it cosumes lot of space on line."
- **(P5)** "no way to set alignment of measurements  to line direction.  its always  horizontal which overlaps on other
  measurements."

### 21.2 Claude's reading

- **P1 — open on Mark once the lines are set.** Survey prep opens a face on 2 · Mark when that face's four lines are set,
  and on 1 · Lines only when they are not; the same rule when he switches face tab; 1 · Lines stays one tap away on the
  strip whenever he wants to change a line.
- **P2 — ✓ Save saves and stays.** Everything a save does today happens — the read-back save, the as-is upload queued,
  the room size kept, the saved survey pictures rewritten, the result said — but survey prep stays open on the same face,
  step and zoom, and the bar's saved state says it was saved; only ← leaves. Save's rule (all six surfaces bounded) is
  unchanged. Every effect happens exactly once a press; ← after a save neither loses nor repeats anything.
- **P3 — every read figure of every mark, always.** Every mark shows every figure that has been read — its size AND its
  position (from the corner, from the floor or ceiling, a Beam's drop, the `⊥` depth) — picked or not. This amends §18
  rule 2 (which showed a mark that is not picked only its read SIZE) and keeps §18's heart: no photo estimate anywhere,
  so an unread figure shows NOTHING on a mark that is not picked, and a quiet `?` only on the picked mark; the one being
  measured stays H1's gold `▶ ?`. A Line's, area's and chain's read figure likewise always. There is no `all` button.
- **P4 — a figure's label is the number alone.** A Line's, an area's, a Tape's and a chain's label drops its code
  (`F-M1`, `F-T1`, `FK1`) and its unit (`mm`): `1234`, not `F-M1 1234 mm`. An area's m² is not shown (§18 rule 3); its
  read perimeter is the number alone. The item's code is shown only where the item is NAMED — the picked item's own
  label (its row in I1, its chip from I2), never on the line. A mark's plates already carry the number alone. A mark's
  CODE (FW1) on its box stays: it names the mark, it is no measurement.
- **P5 — every figure runs along its dimension.** A Line's and a Tape's label is turned to the line's own angle, centred
  on it and breaking it, as ImageMeter draws a length — never upside down (folded into −90° … +90°; a line within a few
  degrees of upright reads bottom to top, as a turned height plate does); a mark's width plate lies level on its width
  line and its height plate turned up its height line; a position figure along its own line; `DimLayout` places every
  plate by its TURNED box, so two turned labels do not overlap, and a label is moved off its line on a leader only when
  there is no room along it. No per-item alignment setting in this slice (ImageMeter keeps one per element; if Amit asks,
  it belongs in the slide-up panel, I2).

### 21.3 Where the code showed a reading to be wrong, and what was built instead

1. **P1: "the squared frame exists" is NOT "the lines are set".** A fresh capture's four lines are the phone's own guess
   (`SurveyPrep.fresh`) and they already close — `SurveyPrep.frameOf` answers a frame on every face of a capture nobody
   has touched — so "open on Mark when the frame exists" would open every fresh capture on 2 · Mark before a single line
   was placed: the opposite of "once lines are set". **Built instead:** a face's lines are set when all four are PLACED
   by hand (`Surface.done()`, the same "bounded" ✓ Save counts) AND they close (`frameOf`): `PhaseTools.linesSet`. The
   cost, said plainly: a face of an older capture marked without ever touching its guessed lines opens on 1 · Lines
   until its four lines are placed once; 2 · Mark is still one tap away.
2. **P2 on a NEW capture.** Survey prep has two callers: a kept capture's (`MainActivity.kt:546`, its `onDone` keeps the
   room size, rewrites the pictures and closes) and a NEW capture's, straight after the shutter (`MainActivity.kt:4053`),
   whose ✓ Save and ← BOTH call `keepWith` → `onKeep`, which files the capture (its faces, `FaceWriter.commit`, and its
   row, `store.insert`) once. Staying there cannot run the keep on every press (it would file the capture twice), and
   it cannot queue the upload at the press either: the upload worker refuses a capture whose row does not exist after
   five tries (`AsIsUpload.kt:388-393`, "waiting to be filed", then "capture gone"), and staying is exactly what would
   outlast them. **Built instead:** `SaveStay.Flow.NEW` — a press there is the read-back save alone; the keep stays on ←,
   and ← then queues the upload the press held back, once (`SaveStay.Leave.then`). Its room size and pictures are the
   keep's own, as today. **SUPERSEDED by port 63 P2N (§23.1):** the keep is split — one function both the
   press and ← call, one at a time — so the press FILES the capture, once, and stays.
3. **P4 on a chain's total:** `Σ` is kept (`Σ 3310`) — it is no code and no unit, and without it the total under the last
   point reads as the last segment's figure — and the word ` laser` after a laser total goes with the code: it cost the
   line its room. Chains are off (`Features.CHAINS = false`); this reaches only a room that already has one.
4. **P5 on the held mark:** "the height plate turned up its height line (as built)" — as built, the HELD mark's
   hand-anchored height plate (the one kept clear of its tabs) was UPRIGHT. It is now turned up too, from the same left
   edge and the same middle height, so every height reads the same way; its width stays level where it was.
5. **P5, the turned box: the bounding box is not enough at the phone's numbers.** A 48 × 19 dp plate turned 60° has a
   41 × 51 dp bounding box, so two labels on two close, slanting Lines whose plates lie clear of each other would be
   pushed apart by their boxes. The layout keeps the TURNED RECTANGLES apart (`DimLayout.Turned.hits`, separating axes);
   for a level or an upright plate the rectangle IS its box, so a mark's plates are kept apart exactly as before
   (`TurnedLabelsTest` pins both, and that `hits` equals the old box test on a grid of level boxes).

### 21.4 What I1c built, function by function, and the `:app` call site each will replace in I1b

Line numbers are `3c377c3`'s (= this commit's: no `:app` file changed). `SPS` = `SurveyPrepScreen.kt`, `SPD` =
`SurveyPrepDraw.kt`, `SP360` = `SurveyPrep360.kt`, `MA` = `MainActivity.kt`, all under `APP/`.

**P1 — the step a face opens on (`PhaseTools`; `OpenStepTest`, 7 tests)**

| Function | Signature | Replaces in `:app` |
|---|---|---|
| `PhaseTools.linesSet` | `(prep, s, l, w, h): Boolean` — all four placed (`Surface.done`) AND `frameOf` closes; false off a face | new; asked by the two below |
| `PhaseTools.openedStep` | `(tab, stage, linesSet): String` | the initial `modeSet = "lines"`, `SPS:285` |
| `PhaseTools.openStep` | `(tab, from, stage, linesSet): String` — the Plan keeps `from`; on 3 · 360 a face tab keeps 360; else Mark when set, Lines when not, through `routeMode` | `showTab`, `SPS:1272-1287`: `modeSet = PhaseTools.openStep(t, mode, quoteStage, linesSet(t))` after `Pick.tab` (whose row 26 keeps the open face's own tab no change) |

**P2 — ✓ Save saves and stays (`SaveStay`, new file; `SaveStayTest`, 6 tests)**

| Function | Signature | Replaces in `:app` |
|---|---|---|
| `SaveStay.may`, `Effect`, `Flow`, `effects` | `may(bounded)`; `Effect { SAVE, UPLOAD, KEEP_SIZE, PICTURES }`; `Flow { KEPT, NEW }`; `effects(bounded, readBack: Boolean?, flow = KEPT): List<Effect>` | `saveNow`, `SPS:1445-1457`: the read-back save, `AsIsUpload.enqueue`, then a NEW callback in `onDone`'s place (`SPS:1456`) that runs `MA:546-565`'s body WITHOUT `surveyKept = null` — `keepRoomSize`, `FaceWriter.rewriteSurvey` and the result — and reports the result back; `onDone` survives only for the NEW flow's ← (`MA:4053-4054`) |
| `SaveStay.Mem`, `OPENED`, `saved`, `written`, `notWritten` | `Mem(at, readBack, pictures, written, why, seq)`; `saved(m, rev, readBack)`; `written(m, seq, n)`; `notWritten(m, seq, why)` — an older press's answer changes nothing | new: one `remember`ed `Mem` beside `savedMsg` (`SPS:319`); `MA`'s `lastResult` lines at `:552`, `:560-561` become the callback's report |
| `SaveStay.bar` | `(tx, m, rev, auto: Msg): String` — the save's own words until the next change, then the autosave's line | `Text(savedMsg(tx) …)`, `SPS:1475` (the keys are the room screen's own: `app.msg.survey_prep_saved_writing`, `…_saved_x`, `…_saved_not_written_x`, `sp.msg.not_saved_did_not`) |
| `SaveStay.changedSince`, `leave`, `Leave` | `(m, rev)`; `leave(m, rev, flow = KEPT): Leave(repeats, then, changedSince)` | `back()`, `SPS:1443`: ← still calls `onCancel`; for `Flow.NEW` it then queues `then` (the held-back upload) after the keep |

I1b must also SERIALISE the picture rewrite (two presses must not run `FaceWriter.rewriteSurvey` on one folder at once —
a `Mutex` in `MA`); `SaveStay` shows only the newest press's answer, and says nothing about the files.

**P3 — every read figure, always (`PhotoFigures`, `DimLayout`; `PhotoFiguresTest` rewritten, 19 tests)**

| Function | Change | Replaces in `:app` |
|---|---|---|
| `PhotoFigures.ofMark` | an unpicked mark shows every READ figure — size and position, the `⊥` depth included — and nothing for an unread one; the picked mark every figure (`?` unread), the gold one gold | as I1a's §19.1 row: the painter's `laid`, `SPD:942-948` |
| `DimLayout.markShown` | the same, through `ofMark` | `DimLayout.mark`, `SPD:947` |

`PhotoFiguresTest` now walks every mark type on a wall, the floor and the ceiling (`SurveyPrep.TYPES` and
`MarkTypes.typesOn`, 40-odd types): nothing read → nothing unpicked and every figure `?` picked; every other figure read
→ exactly those unpicked, each READ; an unread figure made gold → shown gold; everything read → everything, picked or
not, and never an estimate. §18's "no figure of any mark, in any state, reads an estimate" is kept as it was.

**P4 — the number alone (`SurveyMeas`, `SurveyChain`, `PhotoFigures`)**

| Function | Change | Replaces in `:app` |
|---|---|---|
| `SurveyMeas.shownTag` | `1240` read; `?` asked; else `""` (was `F-M1 1240 mm` / `F-M1 ?` / `F-M1`) | under `PhotoFigures.line`, as §19.1 |
| `SurveyMeas.tapeShown` (new) | `(len): String` — `1234` | `SurveyMeas.tapeTag`, `SPD:813` and `SP360:1125` |
| `SurveyChain.totalShown` | `(v, ask)` (no `tx`, no `code`): `Σ 3310` read or the laser's; `Σ ?` asked; else `""` | under `PhotoFigures.chainTotal` |
| `PhotoFigures.line`, `tape` (new), `chainTotal`, `line360`, `chainTotal360` | the number alone; `chainTotal(c, v, pick, ready)` and `chainTotal360(v)` lose their `tx` | `SPD:747-757`, `SPD:813`, `SPD:896-906`; `SP360:1080-1083`, `1125`, `1186` |
| `PhotoFigures.nameOf` (new) | `(prep, pick): String?` — the picked item's code | the held item's row label (`Pick.row`), `SPS:1891`, `1929`; the code chip from I2 |

**P5 — every figure along its dimension (`LabelAngle`, new; `DimLayout`; `TurnedLabelsTest`, 11 tests)**

| Function | Signature | Replaces in `:app` |
|---|---|---|
| `LabelAngle.of`, `fold`, `upright`, `along`, `across`, `NEAR_UPRIGHT_DEG = 5`, `LEVEL = 0`, `UP = -90` | `of(x1, y1, x2, y2): Double` degrees clockwise on the canvas, folded into (−95, 85]; exact 0 and −90 for a level and an upright line | the painter's `c.rotate(-90f, cx, cy)` in `plate`, `SPD:1028`, becomes `c.rotate(p.angle.toFloat(), cx, cy)` |
| `DimLayout.Turned` | `(cx, cy, hw, hh, deg)`: `corners()`, `bounds`, `hits(o, apart)`, `nearest(x, y)`, `Turned.of(box)` | the plate's outline in `plate`, `SPD:1022-1049` (drawn upright about its middle at `pw` × `ph`, then turned) |
| `DimLayout.Plate.angle`, `pw`, `ph`, `rect` | new, with defaults: an old `place` plate reads `angle` 0 or −90 as before | `val w = if (p.turned) b.h else b.w`, `SPD:1026` → `p.pw`, `p.ph` |
| `DimLayout.placeShown` | every plate turned to its line (`turnOf`) and kept apart by its turned rectangle; on the line, ¼ / ¾ along, then off it on a leader square to the line | `DimLayout.place`, `SPD:950`, as §19.1 |
| `DimLayout.turnOf` | `(f: Fig): Double` | — |
| `DimLayout.item` (new), `MarkDims.Role.ITEM` (new), `Fig.taps` (new, default true) | `item(id, key, text, from, to?, picked, gold, measured, dp, textW): Mark?` — a Tape's, Line's or chain segment's figure along its line, an area's or a chain total's at its point; a Tape's is no tap target | the label and `figureTag` of `line` (`SPD:745-757`), of `tape` (`SPD:813-820`, `SurveyMeas.tapeTagAt`) and of `chain` (`SPD:883-907`) |
| `PhotoFigures.items` (new), `PhotoFigures.layout` | `items(prep, s, a, b, pick, toPx, dp, textW, ready)`: every Line, area, Tape and chain figure of the face; `layout` now returns the marks THEN the items, one list for one `placeShown` | the same; the tap targets they add (`o.tags`, `SPD:772`) come from `Out.tags`, which skips a Tape's |

`DimLayout.place` (the old path the screen still calls) is unchanged: `lay(…, slant = false)` keeps every box, test and
answer it had; its tests are untouched but for one line (§21.5).

### 21.5 Tests changed, not only added

- `PhotoFiguresTest` — rewritten for P3 and P4, as above (16 → 19 tests).
- `DimLayoutTest` — the set of `MarkDims.Role` values gains `ITEM`; I1a's "the old path and the new one agree on the
  hand anchor" now agrees on the width exactly and on the height's left edge and middle, the height stood up.
- `:pano` after I1c: 109 test classes, 1,549 tests, 0 failures (after I1a: 106 and 1,522).

### 21.6 Texts

No key is added, changed or deleted. P2's bar reuses the room screen's own four lines. `mr-pending.tsv` is
unchanged, `unkeyed.tsv` stays empty, and the literal ratchet entered the two new files at 0.

## 22. The review of I1 (23 Nov 2026)

Two reviewers read slice I1 (`2b0f7bf` I1a, `4311e1e` I1c, `dc80b56` I1b) and reported six findings. (This is the
section the review task calls "§20, The review of I1"; §20 was already I1b as built, so it is §22 here.) Each was
checked in the code before anything was changed. **All six are CONFIRMED; none is declined.** One (finding 2) is fixed
only in its minimal half, and the other half is said plainly below. `:pano` after the fixes: 108 test classes, 1,525
tests, 0 failures (before: 107 and 1,518 — `OnFaceTest` back with its 4, and 3 new tests). `:app` is compiled only by
CI; nothing has been seen on a phone.

### 22.1 The findings, the verdicts and the fixes

1. **The Mark sheet could not be opened for an item that cannot be held — CONFIRMED (major).** `Pick.row` answered null
   unless the item was HELD, and `Pick.mayHold` is false for every mark at Estimation and Client approved, a surveyed
   mark at the Wish list, Vendor review and Revisions, and every locked item; the long-press menu has no sheet row. So
   at the Wish list a tapped surveyed window had no row, and its Mark sheet — the only place `→ Client wish` and the
   acts are — could not be reached. 0.3.345 drew the picked mark's row for ANY picked mark ("the label stays, and still
   opens its Mark sheet"). **Fixed:** `Pick.row` answers a row for any PICKED item; its nudges only while it is held
   (`mark && held && mayHold` — only a held item moves); its padlock where the phase has one (`PhaseTools.lockOk`),
   held or not. `StepRoute.underMark(toolRow, row, keypad)` (the parameter renamed from `held`) gains `Under.ROW`: the
   picked item's row ALONE where the phase has no tool row (Estimation, Client approved), 48 dp in portrait, 34 in
   landscape; `markPanel` draws the row for `HELD` and `ROW`. Tests: `PickTest` "a picked item that cannot be held
   still has its row" (a surveyed window at the Wish list, every mark at Estimation and Client approved with
   `Under.ROW`, a locked mark with its padlock); `OneStepTest` and `MeasureScreenTest` moved with it. **This reverses
   §20.3 departure 3**: a locked item's padlock is in its row again, as in 0.3.345 (a tap on it is `Pick.locked`, row 17:
   picked, not held).
2. **A NEW capture's ✓ Save did nothing he could see and filed nothing until ← — CONFIRMED (major).** `SaveStay.bar`
   answered the autosave's own line for a press that writes no pictures, so the bar read exactly as before the press.
   **Fixed, minimally:** such a press is `SaveStay.Pictures.ON_LEAVE`, and the bar says **"Survey prep saved — the
   capture is filed when you go back"** (`sp.msg.saved_filed_on_back`, a new key, English first and listed in
   `mr-pending.tsv`) until the next change, then the autosave's line. `SaveStayTest` pins it. **NOT BUILT, AND SAID
   PLAINLY: filing the capture on the first press.** P2 asks that every effect of a save happen on the press; on a new
   capture the filing (`FaceWriter.commit` + `store.insert`), the upload and the pictures still wait for ←. Doing them
   on the press means splitting `NewCaptureSurvey`'s `onKeep` — which files on a background coroutine and then clears
   `pending`, closing the dialog — into a keep-once that stays, after which survey prep must switch to the KEPT flow
   whose `onSaved` needs the capture's `CaptureStore` row (its `panoPath`, `deviceId`, `room`, `stage`) that exists only
   once that coroutine has returned. Built blind (MainActivity is not even type-checked here), a wrong order files the
   capture twice or rewrites pictures for a row not yet inserted. It is the next S2 change to make for P2, with
   MainActivity under a compiler. **BUILT SINCE, as port 63 P2N: §23.1.** The first ✓ Save files the capture on the
   press, once, and stays; the line "…the capture is filed when you go back" and its key are gone.
3. **A read figure with no free spot became a 6 dp dot — CONFIRMED (minor).** In `DimLayout.lay` only `Rank.ACTIVE` was
   forced onto a plate; a read figure on an unpicked mark (`Rank.MEASURED`) was the first to lose its number, and P3
   now hands the layout every read figure of every mark. **Fixed:** on the one step (`slant`) a READ figure is never a
   dot: with no free spot it steps further off on a longer leader (`farCands`: 3 to 6 plate heights out, at ½, ¼ and ¾
   along, either side, turned along its line; a figure with no line 2 to 5 plate heights round its point), and with
   none free there either it takes its first spot, as the gold one does. A read compact pair steps out the same way.
   An unread `?` may still be a dot. Test: `DimLayoutTest` "a read figure is never a dot on the one step" — a door, a
   window beside it, a second window over it and a point, every figure read, nothing picked, on the round canvas and
   at the phone's numbers — red before the fix.
4. **`OnFaceTest` was deleted though `SurveyPrep.onFace` and `placeMiss` are live — CONFIRMED (minor).** I1b mistook it
   for a test of the deleted `MeasureFlow.onFace`. **Fixed:** restored unchanged (4 tests).
5. **After Survey the picked mark still showed `?` that cannot be acted on — CONFIRMED (minor).** `PhotoFigures.ofMark`
   had no stage. **Fixed:** `ofMark`, `onFace`, `layout`, `items`, `DimLayout.markShown` take `ask` (default true), and
   `line`, `chainSeg`, `chainTotal` take `asks`; false, the picked item shows its READ figures only and no `?`. The
   painter's `Marks` carries `ask`, which survey prep sets to `MeasureFlow.canRead(quoteStage)`. Test: `PhotoFiguresTest`
   "after Survey the picked item asks nothing" at every stage past Survey, for a mark held and not, a Line and a chain.
6. **A Line, Tape or chain picked but not held was named nowhere — CONFIRMED (minor).** **Fixed by finding 1's change:**
   the picked item's row is drawn held or not, and for a Line, an area, a Tape or a chain it carries its code
   (`PhotoFigures.nameOf`). Pinned in the new `PickTest` (a Line the walk reached has a row and is named `F-M1`).

### 22.2 What this changes on the phone, against §20.1

- Under the photo in 2 · Mark: the PICKED item's row (not only the held one's) over the tool row — so a mark the walk
  reached keeps its row while he lasers along the wall, and the photo does not change height at the first reading; at
  Estimation and Client approved the picked item's row stands alone.
- The nudges are still only on a HELD mark; the row still has no value and no Delete.
- After Survey no `?` is drawn anywhere.
- On a crowded wall every reading is a number on the photo, some on longer leaders.
- ✓ Save on a new capture says that it was saved and that ← files it.

### 22.3 Texts

One key added: `sp.msg.saved_filed_on_back` (English as its Marathi value, listed in `mr-pending.tsv`, now 25 keys).
1,932 keys; `unkeyed.tsv` stays empty; Tx regenerated, the ratchet lowered nothing.

### 22.4 Where to look first if CI's `:app` compile is red

1. `SurveyPrepScreen.kt` `markPanel`: `StepRoute.underMark(toolRow, row = row != null, keypad = …)` (the named argument
   is now `row`) and `under == StepRoute.Under.HELD || under == StepRoute.Under.ROW`.
2. `SurveyPrepScreen.kt`, the editing photo's `SurveyPrepDraw.Marks(…, ring = ring, ask = MeasureFlow.canRead(quoteStage))`.
3. `SurveyPrepDraw.kt`: the new last `Marks` field `ask: Boolean = true`, and
   `PhotoFigures.layout(…, o.live, o.ready, ask = o.ask)`.

## 23. The builds after I1 (23 Nov 2026)

Slice I1 shipped as Play 0.3.348 (`main` `494c882`, 14:37 IST, 23 Nov 2026). Each build after it is recorded here as it
is made, one subsection each.

### 23.1 P2N — ✓ Save on a brand-new capture files it on the press, and stays

**Why.** Amit, 23 Nov 2026, on 0.3.345: "Click on save always take me out to room foto. its annoying." (P2, §21). I1
answered it for a capture opened from its room. On a BRAND-NEW capture — survey prep straight after the shutter — the
review of I1 (§22, finding 2) left it half done: ✓ Save was the read-back save alone, the bar said "Survey prep saved —
the capture is filed when you go back", and the capture got its row, its six faces, its pictures and its upload only
when he pressed ←. P2N is the other half. Branch `port63-i2`, over `494c882`. `:app` is compiled only by CI; nothing has
been seen on a phone.

**What the phone does now, on a new capture (all six surfaces bounded, as ✓ Save has always needed).**

| He does | The phone does | He sees |
|---|---|---|
| The FIRST ✓ Save | the read-back save; then the capture is FILED, once — its six faces and survey pictures into the room folder (`FaceWriter.commit`) and its row (`store.insert`), with the prep and the size as they stood at the press; then the as-is upload is queued, if the save read back | survey prep STAYS, same face, step and zoom; the bar says "Survey prep saved — writing its pictures…", then "Survey prep saved · N pictures in the room folder" (N: the six faces and every survey picture); `Discard this 360` and the "New 360 — ← keeps it…" note leave the ⋮ menu |
| ✓ Save again WHILE that filing is still running | the read-back save at once; the rest waits its turn behind the filing, and by then the capture is filed, so it is a kept capture's press: the size kept, the pictures rewritten from the prep as it stood at THIS press. Nothing is filed a second time | "…writing its pictures…" until the newest press is answered |
| ✓ Save after the filing | exactly a kept capture's press (§20.1): the save, the upload, the size, the pictures rewritten | as on a capture opened from its room |
| ← before any ✓ Save | what ← has always done on a new capture: it files it, once, with the prep as it stands; no upload is queued (none ever was without a Save) | the room, "Saving six faces…", then "N pictures saved to …" |
| ← after a ✓ Save that filed | the size kept on its row, as ← does on a kept capture. Nothing is filed again, no picture is written, no upload repeated | the room, and the last Save's result line |
| ← while a ✓ Save is still filing | ←'s own turn waits behind the filing and then keeps the size; the upload that press held back is queued by ← (the upload worker waits for the row, as it always has after ←), and the press's late answer queues no second one | the room, "Saving six faces…" until the filing is done |
| The filing FAILS (the disk is full, say) | nothing was filed: the capture is still a new one. The next ✓ Save, or ←, files it — a failed filing is retried, never repeated | "Survey prep saved on this phone; pictures not written: <why>"; `Discard this 360` is offered again |

**The order of effects is `:pano`'s (`SaveStay`); `:app` only asks and does.** Three places run a save, and each asks:

| Where | Asks | Answer |
|---|---|---|
| The screen, at the press (`SurveyPrepScreen.saveNow`) | `SaveStay.effects(done, readBack, SaveStay.flow(saveMem))` | KEPT: `SAVE`, `UPLOAD` when it read back, `KEEP_SIZE`, `PICTURES` (unchanged). NEW: `SAVE`, `FILE` — the press is handed to the caller's keep, and NO upload is queued yet: there is no row to upload from. `flow` is asked at EVERY press, because it changes under the open screen, once |
| The caller's keep, ONE AT A TIME, for a press and for ← alike (`MainActivity`'s `keepNew`, under `NewKeep.lock`) | `SaveStay.keep(filed = the row exists NOW, leaving)` | not filed: `FILE`. Filed, a press: `KEEP_SIZE`, `PICTURES`. Filed, ←: `KEEP_SIZE`. **This is the one place that decides to file, and it can only while there is no row** — so two presses, a press and a ←, or a ← during a filing can never file twice |
| The screen, at the keep's answer (the `report` callback) | `SaveStay.answered(saveMem, seq, n, why)` | the bar shows the NEWEST press's answer alone; the capture is FILED from the first answer that carries pictures (a filing writes them; a rewrite needs its row), whichever press it answers, and never un-filed; the upload the last press held back is queued at the answer that files, once (`Answer.then`) |
| ← (`SurveyPrepScreen.back`) | `SaveStay.leave(saveMem, dirty)`, then `SaveStay.left` | nothing of a save is repeated; an upload still held is queued by ←, once, and forgotten, so a late answer queues none |

`SaveStay.Mem` gained what the screen must remember for this: `filed` (true from the start for a kept capture:
`SaveStay.opened(filed)`), `keeps` (presses handed to the keep and not answered yet) and `held` (an upload is held
back). `SaveStay.filing` reads them as `NOT_FILED` / `FILING` / `FILED`, and `SaveStay.mayDiscard` is `NOT_FILED` alone:
`Discard this 360` deletes the 360 file, which a filed capture's row points at and a filing is reading. New, too:
`Effect.FILE`, `toKeep`, `Answer`. Gone: `Pictures.ON_LEAVE`, `saved`'s `pictures` parameter, `leave`'s `flow` parameter.

**What changed in `:app`.**

- `SurveyPrepScreen.kt` — `SurveyPrepDialog` takes `filed: Boolean` (no default) and a non-null `onSaved`; `saveMem`
  opens as `SaveStay.opened(filed)`; `saveNow` and `back` as the table above; the two `onDiscard` rows of the ⋮ menu are
  drawn only while `SaveStay.mayDiscard(saveMem)`. The `val flow` that read `onSaved == null` is gone.
- `MainActivity.kt` — the kept capture's call passes `filed = true` and is otherwise untouched. `NewCaptureSurvey` takes
  `onSaved` and `onKeep(prep, size)` and passes `filed = false`; its `keepWith` is gone. At its call site the old `onKeep`
  body became `keepNew(p, size, leaving, report)`: a plain `scope.launch` (the composition's scope, so the turns are
  taken on the main thread in the order asked — a `Mutex` is first come, first served), `newKeep.lock.withLock`, then
  `SaveStay.keep`, with the two slow steps (`FaceWriter.commit` + `store.insert`; `FaceWriter.rewriteSurvey`) inside
  `withContext(Dispatchers.Default)`. The session takes the call's prep and size UNDER the lock, just before the faces
  are written, so a later press cannot change it under a filing. `NewKeep` (the lock, and the row once filed) is
  `remember`ed per pending capture. A press does not clear `pending`; ← does.

**Decided here, where the brief was silent.**

1. **A new capture is filed whatever the read-back save answered** — ← has always filed one whose prep did not read
   back, and a press that filed nothing in that case would bring back "Save did nothing". Only the UPLOAD needs the
   read-back, as everywhere.
2. **One upload for however many presses were held back, and the LAST press decides it** (the rule I1 already had for
   ←): it is queued once the row exists and carries the prep as last saved; if the last held press did not read back,
   none is queued — the web never gets a prep this phone does not hold.
3. **"Filed" is read from the answer itself** — pictures written means the row exists — so `report` kept its shape
   (`(Int?, String?)`) and the kept capture's `onSaved` is byte for byte what shipped in 0.3.348.
4. **A press made during the filing is not dropped and not merged: it is queued**, and becomes a kept capture's press.
   Dropping it would lose the changes made between the two presses from the pictures.
5. **A change made after the last ✓ Save, then ←:** it is kept on the phone and goes to the pictures and the web with
   the next ✓ Save — exactly as on a capture opened from its room (`Leave.changedSince`). Said plainly, because before
   P2N ← on a new capture always wrote the pictures from the prep as it stood: now it does so only when ← itself files.
6. **`Discard this 360` goes from the menu the moment a press is filing**, and comes back only if that filing failed.
7. **The room screen's "Saving six faces…" is shown only by a ← that has something to wait for** (the capture not filed
   yet), and that ← clears only the line it set. A press shows nothing on the room screen: the bar says it.
8. **The first press's count is the filing's own** (`FaceWriter.Result.faceCount`: the faces and the survey pictures),
   in the kept capture's sentence; the room screen's result line stays "N pictures saved to …", as ← always said.
9. **Not changed:** a filing is `FaceWriter.commit` and `store.insert` as one step, as it was. If the faces were written
   and the insert then failed, the retry writes the faces again (the gallery then holds both sets). Before P2N a
   filing could fail only under ←, after the dialog had closed, with no retry at all; a press that fails can now be
   pressed again. The double set is not fixed here.
10. **The filing now runs with survey prep still open.** A kept capture's ✓ Save already decodes the full 360 and
   writes its survey pictures under the open screen (0.3.348), so the filing adds the six full-size faces, one at a
   time, to that. On the Mi A3 the first ✓ Save of a new capture may take as long as ← used to; the bar says
   "writing its pictures…" throughout. To be watched on the phone, not measured here.

**Texts.** No key added. `sp.msg.saved_filed_on_back` lost its last reader and is deleted from both tables, from
`mr-pending.tsv` (24 keys) and from the regenerated `Tx`; 1,931 keys; `unkeyed.tsv` stays empty; the ratchet lowered
nothing. The bar reuses the kept capture's three lines.

**Tests.** `SaveStayTest` (7 → 12): a `Phone` that runs the three places exactly as `:app` does, every effect written
down as it runs — the first press on a new capture (and one that did not read back, and a filing that fails); a second
and a third press during the filing; a press after the filing, equal effect for effect to a kept capture's; ← before
any press; ← after a press — answered, still filing, failed, and failing under ←; the screen's memory (an older answer
shows nothing and still files; a failed rewrite never un-files; `left`); and EVERY order of up to seven events (a press
that reads back, one that does not, a change, one turn of the keep, ←) with 0, 1 or 2 failing filings — 292,968 runs:
at most one filing, nothing after it files, no rewrite or kept size before the row, no upload queued at a press or an
answer without a row, the last read-back press always uploaded once the capture is filed, every press answered.
`MeasureScreenTest` (14 → 15) reads the two sources: the screen asks `flow` at every press and never `keep`;
`FaceWriter.commit(` occurs once in the app, with `store.insert(row)` and `newKeep.row = row` inside the `FILE` branch,
in that order, under `newKeep.lock.withLock`, launched by a plain `scope.launch`; `pending = null` only on leaving.
`CaptureFilingTest`'s pin on the kept 360's row moved with the line (`work = ps.work,`). `:pano`: 108 test classes,
1,531 tests, 0 failures, 0 errors (0.3.348: 108 and 1,525).

**Type-checked here, and what that cannot prove.** kotlinc 2.0.21 from the Gradle cache, the real `:pano` classes and
the hand-written stand-ins of §20 (`scratchpad/i2-typecheck/`): `SurveyPrepScreen.kt` whole, with its seven companions,
has NO error (the 235 left are the same stub gaps in the five files this build did not touch, count for count); and
`MainActivity.kt`'s changed blocks — the `pending` block with `keepNew`, `NewKeep`, `NewCaptureSurvey`, the kept
capture's block and `keepRoomSize`, 241 lines cut VERBATIM by a script — compile clean in an extract beside the real
`SurveyPrepScreen.kt`, so both calls of `SurveyPrepDialog` are checked against its real signature. Errors planted in
each (a wrong name, swapped arguments, a missing `filed`, a lost null check) were all reported. Every name each added
`:app` line uses was also checked by hand to be in scope where it is written: in `SurveyPrepDialog`, its parameters
`filed`, `onSaved`, `onCancel`, `onDiscard` and its own `saveMem`, `dirty`, `done`, `prep`, `roomSize`, `webState`,
`context`, `id`; in `AppScreenBody`, `pending`, `busy`, `lastResult`, `surveyTick`, `scope`, `store`, `context`, `tx`,
`keepRoomSize`, `refreshQueue` (all declared at the function's top level, above the block) and the block's own `ps`,
`newKeep`; `SaveStay` is a NEW import in `MainActivity.kt`. **Not proven:** that the stand-ins of `CaptureStore.Capture`,
`FaceWriter` (`Session`, `commit`, `rewriteSurvey`, `Result`), `SyncWorker`, `SurveyPrepStore`, `QuoteStageSync` and
`WishSync` have the real shapes (copied by reading); anything the Compose compiler plugin or Android lint adds; the
rest of `MainActivity.kt`; and everything about timing on a real phone — that the composition's scope runs the turns
on the main thread in order is read from Compose's contract, not run.

**Where to look first if CI's `:app` compile is red.**

1. `MainActivity.kt`, `import com.malletcrafts.interiorestimate.pano.SaveStay` (new), and in `keepNew` the last
   statement of `newKeep.lock.withLock { … }`, an `if` with no `else`.
2. `MainActivity.kt`, `NewCaptureSurvey(pending = ps, onDiscard = …, onSaved = …, onKeep = …)` against its new
   parameters, and inside it `filed = false, onSaved = onSaved, onCancel = { p, size -> onKeep(p, size) }`.
3. `SurveyPrepScreen.kt`, `onSaved(prep.copy(), roomSize.copyOf()) { n, why -> … }` (no `?.invoke` any more) and
   `SaveStay.leave(saveMem, dirty)` (two arguments).

**Three things for Amit to look at first, on the phone.**

1. Shoot a new 360, set all six surfaces' lines, press ✓ Save: it stays, the bar ends on "Survey prep saved · N
   pictures in the room folder", and the pictures are in the room folder while survey prep is still open.
2. Add a mark, ✓ Save again, then ←: the room shows ONE tile for the capture, its folder ONE set of six faces, and the
   survey pictures show the new mark.
3. A new 360, lines set, ✓ Save and ← at once: one tile, one set of faces, and "Saving six faces…" until it is done.

**Not built.** Saving a room from one surface (✓ Save still needs all six; port 63 J1); the saved pictures' "measured
figures only" (M3); a rewrite of the pictures on ← when something changed since the last Save (decision 5).

### 23.2 I2a — the panel's pure rules, beside what stands

Slice I2 ("the panel slides up; the standing rows go", §14) is built in two commits, as I1 was. **I2a (this one) adds
every pure rule I2 needs to `:pano`, with its tests, and changes NOTHING the screen does:** no function `:app` calls
today answers differently, and `:app` still draws the Mark sheet whole, the long-press menu, the floating trash and the
picked item's row. **I2b switches the screen over by calling what is listed here and deleting the old** (§13's "Deleted"
rows for I2). Branch `port63-i2`, over P2N (`7dacbbc`). `:app` is compiled only by CI; nothing has been seen on a phone.

**One thing had to touch `:app`, and it is two branches that cannot be reached yet.** `SurveyPrepTouch.Hit` is a sealed
class and `SurveyPrepScreen.kt` matches over it exhaustively in two places (`pressId`, `markGrab`), so the new
`Hit.Code` would not compile there without a branch each: `is SurveyPrepTouch.Hit.Code -> pickS.picked` and
`null, is SurveyPrepTouch.Hit.Tag, is SurveyPrepTouch.Hit.Code -> null`. `SurveyPrepTouch.hit` answers `Hit.Code` only
when it is handed a chip, and nothing hands it one until I2b.

Line numbers below are this commit's. `SPS` = `SurveyPrepScreen.kt`, `MS` = `MarkSheet.kt` (the composable),
`SPL` = `SurveyPrepLocks.kt`, `SPD` = `SurveyPrepDraw.kt`, `SP360` = `SurveyPrep360.kt`, all under `APP/`.

**The acts (`MarkSheet` in `:pano`; `MarkSheetPanelTest`, 16 tests)**

| New | Signature | Replaces in `:app` |
|---|---|---|
| `MarkSheet.acts`, `Acts`, `Cell`, `FINE` | `acts(tx, p, id, stage, picked, chains = Features.CHAINS, shape = false): Acts`; `Acts(line1, line2)` with `all`, `delete`, `rows`, `has(id)`; `Cell(id, text, aria, delete)` | the long-press menu's rows, `SurveyPrepEdit.menuItemsOf(tx, prep, c.id, quoteStage, asPicked)` at `SPS:2253`, drawn by `SpCtxMenu` (`SPL:123`, `SPS:2259`); the picked item's nudges and padlock in `markPanel` (`SPS:1929-2007`); the floating trash (`SPS:3364-3408`, `SP360:699-705`) |
| `MarkSheet.fineOk` | `(p, id, stage): Boolean` | `Pick.row(…).nudges` at `SPS:1933`, `1976`: where the nudges are offered |
| `MarkSheet.actStep`, `ActStep` | `actStep(tx, p, pn, act, stage, picked, chains, shape): ActStep` — `Refused(panel)` / `Delete` / `Run(act)` | `ctxPick`, `SPS:1212-1223` |
| `MarkSheet.actRuns`, `stepsToDrop` | `(o: Outcome): Boolean`; `(before, afterDone, afterAct): Int` | new: after `panelDone`, before and after `toggleLock` (`SPS:1108`), `duplicateMark` (`SPS:1180`), `lockAllOf` (`SPS:1163`) — the caller drops that many entries from the top of `hist` |

**The panel as a value**

| New | Signature | Replaces in `:app` |
|---|---|---|
| `MarkSheet.panelOpens` | `(p, id): Boolean` | `Pick.row(…).sheet` / `MarkSheet.opens` as "the code is a button", `SPS:1933`, `1967` |
| `MarkSheet.Panel`, `openPanel` | `Panel(id, draft: State?, openedAt, stateFirst, said)` with `with { … }`; `openPanel(p, id, now, longPress = false, focusMove = null, fresh = false, hist = 0): Panel?` | `var markSheet` (`SPS:351`) and `openSheet` (`SPS:889`); `longPressed`'s `ctx = SpCtx(…)` (`SPS:878`) becomes `openPanel(…, longPress = true)` when the finger lifts |
| `MarkSheet.answers`, `DEAF_MS` | `(pn, now): Boolean`; `300L` | new: every `MarkSheetActions` callback (`SPS:2287-2298`) and the sheet state's `confirmValueChange` (`MS:146`) ask it first |
| `MarkSheet.panel`, `PanelView`, `Head`, `optionsLabel` | `panel(tx, p, pn, stage, picked, chains, shape): PanelView?` — `pane, head, acts, said, body: View?, stateFirst` | `MarkSheet.view(tx, prep, st, quoteStage)` at `SPS:2285`; `MsHead` (`MS:218`: its ✕ goes, Done comes) and `MsDone` (`MS:446`, called at `MS:179` and `204`: deleted) |
| `MarkSheet.panelDone`, `panelCancel` | `(p, pn, stage, at, snapshot): Applied`; `(p, pn): Boolean` | `MarkSheet.apply` in `sheetDone` (`SPS:1029`); `MarkSheet.cancel` in `sheetCancel` (`SPS:1044`) |
| `MarkSheet.hasGrip`, `opensWhole`, `halfRestDp`, `opensAtDp`, `foldTopDp`, `doneBottomDp` and the sizes `GRIP_DP`, `HEAD_DP`, `HEAD_LAND_DP`, `ACT_ROW_DP`, `ACT_ROW_LAND_DP`, `SAID_DP` | see the source | `rememberModalBottomSheetState(skipPartiallyExpanded = true)` (`MS:146`) becomes `opensWhole(land)`; `dragHandle = null` (`MS:152`) a grip where `hasGrip(land)` |
| `MarkSheet.slots`, `Slot`, `ACT_COLUMNS`, `ACT_GAP_DP`, `DELETE_GAP_DP`, `DONE_CLEAR_LAND_DP` | `slots(acts, widthDp, land, doneLeftDp = widthDp): List<Slot>` | new: the two act rows between `MsHead` and the blocks in `MsPortrait` (`MS:169`), the one row in `MsLandscape` (`MS:187`) |

**The code chip (`SurveyPrepTouch`; `CodeChipTest`, 14 tests)**

| New | Signature | Replaces in `:app` |
|---|---|---|
| `SurveyPrepTouch.codeChip`, `Chip`, `chipLift`, `handlesOf`, `boxDist` and `CHIP_*` | `codeChip(bb, handles, textW, w, h, dp, lift = 16 dp, avoid = []): Chip?`; `Chip(box, tap, how)`; `chipLift(hand, fs, u, dp)`; `handlesOf(bb, spot, corners = null)` | the picked mark's code drawn inside its box (`SPD:530`, its box noted by `codeAt`, `SPD:502-505`); `Chip.tap` joins the layout's `keep` (`SPD:238`, handed to `DimLayout.placeShown` at `SPD:935`) |
| `SurveyPrepTouch.codeChipOfLine` | `(scr, textW, w, h, dp, avoid = []): Chip?` | the code in the picked Tape's / Line's / chain's row (`PhotoFigures.nameOf`, `SPS:1984`) |
| `SurveyPrepTouch.Hit.Code`; `hit(…, chip: Box? = null)` | the last parameter of `hit` | `markHit`, `SPS:2784-2794`, hands the chip's tap box; `pressId` (`SPS:2797`) and `markGrab` (`SPS:2816`) already have their branch |
| `Pick.tap` on `Hit.Code` | answers `Pick.options(st)` (row 7) | `onTap` at `SPS:1709` needs nothing new: `took` opens the panel when `o.options` (as `longPressed` reads it, `SPS:878`) |

**The lift and the pad (`PhotoLift`, `NudgePad`, new files; `PhotoLiftTest` 6, `NudgePadTest` 11)**

| New | Signature | Replaces in `:app` |
|---|---|---|
| `PhotoLift.lift`, `lifts`, `inView`, `onScreen`, `onPhoto`, `MARGIN_DP` | `lift(photoTop, photoBottom, panelTop, itemTop, itemBottom, margin, land = false): Double` | new: a translation of the photo's box where `SpPhoto` is placed, `SPS:1700`, while the panel is open |
| `NudgePad.start`, `State`, `Corner`, `corner`, `box`, `cell`, `keyAt`, `GRID`, `Key`, `glyph` | `start(p, id, stage, part, bb, w, h, dp): State?`; `State(id, part, stepMm, corner, stepTaken)` | the nudge buttons of `markPanel`, `SPS:1970-1977`; `var part` (`SPS:330`) is its `part` as Fine move starts |
| `NudgePad.press`, `Pressed`, `delta`, `live`, `toggle`, `FINE_MM`, `COARSE_MM` | `press(p, st, key, a, b, snapshot): Pressed(state?, moved)` | `nudgeMark`, `SPS:1097-1106` (deleted) |
| `NudgePad.aim`, `parts`, `pointPart`, `pointOf`, `BODY` | `aim(p, st, part): State` | `onHandle` at `SPS:1711` (`Pick.handle(pick, pt).nudge`) hands the part while the pad is up; an end pressed (`SurveyGuide.EndPress.Grab`, `SPS:2900`) hands `pointPart(i)` |
| `NudgePad.after` | `(p, st, pick, stage): State?` | new: asked in `took` (`SPS:646`) after every event, so the pad goes whenever its item is no longer held |
| `NudgePad.stepText`, `stepLabel`, `doneLabel` | texts of the centre cell and ✓ | new |

**The order the screen calls them in (I2b).**

- **A long press** (`longPressed`): `Pick.longPress` as today; when the finger lifts and `o.options`, `panel = MarkSheet.openPanel(prep, id, now, longPress = true, hist = hist.size)`. **A tap on the chip:** `Pick.tap` answers `options`; `openPanel(…)` the same with `longPress = false`. The reference rectangle is no item: `openPanel` answers null for it.
- **Drawing:** `MarkSheet.panel(tx, prep, panel, quoteStage, asPicked)` (null: close). The head from `head`; the acts from `MarkSheet.slots(view.acts, width, land, doneLeft)`; `said` under them; the blocks from `body` with the existing `MsKind`, `MsTrade`, `MsState`, `MsNote`; `stateFirst` scrolls STATE into view once.
- **Every tap and keystroke in the panel:** `if (!MarkSheet.answers(panel, now)) return`, then `panel = panel.with { MarkSheet.pickSub(prep, it, sub) }` and so on for each existing callback.
- **An act:** `when (val s = MarkSheet.actStep(tx, prep, panel, act, quoteStage, asPicked))` — `Refused`: `panel = s.panel`. `Delete`: `panelCancel`, close, `deleteMark(id)`. `Run`: `val before = hist.size; val done = MarkSheet.panelDone(…) { snapshot() }; val mid = hist.size`; the sheet stays or closes as `sheetDone` decides today; `if (MarkSheet.actRuns(done.outcome))` run the act (`toggleLock`, `duplicateMark`, `lockAllOf`, or Fine move below), then drop `MarkSheet.stepsToDrop(before, mid, hist.size)` entries from the top of `hist`.
- **Fine move:** `took(Pick.shaped(scene(), pick, id))`, then `pad = NudgePad.start(prep, id, quoteStage, part, itemBox, w, h, dp)`. The pad's cells are pressed BEFORE anything on the photo (`NudgePad.keyAt`); `val r = NudgePad.press(prep, pad, key, a, b) { snapshot() }; pad = r.state; if (r.moved) changed()`. `Pick.handle`'s `nudge` goes to `NudgePad.aim`. After every `took`, `pad = NudgePad.after(prep, pad, pick, quoteStage)`. The pad is not "busy" for `Landing` (`sheetOrFinger`, `SPS:682`): a reading lands, the hold goes, the pad with it.
- **The chip:** for a picked mark `SurveyPrepTouch.codeChip(bb, SurveyPrepTouch.handlesOf(bb, spot, corners), textW, w, h, dp, SurveyPrepTouch.chipLift(held, fs, u, dp), avoid = the fit · grid column)`; for a picked Tape, Line, area or chain `codeChipOfLine(scr, …)`. Its `tap` goes into the layout's `keep` and to `SurveyPrepTouch.hit(…, chip = tap)`; its `box` is drawn; its label is `MarkSheet.optionsLabel(tx, code)`.
- **The lift:** while the panel is open, `PhotoLift.lift(photoTop, photoBottom, panelTop, chipTop, itemBottom, 12 dp, land)` each frame the panel's top moves.

**Decided here, where the design was silent.**

1. **`Lock all` reads "Lock all" or "Unlock all" on the cell** (the picture shows "Lock all"; the menu's "Lock all Switch plate" does not fit a third of 336 dp) **and names its group to a screen reader in the menu's own words** ("Lock all Door"). "Unlock all" had no bare key: `sp.sheet.unlock_all` is new. §12's `sp.sheet.unlock` is NOT added — `common.unlock` already says it.
2. **No `Fine move` on a chain.** Nothing moves a chain whole, the chain tool is off, and its points still drag along its axis as built. So a chain's panel is `Lock` / `Lock all · Delete`.
3. **A refused act says why IN THE PANEL and leaves it open with what was typed** (`Panel.said`, one line under the acts, gone at the next tap or keystroke): Delete and the shape row on a locked item (the lock's words), Unlock on a mark in Scope, Lock all outside Survey. The photo's note line is under the panel, and closing the panel on a refusal would throw a typed note away for nothing. §6's "on a locked mark Delete is drawn and answers the lock's words" is this.
4. **The first 300 ms cover every way the panel opens** (the chip as well as the long press — §6 says so; the brief named the long press) **and the dimmed photo as well as the panel**: a hurried second tap on the photo above would otherwise cancel the panel he had just opened. Back always answers.
5. **"With STATE in view"** is one flag, `stateFirst`: a long press on a mark past Existing, and any opening on a move's Why box. With two rows of acts STATE already starts about 200 dp down, above the half rest; the flag is for the short window and the keyboard.
6. **Line 1's row is kept whenever there is a delete cell** (`Acts.rows`), so Delete can never move up under Done. It cannot be empty there by today's rules (a delete cell implies a padlock); the rule does not depend on that.
7. **Delete is always in the LAST column of line 2**, the middle column left empty where there is no shape row — it does not move as Q2's switch turns on, and it is 24 dp from whatever stands beside it.
8. **The chip stands above the held mark's hand-anchored width plate** (`chipLift`): the chip is placed first and handed to the layout as a box to keep clear, so a chip in that plate's row would push the gold width plate of a mark just placed off its anchor. A mark with no handles has it 16 dp over its top edge.
9. **Where a mark's chip goes when neither above nor below has room** (a mark as tall as the photo, or one that fills it): left of its top, right of its top, then INSIDE the box 16 dp in from its top-left corner, where it beats the body it lies on. **No chip at all** where no place is 16 dp clear of the handles — a long press still opens the panel.
10. **A Tape's, Line's, area's or chain's chip stands on the side of its first point that points away from the rest of the item** (left of a level tape pulled to the right; under a Line drawn up from the floor), 16 dp clear of every one of its points.
11. **A tap on the body of the item being fine-moved aims the pad at the whole item again**; a tap on one of its figures turns it gold and leaves the pad up (Pick row 4 keeps the hold).
12. **One ↶ step for a whole Fine move**, taken just before its first nudge that moves anything; a press that moves nothing (an arrow an edge does not answer to, an item hard against the face's edge) takes none.
13. **An edge answers two arrows only** (a top or bottom edge ▲ ▼, a left or right edge ◀ ▶), the other two drawn dim, as the row drew two pairs.
14. **A Tape or a Line is never nudged shorter than `SurveyMeas.MIN_LEN`** (20 mm, the length one may be plotted at). A drag of an end has no such floor; the pad's has, because a 1 mm step makes a zero-length line easy.
15. **The pad's corner is chosen once, as Fine move starts**, and kept: a pad that changed sides as the item crossed the middle would leave the thumb pressing it. Where the item lies under both bottom corners (a unit along the whole foot of the wall), the corner whose pad covers less of it; a tie goes right.
16. **The lift keeps the item's TOP in view when both ends cannot be** (its code chip and top edge), and is never more than the panel covers, so no empty band opens under the photo.
17. **`MarkSheet.acts`, `panel` and `actStep` take the chain switch** as `menuItemsOf` does, so `FeaturesTest`'s inventory of who reads `Features.CHAINS` gains `MarkSheet.kt` (3).

**Found while building, for I2b to settle.**

- **The reference rectangle's long press** opens today's menu with its own Lock · Cancel (`SPS:875`). It is no item and has no panel; with `SpCtxMenu` deleted it needs another home (its padlock already stands in 1 · Lines' panel) or the menu survives for it alone.
- **On the 360** the panel opens by a long press only (§11): no chip is drawn there, and the 360's own floating trash (`SP360:699-705`) goes with the photo's.
- **`DimLayout`'s hand-anchored height plate** stands right of the mark; a chip that falls to "right of its top" (a tall mark against the left edge) shares that side, and the layout then moves the plate off its anchor. Rare; to be looked at on the phone.
- **The four-corner closing check's sentence** is still unwritten (§20.3, 2). Q2's.

**Texts.** Five keys, English first, each listed in `mr-pending.tsv` (29 keys): `sp.sheet.fine_move` "Fine move", `sp.sheet.unlock_all` "Unlock all", `sp.sheet.aria.open_x` "Options for {code}", `sp.fine.aria.step_x` "Step {mm} mm · tap to change", `sp.fine.aria.done` "Finish fine move". Each has its reader in `:pano`. 1,936 keys; `Tx` regenerated; `unkeyed.tsv` stays empty; the ratchet lowered nothing and entered `NudgePad.kt` and `PhotoLift.kt` at 0. No key was deleted: every key the menu, the trash and the row read still has its reader on screen until I2b.

**Tests.** `:pano`: 112 test classes, 1,578 tests, 0 failures, 0 errors (P2N: 108 and 1,531). New: `MarkSheetPanelTest` 16, `CodeChipTest` 14, `NudgePadTest` 11, `PhotoLiftTest` 6. Changed: `FeaturesTest`'s inventory (decision 17). No earlier test was loosened or deleted.

**Type-checked here, and what that cannot prove.** kotlinc 2.0.21 from the Gradle cache, the real `:pano` classes and the stand-ins of §20: `SurveyPrepScreen.kt` whole, with its seven companions, has NO error (the 235 left are the same stub gaps in the five files this commit did not touch, count for count). With the two new branches taken out the compiler reports exactly the two `when`s as not exhaustive, so the check sees what it is there for. Both added lines use only `pickS` (declared in `SpPhoto` above them, and used by the branch beside it) and the type `SurveyPrepTouch.Hit.Code`. Not proven: anything the Compose compiler plugin or lint adds, and everything about a phone.

**Where to look first if CI's `:app` compile is red.** `SurveyPrepScreen.kt`, `pressId` (`SPS:2797-2804`) and `markGrab` (`SPS:2816-2820`): the two `Hit.Code` branches. Nothing else in `:app` changed.
