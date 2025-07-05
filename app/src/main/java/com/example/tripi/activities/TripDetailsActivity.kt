package com.example.tripi.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tripi.R
import com.example.tripi.databinding.ActivityTripDetailsBinding
import com.example.tripi.models.Trip
import com.google.firebase.firestore.GeoPoint

class TripDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTripDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val trip = intent.getParcelableExtra<Trip>("TRIP")
        if (trip != null) {
            displayTripDetails(trip)
        } else {
            finish()
        }
    }

    private fun displayTripDetails(trip: Trip) {
        binding.collapsingToolbar.title = trip.name
        binding.tripNameTextView.text = trip.name
        binding.descriptionTextView.text = trip.description.ifBlank { "No description available." }

        // Load image
        Glide.with(this)
            .load(trip.imageUrl)
            .placeholder(R.drawable.ic_trip_placeholder)
            .into(binding.headerImageView)

        // Show static map with GeoPoint
        loadStaticMap(GeoPoint(trip.lat, trip.lon))
    }

    private fun loadStaticMap(location: GeoPoint) {
        val lat = location.latitude
        val lon = location.longitude
        val staticMapUrl = "https://maps.googleapis.com/maps/api/staticmap" +
                "?center=$lat,$lon" +
                "&zoom=13" +
                "&size=600x300" +
                "&markers=color:red%7C$lat,$lon" +
                "&key=YOUR_GOOGLE_MAPS_API_KEY"

        Glide.with(this)
            .load(staticMapUrl)
            .placeholder(R.drawable.ic_trip_placeholder)
            .into(binding.mapImageView)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}