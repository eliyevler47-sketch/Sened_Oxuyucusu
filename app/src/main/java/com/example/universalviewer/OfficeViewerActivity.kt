package com.example.universalviewer

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

class OfficeViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val scrollView = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#F0F0F0")) } // Açıq boz fon
        val container = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        
        val textView = TextView(this).apply { 
            setPadding(32, 32, 32, 32)
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE) // Sənəd ağ vərəqdə
            setLineSpacing(1.2f, 1.2f)
        }
        
        container.addView(textView)
        scrollView.addView(container)
        setContentView(scrollView)

        val uri = intent.data
        if (uri != null) {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val fileName = getFileName(uri)
                    
                    when {
                        fileName.endsWith(".docx", true) -> {
                            val document = XWPFDocument(inputStream)
                            val extractor = XWPFWordExtractor(document)
                            textView.text = "📄 WORD SƏNƏDİ\n\n" + extractor.text
                            extractor.close()
                        }
                        fileName.endsWith(".xlsx", true) -> {
                            val workbook = XSSFWorkbook(inputStream)
                            val stringBuilder = StringBuilder("📊 EXCEL CƏDVƏLİ\n\n")
                            
                            // Formulları yox, həqiqi dəyərləri oxuyan formatlayıcı
                            val formatter = DataFormatter() 

                            for (sheetIndex in 0 until workbook.numberOfSheets) {
                                val sheet = workbook.getSheetAt(sheetIndex)
                                stringBuilder.append("Vərəq: ${sheet.sheetName}\n")
                                for (row in sheet) {
                                    for (cell in row) {
                                        // cell.toString() yerinə formatter.formatCellValue(cell) istifadə edirik
                                        stringBuilder.append("${formatter.formatCellValue(cell)}  |  ")
                                    }
                                    stringBuilder.append("\n")
                                }
                                stringBuilder.append("\n-------------------\n")
                            }
                            textView.text = stringBuilder.toString()
                            workbook.close()
                        }
                        fileName.endsWith(".pptx", true) -> {
                            val slideShow = XMLSlideShow(inputStream)
                            val stringBuilder = StringBuilder("📺 POWERPOINT SLAYDARI\n\n")
                            
                            for ((index, slide) in slideShow.slides.withIndex()) {
                                stringBuilder.append("Slayd ${index + 1}:\n")
                                for (shape in slide.shapes) {
                                    if (shape is org.apache.poi.sl.usermodel.TextShape<*, *>) {
                                        stringBuilder.append(shape.text).append("\n")
                                    }
                                }
                                stringBuilder.append("\n-------------------\n")
                            }
                            textView.text = stringBuilder.toString()
                            slideShow.close()
                        }
                        else -> {
                            textView.text = "Bu Ofis faylı növü dəstəklənmir."
                        }
                    }
                }
            } catch (e: Exception) {
                textView.text = "Fayl oxunarkən xəta baş verdi:\n${e.message}"
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
