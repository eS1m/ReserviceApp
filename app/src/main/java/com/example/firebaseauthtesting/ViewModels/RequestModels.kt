package com.example.firebaseauthtesting.ViewModels

import com.google.firebase.Timestamp

// Data model for a service request, now in its own file.
data class ServiceRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "", // Denormalized for easy display
    val businessId: String = "",
    val businessName: String = "", // Denormalized for easy display
    val status: String = "Pending", // e.g., "Pending", "Accepted", "Declined"
    val timestamp: Timestamp = Timestamp.now()
)

// UI state for the request creation process, now in its own file.
sealed interface RequestState {
    object Idle : RequestState
    object Loading : RequestState
    object Success : RequestState
    data class Error(val message: String) : RequestState
}
