package com.example.universalviewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PdfViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Arxa fon tünd, səhifələr ağ olacaq
        val scrollView = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#1E1E1E")) }
        val linearLayout = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        scrollView.addView(linearLayout)
        setContentView(scrollView)

        val uri = intent.data
        if (uri != null) {
            try {
                val file = File(cacheDir, "temp.pdf")
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fileDescriptor)

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    // Yüksək keyfiyyət üçün ölçünü 2 dəfə böyüdürük
                    val width = page.width * 2
                    val height = page.height * 2
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    
                    // Ağ fon (PDF üçün çox vacibdir)
                    bitmap.eraseColor(Color.WHITE)
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    val imageView = ImageView(this).apply {
                        setImageBitmap(bitmap)
                        setBackgroundColor(Color.WHITE)
                        // Səhifələr arası boşluq
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 0, 0, 24)
                        layoutParams = params
                    }
                    
                    linearLayout.addView(imageView)
                    page.close()
                }
                renderer.close()
            } catch (e: Exception) {
                val errorText = TextView(this).apply { text = "Xəta: ${e.message}"; setTextColor(Color.WHITE) }
                linearLayout.addView(errorText)
            }
        }
    }
}
