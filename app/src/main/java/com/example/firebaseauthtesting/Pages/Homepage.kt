package com.example.firebaseauthtesting.Pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firebaseauthtesting.R
import com.example.firebaseauthtesting.Screen
import com.example.firebaseauthtesting.ViewModels.HomeUiState
import com.example.firebaseauthtesting.ViewModels.HomepageViewModel

@Composable
fun RequestCard(
    modifier: Modifier = Modifier,
    text: String = "",
    iconVector: ImageVector,
    onClick: () -> Unit
) {
    val pantonFamily = FontFamily(
        Font(R.font.panton)
    )

    Box(
        modifier = modifier
            .width(50.dp)
            .height(80.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            border = BorderStroke(1.dp, Color.Black)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        fontFamily = pantonFamily
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    navController: NavController,
    viewModel: HomepageViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val cardItems = listOf(
        "Utilities" to Icons.Outlined.Lightbulb,
        "Home Repair" to Icons.Outlined.Build,
        "Maid" to Icons.Outlined.Person
    )
    val gradientColors = listOf(
        Color(0xFF1b4332),
        Color(0xFF52b788)
    )

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
                    Text("Home",
                        fontFamily = interphasesFamily,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Outlined.Assignment, contentDescription = "Requests")
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.Business.route)
                    }) {
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
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Text("Loading...",
                        fontFamily = interphasesFamily,
                        fontSize = 32.sp)
                }
                is HomeUiState.Success -> {
                    // Display the user's name when loaded
                    Text("Welcome, ${state.fullName}!",
                        fontFamily = interphasesFamily,
                        fontSize = 32.sp)
                }
                is HomeUiState.Error -> {
                    // Show an error if something went wrong
                    Text("Welcome!",
                        fontFamily = interphasesFamily,
                        fontSize = 32.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "What would you like to request today?",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = pantonFamily,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cardItems) { item ->
                    val serviceName = item.first
                    RequestCard(
                        text = item.first,
                        iconVector = item.second,
                        onClick = {
                            navController.navigate(Screen.BusinessMap.createRoute(serviceName))
                        }
                    )
                }
            }
        }
    }
}
