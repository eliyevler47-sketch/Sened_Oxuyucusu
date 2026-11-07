package com.example.universalviewer.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.universalviewer.DocumentItem
import com.example.universalviewer.PreferenceManager
import com.example.universalviewer.databinding.FragmentRecentBinding
import com.example.universalviewer.utils.DocumentRouter

class RecentFragment : Fragment() {

    private var _binding: FragmentRecentBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PreferenceManager
    private lateinit var adapter: RecentAdapter

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            DocumentRouter.routeFile(requireContext(), uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PreferenceManager(requireContext())

        adapter = RecentAdapter(
            onOpen = { item -> DocumentRouter.routeFile(requireContext(), Uri.parse(item.uri)) },
            onPin = { item ->
                prefManager.togglePin(item.uri)
                loadData()
            },
            onFavorite = { item ->
                prefManager.toggleFavorite(item.uri)
                loadData()
            },
            onDelete = { item ->
                prefManager.removeRecent(item.uri)
                loadData()
            }
        )

        binding.recyclerRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecent.adapter = adapter

        // Boş ekrana basanda fayl seçmə pəncərəsini aç
        binding.emptyState.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val list = prefManager.getSortedDocumentList()
        if (list.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerRecent.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerRecent.visibility = View.VISIBLE
        }
        adapter.submitList(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerRecent.adapter = null
        _binding = null
    }
}
