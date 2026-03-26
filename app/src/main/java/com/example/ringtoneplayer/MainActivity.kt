package com.example.ringtoneplayer

import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.ringtoneplayer.databinding.ActivityMainBinding
import com.example.ringtoneplayer.services.MusicPlayerService
import com.example.ringtoneplayer.models.Song
import com.example.ringtoneplayer.utils.*
import com.example.ringtoneplayer.ui.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity(), ServiceConnection, MusicPlayerService.MusicPlayerCallback {

    lateinit var binding: ActivityMainBinding
    private lateinit var sheetBehavior: BottomSheetBehavior<FrameLayout>
    val viewModel: MainViewModel by viewModels()
    private var musicService: MusicPlayerService? = null
    private var isBound = false

    private lateinit var preferences: PlayerPreferences
    private lateinit var skinManager: SkinManager
    private lateinit var motionManager: MotionManager
    private lateinit var themeManager: com.example.ringtoneplayer.ui.ThemeManager
    private lateinit var playbackManager: PlaybackManager
    private lateinit var dialogHelper: DialogHelper
    private lateinit var permissionManager: PermissionManager

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private lateinit var coverPickerLauncher: ActivityResultLauncher<String>
    private lateinit var deleteLauncher: ActivityResultLauncher<IntentSenderRequest>

    private var lastLoadedSongId: Long = -1L

    private val currentSongObserver = Observer<Song?> { song ->
        song?.let { 
            updateUIForSong(it)
            updateFavoriteIcon(it.isFavorite)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initManagers()
        setupLaunchers()
        setupUI()
        bindMusicService()
        
        // فحص التحديثات عند فتح التطبيق
        checkAppUpdates()
        
        permissionManager.checkStoragePermissions({ loadSongsFromDevice() }, requestPermissionLauncher)
    }

    private fun checkAppUpdates() {
        lifecycleScope.launch {
            UpdateManager(this@MainActivity).checkForUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        applySelectedColorTheme()
        loadCustomBackground()
    }

    private fun initManagers() {
        preferences = PlayerPreferences(this)
        skinManager = SkinManager(binding)
        motionManager = MotionManager(binding)
        themeManager = com.example.ringtoneplayer.ui.ThemeManager(this, binding)
        playbackManager = PlaybackManager(binding, viewModel) { musicService }
        dialogHelper = DialogHelper(this)
        permissionManager = PermissionManager(this)
    }

    private fun setupLaunchers() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val uriString = it.toString()
                preferences.customBackgroundUri = uriString
                viewModel.setCustomBackgroundUri(uriString)
                loadCustomBackground(uriString)
            }
        }
        coverPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val bitmap = Glide.with(applicationContext).asBitmap().load(it).submit(500, 500).get()
                        musicService?.updateCurrentArt(bitmap)
                    } catch (e: Exception) {}
                }
            }
        }
        deleteLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) { loadSongsFromDevice() }
        }
    }

    private fun setupUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        sheetBehavior = BottomSheetBehavior.from(binding.fullPlayerSheet)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> binding.miniPlayerCard.visibility = View.GONE
                    BottomSheetBehavior.STATE_HIDDEN, BottomSheetBehavior.STATE_COLLAPSED -> binding.miniPlayerCard.visibility = View.VISIBLE
                    else -> {}
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.miniPlayerCard.alpha = 1f - slideOffset
                if (slideOffset > 0.5f) binding.miniPlayerCard.visibility = View.GONE
                else if (slideOffset < 0.5f && sheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) binding.miniPlayerCard.visibility = View.VISIBLE
            }
        })

        binding.miniPlayerCard.setOnClickListener { sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED }
        binding.viewPager.adapter = MainPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = true

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            val resourceId = when (pos) {
                0 -> R.string.tab_songs
                1 -> R.string.tab_albums
                2 -> R.string.tab_artists
                3 -> R.string.tab_folders
                4 -> R.string.tab_playlists
                5 -> R.string.tab_genres
                6 -> R.string.tab_books
                7 -> R.string.tab_settings
                else -> null
            }
            tab.text = if (resourceId != null) getString(resourceId) else "Tab ${pos + 1}"
        }.attach()

        setupControlListeners()
        setupTopBarListeners()
        setupSystemInsets()
        
        viewModel.songs.observe(this) { songs ->
            binding.tvPlayCount.text = "${getString(R.string.play)} (${songs.size})"
        }
        
        viewModel.currentSong.observeForever(currentSongObserver)
        viewModel.customBackgroundUri.observe(this) { uri -> loadCustomBackground(uri) }

        motionManager.start()
        applySelectedColorTheme()
        loadCustomBackground()
    }

    private fun setupTopBarListeners() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.playAllLayout.setOnClickListener {
            val songs = viewModel.songs.value
            if (!songs.isNullOrEmpty()) {
                viewModel.setCurrentSong(0)
                Toast.makeText(this, "بدأ تشغيل الكل", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.no_music_found, Toast.LENGTH_SHORT).show()
            }
        }

        binding.shuffleLayout.setOnClickListener {
            val songs = viewModel.songs.value
            if (!songs.isNullOrEmpty()) {
                musicService?.isShuffle = true
                val randomIndex = (songs.indices).random()
                viewModel.setCurrentSong(randomIndex)
                updateShuffleRepeatUI()
                Toast.makeText(this, R.string.shuffle_all, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSort.setOnClickListener {
            dialogHelper.showSortDialog(viewModel)
        }
    }

    private fun applySelectedColorTheme() {
        val helper = ColorThemeHelper(this)
        val currentTheme = helper.getSelectedTheme()
        themeManager.applyGlobalThemeToUI(currentTheme.bgColor, currentTheme.accentColor)
        viewModel.setThemeColor(currentTheme.accentColor)
    }

    private fun setupControlListeners() {
        binding.btnPlayPause.setOnClickListener { playbackManager.togglePlayPause() }
        binding.btnNext.setOnClickListener { playbackManager.playNext() }
        binding.btnPrevious.setOnClickListener { playbackManager.playPrevious() }
        
        val p = binding.playerLayout
        p.controlsLayout.btnPlayPause.setOnClickListener { playbackManager.togglePlayPause() }
        p.controlsLayout.btnNext.setOnClickListener { playbackManager.playNext() }
        p.controlsLayout.btnPrev.setOnClickListener { playbackManager.playPrevious() }
        
        p.skinCircular.seekBar.setOnSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(seekBar: CircularSeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seekTo(progress)
                    p.skinCircular.tvCurrentTime.text = AudioHelper.formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {}
        })

        p.topNavBar.btnBack.setOnClickListener { sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }
        
        p.topNavBar.btnTheme.setOnClickListener {
            startActivity(Intent(this, ThemeActivity::class.java))
        }

        p.infoSection.btnAdd.setOnClickListener {
            viewModel.currentSong.value?.let { song ->
                Toast.makeText(this, "تمت الإضافة: ${song.title}", Toast.LENGTH_SHORT).show()
            }
        }
        p.infoSection.btnFav.setOnClickListener {
            viewModel.currentSong.value?.let { song ->
                viewModel.toggleFavorite(song)
                updateFavoriteIcon(song.isFavorite)
            }
        }

        p.extraControls.btnList.setOnClickListener {
            binding.viewPager.currentItem = 0
            sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
        p.extraControls.btnTimer.setOnClickListener { showSleepTimerDialog() }
        p.extraControls.btnVolume.setOnClickListener {
            val am = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
        }
        p.extraControls.btnEqualizer.setOnClickListener {
            try {
                val intent = Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, musicService?.audioSessionId ?: 0)
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
                }
                @Suppress("DEPRECATION")
                startActivityForResult(intent, 1001)
            } catch (e: Exception) {
                Toast.makeText(this, "الموازن غير مدعوم", Toast.LENGTH_SHORT).show()
            }
        }

        p.controlsLayout.btnShuffle.setOnClickListener {
            musicService?.let {
                it.isShuffle = !it.isShuffle
                updateShuffleRepeatUI()
            }
        }
        p.controlsLayout.btnRepeat.setOnClickListener {
            musicService?.let {
                it.repeatMode = (it.repeatMode + 1) % 3 
                updateShuffleRepeatUI()
            }
        }
    }

    private fun updateShuffleRepeatUI() {
        val service = musicService ?: return
        val p = binding.playerLayout
        val activeColor = Color.parseColor("#FF1744")
        
        p.controlsLayout.btnShuffle.setColorFilter(if (service.isShuffle) activeColor else Color.WHITE)
        
        if (service.repeatMode == 2) {
            p.controlsLayout.btnRepeat.setImageResource(R.drawable.ic_repeat_one)
            p.controlsLayout.btnRepeat.setColorFilter(activeColor)
        } else {
            p.controlsLayout.btnRepeat.setImageResource(R.drawable.ic_repeat)
            p.controlsLayout.btnRepeat.setColorFilter(if (service.repeatMode == 1) activeColor else Color.WHITE)
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        val icon = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        val color = if (isFavorite) Color.parseColor("#FF1744") else Color.WHITE
        binding.playerLayout.infoSection.btnFav.setImageResource(icon)
        binding.playerLayout.infoSection.btnFav.setColorFilter(color)
    }

    private fun setupSystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContainer) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.setPadding(0, systemBars.top, 0, 0)
            binding.playerLayout.topNavBar.root.setPadding(0, systemBars.top, 0, 0)
            binding.miniPlayerCard.translationY = -systemBars.bottom.toFloat()
            insets
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) loadSongsFromDevice()
    }

    fun loadSongsFromDevice() {
        lifecycleScope.launch(Dispatchers.IO) {
            val songs = MusicLibrary.loadSongsFromDevice(this@MainActivity)
            withContext(Dispatchers.Main) { viewModel.updateMusicData(songs) }
        }
    }

    private fun loadCustomBackground(uriString: String? = null) {
        val path = uriString ?: preferences.customBackgroundUri ?: return
        binding.ivCustomBackground.visibility = View.VISIBLE
        try {
            if (path.contains(",")) {
                val colors = path.split(",").map { Color.parseColor(it) }.toIntArray()
                binding.mainContainer.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors)
                binding.ivCustomBackground.visibility = View.GONE
            } else if (path.startsWith("#")) {
                binding.mainContainer.setBackgroundColor(Color.parseColor(path))
                binding.ivCustomBackground.visibility = View.GONE
            } else {
                Glide.with(this).load(Uri.parse(path)).centerCrop().into(binding.ivCustomBackground)
            }
        } catch (e: Exception) {}
    }

    fun showSongOptions(song: Song, view: View) {
        dialogHelper.showSongOptions(song, viewModel, coverPickerLauncher, deleteLauncher)
    }

    fun updateActionMode(count: Int) {
        binding.secondaryToolbar.visibility = if (count > 0) View.VISIBLE else View.GONE
        binding.tvSelectionCount.text = count.toString()
    }

    fun showSleepTimerDialog() {
        dialogHelper.showSleepTimerDialog(musicService)
    }

    private fun updateUIForSong(song: Song) {
        if (song.id == lastLoadedSongId) return
        lastLoadedSongId = song.id
        binding.tvMiniTitle.text = song.title
        binding.tvMiniArtist.text = song.artist
        binding.playerLayout.infoSection.tvMainTitle.text = song.title
        binding.playerLayout.infoSection.tvMainArtist.text = song.artist
        
        musicService?.playSong(song.uri, song.title, song.artist, null, viewModel.currentIndex, viewModel.songs.value?.size ?: 0)
        
        val artUri = AudioHelper.getAlbumArtUri(song.albumId)
        Glide.with(this).asBitmap().load(artUri).placeholder(R.drawable.ic_music_note).into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
            override fun onResourceReady(r: Bitmap, t: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                binding.imgVinylMini.setImageBitmap(r)
                skinManager.updateArtForSkins(r)
            }
            override fun onLoadCleared(p: android.graphics.drawable.Drawable?) {}
        })
    }

    override fun onProgressUpdate(position: Int, duration: Int) {
        runOnUiThread {
            motionManager.updateSyncData(position, duration, musicService?.isPlaying() ?: false)
            if (duration > 0) {
                binding.seekBar.progress = (position * 100 / duration)
                val p = binding.playerLayout
                p.skinCircular.seekBar.max = duration
                p.skinCircular.seekBar.progress = position
                p.skinCircular.tvCurrentTime.text = AudioHelper.formatTime(position)
                p.skinCircular.tvTotalTime.text = AudioHelper.formatTime(duration)
                updateDotPosition(p.skinCircular.neonDot, p.skinCircular.albumCard, position, duration)
            }
        }
    }

    private fun updateDotPosition(dot: View, anchor: View, progress: Int, max: Int) {
        if (max > 0) {
            val angle = (progress.toFloat() / max * 360f + 270f) % 360f
            val params = dot.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params?.let { 
                it.circleAngle = angle
                dot.layoutParams = it 
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        musicService = (service as MusicPlayerService.MusicBinder).getService()
        musicService?.registerCallback(this)
        isBound = true
        updateShuffleRepeatUI()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        isBound = false
        musicService = null
    }

    private fun bindMusicService() {
        val intent = Intent(this, MusicPlayerService::class.java)
        bindService(intent, this, Context.BIND_AUTO_CREATE)
        startService(intent)
    }

    override fun onPlaybackStarted() { updatePlaybackIcons(true) }
    override fun onPlaybackPaused() { updatePlaybackIcons(false) }
    override fun onPlaybackResumed() { updatePlaybackIcons(true) }
    override fun onPlaybackCompleted() { playbackManager.playNext() }
    override fun onArtUpdated(bitmap: Bitmap) { skinManager.updateArtForSkins(bitmap) }
    override fun onVisualizerData(data: ByteArray) { 
        runOnUiThread { (binding.playerLayout.visualizer as? VisualizerView)?.updateVisualizer(data) } 
    }
    override fun onNextTrack() { playbackManager.playNext() }
    override fun onPreviousTrack() { playbackManager.playPrevious() }

    private fun updatePlaybackIcons(playing: Boolean) {
        val icon = if (playing) R.drawable.ic_pause else R.drawable.ic_play
        binding.btnPlayPause.setImageResource(icon)
        binding.playerLayout.controlsLayout.btnPlayPause.setImageResource(icon)
    }

    override fun onDestroy() { 
        super.onDestroy()
        motionManager.stop()
        if (isBound) {
            musicService?.unregisterCallback()
            unbindService(this)
        }
    }
}
