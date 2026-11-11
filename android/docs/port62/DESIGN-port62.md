# DESIGN-port62 — Port 62: site name on S5, wish marks on the phone, per-room plans; S5-web arranger and RFQ (outlines)

**Dated 11 Nov 2026 (IST). S2 APK and S5 QUOTE; S6 ERP only as vanilla ERPNext seeded by config-as-code.**
A plan, not code. It follows `GAP-0.3.318.md` (same folder: the readers' line references, the twelve rename
defects R1–R12, the delta D1–D6) and takes its slice shape from `../port61c/DESIGN-port61c.md`. **A critic's
fourteen findings on the first version (`7f3568c`) are applied throughout; what was confirmed, what was declined
and why is the last section, "The port 62 plan critic".**

- `P L<n>` = the approved prototype v61c.2 (Version 86), a built page of 9,563 lines, re-read for this plan.
  Phone paths `app/…` and `pano/…` as in the audit. S5 at `9a51733` (= `stage`). Sizes: S about a day, M two to
  four days, L a week or needs its own design first.
- **The page in the scratchpad was REBUILT on 11 Nov 2026 (18:36 IST) and its lines have moved** (the L2 review,
  finding 5). `v61cbuild/index61c2.html` is now **mock-up v62a**: v61c.2 plus this plan's prototype items P0 and
  P2, "to approve, not on the phone" — 9,657 lines. Diffed against the 9,563-line page this plan read (the copy
  the critic drove, `critic/site/` in the same scratchpad): 21 hunks, every one P0's or P2's — CSS, the header and
  help texts, new rows in the one-line text table, the rename dialog, the room screen's "Plan & elevations" chip
  and survey prep opening on its Plan tab. **Nothing of the wish-marks code changed**: every line between the
  hunks is byte-identical, and `proto-tables.mjs` prints the same rows from both pages (only its first line, the
  modules' line numbers, differs). So a `P L<n>` in this file, and an `index61c2.html <n>` in the code's
  comments, still names v61c.2's line; **on the page now on disk read it as:**

  | `P L<n>` cited here | on the rebuilt page | what lies there |
  |---|---|---|
  | up to 1595 | the same | the CSS (`L1257`–`L1372`) |
  | 1596 – 1714 | + 7 | the header and the help cards |
  | 1715 – 6923 | **+ 26** | T53, L53, P54 and survey prep: everything L1–L6 and L10 cite (`outline` 2872 → 2898, `interceptDelete` 2940 → 2966, `TOOLS` 3947 → 3973, `clearCount` 5651 → 5677) |
  | 6924 – 7923 | + 27 | Start over's note (6942 → 6969), `openLWH` (6977 → 7004) |
  | 7924 – 8151 | + 55 to + 77 | the rename dialog (P0's own lines are among them) |
  | 8152 on | + 93, then + 94 from 8162 | the room screen and Capture 360 (8185 → 8279), the Wish list (`rowCount` 8530 → 8624), the tours (8838 → 8932) |

  Search for the function's name, not the number, when a line does not match. The page a later slice reads is
  whatever is on disk then; this table is for the build of 11 Nov only.
- **Rules that bind every slice.** The prototype is the spec for every APK screen: replicate, invent nothing; a
  state it does not draw goes on a **prototype question** list, never onto the phone. Texts come from
  `android/i18n` (a missing one is added from the prototype's own words, never coined); exports and wire values
  stay English (decision 4). `:pano` is pure JVM and tested here; `:app` compiles only in CI. S5 ships to `stage`
  first, then the phone. Each slice leaves the Play build usable. LEAN: one build agent, one review lens.

## 0. Amit's four answers (11 Nov 2026) and the scope split

1. First to the phone: **"Wish marks on photos"**.
2. Site name: **"Yes — S5 keeps the site name"** — the option as put to him: S5 stores each project's site name and
   gets a rename route; the phone renames through S5; ERP is not involved; a failed rename says why in the
   dialog, and that text goes through the prototype first.
3. RFQ: **"this RFQ stuff will be done on S5 an on computer webbrowser. lets not complicate apk for now."**
4. Floor plan: **"need floor plan per room the way current survey happens per room. arranging a floor plan will be
   done in s5 rather than apk. floor and wall elevations are required so that it will be a cleaner look to verify
   quickly. the whole project floor plan should be visible once its done on s5. each room floor plan should be
   visible inside that room only."**

His journey, the same day (whole in GAP §1): survey first; then on the same survey photos the client wish list
is MARKED (new wardrobe, POP, new dining table, "remove this window and install UPVC window", "close this door with
brickwork"); vendors review and quote; the wish list goes to MULTIPLE vendors by RFQ and the quotes are compared;
revisions against quoted amounts; the quote is finalised; work starts. "RFQ will be sent as plain 2d diagrams as a
document ... RFQ will be per line of service like painting, electric work, civil work, furniture work, doors and
windows, POP etc."

| Step of the journey | S2 APK (phone) | S5 QUOTE (web, on a computer) |
|---|---|---|
| Site name | renames through S5, says why on failure (§2) | stores it, one guarded rename route (§2) |
| Survey | as shipped | as shipped |
| Wish marks on the photos | **all of it:** state, Mark sheet, new types, drawn on face / 360 / List / drawings, the wish list counting them (§3) | shows them to staff; a vendor or client login is given the room AS SURVEYED and nothing of the wish round, until Amit decides what each sees (§3.11, §3.13) |
| Vendor review, RFQ, quotes, comparison, revisions | **nothing new.** Answer 3 says so of the RFQ and the quotes; keeping the REST of the vendor round off the phone (Trade chip, packs, State ▾, the bundle) is CLAUDE'S READING of it, not his decision — asked as Q-list 3. The wish row's Vendor field stays as shipped | all of it (§6). ERPNext's standard Request for Quotation and Supplier Quotation are the RECOMMENDED home of a quote, not a decision (§6, question 7) |
| A room's plan and elevations | opens from the room screen (PL, §4; decided 11 Nov) | the six line drawings on the room page (slice RP, §4.1); the arranger and the RFQ document both stand on them |
| The whole flat's plan | read-only, later | the arranger and the whole plan on the project page (§5) |
| Finalise, then work | as shipped (PM approves, scope, album) | as shipped; the award join comes with §6 |

**The 19 questions parked in `DESIGN-port61c.md`** (it does not number them; the count is this file's reading:
K3's five + K4's three + K5's seven + the tour's three + FW2's one):

| Parked at | n | After this direction |
|---|---|---|
| §18 K3, five album states (`:724-731`) | 5 | none superseded — still parked |
| §19 K4, queued / refused / other-login writes (`:1381-1387`) | 3 | none superseded |
| §20 K5, the seven from the viewfinder to the unsent single photo (`:2106-2130`) | 7 | none superseded |
| §31 the tour, "Held for Amit" (`:4820-4838`) | 3 | **Q1 superseded for `phases`** (slice L4 ports the band, so no step is cut) and narrowed for `existing` (L6 brings More ▸ by trade; only step 9's List chips stay missing). **Q2 grows**: after port 62 the phone's Vendor review has no packs and its Wish list no bathroom bundle, so the `phases` and `wish` tour texts are rewritten on the prototype first (item P1) and TR waits for that. Q3 (the 360 as a tab) stands, and matters more: wishes are marked on the 360 too |
| §16 FW2 review, finding 2 (`:273`: the photo line's `Survey` against the sheet row's `not on S5 yet`) | 1 | not superseded |

Of the audit's own six (GAP §5): Q3 is answered, so Q2 no longer decides the fix; Q1 is answered in part (the
arranger goes to S5; what he saw of the per-room plan is being asked, §4); Q4 ships as the prototype has it (rows
ticked by hand, counting the marks); Q5 is §6's question 1; Q6 is §6's questions 2 (who enters a quote) and 7
(where a quote is kept — its option (c), "kept on S5 only for now", is still open).

## 1. The slice table

"Play" = the Play build a slice rides (B1, B2 …). Every merge to `main` publishes to Play, so slices gather on
`batch-next` (each one CI-green) and `main` is fast-forwarded only at a build boundary: **B2 = L1 + L2 + L3 + L4,
merged together** — none of the four reaches `main` alone, because L3 without L4 lets a wish be made where the
prototype has no tool, and cannot place a new thing as a wish at all; **B3 = L5 + L6, together** (L5 alone would
put ten new tiles into v52's More grid). "(B2)" = part of that build, nothing of its own to see. B2 is the first
Play build after the site-name fix, and **it already lets him mark a wish**: an existing thing changed (L3) and a
new thing placed (L4). RD and V5 are on S5 `stage` before B2; T5 before B3.

| # | id | slice | piece | size | needs | Play | What Amit sees when it ships |
|---|---|---|---|---|---|---|---|
| 1 | P0 | Prototype: the rename dialog says why it failed; its grey line for a site S5 holds; a site still uploading | S2 APK (prototype) | S | — | — | On the prototype a rename that fails shows a red line under the name; he approves the words |
| 2 | A1 | S5 keeps the site name: migration 0011, `PUT …/site`, the list carries it | S5 QUOTE | S | — | stage | On /projects each project shows its site name once it has one |
| 3 | A2 | The phone renames a site through S5 and says why when it cannot | S2 APK | M | P0 approved; A1 on stage | **B1** | He holds a site, types a name, taps Save: it changes at once on the phone and the web, and the old name is gone from the list; with no signal the dialog says so and stays open |
| 4 | RD | S5 gives a vendor or client the room as surveyed: no wish, note, why or unknown key, at every level of the record | S5 QUOTE | S–M | — | stage, before B2 | Nothing for staff; a vendor or client login sees the surveyed room exactly as today, whatever is marked later |
| 5 | V5 | /projects shows staff a mark's state, act and note | S5 QUOTE | M | RD | stage, before B2 | On the web staff see a wish dashed with its pill, act and note, office notes tagged "Office only"; a vendor or client sees no change |
| 6 | L1 | Pure models in `:pano`: MarkLife, PhaseTools (six stages), Trades | S2 APK | M | — | (B2) | Nothing on screen |
| 7 | L2 | A mark's state drawn on the face; Drop instead of delete; clears keep wishes; the saved pictures stay the survey | S2 APK | M | L1 | (B2) | Nothing he can reach yet (no control makes a wish before L3) |
| 8 | L3 | The Mark sheet: → Client wish, Replace / Shift / Repair / Remove, the note, TRADE on an Other | S2 APK | M | L2; RD and V5 on stage | (B2) | In the Wish list stage he taps a window's code, picks → Client wish and ↻ Replace, types "install UPVC window": the window turns dashed with a "Client wish" pill |
| 9 | L4 | The phase in survey prep (the band; the mark tools hidden at Estimated and Client approved; earlier marks read-only) and a new mark placed as a Client wish | S2 APK | M | L3 | **B2** = L1–L4 | Survey prep opens under "2 · Wish list · every mark you place is a client wish"; the tool chip reads "… · as Client wish" and every mark he places is born a wish; a survey mark cannot be moved or deleted by mistake; at Estimated and after approval there are no mark tools |
| 10 | T5 | S5 learns the ten new types, their sub-kinds and the 13 trades | S5 QUOTE | M | — | stage, before B3 | /projects names a Carpentry unit or a Ceiling drop instead of a grey code |
| 11 | L5 | The ten new types and sub-kinds, pure: boxes, measures, codes | S2 APK | M | L1 | (B3), merged only with L6 | Nothing on screen |
| 12 | L6 | More ▸ by trade, the kind picker, the Note pin | S2 APK | M | L5, L3; T5 on stage | **B3** = L5 + L6 | He places "Carpentry unit · Wardrobe" for a new wardrobe, "Ceiling drop · Full false ceiling" for POP, and a Note that is only words |
| 13 | L7 | A wish on the 360 and in 3 · List | S2 APK | M | L4 | B4 | The same dashed outline and pill on the 360; a List row shows its state and the first line of its note |
| 14 | L8 | A wish on the six drawings, with the legend | S2 APK | M | L6, L7 | B4 | PLAN's drawings show wishes dashed, with the pill, the act and the note mark; a legend line says what is on the sheet |
| 15 | L9 | The Wish list counts its marks; "Marks:" on the project | S2 APK | S–M | L6 | B5 | A ticked row reads "7 marks in 2 rooms · 3 wished"; an unticked row that holds wishes asks "tick it?" |
| 16 | L10 | Lines, readings, the room-size line and Capture 360 by phase | S2 APK | S–M | L4 | B5 | After Survey there is no 1 · Lines and no reading box; Capture 360 shows in Survey and again after the client approves |
| 17 | P1 | Prototype: the "The phases" and "Build the wish list" tour texts for the phone as port 62 leaves it | S2 APK (prototype) | S | — | — | On the prototype the two tours no longer speak of four phases, vendor packs or the bathroom bundle; he approves the words |
| 18 | TR | Tours: "The phases" and "Build the wish list" switch on | S2 APK | S | **P1 approved**; L10, L3, L4 | B6 | Help lists three tours instead of one — only once P1 is approved; until then the two stay off |
| 19 | PL | The room's plan opens from the room screen (Amit, 11 Nov: "it should be on the room") | S2 APK | S | prototype item P2 approved (§4) | the first build after P2 | One tap on a room opens its floor plan, ceiling and four elevations |
| 20 | RP | S5 web: a room's floor plan, ceiling plan and four elevations as line drawings on its room page (§4.1) | S5 QUOTE | M | RD; T5 | stage | On a computer each room's page shows its six clean drawings beside the photos, inside that room only |
| 21 | AR | S5 web: arrange the rooms into the flat's plan (outline, §5) | S5 QUOTE | L | **RP**; its own design | — | On a computer he drags the rooms into place; the whole plan shows on the project page |
| 22 | RQ | S5 web: an RFQ per trade to several vendors, quotes, comparison, rounds (outline, §6) | S5 QUOTE + S6 ERP | L, several slices | **RP** (the drawings); §6's seven answers; AR only for the whole-flat key plan | — | On a computer he sends one trade's 2D document to several vendors and compares what they quote |
| 23 | A3 | The rename's neighbours: R10–R12, and R9 for a project or client rename | S2 APK | M | A2 | later | A renamed project keeps its photos; Choose rooms works from the project screen |

## 2. Slice A — the site name

**What a "site" is on each side.** On the phone it is a level of the tree (client → site → project → room). S5
has projects only (`asis_projects`: project, title, customer — `functions/_lib/asis.js:1924`). So S5 stores the
site name **on each project**, and a site is the client's projects that carry the same name. No site table.

### A1 · S5 half (S)
- **Migration `0011_site_name.sql`** — checked: `migrations/` holds 0001–0010 on the working branch and on
  `origin/stage` (both `9a51733`), so 0011 is free. `asis_projects` gains `site`, `site_by`, `site_at` (NULL reads
  as `''`; no backfill). S5's CLAUDE.md promises "0011 or after" to the `asis_image_moves` drop: it becomes 0012.
- **`PUT /api/asis/:project/site`**, body `{site, base}`, bounded by `MAX_REC`. `site`: a string, newlines folded,
  trimmed, 1–140 characters (the cap rooms and party ids use), else 400. `base` REQUIRED: the name the writer last
  saw, `''` when none. 404 `This project has never been uploaded`.
- **The guard, in the house idiom** (S5-P's: the value is its own stamp) — ONE statement:
  `UPDATE asis_projects SET site = ?2, site_by = ?3, site_at = ?4 WHERE project = ?1 AND COALESCE(site,'') = ?base
  AND COALESCE(site,'') <> ?2`. The same name again answers `written: false` (the retry after a lost answer); a
  stale base answers **409** `Renamed by <login> meanwhile — now <site>; not saved` with `site`, `site_by`,
  `site_at` as stored. No read-then-write window.
- **Who:** MCFT staff, any of them, at every quote stage (a label, not scope). A vendor or client gets 403
  `Only MCFT staff can rename a site` before the project is read; the route joins the permissions walk.
- **First value — a NEW rule on S5, not an existing idiom.** The room record PUT takes an optional `site` and
  writes it ONLY WHILE THE STORED ONE IS BLANK (`site = CASE WHEN COALESCE(site,'') = '' THEN ?n ELSE site END`,
  in the project upsert). The same upsert overwrites `title` and `customer` whenever an upload names one
  (`asis.js:1924`, "an upload that does name one sets it again"); `site` deliberately does NOT follow them — an
  upload never renames, because a phone holding an old name must not write it back.
- **What the reads return.** `GET /api/asis`: each project gains `site` and `site_at`; the project and room GETs
  carry `site`, `site_by`, `site_at`. Additive: 0.3.318 ignores them. /projects shows the name, read-only.
  `site` becomes a reserved room name once d1-peek shows no room on stage is called that; d1-peek prints
  `site_at` and whether a site is set, never the name (it may be an address).
- **Tests** (`tests/api.test.mjs`, memory and sqliteD1): the 400 loop; the 403 walk; 404; 413; a first write with
  `base: ''`; the same-name replay; the stale 409 with a racing writer; an upload setting it only while blank,
  and — its own test, because the rule is new — an upload naming a DIFFERENT site leaving the stored one alone.

### A2 · phone half (M) — after P0 is approved and A1 is on stage
- **`pano/RenameRoute.kt`, beside `DeleteRoute.kt`** — `Renames.route(tx, kind, name, serverId, hasLocalRow,
  s5Projects, signedIn, legacyPair, unlisted)` → `S5(writes)` · `Local` · `Legacy(docname)` · `Blocked(reason)`.
  For a **site**: any project under it that S5 lists → `S5` (signed out → `Blocked`); none, but a local row →
  `Local`; neither ("(no site)", the demo site, a site only an old ERP bootstrap named) → `Blocked`; never the
  bench. Client and project keep today's two routes, with `Blocked` in place of a call that cannot succeed. The
  same file holds the wire: `siteBody`, and `siteVerdict` → Renamed · Same · Stale · Refused(S5's words) · Offline.
- **A base PER PROJECT.** `writes` is one `(project, base)` for each listed project, the base being THAT
  project's `site` exactly as S5's list gave it. The phone groups a site by `Catalogue`'s normalised key (trimmed,
  lower case, runs of space and underscore as one), while A1's guard compares the stored string exactly — so two
  projects of one phone-site can hold two spellings, and one shared base would make the second PUT a 409.
- **R9 on the site path, closed here.** The phone-minted S5 key is a hash of client|site|title, re-derived and
  not stored (`app/S5Session.kt:252-276`), and the site name is the one part a site rename changes: a project
  uploaded under the old hash that the masters do not list yet would get a new hash on its next capture — two S5
  projects. So `unlisted` = "a capture of this site is queued or uploaded under an `S5-…` key the masters do not
  list". The S5 route reads S5's list FIRST (it needs the signal anyway); still unlisted → `Blocked`. The Local
  route with `unlisted` → `Blocked` too. The words are P0's (d).
- **`RenameRouteTest`** (`:pano`): one case per row kind — the demo site, "(no site)", an S5-listed site with and
  without a local row, local only, the local row gone, signed out, a bootstrap-only site, a client, a project;
  two listed projects whose stored spellings differ → two writes, each with its own base; an unlisted `S5-…`
  capture on the S5 route and on the Local route → `Blocked` — and the verdict on 200, `written: false`, 409,
  403, 404 and no answer.
- **`app/MainActivity.kt:1188-1245` (onSave)** routes through it. `S5`: one PUT per listed project (a site usually
  has one); success is S5's answers ALONE. Then, in one step: those projects' `site_name` in the stored masters is
  set from S5's answer, the client's local rows under the old name are re-keyed (`Catalogue.renameLocal`, its
  Boolean now read), the room-scope key moves (`Catalogue.kt:376`), **the client's bootstrap `sites` row of the
  old name is re-named with it** (below), the tree follows, the dialog closes. On any failure the dialog STAYS
  OPEN with the reason under the field, and nothing moves.
- **The old name must not stay behind as an empty site.** The tree makes a site group for every row of the
  masters' `sites` array a client owns, projects or none (`app/Catalogue.kt:662-672`: "an empty group is a site
  the office knows about that simply has no jobs on it yet"), and `S5Masters.fetch` keeps that array as a
  bootstrap brought it ("MERGED, NOT REPLACED"). Left alone, a rename that S5 accepted would list the new site
  AND the old name as an empty one that can only answer Blocked — on any phone that ever bootstrapped, Amit's
  included. So (i) the rename step sets that row's `site_name` to the new name (its type, city and address stay
  with the site); and (ii) where the tree is read, a bootstrap `sites` row whose docname is the `site` of a
  project that S5 names differently is listed under S5's name, never as a second site — which also covers a
  phone with a live ERP pair, whose next bootstrap brings the bench's old name back. The rule that decides
  which site rows are listed moves into `:pano` (`SiteRows.listed`), so it is tested: "an S5 rename with a
  bootstrap row of the old name → ONE site listed", "a later bootstrap carrying the old name → still one", and
  "a bootstrap site with no project and no rename → still listed, empty" (today's behaviour kept).
- **One source for the name.** `S5Masters.fetch` (`app/S5Session.kt:298-348`) takes `site` from S5's list, on
  whichever entry holds the project, and stops stamping the local row's name into S5 entries (`:338-339`); a
  project S5 holds with no site resolves from the local row where the tree is read (`Catalogue.kt:479`).
- **Uploads and the one-off seed.** The record gains `site` (`pano/AsIsPath.kt` `body`, with its test; called at
  `app/AsIsUpload.kt:456-462`). `SyncWorker`, after the S5 list read (`:143`), sends `PUT …/site {site, base: ''}`
  for each listed project S5 holds with no site and the phone knows one for (first write wins); never the demo.
- **`app/TreeShell.kt:601-757`:** the error line under the field, set on failure, cleared on edit and dismiss —
  exactly as P0 is approved. New texts are born keyed; the Marathi is Claude's (standing rule of 28 Oct).

**The twelve audited defects.** CLOSED: **R1** (a working route for an S5-signed phone), **R2** (every refusal
is said in the dialog — client and project renames too), **R3** (no local row and nothing on S5 → refused,
navigation untouched), **R4** (S5's name is the one shown), **R6** (judged on the rename alone), **R7** (the
room-scope key moves with the name). NO LONGER REACHABLE for a site: **R5** (the bench's cross-client clash scan;
the site rename no longer calls the bench, and S1 is not changed). HALF CLOSED: **R8** — the phone's unsynced rows
are re-keyed in the same step; the bench is deliberately not told (answer 2), so a phone still holding a live ERP
pair would file later captures under a second bench site until the ERP leg retires (`s5-s6-integration.md` §4).
CLOSED FOR A SITE RENAME ONLY: **R9** — the phone-minted S5 key is a hash of client|site|title, re-derived and
not stored, and the SITE is the part a site rename changes, so it is on the site path: A2 refuses the rename
while a capture of the site sits under an unlisted `S5-…` key (above). Its other two thirds — a PROJECT rename
changes the title, a CLIENT rename the client — stay open in A3, where the key is stored instead of re-derived.
DELIBERATELY LEFT, slice A3: **R10** (a PROJECT or CLIENT rename leaves the phone's captures under the old
name), **R11** (the v37 Choose rooms / + Room fix below the tree's early return), **R12** (a one-site client
resolved by name across clients).

**P0 · prototype items (Amit approves them there before A2 is built).** The prototype's `doRename`
(`P L7934-7938`) never fails, so there is nothing to copy. (a) The failure line: where it sits, and its words for
no signal, signed out of S5, S5's own refusal (shown as S5 sent it) and "renamed 1 of 2 — Save again to finish".
(b) The grey line for a site S5 holds: "…the new name reaches ERP and every other device" (`P L7926`, key
`app.x_server_so_new`) is untrue now. (c) A row that cannot be renamed: the line, or no rename offered. (d) The
words for a site with a capture still on its way to the web (R9): rename refused until it is listed.

## 3. The wish-marks port (prototype v53, and the parts of v54 the wish journey needs)

**The prep file and S5 keep unknown mark keys whole — verified, both sides.** Phone: a mark is read by seven keys
(`pano/SurveyPrepFile.kt:48`); every other key goes into `Mark.extra` (`:148`) and is written back after the seven
(`:230`); the same holds for a Tape, Line or area (`:168`), a chain, a surface and the file, pinned by
`SurveyPrepFileTest` ("round-trips every field, each at its own level"). A type the build does not know is kept
and drawn grey (`pano/SurveyPrep.kt:679`). S5: the record PUT stores the body as sent (`asis.js:1438`) and checks
no mark key or type (`:1387-1399`). So **no file migration and no S5 migration**: v53 adds to a mark, ONLY WHEN
NOT DEFAULT, `life` `lifeAt` `lifeAs` `why` `born` `act` `note` `vis` `sub` `cat` (`P L2749-2751`), and a v52 file
saves byte-identical. What must change first is what S5 hands back (RD, §3.11). The port is **a listed subset,
as QM1's strip was** (the list is in §7). Amit's answer 3 is about "this RFQ stuff"; that the Trade chip, State ▾
and Move N ▸, the Export sheet and its packs, the Client pack and the bathroom bundle also stay off the phone is
CLAUDE'S READING of "lets not complicate apk for now", not his decision — several of them are not vendor-round
at all, and three are drawn in the Wish list phase itself. Q-list 3 puts the whole subset to him, phase by
phase. The life-cycle table is ported WHOLE, and the Mark sheet offers exactly what the prototype's phase rule
allows in each phase.

**The stage survey prep is given** (every slice below reads it the same way; `PhaseTools` holds it):
- S5 lists the project → its stage index 0–5 from `QuoteStageSync.cached`.
- **The project is not on S5** (made on the phone, never sent — the first survey of every new site,
  `MainActivity.kt:3838`) → **0, Survey's rules.** This is the prototype's own rule, not a guess: "The tools keep
  following the project's own phase, Survey for a project made on the phone (S5's default for the project its
  first upload creates, migration 0004), which nothing on the phone can move" (`P L4091-4093`). `notOnS5`
  (`P L4095`) changes the strip, Approve and the album only. So no wish is made during a first survey (Amit,
  14 Oct: "the wishlist should never get mixed with survey").
- S5 lists the project but its stage has not been read → null, the prototype's unread rule (every tool and
  every move, the footer "Quote stage not read yet — S5 decides when online", `P L2775`, `L3979`).
- **Client approved is a SIXTH stage, named everywhere.** The prototype's tables have five columns
  (`P L3947-3961`, `L3964`); what it does at the sixth was DRIVEN for this plan (11 Nov, the served page at 360 ×
  780, stages 1, 3 and 5) and is written into L3, L4 and L10 below. `PhaseTools` holds six columns, never five
  with a fall-through.

### 3.1 L1 · pure models (M, `:pano` only)
- **Prototype:** LIFE `P L2756-2764`, STROKE `L2768`, ACTS `L2770`, `spared` `L2789`, MOVES `L2794-2811`,
  `allowedMoves` `L2818`, `move` `L2834-2846`, `asPlace` `L2848`, the delete, unlock and clear rules `L2936-2955`,
  the counts `L2964-2977`; P54's PHASES `L3931`, TOOLS `L3947-3961`, AS_FIXED `L3964`, `moveOk` `L3985`, `editable`
  / `delOk` `L4232-4237`, the refusals `L4241-4250`; TRADES `L2379-2392`, `tradeOf` `L2487`, SEES / OWN / ON
  `L2493-2504`, SERVICE_TRADE `L2509-2521`, `moreRows` `L2602`.
- **Files:** new `pano/MarkLife.kt`, `pano/PhaseTools.kt`, `pano/Trades.kt`; typed readers and writers over
  `Mark.extra` and `Meas.extra` (the map is REPLACED, never changed in place; a key is written only when not
  default; back at Existing `life`, `lifeAt`, `lifeAs`, `act`, `why` are deleted; an unknown `life` reads Existing).
- **Tests:** `MarkLifeTest` (every MOVES row; born; restore to what it was born; a why of 3–140; Scope locks, Out
  of scope unlocks; Survey refuses; there-and-back byte-identical), `PhaseToolsTest` (TOOLS cell by cell over SIX
  stages plus unread and not-on-S5 — stage 3 and stage 5 pinned by name: `mark` and `palette` hidden, so the
  "as …" rule is never asked there; `capture` shown at 0 and 5 only; AS_FIXED, `moveOk`, `editable`, `delOk`, the
  words), `TradesTest` (every type and sub-kind on every surface kind), `SurveyPrepFileTest` (a v53 prep
  round-trips; a v52 prep saves unchanged). Nothing is drawn.
- **L1 AS BUILT (11 Nov 2026; branch `port62-wish`, local).** The three objects, `TradesTest` (14), `MarkLifeTest`
  (33), `PhaseToolsTest` (22) and two tests in `SurveyPrepFileTest`; MastersTest's wire check names every reader
  and writer of the stored keys. `:pano` 1,081 tests in 80 suites, none failed (1,010 in 77 before). No new key,
  no table change, nothing on screen; `android/i18n/README.md` ("Port 62 slice L1") lists the keys now read.
  **Cross-checked against the prototype's own code, not only its text:** `proto-tables.mjs` (this folder) loads
  the page's T53, L53, P54 and L57 modules into node and prints what they answer; every row the tests pin for
  stages 0–4 and unread equals it, and 30 one-line mutations of the three objects each turned their suite red.
  - **The sixth column, cell by cell** (`PhaseTools.TOOLS`; the plan names `mark`, `palette` and `capture`, the
    other eleven are read off the page): hidden — `lines`, `mark`, `palette`, `readings`, `trade`, `stateFilter`,
    `wishEdit`, `vendorUI` (what `can()` answers where the page asks it), `vendorPacks`, `handoff` (the strip is
    folded); shown — `list`, `view360`, `wishlist` (nothing asks `can()` for them and the page shows all three:
    the mode is held on List, the 360 opens, the Wish list chip is hidden at Survey alone, CSS `P L1257`) and
    `capture` (CSS `P L1362`, `L1372`).
  - **The stage is one decision.** `PhaseTools.stageOf(cache, phoneOnly)` is the project line's own (`QuoteStage.
    projectLine`): a copy READ as not listed is Survey too, as that line calls it "not on S5 yet"; a test holds
    the two together for every kind of line.
  - **At Client approved, four things the phone decides until Q-list 6 is answered:** no move (`moveOk`, as
    planned); `MarkLife.move` refuses there with the approval lock's own words, as it refuses at Survey with the
    gate's; `asFor` answers NULL where there is no palette, at Estimated as well — nothing is placed, so the
    prototype's fall-through to "as Client wish" cannot be reached; and — since the L1 review, below — a mark
    past Existing has NO delete control there (`delOk`), so no control reaches that refusal at all. Everything
    else there is the prototype's own code for a stage past Estimation (`editable`, `refuseText`) — see the
    additions to Q-list 6.
  - **For the slices that wire it.** L3: the lock is the sheet's rule (`move` does not look at it, as the
    prototype's does not); "decided by staff" is drawn through `album.staff`, lower-cased — the prototype's own
    text layer shows `ठरवणारे: स्टाफ` that way, so no key is missing. L4: **what a Duplicate is born as is
    NOT what this plan's L4 assumes — Q-list 8, to settle before Duplicate is wired**; and the four lines' and
    the reference's padlocks are never hidden by the phase (`P L4259`). L9: the roll-up's `existing`, `wish`,
    `scope` and `dropped` have no key and the prototype shows that line in English under मराठी; L1 ports the
    counts only.
  - **Not ported, and why.** `move`'s "Not a mark" refusal and its `o.as` override (a `SurveyPrep.Mark` is always
    a mark; no caller of the prototype passes `as`); the Trade chip's `MarkFilter` and `paletteMain` by trade
    (Q-list 3); `serviceOf` and `serviceRoomMatch` (L9, they need L5's sub-kinds); the roll-up's words and hint
    (L9); the pill, glyph and badge after a code (L2); the now-line's "since" and "decided by" (L3); the chip's
    "as …" words (L4); the SUBS table (L5 — TradesTest carries the sub-kind ids as its own fixture).

#### The L1 review (11 Nov 2026) — three findings on commit `0619176`, all minor

Each was checked against the committed code and the prototype's own code before anything was changed
(`proto-tables.mjs` run again on the page; the reviewer's probe was not kept, so its rows were re-derived).
**Confirmed: all three. Declined: none.** Fixed in one commit on `port62-wish`, local.

| # | Finding | Verdict, and what was checked | Fixed how |
|---|---|---|---|
| 1 | At Client approved the delete control still shows on a mark past Existing, though the phone's own no-move rule means it can only refuse | **CONFIRMED.** The page keeps Delete on such a mark because it is "a life-cycle move, which the phase already filters (moveOk) — so it stays, except in Estimation" (`P L4229-4230`, `delOk` `L4235-4237`), and its `delOk` answers `1110111` for a wish on a surveyed mark, a mark born a wish, a mark born Vendor added and a Dropped mark (run). L1's `PhaseTools.delOk` answered the same — while `moveOk` is false for all 17 rows at stage 5, `allowedMoves` is empty and `MarkLife.move` / `restore` answer `Refusal.APPROVED`. So on the phone Drop would open the sheet on the approval lock's line and Restore be refused with it: a control that can only refuse | **Hidden, as at Estimation** — the first of the reviewer's two fixes, and widened. The page's own reason for keeping the control is that the phase offers the move; where the phone offers none, the same reason removes it, and it is this plan's rule for the step chips 1 and 2 there (§3.4: a control that cannot act is a prototype gap, not a design). **The reviewer's formula was not enough:** `(ph != APPROVED && past Existing) \|\| editable` leaves the control on a mark born Vendor added, which `editable` still answers true for there and whose Delete is a Drop all the same (tried: the new test fails on "wish born vadd"). So at Client approved a mark past Existing has no delete control whatever it was born; a Note, a Line, a Tape and a chain keep theirs (a real delete, the page's own answer). `PhaseToolsTest`: the table's sixth cell for the four rows is 0, and a new test walks the seven states past Existing × born × locked — nothing is offered, `move` and `restore` refuse, `delOk` is false at stages 3 and 5 and true at 1, 2, 4 and unread — and requires every delete control that does show at stage 5 to be v52's plain Delete. Both fail on L1's code. Q-list 6(c) stays, reworded: the question is the prototype's |
| 2 | "AC", a shown trade short name, is marked `// text:value` instead of being keyed | **CONFIRMED.** `Trades.kt:83` was `HVAC -> "AC" // text:value …`; the page's TRADES has `short:'AC'` (`P L2385`); no key held English "AC" (`trade.hvac` is "AC & ventilation", `type.ac` "AC unit"); the page's text layer shows `"AC" -> "AC"` under मराठी (run); `AC` is on `latin-ok.txt`. What the phone would show was right in both languages; the mark is for wire and stored values, and this is a shown text — the only one of the three objects not read from the tables | **Keyed** — the first of the reviewer's two fixes, because the slice rule is that a shown literal is born keyed: `trade.hvac.short`, English `AC` (the prototype's own word, never coined), Marathi `AC` (as the page shows it; decision 6), flag `phone-text`. Tx regenerated with `:pano:i18nGen` (1,896 keys, 1,825 accessors), the ratchet run (nothing to lower), `Trades.short` reads `tx.trade_hvac_short`. `TradesTest` gains a test that asks a table whose every cell is a marker for each trade's name and short name; it fails on the literal |
| 3 | `i18n/README.md`'s "What is here" row still says 1,794 keys; L1 pointed at the stale row instead of correcting it | **CONFIRMED.** The row read "1,794 keys each"; L1's own section said the tables held 1,895 and that the row was last brought up to date at M3b | The row is brought up to date: 1,896 keys, its composition counted from each commit's own table (1,794 through K3's review, then 3 in M3b, 1 in M2, 16 in M6, 81 in M8, 1 here), and L1's sentence about the stale row is gone |

`:pano` after the fix, from the XML: 1,083 tests in 80 suites, none failed (1,081 before). `:app` is compiled only
by CI; nothing calls `delOk` or `Trades.short` there yet.

**Seen while confirming finding 1, of the same kind, NOT changed here** (not a reported finding; recorded for the
slice that wires it): at Client approved a mark born Vendor added that has reached **Scope** is still `editable`
— the page's own answer — so its padlock shows, and unlocking it is refused with "… is in Scope — take it Out
of scope in its sheet" (`MarkLife.unlockRefusal`), a move the phone's sheet does not offer there. The padlock is
`editable`'s, shared with move, resize, nudge and duplicate, and `setLocked` is wired in L2: L2 decides whether
that padlock is hidden there too, and until then it is on Q-list 6(c).
**Decided in L2 (11 Nov 2026): hidden** -- `PhaseTools.lockOk`, §3.2 "L2 AS BUILT".

### 3.2 L2 · the state on the face, and the rules that protect a wish (M)
- **Prototype:** `outline` `P L2872`, `ring` `L2887` (a spot), `paintState` `L2898-2911` (after the code: the act
  glyph, the ✎ note badge, V+, then the pill, cut to its first word when crowded), the selected label's 3 dp state
  bar `L2930`, `delWord` `L2936`, `interceptDelete` `L2940-2946`, `unlockRefusal` `L2947`, `clearText` `L2952`.
- **Files:** `app/SurveyPrepDraw.kt` (`drawMarks` `:250`, `spot` `:219`) draws from `MarkLife.stroke` and
  `MarkLife.tag` — Client wish dashed 6/4 with a teal pill, Vendor review 2/3, Vendor added dotted, Client approved
  and Scope 2.5 dp, Dropped grey at 40 % with a red diagonal. `pano/SurveyPrep.kt` `deleteMark` `:1144`,
  `dropUnlocked` `:1185`; `pano/SurveyPrepEdit.kt` `setLocked` `:84`, `lockAll` `:128`, `clearItems` `:318`,
  `startOver` `:348`: a mark past Existing is never deleted (Delete reads Drop, or Restore on a Dropped one), the
  clears and Start over keep it and say so, a Scope mark stays locked.
- **The SAVED pictures use the same painter and must not follow it.** `app/FaceWriter.kt:380-394` calls
  `SurveyPrepDraw.drawMarks(…, Tx.EN, …)` and saves "<face> survey marks" to the gallery for the handover folder;
  its own comment calls them "documents other parties consume". Left as it is, every Save from B2 would write
  states and acts into that JPEG, and from B3 the first 24 characters of an OFFICE-ONLY Note (L6's pin and tag).
  So `drawMarks` takes an audience, and the pure `pano/SheetMarks.kt` decides what a sheet for others draws:
  **the room as surveyed** — every mark born Existing, drawn as v52 draws it (no state outline, pill, act or ✎);
  no mark born a wish or Vendor added (it is not in the room); no Note pin; no why. It is RD's rule (§3.11), so
  the phone's files and S5 give outsiders one answer. A v52 prep gives today's picture to the byte. What these
  pictures should carry once wishes exist (the prototype's rule for a document is the pack, `P L3280-3289`) is
  Q-list 7; until Amit rules, they stay the survey.
- **Tests:** the stroke and tag tables in `MarkLifeTest`; `ClearTest`, `StartOverTest`, `LockTest` gain a wish;
  `SheetMarksTest` (a wish-born mark, a Note, an office-only Note, a why and a Dropped mark born Existing: the
  first three are not on the sheet, the last is drawn as surveyed; a v52 prep's list is unchanged).
  **i18n on:** the eight state names, `life.msg.x_restored_x`, `life.cleared_x_item_x`, `life.x_mark_x_past`,
  `life.x_scope_mark_x`, `refuse.life.x_scope_take_out`.
- **Amit:** nothing yet. Drop needs its why, typed in the sheet, so L2 reaches Play only with L3 behind it.
- **L2 AS BUILT (11 Nov 2026; branch `port62-wish`, local).** New `pano/SheetMarks.kt` and `SheetMarksTest` (10); the
  painter's state in `app/SurveyPrepDraw.kt`; the rules in `pano/SurveyPrep.kt` and `pano/SurveyPrepEdit.kt`, wired in
  `app/SurveyPrepScreen.kt`; `app/FaceWriter.kt` draws for others. `MarkLifeTest` +4 (the width and tag tables, the
  crowded pill in both languages, the label's bar), `ClearTest` +4, `StartOverTest` +2, `LockTest` +3, `PhaseToolsTest`
  +1. `:pano` 1,107 tests in 81 suites, none failed (1,083 in 80 before). No new key, no table change, Tx not regenerated;
  `android/i18n/README.md` ("Port 62 slice L2") lists what a screen can now show. **Nothing here is reachable by a user:
  no control sets a state** (L3's sheet, L4's placement). `:app` is compiled only by CI.
  - **Cross-checked against the prototype's own code.** `proto-tables.mjs` gains an "L2" section: the page's `outline`,
    `ring` and `paintState` run on a recording canvas, in English and under मराठी. Every row of MarkLifeTest's two
    tables equals what it prints (but the Note's: the page never asks `paintState` about one, it paints its pin) --
    the outline's width for each state plain, picked, in the loupe and picked in the
    loupe (a state that names a width keeps it in the loupe; picked is `max(w, 2.5) + 1`); the ring (3 u out, the
    state's width or 2); and 24 marks' tags, crowded and not. 46 one-line mutations of the new rules -- seven of them
    in the app's own sources, which `SheetMarksTest` reads -- each turned their suite red (one survived the first run:
    an UNLOCKED Scope mark counted as "kept locked"; `LockTest` now holds it).
  - **Three things the page's code does that its comments do not say, all ported as it DOES them.** (a) The tag is
    drawn at ITS OWN alpha: `paintState` sets `globalAlpha` (1, or .6 for a Dropped mark) instead of multiplying it,
    so while another mark is picked the rest dim to 75 % and their pills do not. The phone fades a mark through a
    layer, so that layer is closed for the tag and opened again after it (only for a mark that has a tag). (b) An act
    is drawn on ANY mark past Existing that carries one, whatever it was born; an Existing mark's stale act is not.
    (c) An Existing mark born Vendor added shows V+ only beside a note or an act (the early return comes first).
  - **The saved pictures: an allow-list, and `born` read strictly.** `SheetMarks.asSurveyed` keeps a mark's seven
    fields and `locked`, `sub` -- RD's own row for a mark (§3.11) -- so a key a later build adds is office-only
    until named; and a `born` that is anything but absent or `exist` is "not in the room", an unknown value included
    (RD's wording; `MarkLife.bornOf` reads an unknown born as Existing, which is right for the screen and wrong for
    a document). The painter has NO branch on the audience: it draws a state from the mark it is handed, and for
    others it is handed marks that have none. `FaceWriter` gates the picture on the same call (`SheetMarks.any`), so
    a surface that holds only wishes gets no "survey marks" picture, as one with nothing marked never did.
    `SheetMarksTest` reads the app's sources for the wiring (one read of a surface's marks in the painter; no default
    audience; who passes which), and MastersTest holds `SheetMarks` among the whole export builders: no language.
  - **For L3: the stage, and Drop.** `SurveyPrepDialog` holds `val quoteStage: Int? = PhaseTools.SURVEY` until L3
    makes it the parameter this plan gives L3. So in L2 a Dropped mark's Restore is answered by Survey's gate and
    moves nothing -- which is also why L2 ships no control that sets a state. A tap on **Drop** does nothing yet
    (`MarkLife.DeleteAct.DROP_SHEET -> return` in `deleteMark`): L3 opens the sheet there, on Dropped. Neither path
    deletes, and `SurveyPrep.Prep.deleteMark` refuses a mark past Existing on its own.
  - **For L4: the padlock at Client approved -- decided here, as the L1 review left it to L2.** A LOCKED mark in
    Scope has no padlock there (`PhaseTools.lockOk`): its unlock can only answer "take it Out of scope in its sheet",
    a move the phone's sheet does not offer after approval -- the L1 review's rule for its Delete. Everywhere else
    `lockOk` is `editable`. L4 asks it for the padlock and for the menu's Lock row. Q-list 6(c) is reworded.
  - **Not ported, and why.** The Note's pin (`paintNote`, L6): until then a NOTE mark from a file is drawn as an
    unknown type, as before, and `MarkLife.tag` gives it none. `outlinePoly` on the 360 and the List chip's dot (L7).
    The picked label as a button that opens the sheet (L3). The phase's gate on Delete, the padlock and the clears
    (`P54.spDelOk`, `spEdit`, `spBulk`: L4).
  - **Seen, NOT changed -- the Plan tab's drawings and their export.** `pano/SurveyPlan.kt` draws every mark of a
    surface as v52 does, and `SurveyExport` saves that drawing as the A4 sheet that is shared. From B2 a mark born a
    wish is on that sheet as if it were in the room, undistinguished, until L8 (B4) draws wishes there. It is the same
    kind of document as finding 3's pictures, and this plan names no rule for it between B2 and B4: `SheetMarks` could
    hand `SurveyExport` the room as surveyed in one line, but the Plan TAB is the office's own screen and paints
    through the same painter, so it is L8's decision or the plan owner's, not made here. Put with Q-list 7.
    **Decided in the L2 review (finding 1): the exported sheet is the room as surveyed until L8** — below.

#### The L2 review (11 Nov 2026) — five findings on commit `f9e20b0`: one major, four minor

Each was checked against the committed code and against the prototype's own code before anything was changed: the
page now on disk AND the 9,563-line v61c.2 this plan read, which were diffed (finding 5), with `proto-tables.mjs`
run on both. **Confirmed: all five** (finding 5 with its "+26 throughout" corrected). **Declined: none.** Fixed in
one commit on `port62-wish`, local. Each code fix has a `:pano` test that FAILS without it — tried: the four
fixes were put back one by one in a throwaway copy and six tests went red (`LockTest` 1, `MarkLifeTest` 3,
`SheetMarksTest` 2), then the files were restored and checked by hash.

| # | Finding (severity) | Verdict, and what was checked | Fixed how |
|---|---|---|---|
| 1 | From B2 until L8 (B4) the Plan tab's exported A4 sheet would draw a mark born a wish as if it were in the room (major) | **CONFIRMED.** `SurveyPlan` reads `prep.surfaces[s]?.marks` at five places (`:306`, `:372`, `:413`, `:523`, `:535`) with no filter, and draws a D, W or O on a wall as "a gap through the wall band" (`:310-312`); `SurveyExport.sheet` drew from it in `Tx.EN` (`:157-159`); the app's only caller is `planSheet()` (`SurveyPrepScreen.kt:1380`), whose sheet `PlanExport` saves to Downloads and shares (`:1412`). Nothing reaches it in L2 (no control sets a state), but B2 = L1–L4 would: a door the client WISHES for, cut into the wall of a shared drawing. It is the plan critic's finding 3 again, for a document L2's builder saw and left to the plan owner | **The sheet draws the room as surveyed** — the reviewer's first fix, and this plan's own rule for the saved pictures (the critic's correction). New pure `SheetMarks.prep(prep, audience)`: the office gets the prep itself; others a prep whose every surface holds `SheetMarks.marks(…, OTHERS)` and shares everything else (lines, readings, Tapes, Lines, chains, the reference), read only — and a prep with nothing to leave out, every v52 prep, is answered as ITSELF, so its sheet is what it always was. `SurveyExport.sheet` hands `SurveyPlan` that prep and never its own. **The Plan tab ON SCREEN is unchanged** (the office's: every mark, drawn as v52 draws it until L8) — so between B2 and B4 the drawing on screen shows a new wished door and its exported sheet does not; that is said in §7 and stays on Q-list 7. `SheetMarksTest` +3: on all six drawings the sheet of a room with a wish round (a wished window, a Dropped door, a Scope socket; a door, a window, an opening, a Note, two units and two overhead things that were never in the room) is BYTE FOR BYTE the sheet of the room as surveyed, none of the eight new codes is on any sheet while each is on the screen's drawing, the sheet's wall is cut at the two surveyed openings only, and the prep is unchanged; `prep()`'s sharing and identity; and, read from the sources, that `SurveyExport` hands `SurveyPlan` nothing but that prep and that in the app only `SurveyPlanScreen` makes a drawing and only `planSheet()` a sheet. **L8** (§3.8) replaces this rule with the prototype's own for a drawing, and changes these tests on purpose |
| 2 | "V+" is a shown text marked `// text:value` instead of keyed, which the L1 review ruled against for "AC" (minor) | **CONFIRMED.** `MarkLife.kt:110` was `const val VPLUS = "V+" // text:value …`, drawn on the face after a mark's code (`SurveyPrepDraw.kt:326-330`). The page paints it from a literal too (`const t='V+'`, paintState) and OUTSIDE its text layer — the act and the pill go through `V57L`, V+ does not — so it reads V+ under मराठी there; no key held it (the only V+ in the tables is inside `tour.vendor.6.body`, "the V+ badge" / "V+ बॅज"). It is letters a person reads, the abbreviation of Vendor added, not a glyph: the same case as AC, and the README's rule for the mark is "a wire or stored value, a product name, a unit or a code" | **Keyed** — the reviewer's first fix: `life.vadd.badge`, English `V+` (the prototype's own text), Marathi `V+` (as the page paints it and as the tour's own Marathi names it), flag `phone-text`; `V` on `latin-ok.txt` now names both keys. `MarkLife.Tag.vplus` is the TEXT (`String?`, null: none) and `MarkLife.VPLUS` is gone; the painter draws what the tag hands it. `MarkLifeTest`: a table whose every cell is a marker must answer every state's pill and the badge with one cell (as `TradesTest` asks for the trade names), and `MarkLife`'s code may hold no `"V+"`. What the phone shows is unchanged in both languages. ✎, the acts' glyphs and ` · ` stay marked: glyphs and a separator |
| 3 | Under मराठी the delete control on a wish reads "ड्रॉप", the Ceiling drop type's short name, while the state it leads to reads "वगळले" (minor) | **CONFIRMED — a table matter, as the reviewer says, and the tables already hold the answer.** `MarkLife.deleteLabel` read `tx.type_cdp_short`, whose `where` lists two unrelated uses of the English word (`trades53.js:40`, the type; `life53.js:193`, delWord). The page shows `"Drop" -> "ड्रॉप"` and `"Drop FW1" -> "ड्रॉप FW1"` (run on both builds) only because its text layer finds a text BY ITS ENGLISH WORD; the same tables say `life.drop` वगळले, `life.dropped` → वगळले, and — the sentence L2 itself shows after a clear — "drop them one by one" as "ते एकेक करून **वगळा**" (`life.x_mark_x_past`). So one control would have named the act by a transliterated noun and the next line by its Marathi verb. The English is the prototype's word and does not change; the Marathi is Claude's to finalise (decision 6, 28 Oct), so it is not a prototype question | **The verb has its own key** — the reviewer's first fix: `life.drop.verb`, English `Drop`, Marathi `वगळा`, flag `phone-text` (the page draws the word through the type's key; `type.cdp.short` keeps its `where` and its ड्रॉप for the Ceiling drop type, L5 and L6). `MarkLife.deleteLabel` reads it, so the picked row's button, a list row, the menu's first row and the head of their screen-reader labels (`वगळा FD1 दरवाजा`) follow. `MarkLifeTest` (the seven Drop rows; Drop a cell of its own; `MarkLife` may not name `type_cdp_short`) and `LockTest` (the row and its label) — both fail on L2's code. `type_cdp_short` has no reader again until L5 |
| 4 | A Restore refused at Survey takes an undo snapshot first; the prototype answers before any snapshot (minor) | **CONFIRMED.** `SurveyPrepScreen.kt:722-726`: `snapshot(); val moved = MarkLife.restore(…); if (!moved.ok) { hist.removeAt(hist.size - 1); … }`, and `snapshot()` (`:387`) is `hideSnack(); hist.add(prep.copy()); if (hist.size > 40) hist.removeAt(0)` — with 40 steps held a refused tap drops the OLDEST and then its own, leaving 39. The page: `if(stage===0){ H.snack(T_SURVEY,false); H.render(); return true; }` and only then `H.snapshot()` (interceptDelete). Unreachable in L2 (no mark can be Dropped), reachable from L3 at Survey and at the phone's own gate | **The gate is asked first**, as the reviewer wrote it: `val gate = PhaseTools.gate(quoteStage); if (gate != null) { snack(PhaseTools.gateText(tx, gate), undo = false); return }`, then the snapshot, the move and — for the refusals the page also snapshots for (a move not offered: Estimation) — the pop, as before. The words do not change: at a gate `MarkLife.restore` answers the gate's own line. The order is the app's, compiled only in CI, so `MarkLifeTest` reads `deleteMark`'s RESTORE branch from the source (gate, its line, ONE snapshot, then the move) and pins beside it that the gate's line is what a refused Restore says at stages 0 and 5 in both languages, and that stages 1–4 and unread have no gate. **L3 keeps this order** when `quoteStage` becomes the dialog's parameter |
| 5 | The page line numbers in L2's comments and in this plan are 26 off the prototype file now on disk (minor) | **CONFIRMED, and the "+26 throughout" CORRECTED.** The file at the builders' path is 9,657 lines, written 11 Nov 13:18 UTC; this plan read 9,563. The reviewer did not diff the two builds; this pass did (the 9,563-line page is still in the scratchpad, `critic/site/`). The page on disk is **mock-up v62a** — v61c.2 plus the prototype items P0 and P2, to approve — and the offset is +26 only for lines 1715–6923: +7 before that from 1596, +27 from 6924 (the reviewer's own "Start over 6969 (6942)" is +27), and up to +94 from 7924 on. No hunk touches T53, L53, P54 or survey prep's delete, lock, clear and Start over; `proto-tables.mjs` prints the same rows from both pages | No code change, as the reviewer says: the comments cite v61c.2, the approved build. The offsets are a table at the head of this file, under the `P L<n>` rule, for the L3 and L4 builders — who should also know that **the page at that path is no longer the approved v61c.2 alone**: it carries P0's rename dialog and P2's room chip, which are not approved and are no part of L3 or L4 |

`:pano` after the fix, from the XML: 1,112 tests in 81 suites, none failed (1,107 before: `MarkLifeTest` +2,
`SheetMarksTest` +3). Two keys added, both `phone-text` (1,896 → 1,898; Tx regenerated with `:pano:i18nGen`, 1,827
accessors; `i18nRatchet` lowered nothing; `unkeyed.tsv` still empty). `:app` is compiled only by CI — two files
changed there, `SurveyPrepDraw.kt` (the badge's text from the tag) and `SurveyPrepScreen.kt` (the gate before the
snapshot) — and nothing has been seen on a phone.

### 3.3 L3 · the Mark sheet (M) — the first thing he can do
- **Prototype:** `openSheet` `P L3012`, the sheet `L3024-3063`: the head (code · name, the trade's dot); STATE (the
  now-line "● Client wish · since … IST · decided by the client", then the phase's moves with their hints, a Why
  box under a move that needs one); the four **acts** on a mark born Existing that has left or is leaving Existing
  (`L3040-3042`: ↻ Replace · ⇢ Shift · ⚒ Repair · ✕ Remove); NOTE (two lines, 0/280, "Client & vendors | Office
  only"); ONE Done; ✕, the scrim and ◁ cancel; `apply` `L3071-3088`, one undo step per sheet. Gates: Survey's
  "Finish the survey first — …", an unread stage's footer, a locked mark ("only its note can change"), the amber
  line after the estimate. A Line or area gets TRADE and NOTE only (`L2783`). **TRADE is also on an Other**
  (`takesCat`, `L3027`: `e.t==='X'` or a Note; the 13-trade picker `L3059`) — in L3, not L6, so an Other can be
  given its trade from the first build.
- **What the sheet offers, stage by stage** (`moveOk` `L3985-3988`; driven 11 Nov on FW1):
  Survey and a project not on S5 — the gate line, no move. Client wish list — Existing: → Client wish; a wish:
  → Dropped, ← Back to Existing; a Dropped mark: ↺ Restore. Vendor quoted and Revisions — the table's moves past
  the wish (the one-vendor model, §7). Estimated — no move: "<state> · Estimation moves nothing — move the phase
  back on the project screen". Unread — every move and the footer.
  **Client approved — PROVISIONAL, Q-list 6.** `moveOk` names no rule for it and falls through to Vendor
  review's: driven, a wish is offered → Vendor review and → Dropped, a Client approved mark → Scope, ← Client
  wish and → Dropped. That would record a change after approval somewhere other than the change requests
  (Amit, 28 Oct: the wish list locks at Client approved; change requests are the ONE place). Until he rules on
  the prototype the phone offers NO move there: STATE shows the now-line and, in the slot Survey's gate uses, the
  approval lock's own words (`refuse.readonly.wishlist_approved`, "Approved — changes go through change
  requests"). The NOTE field is drawn as the prototype draws it at every stage; whether it locks too is the same
  question.
- **Files:** new `pano/MarkSheet.kt` (the sheet's state and `apply`, pure) and `app/MarkSheet.kt` (a bottom sheet,
  keyboard-aware, no scroll in either orientation); `app/SurveyPrepScreen.kt`: the selected mark's code opens it,
  Delete on a mark past Existing opens it on Dropped (wherever `PhaseTools.delOk` draws it: not at Estimated
  and, since the L1 review, not at Client approved); `SurveyPrepDialog` (`:181`) is given the project's stage
  at its two call sites (`MainActivity.kt:519`, `:3838`) by the rule at the head of §3 — Survey's gate for a
  project not on S5, null only for a listed project whose stage is unread. In `deleteMark` a Restore asks the
  phase's gate BEFORE its undo snapshot (the L2 review, finding 4; `MarkLifeTest` reads that order from the source)
  — L3 keeps it, and opens the sheet on Dropped the same way, with no snapshot of its own (one undo step per
  sheet, taken by `apply`). **Prep:** it writes `life`, `lifeAt`,
  `lifeAs`, `act`, `why`, `note`, `vis` (and `cat` on a Line, an area or an Other).
- **Tests:** `MarkSheetTest` — nothing changed closes with no undo step; a move that needs a why keeps Done off
  until 3 characters; `act` written only on Existing → Client wish; the note capped at 280 code points; "Office
  only" kept only with a note; a locked mark changes its note alone; an Other takes a trade and `cat` is written
  only when not General; the offered moves at each of the six stages, unread and not-on-S5 (no move at Survey,
  Estimated, Client approved and not-on-S5).
- **i18n on:** `life.aria.mark_sheet`, the move labels (`life.client_wish`, `life.dropped`, `life.back_existing`,
  `life.restore` and the vendor-side ones), the acts (`life.replace`, `life.shift`, `job.repair`, `common.remove`),
  `life.why_least_3_characters`, `life.note`, `life.decided_x`, `life.since_x_x_ist`, `common.client_vendors`,
  `common.office_only`, `refuse.life.finish_survey_first_stage`.
- **Amit can** (once B2 = L1–L4 is on Play; L3 never ships alone): at Client wish list, tap a window's code,
  then → Client wish → ↻ Replace → "install UPVC window" → Done; a door → ✕ Remove → "close with brickwork".
  The mark is dashed, with the pill, the act and ✎.

- **L3 AS BUILT (11 Nov 2026; branch `port62-wish`, local).** New `pano/MarkSheet.kt` (the sheet's state, everything it
  draws, its Done) and `MarkSheetTest` (23); new `app/MarkSheet.kt` (the bottom sheet, `SpMarkSheet`); wired in
  `app/SurveyPrepScreen.kt` (the `quoteStage` parameter, `openSheet`, `sheetDone`, the three codes that open it, Drop);
  the stage handed at both call sites of `MainActivity.kt` through the new `QuoteStageSync.prepStage` and a second
  `WishSync.stageKey`. `:pano` 1,135 tests in 82 suites, none failed (1,112 in 81 before). Two keys added (below); Tx regenerated; `android/i18n/README.md` ("Port 62
  slice L3") lists every text. `:app` is compiled only by CI and NOTHING HAS BEEN SEEN ON A PHONE.
  - **Cross-checked against the prototype's own code, and driven.** `proto-tables.mjs` gains an "L3" section: the page's
    own L53 module bound to a stub host, its sheet PAINTED by its own `paint` and driven through its own click, input
    and `apply` handlers. Its first table -- the sheet for 21 kinds of item at stages 0-5 and unread, 147 rows -- IS the
    test's golden (`pano/src/test/resources/marksheet/proto-sheet-rows.txt`, cut from the script's output as its header
    says): `MarkSheetTest` prints `MarkSheet.view` in the same form and compares row for row -- every text, every dot's
    colour, every control's on / off -- for stages 0-4 and unread; at stage 5 it pins the phone's own rule instead. Its
    journeys (17; 18 since the L3 review) are the expected values of the hand-written tests: the sheet's state after each tap and keystroke, the
    keys Done writes IN THEIR ORDER, and the undo steps. Both builds of the page print the same L3 rows (v61c.2, 9,563
    lines, and the v62a page now at the builders' path). The page was also driven in a browser (Playwright, the Demo
    project, portrait 360 x 708 and landscape 732 x 336, English and मराठी) and its sheet measured: every size in
    `app/MarkSheet.kt` is a measured one. 73 one-line mutations -- 66 of `MarkSheet`'s rules, 7 of the app's wiring, which
    the test reads from the sources -- were run against `MarkSheetTest` in a throwaway copy: 71 turned it red at once,
    and the two that survived (a why of EXACTLY three characters; which Why box takes the keyboard where there is none)
    each got the assertion that now kills it. **The app's half was not compiled** (no Android SDK here). The nearest
    check there is: `kotlinc` 2.0.21 over `app/MarkSheet.kt` against the real `:pano` classes and hand-written
    signature stubs of the Compose calls it makes (in the scratchpad, not a repository) -- it type-checks clean -- and
    over every app source for syntax: none wrong, and no error on a line this slice touched beyond Android names the
    stubs do not carry. That proves the file's own Kotlin, its imports and its use of `:pano`; CI is the compiler.
  - **What the page's code does that this plan does not say -- ported as it DOES them.** (a) The act chips are drawn on
    every mark born Existing that is past Existing -- a Dropped one and one in Scope too -- **at Estimation as well**,
    and an act chip changed there, with no move, IS written: `apply` asks the stage only through the moves. So at
    Estimation the sheet still changes an act, the note, its audience and an Other's trade (Q-list 9). (b) An act chip
    changed beside any move but Existing → Client wish is DROPPED: `move` writes the act on that one move, and the
    chip-alone write needs no move picked. (c) Done is never off on screen: a why cut under three characters un-picks
    its move (`onInput`), so what waits for the why is the MOVE's button; `MarkSheet.view` still answers Done off for a
    state that holds such a move, and `apply` writes nothing for it. (d) A why typed in the box the sheet OPENED on
    (Delete on a wish) picks Dropped at its third character; typed in any other box it does not -- a tap does. (e) A
    mark past Existing with no `act` stored (no build writes one) gets `act: replace` from any Done, with an undo step:
    the chip stands on Replace and differs from "none" -- at Survey too, where no chip is drawn; **not at Client
    approved since the L3 review** (below). (f) A stored `vis` that is not `office` is deleted by any Done.
    (g) "decided by …" is drawn whenever `lifeAs` is stored, on an Existing mark too; a `lifeAt` that is no time gives
    no "since". (h) The note, its audience and TRADE are the item's at EVERY stage, Survey included: a note on a
    surveyed mark is not a wish.
  - **Decided here, where the plan was silent or the phone must differ.**
    1. *Whose stage.* The dialog is given the stage of THE CAPTURE'S OWN PROJECT -- its ERP id, client and title as the
       capture's row (or the pending split) holds them, through `WishSync.stageKey`, then `QuoteStageSync.prepStage` =
       `PhaseTools.stageOf` over the one store's copy -- not of whatever project the tree shows. It is read on every
       recomposition of the call site, so a sync that moves the stage while survey prep is open reaches it (the page
       reads the stage on every render). No default on the parameter. **For TR:** the tour's Demo project is a project
       S5 does not list, so its survey prep opens under Survey's rules whatever `TourState.demoPhase` says; the one
       tour that ships (`survey`) runs at phase 0. The `phases` and `wish` tours need that phase handed here.
    2. *The texts.* An act chip is the whole key `act.*.sym` ("↻ Replace"), not a glyph glued to the name key this plan
       listed: the tables hold exactly the chip's text, and it is what the page's own layer finds for it; the test
       holds the two equal. STATE is `common.state` in capitals and "staff" `album.staff` lower-cased, as the page's
       layer shows them. **Two keys added**, the page's own English: `life.note_length_optional`,
       `life.note_area_optional` -- a Line's and an area's note placeholder, which the page glues from three pieces
       (one of them unkeyed) and so shows in English under मराठी.
    3. *The sheet's window.* material3's `ModalBottomSheet`, `skipPartiallyExpanded = true` (it opens whole, as the
       album's sheets), no drag handle, full width in landscape, the prototype's colours. A SWIPE DOWN cancels it,
       which the page has not: material3 gives every sheet one.
    4. *The keyboard.* The page never has one. The sheet rises over it (`imePadding`), and only when the blocks then
       need more height than is left do they scroll, between the head and Done, which stay put (in landscape Done sits
       in the left column, as drawn, and scrolls with it). With the keyboard down nothing scrolls in either
       orientation at 360 x 780 -- this plan's "no scroll" (Q-list 10).
    5. *The trade picker* is a Popup over the sheet, 8 dp in from its sides and foot (460 dp wide at the right in
       landscape): on a Line's short sheet it stands taller than the sheet, as on the page. It takes no focus and no
       outside tap, so the page's three cases hold: the TRADE button shuts it, any other control does its own work, a
       bare part of the sheet shuts it. ◁ cancels the sheet, as the page's does.
    6. *V+ on the now-line* is a filled span in the badge's colour: no 3 dp corners, no padding (it flows with the
       line, which wraps in portrait).
    7. *The Why box takes the keyboard* when Delete on a wish opened the sheet (the page focuses it).
    8. *KIND* is not drawn: the phone has no sub-kind until L5, and its picker is L6's. In landscape its row's gap is
       kept, so TRADE and NOTE sit where the page has them.
    9. *A Note a file already holds* opens the sheet like any item (TRADE and NOTE, no STATE); its head reads the type's
       token, `NOTE`, until L5 gives the phone the type. Done with no words says the page's line and stays open -- on
       the snackbar UNDER the sheet, where it cannot be read; nothing can place a Note before L6, which brings its
       `fresh` sheet and should say that line on the sheet.
  - **Where it opens.** The picked mark's label under the photo (2 · Mark and 4 · 360), a picked Line's or area's code,
    a list row's code (3 · List and the List tab), and Drop on a mark past Existing. A Tape, a chain and the reference
    have no sheet (`MarkSheet.opens`): their code is no button.
  - **For L4.** The sheet is NOT gated by `PhaseTools.editable`: the page opens it on any mark at any stage, and only
    its moves follow the phase. `delOk` hides the Delete that opens it on Dropped at Estimated and at Client approved;
    until L4 wires that, a tap on Drop there opens the sheet on the line that says why nothing moves (B2 never ships
    without L4). `quoteStage` is the dialog's parameter now: the band, the strip and the palette read the same value.
  - **Not ported, and why.** KIND and `setSub` (L5, L6); a new Note's `fresh` sheet and `removeFresh` (L6); the List's
    "State: … ▾", "Move N ▸" and `bulkMove` (Q-list 3); `openSheet`'s `focus:'note'` (no caller on the page).

#### The L3 review (11 Nov 2026) — one finding on commit `2190179`, minor

Checked against the committed code and against the prototype's own code before anything was changed: the fault
was first reproduced as a FAILING `:pano` test on L3's code, and the page's own answer was run for the record —
`proto-tables.mjs` gains one journey ("... the same at Survey (stage 0) and at Client approved (stage 5)"), the
page's own `openSheet`, input and `apply` handlers at both stages. **Confirmed: the one finding. Declined: none.**
The reviewer's clean checks (the golden re-cut and byte-identical, the page driven at stages 0–5 and unread, the
note's cut, the geometry untouched, the app's half read call by call, the i18n tree) were not repeated beyond what
the fix touches: the golden was re-cut from the script after its new journey and is still byte-identical.

| # | Finding (severity) | Verdict, and what was checked | Fixed how |
|---|---|---|---|
| 1 | At Client approved, Done can still write `act` on a mark, though the sheet draws no act chip there (minor) | **CONFIRMED.** `MarkSheet.apply`'s `actCh` (`:363-364`) asked: no move picked, a mark that is no Note, born Existing, past Existing, not read-only, `st.act != st.act0` — and never the gate, which `actsShown` (`:125`) and `pickAct` (`:161`) both ask. `open` (`:106`) sets `act = act0 ?: REPLACE`, so a mark past Existing whose stored `act` is absent or none of the four opens with `act = replace`, `act0 = null`, and they differ before anything is touched. The new test on L3's code: `{life: wish, lifeAs: client}` at stage 5, open then Done → `DONE · undo 1 · {life=wish, lifeAs=client, act=replace}`, while the sheet shows no chip, no move and the approval lock's line. And a mark whose `act` this build does not know (`closeup`) had it REPLACED by `replace` by a note-only Done there. **The page** (run): at stage 5 it paints Vendor review's sheet — `acts drawn: true` — and writes `act: replace` in all three cases; so the write follows a drawn control on the page and none on the phone, whose no-chip rule there is its own (Q-list 6, the plan critic's finding 1). Reach is narrow, as the reviewer says: `MarkLife.move` writes `act` on Existing → Client wish, so only a file another writer made, or one holding an act this build does not know, gets here — but it is a wish's act written after approval, somewhere other than the change requests, with no control shown | **`actCh` is false at the phone's own gate** — the reviewer's fix, one term: `&& PhaseTools.gate(stage) != PhaseTools.Gate.APPROVED`. Nothing else in `apply` changes: at Client approved the note with its audience and an Other's trade are still written, one undo step (provisional, Q-list 6 and 9). **Survey is left as the page has it**, as the reviewer asks: the page's `actCh` has no stage term (`P L3076`) and at stage 0 it draws its gate's line and NO chip, yet writes `act: replace` in the same three cases (run: `acts drawn: false`, then the three writes) — the phone does what the page DOES there, the test pins it, and it is added to Q-list 9 as the prototype's question. `MarkSheetTest` +1: at stage 5 a wish with no stored act, open then Done → `UNCHANGED`, 0 undo steps, the map not even replaced, no `act` key; the same with a note typed → the note written, `act` still absent; an unknown act kept, untouched and beside a note; an Other's trade written with no act; a state that holds another act (the screen cannot make one) writes none; and at stages 0–4 and unread the page's own values. It FAILS on L3's code (run first, before the fix: "expected UNCHANGED · undo 0 … but was DONE · undo 1 · {…, act=replace}") |

`:pano` after the fix, from the XML, forced fresh: 1,136 tests in 82 suites, none failed, none skipped (1,135
before: `MarkSheetTest` 23 → 24). No key added, Tx not regenerated, `i18nRatchet` lowered nothing, `unkeyed.tsv`
still empty. `:app` is unchanged by this fix (it calls `MarkSheet.apply` as before), is compiled only by CI — and
nothing has been seen on a phone. If Amit's answer to Q-list 6 / 9 makes the sheet read-only at approval, this
term goes with it.

### 3.4 L4 · the phase in survey prep, and a NEW thing placed as a Client wish (M) — closes B2
L4 carries the phase's gate on the mark tools TOGETHER with the "as …" placement, so no build can place a mark
where the prototype has no tool. (The first version of this plan left the gate to L10, three builds later: on
B2–B4 a tap at Estimated or after approval would have placed a mark born a Client wish.) The band moves here
with the gate, because a hidden tool needs the line that says why.
- **Prototype:** `asPlace` `P L2848`; the chip's second half `L2860`; AS_FIXED `L3964`; the TOOLS rows `mark`
  and `palette` (`L3949`, `L3953`) and `spPre`'s moving-on of a hidden step and tool (`L4208-4210`); the band's
  words `L3931-3941`, its place (top in portrait, over the photo's foot in landscape, `L4199`) and its face at
  Client approved (`L4219-4221`); `placed` `L2959`. Earlier-phase marks are read-only: `editable` / `delOk`
  `L4232-4237`, the controls HIDDEN, never greyed (`spPost` `L4255-4263`), the words `refuseText` `L4241`.
- **By stage** (driven 11 Nov at stages 1, 3 and 5; the rest read):

  | Stage | 2 · Mark and the palette | The chip's second half | The band |
  |---|---|---|---|
  | Survey; a project not on S5 | shown | none | "1 · Survey · capture · bound · mark what is there · readings" |
  | Client wish list | shown | "· as Client wish", plain words | "2 · Wish list · every mark you place is a client wish" |
  | Vendor quoted | shown | "· as Vendor added", plain words | "3 · Vendor review · …" |
  | **Estimated** | **HIDDEN** — the mode goes to 3 · List, the tool back to Select, no tools row | never drawn | "4 · Estimation · hand-off · read-only · nothing new is marked" |
  | Revisions | shown | "· as Vendor added", plain words | "5 · Revisions · …" |
  | **Client approved** | **HIDDEN** — `can()` reads the missing sixth column as 0; mode List, no tools row | never drawn | "Client approved · <the project's one line>" |
  | listed, stage unread | shown | the "as Existing ▾" button, cycling Existing → Client wish → Vendor added per capture (`L2857-2858`) | "Phase not read yet — S5 decides when online" |

  At stages 3 and 5 `AS_FIXED` is null / undefined, so `asFor` would fall through to 'wish' with the cycling
  button (`L2857`, `L2860`) — but the palette is hidden at both, so the prototype never draws that chip, and
  neither does the phone: `PhaseToolsTest` pins that the placement rule is not asked where the palette is hidden.
- **One thing the prototype draws at Client approved that the phone does not copy — Q-list 6.** Driven: the
  step chips 1 and 2 stay visible there (the CSS hides 1 · Lines at phases 1–4 and 2 · Mark at phase 3 only,
  `P L1272-1274`) while `spPre` bounces any tap back to List. A chip that cannot be entered is a prototype gap,
  not a design; until Amit rules, the phone shows 3 · List alone, as at Estimated.
- **Files:** `pano/PhaseTools.kt` (wired in), `pano/SurveyPrepPalette.kt` `chipText` (`:108-112`);
  `app/SurveyPrepScreen.kt` (the band; the step strip and palette by stage; place, duplicate, nudge, drag, lock,
  the trash and the ⋮ acts ask `PhaseTools`); `pano/SurveyPrepEdit.kt` `duplicate` `:264`. **Prep:** a new mark
  gets `born`, `life`, `lifeAt`, `lifeAs` once, at placement; never a Note. (**A DUPLICATE is not such a
  placement on the prototype** — found building L1: the page copies the box alone and stamps nothing. Q-list 8.
  **Since the L4 review the phone makes that copy of a MARK only where it is what a tap would place** — Survey, and
  unread with the chip on "as Existing" — `PhaseTools.copyOk`, below.)
- **Tests:** `PhaseToolsTest` (the table above, row by row), `PaletteTest` (the chip per stage), `DuplicateTest`,
  `PlaceAtTest` (nothing is placed at Estimated, at Client approved, or as a wish on a project not on S5).
  **i18n on:** `phase.band.*`, `sp.toolchip.as_wish`, `sp.toolchip.as_existing`, `life.aria.marks_are_placed_x`,
  `refuse.phase.x_was_marked_x`, `refuse.phase.x_estimation_changes_nothing`.
- **B2 and the exported drawings (the L2 review, finding 1).** From this slice a mark can be BORN a wish, and
  until L8 the six drawings draw every mark as a surveyed one. The exported A4 sheet therefore draws the room as
  surveyed (`SurveyExport.sheet` through `SheetMarks.prep(…, OTHERS)`, in since the L2 review): B2 is not merged
  with `SheetMarksTest`'s three sheet tests red or removed. The Plan tab on screen still draws the new mark, plain.
- **Amit can:** at Client wish list, pick a type and tap the photo: the box is born a wish — with today's twelve
  types until B3 (for one build a new wardrobe is an "Other" with its trade, from L3, plus a note). At Estimated
  and after approval survey prep opens on 3 · List with no mark tools, under the band that says why.

- **L4 AS BUILT (11 Nov 2026; branch `port62-wish`, local).** `pano/PhaseTools.kt` (`PREP_HONOURS`, `prepShows`,
  `prepSteps`, `prepMode`, `place`, the three rules and `refuseText` by an item's id, `Band.rest`),
  `pano/SurveyPrepPalette.kt` (`Chip`, `chip`, `asName`), `pano/SurveyPrepEdit.kt` (`menuItemsOf` with the stage; what
  `duplicate` copies, written down); in the app `SurveyPrepScreen.kt` (the wiring), `SurveyPrep360.kt` (the same rules
  on the 360), `SurveyPrepTools.kt` (`SpPhaseBand`; the chip's second half), `SurveyPrepDraw.kt` (no tabs on a
  read-only mark), `SurveyPrepStore.kt` (the capture's remembered "as"), `QuoteStageSync.prepLine` and the dialog's
  second parameter at both call sites of `MainActivity.kt`. Tests: `PhaseToolsTest` +8, `PaletteTest` +4,
  `PlaceAtTest` +6, `DuplicateTest` +1. `:pano` 1,155 tests in 82 suites, none failed, none skipped (1,136 before).
  No key added, Tx regenerated to the same file, `i18nRatchet` lowered nothing, `unkeyed.tsv` empty;
  `android/i18n/README.md` ("Port 62 slice L4") lists every text now drawn. `:app` is compiled only by CI and NOTHING
  HAS BEEN SEEN ON A PHONE.
  - **Cross-checked against the prototype's own code, and driven.** `proto-tables.mjs` gains an "L4" section: the
    page's own `P54.spPre` run on a stub survey-prep root (the step and tool it moves on, the band it writes, in
    portrait and landscape), L53's `asChipHTML` / `cycleAs` / `asPlace` per stage, and `spEdit` / `spDelOk` /
    `spBulk` / `refuseText` for eight kinds of item. The page was also DRIVEN (Playwright, the Demo project, 360 x
    780 portrait and 732 x 336 landscape, English and मराठी, stages 0-5 and unread): the band's text, place and
    measured box; the strip; the chip's words, its second half's tag (a `span` where the phase fixes it, a `button`
    while unread) and labels; a real tap that places a mark and the keys it gains; the picked row's controls, the
    floating trash, the long-press menu's rows and ⋮'s buttons for a surveyed mark, a mark just placed, its copy and a
    Line; a drag on a surveyed mark; the cycle. Every expected value in the four test classes is written by hand
    from those two runs. 28 one-line mutations -- 20 of the `:pano` rules, 8 of the app's wiring, which
    `PhaseToolsTest` reads from the sources -- were run against the four suites: 26 turned them red at once; one
    survivor got its assertion (the menu's Unlock on a locked Scope mark), the other was a line that could not
    change an answer and is gone. **The app's half was not compiled** (no Android SDK here). The nearest check:
    `kotlinc` 2.0.21 over every app source against the real `:pano` classes and the L3 signature stubs, widened in
    the scratchpad for the calls this slice adds -- no syntax error, no new error against the same run on L3's
    commit beyond Android names the stubs lack, and `SurveyPrepTools.kt`'s two new composables type-check clean.
    That proves this slice's Kotlin and its use of `:pano`, not Compose; CI is the compiler.
  - **WHICH ROWS OF THE TABLE L4 HONOURS: `mark` AND `palette`, AND ONLY THOSE** (`PhaseTools.PREP_HONOURS`). This
    plan says two things about B2 and they do not agree: §3.4's "Amit can" has survey prep OPEN on 3 · List at
    Estimated and after approval, which needs 1 · Lines hidden there too; §3.10 and §7 give `lines` to L10 ("B2:
    the mark tools at Estimated and after approval; B5: Lines, readings and Capture 360 between Survey and
    approval"), with `StepRouteTest` by stage among L10's tests. L4 follows the second: the strip keeps 1 · Lines in
    every phase and survey prep still opens on it, as 0.3.318 does; at Estimated and at Client approved the strip
    reads 1 · 3 · 4 (each step keeps its own number, as the page's hidden step leaves the others'), a step set to
    2 · Mark shows 3 · List, the tool is Select, and there is no palette, no More sheet and no chip. **"Opens on 3 ·
    List" comes true with L10**, which adds `LINES` and `READINGS` to that one set: `prepSteps` / `prepMode` then
    answer what `steps` / `modeFor` answer and can be folded into them (`PhaseToolsTest` pins exactly where the two
    differ today: 1 · Lines at stages 1-5). The screen already hands `prepMode` whether the open face can be marked,
    asked only once Lines is hidden. Until L10 the four lines can still be moved in every phase, as today -- at
    Client approved too, where the page shows the step and bounces it (Q-list 6).
  - **The step and the tool are DERIVED, then made permanent.** The dialog keeps the step and tool as last set and
    reads `PhaseTools.prepMode` / `toolFor` of them on every recomposition, so a sync that moves the stage while
    survey prep is open takes the step and the tool with it in the same frame (the page's `spPre` runs first in every
    render); an effect then writes the moved-on values back, as `spPre` writes `st.mode` and `st.tool`, so a stage
    moved back later does not bring a hidden step back by itself. A press routed to where a hidden step was, and a
    tour's `mode` move, do nothing (`setMode` and the step router read `prepSteps`).
  - **The band** (`SpPhaseBand`, sizes from the page's CSS and its measured boxes): 20 dp; five 8 x 3 dp pips, 2 dp
    apart, lit up to the phase; the head bold in gold; the words in #9AA096 with an ellipsis, amber while unread; 11
    sp on a 20 sp line, 0.3 sp tracking. Portrait: the screen's FIRST line, over the bar, 6 dp inset (the page: first
    child of `#spctl`, `[10, 8, 340, 20]`); ⋮'s menu drops 20 dp with the bar (CSS `L1283`). Landscape: over the foot
    of the LEFT column -- the photo, 3 · List or the Plan drawing -- on black at 72 %, its lower corners the photo's
    12 dp, 10 dp inset (the page: last child of `#spmain`, `[8, 310, 388, 20]`, in list mode the same box); the note
    and the snackbar are drawn after it, over it, as the page's z-order has them; it takes no touch. At Client
    approved its second part is the project's ONE derived line (`QuoteStageSync.prepLine` = `lineFor(...).text`
    while that line is the execution line), read at the call site on every recomposition like the stage.
  - **The chip.** `SurveyPrepPalette.chip` gives the whole text and where its second half starts; `SpToolChip` draws
    that half bold in the same run of words, so it wraps with them. Where the phase fixes it, it is plain words; while
    the stage is unread it is underlined and a press that lands ON those words cycles it (told from any other press
    by the text's own layout: every other press on the chip is still the photo's, as the page's chip takes no touch
    but its buttons). What it was cycled to is kept per capture in `<id>.as` beside the prep (`SurveyPrepStore`; the
    page's `mcft-placeas-v53`), never uploaded, deleted with the capture. **Not copied:** the page's second half is
    an inline element that keeps the BUTTON's 32 px line even as plain words, so its chip is 55 px tall in portrait
    and 72 px where the words break (measured); the phone draws the half on the chip's own 15 sp line (Q-list 12c).
  - **A placement.** Both views call `PhaseTools.place`: nothing, and no undo step, where there is no palette; else
    `addMarkAt`, then `MarkLife.placeAs` with `asFor` -- exactly the page's placed mark (`born`, `life`, `lifeAt`,
    `lifeAs`; driven) -- and at Revisions the note "After the estimate — tell the office" (the page's `stage>=3`).
    Nothing else in the app adds a mark (`PhaseToolsTest` reads every app source for it).
  - **A mark of an earlier phase.** HIDDEN, by `PhaseTools.editable` / `lockOk` / `delOk`: the picked row's nudges,
    padlock and delete (the label stays and still opens the sheet); a Line's and a chain's padlock and delete; a
    list row's delete word; the floating trash on both views; the eight resize tabs; the menu's rows (on a surveyed
    mark after Survey: its head and Cancel, as driven); ⋮'s two clears and Start over outside Survey. REFUSED, with
    the phase's own line as a 2.6 s note and no undo entry, should an act still arrive: delete (asked before the
    lock, as `sp.js` asks), nudge, lock, duplicate, Lock all, a clear, Start over. A press on such a mark PICKS it
    and nothing moves; once the finger has moved 4 dp it says why, once per gesture -- the phone's own path for a
    locked mark.
  - **Decided here, where this plan was silent or the page's code and its rule disagree** (each is a prototype
    question, Q-list 11 and 12; none is a new state on the phone):
    1. *A refused drag takes no undo step.* The page snapshots before it asks (`o.before()` then `moveEl`), so a
       drag on a surveyed mark in the Wish list leaves an EMPTY undo step (driven: `hist` 0 -> 1, nothing moved).
       The phone refuses first, as it does for a locked mark.
    2. *No tabs on a read-only mark on the 360 either.* The page hides them on the squared face (`P L5097`) and still
       draws them on the 360, where a press on one is refused. One rule for both views.
    3. *At Estimation a Line's end and a chain's point do not move.* `editable` answers false for them there and
       the page hides their padlock and delete, but its end and node grabs never ask it -- on its 360 they still
       move. The phone holds them and says "F-M1 · Estimation changes nothing — …" (`refuseText`, the page's words).
    4. *No palette on the 360 at Client approved.* The page draws its tools row there (CSS `L1277` hides it at
       Estimation only) and `spPre` bounces every tool to Select (driven). Hidden, like steps 1 and 2 (Q-list 6).
    5. *Lock all stays on the menu of a mark the phase lets be edited, and refuses* -- as the page's does (driven:
       "Clearing and locking every mark is a Survey tool — move the phase back to use it"). Copied as it is; Q-list 11.
    6. *The copy of a wish is the box alone* (Q-list 8, as planned): `DuplicateTest` pins that nothing is stamped
       and that the copy is then held like a surveyed mark in the very phase it was made in (driven: its row is its
       label alone). **SUPERSEDED BY THE L4 REVIEW, finding 1, below:** what `duplicate` makes is unchanged, but the
       phone no longer offers Duplicate on a mark where that copy would be made of a wish.
    7. *`Tour.kt`'s two `missing` notes* no longer say the phone has no band or has never had the sheet; they say no
       tour anchor is on either yet. Nothing else of the tours is touched (L10, P1, TR).
  - **For L10.** Add `LINES` and `READINGS` to `PREP_HONOURS` (and fold `prepSteps` / `prepMode` into `steps` /
    `modeFor`); the reading boxes, the room-size line and Capture 360 are untouched here. **For L7:** the 360
    already has the chip, the placement and the read-only rules; it draws no state yet. **For L6:** a Note's chip has
    no second half and `place` stamps none; its `fresh` sheet is L6's. **For TR:** no tour anchor is on the band or on
    the chip's second half (`phase:band`, `toolchip:as`).
  - **Not ported, and why.** `spPre`'s resets of the Trade chip and the List's state filter (neither is on the
    phone, Q-list 3); `T53.newFields` at placement (a new mark's sub-kind and trade: L5, L6); ⋮'s "Lock all marks /
    Unlock all marks" box (the phone never had it); the reading boxes as figures (L10).

#### The L4 review (11 Nov 2026) — two findings on commit `1efd0f2`: one major, one minor

Each was checked against the committed code, S5's `asis.js` (slice RD, read in the local checkout) and the
prototype's own code before anything was changed; `proto-tables.mjs` was run again on the page for the copy's
row ("a copy of a wish (the box alone): `NLD --- --- --- --- --- NLD`", and the page's menu keeps Duplicate on a
mark born in the phase at stages 1, 2, 4 and 5). **Confirmed: both. Declined: none.** Finding 1 is fixed in code; finding 2 is the plan
owner's to decide between two remedies and is recorded here, in §3.10 and in §7 — no code, as the reviewer asks.

| # | Finding (severity) | Verdict, and what was checked | Fixed how |
|---|---|---|---|
| 1 | A Duplicate made after Survey is an unstamped mark: S5 and the exported sheet treat the copy of a wish as a thing that is in the room (major) | **CONFIRMED, every step.** L4 gated Duplicate by `PhaseTools.editable` (the menu's row and `duplicateMark`), and after Survey `editable` is true ONLY for a mark born in that phase — so every copy of a mark made at stages 1, 2, 4 and 5 was the copy of a thing that is not in the room. `SurveyPrepEdit.duplicate` builds `Mark(newId, t, code, box)` with no extra; `SheetMarks.surveyed` is `born == null \|\| born == exist`, so the exported A4 sheet and the saved "survey marks" pictures draw that copy; S5's `markSurveyed` (`asis.js`, slice RD) is `!ownKey(e,'born') \|\| born === 'exist'`, so a vendor and a client login are given it, against RD's own rule "the room as surveyed, whatever is marked later", and V5 gives it reading boxes on /projects. **The page does the same** (`duplicateItem` `P L5633-5635`, gated by `spEdit`, the copy `{id, t, code, x0, x1, y0, y1, locked: false}`), which is why the builder followed Q-list 8 — but Q-list 8 recorded only the read-only copy, not where it goes. On the prototype there is no S5 allow-list and no surveyed-room sheet for it to slip through; on the phone there are both, and before L4 no mark could be born a wish, so the path opened with this commit. At Client approved it was also a new mark made after approval. **One case the reviewer did not name, found confirming it:** while the stage is UNREAD and the capture's chip is cycled to "as Client wish", a tap is stamped and a copy would not have been — the same leak, and "Duplicate is a Survey tool" (`bulk`) alone would have left it | **A mark is duplicated only where its copy — the box alone — is what a type tool's tap would place.** New `PhaseTools.copyOk(mark, stage, picked)` = `editable` and `asFor(stage, picked) == Existing`: at Survey (and on a project S5 does not list), and while unread as long as the chip reads "as Existing". Not at the Wish list, Vendor review, Revisions or Client approved, nor while unread with the chip cycled on. It is the reviewer's strict remedy ("Duplicate of a mark is a Survey tool until Amit rules") stated by the placement rule instead of by the stage, so the unread case is closed too; the other remedy — stamping the copy with `MarkLife.placeAs` — would have put on the phone a mark the prototype does not make. `SurveyPrepEdit.menuItemsOf` takes the chip's `picked` (no default) and hides the row by `copyOk`; `duplicateMark` asks `copyOk` after `editable`, before the undo step — a press that still arrives makes nothing and says nothing (the prototype has no line for it; no key added). A Tape, a Line, an area and a chain are copied wherever they may be edited, as before (no state; every document draws them as they are); a Note keeps `editable`'s answer (no tap stamps one, no document for others shows one; L6's). What `duplicate` MAKES is unchanged. **Where the phone now differs from the page:** no Duplicate row on a mark born in the phase at stages 1, 2, 4, 5 — Q-list 8, which now carries the consequence. **What stays as the page has it, said so it is not read as closed:** at Survey a copy of a mark born a wish (the stage moved back) is an Existing mark, as every mark placed at Survey is, under either answer to Q-list 8. Tests: `PhaseToolsTest` +2 (the table by stage and chip for four births of mark, a Line, a chain, a Note and an id that is no item; the rule itself — wherever a copy is offered a tap there is unstamped, and the reverse; the menu's rows, only Duplicate moved) and its menu and source tests changed (the one menu call hands the chip; `copyOk` is asked once, before `snapshot()` and before the app's one `SurveyPrepEdit.duplicate(`); `DuplicateTest` +1 and its L4 test now also shows the leak the copy would have been (a sheet for others drawing `FE2`, never `FE1`). Eight one-line mutations of the fix — the menu back to `editable` (L4 as reviewed), `copyOk` as `editable`, as `bulk`, a Note or a Line by the mark's rule, the screen not asking, asking after the undo step, the menu not handed the chip — each turned at least one test of the two classes red |
| 2 | From B2 until L10 (B5) a new 360 captured in a project past Survey is surveyed as wishes, or cannot be marked at all (minor) | **CONFIRMED.** `NewCaptureSurvey` (`MainActivity.kt`) hands a new capture's survey prep its project's stage (`QuoteStageSync.prepStage`, L3's comment: "a new room of a listed project takes that project's"), and since L4 the stage decides placement: every mark of the new room is born a Client wish at the Wish list, Vendor added at Vendor review and Revisions, and there is no 2 · Mark at Estimated or Client approved. No app source names `Tool.CAPTURE` and `PREP_HONOURS` is `{MARK, PALETTE}`: Capture 360 is untouched until L10, as this plan requires. On the prototype the room cannot exist at stages 1–4: `TOOLS.capture` is `[1,0,0,0,0]` (`P L3961`) and CSS `L1362` hides the button there (driven by the plan critic at stages 1 and 3). So between B2 and B5 an engineer who adds a missed room after the stage has moved marks its real doors and windows as wishes — left off the exported sheet, kept from a vendor and a client login, with no reading box on /projects — under a band and a chip that do say "every mark you place is a client wish". §7 named the hidden tools, not this | **No code in L4** (the capture rule is L10's, and the reviewer proposes none). Recorded in §7 (a new risk), in §3.10 (L10 closes it) and here as B2's build note: *"a room added after the stage has moved is marked as that phase marks — its doors and windows would be born wishes. Put the stage back to Survey on /projects before capturing a missed room, and forward again after; a mark already placed as a wish stays one."* At Client approved the stage cannot go back: a 360 taken there is a work photo and its survey prep has no mark tools, as on the prototype. **For the plan owner, before B2 is merged — the other remedy is open:** bring L10's Capture 360 gate (`PhaseTools.can(Tool.CAPTURE, stage)` on the room screen: hidden at stages 1–4, shown at Survey and at Client approved) forward into B2, so the room cannot be made, as on the prototype. It was not done here: it is a change to what B2 hides ("a change he may read as a loss", §3.10) and to an app screen this container cannot compile, and which build carries it is the plan's decision, not a review fix's |

`:pano` after the fix, from the XML, forced fresh: 1,158 tests in 82 suites, none failed, none skipped (1,155
before: `PhaseToolsTest` 32 → 34, `DuplicateTest` 7 → 8). No key added or removed, Tx not regenerated,
`i18nRatchet` lowered nothing, `unkeyed.tsv` untouched and still empty. **The app's half was not compiled** (no
Android SDK here): `SurveyPrepScreen.kt` changes two calls — the menu's `menuItemsOf(…, quoteStage, asPicked)`
and `duplicateMark`'s `copyOk` line — and the L4 `kotlinc` check
(2.0.21 over every app source against the real `:pano` classes and the stubs) reports no new error against L4's
own run, while the same check with either call given the old arguments reports the mismatch at `duplicateMark`
(the menu's call sits inside a composable the stubs do not resolve, so it is read, not checked). CI is the
compiler, and NOTHING HAS BEEN SEEN ON A PHONE. If Amit's answer to Q-list 8 is "a copy is born what the phase
places", `copyOk` goes and `duplicate` stamps; if it is "Duplicate is a Survey tool", `copyOk` is already it but
for the unread chip.

### 3.5 L5 · the ten new types and their sub-kinds, pure (M)
- **Prototype:** the ten `P L2397-2407` (NOTE, GRL, AC, CDP, PLB, CTR, CRP, LV, SAN, DEF — 22 types: wall 21,
  floor 11, ceiling 13; PLB and LV are 50 mm points), SUBS `L2423-2466`, `displayName` `L2478`, the code grammar
  (the whole letter run is the type) `L2534-2543`, `defaults53` `L2551-2565`, `measures53` `L2569-2588`,
  `placeRefusal` `L2590`.
- **Files:** `pano/SurveyPrep.kt` (TYPES `:660-672`, `isPoint` `:700`, defaults, `measures0`, `highest` /
  `claimCode` `:1094-1121` over a letter run), `pano/Pano360.kt` (`placeRefusal` `:235`), `pano/i18n/Masters.kt`.
  **Prep:** `t` is the token (`FCRP1`, `CCDP1`, `FPLB1.cl`); `sub` only when not the first sub-kind.
- **Tests:** `SurveyPrepTest` (each type's default box and measures per surface kind), `KeyGrammarTest` (every new
  reading key passes S5's `KEY_RE` — it already walks TYPES), a code test (`FP1` and `FPLB1` never collide).
- **i18n on:** `type.*` (86 keys, 17 in use today) with `type.sub.*`, `t53.plumbing_point_x`, the 360's refusals.
- **Merged to `main` ONLY with L6.** `SpMoreSheet` (`app/SurveyPrepTools.kt:134`) lists
  `SurveyPrepPalette.forSurface(s).all`, which is every row of `SurveyPrep.TYPES`, in a five-column grid with a
  fallback icon — so L5 on `main` by itself would publish a Play build with ten new tiles (21 on a wall) in
  v52's More grid. L5 stays on `batch-next` until L6 is green; B3 is the two together.

### 3.6 L6 · More ▸ by trade, the kind picker, the Note pin (M) — after T5 is on stage
- **Prototype:** More ▸ as one row per trade, General first (`moreRows` `P L2602`, `renderMore` `L2688-2694`); the
  tool's remembered sub-kind (`L2471-2473`) and KIND chips in the Mark sheet (`L3030`); the ten icons `L2649-2666`
  and the PLB / LV spot symbols `L2669`; the Note: a 20 dp pin, tag "FNOTE1 ✎ first 24 characters…", a lock glyph
  when Office only, the sheet opening on its note field, an empty new Note removed (`L2912-2926`, `L3066-3070`).
- **Files:** `app/SurveyPrepTools.kt` (`SpMoreSheet` `:132`, icons), `app/SurveyPrepDraw.kt`, `app/MarkSheet.kt`
  (KIND; TRADE for a Note — an Other has it since L3), `pano/SurveyPrepPalette.kt`. With no Trade chip the three
  palette slots stay v52's (`paletteMain('all')`, `L2597`). The Note pin and its tag are drawn on the SCREEN
  only: the saved "survey marks" pictures go through `SheetMarks` (L2) and carry no Note.
- **Tests:** `PaletteTest` (the rows per surface kind), `MarkSheetTest` (KIND; a Note needs its words),
  `SheetMarksTest` again with a Note of each audience.
  **i18n on:** `trade.*.short`, `life.what_should_office_vendor`, `life.msg.empty_note_removed`, `life.trade`.
- **Amit can:** his five examples as the prototype does them — Carpentry unit · Wardrobe; Ceiling drop · Full
  false ceiling (POP); Carpentry unit · Other unit plus a note (dining table); the window and the door from L3.

### 3.7 L7 · a wish on the 360 and in 3 · List (M)
- **Prototype:** `outlinePoly` `P L2880` and the same pill on the 360; in List the state dot before the code
  `L2933`, a Note's panel in place of measure rows `L3138`, the first line of a mark's note under its row `L3141`.
- **Files:** `app/SurveyPrep360.kt`; `app/SurveyPrepScreen.kt` `SpTable` `:2837`. **Tests:** the List's row model
  in `:pano`. **i18n on:** the state names again, `common.office_only`.

### 3.8 L8 · a wish on the six drawings (M)
- **Prototype:** `ls` `P L6121` (the outline by state), `sfx` `L6126` (act and ✎), `drawStateMark` `L6135` (the
  pill, V+), numbered Note pins, the legend line `L6460-6465` and the legend with "Office view: on / off"
  `L6502-6515`. With nothing past Existing, no note and no new type, a drawing is v52's to the byte (`L6083-6084`).
- **Files:** `pano/SurveyPlan.kt`, `pano/SurveyExport.kt`, `app/SurveyPlanScreen.kt`, `app/PlanExport.kt` (the
  exported sheet carries the same marks, in English). **i18n on:** `sp.legend_x`, `sp.office_view_*`.
  **L8 takes over the rule B2 ships with** (the L2 review, finding 1): until this slice `SurveyExport.sheet` draws
  the room as surveyed — it hands `SurveyPlan` `SheetMarks.prep(prep, OTHERS)`, so no mark born a wish, no Note
  and no state is on a sheet. L8 replaces that line with what the prototype's drawings carry (the states, the
  legend, Office view) and rewrites `SheetMarksTest`'s three sheet tests in the same commit; it does not just
  delete them. What a sheet for OTHERS may carry is still Q-list 7.
- **Tests:** `SurveyPlanTest`, `SvgExportTest`: a v52 prep gives today's SVG; a wish carries its dash and state;
  an office-only note is drawn only with Office view on.

### 3.9 L9 · the Wish list counts its marks (S–M)
- **Prototype:** one prep per room, the newest 360 that has one (`P L8502-8513`); the service a mark answers
  (`serviceOf` `L2526`, SERVICE_TRADE `L2509`); `rowCount` `L8530`, the count line `L8535`, `uncovered` `L8539`
  and the two amber lines `L8548`, `L8586`; the project's roll-up `L2964-2977` ("Marks: 8 existing · 4 wish").
- **Files:** `pano/WishList.kt` (a pure `WishMarks`), `app/WishListScreen.kt:209-281`, `app/SurveyPrepStore.kt`
  (a project's preps), `app/BrowseScreens.kt:356-399`. The rows stay ticked by hand (GAP Q4). **The two bathroom
  rows count by the approved page's rule, not v53's:** `rowCount` begins `if (WB54.isBundle(svc)) return
  bundleCount(…)` (`L8530`), and `bundleCount` (`L8522-8529`) counts every mark any of the bundle's five parts
  answers (`partsOfMark`, `L4453`, over the PARTS table `L4393` — electrical and painting marks included) in the
  row's APPLIES TO rooms, or, with none chosen, the rooms its name matches (`L2524`). So `WishMarks` ports
  `bundleCount`, `isBundle` and the PARTS table as pure data, and the phone's count line equals the prototype's
  for the same room. The bundle's own control (Parts ▾) and the pack buttons are not drawn — Q-list 3.
- **Tests:** `WishMarksTest` (Dropped and Notes left out; camera → CCTV, intercom → Intercom; a bathroom row
  counting an electrical point and a Damp defect in its bathroom, by APPLIES TO and by its room's name;
  a wish no ticked row covers). **i18n on:** `wish.no_marks_yet_mark`, `wish.x_scope`,
  `wish.x_but_not_list`, `wish.x_not_any_*`, `life.marks`, `life.on_this_phone`.

### 3.10 L10 · lines, readings, the room-size line and Capture 360 by phase (S–M); then P1 and TR
The band and the `mark` / `palette` rows are L4's. L10 is the rest of the phase's hiding.
- **Prototype:** TOOLS rows `lines`, `readings`, `capture` (`P L3948`, `L3954`, `L3961`); `spPre` moving a hidden
  step on (`L4208`); readings refused outside Survey (`L4250-4253`, the figure drawn without its box, CSS
  `L1360`); the room-size line: its pencil hidden and the line taking no tap in phases 1–4 (CSS `L1278-1279`);
  Capture 360 on the room screen (`L8185`, CSS `L1362`, `L1372`).
- **Capture 360 is shown at Survey AND at Client approved, hidden between.** `TOOLS.capture` has five columns
  (`[1,0,0,0,0]`); CSS `L1362` hides the button at phases 1–4 only, and `L1372` shows it again at Client approved
  in execution — "a work-stage photo needs the capture sheet". Driven 11 Nov: hidden at stages 1 and 3, shown at
  5. A cell-by-cell port of five columns would read the sixth as hidden and take away the photos-by-work capture
  that FW2 and K5 shipped — at a stage that can never be moved back. `PhaseTools.capture` is `[1,0,0,0,0,1]`,
  and the sixth cell has its own test.
- **Files:** `app/SurveyPrepScreen.kt`, `pano/StepRoute.kt`, the room screen in `app/MainActivity.kt`. The TOOLS
  rows L10 honours: `lines`, `readings`, `capture`; it also checks `wishlist`, `wishEdit` and `vendorUI` against
  what the shipped Wish list already does (no change expected). **i18n on:** `refuse.readonly.readings`,
  `refuse.phase.*`. **Tests:** `StepRouteTest` by stage, all six plus unread and not-on-S5; `PhaseToolsTest` gains
  the `capture` row's sixth cell and the size-line rule (editable at Survey; not in phases 1–4; at Client
  approved as the prototype's CSS leaves it — part of Q-list 6).
- **Amit sees a change he may read as a loss:** after Survey there is no 1 · Lines, no reading box, and no
  Capture 360 until he moves the phase back — or the client approves, when Capture 360 returns for photos by
  work. The builder drives the served prototype at all six stages first.
- **L10's Capture 360 gate also closes the L4 review's finding 2** (§3.4): from B2 until this slice a new 360
  captured in a project past Survey opens survey prep at that project's stage, so its marks are born wishes (or,
  at Estimated, cannot be placed). Hidden at stages 1–4, the room cannot be made there, as on the prototype.
  Unless the plan owner brings this one gate forward into B2, B2–B4's build notes carry the warning.
- **P1 · prototype item (S): the two tours' texts.** The tours cannot switch on as they are written.
  `wish.5` (`P L8971-8973`) is "The bathroom bundle … Parts ▾ lists them", and the flow's own line says "tick the
  services and the bathroom bundle" (`L8957`): port 62 puts no bundle on the phone, yet the step's anchor
  (`service:row:master-bath`) resolves, so it would run and say something untrue. `phases.1` says "Four phases,
  one strip" (`L8838-8839`; the strip has five on the prototype and the phone alike), and `phases.6` "Vendor
  review turns the client's wishes into trades and vendor packs" (`L8854`). P1 rewrites these on the prototype
  for the phone as port 62 leaves it, and Amit approves the words there.
- **TR (S) — waits for P1.** `pano/Tour.kt` flips `phases` (`:127-129`) and `wish` (`:253-255`) once their anchors
  exist AND P1 is approved, taking the approved words into `android/i18n`. Until then both stay `ships = false`,
  and L10 corrects their `missing` notes: `wish` gains step 5 (the bundle) beside steps 6–13; `phases` names its
  texts. "Help lists three tours" is promised only after P1.

**Q-list for the prototype (raised with Amit, each as a selector; none blocks B2).**
1. The phone's 3 · List is rows, the prototype's chips (`Tour.kt:210`): where the state dot and the note line sit.
2. The phone's 360 is step 4, the prototype's a tab (parked Q3).
3. **The subset, phase by phase — Claude's reading of answer 3, for him to confirm or widen.** Left off the
   phone by this plan: at **Wish list** — "State: … ▾", "Move N ▸" and "⇪ Export" in 3 · List (`P L3951`,
   `L2990-2991`), "Client pack ⇪" on the Wish list's head (`L8571`), and the two bathroom rows as bundles with
   Parts ▾ (`L8578`, `L8592`); at **Vendor review and Revisions** — the Trade chip and its filter, the Vendor
   pack buttons, VENDOR PACKS on the strip; at **Estimation** — GOES TO ESTIMATION and the Scope pack. The
   band's and the strip's own words for Vendor review and Revisions ("trades · vendor packs", "marks and packs
   move", `P L3936-3941`) name tools the phone will then not have. Either he approves the listed subset on the
   prototype, or those phases are redrawn there, or a part comes back in.
4. His own words the prototype lacks (GAP D1): "New" on a born-wish pill, a "close up" act, a structured
   "replace with", a wish that is an area or a length.
5. The `vendor` and `plan` tours on the phone; and P1's texts for `phases` and `wish`.
6. **Survey prep at Client approved** (driven 11 Nov). The prototype has no column for it: the Mark sheet
   falls through to Vendor review's moves, the note stays editable, the room-size line editable, and the step
   chips 1 and 2 show but cannot be entered. The phone, until he rules: no move (the approval lock's words), 3 ·
   List alone, the rest as the prototype leaves it. To decide on the prototype: is a mark's state and note
   locked at approval like the wish list, with change requests the one way to change it?
   **Four more of the same kind, read in the page's code while building L1 (not driven):** (a) the Trade chip
   and "State: … ▾" are DRAWN there (the CSS hides them at other phases only, `P L1275-1276`) while `spPre`
   resets both on every render (`L4212-4216`) — controls that cannot act, like step chips 1 and 2; neither is on
   the phone. (b) A refused edit there says "FW1 was marked in Survey — move the phase back to change it"
   (`refuseText`, `L4245`), which nobody can do after approval; the phone says it as written until he rules.
   (c) `editable` and `delOk` fall through to Vendor review's (`L4232-4237`): a mark born Vendor added can still
   be locked, unlocked and duplicated (item 8 — **not duplicated on the phone since the L4 review**, §3.4); a Line,
   a Tape, a chain and a Note edited or deleted; and Delete
   shows on a mark past Existing, where on the prototype it works (Drop, Restore). **The phone does not draw
   that Delete** (the L1 review, §3.1): with no move offered it could only open the sheet on the approval
   lock's words or be refused with them, so it is hidden as at Estimated — to confirm or redraw on the
   prototype. **Nor, since L2, the padlock of a LOCKED mark in Scope** (`PhaseTools.lockOk`, §3.2): on the
   prototype it shows there and its unlock says "take it Out of scope in its sheet", which the prototype's sheet
   then offers and the phone's does not — the same question, to confirm or redraw with the Delete.
   (d) For L10's size-line rule: the CSS leaves the room-size pencil showing
   there (`L1278` hides it at phases 1–4 only), but `openLWH` returns at once wherever readings are hidden
   (`L6977`), Client approved included — so the pencil is one more control that shows and cannot act, not an
   editable size line.
7. **What a document for others carries once wishes exist**: the phone's saved "survey marks" pictures (L2)
   and what a vendor or a client login sees of a room on /projects (RD). Both are the surveyed room until he
   rules; the prototype's own rule for a document is the pack (`P L3194-3199`, `L3280-3289`).
   **A third document, seen building L2 and NOT covered by either:** the Plan tab's exported A4 sheet
   (`SurveyExport`, drawn by `SurveyPlan`). From B2 until L8 (B4) it draws a mark born a wish like a surveyed one
   (§3.2, "Seen, NOT changed"). Whether that export is the surveyed room in the meantime is the same question.
   **Since the L2 review it is the surveyed room** (`SheetMarks.prep`, §3.2), like the other two, until he rules
   or L8 draws a wish as a wish there; the Plan tab on screen, the office's, still draws every mark.
8. **What a Duplicate is born as** (found building L1, 11 Nov; read in the page's code, not driven). L53's own
   comment says placing "as …" is called by "addEl / addElAt / duplicate" (`P L2957`), and §3.4 L4 above was
   written from it ("a new mark gets `born`, `life`, `lifeAt`, `lifeAs` once, at placement", `DuplicateTest`).
   The page does otherwise: `L53.placed` has two callers, the tap of a type tool on a face and on the 360
   (`P L5865`, `L5926`); `duplicateItem` (`L5633-5640`) builds `{id, t, code, x0, x1, y0, y1, locked: false}`
   and nothing more. So on the prototype the copy of a wish, made in the Wish list phase, is an EXISTING mark
   with no sub-kind, trade, act or note — which that phase then holds read-only, as a survey mark — and a copy
   made at Vendor review or after approval is the same. To decide on the prototype: is a copy born what the
   phase places (keeping its sub-kind and trade), or is Duplicate a Survey tool? Until he rules, L4 does what
   the page DOES, not what its comment says: a copy is the box alone, as `SurveyPrepEdit.duplicate` makes it
   today, offered where `PhaseTools.editable` allows — and `DuplicateTest` pins that no copy is stamped.
   **WHERE THAT COPY GOES, which this item did not say (the L4 review, finding 1, §3.4):** an unstamped mark is
   read as IN THE ROOM by everything that leaves the office — the exported A4 sheet and the saved pictures
   (`SheetMarks.surveyed`), S5's answer to a vendor or a client login (slice RD) and /projects' reading boxes
   (V5). So the copy of a wished wardrobe, made in the Wish list phase, would have gone to a vendor as a wardrobe
   that is there. **Since that review the phone copies a MARK only where the box alone is what a tap would
   place** (`PhaseTools.copyOk`): at Survey, and while the stage is unread with the chip on "as Existing" — the
   strict reading of both answers above, since there either makes a mark born Existing. The row is hidden on a
   mark born in the phase at the Wish list, Vendor review, Revisions and Client approved, where the prototype
   draws it; a Tape, a Line, an area and a chain keep theirs. Two things for him with the question: on the
   prototype the same copy is made (and nothing there sends it out as surveyed); and at Survey a copy of a mark
   born a wish — the stage moved back — is still an Existing mark, on the page and the phone alike.
9. **What the Mark sheet may still change at Estimation** (found building L3, 11 Nov; run on the page's own code).
   The band there says "hand-off · read-only · nothing new is marked" and the sheet offers no move ("… Estimation moves
   nothing"), yet its act chips are drawn and on for a mark born Existing that is past Existing, its note and audience
   are open, and an Other's TRADE can be picked: `apply` has no rule for the stage (`P L3071-3088`), so each is
   written. The phone does the same, as the page DOES. To decide on the prototype: is the sheet read-only at
   Estimation -- and, with Q-list 6, at Client approved, where the phone already draws no act chip?
   **Since the L3 review the phone also WRITES no act at Client approved** (§3.3): the note, its audience and an
   Other's trade are still written there, provisionally. **And one more of the kind, at Survey** (run on the page's
   code in that review): the page draws its gate's line and no chip there, yet any Done on a mark past Existing
   whose `act` is absent or not one of the four writes `act: replace` -- with nothing touched, and beside a note.
   Reachable only with the stage moved back to Survey, or for a file another writer made; the phone does the
   same, as the page DOES. Is that write meant at Survey?
10. **The Mark sheet with the keyboard up** -- a state a page never has (L3). On the phone the sheet rises over the
    keyboard and, when its blocks then need more height than is left, they scroll between the head and Done. Shown to
    him on the phone with B2, not drawn on the prototype; listed so it is his to change.
11. **"Lock all <type>" on the menu outside Survey** (driven building L4, 11 Nov). The long-press menu of a mark
    the phase lets be edited -- one just placed in the Wish list, a Line in any phase but Estimation -- still
    offers Lock all, and the tap is refused: "Clearing and locking every mark is a Survey tool — move the phase
    back to use it" (`P L4263` hides the row only with Duplicate and Lock; `lockAll` asks `spBulk`, `L5055`). The
    phone does the same, as the page DOES. Is the row meant to be there?
12. **Four things the page's code does in survey prep that its own phase rule does not say** (driven and read
    building L4; the phone does NOT copy them, §3.4 "L4 AS BUILT"; each is for the prototype to settle):
    (a) a drag on a read-only mark takes an undo step before it is refused, so ↶ lights up and undoes nothing;
    (b) on the 360 a read-only picked mark still shows its eight resize tabs, and at Estimation a Line's end and a
    chain's point can still be dragged there (the end and node grabs do not ask `spEdit`); (c) the chip's "· as
    Client wish" keeps the cycling button's 32 px line although it is plain words, so the chip is 55-72 px tall;
    (d) at Client approved the 360 still draws its palette row, every tool of which bounces back to Select -- one
    more control that shows and cannot act (item 6).

### 3.11 RD · S5 gives a vendor or client the room as surveyed (S–M) — on stage BEFORE B2
The room GET answers the stored record whole to every caller the access list admits (`asis.js:1472-1476`,
`json({ ...d, … })`); it is the only outlet of the whole record (`captureSummary` only counts marks), and the
prototype's own note names the fix as a precondition (`P L1921`).

**The rule, decided here** (the first version kept `life`, `born` and `act` for everyone and removed only whys
and office notes; that still showed a client or vendor a Dropped mark, and one vendor another vendor's answer —
which no pack of the prototype does: no pack's states include `drop`, the client's Wish list pack is
`exist`, `wish`, a vendor's `exist`, `wish`, `vrev`, `vadd`, `P L3194-3199`, `L3280-3287`). S5 cannot apply the
pack rule yet: it does not know a vendor's trade, and /projects is not a pack. So until Amit rules (Q-list 7),
**a caller who is not staff gets the room AS SURVEYED — what that login sees today, whatever is marked later:**
- a mark born a wish or Vendor added (`born` is anything but absent / `exist`) is left out: it is not in the room;
- a Note (`t: 'NOTE'`) is left out, whatever its audience;
- every other mark is given WITHOUT `life`, `lifeAt`, `lifeAs`, `act`, `why`, `note`, `vis` — so a surveyed
  window the client wants replaced, or a wish since Dropped, reads as the window that is there;
- the readings in the answer are filtered to the codes that remain; `captures[]` counts the marks that remain.
It is the same rule as the phone's saved pictures (L2, `SheetMarks`): outside the office, one answer.
**It is deliberately STRICTER than the prototype's own S5 note** (`P L1921`: remove, for vendor and client
callers, every office-only Note and the `note` and `why` of a mark set office-only — which would let them read a
"Client & vendors" note on /projects). That note was written for one vendor round; with the wish list going to
SEVERAL vendors (Amit, 11 Nov), a "Client & vendors" note or a vendor-side state on /projects would reach every
vendor on the access list, whatever their trade. The strict rule ships first because it cannot leak; opening it
is his decision.

**An allow-list at EVERY level, not only inside `els`.** One exported `redactRecord(d, staff)` at that GET — both
addresses, the room and `/c/:capture`. Staff get the record byte for byte. Anyone else gets a copy built from
named keys only, so a key a later phone build adds — at any level — is office-only until S5 names it:

| Level | Keys a non-staff caller receives |
|---|---|
| the record | `v project project_title customer room capture captured_at app_version L W H prep surfaces work` — not `work_by`, `work_at` (staff logins, written by `setCaptureWork`, `asis.js:1707`), not `erp_capture`, `stage` |
| `prep` | `v L W H w` (not `actual`: the page reads the readings table) |
| `prep.w[surface]` | `bottom top left right set els meas chains`, and `ref`, `lock` as the phone writes them (`SurveyRef.KEY`, `LINES_LOCK`: geometry and a flag, no words) |
| `surfaces[surface]` | `a b image` |
| a mark (`els`) | `id t code x0 x1 y0 y1 locked sub` |
| a Tape, Line or area (`meas`) | `id code kind locked pts` (not `cat`, `note`, `vis`) |
| a chain | `id code axis locked nodes snaps` |

The keys the handler itself adds (`updated_by`, `captures[]`, `images`, the stage) are outside the stored record
and unchanged; `updated_by` is the uploader's staff login and goes to every admitted caller today — named here,
not widened, not narrowed by this slice.
**Tests:** staff deep-equal to what was stored; a vendor and a client, at both addresses, lose exactly the
things above — one test per level with an UNKNOWN key at that level (record, prep, surface, `surfaces`, mark,
meas, chain), a born-wish mark, a Note of each audience, a Dropped mark born Existing (kept, as surveyed), a
`why`, an office-only note on a mark and on a Line; a record with only known keys answers as today; a malformed
`els`, `meas` or `w` never throws; the readings of a mark left out are not in the answer.
**For Amit (Q-list 7, an S5 QUOTE question):** what a client login and a vendor login see of the wishes on
/projects — the surveyed room only (this slice), or the pack's rule per caller, which needs §6's records to
know a vendor's trade.

### 3.12 T5 · S5 learns the types and trades (M) — on stage BEFORE B3
`app/src/asis_view.js`: the ten rows in TYPES (`:47`) per surface kind, the sub-kind names, the point set grown
to PLB and LV (`:69`), the `measures0` branches (a Note has none), and TRADES / `tradeOf` / SEES / SERVICE_TRADE
exported — held equal to the phone's tables by `tests/asis-view.test.mjs`, as TYPES and the quote stages are.
`KEY_RE` is unchanged. Until it ships, a new type shows on /projects as a grey code with the wrong checklist.

### 3.13 V5 · /projects shows staff a mark's state, act and note (M) — on stage BEFORE B2
Moved ahead of B2: `surfaceView` (`asis_view.js:393-403`) reads only a mark's code, type and box, so from the
first wish until this ships /projects would draw a mark born a wish exactly like a surveyed one, measure
checklist and all. It needs RD only (the type names are T5's, separately).
`surfaceView` carries `life`, `born`, `act`, `sub`, `cat`, `note`, `vis` from the record — which only staff
receive (RD); `projects.js` `surfaceHtml` (`:236`) draws each box in its state's outline with the pill, the act
and ✎, a Note as a pin with its words, the list head as "FW1 · Window · Client wish · ↻ Replace" with the note
under it (tagged "Office only" where it is), a mark not in the room (born a wish or Vendor added) with no
reading box, and the project page a "Marks: …" line from a per-state count added to `captureSummary`
(`asis.js:469`) for staff. A vendor or client sees the surveyed room, unchanged. Display only; nothing on the
web edits a mark.

## 4. The room's plan on the phone — DECIDED 11 Nov: it opens from the room screen

**What 0.3.318 already has.** Inside one capture's survey prep, the eighth tab, PLAN (`pano/StepRoute.kt:65`):
six drawings of that room — Floor, reflected Ceiling, Front, Right, Back, Left elevations — one at a time by
chips, with pinch, pan and fit (`app/SurveyPlanScreen.kt`, `pano/SurveyPlan.kt:71-75`), and a 1:50 A4 export of
the drawing on screen as SVG, PDF or Share. It is reached room → photo → survey prep → PLAN. So "inside that
room only" is already true; what his words add is "a cleaner look to verify quickly" — or he did not find the tab.

| | Candidate | Size | Note |
|---|---|---|---|
| a | **A way in, nothing new drawn:** the room screen opens its current capture's PLAN directly | S | if the six drawings are right and only hidden |
| b | **One verification sheet per room:** the floor plan with its four elevations folded out round it (the cube net S5's room page uses), as line drawings on one zoomable page | M | `SurveyPlan` has the six renderers and `FacePrep.EDGE_NAMES` says which edge meets which; "floor and wall elevations … verify quickly" read literally |
| c | The room's floor plan as the picture on its tile | S–M | recognisable at a glance; verifies little |
| d | The six drawings as they are, with the wishes on them (slice L8) | in §3 | if "verify" means checking the wishes on clean drawings |

**DECIDED (Amit, 11 Nov 2026, on a selector, after this section was first written): "Found it, but it should be on
the room"** — the PLAN tab works; the floor plan and elevations must open from the ROOM screen itself, one tap, not
hidden inside a capture. So the choice is candidate (a): a way in, nothing new drawn — the room screen opens the six
drawings of the room's current capture (the last one with a survey prep). It is an S2 UI change, so it is drawn on the
prototype first as item **P2** (the room screen's plan entry, and what it shows for a room with no prep yet) and rides
the first build after his approval there. Candidates (b)–(d) stay recorded, not chosen. Fixed either way: no arranger on the phone (answer 4; the audit's
F1 and F2 move to S5), and the phone's whole-flat view is §5's, read-only, later.

### 4.1 RP · S5 web: a room's six line drawings on its room page (M) — the S5 half of answer 4
Amit: "floor and wall elevations are required so that it will be a cleaner look to verify quickly … each room
floor plan should be visible inside that room only." S5 has no plan or elevation renderer today: the room page
draws the six squared PHOTOS as a cube net (`app/src/asis_view.js:27`), and nothing else. The first version of
this plan named the drawings only inside AR's and RQ's outlines, so both stood on something no slice built.
- **What it is.** On /projects' room page, under the photos' net, the same six as LINE drawings — Floor plan,
  Reflected ceiling plan, Front, Right, Back and Left elevations — in the same net (floor in the middle, each
  wall on its edge), display only, for the capture on screen. Inside that room only; nothing on the project page.
- **The source.** The prototype's six drawings are already JavaScript (`P L6284-6429`, the v52 drawings; the
  v53 states and legend `L6075-6135`, `L6430-6521` for staff, who alone receive them — RD). They move to a new
  `app/src/asis_plan.js`, pure (record in, SVG out).
- **The mapping is pinned, not re-derived.** `pano/SurveyPlan.kt:29-38` states it and binds S5 by name
  ("CONSTRAINT FOR S5 QUOTE … or the phone and the web will show one room as two mirror images"): `planPoint`
  (front x → (x, 0); right x → (W, x); back x → (W − x, L); left x → (0, L − x)), `planY` = L − y for the floor
  AND the ceiling (the ceiling plan reflected, lying over the floor plan), `elevY` = B − y; front at the top,
  the walls clockwise. `tests/asis-plan.test.mjs` holds the three functions equal to the phone's on the same
  fixture points, and checks a door on the right wall draws on the right.
- **Needs** RD (it draws the record the caller is given, so a vendor or client gets the surveyed room's
  drawings) and T5 (the new types' symbols). **AR and RQ both depend on it**: AR's pieces and RQ's pages are
  these drawings.
- **Is this what "visible inside that room" means?** It is the S5 reading of his sentence. Whether he also wants
  it on the PHONE's room screen is §4's pending choice; the two are asked together (PL).

## 5. S5 web — the arranger (outline only; its own design before any build)

- **After RP** (§4.1), which brings `app/src/asis_plan.js` and the pinned mapping.
- **Geometry.** The prototype's `FP54` is pure and already JavaScript (`P L4526-4612`: `rectOf`, `toWorld`,
  `rotate`, `snap` at 150 mm, `overlapping`, `fit`, `zoomAt`, `hit`, `startRow`, `appendRow`, `openings`,
  `inView`). It joins `app/src/asis_plan.js`, with the prototype's own checks added to `tests/asis-plan.test.mjs`.
- **Pieces.** One per surveyed room: its CURRENT capture's W × L with its doors, windows and openings, drawn by
  RP's floor-plan renderer — the phone's mapping, already pinned there, so the flat cannot come out mirrored.
- **The page.** A "Floor plan" part of the project page, staff editing with a mouse: drag, edge snap, click then
  ↻ 90°, wheel zoom, Fit, Reset with an inline confirm, overlaps in amber, the rooms not surveyed listed under it
  — the prototype's page (`P L4615-4789`) with a pointer for a finger.
- **Where the layout is stored.** One document per project on S5 (the prototype's changelog says sharing a layout
  needs exactly this, `P L1882`): `layout` (JSON `{room: {x, y, rot}}`), `layout_at`, `layout_by` on
  `asis_projects`; `GET | PUT /api/asis/:project/layout`, the PUT staff-only, the whole document with a REQUIRED
  `base` (the wish list's idiom), 409 when stale. Keyed by ROOM, not capture, so a re-shoot keeps its place.
- **"The whole project floor plan should be visible once its done."** The project page draws the saved layout
  read-only for everyone it admits, and offers it as one SVG / PDF sheet (the audit's F7, which nothing draws
  today). By the one-state rule "done" is DERIVED — every surveyed room placed, none overlapping — not a button.
- **What the phone shows, later:** the project screen's Floor plan tile opening a read-only view of that layout.
  The prototype draws an arranger there, so a read-only page is a prototype item first. **Size:** L. **Open:**
  whether clients and vendors see the whole plan; whether a wished door or window is drawn on a piece.

## 6. S5 web — RFQ (outline only; several slices, after its own design)

- **The document: one per trade, "plain 2d diagrams".** The model is the prototype's Vendor review pack with
  Drawings and List and no photo faces (`P L3194-3199` the packs, `L3278-3289` the ONE rule of what a pack draws,
  `L3331-3361` the page plan): a cover (client INITIALS only, the trade, an RFQ number, issue and reply-by dates,
  the vendor it is addressed to); per room the Floor plan always, as key plan, plus each drawing that holds
  something of the trade; that trade's marks in colour with figures, what it needs to see as grey outlines with
  codes, every other trade hidden; then the measured list with quantities per type. Never on it: office-only
  notes, a why, the Vendor field, the client's full name, any cost figure. The code that draws it is already
  JavaScript in the prototype — `classify` and `plan` (`L3280`, `L3331`), the totals (`L6444`) — and moves to S5
  beside RP's `asis_plan.js`, which already holds the six drawings (§4.1: RQ cannot start before RP), since
  answer 3 makes S5 the only place an RFQ is made. Pages are SVG, printed to PDF by the browser; the whole-flat
  key plan joins the cover once §5 exists.
- **Lines of service.** The 13 trades already carry Amit's six: painting → Painting & finishes; electric →
  Electrical (AC and CCTV & data beside it); civil → Civil & structure (Tiling beside it); furniture → Carpentry &
  furniture; doors and windows → Doors & windows; POP → False ceiling. Which list rules is question 1.
- **Where an RFQ and a quote are KEPT is not decided — question 7.** The audit offered three homes (GAP §5 Q6:
  ERPNext's RFQ and Supplier Quotation; the same with a vendor page; "kept on S5 only for now, ERP later") and
  called the doctypes "the candidate backend, named and not designed" (GAP §4.2 D3). Amit's four answers do not
  choose. The next three bullets describe the RECOMMENDED home, ERPNext's standard Buying doctypes; under option
  (c) the snapshot, the vendors addressed and the quotes are D1 rows on S5 and nothing below touches ERP yet.
- **One trade to several vendors.** An RFQ tab on the project (staff): pick the trade, the Suppliers (read from
  ERP), a reply-by date. S5 freezes a SNAPSHOT of that trade's wish marks and quantities under a revision number
  in D1 (never an amount) and creates ONE ERPNext Request for Quotation naming those Suppliers.
- **Each quote, and comparing.** A vendor's reply becomes an ERPNext Supplier Quotation against that RFQ.
  **Amounts then live in ERP only**: S5 keeps the document names and reads the figures live, for staff; a comparison
  page lays the vendors side by side per line. A vendor's prices are cost data — never in a repository, a test or
  the prototype (invented figures only), never in client-facing output; the word "margin" is not used.
- **Revision rounds.** A changed wish set is a new snapshot and a new RFQ to the same vendors; earlier quotes stay
  under the earlier revision. Sent / quoted / awarded is DERIVED from these records (one state, one place), not a
  third hand-set stage beside the quote stage and the mark's state.
- **ERP stays vanilla — and this is an AMENDMENT to the approved integration note, not something it already
  covers.** `s5-s6-integration.md` §1 item 4 approves S5 creating "Lead, Quotation, Sales Order, Project"; Request
  for Quotation and Supplier Quotation are not in that list, and its credential rule (§3.4) is "single-doctype
  WRITE pairs like the existing Lead token". So the recommended home needs that note amended first (two more
  doctypes), and the rule kept: ONE narrowly-scoped pair per doctype — an RFQ writer and a Supplier Quotation
  writer, each reading Supplier and Item — not one user writing both. `mcft_erpnext` seeds them, and one
  non-stock service Item per trade (its own Item Group, its units), as config-as-code reachable from BOTH
  `after_install` and `after_migrate`, checked by `verify_setup`, proven on `mcft-erp-dev` first.
- **What could be verified from disk.** (1) `Supplier Quotation` is named by shipped S1 code that runs on mcft-stg:
  a permission probe (`mallet_estimator/integration.py:626`) and a count by its `supplier` field
  (`patches/stock_vendor_model.py:72`, guarded by `frappe.db.exists`) — weak evidence that the doctype and that
  field exist. (2) `Request for Quotation` appears only in prose (`mcft-erpnext-context` `estimator/DESIGN.md:20`:
  "RFQ → Supplier Quotation auto-creates Item Prices"), never exercised. (3) No frappe or erpnext source is on
  disk. (4) `mcft_erpnext/generic.py` seeds supplier groups, suppliers, item groups and UOMs; no item seeder yet.
- **CONFIRM ON THE LIVE SITE** (dev first, each absence read behind a control query): both doctypes and the Buying
  module on production's app set; the RFQ's child tables and mandatory fields; that a non-stock service Item is
  accepted on its rows; create and submit over plain REST by a narrow user, and whether submit emails suppliers;
  how a Supplier Quotation is tied to its RFQ over REST; the comparison report's name on v16; whether a Supplier
  Quotation writes Item Prices (it must never touch "Estimation (Assumed)"); India Compliance's mandatory fields.

**Open questions for Amit (seven; each becomes a selector, recommended option first).**
1. **S5 QUOTE** — the lines of service an RFQ goes out by: the 13 trades (recommended; his six all fit), his own
   short list with the rest folded in, or S5's twelve wish-list services.
2. **S5 QUOTE + S6 ERP** — who enters a vendor's quote: staff, from the vendor's reply (recommended first), or
   the vendor on a page of his own.
3. **S5 QUOTE** — what a vendor quotes: a rate per line against S5's quantity (recommended; it can be compared),
   one lumpsum per trade, or either.
4. **S5 QUOTE** — how the document reaches a vendor: staff share the PDF by hand (recommended), or ERPNext emails it.
5. **S5 QUOTE + S2 APK** — the per-mark vendor answers (Vendor review / approved / added) hold ONE vendor's word:
   keep them as the summary staff set after comparing (recommended), or make them S5's, derived from the quotes.
6. **S5 QUOTE + S6 ERP** — the award: record the accepted Supplier Quotation per trade and stop there for now
   (recommended), or carry it on to a Purchase Order and into the estimate.
7. **S5 QUOTE + S6 ERP** — where an RFQ and each vendor's quote are kept: ERPNext's standard Request for
   Quotation and Supplier Quotation, S5 holding only the document names (recommended: the comparison report and
   the later purchase flow come with it, and no amount is stored on S5 — but it amends the integration note of
   10 Nov, above); or on S5 only for now, ERP later (GAP Q6's option c: sooner, but amounts — cost data — would
   then sit in D1 and in its nightly backup).

## 7. Risks, and what is deliberately not in port 62

**Risks**
- **An older build opens a prep that holds wishes.** 0.3.318 keeps every key (verified) but its Delete removes a
  wish outright, its clears and Start over do not spare one, and it draws new types grey. Today only Amit's phone
  holds captures and Play updates it; a second phone must be on B2 or later before it opens such a room.
- **The record PUT replaces the whole record**, so two phones editing one capture's marks is last-write-wins, as
  today — but what can be lost is now a client's wish, not only a box.
- **L4 and L10 hide tools he uses today** — B2: the mark tools at Estimated and after approval; B5: Lines,
  readings and Capture 360 between Survey and approval. It is the approved prototype, the band says the phase,
  and each build's note says so plainly, so it does not read as a fault.
- **From B2 until L8 (B4) the Plan tab and its export differ.** On screen the six drawings draw a mark born a
  wish like a surveyed one (a wished door is a gap in the wall, with no pill); the exported sheet leaves every
  such mark out (the L2 review, finding 1). The safe half of a state the prototype does not draw — its drawings
  show wishes as wishes, which is L8 — and B2's build note says so plainly, so a wish missing from an exported
  drawing does not read as a fault.
- **From B2 until L10 (B5) a room captured after the stage has moved is marked as that phase marks** (the L4
  review, finding 2, §3.4). Capture 360 stays on the room screen in every phase until L10, and a new capture's
  survey prep takes its project's stage: at Client wish list every mark placed in the new room is born a Client
  wish, at Vendor quoted and Revisions Vendor added, and at Estimated there is no 2 · Mark. On the prototype that
  room cannot be made (Capture 360 is hidden at phases 1–4). Real doors and windows marked so are left off the
  exported sheet, kept from a vendor and a client login, and get no reading box on /projects; the band and the
  chip say what is happening, and nothing turns such a mark into a surveyed one afterwards. Until L10 — or until
  its Capture 360 gate is brought forward — each build's note says: put the stage back to Survey on /projects
  before capturing a missed room, and forward again after.
- **Duplicate of a mark is a Survey tool on the phone** — and an unread stage's while its chip reads "as
  Existing" (the L4 review, finding 1; Q-list 8): the prototype also offers it on a mark born in the Wish list,
  Vendor review or Revisions, where the copy — unstamped — would leave the office as a thing that is in the room.
  He may read the missing row as a loss; B2's note says so.
- **Client approved is a state the prototype's survey prep does not design** (Q-list 6). The phone's behaviour
  there is provisional and the strict one: no move, no mark tools.
- **A vendor or client login on /projects sees no wish at all** until Amit answers Q-list 7 — stricter than the
  prototype's own S5 note. Chosen because S5 cannot yet tell one vendor's trade from another's.
- **A phone with a live ERP pair** re-reads the bench's site rows on every bootstrap; A2's read-time rule keeps
  the renamed site as one row, but the bench itself still holds the old name (R8).
- **The vendor-side states are a one-vendor model**, ported because the table is one piece (§6, question 5).
- **The bench's site name is no longer changed** (R8's left half); `rename_node` for a site has no caller after
  A2, and its removal belongs to the ERP-leg retirement, not here.
- **Prototype first.** A2 waits for P0, TR for P1. B2 does not wait for the Q-list, but a screen difference found
  while building stops at the prototype, not on the phone.
- **The line references are to a built page that is in no repository** (the rule of 3 Nov 2026): the published
  prototype is the source, and each slice re-reads its lines before it changes anything.

**Deliberately not in port 62**
- On the phone — Claude's reading of answer 3, put to Amit phase by phase as Q-list 3, not his decision: the
  Trade chip and its filter, State ▾ and Move N ▸, the Export sheet and the five packs (the Client pack among
  them), VENDOR PACKS, GOES TO ESTIMATION, the bathroom bundle's Parts ▾ (its COUNT rule is ported, L9). And,
  by his answers 3 and 4 or by earlier decisions: the 360 as a tab, the arranger, vendor and client sign-in
  (K6), an in-app camera, any amount or vendor quote.
- The four things Amit's words ask for that the prototype does not draw (Q-list 4): prototype work first. The
  twelve-row wish list's future (GAP Q4): it ships as the prototype has it.
- R10–R12 and R9 for a project or client rename (slice A3), AS2, the 19 parked questions, a web rename control,
  any web editing of a mark, anything of the wish round for a vendor or client login on S5 (Q-list 7; per-trade
  visibility needs §6's records).
- Any custom ERP code, any S1 change, any change to the legacy ERP leg.

## The port 62 plan critic (11 Nov 2026) — fourteen findings on commit `7f3568c`

Each finding was checked against the prototype page and the code before anything was changed. The prototype was
also DRIVEN for findings 1, 6 and 12 (the served page at 360 × 780, the Demo project, its stage set to Client
wish list, Estimated and Client approved in turn; the driver is in the session scratchpad, not in a repository,
and can be rewritten from this paragraph): at stage 1 survey prep shows steps 2 and 3, the palette, the band
"2 · Wish list · every mark you place is a client wish", FW1 offered → Client wish, Capture 360 hidden; at stage
3 step 3 alone, no tools row, "Existing · Estimation moves nothing — …", Capture 360 hidden; at stage 5 steps 1,
2 and 3 visible with the mode held on List, no tools row, the band "Client approved · Execution · …", a wish
offered → Vendor review and → Dropped, a Client approved mark → Scope, ← Client wish and → Dropped, and Capture
360 SHOWN.

**Confirmed: all fourteen. Declined whole: none. Declined in part: one detail of finding 5 and the proposed fix
of finding 12** (both noted in their rows).

| # | Finding (severity) | Verdict, and what was checked | Fixed where |
|---|---|---|---|
| 1 | B2–B4 let a wish be born at Estimated and at Client approved (blocker) | **CONFIRMED.** `TOOLS.mark` and `palette` are `[1,1,1,0,1]` with no sixth column, so `can()` reads Client approved as hidden (`P L3947-3961`, `L3979`); `AS_FIXED[3]` is null and `[5]` undefined, so `asFor` falls to 'wish' (`L2857`); the first plan put the gate in L10, three builds after L4; `moveOk` has no rule for stage 5 (`L3985-3988`). Driven as above | §1 (B2 = L1–L4 together); §3 head (six stages); §3.3 L3 (the sheet stage by stage, Client approved provisional); §3.4 L4 rewritten (gate, band and placement together); §3.10 L10 reduced; Q-list 6 |
| 2 | A2 leaves the old site name as an empty site (major) | **CONFIRMED.** `app/Catalogue.kt:662-672` makes a group for every bootstrap `sites` row a client owns; `S5Masters.fetch` keeps that array ("MERGED, NOT REPLACED"); A2's one step did not touch it | §2 A2: the row is re-named in the rename step, a read-time rule for a later bootstrap, the listing rule moved to `:pano` (`SiteRows.listed`) with three tests |
| 3 | The saved "survey marks" pictures would carry office-only note text (major) | **CONFIRMED.** `app/FaceWriter.kt:380-394` calls `SurveyPrepDraw.drawMarks(…, Tx.EN, …)` and saves to the gallery; the first plan never named the file | §3.2 L2 (`SheetMarks`, an audience on `drawMarks`, `SheetMarksTest`); §3.6 L6; Q-list 7 |
| 4 | RD does not close the leak it names; the allow-list covers mark keys only (major) | **CONFIRMED.** No pack's states include `drop`; the client's and a vendor's packs are narrower than what RD kept (`P L3194-3199`, `L3280-3287`); `asis.js:1472-1476` spreads the whole record, and `work_by` / `work_at` are in it (`:1707`). **The fix goes further than the critic's "simplest":** non-staff get the room as surveyed, because any per-caller pack rule needs a vendor's trade, which S5 does not hold; this is stricter than the prototype's own S5 note (`P L1921`) and is said so | §3.11 RD rewritten (the rule; the allow-list at seven levels; a test per level); §3.13 V5 (staff only); Q-list 7; §7 |
| 5 | TR switches on two tours whose texts describe things port 62 does not ship (major) | **CONFIRMED**, with one detail corrected: "Four phases, one strip" is stale on the PROTOTYPE too — its own strip has five (`P L3931-3941`) — so it is not a phone-only difference. `wish.5` (`L8971-8973`) and `phases.6` (`L8854`) as the critic says; `pano/Tour.kt:253-255` names steps 6–13 only | new prototype item P1; TR waits for it; L10 corrects the `missing` notes (§3.10); the slice table |
| 6 | L10 would hide Capture 360 after approval (major) | **CONFIRMED.** `TOOLS.capture` `[1,0,0,0,0]`; CSS `P L1362` hides at phases 1–4, `L1372` shows at Client approved in execution; driven: shown at stage 5. The room-size line rule (`L1278-1279`) was also missing | §3.1 (six stages); §3.10 L10 (`[1,0,0,0,0,1]`, its own test, the size line) |
| 7 | The exclusions are recorded as following from answer 3; L9 counts bathroom rows by a rule the page no longer uses (major) | **CONFIRMED.** Answer 3 is about "this RFQ stuff"; `stateFilter` is `[0,1,1,1,1]` and List draws State ▾ and ⇪ Export at Wish list (`P L2990-2991`); "Client pack ⇪" is on the Wish list head whenever it is not read-only (`L8571`); `rowCount` starts with the bundle's count (`L8530`, `L8522-8529`) | §0 table; §3 intro; Q-list 3 widened to every phase; §3.9 L9 ports `bundleCount` and the PARTS table; §7 |
| 8 | Answer 4's per-room plan and elevations on S5 have no slice (major) | **CONFIRMED.** S5 has no plan renderer (`asis_view.js:27` is the photo net); the drawings appeared only inside AR's and RQ's outlines | new slice RP (§4.1); AR and RQ depend on it (§1, §5, §6) |
| 9 | R9 is filed as "none on the site path" (minor) | **CONFIRMED.** `app/S5Session.kt:252-276`: the key is SHA-256 of client|site|title and `keyFor` falls back to it; a site rename changes exactly the site part | §2 A2 (`unlisted` → the list read first, else `Blocked`; two test cases; P0 item d); the defects paragraph; A3 keeps the project and client halves |
| 10 | A1 misstates S5's customer rule; A2 carries one base for several projects (minor) | **CONFIRMED, both.** `asis.js:1924`: `customer = CASE WHEN ?3 <> '' THEN ?3 ELSE customer END`, "an upload that does name one sets it again"; the phone groups by `Catalogue`'s normalised key while A1's guard compares exactly | §2 A1 (stated as a NEW rule, its own test); §2 A2 (a base per project, a test) |
| 11 | What B2 contains is stated two ways; TRADE for an Other is missing from L3 (minor) | **CONFIRMED.** §1's paragraph said "as soon as L3 is green" while the table put L4 in B2; `takesCat` is `e.t==='X'` or a Note (`P L3027`) | §1 (B2 = L1 + L2 + L3 + L4, merged together); §3.3 L3 (TRADE on an Other, its test) |
| 12 | A project not on S5 is mapped to the "unread" rule (minor) | **CONFIRMED that the plan was wrong; the proposed fix DECLINED in part.** No prototype question is needed: the prototype states the rule — "The tools keep following the project's own phase, Survey for a project made on the phone" (`P L4091-4093`) — so the page does have a survey-prep state for a never-sent project, and it is Survey's. The phone uses Survey's gate, as the critic's interim suggestion also said | §3 head ("The stage survey prep is given"); §3.3 L3; §3.4 L4's table |
| 13 | The RFQ's home in ERPNext is written as settled (minor) | **CONFIRMED.** GAP §5 Q6 offered three homes and §4.2 D3 calls the doctypes a candidate; `s5-s6-integration.md` §1 item 4 lists Lead, Quotation, Sales Order, Project, and §3.4 speaks of single-doctype write pairs | §0 table; §6 (recommended, not decided; question 7; one pair per doctype; an amendment to the integration note) |
| 14 | V5 comes four builds after wishes exist; L5 is "nothing on screen" only if never merged alone (minor) | **CONFIRMED, both.** `asis_view.js:393-403` reads code, type and box only; `SpMoreSheet` (`app/SurveyPrepTools.kt:134`) lists every row of `SurveyPrep.TYPES`, and every merge to `main` publishes | §1 (V5 on stage before B2, needing RD only; L5 merged only with L6); §3.5; §3.13 |

**What the critic pass changed in the plan's shape:** two slices added (P1, RP), none removed — 23 rows; V5 moved
ahead of B2; the phase gate and the band moved from L10 into L4; B2 and B3 named as merges of several slices;
RD's rule made strict; two questions added to the prototype's Q-list (6 and 7) and one to §6 (7).
