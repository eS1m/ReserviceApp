package com.example.firebaseauthtesting

sealed class Screen(val route: String, val title: String? = null) {
    object Splash : Screen("splash")
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

    object ServiceMap : Screen("servicemap/{serviceName}") {
        fun createRoute(serviceName: String) = "servicemap/$serviceName"
    }
    fun withArgs(vararg args: String): String {
        var finalRoute = route
        args.forEach { arg ->
            finalRoute = finalRoute.substringBefore("/{") + "/$arg"
        }
        return finalRoute
    }

    companion object {
        // BusinessMap is no longer here
    }
}