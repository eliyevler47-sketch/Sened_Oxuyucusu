package com.example.universalviewer.ui

import android.graphics.PorterDuff
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.universalviewer.DocumentItem
import com.example.universalviewer.R
import com.example.universalviewer.databinding.ItemDocumentBinding

class RecentAdapter(
    private val onOpen: (DocumentItem) -> Unit,
    private val onPin: (DocumentItem) -> Unit,
    private val onFavorite: (DocumentItem) -> Unit,
    private val onDelete: (DocumentItem) -> Unit
) : ListAdapter<DocumentItem, RecentAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DocumentItem>() {
            override fun areItemsTheSame(a: DocumentItem, b: DocumentItem) = a.uri == b.uri
            override fun areContentsTheSame(a: DocumentItem, b: DocumentItem) =
                a.uri == b.uri && a.fileName == b.fileName &&
                a.isPinned == b.isPinned && a.isFavorite == b.isFavorite &&
                a.lastOpened == b.lastOpened && a.timeSpentMs == b.timeSpentMs
        }
    }

    inner class VH(val binding: ItemDocumentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.binding
        val ctx = b.root.context

        b.tvFileName.text = item.fileName

        // Son açılma və Oxunma vaxtını birləşdiririk
        val relativeTime = DateUtils.getRelativeTimeSpanString(
            item.lastOpened, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        )
        
        val readTimeStr = if (item.timeSpentMs > 0) {
            val minutes = item.timeSpentMs / 60000
            val seconds = (item.timeSpentMs % 60000) / 1000
            if (minutes > 0) " • $minutes dəq oxunub" else " • $seconds san oxunub"
        } else ""

        b.tvLastOpened.text = "$relativeTime$readTimeStr"

        val (iconRes, colorRes) = iconFor(item.fileName)
        b.ivIcon.setImageResource(iconRes)
        b.ivIcon.setColorFilter(ContextCompat.getColor(ctx, colorRes), PorterDuff.Mode.SRC_IN)

        // Favori İkonu
        b.btnFavorite.setImageResource(
            if (item.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_border
        )
        b.btnFavorite.setColorFilter(
            if (item.isFavorite) 0xFFFFC107.toInt() else ContextCompat.getColor(ctx, R.color.cyber_icon_inactive),
            PorterDuff.Mode.SRC_IN
        )

        // Pin İkonu
        b.btnPin.setImageResource(
            if (item.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_border
        )
        b.btnPin.setColorFilter(
            if (item.isPinned) ContextCompat.getColor(ctx, R.color.accent_orange) else ContextCompat.getColor(ctx, R.color.cyber_icon_inactive),
            PorterDuff.Mode.SRC_IN
        )

        b.root.setOnClickListener { onOpen(item) }
        b.btnPin.setOnClickListener { onPin(item) }
        b.btnFavorite.setOnClickListener { onFavorite(item) }
        b.root.setOnLongClickListener {
            onDelete(item)
            true
        }
    }

    private fun iconFor(fileName: String): Pair<Int, Int> {
        return when {
            fileName.endsWith(".pdf", true) -> android.R.drawable.ic_menu_save to R.color.accent_orange
            fileName.endsWith(".docx", true) || fileName.endsWith(".doc", true) -> android.R.drawable.ic_menu_edit to R.color.accent_blue
            fileName.endsWith(".xlsx", true) || fileName.endsWith(".xls", true) -> android.R.drawable.ic_menu_agenda to R.color.accent_green
            fileName.endsWith(".pptx", true) || fileName.endsWith(".ppt", true) -> android.R.drawable.ic_menu_slideshow to R.color.accent_orange
            fileName.endsWith(".txt", true) -> android.R.drawable.ic_menu_info_details to R.color.text_secondary
            fileName.endsWith(".html", true) || fileName.endsWith(".htm", true) -> android.R.drawable.ic_menu_view to R.color.accent_blue
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) || fileName.endsWith(".png", true) || fileName.endsWith(".gif", true) -> android.R.drawable.ic_menu_gallery to R.color.accent_orange
            else -> android.R.drawable.ic_menu_sort_by_size to R.color.text_secondary
        }
    }
}
