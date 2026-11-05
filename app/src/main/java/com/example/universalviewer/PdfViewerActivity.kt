package com.example.universalviewer

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class PdfViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val container = findViewById<LinearLayout>(R.id.contentContainer)
        val fabSettings = findViewById<FloatingActionButton>(R.id.fabSettings)
        
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "PDF Sənədi"
        toolbar.setNavigationOnClickListener { finish() }

        val pdfView = PDFView(this, null).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#2C2C2C")) // Tünd peşəkar fon
        }
        container.addView(pdfView)

        // Peşəkar Funksiyalar Menyusu
        fabSettings.setOnClickListener {
            val options = arrayOf("🔍 Səhifəyə Keçid", "📤 Sənədi Paylaş")
            AlertDialog.Builder(this)
                .setTitle("PDF Alətləri")
                .setItems(options) { _, which ->
                    when(which) {
                        0 -> showJumpToPageDialog(pdfView)
                        1 -> shareFile()
                    }
                }.show()
        }

        val uri = intent.data
        if (uri != null) {
            try {
                val file = File(cacheDir, "temp_view.pdf")
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }

                pdfView.fromFile(file)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .enableAnnotationRendering(false)
                    .password(null)
                    .scrollHandle(null)
                    .enableAntialiasing(true)
                    .spacing(24)
                    .pageSnap(true)
                    .load()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Səhifəyə keçid dialoqu
    private fun showJumpToPageDialog(pdfView: PDFView) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Səhifə nömrəsi (1 - ${pdfView.pageCount})"
            setPadding(32, 32, 32, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Səhifəyə Keçid")
            .setView(input)
            .setPositiveButton("Get") { _, _ ->
                val pageStr = input.text.toString()
                if (pageStr.isNotEmpty()) {
                    val page = pageStr.toIntOrNull()
                    if (page != null && page > 0 && page <= pdfView.pageCount) {
                        pdfView.jumpTo(page - 1, true) // PDF səhifələri 0-dan başlayır
                    } else {
                        Toast.makeText(this, "Yanlış səhifə nömrəsi!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Ləğv et", null)
            .show()
    }

    // Paylaşma funksiyası
    private fun shareFile() {
        intent.data?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Sənədi Paylaş"))
        }
    }
}
