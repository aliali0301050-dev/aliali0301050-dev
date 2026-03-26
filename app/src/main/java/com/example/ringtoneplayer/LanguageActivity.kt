package com.example.ringtoneplayer

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ringtoneplayer.databinding.ActivityLanguageBinding
import java.util.*

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // تطبيق اللغة الحالية قبل setContentView
        val currentLang = getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("My_Lang", "ar") ?: "ar"
        setLocale(currentLang)
        
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val languages = listOf(
            LangItem("العربية", "ar"),
            LangItem("English", "en"),
            LangItem("Deutsch", "de"),
            LangItem("Español", "es"),
            LangItem("Français", "fr"),
            LangItem("Русский", "ru"),
            LangItem("Türkçe", "tr"),
            LangItem("Português", "pt"),
            LangItem("हिन्दी (Hindi)", "hi"),
            LangItem("简体中文 (Chinese)", "zh"),
            LangItem("فارسی (Persian)", "fa")
        )

        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        val currentLang = getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("My_Lang", "ar") ?: "ar"
        binding.rvLanguages.adapter = LanguageAdapter(languages, currentLang) { selectedLang ->
            if (selectedLang != currentLang) {
                saveLanguage(selectedLang)
            }
        }
    }

    private fun saveLanguage(lang: String) {
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        prefs.edit().putString("My_Lang", lang).apply()
        
        // الطريقة الاحترافية: إعادة تشغيل التطبيق بالكامل من نقطة الصفر
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        
        // إغلاق جميع الأنشطة الحالية لضمان إعادة بناء الواجهة باللغة الجديدة
        finishAffinity()
    }

    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    data class LangItem(val name: String, val code: String)

    inner class LanguageAdapter(
        private val list: List<LangItem>,
        private val currentCode: String,
        private val onLangClick: (String) -> Unit
    ) : RecyclerView.Adapter<LanguageAdapter.LangVH>() {

        inner class LangVH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvLanguageName)
            val check: ImageView = v.findViewById(R.id.ivCheck)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LangVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
            return LangVH(v)
        }

        override fun onBindViewHolder(holder: LangVH, position: Int) {
            val item = list[position]
            holder.name.text = item.name
            holder.check.visibility = if (item.code == currentCode) View.VISIBLE else View.GONE
            
            holder.itemView.setOnClickListener { onLangClick(item.code) }
        }

        override fun getItemCount() = list.size
    }
}
