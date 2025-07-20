package com.example.tripi.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tripi.R

class ImagePagerAdapter(
    private val images: MutableList<Uri> = mutableListOf(),
    private val onRemoveClick: ((position: Int) -> Unit)? = null
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val btnRemove: ImageView? = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_with_remove, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUri = images[position]

        Glide.with(holder.imageView.context)
            .load(imageUri)
            .centerCrop()
            .into(holder.imageView)

        holder.btnRemove?.setOnClickListener {
            onRemoveClick?.invoke(position)
        }
    }

    override fun getItemCount(): Int = images.size

    fun addImage(uri: Uri) {
        images.add(uri)
        notifyItemInserted(images.size - 1)
    }
    fun addImageFromUrl(url: String) {
        images.add(Uri.parse(url))
        notifyDataSetChanged()
    }
    fun removeImageUri(uri: Uri) {
        val removed = images.remove(uri)
        if (removed) {
            notifyDataSetChanged()
        }
    }
    fun removeImage(position: Int) {
        if (position in 0 until images.size) {
            images.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getImages(): List<Uri> = images.toList()
}