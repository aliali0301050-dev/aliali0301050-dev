package com.example.ringtoneplayer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlaylistDao {
    @Insert
    fun insert(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists")
    fun getAll(): List<PlaylistEntity>
}