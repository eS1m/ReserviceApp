package com.example.firebaseauthtesting.ViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseauthtesting.Models.Business
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BusinessMapViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _businesses = MutableStateFlow<List<Business>>(emptyList())
    val businesses = _businesses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()


    fun fetchBusinesses(serviceCategory: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                _error.value = "User not logged in. Cannot display businesses."
                _isLoading.value = false
                _businesses.value = emptyList()
                Log.w("BusinessMapViewModel", "No user logged in.")
                return@launch
            }

            try {
                Log.d("BusinessMapViewModel", "Fetching businesses for category: '$serviceCategory', excluding user: $currentUserId")

                val result = db.collection("businesses")
                    .whereArrayContains("services", serviceCategory)
                    .whereNotEqualTo("uid", currentUserId)
                    .get()
                    .await()

                val businessList = result.toObjects<Business>()
                Log.d("BusinessMapViewModel", "Query successful. Found ${businessList.size} businesses.")
                _businesses.value = businessList
                Log.d("BusinessMapViewModel", "Successfully loaded ${businessList.size} businesses.")

            } catch (e: Exception) {
                Log.e("BusinessMapViewModel", "Error fetching businesses", e)
                _error.value = "Failed to load businesses: ${e.message}"
                _businesses.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
