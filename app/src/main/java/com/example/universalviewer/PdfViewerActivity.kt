package com.example.universalviewer

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.appbar.MaterialToolbar
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.File

class PdfViewerActivity : BaseViewerActivity() {
    private lateinit var pdfView: PDFView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "PDF Sənədi"

        val container = findViewById<LinearLayout>(R.id.contentContainer)
        container.setPadding(16, 16, 16, 16)
        container.setBackgroundColor(Color.parseColor("#E0E0E0"))

        pdfView = PDFView(this, null).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        container.addView(pdfView)

        val uri = intent.data
        if (uri != null) {
            try {
                val pdfFile = File(cacheDir, "temp_view.pdf")
                contentResolver.openInputStream(uri)?.use { input ->
                    pdfFile.outputStream().use { output -> input.copyTo(output) }
                }

                pdfView.fromFile(pdfFile)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .enableAnnotationRendering(false)
                    .password(null)
                    .scrollHandle(null)
                    .enableAntialiasing(true)
                    .spacing(16)
                    .pageSnap(false)
                    .load()
            } catch (e: Exception) { 
                Toast.makeText(this, "PDF açılarkən xəta baş verdi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showSearchDialog() {
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
                    // PDFView-in daxili axtarış funksiyası (Sarı rənglə işıqlandırır)
                    pdfView.findAllAsync(query)
                }
            }
            .setNegativeButton("Ləğv et", null)
            .show()
    }
}
