package com.example.universalviewer

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

class OfficeViewerActivity : AppCompatActivity() {
    
    private var isDarkMode = false
    private var currentFontSize = 16 // Default şrift ölçüsü

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val container = findViewById<LinearLayout>(R.id.contentContainer)
        val fabSettings = findViewById<FloatingActionButton>(R.id.fabSettings)

        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "Ofis Sənədi"
        toolbar.setNavigationOnClickListener { finish() }

        // Yükləmə animasiyası
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
            setBackgroundColor(Color.parseColor("#F8F9FA"))
            settings.javaScriptEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
        }

        // Seçimlər Menyusu (Gecə rejimi, Şrift ölçüsü)
        fabSettings.setOnClickListener {
            val options = arrayOf("🌙 Gecə Rejimi (Yandır/Söndür)", "🔤 Şrifti Böyüt", "🔤 Şrifti Kiçilt", "📤 Paylaş")
            AlertDialog.Builder(this)
                .setTitle("Sənəd Parametrləri")
                .setItems(options) { _, which ->
                    when(which) {
                        0 -> {
                            isDarkMode = !isDarkMode
                            val bgColor = if(isDarkMode) "#121212" else "#F8F9FA"
                            val paperColor = if(isDarkMode) "#1E1E1E" else "#FFFFFF"
                            val textColor = if(isDarkMode) "#E0E0E0" else "#2C3E50"
                            webView.evaluateJavascript("document.body.style.backgroundColor='$bgColor'; document.querySelectorAll('.paper, .slide').forEach(el => {el.style.backgroundColor='$paperColor'; el.style.color='$textColor';});", null)
                            Toast.makeText(this, if(isDarkMode) "Gecə rejimi aktivdir" else "Gündüz rejimi aktivdir", Toast.LENGTH_SHORT).show()
                        }
                        1 -> {
                            currentFontSize += 2
                            webView.evaluateJavascript("document.body.style.fontSize='${currentFontSize}px'; document.querySelectorAll('p, td, th').forEach(el => el.style.fontSize='${currentFontSize}px');", null)
                        }
                        2 -> {
                            if(currentFontSize > 10) {
                                currentFontSize -= 2
                                webView.evaluateJavascript("document.body.style.fontSize='${currentFontSize}px'; document.querySelectorAll('p, td, th').forEach(el => el.style.fontSize='${currentFontSize}px');", null)
                            }
                        }
                        3 -> {
                            // Paylaşma funksiyası (Intent)
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "Universal Viewer ilə açıldı: ${intent.data}")
                                type = "text/plain"
                            }
                            startActivity(android.content.Intent.createChooser(sendIntent, "Paylaş"))
                        }
                    }
                }.show()
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
                            fileName.endsWith(".docx", true) -> htmlContent = convertWordToHtml(inputStream)
                            fileName.endsWith(".xlsx", true) -> htmlContent = convertExcelToHtml(inputStream)
                            fileName.endsWith(".pptx", true) -> htmlContent = convertPptToHtml(inputStream)
                            else -> htmlContent = "<h3 style='text-align:center; color:red;'>Bu fayl dəstəklənmir.</h3>"
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

    // --- WORD-Ü HTML-Ə ÇEVİRİRİK (Cədvəl və Qalın yazılar dəstəklənir) ---
    private fun convertWordToHtml(inputStream: InputStream): String {
        val document = XWPFDocument(inputStream)
        val html = StringBuilder("""
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { font-family: 'Segoe UI', sans-serif; padding: 15px; background-color: #F8F9FA; transition: background-color 0.3s; }
                .paper { background: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); line-height: 1.8; color: #2C3E50; transition: background-color 0.3s, color 0.3s; }
                p { margin-bottom: 12px; font-size: 16px;}
                table { border-collapse: collapse; width: 100%; margin: 15px 0; }
                td, th { border: 1px solid #ddd; padding: 10px; font-size: 15px; }
            </style></head><body><div class="paper">
        """.trimIndent())
        
        // Paraqrafları oxuyuruq
        for (paragraph in document.paragraphs) {
            if (paragraph.text.isNotBlank()) {
                html.append("<p>").append(paragraph.text).append("</p>")
            }
        }

        // Cədvəlləri oxuyuruq (Word-də cədvəl varsa)
        for (table in document.tables) {
            html.append("<table>")
            for (row in table.rows) {
                html.append("<tr>")
                for (cell in row.tableCells) {
                    html.append("<td>").append(cell.text).append("</td>")
                }
                html.append("</tr>")
            }
            html.append("</table>")
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
        val html = StringBuilder("""
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { font-family: 'Segoe UI', sans-serif; padding: 15px; background-color: #F8F9FA; transition: background-color 0.3s; }
                .paper { background: white; padding: 20px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); overflow-x: auto; margin-bottom: 20px; transition: background-color 0.3s; }
                h3 { color: #1E1B4B; font-size: 18px; border-bottom: 2px solid #E2E8F0; padding-bottom: 10px;}
                table { border-collapse: collapse; width: 100%; font-size: 14px; }
                th, td { border: 1px solid #E2E8F0; padding: 10px; text-align: left; white-space: nowrap; }
                th { background: #F1F5F9; font-weight: bold; }
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
                    if (rowIndex == 0) html.append("<th>$cellValue</th>") else html.append("<td>$cellValue</td>")
                }
                html.append("</tr>")
            }
            html.append("</table></div>")
        }
        html.append("</body></html>")
        workbook.close()
        return html.toString()
    }

    // --- POWERPOINT-İ HTML SLAYD KARTLARINA ÇEVİRİRİK ---
    private fun convertPptToHtml(inputStream: InputStream): String {
        val slideShow = XMLSlideShow(inputStream)
        val html = StringBuilder("""
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { font-family: 'Segoe UI', sans-serif; padding: 15px; background-color: #F8F9FA; transition: background-color 0.3s; }
                .slide { background: white; padding: 25px; margin-bottom: 25px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); position: relative; transition: background-color 0.3s; }
                .slide-number { position: absolute; top: 15px; right: 20px; color: #94A3B8; font-size: 12px; font-weight: bold; }
                .title { color: #1E1B4B; font-size: 20px; font-weight: 600; margin-bottom: 20px; border-bottom: 2px solid #F1F5F9; padding-bottom: 10px; line-height: 1.3;}
                .content p { font-size: 16px; color: #334155; line-height: 1.6; margin-bottom: 10px; }
                .bullet { display: flex; margin-bottom: 10px; }
                .bullet-dot { margin-right: 10px; color: #3B82F6; font-weight: bold; }
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
                                html.append("<div class='title'>").append(pText).append("</div>")
                                isFirstShape = false
                            } else {
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
            } finally { cursor?.close() }
        }
        return result ?: uri.path?.substringAfterLast('/') ?: ""
    }
}
