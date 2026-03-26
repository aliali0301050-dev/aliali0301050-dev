package com.example.ringtoneplayer.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ringtoneplayer.R
import com.example.ringtoneplayer.databinding.LayoutSkinSelectorBinding
import com.example.ringtoneplayer.utils.PlayerPreferences

class SkinSelectorActivity : AppCompatActivity() {

    private lateinit var binding: LayoutSkinSelectorBinding
    private lateinit var preferences: PlayerPreferences
    private var selectedThemeId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutSkinSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = PlayerPreferences(this)
        selectedThemeId = preferences.getInt("selected_neon_theme", 0)

        setupList()
        
        binding.btnSkinBack.setOnClickListener { finish() }
        binding.btnApplySkin.setOnClickListener {
            preferences.putInt("selected_neon_theme", selectedThemeId)
            preferences.playerSkinId = 0
            setResult(RESULT_OK)
            finish()
        }
        
        updateApplyButton(selectedThemeId)
    }

    private fun setupList() {
        // استخدام مسميات الألوان الحقيقية المتوفرة في colors.xml
        val themes = listOf(
            NeonTheme(0, "Neon Pulse", R.color.palette_silk_neon),
            NeonTheme(1, "Cyber Sky", R.color.palette_cyber_neon),
            NeonTheme(2, "Toxic Lime", R.color.palette_forest_neon),
            NeonTheme(3, "Violet Dream", R.color.palette_royal_neon),
            NeonTheme(4, "Gold Ember", R.color.palette_gold_neon)
        )

        binding.rvSkins.layoutManager = LinearLayoutManager(this)
        binding.rvSkins.adapter = ThemeAdapter(themes)
    }

    private fun updateApplyButton(themeId: Int) {
        val accentColors = listOf(
            R.color.palette_silk_neon, R.color.palette_cyber_neon, R.color.palette_forest_neon,
            R.color.palette_royal_neon, R.color.palette_gold_neon
        )
        // التأكد من أن الـ index آمن
        val safeIndex = themeId.coerceIn(0, accentColors.size - 1)
        val color = ContextCompat.getColor(this, accentColors[safeIndex])
        binding.btnApplySkin.backgroundTintList = ColorStateList.valueOf(color)
    }

    data class NeonTheme(val id: Int, val name: String, val accentColor: Int)

    inner class ThemeAdapter(private val themes: List<NeonTheme>) : RecyclerView.Adapter<ThemeAdapter.ThemeVH>() {
        
        inner class ThemeVH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvSkinName)
            val colorCircle: View = v.findViewById(R.id.colorPreview)
            val colorGlow: View = v.findViewById(R.id.colorGlow)
            val selectedIcon: ImageView = v.findViewById(R.id.ivSelected)
            val card: View = v
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_skin_preview, parent, false)
            return ThemeVH(v)
        }

        override fun onBindViewHolder(holder: ThemeVH, position: Int) {
            val theme = themes[position]
            holder.name.text = theme.name
            val accentColor = ContextCompat.getColor(holder.itemView.context, theme.accentColor)
            
            holder.colorCircle.backgroundTintList = ColorStateList.valueOf(accentColor)
            holder.colorGlow.backgroundTintList = ColorStateList.valueOf(accentColor)

            holder.selectedIcon.visibility = if (position == selectedThemeId) View.VISIBLE else View.GONE
            holder.selectedIcon.imageTintList = ColorStateList.valueOf(accentColor)

            holder.card.setOnClickListener {
                val oldId = selectedThemeId
                selectedThemeId = holder.adapterPosition
                notifyItemChanged(oldId)
                notifyItemChanged(selectedThemeId)
                updateApplyButton(selectedThemeId)
            }
        }

        override fun getItemCount() = themes.size
    }
}
