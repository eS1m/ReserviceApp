package com.example.firebaseauthtesting.ViewModels

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val fullName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0), // Default location
    val isBusiness: Boolean = false
)
sealed class AuthState {
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Unauthenticated : AuthState()
    object Loading :AuthState()
    data class Error(val message : String) : AuthState()
    data object RequiresProfileCompletion : AuthState()
}

class AuthViewModel() : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    val authState = _authState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    init {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(user)
                fetchUserProfile(user.uid)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    private fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val document = db.collection("users").document(userId).get().await()
                _userProfile.value = document.toObject<UserProfile>()
            } catch (e: Exception) {
                _userProfile.value = null
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _authState.value = AuthState.Unauthenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Logout failed: ${e.message}")
            }
        }
    }
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email or password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                authResult.user?.let { user ->
                    // 3. On successful login, pass the user object
                    _authState.value = AuthState.Authenticated(user)
                } ?: run {
                    _authState.value = AuthState.Error("Login failed: User is null")
                }
                Log.d("AuthViewModel", "Login successful for email: $email")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed: ${e.message}", e)
                _authState.value = AuthState.Error(e.message ?: "Login failed: Unknown error")
            }
        }
    }

    fun signup(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    // SUCCESS: User is created in Firebase Auth.
                    // Now, signal that the app must navigate to the profile completion screen.
                    _authState.value = AuthState.RequiresProfileCompletion
                    Log.d("AuthViewModel", "Signup success. UID: ${firebaseUser.uid}. Signaling profile completion is required.")
                } else {
                    _authState.value = AuthState.Error("Signup successful, but failed to get user.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Signup failed", e)
                _authState.value = AuthState.Error(e.message ?: "Signup failed.")
            }
        }
    }

    fun createUserWithProfile(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        location: com.google.firebase.firestore.GeoPoint,
        isBusiness: Boolean
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {

                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user != null) {

                    val userProfile = hashMapOf(
                        "fullName" to fullName,
                        "phoneNumber" to phoneNumber,
                        "location" to location,
                        "email" to email,
                        "isBusiness" to isBusiness
                    )

                    db.collection("users").document(user.uid)
                        .set(userProfile)
                        .await()


                    _authState.value = AuthState.Authenticated(user)

                } else {
                    throw IllegalStateException("User creation returned null user.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign up failed: ${e.message}")
            }
        }
    }

    fun verifyUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        fetchUserProfile(userId)
    }
}
