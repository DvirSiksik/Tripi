package com.example.tripi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.tripi.R
import com.example.tripi.adapters.TripsAdapter
import com.example.tripi.adapters.TripsPagerAdapter
import com.example.tripi.databinding.ActivityMainBinding
import com.example.tripi.models.Trip
import com.google.firebase.Timestamp
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.*
import kotlin.random.Random
import com.example.tripi.network.RetrofitInstance
import com.example.tripi.network.TripApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var tripsAdapter: TripsAdapter
    private val tripsList = mutableListOf<Trip>()
    private val selectedSuggestions = mutableListOf<Trip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupTripsRecyclerView()
        setupClosestTripsRecyclerView()
        setupBottomNavigation()
        setupListeners()
        fetchUpcomingTripsFromFirestore()
        fetchTripsFromGooglePlaces()
    }

    private fun setupClosestTripsRecyclerView() {
        binding.closestTripsViewPager.adapter = TripsPagerAdapter(tripsList) { trip ->
        }
        binding.closestTripsViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateCurrentTripName(position)
            }
        })
        if (tripsList.isNotEmpty()) {
            updateCurrentTripName(0)
        }
    }

    private fun updateCurrentTripName(position: Int) {
        if (position in 0 until tripsList.size) {
            binding.currentTripNameText.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    binding.currentTripNameText.text = tripsList[position].name
                    binding.currentTripNameText.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        }
    }
    private fun setupTripsRecyclerView() {
        tripsAdapter = TripsAdapter(tripsList) { trip ->
            val intent = Intent(this, TripDetailsActivity::class.java).apply {
                putExtra("TRIP_ID", trip.id)
                putExtra("TRIP_NAME", trip.name)
                putStringArrayListExtra("TRIP_IMAGES", ArrayList(trip.imageUrls))
            }
            startActivity(intent)
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
            else -> R.id.navigation_home
        }
    }

    private fun setupBottomNavigation() {
        updateBottomNavSelection()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val targetActivity = when (item.itemId) {
                R.id.navigation_home -> MainActivity::class.java
                R.id.navigation_trips -> MyTripsActivity::class.java
                R.id.navigation_profile -> ProfileActivity::class.java
                else -> return@setOnItemSelectedListener false
            }

            if (this::class.java == targetActivity) {
                return@setOnItemSelectedListener true
            }

            startActivity(Intent(this, targetActivity).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            true
        }
    }
    private fun setupListeners() {
        binding.addTripButton.setOnClickListener {
            if (selectedSuggestions.isNotEmpty()) {
                val intent = Intent(this, CreateTripActivity::class.java)
                intent.putParcelableArrayListExtra("selectedRoutes", ArrayList(selectedSuggestions))
                startActivity(intent)
            } else {
                startActivity(Intent(this, CreateTripActivity::class.java))
            }
        }
    }

    private fun fetchTripsFromGooglePlaces() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = RetrofitInstance.googlePlacesRetrofit.create(TripApiService::class.java)
                val appContext = applicationContext
                val apiKey = appContext.packageManager
                    .getApplicationInfo(appContext.packageName, android.content.pm.PackageManager.GET_META_DATA)
                    .metaData.getString("com.google.android.geo.API_KEY")

                val response = api.getRecommendedTrips(apiKey = apiKey ?: "")
                if (response.isSuccessful) {
                    val body = response.body()
                    val googleTrips = body?.results?.take(20)?.map { result ->
                        Trip(
                            id = result.name ?: "Unknown",
                            name = result.name ?: "Unnamed",
                            startDate = Timestamp.now(),
                            endDate = Timestamp.now(),
                            lat = result.geometry?.location?.lat ?: 0.0,
                            lon = result.geometry?.location?.lng ?: 0.0,
                            imageUrls = result.photos?.map {
                                "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=${it.photo_reference}&key=$apiKey"
                            } ?: emptyList(),
                            description = result.formatted_address ?: "",
                            durationMinutes = 120
                        )
                    } ?: emptyList()

                    withContext(Dispatchers.Main) {
                        selectedSuggestions.clear()
                        selectedSuggestions.addAll(googleTrips)

                        tripsAdapter = TripsAdapter(selectedSuggestions) { trip ->
                            val intent = Intent(this@MainActivity, TripDetailsActivity::class.java).apply {
                                putExtra("TRIP_ID", trip.id)
                                putExtra("TRIP_NAME", trip.name)
                                putStringArrayListExtra("TRIP_IMAGES", ArrayList(trip.imageUrls))
                            }
                            startActivity(intent)
                        }

                        binding.suggestionsRecyclerView.apply {
                            adapter = tripsAdapter
                            layoutManager = LinearLayoutManager(
                                this@MainActivity,
                                LinearLayoutManager.HORIZONTAL,
                                false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Google Places API failed: ${e.message}")
            }
        }
    }
    private fun fetchUpcomingTripsFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("trips")
            .whereArrayContains("sharedWith", userId)
            .get()
            .addOnSuccessListener { result ->
                val upcomingTrips = result.documents.mapNotNull { doc ->
                    doc.toObject(Trip::class.java)?.copy(id = doc.id)
                }.sortedBy { it.startDate }
                tripsList.clear()
                tripsList.addAll(upcomingTrips)
                (binding.closestTripsViewPager.adapter as? TripsPagerAdapter)?.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to fetch trips from Firestore: ${e.message}")
            }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { firstChar ->
                if (firstChar.isLowerCase()) firstChar.titlecase() else firstChar.toString()
            }
        }
    }
}