package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class BusinessLocation(
    val uid: String,
    val name: String,
    val location: GeoPoint
)
sealed class MapUiState {
    object Loading : MapUiState()
    data class Success(val locations: List<BusinessLocation>) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

class ServiceMapViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState

    fun fetchBusinessesByService(serviceName: String) {
        _uiState.value = MapUiState.Loading
        viewModelScope.launch {
            try {

                val snapshot = db.collection("businesses")
                    .whereArrayContains("services", serviceName)
                    .get()
                    .await()

                val businessLocations = snapshot.documents.mapNotNull { doc ->
                    val location = doc.getGeoPoint("location")
                    val name = doc.getString("businessName")
                    val uid = doc.getString("uid")

                    if (location != null && name != null && uid != null) {
                        BusinessLocation(uid = uid, name = name, location = location)
                    } else {
                        null
                    }
                }

                _uiState.value = MapUiState.Success(businessLocations)

            } catch (e: Exception) {
                _uiState.value = MapUiState.Error(e.message ?: "Failed to fetch businesses.")
            }
        }
    }
}
