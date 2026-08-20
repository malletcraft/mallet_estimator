package com.malletcrafts.sitephotos

import android.content.Context
import com.malletcrafts.sitephotos.pano.Annotation
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Annotations on this phone: one JSON file per (capture, face) under
 * filesDir/annotations, plus a .dirty marker until the server has the same
 * bytes. The truth for images stays MediaStore; the truth for measurements
 * is this JSON — never burned into pixels.
 */
class AnnotationStore(context: Context) {

    private val dir = File(context.filesDir, "annotations").apply { mkdirs() }

    private fun fileOf(deviceId: String, face: String) =
        File(dir, "${deviceId}_$face.json")

    private fun dirtyOf(deviceId: String, face: String) =
        File(dir, "${deviceId}_$face.dirty")

    fun load(deviceId: String, face: String): Annotation.FaceAnnotations {
        val f = fileOf(deviceId, face)
        if (!f.exists()) return Annotation.FaceAnnotations()
        return runCatching { decode(JSONObject(f.readText())) }
            .getOrDefault(Annotation.FaceAnnotations())
    }

    fun save(deviceId: String, face: String, ann: Annotation.FaceAnnotations) {
        val f = fileOf(deviceId, face)
        if (ann.isEmpty) f.delete() else f.writeText(encode(ann).toString())
        dirtyOf(deviceId, face).writeText("")   // empty still syncs: it deletes
    }

    /** Faces of this capture whose annotations the server hasn't seen. */
    fun dirtyFaces(deviceId: String): List<String> =
        dir.listFiles { f -> f.name.startsWith("${deviceId}_") && f.name.endsWith(".dirty") }
            ?.map { it.name.removePrefix("${deviceId}_").removeSuffix(".dirty") }
            ?: emptyList()

    fun markSynced(deviceId: String, face: String) {
        dirtyOf(deviceId, face).delete()
    }

    /** True when any face of this capture carries annotations. */
    fun hasAny(deviceId: String): Boolean =
        dir.listFiles { f -> f.name.startsWith("${deviceId}_") && f.name.endsWith(".json") }
            ?.isNotEmpty() == true

    companion object {
        fun encode(ann: Annotation.FaceAnnotations): JSONObject = JSONObject()
            .put("lines", JSONArray().apply {
                ann.lines.forEach {
                    put(JSONObject().put("x1", it.x1).put("y1", it.y1)
                        .put("x2", it.x2).put("y2", it.y2).put("mm", it.mm))
                }
            })
            .put("pins", JSONArray().apply {
                ann.pins.forEach {
                    put(JSONObject().put("x", it.x).put("y", it.y).put("text", it.text))
                }
            })

        fun decode(o: JSONObject): Annotation.FaceAnnotations {
            val lines = mutableListOf<Annotation.Line>()
            val pins = mutableListOf<Annotation.Pin>()
            o.optJSONArray("lines")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val l = arr.getJSONObject(i)
                    lines += Annotation.Line(l.getDouble("x1"), l.getDouble("y1"),
                        l.getDouble("x2"), l.getDouble("y2"), l.getInt("mm"))
                }
            }
            o.optJSONArray("pins")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    pins += Annotation.Pin(p.getDouble("x"), p.getDouble("y"),
                        p.getString("text"))
                }
            }
            return Annotation.FaceAnnotations(lines, pins)
        }
    }
}
