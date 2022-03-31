package me.ruyeo.employeesinfo.faceDetect.utils

import android.Manifest
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Toast
import me.ruyeo.employeesinfo.faceDetect.ui.CameraFragment

/**
 *Created by farrukh_kh on 6/8/21 5:32 PM
 *kh.farrukh.facerecognition.utils
 **/
object Constants {

    const val KEY_USE_FACING = "use_facing"
    const val CAMERA_REQUEST = Manifest.permission.CAMERA
    const val REQUEST_CODE = 1
    const val API_INPUT_SIZE = 112
    const val API_IS_QUANTIZED = false
    const val API_MODEL_FILE = "mobile_face_net.tflite"
    const val API_LABELS_FILE = "file:///android_asset/labelmap.txt"
    const val MAINTAIN_ASPECT = false
    val DESIRED_PREVIEW_SIZE = Size(640, 360)
    const val TEXT_SIZE_DIP = 10f

    const val FRAGMENT_DIALOG = "dialog"
    const val MINIMUM_PREVIEW_SIZE = 320
    const val KEY_FACING = "camera_facing"

    const val OUTPUT_SIZE = 192
    const val IMAGE_MEAN = 128.0f
    const val IMAGE_STD = 128.0f
    const val NUM_THREADS = 4
}