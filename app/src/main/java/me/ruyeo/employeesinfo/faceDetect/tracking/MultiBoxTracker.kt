package me.ruyeo.employeesinfo.faceDetect.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Paint.Cap
import android.graphics.Paint.Join
import android.text.TextUtils
import android.util.Pair
import android.util.TypedValue
import me.ruyeo.employeesinfo.data.model.Recognition
import me.ruyeo.employeesinfo.faceDetect.utils.BorderedText
import me.ruyeo.employeesinfo.faceDetect.utils.ImageUtils
import java.util.*
import kotlin.math.min

/**
 *Created by farrukh_kh on 6/8/21 1:11 PM
 *kh.farrukh.facerecognition.tracking
 **/
class MultiBoxTracker(context: Context) {
    private val screenRects: MutableList<Pair<Float, RectF>> = LinkedList()
    private val availableColors: Queue<Int> = LinkedList()
    private val trackedObjects: MutableList<TrackedRecognition> = LinkedList()
    private val boxPaint = Paint()
    private val textSizePx: Float
    private val borderedText: BorderedText
    private var frameToCanvasMatrix: Matrix? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var sensorOrientation = 0

    companion object {
        private const val TEXT_SIZE_DIP = 18f
        private const val MIN_SIZE = 16.0f
        private val COLORS = intArrayOf(
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE,
            Color.parseColor("#55FF55"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"),
            Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"),
            Color.parseColor("#0D0068")
        )
    }

    init {
        for (color in COLORS) {
            availableColors.add(color)
        }
        boxPaint.color = Color.RED
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 10.0f
        boxPaint.strokeCap = Cap.ROUND
        boxPaint.strokeJoin = Join.ROUND
        boxPaint.strokeMiter = 100f
        textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.resources.displayMetrics
        )
        borderedText = BorderedText(textSizePx)
    }

    @Synchronized
    fun setFrameConfiguration(
        width: Int, height: Int, sensorOrientation: Int
    ) {
        frameWidth = width
        frameHeight = height
        this.sensorOrientation = sensorOrientation
    }

    @Synchronized
    fun drawDebug(canvas: Canvas) {
        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 60.0f
        val boxPaint = Paint()
        boxPaint.color = Color.RED
        boxPaint.alpha = 200
        boxPaint.style = Paint.Style.STROKE
        for (detection in screenRects) {
            val rect = detection.second
            canvas.drawRect(rect, boxPaint)
            canvas.drawText("" + detection.first, rect.left, rect.top, textPaint)
            borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first)
        }
    }

    @Synchronized
    fun trackResults(results: List<Recognition>, timestamp: Long) {
//        Log.e("trackResults", "Processing ${results.size} results from $timestamp")
        processResults(results)
    }

    @Synchronized
    fun draw(canvas: Canvas) {
        val rotated = sensorOrientation % 180 == 90
        val multiplier = min(
            canvas.height / (if (rotated) frameWidth else frameHeight).toFloat(),
            canvas.width / (if (rotated) frameHeight else frameWidth).toFloat()
        )
        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            (multiplier * if (rotated) frameHeight else frameWidth).toInt(),
            (multiplier * if (rotated) frameWidth else frameHeight).toInt(),
            sensorOrientation,
            false
        )
        for (recognition in trackedObjects) {
            val trackedPos = RectF(recognition.location)
            frameToCanvasMatrix!!.mapRect(trackedPos)
            boxPaint.color = recognition.color
            val cornerSize = min(trackedPos.width(), trackedPos.height()) / 8.0f
            canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint)
            @SuppressLint("DefaultLocale") val strConfidence =
                if (recognition.detectionConfidence < 0) "" else String.format(
                    "%.2f",
                    recognition.detectionConfidence
                ) + ""
            val labelString = if (!TextUtils.isEmpty(recognition.title)) String.format(
                "%s %s",
                recognition.title,
                strConfidence
            ) else strConfidence
            borderedText.drawText(
                canvas, trackedPos.left + cornerSize, trackedPos.top, labelString, boxPaint
            )
        }
    }

    private fun processResults(results: List<Recognition>) {
        val rectsToTrack: MutableList<Pair<Float, Recognition>> =
            LinkedList<Pair<Float, Recognition>>()
        screenRects.clear()
        val rgbFrameToScreen = Matrix(frameToCanvasMatrix)
        for (result in results) {
            if (result.location == null) {
                continue
            }
            val detectionFrameRect = RectF(result.location)
            val detectionScreenRect = RectF()
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect)
//            Log.e(
//                "processResults",
//                "Result! Frame: ${result.location} mapped to screen: $detectionScreenRect"
//            )
            screenRects.add(Pair(result.distance, detectionScreenRect))
            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
//                Log.e("processResults", "Degenerate rectangle! $detectionFrameRect")
                continue
            }
            rectsToTrack.add(Pair<Float, Recognition>(result.distance, result))
        }
        trackedObjects.clear()
        if (rectsToTrack.isEmpty()) {
//            Log.e("processResults", "Nothing to track, aborting")
            return
        }
        for (potential in rectsToTrack) {
            val trackedRecognition = TrackedRecognition()
            trackedRecognition.detectionConfidence = potential.first
            trackedRecognition.location = RectF(potential.second.location)
            trackedRecognition.title = potential.second.title
            potential.second.color.let {
                if (it != null) {
                    trackedRecognition.color = it
                } else {
                    trackedRecognition.color = COLORS[trackedObjects.size]
                }
            }
            trackedObjects.add(trackedRecognition)
            if (trackedObjects.size >= COLORS.size) {
                break
            }
        }
    }

    private class TrackedRecognition {
        var location: RectF? = null
        var detectionConfidence = 0f
        var color = 0
        var title: String? = null
    }
}