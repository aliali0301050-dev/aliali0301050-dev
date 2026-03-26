package com.example.ringtoneplayer.ui

import android.content.*
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ringtoneplayer.databinding.ActivityVolumeBoosterBinding
import com.example.ringtoneplayer.services.MusicPlayerService
import com.example.ringtoneplayer.utils.PlayerPreferences

class VolumeBoosterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVolumeBoosterBinding
    private var musicService: MusicPlayerService? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private lateinit var preferences: PlayerPreferences

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            setupBooster()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVolumeBoosterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = PlayerPreferences(this)

        binding.btnBack.setOnClickListener { finish() }

        setupButtons()

        val intent = Intent(this, MusicPlayerService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun setupBooster() {
        val sessionId = musicService?.audioSessionId ?: return
        if (sessionId == 0) return

        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                val gain = preferences.getInt("loudness_gain", 0)
                setTargetGain(gain)
                enabled = true
            }
            updateUI(preferences.getInt("loudness_gain", 0))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupButtons() {
        binding.btn125.setOnClickListener { applyBoost(250) }
        binding.btn150.setOnClickListener { applyBoost(500) }
        binding.btn175.setOnClickListener { applyBoost(750) }
        
        binding.imgKnobMain.setOnClickListener {
            val currentGain = preferences.getInt("loudness_gain", 0)
            val nextGain = when {
                currentGain < 250 -> 250
                currentGain < 500 -> 500
                currentGain < 750 -> 750
                else -> 0
            }
            applyBoost(nextGain)
        }
    }

    private fun applyBoost(gain: Int) {
        try {
            loudnessEnhancer?.setTargetGain(gain)
            preferences.putInt("loudness_gain", gain)
            updateUI(gain)
        } catch (e: Exception) {
            Toast.makeText(this, "فشل تطبيق التعزيز", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(gain: Int) {
        val percent = 100 + (gain / 10)
        binding.tvBoostPercent.text = "$percent%"
        binding.imgKnobMain.rotation = (gain.toFloat() / 750f * 270f)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        loudnessEnhancer?.release()
    }
}
