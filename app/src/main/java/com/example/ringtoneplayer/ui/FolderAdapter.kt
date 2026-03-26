package com.example.ringtoneplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ringtoneplayer.R

class FolderAdapter(
    private var list: List<Folder>,
    private val onFolderClick: (Folder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderVH>() {

    inner class FolderVH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.ivFolderIcon)
        val name: TextView = v.findViewById(R.id.tvFolderName)
        val count: TextView = v.findViewById(R.id.tvSongCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return FolderVH(v)
    }

    override fun onBindViewHolder(holder: FolderVH, position: Int) {
        val folder = list[position]
        holder.name.text = folder.name
        holder.count.text = "${folder.songCount} Songs"
        
        holder.itemView.setOnClickListener { onFolderClick(folder) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Folder>) {
        list = newList
        notifyDataSetChanged()
    }
}
