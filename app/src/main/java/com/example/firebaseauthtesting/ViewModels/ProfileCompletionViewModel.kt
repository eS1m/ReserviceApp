package com.example.firebaseauthtesting.ViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
sealed class ProfileUpdateState {
    data object Idle : ProfileUpdateState()
    data object Loading : ProfileUpdateState()
    data object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

class ProfileCompletionViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow< ProfileUpdateState>(ProfileUpdateState.Idle)
    val uiState = _uiState.asStateFlow()

    fun createAndSaveProfile(fullName: String, phone: String, location: GeoPoint) {
        viewModelScope.launch {
            _uiState.value = ProfileUpdateState.Loading
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = ProfileUpdateState.Error("You are not logged in.")
                return@launch
            }

            // Create a user object with all data, including a default 0,0 location.
            val user = hashMapOf(
                "uid" to userId,
                "email" to auth.currentUser?.email, // Good to store the email
                "fullName" to fullName,
                "phoneNum" to phone,
                "location" to location,
                "createdAt" to System.currentTimeMillis() // Good for tracking
            )

            try {
                // Use .set() to create the document with the user's ID
                db.collection("users").document(userId)
                    .set(user)
                    .await()

                _uiState.value = ProfileUpdateState.Success
                Log.d("ProfileCompletionVM", "User profile document created successfully.")

            } catch (e: Exception) {
                Log.e("ProfileCompletionVM", "Error creating profile document", e)
                _uiState.value = ProfileUpdateState.Error(e.message ?: "Failed to save profile.")
            }
        }
    }
}
