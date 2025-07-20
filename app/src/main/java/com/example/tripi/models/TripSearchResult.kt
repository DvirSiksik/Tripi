package com.example.tripi.models

data class TripSearchResult(
    val id: String,
    val name: String,
    val location: String,
    val lat: Double? = 0.0,
    val lon: Double? = 0.0,
    val duration: Int,
    val rating: Float?,
    val description: String?,
    val imageUrl: String?
)