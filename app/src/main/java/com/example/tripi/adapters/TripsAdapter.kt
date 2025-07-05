package com.example.tripi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tripi.R
import com.example.tripi.models.Trip
import com.bumptech.glide.Glide
import android.widget.ImageView

class TripsAdapter(
    private val trips: List<Trip>,
    private val onItemClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripsAdapter.TripViewHolder>() {

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripImage: ImageView = itemView.findViewById(R.id.tripImageView)
        private val tripName: TextView = itemView.findViewById(R.id.tripNameTextView)
        private val tripDescription: TextView = itemView.findViewById(R.id.tripDescriptionTextView)
        private val tripDuration: TextView = itemView.findViewById(R.id.tripDurationTextView)
        private val tripLocation: TextView = itemView.findViewById(R.id.tripLocationTextView)

        fun bind(trip: Trip) {
            tripName.text = trip.name
            tripDescription.text = trip.description
            tripDuration.text = "Duration: ${trip.durationMinutes} min"
            tripLocation.text = "Location: %.4f, %.4f".format(trip.lat, trip.lon)

            Glide.with(itemView.context)
                .load(trip.imageUrl)
                .placeholder(R.drawable.ic_trip_placeholder)
                .into(tripImage)

            itemView.setOnClickListener { onItemClick(trip) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    override fun getItemCount(): Int = trips.size
}