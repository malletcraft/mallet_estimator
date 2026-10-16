package com.malletcrafts.sitephotos.pano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** A reading box writes only what was typed (v52 slice 11 review: a derived chain figure must never become a reading). */
class MmBoxTest {

    @Test fun aDerivedFigureLeftUneditedWritesNothing() {
        // ChainTest's side wall: L 5458.5 edge to edge, FK1.1 read 3054 -> FK1.2 derived 2404.5, shown "2405"
        assertEquals("2405", MmBox.shownText(2404.5))
        assertNull(MmBox.commit(2404.5, "2405", isReading = false))
    }

    @Test fun aFractionalReadingLeftUneditedIsNotRewritten() {
        assertNull(MmBox.commit(905.25, MmBox.shownText(905.25)))
        assertNull(MmBox.commit(3314.7, "3315"))
    }

    @Test fun anIntegerReadingLeftUneditedWritesNothing() {
        assertNull(MmBox.commit(905.0, "905"))
        assertNull(MmBox.commit(null, ""))
    }

    @Test fun typingOverADerivedFigureMakesItAReading() {
        val c = assertNotNull(MmBox.commit(2404.5, "2410", isReading = false))
        assertEquals(2410.0, c.value)
    }

    @Test fun typingIntoAnEmptyBoxWritesTheReading() {
        assertEquals(905.0, assertNotNull(MmBox.commit(null, "905")).value)
    }

    @Test fun clearingAReadingRemovesIt() {
        val c = assertNotNull(MmBox.commit(905.0, ""))
        assertNull(c.value)
    }

    @Test fun clearingADerivedFigureRemovesNothing() {
        assertNull(MmBox.commit(2404.5, "", isReading = false))
    }

    @Test fun zeroIsNotAReading() {
        assertNull(MmBox.commit(null, "0"))
        assertNull(MmBox.commit(905.0, "0"))
    }
}
