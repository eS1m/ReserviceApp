package com.example.firebaseauthtesting.Pages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firebaseauthtesting.R
import com.example.firebaseauthtesting.Screen
import com.example.firebaseauthtesting.ViewModels.AuthViewModel
import com.example.firebaseauthtesting.ViewModels.BusinessProfile
import com.example.firebaseauthtesting.ViewModels.BusinessUiState
import com.example.firebaseauthtesting.ViewModels.BusinessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Business(
    navController: NavController,
    authViewModel: AuthViewModel,
    // Use the factory to correctly initialize the ViewModel
    businessViewModel: BusinessViewModel = viewModel()
) {
    val gradientColors = listOf(
        Color(0xFF1b4332),
        Color(0xFF52b788)
    )

    val uiState by businessViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // This will run when the composable first enters the screen
    LaunchedEffect(Unit) {
        businessViewModel.checkUserBusinessStatus()
    }

    val interphasesFamily = FontFamily(Font(R.font.interphases))
    val pantonFamily = FontFamily(Font(R.font.panton))

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = gradientColors)),
        topBar = {
            TopAppBar(
                title = {
                    Text("Business",
                        fontFamily = interphasesFamily,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(start = 10.dp, top = 20.dp))
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
                    IconButton(onClick = { navController.navigate(Screen.Home.route) }) {
                        Icon(Icons.Outlined.Assignment, contentDescription = "Requests")
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Outlined.Loop, contentDescription = "Reservices")
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val state = uiState) {
                is BusinessUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is BusinessUiState.NeedsManagerSetup -> {
                    ManagerSetupView(
                        businessViewModel = businessViewModel,
                        interphasesFamily = interphasesFamily
                    )
                }
                is BusinessUiState.NotABusiness -> {
                    Text(
                        text = "You are currently not a business account",
                        fontFamily = interphasesFamily,
                        fontSize = 32.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Click the button to become a business!",
                        fontFamily = pantonFamily,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { businessViewModel.upgradeToBusinessAccount() }) {
                        Text(text = "Become a Business")
                    }
                }
                is BusinessUiState.IsBusiness -> {
                    BusinessDashboard(
                        profile = state.profile,
                        onSave = { selectedServices ->
                            businessViewModel.saveServices(selectedServices)
                        },
                        navController = navController
                    )
                }
                is BusinessUiState.Error -> {
                    Text(text = state.message, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun BusinessDashboard(
    profile: BusinessProfile,
    onSave: (List<String>) -> Unit,
    navController: NavController
) {
    val allServices = listOf("Utilities", "Home Repair", "Maid", "Custom")
    var selectedServices by remember { mutableStateOf(profile.services) }
    LaunchedEffect(profile.services) {
        selectedServices = profile.services
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Your Business Dashboard", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Select the services you offer:", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        allServices.forEach { serviceName ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Checkbox(
                    checked = serviceName in selectedServices,
                    onCheckedChange = { isChecked ->
                        val currentServices = selectedServices.toMutableList()
                        if (isChecked) {
                            if (!currentServices.contains(serviceName)) currentServices.add(serviceName)
                        } else {
                            currentServices.remove(serviceName)
                        }
                        selectedServices = currentServices
                    }
                )
                Text(text = serviceName, color = Color.White, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Screen.BusinessRequests.route) }) {
            Text("View Incoming Requests")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onSave(selectedServices) },
            enabled = selectedServices != profile.services
        ) {
            Text("Save Changes")
        }
    }
}

@Composable
fun ManagerSetupView(
    businessViewModel: BusinessViewModel,
    interphasesFamily: FontFamily
) {
    var managerName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Final Step", fontFamily = interphasesFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Please set up the name of the business manager to continue.", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = managerName,
            onValueChange = { managerName = it },
            label = { Text("Manager's Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Black.copy(alpha = 0.2f), unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                cursorColor = Color.White, focusedLabelColor = Color.White, unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedIndicatorColor = Color.White, unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f)
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                businessViewModel.saveManagerName(managerName)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = managerName.isNotBlank()
        ) {
            Text("Save Manager Name")
        }
    }
}