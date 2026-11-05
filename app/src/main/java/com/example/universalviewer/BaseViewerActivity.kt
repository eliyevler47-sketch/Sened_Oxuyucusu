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
            prefManager.saveRecentFile(uri)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ÇÖKMƏNİN QARŞISINI ALAN HİSSƏ: Dizaynı burada yükləyirik
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
                    filePickerLauncher.launch(arrayOf(
                        "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "text/plain", "text/html", "image/*"
                    ))
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
            val timeSpent = System.currentTimeMillis() - startTime
            prefManager.addUsageTime(timeSpent)
        }
    }

    private fun showSettingsDialog() {
        val options = arrayOf(
            "🌙 Gecə/Gündüz Rejimi",
            "🔤 Şrifti Böyüt",
            "🔤 Şrifti Kiçilt",
            "🔤 Standart Şrift",
            "⏱️ Sərf Edilən Vaxt: ${prefManager.getTotalUsageTime()}"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Sənəd Parametrləri")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onThemeToggle()
                    1 -> onFontSizeChange(2)
                    2 -> onFontSizeChange(-2)
                    3 -> onFontSizeChange(0)
                }
            }.show()
    }

    abstract fun onThemeToggle()
    abstract fun onFontSizeChange(change: Int)
}
