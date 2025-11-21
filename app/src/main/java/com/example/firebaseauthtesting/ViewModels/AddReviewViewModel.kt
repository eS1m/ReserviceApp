package com.example.firebaseauthtesting.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauthtesting.Models.Business
import com.example.firebaseauthtesting.Models.Review
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddReviewViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // To handle UI state like Loading, Success, Error
    sealed class ReviewSubmissionState {
        object Idle : ReviewSubmissionState()
        object Loading : ReviewSubmissionState()
        object Success : ReviewSubmissionState()
        data class Error(val message: String) : ReviewSubmissionState()
    }

    private val _submissionState = MutableStateFlow<ReviewSubmissionState>(ReviewSubmissionState.Idle)
    val submissionState = _submissionState.asStateFlow()

    fun submitReview(
        businessId: String,
        businessName: String,
        clientId: String,
        clientName: String,
        rating: Int,
        comment: String,
        serviceRequestId: String
    ) {
        viewModelScope.launch {
            _submissionState.value = ReviewSubmissionState.Loading
            try {
                db.runTransaction { transaction ->
                    val businessRef = db.collection("businesses").document(businessId)
                    val businessDoc = transaction.get(businessRef)

                    val oldRatingTotal = businessDoc.getDouble("averageRating") ?: 0.0
                    val oldRatingCount = businessDoc.getLong("ratingCount")?.toInt() ?: 0

                    val newRatingCount = oldRatingCount + 1
                    val newRatingTotal = oldRatingTotal + rating
                    val newAverageRating = newRatingTotal / newRatingCount

                    // 1. Update the business document
                    transaction.update(businessRef, "averageRating", newAverageRating)
                    transaction.update(businessRef, "ratingCount", newRatingCount)

                    // 2. Create the new review document
                    val reviewRef = db.collection("reviews").document() // Auto-generate ID
                    val newReview = Review(
                        businessId = businessId,
                        businessName = businessName,
                        clientId = clientId,
                        clientName = clientName,
                        rating = rating,
                        comment = comment
                    )
                    transaction.set(reviewRef, newReview)

                    // 3. Update the service request status to prevent another review
                    val requestRef = db.collection("serviceRequests").document(serviceRequestId)
                    transaction.update(requestRef, "status", "Reviewed")

                    null // Transaction result
                }.await()

                _submissionState.value = ReviewSubmissionState.Success
                Log.d("AddReviewViewModel", "Transaction successful")

            } catch (e: Exception) {
                Log.e("AddReviewViewModel", "Transaction failed", e)
                _submissionState.value = ReviewSubmissionState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun resetState() {
        _submissionState.value = ReviewSubmissionState.Idle
    }
}
