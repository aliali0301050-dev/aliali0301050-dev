package com.example.ringtoneplayer.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

/**
 * FINAL MASTER VERSION: Production Ready SilkWaveView.
 * Optimized to avoid allocations in onDraw.
 */
class SilkWaveView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var phase = 0f
    private var startColor = Color.parseColor("#FF007F")
    private var endColor = Color.parseColor("#FFD700")
    private var amplitudeMultiplier = 0f
    private val wavePaths = Array(7) { Path() }
    private var gradient: LinearGradient? = null

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setThemeColors(start: Int, end: Int) {
        if (this.startColor != start || this.endColor != end) {
            this.startColor = start
            this.endColor = end
            gradient = null // Force recreate on next draw
            invalidate()
        }
    }

    fun setThemeColor(color: Int) {
        setThemeColors(color, color)
    }

    fun updateVisualizer(bytes: ByteArray) {
        var sum = 0f
        for (i in bytes.indices step 8) {
            sum += abs((bytes[i].toInt() and 0xFF) - 128).toFloat()
        }
        val target = (sum / (bytes.size / 8f))
        amplitudeMultiplier = amplitudeMultiplier * 0.8f + target * 0.2f
        phase += 0.08f 
        postInvalidateOnAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradient = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val midY = h / 2

        if (gradient == null) {
            gradient = LinearGradient(0f, 0f, w, 0f, startColor, endColor, Shader.TileMode.CLAMP)
        }
        paint.shader = gradient

        for (i in 0 until 7) {
            val path = wavePaths[i]
            path.reset()

            paint.alpha = (0.3f * 255 + i * 15).toInt().coerceAtMost(255)
            paint.strokeWidth = 1.5f + (i * 0.5f)

            path.moveTo(0f, midY)
            val musicEffect = amplitudeMultiplier * (1.5f + i * 0.2f)

            for (x in 0 until w.toInt() step 6) {
                val scaling = 1f - abs(x - w / 2) / (w / 2)
                val y = sin(x * 0.012 + phase + (i * 0.7)) * (40 + i * 10 + musicEffect) * scaling
                path.lineTo(x.toFloat(), midY + y.toFloat())
            }
            canvas.drawPath(path, paint)
        }
    }
}
