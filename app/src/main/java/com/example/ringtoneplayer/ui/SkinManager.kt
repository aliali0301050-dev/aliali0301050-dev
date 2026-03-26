package com.example.ringtoneplayer.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import com.example.ringtoneplayer.databinding.ActivityMainBinding

class SkinManager(private val binding: ActivityMainBinding) {

    private val p get() = binding.playerLayout

    fun getSkinPreferredColors(skinId: Int): Pair<Int, Int> {
        // بما أننا اعتمدنا تصميماً واحداً، سنعيد الألوان الملكية دائماً
        return Color.parseColor("#251329") to Color.parseColor("#FF2D75")
    }

    fun applyPlayerSkin(skinId: Int) {
        // الشكل الوحيد المتاح هو الدائري (skinCircular)
        p.skinCircular.root.visibility = View.VISIBLE
        
        // ضبط الموازن الملكي
        p.visualizer.visibility = View.VISIBLE
        if (p.visualizer is VisualizerView) {
            (p.visualizer as VisualizerView).mode = VisualizerView.VisualizerMode.SILK
        }
    }

    fun updateArtForSkins(bitmap: Bitmap) {
        // تحديث صورة الألبوم للشكل الملكي فقط
        p.skinCircular.ivMainAlbumArt.setImageBitmap(bitmap)
    }
}
