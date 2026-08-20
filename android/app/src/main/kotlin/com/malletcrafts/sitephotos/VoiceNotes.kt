package com.malletcrafts.sitephotos

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Voice notes for annotation pins.
 *
 * Why they exist, in Amit's words: on site you often do not have a free
 * hand for typing — you are holding a laser, or a tape, or a torch. Saying
 * "this beam drops 200 below the slab, wardrobe has to stop short" takes
 * three seconds and types in thirty.
 *
 * Clips live beside the annotations in the app's own files directory, named
 * after the capture and face, so a phone that never gets signal on site
 * still records and still plays back — the upload is a separate, later
 * concern handled by SyncWorker.
 */
object VoiceNotes {

    private const val DIR = "audio"

    fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    /** m4a/AAC: small, universally playable, and what a phone records best. */
    fun newName(deviceId: String, face: String): String =
        "${deviceId}_${face}_${System.currentTimeMillis()}.m4a"

    class Recorder(private val context: Context) {
        private var rec: MediaRecorder? = null
        var current: File? = null
            private set

        fun start(name: String): Boolean = runCatching {
            val out = file(context, name)
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context)
                    else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(out.absolutePath)
            r.prepare()
            r.start()
            rec = r
            current = out
            true
        }.getOrElse {
            release()
            false
        }

        /** Returns the finished file, or null if nothing usable was captured.
         *  A recorder stopped within a moment of starting throws rather than
         *  writing a valid file — that is a discarded tap, not a note. */
        fun stop(): File? {
            val r = rec ?: return null
            val out = current
            val ok = runCatching { r.stop() }.isSuccess
            release()
            if (!ok || out == null || !out.exists() || out.length() < 1024) {
                out?.delete()
                return null
            }
            return out
        }

        fun cancel() {
            val out = current
            runCatching { rec?.stop() }
            release()
            out?.delete()
        }

        private fun release() {
            runCatching { rec?.release() }
            rec = null
            current = null
        }
    }

    /** One player at a time — two voice notes talking over each other helps
     *  nobody. */
    private var player: MediaPlayer? = null

    fun play(path: String, onDone: () -> Unit = {}) {
        stopPlayback()
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener { stopPlayback(); onDone() }
                prepare()
                start()
            }
        }.onFailure { onDone() }
    }

    fun stopPlayback() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}
