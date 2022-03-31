package me.ruyeo.employeesinfo.faceDetect.ui

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PreviewCallback
import android.os.Bundle
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.ruyeo.employeesinfo.R
import me.ruyeo.employeesinfo.faceDetect.customviews.AutoFitTextureView
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.KEY_FACING
import me.ruyeo.employeesinfo.faceDetect.utils.ImageUtils
import java.io.IOException

/**
 *Created by farrukh_kh on 6/8/21 4:57 PM
 *kh.farrukh.facerecognition.ui
 **/

class LegacyCameraFragment(
    private val imageListener: PreviewCallback,
    private val layout: Int,
    private val desiredSize: Size,
    private var facing: Int
) : Fragment() {

    companion object {
        fun newInstance(
            imageListener: PreviewCallback,
            layout: Int,
            desiredSize: Size,
            facing: Int
        ) = LegacyCameraFragment(imageListener, layout, desiredSize, facing)
    }

    private var camera: Camera? = null
    private var textureView: AutoFitTextureView? = null
    private var backgroundThread: HandlerThread? = null

    private val surfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            texture: SurfaceTexture, width: Int, height: Int
        ) {
            val index = getCameraId()
            try {
                camera = Camera.open(index)
            }catch (e: Exception) {
                Log.e(getString(R.string.app_name), "failed to open Camera");
                e.printStackTrace();
            }
            try {
                val parameters = camera!!.parameters
                val focusModes = parameters.supportedFocusModes
                if (focusModes != null
                    && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                ) {
                    parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                }
                val cameraSizes = parameters.supportedPreviewSizes
                val sizesList = ArrayList<Size>()
                cameraSizes.forEach {
                    sizesList.add(Size(it.width, it.height))
                }
                val previewSize: Size = CameraFragment.chooseOptimalSize(
                    sizesList.toTypedArray(), desiredSize.width, desiredSize.height
                )
                parameters.setPreviewSize(previewSize.width, previewSize.height)
                camera!!.setDisplayOrientation(90)
                camera!!.parameters = parameters
                camera!!.setPreviewTexture(texture)
            } catch (exception: IOException) {
                camera!!.release()
            }
            camera!!.setPreviewCallbackWithBuffer(imageListener)
            val s = camera!!.parameters.previewSize
            camera!!.addCallbackBuffer(ByteArray(ImageUtils.getYUVByteSize(s.height, s.width)))
            textureView!!.setAspectRatio(s.height, s.width)
            camera!!.startPreview()
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        facing = args!!.getInt(KEY_FACING, CameraInfo.CAMERA_FACING_FRONT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textureView = view.findViewById(R.id.texture_view)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView!!.isAvailable) {
            camera?.startPreview()
        } else {
            textureView!!.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        stopCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread!!.start()
    }

    private fun stopBackgroundThread() {
        backgroundThread!!.quitSafely()
        try {
            backgroundThread!!.join()
            backgroundThread = null
        } catch (e: InterruptedException) {
//            Log.e("stopBgThread", "Exception: ${e.message}")
        }
    }

    private fun stopCamera() {
        if (camera != null) {
            camera!!.stopPreview()
            camera!!.setPreviewCallback(null)
            camera!!.release()
            camera = null
        }
    }

    private fun getCameraId(): Int {
        val ci = CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, ci)
            if (ci.facing == facing) return i
        }
        return -1
    }
}