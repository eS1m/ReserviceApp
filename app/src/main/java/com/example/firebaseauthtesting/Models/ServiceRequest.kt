package com.example.firebaseauthtesting.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ServiceRequest(
    val requestId: String = "",
    val userId: String = "",
    val userName: String = "",
    val businessId: String = "",
    val serviceCategory: String = "",
    val status: String = "Pending",
    @ServerTimestamp
    val timestamp: Timestamp = Timestamp.now(),
    val scheduledDateTime: Timestamp? = null
)