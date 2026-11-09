package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * THE X3'S WORDS (port 61c slice M6). `text(Tx.EN, …)` is BYTE-EQUAL to the literals X3Bridge
 * composed at 80d3526 for every [CameraNote.Kind], the composed connect-error sentence included,
 * and [CameraNote.Refused]'s message IS the `Tx.EN` render (the PlanSave pin) — so a logcat line
 * reads exactly as before M6. Under `Tx.MR` the pins are the card's values.
 */
class CameraNoteTest {

    @Test fun `text under Tx EN is byte-equal to X3Bridge's previous literals, for every kind`() {
        assertEquals("camera reported capture finished but returned no file",
            CameraNote.text(Tx.EN, CameraNote.Kind.NO_FILE))
        assertEquals("capture error 102", CameraNote.text(Tx.EN, CameraNote.Kind.CAPTURE_ERROR, 102))
        assertEquals("stitch export failed (7): mesh export failed",
            CameraNote.text(Tx.EN, CameraNote.Kind.STITCH_FAILED, 7, "mesh export failed"))
        // the SDK can hand onFail a null message; the old template printed "null", and so does this
        assertEquals("stitch export failed (7): null", CameraNote.text(Tx.EN, CameraNote.Kind.STITCH_FAILED, 7, null))
        assertEquals("stitch export cancelled", CameraNote.text(Tx.EN, CameraNote.Kind.STITCH_CANCELLED))
    }

    @Test fun `the composed connect-error sentence is one key, byte for byte`() {
        assertEquals("camera connect error 1 — check the X3 is on, its Wi-Fi is joined in Android " +
            "Settings, and no other app (Insta360 app) is holding the camera",
            CameraNote.connectError(1)(Tx.EN))
    }

    @Test fun `Refused's message is the pinned Tx EN render`() {
        assertEquals("capture error 102", CameraNote.Refused(CameraNote.Kind.CAPTURE_ERROR, 102).message)
        assertEquals("stitch export cancelled", CameraNote.Refused(CameraNote.Kind.STITCH_CANCELLED).message)
        assertEquals("stitch export failed (7): mesh export failed",
            CameraNote.Refused(CameraNote.Kind.STITCH_FAILED, 7, "mesh export failed").message)
        assertEquals("camera reported capture finished but returned no file",
            CameraNote.Refused(CameraNote.Kind.NO_FILE).message)
    }

    @Test fun `joinWifi wraps each of the three problems`() {
        assertEquals("phone is not on the camera's Wi-Fi — join the X3's Wi-Fi in Settings, then try again",
            CameraNote.joinWifi(CameraNote.notOnCameraWifi)(Tx.EN))
        assertEquals("cannot watch Wi-Fi: boom — join the X3's Wi-Fi in Settings, then try again",
            CameraNote.joinWifi(CameraNote.cannotWatchWifi("boom"))(Tx.EN))
        // the no-ConnectivityManager problem is a system identifier, a value under both languages
        val noCm: Msg = { _ -> CameraNote.NO_CM }
        assertEquals("no ConnectivityManager — join the X3's Wi-Fi in Settings, then try again",
            CameraNote.joinWifi(noCm)(Tx.EN))
        assertEquals("no ConnectivityManager — Settings मध्ये X3 च्या Wi-Fi ला जोडा, मग पुन्हा प्रयत्न करा",
            CameraNote.joinWifi(noCm)(Tx.MR))
    }

    @Test fun `line renders a Refused in the live language and keeps an alien throwable's message`() {
        val r = CameraNote.Refused(CameraNote.Kind.STITCH_CANCELLED)
        assertEquals("stitch export cancelled", CameraNote.line(Tx.EN, r))
        assertEquals("स्टिच एक्सपोर्ट रद्द झाले", CameraNote.line(Tx.MR, r))
        assertEquals("boom", CameraNote.line(Tx.MR, IllegalStateException("boom")))
        assertEquals("IllegalStateException", CameraNote.line(Tx.MR, IllegalStateException()))
    }

    @Test fun `under Tx MR the notes are the card's values`() {
        assertEquals("फोन कॅमेऱ्याच्या Wi-Fi वर नाही", CameraNote.notOnCameraWifi(Tx.MR))
        assertEquals("Wi-Fi वर लक्ष ठेवता येत नाही: boom", CameraNote.cannotWatchWifi("boom")(Tx.MR))
        assertEquals("कॅमेरा कनेक्ट त्रुटी 1 — X3 चालू आहे, त्याचे Wi-Fi Android Settings मध्ये जोडले आहे आणि दुसरे कोणतेही अ‍ॅप (Insta360 अ‍ॅप) कॅमेरा वापरत नाही ना ते तपासा", CameraNote.connectError(1)(Tx.MR))
        assertEquals("कॅमेऱ्याने कॅप्चर पूर्ण झाल्याचे सांगितले, पण फाइल दिली नाही", CameraNote.text(Tx.MR, CameraNote.Kind.NO_FILE))
        assertEquals("कॅप्चर त्रुटी 102",
            CameraNote.text(Tx.MR, CameraNote.Kind.CAPTURE_ERROR, 102))
        assertEquals("स्टिच एक्सपोर्ट झाले नाही (7): mesh export failed",
            CameraNote.text(Tx.MR, CameraNote.Kind.STITCH_FAILED, 7, "mesh export failed"))
    }
}
