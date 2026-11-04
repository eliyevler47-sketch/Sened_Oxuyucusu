package com.example.universalviewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val container = findViewById<LinearLayout>(R.id.contentContainer)
        
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "PDF Sənədi"
        toolbar.setNavigationOnClickListener { finish() }

        val progressBar = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }
        container.gravity = Gravity.CENTER
        container.addView(progressBar)

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val file = File(cacheDir, "temp.pdf")
                    contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fileDescriptor)
                    
                    withContext(Dispatchers.Main) {
                        container.removeView(progressBar)
                        container.setPadding(24, 24, 24, 24)
                        container.gravity = Gravity.CENTER_HORIZONTAL
                        container.setBackgroundColor(Color.parseColor("#333333")) // Tünd fon
                    }

                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        // Keyfiyyəti artırmaq üçün ekran sıxlığına vururuq
                        val scale = resources.displayMetrics.density * 2
                        val width = (page.width * scale).toInt()
                        val height = (page.height * scale).toInt()
                        
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE) // Səhifə ağ olmalıdır
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        
                        withContext(Dispatchers.Main) {
                            val imageView = ImageView(this@PdfViewerActivity).apply {
                                setImageBitmap(bitmap)
                                setBackgroundColor(Color.WHITE)
                                val params = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                params.setMargins(0, 0, 0, 24) // Səhifələr arası boşluq
                                layoutParams = params
                            }
                            container.addView(imageView)
                        }
                        page.close()
                    }
                    renderer.close()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        container.removeView(progressBar)
                        container.addView(TextView(this@PdfViewerActivity).apply { 
                            text = "Xəta: ${e.message}" 
                            setTextColor(Color.WHITE)
                            textSize = 18f
                            setPadding(32,32,32,32)
                        })
                    }
                }
            }
        }
    }
}
