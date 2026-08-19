package com.malletcrafts.sitephotos.camera

import android.app.Application
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

    private val cameraCallback = object : ICameraChangedCallback {
        override fun onCameraStatusChanged(enabled: Boolean) {
            statusListener?.invoke(enabled, null)
        }

        override fun onCameraConnectError(errorCode: Int) {
            statusListener?.invoke(false, "camera connect error $errorCode")
        }
    }

    override fun init(app: Any) {
        val application = app as Application
        InstaCameraSDK.init(application)
        InstaMediaSDK.init(application)
        InstaCameraManager.getInstance().registerCameraChangedCallback(cameraCallback)
    }

    override val connected: Boolean
        get() = InstaCameraManager.getInstance().cameraConnectedType !=
            InstaCameraManager.CONNECT_TYPE_NONE

    override fun connect(onChange: (Boolean, String?) -> Unit) {
        statusListener = onChange
        InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_WIFI)
    }

    override fun disconnect() {
        statusListener = null
        InstaCameraManager.getInstance().closeCamera()
    }

    override fun shootAndExport(targetPath: String, onDone: (Result<String>) -> Unit) {
        val mgr = InstaCameraManager.getInstance()
        mgr.setCaptureStatusListener(object : ICaptureStatusListener {
            override fun onCaptureStarting() {}
            override fun onCaptureWorking() {}
            override fun onCaptureStopping() {}
            override fun onFileSaving() {}
            override fun onCaptureTimeChanged(captureTime: Long) {}
            override fun onCaptureCountChanged(captureCount: Int) {}

            override fun onCaptureFinish(filePaths: Array<String>?) {
                mgr.setCaptureStatusListener(null)
                if (filePaths.isNullOrEmpty()) {
                    onDone(Result.failure(IllegalStateException(
                        "camera reported capture finished but returned no file")))
                    return
                }
                export(filePaths, targetPath, onDone)
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
                    override fun onSuccess() = onDone(Result.success(targetPath))
                    override fun onFail() =
                        onDone(Result.failure(RuntimeException("stitch export failed")))
                    override fun onCancel() =
                        onDone(Result.failure(RuntimeException("stitch export cancelled")))
                    override fun onProgress(progress: Float) {}
                })
            }.onFailure { onDone(Result.failure(it)) }
        }.start()
    }
}
