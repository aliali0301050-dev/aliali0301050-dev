package com.example.ringtoneplayer.ui

import android.content.*
import android.graphics.Color
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ringtoneplayer.R
import com.example.ringtoneplayer.databinding.ActivityEqualizerBinding
import com.example.ringtoneplayer.services.MusicPlayerService

class EqualizerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEqualizerBinding
    private var musicService: MusicPlayerService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            setupAudioEffects()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEqualizerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        
        val intent = Intent(this, MusicPlayerService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        setupPresets()
    }

    private fun setupPresets() {
        val presets = listOf("مخصص", "عادي", "بوب", "هيب هوب", "روك", "كلاسيكي", "جاز", "رقص")
        binding.presetsContainer.removeAllViews()
        presets.forEach { name ->
            val tv = TextView(this).apply {
                text = name
                setTextColor(Color.WHITE)
                setPadding(40, 15, 40, 15)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(10, 0, 10, 0)
                layoutParams = params
                background = getDrawable(R.drawable.bg_preset_button)
                setOnClickListener {
                    updatePresetSelection(this)
                    applyPreset(name)
                }
            }
            binding.presetsContainer.addView(tv)
        }
    }

    private fun updatePresetSelection(selected: TextView) {
        for (i in 0 until binding.presetsContainer.childCount) {
            val child = binding.presetsContainer.getChildAt(i) as TextView
            child.alpha = 0.5f
            child.background = getDrawable(R.drawable.bg_preset_button)
        }
        selected.alpha = 1.0f
        // Optional: highlight color
    }

    private fun applyPreset(name: String) {
        val equalizer = musicService?.getEqualizer() ?: return
        val presetIndex = findPresetIndex(equalizer, name)
        if (presetIndex >= 0) {
            equalizer.usePreset(presetIndex.toShort())
            setupBands(equalizer) // Refresh sliders
        }
    }

    private fun findPresetIndex(equalizer: Equalizer, name: String): Int {
        for (i in 0 until equalizer.numberOfPresets.toInt()) {
            if (equalizer.getPresetName(i.toShort()).lowercase().contains(name.lowercase())) {
                return i
            }
        }
        return -1
    }

    private fun setupAudioEffects() {
        val equalizer = musicService?.getEqualizer()
        if (equalizer != null) {
            setupBands(equalizer)
            binding.switchEq.isChecked = equalizer.enabled
            binding.switchEq.setOnCheckedChangeListener { _, isChecked ->
                equalizer.enabled = isChecked
            }
        }
        setupKnobs()
    }

    private fun setupBands(equalizer: Equalizer) {
        val numBands = equalizer.numberOfBands
        val range = equalizer.bandLevelRange ?: shortArrayOf(-1500, 1500)
        val minLevel = range[0]
        val maxLevel = range[1]

        binding.bandsContainer.removeAllViews()

        for (i in 0 until numBands.toInt()) {
            val band = i.toShort()
            val freq = equalizer.getCenterFreq(band) / 1000
            
            val bandView = LayoutInflater.from(this).inflate(R.layout.item_equalizer_band, binding.bandsContainer, false)
            val seekBar = bandView.findViewById<SeekBar>(R.id.seekBar)
            val tvFreq = bandView.findViewById<TextView>(R.id.tvFreq)
            val tvLevel = bandView.findViewById<TextView>(R.id.tvLevel)

            seekBar.max = (maxLevel - minLevel).toInt()
            val currentLevel = equalizer.getBandLevel(band)
            seekBar.progress = (currentLevel - minLevel).toInt()
            tvLevel.text = "${currentLevel / 100}"
            
            tvFreq.text = if (freq < 1000) "${freq}Hz" else "${freq / 1000}kHz"

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                    if (f) {
                        val level = (p + minLevel).toShort()
                        musicService?.updateEqualizerBand(band, level)
                        tvLevel.text = "${level / 100}"
                    }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })

            binding.bandsContainer.addView(bandView)
        }
    }

    private fun setupKnobs() {
        val bassBoost = musicService?.getBassBoost()
        val virtualizer = musicService?.getVirtualizer()

        // Sync initial values
        val bassStrength = bassBoost?.roundedStrength ?: 0
        binding.progressBass.progress = bassStrength.toInt()
        binding.knobBass.rotation = (bassStrength.toFloat() / 1000f) * 270f
        
        val virtStrength = virtualizer?.roundedStrength ?: 0
        binding.progressTreble.progress = virtStrength.toInt()
        binding.knobTreble.rotation = (virtStrength.toFloat() / 1000f) * 270f

        // Bass Boost Knob logic (Click to cycle or drag? Image suggests a knob. We'll stick to cycle for simplicity or add drag later)
        binding.knobBass.setOnClickListener {
            val current = bassBoost?.roundedStrength ?: 0
            val next = if (current >= 1000) 0 else current + 100
            musicService?.updateBassBoost(next.toShort())
            binding.progressBass.progress = next.toInt()
            binding.knobBass.animate().rotation((next.toFloat() / 1000f) * 270f).setDuration(200).start()
        }

        // Virtualizer (Treble in design) logic
        binding.knobTreble.setOnClickListener {
            val current = virtualizer?.roundedStrength ?: 0
            val next = if (current >= 1000) 0 else current + 100
            musicService?.updateVirtualizer(next.toShort())
            binding.progressTreble.progress = next.toInt()
            binding.knobTreble.animate().rotation((next.toFloat() / 1000f) * 270f).setDuration(200).start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}
