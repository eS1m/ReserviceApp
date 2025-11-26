package com.example.firebaseauthtesting.Pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CleanHands
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.firebaseauthtesting.R
import com.example.firebaseauthtesting.Screen
import com.example.firebaseauthtesting.ViewModels.AuthViewModel

data class ServiceCategory(val name: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Homepage(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val fullName by authViewModel.fullName.collectAsStateWithLifecycle()

    val gradientColors = listOf(Color(0xFF1b4332), Color(0xFF52b788))
    val interphasesFamily = FontFamily(Font(R.font.interphases))

    val serviceCategories = listOf(
        ServiceCategory("Utilities", Icons.Outlined.Settings),
        ServiceCategory("Home Repair", Icons.Outlined.Home),
        ServiceCategory("Maid", Icons.Outlined.CleanHands),
        ServiceCategory("Custom", Icons.Outlined.Create)
    )

    var showCustomServiceDialog by remember { mutableStateOf(false) }
    var customServiceName by remember { mutableStateOf("") }

    if (showCustomServiceDialog) {
        AlertDialog(
            onDismissRequest = {
                showCustomServiceDialog = false
                customServiceName = "" // Reset on dismiss
            },
            title = { Text("Custom Service") },
            text = {
                Column {
                    Text("What service are you looking for?")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customServiceName,
                        onValueChange = { customServiceName = it },
                        label = { Text("Service Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customServiceName.isNotBlank()) {
                            navController.navigate(Screen.BusinessMap.route + "/Custom")
                            showCustomServiceDialog = false
                            customServiceName = "" // Reset after search
                        }
                    },
                    enabled = customServiceName.isNotBlank()
                ) {
                    Text("Search")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showCustomServiceDialog = false
                    customServiceName = "" // Reset on cancel
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = gradientColors)),
        topBar = {
            TopAppBar(
                modifier = Modifier.height(112.dp),
                title = {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Welcome,",
                            fontFamily = interphasesFamily,
                            fontSize = 24.sp
                        )
                        Text(
                            text = fullName ?: "loading...",
                            fontFamily = interphasesFamily,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                ),
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Black.copy(alpha = 0.3f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigate(Screen.ProfileRequests.route)}) {
                        Icon(Icons.Outlined.Assignment, contentDescription = "Requests")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Business.route) }) {
                        Icon(Icons.Outlined.Loop, contentDescription = "Business")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(Icons.Outlined.Person, contentDescription = "Profile")
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(top = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "What service are you looking for?",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontFamily = interphasesFamily
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(serviceCategories) { category ->
                    ServiceCard(category = category) {
                        if (category.name == "Custom") {
                            showCustomServiceDialog = true
                        } else {
                        navController.navigate(Screen.BusinessMap.route + "/${category.name}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCard(category: ServiceCategory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(width = 160.dp, height = 140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                modifier = Modifier.size(56.dp),
                tint = Color.White // Ensure the icon is visible
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
