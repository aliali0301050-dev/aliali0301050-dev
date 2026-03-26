package com.example.ringtoneplayer.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.example.ringtoneplayer.R

class ColorThemeHelper(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)

    enum class ThemeType(val bgColor: Int, val accentColor: Int) {
        MIDNIGHT_ECLIPSE(Color.parseColor("#0A0A0A"), Color.parseColor("#FFD700")),
        ROYAL_AMETHYST(Color.parseColor("#1A0B2E"), Color.parseColor("#E0B0FF")),
        OCEANIC_DEPTH(Color.parseColor("#001F3F"), Color.parseColor("#00E5FF")),
        CARBON_RED(Color.parseColor("#121212"), Color.parseColor("#DC143C"))
    }

    fun applyTheme(activity: Activity) {
        activity.setTheme(R.style.AppTheme)
    }

    fun getSelectedTheme(): ThemeType {
        val themeName = prefs.getString("selected_theme_v3", ThemeType.MIDNIGHT_ECLIPSE.name)
        return try { ThemeType.valueOf(themeName!!) } catch (e: Exception) { ThemeType.MIDNIGHT_ECLIPSE }
    }

    fun saveTheme(theme: ThemeType) {
        prefs.edit().putString("selected_theme_v3", theme.name).apply()
    }
}
