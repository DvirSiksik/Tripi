package com.example.tripi.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class ItineraryItem(
    var id: String = "",
    val tripId: String = "",
    val title: String = "",
    val description: String = "",
    val time: Timestamp = Timestamp.now(),
    val location: GeoPoint? = null,
    val cost: Double = 0.0,
    val category: String = "", // Flight, Hotel, Activity, etc.
    val address: String = ""
)