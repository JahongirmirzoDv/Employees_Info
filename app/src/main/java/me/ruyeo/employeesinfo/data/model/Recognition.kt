package me.ruyeo.employeesinfo.data.model

import android.graphics.Bitmap
import android.graphics.RectF

/**
 *Created by farrukh_kh on 6/8/21 1:01 PM
 *kh.farrukh.facerecognition.tflite
 **/

/**
 FaceID uchun, kameraga qaragan yuz uchun data class
 */
data class Recognition(
    val id: Int,
    val title: String,
    val distance: Float,
    var location: RectF?,
    var color: Int? = null,
    var extra: Any? = null,
    var crop: Bitmap? = null
) {
    fun toRegisteredFace() = RegisteredFace(id, title, extra!!)
}