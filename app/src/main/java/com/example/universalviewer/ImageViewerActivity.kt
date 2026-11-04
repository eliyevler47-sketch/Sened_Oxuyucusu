package com.example.universalviewer

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.chrisbanes.photoview.PhotoView

class ImageViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoView = PhotoView(this)
        setContentView(photoView)
        intent.data?.let { uri: Uri -> photoView.setImageURI(uri) }
    }
}
