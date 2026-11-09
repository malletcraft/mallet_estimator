package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * FACEWRITER'S REFUSALS (port 61c slice M8). Under `Tx.EN` every kind is BYTE-EQUAL to the string
 * FaceWriter's require/error composed at cdde242, so nothing a catcher shows, logs or stores moves
 * under English — AsIsUpload's note off a failed fullPano included; under `Tx.MR` the pins are
 * hand-typed against the table's values (decision 6: Marathi by Claude, the phone is the review).
 */
class FaceNoteTest {

    @Test fun `text under Tx EN is byte-equal to FaceWriter's previous literals`() {
        assertEquals("could not open the selected photo", FaceNote.text(Tx.EN, FaceNote.Kind.OPEN))
        assertEquals("not a decodable image", FaceNote.text(Tx.EN, FaceNote.Kind.NOT_IMAGE))
        // the two-fragment sentence, composed exactly as begin() concatenated it (× and — included)
        assertEquals("not a 360 photo (5760×2880 is not 2:1) — export the 360 from the Insta360 app first",
            FaceNote.text(Tx.EN, FaceNote.Kind.NOT_360, w = 5760, h = 2880))
        assertEquals("could not decode the selected photo", FaceNote.text(Tx.EN, FaceNote.Kind.DECODE_SELECTED))
        assertEquals("could not decode the photo", FaceNote.text(Tx.EN, FaceNote.Kind.DECODE))
        assertEquals("gallery refused the file", FaceNote.text(Tx.EN, FaceNote.Kind.GALLERY))
        assertEquals("could not write to the gallery", FaceNote.text(Tx.EN, FaceNote.Kind.WRITE))
    }

    @Test fun `Refused's message IS the pinned English render, so a logcat line and a stored note keep today's bytes`() {
        assertEquals("could not open the selected photo", FaceNote.Refused(FaceNote.Kind.OPEN).message)
        assertEquals("not a 360 photo (5760×2880 is not 2:1) — export the 360 from the Insta360 app first",
            FaceNote.Refused(FaceNote.Kind.NOT_360, 5760, 2880).message)
        // AsIsUpload's fullPano path stores (e.message ?: simpleName) — this is the message it stores
        assertEquals("could not decode the selected photo", FaceNote.Refused(FaceNote.Kind.DECODE_SELECTED).message)
    }

    @Test fun `line renders a Refused in both languages and any other Throwable as its message, orEmpty`() {
        val r = FaceNote.Refused(FaceNote.Kind.GALLERY)
        assertEquals("gallery refused the file", FaceNote.line(Tx.EN, r))
        assertEquals("गॅलरीने फाइल नाकारली", FaceNote.line(Tx.MR, r))
        assertEquals("360 फोटो नाही (5760×2880 म्हणजे 2:1 नाही) — आधी Insta360 अ‍ॅपमधून 360 एक्सपोर्ट करा",
            FaceNote.line(Tx.MR, FaceNote.Refused(FaceNote.Kind.NOT_360, 5760, 2880)))
        // an alien Throwable keeps the catchers' M6 orEmpty() bytes, whatever the language
        assertEquals("boom", FaceNote.line(Tx.MR, Exception("boom")))
        // the orEmpty choice pinned with a NULL-message Throwable (the M6 review's lesson: a byte claim
        // with a silent exception is the stale-note fault wearing a test) — never "Exception"
        assertEquals("", FaceNote.line(Tx.EN, Exception()))
        assertEquals("", FaceNote.line(Tx.MR, Exception()))
    }

    @Test fun `text under Tx MR is the table's value, hand-typed`() {
        assertEquals("निवडलेला फोटो उघडता आला नाही", FaceNote.text(Tx.MR, FaceNote.Kind.OPEN))
        assertEquals("ही इमेज वाचता येत नाही", FaceNote.text(Tx.MR, FaceNote.Kind.NOT_IMAGE))
        assertEquals("निवडलेला फोटो वाचता आला नाही", FaceNote.text(Tx.MR, FaceNote.Kind.DECODE_SELECTED))
        assertEquals("फोटो वाचता आला नाही", FaceNote.text(Tx.MR, FaceNote.Kind.DECODE))
        assertEquals("गॅलरीत लिहिता आले नाही", FaceNote.text(Tx.MR, FaceNote.Kind.WRITE))
    }
}
