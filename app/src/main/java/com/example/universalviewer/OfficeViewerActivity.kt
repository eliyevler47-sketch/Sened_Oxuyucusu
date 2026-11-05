package com.example.universalviewer

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val fileName = getFileName(uri)
                        
                        when {
                            fileName.endsWith(".docx", true) -> {
                                val text = readWord(inputStream)
                                withContext(Dispatchers.Main) { showWord(text, container) }
                            }
                            fileName.endsWith(".xlsx", true) -> {
                                val text = readExcel(inputStream)
                                withContext(Dispatchers.Main) { showExcel(text, container) }
                            }
                            fileName.endsWith(".pptx", true) -> {
                                val slides = readPptx(inputStream)
                                withContext(Dispatchers.Main) { showPptx(slides, container) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        container.addView(TextView(this@OfficeViewerActivity).apply { text = "Xəta: ${e.message}"; setPadding(32,32,32,32) })
                    }
                }
            }
        }
    }

    // --- WORD OXUYUCUSU ---
    private fun readWord(inputStream: InputStream): String {
        val document = XWPFDocument(inputStream)
        val extractor = XWPFWordExtractor(document)
        val text = extractor.text
        extractor.close()
        return text
    }

    private fun showWord(text: String, container: LinearLayout) {
        val scrollView = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#E0E0E0")) }
        val textView = TextView(this).apply {
            this.text = text
            setPadding(48, 48, 48, 48)
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setLineSpacing(1.5f, 1.3f)
        }
        scrollView.addView(textView)
        container.addView(scrollView)
    }

    // --- EXCEL OXUYUCUSU (Formulları Hesablayır) ---
    private fun readExcel(inputStream: InputStream): String {
        val workbook = XSSFWorkbook(inputStream)
        val stringBuilder = StringBuilder("📊 EXCEL CƏDVƏLİ\n\n")
        val formatter = DataFormatter()
        // FORMULLARI HESABLAYAN ƏSAS HİSSƏ
        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        
        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            stringBuilder.append("📌 Vərəq: ${sheet.sheetName}\n\n")
            for (row in sheet) {
                for (cell in row) {
                    // formatter.formatCellValue(cell, evaluator) -> Formulu hesablayıb nəticəni göstərir
                    val cellValue = formatter.formatCellValue(cell, evaluator)
                    stringBuilder.append("$cellValue\t | \t")
                }
                stringBuilder.append("\n")
            }
            stringBuilder.append("\n──────────────────\n\n")
        }
        workbook.close()
        return stringBuilder.toString()
    }

    private fun showExcel(text: String, container: LinearLayout) {
        val scrollView = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#E0E0E0")) }
        val textView = TextView(this).apply {
            this.text = text
            setPadding(32, 32, 32, 32)
            textSize = 14f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setLineSpacing(1.3f, 1.3f)
            typeface = Typeface.MONOSPACE // Cədvəl düzgün görünsün deyə
        }
        scrollView.addView(textView)
        container.addView(scrollView)
    }

    // --- POWERPOINT OXUYUCUSU ---
    private fun readPptx(inputStream: InputStream): List<SpannableStringBuilder> {
        val slideShow = XMLSlideShow(inputStream)
        val slideList = mutableListOf<SpannableStringBuilder>()
        
        for ((index, slide) in slideShow.slides.withIndex()) {
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

                            if (run.isBold) spannableBuilder.setSpan(StyleSpan(Typeface.BOLD), start, spannableBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            if (run.isItalic) spannableBuilder.setSpan(StyleSpan(Typeface.ITALIC), start, spannableBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        if (paragraphText.isNotBlank()) spannableBuilder.append("\n")
                    }
                }
            }
            slideList.add(spannableBuilder)
        }
        slideShow.close()
        return slideList
    }

    private fun showPptx(slides: List<SpannableStringBuilder>, container: LinearLayout) {
        val recyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(this@OfficeViewerActivity)
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            setPadding(24, 24, 24, 24)
        }
        recyclerView.adapter = PptxAdapter(slides)
        container.addView(recyclerView)
    }

    class PptxAdapter(private val slides: List<SpannableStringBuilder>) : RecyclerView.Adapter<PptxAdapter.ViewHolder>() {
        class ViewHolder(val card: MaterialCardView) : RecyclerView.ViewHolder(card)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val card = MaterialCardView(parent.context).apply {
                radius = 16f
                cardElevation = 8f
                setContentPadding(32, 32, 32, 32)
                val params = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 32)
                layoutParams = params
                setCardBackgroundColor(Color.WHITE)
            }
            return ViewHolder(card)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val textView = TextView(holder.card.context).apply {
                text = slides[position]
                textSize = 16f
                setTextColor(Color.BLACK)
                setLineSpacing(1.4f, 1.4f)
            }
            holder.card.removeAllViews()
            holder.card.addView(textView)
        }

        override fun getItemCount() = slides.size
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
