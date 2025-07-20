package com.example.tripi.activities

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.tripi.R
import com.example.tripi.adapters.ImagePagerAdapter
import com.example.tripi.databinding.ActivityTripDetailsBinding
import com.example.tripi.models.Trip
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage

class TripDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTripDetailsBinding
    private lateinit var imageAdapter: ImagePagerAdapter
    private var trip: Trip? = null

    private val pickImages = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val tripId = trip?.id ?: return@registerForActivityResult
        val storageRef = FirebaseStorage.getInstance().reference.child("trip_images/$tripId/")

        uris?.forEachIndexed { index, uri ->
            val fileRef = storageRef.child("new_image_${System.currentTimeMillis()}_$index.jpg")
            fileRef.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                fileRef.downloadUrl
            }.addOnSuccessListener { url ->
                imageAdapter.addImage(Uri.parse(url.toString()))
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
                "&key=AIzaSyBJXMokRfxMWkbiJEOpRM7i6ck_Y7Ji7Uk"

        Glide.with(this)
            .load(staticMapUrl)
            .placeholder(R.drawable.ic_trip_placeholder)
            .into(binding.mapImageView)
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
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "trip_image_${System.currentTimeMillis()}.jpg")

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