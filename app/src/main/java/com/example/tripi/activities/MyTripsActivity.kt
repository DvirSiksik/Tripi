package com.example.tripi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tripi.R
import com.example.tripi.adapters.TripsAdapter
import com.example.tripi.databinding.ActivityMyTripsBinding
import com.example.tripi.models.Trip
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

        setupBottomNavigation()
        setupRecyclerView()
        setupToolbar()
        setupFAB()
        loadUserTrips()
    }

    private fun setupRecyclerView() {
        tripsAdapter = TripsAdapter(tripsList) { trip ->
            val intent = Intent(this, TripDetailsActivity::class.java)
            intent.putExtra("TRIP", trip)
            startActivity(intent)
        }
        binding.tripsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyTripsActivity)
            adapter = tripsAdapter
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = "My Trips"
    }

    private fun setupFAB() {
        val fabAddTrip: FloatingActionButton = binding.fabAddTrip
        fabAddTrip.setOnClickListener {
            val intent = Intent(this, CreateTripActivity::class.java)
            startActivity(intent)
        }
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
    override fun onResume() {
        super.onResume()
        updateBottomNavSelection()
    }

    private fun updateBottomNavSelection() {
        binding.bottomNavigation.selectedItemId = when (this) {
            is MainActivity -> R.id.navigation_home
            is MyTripsActivity -> R.id.navigation_trips
            is ProfileActivity -> R.id.navigation_profile
            else -> R.id.navigation_home // default
        }
    }

    private fun setupBottomNavigation() {
        updateBottomNavSelection() // עדכן בהתחלה

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val targetActivity = when (item.itemId) {
                R.id.navigation_home -> MainActivity::class.java
                R.id.navigation_trips -> MyTripsActivity::class.java
                R.id.navigation_profile -> ProfileActivity::class.java
                else -> return@setOnItemSelectedListener false
            }

            if (this::class.java == targetActivity) {
                return@setOnItemSelectedListener true // כבר בטאב הזה
            }

            startActivity(Intent(this, targetActivity).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            true
        }
    }
}