package com.example.tripi.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tripi.adapters.TripsAdapter
import com.example.tripi.databinding.ActivityMyTripsBinding
import com.example.tripi.models.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyTripsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyTripsBinding
    private lateinit var tripsAdapter: TripsAdapter
    private val tripsList = mutableListOf<Trip>()
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupToolbar()
        loadUserTrips()
    }

    private fun setupRecyclerView() {
        tripsAdapter = TripsAdapter(tripsList) { trip ->
            Toast.makeText(this, "Selected: ${trip.name}", Toast.LENGTH_SHORT).show()
            // אפשר לפתוח TripDetailsActivity בהמשך
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

    private fun loadUserTrips() {
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val tripsRef = db.collection("trips")
        val userTrips = mutableListOf<Trip>()


        tripsRef.whereEqualTo("creatorId", userId)
            .get()
            .addOnSuccessListener { createdDocs ->
                for (doc in createdDocs) {
                    userTrips.add(doc.toObject(Trip::class.java))
                }


                tripsRef.whereArrayContainsAny("sharedWith", listOf(userId, FirebaseAuth.getInstance().currentUser?.email))
                    .get()
                    .addOnSuccessListener { sharedDocs ->
                        for (doc in sharedDocs) {
                            val trip = doc.toObject(Trip::class.java)
                            if (!userTrips.any { it.id == trip.id }) {
                                userTrips.add(trip)
                            }
                        }
                        tripsList.clear()
                        tripsList.addAll(userTrips)
                        tripsAdapter.notifyDataSetChanged()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MyTripsActivity", "Error fetching trips", e)
                Toast.makeText(this, "Error fetching trips", Toast.LENGTH_SHORT).show()
            }
    }
}