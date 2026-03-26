package com.example.ringtoneplayer.models

data class Album(
    val name: String,
    val artist: String,
    val albumId: Long,
    val songs: List<Song>
)
