package me.ruyeo.employeesinfo.faceDetect.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import me.ruyeo.employeesinfo.R
import me.ruyeo.employeesinfo.faceDetect.customviews.AutoFitTextureView
import me.ruyeo.employeesinfo.faceDetect.utils.CompareSizesByArea
import me.ruyeo.employeesinfo.faceDetect.utils.ConnectionCallback
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.FRAGMENT_DIALOG
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.MINIMUM_PREVIEW_SIZE
import me.ruyeo.employeesinfo.faceDetect.utils.ErrorDialog
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 *Created by farrukh_kh on 6/8/21 4:56 PM
 *kh.farrukh.facerecognition.ui
 **/
class CameraFragment private constructor(
    private val cameraConnectionCallback: ConnectionCallback,
    private val imageListener: OnImageAvailableListener,
    private val layout: Int,
    private val inputSize: Size
) : Fragment() {

    companion object {

        fun newInstance(
            callback: ConnectionCallback,
            imageListener: OnImageAvailableListener,
            layout: Int,
            inputSize: Size
        ) = CameraFragment(callback, imageListener, layout, inputSize)

        fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int): Size {
            val minSize = max(min(width, height), MINIMUM_PREVIEW_SIZE)
            val desiredSize = Size(width, height)

            var exactSizeFound = false
            val bigEnough: MutableList<Size?> = ArrayList()
            val tooSmall: MutableList<Size?> = ArrayList()
            for (option in choices) {
                if (option == desiredSize) {
                    exactSizeFound = true
                }
                if (option.height >= minSize && option.width >= minSize) {
                    bigEnough.add(option)
                } else {
                    tooSmall.add(option)
                }
            }
//            Log.e("chooseOptimalSize", "Desired size: $desiredSize, min: $minSize*$minSize")
//            Log.e("chooseOptimalSize", "Valid preview sizes: [${TextUtils.join(", ", bigEnough)}]")
//            Log.e(
//                "chooseOptimalSize",
//                "Rejected preview sizes: [${TextUtils.join(", ", tooSmall)}]"
//            )
            if (exactSizeFound) {
//                Log.e("chooseOptimalSize", "Exact size match found")
                return desiredSize
            }
            return if (bigEnough.size > 0) {
                val chosenSize =
                    Collections.min(bigEnough, CompareSizesByArea())!!
//                Log.e("chooseOptimalSize", "Chosen size: ${chosenSize.width}*${chosenSize.height}")
                chosenSize
            } else {
//                Log.e("chooseOptimalSize", "Couldn't find any suitable preview size")
                choices[0]
            }
        }
    }

    private val cameraOpenCloseLock = Semaphore(1)
    private val captureCallback = object : CaptureCallback() {
        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
        }
    }

    private var cameraId: String? = null

    private var textureView: AutoFitTextureView? = null
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var sensorOrientation: Int? = null
    private var previewSize: Size? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var previewReader: ImageReader? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cd: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = cd
            createCameraPreviewSession()
        }

        override fun onDisconnected(cd: CameraDevice) {
            cameraOpenCloseLock.release()
            cd.close()
            cameraDevice = null
        }

        override fun onError(cd: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cd.close()
            cameraDevice = null
            val activity = activity
            activity?.finish()
        }
    }

    private val surfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textureView = view.findViewById(R.id.texture_view)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView!!.isAvailable) {
            openCamera(textureView!!.width, textureView!!.height)
        } else {
            textureView!!.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    fun setCamera(cameraId: String) {
        this.cameraId = cameraId
    }

    private fun setUpCameraOutputs() {
        val manager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

            previewSize = chooseOptimalSize(
                map!!.getOutputSizes(SurfaceTexture::class.java),
                inputSize.width,
                inputSize.height
            )
            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView!!.setAspectRatio(previewSize!!.width, previewSize!!.height)
            } else {
                textureView!!.setAspectRatio(previewSize!!.height, previewSize!!.width)
            }
        } catch (e: CameraAccessException) {
            ErrorDialog.newInstance("This device doesn't support Camera2 API")
                .show(childFragmentManager, FRAGMENT_DIALOG)
            throw IllegalStateException("This device doesn't support Camera2 API")
        } catch (e: Exception) {
//            Log.e("setUpCameraOptions", "Exception: ${e.message}")
        }
        cameraConnectionCallback.onPreviewSizeChosen(previewSize, sensorOrientation!!)
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {
        setUpCameraOutputs()
        configureTransform(width, height)
        val manager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId!!, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
//            Log.e("openCamera", "Exception: ${e.message}")
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            if (null != captureSession) {
                captureSession!!.close()
                captureSession = null
            }
            if (null != cameraDevice) {
                cameraDevice!!.close()
                cameraDevice = null
            }
            if (null != previewReader) {
                previewReader!!.close()
                previewReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("ImageListener")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread!!.quitSafely()
        try {
            backgroundThread!!.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
//            Log.e("stopBackgroundThread", "Exception: ${e.message}")
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = textureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
            val surface = Surface(texture)
            previewRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder!!.addTarget(surface)
//            Log.e(
//                "createCameraPreview",
//                "Opening camera preview: ${previewSize!!.width}*${previewSize!!.height}"
//            )
            previewReader = ImageReader.newInstance(
                previewSize!!.width, previewSize!!.height, ImageFormat.YUV_420_888, 2
            )
            previewReader!!.setOnImageAvailableListener(imageListener, backgroundHandler)
            previewRequestBuilder!!.addTarget(previewReader!!.surface)

            cameraDevice!!.createCaptureSession(
                listOf(surface, previewReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        if (null == cameraDevice) {
                            return
                        }
                        captureSession = cameraCaptureSession
                        try {
                            previewRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            previewRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                            )
                            previewRequest = previewRequestBuilder!!.build()
                            captureSession!!.setRepeatingRequest(
                                previewRequest!!, captureCallback, backgroundHandler
                            )
                        } catch (e: CameraAccessException) {
//                            Log.e("onConfigured", "Exception: ${e.message}")
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
//            Log.e("onConfigured", "Exception: ${e.message}")
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity = activity
        if (null == previewSize || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(
            0f, 0f, previewSize!!.height.toFloat(),
            previewSize!!.width.toFloat()
        )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / previewSize!!.height,
                viewWidth.toFloat() / previewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView!!.setTransform(matrix)
    }
}