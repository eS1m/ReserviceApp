package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
// We no longer need the ktx toObjects, but toObject is still useful
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// A UI State sealed interface specifically for the ProfileRequests screen
sealed interface ProfileRequestsUiState {
    object Loading : ProfileRequestsUiState
    data class Success(val sentRequests: List<ServiceRequest>) : ProfileRequestsUiState
    data class Error(val message: String) : ProfileRequestsUiState
}

class ProfileRequestsViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog = _showPaymentDialog.asStateFlow()

    private var pendingRequestId: String? = null

    private val _uiState = MutableStateFlow<ProfileRequestsUiState>(ProfileRequestsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _showReviewDialog = MutableStateFlow(false)
    val showReviewDialog = _showReviewDialog.asStateFlow()

    private val _requestToReview = MutableStateFlow<ServiceRequest?>(null)
    val requestToReview = _requestToReview.asStateFlow()

    // Used to hold the user's input in the dialog
    private val _reviewRating = MutableStateFlow(0)
    val reviewRating = _reviewRating.asStateFlow()

    private val _reviewComment = MutableStateFlow("")
    val reviewComment = _reviewComment.asStateFlow()

    init {
        // Fetch the requests as soon as the ViewModel is created
        fetchSentRequests()
    }

    fun onReviewClick(request: ServiceRequest) {
        _requestToReview.value = request
        _showReviewDialog.value = true
    }

    fun onDismissReviewDialog() {
        _showReviewDialog.value = false
        _requestToReview.value = null
        // Reset input fields
        _reviewRating.value = 0
        _reviewComment.value = ""
    }

    fun onRatingChange(newRating: Int) {
        _reviewRating.value = newRating
    }

    fun onCommentChange(newComment: String) {
        _reviewComment.value = newComment
    }

    fun fetchSentRequests() {
        _uiState.value = ProfileRequestsUiState.Loading
        val currentUser = auth.currentUser ?: run {
            _uiState.value = ProfileRequestsUiState.Error("User not logged in.")
            return
        }

        val query = db.collection("serviceRequests")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _uiState.value = ProfileRequestsUiState.Error(e.message ?: "Failed to listen for requests.")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val requests = snapshot.documents.mapNotNull { document ->
                    document.toObject(ServiceRequest::class.java)?.copy(id = document.id)
                }
                _uiState.value = ProfileRequestsUiState.Success(requests)
            }
        }
    }

    fun setPaymentMethodAndProceed(method: String) {
        val requestId = pendingRequestId ?: return // Ensure we have a request ID

        viewModelScope.launch {
            _showPaymentDialog.value = false // Close the dialog
            _uiState.value = ProfileRequestsUiState.Loading // Show loading again
            val currentUser = auth.currentUser ?: return@launch

            try {
                // Step 1: Update the user's profile with the new payment method
                db.collection("users").document(currentUser.uid)
                    .update("paymentMethod", method)
                    .await()

                // Step 2: Now that the method is saved, proceed with the original action
                submitPayment(requestId)

            } catch (e: Exception) {
                _uiState.value = ProfileRequestsUiState.Error(e.message ?: "Failed to save payment method.")
            }
        }
    }

    fun dismissPaymentDialog() {
        _showPaymentDialog.value = false
        pendingRequestId = null
    }

    fun checkAndInitiatePayment(requestId: String) {
        viewModelScope.launch {
            _uiState.value = ProfileRequestsUiState.Loading
            val currentUser = auth.currentUser ?: run {
                _uiState.value = ProfileRequestsUiState.Error("User not logged in")
                return@launch
            }
            try {
                val userDoc = db.collection("users").document(currentUser.uid).get().await()
                val paymentMethod = userDoc.getString("paymentMethod")

                if (paymentMethod.isNullOrBlank()) {

                    pendingRequestId = requestId
                    _showPaymentDialog.value = true
                    fetchSentRequests()
                } else {
                    submitPayment(requestId)
                }
            } catch (e: Exception) {
                _uiState.value = ProfileRequestsUiState.Error(e.message ?: "Failed to check payment method.")
            }
        }
    }

    fun cancelRequest(requestId: String) {
        if (requestId.isEmpty()) {
            _uiState.value = ProfileRequestsUiState.Error("Invalid request ID.")
            return
        }
        viewModelScope.launch {
            try {
                db.collection("serviceRequests").document(requestId)
                    .update("status", "Cancelled")
                    .await()
            } catch (e: Exception) {
                _uiState.value = ProfileRequestsUiState.Error(e.message ?: "Failed to cancel the request.")
            }
        }
    }

    fun submitPayment(requestId: String) {
        if (requestId.isEmpty()) {
            _uiState.value = ProfileRequestsUiState.Error("Invalid request ID for payment.")
            return
        }
        viewModelScope.launch {
            try {
                db.collection("serviceRequests").document(requestId)
                    .update("status", "Confirming Payment")
                    .await()
            } catch (e: Exception) {
                _uiState.value = ProfileRequestsUiState.Error(e.message ?: "Failed to submit payment.")
            }
        }
    }
}
