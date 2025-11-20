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

    private val _businesses = MutableStateFlow<List<Business>>(emptyList())
    val businesses: StateFlow<List<Business>> = _businesses

    private val _selectedBusiness = MutableStateFlow<Business?>(null)
    val selectedBusiness: StateFlow<Business?> = _selectedBusiness

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

                val result = db.collection("businesses")
                    .whereNotEqualTo("uid", currentUserId)
                    .get()
                    .await()

                _businesses.value = result.documents.mapNotNull { document ->
                    document.toObject(Business::class.java)?.copy(uid = document.id)
                }

            } catch (e: Exception) {
                _businesses.value = emptyList()
            }
        }
    }

    fun requestPayment(requestId: String) {
        viewModelScope.launch {
            try {
                db.collection("serviceRequests").document(requestId)
                    .update("status", "Pending Payment")
                    .await()
            } catch (e: Exception) {
            }
        }
    }


    fun selectBusiness(businessId: String?) {
        if (businessId == null) {
            _selectedBusiness.value = null
            return
        }

        viewModelScope.launch {
            try {
                val document = db.collection("businesses").document(businessId).get().await()
                _selectedBusiness.value = document.toObject(Business::class.java)?.copy(uid = document.id)
            } catch (e: Exception) {
                _selectedBusiness.value = null
            }
        }
    }

    fun createServiceRequest(
        business: Business, // Was: businessId: String, businessName: String
        userName: String,
        scheduledDate: String,
        scheduledTime: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _requestState.value = ServiceRequestState.Error("You must be logged in to make a request.")
            return
        }
        _requestState.value = ServiceRequestState.Loading
        viewModelScope.launch {
            try {
                val newRequest = ServiceRequest(
                    userId = currentUser.uid,
                    userName = userName,
                    businessId = business.uid, // Get ID from the object
                    businessName = business.businessName,
                    status = "Pending",
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime
                )
                db.collection("serviceRequests").add(newRequest).await()
                _requestState.value = ServiceRequestState.Success("Request sent successfully!")
            } catch (e: Exception) {
                _requestState.value = ServiceRequestState.Error("Failed to send request: ${e.message}")
            }
        }
    }

    fun resetRequestState() {
        _requestState.value = ServiceRequestState.Idle
    }
}
