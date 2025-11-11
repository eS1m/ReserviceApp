package com.example.firebaseauthtesting.ViewModels

import android.util.Log
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
    data class NeedsManagerSetup(val profile: BusinessProfile) : BusinessUiState()
    data class IsBusiness(val profile: BusinessProfile) : BusinessUiState()
    object NotABusiness : BusinessUiState()
    data class Error(val message: String) : BusinessUiState()
}

data class BusinessProfile(
    val isBusiness: Boolean = false,
    val services: List<String> = emptyList(),
    val manager: String? = null
)

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
                val userProfile = document.toObject(UserProfile::class.java)

                val businessDetails = userProfile?.business
                if (businessDetails != null && businessDetails.isBusiness) {
                    val profile = BusinessProfile(
                        isBusiness = true,
                        services = businessDetails.services,
                        manager = businessDetails.manager
                    )

                    if (businessDetails.manager.isNullOrBlank()) {
                        _uiState.value = BusinessUiState.NeedsManagerSetup(profile)
                    } else {
                        _uiState.value = BusinessUiState.IsBusiness(profile)
                    }


                } else {
                    _uiState.value = BusinessUiState.NotABusiness
                }
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error("Failed to fetch user data: ${e.message}")
                Log.e("BusinessViewModel", "Error in checkUserBusinessStatus", e)
            }
        }
    }

    fun saveManagerName(managerName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                onResult(false, "User not logged in.")
                return@launch
            }

            if (managerName.isBlank()) {
                onResult(false, "Manager name cannot be empty.")
                return@launch
            }

            val userDocRef = db.collection("users").document(userId)

            try {
                userDocRef.update("business.manager", managerName).await()
                checkUserBusinessStatus()
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("BusinessViewModel", "Error saving manager name", e)
                onResult(false, "Failed to save manager name: ${e.message}")
            }
        }
    }

    fun saveServices(selectedServices: List<String>) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                db.collection("users").document(userId).update("business.services", selectedServices).await()

                _uiState.value = BusinessUiState.IsBusiness(BusinessProfile(isBusiness = true, services = selectedServices))
                Log.d("BusinessViewModel", "Services updated successfully.")
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error("Failed to save services: ${e.message}")
                Log.e("BusinessViewModel", "Error saving services", e)
            }
        }
    }

    fun upgradeToBusinessAccount() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val businessData = mapOf(
                    "isBusiness" to true,
                    "services" to emptyList<String>()
                )
                db.collection("users").document(userId).update("business", businessData).await()

                _uiState.value = BusinessUiState.IsBusiness(
                    BusinessProfile(isBusiness = true, services = emptyList())
                )
                Log.d("BusinessViewModel", "User upgraded to business account.")
            } catch (e: Exception) {
                _uiState.value = BusinessUiState.Error("Update failed: ${e.message}")
                Log.e("BusinessViewModel", "Error upgrading account", e)
            }
        }
    }
}

