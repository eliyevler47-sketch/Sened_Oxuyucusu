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
                a.lastOpened == b.lastOpened
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

        b.tvLastOpened.text = DateUtils.getRelativeTimeSpanString(
            item.lastOpened,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )

        val (iconRes, colorRes) = iconFor(item.fileName)
        b.ivIcon.setImageResource(iconRes)
        b.ivIcon.setColorFilter(
            ContextCompat.getColor(ctx, colorRes),
            PorterDuff.Mode.SRC_IN
        )

        // Favori
        b.btnFavorite.setImageResource(android.R.drawable.btn_star)
        b.btnFavorite.setColorFilter(
            if (item.isFavorite) ContextCompat.getColor(ctx, R.color.accent_orange)
            else ContextCompat.getColor(ctx, R.color.cyber_icon_inactive),
            PorterDuff.Mode.SRC_IN
        )

        // Pin
        b.btnPin.setImageResource(android.R.drawable.ic_menu_mylocation)
        b.btnPin.alpha = if (item.isPinned) 1f else 0.5f
        b.btnPin.setColorFilter(
            if (item.isPinned) ContextCompat.getColor(ctx, R.color.accent_orange)
            else ContextCompat.getColor(ctx, R.color.cyber_icon_inactive),
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
            fileName.endsWith(".pdf", true) ->
                android.R.drawable.ic_menu_save to R.color.accent_orange
            fileName.endsWith(".docx", true) || fileName.endsWith(".doc", true) ->
                android.R.drawable.ic_menu_edit to R.color.accent_blue
            fileName.endsWith(".xlsx", true) || fileName.endsWith(".xls", true) ->
                android.R.drawable.ic_menu_agenda to R.color.accent_green
            fileName.endsWith(".pptx", true) || fileName.endsWith(".ppt", true) ->
                android.R.drawable.ic_menu_slideshow to R.color.accent_orange
            fileName.endsWith(".txt", true) ->
                android.R.drawable.ic_menu_info_details to R.color.text_secondary
            fileName.endsWith(".html", true) || fileName.endsWith(".htm", true) ->
                android.R.drawable.ic_menu_view to R.color.accent_blue
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) ||
            fileName.endsWith(".png", true) || fileName.endsWith(".gif", true) ||
            fileName.endsWith(".webp", true) ->
                android.R.drawable.ic_menu_gallery to R.color.accent_orange
            else ->
                android.R.drawable.ic_menu_sort_by_size to R.color.text_secondary
        }
    }
}
