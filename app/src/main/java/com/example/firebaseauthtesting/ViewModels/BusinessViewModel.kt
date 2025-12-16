package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class BusinessProfile(
    val managerName: String = "",
    val services: List<String> = emptyList()
)

sealed interface BusinessUiState {
    object Loading : BusinessUiState
    data class IsBusiness(val profile: BusinessProfile) : BusinessUiState
    object NeedsManagerSetup : BusinessUiState
    object NotABusiness : BusinessUiState
    data class Error(val message: String) : BusinessUiState
}

class BusinessViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow<BusinessUiState>(BusinessUiState.Loading)
    val uiState: StateFlow<BusinessUiState> = _uiState.asStateFlow()

    private val _incomingRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val incomingRequests: StateFlow<List<ServiceRequest>> = _incomingRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun checkUserBusinessStatus() {
        viewModelScope.launch {
            _uiState.value = BusinessUiState.Loading
            val currentUser = auth.currentUser ?: run {
                _uiState.value = BusinessUiState.Error("User not logged in")
                return@launch
            }

            try {
                val userDoc = db.collection("users").document(currentUser.uid).get(Source.SERVER).await()

                if (userDoc.exists() && userDoc.getBoolean("isBusiness") == true) {
                    val businessDoc = db.collection("businesses").document(currentUser.uid).get(Source.SERVER).await()

                    val managerName = businessDoc.getString("managerName")

                    if (managerName.isNullOrBlank()) {
                        _uiState.value = BusinessUiState.NeedsManagerSetup
                    } else {
                        val services = businessDoc.get("services") as? List<String> ?: emptyList()
                        _uiState.value = BusinessUiState.IsBusiness(BusinessProfile(managerName, services))
                    }
                } else {
                    _uiState.value = BusinessUiState.NotABusiness
                }
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun saveManagerName(name: String) {
        viewModelScope.launch {
            _uiState.value = BusinessUiState.Loading
            val currentUser = auth.currentUser ?: run {
                _uiState.value = BusinessUiState.Error("User not logged in.")
                return@launch
            }
            try {
                db.collection("businesses").document(currentUser.uid)
                    .set(mapOf("managerName" to name), com.google.firebase.firestore.SetOptions.merge())
                    .await()

                checkUserBusinessStatus()
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error(e.message ?: "Failed to save manager name.")
            }
        }
    }

    fun upgradeToBusinessAccount() {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            try {
                db.collection("users").document(currentUser.uid)
                    .update("isBusiness", true)
                    .await()
                checkUserBusinessStatus()
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error("Failed to upgrade account: ${e.message}")
            }
        }
    }

    fun saveServices(services: List<String>) {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            try {
                db.collection("businesses").document(currentUser.uid)
                    .set(mapOf("services" to services), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                checkUserBusinessStatus()
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error("Failed to save services: ${e.message}")
            }
        }
    }

    fun fetchIncomingRequests() {
        _isLoading.value = true
        val currentUser = auth.currentUser ?: run {
            _error.value = "Business user not logged in."
            _isLoading.value = false
            return
        }

        val query = db.collection("serviceRequests")
            .whereEqualTo("businessId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)


        query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _error.value = "Failed to listen for requests: ${e.message}"
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val requests = snapshot.documents.mapNotNull { document ->
                    val request = document.toObject(ServiceRequest::class.java)
                    request?.copy(id = document.id)
                }
                _incomingRequests.value = requests
                _error.value = null
            }

            _isLoading.value = false
        }
    }

    fun acceptRequestWithAmount(requestId: String, amount: Double) {
        if (requestId.isEmpty()) {
            _error.value = "Cannot update status: Invalid request ID."
            return
        }
        viewModelScope.launch {
            try {
                db.collection("serviceRequests").document(requestId)
                    .update(mapOf(
                        "status" to "Accepted",
                        "amount" to amount
                    )).await()
            } catch (e: Exception) {
                _error.value = "Failed to update status: ${e.message}"
            }
        }
    }

    fun updateRequestStatus(requestId: String, newStatus: String) {
        if (requestId.isEmpty()) {
            _error.value = "Cannot update status: Invalid request ID."
            return
        }
        viewModelScope.launch {
            try {
                db.collection("serviceRequests").document(requestId)
                    .update("status", newStatus).await()
            } catch (e: Exception) {
                _error.value = "Failed to update status: ${e.message}"
            }
        }
    }
}
