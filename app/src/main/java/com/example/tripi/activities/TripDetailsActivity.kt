package com.example.tripi.activities

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
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
import java.text.SimpleDateFormat
import java.util.*

class TripDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityTripDetailsBinding
    private lateinit var imageAdapter: ImagePagerAdapter
    private var trip: Trip? = null
    private lateinit var googleMap: GoogleMap

    private lateinit var headerImageHandler: Handler
    private lateinit var headerImageRunnable: Runnable
    private val SLIDER_INTERVAL = 60000L
    private var currentHeaderImageIndex = 0
    private lateinit var imageUris: MutableList<Uri>
    private var isSliderRunning = false

    private var isEditMode = false

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
                    imageUris.add(Uri.parse(urlStr))
                    imageAdapter.notifyDataSetChanged()
                    tripDocRef.update("imageUrls", FieldValue.arrayUnion(urlStr))
                    Toast.makeText(this, "Uploaded image", Toast.LENGTH_SHORT).show()

                    if (imageUris.size == 1) {
                        updateHeaderImage()
                    } else if (imageUris.size == 2 && !isSliderRunning) {
                        startImageSlider()
                    }
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
        setupListeners()
        setupEditButton()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    private fun displayTripDetails(trip: Trip) {
        binding.collapsingToolbar.title = trip.name
        binding.tripNameTextView.text = trip.name
        binding.tripNameEditText.setText(trip.name)

        try {
            val startDate = trip.startDate?.toDate() ?: return
            val endDate = trip.endDate?.toDate() ?: return
            val isSingleDay = startDate.time == endDate.time
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
            binding.dateRangeTextView.text = if (isSingleDay) {
                dateFormat.format(startDate)
            } else {
                "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
            }
        } catch (e: Exception) {
            Log.e("DateError", "Date formatting failed: ${e.message}")
            binding.dateRangeTextView.text = "Date not available"
        }

        binding.descriptionTextView.text = trip.description.ifBlank { "No description available." }
        binding.tripDescriptionEditText.setText(trip.description)

        imageUris = trip.imageUrls.map { Uri.parse(it) }.toMutableList()
        imageAdapter = ImagePagerAdapter(imageUris)

        binding.imageViewPager.adapter = imageAdapter
        binding.imageViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        TabLayoutMediator(binding.tabLayout, binding.imageViewPager) { tab, _ ->
            tab.text = ""
        }.attach()

        if (imageUris.isNotEmpty()) {
            updateHeaderImage()
        }

        if (imageUris.size > 1) {
            startImageSlider()
        }
    }
    private fun setupEditButton() {
        binding.fabEditTrip.setOnClickListener {
            if (isEditMode) {
                saveChanges()
            } else {
                enableEditMode()
            }
        }
    }

    private fun enableEditMode() {
        isEditMode = true
        binding.tripNameTextView.visibility = View.GONE
        binding.tripNameEditText.visibility = View.VISIBLE
        binding.descriptionTextView.visibility = View.GONE
        binding.tripDescriptionEditText.visibility = View.VISIBLE
        binding.editControlsLayout.visibility = View.VISIBLE
        binding.addImageButton.visibility = View.VISIBLE
        binding.fabEditTrip.setImageResource(R.drawable.ic_check)
    }

    private fun disableEditMode() {
        isEditMode = false
        binding.tripNameEditText.visibility = View.GONE
        binding.tripNameTextView.visibility = View.VISIBLE

        binding.tripDescriptionEditText.visibility = View.GONE
        binding.descriptionTextView.visibility = View.VISIBLE
        binding.editControlsLayout.visibility = View.GONE
        binding.addImageButton.visibility = View.GONE
        binding.fabEditTrip.setImageResource(R.drawable.ic_edit)
    }

    private fun saveChanges() {
        val newName = binding.tripNameEditText.text.toString()
        val newDescription = binding.tripDescriptionEditText.text.toString()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Trip name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        trip?.name = newName
        trip?.description = newDescription

        val tripId = trip?.id ?: return
        FirebaseFirestore.getInstance().collection("trips").document(tripId)
            .update(
                "name", newName,
                "description", newDescription
            )
            .addOnSuccessListener {
                binding.tripNameTextView.text = newName
                binding.descriptionTextView.text = newDescription
                disableEditMode()
                Toast.makeText(this, "Trip updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating trip: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startImageSlider() {
        if (isSliderRunning) return

        headerImageHandler = Handler(Looper.getMainLooper())
        headerImageRunnable = object : Runnable {
            override fun run() {
                if (imageUris.size > 1) {
                    currentHeaderImageIndex = (currentHeaderImageIndex + 1) % imageUris.size
                    updateHeaderImage()
                }
                headerImageHandler.postDelayed(this, SLIDER_INTERVAL)
            }
        }

        isSliderRunning = true
        headerImageHandler.postDelayed(headerImageRunnable, SLIDER_INTERVAL)
    }

    private fun stopImageSlider() {
        if (::headerImageHandler.isInitialized) {
            headerImageHandler.removeCallbacks(headerImageRunnable)
        }
        isSliderRunning = false
    }

    private fun updateHeaderImage() {
        if (imageUris.isEmpty()) return
        if (currentHeaderImageIndex >= imageUris.size) {
            currentHeaderImageIndex = 0
        }

        Glide.with(this)
            .load(imageUris[currentHeaderImageIndex])
            .transition(DrawableTransitionOptions.withCrossFade(800))
            .centerCrop()
            .placeholder(R.drawable.ic_trip_placeholder)
            .error(R.drawable.ic_trip_placeholder)
            .into(binding.headerImageView)
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
            val lng = point["lng"] ?: point["lon"]

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

        binding.saveButton.setOnClickListener {
            saveChanges()
        }

        binding.cancelButton.setOnClickListener {
            disableEditMode()
        }
    }

    override fun onBackPressed() {
        if (isEditMode) {
            AlertDialog.Builder(this)
                .setTitle("Discard changes?")
                .setMessage("Are you sure you want to discard your changes?")
                .setPositiveButton("Discard") { _, _ -> disableEditMode() }
                .setNegativeButton("Keep Editing", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onPause() {
        super.onPause()
        stopImageSlider()
    }

    override fun onResume() {
        super.onResume()
        if (imageUris.size > 1 && !isSliderRunning) {
            startImageSlider()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopImageSlider()
    }
}