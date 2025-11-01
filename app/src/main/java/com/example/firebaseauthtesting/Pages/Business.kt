import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.firebaseauthtesting.ViewModels.BusinessUiState
import com.example.firebaseauthtesting.ViewModels.BusinessViewModel

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
            when (uiState) {
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
                    BusinessDashboard()
                }

                is BusinessUiState.Error -> {
                    // Show an error message if something went wrong
                    val errorMessage = (uiState as BusinessUiState.Error).message
                    Text(text = errorMessage, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun BusinessDashboard() {
    val interphasesFamily = FontFamily(
        Font(R.font.interphases)
    )
    val pantonFamily = FontFamily(
        Font(R.font.panton)
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your Business Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontFamily = interphasesFamily,
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Here you can manage your services and requests.",
            color = Color.White,
            fontFamily = pantonFamily,
            fontSize = 16.sp
        )
        // TODO: Add your business-specific UI here (e.g., list of services, etc.)
    }
}