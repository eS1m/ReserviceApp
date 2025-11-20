package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// FIX 1: Import your actual 'Business' model.
import com.example.firebaseauthtesting.Models.Business
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// FIX: Rename this sealed class to avoid redeclaration error.
sealed class ServiceRequestState {
    object Idle : ServiceRequestState()
    object Loading : ServiceRequestState()
    data class Success(val message: String) : ServiceRequestState()
    data class Error(val message: String) : ServiceRequestState()
}

class BusinessDetailsViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // FIX 2: Change the StateFlow to hold a List of 'Business' objects.
    private val _businesses = MutableStateFlow<List<Business>>(emptyList())
    val businesses: StateFlow<List<Business>> = _businesses

    // FIX 3: The selected business must also be of type 'Business'.
    private val _selectedBusiness = MutableStateFlow<Business?>(null)
    val selectedBusiness: StateFlow<Business?> = _selectedBusiness

    // FIX: Update the type of _requestState and requestState.
    private val _requestState = MutableStateFlow<ServiceRequestState>(ServiceRequestState.Idle)
    val requestState: StateFlow<ServiceRequestState> = _requestState

    fun getBusinessProfiles() {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    _businesses.value = emptyList()
                    return@launch
                }

                // This query correctly filters out the user's own business on the server.
                val result = db.collection("businesses")
                    // IMPORTANT: Ensure your Firestore documents have a 'uid' field for this to work.
                    .whereNotEqualTo("uid", currentUserId)
                    .get()
                    .await()

                // FIX 4: Convert the Firestore documents to your 'Business' class.
                _businesses.value = result.documents.mapNotNull { document ->
                    document.toObject(Business::class.java)?.copy(uid = document.id)
                }

            } catch (e: Exception) {
                _businesses.value = emptyList()
            }
        }
    }

    fun selectBusiness(businessId: String?) {
        // Allow clearing the selection with a null ID.
        if (businessId == null) {
            _selectedBusiness.value = null
            return
        }

        viewModelScope.launch {
            try {
                val document = db.collection("businesses").document(businessId).get().await()
                // FIX 5: Convert the single document to your 'Business' class.
                _selectedBusiness.value = document.toObject(Business::class.java)?.copy(uid = document.id)
            } catch (e: Exception) {
                _selectedBusiness.value = null
            }
        }
    }

    fun createServiceRequest(
        businessId: String,
        businessName: String,
        userName: String,
        scheduledDate: String,
        scheduledTime: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // FIX: Update the state value.
            _requestState.value = ServiceRequestState.Error("You must be logged in to make a request.")
            return
        }
        // FIX: Update the state value.
        _requestState.value = ServiceRequestState.Loading
        viewModelScope.launch {
            try {
                // FIX: Remove the 'timestamp' line. Firestore will add it on the server.
                val newRequest = ServiceRequest(
                    userId = currentUser.uid,
                    userName = userName,
                    businessId = businessId,
                    businessName = businessName,
                    status = "Pending", // Default status
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime
                )
                // Add the new request to the "serviceRequests" collection
                db.collection("serviceRequests").add(newRequest).await()
                // FIX: Update the state value.
                _requestState.value = ServiceRequestState.Success("Request sent successfully!")
            } catch (e: Exception) {
                // FIX: Update the state value.
                _requestState.value = ServiceRequestState.Error("Failed to send request: ${e.message}")
            }
        }
    }

    fun resetRequestState() {
        // FIX: Update the state value.
        _requestState.value = ServiceRequestState.Idle
    }
}
