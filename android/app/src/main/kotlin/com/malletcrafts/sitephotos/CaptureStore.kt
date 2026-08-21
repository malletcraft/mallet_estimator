package com.malletcrafts.sitephotos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.io.File

/**
 * The on-device queue and the cached masters. Plain SQLite on purpose — this
 * table IS the offline story, and the fewer layers between "the shutter
 * fired" and "a row exists", the fewer ways a capture can be lost in a
 * basement.
 */
class CaptureStore(context: Context) :
    SQLiteOpenHelper(context, "captures.db", null, 2) {

    private val mastersFile = File(context.filesDir, "masters.json")

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE captures (
                 device_id TEXT PRIMARY KEY,
                 project TEXT NOT NULL,
                 project_title TEXT NOT NULL,
                 customer_name TEXT NOT NULL DEFAULT '',
                 room TEXT NOT NULL,
                 stage TEXT NOT NULL DEFAULT '',
                 capture_date TEXT NOT NULL,
                 pano_path TEXT NOT NULL,
                 created_at INTEGER NOT NULL,
                 state TEXT NOT NULL DEFAULT 'LOCAL',
                 server_name TEXT,
                 error TEXT,
                 sku TEXT NOT NULL DEFAULT ''
               )"""
        )
    }

    /** ADD COLUMN, never a rebuild.
     *
     *  This table IS the offline queue, and the phones that most need an
     *  upgrade are the ones carrying unsent captures. A drop-and-recreate
     *  migration would throw those away on the version that finally fixed
     *  the thing they were waiting for, so every future change to this
     *  schema has to be additive in exactly this shape. */
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        if (old < 2) {
            runCatching {
                db.execSQL("ALTER TABLE captures ADD COLUMN sku TEXT NOT NULL DEFAULT ''")
            }
        }
    }

    data class Capture(
        val deviceId: String,
        val project: String,
        val projectTitle: String,
        val customerName: String,
        val room: String,
        val stage: String,
        val captureDate: String,
        val panoPath: String,
        val createdAt: Long,
        val state: String,
        val serverName: String?,
        val error: String?,
        /** The Estimate SKU this photo is filed against, or "" for a room
         *  shot. Local until the next sync carries it up. */
        val sku: String = "",
    )

    fun insert(c: Capture) {
        writableDatabase.insertOrThrow("captures", null, ContentValues().apply {
            put("device_id", c.deviceId)
            put("project", c.project)
            put("project_title", c.projectTitle)
            put("customer_name", c.customerName)
            put("room", c.room)
            put("stage", c.stage)
            put("capture_date", c.captureDate)
            put("pano_path", c.panoPath)
            put("created_at", c.createdAt)
            put("state", c.state)
            put("sku", c.sku)
        })
    }

    /** Re-file a capture: its stage, its SKU, or both.
     *
     *  Both are set at the shutter from what the project was at, and both
     *  are routinely wrong by the time anyone looks. Passing null leaves a
     *  field alone — a stage move must not silently drop the SKU. */
    fun setTags(deviceId: String, stage: String? = null, sku: String? = null) {
        val values = ContentValues().apply {
            if (stage != null) put("stage", stage)
            if (sku != null) put("sku", sku)
        }
        if (values.size() == 0) return
        writableDatabase.update("captures", values, "device_id = ?", arrayOf(deviceId))
    }

    /** The sync step that adopts a device-typed site: once the server has
     *  matched or minted the real Project, the row learns its id. */
    fun setProject(deviceId: String, project: String) {
        writableDatabase.update("captures", ContentValues().apply {
            put("project", project)
        }, "device_id = ?", arrayOf(deviceId))
    }

    fun setState(deviceId: String, state: String, serverName: String? = null,
                 error: String? = null) {
        writableDatabase.update("captures", ContentValues().apply {
            put("state", state)
            if (serverName != null) put("server_name", serverName)
            put("error", error)
        }, "device_id = ?", arrayOf(deviceId))
    }

    fun all(): List<Capture> = query("SELECT * FROM captures ORDER BY created_at DESC")

    fun pending(): List<Capture> =
        query("SELECT * FROM captures WHERE state != 'SYNCED' ORDER BY created_at ASC")

    private fun query(sql: String): List<Capture> {
        val out = mutableListOf<Capture>()
        readableDatabase.rawQuery(sql, null).use { cur ->
            val ix = { name: String -> cur.getColumnIndexOrThrow(name) }
            while (cur.moveToNext()) {
                out.add(Capture(
                    deviceId = cur.getString(ix("device_id")),
                    project = cur.getString(ix("project")),
                    projectTitle = cur.getString(ix("project_title")),
                    customerName = cur.getString(ix("customer_name")),
                    room = cur.getString(ix("room")),
                    stage = cur.getString(ix("stage")),
                    captureDate = cur.getString(ix("capture_date")),
                    panoPath = cur.getString(ix("pano_path")),
                    createdAt = cur.getLong(ix("created_at")),
                    state = cur.getString(ix("state")),
                    serverName = cur.getString(ix("server_name")),
                    error = cur.getString(ix("error")),
                    sku = cur.getString(ix("sku")) ?: "",
                ))
            }
        }
        return out
    }

    // ---- masters --------------------------------------------------------
    // The last good bootstrap, kept on the device — same rule as the PWA: an
    // offline shell that opens to empty pickers is not offline support.

    fun saveMasters(json: JSONObject) {
        mastersFile.writeText(json.toString())
    }

    fun masters(): JSONObject? =
        runCatching { JSONObject(mastersFile.readText()) }.getOrNull()
}
