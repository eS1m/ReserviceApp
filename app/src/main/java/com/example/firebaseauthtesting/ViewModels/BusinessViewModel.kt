package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
// FIX: Import the centralized ServiceRequest model
import com.example.firebaseauthtesting.Models.ServiceRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Represents the data for the Business Dashboard
data class BusinessProfile(
    val uid: String = "",
    val businessName: String = "",
    val services: List<String> = emptyList()
)

sealed interface RequestCreationState {
    object Idle : RequestCreationState
    object Loading : RequestCreationState
    object Success : RequestCreationState
    data class Error(val message: String) : RequestCreationState
}

// Represents the different states the Business.kt screen can be in
sealed class BusinessUiState {
    object Loading : BusinessUiState()
    object NotABusiness : BusinessUiState()
    data class NeedsManagerSetup(val businessId: String) : BusinessUiState()
    // IsBusiness now only needs the profile. The requests are handled by a separate flow.
    data class IsBusiness(val profile: BusinessProfile) : BusinessUiState()
    data class Error(val message: String) : BusinessUiState()
}

class BusinessViewModel(private val authViewModel: AuthViewModel) : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val _uiState = MutableStateFlow<BusinessUiState>(BusinessUiState.Loading)
    val uiState: StateFlow<BusinessUiState> = _uiState

    private val _requestCreationState = MutableStateFlow<RequestCreationState>(RequestCreationState.Idle)
    val requestCreationState: StateFlow<RequestCreationState> = _requestCreationState

    // This is the single source of truth for incoming requests
    private val _incomingRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val incomingRequests: StateFlow<List<ServiceRequest>> = _incomingRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Fetch the user status and requests when the ViewModel is created.
        checkUserBusinessStatus()
        fetchIncomingRequests()
    }

    // Called when the screen first launches
    fun checkUserBusinessStatus() {
        val user = authViewModel.currentUser.value ?: run {
            _uiState.value = BusinessUiState.Error("User not logged in.")
            return
        }

        viewModelScope.launch {
            _uiState.value = BusinessUiState.Loading
            try {
                val userDoc = db.collection("users").document(user.uid).get().await()
                val isBusinessUser = userDoc.getBoolean("isBusiness") ?: false

                if (!isBusinessUser) {
                    _uiState.value = BusinessUiState.NotABusiness
                    return@launch
                }

                val businessDoc = db.collection("businesses").document(user.uid).get().await()
                if (!businessDoc.exists()) {
                    _uiState.value = BusinessUiState.Error("Business document not found.")
                    return@launch
                }

                if (businessDoc.getString("manager") == null) {
                    _uiState.value = BusinessUiState.NeedsManagerSetup(user.uid)
                } else {
                    val profile = BusinessProfile(
                        uid = user.uid,
                        businessName = businessDoc.getString("businessName") ?: "No Name",
                        services = businessDoc.get("services") as? List<String> ?: emptyList()
                    )
                    // FIX: Simply transition to the IsBusiness state.
                    // The request list is handled independently by the 'incomingRequests' flow.
                    _uiState.value = BusinessUiState.IsBusiness(profile)
                }
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error(e.message ?: "An error occurred.")
            }
        }
    }

    // Fetches requests and updates the public StateFlow for the BusinessRequestsScreen
    fun fetchIncomingRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = auth.currentUser ?: run {
                _error.value = "User not logged in."
                _isLoading.value = false
                return@launch
            }

            try {
                val snapshot = db.collection("serviceRequests")
                    .whereEqualTo("businessId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                // The toObjects call will now use the correct imported model
                _incomingRequests.value = snapshot.toObjects()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch incoming requests."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Updates the status and then refreshes the list
    fun updateRequestStatus(requestId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                db.collection("serviceRequests").document(requestId)
                    .update("status", newStatus)
                    .await()
                // After updating, just refresh the request list.
                fetchIncomingRequests()
            } catch (e: Exception) {
                _error.value = "Failed to update status: ${e.message}"
            }
        }
    }

    // --- RETAINED ORIGINAL FUNCTIONS ---

    fun upgradeToBusinessAccount() {
        checkUserBusinessStatus()
    }

    fun saveManagerName(managerName: String, callback: (Boolean, String?) -> Unit) {
        val user = authViewModel.currentUser.value ?: run {
            callback(false, "User not logged in.")
            return
        }
        viewModelScope.launch {
            try {
                db.collection("businesses").document(user.uid)
                    .update("manager", managerName)
                    .await()
                checkUserBusinessStatus()
                callback(true, null)
            } catch (e: Exception) {
                callback(false, e.message ?: "Failed to save manager name.")
            }
        }
    }

    fun saveServices(services: List<String>) {
        val user = authViewModel.currentUser.value ?: return
        viewModelScope.launch {
            try {
                db.collection("businesses").document(user.uid)
                    .update("services", services)
                    .await()
                checkUserBusinessStatus()
            } catch (e: Exception) {
                // Optionally update UI with an error message
            }
        }
    }

    // FIX: This function requires the 'scheduledDate' and 'scheduledTime' parameters.
    fun createServiceRequest(
        businessId: String,
        businessName: String,
        userName: String,
        scheduledDate: String,
        scheduledTime: String
    ) {
        val currentUser = Firebase.auth.currentUser ?: run {
            _requestCreationState.value = RequestCreationState.Error("You must be logged in to make a request.")
            return
        }

        if (userName.isBlank() || userName == "Anonymous") {
            _requestCreationState.value = RequestCreationState.Error("Cannot make request without a valid user name.")
            return
        }

        viewModelScope.launch {
            _requestCreationState.value = RequestCreationState.Loading
            try {
                val newRequestRef = db.collection("serviceRequests").document()

                val request = ServiceRequest(
                    id = newRequestRef.id,
                    userId = currentUser.uid,
                    userName = userName,
                    businessId = businessId,
                    businessName = businessName,
                    status = "Pending",
                    timestamp = Timestamp.now(),
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime
                )

                newRequestRef.set(request).await()
                _requestCreationState.value = RequestCreationState.Success

            } catch (e: Exception) {
                _requestCreationState.value = RequestCreationState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun resetRequestCreationState() {
        _requestCreationState.value = RequestCreationState.Idle
    }
}
