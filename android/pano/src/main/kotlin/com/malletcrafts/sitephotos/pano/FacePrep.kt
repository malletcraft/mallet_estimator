package com.malletcrafts.sitephotos.pano

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlin.random.Random

/**
 * Face Prep — the room box on each of a capture's six faces, the things that
 * stick out of it, and every dimension ImageMeter will need, in mm.
 *
 * Ported from the browser prototype Amit worked through on 2026-10-09 (v1..v21,
 * mcft-workspace `survey-server/faceprep/index.html`), which is the spec: same
 * names, same rules, same numbers. Pure Kotlin so the geometry is tested here
 * rather than first compiled by CI on the shipping branch.
 *
 * Coordinates on a face are (u, v) in 0..1 across the measured box: u left to
 * right, v top to bottom, exactly as the face photo shows it. Walls are read
 * from inside the room; the floor has the front wall at its top, the ceiling
 * the back wall (Panorama's own up vectors for "down" and "up").
 */
object FacePrep {

    /** The order Amit asked for: floor, front, left, right, back, ceiling. */
    val FACE_ORDER = listOf("floor", "front", "left", "right", "back", "ceiling")
    val WALLS = listOf("front", "left", "right", "back")
    val SIDES = listOf("top", "right", "bottom", "left")

    /** Face Prep name → Panorama face name (the file label on disk). */
    fun panoFace(face: String): String = when (face) { "floor" -> "down"; "ceiling" -> "up"; else -> face }

    val LABEL = mapOf("floor" to "Floor", "front" to "Front wall", "left" to "Left wall",
        "right" to "Right wall", "back" to "Back wall", "ceiling" to "Ceiling")

    /** What each box edge of a face meets — used to say "650 from left wall". */
    val EDGE_NAMES: Map<String, Map<String, String>> = mapOf(
        "front" to mapOf("u0" to "left wall", "u1" to "right wall", "v0" to "ceiling", "v1" to "floor"),
        "right" to mapOf("u0" to "front wall", "u1" to "back wall", "v0" to "ceiling", "v1" to "floor"),
        "back" to mapOf("u0" to "right wall", "u1" to "left wall", "v0" to "ceiling", "v1" to "floor"),
        "left" to mapOf("u0" to "back wall", "u1" to "front wall", "v0" to "ceiling", "v1" to "floor"),
        "ceiling" to mapOf("u0" to "left wall", "u1" to "right wall", "v0" to "back wall", "v1" to "front wall"),
        "floor" to mapOf("u0" to "left wall", "u1" to "right wall", "v0" to "front wall", "v1" to "back wall"),
    )

    val STEP_KINDS = listOf("column" to "Column", "beam" to "Beam", "step" to "Other step")
    val DETAIL_TYPES = listOf("door" to "Door", "window" to "Window", "opening" to "Opening",
        "electrical" to "Electrical point", "loft" to "Loft")
    fun stepLabel(kind: String) = STEP_KINDS.firstOrNull { it.first == kind }?.second ?: kind
    fun detailLabel(type: String) = DETAIL_TYPES.firstOrNull { it.first == type }?.second ?: type

    // ---------------------------------------------------------------- model

    /** Where the measured face's four corners are in the photo, as fractions of the photo's width/height. */
    data class Box(var x0: Double, var y0: Double, var x1: Double, var y1: Double)

    /** A column, beam or other step: the part of the face it takes away. */
    class Step(var kind: String, var u0: Double, var v0: Double, var u1: Double, var v1: Double, var site: Boolean = false) {
        fun norm() = Step(kind, min(u0, u1), min(v0, v1), max(u0, u1), max(v0, v1), site)
        fun copy() = Step(kind, u0, v0, u1, v1, site)
    }

    /**
     * A door, window, opening, electrical point or loft. [sx]/[sy] say which
     * edges carry the two size arrows (the corner the drag started from);
     * [sw]/[sh] are dividers on those arrows. [id] is a key that survives
     * deleting a neighbour, so a site figure stays on the thing it measured.
     */
    class Detail(var type: String, var u0: Double, var v0: Double, var u1: Double, var v1: Double,
                 var sx: Int = 0, var sy: Int = 0,
                 val sw: MutableList<Double> = mutableListOf(), val sh: MutableList<Double> = mutableListOf(),
                 var id: String = newId()) {
        fun copy() = Detail(type, u0, v0, u1, v1, sx, sy, sw.toMutableList(), sh.toMutableList(), id)
    }

    /** A free measure line, any direction, with its own dividers; [id] as for [Detail]. */
    class MLine(var a: DoubleArray, var b: DoubleArray, val ts: MutableList<Double> = mutableListOf(), var id: String = newId()) {
        fun copy() = MLine(a.copyOf(), b.copyOf(), ts.toMutableList(), id)
        fun at(t: Double) = doubleArrayOf(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)
    }

    /** A short random key, as the prototype's idOf: six base-36 characters. */
    fun newId(): String { val cs = "abcdefghijklmnopqrstuvwxyz0123456789"; return String(CharArray(6) { cs[Random.nextInt(cs.length)] }) }

    class Face(
        var box: Box? = null,
        val steps: MutableList<Step> = mutableListOf(),
        val splits: MutableMap<String, MutableList<Double>> = SIDES.associateWith { mutableListOf<Double>() }.toMutableMap(),
        val dets: MutableList<Detail> = mutableListOf(),
        val lines: MutableList<MLine> = mutableListOf(),
        /** SITE figures in mm, keyed by [Dim.key]. They never move the drawing; the calculated figure stays beside them. */
        val meas: MutableMap<String, Int> = mutableMapOf(),
    ) {
        fun copy() = Face(box?.copy(), steps.map { it.copy() }.toMutableList(),
            splits.mapValues { it.value.toMutableList() }.toMutableMap(),
            dets.map { it.copy() }.toMutableList(), lines.map { it.copy() }.toMutableList(), meas.toMutableMap())
        fun isEmpty() = box == null && steps.isEmpty() && dets.isEmpty() && lines.isEmpty() && splits.values.all { it.isEmpty() }
    }

    /**
     * One room: its measured box (H, X = width left→right, Z = length front→back,
     * all mm), where the camera stood ([cx], [cz] mm off centre, [ch] lens
     * height), the six faces, and laser readings of the box itself.
     */
    class Room(var H: Int, var X: Int, var Z: Int, var cx: Int = 0, var cz: Int = 0, var ch: Int = H / 2,
               val faces: MutableMap<String, Face> = FACE_ORDER.associateWith { Face() }.toMutableMap(),
               val laser: MutableMap<String, Int> = mutableMapOf()) {
        fun face(f: String): Face = faces.getOrPut(f) { Face() }
        fun copy() = Room(H, X, Z, cx, cz, ch, faces.mapValues { it.value.copy() }.toMutableMap(), laser.toMutableMap())
    }

    // ---------------------------------------------------------------- camera guess

    /**
     * Where the room box falls on a face photo seen from the camera: the
     * starting box before Amit lines it up. [fovDeg] is the face's field of
     * view, [w]/[h] the photo's square part in pixels. Null when the camera
     * is outside the box. Same basis as [Panorama.faceFromEquirect].
     */
    fun cameraGuess(room: Room, face: String, fovDeg: Double): Box? {
        val (f, r, u) = basis(face)
        val t = tan(Math.toRadians(fovDeg) / 2.0)
        val xs = doubleArrayOf(-room.X / 2.0 - room.cx, room.X / 2.0 - room.cx)
        val ys = doubleArrayOf(-room.ch.toDouble(), (room.H - room.ch).toDouble())
        val zs = doubleArrayOf(-room.Z / 2.0 - room.cz, room.Z / 2.0 - room.cz)
        val fix: Pair<Int, Double> = when (face) {
            "front" -> 2 to zs[1]; "back" -> 2 to zs[0]; "right" -> 0 to xs[1]
            "left" -> 0 to xs[0]; "ceiling" -> 1 to ys[1]; else -> 1 to ys[0]
        }
        val px = mutableListOf<Double>(); val py = mutableListOf<Double>()
        for (x in xs) for (y in ys) for (z in zs) {
            val v = doubleArrayOf(x, y, z)
            if (v[fix.first] != fix.second) continue
            val d = dot(v, f); if (d <= 1) return null
            val sx = dot(v, r) / d; val sy = dot(v, u) / d
            px.add((sx / t + 1) / 2); py.add((1 - sy / t) / 2)
        }
        return Box(px.min(), py.min(), px.max(), py.max())
    }

    /**
     * Where the room box is on an ELEVATION — the squared-on photo the split
     * already makes from the corners set there (FaceWriter, WallCorners.elevation).
     * The corners land exactly [margin] of a side inside the picture, so the box
     * is known and nobody lines it up by hand (Amit, 2026-10-09: "why do i need
     * to set corner in apk now? everything should be taken care automatically
     * for splitting fotos"). [totalW]/[totalH] are the saved file's pixels,
     * caption strip included; the strip is FaceWriter.captionedBitmap's own
     * rule (5.2 % of the picture, at least 28 px), so the picture's height is
     * the one that rule maps onto [totalH]. Fractions are of the width, like every box.
     */
    fun elevationBox(totalW: Int, totalH: Int, margin: Double = 0.06): Box {
        var h = totalH
        for (c in totalH downTo max(1, totalH - 600)) if (c + max((c * 0.052).toInt(), 28) == totalH) { h = c; break }
        val span = 1 + 2 * margin; val a = margin / span; val b = (1 + margin) / span
        return Box(a, a * h / totalW, b, b * h / totalW)
    }

    private fun basis(face: String): Triple<DoubleArray, DoubleArray, DoubleArray> = when (face) {
        "front" -> Triple(d(0, 0, 1), d(1, 0, 0), d(0, 1, 0))
        "right" -> Triple(d(1, 0, 0), d(0, 0, -1), d(0, 1, 0))
        "back" -> Triple(d(0, 0, -1), d(-1, 0, 0), d(0, 1, 0))
        "left" -> Triple(d(-1, 0, 0), d(0, 0, 1), d(0, 1, 0))
        "ceiling" -> Triple(d(0, 1, 0), d(1, 0, 0), d(0, 0, -1))
        else -> Triple(d(0, -1, 0), d(1, 0, 0), d(0, 0, 1))
    }
    private fun d(x: Int, y: Int, z: Int) = doubleArrayOf(x.toDouble(), y.toDouble(), z.toDouble())
    private fun dot(a: DoubleArray, b: DoubleArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    // ---------------------------------------------------------------- face size, wall span

    /**
     * Horizontal and vertical size of a face in mm. A wall is as long as the
     * floor plan says it runs, which is not always the room's full length: the
     * master bedroom's front wall steps in 292 mm at the right, so its right
     * wall is 4013, not 4318 (Amit's site sheet, 2026-10-09).
     */
    fun faceDims(room: Room, face: String): Pair<Int, Int> {
        if (face in WALLS) {
            val (s0, s1) = wallSpan(room, face)
            val l = if (face == "front" || face == "back") room.X else room.Z
            return ((s1 - s0) * l).roundToInt() to room.H
        }
        return room.X to room.Z
    }

    class Contact(val wall: String, val u0: Double, val u1: Double, val d: Double)

    /** Which walls a floor/ceiling step stands against, its run along each (wall u) and its depth into the room (mm). */
    fun plateContacts(room: Room, plate: String, st: Step): List<Contact> {
        val a0 = st.u0; val a1 = st.u1
        val b0 = if (plate == "ceiling") st.v0 else 1 - st.v1
        val b1 = if (plate == "ceiling") st.v1 else 1 - st.v0
        val out = mutableListOf<Contact>()
        if (a1 > 0.995) out.add(Contact("right", 1 - b1, 1 - b0, (1 - a0) * room.X))
        if (a0 < 0.005) out.add(Contact("left", b0, b1, a1 * room.X))
        if (b1 > 0.995) out.add(Contact("front", a0, a1, (1 - b0) * room.Z))
        if (b0 < 0.005) out.add(Contact("back", 1 - a1, 1 - a0, b1 * room.Z))
        return out
    }

    /**
     * A corner step on the floor plan that is longer INTO the room than it runs
     * ALONG this wall is a missing corner of the room, so the wall stops at it.
     * Columns and beams never shorten a wall — they stand against it.
     */
    fun endsWall(room: Room, st: Step, c: Contact): Boolean {
        if (st.kind == "column" || st.kind == "beam" || plateContacts(room, "floor", st).size < 2) return false
        val l = if (c.wall == "front" || c.wall == "back") room.X else room.Z
        return (c.u1 - c.u0) * l < c.d && (c.u0 < 0.005 || c.u1 > 0.995)
    }

    /** Where a wall really starts and ends along the room (0..1 in that wall's own u). */
    fun wallSpan(room: Room, wall: String): Pair<Double, Double> {
        var s0 = 0.0; var s1 = 1.0
        for (s in room.face("floor").steps) {
            val st = s.norm()
            for (c in plateContacts(room, "floor", st)) {
                if (c.wall != wall || !endsWall(room, st, c)) continue
                if (c.u0 < 0.005) s0 = max(s0, c.u1)
                if (c.u1 > 0.995) s1 = min(s1, c.u0)
            }
        }
        return if (s1 - s0 > 0.05) s0 to s1 else 0.0 to 1.0
    }

    // ---------------------------------------------------------------- outline

    class Piece(val a: IntArray, val b: IntArray, val len: Int, val inward: IntArray, val side: String?, val h: Boolean)

    /**
     * The face outline in whole mm: the measured box minus every column / beam
     * step, worked on a grid of every step edge, cut at the box dividers. Each
     * piece gets its own dimension; the totals stay as measured.
     */
    fun outline(room: Room, face: String, f: Face = room.face(face)): List<Piece> {
        val (w, h) = faceDims(room, face)
        val st = f.steps.map { it.norm() }.map { intArrayOf((it.u0 * w).roundToInt(), (it.u1 * w).roundToInt(), (it.v0 * h).roundToInt(), (it.v1 * h).roundToInt()) }
            .filter { it[1] > it[0] && it[3] > it[2] }
        val us = (listOf(0, w) + st.flatMap { listOf(it[0], it[1]) }).distinct().sorted()
        val vs = (listOf(0, h) + st.flatMap { listOf(it[2], it[3]) }).distinct().sorted()
        fun filled(i: Int, j: Int): Boolean {
            if (i < 0 || j < 0 || i >= us.size - 1 || j >= vs.size - 1) return false
            val u = (us[i] + us[i + 1]) / 2.0; val v = (vs[j] + vs[j + 1]) / 2.0
            return st.none { u > it[0] && u < it[1] && v > it[2] && v < it[3] }
        }
        // raw edges: (horizontal?, at, s0, s1, inS)
        data class Seg(val h: Boolean, val at: Int, var s0: Int, var s1: Int, val inS: Int)
        val raw = mutableListOf<Seg>()
        for (j in vs.indices) for (i in 0 until us.size - 1) {
            val up = filled(i, j - 1); val dn = filled(i, j)
            if (up != dn) raw.add(Seg(true, vs[j], us[i], us[i + 1], if (dn) 1 else -1))
        }
        for (i in us.indices) for (j in 0 until vs.size - 1) {
            val lf = filled(i - 1, j); val rt = filled(i, j)
            if (lf != rt) raw.add(Seg(false, us[i], vs[j], vs[j + 1], if (rt) 1 else -1))
        }
        raw.sortWith(compareBy<Seg>({ if (it.h) 1 else 0 }, { it.at }, { it.inS }, { it.s0 }))
        val segs = mutableListOf<Seg>()
        for (r in raw) {
            val l = segs.lastOrNull()
            if (l != null && l.h == r.h && l.at == r.at && l.inS == r.inS && l.s1 == r.s0) l.s1 = r.s1 else segs.add(r.copy())
        }
        val pieces = mutableListOf<Piece>()
        for (s in segs) {
            val side = if (s.h) (if (s.at == 0) "top" else if (s.at == h) "bottom" else null)
                       else (if (s.at == 0) "left" else if (s.at == w) "right" else null)
            val len = if (s.h) w else h
            val cuts = if (side != null) f.splits[side].orEmpty().map { (it * len).roundToInt() }.filter { it > s.s0 && it < s.s1 }.sorted() else emptyList()
            val ks = listOf(s.s0) + cuts + listOf(s.s1)
            for (k in 0 until ks.size - 1) {
                val a = if (s.h) intArrayOf(ks[k], s.at) else intArrayOf(s.at, ks[k])
                val b = if (s.h) intArrayOf(ks[k + 1], s.at) else intArrayOf(s.at, ks[k + 1])
                pieces.add(Piece(a, b, ks[k + 1] - ks[k], if (s.h) intArrayOf(0, s.inS) else intArrayOf(s.inS, 0), side, s.h))
            }
        }
        val rank = mapOf("top" to 0, "right" to 1, "bottom" to 2, "left" to 3)
        return pieces.sortedWith(compareBy<Piece>({ rank[it.side] ?: 4 },
            { if (it.h) it.a[1] else it.a[0] }, { if (it.h) it.a[0] else it.a[1] }))
    }

    // ---------------------------------------------------------------- guides between faces

    /**
     * A column's face on a wall photo is nearer the lens than the wall, so it
     * looks bigger and pushed away from the point straight in front of the
     * lens — by D/(D-d), D the camera's distance to that wall. The floor or
     * ceiling plan knows the depth, so each side's guide is corrected by it.
     */
    class WallGeo(val L: Int, val D: Double, val uc: Double)
    fun wallGeom(room: Room): Map<String, WallGeo> {
        val ac = 0.5 + room.cx.toDouble() / room.X; val bc = 0.5 + room.cz.toDouble() / room.Z
        return mapOf("front" to WallGeo(room.X, (1 - bc) * room.Z, ac), "back" to WallGeo(room.X, bc * room.Z, 1 - ac),
            "right" to WallGeo(room.Z, (1 - ac) * room.X, 1 - bc), "left" to WallGeo(room.Z, ac * room.X, bc))
    }
    private fun proud(g: WallGeo, d: Double) = min(d, g.D * 0.9)
    fun toTrue(g: WallGeo, u: Double, d: Double) = g.uc + (u - g.uc) * (g.D - proud(g, d)) / g.D
    fun toPhoto(g: WallGeo, u: Double, d: Double) = g.uc + (u - g.uc) * g.D / (g.D - proud(g, d))
    private fun toAB(wall: String, u: Double): DoubleArray = when (wall) {
        "front" -> doubleArrayOf(u, 1.0); "back" -> doubleArrayOf(1 - u, 0.0)
        "right" -> doubleArrayOf(1.0, 1 - u); else -> doubleArrayOf(0.0, u)
    }
    private fun plateUV(plate: String, ab: DoubleArray) = if (plate == "ceiling") ab else doubleArrayOf(ab[0], 1 - ab[1])
    fun spanToRoom(sp: Pair<Double, Double>, u: Double) = sp.first + u * (sp.second - sp.first)
    fun roomToSpan(sp: Pair<Double, Double>, u: Double) = (u - sp.first) / (sp.second - sp.first)

    fun stepName(f: Face, i: Int): String {
        val k = f.steps[i].kind; var n = 0
        for (j in 0..i) if (f.steps[j].kind == k) n++
        return "${stepLabel(k).split(" ")[0]} $n"
    }
    fun detName(f: Face, i: Int): String {
        val t = f.dets[i].type; var n = 0
        for (j in 0..i) if (f.dets[j].type == t) n++
        return "${detailLabel(t).split(" ")[0]} $n"
    }

    /** A wall's column or beam carried onto the floor/ceiling: an open dashed guide. */
    class PlateGuide(val p0: DoubleArray, val p1: DoubleArray, val q0: DoubleArray, val q1: DoubleArray,
                     val kind: String, val along: Int, val corrected: Boolean, val label: String)

    fun plateGuides(room: Room, face: String): List<PlateGuide> {
        if (face != "floor" && face != "ceiling") return emptyList()
        val geo = wallGeom(room); val (w, h) = faceDims(room, face); val out = mutableListOf<PlateGuide>()
        val contacts = room.face(face).steps.map { plateContacts(room, face, it.norm()) }
        for (wall in WALLS) {
            val g = geo.getValue(wall); val f = room.face(wall); val sp = wallSpan(room, wall)
            f.steps.forEachIndexed { i, s0 ->
                val n = s0.norm()
                val su0 = spanToRoom(sp, n.u0); val su1 = spanToRoom(sp, n.u1)
                if (!(if (face == "floor") n.v1 > 0.995 else n.v0 < 0.005)) return@forEachIndexed
                var best: DoubleArray? = null   // ov, d, t0, t1
                for (cs in contacts) for (c in cs) {
                    if (c.wall != wall) continue
                    val t0 = toTrue(g, su0, c.d); val t1 = toTrue(g, su1, c.d); val ov = min(t1, c.u1) - max(t0, c.u0)
                    if (ov > 0 && (best == null || ov > best[0])) best = doubleArrayOf(ov, c.d, t0, t1)
                }
                val u0 = best?.get(2) ?: su0; val u1 = best?.get(3) ?: su1; val depth = best?.get(1) ?: 400.0
                val p0 = plateUV(face, toAB(wall, u0)); val p1 = plateUV(face, toAB(wall, u1))
                val inw = when (wall) { "front" -> intArrayOf(0, -1); "back" -> intArrayOf(0, 1); "right" -> intArrayOf(-1, 0); else -> intArrayOf(1, 0) }
                val dv = if (face == "floor") intArrayOf(inw[0], -inw[1]) else inw
                val step = doubleArrayOf(dv[0] * depth / w, dv[1] * depth / h)
                out.add(PlateGuide(p0, p1, clampUV(p0[0] + step[0], p0[1] + step[1]), clampUV(p1[0] + step[0], p1[1] + step[1]),
                    n.kind, if (dv[0] == 0) 0 else 1, best != null,
                    "${stepName(f, i)} · $wall wall · " + if (best != null) "corrected for ${best[1].roundToInt()} mm depth" else "depth unknown — draw it here to correct"))
            }
        }
        return out
    }

    /** A floor/ceiling plan step, where this wall's photo shows its face. */
    class WallGuide(val u0: Double, val u1: Double, val plate: String, val kind: String, val label: String)

    fun wallGuides(room: Room, face: String): List<WallGuide> {
        if (face !in WALLS) return emptyList()
        val g = wallGeom(room).getValue(face); val sp = wallSpan(room, face); val out = mutableListOf<WallGuide>()
        for (plate in listOf("floor", "ceiling")) room.face(plate).steps.forEachIndexed { i, s0 ->
            val st = s0.norm()
            for (c in plateContacts(room, plate, st)) {
                if (c.wall != face || (plate == "floor" && endsWall(room, st, c))) continue
                out.add(WallGuide(clamp01(roomToSpan(sp, toPhoto(g, c.u0, c.d))), clamp01(roomToSpan(sp, toPhoto(g, c.u1, c.d))),
                    plate, st.kind, "${stepName(room.face(plate), i)} · $plate plan · ${c.d.roundToInt()} mm deep"))
            }
        }
        return out
    }

    // ---------------------------------------------------------------- details, lines, laser

    fun mmU(w: Int, a: Double, b: Double) = abs((b * w).roundToInt() - (a * w).roundToInt())
    fun mmV(h: Int, a: Double, b: Double) = abs((b * h).roundToInt() - (a * h).roundToInt())

    /** A locating dimension of a detail: from [a] to [b] (face uv), to the thing named [to]. */
    class Locator(val a: DoubleArray, val b: DoubleArray, val to: String) {
        val horizontal get() = abs(a[1] - b[1]) < 1e-9
    }

    /**
     * Where a detail sits: up from the floor (walls) or from the nearer edge
     * (floor/ceiling), and across from the nearest thing beside it at the same
     * height — a wall, a column, or another detail.
     */
    fun locators(face: String, f: Face, i: Int): List<Locator> {
        val d = f.dets[i]; val names = EDGE_NAMES.getValue(face); val wall = face in WALLS; val out = mutableListOf<Locator>()
        val ue = if (d.sx == 1) d.u1 else d.u0; val ve = if (d.sy == 1) d.v1 else d.v0
        if (wall) { if (d.v1 < 0.999) out.add(Locator(doubleArrayOf(ue, d.v1), doubleArrayOf(ue, 1.0), names.getValue("v1"))) }
        else {
            val top = d.v0; val bot = 1 - d.v1
            if (min(top, bot) > 0.001) out.add(if (top <= bot) Locator(doubleArrayOf(ue, 0.0), doubleArrayOf(ue, d.v0), names.getValue("v0"))
                                               else Locator(doubleArrayOf(ue, d.v1), doubleArrayOf(ue, 1.0), names.getValue("v1")))
        }
        var leftAt = 0.0; var leftTo = names.getValue("u0"); var rightAt = 1.0; var rightTo = names.getValue("u1")
        val others = f.steps.mapIndexed { k, s -> s.norm().let { doubleArrayOf(it.u0, it.v0, it.u1, it.v1) } to stepName(f, k) } +
            f.dets.mapIndexedNotNull { k, r -> if (k == i) null else doubleArrayOf(r.u0, r.v0, r.u1, r.v1) to detName(f, k) }
        for ((r, name) in others) {
            if (r[3] <= d.v0 || r[1] >= d.v1) continue
            if (r[2] <= d.u0 + 1e-6 && r[2] > leftAt) { leftAt = r[2]; leftTo = name }
            if (r[0] >= d.u1 - 1e-6 && r[0] < rightAt) { rightAt = r[0]; rightTo = name }
        }
        val gl = d.u0 - leftAt; val gr = rightAt - d.u1
        if (gl <= gr && gl > 0.001) out.add(Locator(doubleArrayOf(leftAt, ve), doubleArrayOf(d.u0, ve), leftTo))
        else if (gr > 0.001) out.add(Locator(doubleArrayOf(d.u1, ve), doubleArrayOf(rightAt, ve), rightTo))
        return out
    }

    fun locatorMm(w: Int, h: Int, l: Locator) = if (l.horizontal) mmU(w, l.a[0], l.b[0]) else mmV(h, l.a[1], l.b[1])

    /** True mm length of a line on the face plane — a diagonal is not under-read. */
    fun lineMm(w: Int, h: Int, a: DoubleArray, b: DoubleArray) = hypot((b[0] - a[0]) * w, (b[1] - a[1]) * h).roundToInt()

    /** t along [l] of the point nearest [uv], measured in mm so the face's aspect does not skew it. */
    fun lineProj(w: Int, h: Int, l: MLine, uv: DoubleArray): Double {
        val ax = (l.b[0] - l.a[0]) * w; val ay = (l.b[1] - l.a[1]) * h
        val px = (uv[0] - l.a[0]) * w; val py = (uv[1] - l.a[1]) * h; val l2 = ax * ax + ay * ay
        return if (l2 > 0) (px * ax + py * ay) / l2 else 0.0
    }

    /** Level or upright when within 3°, so a wall run or a height is not off by a hand's tremble. */
    fun lineSnapAxis(w: Int, h: Int, a: DoubleArray, q: DoubleArray): DoubleArray {
        val du = (q[0] - a[0]) * w; val dv = (q[1] - a[1]) * h; val k = tan(Math.toRadians(3.0))
        if (abs(dv) <= abs(du) * k) return doubleArrayOf(q[0], a[1])
        if (abs(du) <= abs(dv) * k) return doubleArrayOf(a[0], q[1])
        return q
    }

    /** Pieces of a line between its dividers, in mm, start to end. */
    fun linePieces(w: Int, h: Int, l: MLine): List<Int> {
        val tot = lineMm(w, h, l.a, l.b); val ts = listOf(0.0) + l.ts.sorted() + listOf(1.0)
        return (1 until ts.size).map { (tot * (ts[it] - ts[it - 1])).roundToInt() }
    }

    /**
     * A room size typed or read with the laser: it changes the room box, and
     * the reading is kept. Room sizes are the one figure that still moves the
     * geometry — every face is drawn from them. Shape dimensions never do; see [Dim].
     */
    fun roomReading(room: Room, k: String, mm: Int) {
        when (k) { "H" -> { room.H = mm; room.ch = min(room.ch, mm - 100) }; "X" -> room.X = mm; else -> room.Z = mm }
        room.laser[k] = mm
    }

    // ---------------------------------------------------------------- dimensions and site figures

    val SIDE_NAME = mapOf("top" to "Top", "right" to "Right", "bottom" to "Bottom", "left" to "Left")

    /**
     * One dimension drawn on a face: its stable [key], what it is called, the
     * figure the photo CALCULATES, and the SITE figure if one was taken (laser
     * or typed). Keys follow the prototype (v23): `E:<side>:<k>` a box-edge
     * piece, `T:<side>` a total, `d:<id>:w|h|lh|lv` or `w<k>`/`h<k>` for a
     * divided arrow, `l:<id>` a line and `l:<id>:<k>` its pieces.
     *
     * A site figure is written beside the calculated one and changes nothing
     * else (Amit, 2026-10-09: "override it ... still preserving calculated
     * measurement ... show just beside it in different colour").
     */
    class Dim(val key: String, val label: String, val calc: Int, val site: Int?)

    /** Box-edge piece keys in [outline] order: `E:<side or step>:<k>`, k counted per side from 1. */
    fun edgeKeys(pieces: List<Piece>): List<String> {
        val per = HashMap<String, Int>()
        return pieces.map { pc -> val sk = pc.side ?: "step"; val k = (per[sk] ?: 0) + 1; per[sk] = k; "E:$sk:$k" }
    }

    fun detKey(d: Detail, part: String) = "d:${d.id}:$part"
    fun lineKey(l: MLine, piece: Int? = null) = if (piece == null) "l:${l.id}" else "l:${l.id}:$piece"

    /** The width arrow's pieces as (key part, mm): "w" when undivided, else "w1", "w2"… */
    fun detWidths(w: Int, d: Detail): List<Pair<String, Int>> {
        val ts = listOf(0.0) + d.sw.sorted() + listOf(1.0)
        return (0 until ts.size - 1).map { k -> (if (ts.size == 2) "w" else "w${k + 1}") to mmU(w, d.u0 + (d.u1 - d.u0) * ts[k], d.u0 + (d.u1 - d.u0) * ts[k + 1]) }
    }
    fun detHeights(h: Int, d: Detail): List<Pair<String, Int>> {
        val ts = listOf(0.0) + d.sh.sorted() + listOf(1.0)
        return (0 until ts.size - 1).map { k -> (if (ts.size == 2) "h" else "h${k + 1}") to mmV(h, d.v0 + (d.v1 - d.v0) * ts[k], d.v0 + (d.v1 - d.v0) * ts[k + 1]) }
    }

    /**
     * Every dimension on a face, in the order the photo draws them: edge
     * pieces, the four totals, each detail (width, height, locators), each
     * line (pieces, then its total). This order is what "next unmeasured"
     * walks, and what the CSV lists.
     */
    fun dims(room: Room, face: String): List<Dim> {
        val f = room.face(face); val (w, h) = faceDims(room, face); val out = mutableListOf<Dim>()
        fun add(key: String, label: String, calc: Int) = out.add(Dim(key, label, calc, f.meas[key]))
        val pcs = outline(room, face, f); val ek = edgeKeys(pcs)
        pcs.forEachIndexed { n, pc -> add(ek[n], "E${n + 1} · ${pc.side?.let { SIDE_NAME.getValue(it).lowercase() + " edge" } ?: "step face"}", pc.len) }
        for (sd in SIDES) add("T:$sd", "${SIDE_NAME.getValue(sd)} total", if (sd == "top" || sd == "bottom") w else h)
        f.dets.forEachIndexed { i, d ->
            val dn = detName(f, i)
            val ws = detWidths(w, d); ws.forEachIndexed { k, (part, mm) -> add(detKey(d, part), "$dn width${if (ws.size == 1) "" else " piece ${k + 1}"}", mm) }
            val hs = detHeights(h, d); hs.forEachIndexed { k, (part, mm) -> add(detKey(d, part), "$dn height${if (hs.size == 1) "" else " piece ${k + 1}"}", mm) }
            for (loc in locators(face, f, i)) add(detKey(d, if (loc.horizontal) "lh" else "lv"), "$dn from ${loc.to}", locatorMm(w, h, loc))
        }
        f.lines.forEachIndexed { i, l ->
            val ps = linePieces(w, h, l)
            if (ps.size == 1) add(lineKey(l), "Line ${i + 1}", ps[0])
            else { ps.forEachIndexed { k, p -> add(lineKey(l, k + 1), "Line ${i + 1} piece ${k + 1}", p) }
                   add(lineKey(l), "Line ${i + 1}", lineMm(w, h, l.a, l.b)) }
        }
        return out
    }

    /** The next dimension after [current] (wrapping round) with no site figure, or null when every one is measured. */
    fun nextUnmeasured(ds: List<Dim>, current: String?): String? {
        val i = ds.indexOfFirst { it.key == current }
        val order = if (i < 0) ds else ds.drop(i + 1) + ds.take(i)
        return order.firstOrNull { it.site == null && it.key != current }?.key
    }

    /** Store a site figure. Geometry is untouched — that is the point. */
    fun setSite(f: Face, key: String, mm: Int) { f.meas[key] = mm }
    fun clearSite(f: Face, key: String) { f.meas.remove(key) }

    /** Drop the site figures of a deleted detail or line, so a stale figure cannot reappear. */
    fun forgetSites(f: Face, prefix: String) { f.meas.keys.removeAll { it == prefix || it.startsWith("$prefix:") } }

    /** 0 not started, 1 partly done, 2 done (every dimension has a site figure). */
    fun faceStatus(room: Room, face: String): Int {
        val f = room.face(face)
        val marked = f.box != null || f.steps.isNotEmpty() || f.dets.isNotEmpty() || f.lines.isNotEmpty() ||
            f.splits.values.any { it.isNotEmpty() } || f.meas.isNotEmpty()
        if (!marked) return 0
        return if (dims(room, face).all { it.site != null }) 2 else 1
    }

    /**
     * Copy a step, detail or line a little down and right, as a new thing
     * with its own id (so it has no site figures yet). Returns its index.
     */
    fun duplicate(f: Face, list: String, i: Int): Int? {
        val du = 0.04; val dv = 0.04
        when (list) {
            "steps" -> { val s = f.steps.getOrNull(i)?.norm() ?: return null
                val ou = min(du, 1 - s.u1); val ov = min(dv, 1 - s.v1)
                f.steps.add(Step(s.kind, s.u0 + ou, s.v0 + ov, s.u1 + ou, s.v1 + ov, s.site)); return f.steps.size - 1 }
            "dets" -> { val d = f.dets.getOrNull(i) ?: return null
                val ou = min(du, 1 - d.u1); val ov = min(dv, 1 - d.v1)
                f.dets.add(Detail(d.type, d.u0 + ou, d.v0 + ov, d.u1 + ou, d.v1 + ov, d.sx, d.sy, d.sw.toMutableList(), d.sh.toMutableList())); return f.dets.size - 1 }
            "line" -> { val l = f.lines.getOrNull(i) ?: return null
                val ou = min(du, 1 - max(l.a[0], l.b[0])); val ov = min(dv, 1 - max(l.a[1], l.b[1]))
                f.lines.add(MLine(doubleArrayOf(l.a[0] + ou, l.a[1] + ov), doubleArrayOf(l.b[0] + ou, l.b[1] + ov), l.ts.toMutableList())); return f.lines.size - 1 }
        }
        return null
    }

    /** One CSV row per dimension: measure, calculated mm, site mm ("" when not taken). */
    fun csvRows(room: Room, face: String): List<Triple<String, Int, String>> =
        dims(room, face).map { Triple(it.label, it.calc, it.site?.toString() ?: "") }

    // ---------------------------------------------------------------- grid, references

    /** The reference grid's settings: spacing across/up, bold every [major] mm, start corner, shift. */
    data class Grid(val on: Boolean = false, val x: Int = 500, val y: Int = 500, val major: Int = 1000,
                    val fromLeft: Boolean = true, val fromBottom: Boolean = true, val sx: Int = 0, val sy: Int = 0,
                    val alpha: Int = 35, val labels: Boolean = true)

    /** Grid lines as face fractions with the mm each stands for, counted from the chosen corner after the shift. */
    fun gridLines(g: Grid, w: Int, h: Int): Pair<List<Pair<Double, Int>>, List<Pair<Double, Int>>> {
        if (!g.on || g.x <= 0 || g.y <= 0) return emptyList<Pair<Double, Int>>() to emptyList()
        val us = mutableListOf<Pair<Double, Int>>(); val vs = mutableListOf<Pair<Double, Int>>()
        var k = ceil((1.0 - g.sx) / g.x).toInt()
        while (true) { val m = k * g.x + g.sx; k++; if (m >= w - 1) break; if (m <= 1) continue
            us.add((if (g.fromLeft) m.toDouble() / w else 1 - m.toDouble() / w) to m) }
        k = ceil((1.0 - g.sy) / g.y).toInt()
        while (true) { val m = k * g.y + g.sy; k++; if (m >= h - 1) break; if (m <= 1) continue
            vs.add((if (g.fromBottom) 1 - m.toDouble() / h else m.toDouble() / h) to m) }
        return us to vs
    }

    /** Everything a divider or a mark can be lined up with on this face. */
    fun references(room: Room, face: String, grid: Grid): Pair<List<Double>, List<Double>> {
        val f = room.face(face); val us = mutableListOf(0.0, 1.0); val vs = mutableListOf(0.0, 1.0)
        f.steps.forEach { val n = it.norm(); us += listOf(n.u0, n.u1); vs += listOf(n.v0, n.v1) }
        f.dets.forEach { us += listOf(it.u0, it.u1); vs += listOf(it.v0, it.v1) }
        plateGuides(room, face).forEach { if (it.along == 0) us += listOf(it.p0[0], it.p1[0]) else vs += listOf(it.p0[1], it.p1[1]) }
        wallGuides(room, face).forEach { us += listOf(it.u0, it.u1) }
        us += f.splits.getValue("top") + f.splits.getValue("bottom"); vs += f.splits.getValue("left") + f.splits.getValue("right")
        val (w, h) = faceDims(room, face); val (gu, gv) = gridLines(grid, w, h)
        us += gu.map { it.first }; vs += gv.map { it.first }
        return us to vs
    }

    /**
     * The guide across the whole face through a line divider — upright for a
     * level line, level for an upright one, both for a slanting one — and
     * whether it runs along something already marked (within 30 mm).
     */
    class LineGuide(val upright: Double?, val level: Double?, val left: Int, val right: Int, val onRef: Boolean)

    fun lineGuide(room: Room, face: String, grid: Grid, l: MLine, t: Double): LineGuide {
        val (w, h) = faceDims(room, face); val q = l.at(t)
        val du = abs(l.b[0] - l.a[0]) * w; val dv = abs(l.b[1] - l.a[1]) * h
        val ang = Math.toDegrees(atan2(dv, du)); val up = ang < 70; val lv = ang > 20
        val tot = lineMm(w, h, l.a, l.b); val a = (tot * t).roundToInt()
        val (ru, rv) = references(room, face, grid)
        val onU = up && ru.any { abs(it - q[0]) < 30.0 / w }; val onV = lv && rv.any { abs(it - q[1]) < 30.0 / h }
        return LineGuide(if (up) q[0] else null, if (lv) q[1] else null, a, tot - a, onU || onV)
    }

    fun clamp01(x: Double) = min(1.0, max(0.0, x))
    fun clampUV(u: Double, v: Double) = doubleArrayOf(clamp01(u), clamp01(v))
}
