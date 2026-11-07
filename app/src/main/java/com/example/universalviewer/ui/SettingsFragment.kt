package com.example.universalviewer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.universalviewer.PreferenceManager
import com.example.universalviewer.R
import com.example.universalviewer.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PreferenceManager(requireContext())

        setupUsageTime()
        setupFontSize()
        setupRecentSettings()
        setupAbout()
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun setupUsageTime() {
        binding.btnResetUsage.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Statistikanı sıfırla")
                .setMessage("Ümumi istifadə vaxtı sıfırlanacaq. Davam edilsin?")
                .setPositiveButton("Bəli") { _, _ ->
                    prefManager.resetUsageTime()
                    refreshStats()
                }
                .setNegativeButton("Ləğv et", null)
                .show()
        }
    }

    private fun refreshStats() {
        binding.tvUsageTime.text = formatUsage(prefManager.getTotalUsageMs())
        binding.tvRecentCount.text = "${prefManager.getDocumentList().size} fayl"
    }

    private fun formatUsage(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return when {
            h > 0 -> "$h saat $m dəq $s san"
            m > 0 -> "$m dəq $s san"
            else -> "$s san"
        }
    }

    private fun setupFontSize() {
        when (prefManager.getFontSize()) {
            0 -> binding.radioSmall.isChecked = true
            2 -> binding.radioLarge.isChecked = true
            else -> binding.radioMedium.isChecked = true
        }

        binding.radioGroupFont.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.radioSmall -> 0
                R.id.radioLarge -> 2
                else -> 1
            }
            prefManager.setFontSize(value)
        }
    }

    private fun setupRecentSettings() {
        binding.btnClearRecent.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Son sənədləri təmizlə")
                .setMessage("Bütün son açılan sənədlər silinəcək. Davam edilsin?")
                .setPositiveButton("Bəli") { _, _ ->
                    prefManager.clearRecent()
                    refreshStats()
                }
                .setNegativeButton("Ləğv et", null)
                .show()
        }
    }

    private fun setupAbout() {
        val version = try {
            requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: Exception) { "1.0" }
        binding.tvVersion.text = "Versiya $version"
        binding.tvAppName.text = "Universal Viewer"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
