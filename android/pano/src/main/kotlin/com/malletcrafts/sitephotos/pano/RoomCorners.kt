package com.malletcrafts.sitephotos.pano

import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot

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

    /** The point at index 0..7: 0-3 ceiling FL FR BR BL, 4-7 floor. */
    operator fun get(i: Int): DoubleArray = if (i < 4) ceiling[i] else floor[i - 4]

    fun with(i: Int, d: DoubleArray): RoomCorners =
        if (i < 4) RoomCorners(ceiling.mapIndexed { k, v -> if (k == i) d else v }, floor)
        else RoomCorners(ceiling, floor.mapIndexed { k, v -> if (k == i - 4) d else v })

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

    companion object {
        /** Below this Harris response the window counts as a plain surface
         *  and the point is left alone. Set on Vrushal's living room 360 at
         *  2048 px (the size the app previews at): its pink-on-pink corners
         *  are low-contrast, and at 1e8 most floor corners did not snap at
         *  all; at 1e7 they landed within a few pixels of the corner read by
         *  eye. Ceiling corners against the cornice were mixed -- the snap
         *  is a help, the person still judges. */
        const val MIN_CORNER = 1e7

        /** Names for the eight, in index order, as the screen labels them. */
        val NAMES = listOf("FL", "FR", "BR", "BL").let { c ->
            c.map { "$it ceiling" } + c.map { "$it floor" }
        }

        /** Yaw of each vertical corner when nothing is known about the room:
         *  a square room, camera at its centre. */
        private val SQUARE_YAWS = listOf(-45.0, 45.0, 135.0, -135.0)

        /**
         * Where the eight start before anybody drags them. With L x W x H, from
         * the station the plan proposes; with only a height, or nothing, a
         * square room seen from its centre -- near enough to grab and snap.
         */
        fun start(plan: CaptureGeometry.Plan?): RoomCorners {
            if (plan == null) {
                fun at(yaw: Double, pitch: Double) =
                    WallCorners.rayAt(WallCorners.basis(yaw, pitch, 90.0), 0.5, 0.5)
                return RoomCorners(SQUARE_YAWS.map { at(it, 35.0) }, SQUARE_YAWS.map { at(it, -35.0) })
            }
            val s = plan.station
            val xl = -s.xIn; val xr = plan.lengthIn - s.xIn
            val zf = plan.widthIn - s.yIn; val zb = -s.yIn
            val plan4 = listOf(xl to zf, xr to zf, xr to zb, xl to zb)
            val yc = plan.heightIn - s.zIn; val yf = -s.zIn
            return RoomCorners(
                plan4.map { (x, z) -> WallCorners.unit(doubleArrayOf(x, yc, z)) },
                plan4.map { (x, z) -> WallCorners.unit(doubleArrayOf(x, yf, z)) })
        }

        /**
         * Pull a roughly placed point onto the nearest real corner in the photo.
         *
         * Looks at a small window around the point (a few degrees), finds the
         * strongest corner-like feature there -- where two edges cross, like a
         * wall edge meeting the skirting or the cornice -- preferring the one
         * nearest the finger, and returns its direction. When the window holds
         * nothing corner-like (a plain wall) the point is returned unchanged:
         * a snap that cannot find a corner must not invent one.
         *
         * Harris response on a gnomonic patch; pure JVM so it is tested on a
         * synthetic room rather than trusted.
         */
        fun snap(pano: Panorama.Image, d: DoubleArray, windowDeg: Double = 6.0, px: Int = 64, minCorner: Double = MIN_CORNER): DoubleArray {
            val yaw = Math.toDegrees(atan2(d[0], d[2]))
            val pitch = Math.toDegrees(atan2(d[1], hypot(d[0], d[2])))
            val b = WallCorners.basis(yaw, pitch, windowDeg, clamp = false)
            val g = Array(px) { r -> DoubleArray(px) { c ->
                val p = WallCorners.sample(pano, WallCorners.rayAt(b, (c + 0.5) / px, (r + 0.5) / px))
                0.299 * (p ushr 16 and 0xFF) + 0.587 * (p ushr 8 and 0xFF) + 0.114 * (p and 0xFF)
            } }
            val ix = Array(px) { DoubleArray(px) }
            val iy = Array(px) { DoubleArray(px) }
            for (r in 1 until px - 1) for (c in 1 until px - 1) {
                ix[r][c] = (g[r - 1][c + 1] + 2 * g[r][c + 1] + g[r + 1][c + 1]) -
                    (g[r - 1][c - 1] + 2 * g[r][c - 1] + g[r + 1][c - 1])
                iy[r][c] = (g[r + 1][c - 1] + 2 * g[r + 1][c] + g[r + 1][c + 1]) -
                    (g[r - 1][c - 1] + 2 * g[r - 1][c] + g[r - 1][c + 1])
            }
            val w = 2
            val sigma = px / 3.0
            var best = 0.0; var br = -1; var bc = -1
            var peak = 0.0
            for (r in w + 1 until px - w - 1) for (c in w + 1 until px - w - 1) {
                var a = 0.0; var bb = 0.0; var cc = 0.0
                for (dr in -w..w) for (dc in -w..w) {
                    val gx = ix[r + dr][c + dc]; val gy = iy[r + dr][c + dc]
                    a += gx * gx; bb += gy * gy; cc += gx * gy
                }
                val resp = a * bb - cc * cc - 0.05 * (a + bb) * (a + bb)
                if (resp > peak) peak = resp
                val dx = c + 0.5 - px / 2.0; val dy = r + 0.5 - px / 2.0
                val weighted = resp * exp(-(dx * dx + dy * dy) / (2 * sigma * sigma))
                if (weighted > best) { best = weighted; br = r; bc = c }
            }
            // Nothing corner-like at all: leave the point where the finger put it.
            if (br < 0 || peak < minCorner) return d
            return WallCorners.rayAt(b, (bc + 0.5) / px, (br + 0.5) / px)
        }
    }
}
