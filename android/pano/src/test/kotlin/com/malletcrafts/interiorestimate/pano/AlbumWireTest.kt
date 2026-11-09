package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.Album.BytesPatch
import com.malletcrafts.interiorestimate.pano.Album.CrKind
import com.malletcrafts.interiorestimate.pano.Album.CrOutcome
import com.malletcrafts.interiorestimate.pano.Album.Entry
import com.malletcrafts.interiorestimate.pano.Album.Op
import com.malletcrafts.interiorestimate.pano.Album.Photo
import com.malletcrafts.interiorestimate.pano.Album.ReviewPatch
import com.malletcrafts.interiorestimate.pano.Album.View
import com.malletcrafts.interiorestimate.pano.Album.Viewer
import com.malletcrafts.interiorestimate.pano.Album.Work
import com.malletcrafts.interiorestimate.pano.Album.WorkState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/** MiniJson's tree read through [AlbumWire.Read]'s three questions: what the tests parse with. */
internal object MiniRead : AlbumWire.Read {
    override fun field(o: Any?, key: String): Any? = (o as? Map<*, *>)?.get(key)
    override fun items(a: Any?): List<Any?>? = (a as? List<*>)?.toList()
    override fun isObject(v: Any?): Boolean = v is Map<*, *>
}

/**
 * org.json's shapes without org.json (it is Android's): JSON null held as a sentinel, as `JSONObject.NULL` is, and a
 * number that fits an Int held as an Integer, as JSONTokener does -- so the reader contract :app implements in QV is
 * proven on both kinds of tree.
 */
private object OrgNull
private class OrgObj(val m: Map<String, Any>)
private class OrgArr(val a: List<Any>)

private fun org(v: Any?): Any = when (v) {
    null -> OrgNull
    is Map<*, *> -> OrgObj(v.entries.associate { (k, x) -> (k as String) to org(x) })
    is List<*> -> OrgArr(v.map { org(it) })
    is Long -> if (v in Int.MIN_VALUE..Int.MAX_VALUE) v.toInt() else v
    else -> v
}

private object OrgRead : AlbumWire.Read {
    override fun field(o: Any?, key: String): Any? = (o as? OrgObj)?.m?.get(key)?.takeIf { it !== OrgNull }
    override fun items(a: Any?): List<Any?>? = (a as? OrgArr)?.a?.map { it.takeIf { x -> x !== OrgNull } }
    override fun isObject(v: Any?): Boolean = v is OrgObj
}

/**
 * The album's answers read off S5's own bodies (port 61c slice AQ): [S5Album] holds what S5 QUOTE 9a51733 answered,
 * on its real SQL, to the bodies AlbumQueue builds. A synthetic project with invented names: mallet_estimator is
 * public. Every expected value is written out by hand.
 */
class AlbumWireTest {

    private fun tree(s: String): Any? = MiniJson.parse(s)
    private fun view(s: String): View = assertNotNull(AlbumWire.view(MiniRead, tree(s)))

    /** [v] as org.json writes `JSONObject(stored)`: a null is JSON null, a number its digits, a string quoted. */
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

    /** [v] written as the phone stores its mirror (AlbumWire.stored, serialised as org.json would) and read back through view. */
    private fun roundTrip(v: View): View = view(json(AlbumWire.stored(v)))

    private val scope12 = listOf(10, 20, 30, 40, 50, 60, 70, 100, 110, 120, 130, 140)
    private val scope13 = listOf(10, 20, 30, 40, 50, 60, 70, 90, 100, 110, 120, 130, 140)

    @Test fun `the PM's album GET reads whole - the head, the works, the log, the stage moves, the photos, the captures`() {
        val v = view(S5Album.GET_PM)
        assertEquals("pm", v.role)
        assertEquals(emptyList(), v.mine)
        assertTrue(v.exec)
        assertEquals(scope13, v.scope, "the approved add on 90 amended the scope")
        assertEquals("2026-11-03T05:16:04.201Z", v.scopeAt)
        assertEquals("pm@x", v.pm)
        assertEquals("Client approved", v.quoteStage)
        assertEquals("2026-11-03T05:16:04.183Z", v.quoteStageAt)
        assertEquals("pm@x", v.quoteStageBy, "who set the stage, for the project-state store (QM0, QuoteStage.fromAlbum)")
        // works[16]: S5's order, the base in seq, the live request as its raise row with the kind in value
        assertEquals(Album.WORK_IDS, v.works.map { it.id })
        assertEquals(Work(10, "Done", "pm@x", "P M", "2026-11-03T05:16:04.196Z", 7, true, null, 0), v.works.first { it.id == 10 })
        assertEquals(Work(50, "Not started", seq = 11, inScope = true, count = 1,
            cr = Entry(10, "ALB-000000000009", Album.KIND_CR_RAISE, 50, "change", note = "second coat", by = "pm@x", byName = "P M", at = "2026-11-03T05:16:04.204Z")),
            v.works.first { it.id == 50 })
        assertEquals(CrKind.CHANGE, v.works.first { it.id == 50 }.crKind)
        assertEquals(Work(90, seq = 9), v.works.first { it.id == 90 })
        assertEquals(listOf(80, 85, 95), v.works.filter { !it.inScope }.map { it.id })
        assertEquals(1, v.works.first { it.id == 95 }.count)
        // the log: five stage moves, two states, a raise and its answer, a live raise, a review -- in seq order
        assertEquals((1L..11L).toList(), v.log.map { it.seq })
        assertEquals(listOf("stage", "stage", "stage", "stage", "stage", "state", "state", "cr_raise", "cr_clear", "cr_raise", "review"), v.log.map { it.kind })
        assertEquals(Entry(6, "ALB-000000000001", "state", 10, "In progress", "Not started", "", "pm", "pm@x", "P M", "2026-11-03T05:16:04.187Z", null, "phone", null), v.log[5])
        assertEquals(Entry(9, "ALB-000000000008", "cr_clear", 90, "approved", "add", "by phone", "pm", "pm@x", "P M", "2026-11-03T05:16:04.201Z", null, "phone", "2026-11-02"), v.log[8])
        assertEquals(Entry(11, "ALB-000000000020", "review", 50, "accepted", "pending", "", "pm", "pm@x", "P M", "2026-11-03T05:16:04.218Z", "ALB-00000000001e", "phone", null), v.log[10])
        assertEquals(Entry(1, "ALB-0954f0a37b0a", "stage", null, "Client wish list", "Survey", "", "pm", "pm@x", "P M", "2026-11-03T05:16:04.178Z"), v.log[0])
        assertEquals("", v.log[7].prev, "a raise's null prev reads as none")
        assertEquals(11L, v.logNext)
        assertFalse(v.logMore)
        // stages[]: stageView sends no kind -- the reader names it, and quoteAt reads every row as a move
        assertEquals(listOf("Client wish list", "Vendor quoted", "Estimated", "Revisions", "Client approved"), v.stages.map { it.value })
        assertTrue(v.stages.all { it.kind == Album.KIND_STAGE && it.work == null && it.role == "" })
        assertEquals(Entry(5, "ALB-e47b89520fff", "stage", null, "Client approved", "Revisions", by = "pm@x", byName = "P M", at = "2026-11-03T05:16:04.183Z"), v.stages[4])
        // photos[]: S5's '' for none reads as null; a staff photo has no vendor and was never reviewed
        assertEquals(listOf(
            Photo("ALB-000000000014", 95, "2026-11-03T05:16:04.209Z", "staff", "Kitchen", "photo", "units in", null, "pm", "pm@x", "P M", "pending", null, null, null, 10, null, "phone", null),
            Photo("ALB-00000000001e", 50, "2026-11-03T05:16:04.215Z", "vendor", null, "photo", "membrane, second coat", "Demo Waterproofing Co.", "vendor", "v@x", "V N",
                "accepted", "pm@x", "P M", "2026-11-03T05:16:04.218Z", 10, null, "phone", null)), v.photos)
        // captures[]: a staff 360, filed by S5 (0 = the quote journey), at the moment it was TAKEN; read as a CAPTURE item
        // with S5's upload `at` beside it, which names the phone's cached copy of its face (port 61c slice K3)
        assertEquals(listOf(Photo("MCAP-DEMO-1", 0, "2026-10-11T08:30:00+05:30", Album.PARTY_STAFF, "Kitchen", Album.PHOTO_360, by = "pm@x",
            source = Album.Source.CAPTURE, uploadedAt = "2026-11-03T05:16:04.175Z")), v.captures)
        // and the photos read as ALBUM items, the default
        assertTrue(v.photos.all { it.source == Album.Source.ALBUM && it.capture == null && it.uploadedAt == "" })
        assertEquals(0, v.toReview)
        assertEquals(1, v.changeRequests)
        assertFalse(v.closed)
        assertNull(v.closedAt, "S5's '' for no close")
        assertFalse(v.more)
    }

    @Test fun `what the phone derives from the GET agrees with what S5 summed - states, bases, live and decided requests, the amended scope`() {
        val v = view(S5Album.GET_PM)
        assertEquals(mapOf(10 to WorkState("Done", 7, "2026-11-03T05:16:04.196Z")), Album.statesOfWorks(v.works))
        assertEquals(Album.statesOfWorks(v.works), Album.statesOfLog(v.log))
        // the base is the newest seq of ANY kind: the review on 50 moved it to 11 past its raise at 10
        assertEquals(listOf(7L, 11L, 9L, 0L), listOf(10, 50, 90, 20).map { Album.baseOf(v, it) })
        assertEquals(v.works.map { it.seq }, Album.WORK_IDS.map { Album.baseOf(v, it) })
        assertEquals(listOf(50 to CrKind.CHANGE), Album.changeRequests(v.log).map { it.id to it.kind })
        assertEquals(v.changeRequests, Album.changeRequests(v.log).size)
        val d = Album.crDone(v.log, 90).single()
        assertEquals(Triple(CrOutcome.APPROVED, CrKind.ADD, "2026-11-02"), Triple(d.outcome, d.kind, d.day))
        assertEquals(v.scope, Album.amendedScope(scope12, CrKind.ADD, CrOutcome.APPROVED, 90))
        // the chapters the screen will draw from it: the capture taken before any logged move groups at Survey
        val pm = Album.viewerOf(v)
        assertEquals(Viewer(Album.PM), pm)
        val ch = Album.chapters(v.captures + v.photos, v.log, v.stages, v.scope, v.quoteStage, v.quoteStageAt, Album.statesOfWorks(v.works), pm)
        assertEquals(listOf("MCAP-DEMO-1"), ch.quote.groups.first { it.stage == "Survey" }.items.map { it.id })
        assertEquals(listOf("ALB-000000000014"), ch.works.first { it.id == 95 }.items.map { it.id })
        assertEquals(listOf("ALB-00000000001e"), ch.works.first { it.id == 50 }.items.map { it.id })
        assertEquals("Done", ch.works.first { it.id == 10 }.state)
        assertFalse(ch.execLocked)
        assertEquals(v.photos.size + v.captures.size, ch.total)
        assertEquals(emptyList(), Album.pending(v.photos), "the vendor's photo is accepted")
    }

    @Test fun `the vendor's GET - its works, its own photo with its name, no stage moves, no PM and no scope stamp`() {
        val v = view(S5Album.GET_VENDOR)
        assertEquals("vendor", v.role)
        assertEquals(listOf(30, 50), v.mine)
        assertEquals(scope13, v.scope)
        assertEquals("", v.scopeAt, "S5 sends scope_at to staff only")
        assertNull(v.pm)
        assertEquals(emptyList(), v.stages)
        assertEquals(emptyList(), v.captures)
        assertEquals(listOf(10L, 11L), v.log.map { it.seq }, "the rows of its own works only")
        assertEquals("Demo Waterproofing Co.", v.photos.single().vendor)
        val me = Album.viewerOf(v, listOf("Demo Waterproofing Co."))
        assertEquals(Viewer(Album.VENDOR, setOf(30, 50), setOf("demo waterproofing co.")), me)
        assertTrue(v.photos.all { Album.canSee(me, it) })
        assertEquals(listOf(50), v.works.filter { it.cr != null }.map { it.id })
    }

    @Test fun `the client's GET - the vendor's names blanked, both photos visible, the decided request in the log`() {
        val v = view(S5Album.GET_CLIENT)
        assertEquals("client", v.role)
        val vp = v.photos.first { it.party == Album.PARTY_VENDOR }
        assertEquals(Triple(null as String?, "", ""), Triple(vp.vendor, vp.by, vp.byName))
        assertTrue(v.photos.all { Album.canSee(Viewer(Album.CLIENT), it) })
        assertEquals(CrOutcome.APPROVED, Album.crDone(v.log, 90).single().outcome)
        assertEquals("", v.scopeAt)
        assertNull(v.pm)
    }

    @Test fun `a project at Survey - no baseline, no log, locked, its capture still listed`() {
        val v = view(S5Album.GET_LOCKED)
        assertFalse(v.exec)
        assertNull(v.scope, "scope null is no baseline")
        assertEquals("Survey", v.quoteStage)
        assertEquals("", v.quoteStageAt)
        assertEquals("", v.quoteStageBy, "never moved: S5 sends '' for who")
        assertEquals(emptyList(), v.log)
        assertEquals(0L, v.logNext)
        assertEquals(16, v.works.size)
        assertFalse(Album.exec(v.quoteStage, Album.statesOfWorks(v.works).isNotEmpty()))
        assertEquals(listOf("MCAP-DEMO-2"), v.captures.map { it.id })
    }

    @Test fun `an org-json-shaped tree reads the same - JSON null as a sentinel, integers as Int`() {
        for (s in listOf(S5Album.GET_PM, S5Album.GET_VENDOR, S5Album.GET_CLIENT, S5Album.GET_LOCKED))
            assertEquals(AlbumWire.view(MiniRead, tree(s)), AlbumWire.view(OrgRead, org(tree(s))))
        val op = Op("ALB-000000000001", Album.KIND_STATE, 10, "0", "In progress", "Not started")
        for (s in listOf(S5Album.STATE_POSTED, S5Album.STATE_STALE, S5Album.STATE_LOCKED))
            assertEquals(AlbumWire.reply(MiniRead, tree(s), op), AlbumWire.reply(OrgRead, org(tree(s)), op))
    }

    @Test fun `reading is tolerant, never inventive - unknown fields ignored, a row without its key left out`() {
        assertNull(AlbumWire.view(MiniRead, tree("[]")))
        assertNull(AlbumWire.view(MiniRead, tree("""{"works":[]}""")), "no role: not S5's album")
        assertNull(AlbumWire.view(MiniRead, null))
        assertEquals(View("pm"), view("""{"role":"pm","new_field":{"x":1}}"""))
        val v = view("""{"role":"pm","works":[{"state":"Done"},{"id":10,"seq":4.0}],"log":[{"id":"ALB-a"},{"seq":3,"id":"ALB-b","kind":"state","work":10.5},{"seq":2.5,"id":"ALB-c"}],
            "photos":[{"work":10},{"id":"ALB-p","work":null,"bytes":"12"}],"captures":[{"room":"Hall"},{"capture":"","work":0}],"scope":"x","mine":[30,"50",50.0]}""")
        assertEquals(listOf(Work(10, seq = 4)), v.works, "a work without its id is left out; 4.0 is S5's integer 4")
        assertEquals(listOf(Entry(3, "ALB-b", "state", null)), v.log, "no seq or a seq of 2.5: left out; a work of 10.5 is no work")
        assertEquals(listOf(Photo("ALB-p", null, "")), v.photos, "bytes as text reads 0")
        assertEquals(emptyList(), v.captures)
        assertNull(v.scope, "a scope that is not a list is no baseline")
        assertEquals(listOf(30, 50), v.mine)
    }

    @Test fun `a log answer - the entry, the work row and the scope as it now stands, written or replay`() {
        val op = Op("ALB-000000000001", Album.KIND_STATE, 10, "0", "In progress", "Not started")
        val r = AlbumWire.reply(MiniRead, tree(S5Album.STATE_POSTED), op)
        assertEquals(true, r.written)
        assertFalse(r.locked)
        assertEquals("", r.error)
        assertEquals(Entry(6, "ALB-000000000001", "state", 10, "In progress", "Not started", "", "pm", "pm@x", "P M", "2026-11-03T05:16:04.187Z", null, "phone", null), r.answer.entry)
        assertEquals(Work(10, "In progress", "pm@x", "P M", "2026-11-03T05:16:04.187Z", 6), r.answer.work)
        assertEquals(scope12, r.answer.scope)
        assertEquals("2026-11-03T05:16:04.183Z", r.answer.scopeAt)
        assertEquals(false, AlbumWire.reply(MiniRead, tree(S5Album.STATE_REPLAY), op).written)
        // the approved add answers the AMENDED scope with its new stamp
        val a = AlbumWire.reply(MiniRead, tree(S5Album.ANSWER_APPROVED), op.copy(kind = Album.KIND_CR_CLEAR, work = 90))
        assertEquals(scope13 to "2026-11-03T05:16:04.201Z", a.answer.scope to a.answer.scopeAt)
        // a raise's work row carries the live request it just made
        val raise = AlbumWire.reply(MiniRead, tree(S5Album.RAISE_ADD), op.copy(kind = Album.KIND_CR_RAISE, work = 90)).answer.work!!
        assertEquals(CrKind.ADD to false, raise.crKind to raise.inScope)
    }

    @Test fun `a log 409 - S5's words, its current entry and work, and the lock with no baseline at all`() {
        val op = Op("ALB-000000000002", Album.KIND_STATE, 10, "0", "Done", "In progress")
        val stale = AlbumWire.reply(MiniRead, tree(S5Album.STATE_STALE), op)
        assertEquals("Changed meanwhile — In progress by pm@x; not saved", stale.error)
        assertFalse(stale.locked)
        assertEquals(6L, stale.answer.entry!!.seq)
        assertEquals(6L, stale.answer.work!!.seq)
        assertEquals("The work is In progress, not Not started; not saved", AlbumWire.reply(MiniRead, tree(S5Album.STATE_WRONG_PREV), op).error)
        val locked = AlbumWire.reply(MiniRead, tree(S5Album.STATE_LOCKED), op)
        assertTrue(locked.locked)
        assertEquals("Work starts once the client approves the estimate", locked.error)
        assertNull(locked.answer.entry, "no row on the work yet")
        assertEquals(null to "", locked.answer.scope to locked.answer.scopeAt, "the scope IS in the answer: none")
        // a refusal carries words and nothing else: no scope stamp, so the mirror keeps its scope
        for (s in listOf(S5Album.STATE_BASE_TEXT, S5Album.STATE_DESIGNER)) {
            val r = AlbumWire.reply(MiniRead, tree(s), op)
            assertEquals(Album.Answer(), r.answer)
            assertTrue(r.error.isNotEmpty())
        }
    }

    @Test fun `a photo's record, its JPEG, a review and a re-file - each answer read in its own shape`() {
        val ph = Op("ALB-000000000014", Album.OP_PHOTO, 90, "", photo = "ALB-000000000014", room = "Kitchen", caption = "units in")
        val rec = AlbumWire.reply(MiniRead, tree(S5Album.RECORD), ph).answer.photo!!
        assertEquals(Photo("ALB-000000000014", 90, "2026-11-03T05:16:04.209Z", "staff", "Kitchen", "photo", "units in", null, "pm", "pm@x", "P M", via = "phone"), rec)
        assertEquals(10L, AlbumWire.reply(MiniRead, tree(S5Album.RECORD_LANDED), ph).answer.photo!!.bytes)
        assertTrue(AlbumWire.reply(MiniRead, tree(S5Album.RECORD_LOCKED), ph).let { it.locked && it.answer.photo == null })
        // the JPEG's answer names no photo: the bytes land on the op's
        val img = ph.copy(recorded = true)
        assertEquals(BytesPatch("ALB-000000000014", 10), AlbumWire.reply(MiniRead, tree(S5Album.IMG), img).answer.bytes)
        assertEquals(false, AlbumWire.reply(MiniRead, tree(S5Album.IMG_AGAIN), img).written)
        assertNull(AlbumWire.reply(MiniRead, tree(S5Album.IMG_NO_RECORD), img).answer.bytes)
        // a review: `photo` is the id, the review fields flat beside it, and the review's own log row
        val rv = Op("ALB-000000000020", Album.OP_REVIEW, 50, "", Album.REVIEW_ACCEPTED, photo = "ALB-00000000001e")
        val r = AlbumWire.reply(MiniRead, tree(S5Album.REVIEW), rv).answer
        assertEquals(ReviewPatch("ALB-00000000001e", "accepted", "pm@x", "P M", "2026-11-03T05:16:04.218Z"), r.review)
        assertEquals(11L to Album.KIND_REVIEW, r.entry!!.seq to r.entry!!.kind)
        assertNull(r.photo, "a review answer's photo is an id, never a row")
        assertEquals(ReviewPatch("ALB-00000000001e", "pending"), AlbumWire.reply(MiniRead, tree(S5Album.REVIEW_NO_IMAGE), rv).answer.review)
        assertEquals("Reviewed meanwhile — accepted by pm@x; not saved", AlbumWire.reply(MiniRead, tree(S5Album.REVIEW_STALE), rv).error)
        assertNull(AlbumWire.reply(MiniRead, tree(S5Album.REVIEW_STAFF), rv).answer.review)
        // a re-file answers the whole row, on its 200, its 409s and its 403s alike -- but the staff-only 403, before the photo is read
        val rf = Op("ALB-00000000ff01", Album.OP_REFILE, 95, "90", photo = "ALB-000000000014")
        assertEquals(95, AlbumWire.reply(MiniRead, tree(S5Album.REFILE), rf).answer.photo!!.work)
        assertEquals(95, AlbumWire.reply(MiniRead, tree(S5Album.REFILE_STALE), rf).answer.photo!!.work)
        assertEquals("vendor", AlbumWire.reply(MiniRead, tree(S5Album.REFILE_VENDOR), rf).answer.photo!!.party)
        assertTrue(AlbumWire.reply(MiniRead, tree(S5Album.REFILE_LOCKED), rf).let { it.locked && it.answer.photo!!.work == 0 })
        assertNull(AlbumWire.reply(MiniRead, tree(S5Album.REFILE_STAFF), rf).answer.photo)
    }

    // ---- the mirror as stored (port 61c slice QV) ------------------------------------------------------------

    @Test fun `stored - the mirror written in S5's own shape reads back whole, through the one reader`() {
        // S5's four GETs, each read, stored, serialised and read again: byte-equal Views (the roles, a null scope, a
        // scope stamp and PM blanked for a vendor, the works with their live request, the log, the stage moves with
        // their kind supplied by the reader, the photos with a vendor's names blanked, the capture at the moment taken)
        for (body in listOf(S5Album.GET_PM, S5Album.GET_VENDOR, S5Album.GET_CLIENT, S5Album.GET_LOCKED, S5Quote.ALBUM)) {
            val v = view(body)
            assertEquals(v, roundTrip(v), body.take(60))
        }
        // what is stored is S5's field names: scope null is JSON null, never a dropped key (a key dropped would read
        // as null too, but the mirror is a copy of S5's answer and says so)
        val locked = AlbumWire.stored(view(S5Album.GET_LOCKED))
        assertTrue(locked.containsKey("scope")); assertNull(locked["scope"])
        // S5 sends `pm` '' for none; the reader keeps it as null (View.pm), so the mirror stores null, which reads back the same
        assertEquals("", locked["scope_at"]); assertNull(locked["pm"]); assertNull(view(S5Album.GET_LOCKED).pm)
        val pm = AlbumWire.stored(view(S5Album.GET_PM))
        assertEquals(listOf(10, 20, 30, 40, 50, 60, 70, 90, 100, 110, 120, 130, 140), pm["scope"])
        assertEquals("pm", pm["role"]); assertEquals("2026-11-03T05:16:04.201Z", pm["scope_at"]); assertEquals("pm@x", pm["pm"])
        assertEquals("Client approved", pm["quote_stage"]); assertEquals("2026-11-03T05:16:04.183Z", pm["quote_stage_at"])
        @Suppress("UNCHECKED_CAST") val works = pm["works"] as List<Map<String, Any?>>
        assertEquals(Album.WORK_IDS, works.map { it["id"] })
        assertEquals(mapOf("id" to "ALB-000000000009", "kind" to "change", "by" to "pm@x", "by_name" to "P M", "at" to "2026-11-03T05:16:04.204Z", "note" to "second coat", "seq" to 10L),
            works.first { it["id"] == 50 }["cr"])
        assertNull(works.first { it["id"] == 10 }["cr"])
        @Suppress("UNCHECKED_CAST") val caps = pm["captures"] as List<Map<String, Any?>>
        assertEquals(listOf(mapOf("room" to "Kitchen", "capture" to "MCAP-DEMO-1", "captured_at" to "2026-10-11T08:30:00+05:30", "work" to 0, "by" to "pm@x",
            "by_name" to "", "at" to "2026-11-03T05:16:04.175Z")), caps)
        // a capture stored and read back is still a CAPTURE item with its upload `at` (K3): the face's file name survives a restart
        assertEquals(Album.Source.CAPTURE, roundTrip(view(S5Album.GET_PM)).captures.single().source)
        assertEquals("2026-11-03T05:16:04.175Z", roundTrip(view(S5Album.GET_PM)).captures.single().uploadedAt)
        // the K3 review, finding 3: a capture's `by_name`, when S5 sends it, is read and kept through the stored mirror,
        // so the card names a person, not a login (S5's capturesOf sends `by` alone today, and it reads blank)
        val named = view("""{"role":"pm","captures":[{"room":"Kitchen","capture":"MCAP-T1","captured_at":"2026-10-11T08:30:00+05:30","stage":"","work":0,"by":"d@x","by_name":"D N","at":"2026-11-03T05:16:04.175Z"}]}""")
        assertEquals("D N", named.captures.single().byName)
        assertEquals("D N", roundTrip(named).captures.single().byName)
        assertEquals("", view(S5Album.GET_PM).captures.single().byName)
        // a paged mirror (two pages merged) stores its whole log and the newer page's cursor
        val first = view(S5Album.GET_PM)
        val page = first.copy(log = first.log.takeLast(2), logNext = 99, logMore = true)
        val merged = Album.paged(first.copy(log = first.log.dropLast(2), logNext = 5, logMore = true), page)
        assertEquals(first.log, merged.log)
        assertEquals(merged, roundTrip(merged))
        assertEquals(99L, roundTrip(merged).logNext); assertTrue(roundTrip(merged).logMore)
    }

    @Test fun `a body that is not S5's JSON reads as nothing - and an op of no known kind has no rows`() {
        val op = Op("ALB-000000000001", Album.KIND_STATE, 10)
        assertEquals(AlbumQueue.Reply(), AlbumWire.reply(MiniRead, null, op))
        assertEquals(AlbumQueue.Reply(), AlbumWire.reply(MiniRead, tree("[1,2]"), op))
        // no `written` is no `written` (the AQ review): never read as true, which would take any 200 for a write
        assertNull(AlbumWire.reply(MiniRead, null, op).written)
        assertNull(AlbumWire.reply(MiniRead, tree("[1,2]"), op).written)
        assertNull(AlbumWire.reply(MiniRead, tree("""{"ok":true}"""), op).written)
        assertNull(AlbumWire.reply(MiniRead, tree("""{"ok":true,"written":"true"}"""), op).written, "a string is not S5's boolean")
        assertEquals(AlbumQueue.Reply(error = "Changed meanwhile — In progress by pm@x; not saved"),
            AlbumWire.reply(MiniRead, tree(S5Album.STATE_STALE), op.copy(kind = "bogus")))
    }

    // ---- K4: every login's outbox as the phone stores it (AlbumWire.storedOutbox / outboxOf) ----------------------

    private val o1 = Op("ALB-000000000001", Album.KIND_STATE, 10, base = "6", value = "Done", prev = "In progress", note = "debris out", at = "2026-11-09T06:30:00Z")
    private val o2 = Op("ALB-000000000008", Album.KIND_CR_CLEAR, 90, base = "8", value = "approved", prev = "add", note = "a call", clientAt = "2026-11-09",
        error = "Only the Project Manager sets a work's state", at = "2026-11-09T06:31:00Z")
    private val o3 = Op("ALB-000000000020", Album.OP_REVIEW, 50, base = "2026-11-03T05:16:04.218Z", value = "internal", photo = "ALB-00000000001e")
    private val o4 = Op("ALB-000000000014", Album.OP_PHOTO, 90, base = "", photo = "ALB-000000000014", room = null, caption = "units in", recorded = true)

    private fun outboxes() = AlbumQueue.Outboxes()
        .with("pm@x", "PROJ-T1", AlbumQueue.Stored(queue = listOf(o1, o3, o4), dropped = listOf(o2)))
        .with("pm@x", "PROJ-T2", AlbumQueue.Stored(queue = listOf(o2.copy(error = ""))))
        .with("d@x", "PROJ-T1", AlbumQueue.Stored(queue = listOf(o1.copy(id = "ALB-0000000000d1"))))

    @Test fun `the outbox round-trips exactly through the stored shape - every login, every project, every field`() {
        val o = outboxes()
        val stored = AlbumWire.storedOutbox(o)
        assertEquals(1, stored["v"])
        assertEquals(o, AlbumWire.outboxOf(MiniRead, tree(json(stored))))
        assertEquals(o, AlbumWire.outboxOf(OrgRead, org(tree(json(stored)))), "the same through an org-json-shaped tree")
        // the shape: lists of entries keyed inside, a base TEXT, a work a number or null, a null a JSON null never a dropped key
        val text = json(stored)
        assertTrue(text.startsWith("""{"v":1,"logins":[{"login":"pm@x","projects":[{"project":"PROJ-T1","queue":[{"id":"ALB-000000000001","kind":"state","work":10,"base":"6","""), text.take(160))
        assertTrue(text.contains(""""room":null,"caption":"units in","photo_kind":"photo","client_at":"","error":"","recorded":true,"at":"""""))
        assertTrue(text.contains(""""base":"2026-11-03T05:16:04.218Z""""))
        // one login's ops are read for that login alone
        val back = AlbumWire.outboxOf(MiniRead, tree(text))
        assertEquals(listOf(o1, o3, o4), back.of("pm@x", "PROJ-T1").queue)
        assertEquals(listOf("ALB-0000000000d1"), back.of("d@x", "PROJ-T1").queue.map { it.id })
        assertEquals(emptyList(), back.of("v@x", "PROJ-T1").queue)
    }

    @Test fun `an op without an id is left out, one of an unknown kind kept for the drain's 400, a base stays text, a null stays a null`() {
        val text = """{"v":1,"logins":[
            {"login":" PM@X ","projects":[{"project":"PROJ-T1","queue":[
                {"kind":"state","work":10,"base":"0","value":"Done"},
                {"id":"","kind":"state","work":10},
                {"id":"ALB-0000000000a1","kind":"teleport","work":10,"base":"0"},
                {"id":"ALB-0000000000a2","kind":"state","work":null,"base":7,"value":"Done"},
                {"id":"ALB-0000000000a3","kind":"review","work":50,"base":"","photo":"ALB-00000000001e","value":"accepted","room":null}
            ],"dropped":[]}]},
            {"login":"","projects":[{"project":"PROJ-T1","queue":[{"id":"ALB-0000000000a4","kind":"state","work":10}]}]},
            {"login":"pm@x","projects":[{"project":"","queue":[{"id":"ALB-0000000000a5","kind":"state","work":10}]},
                {"project":"PROJ-T1","queue":[{"id":"ALB-0000000000a6","kind":"state","work":20}]}]},
            "not an entry"
        ]}"""
        val o = AlbumWire.outboxOf(MiniRead, tree(text))
        assertEquals(setOf("pm@x"), o.logins.keys, "a blank login is no login; the key as S5 matches a party")
        val q = o.of("pm@x", "PROJ-T1").queue
        assertEquals(listOf("ALB-0000000000a1", "ALB-0000000000a2", "ALB-0000000000a3", "ALB-0000000000a6"), q.map { it.id },
            "no id: left out (it could never be replayed); an unknown kind: KEPT; a second entry for the login appended")
        assertEquals("teleport", q[0].kind)
        assertNull(AlbumQueue.stepOf(q[0]), "the drain answers it with AQ's 400, that op alone")
        assertEquals(listOf<Any?>(null, "7"), listOf(q[1].work, q[1].base), "a work null stays null; a number an older writer stored reads as its digits")
        assertEquals(listOf("", "accepted", null), listOf(q[2].base, q[2].value, q[2].room))
        assertEquals("", q[2].at, "an op an older build queued has no tap time")
        // anything not in the shape reads as none, never a throw
        for (bad in listOf("null", "[]", "{}", """{"logins":{}}""", """{"logins":[1,2]}""")) assertEquals(AlbumQueue.Outboxes(), AlbumWire.outboxOf(MiniRead, tree(bad)), bad)
    }

    @Test fun `a photo op round-trips through the stored outbox with its client_at, recorded and caption (K5)`() {
        val shot = AlbumWrite.shotOp(AlbumWrite.Shot("ALB-00000000c001", 90, "Kitchen", "units in, \"left\" wall"), java.time.Instant.parse("2026-11-09T06:31:07.250Z"))
        val single = AlbumWrite.single("MCAP-0a1b2c3d4e5f", 95, "Living Room", java.time.Instant.parse("2026-11-09T06:40:00Z").toEpochMilli())!!
        val o = AlbumQueue.Outboxes().with("pm@x", "PROJ-T1", AlbumQueue.Stored(queue = listOf(shot.copy(recorded = true), single),
            dropped = listOf(shot.copy(id = "ALB-00000000c002", photo = "ALB-00000000c002", error = AlbumQueue.NO_FILE_WORDS))))
        val text = json(AlbumWire.storedOutbox(o))
        val back = AlbumWire.outboxOf(MiniRead, tree(text))
        assertEquals(o, back)
        assertEquals(o, AlbumWire.outboxOf(OrgRead, org(tree(text))), "the same through an org-json-shaped tree")
        val q = back.of("pm@x", "PROJ-T1").queue
        assertEquals("2026-11-09T06:31:07.250Z", q[0].clientAt)
        assertTrue(q[0].recorded, "its JPEG is next on the next pass")
        assertEquals("units in, \"left\" wall", q[0].caption)
        assertEquals("ALB-0a1b2c3d4e5f", q[1].photo)
        assertEquals("", q[1].caption)
        assertTrue(text.contains(""""photo_kind":"photo","client_at":"2026-11-09T06:31:07.250Z","error":"","recorded":true,"at":"2026-11-09T06:31:07.250Z""""))
    }
}
