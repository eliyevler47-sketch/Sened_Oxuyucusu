package com.example.universalviewer

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class HtmlViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE) // Ağ fon
        }
        setContentView(webView)
        
        intent.data?.let { uri -> 
            // Faylı düzgün yükləmək üçün
            webView.loadUrl(uri.toString()) 
        }
    }
}
