package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddReviewViewModel : ViewModel() {

    private val db = Firebase.firestore

    fun submitReview(
        businessId: String,
        userId: String,
        userName: String,
        rating: Double,
        comment: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val businessRef = db.collection("businesses").document(businessId)
                val newRatingRef = db.collection("ratings").document() // Create a new empty document reference

                // Create the new rating object
                val newRating = hashMapOf(
                    "businessId" to businessId,
                    "userId" to userId,
                    "userName" to userName,
                    "rating" to rating,
                    "comment" to comment,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                // Run a transaction to ensure data consistency
                db.runTransaction { transaction ->
                    val businessSnapshot = transaction.get(businessRef)

                    // Get current rating info from the business document
                    val oldRatingCount = businessSnapshot.getLong("ratingCount") ?: 0L
                    val oldAverageRating = businessSnapshot.getDouble("averageRating") ?: 0.0

                    // Calculate the new average rating
                    val newRatingCount = oldRatingCount + 1
                    val newAverageRating = ((oldAverageRating * oldRatingCount) + rating) / newRatingCount

                    // 1. Update the business document with new rating info
                    transaction.update(businessRef, "ratingCount", newRatingCount)
                    transaction.update(businessRef, "averageRating", newAverageRating)

                    // 2. Create the new document in the 'ratings' collection
                    transaction.set(newRatingRef, newRating)

                    null // Transaction must return null
                }.await()

                onComplete(true, null)

            } catch (e: Exception) {
                onComplete(false, e.message ?: "An error occurred.")
            }
        }
    }
}
