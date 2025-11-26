package com.example.firebaseauthtesting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.firebaseauthtesting.Pages.* // Keep this wildcard import
import com.example.firebaseauthtesting.ViewModels.AuthViewModel
import com.example.firebaseauthtesting.ViewModels.BusinessViewModel
import kotlinx.coroutines.flow.first

object ProfileCompletion : Screen("profile_completion/{fullName}/{email}/{password}")

@Composable
fun MyAppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {

        val user = authViewModel.currentUser.first()
        startDestination = if (user != null) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }
    }


    if (startDestination == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1b4332)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    } else {

        NavHost(
            navController = navController,
            startDestination = startDestination!!,
            modifier = modifier
        ) {

            composable(Screen.Login.route) {
                Login(navController = navController, authViewModel = authViewModel)
            }

            composable(Screen.Signup.route) {
                Signup(navController = navController)
            }

            composable(
                route = ProfileCompletion.route,
                arguments = listOf(
                    navArgument("fullName") { type = NavType.StringType },
                    navArgument("email") { type = NavType.StringType },
                    navArgument("password") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val fullName = backStackEntry.arguments?.getString("fullName") ?: ""
                val email = backStackEntry.arguments?.getString("email")?.let {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } ?: ""
                val password = backStackEntry.arguments?.getString("password") ?: ""

                ProfileCompletionScreen(
                    navController = navController,
                    fullName = fullName,
                    email = email,
                    password = password
                )
            }

            composable(Screen.Home.route) {
                Homepage(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }

            composable(Screen.Profile.route) {
                Profile(
                    navController = navController,
                    authViewModel = authViewModel,
                )
            }

            composable(Screen.Business.route) {
                Business(
                    navController = navController,
                    authViewModel = authViewModel,
                )
            }

            composable(
                route = Screen.BusinessMap.route,
                arguments = listOf(
                    navArgument("serviceCategory") { type = NavType.StringType },
                    navArgument("customServiceName") {type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val serviceCategory = backStackEntry.arguments?.getString("serviceCategory")
                val customServiceName = backStackEntry.arguments?.getString("customServiceName")
                if (serviceCategory != null) {
                    BusinessMapScreen(
                        navController = navController,
                        serviceCategory = serviceCategory,
                        customServiceName = customServiceName
                    )
                }
            }

            composable(Screen.BusinessRequests.route) {
                BusinessRequests()
            }

            composable(Screen.ProfileRequests.route) {
                ProfileRequests()
            }
        }
    }
}
