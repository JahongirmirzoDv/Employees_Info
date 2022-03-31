package me.ruyeo.employeesinfo.faceDetect.customviews

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import java.util.*

/**
 *Created by farrukh_kh on 6/8/21 12:52 PM
 *kh.farrukh.facerecognition.customviews
 **/
class OverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val callbacks = LinkedList<DrawCallback>()

    fun addCallback(callback: DrawCallback) {
        callbacks.add(callback)
    }

    @Synchronized
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        for (callback in callbacks) {
            callback.drawCallback(canvas)
        }
    }

    interface DrawCallback {
        fun drawCallback(canvas: Canvas?)
    }
}