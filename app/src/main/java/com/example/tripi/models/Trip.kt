package com.example.tripi.models

import android.os.Parcelable
import com.example.tripi.R
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Trip(
    val id: String = "",
    val name: String = "",
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp = Timestamp.now(),
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val userId: String = "",
    val description: String = "",
    val durationMinutes: Int = 90,
    val city: String = "",
    val sharedWith: List<String> = emptyList(),
    val imageUrl: String = "",
    val imageRes: Int = R.drawable.ic_trip_placeholder
) : Parcelable