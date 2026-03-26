package com.example.ringtoneplayer.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class CircularSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private var actualRadius = 0f

    var progress = 0
        set(value) {
            if (!isUserTouching) {
                field = value
                invalidate()
            }
        }
        
    var max = 100
        set(value) {
            field = if (value <= 0) 100 else value
            invalidate()
        }

    private var isUserTouching = false
    private var startColor = Color.WHITE 
    private var endColor = Color.WHITE
    private var trackColor = Color.TRANSPARENT 

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f * density // سُمك الخط ليلتحم بمنتصف النقطة
        strokeCap = Paint.Cap.ROUND
    }

    interface OnCircularSeekBarChangeListener {
        fun onProgressChanged(seekBar: CircularSeekBar?, progress: Int, fromUser: Boolean)
        fun onStartTrackingTouch(seekBar: CircularSeekBar?)
        fun onStopTrackingTouch(seekBar: CircularSeekBar?)
    }

    private var listener: OnCircularSeekBarChangeListener? = null

    fun setOnSeekBarChangeListener(l: OnCircularSeekBarChangeListener?) {
        this.listener = l
    }

    fun setProgressColor(color: Int) {
        this.startColor = color
        this.endColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val center = width / 2f
        
        // الضبط المليمتري: 145dp هو المسار الدقيق للنقطة في الـ XML
        actualRadius = 145f * density
        
        // منع القص من الأطراف لضمان بقاء الخط كاملاً داخل الـ 300dp
        val strokeHalf = progressPaint.strokeWidth / 2f
        if (actualRadius > center - strokeHalf) {
            actualRadius = center - strokeHalf
        }

        val sweepAngle = if (max > 0) (progress.toFloat() / max.toFloat()) * 360f else 0f
        val rectF = RectF(center - actualRadius, center - actualRadius, center + actualRadius, center + actualRadius)
        
        progressPaint.color = startColor
        // توهج نيون أبيض ناعم ليختلط مع توهج النقطة
        progressPaint.setShadowLayer(15f, 0f, 0f, Color.parseColor("#4DFFFFFF"))
        
        canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val center = width / 2f
        val dx = event.x - center
        val dy = event.y - center
        val distance = sqrt(dx * dx + dy * dy)
        val touchSlop = 50 * density
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (distance < actualRadius - touchSlop || distance > actualRadius + touchSlop) return false
                isUserTouching = true
                listener?.onStartTrackingTouch(this)
                updateProgressFromTouch(dx, dy)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isUserTouching) {
                    updateProgressFromTouch(dx, dy)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isUserTouching) {
                    isUserTouching = false
                    listener?.onStopTrackingTouch(this)
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateProgressFromTouch(dx: Float, dy: Float) {
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        angle += 90f
        if (angle < 0) angle += 360f
        
        val newProgress = ((angle / 360f) * max).toInt().coerceIn(0, max)
        if (newProgress != progress) {
            progress = newProgress
            invalidate()
            listener?.onProgressChanged(this, progress, true)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
