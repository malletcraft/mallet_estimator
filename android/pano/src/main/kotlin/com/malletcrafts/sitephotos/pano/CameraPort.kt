package com.malletcrafts.sitephotos.pano

/**
 * The app's view of a 360 camera, SDK-free. The proprietary Insta360 SDK
 * lives behind this interface in the optional :camera module; the app
 * discovers the implementation by class name at runtime and otherwise falls
 * back to gallery-pick. Living in :pano keeps it on every classpath without
 * dragging Android or the SDK anywhere.
 */
interface CameraPort {

    /** True when a real SDK is linked and initialised. */
    val ready: Boolean

    /** Call once from Application.onCreate. Takes the Application untyped so
     *  this module stays pure JVM. */
    fun init(app: Any)

    /** True while a camera is connected (any transport). */
    val connected: Boolean

    /** Open the Wi-Fi connection (must be called on the main thread).
     *  [onChange] fires with every connect/disconnect, plus an error text
     *  when the SDK reports one. */
    fun connect(onChange: (connected: Boolean, error: String?) -> Unit)

    fun disconnect()

    /** Take a photo on the connected camera, stitch it, and write the
     *  equirectangular JPG to [targetPath]. The callback delivers the path
     *  on success — from there the pano enters the exact pipeline
     *  gallery-picked files use. May be called from any thread; calls back
     *  on an arbitrary thread. */
    fun shootAndExport(targetPath: String, onDone: (Result<String>) -> Unit)
}
