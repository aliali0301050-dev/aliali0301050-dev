package com.example.ringtoneplayer.ui

import android.content.ContentUris
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ringtoneplayer.R
import com.example.ringtoneplayer.models.Song
import com.example.ringtoneplayer.utils.AudioHelper

class SongAdapter(
    private var songs: List<Song>,
    private val listener: OnSongClickListener
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    val selectedPositions = mutableSetOf<Int>()
    var isSelectionMode = false
    var currentPlayingId: Long = -1
    private var accentColor: Int = Color.parseColor("#FF1744")

    interface OnSongClickListener {
        fun onSongClick(song: Song, position: Int)
        fun onSongLongClick(song: Song, position: Int)
        fun onMoreClick(song: Song, view: View)
        fun onSelectionChanged(selectedCount: Int)
    }

    fun setAccentColor(color: Int) {
        this.accentColor = color
        notifyDataSetChanged()
    }

    inner class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvSongTitle)
        val artist: TextView = view.findViewById(R.id.tvSongArtist)
        val duration: TextView = view.findViewById(R.id.tvDuration)
        val albumArt: ImageView = view.findViewById(R.id.ivSongIcon)
        val moreBtn: ImageButton = view.findViewById(R.id.btnMore)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)
        val root: View = view.findViewById(R.id.songItemCard)
        val musicIndicator: MusicIndicatorView = view.findViewById(R.id.musicIndicator)

        fun bind(song: Song, position: Int) {
            title.text = song.title
            artist.text = song.artist
            duration.text = AudioHelper.formatTime(song.duration.toInt())

            val artUri = AudioHelper.getAlbumArtUri(song.albumId)

            Glide.with(itemView.context)
                .load(artUri)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .centerCrop()
                .into(albumArt)

            checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            checkBox.isChecked = selectedPositions.contains(position)
            moreBtn.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

            // [FIXED] تحسين منطق ظهور العلامة وتطبيق اللون
            if (song.id == currentPlayingId) {
                musicIndicator.visibility = View.VISIBLE
                musicIndicator.setPlaying(true)
                musicIndicator.setBarColor(accentColor)
                title.setTextColor(accentColor)
            } else {
                musicIndicator.visibility = View.GONE
                musicIndicator.setPlaying(false)
                title.setTextColor(Color.WHITE)
            }

            if (selectedPositions.contains(position)) {
                root.setBackgroundColor(Color.parseColor("#4DFFFFFF"))
            } else {
                root.setBackgroundColor(Color.TRANSPARENT)
            }

            itemView.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(position)
                } else {
                    listener.onSongClick(song, position)
                }
            }

            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    toggleSelection(position)
                    listener.onSongLongClick(song, position)
                }
                true
            }

            moreBtn.setOnClickListener { listener.onMoreClick(song, it) }
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
        }
        notifyItemChanged(position)
        if (selectedPositions.isEmpty()) {
            isSelectionMode = false
            notifyDataSetChanged()
        }
        listener.onSelectionChanged(selectedPositions.size)
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedPositions.clear()
        notifyDataSetChanged()
        listener.onSelectionChanged(0)
    }

    fun updatePlayingId(id: Long) {
        val oldId = currentPlayingId
        currentPlayingId = id
        songs.forEachIndexed { index, song ->
            if (song.id == oldId || song.id == id) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) = holder.bind(songs[position], position)

    override fun getItemCount(): Int = songs.size

    fun updateData(newSongs: List<Song>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = songs.size
            override fun getNewListSize(): Int = newSongs.size
            override fun areItemsTheSame(o: Int, n: Int) = songs[o].id == newSongs[n].id
            override fun areContentsTheSame(o: Int, n: Int) = songs[o] == newSongs[n]
        })
        songs = newSongs
        diffResult.dispatchUpdatesTo(this)
    }
}
