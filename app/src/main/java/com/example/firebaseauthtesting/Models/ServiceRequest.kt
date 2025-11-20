package com.example.firebaseauthtesting.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ServiceRequest(
    val userId: String = "",
    val id: String = "",
    val userName: String = "",
    val businessId: String = "",
    val businessName: String = "",
    val status: String = "",
    val scheduledDate: String? = null,
    val scheduledTime: String? = null,

    // --- APPLY THE FIX HERE ---
    @ServerTimestamp
    val timestamp: Timestamp? = null
)