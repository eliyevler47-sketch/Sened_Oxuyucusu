package com.example.universalviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

abstract class BaseViewerActivity : AppCompatActivity() {
    protected lateinit var prefManager: PreferenceManager
    private var startTime: Long = 0

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            // YENİ SƏNƏD AÇANDA ADINI DÜZGÜN ÖTÜRÜRÜK
            val fileName = getFileName(uri)
            prefManager.saveRecentFile(uri, fileName)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer) 
        
        prefManager = PreferenceManager(this)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.viewer_menu)
        
        toolbar.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.action_search -> { 
                    if (this is OfficeViewerActivity) this.showSearchDialog()
                    if (this is PdfViewerActivity) this.showSearchDialog()
                    true 
                }
                R.id.action_open_new -> { 
                    filePickerLauncher.launch(arrayOf("*/*"))
                    true 
                }
                else -> false
            }
        }
        toolbar.setNavigationOnClickListener { finish() }

        val fab = findViewById<FloatingActionButton>(R.id.fabSettings)
        fab.setOnClickListener { showSettingsDialog() }
    }

    override fun onResume() {
        super.onResume()
        startTime = System.currentTimeMillis()
    }

    override fun onPause() {
        super.onPause()
        if (startTime > 0) {
            prefManager.addUsageTime(System.currentTimeMillis() - startTime)
        }
    }

    private fun showSettingsDialog() {
        val options = arrayOf(
            "🔤 Şrifti Böyüt",
            "🔤 Şrifti Kiçilt",
            "🔤 Standart Şrift",
            "⏱️ Sərf Edilən Vaxt: ${prefManager.getTotalUsageTime()}"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Sənəd Parametrləri")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onFontSizeChange(2)
                    1 -> onFontSizeChange(-2)
                    2 -> onFontSizeChange(0)
                }
            }.show()
    }

    abstract fun onFontSizeChange(change: Int)
    
    // Köməkçi funksiya (Base daxilində)
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try { if (cursor != null && cursor.moveToFirst()) { val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (index != -1) result = cursor.getString(index) } } finally { cursor?.close() }
        }
        return result ?: "Yeni Sənəd"
    }
}
