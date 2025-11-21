package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
// We no longer need the ktx toObjects, but toObject is still useful
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

                val requests = snapshot.documents.mapNotNull { document ->
                    val request = document.toObject(ServiceRequest::class.java)
                    request?.copy(id = document.id)
                }
                _uiState.value = ProfileRequestsUiState.Success(requests)
            } catch (e: Exception) {
                _uiState.value = ProfileRequestsUiState.Error(e.message ?: "Failed to fetch sent requests.")
            }
        }
    }

    fun cancelRequest(requestId: String) {
        // Prevent crash if an empty ID is somehow passed
        if (requestId.isEmpty()) {
            _uiState.value = ProfileRequestsUiState.Error("Invalid request ID.")
            return
        }
        viewModelScope.launch {
            _uiState.value = ProfileRequestsUiState.Loading
            try {
                db.collection("serviceRequests").document(requestId)
                    .update("status", "Cancelled")
                    .await()
                fetchSentRequests()
            } catch (e: Exception) {
                _uiState.value = ProfileRequestsUiState.Error(e.message ?: "Failed to cancel the request.")
            }
        }
    }

    fun submitPayment(requestId: String) {
        if (requestId.isEmpty()) {
            _uiState.value = ProfileRequestsUiState.Error("Invalid request ID for payment.")
            return
        }
        viewModelScope.launch {
            _uiState.value = ProfileRequestsUiState.Loading
            try {
                db.collection("serviceRequests").document(requestId)
                    .update("status", "Confirming Payment")
                    .await()
                fetchSentRequests()
            } catch (e: Exception) {
                _uiState.value = ProfileRequestsUiState.Error(e.message ?: "Failed to submit payment.")
            }
        }
    }
}
