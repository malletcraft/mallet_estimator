package com.malletcrafts.sitephotos.pano

/**
 * A room's eight corners: the ceiling end and the floor end of each of its
 * four vertical corners. Every face is built from four of them.
 *
 * Amit, 2026-10-08: "corner placement is very tedious on app" -- four drags
 * per face is twenty-four for a room, and every one of them is a corner that
 * some other face already had. A room has four vertical corners, so eight
 * points describe all six faces: each wall is two neighbouring corners, top
 * and bottom, and the ceiling and floor are the four top or four bottom ends.
 *
 * Since 2026-10-08 they come from a RoomQuad -- two opposite corners and
 * four wall lines (Amit: "setting up
 * corners is still difficult" -- lines are dragged, not points), and this
 * class only hands each face its four.
 *
 * Order is FL, FR, BR, BL (front-left round to back-left, seen from above,
 * the front wall being the one the "front" face looks at). Directions are in
 * the Panorama frame: x right, y up, z forward.
 */
class RoomCorners(
    /** Ceiling end of each vertical corner, FL FR BR BL. */
    val ceiling: List<DoubleArray>,
    /** Floor end of each vertical corner, FL FR BR BL. */
    val floor: List<DoubleArray>,
) {
    init {
        require(ceiling.size == 4 && floor.size == 4) { "four corners, top and bottom" }
    }

    /**
     * The four that make one face, TL TR BR BL as that face shows them.
     *
     * A wall seen from inside has its left-hand vertical corner on the left:
     * the front wall runs FL -> FR, the right wall FR -> BR, and so on round
     * the room. The ceiling and floor are ordered by where their corners fall
     * on the face, because which edge is "top" depends on which way the face
     * looks (the floor face has the front wall at the top, the ceiling face
     * the back wall).
     */
    fun forFace(face: String): List<DoubleArray> = when (face) {
        "front" -> wall(0, 1)
        "right" -> wall(1, 2)
        "back" -> wall(2, 3)
        "left" -> wall(3, 0)
        "up" -> onScreen(ceiling, 90.0)
        "down" -> onScreen(floor, -90.0)
        else -> error("not a face: $face")
    }

    private fun wall(l: Int, r: Int) = listOf(ceiling[l], ceiling[r], floor[r], floor[l])

    private fun onScreen(four: List<DoubleArray>, pitch: Double): List<DoubleArray> {
        val b = WallCorners.basis(0.0, pitch, 120.0)
        val placed = four.map { d -> (WallCorners.pointOf(b, d) ?: Pair(0.5, 0.5)) to d }
        val bySy = placed.sortedBy { it.first.second }
        val top = bySy.take(2).sortedBy { it.first.first }
        val bottom = bySy.drop(2).sortedBy { it.first.first }
        return listOf(top[0].second, top[1].second, bottom[1].second, bottom[0].second)
    }
}
