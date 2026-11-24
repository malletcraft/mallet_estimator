# DESIGN-tree — the client / site / project tree after build 0.3.350

S2 APK (`mallet_estimator` → `android/`). Written 23 Nov 2026 (IST) on branch `port63-f4`, over `cff09e5` (port 63
F3b; `:pano` 113 classes, 1,620 tests). The tree's code at `cff09e5` is the code build 0.3.350 carries: no commit
since it has touched `Catalogue`, `S5Session`, `S5Sites`, `SiteRename`, `SyncWorker`, `Crumbs`, `RenameRoute`,
`SiteRows` or `DeleteRoute`.

**Basis.** Three readers' reports on the tree (what the phone lists and under what; the renames; the "site switch")
and a verifying pass that compiled the real `Catalogue.kt` and `S5Sites.kt` against the real `:pano` jar and ran them
over made-up names and over a read-only snapshot of staging. Those reports and that snapshot name real people and
are in no repository; this file names nobody. Where it says "his own client", "the second client" or "a project of
his", the name is in the session's notes.

**Not seen on a phone.** `:app` is compiled only by CI. What was run for this file is `:pano`'s tests and a scratch
harness that compiles the real `Catalogue.kt`, `S5Sites.kt`, `S5Session.kt`, `CaptureStore.kt` and `JsonRead.kt` of
this branch against in-memory stand-ins for Android. §6 lists what that does not prove.

Paths: `pano/…` = `android/pano/src/main/kotlin/com/malletcrafts/interiorestimate/pano/…`, `app/…` =
`android/app/src/main/kotlin/com/malletcrafts/interiorestimate/…`.

## 0. What Amit asked

Amit, 23 Nov 2026 (~22:00 IST), with build 0.3.350 on his phone, typed in place of an answer to another question.
The parts of his message this file answers, his words:

> "… get rid of any other clients. … verify if renaming a site, client works actually correct. site switch is not
> happening correctly. fix it."

His two answers on the selector that followed (~22:15 IST):

- the other clients' data is **hidden, and kept in ERP** (their projects were set to Cancelled on the staging bench
  the same evening; nothing was deleted);
- what he had seen of a rename was **"Rename did not take"**: after a rename the old name came back, or the site
  split into two rows.

The fix is four slices and a fifth that needs his yes (§5). This file is written with them, each its own commit:
**F4a** (the plan's Slice 1: what the phone lists, §2), **F4b** (the plan's Slice 4: a rename that cannot work says
so before Save, and never forks, §3), **F4c** (the plan's Slice 2: one project, one site, §7) and **F4d** (the
plan's Slice 3: navigation, the "site switch", §8). §9 is one question checked with them and not fixed at the time.
**§10 is the review of the four slices and what was done about it** — one commit over F4d, which also closes §9 and
several lines of §5. Sections 1 to 9 are kept as written, with a pointer to §10 wherever it changed what they say.

A third sentence of the same message is the one F4c answers. It names one of his projects and the flat it belongs
to, so it is in the session's notes and not here; what it says is that the project belongs to that flat and no
other. On his phone it was listed outside the flat's site.

## 1. The faults these two slices close

The verifying pass numbered nineteen faults, V1 to V19. Reduced here to the ones this build closes or changes; the
rest are in §5 with the slice that takes each.

| # | The fault | Closed by |
|---|---|---|
| V2 | A phone with no live ERP pair keeps its last ERP bootstrap for ever. Every read of S5's list kept the bootstrap's projects and its whole `sites` array; sign-out drops only what S5 added; a bootstrap is replaced only by the next one, and a phone whose pair is gone or refused never receives one. So nothing done in ERP could reach such a phone. | **F4a**: `TreeKeep.keepsProject`, `TreeKeep.keepsSite`, asked by `S5Masters.fetch` |
| V3 | The bench's bootstrap sends every Mallet Site, and the tree makes a client of every site row's owner — so a client whose projects were all cancelled stays, with an empty site. | **F4a, on a phone with no live pair only**: a site row stays only while a kept project links to it. On a phone that bootstraps it is the bench's to fix (§5) |
| V14 | The Demo Client is always appended to the tree although the help tours are off, and the search overlay shows three hard-coded suggestions. | **F4a**: `TreeKeep.demoListed`, `TreeKeep.listsCapture`; the suggestions are gone |
| V5 | Renaming a phone-made client says "Renamed" and leaves two clients: the project S5 holds stays under the old name (the phone re-reads its client from the capture row, which no rename touched), a copy appears under the new one, and the next capture mints a second S5 project. | **F4b**: such a client is shut (`Renames.route`'s `onS5`); a row nothing of which is on S5 is renamed with its captures (`Renames.rekey`) |
| V6 | A client or a project that carries an id cannot be renamed on a phone with no pair — and the dialog says "Could not rename: no server configured" only after Save. With a dead pair stored the call goes to the bench with the dead key. A phone-minted `S5-…` id is sent to the bench as a Project docname. | **F4b**: the refusal is said before Save, in true words, and neither call is made. The rename itself is Slice 5 |
| V13 | Deleting a site S5 holds, on a phone with no pair, removes the phone's row, says "Removed from this phone — … had never reached the office system", and the site stays, listed by S5. A project under an `S5-…` key is sent to the bench. | **F4b**: `Deletes.route`'s `onS5` — refused, saying so |
| R10 (audit of 0.3.318) | A local rename of a client or a project leaves the phone's captures under the old name: the renamed project shows no photos, and its upload goes up under the old name. | **F4b**, for a row made on this phone: `Renames.rekey`, `CaptureStore.setNames` |

"Rename did not take", as far as it could be reconstructed: V5 is the rename after which **the old name came back**
(the old client is listed again at every read of S5's list, holding the real project, beside the new one — two rows),
and V6 is the rename that was refused. The site that **split into two rows** needs a site with projects S5 lists
beside projects only ERP holds; after F4a a phone with no live pair lists no such site.

**What he most probably met**, as the verifying pass read it: his own sentence is the evidence that his phone lists
other clients, and a tree drawn from S5's list alone has three or four rows — so his masters very probably still held
a bootstrap from before the phone's ERP key died on 23 Oct. Whether they did was not readable from outside the
phone. One look at the Clients screen of 0.3.350 settles it: thirteen or more rows is the stale bootstrap.

## 2. Slice F4a as built — on a phone with no live ERP pair, S5's list is the list

### 2.1 The rule

`pano/TreeKeep.kt` (new), pure, tested by `TreeKeepTest`:

```kotlin
object TreeKeep {
    data class Entry(val project: String, val client: String, val title: String, val siteId: String)
    data class Cap(val project: String, val client: String, val title: String, val sent: Boolean)
    fun erpLive(pair: Boolean, refused: Boolean): Boolean
    fun keepsProject(e: Entry, erpLive: Boolean, s5Listed: Set<String>, captures: List<Cap>): Boolean
    fun keepsSite(docname: String, erpLive: Boolean, keptSiteIds: Set<String>): Boolean
    fun demoListed(tours: Boolean = Features.HELP_TOURS): Boolean
    fun listsCapture(deviceId: String, demo: Boolean = demoListed()): Boolean
    fun projectOf(title: String, titles: List<String>): Int
}
```

- **A live pair** (`erpLive`: one is stored and the bench has not refused it) keeps everything. Its bootstrap is the
  bench's own answer and is replaced whole by its next sync; S5's list is merged into it as before.
- **Without one**, a bootstrap project stays only if **S5 lists its id**, or **a capture of it on this phone has not
  reached a server yet** — by the project id once the capture carries one, else by client and title as the catalogue
  compares names (trimmed, lower case, runs of space and underscore as one). A capture a server already holds
  (`sent`: the store's state is `SYNCED`) keeps nothing listed.
- **A `sites` row** stays only while a kept project links to it (its docname is the `site` of a kept entry). A row
  with no docname links nothing.
- **Not asked:** rooms, job types, site types and stages (S5 serves none; emptying them would empty every picker),
  and the rows made on this phone (`Catalogue`'s local rows: this phone's own words, and one of them is the only name
  the phone has for the site of a project S5 has not named).

### 2.2 Where `:app` asks it

| Site | What changed |
|---|---|
| `app/S5Session.kt` `S5Masters.fetch(context, erpLive)` | The one read of S5's list that writes the masters. Before its merge it builds the ids S5 just listed and every capture as a `TreeKeep.Cap` (`sent = state == "SYNCED"`), asks `keepsProject` for each bootstrap entry (an entry an earlier S5 read added is still replaced, never asked) and `keepsSite` for each row of `sites`. It reads no pair itself: `erpLive` is the caller's. Only `projects` and `sites` are written. |
| `app/S5Session.kt` `S5Masters.erpLive(context)` (new) | `TreeKeep.erpLive(pair = FrappeClient.load(context) != null, refused = FrappeClient.refused(context))` — the one place `:app` reads the two facts. |
| `app/SyncWorker.kt` `s5Leg` | `S5Masters.fetch(ctx, erpLive = false)`. The leg runs only when `doWork` found no pair or its one bootstrap probe refused, so the answer is the leg's own condition — not the refusal flag, which `doWork` writes after the leg. |
| `app/MainActivity.kt` `LaunchedEffect(configured)` | `booted` = the start's own bootstrap landed just now; `erpLive = booted \|\| S5Masters.erpLive(context)`. Live: S5's list is read for its site names alone (`SiteRename.hear`), as before. Not live and signed in to S5: `S5Masters.fetch(context, erpLive = false)`. **Before this slice the read was made only with NO pair stored**, so a phone holding a dead key drew its last bootstrap until a sync ran. |
| `app/MainActivity.kt`, the rename's read-first | `S5Masters.fetch(context, erpLive = S5Masters.erpLive(context))`. |
| `app/Catalogue.kt` `projects` | The demo flat is appended only `if (TreeKeep.demoListed() && …)`. |
| `app/MainActivity.kt` `SearchOverlay(…)` | `recents = emptyList()`; `app/HomeScreens.kt` draws the "Try" head only over a list that is not empty. |
| `app/MainActivity.kt` `recentRooms` | A capture whose project the tree does not list (`TreeKeep.projectOf` answers -1) is skipped; the card is the tree's project, never the capture's own names with "Main site". |
| `app/MainActivity.kt` `QueueScreen(sent = …)` | `&& TreeKeep.listsCapture(it.deviceId)`: the demo's own capture is not a row of Sent while the Demo Client is not listed. |

### 2.3 What Amit sees after one app start (or one sync) with a signal

His two clients only: the projects S5 lists, each under S5's site name where S5 holds one, and any project a capture
of which exists only on his phone. No Demo Client. The Work tab's recent rooms are rooms of those projects. The
Queue's Sent list is unchanged except for the demo's capture. Projects that only ERP holds are gone from the phone
and are still in ERP — his own among them, which is the one decision below.

A phone with a live ERP pair is not changed by this slice: no phone holds one today.

### 2.4 Decided by Claude, not by Amit

1. **Projects only ERP holds leave the tree of a phone with no live pair — his own as well.** He is being asked.
   The choice has ONE home, the last line of `TreeKeep.keepsProject`:

   ```kotlin
   return captures.any { c -> !c.sent && isOf(c, e) }
   ```

   Should he rule that the ERP-only projects of a client this phone works for stay, that line alone becomes

   ```kotlin
   return captures.any { c -> (!c.sent && isOf(c, e)) || (c.project in s5Listed && k(c.client) == k(e.client)) }
   ```

   (a capture on this phone ties the entry's client to a project S5 lists), with its cases added to `TreeKeepTest`.
   Nothing in `:app` changes: `S5Masters.fetch` asks the function and holds no answer of its own. What that second
   form does not cover is a client tied to S5 by nothing on this phone (after a reinstall): its ERP-only projects
   would go as they do now.
2. **`sent` is the capture store's state `SYNCED`.** A capture the S5 leg stamped "on this phone only" (a flat photo,
   a 360 whose survey prep is not saved) is not sent, so its project stays listed for as long as the capture is on
   the phone and unsent: work that exists only here is never hidden by a list it has not reached.
3. **At app start the bench's own answer outranks the stored refusal.** The refusal flag is as old as the last sync;
   a bootstrap that lands at start is a live pair whatever the flag says, and then S5's list is not allowed to cut
   it.
4. **The Demo Client's row follows `Features.HELP_TOURS`**, through `TreeKeep.demoListed`. What a phone seeded while
   the tours were on (its capture row, pano and survey prep) stays in the store, untouched, and is listed again the
   day the switch is. `Features.kt`'s own note on the switch says so now.
5. **The demo's capture leaves the Queue's Sent list with the row** (`TreeKeep.listsCapture`). The plan named the
   tree and the recents; a "Demo Client" line under Sent is the same third client by another door.
6. **The search overlay's suggestions are removed, not replaced.** They were three hard-coded demo strings. Nothing
   is suggested, and the "Try" head is not drawn over an empty list.
7. **The recents' join stays the title alone** (`TreeKeep.projectOf`), as every screen that shows a project's photos
   joins them today (V15, §5). It is now in one pure place for whoever changes the join.
8. **`ported.txt`** lists `pano/TreeKeep.kt` (no shown text). No key is added and no text changes in this slice.

### 2.5 Tests

`:pano` 113 classes, 1,620 tests → **114 classes, 1,631 tests**, all green (the totals of
`pano/build/test-results/test`).

- `TreeKeepTest` (new, 11): a live pair keeps everything; no live pair — listed stays, ERP-only goes; an unsent
  capture keeps its project by id, else by client and title, and a sent one, another project's, another client's do
  not; a site row only while a kept project links to it; `erpLive`; a whole stale bootstrap of three made-up clients
  cut to S5's list; the Demo Client and its capture by the switch; `projectOf`; and three source tests of `:app`
  (the read asks the rule for every entry and row and reads no pair itself; each of the three callers says whether
  the pair is live; the demo, the suggestions, the recents and the Sent list follow).
- **Pins that moved, each with its reason beside it:** `RenameRouteTest` — `fun fetch(context: Context)` is now
  `fun fetch(context: Context, erpLive: Boolean)` (twice), the rename's read-first names `erpLive`, the S5 leg's read
  is `S5Masters.fetch(ctx, erpLive = false)`, and the app start's two pins read "a LIVE pair" where they read "a pair
  is stored"; `FeaturesTest` — the inventory of sources that read `Features.HELP_TOURS` gains `pano/TreeKeep.kt` (1),
  with two pins on the Demo Client's row.

## 3. Slice F4b as built — a rename that cannot work says so before Save, and never forks

### 3.1 The rules

`pano/RenameRoute.kt`, pure, tested by `RenameRouteTest`:

```kotlin
object Renames {
    fun route(tx: Tx, kind: String, name: String, serverId: String, hasLocalRow: Boolean,
              s5Projects: List<RenameRoute.Write>, signedIn: Boolean, erpLive: Boolean, unlisted: Boolean,
              listRead: Boolean = false, heard: Boolean = true, mayHold: Boolean = false, onS5: Boolean = false): RenameRoute
    const val MINTED = "S5-"
    fun minted(id: String): Boolean
    fun onS5(carried: List<String>, derived: List<String>, listed: (String) -> Boolean): Boolean
    fun rekey(kind: String, client: String, title: String, to: String): (TreeKeep.Cap) -> TreeKeep.Cap
    fun officeRecord(tx: Tx, kind: String): String
    fun heldByS5(tx: Tx, kind: String): String
}
```

`erpLive` took the place of `legacyPair`, in the same position: "a pair is stored" became "a LIVE pair" (stored and
not refused, `TreeKeep.erpLive`). `onS5` is new and last, default false.

**A site's route is not changed by one line.** Everything below is for a client and a project, in this order:

1. **A bench docname** — an id that is not a phone-minted key — goes to the bench only with a live pair
   (`RenameRoute.Legacy`, as before). Without one: `Blocked(officeRecord, SHUT)`. It used to be
   `Blocked("Could not rename: no server configured", SERVER)`, drawn after Save; with a dead pair stored it was
   `Legacy`, and the call went out with the dead key.
2. **S5 holds something of the row, or the id is a phone-minted `S5-…` key** → `Blocked(heldByS5, SHUT)`, whatever
   the pair. A minted key used to be `Legacy("S5-…")` with a pair stored: `rename_node` asked the bench for a Project
   it has never held.
3. **A row made on this phone** → `RenameRoute.Local`, now reached only when nothing of the row is on S5.
4. **Neither** (the demo's rows) → `Blocked(nothingRenamed, PHONE)`, as before.

SHUT is the line the site's dialog already has: the name is shown and not edited, Save is off, and the reason stands
in the grey line's place **from the moment the dialog opens**.

**`onS5`** — whether S5 holds something of a row, or is being sent something of it:

- a CARRIED id (the server id of a project the tree lists under the row; the project id of a capture of the row)
  counts when S5's list has named it, or when it is a phone-minted key — a capture keyed for S5 has gone up under
  that key or is about to, and the key is a hash of the very names a rename would change;
- a DERIVED key (what a row made on this phone would be called on S5) always looks minted, so it counts only when
  S5's list has named it: the project made here that S5 holds, seen while nobody is signed in and the masters no
  longer list it.

**`rekey`** — the change a local rename makes to ONE capture: a client rename gives the new name to every capture of
that client, a project rename to every capture of that client and title (names as the catalogue compares them); a
site rename changes none (a capture carries no site). Left exactly as it is: a capture a server already holds, and a
capture that carries a project id — that one is the project's, and a row with such a capture never takes the Local
route.

`pano/DeleteRoute.kt`: `Deletes.route(tx, kind, serverId, serverKnown, onS5 = false)`. After the client's own
refusal and before everything else: `onS5`, or a phone-minted id → `Blocked` (`app.site_s5_not_removed`,
`app.project_s5_not_removed`). With nothing on S5 the three routes are exactly as they were.

### 3.2 Where `:app` asks them

| Site | What changed |
|---|---|
| `app/Catalogue.kt` `onS5(masters, captures, kind, client, site, project)` (new) | Gathers what `Renames.onS5` is asked with, for a client with everything under it, a site, or one project: the tree's projects of the row (`serverId`), the row's captures (`project`; a capture's site read from the phone's own row for its client and title, never the demo's), the minted keys of the row's phone-made rows (`S5Auth.localKey`), and "listed" = `S5Sites` holds the id. The one caller of the rule. |
| `app/MainActivity.kt` `renameRoute` | `erpLive = S5Masters.erpLive(context)` where it passed `legacyPair = FrappeClient.load(context) != null`; `onS5 = !isSite && cat.onS5(masters, queue, t.kind, t.client, t.site, t.project)`. The dialog's opening form is this same route (`val opened = remember(…) { renameRoute(t) }`), so the reason is on screen before any Save. |
| `app/MainActivity.kt` `renameHere` | After `cat.renameLocal` answered true and before `reload()`: for a client or a project, every capture is asked `Renames.rekey(t.kind, t.client, t.project, newName)` as a `TreeKeep.Cap` (`sent = state == "SYNCED"`), and a changed one is written with `store.setNames`. |
| `app/CaptureStore.kt` `setNames(deviceId, customerName, projectTitle)` (new) | One UPDATE of the two name columns of one row. The project id, the state and the files are not touched. |
| `app/MainActivity.kt`, the dialog's `onDelete` | `Deletes.route(tx, t.kind, t.serverId, t.serverKnown, onS5 = cat.onS5(…))`. |
| `app/TreeShell.kt` `RenameDialog` | A comment only: SHUT was already drawn for any kind. |

### 3.3 What Amit sees

- **His own client** (an ERP customer; its id reaches the phone on the project S5 lists): the rename dialog opens
  with the name greyed, Save off, and "This client is the office's record. It is renamed at the office, not from
  this phone." Before: an editable name, then "Could not rename: no server configured" after Save.
- **The second client** (made on his phone, its project on S5): the name greyed, and "This client cannot be renamed
  from this phone: a project of it is on S5, or on its way there, and the new name would not reach it." Before: it
  said "Renamed" and listed two clients.
- **Every project he has**: greyed, with the project's twin of one of those two sentences — the office's for a
  project with an ERP id, S5's for one under a phone-minted key.
- **A site S5 holds**: renamed exactly as in 0.3.350, on S5 and on the phone. "(no site)" stays shut.
- **Delete, on a site or a project S5 holds**: "This site has a project on S5, or on its way there. This phone
  cannot delete it there, so nothing was removed." in red, no button — and the row is still there. Before: the row
  went, the banner said it had never reached the office system, and S5 went on listing it.
- **A client or a project he types on site and has not yet sent**: renamed on the phone as before, and now its
  photos go with it — the project still shows them, and the first upload goes up under the new name, as one project.

### 3.4 Decided by Claude, not by Amit

1. **The order: the docname decides before S5 does.** A client that is an ERP customer AND has a project on S5 reads
   the office's sentence, not S5's: it is the reason that names where the rename CAN be made.
2. **A capture that carries a phone-minted key counts as S5's — sent or not.** Whether its upload has landed is a
   job's state, not a fact of the row; the key is a hash of the names. So the sentence says "on S5, or on its way
   there", which is true in both states, and a row with such a capture is shut rather than renamed with its key
   re-minted. The cost: a phone-made client or project can be renamed only until its first capture has been through
   one sync. Re-minting the key of a capture whose upload may be half sent was judged the worse risk.
3. **`rekey` leaves alone any capture that carries a project id**, minted or not. Such a capture cannot belong to a
   row that takes the Local route; were one ever to, changing its names and not its id would be the fork again.
4. **`Deletes.route` refuses on `onS5` before it looks at the docname.** So a row with a bench docname that S5 also
   holds is refused on a live-pair phone as well: a bench delete would leave it listed by S5 — and for a site S5
   named differently it would have cascaded the WHOLE bench site from a row that shows one project (V12's hazard,
   closed for that case as a side effect).
5. **The delete's `onS5` is the rename's.** One gathering (`Catalogue.onS5`), one rule; a site is asked with every
   project and capture under it. So a row whose only tie to S5 is a capture a sync has keyed and not yet uploaded is
   refused as well — "or on its way there". Removed from the phone, its capture would still go up and bring the row
   back from S5, or sit in the queue under a project the tree no longer lists. Deleting that capture first (the
   capture screen's Delete) makes the row the phone's own again, and then it is removed as before. The sentence does
   not say that; it is the first thing to hear back about from the phone.
6. **The "(no client)" bucket** — S5's projects with no customer and no capture on this phone — is shut with the
   client's S5 sentence. It was refused after Save with "Could not rename: (no client)".
7. **Both parameters default to the old behaviour** (`onS5 = false`), as `listRead`, `heard` and `mayHold` do, so
   the existing cases stand unchanged; `RenameRouteTest` and `DeleteRouteTest` each pin that `:app`'s one
   caller passes it.
8. **The six sentences** (§4), and that each is a whole key per kind.

### 3.5 Tests

`:pano` 114 classes, 1,631 tests → **114 classes, 1,644 tests**, all green.

- `RenameRouteTest` (+7): **the rename table of a phone with no live pair, row by row** — a site S5 holds (works),
  "(no site)" (shut), an ERP-only site (shut), a client that is an ERP customer (shut, the office's; signed in or
  not, a phone row or not, whatever S5 holds), a client made on the phone whose project S5 holds (shut, never
  Local), a project by its ERP id, under a minted key, and as a phone row of a project S5 holds; a minted key is
  never a docname, with a live pair, a dead one or none, and is `:app`'s own prefix; the Local route only for a row
  nothing of which is on S5; `onS5`; `rekey`; the four reasons as whole keys per kind, English under मराठी for now;
  and one source test of `:app` (the dialog routes on `S5Masters.erpLive` and `Catalogue.onS5`, opens on that route,
  draws SHUT for any kind; `renameHere` renames, re-keys, writes and only then re-reads; `setNames` writes two
  columns).
- `DeleteRouteTest` (+6): a site S5 holds is refused, never LocalOnly; a minted key never reaches the bench; a
  docname S5 also holds is refused; a client keeps its own refusal; the sentence names S5 and says nothing was
  removed; `:app`'s one caller passes `onS5`.
- **Pins that moved, each with its reason beside it:** `RenameRouteTest`'s helper and five cases name `erpLive`
  where they named the old argument; and ONE expectation changed — a client or project with a docname and no pair
  was `Blocked("Could not rename: no server configured", Line.SERVER)` with `shut` false, and is
  `Blocked(<the office's sentence>, Line.SHUT)` with `shut` true. That is the slice.

## 4. The new texts

Six keys, each a phone text, English first (Amit, 16 Nov 2026): the Marathi value of each is its English, and each
is listed in `android/i18n/mr-pending.tsv` (31 → 37). No existing text changed. `android/i18n/README.md` has the
table with each key's reader.

| Key | English |
|---|---|
| `app.client_office_record_renamed` | This client is the office's record. It is renamed at the office, not from this phone. |
| `app.project_office_record_renamed` | This project is the office's record. It is renamed at the office, not from this phone. |
| `app.client_s5_not_renamed` | This client cannot be renamed from this phone: a project of it is on S5, or on its way there, and the new name would not reach it. |
| `app.project_s5_not_renamed` | This project cannot be renamed from this phone: it is on S5, or on its way there, and the new name would not reach it. |
| `app.site_s5_not_removed` | This site has a project on S5, or on its way there. This phone cannot delete it there, so nothing was removed. |
| `app.project_s5_not_removed` | This project is on S5, or on its way there. This phone cannot delete it there, so nothing was removed. |

"S5" is written as the site's rename dialog already writes it ("This site is on S5, so the new name is saved there…").

## 5. What is NOT fixed, for whoever continues

In the order the verified plan gives them. V-numbers are the verifying pass's.

- **Slice 2 — one project, one site** (V1): **built, F4c — §7.** The data that prompted it (a project of his on S5
  with no site name) was written on S5 stage the same evening, so his phone lists it under the flat's name at its
  next list read; the control he lacked is the project's Site row.
- **Slice 3 — navigation, the "site switch"** (V9, V10, V11, and V8's twin): **built, F4d — §8.** Left as the plan
  leaves it: only the LAST crumb opens "Switch" (the site crumb goes up when a project is open), and rotation.
- **Slice 5 — S5 keeps the client's name**: **built, both halves — §11** (Amit, 24 Nov 2026: "Client rename"). What
  follows is the plan as it stood on 23 Nov; the warning at its end holds only until every phone that made a client
  has synced once with the build that carries §11 (its backfill sends the name). (V4, V16.) The client of a
  project made on the phone exists only on that phone: S5 holds customer "" for it, so a reinstall or a second phone
  files it under "(no client)". A `client_name` on the S5 project (first value from the upload while blank, a guarded
  `PUT …/client`), read by `S5Masters.fetch` before the capture row; the S5 key STORED instead of re-derived
  (**the key half is done — §10.1**: the phone remembers each key and the client it is for; S5 itself still holds no
  client name). Until then: **do not use Clear storage or reinstall** on a phone that made a client — its capture
  rows, and now the keys remembered from them, are the only copy of that client's name.
- **Later, small.** Photos joined by project id, else client and title (V15; today the title alone, at eleven
  sites and in `TreeKeep.projectOf`) — before any title rename. A site row takes a bench docname only from a row keyed
  under its own name (V12: on a live-pair phone a site S5 named differently carries the WHOLE ERP site's docname, so
  its delete would cascade the sibling projects listed elsewhere). `distinctBy` the folded key, not lower case (V17).
  Rotation and process death reset the tree to Clients; the room chooser is drawn only inside a room (audit R11); the
  rename dialog blanks the tree behind it; S5's customer id is shown as a client's name (V19).
- **Found while building F4b, and left** (none was in the plan's two slices):
  - *A bench delete with no live pair* (**fixed — §10.5**). A row with a bench docname that S5 does not hold still routes to the bench
    (`DeleteRoute.Server`); with no pair the dialog then says "no server configured" in red and offers the cascade
    button, which cannot work either — the delete twin of V6. After F4a the only such rows on a phone with no live
    pair are an ERP-only project kept by an unsent capture, and its site. `Deletes.route` would take `erpLive` as
    `Renames.route` does, with a sentence per kind.
  - *The delete's refusal arrives on the tap.* The dialog draws "Deleting removes it from ERP for everyone" and its
    Delete button before `Deletes.route` is asked, so for a row S5 holds the refusal replaces them only after the
    tap. The rename's reason is drawn from the moment the dialog opens; the delete's is not.
  - *A local CLIENT rename does not move the sites' chosen rooms.* They are kept under client and site
    (`Catalogue.scopeKey`); a site rename moves them, a client rename never did, so the rooms grid of each site of a
    renamed phone-only client falls back to its 2 BHK preset.
  - *Two phone-only projects of one client with one title, at two sites.* A capture carries no site, so
    `Renames.rekey` renames the captures of that client and title whichever of the two rows was renamed. It is V15's
    ambiguity (photos joined by title), not a new one.
  - *A bench rename (live pair) re-keys nothing on the phone* — V18, unchanged: `Renames.rekey` is called on the Local
    route only.
  - *A local rename racing a running upload* (found by the review of F4, and left): §10.9 — no longer a fork since the
    key is remembered; that one upload carries the old title.
- **Found while building F4c and F4d, and left:**
  - *A project S5 no longer lists cannot be removed from the phone* — §9, with the rule proposed (**built — §10.3**).
  - *The project's dialog has one way in, a long press on its row* — and since F4d the row of a site's ONLY project
    is reached through the site's name in the crumb rail (§8.5, 1). A visible way from inside the project (the Site
    line on the project's own sheet, say) is a screen change and was not made (**made — §10.2: the Site chip**).
  - *`QuoteStageSync.refresh` reads S5's list but adds no project to the masters*; only `S5Masters.fetch` does (the
    app's start, a sync's S5 leg, a rename's or a move's read-first). So a project made on the phone stays the
    phone's own row in the tree — shut as "on S5, or on its way there" — until one of those runs after its upload.
    The move's read-first is the one place that reads on demand; opening the project does not, because a phone's own
    row carries no id to read S5 with.
  - *A move through S5 on a phone with a live pair* leaves the bench's site link as it was, and `SiteRows.listed`
    then lists the bench's row under S5's name when every project linked to it moved (V12's shape). No phone holds a
    live pair.
  - *The site rename's lines are a move's too* (`refuse.site.signed_out`, `refuse.site.offline`): "not saved", and
    "Save again" where a move has no Save.
  - *Search's pick and the Work tab's Resume* set the place themselves and are not asked of `TreeNav`.
- **Not the phone's to fix.** V3 on a phone that bootstraps (the bench sends every Mallet Site: a filter in
  `bootstrap()`, a bench deploy, not worth it while no phone holds a live pair). V18 (the bench's client rename edits
  the customer's name and not its docname, and leaves the phone's rows; needs a live pair). The old test project on
  S5 stage (S5 has no project delete).

## 6. What is not proven

- **Nothing was seen on a phone**, and `:app` was not compiled: CI is its only compiler. The scratch harness
  compiled the real `S5Session.kt`, `Catalogue.kt`, `CaptureStore.kt`, `S5Sites.kt` and `JsonRead.kt` of this branch
  and ran `S5Masters.fetch` over a made-up stale bootstrap (three ERP clients, one project S5 lists, one unsent
  capture, one S5-only project) — the cut, the merge, a second read giving the same masters, an unsent capture's
  project leaving once the capture is sent, no answer from S5 keeping everything. For F4b it ran the rename and
  delete routes through the real `Catalogue.onS5` for every row of §3.3, a local client rename and a local project
  rename with the real `CaptureStore.setNames` (over an in-memory stand-in for SQLite), and the signed-out case. The
  changed lines of `MainActivity.kt`, `SyncWorker.kt` and `HomeScreens.kt` were type-checked only as statements
  mirrored in that harness, argument for argument; `TreeShell.kt` changed in a comment alone.
- **The shut dialog of a client and of a project has never been drawn.** `RenameDialog` draws SHUT for any kind (the
  name in the field's disabled tones, Save off, the reason in the grey line's place) and has done so for a site
  since 0.3.318; for a client the reason now stands directly above the existing note "A client is the office's
  customer record, so it is not removed from a phone…", two paragraphs that both speak of the office. How the
  130-character reasons wrap at 360 dp, and in landscape, is the phone's to show.
- **The six new sentences are Claude's wording**, English only (§4). Whether "on S5, or on its way there" reads
  plainly to a site engineer is Amit's to judge on the phone.
- **"On its way" is read from the key, not from the upload.** A capture counts as S5's from the moment a sync gives
  it a phone-minted key, whether or not its upload has started (a 360 whose survey prep is not saved is keyed and
  never queued). So a phone-made client is shut after its first capture has been through one sync, and stays shut
  until Slice 5: the rename it loses is one that would have forked it.
- **Whether his phone's masters hold a stale bootstrap** was never read. If they do not, this slice changes only the
  Demo Client, the suggestions, the recents and the Sent list on his phone.
- **A row typed on the phone that an ERP project used to hide.** The tree hides a local row while a masters project
  of the same client and title exists. Where such an ERP-only project now leaves, a local row of the same name that
  was hidden under it shows again, with its Offline pill. The ERP leg pruned covered rows while the pair worked, so
  this needs a row typed after the key died that repeats an ERP project's client and title; not reproduced.
- **A stored pair that died and was never probed** still reads as live at app start (the flag is unset until a sync's
  probe refuses it), so that start takes the old path once; the sync that follows cuts the tree.
- **The Queue's Sent list still names every client a capture of this phone was sent for**, the other clients'
  included: it is a history of what was sent, not the tree. Left as it is; said here so it is not a surprise.

## 7. Slice F4c as built — one project, one site

The plan's Slice 2, its own commit over F4b. Closes V1.

### 7.1 What was wrong

A site is not an entity on S5 or on the phone: it is the name each project carries. The phone could rename a site
(every project of it at once) and could not give ONE project a site. A project S5 lists with no site name is filed
under the "(no site)" bucket; the bucket cannot be renamed (it would name every unnamed project at once), the seed
and the upload send nothing for it, and no screen moved a project. That is where a project of his own sat, outside
its flat's site, with nothing he could press. (The data was corrected on S5 stage the same evening by one staff
write; the missing control is this slice.)

### 7.2 The rules

`pano/RenameRoute.kt`, pure, tested by `MoveRouteTest` (new):

```kotlin
object Renames {
    fun moveShut(tx: Tx, project: String, held: Held?, hasLocalRow: Boolean, mastersOnly: Boolean, erpLive: Boolean, demo: Boolean): String?
    fun moveRoute(tx: Tx, project: String, held: Held?, hasLocalRow: Boolean, mastersOnly: Boolean,
                  erpLive: Boolean, signedIn: Boolean, demo: Boolean, from: String, to: String,
                  onWay: Boolean = false, listRead: Boolean = false): RenameRoute
    fun siteChoices(sites: List<String>, current: String): List<String>
    fun moveTarget(typed: String, sites: List<String>): String
}
```

A move is "give this ONE project that site name": the write a site's rename makes once per project
(`PUT /api/asis/:project/site {site, base}`, S5 QUOTE slice A1), made for one project with that project's own base.
`moveRoute` answers with the routes a rename has, in this order:

1. **Shut** (`moveShut`, asked when the dialog opens, before any site is picked) → `Blocked(…, SHUT)`: the demo's
   project; a row that is neither S5's, nor an entry of the masters, nor made on this phone; and an entry of an ERP
   bootstrap that S5 does not list **on a phone with a live ERP pair** — its site is the bench's, the bench moves no
   project (it only fills a site that is blank), and the next bootstrap would put the old site back over anything
   changed here. One sentence for all three (`app.project_site_not_changed_here`).
2. **The target** → `Blocked(app.not_moved_pick_another_site, PHONE)`: a blank name, the "(no site)" bucket (a bucket
   is not a place a project is moved to) and the site the project is already at, names as the catalogue compares
   them. Never sent, never "Moved".
3. **S5 lists the project** (`held` not null) → `RenameRoute.S5` with ONE write, its base S5's name for THIS project
   exactly as S5 gave it — `''` when S5 holds none, which is the bucket's projects. Signed out:
   `Blocked(refuse.site.signed_out, S5)`, known on the phone, so nothing is sent.
4. **An entry of an ERP bootstrap S5 does not list, with no live pair** → `RenameRoute.Local`: the phone's copy of
   the entry takes the name (below, decision 2).
5. **A row made on this phone that S5's list has not named** → `RenameRoute.Local`. But where a capture of the row
   already carries a phone-minted S5 key (`onWay`), the key is a hash of client|site|title and a row moved now would
   mint a NEW key at its next capture — two S5 projects (audit R9, as for a site). So `S5(emptyList(), readFirst =
   true)`: S5's list is read first and the route asked again with `listRead`. Listed now, it is S5's project and
   takes the write (3); still not, `Blocked(app.project_capture_on_way_not_moved, S5)`.

Never `RenameRoute.Legacy`: the bench is not told of a move, as it is not told of a site's rename.

`siteChoices`: the client's NAMED sites other than the one the project is at, in the tree's order, each once — never
the bucket. `moveTarget`: a typed name that is one of the client's sites (as the catalogue compares names) means
that site in ITS spelling, so "flat 9" typed for a client who has "Flat 9" joins that site and does not give S5 a
second spelling of it.

### 7.3 Where `:app` asks them

| Site | What changed |
|---|---|
| `app/Catalogue.kt` `moveFacts(masters, captures, p)` (new, private) | Gathers what the rule is asked with, for the project as the tree lists it. **An entry of the masters**: S5's name for its id (`S5Sites.held`), and `mastersOnly` = no name held and the entry is a bootstrap's (`bootstrapEntry`: not one a read of S5's list added). **The phone's own row**: the S5 project it IS, where S5's list has named one — by the project id a capture of the row carries (the capture's site read from the phone's row for its client and title, never the demo's), else by the key the row would have on S5 (`S5Auth.localKey`); and `onWay` = a capture of the row carries a phone-minted key that `S5Sites` does not hold. |
| `app/Catalogue.kt` `moveShut(tx, masters, captures, p, erpLive)`, `moveRoute(tx, masters, captures, p, to, signedIn, erpLive, listRead)` (new) | The one caller of each rule, through `moveFacts`. `from` is the site the TREE lists the project under. |
| `app/Catalogue.kt` `projectOf(masters, serverId, client, site, title)` (new) | The project a dialog's target is, as the tree lists it NOW: by its server id, else by client, site and title, else by client and title (the phone's own row has become S5's since, or its site moved under it). |
| `app/Catalogue.kt` `moveLocal(client, site, project, to)` (new) | The phone's own row says the new site. One row per client, site and project: a row already standing at the target is the same row. The moved row takes the type and address of the site it joins, and at a site nobody has named yet it carries no address. |
| `app/Catalogue.kt` `setMastersSite(masters, projectId, to)` (new) | The `site_name` of a bootstrap's entry, and nothing else of it; never an entry S5's list added. The caller saves the masters. |
| `app/MainActivity.kt` `moveProject(t, typed)` (new) | Re-finds the project in the tree (`projectOf`), takes `Renames.moveTarget`, asks `cat.moveRoute`, and follows it: `Local` → `moveOnPhone`; `S5` → the list read first where the route says so (`S5Masters.fetch`, then `projectOf` and the route again on the masters that read gave), then `SiteRename.rename(context, s5.writes, to)` — the site rename's own sender, one PUT — and on `Outcome.Done` the phone's own row follows (`moveLocal`, wherever that row stands) and `moved`; anything else `reload()` and the reason. `Blocked` → the reason. |
| `app/MainActivity.kt` `moveOnPhone(p, to)` (new) | `cat.moveLocal` and `cat.setMastersSite` (saved when it changed something); neither moved anything → a failure, never "Moved". Then `cat.moveRoomScope(client, from, to, leave = nothing is listed under the old site any more)`. |
| `app/MainActivity.kt` `moved(p, name)` (new) | `reload()`, the open client and site become the project's, an OPEN project is re-read from the tree, the banner `app.msg.moved_to_x`, the dialog closes. |
| `app/MainActivity.kt` `moveFailed(why)`, `moveWhy` (new) | The reason under the Site row; cleared in `closeRename()` and when another site is typed. |
| `app/MainActivity.kt`, the dialog's call | `moving` (the tree's project, for kind project only), `moveShut`, `moveSites`, and six arguments to `RenameDialog`: `site`, `siteChoices`, `moveShut`, `moveWhy`, `onMoveEdit`, `onMove`. |
| `app/TreeShell.kt` `RenameDialog` | Six new parameters, all defaulted. Under the name and its grey line, when `site != null && onMove != null`: a divider, the label `Site` (`common.site`), and a row with the site's name (through `Masters.site_name`, so the bucket reads in the language shown) and a down chevron. A tap unfolds, IN the dialog: the head `Move the project to` (`app.move_project`), the client's other sites one tap each, a field `New site` (`common.new_site`) and the button `Use this site` (`app.use_site`). Shut: no chevron, no picker, `moveShut` under the row. `moveWhy` in red under the row. Nothing of the row reads whether the NAME is shut. |

### 7.4 What Amit sees

- **A project in "(no site)"** (S5 lists it, with no site name): long-press it → the dialog. Its name is greyed with
  the office's sentence, as F4b left it. Under that: `Site` / `(no site)` ▾. Tap → `Move the project to` / the
  client's sites. Tap his flat's site → "Moved to <site>": the project is listed there at once, he is taken to that
  site's project list, the bucket is gone if it was the last project in it, and the web's `/projects` and any other
  phone say the same after their next read.
- **A project at the wrong site**: the same, and its old site keeps its other projects.
- **A site the client does not have yet**: typed in `New site`, then `Use this site`.
- **A failure** is said in red under the Site row and the dialog stays open, nothing changed: not signed in to S5;
  offline; S5's own words when somebody renamed the project's site meanwhile (the row then shows the site S5 holds,
  and picking again sends against it).
- **A project he typed on site and has not sent**: moved on the phone, with or without a signal. The rooms chosen
  for the site it leaves go with it where the site it joins has none.
- **A project he photographed minutes ago** (uploaded, but S5's list not read since): the move reads S5's list first
  and then moves it on S5 — one tap for him. If the photo has not gone up: "A photo of this project has not reached
  S5 yet — move it once it has; not moved".

### 7.5 Decided by Claude, not by Amit

1. **The picker unfolds inside the dialog**, not as a second sheet over it. Nobody could see a sheet over a dialog
   on a phone from here, and an unfolding list cannot land behind its own window. The plan said "a row … opening
   the client's sites plus New site"; where it opens was open.
2. **A bootstrap-only entry.** The brief for this slice read "cannot exist any more after Slice 1"; it can — F4a
   keeps an ERP-only project while a capture of it has not reached a server. Met on a phone with **no live pair** the
   route is `Local`: the phone's copy of the entry's `site_name` is the only name the phone has for it, and it is
   what the capture's upload carries to S5 as S5's first value (`Catalogue.siteFor`), so the move reaches S5 with the
   project. Met **with a live pair** the row is shut. `MoveRouteTest` pins both, and the order: `mastersOnly` is asked
   before `hasLocalRow`, because a hidden row of the phone's behind a bench entry, moved alone, would say "Moved" and
   change nothing on screen.
3. **The entry's link to the bench's site row is left as it is.** `SiteRows.listed` then lists that bench row under
   the name its projects are listed under — the designed read-time rule for a renamed site — so no empty site of the
   old name is left (run in the harness). The cost is V12's, unchanged: such a site row shows the bench row's type
   and docname.
4. **A blank target, the bucket and the same site are a refusal under the row (`Line.PHONE`), not SHUT.** The plan
   listed them with the demo as "shut". SHUT means the row has no picker; these are a wrong pick, and the next pick
   can be right. The dialog cannot produce the first two by a tap (the choices never hold them, and the button is off
   while the field is blank); the sentence is for a name typed.
5. **`onWay` and `listRead` were added to the plan's signature**, defaulted. Without them a row whose capture is
   keyed for S5 would be moved on the phone alone and fork at its next capture — the fault F4b closed for renames.
   The read-first is the site rename's own (`S5Masters.fetch`), so it has a fourth caller; `TreeKeepTest`'s count
   moved 3 → 4 with the reason beside it.
6. **The phone's own row follows its project after an S5 move — wherever the row stands** (`localSiteFor`), not only
   a row under the old site. Signed out, the masters drop what S5 listed and the phone's row shows again: it then
   shows at the site S5 holds, and its move says to sign in (run in the harness). What it costs is the known residue
   of a site rename (audit R9's): the row's re-derived key no longer matches the key S5 holds, so with no capture of
   the project left on the phone AND nobody signed in, the row looks like the phone's own. Slice 5 stores the key.
   (**The review found this was worse than a look — the next capture forked the project — and the key is stored now:
   §10.1.**)
7. **The place follows the project.** After a move he is at the project's new site (its project list, or the project
   itself if it was open), not left in a site the project just left — which may no longer exist.
8. **The move of the phone's row carries the chosen rooms, not the address.** Rooms: as S5's names already move them
   (`moveRoomScope`; the old site keeps its list while anything is listed there). Address: a site nobody has named yet
   gets none — an address is the flat's it was typed for.
9. **No new sentence for signed-out, offline and S5's 409**: the site rename's own lines
   (`refuse.site.signed_out`, `refuse.site.offline`, `Renames.text`). They end "not saved" and one says "Save again",
   where a move has no Save; they are the approved lines of the same write, with their Marathi. If they read wrongly
   on the phone, each is one key.
10. **`app.move_project` ("Move the project to") is reused** as the picker's head. It is the prototype's key for the
    stage sheet that v61c removed, had no reader on the phone, and says exactly this. `Use this site` is the New site
    dialog's own button.
11. **The four new sentences** (§7.6).

### 7.6 The new texts

Four keys, each a phone text, English first: the Marathi value of each is its English, and each is listed in
`android/i18n/mr-pending.tsv` (37 → 41). No existing text changed.

| Key | English |
|---|---|
| `app.msg.moved_to_x` | Moved to {site} |
| `app.project_site_not_changed_here` | This project's site cannot be changed from this phone: S5 does not hold the project and it was not made here. |
| `app.project_capture_on_way_not_moved` | A photo of this project has not reached S5 yet — move it once it has; not moved |
| `app.not_moved_pick_another_site` | Not moved: pick another site, or type a new site's name. |

Reused, with their Marathi: `common.site`, `common.new_site`, `app.use_site`, `app.move_project`,
`refuse.site.signed_out`, `refuse.site.offline`.

### 7.7 Tests

`:pano` 114 classes, 1,644 tests → **115 classes, 1,659 tests**, all green.

- `MoveRouteTest` (new, 15): a project S5 lists moves by ONE write with its own base, whatever else is true of it;
  the bucket's project with base `''`, and the body S5 takes; signed out; the phone's own row; a capture on its way
  (read first, then S5 or the refusal); a bootstrap-only entry with and without a live pair; shut, and that the
  route's shut IS `moveShut`'s sentence for every combination of the facts; the target (blank, the bucket in four
  spellings, the same site in three) for each kind of project; never the bench and never more than one write, over
  every combination; `siteChoices`; `moveTarget`; the four texts; and three source tests of `:app` (the gathering;
  the handlers; the dialog's row).
- **A pin that moved, with its reason beside it:** `TreeKeepTest` — the callers of `S5Masters.fetch(` are four, not
  three.

### 7.8 What is not proven (F4c)

- **Nothing was seen on a phone** and `:app` was not compiled. The scratch harness compiled the real `Catalogue.kt`,
  `S5Sites.kt`, `S5Session.kt`, `CaptureStore.kt`, `JsonRead.kt` and — new — `SiteRename.kt`, with `MainActivity`'s
  four handlers mirrored statement for statement, and ran them over a stand-in for S5 written from S5's own rule
  (`setSite`: the base compared exactly; the same name `written: false`; a stale base 409 with the name as stored):
  the bucket's project moved by one PUT with base `''`; signed out, offline, the same site and the bucket refused
  with nothing sent; a 409 said, the tree redrawn from it, and the second pick sent against the stored name; an open
  project re-read; a phone-only row moved with its rooms and no address; the read-first case; the capture still on
  its way; the bootstrap-only entry with and without a pair. S5 itself was not called.
- **The Site row has never been drawn.** It is Compose written without a compiler: a `Row` with `clickableRow`, an
  `Icon`, an `OutlinedTextField` and a `TextButton`, each as the same file already calls them. How the unfolded
  picker sits in the dialog at 360 dp with five sites, and in landscape, is the phone's to show; the body scrolls.
- **The row is below the name's reason.** For a project of his, the dialog reads: the name greyed, the office's
  sentence, then `Site`. Whether the row should stand first is his to say.
- **The way in is a long press.** The project's dialog opens by a long press on its row in the site's project
  list, as it did; nothing else opens it.
- **A live-pair phone** moves a project S5 lists through S5 as well, and the bench's own site link is not told
  (the plan's "does not fix"). No phone holds a live pair.

## 8. Slice F4d as built — navigation, the "site switch"

The plan's Slice 3, its own commit over F4c. Closes V9, V10, V11 and V8.

### 8.1 What was wrong

The place the app is at was four loose values in the screen — two NAMES, a COPY of the project taken the moment it
was opened, and a room — moved by rules written inline, each a little different.

| # | The fault | Closed by |
|---|---|---|
| V11 (audit R12) | Opening a client with one site looked that site up BY NAME ACROSS EVERY CLIENT: the first project in the whole tree at a site of that name decided whose it was. A client whose only site was called what an earlier client's was opened the other client's project, under his own crumbs — and a photo taken there was filed to the other client. | `TreeNav.openSite` asks that client's projects alone; the cross-client function is deleted |
| V9 | Opening a client went down through a single site and a single project; tapping a site row did not; Back skipped a one-project list either way — so Back skipped a list he had just walked through. The breadcrumb's last pill always drew a chevron, though a tap opens "Switch" only with a sibling to switch to: on a site holding one project, a chevron with nothing behind it. The Switch sheet's rows did not scroll. | `TreeNav.openSite` / `back` are one rule read both ways; `TreeNav.switches`; a scrolling column |
| V10 | Nothing re-read the place when the tree changed under it: a project whose site S5 renamed, or that a sync re-keyed, kept its old crumb, and Back from it showed the old site's list, empty. And the read made when a project is opened heard S5's site names but redrew only the quote line. | `TreeNav.settle`, in one effect keyed on the tree; `Renames.namesChanged`, then `reload()` |
| V8 | "New site" with the client and title of a project the tree already lists built a second project BY HAND that was no row of the tree: it showed the real one's photos under the site just typed, its phase strip took no tap, and Back left it for good. The row it left on the phone gave the real project's next upload the wrong site. Opened from inside a bucket, the dialog was prefilled with "(no site)" as though it were a name. | `TreeNav.existing`, `prefill`, `siteTyped`, `offersMove`; the project is read from the tree |

### 8.2 The rules

`pano/TreeNav.kt` (new), pure, tested by `TreeNavTest` (new):

```kotlin
object TreeNav {
    data class P(val key: String, val client: String, val site: String, val title: String)
    data class At(val client: String?, val site: String?, val project: String?)
    val ROOT: At
    const val NO_CLIENT = "(no client)"
    fun openSite(client: String, site: String, projects: List<P>): At
    fun openClient(client: String, sites: List<String>, projects: List<P>): At
    fun back(at: At, sitesOf: (String) -> List<String>, projects: List<P>): At
    fun settle(at: At, open: P?, clients: List<String>, sitesOf: (String) -> List<String>, projects: List<P>): At
    fun existing(client: String, title: String, projects: List<P>): P?
    fun switches(isLast: Boolean, siblings: Int): Boolean
    fun prefill(name: String?): String
    fun siteTyped(typed: String, fallback: String): String
    fun offersMove(typed: String, at: String): Boolean
}
```

and in `pano/RenameRoute.kt`: `Renames.namesChanged(before: Map<String, Held>, after: Map<String, Held>): Boolean`.

- **Down.** `openSite`: the site, and through it into its project when it holds exactly one — asked of THAT client's
  projects only. `openClient`: the client's sites, or straight through a single site.
- **Back** is the same rule read upwards: from a project to its site's list unless that list is this project alone;
  from a site to the client's sites unless the client has one site; from a client to the clients. So
  `back(open(x))` is where `open` started, and Back never stops at a list of one.
- **Settle** — the place found again in the tree as it is now. A project open: by its key; else by the client and
  title of the project as it was opened (the phone's own row took S5's key at a sync; or its key was its names and
  its site changed), at its old site if more than one matches. Found: its client, SITE and key as the tree lists
  them now. Not found: one level up. A site open: it stays while the client still has a site of that name, else the
  client's sites. A client open: it stays while the tree lists it, else the clients. A name that still matches is
  given in the tree's own spelling.
- **`existing`**: the project a typed client and title already is, at whatever site — as the tree itself hides the
  phone's own row behind a listed project of the same client and title at ANY site, and a capture joins its project
  by those two.
- **`switches`**: the last crumb, with another of its level to switch to.
- **`prefill`**: the open client's or site's own name, and nothing for "(no site)" or "(no client)". **`siteTyped`**:
  the site a NEW project is filed under — the name typed when it is a site's own, else the default. **`offersMove`**:
  a site was typed, it is a site's own name, and it is not the site the project is at.
- **`namesChanged`**: some project's site name differs between two readings of what the phone holds of S5's names.
  Not a change: a project heard for the first time with no name, and a newer stamp on the same name.

### 8.3 Where `:app` asks them

| Site | What changed |
|---|---|
| `app/MainActivity.kt`, beside `cs` | `treePs` = the tree's projects as `TreeNav.P`. **`LaunchedEffect(cs)`**: `TreeNav.settle` of the open place; where it differs, or the project object does, the client, site and project are replaced — the project by the TREE's object, so a copy that still says the old site goes even where the key stands. No project any more: its room, capture and face close with it. A wish list open on a project that took a new key follows it to that key. Composed above every early return (the search overlay, the tabs, the rename dialog). |
| `app/MainActivity.kt` `LaunchedEffect(navProject?.serverId)` | S5's names are read before the list read (`S5Sites.all`) and compared after: `if (read && Renames.namesChanged(…)) reload()`. |
| `app/MainActivity.kt`, the tree branch | `go(to: TreeNav.At)` applies a place, the project read from the tree by its key. `openClient(c)` and the new `openSite(c, s)` are one line each over `TreeNav`; the `BackHandler` is `go(TreeNav.back(…))`. The site row's `onOpen` and the site crumb's `onSibling` call `openSite`. The inline skip rule and `openSiteInto` are deleted. |
| `app/MainActivity.kt`, the New site dialog | Prefilled through `TreeNav.prefill`. `onSave`: `TreeNav.existing(client, proj, treePs)` first. A project the tree lists is opened as itself — nothing is added to the phone's rows — and where `TreeNav.offersMove(site, real.site)`, its dialog is opened with `moveTo` = the site typed. Otherwise the new row is added under `TreeNav.siteTyped(site, DEFAULT_SITE)` and `navProject` is read back from the tree (`cat.projectOf`). The dialog now hands back the site AS TYPED (blank when none), so "no site typed" can be told from one typed. |
| `app/MainActivity.kt` `RenameTarget` | `moveTo: String = ""`. |
| `app/TreeShell.kt` `RenameDialog` | `moveTo: String = ""`: the Site row's picker opens unfolded with that name in its field, under the line `app.project_exists_use_site`. |
| `app/Crumbs.kt` `CrumbRail`, `CrumbPill` | `val switches = TreeNav.switches(isLast, c.siblings.size)` is read by the tap and handed to the pill as `chevron`; the pill draws the chevron `if (chevron)`, no longer `if (current)`. The Switch sheet's rows are in `Column(Modifier.heightIn(max = 460.dp).verticalScroll(…))`, as the app's other sheets are. |

### 8.4 What Amit sees

- **A client never opens another client's project.**
- **Tapping a site that holds one project opens the project**, as tapping a one-site client always did. Back from
  it goes to the list he tapped in — never past it, and there is no one-row list in between.
- **No dead chevron.** The last crumb carries one only where a tap opens "Switch". The only project of its site
  has none.
- **A project whose site changed while he was in it** — S5's own answer to a move from another phone or the web,
  heard when he opened the project or at a sync — keeps him in the project, the crumb says the site it is at now,
  and Back lists that site.
- **A site that was renamed or emptied while its list was open** is not left on screen as an empty list under a
  name that is gone: he is at the client's sites, where it stands under its new name.
- **Typing an existing project into New site opens the real one.** If he typed another site for it, its dialog is
  open on the Site row with that site in the field, and says: "This client already has this project. To move it to
  the site you typed, tap Use this site." One tap moves it; Cancel leaves him in the project where it is.
- **New project from inside "(no site)"** starts with the site blank, and is filed under "Main site" if he types
  none — never under a site called "(no site)".
- **A long list of clients in Switch scrolls.**

### 8.5 Decided by Claude, not by Amit

1. **A site row's tap descends through a single project.** The plan's rule ("openSite … descends through a single
   project", "back is the exact inverse"); the other way to make the two directions agree — Back remembering how he
   came — needs a history that a re-read of the tree would then have to settle too. **What it costs, and the first
   thing to judge on the phone:** the project LIST of a one-project site is no longer what a tap on the site shows.
   It is reached by the site's name in the crumb rail, from inside the project — and that list is where a project is
   long-pressed for its dialog (rename, the Site row, delete) and where "New project" stands. A one-site client's
   project was already reached that way.
2. **`back(open(x))` is "where open started" with a list of one never a stop.** A place reached by the crumb — the
   one-row list above — goes back as if it had been passed through.
3. **Settle steps up; it never guesses a site.** With only a site open and its name gone, he is at the client's
   sites — not carried to "the site that is probably the same one". With a project open he is carried, because the
   project's key says which one it is.
4. **No project open, no room open.** Where the settled place has no project, the room, the capture and the face
   close with it. Survey prep is not closed by this: it is its own window over the tree and holds what it was
   opened with.
5. **An open wish list follows its project to a new key; an open album closes.** Both are held by the project's key.
   The wish list is carried so that the sync which first lists a phone-made project does not close it under his
   hand (the stale copy used to keep it open). The album is not: `FeaturesTest` pins that it is opened in one place
   and left in one, site progress is off, and a third write of its key was not worth moving that pin for.
6. **An existing project is found by client and title at ANY site** — the tree's own rule for hiding a row, and the
   upload's for choosing a project. So two real projects of one client with one title at two sites cannot be made
   from New site; the second opens the first. That is V15 (photos joined by title), unchanged and now said.
7. **The move from New site is offered, never made.** He typed a site for a project that exists somewhere else; the
   app does not decide that he meant to move it.
8. **A blank site in New site is "Main site" for a new project and "no site typed" for an existing one.** The dialog
   used to hand back "Main site" for a blank field, which would have offered to move every existing project there.
9. **`Renames.namesChanged` is its own rule**, one line: what counts as a change of the TREE is stated and tested,
   not left to a map comparison that a newer stamp on the same name would trip.
10. **The one new sentence** (§8.6), and that it quotes the button's own words.

### 8.6 The new text

One key, a phone text, English first (`mr-pending.tsv` 41 → 42). No existing text changed.

| Key | English |
|---|---|
| `app.project_exists_use_site` | This client already has this project. To move it to the site you typed, tap Use this site. |

### 8.7 Tests

`:pano` 115 classes, 1,659 tests → **116 classes, 1,675 tests**, all green.

- `TreeNavTest` (new, 16): the way down; **two clients, one site name** — the fixture is the fault's (another
  client's project comes first under that name), and no client or site of the tree opens a project that is not its
  own, in either order of the tree; `back(open(x))` for every node of a made-up tree; Back never stops at a list of
  one and reaches the clients in at most three strictly higher steps from every place; settle — no change (every
  place, and the tree's spelling), a renamed site with a project open and with only the site open, one project
  moved, a key that changed (the phone's row that became S5's, a row whose site moved, a twin, two rows of one
  title, never another client's project), what is no longer in the tree (a project, a site, a client), and that
  whatever it is given the answer is a place of the tree; `existing` and `offersMove`; `prefill` and `siteTyped`,
  with `NO_CLIENT` pinned to `:app`'s; `switches`; `namesChanged`; and four source tests of `:app` (the moves; the
  settle and the read on project open; New site; the breadcrumb).
- No existing pin moved.

### 8.8 What is not proven (F4d)

- **Nothing was seen on a phone** and `:app` was not compiled. The harness ran `MainActivity`'s navigation mirrored
  statement for statement over the real `Catalogue.Snapshot` and `TreeNav`: the old cross-client rule reproduced on
  a made-up tree and the new one opening the right project; Back from each kind of place; the chevron's answer; a
  project moved by the office while he was in it (the list read, `namesChanged`, the re-read, the place following
  it, Back listing the site it is really at); a site renamed while its list was open; New site with an existing
  project (opened as itself, no row added, the move offered and then made by one more call); a new project from
  inside the bucket; a phone-only site whose last project was removed.
- **What stood in for a compiler, for F4c and F4d alike.** Beyond the mirrored run, the EXACT text of every handler
  these two slices added to `MainActivity.kt` — the four move handlers, the dialog's three values and its new
  arguments, the settle, `go` / `openClient` / `openSite` / Back, New site's `onSave`, the read on project open —
  was cut from the file by a script and compiled against the real `Catalogue`, `S5Sites`, `S5Session`,
  `CaptureStore`, `SiteRename` and the real `:pano` jar, with stand-ins only for Compose's state, `remember`,
  `scope.launch` and `withContext`; and the Site row's exact text was compiled over stand-ins for the Compose calls
  it makes. Both came out clean. Neither is CI: Compose's own signatures, the imports of `TreeShell.kt` and
  `Crumbs.kt`, and how the pieces sit in composition are CI's to say. The least sure lines are `TreeShell.kt`'s
  Site row (a `Row` whose modifier is an `if` … `else` expression followed by `.heightIn`) and `Crumbs.kt`'s
  scrolling `Column` with its new `verticalScroll` import.
- **The settle runs in a `LaunchedEffect`**, one frame after the tree changes: for that frame the screen draws the
  old place over the new tree, as it did for good before this slice.
- **Rotation and process death** still reset the tree to Clients (V19); nothing here keeps the place across them.
- **The site crumb still goes up** when it is not the last crumb, as the plan leaves it; only the last crumb
  switches.
- **Search's pick and the Work tab's Resume** still set the place themselves, from a hit or a recent that names a
  project of the tree by its key. A place they set wrongly would be settled at the tree's next change, not at once.

## 9. Checked, not fixed: a project made on this phone that S5 no longer lists

(**Fixed since, by the rule proposed at the end of this section — §10.3.** What follows is the check as it was run.)

Asked with this build: a project made on a phone, uploaded, and since removed from S5 by the office (S5 has no
project delete; the office removes one by a guarded one-off fix of its rows) may still have its row and a sent
capture on the phone. Since F4b a phone-minted key "counts as S5's, sent or not". Can he remove such a project?

Run in the harness on made-up names (a client, a site, one project made on the phone, its capture sent, S5 listing
it, then S5 not listing it, then one read of S5's list with no live pair):

- **What the tree shows.** The project is still listed — as the phone's OWN row again, with its Offline pill, under
  the client and site it was typed under. The masters entry S5's list had added is gone with the read; the phone's
  row was only hidden behind it.
- **Its name is shut and its delete is refused**, with F4b's sentences ("… is on S5, or on its way there …"):
  its capture carries the phone-minted key.
- **After he deletes that capture** (the capture screen's Delete removes the row and its files at once), **the
  delete is still refused, and so is its site's.** Why, exactly: `Catalogue.onS5`'s second half asks whether S5's
  list has named the key the phone's row WOULD have on S5 (`S5Auth.localKey` of its client, site and title — the
  very key it was uploaded under), and "named" is `S5Sites` holding an entry for it. **`S5Sites` never removes an
  entry**: its own header says "Nothing is removed: … S5 has no project delete". So a project S5 once listed reads
  as listed for ever.
- **Its Site row** sends the one write to the key S5 no longer holds, and S5's own 404 is said under the row
  ("This project has never been uploaded").

So he cannot remove it from the phone. He can leave it; nothing uploads from it once its capture is gone.

**Not fixed here, because it is not one small rule.** The obvious rule — "a read of S5's whole list forgets the
names of projects it no longer lists" — is wrong as written in two ways that the store's own header already warns
of: reads overlap (a list read sent before a project's first upload can land after the answer that named it, and
would un-list a project S5 holds — which opens, for that window, exactly the local rename F4b shut), and the list
is the list of whoever is signed in (a second login's shorter list would forget the first's projects). **The rule
proposed, for whoever takes it:** the DELETE asks S5 at the moment it matters, as a site's rename does (`listRead`):
where a row's only tie to S5 is a key `S5Sites` once heard — no capture of it on the phone carries a key — the
dialog reads S5's list first, and a key that fresh list does not name is not on S5: the row is then the phone's
own and is removed here. A capture still on the phone keeps the refusal, and the sentence should then say to
delete the capture first. `Deletes.route` gains `listRead` and the fresh list's answer; nothing else reads it.

## 10. Review of F4 and what was done

A behaviour reviewer and a compile reviewer read the four slices (the compile reviewer found nothing). The behaviour
reviewer confirmed nine findings: three major, six minor. Eight are fixed here, as ONE commit over F4d; the ninth is
closed as a fork by the first and recorded (§10.9). Every rule below is a pure function of `:pano` with tests; `:app`
draws and calls. New texts are in §10.10.

### 10.1 MAJOR — an S5 key is remembered, never re-derived

**The fault.** `S5Auth.keyFor` answered a masters entry for the client and title, else `localKey(client, site, title)`:
a hash of the names AS THEY STAND. After a move (F4c moves the phone's own row with its project) or a site rename the
row carries the new site. While a capture of the project is on the phone the masters still tie it to its client and
the old key is found. Once the captures have left the phone S5's list names the project with no customer (S5 holds
none for a project made on a phone), `S5Masters.fetch` files it under "(no client)", no masters entry matches the
client, and the next capture MINTS A SECOND S5 PROJECT. Run in the harness: after the move the names give another
key than the one S5 holds.

**The fix: the key is stored when it is minted; one map and two readers** (the verifying pass's V16).

`pano/KeyBook.kt` (new), pure, tested by `KeyBookTest`:

```kotlin
object KeyBook {
    data class Names(val client: String, val title: String)
    data class Book(val byName: Map<String, String>, val byId: Map<String, Names>)
    fun fold(client: String, title: String): String
    fun keyFor(book: Book, client: String, title: String): String?
    fun namesOf(book: Book, project: String): Names?
    fun wins(stored: String, comer: String, listed: (String) -> Boolean): String
    fun take(book: Book, project: String, client: String, title: String, listed: (String) -> Boolean = { false }): Book
    fun learn(book: Book, captures: List<TreeKeep.Cap>, listed: (String) -> Boolean): Book
    fun renamed(book: Book, kind: String, client: String, title: String, to: String): Book
    fun forget(book: Book, project: String): Book
}
```

- **By name**: the folded client and the folded title -> the S5 project id. NOT the site: the masters are matched the
  same way (a client's title at any site), and the site is the very part a move or a site rename changes.
- **By id**: the id -> the client and title it is for, as typed. The phone's only copy of a client's name for a
  project whose captures have gone.
- **Two keys claimed for one client and title** (`wins`): the one S5's list has named is S5's project; where both are
  named, or neither, the one remembered first stays. Captures are read oldest first, so "first" is the key the project
  was first shot under.
- **An id has one client and title at a time** (`take`): a capture renamed with its row moves the entry and leaves no
  stale name behind, so a NEW project typed under the old names is not joined to the renamed one.

`app/S5Keys.kt` (new): the store. Two prefs files (each is keyed by free text), written entry by entry, under a lock.

| Where | What changed |
|---|---|
| `app/S5Session.kt` `S5Auth.keyFor` | A masters entry for the client and title; else **the key the book holds** (read with every capture on the phone taken into it first); and only then a mint — **remembered before it is answered**. |
| `app/S5Session.kt` `S5Masters.fetch` | Reads the book once. A project S5 lists with no customer, and no capture of it on the phone, is filed under **the client its key was remembered for**; only with none of the three is it "(no client)". The title likewise. |
| `app/Catalogue.kt` `keyOfRow` (new, private) | "What is this phone-made row called on S5": the remembered key, else — for a row no capture has been keyed for yet — the hash its first capture will be given. The ONE such place; `s5ProjectsOf`, `s5Ties` (the old `onS5`'s gathering) and `moveFacts` ask it where each worked a key out for itself. |
| `app/MainActivity.kt` | The captures are read into the book at the app's start (signal or none), and before a capture row is deleted. `renameHere` moves the entries of a never-sent row it renames (`S5Keys.renamed`), after the captures are re-keyed. |

**Fed by**: the mint; every capture row that carries a project id (so the keys minted by earlier builds are in the book
from the first run — the verifying pass found one key on his phone that no longer re-derives from its names; it is
learnt from its capture row); a local rename. **Loses a key only** to the one delete that proved it gone (§10.3). Not
sign-out: the keys are not a person's.

With that in place the row may move with the project, as F4c built it.

### 10.2 MAJOR — a visible way to the project's dialog from inside the project

Since F4d a one-project site opens its project directly, so the list whose long press opens the dialog (Rename, Site,
Delete) is never seen. `RoomsScreen` has ONE new chip, `Site · <name> ▾` (the site through `Masters.site_name`: the
bucket reads "(no site)"), which opens the SAME dialog with the same target the long press builds.

- **It is the FIRST chip of the row** — decided by Claude. The row scrolls sideways and at 360 dp already holds three
  chips before the dates chip; a handle he has to scroll to is not the visible handle he asked for.
- **A project renamed from inside stays open.** `renameHere` set `navProject = null`; it now re-reads the project from
  the tree where one was open (`cat.projectOf`, as a move does). Renamed from the list, none is opened.

### 10.3 MAJOR — a phone-made project that S5 no longer lists can be removed, and its Site row tells the truth

**The fault** is §9's: `S5Sites` never forgets, so a project the office took off S5 read as "on S5" for ever.

**The rule**, in `Deletes.route` (new routes `DeleteRoute.ReadFirst` and `DeleteRoute.Gone`; `Deletes.Tie`,
`Deletes.ListRead`), tested branch by branch in `DeleteRouteTest`:

1. Asked ONLY at a delete, for the keys the refusal rests on (`Renames.heldKeys`: what the row's projects and
   captures carry that S5's list has named or the phone minted, and the remembered keys of its phone-made rows that
   S5's list has named).
2. **A capture of the row still on the phone keeps the refusal** — keyed or not, sent or not: it may still be on its
   way up, so no list can call the row gone. The sentence says to delete the photos first, and that the row is then
   removed only if S5 does not hold it (`app.project_s5_photos_first`, `app.site_s5_photos_first`).
3. With none: not signed in to S5 -> the sign-in line at once, nothing read. Else `ReadFirst`: S5's list is read NOW
   (`S5Masters.listedNow`: one GET with the S5 SESSION, never the legacy ERP key) and the route asked again.
4. The read counts only as `READ`: 200 with a `projects` array, by a login the phone's own record of the sign-in calls
   staff (`S5Session.who`'s `kind`; the phone only ever stores a staff sign-in, and a legacy key cannot show it). No
   answer -> `OFFLINE`, the offline line. Not known to be staff -> `UNPROVEN`, the refusal stands.
5. S5 names one of the keys -> the refusal stands (`app.*_s5_not_removed`). **S5 names none -> `Gone(keys)`**: the
   phone's row is removed and THOSE keys, only those, are forgotten — `S5Sites.forget`, `S5Keys.forget`, and the
   masters' entries S5's list had added for them (`S5Masters.forgetKeys`). The banner says why it could go
   (`app.msg.removed_phone_x_s5_gone`), never "had never reached the office system".

Nothing is sent to S5. No general read of S5's list forgets anything: reads overlap, and a list is the list of whoever
is signed in (§9's two objections stand; the delete's own read meets neither — the row has nothing left to upload, and
the login is staff).

**The Site row.** `moveShut` cannot know S5 dropped a project without asking S5. So the answer is taken where S5
gives it: the move's one PUT comes back as S5's 404, which `Renames.notHeld` reads as "S5 does not hold this project".
The Site row is then SHUT for that dialog with a true sentence (`app.project_s5_gone_site_shut`) in place of S5's own
words for the 404 ("This project has never been uploaded" — untrue of a project S5 once listed).

### 10.4 minor — a move into a site that already has a project of the same title is refused

`Renames.moveRoute` takes `twin` (the tree lists ANOTHER project of this client with this title at the site picked;
gathered in `Catalogue.moveRoute`) and answers `Blocked(app.not_moved_site_has_project, PHONE)`. `moveLocal`'s own
merge of two rows is now reached only for the phone's hidden row following its own S5 project, where it is right.

### 10.5 minor — a bench docname with no live ERP pair is the office's record

`Deletes.route` takes `erpLive`. A site or a project that carries a bench docname, on a phone with no live pair, is
`Blocked` at once (`app.project_office_record_removed`, `app.site_office_record_removed`) instead of going out with a
dead or missing key. **Before S5 is asked anything** — decided by Claude: the docname decides first, as it does for a
rename, because it is the reason that names where the thing CAN be done. With a live pair the order is F4b's (S5
first, then the bench). The defaults (`erpLive = true`, `tie = null`) are the route as it was.

### 10.6 minor — the "S5 does not hold the project and it was not made here" sentence only where it is true

`MainActivity` said it in three states it is not true of. Now: the project went from the tree while its dialog was
open (before the route, and after the list read) -> `app.project_not_listed_nothing_moved`; no row of the phone's
changed, and the route that cannot be -> `app.nothing_moved_try_again`. The original sentence has one reader left, the
rule (`Renames.moveShut`).

### 10.7 minor — a tap on a listed site only fills the field

In the Site row's picker a tap on a site moved the project at once. It now fills the field; `Use this site` is the
one thing that makes the move.

### 10.8 minor — the dialog opened by New site says why, also where the move is shut

The line was drawn only where the row was live. It is drawn whenever New site opened the dialog: with the button's
words where the project can be moved (`app.project_exists_use_site`), and where it cannot, a line that names no button
(`app.project_exists_opened`), followed by the reason the row is shut.

### 10.9 minor, recorded — a local rename racing a running upload

An upload job that read a capture's OLD names just before `renameHere` renamed it calls `keyFor` with them. This was a
fork: the key was a hash of the old names, and every later capture of the renamed row hashed the new ones. **It is not
one any more, by §10.1**: the job's key is written on the capture row, the row now carries the new names, and the next
read of the captures remembers that key for the NEW names — so the row's later captures are the same S5 project. What
is left is cosmetic: that first upload carries the old title to S5, and the next upload sets it again. Cancelling and
re-queueing the job was not built: a cancel does not stop a blocking request already in flight, so it would not close
the window it was for. This path was reasoned from the code and not run.

### 10.10 The new texts

Eleven keys, each a phone text, English first: the Marathi value of each is its English, and each is listed in
`android/i18n/mr-pending.tsv` (42 -> 53). No existing text changed.

| Key | English |
|---|---|
| `app.site_x_open` | Site · {site} ▾ |
| `app.project_office_record_removed` | This project is the office's record. It is removed at the office, not from this phone. |
| `app.site_office_record_removed` | This site is the office's record. It is removed at the office, not from this phone. |
| `app.project_s5_photos_first` | Nothing was removed: this project has photos on this phone. Delete its photos first. It is then removed only if S5 does not hold it. |
| `app.site_s5_photos_first` | Nothing was removed: this site has photos on this phone. Delete its photos first. It is then removed only if S5 holds no project of it. |
| `app.msg.removed_phone_x_s5_gone` | Removed from this phone — S5 no longer lists {name}. |
| `app.not_moved_site_has_project` | Not moved: that site already has a project with this name. |
| `app.project_not_listed_nothing_moved` | This project is no longer listed; nothing moved. |
| `app.nothing_moved_try_again` | Nothing moved: this phone could not change the project's row. Close this and try again. |
| `app.project_exists_opened` | This client already has this project, so it was opened. No new one was made. |
| `app.project_s5_gone_site_shut` | S5 no longer holds this project, so its site cannot be changed from this phone. Nothing moved. |

Reused: `refuse.site.signed_out` and `refuse.site.offline` for a delete that could not read S5's list. They end "not
saved" and one says "Save again", where a delete has no Save — §5 already records that of a move; it is one key each
if they read wrongly on the phone.

### 10.11 Tests

`:pano` 116 classes, 1,677 tests -> **117 classes, 1,701 tests**, all green (the totals of
`pano/build/test-results/test`).

- `KeyBookTest` (new, 12): the rule case by case, the reviewer's case, and four source tests of `:app` (one mint, and
  only where the masters and the book hold none; S5's list filed under the remembered client; what feeds the book and
  the one thing that removes a key; the Site chip and the project that stays open).
- `DeleteRouteTest` (+7): photos first; read first, never signed out; the read decides (Gone, still listed, offline,
  not staff); a row with nothing on S5 is not asked; the office's record; the banner; the read and the handler, as
  source.
- `MoveRouteTest` (+4): the twin; S5's 404; the true sentences; why New site opened the dialog.
- `RenameRouteTest` (+1): `heldKeys`, and that `onS5` is "there is one".
- **Pins that moved, each with its reason beside it:** `RenameRouteTest` — a site's second half and the five pins of
  the gathering (`s5Ties`, `keyOfRow`, `Renames.heldKeys` in place of `onS5`'s inline gathering and `S5Auth.localKey`);
  `DeleteRouteTest` — the one caller is `deleteRoute` and the local-only sentence is `removeHere`'s; `MoveRouteTest` —
  the row's key, `twin = twin`, `moveGone` at close and in the dialog's argument, and a pick that fills the field;
  `TreeNavTest` — the why-it-opened line. No test was deleted or loosened.

### 10.12 Known, and left

- **Rows typed on this phone for other test clients stay until he deletes them.** F4a cuts what an ERP bootstrap
  left; a row typed on the phone is the phone's own words and is never pruned. Such a client shows with its Offline
  pill. Each is removed from its dialog — at once where nothing of it is on S5, and by §10.3 where S5 once listed it.
- **A stored ERP key that is dead but never probed counts as live until the first sync** (§6): for that one app start
  a bench docname's delete would still go out with it, and fail there.
- **The delete's refusal still arrives on the tap** (§5): the dialog draws "Deleting removes it from ERP for
  everyone" and its button first.
- **A capture row deleted while its upload job is mid-request.** The delete of the capture does not cancel the job. If
  the project is deleted from the phone within the seconds that job takes, S5 can gain the project after the list
  that said it was gone; S5's next list then names it with no customer and, the key having been forgotten, it is filed
  under "(no client)". Not reproduced; it needs two deletes inside one request.
- **The photos-first sentence is also said of a project S5 still holds** (the usual case). It does not promise the
  delete will then work ("only if S5 does not hold it"), but a person may delete photos to find that out. Photos a
  server holds are still on the server; photos never sent are lost with the delete, as with any capture delete.
- **A bench rename (live pair) moves no remembered key**, and still puts him out of a project renamed from inside it
  (only the local rename re-reads the project) — V18's twin; no phone holds a live pair.
- **A project typed again under the client and title of an old capture takes that capture's id** — whatever it is. A
  capture from the ERP days carries a bench docname, so a project of that client and title made again on the phone
  goes to S5 under that docname (as the projects S5 already lists for an ERP customer are keyed), and is then "the
  office's record" for a rename or a delete. It used to get a freshly minted key, beside the ERP project.
- **S5 still holds no client name.** The remembered keys are on this phone only: Clear storage or a reinstall still
  loses them (Slice 5). — **Closed for the name by §11**: S5 holds it and the tree reads it first. The remembered keys
  are still this phone's alone, and matter only for a project no capture of which has reached S5.

### 10.13 What is not proven

- **Nothing was seen on a phone**, and `:app` was not compiled: CI is its only compiler.
- **What stood in for a compiler.** The harness (`f4fix-tree` in the session's scratchpad) compiled the real
  `Catalogue.kt`, `S5Session.kt`, `S5Sites.kt`, `S5Keys.kt`, `CaptureStore.kt`, `SiteRename.kt` and `JsonRead.kt`
  against the real `:pano` jar; and the EXACT text of every handler this commit touched in `MainActivity.kt`
  (`deleteRoute`, `removeHere`, the four move handlers, the whole of `renameHere`, the delete's tap, the chip's
  `onSite`, the two `S5Keys.learn` lines) was cut from the file and compiled with stand-ins only for Compose's state,
  `scope.launch` and `withContext` — and then RUN: §10.1's case end to end (move, captures removed, the next capture
  takes the same key, the project listed once under its client; and a key minted before the book existed), §10.2's
  rename from inside, every branch of §10.3 (photos first; Gone, with nothing sent to S5 and only that key forgotten;
  still listed; offline; signed out; not staff; a site; a row never sent), §10.4, §10.5 and §10.6. The Site row's
  exact text was compiled over stand-ins for the Compose calls it makes. Neither is CI.
- **The least sure lines** are the three that sit in Compose and were checked only by their shape against a
  neighbour: the chip in `BrowseScreens.kt` (`onSite?.let { AssistChip(onClick = it, label = { … }) }`, the shape of
  the Choose rooms chip beside it), the dialog's `moveShut = moveGone?.invoke(tx) ?: moveShut.orEmpty()`, and the
  delete tap's `else if` chain as the last statement of the `onDelete` lambda.
- **S5 itself was not called.** The delete's read was run against a stand-in list; that staff see every project in
  `GET /api/asis` is S5's own rule, read in its notes and not probed here.
- **How the chip row sits at 360 dp** with a long site name first, and how the 130-character sentences wrap in the
  dialog, are the phone's to show.

## 11. Slice 5, the phone half, as built — the client's name is S5's, and a client rename works

24 Nov 2026 (IST), branch `port63-client` over `26b3653` (build 0.3.356; `:pano` 117 classes, 1,701 tests → 118 and
1,723). Amit: "Client rename". S5's half is `mcft-interior-estimate` `5772723`: staff are given `client_name`,
`client_name_by`, `client_name_at` on the list, the project and the room; a record's `client_name` sets a FIRST name
only; `PUT /api/asis/:project/client {client_name, base}` renames one project's client. The name is a LABEL: nothing
here sends, reads or changes `customer`, the ERP Customer id.

### 11.1 As built

The rules are `pano/ClientNames.kt` (tested by `ClientNamesTest`, which also reads `:app` as source).

- **One home for S5's answer: `app/S5Clients.kt`**, per S5 project id, the twin of `S5Sites` and fed wherever that is
  (`S5Masters.fetch`, `QuoteStageSync.refresh`, `SiteRename.hear`, every answer to a `PUT …/client`), by S5's own
  stamp (`Renames.takes`). No entry = S5 gave this login no key (not staff, or an older S5).
- **The tree reads it first** (`Catalogue.erpProjects`, `ClientNames.nameOf`; `S5Auth.keyFor` the same way): a
  reinstalled phone lists each S5 project under its client, not under "(no client)".
- **The phone's rows FOLLOW the name where it is written** (`S5Clients.takeList` → `ClientNames.steps` / `follows`):
  the project's capture rows, the row made on this phone, the remembered S5 key and the chosen rooms. They are FILED
  under the client's name, so a name only shown would have hidden the photos. Asked for every named row of every
  read, so a step that failed is made good by the next one.
- **The upload's record carries `client_name`** (`AsIsPath.body`, `ClientNames.recordName`): the capture row's
  client, never blank, the bucket, the demo client or the customer id.
- **The backfill** (`ClientRename.seed`, in the sync's S5 leg after `SiteRename.seed`): a project S5 lists to staff
  with NO name, for which the phone holds one, is sent it once with base `""`. A 409's name is taken — S5 wins. Quiet
  on a refusal or no answer; one pass, no loop; it sends nothing once S5 has a name.
- **The rename** (`MainActivity` `clientRoute` → `ClientNames.route`, asked first by `renameRoute`; `renameClient`;
  `ClientRename.rename` / `finish`): the name is checked on the phone as S5 checks it (`ClientNames.typed`: one line,
  1 to 140 UTF-16 units, no control character), S5's list is read FIRST, then ONE `PUT …/client` per project with
  that project's own base. "Renamed" only when every project took it; only then does what the phone alone still
  keeps under the old name move (Slice 4's re-key: rows never sent, their captures, keys, masters rows, wish lists).
  Otherwise the dialog stays open: `Renamed on 1 of 2 projects` and one line per project left, saying why.
- **A project's TITLE is still not renamed where S5 holds it** — refused before Save exactly as F4b left it
  (`Renames.heldByS5`). S5 has no route for it, and photos are still joined by title (V15).

### 11.2 Decided by Claude, not by Amit

1. **`Renames.route` is untouched**; `ClientNames.route` answers first for a client and null means "the routes a
   client had". ~~With a LIVE ERP pair a client that carries a Customer id keeps the bench rename~~ — superseded by
   the review (§11.6, fault 1): where S5 holds a name key for a project of the client the rename goes through S5
   whatever the pair, and the same Save tells the bench. With no pair it is renamed through S5 alone — the label is
   not the Customer.
2. **A client's Save always reads S5's list first** (a fifth caller of `S5Masters.fetch`; `TreeKeepTest`'s pin moved
   4 → 5 with the reason beside it): the bases must be S5's names NOW, and "offline" is said before any project is
   touched.
3. **After "1 of 2" the tree shows what S5 holds** — two clients, because S5 holds two names — and Save again
   finishes it. No automatic retry after a 409, as for a site.
4. **The label rule is applied to the client's name only.** A site's dialog still shows S5's own 400.
5. **The "(no client)" row that holds S5 projects is shut with its own sentence**: it is not a client, and one Save
   there would give unrelated projects one name.
6. **A bootstrap `sites` row is listed under its linked project's client**, so an S5 name does not leave the old
   client behind as an empty site.
7. Eight texts, English first (`android/i18n/README.md`, "Port 63 slice 5"); three of the site rename's are reused.

### 11.3 Checked, not changed: does the client's name reach the project TITLE or the S5 key?

No, as text. The record's `project_title` is the title typed in New site (never composed from the client) and
`project` is the project's id. A phone-minted key is `S5-` and ten hex digits of a SHA-256 over client, site and
title: the client's name is an INPUT of that truncated hash and is in no key as readable text. `customer` is the ERP
Customer id, or blank.

### 11.4 Found and left

- **The projects under "(no client)" cannot be named from the phone** unless some phone still holds their client's
  name (its backfill sends it). A way to give such a project its client is not built.
- ~~**A bench client rename (live pair) does not tell S5**, and S5's name then wins on the tree. No phone holds a
  pair.~~ Closed by the review (§11.6, fault 1): it was a fault, not a note — the screen said "Renamed".
- **A bootstrap `sites` row with NO linked project** keeps the bench's own customer name, so on a phone whose masters
  are a bootstrap's it can stay behind as an empty site of the old client after another phone's rename.
- **A wish list kept under `local:<client>|<title>`** follows a completed Save on this phone only; a name taken from
  another phone's rename does not re-key it.
- **`Catalogue.renameSiteRows` compares a masters row's own customer name with the tree's client**: where S5's name
  differs from the bootstrap's it finds no row. Same class as the line above; live-pair masters only.
- **Two S5 projects of one client with one title** are one row by name (V15's class, not new).

### 11.5 What is not proven

- **Nothing was seen on a phone and `:app` was not compiled**: CI is its only compiler, and this slice ran no scratch
  harness. The least sure lines: `MainActivity`'s `onSave` (`if (…) renameClient(t, newName) else when (…)`) and
  the `renameClient` coroutine; `Catalogue`'s `clients()` / `snapshot` / `ServerSites.owners`; `S5Clients.takeList`.
- **S5 was not called.** The answers `ClientNamesTest` reads are written from S5's source at `5772723`, not captured.

### 11.6 Review and fixes

24 Nov 2026 (IST), one commit over `f01a2ac` (`:pano` 118 classes, 1,723 tests → 118 and 1,735). A behaviour review
of slice 5 found seven faults; each was checked against the code first and all seven were real. Every decision
touched is now a pure function in `pano/ClientNames.kt`, tested in `ClientNamesTest` by cases; the source pins there
only hold `:app` to calling it.

**Found, and what was done**

1. **A bench rename was overridden by S5's old name while the screen said "Renamed".** `ClientNames.route` answered
   null for a bench Customer with a live pair, so the bench alone was renamed and the tree, which reads S5's name
   first, went on showing the old one. Now the route has no bench question: wherever S5 gives a name key for a
   project of the client the rename goes to S5, and the same Save tells the bench (`ClientNames.bench`,
   `ClientRename.bench`). **The bench is told FIRST**: `rename_node` refuses a name another Customer holds, and a
   refusal there leaves S5 untouched and is said in the bench's own words. Told the name it already has it answers
   the same, so a repeated Save harms nothing. When the bench took the name and S5 then left a project behind, the
   dialog stays open and its first line says so (`refuse.client.office_only_x`), with S5's own lines under it —
   never "Renamed".
2. **A client could be renamed onto an existing client's name.** `ClientNames.refusal` is asked at Save before any
   route (S5's, the bench's, the phone's own row) and again on the list the Save reads: refused, before anything is
   sent, when the target client already lists a project with the title of one of this client's
   (`refuse.client.has_project_x` — the site move's `twin` rule and wording pattern). **A rename onto an existing
   client with NO title in common is a merge, and it is allowed** (decided by Claude): every project keeps its own
   S5 key and title, the rows are filed by client and title, so each is still found; S5 holds a name per project and
   has no client entity to collide. The bucket's name and the demo client's are refused as a target
   (`refuse.client.name_reserved`): a client given the bucket's name could never be renamed again.
3. **Staff-only names survived a sign-out.** `S5Clients` now carries a mark of the login that read the names
   (`ClientNames.owner`: a digest of the S5 login, else of the legacy key — the key alone, never its secret). `held`
   and `all` give nothing to any other login or to a signed-out phone (`ClientNames.shown`, `kept`), so the tree
   shows none and the backfill, which sends only for a project S5 gave THIS login a key for, sends nothing. The file
   is emptied at sign-out (`S5Session.signOut`; `FrappeClient.forget` where the key was the login), at the first
   answer taken for another login, and by a read of S5's list that names projects and carries the key on none of
   them — a login that is not staff (`ClientNames.forgets`; `S5Clients.takeRead` replaces the feeders' `takeList`).
4. **An upload already running could bring the old name back.** Two changes, neither a loop: the record's
   `client_name` is read WHEN THE RECORD IS SENT, from the capture's own row as it stands then
   (`AsIsUploadWorker.putRecord`), and a client's Save and an upload's first record PUT take one gate
   (`ClientRename.GATE`, a `Mutex`). So a record lands either before the Save reads S5's list — its project is then
   renamed with the rest — or after the phone's rows moved, carrying the new name.
5. **The second Save reused the first Save's project list.** `clientRoute` no longer reads `renameIds`: the client's
   projects are worked out at every asking from the tree as it is then (`ClientNames.writes`), so after "1 of 2" the
   project that took the name is not sent again, one another phone gave the old client meanwhile is, and one S5 no
   longer lists is not sent to be refused.
6. **"Offline" was said for every route that was neither S5 nor shut**, with the list read a second before.
   `ClientNames.next` turns the route asked after the read into what the Save does — send, rename the phone's own
   row, the bench alone (judged on the bench's answer), or say the route's own sentence — and `ClientNames.notRead`
   says offline only for a read that failed (the sign-in line where the phone holds nothing to call S5 with).
7. **Tests**: twelve new, by cases — the route with a bench Customer and a live pair, the bench-took-S5-left line,
   the second Save healing a partial with a project added between Saves, the clash / merge / reserved-name /
   own-partial cases, what a signed-out phone, the next person and a non-staff list are shown and send, and every
   route's answer after the read.

Three texts, English first (`android/i18n/README.md`, "Port 63 slice 5, its review"; `mr-pending.tsv` 61 → 64).

**Left**

- **A partial rename of a client that has TWO projects of one title, whose dialog was then closed**: the project that
  moved now sits under the new name, so finishing the rename from a new dialog is refused as a clash. While the
  dialog stays open the Save that finishes it is let through (`refusal`'s `begun`). Closed, there is no way to
  finish it from the phone — renaming back is the same clash — and the project left behind is named by S5's own
  route (the data steward). It needs all three: two projects of one title under one client (V15's one-row-by-name
  case already), a Save that S5 took in part, and the dialog closed on it. A partial with no such twin is finished
  from a new dialog, as a merge.
- **Only one bench Customer is told**: the client's row carries the first project's Customer id. A client name that
  groups projects of two Customers renames one at the bench.
- **A bench refusal stops the S5 rename** also where the Customer id on the row is another record's than the label
  suggests. It is said as the bench's; nothing is sent.
- **The bench took the name and S5 took none** (no signal after the first call): said, with the offline line; healed
  by Save again. Closed without it, the office's record carries the new name and S5 the old until somebody renames
  again — the tree shows S5's.
- **The gate is this phone's.** Another phone's upload is S5's to order: S5 takes a record's name only while it
  holds none, so the worst case is a project S5 had not listed arriving under the old name from another phone —
  heard at the next list read as a second client, never reported here as renamed and then reverted.
- **The backfill is not under the gate.** It writes with base `""` only, so beside a Save it either loses with a 409
  and takes S5's name, or makes that project's write stale — a partial that is said and healed by Save again.
- **Names already filed stay filed after sign-out**: a capture row, a row made on the phone and a remembered key
  that FOLLOWED S5's name keep it. Sign-out clears what S5 told (`S5Clients`, the masters); it does not rewrite the
  phone's own rows.
- **A list read in flight across a sign-out and a sign-in as someone else** would be marked as the new login's. The
  window is one request.

**Not proven**

- `:app` was not compiled here. The least sure lines: `renameClient`'s `return@launch` inside
  `ClientRename.GATE.withLock { … }`; `clientClash` returning a lambda as `Msg?`; `putRecord()` writing into `body`
  and reading the `var c`; `S5Clients.take`'s `claim(…) && takeList(…) > 0`; the `CancellationException` catch
  in `AsIsUploadWorker.doWork`.
- Nothing was seen on a phone, and neither S5 nor the bench was called.
