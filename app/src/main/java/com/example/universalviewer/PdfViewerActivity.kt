package com.example.universalviewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
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
        val container = findViewById<android.widget.LinearLayout>(R.id.contentContainer)
        
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "PDF Sənədi"
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(this@PdfViewerActivity)
            setBackgroundColor(Color.parseColor("#2C2C2C")) // Tünd fon
        }
        container.addView(recyclerView)

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Faylı müvəqqəti yaddaşa (cache) kopyalayırıq
                    val file = File(cacheDir, "temp_view.pdf")
                    contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    
                    val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fileDescriptor)
                    val bitmaps = mutableListOf<Bitmap>()
                    
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        // Səhifələri yüksək keyfiyyətlə çəkmək üçün ölçünü böyüdürük
                        val scale = resources.displayMetrics.density * 2
                        val width = (page.width * scale).toInt()
                        val height = (page.height * scale).toInt()
                        
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        // AĞ FON MÜTLƏQ LAZIMDIR, YOXSA QARA GÖRÜNÜR
                        bitmap.eraseColor(Color.WHITE) 
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bitmap)
                        page.close()
                    }
                    renderer.close()
                    fileDescriptor.close()

                    withContext(Dispatchers.Main) {
                        recyclerView.adapter = PdfAdapter(bitmaps)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    class PdfAdapter(private val bitmaps: List<Bitmap>) : RecyclerView.Adapter<PdfAdapter.ViewHolder>() {
        class ViewHolder(val photoView: PhotoView) : RecyclerView.ViewHolder(photoView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val photoView = PhotoView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(20, 20, 20, 20)
                setBackgroundColor(Color.WHITE)
            }
            return ViewHolder(photoView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.photoView.setImageBitmap(bitmaps[position])
        }

        override fun getItemCount() = bitmaps.size
    }
}
