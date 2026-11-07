package com.example.universalviewer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.universalviewer.PreferenceManager
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

        setupFontSizeSlider()
        setupRecentSettings()
        setupAbout()
    }

    override fun onResume() {
        super.onResume()
        binding.tvRecentCount.text = "${prefManager.getDocumentList().size} fayl"
    }

    private fun setupFontSizeSlider() {
        // -2, -1, 0, 1, 2 dəyərlərini 0, 1, 2, 3, 4 ilə əlaqələndiririk
        val currentLevel = prefManager.getFontSize()
        binding.seekBarFont.progress = currentLevel + 2

        binding.seekBarFont.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newLevel = progress - 2
                    prefManager.setFontSize(newLevel)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun setupRecentSettings() {
        binding.btnClearRecent.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Son sənədləri təmizlə")
                .setMessage("Bütün son açılan sənədlər və onların oxunma statistikası silinəcək. Davam edilsin?")
                .setPositiveButton("Bəli") { _, _ ->
                    prefManager.clearRecent()
                    binding.tvRecentCount.text = "0 fayl"
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
