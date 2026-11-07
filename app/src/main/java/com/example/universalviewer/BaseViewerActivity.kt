package com.example.universalviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.universalviewer.utils.DocumentRouter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

abstract class BaseViewerActivity : AppCompatActivity() {
    protected lateinit var prefManager: PreferenceManager
    private var startTime: Long = 0
    private var currentUriString: String? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            DocumentRouter.routeFile(this, uri)
            finish() 
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer) 
        
        prefManager = PreferenceManager(this)
        currentUriString = intent.data?.toString()
        
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

        onFontSizeChange(prefManager.getFontSize())
    }

    override fun onResume() {
        super.onResume()
        startTime = System.currentTimeMillis()
    }

    override fun onPause() {
        super.onPause()
        // Xüsusi SƏNƏD ÜÇÜN vaxtı yadda saxlayırıq
        if (startTime > 0 && currentUriString != null) {
            val timeSpent = System.currentTimeMillis() - startTime
            prefManager.addDocumentUsageTime(currentUriString!!, timeSpent)
            startTime = 0
        }
    }

    private fun showSettingsDialog() {
        val options = arrayOf(
            "➕ Şrifti Daha da Böyüt",
            "➕ Şrifti Böyüt",
            "🔄 Standart Şrift",
            "➖ Şrifti Kiçilt",
            "➖ Şrifti Daha da Kiçilt"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Sənəd Parametrləri")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> changeFontSize(2) 
                    1 -> changeFontSize(1) 
                    2 -> changeFontSize(0) 
                    3 -> changeFontSize(-1)
                    4 -> changeFontSize(-2)
                }
            }.show()
    }

    private fun changeFontSize(level: Int) {
        prefManager.setFontSize(level)
        onFontSizeChange(level)
    }

    abstract fun onFontSizeChange(level: Int)
}
