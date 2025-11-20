package com.example.firebaseauthtesting.Models

import com.google.firebase.firestore.GeoPoint

data class Business(
    val uid: String = "",
    val businessName: String = "",
    val managerName: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val location: GeoPoint? = null,
    val services: List<String> = emptyList(),
    val rating: Double = 0.0,
    val ratingCount: Int = 0
)