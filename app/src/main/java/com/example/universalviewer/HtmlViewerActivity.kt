package com.example.universalviewer

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class HtmlViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val container = findViewById<LinearLayout>(R.id.contentContainer)

        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "Mətn / HTML"
        toolbar.setNavigationOnClickListener { finish() }

        val webView = WebView(this).apply { setBackgroundColor(Color.WHITE) }
        container.addView(webView)

        intent.data?.let { uri -> webView.loadUrl(uri.toString()) }
    }
}
