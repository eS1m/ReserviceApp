package com.example.firebaseauthtesting.Models

import com.google.firebase.firestore.GeoPoint

data class Business(
    val uid: String = "",
    val businessName: String = "",
    val location: GeoPoint? = null,
    val services: List<String> = emptyList(),
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0,

    val managerName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,

    @field:Transient val recentAverageRating: Double = 0.0
)
