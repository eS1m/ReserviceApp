package com.example.firebaseauthtesting.Pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.firebaseauthtesting.ViewModels.BusinessViewModel
// FIX: Corrected the import path from 'models' to 'model' (singular)
import com.example.firebaseauthtesting.Models.ServiceRequest
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessRequestsScreen(
    navController: NavController,
    businessViewModel: BusinessViewModel
) {
    // State is collected from the shared BusinessViewModel
    val incomingRequests by businessViewModel.incomingRequests.collectAsState()
    val isLoading by businessViewModel.isLoading.collectAsState()
    val errorMessage by businessViewModel.error.collectAsState()

    // Trigger a data refresh when the screen becomes visible
    LaunchedEffect(Unit) {
        businessViewModel.fetchIncomingRequests()
    }

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
            when {
                isLoading -> {
                    // Show a loading spinner while data is being fetched
                    CircularProgressIndicator()
                }
                errorMessage != null -> {
                    // Show an error message if something went wrong
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
                incomingRequests.isEmpty() -> {
                    // Show a message if there are no requests
                    Text("You have no incoming service requests.")
                }
                else -> {
                    // Display the list of incoming requests
                    RequestList(
                        requests = incomingRequests,
                        onUpdateStatus = { requestId, newStatus ->
                            businessViewModel.updateRequestStatus(requestId, newStatus)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestList(
    requests: List<ServiceRequest>,
    onUpdateStatus: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests, key = { it.id }) { request ->
            IncomingRequestCard(
                request = request,
                onUpdateStatus = { newStatus ->
                    onUpdateStatus(request.id, newStatus)
                }
            )
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: ServiceRequest,
    onUpdateStatus: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Request from: ${request.userName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: ${request.status}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (request.status) {
                        "Accepted" -> Color(0xFF6a994e)
                        "Declined" -> Color(0xFFbc4749)
                        "Pending Payment" -> Color(0xFFbc6c25)
                        "Reservice Complete" -> Color(0xFF0a9396)
                        "Pending" -> Color(0xFFe0e1dd)
                        "Cancelled" -> Color(0xFF84a98c)
                        else -> Color.Black
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (request.status == "Pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Accept Button
                    Button(
                        onClick = { onUpdateStatus("Accepted") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6a994e))
                    ) {
                        Text("Accept")
                    }
                    // Decline Button
                    Button(
                        onClick = { onUpdateStatus("Declined") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFbc4749))
                    ) {
                        Text("Decline")
                    }
                }
            }

            if (request.scheduledDate != null && request.scheduledTime != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scheduled for:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "${request.scheduledDate} at ${request.scheduledTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Section 4: Timestamp of the request
            request.timestamp?.let { ts ->
                Text(
                    text = "Received on: ${formatTimestamp(ts)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp): String {
    // This helper function remains unchanged
    val sdf = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}
