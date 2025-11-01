package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Data class to hold only the necessary info for a map marker
data class BusinessMarker(
    val uid: String,
    val fullName: String,
    val location: GeoPoint
)

// UI State for the map screen
sealed class MapUiState {
    object Loading : MapUiState()
    data class Success(val businesses: List<BusinessMarker>) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

class BusinessMapViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchAllBusinesses()
    }

    private fun fetchAllBusinesses() {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            try {
                // Query the 'users' collection for documents where 'isBusiness' is true
                val querySnapshot = db.collection("users")
                    .whereEqualTo("isBusiness", true)
                    .get()
                    .await()

                val businessList = querySnapshot.documents.mapNotNull { document ->
                    // Safely extract data and create a BusinessMarker object
                    val fullName = document.getString("fullName")
                    val location = document.getGeoPoint("location")
                    if (fullName != null && location != null) {
                        BusinessMarker(
                            uid = document.id,
                            fullName = fullName,
                            location = location
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
}