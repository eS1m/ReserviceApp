package com.example.firebaseauthtesting.ViewModels

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore


import androidx.lifecycle.ViewModel
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ProfileRequestsUiState {
    object Loading : ProfileRequestsUiState()
    data class Success(val requests: List<ServiceRequest>) : ProfileRequestsUiState()
    data class Error(val message: String) : ProfileRequestsUiState()
}

class ProfileRequestsViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val _uiState = MutableStateFlow<ProfileRequestsUiState>(ProfileRequestsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        listenForUserRequests()
    }

    private fun listenForUserRequests() {
        _uiState.value = ProfileRequestsUiState.Loading
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = ProfileRequestsUiState.Error("You are not logged in.")
            return
        }

        val query = db.collection("requests")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _uiState.value = ProfileRequestsUiState.Error("Failed to listen for requests: ${e.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val requests = snapshot.toObjects<ServiceRequest>()
                _uiState.value = ProfileRequestsUiState.Success(requests)
            } else {
                _uiState.value = ProfileRequestsUiState.Error("No data received.")
            }
        }
    }
}