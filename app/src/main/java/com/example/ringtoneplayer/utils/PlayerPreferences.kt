package com.example.ringtoneplayer.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.ringtoneplayer.models.Song
import com.example.ringtoneplayer.models.RepeatMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class PlayerPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
    private val playCountsPrefs: SharedPreferences = context.getSharedPreferences("play_counts", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    
    fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    
    fun getFloat(key: String, defaultValue: Float): Float = prefs.getFloat(key, defaultValue)
    fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()

    fun getString(key: String, defaultValue: String?): String? = prefs.getString(key, defaultValue)
    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    // --- ميزة التبديل العشوائي (Shuffle) ---
    var isShuffle: Boolean
        get() = prefs.getBoolean("is_shuffle", false)
        set(value) = prefs.edit().putBoolean("is_shuffle", value).apply()

    // --- ميزة حفظ صور المعرض المخصصة ---
    fun saveCustomGalleryImage(uri: String) {
        val currentImages = getCustomGalleryImages().toMutableList()
        if (!currentImages.contains(uri)) {
            currentImages.add(0, uri)
            val limitedList = if (currentImages.size > 12) currentImages.take(12) else currentImages
            prefs.edit().putString("custom_gallery_list", gson.toJson(limitedList)).apply()
        }
    }

    fun getCustomGalleryImages(): List<String> {
        val json = prefs.getString("custom_gallery_list", "[]")
        return try {
            gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    var sortType: String
        get() = prefs.getString("sort_type", "date") ?: "date"
        set(value) = prefs.edit().putString("sort_type", value).apply()

    var isAscending: Boolean
        get() = prefs.getBoolean("is_ascending", false)
        set(value) = prefs.edit().putBoolean("is_ascending", value).apply()

    var minDuration: Int
        get() = prefs.getInt("min_duration", 0)
        set(value) = prefs.edit().putInt("min_duration", value).apply()

    var customBackgroundUri: String?
        get() = prefs.getString("custom_bg_uri", null)
        set(value) = prefs.edit().putString("custom_bg_uri", value).apply()

    var playerSkinId: Int
        get() = prefs.getInt("player_skin_id", 0)
        set(value) = prefs.edit().putInt("player_skin_id", value).apply()

    var lastTrackIndex: Int
        get() = prefs.getInt("last_track_index", 0)
        set(value) = prefs.edit().putInt("last_track_index", value).apply()

    var lastPosition: Int
        get() = prefs.getInt("last_position", 0)
        set(value) = prefs.edit().putInt("last_position", value).apply()

    fun getFavoriteSongs(): Set<Long> {
        val json = prefs.getString("favorite_songs", "[]")
        return try {
            gson.fromJson(json, object : TypeToken<Set<Long>>() {}.type)
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun saveFavoriteSongs(favorites: Set<Long>) {
        prefs.edit().putString("favorite_songs", gson.toJson(favorites)).apply()
    }

    // --- محرك النسخ الاحتياطي (Backup Engine) ---
    fun exportBackup(context: Context): Uri? {
        val backupData = mutableMapOf<String, Any>()
        backupData["favorite_songs"] = getFavoriteSongs().toList()
        backupData["player_skin_id"] = playerSkinId
        backupData["sort_type"] = sortType
        backupData["is_ascending"] = isAscending
        backupData["min_duration"] = minDuration
        backupData["custom_bg_uri"] = customBackgroundUri ?: ""
        backupData["gallery_images"] = getCustomGalleryImages()

        val json = gson.toJson(backupData)
        val file = File(context.getExternalFilesDir(null), "RingtonePlayer_Backup.json")
        file.writeText(json)
        
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun importBackup(jsonString: String): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(jsonString, type)
            
            data["favorite_songs"]?.let { 
                val list = (it as List<*>).map { item -> (item as Double).toLong() }
                saveFavoriteSongs(list.toSet())
            }
            data["player_skin_id"]?.let { playerSkinId = (it as Double).toInt() }
            data["sort_type"]?.let { sortType = it as String }
            data["is_ascending"]?.let { isAscending = it as Boolean }
            data["min_duration"]?.let { minDuration = (it as Double).toInt() }
            data["custom_bg_uri"]?.let { customBackgroundUri = it as String }
            data["gallery_images"]?.let { 
                prefs.edit().putString("custom_gallery_list", gson.toJson(it)).apply()
            }
            true
        } catch (e: Exception) { false }
    }
}
