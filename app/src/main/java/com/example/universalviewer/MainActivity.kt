package com.example.universalviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var prefManager: PreferenceManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DocumentAdapter

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            routeFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        prefManager = PreferenceManager(this)
        recyclerView = findViewById(R.id.recyclerViewRecent)
        val btnOpen = findViewById<MaterialButton>(R.id.btnOpenFile)

        btnOpen.setOnClickListener {
            filePickerLauncher.launch(arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain", "text/html", "image/*"
            ))
        }

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadRecentFiles()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DocumentAdapter(emptyList()) { action, item ->
            when(action) {
                "CLICK" -> routeFile(Uri.parse(item.uri))
                "PIN" -> { prefManager.togglePin(item.uri); loadRecentFiles() }
                "FAVORITE" -> { prefManager.toggleFavorite(item.uri); loadRecentFiles() }
            }
        }
        recyclerView.adapter = adapter
    }

    private fun loadRecentFiles() {
        adapter.updateList(prefManager.getSortedDocumentList())
    }

    private fun routeFile(uri: Uri) {
        val fileName = getFileName(uri)
        prefManager.saveRecentFile(uri, fileName)
        val mimeType = contentResolver.getType(uri)
        
        when {
            mimeType?.contains("pdf") == true || fileName.lowercase().endsWith(".pdf") -> {
                startActivity(Intent(this, PdfViewerActivity::class.java).apply { data = uri; putExtra("FILE_NAME", fileName) })
            }
            fileName.lowercase().endsWith(".docx") || fileName.lowercase().endsWith(".xlsx") || fileName.lowercase().endsWith(".pptx") -> {
                startActivity(Intent(this, OfficeViewerActivity::class.java).apply { data = uri; putExtra("FILE_NAME", fileName) })
            }
            mimeType?.contains("html") == true || mimeType?.contains("text") == true || fileName.lowercase().endsWith(".txt") -> {
                startActivity(Intent(this, HtmlViewerActivity::class.java).apply { data = uri; putExtra("FILE_NAME", fileName) })
            }
            mimeType?.startsWith("image/") == true -> {
                startActivity(Intent(this, ImageViewerActivity::class.java).apply { data = uri; putExtra("FILE_NAME", fileName) })
            }
            else -> Toast.makeText(this, "Bu fayl növü dəstəklənmir.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = DocumentFile.fromSingleUri(this, uri)?.name
        if (fileName.isNullOrBlank()) {
            val cursor = contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) fileName = cursor.getString(index)
                cursor.close()
            }
        }
        return fileName ?: "Sənəd"
    }

    inner class DocumentAdapter(
        private var items: List<DocumentItem>,
        private val onAction: (String, DocumentItem) -> Unit
    ) : RecyclerView.Adapter<DocumentAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvFileName: TextView = view.findViewById(R.id.tvFileName)
            val btnPin: ImageButton = view.findViewById(R.id.btnPin)
            val btnFavorite: ImageButton = view.findViewById(R.id.btnFavorite)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_document, parent, false))
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvFileName.text = item.fileName
            holder.btnPin.setColorFilter(if (item.isPinned) android.graphics.Color.parseColor("#1E1B4B") else android.graphics.Color.parseColor("#B0B0B0"))
            holder.btnFavorite.setColorFilter(if (item.isFavorite) android.graphics.Color.parseColor("#F59E0B") else android.graphics.Color.parseColor("#B0B0B0"))
            holder.itemView.setOnClickListener { onAction("CLICK", item) }
            holder.btnPin.setOnClickListener { onAction("PIN", item) }
            holder.btnFavorite.setOnClickListener { onAction("FAVORITE", item) }
        }
        override fun getItemCount() = items.size
        fun updateList(newItems: List<DocumentItem>) { items = newItems; notifyDataSetChanged() }
    }
}
