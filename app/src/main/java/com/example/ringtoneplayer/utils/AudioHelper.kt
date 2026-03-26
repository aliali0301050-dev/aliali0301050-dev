package com.example.ringtoneplayer.utils

import android.content.ContentUris
import android.net.Uri
import java.util.*

object AudioHelper {
    /**
     * تنسيق الوقت بدقة عالية mm:ss:SS لاستخدامه في محرر القص
     */
    fun formatPreciseTime(ms: Long): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        val hundredths = (ms % 1000) / 10
        return String.format(Locale.US, "%02d:%02d:%02d", minutes, seconds, hundredths)
    }

    /**
     * تنسيق الوقت العادي mm:ss للمشغل
     */
    fun formatTime(ms: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", ms / 1000 / 60, ms / 1000 % 60)
    }

    /**
     * جلب Uri غلاف الألبوم باستخدام الـ ID
     */
    fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
    }
}
