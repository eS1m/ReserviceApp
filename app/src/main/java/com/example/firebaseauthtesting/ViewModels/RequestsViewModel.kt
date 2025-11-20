package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// NOTE: ServiceRequest and RequestState are no longer defined here.

// This ViewModel handles CREATING a request
class RequestsViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _requestState = MutableStateFlow<RequestState>(RequestState.Idle)
    val requestState: StateFlow<RequestState> = _requestState

    fun createServiceRequest(businessId: String, businessName: String, userName: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _requestState.value = RequestState.Error("You must be logged in to make a request.")
            return
        }

        if (userName.isBlank() || userName == "Anonymous") {
            _requestState.value = RequestState.Error("Cannot make request without a valid user name.")
            return
        }

        viewModelScope.launch {
            _requestState.value = RequestState.Loading
            try {
                val newRequestRef = db.collection("serviceRequests").document()

                val request = ServiceRequest(
                    id = newRequestRef.id,
                    userId = currentUser.uid,
                    userName = userName,
                    businessId = businessId,
                    businessName = businessName,
                    status = "Pending",
                    timestamp = Timestamp.now()
                )

                newRequestRef.set(request).await()
                _requestState.value = RequestState.Success

            } catch (e: Exception) {
                _requestState.value = RequestState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun resetState() {
        _requestState.value = RequestState.Idle
    }
}
