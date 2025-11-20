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
                // Step 1: Check the user's account type from the 'users' collection
                val userDoc = db.collection("users").document(currentUser.uid).get(Source.SERVER).await()

                if (userDoc.exists() && userDoc.getBoolean("isBusiness") == true) {
                    // --- THIS IS THE FIX ---
                    // Step 2: Now that we know it's a business, fetch the profile from the 'businesses' collection
                    val businessDoc = db.collection("businesses").document(currentUser.uid).get(Source.SERVER).await()

                    // Step 3: Check for the 'manager' field in the 'businesses' document
                    val managerName = businessDoc.getString("manager") // Using "manager" as you specified

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
                // --- THIS IS THE FIX ---
                // Save the manager name to the 'businesses' collection, not the 'users' collection.
                // We use .set with merge=true to create the doc if it doesn't exist, or update it if it does.
                db.collection("businesses").document(currentUser.uid)
                    .set(mapOf("manager" to name), com.google.firebase.firestore.SetOptions.merge())
                    .await()

                checkUserBusinessStatus()
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error(e.message ?: "Failed to save manager name.")
            }
        }
    }

    // This function remains correct as it modifies the user's role in the 'users' collection
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

    // This function should also write to the 'businesses' collection
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

    // --- Functions for BusinessRequests.kt (No changes needed here) ---

    fun fetchIncomingRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = auth.currentUser ?: run {
                _error.value = "Business user not logged in."
                _isLoading.value = false
                return@launch
            }
            try {
                val snapshot = db.collection("serviceRequests")
                    .whereEqualTo("businessId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val requests = snapshot.documents.mapNotNull { document ->
                    val request = document.toObject(ServiceRequest::class.java)
                    request?.copy(id = document.id)
                }
                _incomingRequests.value = requests
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to fetch incoming requests: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateRequestStatus(requestId: String, newStatus: String) {
        if (requestId.isEmpty()) {
            _error.value = "Cannot update status: Invalid request ID."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("serviceRequests").document(requestId)
                    .update("status", newStatus).await()
                fetchIncomingRequests()
            } catch (e: Exception) {
                _error.value = "Failed to update status: ${e.message}"
            }
        }
    }
}
