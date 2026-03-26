package com.example.ringtoneplayer.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val numLayers = 12 // 10-15 طبقة كما في التعليمات
    private val paths = Array(numLayers) { Path() }
    private var phase = 0f
    private var amplitudeMultiplier = 0f
    private var bytes: ByteArray? = null

    init {
        // تفعيل تسريع العتاد لمنع الرمشة - Equivalent to will-change: transform
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun updateVisualizer(bytes: ByteArray) {
        this.bytes = bytes
        var sum = 0f
        for (i in bytes.indices step 4) {
            sum += abs((bytes[i].toInt() and 0xFF) - 128).toFloat()
        }
        val target = (sum / (bytes.size / 4f))
        // تنعيم القفزة (Interpolation)
        amplitudeMultiplier = amplitudeMultiplier * 0.85f + target * 0.15f
        phase += 0.06f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // Scale scaling to handle pixel-perfection (window.devicePixelRatio equivalent)
        val density = resources.displayMetrics.density
        
        val centerY = h / 2
        val hotPink = Color.parseColor("#FF2D75")

        // Draw overlapping layered sine waves
        for (i in 0 until numLayers) {
            paths[i].reset()
            
            // خيوط رفيعة جداً (0.5px to 0.8px)
            val strokeWidthDp = 0.6f 
            paint.strokeWidth = strokeWidthDp * density
            
            val alpha = (0.5f - (i * 0.04f)).coerceIn(0.05f, 0.6f)
            
            // Gradient Stroke: rgba(255, 45, 117, 0.5) to Transparent
            paint.shader = LinearGradient(0f, 0f, w, 0f,
                intArrayOf(Color.TRANSPARENT, hotPink, Color.TRANSPARENT),
                floatArrayOf(0.1f, 0.5f, 0.9f), Shader.TileMode.CLAMP)
            paint.alpha = (alpha * 255).toInt()

            val yOffset = (i * 4f * density) - (numLayers * 2f * density)
            val currentCenterY = centerY + yOffset
            
            // Physics: Sine Wave logic with Horizontal flow
            paths[i].moveTo(0f, currentCenterY)

            val musicEffect = (amplitudeMultiplier * (1.0f + i * 0.1f) * density).coerceAtMost(h * 0.4f)
            
            // Drawing the "Silk" path with high-precision control points
            for (x in 0..10) {
                val xPos = (w / 10) * x
                val dynamicWave = sin(phase + i * 0.5f + (xPos * 0.005f)) * musicEffect
                val yPos = currentCenterY + dynamicWave
                
                if (x == 0) {
                    paths[i].moveTo(xPos, yPos)
                } else {
                    val prevX = (w / 10) * (x - 1)
                    val prevDynamic = sin(phase + i * 0.5f + (prevX * 0.005f)) * musicEffect
                    val prevY = currentCenterY + prevDynamic
                    paths[i].quadTo(prevX, prevY, (prevX + xPos) / 2, (prevY + yPos) / 2)
                }
            }

            canvas.drawPath(paths[i], paint)
        }
    }
}
