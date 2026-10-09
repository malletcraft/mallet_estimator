package com.malletcrafts.sitephotos

import android.content.Context
import com.malletcrafts.sitephotos.pano.FacePrep
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Face Prep marks for one capture, on the phone: filesDir/faceprep/<captureId>.json.
 *
 * Local only for now. The marks are the work that produces the ImageMeter
 * photos and the measures list; nothing on the bench reads them yet, so
 * syncing them would be a second copy with no reader — exactly what Amit's
 * "no fallbacks" rule warns against. When a reader exists, the sync comes
 * with it. Deleting the capture deletes this file with it.
 */
class FacePrepStore(context: Context) {
    private val dir = File(context.filesDir, "faceprep").apply { mkdirs() }
    private fun file(id: String) = File(dir, "$id.json")

    fun exists(id: String) = file(id).exists()
    fun delete(id: String) { runCatching { file(id).delete() } }

    fun load(id: String): FacePrep.Room? = runCatching {
        val f = file(id); if (!f.exists()) null else decode(JSONObject(f.readText()))
    }.getOrNull()

    fun save(id: String, room: FacePrep.Room) {
        // Write-then-rename: a phone that dies mid-save keeps the last good copy
        // instead of a half-written file that decodes to nothing.
        val tmp = File(dir, "$id.json.tmp")
        tmp.writeText(encode(room).toString())
        tmp.renameTo(file(id))
    }

    companion object {
        fun encode(r: FacePrep.Room): JSONObject = JSONObject().apply {
            put("v", 1); put("H", r.H); put("X", r.X); put("Z", r.Z); put("cx", r.cx); put("cz", r.cz); put("ch", r.ch)
            put("laser", JSONObject(r.laser as Map<*, *>))
            put("faces", JSONObject().apply {
                for ((name, f) in r.faces) put(name, JSONObject().apply {
                    f.box?.let { b -> put("box", JSONArray(listOf(b.x0, b.y0, b.x1, b.y1))) }
                    put("steps", JSONArray(f.steps.map { s ->
                        JSONObject().put("kind", s.kind).put("u0", s.u0).put("v0", s.v0).put("u1", s.u1).put("v1", s.v1).put("site", s.site) }))
                    put("splits", JSONObject().apply { for ((side, ts) in f.splits) put(side, JSONArray(ts)) })
                    put("dets", JSONArray(f.dets.map { d ->
                        JSONObject().put("type", d.type).put("u0", d.u0).put("v0", d.v0).put("u1", d.u1).put("v1", d.v1)
                            .put("sx", d.sx).put("sy", d.sy).put("sw", JSONArray(d.sw)).put("sh", JSONArray(d.sh))
                            .put("lz", JSONObject(d.lz as Map<*, *>)) }))
                    put("lines", JSONArray(f.lines.map { l ->
                        JSONObject().put("a", JSONArray(l.a.toList())).put("b", JSONArray(l.b.toList()))
                            .put("ts", JSONArray(l.ts)).apply { l.len?.let { put("len", it) } } }))
                })
            })
        }

        private fun JSONArray?.doubles(): MutableList<Double> =
            if (this == null) mutableListOf() else MutableList(length()) { getDouble(it) }

        private fun JSONObject?.ints(): MutableMap<String, Int> {
            val out = mutableMapOf<String, Int>(); if (this == null) return out
            for (k in keys()) out[k] = optInt(k)
            return out
        }

        fun decode(o: JSONObject): FacePrep.Room {
            val r = FacePrep.Room(o.getInt("H"), o.getInt("X"), o.getInt("Z"), o.optInt("cx"), o.optInt("cz"), o.optInt("ch", o.getInt("H") / 2))
            r.laser.putAll(o.optJSONObject("laser").ints())
            val faces = o.optJSONObject("faces") ?: JSONObject()
            for (name in FacePrep.FACE_ORDER) {
                val fo = faces.optJSONObject(name) ?: continue
                val f = r.face(name)
                fo.optJSONArray("box")?.let { if (it.length() == 4) f.box = FacePrep.Box(it.getDouble(0), it.getDouble(1), it.getDouble(2), it.getDouble(3)) }
                fo.optJSONArray("steps")?.let { a -> for (i in 0 until a.length()) { val s = a.getJSONObject(i)
                    f.steps.add(FacePrep.Step(s.getString("kind"), s.getDouble("u0"), s.getDouble("v0"), s.getDouble("u1"), s.getDouble("v1"), s.optBoolean("site"))) } }
                fo.optJSONObject("splits")?.let { sp -> for (side in FacePrep.SIDES) f.splits[side] = sp.optJSONArray(side).doubles() }
                fo.optJSONArray("dets")?.let { a -> for (i in 0 until a.length()) { val d = a.getJSONObject(i)
                    f.dets.add(FacePrep.Detail(d.getString("type"), d.getDouble("u0"), d.getDouble("v0"), d.getDouble("u1"), d.getDouble("v1"),
                        d.optInt("sx"), d.optInt("sy"), d.optJSONArray("sw").doubles(), d.optJSONArray("sh").doubles(), d.optJSONObject("lz").ints())) } }
                fo.optJSONArray("lines")?.let { a -> for (i in 0 until a.length()) { val l = a.getJSONObject(i)
                    val pa = l.getJSONArray("a"); val pb = l.getJSONArray("b")
                    f.lines.add(FacePrep.MLine(doubleArrayOf(pa.getDouble(0), pa.getDouble(1)), doubleArrayOf(pb.getDouble(0), pb.getDouble(1)),
                        l.optJSONArray("ts").doubles(), if (l.has("len")) l.getInt("len") else null)) } }
            }
            return r
        }
    }
}
