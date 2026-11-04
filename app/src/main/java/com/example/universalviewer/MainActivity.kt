package com.example.universalviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.universalviewer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { }
            routeFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }

        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri -> routeFile(uri) }
        }
    }

    private fun routeFile(uri: Uri) {
        val mimeType = contentResolver.getType(uri)
        val fileName = getFileName(uri).lowercase()
        val displayName = getFileName(uri) // Faylın həqiqi adı
        
        when {
            mimeType?.contains("pdf") == true || fileName.endsWith(".pdf") -> {
                startActivity(Intent(this, PdfViewerActivity::class.java).apply { 
                    data = uri 
                    putExtra("FILE_NAME", displayName) // Adı ötürürük
                })
            }
            mimeType?.contains("word") == true || mimeType?.contains("officedocument") == true || 
            fileName.endsWith(".docx") || fileName.endsWith(".xlsx") || fileName.endsWith(".pptx") -> {
                startActivity(Intent(this, OfficeViewerActivity::class.java).apply { 
                    data = uri
                    putExtra("FILE_NAME", displayName)
                })
            }
            mimeType?.contains("html") == true || mimeType?.contains("text") == true || fileName.endsWith(".txt") -> {
                startActivity(Intent(this, HtmlViewerActivity::class.java).apply { 
                    data = uri 
                    putExtra("FILE_NAME", displayName)
                })
            }
            mimeType?.startsWith("image/") == true -> {
                startActivity(Intent(this, ImageViewerActivity::class.java).apply { 
                    data = uri 
                    putExtra("FILE_NAME", displayName)
                })
            }
            else -> {
                Toast.makeText(this, "Bu fayl növü dəstəklənmir.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
        }
        return result ?: uri.path?.substringAfterLast('/') ?: ""
    }
}
