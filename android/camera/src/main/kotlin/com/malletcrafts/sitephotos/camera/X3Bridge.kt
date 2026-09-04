package com.malletcrafts.sitephotos.camera

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import com.arashivision.sdkcamera.InstaCameraSDK
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.ICameraChangedCallback
import com.arashivision.sdkcamera.camera.callback.ICaptureStatusListener
import com.arashivision.sdkmedia.InstaMediaSDK
import com.arashivision.sdkmedia.export.ExportImageParamsBuilder
import com.arashivision.sdkmedia.export.ExportUtils
import com.arashivision.sdkmedia.export.IExportCallback
import com.arashivision.sdkmedia.work.WorkWrapper
import com.malletcrafts.sitephotos.pano.CameraPort

/**
 * The only class that touches the Insta360 SDK. Flow, per the
 * Insta360Develop CameraSDK docs: openCamera(WIFI) on the main thread →
 * startNormalCapture() → onCaptureFinish(filePaths) → MediaSDK export
 * (ExportMode.PANORAMA, dynamic stitch) → stitched equirectangular JPG at
 * the target path. The app never sees an SDK type — only CameraPort.
 */
object X3Bridge : CameraPort {

    override val ready: Boolean = true

    private var statusListener: ((Boolean, String?) -> Unit)? = null

    // THE CAMERA'S Wi-Fi HAS NO INTERNET, and on Android 10+ that is enough to
    // break the connection on its own.
    //
    // Joining the X3's access point does NOT make it the process's default
    // network: Android keeps routing through mobile data because the AP fails
    // its captive-portal check, so every socket the SDK opens to 192.168.42.1
    // leaves over cellular and never arrives. The camera is associated, the
    // phone shows it connected, and openCamera fails anyway — which is exactly
    // the shape of the connect error Amit is seeing.
    //
    // The fix is to ask for the Wi-Fi transport explicitly and bind this
    // PROCESS to it. The subtlety that catches people: NetworkRequest.Builder
    // adds NET_CAPABILITY_INTERNET by default, and a request carrying it can
    // never match an access point with no internet — so it has to be removed
    // or the callback simply never fires.
    private var appRef: Application? = null
    private var netCallback: ConnectivityManager.NetworkCallback? = null

    private fun cm(): ConnectivityManager? =
        appRef?.getSystemService(ConnectivityManager::class.java)

    private fun bindToCameraWifi(onBound: (String?) -> Unit) {
        val manager = cm() ?: return onBound("no ConnectivityManager")
        releaseNetwork()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val main = Handler(Looper.getMainLooper())
        var settled = false
        val settle = { problem: String? ->
            if (!settled) { settled = true; main.post { onBound(problem) } }
        }
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                manager.bindProcessToNetwork(network)
                settle(null)
            }
        }
        netCallback = cb
        // registerNetworkCallback, NOT requestNetwork. requestNetwork asks the
        // system to BRING UP a network and is documented as needing
        // CHANGE_NETWORK_STATE, which this module does not declare — it would
        // have thrown SecurityException on the phone and looked exactly like
        // another connect failure. Nothing here needs a network created: the
        // person has already joined the X3's Wi-Fi in Settings, so all this
        // wants is a handle to the one that exists, which ACCESS_NETWORK_STATE
        // (already declared) is enough for.
        runCatching { manager.registerNetworkCallback(request, cb) }
            .onFailure { return settle("cannot watch Wi-Fi: ${it.message}") }
        // Its own timeout, since registerNetworkCallback has no timeout
        // parameter and a connect button that never answers is worse than one
        // that fails.
        main.postDelayed({ settle("phone is not on the camera's Wi-Fi") },
                         WIFI_BIND_TIMEOUT_MS.toLong())
    }

    private fun releaseNetwork() {
        val manager = cm() ?: return
        netCallback?.let {
            runCatching { manager.unregisterNetworkCallback(it) }
            netCallback = null
        }
        // Hand the process back to the normal default network, or every later
        // call to ERPNext goes out over an access point with no internet.
        runCatching { manager.bindProcessToNetwork(null) }
    }

    private const val WIFI_BIND_TIMEOUT_MS = 15_000

    // Signatures verified against the shipped V1.10.1 AARs (javap in CI,
    // 2026-08-19): status carries the connect type, and every other method
    // has a default.
    private val cameraCallback = object : ICameraChangedCallback {
        override fun onCameraStatusChanged(enabled: Boolean, connectType: Int) {
            // A DROPPED CAMERA MUST HAND THE NETWORK BACK. While this process
            // is bound to the X3's access point every ERPNext call leaves over
            // a link with no internet, so a camera that disconnects without
            // anyone pressing the toggle would silently break sync — a worse
            // fault than the one this binding fixes.
            if (!enabled) releaseNetwork()
            statusListener?.invoke(enabled, null)
        }

        override fun onCameraConnectError(errorCode: Int) {
            // The bare number told Amit nothing on 2026-09-04 and told me
            // nothing either: the SDK ships no public code table, so the only
            // honest thing the app can do is say what the code was AND what
            // the three things that actually cause it are.
            statusListener?.invoke(false,
                "camera connect error $errorCode — check the X3 is on, its " +
                "Wi-Fi is joined in Android Settings, and no other app " +
                "(Insta360 app) is holding the camera")
        }
    }

    override fun init(app: Any) {
        val application = app as Application
        appRef = application
        InstaCameraSDK.init(application)
        InstaMediaSDK.init(application)
        InstaCameraManager.getInstance().registerCameraChangedCallback(cameraCallback)
    }

    override val connected: Boolean
        get() = InstaCameraManager.getInstance().cameraConnectedType !=
            InstaCameraManager.CONNECT_TYPE_NONE

    override fun connect(onChange: (Boolean, String?) -> Unit) {
        statusListener = onChange
        // Bind FIRST, open second. openCamera reaches the camera over a socket,
        // so binding after it would race the very connection it is meant to fix.
        bindToCameraWifi { problem ->
            if (problem != null) {
                onChange(false, "$problem — join the X3's Wi-Fi in Settings, then try again")
                return@bindToCameraWifi
            }
            InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_WIFI)
        }
    }

    override fun disconnect() {
        statusListener = null
        InstaCameraManager.getInstance().closeCamera()
        releaseNetwork()
    }

    override fun shootAndExport(targetPath: String, onDone: (Result<String>) -> Unit) {
        val mgr = InstaCameraManager.getInstance()
        // V1.10.1: onCaptureFinish and onCaptureError are the two abstract
        // members; the lifecycle chatter all has defaults.
        mgr.setCaptureStatusListener(object : ICaptureStatusListener {
            override fun onCaptureFinish(filePaths: Array<String>?) {
                mgr.setCaptureStatusListener(null)
                if (filePaths.isNullOrEmpty()) {
                    onDone(Result.failure(IllegalStateException(
                        "camera reported capture finished but returned no file")))
                    return
                }
                export(filePaths, targetPath, onDone)
            }

            override fun onCaptureError(errorCode: Int) {
                mgr.setCaptureStatusListener(null)
                onDone(Result.failure(RuntimeException("capture error $errorCode")))
            }
        })
        mgr.startNormalCapture()
    }

    private fun export(
        filePaths: Array<String>,
        targetPath: String,
        onDone: (Result<String>) -> Unit,
    ) {
        // WorkWrapper + export are documented as heavy — keep them off the
        // main thread; the export callback itself may arrive on any thread.
        Thread {
            runCatching {
                val work = WorkWrapper(filePaths)
                val params = ExportImageParamsBuilder()
                    .setExportMode(ExportUtils.ExportMode.PANORAMA)
                    .setTargetPath(targetPath)
                ExportUtils.exportImage(work, params, object : IExportCallback {
                    override fun onStart(exportId: Int) {}
                    override fun onSuccess() = onDone(Result.success(targetPath))
                    override fun onFail(errorCode: Int, message: String?) =
                        onDone(Result.failure(RuntimeException(
                            "stitch export failed ($errorCode): $message")))
                    override fun onCancel() =
                        onDone(Result.failure(RuntimeException("stitch export cancelled")))
                })
            }.onFailure { onDone(Result.failure(it)) }
        }.start()
    }
}
