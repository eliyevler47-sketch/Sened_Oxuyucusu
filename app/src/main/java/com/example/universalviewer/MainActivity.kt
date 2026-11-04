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
        if (uri != null) routeFile(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun routeFile(uri: Uri) {
        val mimeType = contentResolver.getType(uri)
        when {
            mimeType?.contains("pdf") == true -> {
                startActivity(Intent(this, PdfViewerActivity::class.java).apply { data = uri })
            }
            mimeType?.contains("word") == true || mimeType?.contains("officedocument") == true -> {
                startActivity(Intent(this, OfficeViewerActivity::class.java).apply { data = uri })
            }
            mimeType?.contains("html") == true || mimeType?.contains("text") == true -> {
                startActivity(Intent(this, HtmlViewerActivity::class.java).apply { data = uri })
            }
            mimeType?.startsWith("image/") == true -> {
                startActivity(Intent(this, ImageViewerActivity::class.java).apply { data = uri })
            }
            else -> {
                Toast.makeText(this, "Bu fayl növü dəstəklənmir.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
