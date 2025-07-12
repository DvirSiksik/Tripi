package com.example.tripi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.tripi.R
import com.example.tripi.adapters.TripsAdapter
import com.example.tripi.databinding.ActivityMainBinding
import com.example.tripi.models.Trip
import com.example.tripi.network.RetrofitInstance
import com.google.firebase.Timestamp
import kotlinx.coroutines.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var tripsAdapter: TripsAdapter
    private val tripsList = mutableListOf<Trip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupTripsRecyclerView()
        setupBottomNavigation()
        setupListeners()
        fetchTripsFromOpenTripMap()
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

        binding.tripsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = tripsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_trips -> {
                    startActivity(Intent(this, MyTripsActivity::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupListeners() {
        binding.addTripButton.setOnClickListener {
            startActivity(Intent(this, CreateTripActivity::class.java))
        }
    }

    private fun fetchTripsFromOpenTripMap() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val lat = 31.7683
                val lon = 35.2137

                val response = RetrofitInstance.openTripApi.getPlacesNearby(
                    longitude = lon,
                    latitude = lat,
                    categories = "natural,interesting_places,historic,architecture"
                )

                if (response.isSuccessful) {
                    tripsList.clear()

                    val features = response.body()?.features ?: emptyList()

                    val deferredTrips = features.map { feature ->
                        async(Dispatchers.IO) {
                            try {
                                val detailsResp = RetrofitInstance.openTripApi.getPlaceDetails(feature.xid)
                                val details = detailsResp.body()

                                val imageUrl = details?.preview?.source
                                    ?: getWikimediaImageUrl(details?.wikidata)

                                Trip(
                                    id = feature.xid,
                                    name = feature.properties.name.ifBlank { "Unnamed Place" },
                                    startDate = Timestamp.now(),
                                    endDate = Timestamp.now(),
                                    lat = feature.geometry.coordinates[1],
                                    lon = feature.geometry.coordinates[0],
                                    imageUrls = listOfNotNull(imageUrl),
                                    description = details?.wikipedia_extracts?.text ?: "No description available.",
                                    durationMinutes = Random.nextInt(45, 180),
                                    categories = feature.properties.kinds?.split(",") ?: emptyList()
                                )
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error fetching place details: ${e.message}")
                                null
                            }
                        }
                    }

                    val trips = deferredTrips.awaitAll().filterNotNull()
                    tripsList.addAll(trips)
                    tripsAdapter.notifyDataSetChanged()

                } else {
                    Log.e("MainActivity", "Failed to load places: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "API Error: ${e.message}")
            }
        }
    }

    private suspend fun getWikimediaImageUrl(wikidataId: String?): String? {
        if (wikidataId.isNullOrBlank()) return null

        return try {
            val response = RetrofitInstance.wikimediaApi.getWikimediaImage(
                titles = "File:$wikidataId.jpg"
            )
            val pages = response.body()?.query?.pages
            pages?.values?.firstOrNull()
                ?.imageinfo?.firstOrNull()
                ?.url
        } catch (e: Exception) {
            Log.e("MainActivity", "Wikimedia API error: ${e.message}")
            null
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