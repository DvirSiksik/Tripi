package com.example.tripi.models

import com.google.firebase.Timestamp

data class Route(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val creatorId: String = "",
    val tripIds: List<String> = emptyList(),
    val participants: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)