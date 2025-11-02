package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath


data class BusinessMarker(
    val uid: String,
    val fullName: String,
    val location: GeoPoint,
    val services: List<String> = emptyList()
)

// UI State for the map screen
sealed class MapUiState {
    object Loading : MapUiState()
    data class Success(val businesses: List<BusinessMarker>) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

class BusinessMapViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState = _uiState.asStateFlow()


    fun fetchBusinessesByService(serviceCategory: String) {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading

            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _uiState.value = MapUiState.Error("User not logged in.")
                return@launch
            }
            try {
                val querySnapshot = db.collection("users")
                    .whereEqualTo("isBusiness", true)
                    .whereArrayContains("services", serviceCategory)
                    .whereNotEqualTo(FieldPath.documentId(), currentUserId)
                    .get()
                    .await()

                val businessList = querySnapshot.documents.mapNotNull { document ->
                    val fullName = document.getString("fullName")
                    val location = document.getGeoPoint("location")
                    @Suppress("UNCHECKED_CAST")
                    val services = document.get("services") as? List<String> ?: emptyList()
                    if (fullName != null && location != null) {
                        BusinessMarker(
                            uid = document.id,
                            fullName = fullName,
                            location = location,
                            services = services
                        )
                    } else {
                        null
                    }
                }
                _uiState.value = MapUiState.Success(businessList)
            } catch (e: Exception) {
                _uiState.value = MapUiState.Error("Failed to fetch businesses: ${e.message}")
            }
        }
    }
    fun createServiceRequest(
        businessId: String,
        serviceCategory: String,
        scheduledDateTime: Timestamp,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            val userProfile = db.collection("users").document(currentUser!!.uid).get().await()
            val userName = userProfile.getString("fullName") ?: "Unknown User"

            if (currentUser == null) {
                onResult(false, "You must be logged in to make a request.")
                return@launch
            }

            try {
                val newRequestRef = db.collection("requests").document()
                val request = ServiceRequest(
                    requestId = newRequestRef.id,
                    userId = currentUser.uid,
                    userName = userName,
                    businessId = businessId,
                    serviceCategory = serviceCategory,
                    // Pass the new data to the model
                    scheduledDateTime = scheduledDateTime
                )

                newRequestRef.set(request).await()
                onResult(true, "Request successfully sent!")

            } catch (e: Exception) {
                onResult(false, "Error sending request: ${e.message}")
            }
        }
    }
}
