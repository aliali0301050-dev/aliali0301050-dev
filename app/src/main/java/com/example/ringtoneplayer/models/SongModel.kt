package com.example.ringtoneplayer.models

data class SongModel(
    val id: Long,
    val title: String?,
    val artist: String?,
    val uri: String,
    val path: String?
)
