package com.example.tripi.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.tripi.R
import com.example.tripi.adapters.ImagePagerAdapter
import com.example.tripi.databinding.ActivityCreateTripBinding
import com.example.tripi.models.Trip
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import java.util.Calendar
import java.util.UUID

class CreateTripActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityCreateTripBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ImagePagerAdapter
    private val calendar = Calendar.getInstance()
    private val pointsOfInterest = mutableListOf<LatLng>()

    // ActivityResultLauncher for picking multiple images
    private val pickImages = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris?.forEach { uri ->
            adapter.addImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap()
        setupDatePickers()
        setupImageGallery()
        setupListeners()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.setOnMapClickListener { latLng ->
            pointsOfInterest.add(latLng)
            googleMap.addMarker(MarkerOptions().position(latLng))
            redrawPolyline()
        }

        getLastKnownLocation()
    }

    private fun redrawPolyline() {
        googleMap.clear()
        pointsOfInterest.forEach { point ->
            googleMap.addMarker(MarkerOptions().position(point))
        }
        if (pointsOfInterest.size >= 2) {
            val addPolyline = googleMap.addPolyline(
                PolylineOptions()
                    .addAll(pointsOfInterest)
                    .width(8f)
                    .color(resources.getColor(R.color.purple_500, null)))
        }
    }

    private fun getLastKnownLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun setupDatePickers() {
        binding.startDateButton.setOnClickListener { showDatePicker(true) }
        binding.endDateButton.setOnClickListener { showDatePicker(false) }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val dateStr = "$day/${month + 1}/$year"
                if (isStartDate) binding.startDateButton.text = dateStr
                else binding.endDateButton.text = dateStr
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupImageGallery() {
        adapter = ImagePagerAdapter { position ->
            adapter.removeImage(position)
        }

        binding.imageViewPager.adapter = adapter
        binding.dotsIndicator.attachTo(binding.imageViewPager)

        // Set orientation for horizontal scrolling
        binding.imageViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
    }

    private fun setupListeners() {
        binding.addPointButton.setOnClickListener {
            Toast.makeText(this, "Tap the map to add points", Toast.LENGTH_SHORT).show()
        }

        binding.saveButton.setOnClickListener {
            saveTrip()
        }

        binding.selectImageButton.setOnClickListener {
            pickImages.launch(arrayOf("image/*"))
        }
    }

    private fun saveTrip() {
        val tripName = binding.tripNameEditText.text.toString().trim()
        val description = binding.tripDescriptionEditText.text.toString()
        val duration = binding.tripDurationEditText.text.toString().toIntOrNull() ?: 60

        if (tripName.isEmpty()) {
            Toast.makeText(this, "Please enter trip name", Toast.LENGTH_SHORT).show()
            return
        }

        if (pointsOfInterest.isEmpty()) {
            Toast.makeText(this, "Please add at least one point", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val tripId = UUID.randomUUID().toString()

        if (adapter.itemCount == 0) {
            createTrip(tripId, userId, tripName, description, duration, emptyList())
            return
        }

        uploadImagesToFirebaseStorage(tripId) { imageUrls ->
            createTrip(tripId, userId, tripName, description, duration, imageUrls)
        }
    }

    private fun createTrip(
        tripId: String,
        userId: String,
        tripName: String,
        description: String,
        duration: Int,
        imageUrls: List<String>
    ) {
        val sharedEmails = binding.sharedWithEditText.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val trip = Trip(
            id = tripId,
            name = tripName,
            startDate = Timestamp.now(),
            endDate = Timestamp.now(),
            lat = pointsOfInterest.first().latitude,
            lon = pointsOfInterest.first().longitude,
            imageUrls = imageUrls,
            description = description,
            durationMinutes = duration,
            points = pointsOfInterest.map { mapOf("lat" to it.latitude, "lon" to it.longitude) },
            sharedWith = (sharedEmails + userId).distinct(),
            creatorId = userId
        )

        db.collection("trips").document(tripId).set(trip)
            .addOnSuccessListener {
                Toast.makeText(this, "Trip created successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error creating trip: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImagesToFirebaseStorage(tripId: String, onComplete: (List<String>) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("trip_images/$tripId/")
        val uploadedUrls = mutableListOf<String>()
        val images = adapter.getImages()

        for ((index, uri) in images.withIndex()) {
            val fileRef = storageRef.child("image_$index.jpg")
            fileRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    fileRef.downloadUrl
                }
                .addOnSuccessListener { url ->
                    uploadedUrls.add(url.toString())
                    if (uploadedUrls.size == images.size) {
                        onComplete(uploadedUrls)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload image: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}