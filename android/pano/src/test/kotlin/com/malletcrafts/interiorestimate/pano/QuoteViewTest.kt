package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.QuoteView.Mode
import com.malletcrafts.interiorestimate.pano.QuoteView.Row
import com.malletcrafts.interiorestimate.pano.QuoteView.ScopeRow
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The approved quote view and the scope sheet (port 61c slice QV), against SPEC61c §4.1 item 3 and §4.2 with §14.6's
 * notes, and index61c2.html (stripHTML `op56`, scopeSheet): the five ticked steps, the Approved chip, the APPROVED
 * SCOPE row in its three states, and the sheet's three modes. The English and the Marathi a pin expects are written
 * out by hand; the mirror's figures are S5's own (S5Quote.ALBUM, S5Album.GET_PM).
 */
class QuoteViewTest {

    private fun tree(s: String): Any? = MiniJson.parse(s)
    private fun view(s: String): Album.View = assertNotNull(AlbumWire.view(MiniRead, tree(s)))

    @Test fun `the five steps are the strip's, every one ticked and none current - and the Approved chip folds the view`() {
        val en = QuoteView.segments(Tx.EN)
        assertEquals(listOf("Survey", "Wish list", "Vendor review", "Estimation", "Revisions"), en.map { it.name })
        assertEquals(listOf(1, 2, 3, 4, 5), en.map { it.n })
        // the plain label, never the `(now)` one: approval is final, no step is current and none is a tap
        assertEquals("Phase 3 of 5: Vendor review", en[2].label)
        assertTrue(en.none { it.label.contains("(now)") })
        val mr = QuoteView.segments(Tx.MR)
        assertEquals(listOf("सर्व्हे", "इच्छा-यादी", "व्हेंडर तपासणी", "अंदाजपत्रक", "फेरबदल"), mr.map { it.name })
        assertEquals("फेज 3/5: व्हेंडर तपासणी", mr[2].label)
        assertEquals("Approved", QuoteView.approvedChip(Tx.EN)); assertEquals("मंजूर", QuoteView.approvedChip(Tx.MR))
        assertEquals("Client approved — in execution. Tap to fold", QuoteView.approvedChipLabel(Tx.EN))
        assertEquals("ग्राहक मंजूर — अंमलबजावणी मध्ये. घडी करण्यासाठी टॅप करा", QuoteView.approvedChipLabel(Tx.MR))
        assertEquals("tap to view", QuoteView.tapToView(Tx.EN)); assertEquals("पाहण्यासाठी टॅप करा", QuoteView.tapToView(Tx.MR))
        assertEquals("APPROVED SCOPE", QuoteView.scopeLabel(Tx.EN)); assertEquals("मंजूर स्कोप", QuoteView.scopeLabel(Tx.MR))
    }

    @Test fun `APPROVED SCOPE with a baseline - n of 16 works, since the stamp in IST, tap to view`() {
        // S5's own album GET: six works approved at 07:01:08 UTC on 3 Nov = 12:31 IST
        val m = view(S5Quote.ALBUM)
        assertEquals(ScopeRow("6 of 16 works · since 3 Nov, 12:31 · tap to view", tap = true, record = false, pmOnly = null),
            QuoteView.scopeRow(Tx.EN, m.scope, m.scopeAt, m.role))
        assertEquals("16 पैकी 6 कामे · 3 Nov, 12:31 पासून · पाहण्यासाठी टॅप करा", QuoteView.scopeRow(Tx.MR, m.scope, m.scopeAt, m.role).text)
        // the PM's GET of the AQ harness: thirteen works since 10:46 IST
        val pm = view(S5Album.GET_PM)
        assertEquals("13 of 16 works · since 3 Nov, 10:46 · tap to view", QuoteView.scopeRow(Tx.EN, pm.scope, pm.scopeAt, pm.role).text)
        // a vendor or a client reads the scope with no stamp (S5 blanks scope_at for them): no since part
        val vendor = view(S5Album.GET_VENDOR)
        assertEquals("", vendor.scopeAt)
        assertEquals("13 of 16 works · tap to view", QuoteView.scopeRow(Tx.EN, vendor.scope, vendor.scopeAt, vendor.role).text)
        assertTrue(QuoteView.scopeRow(Tx.EN, vendor.scope, vendor.scopeAt, vendor.role).tap)
        // the row is the tap whoever looks: anyone can view the scope
        for (role in Album.ROLE_IDS) assertTrue(QuoteView.scopeRow(Tx.EN, listOf(10), "", role).tap, role)
    }

    @Test fun `APPROVED SCOPE with no baseline - none recorded, Record for the PM, the PM-only line for anyone else, alone before the role is read`() {
        assertEquals(ScopeRow("none recorded", tap = false, record = true, pmOnly = null), QuoteView.scopeRow(Tx.EN, null, "", Album.PM))
        for (role in listOf(Album.DESIGNER, Album.VENDOR, Album.CLIENT)) {
            assertEquals(ScopeRow("none recorded", tap = false, record = false, pmOnly = "Only the Project Manager records the approved scope"),
                QuoteView.scopeRow(Tx.EN, null, "", role), role)
        }
        // before the mirror has said the role: none recorded alone -- no button, no line
        assertEquals(ScopeRow("none recorded", tap = false, record = false, pmOnly = null), QuoteView.scopeRow(Tx.EN, null, "", null))
        assertEquals(ScopeRow("नोंदवलेला नाही", tap = false, record = false, pmOnly = "मंजूर स्कोप फक्त प्रोजेक्ट मॅनेजर नोंदवतो"),
            QuoteView.scopeRow(Tx.MR, null, "", Album.DESIGNER))
        // a project at Survey has no baseline: S5's GET says scope null
        val locked = view(S5Album.GET_LOCKED)
        assertNull(locked.scope)
        assertTrue(QuoteView.scopeRow(Tx.EN, locked.scope, locked.scopeAt, locked.role).record)
    }

    @Test fun `the sheet's three modes - title, note, count line and buttons, in both languages`() {
        val at = "2026-11-03T07:01:08.769Z"
        assertEquals("Works in the approved estimate", QuoteView.title(Tx.EN, Mode.APPROVE))
        assertEquals("Works in the approved estimate", QuoteView.title(Tx.EN, Mode.RECORD))
        assertEquals("Approved scope", QuoteView.title(Tx.EN, Mode.VIEW))
        assertEquals("मंजूर अंदाजपत्रकातील कामे", QuoteView.title(Tx.MR, Mode.RECORD)); assertEquals("मंजूर स्कोप", QuoteView.title(Tx.MR, Mode.VIEW))
        // one note per mode, S5 /projects scopeHtml verbatim (review 9)
        assertEquals("Tick every work the client is approving: Move to Client approved sends them as the baseline. Approval is final — after it the scope changes only through change requests.",
            QuoteView.note(Tx.EN, Mode.APPROVE, at))
        assertEquals("Approved before a scope was recorded: tick the works the client approved and Record the approved scope. After that the scope changes only through change requests.",
            QuoteView.note(Tx.EN, Mode.RECORD, at))
        assertEquals("the baseline since 3 Nov, 12:31 — approval is final: the scope changes only through a change request (add, remove or change a work) that the client answers",
            QuoteView.note(Tx.EN, Mode.VIEW, at))
        assertEquals("3 Nov, 12:31 पासूनचा मंजूर स्कोप — मंजुरी अंतिम आहे: स्कोप फक्त ग्राहक उत्तर देईल अशा बदल विनंतीमधूनच बदलतो (काम समाविष्ट करणे, वगळणे किंवा बदलणे)",
            QuoteView.note(Tx.MR, Mode.VIEW, at))
        // the view's note is blank when S5 sent no stamp (a vendor's or a client's mirror), as the prototype draws it
        assertEquals("", QuoteView.note(Tx.EN, Mode.VIEW, ""))
        // the count line counts the sixteen only; the view has none
        assertEquals("2 of 16 works ticked", QuoteView.countLine(Tx.EN, Mode.APPROVE, listOf(10, 999, 20, 10)))
        assertEquals("0 of 16 works ticked", QuoteView.countLine(Tx.EN, Mode.RECORD, emptyList()))
        assertEquals("16 पैकी 2 कामे टिक केली", QuoteView.countLine(Tx.MR, Mode.RECORD, listOf(10, 20)))
        assertNull(QuoteView.countLine(Tx.EN, Mode.VIEW, listOf(10)))
        // the foot: Cancel / Move to Client approved, Cancel / Record the approved scope, Close alone
        assertEquals("Cancel" to "Move to Client approved", QuoteView.closeLabel(Tx.EN, Mode.APPROVE) to QuoteView.submitLabel(Tx.EN, Mode.APPROVE))
        assertEquals("Cancel" to "Record the approved scope", QuoteView.closeLabel(Tx.EN, Mode.RECORD) to QuoteView.submitLabel(Tx.EN, Mode.RECORD))
        assertEquals("Close", QuoteView.closeLabel(Tx.EN, Mode.VIEW)); assertNull(QuoteView.submitLabel(Tx.EN, Mode.VIEW))
        assertEquals("रद्द करा" to "मंजूर स्कोप नोंदवा", QuoteView.closeLabel(Tx.MR, Mode.RECORD) to QuoteView.submitLabel(Tx.MR, Mode.RECORD))
        assertEquals("बंद करा", QuoteView.closeLabel(Tx.MR, Mode.VIEW))
    }

    @Test fun `the sixteen rows in WORKS order - ticked by the ticks, disabled in the view, a tap toggles elsewhere`() {
        val rows = QuoteView.rows(Mode.RECORD, setOf(20, 140))
        assertEquals(Album.WORK_IDS, rows.map { it.id })
        assertEquals(listOf(20, 140), rows.filter { it.on }.map { it.id })
        assertTrue(rows.none { it.disabled })
        assertEquals(Row(20, on = true, disabled = false), rows[1])
        // the view: ticked and disabled, read-only (cb(on, true), aria-disabled)
        val ro = QuoteView.rows(Mode.VIEW, view(S5Quote.ALBUM).scope!!)
        assertEquals(listOf(10, 20, 30, 50, 90, 140), ro.filter { it.on }.map { it.id })
        assertTrue(ro.all { it.disabled })
        assertEquals("Demolition & site preparation", QuoteView.rowName(Tx.EN, 10))
        assertEquals("तोडकाम व साइट तयारी", QuoteView.rowName(Tx.MR, 10))
        // a tap toggles; the view ignores it; an id that is not a work changes nothing
        assertEquals(setOf(10, 20), QuoteView.toggled(Mode.RECORD, setOf(10), 20))
        assertEquals(setOf(20), QuoteView.toggled(Mode.RECORD, setOf(10, 20), 10))
        assertEquals(setOf(10), QuoteView.toggled(Mode.VIEW, setOf(10), 20))
        assertEquals(setOf(10), QuoteView.toggled(Mode.APPROVE, setOf(10), 45))
    }

    @Test fun `submit - the ticks in WORKS order, each once - nothing ticked is refused in S5's words before any call`() {
        assertEquals(listOf(10, 20, 140), QuoteView.submit(listOf(140, 20, 10, 20)))
        assertEquals(listOf(10), QuoteView.submit(setOf(10, 999)))
        assertNull(QuoteView.submit(emptyList()))
        assertNull(QuoteView.submit(listOf(999, 0)))
        assertEquals("Tick the works in the approved estimate before moving to Client approved", QuoteView.emptyRefusal(Tx.EN))
        assertEquals("\"ग्राहक मंजूर\" वर नेण्यापूर्वी मंजूर अंदाजपत्रकातील कामे टिक करा", QuoteView.emptyRefusal(Tx.MR))
        // the same words QuoteMove refuses an empty record with, so the sheet and the mover never say two things
        assertEquals(QuoteView.emptyRefusal(Tx.EN), QuoteMove.localText(Tx.EN, QuoteMove.Local.SCOPE_MISSING))
        assertFalse(QuoteView.emptyRefusal(Tx.MR).isBlank())
    }
}
