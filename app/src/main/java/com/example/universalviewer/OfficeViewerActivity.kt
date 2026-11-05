package com.example.universalviewer

import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class OfficeViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val container = findViewById<LinearLayout>(R.id.contentContainer)

        toolbar.title = intent.getStringExtra("FILE_NAME") ?: "Sənəd"
        toolbar.setNavigationOnClickListener { finish() }

        val progressBar = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = android.view.Gravity.CENTER }
        }
        container.gravity = android.view.Gravity.CENTER
        container.addView(progressBar)

        val webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#F3F4F6"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
        }

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val fileName = getFileName(uri).lowercase()
                        
                        // Faylı oxuyub Base64-ə çeviririk
                        val bytes = inputStream.readBytes()
                        val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        inputStream.close()

                        withContext(Dispatchers.Main) {
                            container.removeView(progressBar)
                            container.gravity = android.view.Gravity.NO_GRAVITY
                            container.addView(webView)

                            if (fileName.endsWith(".docx")) {
                                // HTML faylını yükləyirik
                                webView.loadUrl("file:///android_asset/viewer.html")
                                
                                // HTML yüklənəndən sonra JS funksiyasını çağırıb məlumatı ötürürük
                                webView.webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        val jsCode = "renderDocx('$base64String');"
                                        webView.evaluateJavascript(jsCode, null)
                                    }
                                }
                            } else {
                                webView.loadData("<h3 style='text-align:center; padding:20px; font-family:sans-serif;'>Hazırda yalnız Word (.docx) formatı dəstəklənir.</h3>", "text/html", "UTF-8")
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        container.removeView(progressBar)
                        container.addView(TextView(this@OfficeViewerActivity).apply { 
                            text = "Xəta: ${e.message}" 
                            setPadding(32,32,32,32) 
                        })
                    }
                }
            }
        }
    }

    private fun getFileName(uri: android.net.Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            } finally { cursor?.close() }
        }
        return result ?: uri.path?.substringAfterLast('/') ?: ""
    }
}
