package com.example.firebaseauthtesting.Models

import com.google.firebase.Timestamp

data class ServiceRequest(
    val requestId: String = "",
    val userId: String = "",
    val userName: String = "",
    val businessId: String = "",
    val serviceCategory: String = "",
    val status: String = "Pending",
    val timestamp: Timestamp = Timestamp.now()
)