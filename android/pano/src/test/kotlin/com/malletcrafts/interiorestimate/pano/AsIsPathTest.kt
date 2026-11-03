package com.malletcrafts.interiorestimate.pano

import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * THE AS-IS UPLOAD'S ADDRESSES AND RECORD (port 61c slice AS1). The three capture addresses, each segment
 * encoded once and the fixed parts never; the record body's keys, order and S5 types (the capture is the device
 * id, `erp_capture` only when ERP named it, `work` always and JSON null when the table says so); `captured_at`
 * byte for byte as 0.3.212 sent it; and, read as source like CaptureStoreRevertTest reads CaptureStore.kt
 * (:app compiles only in CI), the job's order -- record, photos, readings, all under the device id -- the
 * forget on `created` BEFORE the first photo, the photo's and the readings PATCH's 404 that are no end, the
 * record re-sent with the surfaces that landed when a photo is refused, and the pull by the capture's two names.
 */
class AsIsPathTest {

    /** encodeURIComponent for the three characters a project or room name is likeliest to carry. */
    private val enc: (String) -> String = { s -> s.replace("%", "%25").replace(" ", "%20").replace("/", "%2F") }

    private val CAP = "MCAP-20251103-120000-1"

    @Test fun `the three addresses - record, photo, readings - under the capture's device id`() {
        assertEquals(listOf("PROJ-DEMO", "Living Room", "c", CAP), AsIsPath.record("PROJ-DEMO", "Living Room", CAP))
        assertEquals("/api/asis/PROJ-DEMO/Living%20Room/c/$CAP", AsIsPath.path(AsIsPath.record("PROJ-DEMO", "Living Room", CAP), enc))
        assertEquals("/api/asis/PROJ-DEMO/Living%20Room/c/$CAP/img/front", AsIsPath.path(AsIsPath.image("PROJ-DEMO", "Living Room", CAP, "front"), enc))
        assertEquals("/api/asis/PROJ-DEMO/Living%20Room/c/$CAP/readings", AsIsPath.path(AsIsPath.readings("PROJ-DEMO", "Living Room", CAP), enc))
        // the old room address (no capture) is 0.3.212's and is not built here: every address has the /c/<capture> part
        for (segs in listOf(AsIsPath.record("P", "R", "C"), AsIsPath.image("P", "R", "C", "floor"), AsIsPath.readings("P", "R", "C")))
            assertEquals(listOf("c", "C"), segs.subList(2, 4), "$segs")
    }

    @Test fun `each segment is encoded exactly once, verbatim, and the fixed parts never`() {
        // once: a % in a name goes as %25, never %2520; a slash in a project id stays inside its one segment
        assertEquals("/api/asis/Demo%20flat%2F1/A%25B/c/x", AsIsPath.path(AsIsPath.record("Demo flat/1", "A%B", "x"), enc))
        // one call per segment, the segment as given; "/api/asis/" and the slashes are not passed to the encoder
        val seen = ArrayList<String>()
        val p = AsIsPath.path(AsIsPath.image("P 1", "R/2", "C%3", "front")) { seen += it; "<$it>" }
        assertEquals(listOf("P 1", "R/2", "c", "C%3", "img", "front"), seen)
        assertEquals("/api/asis/<P 1>/<R/2>/<c>/<C%3>/<img>/<front>", p)
        assertEquals("/api/asis/", AsIsPath.PREFIX)
    }

    private fun body(erp: String? = "MEST-PH-00012", work: Int? = null, stage: String = "", prep: Any? = mapOf("v" to 3), surfaces: Any? = mapOf("front" to mapOf("a" to 2678.0))) =
        AsIsPath.body(
            project = "PROJ-DEMO", projectTitle = "Demo flat", customer = "CUST-DEMO", room = "Living Room", capture = CAP, erpCapture = erp,
            capturedAt = "2023-11-15T03:43:20+05:30", appVersion = "0.3.280", l = 3319.0, w = 2678.0, h = 2677.0, prep = prep, surfaces = surfaces,
            work = work, stage = stage,
        )

    @Test fun `the record body - S5's keys in order, the capture is the device id, erp_capture only when ERP named it`() {
        val prep = mapOf("v" to 3); val surfaces = mapOf("front" to mapOf("a" to 2678.0, "b" to 2677.0, "image" to true))
        val b = body(prep = prep, surfaces = surfaces)
        assertEquals(listOf("v", "project", "project_title", "customer", "room", "capture", "erp_capture", "captured_at", "app_version",
            "L", "W", "H", "prep", "surfaces", "work"), b.keys.toList())
        assertEquals(1, b["v"])
        assertEquals("PROJ-DEMO", b["project"]); assertEquals("Demo flat", b["project_title"]); assertEquals("CUST-DEMO", b["customer"])
        assertEquals("Living Room", b["room"])
        assertEquals(CAP, b["capture"], "the capture is the DEVICE id, which S5 requires to equal the address")
        assertEquals("MEST-PH-00012", b["erp_capture"], "ERP's name rides beside it, information only")
        assertEquals("2023-11-15T03:43:20+05:30", b["captured_at"]); assertEquals("0.3.280", b["app_version"])
        assertEquals(3319.0, b["L"]); assertEquals(2678.0, b["W"]); assertEquals(2677.0, b["H"])
        assertSame(prep, b["prep"]); assertSame(surfaces, b["surfaces"])
        // no ERP name yet: the key is not sent at all -- the phone invents no name, and S5 ignores the field anyway
        val b2 = body(erp = null)
        assertFalse(b2.containsKey("erp_capture"), "$b2")
        assertEquals(listOf("v", "project", "project_title", "customer", "room", "capture", "captured_at", "app_version",
            "L", "W", "H", "prep", "surfaces", "work"), b2.keys.toList())
        assertEquals(CAP, b2["capture"])
    }

    @Test fun `the record's work is the one filing - Album workOfCapture of the row's work and stage - and a null is SENT as null`() {
        fun work(w: Int?, stage: String): Pair<Boolean, Any?> = body(work = w, stage = stage).let { it.containsKey("work") to it["work"] }
        // a row from before schema 6 (work NULL) files by its stage, through the same table S5 holds
        assertEquals(true to 20, work(null, "Plaster & waterproofing"))
        assertEquals(true to 0, work(null, ""))
        assertEquals(true to 0, work(null, "Site survey"))
        // "not filed to a work": the key is PRESENT with a null, so S5 stores null (not filed: hidden from the client); with
        // the key absent S5 would file the capture at 0, the quote journey, which the client sees (asis.js workOfRecord)
        assertEquals(true to null, work(null, "Material delivered on site"))
        // a row the phone has filed stands as it is, whatever its stage says
        assertEquals(true to 0, work(0, "Plaster & waterproofing"))
        assertEquals(true to 90, work(90, "Plaster & waterproofing"))
        // a stray id is no work: filed by the stage
        assertEquals(true to 20, work(7, "Plaster & waterproofing"))
        assertNull(body(work = null, stage = "Material delivered on site")["work"])
    }

    @Test fun `captured_at is byte for byte what 0 3 212 sent - the shutter instant at the phone's zone, java time's own text`() {
        val ist = ZoneId.of("Asia/Kolkata")
        assertEquals("2023-11-15T03:43:20+05:30", AsIsPath.capturedAt(1_700_000_000_000L, ist))
        assertEquals("2023-11-15T03:43:20.123+05:30", AsIsPath.capturedAt(1_700_000_000_123L, ist), "milliseconds when there are any")
        assertEquals("2023-11-15T03:44+05:30", AsIsPath.capturedAt(1_700_000_040_000L, ist), "no seconds when they are zero: java.time's rule, and what S5 holds")
        assertEquals("2023-11-14T22:13:20Z", AsIsPath.capturedAt(1_700_000_000_000L, ZoneId.of("UTC")))
        // and it IS the expression every build since 0.3.212 ran (S5's C3 matches the text, so this must never be tidied)
        for (ms in listOf(1_700_000_000_000L, 1_700_000_000_123L, 1_700_000_040_000L, 0L))
            assertEquals(Instant.ofEpochMilli(ms).atZone(ist).toOffsetDateTime().toString(), AsIsPath.capturedAt(ms, ist))
    }

    // ---- :app, read as source (CaptureStoreRevertTest's way: only CI compiles :app) -----------------------------

    private fun app(name: String): String {
        val f = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/$name")
        assertTrue(f.isFile, "the app's $name is beside :pano (${f.absolutePath})")
        return f.readText()
    }

    /** The offset of [needle] in [src], which must be there exactly once. */
    private fun once(src: String, needle: String, where: String): Int {
        val i = src.indexOf(needle)
        assertTrue(i >= 0, "$where: `$needle` is not in the source")
        assertEquals(-1, src.indexOf(needle, i + 1), "$where: `$needle` is in the source more than once")
        return i
    }

    /** Every offset of [needle] in [src], in order. */
    private fun all(src: String, needle: String): List<Int> =
        Regex(Regex.escape(needle)).findAll(src).map { it.range.first }.toList()

    private fun pano(name: String): String {
        val f = File("src/main/kotlin/com/malletcrafts/interiorestimate/pano/$name")
        assertTrue(f.isFile, "the pano's $name (${f.absolutePath})")
        return f.readText()
    }

    @Test fun `the upload job sends the record first, then the photos, then the readings, all under the device id (AsIsUpload kt)`() {
        val src = app("AsIsUpload.kt")
        val record = once(src, "url(AsIsPath.record(c.project, c.room, c.deviceId))", "the record PUT")
        val image = once(src, "url(AsIsPath.image(c.project, c.room, c.deviceId, s))", "a surface photo's PUT")
        val push = once(src, "ReadingsSync.push(ctx, id, auth, c.project, c.room, c.deviceId,", "the readings push")
        assertTrue(record < image && image < push, "record at $record, photo at $image, readings at $push: the order is record, photos, readings")
        // the body is AsIsPath's, the capture its device id, captured_at the pinned expression on the phone's own zone
        once(src, "capture = c.deviceId, erpCapture = c.serverName,", "the record body")
        once(src, "capturedAt = AsIsPath.capturedAt(c.createdAt, ZoneId.systemDefault()),", "captured_at")
        once(src, "work = c.work, stage = c.stage,", "the filing")
        assertFalse(src.contains("c.serverName ?: c.deviceId"), "the record is named by the device id, never by the ERP name")
        assertFalse(src.contains("\"/api/asis/\""), "no address is built by hand: AsIsPath holds them")
        // `created` is read from the record's answer and the seen state forgotten AT ONCE -- before the first photo, not
        // after them (port item 4; the AS1 review, 3 Nov 2026): a photo that fails runs the job again from the record,
        // whose second PUT is never `created`, so a forget placed after the photos would then never happen and the next
        // pull would delete the readings on the phone as "gone from the web"
        val created = once(src, "answer.optBoolean(\"created\", false)", "created")
        val forget = once(src, "if (created) ReadingsSync.forget(ctx, id)", "the forget")
        assertTrue(record < created && created < forget && forget < image, "created read at $created, forget at $forget, photo at $image: the forget comes before the first photo")
        assertTrue(forget < push, "forget at $forget, push at $push")
        // the record PUT is ONE function, sent first and again after a refused photo, and the forget is inside it
        val fn = once(src, "fun putRecord() {", "the record PUT")
        val firstCall = src.indexOf("putRecord()", fn + "fun putRecord() {".length)
        assertTrue(fn < created && forget < firstCall, "the forget (at $forget) is inside putRecord (at $fn), before its first call (at $firstCall)")
        // a 404 is the record not being on the web -- at a photo AND at the readings PATCH: the job runs again from the
        // record, it is not refused and not "done" with "web readings not available yet"
        val missing = all(src, "if (e.code == 404) throw AsIsUpload.RecordMissing(e)")
        assertEquals(2, missing.size, "the 404 is judged at the photo PUT and at the readings PATCH: $missing")
        assertTrue(image < missing[0] && missing[0] < push && push < missing[1], "photo at $image, push at $push, the 404s at $missing")
        assertFalse(src.contains("ReadingsSync.notAvailable"), "the push never reads a 404 as NOT_YET: that reading is the pull's alone")
        once(src, "} catch (e: AsIsUpload.RecordMissing) {", "RecordMissing is caught by name")
        assertTrue(Regex("""catch \(e: AsIsUpload\.RecordMissing\) \{[^}]*"error"[^}]*Result\.retry\(\)""").containsMatchIn(src), "RecordMissing: state \"error\" and Result.retry()")
        // a REFUSED photo (the job ends, nothing retries it) re-sends the record with only the surfaces that landed
        // marked `image: true`, then stays the photo's refusal -- so no record on the web claims a photo R2 does not hold
        val refused = Regex("""if \(e\.refused\) \{\s*for \(t in frames\.keys\) if \(t !in landed\) surfaces\.optJSONObject\(t\)\?\.put\("image", false\)\s*putRecord\(\)\s*}\s*throw e""")
            .find(src)
        assertTrue(refused != null && image < refused.range.first && refused.range.first < missing[1], "a refused photo re-sends the record with the surfaces that landed, then rethrows")
        assertTrue(src.indexOf("landed += s") > refused!!.range.last, "a surface is landed only after its PUT returned")
    }

    @Test fun `the comments say what an absent work does on S5 - filed at 0, the quote journey, which the client sees`() {
        // the first version said an absent `work` would make S5 "file by a stage the record does not carry", which cannot
        // happen (workOfRecord with no stage answers 0): a reader could have tidied the null away believing nothing changes
        for ((name, src) in listOf("AsIsPath.kt" to pano("AsIsPath.kt"), "AsIsUpload.kt" to app("AsIsUpload.kt"))) {
            assertTrue(src.contains("0, the quote"), "$name names the outcome of an absent work: 0, the quote journey")
            assertFalse(src.contains("file by a `stage`") || src.contains("file it from `stage`"), "$name: the stated hazard was wrong")
        }
    }

    @Test fun `the sync names the capture, and survey prep pulls by its two names (ReadingsSync kt, SurveyPrepScreen kt)`() {
        val rs = app("ReadingsSync.kt")
        once(rs, "AsIsPath.path(AsIsPath.readings(project, room, capture))", "the readings address")
        assertFalse(rs.contains("\"/readings\""), "no room-level readings address is built any more")
        once(rs, "fun fetch(auth: String, project: String, room: String, capture: String, erpCapture: String? = null)", "fetch takes the capture and its ERP name")
        once(rs, "if (e.code == 404 && older != null) get(auth, project, room, older) else throw e", "a 404 on the device id asks the ERP name once; anything else throws")
        once(rs, "fun push(context: Context, id: String, auth: String, project: String, room: String, capture: String,", "push takes the capture")
        once(rs, "fun forget(context: Context, id: String)", "forget")
        assertTrue(Regex("""fun forget\(context: Context, id: String\) \{\s*update\(context, id\) \{ st -> st\.seen\.clear\(\); st\.conflicts\.clear\(\) \}""").containsMatchIn(rs), "forget clears the seen baseline and the waiting conflicts")
        val sp = app("SurveyPrepScreen.kt")
        once(sp, "ReadingsSync.fetch(auth, row.project, row.room, row.deviceId, row.serverName)", "survey prep's pull")
    }
}
