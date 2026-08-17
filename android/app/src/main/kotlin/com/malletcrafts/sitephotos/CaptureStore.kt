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
    SQLiteOpenHelper(context, "captures.db", null, 1) {

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
                 error TEXT
               )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) = Unit

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
        })
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
