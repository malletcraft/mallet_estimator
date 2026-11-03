package com.malletcrafts.interiorestimate.pano

/**
 * S5 QUOTE'S OWN ANSWERS TO THE QUOTE MOVE, verbatim (port 61c slice QM0, 3 Nov 2026). Each was produced by S5 as shipped
 * on stage (mcft-interior-estimate 9a51733): its router and its REAL SQL (every migration, on SQLite, as S5's own `(SQL)`
 * tests run it), a fake ERP answering whoami, and every move and scope record sent in the request shape [QuoteMove.stage]
 * and [QuoteMove.scope] build ({stage, base, scope?, scope_base?, via} and {works, base}). The scenario: PROJ-DEMO moved
 * from Survey through the quote journey (a stale base, a move back), refused an Approve with no scope, a Designer's
 * Approve (the PM named), a vendor's and a client's moves, a stage that is not one of the six, a stale scope_base after
 * the PM recorded a scope at Revisions, then approved with six works and refused every move from Client approved;
 * PROJ-REC approved before it had a scope (set in SQL, as a project approved before migration 0008) and its scope
 * recorded once; PROJ-NEVER never uploaded; then two work states and a change request on PROJ-DEMO and the list and
 * album GETs.
 *
 * WHAT THE SENT_ BODIES ARE, three kinds (QM0 review: this note once said every one was phone-built and pinned):
 * - NINE are built by QuoteMove from a copy that agrees with S5, byte for byte, and QuoteMoveTest's first test pins them
 *   to it: MOVED_FIRST, STALE, MOVED_BACK, PM_ONLY, SCOPE_STALE, APPROVED, NEVER, REC_APPROVED, REC_FINAL;
 * - TWO the phone never builds, and QuoteMoveTest pins that it does not: SCOPE_MISSING (an Approve with no scope, refused
 *   on the phone) and BAD_STAGE (a stage outside the six, nothing built). The harness sent them only to draw out S5's
 *   answer, which the verdict tests read as a defence;
 * - SIX QuoteMove can build, but only from a copy that disagrees with S5 -- the harness sent them from S5's state, not
 *   from a phone's: FINAL and FINAL_AGAIN carry the approval's own stamp as the base, from a copy not at Client
 *   approved; FINAL_STALE carries Revisions's stamp to Revisions, from a copy at another stage; REC_REVISIONS,
 *   REC_STALE and REC_NOT_YET are scope records, which the phone builds only from a copy at Client approved, while S5
 *   was at Revisions or Survey. They are not pinned to a phone input. Their ANSWERS still reach a phone whose copy is
 *   stale: FINAL_STALE's shape answers any move from a pre-approval copy after the web approved the project, and
 *   REC_NOT_YET's a scope record after a direct D1 put-back of an approved project (one was made on 28 Oct 2026).
 *
 * Every name is invented: mallet_estimator is public. The harness that drove S5 is in the session's notes
 * (qm0/harness.mjs; the 401s from qm0fix/h401.mjs, the same S5 with no sign-in and with a key ERP refuses), not in this
 * repository: when S5 changes an answer, run it again against the new S5 and paste its output here -- a body is never
 * edited by hand.
 */
internal object S5Quote {

    /** 200 · move to Client wish list, base none */
    const val MOVED_FIRST = """{"ok":true,"project":"PROJ-DEMO","quote_stage":"Client wish list","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.689Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_MOVED_FIRST = """{"stage":"Client wish list","base":"","via":"phone"}"""
    /** 409 · stale base */
    const val STALE = """{"error":"Changed by pm@x meanwhile — now Client wish list; not saved","quote_stage":"Client wish list","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.689Z","scope":null,"scope_at":""}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_STALE = """{"stage":"Vendor quoted","base":"","via":"phone"}"""
    /** 200 · move back to Vendor quoted */
    const val MOVED_BACK = """{"ok":true,"project":"PROJ-DEMO","quote_stage":"Vendor quoted","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.706Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_MOVED_BACK = """{"stage":"Vendor quoted","base":"2026-11-03T07:01:08.702Z","via":"phone"}"""
    /** 409 · approve without a scope, none stored */
    const val SCOPE_MISSING = """{"error":"scope_missing","message":"Tick the works in the approved estimate before moving to Client approved","quote_stage":"Revisions","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.710Z","scope":null,"scope_at":""}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_SCOPE_MISSING = """{"stage":"Client approved","base":"2026-11-03T07:01:08.710Z","via":"phone"}"""
    /** 403 · approve by a designer */
    const val PM_ONLY = """{"error":"Only the Project Manager approves the estimate","album_role":"designer","pm":"pm@x"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_PM_ONLY = """{"stage":"Client approved","base":"2026-11-03T07:01:08.710Z","scope":[10,20],"scope_base":"","via":"phone"}"""
    /** 403 · move by a vendor */
    const val STAFF_ONLY = """{"error":"Only MCFT staff can set the quote stage"}"""
    /** 403 · move by a client */
    const val STAFF_ONLY_CLIENT = """{"error":"Only MCFT staff can set the quote stage"}"""
    /** 400 · not a stage */
    const val BAD_STAGE = """{"error":"stage must be one of Survey, Client wish list, Vendor quoted, Estimated, Revisions, Client approved"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_BAD_STAGE = """{"stage":"On hold","base":"2026-11-03T07:01:08.710Z","via":"phone"}"""
    /** 409 · approve with a stale scope_base */
    const val SCOPE_STALE = """{"error":"The scope was changed meanwhile; not saved — it now shows what is stored","quote_stage":"Revisions","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.710Z","scope":[10,20,30],"scope_at":"2026-11-03T07:01:08.745Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_SCOPE_STALE = """{"stage":"Client approved","base":"2026-11-03T07:01:08.710Z","scope":[10,20,30,50],"scope_base":"","via":"phone"}"""
    /** 200 · approve */
    const val APPROVED = """{"ok":true,"project":"PROJ-DEMO","quote_stage":"Client approved","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.769Z","scope":[10,20,30,50,90,140],"scope_at":"2026-11-03T07:01:08.769Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_APPROVED = """{"stage":"Client approved","base":"2026-11-03T07:01:08.710Z","scope":[10,20,30,50,90,140],"scope_base":"2026-11-03T07:01:08.745Z","via":"phone"}"""
    /** 409 · move from Client approved */
    const val FINAL = """{"error":"Approved — changes go through change requests; not saved","final":true,"quote_stage":"Client approved","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.769Z","scope":[10,20,30,50,90,140],"scope_at":"2026-11-03T07:01:08.769Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_FINAL = """{"stage":"Revisions","base":"2026-11-03T07:01:08.769Z","via":"phone"}"""
    /** 409 · approve again */
    const val FINAL_AGAIN = """{"error":"Approved — changes go through change requests; not saved","final":true,"quote_stage":"Client approved","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.769Z","scope":[10,20,30,50,90,140],"scope_at":"2026-11-03T07:01:08.769Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_FINAL_AGAIN = """{"stage":"Client approved","base":"2026-11-03T07:01:08.769Z","scope":[10],"scope_base":"2026-11-03T07:01:08.769Z","via":"phone"}"""
    /** 409 · move from Client approved, stale base */
    const val FINAL_STALE = """{"error":"Changed by pm@x meanwhile — now Client approved; not saved","quote_stage":"Client approved","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.769Z","scope":[10,20,30,50,90,140],"scope_at":"2026-11-03T07:01:08.769Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/stage */
    const val SENT_FINAL_STALE = """{"stage":"Revisions","base":"2026-11-03T07:01:08.710Z","via":"phone"}"""
    /** 404 · move a project never uploaded */
    const val NEVER = """{"error":"This project has never been uploaded"}"""
    /** sent · PUT /api/asis/PROJ-NEVER/stage */
    const val SENT_NEVER = """{"stage":"Client wish list","base":"","via":"phone"}"""
    /** 200 · record the scope at Revisions */
    const val REC_REVISIONS = """{"ok":true,"project":"PROJ-DEMO","scope":[10,20,30],"scope_at":"2026-11-03T07:01:08.745Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/album/scope */
    const val SENT_REC_REVISIONS = """{"works":[10,20,30],"base":""}"""
    /** 409 · record the scope with a stale base */
    const val REC_STALE = """{"error":"The scope was changed meanwhile; not saved — it now shows what is stored","scope":[10,20,30],"scope_at":"2026-11-03T07:01:08.745Z","quote_stage":"Revisions","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.710Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/album/scope */
    const val SENT_REC_STALE = """{"works":[10,20],"base":""}"""
    /** 403 · record the scope by a designer */
    const val REC_DESIGNER = """{"error":"Only the Project Manager records the approved scope","album_role":"designer","pm":"pm@x"}"""
    /** 403 · record the scope by a vendor */
    const val REC_VENDOR = """{"error":"Only the Project Manager records the approved scope"}"""
    /** 400 · record an empty scope */
    const val REC_EMPTY = """{"error":"works must name at least one work"}"""
    /** 409 · record the scope once approved with one */
    const val REC_FINAL = """{"error":"Approved — the scope changes only through change requests; not saved","final":true,"scope":[10,20,30,50,90,140],"scope_at":"2026-11-03T07:01:08.769Z","quote_stage":"Client approved","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.769Z"}"""
    /** sent · PUT /api/asis/PROJ-DEMO/album/scope */
    const val SENT_REC_FINAL = """{"works":[10,20],"base":"2026-11-03T07:01:08.769Z"}"""
    /** 409 · record the scope at Survey */
    const val REC_NOT_YET = """{"error":"The scope is set from Estimated on — the quote stage is Survey","scope":null,"scope_at":"","quote_stage":"Survey","quote_stage_by":"","quote_stage_at":""}"""
    /** sent · PUT /api/asis/PROJ-REC/album/scope */
    const val SENT_REC_NOT_YET = """{"works":[10],"base":""}"""
    /** 200 · record the scope at Client approved, none stored */
    const val REC_APPROVED = """{"ok":true,"project":"PROJ-REC","scope":[10,20,140],"scope_at":"2026-11-03T07:01:08.797Z"}"""
    /** sent · PUT /api/asis/PROJ-REC/album/scope */
    const val SENT_REC_APPROVED = """{"works":[10,20,140],"base":""}"""
    /** 409 · record the scope again */
    const val REC_AGAIN = """{"error":"Approved — the scope changes only through change requests; not saved","final":true,"scope":[10,20,140],"scope_at":"2026-11-03T07:01:08.797Z","quote_stage":"Client approved","quote_stage_by":"pm@x","quote_stage_at":"2026-10-20T04:30:00.000Z"}"""
    /** 404 · record a scope on a project never uploaded */
    const val REC_NEVER = """{"error":"This project has never been uploaded"}"""
    /** 401 · move, or record the scope, not signed in (no session S5 holds, no key) -- both routes answer it alike */
    const val SIGN_IN = """{"error":"Sign in with your ERP account"}"""
    /** 401 · move, or record the scope, with a key ERP refuses -- both routes answer it alike */
    const val KEY_REFUSED = """{"error":"ERP did not accept this API key"}"""
    /** 200 · list pm */
    const val LIST = """{"projects":[{"project":"PROJ-REC","title":"Demo flat","customer":"CUST-DEMO","updated_at":"2026-11-03T07:01:08.789Z","rooms":1,"captures":1,"quote_stage":"Client approved","quote_stage_by":"pm@x","quote_stage_at":"2026-10-20T04:30:00.000Z","work_states":{}},{"project":"PROJ-DEMO","title":"Demo flat","customer":"CUST-DEMO","updated_at":"2026-11-03T07:01:08.680Z","rooms":1,"captures":1,"quote_stage":"Client approved","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.769Z","work_states":{"10":{"state":"In progress","seq":7},"20":{"state":"Done","seq":9}}}]}"""
    /** 200 · album pm */
    const val ALBUM = """{"project":"PROJ-DEMO","role":"pm","mine":[],"exec":true,"scope":[10,20,30,50,90,140],"scope_at":"2026-11-03T07:01:08.769Z","pm":"pm@x","quote_stage":"Client approved","quote_stage_by":"pm@x","quote_stage_at":"2026-11-03T07:01:08.769Z","works":[{"id":10,"state":"In progress","by":"pm@x","by_name":"P M","at":"2026-11-03T07:01:08.813Z","seq":10,"in_scope":true,"cr":{"id":"ALB-0000000000a4","kind":"change","by":"pm@x","by_name":"P M","at":"2026-11-03T07:01:08.828Z","note":"second coat","seq":10},"count":0},{"id":20,"state":"Done","by":"pm@x","by_name":"P M","at":"2026-11-03T07:01:08.823Z","seq":9,"in_scope":true,"cr":null,"count":0},{"id":30,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":true,"cr":null,"count":0},{"id":40,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":false,"cr":null,"count":0},{"id":50,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":true,"cr":null,"count":0},{"id":60,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":false,"cr":null,"count":0},{"id":70,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":false,"cr":null,"count":0},{"id":80,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":false,"cr":null,"count":0},{"id":85,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":false,"cr":null,"count":0},{"id":90,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":true,"cr":null,"count":0},{"id":95,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":false,"cr":null,"count":0},{"id":100,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":false,"cr":null,"count":0},{"id":110,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":false,"cr":null,"count":0},{"id":120,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":false,"cr":null,"count":0},{"id":130,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":false,"cr":null,"count":0},{"id":140,"state":"Not started","by":"","by_name":"","at":"","seq":0,"in_scope":true,"cr":null,"count":0}],"log":[{"seq":1,"id":"ALB-83023835101b","kind":"stage","work":null,"photo":null,"value":"Client wish list","prev":"Survey","note":"","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T07:01:08.689Z"},{"seq":2,"id":"ALB-3f0db65d1f27","kind":"stage","work":null,"photo":null,"value":"Vendor quoted","prev":"Client wish list","note":"","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T07:01:08.698Z"},{"seq":3,"id":"ALB-717aa1d93b23","kind":"stage","work":null,"photo":null,"value":"Estimated","prev":"Vendor quoted","note":"","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T07:01:08.702Z"},{"seq":4,"id":"ALB-d8ead54f9ef4","kind":"stage","work":null,"photo":null,"value":"Vendor quoted","prev":"Estimated","note":"","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T07:01:08.706Z"},{"seq":5,"id":"ALB-eba819199d63","kind":"stage","work":null,"photo":null,"value":"Revisions","prev":"Vendor quoted","note":"","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T07:01:08.710Z"},{"seq":6,"id":"ALB-42376c7281fa","kind":"stage","work":null,"photo":null,"value":"Client approved","prev":"Revisions","note":"","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T07:01:08.769Z"},{"seq":7,"id":"ALB-0000000000a1","kind":"state","work":10,"photo":null,"value":"In progress","prev":"Not started","note":"","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T07:01:08.813Z"},{"seq":8,"id":"ALB-0000000000a2","kind":"state","work":20,"photo":null,"value":"In progress","prev":"Not started","note":"","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T07:01:08.818Z"},{"seq":9,"id":"ALB-0000000000a3","kind":"state","work":20,"photo":null,"value":"Done","prev":"In progress","note":"","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T07:01:08.823Z"},{"seq":10,"id":"ALB-0000000000a4","kind":"cr_raise","work":10,"photo":null,"value":"change","prev":null,"note":"second coat","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T07:01:08.828Z"}],"log_next":10,"log_more":false,"stages":[{"seq":1,"id":"ALB-83023835101b","value":"Client wish list","prev":"Survey","by":"pm@x","by_name":"P M","via":"phone","at":"2026-11-03T07:01:08.689Z"},{"seq":2,"id":"ALB-3f0db65d1f27","value":"Vendor quoted","prev":"Client wish list","by":"pm@x","by_name":"P M","via":"phone","at":"2026-11-03T07:01:08.698Z"},{"seq":3,"id":"ALB-717aa1d93b23","value":"Estimated","prev":"Vendor quoted","by":"pm@x","by_name":"P M","via":"phone","at":"2026-11-03T07:01:08.702Z"},{"seq":4,"id":"ALB-d8ead54f9ef4","value":"Vendor quoted","prev":"Estimated","by":"pm@x","by_name":"P M","via":"phone","at":"2026-11-03T07:01:08.706Z"},{"seq":5,"id":"ALB-eba819199d63","value":"Revisions","prev":"Vendor quoted","by":"pm@x","by_name":"P M","via":"phone","at":"2026-11-03T07:01:08.710Z"},{"seq":6,"id":"ALB-42376c7281fa","value":"Client approved","prev":"Revisions","by":"pm@x","by_name":"P M","via":"phone","at":"2026-11-03T07:01:08.769Z"}],"photos":[],"captures":[{"room":"Kitchen","capture":"MCAP-DEMO-1","captured_at":"2026-10-11T08:30:00+05:30","stage":"","work":0,"by":"pm@x","at":"2026-11-03T07:01:08.680Z"}],"to_review":0,"change_requests":1,"closed":false,"closed_at":"","more":false}"""
}
