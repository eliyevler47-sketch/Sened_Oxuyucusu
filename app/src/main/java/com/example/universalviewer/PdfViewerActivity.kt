package com.example.universalviewer

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class PdfViewerActivity : BaseViewerActivity() {
    private lateinit var pdfView: PDFView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "PDF Sənədi"
        
        toolbar.inflateMenu(R.menu.viewer_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.action_search -> { showSearchDialog(); true }
                R.id.action_open_new -> { openNewFile(); true }
                else -> false
            }
        }
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

    // PDF daxilində söz axtarışı (Barteksc kitabxanasının daxili metodu)
    private fun showSearchDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Axtarılacaq söz..."
            setPadding(32, 32, 32, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("PDF-də Axtar")
            .setView(input)
            .setPositiveButton("Axtar") { _, _ ->
                val query = input.text.toString()
                if (query.isNotEmpty()) {
                    pdfView.search(query)
                }
            }
            .setNegativeButton("Ləğv et", null)
            .show()
    }

    override fun onThemeToggle() {
        if (pdfView.isNightModeActive) pdfView.setNightMode(false) else pdfView.setNightMode(true)
    }

    override fun onFontSizeChange(change: Int) {
        if (change == 0) pdfView.zoomTo(1f) else {
            val currentZoom = pdfView.zoom
            pdfView.zoomTo(currentZoom + (change * 0.1f))
        }
    }
}
