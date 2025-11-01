package com.example.firebaseauthtesting.ViewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.io.path.exists

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val fullName: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomepageViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadUserName()
    }

    private fun loadUserName() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = HomeUiState.Error("User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("HomepageViewModel", "Fetching profile for UID: $userId")
                val document = db.collection("users").document(userId).get().await()

                // 6. Fetch "fullName" instead of "username".
                val fullName = document.getString("fullName")

                if (document.exists() && !fullName.isNullOrBlank()) {
                    _uiState.value = HomeUiState.Success(fullName)
                    Log.d("HomepageViewModel", "Successfully loaded name: $fullName")
                } else {
                    _uiState.value = HomeUiState.Error("Could not find user's name.")
                    Log.w("HomepageViewModel", "Document for user $userId missing or name is blank.")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Failed to load profile.")
                Log.e("HomepageViewModel", "Error fetching user profile", e)
            }
        }
    }
}