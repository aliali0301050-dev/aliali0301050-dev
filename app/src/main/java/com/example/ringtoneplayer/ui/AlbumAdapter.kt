package com.example.ringtoneplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ringtoneplayer.R
import com.example.ringtoneplayer.models.Album
import com.example.ringtoneplayer.utils.AudioHelper
import com.google.android.material.imageview.ShapeableImageView

class AlbumAdapter(
    private var list: List<Album>,
    private val onAlbumClick: (Album) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumVH>() {

    inner class AlbumVH(v: View) : RecyclerView.ViewHolder(v) {
        val cover: ShapeableImageView = v.findViewById(R.id.ivAlbumCover)
        val name: TextView = v.findViewById(R.id.tvAlbumName)
        val artist: TextView = v.findViewById(R.id.tvAlbumArtist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return AlbumVH(v)
    }

    override fun onBindViewHolder(holder: AlbumVH, position: Int) {
        val album = list[position]
        holder.name.text = album.name
        holder.artist.text = album.artist
        
        // جلب صورة الألبوم باستخدام Glide
        val artUri = AudioHelper.getAlbumArtUri(album.albumId)
        Glide.with(holder.itemView.context)
            .load(artUri)
            .placeholder(R.drawable.ic_music_note)
            .error(R.drawable.ic_music_note)
            .into(holder.cover)

        holder.itemView.setOnClickListener { onAlbumClick(album) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Album>) {
        list = newList
        notifyDataSetChanged()
    }
}
