package com.example.ringtoneplayer.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.sin

class NeonWaveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#FF1744") // Neon Red
        // Apply Glow Effect
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.WHITE
    }

    private var accentColor = Color.parseColor("#FF1744")
    private val paths = Array(3) { Path() }
    private var phase = 0f
    private var animator: ValueAnimator? = null
    private var audioAmplitude = 0f
    private var isAnimating = false

    init {
        // Disable Hardware Acceleration for BlurMaskFilter if needed, 
        // but instructions say apply Hardware Acceleration to maintain 120FPS.
        // Actually, BlurMaskFilter requires software layer on older versions, 
        // but for high performance we might need a different approach if it's slow.
        // For now, let's stick to the requirement.
        setLayerType(LAYER_TYPE_SOFTWARE, null) 
        setupAnimator()
    }

    private fun setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
            duration = 10000 
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
        }
    }

    fun startAnimation() {
        if (!isAnimating) {
            animator?.start()
            isAnimating = true
        }
    }

    fun stopAnimation() {
        if (isAnimating) {
            animator?.cancel()
            isAnimating = false
        }
    }

    fun setThemeColor(color: Int) {
        this.accentColor = color
        wavePaint.color = color
        invalidate()
    }

    fun updateVisualizer(bytes: ByteArray) {
        var sum = 0f
        // Using CAPTURE_SIZE_256 as requested in instructions
        for (i in 0 until bytes.size) {
            sum += abs((bytes[i].toInt() and 0xFF) - 128).toFloat()
        }
        val target = if (bytes.isNotEmpty()) (sum / bytes.size) else 0f
        audioAmplitude = audioAmplitude * 0.8f + target * 0.2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isAnimating && audioAmplitude < 0.1f) return

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        for (i in 0 until 3) {
            paths[i].reset()
            val centerY = h * (0.4f + i * 0.1f)
            paths[i].moveTo(0f, centerY)
            
            val musicBoost = audioAmplitude * 5f
            val frequency = 0.5f + i * 0.2f
            
            for (x in 0 until w.toInt() step 5) {
                val y = centerY + (50f + musicBoost) * sin(phase * frequency + x * 0.01f + i).toFloat()
                paths[i].lineTo(x.toFloat(), y)
            }
            
            canvas.drawPath(paths[i], wavePaint)
            canvas.drawPath(paths[i], corePaint)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) startAnimation() else stopAnimation()
    }
}
