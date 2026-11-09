package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Masters
import com.malletcrafts.interiorestimate.pano.i18n.Tx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * THE M2 LOOKUPS UNDER मराठी (the M3a fix-7 reason): every Masters map passes an unknown value
 * through unchanged, so a lookup wired to the wrong map or fed the wrong identity prints the same
 * English in every `Tx.EN` pin and leaks Latin only under मराठी — the PL review's fault. Each pin
 * here is hand-typed against the strings the served prototype v61c.2 answered at the Mi A3 frame
 * (§24's derivation: the queue screenshots and the 116 vis() probes).
 */
class TreeQueueMarathiTest {

    @Test fun `the tree label is the token Latin with the room through its master`() {
        assertEquals("LR · हॉल", RoomToken.label(Tx.MR, "Living Room"))
        assertEquals("MB · मास्टर बेडरूम", RoomToken.label(Tx.MR, "Master Bedroom"))
        // a typed room the master does not know passes through unchanged
        assertEquals("PR · Powder Room", RoomToken.label(Tx.MR, "Powder Room"))
        // and English stays byte for byte what the tree drew before M2
        assertEquals("LR · Living Room", RoomToken.label(Tx.EN, "Living Room"))
    }

    @Test fun `the delete routes say the page's own Marathi, per branch`() {
        val cant = Deletes.route(Tx.MR, "project", "", serverKnown = true) as DeleteRoute.Blocked
        assertEquals("ऑफिस सिस्टमवर हा प्रोजेक्ट ओळखता येत नाही. रिफ्रेशसाठी खाली ओढा — आता हटवल्यास तो फक्त या फोनवरून जाईल.", cant.reason)
        val cantSite = Deletes.route(Tx.MR, "site", " ", serverKnown = true) as DeleteRoute.Blocked
        assertEquals("ऑफिस सिस्टमवर ही साइट ओळखता येत नाही. रिफ्रेशसाठी खाली ओढा — आता हटवल्यास ती फक्त या फोनवरून जाईल.", cantSite.reason)
        val client = Deletes.route(Tx.MR, "client", "CUST-0001", serverKnown = true) as DeleteRoute.Blocked
        assertEquals("ग्राहक हा ऑफिसचा कस्टमर रेकॉर्ड आहे, त्यामुळे तो फोनवरून काढता येत नाही. त्याखालच्या साइट्स इथे हटवा; ग्राहक हटवणे हे ऑफिसचे काम आहे.", client.reason)
        assertEquals("या फोनवरून काढले — मुख्य साइट कधीच ऑफिस सिस्टमवर पोहोचले नव्हते.",
            Deletes.localOnlyMessage(Tx.MR, "Main site"))
        // a typed name passes through the site-name master unchanged
        assertTrue(Deletes.localOnlyMessage(Tx.MR, "Baner flat").contains("Baner flat"))
    }

    @Test fun `the site-name master both ways, a site type and a job type`() {
        assertEquals("मुख्य साइट", Masters.site_name(Tx.MR, "Main site"))
        assertEquals("(साइट नाही)", Masters.site_name(Tx.MR, "(no site)"))
        assertEquals("Sunshine Towers", Masters.site_name(Tx.MR, "Sunshine Towers"))
        assertEquals("Main site", Masters.site_name(Tx.EN, "Main site"))
        assertEquals("फ्लॅट", Masters.site(Tx.MR, "Flat"))
        assertEquals("पुरवठा व बसवणे", Masters.job(Tx.MR, "Supply & install"))
    }

    @Test fun `the project sheet's Status value Open renders through the page's own key`() {
        // the second round's finding 1: index61c2.html:7915 renders 'Open' through app.status_open; other
        // statuses are ERP's own stored words, drawn as stored (the ProjectSheet when in TreeShell.kt)
        assertEquals("चालू", Tx.MR.app_status_open)
        assertEquals("Open", Tx.EN.app_status_open)
    }

    @Test fun `the kind compositions read as the page composes them`() {
        // the {kind} word is the noun, the page's own look ("साइट चे नाव बदला किंवा काढा")
        assertEquals("साइट चे नाव बदला किंवा काढा", Tx.MR.app_rename_remove_x(Tx.MR.noun_site(1)))
        assertEquals("ग्राहक चे नाव बदला", Tx.MR.app_rename_x(Tx.MR.noun_client(1)))
        // the field label: the CAPITALISED kind + app.name ("प्रोजेक्ट नाव"; EN "Project" + " name")
        assertEquals("प्रोजेक्ट नाव", Tx.MR.common_project + Tx.MR.app_name)
        assertEquals("Project name", Tx.EN.common_project + Tx.EN.app_name)
    }

    @Test fun `the queue's words are the served page's`() {
        assertEquals("सिंक रांग", Tx.MR.app_sync_queue)
        assertEquals("1 बाकी", Tx.MR.app_x_waiting_queue(n = 1))
        assertEquals("3 waitings", Tx.EN.app_x_waiting_queue(n = 3)) // the prototype's own fold, byte for byte
        assertEquals("हटवले जाणार", Tx.MR.app_about_delete)
        assertEquals("PROJ-0001 आणि त्यातील सर्व काही हटवा", Tx.MR.app_delete_x_everything("PROJ-0001"))
    }
}
