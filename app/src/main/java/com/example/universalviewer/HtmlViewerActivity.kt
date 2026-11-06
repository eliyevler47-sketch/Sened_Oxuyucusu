package com.example.universalviewer

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import com.google.android.material.appbar.MaterialToolbar

class HtmlViewerActivity : BaseViewerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "Mətn / HTML"

        val container = findViewById<LinearLayout>(R.id.contentContainer)
        val webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.WHITE)
        }
        container.addView(webView)
        intent.data?.let { uri -> webView.loadUrl(uri.toString()) }
    }

    // Yalnız bunu saxlayırıq
    override fun onFontSizeChange(change: Int) {}
}
