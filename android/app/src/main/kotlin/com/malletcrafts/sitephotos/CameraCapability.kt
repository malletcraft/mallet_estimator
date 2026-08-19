package com.malletcrafts.sitephotos

import com.malletcrafts.sitephotos.pano.CameraPort

/**
 * Discovery seam for the optional :camera module (Insta360 X3 direct
 * connection). The implementation class is looked up by NAME, never
 * imported, so every :app build compiles whether or not the camera module
 * (and the proprietary SDK it wraps) is present. The typed surface the app
 * actually talks to is CameraPort, which lives in :pano and is always on
 * the classpath. Gallery-pick remains the capture path whenever the probe
 * comes back empty.
 */
object CameraCapability {

    private const val BRIDGE = "com.malletcrafts.sitephotos.camera.X3Bridge"

    val port: CameraPort? by lazy {
        runCatching {
            Class.forName(BRIDGE).getDeclaredField("INSTANCE").get(null) as? CameraPort
        }.getOrNull()
    }

    /** True when this build carries a ready camera bridge. */
    fun available(): Boolean = port?.ready == true
}
