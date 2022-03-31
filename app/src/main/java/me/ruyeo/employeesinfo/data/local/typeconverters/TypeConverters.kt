package me.ruyeo.employeesinfo.faceDetect.database

import androidx.room.TypeConverter

/**
 *Created by farrukh_kh on 6/9/21 3:46 PM
 *kh.farrukh.facerecognition.database
 **/
object CustomTypeConverters {

    @TypeConverter
    fun extraToString(extra: Any?): String? {
        var string: String? = null
        extra?.let {
            val extraAsArray = extra as Array<FloatArray>
            string = extraAsArray.contentDeepToString()
        }
        return string
    }

    @TypeConverter
    fun stringToExtra(string: String?): Any? {
        var extraAsArray: Array<FloatArray>? = null
        string?.let {
            val listOfFloat = string.substring(2, string.length - 2).split(",")
            extraAsArray = Array(size = 1) {
                val floatArray = FloatArray(size = listOfFloat.size) { innerIt ->
                    listOfFloat[innerIt].toFloat()
                }
                floatArray
            }
        }
        return extraAsArray
    }
}