package com.example.universalviewer.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.universalviewer.ImageToPdfActivity
import com.example.universalviewer.databinding.FragmentToolsBinding
import com.example.universalviewer.databinding.ItemToolBinding

class ToolsFragment : Fragment() {

    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val convertTools = listOf(
            ToolItem("Image to PDF", android.R.drawable.ic_menu_gallery, "#FFCA28"),
            ToolItem("Scan to PDF", android.R.drawable.ic_menu_camera, "#26C6DA"),
            ToolItem("Word to PDF", android.R.drawable.ic_menu_edit, "#42A5F5"),
            ToolItem("PDF to Word", android.R.drawable.ic_menu_save, "#EF5350")
        )

        val editTools = listOf(
            ToolItem("Edit text", android.R.drawable.ic_menu_edit, "#EF5350"),
            ToolItem("Annotate", android.R.drawable.ic_menu_agenda, "#FFCA28"),
            ToolItem("Add text", android.R.drawable.ic_menu_add, "#AB47BC"),
            ToolItem("Sign", android.R.drawable.ic_menu_edit, "#29B6F6"),
            ToolItem("Merge PDF", android.R.drawable.ic_menu_sort_by_size, "#42A5F5"),
            ToolItem("Split PDF", android.R.drawable.ic_menu_crop, "#66BB6A")
        )

        binding.rvConvertTools.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvConvertTools.adapter = ToolAdapter(convertTools) { name ->
            when(name) {
                "Image to PDF" -> {
                    // YENİ: Xüsusi Activity-ə yönləndiririk
                    startActivity(Intent(requireContext(), ImageToPdfActivity::class.java))
                }
                else -> Toast.makeText(requireContext(), "$name funksiyası tezliklə əlavə olunacaq!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvEditTools.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvEditTools.adapter = ToolAdapter(editTools) { name ->
            Toast.makeText(requireContext(), "$name funksiyası tezliklə əlavə olunacaq!", Toast.LENGTH_SHORT).show()
        }
    }

    data class ToolItem(val name: String, val iconRes: Int, val colorHex: String)

    inner class ToolAdapter(
        private val items: List<ToolItem>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<ToolAdapter.ViewHolder>() {
        inner class ViewHolder(val binding: ItemToolBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemToolBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvToolName.text = item.name
            holder.binding.ivToolIcon.setImageResource(item.iconRes)
            holder.binding.ivToolIcon.setColorFilter(Color.parseColor(item.colorHex), PorterDuff.Mode.SRC_IN)
            holder.binding.root.setOnClickListener { onClick(item.name) }
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
