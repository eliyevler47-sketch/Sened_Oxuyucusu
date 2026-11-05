package com.example.universalviewer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
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

        val recyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(this@PdfViewerActivity)
            setBackgroundColor(Color.parseColor("#4A4A4A")) // Tünd boz fon
            setPadding(24, 24, 24, 24)
            clipToPadding = false 
        }
        container.addView(recyclerView)

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val file = File(cacheDir, "temp_view.pdf")
                    contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    
                    val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fileDescriptor)
                    val bitmaps = mutableListOf<Bitmap>()
                    
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val scale = resources.displayMetrics.density * 2.5f
                        val width = (page.width * scale).toInt()
                        val height = (page.height * scale).toInt()
                        
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE) 
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bitmap)
                        page.close()
                    }
                    renderer.close()
                    fileDescriptor.close()

                    withContext(Dispatchers.Main) {
                        recyclerView.adapter = PdfProAdapter(bitmaps)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    class PdfProAdapter(private val bitmaps: List<Bitmap>) : RecyclerView.Adapter<PdfProAdapter.ViewHolder>() {
        
        class ViewHolder(val cardView: MaterialCardView, val photoView: PhotoView) : RecyclerView.ViewHolder(cardView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context
            
            val cardView = MaterialCardView(context).apply {
                radius = 8f
                cardElevation = 12f
                val params = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 32) // Səhifələr arası boşluq
                layoutParams = params
                setCardBackgroundColor(Color.WHITE)
            }

            val photoView = PhotoView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER 
                adjustViewBounds = true
            }

            cardView.addView(photoView)
            return ViewHolder(cardView, photoView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.photoView.setImageBitmap(bitmaps[position])
        }

        override fun getItemCount() = bitmaps.size
    }
}
