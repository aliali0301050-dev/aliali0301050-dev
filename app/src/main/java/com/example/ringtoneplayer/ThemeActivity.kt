package com.example.ringtoneplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ringtoneplayer.databinding.ActivityThemeBinding
import com.example.ringtoneplayer.ui.BackgroundAdapter
import com.example.ringtoneplayer.ui.BackgroundItem
import com.example.ringtoneplayer.utils.ColorThemeHelper
import com.example.ringtoneplayer.utils.PlayerPreferences

class ThemeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeBinding
    private lateinit var themeHelper: ColorThemeHelper
    private lateinit var preferences: PlayerPreferences
    private lateinit var bgAdapter: BackgroundAdapter

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val uriString = it.toString()
            preferences.customBackgroundUri = uriString
            preferences.saveCustomGalleryImage(uriString)
            refreshBackgroundList()
            Toast.makeText(this, R.string.background_updated, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        themeHelper = ColorThemeHelper(this)
        preferences = PlayerPreferences(this)
        
        themeHelper.applyTheme(this)

        setupThemeColorsList()
        setupBackgroundList()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupThemeColorsList() {
        val themes = listOf(
            ThemeItem(getString(R.string.theme_midnight_gold), ColorThemeHelper.ThemeType.MIDNIGHT_ECLIPSE, ColorThemeHelper.ThemeType.MIDNIGHT_ECLIPSE.accentColor),
            ThemeItem(getString(R.string.theme_royal_violet), ColorThemeHelper.ThemeType.ROYAL_AMETHYST, ColorThemeHelper.ThemeType.ROYAL_AMETHYST.accentColor),
            ThemeItem(getString(R.string.theme_ocean_cyan), ColorThemeHelper.ThemeType.OCEANIC_DEPTH, ColorThemeHelper.ThemeType.OCEANIC_DEPTH.accentColor),
            ThemeItem(getString(R.string.theme_cyber_red), ColorThemeHelper.ThemeType.CARBON_RED, ColorThemeHelper.ThemeType.CARBON_RED.accentColor)
        )

        binding.rvThemes.layoutManager = GridLayoutManager(this, 2)
        binding.rvThemes.adapter = ThemeAdapter(themes) { selectedTheme ->
            themeHelper.saveTheme(selectedTheme)
            binding.root.setBackgroundColor(selectedTheme.bgColor)
            window.statusBarColor = selectedTheme.bgColor
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, 350)
        }
    }

    private fun setupBackgroundList() {
        binding.rvBackgrounds.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        refreshBackgroundList()
    }

    private fun refreshBackgroundList() {
        val items = mutableListOf<BackgroundItem>()
        
        // 1. زر الإضافة
        items.add(BackgroundItem("add", BackgroundItem.Type.ADD_BUTTON))

        // 2. صور الهاتف
        preferences.getCustomGalleryImages().forEach { uri ->
            items.add(BackgroundItem(uri, BackgroundItem.Type.IMAGE))
        }

        // 3. تدرجات لونية
        items.add(BackgroundItem("#0F0C29,#302B63,#24243E", BackgroundItem.Type.GRADIENT)) 
        items.add(BackgroundItem("#833ab4,#fd1d1d,#fcb045", BackgroundItem.Type.GRADIENT)) 
        items.add(BackgroundItem("#000000,#434343", BackgroundItem.Type.GRADIENT))         
        items.add(BackgroundItem("#1e3c72,#2a5298", BackgroundItem.Type.GRADIENT))         
        items.add(BackgroundItem("#d31027,#ea384d", BackgroundItem.Type.GRADIENT))         
        items.add(BackgroundItem("#11998e,#38ef7d", BackgroundItem.Type.GRADIENT))         
        items.add(BackgroundItem("#8e2de2,#4a00e0", BackgroundItem.Type.GRADIENT))         

        // 4. ألوان صلبة
        items.add(BackgroundItem("#05070A", BackgroundItem.Type.COLOR)) 
        items.add(BackgroundItem("#1A1A2E", BackgroundItem.Type.COLOR)) 
        items.add(BackgroundItem("#2D033B", BackgroundItem.Type.COLOR)) 

        bgAdapter = BackgroundAdapter(items, preferences.customBackgroundUri) { item ->
            if (item.type == BackgroundItem.Type.ADD_BUTTON) {
                pickImageLauncher.launch("image/*")
            } else {
                preferences.customBackgroundUri = item.value
                refreshBackgroundList()
                Toast.makeText(this, R.string.background_updated, Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvBackgrounds.adapter = bgAdapter
    }

    data class ThemeItem(val name: String, val type: ColorThemeHelper.ThemeType, val colorInt: Int)

    inner class ThemeAdapter(
        private val list: List<ThemeItem>,
        private val onThemeClick: (ColorThemeHelper.ThemeType) -> Unit
    ) : RecyclerView.Adapter<ThemeAdapter.ThemeVH>() {

        inner class ThemeVH(v: View) : RecyclerView.ViewHolder(v) {
            val preview: View = v.findViewById(R.id.viewColorPreview)
            val name: TextView = v.findViewById(R.id.tvThemeName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_theme, parent, false)
            return ThemeVH(v)
        }

        override fun onBindViewHolder(holder: ThemeVH, position: Int) {
            val item = list[position]
            holder.name.text = item.name
            holder.preview.setBackgroundColor(item.colorInt)
            holder.itemView.setOnClickListener { onThemeClick(item.type) }
        }

        override fun getItemCount() = list.size
    }
}
