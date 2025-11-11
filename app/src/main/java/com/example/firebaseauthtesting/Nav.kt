package com.example.firebaseauthtesting

import Business
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.firebaseauthtesting.Pages.BusinessMapScreen
import com.example.firebaseauthtesting.Pages.BusinessRequestsScreen
import com.example.firebaseauthtesting.Pages.Login
import com.example.firebaseauthtesting.Pages.ProfileCompletionScreen
import com.example.firebaseauthtesting.Pages.HomePage
import com.example.firebaseauthtesting.Pages.Profile
import com.example.firebaseauthtesting.Pages.ProfileRequestsScreen
import com.example.firebaseauthtesting.Pages.Signup
import com.example.firebaseauthtesting.ViewModels.AuthViewModel
import com.example.firebaseauthtesting.ViewModels.AuthState
import com.example.firebaseauthtesting.ViewModels.ProfileCompletionViewModel

@Composable
fun MyAppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val splashScreenRoute = "splash"

    LaunchedEffect(Unit) {
        authViewModel.verifyUserProfile()
    }


    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
            is AuthState.Unauthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = splashScreenRoute,
        modifier = modifier
    ) {
        composable(splashScreenRoute) {
            LaunchedEffect(authState) {
                when (authState) {
                    is AuthState.Authenticated -> {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(splashScreenRoute) { inclusive = true }
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(splashScreenRoute) { inclusive = true }
                        }
                    }
                    is AuthState.Loading -> {
                    }
                    else -> {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(splashScreenRoute) { inclusive = true }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1b4332)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        composable(Screen.Login.route) {
            Login(navController = navController, authViewModel = authViewModel)
        }
        composable(Screen.Signup.route) {
            Signup(navController = navController, authViewModel = authViewModel)
        }
        composable(
            Screen.ProfileCompletion.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType }
            )
        ) {backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val encodedPassword = backStackEntry.arguments?.getString("password") ?: ""
            val password = java.net.URLDecoder.decode(encodedPassword, "UTF-8")

            ProfileCompletionScreen(
                navController = navController,
                authViewModel = authViewModel,
                email = email,
                password = password
            )
        }
        composable(Screen.Home.route) {
            HomePage(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable(Screen.Profile.route){
            Profile(
                navController = navController,
                authViewModel = authViewModel,
            )
        }
        composable(Screen.Business.route) {
            Business(
                navController = navController,
                authViewModel = authViewModel,
                businessViewModel = viewModel()
            )
        }
        composable(
            route = Screen.BusinessMap.route,
            arguments = listOf(navArgument("serviceCategory") { type = NavType.StringType })
        ) { backStackEntry ->
            val serviceCategory = backStackEntry.arguments?.getString("serviceCategory")

            BusinessMapScreen(
                navController = navController,
                authViewModel = authViewModel,
                mapViewModel = viewModel(),
                requestViewModel = viewModel(),
                serviceCategory = serviceCategory ?: "Unknown"
            )
        }

        composable(Screen.BusinessRequests.route) {
            BusinessRequestsScreen(navController = navController)
        }

        composable(Screen.ProfileRequests.route) {
            ProfileRequestsScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
    }
}