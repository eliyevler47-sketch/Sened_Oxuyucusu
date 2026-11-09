package com.example.universalviewer

import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.io.FileOutputStream

class ImageToPdfActivity : AppCompatActivity() {

    private val selectedImages = mutableListOf<Uri>()
    private lateinit var adapter: ImagePreviewAdapter
    private lateinit var pdfDocument: PdfDocument

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImages.addAll(uris)
            adapter.notifyDataSetChanged()
        }
    }

    private val savePdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        if (uri != null && ::pdfDocument.isInitialized) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                Toast.makeText(this, "PDF uğurla yadda saxlanıldı!", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Xəta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (::pdfDocument.isInitialized) pdfDocument.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Sadə UI (XML yaratmadan Kotlin ilə)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "Image to PDF"
            setNavigationIcon(android.R.drawable.ic_menu_revert)
            setNavigationOnClickListener { finish() }
        }
        layout.addView(toolbar)

        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ImageToPdfActivity)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        adapter = ImagePreviewAdapter(selectedImages)
        recyclerView.adapter = adapter
        layout.addView(recyclerView)

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        val addBtn = Button(this).apply {
            text = "Şəkil Əlavə Et"
            setOnClickListener { pickImagesLauncher.launch("image/*") }
        }
        buttonLayout.addView(addBtn)

        val saveBtn = Button(this).apply {
            text = "PDF Yarat və Saxla"
            setOnClickListener { createAndSavePdf() }
        }
        buttonLayout.addView(saveBtn)

        layout.addView(buttonLayout)
        setContentView(layout)
    }

    private fun createAndSavePdf() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Zəhmət olmasa şəkil seçin!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            pdfDocument = PdfDocument()
            var pageNumber = 1
            for (uri in selectedImages) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                pageNumber++
            }
            savePdfLauncher.launch("Yeni_Sened.pdf")
        } catch (e: Exception) {
            Toast.makeText(this, "Xəta: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Sadə adapter (sadəcə şəkillərin adını göstərir)
    inner class ImagePreviewAdapter(private val items: List<Uri>) : RecyclerView.Adapter<ImagePreviewAdapter.VH>() {
        inner class VH(val textView: TextView) : RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply { setPadding(16, 16, 16, 16); textSize = 16f }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.textView.text = "Şəkil ${position + 1}: ${items[position].lastPathSegment}"
        }

        override fun getItemCount() = items.size
    }
}
