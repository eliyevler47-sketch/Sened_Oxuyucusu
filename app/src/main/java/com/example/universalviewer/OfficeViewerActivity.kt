package com.example.universalviewer

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
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
            setBackgroundColor(Color.parseColor("#E0E0E0")) // Arxa fon boz
        }
        
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(contentLayout)
        container.addView(scrollView)

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val fileName = getFileName(uri)
                        
                        withContext(Dispatchers.Main) {
                            when {
                                fileName.endsWith(".docx", true) -> {
                                    contentLayout.addView(createWordView(inputStream))
                                }
                                fileName.endsWith(".xlsx", true) -> {
                                    contentLayout.addView(createExcelView(inputStream))
                                }
                                fileName.endsWith(".pptx", true) -> {
                                    createPptxView(inputStream, contentLayout)
                                }
                                else -> {
                                    contentLayout.addView(TextView(this@OfficeViewerActivity).apply { text = "Dəstəklənmir" })
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        contentLayout.addView(TextView(this@OfficeViewerActivity).apply { text = "Xəta: ${e.message}" })
                    }
                }
            }
        }
    }

    // WORD OXUYUCUSU
    private fun createWordView(inputStream: InputStream): TextView {
        val document = XWPFDocument(inputStream)
        val extractor = XWPFWordExtractor(document)
        val text = extractor.text
        extractor.close()
        
        return TextView(this).apply {
            this.text = text
            setPadding(48, 48, 48, 48)
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setLineSpacing(1.5f, 1.3f)
        }
    }

    // EXCEL OXUYUCUSU
    private fun createExcelView(inputStream: InputStream): TextView {
        val workbook = XSSFWorkbook(inputStream)
        val stringBuilder = StringBuilder("📊 EXCEL CƏDVƏLİ\n\n")
        val formatter = DataFormatter()
        
        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            stringBuilder.append("📌 Vərəq: ${sheet.sheetName}\n\n")
            for (row in sheet) {
                for (cell in row) {
                    stringBuilder.append("${formatter.formatCellValue(cell)}\t | \t")
                }
                stringBuilder.append("\n")
            }
            stringBuilder.append("\n──────────────────\n\n")
        }
        workbook.close()

        return TextView(this).apply {
            this.text = stringBuilder.toString()
            setPadding(48, 48, 48, 48)
            textSize = 14f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setLineSpacing(1.3f, 1.3f)
        }
    }

    // POWERPOINT OXUYUCUSU (Rənglər çıxarıldı, amma Qalın və Maili yazılar qalır)
    private fun createPptxView(inputStream: InputStream, container: LinearLayout) {
        val slideShow = XMLSlideShow(inputStream)
        
        for ((index, slide) in slideShow.slides.withIndex()) {
            val card = MaterialCardView(this).apply {
                radius = 16f
                cardElevation = 8f
                setContentPadding(32, 32, 32, 32)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 32)
                layoutParams = params
                setCardBackgroundColor(Color.WHITE)
            }

            val slideText = TextView(this).apply {
                textSize = 16f
                setTextColor(Color.BLACK)
                setLineSpacing(1.4f, 1.4f)
            }

            val spannableBuilder = SpannableStringBuilder()
            spannableBuilder.append("📌 Slayd ${index + 1}\n\n")

            for (shape in slide.shapes) {
                if (shape is org.apache.poi.sl.usermodel.TextShape<*, *>) {
                    for (paragraph in shape.textParagraphs) {
                        var paragraphText = ""
                        for (run in paragraph.textRuns) {
                            val start = spannableBuilder.length
                            val text = run.rawText ?: ""
                            spannableBuilder.append(text)
                            paragraphText += text

                            // Şrift stilləri (Qalın, Maili) - Rəng hissəsi SİLİNDİ
                            if (run.isBold) {
                                spannableBuilder.setSpan(StyleSpan(Typeface.BOLD), start, spannableBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            if (run.isItalic) {
                                spannableBuilder.setSpan(StyleSpan(Typeface.ITALIC), start, spannableBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                        }
                        if (paragraphText.isNotBlank()) {
                            spannableBuilder.append("\n")
                        }
                    }
                }
            }

            slideText.text = spannableBuilder
            card.addView(slideText)
            container.addView(card)
        }
        slideShow.close()
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
