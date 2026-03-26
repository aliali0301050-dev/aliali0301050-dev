package com.example.ringtoneplayer.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.ringtoneplayer.R

class BackgroundAdapter(
    private var items: List<BackgroundItem>,
    private var selectedValue: String?,
    private val onSelected: (BackgroundItem) -> Unit
) : RecyclerView.Adapter<BackgroundAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_background, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        // 1. Reset Common UI Elements
        holder.ivCheck.visibility = if (item.value == selectedValue) View.VISIBLE else View.GONE
        holder.checkContainer.strokeColor = if (item.value == selectedValue) Color.parseColor("#FFAB00") else Color.WHITE
        holder.ivAddIcon.visibility = if (item.type == BackgroundItem.Type.ADD_BUTTON) View.VISIBLE else View.GONE
        holder.checkContainer.visibility = if (item.type == BackgroundItem.Type.ADD_BUTTON) View.GONE else View.VISIBLE

        // 2. Fix Recycling Issues (Clear previous state)
        Glide.with(holder.itemView.context).clear(holder.preview)
        holder.preview.background = null
        holder.preview.setImageDrawable(null)

        when (item.type) {
            BackgroundItem.Type.COLOR -> {
                try {
                    holder.preview.setBackgroundColor(Color.parseColor(item.value.trim()))
                } catch (e: Exception) {
                    holder.preview.setBackgroundColor(Color.DKGRAY) // Fallback
                }
            }
            BackgroundItem.Type.GRADIENT -> {
                try {
                    val colorStrings = item.value.split(",")
                    if (colorStrings.size >= 2) {
                        val colors = colorStrings.map { Color.parseColor(it.trim()) }.toIntArray()
                        val gd = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors)
                        gd.cornerRadius = 12f * holder.itemView.resources.displayMetrics.density
                        holder.preview.background = gd
                    } else {
                        holder.preview.setBackgroundColor(Color.GRAY)
                    }
                } catch (e: Exception) {
                    holder.preview.setBackgroundColor(Color.DKGRAY) // Fallback
                }
            }
            BackgroundItem.Type.IMAGE -> {
                holder.preview.setBackgroundColor(Color.TRANSPARENT)
                
                // [OPTIMIZED LOAD] تقليل استهلاك الرام وتفعيل الكاش
                Glide.with(holder.itemView.context)
                    .load(item.value)
                    .apply(RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(200, 320) // تحميل نسخة مصغرة للمعاينة لتوفير الذاكرة
                        .placeholder(R.drawable.circle_background)
                        .error(R.drawable.ic_music_note)
                        .centerCrop()
                    )
                    .into(holder.preview)
            }
            BackgroundItem.Type.ADD_BUTTON -> {
                holder.preview.setBackgroundColor(Color.parseColor("#1AFFFFFF"))
            }
        }

        holder.itemView.setOnClickListener { onSelected(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val preview: ImageView = v.findViewById(R.id.previewView)
        val ivCheck: ImageView = v.findViewById(R.id.ivCheck)
        val checkContainer: com.google.android.material.card.MaterialCardView = v.findViewById(R.id.checkContainer)
        val ivAddIcon: ImageView = v.findViewById(R.id.ivAddIcon)
    }
}

data class BackgroundItem(val value: String, val type: Type) {
    enum class Type { COLOR, GRADIENT, IMAGE, ADD_BUTTON }
}
