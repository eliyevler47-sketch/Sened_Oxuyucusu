package com.example.universalviewer.ui

import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.universalviewer.DocumentItem
import com.example.universalviewer.PreferenceManager
import com.example.universalviewer.databinding.FragmentHomeBinding
import com.example.universalviewer.databinding.ItemCategoryBinding
import com.example.universalviewer.databinding.ItemDocumentBinding
import com.example.universalviewer.utils.DocumentRouter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager
    private var isBookmarksTab = false

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) { DocumentRouter.routeFile(requireContext(), uri) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PreferenceManager(requireContext())
        
        // Tab klikləri üçün (fragment_home.xml-dəki id-lərə uyğun olaraq dəyişdirin)
        binding.root.findViewWithTag<TextView>("tab_recent")?.setOnClickListener {
            isBookmarksTab = false
            setupRecentFiles()
        }
        binding.root.findViewWithTag<TextView>("tab_bookmarks")?.setOnClickListener {
            isBookmarksTab = true
            setupRecentFiles()
        }
    }

    override fun onResume() {
        super.onResume()
        setupCategories()
        setupRecentFiles()
    }

    private fun setupCategories() {
        val docs = prefManager.getDocumentList()
        val categories = listOf(
            CategoryItem("All", docs.size, android.R.drawable.ic_menu_sort_by_size, "#2979FF"),
            CategoryItem("PDF", docs.count { it.fileName.endsWith(".pdf", true) }, android.R.drawable.ic_menu_save, "#FF5722"),
            CategoryItem("Word", docs.count { it.fileName.endsWith(".docx", true) }, android.R.drawable.ic_menu_edit, "#2979FF"),
            CategoryItem("Excel", docs.count { it.fileName.endsWith(".xlsx", true) }, android.R.drawable.ic_menu_agenda, "#00C853"),
            CategoryItem("PPT", docs.count { it.fileName.endsWith(".pptx", true) }, android.R.drawable.ic_menu_slideshow, "#FF5722"),
            CategoryItem("TXT", docs.count { it.fileName.endsWith(".txt", true) }, android.R.drawable.ic_menu_info_details, "#A0A0A0"),
            CategoryItem("Image", docs.count { it.fileName.endsWith(".jpg", true) || it.fileName.endsWith(".png", true) }, android.R.drawable.ic_menu_gallery, "#FFCA28")
        )

        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvCategories.adapter = CategoryAdapter(categories) { categoryName ->
            val mimeTypes = when(categoryName) {
                "PDF" -> arrayOf("application/pdf")
                "Word" -> arrayOf("application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                "Excel" -> arrayOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                "PPT" -> arrayOf("application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
                "TXT" -> arrayOf("text/plain", "text/html")
                "Image" -> arrayOf("image/*")
                else -> arrayOf("*/*")
            }
            filePickerLauncher.launch(mimeTypes)
        }
    }

    private fun setupRecentFiles() {
        val list = if (isBookmarksTab) prefManager.getBookmarkedDocuments() else prefManager.getSortedDocumentList().take(5)
        if (list.isEmpty()) {
            binding.rvRecentFiles.visibility = View.GONE
        } else {
            binding.rvRecentFiles.visibility = View.VISIBLE
            binding.rvRecentFiles.layoutManager = LinearLayoutManager(requireContext())
            binding.rvRecentFiles.adapter = RecentMiniAdapter(list) { uri ->
                DocumentRouter.routeFile(requireContext(), Uri.parse(uri))
            }
        }
    }

    data class CategoryItem(val name: String, val count: Int, val iconRes: Int, val colorHex: String)

    inner class CategoryAdapter(private val items: List<CategoryItem>, private val onClick: (String) -> Unit) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvCategoryName.text = item.name
            holder.binding.tvCategoryCount.text = "${item.count}"
            holder.binding.ivCategoryIcon.setImageResource(item.iconRes)
            holder.binding.ivCategoryIcon.setColorFilter(Color.parseColor(item.colorHex), PorterDuff.Mode.SRC_IN)
            holder.binding.root.setOnClickListener { onClick(item.name) }
        }
        override fun getItemCount() = items.size
    }

    inner class RecentMiniAdapter(private val items: List<DocumentItem>, private val onClick: (String) -> Unit) : RecyclerView.Adapter<RecentMiniAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemDocumentBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvFileName.text = item.fileName
            holder.binding.tvLastOpened.text = if (item.isFavorite) "Əlfəcinləndi" else "Son açılan"
            holder.binding.btnFavorite.visibility = View.GONE
            holder.binding.btnPin.visibility = View.GONE
            holder.binding.root.setOnClickListener { onClick(item.uri) }
        }
        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
