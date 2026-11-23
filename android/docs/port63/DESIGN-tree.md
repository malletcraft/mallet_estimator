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

The fix is four slices and a fifth that needs his yes (§5). This file is written with the first build's two:
**F4a** (the plan's Slice 1: what the phone lists) and **F4b** (the plan's Slice 4: a rename that cannot work says so
before Save, and never forks). F4b is the next commit and is added to this file with it.

## 1. The faults these two slices close

The verifying pass numbered nineteen faults, V1 to V19. Reduced here to the ones this build closes or changes; the
rest are in §5 with the slice that takes each.

| # | The fault | Closed by |
|---|---|---|
| V2 | A phone with no live ERP pair keeps its last ERP bootstrap for ever. Every read of S5's list kept the bootstrap's projects and its whole `sites` array; sign-out drops only what S5 added; a bootstrap is replaced only by the next one, and a phone whose pair is gone or refused never receives one. So nothing done in ERP could reach such a phone. | **F4a**: `TreeKeep.keepsProject`, `TreeKeep.keepsSite`, asked by `S5Masters.fetch` |
| V3 | The bench's bootstrap sends every Mallet Site, and the tree makes a client of every site row's owner — so a client whose projects were all cancelled stays, with an empty site. | **F4a, on a phone with no live pair only**: a site row stays only while a kept project links to it. On a phone that bootstraps it is the bench's to fix (§5) |
| V14 | The Demo Client is always appended to the tree although the help tours are off, and the search overlay shows three hard-coded suggestions. | **F4a**: `TreeKeep.demoListed`, `TreeKeep.listsCapture`; the suggestions are gone |
| V5 | Renaming a phone-made client says "Renamed" and leaves two clients: the project S5 holds stays under the old name (the phone re-reads its client from the capture row, which no rename touched), a copy appears under the new one, and the next capture mints a second S5 project. | F4b |
| V6 | A client or a project that carries an id cannot be renamed on a phone with no pair — and the dialog says "Could not rename: no server configured" only after Save. With a dead pair stored the call goes to the bench with the dead key. A phone-minted `S5-…` id is sent to the bench as a Project docname. | F4b (the refusal is said before Save, in true words; the rename itself is Slice 5) |
| V13 | Deleting a site S5 holds, on a phone with no pair, removes the phone's row, says "Removed from this phone — … had never reached the office system", and the site stays, listed by S5. | F4b |

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

## 5. What is NOT fixed, for whoever continues

In the order the verified plan gives them. V-numbers are the verifying pass's.

- **F4b, the next commit of this build (the plan's Slice 4)** — V5, V6, V13: `Renames.route` gains `onS5` and takes
  `erpLive` in place of the legacy-pair flag; a rename that cannot work is shut with a true sentence from the moment
  the dialog opens; the Local route re-keys the row's unsent capture rows; `Deletes.route` gains the same `onS5`.
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
- **Not the phone's to fix.** V3 on a phone that bootstraps (the bench sends every Mallet Site: a filter in
  `bootstrap()`, a bench deploy, not worth it while no phone holds a live pair). V18 (the bench's client rename edits
  the customer's name and not its docname, and leaves the phone's rows; needs a live pair). The old test project on
  S5 stage (S5 has no project delete).

## 6. What is not proven

- **Nothing was seen on a phone**, and `:app` was not compiled: CI is its only compiler. The scratch harness
  compiled the real `S5Session.kt`, `Catalogue.kt`, `CaptureStore.kt`, `S5Sites.kt` and `JsonRead.kt` of this branch
  and ran `S5Masters.fetch` over a made-up stale bootstrap (three ERP clients, one project S5 lists, one unsent
  capture, one S5-only project) — the cut, the merge, a second read giving the same masters, an unsent capture's
  project leaving once the capture is sent, no answer from S5 keeping everything. The changed lines of
  `MainActivity.kt`, `SyncWorker.kt` and `HomeScreens.kt` were type-checked only as mirrored statements in that
  harness.
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
