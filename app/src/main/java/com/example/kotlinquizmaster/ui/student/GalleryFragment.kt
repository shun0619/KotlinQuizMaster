package com.example.kotlinquizmaster.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.databinding.DialogImageBinding
import com.example.kotlinquizmaster.databinding.FragmentGalleryBinding
import com.example.kotlinquizmaster.databinding.ItemGalleryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Image gallery in a two-column grid. Long-press a tile to view it full size. */
class GalleryFragment : Fragment() {

    private data class Picture(val imageRes: Int, val caption: String)

    private val pictures = listOf(
        Picture(R.drawable.gallery_val, "val: read-only values"),
        Picture(R.drawable.gallery_var, "var: changing values"),
        Picture(R.drawable.gallery_if, "Conditions"),
        Picture(R.drawable.gallery_loop, "Loops"),
        Picture(R.drawable.gallery_fun, "Functions"),
        Picture(R.drawable.gallery_list, "Collections")
    )

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.galleryGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.galleryGrid.adapter = Adapter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showFullImage(picture: Picture) {
        val dialogBinding = DialogImageBinding.inflate(layoutInflater)
        dialogBinding.fullImage.setImageResource(picture.imageRes)
        dialogBinding.fullImage.contentDescription = picture.caption
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(picture.caption)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {
        inner class Holder(val binding: ItemGalleryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = pictures.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val picture = pictures[position]
            holder.binding.image.setImageResource(picture.imageRes)
            holder.binding.image.contentDescription = picture.caption
            holder.binding.caption.text = picture.caption
            holder.binding.root.setOnClickListener {
                Toast.makeText(requireContext(), getString(R.string.gallery_hint), Toast.LENGTH_SHORT).show()
            }
            // Event: long click
            holder.binding.root.setOnLongClickListener { showFullImage(picture); true }
        }
    }
}
