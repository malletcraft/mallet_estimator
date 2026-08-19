package com.malletcrafts.sitephotos.camera

/**
 * The single doorway between the app and the Insta360 SDK. The app never
 * references this class at compile time — it discovers it by name
 * (reflection in CameraCapability), so every app build compiles whether or
 * not this module is included.
 *
 * Scaffold state: the SDK AAR is not wired yet (it is parked privately and
 * dropped into libs/ at build time). Each TODO below names the SDK surface
 * it will call, per the Insta360Develop CameraSDK docs, so the wiring is a
 * fill-in, not a design session.
 */
object X3Bridge {

    /** True once the real SDK is linked and initialised. The app shows the
     *  direct-capture entry only when this reports true. */
    val ready: Boolean get() = false

    /** Join + handshake with the X3 over its Wi-Fi.
     *  TODO(SDK): InstaCameraManager.getInstance().openCamera(CONNECT_TYPE_WIFI) */
    fun connect(): Boolean = false

    /** List panoramas shot since [sinceMillis], newest first.
     *  TODO(SDK): WorkWrapper listing via InstaCameraManager camera files. */
    fun listRecent(sinceMillis: Long): List<String> = emptyList()

    /** Download one camera file into [destPath] and return true on success.
     *  The pano then enters the exact pipeline gallery-picked files use:
     *  FaceWriter split → MediaStore → SyncWorker.
     *  TODO(SDK): file download API + (if needed) stitch via MediaSDK. */
    fun downloadTo(cameraFile: String, destPath: String): Boolean = false
}
