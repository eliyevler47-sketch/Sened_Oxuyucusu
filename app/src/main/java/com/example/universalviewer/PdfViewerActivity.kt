package com.example.universalviewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PdfViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scrollView = ScrollView(this)
        val linearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
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
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val imageView = ImageView(this)
                    imageView.setImageBitmap(bitmap)
                    linearLayout.addView(imageView)
                    page.close()
                }
                renderer.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
