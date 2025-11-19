package com.example.firebaseauthtesting.ViewModels

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.ViewModel
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import android.util.Log

sealed class ProfileRequestsUiState {
    object Loading : ProfileRequestsUiState()
    data class Success(val requests: List<ServiceRequest>) : ProfileRequestsUiState()
    data class Error(val message: String) : ProfileRequestsUiState()
    object NeedsPaymentMethodSetup : ProfileRequestsUiState()
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

    fun dismissPaymentDialog() {
        val lastSuccessState = (_uiState.value as? ProfileRequestsUiState.Success)
        if (lastSuccessState != null) {
            _uiState.value = lastSuccessState
        } else {
            listenForUserRequests()
        }
    }

    fun cancelRequest(requestId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (requestId.isBlank()) {
                onResult(false, "Invalid request ID.")
                return@launch
            }

            try {
                db.collection("requests").document(requestId)
                    .update("status", "cancelled")
                    .await()

                onResult(true, "Request successfully cancelled.")

            } catch (e: Exception) {
                Log.e("ProfileRequestsVM", "Error cancelling request", e)
                onResult(false, "Failed to cancel request: ${e.message}")
            }
        }
    }

    fun initiatePayment() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = ProfileRequestsUiState.Error("User not logged in.")
                return@launch
            }
            try {
                val userDoc = db.collection("users").document(userId).get().await()

                if (userDoc.contains("paymentMethod")) {
                    Log.d("Payment", "User already has a payment method: ${userDoc.getString("paymentMethod")}")
                    // TODO: Navigate to the full payment screen
                } else {
                    Log.d("Payment", "User needs to set up a payment method.")
                    _uiState.value = ProfileRequestsUiState.NeedsPaymentMethodSetup
                }
            } catch (e: Exception) {
                _uiState.value = ProfileRequestsUiState.Error("Failed to check payment status: ${e.message}")
            }
        }
    }

    fun savePaymentMethod(method: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                onResult(false, "User not logged in.")
                return@launch
            }
            try {
                db.collection("users").document(userId)
                    .update("paymentMethod", method)
                    .await()
                _uiState.value = ProfileRequestsUiState.Success((uiState.value as? ProfileRequestsUiState.Success)?.requests ?: emptyList())
                onResult(true, "Payment method saved!")
            } catch (e: Exception) {
                onResult(false, "Failed to save payment method: ${e.message}")
            }
        }
    }
}