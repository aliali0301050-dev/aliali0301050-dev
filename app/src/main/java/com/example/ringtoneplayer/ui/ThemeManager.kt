package com.example.ringtoneplayer.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.ringtoneplayer.R
import com.example.ringtoneplayer.databinding.ActivityMainBinding

class ThemeManager(private val context: Context, private val binding: ActivityMainBinding) {

    private var currentAccentColor: Int = Color.parseColor("#FF2D75")

    fun getCurrentAccentColor(): Int = currentAccentColor

    fun applyNeonTheme(themeId: Int) {
        val palette = getNeonPalette(themeId)
        val bgColor = ContextCompat.getColor(context, palette.first)
        val accentColor = ContextCompat.getColor(context, palette.second)
        currentAccentColor = accentColor
        
        applyGlobalThemeToUI(bgColor, accentColor)
    }

    private fun getNeonPalette(id: Int): Pair<Int, Int> {
        return when (id) {
            0 -> Pair(R.color.palette_silk_top, R.color.palette_silk_neon)
            1 -> Pair(R.color.palette_cyber_top, R.color.palette_cyber_neon)
            2 -> Pair(R.color.palette_forest_top, R.color.palette_forest_neon)
            3 -> Pair(R.color.palette_royal_top, R.color.palette_royal_neon)
            4 -> Pair(R.color.palette_gold_top, R.color.palette_gold_neon)
            else -> Pair(R.color.palette_silk_top, R.color.palette_silk_neon)
        }
    }

    fun applyGlobalThemeToUI(bgColor: Int, accentColor: Int) {
        val p = binding.playerLayout
        val accentList = ColorStateList.valueOf(accentColor)
        
        val bgGradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(bgColor, Color.parseColor("#050505"), Color.BLACK)
        )
        p.playerMainContainer.background = bgGradient
        
        p.skinCircular.neonDot.backgroundTintList = accentList
        p.skinCircular.auraView.backgroundTintList = accentList
        p.skinCircular.seekBar.setProgressColor(accentColor)
        p.skinCircular.albumCard.strokeColor = accentColor
        p.topNavBar.tvTabSong.setTextColor(accentColor)
        
        // [FIXED] Removed reference to playGlow since it was removed in capsule design
        
        binding.tabLayout.setSelectedTabIndicatorColor(accentColor)
        binding.tabLayout.setTabTextColors(Color.parseColor("#B3FFFFFF"), accentColor)
        binding.seekBar.progressTintList = accentList
        binding.btnPlayPause.backgroundTintList = accentList

        val root = binding.root
        val headerIds = intArrayOf(R.id.headerFiles, R.id.headerAudio, R.id.headerAppearance, R.id.headerSupport)
        headerIds.forEach { id -> root.findViewById<TextView>(id)?.setTextColor(accentColor) }

        val iconIds = intArrayOf(
            R.id.i1, R.id.i2, R.id.i3, R.id.iTrash, R.id.i5,
            R.id.i7, R.id.iPauseLoss, R.id.i8, R.id.iBtBlock, R.id.iTheme, 
            R.id.i11, R.id.iAlbumGrid, R.id.i12, R.id.i13, R.id.iUpdate,
            R.id.iShare, R.id.iSupport, R.id.iPrivacy
        )
        iconIds.forEach { id -> root.findViewById<ImageView>(id)?.imageTintList = accentList }
    }
}
