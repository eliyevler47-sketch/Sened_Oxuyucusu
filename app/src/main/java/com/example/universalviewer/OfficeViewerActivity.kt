package com.example.universalviewer

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.ProgressBar
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

        // Kasma olmasın deyə əvvəlcə Yükləmə (Loading) ekranı
        val progressBar = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = android.view.Gravity.CENTER }
        }
        container.gravity = android.view.Gravity.CENTER
        container.addView(progressBar)

        val webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#F8F9FA")) // Təmiz ağ-boz fon
            settings.javaScriptEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
        }

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
                                htmlContent = "<h3 style='text-align:center; color:red;'>Bu fayl dəstəklənmir.</h3>"
                            }
                        }

                        withContext(Dispatchers.Main) {
                            container.removeView(progressBar)
                            container.gravity = android.view.Gravity.NO_GRAVITY
                            container.addView(webView)
                            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        container.removeView(progressBar)
                        webView.loadData("<h3 style='color:red; text-align:center;'>Xəta: ${e.message}</h3>", "text/html", "UTF-8")
                    }
                }
            }
        }
    }

    // --- WORD-Ü HTML-Ə ÇEVİRİRİK (Premium Dizayn) ---
    private fun convertWordToHtml(inputStream: InputStream): String {
        val document = XWPFDocument(inputStream)
        val html = StringBuilder("""
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 15px; background-color: #F8F9FA; }
                .paper { background: white; padding: 40px; border-radius: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.08); line-height: 1.8; font-size: 16px; color: #2C3E50; }
                p { margin-bottom: 15px; }
            </style></head><body><div class="paper">
        """.trimIndent())
        
        for (paragraph in document.paragraphs) {
            if (paragraph.text.isNotBlank()) {
                // Word-dəki boşluqları və sətirləri qoruyuruq
                val safeText = paragraph.text.replace("\n", "<br>")
                html.append("<p>").append(safeText).append("</p>")
            }
        }
        html.append("</div></body></html>")
        document.close()
        return html.toString()
    }

    // --- EXCEL-İ HTML CƏDVƏLİNƏ ÇEVİRİRİK (Müasir Cədvəl) ---
    private fun convertExcelToHtml(inputStream: InputStream): String {
        val workbook = XSSFWorkbook(inputStream)
        val formatter = DataFormatter()
        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        val html = StringBuilder("""
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { font-family: 'Segoe UI', sans-serif; padding: 15px; background-color: #F8F9FA; }
                .paper { background: white; padding: 20px; border-radius: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.08); overflow-x: auto; margin-bottom: 20px;}
                h3 { color: #1E1B4B; font-size: 18px; border-bottom: 2px solid #E2E8F0; padding-bottom: 10px;}
                table { border-collapse: collapse; width: 100%; margin-bottom: 10px; font-size: 14px; }
                th, td { border: 1px solid #E2E8F0; padding: 12px; text-align: left; white-space: nowrap; }
                th { background: #F1F5F9; font-weight: bold; color: #1E293B; }
                tr:nth-child(even) { background: #F8FAFC; }
            </style></head><body>
        """.trimIndent())

        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            html.append("<div class='paper'>")
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
            html.append("</table></div>")
        }
        html.append("</body></html>")
        workbook.close()
        return html.toString()
    }

    // --- POWERPOINT-İ HTML SLAYD KARTLARINA ÇEVİRİRİK (Rəng və Ölçü Dəstəyi) ---
    private fun convertPptToHtml(inputStream: InputStream): String {
        val slideShow = XMLSlideShow(inputStream)
        val html = StringBuilder("""
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { font-family: 'Segoe UI', sans-serif; padding: 15px; background-color: #F8F9FA; }
                .slide { background: white; padding: 30px; margin-bottom: 25px; border-radius: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.08); position: relative; }
                .slide-number { position: absolute; top: 15px; right: 20px; color: #94A3B8; font-size: 12px; font-weight: bold; }
                .title { color: #1E1B4B; font-size: 22px; font-weight: 600; margin-bottom: 20px; border-bottom: 2px solid #F1F5F9; padding-bottom: 10px;}
                .content p { font-size: 16px; color: #334155; line-height: 1.6; margin-bottom: 10px; }
                .bullet { display: flex; margin-bottom: 8px; }
                .bullet-dot { margin-right: 10px; color: #3B82F6; }
            </style></head><body>
        """.trimIndent())
        
        for ((index, slide) in slideShow.slides.withIndex()) {
            html.append("<div class='slide'>")
            html.append("<div class='slide-number'>Slayd ${index + 1}</div>")
            html.append("<div class='content'>")
            
            var isFirstShape = true
            
            for (shape in slide.shapes) {
                if (shape is org.apache.poi.sl.usermodel.TextShape<*, *>) {
                    for (paragraph in shape.textParagraphs) {
                        val pText = paragraph.textRuns.joinToString("") { it.rawText ?: "" }
                        
                        if (pText.isNotBlank()) {
                            if (isFirstShape) {
                                // İlk yazı adətən slaydın başlığı olur
                                html.append("<div class='title'>").append(pText).append("</div>")
                                isFirstShape = false
                            } else {
                                // Qalan yazılar bəndlər (bullet) kimi göstərilir
                                html.append("<div class='bullet'><span class='bullet-dot'>•</span><p>").append(pText).append("</p></div>")
                            }
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
