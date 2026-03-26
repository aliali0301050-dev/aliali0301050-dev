package com.example.ringtoneplayer.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Playlist(
    val id: Long,
    val name: String,
    val songs: MutableList<Song> = mutableListOf()
) : Parcelable
