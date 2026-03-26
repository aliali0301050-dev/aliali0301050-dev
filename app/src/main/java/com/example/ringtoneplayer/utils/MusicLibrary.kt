package com.example.ringtoneplayer.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.ringtoneplayer.models.Song
import com.example.ringtoneplayer.models.SongModel

object MusicLibrary {

    fun loadSongsFromDevice(context: Context): List<Song> {
        val songList = mutableListOf<Song>()
        val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // تم التعديل لعرض ملفات الموسيقى فقط واستبعاد نغمات النظام
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(contentUri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: ""
                    
                    // استبعاد ملفات النظام والمصنع بشكل نهائي
                    if (path.contains("/system/", ignoreCase = true) || 
                        path.contains("/product/", ignoreCase = true) ||
                        path.contains("/preloads/", ignoreCase = true)) {
                        continue
                    }

                    val id = cursor.getLong(idCol)
                    songList.add(
                        Song(
                            id = id,
                            title = cursor.getString(titleCol) ?: "Unknown",
                            artist = cursor.getString(artistCol) ?: "Unknown Artist",
                            album = cursor.getString(albumCol) ?: "Unknown Album",
                            duration = cursor.getLong(durCol),
                            size = cursor.getLong(sizeCol),
                            uri = ContentUris.withAppendedId(contentUri, id),
                            path = path,
                            albumId = cursor.getLong(albumIdCol)
                        )
                    )
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        
        return songList
    }

    fun fetchAudioFiles(context: Context): ArrayList<SongModel> {
        val list = ArrayList<SongModel>()
        val songs = loadSongsFromDevice(context)
        for (s in songs) {
            list.add(SongModel(s.id, s.title, s.artist, s.uri.toString(), s.path))
        }
        return list
    }
}
