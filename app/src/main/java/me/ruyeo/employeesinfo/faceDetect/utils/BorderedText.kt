package me.ruyeo.employeesinfo.faceDetect.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 *Created by farrukh_kh on 6/8/21 12:14 PM
 *kh.farrukh.facerecognition.utils
 **/
class BorderedText(private var textSize: Float) {

    private var interiorPaint = Paint().apply {
        textSize = this@BorderedText.textSize
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = false
        alpha = 255
    }
    private var exteriorPaint = Paint().apply {
        textSize = this@BorderedText.textSize
        color = Color.BLACK
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = textSize / 8
        isAntiAlias = false
        alpha = 255
    }

    fun setTypeface(typeface: Typeface) {
        interiorPaint.typeface = typeface
        exteriorPaint.typeface = typeface
    }

    fun drawText(canvas: Canvas, positionX: Float, positionY: Float, text: String) {
        canvas.drawText(text, positionX, positionY, exteriorPaint)
        canvas.drawText(text, positionX, positionY, interiorPaint)
    }

    fun drawText(canvas: Canvas, posX: Float, posY: Float, text: String, bgPaint: Paint) {
        val width = exteriorPaint.measureText(text)
        val textSize = exteriorPaint.textSize
        val paint = Paint(bgPaint).apply {
            style = Paint.Style.FILL
            alpha = 160
        }
        canvas.drawRect(posX, posY + textSize.toInt(), posX + width.toInt(), posY, paint)
        canvas.drawText(text, posX, posY + textSize, interiorPaint)
    }
}