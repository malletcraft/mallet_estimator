package com.malletcrafts.interiorestimate.pano

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * THE QUOTE STAGE ON THE PROJECT SCREEN (v52 item 16; Amit, 14 Oct 2026, 06:53 IST,
 * the stages: "survey, client wish list, vendor quoted, client approved, estimated").
 * SIX SINCE v61b (Amit approved the prototype on 26 Oct 2026): Estimated and Client approved
 * swapped and Revisions put between them -- the estimate is made, the designer, client and
 * vendors revise it, the client approves, and only then does execution begin. S5 QUOTE's
 * list changed the same day (functions/_lib/asis.js); the phone shows what S5 answers.
 *
 * The COMMERCIAL state of the job, held by S5 QUOTE (one per project, set by staff on
 * /projects) -- not the ERP work stage the PROJECT IS AT card shows. The phone only reads
 * it and never moves it. This is the pure half: what the phone keeps per project ([Cache])
 * and the line it draws from that ([lineFor]), worded as decided on 15 Oct (decision 1):
 * 404 / not listed -> "not on S5 yet"; 200 with no time -> "<stage> · not set on S5 yet";
 * 200 with a time -> "since d MMM, HH:mm IST on S5" (en-GB short months, so September
 * is "Sept" as the prototype prints it); the read time on the second line only
 * when the cached copy is older than today (IST).
 */
object QuoteStage {

    /** S5's QUOTE_STAGES (functions/_lib/asis.js), in order. QuoteStageTest holds the two equal. */
    val STAGES: List<String> = listOf("Survey", "Client wish list", "Vendor quoted", "Estimated", "Revisions", "Client approved")

    /** The last stage: the client has approved the estimate and execution begins (v61b). */
    const val APPROVED = "Client approved"

    const val TITLE = "QUOTE STAGE"
    const val NOT_READ = "not read yet"
    const val UNREAD = "Quote stage not read yet — S5 decides when online"
    const val NOT_ON_S5 = "not on S5 yet"
    const val NOT_SENT = "not on S5 yet — this project has not been sent"
    const val NOT_SET = "not set on S5 yet"

    val IST: ZoneId = ZoneId.of("Asia/Kolkata")

    // Fixed, not the locale's, so the line does not change with the handset's locale data --
    // but spelled as the prototype shows it: its en-GB `month:'short'` gives "Sept" for
    // September in Chrome and WebView, and the prototype is the spec character for character.
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec")

    /**
     * What the phone keeps for one project after reading S5's list. [listed] false means the
     * caller's list did not carry the project (never uploaded, or not visible to this person);
     * [at] is S5's quote_stage_at ("" when never set), [readAt] when THIS phone's read of it went out (UTC ISO).
     */
    data class Cache(
        val listed: Boolean,
        val stage: String = "",
        val by: String = "",
        val at: String = "",
        val readAt: String = "",
    )

    /** One project as S5's GET /api/asis lists it. */
    data class Listed(val project: String, val stage: String, val by: String, val at: String)

    /** The drawn line: the title, how many of the six pips are lit, the second line, and whether it carries the read time. */
    data class Line(val title: String, val lit: Int, val second: String, val showRead: Boolean)

    /** The 0-based place of [stage] in [STAGES], or -1 when it is not one of them. */
    fun indexOf(stage: String): Int = STAGES.indexOf(stage)

    /** The time [iso] names (UTC "Z" or an offset), or null when it is blank or not a time. */
    fun instantOf(iso: String): Instant? {
        if (iso.isBlank()) return null
        return try { OffsetDateTime.parse(iso.trim()).toInstant() } catch (e: Exception) { null }
    }

    /** "14 Oct, 08:08" in IST. */
    fun dayTime(t: Instant): String {
        val z = t.atZone(IST)
        return "${z.dayOfMonth} ${MONTHS[z.monthValue - 1]}, ${hhmm(z)}"
    }

    private fun hhmm(z: ZonedDateTime): String =
        z.hour.toString().padStart(2, '0') + ":" + z.minute.toString().padStart(2, '0')

    /** "since 14 Oct, 08:08 IST on S5" for S5's quote_stage_at, or null when it holds no time. */
    fun since(at: String): String? = instantOf(at)?.let { "since ${dayTime(it)} IST on S5" }

    /** True when [readAt] falls on a day before [now]'s day, both in IST (decision 1). */
    fun readIsOld(readAt: String, now: Instant): Boolean {
        val r = instantOf(readAt) ?: return false
        return r.atZone(IST).toLocalDate().isBefore(now.atZone(IST).toLocalDate())
    }

    /** The line for a project. [cache] null: the phone has never read the stage. */
    fun lineFor(cache: Cache?, now: Instant): Line {
        if (cache == null) return Line("$TITLE · $NOT_READ", 0, UNREAD, false)
        val old = readIsOld(cache.readAt, now)
        val read = if (old) " · read ${hhmm(instantOf(cache.readAt)!!.atZone(IST))}" else ""
        if (!cache.listed) return Line("$TITLE · $NOT_ON_S5", 0, NOT_SENT + read, old)
        val stage = cache.stage.trim()
        if (stage.isEmpty()) return Line("$TITLE · $NOT_READ", 0, UNREAD + read, old)
        // S5 answers only its six names; a name the phone does not know is shown as S5 wrote
        // it, with no pip lit, rather than guessed onto one of the six.
        val lit = indexOf(stage) + 1
        val second = since(cache.at) ?: "$stage · $NOT_SET"
        return Line("$TITLE · $stage", lit, second + read, old)
    }

    /** A project only on this phone (no ERP id): it cannot be on S5, and S5 is never asked. */
    fun phoneOnly(): Line = Line("$TITLE · $NOT_ON_S5", 0, NOT_SENT, false)

    /**
     * The cache after one read of S5's list at [readAt]: every listed project as listed, and
     * every [known] project the list did not carry as not listed. Blank ids are ignored.
     */
    fun fromList(listed: List<Listed>, known: Collection<String>, readAt: String): Map<String, Cache> {
        val out = linkedMapOf<String, Cache>()
        for (k in known) if (k.isNotBlank()) out[k] = Cache(listed = false, readAt = readAt)
        for (p in listed) if (p.project.isNotBlank())
            out[p.project] = Cache(listed = true, stage = p.stage, by = p.by, at = p.at, readAt = readAt)
        return out
    }

    /**
     * The writes of one read of S5's list, as a COMPARE-AND-SET PER ID: [seen] is every id's copy as
     * it was stored when the GET went out, [current] as it is stored now (an id absent from a map was
     * not cached then). Only an id nothing wrote in between takes the read; an id written meanwhile
     * keeps what is stored, since that is newer than this GET -- an answer that named the stage
     * ([named]: the wish list's `final` 409 after an Approve) or a read that went out later and
     * landed first. Without it a project-open read that went out before the Approve could land after
     * the 409 and put Revisions back, unlocking the list under S5's "…; not saved" (slice W fix
     * review). [readAt] is when the GET went OUT, so a slow answer is not dated as fresh. The same
     * rule as WishList.afterGet for the list itself.
     */
    fun fromList(
        listed: List<Listed>, known: Collection<String>, readAt: String,
        seen: Map<String, Cache>, current: Map<String, Cache>,
    ): Map<String, Cache> = fromList(listed, known, readAt).filter { (id, _) -> current[id] == seen[id] }

    /**
     * The cache after an answer of S5's that NAMED the project's stage without who set it or when:
     * the wish list PUT's refusals for the stage -- the `final` 409 at Client approved and the 409
     * at Survey -- carry `quote_stage` and nothing more (wishView, asis.js). The phone keeps ONE
     * copy of the stage, so the answer goes into it, and the Wish list, the project line and every
     * other reader agree with what S5 has just said (port 61c slice W review: an Approve landing
     * between the sync's read of the list and its PUT left the Wish list editable under S5's
     * "not saved", and each edit there was thrown away by the next sync). Null when nothing
     * changes: the cache already says [stage], or [stage] is not one of the six. Otherwise the
     * project is listed at [stage], read at [readAt], its `by` and `at` unknown ("") until the next
     * read of S5's list fills them in.
     */
    fun named(cache: Cache?, stage: String, readAt: String): Cache? {
        val s = stage.trim()
        if (indexOf(s) < 0) return null
        if (cache != null && cache.listed && cache.stage.trim() == s) return null
        return Cache(listed = true, stage = s, readAt = readAt)
    }
}
