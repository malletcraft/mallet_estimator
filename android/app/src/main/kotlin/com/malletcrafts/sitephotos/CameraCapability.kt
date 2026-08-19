package com.malletcrafts.sitephotos

/**
 * Discovery seam for the optional :camera module (Insta360 X3 direct
 * connection). The bridge class is looked up by NAME, never imported, so
 * this file — and every :app build — compiles whether or not the camera
 * module (and the proprietary SDK it wraps) is present. Gallery-pick
 * remains the capture path whenever the probe fails.
 */
object CameraCapability {

    private const val BRIDGE = "com.malletcrafts.sitephotos.camera.X3Bridge"

    /** True when this build carries the camera module AND its SDK reports
     *  ready. The UI shows the direct-capture entry only then. */
    fun available(): Boolean = runCatching {
        val cls = Class.forName(BRIDGE)
        val bridge = cls.getDeclaredField("INSTANCE").get(null)
        cls.getMethod("getReady").invoke(bridge) == true
    }.getOrDefault(false)
}
