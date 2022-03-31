package me.ruyeo.employeesinfo.faceDetect.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max

/**
 *Created by farrukh_kh on 6/8/21 12:27 PM
 *kh.farrukh.facerecognition.utils
 **/
object ImageUtils {

    private const val kMaxChannelValue = 262143

    fun getYUVByteSize(width: Int, height: Int): Int {
        val ySize = width * height

        val uvSize = (width + 1) / 2 * ((height + 1) / 2) * 2
        return ySize + uvSize
    }

    //tekshirib ko`rish uchun. Istalgan joyda bitmap ni saqlab ko`rish mumkin
    fun saveBitmap(bitmap: Bitmap, filename: String) {
        val root =
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "tensorflow"
        Log.e("saveBitmap", "width:${bitmap.width}, height:${bitmap.height}, to:$root")
        val myDir = File(root)
        if (!myDir.mkdirs()) {
            Log.e("saveBitmap", "Make dir failed")
        }
        val file = File(myDir, filename)
        if (file.exists()) {
            file.delete()
        }
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            Log.e("saveBitmap", "Exception: ${e.message}")
        }
    }

    fun convertYUV420SPToARGB8888(input: ByteArray, width: Int, height: Int, output: IntArray) {
        val frameSize = width * height
        var j = 0
        var yp = 0
        while (j < height) {
            var uvp = frameSize + (j shr 1) * width
            var u = 0
            var v = 0
            var i = 0
            while (i < width) {
                val y = 0xff and input[yp].toInt()
                if (i and 1 == 0) {
                    v = 0xff and input[uvp++].toInt()
                    u = 0xff and input[uvp++].toInt()
                }
                output[yp] = YUV2RGB(y, u, v)
                i++
                yp++
            }
            j++
        }
    }

    private fun YUV2RGB(yP: Int, uP: Int, vP: Int): Int {
        var y = yP
        var u = uP
        var v = vP
        y = if (y - 16 < 0) 0 else y - 16
        u -= 128
        v -= 128

        val y1192 = 1192 * y
        var r = y1192 + 1634 * v
        var g = y1192 - 833 * v - 400 * u
        var b = y1192 + 2066 * u

        r = if (r > kMaxChannelValue) kMaxChannelValue else if (r < 0) 0 else r
        g = if (g > kMaxChannelValue) kMaxChannelValue else if (g < 0) 0 else g
        b = if (b > kMaxChannelValue) kMaxChannelValue else if (b < 0) 0 else b
        return -0x1000000 or (r shl 6 and 0xff0000) or (g shr 2 and 0xff00) or (b shr 10 and 0xff)
    }

    fun convertYUV420ToARGB8888(
        yData: ByteArray,
        uData: ByteArray,
        vData: ByteArray,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        out: IntArray
    ) {
        var yp = 0
        for (j in 0 until height) {
            val pY = yRowStride * j
            val pUV = uvRowStride * (j shr 1)
            for (i in 0 until width) {
                val uvOffset = pUV + (i shr 1) * uvPixelStride
                out[yp++] = YUV2RGB(
                    0xff and yData[pY + i].toInt(), 0xff and uData[uvOffset]
                        .toInt(), 0xff and vData[uvOffset].toInt()
                )
            }
        }
    }

    fun getTransformationMatrix(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        applyRotation: Int,
        maintainAspectRatio: Boolean
    ): Matrix {
        val matrix = Matrix()
        if (applyRotation != 0) {

            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)
            matrix.postRotate(applyRotation.toFloat())
        }

        val transpose = (abs(applyRotation) + 90) % 180 == 0
        val inWidth = if (transpose) srcHeight else srcWidth
        val inHeight = if (transpose) srcWidth else srcHeight

        if (inWidth != dstWidth || inHeight != dstHeight) {
            val scaleFactorX = dstWidth / inWidth.toFloat()
            val scaleFactorY = dstHeight / inHeight.toFloat()
            if (maintainAspectRatio) {
                val scaleFactor = max(scaleFactorX, scaleFactorY)
                matrix.postScale(scaleFactor, scaleFactor)
            } else {
                matrix.postScale(scaleFactorX, scaleFactorY)
            }
        }
        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }
        return matrix
    }
}