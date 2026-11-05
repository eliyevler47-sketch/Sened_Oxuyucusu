package com.example.universalviewer

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
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
            setBackgroundColor(Color.parseColor("#2C2C2C"))
        }
        container.addView(pdfView)

        fabSettings.setOnClickListener {
            val options = arrayOf("🌙 Gecə Rejimi (Rəngləri Tərs Et)", "ℹ️ Səhifə Məlumatı")
            AlertDialog.Builder(this)
                .setTitle("PDF Parametrləri")
                .setItems(options) { _, which ->
                    when(which) {
                        0 -> {
                            // PDF-in rənglərini tərsinə çeviririk
                            if (pdfView.isNightModeActive) {
                                pdfView.setNightMode(false)
                                Toast.makeText(this, "Gündüz rejimi", Toast.LENGTH_SHORT).show()
                            } else {
                                pdfView.setNightMode(true)
                                Toast.makeText(this, "Gecə rejimi aktivdir", Toast.LENGTH_SHORT).show()
                            }
                        }
                        1 -> {
                            Toast.makeText(this, "Ümumi Səhifə: ${pdfView.pageCount}", Toast.LENGTH_LONG).show()
                        }
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
}
