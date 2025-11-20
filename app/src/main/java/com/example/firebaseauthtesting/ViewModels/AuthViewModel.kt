package com.example.firebaseauthtesting.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _signupSuccess = MutableStateFlow(false)
    val signupSuccess: StateFlow<Boolean> = _signupSuccess

    private val _fullName = MutableStateFlow<String?>(null)
    val fullName: StateFlow<String?> = _fullName

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()


    init {
        viewModelScope.launch {
            _currentUser.collect { user ->
                if (user != null) {

                    fetchUserFullName(user.uid)
                } else {
                    _fullName.value = null
                }
            }
        }
    }


    private fun fetchUserFullName(userId: String) {
        viewModelScope.launch {
            try {
                val document = db.collection("users").document(userId).get().await()
                _fullName.value = document.getString("fullName")
            } catch (e: Exception) {
                _error.value = "Failed to fetch user profile: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun login(email: String, password: String) {
        if (_currentUser.value != null || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _currentUser.value = auth.currentUser
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred."
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }
}
