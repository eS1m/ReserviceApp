package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint // From the map library

// This sealed class should already exist and is correct
sealed class SaveStatus {
    object Idle : SaveStatus()
    object Success : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}

class ProfileCompletionViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus

    fun createAccountAndProfile(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        location: GeoPoint,
        isBusiness: Boolean
    ) {
        if (_isSaving.value) return

        _isSaving.value = true
        _saveStatus.value = SaveStatus.Idle
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid
                    ?: throw IllegalStateException("Authentication failed, user ID is null.")

                val firestoreGeoPoint = com.google.firebase.firestore.GeoPoint(location.latitude, location.longitude)

                val userDocument = hashMapOf(
                    "id" to userId,
                    "fullName" to fullName,
                    "email" to email,
                    "phoneNumber" to phoneNumber,
                    "location" to firestoreGeoPoint,
                    "isBusiness" to isBusiness
                )
                db.collection("users").document(userId).set(userDocument).await()
                if (isBusiness) {
                    val businessDocument = hashMapOf(
                        "uid" to userId,
                        "businessName" to fullName,
                        "contactEmail" to email,
                        "contactPhone" to phoneNumber,
                        "location" to firestoreGeoPoint,
                        "averageRating" to 0.0,
                        "ratingCount" to 0L,
                        "manager" to null,
                        "services" to emptyList<String>()
                    )


                    db.collection("businesses").document(userId).set(businessDocument).await()
                }

                _saveStatus.value = SaveStatus.Success

            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error(e.message ?: "An unknown error occurred.")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }
}
