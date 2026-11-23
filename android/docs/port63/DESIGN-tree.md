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

The fix is four slices and a fifth that needs his yes (§5). This file is written with the first build's two, each
its own commit: **F4a** (the plan's Slice 1: what the phone lists, §2) and **F4b** (the plan's Slice 4: a rename that
cannot work says so before Save, and never forks, §3).

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

- **Slice 2 — one project, one site** (V1). Nothing on the phone gives ONE project a site: the "(no site)" bucket
  cannot be renamed, the seed and the upload send nothing for it, and no screen moves a project. A "Site" row on the
  project's dialog: pick one of the client's sites or a new name, ONE `PUT …/site` for that project with its own base
  (`SiteRename.rename` already sends one), `Catalogue.moveLocal` for a phone row. The data that prompted it (a
  project of his on S5 with no site name) was written on S5 stage the same evening, so his phone lists it under the
  flat's name at its next list read.
- **Slice 3 — navigation, the "site switch"** (V9, V10, V11, and V8's twin). A chevron drawn on a crumb whose tap
  does nothing; only the last crumb opens "Switch"; Back skips a project list he walked through; the open place is
  held as names and a stale `Project` copy that nothing re-reads; a one-site client is opened by site NAME across all
  clients (audit R12); the New site dialog with an existing project's client and title builds a twin that is not a
  row of the tree. A pure `TreeNav` (openClient, openSite, back, settle, existing, switches).
- **Slice 5 — S5 keeps the client's name** (V4, V16; S5 QUOTE first, then S2 APK; needs Amit's yes). The client of a
  project made on the phone exists only on that phone: S5 holds customer "" for it, so a reinstall or a second phone
  files it under "(no client)". A `client_name` on the S5 project (first value from the upload while blank, a guarded
  `PUT …/client`), read by `S5Masters.fetch` before the capture row; the S5 key STORED instead of re-derived. Until
  then: **do not use Clear storage or reinstall** on a phone that made a client — its capture rows are the only copy
  of that client's name.
- **Later, small.** Photos joined by project id, else client and title (V15; today the title alone, at eleven
  sites and in `TreeKeep.projectOf`) — before any title rename. A site row takes a bench docname only from a row keyed
  under its own name (V12: on a live-pair phone a site S5 named differently carries the WHOLE ERP site's docname, so
  its delete would cascade the sibling projects listed elsewhere). `distinctBy` the folded key, not lower case (V17).
  Rotation and process death reset the tree to Clients; the room chooser is drawn only inside a room (audit R11); the
  rename dialog blanks the tree behind it; S5's customer id is shown as a client's name (V19).
- **Found while building F4b, and left** (none was in the plan's two slices):
  - *A bench delete with no live pair.* A row with a bench docname that S5 does not hold still routes to the bench
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
