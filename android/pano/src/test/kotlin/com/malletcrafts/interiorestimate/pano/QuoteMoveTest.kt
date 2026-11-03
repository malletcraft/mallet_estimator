package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.QuoteMove.Local
import com.malletcrafts.interiorestimate.pano.QuoteMove.Move
import com.malletcrafts.interiorestimate.pano.QuoteMove.Now
import com.malletcrafts.interiorestimate.pano.QuoteMove.Verdict
import com.malletcrafts.interiorestimate.pano.QuoteStage.Cache
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The guarded quote move (port 61c slice QM0): the requests [QuoteMove] builds, pinned to the bodies S5 QUOTE accepted
 * ([S5Quote], S5 at 9a51733 on its real SQL), and S5's every answer read into its verdict from the body's fields, in
 * S5's order (asis.js, `PUT …/stage` and `PUT …/album/scope`). v61c.2: a project S5 does not list never builds a request.
 */
class QuoteMoveTest {

    private val P = "PROJ-DEMO"
    private fun tree(s: String): Any? = MiniJson.parse(s)

    /** [v] as org.json writes `JSONObject(fields)`, as AlbumQueueTest serialises the album outbox's requests. */
    private fun json(v: Any?): String = when (v) {
        null -> "null"
        is String -> "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Number, is Boolean -> v.toString()
        is Map<*, *> -> v.entries.joinToString(",", "{", "}") { json(it.key as String) + ":" + json(it.value) }
        is List<*> -> v.joinToString(",", "[", "]") { json(it) }
        else -> fail("not a JSON value: $v (${v::class})")
    }

    private fun send(m: Move): Move.Send = assertIs<Move.Send>(m, "expected a request, got $m")
    private fun body(m: Move): String = json(send(m).request.fields)

    // the stamps S5 wrote in the harness run (S5Quote)
    private val tWish = "2026-11-03T07:01:08.689Z"
    private val tEstimated = "2026-11-03T07:01:08.702Z"
    private val tRevisions = "2026-11-03T07:01:08.710Z"
    private val tScope = "2026-11-03T07:01:08.745Z"
    private val tApproved = "2026-11-03T07:01:08.769Z"

    private fun at(stage: String, at: String) = Cache(listed = true, stage = stage, by = "pm@x", at = at, readAt = "2026-11-03T07:01:09Z")

    @Test fun `each move is exactly the body S5 accepted - built by QuoteMove, serialised as org-json writes it`() {
        // the first move: S5 never stamped the stage, so the base is ''
        assertEquals(S5Quote.SENT_MOVED_FIRST, body(QuoteMove.stage(P, Cache(listed = true, stage = "Survey", readAt = "r"), "Client wish list")))
        // a stale copy still on '' after S5 moved: the same base, which S5 answers 409
        assertEquals(S5Quote.SENT_STALE, body(QuoteMove.stage(P, Cache(listed = true, stage = "Client wish list", readAt = "r"), "Vendor quoted")))
        // a backward move is a move like any other until Client approved
        assertEquals(S5Quote.SENT_MOVED_BACK, body(QuoteMove.stage(P, at("Estimated", tEstimated), "Vendor quoted")))
        // Approve: the ticks, de-duplicated, in WORKS order, with the scope_at the mirror last saw
        assertEquals(S5Quote.SENT_APPROVED, body(QuoteMove.stage(P, at("Revisions", tRevisions), "Client approved", listOf(140, 10, 90, 20, 30, 50, 10, 140), tScope)))
        // scope_base '' when no scope is stored
        assertEquals(S5Quote.SENT_PM_ONLY, body(QuoteMove.stage(P, at("Revisions", tRevisions), "Client approved", listOf(20, 10))))
        assertEquals(S5Quote.SENT_SCOPE_STALE, body(QuoteMove.stage(P, at("Revisions", tRevisions), "Client approved", setOf(50, 30, 20, 10), "")))
        assertEquals(S5Quote.SENT_NEVER, body(QuoteMove.stage("PROJ-NEVER", Cache(listed = true, stage = "Survey", readAt = "r"), "Client wish list")))
        // Record the approved scope: {works, base}, in WORKS order, at Client approved only
        assertEquals(S5Quote.SENT_REC_APPROVED, body(QuoteMove.scope("PROJ-REC", at("Client approved", "2026-10-20T04:30:00.000Z"), listOf(140, 20, 10, 20), "")))
        assertEquals(S5Quote.SENT_REC_FINAL, body(QuoteMove.scope(P, at("Client approved", tApproved), listOf(20, 10), tApproved)))
        // the two S5Quote says the phone never builds: SENT_SCOPE_MISSING (an Approve with no scope) is refused on the
        // phone, and SENT_BAD_STAGE (a stage outside the six) builds nothing -- from the copy the harness sent them from
        assertEquals("""{"stage":"Client approved","base":"$tRevisions","via":"phone"}""", S5Quote.SENT_SCOPE_MISSING)
        assertEquals(Move.Refused(Local.SCOPE_MISSING), QuoteMove.stage(P, at("Revisions", tRevisions), "Client approved"))
        assertEquals("""{"stage":"On hold","base":"$tRevisions","via":"phone"}""", S5Quote.SENT_BAD_STAGE)
        assertEquals(Move.None, QuoteMove.stage(P, at("Revisions", tRevisions), "On hold"))
    }

    @Test fun `the route, the JSON types, and the base sent byte for byte - never reformatted (SPEC61c R6)`() {
        val enc: (String) -> String = { s -> s.replace("%", "%25").replace(" ", "%20").replace("/", "%2F") }
        val m = send(QuoteMove.stage(P, at("Revisions", tRevisions), "Client approved", listOf(10), tScope))
        assertEquals("PUT /api/asis/PROJ-DEMO/stage application/json", "${m.request.method} ${m.request.path(enc)} ${m.request.contentType}")
        assertEquals(listOf("stage", "base", "scope", "scope_base", "via"), m.request.fields.keys.toList())
        assertTrue(m.request.fields["scope"] is List<*> && (m.request.fields["scope"] as List<*>).all { it is Int })
        assertEquals("phone", m.request.fields["via"])
        assertNull(m.request.file)
        assertEquals(tRevisions, m.base); assertEquals(tScope, m.scopeBase)
        // a UTC 'Z' stamp with milliseconds, an offset stamp, a stamp with no milliseconds: each the very bytes S5 wrote
        for (stamp in listOf("2026-11-03T07:01:08.710Z", "2026-11-03T12:31:08.710+05:30", "2026-11-03T07:01:08Z", "2026-11-03T07:01:08.700Z")) {
            val s = send(QuoteMove.stage(P, at("Estimated", stamp), "Revisions"))
            assertEquals(stamp, s.request.fields["base"], "the base is quote_stage_at as stored, never parsed and written back")
            assertEquals(stamp, s.base)
        }
        // no scope on any other move: S5 answers 400 "scope goes only with Client approved"
        val other = send(QuoteMove.stage(P, at("Estimated", tEstimated), "Revisions", listOf(10, 20), tScope))
        assertEquals(listOf("stage", "base", "via"), other.request.fields.keys.toList())
        assertNull(other.scopeBase)
        val sc = send(QuoteMove.scope("Demo flat/1", at("Client approved", tApproved), listOf(10), ""))
        assertEquals("PUT /api/asis/Demo%20flat%2F1/album/scope", "${sc.request.method} ${sc.request.path(enc)}")
        assertEquals(listOf("works", "base"), sc.request.fields.keys.toList())
    }

    @Test fun `an empty tick list is refused on the phone, and only the sixteen works are ever sent`() {
        for (ticks in listOf(null, emptyList(), listOf(0), listOf(999, -1))) {
            assertEquals(Move.Refused(Local.SCOPE_MISSING), QuoteMove.stage(P, at("Revisions", tRevisions), "Client approved", ticks, tScope), "$ticks")
            assertEquals(Move.Refused(Local.SCOPE_MISSING), QuoteMove.scope(P, at("Client approved", tApproved), ticks.orEmpty(), ""), "$ticks")
        }
        // a scope record's empty list is refused BEFORE the stage is looked at (the prototype's recordScope order):
        // at every stage before Client approved, and on a copy never read -- not the "nothing to send" of a stage
        for (s in QuoteStage.STAGES.dropLast(1)) assertEquals(Move.Refused(Local.SCOPE_MISSING), QuoteMove.scope(P, at(s, tRevisions), emptyList(), ""), s)
        assertEquals(Move.Refused(Local.SCOPE_MISSING), QuoteMove.scope(P, null, emptyList(), ""))
        assertEquals(Move.Refused(Local.SCOPE_MISSING), QuoteMove.scope(P, at("Estimated", tEstimated), listOf(0, 999), ""))
        assertEquals("[10,140]", json(send(QuoteMove.stage(P, at("Revisions", tRevisions), "Client approved", listOf(999, 140, 0, 10), "")).request.fields["scope"]))
        assertEquals(listOf(10, 20, 30, 40, 50, 60, 70, 80, 85, 90, 95, 100, 110, 120, 130, 140), QuoteMove.worksOf(Album.WORK_IDS.reversed()))
    }

    @Test fun `no move is built from Client approved - approval is final - and nothing is built for an unread or same stage`() {
        for (to in QuoteStage.STAGES) assertEquals(
            if (to == QuoteStage.APPROVED) Move.None else Move.Refused(Local.APPROVED_FINAL),
            QuoteMove.stage(P, at("Client approved", tApproved), to, listOf(10), tApproved), to)
        // unread: no copy, a blank stage, a name outside the six -- no move is offered
        for (c in listOf(null, Cache(listed = true, readAt = "r"), at("On hold", tApproved))) assertEquals(Move.None, QuoteMove.stage(P, c, "Estimated"))
        // a target outside the six, and the stage it already is
        assertEquals(Move.None, QuoteMove.stage(P, at("Estimated", tEstimated), "On hold"))
        assertEquals(Move.None, QuoteMove.stage(P, at("Estimated", tEstimated), " Estimated "))
        // the scope is recorded only at Client approved
        for (s in QuoteStage.STAGES.dropLast(1)) assertEquals(Move.None, QuoteMove.scope(P, at(s, tEstimated), listOf(10), ""), s)
        assertEquals(Move.None, QuoteMove.scope(P, null, listOf(10), ""))
    }

    @Test fun `v61c2 - a project S5 does not list builds no request, refused before every other check, nothing kept to send later`() {
        // no S5 id (made on this phone), or read as not listed -- whatever stage, stamp or states the phone holds
        val held = Cache(listed = false, stage = "Revisions", by = "pm@x", at = tRevisions, readAt = "r",
            states = mapOf(10 to Album.WorkState(Album.DONE, 7)))
        val cases = listOf("" to null, "" to at("Revisions", tRevisions), " " to at("Client approved", tApproved), P to held,
            P to Cache(listed = false, readAt = "r"), "" to Cache(listed = false, readAt = "r"))
        for ((id, c) in cases) {
            assertTrue(QuoteMove.notOnS5(id, c), "$id $c")
            for (to in QuoteStage.STAGES) {
                // the refusal comes first: before the approval-final refusal, the empty-scope one, the unread and the same-stage none
                assertEquals(Move.Refused(Local.NOT_ON_S5), QuoteMove.stage(id, c, to), "$id $c $to")
                assertEquals(Move.Refused(Local.NOT_ON_S5), QuoteMove.stage(id, c, to, emptyList(), ""), "$id $c $to")
                assertEquals(Move.Refused(Local.NOT_ON_S5), QuoteMove.stage(id, c, to, listOf(10), tScope), "$id $c $to")
            }
            assertEquals(Move.Refused(Local.NOT_ON_S5), QuoteMove.stage(id, c, "On hold"))
            assertEquals(Move.Refused(Local.NOT_ON_S5), QuoteMove.scope(id, c, listOf(10), ""))
            assertEquals(Move.Refused(Local.NOT_ON_S5), QuoteMove.scope(id, c, emptyList(), ""))
        }
        // the words, in both languages: the prototype's NOT_SENT_MSG
        assertEquals("not on S5 yet — this project has not been sent", QuoteMove.localText(Tx.EN, Local.NOT_ON_S5))
        assertEquals("अजून S5 वर नाही — हा प्रोजेक्ट पाठवलेला नाही", QuoteMove.localText(Tx.MR, Local.NOT_ON_S5))
        // a refusal carries nothing to send and nothing for the store: there is no request in it and no copy of a stage
        assertEquals(listOf("why"), Move.Refused::class.java.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }.map { it.name })
        // ... and once S5 lists the project (the store's copy is listed), the move is v61c.1's
        assertEquals(S5Quote.SENT_MOVED_FIRST, body(QuoteMove.stage(P, Cache(listed = true, stage = "Survey", readAt = "r"), "Client wish list")))
        assertTrue(!QuoteMove.notOnS5(P, null), "never read is not 'not on S5': it is unread")
    }

    private fun stage(code: Int?, s: String?, sent: Move.Send): Verdict = QuoteMove.answer(MiniRead, code, s?.let { tree(it) }, sent)
    private fun rec(code: Int?, s: String?): Verdict = QuoteMove.scopeAnswer(MiniRead, code, s?.let { tree(it) })
    private fun sent(base: String, scopeBase: String? = null) = Move.Send(AlbumQueue.Request("PUT", listOf(P, "stage"), emptyMap(), AlbumQueue.JSON), base, scopeBase)

    @Test fun `every S5 answer to a move reads as its verdict, from the body's fields, in S5's order`() {
        val first = send(QuoteMove.stage(P, Cache(listed = true, stage = "Survey", readAt = "r"), "Client wish list"))
        assertEquals(Verdict.Moved(Now("Client wish list", "pm@x", tWish, null, null)), stage(200, S5Quote.MOVED_FIRST, first))
        // 409, base '' while S5 has a stamp: stale, with S5's stage as it is now
        assertEquals(Verdict.Stale(Now("Client wish list", "pm@x", tWish, null, "")), stage(409, S5Quote.STALE, first))
        // 409 scope_missing (an Approve with no scope and none stored): its message, S5's words, as data
        assertEquals(Verdict.ScopeMissing("Tick the works in the approved estimate before moving to Client approved", Now("Revisions", "pm@x", tRevisions, null, "")),
            stage(409, S5Quote.SCOPE_MISSING, sent(tRevisions)))
        // 403 with album_role: only the PM approves -- the role and the PM's login taken
        assertEquals(Verdict.PmOnly("designer", "pm@x"), stage(403, S5Quote.PM_ONLY, sent(tRevisions, "")))
        // 403 without it: not staff
        assertEquals(Verdict.StaffOnly, stage(403, S5Quote.STAFF_ONLY, sent(tRevisions)))
        assertEquals(Verdict.StaffOnly, stage(403, S5Quote.STAFF_ONLY_CLIENT, sent(tRevisions)))
        // 400: S5's own words, as data
        assertEquals(Verdict.Refused(400, "stage must be one of Survey, Client wish list, Vendor quoted, Estimated, Revisions, Client approved"), stage(400, S5Quote.BAD_STAGE, sent(tRevisions)))
        // 409, the scope_base not S5's scope_at: the scope changed meanwhile
        assertEquals(Verdict.ScopeStale(Now("Revisions", "pm@x", tRevisions, listOf(10, 20, 30), tScope)), stage(409, S5Quote.SCOPE_STALE, sent(tRevisions, "")))
        // 200 to Approve: the stage and the scope S5 stored
        assertEquals(Verdict.Moved(Now("Client approved", "pm@x", tApproved, listOf(10, 20, 30, 50, 90, 140), tApproved)), stage(200, S5Quote.APPROVED, sent(tRevisions, tScope)))
        // 409 final: a move from Client approved with the right base, or Client approved again
        val nowApproved = Now("Client approved", "pm@x", tApproved, listOf(10, 20, 30, 50, 90, 140), tApproved)
        assertEquals(Verdict.Final(nowApproved), stage(409, S5Quote.FINAL, sent(tApproved)))
        assertEquals(Verdict.Final(nowApproved), stage(409, S5Quote.FINAL_AGAIN, sent(tApproved, tApproved)))
        // ... but a STALE base is answered first, as S5 does, even at Client approved: stale, not final
        assertEquals(Verdict.Stale(nowApproved), stage(409, S5Quote.FINAL_STALE, sent(tRevisions)))
        // 404 from S5: no record of the project
        assertEquals(Verdict.NotOnS5, stage(404, S5Quote.NEVER, sent("")))
    }

    @Test fun `the 409s are told apart by fields, never by S5's English`() {
        // the same bodies with every error text replaced: the verdicts do not move
        fun blind(s: String): String = s.replace(Regex("\"error\":\"[^\"]*\""), "\"error\":\"x\"")
        assertIs<Verdict.Stale>(stage(409, blind(S5Quote.STALE), sent("")))
        assertIs<Verdict.Final>(stage(409, blind(S5Quote.FINAL), sent(tApproved)))
        assertIs<Verdict.Stale>(stage(409, blind(S5Quote.FINAL_STALE), sent(tRevisions)))
        assertIs<Verdict.ScopeStale>(stage(409, blind(S5Quote.SCOPE_STALE), sent(tRevisions, "")))
        assertIs<Verdict.ScopeFinal>(rec(409, blind(S5Quote.REC_FINAL)))
        assertIs<Verdict.NotYet>(rec(409, blind(S5Quote.REC_NOT_YET)))
        assertIs<Verdict.ScopeStale>(rec(409, blind(S5Quote.REC_STALE)))
        // scope_missing is S5's CODE in `error` (asis.js SCOPE_MISSING), not words: reworded, it would be S5's last line, stale
        assertIs<Verdict.ScopeMissing>(stage(409, S5Quote.SCOPE_MISSING, sent(tRevisions)))
        assertIs<Verdict.Stale>(stage(409, blind(S5Quote.SCOPE_MISSING), sent(tRevisions)))
        // the scope_base is compared only when one was sent
        assertIs<Verdict.Stale>(stage(409, S5Quote.SCOPE_STALE, sent(tRevisions)))
        // S5's ORDER, on bodies built here (S5 itself never sends two of these at once, so its own answers cannot tell a
        // reader in another order apart): the stamp before `final`, `final` before the scope_base, the scope_base before
        // scope_missing -- asis.js's lines, top to bottom
        fun both(at: String, scopeAt: String, extra: String) =
            """{"error":"x"$extra,"quote_stage":"Client approved","quote_stage_by":"pm@x","quote_stage_at":"$at","scope":[10],"scope_at":"$scopeAt"}"""
        assertIs<Verdict.Stale>(stage(409, both(tApproved, tScope, ""","final":true"""), sent(tRevisions, tScope)), "a stale base first, even with final")
        assertIs<Verdict.Final>(stage(409, both(tApproved, tApproved, ""","final":true"""), sent(tApproved, "")), "final before a scope_base that differs")
        assertIs<Verdict.ScopeStale>(stage(409, both(tRevisions, tScope, "").replace("\"error\":\"x\"", "\"error\":\"scope_missing\""), sent(tRevisions, "")),
            "a scope_base that differs before scope_missing")
        // a scope record's not-yet is EVERY stage before Estimated (asis.js SCOPE_STAGES), not only Survey; and with no
        // scope stored, from Estimated on, it is the scope changed meanwhile -- the bodies in asis.js's notYet and stale shapes
        fun recAt(stage: String, at: String, error: String) =
            """{"error":"$error","scope":null,"scope_at":"","quote_stage":"$stage","quote_stage_by":"pm@x","quote_stage_at":"$at"}"""
        for ((st, t) in listOf("Client wish list" to tWish, "Vendor quoted" to "2026-11-03T07:01:08.706Z")) {
            val words = "The scope is set from Estimated on — the quote stage is $st"
            assertEquals(Verdict.NotYet(words, Now(st, "pm@x", t, null, "")), rec(409, recAt(st, t, words)), st)
            assertIs<Verdict.NotYet>(rec(409, recAt(st, t, "x")), st)
        }
        assertEquals(Verdict.ScopeStale(Now("Revisions", "pm@x", tRevisions, null, "")),
            rec(409, recAt("Revisions", tRevisions, "The scope was changed meanwhile; not saved — it now shows what is stored")))
        assertIs<Verdict.ScopeStale>(rec(409, recAt("Estimated", tEstimated, "x")))
    }

    @Test fun `every S5 answer to a scope record reads as its verdict`() {
        assertEquals(Verdict.Recorded(listOf(10, 20, 140), "2026-11-03T07:01:08.797Z"), rec(200, S5Quote.REC_APPROVED))
        assertEquals(Verdict.Recorded(listOf(10, 20, 30), tScope), rec(200, S5Quote.REC_REVISIONS))
        assertEquals(Verdict.ScopeStale(Now("Revisions", "pm@x", tRevisions, listOf(10, 20, 30), tScope)), rec(409, S5Quote.REC_STALE))
        assertEquals(Verdict.PmScope("designer", "pm@x"), rec(403, S5Quote.REC_DESIGNER))
        assertEquals(Verdict.PmScope(null, null), rec(403, S5Quote.REC_VENDOR), "not staff: the same words, no role")
        assertEquals(Verdict.Refused(400, "works must name at least one work"), rec(400, S5Quote.REC_EMPTY))
        assertEquals(Verdict.ScopeFinal(Now("Client approved", "pm@x", tApproved, listOf(10, 20, 30, 50, 90, 140), tApproved)), rec(409, S5Quote.REC_FINAL))
        assertEquals(Verdict.ScopeFinal(Now("Client approved", "pm@x", "2026-10-20T04:30:00.000Z", listOf(10, 20, 140), "2026-11-03T07:01:08.797Z")), rec(409, S5Quote.REC_AGAIN))
        assertEquals(Verdict.NotYet("The scope is set from Estimated on — the quote stage is Survey", Now("Survey", "", "", null, "")), rec(409, S5Quote.REC_NOT_YET))
        assertEquals(Verdict.NotOnS5, rec(404, S5Quote.REC_NEVER))
        assertEquals(listOf("Estimated", "Revisions", "Client approved"), QuoteMove.SCOPE_STAGES, "asis.js SCOPE_STAGES")
    }

    @Test fun `no answer of S5's handler is no answer - the phone re-reads, and no verdict keeps the old base`() {
        val s = sent(tRevisions, "")
        // nothing came back
        assertEquals(Verdict.NoAnswer(null), stage(null, null, s))
        assertEquals(Verdict.NoAnswer(null), rec(null, null))
        // a body that is not S5's: a proxy's page, a cut body, a 200 without S5's fields
        // (each 200 check alone, QM0 review: one body with S5's every field but no stamp, one with every field but no `ok`)
        for ((code, b) in listOf(200 to null, 200 to "\"<html>\"", 200 to """{"ok":true}""", 200 to """{"quote_stage":"Revisions"}""",
                200 to """{"ok":true,"quote_stage":"Revisions","quote_stage_by":"pm@x","quote_stage_at":""}""",
                200 to """{"quote_stage":"Revisions","quote_stage_by":"pm@x","quote_stage_at":"$tRevisions"}""",
                200 to """{"ok":false,"quote_stage":"Revisions","quote_stage_by":"pm@x","quote_stage_at":"$tRevisions"}""",
                403 to "\"Forbidden\"", 403 to "{}", 404 to "{}", 409 to """{"error":"x"}""", 409 to """{"quote_stage":"Revisions","quote_stage_at":""}""",
                400 to "{}", 413 to null, 401 to "{}", 401 to null, 401 to "\"Unauthorized\"", 405 to """{"error":"Method not allowed"}""",
                500 to """{"error":"x"}""", 502 to null)) {
            assertEquals(Verdict.NoAnswer(code), stage(code, b, s), "$code $b")
        }
        for ((code, b) in listOf(200 to """{"ok":true,"scope":[10]}""", 200 to """{"ok":true,"scope_at":"t"}""", 403 to "{}", 409 to """{"error":"x"}""",
                401 to "{}", 401 to null, 503 to null))
            assertEquals(Verdict.NoAnswer(code), rec(code, b), "$code $b")
        assertEquals(Verdict.Refused(413, "Request too large"), stage(413, """{"error":"Request too large"}""", s))
        // none of them names a stage, so the store keeps its copy -- and with it no base the phone could resend
        for (v in listOf(Verdict.NoAnswer(null), Verdict.NoAnswer(502), Verdict.Offline, Verdict.SignIn("x"), Verdict.StaffOnly,
                Verdict.PmOnly("designer", "pm@x"), Verdict.PmScope(null, null), Verdict.Refused(400, "x"), Verdict.Recorded(listOf(10), tScope)))
            assertNull(QuoteMove.copyOf(v, "r"), "$v")
    }

    @Test fun `S5's 401 is a sign-in refused, not no answer - the status line says sign in again, on both routes (QM0 review)`() {
        val s = sent(tRevisions, "")
        // S5's own 401s (asis.js identify): no session S5 holds, and a key ERP refuses
        assertEquals(Verdict.SignIn("Sign in with your ERP account"), stage(401, S5Quote.SIGN_IN, s))
        assertEquals(Verdict.SignIn("ERP did not accept this API key"), stage(401, S5Quote.KEY_REFUSED, s))
        assertEquals(Verdict.SignIn("Sign in with your ERP account"), rec(401, S5Quote.SIGN_IN))
        assertEquals(Verdict.SignIn("ERP did not accept this API key"), rec(401, S5Quote.KEY_REFUSED))
        // read by the field: S5's words reworded are still a sign-in refused
        assertEquals(Verdict.SignIn("x"), stage(401, """{"error":"x"}""", s))
        // the phone's sign-in words (app.sign_again_menu_server_x), then S5's as data -- never "No answer to the save"
        assertEquals("Sign in again (menu → Server): Sign in with your ERP account", QuoteMove.text(Tx.EN, stage(401, S5Quote.SIGN_IN, s)))
        assertEquals("पुन्हा साइन इन करा (मेनू → सर्व्हर): ERP did not accept this API key", QuoteMove.text(Tx.MR, rec(401, S5Quote.KEY_REFUSED)))
        // it names no stage: the store keeps its copy
        assertNull(QuoteMove.copyOf(stage(401, S5Quote.SIGN_IN, s), "r"))
    }

    @Test fun `S5's 404 goes into the one store as not listed - the line, the mover and Approve read not on S5 yet (QM0 review)`() {
        val v = stage(404, S5Quote.NEVER, sent(""))
        assertEquals(Verdict.NotOnS5, v)
        assertEquals(Cache(listed = false, readAt = "r5"), QuoteMove.copyOf(v, "r5"))
        assertEquals(Cache(listed = false, readAt = "r5"), QuoteMove.copyOf(rec(404, S5Quote.REC_NEVER), "r5"))
        // a copy read as listed at Revisions, with a work state, and then S5 answers that it holds no such project
        val stored = at("Revisions", tRevisions).copy(states = mapOf(10 to Album.WorkState(Album.DONE, 7)))
        val gone = QuoteStage.merge(stored, QuoteMove.copyOf(v, "2026-11-03T07:02:00Z")!!, 5L, 3L)
        assertEquals(Cache(listed = false, readAt = "2026-11-03T07:02:00Z"), gone)
        val line = QuoteStage.projectLine(gone, false, emptyList(), Tx.EN, java.time.Instant.parse("2026-11-03T08:00:00Z"))
        assertEquals(QuoteStage.LineKind.LOCAL, line.kind)
        assertEquals(QuoteMove.text(Tx.EN, v), line.second, "the status line and the project line say the same")
        assertEquals(Move.Refused(Local.NOT_ON_S5), QuoteMove.stage(P, gone, "Estimated"))
        assertEquals(Move.Refused(Local.NOT_ON_S5), QuoteMove.scope(P, gone, listOf(10), ""))
        // a read that went out before the 404 (a lower seq) does not put the project back ...
        val listedAgain = listOf(QuoteStage.Listed(P, "Revisions", "pm@x", tRevisions))
        assertTrue(QuoteStage.fromList(listedAgain, listOf(P), "2026-11-03T07:01:30Z", 4L, mapOf(P to 5L), mapOf(P to gone)).isEmpty())
        // ... and a read after it that lists the project makes it movable again, with S5's stamp as the base
        val back = QuoteStage.fromList(listedAgain, listOf(P), "2026-11-03T07:03:00Z", 6L, mapOf(P to 5L), mapOf(P to gone))[P]!!
        assertEquals(tRevisions, send(QuoteMove.stage(P, back, "Estimated")).base)
    }

    @Test fun `a stage S5 named without its stamp builds no move until a read fills the stamp in - never a false 409 (QM0 review)`() {
        // the wish list's Survey 409 names the stage and nothing more (QuoteStageSync.named): who and when UNKNOWN
        val named = QuoteStage.named(at("Client wish list", tWish), "Survey", "2026-11-03T07:01:10Z")!!
        assertTrue(named.atUnknown)
        for (to in QuoteStage.STAGES) assertEquals(Move.None, QuoteMove.stage(P, named, to, listOf(10), tScope), to)
        // named Client approved (the final 409): refused as final, which needs no base
        assertEquals(Move.Refused(Local.APPROVED_FINAL), QuoteMove.stage(P, QuoteStage.named(at("Revisions", tRevisions), QuoteStage.APPROVED, "r")!!, "Revisions"))
        // a stage S5 NEVER set is not unknown: base '', which S5 accepts (S5Quote.MOVED_FIRST)
        assertFalse(Cache(listed = true, stage = "Survey", readAt = "r").atUnknown)
        assertEquals(S5Quote.SENT_MOVED_FIRST, body(QuoteMove.stage(P, Cache(listed = true, stage = "Survey", readAt = "r"), "Client wish list")))
        // the follow-up read (a later seq) fills it in: the move carries S5's own stamp
        val tBack = "2026-11-03T07:01:09.500Z"
        val survey = listOf(QuoteStage.Listed(P, "Survey", "pm@x", tBack))
        val filled = QuoteStage.fromList(survey, listOf(P), "2026-11-03T07:01:11Z", 3L, mapOf(P to 2L), mapOf(P to named))[P]!!
        assertFalse(filled.atUnknown)
        assertEquals(tBack, send(QuoteMove.stage(P, filled, "Client wish list")).base)
        // ... or says S5 never set it: base ''
        val never = QuoteStage.fromList(listOf(QuoteStage.Listed(P, "Survey", "", "")), listOf(P), "2026-11-03T07:01:11Z", 3L, mapOf(P to 2L), mapOf(P to named))[P]!!
        assertEquals("", send(QuoteMove.stage(P, never, "Client wish list")).base)
        // a read that went out before the answer (a lower seq) leaves the copy as named: still nothing is built
        assertTrue(QuoteStage.fromList(survey, listOf(P), "2026-11-03T07:01:09Z", 1L, mapOf(P to 2L), mapOf(P to named)).isEmpty())
        // a move's own answer, and the album GET, fill it in too
        val staleAnswer = stage(409, S5Quote.STALE, sent(""))
        assertEquals(tWish, send(QuoteMove.stage(P, QuoteStage.merge(named, QuoteMove.copyOf(staleAnswer, "r")!!, 3L, 2L), "Vendor quoted")).base)
        assertFalse(QuoteStage.merge(named, QuoteStage.fromAlbum(AlbumWire.view(MiniRead, tree(S5Quote.ALBUM))!!, "r"), 3L, 2L).atUnknown)
    }

    @Test fun `every answer that says what S5 holds goes into the one store - the next move carries S5's base`() {
        val read = "2026-11-03T07:01:09Z"
        val stale = stage(409, S5Quote.STALE, sent(""))
        assertEquals(Cache(listed = true, stage = "Client wish list", by = "pm@x", at = tWish, readAt = read), QuoteMove.copyOf(stale, read))
        // through the store's merge rule: S5's stamp is newer than the stored copy's '', so it is taken ...
        val stored = Cache(listed = true, stage = "Survey", readAt = "2026-11-03T07:00:00Z", states = mapOf(10 to Album.WorkState(Album.IN_PROGRESS, 7)))
        val merged = QuoteStage.merge(stored, QuoteMove.copyOf(stale, read)!!, 2L, 1L)
        assertEquals("Client wish list", merged.stage)
        assertEquals(stored.states, merged.states, "a move's answer carries no work states: the stored ones stay")
        // ... and the next tap is sent with S5's stamp as its base
        assertEquals(tWish, send(QuoteMove.stage(P, merged, "Vendor quoted")).base)
        // a move, a final 409, a scope 409: each copy is S5's stage as it is now
        for ((v, stageNow) in listOf(stage(200, S5Quote.APPROVED, sent(tRevisions, tScope)) to "Client approved",
                stage(409, S5Quote.FINAL, sent(tApproved)) to "Client approved", stage(409, S5Quote.SCOPE_STALE, sent(tRevisions, "")) to "Revisions",
                rec(409, S5Quote.REC_FINAL) to "Client approved", rec(409, S5Quote.REC_NOT_YET) to "Survey"))
            assertEquals(stageNow, QuoteMove.copyOf(v, read)!!.stage, "$v")
        // a final 409 makes the line Execution at once (the Wish list's W rule, for every reader)
        val final = stage(409, S5Quote.FINAL, sent(tApproved))
        assertIs<Verdict.Final>(final)
        val line = QuoteStage.projectLine(QuoteStage.merge(at("Revisions", tRevisions), QuoteMove.copyOf(final, read)!!, 3L, 2L),
            false, emptyList(), Tx.EN, java.time.Instant.parse("2026-11-03T08:00:00Z"))
        assertEquals("Execution · not read yet", line.text, "S5 named the stage; the works are not read yet")
    }

    @Test fun `the status line is S5's words through their keys, in English and in Marathi`() {
        val stale = stage(409, S5Quote.STALE, sent(""))
        assertEquals("Changed by pm@x at 12:31 — not saved", QuoteMove.text(Tx.EN, stale), "07:01 UTC is 12:31 IST")
        assertEquals("12:31 ला pm@x यांनी बदलला — सेव्ह झाले नाही", QuoteMove.text(Tx.MR, stale))
        assertEquals("Changed meanwhile — not saved", QuoteMove.text(Tx.EN, Verdict.Stale(Now("Revisions", "", tRevisions, null, ""))))
        assertEquals("Changed meanwhile — not saved", QuoteMove.text(Tx.EN, Verdict.Stale(Now("Revisions", "pm@x", "not a time", null, ""))))
        val n = Now("Client approved", "pm@x", tApproved, null, null)
        val en = mapOf<Verdict, String?>(
            Verdict.Moved(n) to null,
            Verdict.Recorded(listOf(10), tScope) to null,
            Verdict.PmOnly("designer", "pm@x") to "Only the Project Manager approves the estimate",
            Verdict.PmScope("designer", "pm@x") to "Only the Project Manager records the approved scope",
            Verdict.StaffOnly to "Only MCFT staff can set the quote stage",
            Verdict.Final(n) to "Approved — changes go through change requests; not saved",
            Verdict.ScopeStale(n) to "The scope was changed meanwhile; not saved — it now shows what is stored",
            Verdict.ScopeMissing("whatever S5 says", n) to "Tick the works in the approved estimate before moving to Client approved",
            Verdict.ScopeFinal(n) to "Approved — the scope changes only through change requests; not saved",
            Verdict.NotYet("The scope is set from Estimated on — the quote stage is Survey", n) to "The scope is set from Estimated on — the quote stage is Survey",
            Verdict.Refused(400, "scope must name at least one work") to "scope must name at least one work",
            Verdict.NotOnS5 to "not on S5 yet — this project has not been sent",
            Verdict.SignIn("Sign in with your ERP account") to "Sign in again (menu → Server): Sign in with your ERP account",
            Verdict.NoAnswer(null) to "No answer to the save — showing the stage as it is now",
            Verdict.Offline to "Offline — the quote stage moves only when S5 answers",
        )
        for ((v, t) in en) assertEquals(t, QuoteMove.text(Tx.EN, v), "$v")
        assertEquals("अंदाजपत्रक फक्त प्रोजेक्ट मॅनेजर मंजूर करतो", QuoteMove.text(Tx.MR, Verdict.PmOnly("designer", "pm@x")))
        assertEquals("मंजूर झाले — पुढचे बदल \"बदल विनंती\" मधूनच होतात; सेव्ह झाले नाही", QuoteMove.text(Tx.MR, Verdict.Final(n)))
        assertEquals("मंजूर झाले — स्कोप फक्त \"बदल विनंती\" मधूनच बदलतो; सेव्ह झाले नाही", QuoteMove.text(Tx.MR, Verdict.ScopeFinal(n)))
        assertEquals("ऑफलाइन — S5 उत्तर देईल तेव्हाच कोटेशनचा टप्पा बदलतो", QuoteMove.text(Tx.MR, Verdict.Offline))
        // S5's 400 words are data: shown as sent in either language
        assertEquals("scope must name at least one work", QuoteMove.text(Tx.MR, Verdict.Refused(400, "scope must name at least one work")))
        assertEquals("Approved — changes go through change requests; not saved", QuoteMove.localText(Tx.EN, Local.APPROVED_FINAL))
        assertEquals("Tick the works in the approved estimate before moving to Client approved", QuoteMove.localText(Tx.EN, Local.SCOPE_MISSING))
        // the refusal words are S5's own, byte for byte (asis.js at 9a51733)
        assertEquals("Approved — the scope changes only through change requests; not saved",
            (MiniJson.parse(S5Quote.REC_FINAL) as Map<*, *>)["error"])
    }
    @Test fun `a segment's tap moves to that segment's stage - the strip and the mover agree on the body S5 accepted (QM1)`() {
        // the strip's phase i stands for STAGES[i] (Phases.stageOf): a forward tap at Survey, base '', is S5Quote's first move
        assertEquals(Phases.Tap.MOVE, Phases.tap(0, 1))
        assertEquals(S5Quote.SENT_MOVED_FIRST, body(QuoteMove.stage(P, Cache(listed = true, stage = "Survey", readAt = "r"), Phases.stageOf(1))))
        // a step back asks first (Phases.ask), and the move it confirms is the body S5 accepted for the move back
        assertEquals(Phases.Tap.ASK, Phases.tap(3, 2))
        assertEquals(S5Quote.SENT_MOVED_BACK, body(QuoteMove.stage(P, at("Estimated", tEstimated), Phases.stageOf(2))))
        // the current segment is the line's stage, so the strip and the store never disagree on where the project is
        for ((i, s) in QuoteStage.STAGES.dropLast(1).withIndex()) {
            val line = QuoteStage.projectLine(at(s, tRevisions), false, emptyList(), Tx.EN, java.time.Instant.parse("2026-11-03T08:00:00Z"))
            assertEquals(i, Phases.phaseOf(line), s)
            assertEquals(s, Phases.stageOf(i))
        }
        // v61c.2: the strip offers no tap on a project S5 does not list, and the mover refuses it anyway, before any call
        assertTrue(Phases.segments(Tx.EN, QuoteStage.projectLine(null, true, emptyList(), Tx.EN, java.time.Instant.parse("2026-11-03T08:00:00Z"))).none { it.tap })
        assertEquals(Move.Refused(Local.NOT_ON_S5), QuoteMove.stage("", null, Phases.stageOf(1)))
    }

    @Test fun `a request that threw is offline only when it never reached S5 - a timeout or a reset once it went out is no answer, so the phone re-reads (QM1 review)`() {
        // never reached S5: no route to it -- nothing was sent, so nothing need be read
        for (e in listOf(java.net.UnknownHostException("stage"), java.net.ConnectException("Connection refused"),
                java.net.NoRouteToHostException("No route to host"), javax.net.ssl.SSLHandshakeException("handshake")))
            assertEquals(Verdict.Offline, QuoteMove.verdictOf(e), e.javaClass.simpleName)
        // once the request may have gone out S5 may have moved the stage: no answer, and the phone re-reads rather than say offline
        // (a read timeout and a reset after the body was written are IOExceptions too, as a connect timeout is: told apart from
        // "never sent" by their type, not by one catch arm)
        for (e in listOf(java.net.SocketTimeoutException("timeout"), java.net.SocketException("Connection reset"), java.io.EOFException(),
                java.io.InterruptedIOException("timeout"), java.io.IOException("unexpected end of stream"), javax.net.ssl.SSLException("closed")))
            assertEquals(Verdict.NoAnswer(null), QuoteMove.verdictOf(e), e.javaClass.simpleName)
        // the words follow: offline says so; no answer says the stage is shown as it is now, which the re-read makes true
        assertEquals("Offline — the quote stage moves only when S5 answers", QuoteMove.text(Tx.EN, QuoteMove.verdictOf(java.net.UnknownHostException("x"))))
        assertEquals("No answer to the save — showing the stage as it is now", QuoteMove.text(Tx.EN, QuoteMove.verdictOf(java.net.SocketTimeoutException("x"))))
        // neither names a stage: the store keeps its copy, and with it no base the phone could resend
        assertNull(QuoteMove.copyOf(QuoteMove.verdictOf(java.net.SocketTimeoutException("x")), "r"))
        assertNull(QuoteMove.copyOf(QuoteMove.verdictOf(java.net.UnknownHostException("x")), "r"))
    }

    @Test fun `a phone with nothing to send with refuses before any call, in its own sign-in words (QV)`() {
        // S5Auth.fresh is null: no S5 session and no ERP key. Nothing is sent, and the words are the phone's sign-in
        // line, not a 401 the phone could not read (QuoteMove.Verdict.SignIn carries S5's words, which never came)
        assertEquals("Sign in first — menu → Server.", QuoteMove.localText(Tx.EN, Local.NOT_SIGNED_IN))
        assertEquals("आधी साइन इन करा — मेनू → सर्व्हर.", QuoteMove.localText(Tx.MR, Local.NOT_SIGNED_IN))
        // every local refusal has words in both languages
        for (why in Local.entries) { assertTrue(QuoteMove.localText(Tx.EN, why).isNotBlank(), "$why"); assertTrue(QuoteMove.localText(Tx.MR, why).isNotBlank(), "$why") }
    }
}
