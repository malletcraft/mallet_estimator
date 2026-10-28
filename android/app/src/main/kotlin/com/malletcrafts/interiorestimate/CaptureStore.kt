package com.malletcrafts.interiorestimate

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.malletcrafts.interiorestimate.pano.CaptureSchema
import org.json.JSONObject
import java.io.File

/**
 * The on-device queue and the cached masters. Plain SQLite on purpose — this
 * table IS the offline story, and the fewer layers between "the shutter
 * fired" and "a row exists", the fewer ways a capture can be lost in a
 * basement.
 */
class CaptureStore(context: Context) :
    SQLiteOpenHelper(context, "captures.db", null, CaptureSchema.VERSION) {

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
                 sku TEXT NOT NULL DEFAULT '',
                 kind TEXT NOT NULL DEFAULT '360',
                 fov REAL NOT NULL DEFAULT 0,
                 room_l_in REAL NOT NULL DEFAULT 0,
                 room_w_in REAL NOT NULL DEFAULT 0,
                 room_h_in REAL NOT NULL DEFAULT 0,
                 work INTEGER
               )"""
        )
    }

    /** ADD COLUMN, never a rebuild.
     *
     *  This table IS the offline queue, and the phones that most need an
     *  upgrade are the ones carrying unsent captures. A drop-and-recreate
     *  migration would throw those away on the version that finally fixed
     *  the thing they were waiting for, so every future change to this
     *  schema has to be additive in exactly this shape.
     *
     *  The statements are CaptureSchema's (in :pano, where a test pins them):
     *  a new version appends its ADD COLUMN there, raises its VERSION, and
     *  gives the column the same shape in onCreate above. Nothing else runs
     *  here, and each statement runs in its own runCatching, as each did
     *  under its own `if (old < N)` before version 6. */
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        for (sql in CaptureSchema.upgrade(old)) runCatching { db.execSQL(sql) }
    }

    /** A downgrade leaves the database exactly as it is.
     *
     *  versionCode is 100 + the CI run number, so reverting a schema change
     *  ships as a NEWER build carrying OLDER schema code, and that build meets
     *  a database whose version is higher than its own. SQLiteOpenHelper's
     *  default answer is to throw, so the app could not open its own queue --
     *  on exactly the phone that may be holding unsent captures. Doing nothing
     *  is safe, and stays safe only while the rule above holds:
     *
     *  - every column ever added is additive: ADD COLUMN, nullable or with a
     *    DEFAULT, never a rebuild, a rename or a drop, and onCreate gives each
     *    column the same shape its ADD COLUMN does. So an older build's insert,
     *    which does not name the newer columns, still succeeds and leaves them
     *    NULL or at their default;
     *  - rows are read BY NAME with getColumnIndexOrThrow (query, below) and
     *    every write names its columns, so a column this build does not know
     *    is never read and never touched.
     *
     *  The helper then writes this build's version back, so the next newer
     *  build runs its upgrade again over columns that already exist. That is
     *  why every upgrade statement runs inside runCatching: the
     *  duplicate-column failure is the expected case there, and a
     *  down-then-up cycle is harmless. A schema change that is NOT additive
     *  must replace this no-op in the same commit. */
    override fun onDowngrade(db: SQLiteDatabase, old: Int, new: Int) {}

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
        /** "360" or "Photo". A repair job is a close-up of a broken hinge;
         *  splitting that into six faces would be nonsense. */
        val kind: String = "360",
        /** The angle this phone actually split at, so the SERVER can split
         *  the same pano the same way.
         *
         *  0 means "not recorded" — every row queued by a build before this
         *  one, and every Photo, which is not projected at all. The sync
         *  omits the argument then, and the bench applies its own default,
         *  which is exactly what those captures got at the time. */
        val fov: Double = 0.0,
        val roomLengthIn: Double = 0.0,
        val roomWidthIn: Double = 0.0,
        val roomHeightIn: Double = 0.0,
        /** The work this capture is filed under: 0 the quote journey, else
         *  one of the sixteen work ids (Album.WORKS). null is "filed by its
         *  stage": every row from before schema 6, and every capture until
         *  the phone files by work -- nothing writes this yet. */
        val work: Int? = null,
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
            put("kind", c.kind)
            put("fov", c.fov)
            put("room_l_in", c.roomLengthIn)
            put("room_w_in", c.roomWidthIn)
            put("room_h_in", c.roomHeightIn)
            // Only when filed by work: a row left out stays NULL, "filed by its stage".
            c.work?.let { put("work", it) }
        })
    }

    /**
     * Remove one capture from this phone.
     *
     * Nothing in the app could delete anything, so a room typed wrong or a
     * shutter pressed by accident was permanent. Deleting the ROW is this
     * class's job; the files are the caller's, because MediaStore entries and
     * app-private files are deleted through different APIs and pretending
     * otherwise here would hide a failure.
     */
    fun delete(deviceId: String) {
        writableDatabase.delete("captures", "device_id = ?", arrayOf(deviceId))
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

    /** File a capture by work: 0 the quote journey, else one of the sixteen
     *  work ids (Album.WORKS). The stage it was taken at stays as history.
     *  Never null: NULL in this column means "filed by its stage", a row from
     *  before the phone filed by work, and a re-file must not make a capture
     *  look older. */
    fun setWork(deviceId: String, work: Int) {
        writableDatabase.update("captures", ContentValues().apply {
            put("work", work)
        }, "device_id = ?", arrayOf(deviceId))
    }

    /** The room's measured size, in inches, as survey prep now has it (v51: the
     *  L · W · H line there is editable). Only the phone's row moves: a capture
     *  already on the bench keeps the size it was synced with there. */
    fun setRoomSize(deviceId: String, lengthIn: Double, widthIn: Double, heightIn: Double) {
        writableDatabase.update("captures", ContentValues().apply {
            put("room_l_in", lengthIn)
            put("room_w_in", widthIn)
            put("room_h_in", heightIn)
        }, "device_id = ?", arrayOf(deviceId))
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
                    kind = cur.getString(ix("kind")) ?: "360",
                    fov = runCatching { cur.getDouble(ix("fov")) }.getOrDefault(0.0),
                    roomLengthIn = runCatching { cur.getDouble(ix("room_l_in")) }.getOrDefault(0.0),
                    roomWidthIn = runCatching { cur.getDouble(ix("room_w_in")) }.getOrDefault(0.0),
                    roomHeightIn = runCatching { cur.getDouble(ix("room_h_in")) }.getOrDefault(0.0),
                    // NULL must stay null: getInt reads it as 0, the quote journey,
                    // which would file every capture not yet filed by work there.
                    work = runCatching { ix("work").let { i -> if (cur.isNull(i)) null else cur.getInt(i) } }.getOrNull(),
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
