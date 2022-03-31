package me.ruyeo.employeesinfo.faceDetect.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView

/**
 *Created by farrukh_kh on 6/8/21 12:44 PM
 *kh.farrukh.facerecognition.customviews
 **/
class AutoFitTextureView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    TextureView(context, attrs, defStyle) {

    private var ratioWidth = 0
    private var ratioHeight = 0

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    fun setAspectRatio(width: Int, height: Int) {
        require(!(width < 0 || height < 0)) { "Size cannot be negative." }
        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        when {
            0 == ratioWidth || 0 == ratioHeight -> setMeasuredDimension(width, height)
            width < height * ratioWidth / ratioHeight -> setMeasuredDimension(
                width,
                width * ratioHeight / ratioWidth
            )
            else -> setMeasuredDimension(height * ratioWidth / ratioHeight, height)
        }
    }
}