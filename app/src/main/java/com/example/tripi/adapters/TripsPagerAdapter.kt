package com.example.tripi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tripi.databinding.ItemTripPagerBinding
import com.example.tripi.models.Trip
import com.example.tripi.R

class TripsPagerAdapter(
    private val trips: List<Trip>,
    private val onItemClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripsPagerAdapter.TripViewHolder>() {

    inner class TripViewHolder(val binding: ItemTripPagerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripPagerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]

        with(holder.binding) {
            tripName.text = trip.name
            tripDescription.text = trip.description
            tripCategories.text = trip.categories.take(3).joinToString(" • ")

            val imageUrl = trip.imageUrls.firstOrNull()
            Glide.with(root.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error_image)
                .into(tripImageView)

            root.setOnClickListener { onItemClick(trip) }
        }
    }

    override fun getItemCount(): Int = trips.size
}