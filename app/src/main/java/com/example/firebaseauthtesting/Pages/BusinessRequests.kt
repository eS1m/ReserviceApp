package com.example.firebaseauthtesting.Pages

import androidx.compose.foundation.layout.*
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
import com.example.firebaseauthtesting.ViewModels.RequestsUiState
import com.example.firebaseauthtesting.ViewModels.RequestsViewModel
import com.example.firebaseauthtesting.Utils.formatScheduledTimestamp
import com.example.firebaseauthtesting.Utils.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessRequestsScreen(
    navController: NavController,
    requestsViewModel: RequestsViewModel = viewModel()
) {
    val uiState by requestsViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incoming Requests", color = Color.White) },
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
                is RequestsUiState.Loading -> CircularProgressIndicator()
                is RequestsUiState.Error -> Text(text = state.message, color = Color.Red)
                is RequestsUiState.Success -> {
                    if (state.requests.isEmpty()) {
                        Text("You have no incoming requests.", color = Color.Gray)
                    } else {
                        RequestList(
                            requests = state.requests,
                            viewModel = requestsViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestList(
    requests: List<ServiceRequest>,
    viewModel: RequestsViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            RequestItemCard(request = request, viewModel = viewModel)
        }
    }
}

@Composable
fun RequestItemCard(request: ServiceRequest, viewModel: RequestsViewModel) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Request from: ${request.userName}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Service: ${request.serviceCategory}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))

            Text(
                "Status: ${request.status}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            request.scheduledDateTime?.let { scheduledTime ->
                Text(
                    text = "Requested for: ${formatScheduledTimestamp(scheduledTime)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            if (request.status == "Accepted") {
                Text(
                    "Pending Payment from user.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            } else if (request.status == "Paid") {
                Text(
                    "Payment Received!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32), // Green color for success
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Received: ${formatTimestamp(request.timestamp)}", // Use the helper function
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(Modifier.height(16.dp))

            if (request.status == "Pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { viewModel.updateRequestStatus(request.requestId, "Accepted") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Accept")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.updateRequestStatus(request.requestId, "Declined") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) {
                        Text("Decline")
                    }
                }
            }
        }
    }
}
