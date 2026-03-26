package com.example.ringtoneplayer.utils

import android.content.Context
import android.media.audiofx.Equalizer
import android.util.Log

class EqualizerHelper(private val context: Context) {
    private var equalizer: Equalizer? = null
    private val preferences = PlayerPreferences(context)

    fun initEqualizer(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = preferences.getBoolean("eq_enabled", true)
            }
            loadSavedSettings()
        } catch (e: Exception) {
            Log.e("EqualizerHelper", "Failed to init equalizer: ${e.message}")
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
            preferences.putInt("eq_band_$band", level.toInt())
        } catch (e: Exception) {}
    }

    fun getBandLevel(band: Short): Short {
        return preferences.getInt("eq_band_$band", 0).toShort()
    }

    fun usePreset(presetIndex: Short) {
        try {
            equalizer?.usePreset(presetIndex)
            val bands = equalizer?.numberOfBands ?: 0
            for (i in 0 until bands) {
                val level = equalizer?.getBandLevel(i.toShort()) ?: 0
                preferences.putInt("eq_band_$i", level.toInt())
            }
            preferences.putInt("eq_preset", presetIndex.toInt())
        } catch (e: Exception) {}
    }

    fun isEnabled(): Boolean = equalizer?.enabled ?: false

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            preferences.putBoolean("eq_enabled", enabled)
        } catch (e: Exception) {}
    }

    private fun loadSavedSettings() {
        try {
            val bands = equalizer?.numberOfBands ?: 0
            for (i in 0 until bands) {
                val level = preferences.getInt("eq_band_$i", 0).toShort()
                equalizer?.setBandLevel(i.toShort(), level)
            }
        } catch (e: Exception) {}
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }

    fun getNumberOfBands(): Short = equalizer?.numberOfBands ?: 0
    fun getBandLevelRange(): ShortArray = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
    fun getCenterFreq(band: Short): Int = equalizer?.getCenterFreq(band) ?: 0
}