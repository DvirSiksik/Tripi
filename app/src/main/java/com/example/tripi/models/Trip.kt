package com.example.tripi.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Trip(
    val id: String = "",
    var name: String = "",
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp = Timestamp.now(),
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val imageUrls: List<String> = emptyList(),
    var description: String = "",
    val durationMinutes: Int = 60,
    val categories: List<String> = emptyList(),
    val points: List<Map<String, Double>> = emptyList(),
    val sharedWith: List<String> = emptyList(),
    val creatorId: String = "",
    val includedTrips: List<String> = emptyList()
) : Parcelable