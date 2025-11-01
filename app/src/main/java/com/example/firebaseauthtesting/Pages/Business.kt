import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.firebaseauthtesting.R
import com.example.firebaseauthtesting.Screen
import com.example.firebaseauthtesting.ViewModels.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebaseauthtesting.ViewModels.BusinessProfile
import com.example.firebaseauthtesting.ViewModels.BusinessUiState
import com.example.firebaseauthtesting.ViewModels.BusinessViewModel
import kotlin.collections.remove
import kotlin.collections.toMutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Business(
    navController: NavController,
    authViewModel: AuthViewModel,
    businessViewModel: BusinessViewModel = viewModel()
) {
    val gradientColors = listOf(
        Color(0xFF1b4332),
        Color(0xFF52b788)
    )

    val uiState by businessViewModel.uiState.collectAsState()

    val interphasesFamily = FontFamily(
        Font(R.font.interphases)
    )
    val pantonFamily = FontFamily(
        Font(R.font.panton)
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            ),
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
                    IconButton(onClick = {
                        navController.navigate(Screen.Home.route)
                    }) {
                        Icon(Icons.Outlined.Assignment, contentDescription = "Requests")
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Outlined.Loop, contentDescription = "Reservices")
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.Profile.route)
                    }) {
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
                    CircularProgressIndicator(color = Color.White)
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
                    Button(
                        onClick = {
                            businessViewModel.upgradeToBusinessAccount()
                        }
                    ) {
                        Text(text = "Become a Business")
                    }
                }

                is BusinessUiState.IsBusiness -> {
                    BusinessDashboard(
                        profile = state.profile,
                        onSave = { selectedServices ->
                            businessViewModel.saveServices(selectedServices)
                        }
                    )
                }
                is BusinessUiState.Error -> {
                    val errorMessage = (uiState as BusinessUiState.Error).message
                    Text(text = errorMessage, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun BusinessDashboard(profile: BusinessProfile, onSave: (List<String>) -> Unit) {
    val allServices = listOf("Utilities", "Home Repair", "Maid")
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
        Text(
            text = "Your Business Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select the services you offer:",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Create a Checkbox for each service
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
                            currentServices.add(serviceName)
                        } else {
                            currentServices.remove(serviceName)
                        }
                        selectedServices = currentServices
                    }
                )
                Text(text = serviceName, color = Color.White, modifier = Modifier.padding(start = 8.dp))
            }
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