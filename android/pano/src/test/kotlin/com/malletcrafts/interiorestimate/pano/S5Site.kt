package com.malletcrafts.interiorestimate.pano

/**
 * S5 QUOTE'S OWN ANSWERS TO THE SITE-NAME ROUTE, verbatim (port 62 slice A2, 11 Nov 2026). Each was produced by S5 as
 * committed with its slice A1 (mcft-interior-estimate 69554b5 and its review 3a1c490; run at 6da2c80, not yet on stage
 * when this was captured): its router and its REAL SQL (every migration through 0011, on SQLite, as S5's own `(SQL)`
 * tests run it), a fake ERP answering whoami, and every write sent in the shape [Renames.siteRequest] builds
 * (`PUT /api/asis/:project/site {site, base}`).
 *
 * The scenario: PROJ-DEMO and PROJ-TWO uploaded with no site (the list as an S5 with slice A1 gives it before any name);
 * PROJ-DEMO named with base '' (the seed), the same PUT again (the retry after a lost answer), renamed with the name last
 * seen, renamed again by another staff login, then a PUT still carrying the older base (S5's 409, "Renamed by … meanwhile");
 * a base sent for PROJ-TWO, which S5 holds no name for; the 409 S5 gives when the stored name is the base after all (its
 * UPDATE made to change nothing once, as S5's own test of that branch does); a vendor, a client, a project never uploaded,
 * a name over 140 characters, no base, no sign-in, a key ERP refuses; then PROJ-UP uploaded with `site` in its record,
 * uploaded again with another, and PROJ-DEMO uploaded again naming its first name (an upload never renames); and the list.
 *
 * Which SENT_ bodies the phone builds: every one but SENT_BAD_BASE (the phone always sends a base) -- RenameRouteTest
 * builds the others with [Renames.siteRequest] and compares them byte for byte. SENT_BAD_NAME is built the same way; the
 * phone does not hold a name to S5's 140 characters, so S5's own words answer one that is too long.
 *
 * Every name is invented: mallet_estimator is public. The harness that drove S5 is android/docs/port62/a2/harness.mjs:
 * when S5 changes an answer, run it again against the new S5 and paste its output here -- a body is never edited by hand.
 */
internal object S5Site {

    /** 200 · GET /api/asis before any site is named: site and site_at are blank on every row */
    const val LIST_NONE = """{"projects":[{"project":"PROJ-TWO","title":"Demo flat","customer":"","updated_at":"2026-11-11T17:45:42.174Z","rooms":1,"captures":1,"quote_stage":"Survey","quote_stage_by":"","quote_stage_at":"","site":"","site_at":"","work_states":{}},{"project":"PROJ-DEMO","title":"Demo flat","customer":"","updated_at":"2026-11-11T17:45:42.163Z","rooms":1,"captures":1,"quote_stage":"Survey","quote_stage_by":"","quote_stage_at":"","site":"","site_at":"","work_states":{}}]}"""
    /** 200 · the first name, base none (the seed, and a rename of a site S5 holds no name for) */
    const val NAMED_FIRST = """{"ok":true,"written":true,"project":"PROJ-DEMO","site":"Demo Site, Pune","site_by":"staff@mcft.test","site_at":"2026-11-11T17:45:42.194Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_NAMED_FIRST = """{"site":"Demo Site, Pune","base":""}"""
    /** 200 · the same name again, whatever its base: the retry after a lost answer */
    const val SAME = """{"ok":true,"written":false,"project":"PROJ-DEMO","site":"Demo Site, Pune","site_by":"staff@mcft.test","site_at":"2026-11-11T17:45:42.194Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_SAME = """{"site":"Demo Site, Pune","base":""}"""
    /** 200 · a rename with the name last seen as its base */
    const val RENAMED = """{"ok":true,"written":true,"project":"PROJ-DEMO","site":"Demo Site, Kothrud","site_by":"staff@mcft.test","site_at":"2026-11-11T17:45:42.211Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_RENAMED = """{"site":"Demo Site, Kothrud","base":"Demo Site, Pune"}"""
    /** 200 · another staff login renames it meanwhile */
    const val RENAMED_BY_LEAD = """{"ok":true,"written":true,"project":"PROJ-DEMO","site":"Demo Site, Baner","site_by":"lead@mcft.test","site_at":"2026-11-11T17:45:42.220Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_RENAMED_BY_LEAD = """{"site":"Demo Site, Baner","base":"Demo Site, Kothrud"}"""
    /** 409 · a stale base: somebody renamed the site meanwhile; S5 wins */
    const val STALE = """{"error":"Renamed by lead@mcft.test meanwhile — now Demo Site, Baner; not saved","site":"Demo Site, Baner","site_by":"lead@mcft.test","site_at":"2026-11-11T17:45:42.220Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_STALE = """{"site":"Demo Site, Aundh","base":"Demo Site, Kothrud"}"""
    /** 409 · a base, for a project S5 holds no name for */
    const val STALE_NONE = """{"error":"This project has no site name on S5 yet: send base \"\" to give it one; not saved","site":"","site_by":"","site_at":""}"""
    /** sent · PUT /api/asis/PROJ-TWO/site */
    const val SENT_STALE_NONE = """{"site":"Demo Site, Aundh","base":"Demo Site, Pune"}"""
    /** 409 · the stored name is the base after all (it moved away and back): send it again */
    const val STALE_AGAIN = """{"error":"Changed meanwhile; not saved — send it again","site":"Demo Site, Baner","site_by":"lead@mcft.test","site_at":"2026-11-11T17:45:42.220Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_STALE_AGAIN = """{"site":"Demo Site, Aundh","base":"Demo Site, Baner"}"""
    /** 403 · a vendor */
    const val VENDOR = """{"error":"Only MCFT staff can rename a site"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_VENDOR = """{"site":"Demo Site, Aundh","base":"Demo Site, Baner"}"""
    /** 403 · a client */
    const val CLIENT = """{"error":"Only MCFT staff can rename a site"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_CLIENT = """{"site":"Demo Site, Aundh","base":"Demo Site, Baner"}"""
    /** 404 · a project S5 has never been sent */
    const val NEVER = """{"error":"This project has never been uploaded"}"""
    /** sent · PUT /api/asis/PROJ-NEVER/site */
    const val SENT_NEVER = """{"site":"Demo Site, Aundh","base":""}"""
    /** 400 · a name over 140 characters */
    const val BAD_NAME = """{"error":"site must be the site's name: text of 1 to 140 characters, on one line, with no control characters"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_BAD_NAME = """{"site":"DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD","base":"Demo Site, Baner"}"""
    /** 400 · no base */
    const val BAD_BASE = """{"error":"base must be the site name last seen, or \"\" when the project has none"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_BAD_BASE = """{"site":"Demo Site, Aundh"}"""
    /** 401 · no sign-in at all */
    const val NO_SIGN_IN = """{"error":"Sign in with your ERP account"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_NO_SIGN_IN = """{"site":"Demo Site, Aundh","base":"Demo Site, Baner"}"""
    /** 401 · a key ERP does not accept */
    const val KEY_REFUSED = """{"error":"ERP did not accept this API key"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/site */
    const val SENT_KEY_REFUSED = """{"site":"Demo Site, Aundh","base":"Demo Site, Baner"}"""
    /** 200 · GET /api/asis afterwards: PROJ-DEMO as last renamed, PROJ-TWO still unnamed, PROJ-UP named by its first upload alone */
    const val LIST_NAMED = """{"projects":[{"project":"PROJ-DEMO","title":"Demo flat","customer":"","updated_at":"2026-11-11T17:45:42.280Z","rooms":1,"captures":1,"quote_stage":"Survey","quote_stage_by":"","quote_stage_at":"","site":"Demo Site, Baner","site_at":"2026-11-11T17:45:42.220Z","work_states":{}},{"project":"PROJ-UP","title":"Demo flat","customer":"","updated_at":"2026-11-11T17:45:42.270Z","rooms":1,"captures":1,"quote_stage":"Survey","quote_stage_by":"","quote_stage_at":"","site":"Demo Office, Pune","site_at":"2026-11-11T17:45:42.262Z","work_states":{}},{"project":"PROJ-TWO","title":"Demo flat","customer":"","updated_at":"2026-11-11T17:45:42.174Z","rooms":1,"captures":1,"quote_stage":"Survey","quote_stage_by":"","quote_stage_at":"","site":"","site_at":"","work_states":{}}]}"""
}
