package com.example.ringtoneplayer.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.*
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.ringtoneplayer.R
import com.example.ringtoneplayer.databinding.ActivityPlayerBinding
import com.example.ringtoneplayer.services.MusicPlayerService
import com.example.ringtoneplayer.utils.AudioHelper
import com.example.ringtoneplayer.utils.PlayerPreferences

class PlayerActivity : AppCompatActivity(), MusicPlayerService.MusicPlayerCallback {

    private lateinit var binding: ActivityPlayerBinding
    private var musicService: MusicPlayerService? = null
    private var isBound = false
    private lateinit var preferences: PlayerPreferences
    private var pulseAnimator: ObjectAnimator? = null
    private lateinit var audioManager: AudioManager

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()
            isBound = true
            musicService?.registerCallback(this@PlayerActivity)
            updateUI()
            updateShuffleRepeatIcons()
            applySelectedNeonTheme() 
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = PlayerPreferences(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setupListeners()
        bindMusicService()
        startPulseAnimation()
    }

    private fun startPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.skinCircular.auraView,
            PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.15f),
            PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.15f),
            PropertyValuesHolder.ofFloat("alpha", 0.15f, 0.35f)
        ).apply {
            duration = 2500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun applySelectedNeonTheme() {
        val themeId = preferences.getInt("selected_neon_theme", 0)
        val palette = getNeonPalette(themeId)
        
        val bgColor = ContextCompat.getColor(this, palette.first)
        val accentColor = ContextCompat.getColor(this, palette.second)

        val bgGradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(bgColor, Color.parseColor("#050505"), Color.BLACK)
        )
        binding.playerMainContainer.background = bgGradient

        val accentList = ColorStateList.valueOf(accentColor)
        // [FIXED] Removed reference to playGlow since it was removed in capsule design
        binding.skinCircular.neonDot.backgroundTintList = accentList
        binding.skinCircular.auraView.backgroundTintList = accentList
        binding.topNavBar.tvTabSong.setTextColor(accentColor)
    }

    private fun getNeonPalette(id: Int): Pair<Int, Int> {
        return when (id) {
            0 -> R.color.palette_silk_top to R.color.palette_silk_neon
            1 -> R.color.palette_cyber_top to R.color.palette_cyber_neon
            2 -> R.color.palette_forest_top to R.color.palette_forest_neon
            3 -> R.color.palette_royal_top to R.color.palette_royal_neon
            4 -> R.color.palette_gold_top to R.color.palette_gold_neon
            else -> R.color.palette_silk_top to R.color.palette_silk_neon
        }
    }

    private fun setupListeners() {
        // أزرار التحكم الأساسية
        binding.controlsLayout.btnPlayPause.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            musicService?.let { if (it.isPlaying()) it.pauseMusic() else it.resumeMusic() }
        }
        binding.controlsLayout.btnNext.setOnClickListener { musicService?.playNext() }
        binding.controlsLayout.btnPrev.setOnClickListener { musicService?.playPrevious() }
        
        binding.controlsLayout.btnShuffle.setOnClickListener {
            musicService?.let {
                it.isShuffle = !it.isShuffle
                preferences.isShuffle = it.isShuffle
                updateShuffleRepeatIcons()
                Toast.makeText(this, if (it.isShuffle) "عشوائي: مفعل" else "عشوائي: معطل", Toast.LENGTH_SHORT).show()
            }
        }

        binding.controlsLayout.btnRepeat.setOnClickListener {
            musicService?.let {
                it.repeatMode = (it.repeatMode + 1) % 3
                preferences.putInt("repeat_mode_int", it.repeatMode)
                updateShuffleRepeatIcons()
                val modes = arrayOf("تكرار: معطل", "تكرار الكل", "تكرار مرة واحدة")
                Toast.makeText(this, modes[it.repeatMode], Toast.LENGTH_SHORT).show()
            }
        }

        // شريط الأدوات الإضافي (Extra Controls)
        binding.extraControls.btnEqualizer.setOnClickListener {
            startActivity(Intent(this, EqualizerActivity::class.java))
        }

        binding.extraControls.btnVolume.setOnClickListener {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        }

        binding.extraControls.btnTimer.setOnClickListener {
            DialogHelper(this).showSleepTimerDialog(musicService)
        }

        binding.extraControls.btnHistory.setOnClickListener {
            Toast.makeText(this, "سجل التشغيل قيد التطوير", Toast.LENGTH_SHORT).show()
        }

        binding.extraControls.btnList.setOnClickListener {
            Toast.makeText(this, "قائمة الانتظار قيد التطوير", Toast.LENGTH_SHORT).show()
        }

        // شريط التقدم
        binding.skinCircular.seekBar.setOnSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(seekBar: CircularSeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seekTo(progress)
                    binding.skinCircular.tvCurrentTime.text = AudioHelper.formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {}
        })

        binding.topNavBar.btnBack.setOnClickListener { finish() }
        
        binding.topNavBar.btnTheme.setOnClickListener {
            startActivity(Intent(this, SkinSelectorActivity::class.java))
        }
    }

    private fun updateShuffleRepeatIcons() {
        musicService?.let {
            binding.controlsLayout.btnShuffle.alpha = if (it.isShuffle) 1.0f else 0.4f
            binding.controlsLayout.btnRepeat.alpha = if (it.repeatMode > 0) 1.0f else 0.4f
        }
    }

    override fun onProgressUpdate(position: Int, duration: Int) {
        runOnUiThread {
            binding.skinCircular.seekBar.max = duration
            binding.skinCircular.seekBar.progress = position
            binding.skinCircular.tvCurrentTime.text = AudioHelper.formatTime(position)
            binding.skinCircular.tvTotalTime.text = AudioHelper.formatTime(duration)
            
            val angle = (position.toFloat() / duration * 360f + 270f) % 360f
            val params = binding.skinCircular.neonDot.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.circleAngle = angle
            binding.skinCircular.neonDot.layoutParams = params
        }
    }

    override fun onPlaybackStarted() { runOnUiThread { updateUI() } }
    override fun onPlaybackPaused() { runOnUiThread { updateUI(); pulseAnimator?.pause() } }
    override fun onPlaybackResumed() { runOnUiThread { updateUI(); pulseAnimator?.resume() } }
    override fun onPlaybackCompleted() { runOnUiThread { musicService?.playNext() } }
    override fun onArtUpdated(bitmap: Bitmap) { runOnUiThread { binding.skinCircular.ivMainAlbumArt.setImageBitmap(bitmap) } }
    override fun onNextTrack() { runOnUiThread { updateUI() } }
    override fun onPreviousTrack() { runOnUiThread { updateUI() } }
    override fun onVisualizerData(data: ByteArray) { runOnUiThread { (binding.visualizer as? VisualizerView)?.updateVisualizer(data) } }

    private fun updateUI() {
        musicService?.let {
            binding.infoSection.tvMainTitle.text = it.currentSongTitle
            binding.infoSection.tvMainArtist.text = it.currentSongArtist
            binding.controlsLayout.btnPlayPause.setImageResource(if (it.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play)
        }
    }

    private fun bindMusicService() {
        bindService(Intent(this, MusicPlayerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnimator?.cancel()
        if (isBound) { musicService?.unregisterCallback(); unbindService(connection) }
    }
}
