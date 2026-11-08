package com.example.universalviewer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        binding.btnClearRecent.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Son sənədləri təmizlə")
                .setMessage("Bütün son açılan sənədlər silinəcək. Davam edilsin?")
                .setPositiveButton("Bəli") { _, _ ->
                    prefManager.clearRecent()
                    Toast.makeText(requireContext(), "Təmizləndi!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Ləğv et", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
