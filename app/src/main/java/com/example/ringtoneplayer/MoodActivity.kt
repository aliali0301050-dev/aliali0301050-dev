package com.example.ringtoneplayer

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MoodActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood)

        val btnCalm: Button = findViewById(R.id.btnCalm)
        val btnHappy: Button = findViewById(R.id.btnHappy)
        val btnEnergetic: Button = findViewById(R.id.btnEnergetic)

        btnCalm.setOnClickListener { showMessage("Calm Mode Selected") }
        btnHappy.setOnClickListener { showMessage("Happy Mode Selected") }
        btnEnergetic.setOnClickListener { showMessage("Energetic Mode Selected") }
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
