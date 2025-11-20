package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.firebaseauthtesting.Models.ServiceRequest

// A UI State sealed interface specifically for the ProfileRequests screen
sealed interface ProfileRequestsUiState {
    object Loading : ProfileRequestsUiState
    data class Success(val sentRequests: List<ServiceRequest>) : ProfileRequestsUiState
    data class Error(val message: String) : ProfileRequestsUiState
}

class ProfileRequestsViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow<ProfileRequestsUiState>(ProfileRequestsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        // Fetch the requests as soon as the ViewModel is created
        fetchSentRequests()
    }

    fun fetchSentRequests() {
        viewModelScope.launch {
            _uiState.value = ProfileRequestsUiState.Loading
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.value = ProfileRequestsUiState.Error("User not logged in.")
                return@launch
            }

            try {
                val snapshot = db.collection("serviceRequests")
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val requests = snapshot.toObjects<ServiceRequest>()
                _uiState.value = ProfileRequestsUiState.Success(requests)
            } catch (e: Exception) {
                _uiState.value = ProfileRequestsUiState.Error(e.message ?: "Failed to fetch sent requests.")
            }
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            try {
                db.collection("serviceRequests").document(requestId)
                    .update("status", "Cancelled")
                    .await()
                fetchSentRequests()
            } catch (e: Exception) {
                println("Error cancelling request: ${e.message}")
            }
        }
    }
}
