package com.example.universalviewer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.universalviewer.databinding.ActivityMainBinding
import com.example.universalviewer.utils.DocumentRouter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefManager: PreferenceManager
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)

        // Xaricdən gələn faylı yoxla (Tətbiq bağlıykən açılıbsa)
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Xaricdən gələn faylı yoxla (Tətbiq açıqkən fayl seçilibsə)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                // Faylı dərhal müvafiq viewer-ə yönləndir
                DocumentRouter.routeFile(this, uri)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startTime = System.currentTimeMillis()
    }

    override fun onPause() {
        super.onPause()
        if (startTime > 0) {
            prefManager.addUsageTime(System.currentTimeMillis() - startTime)
            startTime = 0
        }
    }
}
