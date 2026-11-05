package com.example.universalviewer

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class OfficeViewerActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val container = findViewById<LinearLayout>(R.id.contentContainer)
        val fabSettings = findViewById<FloatingActionButton>(R.id.fabSettings)

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

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#F3F4F6"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
        }

        // SÖZ AXTARIŞI (Yuxarıdaki Ayarlar Düyməsinə basanda)
        fabSettings.setOnClickListener {
            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_TEXT
                hint = "Axtarılacaq söz..."
                setPadding(32, 32, 32, 32)
            }
            AlertDialog.Builder(this)
                .setTitle("Sənəddə Axtar")
                .setView(input)
                .setPositiveButton("Axtar") { _, _ ->
                    val query = input.text.toString()
                    if (query.isNotEmpty()) {
                        webView.findAllAsync(query) // Sözü tapır və sarı rənglə işarələyir
                    }
                }
                .setNegativeButton("Ləğv et", null)
                .show()
        }

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val fileName = getFileName(uri).lowercase()
                        val bytes = inputStream.readBytes()
                        val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        inputStream.close()

                        withContext(Dispatchers.Main) {
                            container.removeView(progressBar)
                            container.gravity = android.view.Gravity.NO_GRAVITY
                            container.addView(webView)

                            if (fileName.endsWith(".docx")) {
                                webView.loadUrl("file:///android_asset/viewer.html")
                                webView.webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        webView.evaluateJavascript("renderDocx('$base64String');", null)
                                    }
                                }
                            } else {
                                webView.loadData("<h3 style='text-align:center;'>Hazırda yalnız Word (.docx) dəstəklənir.</h3>", "text/html", "UTF-8")
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        container.removeView(progressBar)
                        container.addView(android.widget.TextView(this@OfficeViewerActivity).apply { text = "Xəta: ${e.message}"; setPadding(32,32,32,32) })
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
