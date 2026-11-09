package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * THE AS-IS UPLOAD'S STORED NOTES AND THEIR LINES (port 61c slice M2). Three contracts:
 * the CONSTANTS' bytes are what 0.3.299 and every earlier build stored (a moved constant that
 * changed a byte orphans every state file on the phone); `statusLine(Tx.EN, …)` is BYTE-EQUAL to
 * the strings AsIsUpload.status composed before M2; and under `Tx.MR` every line is the card's
 * value — while a note that matches nothing is drawn AS STORED, never guessed at (the M4 rule).
 */
class UploadNoteTest {

    private fun st(state: String, note: String? = null, image: Boolean = true, hm: String = "14:05") =
        UploadNote.Status(state, hm, note, image, ok = state == "done")

    @Test fun `the stored notes are the bytes every build wrote`() {
        // pinned against the values 0.3.299 stored, byte for byte
        assertEquals("not signed in - open the menu and sign in", UploadNote.NOT_SIGNED_IN)
        assertEquals("no survey prep saved for this capture", UploadNote.NO_PREP)
        assertEquals("not enough memory to square the 360 — will try again", UploadNote.NO_MEMORY)
        assertEquals("this capture is no longer on the phone", UploadNote.CAPTURE_GONE)
        assertEquals("waiting for the capture to be filed", UploadNote.WAITING_FILED)
        assertEquals("waiting for the project to reach ERP", UploadNote.WAITING_PROJECT)
        assertEquals("the web refused the saved password", UploadNote.WEB_REFUSED_PASSWORD)
        assertEquals("web session expired - renewing when the web answers", UploadNote.SESSION_EXPIRED)
        assertEquals("web session renewed", UploadNote.SESSION_RENEWED)
        assertEquals("web session to be renewed when the web answers", UploadNote.SESSION_TO_RENEW)
        assertEquals("web sign-in refused - sending with the ERP key", UploadNote.SIGNIN_REFUSED_KEY)
        assertEquals("on this phone only - save its survey prep to send it to the web", UploadNote.PHONE_ONLY_SAVE_PREP)
        assertEquals("on this phone only - the web takes a 360's survey prep, not a photo", UploadNote.PHONE_ONLY_NOT_PHOTO)
        assertEquals("pano file missing on device", UploadNote.PANO_MISSING)
        assertEquals("sign in again (menu → Server) - ", UploadNote.SIGN_AGAIN_PREFIX)
        assertEquals("readings not sent: ", UploadNote.READINGS_NOT_SENT_PREFIX)
        assertEquals("web answered ", UploadNote.WEB_ANSWERED_PREFIX)
    }

    @Test fun `statusLine under Tx EN is byte-equal to the strings status composed before M2`() {
        assertEquals("On web ✓ 14:05", UploadNote.statusLine(Tx.EN, st("done")))
        assertEquals("On web ✓ 14:05 · no photos, the 360 is not on this phone",
            UploadNote.statusLine(Tx.EN, st("done", image = false)))
        assertEquals("On web ✓ 14:05 · F-M1.len changed on the web by staff@x",
            UploadNote.statusLine(Tx.EN, st("done", note = "F-M1.len changed on the web by staff@x")))
        assertEquals("On web ✓ 14:05 · no photos, the 360 is not on this phone · FK1.t changed on the web",
            UploadNote.statusLine(Tx.EN, st("done", note = "FK1.t changed on the web", image = false)))
        assertEquals("Not on web: web answered 503: Not staff",
            UploadNote.statusLine(Tx.EN, st("error", note = "web answered 503: Not staff")))
        assertEquals("Not on web: not signed in - open the menu and sign in",
            UploadNote.statusLine(Tx.EN, st("refused", note = UploadNote.NOT_SIGNED_IN)))
        // an error with NO stored note renders the upload-failed default (the M2 critic's second round)
        assertEquals("Not on web: upload failed", UploadNote.statusLine(Tx.EN, st("error")))
        assertEquals("Not on web: upload failed", UploadNote.statusLine(Tx.EN, st("refused", note = "")))
        assertEquals("Waiting to upload", UploadNote.statusLine(Tx.EN, st("waiting")))
        assertEquals("Waiting to upload · waiting for the project to reach ERP",
            UploadNote.statusLine(Tx.EN, st("waiting", note = UploadNote.WAITING_PROJECT)))
    }

    @Test fun `statusLine under Tx MR is the card's values`() {
        assertEquals("वेबवर ✓ 14:05", UploadNote.statusLine(Tx.MR, st("done")))
        assertEquals("वेबवर ✓ 14:05 · फोटो नाहीत, 360 या फोनवर नाही",
            UploadNote.statusLine(Tx.MR, st("done", image = false)))
        assertEquals("वेबवर नाही: साइन इन केलेले नाही - मेनू उघडा आणि साइन इन करा",
            UploadNote.statusLine(Tx.MR, st("refused", note = UploadNote.NOT_SIGNED_IN)))
        assertEquals("वेबवर नाही: अपलोड झाले नाही", UploadNote.statusLine(Tx.MR, st("error")))
        assertEquals("अपलोड बाकी", UploadNote.statusLine(Tx.MR, st("waiting")))
        assertEquals("अपलोड बाकी · प्रोजेक्ट ERP मध्ये पोहोचण्याची वाट पाहत आहे",
            UploadNote.statusLine(Tx.MR, st("waiting", note = UploadNote.WAITING_PROJECT)))
    }

    @Test fun `line renders every constant under Tx MR`() {
        val mr = mapOf(
            UploadNote.NOT_SIGNED_IN to "साइन इन केलेले नाही - मेनू उघडा आणि साइन इन करा",
            UploadNote.NO_PREP to "या कॅप्चरसाठी सर्व्हे तयारी सेव्ह केलेली नाही",
            UploadNote.NO_MEMORY to "360 सरळ करण्यासाठी पुरेशी मेमरी नाही — पुन्हा प्रयत्न होईल",
            UploadNote.CAPTURE_GONE to "हे कॅप्चर आता फोनवर नाही",
            UploadNote.WAITING_FILED to "कॅप्चर फाइल होण्याची वाट पाहत आहे",
            UploadNote.WAITING_PROJECT to "प्रोजेक्ट ERP मध्ये पोहोचण्याची वाट पाहत आहे",
            UploadNote.WEB_REFUSED_PASSWORD to "वेबने सेव्ह केलेला पासवर्ड नाकारला",
            UploadNote.SESSION_EXPIRED to "वेब सेशन संपले - वेब उत्तर देईल तेव्हा नवीन होईल",
            UploadNote.SESSION_RENEWED to "वेब सेशन नवीन झाले",
            UploadNote.SESSION_TO_RENEW to "वेब उत्तर देईल तेव्हा वेब सेशन नवीन होईल",
            UploadNote.SIGNIN_REFUSED_KEY to "वेब साइन-इन नाकारले - ERP कीने पाठवत आहे",
            UploadNote.PHONE_ONLY_SAVE_PREP to "फक्त या फोनवर - वेबवर पाठवण्यासाठी त्याची सर्व्हे तयारी सेव्ह करा",
            UploadNote.PHONE_ONLY_NOT_PHOTO to "फक्त या फोनवर - वेब 360 ची सर्व्हे तयारी घेते, फोटो नाही",
            UploadNote.PANO_MISSING to "360 ची फाइल फोनवर नाही",
        )
        for ((stored, want) in mr) assertEquals(want, UploadNote.line(Tx.MR, stored), stored)
        // and under English every constant renders ITSELF, byte for byte
        for (stored in mr.keys) assertEquals(stored, UploadNote.line(Tx.EN, stored))
    }

    @Test fun `the two nested prefixes run their tail back through line`() {
        assertEquals("पुन्हा साइन इन करा (मेनू → सर्व्हर) - वेबने सेव्ह केलेला पासवर्ड नाकारला",
            UploadNote.line(Tx.MR, UploadNote.SIGN_AGAIN_PREFIX + UploadNote.WEB_REFUSED_PASSWORD))
        assertEquals("रीडिंग पाठवले नाहीत: वेबचे उत्तर 401: Not staff",
            UploadNote.line(Tx.MR, UploadNote.READINGS_NOT_SENT_PREFIX + "web answered 401: Not staff"))
        // and byte-identically in English
        assertEquals("sign in again (menu → Server) - the web refused the saved password",
            UploadNote.line(Tx.EN, UploadNote.SIGN_AGAIN_PREFIX + UploadNote.WEB_REFUSED_PASSWORD))
    }

    @Test fun `web answered keeps S5's own tail as given`() {
        assertEquals("वेबचे उत्तर 401: Not staff", UploadNote.line(Tx.MR, "web answered 401: Not staff"))
        assertEquals("वेबचे उत्तर 503", UploadNote.line(Tx.MR, "web answered 503"))
        // what follows the prefix but is not a status code is drawn as stored
        assertEquals("web answered nothing useful", UploadNote.line(Tx.MR, "web answered nothing useful"))
    }

    // ---- the M6 block: the S5 sign-in refusals (port 61c slice M6) ----

    @Test fun `the sign-in constants are the bytes 0 3 303 stored`() {
        // pinned against what S5Session wrote into prefs (`refused`) and AsIsUpload folded into a
        // capture's note at 0.3.303, byte for byte — a changed byte orphans every stored refusal
        assertEquals("Email and password are needed", UploadNote.EMAIL_PASSWORD_NEEDED)
        assertEquals("web unreachable", UploadNote.WEB_UNREACHABLE)
        assertEquals("ERP did not accept that email and password", UploadNote.ERP_NOT_ACCEPT)
        assertEquals("the web did not start a session", UploadNote.WEB_NO_SESSION)
        assertEquals("signed out meanwhile", UploadNote.SIGNED_OUT_MEANWHILE)
        assertEquals("Only MCFT staff can send captures from this app; this ERP account is ", UploadNote.ONLY_STAFF_IS)
        assertEquals("Only MCFT staff can send captures from this app; this ERP account is not staff",
            UploadNote.onlyStaff(""))
        assertEquals("Only MCFT staff can send captures from this app; this ERP account is a vendor account",
            UploadNote.onlyStaff("vendor"))
    }

    @Test fun `line renders each sign-in refusal under both Tx`() {
        val pins = mapOf(
            UploadNote.EMAIL_PASSWORD_NEEDED to "ईमेल आणि पासवर्ड दोन्ही हवेत",
            UploadNote.WEB_UNREACHABLE to "वेबशी संपर्क होत नाही",
            UploadNote.ERP_NOT_ACCEPT to "ERP ने तो ईमेल आणि पासवर्ड स्वीकारला नाही",
            UploadNote.WEB_NO_SESSION to "वेबने सेशन सुरू केले नाही",
            UploadNote.SIGNED_OUT_MEANWHILE to "दरम्यान साइन आउट झाले",
        )
        for ((stored, want) in pins) assertEquals(want, UploadNote.line(Tx.MR, stored), stored)
        for (stored in pins.keys) assertEquals(stored, UploadNote.line(Tx.EN, stored))
    }

    @Test fun `the only-staff refusal round-trips — one composer, one parser, both Tx`() {
        // owner and parser one file (the conflictNoteLine idiom): line(tx, onlyStaff(kind)) is the
        // key's render for a blank and a named kind; under English it is the stored bytes themselves
        assertEquals("या अ‍ॅपमधून कॅप्चर फक्त MCFT स्टाफच पाठवू शकतो; हे ERP खाते स्टाफचे नाही", UploadNote.line(Tx.MR, UploadNote.onlyStaff("")))
        assertEquals("या अ‍ॅपमधून कॅप्चर फक्त MCFT स्टाफच पाठवू शकतो; हे ERP खाते vendor प्रकारचे आहे",
            UploadNote.line(Tx.MR, UploadNote.onlyStaff("vendor")))
        assertEquals("Only MCFT staff can send captures from this app; this ERP account is not staff",
            UploadNote.line(Tx.EN, UploadNote.onlyStaff("")))
        assertEquals("Only MCFT staff can send captures from this app; this ERP account is a client account",
            UploadNote.line(Tx.EN, UploadNote.onlyStaff("client")))
        // a tail neither composed form carries is drawn AS STORED, never guessed at
        assertEquals(UploadNote.ONLY_STAFF_IS + "something else",
            UploadNote.line(Tx.MR, UploadNote.ONLY_STAFF_IS + "something else"))
    }

    @Test fun `the sign-again tail now translates whole through the nested line`() {
        // M2 built the pipe (the nested SIGN_AGAIN parse); M6 fills the last constants — the queue
        // rows' and web line's sign-in tails translate with no new wiring
        assertEquals("पुन्हा साइन इन करा (मेनू → सर्व्हर) - ERP ने तो ईमेल आणि पासवर्ड स्वीकारला नाही",
            UploadNote.line(Tx.MR, UploadNote.SIGN_AGAIN_PREFIX + UploadNote.ERP_NOT_ACCEPT))
        assertEquals("sign in again (menu → Server) - ERP did not accept that email and password",
            UploadNote.line(Tx.EN, UploadNote.SIGN_AGAIN_PREFIX + UploadNote.ERP_NOT_ACCEPT))
        // S5's OWN error sentence is data: the head translates, the tail stays as S5 sent it
        assertEquals("पुन्हा साइन इन करा (मेनू → सर्व्हर) - User disabled. Ask the office.",
            UploadNote.line(Tx.MR, UploadNote.SIGN_AGAIN_PREFIX + "User disabled. Ask the office."))
    }

    @Test fun `an unknown note is drawn as stored, and the stored conflict note re-renders`() {
        assertEquals("the bench said no", UploadNote.line(Tx.MR, "the bench said no"))
        // the done state's tail is ReadingsSync.push's stored conflictText(Tx.EN, …): its owner re-renders it
        assertEquals("F-M1.len वेबवर staff@x यांनी बदलले; FK1.t वेबवर बदलले",
            UploadNote.line(Tx.MR, "F-M1.len changed on the web by staff@x; FK1.t changed on the web"))
    }
}
