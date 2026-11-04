package com.example.universalviewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
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
        val recyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(this@PdfViewerActivity)
            setBackgroundColor(Color.parseColor("#333333")) // Tünd fon
        }
        
        // activity_viewer.xml-dəki container-i tapırıq
        val container = findViewById<android.widget.LinearLayout>(R.id.contentContainer)
        container.addView(recyclerView)

        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "PDF Sənədi"
        toolbar.setNavigationOnClickListener { finish() }

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
                    
                    val bitmaps = mutableListOf<Bitmap>()
                    
                    // Bütün səhifələri çəkirik
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val scale = resources.displayMetrics.density * 1.5f
                        val width = (page.width * scale).toInt()
                        val height = (page.height * scale).toInt()
                        
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bitmap)
                        page.close()
                    }
                    renderer.close()

                    withContext(Dispatchers.Main) {
                        recyclerView.adapter = PdfPageAdapter(bitmaps)
                    }
                } catch (e: Exception) {
                    // Xəta halı
                }
            }
        }
    }

    // PDF səhifələrini göstərən Adapter
    class PdfPageAdapter(private val bitmaps: List<Bitmap>) : RecyclerView.Adapter<PdfPageAdapter.ViewHolder>() {
        class ViewHolder(val photoView: PhotoView) : RecyclerView.ViewHolder(photoView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val photoView = PhotoView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(20, 20, 20, 20)
            }
            return ViewHolder(photoView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.photoView.setImageBitmap(bitmaps[position])
        }

        override fun getItemCount() = bitmaps.size
    }
}
