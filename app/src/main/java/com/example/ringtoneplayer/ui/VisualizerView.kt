package com.example.ringtoneplayer.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class VisualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class VisualizerMode { SILK, BARS, CIRCULAR_BARS }
    var mode: VisualizerMode = VisualizerMode.SILK

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    private var phase = 0f
    private var amplitude = 0f
    private var rawData: ByteArray? = null
    private val path = Path()
    private var startColor = Color.parseColor("#00E5FF")
    private var endColor = Color.parseColor("#FF2D75")
    private var gradient: Shader? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun updateVisualizer(data: ByteArray) {
        rawData = data
        var sum = 0f
        for (i in 0 until data.size step 4) {
            sum += abs((data[i].toInt() and 0xFF) - 128).toFloat()
        }
        val targetAmplitude = (sum / (data.size / 4f)) * 2.5f // زيادة الحساسية للبروز
        amplitude = amplitude * 0.8f + targetAmplitude * 0.2f
        phase += 0.15f // زيادة سرعة الموجة قليلاً
        postInvalidateOnAnimation()
    }

    fun setThemeColors(start: Int, end: Int) {
        this.startColor = start
        this.endColor = end
        gradient = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val midY = h / 2

        if (mode == VisualizerMode.SILK) {
            if (gradient == null) gradient = LinearGradient(0f, 0f, w, 0f, startColor, endColor, Shader.TileMode.CLAMP)
            paint.shader = gradient
            drawSilk(canvas, w, h, midY)
        }
    }

    private fun drawSilk(canvas: Canvas, w: Float, h: Float, midY: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f // خيوط أسمك لتكون بارزة
        paint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL) // توهج ناعم
        
        // رسم 6 خيوط بدلاً من 4 لزيادة الكثافة والجمال
        for (i in 0 until 6) {
            path.reset()
            paint.alpha = (60 + i * 30).coerceAtMost(255)
            val freqMult = 0.5f + i * 0.25f
            val shift = phase + (i * 1.2f)
            val ampMult = 0.4f + i * 0.4f

            path.moveTo(0f, midY)
            for (x in 0..w.toInt() step 5) {
                val scaling = sin(x.toFloat() / w * PI).toFloat()
                val y = midY + sin(x * 0.012f * freqMult + shift).toFloat() * amplitude * ampMult * scaling
                path.lineTo(x.toFloat(), y)
            }
            canvas.drawPath(path, paint)
        }
    }
}
