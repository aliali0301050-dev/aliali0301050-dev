package com.example.ringtoneplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ringtoneplayer.utils.ColorThemeHelper
import java.util.*

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString("My_Lang", "ar") ?: "ar"
        
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // استخدام المساعد الجديد بعد تغيير الاسم لمنع التضارب
        ColorThemeHelper(this).applyTheme(this)
        super.onCreate(savedInstanceState)
    }
}
