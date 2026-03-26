package com.example.ringtoneplayer.ui

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ringtoneplayer.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class Folder(val name: String, val path: String, val songCount: Int)

class MainViewModel : ViewModel() {
    private val _allSongs = MutableLiveData<List<Song>>(emptyList())
    private val _songs = MutableLiveData<List<Song>>(emptyList())
    val songs: LiveData<List<Song>> = _songs

    private val _albums = MutableLiveData<List<Album>>(emptyList())
    val albums: LiveData<List<Album>> = _albums

    private val _artists = MutableLiveData<List<Artist>>(emptyList())
    val artists: LiveData<List<Artist>> = _artists

    private val _folders = MutableLiveData<List<Folder>>(emptyList())
    val folders: LiveData<List<Folder>> = _folders

    private val _currentSong = MutableLiveData<Song?>()
    val currentSong: LiveData<Song?> = _currentSong

    private val _themeColor = MutableLiveData<Int>()
    val themeColor: LiveData<Int> = _themeColor

    private val _customBackgroundUri = MutableLiveData<String?>()
    val customBackgroundUri: LiveData<String?> = _customBackgroundUri

    private val _isAlbumGridEnabled = MutableLiveData<Boolean>(false)
    val isAlbumGridEnabled: LiveData<Boolean> = _isAlbumGridEnabled

    var currentIndex = -1
    var currentSortType = "date"
    var isAscending = false
    var minDurationSeconds = 0
    var searchQuery = ""
    
    var folderFilter: String? = null
    var albumIdFilter: Long? = null
    var artistNameFilter: String? = null
    var genreFilter: String? = null

    fun setThemeColor(color: Int) {
        _themeColor.value = color
    }

    fun setCustomBackgroundUri(uri: String?) {
        _customBackgroundUri.value = uri
    }

    fun setAlbumGridEnabled(enabled: Boolean) {
        _isAlbumGridEnabled.value = enabled
    }

    fun updateMusicData(songList: List<Song>) {
        viewModelScope.launch(Dispatchers.Default) {
            val albumList = if (songList.isNotEmpty()) {
                songList.groupBy { it.albumId }.map { (id, songs) ->
                    Album(songs[0].album, songs[0].artist, id, songs)
                }
            } else emptyList()

            val artistList = if (songList.isNotEmpty()) {
                songList.groupBy { it.artist }.map { (name, songs) ->
                    Artist(name, songs)
                }
            } else emptyList()

            val folderList = if (songList.isNotEmpty()) {
                songList.groupBy { File(it.path).parent ?: "Unknown" }.map { (path, songs) ->
                    Folder(File(path).name, path, songs.size)
                }
            } else emptyList()

            withContext(Dispatchers.Main) {
                _allSongs.value = songList
                _albums.value = albumList
                _artists.value = artistList
                _folders.value = folderList
                applyFilterAndSort()
            }
        }
    }

    fun setCurrentSong(index: Int) {
        val list = _songs.value ?: return
        if (index in list.indices) {
            currentIndex = index
            _currentSong.value = list[index]
        }
    }

    fun toggleFavorite(song: Song) {
        song.isFavorite = !song.isFavorite
        _currentSong.value = song
    }

    fun getNextIndex(): Int {
        val list = _songs.value ?: return -1
        if (list.isEmpty()) return -1
        return (currentIndex + 1) % list.size
    }

    fun getPrevIndex(): Int {
        val list = _songs.value ?: return -1
        if (list.isEmpty()) return -1
        return if (currentIndex > 0) currentIndex - 1 else list.size - 1
    }

    fun filterByFolder(path: String?) {
        clearFilters()
        folderFilter = path
        applyFilterAndSort()
    }

    fun filterByAlbum(id: Long?) {
        clearFilters()
        albumIdFilter = id
        applyFilterAndSort()
    }

    fun filterByArtist(name: String?) {
        clearFilters()
        artistNameFilter = name
        applyFilterAndSort()
    }

    fun filterByGenre(name: String?) {
        clearFilters()
        genreFilter = name
        applyFilterAndSort()
    }

    fun clearFilters() {
        folderFilter = null
        albumIdFilter = null
        artistNameFilter = null
        genreFilter = null
        searchQuery = ""
        applyFilterAndSort()
    }

    fun applyFilterAndSort() {
        val list = _allSongs.value ?: return
        
        viewModelScope.launch(Dispatchers.Default) {
            var filtered = list.filter { it.duration >= minDurationSeconds * 1000 }
            
            if (folderFilter != null) {
                filtered = filtered.filter { File(it.path).parent == folderFilter }
            } else if (albumIdFilter != null) {
                filtered = filtered.filter { it.albumId == albumIdFilter }
            } else if (artistNameFilter != null) {
                filtered = filtered.filter { it.artist == artistNameFilter }
            } else if (genreFilter != null) {
                filtered = filtered.filter { it.album == genreFilter }
            }
            
            if (searchQuery.isNotEmpty()) {
                filtered = filtered.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    it.artist.contains(searchQuery, ignoreCase = true) 
                }
            }
            
            val sorted = when (currentSortType) {
                "title" -> if (isAscending) filtered.sortedBy { it.title.lowercase() } else filtered.sortedByDescending { it.title.lowercase() }
                "artist" -> if (isAscending) filtered.sortedBy { it.artist.lowercase() } else filtered.sortedByDescending { it.artist.lowercase() }
                "date" -> if (isAscending) filtered.sortedBy { it.id } else filtered.sortedByDescending { it.id }
                else -> filtered
            }
            
            withContext(Dispatchers.Main) {
                _songs.value = sorted
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        searchQuery = query
        applyFilterAndSort()
    }

    fun updateSort(type: String, ascending: Boolean) {
        currentSortType = type
        isAscending = ascending
        applyFilterAndSort()
    }

    fun setMinDuration(seconds: Int) {
        minDurationSeconds = seconds
        applyFilterAndSort()
    }

    fun getDuplicateSongsCount(): Int {
        val all = _allSongs.value ?: return 0
        val duplicates = all.groupBy { it.title.lowercase() + it.size }
            .filter { it.value.size > 1 }
        return duplicates.values.sumOf { it.size - 1 }
    }
}
