package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerMessageTest {

    /** The exact reply the phone showed Amit on 2026-08-24, in red, cut off
     *  after "linked with S". */
    private val linkExists = """
        {"exception":"frappe.exceptions.LinkExistsError: Cannot delete or cancel
        because Site Photo 360 <a href=\"https://mcft-stg.frappe.cloud/desk/site-photo-360/MEST-PH-2026-00027\">MEST-PH-2026-00027</a>
        is linked with Site Photo Inbox <a href=\"#\">MEST-INBOX-0004</a>"}
    """.trimIndent().replace("\n", " ")

    @Test
    fun the_python_class_name_never_reaches_the_screen() {
        val out = ServerMessage.humanise(linkExists, 417)
        assertFalse(out.contains("frappe.exceptions"),
            "a Python class name is not an error message: $out")
        assertTrue(out.startsWith("Cannot delete or cancel"))
    }

    @Test
    fun the_markup_goes_and_the_docname_stays() {
        val out = ServerMessage.humanise(linkExists, 417)
        assertFalse(out.contains("<a"), "markup reached the screen: $out")
        assertFalse(out.contains("href"))
        assertFalse(out.contains("https://"),
            "the URL is noise; the docname is the point: $out")
        assertTrue(out.contains("MEST-PH-2026-00027"),
            "the capture must still be named: $out")
    }

    @Test
    fun the_half_that_says_what_is_in_the_way_survives() {
        // The original truncation fell after "linked with S", removing the only
        // part a person could act on.
        val out = ServerMessage.humanise(linkExists, 417)
        assertTrue(out.contains("linked with Site Photo Inbox"),
            "the blocker must be named: $out")
    }

    @Test
    fun frappes_own_human_sentence_wins_over_the_exception() {
        val body = """
            {"exception":"frappe.exceptions.ValidationError: raw and ugly",
             "_server_messages":"[\"{\\\"message\\\": \\\"PROJ-0005 still has 2 capture(s). Delete those too?\\\"}\"]"}
        """.trimIndent().replace("\n", " ")
        assertEquals("PROJ-0005 still has 2 capture(s). Delete those too?",
            ServerMessage.humanise(body, 417))
    }

    @Test
    fun a_body_that_says_nothing_falls_back_to_the_status() {
        assertEquals("HTTP 502", ServerMessage.humanise("<html>gateway</html>", 502))
        assertEquals("HTTP 403", ServerMessage.humanise("", 403))
    }

    @Test
    fun a_colon_in_an_ordinary_sentence_is_not_a_class_prefix() {
        val body = """{"exception":"Not allowed: this identity cannot write"}"""
        assertEquals("Not allowed: this identity cannot write",
            ServerMessage.humanise(body, 403))
    }

    @Test
    fun a_long_message_is_cut_between_words_not_inside_one() {
        val long = "word ".repeat(200).trim()
        val out = ServerMessage.humanise("""{"exception":"$long"}""", 417)
        assertTrue(out.length <= ServerMessage.MAX + 1)
        assertTrue(out.endsWith("word…"), "must end at a word: $out")
    }
}
