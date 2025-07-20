package com.example.tripi.activities

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.tripi.R
import com.example.tripi.adapters.ImagePagerAdapter
import com.example.tripi.databinding.ActivityTripDetailsBinding
import com.example.tripi.models.Trip
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class TripDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityTripDetailsBinding
    private lateinit var imageAdapter: ImagePagerAdapter
    private var trip: Trip? = null
    private lateinit var googleMap: GoogleMap

    private val pickImages = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val tripId = trip?.id ?: return@registerForActivityResult
        val storageRef = FirebaseStorage.getInstance().reference.child("trip_images/$tripId/")
        val tripDocRef = FirebaseFirestore.getInstance().collection("trips").document(tripId)

        uris?.forEachIndexed { index, uri ->
            val fileRef = storageRef.child("new_image_${System.currentTimeMillis()}_$index.jpg")
            fileRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    fileRef.downloadUrl
                }.addOnSuccessListener { url ->
                    val urlStr = url.toString()
                    imageAdapter.addImage(Uri.parse(urlStr))
                    tripDocRef.update("imageUrls", FieldValue.arrayUnion(urlStr))
                    Toast.makeText(this, "Uploaded image", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        trip = intent.getParcelableExtra("TRIP")
        if (trip != null) {
            displayTripDetails(trip!!)
        } else {
            finish()
        }

        setupBottomNavigation()
        setupListeners()

        // Load the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun displayTripDetails(trip: Trip) {
        binding.collapsingToolbar.title = trip.name
        binding.tripNameTextView.text = trip.name
        binding.descriptionTextView.text = trip.description.ifBlank { "No description available." }

        val imageUris = trip.imageUrls.map { Uri.parse(it) }.toMutableList()
        imageAdapter = ImagePagerAdapter(imageUris)
        binding.imageViewPager.adapter = imageAdapter
        binding.imageViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        TabLayoutMediator(binding.tabLayout, binding.imageViewPager) { tab, _ ->
            tab.text = ""
        }.attach()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        trip?.let { drawTripRoute(it.points) }
    }

    private fun drawTripRoute(points: List<Map<String, Double>>) {
        if (points.isEmpty()) return

        val polylineOptions = PolylineOptions()
            .color(Color.BLUE)
            .width(6f)

        val boundsBuilder = LatLngBounds.Builder()
        var addedAtLeastOne = false

        points.forEachIndexed { index, point ->
            val lat = point["lat"]
            val lng = point["lng"]?: point["lon"]

            if (lat != null && lng != null) {
                val latLng = LatLng(lat, lng)
                polylineOptions.add(latLng)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Point ${index + 1}")
                )
                boundsBuilder.include(latLng)
                addedAtLeastOne = true
            }
        }

        if (addedAtLeastOne) {
            googleMap.addPolyline(polylineOptions)
            val bounds = boundsBuilder.build()
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } else {
            Toast.makeText(this, "No valid points to display on map", Toast.LENGTH_SHORT).show()
        }
        Log.d("TripPointsDebug", "Points: $points")
    }

    private fun setupListeners() {
        binding.addImageButton.setOnClickListener {
            pickImages.launch(arrayOf("image/*"))
        }

        binding.downloadImageButton.setOnClickListener {
            val currentPosition = binding.imageViewPager.currentItem
            val imageUri = imageAdapter.getImages().getOrNull(currentPosition) ?: return@setOnClickListener

            val request = DownloadManager.Request(imageUri)
                .setTitle("trip_image.jpg")
                .setDescription("Downloading trip image")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_PICTURES,
                    "trip_image_${System.currentTimeMillis()}.jpg"
                )

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(this, "Downloading image...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MyTripsActivity::class.java))
                    true
                }
                R.id.navigation_trips -> true
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}