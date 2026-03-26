package com.example.ringtoneplayer.ui

import android.view.Choreographer
import com.example.ringtoneplayer.databinding.ActivityMainBinding
import kotlin.math.PI

class MotionManager(private val binding: ActivityMainBinding) {

    private var lastFrameTime = 0L
    private var syncProgress = 0
    private var syncDuration = 0
    private var isPlaying = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameTime != 0L) {
                val deltaTime = (frameTimeNanos - lastFrameTime) / 1_000_000_000f
                updateMotions(deltaTime)
            }
            lastFrameTime = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun updateSyncData(progress: Int, duration: Int, playing: Boolean) {
        syncProgress = progress
        syncDuration = duration
        isPlaying = playing
    }

    private fun updateMotions(dt: Float) {
        if (syncDuration <= 0) return
        val p = binding.playerLayout

        // 1. Neon Dot Motion (تحديث حركة نقطة النيون في التصميم الملكي)
        val theta = (syncProgress.toDouble() / syncDuration.toDouble()) * 2.0 * PI - (PI / 2.0)
        val angleDegrees = Math.toDegrees(theta).toFloat() + 90f
        
        val dotParams = p.skinCircular.neonDot.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        dotParams.circleAngle = (angleDegrees + 360) % 360
        p.skinCircular.neonDot.layoutParams = dotParams

        // 2. Continuous Rotations (إضافة دوران ناعم لصورة الألبوم إذا كان يعمل)
        if (isPlaying) {
            p.skinCircular.albumCard.rotation = (p.skinCircular.albumCard.rotation + 10f * dt) % 360f
        }
    }
}
