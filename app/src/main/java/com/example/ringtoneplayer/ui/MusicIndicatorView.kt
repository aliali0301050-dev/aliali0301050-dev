package com.example.ringtoneplayer.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class MusicIndicatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF2D55")
        style = Paint.Style.FILL
    }

    private val numBars = 3
    private val barHeights = FloatArray(numBars) { 0.2f }
    private val animators = mutableListOf<ValueAnimator>()
    private var isPlaying = false

    init {
        setupAnimators()
    }

    private fun setupAnimators() {
        animators.clear()
        for (i in 0 until numBars) {
            val animator = ValueAnimator.ofFloat(0.2f, 0.8f).apply {
                duration = 300L + Random.nextLong(200L)
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    barHeights[i] = it.animatedValue as Float
                    postInvalidateOnAnimation()
                }
            }
            animators.add(animator)
        }
    }

    fun setBarColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setPlaying(playing: Boolean) {
        this.isPlaying = playing
        if (playing) {
            // [FIXED] التأكد من تشغيل الأنميشن حتى لو كانت الحالة السابقة "تشغيل"
            animators.forEach { if (!it.isRunning) it.start() }
        } else {
            animators.forEach { it.cancel() }
            barHeights.fill(0.2f)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        
        val barWidth = width / (numBars * 2f - 1)
        val maxBarHeight = height.toFloat()

        for (i in 0 until numBars) {
            val left = i * (barWidth * 2)
            val top = maxBarHeight * (1 - barHeights[i])
            val right = left + barWidth
            val bottom = maxBarHeight
            canvas.drawRect(left, top, right, bottom, paint)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPlaying) {
            animators.forEach { it.start() }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animators.forEach { it.cancel() }
    }
}
