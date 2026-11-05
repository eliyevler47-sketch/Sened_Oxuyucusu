package com.example.universalviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var prefManager: PreferenceManager
    private lateinit var recyclerView: RecyclerView

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
        loadRecentFiles()
    }

    private fun loadRecentFiles() {
        val recentUris = prefManager.getRecentFiles()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RecentAdapter(recentUris) { uriString ->
            routeFile(Uri.parse(uriString))
        }
    }

    private fun routeFile(uri: Uri) {
        prefManager.saveRecentFile(uri)
        val mimeType = contentResolver.getType(uri)
        val fileName = getFileName(uri).lowercase()
        
        when {
            mimeType?.contains("pdf") == true || fileName.endsWith(".pdf") -> {
                startActivity(Intent(this, PdfViewerActivity::class.java).apply { data = uri; putExtra("FILE_NAME", getFileName(uri)) })
            }
            fileName.endsWith(".docx") || fileName.endsWith(".xlsx") || fileName.endsWith(".pptx") -> {
                startActivity(Intent(this, OfficeViewerActivity::class.java).apply { data = uri; putExtra("FILE_NAME", getFileName(uri)) })
            }
            mimeType?.contains("html") == true || mimeType?.contains("text") == true || fileName.endsWith(".txt") -> {
                startActivity(Intent(this, HtmlViewerActivity::class.java).apply { data = uri })
            }
            mimeType?.startsWith("image/") == true -> {
                startActivity(Intent(this, ImageViewerActivity::class.java).apply { data = uri; putExtra("FILE_NAME", getFileName(uri)) })
            }
            else -> Toast.makeText(this, "Bu fayl növü dəstəklənmir.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try { if (cursor != null && cursor.moveToFirst()) { val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (index != -1) result = cursor.getString(index) } } finally { cursor?.close() }
        }
        return result ?: uri.path?.substringAfterLast('/') ?: ""
    }

    inner class RecentAdapter(private val items: List<String>, private val onClick: (String) -> Unit) : RecyclerView.Adapter<RecentAdapter.ViewHolder>() {
        inner class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(parent.context).apply { 
                setPadding(24, 24, 24, 24); textSize = 16f; setBackgroundColor(android.graphics.Color.WHITE)
                val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                layoutParams = params
                setMargins(this, 0, 8)
            }
            return ViewHolder(tv)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val path = items[position]
            val fileName = Uri.parse(path).lastPathSegment ?: "Sənəd"
            holder.textView.text = "📄 ${fileName.substringAfterLast('/')}"
            holder.textView.setOnClickListener { onClick(path) }
        }
        override fun getItemCount() = items.size
        fun setMargins(view: android.view.View, left: Int, top: Int) {
            if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                val p = view.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(left, top, 0, 0)
                view.requestLayout()
            }
        }
    }
}
