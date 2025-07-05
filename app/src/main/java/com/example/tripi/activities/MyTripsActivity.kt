package com.example.tripi.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tripi.R
import com.example.tripi.adapters.TripsAdapter
import com.example.tripi.databinding.ActivityMyTripsBinding
import com.example.tripi.models.Trip
import com.google.firebase.Timestamp

class MyTripsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyTripsBinding
    private lateinit var tripsAdapter: TripsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupToolbar()
    }

    private fun setupRecyclerView() {
        tripsAdapter = TripsAdapter(getSampleTrips()) { trip ->
            Toast.makeText(this, "Selected: ${trip.name}", Toast.LENGTH_SHORT).show()
        }
        binding.tripsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyTripsActivity)
            adapter = tripsAdapter
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Trips"
    }

    private fun getSampleTrips(): List<Trip> {
        return listOf(
            Trip(
                name = "Trip to Paris",
                startDate = Timestamp.now(),
                imageRes = R.drawable.ic_trip_placeholder
            ),
            Trip(
                name = "Beach Vacation",
                startDate = Timestamp.now(),
                imageRes = R.drawable.ic_trip_placeholder
            )
        )
    }
}