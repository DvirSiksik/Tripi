package com.example.tripi.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tripi.R
import com.example.tripi.models.TripSearchResult

class TripSearchAdapter(
    private val trips: MutableList<TripSearchResult>,
    private val onAddClicked: (TripSearchResult) -> Unit
) : RecyclerView.Adapter<TripSearchAdapter.TripSearchViewHolder>() {

    fun updateData(newTrips: List<TripSearchResult>) {
        trips.clear()
        trips.addAll(newTrips)
        notifyDataSetChanged()
    }

    class TripSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tripNameTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.tripLocationTextView)
        val durationTextView: TextView = itemView.findViewById(R.id.tripDurationTextView)
        val ratingTextView: TextView = itemView.findViewById(R.id.tripRatingTextView)
        val imageView: ImageView = itemView.findViewById(R.id.tripImageView)
        val addButton: Button = itemView.findViewById(R.id.addTripButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripSearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_search_result, parent, false)
        return TripSearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripSearchViewHolder, position: Int) {
        val trip = trips[position]

        holder.nameTextView.text = trip.name
        holder.locationTextView.text = trip.location
        holder.durationTextView.text = "${trip.duration} min"
        holder.ratingTextView.text = trip.rating?.let { "★ ${"%.1f".format(it)}" } ?: ""

        trip.imageUrl?.let { url ->
            Glide.with(holder.itemView.context)
                .load(url)
                .placeholder(R.drawable.ic_placeholder)
                .into(holder.imageView)
        }

        holder.addButton.setOnClickListener {
            onAddClicked(trip)
        }
    }

    override fun getItemCount(): Int = trips.size
}