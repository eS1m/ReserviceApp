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
                            onUpdateRequest = { requestId, newStatus ->
                                requestsViewModel.updateRequestStatus(requestId, newStatus)
                            }
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
    onUpdateRequest: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            RequestItemCard(request = request, onUpdateRequest = onUpdateRequest)
        }
    }
}

@Composable
fun RequestItemCard(
    request: ServiceRequest,
    onUpdateRequest: (String, String) -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(request.serviceCategory, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("From: ${request.userName}", style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${request.status}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            if (request.status == "Pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onUpdateRequest(request.requestId, "Declined") },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Decline")
                    }
                    Button(onClick = { onUpdateRequest(request.requestId, "Accepted") }) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

