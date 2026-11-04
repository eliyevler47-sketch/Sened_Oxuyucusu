package com.example.universalviewer

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

class OfficeViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val container = findViewById<LinearLayout>(R.id.contentContainer)

        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "Ofis Sənədi"
        toolbar.setNavigationOnClickListener { finish() }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(24, 24, 24, 24)
        }
        
        val textView = TextView(this).apply {
            setPadding(48, 48, 48, 48)
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setBackgroundColor(Color.WHITE)
            setLineSpacing(1.5f, 1.3f) // Sətirlər arası məsafə
        }
        
        scrollView.addView(textView)
        container.addView(scrollView)

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val fileName = getFileName(uri)
                        var resultText = ""

                        when {
                            fileName.endsWith(".docx", true) -> {
                                val document = XWPFDocument(inputStream)
                                val extractor = XWPFWordExtractor(document)
                                resultText = "📄 WORD SƏNƏDİ\n\n" + extractor.text
                                extractor.close()
                            }
                            fileName.endsWith(".xlsx", true) -> {
                                val workbook = XSSFWorkbook(inputStream)
                                val stringBuilder = StringBuilder("📊 EXCEL CƏDVƏLİ\n\n")
                                val formatter = DataFormatter()
                                
                                for (sheetIndex in 0 until workbook.numberOfSheets) {
                                    val sheet = workbook.getSheetAt(sheetIndex)
                                    stringBuilder.append("📌 Vərəq: ${sheet.sheetName}\n\n")
                                    for (row in sheet) {
                                        for (cell in row) {
                                            // cell.toString() yerinə formatter istifadə edirik ki, formul yox, rəqəm göstərsin
                                            stringBuilder.append("${formatter.formatCellValue(cell)}\t | \t")
                                        }
                                        stringBuilder.append("\n")
                                    }
                                    stringBuilder.append("\n──────────────────\n\n")
                                }
                                resultText = stringBuilder.toString()
                                workbook.close()
                            }
                            fileName.endsWith(".pptx", true) -> {
                                val slideShow = XMLSlideShow(inputStream)
                                val stringBuilder = StringBuilder("📺 POWERPOINT TƏQDİMATI\n\n")
                                
                                for ((index, slide) in slideShow.slides.withIndex()) {
                                    stringBuilder.append("📌 Slayd ${index + 1}\n\n")
                                    for (shape in slide.shapes) {
                                        if (shape is org.apache.poi.sl.usermodel.TextShape<*, *>) {
                                            // DÜZƏLİŞ: paragraphs yerinə textParagraphs istifadə edildi
                                            for (paragraph in shape.textParagraphs) {
                                                if (paragraph.text.isNotBlank()) {
                                                    stringBuilder.append("• ").append(paragraph.text).append("\n")
                                                }
                                            }
                                            stringBuilder.append("\n")
                                        }
                                    }
                                    stringBuilder.append("──────────────────\n\n")
                                }
                                resultText = stringBuilder.toString()
                                slideShow.close()
                            }
                            else -> resultText = "Bu Ofis faylı növü dəstəklənmir."
                        }

                        withContext(Dispatchers.Main) {
                            textView.text = resultText
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        textView.text = "Fayl oxunarkən xəta baş verdi:\n${e.message}"
                    }
                }
            }
        }
    }

    private fun getFileName(uri: android.net.Uri): String {
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
