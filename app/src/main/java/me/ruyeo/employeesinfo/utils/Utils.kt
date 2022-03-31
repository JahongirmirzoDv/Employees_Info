package me.ruyeo.employeesinfo.utils

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.widget.Toast


object Utils {

    fun toast(context: Context?, message: String?) {
        context?.let {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun toastLong(context: Context?, message: String?) {
        context?.let {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun toastNoConnection(context: Context?) {
        context?.let {
            Toast.makeText(context, "Internet bilan aloqa yo'q!", Toast.LENGTH_SHORT).show()
//            Snackbar.make(view, "Profilga fotosurat joylash.", Snackbar.LENGTH_LONG)
//                    .setAction("Ok", null).show()
        }
    }

/*    fun pxToDp(px: Int, context: Context?): Int {
        return if (context != null) {
            val resources = context.resources
            val metrics = resources.displayMetrics
            (px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
        } else {
            val metrics = Resources.getSystem().displayMetrics
            (px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
        }
    }*/

  /*  fun dpToPx(dp: Int, context: Context?): Int {
        return if (context != null) {
            val resources = context.resources
            val metrics = resources.displayMetrics
            (dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
        } else {
            val metrics = Resources.getSystem().displayMetrics
            (dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
        }
    }*/



    fun fadeOut(v: View) {
        val fadeOut: Animation = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator() //and this
        fadeOut.duration = 750
        fadeOut.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                v.visibility = View.GONE
            }
        })
        v.startAnimation(fadeOut)
    }

    fun fadeIn(v: View) {
        val fadeOut: Animation = AlphaAnimation(0f, 1f)
        fadeOut.interpolator = AccelerateInterpolator() //and this
        fadeOut.duration = 750
        fadeOut.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                v.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {}
        })
        v.startAnimation(fadeOut)
    }




//    fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
//        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
//        vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
//        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        vectorDrawable.draw(canvas)
//        return BitmapDescriptorFactory.fromBitmap(bitmap)
//    }

    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        var width: Int = drawable.intrinsicWidth
        width = if (width > 0) width else 1
        var height: Int = drawable.intrinsicHeight
        height = if (height > 0) height else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


    fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Int): Bitmap? {
        val output = Bitmap.createBitmap(
            bitmap.width, bitmap
                .height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        val roundPx = pixels.toFloat()
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }


    fun expandNoDuration(v: View) {
        v.measure(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        val targetHeight = v.measuredHeight

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        val a: Animation = object : Animation() {
            override fun applyTransformation(
                interpolatedTime: Float,
                t: Transformation?
            ) {
                v.layoutParams.height =
                    if (interpolatedTime == 1f) WindowManager.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        v.startAnimation(a)
    }

    fun collapseNoDuration(v: View) {
        val initialHeight = v.measuredHeight
        val a: Animation = object : Animation() {
            override fun applyTransformation(
                interpolatedTime: Float,
                t: Transformation?
            ) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        v.startAnimation(a)
    }



    fun closeKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun getPath(context: Context, uri: Uri?): String? {
        var result: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = context.contentResolver.query(uri!!, proj, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val column_index: Int = cursor.getColumnIndexOrThrow(proj[0])
                result = cursor.getString(column_index)
            }
            cursor.close()
        }
        if (result == null) {
            result = "Not found"
        }
        return result
    }
}