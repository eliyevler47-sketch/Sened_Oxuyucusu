package com.example.universalviewer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.universalviewer.PreferenceManager
import com.example.universalviewer.R
import com.example.universalviewer.databinding.FragmentHomeBinding
import com.example.universalviewer.databinding.ItemCategoryBinding
import com.example.universalviewer.databinding.ItemToolBinding
import com.example.universalviewer.utils.DocumentRouter
import java.io.File
import java.io.FileOutputStream

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            DocumentRouter.routeFile(requireContext(), uri)
        }
    }

    // Şəkil seçib PDF-ə çevirmək üçün
    private val imageToPdfLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            convertImageToPdf(uri)
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            Toast.makeText(requireContext(), "Şəkil çəkildi!", Toast.LENGTH_SHORT).show()
        }
    }
    private var currentPhotoUri: Uri? = null

    // Kamera icazəsi istəmək üçün
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Kamera üçün icazə verilmədi.", Toast.LENGTH_SHORT).show()
        }
    }

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
        val docs = prefManager.getDocumentList()
        
        val pdfCount = docs.count { it.fileName.endsWith(".pdf", true) }
        val wordCount = docs.count { it.fileName.endsWith(".docx", true) || it.fileName.endsWith(".doc", true) }
        val excelCount = docs.count { it.fileName.endsWith(".xlsx", true) || it.fileName.endsWith(".xls", true) }
        val pptCount = docs.count { it.fileName.endsWith(".pptx", true) || it.fileName.endsWith(".ppt", true) }
        val txtCount = docs.count { it.fileName.endsWith(".txt", true) || it.fileName.endsWith(".html", true) }

        val categories = listOf(
            CategoryItem("All", docs.size, android.R.drawable.ic_menu_sort_by_size, "#2979FF"),
            CategoryItem("PDF", pdfCount, android.R.drawable.ic_menu_save, "#FF5722"),
            CategoryItem("Word", wordCount, android.R.drawable.ic_menu_edit, "#2979FF"),
            CategoryItem("Excel", excelCount, android.R.drawable.ic_menu_agenda, "#00C853"),
            CategoryItem("PPT", pptCount, android.R.drawable.ic_menu_slideshow, "#FF5722"),
            CategoryItem("TXT", txtCount, android.R.drawable.ic_menu_info_details, "#A0A0A0")
        )

        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCategories.adapter = CategoryAdapter(categories) { categoryName ->
            val mimeTypes = when(categoryName) {
                "PDF" -> arrayOf("application/pdf")
                "Word" -> arrayOf("application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                "Excel" -> arrayOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                "PPT" -> arrayOf("application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
                "TXT" -> arrayOf("text/plain", "text/html")
                else -> arrayOf("*/*")
            }
            filePickerLauncher.launch(mimeTypes)
        }
    }

    private fun setupTools() {
        val tools = listOf(
            ToolItem("Scan", android.R.drawable.ic_menu_camera, "#00C853"),
            ToolItem("Recent", android.R.drawable.ic_menu_recent_history, "#2979FF"),
            ToolItem("Convert", android.R.drawable.ic_menu_send, "#FF5722"),
            ToolItem("Share", android.R.drawable.ic_menu_share, "#FF5722")
        )
        binding.rvTools.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvTools.adapter = ToolAdapter(tools) { toolName ->
            when(toolName) {
                "Scan" -> {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        openCamera()
                    } else {
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                "Recent" -> findNavController().navigate(R.id.nav_recent)
                "Convert" -> imageToPdfLauncher.launch(arrayOf("image/*")) // Şəkil seçib PDF edir
                "Share" -> shareApp()
            }
        }
    }

    private fun openCamera() {
        try {
            val photoFile = File.createTempFile("scan_", ".jpg", requireContext().cacheDir)
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                photoFile
            )
            takePictureLauncher.launch(currentPhotoUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Kamera xətası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Şəkli PDF-ə çevirən real funksiya
    private fun convertImageToPdf(imageUri: Uri) {
        try {
            val bitmap = android.provider.MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)

            val pdfFile = File(requireContext().cacheDir, "converted_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()

            val pdfUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", pdfFile)
            Toast.makeText(requireContext(), "PDF yaradıldı!", Toast.LENGTH_SHORT).show()
            DocumentRouter.routeFile(requireContext(), pdfUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Çevirmə xətası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareApp() {
        try {
            val appInfo = requireContext().packageManager.getApplicationInfo(requireContext().packageName, 0)
            val apkFile = File(appInfo.publicSourceDir)
            val apkUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", apkFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Tətbiqi Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Paylaşma xətası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class CategoryItem(val name: String, val count: Int, val iconRes: Int, val colorHex: String)
    data class ToolItem(val name: String, val iconRes: Int, val colorHex: String)

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
            holder.binding.tvCategoryCount.text = "${item.count} files"
            holder.binding.ivCategoryIcon.setImageResource(item.iconRes)
            holder.binding.ivCategoryIcon.setColorFilter(Color.parseColor(item.colorHex), PorterDuff.Mode.SRC_IN)
            holder.binding.root.setOnClickListener { onClick(item.name) }
        }

        override fun getItemCount() = items.size
    }

    inner class ToolAdapter(
        private val items: List<ToolItem>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<ToolAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemToolBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemToolBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvToolName.text = item.name
            holder.binding.ivToolIcon.setImageResource(item.iconRes)
            holder.binding.ivToolIcon.setColorFilter(Color.parseColor(item.colorHex), PorterDuff.Mode.SRC_IN)
            holder.binding.root.setOnClickListener { onClick(item.name) }
        }

        override fun getItemCount() = items.size
    }
}
