package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// State definition for the Business page UI
sealed class BusinessUiState {
    object Loading : BusinessUiState()
    object IsBusiness : BusinessUiState()
    object NotABusiness : BusinessUiState()
    data class Error(val message: String) : BusinessUiState()
}

class BusinessViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val _uiState = MutableStateFlow<BusinessUiState>(BusinessUiState.Loading)
    val uiState: StateFlow<BusinessUiState> = _uiState

    init {
        checkUserBusinessStatus()
    }

    fun checkUserBusinessStatus() {
        viewModelScope.launch {
            _uiState.value = BusinessUiState.Loading
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = BusinessUiState.Error("User not logged in.")
                return@launch
            }

            try {
                val document = db.collection("users").document(userId).get().await()
                val isBusiness = document.getBoolean("isBusiness") ?: false
                _uiState.value = if (isBusiness) {
                    BusinessUiState.IsBusiness
                } else {
                    BusinessUiState.NotABusiness
                }
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error("Failed to fetch user data: ${e.message}")
            }
        }
    }

    fun upgradeToBusinessAccount() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                db.collection("users").document(userId).update("isBusiness", true).await()
                _uiState.value = BusinessUiState.IsBusiness
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error("Update failed: ${e.message}")
            }
        }
    }
}

