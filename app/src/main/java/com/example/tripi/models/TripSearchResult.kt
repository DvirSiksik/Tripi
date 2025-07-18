package com.example.tripi.models

data class TripSearchResult(
    val id: String,
    val name: String,
    val location: String,
    val duration: Int, // in minutes
    val rating: Float?,
    val description: String?,
    val imageUrl: String?
)