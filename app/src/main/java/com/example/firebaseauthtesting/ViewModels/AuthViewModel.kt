package com.example.firebaseauthtesting.ViewModels

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val fullName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    var business: BusinessDetails? = null
)

data class BusinessDetails(
    @get:PropertyName("isBusiness") @set:PropertyName("isBusiness")
    var isBusiness: Boolean = false,
    var manager: String? = null,
    var services: List<String> = emptyList(),
)

sealed class AuthState {
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Unauthenticated : AuthState()
    object Loading :AuthState()
    data class Error(val message : String) : AuthState()
}

class AuthViewModel() : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    private var userProfileListener: ListenerRegistration? = null
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
    fun fetchUserProfile(userId: String) {
        userProfileListener?.remove()

        userProfileListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AuthViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _userProfile.value = snapshot.toObject(UserProfile::class.java)

                } else {
                    _userProfile.value = null
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        userProfileListener?.remove()
    }

    fun logout() {
        auth.signOut()
        userProfileListener?.remove()
        _userProfile.value = null
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
                    _authState.value = AuthState.Authenticated(user)
                    _userProfile.value = null
                    fetchUserProfile(user.uid)
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

    fun createUserWithProfile(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        location: GeoPoint,
        isBusiness: Boolean
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {

                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user != null) {

                    val businessDetails = BusinessDetails(
                        isBusiness = isBusiness,
                        services = emptyList()
                    )

                    val userProfile = UserProfile(
                        fullName = fullName,
                        phoneNumber = phoneNumber,
                        location = location,
                        email = email,
                        business = businessDetails
                    )

                    db.collection("users").document(user.uid)
                        .set(userProfile)
                        .await()

                    fetchUserProfile(user.uid)
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


