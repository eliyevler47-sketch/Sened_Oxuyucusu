package com.example.universalviewer

import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.appbar.MaterialToolbar

class ImageViewerActivity : BaseViewerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_viewer) SİLİNDİ

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "Şəkil"

        val container = findViewById<LinearLayout>(R.id.contentContainer)
        val photoView = PhotoView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        container.addView(photoView)
        intent.data?.let { uri: Uri -> photoView.setImageURI(uri) }
    }

    override fun onThemeToggle() {}
    override fun onFontSizeChange(change: Int) {}
}
