package com.mapbox.vision.examples

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.DimenRes
import android.util.AttributeSet
import android.view.View
import com.mapbox.vision.mobile.core.models.frame.ImageSize
import com.mapbox.vision.safety.core.models.CollisionObject

class SafetyModeView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val DISTANCE_BASE_RANGE_METERS = 40f
    }

    private enum class Mode {
        NONE,
        WARNING,
        CRITICAL
    }

    private data class WarningShape(
        val center: PointF,
        val radius: Float
    )

    private var mode = Mode.NONE

    private var scaleFactor = 1f
    private var scaledSize = ImageSize(1, 1)

    private val distancePath = Path()
    private var warningShapes: List<WarningShape> = emptyList()

    private val distancePaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val collisionPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val criticalDrawable: Drawable = context.getDrawable(R.drawable.ic_collision_critical)!!
    private val alertDrawable: Drawable = context.getDrawable(R.drawable.ic_collision_alert)!!

    private val alertWidth: Int
    private val alertHeight: Int
    private val criticalAlertWidth: Int
    private val criticalAlertHeight: Int
    private val criticalAlertMargin: Int
    private val criticalMarginHorizontal: Int
    private val criticalMarginVertical: Int

    private val distanceRectHeight: Float
    private val distanceRectBaseWidth: Float
    private val distanceRectBaseWidthMin: Float
    private val distanceRectBaseWidthMax: Float

    init {
        alertWidth = pixels(R.dimen.alert_width) / 2
        alertHeight = pixels(R.dimen.alert_height) / 2
        criticalAlertWidth = pixels(R.dimen.critical_alert_width)
        criticalAlertHeight = pixels(R.dimen.critical_alert_height)
        criticalAlertMargin = pixels(R.dimen.critical_alert_margin)
        criticalMarginHorizontal = pixels(R.dimen.critical_margin_horizontal)
        criticalMarginVertical = pixels(R.dimen.critical_margin_vertical)

        distanceRectHeight = pixels(R.dimen.distance_rect_height).toFloat()
        distanceRectBaseWidth = pixels(R.dimen.distance_rect_base).toFloat()
        distanceRectBaseWidthMin = pixels(R.dimen.distance_rect_base_width_min).toFloat()
        distanceRectBaseWidthMax = pixels(R.dimen.distance_rect_base_width_max).toFloat()
    }

    private fun View.pixels(@DimenRes res: Int) = context.resources.getDimensionPixelSize(res)

    private val transparent = resources.getColor(android.R.color.transparent, null)
    private val dark = resources.getColor(R.color.black_70_transparent, null)

    private fun getDistanceShader(top: Float, bottom: Float) = LinearGradient(
        0f, top, 0f, bottom,
        resources.getColor(R.color.minty_green, null),
        transparent,
        Shader.TileMode.REPEAT
    )

    private fun getWarningShader(centerX: Float, centerY: Float, radius: Float) = RadialGradient(
        centerX, centerY, radius,
        resources.getColor(R.color.neon_red, null),
        transparent,
        Shader.TileMode.REPEAT
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // FIXME
//        val frameSize = VisionManager.getFrameSize()
        val frameSize = ImageSize(1280, 720)
        scaleFactor = Math.max(
            width.toFloat() / frameSize.imageWidth,
            height.toFloat() / frameSize.imageHeight
        )
        scaledSize = ImageSize(
            imageWidth = (frameSize.imageWidth * scaleFactor).toInt(),
            imageHeight = (frameSize.imageHeight * scaleFactor).toInt()
        )
    }

    private fun Float.scaleX(): Float = this * scaleFactor - (scaledSize.imageWidth - width) / 2

    private fun Float.scaleY(): Float = this * scaleFactor - (scaledSize.imageHeight - height) / 2

    fun drawWarnings(collisions: Array<CollisionObject>) {
        distancePath.reset()
        mode = Mode.WARNING
        setBackgroundColor(transparent)

        warningShapes = collisions.map { collision ->
            WarningShape(
                center = PointF(
                    ((collision.lastDetection.boundingBox.left + collision.lastDetection.boundingBox.right) / 2).scaleX(),
                    ((collision.lastDetection.boundingBox.bottom + collision.lastDetection.boundingBox.top) / 2).scaleY()
                ),
                radius = (collision.lastDetection.boundingBox.right - collision.lastDetection.boundingBox.left) * scaleFactor
            )
        }

        invalidate()
    }

    fun drawCritical() {
        distancePath.reset()
        setBackgroundColor(dark)
        mode = Mode.CRITICAL

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        when (mode) {
            Mode.NONE -> Unit
            Mode.WARNING -> {
                for (warning in warningShapes) {
                    collisionPaint.shader = getWarningShader(
                        centerX = warning.center.x,
                        centerY = warning.center.y,
                        radius = warning.radius
                    )
                    canvas.drawCircle(
                        warning.center.x, warning.center.y, warning.radius, collisionPaint
                    )
                    alertDrawable.bounds.set(
                        warning.center.x.toInt() - alertWidth,
                        warning.center.y.toInt() - alertHeight,
                        warning.center.x.toInt() + alertWidth,
                        warning.center.y.toInt() + alertHeight
                    )
                    alertDrawable.draw(canvas)
                }

                criticalDrawable.bounds.set(
                    (width - criticalAlertWidth) / 2,
                    criticalAlertMargin,
                    (width + criticalAlertWidth) / 2,
                    criticalAlertMargin + criticalAlertHeight
                )
                criticalDrawable.draw(canvas)
            }
            Mode.CRITICAL -> {
                criticalDrawable.bounds.set(
                    criticalMarginHorizontal,
                    criticalMarginVertical,
                    width - criticalMarginHorizontal,
                    height - criticalMarginVertical
                )
                criticalDrawable.draw(canvas)
            }
        }

        super.onDraw(canvas)
    }

    fun clean() {
        mode = Mode.NONE
        setBackgroundColor(transparent)
        invalidate()
    }
}
