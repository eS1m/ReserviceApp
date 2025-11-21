package com.example.firebaseauthtesting.Models

data class Review(
    val businessId: String = "",
    val businessName: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val comment: String = "",
    val rating: Int = 0,
)
