package com.example.firebaseauthtesting

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Signup : Screen("signup_screen")
    object ProfileCompletion : Screen("profile_completion/{email}/{password}")
    object Home : Screen("home_screen")
    object Profile : Screen("profile_screen")
    object Business : Screen("business_screen")
    object BusinessMap: Screen("business_map_screen")
}