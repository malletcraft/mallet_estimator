package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.SiteRows.Proj
import com.malletcrafts.interiorestimate.pano.SiteRows.Row
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHICH SITES THE TREE LISTS, AND UNDER WHAT NAME (port 62 slice A2; the plan critic's finding 2). The three cases
 * the plan names -- an S5 rename with a bootstrap row of the old name lists ONE site; a later bootstrap carrying the
 * old name back still lists one; a bootstrap site with no project and no rename is still listed, empty -- the rows
 * around them, and the one source of a site's name.
 */
class SiteRowsTest {

    private val C = "Asha P"

    /** The site names the tree ends up listing for one client: every project's site and every row's listed name, by the catalogue's key. */
    private fun sites(rows: List<Row>, projects: List<Proj>): List<String> =
        (projects.map { it.site } + SiteRows.listed(rows, projects)).distinctBy { Renames.key(it) }

    @Test fun `an S5 rename with a bootstrap row of the old name - ONE site listed`() {
        // S5 took "Asha flat, Baner"; the masters still hold the bench's row "Main site" (MEST-SITE-0005) and the
        // bootstrap's project entry linking to it -- the project itself is listed under S5's name
        val rows = listOf(Row("MEST-SITE-0005", C, "Main site"))
        val projects = listOf(Proj(C, "Asha flat, Baner", "MEST-SITE-0005"))
        assertEquals(listOf("Asha flat, Baner"), SiteRows.listed(rows, projects))
        assertEquals(listOf("Asha flat, Baner"), sites(rows, projects), "never the old name beside it as an empty site")
    }

    @Test fun `a later bootstrap carrying the old name - still one`() {
        // the rename step re-named the stored row; a phone with a live pair bootstraps again and the bench, which is
        // deliberately not told, sends "Main site" back on the row AND on the project -- S5's name still lists both
        val rows = listOf(Row("MEST-SITE-0005", C, "Main site"), Row("MEST-SITE-0009", C, "Farmhouse"))
        val projects = listOf(Proj(C, SiteRows.nameOf("Asha flat, Baner", "Main site", ""), "MEST-SITE-0005"))
        assertEquals(listOf("Asha flat, Baner", "Farmhouse"), SiteRows.listed(rows, projects))
        assertEquals(listOf("Asha flat, Baner", "Farmhouse"), sites(rows, projects))
        // and once the row itself says the new name (the rename step, or a bench that was told): the same one site
        assertEquals(listOf("Asha flat, Baner"), SiteRows.listed(listOf(Row("MEST-SITE-0005", C, "Asha flat, Baner")), projects.take(1)))
    }

    @Test fun `a bootstrap site with no project and no rename - still listed, empty`() {
        val rows = listOf(Row("MEST-SITE-0002", C, "Main site"), Row("MEST-SITE-0007", "Other client", "Main site"))
        assertEquals(listOf("Main site", "Main site"), SiteRows.listed(rows, emptyList()), "a site the office knows with no jobs on it yet")
        // projects of other sites do not move it
        assertEquals(listOf("Main site", "Main site"), SiteRows.listed(rows, listOf(Proj(C, "Farmhouse", "MEST-SITE-0031"), Proj(C, "Phone site", ""))))
    }

    @Test fun `a row keeps its own name while any project linked to it is still listed under it`() {
        val row = Row("MEST-SITE-0005", C, "Main site")
        // 1 of 2 renamed on S5: the other project is still at "Main site", so the row stays there -- with a project, not empty
        val half = listOf(Proj(C, "Asha flat, Baner", "MEST-SITE-0005"), Proj(C, "Main site", "MEST-SITE-0005"))
        assertEquals(listOf("Main site"), SiteRows.listed(listOf(row), half))
        assertEquals(listOf("Asha flat, Baner", "Main site"), sites(listOf(row), half), "two sites, as S5 holds them -- neither empty")
        // a change of case or spacing is the same name: the row is where it was, in its own spelling
        assertEquals(listOf("Main site"), SiteRows.listed(listOf(row), listOf(Proj(C, "main_site ", "MEST-SITE-0005"))))
        // every linked project renamed, to two names: the row goes with the first -- it is never left as a third site
        assertEquals(listOf("A"), SiteRows.listed(listOf(row), listOf(Proj(C, "A", "MEST-SITE-0005"), Proj(C, "B", "MEST-SITE-0005"))))
    }

    @Test fun `only a project that links to the row by its docname moves it - never a name alone, never the bucket`() {
        val row = Row("MEST-SITE-0005", C, "Main site")
        // no link (a project only S5 lists, a phone row): the row is not theirs to move
        assertEquals(listOf("Main site"), SiteRows.listed(listOf(row), listOf(Proj(C, "Asha flat, Baner", ""))))
        // another row's project
        assertEquals(listOf("Main site"), SiteRows.listed(listOf(row), listOf(Proj(C, "Asha flat, Baner", "MEST-SITE-0006"))))
        // a row with no docname links to nothing, whatever a project's blank link says
        assertEquals(listOf("Main site"), SiteRows.listed(listOf(Row("", C, "Main site")), listOf(Proj(C, "Asha flat, Baner", ""))))
        // a linked project filed under the bucket has no name to give the row
        assertEquals(listOf("Main site"), SiteRows.listed(listOf(row), listOf(Proj(C, Renames.NO_SITE, "MEST-SITE-0005"), Proj(C, " ", "MEST-SITE-0005"))))
        // rows come back in their own order, one name each
        val rows = listOf(Row("S-1", C, "One "), Row("S-2", C, "Two"), Row("S-3", C, "Three"))
        assertEquals(listOf("One", "New two", "Three"), SiteRows.listed(rows, listOf(Proj(C, "New two", "S-2"))))
    }

    @Test fun `one source for a site's name - S5's where S5 holds one, else the masters', else the phone's own row, else none`() {
        assertEquals("Asha flat, Baner", SiteRows.nameOf("Asha flat, Baner", "Main site", "Phone name"), "S5's name is the one shown (R4)")
        assertEquals("Asha flat, Baner", SiteRows.nameOf(" Asha flat, Baner ", "", ""))
        // S5 lists the project and holds no name yet, or does not list it: the masters' own
        assertEquals("Main site", SiteRows.nameOf("", "Main site", "Phone name"))
        assertEquals("Main site", SiteRows.nameOf(null, " Main site ", "Phone name"))
        // a project only S5 lists has none in the masters: the name this phone's own row gives it
        assertEquals("Phone name", SiteRows.nameOf("", "", "Phone name"))
        assertEquals("Phone name", SiteRows.nameOf(null, "  ", " Phone name "))
        // none at all: the caller files it under its bucket
        assertEquals("", SiteRows.nameOf(null, "", "")); assertEquals("", SiteRows.nameOf(" ", " ", " "))
    }

    @Test fun `the catalogue lists through these rules, and the rename step re-names the stored row (read as source)`() {
        val f = File("../app/src/main/kotlin/com/malletcrafts/interiorestimate/Catalogue.kt")
        assertTrue(f.isFile, "the app's Catalogue.kt is beside :pano (${f.absolutePath})")
        val cat = f.readText()
        // both tree builders read the rows through the ONE keyed listing
        assertEquals(2, Regex("val listed = serverSites\\(masters, all\\)").findAll(cat).count(), "sitesOf and snapshot")
        assertTrue(cat.contains("SiteRows.listed(rows, projects.map { SiteRows.Proj(it.client, it.site, it.siteId) })"), "the listing is SiteRows.listed, over the projects as the tree lists them")
        assertEquals(0, Regex("optString\\(\"site_name\"\\)\\.orEmpty\\(\\)").findAll(cat).count(), "no builder names an empty group from the row's own site_name any more")
        // the rename step: the bootstrap's `sites` row, and the client's project entries of the old name
        val step = cat.substring(cat.indexOf("fun renameSiteRows("))
        assertTrue(step.contains("for (list in listOf(\"sites\", \"projects\"))") && step.contains("o.put(\"site_name\", to)"), "both lists take the new name")
        assertTrue(step.contains("!same(owner, client) || !same(name, from)"), "only this client's rows of the old name")
    }
}
