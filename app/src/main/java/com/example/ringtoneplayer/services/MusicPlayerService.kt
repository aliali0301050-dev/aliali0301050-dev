package com.example.ringtoneplayer.services

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.*
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.example.ringtoneplayer.R
import com.example.ringtoneplayer.utils.PlayerPreferences
import java.util.*

class MusicPlayerService : Service(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        const val ACTION_PLAY = "com.example.ringtoneplayer.PLAY"
        const val ACTION_PAUSE = "com.example.ringtoneplayer.PAUSE"
        const val ACTION_NEXT = "com.example.ringtoneplayer.NEXT"
        const val ACTION_PREVIOUS = "com.example.ringtoneplayer.PREVIOUS"
        const val CHANNEL_ID = "music_player_channel_v2"
        const val NOTIFICATION_ID = 101
    }

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var visualizer: Visualizer? = null
    private val binder = MusicBinder()
    private var callback: MusicPlayerCallback? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var preferences: PlayerPreferences
    private lateinit var audioManager: AudioManager

    var currentSongTitle = ""
    var currentSongArtist = ""
    private var currentArt: Bitmap? = null
    private var dominantColor = Color.WHITE

    var isShuffle = false
    var repeatMode = 0
    var currentSpeed = 1.0f
    var currentQueuePosition = 0
    var queueSize = 0

    val audioSessionId: Int get() = mediaPlayer?.audioSessionId ?: 0
    private var sleepTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { if (it.isPlaying) callback?.onProgressUpdate(it.currentPosition, it.duration) }
            handler.postDelayed(this, 16)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseMusic()
            AudioManager.AUDIOFOCUS_GAIN -> {
                // لا نستأنف التشغيل تلقائياً لضمان عدم العمل في الجيب
            }
        }
    }

    inner class MusicBinder : Binder() { fun getService(): MusicPlayerService = this@MusicPlayerService }

    interface MusicPlayerCallback {
        fun onPlaybackCompleted(); fun onPlaybackPaused(); fun onPlaybackResumed()
        fun onPlaybackStarted(); fun onProgressUpdate(position: Int, duration: Int)
        fun onNextTrack(); fun onPreviousTrack(); fun onVisualizerData(data: ByteArray)
        fun onArtUpdated(bitmap: Bitmap)
    }

    override fun onCreate() {
        super.onCreate()
        preferences = PlayerPreferences(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        isShuffle = preferences.isShuffle
        repeatMode = preferences.getInt("repeat_mode_int", 1)
        createNotificationChannel()
        setupMediaSession()
        initMediaPlayer()
        handler.post(progressRunnable)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Music controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicPlayerSession")
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { resumeMusic() }
            override fun onPause() { pauseMusic() }
            override fun onSkipToNext() { playNext() }
            override fun onSkipToPrevious() { playPrevious() }
            override fun onSeekTo(pos: Long) { seekTo(pos.toInt()) }
            // منع الاستجابة العشوائية لأزرار الوسائط في الجيب
            override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                val action = mediaButtonEvent?.action
                if (Intent.ACTION_MEDIA_BUTTON == action) {
                    // هنا يمكن إضافة منطق لمنع الضغطات العشوائية
                }
                return super.onMediaButtonEvent(mediaButtonEvent)
            }
        })
        mediaSession.isActive = true
    }

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
                setOnCompletionListener { 
                    if (repeatMode == 2) {
                        seekTo(0)
                        start()
                    } else {
                        playNext() 
                    }
                }
            }
        }
    }

    fun playSong(uri: Uri, title: String, artist: String, albumArt: Bitmap?, position: Int = 0, total: Int = 0) {
        currentSongTitle = title; currentSongArtist = artist; currentArt = albumArt
        currentQueuePosition = position; queueSize = total
        
        val result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return

        mediaPlayer?.apply {
            try {
                reset()
                setDataSource(applicationContext, uri)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    initAudioEffects(audioSessionId)
                    initVisualizer(audioSessionId)
                    updateMetadata()
                    callback?.onPlaybackStarted()
                    showNotification()
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                }
            } catch (e: Exception) {}
        }
    }

    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                callback?.onPlaybackPaused()
                showNotification()
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
        } catch (e: Exception) {}
    }

    fun resumeMusic() {
        try {
            val result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mediaPlayer?.start()
                callback?.onPlaybackResumed()
                showNotification()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            }
        } catch (e: Exception) {}
    }

    fun playNext() { callback?.onNextTrack() }
    fun playPrevious() { callback?.onPreviousTrack() }
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    fun seekTo(pos: Int) { mediaPlayer?.seekTo(pos) }

    fun registerCallback(cb: MusicPlayerCallback) { this.callback = cb }
    fun unregisterCallback() { this.callback = null }

    fun updateCurrentArt(bitmap: Bitmap) {
        currentArt = bitmap
        callback?.onArtUpdated(bitmap)
        updateMetadata()
        showNotification()
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimer?.cancel()
        if (minutes > 0) {
            sleepTimer = object : CountDownTimer((minutes * 60 * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() { pauseMusic() }
            }.start()
        }
    }

    fun getEqualizer() = equalizer
    fun getBassBoost() = bassBoost
    fun getVirtualizer() = virtualizer

    fun updateEqualizerBand(band: Short, level: Short) { equalizer?.setBandLevel(band, level) }
    fun updateBassBoost(strength: Short) { bassBoost?.setStrength(strength) }
    fun updateVirtualizer(strength: Short) { virtualizer?.setStrength(strength) }

    private fun initAudioEffects(sessionId: Int) {
        try {
            if (equalizer == null) equalizer = Equalizer(0, sessionId).apply { enabled = true }
            if (bassBoost == null) bassBoost = BassBoost(0, sessionId).apply { enabled = true }
            if (virtualizer == null) virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {}
    }

    private fun initVisualizer(sessionId: Int) {
        try {
            visualizer?.release()
            visualizer = Visualizer(sessionId).apply {
                captureSize = 256
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, data: ByteArray?, samplingRate: Int) { data?.let { callback?.onVisualizerData(it) } }
                    override fun onFftDataCapture(v: Visualizer?, data: ByteArray?, samplingRate: Int) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                enabled = true
            }
        } catch (e: Exception) {}
    }

    private fun updateMetadata() {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSongTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSongArtist)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentArt)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer?.duration?.toLong() ?: -1L)
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun updatePlaybackState(state: Int) {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or 
                       PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                       PlaybackStateCompat.ACTION_SEEK_TO)
            .setState(state, mediaPlayer?.currentPosition?.toLong() ?: 0L, 1.0f)
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun showNotification() {
        val flag = PendingIntent.FLAG_IMMUTABLE
        
        val playPauseAction = if (isPlaying()) {
            NotificationCompat.Action(R.drawable.ic_pause, "Pause", 
                PendingIntent.getService(this, 0, Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PAUSE }, flag))
        } else {
            NotificationCompat.Action(R.drawable.ic_play, "Play", 
                PendingIntent.getService(this, 0, Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PLAY }, flag))
        }

        val nextAction = NotificationCompat.Action(R.drawable.ic_next, "Next", 
            PendingIntent.getService(this, 0, Intent(this, MusicPlayerService::class.java).apply { action = ACTION_NEXT }, flag))

        val prevAction = NotificationCompat.Action(R.drawable.ic_previous, "Previous", 
            PendingIntent.getService(this, 0, Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PREVIOUS }, flag))

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(currentSongTitle)
            .setContentText(currentSongArtist)
            .setLargeIcon(currentArt)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(isPlaying())
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> pauseMusic()
            ACTION_PLAY -> resumeMusic()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder
    override fun onDestroy() {
        mediaPlayer?.release()
        visualizer?.release()
        mediaSession.release()
        audioManager.abandonAudioFocus(this)
        handler.removeCallbacks(progressRunnable)
        super.onDestroy()
    }
}
