package com.example.ringtoneplayer.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val size: Long,
    val uri: Uri,
    val path: String,
    val albumId: Long,
    var isFavorite: Boolean = false
) : Parcelable
