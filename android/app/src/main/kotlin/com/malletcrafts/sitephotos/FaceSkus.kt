package com.malletcrafts.sitephotos

import android.content.Context
import org.json.JSONObject

/**
 * What work is expected on each FACE of a capture.
 *
 * Amit, 2026-08-22: "why no sku per foto?" Because the tag lived on the
 * capture, and a capture is usually a 360 — which is the record of a whole
 * ROOM and cannot be one article. You cannot say "this 360 is the wardrobe".
 * Each of its six faces, though, IS a single wall, floor or ceiling, and that
 * is precisely the thing a SKU describes. A flat photo is the same case with
 * one face.
 *
 * Held on the phone first and pushed on sync, for the ordinary reason: the
 * moment somebody decides what a wall needs is the moment they are standing
 * in front of it, which is rarely the moment there is signal.
 */
object FaceSkus {

    private fun prefs(context: Context) =
        context.getSharedPreferences("faceskus", Context.MODE_PRIVATE)

    private fun key(captureId: String, face: String) = "$captureId|${face.lowercase()}"

    /** Pending pushes: the same key with a "!" prefix, so one pass over the
     *  map finds everything the bench has not been told about yet. */
    private fun dirtyKey(captureId: String, face: String) = "!" + key(captureId, face)

    fun set(context: Context, captureId: String, face: String, sku: String) {
        prefs(context).edit()
            .putString(key(captureId, face), sku)
            .putBoolean(dirtyKey(captureId, face), true)
            .apply()
    }

    /** face -> sku for one capture. Blank values are absences, not tags. */
    fun of(context: Context, captureId: String): Map<String, String> {
        val out = HashMap<String, String>()
        val head = "$captureId|"
        for ((k, v) in prefs(context).all) {
            if (!k.startsWith(head)) continue
            val sku = (v as? String).orEmpty()
            if (sku.isBlank()) continue
            out[k.removePrefix(head)] = sku
        }
        return out
    }

    /** What the bench sent back, adopted for faces this phone has no unsent
     *  change on — the same rule the annotation pull follows, and for the
     *  same reason: a pull must never eat a decision made here. */
    fun acceptFromServer(context: Context, captureId: String, remote: JSONObject) {
        val p = prefs(context)
        val edit = p.edit()
        val keys = remote.keys()
        while (keys.hasNext()) {
            val face = keys.next()
            if (p.getBoolean(dirtyKey(captureId, face), false)) continue
            edit.putString(key(captureId, face), remote.optString(face))
        }
        edit.apply()
    }

    /** Every (captureId, face, sku) still owed to the bench. */
    fun pending(context: Context): List<Triple<String, String, String>> {
        val p = prefs(context)
        val out = mutableListOf<Triple<String, String, String>>()
        for ((k, v) in p.all) {
            if (!k.startsWith("!")) continue
            if (v != true) continue
            val real = k.removePrefix("!")
            val id = real.substringBefore('|')
            val face = real.substringAfter('|')
            if (id.isBlank() || face.isBlank()) continue
            out.add(Triple(id, face, p.getString(real, "").orEmpty()))
        }
        return out
    }

    fun markSynced(context: Context, captureId: String, face: String) {
        prefs(context).edit().remove(dirtyKey(captureId, face)).apply()
    }
}
