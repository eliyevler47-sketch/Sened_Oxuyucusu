package com.example.universalviewer

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HtmlViewerActivity : BaseViewerActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "Mətn / HTML"

        val container = findViewById<LinearLayout>(R.id.contentContainer)
        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            settings.javaScriptEnabled = true
        }
        container.addView(webView)

        val uri = intent.data
        if (uri != null) {
            val fileName = intent.getStringExtra("FILE_NAME") ?: ""
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    if (fileName.endsWith(".txt", true)) {
                        val inputStream = contentResolver.openInputStream(uri)
                        val text = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                        inputStream?.close()
                        
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    body { background-color: #E0E0E0; padding: 15px; font-family: sans-serif; font-size: 16px;}
                                    .paper { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); color: #333; white-space: pre-wrap; line-height: 1.6; }
                                </style>
                            </head>
                            <body>
                                <div class="paper">${text.replace("<", "&lt;").replace(">", "&gt;")}</div>
                            </body>
                            </html>
                        """.trimIndent()

                        withContext(Dispatchers.Main) {
                            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            webView.loadUrl(uri.toString())
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@HtmlViewerActivity, "Fayl oxunarkən xəta: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
