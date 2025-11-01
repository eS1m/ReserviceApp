package com.example.firebaseauthtesting.Pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.example.firebaseauthtesting.ViewModels.ProfileRequestsUiState
import com.example.firebaseauthtesting.ViewModels.ProfileRequestsViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRequestsScreen(
    navController: NavController,
    requestsViewModel: ProfileRequestsViewModel = viewModel()
) {
    val uiState by requestsViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Sent Requests", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1b4332))
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ProfileRequestsUiState.Loading -> CircularProgressIndicator()
                is ProfileRequestsUiState.Error -> Text(text = state.message, color = Color.Red)
                is ProfileRequestsUiState.Success -> {
                    if (state.requests.isEmpty()) {
                        Text("You have not made any requests.", color = Color.Gray)
                    } else {
                        SentRequestList(requests = state.requests)
                    }
                }
            }
        }
    }
}

@Composable
fun SentRequestList(requests: List<ServiceRequest>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            SentRequestItemCard(request = request)
        }
    }
}

@Composable
fun SentRequestItemCard(request: ServiceRequest) {
    val statusColor = when (request.status) {
        "Accepted" -> Color(0xFF2E7D32) // Dark Green
        "Declined" -> Color(0xFFC62828) // Dark Red
        else -> Color.Gray
    }

    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Request for: ${request.serviceCategory}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Status: ${request.status}",
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Sent: ${formatTimestamp(request.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}
