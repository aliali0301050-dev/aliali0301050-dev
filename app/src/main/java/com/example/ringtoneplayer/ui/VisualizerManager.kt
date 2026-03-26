package com.example.ringtoneplayer.ui

import com.example.ringtoneplayer.databinding.ActivityMainBinding

class VisualizerManager(private val binding: ActivityMainBinding) {

    fun updateVisualizer(data: ByteArray) {
        val p = binding.playerLayout
        
        // تحديث الموازن الرئيسي الوحيد في التصميم الملكي
        if (p.visualizer is VisualizerView) {
            (p.visualizer as VisualizerView).updateVisualizer(data)
        }
    }
}
