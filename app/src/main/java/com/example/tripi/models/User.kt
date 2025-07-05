package com.example.tripi.models

import com.example.tripi.R

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val profileImage: String = "",
    val imageUrl: String? = null,
    val imageRes: Int = R.drawable.ic_trip_placeholder
)