package com.malletcrafts.sitephotos

import android.app.Application

class SitePhotosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // The Insta360 SDKs demand Application-time init. A stub-less build
        // resolves no port and this is a no-op.
        CameraCapability.port?.init(this)
    }
}
