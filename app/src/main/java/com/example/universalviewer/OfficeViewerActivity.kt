package com.example.universalviewer

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
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

        val webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#F3F4F6")) // Açıq boz fon
            settings.javaScriptEnabled = true
        }
        container.addView(webView)

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val fileName = getFileName(uri)
                        var htmlContent = ""

                        when {
                            fileName.endsWith(".docx", true) -> {
                                htmlContent = convertWordToHtml(inputStream)
                            }
                            fileName.endsWith(".xlsx", true) -> {
                                htmlContent = convertExcelToHtml(inputStream)
                            }
                            fileName.endsWith(".pptx", true) -> {
                                htmlContent = convertPptToHtml(inputStream)
                            }
                            else -> {
                                htmlContent = "<h3 style='text-align:center; margin-top:50px; font-family:sans-serif;'>Bu fayl dəstəklənmir.</h3>"
                            }
                        }

                        withContext(Dispatchers.Main) {
                            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        webView.loadData("<h3 style='color:red; font-family:sans-serif;'>Xəta: ${e.message}</h3>", "text/html", "UTF-8")
                    }
                }
            }
        }
    }

    // --- WORD-Ü HTML-Ə ÇEVİRİRİK ---
    private fun convertWordToHtml(inputStream: InputStream): String {
        val document = XWPFDocument(inputStream)
        val html = StringBuilder("<html><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>body{font-family:sans-serif; padding:15px; background-color:#f3f4f6;} .paper{background:white; padding:30px; border-radius:12px; box-shadow:0 4px 10px rgba(0,0,0,0.1); line-height:1.6; font-size:16px; color:#333;}</style></head><body><div class='paper'>")
        
        for (paragraph in document.paragraphs) {
            if (paragraph.text.isNotBlank()) {
                html.append("<p>").append(paragraph.text).append("</p>")
            }
        }
        html.append("</div></body></html>")
        document.close()
        return html.toString()
    }

    // --- EXCEL-İ HTML CƏDVƏLİNƏ ÇEVİRİRİK ---
    private fun convertExcelToHtml(inputStream: InputStream): String {
        val workbook = XSSFWorkbook(inputStream)
        val formatter = DataFormatter()
        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        val html = StringBuilder("<html><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>body{font-family:sans-serif; padding:15px; background-color:#f3f4f6;} .paper{background:white; padding:15px; border-radius:12px; box-shadow:0 4px 10px rgba(0,0,0,0.1); overflow-x:auto;} h3{color:#1E1B4B;} table{border-collapse:collapse; width:100%; margin-bottom:20px;} th,td{border:1px solid #ddd; padding:10px; text-align:left; white-space:nowrap;} th{background:#f4f4f4; font-weight:bold; color:#333;}</style></head><body><div class='paper'>")

        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            html.append("<h3>📌 Vərəq: ${sheet.sheetName}</h3>")
            html.append("<table>")
            for ((rowIndex, row) in sheet.withIndex()) {
                html.append("<tr>")
                for (cell in row) {
                    val cellValue = formatter.formatCellValue(cell, evaluator)
                    if (rowIndex == 0) {
                        html.append("<th>$cellValue</th>")
                    } else {
                        html.append("<td>$cellValue</td>")
                    }
                }
                html.append("</tr>")
            }
            html.append("</table>")
        }
        html.append("</div></body></html>")
        workbook.close()
        return html.toString()
    }

    // --- POWERPOINT-İ HTML SLAYD KARTLARINA ÇEVİRİRİK (XƏTA BURADA HƏLL EDİLDİ) ---
    private fun convertPptToHtml(inputStream: InputStream): String {
        val slideShow = XMLSlideShow(inputStream)
        val html = StringBuilder("<html><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>body{font-family:sans-serif; padding:15px; background-color:#f3f4f6;} .slide{background:white; padding:30px; margin-bottom:25px; border-radius:12px; box-shadow:0 4px 10px rgba(0,0,0,0.1);} .slide-number{color:#888; font-size:12px; font-weight:bold; text-transform:uppercase; letter-spacing:1px; margin-bottom:15px; border-bottom:1px solid #eee; padding-bottom:10px;} .content p{font-size:16px; color:#333; line-height:1.6; margin-bottom:10px;}</style></head><body>")
        
        for ((index, slide) in slideShow.slides.withIndex()) {
            html.append("<div class='slide'>")
            html.append("<div class='slide-number'>Slayd ${index + 1}</div>")
            html.append("<div class='content'>")
            
            for (shape in slide.shapes) {
                if (shape is org.apache.poi.sl.usermodel.TextShape<*, *>) {
                    for (paragraph in shape.textParagraphs) {
                        // ULTRA-PROFESSIONAL HƏLL: paragraph.text YOX, textRuns vasitəsilə oxuyuruq
                        val pText = paragraph.textRuns.joinToString("") { it.rawText ?: "" }
                        if (pText.isNotBlank()) {
                            html.append("<p>").append(pText).append("</p>")
                        }
                    }
                }
            }
            html.append("</div></div>")
        }
        html.append("</body></html>")
        slideShow.close()
        return html.toString()
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
