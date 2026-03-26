package com.example.ringtoneplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RingtoneAdapter(
    private val ringtones: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<RingtoneAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.nameRingtone)
        val favorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        val share: ImageButton = itemView.findViewById(R.id.btnShare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ringtone, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ringtone = ringtones[position]
        holder.name.text = ringtone
        holder.itemView.setOnClickListener { onClick(ringtone) }
    }

    override fun getItemCount() = ringtones.size
}