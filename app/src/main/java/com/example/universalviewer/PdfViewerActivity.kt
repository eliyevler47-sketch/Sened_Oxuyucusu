package com.example.universalviewer

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class PdfViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val container = findViewById<LinearLayout>(R.id.contentContainer)
        
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "PDF Sənədi"
        toolbar.setNavigationOnClickListener { finish() }

        // DÜZƏLİŞ: PDFView(this, null) edildi
        val pdfView = PDFView(this, null).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#2C2C2C")) // Tünd peşəkar fon
        }
        container.addView(pdfView)

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
