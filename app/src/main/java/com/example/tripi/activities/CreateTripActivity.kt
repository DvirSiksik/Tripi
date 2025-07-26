package com.example.tripi.activities

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.tripi.adapters.ImagePagerAdapter
import com.example.tripi.adapters.SelectedTripsAdapter
import com.example.tripi.adapters.TripSearchAdapter
import com.example.tripi.databinding.ActivityCreateTripBinding
import com.example.tripi.models.Trip
import com.example.tripi.models.TripSearchResult
import com.example.tripi.network.GooglePhoto
import com.example.tripi.network.GooglePlaceResult
import com.example.tripi.network.PlacesClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.example.tripi.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CircleOptions
import android.os.Looper
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException


class CreateTripActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityCreateTripBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore
    private lateinit var imageAdapter: ImagePagerAdapter
    private lateinit var searchAdapter: TripSearchAdapter
    private lateinit var selectedTripsAdapter: SelectedTripsAdapter

    private val calendar = Calendar.getInstance()
    private val pointsOfInterest = mutableListOf<LatLng>()
    private val searchResults = mutableListOf<TripSearchResult>()
    private val selectedTrips = mutableListOf<TripSearchResult>()

    private val pickImages = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris?.forEach { uri -> imageAdapter.addImage(uri) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ), 1)

        setupMap()
        setupDatePickers()
        setupImageGallery()
        setupSearchFunctionality()
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
        getLastKnownLocationAndZoom()
    }

    private fun redrawPolyline() {
        googleMap.clear()
        pointsOfInterest.forEach { point ->
            googleMap.addMarker(MarkerOptions().position(point))
        }
        if (pointsOfInterest.size >= 2) {
            googleMap.addPolyline(
                PolylineOptions()
                    .addAll(pointsOfInterest)
                    .width(8f)
                    .color(resources.getColor(R.color.purple_500, null))
            )
        }
    }

    private fun getLastKnownLocationAndZoom() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not received", Toast.LENGTH_SHORT).show()
            return
        }

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            numUpdates = 1
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11f))

                    googleMap.addCircle(
                        CircleOptions()
                            .center(latLng)
                            .radius(80000.0)
                            .strokeWidth(4f)
                    )
                } else {
                    Toast.makeText(this@CreateTripActivity, "Location is null", Toast.LENGTH_SHORT).show()
                }
            }
        }, Looper.getMainLooper())
    }

    private fun setupDatePickers() {
        binding.startDateButton.setOnClickListener { showDatePicker(true) }
        binding.endDateButton.setOnClickListener { showDatePicker(false) }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            val dateStr = "$day/${month + 1}/$year"
            if (isStartDate) binding.startDateButton.text = dateStr
            else binding.endDateButton.text = dateStr
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setupImageGallery() {
        imageAdapter = ImagePagerAdapter { position -> imageAdapter.removeImage(position) }
        binding.imageViewPager.adapter = imageAdapter
        binding.dotsIndicator.attachTo(binding.imageViewPager)
        binding.imageViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
    }

    private fun setupSearchFunctionality() {
        searchAdapter = TripSearchAdapter(mutableListOf()) { trip -> addSelectedTrip(trip) }
        binding.searchResultsRecyclerView.adapter = searchAdapter
        binding.searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)

        selectedTripsAdapter = SelectedTripsAdapter(selectedTrips) { trip -> removeSelectedTrip(trip) }
        binding.selectedTripsRecyclerView.adapter = selectedTripsAdapter
        binding.selectedTripsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        binding.addPointButton.setOnClickListener {
            Toast.makeText(this, "Tap the map to add points", Toast.LENGTH_SHORT).show()
        }

        binding.saveButton.setOnClickListener { saveTrip() }
        binding.selectImageButton.setOnClickListener { pickImages.launch(arrayOf("image/*")) }
        binding.searchButton.setOnClickListener { performSearch() }

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
    }

    private fun performSearch() {
        val query = binding.searchEditText.text.toString()
        if (query.isBlank()) {
            Toast.makeText(this, "Please enter search query", Toast.LENGTH_SHORT).show()
            return
        }

        binding.searchInputLayout.isEnabled = false
        binding.searchButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = PlacesClient.apiService.searchPlaces(query = query)
                Log.d("PlacesAPI", "URL: ${response.raw().request().url()}")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("PlacesAPI", "Status: ${body?.status}")
                        Log.d("PlacesAPI", "Results: ${body?.results?.size}")
                        if (body?.status != "OK") {
                            Toast.makeText(this@CreateTripActivity, "Places API error: ${body?.status}", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }

                        val tripResults = body.results.map { place ->
                            TripSearchResult(
                                id = place.place_id ?: UUID.randomUUID().toString(),
                                name = place.name ?: "Unknown Place",
                                location = place.formatted_address ?: "",
                                duration = calculateDuration(place),
                                rating = place.rating,
                                description = "Tourist attraction in Israel",
                                imageUrl = getPhotoUrl(place.photos?.firstOrNull()),
                                lat = place.geometry?.location?.lat,
                                lon = place.geometry?.location?.lng
                            )
                        }
                        searchAdapter.updateData(tripResults)
                        searchResults.clear()
                        searchResults.addAll(tripResults)
                    } else {
                        val error = response.errorBody()?.string()
                        Log.e("PlacesAPI", "HTTP error ${response.code()}: $error")
                        Toast.makeText(this@CreateTripActivity, "error: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PlacesAPI", "Exception: ${e.message}", e)
                    Toast.makeText(this@CreateTripActivity, " Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.searchInputLayout.isEnabled = true
                    binding.searchButton.isEnabled = true
                }
            }
        }
    }

    private fun calculateDuration(place: GooglePlaceResult): Int {
        return when {
            place.name?.contains("museum", ignoreCase = true) == true -> 120
            place.name?.contains("park", ignoreCase = true) == true -> 90
            else -> 60
        }
    }

    private fun getPhotoUrl(photo: GooglePhoto?): String? {
        return photo?.let {
            "https://maps.googleapis.com/maps/api/place/photo" +
                    "?maxwidth=400" +
                    "&photoreference=${it.photo_reference}" +
                    "&key=AIzaSyBJXMokRfxMWkbiJEOpRM7i6ck_Y7Ji7Uk"
        }
    }

    private fun addSelectedTrip(trip: TripSearchResult) {
        if (selectedTrips.none { it.id == trip.id }) {
            selectedTrips.add(trip)
            selectedTripsAdapter.notifyDataSetChanged()
            Toast.makeText(this, "${trip.name} added to your trip", Toast.LENGTH_SHORT).show()

            val lat = trip.lat
            val lon = trip.lon
            if (lat != null && lon != null) {
                val latLng = LatLng(lat, lon)
                pointsOfInterest.add(latLng)
                googleMap.addMarker(MarkerOptions().position(latLng).title(trip.name))
                redrawPolyline()

            }
            trip.imageUrl?.let { imageUrl ->
                imageAdapter.addImageFromUrl(imageUrl)
            }
        }
    }

    private fun removeSelectedTrip(trip: TripSearchResult) {
        selectedTrips.removeAll { it.id == trip.id }
        selectedTripsAdapter.notifyDataSetChanged()

        trip.lat?.let { lat ->
            trip.lon?.let { lon ->
                val point = LatLng(lat, lon)
                pointsOfInterest.removeAll { it.latitude == point.latitude && it.longitude == point.longitude }
                redrawPolyline()
            }
        }
        trip.imageUrl?.let { imageUrl ->
            val imageUri = Uri.parse(imageUrl)
            imageAdapter.removeImageUri(imageUri)
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

        if (imageAdapter.itemCount == 0) {
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

        val includedTrips = selectedTrips.map { it.id }

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
            creatorId = userId,
            includedTrips = includedTrips
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
        val images = imageAdapter.getImages()

        if (images.isEmpty()) {
            onComplete(emptyList())
            return
        }

        val client = OkHttpClient()

        CoroutineScope(Dispatchers.IO).launch {
            for ((index, uri) in images.withIndex()) {
                try {
                    val fileUri = if (uri.scheme == "http" || uri.scheme == "https") {
                        val request = Request.Builder().url(uri.toString()).build()
                        val response = client.newCall(request).execute()
                        val responseBody = response.body() ?: throw IOException("Response body is null for $uri")
                        val inputStream = responseBody.byteStream()
                        val file = File.createTempFile("image_${index}_", ".jpg", cacheDir)
                        file.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }
                        Uri.fromFile(file)
                    } else {
                        uri
                    }

                    val fileRef = storageRef.child("image_${index}.jpg")
                    val uploadTask = fileRef.putFile(fileUri).continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                        fileRef.downloadUrl
                    }

                    val downloadUrl = uploadTask.await()
                    uploadedUrls.add(downloadUrl.toString())

                    if (uploadedUrls.size == images.size) {
                        withContext(Dispatchers.Main) { onComplete(uploadedUrls) }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CreateTripActivity,
                            "Failed to upload image: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}