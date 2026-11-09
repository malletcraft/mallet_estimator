package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.CrKind
import com.malletcrafts.interiorestimate.pano.Album.CrOutcome
import com.malletcrafts.interiorestimate.pano.Album.Op
import com.malletcrafts.interiorestimate.pano.Album.Outbox
import com.malletcrafts.interiorestimate.pano.Album.Photo
import com.malletcrafts.interiorestimate.pano.Album.Verdict
import com.malletcrafts.interiorestimate.pano.Album.View
import com.malletcrafts.interiorestimate.pano.Album.Work
import com.malletcrafts.interiorestimate.pano.AlbumQueue.Step
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * S5's validators (functions/_lib/asis.js @ 9a51733: validLogEntry, validPhoto, validReview, validPhotoWork), ported
 * line for line onto a parsed body with JavaScript's own tests -- `Number.isInteger`, `typeof === 'string'`, a Set of
 * numbers that never holds a number's text, `'k' in b` -- and S5's own words. Null = accepted. The port is pinned to
 * S5 by the words of the 400s S5 really sent ([S5Album]).
 */
private object S5Valid {
    private val WORKS = Album.WORK_IDS.map { it.toLong() }.toSet()
    private val ID = Regex("^ALB-[0-9a-f]{12}$")
    private fun isInteger(v: Any?) = v is Long || (v is Double && v.isFinite() && v == Math.floor(v))
    private fun num(v: Any?): Long? = if (isInteger(v)) (v as Number).toLong() else null
    private fun workId(v: Any?) = num(v)?.let { it in WORKS } == true
    private fun validWork(v: Any?) = v == null || num(v) == 0L || workId(v)
    /** shortText: '' when absent or null, null when not text or over [max] (trimmed first). */
    private fun short(b: Map<*, *>, k: String, max: Int): String? { val v = b[k] ?: return ""; if (v !is String) return null; val t = v.trim(); return if (t.length > max) null else t }
    private const val WORK_LIST = "10, 20, 30, 40, 50, 60, 70, 80, 85, 90, 95, 100, 110, 120, 130, 140"

    fun logEntry(b: Any?): String? {
        if (b !is Map<*, *> || num(b["v"]) != 1L) return "Send {v: 1, id, kind, work, value, prev, note, base}"
        val id = b["id"]
        if (id !is String || !ID.matches(id)) return "id must be ALB- and 12 hex digits, minted by the writer"
        if (b["kind"] !in listOf("state", "cr_raise", "cr_clear")) return "kind must be one of state, cr_raise, cr_clear"
        if (!workId(b["work"])) return "work must be one of $WORK_LIST"
        if (!isInteger(b["base"]) || num(b["base"])!! < 0) return "base must be the seq last seen for this work (0 when none)"
        val note = short(b, "note", 500) ?: return "note must be text of at most 500 characters"
        short(b, "via", 40) ?: return "via must be text of at most 40 characters"
        val clientAt = short(b, "client_at", 40) ?: return "client_at must be text of at most 40 characters"
        when (b["kind"]) {
            "state" -> {
                if (b["value"] !in Album.STATES) return "value must be one of " + Album.STATES.joinToString(", ")
                if (b["prev"] !in Album.STATES) return "prev must be the state the work had: one of " + Album.STATES.joinToString(", ")
                if (b["value"] == b["prev"]) return "The work is already ${b["value"]}"
                if (b["prev"] == "Done" && note.isEmpty()) return "A reason is needed to change a Done work"
            }
            "cr_raise" -> if (b["value"] !in listOf("add", "remove", "change")) return "value must be the change request's kind: add, remove, change"
            else -> {
                if (b["value"] !in listOf("approved", "declined", "withdrawn")) return "value must be the answer: approved, declined, withdrawn"
                if (b["value"] != "withdrawn" && !Album.isDay(clientAt)) return "client_at must be the date the client answered, as YYYY-MM-DD"
            }
        }
        return null
    }

    fun photo(b: Any?): String? {
        if (b !is Map<*, *> || num(b["v"]) != 1L) return "Send {v: 1, work, room, caption, kind}"
        if (!b.containsKey("work") || !validWork(b["work"])) return "work must be 0 (the quote journey), null (not filed) or one of $WORK_LIST"
        short(b, "room", 140) ?: return "room must be text of at most 140 characters"
        short(b, "caption", 500) ?: return "caption must be text of at most 500 characters"
        val kind = if (b.containsKey("kind")) b["kind"] else "photo"
        if (kind != "photo" && kind != "360") return "kind must be photo or 360"
        short(b, "via", 40) ?: return "via must be text of at most 40 characters"
        short(b, "client_at", 40) ?: return "client_at must be text of at most 40 characters"
        return null
    }

    fun review(b: Any?): String? {
        if (b !is Map<*, *>) return "Send {id, state: accepted or internal, base}"
        val id = b["id"]
        if (id !is String || !ID.matches(id)) return "id must be ALB- and 12 hex digits, minted by the writer"
        if (b["state"] != "accepted" && b["state"] != "internal") return "state must be accepted or internal"
        val base = b["base"]
        if (base !is String || base.length > 40) return "base must be the review_at last seen, or \"\" when the photo was never reviewed"
        short(b, "via", 40) ?: return "via must be text of at most 40 characters"
        short(b, "client_at", 40) ?: return "client_at must be text of at most 40 characters"
        return null
    }

    fun photoWork(b: Any?): String? {
        if (b !is Map<*, *> || !b.containsKey("work")) return "Send {work, base}: work the one of the sixteen works the photo goes to, base its work as last seen"
        if (!workId(b["work"])) return "work must be one of $WORK_LIST: a re-filed photo is a work photo, never the quote journey (0) or unfiled (null)"
        if (!b.containsKey("base") || !validWork(b["base"])) return "base must be the photo's work as last seen: 0, null or one of $WORK_LIST"
        return null
    }
}

/**
 * The album outbox on the wire (port 61c slice AQ): every queued op becomes exactly the S5 call S5 QUOTE 9a51733
 * accepted ([S5Album]'s SENT_* bodies), and every S5 answer moves the queue as "Offline and conflicts" says. The
 * names are invented (mallet_estimator is public); every expected value is written out by hand.
 */
class AlbumQueueTest {

    private val P = "PROJ-DEMO"
    private fun tree(s: String): Any? = MiniJson.parse(s)

    /** [v] as org.json writes `JSONObject(fields)`: a null is JSON null, a number its digits, a string quoted. */
    private fun json(v: Any?): String = when (v) {
        null -> "null"
        is String -> buildString {
            append('"')
            for (c in v) when {
                c == '"' -> append("\\\""); c == '\\' -> append("\\\\"); c == '\n' -> append("\\n"); c == '\r' -> append("\\r"); c == '\t' -> append("\\t")
                c < ' ' -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
            append('"')
        }
        is Number, is Boolean -> v.toString()
        is Map<*, *> -> v.entries.joinToString(",", "{", "}") { json(it.key as String) + ":" + json(it.value) }
        is List<*> -> v.joinToString(",", "[", "]") { json(it) }
        else -> fail("not a JSON value: $v (${v::class})")
    }

    private fun req(op: Op): AlbumQueue.Request = assertNotNull(AlbumQueue.requestOf(op, P))
    private fun body(op: Op): String = json(req(op).fields)
    private fun reply(s: String?, op: Op) = AlbumWire.reply(MiniRead, s?.let { tree(it) }, op)

    // the ops of the harness run, built by Album's own builders (each id fixed to the one S5 saw)
    private val raiseRow = Album.Entry(8, "ALB-000000000007", Album.KIND_CR_RAISE, 90, "add", note = "factory-made units added")
    private val at6 = View(Album.PM, works = listOf(Work(10, "In progress", at = "2026-11-03T05:16:04.187Z", seq = 6)))
    private val at8 = View(Album.PM, works = listOf(Work(90, seq = 8)), log = listOf(raiseRow))
    private val sState = Album.stateOp(null, 10, "Not started", "In progress", "").copy(id = "ALB-000000000001")
    private val sDone = Album.stateOp(at6, 10, "In progress", "Done", " debris out ").copy(id = "ALB-000000000006")
    private val sRaise = Album.crRaiseOp(null, 90, CrKind.ADD, "factory-made units added").copy(id = "ALB-000000000007")
    private val sAnswer = Album.crAnswerOp(at8, at8.log, 90, CrOutcome.APPROVED, "2026-11-02", "by phone").copy(id = "ALB-000000000008")
    private val sChange = Album.crRaiseOp(null, 50, CrKind.CHANGE, "second coat").copy(id = "ALB-000000000009")
    private val pStaff = Album.photoOp(90, "Kitchen", "units in").copy(id = "ALB-000000000014", photo = "ALB-000000000014")
    private val pVendor = Album.photoOp(50, null, "membrane, second coat").copy(id = "ALB-00000000001e", photo = "ALB-00000000001e")
    private val vendorPhoto = Photo("ALB-00000000001e", 50, "2026-11-03T05:16:04.215Z", Album.PARTY_VENDOR, vendor = "Demo Waterproofing Co.", bytes = 10)
    private val rAccept = Album.reviewOp(vendorPhoto, Album.REVIEW_ACCEPTED).copy(id = "ALB-000000000020")
    private val staffPhoto = Photo("ALB-000000000014", 90, "2026-11-03T05:16:04.209Z", by = "pm@x", bytes = 10)
    private val fRefile = Album.refileOp(staffPhoto, 95)

    @Test fun `each op is exactly the body S5 accepted - built by Album's builders, serialised as org-json writes it`() {
        assertEquals(S5Album.SENT_STATE, body(sState))
        assertEquals(S5Album.SENT_STATE_DONE, body(sDone), "the remark trimmed, the base 6 from the mirror")
        assertEquals(S5Album.SENT_RAISE_ADD, body(sRaise))
        assertEquals(S5Album.SENT_ANSWER, body(sAnswer), "prev (the kind it answers) is S5's to store, never sent")
        assertEquals("add", sAnswer.prev, "the op keeps it, for its provisional row")
        assertEquals(S5Album.SENT_RAISE_CHANGE, body(sChange))
        assertEquals(S5Album.SENT_RECORD, body(pStaff))
        assertEquals(S5Album.SENT_RECORD_VENDOR, body(pVendor), "a room left out is JSON null, as S5 stores it")
        assertEquals(S5Album.SENT_REVIEW, body(rAccept), "a review's base is the review_at STRING, '' for never")
        assertEquals(S5Album.SENT_REFILE, body(fRefile))
        assertEquals(S5Album.SENT_REFILE_NULL_BASE, body(Album.refileOp(staffPhoto.copy(work = null), 100)), "an unfiled photo's base is JSON null")
    }

    @Test fun `each op is the right route - method, path, content type, and the JPEG from the shots folder`() {
        val enc: (String) -> String = { s -> s.replace("%", "%25").replace(" ", "%20").replace("/", "%2F") }
        assertEquals("POST /api/asis/PROJ-DEMO/album/log application/json", req(sState).let { "${it.method} ${it.path(enc)} ${it.contentType}" })
        assertEquals("PUT /api/asis/PROJ-DEMO/album/photos/ALB-000000000014 application/json", req(pStaff).let { "${it.method} ${it.path(enc)} ${it.contentType}" })
        val img = req(pStaff.copy(recorded = true))
        assertEquals("PUT /api/asis/PROJ-DEMO/album/photos/ALB-000000000014/img image/jpeg", "${img.method} ${img.path(enc)} ${img.contentType}")
        assertEquals("album/shots/ALB-000000000014.jpg", img.file)
        assertEquals(emptyMap(), img.fields)
        assertEquals("PUT /api/asis/PROJ-DEMO/album/photos/ALB-00000000001e/review", req(rAccept).let { "${it.method} ${it.path(enc)}" })
        assertEquals("PUT /api/asis/PROJ-DEMO/album/photos/ALB-000000000014/work", req(fRefile).let { "${it.method} ${it.path(enc)}" })
        // the project is one segment, encoded by the caller (:app's AsIsUpload.seg), never split by a slash in its id
        assertEquals(listOf("Demo flat/1", "album", "log"), AlbumQueue.requestOf(sState, "Demo flat/1")!!.segments)
        assertEquals("/api/asis/Demo%20flat%2F1/album/log", AlbumQueue.requestOf(sState, "Demo flat/1")!!.path(enc))
        for (op in listOf(sState, sDone, sRaise, sAnswer, pStaff, rAccept, fRefile)) assertNull(req(op).file)
    }

    @Test fun `every field has S5's JSON type - a seq and a work are numbers, a review base text, a null is a null that is SENT (critic S2)`() {
        val log = req(sDone).fields
        assertEquals(listOf("v", "id", "kind", "work", "value", "prev", "note", "base", "client_at", "via"), log.keys.toList())
        assertTrue(log["v"] is Int && log["work"] is Int && log["base"] is Long, log.toString())
        assertEquals(6L, log["base"])
        assertTrue(log["id"] is String && log["kind"] is String && log["value"] is String && log["prev"] is String && log["note"] is String && log["via"] is String)
        assertTrue(log.containsKey("client_at") && log["client_at"] == null)
        assertTrue(req(sRaise).fields.let { it.containsKey("prev") && it["prev"] == null })
        val rec = req(pStaff).fields
        assertEquals(listOf("v", "work", "room", "caption", "kind", "client_at", "via"), rec.keys.toList())
        assertTrue(rec["v"] is Int && rec["work"] is Int && rec["room"] is String && rec["caption"] is String && rec["kind"] is String)
        val rv = req(rAccept).fields
        assertEquals(listOf("id", "state", "base", "via"), rv.keys.toList())
        assertTrue(rv["base"] is String)
        val rf = req(fRefile).fields
        assertEquals(listOf("work", "base"), rf.keys.toList())
        assertTrue(rf["work"] is Int && rf["base"] is Int)
        assertTrue(req(Album.refileOp(staffPhoto.copy(work = null), 95)).fields.let { it.containsKey("base") && it["base"] == null })
        // through the wire and back, S5's validators accept every one of them
        for (op in listOf(sState, sDone, sRaise, sAnswer, sChange)) assertNull(S5Valid.logEntry(tree(body(op))), body(op))
        for (op in listOf(pStaff, pVendor)) assertNull(S5Valid.photo(tree(body(op))), body(op))
        assertNull(S5Valid.review(tree(body(rAccept))))
        assertNull(S5Valid.photoWork(tree(body(fRefile))))
        assertNull(S5Valid.photoWork(tree(body(Album.refileOp(staffPhoto.copy(work = 0), 95)))))
    }

    @Test fun `the ported validators say what S5 said - a base sent as text is S5's 400, which would drop every state write`() {
        // the words of S5's real answers ([S5Album]) are the port's
        assertEquals(MiniRead.field(tree(S5Album.STATE_BASE_TEXT), "error"), S5Valid.logEntry(tree(S5Album.SENT_STATE.replace("\"base\":0", "\"base\":\"0\""))))
        assertEquals(MiniRead.field(tree(S5Album.REFILE_NO_BASE), "error"), S5Valid.photoWork(tree("""{"work":100}""")))
        assertEquals(MiniRead.field(tree(S5Album.RECORD_CAPTION), "error"), S5Valid.photo(tree(S5Album.SENT_RECORD.replace("units in", "x".repeat(501)))))
        // and the shapes a careless serialiser would produce are refused
        assertEquals("base must be the photo's work as last seen: 0, null or one of 10, 20, 30, 40, 50, 60, 70, 80, 85, 90, 95, 100, 110, 120, 130, 140",
            S5Valid.photoWork(tree("""{"work":95,"base":"90"}""")))
        assertEquals("base must be the review_at last seen, or \"\" when the photo was never reviewed", S5Valid.review(tree("""{"id":"ALB-000000000020","state":"accepted","base":0}""")))
        assertEquals("work must be one of 10, 20, 30, 40, 50, 60, 70, 80, 85, 90, 95, 100, 110, 120, 130, 140", S5Valid.logEntry(tree(S5Album.SENT_STATE.replace("\"work\":10", "\"work\":\"10\""))))
        assertEquals("client_at must be the date the client answered, as YYYY-MM-DD", S5Valid.logEntry(tree(S5Album.SENT_ANSWER.replace("\"2026-11-02\"", "null"))))
    }

    @Test fun `every text is held to S5's cap on the trimmed text - an op an older build queued is no 400 either`() {
        val long = Op("ALB-000000000031", Album.KIND_STATE, 10, "4", "Done", "In progress", note = "  " + "n".repeat(600))
        assertEquals("n".repeat(500), req(long).fields["note"])
        assertNull(S5Valid.logEntry(tree(body(long))))
        val ph = Op("ALB-000000000032", Album.OP_PHOTO, 90, "", photo = "ALB-000000000032", room = " " + "r".repeat(200), caption = "c".repeat(700) + " ")
        assertEquals(140, (req(ph).fields["room"] as String).length)
        assertEquals(500, (req(ph).fields["caption"] as String).length)
        assertNull(S5Valid.photo(tree(body(ph))))
        assertNull(req(ph.copy(room = "   ")).fields["room"], "a blank room is none")
        // client_at: a day with a time after it keeps its day past the cap; another long text is no answer at all
        val clr = Op("ALB-000000000033", Album.KIND_CR_CLEAR, 90, "8", "declined", "add", clientAt = " 2026-11-02T10:00:00.000+05:30 and some more ")
        assertEquals("2026-11-02", req(clr).fields["client_at"])
        assertEquals("2026-11-02T10:00:00+05:30", req(clr.copy(clientAt = "2026-11-02T10:00:00+05:30")).fields["client_at"], "within the cap: as sent")
        assertNull(req(clr.copy(value = "withdrawn", clientAt = "w".repeat(41))).fields["client_at"])
        assertNull(req(clr.copy(clientAt = "  ")).fields["client_at"])
        // a base no seq can be (an unreadable or negative one) goes as 0: S5 then answers a 409 with what it has, never a 400
        assertEquals(listOf(0L, 0L, 12L), listOf("x", "-3", " 12 ").map { req(sState.copy(base = it)).fields["base"] })
    }

    @Test fun `next is the head of the queue and its step - a photo's record, then its JPEG`() {
        assertNull(AlbumQueue.next(Outbox()))
        assertEquals(AlbumQueue.Next(sState, Step.LOG), AlbumQueue.next(Outbox(queue = listOf(sState, pStaff))))
        assertEquals(Step.RECORD, AlbumQueue.next(Outbox(queue = listOf(pStaff)))!!.step)
        assertEquals(Step.IMG, AlbumQueue.next(Outbox(queue = listOf(pStaff.copy(recorded = true))))!!.step)
        assertEquals(listOf(Step.LOG, Step.LOG, Step.LOG, Step.REVIEW, Step.REFILE), listOf(sState, sRaise, sAnswer, rAccept, fRefile).map { AlbumQueue.stepOf(it) })
    }

    @Test fun `a photo whose record answered is followed by its JPEG - the op stays at the head until the bytes land`() {
        val mirror = View(Album.PM, exec = true)
        var o = Outbox(mirror, listOf(pStaff, sChange))
        val a1 = AlbumQueue.apply(o, pStaff, 200, reply(S5Album.RECORD, pStaff))
        assertEquals(Verdict.POSTED, a1.verdict)
        assertTrue(a1.goOn)
        o = a1.outbox
        assertEquals(listOf(pStaff.copy(recorded = true), sChange), o.queue)
        assertEquals(0L, o.mirror!!.photos.single().bytes, "the row is in the mirror; its JPEG is not on S5 yet")
        val n = AlbumQueue.next(o)!!
        assertEquals(Step.IMG, n.step)
        val a2 = AlbumQueue.apply(o, n.op, 200, reply(S5Album.IMG, n.op))
        assertEquals(listOf(sChange), a2.outbox.queue)
        assertEquals(10L, a2.outbox.mirror!!.photos.single().bytes)
        assertEquals(emptyList(), a2.outbox.dropped)
        // the record's replay (a lost answer) moves on to the JPEG the same way
        assertEquals(listOf(pStaff.copy(recorded = true)), AlbumQueue.apply(Outbox(mirror, listOf(pStaff)), pStaff, 200, reply(S5Album.RECORD_REPLAY, pStaff)).outbox.queue)
    }

    @Test fun `a JPEG answered written false is the one S5 already holds - landed - and a record whose row has bytes needs no JPEG`() {
        val img = pStaff.copy(recorded = true)
        val a = AlbumQueue.apply(Outbox(View(Album.PM), listOf(img)), img, 200, reply(S5Album.IMG_AGAIN, img))
        assertEquals(Verdict.REPLAY, a.verdict)
        assertEquals(emptyList(), a.outbox.queue)
        assertEquals(emptyList(), a.outbox.dropped)
        // the replay of a record whose JPEG landed on an earlier pass (bytes 10): nothing left to send
        val b = AlbumQueue.apply(Outbox(View(Album.PM), listOf(pStaff, sState)), pStaff, 200, reply(S5Album.RECORD_LANDED, pStaff))
        assertEquals(listOf(sState), b.outbox.queue)
        assertEquals(10L, b.outbox.mirror!!.photos.single().bytes)
    }

    @Test fun `401 keeps the op for a sign-in and a resend - 404, 405, 5xx and no network stop the drain, the queue as it was`() {
        val o = Outbox(View(Album.PM), listOf(sState, sRaise))
        val r = AlbumQueue.apply(o, sState, 401, reply("""{"error":"Sign in again"}""", sState))
        assertEquals(Verdict.RELOGIN, r.verdict)
        assertFalse(r.goOn)
        assertEquals(o, r.outbox)
        val img = pStaff.copy(recorded = true)
        for ((code, body) in listOf(405 to """{"error":"Method not allowed"}""", 500 to null, 502 to "<html>", 503 to """{"error":"Photo storage not bound for this environment"}""", 0 to null, -1 to null)) {
            val ob = Outbox(View(Album.PM), listOf(img, sState))
            val s = AlbumQueue.apply(ob, img, code, reply(body?.takeIf { it.startsWith("{") }, img))
            assertEquals(Verdict.STOP, s.verdict, "$code")
            assertFalse(s.goOn, "$code")
            assertEquals(ob, s.outbox, "$code")
        }
        // a 404 stops too, the queue as it was -- but for a JPEG, whose 404 says S5 has no record: see the test below
        for (op in listOf(sState, pStaff, rAccept, fRefile)) {
            val ob = Outbox(View(Album.PM, photos = listOf(vendorPhoto, staffPhoto)), listOf(op, sRaise))
            val s = AlbumQueue.apply(ob, op, 404, reply("""{"error":"Not found"}""", op))
            assertEquals(Verdict.STOP, s.verdict, op.kind)
            assertEquals(ob, s.outbox, op.kind)
        }
    }

    @Test fun `a 200 that is not S5's handler moves nothing - every 200 S5 sends on these routes says written true or false`() {
        // S5 9a51733: the log, record, img, review and re-file 200s all carry a boolean `written`. A 200 without one is a
        // proxy's page, a redirect followed to HTML or a cut body: read as POSTED it lost the write -- and at a photo's
        // record it marked the record answered with no row behind it, so the JPEG 404'd for ever (the AQ review)
        val img = pStaff.copy(recorded = true)
        for (op in listOf(sState, sRaise, pStaff, img, rAccept, fRefile)) for (body in listOf(null, "[1]", """{"hello":1}""", """{"ok":true}""")) {
            val ob = Outbox(View(Album.PM, photos = listOf(vendorPhoto, staffPhoto)), listOf(op, sChange))
            val s = AlbumQueue.apply(ob, op, 200, reply(body, op))
            assertEquals(Verdict.STOP, s.verdict, "${op.kind} $body")
            assertFalse(s.goOn)
            assertEquals(ob, s.outbox, "${op.kind} $body: the op stays, unchanged, for the next pass")
        }
    }

    @Test fun `a JPEG answered 404 sends its record again on the next pass - S5 replays it for its uploader, then the JPEG lands`() {
        // the record was taken as answered but S5 has no row (an answer that was not S5's, on a build before this fix)
        val o = drain(Outbox(View(Album.PM, exec = true), listOf(pStaff.copy(recorded = true), sChange)),
            Exchange("PUT /api/asis/PROJ-DEMO/album/photos/ALB-000000000014/img", "{}", 404, S5Album.IMG_NO_RECORD))
        assertEquals(listOf(pStaff, sChange), o.queue, "recorded cleared: the record goes first again")
        assertEquals(emptyList(), o.dropped)
        val o2 = drain(o,
            Exchange("PUT /api/asis/PROJ-DEMO/album/photos/ALB-000000000014", S5Album.SENT_RECORD, 200, S5Album.RECORD_REPLAY),
            Exchange("PUT /api/asis/PROJ-DEMO/album/photos/ALB-000000000014/img", "{}", 200, S5Album.IMG),
            Exchange(LOG, S5Album.SENT_RAISE_CHANGE, 200, S5Album.RAISE_CHANGE))
        assertEquals(emptyList(), o2.queue)
        assertEquals(10L, o2.mirror!!.photos.single().bytes)
    }

    // ---- the drain as K4's job will run it: next, requestOf, send, AlbumWire.reply, apply, until empty, STOP or RELOGIN ----

    /** One S5 exchange the fake expects: the request it must receive, and what S5 really answered to it. */
    private data class Exchange(val line: String, val body: String?, val code: Int, val answer: String?)

    /** Drains [o] against a fake S5 that answers [script] in order and fails on any request it was not told to expect. */
    private fun drain(o: Outbox, vararg script: Exchange): Outbox {
        var out = o
        val left = ArrayDeque(script.toList())
        repeat(50) {
            val n = AlbumQueue.next(out) ?: return out.also { assertTrue(left.isEmpty(), "unsent: $left") }
            val rq = AlbumQueue.requestOf(n.op, P)
            val ex = left.removeFirstOrNull() ?: fail("S5 was not expecting ${rq?.method} ${rq?.path { it }}")
            assertEquals(ex.line, "${rq?.method} ${rq?.path { it }}")
            if (ex.body != null) assertEquals(ex.body, json(rq!!.fields))
            val a = AlbumQueue.apply(out, n.op, ex.code, reply(ex.answer, n.op))
            out = a.outbox
            if (!a.goOn) return out.also { assertTrue(left.isEmpty(), "unsent: $left") }
        }
        fail("the drain did not end")
    }

    private val LOG = "POST /api/asis/PROJ-DEMO/album/log"

    @Test fun `an offline chain drains as S5 really took it - each op rebased onto S5's last row, each body the one S5 accepted`() {
        // queued with no signal, every log op against the mirror's base 0: the rebase must produce S5's 6 and 8
        val q = listOf(sState, sDone.copy(base = "0"), sRaise, sAnswer.copy(base = "0"), pStaff)
        val o = drain(Outbox(View(Album.PM, exec = true), q),
            Exchange(LOG, S5Album.SENT_STATE, 200, S5Album.STATE_POSTED),
            Exchange(LOG, S5Album.SENT_STATE_DONE, 200, S5Album.STATE_DONE),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 200, S5Album.RAISE_ADD),
            Exchange(LOG, S5Album.SENT_ANSWER, 200, S5Album.ANSWER_APPROVED),
            Exchange("PUT /api/asis/PROJ-DEMO/album/photos/ALB-000000000014", S5Album.SENT_RECORD, 200, S5Album.RECORD),
            Exchange("PUT /api/asis/PROJ-DEMO/album/photos/ALB-000000000014/img", "{}", 200, S5Album.IMG))
        assertEquals(emptyList(), o.queue)
        assertEquals(emptyList(), o.dropped)
        val m = o.mirror!!
        assertEquals(listOf(6L, 7L, 8L, 9L), m.log.map { it.seq })
        assertEquals("Done", Album.stateOf(m.log, 10))
        assertEquals(listOf(10, 20, 30, 40, 50, 60, 70, 90, 100, 110, 120, 130, 140), m.scope, "the approved add's answer amended the mirror's scope")
        assertEquals(10L, m.photos.single().bytes)
    }

    @Test fun `a 409 mid-queue - S5 wins - drops the work's later ops and keeps every other work's, which still go`() {
        val pOn10 = Album.photoOp(10, "Kitchen", "debris", random = Random(3))
        val sHold = Album.stateOp(null, 10, "Done", "On hold", "", Random(4))
        val q = listOf(sState, sDone.copy(base = "0"), sRaise, pOn10, sHold, sChange)
        val o = drain(Outbox(View(Album.PM, exec = true), q),
            Exchange(LOG, S5Album.SENT_STATE, 200, S5Album.STATE_POSTED),
            // somebody moved 10 meanwhile: S5's own 409, its words and its rows
            Exchange(LOG, S5Album.SENT_STATE_DONE, 409, S5Album.STATE_STALE),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 200, S5Album.RAISE_ADD),
            Exchange(LOG, S5Album.SENT_RAISE_CHANGE, 200, S5Album.RAISE_CHANGE))
        assertEquals(emptyList(), o.queue)
        assertEquals(listOf(sDone.id, pOn10.id, sHold.id), o.dropped.map { it.id }, "the photo on 10 and the later state on 10 rode on the lost write")
        assertTrue(o.dropped.all { it.error == "Changed meanwhile — In progress by pm@x; not saved" })
        assertEquals("In progress", Album.stateOf(o.mirror!!.log, 10), "the mirror took S5's state")
    }

    @Test fun `a 409 locked drops the work's later ops in the banner's words, and a photo record's locked 409 the same`() {
        val o = drain(Outbox(View(Album.PM), listOf(sState, Album.photoOp(10, null, "x", random = Random(5)), sRaise)),
            Exchange(LOG, S5Album.SENT_STATE, 409, S5Album.STATE_LOCKED),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 200, S5Album.RAISE_ADD))
        assertEquals(2, o.dropped.size)
        assertTrue(o.dropped.all { it.error == "Work starts once the client approves the estimate" && it.work == 10 })
        val ph = Album.photoOp(10, "Kitchen", "units in", random = Random(6))
        val a = AlbumQueue.apply(Outbox(View(Album.PM), listOf(ph, sChange)), ph, 409, reply(S5Album.RECORD_LOCKED, ph))
        assertEquals(Verdict.LOCKED, a.verdict)
        assertEquals(listOf(sChange), a.outbox.queue)
        assertEquals(View(Album.PM), a.outbox.mirror, "the record's locked 409 echoes nothing")
    }

    @Test fun `refused for good - that op alone - and the next op that rode on it takes back S5's base`() {
        val sNext = Album.stateOp(null, 10, "In progress", "Done", "", Random(7)).copy(base = "99")
        val o = drain(Outbox(View(Album.PM, exec = true), listOf(sState, sNext, sRaise)),
            Exchange(LOG, S5Album.SENT_STATE, 403, S5Album.STATE_DESIGNER),
            Exchange(LOG, """{"v":1,"id":"${sNext.id}","kind":"state","work":10,"value":"Done","prev":"Not started","note":"","base":0,"client_at":null,"via":"phone"}""", 400, S5Album.STATE_BASE_TEXT),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 200, S5Album.RAISE_ADD))
        assertEquals(listOf(sState.id, sNext.id), o.dropped.map { it.id })
        assertEquals(listOf("Only the Project Manager sets a work's state", "base must be the seq last seen for this work (0 when none)"), o.dropped.map { it.error })
        assertEquals(emptyList(), o.queue)
    }

    @Test fun `the S5-P re-file - a posted re-file rebases the next one of the same photo, a 403 refuses that op alone, a 409 drops the photo's later re-files`() {
        val mirror = View(Album.PM, exec = true, photos = listOf(staffPhoto))
        val again = Album.refileOp(staffPhoto, 100, Random(8))           // built off the mirror: base 90, stale once the first lands
        val sOn100 = Album.stateOp(null, 100, "Not started", "In progress", "", Random(9))
        var o = AlbumQueue.apply(Outbox(mirror, listOf(fRefile, again, sOn100)), fRefile, 200, reply(S5Album.REFILE, fRefile)).outbox
        assertEquals(listOf(again.copy(base = "95"), sOn100), o.queue, "S5 now files it at 95: the next re-file of it says so")
        assertEquals(95, o.mirror!!.photos.single().work)
        assertEquals("""{"work":100,"base":95}""", body(o.queue[0]))
        // the replay (a lost answer) is the same
        assertEquals(o.queue, AlbumQueue.apply(Outbox(mirror, listOf(fRefile, again, sOn100)), fRefile, 200, reply(S5Album.REFILE_REPLAY, fRefile)).outbox.queue)
        // another login's 403: that op alone, the state on 100 still goes, and the mirror takes the photo as S5 has it
        val next = o.queue[0]
        val r = AlbumQueue.apply(o, next, 403, reply(S5Album.REFILE_WHO, next))
        assertEquals(Verdict.REFUSED, r.verdict)
        assertTrue(r.goOn)
        assertEquals(listOf(sOn100), r.outbox.queue)
        assertEquals(listOf(next.copy(error = "Only the login that uploaded the photo re-files it")), r.outbox.dropped)
        assertEquals(95, r.outbox.mirror!!.photos.single().work)
        // a vendor's photo: 403 too, refused for good with the photo -- never a 409
        val v = Album.refileOp(vendorPhoto, 95, Random(10))
        val rv = AlbumQueue.apply(Outbox(View(Album.PM, photos = listOf(vendorPhoto)), listOf(v, sOn100)), v, 403, reply(S5Album.REFILE_VENDOR, v))
        assertEquals(listOf(sOn100), rv.outbox.queue)
        assertEquals("accepted", rv.outbox.mirror!!.photos.single().review)
        // a refused re-file hands its base to the next re-file of the same photo (S5 still files it there)
        val r2 = Album.refileOp(staffPhoto, 110, Random(11)).copy(base = "70")
        assertEquals(listOf(r2.copy(base = "90")), Album.afterRefused(Outbox(mirror, listOf(fRefile, r2)), fRefile, "x").queue)
        // re-filed meanwhile: S5 wins over the re-file -- and over nothing else. A re-file moves no works[].seq (S5-P),
        // so the state on 100 S5 still takes goes on (the AQ review: it was dropped in a re-file's words)
        val c = AlbumQueue.apply(Outbox(mirror, listOf(again, sOn100, sChange)), again, 409, reply(S5Album.REFILE_STALE, again))
        assertEquals(Verdict.CONFLICT, c.verdict)
        assertEquals(listOf(sOn100, sChange), c.outbox.queue)
        assertEquals(listOf(again.id), c.outbox.dropped.map { it.id })
        assertEquals(95, c.outbox.mirror!!.photos.single().work)
        // the lock: the banner's words, the photo as stored -- by the photo too: the lock is the project's, so the
        // state on 100 meets its own locked 409 rather than leave in a re-file's
        val lock = AlbumQueue.apply(Outbox(mirror, listOf(again, sOn100)), again, 409, reply(S5Album.REFILE_LOCKED, again))
        assertEquals(Verdict.LOCKED, lock.verdict)
        assertEquals("Work starts once the client approves the estimate", lock.outbox.dropped.single().error)
        assertEquals(listOf(sOn100), lock.outbox.queue)
        // a re-file writes no log row: it moves no base
        assertNull(AlbumWire.reply(MiniRead, tree(S5Album.REFILE), fRefile).answer.entry)
        assertEquals(0L, Album.baseOf(c.outbox.mirror, 95))
    }

    // ---- the AQ review: a re-file rides on its PHOTO, and a photo's dependants leave with it ----

    private fun refileLine(id: String) = "PUT /api/asis/PROJ-DEMO/album/photos/$id/work"

    @Test fun `a photo op dropped by its work's 409 takes its re-file with it - S5 has no row, so the re-file 404'd at the head for ever`() {
        // queued offline: Done on 10, a staff photo into 10, and from that photo's provisional tile its re-file into 95
        val pOn10 = Album.photoOp(10, "Kitchen", "debris", random = Random(30))
        val tile = Album.provisionalPhoto(pOn10, Album.PM, "pm@x", "P M", "2026-11-03T05:20:00.000Z")
        val fTile = Album.refileOp(tile, 95, Random(31))
        assertEquals(95, fTile.work, "filed under the work it goes to, so the work-10 drop alone never reached it")
        // 10 moved on S5 meanwhile: the state's 409 drops the photo on 10, and the photo's re-file with it, in S5's words
        val o = drain(Outbox(View(Album.PM, exec = true), listOf(sDone, pOn10, fTile, sChange)),
            Exchange(LOG, S5Album.SENT_STATE_DONE, 409, S5Album.STATE_STALE),
            Exchange(LOG, S5Album.SENT_RAISE_CHANGE, 200, S5Album.RAISE_CHANGE))
        assertEquals(emptyList(), o.queue)
        assertEquals(listOf(sDone.id, pOn10.id, fTile.id), o.dropped.map { it.id })
        assertTrue(o.dropped.all { it.error == "Changed meanwhile — In progress by pm@x; not saved" })
        // the record's own locked 409: the same
        val lk = AlbumQueue.apply(Outbox(View(Album.PM), listOf(pOn10, fTile, sChange)), pOn10, 409, reply(S5Album.RECORD_LOCKED, pOn10))
        assertEquals(listOf(sChange), lk.outbox.queue)
        assertEquals(listOf(pOn10.id, fTile.id), lk.outbox.dropped.map { it.id })
        assertTrue(lk.outbox.dropped.all { it.error == "Work starts once the client approves the estimate" })
    }

    @Test fun `a photo op refused for good takes its photo's later re-files and reviews with it, and nothing else`() {
        val pOn10 = Album.photoOp(10, "Kitchen", "x".repeat(20), random = Random(32))
        val fTile = Album.refileOp(Album.provisionalPhoto(pOn10, Album.PM, "pm@x", "P M", "2026-11-03T05:20:00.000Z"), 95, Random(33))
        // the pure layer lets a review be built off a queued vendor photo's tile: it goes with its photo too
        val pV = Album.photoOp(50, null, "membrane", random = Random(34))
        val rTile = Album.reviewOp(Album.provisionalPhoto(pV, Album.VENDOR, "v@x", "V N", "2026-11-03T05:21:00.000Z"), Album.REVIEW_ACCEPTED, Random(35))
        val o = drain(Outbox(View(Album.PM, exec = true), listOf(pOn10, fTile, sRaise, pV, rTile, sChange)),
            Exchange("PUT /api/asis/PROJ-DEMO/album/photos/${pOn10.id}", null, 400, S5Album.RECORD_CAPTION),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 200, S5Album.RAISE_ADD),
            Exchange("PUT /api/asis/PROJ-DEMO/album/photos/${pV.id}", null, 400, S5Album.RECORD_CAPTION),
            Exchange(LOG, S5Album.SENT_RAISE_CHANGE, 200, S5Album.RAISE_CHANGE))
        assertEquals(emptyList(), o.queue)
        assertEquals(listOf(pOn10.id, fTile.id, pV.id, rTile.id), o.dropped.map { it.id })
        assertTrue(o.dropped.all { it.error == "caption must be text of at most 500 characters" })
        // a photo op that LANDS takes nothing: its re-file goes next
        val landed = Album.afterPost(Outbox(View(Album.PM), listOf(pOn10, fTile)), pOn10, Album.Answer())
        assertEquals(listOf(fTile), landed.queue)
    }

    @Test fun `a re-file or review of a photo the mirror does not hold is refused on its 404, that op alone - one in the mirror waits`() {
        // the tile of a photo op dropped BEFORE the re-file was queued (K5 keeps a dropped shot's tile, marked not sent)
        val pOn90 = Album.photoOp(90, "Kitchen", "units in", random = Random(37))
        val fTile = Album.refileOp(Album.provisionalPhoto(pOn90, Album.PM, "pm@x", "P M", "2026-11-03T05:20:00.000Z"), 95, Random(38))
        val pV = Album.photoOp(50, null, "membrane", random = Random(39))
        val rTile = Album.reviewOp(Album.provisionalPhoto(pV, Album.VENDOR, "v@x", "V N", "2026-11-03T05:21:00.000Z"), Album.REVIEW_ACCEPTED, Random(40))
        val m = View(Album.PM, exec = true, photos = listOf(staffPhoto))
        val o = drain(Outbox(m, listOf(fTile, rTile, sChange), dropped = listOf(pOn90.copy(error = "x"), pV.copy(error = "x"))),
            Exchange(refileLine(pOn90.id), """{"work":95,"base":90}""", 404, S5Album.REFILE_NO_RECORD),
            Exchange("PUT /api/asis/PROJ-DEMO/album/photos/${pV.id}/review", null, 404, S5Album.REVIEW_NO_RECORD),
            Exchange(LOG, S5Album.SENT_RAISE_CHANGE, 200, S5Album.RAISE_CHANGE))
        assertEquals(emptyList(), o.queue)
        assertEquals(listOf(fTile.id, rTile.id), o.dropped.drop(2).map { it.id })
        assertEquals(listOf("Not found", "Not found"), o.dropped.drop(2).map { it.error })
        // and with its photo op queued BEHIND it (FIFO: that op is never sent while the re-file holds the head): refused too
        val behind = AlbumQueue.apply(Outbox(m, listOf(fTile, pOn90)), fTile, 404, reply(S5Album.REFILE_NO_RECORD, fTile))
        assertEquals(Verdict.REFUSED, behind.verdict)
        assertEquals(listOf(pOn90), behind.outbox.queue)
        // a photo the mirror holds: S5's 404 is not "no row" (an older S5 without the route, a project not visible yet) -- STOP
        val held = Outbox(m, listOf(fRefile, sChange))
        assertEquals(AlbumQueue.Applied(held, Verdict.STOP), AlbumQueue.apply(held, fRefile, 404, reply("""{"error":"No such endpoint"}""", fRefile)))
        // a log op's 404 is STOP whatever the mirror holds
        assertEquals(Verdict.STOP, AlbumQueue.apply(Outbox(m, listOf(sChange)), sChange, 404, reply(S5Album.REFILE_NO_RECORD, sChange)).verdict)
    }

    @Test fun `no work's 409 drops a re-file into it - a re-file moves no works seq (S5-P), so S5 takes it`() {
        val sOn95 = Album.stateOp(null, 95, "Not started", "In progress", "", Random(41))
        val q = listOf(sOn95, fRefile, sChange)
        val c = AlbumQueue.apply(Outbox(View(Album.PM, exec = true, photos = listOf(staffPhoto)), q), sOn95, 409, reply(S5Album.STATE_STALE, sOn95))
        assertEquals(listOf(fRefile, sChange), c.outbox.queue)
        assertEquals(listOf(sOn95.id), c.outbox.dropped.map { it.id })
        val lk = AlbumQueue.apply(Outbox(View(Album.PM, exec = true, photos = listOf(staffPhoto)), q), sOn95, 409, reply(S5Album.STATE_LOCKED, sOn95))
        assertEquals(listOf(fRefile, sChange), lk.outbox.queue, "nor its lock: the re-file meets its own")
    }

    @Test fun `a state 409 no longer cascades through a photo's re-files - each re-file goes, on the base the one before it left (v2)`() {
        // [Done on 95, P 90 to 95, P to 100, state on 100], the second re-file built off the mirror (base 90)
        val sOn95 = Album.stateOp(null, 95, "Not started", "In progress", "", Random(42))
        val to100 = Album.refileOp(staffPhoto, 100, Random(43))
        val sOn100 = Album.stateOp(null, 100, "Not started", "In progress", "", Random(44))
        val o = drain(Outbox(View(Album.PM, exec = true, photos = listOf(staffPhoto)), listOf(sOn95, fRefile, to100, sOn100)),
            Exchange(LOG, null, 409, S5Album.STATE_STALE),
            Exchange(refileLine(staffPhoto.id), S5Album.SENT_REFILE, 200, S5Album.REFILE),
            // the second re-file rides on the first's answer, not on the 90 it was queued with -- then the signal goes
            Exchange(refileLine(staffPhoto.id), """{"work":100,"base":95}""", 0, null))
        assertEquals(listOf(to100.copy(base = "95"), sOn100), o.queue)
        assertEquals(listOf(sOn95.id), o.dropped.map { it.id })
        assertEquals(95, o.mirror!!.photos.single().work)
    }

    @Test fun `a re-file's 409 drops the later re-files of the same photo - their base is where it would have put it (v3) - and nothing on their works`() {
        val mirror = View(Album.PM, exec = true, photos = listOf(staffPhoto))
        val to100 = Album.refileOp(staffPhoto, 100, Random(45))
        val to110 = Album.refileOp(staffPhoto.copy(work = 100), 110, Random(46))     // as the screen shows it with the queue applied
        val other = Album.refileOp(staffPhoto.copy(id = "ALB-0000000000aa"), 110, Random(47))
        val sOn110 = Album.stateOp(null, 110, "Not started", "In progress", "", Random(48))
        val c = AlbumQueue.apply(Outbox(mirror, listOf(to100, to110, other, sOn110)), to100, 409, reply(S5Album.REFILE_STALE, to100))
        assertEquals(listOf(other, sOn110), c.outbox.queue, "another photo's re-file and the state on 110 go on")
        assertEquals(listOf(to100.id, to110.id), c.outbox.dropped.map { it.id })
        assertTrue(c.outbox.dropped.all { it.error == "Re-filed meanwhile — it is in Onsite carpentry & panelling now; not saved" })
        assertEquals(95, c.outbox.mirror!!.photos.first { it.id == staffPhoto.id }.work)
    }

    @Test fun `a review chain - the posted review gives the next review of its photo its review_at, the next state on its work its seq`() {
        val rNext = Album.reviewOp(vendorPhoto, Album.REVIEW_INTERNAL, Random(12))
        val sOn50 = Album.stateOp(null, 50, "Not started", "In progress", "", Random(13))
        val m = View(Album.PM, photos = listOf(vendorPhoto))
        val a = AlbumQueue.apply(Outbox(m, listOf(rAccept, rNext, sOn50)), rAccept, 200, reply(S5Album.REVIEW, rAccept))
        assertEquals(listOf(rNext.copy(base = "2026-11-03T05:16:04.218Z"), sOn50.copy(base = "11")), a.outbox.queue)
        assertEquals("accepted", a.outbox.mirror!!.photos.single().review)
        // a chain runs within ONE kind: a re-file queued on the same photo keeps the work it saw, never a review_at
        val rf = Album.refileOp(vendorPhoto, 95, Random(14))
        assertEquals(listOf(rf), AlbumQueue.apply(Outbox(m, listOf(rAccept, rf)), rAccept, 200, reply(S5Album.REVIEW, rAccept)).outbox.queue)
        assertEquals(listOf(rNext), Album.afterRefused(Outbox(m, listOf(rf, rNext)), rf, "x").queue, "nor a review a re-file's work")
        // a review before the JPEG landed: S5's 409, the photo still the PM's to review
        val nb = AlbumQueue.apply(Outbox(m.copy(photos = listOf(vendorPhoto.copy(bytes = 0))), listOf(rAccept)), rAccept, 409, reply(S5Album.REVIEW_NO_IMAGE, rAccept))
        assertEquals(Verdict.CONFLICT, nb.verdict)
        assertEquals(1, Album.pending(nb.outbox.mirror!!.photos).size)
        // a review of a staff photo: refused for good
        assertEquals(Verdict.REFUSED, AlbumQueue.apply(Outbox(m, listOf(rAccept)), rAccept, 400, reply(S5Album.REVIEW_STAFF, rAccept)).verdict)
    }

    @Test fun `a replayed review gives the next review of its photo the replayed row's own at, never the photo's review_at now (the K4 review, finding 2)`() {
        // asis.js, the review PUT's replay: the flat fields are `reviewOf(now_)` -- the photo as it stands NOW -- and
        // `entry` the op's own row. Shaped by hand from asis.js (S5Album's harness has no second PM): rAccept landed at
        // t1 and its answer was lost; another login S5 gives role pm (D3) then kept the photo and accepted it again at t3;
        // the resent rAccept is answered written: false with t3's review beside rAccept's own row at t1.
        val t1 = "2026-11-03T05:16:04.218Z"
        val t3 = "2026-11-03T05:31:00.000Z"
        val now = """{"ok":true,"written":false,"id":"ALB-000000000020","photo":"ALB-00000000001e","review":"accepted","review_by":"other@x","review_by_name":"O T","review_at":"$t3""""
        val row = """"entry":{"seq":11,"id":"ALB-000000000020","kind":"review","work":50,"photo":"ALB-00000000001e","value":"accepted","prev":"pending","note":"","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"$t1"}"""
        val rNext = Album.reviewOp(vendorPhoto, Album.REVIEW_INTERNAL, Random(60))
        val m = View(Album.PM, photos = listOf(vendorPhoto))
        val a = AlbumQueue.apply(Outbox(m, listOf(rAccept, rNext)), rAccept, 200, reply("$now,$row}", rAccept))
        assertEquals(Verdict.REPLAY, a.verdict)
        assertEquals(listOf(rNext.copy(base = t1)), a.outbox.queue,
            "based on the replayed row, so S5's guard on the old review_at answers this keep 409 Reviewed meanwhile instead of taking it over a review the PM never saw")
        // the mirror still takes the photo as S5 holds it now
        assertEquals(t3, a.outbox.mirror!!.photos.single().reviewAt)
        assertEquals("other@x", a.outbox.mirror!!.photos.single().reviewBy)
        // a replay with no row of its own (S5 found the state already there, `now_.review === v.state`, and no entry for
        // the id), or a row that is not this op's, leaves the flat review_at as the base
        assertEquals(listOf(rNext.copy(base = t3)), AlbumQueue.apply(Outbox(m, listOf(rAccept, rNext)), rAccept, 200, reply("$now}", rAccept)).outbox.queue)
        val notOurs = row.replace("\"id\":\"ALB-000000000020\"", "\"id\":\"ALB-000000000021\"")
        assertEquals(listOf(rNext.copy(base = t3)), AlbumQueue.apply(Outbox(m, listOf(rAccept, rNext)), rAccept, 200, reply("$now,$notOurs}", rAccept)).outbox.queue)
        // a fresh write: the row's at and the photo's review_at are one instant (asis.js albumReview binds ?6 to both)
        assertEquals(listOf(rNext.copy(base = t1)), AlbumQueue.apply(Outbox(m, listOf(rAccept, rNext)), rAccept, 200, reply(S5Album.REVIEW, rAccept)).outbox.queue)
    }

    @Test fun `an op of a kind this build cannot send has no request - a 400 drops it alone`() {
        val bogus = Op("ALB-000000000099", "bogus", 10)
        assertNull(AlbumQueue.stepOf(bogus))
        assertNull(AlbumQueue.requestOf(bogus, P))
        assertEquals(AlbumQueue.Next(bogus, null), AlbumQueue.next(Outbox(queue = listOf(bogus, sState))))
        val a = AlbumQueue.apply(Outbox(queue = listOf(bogus, sState)), bogus, 400, AlbumQueue.Reply())
        assertEquals(listOf(sState), a.outbox.queue)
        assertEquals(listOf(bogus), a.outbox.dropped)
    }

    // ---- K4: the drain (K2's), the pass AlbumOutbox runs, against a fake S5 answering S5Album's bodies ----------

    /** A fake S5 for [AlbumQueue.drain]: answers [script] in order, keeps the outbox as stored, and records what it saw. */
    private inner class Fake(var stored: Outbox, vararg script: Exchange, val renew: List<Boolean> = emptyList()) {
        val left = ArrayDeque(script.toList())
        val sent = ArrayList<String>()
        var renewals = 0
        /** For each commit, whether the op was still queued as S5's answer reached the phone (the answer before the removal). */
        val queuedAtCommit = ArrayList<Boolean>()
        var onSend: ((Int) -> Unit)? = null

        fun run(sends: Set<Step> = AlbumQueue.K4_STEPS): AlbumQueue.Pass = AlbumQueue.drain(P, MiniRead,
            load = { stored },
            send = { rq ->
                val ex = left.removeFirstOrNull() ?: fail("S5 was not expecting ${rq.method} ${rq.path { it }}")
                assertEquals(ex.line, "${rq.method} ${rq.path { it }}")
                if (ex.body != null) assertEquals(ex.body, json(rq.fields))
                sent += json(rq.fields)
                onSend?.invoke(sent.size)
                AlbumQueue.Sent(ex.code, ex.answer?.let { tree(it) })
            },
            commit = { op, code, reply ->
                queuedAtCommit += stored.queue.any { it.id == op.id }
                val a = AlbumQueue.apply(stored, op, code, reply)
                stored = a.outbox
                a
            },
            relogin = { renewals += 1; renew.getOrElse(renewals - 1) { false } },
            stopped = { false },
            sends = sends)
    }

    @Test fun `the drain sends FIFO and each op rides on the answer before it`() {
        val f = Fake(Outbox(View(Album.PM, exec = true), listOf(sState, sDone.copy(base = "0"), sRaise, sAnswer.copy(base = "0"))),
            Exchange(LOG, S5Album.SENT_STATE, 200, S5Album.STATE_POSTED),
            Exchange(LOG, S5Album.SENT_STATE_DONE, 200, S5Album.STATE_DONE),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 200, S5Album.RAISE_ADD),
            Exchange(LOG, S5Album.SENT_ANSWER, 200, S5Album.ANSWER_APPROVED))
        assertEquals(AlbumQueue.Pass(4, false), f.run())
        assertEquals(emptyList(), f.stored.queue)
        assertEquals(emptyList(), f.stored.dropped)
        assertTrue(f.left.isEmpty())
    }

    @Test fun `the answer is taken before the op leaves the queue`() {
        val f = Fake(Outbox(View(Album.PM, exec = true), listOf(sState, sRaise)),
            Exchange(LOG, S5Album.SENT_STATE, 200, S5Album.STATE_POSTED),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 409, S5Album.STATE_STALE))
        f.run()
        assertEquals(listOf(true, true), f.queuedAtCommit, "every commit sees its op still queued: :app puts S5's row on the phone first")
        assertEquals(emptyList(), f.stored.queue)
    }

    @Test fun `two works interleaved - a 409 on one drops its later ops, the other's still go, in order`() {
        val f = Fake(Outbox(View(Album.PM, exec = true), listOf(sState, sRaise, sDone.copy(base = "0"), sAnswer.copy(base = "0"))),
            Exchange(LOG, S5Album.SENT_STATE, 409, S5Album.STATE_STALE),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 200, S5Album.RAISE_ADD),
            // 90's answer rides on its raise's answer (8); 10's Done left with the state S5 won over
            Exchange(LOG, S5Album.SENT_ANSWER, 200, S5Album.ANSWER_APPROVED))
        assertEquals(AlbumQueue.Pass(3, false), f.run())
        assertEquals(listOf(sState.id, sDone.id), f.stored.dropped.map { it.id })
        assertEquals(emptyList(), f.stored.queue)
    }

    @Test fun `an op tapped during a pass goes in the same pass`() {
        val f = Fake(Outbox(View(Album.PM, exec = true), listOf(sState)),
            Exchange(LOG, S5Album.SENT_STATE, 200, S5Album.STATE_POSTED),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 200, S5Album.RAISE_ADD))
        // the tap lands while the first request is out (AlbumOutbox.add appends under the store's lock)
        f.onSend = { n -> if (n == 1) f.stored = f.stored.copy(queue = f.stored.queue + sRaise) }
        assertEquals(AlbumQueue.Pass(2, false), f.run())
        assertEquals(emptyList(), f.stored.queue)
    }

    @Test fun `a 401 renews once and resends the same op - a second stops the pass with the queue as it was`() {
        val start = Outbox(View(Album.PM, exec = true), listOf(sState, sRaise))
        val ok = Fake(start,
            Exchange(LOG, S5Album.SENT_STATE, 401, """{"error":"Sign in"}"""),
            Exchange(LOG, S5Album.SENT_STATE, 200, S5Album.STATE_POSTED),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 200, S5Album.RAISE_ADD), renew = listOf(true))
        assertEquals(AlbumQueue.Pass(2, false), ok.run())
        assertEquals(1, ok.renewals)
        assertEquals(ok.sent[0], ok.sent[1], "the same op, the same id, sent again")
        val twice = Fake(start,
            Exchange(LOG, S5Album.SENT_STATE, 401, null),
            Exchange(LOG, S5Album.SENT_STATE, 401, null), renew = listOf(true, true))
        assertEquals(AlbumQueue.Pass(0, true, 401), twice.run())
        assertEquals(1, twice.renewals, "renewed once per pass")
        assertEquals(start, twice.stored)
        val refused = Fake(start, Exchange(LOG, S5Album.SENT_STATE, 401, null), renew = listOf(false))
        assertEquals(AlbumQueue.Pass(0, true, 401), refused.run())
        assertEquals(start, refused.stored)
    }

    @Test fun `no network, a timeout and a 5xx stop the pass - the next pass replays the same ids and S5's written false lets them go`() {
        val start = Outbox(View(Album.PM, exec = true), listOf(sState, sRaise))
        for (code in listOf(0, -1, 500, 503)) {
            val f = Fake(start, Exchange(LOG, S5Album.SENT_STATE, code, null))
            assertEquals(AlbumQueue.Pass(0, true, code), f.run(), "$code")
            assertEquals(start, f.stored, "$code: the queue as it was")
        }
        // the first write landed and its answer was lost: the next pass sends the same id and S5 replays it
        val next = Fake(start,
            Exchange(LOG, S5Album.SENT_STATE, 200, S5Album.STATE_REPLAY),
            Exchange(LOG, S5Album.SENT_RAISE_ADD, 200, S5Album.RAISE_ADD))
        assertEquals(AlbumQueue.Pass(2, false), next.run())
        assertEquals(emptyList(), next.stored.queue)
        assertEquals(emptyList(), next.stored.dropped)
    }

    @Test fun `the GET is asked after a pass that took an answer, never after one that only stopped`() {
        // :app asks the album GET when Pass.answered > 0 (AlbumPageTest reads it there): a refusal is an answer, a STOP is not
        val refused = Fake(Outbox(View(Album.PM, exec = true), listOf(sState)), Exchange(LOG, S5Album.SENT_STATE, 403, S5Album.STATE_DESIGNER))
        assertEquals(AlbumQueue.Pass(1, false), refused.run())
        val stopped = Fake(Outbox(View(Album.PM, exec = true), listOf(sState)), Exchange(LOG, S5Album.SENT_STATE, 502, null))
        assertEquals(0, stopped.run().answered)
        val empty = Fake(Outbox(View(Album.PM, exec = true)))
        assertEquals(AlbumQueue.Pass(0, false), empty.run())
    }

    @Test fun `dropped keeps the newest twenty, each with S5's words`() {
        val ops = (1..25).map { i -> Album.stateOp(null, 10, "Not started", "In progress", "", Random(100 + i)) }
        val f = Fake(Outbox(View(Album.PM, exec = true), ops), *ops.map { Exchange(LOG, null, 403, S5Album.STATE_DESIGNER) }.toTypedArray())
        assertEquals(AlbumQueue.Pass(25, false), f.run())
        val kept = AlbumQueue.Stored(f.stored.queue, f.stored.dropped).capped()
        assertEquals(AlbumQueue.KEEP_DROPPED, kept.dropped.size)
        assertEquals(ops.takeLast(20).map { it.id }, kept.dropped.map { it.id }, "the newest twenty")
        assertTrue(kept.dropped.all { it.error == "Only the Project Manager sets a work's state" })
        assertEquals(AlbumQueue.Stored(dropped = ops.take(3)), AlbumQueue.Stored(dropped = ops.take(3)).capped())
    }

    @Test fun `a work's 409 leaves the reviews on it, and a review's 409 drops the photo's later reviews alone (the K4 critic, finding 2)`() {
        val other = vendorPhoto.copy(id = "ALB-00000000002e")
        val m = View(Album.PM, exec = true, photos = listOf(vendorPhoto, other))
        val s50 = Album.stateOp(null, 50, "Not started", "In progress", "", Random(50))
        val s50b = Album.stateOp(null, 50, "In progress", "On hold", "", Random(51))
        val rOther = Album.reviewOp(other, Album.REVIEW_INTERNAL, Random(52))
        // the state's 409 drops the work's later LOG ops; S5's review never reads the work's seq, so the reviews go
        val c = AlbumQueue.apply(Outbox(m, listOf(s50, rAccept, rOther, s50b)), s50, 409, reply(S5Album.STATE_STALE, s50))
        assertEquals(Verdict.CONFLICT, c.verdict)
        assertEquals(listOf(rAccept, rOther), c.outbox.queue)
        assertEquals(listOf(s50.id, s50b.id), c.outbox.dropped.map { it.id })
        // nor does a work's lock: the review route has none
        assertEquals(listOf(rAccept), AlbumQueue.apply(Outbox(m, listOf(s50, rAccept)), s50, 409, reply(S5Album.STATE_LOCKED, s50)).outbox.queue)
        // a review's 409: the later reviews of THAT photo go with it, never another photo's, never the work's log ops
        val rAgain = Album.reviewOp(vendorPhoto, Album.REVIEW_INTERNAL, Random(53))
        val r = AlbumQueue.apply(Outbox(m, listOf(rAccept, rAgain, rOther, s50)), rAccept, 409, reply(S5Album.REVIEW_STALE, rAccept))
        assertEquals(listOf(rOther, s50), r.outbox.queue)
        assertEquals(listOf(rAccept.id, rAgain.id), r.outbox.dropped.map { it.id })
        assertTrue(r.outbox.dropped.all { it.error == "Reviewed meanwhile — accepted by pm@x; not saved" })
    }

    // S5's 409 from asis.js's log POST when the scope moved between its own read and its batch (876-878: `w.seq ===
    // v.base`, the live request still there): shaped by hand from that code, since the harness that produced S5Album's
    // bodies did not race two amendments. The scope and stamp are those another work's approved add left.
    private val scopeRace = """{"error":"The scope was changed meanwhile; not saved — it now shows what is stored","work":{"id":90,"state":"Not started","by":"","by_name":"","at":"","seq":8,"in_scope":false,"cr":{"id":"ALB-000000000007","kind":"add","by":"pm@x","by_name":"P M","at":"2026-11-03T05:16:04.199Z","note":"factory-made units added","seq":8},"count":0},"entry":{"seq":8,"id":"ALB-000000000007","kind":"cr_raise","work":90,"photo":null,"value":"add","prev":null,"note":"factory-made units added","role":"pm","by":"pm@x","by_name":"P M","via":"phone","client_at":null,"at":"2026-11-03T05:16:04.199Z"},"scope":[10,20,30,40,50,60,70,80,100,110,120,130,140],"scope_at":"2026-11-03T05:16:04.300Z"}"""

    @Test fun `a recorded answer's 409 with its own base and a live request is S5's scope race - sent again, never dropped (the K4 critic, finding 5)`() {
        val start = Outbox(View(Album.PM, exec = true), listOf(sAnswer, sChange))
        val race = AlbumQueue.apply(start, sAnswer, 409, reply(scopeRace, sAnswer))
        assertEquals(AlbumQueue.Applied(start, Verdict.STOP), race)
        val generic = scopeRace.replace("The scope was changed meanwhile; not saved — it now shows what is stored", "Changed meanwhile; not saved")
        assertEquals(Verdict.STOP, AlbumQueue.apply(start, sAnswer, 409, reply(generic, sAnswer)).verdict)
        // no live request left on its own base: S5 has nothing to clear, so it is S5 winning (a resend could only loop)
        val noCr = scopeRace.replace(Regex(""""cr":\{[^}]*\}"""), "\"cr\":null").replace("The scope was changed meanwhile; not saved — it now shows what is stored", "No change request to clear on this work")
        assertEquals(Verdict.CONFLICT, AlbumQueue.apply(start, sAnswer, 409, reply(noCr, sAnswer)).verdict)
        // another base: somebody moved the work, S5 wins
        assertEquals(Verdict.CONFLICT, AlbumQueue.apply(start, sAnswer.copy(base = "7"), 409, reply(scopeRace, sAnswer)).verdict)
        // a state's 409 on its own base is the state S5 has, never a race
        assertEquals(Verdict.CONFLICT, AlbumQueue.apply(Outbox(View(Album.PM), listOf(sDone)), sDone, 409, reply(S5Album.STATE_WRONG_PREV, sDone)).verdict)
        // the pass stops on the race and the next pass sends the same id, which S5 now writes
        val f = Fake(start, Exchange(LOG, S5Album.SENT_ANSWER, 409, scopeRace))
        assertEquals(AlbumQueue.Pass(0, true, 409), f.run())
        val g = Fake(f.stored, Exchange(LOG, S5Album.SENT_ANSWER, 200, S5Album.ANSWER_APPROVED), Exchange(LOG, S5Album.SENT_RAISE_CHANGE, 200, S5Album.RAISE_CHANGE))
        assertEquals(AlbumQueue.Pass(2, false), g.run())
    }

    @Test fun `settle - an op S5 holds leaves dropped, whatever its answer said (the K4 critic, finding 4)`() {
        // three ops refused 403 after a PM was named: S5 held the first two (their answers had been lost)
        val s = AlbumQueue.Stored(queue = listOf(sChange), dropped = listOf(sState, rAccept, sRaise).map { it.copy(error = "Only the Project Manager sets a work's state") })
        val mirror = View(Album.DESIGNER, log = listOf(
            Album.Entry(6, sState.id, Album.KIND_STATE, 10, "In progress", "Not started"),
            Album.Entry(11, rAccept.id, Album.KIND_REVIEW, 50, "accepted", "pending", photo = vendorPhoto.id),
            // the same id as the raise, but another entry (a collision): not the raise S5 holds
            Album.Entry(12, sRaise.id, Album.KIND_STATE, 10, "Done", "In progress")))
        val t = AlbumQueue.settle(s, mirror)
        assertEquals(listOf(sRaise.id), t.dropped.map { it.id })
        assertEquals(s.queue, t.queue, "the queue is never touched")
        assertEquals(s, AlbumQueue.settle(s, null))
        assertEquals(s, AlbumQueue.settle(s, View(Album.PM)))
    }

    @Test fun `a photo op at the head is held - K5's step, nothing sent, the pass ends untouched (the K4 critic, finding 9)`() {
        val start = Outbox(View(Album.PM, exec = true), listOf(pStaff, sState))
        val f = Fake(start)
        assertEquals(AlbumQueue.Pass(0, true, held = true), f.run())
        assertEquals(start, f.stored)
        assertEquals(setOf(Step.LOG, Step.REVIEW, Step.REFILE), AlbumQueue.K4_STEPS)
        // with every step allowed (K5), the same queue sends its record first
        val k5 = Fake(start, Exchange("PUT /api/asis/PROJ-DEMO/album/photos/ALB-000000000014", S5Album.SENT_RECORD, 503, null))
        assertEquals(AlbumQueue.Pass(0, true, 503), k5.run(Step.entries.toSet()))
    }

    @Test fun `every login's outbox - one login's ops are never read, listed or written for another (the K4 critic, finding 8)`() {
        var all = AlbumQueue.Outboxes()
        all = all.with(" PM@x ", P, AlbumQueue.Stored(queue = listOf(sState)))
        all = all.with("d@x", P, AlbumQueue.Stored(queue = listOf(sRaise)))
        assertEquals(listOf(sState), all.of("pm@x", P).queue, "the login as S5 matches a party: trimmed, lower-cased")
        assertEquals(listOf(sRaise), all.of("D@X", P).queue)
        assertEquals(emptyList(), all.of("v@x", P).queue)
        assertEquals(emptyList(), all.of("", P).queue, "nobody signed in reads nothing")
        assertEquals(all, all.with("", P, AlbumQueue.Stored(queue = listOf(sChange))), "and stores nothing")
        assertEquals(listOf(P), all.projects("pm@x"))
        assertEquals(emptyList(), all.projects("v@x"))
        // an entry emptied leaves the map; the other login's stays
        val after = all.with("pm@x", P, AlbumQueue.Stored())
        assertEquals(setOf("d@x"), after.logins.keys)
        assertEquals(emptyList(), after.projects("pm@x"))
        // dropped alone keeps the entry, and lists no project to drain
        val dropped = all.with("pm@x", P, AlbumQueue.Stored(dropped = listOf(sState)))
        assertEquals(emptyList(), dropped.projects("pm@x"))
        assertEquals(listOf(sState), dropped.of("pm@x", P).dropped)
    }
}
