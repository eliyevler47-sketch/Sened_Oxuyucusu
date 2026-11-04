package com.example.universalviewer

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

class OfficeViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply { setPadding(32, 32, 32, 32); textSize = 16f }
        val scrollView = ScrollView(this)
        scrollView.addView(textView)
        setContentView(scrollView)

        val uri = intent.data
        if (uri != null) {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val document = XWPFDocument(inputStream)
                    val extractor = XWPFWordExtractor(document)
                    textView.text = extractor.text
                    extractor.close()
                }
            } catch (e: Exception) {
                textView.text = "Fayl oxunarkən xəta baş verdi: ${e.message}"
            }
        }
    }
}
