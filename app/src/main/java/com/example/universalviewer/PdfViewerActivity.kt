package com.example.universalviewer

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class PdfViewerActivity : BaseViewerActivity() {
    private lateinit var pdfView: PDFView
    private var isNightMode = false // Xəta verən isNightModeActive yerinə

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "PDF Sənədi"
        toolbar.setNavigationOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.contentContainer)
        pdfView = PDFView(this, null).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#2C2C2C"))
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
                    .enableSwipe(true).swipeHorizontal(false).enableDoubletap(true)
                    .defaultPage(0).enableAnnotationRendering(false).password(null)
                    .scrollHandle(null).enableAntialiasing(true).spacing(24).pageSnap(true)
                    .load()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // PDF Axtarışı (Barteksc-də mətn axtarışı olmadığı üçün sadəcə məlumat veririk)
    fun showSearchDialog() {
        Toast.makeText(this, "PDF daxilində mətn axtarışı hazırda dəstəklənmir. Zəhmət olmasa Word/Excel fayllarında yoxlayın.", Toast.LENGTH_LONG).show()
    }

    // PDF üçün Gecə Rejimi (Xəta verən hissə düzəldildi)
    override fun onThemeToggle() {
        isNightMode = !isNightMode
        pdfView.setNightMode(isNightMode)
    }

    override fun onFontSizeChange(change: Int) {
        if (change == 0) pdfView.zoomTo(1f) else {
            val currentZoom = pdfView.zoom
            pdfView.zoomTo(currentZoom + (change * 0.1f))
        }
    }
}
