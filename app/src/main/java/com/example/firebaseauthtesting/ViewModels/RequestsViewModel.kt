package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class RequestsUiState {
    object Loading : RequestsUiState()
    data class Success(val requests: List<ServiceRequest>) : RequestsUiState()
    data class Error(val message: String) : RequestsUiState()
}

class RequestsViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val _uiState = MutableStateFlow<RequestsUiState>(RequestsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        listenForBusinessRequests()
    }

    private fun listenForBusinessRequests() {
        _uiState.value = RequestsUiState.Loading
        val businessId = auth.currentUser?.uid
        if (businessId == null) {
            _uiState.value = RequestsUiState.Error("Not logged in.")
            return
        }
        val query = db.collection("requests")
            .whereEqualTo("businessId", businessId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        query.addSnapshotListener { snapshot, e ->
            if (e != null) {

                _uiState.value = RequestsUiState.Error("Failed to listen for requests: ${e.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {

                val requests = snapshot.toObjects<ServiceRequest>()
                _uiState.value = RequestsUiState.Success(requests)
            } else {

                _uiState.value = RequestsUiState.Error("No data received.")
            }
        }
    }

    fun updateRequestStatus(requestId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                db.collection("requests").document(requestId).update("status", newStatus).await()
            } catch (e: Exception) {

            }
        }
    }
}