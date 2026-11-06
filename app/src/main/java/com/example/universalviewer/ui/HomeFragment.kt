package com.example.universalviewer.ui

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.universalviewer.PreferenceManager
import com.example.universalviewer.databinding.FragmentHomeBinding
import com.example.universalviewer.databinding.ItemCategoryBinding
import com.example.universalviewer.databinding.ItemToolBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PreferenceManager(requireContext())
        
        setupCategories()
        setupTools()
    }

    private fun setupCategories() {
        val docs = prefManager.getDocuments()
        
        val pdfCount = docs.count { it.fileName.endsWith(".pdf", true) }
        val wordCount = docs.count { it.fileName.endsWith(".docx", true) }
        val excelCount = docs.count { it.fileName.endsWith(".xlsx", true) }
        val pptCount = docs.count { it.fileName.endsWith(".pptx", true) }
        val txtCount = docs.count { it.fileName.endsWith(".txt", true) }

        val categories = listOf(
            CategoryItem("All", docs.size, android.R.drawable.ic_menu_sort_by_size, "#2979FF"),
            CategoryItem("PDF", pdfCount, android.R.drawable.ic_menu_save, "#FF5722"),
            CategoryItem("Word", wordCount, android.R.drawable.ic_menu_edit, "#2979FF"),
            CategoryItem("Excel", excelCount, android.R.drawable.ic_menu_agenda, "#00C853"),
            CategoryItem("PPT", pptCount, android.R.drawable.ic_menu_slideshow, "#FF5722"),
            CategoryItem("TXT", txtCount, android.R.drawable.ic_menu_info_details, "#A0A0A0")
        )

        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCategories.adapter = CategoryAdapter(categories)
    }

    private fun setupTools() {
        val tools = listOf(
            ToolItem("Scan", android.R.drawable.ic_menu_camera, "#00C853"),
            ToolItem("Create", android.R.drawable.ic_menu_add, "#FF5722"),
            ToolItem("Convert", android.R.drawable.ic_menu_send, "#2979FF"),
            ToolItem("Share", android.R.drawable.ic_menu_share, "#FF5722")
        )
        binding.rvTools.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvTools.adapter = ToolAdapter(tools)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class CategoryItem(val name: String, val count: Int, val iconRes: Int, val colorHex: String)
    data class ToolItem(val name: String, val iconRes: Int, val colorHex: String)

    inner class CategoryAdapter(private val items: List<CategoryItem>) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvCategoryName.text = item.name
            holder.binding.tvCategoryCount.text = "${item.count} files"
            holder.binding.ivCategoryIcon.setImageResource(item.iconRes)
            holder.binding.ivCategoryIcon.setColorFilter(Color.parseColor(item.colorHex), PorterDuff.Mode.SRC_IN)
        }

        override fun getItemCount() = items.size
    }

    inner class ToolAdapter(private val items: List<ToolItem>) : RecyclerView.Adapter<ToolAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemToolBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemToolBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvToolName.text = item.name
            holder.binding.ivToolIcon.setImageResource(item.iconRes)
            holder.binding.ivToolIcon.setColorFilter(Color.parseColor(item.colorHex), PorterDuff.Mode.SRC_IN)
        }

        override fun getItemCount() = items.size
    }
}
