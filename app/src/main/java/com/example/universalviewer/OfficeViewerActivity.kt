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
            setBackgroundColor(Color.parseColor("#F3F4F6"))
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
                                htmlContent = "<h3>Bu fayl dəstəklənmir.</h3>"
                            }
                        }

                        withContext(Dispatchers.Main) {
                            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        webView.loadData("<h3 style='color:red;'>Xəta: ${e.message}</h3>", "text/html", "UTF-8")
                    }
                }
            }
        }
    }

    // --- WORD-Ü HTML-Ə ÇEVİRİRİK ---
    private fun convertWordToHtml(inputStream: InputStream): String {
        val document = XWPFDocument(inputStream)
        val html = StringBuilder("<html><head><style>body{font-family:sans-serif; padding:20px; line-height:1.6;} .paper{background:white; padding:40px; border-radius:10px; box-shadow:0 4px 6px rgba(0,0,0,0.1);}</style></head><body><div class='paper'>")
        
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
        val html = StringBuilder("<html><head><style>body{font-family:sans-serif; padding:20px;} .paper{background:white; padding:20px; border-radius:10px; box-shadow:0 4px 6px rgba(0,0,0,0.1); overflow-x:auto;} table{border-collapse:collapse; width:100%;} th,td{border:1px solid #ccc; padding:8px; text-align:left;} th{background:#f4f4f4;}</style></head><body><div class='paper'>")

        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            html.append("<h3>Vərəq: ${sheet.sheetName}</h3>")
            html.append("<table>")
            for ((rowIndex, row) in sheet.withIndex()) {
                html.append("<tr>")
                for (cell in row) {
                    val valStr = formatter.formatCellValue(cell, evaluator)
                    if (rowIndex == 0) html.append("<th>$valStr</th>") else html.append("<td>$valStr</td>")
                }
                html.append("</tr>")
            }
            html.append("</table><br><br>")
        }
        html.append("</div></body></html>")
        workbook.close()
        return html.toString()
    }

    // --- POWERPOINT-İ HTML SLAYD KARTLARINA ÇEVİRİRİK ---
    private fun convertPptToHtml(inputStream: InputStream): String {
        val slideShow = XMLSlideShow(inputStream)
        val html = StringBuilder("<html><head><style>body{font-family:sans-serif; padding:15px; background:#f3f4f6;} .slide{background:white; padding:30px; margin-bottom:25px; border-radius:12px; box-shadow:0 4px 10px rgba(0,0,0,0.1);} .title{color:#1E1B4B; font-size:14px; font-weight:bold; border-bottom:1px solid #eee; padding-bottom:10px; margin-bottom:20px;} .content{font-size:16px; color:#333; line-height:1.6;}</style></head><body>")
        
        for ((index, slide) in slideShow.slides.withIndex()) {
            html.append("<div class='slide'>")
            html.append("<div class='title'>Slayd ${index + 1}</div>")
            html.append("<div class='content'>")
            for (shape in slide.shapes) {
                if (shape is org.apache.poi.sl.usermodel.TextShape<*, *>) {
                    for (paragraph in shape.textParagraphs) {
                        if (paragraph.text.isNotBlank()) {
                            html.append("<p>").append(paragraph.text).append("</p>")
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
