package com.example.firebaseauthtesting.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ServiceRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val businessId: String = "",
    val businessName: String = "",

    val service: String = "",

    val amount: Double? = null,

    @ServerTimestamp
    val timestamp: Date? = null,
    val scheduledDate: String = "",
    val scheduledTime: String = "",

    val status: String = "Pending"
)