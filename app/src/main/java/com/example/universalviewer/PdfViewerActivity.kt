package com.example.universalviewer

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.appbar.MaterialToolbar
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewerActivity : BaseViewerActivity() {
    private lateinit var pdfView: PDFView
    private lateinit var pdfFile: File

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
                pdfFile = File(cacheDir, "temp_view.pdf")
                contentResolver.openInputStream(uri)?.use { input ->
                    pdfFile.outputStream().use { output -> input.copyTo(output) }
                }

                pdfView.fromFile(pdfFile)
                    .enableSwipe(true).swipeHorizontal(false).enableDoubletap(true)
                    .defaultPage(0).enableAnnotationRendering(false).password(null)
                    .scrollHandle(null).enableAntialiasing(true).spacing(24).pageSnap(true)
                    .load()
            } catch (e: Exception) { 
                e.printStackTrace() 
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
                if (query.isNotEmpty()) { searchPdf(query) }
            }
            .setNegativeButton("Ləğv et", null)
            .show()
    }

    private fun searchPdf(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val document = PDDocument.load(pdfFile)
                val stripper = PDFTextStripper()
                var foundPage = -1

                for (i in 1..document.numberOfPages) {
                    stripper.startPage = i
                    stripper.endPage = i
                    val text = stripper.getText(document)
                    if (text.contains(query, ignoreCase = true)) {
                        foundPage = i
                        break
                    }
                }
                document.close()

                withContext(Dispatchers.Main) {
                    if (foundPage != -1) {
                        pdfView.jumpTo(foundPage - 1, true)
                        Toast.makeText(this@PdfViewerActivity, "Tapıldı! Səhifə: $foundPage", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@PdfViewerActivity, "Söz tapılmadı.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PdfViewerActivity, "Axtarış zamanı xəta: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
