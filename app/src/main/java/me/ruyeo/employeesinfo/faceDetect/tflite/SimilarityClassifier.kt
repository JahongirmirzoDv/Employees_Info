package me.ruyeo.employeesinfo.faceDetect.tflite

import android.graphics.Bitmap
import me.ruyeo.employeesinfo.data.model.Recognition

/**
 *Created by farrukh_kh on 6/8/21 1:01 PM
 *kh.farrukh.facerecognition.tflite
 **/
interface SimilarityClassifier {
    fun register(recognition: Recognition)
    fun recognizeImage(bitmap: Bitmap, getExtra: Boolean): List<Recognition?>
}