package com.example.ringtoneplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ringtoneplayer.R
import com.example.ringtoneplayer.models.Artist
import com.example.ringtoneplayer.utils.AudioHelper
import com.google.android.material.imageview.ShapeableImageView

class ArtistAdapter(
    private var list: List<Artist>,
    private val onArtistClick: (Artist) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistVH>() {

    inner class ArtistVH(v: View) : RecyclerView.ViewHolder(v) {
        val image: ShapeableImageView = v.findViewById(R.id.ivArtistImage)
        val name: TextView = v.findViewById(R.id.tvArtistName)
        val count: TextView = v.findViewById(R.id.tvSongCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_artist, parent, false)
        return ArtistVH(v)
    }

    override fun onBindViewHolder(holder: ArtistVH, position: Int) {
        val artist = list[position]
        holder.name.text = artist.name
        holder.count.text = "${artist.songs.size} Songs"

        // Use album art from first song as artist image
        if (artist.songs.isNotEmpty()) {
            val artUri = AudioHelper.getAlbumArtUri(artist.songs[0].albumId)
            Glide.with(holder.itemView.context)
                .load(artUri)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(holder.image)
        }

        holder.itemView.setOnClickListener { onArtistClick(artist) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Artist>) {
        list = newList
        notifyDataSetChanged()
    }
}
