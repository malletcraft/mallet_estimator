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

## What is here

| File | What it is | Who writes it |
|---|---|---|
| `strings-en.json`, `strings-mr.json` | The two tables, 1,801 keys each: the prototype's 1,607 (v61c.1) and the v61c card's 194 (L2). In the prototype's byte format (one-space JSON, no trailing newline) and key order (family, then key); never reformatted. | a reviewed commit here |
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
