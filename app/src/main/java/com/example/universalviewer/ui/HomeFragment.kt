package com.example.universalviewer.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import java.io.FileOutputStream

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) { DocumentRouter.routeFile(requireContext(), uri) }
    }

    // ÇOXLU ŞƏKİL SEÇMƏK ÜÇÜN
    private val multipleImagesPicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            createPdfFromImages(uris)
        }
    }

    // PDF-i SAXLAMAQ ÜÇÜN YER SEÇMƏK
    private var pdfToSave: PdfDocument? = null
    private val savePdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        if (uri != null && pdfToSave != null) {
            try {
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfToSave?.writeTo(outputStream)
                }
                pdfToSave?.close()
                Toast.makeText(requireContext(), "PDF uğurla yadda saxlanıldı!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Yadda saxlama xətası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            pdfToSave?.close()
        }
        pdfToSave = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PreferenceManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        setupCategories()
        setupRecentFiles()
    }

    private fun setupCategories() {
        val docs = prefManager.getDocumentList()
        val pdfCount = docs.count { it.fileName.endsWith(".pdf", true) }
        val wordCount = docs.count { it.fileName.endsWith(".docx", true) || it.fileName.endsWith(".doc", true) }
        val excelCount = docs.count { it.fileName.endsWith(".xlsx", true) || it.fileName.endsWith(".xls", true) }
        val pptCount = docs.count { it.fileName.endsWith(".pptx", true) || it.fileName.endsWith(".ppt", true) }
        val txtCount = docs.count { it.fileName.endsWith(".txt", true) || it.fileName.endsWith(".html", true) }
        val imgCount = docs.count { it.fileName.endsWith(".jpg", true) || it.fileName.endsWith(".png", true) }

        val categories = listOf(
            CategoryItem("All", docs.size, android.R.drawable.ic_menu_sort_by_size, "#2979FF"),
            CategoryItem("PDF", pdfCount, android.R.drawable.ic_menu_save, "#FF5722"),
            CategoryItem("Word", wordCount, android.R.drawable.ic_menu_edit, "#2979FF"),
            CategoryItem("Excel", excelCount, android.R.drawable.ic_menu_agenda, "#00C853"),
            CategoryItem("PPT", pptCount, android.R.drawable.ic_menu_slideshow, "#FF5722"),
            CategoryItem("TXT", txtCount, android.R.drawable.ic_menu_info_details, "#A0A0A0"),
            CategoryItem("Image", imgCount, android.R.drawable.ic_menu_gallery, "#FFCA28")
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
        val recentDocs = prefManager.getSortedDocumentList().take(10)
        binding.rvRecentFiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentFiles.adapter = RecentMiniAdapter(recentDocs) { uri ->
            DocumentRouter.routeFile(requireContext(), Uri.parse(uri))
        }
    }

    private fun createPdfFromImages(uris: List<Uri>) {
        try {
            val pdfDocument = PdfDocument()
            var pageNumber = 1
            for (uri in uris) {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                pageNumber++
            }
            pdfToSave = pdfDocument
            // İstifadəçiyə harada saxlamaq istədiyini soruşuruq
            savePdfLauncher.launch("Birləşdirilmis_Shekiller.pdf")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Xəta: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    data class CategoryItem(val name: String, val count: Int, val iconRes: Int, val colorHex: String)

    inner class CategoryAdapter(
        private val items: List<CategoryItem>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

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

    inner class RecentMiniAdapter(
        private val items: List<DocumentItem>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<RecentMiniAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemDocumentBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvFileName.text = item.fileName
            holder.binding.tvLastOpened.text = "Son açılan"
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
