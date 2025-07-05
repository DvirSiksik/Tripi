package com.example.tripi.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.tripi.adapters.TripsAdapter
import com.example.tripi.databinding.ActivityMainBinding
import com.example.tripi.models.Trip
import com.example.tripi.network.RetrofitInstance
import com.google.firebase.Timestamp
import kotlinx.coroutines.*
import kotlin.random.Random
import com.example.tripi.R
import java.util.UUID
import com.google.gson.JsonParser

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
        fetchTripsFromGeoapify()
    }

    private fun setupTripsRecyclerView() {
        tripsAdapter = TripsAdapter(tripsList) { trip ->
            val intent = Intent(this, TripDetailsActivity::class.java)
            intent.putExtra("TRIP", trip)
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
                R.id.navigation_friends -> {
                    startActivity(Intent(this, FriendsActivity::class.java))
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

    private fun fetchTripsFromGeoapify() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val lat = 31.7683
                val lon = 35.2137
                val response = RetrofitInstance.api.getPlacesNear(
                    filter = "circle:$lon,$lat,10000",
                    bias = "proximity:$lon,$lat"
                )

                tripsList.clear()

                val convertedTrips = response.features.mapNotNull { feature ->
                    val name = feature.properties.name ?: return@mapNotNull null
                    val coords = feature.geometry.coordinates


                    val imageUrl = try {
                        val json = JsonParser.parseString(feature.properties.details.toString()).asJsonObject
                        json.get("image")?.asString
                    } catch (e: Exception) {
                        null
                    } ?: feature.properties.datasource?.raw?.image ?: ""

                    Log.d("TripImage", "Loaded image: $imageUrl")
                    Log.d("GeoapifyFull", response.toString())
                    Trip(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        startDate = Timestamp.now(),
                        endDate = Timestamp.now(),
                        lat = coords[1],
                        lon = coords[0],
                        imageUrl = imageUrl,
                        description = feature.properties.formatted ?: "No description available.",
                        durationMinutes = Random.nextInt(30, 180)
                    )
                }

                tripsList.addAll(convertedTrips)
                tripsAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e("MainActivity", "Geoapify API Error: ${e.message}")
            }
        }
    }
}