package com.example.ringtoneplayer.ui

import com.example.ringtoneplayer.databinding.ActivityMainBinding
import com.example.ringtoneplayer.services.MusicPlayerService

class PlaybackManager(
    private val binding: ActivityMainBinding,
    private val viewModel: MainViewModel,
    private val musicService: () -> MusicPlayerService?
) {
    private var lastActionTime = 0L

    fun playNext() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastActionTime < 800) return
        lastActionTime = currentTime

        val nextIndex = viewModel.getNextIndex()
        if (nextIndex != -1 && nextIndex != viewModel.currentIndex) {
            viewModel.setCurrentSong(nextIndex)
        }
    }

    fun playPrevious() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastActionTime < 800) return
        lastActionTime = currentTime

        val prevIndex = viewModel.getPrevIndex()
        if (prevIndex != -1) {
            viewModel.setCurrentSong(prevIndex)
        }
    }

    fun togglePlayPause() {
        musicService()?.let {
            if (it.isPlaying()) it.pauseMusic() else it.resumeMusic()
        }
    }

    fun updatePlaybackUI(isPlaying: Boolean) {
        val icon = if (isPlaying) com.example.ringtoneplayer.R.drawable.ic_pause else com.example.ringtoneplayer.R.drawable.ic_play
        binding.btnPlayPause.setImageResource(icon)
        binding.playerLayout.controlsLayout.btnPlayPause.setImageResource(icon)
    }
}
