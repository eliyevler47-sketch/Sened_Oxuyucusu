package com.example.universalviewer

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.io.InputStream

class OfficeViewerActivity : BaseViewerActivity() {
    private lateinit var webView: WebView
    private var currentFontSize = 16

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_viewer) SİLİNDİ

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "Sənəd"

        val container = findViewById<LinearLayout>(R.id.contentContainer)
        val progressBar = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = android.view.Gravity.CENTER }
        }
        container.gravity = android.view.Gravity.CENTER
        container.addView(progressBar)

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#F3F4F6"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
        }

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val fileName = getFileName(uri).lowercase()
                        val bytes = inputStream.readBytes()
                        inputStream.close()

                        withContext(Dispatchers.Main) {
                            container.removeView(progressBar)
                            container.gravity = android.view.Gravity.NO_GRAVITY
                            container.addView(webView)
                            webView.loadUrl("file:///android_asset/viewer.html")

                            webView.webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    
                                    if (fileName.endsWith(".docx")) {
                                        val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                        webView.evaluateJavascript("renderDocx('$base64String');", null)
                                    } 
                                    else if (fileName.endsWith(".xlsx")) {
                                        val html = convertExcelToHtml(bytes.inputStream())
                                        val base64Html = Base64.encodeToString(html.toByteArray(), Base64.NO_WRAP)
                                        webView.evaluateJavascript("renderHtml('$base64Html');", null)
                                    } 
                                    else if (fileName.endsWith(".pptx")) {
                                        val html = convertPptToHtml(bytes.inputStream())
                                        val base64Html = Base64.encodeToString(html.toByteArray(), Base64.NO_WRAP)
                                        webView.evaluateJavascript("renderHtml('$base64Html');", null)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        container.removeView(progressBar)
                        container.addView(TextView(this@OfficeViewerActivity).apply { text = "Xəta: ${e.message}"; setPadding(32,32,32,32) })
                    }
                }
            }
        }
    }

    private fun convertExcelToHtml(inputStream: InputStream): String {
        val workbook = XSSFWorkbook(inputStream)
        val formatter = DataFormatter()
        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        val html = StringBuilder("")

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
        workbook.close()
        return html.toString()
    }

    private fun convertPptToHtml(inputStream: InputStream): String {
        val slideShow = XMLSlideShow(inputStream)
        val html = StringBuilder("")
        for ((index, slide) in slideShow.slides.withIndex()) {
            html.append("<div class='slide'>")
            html.append("<div class='slide-title'>Slayd ${index + 1}</div>")
            html.append("<div class='slide-content'>")
            for (shape in slide.shapes) {
                if (shape is org.apache.poi.sl.usermodel.TextShape<*, *>) {
                    for (paragraph in shape.textParagraphs) {
                        val pText = paragraph.textRuns.joinToString("") { it.rawText ?: "" }
                        if (pText.isNotBlank()) { html.append("<p>").append(pText).append("</p>") }
                    }
                }
            }
            html.append("</div></div>")
        }
        slideShow.close()
        return html.toString()
    }

    fun showSearchDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Axtarılacaq söz..."
            setPadding(32, 32, 32, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Sənəddə Axtar")
            .setView(input)
            .setPositiveButton("Axtar") { _, _ ->
                val query = input.text.toString()
                if (query.isNotEmpty()) { webView.findAllAsync(query) }
            }
            .setNegativeButton("Ləğv et", null)
            .show()
    }

    override fun onThemeToggle() {
        webView.evaluateJavascript("""
            (function() {
                var body = document.body;
                if (body.style.backgroundColor === 'black' || body.style.backgroundColor === 'rgb(0, 0, 0)') {
                    body.style.backgroundColor = '#F3F4F6';
                    document.querySelectorAll('.docx, .slide, .paper').forEach(el => { el.style.backgroundColor = 'white'; el.style.color = '#333'; });
                } else {
                    body.style.backgroundColor = 'black';
                    document.querySelectorAll('.docx, .slide, .paper').forEach(el => { el.style.backgroundColor = '#1E1E1E'; el.style.color = 'white'; });
                }
            })();
        """.trimIndent(), null)
    }

    override fun onFontSizeChange(change: Int) {
        if (change == 0) currentFontSize = 16 else currentFontSize += change
        webView.evaluateJavascript("document.body.style.fontSize='${currentFontSize}px';", null)
    }
    
    private fun getFileName(uri: android.net.Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try { if (cursor != null && cursor.moveToFirst()) { val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (index != -1) result = cursor.getString(index) } } finally { cursor?.close() }
        }
        return result ?: uri.path?.substringAfterLast('/') ?: ""
    }
}
