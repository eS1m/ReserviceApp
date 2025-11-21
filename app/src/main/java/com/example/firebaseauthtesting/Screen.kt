package com.example.firebaseauthtesting

sealed class Screen(val route: String, val title: String? = null) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object ProfileCompletion : Screen("profile_completion")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Business : Screen("business")
    object BusinessRequests : Screen("business_requests")
    // In your Screen sealed class
    object ProfileRequests : Screen("profile_requests", "My Requests")
    object BusinessMap : Screen("business_map/{serviceCategory}")


    companion object {
        // BusinessMap is no longer here
    }
}