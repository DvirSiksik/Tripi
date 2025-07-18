package com.example.tripi.adapters

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

class SelectedTripsAdapter(
    private val trips: List<TripSearchResult>,
    private val onRemoveClicked: (TripSearchResult) -> Unit
) : RecyclerView.Adapter<SelectedTripsAdapter.SelectedTripViewHolder>() {

    class SelectedTripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tripNameTextView)
        val durationTextView: TextView = itemView.findViewById(R.id.tripDurationTextView)
        val imageView: ImageView = itemView.findViewById(R.id.tripImageView)
        val removeButton: Button = itemView.findViewById(R.id.removeTripButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedTripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_trip, parent, false)
        return SelectedTripViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectedTripViewHolder, position: Int) {
        val trip = trips[position]

        holder.nameTextView.text = trip.name
        holder.durationTextView.text = "${trip.duration} min"

        trip.imageUrl?.let { url ->
            Glide.with(holder.itemView.context)
                .load(url)
                .placeholder(R.drawable.ic_placeholder)
                .into(holder.imageView)
        }

        holder.removeButton.setOnClickListener {
            onRemoveClicked(trip)
        }
    }

    override fun getItemCount(): Int = trips.size
}