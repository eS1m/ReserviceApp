package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RequestsViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    private val _userRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())

    val userRequests: StateFlow<List<ServiceRequest>> = _userRequests.asStateFlow()

    private val _businessRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val businessRequests: StateFlow<List<ServiceRequest>> = _businessRequests.asStateFlow()


    fun fetchUserRequests() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) return@launch

            _isLoading.value = true
            try {
                db.collection("serviceRequests")
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            // Handle error
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            _userRequests.value = snapshot.toObjects()
                        }
                    }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchBusinessRequests() {
        // ... (Implementation for fetching business requests)
    }

    fun updateRequestStatus(requestId: String, newStatus: String) {
        // ... (Implementation for updating status)
    }
}
