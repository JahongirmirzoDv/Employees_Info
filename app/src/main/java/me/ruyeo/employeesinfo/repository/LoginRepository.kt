package me.ruyeo.employeesinfo.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.HandlerThread
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import me.ruyeo.employeesinfo.data.api.ApiHelper
import me.ruyeo.employeesinfo.data.local.AppDatabase
import me.ruyeo.employeesinfo.data.model.Staff
import me.ruyeo.employeesinfo.faceDetect.tflite.TFLiteFaceRecognitionModel
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.API_INPUT_SIZE
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.API_IS_QUANTIZED
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.API_LABELS_FILE
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.API_MODEL_FILE
import java.util.*


/**
Muvaffaqiyatli login qilinganda, backenddan barcha xodimlar ro`yxati olinib,
yuz malumotlari saqlab qo`yiladi
 */
class LoginRepository(
    private val apiHelper: ApiHelper,
    private val appDatabase: AppDatabase,
    private val context: Context
) {

    private val tfLiteFaceRecognitionModel = TFLiteFaceRecognitionModel.create(
        context.assets,
        API_MODEL_FILE,
        API_LABELS_FILE,
        API_INPUT_SIZE,
        API_IS_QUANTIZED,
        context
    )

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    val faceDetector = FaceDetection.getClient(options)

    suspend fun login(username: String, password: String) = apiHelper.login(username, password)

    /**
    Backenddan barcha xodimlar ro`yxatini olish
     */
    suspend fun getAllStaff() = apiHelper.getAllStaff()

    suspend fun clearAll(){
        appDatabase.getStaffDao().clearAll()
        appDatabase.getFacesDao().clearAll()
    }

    /**
    Xodimlarni local database ga saqlash
     */
    fun saveAllStaff(staffList: List<Staff>) {
        appDatabase.getStaffDao().insertAll(staffList)
        saveFacesOfStaff(staffList)
    }

    /**
    Xodimlarni rasmini olib, yuz malumotlarini aniqlab, local database ga saqlab qo`yish
     */
    private fun saveFacesOfStaff(staffList: List<Staff>) {
        staffList.forEach { currentStaff ->
            Log.e("staff", "id:${currentStaff.id} + ${currentStaff.firstName}")
            Glide.with(context)
                .asBitmap()
                .load(currentStaff.image)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val image = InputImage.fromBitmap(resource, 0)
                        faceDetector
                            .process(image)
                            .addOnSuccessListener(OnSuccessListener { faces ->
                                if (faces.isEmpty()) {
                                    Log.e("no face", "${currentStaff.firstName} da face yo`q!")
                                    return@OnSuccessListener
                                }
                                val thread = HandlerThread("FaceRecognize")
                                thread.start()
                                thread.run {
                                    val faceBmp = Bitmap.createBitmap(
                                        API_INPUT_SIZE,
                                        API_INPUT_SIZE,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val cvFace = Canvas(faceBmp)
                                    val matrix = Matrix()
                                    matrix.postTranslate(
                                        (-faces[0].boundingBox.left).toFloat(),
                                        (-faces[0].boundingBox.top).toFloat()
                                    )
                                    val sx = API_INPUT_SIZE.toFloat() / faces[0].boundingBox.width()
                                    val sy =
                                        API_INPUT_SIZE.toFloat() / faces[0].boundingBox.height()
                                    matrix.postScale(sx, sy)
                                    cvFace.drawBitmap(resource, matrix, null)
                                    val results = tfLiteFaceRecognitionModel.recognizeImage(
                                        faceBmp,
                                        true
                                    )
                                    appDatabase.getFacesDao().insertFace(
                                        results[0]!!.toRegisteredFace().copy(
                                            id = currentStaff.id,
                                            name = currentStaff.firstName
                                        )
                                    )
                                    if (results.isEmpty()) {
                                        Log.e("ERROR", "results is empty")
                                    }
                                }
                            })
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
    }
}