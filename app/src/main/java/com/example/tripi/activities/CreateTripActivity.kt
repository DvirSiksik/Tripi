package com.example.tripi.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tripi.R
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar


class CreateTripActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityCreateTripBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore
    private var selectedLocation: LatLng? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap()
        setupDatePickers()
        setupListeners()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng))
        }

        getLastKnownLocation()
    }

    private fun getLastKnownLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
                        selectedLocation = latLng
                        googleMap.addMarker(MarkerOptions().position(latLng))
                    }
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun setupDatePickers() {
        binding.startDateButton.setOnClickListener {
            showDatePicker(true)
        }

        binding.endDateButton.setOnClickListener {
            showDatePicker(false)
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val dateStr = "${day}/${month + 1}/${year}"

                if (isStartDate) {
                    binding.startDateButton.text = dateStr
                } else {
                    binding.endDateButton.text = dateStr
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            saveTrip()
        }
    }

    private fun saveTrip() {
        val tripName = binding.tripNameEditText.text.toString().trim()
        val startDate = Timestamp(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endDate = Timestamp(calendar.time)

        if (tripName.isEmpty()) {
            Toast.makeText(this, "Please enter trip name", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val trip = Trip(
            name = tripName,
            startDate = startDate,
            endDate = endDate,
            lat = selectedLocation!!.latitude,
            lon = selectedLocation!!.longitude,
            userId = userId,
            imageUrl = ""
        )

        db.collection("trips")
            .add(trip)
            .addOnSuccessListener {
                Toast.makeText(this, "Trip created successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error creating trip: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}