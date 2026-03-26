package com.example.ringtoneplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistActivity : AppCompatActivity() {
    private lateinit var db: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlaylistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        db = Room.databaseBuilder(applicationContext, DatabaseHelper::class.java, "ringtone_db").build()

        loadPlaylists()
    }

    private fun loadPlaylists() {
        CoroutineScope(Dispatchers.IO).launch {
            val playlists = db.playlistDao().getAll()
            withContext(Dispatchers.Main) {
                adapter = PlaylistAdapter(playlists)
                recyclerView.adapter = adapter
            }
        }
    }
}
